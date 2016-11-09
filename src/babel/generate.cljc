(ns babel.generate
  (:refer-clojure :exclude [get-in deref resolve find parents])
  (:require
   [babel.over :as over :refer [intersection-with-identity show-bolt truncate truncate-expressions]]
   #?(:clj [clojure.tools.logging :as log])
   #?(:cljs [babel.logjs :as log]) 
   [clojure.string :as string]
   [dag_unify.core :refer [copy fail-path get-in fail? strip-refs unify unify!]]))
                                        
;; during generation, will not decend deeper than this when creating a tree:
;; TODO: should also be possible to override per-language.
(def ^:const max-total-depth 6)

;; TODO support setting max-generated-complements to :unlimited
(def ^:const max-generated-complements 20000)

;; use map or pmap.
(def ^:const mapfn map)

(def ^:const randomize-lexemes-before-phrases true)
(def ^:const error-if-no-complements false)

(declare add-all-comps)
(declare add-all-comps-with-paths)
(declare add-complement-to-bolt)
(declare any-possible-complement?)
(declare bolt-depth)
(declare candidate-parents)
(declare exception)
(declare find-comp-paths-in)
(declare lazy-mapcat)
(declare lazy-shuffle)
(declare lexemes-before-phrases)
(declare lightning-bolts)
(declare generate-all)
(declare not-fail?)
(declare spec-info)

;; FIXME: truncate-children=false (the non-default option) is not propagated through all calls,
;; causing trees to be unexpectedly truncated.
(defn generate
  "Return a single expression generated by the given language model, constrained by the given spec."
  [spec language-model
   & {:keys [max-total-depth truncate-children lexicon]
      :or {max-total-depth max-total-depth
           lexicon nil
           truncate-children true}}]
  (if (:generate-fn language-model)
    ((:generate-fn language-model) spec)
    (let [spec (if (or (fail? (unify spec {:synsem {:subcat '()}}))
                       (not (= ::none (get-in spec [:synsem :subcat] ::none))))
                 spec
                 
                 ;; else:
                 (unify spec
                        {:synsem {:subcat '()}}))

          ;; remove metadata (if any)
          ;; that's not relevant to generation:
          spec (dissoc spec
                       :dag_unify.core/serialized)
          
          lexicon (if lexicon lexicon (:lexicon language-model))
          morph (:morph language-model)
          morph-ps (:morph-ps language-model)]
      (log/debug (str "generate: generating from spec: "
                      (strip-refs spec) " with max-total-depth: " max-total-depth ";truncate: " truncate-children))
      (let [expression (first (take 1 (generate-all spec language-model 0
                                                    :max-total-depth max-total-depth
                                                    :truncate-children truncate-children)))]
        (if expression
          (log/debug (str "generate: generated "
                          (morph-ps expression)
                          " for spec:" (strip-refs spec)))
          (log/warn (str "generate: no expression could be generated for spec:" (strip-refs spec))))
        expression))))

(defn generate-all
  "Returns all possible expressions generated by the given language model, constrained by the given spec.
   Depending on the grammar in the language model, could be an infinite number of expressions."
  [spec language-model total-depth
   & {:keys [max-total-depth truncate-children]
      :or {max-total-depth max-total-depth
           truncate-children true}}]
  (let [total-depth (if total-depth total-depth 0)]
    (if truncate-children
      (->
       (lightning-bolts language-model spec 0 total-depth :max-total-depth max-total-depth)
       (add-all-comps language-model total-depth true max-total-depth spec)
       (truncate-expressions [[:head]] language-model))
      (->
       (lightning-bolts language-model spec 0 total-depth :max-total-depth max-total-depth)
       (add-all-comps language-model total-depth false max-total-depth spec)))))

(defn lightning-bolts
  "Returns a lazy-sequence of all possible bolts given a spec, where a bolt is a tree
  such that only the head children are generated. This sequence is used by (generate (above))
  to generate expressions by adding complements using (add-all-comps)."
  [language-model spec depth total-depth
                       & {:keys [max-total-depth]
                          :or {max-total-depth max-total-depth}}]
  (log/debug (str "lightning-bolts: start: spec: " (spec-info spec)))
  (let [grammar (:grammar language-model)
        depth (if depth depth 0)
        ;; this is the relative depth; that is, the depth from the top of the current lightning bolt.
        ;; total-depth, on the other hand, is the depth all the way to the top of the entire
        ;; expression, which might involve several parent lightning bolts.
        ;;        parents (lazy-shuffle (candidate-parents grammar spec))]
        parents (candidate-parents grammar spec)]
    (let [lexical ;; 1. generate list of all phrases where the head child of each parent is a lexeme.
          (when (= false (get-in spec [:head :phrasal] false))
            (lazy-mapcat
             (fn [parent]
               (let [debug (log/trace (str "parent before unify with parent: " (spec-info parent)))
                     parent (unify parent spec)
                     subset (if-let [index-fn (:index-fn language-model)]
                              (do (log/trace (str "found index-fn."))
                                  (log/debug (str "lightning-bolts: calling index-fn with rule: " (:rule parent)
                                                  " with spec: " (strip-refs (get-in parent [:head] :top))))
                                  (index-fn (get-in parent [:head] :top)))
                              (do (log/warn (str "no indices found for spec: " spec))
                                  []))]
                 (if (not (empty? subset))
                   (log/debug (str "lightning-bolts: " (get-in parent [:rule])
                                   " : (optimizeme) size of subset of candidate heads: "
                                   (count subset) " with spec: " (strip-refs spec))))
                 (log/trace (str "lightning-bolts: " (get-in parent [:rule])
                                 " : head lexeme candidates: "
                                 (string/join ","
                                              (map #((:morph language-model) %)
                                                   subset))))
                                                     
                 (log/trace (str "trying overh with parent: " (:rule parent) " and head constraints: " (get-in parent [:head])))
                 (let [result (over/overh parent (lazy-shuffle subset))]
                   (if (not (empty? result))
                     (log/debug (str "lightning-bolts: (optimizeme) surviving candidate heads: " (count result))))

                   (log/trace (str "lightning-bolts: surviving results: " 
                                   (string/join ","
                                                (map #((:morph language-model) %)
                                                     result))))

                   (if (and (not (empty? subset)) (empty? result)
                            (> (count subset)
                               50))
                     ;; log/warn because it's very expensive to run
                     ;; over/overh: for every candidate, both parent
                     ;; and candidate head must be copied.
                     (log/debug (str "tried: " (count subset) " lexical candidates with spec:" ( strip-refs spec) " and all of them failed as heads of parent:" (get-in parent [:rule]))))
                   result)))
             (filter #(= false
                         (get-in % [:head :phrasal] false))
                     parents)))
          phrasal ;; 2. generate list of all phrases where the head child of each parent is itself a phrase.
          (if (and (< total-depth max-total-depth)
                   (= true (get-in spec [:head :phrasal] true)))
            (do
              (lazy-mapcat (fn [parent]
                             (log/debug (str "calling lightning-bolts recursively: parent: " (:rule parent) " with spec : "
                                             (spec-info (get-in parent [:head]))))
                             (over/overh
                              parent
                              (lightning-bolts language-model (get-in parent [:head])
                                               (+ 1 depth) (+ 1 total-depth)
                                               :max-total-depth max-total-depth)))
                         (filter #(= true
                                     (get-in % [:head :phrasal] true))
                                 parents))))]
      (filter
       (fn [bolt]
         (any-possible-complement?
          bolt [:comp] language-model total-depth
          :max-total-depth max-total-depth))
       (if (or true (lexemes-before-phrases total-depth max-total-depth))
         (lazy-cat lexical phrasal)
         (lazy-cat phrasal lexical))))))

(defn add-all-comps
  "At each point in each bolt in the list of list of bolts,
  _bolt-groups_, add all possible complements at all open nodes in the
  bolt, from deepest and working upward to the top. Return a lazy
  sequence of having added all possible complements at each node in
  the bolt."
  [bolts language-model total-depth truncate-children max-total-depth top-level-spec]
  (lazy-mapcat
   (fn [bolt]
     (add-all-comps-with-paths [bolt] language-model total-depth
                               (find-comp-paths-in (bolt-depth bolt))
                               truncate-children max-total-depth
                               top-level-spec))
   bolts))

(defn add-all-comps-with-paths [bolts language-model total-depth comp-paths
                                truncate-children max-total-depth top-level-spec]
  (if (empty? comp-paths)
    bolts
    (add-all-comps-with-paths
     (lazy-mapcat
      (fn [bolt]
        (let [path (first comp-paths)]
          (add-complement-to-bolt bolt path
                                  language-model (+ total-depth (count path))
                                  top-level-spec
                                  :max-total-depth max-total-depth
                                  :truncate-children truncate-children
                                  :top-level-spec top-level-spec)))
      bolts)
     language-model total-depth (rest comp-paths)
     truncate-children max-total-depth top-level-spec)))

(defn add-complement-to-bolt [bolt path language-model total-depth top-level-spec
                              & {:keys [max-total-depth truncate-children]
                                 :or {max-total-depth max-total-depth
                                      truncate-children true}}]
  (log/debug (str "add-complement-to-bolt: " (show-bolt bolt language-model)
                  "@[" (string/join " " path) "]" "^" total-depth
                  "; top-level spec: " (strip-refs top-level-spec)))
  (log/debug (str "add-complement-to-bolt: complement-spec: (1) "
                  (strip-refs (get-in bolt path))))

  (let [lexicon (or (-> :generate :lexicon language-model)
                    (:lexicon language-model))
        from-bolt bolt ;; so we can show what (add-complement-to-bolt) did to the input bolt, for logging.
        spec (strip-refs (get-in bolt path))
        debug (log/debug (str "spec to find complement lexemes: " spec))
        complement-candidate-lexemes
        (if (not (= true (get-in bolt (concat path [:phrasal]))))
          (if-let [index-fn
                   (:index-fn language-model)]
            (do (log/debug (str "add-complement-to-bolt with bolt: " (show-bolt bolt language-model)
                                " calling index-fn with spec: " spec ))
                (index-fn spec))
            (flatten (vals lexicon))))
        debug 
        (log/trace (str "lexical-complements (pre-over):"
                        (string/join ","
                                     (map #((:morph language-model) %)
                                          complement-candidate-lexemes))))
               
        bolt-child-synsem (get-in bolt (concat path [:synsem]))
        lexical-complements (lazy-shuffle
                             (filter (fn [lexeme]
                                       (and (not-fail? (unify (get-in lexeme [:synsem] :top)
                                                              bolt-child-synsem))))
                                     complement-candidate-lexemes))]
    (log/trace (str "lexical-complements (post-over):"
                    (string/join ","
                                 (map #((:morph language-model) %)
                                      lexical-complements))))
    (filter #(not-fail? %)
            (mapfn (fn [complement]
                     (let [unified
                           (unify! (copy bolt)
                                   (assoc-in {} path 
                                             (copy complement)))]
                       (if (and (not-fail? unified)
                                truncate-children)
                         (truncate unified [path] language-model)
                         unified)))
                   (let [phrasal-complements (if (and (> max-total-depth total-depth)
                                                      (= true (get-in spec [:phrasal] true)))
                                               (do
                                                 (log/debug (str "calling (generate-all) from add-complement-to-bolt with bolt:"
                                                                 (show-bolt bolt language-model)))
                                                 (generate-all spec language-model (+ (count path) total-depth)
                                                               :max-total-depth max-total-depth)))
                         lexemes-before-phrases (lexemes-before-phrases total-depth max-total-depth)]
                     (cond (and lexemes-before-phrases
                                (empty? lexical-complements)
                                (= false (get-in spec [:phrasal] true)))
                           (log/warn (str "failed to generate any lexical complements with spec: "
                                          (strip-refs spec)))
                           
                           (and lexemes-before-phrases
                                (= true (get-in spec [:phrasal] false))
                                (empty? phrasal-complements))
                           (log/warn (str "failed to generate any phrasal complements with spec: "
                                          (strip-refs spec)))
                           
                           (and (empty? lexical-complements)
                                (empty? phrasal-complements))
                           
                           (let [message (str "add-complement-to-bolt: could generate neither phrasal "
                                              "nor lexical complements for "
                                      "bolt:" (show-bolt bolt language-model) "; immediate parent: "
                                      (get-in bolt (concat (butlast path) [:rule]) :norule) " "
                                      "while trying to create a complement: "
                                      (spec-info spec)
                                      )]
                             (log/debug message)
                             (if error-if-no-complements (exception message)))
                           
                           lexemes-before-phrases
                           (take max-generated-complements
                                 (lazy-cat lexical-complements phrasal-complements))
                           true
                           (take max-generated-complements
                                 (lazy-cat phrasal-complements lexical-complements))))))))

;; TODO: was copied from (defn add-complement-to-bolt) and then modified:
;; refactor both above and below so that commonalities are shared.
(defn any-possible-complement? [bolt path language-model total-depth
                                & {:keys [max-total-depth]
                                   :or {max-total-depth max-total-depth}}]
  (let [lexicon (or (-> :generate :lexicon language-model)
                    (:lexicon language-model))
        spec (get-in bolt path)
        immediate-parent (get-in bolt (butlast path))
        complement-candidate-lexemes
        (if (not (= true (get-in bolt (concat path [:phrasal]))))
          (let [pred (get-in spec [:synsem :sem :pred])
                cat (get-in spec [:synsem :cat])
                pred-set (if (and (:pred2lex language-model)
                                  (not (= :top pred)))
                           (get (:pred2lex language-model) pred))
                cat-set (if (and (:cat2lex language-model)
                                 (not (= :top cat)))
                          (get (:cat2lex language-model) cat))
                subset
                (cond (empty? pred-set)
                      cat-set
                      (empty? cat-set)
                      pred-set
                      true
                      (intersection-with-identity pred-set cat-set))]
            (if (not (empty? subset))
              subset
              (vals (:lexicon language-model)))))
        bolt-child-synsem (strip-refs (get-in bolt (concat path [:synsem]) :top))
        lexical-complements (filter (fn [lexeme]
                                      (and (not-fail? (unify (strip-refs (get-in lexeme [:synsem] :top))
                                                             bolt-child-synsem))))
                                    complement-candidate-lexemes)]
    (or (not (empty? lexical-complements))
        (not (empty?
              (filter #(not-fail? %)
                      (mapfn (fn [complement]
                               (unify (strip-refs (get-in bolt [:synsem]))
                                      (assoc-in {} (concat path [:synsem])
                                                complement)))
                             (if (and (> max-total-depth total-depth)
                                      (= true (get-in spec [:phrasal] true)))
                               (generate-all spec language-model (+ (count path) total-depth)
                                             :max-total-depth max-total-depth)))))))))
  
(defn bolt-depth [bolt]
  (if-let [head (get-in bolt [:head] nil)]
    (+ 1 (bolt-depth head))
    0))

(defn find-comp-paths-in [depth]
  (cond
    ;; most-frequent cases done statically:
    (= 0 depth) nil
    (= 1 depth) [[:comp]]
    (= 2 depth) [[:head :comp][:comp]]
    (= 3 depth) [[:head :head :comp][:head :comp][:comp]]
    (= 4 depth) [[:head :head :head :comp][:head :head :comp][:head :comp][:comp]]

    ;; 
    true
    (cons (vec (concat (take (- depth 1) (repeatedly (fn [] :head))) [:comp]))
          (find-comp-paths-in (- depth 1)))))

(defn candidate-parents
  "find subset of _rules_ for which each member unifies successfully with _spec_"
  [rules spec]
  (log/trace (str "candidate-parents: spec: " (strip-refs spec)))
  (let [result
        (filter not-fail?
                (mapfn (fn [rule]
                         (log/trace (str "candidate-parents: rule: " (:rule rule)))
                         (if (and (not-fail? (unify (get-in rule [:synsem :cat] :top)
                                                    (get-in spec [:synsem :cat] :top)))
                                  (not-fail? (unify (get-in rule [:synsem :infl] :top)
                                                    (get-in spec [:synsem :infl] :top)))
                                  (not-fail? (unify (get-in rule [:synsem :sem :tense] :top)
                                                    (get-in spec [:synsem :sem :tense] :top)))
                                  (not-fail? (unify (get-in rule [:synsem :modified] :top)
                                                   (get-in spec [:synsem :modified] :top))))
                           (unify spec rule)
                           :fail))
                       rules))]
    (log/debug (str "candidate-parents for spec: " (strip-refs spec) " : "
                    (string/join "," (map :rule result))))
    result))

(defn lazy-shuffle [seq]
  (lazy-seq (shuffle seq)))

(defn exception [error-string]
  #?(:clj
     (throw (Exception. (str ": " error-string))))
  #?(:cljs
     (throw (js/Error. error-string))))

;; Thanks to http://clojurian.blogspot.com.br/2012/11/beware-of-mapcat.html
(defn lazy-mapcat  [f coll]
  (lazy-seq
   (if (not-empty coll)
     (concat
      (f (first coll))
      (lazy-mapcat f (rest coll))))))

(defn lexemes-before-phrases
  "returns true or false: true means generate by adding lexemes first;
  otherwise, by adding phrases first. Takes depth as an argument,
  which makes returning true (i.e. lexemes first) increasingly likely
  as depth increases."
  [depth max-total-depth]
  (if (not randomize-lexemes-before-phrases)
    false
    (if (> max-total-depth 0)
      (let [prob (- 1.0 (/ (- max-total-depth depth)
                           max-total-depth))]
        (log/trace (str "P(c," depth ") = " prob " (c: probablity of choosing lexemes rather than phrases given a depth)."))
        (> (* 10 prob) (rand-int 10)))
      false)))

(defn spec-info
  "give a human-readable summary of _spec_."
  [spec]
  (strip-refs
   (merge
    (if-let [cat (get-in spec [:synsem :cat])]
      {:cat cat})
    (if-let [essere (get-in spec [:synsem :essere])]
      {:essere essere})
    (if-let [pred (get-in spec [:synsem :sem :pred])]
      {:pred pred})
    (if-let [agr (get-in spec [:synsem :agr])]
      {:agr agr})
    (if-let [def (get-in spec [:synsem :sem :spec :def])]
      {:def def})
    (if-let [pronoun (get-in spec [:synsem :pronoun])]
      {:pronoun pronoun})
    ;; :synsem/:sem/:mod is sometimes used with nil explicitly, so need to have a special test for it
    (let [mod (get-in spec [:synsem :sem :mod] :not-found-by-spec-info)]
      (if (not (= :not-found-by-spec-info mod))
        {:mod mod}))
    (if-let [modified (get-in spec [:modified])]
      {:modified modified})
    (if-let [subcat1 (if (not (empty? (get-in spec [:synsem :subcat])))
                      (get-in spec [:synsem :subcat :1 :cat]))]
      {:subcat/:1/:cat subcat1
       :subcat/:1/:agr (get-in spec [:synsem :subcat :1 :agr])}))))

(defn not-fail? [arg]
  (not (= :fail arg)))

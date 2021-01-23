(ns menard.generate
  (:require
   #?(:clj [clojure.tools.logging :as log])
   #?(:cljs [cljslog.core :as log])
   [menard.exception :refer [exception]]
   [menard.serialization :as ser]
   [menard.treeops :as tr]
   [dag_unify.core :as u :refer [unify]]
   [dag_unify.diagnostics :as diag :refer [strip-refs]]
   [dag_unify.serialization :refer [serialize]]))

;; See:
;; - english.cljc/generate
;; - nederlands.cljc/generate
;; for example usage.
;; Diagnostics:
;; 1. Start with
;; changing the log/debug in (defn add) (below)
;; to log/info.
;; 2. change other log/debugs to log/infos.
;; Profiling:
;; Turn on (def ^:dynamic profiling?) in your namespace with:
;; (binding [menard.generate/profiling? true]
;;     (menard.generate ..))
(declare add)
(declare add-lexeme)
(declare add-rule)
(declare frontier)
(declare generate-all)
(declare get-lexemes)
(declare reflexive-violations)

#?(:clj (def ^:dynamic fold? false))
#?(:clj (def ^:dynamic truncate? false))

;; clojurescript is much slower without these settings:
;; TODO: investigate why that only holds true for
;; .cljs and not .clj.
#?(:cljs (def ^:dynamic fold? true))
#?(:cljs (def ^:dynamic truncate? true))

;; TODO: rename to allow-rule-backtracking?
(def ^:dynamic allow-backtracking? true)

(def ^:dynamic allow-lexeme-backtracking? true)
(def ^:dynamic max-depth 15)
(def ^:dynamic max-fails 10000)
(def ^:dynamic profiling? false)
(def ^:dynamic counts? (or profiling? (not (nil? max-fails))))
(def ^:dynamic stop-generation-at
 "To use: in your own namespace, override this variable with the path
  before whose generation you want to stop.
  Generation will stop immediately
  when (frontier tree) is equal to this path."
  [])
(def ^:dynamic warn-on-no-matches?
  "warn in (add-rule) and (add) if no grammar rules matched the given spec"
  false)

(def count-adds (atom 0))
(def count-lexeme-fails (atom 0))
(def count-rule-fails (atom 0))

(defn generate
  "Generate a single expression that satisfies _spec_ given the
  _grammar_, and the lexicon indexed by _lexicon-index-fn_, and an
  optionaly function to print out a tree _syntax-tree-fn_. See 
  nederlands/generate and english/generate for sample syntax-tree functions."
  [spec grammar lexicon-index-fn & [syntax-tree-fn]]
  (when counts?
    (reset! count-adds 0)
    (reset! count-lexeme-fails 0)
    (reset! count-rule-fails 0))
  (let [syntax-tree-fn (or syntax-tree-fn (fn [tree] (ser/syntax-tree tree [])))
        result
        (first (generate-all [spec] grammar lexicon-index-fn syntax-tree-fn))]
    (when profiling?
      (log/debug (str "generated: " (syntax-tree-fn result) " with "
                     @count-adds " add" (when (not (= @count-adds 1)) "s") ", "
                     @count-lexeme-fails " lexeme fail" (when (not (= @count-lexeme-fails 1)) "s") " and "
                     @count-rule-fails " rule fail" (when (not (= @count-rule-fails 1)) "s")
                     ".")))
    result))

(defn generate-all
  "Recursively generate trees given input trees. continue recursively
   until no further expansion is possible."
  [trees grammar lexicon-index-fn syntax-tree-fn]
  (when (seq trees)
    (let [tree (first trees)
          frontier (frontier tree)]
      (log/debug (str "generate-all: " frontier ": " (syntax-tree-fn tree)))
      (cond (= :fail tree)
            []

            (and counts?
                 (> (+ @count-lexeme-fails @count-rule-fails)
                    max-fails))
            (do
              (log/debug (str "too many fails: " @count-lexeme-fails " lexeme fail(s) and " @count-rule-fails
                             " rule fail(s); giving up on this tree: " (syntax-tree-fn tree) " at: " frontier "; looking for: "
                             (strip-refs (u/get-in tree frontier))))
              [])

            (> (count frontier) max-depth)
            (do
              (log/debug (str "too deep: giving up on this tree: " (syntax-tree-fn tree) "."))
              [])
            
            (or (u/get-in tree [::done?])
                (and (seq frontier) (= frontier stop-generation-at)))
            (do
              (when (not (u/get-in tree [::done?]))
                (log/debug (str "early stop of generation: " (syntax-tree-fn tree) " at: " frontier)))
              (lazy-seq
               (cons tree
                     (generate-all (rest trees) grammar lexicon-index-fn syntax-tree-fn))))
            :else
            (lazy-cat
             (generate-all
              (add tree grammar lexicon-index-fn syntax-tree-fn) grammar lexicon-index-fn syntax-tree-fn)
             (generate-all (rest trees) grammar lexicon-index-fn syntax-tree-fn))))))

(defn add
  "Return a lazy sequence of all trees made by adding every possible
  leaf and tree to _tree_. This sequence is the union of all trees
  made by adding a leaf (add-lexeme) or by adding a whole
  sub-tree (add-rule)."
  [tree grammar lexicon-index-fn syntax-tree-fn]
  (when counts? (swap! count-adds (fn [_] (+ 1 @count-adds))))
  (let [at (frontier tree)
        rule-at? (u/get-in tree (concat at [:rule]) false)
        phrase-at? (u/get-in tree (concat at [:phrase]) false)
        spec (u/get-in tree at)
        gen-condition
        (cond
          ;; condition 0: tree is :fail.
          (= tree :fail) :tree-is-fail
          
          ;; condition 1: tree is done: return a list with one member: the tree.
          (u/get-in tree [::done?]) :done

          ;; condition 2: only add rules at location _at_:
          (or
           (and (not (= false rule-at?))
                (not (= :top rule-at?)))
           (and (not (= false phrase-at?))
                (not (= :top phrase-at?)))
           (= true (u/get-in tree (concat at [:phrasal])))
           (u/get-in tree (concat at [:head]))
           (u/get-in tree (concat at [:comp]))) :rules-only
          
          ;; condition 3: add only lexemes at location _at_:
          (= false (u/get-in tree (concat at [:phrasal]))) :lexemes-only
          
          ;; condition 4: add both lexemes and rules at location _at_:
          :else :both)]
    (log/info (str "add: start: " (syntax-tree-fn tree) " at: " at
                   (str "; looking for: "
                        (strip-refs (select-keys (u/get-in tree at) [:cat :canonical :infl]))
                        "; gen-condition: " gen-condition)))
    (->>
     (cond
       ;; condition 0: tree is :fail.
       (= gen-condition :fail)
       (exception (str "add: tree is unexpectedly :fail."))

       ;; condition 1: tree is done: return a list with one member: the tree.
       (= gen-condition :done)
       [tree]

       ;; condition 2: only add rules at location _at_:
       (= gen-condition :rules-only)
       (let [result (add-rule tree grammar syntax-tree-fn)]
         (when (and warn-on-no-matches? (empty? result))
           (let [fail-paths
                 (vec 
                  (->> grammar
                       (filter #(= (u/get-in spec [:rule])
                                   (u/get-in % [:rule])))
                       (map (fn [rule]
                              (diag/fail-path spec rule)))))]
             (log/warn (str (syntax-tree-fn tree) ": no rule: "
                            (u/get-in spec [:rule]) " matched spec: "
                            (strip-refs (u/get-in tree at)) " at: " at
                            "; fail-paths:"
                            (when (seq fail-paths)
                              fail-paths)))))
         result)

       ;; condition 3: add only lexemes at location _at_:
       (= gen-condition :lexemes-only)
       (add-lexeme tree lexicon-index-fn syntax-tree-fn)

       ;; condition 4: add both lexemes and rules at location _at_:
       :else ;; (= gen-condition :both)
       (let [both (lazy-cat (add-lexeme tree lexicon-index-fn syntax-tree-fn)
                            (add-rule tree grammar syntax-tree-fn))]
         (log/debug (str "add: adding both lexemes and rules."))
         (cond (and (empty? both)
                    allow-backtracking?)
               (log/debug (str "backtracking: " (syntax-tree-fn tree) " at rule: "
                               (u/get-in tree (concat (butlast at) [:rule])) " for child: "
                               (last at)))
               (empty? both)
               (exception (str "dead end: " (syntax-tree-fn tree)
                               " at: " at "; looking for: "
                               (strip-refs (u/get-in tree at))))

               :else both)))
     (filter #(reflexive-violations % syntax-tree-fn)))))

(declare get-lexemes)

(defn add-lexeme
  "Return a lazy sequence of all trees made by adding every possible
  leaf (via (get-lexemes)) to _tree_."
  [tree lexicon-index-fn syntax-tree]
  (log/debug (str "add-lexeme: " (syntax-tree tree)))
  (let [at (frontier tree)
        done-at (concat (tr/remove-trailing-comps at) [:menard.generate/done?])
        spec (u/get-in tree at)]
    (log/debug (str "add-lexeme: " (syntax-tree tree) " at: " at " with spec: "
                    (serialize spec)))
    (if (= true (u/get-in spec [:phrasal]))
      (exception (str "don't call add-lexeme with phrasal=true! fix your grammar and/or lexicon."))
      (->> (get-lexemes spec lexicon-index-fn)

           (#(if (and (empty? %)
                      (= false allow-lexeme-backtracking?)
                      (= false (u/get-in spec [:phrasal] ::none)))
               (exception (str "no lexemes for tree: " (syntax-tree tree) " at: " at "; no lexemes matched spec: " (dag_unify.diagnostics/strip-refs spec)))
               %))

           ;; need this to prevent eagerly generating a tree for every matching lexeme:
           (#(take (count %) %))

           (map (fn [candidate-lexeme]
                  (-> tree
                      u/copy
                      (u/assoc-in! done-at true)

                      ;; do the actual adjoining of the child within the _tree_'s path _at_:
                      ;;
                      ;;            tree->     /\
                      ;;                       \ ..
                      ;;                       /..
                      ;;                      /
                      ;; path points here -> [] <- add candidate-lexeme here
                      ;;
                      (u/assoc-in! at candidate-lexeme)
                      (#(do (when (not (= :fail %))
                              (log/debug (str "successfully added lexeme: " (strip-refs candidate-lexeme))))
                            %))
                      (tr/update-syntax-tree at syntax-tree)
                      (#(if truncate?
                          (tr/truncate-at % at syntax-tree)
                          %))
                      (#(if fold?
                          (tr/foldup % at syntax-tree)
                          %)))))

           (remove #(= :fail %))))))

(defn add-rule
  "Return a lazy sequence of all trees made by adding every possible grammatical
   rule in _grammar_ to _tree_."
  [tree grammar syntax-tree & [rule-name]]
  (let [at (frontier tree)
        rule-name
        (cond rule-name rule-name
              (not (nil? (u/get-in tree (concat at [:rule])))) (u/get-in tree (concat at [:rule]))
              :else nil)
        cat (u/get-in tree (concat at [:cat]))
        at-num (tr/numeric-frontier (:syntax-tree tree {}))]
    (log/debug (str "add-rule: @" at ": " (when rule-name (str "'" rule-name "'")) ": "
                    (syntax-tree tree) " at: " at))
    (->>
     ;; start with the whole grammar, shuffled:
     (shuffle grammar)
     
     ;; if a :rule is supplied, then filter out all rules that don't have this name:
     (filter #(or (nil? rule-name)
                  (do
                    (log/debug (str "add-rule: looking for rule named: " (u/get-in % [:rule])))
                    (= (u/get-in % [:rule]) rule-name))))
     
     ;; if a :cat is supplied, then filter out all rules that specify a different :cat :
     (filter #(or (nil? cat) (= cat :top) (= :top (u/get-in % [:cat] :top)) (= (u/get-in % [:cat]) cat)))
     
     ;; do the actual adjoining of the child within the _tree_'s path _at_:
     ;;
     ;;            tree->     /\
     ;;                       \ ..
     ;;                       /..
     ;;                      /
     ;; _at_ points here -> [] <- add candidate grammar rule here
     ;;
     (map (fn [rule]
            (log/debug (str "trying rule: " (:rule rule)))
            (let [result
                  (u/assoc-in tree
                              at rule)]
              (when (= :fail result)
                (log/debug
                 (str "rule: " (:rule rule) " failed: "
                      (diag/fail-path tree
                                      (u/assoc-in {}
                                                  at
                                                  rule)))))
              result)))

     ;; some attempts to adjoin will have failed, so remove those:
     (filter #(or (not (= :fail %))
                  (do
                    (when counts? (swap! count-rule-fails inc))
                    false)))
     (map
      #(u/unify! %
                 (assoc-in {} (concat [:syntax-tree] at-num)
                                   (let [one-is-head? (tr/headness? % (concat at [:1]))]
                                     {:head? (= :head (last at))
                                      :1 {:head? one-is-head?}
                                      :2 {:head? (not one-is-head?)}
                                      :variant (u/get-in % [:variant])
                                      :rule
                                      (or rule-name
                                          (u/get-in % (concat at [:rule])))}))))

     (remove #(= % :fail))

     (map (fn [tree]
            (log/debug (str "returning:  " (syntax-tree tree) "; added rule named: " rule-name))
            tree)))))

(defn get-lexemes
  "Get lexemes matching the spec. Use index, where the index 
   is a function that we call with _spec_ to get a set of lexemes
   that matches the given _spec_."
  [spec lexicon-index-fn]
  (log/debug (str "get-lexemes with spec: " (strip-refs spec)))
  (->> (lexicon-index-fn spec)

       (#(do
           (when profiling?
             (log/debug (str "returned: " (count %) " lexeme(s) found.")))
           %))

       (map (fn [lexeme]
              {:lexeme lexeme
               :unify (unify lexeme spec)}))
       (filter (fn [tuple]
                 (let [lexeme (:lexeme tuple)
                       unify (:unify tuple)]
                   (cond (not (= :fail unify))
                         true

                         :else (do
                                (log/debug (str "lexeme candidate failed: " (dag_unify.diagnostics/fail-path spec lexeme)))
                                (when counts? (swap! count-lexeme-fails inc))
                                false)))))
       (map :unify)))

(defn frontier
  "get the next path to which to adjoin within _tree_, or empty path [], if tree is complete."
  [tree]
  
  (let [retval
        (cond
          (= :fail tree)
          []
          
          (= (u/get-in tree [::done?]) true)
          []
          
          (= (u/get-in tree [:phrasal]) false)
          []

          (empty? tree)
          []

          (= ::none (u/get-in tree [::started?] ::none))
          []
          
          (and (u/get-in tree [:head ::done?])
               (u/get-in tree [:comp ::done?]))
          []

          (and (= (u/get-in tree [:phrasal] true) true)
               (= (u/get-in tree [::started?] true) true)
               (not (u/get-in tree [:head ::done?])))
          (cons :head (frontier (u/get-in tree [:head])))

          (and (= (u/get-in tree [:phrasal] true) true)
               (= (u/get-in tree [::started?] true) true)
               (not (u/get-in tree [:comp ::done?])))
          (cons :comp (frontier (u/get-in tree [:comp])))

          :else [])]
    retval))

(defn reflexive-violations [expression syntax-tree-fn]
  (log/debug (str "filtering after adding..:" (syntax-tree-fn expression) "; reflexive: " (u/get-in expression [:reflexive] ::unset)))
  (log/debug (str "   subj/obj identity: " (= (:ref (u/get-in expression [:sem :subj]))
                                              (:ref (u/get-in expression [:sem :obj])))))
  (or
   ;; not a verb:
   (not (= :verb (u/get-in expression [:cat])))

   (and
    ;; non-reflexive verb..
    (= false (u/get-in expression [:reflexive] false))
    ;; .. and :subj and :obj both have :pred = :top
    (= :top (u/get-in expression [:sem :subj :pred] :top))
    (= :top (u/get-in expression [:sem :obj :pred] :top)))

   (and
    ;; non-reflexive verb..
    (= false (u/get-in expression [:reflexive] false))
    ;; .. and :subj and :obj have different :preds (i.e. :i != :you is ok)
    ;; but :i = :i is not ok:
    ;; TODO: this also won't allow e.g. "the man saw a man",
    ;; so this restriction should be less strict: should only disallow
    ;; equal :pred values when the :subj and :obj are pronouns.
    (not (= (u/get-in expression [:sem :subj :pred])
            (u/get-in expression [:sem :obj :pred]))))
    
   ;; reflexive verb: the :subj and :obj refs must be equal:
   (and
    (= true (u/get-in expression [:reflexive] false))
    (= (:ref (u/get-in expression [:sem :subj]))
       (:ref (u/get-in expression [:sem :obj]))))

   (and
    (= :top (u/get-in expression [:reflexive] :top))
    (= (:ref (u/get-in expression [:sem :subj]))
       (:ref (u/get-in expression [:sem :obj])))
    (do
      (log/debug (str ":reflexive not specified: assuming it will be true."))
      true))))

(defn- add-until-done [tree grammar index-fn syntax-tree]
  (if (u/get-in tree [:menard.generate/done?])
    ;; we are done: just return a list of the finished tree:
    [tree]

    ;; not done yet; keep going.
    (-> tree (add grammar index-fn syntax-tree) first (add-until-done grammar index-fn syntax-tree))))

(defn- add-until
  "to the tree _tree_, do (add) _n_ times."
  [tree grammar index-fn syntax-tree n]
  (cond

   (= true (u/get-in tree [:menard.generate/done?]))
   (do
     (log/warn "do-until: tree is done already with n=" n " steps still asked for: original caller should call add-until with a smaller _n_.")
     tree)

   ;; n=0: we're done; return:
   (= n 0) tree

   ;; main case: add and call again with n=n-1:
   :else
   (add-until (-> tree
                  (add grammar index-fn syntax-tree)
                  first)
              grammar
              index-fn
              syntax-tree
              (- n 1))))

(defn generate-seedlike
  "Return a lazy sequence of sentences, all of which are generated starting with a seed tree that itself
     is generated from _spec_. The seed tree's size is set by _seed-tree-size_, which means how big to make the seed tree:
   A bigger seed tree size means step 1 takes longer, but step 2 is shorter, and step 2's output sentences are more similar to each other.
   A smaller seed tree size means step 1 runs shorter, but step 2 is longer, and step 2's output sentences are more distinct from each other.
   So the tradeoff if between variety and speed: the sentences are more similar the more we pre-compute
   in the start, and the more different and slower the sentences are the less we pre-compute.
   For example, for expression 16: total size is 13, and current measurements are:
    - if seed-tree-size=13, then initial seed takes 1500 ms and each child tree takes    0 ms (because tree is already fully done).
    - if seed-tree-size=12, then initial seed takes 1375 ms and each child tree takes   35 ms.
    - if seed-tree-size=10, then initial seed takes 1200 ms and each child tree takes  100 ms.
    - if seed-tree-size=3,  then initial seed takes   73 ms and each child tree takes 1200 ms."
  [spec seed-tree-size grammar index-fn syntax-tree]
  (log/debug (str "doing step 1: generate seed tree of size " seed-tree-size " .."))
  (let [seed-tree (-> spec
                      ((fn [tree]
                         ;; add to the tree until it's reached the desired size:
                         (add-until tree grammar index-fn syntax-tree seed-tree-size))))]
    (log/debug (str "doing step 2: generate trees based on step 1's seed tree: " (syntax-tree seed-tree)))
    (repeatedly #(-> seed-tree
                     (add-until-done grammar index-fn syntax-tree)
                     first))))

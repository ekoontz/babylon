(ns babylon.nederlands
  (:require-macros [babylon.grammar])
  (:require #?(:clj [clojure.java.io :refer [resource]])
            [clojure.string :as string]
            [babylon.lexiconfn :as l]
            [babylon.generate :as g]
            [babylon.grammar :as grammar]
            [babylon.morphology :as m]
            [babylon.parse :as p]
            [babylon.serialization :as s]
            [babylon.ug :as ug]
            #?(:clj [clojure.tools.logging :as log])
            #?(:cljs [cljslog.core :as log])
            [dag_unify.core :as u :refer [pprint unify]]))
;;
;; For generation and parsing of Dutch.
;;

;; <lexicon>
#?(:clj
   (def lexical-rules
     [(l/read-and-eval "babylon/nederlands/lexicon/rules/rules-0.edn")
      (l/read-and-eval "babylon/nederlands/lexicon/rules/rules-1.edn")
      (l/read-and-eval "babylon/nederlands/lexicon/rules/rules-2.edn")]))

#?(:clj
   (defn compile-lexicon-source [source-filename]
     (-> source-filename
         l/read-and-eval
         l/add-exceptions-to-lexicon
         (l/apply-rules-in-order (nth lexical-rules 0) :0)
         (l/apply-rules-in-order (nth lexical-rules 1) :1)
         (l/apply-rules-in-order (nth lexical-rules 2) :2))))

#?(:clj
   (def lexicon
     (merge-with concat
       (compile-lexicon-source "babylon/nederlands/lexicon/adjectives.edn")
       (compile-lexicon-source "babylon/nederlands/lexicon/misc.edn")
       (compile-lexicon-source "babylon/nederlands/lexicon/nouns.edn")
       (compile-lexicon-source "babylon/nederlands/lexicon/propernouns.edn")
       (compile-lexicon-source "babylon/nederlands/lexicon/verbs.edn"))))

#?(:cljs
   (def lexicon
     (-> (l/read-compiled-lexicon "babylon/nederlands/lexicon/compiled.edn")
         l/deserialize-lexicon              
         vals
         flatten)))

#?(:clj
   (defn write-compiled-lexicon []
     (l/write-compiled-lexicon lexicon
                               "src/babylon/nederlands/lexicon/compiled.edn")))

#?(:clj
   (def flattened-lexicon
     (flatten (vals lexicon))))

#?(:clj
   (def verb-lexicon
     (->> flattened-lexicon
          (filter #(and (not (u/get-in % [:exception]))
                        (= (u/get-in % [:cat]) :verb))))))

(def non-verb-lexicon
  (->> flattened-lexicon
       (filter #(and (not (= (u/get-in % [:cat]) :verb))
                     (not (u/get-in % [:exception]))))))

;; </lexicon>

;; <morphology>

;; TODO: move other cljs functions to this file as
;; with this one (def morphology).
#?(:clj
   (def morphology (m/compile-morphology
                    ["babylon/nederlands/morphology/adjectives.edn"
                     "babylon/nederlands/morphology/nouns.edn"
                     "babylon/nederlands/morphology/verbs.edn"])))
#(?:cljs
  (def morphology
    (->> (m/read-compiled-morphology "babylon/nederlands/morphology/compiled.edn")
         (map dag_unify.serialization/deserialize))))

(declare sentence-punctuation)

#?(:clj
   (defn morph
     ([tree]
      (cond
        (map? (u/get-in tree [:syntax-tree]))
        (s/morph (u/get-in tree [:syntax-tree]) morphology)

        true
        (s/morph tree morphology)))

     ([tree & {:keys [sentence-punctuation?]}]
      (if sentence-punctuation?
        (-> tree
            morph
            (sentence-punctuation (u/get-in tree [:sem :mood] :decl)))))))

#?(:cljs
   (defn morph
     ([tree]
      (log/info (str "in babylon.nederlands morph.."))
      (cond
        (map? (u/get-in tree [:syntax-tree]))
        (s/morph (u/get-in tree [:syntax-tree]) morphology)

        true
        (s/morph tree morphology)))

     ([tree & {:keys [sentence-punctuation?]}]
      (if sentence-punctuation?
        (-> tree
            morph
            (sentence-punctuation (u/get-in tree [:sem :mood] :decl)))))))

#?(:clj
   (defn write-compiled-morphology []
     (m/write-compiled-morphology morphology
                                  "src/babylon/nederlands/morphology/compiled.edn")))

;; </morphology>

;; <grammar>
(def finite-tenses
  [;; "hij werkt"
   {:variant :present-simple
    :abbreviation :simple-present
    :infl :present
    :modal false
    :sem {:tense :present
          :aspect :simple}}])

#?(:clj
   (def grammar
     (-> "babylon/nederlands/grammar.edn"
         resource
         slurp
         read-string
         grammar/process)))

#?(:cljs
   (def grammar
     (->> (babylon.grammar/read-compiled-grammar "babylon/nederlands/grammar/compiled.edn")
          (map dag_unify.serialization/deserialize))))

#?(:clj
   (defn write-compiled-grammar []
     (grammar/write-compiled-grammar grammar
                                     "src/babylon/nederlands/grammar/compiled.edn")))

;; </grammar>

;; <expressions>

#?(:clj
   (def expressions
     (-> "babylon/nederlands/expressions.edn"
         resource slurp read-string eval)))

#?(:cljs
   (def expressions
     (grammar/read-expressions "babylon/nederlands/expressions.edn")))

;; </expressions>


;; <functions>

#?(:clj
   (defn syntax-tree [tree]
      (s/syntax-tree tree morphology)))

#?(:cljs
   (defn syntax-tree [tree]
     (s/syntax-tree tree morphology)))

#?(:clj
   (defn index-fn [spec]
     (let [result
           (cond (= (u/get-in spec [:cat]) :verb)
                 verb-lexicon

                 (and (= (u/get-in spec [:cat]))
                      (not (= :top (u/get-in spec [:cat]))))
                 non-verb-lexicon

                 true
                 (lazy-cat verb-lexicon non-verb-lexicon))]
       (if true
         (shuffle result)
         result))))

#?(:cljs
   ;; note that we exclude [:exception]s from the lexemes that we use for
   ;; generation since they are only to be used for parsing.
   (def lexeme-map
     {:verb (->> lexicon
                 (filter #(= :verb (u/get-in % [:cat])))
                 (filter #(not (u/get-in % [:exception]))))
      :det (->> lexicon
                (filter #(= :det (u/get-in % [:cat]))))
      :intensifier (->> lexicon
                        (filter #(= :intensifier (u/get-in % [:cat]))))
      :noun (->> lexicon
                 (filter #(= :noun (u/get-in % [:cat])))
                 (filter #(not (u/get-in % [:exception]))))
      :top lexicon
      :adjective (->> lexicon
                      (filter #(= :adjective (u/get-in % [:cat]))))}))

#?(:cljs
   (defn index-fn [spec]
     ;; for now a somewhat bad index function: simply returns
     ;; lexemes which match the spec's :cat, or, if the :cat isn't
     ;; defined, just return all the lexemes.
     (let [result (get lexeme-map (u/get-in spec [:cat] :top) nil)]
       (if (not (nil? result))
           (shuffle result)
           (do
             (log/warn (str "no entry from cat: " (u/get-in spec [:cat] ::none) " in lexeme-map: returning all lexemes."))
             lexicon)))))

#?(:clj
   (defn generate
     "generate one random expression that satisfies _spec_."
     [spec]
     (binding []) ;;  g/stop-generation-at [:head :comp :head :comp]
     (g/generate spec grammar index-fn syntax-tree)))

#(:cljs
  (defn generate [spec & [times]]
    (let [attempt
          (try
            (g/generate spec
                        grammar
                        (fn [spec]
                          (shuffle (index-fn spec)))
                        syntax-tree)
            (catch js/Error e
              (cond
                (or (nil? times)
                    (< times 2))
                (do
                  (log/warn (str "retry #" (if (nil? times) 1 (+ 1 times))))
                  (generate spec (if (nil? times) 1 (+ 1 times))))
                true nil)))]
        (cond
          (and (or (nil? times)
                   (< times 2))
               (or (= :fail attempt)
                   (nil? attempt)))
          (do
            (log/info (str "retry #" (if (nil? times) 1 (+ 1 times))))
            (generate spec (if (nil? times) 1 (+ 1 times))))
          (or (nil? attempt) (= :fail attempt))
          (log/error (str "giving up generating after 2 times; sorry."))
          true
          {:structure attempt
           :syntax-tree (syntax-tree attempt)
           :surface (morph attempt)}))))

#?(:clj
   (defn get-lexemes [spec]
     (g/get-lexemes spec index-fn syntax-tree)))

#?(:clj
   (defn generate-n
     "generate _n_ consecutive in-order expressions that satisfy _spec_."
     [spec n]
     (take n (repeatedly #(generate spec)))))

(defn parse [expression]
  (binding [p/grammar grammar
            p/syntax-tree syntax-tree
            l/lexicon lexicon
            l/morphology morphology
            p/split-on #"[ ]"
            p/lookup-fn l/matching-lexemes]
    (p/parse expression morph)))

#?(:clj
   (defn analyze [surface]
     (binding [l/lexicon lexicon
               l/morphology morphology]
       (l/matching-lexemes surface))))              

#(:clj
  (defn demo []
    (count
     (->>
      (range 0 (count expressions))
      (map (fn [index]
             (let [generated-expressions
                   (->> (repeatedly (fn [] (generate (nth expressions index))))
                        (take 20)
                        (filter (fn [generated] (not (nil? generated)))))]
               ;; for each expression:
               ;; generate it, and print the surface form
               ;; parse the surface form and return the first parse tree.
               (count
                (->> generated-expressions
                     (map (fn [generated-expression]
                            (-> generated-expression
                                (morph :sentence-punctuation? true)
                                println)
                            (if false
                              (-> generated-expression
                                  morph
                                  parse
                                  first
                                  syntax-tree
                                  println))
                            (if false (println)))))))))))))

(defn generate-word-by-word [tree grammar index-fn syntax-tree]
  (let [add-rule (fn [tree]
                   (first (g/add-rule tree grammar syntax-tree)))
        add-lexeme (fn [tree]
                     (first (g/add-lexeme tree index-fn syntax-tree)))
        add (fn [tree]
              (let [at (g/frontier tree)
                    add-phrasal? (u/get-in tree (concat at [:phrasal]))]
                (cond add-phrasal?
                      (add-rule tree)
                      true
                      (add-lexeme tree))))]
    (log/debug (str "intermediate result: " (morph tree)))
    (cond
      (nil? tree) tree
      (:fail tree) tree
      (u/get-in tree [:babylon.generate/done?]) tree
      true (generate (add tree) grammar index-fn syntax-tree))))

(defn testing-with [grammar index-fn syntax-tree]
  (g/generate
   {:phrasal true
    :rule "s"
    :reflexive false
    :comp {:phrasal true
           :rule "np"
           :head {:phrasal true
                  :comp {:phrasal true}}
           :comp {:phrasal false}}
    :head {:phrasal true
           :rule "vp"
           :head {:phrasal false}
           :comp {:phrasal true
                  :rule "np"
                  :head {:phrasal true
                         :comp {:phrasal true}}
                  :comp {:phrasal false}}}}
   grammar index-fn syntax-tree))

#?(:clj
   (defn testing []
     (let [testing (fn []
                     (time (testing-with grammar index-fn syntax-tree)))]
       (repeatedly #(do (println (str " " (sentence-punctuation (morph (testing)) :decl)))
                        1)))))

(def bigram
  {:phrasal true
   :head {:phrasal false}
   :comp {:phrasal false}
   :subcat []})

#?(:clj
   (defn bigrams []
     (repeatedly #(println
                   (morph (time
                           (g/generate
                            bigram
                            grammar index-fn syntax-tree)))))))

(defn sentence-punctuation
  "Capitalizes the first letter and puts a period (.) or question mark (?) at the end."
  [input mood]
  (str (string/capitalize (first input))
       (subs input 1 (count input))
       (if (= mood :interog)
         "?"
         ".")))

;; </functions>

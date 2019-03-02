(ns babylon.english
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [babylon.lexiconfn :as l]
            [babylon.generate :as g]
            [babylon.grammar :as grammar]
            [babylon.morphology :as m]
            [babylon.parse :as p]
            [babylon.ug :as ug]
            [clojure.tools.logging :as log]
            [dag_unify.core :as u :refer [unify]]))
;;
;; For generation and parsing of English.
;;
(defn apply-rules-to [lexicon]
  (-> lexicon
      (l/apply-rules-in-order (l/read-rules "babylon/english/lexicon/rules/rules-0.edn"))
      (l/apply-rules-in-order (l/read-rules "babylon/english/lexicon/rules/rules-1.edn"))
      (l/apply-rules-to-lexicon (l/read-rules "babylon/english/lexicon/rules/rules-2.edn") true)))

(defn compile-lexicon [filename]
  (-> filename
      l/read-rules
      l/add-exceptions-to-lexicon
      apply-rules-to))

(def lexicon
  (merge-with concat
    (compile-lexicon "babylon/english/lexicon/adjectives.edn")
    (compile-lexicon "babylon/english/lexicon/misc.edn")
    (compile-lexicon "babylon/english/lexicon/propernouns.edn")
    (compile-lexicon "babylon/english/lexicon/nouns.edn")
    (compile-lexicon "babylon/english/lexicon/verbs.edn")))

(def finite-tenses
  [{:infl :present
    :sem {:tense :future}
    :head {:aux true}}
   {:infl :present
    :sem {:tense :present
          :aspect :simple}}
   {:infl :past-simple
    :sem {:tense :past
          :aspect :simple}}])

(def tenses
  (concat finite-tenses
          [{:infl :base}]))

(def aux-tenses
  [{:infl :present
    :sem {:tense :future}}])

(def vp-aux-phrase
  (let [comp-subcat (atom :top)
        comp (atom {:subcat comp-subcat})]
    {:subcat comp-subcat
     :head {:subcat {:1 comp}}
     :comp comp}))

(def grammar
  (-> "babylon/english/grammar.edn"
      io/resource
      slurp
      read-string
      grammar/process))

(def morphology
  (concat
   (-> "babylon/english/morphology/nouns.edn"
       l/read-rules)
   (-> "babylon/english/morphology/verbs.edn"
       l/read-rules)))

(defn an
  "change 'a' to 'an' if the next word starts with a vowel; 
   and change 'an' to 'a' if the next word does *not* start with a vowel."
  [input]
  (-> input
      (string/replace #"\b([aA]) ([aeiou])"  "$1n $2")
      (string/replace #"\b([aA])n ([^aeiou])" "$1 $2")))

(declare sentence-punctuation)

(defn morph
  ([structure]
   (binding [grammar/morph-leaf m/morph-leaf
             m/morphology morphology]
     (cond (nil? structure) structure
           true
           (-> (grammar/default-morph-fn structure)
               an))))

  ([structure & {:keys [sentence-punctuation?]}]
   (binding [grammar/morph-leaf m/morph-leaf
             m/morphology morphology]
     (if sentence-punctuation?
       (-> (grammar/default-morph-fn structure)
           an
           sentence-punctuation)))))

(defn sentence-punctuation
  "Capitalizes the first letter and puts a period (.) at the end."
  [input]
  (str (string/capitalize (first input))
       (subs input 1 (count input))
       "."))

(defn syntax-tree
  "print a concise representation of a tree."
  [structure]
  (binding [grammar/morph-leaf m/morph-leaf
            m/morphology morphology]
    (grammar/syntax-tree structure)))
        
(defn lookup
  "find lexemes that satisfy _spec_."
  [spec]
  (binding [g/lexicon lexicon
            m/morphology morphology
            g/morph-ps syntax-tree]
    (let [spec (let [with-subcat-empty
                     (unify spec {:subcat []})]
                 (if (= :fail with-subcat-empty)
                   spec
                   with-subcat-empty))]
       (g/get-lexemes spec))))

(defn generate
  "generate one random expression that satisfies _spec_."
  [spec]
  (binding [g/lexicon lexicon
            m/morphology morphology
            g/morph-ps syntax-tree]
    (let [spec (let [with-cat
                     (unify spec {:cat (first (shuffle [:noun :verb]))})]
                 (if (= :fail with-cat)
                     spec
                     with-cat))
          spec (let [with-subcat-empty
                     (unify spec {:subcat []})]
                 (if (= :fail with-subcat-empty)
                   spec
                   with-subcat-empty))]
      (-> spec
          (g/generate grammar)))))

(defn generate-n
  "generate _n_ consecutive in-order expressions that satisfy _spec_."
  [spec n]
  (binding [g/lexicon lexicon
            m/morphology morphology
            g/morph-ps syntax-tree
            g/shuffle? false]
    (take n (g/generate-all (unify spec
                                   {:subcat []})
                            grammar))))
(defn parse [expression]
  (binding [p/grammar grammar
            l/lexicon lexicon
            l/morphology morphology
            p/lookup-fn l/matching-lexemes]
    (p/parse expression)))

(defn analyze [surface]
  (binding [l/lexicon lexicon
            l/morphology morphology]
    (l/matching-lexemes surface)))              

(def tree-specs
  [{:comp {:phrasal true
           :head {:phrasal true
                  :head {:phrasal true}}}
    :head {:phrasal true
           :comp {:phrasal true
                  :head {:phrasal true}}}}

   {:comp {:phrasal true
           :head {:phrasal true
                  :head {:phrasal true}}}
    :head {:phrasal true
           :comp {:phrasal true}}}

   {:comp {:phrasal false}
    :head {:phrasal false}}])


(defn generate-long-sentence [& {:keys [spec trees]}]
  (if (empty? trees)
    (throw (Exception. (str "no trees left; can't generate a sentence.")))
   (let [extra-constraints (if spec spec :top)
         tree (unify extra-constraints (first trees))]
    (try (generate
          (unify {:cat :verb :subcat []}
                 tree))
         (catch Exception e
           (do (log/warn (str "failed to generate: "
                              (syntax-tree (:tree (ex-data e)))
                              " at path:" (:frontier-path (ex-data e))
                              " with spec: " (u/strip-refs (:child-spec (ex-data e))) 
                              "; retrying with same root: '" (u/get-in (:tree (ex-data e)) [:root])
                              "'.."))
               (generate-long-sentence
                :trees (rest trees)
                :spec {:root (u/get-in (:tree (ex-data e)) [:root])})))))))

(defn demo []
  (println "Generation:")
  (println "===")
  (println)
  (println "= transitive sentences =")
  (println)
  (count (take 10 (repeatedly #(println (morph (generate
                                                {:cat :verb
                                                 :reflexive false
                                                 :sem {:pred :see
                                                       :obj {:pred :top}}})
                                               :sentence-punctuation? true)))))
  (println)
  (println "= reflexive sentences =")
  (println)
  (count (take 10 (repeatedly #(println (morph
                                         (generate {:cat :verb
                                                    :sem {:pred :see}
                                                    :reflexive true})
                                         :sentence-punctuation? true)))))
  (println)
  (println "= 'long' sentences =")
  (println)
  (count
   (take 10
    (repeatedly
      #(println
         (morph
          (generate-long-sentence :trees tree-specs)
          :sentence-punctuation? true)))))
  (println)
  (println "Parsing:")
  (println "===")
  (println)
  (count (take 10
               (repeatedly #(let [expression (morph (generate {:cat (first (shuffle [:noun :verb]))
                                                               :subcat []}))]
                              (println (->> (parse expression)
                                            (map syntax-tree)
                                            (string/join ", "))))))))

(defn capitalize-first-letter
  "clojure.string/capitalize is too much: it lower-cases every word in the string *except*
   the first letter. We need a function that capitalizes the first letter and leaves
   the rest alone (they might already be upper-case like the pronoun 'I', and should stay that way)."
  [input]
  (string/join ""
               (concat (string/capitalize (first input))
                       (rest input))))

(defn an
  "change 'a' to 'an' if the next word starts with a vowel."
  [input]
  (-> input
      (string/replace #"\b([aA]) ([aeiou])"   "$1n $2")
      (string/replace #"\b([aA])n ([^aeiou])" "$1 $2")))

(defn poetry-line []
  (->
   {:cat :verb
    :subcat []
    :pred :top
    :comp {:phrasal true
           :head {:phrasal true}}
    :head {:phrasal true
           :comp {:phrasal true
                  :head {:phrasal true}}}}
   generate
   morph
   an
   capitalize-first-letter
   (str ". ")))

(defn benchmark []
  (repeatedly
   #(println
     (time (poetry-line)))))

(defn poetry []
  (repeatedly
   #(println
     (poetry-line))))


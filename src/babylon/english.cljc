(ns babylon.english
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [dag_unify.core :as u :refer [unify]]
            [dag_unify.dissoc :refer [dissoc-in]]
            [babylon.lexiconfn :as l]
            [babylon.generate :as g]
            [babylon.grammar :as grammar :refer [syntax-tree]]
            [babylon.parse :as p]
            [babylon.ug :refer [head-last head-rule process-grammar subcat-1]]))
;;
;; For generation and parsing of English.
;; 
(def lexical-rules
  [
   ;; determiners have an empty subcat.
   [{:cat :det} {:subcat []}]])

(def lexicon (-> "babylon/english/lexicon.edn"
                 io/resource
                 slurp
                 read-string
                 (l/process lexical-rules)))

(def grammar (-> "babylon/english/grammar.edn"
                 io/resource
                 slurp
                 read-string
                 process-grammar))

(def morphology
  (concat
   [(-> "babylon/english/morphology/nouns.edn"
        clojure.java.io/resource
        slurp
        read-string)
    (-> "babylon/english/morphology/verbs.edn"
        clojure.java.io/resource
        slurp
        read-string)]))

(defn morph [structure]
  ;; TODO: flesh out:
  (grammar/default-morph-fn structure))

(defn generate [spec]
  (binding [g/grammar grammar
            g/lexicon lexicon
            g/morph-ps syntax-tree]
    (g/generate spec)))

(defn parse [expression]
  (binding [p/grammar grammar
            p/lexicon lexicon
            p/lookup-fn (fn [word]
                          (get lexicon word))]
    (p/parse expression
             {:grammar grammar
              :lexicon lexicon
              :lookup (fn [word]
                        (get lexicon word))})))

(defn demo []
  (println "Generation:")
  (println "===")
  (count (take 10 (repeatedly #(println (morph (generate :top))))))
  (println "===")
  (count (take 10 (repeatedly #(println (morph (generate {:cat :v}))))))
  (println "===")
  (count (take 10 (repeatedly #(println (morph (generate {:cat :n}))))))
  (println "Parsing:")
  (println "===")
  (count (take 10
               (repeatedly #(let [expression (morph (generate {:cat :top}))]
                              (println (->> (parse expression)
                                            (map syntax-tree)
                                            (string/join ", "))))))))


(ns menard.model
  (:refer-clojure :exclude [load])
  (:require [clojure.tools.logging :as log]))

(defn load [language-name rules-fn lexicon-fn fill-lexicon-indexes-fn load-morphology-fn load-grammar-fn]
  (log/info (str "loading resources for language: " language-name))
  (let [rules (rules-fn)]
    (log/info (str "loaded: " (count rules) " lexical rule sets."))
    (let [lexicon (lexicon-fn rules)]
      (log/info (str "loaded: " (count (keys lexicon)) " lexeme keys."))
      (let [indices (fill-lexicon-indexes-fn lexicon)]
        (log/info (str "loaded: " (count (keys indices)) " lexicon indices."))
        (let [morphology (load-morphology-fn)]
          (log/info (str "loaded: " (count morphology) " morphological rules."))
          (let [grammar (load-grammar-fn)]
            (log/info (str "loaded: " (count grammar) " grammar rules."))
            {:grammar grammar
             :language language-name
             :morphology morphology
             :rules rules
             :lexicon lexicon
             :indices indices}))))))






(ns babel.francais
  (:require
   [babel.francais.grammar :as grammar]
   [babel.francais.morphology :as morph :refer [fo]]
   [babel.generate :as generate]
   [babel.parse :as parse]
   [clojure.repl :refer [doc]]
   [clojure.string :as string]))

;; can't decide between 'morph' or 'fo' or something other better name.
(defn morph [expr & {:keys [from-language show-notes]
                     :or {from-language nil
                          show-notes false}}]
  (fo expr :from-language from-language :show-notes show-notes))

(def medium-model (promise))
(defn medium [] (if (realized? medium-model)
                  @medium-model
                  @(deliver medium-model (grammar/medium))))

(defn analyze
  ([surface-form]
   (analyze surface-form (:lexicon (medium))))
  ([surface-form lexicon]
   (morph/analyze surface-form lexicon)))

(defn generate
  ([]
   (let [result (generate/generate :top (medium))]
     (if result
       (conj {:surface (fo result)}
             result))))
  ([spec & {:keys [max-total-depth model truncate-children]
            :or {max-total-depth generate/max-depth
                 truncate-children true
                 model (medium)}}]
   (let [result (generate/generate spec model)]
     (conj {:surface (morph result)}
           result))))

(defn parse
  "parse a string in French into zero or more (hopefully more) phrase structure trees"
  
  ([input & {:keys [parse-with-truncate model]}]
   (parse/parse input (or model (medium))
                :parse-with-truncate (if (nil? parse-with-truncate)
                                       true
                                       parse-with-truncate))))



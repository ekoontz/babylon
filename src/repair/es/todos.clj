;; Command line usage: 'lein run -m repair.es.todos/fix'
(ns repair.es.todos)
(require '[babel.writer :refer [fill-by-spec fill-verb write-lexicon]])
(require '[babel.english :as en])
(require '[babel.espanol :as es :refer [verbs]])
(require '[babel.espanol.morphology :as morph :refer [fo]])
(require '[babel.parse :as parse])
(require '[babel.reader :as reader])
(require '[babel.repair :refer [process]])
(require '[clojure.data.json :as json])
(require '[dag-unify.core :refer [unify deserialize]])

(defn fix [ & [count]]
  (let [count 10]
    (write-lexicon "es" @es/lexicon)
    (process
     (reduce concat
             (map (fn [verb]
                    (map (fn [tense]
                           {:fill
                            {:spec (unify {:root {:espanol {:espanol verb}}}
                                          tense)
                             :source-model en/small
                             :target-model es/small
                             :count count}})
                         (shuffle
                          [
                           {:synsem {:sem {:aspect :perfect
                                           :tense :past}}}
                           {:synsem {:sem {:tense :conditional}}}
                           {:synsem {:sem {:tense :futuro}}}
                           {:synsem {:sem {:tense :present}}}
                           ])
                         ))
                  (sort verbs)
                  )))))



(ns babel.english.demo
  (:require
   [babel.english :refer [generate]]
   [babel.english.grammar :refer [few-rules medium small-lexicon]]
   [babel.english.morphology :refer [fo]]
   #?(:cljs [babel.logjs :as log])
   [clojure.string :as string]
   #?(:clj [clojure.tools.logging :as log])
   [dag_unify.core :refer [unifyc]]))

(declare run-demo-with)

;; during generation, will not decend deeper than this when creating a tree:
;; should also be possible to override per-language.
(def ^:const max-total-depth 25)

(def default-language-model (medium))

(defn demo [ & [n spec]]
  (let [demo-specs
        [{:demo-name "Dog noun phrases"
          :synsem {:cat :noun
                   :sem {:pred :cane}}}

         {:demo-name "Very specific noun phrases: few-rules"
          :lm (few-rules)
          :synsem {:cat :noun
                   :sem {:pred :cane
                         :number :plur
                         :mod {:pred :first}
                         :spec {:def :genitive
                                :of {:pred :luisa}}}}}

         {:demo-name "Very specific noun phrases: small-lexicon"
          :lm (small-lexicon)
          :synsem {:cat :noun
                   :sem {:pred :cane
                         :number :plur
                         :mod {:pred :second}
                         :spec {:def :genitive
                                :of {:pred :luisa}}}}}
        
         {:demo-name "Very specific noun phrases"
          :synsem {:cat :noun
                   :sem {:pred :cane
                         :number :plur
                         :mod {:pred :first}
                         :spec {:def :genitive
                                :of {:pred :luisa}}}}}

         {:demo-name "Noun Phrases"
          :synsem {:cat :noun}}
         
         {:demo-name "Sentences about dogs eating"
          :synsem {:cat :verb
                   :sem {:subj {:pred :cane}
                         :pred :eat}}}

         {:demo-name "Sentences about thinking"
          :synsem {:cat :verb
                   :sem {:pred :think}}}

         {:demo-name "Sentences about believing"
          :synsem {:cat :verb
                   :sem {:pred :believe}}}

         {:demo-name "Sentences about assuming"
          :synsem {:cat :verb
                   :sem {:pred :assume}}}
        
         {:synsem {:cat :verb}
          :demo-name "Sentences"}

         {:synsem :top
          :demo-name "Totally random expressions"}
         ]]
         
    (doall (map (fn [spec]
                  (let [log-message 
                        (str "running demo: " (:demo-name spec) "; " n " attempts.")]
                    (do (log/info log-message)
                        (println)
                        (println log-message)
                        (println)
                        (let [language-model (if (:lm spec) (:lm spec)
                                                 default-language-model)
                              expressions (run-demo-with n (dissoc spec :lm) language-model)]
                          (count (pmap (fn [expression]
                                         (let [formatted (fo expression :show-notes false)]
                                           (println
                                            (str (string/capitalize (subs formatted 0 1))
                                                 (subs formatted 1 (count formatted))
                                                 "."))))
                                       expressions))))))
                (if (and spec (not (empty? (string/trim spec))))
                  (filter #(= (:demo-name %)
                              spec)
                          demo-specs)
                  demo-specs)))
    (println (str "babel demo is finished. JVM shutting down."))))

(defn run-demo-with [n spec model]
  "print out _n_ generated sentences to stdout."
  (let [n (if n (Integer. n)
              100)
        spec (if spec (unifyc spec
                              {:modified false})
                 {:modified false})]
    (filter #(not (nil? %)) 
            (take n (repeatedly
                     #(let [result
                            (time
                             (generate spec
                                       :model model
                                       :max-total-depth max-total-depth))]
                        result))))))


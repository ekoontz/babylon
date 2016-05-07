(ns babel.italiano.benchmark
  (:refer-clojure :exclude [get-in])
  (:require [babel.engine :refer [generate-all]]
            [babel.italiano :refer [analyze generate parse]]
            [babel.italiano.grammar :refer [small medium np-grammar]]
            [babel.italiano.lexicon :refer [lexicon]]
            [babel.italiano.morphology :as morph :refer [analyze-regular fo replace-patterns]]
            [babel.italiano.morphology.nouns :as nouns]
            [babel.italiano.morphology.verbs :as verbs]
            [babel.parse :as parse]
            #?(:cljs [cljs.test :refer-macros [deftest is]])
            #?(:clj [clojure.tools.logging :as log])
            #?(:cljs [babel.logjs :as log])
            [clojure.string :as string]
            [dag_unify.core :refer [get-in strip-refs]]))

(defn exception [error-string]
  #?(:clj
     (throw (Exception. error-string)))
  #?(:cljs
     (throw (js/Error. error-string))))

(defn run-benchmark
  ([]
   (run-benchmark 10))

  ([times]
   (count (take (Integer/parseInt times)
                (repeatedly #(let [debug (println "starting generation..")
                                   expr (time (generate {:comp {:synsem {:agr {:person :3rd}}}
                                                         :synsem {:cat :verb}}))]
                               (println (str "generated: " (fo expr)))
                               (println (str "starting parsing.."))
                               ;; take the first parse in order to force evaluation of parsing so that (time ..)'s return value is meaningful.
                               (let [parses (time (take 1 (reduce concat (map :parses (parse (fo expr))))))]
                                 (if (empty? parses)
                                   (throw (exception (str "could not parse: " (fo expr) " with semantics:"
                                                          (strip-refs (get-in expr [:synsem :sem]))))))
                                 
                                 (println (str "parsed: " (fo (first parses))))
                                 (println ""))))))))
(defn -main [times]
  (run-benchmark times))




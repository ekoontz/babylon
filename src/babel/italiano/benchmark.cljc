(ns babel.italiano.benchmark
  (:refer-clojure :exclude [get-in])
  (:require [babel.italiano :as italiano :refer [analyze parse]]
            [babel.italiano.grammar :as grammar]
            [babel.italiano.morphology :as morph :refer [analyze-regular fo replace-patterns]]
            [babel.italiano.morphology.nouns :as nouns]
            [babel.italiano.morphology.verbs :as verbs]
            [babel.parse :as parse]
            #?(:cljs [cljs.test :refer-macros [deftest is]])
            #?(:clj [clojure.tools.logging :as log])
            #?(:cljs [babel.logjs :as log])
            [clojure.repl :refer [doc]]
            [clojure.string :as string]
            [dag_unify.core :refer [get-in strip-refs]]))

;; Creating a language model is expensive so we'll do it once before running any benchmarks..
(def small (grammar/small))

;; .. and use this language model for all generation.
(defn generate [spec]
  (italiano/generate spec :model small))

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

;; lein run -m babel.italiano.benchmark/parse-mark 20 "i gatti neri hanno bevuto il vino rosso"
;; lein run -m babel.italiano.benchmark/parse-mark 20 "gli uomini alti hanno bevuto il vino rosso alla casa rossa"
(defn parse-mark [times expr]
  (count (take (Integer. times)
               (repeatedly
                #(println (with-out-str
                            (time (mapcat :parses (parse expr)))))))))

(defn benchmark [spec model do-this-many]
  (let [do-this-many (Integer. do-this-many)
        timings
        (->>
         (take do-this-many
               (repeatedly 
                #(with-out-str (time (fo
                                      (let [generated
                                            (italiano/generate spec :model model)
                                            output (log/info (fo generated))]
                                        generated))))))
         (map #(string/replace % #".*time:\s*([0-9.]+).*" "$1"))
         (map string/trim)
         (map #(Double. %))
         ;; remove first run time - it's an outlier because of startup
         ;; and language model-loading.
         (#(if (> (count %) 1) (rest %) %))
         (sort))]
    (let [result
          {:mean (/ (reduce + timings) (count timings))
           :median (nth timings (int (Math/floor (/ (count timings) 2))))
           :.75 (nth timings (int (Math/floor (* (count timings) 0.75))))
           :.95 (nth timings (int (Math/floor (* (count timings) 0.95))))
           :times timings
           }]
      (println result)
      result)))

;; lein run -m babel.italiano.benchmark/gen-mark 10
(defn gen-mark [do-this-many]
  (let [do-this-many (Integer. do-this-many)]
    (benchmark {:synsem {:subcat '()
                         :sem {:pred :be-called
                               :tense :present
                               :subj :top
                               :iobj :top}}}
               small
               do-this-many)))

(defn gen-mark2 [do-this-many]
  (let [do-this-many (Integer. do-this-many)]
    (benchmark {:synsem {:subcat '()
                         :essere false
                         :sem {:pred :do
                               :tense :past
                               :aspect :perfect}}}
               small
               do-this-many)))

(defn gen-mark3 [do-this-many]
  (let [do-this-many (Integer. do-this-many)]
    (benchmark {:synsem {:subcat '()
                         :essere true
                         :sem {:tense :past
                               :aspect :perfect}}
                :root {:italiano {:italiano "andare"}}}
               small
               do-this-many)))

  

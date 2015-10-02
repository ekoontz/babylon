(ns babel.espanol.writer
  (:refer-clojure :exclude [get-in]))

(require '[babel.cache :refer (build-lex-sch-cache create-index spec-to-phrases)])
(require '[babel.forest :as forest])
(require '[babel.english.grammar :as en])
(require '[babel.enrich :refer [enrich]])
(require '[babel.espanol.grammar :refer [grammar]])
(require '[babel.espanol.lexicon :as lex])
(require '[babel.espanol.pos :refer :all])
(require '[babel.lexiconfn :refer (compile-lex map-function-on-map-vals unify)])
(require '[babel.espanol.morphology :as morph :refer [fo]])
(require '[babel.parse :as parse])
(require '[babel.ug :refer :all])
(require '[babel.writer :refer [process write-lexicon]])
(require '[clojure.string :as string])
(require '[clojure.tools.logging :as log])
(require '[dag-unify.core :refer (fail? get-in strip-refs)])
(require '[dag-unify.core :as unify])

;; for debugging:
(require '[babel.espanol.morphology.verbs :as verbs])

;; see TODOs in lexiconfn/compile-lex (should be more of a pipeline as opposed to a argument-position-sensitive function.
(def lexicon
  (future (-> (compile-lex lex/lexicon-source morph/exception-generator morph/phonize)

              ;; make an intransitive version of every verb which has an
              ;; [:sem :obj] path.
              intransitivize
              
              ;; if verb does specify a [:sem :obj], then fill it in with subcat info.
              transitivize
              
              ;; Cleanup functions can go here. Number them for ease of reading.
              ;; 1. this filters out any verbs without an inflection: infinitive verbs should have inflection ':infinitive', 
              ;; rather than not having any inflection.
              (map-function-on-map-vals 
               (fn [k vals]
                 (filter #(or (not (= :verb (get-in % [:synsem :cat])))
                              (not (= :none (get-in % [:synsem :infl] :none))))
                         vals))))))
(def small
  (future
    (let [grammar
          (filter #(or (= (:rule %) "s-conditional-nonphrasal")
                       (= (:rule %) "s-present-nonphrasal")
                       (= (:rule %) "s-future-nonphrasal")
                       (= (:rule %) "s-imperfetto-nonphrasal")
                       (= (:rule %) "s-preterito-nonphrasal")
                       (= (:rule %) "s-aux")
                       (= (:rule %) "vp-aux"))
                  grammar)
          lexicon
          (into {}
                (for [[k v] @lexicon]
                  (let [filtered-v
                        (filter #(or (= (get-in % [:synsem :cat]) :verb)
                                     (= (get-in % [:synsem :propernoun]) true)
                                     (= (get-in % [:synsem :pronoun]) true))
                                v)]
                    (if (not (empty? filtered-v))
                      [k filtered-v]))))]
      {:name "small"
       :language "es"
       :language-keyword :espanol
       :enrich enrich
       :grammar grammar
       :lexicon lexicon
       :morph fo
       :index (create-index grammar (flatten (vals lexicon)) head-principle)})))

(def medium
  (future
    (let [lexicon
          (into {}
                (for [[k v] @lexicon]
                  (let [filtered-v v]
                    (if (not (empty? filtered-v))
                      [k filtered-v]))))]
      {:name "medium"
       :enrich enrich
       :morph fo
       :grammar grammar
       :lexicon lexicon
       :index (create-index grammar (flatten (vals lexicon)) head-principle)
       })))

(defn todos [ & [count]]
  (let [count (if count (Integer. count) 10)]
    (write-lexicon "es" @lexicon)
    (let [
          ;; subset of the lexicon: only verbs which are infinitives and that can be roots:
          ;; (i.e. those that have a specific (non- :top) value for [:synsem :sem :pred])
          root-verbs 
          (zipmap
           (keys @lexicon)
           (map (fn [lexeme-set]
                  (filter (fn [lexeme]
                            (and
                             (= (get-in lexeme [:synsem :cat]) :verb)
                             (= (get-in lexeme [:synsem :infl]) :top)
                             (not (= :top (get-in lexeme [:synsem :sem :pred] :top)))))
                          lexeme-set))
                (vals @lexicon)))

          todos
          (reduce concat
                  (map (fn [key]
                         (get root-verbs key))
                       (sort (keys root-verbs))))]
    (write-lexicon "es" @lexicon)
    (log/info (str "done writing lexicon."))
    (log/info (str "generating examples with this many verbs:"
                   (.size todos)))

    (.size (pmap (fn [verb]
                   (log/trace (str "verb: " (strip-refs verb)))
                   (let [root-form (get-in verb [:espanol :espanol])]
                     (log/info (str "generating with verb: '" root-form "'"))
                     (.size (map (fn [tense]
                                   (let [spec (unify {:root {:espanol {:espanol root-form}}}
                                                     tense)]
                                     (.size
                                      (map (fn [gender]
                                             (let [spec (unify spec
                                                               {:comp {:synsem {:agr gender}}})]
                                               (log/trace (str "generating from gender: " gender))
                                               (.size
                                                (map (fn [person]
                                                       (let [spec (unify spec
                                                                         {:comp {:synsem {:agr {:person person}}}})]
                                                         (log/trace (str "generating from person: " person))
                                                         (.size
                                                          (map (fn [number]
                                                                 (let [spec (unify spec
                                                                                   {:comp {:synsem {:agr {:number number}}}})]
                                                                   (log/debug (str "generating from spec: " spec))
                                                                   (try
                                                                     (process [{:fill-one-language
                                                                                {:count 1
                                                                                 :spec spec
                                                                                 :model small
                                                                                 }}]
                                                                              "es")
                                                                     (catch Exception e
                                                                       (cond
                                                                        
                                                                        ;; TODO: make this conditional on
                                                                        ;; there being a legitimate reason for the exception -
                                                                        ;; e.g. the verb is "funzionare" (which takes a non-human
                                                                        ;; subject), but we're trying to generate with
                                                                        ;; {:agr {:person :1st or :2nd}}, for which the only lexemes
                                                                        ;; are human.
                                                                        true
                                                                        
                                                                        (log/warn (str "ignoring exception: " e))
                                                                        false
                                                                       (throw e))))
                                                                   ))
                                                               [:sing :plur]))))
                                                     [:1st :2nd :3rd]))))
                                           (cond (= tense
                                                    {:synsem {:sem {:aspect :perfect
                                                                    :tense :past}}})
                                                 [{:gender :masc}
                                                  {:gender :fem}]
                                                 true
                                                 [:top])))))
                                 (list {:synsem {:sem {:tense :conditional}}}
                                       {:synsem {:sem {:tense :future}}}
                                       {:synsem {:sem {:tense :present}}}
                                       {:synsem {:sem {:aspect :perfect
                                                       :tense :past}}}
                                       )))))
                 todos)))))


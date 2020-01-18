(ns babylon.nederlands.cljs_support
  (:require-macros [babylon.morphology]
                   [babylon.grammar]
                   [babylon.lexiconfn])
  (:require [babylon.generate :as g]
            [babylon.grammar :as gra]
            [babylon.lexiconfn :as l]
            [babylon.morphology :as morph]
            [babylon.nederlands :as nl]
            [babylon.serialization :as s]
            [cljslog.core :as log]
            [dag_unify.core :as u]))

(defn generate [spec & [times]]
  (let [attempt
        (try
          (g/generate spec
                      nl/grammar
                      (fn [spec]
                        (shuffle (index-fn spec)))
                      nl/syntax-tree)
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
         :syntax-tree (nl/syntax-tree attempt)
         :surface (nl/morph attempt)})))

;; note that we exclude [:exception]s from the lexemes that we use for
;; generation since they are only to be used for parsing.
;; TODO: this is duplicated in babylon/nederlands.cljc (see def verb-lexicon).
(def lexeme-map
  {:verb (->> nl/lexicon
              (filter #(= :verb (u/get-in % [:cat])))
              (filter #(not (u/get-in % [:exception]))))
   :det (->> nl/lexicon
             (filter #(= :det (u/get-in % [:cat]))))
   :intensifier (->> lexicon
                     (filter #(= :intensifier (u/get-in % [:cat]))))
   :noun (->> nl/lexicon
              (filter #(= :noun (u/get-in % [:cat])))
              (filter #(not (u/get-in % [:exception]))))
   :top nl/lexicon
   :adjective (->> nl/lexicon
                   (filter #(= :adjective (u/get-in % [:cat]))))})

(defn index-fn [spec]
  ;; for now a somewhat bad index function: simply returns
  ;; lexemes which match the spec's :cat, or, if the :cat isn't
  ;; defined, just return all the lexemes.
  (let [result (get lexeme-map (u/get-in spec [:cat] :top) nil)]
    (if (not (nil? result))
        (shuffle result)
        (do
          (log/warn (str "no entry from cat: " (u/get-in spec [:cat] ::none) " in lexeme-map: returning all lexemes."))
          lexicon))))

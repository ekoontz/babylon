(ns babylon.nederlands.cljs_support
  (:require-macros [babylon.nederlands])
  (:require [babylon.nederlands :as nl]
            [babylon.serialization :as s]
            [cljslog.core :as log]
            [dag_unify.core :as u]))
(def lexicon-atom (atom nil))

(defn lexicon []
  (if (nil? @lexicon-atom)
    (do (swap! lexicon-atom
               (fn []
                 (-> (nl/read-compiled-lexicon)
                     babylon.lexiconfn/deserialize-lexicon              
                     vals
                     flatten)))
        @lexicon-atom)
    @lexicon-atom))

(def lexeme-map-atom (atom nil))

;; note that we exclude [:exception]s from the lexemes that we use for
;; generation since they are only to be used for parsing.
;; TODO: this is duplicated in babylon/nederlands.cljc (see def verb-lexicon).
(defn lexeme-map []
    (log/info (str "inside the lexeme-map function...(in babylon (cljs_support))!!"))
    (if (nil? @lexeme-map-atom)
      (do (swap! lexeme-map-atom
                 (fn []
                   {:verb (->> (lexicon)
                               (filter #(= :verb (u/get-in % [:cat])))
                               (filter #(not (u/get-in % [:exception]))))
                    :det (->> (lexicon)
                              (filter #(= :det (u/get-in % [:cat]))))
                    :intensifier (->> (lexicon)
                                      (filter #(= :intensifier (u/get-in % [:cat]))))
                    :noun (->> (lexicon)
                               (filter #(= :noun (u/get-in % [:cat])))
                               (filter #(not (u/get-in % [:exception]))))
                    :top (lexicon)
                    :adjective (->> (lexicon)                                                          
                                    (filter #(= :adjective (u/get-in % [:cat]))))})))
      @lexeme-map-atom))

(defn index-fn [spec]
  ;; for now a somewhat bad index function: simply returns
  ;; lexemes which match the spec's :cat, or, if the :cat isn't
  ;; defined, just return all the lexemes.
  (log/info (str "inside the index-fn function...(in babylon (cljs_support))!!"))
  (let [result (get (lexeme-map) (u/get-in spec [:cat] :top) nil)]
    (if (not (nil? result))
        (shuffle result)
        (do
          (log/info (str "no entry from cat: " (u/get-in spec [:cat] ::none) " in lexeme-map: returning all lexemes."))
          (lexicon)))))

(declare morphology)

(defn morph
  ([tree]
   (cond
     (map? (u/get-in tree [:syntax-tree]))
     (s/morph (u/get-in tree [:syntax-tree]) (morphology))

     true
     (s/morph tree (morphology))))

  ([tree & {:keys [sentence-punctuation?]}]
   (if sentence-punctuation?
     (-> tree
         morph
         (nl/sentence-punctuation (u/get-in tree [:sem :mood] :decl))))))

(def morphology-atom (atom nil))

(defn morphology []
  (or @morphology-atom
      (do (swap! morphology-atom (fn [] (nl/compile-morphology)))
          @morphology-atom)))

(def grammar-atom (atom nil))
(def expressions-atom (atom nil))

(defn grammar []
  (->> (nl/read-compiled-grammar)
       (map dag_unify.serialization/deserialize)))

(def expressions-atom (atom nil))

(defn expressions []
  (or @expressions-atom
      (do (swap! expressions-atom (fn [] (nl/read-expressions))))))

(defn syntax-tree [tree]
  (s/syntax-tree tree (morphology)))


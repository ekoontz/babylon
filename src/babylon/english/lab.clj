(ns babylon.english.lab
  (:require
   [babylon.english :as en :refer [analyze generate grammar morph parse syntax-tree]]
   [babylon.generate :as g]
   [dag_unify.core :as u :refer [unify]]
   [clojure.tools.logging :as log]))

(def specs
  [{:phrasal true
    :rule "np"
    :head {:rule "nbar4"
           :phrasal true
           :comp {:phrasal true
                  :comp {:phrasal true
                         :comp {:phrasal true}}}}}
   {:phrasal true
    :rule "s"
    :comp {:phrasal true
           :agr {:number :sing}
           :sem {:pred :dog}}
    :canonical "be"}])

(defn gen
  "how to generate a phrase with particular constraints."
  [i]
  (let [expression (generate (nth specs i))]
      {:st (syntax-tree expression)
       :morph (morph expression)
       :agr (u/strip-refs (u/get-in expression [:head :agr]))
       ;;       :parses (map syntax-tree (parse (morph expression)))
       :phrase? (u/get-in expression [:head :comp :phrasal])}))

(def specific-sentence-spec
  [{:cat :verb
    :subcat []
    :pred :top
    :comp {:phrasal true
           :rule "np"
           :agr {:number :sing
                 :person :3rd}
           :head {:phrasal true
                  :rule "nbar2"
                  :head {:canonical "dog"}}}
    :head {:phrasal false
           :canonical "be"}}])

(def poetry-specs
  [
   {:cat :verb
    :phrasal true
    :subcat []
    :comp {:phrasal false}
    :head {:phrasal true
           :comp {:phrasal true}}}])

(defn poetry-line []
  (try
    (->
     poetry-specs
     shuffle
     first
     generate)
    (catch Exception e
      (log/warn (str "fail:(" (-> e ex-data :why) ":)"
                     (syntax-tree (:tree (ex-data e))) " with spec:"
                     (u/strip-refs (:child-spec (ex-data e))) "; at path:"
                     (:frontier-path (ex-data e)) "; immediate-parent: "
                     (-> e ex-data :immediate-parent))))))

(defn benchmark []
  (repeatedly
   #(time (->
           (or (poetry-line) "(failed)")
           (morph :sentence-punctuation? true)
           println))))

(defn poetry []
  (loop []
    (println (morph (or (poetry-line) "(failed)") :sentence-punctuation? true))
    (recur)))

(defn long-s []
  (count
   (take 1
    (repeatedly
      #(println
         (morph
          (generate
           {:cat :verb
            :subcat []
            :pred :top
            :comp {:phrasal true
                   :head {:phrasal true}}
            :head {:phrasal true
                   :comp {:phrasal true
                          :head {:phrasal true}}}})))))))

(defn set-started [tree path]
  (if (not (empty? path))
    (set-started
     (u/assoc-in tree (concat path [:babylon.generate/started?]) true)
     (rest path))
    (u/assoc-in tree [:babylon.generate/started?] true)))

(defn set-done [tree path]
  (if (and (not (empty? path))
           (= :comp (last path)))
      (set-done
       (u/assoc-in tree (concat path [:babylon.generate/done?]) true)
       (butlast path))
    (u/assoc-in tree (concat path [:babylon.generate/done?]) true)))

(defn add-rule-at [tree rule-name at]
  (->> grammar
       (filter #(= (:rule %) rule-name))
       (map #(u/assoc-in tree at %))
       (map #(set-started % at))))

(defn add-lexeme-at [tree surface at]
  (->> (analyze surface)
       (map (fn [lexeme]
              (log/debug (str "lexeme: " (u/fail-path (u/get-in tree at)
                                                      lexeme)))
              (u/assoc-in tree at lexeme)))
       (map #(set-done % at))))

(def skels
  (->> grammar
       (filter #(= (:rule %) "s"))
       (mapcat #(add-rule-at % "vp-aux" [:head]))
       (mapcat #(add-lexeme-at % "would" [:head :head]))
       (mapcat #(add-rule-at % "vp" [:head :comp]))
       (mapcat #(add-lexeme-at % "see" [:head :comp :head]))
       (filter #(not (= % :fail)))))

(def skel2
  (->> grammar
       (filter #(= (:rule %) "s"))
       (mapcat #(add-rule-at % "vp-aux" [:head]))
       (mapcat #(add-lexeme-at % "would" [:head :head]))
       (mapcat #(add-rule-at % "vp" [:head :comp]))
       (mapcat #(add-lexeme-at % "see" [:head :comp :head]))
       (mapcat #(add-lexeme-at % "her" [:head :comp :comp]))
       (filter #(not (= % :fail)))
       first))

(defn fold-up [tree path]
  (let [tree
        (-> (u/get-in tree path) ;; <- descend tree to the subtree to be operated on.
            (dissoc :dag_unify.serialization/serialized))]
    (swap! (get tree :head)
           (fn [x]
             {:surface (str
                        (clojure.string/join " " [(morph (u/get-in tree [:head]))
                                                  (morph (u/get-in tree [:comp :head]))])
                        " ..")
              :babylon.generate/done? true
              :syntax-tree (syntax-tree (u/get-in tree [:head]))
              :sem (u/get-in tree [:head :sem])
              :subcat {:1 (u/get-in tree [:head :subcat :1])
                       :2 (u/get-in tree [:comp :head :subcat :2])
                       :3 []}}))
    (swap! (get tree :comp)
           (fn [x] (u/get-in tree [:comp :comp])))))

(defn do-it [skels]
  (loop [skels skels]
    (if (not (empty? skels))
      (fold-up (first skels) [:head])
      (recur (rest skels))))

  (first skels))

;;    (if false
;;      (->>
;;       (-> skel
;;           (add-lexeme-at "her" [:head :comp])
;;       (filter #(not (= % :fail)))
;;       first)))

;;(def skel (do-it skels)

;; BUG: this fails:
;; (u/assoc-in skel [:head :comp] (first (analyze "her")))

;; also this:
;; (unify skel (create-path-in [:head :comp] (first (analyze "her"))))

;; also this:
;; (def u1 (u/copy skel))
;; (def u2 (u/copy (dag_unify.serialization/create-path-in [:head :comp] (first (analyze "her")))))
;; (unify u1 u2)

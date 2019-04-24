(ns babylon.english.lab
  (:require
   [babylon.english :as en :refer [analyze generate grammar morph parse syntax-tree]]
   [babylon.generate :as g]
   [dag_unify.core :as u :refer [unify fail? ref? simplify-ref]]
   [dag_unify.serialization :as s :refer [all-refs]]
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
  (log/debug (str "adding rule: " rule-name " at: " at))
  (->> grammar
       (filter #(= (:rule %) rule-name))
       shuffle
       (g/eugenes-map #(u/assoc-in tree at %))
       (g/eugenes-map #(set-started % at))))

(def flattened-lexicon
  (->>
   babylon.english/lexicon
   vals
   flatten
   (filter #(not (u/get-in % [:exception])))))

(defn add-with-spec [tree & [spec]]
  (let [at (g/frontier tree)
        spec (or spec :top)
        spec (unify spec (u/get-in tree at))]
    (if (= spec :fail)
      []
      (do
        (log/debug (str "spec: " (u/strip-refs spec)))
        (->> flattened-lexicon
             (remove #(= :fail (unify % spec)))
             shuffle
             (g/eugenes-map #(u/assoc-in tree at %))
             (map #(set-done % at)))))))

(defn fold-up [tree at]
  (let [subtree (u/get-in tree at)]
    (log/debug (str "swap head.."))
    (swap! (get subtree :head)
           (fn [head]
             {:surface (str
                        (clojure.string/join " " [(morph head)
                                                  (morph (u/get-in subtree [:comp :head]))])
                        " ..")
              :babylon.generate/done? true
              ;; TODO: needs to be sensitive to the orderedness of the children:
              ;; currently assumes that head is first (:head == :1,:comp == :2)
              ;; but need to also check and handle (:comp == :1,:head == :2)
              :syntax-tree (str (syntax-tree head)
                                " .["
                                (u/get-in subtree [:comp :rule])
                                " "
                                "*^"
                                (syntax-tree (u/get-in subtree [:comp :head]))
                                " ")

              :sem (u/get-in head [:sem])
              :subcat {:1 (u/get-in head [:subcat :1])
                       :2 (u/get-in subtree [:comp :head :subcat :2])
                       :3 []}}))
    (log/debug (str "swap comp.."))
    (swap! (get subtree :comp)
           (fn [comp] (u/get-in comp [:comp])))
    ;; we have to reach into the internals of dag_unify to
    ;; to remove the now-invalid cached serialization of
    ;; each tree:
    (dissoc tree :dag_unify.serialization/serialized)))

(defn working-example []
  (->>
   ;; 1. build s->vp-aux->aux-verb:
   ;;
   ;;    s
   ;;   / \ 
   ;;  /   \ H
   ;;  _    vp-aux(new)
   ;;      /   
   ;;     / H   
   ;;   would(new)
   ;;
   grammar
   (filter #(= (:rule %) "s"))
   shuffle
   (g/eugenes-map #(set-started % []))
   (g/lazy-mapcat #(add-rule-at % "vp-aux" (g/frontier %)))
   (g/lazy-mapcat #(add-with-spec % {:canonical "would"}))

   ;; 2. add vp->verb:
   ;;
   ;;    s
   ;;   / \ 
   ;;  /   \ H
   ;;  _    vp-aux
   ;;      /   \
   ;;     / H   vp(new)
   ;;   would  / \
   ;;         /   \
   ;;      H /     \
   ;;      see      _
   ;;
   (g/lazy-mapcat #(add-rule-at % "vp" (g/frontier %)))
   (g/lazy-mapcat add-with-spec)
   
   ;; 3. fold up tree from the above representation to:
   ;;    s
   ;;   / \
   ;;  /   \ H
   ;;  _    vp-aux
   ;;      /      \
   ;;     / H      \
   ;;    would see  _
   ;;
   (remove #(= % :fail))
   (g/eugenes-map #(fold-up % [:head]))

   ;; 4. add complement at path [:head :comp]:
   ;;    s
   ;;   / \
   ;;  /   \ H
   ;;  _     vp-aux
   ;;       /      \
   ;;      / H      \
   ;;    would see   <new>
   ;;
   (g/lazy-mapcat #(add-with-spec % {:canonical "themselves"}))


   ;; 5. add complement at path [:comp]:
   ;;     s
   ;;   /   \
   ;;  /     \ H
   ;;  <new>  vp-aux
   ;;        /     \
   ;;       / H     \
   ;;    would see   herself
   ;;
   (g/lazy-mapcat #(add-with-spec % {:canonical "you"}))
   (remove #(= % :fail))))

(defn demo []
  (repeatedly #(println (syntax-tree (time (first (working-example)))))))



                                

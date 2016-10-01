(ns babel.over
  (:refer-clojure :exclude [get get-in resolve find parents])
  (:require
   [babel.exception :refer [exception]]
   [babel.lexiconfn :refer [get-fail-path]]
   [clojure.string :as string]
   #?(:clj [clojure.tools.logging :as log])
   #?(:cljs [babel.logjs :as log]) 
   [dag_unify.core :refer [copy fail? fail-path fail-path-between get-in strip-refs unify unifyc
                           ;; temporary: until we move (truncate) from here to dag_unify.
                           deserialize dissoc-paths serialize
]]))

;; TODO: need better debugging throughout this file to diagnose generation failures.
;; using (get-fail-path) is one example.

;; use map or pmap.
(def ^:const mapfn pmap)

(def ^:dynamic *extra-diagnostics* false)

(defn overh
  "add given head as the head child of the phrase: parent."
  [parent head]
  (when (and (map? parent)
             (map? head))
    (do
      (log/trace (str "overh: parent: " (get-in parent [:rule])))
      (log/trace (str "overh: head: " (get-in head [:rule] (str "head is a lexeme with pred: " (strip-refs (get-in head [:synsem :sem :pred]))))))
      (log/trace (str "overh: parent: " parent))
      (log/trace (str "overh: head: " head))))
  ;; TODO: get rid of all this type-checking and use
  ;; whatever people use for Clojure argument type-checking.
  (cond

   (nil? head)
   nil

   (or
    (seq? parent)
    (set? parent)
    (vector? parent))
   (let [parents (lazy-seq parent)]
     (filter (fn [result]
               (not (fail? result)))
             (mapcat (fn [each-parent]
                       (overh each-parent head))
                     parents)))

   (or (set? head)
       (vector? head))
   (do (log/trace "head is a set: converting to a seq.")
       (overh parent (lazy-seq head)))

   (seq? head)
   (let [head-children head]
     (log/trace (str "head is a seq - actual type is " (type head)))
     (filter (fn [result]
               (not (fail? result)))
             (mapcat (fn [child]
                       (overh parent child))
                     head-children)))
   true
   ;; TODO: 'true' here assumes that both parent and head are maps: make this assumption explicit,
   ;; and save 'true' for errors.
   (let [result (unify (copy parent)
                       {:head (copy head)})

         is-fail? (fail? result)
         label (if (get-in parent [:rule]) (get-in parent [:rule]) (:comment parent))]
     (if (not is-fail?)
       (do
         (log/debug (str "overh: " (get-in parent [:rule]) " -> " (get-in head [:rule]
                                                                        (get-in head [:synsem :sem :pred]
                                                                                "(no pred for head)"))))
         (log/trace (str "overh successful result: " (strip-refs (dissoc result :dag_unify.core/serialized))))
         (list result))))))

;; Haskell-looking signature:
;; (parent:map) X (child:{set,seq,fs}) => list:map
;; TODO: verify that the above comment about the signature
;; is still true.
(defn overc [parent comp]
  "add given child as the comp child of the phrase: parent."

  (log/trace (str "set? parent:" (set? parent)))
  (log/trace (str "seq? parent:" (seq? parent)))
  (log/trace (str "seq? comp:" (seq? comp)))

  (log/trace (str "type of parent: " (type parent)))
  (log/trace (str "type of comp  : " (type comp)))
  (log/trace (str "nil? comp  : " (nil? comp)))

  (cond
   (nil? comp) nil

   (or
    (seq? parent)
    (set? parent)
    (vector? parent))
   (let [parents (lazy-seq parent)]
     (filter (fn [result]
               (not (fail? result)))
             (mapcat (fn [parent]
                       (overc parent comp))
                     parents)))

   #?(:clj (future? comp))
   #?(:clj (overc parent (deref comp)))

   (or (set? comp)
       (vector? comp))
   (do (log/trace "comp is a set: converting to a seq.")
       (overc parent (lazy-seq comp)))

   (seq? comp)
   (let [comp-children comp]
     (log/trace (str "comp is a seq - actual type is " (type comp)))
     (filter (fn [result]
               (not (fail? result)))
             (mapcat (fn [child]
                       (overc parent child))
                     comp-children)))
   true
   (let [result (unifyc parent
                        {:comp comp})
         is-fail? (= :fail result)]
     (if (not is-fail?)
       (do
         (log/debug (str "overc: " (get-in parent [:rule]) " -> " (get-in comp [:rule]
                                                                          (get-in comp [:synsem :sem :pred]
                                                                                  "(no pred for comp)"))))
         ;; TODO: why are we returning a list here rather than just the result?
         (list result))))))

(defn overhc [parent head comp]
  (-> parent
      (overh head)
      (overc comp)))

;; TODO: distinguish between when:
;; 1) called with only a child1 (no child2),
;; 2) called with both a child1 and a child2, but child2's supplied value is nil:
;;    should be treated the same as empty list.
(defn over [parents child1 & [child2]]
  (log/trace (str "over:" (count parents)))
  (cond (vector? child1)
        (over parents (seq child1) child2)

        (vector? child2)
        (over parents child1 (seq child2))

        (map? parents)
        (do
          (log/trace (str "parents is a map: converting to a list and re-calling."))
          (over (list parents) child1 child2))

        (nil? child2)
        (over parents child1 :top)        

        (empty? parents)
        nil

        (and (map? parents)
             (not (nil? (:dag_unify.core/serialized parents))))
        ;; In this case, supposed 'parent' is really a lexical item: for now, definition of 'lexical item' is,
        ;; it has a non-nil value for :dag_unify.core/serialized - just return nil, nothing else to do.
        ;; TODO: above test of 'lexical item' is not right: a parent might very well have a :serialized key.
        (throw (exception (str "Don't know what to do with this parent: " parents)))

        ;; if parent is a symbol, evaluate it; should evaluate to a list of expansions (which might also be symbols, etc).
        #?(:clj (symbol? parents))
        #?(:clj (over (eval parents) child1 child2))

        true
        (let [parent (first parents)] ;; TODO: use recur
          (cond
            (nil? (get-in parent [:schema-symbol] nil))
            (throw (exception (str "no schema symbol for parent: " (:rule parent))))

            (nil? (:first parent))
            (throw (exception (str "no :first key for parent: " (:rule parent) " : should be set to either :head or :comp.")))

            (and (not (= :head (:first parent)))
                 (not (= :comp (:first parent))))
            (throw (exception (str ":first key for parent: " (:rule parent) " : must be either :head or :comp, but it was: " (:first parent))))
            
            true
            (let [[head comp] (if (= (:first parent) :head)
                                [child1 child2]
                                [child2 child1])]
              (log/trace (str "over: parent: " (get-in parent [:rule]) " (" (get-in parent [:schema-symbol]) "); heads:["
                              (string/join ","
                                           (map (fn [h]
                                                  (get-in h [:rule]
                                                          (str (get-in h [:synsem :sem :pred]))))
                                                head))
                              "]"))
              (if (= (:first parent) :head)
                ;; else, head is left child.
                (do (log/trace "over: left child (head):" (strip-refs child1))
                    (log/trace "over: right child (comp):" (strip-refs child2)))
                ;; else, head is right child.
                (do (log/trace "over: left child (comp):" (strip-refs child1))
                    (log/trace "over: right child (head):" (strip-refs child2))))

              (concat
               ;; if parent is map, do introspection: figure out the schema from the :schema-symbol attribute,
               ;; and figure out head-comp ordering from :first attribute.
               (filter (fn [each]
                         (not (fail? each)))
                       (overhc parent
                               (if (= (:first parent) :head)
                                 child1 child2)
                               (if (= (:first parent) :head)
                                 child2 child1)))
               (over (rest parents) child1 child2)))))))

(defn morph-with-recovery [morph-fn input]
  (if (nil? input)
    (exception (str "don't call morph-with-recovery with input=nil.")))
  (if (nil? morph-fn)
    (exception (str "don't call morph-with-recovery with morph-fn=nil.")))
  (let [result (morph-fn input)
        result (if (or (nil? result)
                       (= "" result))
                 (get-in input [:english :english] "")
                 result)
        result (if (or (nil? result)
                       (= "" result))
                 (get-in input [:english] "")
                 result)
        result (if (or (nil? result)
                       (= "" result))
                 (get-in input [:rule] "")
                 result)
        result (if (or (nil? result)
                       (= "" result))
                 (exception
                  (str "r5: " input "/" (nil? input)))
                 result)]
    result))

(defn show-bolt [bolt language-model]
  (if (nil? bolt)
    (exception (str "don't call show-bolt with bolt=null."))
    (let [morph (:morph language-model)]
      (if (nil? morph)
        (exception (str "don't call show-bolt with morph=null."))
        (str "[" (get-in bolt [:rule])
             " '" (morph-with-recovery morph bolt) "'"
             (let [head-bolt (get-in bolt [:head])]
               (if (not (nil? head-bolt))
                 (let [rest-str (show-bolt (get-in bolt [:head]) language-model)]
                   (if (not (nil? rest-str))
                     (str " -> " rest-str)))))
             "]")))))

(defn subpath? [path1 path2]
  "return true if path1 is subpath of path2."
  (if (empty? path1)
    true
    (if (= (first path1) (first path2))
      (subpath? (rest path1)
                (rest path2))
      false)))

(defn truncate [input truncate-paths language-model]
  (log/debug (str "truncating@" truncate-paths ":" (show-bolt input language-model)))
  (let [serialized (if (:dag_unify.core/serialized input)
                     (:dag_unify.core/serialized input)
                     (serialize input))
        paths-and-vals (rest serialized)
        path-sets (mapfn first paths-and-vals)
        path-vals (mapfn second paths-and-vals)
        truncated-path-sets (mapfn
                             (fn [path-set] 
                               (filter (fn [path] 
                                         (not (some (fn [truncate-path]
                                                      (subpath? truncate-path path))
                                                    truncate-paths)))
                                       path-set))
                             path-sets)
        skeleton (first serialized)
        truncated-skeleton (dissoc-paths skeleton truncate-paths)
        truncated-serialized
        (cons truncated-skeleton
              (zipmap truncated-path-sets
                      path-vals))]
    (deserialize truncated-serialized)))

(defn truncate-expressions [expressions truncate-paths language-model]
  (map #(truncate % truncate-paths language-model)
       expressions))

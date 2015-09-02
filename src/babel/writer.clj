(ns babel.writer
  (:refer-clojure :exclude [get-in merge])
  (:require
    [babel.lexiconfn :refer [sem-impl]]
    [clojure.data.json :as json]
    [clojure.string :as string]
    [clojure.tools.logging :as log]
    [dag-unify.core :refer [get-in strip-refs serialize unify ref?]]
    [babel.engine :as engine]
    [babel.korma :as korma]
    [korma.core :as k]))

(defn truncate [ & [table]]
  (let [table (if table table "expression")]
;; doesn't work:
;;    (k/exec-raw ["TRUNCATE ?" [table]])))
   (k/exec-raw [(str "TRUNCATE " table)])))
;; catch exceptions when trying to populate
;; TODO: more fine-grained approach to dealing with exceptions:
;; should be sensitive to what caused the failure:
;; (which language,lexicon,grammar,etc..).
(def mask-populate-errors true)

(defn expression-pair [source-language-model target-language-model spec]
  "Generate a pair: {:source <source_expression :target <target_expression>}.

   First, the target expression is generated according to the given target-language-model and spec.

   Then, the source expression is generated according to the semantics of this target expression.

   Thus the source expression's spec is not specified directly - rather it is derived from the 
   semantics of the target expression."

  (let [no-source-language (if (nil? source-language-model)
                             (throw (Exception. "No source language model was supplied.")))
        no-target-language (if (nil? target-language-model)
                             (throw (Exception. "No target language model was supplied.")))

        target-language-sentence (engine/generate spec target-language-model :enrich true)

        target-language-sentence (let [subj (get-in target-language-sentence
                                                    [:synsem :sem :subj] :notfound)]
                                   (cond (not (= :notfound subj))
                                         (do
                                           (log/debug (str "subject constraints: " subj))
                                           (unify target-language-sentence
                                                  {:synsem {:sem {:subj subj}}}))
                                         true
                                         target-language-sentence))
        source-fo (:morph @source-language-model)
        target-fo (:morph @target-language-model)

        ;; TODO: add warning if semantics of target-expression is merely :top - it's
        ;; probably not what the caller expected. Rather it should be something more
        ;; specific, like {:pred :eat :subj {:pred :antonio}} ..etc.
        source-language-sentence (engine/generate (get-in target-language-sentence [:synsem :sem] :top)
                                                  source-language-model :enrich true)

        semantics (strip-refs (get-in target-language-sentence [:synsem :sem] :top))
        debug (log/debug (str "semantics: " semantics))

        target-language-surface (target-fo target-language-sentence)
        debug (log/debug (str "target surface: " target-language-surface))

        source-language (:language (if (future? source-language-model)
                                     @source-language-model
                                     source-language-model))
        target-language (:language (if (future? target-language-model)
                                     @target-language-model
                                     target-language-model))
        error (if (or (nil? target-language-surface)
                      (= target-language-surface ""))
                (let [message (str "Could not generate a sentence in target language '" target-language 
                                   "' for this spec: " spec)]
                  (if (= true mask-populate-errors)
                    (log/warn message)
                    ;; else
                    (throw (Exception. message)))))
        source-language-sentence (engine/generate {:synsem {:sem semantics
                                                            :subcat '()}}
                                                  source-language-model
                                                  :enrich true)
        source-language-surface (source-fo source-language-sentence)
        debug (log/debug (str "source surface: " source-language-surface))

        error (if (or (nil? source-language-surface)
                      (= source-language-surface ""))
                (let [message (str "Could not generate a sentence in source language '" source-language 
                                   "' for this semantics: " semantics "; target language was: " target-language 
                                   "; target expression was: '" ((:morph  @target-language-model) target-language-sentence) "'")]
                  (if (= true mask-populate-errors)
                    (log/warn message)
                    ;; else
                    (throw (Exception. message)))))]
    {:source source-language-sentence
     :target target-language-sentence}))

(defn insert-expression [expression surface table language model-name]
  "Insert an expression into the given table. The information to be added will be:
   1. the surface form of the expression
   2. The JSON representation of the expression (the structure)
   3. the short name of the language (e.g. 'en','es', or 'it')
   4. the serialized form of the structure

   The structure column allows JSON queries using the containment operator (@>) to 
   find expressions matching a desired specification. See reader.clj for how these
   queries are done.

   The serialized column allows loading a desired expression as a Clojure map into the runtime system, including
   the expression' internal structure-sharing."

  (k/exec-raw [(str "INSERT INTO " table " (surface, structure, serialized, language, model) VALUES (?,"
                    "'" (json/write-str (strip-refs expression)) "'"
                    ","
                    "'" (str (serialize expression)) "'"
                    ","
                    "?,?)")
               [surface
                language
                model-name]]))

(defn insert-lexeme [canonical lexeme language]
  (k/exec-raw [(str "INSERT INTO lexeme 
                                 (canonical, structure, serialized, language) 
                          VALUES (?,"
                    "'" (json/write-str (strip-refs lexeme)) "'"
                    ",?,?)")
               [canonical (str (serialize lexeme))
                language]]))

;; TODO: use named optional parameters.
(defn populate [num source-language-model target-language-model & [ spec table ]]
  (let [spec (if spec spec :top)
        debug (log/debug (str "populate spec(1): " spec))
        debug (log/debug (str "type of source language model: " (type source-language-model)))
        debug (log/debug (str "type of target language model: " (type target-language-model)))
        spec (cond
              (not (= :notfound (get-in spec [:synsem :sem :subj] :notfound)))
              (unify spec
                     {:synsem {:sem {:subj (sem-impl (get-in spec [:synsem :sem :subj]))}}})
              true
              spec)

        debug (log/debug (str "populate spec(2): " spec))

        ;; subcat is empty, so that this is a complete expression with no missing arguments.
        ;; e.g. "she sleeps" rather than "sleeps".
        spec (unify spec {:synsem {:subcat '()}}) 

        debug (log/debug (str "populate spec(3): " spec))

        table (if table table "expression")]
    (dotimes [n num]
      (let [sentence-pair (expression-pair source-language-model target-language-model spec)
            target-sentence (:target sentence-pair)
            source-sentence (:source sentence-pair)
            source-language-model (if (ref? source-language-model) @source-language-model source-language-model)
            target-language-model (if (ref? target-language-model) @target-language-model target-language-model)

            target-morphology (:morph @target-language-model)
            source-morphology (:morph @source-language-model)

            source-sentence-surface (source-morphology source-sentence)
            target-sentence-surface (target-morphology target-sentence)

            source-language (:language @source-language-model)
            target-language (:language @target-language-model)

            ]

        (log/info (str target-language ": '"
                        target-sentence-surface
                        "'"))
        (insert-expression target-sentence ;; underlying structure
                           target-sentence-surface ; surface string
                           "expression" ;; table
                           target-language
                           (:name target-language-model)) ;; name, like 'small','medium', etc.
        
        (log/info (str source-language ": '"
                       source-sentence-surface
                        "'"))
        (insert-expression source-sentence
                           source-sentence-surface
                           "expression"
                           source-language
                           (:name source-language-model))))))

(defn fill [num source-lm target-lm & [spec]]
  "wipe out current table and replace with (populate num spec)"
  (truncate)
  (let [spec (if spec spec :top)]
    (populate num source-lm target-lm spec)))

(defn populate-from [source-language-model target-language-model target-lex-set target-grammar-set]
  "choose one random member from target-lex-set and one from target-grammar-set, and populate from this pair"
  (let [lex-spec (nth target-lex-set (rand-int (.size target-lex-set)))
        grammar-spec (nth target-grammar-set (rand-int (.size target-grammar-set)))]
    (populate 1 source-language-model target-language-model (unify lex-spec grammar-spec))))

(defn fill-by-spec [spec count table source-model target-model]
  (if (nil? source-model)
    (throw (Exception. "No source language model was supplied.")))
  (if (nil? target-model)
    (throw (Exception. "No target language model was supplied.")))
  (let [target-language (:language @target-model)
        json-spec (json/write-str spec)
        current-target-count
        (:count
         (first
          (k/exec-raw ["SELECT count(*) FROM expression 
                         WHERE structure @> ?::jsonb
                           AND language=?"
                       [(json/write-str spec) target-language]]
                      :results)))]
    (log/debug (str "current-target-count for spec: " spec "=" current-target-count))
    (let [count (- count current-target-count)]
      (if (> current-target-count 0)
        (log/warn (str "There are already " current-target-count " expressions for this spec.")))
      (if (> count 0)
        (do
          (log/info (str "Generating " count " target expressions."))
          (populate count
                    source-model
                    target-model
                    spec table))
        (log/warn (str " not generating any new expressions for this spec."))))))

(defn fill-verb [verb count source-model target-model & [spec table]] ;; spec is for additional constraints on generation.
  (let [spec (if spec spec :top)
        target-language-keyword (:language-keyword @target-model)
        tenses [{:synsem {:sem {:tense :conditional}}}
                {:synsem {:sem {:tense :futuro}}}
                {:synsem {:sem {:tense :past :aspect :progressive}}}
                {:synsem {:sem {:tense :past :aspect :perfect}}}
                {:synsem {:sem {:tense :present}}}]]
    (let [spec (unify {:root {target-language-keyword {target-language-keyword verb}}}
                       spec)]
      (log/debug (str "fill-verb spec: " spec))
      (pmap (fn [tense] (fill-by-spec (unify spec
                                             tense)
                                      count
                                      table
                                      source-model
                                      target-model))
            tenses))))

(defn truncate-lexicon [language]
  (k/exec-raw [(str "DELETE FROM lexeme WHERE language=?")
               [language]]))

(defn write-lexicon [language lexicon]
  (truncate-lexicon language)
  (loop [canonical (sort (keys lexicon)) result nil]
    (if (not (empty? canonical))
      (let [canonical-form (first canonical)]
        (println (str language ":" canonical-form))
        (loop [lexemes (get lexicon canonical-form) inner-result nil]
          (if (not (empty? lexemes))
            (do
              (insert-lexeme canonical-form (first lexemes) language)
              (recur (rest lexemes) inner-result))
            inner-result))
        (recur (rest canonical) result))
      result)))

(defn -main [& args]
  (if (not (nil? (first args)))
    (populate (Integer. (first args)))
    (populate 100)))

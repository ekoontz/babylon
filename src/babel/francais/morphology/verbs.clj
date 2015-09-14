(ns babel.francais.morphology.verbs
  (:refer-clojure :exclude [get-in merge resolve]))
(require '[babel.stringutils :refer :all])
(require '[babel.francais.morphology.nouns :as nouns])
(require '[clojure.core :as core])
(require '[clojure.string :as string])
(require '[clojure.string :refer (trim)])
(require '[clojure.tools.logging :as log])
(require '[dag-unify.core :refer (copy dissoc-paths fail? get-in merge ref? strip-refs unifyc)])

(declare number-and-person)

(defn conditional [word]
  (let [infinitive (get-in word '(:français))
        ar-type (try (re-find #"ar$" infinitive)
                     (catch Exception e
                       (throw (Exception. (str "Can't regex-find on non-string: " infinitive " from word: " word)))))
        er-type (re-find #"[eé]r$" infinitive)
        ir-type (re-find #"ir$" infinitive)
        re-type (re-find #"re$" infinitive)
        stem (string/replace infinitive #"[iaeé]r$" "")
        stem (if re-type
               ;; prendre -> prend
               (string/replace infinitive #"re$" "")
               stem)
        last-stem-char-is-i (re-find #"ir$" infinitive)
        last-stem-char-is-e (re-find #"er$" infinitive)
        person (get-in word '(:agr :person))
        number (get-in word '(:agr :number))
        number-and-person (number-and-person number person)]
    (cond
     (get-in word [:conditional number-and-person])
     (get-in word [:conditional number-and-person])

     (and (= person :1st) (= number :sing) er-type)
     (str stem "erais")
     (and (= person :1st) (= number :sing) ir-type)
     (str stem "irais")
     (and (= person :1st) (= number :sing) re-type)
     (str stem "rais")

     (and (= person :2nd) (= number :sing) ir-type)
     (str stem "erais")
     (and (= person :2nd) (= number :sing) er-type)
     (str stem "irais")
     (and (= person :2nd) (= number :sing) re-type)
     (str stem "rais")
    
     (and (= person :3rd) (= number :sing) ir-type)
     (str stem "erait")
     (and (= person :3rd) (= number :sing) er-type)
     (str stem "irait")
     (and (= person :3rd) (= number :sing) re-type)
     (str stem "rait")
     
     (and (= person :1st) (= number :plur) er-type)
     (str stem "erions")
     (and (= person :1st) (= number :plur) ir-type)
     (str stem "irions")
     (and (= person :1st) (= number :plur) re-type)
     (str stem "rions")

     (and (= person :2nd) (= number :plur) er-type)
     (str stem "eriez")
     (and (= person :2nd) (= number :plur) ir-type)
     (str stem "iriez")
     (and (= person :2nd) (= number :plur) re-type)
     (str stem "riez")
     ;; </second person plural conditional>

     ;; <third person plural conditional>
     (and (= person :3rd) (= number :plur)
          er-type)
     (str stem "eraient")
     (and (= person :3rd) (= number :plur)
          ir-type)
     (str stem "iraient")
     (and (= person :3rd) (= number :plur)
          re-type)
     (str stem "raient")
     ;; </third person plural conditional>

     :else
     (throw (Exception. (str "get-string-1: conditional regular inflection: don't know what to do with input argument: " (strip-refs word)))))))

(defn future [word]
  (let [infinitive (get-in word '(:français))
        ar-type (try (re-find #"ar$" infinitive)
                     (catch Exception e
                       (throw (Exception. (str "Can't regex-find on non-string: " infinitive " from word: " word)))))
        er-type (re-find #"[eé]r$" infinitive)
        ir-type (re-find #"ir$" infinitive)
        re-type (re-find #"re$" infinitive)
        stem (string/replace infinitive #"[iaeé]r$" "")
        stem (if re-type
               ;; prendre -> prend
               (string/replace infinitive #"re$" "")
               stem)
        last-stem-char-is-i (re-find #"ir$" infinitive)
        last-stem-char-is-e (re-find #"er$" infinitive)
        person (get-in word '(:agr :person))
        number (get-in word '(:agr :number))
        number-and-person (number-and-person number person)]
    (cond
     (get-in word [:conditional number-and-person])
     (get-in word [:conditional number-and-person])
     
     (and (= person :1st) (= number :sing) er-type)
     (str stem "erai")
     (and (= person :1st) (= number :sing) ir-type)
     (str stem "iré")
     (and (= person :1st) (= number :sing) re-type)
     (str stem "rai")
     
     (and (= person :2nd) (= number :sing) ir-type)
     (str stem "iras")
     (and (= person :2nd) (= number :sing) er-type)
     (str stem "eras")
     (and (= person :2nd) (= number :sing) re-type)
     (str stem "ras")
                            
     (and (= person :3rd) (= number :sing) ir-type)
     (str stem "ira")
     (and (= person :3rd) (= number :sing) er-type)
     (str stem "era")
     (and (= person :3rd) (= number :sing) re-type)
     (str stem "ra")
              
     (and (= person :1st) (= number :plur) er-type)
     (str stem "erons")
     (and (= person :1st) (= number :plur) ir-type)
     (str stem "irons")
     (and (= person :1st) (= number :plur) re-type)
     (str stem "rons")
              
     ;; <second person plural future>
     (and (= person :2nd) (= number :plur) er-type)
     (str stem "erez")
     (and (= person :2nd) (= number :plur) ir-type)
     (str stem "irez")
     (and (= person :2nd) (= number :plur) re-type)
     (str stem "rez")
     ;; </second person plural future>

     ;; <third person plural future>
     (and (= person :3rd) (= number :plur)
          er-type)
     (str stem "eront")
     (and (= person :3rd) (= number :plur)
          ir-type)
     (str stem "iront")
     (and (= person :3rd) (= number :plur)
          re-type)
     (str stem "ront")
     ;; </third person plural future>
              
     :else
     (throw (Exception. (str "get-string: futuro regular inflection: don't know what to do with input argument: " (strip-refs word)))))))

(defn imperfect [word]           
  (let [infinitive (get-in word '(:français))
        ar-type (try (re-find #"ar$" infinitive)
                     (catch Exception e
                       (throw (Exception. (str "Can't regex-find on non-string: " infinitive " from word: " word)))))
        er-type (re-find #"er$" infinitive)
        ir-type (re-find #"ir$" infinitive)
        stem (if (get-in word [:imperfect-stem])
               (get-in word [:imperfect-stem])
               (string/replace infinitive #"[iae]r$" ""))
        last-stem-char-is-i (re-find #"ir$" infinitive)
        last-stem-char-is-e (re-find #"er$" infinitive)
        person (get-in word '(:agr :person))
        number (get-in word '(:agr :number))
        number-and-person (number-and-person number person)]
    (cond
     (get-in word [:imperfect number-and-person])
     (get-in word [:imperfect number-and-person])

     (and (= person :1st) (= number :sing) (or er-type stem))
     (str stem "ais")

     (and (= person :1st) (= number :sing) ir-type)
     (str stem "issais")

     (and (= person :2nd) (= number :sing) (or er-type stem))
     (str stem "ais")
           
     (and (= person :2nd) (= number :sing) ir-type)
     (str stem "issais")
              
     (and (= person :3rd) (= number :sing) (or er-type stem))
     (str stem "ait")
           
     (and (= person :3rd) (= number :sing) ir-type)
     (str stem "issait")
           
     (and (= person :1st) (= number :plur) (or er-type stem))
     (str stem "ions")
           
     (and (= person :1st) (= number :plur) ir-type)
     (str stem "issions")
           
     ;; <second person plural imperfecto>
           
     (and (= person :2nd) (= number :plur) (or er-type stem))
     (str stem "iez")
           
     (and (= person :2nd) (= number :plur) ir-type)
     (str stem "issiez")
                 
     ;; </second person plural imperfecto>
           
     ;; <third person plural imperfecto>
     (and (= person :3rd) (= number :plur) (or er-type stem))
     (str stem "aient")
       
     (and (= person :3rd) (= number :plur)
          ir-type)
     (str stem "issaient")
     ;; </third person plural imperfecto>
           
     :else
     (throw (Exception. (str "get-string-1: imperfecto regular inflection: don't know what to do with input argument: " (strip-refs word)))))))

(defn present [word]
  (let [infinitive (get-in word '(:français))
        ar-type (try (re-find #"ar$" infinitive)
                     (catch Exception e
                       (throw (Exception. (str "Can't regex-find on non-string: " infinitive " from word: " word)))))
        er-type (re-find #"[eé]r$" infinitive)
        ir-type (re-find #"ir$" infinitive)
        re-type (re-find #"re$" infinitive)
        stem (string/replace infinitive #"[iaeé]r$" "")
        stem (if re-type
               (string/replace infinitive #"re$" "")
               stem)
        last-stem-char-is-i (re-find #"ir$" infinitive)
        last-stem-char-is-e (re-find #"er$" infinitive)
        person (get-in word '(:agr :person))
        number (get-in word '(:agr :number))
        g-stem (re-find #"[g]er$" infinitive)
        number-and-person (number-and-person number person)
        ]
    (cond
     (get-in word [:present number-and-person])
     (get-in word [:present number-and-person])

     ;;QUI COMINCIANO I VERBI FRANCESI REGOLARI
     (and (= person :1st) (= number :sing) er-type)
     (str stem "e")

     (and (= person :1st) (= number :sing) ir-type)
     (str stem "is")

     (and (= person :1st) (= number :sing) re-type)
     (str stem "")

     (and (= person :2nd) (= number :sing) er-type)
     (str stem "es")

     (and (= person :2nd) (= number :sing) ir-type)
     (str stem "is")

     (and (= person :2nd) (= number :sing) re-type)
     (str stem "s")

     (and (= person :3rd) (= number :sing) er-type)
     (str stem "e")

     (and (= person :3rd) (= number :sing) ir-type)
     (str stem "it")

     (and (= person :3rd) (= number :sing) re-type)
     (str stem "")

     (and (= person :1st) (= number :plur) er-type g-stem)
     (str stem "eons")

     (and (= person :1st) (= number :plur) er-type)
     (str stem "ons")

     (and (= person :1st) (= number :plur) ir-type)
     (str stem "issons")

     (and (= person :1st) (= number :plur) re-type)
     (let [stem (string/replace stem #"d$" "")]
       (str stem "ons"))

     ;; <second person plural present>
     (and (= person :2nd) (= number :plur) er-type)
     (str stem "ez")

     (and (= person :2nd) (= number :plur) ir-type)
     (str stem "issez")  

     (and (= person :2nd) (= number :plur) re-type)
     (str stem "ez")  

     ;; </second person plural present>

     ;; <third person plural present>
     (and (= person :3rd) (= number :plur)
          er-type)
     (str stem "ent")
     (and (= person :3rd) (= number :plur)
          ir-type)
     (str stem "issent")
     (and (= person :3rd) (= number :plur)
          re-type)
     (let [stem (string/replace stem #"d$" "")]
       (str stem "nent"))

     ;; </third person plural present>

     ;; agreement is underspecified, but an infinitive form (the :français key) exists, so just return that infinitive form.
     (and (= (get-in word [:agr]) :top)
          (string? (get-in word [:français])))
     (get-in word [:français])

     :else
     (throw (Exception. (str "get-string: present regular inflection: don't know what to do with input argument: " (strip-refs word) " (er-type: " er-type "; ir-type: " ir-type "; re-type: " re-type))))))

(defn passe-compose [word]
  (let [infinitive (get-in word '(:français))]
    (if-let [irregular (get-in word [:past-participle])]
      irregular

      ;; else, no :past-participle irregular form found, so use regular.
      (let [essere (get-in word [:essere])
            er-type (re-find #"[ée]r$" infinitive) ;; c.f. italiano -are
            re-type (re-find #"re$" infinitive) ;; c.f. italian -ere
            ir-type (re-find #"ir$" infinitive) ;; c.f. italian -ire
            stem (string/replace infinitive #"[iaeé]r$" "")
            stem (if re-type
                   (string/replace infinitive #"re$" "")
                   stem)
            last-stem-char-is-i (re-find #"ir$" infinitive)
            last-stem-char-is-e (re-find #"er$" infinitive)
            person (get-in word '(:agr :person))
            number (get-in word '(:agr :number))
            gender-suffix
            (cond (= :fem (get-in word [:agr :gender]))
                  "e"
                  true
                  "")

            number-suffix
            (cond (= :plur (get-in word [:agr :number]))
                  "s"
                  true
                  "")

            verb-type-suffix
            (cond er-type
                  "é"
                  re-type
                  "u"
                  true
                  "i")]        
        ;; regular
        (str stem verb-type-suffix
             (if essere
               gender-suffix "")
             (if essere
               number-suffix ""))))))

(defn number-and-person [number person]
  (cond (and (= person :1st) (= number :sing))
        :1sing
        (and (= person :1st) (= number :plur))
        :1plur
        (and (= person :2nd) (= number :sing))
        :2sing
        (and (= person :2nd) (= number :plur))
        :2plur
        (and (= person :3rd) (= number :sing))
        :3sing
        (and (= person :3rd) (= number :plur))
        :3plur
        true
        nil))

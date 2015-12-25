(ns babel.italiano.pos)

(require '[babel.lexiconfn :as lexiconfn])
(require '[babel.pos :as pos])
(require '[dag_unify.core :refer (unifyc)])

(def agreement-noun pos/agreement-noun)
(def cat-of-pronoun pos/cat-of-pronoun)
(def common-noun pos/common-noun)
(def comparative pos/comparative)
(def countable-noun pos/countable-noun)
(def determiner pos/determiner)
(def drinkable-noun pos/drinkable-noun)
(def non-comparative-adjective pos/non-comparative-adjective)
(def noun pos/noun)
(def pronoun-acc pos/pronoun-acc)
(def sentential-adverb pos/sentential-adverb)
(def verb-aux
  (let [sem (atom {:tense :past})]
    (unifyc {:synsem {:sem sem
                      :subcat {:2 {:infl :past}}}}
            ;; whether a verb has essere or avere as its
            ;; auxiliary to form its past form:
            (let [aux (atom true)
                  pred (atom :top)
                  sem (atom {:pred pred})
                  subject (atom :top)
                  essere-binary-categorization (atom :top)]
              {:synsem {:aux aux
                        :sem sem
                        :essere essere-binary-categorization
                        :subcat {:1 subject
                                 :2 {:cat :verb
                                     :essere essere-binary-categorization
                                     :aux false
                           :subcat {:1 subject}
                                     :sem sem}}}}))))

(def noun-agreement
  (let [agr (atom :top)]
    {:italiano {:agr agr}
     :synsem {:agr agr}}))

(def feminine-noun (unifyc
                    noun-agreement (:feminine pos/noun)))

(def masculine-noun (unifyc
                     noun-agreement (:masculine pos/noun)))

(def adjective
  (unifyc pos/adjective
          (let [agr (atom :top)
                cat (atom :top)]
            {:italiano {:agr agr
                        :cat cat}
             :synsem {:agr agr
                      :cat cat}})))

;; A generalization of intransitive and transitive:
;; they both have a subject, thus "subjective".
(def verb-subjective
  (unifyc pos/verb-subjective
          (let [infl (atom :top)
                agr (atom :top)
                essere-type (atom :top)]
            {:italiano {:agr agr
                        :essere essere-type
                        :infl infl}
             :synsem {:infl infl
                      :essere essere-type
                      :subcat {:1 {:agr agr}}}})))

(def transitive
  (unifyc verb-subjective
          pos/transitive))

(def intransitive-unspecified-obj
  (unifyc
   {:synsem {:sem {:reflexive false}}}
   (unifyc verb-subjective
           pos/intransitive-unspecified-obj)))

(def intransitive
  (unifyc
   {:synsem {:reflexive false}}
   (unifyc verb-subjective
           pos/intransitive)))

(defn intransitivize [lexicon]
  (lexiconfn/intransitivize lexicon intransitive transitive intransitive-unspecified-obj))

(defn transitivize [lexicon]
  (lexiconfn/transitivize lexicon transitive verb-subjective))

(def pronoun-reflexive
  {:synsem {:cat :noun
            :pronoun true
            :case :acc
            :reflexive true}})

            

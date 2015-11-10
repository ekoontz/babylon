(ns babel.francais.lexicon
  (:refer-clojure :exclude [get-in])
  (:require
   [babel.lexiconfn :refer (unify)]
   [babel.francais.morphology :refer [exception-generator phonize]]
   [babel.francais.pos :refer :all]
   [babel.lexiconfn :refer (compile-lex map-function-on-map-vals unify)]
   [dag-unify.core :refer [get-in]]))

(def lexicon-source 
  {
   "abandoner" {:synsem {:cat :verb
                         :sem {:pred :abandon}}}

   "accepter" {:synsem {:cat :verb
                        :sem {:pred :accept}}}

   "accompagner" {:synsem {:cat :verb
                           :sem {:pred :accompany}}}

   "acheter" {:synsem {:cat :verb
                       :sem {:subj {:human true}
                             :pred :comprare}}}

   "aider" {:synsem {:cat :verb
                     :sem {:pred :aiutare}}}

   "aimer" {:synsem {:cat :verb
                     :sem {:pred :amare
                           :subj {:human true}}}}
   
   "aller" {:français {:future-stem "ir"
                       :present {:1sing "vais"
                                 :2sing "vas"
                                 :3sing "va"
                                 :1plur "allons"
                                 :2plur "allez"
                                 :3plur "vont"}}
            :synsem {:cat :verb
                     :essere true
                     :sem {:subj {:animate true}
                           :pred :go}}}

   "anoncier" {:synsem {:cat :verb
                        :sem {:pred :announce}}}

   "appeler" {:synsem {:cat :verb
                       :boot-stem "appell"
                       :future-stem "appell"
                       :sem {:pred :call}}}

   "apporter" [{:synsem {:cat :verb
                         :sem {:pred :take}}}
               {:synsem {:cat :verb
                         :sem {:pred :carry}}}]

   ;;  CONJUGATES LIKE TENIR
   "apprendre" {:synsem {:cat :verb
                         :past-participle "appris"
                         :sem {:pred :learn}}}
   
   "assurer" [{:synsem {:cat :verb
                        :sem {:pred :assure}}}
              {:synsem {:cat :verb
                        :sem {:pred :insure}}}]

   "attendre" {:synsem {:cat :verb
                        :sem {:pred :wait-for}}}
                       
   "augmenter" {:synsem {:cat :verb
                         :sem {:pred :increase}}}                   
   "avoir"
   (let [common
         {:synsem {:essere false
                   :cat :verb}
          :français {:futuro-stem "aur"
                     :drop-e true
                     :past-participle "eu"
                     :imperfect-stem "av"
                     :present {:1sing "ai"
                               :2sing "as"
                               :3sing "a"
                               :1plur "avons"
                               :2plur "avez"
                               :3plur "ont"}}}]
     [(unify common {:synsem {:sem {:pred :avere
                                    :subj {:human true}}}})
      (unify common verb-aux
             {:synsem {:subcat {:2 {:essere false}}}})])

   "baisser" {:synsem {:cat :verb
                      :sem {:pred :lower}}}

   "boire" {:français {:past-participle "bu"
                       :boot-stem1 "boiv"
                       :boot-stem2 "buv"}
            :synsem {:cat :verb
                     :sem {:pred :drink}}}

   "changer" {:synsem {:cat :verb
                      :sem {:pred :change}}}
   
   "chanter" {:synsem {:cat :verb
                       :sem {:pred :sing}}}
   
   "commencer" {:synsem {:cat :verb
                         :sem {:pred :begin}}}

   "commenter" {:synsem {:cat :verb
                         :sem {:pred :comment}}}
   
   "comprendre" {:synsem {:cat :verb
                          :past-participle "compris"
                          :sem {:pred :understand}}}

   "conserver" [{:synsem {:cat :verb
                          :sem {:pred :conserve}}}
                {:synsem {:cat :verb
                          :sem {:pred :preserve}}}]
   
   "considérer" {:synsem {:cat :verb
                         :sem {:pred :consider}}}
    
   "couper" {:synsem {:cat :verb
                      :sem {:pred :cut}}}
  
   "courir" {:synsem {:cat :verb
                      :sem {:pred :run}}}

   "créer" {:synsem {:cat :verb
                     :sem {:pred :create}}}
   
   "croire" {:synsem {:cat :verb
                      :sem {:pred :believe}}}
   
   "decider" {:synsem {:cat :verb
                       :sem {:pred :decide}}}

   "developer" {:synsem {:cat :verb
                        :sem {:pred :develop}}}
   "devoir"
   (let [common
         {:synsem {:essere false
                   :cat :verb}
          :français {:futuro-stem "devr"
                     :drop-e true
                     :past-participle "dû"
                     :imperfect-stem "dev"
                     :present {:1sing "dois"
                               :2sing "dois"
                               :3sing "doit"
                               :1plur "devons"
                               :2plur "devez"
                               :3plur "doivent"}}}]
     (unify common {:synsem {:sem {:pred :have-to
                                   :subj {:human true}}}}))
  
   "décider" {:synsem {:cat :verb
                       :sem {:pred :decide}}}
   
   "désirer" {:synsem {:cat :verb
                       :sem {:pred :desire}}}
   
   "dire"
   (let [shared-part-of-dire
         {:synsem {:cat :verb}
          :français {:français "dire"
                     :passato "dit"
                     :future-stem "dir"
                     :imperfect-stem "dis"
                     :present {:1sing "dis"
                               :2sing "dis"
                               :3sing "dit"
                               :1plur "disons"
                               :2plur "dites"
                               :3plur "disent"}}}]
     [(merge shared-part-of-dire
             {:synsem {:pred :say}})
      (merge shared-part-of-dire
             {:synsem {:pred :tell}})])
   
   "diviser" {:synsem {:cat :verb
                      :sem {:pred :divide}}}
   
   "donner" {:synsem {:cat :verb
                      :sem {:pred :give}}}
   
   "dormir" {:synsem {:cat :verb
                      :sem {:pred :sleep}}}

   "echapper" {:synsem {:cat :verb
                        :sem {:pred :escape}}}

   "écouter" {:synsem {:cat :verb
                       :sem {:pred :listen-to}}}

   "effacer" {:synsem {:cat :verb
                       :sem {:pred :erase}}}
   "elle"
   [{:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :3rd
                   :number :sing
                   :gender :fem}
              :sem {:human true
                    :pred :lei}
              :subcat '()}}
    {:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :3rd
                   :number :sing
                   :gender :fem}
              :sem {:human false
                    :pred :lei}
             :subcat '()}}]
   "elles"
   {:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :number :plur
                    :gender :fem}
              :sem {:gender :fem
                    :pred :loro}
              :subcat '()}}

   "enseigner" [{:synsem {:cat :verb
                         :sem {:pred :show}}}
               {:synsem {:cat :verb
                         :sem {:pred :teach}}}]

   "entrer" {:synsem {:cat :verb
                      :essere true
                      :sem {:pred :enter}}}

   "envoyer" {:synsem {:cat :verb
                       :sem {:pred :send}}}

   "essayer" {:synsem {:cat :verb
                      :sem {:pred :try}}}
   "être"
   (let [common
         {:synsem {:cat :verb
                   :essere false}
          :français {:futuro-stem "ser"
                     :infinitive "être"
                     :present {:1sing "suis"
                               :2sing "es"
                               :3sing "est"
                               :1plur "sommes"
                               :2plur "êtes"
                               :3plur "sont"}
                     :past-participle "été"
                     :imperfect {:1sing "étais"
                                 :2sing "étais"
                                 :3sing "était"
                                 :1plur "étions"
                                 :2plur "étiez"
                                 :3plur "étaient"}
                     :futuro {:1sing "serai"
                              :2sing "seras"
                              :3sing "sera"
                              :1plur "serons"
                              :2plur "serez"
                              :3plur "seront"}}}]
     [(unify common {:synsem {:sem {:pred :be}}})
      (unify common verb-aux
             {:synsem {:subcat {:2 {:essere true}}}})])
   ;; ^^ in auxiliary form, "être" only allows essere=true verbs.
   ;; compare with "avoir", which only allows essere=false verbs.
     
   "étudier" {:synsem {:cat :verb
                       :sem {:pred :study}}}
   
   "éviter" {:synsem {:cat :verb
                      :sem {:pred :avoid}}}
  
   "exister" {:synsem {:cat :verb
                       :sem {:pred :exist}}}
   
   "exprimer" {:synsem {:cat :verb
                        :sem {:pred :express}}}
   
   "expulser" {:synsem {:cat :verb
                        :sem {:pred :throw-out}}}
   
   "faire"
      (let [shared-part-of-faire
              {:synsem {:cat :verb}
               :français {:français "faire"
                          :drop-e false
                          :passato "fait"
                          :future-stem "fer"
                          :present {:1sing "fais"
                                    :2sing "fais"
                                    :3sing "fait"
                                    :1plur "faisons"
                                    :2plur "faites"
                                    :3plur "font"}}}]
    
     [(merge shared-part-of-faire
             {:synsem {:pred :do}})
      (merge shared-part-of-faire
             {:synsem {:pred :make}})
      (merge shared-part-of-faire
             (let [subject-semantics (ref {:human true})
                  subject-agr (ref :top)]
               {:synsem {:cat :verb
                         :essere true
                         :sem {:pred :hurt-oneself
                               :reflexive true
                               :subj subject-semantics
                               :obj subject-semantics}
                         :subcat {:1 {:agr subject-agr
                                      :sem subject-semantics}
                                  :2 {:agr subject-agr
                                      :pronoun true
                                      :reflexive true
                                      :sem subject-semantics}
                                  :3 {:cat :adverb
                                      :sem {:pred :bad}}}}}))])
   
   "former" {:synsem {:cat :verb
                     :sem {:pred :form}}}
   
   "gagner" [{:synsem {:cat :verb
                       :sem {:pred :earn
                             :subj {:human true}}}}
             {:synsem {:cat :verb
                       :sem {:pred :win
                             :subj {:human true}}}}]

   "gérer" {:synsem {:cat :verb
                    :sem {:pred :manage}}}
   "il"
   [{:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :number :sing
                    :gender :masc}
              :sem {:human true
                    :pred :lui}
              :subcat
              '()}}
    {:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :number :sing
                    :gender :masc}
              :sem {:human false
                    :pred :lui}
              :subcat
              '()}}]

    "ils"
    {:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :number :plur
                    :gender :masc}
              :sem {:gender :masc
                    :pred :loro}
               :subcat '()}}

    "imaginer" {:synsem {:cat :verb
                       :sem {:pred :imagine}}}
  "importer" {:synsem {:cat :verb
                       :sem {:pred :import}}}

  "insister" {:synsem {:cat :verb
                       :sem {:pred :insist}}}

;;  "interessér" {:synsem {:cat :verb
;;                         :sem {:pred :interest??}}}

  "je"
  (let [common {:synsem {:cat :noun
                         :pronoun true
                         :case :nom
                         :agr {:person :1st
                               :number :sing}
                         :sem {:human true
                               :pred :I}
                         :subcat '()}}]
    [(unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :fem}}})
     (unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :masc}}})])
  
  "laisser" {:synsem {:cat :verb
                      :sem {:pred :leave-behind}}}

  "male" {:synsem {:cat :adverb
                     :sem {:pred :bad}}}
  
  "manger"
  {:synsem {:cat :verb
            :sem {:pred :mangiare}}}
  
  "manquer" {:synsem {:cat :verb
                      :sem {:pred :to-be-missing}}}
  
  "marcher" [{:synsem {:cat :verb
                       :sem {:pred :walk}}}
             {:synsem {:cat :verb
                       :sem {:subj {:human false}
                              :pred :work-nonhuman}}}]

  "mettre" {:synsem {:cat :verb
                     :passato "mis"
                     :present {:1sing "mets"
                                    :2sing "mets"
                                    :3sing "met"
                                    :1plur "mettons"
                                    :2plur "mettez"
                                    :3plur "mettent"}
                     
                     :sem {:pred :put}}}
  "nous"
  (let [common {:synsem {:case :nom
                         :agr {:person :1st
                               :number :plur}
                         :sem {:human true
                               :pred :noi}}}]
    [(unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :fem}}})
     (unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :masc}}})])
  
   "observer" {:synsem {:cat :verb
                       :sem {:pred :observe}}}

   "oublier" {:synsem {:cat :verb
                      :sem {:pred :forget}}}
   "parler"
   [{:synsem {:cat :verb
              :sem {:pred :speak
                    :subj {:human true}}}}
    {:synsem {:cat :verb
              :sem {:pred :talk
                    :subj {:human true}}}}]

   "partager" {:synsem {:cat :verb
                       :sem {:pred :share}}}

   "participer" {:synsem {:cat :verb
                         :sem {:pred :participate}}}

   "peindre" {:français {:boot-stem1 "pein"
                         :boot-stem2 "peign"
                         :passé "peint"
                         :futuro-stem "paindr"
                         :imperfect "peign"}
              :synsem {:cat :verb
                       :sem {:pred :paint}}}
;  "profiter (de)" {:synsem {:cat :verb
;                            :sem {:pred :take-advantage-of}}}

  "prendre" {:synsem {:cat :verb
                         :past-participle "pris"
                         :sem {:pred :grab}}}
  
  "pouvoir"
   (let [shared-part-of-pouvoir
         {:synsem {:essere false
                   :cat :verb}
          :français {:futuro-stem "pourr"
                     :drop-e true
                     :past-participle "pu"
                     :imperfect-stem "pouv"
                     :present {:1sing "peux"
                               :2sing "peux"
                               :3sing "peut"
                               :1plur "pouvons"
                               :2plur "pouvez"
                               :3plur "peuvent"}}}]
     [(merge shared-part-of-pouvoir
             {:synsem {:pred :can}})
      (merge shared-part-of-pouvoir
             {:synsem {:pred :may}})
      (merge shared-part-of-pouvoir
             {:synsem {:pred :be-able-to}})])
  
  "regarder" [{:synsem {:cat :verb
                        :sem {:pred :look}}}
              {:synsem {:cat :verb
                        :sem {:pred :watch}}}]
  
  "remarquer" {:synsem {:cat :verb
                        :sem {:pred :note}}}

  "répondre" {:synsem {:cat :verb
                       :sem {:pred :answer}}}

;  "s'amuser" (let [subject-semantics (ref {:human true})
;                    subject-agr (ref :top)]
;                {:synsem {:cat :verb
;                          :essere true
;                          :sem {:pred :have-fun
;                                :reflexive true
;                                :subj subject-semantics
;                                :obj subject-semantics}
;                          :subcat {:1 {:agr subject-agr
;                                       :sem subject-semantics}
;                                   :2 {:agr subject-agr
;                                       :pronoun true
;                                       :reflexive true
;                                       :sem subject-semantics}}}})
  
  "s'amuser" (let [subject-semantics (ref {:human true})
                    subject-agr (ref :top)]
                {:synsem {:cat :verb
                          :essere true
                          :sem {:pred :have-fun
                                :reflexive true
                                :subj subject-semantics
                                :obj subject-semantics}
                          :subcat {:1 {:agr subject-agr
                                       :sem subject-semantics}
                                   :2 {:agr subject-agr
                                       :pronoun true
                                       :reflexive true
                                       :sem subject-semantics}}}})
  
  "se appeler" (let [subject-semantics (ref {:human true})
                    subject-agr (ref :top)]
                {:synsem {:cat :verb
                          :essere true
                          :français {:futuro-stem "appell"
                                     :boot-stem "appell"}
                          :sem {:pred :be-called
                                :reflexive true
                                :subj subject-semantics
                                :obj subject-semantics}
                          :subcat {:1 {:agr subject-agr
                                       :sem subject-semantics}
                                   :2 {:agr subject-agr
                                       :pronoun true
                                       :reflexive true
                                       :sem subject-semantics}}}})
  
  "se endormir" (let [subject-semantics (ref {:human true})
                    subject-agr (ref :top)]
                {:synsem {:cat :verb
                          :essere true
                          :sem {:pred :fall-asleep
                                :reflexive true
                                :subj subject-semantics
                                :obj subject-semantics}
                          :subcat {:1 {:agr subject-agr
                                       :sem subject-semantics}
                                   :2 {:agr subject-agr
                                       :pronoun true
                                       :reflexive true
                                       :sem subject-semantics}}}})
  
  "se énerver" (let [subject-semantics (ref {:human true})
                    subject-agr (ref :top)]
                {:synsem {:cat :verb
                          :essere true
                          :sem {:pred :get-angry
                                :reflexive true
                                :subj subject-semantics
                                :obj subject-semantics}
                          :subcat {:1 {:agr subject-agr
                                       :sem subject-semantics}
                                   :2 {:agr subject-agr
                                       :pronoun true
                                       :reflexive true
                                       :sem subject-semantics}}}})
  
  "se ennuyer" (let [subject-semantics (ref {:human true})
                    subject-agr (ref :top)]
                {:synsem {:cat :verb
                          :essere true
                          :français {:futuro-stem "ennui"
                                     :boot-stem "ennui"}
                          :sem {:pred :get-bored
                                :reflexive true
                                :subj subject-semantics
                                :obj subject-semantics}
                          :subcat {:1 {:agr subject-agr
                                       :sem subject-semantics}
                                   :2 {:agr subject-agr
                                       :pronoun true
                                       :reflexive true
                                       :sem subject-semantics}}}})
  
  "se lever" (let [subject-semantics (ref {:human true})
                    subject-agr (ref :top)]
                {:synsem {:cat :verb
                          :essere true
                          :sem {:pred :get-up
                                :reflexive true
                                :subj subject-semantics
                                :obj subject-semantics}
                          :subcat {:1 {:agr subject-agr
                                       :sem subject-semantics}
                                   :2 {:agr subject-agr
                                       :pronoun true
                                       :reflexive true
                                       :sem subject-semantics}}}})
  
  "soulever" {:synsem {:cat :verb
                       :sem {:pred :lift}}}

  "soutenir" {:synsem {:cat :verb
                       :sem {:pred :support}}}

  "terminer" {:synsem {:cat :verb
                       :sem {:pred :finish}}}

  "touer" {:synsem {:cat :verb
                    :sem {:pred :kill}}}
  "tu"
  (let [common {:synsem {:cat :noun
                         :case :nom
                         :agr {:person :2nd
                               :number :sing}
                         :sem {:human true
                               :pred :tu}}}]
    [(unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :fem}}})
     (unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :masc}}})])
  "vous"
  (let [common {:synsem {:case :nom
                         :agr {:person :2nd
                               :number :plur}
                         :sem {:human true
                               :pred :voi}}}]
    [(unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :fem}}})
     (unify gender-pronoun-agreement
            common
            {:synsem {:agr {:gender :masc}}})])
   })

(def lexicon
  (future (-> (compile-lex lexicon-source exception-generator phonize)

              ;; make an intransitive version of every verb which has a path [:sem :obj].
              intransitivize
              
              ;; if verb does specify a [:sem :obj], then fill it in with subcat info.
              transitivize

              ;; TODO: use lexiconfn/if-then where possible, like espanol/lexicon does.
              ;; default: essere=false
              (map-function-on-map-vals
               (fn [k vals]
                 (map (fn [val]
                        ;; if: 1. the val's :cat is :verb
                        ;;     2. it is not true that essere=true (either essere=false or essere is not defined)
                        ;; then: essere=false
                        (cond (and (= (get-in val [:synsem :cat])
                                      :verb)
                                   (not (= true (get-in val [:synsem :essere] false))))
                              (unify val {:synsem {:essere false}})
                                 
                              true ;; otherwise, leave the verb alone
                              val))
                      vals)))
              
              ;; Cleanup functions can go here. Number them for ease of reading.
              ;; 1. this filters out any verbs without an inflection:
              ;; infinitive verbs should have inflection ':infinitive', 
              ;; rather than not having any inflection.
              (map-function-on-map-vals 
               (fn [k vals]
                 (filter #(or (not (= :verb (get-in % [:synsem :cat])))
                              (not (= :none (get-in % [:synsem :infl] :none))))
                         vals))))))

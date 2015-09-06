(ns babel.francais.lexicon
  (:require
   [babel.lexiconfn :refer (unify)]
   [babel.francais.pos :refer :all]))

(def lexicon-source 
  {
   "abandoner" {:synsem {:cat :verb
                         :sem {:pred :abandon}}}

   "accepter" {:synsem {:cat :verb
                        :sem {:pred :accept}}}

   "accompagner" {:synsem {:cat :verb
                           :sem {:pred :accompany}}}

   "anoncier" {:synsem {:cat :verb
                        :sem {:pred :announce}}}

   "appeler" {:synsem {:cat :verb
                       :sem {:pred :call}}}

   "apporter" [{:synsem {:cat :verb
                         :sem {:pred :take}}}
               {:synsem {:cat :verb
                         :sem {:pred :carry}}}]

   ;;  CONJUGATES LIKE TENIR
   "apprendre" {:synsem {:cat :verb
                         :sem {:pred :imparare}}}
                       
    "attendre" {:synsem {:cat :verb
                         :sem {:pred :wait}}}    
                       
    "assurer" {:synsem {:cat :verb
                         :sem {:pred :assure}}}    
                       
    "augmenter" {:synsem {:cat :verb
                         :sem {:pred :increase}}}                   
                       
   "avoir"
   {:synsem {:essere false
             :sem {:pred :avere}
             :cat :verb}
    :français {:infinitive "avoir"
               :futuro-stem "aur"
               :drop-e true
               :present {:1sing "ai"
                         :2sing "as"
                         :3sing "a"
                         :1plur "avons"
                         :2plur "avez"
                         :3plur "ont"}}}
   
    "boire" {:synsem {:boot-stem1 "boiv"
                        :boot-stem2 "buv"}
    :français {:passato "bu"}                  
             :synsem {:cat :verb
                      :sem {:pred :drink}}}                  
                      
   "changer" {:synsem {:cat :verb
                         :sem {:pred :change}}}                   
    
    "chanter" {:synsem {:cat :verb
                         :sem {:pred :sing}}}
    
                      
   "commencer" {:synsem {:cat :verb
                         :sem {:pred :begin}}}
     
     "ecouter" {:synsem {:cat :verb
                         :sem {:pred :listen}}}                  
     
     "marchcer" {:synsem {:cat :verb
                         :sem {:pred :walk}}}
                       
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
   [{:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :number :plur
                    :gender :fem}
              :sem {:human true
                    :gender :fem
                    :pred :loro}
              :subcat '()}}
    {:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :3rd
                   :number :plur
                   :gender :fem}
             :sem {:human false
                   :gender :fem
                   :pred :loro}
             :subcat '()}}]

   "essayer" {:synsem {:cat :verb
                      :sem {:pred :try}}}
   "être"
   (let [infl (ref :top)
         agr (ref :top)]
     {:synsem {:agr agr
               :cat :verb
               :essere true
               :infl infl
               :subcat {:1 {:agr agr}}}
      :français {:agr agr
                 :futuro-stem "ser"
                 :infinitive "être"
                 :infl infl
                 :present {:1sing "suis"
                           :2sing "es"
                           :3sing "est"
                           :1plur "sommes"
                           :2plur "êtes"
                           :3plur "sont"}
                 :passato "été"
                 :imperfetto {:1sing "étais"
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
                          :3plur "seront"}}})

   "gagner" [{:synsem {:cat :verb
                      :sem {:pred :earn}}}
            {:synsem {:cat :verb
                      :sem {:pred :win}}}]

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
    [{:synsem {:cat :noun
               :pronoun true
               :case :nom
               :agr {:person :3rd
                     :number :plur
                     :gender :masc}
               :sem {:human true
                     :gender :masc
                     :pred :loro}
               :subcat '()}}
     {:synsem {:cat :noun
               :pronoun true
               :case :nom
               :agr {:person :3rd
                     :number :plur
                     :gender :masc}
               :sem {:human false
                     :gender :masc
                     :pred :loro}
               :subcat '()}}]

  "imaginer" {:synsem {:cat :verb
                       :sem {:pred :imagine}}}
  "importer" {:synsem {:cat :verb
                       :sem {:pred :import}}}

  "insister" {:synsem {:cat :verb
                       :sem {:pred :insist}}}

;;  "interessér" {:synsem {:cat :verb
;;                         :sem {:pred :interest??}}}

  "je" {:synsem {:cat :noun
                 :pronoun true
                 :case :nom
                 :agr {:person :1st
                       :number :sing}
                 :sem {:human true
                       :pred :io}
                 :subcat '()}}
   "manger"
   {:synsem {:cat :verb
             :sem {:pred :mangiare}}}
   "nous"
   {:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :1st
                   :number :plur}
              :sem {:human true
                    :pred :noi}
             :subcat '()}}
   "parler"
   [{:synsem {:cat :verb
              :sem {:pred :speak}}}
    {:synsem {:cat :verb
              :sem {:pred :talk}}}]

  "soutenir" {:synsem {:cat :verb
                       :sem {:pred :support}}}

  "terminer" {:synsem {:cat :verb
                       :sem {:pred :finish}}}
   "tu"
   {:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :2nd
                   :number :sing}
              :sem {:human true
                    :pred :tu}
             :subcat '()}}
   "vous"
   {:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :2nd
                   :number :plur}
              :sem {:human true
                    :pred :voi}
             :subcat '()}}
  
;  "profiter (de)" {:synsem {:cat :verb
;                            :sem {:pred :take-advantage-of}}}
  
  "assurer" [{:synsem {:cat :verb
                       :sem {:pred :assure}}}
             {:synsem {:cat :verb
                       :sem {:pred :insure}}}]

  "augmenter" {:synsem {:cat :verb
                        :sem {:pred :increase}}}

  "aider" {:synsem {:cat :verb
                    :sem {:pred :aiutare}}}
  
  "baisser" {:synsem {:cat :verb
                      :sem {:pred :lower}}}
  
  "changer" {:synsem {:cat :verb
                      :sem {:pred :cambiare}}}
  
  "commenter" {:synsem {:cat :verb
                        :sem {:pred :comment}}}
  
  "partager" {:synsem {:cat :verb
                       :sem {:pred :share}}}
  
  "acheter" {:synsem {:cat :verb
                      :sem {:pred :comprare}}}
  
  ;;CONJUGATES LIKE PRENDRE
  "comprendre" {:synsem {:cat :verb
                         :sem {:pred :understand}}}

  "conserver" [{:synsem {:cat :verb
                         :sem {:pred :conserve}}}
               {:synsem {:cat :verb
                         :sem {:pred :preserve}}}]
  
  "considérer" {:synsem {:cat :verb
                         :sem {:pred :consider}}}
  
  ;;SIMILAR TO PRENDRE
  "répondre" {:synsem {:cat :verb
                       :sem {:pred :answer}}}
  
  
  "corrir" {:synsem {:cat :verb
                     :sem {:pred :run}}}
  
  ;;  LIKE PRENDRE
  "correspondre" {:synsem {:cat :verb
                           :sem {:pred :correspond}}}
  
  "couper" {:synsem {:cat :verb
                     :sem {:pred :cut}}}
  
  "créer" {:synsem {:cat :verb
                    :sem {:pred :create}}}
  
  "devoir" {:synsem {:cat :verb
                     :sem {:pred :have-to}}}
  ;; IRREGOLARE
  "decider" {:synsem {:cat :verb
                      :sem {:pred :decide}}}
  
  "laisser" {:synsem {:cat :verb
                      :sem {:pred :leave-behind}}}
  
  "developer" {:synsem {:cat :verb
                        :sem {:pred :develop}}}
  
  "désirer" {:synsem {:cat :verb
                      :sem {:pred :desire}}}
  
  "expulser" {:synsem {:cat :verb
                       :sem {:pred :throw-out}}}
  
  "enseigner" [{:synsem {:cat :verb
                         :sem {:pred :show}}}
               {:synsem {:cat :verb
                         :sem {:pred :teach}}}]

  "entrer" {:synsem {:cat :verb
                     :sem {:pred :enter}}}
  
  "echapper" {:synsem {:cat :verb
                       :sem {:pred :escape}}}

  "écouter" {:synsem {:cat :verb
                      :sem {:pred :listen-to}}}
  
  "attendre" [{:synsem {:cat :verb
                        :sem {:pred :wait-for}}}
              {:synsem {:cat :verb
                        :sem {:pred :hope}}}]
  
  "étudier" {:synsem {:cat :verb
                      :sem {:pred :study}}}
  
  "éviter" {:synsem {:cat :verb
                     :sem {:pred :avoid}}}
  
  "exister" {:synsem {:cat :verb
                      :sem {:pred :exist}}}
  
  "exprimer" {:synsem {:cat :verb
                       :sem {:pred :express}}}
  
  "manquer" {:synsem {:cat :verb
                      :sem {:pred :to-be-missing}}}
  
  "former" {:synsem {:cat :verb
                     :sem {:pred :form}}}
  
  "marcher" {:synsem {:cat :verb
                      :sem {:subj {:human false}
                            :pred :work-nonhuman}}}
  
  "soulever" {:synsem {:cat :verb
                       :sem {:pred :lift}}}

  "gérer" {:synsem {:cat :verb
                    :sem {:pred :manage}}}
  "envoyer" {:synsem {:cat :verb
                      :sem {:pred :send}}}
  "touer" {:synsem {:cat :verb
                    :sem {:pred :kill}}}
  "mettre" {:synsem {:cat :verb
                     :sem {:pred :set}}}
  "regarder" [{:synsem {:cat :verb
                        :sem {:pred :look}}}
              {:synsem {:cat :verb
                        :sem {:pred :watch}}}]
  
  "remarquer" {:synsem {:cat :verb
                        :sem {:pred :note}}}

  "observer" {:synsem {:cat :verb
                       :sem {:pred :observe}}}
  
  "oublier" {:synsem {:cat :verb
                      :sem {:pred :forget}}}

  "participer" {:synsem {:cat :verb
                         :sem {:pred :participate}}}

  "diviser" {:synsem {:cat :verb
                      :sem {:pred :divide}}}
   
   })







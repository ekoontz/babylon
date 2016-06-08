(ns babel.english.lexicon
  (:refer-clojure :exclude [get-in merge])
  (:require
   [babel.encyclopedia :as e]
   [babel.lexiconfn :refer (compile-lex if-then
                                        map-function-on-map-vals unify)]
   [babel.english.morphology :as morph]
   [babel.english.pos :refer [adjective
                              agreement-noun
                              common-noun
                              countable-noun
                              feminine-noun
                              intransitivize
                              masculine-noun
                              subject-verb-agreement
                              transitivize]]
   [dag_unify.core :refer [fail? get-in merge strip-refs]]))

#?(:cljs
   (defn- future [expr]
     expr))

(def lexicon-source
  {

   ;; useful meta-lexeme for debugging generation or parsing -
   ;;    modify its constraints as you prefer for what you are trying to debug.
;   "_"
;   (unify agreement-noun
;          common-noun
;          feminine-noun
;          countable-noun
;          {:synsem {:sem {:pred :blank}}})
;   "_"
;   ;(unify agreement-noun common-noun
;          {:synsem {:sem e/animal}})
   
   "Antonia"
   {:synsem {:sem {:pred :antonia
                   :human true}
             :propernoun true
             :agr {:number :sing
                   :person :3rd
                   :gender :fem}}}
   "Antonia and Luisa"
   {:synsem {:sem {:pred :antonia-and-luisa
                   :human true}
             :propernoun true
             :agr {:number :plur
                   :person :3rd
                   :gender :fem}}}
   "Antonio"
   {:synsem {:propernoun true
             :agr {:number :sing
                   :person :3rd
                   :gender :masc}
             :sem {:pred :antonio
                   :human true}}}
   "a"
   {:synsem {:cat :det
             :def :indef
             :mass false
             :sem {:pred :a-generic-instance-of}
             :agr {:number :sing}}}

   "abandon" {:synsem {:cat :verb
                       :sem {:pred :abandon}
                       :subcat {:1 {:cat :noun
                                    :sem {:human true}}
                                :2 {:cat :noun
                                    :sem {:physical-object true}}}}}
   "accept"  {:synsem {:cat :verb
                       :sem {:pred :accept}}}
   
   "accompany" {:synsem {:cat :verb
                         :sem {:pred :accompany}
                         :subcat {:1 {:cat :noun
                                      :sem {:animate true}}
                                  :2 {:cat :noun
                                      :sem {:human true}}
                                  :3 '()}}
                :english {:past "accompanied"}}
   
   "add" {:synsem {:cat :verb
                   :sem {:pred :add}}}

   "admire" {:synsem {:cat :verb
                      :sem {:pred :admire}}}

   "announce" {:synsem {:cat :verb
                        :sem {:pred :announce}}}

   "answer" {:synsem {:cat :verb
                      :sem {:pred :answer
                            :subj {:human true}}}}

   "approve" {:synsem {:cat :verb
                :sem {:pred :approve}}}

   "arrive" {:synsem {:cat :verb
                      :sem {:pred :arrive}}}
   
   "ask" {:synsem {:cat :verb
                   :sem {:pred :chiedere
                         :subj {:human true}}}}

   "assume" {:synsem {:cat :verb
                      :sem {:pred :assume}
                      :subcat {:1 {:cat :noun
                                   :sem {:human true}}
                               :2 {:cat :comp
                                   :comp-type :that
                                   :subcat '()}}}}
   "assure" {:synsem {:cat :verb
                      :sem {:pred :assure}}}

   "at" (let [obj (atom {:place true
                         :reflexive false})]
          {:synsem {:cat :prep
                    :subcat {:1 {:cat :noun
                                 :pronoun false
                                 :subcat '()
                                 :sem obj}}
                    :sem {:obj obj
                          :pred :at}}})

   "attend" {:synsem {:cat :verb
                      :sem {:pred :attend}
                      :subcat {:1 {:cat :noun
                                   :sem {:human true}}
                               :2 {:cat :noun
                                   :pronoun false
                                   :sem {:event true
                                         :human false}}}}}

   "avoid" {:synsem {:cat :verb
                     :sem {:pred :avoid}
                     :subcat {:1 {:cat :noun
                                  :sem {:animate true}}
                              :2 {:cat :noun}}}}

   "bag" {:synsem {:cat :noun
                   :sem {:pred :bag}}}

   "base" {:synsem {:cat :verb
                    :sem {:pred :support}}}
   
   "be" (let [number-agr (atom :top)
              common {:synsem {:cat :verb}
                      :english {:present {:1sing "am"
                                          :2sing "are"
                                          :3sing "is"
                                          :1plur "are"
                                          :2plur "are"
                                          :3plur "are"}
                                :past {:1sing "was"
                                       :2sing "were"
                                       :3sing "was"
                                       :1plur "were"
                                       :2plur "were"
                                       :3plur "were"}}}]
          [;; intransitive
           (unify common
                  {:synsem {:subcat {:1 {:cat :noun}
                                     :2 '()}
                            :sem {:pred :be}}})


           ;; be + propernoun, e.g. "My name is John"
           (let [gender (atom :top)
                 number (atom :top)
                 subj-agr (atom {:person :3rd
                                 :gender gender
                                 :number number})
                 infl (atom :top)
                 the-real-subj (atom :top)
                 the-obj (atom {:number number
                                :gender gender})] ;; prevents e.g. "Her name is John"
             (unify common
                    subject-verb-agreement
                    {:intransitivize false
                     :transitivize false
                     :synsem {:agr subj-agr
                              :sem {:aspect :progressive
                                    :pred :be-called
                                    :tense :present
                                    :subj the-real-subj
                                    :obj the-obj}
                              :subcat {:1 {:cat :noun
                                           :case :nom ;; TODO: this should be a general lexical post-processing step -
                                           ;; call it subject-is-nominative or similar.
                                           :agr subj-agr
                                           :sem {:pred :name  ;; "My name" in "My name is John"
                                                 :subj the-real-subj}
                                           }
                                       :2 {:cat :noun
                                           :agr subj-agr
                                           :sem the-obj
                                           :pronoun false
                                           :propernoun true ;; "John" in "My name is John"
                                           }
                                       } ;; subcat {
                              } ;; synsem {
                     } ;; end of map
                    ))])

   "be able to" {:english {:imperfect {:1sing "was able to"
                                       :2sing "were able to"
                                       :3sing "was able to"
                                       :1plur "were able to"
                                       :2plur "were able to"
                                       :3plur "were able to"}

                           ;; TODO: improve this. Currently sounds pretty awkward:
                           ;; "he was being able to"
                           :participle "being able to"

                           :present {:1sing "am able to"
                                     :2sing "are able to"
                                     :3sing "is able to"
                                     :1plur "are able to"
                                     :2plur "are able to"
                                     :3plur "are able to"}
                           :past {:1sing "was able to"
                                  :2sing "were able to"
                                  :3sing "was able to"
                                  :1plur "were able to"
                                  :2plur "were able to"
                                  :3plur "were able to"}}
                 :synsem {:cat :verb
                          :sem {:pred :be-able-to}}}
   
   "be born" {:synsem {:cat :verb
                         :sem {:pred :be-born}}
                :english {:future "be born"
                          :participle "being born"
                          :conditional "be born"
                          :imperfect {:1sing "was being born"
                                      :2sing "were being born"
                                      :3sing "was being born"
                                      :1plur "were being born"
                                      :2plur "were being born"
                                      :3plur "were being born"}
                          :past {:english "was born"
                                 :2sing "were born"
                                 :2plur "were born"
                                 :3plur "were born"}
                          :present {:1sing "am born"
                                    :2sing "are born"
                                    :3sing "is born"
                                    :1plur "are born"
                                    :2plur "are born"
                                    :3plur "are born"}}}
   "be missed" {:synsem {:cat :verb
                         :sem {:pred :mancare}}
                :english {:future "be missed"
                          :participle "being missed"
                          :conditional "be missed"
                          :imperfect {:1sing "was being missed"
                                      :2sing "were being missed"
                                      :3sing "was being missed"
                                      :1plur "were being missed"
                                      :2plur "were being missed"
                                      :3plur "were being missed"}
                          :past {:english "was missed"
                                 :2sing "were missed"
                                 :2plur "were missed"
                                 :3plur "were missed"}
                          :present {:1sing "am missed"
                                    :2sing "are missed"
                                    :3sing "is missed"
                                    :1plur "are missed"
                                    :2plur "are missed"
                                    :3plur "are missed"}}}

   ;; TODO: for imperfect, generates things like 'he was be missinging'.
   "be missing" {:english {:imperfect {:1sing "was missing"
                                       :2sing "were missing"
                                       :3sing "was missing"
                                       :1plur "were missing"
                                       :2plur "were missing"
                                       :3plur "were missing"}
                           :present {:1sing "am missing"
                                     :2sing "are missing"
                                     :3sing "is missing"
                                     :1plur "are missing"
                                     :2plur "are missing"
                                     :3plur "are missing"}
                           :past {:1sing "was missing"
                                  :2sing "were missing"
                                  :3sing "was missing"
                                  :1plur "were missing"
                                  :2plur "were missing"
                                  :3plur "were missing"}}
                 :synsem {:cat :verb
                          :sem {:pred :to-be-missing}}}

   "beach" {:synsem {:cat :noun
                     :sem {:place true
                           :pred :beach
                           :artifact false}}}

   "become" {:synsem {:cat :verb
                      :sem {:pred :become}
                      :subcat {:2 {:cat :adjective}}}
             :english {:past "became"}}
   
   "begin" {:synsem {:cat :verb
                     :sem {:pred :begin}}
            :english {:past "began"
                      :participle "beginning"}}

   "believe" (let [common {:synsem {:cat :verb
                                    :subcat {:1 {:cat :noun
                                                 :sem {:human true}}}}}]
               [(unify common
                       {:synsem {:sem {:pred :believe}
                                 :subcat {:2 '()}}}) ;; intransitive
                
                (unify common
                       {:synsem {:sem {:pred :believe}
                                 :subcat {:2 {:cat :comp
                                              :comp-type :that
                                              :subcat '()}
                                          :3 '()}}})])
   
   "bicycle" {:synsem {:cat :noun
                       :sem {:pred :bicycle
                             :artifact true
                             :consumable false
                             :place false}}}
   "black"
   (unify adjective
          {:synsem {:cat :adjective
                    :sem {:mod {:pred :black}
                          :comparative false
                          :physical-object true
                          :human false}}})

   "boil" {:synsem {:cat :verb
                :sem {:pred :boil}}}
   "book"
   {:synsem {:cat :noun
             :sem {:artifact true
                   :pred :libro
                   :legible true
                   :speakable false
                   :mass false
                   :place false
                   :consumable false}}}

   "break" {:synsem {:cat :verb
                     :sem {:pred :break }}
            :english {:past "broke"}}
   "bread"
   ;; inherently singular.
   {:synsem {:agr {:number :sing}
             :cat :noun
             :sem {:pred :bread
                   :edible true
                   :artifact true}
             :subcat {:1 {:cat :det
                          :def :def}}}}

   "bring" {:synsem {:cat :verb
                   :sem {:pred :bring
                         :subj {:human true}
                         :obj {:buyable true}}}
          :english {:past "brought"}}
   
   "buy" {:synsem {:cat :verb
                   :sem {:pred :comprare
                         :subj {:human true}
                         :obj {:buyable true}}}
          :english {:past "bought"
                    :present {:3sing "buys"}}}

   "call" {:synsem {:cat :verb
                     :sem {:pred :call}}}

   "can" 
   {:english {:participle "being able to"
              :past "could"
              :present {:3sing "can"}
              :future "be able to"
              :conditional "be able to"}
    :synsem {:cat :verb
             :sem {:pred :can}}}

   "car" {:synsem {:cat :noun
                   :sem {:pred :car}}}

   "carry" {:synsem {:cat :verb
                     :sem {:subj {:human true}
                           :pred :carry
                           :obj {:physical-object true}}}
            :english {:past "carried"}}

   "cat" {:synsem {:cat :noun
                   :sem {:pred :cat
                         :pet true}}}

   "change" {:synsem {:cat :verb
                      :sem {:pred :change}}} ;; TODO: add reflexive sense

   "change clothes" {:synsem {:cat :verb
                              :sem {:pred :change-clothes
                                    :reflexive true
                                    :subj {:human true}}}
                     :english {:present {:3sing "changes clothes"}
                               :participle "changing clothes"
                               :past "changed clothes"}}

   "chat" {:synsem {:cat :verb
                    :sem {:pred :chat}}
           :english {:participle "chatting"
                     :past "chatted"}}

   "charge" {:synsem {:cat :verb
                      :sem {:pred :caricare}}}

   "check" {:synsem {:cat :verb
                     :sem {:pred :check}}}
                   
   "city" {:synsem {:cat :noun
                    :sem {:pred :city
                          :city true}}}
   
   "close" {:synsem {:cat :verb
                     :sem {:pred :close}}}                

   "come" {:synsem {:cat :verb
                    :sem {:pred :come
                          :subj {:animate true}}}
           :english {:past "came"}}

   "comment" {:synsem {:cat :verb
                       :sem {:pred :comment}}}

   "confess" {:synsem {:cat :verb
                       :sem {:pred :confess}}}

   "consent" {:synsem {:cat :verb
                        :sem {:pred :consent}}}
   
   "conserve" {:synsem {:cat :verb
                        :sem {:pred :conserve}}}

   "consider" {:synsem {:cat :verb
                        :sem {:pred :consider}}}

   "continue" {:synsem {:cat :verb
                      :sem {:pred :continue}}}
   
   "convert" {:synsem {:cat :verb
                       :sem {:pred :convert}}}

   "correspond" {:synsem {:cat :verb
                          :sem {:pred :correspond}}}

   "count" {:synsem {:cat :verb
                      :sem {:pred :count}}}
   
   "create" {:synsem {:cat :verb
                      :sem {:pred :create}}}

   "cry" {:synsem {:cat :verb
                    :sem {:pred :cry}}}

   "cut" {:english {:past "cut"
                    :participle "cutting"}
          :synsem {:cat :verb
                   :sem {:pred :cut}}}

   "dance" {:synsem {:cat :verb
                      :sem {:pred :dance}}}
   
   "decide" {:synsem {:cat :verb
                      :sem {:pred :decide}}}

   "defend" {:synsem {:cat :verb
                      :sem {:pred :defend}}}

   "deny" {:synsem {:cat :verb
                    :sem {:pred :deny}}}

   "desire" {:synsem {:cat :verb
                      :sem {:pred :desire}}}

   "develop" {:synsem {:cat :verb
                       :sem {:pred :develop}}}

   "dictate" {:synsem {:cat :verb
                      :sem {:pred :dictate}}}
   
   "dine" {:synsem {:cat :verb
                    :sem {:pred :cenare
                          :subj {:human true}}}}

   "displace" {:synsem {:cat :verb
                :sem {:pred :displace}}}

   "divide" {:synsem {:cat :verb
                      :sem {:pred :divide}}}
   
   "drink" {:synsem {:cat :verb
                     :sem {:pred :drink
                           :discrete false
                           :subj {:animate true}
                           :obj {:drinkable true}}}
            :english {:past "drank"}}

   "drive" {:synsem {:cat :verb
                     :sem {:pred :guidare}}
            :english {:past "drove"}}

   "disappoint" {:synsem {:cat :verb
                          :sem {:pred :deludere}
                          :subcat {:2 {:cat :noun
                                       :sem {:human true}}}}}
   "do" {:synsem {:cat :verb
                  :sem {:pred :do}}
         :english {:past "did"
                   :present {:3sing "does"}}}

   "download" {:synsem {:cat :verb
                        :sem {:pred :scaricare}}}

   "dog" {:synsem {:cat :noun
                   :sem {:pred :cane
                         :pet true}}}

   "earn"  {:synsem {:cat :verb
                     :sem {:pred :earn
                           :subj {:human true}}}}
   "eat" {:english {:past "ate"}
          :synsem {:cat :verb
                   :sem {:pred :mangiare
                         :subj {:animate true}
                         :obj {:edible true}}}}

   "eat dinner" {:synsem {:cat :verb
                          :sem {:pred :cenare
                                :subj {:human true}}}
                 :english {:present {:3sing "eats dinner"}
                           :participle "eating dinner"
                           :past "ate dinner"}}

   "embrace" {:synsem {:cat :verb
                       :sem {:pred :abbracciare}
                       :subj {:human true}
                       :obj {:human true}}}

   "endure" {:synsem {:cat :verb
                :sem {:pred :endure}}}

   "engage" {:synsem {:cat :verb
                :sem {:pred :engage}}}

   "enjoy" {:english {:present {:3sing "enjoys"}}
            :synsem {:cat :verb
                     :sem {:pred :enjoy}}}
  
   "enter"  {:synsem {:cat :verb
                      :sem {:pred :enter}}}

   "erase"  {:synsem {:cat :verb
                      :sem {:pred :erase}}}

   "escape" {:synsem {:cat :verb
                      :sem {:pred :escape}}}

   "exist" {:synsem {:cat :verb
                     :sem {:pred :exist}}}
   
   "exit" {:synsem {:cat :verb
                     :sem {:pred :exit}}}

   "express" {:synsem {:cat :verb
                       :sem {:pred :express}}}

   "faint" {:synsem {:cat :verb
                     :sem {:pred :faint}}}

   "fall asleep"
   (let [subject-semantics (atom {:animate true})]
     {:synsem {:cat :verb
               :sem {:pred :fall-asleep
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "falling asleep"
                :present {:3sing "falls asleep"}
                :past "fell asleep"}})

   "father" {:synsem {:cat :noun
                      :agr {:gender :masc}
                      :sem {:human true
                            :pred :father
                            :child false}}}
   
   "finish" {:synsem {:cat :verb
                      :sem {:pred :finish}}}

   "first" (unify adjective
                  {:synsem {:sem {:mod {:pred :first}
                                  :comparative false}}})

   "fold" {:synsem {:cat :verb
                    :sem {:pred :fold}}}
   
   "follow" {:synsem {:cat :verb
                      :sem {:pred :follow}}}
   
   "forget" {:synsem {:cat :verb
                      :sem {:pred :forget}}
             :english {:past "forgot"}}

   "form" {:synsem {:cat :verb
                    :sem {:pred :form}}}


   "furnish"  {:synsem {:cat :verb
                        :sem {:pred :furnish}}}

   "game" {:synsem {:cat :noun
                    :sem {:pred :game
                          :animate false
                          :event true
                          :games true}}}
   "get angry"
   (let [subject-semantics (atom {:animate true})]
     {:synsem {:cat :verb
               :sem {:pred :get-angry
                     :reflexive true
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting angry"
                :present {:3sing "gets angry"}
                :past "got angry"}})
   "get bored"
   (let [subject-semantics (atom {:human true})]
     {:synsem {:cat :verb
               :sem {:pred :get-bored
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting bored"
                :present {:3sing "gets bored"}
                :past "got bored"}})
   
   "get dressed"
   (let [subject-semantics (atom {:human true})]
     {:synsem {:cat :verb
               :sem {:pred :get-dressed
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting dressed"
                :present {:3sing "gets dressed"}
                :past "got dressed"}})
   "get off"
   (let [subject-semantics (atom {:animate true})]
     {:synsem {:cat :verb
               :sem {:pred :get-off
                     :subj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting off"
                :present {:3sing "gets off"}
                :past "got off"}})
   "get on"
   (let [subject-semantics (atom {:animate true})]
     {:synsem {:cat :verb
               :sem {:pred :get-on
                     :subj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting on"
                :present {:3sing "gets on"}
                :past "got on"}})
   "get ready"
   (let [subject-semantics (atom {:human true})]
     {:synsem {:cat :verb
               :sem {:pred :get-ready
                     :reflexive true
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting ready"
                :present {:3sing "gets ready"}
                :past "got ready"}})
   "get up"
   (let [subject-semantics (atom {:animate true})]
     {:synsem {:cat :verb
               :sem {:pred :get-up
                     :reflexive true
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "getting up"
                :present {:3sing "gets up"}
                :past "got up"}})

   "Gianluca"
   {:synsem {:agr {:number :sing
                   :person :3rd
                   :gender :masc}
             :sem {:pred :gianluca
                   :human true}
             :propernoun true}}

   "Gianluca and Giovanni"
   {:synsem {:agr {:number :plur
                   :person :3rd
                   :gender :masc}
             :sem {:pred :gianluca-e-giovanni
                   :human true}
             :propernoun true}}

   "Gianluca and Luisa"
   {:synsem {:agr {:number :plur
                   :person :3rd
                   :gender :masc}
             :sem {:pred :gianluca-e-luisa
                   :human true}
             :propernoun true}}

   "Giovanni and I"
   [{:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:gender :masc
                    :person :1st
                    :number :plur}
              :sem {:human true
                    :pred :giovanni-and-i}
              :subcat '()}}]

      ;; TODO: account for "give" being ditransitive.
   "give" {:synsem {:cat :verb
                    :sem {:pred :give}}
           :english {:past "gave"}}

   "go"
   {:synsem {:cat :verb
              :sem {:activity true
                    :discrete false
                    :pred :go
                    :subj {:animate true}}}
    :english {:past "went"}}

   "go downstairs"
   {:synsem {:cat :verb
              :sem {:activity true
                    :discrete false
                    :pred :go-downstairs
                    :subj {:animate true}}}
    :english {:past "went downstairs"
              :participle "going downstairs"
              :present {:3sing "goes downstairs"}}}
   
   "go out"
   {:synsem {:cat :verb
              :sem {:activity true
                    :discrete false
                    :pred :go-out
                    :subj {:animate true}}}
    :english {:past "went out"
              :participle "going out"
              :present {:3sing "goes out"}}}
   
   "go upstairs"
   {:synsem {:cat :verb
              :sem {:activity true
                    :discrete false
                    :pred :go-upstairs
                    :subj {:animate true}}}
    :english {:past "went upstairs"
              :participle "going upstairs"
              :present {:3sing "goes upstairs"}}}
   
   "grab"  {:synsem {:cat :verb
                     :sem {:pred :prendere}}
            :english {:participle "grabbing"
                      :past "grabbed"}}

   "guess" {:synsem {:cat :verb
                     :sem {:pred :guess}}}
   
   ;; TODO: add auxiliary sense of "have"
   "have" {:synsem {:cat :verb
                    :sem {:activity false
                          :discrete false
                          :pred :have
                          :subj {:human true}
                          :obj {:buyable true}}}
           :english {:present {:3sing "has"}
                     :past "had"}}

   "have dinner" {:synsem {:cat :verb
                           :sem {:pred :have-dinner}}
                  :english {:present {:3sing "has dinner"}
                            :past "had dinner"
                            :participle "having dinner"}}
   "have fun"
   (let [subject-semantics (atom {:human true})]
     {:synsem {:cat :verb
               :sem {:pred :have-fun
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "having fun"
                :present {:3sing "has fun"}
                :past "had fun"}})
   
   "have lunch" {:synsem {:cat :verb
                            :sem {:pred :have-lunch}}
                   :english {:present {:3sing "has lunch"}
                             :past "had lunch"
                             :participle "having lunch"}}
   
   "have to" {:synsem {:cat :verb
                       :sem {:pred :have-to}}
              :english {:present {:1sing "have to"
                                  :2sing "have to"
                                  :3sing "has to"
                                  :1plur "have to"
                                  :2plur "have to"
                                  :3plur "have to"}
                        :future "have to"
                        :participle "having to"
                        :past "had to"}}
   "he" {:synsem {:cat :noun
                  :pronoun true
                  :case :nom
                  :agr {:person :3rd
                        :gender :masc
                        :number :sing}
                  :sem {:human true
                        :pred :lui}
                  :subcat '()}}
   "help"
   {:synsem {:cat :verb
             :sem {:pred :aiutare
                   :activity true
                   :obj {:human true}}
             :subcat {:1 {:cat :noun}
                      :2 {:cat :noun
                          :sem {:human true}}}}}
                          
   "her"
   [{:synsem {:cat :det
              :agr {:number :sing}
              :sem {:pred :lei
                    :gender :fem
                    :number :sing}
              :def :possessive}}
    {:synsem {:cat :det
              :agr {:number :plur}
              :sem {:pred :lei
                    :gender :fem
                    :number :sing}
              :def :possessive}}
    {:synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive false
              :agr {:person :3rd
                    :gender :fem
                    :number :sing}
              :sem {:pred :lei
                    :human true}
              :subcat '()}}]

   "herself" {:synsem {:cat :noun
                       :pronoun true
                       :case :acc
                       :reflexive true
                       :agr {:person :3rd
                             :gender :fem
                   :number :sing}
                       :sem {:human true
                             :reflexive true}
                       :subcat '()}}
    "him" {:synsem {:cat :noun
                    :pronoun true
                    :case :acc
                    :reflexive false
                    :agr {:person :3rd
                          :gender :masc
                          :number :sing}
                    :sem {:human true
                          :pred :lui}
                    :subcat '()}}
   
   "himself" {:synsem {:cat :noun
                       :pronoun true
                       :case :acc
                       :reflexive true
                       :agr {:person :3rd
                             :gender :masc
                   :number :sing}
                       :sem {:human true
                             :reflexive true}
                       :subcat '()}}

   "his" [{:synsem {:cat :det
                    :agr {:number :sing}
                    :sem {:pred :lui
                          :human true
                          :gender :masc
                          :number :sing}
                    :def :possessive}}
          {:synsem {:cat :det
                    :agr {:number :plur}
                    :sem {:pred :lui
                          :human true
                          :gender :masc
                          :number :sing}
                    :def :possessive}}]
                 
   "hit" {:english {:past "hit"}
             :synsem {:cat :verb
                      :sem {:pred :hit}}}
   "hold"
   {:synsem {:cat :verb
             :sem {:pred :hold}}
    :english {:past "held"}}

   "hope"
   {:synsem {:cat :verb
             :sem {:pred :hope}}}
   
   "house" {:synsem {:cat :noun
                     :sem {:pred :house
                           :artifact true
                           :place true}}}
   "hug"
   {:synsem {:cat :verb
             :sem {:pred :hug
                   :subj {:human true}
                   :obj {:animate true}}}
    :english {:past "hugged"
              :participle "hugging"}}
            
   "hurt" (let [common {:english {:past "hurt"}
                        :synsem {:cat :verb}}]
            ;; 1. reflexive sense of "hurt"
            [(let [subject-semantics (atom {:human true})
                   subject-agr (atom :top)]

               (merge common
                      {:synsem {:sem {:pred :hurt-oneself
                                      :subj subject-semantics
                                      :obj subject-semantics}
                                :subcat {:1 {:agr subject-agr
                                             :sem subject-semantics}
                                         :2 {:agr subject-agr
                                             :pronoun true
                                             :reflexive true
                                             :sem subject-semantics}}}}))

             ;; 2. transitive sense of "hurt"
             (merge common
                    {:synsem {:sem {:pred :hurt
                                    ;; TODO: consider making lexicon post-processing rule:
                                    ;; if not(reflexive=true) => reflexive=false
                                    :reflexive false
                                    :obj {:animate true}}}})])

   "Jean" {:synsem {:sem {:pred :Jean
                          :human true}
                    :propernoun true
                    :agr {:number :sing
                          :person :3rd
                          :gender :masc}}}

   "Juan" {:synsem {:sem {:pred :Juan
                          :human true}
                    :propernoun true
                    :agr {:number :sing
                          :person :3rd
                          :gender :masc}}}
   "Juana" {:synsem {:sem {:pred :Juana
                          :human true}
                     :propernoun true
                     :agr {:number :sing
                           :person :3rd
                           :gender :fem}}}
   "Juan and I"
   [{:synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:gender :masc
                    :person :1st
                    :number :plur}
              :sem {:human true
                    :pred :Juan-and-i}
              :subcat '()}}]        
   "Juan and me"
   [{:synsem {:cat :noun
              :pronoun true
              :case :acc
              :agr {:gender :masc
                    :person :1st
                    :number :plur}
              :sem {:human true
                    :pred :Juan-and-i}
              :subcat '()}}]        
   "I" 
   [{:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:gender :masc
                    :person :1st
                    :number :sing}
              :sem {:human true
                    :pred :I}
             :subcat '()}}

    {:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:gender :fem
                    :person :1st
                    :number :sing}
              :sem {:human true
                    :pred :I}
              :subcat '()}}]

   "if"   {:synsem {:cat :comp
                    :sem {:pred :that}
                    :subcat {:1 {:cat :verb
                                 :comp-type :if
                                 :subcat '()}
                             :2 '()}}}

   "imagine" {:synsem {:cat :verb
                       :sem {:pred :imagine
                             :subj {:human true}}}}

   "import" {:synsem {:cat :verb
                :sem {:pred :import}}}

   "improve" {:synsem {:cat :verb
                :sem {:pred :improve}}}

   "increase" {:synsem {:cat :verb
                        :sem {:pred :increase}}}

   "insist" {:synsem {:cat :verb
                      :sem {:pred :insist}}}

   "insure" {:synsem {:cat :verb
                      :sem {:pred :insure}}}

   "intelligent" (unify adjective
                        {:synsem {:cat :adjective
                                  :sem {:mod {:pred :intelligent}
                                        :human true
                                        :comparative false}
                                  :subcat {:1 {:cat :det}
                                           :2 '()}}})
   "interrupt" {:synsem {:cat :verb
                :sem {:pred :interrupt}}}
   "it"
   [{:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :top ;; we could just omit this kv, but we explicitly
              ;; set it to :top to show how it's different than other
              ;; pronouns: like 'you' the nominative and accusative
              ;; are the same (compare versus 'I'/'me', 'she'/'her', etc)
              :agr {:person :3rd
                    :number :sing}
              :sem {:pred :lui
                    :gender :masc
                    :human false}
              :subcat '()}}

    {:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :top ;; see above comment
              :agr {:person :3rd
                    :number :sing}
              :sem {:pred :lei
                    :gender :fem
                    :human false}
              :subcat '()}}]

   "itself"
   [{:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :3rd
                    :gender :masc
                    :number :sing}
              :sem {:human false
                    :pred :lui}
              :subcat '()}}
    {:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :3rd
                    :gender :fem
                    :number :sing}
              :sem {:human false
                    :pred :lei}
              :subcat '()}}]

   "keep"
   [{:synsem {:cat :verb
              :sem {:pred :tenere}}
     :english {:past "kept"}}
    {:synsem {:cat :verb
              :sem {:pred :keep-safe}}
     :english {:note "(something safe)"
               :past "kept"}}]

   "key" {:synsem {:cat :noun
                   :sem {:pred :key}}}

   "kill" {:synsem {:cat :verb
                    :sem {:pred :kill}}}

   "learn" {:synsem {:cat :verb
                     :sem {:pred :learn}}}

   "leave" [{:english {:past "left"}
             :synsem {:cat :verb
                      :sem {:pred :leave-behind
                            :subj {:animate true}
                            :obj {:place false}}}}
            
            {:english {:note "on a trip"
                       :past "left"}
             :synsem {:cat :verb
                      :sem {:pred :leave
                            :subj {:animate true}}}}]

   "lie" {:synsem {:cat :verb
                   :sem {:pred :lie}}}
   
   "lift" {:synsem {:cat :verb
                    :sem {:pred :lift}}}
             
   "light" {:synsem {:cat :verb
                     :sem {:pred :light}}}
   
   "listen to" {:synsem {:cat :verb
                         :sem {:pred :listen-to}}
                :english {:participle "listening to"
                          :past "listened to"
                          :present {:3sing "listens to"}}}

   "live" {:synsem {:cat :verb
                    :sem {:subj {:animate true}
                          :pred :live}}}

   "look" {:synsem {:cat :verb
                    :sem {:pred :look}}}

   "look for" {:synsem {:cat :verb
                        :sem {:pred :cercare}
                        :subcat {:1 {:cat :noun
                                     :sem {:animate true}}
                                 :2 {:cat :noun}}}
               :english {:participle "looking for"
                         :past "looked for"
                         :present {:3sing "looks for"}}}

   "look up" {:synsem {:cat :verb
                       :sem {:pred :cercare}}
              :english {:participle "looking up"
                        :past "looked up"
                        :present {:3sing "looks up"}}}

   "lose" {:english {:participle "losing"
                     :past "lost"}
           :synsem {:cat :verb
                    :sem {:pred :lose}}}

   "love" {:synsem {:cat :verb
                    :sem {:pred :amare
                          :subj {:human true}}}}

   "lower" {:synsem {:cat :verb
                     :sem {:pred :lower}}}
   "Luisa"
   {:synsem {:sem {:pred :luisa
                   :human true}
             :agr {:number :sing
                   :person :3rd
                   :gender :fem}
             :propernoun true}}

  "Luisa and I"
   [{:english {:note "♂"}
     :synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:gender :masc
                   :person :1st
                   :number :plur}
             :sem {:human true
                   :pred :luisa-and-i}
             :subcat '()}}
    {:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
             :agr {:gender :fem
                   :person :1st
                   :number :plur}
              :sem {:human true
                    :pred :luisa-and-i}
              :subcat '()}}]

   "make" {:synsem {:cat :verb
                    :sem {:pred :make}}
           :english {:past "made"}}
   
   "man" {:english {:plur "men"}
          :synsem {:agr {:gender :masc}
                   :cat :noun
                   :sem {:human true
                         :pred :man
                         :child false}}}

   "manage" {:synsem {:cat :verb
                :sem {:pred :manage}}}
   "may" {:english {:past "might"
                    :participle "able to"
                    :present {:1sing "may"
                              :2sing "may"
                              :3sing "may"
                              :1plur "may"
                              :2plur "may"
                              :3plur "may"}
                    :future "be able to"
                    :conditional "be able to"}
          :synsem {:cat :verb
                   :sem {:pred :may}}}

   "me" {:synsem {:cat :noun
                  :pronoun true
                  :case :acc
                  :reflexive false
                  :agr {:person :1st
                   :number :sing}
                  :sem {:human true
                        :pred :I}
                  :subcat '()}}

   "measure" {:synsem {:cat :verb
                       :sem {:pred :measure}}}

   "meet"  {:synsem {:cat :verb
                     :sem {:pred :incontrare}}
            :english {:past "met"}}

   "meeting" {:synsem {:cat :noun
                       :sem {:pred :meeting
                             :event true}}}

   "mother" {:synsem {:agr {:gender :fem}
                      :cat :noun
                      :sem {:human true
                            :pred :madre
                            :child false}}}

   "move" {:synsem {:cat :verb
                    :sem {:pred :move}}}

   "multiply" {:synsem {:cat :verb
                        :sem {:pred :multiply}}}

   ;; TODO: should not need to provide an irregular plural form
   ;; [:sem :mass]=true should be sufficient.
   "music" {:synsem {:agr {:number :sing}
                     :cat :noun
                     :sem {:pred :music
                           :animate false
                           :physical-object false
                           :place false
                           :mass true}}}
   "my"
   (map #(unify %
                {:synsem {:cat :det
                          :sem {:pred :I
                                :human true
                                :number :sing}
                          :def :possessive}})
        [{:synsem {:agr {:number :sing}}}
         {:synsem {:agr {:number :plur}}}])
   
   "myself" 
   {:synsem {:cat :noun
             :pronoun true
             :case :acc
             :reflexive true
             :agr {:person :1st
                   :number :sing}
             :sem {:human true
                   :pred :I}
             :subcat '()}}
   "name"
   (let [gender (atom :top)
         number (atom :top)
         agr (atom {:gender gender
                    :number number})
         of (atom {:gender gender
                   :number number})]
     {:synsem {:agr agr
               :cat :noun
               :sem {:animate false
                     :pred :name
                     :place false
                     :physical-object false
                     :subj of}
               :subcat {:1 {:agr agr
                            :cat :det
                            :def :possessive
                            :sem of}}}})

   "note" {:synsem {:cat :verb
                    :sem {:pred :note}}}

   "observe" {:synsem {:cat :verb
                :sem {:pred :observe}}}

   "obtain" {:synsem {:cat :verb
                :sem {:pred :obtain}}}

   "organize" {:synsem {:cat :verb
                :sem {:pred :organize}}}

   "our"
   (map #(unify %
                {:synsem {:cat :det
                          :agr {:gender :masc}
                          :sem {:pred :noi
                                :number :plur}
                          :def :possessive}})
        [{:synsem {:agr {:number :sing}}}
         {:synsem {:agr {:number :plur}}}])

   "ourselves"
   [{:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :1st
                    :number :plur
                    :gender :fem}
              :sem {:human true
                    :pred :noi}
              :subcat '()}}
    
    {:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :1st
                    :number :plur
                    :gender :masc}
              :sem {:human true
                    :pred :noi}
              :subcat '()}}]
   
   "paint"  {:synsem {:cat :verb
                      :sem {:pred :paint}}}

   "participate"  {:synsem {:cat :verb
                            :sem {:pred :participate}}}

   "party" [{:synsem {:cat :noun
                      :sem {:pred :party
                            :place true}}}
            {:synsem {:cat :noun
                      :sem {:pred :party
                           :event true}}}]
   
   ;; TODO: 3sing present exception used below to avoid "playies" is not an exception: it's a rule: y->ys.
   ;; the exceptional case is when "ys" is not used (e.g. "tries").
   "play" [{:comment "We are talking about playing games or sports."
            :english {:note "⚽"}
            :synsem {:cat :verb
                     :sem {:pred :giocare
                           :subj {:human true}
                           :obj {:human false
                                 :games true}}}}

           {:comment "We are talking about playing music or sounds."
            :english {:note "🎼"}
            :synsem {:cat :verb
                     :sem {:pred :suonare
                           :subj {:human true}
                           :obj {:human false
                                 :music true}}}}]

   ;; TODO: all reflexive verbs should have this subject-object agreement like this one does:
   "prepare" (let [subject-semantics (atom {:human true})
                   subject-agreement (atom :top)]
               {:synsem {:cat :verb
                         :sem {:pred :get-ready
                               :subj subject-semantics
                               :obj subject-semantics}
                         :subcat {:1 {:sem subject-semantics
                                      :agr subject-agreement
                                      }
                                  :2 {:pronoun true
                                      :reflexive true
                                      :agr subject-agreement
                                      :sem subject-semantics}}}})
   "preserve" {:synsem {:cat :verb
                        :sem {:pred :preserve}}}

   "print"  {:synsem {:cat :verb
                      :sem {:pred :stampare}}}

   "professor" {:synsem {:agr :top
                         :cat :noun
                         :sem {:human true
                               :pred :professor}}}

   "pupil" {:synsem {:agr :top
                     :cat :noun
                     :sem {:human true
                           ;; example of a synonym, where we use the convention of making the
                           ;; :pred (i.e. :student) the more common case (c.f. "student")
                           :pred :student}}}
   
   "put" {:english {:past "put"
                    :participle "putting"}
          :synsem {:cat :verb
                   :sem {:pred :put}}}
   
   "read" ;; if this was a phonetic dictionary, there would be two entries for each pronounciation (i.e. both "reed" or "red" pronounciations)
   {:english {:past {:english "read"
                     :note "past tense"}}
    :synsem {:cat :verb
             :sem {:pred :leggere
                   :discrete false
                   :subj {:human true}
                   :obj {:legible true}}}}
   
   "receive"  {:synsem {:cat :verb
                        :sem {:pred :ricevere}}}

   "reciprocate" {:synsem {:cat :verb
                           :sem {:pred :reciprocate}}}

   "recognize" {:synsem {:cat :verb
                :sem {:pred :recognize}}}

   "recount" {:synsem {:cat :verb
                :sem {:pred :recount}}}

   "recover" {:synsem {:cat :verb
                :sem {:pred :recover}}}
   "red"
   (unify adjective
          {:synsem {:cat :adjective
                    :sem {:mod {:pred :rosso}
                          :comparative false
                          :physical-object true
                          :human false}}})

   "remain" [{:synsem {:cat :verb
                       :sem {:pred :remain}}} ;; for other than italian
                    
             ;; these two below are for Italian.          
             
             {:english {:note {:it "ri"}}
              :synsem {:cat :verb
                       :sem {:pred :remain1}}}
             {:english {:note {:it "re"}}
              :synsem {:cat :verb
                       :sem {:pred :remain2}}}]

   "remember"  {:synsem {:cat :verb
                         :sem {:pred :ricordare}}}

   "reserve" {:synsem {:cat :verb
                :sem {:pred :reserve}}}

   "respond"  {:synsem {:cat :verb
                        :sem {:pred :answer}}}

   "rest" {:synsem {:cat :verb
                :sem {:pred :rest}}}

   "return" [{:synsem {:cat :verb
                       :sem {:pred :ritornare}}}
             {:synsem {:cat :verb
                       :sem {:pred :tornare}}}
             {:synsem {:cat :verb
                       :sem {:pred :giveback-return}}
              :english {:note "give back"}}]

   "run" {:english {:past "ran"
                    :participle "running"
                    :past-participle "run"}
          :synsem {:cat :verb
                   :sem {:pred :run}}}

   "s" [(let [of (atom :top)]
          {:synsem {:agr {:number :sing}
                    :cat :det
                    :def :genitive
                    :subcat {:1 {:cat :noun
                                 :pronoun false
                                 :sem of
                                 :subcat '()}}
                    :sem {:pred :of
                          :of of}}})
        (let [of (atom :top)]
          {:synsem {:agr {:number :plur}
                    :cat :det
                    :def :genitive
                    :sem {:pred :of
                          :of of}
                    :subcat {:1 {:cat :noun
                                 :pronoun false
                                 :sem of
                                 :subcat '()}}}})]
         
   "say" {:english {:past "said"}
          :synsem {:cat :verb
                   :sem {:pred :say}}}

   "scold" {:synsem {:cat :verb
                :sem {:pred :scold}}}

   ;; TODO: search _within_ or _on_: depends on the object.
   ;;   "search"  {:synsem {:sem {:pred :cercare}}})

   "scrub"  {:synsem {:cat :verb
                     :sem {:pred :scrub}}
             :english {:participle "scrubbing"
                       :past "scrubbed"}}

   "second" (unify adjective
                   {:synsem {:sem {:mod {:pred :first}
                                   :comparative false}}})
   "see"  {:synsem {:cat :verb
                    :sem {:pred :see}
                    :subcat {:1 {:cat :noun
                                 :sem {:animate true}}
                             :2 {:cat :noun
                                 :sem {:physical-object true}}}}
           :english {:past "saw"
                     :past-participle "seen"}}

   "sell" {:synsem {:cat :verb
                    :sem {:pred :vendere
                          :subj {:human true}
                          :obj {:buyable true}}}
           :english {:past "sold"}}

   "send" {:synsem {:cat :verb
                    :sem {:pred :send}}
           :english {:past "sent"}}

   "set" {:synsem {:cat :verb
                   :sem {:pred :set}}
          :english {:past {:english "set"
                           :note "past tense"}}}

   "share" {:synsem {:cat :verb
                     :sem {:pred :share}}}
   "she"
   {:synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :3rd
                   :gender :fem
                   :number :sing}
             :sem {:human true
                   :pred :lei}
             :subcat '()}}

   "show" {:synsem {:cat :verb
                    :sem {:pred :show
                          :past-participle "shown"}}}

   "sigh" {:synsem {:cat :verb
                :sem {:pred :sigh}}}

   "sing" {:synsem {:cat :verb
                    :sem {:pred :sing}}
           :english {:past "sang"}}
   
   "sit down" {:english {:past "sat down"
                         :participle "sitting down"
                         :past-participle "sat down"
                         :present {:3sing "sits down"}}
               :synsem {:cat :verb
                        :sem {:pred :sit-down}}}

   "sleep" {:synsem {:cat :verb
                     :sem {:subj {:animate true}
                           :discrete false
                           :pred :sleep}}
            :english {:past "slept"}}
   "small"
   (unify adjective
          {:synsem {:cat :adjective
                    :sem {:mod {:pred :small}
                          :comparative false}
                    :subcat {:1 {:cat :det}
                             :2 '()}}})
  
   "snap" {:synsem {:cat :verb
                    :sem {:pred :snap-pictures}}
           :english {:past "snapped"
                     :participle "snapping"
                     :note "pictures"}}
   "some"
   [{:synsem {:cat :det
              :def :partitivo
              :sem {:pred :some-of}
              :agr {:number :plur}}}
    {:synsem {:cat :det
              :def :indef
              :sem {:pred :some-one-of}
              :agr {:number :sing}}}]
   "speak"
   {:english {:past "spoke"
              :past-participle "spoken"}
    :synsem {:cat :verb
             :sem {:pred :speak
                   :subj {:human true}
                   :obj {:speakable true}}}}

   "start" {:synsem {:cat :verb
                     :sem {:pred :start}}}
   
   "stay" {:synsem {:cat :verb
                     :sem {:pred :stay}}}

   "stain" {:synsem {:cat :verb
                :sem {:pred :stain}}}

   "steal" {:synsem {:cat :verb
                     :sem {:pred :steal}}
            :english {:past "stole"}}

   "strike" {:english {:past "struck"}
             :synsem {:cat :verb
                      :sem {:pred :strike}}}

   "student" {:synsem {:agr :top
                       :cat :noun
                       :sem {:human true
                             :pred :student}}}

   "study"  {:synsem {:cat :verb
                      :sem {:pred :study}}
             :english {:past "studied"}}

   "stupid" (unify adjective
                   {:synsem {:cat :adjective
                             :sem {:mod {:pred :stupid}
                                   :human true
                                   :comparative false}
                             :subcat {:1 {:cat :det}
                                      :2 '()}}})
   "supply" {:synsem {:cat :verb
                :sem {:pred :supply}}}

   "support" {:synsem {:cat :verb
                       :sem {:pred :support}}}

   "take" (let [common {:synsem {:cat :verb}
                        :english {:past "took"
                                  :past-participle "taken"}}]
            [(unify common
                    {:synsem {:sem {:pred :grab}}
                     :english {:note "grab"}})
             (unify common
                    {:synsem {:sem {:pred :take}}})])

   "take advantage of" {:english {:past "took advantage of"
                                  :participle "taking advantage of"
                                  :past-participle "taken advantage of"
                                  :present {:3sing "takes advantage of"}}
                        :synsem {:cat :verb
                                 :sem {:pred :take-advantage-of}}}
   "talk"
   {:synsem {:cat :verb
             :sem {:pred :talk
                   :subj {:human true}}}}

   "tall"
   (unify adjective
          {:synsem {:cat :adjective
                    :sem {:mod {:pred :tall}
                          :human true
                          :comparative false}
                    :subcat {:1 {:cat :det}
                             :2 '()}}})
   "short"
   [(unify adjective
           {:synsem {:cat :adjective
                     :sem {:mod {:pred :short}
                           :comparative false
                           :physical-object true}
                     :subcat {:1 {:cat :det}
                              :2 '()}}})
    (unify adjective
           {:synsem {:cat :adjective
                     :sem {:mod {:pred :short}
                           :comparative false
                           :event true}
                     :subcat {:1 {:cat :det}
                              :2 '()}}})]
      
   "teach"  {:synsem {:cat :verb
                      :sem {:pred :teach}}
                            :english {:past "taught"}}

   "telephone" {:synsem {:cat :verb
                         :sem {:pred :telefonare}}}

   "tell" {:english {:past "told"}
           :synsem {:cat :verb
                    :sem {:pred :tell}}}

   "that" [;; "that" as in "she thinks that .."
           {:synsem {:cat :comp
                     :sem {:pred :that}
                     :subcat {:1 {:cat :verb
                                  :comp-type :that
                                  :subcat '()}
                              :2 '()}}}
           ;; "that" as in "that woman"
           {:synsem {:cat :det
                     :agr {:number :sing}
                     :sem {:pred :demonstrative}
                     :def :def}}]

   "the" (map #(unify % 
                      {:synsem {:cat :det
                                :sem {:pred :definite}
                                :def :def}})
              [{:synsem {:agr {:number :sing}}}
               {:synsem {:agr {:number :plur}}}])

   "their"
   (map #(unify %
                {:synsem {:cat :det
                          :sem {:pred :loro
                                :number :plur}
                          :def :possessive}})
        [{:synsem {:agr {:number :sing}}}
         {:synsem {:agr {:number :plur}}}])
    
   "themselves"
   [{:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :3rd
                    :number :plur
                    :gender :fem}
              :sem {:human true
                    :reflexive true}
              :subcat '()}}
    
    {:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :3rd
                    :number :plur
                    :gender :masc}
              :sem {:human true
                    :reflexive true}
              :subcat '()}}
    ]
   
   "they"
   [{:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :gender :masc
                    :number :plur}
              :sem {:gender :masc
                    :human true
                    :pred :loro}
              :subcat '()}}
    {:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :gender :masc
                    :number :plur}
              :sem {:gender :masc
                    :human false
                    :pred :loro}
              :subcat '()}}
    {:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :gender :fem
                    :number :plur}
              :sem {:gender :fem
                    :human true
                   :pred :loro}
              :subcat '()}}
    {:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :3rd
                    :gender :fem
                    :number :plur}
              :sem {:gender :fem
                    :human false
                    :pred :loro}
              :subcat '()}}]
            
   "think" (let [common {:synsem {:cat :verb
                                  :subcat {:1 {:cat :noun
                                               :sem {:human true}}}}
                         :english {:past "thought"}}]
             [(unify common
                     {:synsem {:sem {:pred :think}
                               :subcat {:2 '()}}}) ;; intransitive

              (unify common
                     {:synsem {:sem {:pred :think}
                               :subcat {:2 {:cat :comp
                                            :comp-type :comp
                                            :subcat '()}
                                        :3 '()}}})])
   "those" {:synsem {:cat :det
                     :agr {:number :plur}
                     :sem {:pred :demonstrative}
                     :def :def}}

   "throw" {:english {:past "threw"}
            :synsem {:cat :verb
                     :sem {:pred :throw}}}

   "throw out"
   {:synsem {:cat :verb
             :sem {:pred :throw-out}}
    :english {:past "threw out"
              :present {:3sing "throws out"}
              :participle "throwing out"}}

   "transfer" {:english {:past "transferred"
                         :participle "transferring"}
               :synsem {:cat :verb
                        :sem {:pred :transfer}}}

   "try" {:synsem {:cat :verb
                   :sem {:pred :try}}
          :english {:past "tried"}}
            
   "understand" {:english {:past "understood"}
                 :synsem {:cat :verb
                          :sem {:pred :understand}}}
                        
   "understand (deeply)" {:synsem {:cat :verb
                                   :sem {:pred :understand-deeply}}
                          :english {:present {:3sing "understands (deeply)"}
                                    :past "understood (deeply)"
                                    :participle "understanding (deeply)"}}                    
                        
   "understand (simply)" {:synsem {:cat :verb
                                   :sem {:pred :understand-simply}}
                          :english {:present {:3sing "understands (simply)"}
                                    :past "understood (simply)"
                                    :participle "understanding (simply)"}}
                           
   "upload"  {:synsem {:cat :verb
                       :sem {:pred :caricare}}}

   "use"  {:synsem {:cat :verb
                    :sem {:pred :usare}}}

   "wait"  {:synsem {:cat :verb
                     :sem {:pred :wait-for}}}
   "wake up"
   (let [subject-semantics (atom {:animate true})]
     {:synsem {:cat :verb
               :sem {:pred :wake-up
                     :subj subject-semantics
                     :obj subject-semantics}
               :subcat {:1 {:sem subject-semantics}
                        :2 '()}}
      :english {:participle "waking up"
                :present {:3sing "wakes up"}
                :past "woke up"}})

   "walk" {:synsem {:cat :verb
                :sem {:pred :walk}}}
              
   "want" {:synsem {:cat :verb
                    :sem {:pred :want}}}        

   "warm" {:synsem {:cat :verb
                :sem {:pred :warm}}}

   "warn" {:synsem {:cat :verb
                     :sem {:pred :warn}}}
   
   ;; TODO: rename {:pred :wash} to {:pred :wash-oneself} and add non-reflexive "wash" with {:pred :wash}.
   "wash" (let [subject-semantics (atom :top)]
            {:synsem {:cat :verb
                      :sem {:pred :wash
                            :reflexive true
                            :subj subject-semantics
                            :obj subject-semantics}
                      :subcat {:1 {:sem subject-semantics}
                               :2 {:pronoun true
                                   :reflexive true
                                   :sem subject-semantics}}}})
   "waste" {:synsem {:cat :verb
                :sem {:pred :waste}}}

   "watch" {:synsem {:cat :verb
                    :sem {:pred :watch}}}
   "we"
   [{:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :nom
              :agr {:person :1st
                    :gender :fem
                    :number :plur}
              :sem {:human true
                    :gender :fem
                    :pred :noi}
              :subcat '()}}
    {:english {:note "♂"}
     :synsem {:cat :noun
             :pronoun true
             :case :nom
             :agr {:person :1st
                   :gender :masc
                   :number :plur}
             :sem {:human true
                   :gender :masc
                   :pred :noi}
             :subcat '()}}]

   "wear"  {:english {:past "wore"}
            :synsem {:cat :verb
                     :sem {:pred :wear}}}
   "whether"
   {:synsem {:cat :comp
             :sem {:pred :that}
             :subcat {:1 {:cat :verb
                          :comp-type :if
                          :subcat '()}
                      :2 '()}}}

   "win"  {:synsem {:cat :verb
                    :sem {:pred :win
                          :subj {:human true}
                          :obj {:human false}}}
           :english {:past "won"
                     :participle "winning"}}
   
   "woman" {:english {:plur "women"}
            :synsem {:agr {:gender :fem}
                     :cat :noun
                     :sem {:human true
                           :pred :donna
                           :child false}}}

   "wonder" {:synsem {:cat :verb
                      :sem {:pred :wonder}
                      :subcat {:1 {:cat :noun
                                   :sem {:human true}}
                               :2 {:cat :comp
                                   :comp-type :if
                                   :subcat '()}}}}
   "work" [{:synsem {:cat :verb
                     :sem {:pred :work-human
                           :subj {:human true}}}
            :english {:note "human"}}
           
           {:english {:note "nonliving or machines"} ;; TODO: add support in UI for :note.
            :synsem {:cat :verb
                     :sem {:subj {:living false
                                  :human false ;; should not need to add human=false and animate=false: living=false should suffice.
                                  :animate false}
                           :pred :work-nonhuman}}}]
   
   "write"  {:english {:past "wrote"
                       :past-participle "written"}
             :synsem {:cat :verb
                      :sem {:pred :scrivere}}}

   "yell" {:synsem {:cat :verb
                    :sem {:pred :yell}}}
   
   "you"
   [{:english {:note "♂"}
     :target :it ;; Italian makes gender distinction for agreement with verbs and adjectives..
     :synsem {:cat :noun
              :pronoun true
              :reflexive false
              :case :top ;; see comment in "it" about :case.
              :agr {:person :2nd
                    :gender :masc
                    :number :sing}
              :sem {:human true
                    :pred :tu}
              :subcat '()}}
    {:english {:note "♀"}
     :target :it ;; Italian makes gender distinction for agreement with verbs and adjectives..
     :synsem {:cat :noun
              :pronoun true
              :case :top ;; see comment in "it" about :case.
              :reflexive false
              :agr {:person :2nd
                    :gender :fem
                    :number :sing}
              :sem {:human true
                    :pred :tu}
              :subcat '()}}

    {:english {:note "♀"}
     :target :es ;; ..but Spanish does not.
     :synsem {:cat :noun
              :pronoun true
              :case :top ;; see comment in "it" about :case.
              :reflexive false
              :agr {:person :2nd
                    :gender :fem
                    :number :sing}
              :sem {:human true
                    :pred :tu}
              :subcat '()}}]

   "you all"
   [{:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :reflexive false
              :case :top ;; see comment in "it" about :case.
              :agr {:person :2nd
                    :gender :masc
                    :number :plur}
              :sem {:human true
                    :reflexive false
                    :pred :voi}
              :subcat '()}}
    {:english {:note "♀"}
     :synsem {:cat :noun
             :pronoun true
             :reflexive false
             :case :top ;; see comment in "it" about :case.
             :agr {:person :2nd
                   :gender :fem
                   :number :plur}
             :sem {:human true
                   :reflexive false
                   :pred :voi}
             :subcat '()}}]
   "your"
   (map #(unify %
                {:synsem {:cat :det
                          :sem {:pred :tu}
                          :def :possessive}})
        [{:synsem {:agr {:number :sing}}}
         {:synsem {:agr {:number :plur}}}])
   
   "yourself"
   [{:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :2nd
                    :number :sing
                    :gender :fem}
              :sem {:human true
                   :pred :tu}
              :subcat '()}}
    
    {:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :2nd
                    :number :sing
                    :gender :masc}
              :sem {:human true
                    :pred :tu}
              :subcat '()}}]

   "yourselves"
   [{:english {:note "♀"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :2nd
                    :number :plur
                    :gender :fem}
              :sem {:human true
                    :pred :voi}
              :subcat '()}}
    
    {:english {:note "♂"}
     :synsem {:cat :noun
              :pronoun true
              :case :acc
              :reflexive true
              :agr {:person :2nd
                    :number :plur
                    :gender :masc}
              :sem {:human true
                    :pred :voi}
              :subcat '()}}]
   })

(def lexicon (-> (compile-lex lexicon-source 
                              morph/exception-generator 
                              morph/phonize morph/english-specific-rules)
                 ;; make an intransitive version of every verb which has an
                 ;; [:sem :obj] path.
                 intransitivize

                 ;; if verb does specify a [:sem :obj], then fill it in with subcat info.
                 transitivize
                         
                 ;; if a verb has an object,
                 ;; and the object is not {:reflexive true}
                 ;; then the object is {:reflexive false}
                 (if-then {:synsem {:cat :verb
                                    :subcat {:2 {:reflexive false}}}}
                          {:synsem {:subcat {:2 {:reflexive false}}}})

                 ;; if a verb has an object,
                 ;; and the object is {:cat :noun},
                 ;; then the object is {:synsem {:case :acc}}.
                 (if-then {:synsem {:cat :verb
                                    :subcat {:2 {:cat :noun}}}}
                          {:synsem {:subcat {:2 {:case :acc}}}})

                 ;; if not(reflexive), then reflexive = false.
                 (if-then {:synsem {:cat :verb
                                    :sem {:reflexive false}}}
                          {:synsem {:sem {:reflexive false}}})

                 ;; if not(aux), then aux=false
                 (if-then {:synsem {:cat :verb
                                    :aux false}}
                          {:synsem {:aux false}})
                 
                 ;; subject-and-reflexive-pronoun agreement
                 (if-then {:synsem {:sem {:reflexive true}
                                    :cat :verb
                                    :subcat {:1 {:agr :top}
                                             :2 {:agr :top}}}}
                                          
                          (let [subject-agr (atom :top)]
                            {:synsem {:subcat {:1 {:agr subject-agr}
                                               :2 {:agr subject-agr}}}}))))

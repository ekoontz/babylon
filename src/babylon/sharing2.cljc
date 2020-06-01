(ns babylon.sharing2)

(def cons-and-nest
  (let [one (atom :top)
        two (atom :top)
        three (atom :top)
        four (atom {:mod {:first one
                          :rest two}})]
    {:sem four
     :mods-done? true
     :comp {:nest? true
            :sem one
            :head-sem three
            :parent-sem four}
     :head {:sem three
            :mods-done? false
            :mod two}}))

(def cons-and-no-nest
  (let [one (atom :top)
        two (atom :top)
        three (atom :top)]
    {:mods-done? false
     :mod {:first one
           :rest two}
     :comp {:nest? false
            :sem one
            :parent-sem three}
     :head {:mod two
            :mods-done? false}}))

(def nocons-and-nest
  (let [two (atom :top)
        three (atom :top)
        four (atom :top)]
    {:mods-done? true
     :sem {:mod two}
     :comp {:head-sem three
            :parent-sem four}
     :head {:sem three
            :mod two}}))

(def unify-with-this-for-each-of-the-above
  (let [one (atom :top)
        two (atom :top)]
    {:head-sem one
     :parent-sem two
     :head {:head-sem one
            :parent-sem two}}))


(ns dodonpascii.levels
  (:use [dodonpascii.attack]))

(def all-levels
  {1
    {2    {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -100 0]
                         [200 -100 0]]}
     6    {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -200 0]
                         [200 -100 0]
                         [300 -200 0]]}
     10   {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -200 0]
                         [200 -100 0]
                         [300 -100 0]
                         [400 -200 0]]}
     14   {:type        :biplane
           :powerup-opportunity     true
           :make-attack-fn   make-biplane-fn
           :init-coords [[1200 500 90]
                         [1325 500 90]
                         [1450 500 90]
                         [1575 500 90]
                         [1700 500 90]]}
     18   {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -200 0]
                         [200 -150 0]
                         [300 -100 0]
                         [400 -150 0]
                         [500 -200 0]]}
     22   {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -200 0]
                         [200 -150 0]
                         [300 -100 0]
                         [400 -150 0]
                         [500 -200 0]]}
     26   {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -200 0]
                         [200 -150 0]
                         [300 -100 0]
                         [400 -150 0]
                         [500 -200 0]]}
     30   {:type        :heli
           :make-attack-fn   make-heli-fn
           :init-coords [[100 -200 0]
                         [200 -150 0]
                         [300 -100 0]
                         [400 -150 0]
                         [500 -200 0]]}
     }}
  )


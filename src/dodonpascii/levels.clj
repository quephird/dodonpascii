(ns dodonpascii.levels
  (:use [dodonpascii.attack]))

(def all-levels
  {1
    {2    {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :right
           :init-coords [[100 -100 0]
                         [200 -100 0]]}
     7    {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :left
           :init-coords [[1200 -200 0]
                         [1100 -100 0]
                         [1000 -200 0]]}
     10  {:type        :biplane
           :powerup-opportunity     true
           :make-attack-fn   make-biplane-fn
           :dir         :right
           :init-coords [[0 300 -90]
                         [-125 300 -90]
                         [-250 300 -90]
                         [-375 300 -90]
                         [-500 300 -90]]}
     14   {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :right
           :init-coords [[100 -200 0]
                         [200 -100 0]
                         [300 -100 0]
                         [400 -200 0]]}
     18   {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :left
           :init-coords [[1200 -200 0]
                         [1100 -150 0]
                         [1000 -100 0]
                         [900 -150 0]
                         [800 -200 0]]}
     21   {:type        :biplane
           :powerup-opportunity     true
           :make-attack-fn   make-biplane-fn
           :dir         :left
           :init-coords [[1200 500 90]
                         [1325 500 90]
                         [1450 500 90]
                         [1575 500 90]
                         [1700 500 90]]}
     24   {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :right
           :init-coords [[100 -200 0]
                         [200 -150 0]
                         [300 -100 0]
                         [400 -150 0]
                         [500 -200 0]]}
     27   {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :left
           :init-coords [[1100 -200 0]
                         [1000 -150 0]
                         [900 -100 0]
                         [800 -150 0]
                         [700 -200 0]]}
     30   {:type        :heli
           :make-attack-fn   make-heli-fn
           :dir         :right
           :init-coords [[100 -200 0]
                         [200 -150 0]
                         [300 -100 0]
                         [400 -150 0]
                         [500 -200 0]]}
     }}
  )


(ns dodonpascii.levels
  "This module is where the levels and waves of the game are defined as well
   as provides some utility functions."
  (:use     [dodonpascii.bullets :as b]))

(def all-levels
  {1
    {:waves
      {2    {:type :heli,
             :init-coords [[100 -100 0 :right]
                           [200 -100 0 :right]]}
       5    {:type :heli,
             :init-coords [[1100 -100 0 :left]
                           [1000 -100 0 :left]
                           [900 -100 0 :left]]}
       8    {:type :heli,
             :init-coords [[100 -100 0 :right]
                           [200 -100 0 :right]
                           [300 -100 0 :right]]}
       10   {:type :biplane, :powerup-opportunity true,
             :init-coords [[0 300 -90 :right]
                           [-125 300 -90 :right]
                           [-250 300 -90 :right]
                           [-375 300 -90 :right]
                           [-500 300 -90 :right]]}
       13   {:type :heli,
             :init-coords [[100 -200 0 :right]
                           [200 -100 0 :right]
                           [300 -100 0 :right]
                           [400 -200 0 :right]]}
       16   {:type :heli,
             :init-coords [[1200 -200 0 :left]
                           [1100 -150 0 :left]
                           [1000 -100 0 :left]
                           [900 -150 0 :left]
                           [800 -200 0 :left]]}
       19   {:type :biplane, :powerup-opportunity true,
             :init-coords [[1200 500 90 :left]
                           [1325 500 90 :left]
                           [1450 500 90 :left]
                           [1575 500 90 :left]
                           [1700 500 90 :left]]}
       22   {:type :heli,
             :init-coords [[100 -200 0 :right]
                           [200 -150 0 :right]
                           [300 -100 0 :right]
                           [400 -150 0 :right]
                           [500 -200 0 :right]]}
       25   {:type :blue-plane,
             :init-coords [[200 0 -10 :right]
                           [300 -100 -10 :right]
                           [400 -200 -10 :right]
                           [500 -300 -10 :right]]}
       28   {:type :heli,
             :init-coords [[1100 -200 0 :left]
                           [1000 -150 0 :left]
                           [900 -100 0 :left]
                           [800 -150 0 :left]
                           [700 -200 0 :left]]}
       31   {:type :blue-plane,
             :init-coords [[700 -300 10 :left]
                           [800 -200 10 :left]
                           [900 -100 10 :left]
                           [1000 0 10 :left]]}
       34   {:type :heli,
             :init-coords [[100 -200 0 :right]
                           [200 -150 0 :right]
                           [300 -100 0 :right]
                           [400 -150 0 :right]
                           [500 -200 0 :right]]}
       37   {:type :large-plane,
             :init-coords [[200 -100 0 :right]
                           [1000 -100 0 :left]]}
       40   {:type :heli,
             :init-coords [[100 -150 0 :right]
                           [200 -100 0 :right]
                           [300 -150 0 :right]
                           [900 -150 0 :left]
                           [1000 -100 0 :left]
                           [1100 -150 0 :left]]}
       43   {:type :biplane, :powerup-opportunity true,
             :init-coords [[1200 500 90 :left]
                           [1325 500 90 :left]
                           [1450 500 90 :left]
                           [1575 500 90 :left]
                           [1700 500 90 :left]]}
       46   {:type :blue-plane,
             :init-coords [[200 -150 -10 :right]
                           [300 -100 -10 :right]
                           [900 -100 10 :left]
                           [1000 -150 10 :left]]}
       }
     :boss
       {:type            :bfp-5000,
        :status          nil
        :dir             :down,
        :init-coords     [600 -250 0]
        :hitbox-params   [[-225 -125 5][-100 -100 5][0 -150 5][100 -100 5][225 -125 5]]
        :bullet-patterns b/bfp-5000}
     :bg-objects
       [:tree :peony :dandelion]}})

; TODO: Need to return something other than nil here.
(defn get-next-enemy-spawn-time [levels current-level current-spawn-time]
  "Determines the next time that enemies should spawn."
  (let [next-spawn-times (->> (get-in all-levels [current-level :waves])
                           keys
                           (filter #(> % current-spawn-time)))]
    (if (empty? next-spawn-times)
      nil
      (apply min next-spawn-times))))

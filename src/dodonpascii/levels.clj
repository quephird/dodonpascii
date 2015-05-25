(ns dodonpascii.levels
  "This module is where the levels and waves of the game are defined as well
   as provides some utility functions.")

(def all-levels
  {1
    {:waves
      {2    {:type :heli, :dir :right,
             :init-coords [[100 -100 0][200 -100 0]]}
       5    {:type :heli, :dir :left,
             :init-coords [[1200 -200 0][1100 -100 0][1000 -200 0]]}
       8    {:type :heli, :dir :right,
             :init-coords [[100 -100 0][200 -100 0][300 -100 0]]}
       10   {:type :biplane, :dir :right, :powerup-opportunity true,
             :init-coords [[0 300 -90][-125 300 -90][-250 300 -90][-375 300 -90][-500 300 -90]]}
       13   {:type :heli, :dir :right,
             :init-coords [[100 -200 0][200 -100 0][300 -100 0][400 -200 0]]}
       16   {:type :heli, :dir :left,
             :init-coords [[1200 -200 0][1100 -150 0][1000 -100 0][900 -150 0][800 -200 0]]}
       19   {:type :biplane, :dir :left, :powerup-opportunity true,
             :init-coords [[1200 500 90][1325 500 90][1450 500 90][1575 500 90][1700 500 90]]}
       22   {:type :heli, :dir :right,
             :init-coords [[100 -200 0][200 -150 0][300 -100 0][400 -150 0][500 -200 0]]}
       25   {:type :blue-plane, :dir :right,
             :init-coords [[200 0 -10][300 -100 -10][400 -200 -10][500 -300 -10]]}
       26   {:type :tank, :dir :up,
             :init-coords [[100 900 180][200 900 180][300 900 180]]}
       28   {:type :heli, :dir :left,
             :init-coords [[1100 -200 0][1000 -150 0][900 -100 0][800 -150 0][700 -200 0]]}
       31   {:type :blue-plane, :dir :left,
             :init-coords [[700 -300 10][800 -200 10][900 -100 10][1000 0 10]]}
       34   {:type :heli, :dir :right,
             :init-coords [[100 -200 0][200 -150 0][300 -100 0][400 -150 0][500 -200 0]]}
       }
     :boss
       {:type            :bfp-5000,
        :dir             :down,
        :init-coords     [600 -250 0]
        :hitbox-params   [[-225 -125 5][-100 -100 5][0 -150 5][100 -100 5][200 -125 5]]}
     :bg-objects
       [:tree :peony :dandelion]}})

; TODO: Need to return something other than nil here.
(defn get-next-spawn-time [levels current-level current-spawn-time]
  "Determines the next time that enemies should spawn."
  (let [next-spawn-times (->> (get-in all-levels [current-level :waves])
                           keys
                           (filter #(> % current-spawn-time)))]
    (if (empty? next-spawn-times)
      nil
      (apply min next-spawn-times))))

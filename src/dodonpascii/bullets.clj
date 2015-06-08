(ns dodonpascii.bullets
  (:require [quil.core :as q :include-macros true]))

(def bfp-5000
  {5  {:n 10 :dt 0.25 :type :cone :init-coords [[0 -100 70][0 -100 90][0 -100 110]
                                                [-225 -100 70][-225 -100 90][-225 -100 110]
                                                [225 -100 70][225 -100 90][225 -100 110]]}
   10  {:n 10 :dt 0.25 :type :cone :init-coords [[0 -100 70][0 -100 90][0 -100 110]
                                                [-225 -100 70][-225 -100 90][-225 -100 110]
                                                [225 -100 70][225 -100 90][225 -100 110]]}
   15  {:n 10 :dt 0.25 :type :cone :init-coords [[0 -100 70][0 -100 90][0 -100 110]
                                                [-225 -100 70][-225 -100 90][-225 -100 110]
                                                [225 -100 70][225 -100 90][225 -100 110]]}
   20  {:n 10 :dt 0.25 :type :cone :init-coords [[0 -100 70][0 -100 90][0 -100 110]
                                                [-225 -100 70][-225 -100 90][-225 -100 110]
                                                [225 -100 70][225 -100 90][225 -100 110]]}
   })

(defn get-next-spawn-time [patterns current-spawn-time]
  "Determines the next time that bullets should spawn."
  (let [times            (keys patterns)
        next-spawn-times (->> patterns
                           keys
                           (filter #(> % current-spawn-time)))]
    (if (empty? next-spawn-times)
      nil
      (apply min next-spawn-times))))

(defn generate-boss-bullets [{{:keys [bullet-patterns status init-t x y]} :boss
                               next-spawn-time :next-boss-bullet-spawn-time
                               current-time :current-time :as state}]
  "Returns the game state with new bullets as prescribed in the boss wave"
  (let [seconds-into-boss-wave (* 0.001 (- current-time init-t))]
;    (println seconds-into-boss-wave next-spawn-time)
    (if (or (= :dead status)
            (< seconds-into-boss-wave next-spawn-time))
      state
      (let [{:keys [n dt type init-coords]} (get-in bullet-patterns [next-spawn-time])
            new-times        (map #(+ current-time (* 1000 dt %)) (range n))
            new-boss-bullets (for [t          new-times
                                   [dx dy ϕ]  init-coords]
                               {:type type
                                :init-t t
                                :x (+ x dx)
                                :y (+ y dy)
                                :ϕ (q/radians ϕ)
                                :θ 0})
            new-spawn-time   (get-next-spawn-time bullet-patterns next-spawn-time)
            new-events       (for [t new-times]
                               {:type :cone-shot :init-t t})]
        (-> state
          (assoc-in [:next-boss-bullet-spawn-time] new-spawn-time)
          (update-in [:events] concat new-events)
          (update-in [:enemy-bullets] concat new-boss-bullets))))))

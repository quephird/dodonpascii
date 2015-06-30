(ns dodonpascii.bullets
  (:require [quil.core :as q :include-macros true]))

(def bfp-5000
  {5  {:n 10 :dt 0.25 :type :cone :init-coords [[0 -100 70][0 -100 90][0 -100 110]
                                                [-225 -100 70][-225 -100 90][-225 -100 110]
                                                [225 -100 70][225 -100 90][225 -100 110]]}
   })

(defn generate-boss-bullets [{{:keys [bullet-patterns
                                      bullet-spawn-times
                                      status
                                      init-t
                                      x y]} :boss
                               current-time :current-time :as state}]
  "Returns the game state with new bullets as prescribed in the boss wave"
  (let [seconds-into-cycle (-> (- current-time init-t) (* 0.001) (mod 6))
        [next-spawn-time & new-spawn-times]  bullet-spawn-times]

    ; This seems like _such_ a hack but it's the only way
    ; I know to control when bullets are released. ¯\_(ツ)_/¯
    (if (or (= :dead status)
            (< 0.01 (q/abs (- seconds-into-cycle next-spawn-time))))
      state
      (let [{:keys [n dt type init-coords]} (get-in bullet-patterns [next-spawn-time])
            new-times        (map #(+ current-time (* 1000 dt %)) (range n))
            new-boss-bullets (for [t          new-times
                                   [dx dy ϕ]  init-coords]
                               {:type type
                                :init-t t
                                :grazed? false
                                :x (+ x dx)
                                :y (+ y dy)
                                :ϕ (q/radians ϕ)
                                :θ 0})
            new-events       (for [t new-times]
                               {:type :cone-shot :init-t t})]
        (-> state
          (assoc-in [:boss :bullet-spawn-times] new-spawn-times)
          (update-in [:events] concat new-events)
          (update-in [:enemy-bullets] concat new-boss-bullets))))))

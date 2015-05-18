(ns dodonpascii.motion
  (:require [quil.core :as q :include-macros true]))

(defn move-player [{{:keys [x y direction-x direction-y]} :player
                    w :w h :h :as state}]
  "Returns the game state with the player moved to a new position."
  (let [margin  50
        dx (* direction-x 5)
        dy (* direction-y 5)]
    (-> state
      (assoc-in [:player :x] (q/constrain (+ x dx) margin (- w margin)))
      (assoc-in [:player :y] (q/constrain (+ y dy) margin (- h margin))))))

(defn move-player-bullets [{w       :w
                            bullets :player-bullets :as state}]
  "Returns the game state with all player bullets moved to new positions."
  (let [dθ      10
        dr      10
        new-bullets  (->> bullets
                       (filter (fn [{x :x y :y}] (and (> y 0) (> x 0) (< x w))))
                       (map (fn [{ϕ :ϕ :as bullet}] (-> bullet
                                                       (update-in [:x] + (* dr (q/sin (q/radians ϕ))))
                                                       (update-in [:y] - (* dr (q/cos (q/radians ϕ))))
                                                       (update-in [:θ] + dθ)))))]
    (assoc-in state [:player-bullets] new-bullets)))

(defn move-power-ups [{w         :w
                       h         :h
                       power-ups :power-ups :as state}]
  "Returns the game state with all extant power ups moved to new positions."
  (let [new-power-ups  (->> power-ups
                         (filter (fn [{x :x y :y}] (and (< y h) (> x 0) (< x w))))
                         (map (fn [power-up] (-> power-up
                                               (update-in [:y] + 3)))))]
    (assoc-in state [:power-ups] new-power-ups)))

(defmulti move-enemy (fn [enemy state] (:type enemy)))

(defmethod move-enemy :tank [{old-t :t :as enemy}
                             {t :current-time}]
  (let [dt (* 0.001 (- t old-t))]
    (-> enemy
      (assoc-in [:t] t)
      (update-in [:y] - (* dt 40)))))


(defmethod move-enemy :default [{attack-fn :attack-fn :as enemy}
                               {t :current-time}]
  (attack-fn enemy t))

; TODO: Better manage magic numbers for margins
(defn move-enemies [{w       :w
                     h       :h
                     t       :current-time
                     enemies :enemies :as state}]
  "Returns the game state with all enemies moved to new positions,
   and filtering out those that have moved off-screen.

   Note that we allow for a fairly wide margin outside the field of view.
   This is to allow for enemies to emerge from offscreen in a line for example.
   If we enforced a zero width margin then such enemies would never appear."
  (let [new-enemies  (->> enemies
                       (filter (fn [{x :x y :y}] (and (<= y (+ h 200)) (>= x -600) (<= x (+ w 600)))))
                       (map (fn [e] (move-enemy e state))))]
    (assoc-in state [:enemies] new-enemies)))

(defn move-enemy-bullets [{:keys [w h enemy-bullets] :as state}]
  "Returns the game state with all enemy bullets moved to new positions."
  (let [dθ           10
        dr           5
        new-bullets  (->> enemy-bullets
                       (filter (fn [{x :x y :y}] (and (> y 0) (< y h) (> x 0) (< x w))))
                       (map (fn [{ϕ :ϕ :as bullet}] (-> bullet
                                                       (update-in [:x] + (* dr (q/cos ϕ)))
                                                       (update-in [:y] + (* dr (q/sin ϕ)))
                                                       (update-in [:θ] + dθ)))))]
    (assoc-in state [:enemy-bullets] new-bullets)))

(defn move-bg-objects [{:keys [h bg-objects] :as state}]
  (-> state
    (update-in [:bg-objects] (fn [bos] (remove (fn [{y :y}] (> y h)) bos)))
    (update-in [:bg-objects] (fn [bos] (map (fn [bo] (update-in bo [:y] + 5)) bos)))
      ))

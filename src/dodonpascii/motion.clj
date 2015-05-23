(ns dodonpascii.motion
  "This module defines how all entities are moved in the game."
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

(defmethod move-enemy :heli [{:keys [init-t init-x init-y init-θ dir] :as enemy}
                             {curr-t :current-time}]
  (let [f  ({:left - :right +} dir)
        dt (* 0.001 (- curr-t init-t))]
    (-> enemy
      (update-in [:x] (fn [x] (f init-x (* 100 dt))))
      (update-in [:y] (fn [y] (+ init-y 600 (* -600 (- dt 1) (- dt 1)))))
      (update-in [:θ] (fn [θ] (f init-θ (* -40 dt)))))))

(defmethod move-enemy :blue-plane [{:keys [init-t init-x init-y dir] :as enemy}
                                   {curr-t :current-time}]
  (let [f  ({:left - :right +} dir)
        dt (* 0.001 (- curr-t init-t))]
    (-> enemy
      (update-in [:x] (fn [x] (f init-x (* 100 dt))))
      (update-in [:y] (fn [y] (+ init-y (* 400 dt)))))))

(defmethod move-enemy :tank [{prev-t :t :as enemy}
                             {curr-t :current-time}]
  (let [dt (* 0.001 (- curr-t prev-t))]
    (-> enemy
      (assoc-in [:t] curr-t)
      (update-in [:y] - (* dt 40)))))

; TODO: This needs to be cleaned up somehow. :3
(defmethod move-enemy :biplane [{:keys [init-t init-x init-y init-θ dir] :as enemy}
                                {curr-t :current-time}]
  (let [fx  ({:left - :right +} dir)
        fθ  ({:left + :right -} dir)
        dt (* 0.001 (- curr-t init-t))
        t1 (if (= dir :left) (* 0.0025 (- init-x 400)) (* 0.0025 (- 400 init-x)))
        t2 (+ t1 2)]
    (-> enemy
      (update-in [:x] (fn [x]
                        (cond
                          (< dt t1)
                            (fx init-x (* 400 dt))
                          (< dt t2)
                            (fx 400 (* 128 (q/sin (q/radians (* 57 q/PI (- dt t1))))))
                          :else
                            (fx 400 (* 400 (- dt t2))))))
      (update-in [:y] (fn [y]
                        (cond
                          (< dt t1)
                            init-y
                          (< dt t2)
                            (+ (- init-y 128) (* 128 (q/cos (q/radians (* 57 q/PI (- dt t1))))))
                          :else
                            init-y)))
      (update-in [:θ] (fn [θ]
                        (cond
                          (< dt t1)
                            init-θ
                          (< dt t2)
                            (fθ init-θ (* 57 q/PI (- dt t1)))
                          :else
                            init-θ))))))

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
  (let [margin       600
        new-enemies  (->> enemies
                       (filter (fn [{x :x y :y}] (and (>= y (- margin))
                                                      (<= y (+ h margin))
                                                      (>= x (- margin))
                                                      (<= x (+ w margin)))))
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
    (update-in [:bg-objects] (fn [bos] (map (fn [bo] (update-in bo [:y] + 5)) bos)))))

(defmulti move-boss (fn [state] (get-in state [:boss :type])))

(defmethod move-boss :bfp-5000 [{{prev-t :t :as boss} :boss
                                  curr-t :current-time :as state}]
  (let [dt (* 0.001 (- curr-t prev-t))
        new-boss (-> boss
                   (assoc-in [:t] curr-t)
                   (update-in [:y] (fn [y]
                                     (cond
                                       (< y 300)
                                         (+ y (* dt 40))
                                       :else
                                         y))))]
    (assoc-in state [:boss] new-boss)))


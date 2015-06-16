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

(defn move-player-bullets [{:keys [w player-bullets] :as state}]
  "Returns the game state with all player bullets moved to new positions."
  (let [dθ      10
        dr      10
        new-bullets  (->> player-bullets
                       (filter (fn [{x :x y :y}] (and (> y 0) (> x 0) (< x w))))
                       (map (fn [{ϕ :ϕ :as bullet}] (-> bullet
                                                       (update-in [:x] + (* dr (q/sin (q/radians ϕ))))
                                                       (update-in [:y] - (* dr (q/cos (q/radians ϕ))))
                                                       (update-in [:θ] + dθ)))))]
    (assoc-in state [:player-bullets] new-bullets)))

(defn move-power-ups [{:keys [w h power-ups] :as state}]
  "Returns the game state with all extant power ups moved to new positions."
  (let [new-power-ups  (->> power-ups
                         (filter (fn [{x :x y :y}] (and (< y h) (> x 0) (< x w))))
                         (map (fn [power-up] (-> power-up
                                               (update-in [:y] + 3)))))]
    (assoc-in state [:power-ups] new-power-ups)))

(defn move-bonus-items [{bonus-items :bonus-items :as state}]
  (let [new-bonus-items (map (fn [{:keys [y dir] :as b}]
                                (-> b
                                  (update-in [:y] + 5)
                                  (update-in [:x] + (if (= dir :left) -0.5 0.5)))) bonus-items)]
    (assoc-in state [:bonus-items] new-bonus-items)))

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

; TODO: This needs to be cleaned up; magic numbers need to be
;         somehow computed and understandable.
(defmethod move-enemy :biplane [{:keys [init-t init-x init-y init-θ dir] :as enemy}
                                {curr-t :current-time}]
  (let [fx  ({:left - :right +} dir)
        fθ  ({:left + :right -} dir)
        r   128
        t   (* 0.001 (- curr-t init-t))
        t1  (if (= dir :left) (* 0.0025 (- init-x 400)) (* 0.0025 (- 400 init-x)))
        t2  (+ t1 2)
        [new-x new-y new-θ] (cond
                              (< t t1)
                                [(fx init-x (* 400 t))
                                 init-y
                                 init-θ]
                              (< t t2)
                                [(fx 400 (* r (q/sin (q/radians (* 180 (- t t1))))))
                                 (+ (- init-y r) (* r (q/cos (q/radians (* 180 (- t t1))))))
                                 (fθ init-θ (* 180 (- t t1)))]
                              :else
                                [(fx 400 (* 400 (- t t2)))
                                 init-y
                                 init-θ])]
    (-> enemy
      (assoc-in [:x] new-x)
      (assoc-in [:y] new-y)
      (assoc-in [:θ] new-θ))))

;(defmethod move-enemy :small-plane [{:keys [init-t init-x init-y init-θ dir] :as enemy}
;                                {curr-t :current-time}]
;  )

(defmethod move-enemy :large-plane [{:keys [x y θ dir init-t init-x init-y init-θ] :as enemy}
                                    {curr-t :current-time}]
  (let [fx  ({:left - :right +} dir)
        fθ  ({:left + :right -} dir)
        t  (* 0.001 (- curr-t init-t))
        r   150
        t1  2
        t2  (+ t1 6)
        [new-x new-y new-θ] (cond
                              (< t t1)
                                [init-x
                                 (+ init-y (* 200 t))
                                 init-θ]
                              (< t t2)
                                [(fx init-x r (* -1 r (q/cos (q/radians (* 90 (- t t1))))))
                                 (+ init-y 400 (* r (q/sin (q/radians (* 90 (- t t1))))))
                                 (fθ init-θ (* 90 (- t t1)))]
                              :else
                                [(fx init-x r r)
                                 (- (+ init-y 400) (* 200 (- t t2)))
                                 (+ init-θ 180)])]
    (-> enemy
      (assoc-in [:x] new-x)
      (assoc-in [:y] new-y)
      (assoc-in [:θ] new-θ))))


; TODO: Move out filtering of offscreen enemies
(defn move-enemies [{:keys [w h enemies] :as state}]
  "Returns the game state with all enemies moved to new positions,
   and filtering out those that have moved off-screen.

   Note that we allow for a fairly wide margin outside the field of view.
   This is to allow for enemies to emerge from offscreen in a line for example.
   If we enforced a zero width margin then such enemies would never appear
   in the first place because they would be immmediately scrubbed off!"
  (let [margin       600
        new-enemies  (->> enemies
                       (filter (fn [{x :x y :y}] (and (>= y (- margin))
                                                      (<= y (+ h margin))
                                                      (>= x (- margin))
                                                      (<= x (+ w margin)))))
                       (map (fn [e] (move-enemy e state))))]
    (assoc-in state [:enemies] new-enemies)))

(defmulti move-enemy-bullet (fn [bullet current-time] (:type bullet)))

(defmethod move-enemy-bullet :cone [{init-t :init-t ϕ :ϕ :as bullet}
                                    current-time]
  (if (> init-t current-time)
    bullet
    (let [dθ 10
          dr 5]
      (-> bullet
        (update-in [:x] + (* dr (q/cos ϕ)))
        (update-in [:y] + (* dr (q/sin ϕ)))
        (update-in [:θ] + dθ)))))

(defmethod move-enemy-bullet :default [{ϕ :ϕ :as bullet}
                                       current-time]
  (let [dθ 10
        dr 5]
    (-> bullet
      (update-in [:x] + (* dr (q/cos ϕ)))
      (update-in [:y] + (* dr (q/sin ϕ)))
      (update-in [:θ] + dθ))))

; TODO: Move out filtering of offscreen bullets
(defn move-enemy-bullets [{:keys [w h current-time enemy-bullets] :as state}]
  "Returns the game state with all enemy bullets moved to new positions."
  (let [new-bullets  (->> enemy-bullets
                       (filter (fn [{x :x y :y}] (and (> y 0) (< y h) (> x 0) (< x w))))
                       (map (fn [b] (move-enemy-bullet b current-time))))]
    (assoc-in state [:enemy-bullets] new-bullets)))

; TODO: Move out filtering of offscreen objects
(defn move-bg-objects [{:keys [h bg-objects] :as state}]
  "Returns the game state with all background objects moved."
  (-> state
    (update-in [:bg-objects] (fn [bos] (remove (fn [{y :y}] (> y h)) bos)))
    (update-in [:bg-objects] (fn [bos] (map (fn [bo] (update-in bo [:y] + 5)) bos)))))

(defmulti move-boss (fn [state] (get-in state [:boss :type])))

(defmethod move-boss :bfp-5000 [{{prev-t :t
                                   status :status :as boss} :boss
                                  curr-t :current-time :as state}]
  (let [dt (* 0.001 (- curr-t prev-t))
        new-boss (-> boss
                   (assoc-in [:t] curr-t)
                   (update-in [:y] (fn [y]
                                     (cond
                                       (or (< y 300) (= status :dead))
                                         (+ y (* dt 100))
                                       :else
                                         y))))]
    (assoc-in state [:boss] new-boss)))

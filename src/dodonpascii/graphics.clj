(ns dodonpascii.graphics
  "This module is responsible for all rendering."
  (:require [quil.core :as q :include-macros true]))

(defn draw-score [{{font  :score} :fonts
                   {score :score} :player}]
  "Renders the current score"
  (q/text-font font)
  (q/fill 140 255 255)
  (q/text (str score) 50 70))

(defn draw-lives [{{lives  :lives} :player
                   {sprite :player-life} :sprites
                    w :w}]
  "Renders sprites representing the number of lives the player has left."
  (q/push-matrix)
  (q/translate (- w 50) 50)
  (dotimes [_ lives]
    (q/image sprite 0 0)
    (q/translate -50 0))
  (q/pop-matrix))

(defn draw-background [{:keys [bg-objects sprites]}]
  "Renders the game background."
  (q/background 100 255 50)
  (doseq [{:keys [x y type]} bg-objects]
    (q/image (sprites type) x y)))

(defn draw-player [{{x :x y :y} :player
                    {sprites :player} :sprites}]
  "Renders the player."
  (let [idx (mod (quot (q/frame-count) 15) 2)]
    (q/image (sprites idx) x y)))

(defn draw-player-bullets [{bullets :player-bullets
                            sprites :sprites}]
  "Renders the player's bullets."
  (doseq [{bullet-type :type x :x y :y θ :θ} bullets]
    (q/push-matrix)
    (q/translate x y)
    (q/rotate (q/radians θ))
    (q/image (bullet-type sprites) 0 0)
    (q/pop-matrix)))

(defn draw-power-ups [{power-ups :power-ups
                       sprites   :sprites}]
  "Renders all powerups."
  (doseq [{power-up-type :type x :x y :y} power-ups]
    (q/image (power-up-type sprites) x y)))

(defmulti draw-enemy (fn [enemy state] (:type enemy)))

(defmethod draw-enemy :default [{:keys [type x y θ]}
                                {sprites :sprites}]
  (let [idx    (-> (q/frame-count) (quot 4) (mod 2))
        sprite (get-in sprites [type idx])]
    (q/push-matrix)
    (q/translate x y)
    (q/rotate (q/radians θ))
    (q/image sprite 0 0)
    (q/pop-matrix)))
(defmethod draw-enemy :tank [{:keys [type x y θ]}
                             {{sprites :tank} :sprites
                              {player-x :x player-y :y} :player}]
  (let [idx    (-> (q/frame-count) (quot 4) (mod (count sprites)))
        sprite (get-in sprites [idx])]
    (q/push-matrix)
    (q/translate x y)
    (q/rotate (q/radians θ))
    (q/image sprite 0 0)
    (q/pop-matrix)))

; TODO: Use get-in with list of nested indices to draw enemy
(defn draw-enemies [{enemies :enemies :as state}]
;                     sprites :sprites}]
  "Renders the enemies."
  (doseq [enemy enemies]
    (draw-enemy enemy state)))

(defn draw-enemy-bullets [{enemy-bullets :enemy-bullets
                           sprites       :sprites}]
  "Renders the enemy bullets."
  (doseq [{:keys [type x y θ ϕ]} enemy-bullets]
    (q/push-matrix)
    (q/translate x y)
    (q/rotate (q/radians θ))
    (q/image (type sprites) 0 0)
    (q/pop-matrix)))

(defmulti draw-boss (fn [state] (get-in state [:boss :type])))

(defmethod draw-boss :bfp-5000 [{{:keys [x y θ]} :boss
                                 {sprites :bfp-5000} :sprites}]
  (let [idx    (-> (q/frame-count) (quot 4) (mod (count sprites)))
        sprite (get-in sprites [idx])]
    (q/push-matrix)
    (q/translate x y)
    (q/image sprite 0 0)
    (q/pop-matrix)))



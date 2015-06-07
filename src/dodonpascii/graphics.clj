(ns dodonpascii.graphics
  "This module is responsible for all rendering."
  (:require [quil.core :as q :include-macros true]))

(defn draw-score [{{font  :score} :fonts
                   {score :score} :player}]
  "Renders the current score"
  (q/text-font font)
  (q/fill 140 255 255)
  (q/text (str score) 50 70))

(defn draw-player-status [{{:keys [lives bombs]} :player
                           {:keys [player-life extra-bomb]} :sprites
                           w :w}]
  "Renders sprites representing the number of lives and bombs the player has left."
  (q/push-matrix)
  (q/translate (- w 50) 50)
  (q/push-matrix)
  (dotimes [_ lives]
    (q/image player-life 0 0)
    (q/translate -50 0))
  (q/pop-matrix)
  (q/translate 0 50)
  (dotimes [_ bombs]
    (q/image extra-bomb 0 0)
    (q/translate -50 0))
  (q/pop-matrix))

(defn draw-player-stats [{player-stats :player-stats :as state}]
  (q/push-matrix)
  (q/translate 400 300)
  (q/text "LEVEL COMPLETED" 0 0)

  (doseq [[k v] player-stats]
    (q/translate 0 50)
    (q/push-matrix)
    (q/text (str k) 0 0)
    (q/translate 350 0)
    (q/text (str v) 0 0)
    (q/pop-matrix))

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

; TODO: Eventually, the an independently rotating turret
;         will differentiate this implementation from the default one.
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

(defn draw-enemies [{enemies :enemies :as state}]
  "Renders all enemies."
  (doseq [enemy enemies]
    (draw-enemy enemy state)))

(defn draw-bullets [{:keys [enemy-bullets sprites current-time]}]
  "Renders all bullets from enemies as well as bosses."
  (doseq [{:keys [type x init-t y θ ϕ]} enemy-bullets]
    (if (<= init-t current-time)
      (do
        (q/push-matrix)
        (q/translate x y)
        (q/rotate (q/radians θ))
        (q/image (type sprites) 0 0)
        (q/pop-matrix)))))

(defmulti draw-boss (fn [state] (get-in state [:boss :type])))

; TODO: Implement logic to draw boss shrinking when it falls to the ground.
(defmethod draw-boss :bfp-5000 [{{:keys [x y θ status hitboxes]} :boss
                                 {boss-sprites :bfp-5000
                                  fire-sprites :bfp-5000-fire
                                  hit-sprite :bfp-5000-hit} :sprites}]
  (let [idx    (-> (q/frame-count) (quot 4) (mod (count boss-sprites)))
        sprite (get-in boss-sprites [idx])]
    (q/push-matrix)
    (q/translate x y)
    (if (= :hit status)
      (q/image hit-sprite 0 0)
      (q/image sprite 0 0))
    (let [idx    (-> (q/frame-count) (quot 4) (mod (count boss-sprites)))
          sprite (get-in fire-sprites [idx])]
      (doseq [{:keys [x y hp]} hitboxes]
        (if (zero? hp)
          (do
            (q/push-matrix)
            ; This places the fire sprites slightly below the hitboxes.
            (q/translate x (+ y 100))
            (q/image sprite 0 0)
            (q/pop-matrix))))
    (q/pop-matrix))))

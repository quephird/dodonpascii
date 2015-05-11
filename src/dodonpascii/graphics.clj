(ns dodonpascii.graphics
  (:require [quil.core :as q :include-macros true]))

(defn draw-score [{{font  :score} :fonts
                   {score :score} :player}]
  (q/text-font font)
  (q/fill 140 255 255)
  (q/text (str score) 50 50))

(defn draw-background [state]
  "Renders the game background."
  (q/background 0))

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
  "Renders the player's bullets."
  (doseq [{power-up-type :type x :x y :y} power-ups]
    (q/image (power-up-type sprites) x y)))

(defn draw-enemies [{enemies :enemies
                     sprites :sprites}]
  "Renders the enemies."
  (let [idx (mod (quot (q/frame-count) 4) 2)]
    (doseq [{enemy-type :type x :x y :y θ :θ} enemies]
      (q/push-matrix)
      (q/translate x y)
      (q/rotate (q/radians θ))
      (q/image ((sprites enemy-type) idx) 0 0)
      (q/pop-matrix))))
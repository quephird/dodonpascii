(ns dodonpascii.resources
  "This module is responsible for loading of all static resources."
  (:require [quil.core :as q :include-macros true]))

(defn load-fonts []
  {:score             (q/create-font "Andale" 32)})

(defn load-sprites []
  {:player           [(q/load-image "resources/player1.png")
                      (q/load-image "resources/player2.png")]
   :heli             [(q/load-image "resources/heli1.png")
                      (q/load-image "resources/heli2.png")]
   :biplane          [(q/load-image "resources/biplane1.png")
                      (q/load-image "resources/biplane2.png")]
   :blue-plane       [(q/load-image "resources/blue-plane1.png")
                      (q/load-image "resources/blue-plane2.png")]
   :tank             [(q/load-image "resources/tank.png")]
   :enemy-shot        (q/load-image "resources/enemy-shot.png")
   :cone              (q/load-image "resources/cone.png")
   :player-life       (q/load-image "resources/player-life.png")
   :player-shot       (q/load-image "resources/player-shot.png")
   :extra-shots       (q/load-image "resources/extra-shots.png")
   :extra-bomb        (q/load-image "resources/extra-bomb.png")
   :bonus-star        (q/load-image "resources/bonus-star.png")
   :bfp-5000         [(q/load-image "resources/bfp-50001.png")
                      (q/load-image "resources/bfp-50002.png")]
   :bfp-5000-fire    [(q/load-image "resources/bfp-5000-fire1.png")
                      (q/load-image "resources/bfp-5000-fire2.png")]
   :bfp-5000-hit      (q/load-image "resources/bfp-5000-hit.png")
   :tree              (q/load-image "resources/tree.png")
   :peony             (q/load-image "resources/peony.png")
   :dandelion         (q/load-image "resources/dandelion.png")
   :splash            (q/load-image "resources/splash.png")
   :paused            (q/load-image "resources/paused.png")
   :game-over         (q/load-image "resources/game-over.png")})

; TODO: Need distinct sound for vulnerability shot.
(defn load-sounds [m]
  {:bullet-graze       (.loadFile m "resources/bullet-graze.wav")
   :bonus-star-pickup  (.loadFile m "resources/bonus-star-pickup.wav")
   :new-enemy-shot     (.loadFile m "resources/new-enemy-shot.wav")
   :cone-shot          (.loadFile m "resources/cone-shot.wav")
   :new-player-shot    (.loadFile m "resources/new-player-shot.wav")
   :enemy-dead         (.loadFile m "resources/enemy-dead.wav")
   :hitbox-shot        (.loadFile m "resources/enemy-dead.wav")
   :extra-shots-pickup (.loadFile m "resources/extra-shots-pickup.wav")})

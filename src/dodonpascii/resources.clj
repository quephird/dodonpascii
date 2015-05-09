(ns dodonpascii.resources
  (:require [quil.core :as q :include-macros true]))

(defn load-sprites []
  {:player           [(q/load-image "resources/player1.png")
                      (q/load-image "resources/player2.png")]
   :heli             [(q/load-image "resources/heli1.png")
                      (q/load-image "resources/heli2.png")]
   :biplane          [(q/load-image "resources/biplane1.png")
                      (q/load-image "resources/biplane2.png")]
   :player-shot       (q/load-image "resources/player-shot.png")
   :extra-shots       (q/load-image "resources/extra-shots.png")})

(defn load-sounds [m]
  {:new-player-shot    (.loadFile m "resources/new-player-shot.wav")
   :enemy-dead         (.loadFile m "resources/enemy-dead.wav")
   :extra-shots-pickup (.loadFile m "resources/extra-shots-pickup.wav")})

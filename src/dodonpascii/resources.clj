(ns dodonpascii.resources
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
   :tank             [(q/load-image "resources/tank6.png")]
   :enemy-shot        (q/load-image "resources/enemy-shot.png")
   :player-life       (q/load-image "resources/player-life.png")
   :player-shot       (q/load-image "resources/player-shot.png")
   :extra-shots       (q/load-image "resources/extra-shots.png")
   :tree              (q/load-image "resources/tree.png")
   :peony             (q/load-image "resources/peony.png")
   :dandelion         (q/load-image "resources/dandelion.png")})

(defn load-sounds [m]
  {:new-enemy-shot     (.loadFile m "resources/new-enemy-shot.wav")
   :new-player-shot    (.loadFile m "resources/new-player-shot.wav")
   :enemy-dead         (.loadFile m "resources/enemy-dead.wav")
   :extra-shots-pickup (.loadFile m "resources/extra-shots-pickup.wav")})

(ns dodonpascii.core
  "This is the main module of the game, where the game loop resides."
  (:import  [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w]
            ; [dodonpascii.bullets :as b]
            ; [dodonpascii.collision :as o]
            [dodonpascii.controls :as c]
            [dodonpascii.entities :as e]
            ; [dodonpascii.events :as v]
            ; [dodonpascii.generation :as n]
            [dodonpascii.graphics :as g]
            [dodonpascii.helpers :as h]
            ; [dodonpascii.motion :as m]
            [dodonpascii.update :as u] :reload-all))

(defn setup []
  "Sets up the initial game state, is called once at the beginning of the game."
  (let [w (q/width)
        h (q/height)
        m (Minim.)]
    (q/smooth)
    (q/image-mode :center)
    (q/color-mode :hsb)
    (e/make-game w h m)))

(defn- draw-frame-helper [state]
  (g/draw-background state)
  (g/draw-score state)
  (g/draw-player-status state)
  (g/draw-player state)
  (g/draw-player-bullets state)
  (g/draw-power-ups state)
  (g/draw-enemies state)
  (g/draw-bonus-items state)
  (g/draw-bullets state))

(defmulti draw-frame (fn [state]
                      [(:game-status state) (:level-status state)]))

(defmethod draw-frame [:playing :waves] [state]
  (v/handle-events state)
  (draw-frame-helper state))

(defmethod draw-frame [:playing :boss] [state]
  (v/handle-events state)
  (draw-frame-helper state)
  (g/draw-boss state))

(defmethod draw-frame [:playing :end] [state]
  (v/handle-events state)
  (draw-frame-helper state)
  (g/draw-player-stats state))

(defmethod draw-frame [:paused nil] [{w :w h :h
                                      {paused :paused} :sprites :as state}]
  (draw-frame-helper state)
  (q/image paused (* 0.5 w) (* 0.5 h)))

(defmethod draw-frame [:waiting nil] [{w :w h :h
                                       {splash :splash} :sprites}]
  (q/background 0)
  (q/image splash (* 0.5 w) (* 0.5 h)))

(defmethod draw-frame [:game-over nil] [{w :w h :h
                                         {game-over :game-over} :sprites}]
  (q/background 0)
  (q/image game-over (* 0.5 w) (* 0.5 h)))

(defmethod draw-frame :default [{current-time :current-time}])

(q/defsketch dodonpascii
  :size         [1200 800]
  :title        "🚀　🔸🔸🔸🔸🔸🔸🔸🔸  dodonpascii  🔹🔹🔹 💥 👾 💥 👾 👾"
  :setup        setup
  :update       u/update-game
  :key-pressed  c/key-pressed
  :key-released c/key-released
  :draw         draw-frame
  :middleware   [w/fun-mode])

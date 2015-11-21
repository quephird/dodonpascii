(ns dodonpascii.update
  "This module is responsible for updating game state before each frame."
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w]
            [dodonpascii.bullets :as b]
            [dodonpascii.checks :as c]
            [dodonpascii.cleanup :as l]
            [dodonpascii.collision :as o]
            [dodonpascii.events :as e]
            [dodonpascii.helpers :as h]
            [dodonpascii.generation :as n]
            [dodonpascii.motion :as m]
             :reload-all))

(defn reset-enemy-statuses [{enemies :enemies :as state}]
  "This method is necessary for enemies which require multiple shots to kill;
   this allows for a flash effect when hit."
  (let [new-enemies (map (fn [e] (assoc-in e [:status] :alive)) enemies)]
    (-> state
      (assoc-in [:enemies] new-enemies))))

(defmulti update-game (fn [state]
                       [(:game-status state) (:level-status state)]))

(defmethod update-game [:paused nil] [state]
  state)

(defmethod update-game [:waiting nil] [state]
  state)

(defmethod update-game [:playing :waves] [state]
  (-> state
    (h/set-current-time)
    (c/check-game-over)
    (e/clear-previous-events)
    (l/clear-offscreen-objects)
    (reset-enemy-statuses)
    (c/check-powerup-opportunities)
    (o/detect-all-collisions)
    (n/generate-all-objects)
    (m/move-all-objects)))

(defmethod update-game [:playing :boss] [state]
  (-> state
    (h/set-current-time)
    (e/clear-previous-events)
    (l/clear-offscreen-objects)
    (c/check-powerup-opportunities)
    (o/check-boss-shot)
    (c/check-boss-dead)
    (c/check-boss-offscreen)
    (o/check-power-ups)
    (o/check-grazed-bullets)
    (o/check-player-killed)
    (b/generate-boss-bullets)
    (n/generate-bg-objects)
    (m/move-all-objects)
    (m/move-boss)))

(defmethod update-game [:playing :end] [state]
  (-> state
    (h/set-current-time)
    (e/clear-previous-events)
    (n/generate-bg-objects)
    (m/move-player)
    (m/move-player-bullets)
    (m/move-bg-objects)
    (c/check-if-ready-for-next-level)))

(defmethod update-game :default [state]
  state)

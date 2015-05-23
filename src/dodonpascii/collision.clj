(ns dodonpascii.collision
  "This module is responsible for all collision detection."
  (:require [quil.core :as q :include-macros true])
  (:use     [dodonpascii.entities :as e]))

(defn collided-with? [{entity1-x :x entity1-y :y}
                      {entity2-x :x entity2-y :y}]
  "Returns true if the x and y coordinates of each entity are sufficiently close."
  (let [close-enough 24]
    (> close-enough (q/dist entity1-x entity1-y entity2-x entity2-y))))

(defn shot-by-any? [enemy bullets]
  "Returns true if any of the bullets has hit the enemy"
  (not (not-any? (fn [bullet] (collided-with? enemy bullet)) bullets)))

; TODO: Generalize hitbox function for multiple enemies
(defn check-enemies-shot [{:keys [enemies
                                  powerup-opportunities
                                  player-bullets] :as state}]
  "Removes all enemies that are shot; updates score accordingly and
   registers sound events."
  (let [new-enemies    (remove (fn [enemy] (shot-by-any? enemy player-bullets)) enemies)
        shot-enemies   (filter (fn [enemy] (shot-by-any? enemy player-bullets)) enemies)
        shot-enemy-ids (->> shot-enemies (map :id) set)
        new-points     (->> shot-enemies (map :type) (map e/get-score) (reduce + 0))
        new-event      (if (< (count new-enemies) (count enemies)) :enemy-dead)]
    (-> state
      (update-in [:player :score] + new-points)
      (update-in [:events] conj new-event)
      (assoc-in [:enemies] new-enemies))))

(defn check-grazed-bullets [{{:keys [x y]} :player
                              bullets :enemy-bullets :as state}]
  (let [grazes     (filter (fn [{bullet-x :x bullet-y :y}] (> 50 (q/dist x y bullet-x bullet-y))) bullets)
        new-points (-> grazes count (* 10))
        new-events (repeat (count grazes) :bullet-graze)]
    (-> state
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-events))))

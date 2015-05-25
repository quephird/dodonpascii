(ns dodonpascii.collision
  "This module is responsible for all collision detection."
  (:require [quil.core :as q :include-macros true])
  (:use     [dodonpascii.entities :as e]))

(defn collided-with? [{entity1-x :x entity1-y :y}
                      {entity2-x :x entity2-y :y}]
  "Returns true if the x and y coordinates of each entity are sufficiently close."
  (let [close-enough 24]
    (> close-enough (q/dist entity1-x entity1-y entity2-x entity2-y))))

(defn shot-by-any? [target bullets]
  "Returns true if any of the bullets has hit the target"
  (not (not-any? (fn [bullet] (collided-with? target bullet)) bullets)))

(defn clean-bullets [targets bullets]
  "Returns only the bullets that hit no targets"
  (remove (fn [b] (some (fn [t] (collided-with? b t)) targets)) bullets))

; TODO: Generalize hitbox function for multiple enemies
(defn check-enemies-shot [{:keys [enemies
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
  "This function determines how my enemy bullets were grazed by the player,
   returning the game state with the score updated accordingly."
  (let [grazes     (filter (fn [{bullet-x :x bullet-y :y}] (> 50 (q/dist x y bullet-x bullet-y))) bullets)
        new-points (-> grazes count (* 10))
        new-events (repeat (count grazes) :bullet-graze)]
    (-> state
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-events))))

; TODO: Need to better manage scoring.
;       Need to remove player bullet if it scores a hit.
(defn check-boss-shot [{{hitboxes :hitboxes
                         boss-x :x boss-y :y :as boss} :boss
                       player-bullets :player-bullets :as state}]
  (let [hitboxes-with-actual-coords  (map (fn [{x :x y :y}] {:x (+ x boss-x) :y (+ y boss-y)}) hitboxes)
        shot-hitboxes         (filter (fn [v] (shot-by-any? v player-bullets)) hitboxes-with-actual-coords)
        new-bullets           (clean-bullets hitboxes-with-actual-coords player-bullets)
        new-points            (* 100 (count shot-hitboxes))
        new-event             (if (> (count shot-hitboxes) 0) :hitbox-shot)]
    (-> state
      (assoc-in [:player-bullets] new-bullets)
      (update-in [:player :score] + new-points)
      (update-in [:events] conj new-event))))

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
                                  player-bullets
                                  current-time] :as state}]
  "Removes all enemies that are shot; updates score accordingly and
   registers sound events."
  (let [new-enemies    (remove (fn [enemy] (shot-by-any? enemy player-bullets)) enemies)
        shot-enemies   (filter (fn [enemy] (shot-by-any? enemy player-bullets)) enemies)
        shot-enemy-ids (->> shot-enemies (map :id) set)
        new-points     (->> shot-enemies (map :type) (map e/get-score) (reduce + 0))
        new-event      (if (< (count new-enemies) (count enemies)) [{:type :enemy-dead :init-t current-time}])
        new-bonus-items (->> shot-enemies
                           (map (fn [{:keys [x y]}] [{:type :bonus-star :x x :y y :dir :left}
                                                     {:type :bonus-star :x x :y y :dir :right}]))
                           (apply concat))]
    (-> state
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-event)
      (update-in [:bonus-items] concat new-bonus-items)
      (update-in [:player-stats :enemies-shot] + (count shot-enemies))
      (assoc-in [:enemies] new-enemies))))

(defn check-grazed-bullets [{{:keys [x y]} :player
                              bullets :enemy-bullets
                              current-time :current-time :as state}]
  "This function determines how my enemy bullets were grazed by the player,
   returning the game state with the score updated accordingly."
  (let [grazes     (filter (fn [{bullet-x :x bullet-y :y}] (> 50 (q/dist x y bullet-x bullet-y))) bullets)
        new-points (-> grazes count (* 10))
        new-events (repeat (count grazes) {:type :bullet-graze :init-t current-time})]
    (-> state
      (update-in [:player-stats :bullets-grazed] + (count grazes))
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-events))))

(defn check-bonus-pickups [{{:keys [x y]} :player
                              bonus-items :bonus-items
                              current-time :current-time :as state}]
  "This function determines how my enemy bullets were grazed by the player,
   returning the game state with the score updated accordingly."
  (let [pickups           (filter (fn [{bonus-x :x bonus-y :y}] (> 24 (q/dist x y bonus-x bonus-y))) bonus-items)
        new-bonus-items   (remove (fn [{bonus-x :x bonus-y :y}] (> 24 (q/dist x y bonus-x bonus-y))) bonus-items)
        new-points        (-> pickups count (* 250))
        new-events        (repeat (count pickups) {:type :bonus-star-pickup :init-t current-time})]
    (-> state
      (update-in [:player-stats :bonus-stars] + (count pickups))
      (assoc-in [:bonus-items] new-bonus-items)
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-events))))

; TODO: Need to better manage scoring.
(defn check-boss-shot [{{hitboxes :hitboxes
                          boss-x :x boss-y :y :as boss} :boss
                         player-bullets :player-bullets
                         current-time   :current-time   :as state}]
  "Returns the game state updating various attributes such as the score, state of the boss, etc."
  (let [hitboxes-with-actual-coords  (map (fn [{x :x y :y}] {:x (+ x boss-x) :y (+ y boss-y)}) hitboxes)
        shot-hitboxes         (filter (fn [hb] (shot-by-any? hb player-bullets)) hitboxes-with-actual-coords)
        new-hitboxes          (map (fn [hb]
                                     (if (shot-by-any? (merge-with + hb {:x boss-x :y boss-y}) player-bullets)
                                       (update-in hb [:hp] (fn [hp] (q/constrain (dec hp) 0 hp)))
                                       hb)) hitboxes)
        new-bullets           (clean-bullets hitboxes-with-actual-coords player-bullets)
        new-points            (* 100 (count shot-hitboxes))
        new-event             (if (> (count shot-hitboxes) 0) [{:type :hitbox-shot :init-t current-time}])
        new-boss-status       (if (> (count shot-hitboxes) 0) :hit :alive)]
    (-> state
      (assoc-in [:player-bullets] new-bullets)
      (assoc-in [:boss :status] new-boss-status)
      (assoc-in [:boss :hitboxes] new-hitboxes)
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-event))))

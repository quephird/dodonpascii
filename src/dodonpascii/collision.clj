(ns dodonpascii.collision
  "This module is responsible for all collision detection."
  (:require [quil.core :as q :include-macros true])
  (:use     [dodonpascii.entities :as e]))

(defn collided-with? [{entity1-x :x entity1-y :y}
                      {entity2-x :x entity2-y :y}
                      range]
  "Returns true if the positions of each entity are
   within range r of each other"
  (> range (q/dist entity1-x entity1-y entity2-x entity2-y)))

(defn collided-with-any? [target possibilities r]
  "Returns true if any of the objects passed in the
   possibilities parameter has gotten within range r
   of the target"
  (not (not-any? (fn [p] (collided-with? target p r)) possibilities)))

(defn clean-bullets [targets bullets]
  "Returns only the bullets that hit no targets"
  (let [bullet-range 24]
    (remove (fn [b]
              (some (fn [t] (collided-with? b t bullet-range)) targets)) bullets)))

(defn check-player-killed [{{:keys [lives score] :as player} :player
                            :keys [w h enemies enemy-bullets events current-time] :as state}]
  "Returns the game state with the player unchanged or a new one if shot"
  (let [bullet-r     24
        enemy-r      75
        player-shot? (collided-with-any? player enemy-bullets bullet-r)
        collided?    (collided-with-any? player enemies enemy-r)
        [new-player new-events] (if (or player-shot? collided?)
                                  [(-> (make-player (* 0.5 w) (* 0.9 h))
                                     (assoc-in [:score] score)
                                     (assoc-in [:lives] (dec lives)))
                                   (conj events {:type :player-killed :init-t current-time})]
                                  [player events])]
    (-> state
      (assoc-in [:player] new-player)
      (assoc-in [:events] new-events))))

; TODO: Generalize hitbox function for multiple enemies
(defn check-enemies-shot [{:keys [enemies
                                  player-bullets
                                  current-time] :as state}]
  "Removes all enemies that are shot; updates score accordingly and
   registers sound events."

  ; The strategy here is to first take away all hit points from enemies
  ; that were shot and then generate sound events for each hit.
  ; Then we scrub away the ones that are now dead, award points,
  ; generate bonus stars, and update player statistics.
  (let [r 24
        [temp-enemies new-events]
                        (reduce (fn [[acc-enemies acc-events] e]
                                  (if (collided-with-any? e player-bullets r)
                                    [(conj acc-enemies (-> e
                                                         (update-in [:hp] dec)
                                                         (assoc-in [:status] :hit)))
                                     (conj acc-events {:type :enemy-shot :init-t current-time})]
                                    [(conj acc-enemies e)
                                     acc-events])) [() ()] enemies)
        [new-enemies new-bonus-items new-points new-enemies-shot]
                        (reduce (fn [[acc-enemies acc-bonus-items acc-points acc-enemies-shot]
                                    {:keys [x y hp] :as e}]
                                  (if (zero? hp)
                                    [acc-enemies
                                     (concat acc-bonus-items
                                       (repeatedly 2 (fn [] (e/make-bonus-star {:x x :y y}))))
                                     (+ acc-points (e/get-score (:type e)))
                                     (inc acc-enemies-shot)]
                                    [(conj acc-enemies e)
                                     acc-bonus-items
                                     acc-points
                                     acc-enemies-shot])) [() () 0 0] temp-enemies)
        new-bullets    (clean-bullets enemies player-bullets)]
    (-> state
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-events)
      (update-in [:bonus-items] concat new-bonus-items)
      (update-in [:player-stats :enemies-shot] + new-enemies-shot)
      (assoc-in [:player-bullets] new-bullets)
      (assoc-in [:enemies] new-enemies))))

(defn check-power-ups [{:keys [power-ups
                               player
                               current-time] :as state}]
  "Removes all power-ups that the player collides with;
   updates lives, shots, and bombs accordingly and registers sound events."
  (let [r             24
        new-power-ups (remove (fn [power-up] (collided-with? player power-up r)) power-ups)]
    (if (= (count new-power-ups) (count power-ups))
      state
      (-> state
        (update-in [:events] conj {:type :extra-shots-pickup :init-t current-time})
        (update-in [:player :bullet-count] inc)
        (assoc-in [:power-ups] new-power-ups)))))

(defn check-grazed-bullets [{{:keys [x y]} :player
                              bullets :enemy-bullets
                              current-time :current-time :as state}]
  "This function determines how my enemy bullets were grazed by the player,
   returning the game state with the score updated accordingly."
  (let [grazes     (filter (fn [{grazed? :grazed? bullet-x :x bullet-y :y}]
                             (and (false? grazed?)
                                  (> 50 (q/dist x y bullet-x bullet-y)))) bullets)
        new-bullets (map (fn [{grazed? :grazed? bullet-x :x bullet-y :y :as bullet}]
                            (assoc-in bullet [:grazed?] (if (and (false? grazed?)
                                                                  (> 50 (q/dist x y bullet-x bullet-y)))
                                                           :true
                                                           grazed?))) bullets)
        new-points (-> grazes count (* 10))
        new-events (repeat (count grazes) {:type :bullet-graze :init-t current-time})]
    (-> state
      (assoc-in [:enemy-bullets] new-bullets)
      (update-in [:player-stats :bullets-grazed] + (count grazes))
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-events))))

; TODO: Need to improve score handling here.
(defn check-bonus-pickups [{{:keys [x y]} :player
                              bonus-items :bonus-items
                              current-time :current-time :as state}]
  "This function determines which bonus items were picked up,
   awards points for each, and removes them from play."
  (let [{pickups true
         new-bonus-items false} (group-by (fn [{bonus-x :x bonus-y :y}] (> 50 (q/dist x y bonus-x bonus-y))) bonus-items)
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
  (let [r               24
        hitboxes-with-actual-coords  (map (fn [{x :x y :y}] {:x (+ x boss-x) :y (+ y boss-y)}) hitboxes)
        shot-hitboxes   (filter (fn [hb] (collided-with-any? hb player-bullets r)) hitboxes-with-actual-coords)
        new-hitboxes    (map (fn [hb]
                               (if (collided-with-any? (merge-with + hb {:x boss-x :y boss-y}) player-bullets r)
                                 (update-in hb [:hp] (fn [hp] (q/constrain (dec hp) 0 hp)))
                                 hb)) hitboxes)
        new-bullets     (clean-bullets hitboxes-with-actual-coords player-bullets)
        new-points      (* 100 (count shot-hitboxes))
        new-event       (if (> (count shot-hitboxes) 0) [{:type :hitbox-shot :init-t current-time}])
        new-boss-status (if (> (count shot-hitboxes) 0) :hit :alive)]
    (-> state
      (assoc-in [:player-bullets] new-bullets)
      (assoc-in [:boss :status] new-boss-status)
      (assoc-in [:boss :hitboxes] new-hitboxes)
      (update-in [:player :score] + new-points)
      (update-in [:events] concat new-event))))

(defn detect-all-collisions [state]
  (-> state
    check-player-killed
    check-enemies-shot
    check-power-ups
    check-grazed-bullets
    check-bonus-pickups))

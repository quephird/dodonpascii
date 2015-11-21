(ns dodonpascii.checks
  "This module contains functions which inspect various conditions in the game state."
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w]
            [dodonpascii.cleanup :as c]
            [dodonpascii.collision :as o]
            [dodonpascii.helpers :as h]
            [dodonpascii.levels :as v]
            :reload-all))

(defn check-if-ready-for-next-level [{:keys [current-time
                                             current-level
                                             level-status
                                             current-spawn-time
                                             start-level-time
                                             end-level-time] :as state}]
  (let [ready? (>= (- current-time end-level-time) 5000)
        [new-level
         new-level-status
         new-spawn-time
         new-start-level-time
         new-end-level-time] (if ready?
                                [1
                                 :waves
                                 (v/get-next-enemy-spawn-time v/all-levels 1 0)
                                 (h/get-current-time)
                                 nil]
                                [current-level
                                 level-status
                                 current-spawn-time
                                 start-level-time
                                 end-level-time])]
    (-> state
      (assoc-in [:current-level] new-level)
      (assoc-in [:level-status] new-level-status)
      (assoc-in [:current-spawn-time] new-spawn-time)
      (assoc-in [:start-level-time] new-start-level-time)
      (assoc-in [:end-level-time] new-end-level-time))))

(defn check-boss-offscreen [{{:keys [x y status] :as boss} :boss
                             :keys [end-level-time level-status w h] :as state}]
  (let [boss-offscreen?   (c/offscreen? x y w h)
        [new-level-status
         new-boss
         new-end-level-time] (if (and boss-offscreen? (= :dead status))
                               [:end nil (h/get-current-time)]
                               [level-status boss end-level-time])]
        ; new-boss         (if (and boss-offscreen? (= :dead status)) nil boss)]
    (-> state
      ; (assoc-in [:boss] new-boss)
      (assoc-in [:level-status] new-level-status)
      (assoc-in [:end-level-time] new-end-level-time))))

(defn check-game-over [{{lives :lives} :player
                         :keys [game-status level-status] :as state}]
  (let [[new-game-status new-level-status] (if (zero? lives)
                                              [:game-over nil]
                                              [game-status level-status])]
    (-> state
      (assoc-in [:game-status] new-game-status)
      (assoc-in [:level-status] new-level-status))))

; TODO: Figure out if there is a way to avoid having to compute
;         shot enemies both here and in the check-enemies-shot routine.
(defn check-powerup-opportunities [{:keys [enemies player-bullets
                                           powerup-opportunities
                                           power-ups] :as state}]
  "Determines which enemies have been shot, and generates new powerups
   if an entire group of eligible enemies has been shot down."
  (let [r              24
        shot-enemies   (filter (fn [enemy] (o/collided-with-any? enemy player-bullets r)) enemies)
        shot-enemy-ids (map :id shot-enemies)
        new-power-ups (remove empty? (flatten
;                        Iterate through the powerup-opportunities
                                      (for [po-enemy-ids  powerup-opportunities]
;                          Iterate through each of the enemies
                                        (for [{:keys [id x y] :as enemy} shot-enemies]
;                            If the enemy is a member of the group
                                          (if (some #{id} po-enemy-ids)
;                              Remove the enemy from the group
                                            (let [new-po-enemy-ids (remove #{id} po-enemy-ids)]
;                                If the group is now empty
                                              (if (empty? new-po-enemy-ids)
;                                  Make a new powerup with the x and y coords of the enemy
                                                {:type :extra-shots :x x :y y})))))))
        new-powerup-opportunities (map #(remove (set shot-enemy-ids) %) powerup-opportunities)]
    (-> state
      (assoc-in [:powerup-opportunities] new-powerup-opportunities)
      (update-in [:power-ups] concat new-power-ups))))

(defn check-boss-dead [{{:keys [hitboxes status]} :boss
                        current-time :current-time :as state}]
  (let [dead? (->> hitboxes
                (map :hp)
                (reduce + 0)
                zero?)
        new-boss-status  (if dead? :dead status)
        new-event        (if dead? [{:type :boss-dead :init-t current-time}])]
    (-> state
      (assoc-in [:boss :status] new-boss-status)
      (update-in [:events] concat new-event))))

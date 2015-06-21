(ns dodonpascii.generation
  (:require [quil.core :as q :include-macros true])
  (:use     [dodonpascii.bullets :as b]
            [dodonpascii.entities :as e]
            [dodonpascii.levels :as l]))

; TODO: This does not work if the game is paused.
;       This needs to be refactored; it's too big.
(defn generate-enemies [{:keys [w
                                h
                                powerup-opportunities
                                start-level-time
                                current-level
                                current-spawn-time
                                levels] :as state}]
  "Returns the game state either untouched or with new enemies
   depending if the current time coincides with a spawn time."
  (let [current-time       (System/currentTimeMillis)
        seconds-into-level (* 0.001 (- current-time start-level-time))]
    (cond
      (nil? current-spawn-time)
        ; No more enemy waves; this is the boss wave
        (let [boss-parms      (get-in levels [current-level :boss])
              new-boss        (make-boss boss-parms)
              patterns        (get-in levels [current-level :boss :bullet-patterns])
              new-spawn-time  (b/get-next-spawn-time patterns 0)]
          (-> state
            (assoc-in [:next-boss-bullet-spawn-time] new-spawn-time)
            (assoc-in [:level-status] :boss)
            (assoc-in [:boss] new-boss)))
      (< seconds-into-level current-spawn-time)
        ; We haven't arrived at the next enemy wave yet.
        state
      :else
        ; We arrived at or just passed the next enemy spawn time.
        (let [new-wave            (get-in levels [current-level :waves current-spawn-time])
              {:keys [type powerup-opportunity boss dir init-params]} new-wave
              new-enemies         (map (fn [ip] (e/make-enemy type ip)) init-params)
              new-spawn-time      (l/get-next-enemy-spawn-time levels current-level seconds-into-level)
              new-powerup-opportunities (if powerup-opportunity
                                           (conj powerup-opportunities (map :id new-enemies))
                                           powerup-opportunities)]
          (-> state
            (update-in [:enemies] concat new-enemies)
            (update-in [:player-stats :enemies] + (count new-enemies))
            (assoc-in [:powerup-opportunities] new-powerup-opportunities)
            (assoc-in [:current-spawn-time] new-spawn-time))))))

(defn generate-enemy-bullets [{:keys [enemies player current-time] :as state}]
  "Returns the game state with a random number of new enemy bullets,
   with new sound events for each."
  (let [{player-x :x player-y :y} player
        new-bullets  (remove empty?
                       (for [{:keys [x y]} enemies]
                         ; The check for positive y is to make sure bullets
                         ; are not produced when enemies are off-screen.
                         (if (and (< (q/random 1) 0.01) (> y 0))
                           (let [dx (- player-x x)
                                 dy (- player-y y)
                                 dh (q/sqrt (+ (* dx dx) (* dy dy)))
                                 ϕ  (- (q/atan (/ dy dx)) (if (> 0 dx) q/PI 0))]
                             {:type :enemy-shot
                              :init-t current-time
                              :grazed? false
                              :x x
                              :y y
                              :θ 0
                              :ϕ ϕ}))))
        new-events   (repeat (count new-bullets) {:type :new-enemy-shot :init-t current-time})]
    (-> state
      (update-in [:events] concat new-events)
      (update-in [:enemy-bullets] concat new-bullets))))

(defn generate-bg-objects [{:keys [w h current-level levels] :as state}]
  "Returns the game state with a random number of new background objects."
  (let [new-object (if (< (q/random 1) 0.05) [{:type ((get-in levels [current-level :bg-objects]) (rand-int 3))
                                               :x (q/random w)
                                               :y 0}])]
    (-> state
      (update-in [:bg-objects] (fn [bos] (remove (fn [{y :y}] (> y h)) bos)))
      (update-in [:bg-objects] concat new-object))))

(defn generate-all-objects [state]
  (-> state
    (generate-enemies)
    (generate-enemy-bullets)
    (generate-bg-objects)))

(ns dodonpascii.core
  "This is the main module of the game, where the game loop resides."
  (:import  [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w])
  (:use     [dodonpascii.bullets :as b]
            [dodonpascii.collision :as o]
            [dodonpascii.controls :as c]
            [dodonpascii.entities :as e]
            [dodonpascii.events :as v]
            [dodonpascii.graphics :as g]
            [dodonpascii.levels :as l]
            [dodonpascii.motion :as m] :reload-all))

(defn setup []
  "Sets up the initial game state, is called once at the beginning of the game."
  (let [w (q/width)
        h (q/height)
        m (Minim.)]
    (q/smooth)
    (q/image-mode :center)
    (q/color-mode :hsb)
    (e/make-game w h m)))

(defn set-current-time [state]
  (assoc-in state [:current-time] (System/currentTimeMillis)))

(defn offscreen? [x y w h]
  (let [margin 600]
    (or (<= y (- margin))
        (>= y (+ h margin))
        (<= x (- margin))
        (>= x (+ w margin)))))

(defn clear-objects [object-key {:keys [w h] :as state}]
  (let [curr-objects   (get-in state [object-key])
        new-objects    (remove (fn [{:keys [x y]}] (offscreen? x y w h)) curr-objects)]
    (-> state
      (assoc-in [object-key] new-objects))))

(defn clear-offscreen-objects [state]
  "Returns the game state with all offscreen objects removed.

   Note that we allow for a fairly wide margin outside the field of view.
   This is to allow for enemies to emerge from offscreen in a line for example.
   If we enforced a zero width margin then such enemies would never appear
   in the first place because they would be immmediately scrubbed off!"
  (->> state
    (clear-objects :enemies)
    (clear-objects :bonus-items)
    (clear-objects :enemy-bullets)
    (clear-objects :bg-objects)))

(defn check-boss-offscreen [{{:keys [x y status] :as boss} :boss
                               level-status :level-status w :w h :h :as state}]
  (let [boss-offscreen?   (offscreen? x y w h)
        new-level-status (if (and boss-offscreen? (= :dead status)) :end level-status)
        new-boss         (if (and boss-offscreen? (= :dead status)) nil boss)]
    (-> state
;      (assoc-in [:boss] new-boss)
      (assoc-in [:level-status] new-level-status))))

; TODO: Figure out if there is a way to avoid having to compute
;         shot enemies both here and in the check-enemies-shot routine.
(defn check-powerup-opportunities [{:keys [enemies player-bullets
                                           powerup-opportunities
                                           power-ups] :as state}]
  "Determines which enemies have been shot, and generates new powerups
   if an entire group of eligible enemies has been shot down."
  (let [shot-enemies   (filter (fn [enemy] (shot-by-any? enemy player-bullets)) enemies)
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
              {:keys [type powerup-opportunity boss dir init-coords]} new-wave
              new-enemies         (map (fn [ic] (e/make-enemy type ic)) init-coords)
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

(defn check-boss-dead [{{:keys [hitboxes status]} :boss
                        level-status :level-status
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

(defmulti update-game (fn [state]
  [(:game-status state) (:level-status state)]))

(defmethod update-game [:paused nil] [state]
  state)

(defmethod update-game [:waiting nil] [state]
  state)

(defmethod update-game [:playing :waves] [state]
  (-> state
    (set-current-time)
    (v/clear-previous-events)
    (clear-offscreen-objects)
    (check-powerup-opportunities)
    (o/detect-all-collisions)
    (generate-enemies)
    (generate-enemy-bullets)
    (generate-bg-objects)
    (m/move-all-objects)))

(defmethod update-game [:playing :boss] [state]
  (-> state
    (set-current-time)
    (v/clear-previous-events)
    (clear-offscreen-objects)
    (check-powerup-opportunities)
    (o/check-boss-shot)
    (check-boss-dead)
    (check-boss-offscreen)
    (o/check-power-ups)
    (o/check-grazed-bullets)
    (b/generate-boss-bullets)
    (generate-bg-objects)
    (m/move-all-objects)
    (m/move-boss)))

(defmethod update-game [:playing :end] [state]
  (-> state
    (set-current-time)
    (v/clear-previous-events)
    (generate-bg-objects)
    (m/move-player)
    (m/move-player-bullets)
    (m/move-bg-objects)))

(defmethod update-game :default [state]
  state)

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
  :update       update-game
  :key-pressed  c/key-pressed
  :key-released c/key-released
  :draw         draw-frame
  :middleware   [w/fun-mode])

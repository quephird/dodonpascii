(ns dodonpascii.core
  "This is the main module of the game, where the game loop resides."
  (:import  [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w]
            [dodonpascii.bullets :as b]
            [dodonpascii.collision :as o]
            [dodonpascii.controls :as c]
            [dodonpascii.entities :as e]
            [dodonpascii.events :as v]
            [dodonpascii.generation :as n]
            [dodonpascii.graphics :as g]
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

(defn check-game-over [{{lives :lives} :player
                        :keys [game-status level-status] :as state}]
  (let [[new-game-status new-level-status] (if (zero? lives)
                                             [:game-over nil]
                                             [game-status level-status])]
    (-> state
      (assoc-in [:game-status] new-game-status)
      (assoc-in [:level-status] new-level-status))))

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
      (assoc-in [:level-status] new-level-status)
      (assoc-in [:end-level-time] (h/get-current-time)))))

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

(defn reset-enemy-statuses [{enemies :enemies :as state}]
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
    (set-current-time)
    (check-game-over)
    (v/clear-previous-events)
    (clear-offscreen-objects)
    (reset-enemy-statuses)
    (check-powerup-opportunities)
    (o/detect-all-collisions)
    (n/generate-all-objects)
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
    (n/generate-bg-objects)
    (m/move-all-objects)
    (m/move-boss)))

(defmethod update-game [:playing :end] [state]
  (-> state
    (set-current-time)
    (v/clear-previous-events)
    (n/generate-bg-objects)
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
  :renderer     :p3d
  :setup        setup
  :update       update-game
  :key-pressed  c/key-pressed
  :key-released c/key-released
  :draw         draw-frame
  :middleware   [w/fun-mode])

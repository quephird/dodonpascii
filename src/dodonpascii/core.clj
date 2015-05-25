(ns dodonpascii.core
  "This is the main module of the game, where the game loop resides."
  (:import  [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w])
  (:use     [dodonpascii.collision :as o]
            [dodonpascii.controls :as c]
            [dodonpascii.entities :as e]
            [dodonpascii.graphics :as g]
            [dodonpascii.levels :as l]
            [dodonpascii.motion :as m]
            [dodonpascii.sound :as s] :reload-all))

(defn setup []
  "Sets up the initial game state, is called once at the beginning of the game."
  (let [w (q/width)
        h (q/height)
        m (Minim.)]
    (q/smooth)
    (q/image-mode :center)
    (q/color-mode :hsb)
    (e/make-game w h m)))

(defn clear-previous-events [state]
  "Returns the game state with all handled events removed, is called at the beginning of the game loop."
  (assoc-in state [:events] []))

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

(defn check-power-ups [{:keys [power-ups
                               player] :as state}]
  "Removes all power-ups that the player collides with;
   updates lives, shots, and bombs accordingly and registers sound events."
  (let [new-power-ups (remove (fn [power-up] (o/collided-with? player power-up)) power-ups)]
    (if (= (count new-power-ups) (count power-ups))
      state
      (-> state
        (update-in [:events] conj :extra-shots-pickup)
        (update-in [:player :bullet-count] inc)
        (assoc-in [:power-ups] new-power-ups)))))


; TODO: This does not work if the game is paused.
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
        (let [boss-parms (get-in levels [current-level :boss])
              new-boss   (make-boss boss-parms)]
          (-> state
            (assoc-in [:level-status] :boss)
            (assoc-in [:boss] new-boss)))
      (< seconds-into-level current-spawn-time)
        state
      :else
        (let [new-wave            (get-in levels [current-level :waves current-spawn-time])
              {:keys [type powerup-opportunity boss dir init-coords]} new-wave
              new-enemies         (map (fn [[x y θ]] (e/make-enemy x y θ type dir)) init-coords)
              new-spawn-time      (l/get-next-spawn-time levels current-level seconds-into-level)
              new-powerup-opportunities (if powerup-opportunity
                                           (conj powerup-opportunities (map :id new-enemies))
                                           powerup-opportunities)]
          (-> state
            (update-in [:enemies] concat new-enemies)
            (assoc-in [:powerup-opportunities] new-powerup-opportunities)
            (assoc-in [:current-spawn-time] new-spawn-time))))))

(defn generate-enemy-bullets [{:keys [enemies player] :as state}]
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
                             {:type :enemy-shot :x x :y y :θ 0 :ϕ ϕ}))))
        new-events   (repeat (count new-bullets) :new-enemy-shot)]
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

(defmulti update-game (fn [state]
  [(:game-status state) (:level-status state)]))

(defmethod update-game [:paused nil] [state]
  state)

(defmethod update-game [:waiting nil] [state]
  state)

(defmethod update-game [:playing :waves] [state]
  (-> state
    (assoc-in [:current-time] (System/currentTimeMillis))
    (clear-previous-events)
    (check-powerup-opportunities)
    (o/check-enemies-shot)
    (check-power-ups)
    (o/check-grazed-bullets)
    (generate-enemies)
    (generate-enemy-bullets)
    (generate-bg-objects)
    (m/move-player)
    (m/move-player-bullets)
    (m/move-power-ups)
    (m/move-enemies)
    (m/move-enemy-bullets)
    (m/move-bg-objects)))

(defmethod update-game [:playing :boss] [state]
  (-> state
    (assoc-in [:current-time] (System/currentTimeMillis))
    (clear-previous-events)
    (check-powerup-opportunities)
    (o/check-enemies-shot)
    (o/check-boss-shot)
    (check-power-ups)
    (o/check-grazed-bullets)
    (generate-enemy-bullets)
    (generate-bg-objects)
    (m/move-player)
    (m/move-player-bullets)
    (m/move-power-ups)
    (m/move-enemies)
    (m/move-boss)
    (m/move-enemy-bullets)
    (m/move-bg-objects)))

(defmethod update-game :default [state]
  state)

(defn- draw-frame-helper [state]
  (g/draw-background state)
  (g/draw-score state)
  (g/draw-lives state)
  (g/draw-player state)
  (g/draw-player-bullets state)
  (g/draw-power-ups state)
  (g/draw-enemies state)
  (g/draw-enemy-bullets state))

(defmulti draw-frame (fn [state]
  [(:game-status state) (:level-status state)]))

(defmethod draw-frame [:playing :waves] [state]
  "This is the main game rendering function."
  (s/handle-sounds state)
  (draw-frame-helper state))

(defmethod draw-frame [:playing :boss] [state]
  "This is the main game rendering function."
  (s/handle-sounds state)
  (draw-frame-helper state)
  (g/draw-boss state))

(defmethod draw-frame [:paused nil] [{w :w h :h
                                   {paused :paused} :sprites :as state}]
  (draw-frame-helper state)
  (q/image paused (* 0.5 w) (* 0.5 h)))

(defmethod draw-frame [:waiting nil] [{w :w h :h
                                      {logo :logo} :sprites}]
  (q/background 0)
  (q/image logo (* 0.5 w) (* 0.5 h)))

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

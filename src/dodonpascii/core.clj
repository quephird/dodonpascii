(ns dodonpascii.core
  (:import  [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w])
  (:use     [dodonpascii.controls :as c]
            [dodonpascii.graphics :as g]
            [dodonpascii.levels :as l]
            [dodonpascii.motion :as m]
            [dodonpascii.resources :as r]
            [dodonpascii.sound :as s]))

; TODO: Need to return something other than nil here.
(defn get-next-spawn-time [levels current-level current-spawn-time]
  "Determines the next time that enemies should spawn."
  (let [next-spawn-times (->> current-level
                           all-levels
                           keys
                           (filter #(> % current-spawn-time)))]
    (if (empty? next-spawn-times)
      nil
      (apply min next-spawn-times))))

(defn make-player [x y]
  {:lives        3
   :score        0
   :x            x
   :y            y
   :direction-x  0
   :direction-y  0
   :bullet-mode  :shot
   :bullet-count 1})

(defn make-game [w h m]
  "Initializes the entire state of the game including all needed resources."
  {:w                 w
   :h                 h
   :levels            l/all-levels
   :status            :in-progress
   :current-level     1
   :current-spawn-time (get-next-spawn-time all-levels 1 0)
   :start-level-time  (System/currentTimeMillis)
   :current-time      (System/currentTimeMillis)
   :powerup-opportunities []
   :player            (make-player (* w 0.5) (* h 0.8))
   :player-bullets    []
   :powerups          []
   :enemies           []
   :enemy-bullets     []
   :events            []
   :fonts             (r/load-fonts)
   :sprites           (r/load-sprites)
   :sounds            (r/load-sounds m)})

(defn get-score [enemy-type]
  ({:heli    100
    :biplane 150} enemy-type))

(defn setup []
  "Called once at the beginning of the game."
  (let [w (q/width)
        h (q/height)
        m (Minim.)]
    (q/smooth)
    (q/image-mode :center)
    (q/color-mode :hsb)
    (make-game w h m)))

(defn clear-previous-events [state]
  "Called at the beginning of the game loop to erase all handled events."
  (assoc-in state [:events] []))

(defn heli-shot? [{heli-x :x heli-y :y}
                  {bullet-x :x bullet-y :y}]
  "Returns true if the bullet is within the hitbox of the heli"
  (let [range-x 24
        range-y 24]
    (and (< (Math/abs (- bullet-x heli-x)) range-x)
         (< (Math/abs (- bullet-y heli-y)) range-y))))

(defn heli-shot-by-any? [enemy bullets]
  (not (not-any? (fn [bullet] (heli-shot? enemy bullet)) bullets)))

; TODO: Figure out if there is a way to avoid having to compute
;         shot enemies both here and in the check-enemies-shot routine.
(defn check-powerup-opportunities [{enemies               :enemies
                                    bullets               :player-bullets
                                    powerup-opportunities :powerup-opportunities
                                    power-ups             :power-ups :as state}]
  "Determines which enemies have been shot, and generates new powerups
   if an entire group of eligible enemies has been shot down."
  (let [shot-enemies   (filter (fn [enemy] (heli-shot-by-any? enemy bullets)) enemies)
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

; TODO: Generalize hitbox function for multiple enemies
(defn check-enemies-shot [{enemies :enemies
                           powerup-opportunities :powerup-opportunities
                           bullets :player-bullets :as state}]
  "Removes all enemies that are shot; updates score accordingly and
   registers sound events."
  (let [new-enemies    (remove (fn [enemy] (heli-shot-by-any? enemy bullets)) enemies)
        shot-enemies   (filter (fn [enemy] (heli-shot-by-any? enemy bullets)) enemies)
        shot-enemy-ids (->> shot-enemies (map :id) set)
        new-points     (->> shot-enemies (map :type) (map get-score) (reduce + 0))
        new-event      (if (< (count new-enemies) (count enemies)) :enemy-dead)]
    (-> state
      (update-in [:player :score] + new-points)
      (update-in [:events] conj new-event)
      (assoc-in [:enemies] new-enemies))))

(defn collided-with? [{entity1-x :x entity1-y :y}
                      {entity2-x :x entity2-y :y}]
  "Returns true if the x and y coordinates of each entity are sufficiently close."
  (let [range-x 24
        range-y 24]
    (and (< (Math/abs (- entity1-x entity2-x)) range-x)
         (< (Math/abs (- entity1-y entity2-y)) range-y))))

(defn check-power-ups [{power-ups :power-ups
                        player    :player :as state}]
  "Removes all power-ups that the player collides with;
   updates lives, shots, and bombs accordingly and registers sound events."
  (let [new-power-ups (remove (fn [power-up] (collided-with? player power-up)) power-ups)]
    (if (= (count new-power-ups) (count power-ups))
      state
      (-> state
        (update-in [:events] conj :extra-shots-pickup)
        (update-in [:player :bullet-count] inc)
        (assoc-in [:power-ups] new-power-ups)))))

(defn make-enemy [id init-t init-x init-y init-θ enemy-type make-attack-fn dir]
  {:id        id
   :type      enemy-type
   :attack-fn (make-attack-fn init-t init-x init-y init-θ dir)
   :t         init-t
   :x         init-x
   :y         init-y
   :θ         init-θ})

; TODO: simplify the call to make-enemy somehow.
(defn generate-enemies [{w                   :w
                         h                   :h
                         powerup-opportunities :powerup-opportunities
                         start-level-time    :start-level-time
                         current-level       :current-level
                         current-spawn-time  :current-spawn-time
                         levels              :levels :as state}]
  "Returns the game state either untouched or with new enemies
   depending if the current time coincides with a spawn time."
  (let [seconds-into-level (* 0.001 (- (System/currentTimeMillis) start-level-time))]
    (if (< seconds-into-level current-spawn-time)
      state
      (let [{enemy-type          :type
             powerup-opportunity :powerup-opportunity
             make-attack-fn      :make-attack-fn
             dir                 :dir
             init-coords         :init-coords} (get-in levels [current-level current-spawn-time])
             new-enemies    (map (fn [[x y θ]] (make-enemy (gensym "") (System/currentTimeMillis) x y θ enemy-type make-attack-fn dir)) init-coords)
             new-spawn-time (get-next-spawn-time levels current-level seconds-into-level)
             new-powerup-opportunities (if powerup-opportunity
                                         (conj powerup-opportunities (map :id new-enemies))
                                         powerup-opportunities)]
        (-> state
          (update-in [:enemies] concat new-enemies)
          (assoc-in [:powerup-opportunities] new-powerup-opportunities)
          (assoc-in [:current-spawn-time] new-spawn-time))))))

(defn generate-enemy-bullets [{:keys [enemies player] :as state}]
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

(defn update-game [state]
  "This is the main game state update function."
  (-> state
    (assoc-in [:current-time] (System/currentTimeMillis))
    (clear-previous-events)
    (check-powerup-opportunities)
    (check-enemies-shot)
    (check-power-ups)
    (generate-enemies)
    (generate-enemy-bullets)
    (m/move-player)
    (m/move-player-bullets)
    (m/move-power-ups)
    (m/move-enemies)
    (m/move-enemy-bullets)))

(defn draw-frame [state]
  "This is the main game rendering function."
  (s/handle-sounds state)
  (g/draw-background state)
  (g/draw-score state)
  (g/draw-lives state)
  (g/draw-player state)
  (g/draw-player-bullets state)
  (g/draw-power-ups state)
  (g/draw-enemies state)
  (g/draw-enemy-bullets state)
  )

(q/defsketch dodonpascii
  :size         [1200 800]
  :title        "🚀　🔸🔸🔸🔸🔸🔸🔸🔸  dodonpascii  🔹🔹🔹 💥 👾 💥 👾 👾"
  :setup        setup
  :update       update-game
  :key-pressed  c/key-pressed
  :key-released c/key-released
  :draw         draw-frame
  :middleware   [w/fun-mode])

(ns dodonpascii.core
  (:import [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

(def all-levels
  "This defines levels in the following manner:

   {level-number1
     {spawn-time1 [enemy-type number-of-enemies]
      spawn-time2 [enemy-type number-of-enemies]}}
      .
      .
      .
    level-number2
      {spawn-time1 [enemy-type number-of-enemies]
       spawn-time2 [enemy-type number-of-enemies]}}
      .
      .
      .

    Times are in seconds."
  {1
    {5    [:heli 2]
     10   [:heli 3]
     15   [:heli 4]
     20   [:heli 5]
     25   [:heli 5]
     30   [:heli 5]
     35   [:heli 5]
     40   [:heli 5]
     }}
  )

(defn get-next-spawn-time [levels current-level current-spawn-time]
  "Determines the next time that enemies should spawn."
  (let [next-spawn-times (->> (all-levels current-level)
                           keys
                           (filter #(> % current-spawn-time)))]
    (if (empty? next-spawn-times)
      nil
      (apply min next-spawn-times))))

(defn make-game [w h m]
  "Initializes the entire state of the game including all needed resources."
  {:w               w
   :h               h
   :levels          all-levels
   :status          :in-progress
   :current-level   1
   :current-spawn-time (get-next-spawn-time all-levels 1 0)
   :start-level-time (System/currentTimeMillis)
   :player         {:x            (* w 0.5)
                    :y            (* h 0.8)
                    :direction-x  0
                    :direction-y  0
                    :bullet-mode  :shot
                    :bullet-count 1}
   :player-bullets []
   :enemies        []
   :enemy-bullets  []
   :events         []
   :sprites        {:player           [(q/load-image "resources/player1.png")
                                       (q/load-image "resources/player2.png")]
                    :heli             [(q/load-image "resources/heli1.png")
                                       (q/load-image "resources/heli2.png")]
                    :player-shot       (q/load-image "resources/player-shot.png")}
   :sounds         {:new-player-shot   (.loadFile m "resources/new-player-shot.wav")}})

(defn setup []
  "Called once at the beginning of the game."
  (let [w (q/width)
        h (q/height)
        m (Minim.)]
    (q/smooth)
    (q/image-mode :center)
    (make-game w h m)))

(defn clear-previous-events [state]
  "Called at the beginning of the game loop to erase all handled events."
  (assoc-in state [:events] []))

(defn move-player [{{direction-x :direction-x
                     direction-y :direction-y} :player :as state}]
  "Returns the game state with the player moved to a new position."
  (let [dx (* direction-x 5)
        dy (* direction-y 5)]
    (-> state
      (update-in [:player :x] + dx)
      (update-in [:player :y] + dy))))

(defn move-player-bullets [{w       :w
                            bullets :player-bullets :as state}]
  "Returns the game state with all player bullets moved to new positions."
  (let [spin-dθ      5
        new-bullets  (->> bullets
                       (filter (fn [{x :x y :y}] (and (> y 0) (> x 0) (< x w))))
                       (map (fn [{ϕ :ϕ :as bullet}] (-> bullet
                                                       (update-in [:x] + (* 10 (q/sin (q/radians ϕ))))
                                                       (update-in [:y] - (* 10 (q/cos (q/radians ϕ))))
                                                       (update-in [:θ] + spin-dθ)))))]
    (assoc-in state [:player-bullets] new-bullets)))

(defn generate-enemies [{w                   :w
                         start-level-time    :start-level-time
                         current-level       :current-level
                         current-spawn-time  :current-spawn-time
                         levels              :levels :as state}]
  "Returns the game state either untouched or with new enemies
   depending if the current time coincides with a spawn time."
  (let [seconds-into-level (* 0.001 (- (System/currentTimeMillis) start-level-time))]
    (if (< seconds-into-level current-spawn-time)
      state
        (let [[enemy-type enemy-count] (get-in levels [current-level current-spawn-time])
              new-enemies (repeatedly enemy-count (fn [] {:x (q/random (* 0.3 w) (* 0.7 w))
                                                          :y (q/random -200 -100)}))
              new-spawn-time (get-next-spawn-time levels current-level seconds-into-level)]
          (-> state
            (update-in [:enemies] concat new-enemies)
            (assoc-in [:current-spawn-time] new-spawn-time))))))

(defn move-enemies [{w       :w
                     h       :h
                     enemies :enemies :as state}]
  "Returns the game state with all enemies moved to new positions,
   and filtering out those that have moved off-screen."
  (let [new-enemies  (->> enemies
                       (filter (fn [{x :x y :y}] (and (< y (+ h 100)) (> x 0) (< x w))))
                       (map (fn [enemy] (-> enemy
                                          (update-in [:y] + 5)))))]
    (assoc-in state [:enemies] new-enemies)))

(defn update-game [state]
  "This is the main game state update function."
  (-> state
    (clear-previous-events)
    (generate-enemies)
    (move-player)
    (move-player-bullets)
    (move-enemies)))

(defn add-player-bullets [{{x           :x
                            y           :y
                           bullet-count :bullet-count} :player
                           sounds                    :sounds :as state}]
  "Returns the game state with new bullets added to the existing list."
  (let [ϕs          (map #(* 20 (- % (/ (dec bullet-count) 2))) (range bullet-count))
        new-bullets (for [ϕ ϕs] {:type :player-shot :x x :y (- y 35) :ϕ ϕ :θ 0})]
    (doto (:new-player-shot sounds) .rewind .play)
    (update-in state [:player-bullets] concat new-bullets)))

(defn key-pressed [state
                  {key      :key
                   key-code :key-code :as event}]
  "Returns the game state in response to keys changing the player's
   dierction or firing various weapons."
  (case key
    :z
      (add-player-bullets state)
    :left
      (assoc-in state [:player :direction-x] -1)
    :right
      (assoc-in state [:player :direction-x] 1)
    :up
      (assoc-in state [:player :direction-y] -1)
    :down
      (assoc-in state [:player :direction-y] 1)
    state))

(defn key-released [{{x :x y :y} :player :as state}]
  "Returns the game state with player's direction set to 0
   depending on the key pressed."
  (let [key-code (q/key-code)]
    (cond
      (contains? #{37 39} key-code)
        (assoc-in state [:player :direction-x] 0)
      (contains? #{38 40} key-code)
        (assoc-in state [:player :direction-y] 0)
      :else
        state)))

(defn draw-background [state]
  "Renders the game background."
  (q/background 0)
  )

(defn draw-player [{{x :x y :y} :player
                    {sprites :player} :sprites}]
  "Renders the player."
  (let [idx (mod (quot (q/frame-count) 15) 2)]
    (q/image (sprites idx) x y))
  )

(defn draw-player-bullets [{bullets :player-bullets
                            sprites :sprites}]
  "Renders the player's bullets."
  (doseq [{bullet-type :type x :x y :y θ :θ} bullets]
    (q/push-matrix)
    (q/translate x y)
    (q/rotate (q/radians θ))
    (q/image (bullet-type sprites) 0 0)
    (q/pop-matrix)))

(defn draw-enemies [{enemies :enemies
                    {sprites :heli} :sprites}]
  "Renders the enemies."
  (let [idx (mod (quot (q/frame-count) 4) 2)]
    (doseq [{x :x y :y} enemies]
      (q/image (sprites idx) x y))))

(defn draw-frame [state]
  "This is the main game rendering function."
  (draw-background state)
  (draw-player state)
  (draw-player-bullets state)
  (draw-enemies state)
  )

(q/defsketch dodonpascii
  :size         [1200 800]
  :title        "|> - - -  {^_^}              dodonpascii             [@ @] - - - <|"
  :setup        setup
  :update       update-game
  :key-pressed  key-pressed
  :key-released key-released
  :draw         draw-frame
  :middleware   [m/fun-mode])

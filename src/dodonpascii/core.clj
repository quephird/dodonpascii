(ns dodonpascii.core
  (:import [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

; TODO: Think about how to declare formations of baddies;
;         perhaps this could be accomplished by declaring initial positions
;         right here.
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
    {5    [:heli heli-fn [[100 -100 0][200 -100 0]]]
     10   [:heli heli-fn [[100 -100 0][200 -100 0][300 -100 0]]]
     15   [:heli heli-fn [[100 -100 0][200 -100 0][300 -100 0][400 -100 0]]]
     20   [:biplane biplane-fn [[1200 400 90][1350 400 90][1500 400 90][1650 400 90]]]
     25   [:heli 5]
     30   [:heli 5]
     35   [:heli 5]
     40   [:heli 5]
     45   [:biplane 5]
     50   [:heli 5]
     55   [:heli 5]
     60   [:heli 5]
     }}
  )

; TODO: Need to return something other than nil here.
(defn get-next-spawn-time [levels current-level current-spawn-time]
  "Determines the next time that enemies should spawn."
  (let [next-spawn-times (->> (all-levels current-level)
                           keys
                           (filter #(> % current-spawn-time)))]
    (if (empty? next-spawn-times)
      nil
      (apply min next-spawn-times))))

(defn load-sprites []
  {:player           [(q/load-image "resources/player1.png")
                      (q/load-image "resources/player2.png")]
   :heli             [(q/load-image "resources/heli1.png")
                      (q/load-image "resources/heli2.png")]
   :biplane          [(q/load-image "resources/biplane1.png")
                      (q/load-image "resources/biplane2.png")]
   :player-shot       (q/load-image "resources/player-shot.png")
   :extra-shots       (q/load-image "resources/extra-shots.png")})

(defn load-sounds [m]
  {:new-player-shot    (.loadFile m "resources/new-player-shot.wav")
   :enemy-dead         (.loadFile m "resources/enemy-dead.wav")
   :extra-shots-pickup (.loadFile m "resources/extra-shots-pickup.wav")})

(defn make-game [w h m]
  "Initializes the entire state of the game including all needed resources."
  {:w               w
   :h               h
   :levels          all-levels
   :status          :in-progress
   :current-level   1
   :current-spawn-time (get-next-spawn-time all-levels 1 0)
   :start-level-time (System/currentTimeMillis)
   :current-time     (System/currentTimeMillis)
   :player         {:lives        3
                    :score        0
                    :x            (* w 0.5)
                    :y            (* h 0.8)
                    :direction-x  0
                    :direction-y  0
                    :bullet-mode  :shot
                    :bullet-count 1}
   :player-bullets []
   :power-ups      []
   :enemies        []
   :enemy-bullets  []
   :events         []
   :sprites        (load-sprites)
   :sounds         (load-sounds m)})

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

(defn heli-shot? [{heli-x :x heli-y :y}
                  {bullet-x :x bullet-y :y}]
  "Returns true if the bullet is within the hitbox of the heli"
  (let [range-x 24
        range-y 24]
    (and (< (Math/abs (- bullet-x heli-x)) range-x)
         (< (Math/abs (- bullet-y heli-y)) range-y))))

(defn heli-shot-by-any? [enemy bullets]
  (not (not-any? (fn [bullet] (heli-shot? enemy bullet)) bullets)))

; TODO: Generalize when new enemy types are introduced.
;       Look into quad tree for more efficient collision detection.
(defn check-enemies-shot [{enemies :enemies
                           bullets :player-bullets :as state}]
  "Removes all enemies that are shot; updates score accordingly and
   registers sound events."
  (let [new-enemies (remove (fn [enemy] (heli-shot-by-any? enemy bullets)) enemies)
        new-event   (if (< (count new-enemies) (count enemies)) :enemy-dead)]
    (-> state
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

(defn move-power-ups [{w         :w
                       h         :h
                       power-ups :power-ups :as state}]
  "Returns the game state with all extant power ups moved to new positions."
  (let [new-power-ups  (->> power-ups
                         (filter (fn [{x :x y :y}] (and (< y h) (> x 0) (< x w))))
                         (map (fn [power-up] (-> power-up
                                               (update-in [:y] + 3)))))]
    (assoc-in state [:power-ups] new-power-ups)))

; TODO: This is only a temporary means of generating powerups,
;         powerups should be generated by shooting a patrol of special enemies.
(defn generate-power-ups [{w                :w
                           start-level-time :start-level-time :as state}]
  (let [millis-into-level (- (System/currentTimeMillis) start-level-time)]
    (if (< (mod millis-into-level 5000) 10)
      (-> state
        (update-in [:power-ups] conj {:type :extra-shots :x (q/random w) :y 0}))
      state)))

(defn make-enemy [x y θ t enemy-type move-fn]
  (let [init-x x
        init-y y
        init-θ θ
        init-t t]
    {:type enemy-type
     :move-fn move-fn
     :init-t init-t :t init-t
     :init-x init-x :x init-x
     :init-y init-y :y init-y
     :init-θ init-θ :θ init-θ}))

(defn generate-enemies [{w                   :w
                         h                   :h
                         start-level-time    :start-level-time
                         current-level       :current-level
                         current-spawn-time  :current-spawn-time
                         levels              :levels :as state}]
  "Returns the game state either untouched or with new enemies
   depending if the current time coincides with a spawn time."
  (let [seconds-into-level (* 0.001 (- (System/currentTimeMillis) start-level-time))]
    (if (< seconds-into-level current-spawn-time)
      state
        (let [[enemy-type move-fn enemy-coords] (get-in levels [current-level current-spawn-time])
              new-enemies (map (fn [[x y θ]] (make-enemy x y θ (System/currentTimeMillis) enemy-type move-fn)) enemy-coords)
              new-spawn-time (get-next-spawn-time levels current-level seconds-into-level)]
          (-> state
            (update-in [:enemies] concat new-enemies)
            (assoc-in [:current-spawn-time] new-spawn-time))))))

; TODO: improve magic number management; what do they actually mean?
(defn heli-fn [{init-t :init-t
                init-x :init-x
                init-y :init-y
                init-θ :init-θ :as heli}
               t]
  (let [dt (* 0.001 (- t init-t))]
    (-> heli
      (update-in [:x] (fn [x] (+ init-x (* 100 dt))))
      (update-in [:y] (fn [y] (+ 400 (* -400 (- dt 2) (- dt 2)))))
      (update-in [:θ] (fn [θ] (+ init-θ (* -40 dt)))))))

(defn biplane-fn [{init-t :init-t
                   init-x :init-x
                   init-y :init-y
                   init-θ :init-θ :as biplane}
                   t]
  (let [dt (* 0.001 (- t init-t))]
    (-> biplane
      (update-in [:x] (fn [x] (- init-x (* 400 dt)))))
    )
  )

; TODO: Need to handle specialized functions per enemy type
(defn move-enemies [{w       :w
                     h       :h
                     t       :current-time
                     enemies :enemies :as state}]
  "Returns the game state with all enemies moved to new positions,
   and filtering out those that have moved off-screen."
  (let [new-enemies  (->> enemies
                       (filter (fn [{x :x y :y}] (and (< y (+ h 100)) (> x -500) (< x (+ w 500)))))
                       (map (fn [{move-fn :move-fn :as enemy}] (move-fn enemy t))))]
    (assoc-in state [:enemies] new-enemies)))

; TODO: clean up
(defn update-game [state]
  "This is the main game state update function."
  (-> state
    (assoc-in [:current-time] (System/currentTimeMillis))
    (clear-previous-events)
    (check-enemies-shot)
    (check-power-ups)
    (generate-power-ups)
    (generate-enemies)
    (move-player)
    (move-player-bullets)
    (move-power-ups)
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

(defn handle-sounds [{events :events
                      sounds :sounds}]
  "Either plays new sounds or stops them depending on the events in question."
  (doseq [event events]
    (case event
      :enemy-dead
        (doto (sounds event) .rewind .play)
      :extra-shots-pickup
        (doto (sounds event) .rewind .play)
      nil)))

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

(defn draw-power-ups [{power-ups :power-ups
                       sprites   :sprites}]
  "Renders the player's bullets."
  (doseq [{power-up-type :type x :x y :y} power-ups]
    (q/image (power-up-type sprites) x y)))

(defn draw-enemies [{enemies :enemies
                     sprites :sprites}]
  "Renders the enemies."
  (let [idx (mod (quot (q/frame-count) 4) 2)]
    (doseq [{enemy-type :type x :x y :y θ :θ} enemies]
      (q/push-matrix)
      (q/translate x y)
      (q/rotate (q/radians θ))
      (q/image ((sprites enemy-type) idx) 0 0)
      (q/pop-matrix)
      )))

(defn draw-frame [state]
  "This is the main game rendering function."
  (handle-sounds state)
  (draw-background state)
  (draw-player state)
  (draw-player-bullets state)
  (draw-power-ups state)
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

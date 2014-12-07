(ns dodonpascii.core
  (:import [ddf.minim Minim])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

(defn make-game [w h m]
  "Initializes the entire state of the game including all needed resources."
  {:w               w
   :h               h
   :status          :in-progress
   :player         {:x            (* w 0.5)
                    :y            (* h 0.8)
                    :direction-x  0
                    :direction-y  0
                    :bullet-mode  :shot
                    :bullet-count 1}
   :player-bullets []
   :events         []
   :sprites        {:player            (q/load-image "resources/player.png")
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

(defn update-game [state]
  "This is the main game state update function."
  (-> state
    (clear-previous-events)
    (move-player)
    (move-player-bullets)))

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
                    {sprite :player} :sprites}]
  "Renders the player."
  (q/image sprite x y)
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

(defn draw-frame [state]
  "This is the main game rendering function."
  (draw-background state)
  (draw-player state)
  (draw-player-bullets state)
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

(ns dodonpascii.core
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

(defn make-game [w h]
  {:w               w
   :h               h
   :status          :in-progress
   :font           (q/create-font "Courier" 24)
   :player         {:x            (* w 0.5)
                    :y            (* h 0.8)
                    :direction-x  0
                    :direction-y  0}
   :player-bullets []
   :events         []})

(defn setup []
  (let [w (q/width)
        h (q/height)]
    (q/smooth)
    (make-game w h)))

(defn move-player [{{direction-x :direction-x
                     direction-y :direction-y} :player :as state}]
  (let [dx (* direction-x 5)
        dy (* direction-y 5)]
    (-> state
      (update-in [:player :x] + dx)
      (update-in [:player :y] + dy))))

(defn update-game [state]
  (-> state
    (move-player)))

(defn key-pressed [{{x :x y :y} :player :as state}
                    {key      :key
                     key-code :key-code :as event}]
  (case key
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
  (let [key-code (q/key-code)]
    (cond
      (contains? #{37 39} key-code)
        (assoc-in state [:player :direction-x] 0)
      (contains? #{38 40} key-code)
        (assoc-in state [:player :direction-y] 0)
      :else
        state)))

(defn draw-background [state]
  (q/background 0)
  )

(defn draw-player [{font        :font
                    {x :x y :y} :player}]
  (q/fill 127 0 255)
  (q/text-font font)
  (q/text "  /\\\n //\\\\\n<()()>" x y)
  )

(defn draw-frame [state]
  (draw-background state)
  (draw-player state)
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

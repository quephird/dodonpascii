(ns dodonpascii.core
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))

(defn make-game [w h]
  {:w               w
   :h               h
   :status          :in-progress
   :font           (q/create-font "Courier" 24)
   :player         {:x (* w 0.5)
                    :y (* h 0.8)}
   :player-bullets []
   :events         []})

(defn setup []
  (let [w (q/width)
        h (q/height)]
    (q/smooth)
    (q/no-loop)
    (make-game w h)))

(defn update-game [state]
  state)

(defn key-pressed [{{x :x y :y} :player :as state}
                   {key      :key
                    key-code :key-code :as event}]
  (println key key-code)
  )

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
  :draw         draw-frame
;  :key-released key-released
;  :on-close     stop-all-sounds
  :middleware   [m/fun-mode])

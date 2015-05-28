(ns dodonpascii.entities
  "This module is responsible for creating entities."
  (:use     [dodonpascii.resources :as r]
            [dodonpascii.levels :as l]))

; TODO: This function belongs somewhere else.
(defn get-score [enemy-type]
  "Returns the score for a given enemy type."
  ({:heli       100
    :blue-plane 100
    :biplane    150
    :tank       100} enemy-type))

(defn make-player [x y]
  "Returns a hashmap representing the initial state of the player."
  {:lives        3
   :bombs        3
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
   :game-status       :waiting
   :level-status      nil
   :current-level     1
   :current-spawn-time (l/get-next-spawn-time all-levels 1 0)
   :start-level-time  nil
   :current-time      nil
   :powerup-opportunities []
   :player            (make-player (* w 0.5) (* h 0.8))
   :player-bullets    []
   :powerups          []
   :enemies           []
   :boss              nil
   :enemy-bullets     []
   :bg-objects        []
   :events            []
   :fonts             (r/load-fonts)
   :sprites           (r/load-sprites)
   :sounds            (r/load-sounds m)})

(defn make-enemy [init-x init-y init-θ enemy-type dir]
  "Returns a hashmap representing the initial state of the enemy type passed in."
  (let [init-t (System/currentTimeMillis)]
    {:id        (gensym "")
     :type      enemy-type
     :dir       dir
     :init-t    init-t
     :init-x    init-x
     :init-y    init-y
     :init-θ    init-θ
     :t         init-t
     :x         init-x
     :y         init-y
     :θ         init-θ}))

(defn make-boss [{:keys [type dir init-coords hitbox-params]}]
  "Returns a hashmap representing the initial state of the boss type passed in."
  (let [init-t (System/currentTimeMillis)
        [init-x init-y init-θ] init-coords
        hitboxes (map (fn [[x y s]] {:x x :y y :s s}) hitbox-params)]
    {:id        (gensym "")
     :type      type
     :status    :alive
     :dir       dir
     :init-t    init-t
     :init-x    init-x
     :init-y    init-y
     :init-θ    init-θ
     :t         init-t
     :x         init-x
     :y         init-y
     :θ         init-θ
     :hitboxes  hitboxes}))

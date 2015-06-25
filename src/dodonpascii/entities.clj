(ns dodonpascii.entities
  "This module is responsible for creating entities."
  (:require [quil.core :as q :include-macros true])
  (:use     [dodonpascii.resources :as r]
            [dodonpascii.levels :as l]))

; TODO: This function belongs somewhere else.
(defn get-score [enemy-type]
  "Returns the score for a given enemy type."
  ({:heli        100
    :blue-plane  100
    :large-plane 300
    :biplane     150
    :tank        100} enemy-type))

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
   :current-spawn-time (l/get-next-enemy-spawn-time all-levels 1 0)
   :start-level-time  nil
   :next-boss-bullet-spawn-time nil
   :current-time      nil
   :powerup-opportunities []
   :player            (make-player (* w 0.5) (* h 0.8))
   :player-stats      {:shots-fired 0
                       :enemies 0
                       :enemies-shot 0
                       :bullets-grazed 0
                       :bonus-stars 0}
   :player-bullets    []
   :powerups          []
   :enemies           []
   :enemy-bullets     []
   :boss              nil
   :bonus-items       []
   :bg-objects        []
   :events            []
   :fonts             (r/load-fonts)
   :sprites           (r/load-sprites)
   :sounds            (r/load-sounds m)})

(defn reset-game [{:keys [w h] :as state}]
  (-> state
    (assoc-in [:game-status] :playing)
    (assoc-in [:level-status] :waves)
    (assoc-in [:current-level] 1)
    (assoc-in [:current-spawn-time] (l/get-next-enemy-spawn-time all-levels 1 0))
    (assoc-in [:start-level-time] (System/currentTimeMillis))
    (assoc-in [:current-time] (System/currentTimeMillis))
    (assoc-in [:player] (make-player (* w 0.5) (* h 0.8)))
    (assoc-in [:player-bullets] [])
    (assoc-in [:player-stats] {:shots-fired 0
                               :enemies 0
                               :enemies-shot 0
                               :bullets-grazed 0
                               :bonus-stars 0})
    (assoc-in [:powerups] [])
    (assoc-in [:enemies] [])
    (assoc-in [:enemy-bullets] [])
    (assoc-in [:boss] nil)
    (assoc-in [:bonus-items] [])
    (assoc-in [:bg-objects] [])
    (assoc-in [:events] [])
    ))

(defn make-enemy [enemy-type [init-x init-y init-θ dir hp]]
  "Returns a hashmap representing the initial state of the enemy type passed in."
  (let [init-t (System/currentTimeMillis)]
    {:id        (gensym "")
     :type      enemy-type
     :dir       dir
     :hp        hp
     :status    :alive
     :init-t    init-t
     :init-x    init-x
     :init-y    init-y
     :init-θ    init-θ
     :t         init-t
     :x         init-x
     :y         init-y
     :θ         init-θ}))

(defn make-bonus-star [{:keys [x y]}]
  {:id        (gensym "")
   :type      :bonus-star
   :x         x
   :y         y
   :dx        (q/random -1 1)
   :dy        (q/random 2 4)})

(defn make-boss [{:keys [type dir init-coords hitbox-params bullet-patterns]}]
  "Returns a hashmap representing the initial state of the boss type passed in."
  (let [init-t (System/currentTimeMillis)
        [init-x init-y init-θ] init-coords
        hitboxes (map (fn [[x y hp]] {:x x :y y :hp hp}) hitbox-params)]
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
     :hitboxes  hitboxes
     :bullet-patterns bullet-patterns}))

(ns dodonpascii.entities
  "This module is responsible for creating entities."
  (:require [quil.core :as q :include-macros true]
            [dodonpascii.resources :as r]
            [dodonpascii.levels :as l]))

(defn get-current-time []
  (System/currentTimeMillis))

(defn is-player-starting? [{{start-t :life-start-time} :player
                            curr-t :current-time :as state}]
  (if (> 3 (* 0.001 (- curr-t start-t)))
    true
    false))

(defn make-player [x y]
  "Returns a hashmap representing the initial state of the player."
  {:life-start-time (get-current-time)
   :lives        3
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
   :current-spawn-time (l/get-next-enemy-spawn-time l/all-levels 1 0)
   :start-level-time  nil
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
    (assoc-in [:current-spawn-time] (l/get-next-enemy-spawn-time l/all-levels 1 0))
    (assoc-in [:start-level-time] (get-current-time))
    (assoc-in [:current-time]     (get-current-time))
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
    (assoc-in [:events] [])))

(defn make-enemy [enemy-type [init-x init-y init-θ dir hp]]
  "Returns a hashmap representing the initial state of the enemy type passed in."
  (let [init-t (get-current-time)]
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
  (let [init-t (get-current-time)
        [init-x init-y init-θ] init-coords
        hitboxes (map (fn [[x y hp]] {:x x :y y :hp hp}) hitbox-params)
        bullet-spawn-times (-> bullet-patterns keys cycle)]
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
     :bullet-patterns    bullet-patterns
     :bullet-spawn-times bullet-spawn-times}))

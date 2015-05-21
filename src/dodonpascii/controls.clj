(ns dodonpascii.controls
  (:require [quil.core :as q :include-macros true]))

(defn add-player-bullets [{sounds :sounds
                           {:keys [x y bullet-count]} :player :as state}]
  "Returns the game state with new bullets added to the existing list."
  (let [ϕs          (map #(* 20 (- % (/ (dec bullet-count) 2))) (range bullet-count))
        new-bullets (for [ϕ ϕs] {:type :player-shot :x x :y (- y 35) :ϕ ϕ :θ 0})]
    (doto (:new-player-shot sounds) .rewind .play)
    (update-in state [:player-bullets] concat new-bullets)))

(defmulti key-pressed (fn [state event] (:status state)))

(defmethod key-pressed :paused [state
                                {:keys [key key-code] :as event}]
  "Returns the game state in response to keys changing the player's
   dierction or firing various weapons."
  (case key
    :p
      (assoc-in state [:status] :playing)
    state))

(defmethod key-pressed :playing [state
                                 {:keys [key key-code] :as event}]
  "Returns the game state in response to keys changing the player's
   dierction or firing various weapons."
  (case key
    :p
      (assoc-in state [:status] :paused)
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

(defmethod key-pressed :default [state
                                 {:keys [key key-code] :as event}]
  "NOTA BENE: this will work for :waiting and :game-over statuses."
  (case key
    :s
      ; TODO: Think about where this and other "mutating" functions should go;
      ; this namespace should be "dumb" to the details of the game state.
      (-> state
        (assoc-in [:start-level-time] (System/currentTimeMillis))
        (assoc-in [:current-time] (System/currentTimeMillis))
        (assoc-in [:status] :playing))
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

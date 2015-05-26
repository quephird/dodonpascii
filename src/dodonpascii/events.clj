(ns dodonpascii.events
  "This module is responsible for handling all sound effects and music."
  (:use     [dodonpascii.graphics :as g]))

(defmulti clear-previous-events :level-status)

(defmethod clear-previous-events :boss [state]
  "Returns the game state with all handled events removed, is called at the beginning of the game loop."
  (-> state
    (assoc-in [:boss :status] :alive)
    (assoc-in [:events] [])))

(defmethod clear-previous-events :default [state]
  "Returns the game state with all handled events removed, is called at the beginning of the game loop."
  (-> state
    (assoc-in [:events] [])))

(defn handle-events [{:keys [events sounds] :as state}]
  "Either plays new sounds or stops them depending on the events in question."
  (doseq [event events]
    (cond
      (contains? #{:bullet-graze
                   :enemy-dead
                   :hitbox-shot
                   :extra-shots-pickup
                   :new-enemy-shot} event)
        (doto (sounds event) .rewind .play)
      :else nil)))

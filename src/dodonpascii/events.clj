(ns dodonpascii.events
  "This module is responsible for handling all sound effects and music."
  (:use     [dodonpascii.graphics :as g]))

(defmulti clear-previous-events :level-status)

(defmethod clear-previous-events :boss [{:keys [events current-time] :as state}]
  "Returns the game state with all handled events removed, is called at the beginning of the game loop."
  (let [new-events   (remove (fn [{init-t :init-t}] (< init-t current-time)) events)]
    (-> state
      (assoc-in [:boss :status] :alive)
      (assoc-in [:events] new-events))))

(defmethod clear-previous-events :default [{:keys [events current-time] :as state}]
  "Returns the game state with all handled events removed, is called at the beginning of the game loop."
  (let [new-events   (remove (fn [{init-t :init-t}] (< init-t current-time)) events)]
    (-> state
      (assoc-in [:events] new-events))))

(defn handle-events [{:keys [events sounds current-time] :as state}]
  "Either plays new sounds or stops them depending on the events in question."
  (let [active-events (filter (fn [{init-t :init-t}] (> 25 (- init-t current-time))) events)]
    (doseq [{:keys [type init-t]} active-events]
      (cond
        (contains? #{:bullet-graze
                     :enemy-dead
                     :hitbox-shot
                     :extra-shots-pickup
                     :new-enemy-shot
                     :cone-shot} type)
          (doto (sounds type) .rewind .play)
        :else nil))))

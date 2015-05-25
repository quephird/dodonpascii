(ns dodonpascii.sound
  "This module is responsible for handling all sound effects and music.")

(defn handle-sounds [{:keys [events sounds]}]
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

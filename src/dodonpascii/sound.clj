(ns dodonpascii.sound)

(defn handle-sounds [{:keys [events sounds]}]
  "Either plays new sounds or stops them depending on the events in question."
  (doseq [event events]
    (cond
      (contains? #{:bullet-graze
                   :enemy-dead
                   :extra-shots-pickup
                   :new-enemy-shot} event)
        (doto (sounds event) .rewind .play)
      :else nil)))

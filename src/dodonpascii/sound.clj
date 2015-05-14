(ns dodonpascii.sound)

(defn handle-sounds [{events :events
                      sounds :sounds}]
  "Either plays new sounds or stops them depending on the events in question."
  (doseq [event events]
    (cond
      (contains? #{:new-enemy-shot :enemy-dead :extra-shots-pickup} event)
        (doto (sounds event) .rewind .play)
      :else nil)))

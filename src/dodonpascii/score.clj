(ns dodonpascii.score)

(defn get-score [score-type]
  "Returns the score for a given type."
  ({:heli         100
    :blue-plane   100
    :large-plane  1000
    :biplane      150
    :tank         100
    :boss-hit     100
    :bullet-graze 10
    :bonus-star   250} score-type))

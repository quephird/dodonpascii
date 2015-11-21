(ns dodonpascii.cleanup
  "This module is responsible for clearing all offscreen objects off the board."
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as w]
             :reload-all))

(defn offscreen? [x y w h]
  (let [margin 600]
    (or (<= y (- margin))
        (>= y (+ h margin))
        (<= x (- margin))
        (>= x (+ w margin)))))

(defn clear-objects [object-key {:keys [w h] :as state}]
  (let [curr-objects   (get-in state [object-key])
        new-objects    (remove (fn [{:keys [x y]}] (offscreen? x y w h)) curr-objects)]
    (-> state
      (assoc-in [object-key] new-objects))))

(defn clear-offscreen-objects [state]
  "Returns the game state with all offscreen objects removed.

   Note that we allow for a fairly wide margin outside the field of view.
   This is to allow for enemies to emerge from offscreen in a line for example.
   If we enforced a zero width margin then such enemies would never appear
   in the first place because they would be immmediately scrubbed off!"
  (->> state
    (clear-objects :enemies)
    (clear-objects :bonus-items)
    (clear-objects :enemy-bullets)
    (clear-objects :bg-objects)))

(ns dodonpascii.helpers
  "This module holds cross-cutting utility functions."
  (:require [quil.core :as q :include-macros true]))

(defn get-current-time []
  (System/currentTimeMillis))

(defn set-current-time [state]
  (assoc-in state [:current-time] (get-current-time)))

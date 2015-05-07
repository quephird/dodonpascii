(ns dodonpascii.attack
  (:require [quil.core :as q :include-macros true]))

; TODO: DEAL WITH MAGIC NUMBERS!!!
(defn make-heli-fn [init-t init-x init-y init-θ]
  (fn [heli t]
    (let [dt (* 0.001 (- t init-t))]
      (-> heli
        (update-in [:x] (fn [x] (+ init-x (* 100 dt))))
        (update-in [:y] (fn [y] (+ init-y 500 (* -400 (- dt 2) (- dt 2)))))
        (update-in [:θ] (fn [θ] (+ init-θ (* -40 dt))))))))

(defn make-biplane-fn [init-t init-x init-y init-θ]
  (fn [biplane t]
    (let [dt (* 0.001 (- t init-t))
          t1 (* 0.0025 (- init-x 400))
          t2 (+ t1 2)]
      (-> biplane
        (update-in [:x] (fn [x]
                          (cond
                            (< dt t1)
                              (- init-x (* 400 dt))
                            (< dt t2)
                              (- 400 (* 128 (q/sin (q/radians (* 57 q/PI (- dt t1))))))
                            :else
                              (- 400 (* 400 (- dt t2))))))
        (update-in [:y] (fn [y]
                          (cond
                            (< dt t1)
                              init-y
                            (< dt t2)
                              (+ (- 500 128) (* 128 (q/cos (q/radians (* 57 q/PI (- dt t1))))))
                            :else
                              init-y)))
       (update-in [:θ] (fn [θ]
                          (cond
                            (< dt t1)
                              init-θ
                            (< dt t2)
                              (+ 90 (* 57 q/PI (- dt t1)))
                            :else
                              init-θ)))))))


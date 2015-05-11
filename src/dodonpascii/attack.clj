(ns dodonpascii.attack
  (:require [quil.core :as q :include-macros true]))

; TODO: DEAL WITH MAGIC NUMBERS!!!
(defn make-heli-fn [init-t init-x init-y init-θ dir]
  (fn [heli t]
    (let [f  ({:left - :right +} dir)
          dt (* 0.001 (- t init-t))]
      (-> heli
        (update-in [:x] (fn [x] (f init-x (* 100 dt))))
        (update-in [:y] (fn [y] (+ init-y 500 (* -400 (- dt 2) (- dt 2)))))
        (update-in [:θ] (fn [θ] (f init-θ (* -40 dt))))))))

; TODO: This is _totally_ hacky but it works. Ugh.
(defn make-biplane-fn [init-t init-x init-y init-θ dir]
  (fn [biplane t]
    (let [fx  ({:left - :right +} dir)
          fθ  ({:left + :right -} dir)
          dt (* 0.001 (- t init-t))
          t1 (if (= dir :left) (* 0.0025 (- init-x 400)) (* 0.0025 (- 400 init-x)))
          t2 (+ t1 2)]
      (-> biplane
        (update-in [:x] (fn [x]
                          (cond
                            (< dt t1)
                              (fx init-x (* 400 dt))
                            (< dt t2)
                              (fx 400 (* 128 (q/sin (q/radians (* 57 q/PI (- dt t1))))))
                            :else
                              (fx 400 (* 400 (- dt t2))))))
        (update-in [:y] (fn [y]
                          (cond
                            (< dt t1)
                              init-y
                            (< dt t2)
                              (+ (- init-y 128) (* 128 (q/cos (q/radians (* 57 q/PI (- dt t1))))))
                            :else
                              init-y)))
        (update-in [:θ] (fn [θ]
                          (cond
                            (< dt t1)
                              init-θ
                            (< dt t2)
                              (fθ init-θ (* 57 q/PI (- dt t1)))
                            :else
                              init-θ)))))))


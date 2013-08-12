(ns thi.ng.geom.triangulate
  (:require
    [thi.ng.math.core :as m]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.triangle :as t]
    [thi.ng.geom.types :as gt]))

(defn- add-unique-edge!
  [edges p q]
  (let [e [p q]]
    (if (edges e)
      (disj! edges e)
      (let [e2 [q p]]
        (if (edges e2) (disj! edges e2) (conj! edges e))))))

(defn- compute-edges
  [complete tris [px py]]
  (persistent!
    (reduce
      (fn [state t]
        (if (complete t) state
          (let [x (- px (t 3))
                y (- py (t 4))]
            (if (<= (+ (* x x) (* y y)) (t 5))
              (assoc! state
                0 (let [[a b c] t]
                    (-> (state 0)
                        (add-unique-edge! a b)
                        (add-unique-edge! b c)
                        (add-unique-edge! c a))))
              (assoc! state
                1 (conj! (state 1) t))))))
      (transient [(transient #{}) (transient [])])
      tris)))

(defn- make-triangle [a b c]
  (let [[[ox oy] r] (t/circumcircle* a b c)]
    [a b c ox oy (* r r) (+ ox r)]))

(defn shared-vertex?
  [[a1 b1 c1] [a2 b2 c2]]
  (or (identical? a1 a2) (identical? a1 b2) (identical? a1 c2)
      (identical? b1 a2) (identical? b1 b2) (identical? b1 c2)
      (identical? c1 a2) (identical? c1 b2) (identical? c1 c2)))

(defn triangulate
  [points]
  (let [points (sort-by #(% 0) points)
        bmin (apply g/min2 points)
        bmax (apply g/max2 points)
        bext (g/sub2 bmax bmin)
        dm (max (bext 0) (bext 1))
        d2 (* 2.0 dm)
        m (g/mid2 bmin bmax)
        s (make-triangle (g/sub2 m [d2 dm]) (g/add2 m [0 d2]) (g/add2 m [d2 (- dm)]))]
    (loop [points points tris [s] complete (transient #{})]
      (if-let [[px :as p] (first points)]
        (let [complete (reduce #(if (< (%2 6) px) (conj! % %2) %) complete tris)
              [edges tris] (compute-edges complete tris p)
              tris (reduce #(conj! % (make-triangle (%2 0) (%2 1) p)) tris (persistent! edges))]
          (recur (rest points) (persistent! tris) complete))
        (map #(subvec % 0 3)
          (remove #(shared-vertex? s %) (persistent! (reduce conj! complete tris))))))))

(ns consimilo.minhash-util
  (:require [clojure.set :refer [intersection
                                 union]]
            [clojure.tools.logging :as log]))

(defn scalar-mod
  "performs a scalar modulus on each element of vec and k"
  [v k]
  (mapv #(mod % (bigint k)) v))

(defn scalar-mul
  "performs a scalar multiply on each element of vec and k"
  [v k]
  (mapv #(* % (bigint k)) v))

(defn elementwise-add
  "performs elementwise addition betwen vectors v1 and v1"
  [v1 v2]
  (if (not= (count v1) (count v2))
    (log/error "cannot compute elementwise-add on 2 vectors of different length")
    (mapv + v1 v2)))

(defn elementwise-min
  "performs elementwise minimum between vectors v1 and v2"
  [v1 v2]
  (if (not= (count v1) (count v2))
    (log/error "cannot compute elementwise-min on 2 vectors of different length")
    (mapv min v1 v2)))

(defn dot
  "computes the dot product of two vectors v1 and v2"
  [v1 v2]
  (if (not= (count v1) (count v2))
    (log/error "cannot compute dot product on 2 vectors of different length")
    (reduce + (map * v1 v2))))

(defn l2nrm
  "computes the l2 norm of vector v"
  [v]
  (Math/sqrt (reduce + (map #(* % %) v))))

(defn- similarity
  "helper function for computing cosine distance, computes similarity"
  [v1 v2]
  (/ (dot v1 v2)
     (* (l2nrm v1) (l2nrm v2))))

(defn cosine-distance
  "computes cosine distance between two positive vectors of equal length, v1 and v2 "
  [v1 v2]
  (if (not= (count v1) (count v2))
    (log/error "cannot compute cosine-distance between 2 vectors of different length")
    (/ (* 2 (Math/acos (similarity v1 v2)))
       (Math/PI))))

(defn hamming-distance
  [v1 v2]
  (if (not= (count v1) (count v2))
    (log/error "cannot compute hamming-distance between 2 vectors of different length")
    (->> (map = v1 v2) (filter false?) count)))

(defn jaccard-similarity
  "performs jaccard on vectors self and other"
  [v1 v2]
  (if (not= (count v1) (count v2))
    (log/error "cannot compute jaccard-similarity between 2 vectors of different length")
    (/ (count (intersection (set v1) (set v2)))
       (count (union (set v1) (set v2))))))

(defn zip-similarity
  "returns key value pairs {minhash-key, jaccard}"
  [forest query sim-f]
  (zipmap (:top-k query)
          (map #(sim-f (:query-hash query) (get-in @forest [:keys %]))
               (:top-k query))))
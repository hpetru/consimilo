(ns consimilo.lsh-query
  (:require [consimilo.lsh-state :refer [hashranges
                                         trees
                                         k]]
            [consimilo.lsh-util :refer [slice-minhash
                                        coll-prefix
                                        tree-keys
                                        v>=v
                                        v=v]]
            [config.core :refer [env]]
            [clojure.tools.logging :as log]))

(defn- hashtable-lookup
  "returns collection of values for key in nested hashtable {:tree {:key value}....}"
  [hashtable key]
  (map #(get-in hashtable [% key]) (tree-keys (:trees env))))

(defn- hashtables-lookup
  "returns collection of values for keys in nested hashtable"
  [hashtable keys]
  (map #(hashtable-lookup hashtable %) keys))

(defn- pred-search
  "Finds the first index less then `j` for which `pred` is satisfied."
  ([pred j]
   (pred-search pred j 0))
  ([pred j i]
   (if (>= i j)
     i
     (let [h (int (+ i (/ (- j i) 2)))]
       (if-not (pred h)
         (recur pred j (inc h))
         (recur pred h i))))))

(defn- query-fn
  "performs a binary search to find the r-length prefix over the sorted hashtables"
  [forest min-slice tk r]
  (let [sorted (get-in @forest [:sorted-hash tk])
        hashtable (get-in @forest [:hashtables tk])
        min-prefix (coll-prefix min-slice r)
        sorted-range (dec (count sorted))
        i (pred-search (fn [x]
                         (v>=v
                           (coll-prefix (get sorted x) r)
                           min-prefix))
                       sorted-range)]
    (if (v=v (coll-prefix (get sorted i) r) min-prefix)
      (take-while #(v=v (coll-prefix % r) min-prefix) (drop i sorted)))))

(defn- query-k-prefix
  "queries for the r-length prefix of each minhash slice in the forest"
  [forest minhash r]
  (mapcat #(query-fn forest %1 %2 r)
          (slice-minhash minhash hashranges)
          (tree-keys trees)))

(defn query
  "returns a list of the keys of the top k-items most similar to minhash"
  [forest k-items minhash]
  (cond
    (<= k-items 0) (log/warn "k must be greater than zero")
    (< (count minhash) (* k trees)) (log/warn "the perm of Minhash out of range")
    :else (->> (range k)
               reverse
               (mapcat #(query-k-prefix forest minhash %))
               (hashtables-lookup (get @forest :hashtables))
               flatten
               (filter some?)
               (distinct)
               (take k-items))))
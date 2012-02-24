(ns local
  (:require [cljs.reader :as reader]))

(defn local-storage? []
  (not (undefined? (.-localStorage js/window))))

(defn get-item [k]
  (if (local-storage?)
    (.getItem js/localStorage (if (keyword? k) (name k) k))))

(defn get-clojure [k]
  (if-let [item (get-item k)]
    (reader/read-string item)))

(defn set-item! [k v]
  (if (local-storage?)
    (let [item (if (string? v) v (pr-str v))]
      (.setItem js/localStorage (if (keyword? k) (name k) k) item))))

(defn conj-item!
  "Assume localStorage for key k is (or should become) a vector and conj value v"
  [k v]
  (if (local-storage?)
    (set-item! k (conj (or (get-clojure k) []) v))))
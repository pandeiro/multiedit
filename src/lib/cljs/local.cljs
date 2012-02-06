(ns local
  (:require [cljs.reader :as reader]))

(defn local-storage? []
  (not (undefined? (.-localStorage js/window))))

(defn get-item [k]
  (if (local-storage?)
    (. js/localStorage (getItem k))))

(defn get-clojure [k]
  (if-let [item (get-item k)]
    (reader/read-string item)))

(defn set-item [k v]
  (if (local-storage?)
    (let [item (if (string? v) v (pr-str v))]
      (. js/localStorage (setItem k item)))))


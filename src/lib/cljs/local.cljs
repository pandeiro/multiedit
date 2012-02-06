(ns local
  (:require [cljs.reader :as reader]))

(defn local-storage? []
  (not (undefined? (.-localStorage js/window))))

(defn get-item [key]
  (if (local-storage?)
    (. js/localStorage (getItem key))))

(defn get-clojure [id]
  (reader/read-string (get-item id)))

(defn set-item [k v]
  (if (local-storage?)
    (let [item (if (string? v) v (pr-str v))]
      (. js/localStorage (setItem k item)))))


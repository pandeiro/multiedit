(ns local
  (:require [cljs.reader :as reader]))

(defn local-storage? []
  (not (undefined? (.-localStorage js/window))))

(defn get-item [key]
  (if (local-storage?)
    (reader/read-string (. js/localStorage (getItem (name key))))))

(defn set-item [key item]
  (if (local-storage?)
    (let [item (if (string? item) item (pr-str item))]
      (. js/localStorage (setItem (name key) item)))))


(ns local)

(defn local-storage? []
  (boolean (.-localStorage js/window)))

(defn get-item [key]
  (if (local-storage?)
    (. js/localStorage (getItem key))))

(defn set-item [key item]
  (if (local-storage?)
    (let [item (if (string? item) item (pr-str item))]
      (. js/localStorage (setItem key item)))))


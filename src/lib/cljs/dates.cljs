(ns dates
  (:require [goog.date          :as goog]
            [goog.date.relative :as rel]))

(defn now [] (goog/DateTime.))

(defn date [t]
  (goog/DateTime. (if (or (string? t) (number? t)) (js/Date. t) t)))

(defn millis
  ([] (.getTime (now)))
  ([dt]
     (.getTime dt)))

(defn relative [d]
  (if-let [diff (rel/format (cond (number? d) d
                                  (string? d) (js/parseInt d 10)
                                  :else (millis d)))]
    (if (= diff "0 minutes ago") "just now" diff)))
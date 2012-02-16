(ns ^{:doc "This module no longer uses the one.browser.history library due
  to a buggy implementation of Html5History in the Google Closure library
  which causes duplicate events to fire (one for hashchange and another for
  popstate, both of which are involved in any token change). Its replacement
  is a simple event listener on the hashchange event to detect and
  dispatch a new token."}
  one.sample.history
  (:require [one.dispatch :as dispatch]
            [goog.events  :as event]))

(defn set-token [token]
  (let [loc (.split (.-href (.-location js/window)) "#")
        path (aget loc 0)
        hash (aget loc 1)]
    (if (not= hash token)
      (set! (.-location js/window) (str path \# token)))))

(defn get-token
  ([] (get-token (.-href (.-location js/window))))
  ([loc]
     (let [index (. loc (indexOf "#"))]
        (if (> 0 index) "" (. loc (substring (inc index)))))))

(event/listen js/window
              goog.events.EventType.HASHCHANGE
              (fn [e]
                (-> (.-newURL (.getBrowserEvent e))
                    get-token
                    keyword
                    dispatch/fire)))

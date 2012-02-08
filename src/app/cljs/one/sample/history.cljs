(ns ^{:doc "This module no longer uses the one.browser.history library due
  to a buggy implementation of Html5History in the Google Closure library
  which causes duplicate events to fire (one for hashchange and another for
  popstate, both of which are involved in any token change). Its replacement
  is a simple event listener on the hashchange event to detect and
  dispatch a new token."}
  one.sample.history
  (:require [one.dispatch :as dispatch]
            [goog.events  :as event]))

(event/listen js/window goog.events.EventType.HASHCHANGE
              (fn [e]
                (if (not= (.-oldURL e) (.-newURL e))
                  (let [link  (.createElement js/document "a")
                        url   (set! (.-href link) (.-newURL e))
                        token (.substr (.-hash link) 1)]
                    (dispatch/fire (keyword token))))))



(ns ^{:doc "Render the views for the application."}
  one.sample.view
  (:require-macros [one.sample.snippets :as snippets])
  (:require [one.dispatch :as dispatch]))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the `:state` key."
  :state)

(defmethod render :init [_]
  (. js/console (log "init view rendered")))
  
(dispatch/react-to #{:state-change} (fn [_ m] (render m)))

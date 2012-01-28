(ns ^{:doc "Render the views for the application."}
  one.sample.view
  (:use [domina :only [append! destroy-children!]]
        [query  :only [$]])
  (:require-macros [one.sample.snippets :as snippets])
  (:require [one.dispatch               :as dispatch]))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the `:state` key."
  :state)

(defn load-templates []
  (let [content ($ "#content")
        login (:login snippets)]
    (destroy-children! content)
    (append! content login)))

(defmethod render :init [_]
  (load-templates))
  
(dispatch/react-to #{:state-change} (fn [_ m] (render m)))

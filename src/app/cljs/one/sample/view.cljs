(ns ^{:doc "Render the views for the application."}
  one.sample.view
  (:use [domina :only [append! destroy-children! remove-class! add-class!
                       nodes]]
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
  (let [content ($ "#content")]
    (destroy-children! content)
    (append! content (:login snippets))
    (append! content (:workspace snippets))))

(defmethod render :init [_]
  (load-templates))

(defn- deactivate [id-or-node-or-nodes]
  (remove-class! (if (string? id-or-node-or-nodes)
                   ($ id-or-node-or-nodes)
                   (nodes id-or-node-or-nodes))
                 "active"))

(defn- activate [id-or-node-or-nodes]
  (add-class! (if (string? id-or-node-or-nodes)
                ($ id-or-node-or-nodes)
                (nodes id-or-node-or-nodes))
              "active"))

(dispatch/react-to #{:state-change} (fn [_ m] (render m)))

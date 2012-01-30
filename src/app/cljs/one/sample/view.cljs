(ns ^{:doc "Render the views for the application."}
  one.sample.view
  (:use [domina     :only [append! destroy-children! destroy!
                           remove-class! add-class! nodes]]
        [query      :only [$]]
        [geddy.core :only [field]])
  (:require-macros [one.sample.snippets :as snippets])
  (:require [one.dispatch           :as dispatch]
            [one.sample.view.editor :as editor]
            [one.sample.model       :as model]))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the `:state` key."
  :state)

(defn load-templates []
  (let [content ($ "#content")]
    (-> content
        destroy-children!
        (append! (:login snippets))
        (append! (:workspace snippets)))))

(defmethod render :init [_]
  (comment (load-templates)))

(defn- deactivate! [id-or-node-or-nodes]
  (remove-class! (if (string? id-or-node-or-nodes)
                   ($ id-or-node-or-nodes)
                   (nodes id-or-node-or-nodes))
                 "active"))

(defn- activate! [id-or-node-or-nodes]
  (add-class! (if (string? id-or-node-or-nodes)
                ($ id-or-node-or-nodes)
                (nodes id-or-node-or-nodes))
              "active"))


(defmethod render :workspace [{:keys [who id content history]}]
  (let [main ($ "#content")]
    (destroy! ($ "#workspace" main))
    (append! main (:workspace snippets))
    (deactivate! ($ "#content > div"))
    (activate! ($ "#workspace"))
    (editor/add-document-listeners ($ "#workspace-editor-field")
                                   (model/document-session :who who :id id
                                                           :content content
                                                           :history history))))

(dispatch/react-to #{:document-retrieved}
                   (fn [a m]
                     (.log js/console ":document-retrieved reaction")
                     (.log js/console (pr-str a) (pr-str m))))

(dispatch/react-to #{:state-change} (fn [_ m] (render m)))

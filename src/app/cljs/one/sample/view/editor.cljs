(ns one.sample.view.editor
  (:use [query  :only [$]]
        [domina :only [append! destroy-children! detach! nodes single-node]])
  (:require [goog.events       :as event]
            [goog.editor.Field :as ed]
            [one.dispatch      :as dispatch]))

(def CHANGE goog.editor.Field.EventType.DELAYEDCHANGE)
(def CLICK goog.events.EventType.CLICK)

(defn add-document-listeners [element doc]
  (let [field (doc :view element)
        new       ($ "button#new")
        undo      ($ "button#undo")
        redo      ($ "button#redo")
        set-html! (fn [content] (. field (setHtml false content true)))]
    (event/listen field
                  CHANGE
                  (fn [e] (let [content (. field (getCleanContents))
                                previous (first (doc :get-history))]
                            (doc :set! :content content)
                            (if (not= content previous)
                              (do
                                (doc :conj-history! content)
                                (doc :reset-cursor!))))))
    (event/listen undo
                  CLICK
                  (fn [e] (let [content (doc :undo)] (set-html! content))))
    (event/listen redo
                  CLICK
                  (fn [e] (let [content (doc :redo)] (set-html! content))))
    (event/listen new
                  CLICK
                  (fn [e]
                    (append! ($ "#content") (detach! ($ "#workspace")))
                    (dispatch/fire :workspace {:who (doc :get :who)})))))

(defn add-documents-list-listeners []
  (let [items (nodes ($ "#sidebar-documents > ol > li"))]
    (doseq [item items]
      (event/listen item
                    CLICK
                    (fn [e] (dispatch/fire :document-requested (.-id item)))))))

(defn excerpt [s chars]
  (.substring s 0 chars))

(defn list-documents [documents]
  (let [element ($ "#sidebar-documents > ol")]
    (destroy-children! element)
    (doseq [[id doc] documents]
      (append! element (single-node (str "<li id=\"" (name id) "\">"
                                         (excerpt (:content doc) 20)
                                         "</li>"))))
    (add-documents-list-listeners)))

(dispatch/react-to #{:documents-changed}
                   (fn [t d]
                     (list-documents d)))
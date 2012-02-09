(ns one.sample.view.editor
  (:use [query            :only [$]]
        [domina           :only [append! destroy-children! detach! nodes
                                 single-node]]
        [one.sample.model :only [docs]])
  (:require [goog.events               :as event]
            [goog.editor.Field         :as editor-iframe]
            [goog.editor.SeamlessField :as editor-div]
            [one.dispatch              :as dispatch]
            [crate.core                :as crate]))

(def CHANGE goog.editor.Field.EventType.DELAYEDCHANGE)
(def CLICK  goog.events.EventType.CLICK)

(defn spawn-editor
  ([element] (spawn-editor element false))
  ([element iframe?]
     (if iframe?
       (goog.editor/Field. element)
       (goog.editor/SeamlessField. element))))

(declare list-documents)

(defn launch [element doc-session]
  (let [field     (spawn-editor element)
        content   (doc-session :get :content)
        new       ($ "button#new")
        undo      ($ "button#undo")
        redo      ($ "button#redo")
        set-html! (fn [content] (. field (setHtml false (or content "") true)))]
    (do
      (set-html! content)
      (. field (makeEditable))
      (. element (focus)))
    (event/listen field
                  CHANGE
                  (fn [e] (let [content (. field (getCleanContents))
                                previous (first (doc-session :get-history))]
                            (doc-session :set! :content content)
                            (if (not= content previous)
                              (do
                                (doc-session :conj-history! content)
                                (doc-session :reset-cursor!))))))
    (event/listen undo
                  CLICK
                  (fn [e] (let [content (doc-session :undo)] (set-html! content))))
    (event/listen redo
                  CLICK
                  (fn [e] (let [content (doc-session :redo)] (set-html! content))))
    (event/listen new
                  CLICK
                  (fn [e]
                    (append! ($ "#content") (detach! ($ "#workspace")))
                    (dispatch/fire :document-requested)))))

(defn add-documents-list-listeners []
  (let [items (nodes ($ "#sidebar-documents > ol > li"))]
    (doseq [item items]
      (event/listen ($ "a.bookmark" item)
                    CLICK
                    (fn [e] (. e (preventDefault))))
      (event/listen item
                    CLICK
                    (fn [e] (dispatch/fire :document-requested (.-id item)))))))

(defn strip-html [s]
  (-> s
      (.replace "<br>" " ")
      (.replace "&nbsp;" " ")
      (.replace (js/RegExp. "(<([^>]+)>)" "ig") "")))

(defn excerpt [s chars]
  (.substring (strip-html s) 0 chars))

(defn list-documents [documents]
  (let [element ($ "#sidebar-documents > ol")
        sorted  (reverse (sort-by :ts (vals documents)))]
    (destroy-children! element)
    (doseq [{id :id :as doc} sorted]
      (append! element (crate/html
                        [:li {:id (name id)}
                         [:div.excerpt
                          [:span (excerpt (:content doc) 20)]]
                         [:div.document-id
                          [:span
                           [:a.bookmark {:href (str \# (name id))} (name id)]]]])))
    (add-documents-list-listeners)))

(dispatch/react-to #{:documents-changed}
                   (fn [t d]
                     (list-documents d)))
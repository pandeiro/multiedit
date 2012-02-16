(ns one.sample.view.editor
  (:use [query            :only [$]]
        [domina           :only [append! destroy-children! detach! nodes
                                 single-node]]
        [one.sample.model :only [docs]])
  (:use-macros [crate.macros :only [defpartial]])
  (:require [goog.events               :as event]
            [goog.editor.Field         :as editor-iframe]
            [goog.editor.SeamlessField :as editor-div]
            [one.dispatch              :as dispatch]
            [crate.core                :as crate]))

(def CHANGE goog.editor.Field.EventType.DELAYEDCHANGE)
(def CLICK  goog.events.EventType.CLICK)

(defn- strip-html [s]
  (-> s
      (.replace "</div>" "</div> ")
      (.replace "<br>" "\n")
      (.replace "&nbsp;" " ")
      (.replace (js/RegExp. "(<([^>]+)>)" "ig") "")))

(defn- excerpt [s chars]
  (let [stripped (strip-html s)
        trimmed  (.trim stripped)
        length   (.-length trimmed)
        first-br (.indexOf trimmed "\n")
        substr   (.substring trimmed 0 chars)
        last-sp  (.lastIndexOf substr " ")]
    (if (empty? trimmed)
      "<Untitled>"
      (if (and (not= -1 first-br) (< first-br chars))
        (.substring trimmed 0 first-br)
        (if (> length chars)
          (if (not= -1 last-sp)
            (str (.substring substr 0 last-sp) "...")
            (str substr "..."))
          trimmed)))))

(defn- spawn-editor
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
        title     ($ "#star-title span#title")
        set-title! (fn [t]
                     (doc-session :set! :title))
        set-html! (fn [content] (.setHtml field false (or content "") true))]
    (do
      (set-html! content)
      (.makeEditable field)
      (.focus element))
    (event/listen field
                  CHANGE
                  (fn [e] (doc-session :set! :content (.getCleanContents field))))
    (event/listen new
                  CLICK
                  (fn [e]
                    (appe#nd! ($ "#content") (detach! ($ "#workspace")))
                    (dispatch/fire :document-requested)))
    (event/listen title
                  CLICK
                  (fn [e] (.setAttribute title "contenteditable" true)))))

(defn- add-documents-list-listeners []
  (let [items (nodes ($ "#sidebar-documents > ol > li"))]
    (doseq [item items]
      (event/listen ($ "a.bookmark" item)
                    CLICK
                    (fn [e] (.preventDefault e)))
      (event/listen item
                    CLICK
                    (fn [e]
                      (dispatch/fire :document-requested
                                     (.getAttribute item "docid")))))))

(defpartial document-list-item [{:keys [id content ts]}]
  [:li {:docid (name id)}
   [:div.excerpt
    [:span (excerpt content 20)]]
   [:div.document-id
    [:span [:a.bookmark {:href (str \# (name id))}
            (.toString (js/Date. ts))]]]])

(defn- append-list-items! [target items]
  (doseq [item items]
    (append! target (document-list-item item))))

(defn list-documents [documents]
  (let [ol    ($ "#sidebar-documents > ol")
        items (rest (reverse (sort-by :ts (vals documents))))]
    (destroy-children! ol)
    (append-list-items! ol items)
    (add-documents-list-listeners)))

(dispatch/react-to #{:documents-changed}
                   (fn [t d]
                     (list-documents d)))
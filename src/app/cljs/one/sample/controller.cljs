(ns ^{:doc "Respond to user actions by updating local and remote
  application state."}
  one.sample.controller
  (:use [one.sample.model :only (state docs uuid)])
  (:require [cljs.reader        :as reader]
            [goog.events        :as event]
            [one.dispatch       :as dispatch]
            [one.sample.history :as history]
            [local              :as local]))

(defmulti action :type)

(defn authenticate [callback]
  (callback (if (> 6 (rand 10)) "pablo" nil)))

(defn load-local-docs []
  (if-let [local-docs (local/get-clojure "docs")]
    (reset! docs local-docs)))

(defmethod action :init []
  (load-local-docs)
  (authenticate (fn [who]
                  (reset! state {:state :init :who who})
                  (let [token (history/get-token)]
                    (if (not (empty? token))
                      (get-document token)
                      (dispatch/fire :document-requested
                                     (if (not (empty? @docs))
                                       (->> (vals @docs)
                                            (sort-by :ts)
                                            reverse first :id))))))))

(defmethod action :workspace [{who :who}]
  (swap! state assoc :state :workspace :who who))

(defn get-document
  ([id] (get-document id #(dispatch/fire :document-retrieved %)))
  ([id callback]
     (callback (get @docs id {:id id :who (:who @state)}))))

(dispatch/react-to #{:document-requested}
                   (fn [t d]
                     (history/set-token (or d (uuid)))))

(dispatch/react-to #{:init :workspace}
                   (fn [t d] (action (assoc d :type t))))

(defn token-changed-to-id? [s]
  (.test (js/RegExp. "^[a-z0-9]{32}$") (name s)))

(dispatch/react-to token-changed-to-id?
                   (fn [t d] (get-document (name t))))

(event/listen js/window
              "storage"
              (fn [e]
                (dispatch/fire :storage-updated)))
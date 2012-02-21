(ns ^{:doc "Respond to user actions by updating local and remote
  application state."}
  one.sample.controller
  (:use [one.sample.model :only (state docs uuid)])
  (:require [cljs.reader        :as reader]
            [one.dispatch       :as dispatch]
            [one.sample.history :as history]
            [local              :as local]))

(defmulti action
  "Accepts a map containing information about an action to perform.

  Actions may cause state changes on the client or the server. This
  function dispatches on the value of the `:type` key and currently
  supports `:init`, `:form`, and `:greeting` actions.

  The `:init` action will initialize the appliation's state.

  The `:form` action will only update the status atom, setting its state
  to `:from`.

  The `:greeting` action will send the entered name to the server and
  update the state to `:greeting` while adding `:name` and `:exists`
  values to the application's state."
  :type)

(defn authenticate [callback]
  (callback (if (> 6 (rand 10)) "pablo" nil)))

(defn check-for-local-docs []
  (if-let [local-docs (local/get-clojure "docs")]
    (reset! docs local-docs)))

(defmethod action :init []
  (check-for-local-docs)
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

(defn token-changed-to-id?
  (.test (js/RegExp. "^[a-z0-9]{32}$") (name s)))

(dispatch/react-to token-changed-to-id?
                   (fn [t d] (get-document (name t))))
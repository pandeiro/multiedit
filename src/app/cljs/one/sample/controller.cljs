(ns ^{:doc "Respond to user actions by updating local and remote
  application state."}
  one.sample.controller
  (:use [one.sample.model :only (state docs)])
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

(defn check-local-storage []
  (let [local-docs (reader/read-string (local/get-item "docs"))]
    (reset! docs local-docs)))

(defmethod action :init []
  (reset! state {:state :init})
  (authenticate (fn [who]
                  (dispatch/fire :workspace {:who who}))))

(defmethod action :workspace [{who :who}]
  (swap! state assoc :state :workspace :who who))

(defn get-document [id callback]
  (callback (get @docs id)))

(dispatch/react-to #{:document-requested}
                   (fn [t d]
                     (get-document d #(dispatch/fire :document-retrieved %))))

(dispatch/react-to #{:init :workspace}
                   (fn [t d] (action (assoc d :type t))))


(ns ^{:doc "Respond to user actions by updating local and remote
  application state."}
  one.sample.controller
  (:use [one.sample.model :only (state)])
  (:require [one.dispatch       :as dispatch]
            [one.sample.history :as history]))

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

(defmethod action :init []
  (reset! state {:state :init})
  (authenticate (fn [who]
                  (dispatch/fire :workspace {:who who}))))

(defmethod action :workspace [{who :who}]
  (swap! state assoc :state :workspace :who who))

(dispatch/react-to #{:init :workspace}
                   (fn [t d] (action (assoc d :type t))))


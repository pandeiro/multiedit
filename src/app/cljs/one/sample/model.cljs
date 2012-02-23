(ns ^{:doc "Contains client-side state, validators for input fields
  and functions which react to changes made to the input fields."}
  one.sample.model
  (:require [one.dispatch :as dispatch]
            [local        :as local]))

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(def ^{:doc "An atom representing a collection of all open documents"}
  docs (atom {}))

(add-watch docs :documents-state-key
           (fn [k r o n]
             (dispatch/fire :documents-changed n)))

(dispatch/react-to #{:documents-changed}
                   (fn [_ d]
                     (local/set-item! "docs" d)))

(defn uuid []
  (let [chars "0123456789abcdef"
        random #(.floor js/Math (rand 16))]
    (apply str (repeatedly 32 #(get chars (random))))))

(defn now [] (.getTime (js/Date.)))

(defn session [{:keys [who id content title mode cursor born]}]
  (let [state   (atom {})
        watch   (add-watch state :document-state-key
                           (fn [k r o n]
                             (swap! docs assoc (:id n) n)))
        init    (swap! state assoc
                       :who who
                       :id (or id (uuid))
                       :content (or content "")
                       :born (or born (now))
                       :ts (now)
                       :title title
                       :mode (or mode "html")
                       :cursor (or cursor 0))]
    (fn document [command & args]
      (condp = command
        :set! (let [[k v] args]
                (swap! state assoc k v :ts (now))
                (local/conj-item! "activity" {:id (@state :id) :ts (now)}))
        :get  (let [[key] args]
                (@state key))))))


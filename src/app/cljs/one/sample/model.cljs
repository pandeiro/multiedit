(ns ^{:doc "Contains client-side state, validators for input fields
  and functions which react to changes made to the input fields."}
  one.sample.model
 (:require [one.dispatch              :as dispatch]
           [goog.editor.SeamlessField :as editor-div]
           [goog.editor.Field         :as editor-iframe]))
;; Note: ns aliases for goog.editor.Field and goog.editor.SeamlessField do not
;; work due to there being no goog/editor/editor.js file that provides goog.editor
;; in the Google Closure Library.

(def ^{:doc "An atom containing a map which is the application's current state."}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(def ^{:doc "An atom representing a collection of all open documents"}
  docs (atom {}))

(add-watch docs :documents-state-key
           (fn [k r o n]
             (.log js/console "docs watcher")
             (dispatch/fire :documents-changed n)))

(defn uuid []
  (let [chars "0123456789abcdef"
        random #(. js/Math (floor (rand 16)))]
    (apply str (repeatedly 32 #(get chars (random))))))

(defn document-session [& {:keys [who id content]}]
  (let [state   (atom {:who     who
                       :id      (or id (uuid))
                       :content content})
        history (atom '())
        cursor  (atom 0)
        watch   (add-watch state :document-state-key
                         (fn [k r o n]
                           (swap! docs assoc (keyword (:id n)) n)))
        now     #(. (js/Date.) (getTime))]
    (fn document [command & args]
      (condp = command
        :set!          (let [[k v] args]
                         (swap! state assoc k v :ts (now)))
        :get           (let [[key] args]
                         (@state key))
        :conj-history! (let [[content] args]
                         (swap! history conj content))
        :get-history   @history
        :reset-cursor! (reset! cursor 0)
        :undo          (let [snapshot (nth @history (inc @cursor) nil)]
                         (if (nil? snapshot)
                           (reset! cursor 0)
                           (do
                             (swap! cursor inc)
                             snapshot)))
        :redo          (let [snapshot (nth @history (dec @cursor))]
                         (swap! cursor dec)
                         snapshot)))))
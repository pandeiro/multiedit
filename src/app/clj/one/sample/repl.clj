(ns one.sample.repl
  "The starting namespace for the project. This is the namespace that
  users will land in when they start a Clojure REPL. It exists to
  provide convenience functions like 'go' and 'dev-server'."
  (:use [clojure.repl])
  (:require [one.tools :as tools]
            [one.sample.dev-server :as dev]
            [clojure.java.browse :as browse]))

(def ^:dynamic *server-instance* (atom nil))

(defn- run-server []
  (if (nil? @*server-instance*)
    (reset! *server-instance* (dev/run-server))
    (println "Server is already instantiated. Try (restart)")))

(defn go
  "Start a browser-connected REPL and launch a browser to talk to it."
  []
  (run-server)
  (tools/cljs-repl))

(defn dev-server
  "Start the development server and open the host application in the
  default browser."
  []
  (run-server))

;; This is a convenience function so that people can start a CLJS REPL
;; without having to type in (tools/cljs-repl)
(defn cljs-repl
  "Start a ClojureScript REPL."
  []
  (tools/cljs-repl))

;; These are convenience functions for stopping and restarting
(defn stop []
  (if (not (nil? @*server-instance*))
    (.stop @*server-instance*)))

(defn restart []
  (if (not (nil? @*server-instance*))
    (do
      (stop)
      (.start @*server-instance*))))

(println)
(println "Type (go) to launch the development server and setup a browser-connected REPL.")
(println "Type (dev-server) to launch only the development server.")
(println)
(println "Type (stop) or (restart) to stop or restart the server.")
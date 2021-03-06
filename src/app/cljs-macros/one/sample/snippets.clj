(ns one.sample.snippets
  "Macros for including HTML snippets in the ClojureScript application
  at compile time."
  (:use [one.templates :only (render construct-html)])
  (:require [net.cgrand.enlive-html :as html]))

(defn- snippet [file id]
  (render (html/select (construct-html (html/html-resource file)) id)))

(defmacro snippets
  "Expands to a map of HTML snippets which are extracted from the
  design templates."
  []
  {:login (snippet "login.html" [:div#login])
   :workspace (snippet "workspace.html" [:div#workspace])})

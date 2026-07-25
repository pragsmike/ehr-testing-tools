(ns ehr-testing-tools.core
  "Placeholder entry point. The internal corpus/judge organization is an open
  decision (docs/positioning.md, Open decisions) — this namespace exists to
  give the project a working src/test tree, not to anchor real capability
  code."
  (:require [clojure.edn :as edn]))

(defn- pinned-clojure-version
  "Reads deps.edn and returns the pinned org.clojure/clojure :mvn/version."
  []
  (-> (slurp "deps.edn")
      edn/read-string
      (get-in [:deps 'org.clojure/clojure :mvn/version])))

(defn repo-info
  "Identifies this repo and the Clojure version it's pinned to."
  []
  {:name "ehr-testing-tools"
   :clojure-version (pinned-clojure-version)})

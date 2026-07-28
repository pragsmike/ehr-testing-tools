(ns ehr-testing-tools.result
  "The result-not-throw doctrine's shared vocabulary (pattern nursery #11):
  every capability function returns one of :ok / :rejected / :error rather
  than throwing. :rejected is a legitimate, expected outcome (the check
  ran and the answer is no); :error is an operational failure (the check
  couldn't run at all). Exceptions remain reserved for programmer error."
  (:require [malli.core :as m]))

(def Result
  [:map
   [:status [:enum :ok :rejected :error]]
   [:category {:optional true} :keyword]
   [:payload :any]])

(defn ok
  "A successful result. No category -- there is nothing to classify."
  [payload]
  {:status :ok :payload payload})

(defn rejected
  "A legitimate, expected non-success: the check ran and rejected."
  [category payload]
  {:status :rejected :category category :payload payload})

(defn error
  "An operational failure: the check could not run at all."
  [category payload]
  {:status :error :category category :payload payload})

(defn ok?
  [r]
  (= :ok (:status r)))

(defn rejected?
  [r]
  (= :rejected (:status r)))

(defn error?
  [r]
  (= :error (:status r)))

(defn valid?
  "True if r conforms to the Result schema."
  [r]
  (m/validate Result r))

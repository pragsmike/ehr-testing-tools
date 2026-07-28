(ns ehrt.sim.result
  "The result-not-throw doctrine, copied (not depended upon) from
  ehr-testing-tools: every capability function returns one of
  :ok / :rejected / :error rather than throwing. :rejected is a
  legitimate, expected outcome (the check ran and the answer is no);
  :error is an operational failure (the check couldn't run at all).
  Exceptions remain reserved for programmer error.

  This is a deliberate ~30-line copy rather than a shared dependency:
  the dependency arrow between the repos must point tools -> sim only
  (ehr-testing-tools mounts sim as a subcommand; sim knows nothing of
  tools), and the Result maps are structurally typed, so a host built
  on tools' result ns consumes these maps without caring which
  namespace constructed them. If a third repo ever needs the doctrine,
  extract a shared microlib then -- not before. See ADR-0001."
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
  "An operational failure: the operation could not run at all."
  [category payload]
  {:status :error :category category :payload payload})

(defn ok? [r] (= :ok (:status r)))
(defn rejected? [r] (= :rejected (:status r)))
(defn error? [r] (= :error (:status r)))

(defn valid? [r] (m/validate Result r))

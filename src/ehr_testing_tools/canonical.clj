(ns ehr-testing-tools.canonical
  "Canonicalizer registry (pattern nursery #3): named, versioned
  transformations with three laws -- idempotence, explicit composition,
  and endomorphism (never cross-format). This namespace holds the pure
  registry and application API; the idempotence-law property harness
  lives on the test side (canonical_test.clj), generatively tested
  against each entry's own generator, so it can depend on test.check
  without pulling that dependency into the base build. Canonical forms
  are fixed points -- c@v2 need not agree with c@v1, they are simply two
  different registered entries."
  (:require [malli.core :as m]
            [ehr-testing-tools.result :as result]))

(def Entry
  [:map
   [:id :keyword]
   [:version :string]
   [:format :keyword]
   [:fn [:fn fn?]]
   [:docstring :string]
   [:generator {:optional true} :any]])

(defonce ^:private registry (atom {}))

(defn register!
  "Registers a canonicalizer entry, keyed by [id version]. Returns
  result/ok {:id :version} or result/rejected :invalid-entry."
  [entry]
  (if (m/validate Entry entry)
    (do (swap! registry assoc [(:id entry) (:version entry)] entry)
        (result/ok (select-keys entry [:id :version])))
    (result/rejected :invalid-entry {:entry entry})))

(defn lookup
  [id version]
  (get @registry [id version]))

(defn entries
  "All registered entries -- what the idempotence-property harness iterates."
  []
  (vals @registry))

(defn reset-registry!
  "Test/dev support: clears the registry."
  []
  (reset! registry {}))

(defn apply-canonicalizers
  "Applies `steps` (an ordered vector of [id version] pairs -- composition
  order is never implicit, so anything but a vector is rejected) to data,
  in that exact order. Returns result/ok {:data ... :applied [...]}, or
  result/rejected :unknown-canonicalizer on the first unregistered step,
  or result/rejected :unordered-steps if steps is not a vector."
  [data steps]
  (if-not (vector? steps)
    (result/rejected :unordered-steps {:steps steps})
    (reduce
     (fn [acc step]
       (if (result/ok? acc)
         (let [[id version] step
               entry (lookup id version)]
           (if entry
             (result/ok (-> (:payload acc)
                             (update :data (:fn entry))
                             (update :applied conj step)))
             (reduced (result/rejected :unknown-canonicalizer {:id id :version version}))))
         (reduced acc)))
     (result/ok {:data data :applied []})
     steps)))

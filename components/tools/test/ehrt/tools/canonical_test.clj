(ns ehrt.tools.canonical-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.tools.result :as result]
            [ehrt.tools.canonical :as canonical]))

;; Save/restore rather than wipe-and-leave-empty: other namespaces (e.g.
;; corpus.canonicalizers) register real entries at load time, once, into
;; this same shared registry. Wiping it to {} without restoring would
;; permanently erase those entries the moment this namespace's tests run
;; before the namespace that depends on them -- test order across
;; namespaces is not something to rely on for correctness.
(use-fixtures :each (fn [f]
                      (let [snapshot (canonical/registry-snapshot)]
                        (canonical/reset-registry!)
                        (f)
                        (canonical/reset-registry! snapshot))))

(deftest register-and-lookup-test
  (let [r (canonical/register! {:id :trim-trailing-ws :version "1" :format :text
                                 :fn #(str/trimr %)
                                 :docstring "Strips trailing whitespace."
                                 :generator gen/string-ascii})]
    (is (result/ok? r))
    (is (some? (canonical/lookup :trim-trailing-ws "1")))
    (is (nil? (canonical/lookup :nope "1")))))

(deftest register-rejects-invalid-entry-test
  (let [r (canonical/register! {:id :bad :version "1"})]
    (is (result/rejected? r))
    (is (= :invalid-entry (:category r)))))

(deftest apply-canonicalizers-applies-in-order-test
  (canonical/register! {:id :append-a :version "1" :format :text
                         :fn #(str % "a") :docstring "appends a" :generator gen/string-ascii})
  (canonical/register! {:id :append-b :version "1" :format :text
                         :fn #(str % "b") :docstring "appends b" :generator gen/string-ascii})
  (let [forward (canonical/apply-canonicalizers "x" [[:append-a "1"] [:append-b "1"]])
        backward (canonical/apply-canonicalizers "x" [[:append-b "1"] [:append-a "1"]])]
    (is (result/ok? forward))
    (is (= "xab" (:data (:payload forward))))
    (is (= "xba" (:data (:payload backward))))
    (is (= [[:append-a "1"] [:append-b "1"]] (:applied (:payload forward))))))

(deftest apply-canonicalizers-rejects-unknown-test
  (let [r (canonical/apply-canonicalizers "x" [[:no-such-thing "1"]])]
    (is (result/rejected? r))
    (is (= :unknown-canonicalizer (:category r)))))

(deftest apply-canonicalizers-rejects-unordered-steps-test
  (let [r (canonical/apply-canonicalizers "x" #{[:a "1"]})]
    (is (result/rejected? r))
    (is (= :unordered-steps (:category r)))))

;; The idempotence-law harness: generatively tests every registered
;; canonicalizer against its own generator. Future entries inherit this
;; test automatically -- registering a canonicalizer with a :generator
;; is what wires it in; nothing here needs to change when one is added.
(defn- idempotent-property
  [{:keys [fn generator]}]
  (prop/for-all [x generator]
    (= (fn (fn x)) (fn x))))

(deftest idempotence-law-holds-for-well-behaved-canonicalizer-test
  (canonical/register! {:id :trim :version "1" :format :text
                         :fn str/trim :docstring "trims"
                         :generator gen/string-ascii})
  (doseq [entry (filter :generator (canonical/entries))]
    (let [check-result (tc/quick-check 100 (idempotent-property entry))]
      (is (:pass? check-result)
          (str "idempotence failed for " (:id entry) "@" (:version entry))))))

(deftest idempotence-law-catches-a-non-idempotent-canonicalizer-test
  ;; Proves the harness actually catches violations, not just rubber-stamps.
  (canonical/register! {:id :append-counter :version "1" :format :text
                         :fn (let [n (atom 0)] (fn [s] (str s (swap! n inc))))
                         :docstring "deliberately non-idempotent"
                         :generator gen/string-ascii})
  (let [entry (canonical/lookup :append-counter "1")
        check-result (tc/quick-check 20 (idempotent-property entry))]
    (is (false? (:pass? check-result)))))

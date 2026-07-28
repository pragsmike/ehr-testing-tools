(ns ehr-testing-tools.cli.help-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [ehr-testing-tools.cli.help :as help]
            [ehr-testing-tools.cli :as cli]
            [ehr-testing-tools.result :as result]))

(deftest render-top-level-lists-every-group-test
  (let [text (help/render-top-level help/cli-spec)]
    (doseq [g (help/group-names help/cli-spec)]
      (is (str/includes? text g)))
    (is (str/includes? text "--json"))
    (is (str/includes? text "Exit codes"))))

(deftest render-top-level-lists-exit-codes-0-1-2-3-test
  (let [text (help/render-top-level help/cli-spec)]
    (doseq [code [0 1 2 3]]
      (is (str/includes? text (str code "  "))))))

(deftest render-group-known-group-lists-its-verbs-test
  (let [text (help/render-group help/cli-spec "corpus")]
    (is (str/includes? text "generate"))
    (is (str/includes? text "mutate"))
    (is (str/includes? text "intake"))
    (is (str/includes? text "--seed"))))

(deftest render-group-unknown-group-returns-nil-test
  (is (nil? (help/render-group help/cli-spec "bogus"))))

(deftest render-group-check-has-no-sub-verbs-but-lists-its-own-flags-test
  (let [text (help/render-group help/cli-spec "check")]
    (is (str/includes? text "--expected"))
    (is (str/includes? text "DIR"))))

(deftest render-group-gate-documents-positional-path-convention-test
  (let [text (help/render-group help/cli-spec "gate")]
    (is (str/includes? text "PATH"))
    (is (str/includes? text "v2"))
    (is (str/includes? text "fhir"))
    (is (str/includes? text "--treat-no-verdict-as"))))

;; ---- coverage: every [group verb] the spec declares must actually
;; route through dispatch (not :unknown-command), and every pair
;; dispatch itself handles must appear in the spec -- a verb added to
;; either side without the other is a real gap this test surfaces. ----

(defn- stub-key
  [group verb]
  (case [group verb]
    ["artifact" "fetch"] :fetch-fn
    ["artifact" "resolve"] :resolve-fn
    ["corpus" "generate"] :generate-fn
    ["corpus" "mutate"] :mutate-fn
    ["corpus" "intake"] :intake-fn
    ["corpus" "operators"] :operators-fn
    ["gate" "v2"] :gate-v2-fn
    ["gate" "fhir"] :gate-fhir-fn
    ["check" nil] :check-fn
    ["version" nil] :version-fn
    ["doctor" nil] :doctor-fn))

(deftest every-spec-command-pair-actually-routes-in-dispatch-test
  (doseq [[group verb] (help/command-pairs help/cli-spec)]
    (let [called (atom false)
          stub (fn [_opts] (reset! called true) (result/ok {}))
          args (if verb [group verb] [group])
          r (cli/dispatch args {} {(stub-key group verb) stub})]
      (is @called (str group " " verb " did not route to its stub"))
      (is (not (= :unknown-command (:category r)))))))

(def ^:private known-dispatch-pairs
  "Mirrors dispatch's own group/verb `case` branches (cli.clj), read
  from source this session -- the coverage test's other half: every
  pair dispatch actually handles must appear in the spec too, so a
  verb added to dispatch's case without a matching spec entry (and an
  update here) is a discoverable gap rather than a silent one."
  #{["artifact" "fetch"] ["artifact" "resolve"]
    ["corpus" "generate"] ["corpus" "mutate"] ["corpus" "intake"] ["corpus" "operators"]
    ["gate" "v2"] ["gate" "fhir"]
    ["check" nil]
    ["version" nil]
    ["doctor" nil]})

(deftest spec-command-pairs-match-dispatchs-known-routes-test
  (is (= known-dispatch-pairs (set (help/command-pairs help/cli-spec)))))

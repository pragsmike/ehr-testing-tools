(ns ehrt.cli.help-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [ehrt.cli.help :as help]
            [ehrt.cli.core :as cli]
            [ehrt.docs-tooling.interface :as docs-tooling]
            [ehrt.kernel.interface :as result]))

(deftest render-top-level-lists-every-group-test
  (let [text (help/render-top-level help/cli-spec)]
    (doseq [g (help/group-names help/cli-spec)]
      (is (str/includes? text g)))
    (is (str/includes? text "--json"))
    (is (str/includes? text "Exit codes"))))

(deftest render-top-level-lists-pretty-and-edn-flags-test
  ;; ADR-0013: the TTY-default rendering's forcing flags are documented
  ;; alongside --json, not a hidden feature.
  (let [text (help/render-top-level help/cli-spec)]
    (is (str/includes? text "--pretty"))
    (is (str/includes? text "--edn"))))

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
    ;; ADR-0015 amendment (2026-07-30): bare `corpus generate` routes
    ;; to the sim lane now, not synthea -- see notes/ADRs.md ADR-0015's
    ;; own amendment paragraph.
    ["corpus" "generate"] :generate-sim-fn
    ["corpus" "mutate"] :mutate-fn
    ["corpus" "intake"] :intake-fn
    ["corpus" "operators"] :operators-fn
    ["gate" "v2"] :gate-v2-fn
    ["gate" "fhir"] :gate-fhir-fn
    ["gate" "v2-nist"] :gate-v2-nist-fn
    ["check" nil] :check-fn
    ["version" nil] :version-fn
    ["doctor" nil] :doctor-fn
    ["sim" "run"] :sim-run-fn
    ["sim" "check"] :sim-check-fn
    ["sim" "identifiers"] :sim-identifiers-fn
    ["sim" "version"] :sim-version-fn
    ["show" nil] :show-fn
    ["play" nil] :play-fn))

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
    ["gate" "v2"] ["gate" "fhir"] ["gate" "v2-nist"]
    ["check" nil]
    ["version" nil]
    ["doctor" nil]
    ["sim" "run"] ["sim" "check"] ["sim" "identifiers"] ["sim" "version"]
    ["show" nil]
    ["play" nil]})

(deftest spec-command-pairs-match-dispatchs-known-routes-test
  (is (= known-dispatch-pairs (set (help/command-pairs help/cli-spec)))))

;; ---- staleness guard, local half (ci current, AR-CI-2) ----
;;
;; The genuinely live comparison -- this base is the only brick that
;; can see both `cli-spec` (here) and the renderer (docs-tooling, a
;; component this base already depends on) without inverting
;; Polylith's base -> component direction; components/docs-tooling's
;; own docsgen-test can only exercise the renderer against a
;; representative fixture spec, never this one. Goes through
;; docs-tooling's own write-cli-md! (the one interface export,
;; ehrt.docs-tooling.interface) rather than reaching into its internal
;; render-cli-md, so this test respects the same interface boundary
;; ehrt.cli.help/write-cli-md! itself does.
(deftest cli-md-is-current-test
  (let [tmp (java.io.File/createTempFile "cli-md-current-test" ".md")]
    (try
      (docs-tooling/write-cli-md! {:out (.getPath tmp) :spec help/cli-spec})
      (is (= (slurp tmp) (slurp "docs/cli.md"))
          "docs/cli.md is stale -- run `make cli-doc` (or `make docsgen`) to regenerate")
      (finally (.delete tmp)))))

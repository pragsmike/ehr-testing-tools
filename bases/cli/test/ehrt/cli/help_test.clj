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

;; F8 (R3-B1-4, RULED ADR-0115 RQ2, + the ADR-0116-inherited seed-row
;; wording, ADR-0117): `corpus generate`'s --seed doc string states the
;; two-tier design explicitly -- an ergonomic, defaulted front door
;; here, versus sim run/sim identifiers's own required-explicitly
;; tier -- rather than leaving the split silently undocumented.
(deftest corpus-generate-seed-doc-states-the-ergonomic-front-door-tiering-test
  (let [seed-flag (->> (help/find-group help/cli-spec "corpus")
                       :verbs (filter #(= "generate" (:verb %))) first
                       :flags (filter #(= "--seed" (:flag %))) first)]
    (is (= "patient/master-generation seed (integer; non-negative when --source sim), shared by both sources; defaulted here as the ergonomic front door -- the sim-tier verbs (sim run, sim identifiers) require a seed explicitly"
           (:doc seed-flag)))))

(deftest render-group-unknown-group-returns-nil-test
  (is (nil? (help/render-group help/cli-spec "bogus"))))

;; ---- B1 (R3-B3-2, ADR-0118): verb-level help narrowing -- just that
;; verb's own description and flags, not the whole group screen every
;; invocation form used to fall back to. ----

(deftest render-verb-help-known-group-and-verb-renders-just-that-verbs-own-content-test
  (let [text (help/render-verb-help help/cli-spec "sim" "run")]
    (is (str/includes? text "ehrt sim run"))
    (is (str/includes? text "--seed"))
    (is (str/includes? text "--patients"))
    (is (str/includes? text "Exit codes"))
    ;; sibling verbs' own content must NOT leak into the narrowed render
    ;; -- the whole point B1 exists to fix. Checked by each sibling's
    ;; own section header shape (render-verb's "ehrt sim <verb>\n"),
    ;; not a bare substring: `--format`'s own doc string legitimately
    ;; cites "`ehrt sim check`" in prose, a false-positive trap for a
    ;; cruder check.
    (is (not (str/includes? text "\nehrt sim check\n")))
    (is (not (str/includes? text "\nehrt sim identifiers\n")))
    (is (not (str/includes? text "\nehrt sim version\n")))))

(deftest render-verb-help-unknown-verb-in-a-known-group-returns-nil-test
  (is (nil? (help/render-verb-help help/cli-spec "sim" "frobnicate"))))

(deftest render-verb-help-unknown-group-returns-nil-test
  (is (nil? (help/render-verb-help help/cli-spec "bogus" "run"))))

(deftest render-verb-help-never-shows-the-groups-own-example-line-test
  ;; the narrowed verb screen is explicitly NOT "the whole group
  ;; screen" -- the Example: line (B2, below) belongs to the group
  ;; screen only.
  (is (not (str/includes? (help/render-verb-help help/cli-spec "sim" "run") "Example:"))))

;; ---- B2 (R3-B3-1, ADR-0118): one sourced "Example:" line per group
;; with a witnessed invocation anywhere in README.md's Quickstart,
;; docs/use-cases/*.md, or a demo README -- never a composed one. ----

(def ^:private groups-with-a-witnessed-example
  "Every group with a real, sourced invocation found this session --
  cited per line in notes/adr/0118-*.md. version/doctor are
  deliberately excluded: no witnessed invocation of either exists
  anywhere in the three source classes B2 draws from (checked this
  session) -- see render-group-omits-example-for-groups-with-no-
  witnessed-invocation-test, below."
  #{"artifact" "corpus" "gate" "check" "sim" "show" "play"})

(deftest render-group-shows-a-sourced-example-line-for-every-covered-group-test
  (doseq [g groups-with-a-witnessed-example]
    (is (str/includes? (help/render-group help/cli-spec g) "Example:")
        (str g " is missing its Example: line"))))

(deftest render-group-omits-example-for-groups-with-no-witnessed-invocation-test
  (doseq [g ["version" "doctor"]]
    (is (not (str/includes? (help/render-group help/cli-spec g) "Example:"))
        (str g " should have no Example: line -- no witnessed invocation exists, and B2's own rule is never to invent one"))))

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
    ["corpus" "batch"] :batch-fn
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
    ["corpus" "generate"] ["corpus" "mutate"] ["corpus" "intake"] ["corpus" "operators"] ["corpus" "batch"]
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

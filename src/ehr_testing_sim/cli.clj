(ns ehr-testing-sim.cli
  "The `sim` entrypoint AND the mountable command group -- the only
  namespace here that prints. Mirrors ehr-testing-tools' cli.clj shell
  (ADR-0004 there): parse, dispatch to a capability function, render
  EDN (with --json projection), map Result to exit code 0/1/2.

  The embedding contract (ADR-0001 here) is three public values:

    cli-spec        babashka.cli coercions, mergeable into a host's spec
    help-group      one entry in the shape ehr-testing-tools'
                    cli.help/cli-spec :groups vector expects
    dispatch-action (fn [action opts] ...) -> Result, covering the verbs

  A host mounts the whole group with one dispatch arm:
      \"sim\" (sim-cli/dispatch-action action opts)
  plus (merge host-spec sim-cli/cli-spec) and appending help-group to
  its help data. The standalone `sim` binary (main! / -main below) is a
  thin wrapper over the very same dispatch-action, so standalone and
  embedded behavior cannot drift."
  (:require [babashka.cli :as cli]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [ehr-testing-sim.result :as result]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.identifiers :as identifiers]
            [ehr-testing-sim.run :as run]
            [ehr-testing-sim.version :as version])
  (:gen-class))

(def cli-spec
  {:seed {:coerce :long}
   :patients {:coerce :long}
   :arrival-gap {:coerce :long}
   :emit {:coerce :string}
   :at {:coerce :long}
   :reference-date {:coerce :string}
   :utc-offset {:coerce :string}
   :warm-up-seconds {:coerce :long}
   :churn {:coerce :boolean}
   :config {:coerce :string}
   :format {:coerce :string}
   :json {:coerce :boolean}
   :version {:coerce :boolean}
   :help {:coerce :boolean}})

(def help-group
  "This group's help metadata, in the shape ehr-testing-tools'
  cli.help/cli-spec uses for its :groups entries, so `ehr help sim`
  renders through the host's existing help machinery unchanged."
  {:group "sim"
   :doc "Generate synthetic hospital traffic (deterministic, seeded). See docs/problem-statement.md."
   :verbs
   [{:verb "run"
     :doc "Run a simulation: config + seed -> ground-truth event log (+ manifest)."
     :flags [{:flag "--seed" :doc "RNG seed (required; same config+seed => identical output)"}
             {:flag "--patients" :doc "number of patients" :default "1"}
             {:flag "--arrival-gap" :doc "max minutes between arrivals" :default "60"}
             {:flag "--emit" :doc "render output into the payload (\"hl7\" for ADT/ORM/ORU messages, \"fhir\" for a Bundle per patient)"}
             {:flag "--at" :doc "with --emit fhir: seconds from run start to snapshot (default: end of run)"}
             {:flag "--reference-date" :doc "ISO date anchoring HL7 timestamps (pinned input)" :default "2024-01-01"}
             {:flag "--utc-offset" :doc "fixed ISO offset suffixed onto HL7 timestamps (pinned input, no DST)" :default "+00:00"}
             {:flag "--warm-up-seconds" :doc "events before this mark :warm-up true (log stays complete)" :default "0"}
             {:flag "--churn" :doc "activate InjectChurn with a modest sample profile (cancel-*/transfer-in-error/bed-swap/merge)"}
             {:flag "--config" :doc "path to an EDN file supplying data-heavy engine keys with no flag of their own (:pathway/:pathways/:order-profiles/:churn-profile); merged UNDER explicit flags"}
             {:flag "--format" :doc "output rendering: edn (default), json, er7 (bare wire messages to stdout, nothing else; requires --emit hl7), or ground-truth (bare EDN vector of the ground-truth log, nothing else -- pipe straight into `sim check`). --json is a deprecated alias for --format json"}]}
    {:verb "check"
     :doc "Run the invariant catalog over a ground-truth log (EDN on stdin)."
     :flags []}
    {:verb "identifiers"
     :doc "Config + seed -> the complete EDN inventory of every identifier this run's output contains (patient-ids, MRNs, visit beds, HL7 control ids, FHIR resource ids, provider NPIs, run-id) -- ADR-0014's own answer to 'how would we find and remove it.'"
     :flags [{:flag "--seed" :doc "RNG seed (required; same as `sim run`'s own --seed)"}
             {:flag "--patients" :doc "number of patients" :default "1"}
             {:flag "--config" :doc "path to an EDN file supplying data-heavy engine keys (same as `sim run`)"}]}
    {:verb "version"
     :doc "Print this library's version, and git SHA when the repo is present. Same source the run manifest's :generator block stamps -- see also the global --version shortcut."
     :flags []}]})

(defn parse
  [raw-args]
  (cli/parse-args raw-args {:spec cli-spec}))

(defn- check-command
  "Reads a ground-truth log as EDN from *in* and runs the catalog."
  [_opts]
  (let [log (try (edn/read {:eof ::eof} (java.io.PushbackReader. *in*))
                 (catch Exception _e ::unreadable))]
    (cond
      (= ::eof log) (result/error :empty-input {:message "expected a ground-truth EDN vector on stdin"})
      (= ::unreadable log) (result/error :unreadable-input {:message "stdin was not readable EDN"})
      (not (vector? log)) (result/error :malformed-input {:message "expected a vector of event maps"})
      :else (check/check-all log))))

(defn- unknown-action
  [action]
  (result/error :unknown-command
                {:message (str "unknown sim action: " (pr-str action))
                 :known (mapv :verb (:verbs help-group))}))

(defn- version-command
  "sim version: this library's version (ehr-testing-sim.version/version,
  the SAME source the run manifest's :generator block stamps) plus the
  git SHA when a readable `.git` is present -- nil otherwise, never an
  error (`ehr-testing-sim.version/git-sha`'s own never-throwing
  contract)."
  [_opts]
  (result/ok {:version version/version :git-sha (version/git-sha)}))

(defn dispatch-action
  "The mountable dispatch: [action opts] -> Result. The -fn keys are
  injectable for tests (same pattern as tools' dispatch)."
  ([action opts] (dispatch-action action opts {}))
  ([action opts {:keys [run-fn check-fn identifiers-fn version-fn]
                 :or {run-fn run/run-command
                      check-fn check-command
                      identifiers-fn identifiers/identifiers-command
                      version-fn version-command}}]
   (case action
     "run" (run-fn opts)
     "check" (check-fn opts)
     "identifiers" (identifiers-fn opts)
     "version" (version-fn opts)
     (unknown-action action))))

;; --- standalone shell -------------------------------------------------

(defn result->exit-code
  "0 = ran and passed; 1 = ran and legitimately rejected; 2 =
  operational error. Same contract as ehr-testing-tools' ADR-0004
  mapping (the no-verdict arm doesn't apply here yet)."
  [r]
  (cond
    (result/ok? r) 0
    (result/rejected? r) 1
    :else 2))

(defn render
  [r json?]
  (if json? (json/write-str r) (pr-str r)))

(defn resolve-format
  "--format wins outright; --json is a DEPRECATED alias for `--format
  json`, honored only when --format itself is absent; edn is the
  default. Not part of the embedding contract -- rendering (this
  function, render, help-text, main!) is the standalone shell's own
  business; a host does its own rendering over dispatch-action's
  Result."
  [opts]
  (or (:format opts) (when (:json opts) "json") "edn"))

(defn- er7-requires-emit-hl7?
  [format opts]
  (and (= "er7" format) (not= "hl7" (:emit opts))))

(defn- er7-stdout
  "Bare wire bytes: every rendered message, joined by one blank line,
  nothing else -- the property `--format er7` promises. Only called
  once the Result is known :ok (`er7-requires-emit-hl7?` already
  covers the one way it wouldn't have :messages to render)."
  [r]
  (string/join "\n\n" (get-in r [:payload :messages])))

(defn- ground-truth-requires-run?
  "--format ground-truth only means something for the run verb (only
  `run-command`'s own payload ever carries a :ground-truth key) --
  same pre-dispatch-gate shape `er7-requires-emit-hl7?` already
  established, so an invalid combination never even runs a
  simulation."
  [format action]
  (and (= "ground-truth" format) (not= "run" action)))

(defn- ground-truth-stdout
  "Bare EDN: the run's own :ground-truth vector, pr-str'd, nothing
  else -- readable straight back by `edn/read`, exactly the shape
  `check-command` requires on stdin. This is what makes `sim run
  --format ground-truth | sim check` actually work, unlike piping any
  other format (every other format's stdout is a wrapped Result map,
  never a bare vector)."
  [r]
  (pr-str (get-in r [:payload :ground-truth])))

(def ^:private bare-formats
  "Formats whose successful (:ok) render is bare, non-wrapped content
  to stdout -- no :status/:payload envelope, no manifest, no summary.
  A non-:ok Result under a bare format still renders as EDN, but to
  stderr, never stdout: stdout stays reserved for the bare content,
  and a failed run stays diagnosable and scriptable regardless of
  format."
  {"er7" er7-stdout
   "ground-truth" ground-truth-stdout})

(defn- help-text []
  (str "sim -- synthetic hospital traffic generator\n\n"
       (:doc help-group) "\n\n"
       (apply str
              (for [{:keys [verb doc flags]} (:verbs help-group)]
                (str "  sim " verb "  " doc "\n"
                     (apply str (for [{:keys [flag doc default]} flags]
                                  (str "      " flag "  " doc
                                       (when default (str " (default " default ")")) "\n"))))))
       "\nGlobal: --format edn|json|er7 (default edn), --json (deprecated alias for --format json), --version, --help\n"
       "Exit codes: 0 ok, 1 rejected, 2 operational error\n"))

(defn- version-text
  "The --version shortcut's own plain-text rendering -- always raw
  text, like --help's, unaffected by --format (there is no Result to
  render, so there's nothing for --format to apply to)."
  []
  (let [{:keys [version git-sha]} (:payload (version-command {}))]
    (str "sim " version (when git-sha (str " (" git-sha ")")) "\n")))

(defn- bare-format-gate
  "Pre-dispatch validity gates for the bare formats -- checked BEFORE
  dispatch-action runs, so an invalid combination never even executes
  a simulation. Returns a :rejected Result for an invalid combination,
  else nil (meaning: proceed to dispatch-action as normal)."
  [format action opts]
  (cond
    (er7-requires-emit-hl7? format opts)
    (result/rejected :format-er7-requires-emit-hl7
                      {:message "--format er7 renders bare wire messages and requires --emit hl7"
                       :format format :emit (:emit opts)})

    (ground-truth-requires-run? format action)
    (result/rejected :format-ground-truth-requires-run
                      {:message "--format ground-truth renders the run verb's own ground-truth log and requires the run verb"
                       :format format :action action})))

(defn main!
  "Testable main: parse, dispatch, render, exit-code. Injectable
  println/exit for tests, same pattern as tools' main!.

  The bare formats (`bare-formats` -- er7, ground-truth) render bare,
  non-wrapped content to stdout on :ok (nothing else -- no manifest,
  no summary) and each has its own precondition (er7 needs --emit
  hl7; ground-truth needs the run verb): unmet, the Result itself
  becomes a structured :rejected (exit 1) rather than a silent edn
  dump, computed BEFORE dispatch-action runs (`bare-format-gate`).
  Any non-:ok Result under a bare format (that gate, or a genuine
  rejection/error from the run itself) renders as EDN to stderr, never
  stdout -- stdout stays reserved for the bare content, and a failed
  run stays diagnosable and scriptable: the exit-code contract (0/1/2)
  is unchanged across every format. `--format ground-truth` is what
  makes `sim run --format ground-truth | sim check` an actual working
  pipe -- every other format's stdout is a wrapped Result map, which
  `check-command` (needing a bare vector) always rejected."
  ([raw-args] (main! raw-args {}))
  ([raw-args {:keys [println-fn err-println-fn exit-fn]
              :or {println-fn println
                   err-println-fn #(binding [*out* *err*] (println %))
                   exit-fn (fn [code] (System/exit code))}}]
   (let [{:keys [args opts]} (parse raw-args)
         [action] args
         format (resolve-format opts)]
     (cond
       (:version opts)
       (do (println-fn (version-text)) (exit-fn 0))

       (or (:help opts) (nil? action) (= "help" action))
       (do (println-fn (help-text)) (exit-fn 0))

       :else
       (let [r (or (bare-format-gate format action opts)
                    (dispatch-action action opts))]
         (if-let [bare-render (get bare-formats format)]
           (if (result/ok? r)
             (println-fn (bare-render r))
             (err-println-fn (pr-str r)))
           (println-fn (render r (= "json" format))))
         (exit-fn (result->exit-code r)))))))

(defn -main
  [& raw-args]
  (main! raw-args))

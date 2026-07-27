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
            [ehr-testing-sim.result :as result]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.run :as run])
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
   :json {:coerce :boolean}
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
             {:flag "--config" :doc "path to an EDN file supplying data-heavy engine keys with no flag of their own (:pathway/:pathways/:order-profiles/:churn-profile); merged UNDER explicit flags"}]}
    {:verb "check"
     :doc "Run the invariant catalog over a ground-truth log (EDN on stdin)."
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

(defn dispatch-action
  "The mountable dispatch: [action opts] -> Result. The -fn keys are
  injectable for tests (same pattern as tools' dispatch)."
  ([action opts] (dispatch-action action opts {}))
  ([action opts {:keys [run-fn check-fn]
                 :or {run-fn run/run-command
                      check-fn check-command}}]
   (case action
     "run" (run-fn opts)
     "check" (check-fn opts)
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

(defn- help-text []
  (str "sim -- synthetic hospital traffic generator\n\n"
       (:doc help-group) "\n\n"
       (apply str
              (for [{:keys [verb doc flags]} (:verbs help-group)]
                (str "  sim " verb "  " doc "\n"
                     (apply str (for [{:keys [flag doc default]} flags]
                                  (str "      " flag "  " doc
                                       (when default (str " (default " default ")")) "\n"))))))
       "\nGlobal: --json (project EDN result to JSON), --help\n"
       "Exit codes: 0 ok, 1 rejected, 2 operational error\n"))

(defn main!
  "Testable main: parse, dispatch, print, exit-code. Injectable
  println/exit for tests, same pattern as tools' main!."
  ([raw-args] (main! raw-args {}))
  ([raw-args {:keys [println-fn exit-fn]
              :or {println-fn println exit-fn (fn [code] (System/exit code))}}]
   (let [{:keys [args opts]} (parse raw-args)
         [action] args]
     (if (or (:help opts) (nil? action) (= "help" action))
       (do (println-fn (help-text)) (exit-fn 0))
       (let [r (dispatch-action action opts)]
         (println-fn (render r (:json opts)))
         (exit-fn (result->exit-code r)))))))

(defn -main
  [& raw-args]
  (main! raw-args))

(ns ehrt.cli.help
  "The `ehrt help` surface's data (DOC-1): one spec structure describing
  every group/verb, its flags, its positional-argument convention, and
  the shared exit-code table -- rendered to plain text by the pure
  functions below. The spec does not drive argument parsing (`cli/parse`
  is untouched this session); it exists so the help text and a coverage
  test can both walk the same data instead of drifting independently.
  `cli-spec` is this session's ground truth for the CLI surface,
  enumerated from source in DOC-1 Step 0 (see that session's commit
  history for the read-from-source inventory this spec is built from)."
  (:require [clojure.string :as str]
            [ehrt.tools.interface :as tools]))

(def exit-codes
  "0/1/2/3 per ADR-0004's ok/rejected/error mapping, extended by
  ADR-0010 for the no-verdict arm -- see `cli/result->exit-code`'s own
  docstring for the authoritative reasoning; this table only cites it."
  [{:code 0 :meaning "ran and passed"}
   {:code 1 :meaning "ran and legitimately rejected"}
   {:code 2 :meaning "operational error (bad invocation, missing artifact, subprocess failure, etc.)"}
   {:code 3 :meaning "a gate's aggregate contains :no-verdict under the default --treat-no-verdict-as policy (ADR-0010)"}])

(def global-flags
  [{:flag "--json" :doc "project the EDN result to JSON (EDN remains canonical)"}
   {:flag "--pretty" :doc "force a human-readable summary, even when stdout is piped -- the default at a real terminal already; ADR-0013"}
   {:flag "--edn" :doc "force the raw EDN envelope, even at a terminal -- the default when stdout is piped or redirected already; ADR-0013"}
   {:flag "--help" :doc "print this command's usage and exit 0 without running it"}])

(def ^:private artifact-flags
  [{:flag "--name" :doc "artifact name, e.g. \"synthea\""}
   {:flag "--version" :doc "artifact version, e.g. \"4.0.0\""}
   {:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}])

(def ^:private gate-common-flags
  [{:flag "--path" :doc "alternative to the positional PATH"}
   {:flag "--report" :doc "write the report EDN to this path"}
   {:flag "--baseline" :doc "baseline-relative mode: path to a previous --report EDN; only genuinely new findings count"}
   {:flag "--treat-no-verdict-as" :doc "\"pass\" or \"rejected\" -- folds :no-verdict into an existing polarity (ADR-0010)"}])

(def top-level-doc
  "Surfaced by `ehrt help`/bare `ehrt`, above the group list (ADR-0013):
  --json and `show` are easy to miss otherwise -- every command has
  taken --json since D13, and `show` is new."
  "Every command accepts --json (EDN is canonical, --json a projection); `ehrt show FILE` renders a v2/FHIR file for a human. See docs/formats.md.")

(def cli-spec
  {:program "ehrt"
   :doc top-level-doc
   :exit-codes exit-codes
   :global-flags global-flags
   :groups
   [{:group "artifact"
     :doc "Fetch and resolve locked external engine/tool artifacts (ADR-0005)."
     :verbs
     [{:verb "fetch" :doc "Fetch a locked artifact into the local content-addressed cache."
       :flags (into artifact-flags
                    [{:flag "--all" :doc "fetch every artifact the lockfile names (D13); collapses SETUP.md's multi-fetch walkthrough into one command -- --name/--version are ignored when given. One failing artifact does not abort the rest; the aggregate result is the worst-of every individual outcome." :default "false"}])}
      {:verb "resolve" :doc "Resolve an already-fetched artifact to a filesystem path."
       :flags artifact-flags}]}

    {:group "corpus"
     :doc "Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out below may also be spelled as a dir:/file: URL designator (ruling 7, docs/source-sink-design.md D4) instead of a bare path -- bare paths remain the documented, common spelling. `corpus generate sim`/`corpus generate synthea` (ADR-0015) is the front door for generating a corpus; `corpus intake` additionally accepts a generator URL (sim:/synthea:) in place of PATH as its own compose form -- generate, then catalog, in one command (SS-2) -- or a stdin designator (stdin:?format=...&framing=...) -- read piped bytes, spool, then catalog, in one command (SS-3)."
     :verbs
     [{:verb "generate" :doc "Generate a deterministic synthetic corpus. Grows a source subcommand (ADR-0015): `corpus generate synthea` (Synthea, the flags below) or `corpus generate sim` (this workspace's own sim engine, ehrt.sim -- --patients/--churn/--emit/--config below); bare `corpus generate`, with no subcommand, stays exactly `generate synthea` for compatibility with every existing doc and script. Zero-flag defaults (D9, ADR-0019) make either source's bare command a byte-reproducible run -- re-running it into the same (derived) --out-dir without clearing it first is rejected (:out-dir-exists), not silently overwritten."
       :flags [{:flag "--config-path" :doc "synthea: Synthea properties file" :default "resources/synthea-default.properties"}
               {:flag "--seed" :doc "patient/master-generation seed (integer), shared by both sources" :default "1"}
               {:flag "--clinician-seed" :doc "synthea: clinician-generation seed (integer) -- Synthea defaults this to wall-clock time otherwise, which breaks reproducibility even with --seed pinned" :default "the resolved --seed value"}
               {:flag "--population" :doc "synthea: population size (integer)" :default "5"}
               {:flag "--reference-date" :doc "generation reference date, shared by both sources -- YYYYMMDD for synthea; Synthea otherwise generates relative to wall-clock \"now\"" :default "20260101"}
               {:flag "--out-dir" :doc "output directory for the corpus + manifest.edn -- rejected if it already exists and is non-empty" :default "out/corpus/synthea-s<seed>-p<population> for synthea, out/corpus/sim-s<seed>-p<patients> for sim"}
               {:flag "--locale" :doc "synthea: BCP47-ish locale" :default "en-US"}
               {:flag "--timezone" :doc "synthea: timezone" :default "UTC"}
               {:flag "--java-bin" :doc "synthea: java executable to invoke" :default "resolved via the artifact registry"}
               ;; D10 (ADR-0019): --lockfile-path renamed to --lockfile,
               ;; one spelling across every verb that takes a lockfile
               ;; (artifact fetch/resolve, gate fhir, corpus generate) --
               ;; the option key renamed to match (generate! is called
               ;; directly from dispatch with no per-verb translation
               ;; layer, so the CLI spelling and the function's own
               ;; parameter name are the same thing here).
               {:flag "--lockfile" :doc "synthea: path to the lockfile" :default "artifacts.lock.edn"}
               {:flag "--patients" :doc "sim: patient count (integer)" :default "1"}
               {:flag "--churn" :doc "sim: turn churn on with sensible defaults" :default "false"}
               {:flag "--emit" :doc "sim: message format to emit -- \"hl7\" produces a v2 corpus" :default "hl7"}
               {:flag "--config" :doc "sim: path to an EDN file carrying the data-heavy engine keys (:pathway/:pathways/:order-profiles/:churn-profile/:site-profile/:modules/...)" :default "none"}]}
      {:verb "mutate" :doc "Apply one mutation operator at one locator to every matching file under PATH."
       :flags [{:flag "--path" :doc "alternative to the positional PATH"}
               {:flag "--operator-id" :doc "registered operator id -- see `ehrt corpus operators`"}
               {:flag "--operator-version" :doc "operator version" :default "1"}
               {:flag "--locator-path" :doc "format-specific locator string (FHIR data-path, or v2 segment/field grammar) -- falls back to the operator's own :default-locator when declared (D12); still required otherwise"}
               {:flag "--out-dir" :doc "directory for mutants + a lineage/ sidecar subdirectory" :default "<PATH>-mutants/<operator-id>@<operator-version>/"}]
       :positional "PATH"
       :positional-doc "a file, or a directory of files sharing one locator's shape, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"}
      {:verb "intake" :doc "Catalog a foreign (not generated by this repo) corpus batch -- or, given a generator URL (sim:?seed=42, synthea:?seed=1&population=5) in place of PATH, generate the corpus first and then catalog it, in one command (SS-2); or, given a stdin designator (stdin:?format=v2-er7&framing=er7-multi) in place of PATH, read piped bytes, spool them one file per item, and catalog the spool (SS-3)."
       :flags [{:flag "--path" :doc "alternative to the positional PATH"}
               {:flag "--label" :doc "source label for the intake record"}
               {:flag "--out" :doc "catalog output path"}
               {:flag "--received" :doc "received date, YYYY-MM-DD" :default "today"}]
       :positional "PATH"
       :positional-doc "a directory of files to catalog, OR a generator URL (sim:/synthea:) naming a corpus to generate then catalog, OR a stdin designator (stdin:?format=...&framing=...) naming how to decode piped bytes before cataloging them, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"}
      {:verb "operators" :doc "List the registered mutation operator catalog (a pure registry read; dropped/unconvictable candidates are docstring prose -- see docs/judge-calibration.md, not this listing)."
       :flags [{:flag "--format" :doc "\"fhir\" or \"v2\" -- narrow the listing to one format" :default "all"}]}]}

    {:group "gate"
     :doc "Conformance-gate a file or directory against HL7 v2 or FHIR. Bare `ehrt gate PATH` (no v2/fhir verb) sniffs the format via corpus.intake/sniff-format and dispatches (D11, ADR-0019); a directory mixing both formats, or containing a file the sniffer can't classify, is an error naming the explicit override (`gate v2 PATH` / `gate fhir PATH`), never a silent per-file split. PATH (and gate fhir's --out-dir) may also be spelled as a dir:/file: URL designator (ruling 7) instead of a bare path."
     :positional "PATH"
     :positional-doc "a file or directory, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"
     :verbs
     [{:verb "v2" :doc "Gate against HL7 v2 base-structural conformance (HAPI)."
       :flags gate-common-flags}
      {:verb "fhir" :doc "Gate against FHIR base-spec conformance (the official validator)."
       :flags (into gate-common-flags
                    [{:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}
                     {:flag "--out-dir" :doc "validator scratch directory" :default "out/scratch/gate-fhir"}
                     {:flag "--java-bin" :doc "java executable to invoke"}
                     {:flag "--no-verdict-cache" :doc "skip the content-addressed verdict cache (ADR-0016); always re-runs the validator subprocess" :default "false (caching on)"}])}]}

    {:group "check"
     :doc "Check a candidate corpus against an expected corpus and/or explicit per-file assertions -- the corpus's second judge, alongside Gate. DIR may also be spelled as a dir: URL designator (ruling 7) instead of a bare path."
     :positional "DIR"
     :positional-doc "check has no sub-verb: the second positional argument names the candidate directory directly"
     :flags [{:flag "--path" :doc "alternative to the positional DIR"}
             {:flag "--expected" :doc "expected-corpus directory (golden equivalence)"}
             {:flag "--assertions" :doc "path to an EDN file of assertion maps" :default "[{:kind :matches-expected}] when --expected is given"}
             {:flag "--canonicalizers" :doc "ordered \"id@version,id2@version2\" list" :default "none"}
             {:flag "--pair-by" :doc "\"path\" or \"hash\"" :default "path"}
             {:flag "--report" :doc "write the report EDN to this path"}]}

    {:group "version"
     :doc "Prints this repo's own honestly-pre-release identity (never a fabricated semver, D13) plus every pinned artifact's name@version from the lockfile."
     :flags [{:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}]}

    {:group "doctor"
     :doc "Runs SETUP.md's verification checklist as checks (D13): java resolution via the artifact registry, artifact cache presence per lockfile entry, git hooksPath wiring, and platform support. Exit 0 every check passed; 1 at least one failed; 2 couldn't even read the lockfile to know what to check."
     :flags [{:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}]}

    {:group "sim"
     :doc "Run the sim engine, mounted in-process (ADR-0005, ADR-0012 fulfilled) -- ehrt.sim.interface/run-command directly, no subprocess."
     :verbs
     [{:verb "run" :doc "Runs one deterministic simulation and returns its ground truth, manifest, and summary (plus --emit's rendered messages/bundles, when given)."
       :flags [{:flag "--seed" :doc "simulation seed (integer) -- required, determinism is a feature, not a default"}
               {:flag "--patients" :doc "patient count (integer)"}
               {:flag "--reference-date" :doc "ISO date string, pinned input for HL7 timestamp anchoring"}
               {:flag "--warm-up-seconds" :doc "engine warm-up window (integer)" :default "0"}
               {:flag "--emit" :doc "\"hl7\" to render messages into the payload, \"fhir\" to render FHIR bundles instead"}
               {:flag "--churn" :doc "turn churn on with sensible defaults" :default "false"}
               {:flag "--config" :doc "path to an EDN file carrying the data-heavy engine keys with no flag of their own (:pathway/:pathways/:order-profiles/:churn-profile/:site-profile/:modules/...)"}]}]}

    {:group "show"
     :doc "Render a file (or a directory of files sharing one sniffed format) for a human: HL7 v2 (ER7) one segment per line, blank line between messages; FHIR JSON pretty-printed. Pretty-always -- no flags needed, `ehrt show FILE | less` just works regardless of what stdout is attached to. Display is not wire format (ADR-0013): the rendered ER7 is deliberately nonconformant (LF-joined segments) and must never be piped anywhere a real HL7 v2 consumer sits."
     :positional "PATH"
     :positional-doc "a file, or a directory of files sharing one sniffed format, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"
     :flags [{:flag "--path" :doc "alternative to the positional PATH"}]}

    {:group "play"
     :doc "Paces a single HL7 v2 (ER7) file's own messages against their MSH-7 timestamps and renders (or writes) them over time -- `ehrt show` plus time (ADR-0014). `ehrt play FILE` at an arbitrarily large --rate, the default ticker sink, is exactly `ehrt show FILE`. A directory, or a FHIR JSON path, is a named, disclosed deferral this session (:play-input-unsupported)."
     :positional "PATH"
     :positional-doc "a single HL7 v2 (ER7) file, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"
     :flags [{:flag "--path" :doc "alternative to the positional PATH"}
             {:flag "--rate" :doc "stream-seconds per wallclock-second -- 1 is real time" :default "60"}
             {:flag "--idle-cap" :doc "wallclock cap, in seconds, on any single inter-event wait -- a capped wait emits a skip cue (never into a data sink) and is counted separately from a clamped one" :default "5"}
             {:flag "--ticker" :doc "\"full\" (a complete rendered block per message) or \"line\" (one compact MSH-7/MSH-9/PID-3 line per message) -- ignored when --sink is given" :default "full"}
             {:flag "--sink" :doc "a file: designator (ADR-0017's own vocabulary) to write paced, byte-identical-to-unpaced output to, instead of the ticker -- dir:/blaze: (and a future mllp: transport) are named, disclosed deferrals (ADR-0014)"}]}]})

(defn group-names
  "Every group name in the spec, in declared order."
  [spec]
  (mapv :group (:groups spec)))

(defn find-group
  [spec group-name]
  (first (filter #(= group-name (:group %)) (:groups spec))))

(defn verb-names
  "Verb names for a group, or nil for a group with no sub-verb (`check`)."
  [group]
  (some->> (:verbs group) (mapv :verb)))

(defn command-pairs
  "Every [group verb-or-nil] pair the spec declares -- verb is nil for a
  group with no sub-verb (`check`). This is the set a coverage test
  cross-checks against `dispatch`'s own routing, so a verb added to
  dispatch without a matching spec entry is a discoverable gap."
  [spec]
  (vec (mapcat (fn [g]
                 (if (:verbs g)
                   (map (fn [v] [(:group g) (:verb v)]) (:verbs g))
                   [[(:group g) nil]]))
               (:groups spec))))

(defn- render-flag
  [{:keys [flag doc default]}]
  (str "  " flag "  " doc (when default (str " (default: " default ")"))))

(defn- render-flags
  [flags]
  (if (seq flags)
    (str/join "\n" (map render-flag flags))
    "  (no flags)"))

(defn- render-exit-codes
  [exit-codes]
  (str/join "\n" (map (fn [{:keys [code meaning]}] (str "  " code "  " meaning)) exit-codes)))

(defn- render-verb
  "A verb's own :positional/:positional-doc (D10) render the same way a
  group's do (render-group below) -- declared per-verb rather than
  per-group because, unlike gate/check, not every verb in a group takes
  one (corpus generate/operators don't; corpus mutate/intake do)."
  [group-name {:keys [verb doc flags positional positional-doc]}]
  (str "ehrt " group-name " " verb "\n"
       "  " doc "\n"
       (when positional
         (str "\nPositional: " positional " -- " positional-doc "\n"))
       "\n"
       "Flags:\n" (render-flags flags)))

(defn render-group
  "Group usage text: `ehrt help <group>` and `ehrt <group> --help`. Returns
  nil for an unrecognized group name -- callers decide what that means."
  [spec group-name]
  (when-let [g (find-group spec group-name)]
    (str "ehrt " group-name " -- " (:doc g) "\n"
         (when (:positional g)
           (str "\nPositional: " (:positional g) " -- " (:positional-doc g) "\n"))
         "\n"
         (if (:verbs g)
           (str/join "\n\n" (map #(render-verb group-name %) (:verbs g)))
           (str "Flags:\n" (render-flags (:flags g))))
         "\n\nExit codes:\n" (render-exit-codes (:exit-codes spec)))))

(defn render-top-level
  "The top-level usage text: bare `ehrt`, `ehrt help`, `--help` with no
  group. Always succeeds (the spec is static data, not user input)."
  [spec]
  (str "Usage: " (:program spec) " <group> [<verb>] [flags]\n\n"
       (when (:doc spec) (str (:doc spec) "\n\n"))
       "Groups:\n"
       (str/join "\n" (map (fn [g] (str "  " (:group g) "  " (:doc g))) (:groups spec)))
       "\n\n"
       "Run `" (:program spec) " help <group>` for a group's verbs and flags.\n\n"
       "Global flags:\n" (render-flags (:global-flags spec))
       "\n\nExit codes:\n" (render-exit-codes (:exit-codes spec))))

;; ---- impure shell (I/O) ----

(defn write-cli-md!
  "-X-invokable: regenerates docs/cli.md from this namespace's own
  cli-spec (the Makefile's `cli-doc` target passes docs/cli.md, moved
  out of components/tools/docs/ to the root user path, ADR-0010).
  Lives here, not in components/tools/docsgen, because only this base
  can supply the real
  spec without inverting Polylith's base -> component dependency
  direction (ADR-0002's own deviation record on why docsgen.clj's
  cli.md renderer moved out of requiring cli.help directly) --
  ehrt.tools.interface/write-cli-md! does the actual rendering/spit,
  this function only supplies :spec."
  [{:keys [out]}]
  (tools/write-cli-md! {:out out :spec cli-spec}))

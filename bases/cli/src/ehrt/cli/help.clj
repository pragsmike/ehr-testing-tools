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
            [ehrt.docs-tooling.interface :as docs-tooling]))

(def exit-codes
  "0/1/2/3 per ADR-0004's ok/rejected/error mapping, extended by
  tools/ADR-0010 for the no-verdict arm -- see `cli/result->exit-code`'s own
  docstring for the authoritative reasoning; this table only cites it."
  ;; Mapping reasoning: ADR-0004 (ok/rejected/error), extended by tools/ADR-0010
  ;; (no-verdict arm). Authoritative logic: cli/result->exit-code.
  [{:code 0 :meaning "ran and passed"}
   {:code 0 :meaning "bare invocation, help, and --help all exit 0 too"}
   {:code 1 :meaning "ran and legitimately rejected"}
   {:code 2 :meaning "operational error (bad invocation, missing artifact, subprocess failure, etc.)"}
   {:code 3 :meaning "a gate found :no-verdict outcomes and the default --treat-no-verdict-as policy is in effect -- see that flag to fold them into pass or rejected"}])

(def global-flags
  ;; --pretty/--edn terminal-detection defaults: ADR-0013.
  ;; --width: AR-EP-3, ux epilogue -- help output only, not any other
  ;; command's own rendering; scoped in its own :doc below since this
  ;; is the one global flag that doesn't apply everywhere the others do.
  [{:flag "--json" :doc "project the EDN result to JSON (EDN remains canonical)"}
   {:flag "--pretty" :doc "force a human-readable summary, even when stdout is piped -- already the default at a real terminal"}
   {:flag "--edn" :doc "force the raw EDN envelope, even at a terminal -- already the default when stdout is piped or redirected"}
   {:flag "--help" :doc "print this command's usage and exit 0 without running it"}
   {:flag "--width" :doc "wrap help output at this many columns (an integer, 40 or more) -- affects help text only, not any other command's own output" :default "the COLUMNS environment variable, or 80 if that is unset or unusable"}])

(def ^:private artifact-flags
  [{:flag "--name" :doc "artifact name, e.g. \"synthea\""}
   {:flag "--version" :doc "artifact version, e.g. \"4.0.0\""}
   {:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}])

;; no-verdict folding policy: tools/ADR-0010.
(def ^:private gate-common-flags
  [{:flag "--path" :doc "alternative to the positional PATH"}
   {:flag "--report" :doc "write the report EDN to this path"}
   {:flag "--baseline" :doc "baseline-relative mode: path to a previous --report EDN; only genuinely new findings count"}
   {:flag "--treat-no-verdict-as" :doc "\"pass\" or \"rejected\" -- folds :no-verdict into an existing polarity"}])

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
   [;; Artifact registry design: ADR-0005. --all introduced in D13,
    ;; replacing SETUP.md's multi-fetch walkthrough.
    {:group "artifact"
     :doc "Fetch and resolve locked external engine/tool artifacts."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, README.md Quickstart
     ;; fence, line 203 -- source cited in notes/adr/0118-*.md.
     :example "bin/ehrt artifact fetch --name synthea --version 4.0.0"
     :verbs
     [{:verb "fetch" :doc "Fetch a locked artifact into the local content-addressed cache."
       :flags (into artifact-flags
                    [{:flag "--all" :doc "fetch every artifact the lockfile names; --name/--version are ignored when given. One failing artifact does not abort the rest -- the overall result is the worst individual outcome." :default "false"}])}
      {:verb "resolve" :doc "Resolve an already-fetched artifact to a filesystem path."
       :flags artifact-flags}]}

    ;; URL designators: ruling 7, docs/source-sink-design.md D4.
    ;; generate front door + bare=sim: ADR-0015 (+ 2026-07-30 amendment:
    ;; sim needs no fetched artifacts, so the cold first command succeeds).
    ;; intake compose forms: SS-2 (generator URL), SS-3 (stdin designator).
    ;; Zero-flag reproducible defaults: D9 / ADR-0019.
    {:group "corpus"
     :doc "Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out also accepts a dir:/file: URL designator in place of a bare path; bare paths are the common spelling. `corpus generate` is the front door for new corpora; `corpus intake` catalogs existing ones -- and can generate-then-catalog, or read piped bytes, in one command (see intake)."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, README.md Quickstart
     ;; fence, line 197 -- source cited in notes/adr/0118-*.md.
     :example "bin/ehrt corpus generate"
     :verbs
     [{:verb "generate" :doc "Generate a deterministic synthetic corpus. Takes a source subcommand: `corpus generate sim` (this workspace's own engine; the flags marked sim:) or `corpus generate synthea` (the flags marked synthea:). Bare `corpus generate` means `generate sim`. Both bare commands are byte-reproducible as-is; re-running into an existing non-empty --out-dir is rejected (:out-dir-exists), never silently overwritten."
       :flags [{:flag "--config-path" :doc "synthea: Synthea properties file" :default "resources/synthea-default.properties"}
               {:flag "--seed" :doc "patient/master-generation seed (integer; non-negative when --source sim), shared by both sources; defaulted here as the ergonomic front door -- the sim-tier verbs (sim run, sim identifiers) require a seed explicitly" :default "1"}
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
               {:flag "--patients" :doc "sim: patient count (integer): simulated ARRIVALS, not emitted-event volume (docs/consuming-ground-truth.md#scale)" :default "1"}
               {:flag "--churn" :doc "sim: turn churn on with sensible defaults" :default "false"}
               {:flag "--emit" :doc "sim: message format to emit -- \"hl7\" produces a v2 corpus" :default "hl7"}
               {:flag "--config" :doc "sim: path to an EDN file carrying the data-heavy engine keys (:pathway/:pathways/:order-profiles/:churn-profile/:site-profile/:modules/...)" :default "none"}]}
      ;; --locator-path default-locator fallback: D12.
      {:verb "mutate" :doc "Apply one mutation operator at one locator to every matching file under PATH."
       :flags [{:flag "--path" :doc "alternative to the positional PATH"}
               {:flag "--operator-id" :doc "registered operator id -- see `ehrt corpus operators`"}
               {:flag "--operator-version" :doc "operator version" :default "1"}
               {:flag "--locator-path" :doc "format-specific locator string (FHIR data-path, or v2 segment/field grammar) -- falls back to the operator's own :default-locator when it declares one; required otherwise"}
               {:flag "--out-dir" :doc "directory for mutants + a lineage/ sidecar subdirectory" :default "<PATH>-mutants/<operator-id>@<operator-version>/"}]
       :positional "PATH"
       :positional-doc "a file, or a directory of files sharing one locator's shape, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"}
      {:verb "intake" :doc "Catalog a foreign corpus batch (one not generated by this repo). In place of PATH: a generator URL (sim:?seed=42, synthea:?seed=1&population=5) generates the corpus first and then catalogs it; a stdin designator (stdin:?format=v2-er7&framing=er7-multi) reads piped bytes, spools them one file per item, and catalogs the spool."
       :flags [{:flag "--path" :doc "alternative to the positional PATH"}
               {:flag "--label" :doc "source label for the intake record"}
               {:flag "--out" :doc "catalog output path"}
               {:flag "--received" :doc "received date, YYYY-MM-DD" :default "today"}]
       :positional "PATH"
       :positional-doc "a directory of files to catalog, OR a generator URL (sim:/synthea:) naming a corpus to generate then catalog, OR a stdin designator (stdin:?format=...&framing=...) naming how to decode piped bytes before cataloging them, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"}
      {:verb "operators" :doc "List the registered mutation-operator catalog. Candidates that were considered and dropped are documented in docs/judge-calibration.md, not here."
       :flags [{:flag "--format" :doc "\"fhir\" or \"v2\" -- narrow the listing to one format" :default "all"}]}
      ;; ADR-0111: a corpus-level tool, deliberately separate from the
      ;; sim -- works on any directory of valid v2 message files,
      ;; including a foreign corpus this repo never generated (author
      ;; ruling, Q1 a). The HL7 v2 batch protocol's BHS/BTS wrapper
      ;; lands as ehrt.corpus-io.framing's own :batch codec (Q2 a).
      {:verb "batch" :doc "Partitions every HL7 v2 (ER7) message under DIR into schedule-aligned delivery batches, sorted by MSH-7 across every candidate file (never file order), and writes one BHS/BTS-wrapped batch-NNN.hl7 per occupied interval. DIR may be any directory of valid v2 message files, including a foreign corpus this repo never generated. Deterministic: no wall clock anywhere, byte-stable for the same input and --interval."
       :flags [{:flag "--path" :doc "alternative to the positional DIR"}
               {:flag "--interval" :doc "batch interval, in minutes (e.g. 60 for hourly, 1440 for daily) -- buckets align to the Unix epoch, so hourly batches align to the hour and daily batches to UTC midnight. REQUIRED: no default -- there is no universally sensible schedule to assume."}
               {:flag "--out-dir" :doc "directory for the written batch-NNN.hl7 files -- rejected if it already exists and is non-empty" :default "<DIR>-batches/"}]
       :positional "DIR"
       :positional-doc "a directory of HL7 v2 (ER7) message files (multi-message files are split), given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"}]}

    ;; Sniff dispatch: D11 / ADR-0019, via corpus.intake/sniff-format.
    ;; NIST profile tier: ADR-0012. Designators: ruling 7.
    {:group "gate"
     :doc "Conformance-gate a file or directory against HL7 v2, FHIR, or (with --profile) an HL7 v2 conformance profile. Bare `ehrt gate PATH` sniffs the format and dispatches between v2 and fhir only -- never v2-nist, which needs an explicit --profile. A directory mixing formats, or a file that can't be classified, is an error naming the explicit override (`gate v2 PATH` / `gate fhir PATH`), never a silent per-file split. PATH and --scratch-dir (gate fhir only) also accept dir:/file: URL designators."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, README.md "See it
     ;; run" fence, line 92 (a fixture shipped in the repo, runs with
     ;; no fetched artifacts) -- source cited in notes/adr/0118-*.md.
     :example "bin/ehrt gate fhir test-fixtures/fhir/storefront-patient.json"
     :positional "PATH"
     :positional-doc "a file or directory, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"
     :verbs
     [{:verb "v2" :doc "Gate against HL7 v2 base-structural conformance (HAPI)."
       :flags (conj gate-common-flags
                    {:flag "--sample-add-ons"
                     :doc "gating at scale: cap how many messages of each ADD-ON family (an MSH-9 outside the emitter's own message-type registry -- ADT^A08/A31/A28, DFT^P03) are gated, taking the first N by MSH-10. Skeleton-kind families are always gated in full. The report's :run carries the per-stratum n/gated census, so a cap is never silent."})}
      ;; verdict cache: ADR-0016.
      ;; F7 (R3-B1-1, RULED ADR-0115 RQ1, ADR-0117): --out-dir renamed
      ;; --scratch-dir, NO back-compat alias -- corpus generate/mutate/
      ;; batch's own --out-dir names a protected artifact (collision-
      ;; refused); this flag names a freely-reusable validator working
      ;; directory, an unrelated concept that happened to share a name.
      {:verb "fhir" :doc "Gate against FHIR base-spec conformance (the official validator)."
       :flags (into gate-common-flags
                    [{:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}
                     {:flag "--scratch-dir" :doc "validator scratch directory" :default "out/scratch/gate-fhir"}
                     {:flag "--java-bin" :doc "java executable to invoke"}
                     {:flag "--no-verdict-cache" :doc "skip the content-addressed verdict cache; always re-run the validator subprocess" :default "false (caching on)"}])}
      ;; Engine perf note (validator built once per invocation, reused
      ;; across files -- context construction dominates): ADR-0012.
      ;; Π-bundle vocabulary + CDC fixture provenance: ADR-0012 / register.
      {:verb "v2-nist" :doc "Gate against HL7 v2 profile-tier conformance (the NIST engine): profile usage, cardinality, length, conformance statements, co-constraints, slicing, and value-set bindings -- what the structural v2 tier cannot check. Complementary to `gate v2`, not a replacement."
       :flags (into gate-common-flags
                    [{:flag "--profile" :doc "REQUIRED: a conformance-profile bundle directory -- PROFILE.xml required; CONSTRAINTS.xml, VALUESETS.xml, VALUESETBINDINGS.xml, COCONSTRAINTS.xml, SLICINGS.xml optional. No default. To try one: test-fixtures/v2-nist/COVID19_ELR-v2.3.1"}])}]}

    ;; Designators: ruling 7.
    {:group "check"
     :doc "Check a candidate corpus against an expected corpus and/or explicit per-file assertions -- the corpus's second judge, alongside gate. DIR also accepts a dir: URL designator."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, docs/use-cases/
     ;; reproduction-packages.md, line 36 -- source cited in
     ;; notes/adr/0118-*.md.
     :example "bin/ehrt check out/repro-b/fhir --expected out/repro-a/fhir --pair-by hash"
     :positional "DIR"
     :positional-doc "check has no sub-verb: the second positional argument names the candidate directory directly"
     :flags [{:flag "--path" :doc "alternative to the positional DIR"}
             {:flag "--expected" :doc "expected-corpus directory (golden equivalence)"}
             {:flag "--assertions" :doc "path to an EDN file of assertion maps" :default "[{:kind :matches-expected}] when --expected is given"}
             {:flag "--canonicalizers" :doc "ordered \"id@version,id2@version2\" list" :default "none"}
             {:flag "--pair-by" :doc "\"path\" or \"hash\"" :default "path"}
             {:flag "--report" :doc "write the report EDN to this path"}]}

    ;; Honest pre-release identity ruling: D13.
    ;; B2 (R3-B3-1, ADR-0118): no :example -- no witnessed invocation of
    ;; `ehrt version` exists anywhere in README.md's Quickstart, any
    ;; docs/use-cases/*.md, or any demos/**/README.md (checked this
    ;; session); per the sourced-only rule, rendering none rather than
    ;; inventing one -- gap recorded as a register addendum row.
    {:group "version"
     :doc "Print this repo's own pre-release identity (it deliberately has no semver yet) plus every pinned artifact's name@version from the lockfile."
     :flags [{:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}]}

    ;; B2 (R3-B3-1, ADR-0118): no :example, same gap class as version
    ;; above -- no witnessed invocation of `ehrt doctor` exists anywhere
    ;; in the same three source classes (checked this session).
    {:group "doctor"
     :doc "Run SETUP.md's verification checklist as checks: java resolution via the artifact registry, artifact cache presence per lockfile entry, git hooksPath wiring, and platform support. Exit 0: every check passed; 1: at least one failed; 2: couldn't even read the lockfile to know what to check."
     :flags [{:flag "--lockfile" :doc "path to the lockfile" :default "artifacts.lock.edn"}]}

    ;; In-process mount: ADR-0005/ADR-0012 fulfilled; the entry point is
    ;; ehrt.sim.interface/run-command.
    {:group "sim"
     :doc "Run the sim engine, in-process -- no subprocess, no fetched artifacts needed."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, README.md Quickstart
     ;; fence, line 226 -- source cited in notes/adr/0118-*.md.
     :example "bin/ehrt sim run --seed 100 --patients 1"
     :verbs
     [{:verb "run" :doc "Runs one deterministic simulation and returns its ground truth, manifest, and summary (plus --emit's rendered messages/bundles, when given)."
       :flags [{:flag "--seed" :doc "simulation seed (integer, non-negative) -- required, determinism is a feature, not a default"}
               {:flag "--patients" :doc "patient count (integer): simulated ARRIVALS, not emitted-event volume (docs/consuming-ground-truth.md#scale)"}
               {:flag "--arrival-gap" :doc "max minutes between arrivals (integer)" :default "60"}
               {:flag "--reference-date" :doc "ISO date string, pinned input for HL7 timestamp anchoring"}
               {:flag "--utc-offset" :doc "fixed ISO offset suffixed onto HL7 timestamps (pinned input, no DST)" :default "+00:00"}
               {:flag "--warm-up-seconds" :doc "engine warm-up window (integer)" :default "0"}
               {:flag "--emit" :doc "\"hl7\" to render messages into the payload, \"fhir\" to render FHIR bundles instead"}
               {:flag "--at" :doc "with --emit fhir: seconds from run start to snapshot (integer, default: end of run)"}
               {:flag "--churn" :doc "turn churn on with sensible defaults" :default "false"}
               {:flag "--config" :doc "path to an EDN file carrying the data-heavy engine keys with no flag of their own (:pathway/:pathways/:order-profiles/:churn-profile/:site-profile/:modules/...)"}
               {:flag "--format" :doc "\"er7\": bare wire messages to stdout (requires --emit hl7). \"ground-truth\": the bare ground-truth EDN vector -- the SEMANTIC stream underneath every message this simulator emits, and a PUBLIC, VERSIONED contract. Two jobs, both first-class. (1) As a TEST ORACLE for a system of your own: derive your invariants over the patients, encounters, appointments and beds it describes, and assert your system agrees -- `ehrt sim check` is the reference judge over the same log and `ehrt sim mutate` injects one named defect class for a controlled negative. The run contract -- which config keys make which kinds appear at all, what a green check does and does NOT certify, and how far it scales -- is docs/consuming-ground-truth.md; the paste-able strip is docs/use-cases/ground-truth-as-a-test-oracle.md. (2) As the contract both built emitters read, so you can write your own for a format we don't ship (shape contract: docs/formats.md \"The event log\"; worked path, two examples: docs/use-cases/custom-emitter-from-the-event-log.md). Pipes straight into `ehrt sim check` either way. Default: the full EDN envelope; --json works as always."}]}
      {:verb "check" :doc "Runs the invariant catalog (capacity/surge-ladder, timestamp-monotone, and friends) over a ground-truth EDN vector read from stdin -- e.g. `ehrt sim run --format ground-truth | ehrt sim check`. Four of the invariants need config the log itself does not carry, so pass the SAME --config the run used: without it they are checked against the shipped defaults, and a scenario that raises a ward's capacity reads as violating `occupancy-within-capacity` on its own clean log."
       :flags [{:flag "--config" :doc "path to the EDN file the run used (same flag, same file, as `ehrt sim run`) -- its `:facility` and `:warm-up-seconds` are what the config-needing invariants are then checked against, exactly the pair the run's own self-check uses. `:order-profiles` is NOT threaded, here or in the run's self-check, so `result-analytes-match-order-profile` reads the shipped defaults either way" :default "the shipped default facility and a zero warm-up window"}]}
      {:verb "mutate" :doc "Injects ONE event-level defect into a ground-truth EDN vector read from stdin and writes the mutant to stdout -- a filter, so `ehrt sim run --format ground-truth | ehrt sim mutate --operator-id ID --seed N | ehrt sim check` is the whole loop: inject a named defect class, and see the checker report that class and nothing else. Mutating the event log rather than a rendered file means every emitter downstream inherits one mutated truth, instead of the same defect having to be written once per format (`ehrt corpus mutate` is the file-level verb, and stays the right one for faults that only exist once a record is written out as bytes). With no --operator-id, this is a byte-identical pass-through."
       :flags [{:flag "--operator-id" :doc "which defect to inject -- an event-log operator's id, listed by `ehrt corpus operators --format event` with the exact finding each one is built to trip. Omitted: the log passes through unchanged, byte for byte"}
               {:flag "--operator-version" :doc "operator version" :default "1"}
               {:flag "--seed" :doc "the OPERATOR's own seed (integer), required once --operator-id is given -- it selects which one of the log's eligible sites gets the defect, so re-running with the same seed reproduces the same mutant and a different seed injects somewhere else. Independent of the run's own --seed, so this works on any log, including one whose run seed you don't have"}
               {:flag "--lineage" :doc "path to write this mutation's provenance to, as an EDN sidecar: the parent log's hash, the operator and seed, the site it landed on, and the exact finding set the defect is built to trip. Stdout is the mutant and nothing else, so provenance rides beside the pipe rather than in it. Omitted: no file is written and stdout is unchanged"}]}
      {:verb "identifiers" :doc "Config + seed -> the complete EDN inventory of every identifier this run's output would contain (patient-ids, MRNs, visit beds, HL7 control ids, FHIR resource ids, provider NPIs, run-id) -- how you'd find and remove synthetic data that ever reached a real system (docs/simulate-your-facility.md)."
       :flags [{:flag "--seed" :doc "RNG seed (required, non-negative; same as `ehrt sim run`'s own --seed)"}
               {:flag "--patients" :doc "patient count (integer): simulated ARRIVALS, not emitted-event volume (docs/consuming-ground-truth.md#scale)" :default "1"}
               {:flag "--config" :doc "path to an EDN file supplying data-heavy engine keys (same as `ehrt sim run`)"}]}
      {:verb "version" :doc "Print sim's own library version and git SHA -- the same source the run manifest's :generator block stamps."
       :flags []}]}

    ;; Display-vs-wire ruling: ADR-0013.
    {:group "show"
     :doc "Render a file (or a directory of files sharing one sniffed format) for a human: HL7 v2 (ER7) one segment per line, blank line between messages; FHIR JSON pretty-printed. Always pretty -- `ehrt show FILE | less` just works. The rendered ER7 is display-only and deliberately nonconformant (LF-joined segments): never pipe it anywhere a real HL7 v2 consumer sits."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, docs/use-cases/
     ;; generate-sim-traffic.md, line 28 -- source cited in
     ;; notes/adr/0118-*.md.
     :example "bin/ehrt show out/corpus/sim-s42-p5"
     :positional "PATH"
     :positional-doc "a file, or a directory of files sharing one sniffed format, given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"
     :flags [{:flag "--path" :doc "alternative to the positional PATH"}]}

    ;; Pacer design: ADR-0014. Lexical-order contract: ADR-0015.
    ;; --sink designator vocabulary: ADR-0017; deferred sinks: ADR-0014.
    {:group "play"
     :doc "Pace a corpus's own events against their own timestamps and render (or write) them over time -- `ehrt show` plus time. PATH is an HL7 v2 (ER7) file or directory (paced by MSH-7), or a sim event log (a single .edn file, paced by each event's own :t) -- see the PATH description below for both shapes. A directory's files must share the v2 format and are concatenated in LEXICAL FILENAME ORDER before pacing: that ordering is the contract, so name files so sort order is play order (the sim generator pads its own msg-NNN index to the width that corpus needs, so its output always is). FHIR or mixed message input is a named deferral (:play-input-unsupported)."
     ;; B2 (R3-B3-1, ADR-0118): witnessed verbatim, README.md "See it
     ;; run" fence, line 34 -- source cited in notes/adr/0118-*.md. The
     ;; source moved 2026-08-29 when that fence's own lead scenario went
     ;; clinic-decade -> ed-tuesday; the sourced copy follows it.
     :example "bin/ehrt play out/scenarios/ed-tuesday --board 60 --rate 3600"
     :positional "PATH"
     :positional-doc "an HL7 v2 (ER7) file, a directory of files sharing the sniffed v2 format (concatenated in lexical filename order; a .edn event log sitting in that same directory is ignored), or a single .edn sim event log (a vector of ground-truth event maps -- `ehrt sim run --format ground-truth`'s own output, or `ehrt corpus generate sim`'s own events.edn), given as a trailing positional argument, not --path -- an explicit --path is never overridden by it"
     :flags [{:flag "--path" :doc "alternative to the positional PATH"}
             {:flag "--rate" :doc "stream-seconds per wallclock-second -- 1 is real time" :default "60"}
             {:flag "--idle-cap" :doc "wallclock cap, in seconds, on any single inter-event wait -- a capped wait emits a skip cue (never into a data sink) and is counted separately from a clamped one" :default "5"}
             {:flag "--ticker" :doc "message input: \"full\" (a complete rendered block per message) or \"line\" (one compact MSH-7/MSH-9/PID-3 line per message). Event-log input renders the same compact event line either way (timestamp, event kind, location, citation when present) -- ignored when --sink is given; --board wins over it when both are given" :default "full"}
             {:flag "--board" :doc "stream-minutes per snapshot -- shows the occupied beds, grouped by ward, instead of a message-by-message ticker. Message input only (:play-board-unsupported-for-events on an event log). Wins over --ticker when both are given; ignored when --sink is given."}
             {:flag "--sink" :doc "a destination designator -- write the paced output (byte-identical to unpaced) there instead of showing the ticker. file:PATH writes a file; mllp://HOST:PORT sends each message to an MLLP receiver and reads its ACK back, failing on a negative acknowledgement (MSA-1 AE/AR), an unrecognized code, or an ACK that never arrives. An mllp: sink implies mllp framing, so declaring any other framing on it is an error. Message input only (:play-sink-unsupported-for-events on an event log, which has no wire framing to write). dir: and blaze: are recognized but deferred."}]}]})

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

(def default-wrap-width
  "Terminal columns a rendered line degrades gracefully at (AR-U5-1,
  ADR-0063) absent an explicit --width or a usable COLUMNS (AR-EP-3, ux
  epilogue, `notes/adr/0065-ux-epilogue.md`) -- every render-* function
  below takes an optional trailing width arg, both for the
  content-preservation property (AR-U5-2(b), comparing a wrapped render
  against an effectively-unwrapped one) and, since AR-EP-3, as the real
  user-facing knob's own resolved value."
  80)

(def min-wrap-width
  "The floor `--width`/COLUMNS must clear (AR-EP-3): below this a
  wrapped page stops being legible at all -- a flag's own two-space
  indent plus its shortest value already needs more than a handful of
  columns."
  40)

(defn- parse-integer
  "s parsed as a base-10 whole number, or nil -- never throws, unlike
  `Long/parseLong` directly, since both --width (user input) and
  COLUMNS (ambient) need a non-throwing parse to build their own
  differently-shaped fallback around. nil input (COLUMNS unset) is
  also nil out, not an error."
  [s]
  (when (and s (re-matches #"-?\d+" s))
    (try (Long/parseLong ^String s) (catch NumberFormatException _ nil))))

(defn valid-width?
  "True for an integer no smaller than min-wrap-width -- false for nil,
  a non-integer, or anything under the floor."
  [n]
  (and (integer? n) (>= n min-wrap-width)))

(defn parse-width-flag
  "Validates a `--width` flag's raw string value (AR-EP-3): user input,
  rejected by name rather than silently coerced. {:width n} when it
  parses to an integer >= min-wrap-width; {:error {:value s :expected
  \"an integer >= 40\"}} otherwise -- the caller decides how that
  becomes an operational error."
  [s]
  (let [n (parse-integer s)]
    (if (valid-width? n)
      {:width n}
      {:error {:value s :expected (str "an integer >= " min-wrap-width)}})))

(defn resolve-width
  "Resolution order (AR-EP-3): explicit-width (an already-validated
  integer, from a parsed --width) beats columns-env (ambient -- COLUMNS
  -- ANY value that doesn't parse to an integer >= min-wrap-width falls
  back SILENTLY here, never an error: a broken terminal variable must
  not break help) beats default-wrap-width."
  [{:keys [explicit-width columns-env]}]
  (cond
    (some? explicit-width) explicit-width
    (valid-width? (parse-integer columns-env)) (parse-integer columns-env)
    :else default-wrap-width))

(defn- wrap-lines
  "Greedy word-wrap of s into lines of at most width columns, wrapping
  on spaces only -- a single token longer than width still gets its own
  line, unbroken, rather than split mid-token. Joining the result with
  a single space reconstructs s exactly: wrapping only ever replaces an
  existing inter-word space with a line break, never touches word
  content (AR-U5-2(b))."
  [s width]
  (let [words (str/split s #" ")]
    (reduce (fn [lines word]
              (if (empty? lines)
                [word]
                (let [candidate (str (peek lines) " " word)]
                  (if (<= (count candidate) width)
                    (conj (pop lines) candidate)
                    (conj lines word)))))
            []
            words)))

(defn- wrap-with-hanging-indent
  "Renders prefix followed by text, word-wrapped so no line exceeds
  `width` total columns; continuation lines are indented to align under
  where text starts on the first line (prefix's own length) -- flag
  rows wrap under their description start, group/verb docs under their
  own text start, per AR-U5-1's layout convention."
  ([prefix text] (wrap-with-hanging-indent prefix text default-wrap-width))
  ([prefix text width]
   (let [prefix-len (count prefix)
         avail (max 1 (- width prefix-len))
         indent (apply str (repeat prefix-len \space))]
     (str prefix (str/join (str "\n" indent) (wrap-lines text avail))))))

(defn- render-flag
  ([flag-spec] (render-flag flag-spec default-wrap-width))
  ([{:keys [flag doc default]} width]
   (wrap-with-hanging-indent (str "  " flag "  ")
                              (str doc (when default (str " (default: " default ")")))
                              width)))

(defn- render-flags
  ([flags] (render-flags flags default-wrap-width))
  ([flags width]
   (if (seq flags)
     (str/join "\n" (map #(render-flag % width) flags))
     "  (no flags)")))

(defn- render-exit-codes
  ([exit-codes] (render-exit-codes exit-codes default-wrap-width))
  ([exit-codes width]
   (str/join "\n" (map (fn [{:keys [code meaning]}]
                          (wrap-with-hanging-indent (str "  " code "  ") meaning width))
                        exit-codes))))

(defn- render-verb
  "A verb's own :positional/:positional-doc (D10) render the same way a
  group's do (render-group below) -- declared per-verb rather than
  per-group because, unlike gate/check, not every verb in a group takes
  one (corpus generate/operators don't; corpus mutate/intake do)."
  ([group-name verb-spec] (render-verb group-name verb-spec default-wrap-width))
  ([group-name {:keys [verb doc flags positional positional-doc]} width]
   (str "ehrt " group-name " " verb "\n"
        (wrap-with-hanging-indent "  " doc width) "\n"
        (when positional
          (str "\n" (wrap-with-hanging-indent (str "Positional: " positional " -- ") positional-doc width) "\n"))
        "\n"
        "Flags:\n" (render-flags flags width))))

(defn render-group
  "Group usage text: `ehrt help <group>` and `ehrt <group> --help`. Returns
  nil for an unrecognized group name -- callers decide what that means.

  B2 (R3-B3-1, ADR-0118): a group carrying its own :example (a
  witnessed, verbatim invocation -- see cli-spec's own per-group
  comments for each one's source) renders one \"Example:\" line before
  the exit-code table; a group with no witnessed invocation anywhere
  (version, doctor) renders none rather than an invented one. Verb-
  narrowed help (`render-verb-help`) never shows this -- it belongs to
  \"the whole group screen,\" the thing verb-narrowing is for NOT
  showing."
  ([spec group-name] (render-group spec group-name default-wrap-width))
  ([spec group-name width]
   (when-let [g (find-group spec group-name)]
     (str (wrap-with-hanging-indent (str "ehrt " group-name " -- ") (:doc g) width) "\n"
          (when (:positional g)
            (str "\n" (wrap-with-hanging-indent (str "Positional: " (:positional g) " -- ") (:positional-doc g) width) "\n"))
          "\n"
          (if (:verbs g)
            (str/join "\n\n" (map #(render-verb group-name % width) (:verbs g)))
            (str "Flags:\n" (render-flags (:flags g) width)))
          (when (:example g)
            (str "\n\nExample:\n" (wrap-with-hanging-indent "  " (:example g) width)))
          "\n\nExit codes:\n" (render-exit-codes (:exit-codes spec) width)))))

(defn render-verb-help
  "A single verb's own usage text -- `ehrt help <group> <verb>` and
  `ehrt <group> <verb> --help` (B1, R3-B3-2, ADR-0118): just that
  verb's own description and flags, not the whole group's page every
  invocation form used to fall back to regardless of how specifically
  a caller asked. nil when group or verb is unrecognized -- callers
  decide what that means (dispatch's own F6 unknown-verb treatment,
  reusing the unknown-group category verbatim, for a group that HAS
  verbs but not this one)."
  ([spec group-name verb-name] (render-verb-help spec group-name verb-name default-wrap-width))
  ([spec group-name verb-name width]
   (when-let [g (find-group spec group-name)]
     (when-let [v (first (filter #(= verb-name (:verb %)) (:verbs g)))]
       (str (render-verb group-name v width)
            "\n\nExit codes:\n" (render-exit-codes (:exit-codes spec) width))))))

(defn render-top-level
  "The top-level usage text: bare `ehrt`, `ehrt help`, `--help` with no
  group. Always succeeds (the spec is static data, not user input)."
  ([spec] (render-top-level spec default-wrap-width))
  ([spec width]
   (str "Usage: " (:program spec) " <group> [<verb>] [flags]\n\n"
        (when (:doc spec) (str (wrap-with-hanging-indent "" (:doc spec) width) "\n\n"))
        "Groups:\n"
        (str/join "\n" (map (fn [g] (wrap-with-hanging-indent (str "  " (:group g) "  ") (:doc g) width)) (:groups spec)))
        "\n\n"
        (wrap-with-hanging-indent "" (str "Run `" (:program spec) " help <group>` for a group's verbs and flags.") width)
        "\n\n"
        "Global flags:\n" (render-flags (:global-flags spec) width)
        "\n\nExit codes:\n" (render-exit-codes (:exit-codes spec) width))))

;; ---- impure shell (I/O) ----

(defn write-cli-md!
  "-X-invokable: regenerates docs/cli.md from this namespace's own
  cli-spec (the Makefile's `cli-doc` target passes docs/cli.md, moved
  out of components/corpus/docs/ to the root user path, ADR-0010).
  Lives here, not in components/docs-tooling/.../docsgen, because only
  this base can supply the real spec without inverting Polylith's
  base -> component dependency direction (ADR-0002's own deviation
  record on why docsgen.clj's cli.md renderer moved out of requiring
  cli.help directly) -- ehrt.docs-tooling.interface/write-cli-md! does
  the actual rendering/spit, this function only supplies :spec.

  Calls docs-tooling directly (docs-tooling split, 2026-07-31), not
  through ehrt.tools.interface as before that split: routing this call
  through tools as well as keeping it here would have made tools and
  docs-tooling depend on each other -- a real circular component
  dependency (docs-tooling.lint genuinely needs to reach back into
  tools' own operator/framing/schema registries, in the other
  direction) -- `poly check` Error 104, not a style preference."
  [{:keys [out]}]
  (docs-tooling/write-cli-md! {:out out :spec cli-spec}))

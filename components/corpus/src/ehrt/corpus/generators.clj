(ns ehrt.corpus.generators
  "The generator registry (D1/D7, docs/source-sink-design.md Parts I and
  VII; SS-2 Step 1): shaped like ehrt.corpus.operators's
  own registry (register!/lookup/entries) -- adding a generator means
  registering an entry here, not writing a new per-kind adapter.

  An entry is a recipe, not code: :default-params (pinned constants,
  the D8 determinism law -- never the clock/environment/machine) merged
  UNDER whatever params a caller supplies, :params-schema (a Malli
  schema validating the MERGED map, open by default so kind-specific
  extras -- including injected-fake dependencies in tests -- pass
  through unvalidated at this level, same convention as
  ehrt.corpus-io.source-sink's own open :map schemas),
  :out-dir-fn (merged params -> a deterministic output directory, D9's
  own derived-path pattern generalized past synthea alone), and
  :execute-fn (merged params x out-dir -> a Result, driving whatever
  engine seam that kind needs -- corpus.generate's own two-step engine
  for :synthea, ehrt.corpus.sim-adapter's subprocess adapter for :sim,
  SS-2 Step 3).

  This namespace owns the registry and per-kind param resolution only;
  ehrt.corpus.generator-source (SS-2 Step 2) owns the
  unification (execute, verify, wrap as a dir Source) that actually
  CALLS an entry's :out-dir-fn/:execute-fn -- the same registry-vs-
  consumption split operators.clj draws against corpus.mutate."
  (:require [clojure.java.io :as io]
            [malli.core :as m]
            [ehrt.corpus.generate :as generate]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus.sim-adapter :as sim]))

(def GeneratorEntry
  [:map
   [:kind :keyword]
   [:default-params [:map-of :keyword :any]]
   [:params-schema :any]
   [:out-dir-fn [:fn fn?]]
   [:execute-fn [:fn fn?]]])

(defonce ^:private registry (atom {}))

(defn register!
  "Registers a generator entry, keyed by :kind. Returns kernel/ok
  {:kind} or kernel/rejected :invalid-generator-entry."
  [entry]
  (if (m/validate GeneratorEntry entry)
    (do (swap! registry assoc (:kind entry) entry)
        (kernel/ok (select-keys entry [:kind])))
    (kernel/rejected :invalid-generator-entry {:entry entry})))

(defn lookup
  [kind]
  (get @registry kind))

(defn entries
  []
  (vals @registry))

(defn registry-snapshot
  "Test/dev support: the full registry map, keyed by :kind -- for
  saving and later restoring exact state, same convention as
  ehrt.kernel.canonical/corpus.operators."
  []
  @registry)

(defn reset-registry!
  ([] (reset-registry! {}))
  ([snapshot] (reset! registry snapshot)))

(defn resolve-params
  "Merges params onto kind's own pinned :default-params (D8 -- a
  param a caller omits falls back to a pinned constant, never the
  clock/environment/machine), then validates the merged map against
  the registered :params-schema. Returns kernel/ok the merged params,
  or kernel/rejected :unknown-generator-kind (naming every registered
  kind, DOC-1's enumerable-options convention) / :invalid-generator-
  params (malli's own explain)."
  [kind params]
  (if-let [entry (lookup kind)]
    (let [merged (merge (:default-params entry) params)]
      (if (m/validate (:params-schema entry) merged)
        (kernel/ok merged)
        (kernel/rejected :invalid-generator-params
                          {:kind kind :params merged
                           :explain (m/explain (:params-schema entry) merged)})))
    (kernel/rejected :unknown-generator-kind
                      {:kind kind :valid-options (sort (map :kind (entries)))})))

;; ---- seed catalog: synthea, re-expressed over corpus.generate's own
;; two-step engine (D7). Every default below is corpus.generate's OWN
;; pinned var, imported directly -- never a re-typed copy that could
;; silently drift -- so the zero-param `synthea:` URL means exactly
;; what zero-flag `ehr corpus generate` means (D9). ----

(def synthea-params-schema
  [:map
   [:seed {:optional true} :int]
   [:clinician-seed {:optional true} :int]
   [:population {:optional true} :int]
   [:reference-date {:optional true} :string]
   [:config-path {:optional true} :string]])

(register!
 {:kind :synthea
  :default-params {:seed generate/default-seed
                    :population generate/default-population
                    :reference-date generate/default-reference-date
                    :config-path generate/default-config-path}
  :params-schema synthea-params-schema
  :out-dir-fn (fn [{:keys [seed population]}] (generate/default-out-dir seed population))
  :execute-fn (fn [params out-dir] (generate/generate! (assoc params :out-dir out-dir)))})

;; ---- seed catalog: sim, over ehrt.corpus.sim-adapter's own subprocess
;; adapter (Step 3, D7). :patients/:emit get their own pinned defaults
;; here (sim's own CLI has no zero-flag default for either that would
;; produce a v2 corpus by itself -- :emit in particular defaults to
;; nothing upstream, which would produce zero messages) so that a
;; zero-param `sim:` URL still means something: one patient, HL7v2
;; messages emitted. :seed reuses generate/default-seed (the SAME
;; pinned value synthea's own zero-param URL uses) for one shared
;; convention across every registered generator, not a second,
;; independently-chosen constant. ----

(def sim-params-schema
  [:map
   [:seed {:optional true} :int]
   [:patients {:optional true} :int]
   [:churn {:optional true} :boolean]
   [:emit {:optional true} :string]
   [:reference-date {:optional true} :string]
   [:config {:optional true} :string]])

(defn- fan-out-dir
  "One subscriber's own spool directory: `<out-dir>/fan-out/<name>`.

  UNDER THE CORPUS ROOT, per ADR-0175 section 2(f), and one level
  deeper than that section's own sketch for a MEASURED reason. Every
  reader of a generated corpus in this repository -- `ehrt play`,
  `ehrt corpus batch`, `gate v2` -- takes only the candidate files
  sitting DIRECTLY under the path it is given
  (`cli.core/gate-candidate-files-in`, `result/list-files`), so a
  subscriber spool in a subdirectory is invisible to all three and no
  base corpus double-counts. `ehrt.corpus.intake/source-files` is the
  one recursive reader (`file-seq`, for foreign corpora that nest), and
  a single `fan-out/` parent is what makes its extra rows obviously
  derived rather than scattered among the corpus's own msg-*.hl7 files."
  [out-dir subscriber-name]
  (io/file out-dir "fan-out" (name subscriber-name)))

(defn- spool-fan-out!
  "Writes each subscriber's own spool from `run-command`'s already-
  planned `:fan-out` (ehrt.sim-emit-hl7.fan-out/plan). This function
  FILTERS NOTHING and PARSES NOTHING -- the plan is the decision, and
  the decision was made where the message vector lives.

  Per subscriber, three things:

    msg-NNN.hl7  one file per message, width-padded to THAT
                 subscriber's own count (the same `max 3` rule the base
                 spool uses, so a subscriber under 1,000 messages is
                 named exactly as the base spool would name it)
    INDEX.edn    {:name :count :base-indices [...] :filter :msh}
    DIGEST.edn   {:name :count :sha256 <of the messages, in order,
                 concatenated with no separator>}

  `:base-indices` IS THE SUBSEQUENCE, written down: a consumer holding
  the base spool and this file can say which base message each
  subscriber file came from. That is the whole content of the author's
  own 2026-08-28 ruling (collision option (b)) -- identity is the log
  index, never MSH-10, which `control-id-for` is known non-injective
  over (`roadmap.md#oru-control-id-collision`).

  Both sidecars end in `.edn`, so neither is a gate/play/batch
  candidate even for a reader pointed straight at a subscriber
  directory."
  [fan-out out-dir]
  (doseq [{:keys [indices messages] :as subscriber} fan-out]
    (let [dir (fan-out-dir out-dir (:name subscriber))]
      (kernel/mkdirs! dir)
      (let [width (max 3 (count (str (max 0 (dec (count messages))))))
            fmt (str "msg-%0" width "d.hl7")]
        (dorun (map-indexed (fn [i m] (spit (io/file dir (format fmt i)) m)) messages)))
      (spit (io/file dir "INDEX.edn")
            (pr-str (-> (select-keys subscriber [:name :filter :msh])
                        (assoc :count (count messages) :base-indices indices))))
      (spit (io/file dir "DIGEST.edn")
            (pr-str {:name (:name subscriber)
                     :count (count messages)
                     :sha256 (kernel/sha256-string (apply str messages))})))))

(defn- spool-sim-output!
  "Writes sim's own run! payload to out-dir: one .hl7 file per message
  (:messages), sim's own :manifest verbatim as manifest.edn -- this
  repo writes no manifest of its own for sim output; provenance is the
  generator's word (ruling 4, docs/source-sink-design.md D7) -- and,
  when the payload carries :ground-truth (Q2 a., ADR-0100), the run's
  own ground-truth vector, pr-str'd, as events.edn: byte-identical to
  `ehrt sim run --format ground-truth`'s own bare stdout
  (bases/cli/core.clj's sim-ground-truth-bare-text), since both are
  exactly `(pr-str ground-truth)`. events.edn is DATA, not provenance
  -- it doesn't reopen ruling 4's manifest.edn scope. Byte-identity
  with the bare-stdout format IS the test: it is what makes
  `cat events.edn | ehrt sim check` an actual working pipe with zero
  check-side code, the same property `ehrt sim run --format
  ground-truth | ehrt sim check` already has. Returns kernel/error
  :sim-produced-no-messages, writing NOTHING (not even events.edn),
  when the run's own payload carried no messages at all -- an
  all-metadata directory (manifest.edn alone) would defeat
  generator-source/resolve!'s own generic empty-output check, since
  manifest.edn alone makes the directory non-empty. events.edn only
  ever spools alongside a non-empty :messages set (the hl7 path,
  ADR-0100's own fence) -- a fhir/none-emit run still errors here
  exactly as before, since it never reaches this branch."
  [{:keys [messages manifest ground-truth fan-out]} out-dir]
  (if (empty? messages)
    (kernel/error :sim-produced-no-messages
                  {:hint (str "sim's own run produced no messages -- :emit \"hl7\" "
                              "(this entry's own pinned default) is required to produce a v2 corpus")})
    (do
      (kernel/mkdirs! (io/file out-dir))
      ;; ARC 4 SWEEP 2 (2026-08-28): the index is padded to the width
      ;; THIS corpus needs, never to a fixed three. `ehrt.corpus.intake`
      ;; walks a spooled directory `sorted-by-path`, and
      ;; `ehrt.corpus.player`'s own docstring says order is a semantic
      ;; property of the input -- so at 1,000 messages a fixed `%03d`
      ;; made `msg-1000.hl7` sort between `msg-100.hl7` and
      ;; `msg-101.hl7` and the corpus replayed SCRAMBLED. Nothing in
      ;; this repository had ever emitted 1,000 messages from one run
      ;; until arc 4's emission add-ons; the ed-tuesday demo went 782 ->
      ;; 1,447 and the defect surfaced as a bed board showing patients
      ;; admitted years after their own discharge.
      ;;
      ;; `(max 3 ...)` keeps every corpus under 1,000 messages
      ;; BYTE-IDENTICAL, filename for filename, which is why this is a
      ;; fix and not a migration.
      (let [width (max 3 (count (str (dec (count messages)))))
            fmt (str "msg-%0" width "d.hl7")]
        (dorun (map-indexed (fn [i m] (spit (io/file out-dir (format fmt i)) m)) messages)))
      (spit (io/file out-dir "manifest.edn") (pr-str manifest))
      (when ground-truth
        (spit (io/file out-dir "events.edn") (pr-str ground-truth)))
      ;; ARC 4 SWEEP 5 (ADR-0175 design (f)): the subscriber spools,
      ;; written from the plan the run already carries. Absent `:fan-out`
      ;; -- every corpus this project shipped before this sweep -- not one
      ;; byte of this function's output moves, because this line does not
      ;; execute.
      (when (seq fan-out)
        (spool-fan-out! fan-out out-dir))
      (kernel/ok {:out-dir out-dir}))))

(register!
 {:kind :sim
  :default-params {:seed generate/default-seed :patients 1 :emit "hl7"}
  :params-schema sim-params-schema
  :out-dir-fn (fn [{:keys [seed patients]}] (str "out/corpus/sim-s" seed "-p" patients))
  :execute-fn (fn [params out-dir]
                (let [run-result (sim/run! params)]
                  (if-not (kernel/ok? run-result)
                    run-result
                    (spool-sim-output! (:payload run-result) out-dir))))})

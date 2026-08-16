# Archived prompt: event-log-contract-arc (2026-08-16)

Verbatim, as received. Landed as `notes/adr/0141-event-log-contract.md`.

The two design questions this prompt deliberately left unruled (Q-A
stability tier, Q-B artifact shape) were answered mid-session, after
Step 1's census, together with four further rulings. All of them are
recorded verbatim in `.agents/rulings.md` under "From ADR-0141" and are
reproduced at the end of this file.

---

SESSION PROMPT — event-log contract arc (EDN primary): schema, formats.md section, custom-emitter use case

Drafted by the design channel, 2026-08-16, against a fresh public clone at `24f351d` (fence-battery fixes landed, ADR-0140; tree clean; both standing tags verified). Author-ordered before the latency-realism arc ("Choose a."), so that arc lands against a pinned contract.

Why this arc exists (author's charter, restated)

A consumer wants to generate simulated hospital traffic with `ehr-testing-tools` and translate it, with their own code, into proprietary formats we cannot know ahead of time. The traffic must therefore be available in the richest semantic form we have — the ground-truth event log — encoded as EDN (JSON derived later). The mechanism already exists and is load-bearing: both built-in emitters take `ground-truth` as their first argument (`sim-emit-hl7/emit`, `sim-emit-fhir/bundle-run`); `ehrt sim run --format ground-truth` emits the bare EDN vector; `corpus generate sim` persists it as `events.edn` byte-identical (ADR-0100); `sim check` and `play` already consume it. What is missing is the CONTRACT: no executable schema for the event (`sim-engine` has malli schemas for `PatientState` and the record types — the fold results — but not for the event), and no consumer-facing shape document (`docs/formats.md` has no event-log section; `event-sourcing.md` gives the why, not the shape). A consumer today reverse-engineers the event shape from `emit_hl7.clj` — reading our HL7 emitter to write a not-HL7 emitter — which makes our emitter's field choices the de facto contract and makes schema change indistinguishable from schema break.

Author rulings, verbatim

* "Ok, add it, and make EDN be primary. JSON can be derived later. This will be a priority after the immediate review is done."
* "Choose a." — this arc runs BEFORE latency realism.
* Two design questions below (Q-A stability tier, Q-B schema artifact shape) are NOT pre-ruled: Step 1 STOPs with the options and the tree's evidence. The channel's recommendations are stated at each; the author rules.

Channel evidence, from the tree (verify; do not carry forward)

* The event discriminator key is `:event`, not `:type` (`engine.clj:358` `{:event :registered :t t …}`, `:387` `{:event :admission …}`, `:409` `{:event :transfer …}`). The channel's earlier chat framing said `:type` — that was wrong; this prompt corrects it. Grep the tree for the vocabulary; do not take this list as closed: a floor of ~21 event kinds across `sim-engine` and `sim-trajectory` sources (`:admission :bed-swap :cancel-admit :cancel-discharge :cancel-transfer :care-plan-end :care-plan-start :diagnostic-report :discharge :medication-end :medication-order :merge :observation :order-placed :outpatient-visit :outpatient-visit-end :procedure :registered :result-available :step-rejected :transfer`; one grep false-hit `:event :patient-id` is a construction site, not a kind).
* Common keys observed: `:event`, `:t`, `:active-mrn`; kind- specific keys vary (`:reason :attending :from :codes :observations :before :after :world-before :world-after …`). The per-kind required/optional split must be DERIVED from what the engine emits over the scenario corpora, not designed.
* The run envelope (`run.clj:409-416`): `{:ground-truth [...] :manifest {…seed, engine-params, config sha, invocation…} :summary {:patients n :events n}}` (+ `:messages` when `--emit hl7`). `--format ground-truth` emits ONLY the vector.
* `sim-emit-hl7` and `sim-emit-fhir` are the first consumers; `sim-check` and `play` are the second and third. Their reads of event keys are the de facto contract this arc makes explicit.

Read first

* `components/sim-engine/src/ehrt/sim_engine/engine.clj` (constructors, `decide`/`evolve`, existing malli schemas :89-205)
* `components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj` (the trajectory-side event kinds — outpatient, medication, care-plan, procedure, diagnostic-report)
* `components/sim-emit-hl7/src/…/emit_hl7.clj` and `components/sim-emit-fhir/src/…/emit_fhir.clj` (which keys each emitter READS per kind — the de facto contract)
* `components/sim-check/src/…` (`sim check`'s reads)
* `docs/formats.md` (the home; match its section conventions — the lineage record and check report sections are the models), `components/sim/docs/event-sourcing.md`, `docs/patient-state-model.md`
* `components/docs-tooling/resources/docs-tooling/exercised-sources.edn`
   * one existing `bin/usecase-*` exerciser (the pattern the new use-case page's exerciser follows)
* ADR-0100 (events.edn byte-identity), the founding-thesis property test (`emit_fhir_test.clj:147`, naturality at 150 trials)

Step 0 — Preflight, deferred tag
`bin/preflight` plain; verify both standing tags. Pay the fence-battery micro-session's deferred close tag under the standing conditional license IF the author-side CI relay is present in this prompt's context — it is NOT: STOP-AND-REPORT the tag as pending and PROCEED (the ADR-0139/0140 pattern: create annotated without `--push`, hold, continue; the arc's own work does not depend on it).

Step 1 — Probe, then STOP with the two design questions

1. Derive the event vocabulary and per-kind key population from the tree, not from this prompt: run the two demo scenarios (`ed-tuesday`, `clinic-decade`, cleared `out/`) and the `demos/traces/*` configs, collect every event, and tabulate per `:event` kind: total count, keys always present, keys sometimes present, value shapes (keyword / string / long / instant / nested map / vector-of). Cross-check the tabulation against the constructors (a kind the corpora never produce but the engine can — `:step-rejected`, `:cancel-*` — must come from the source). Land this as `.agents/plans/<date>-event-log-census.md` — the arc's Step-1 evidence, the same discipline as the fence census.
2. Cross-check against the consumers: for each of the four built-in consumers, which keys does it READ per kind? Any key a consumer reads that the census never observed is a finding (dead read or untested path); any key the census observes that no consumer reads is a note (present but unconsumed — still part of the contract if it carries meaning).
3. STOP-AND-REPORT with the census and these two questions:

Q-A — stability tier of the event contract. (a) Public, versioned: the schema carries a version; the run envelope's `:manifest` gains `:event-schema-version`; a deprecation policy is stated in formats.md (a key or kind is marked deprecated for one minor before removal; additive change is non-breaking; the schema's own test enforces that a bump accompanies any non-additive diff against the committed schema). (b) Best-effort: schema is documentation, no version, no promise. Channel recommends (a) — the whole point of the arc is that a proprietary consumer can build against it; a contract with no stability tier is a description, not a contract. Cost is one manifest key and one test.

Q-B — the schema's artifact shape. (a) Malli schema defined in `sim-engine` Clojure source (`Event` as `[:multi {:dispatch :event} …]`), AND exported as a committed EDN artifact (`components/sim-engine/resources/sim-engine/event-schema.edn`) with a parity test that the export equals the source-of-truth — so the contract is data a non-Clojure consumer can read, and JSON later is a projection of that EDN with stated conventions. (b) Clojure source only; formats.md prose is the consumer's contract. Channel recommends (a) — it is what "EDN primary, JSON derived later" means mechanically, and it costs one resource file plus one parity test.

Wait for rulings. Do not write the schema until they arrive.

Step 2 — The schema, red-first (after rulings)

1. `Event` malli schema in `sim-engine`: closed enum of kinds (exactly the census + source-derived set; STOP if the two disagree in a way the session cannot explain), per-kind required/optional keys from the census, value schemas from observed shapes, `:t` monotone within a run stated as a RUN-level property (not a per-event schema constraint). Common keys factored once.
2. Red-first, two tests: (i) a property test that every event in the two scenario corpora plus every `decide` output over `test.check`-generated worlds validates against `Event` — write the schema deliberately incomplete first (one kind missing) to witness RED, then complete it: GREEN. (ii) Consumer conformance: both built-in emitters and `sim-check` validate their INPUT against `Event` in their tests (not in production code — no runtime cost added), which converts them from de facto to first consumers of the explicit contract.
3. Per Q-B (a): export the schema to the committed EDN resource; parity test. Per Q-A (a): `:event-schema-version "1.0.0"` in the manifest; the non-additive-diff-requires-bump test.
4. Commit: `feat: Event schema -- the ground-truth event log's contract, derived from the tree, red-first; both emitters and sim-check validate against it in tests (event-log-contract arc, Q-A/Q-B as ruled)`.

Step 3 — `docs/formats.md` "The event log", GENERATED
Do not hand-write the shape. Add a docsgen target that renders the section FROM the schema (per kind: meaning — one sentence, authored in the schema's own docstring/`:doc` property so it lives with the data; keys with required/optional and value shape; one real example event lifted from the census; which state transition it drives). Plus authored envelope prose: what `--format ground-truth` emits vs the full envelope; ordering guarantee; EDN conventions (keywords, instants as `#inst`, sets); the stability policy per Q-A; and one paragraph "JSON: derived from the EDN by these rules — `:event :admission` → `"event": "admission"`, instants as ISO-8601 (or epoch-ms; state which, matching what `--json` already does — verify, do not assume)". Fold the target into `docsgen`; CI freshness diffs it (ADR-0136's mechanism, so this new derived surface is born inside the gate). Commit: `docs: formats.md gains "The event log", generated from the Event schema; docsgen + CI freshness cover it`.

Step 4 — The use-case page: "Custom emitter from the event log"
`docs/use-cases/custom-emitter-from-the-event-log.md`, via `components/corpus/docs/use-cases.edn` (the generator's source, not a hand file — the pages are generated). Its strip: `bin/ehrt sim run --seed … --format ground-truth > out/…/events.edn` then a tiny reference custom emitter — `bin/example-custom-emitter`, ~30 lines of Clojure (`clojure -M …` or a babashka-free `clj` one-liner file) that reads the EDN and produces a deliberately trivial proprietary format (e.g. one pipe-delimited line per admission/discharge) — proving the seam end to end. Register it in `exercised-sources.edn` with a `bin/usecase-custom-emitter` exerciser following the existing pattern, so the page is exercised from birth (R-F8's proposed reader-path rule, satisfied by construction). The palgebra signature line for the page: `event-log × custom-emitter → proprietary-datum [Emit]` — its diagram renders result nodes (ADR-0135) automatically. Commit: `docs: use case -- custom emitter from the event log, exercised from birth`.

Step 5 — Records and close
ADR (next free number) with the census, the two rulings verbatim, red/green witnesses, and the contract's stated stability policy; roadmap: the arc row → CLOSED, latency-realism row annotated "lands against Event schema vN"; rulings rows; session record; prompt archive. Full `make test` unpiped, `MAKE_EXIT` captured — predict blocks before running (new test namespaces likely: schema property test, consumer conformance, parity, docsgen; count per project context). Push; `bin/post-push-verify`. Tag deferred to the next Step 0.

Fences

* New: `Event` schema + tests in `sim-engine`; the EDN export resource; consumer-conformance TESTS in sim-emit-hl7, sim-emit-fhir, sim-check (no production-code changes in any consumer); the docsgen renderer + Makefile/CI target; the use-case entry in `use-cases.edn` + exerciser + example emitter; formats.md's generated section + its authored envelope prose; the census plan file; close artifacts.
* The event log's SHAPE does not change in this arc: the schema DESCRIBES what the tree produces. If the census reveals a shape defect (an inconsistent key across kinds, a value that should be an instant but is a string), that is a REGISTER ROW for a follow-on, not a fix here — describing the current truth first, then changing it under the versioned contract, is the whole point of Q-A.
* Zero engine `decide`/`evolve` changes; zero emitter production changes; vendored bytes verbatim; `docs/notation.md` untouched.
* STOP-AND-REPORT: census vs source vocabulary disagreeing unexplainably; a consumer reading a key the census never sees; the docsgen renderer needing schema information the schema cannot carry (design gap — report, do not hack); the use-case page's exerciser unable to run offline; fence pressure.

---

## Rulings received mid-session, verbatim

After Step 1's STOP:

> "Q-A a. Q-B b→a (a). Promote the tabulator to bin/event-census,
> author-licensed fence widening. Nested-:event collision: describe in
> schema (separate fact schemas) and lead the formats.md prose with the
> warning; no rename this arc. S-1..S-5 and the Z-segment asymmetry
> stay register rows. Proceed to Step 2."

After Step 2 was verified from a fresh clone:

> "Step 2 verified and accepted from fresh clone: export is real EDN (0
> #"…" literals, [:re …] normalized), all 21 kinds declared,
> baseline/current differ only by the FROZEN banner (correct at v1.0.0
> birth). Two-artifact gate stands; the ObservationEntry export is
> accepted as-is. Proceed to Step 3 as ruled — the generated formats.md
> section led by the nested-:event warning, rendered from the schema's
> :doc/:transition properties, folded into docsgen and CI freshness.
> Add one sentence the ruling didn't carry: the [:re …] pattern dialect
> is java.util.regex. Then Step 4 (the use-case page, exercised from
> birth) and Step 5 close."

## Deviations from this prompt, all disclosed

- **Step 3's "instants as `#inst`" and the ISO-8601-vs-epoch-ms
  paragraph do not apply.** Verified, not assumed: an event log
  contains **no instants at all** — zero `#inst`, and `:t` is an
  integer of seconds since run start. The page states that instead of
  inventing a rule.
- **ADR-0100's byte-identity, as this prompt restates it, is not quite
  right.** `events.edn` and a redirected `--format ground-truth` differ
  by one trailing newline; ADR-0100's own test compares against
  `sim-run-command`'s internal `:bare-text`, not CLI stdout. The page
  says so precisely.
- **`bin/example-custom-emitter` is ~40 lines, not ~30**, because it
  also reports what it did *not* translate.
- **Two fence widenings**, both author-accepted: `bin/event-census`
  (licensed by ruling) and a one-line `ObservationEntry` export on
  `sim-model`'s interface.

# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (in progress)
- Nothing in progress at end of session (sim split B M3 landed same
  day it started -- see Done, below. GMF parity arc stays COMPLETE,
  unaffected by this front). `.agents/plans/2026-08-04-sim-split-b-
  plan.md`'s own forced sequence (AR-6: M1 → M2 → M3 → M4) has M4
  (`sim-check` + residual thinning) next, its own session, prompt
  authored in the design channel once scheduled -- not yet in
  progress.

## Next (backlog, no session scheduled)
- The lookup-column `time` gap (named in the schema-invalid family
  backlog since ADR-0039, still untouched — Wave I's own six
  mechanisms didn't cover it). Bulk vendoring (batched by closure
  family) follows once the catalog fully walks.
- **Wave G attachment deferral** (ADR-0037 AR-4, named trigger "multi-
  module assignment per patient"): upstream's own all-waiting-modules-
  attach-to-one-visit semantics only diverges from this project's
  per-module wait when one patient runs multiple modules concurrently —
  the engine's current one-module-per-patient assignment never
  exercises this, so it is deferred, not built. Revisit trigger: a
  future session that assigns more than one module to the same patient.
- Pairing-as-data (review P3-3): mutate↔judge conviction registry — design pass in
  the design channel first; vocabulary is load-bearing
- Storefront demo fixture: minimal clean-gating FHIR fixture so the README's mutate
  demo shows a real accepted→rejected flip (2026-08-01 capture session finding)
- make quickstart → nightly integration workflow + single-```sh-fence guard in README
  (quickstart_fresh docstring corrected in same change)
- generator-source three-concerns split (ADR-0017 named-future)
- ehrt.corpus.display placement — presentation-leaning (ADR-0018 named-future)
- Markdown-table helper dedup (ADR-0018 named-future)

## Externals (author-only)
- NIST licensing inquiry: send the drafted gist (retires the confirmation-pending
  posture cited on the storefront Gate row)
- IG pinning: choose and commit the profile-tier conformance target (Gate row's
  other caveat)
- Clojars publish, when satisfied with the product (ruled 2026-07-31; ends the
  greenfield era — output formats freeze harder after first tag). **Dated note
  (D1a rider, 2026-08-02): this row IS the Clojars-vs-Maven-Central ruling —
  cross-referenced into `notes/ADRs.md` ADR-0001's own H5 entry today, closing
  that half of H5 as an open gate; the group/coordinates naming half and
  publication itself both stay open/parked, unchanged by this note.**
- SETUP rewalk by an unspoiled human reader (F3 superseded-pending-rewalk)
- Upstream the adapted repo-adaptation skill to pragsmike/skills (and cyberneutics
  if wanted) — AUTHOR ACTION named 2026-08-01
- Item 9 (ADR-0024, landed 2026-08-01 as mirror-with-gate, not symlinks): the
  fresh-session discovery probe is DONE — see Done section below. The
  "fast-forward /mnt/c" remainder is SUPERSEDED (2026-08-02, post-Wave-D
  cleanup): /mnt/c is now kept read-only and synced only via
  `bin/sync-mnt-c`, see the dual-clone-guardrails Done entry below.
- Post-Wave-D cleanup (2026-08-02, ADR-0030 J4): does the `/mnt/c`
  clone still earn its keep at all, now that it's read-only and
  synced only via `bin/sync-mnt-c`, or should it be removed outright?
  The guardrails are sound either way — this is a standing-cost
  question (a second checkout to keep in sync, however mechanically),
  not a correctness one. Named, not decided, this session (J4's own
  explicit fence) — AUTHOR ACTION.

## Deferred (explicitly, with revisit triggers)
- **Carry-across emission** (2026-08-04, `notes/ADRs.md` ADR-0042
  AR-2): a straddling encounter (opens history, closes horizon) yields
  NO in-window wire traffic for that patient under Wave H's own pre-
  roll — real hospital censuses DO show patients mid-stay at window
  open, but building that emission is out of this session's own scope.
  Revisit trigger: a test scenario needs mid-stay-at-window-open
  realism.
- **Wellness cadence chronic-meds cap** (2026-08-03, `notes/ADRs.md`
  ADR-0037 AR-1): `EncounterModule.recommendedTimeBetweenWellnessVisits`'s
  own chronic-medications annual cap ("if hasChronicMeds && interval >
  1 year, interval = 1 year", lines 209-211 at the pin) is EXCLUDED from
  `next-wellness-tick` by ruling, not omitted by oversight —
  `active-chronic-medications` exists in this project's own persona/
  attribute model with no input cascade, so wiring the cap in is a
  register item, not a design question. Revisit trigger: a future
  session ranking calibration fidelity for the chronic cluster, or a
  finding that the cap's absence materially skews a census/corpus
  result.
- **Backload named future** (2026-08-03, `notes/ADRs.md` ADR-0031 AR-3):
  pre-roll stays emit-nothing, reaffirmed — no backloaded-history mode
  in the sim. The backload need (pre-window messages for systems that
  ingest historical loads) is a TOOLS-SIDE construction over sim
  output, fault-injection's own sibling, not a sim feature. Revisit
  trigger: a real consumer for pre-window messages appears.
- P2-5 intake staging-dir behavior (deferred 2026-07-31)
- Reading-set budget numbers (charter §6: rule after real sizes are measured)
- Verdict-cache placement revisit (ADR-0011 note: second consumer, or never)
- Sim-manifest interop design between sim and corpus (pre-review open
  thread). **RESOLVED (2026-08-04, sim split B M1, `notes/ADRs.md`
  ADR-0043):** `components/provenance` is the interop design —
  ManifestV0/V1/V1_1 + validators moved to the single acyclic home
  both corpus and sim depend on; the sim manifest mirror
  (`MirroredManifest`) retired with it. See Done, below.
- Sim split S4 (`sim-engine`: `engine`, `churn`, `order-profiles`) —
  trigger: a second `engine` consumer appears (the FHIR emitter is the
  likely one) or engine work itself needs the emit-state/check boundary
  designed, same plan. **Dated note (2026-08-04, sim split B M1
  session, AR-M1-5 / plan AR-4, framing (b)):** superseded-by-citation,
  not fired — `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED)
  proceeds with S4's scope as M2 (`sim-engine`) ahead of this trigger
  firing, author override plainly stated; the trigger's own reasoning
  is honored in substance (M3/`sim-emit-fhir` is committed scope in the
  same sequence, so M2 is designed against two known consumers) — not
  claimed that the trigger fired. `.agents/plans/2026-08-02-sim-split-
  plan.md`'s own dated status note carries the same ruling; `notes/
  ADRs.md` ADR-0043 records it verbatim. **EXECUTED (2026-08-04, sim
  split B M2, same day):** `components/sim-engine` landed for real —
  see Done, above. **AR-4 DISCHARGED (2026-08-04, sim split B M3, same
  day, AR-M3-5):** `components/sim-emit-fhir` landed for real —
  `sim-engine`'s boundary now serves two shipping consumers with
  distinct surfaces (`sim-emit-hl7` reads the event log, `sim-emit-fhir`
  reads folded state via `engine/replay`), discharging in substance the
  "committed, not yet present" framing AR-4 stated at M2 time; see
  Done, below.
- `ImagingStudy` (R5, CHF trigger) and the stroke-risk data source (R7)
  — GMF coverage Wave D closed 2026-08-02 (D0-D3, see Done below)
  without owning either; H3's own attribute-weighted `distributed_
  transition` mechanism landed D3 but is only half of stroke's own
  revisit trigger (`stroke.json` stays deferred). **Dated
  cross-reference (2026-08-03, ADR-0031):** the stroke-risk DATA-SOURCE
  question is RULED — `.agents/plans/2026-08-02-gmf-parity-plan.md` §2
  (the risk-attribute register, curated calibration content rather than
  a ported calculation). This row's remaining substance is Wave E
  scheduling (stroke as the register's first consumer), not an open
  design question.
- `myocardial_infarction.json` — the three independent blockers this
  row originally named (`ImagingStudy`/R5, `SupplyList`, `Counter`) are
  ALL now built (GMF coverage Wave F, ADR-0036) — this row's own
  original claim is stale, corrected here rather than left to drift.
  The Wave F census re-run (`docs/gmf-interpreter.md` §15) traced it
  directly: `ImagingStudy` was never the module's ONLY gap — it surfaced
  an unrecognized lookup-table column, `state` (H2's own
  `recognized-lookup-table-columns` boundary), a pre-existing,
  unrelated gap Wave F did not touch. **RESOLVED 2026-08-03 (GMF
  coverage Wave LC, ADR-0038):** the whitelist itself retired; this
  module now censuses `:ok-walked` — see Done, below.
- **Census tool refinements** (ADR-0035/ADR-0036's own disclosed, not-
  fixed findings, `ehrt.sim-trajectory.census`): (a) no substance
  qualifier on a `:ok-walked` verdict — a module that produces zero
  trajectory events on every seed censuses identically to one with rich
  content (`docs/gmf-interpreter.md` §15's own AR-8b substance note: 26
  of 42 pre-Wave-F `:ok-walked` modules produce zero events on every
  seed); (b) no per-module census-seed override (every module shares
  the SAME global seed count); (c) the artifact filename has no same-
  calendar-day disambiguation (worked around by hand-appending a wave
  suffix in both the F0 and F re-runs, not fixed in the tool itself).
  Revisit trigger: whichever future session next re-runs the census and
  hits the filename collision again, or needs to distinguish "walks but
  produces nothing" from "walks and produces real content" for ranking
  purposes.
- UTI's own `ed_bundle.json` O2-saturation Observation states carry a
  `gmf_version 2` `:distribution` this loader has NEVER normalized
  (Observation is not one of ADR-0035's three ported contexts) — a
  stray, still-raw, string-keyed field `emit-and-advance`'s own
  `(= :procedure (:type state))` gate correctly ignores (ADR-0035's own
  execution note, Step 2's "real bug found and fixed mid-step"). The
  raw field itself stays unnormalized, disclosed, not built — revisit
  trigger: a future session that needs Observation's own v2 timing/
  value distributions for real (no vendored-corpus module currently
  reads the sampled value back).
- **Vital-sign channel** (ADR-0036 AR-7, GMF coverage Wave F's own
  explicit deferral): the `VitalSign` STATE type and the `:vital-sign`
  CONDITION type both require a vital-sign REGISTER with baseline
  values (State.java: Synthea's lifecycle engine sets these before any
  module runs) — engine-delegated content this project does not yet
  supply, authored calibration content pairing naturally with the
  re-scoped Wave E (risk-attribute register, above). Blocks
  `congestive_heart_failure`/`contraceptives`/`covid19` directly
  (census-confirmed). Revisit trigger: Wave E's own design session, or
  whichever session first needs a real vital-sign baseline.
- **Lookup-table columns `race`/`time`** (ADR-0036 AR-7, deferred to
  Wave I): `acute-myeloid-leukemia` (`race`), `hiv-diagnosis` (`time`),
  plus the seven modules Wave F's own census re-run newly surfaced
  behind `ImagingStudy` (`diabetic_retinopathy_stage`, `state`,
  `operative_status`, `cardiac_surgery`, `vhd_mr_risk`, `vhd_ps_risk`,
  `vhd_tr_risk`, `docs/gmf-interpreter.md` §15's own AR-8 trace) —
  `race` shares this wave's own persona-race prerequisite (`ehrt.sim-
  model.persona`'s new optional `:race` field, ADR-0036 AR-4/AR-5).
  H2's own `recognized-lookup-table-columns` boundary. **RESOLVED
  2026-08-03 (GMF coverage Wave LC, ADR-0038), pulled forward from
  Wave I:** the boundary itself retired (generalized to attribute
  resolution, not a bigger whitelist) — all 9 modules move to
  `:ok-walked` — see Done, below.
- `Active CarePlan` (condition type) — design-ruled, implementation-
  deferred (Wave D stage D2's own G2): no vendored module exercises it
  yet; build fresh against the first real candidate's own usage.
- `ehrt.sim-trajectory.gmf-interpreter/resolve-time-advance`'s own
  Procedure-duration gap: `:duration` is passed as a flat map but
  `resolve-time-advance` destructures nested `:range`/`:exact` keys
  from it, finding neither — EVERY vendored Procedure state's own
  duration silently never advances virtual time, v1 or v2 gmf_version
  alike (found live, Wave D stage D3, `docs/gmf-interpreter.md` §14's
  own D3c finding 1). **FIXED (2026-08-03, ADR-0031 AR-6's first
  defect-fix session, `notes/ADRs.md` ADR-0032) — see Done, below.**
- The compile-trajectory/engine/emit full-pipeline gap for closure-
  having modules — **UPGRADED from "unproven" to "confirmed broken"
  (2026-08-02, post-Wave-D cleanup, ADR-0030 J3):** `engine.clj`'s own
  `:registered` decide method calls `run-module` at a bare arity that
  never threads a closure's own submodule registry through
  (`ear_infections`/`urinary_tract_infections`: any walk reaching a
  `CallSubmodule` state throws) nor an `initial-attributes` seed
  (`total_joint_replacement`: the walk blocks permanently at age 0,
  silently producing zero compiled content). Pinned live by
  `components/sim-emit-hl7/test/`'s three new round-trip tests, one
  per root. The session wiring `engine.clj` to carry a closure's own
  `modules`/`tables`/`initial-attributes` through to `run-module`
  inherits this gap AND must update those three tests (they are
  designed to fail loudly the moment this lands). **FIXED (2026-08-03,
  ADR-0031 AR-6's second defect-fix session, `notes/ADRs.md` ADR-0033)
  — see Done, below.**
- `bin/regression-oracle`'s own "always read `digest.clj` from the
  CURRENT checkout" design (ADR-0030 J2's own precedent) is incompatible
  with a session that changes the PRODUCER FUNCTIONS' own call shape
  mid-span — found live, ADR-0033's own execution note: ADR-0033 AR-2's
  hard `:modules` shape switch made `sinusitis`/`death-fixture`/`sepsis`'s
  own oracle producers call `gmf/singleton-closure` (absent before that
  session), a compile error against the BASELINE worktree, not a digest
  difference, when run through the script literally/unmodified. Worked
  around by hand that session (each commit's own `digest.clj` against its
  own worktree/classpath); not a fix this session (ADR-0034, GMF census)
  owns either — named here as a real, standing limitation of the oracle
  harness itself. Revisit trigger: a future defect-fix or refactor
  session that again changes an oracle-covered producer's own call
  shape should expect the same workaround, or this row graduates into
  an actual `bin/regression-oracle` enhancement (e.g. an opt-in "run
  each commit's own digest.clj against its own worktree" mode).

## Done (this session, 2026-08-04, sim split B M3 — `sim-emit-fhir` lands — ADR-0043)
- `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED, AR-1..AR-6) M3
  of four executed: `components/sim-emit-fhir` created, `emit_state.clj`
  (267 LOC) moved out of `components/sim` as `ehrt.sim-emit-fhir.
  emit-fhir` — the AR-3 rename, the one sanctioned improvement this
  stage licenses. `ehrt.sim-emit-fhir.interface` carries `bundle-run`
  only, the src-caller union confirmed by fresh call-position grep
  against `run.clj`/`identifiers.clj`; `snapshot-at` stays internal
  (no real external caller). `ff82bf0`.
- `org.clojure/data.json` moves to `components/sim-emit-fhir/deps.edn`
  (test-scope, its only real user); drops from `components/sim/deps.edn`
  in the same commit. Residual sim's two src-scope callers repoint to
  the interface; `identifiers_test.clj` (test-scope) repoints
  mechanically to `ehrt.sim-emit-fhir.emit-fhir` internals.
- Oracle script fix (AR-M3-4): `bin/regression-oracle`'s synthetic
  classpath heredoc learns `poly/sim-engine` (missing since M2 —
  digest.clj has required `ehrt.sim-engine.engine` directly since then,
  so normal-mode brackets could not resolve on any post-M2 ref).
  Red→green proven with the same-ref bracket (`c037f37 c037f37`):
  `FileNotFoundException` before, all eleven batches IDENTICAL after.
  `438d762`.
- Stale-path sweep: `ehrt.docs-tooling.stale-path-test`'s retired-
  namespace family gains `ehrt.sim.emit-state` (namespace and path
  form) — no real violations existed in the gate's own scan scope this
  time, so the new pattern clauses were proven red→green directly
  (temporarily removed, watched fail, restored, watched pass). Four
  live current-tense surfaces outside the gate's scan scope
  (`components/sim/docs/`) swept forward anyway: `sim-theory.md`,
  `sim-theory.edn`, `event-sourcing.md`, the emit-state demo's own
  README. `d5e4417`.
- AR-4 discharged (AR-M3-5, see the S4 row above): `sim-engine`'s
  boundary now serves two shipping consumers with distinct surfaces —
  `sim-emit-hl7` reads the event log, `sim-emit-fhir` reads folded
  state via `engine/replay` — the "committed, not yet present" framing
  AR-4 stated at M2 time discharges in substance.
- `clojure -M:poly check` clean and full suite green at every one of
  the three commits above (0 failures, 0 errors, both projects, 202
  Test-results blocks); normal-mode regression-oracle bracket / façade-
  seam / deftest-parity verification recorded in the session record.

## Done (this session, 2026-08-04, sim split B M2 — `sim-engine` lands — ADR-0043)
- `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED, AR-1..AR-6) M2
  of four executed: `components/sim-engine` created, `engine.clj`/
  `churn.clj`/`order_profiles.clj` (1,883 LOC) moved verbatim out of
  `components/sim`, `ehrt.sim-engine.interface` designed from fresh
  caller-evidence grep (both-direction deltas recorded in ADR-0043's
  own M2 section — `patient-id-for` has no real src-scope caller and
  stays OUT of the interface, contra the design-channel candidate
  list). `9ccc04f`, `701d0be`.
- `order-profiles.edn`'s resource moves with its loader (the one
  disclosed behavior-adjacent edit this stage licenses), load path
  updated. Residual sim's `run`/`check`/`emit-state`/`identifiers`
  repoint to the interface; test-scope callers (sim's own five test
  files, sim-emit-hl7's six vendored/replay tests, `bin/oracle-src/
  ehrt/oracle/digest.clj`) repoint mechanically to `ehrt.sim-engine.
  engine` internals.
- Stale-path sweep: two real violations in `docs/site-profiles.md`
  fixed forward, `ehrt.docs-tooling.stale-path-test`'s retired-
  namespace family gains `ehrt.sim.engine`/`churn`/`order-profiles`
  (namespace and path form), watched red→green live. `0543043`.
- ADR-0043's own M1-era "Dependency directions" note corrected by
  dated addendum, not rewrite: `sim-engine` depends on `sim-model` AND
  `sim-trajectory` (the engine's own `:registered` decide method calls
  `run-module`/`compile-trajectory`), never on `kernel` at all — the
  M1-time plan text guessed kernel; fresh M2 grep found otherwise.
- M1's three disclosed judgment calls (corpus relay design, the 9/6
  schema/builder test split, `valid?`'s retirement) ratified by the
  author, dated note appended to ADR-0043.
- `clojure -M:poly check` clean and full suite green at every one of
  the three commits above (0 failures, 0 errors, both projects);
  split-mode regression-oracle bracket / façade-seam / deftest-parity
  verification recorded in the session record.

## Done (this session, 2026-08-04, sim split B M1 — provenance lands, mirror retires — ADR-0043)
- `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED, AR-1..AR-6)
  M1 of four executed: `components/provenance` created
  (ManifestV0/V1/V1_1 + validators moved verbatim out of
  `corpus/manifest.clj`, exposed via `ehrt.provenance.interface`;
  builders stay producer-side, corpus's `build`/`build-v1-1` and sim's
  `build`). `83304c1`.
- `ehrt.corpus.manifest` repoints to provenance (relay, zero consumer
  churn — `generate.clj`/`intake.clj`/their tests needed no changes);
  `ehrt.corpus.interface`'s `ManifestV1_1` re-export repoints directly.
  `ab8a50c`.
- Conformance's `sim-manifest-contract-test` repoints to provenance
  directly, docstring rewritten with a dated note (AR-5(b) refined):
  the binding-half/mirror-drift framing retires with the mirror, the
  end-to-end builder-validity substance continues. Fast-lane companion
  unit test added sim-side. `46fef14`.
- `ehrt.sim.manifest`'s `MirroredManifest` and its own `valid?` retire
  entirely (fresh grep found no real caller outside the retired
  tripwire test) — docstring carries the retirement disclosure, quoting
  the mirror's own M3-Task-0 lesson verbatim. `dff47fb`.
- Two-repo vestige sweep (per-file judgment; one real mechanically-
  stale path fixed, `bases/cli/core.clj`'s broken `notes/ehr-testing-
  sim-mounting-note.md` citation) + a new docs-tooling gate enforcing
  the session-record/prompt pairing invariant both directions, seven
  pre-cutover records allowlisted (derived fresh, red→green proven
  live). `9ec8360`.
- Intake-front-door doctrine written down (no code change): sim runs
  enter `ehr corpus intake` as if foreign, deliberately, the discipline
  having caught real defects before.
- S4's own Deferred row (above) and the 2026-08-02 sim-split plan get
  AR-4's dated note (framing (b), author override plainly stated); the
  sim-manifest-interop Deferred row (above) marked RESOLVED.
- `clojure -M:poly check` clean and full suite green at every one of
  the five commits above; regression-oracle / façade-seam / deftest-
  parity verification recorded in the session record.

## Done (this session, 2026-08-04, Wave H — pre-roll — ADR-0042, GMF PARITY ARC COMPLETE)
- **Config + interpreter phase boundary (Step 1, `98f099b`).** New
  opt-in `:history` run-config flag (absent = pre-existing behavior,
  byte-identical). The interpreter's `mark-phase` mints a `:phase`
  mark (`:history`/`:horizon`) alongside the existing `:pre-horizon`
  boolean, via AR-2's own encounter-anchored inheritance — additive
  arities throughout (`run-module`, the `sim-trajectory` interface),
  zero change to any pre-H call site.
- **Compile filter + straddle inheritance (Step 2, `73bb26f`).**
  `compile-trajectory`'s new `history?` path drops every `:phase
  :history` event uniformly (no dropped-types/fact-types bucketing).
  A property test running 150 random seeds against a purpose-built
  module with a GUARANTEED straddle (`:persona-config {:age-min 0
  :age-max 0}` bounds the DOB-to-registration-t gap to 3–365 days,
  the module's own Encounter closes 500 days after opening) proves the
  invariant catalog never trips, for any seed.
- **Seed-777 retirement + a second straddle class, found live (Step 3,
  `9240db8`).** The UTI engine round-trip test now runs the config-
  default ORDINARY seed (20260802) under `:history true` — the hand-
  picked seed 777 dodge (ADR-0033/0034's own dated notes) retires.
  Running the real closure surfaced a narrower, unanticipated gap:
  `:medication-end`/`:care-plan-end`/`:condition-end` can fire OUTSIDE
  any encounter, orphaning a reference to a dropped antecedent —
  closed by `history-phase?`, one `:references` hop past AR-2's own
  encounter-anchored rule, the SAME "no orphaned reference to
  something dropped" principle applied one step further, not a new
  one. The ear-infections wellness-fold interpreter-layer proof lands
  alongside (`:wellness-wait` needs no special handling, confirmed).
- **Oracle (Step 4, `6a587ff`).** `bin/regression-oracle 537f954
  6a587ff`: all NINE pre-existing vendored root batches IDENTICAL —
  AR-3's own gating argument holds, byte-verified. `digest.clj` gains
  two FIRST history-mode baselines (`ear-infections-history-engine`,
  `urinary-tract-infections-history-engine`).
- `notes/ADRs.md` ADR-0042 (AR-1 through AR-6, AR-6's own
  reconciliation read recorded in full — `:pre-horizon-facts` and
  `check.clj`'s own `:clinical-content-only-when-admitted` invariant
  are CLEANLY SUBSUMED, no conflict — plus the Step 3 finding above,
  the full oracle identity bracket and the two new baselines).
  `.agents/plans/2026-08-02-gmf-parity-plan.md`'s own H row and Status
  header both close, dated. `components/sim-trajectory/docs/gmf-
  interpreter.md` §3 gains an IMPLEMENTED dated note.
- **This was the SOLE remaining GMF-coverage wave — the parity arc
  (Waves A through H) is COMPLETE.** Carry-across (mid-stay-at-window-
  open emission) is the one named future this wave leaves open, moved
  to Deferred above with its own revisit trigger. Roadmap attention
  moves to the non-GMF fronts: census tool refinements, Wave G's own
  attachment deferral, and the standing tooling/design backlog.
- Session record: `.agents/session-records/2026-08-04-gmf-coverage-
  wave-h.md`.

## Done (this session, 2026-08-04, GMF coverage Wave I2 — the last two — ADR-0041, PARITY ACHIEVED)
- **Death cause forms + `:active-careplan` (Step 1+2, one commit,
  `14e8dce`).** Landed together, disclosed — the same shared-file-
  region shape ADR-0040 AR-5 already took. `death-cause-codes`
  resolves `:codes`/`:condition-onset`/`:referenced-by-attribute` per
  `State.java`'s own REAL priority order (source re-read fresh: codes
  first — CORRECTS docs/gmf-interpreter.md section 10's own backwards
  paraphrase). `:condition-onset` gains `:assign-to-attribute` (found
  live, necessary for `congestive_heart_failure.json`'s own `chf`
  attribute to ever be written). `active-careplan-condition-holds?`
  reuses `active-onset-condition-holds?` over `:care-plan-start`/
  `:care-plan-end` for the `:codes` form (`depression_screening.json`'s
  own real use); `:referenced-by-attribute` installed, fixture-proven,
  not yet vendored-exercised. 13 new tests (net +11), full
  sim-trajectory-adjacent suite green (299 tests, 802 assertions).
- **Oracle bracket (Step 3).** `bin/regression-oracle dd6a9d4 14e8dce`
  — all 9 vendored root batches IDENTICAL.
- **Census re-run (Step 4).** `:ok-walked` 82→84, `:walk-failed` 2→0.
  **84/85 `:ok-walked` + 1 `:out-of-scope-by-ruling` + ZERO
  `:load-failed`/`:walk-failed` — parity plan §1/§3's own countable
  definition MET. PARITY ACHIEVED**, pin
  `7e08387c68a7f0e21d13076609a159fd473fc902`, 2026-08-04
  (`.agents/plans/2026-08-02-gmf-parity-plan.md`'s own Status header;
  `components/sim-trajectory/docs/gmf-interpreter.md` §15's own new
  census-re-run subsection). **Wave H is now the SOLE remaining wave.**

## Done (this session, 2026-08-04, GMF coverage Wave I — the singleton tail — ADR-0040)
- **NamedDistribution in `complex_transition` + encounter-class
  vocabulary (Step 1, `d779cd6`).** AR-1's "4 modules" claim corrected
  live (only `injuries.json` was actually NamedDistribution; the other
  3 needed `:hospice`/`:home`/`:urgentcare` encounter-class values,
  AR-1b, author-approved mid-session). `complex_transition`'s nested
  `:distributions` gains the SAME resolution `distributed_transition`'s
  top-level field already had.
- **SetAttribute source precedence (Step 2, `93de2c0`).** The F0
  conflict rejection retired — upstream's own chain (range >
  distribution > valueCode > valueAttribute > value) is legal, ordered
  co-presence, not a conflict. `:range`/`:value-attribute` join.
- **Observation-condition absence → false (Step 3, `f99dff9`).**
  Corrected against the pin: upstream's own issue-774 band-aid, adopted
  unconditionally (this project has no split-records axis to gate it).
- **Vital-sign vocabulary completed (Step 4, `24f0184`; follow-up,
  `d7f5003`).** First pass: 5 rows from the category-gated enumeration
  AR-4 named. Found live re-running the census: the category gate was
  narrower than the actual mechanism (which doesn't gate on category at
  all) — 6 more rows, `category: "laboratory"` upstream. Fixed same
  session.
- **AllergyOnset + Vaccine (Step 5, `959b0bc`).** Both follow the
  nearest existing state's own shape (`ConditionOnset` for
  AllergyOnset's own established M5a simplification; Vaccine simpler
  still, no target-encounter distinction upstream at all). Found live:
  a `:series` key collision with ImagingStudy's own field, fixed with a
  `kw-type` guard — full 325-test sim-trajectory suite re-run green.
- **Oracle bracket (Step 6).** `bin/regression-oracle 3d85fa0 HEAD`,
  run twice (after Step 5 and after the Step 4 follow-up) — all 9
  vendored root batches IDENTICAL both times.
- **Census re-run (Step 7, `8ab71e7`).** `:ok-walked` 75→82 (not 84).
  7 of 9 originally-blocked modules resolve fully. 2 unmask NEW,
  unrelated, unfixed gaps — see Next, above, and ADR-0040 AR-7.
- `notes/ADRs.md` ADR-0040 (AR-1 through AR-7, AR-1b addendum, the
  AR-4 follow-up, AR-5's read findings, the oracle table, full census
  classification). `.agents/plans/2026-08-02-gmf-parity-plan.md` §4
  gains the Wave I execution note.
- **Deviation, disclosed (twice):** AR-1's own "4 modules" claim didn't
  hold against the live tree (AR-1b, escalated via AskUserQuestion
  before any code was written). AR-7's own "parity achieved" framing
  didn't hold either — recorded honestly in the Step 7 commit and
  ADR-0040's own Fence, not silently adopted; no PARITY ACHIEVED note
  was written anywhere.

## Done (this session, 2026-08-04, GMF coverage Wave VS — the vital-sign channel — ADR-0039)
- **Register + baselines + vocabulary (Step 1, `60c8bb1`).** Ctx's own
  new `:vital-signs` compartment — GLOBAL over the whole walk, never
  root-scoped the way workflow `:attributes` is (ADR-0027 D1's own
  third compartment) — threaded through every outcome-producing site.
  Seeded from a new authored-constants table
  (`vital-sign-baselines.edn`) at patient creation, zero rng draws.
  `vital-signs.edn` gains five rows (LVEF, BMI, HDL, Triglycerides,
  Height), LOINC codes copied verbatim from the real candidate closures.
- **`VitalSign` state (Step 2, `6141d6c`).** Loader + interpreter:
  samples once (exact/range/distribution) into the register, never a
  trajectory event; a carried CQL `:expression` is a clean, named load
  rejection. `VitalSign` was the last row in
  `components/sim-trajectory/docs/gmf-interpreter.md`'s own Deferred
  table (§1) — now empty.
- **`:vital-sign` condition (Step 3, `f04218d`).** Reads the register,
  honest absence (ADR-0036 AR-4's rule, extended) when a name is
  genuinely unset — only `Left ventricular Ejection fraction`
  (deliberately baseline-less) can ever be.
- **Oracle bracket (Step 4).** `bin/regression-oracle b396c2c f04218d`
  — all 9 vendored root batches IDENTICAL. Pure identity, byte-verified.
- **Census re-run (Step 5, `3e83390`).** `:ok-walked` 73→75. `covid19`/
  `contraceptives` resolve fully. `congestive-heart-failure` STAYS
  `:load-failed`, on a different, newly-unmasked gap (a `SetAttribute`
  `:value`/`:distribution` conflict, `Inpatient LOS`). `wellness-
  encounters` STAYS `:walk-failed` — `Height` now resolves, the very
  next state (`Record_Weight`) needs `Weight`, outside this Wave's own
  8-name scope. `metabolic-syndrome-care` unchanged, `:ok-walked`, its
  own VS conditions still latent.
- `notes/ADRs.md` ADR-0039 (AR-1 through AR-7, execution note with the
  oracle table and full census movement classification, AR-6's two
  recorded source reads). `.agents/plans/2026-08-02-gmf-parity-plan.md`
  §4 gains LC/VS table rows and the dated H-reordering note (H now
  runs LAST). `components/sim-trajectory/docs/gmf-interpreter.md` §1's
  Deferred table empties; new §16 carries AR-6's findings.
- **Deviation, disclosed:** the session prompt's own suggested
  "vital-sign family closed" framing does not hold — two of four
  originally-blocked modules resolve fully, one unmasks an unrelated
  gap, one advances its own frontier by one state. Recorded honestly
  in the Step 5 commit and ADR-0039's own Fence, not silently adopted.

## Done (this session, 2026-08-03, GMF coverage Wave LC — lookup-column generalization — ADR-0038)
- **Loader generalization (Step 1, `26f280a`).** H2's own
  `recognized-lookup-table-columns` whitelist retired — never a mirror
  of anything upstream does (read directly against the pin,
  `LookupTableTransition`'s own `loadLookupTable`/`follow`: no closed
  column vocabulary at all). Any non-weight column other than `age`/
  `time` now loads unconditionally; load-time rejection narrows to a
  structurally malformed `age`/`time` cell
  (`:malformed-lookup-table-range`). `time`'s own two accepted forms
  (`Utilities.parseDateRange`, transcribed from the pin) convert to
  this project's own epoch-day unit.
- **Walk-time resolution (Step 2, `6af4dc0`).** `lookup-column-value`
  resolves a column module-attribute-first, then a persona-field
  mapping (`gender`/`race`/`state`/`socioeconomic_category`), else
  honest absence (ADR-0036 AR-4's own guard-layer precedent, reused
  verbatim) — case-sensitive value comparison, `LookupTableKey.equals`'s
  own `List<String>.equals`, deliberately NOT the `:race` CONDITION
  type's case-insensitive match. `:time-range` containment joins
  `:age-range`.
- **Persona `:state` (Step 3, `50f7efd`).** A third config-gated draw
  (16), ADR-0036's own race/SES pattern verbatim — deliberately
  distinct from the pre-existing `:address :state` (a USPS
  abbreviation; the lookup-table CSVs key on full state names).
- **Oracle bracket (Step 4).** `bin/regression-oracle 4d868df 50f7efd`
  — all 9 vendored root batches IDENTICAL. Pure identity, byte-verified.
- **Census re-run (Step 5, `a12c911`).** `:ok-walked` 64→73,
  `:load-failed` 17→8 (−9, exactly the 9 lookup-column-blocked
  modules), every other verdict category unchanged. All 9 predicted
  modules (`acute-myeloid-leukemia`, `diabetic-retinopathy-treatment`,
  `hiv-diagnosis`, `myocardial-infarction`,
  `stable-ischemic-heart-disease`, `vhd-aortic`, `vhd-mitral`,
  `vhd-pulmonic`, `vhd-tricuspid`) move `:load-failed` → `:ok-walked`
  cleanly — zero surfaced a next blocker, zero regressed.
- `notes/ADRs.md` ADR-0038 (AR-1 through AR-5, execution note with the
  oracle table and full census movement classification). This
  roadmap's own Deferred section: the `myocardial_infarction.json` row
  and the `race`/`time` lookup-column row both marked RESOLVED, pointing
  here.
- **Next frontier (post-LC, undecided):** the schema-invalid family
  (`injuries`/hospice's own `complex_transition` NamedDistribution
  gap), the vital-sign channel (ADR-0036 AR-7, still unmoved), and the
  8 modules remaining `:load-failed` on unrelated gaps
  (`VitalSign`/`AllergyOnset`/`Vaccine`) — awaiting the design
  channel's own post-LC read of the new census artifact before a next
  wave is scheduled.

## Done (this session, 2026-08-03, GMF coverage Wave G — wellness cycle — ADR-0037)
- **The wellness cadence table + pure schedule function (Step 1,
  `d209267`).** `EncounterModule.recommendedTimeBetweenWellnessVisits`
  transcribed verbatim from the pinned Synthea source (lines 176-201)
  into `resources/sim-trajectory/wellness-cadence.edn`, the
  chronic-meds annual cap EXCLUDED by ruling (see Deferred, above).
  `next-wellness-tick` is a pure, zero-draw function anchoring the
  cycle at DOB — no stored schedule state anywhere, load-bearing for
  the oracle bracket below.
- **Loader: `wellness: true` loads as `:wellness-wait` (Step 2,
  `23974ba`).** Retires the Wave B create-now substitution
  (`normalize-state`'s own dated retirement comment; `docs/gmf-
  interpreter.md` sections 4 and 9 gain matching dated notes) — a
  distinct state type, not a synthesized `:encounter-class`.
- **Interpreter wait semantics (Step 3, `cbf5330`).** `wellness-wait-
  step` advances the module clock to `next-wellness-tick`, opens the
  wellness encounter AT the tick (attaching `:reason`, a new thread),
  and is bounded by `horizon-end-t` exactly as Delay is — no code
  change needed there, the existing loop-level horizon check already
  covers it. `ear_infections`' own interpreter test gains a new-timing
  assertion (the wellness encounter now fires strictly after the last
  medication ends); the sim-emit-hl7 engine round-trip test's docstring
  records the round trip stays green under the new timing.
- **Live finding, found running this Wave's own census, fixed same
  session (`203ed9f`): `next-wellness-tick`'s boundary semantics
  refined from inclusive `>=` to strict `>`.** The real `med_rec.json`
  has ZERO delay anywhere in its own wellness-wait loop body — an
  inclusive first call at `t` = DOB returned DOB unchanged, and every
  re-entry at that same unchanged `t` returned the same tick again, an
  infinite zero-advance spin into `max-steps`. Strict `>` guarantees
  every call returns a tick strictly later than its own `t`, matching
  upstream's own never-fires-twice-at-the-same-instant guarantee.
  Verified directly: a standalone trace of `med_rec.json` now completes
  at `:horizon-complete` with 269 real events, where it previously
  threw.
- **Oracle bracket (`58fdd9c` → `203ed9f`, `bin/regression-oracle`, all
  9 root batches): IDENTICAL except `ear-infections`/`ear-infections-
  engine`, exactly the two batches ADR-0037 AR-6 predicted** — the
  ear-infections episode now resolves at a real cadence tick instead of
  immediately after medications end, by design. Every other batch
  (`appendicitis`, `death-fixture`, `sepsis`, `sinusitis`, `sore-
  throat`, `total-joint-replacement-engine`, `urinary-tract-infections-
  engine`) byte-identical. The `next-wellness-tick` fix itself changed
  NEITHER ear-infections digest from what the pre-fix oracle run
  already recorded — the fix never touches ear_infections' own real
  seeded walks (verified, not assumed).
- **Census re-run (Step 5, `8fc4b03`,
  `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387-
  wave-g.edn`): `:ok-walked` 60→64, `:load-failed` 18→17, `:walk-failed`
  7→3, `:out-of-scope-by-ruling` 0→1, total 85→85 unchanged.** The
  `:wellness-timing` detector retires (`ehrt.sim-trajectory.census`,
  kept as history, no longer called); a new `out-of-scope-by-ruling?`
  classifier reclassifies `gallstones` (its own sole gap, `Physiology`,
  the census's first ruled exclusion). All four real upstream loop
  modules (`med-rec`/`mend-program`/`metabolic-syndrome-care`/
  `veteran-substance-abuse-treatment`) resolve fully to `:ok-walked`.
  Of the 19 formerly-tagged modules: 7 stay `:ok-walked` but change walk
  digest (a wait now times the encounter differently — expected); 8
  show no observable difference (their own seeds/horizon never cross
  the wellness-wait path differently). No module outside these 19
  moved at all — full classification in `docs/gmf-interpreter.md`'s own
  new dated subsection.
- `notes/ADRs.md` ADR-0037 (AR-1 through AR-8, the mid-session
  `next-wellness-tick` deviation record, execution note with the full
  oracle table and census movement classification).
  `.agents/plans/2026-08-02-gmf-parity-plan.md` §4 (Wave G row marked
  DONE in the dated note above the table; Wave H's own row gains the
  wellness-wait-during-history-phase dated note). This roadmap's own
  Deferred section gains two rows (the chronic-meds cadence cap; the
  Wave G attachment deferral) and Next moves to Wave H's own design
  session.
- Commits, in order: `d209267` (Step 1, cadence table + schedule
  function), `23974ba` (Step 2, loader), `cbf5330` (Step 3, interpreter
  + tests), `203ed9f` (live fix, `next-wellness-tick` strict `>`),
  `8fc4b03` (Step 5, census re-run), and this session's own closing
  records commit. Session record: `.agents/session-records/2026-08-03-
  gmf-coverage-wave-g.md`.

## Done (this session, 2026-08-03, Wave F0 — distributions — ADR-0035)
- GAUSSIAN/EXPONENTIAL/TRIANGULAR join the v2 distribution vocabulary
  (loader, `ced1c06`) across Delay/Symptom timing, Procedure duration,
  and SetAttribute value; an unrecognized `:kind` rejects cleanly
  (`:unsupported-distribution-kind`) instead of throwing — structurally
  closes the `gmf_version 2` loader-exception class ADR-0034's census
  found. Single-draw interpreter sampling (`c5cde06`): `sample-
  distribution` + `probit-approx` (Acklam's rational inverse-CDF
  approximation, source-cited, spot-checked against known standard-
  normal quantiles), fixed-consumption law throughout. SetAttribute's
  own silent-nil gap fixed (`c9de204`): a state whose only value source
  was `:distribution` used to write `nil` — invisible to the census's
  own walk-verification, since a module still loads and walks clean
  with `nil` attributes feeding its guards.
- Real bug found and fixed mid-session (Step 2): `emit-and-advance` is
  the shared helper every trajectory-event-producing state type calls,
  not Procedure-only — an ungated `:distribution` check crashed on
  `uti/ed_bundle.json`'s own O2-saturation Observation states (a
  pre-existing, out-of-scope, never-normalized raw v2 field). Gated on
  `(= :procedure (:type state))`; a regression test pins the exact
  shape.
- Oracle bracket (`d9545c9` → `c9de204`, `bin/regression-oracle`,
  9 root batches incl. the three engine-layer closures ADR-0033 AR-4b
  added): IDENTICAL — pure identity held, byte-verified.
- Census re-run (`c80c5c5`,
  `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387-
  wave-f0.edn`, committed alongside the original): `:ok-walked` 40→42,
  `:load-failed` 39→34, `:walk-failed` 6→9. All 11 originally-loader-
  exception modules traced individually — 2 resolve, 3 surface a next
  blocker, 6 stay `:load-failed` on an unrelated, earlier-in-key-order
  gap (the loader's own first-found short-circuit never reached their
  fixed content at all). Zero SetAttribute-distribution digest movement
  among already-`:ok-walked` modules — `hypertension.json` (this
  session's own cited example) stays `:load-failed` on `Counter`,
  blocked before its own GAUSSIAN SetAttribute state is ever reached; an
  honest negative result, not a gap in the fix (proven directly by unit
  tests instead). All seven vendored roots unmoved. Found live: the
  census tool's own filename has no same-calendar-day disambiguation —
  disclosed in `docs/gmf-interpreter.md`'s own new §15 subsection, not
  fixed (out of this session's fence).
- `notes/ADRs.md` ADR-0035 (AR-1 through AR-8, execution note with the
  full oracle table and census delta classification). AR-8's ratified
  resequencing captured in `.agents/plans/2026-08-02-gmf-parity-plan.md`
  §4 (a new F0 row; Wave E kept in the table, annotated RE-SCOPED, not
  deleted) and this roadmap's own Next section (below).
- Commits, in order: `ced1c06` (Step 1, loader), `c5cde06` (Step 2,
  interpreter timing sampling + the `emit-and-advance` scoping fix),
  `c9de204` (Step 3, SetAttribute), `c80c5c5` (Step 5, census re-run +
  doc delta note), and this session's own closing records commit.
  Session record: `.agents/session-records/2026-08-03-wave-f0-
  distributions.md`.

## Done (this session, 2026-08-03, GMF coverage Wave F — Counter/ImagingStudy/SupplyList/condition rider — ADR-0036)
- `Counter` (SetAttribute-shaped attribute arithmetic, legacy amount-
  default-to-1, zero draws), `ImagingStudy` (one glass-box trajectory
  event carrying procedure code/primary modality/drawn series+instance
  counts, compiling to the SAME IR step family a Procedure produces via
  `compile-trajectory`'s own `procedure->step`, no clock advance), and
  `SupplyList` (a log-only trajectory fact, the ConditionEnd
  no-open-encounter precedent verbatim, unconditional) all land
  together (`98f53ad`) — this document's own original Deferred table's
  last remaining row (`Counter`) is now built, and `ImagingStudy`
  reverses its own R5 deferral (ADR-0029).
- Condition rider + persona (`c9b2bbf`): `Not` (recursive negation —
  fixed `normalize-condition`'s own recursive-normalization gap, gated
  on the plural `:conditions` key, never firing for `Not`'s singular
  `:condition`), `Race` (case-insensitive), `Socioeconomic Status`
  (case-sensitive) join the condition vocabulary. Persona gains
  optional `:race`/`:socioeconomic-category` fields, sampled ONLY when
  config supplies category weights — a deliberate, narrow, DOCUMENTED
  exception to the fixed-RNG-consumption law (`persona.clj`'s own
  docstring has the full reasoning: config-time variation, the same
  class `age-min`/`age-max` already are, never a runtime-outcome-
  dependent branch), proven by a direct draw-count test (13 unconfigured,
  15 with both weights, 14 with one). Evaluating Race/Socioeconomic
  Status against a persona missing the field is a WALK ERROR, not a
  silent false and not an escaping exception — `step` still throws a
  distinctly-marked exception (propagating through and/or/at-least/
  guard/transition recursion exactly like every other exception this
  namespace throws); `walk-module`/`run-module`'s own loop is the ONE
  place that specific marker is caught and converted into a recorded
  `:walk-error` status, proven NOT to catch any other exception class
  by a dedicated test.
- Oracle bracket (`e26c9c1` → `c9b2bbf`, `bin/regression-oracle`, all 9
  root batches): IDENTICAL — pure identity held, byte-verified, the
  real script's own output.
- Census re-run (`83f7858`,
  `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387-
  wave-f.edn`, committed alongside both prior artifacts): `:ok-walked`
  42→60, `:load-failed` 34→18, `:walk-failed` 9→7. `Counter`/
  `ImagingStudy`/`SupplyList` vanish from top-gap-mechanisms entirely.
  All 20 verdict changes traced individually (`docs/gmf-interpreter.md`
  §15's own AR-8 subsection has the full account): 10 `Counter`-blocked
  and 4 `SupplyList`-blocked modules resolve fully, 2 more surface a
  `max-steps` runaway (a wellness-cycle-adjacent substitution artifact,
  joining `med-rec`/`veteran-substance-abuse-treatment` in the Wave G
  ledger); all 4 `Race`/`Not`-blocked modules resolve fully; all 10
  `ImagingStudy`-blocked modules surface a next blocker (never resolved
  alone, never regressed) — `VitalSign` (1), `Physiology` (1, a
  genuinely new deferred type), an unrecognized lookup-table column (7,
  each distinct), and a pre-existing `complex_transition`/
  NamedDistribution schema gap (1, `injuries.json`). All seven vendored
  roots unmoved. Substance note (AR-8b, found live): 26 of 42 pre-Wave-F
  `:ok-walked` modules — including `stroke` — produce ZERO trajectory
  events on every census seed; walk-verification attests determinism of
  what a walk touches, which for the gated chronic cluster is currently
  almost nothing (named for a future ranking session, not a Wave F
  defect).
- `notes/ADRs.md` ADR-0036 (AR-1 through AR-8, execution note with the
  oracle table and full census movement classification).
  `docs/gmf-interpreter.md` §1 (Counter/ImagingStudy/SupplyList moved
  from Deferred to the main table; `VitalSign` newly disclosed in
  Deferred), §2 (Not/Race/Socioeconomic Status prose paragraph), §15
  (the Wave F census re-run subsection + AR-8b substance note). This
  roadmap's own Deferred section gains four rows (the stale
  `myocardial_infarction.json` claim corrected; census tool
  refinements; the UTI Observation raw-`:distribution` gap; the
  vital-sign channel; lookup-table columns `race`/`time` for Wave I)
  and this Next section moves Wave F to Done, entering Wave G's own
  design session in its place.
- Commits, in order: `98f53ad` (Steps 1-3, Counter/ImagingStudy/
  SupplyList), `c9b2bbf` (Step 4, condition rider + persona), `83f7858`
  (Step 6, census re-run), and this session's own closing records
  commit. Session record: `.agents/session-records/2026-08-03-gmf-
  coverage-wave-f.md`.

## Done (this session, 2026-08-03, GMF census — ADR-0034)
- `ehrt.sim-trajectory.census` (`development/src`, a dev entry point per
  ADR-0031 AR-1) lands: pin verification, closure resolution over an
  external Synthea checkout, AR-2's verdict vocabulary with gap
  extraction, AR-3's mechanical wellness-substitution tag, AR-4's 3-seed
  smoke walk + sha256 digest. 5 co-landing tests, one per verdict class
  plus the substitution tag, green — disclosed not run by
  `clojure -M:poly test :all skip:integration` (poly test is per-project
  against a project's own bricks; the `dev` project carries none),
  verified instead by direct `clojure -M:dev:test` invocation.
- First census run at the interpreter doc's own pin
  (`7e08387c68a7f0e21d13076609a159fd473fc902`), committed:
  `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387.edn`
  — 85 modules, 40 `:ok-walked`, 39 `:load-failed`, 6 `:walk-failed`, 0
  `:out-of-scope-by-ruling`, 19 carrying the wellness-timing tag. Sanity
  anchors held (all seven vendored roots `:ok-walked`; all five of
  ADR-0031 AR-5(a)'s named wellness modules tagged) — no
  STOP-AND-ESCALATE.
- Two real, disclosed findings the full-catalog sweep surfaced that no
  hand survey had: `ehrt.sim-trajectory.gmf/gmf-v2-timing->v1` throws a
  raw `IllegalArgumentException` (not a `:rejected` Result) on a real
  `GAUSSIAN`/`EXPONENTIAL` `gmf_version 2` distribution kind (11 modules
  combined — the census tool itself now wraps `load-closure` in
  try/catch so this doesn't abort the run); two new unrecognized
  condition types, `Race` (3 modules) and `Not` (1 module), neither ever
  named in `docs/gmf-interpreter.md` §2. Both named, not fixed — the
  census observes, per its own fence.
- `docs/gmf-interpreter.md` §15 (new, dated) carries the full
  verdict/gap breakdown and supersedes §8's own hand-scouted
  prioritization table (kept, annotated, not deleted) as the frontier of
  record.
- `notes/ADRs.md` ADR-0034 records this session's own rulings (mirrored
  from the driving prompt's author rulings) and execution note.
  AR-6 bookkeeping: the `bin/regression-oracle` checkout-only limitation
  (Deferred, above) and a dated pointer at Wave H's own row
  (`.agents/plans/2026-08-02-gmf-parity-plan.md` §4) to the UTI
  pre-horizon straddle finding (ADR-0033's own execution note).
- Session record: `.agents/session-records/2026-08-03-gmf-census.md`.
  Commits: `6392363` (Step 1, tool + tests), `41c86a0` (Step 2, census
  run + doc summary), and this session's own closing records commit.

## Done (this session, 2026-08-03, engine closure-context fix)
- ADR-0031 AR-6's second (and final) defect-fix session, ADR-0030 J3
  closed: `engine.clj`'s `:registered` decide now calls `run-module` at
  the full arity, threading a closure's own `:modules`/`:tables`/
  `:initial-attributes` through — `notes/ADRs.md` ADR-0033 AR-2/AR-3
  (`74be432`). The three pinned round-trip tests
  (`components/sim-emit-hl7/test/`) converted from asserting the broken
  behavior to asserting the real round trip, one commit per root
  (`5ac9382`, `16b3b57`, `0f9c827`).
- Oracle bracket: the six pre-existing roots stayed byte-identical
  (ADR-0033 AR-4, proven via a disclosed per-worktree-`digest.clj`
  deviation, not the one-liner unmodified — `notes/ADRs.md` ADR-0033's
  own execution note has the full reasoning and both digest tables);
  `digest.clj` extended with three engine-layer FIRST BASELINES for the
  closure roots (AR-4b, `ba6910a`) — closes ADR-0032's own oracle-gap
  disclosure.
- Real finding, disclosed not fixed: the UTI round-trip test's own
  original seed trips an already-documented, separate v1 scope boundary
  (`:pre-horizon-facts` not feeding the engine's patient-state fold when
  an Encounter straddles the fixed registration-t anchor) — resolved by
  an empirically-chosen seed for the test, per ADR-0033's own execution
  note; the underlying fold-boundary gap itself is NOT this session's
  own scope.

## Done (this session, 2026-08-01, migration session 1)
- Items 6+7: `agent/scenario-roster.md` merged into `.agents/skills/scenarios/roster.md`,
  `agent/` (singular) retired (47c815c)
- Item 13: `.agents/plans/roadmap.md` (this file) lands from the design-channel
  ledger handover (47c815c)
- Item 9: `.claude/skills/` mirror-with-gate lands (ADR-0024); end-to-end proof
  and the /mnt/c fast-forward are AUTHOR ACTION, see Externals above (a9e5be6,
  8df3cf3)

## Done (this session, 2026-08-02, migration session 2)
- Items 1+12: `notes/prompts/` sealed — `ehrt.docs-tooling.notes-prompts-frozen-test`
  pins the 29-file set, `stale_path_test.clj` gains the archive-instruction
  tripwire (both red→green live-proven) (6c3c494)
- Items 4+11: `notes/README.md` lands (six top-level files + three subdirs
  indexed, zone-marked); `.agents/skills/README.md` plus all 10 skill-directory
  READMEs land (mirrored to `.claude/skills/`); `ehrt.docs-tooling.readme-presence-test`
  enforces both trees going forward, `notes/sim/`/`notes/tools/` exempt
  (ruling 6) (ab9fe5e)

## Done (this session, 2026-08-02, migration session 3)
- Item 10: `ehrt.docs-tooling.index-completeness-test` lands — both directions,
  over `.agents/plans/`, `.agents/prompts/`, `.agents/session-records/`,
  `.agents/skills/`, `notes/`; `notes/prompts/` convention-exempt; ruling 6
  extended to completeness (77880f7)
- Item 3(a): sim citation-stubs pass, reading (a) — 8 F-rows and 10 ADRs cited
  at their live restatement site (4 were miscitations, fixed not just
  supplemented); `notes/facts-register.md` F20 stub names the two-file
  topology; full accounting in the session record (54ab3b6)

## Done (this session, 2026-08-02, migration session 4)
- Item 8: `.agents/reading-sets.edn` lands — five named sets, each path
  justified inline, gated by `ehrt.docs-tooling.reading-set-budget-test`
  (ghost-path + budget checks, both red→green live-proven); every
  `:budget-lines` is this session's own measured actual, corrected once
  more for this section's own weight (see the session record's "it
  caught itself twice" note): `:onboarding` 538, `:corpus` 1519, `:sim`
  574, `:judge` 644, `:docs` 433 — the baseline the author's future
  budget ruling (charter §6) now has a real number to cite (ab679c9)
- `components/sim/src`/`test` bare-`ADR-NNNN` docstring sweep (the "Next"
  backlog item migration session 3 named): 151 bare references across 39
  files classified and requalified — 149 to `sim/ADR-NNNN`, 1 to
  `tools/ADR-0015` (a cross-repo miscitation this sweep discovered), 1
  left deliberately bare (already correctly cites the live register);
  two further wrong-file-path miscitations also fixed; docstring/comment/
  fixture-remark edits only, zero behavior change; full one-to-one
  accounting in the session record (72f5542)

## Done (this session, 2026-08-02, migration session 5)
- Item 5: way-of-working session mechanics distilled into five repo-local
  skills -- `build-session`, `capture-session`, `extraction-stage`,
  `errata-sweep`, `session-prompt` -- each citing its own provenance in
  AGENTS.md/AUTHORS-GUIDE.md/notes/ADRs.md/named session records;
  mirrored to `.claude/skills/`, indexed in both READMEs, all three
  affected gates (readme-presence, index-completeness,
  skill-mirror-currency) green (60b9f87)
- Item 9's fresh-session discovery probe: CONFIRMED 2026-08-02 (external
  observation, no commit) -- a fresh, non-nested Claude Code session's
  own Skill listing showed `wsl-windows-git-hygiene` with its full
  description, closing the acceptance test migration session 1 could
  not self-administer. The paired AUTHOR ACTION (fast-forward `/mnt/c`)
  remains open, see Externals above.
- `build-session` added to all five `.agents/reading-sets.edn` sets
  (AR-3: ceremony applies regardless of task class); `session-prompt`
  named in `:onboarding`'s own comments for design-channel
  prompt-authoring sessions, not added as a budgeted path;
  `capture-session`/`extraction-stage`/`errata-sweep` stay excluded from
  every set, same reason the existing ten skills are (session-type, not
  domain-task-class). Budget deltas in the session record and
  `notes/ADRs.md` ADR-0023's own dated-note thread.

## Done (this session, 2026-08-02, migration session 6)
- Item 14 (last open item, migration report fully executed): use-cases
  split — `docs/use-cases.md` is now a generated index (one line per
  case, linking out), `docs/use-cases/<id>.md` is one standalone page
  per case, both from `components/corpus/docs/use-cases.edn` unchanged
  except for the internal cross-case and reference-doc links the file
  split itself requires (never a case's own narrative/strip/equations
  text). Content conservation proven one-to-one against the prior
  single-file rendering (script-diffed per case, modulo heading/banner/
  link-depth scaffolding); the CI freshness gate
  (`.github/workflows/test.yml`) now diffs `docs/use-cases/` alongside
  the index. Every repo citation of `docs/use-cases.md#<case>` swept to
  its per-case file. Full accounting in the session record (ceca0f7,
  plus this checkpoint's own commit).

## Done (this session, 2026-08-02, sim split S1+S2)
- Sim split S1: `components/sim-model` extracted from `components/sim`
  (`pathway`/`facility`/`persona`/`config`), `.agents/plans/2026-08-02-
  sim-split-plan.md`'s AR-1..AR-4, `notes/ADRs.md` ADR-0025. Committed
  and pushed by the author (8d5c86c) before S2 began.
- Sim split S2: `components/sim-trajectory` extracted from
  `components/sim` (`gmf`/`gmf-interpreter`/`compile-trajectory`, their
  three docs and vendored-module fixtures), same plan, same ADR.
  poly check clean, poly test 0 failures/0 errors, golden run
  byte-identical, deftest+defspec parity (403=403=403) at every stage.
  Full caller map, verification baselines, and the one fixed-forward
  deviation (fixture resource-path/`ns`-form misses, caught by `poly
  test` before commit) in the session record and ADR-0025's own
  Decision section.
- S3 (`sim-emit-hl7`)/S4 (`sim-engine`) rows moved to Deferred above
  with their trigger conditions; the GMF coverage-expansion payoff
  milestone S2 unblocks is deferred alongside them, explicitly not
  started this session (plan's own R-4).

## Done (this session, 2026-08-02, GMF coverage Wave A)
- Condition vocabulary v1→v1.1: `:or`/`:at-least` (compound wrappers),
  `:date` (calendar-year vs. `ctx`'s own virtual clock), `:observation`-
  as-a-condition-type (log query over already-emitted `:observation`
  events), and `:symptom`-as-a-condition-type (an emergent finding, not
  one of AR-2's five named candidates — required for `:at-least`'s only
  real vendored use). `Active Allergy` needed no new work (already M5b).
  `Vital Sign`/`Active CarePlan` confirmed still OUT (AR-2's pre-ruling,
  neither appears in `sore_throat.json`). `notes/ADRs.md` ADR-0026.
  Commits: `0b2c1b2` (wave plan), `9176250` (`:symptom`), `f99e87a`
  (`:at-least`+`:or`), `5e3e72c` (`:date`), `6a35492` (`:observation`),
  `6a3e11b` (docs).
- `sore_throat.json` vendored (`a2cf68d`) — the wave's own payoff module,
  state-type clean since M5-prep, blocked at every prior survey by
  exactly the condition-vocabulary gap this wave closed. Real branch
  coverage through `Determine_if_Bacterial`'s `At Least` compound
  (`vendored_sore_throat_test.clj`, both threshold branches).
  `stroke.json`'s own `Date` gap is also resolved, but the module stays
  deferred (its `Death` state-type gap, AR-6 — Wave C's own trigger).
- Regression oracle (fixed-seed `sinusitis`/`appendicitis` walks)
  byte-identical across every commit this session, confirmed at close.
  Full accounting in the session record and ADR-0026's own Decision
  section.

## Done (this session, 2026-08-02, GMF coverage Wave B)
- `CallSubmodule` — loader closure resolution (D3, `gmf/load-closure`,
  the all-or-nothing gate extended over a whole closure, a cyclic-graph
  check with a real bug found and fixed by its own red test),
  interpreter call/return (D1/D4, descend-run-return, root-scoped
  workflow attributes, a defensive call-depth backstop), and D2's own
  cross-boundary `:call-path` citations. The fifth transition kind,
  `type_of_care_transition` (D5, characterized against Synthea's own
  `Transition.java` + `telemedicine_config.json` before
  implementation — a real Java `Random` sequential-seed clustering bug
  found and fixed in the test suite itself). `notes/ADRs.md` ADR-0027.
  Commits: `a92254b` (ADR + roadmap), `f596a37` (closure survey, D5/D7),
  `9a2f0cd` (D1 refactor), `599fa47` (D3 closure loading), `cc9e0d6`
  (D1-D4 call/return), `13b924e` (encounter-class normalizations,
  disclosed addition), `3adf974` (D5).
- `ear_infections.json` closure vendored (`01eb56b`) — the fourth real
  vendored module and this project's first CLOSURE (root plus two
  called submodules, `resources/modules/NOTICE`'s own new rows), state-
  type clean of every Wave-D-scoped deferred type once its real closure
  was read, at the cost of two Step 2c mandatory-path findings
  (`assign_to_attribute`/`referenced_by_attribute`, `is nil`/`is not
  nil` operators) plus two Step 2e loader normalizations. A real,
  end-to-end vendored walk reaches through a called submodule with
  correct call-path citations and cross-module `referenced_by_attribute`
  resolution (`vendored_ear_infections_test.clj`).
- **Payoff-map update: `urinary_tract_infections.json` is NOT vendored
  this wave (D6).** Its own real closure (twelve files, not the four
  the wave plan's own top-level survey assumed) is dirty in EVERY
  branch with `DiagnosticReport`/`MultiObservation`, both Wave D's own
  scope — the payoff shrinks from two closures to one, honestly, per
  the session prompt's own "contingent on its closure surveying clean"
  framing. A genuinely new, unplanned finding along the way:
  `lookup_table_transition`, a SIXTH GMF transition kind, on that
  module's own entry path — named, not built (ADR-0027's own Deviation
  record). `total_joint_replacement.json`/`myocardial_infarction.json`
  had `CallSubmodule` removed from their own blocker lists
  (`docs/gmf-interpreter.md`'s own dated notes) but neither was
  re-characterized this session — both still need a real closure read
  before any vendoring claim, and MI still needs Wave C (`Death`) per
  the wave plan's own "B+C → MI" sequencing either way.
- Regression oracle (fixed-seed `sinusitis`/`appendicitis`/
  `sore_throat` walks) byte-identical across every commit this session,
  confirmed at close; full workspace `poly test :all skip:integration`
  green (a self-caught `.agents/reading-sets.edn` budget bump along the
  way, the same shape Wave A's own close-out already hit).

## Done (this session, 2026-08-02, GMF coverage Wave C)
- `Death` lands as real, terminal, trajectory-event-producing v1 state
  (loader + interpreter, `notes/ADRs.md` ADR-0028 C1/C2) — three time
  forms grounded against `State.java`'s own real `Death` class at the
  pinned commit (immediate/exact/range, `range` reusing the same
  fixed-consumption helper `Delay`/`Procedure` duration already share);
  only the `:codes` cause-of-death form is built (`:condition-onset`/
  `:referenced-by-attribute` named unbuilt, no vendored module needs
  either). The walk terminates AT `:death` — a disclosed departure from
  real Synthea's own continue-past-Death semantics — property-tested
  (`no-trajectory-event-ever-follows-death`, 200 seeds).
- `:death` compiles into the pathway WITHOUT a new IR step type (C4) —
  `:discharge` (`sim-model/pathway.clj`) gains two new optional fields,
  `:disposition`/`:codes`; real HL7v2 already models a death this way
  (an ordinary ADT^A03 whose PV1-36 carries an expired disposition
  code). Death inside an encounter attaches as its own terminal
  disposition; death outside any encounter closes the pathway without
  fabricating a discharge from an admission that never happened.
- `:expired` lands in CODE for the first time (C3) — `PatientState`'s
  own `:status` enum, `docs/patient-state-model.md`'s own accumulator
  claim, docs-only until this session (checked directly: `:expired`
  existed nowhere in `components/sim/src` except three lines of prose).
  `ehrt.sim.engine`'s own `:discharge` decide/evolve fold an
  expired-disposition discharge to `:status :expired`, location/
  attending UNCHANGED (the body stays where it was), and suppress the
  existing bed-ready-transfer coupling (a finding the docs' own gap
  table didn't name — no bed is actually vacated). One new structural
  invariant, `expired-patient-retains-location`; `order-only-when-
  admitted`/`clinical-content-only-when-admitted` already generalize to
  cover `:expired` automatically, zero code change, confirmed by the
  green suite.
- **Payoff-map correction: `stroke.json` is NOT vendored this wave.**
  Real-closure characterization found `Chance_of_Stroke`'s own
  `distributed_transition` gates onset on `{"attribute": "stroke_risk",
  "default": 0}` — a real Synthea engine attribute (Framingham
  cardiovascular risk, `CardiovascularDiseaseModule`) this project has
  no source for, whose own JSON default makes onset structurally
  unreachable if honored literally. Escalated and ruled (design
  channel, 2026-08-02): deferred, D6-style, revisit trigger named
  (`docs/gmf-interpreter.md` section 10; ADR-0028's own Deviation
  record). `Death` is proven instead against this project's own hand-
  authored `death-fixture.json` — interpreter, compile-trajectory, and
  a full 200-patient engine/check round trip, both outcomes present,
  the full invariant catalog holds.
- Commits, in order: `7e4204b` (Step 0, ADR + coverage-plan riders),
  `ed4f7bd` (Step 1, characterization), `a900f99` (Step 2a, Death state/
  terminal contract), `47d0f66` (Step 2b, compile-trajectory mapping),
  `380a3e2` (Step 2c, engine/check minimal path), `66005ae` (Step 3,
  death-fixture proof). `notes/ADRs.md` ADR-0028.
- Regression oracle (fixed-seed `sinusitis`/`appendicitis`/
  `sore_throat`/`ear_infections` walks) byte-identical across every
  commit this session, confirmed at close; full workspace `poly test
  :all skip:integration` and `poly check` green throughout.

## Done (this session, 2026-08-02, sim split S3 / GMF coverage Wave D stage D0)
- Sim split S3: `components/sim-emit-hl7` extracted from `components/sim`
  (`emit-hl7`/`v2-replay`/`site-profile`), fired now per ADR-0029 R1
  rather than waiting on its own named trigger — front-running
  deliberately, the same session that ruled Wave D's own design (R1–R7).
  `ehrt.sim-emit-hl7.interface` re-exports `emit` (3/5/6-arg only),
  `control-id-for`, `default-reference-date`, `default-utc-offset`;
  `v2-replay`/`site-profile` have no real external caller and stay fully
  internal. `notes/ADRs.md` ADR-0025 (dated S3-executed note) and
  ADR-0029 (Wave D design, D0 execution note).
- `poly check` clean; `poly test :all skip:integration` 0 failures/0
  errors; golden run (seed 42, 5 patients, `--emit hl7`) byte-identical
  except the manifest's `:generator :sha256` (tracks HEAD, expected);
  deftest+defspec parity 281 = 206 residual + 75 `sim-emit-hl7`.
- Wave D itself (D1–D3: observation family, CarePlan family,
  `lookup_table_transition`) is not started — named in ADR-0029 and
  `.agents/plans/2026-08-02-gmf-coverage-plan.md`'s own restructure,
  each its own future session.
- Commits, in order: `7935b71`/`7a3dd58` (Step 0, ADR-0029 + plan
  restructure, plus a same-session fix-forward for an ADR
  insertion-order mistake caught before Step 1), `ccce1fc` (Step 1,
  characterization), `e38e232` (Step 2, extraction), and this session's
  own closing records commit.

## Done (this session, 2026-08-02, GMF coverage Wave D stage D1 -- D1a + D1b)
- D1a (characterization + schema PROPOSAL, same-day prior session):
  `sepsis.json`'s own closure surveyed clean of every D3-scoped
  transition kind; `MultiObservation`/`DiagnosticReport`'s shared
  `ObservationGroup` parent grounded against real Synthea source; three
  value-sourcing mechanisms found side by side (`range` built,
  `value_code`/`vital_sign` not); the `vital_sign` field's own real
  upstream source (`LifecycleModule.java`) found unported, with no
  physiology-simulation equivalent in this project. Schema PROPOSAL
  (P1–P6) drafted, awaiting ruling.
- D1b (implementation, this session): the PROPOSAL RULED AS DRAFTED,
  Q1–Q4 resolved (`:category` added now; one curated vital-sign
  reference table answers both the code gap and the value gap and
  supplies OBX reference-range/abnormal-flag; ruled on this session's
  own engine-source evidence, confirmation duty carried forward to the
  next MultiObservation/DiagnosticReport module vendored) — full chain
  landed: `sim-model`'s `:diagnostic-report` step, `sim-trajectory`'s
  loader/interpreter/compile-trajectory, `sim`'s engine pass-through and
  invariant extension, `sim-emit-hl7`'s ORU rendering, `sepsis.json`
  vendored for real. `poly check` clean; full non-integration suite
  green (188 namespaces, 0 failures/0 errors); the byte-identical oracle
  held for all five pre-existing vendored roots. `VitalSign`/`Vital
  Sign` stay design-ruled, implementation-deferred (F3) -- no vendored
  module exercises either yet.
- Commits, in order: `297e337` (Step 0, RULED + roadmap), `917e9cf`
  (reading-set budget self-catch, fix-forward), `5974fd2` (Step 1,
  vital-sign table), `f4a4e99` (Step 2a, sim-model), `acd49f5` (Step 2b,
  sim-trajectory), `7a13de5` (Step 2c, sim), `e345f13` (Step 2d,
  sim-emit-hl7), `870a1ab` (Step 3, sepsis vendored), and this session's
  own closing records commit. `notes/ADRs.md` ADR-0029's own D1a schema
  RULING and D1b execution note; `.agents/session-records/2026-08-02-
  gmf-coverage-wave-d-stage-d1b.md`.

## Done (this session, 2026-08-02, GMF coverage Wave D stage D2)
- Step 1 characterization: both closures fetched in full at the pin
  (27 files for `myocardial_infarction.json`, through its own CABG
  surgical pathway; 4 for `total_joint_replacement.json`). MI dirty
  with three independent blockers (`lookup_table_transition`/D3,
  `ImagingStudy`/R5, a genuinely new state type `SupplyList`) —
  deferred. TJR's own `CarePlanEnd.careplan` field grounded directly
  against `State.java` as a same-module state-name reference,
  structurally identical to `MedicationEnd.medication_order` — R2(b)'s
  pair-mirror confirmed against source.
- Step 2 implementation: the full CarePlan chain landed and stayed
  green throughout — `:care-plan-start`/`:care-plan-end` pathway-IR
  steps (sim-model); loader/interpreter/compile-trajectory mapping
  (sim-trajectory), including a new backward-compatible `run-module`
  `initial-attributes` arity; `CarePlanRecord`/decide/evolve fold and
  a `clinical-content-only-when-admitted` extension (sim); a disclosed
  registry non-entry plus two asserting tests (sim-emit-hl7, G3).
  `Active CarePlan` condition stays design-ruled, implementation-
  deferred per G2 (zero exercising modules).
- Fix-forward (same session): testing the `joint_replacement` fix live
  against the real `total_joint_replacement.json` closure surfaced a
  SECOND, independent blocker — `Joint_Replacement_Guard`'s own
  compound Age condition is outside this interpreter's own
  `age-guard-jump-days` analytical-resolution shape (bare `:age >= N
  years` only), so the walk blocks permanently at age 0. Declared D2
  vendoring scope revised to ZERO roots (ADR-0029's own G4 explicitly
  permits this outcome) — the CarePlan mechanism itself stands as
  real, tested infrastructure regardless, the same "build the
  mechanism, defer the vendoring target" shape `VitalSign` (D1a)
  already established.
- Regression oracle: all seven pre-existing vendored-root test
  namespaces held byte-for-byte identical test/assertion counts across
  every checkpoint (a disclosed full-suite comparison method, not a
  literal SHA-256 digest — ADR-0029's own dated note has the reasoning).
- Commits, in order: `a41d8c2` (Step 0), `0131985` (Step 1),
  `7319680` (Step 2a, sim-model), `efe1972` (Step 2b, sim-trajectory),
  `c1dee3d` (Step 2c, sim), `b499efc` (Step 2d, sim-emit-hl7),
  `85c75de` (fix-forward, revised scope), and this session's own
  closing records commit. `notes/ADRs.md` ADR-0029's own D2
  characterization/execution/deviation notes; `.agents/session-records/
  2026-08-02-gmf-coverage-wave-d-stage-d2.md`.

## Done (this session, 2026-08-02, GMF coverage Wave D stage D3 -- Wave D CLOSED)
- Three named mechanisms, H1-H8 ruled and landed in full: `lookup_
  table_transition` (the sixth GMF transition kind, H2) plus closure
  DATA-FILE members (R4, `ehrt.sim-trajectory.gmf/load-closure`'s own
  `table-resolve-fn`); attribute-weighted `distributed_transition`
  weights (H3, proven against a hand-authored fixture -- `stroke.json`
  stays deferred, the mechanism landing is only half its own revisit
  trigger); `age-guard-jump-days` extended under a sound-jump-or-
  escalate rule (H4, `total_joint_replacement.json`'s own compound
  `Joint_Replacement_Guard` unblocked, permanently blocked at age 0
  since Wave D stage D2's own fix-forward finding).
- **Both stages' own outstanding payoffs land as vendored roots**:
  `urinary_tract_infections.json` (the SEVENTH vendored module, this
  project's SECOND closure -- twelve files -- and FIRST data-file
  closure members, two lookup-table CSVs) and `total_joint_replacement.
  json` (the EIGHTH vendored module, THIRD closure -- four files).
  Interpreter-layer proof only for both (`ehrt.sim-trajectory.vendored-
  uti-test`/`vendored-tjr-test`) -- the SAME standing, already-
  disclosed full-pipeline gap `ear_infections.json`'s own vendored test
  already carries (confirmed by direct search: no full compile-
  trajectory/engine/emit round trip exists for ANY closure-having
  module vendored to date), not newly introduced this session.
- Three disclosed deviations, each a real finding surfaced testing the
  ruled mechanisms/vendoring against real content, none reopening H1-H8's
  own design: four cheap mechanical loader/interpreter additions
  (`gmf_version` 2 timing encoding, `SetAttribute` `value_code`, a
  fourth observation value mechanism `:exact`, seven new vital-sign
  table rows); a real interpreter BUG fix (`first-matching-entry`'s own
  missing fallback-to-last-entry semantic, matching real Synthea's
  `ConditionalTransition`/`ComplexTransition.follow` exactly -- isolated
  into its own commit since it changes shared dispatch logic, confirmed
  to change nothing for any already-vendored root); three more
  UTI-specific loader findings (a new `virtual` encounter-class value,
  `complex_transition`'s own either/or, a real upstream CSV
  byte-order-mark).
- Regression oracle: the full non-integration suite (192 namespaces)
  green at every checkpoint, 0 failures/0 errors; every pre-existing
  vendored-root test namespace (sinusitis/appendicitis/sore_throat/
  ear_infections/sepsis/death-fixture, plus sim-emit-hl7's own
  emission-layer suites, HL7 bytes included) held IDENTICAL test-count/
  assertion-count/zero-failures throughout, the same disclosed
  full-suite comparison method ADR-0029's own D2 note first established.
- **GMF coverage Wave D is CLOSED as of this session** (D0 sim-split
  S3 extraction, D1 observation family + `sepsis.json`, D2 CarePlan
  mechanism, D3 this entry) -- full retrospective, payoff tally, and
  standing named items in `.agents/plans/2026-08-02-gmf-coverage-
  plan.md`'s own close-out section. S4 (`sim-engine` split) trigger
  status: NOT fired -- `emit-state` remains the sole direct reader of
  `PatientState` across every Wave D stage.
- Commits, in order: `07ff1d5` (Step 0, H1-H8 + roadmap), `074d4d7`
  (Step 1, characterization), `ea85852` (Step 2a, H2), `af89d0e`
  (Step 2b, H3), `91c9bfd` (Step 2c, H4), `5d87388` (disclosed
  mechanical additions), `fdd0644` (disclosed bug fix,
  `first-matching-entry`), `4d9178b` (disclosed UTI loader findings),
  `8dcec56` (Step 3, UTI vendored), `430edbb` (Step 3, TJR vendored),
  and this session's own closing records commit. `notes/ADRs.md`
  ADR-0029's own D3 characterization/execution/deviation/H8 notes;
  `.agents/session-records/2026-08-02-gmf-coverage-wave-d-stage-d3.md`.

## Done (this session, 2026-08-02, post-Wave-D cleanup)
- **Oracle byte-verification (J1) — CLOSED, upgraded to byte-verified.**
  `bin/regression-oracle` (standing equipment, `bin/oracle-src/ehrt/
  oracle/digest.clj`) built and run against three commits in disposable
  worktrees: `bbeceb6` (D1b close-out) -> `d23fa9b` (D2 close-out) ->
  `7257775` (D3/Wave-D close-out). IDENTICAL SHA-256 digests on all six
  pre-existing vendored roots across BOTH spans -- the required D3
  check and the optional D2 extension both ran, since the D2 baseline
  was cheaply reproducible. Dated notes on `notes/ADRs.md` ADR-0029's
  own D2 and D3 sections upgrade both stages' regression-oracle claim
  from count-verified to byte-verified; the byte-verified chain now
  runs unbroken from D1b's own literal digest through `7257775`.
- **Oracle doctrine (J2) — CLOSED.** `build-session/SKILL.md` (both
  `.agents/` and `.claude/` mirrors) gains a VERIFICATION section:
  a regression-oracle claim means `bin/regression-oracle`'s own
  output, never a test/assertion-count comparison. `AGENTS.md` gets
  the one-line pointer.
- **Dual-clone guardrails (J4) — CLOSED, all five parts.** `/mnt/c`
  (`C:\Users\prags\Documents\ehr-testing-tools`) is now read-only
  (attrib.exe +R, whole tree including `.git`) with reject-all
  pre-commit/pre-push hooks in its own `.git/hooks` (per-clone,
  uncommitted, `core.hooksPath` unset there so they actually fire).
  `bin/sync-mnt-c` is the ONLY sanctioned way that clone moves --
  already run once to fast-forward it and bootstrap the lock.
  `build-session/SKILL.md` gains a preflight step (resolve both clone
  roots, edit target must resolve under the ext4 root). Every guard
  demonstrated firing for real: a blocked `Edit` (`EPERM`), a blocked
  `git commit` (`REJECTED`, exit 1), the pre-push hook standalone.
  Whether `/mnt/c` still earns its keep at all is named as an author
  question in Externals, above -- not decided this session.
- **Closure engine round-trips (J3) — CLOSED, three findings, no
  fixes.** `components/sim-emit-hl7/test/`'s three new vendored round-
  trip tests (`ear_infections`/`urinary_tract_infections`/
  `total_joint_replacement`) each PIN a confirmed, real engine gap
  rather than proving the round trip works: `engine.clj`'s own
  `:registered` decide method calls `run-module` at a bare arity that
  never threads a closure's own submodule registry through (ear_
  infections/UTI: every walk that reaches a `CallSubmodule` state
  throws) nor an `initial-attributes` seed (TJR: the walk blocks
  permanently at age 0, silently producing zero compiled content).
  H6's own "a full engine/check run" instruction, disclosed as never
  fulfilled since Wave B, was actually TRIED this session for the
  first time -- and found genuinely broken, not merely untested. Named
  in Deferred, below; NOT fixed under this session's own tests-only
  ruling (ADR-0030 J3).
- `notes/ADRs.md` ADR-0030 (this session's own rulings J1-J5, execution
  note, dated notes on ADR-0029's D2/D3).
- Commits, in order: `64e250f` (Step 0, ADR-0030 + roadmap), `56c7cef`
  (Step 1, oracle harness + verification), `31e8460`/`4eecd3f`
  (exec-bit fix-forwards), `cd76334` (concurrent author commit,
  bundled J2's own doctrine changes plus an unrelated new plan file --
  split apart per staging hygiene rather than force-pushed over, see
  the session record), `71093d5` (small index/budget fix-forward),
  `00c32f8` (Step 3, dual-clone guardrails), `9a2514f`/`46f066d`/
  `093d321` (Step 4, one closure round-trip test per root), and this
  session's own closing records commit. Session record:
  `.agents/session-records/2026-08-02-post-wave-d-cleanup.md`.

## Done (this session, 2026-08-03, Procedure-duration fix — ADR-0031 AR-6, first defect-fix)
- **The fix (D3c finding 1, closed):** `emit-and-advance`
  (gmf-interpreter.clj) now wraps a Procedure's flat `{:low :high
  :unit}` `:duration` as `{:range duration}` before calling
  `resolve-time-advance` — the one-line call-site fix ADR-0032 AR-2
  ruled, proven red (0 advance, 0 draws) before and green after by a
  focused test (`gmf_interpreter_test.clj`). Full non-integration suite
  (`clojure -M:poly test :all skip:integration`) stayed 0 failures/0
  errors throughout; no existing test encoded the zero-advance
  behavior, so no test triage was needed. `engine.clj` and the three
  J3 pinned round-trip tests were not touched — that is AR-6's SECOND,
  separate defect-fix session.
- **Oracle bracket, with a real escalation resolved mid-session:**
  `bin/regression-oracle dc7b371 1ea1f4a` found `appendicitis`/
  `sepsis` changed as predicted (AR-4), but ALSO found `death-fixture`
  changed — one of AR-4's own five "must stay identical" roots,
  triggering that paragraph's own STOP-AND-ESCALATE clause literally.
  Root cause, found reading the fixture directly: `death-fixture.json`'s
  own `Stabilization_Procedure` state carries a genuine duration-bearing
  `:duration` (`{:low 30 :high 30 :unit "minutes"}`) that AR-4's own
  three-root survey never enumerated — the fix behaved exactly as
  designed; the pre-session survey was incomplete. Author-ruled
  (mid-session): `death-fixture` reclassified from the identity set
  into the duration-bearing set (now four), its new digest accepted as
  baseline — a dated correction on ADR-0032's own AR-4, not a silent
  re-baseline. Separately found: `bin/oracle-src/ehrt/oracle/digest.clj`
  (the post-Wave-D cleanup session's own equipment) has never covered
  `total_joint_replacement`/the UTI closure at all (a pre-existing
  six-root scope, not this session's own gap) — author-ruled to
  disclose only this session, corroborated instead by the green full
  suite (including both roots' own vendored tests and the three J3
  pins) and a direct read confirming TJR has no duration-bearing
  Procedure state at all (v1 or v2). Full digest table in the session
  record.
- `notes/ADRs.md` ADR-0032 (this session's own AR-1 through AR-5
  rulings, the AR-4 dated correction, and the execution note).
- Commits, in order: `1ea1f4a` (Step 1, the fix + focused test),
  `7587d1d` (Step 2, ADR-0032 capture), and this session's own closing
  records commit (Step 4 — the oracle bracket, Step 3, made no commit
  of its own, evidence only). Session record:
  `.agents/session-records/2026-08-03-procedure-duration-fix.md`.

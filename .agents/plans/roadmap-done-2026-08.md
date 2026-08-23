# Roadmap Done history — 2026-08 (moved verbatim from .agents/plans/roadmap.md by scaffolding compaction B)
Attic file — every entry below is an exact byte-for-byte relocation from the live roadmap's own Done section, moved 2026-08-05; see .agents/plans/roadmap.md for the live Now/Next/Deferred/Done.

## Done (this session, 2026-08-05, scaffolding compaction A — riders, vestige retirements, Deferred triage — ADR-0045)
- **Riders (AR-A-1/AR-A-2).** `census_test.clj`'s two "the
  roadmap's own Wave I finding" citations (lines 12, 41) corrected
  to ADR-0044's own citation fix (the invisibility claim stands on
  live before/after `poly test` evidence, not a roadmap row that
  was never found). Budget-numbers closure note added (below).
- **Vestige retirements (AR-A-3/AR-A-4).** `ehrt.corpus.sim-
  adapter`'s legacy discovery-key tolerance (`:sim-dir`,
  `:env-sim-dir-fn`, `:default-dir`) retired — fresh grep this
  session found zero external callers (the two `generators_test.clj`
  hits are prose, not passed opts); `sim_adapter_test.clj` updated
  to the current contract. `intake_test.clj`'s `sample-manifest`
  fixture's `:generator :name` corrected from `"ehr-testing-sim"`
  (the dead sibling repo) to `"ehrt.sim"` (`ehrt.sim.manifest/
  build`'s own real stamp, confirmed by fresh read).
- **Deferred triage (AR-A-5).** Four rows already carrying a
  closure note relocated verbatim from Deferred (precedent: prior
  sessions' own "S3/S4 rows moved to Deferred" pattern, now run in
  reverse — closed rows move OUT of Deferred):

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
  **CLOSED 2026-08-05 (standing-equipment promotion, `notes/ADRs.md`
  ADR-0044 AR-P-3):** digest.clj is now `components/oracle`, a real
  Polylith component -- `bin/regression-oracle` points each side's own
  synthetic classpath at that worktree's own copy, so this row's own
  workaround (hand-running each commit's own digest.clj against its own
  worktree) is now the SCRIPT's own default behavior, not a manual
  fallback. The revisit trigger above never fires again for this reason;
  a NEW, narrower transitional gap replaces it (a bracket spanning a
  ref from before this promotion, `components/oracle` absent there)
  with its own fallback and retirement condition, `bin/regression-
  oracle`'s own `oracle_wiring_for` function.

- **Docs coherence pass** (2026-08-04, sim split B M4, `notes/ADRs.md`
  ADR-0043 AR-M4-7): the sim split B arc's own per-stage stale-path
  sweeps (S1-S4/M1-M4, each a manual, disclosed, live fix) have each
  individually found current-tense namespace citations in
  component-owned `docs/` trees that `ehrt.docs-tooling.stale-path-
  test` structurally cannot gate (component-owned docs are out of its
  scan scope by design). No single session owns systematic coverage
  of that surface. Revisit trigger: a future session that wants one
  pass across every component's own `docs/` tree rather than
  continuing the per-extraction manual-sweep pattern — named as the
  cleanup arc's own likely next front, not committed or scoped here.
  **EXECUTED (2026-08-05, docs coherence pass, ADR-0043's own tail):**
  `gmf-interpreter.md` split (living reference / dated findings
  trail) and its own sections 1-8 currency pass;
  `patient-state-model.md`'s history-phase/vital-register additive
  sections and errata sweep. See Done, below.

  Three STALE-AUDIT rows closed this session on fresh evidence
  (grep/read commands recorded in the session record), not asserted:

- **`Active CarePlan` (condition type)** — was design-ruled,
  implementation-deferred (Wave D stage D2's own G2: no vendored
  module exercised it yet). **CLOSED (compaction A, AR-A-5
  STALE-AUDIT, fresh-grepped this session):** landed for real GMF
  coverage Wave I2 (2026-08-04, `notes/ADRs.md` ADR-0041 AR-2,
  commit `14e8dce`) — `ehrt.sim-trajectory.gmf-interpreter/active-
  careplan-condition-holds?` implements Synthea's own `Logic.java`
  `ActiveCarePlan` dispatch; confirmed present in the live
  interpreter this session (`components/sim-trajectory/src/ehrt/
  sim_trajectory/gmf_interpreter.clj` lines 609-812). This roadmap
  row was the last place still calling it deferred — the docs
  coherence pass (2026-08-05, ADR-0043 AR-D-1) had already fixed
  the same stale claim in `gmf-interpreter.md`'s own sections 1-8;
  this closure brings the roadmap into agreement.

- **Lookup-table column `race`** (ADR-0036 AR-7, deferred to
  Wave I). **CLOSED (compaction A, AR-A-5 STALE-AUDIT, fresh-
  grepped this session):** `ehrt.sim-model.persona`'s optional
  `:race` field is live (`components/sim-model/src/ehrt/sim_model/
  persona.clj` line 122); GMF coverage Wave LC (2026-08-03,
  ADR-0038) generalized the lookup-table column boundary and the
  census confirmed `acute-myeloid-leukemia` (the race-blocked
  module) moved `:load-failed` → `:ok-walked` along with the other
  8 modules in that wave's own batch. The original row's `time`
  component does NOT close alongside it — see the slimmed `time`
  row still standing in Deferred, above, and its own author-ruling
  note.

- **Reading-set budget numbers** (charter §6: rule after real
  sizes are measured). **CLOSED (2026-08-05, docs coherence pass,
  `notes/ADRs.md` ADR-0043 AR-D-3, relocated compaction A AR-A-2):**
  every reading set's budget recomputed as actual × 1.15 (rounded
  up to the nearest 5), one dated note replacing 14 accumulated
  bump comments: `:onboarding` 2090→2405, `:corpus` 1731→1995,
  `:sim` 793→915, `:judge` 851→980, `:docs` 671→775. This Deferred
  row had no closure note of its own until this session found the
  gap and added one.

## Done (this session, 2026-08-05, standing-equipment promotion — ADR-0044)
- **Census promotion (Step 1, `a17fab1`, AR-P-1).** `ehrt.sim-trajectory.
  census` and its 7-test co-landing suite move verbatim from
  `development/{src,test}` into `components/sim-trajectory` — equipment,
  not API, the interface unchanged. Real finding, disclosed, fixed
  forward: running these tests under `poly test` for the first time
  ever surfaced two stale fixtures (GMF coverage Wave VS had landed real
  `VitalSign`/`:vital-sign` support since this test file was last
  actually exercised) — swapped to deliberately fictional type names,
  immune to the next coverage wave going stale the same way.
- **Oracle component (Step 2, `c065cdd`, AR-P-2).** `components/oracle`
  created; `ehrt.oracle.digest` moves out of `bin/oracle-src` (retired)
  with four interface repoints (`ehrt.sim-trajectory.interface`, gaining
  `dob-epoch-day`; `ehrt.sim-engine.interface`; unchanged
  `ehrt.sim-model.interface`/`ehrt.sim-emit-hl7.interface`). Verified
  byte-identical against the pre-promotion producer before committing.
  Documented in `AGENTS.md`/`docs/dev/architecture.md` (structure-
  currency gate caught its own absence, red then green, live).
- **Script redesign (Step 3, `3da479e`, AR-P-3).** `bin/regression-
  oracle` resolves each side's own worktree copy of `components/oracle`
  now (J2 closes structurally, dated note above and on ADR-0030 J2
  itself); a cross-side soundness check (diff outside the digest's own
  `(ns ...)` form) gates any non-trivial producer change behind an
  explicit `--declared-digest-change` flag; a transitional fallback
  reads a pre-promotion worktree's own `bin/oracle-src` instead. Proven
  red→green four ways (two same-ref brackets, a no-flag abort, the same
  bracket with the flag) before committing.
- **Verification (Step 5).** AR-P-5's own declared transitional split
  bracket, `bin/regression-oracle f9830ec 3da479e --declared-digest-change`:
  soundness check correctly reports DIFFERS outside the `(ns ...)` form
  (the printed diff is exactly the AR-P-2 interface repoints, nothing
  else); all ELEVEN batches byte-identical, expected-change set NONE.
  `poly check` clean and the full suite green (204 `Test results:`
  blocks, 0 failures/0 errors, both lanes) at every code commit.
- `notes/ADRs.md` ADR-0044 (AR-P-1 through AR-P-5, full execution note,
  a dated amendment on ADR-0030's own J2 entry). Session record:
  `.agents/session-records/2026-08-05-standing-equipment-promotion.md`.

## Done (this session, 2026-08-05, docs coherence pass — ADR-0043 AR-D)
- **AR-D-1 (the split).** `components/sim-trajectory/docs/gmf-
  interpreter.md` (3,668 lines, two strata) splits: sections 1-8 +
  appendix + ratification record stay as the living reference (1,428
  lines); sections 9-16 (eight dated GMF-coverage wave sections)
  move VERBATIM to the new `gmf-interpreter-findings.md`, replaced by
  a one-line-per-wave pointer index (extraction diff-verified byte-
  identical). Sections 1-8's own currency pass found two genuinely
  contradicted claims (the H2 lookup-table column whitelist, retired
  Wave LC/ADR-0038; `Vital Sign`/`Active CarePlan` conditions, landed
  Wave VS/ADR-0039 and Wave I2/ADR-0041) and fixed both with inline
  dated notes, plus repointed 25 internal `§9`-`§16` self-citations
  the split itself broke.
- **AR-D-2 (patient-state-model.md).** Two additive sections: the
  history phase (GMF-sourced patients, ADR-0042) and the vital-sign
  register (GMF Wave VS, ADR-0039), explicitly disambiguated from this
  document's own pre-existing "log is `Person.history` done right"
  discussion. Errata-sweep procedure applied to Step 0's candidate
  list; no contradicted claims found in this document itself (both
  re-verified against live `engine.clj` code) — the two sections are
  purely additive. The M6 accumulator-field gap (`:discharged-at`/
  `:conditions`/`:observations`/`:medication-orders`/`:care-plans`/
  `:merged`, present in code, absent from this document) is disclosed,
  named a future, not fixed (out of AR-D-2's own scope).
- **AR-D-3 (budget re-baseline).** Every reading set's budget
  (previously the exact measured actual, bumped in place 14 times
  across the arc) recomputed as actual × 1.15, rounded up to the
  nearest 5, one dated note replacing the 14 accumulated bump
  comments: `:onboarding` 2090→2405, `:corpus` 1731→1995, `:sim`
  793→915, `:judge` 851→980, `:docs` 671→775. `:paths` membership
  unchanged in every set.
- **AR-D-4/5/6 (M4 verification riders).** `explain-profiles`
  (`order_profiles.clj`, zero callers, fresh-grepped) deleted,
  enforcing AR-M4-5(a) as originally ruled. `notes/ADRs.md` ADR-0043
  gains two dated tail notes: the façade docstring's annotate-over-
  delete treatment RATIFIED (AR-D-5); the M1-M3-vs-M4 parity-ledger
  counting-definition gap disclosed, conservation verified under both
  definitions (AR-D-6).
- **Verification.** `bin/regression-oracle 0986a86 <session tip>`: all
  ELEVEN vendored-root batches byte-identical. Deftest parity and the
  sim façade seam: trivially unchanged (zero test files, zero façade
  files touched this session — confirmed by diffstat, not merely
  asserted). `clojure -M:poly check`: OK throughout; `clojure -M:poly
  test`: 0 failures/0 errors, run fresh after every code-adjacent
  step. `notes/ADRs.md` ADR-0043's own tail (AR-D-4/5/6 dated notes,
  above).
- Commits, in order: `e6a0b28` (Step 1, the split), `ed84c8d` (Step 2,
  sections 1-8 currency pass), `66c98f4` (Step 3, patient-state-model
  additive sections + errata sweep), `9e3709c` (Step 4, budget
  re-baseline), `2a94144` (Step 5, the three riders), and this
  session's own closing records commit (Step 6). Session record:
  `.agents/session-records/2026-08-05-docs-coherence-pass.md`.

## Done (this session, 2026-08-04, sim split B M4 — `sim-check` lands, arc COMPLETE — ADR-0043)
- `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED, AR-1..AR-6) M4
  of four executed, LAST stage: `components/sim-check` created,
  `check.clj` (571 LOC — the invariant catalog) moved verbatim out of
  `components/sim`, `ehrt.sim-check.interface` designed from fresh
  call-position grep (`check-all`, all four arities — no delta from
  the design channel's own candidate list). `c43f7cc`.
- `ehrt.sim.interface`'s own fat-component disclosure docstring gains
  a dated closing note: the extraction it disclosed (S1/S2/S3/M1/M2/
  M3/M4) is complete, residual sim is pure orchestration behind the
  SAME façade, unchanged in width or shape (08-02 plan AR-3, honored
  not revisited).
- Five parked findings disposed (AR-M4-5): `explain-profiles` (zero
  callers, left in place, disposal not deletion); `:coverage` alias
  gains sim/sim-engine/sim-emit-fhir/sim-check (a gap standing since
  M2); `docs/dev/architecture.md`'s mermaid diagram gains the
  `provenance` node/edges M1 missed; the emit-state demo README's
  stale `ehrt.sim.emit-hl7` bare-cite (M3-disclosed) fixed forward;
  `components/sim/deps.edn` drops malli/babashka.cli (fresh grep found
  zero real users of either). `e948296`.
- `emitter_order_independence_test.clj` moves to
  `components/sim-emit-hl7/test` (AR-M4-4) — emit-hl7's own
  determinism guard, misplaced only because check.clj hadn't moved out
  yet when it was written.
- Stale-path sweep: `ehrt.docs-tooling.stale-path-test`'s
  retired-namespace family gains `ehrt.sim.check` (namespace and path
  form) — no real violations in the gate's own scan scope; four
  current-tense surfaces outside that scope (component-owned `docs/`,
  plus four src/test docstrings) swept forward anyway. `56b62a7`.
- **Arc COMPLETE**: the five-brick decomposition plan AR-1 named
  (`sim-engine`, `sim-emit-fhir`, `sim-check`, `provenance`, residual
  `sim`) is landed in full across M1-M4, all same-day, every stage
  oracle-proven byte-identical. See `notes/ADRs.md` ADR-0043's own M4
  execution record for the arc-complete statement and the standing
  deferred items re-cited (J2 oracle redesign, carry-across emission,
  sim-cli retirement (closed), census-tool refinements, the new docs
  coherence pass row above) — none re-opened.
- `clojure -M:poly check` clean and full suite green at every one of
  the three commits above (0 failures, 0 errors, both projects, 202
  Test-results blocks); normal-mode regression-oracle bracket / façade-
  seam / deftest-parity verification recorded in the session record.

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

## Done (this session, 2026-08-05, alignment riders — ADR-0048)
- **Stray Deferred row relocated (AR-R-4).** One row already carrying
  its own closure note, relocated verbatim from the live roadmap's
  Deferred section (notes intact, relocation not rewrite — the same
  AR-A-5 discipline applied to a row AR-A-5's own sweep missed). The
  drift was originally disclosed 2026-08-05 in `notes/adr/0047-
  scaffolding-compaction-c.md` finding 5 and `.agents/state.md`: this
  row carried an in-place "RESOLVED... see Done, below" note rather
  than having actually been relocated, a pre-existing drift from
  2026-08-03 predating compaction A's own AR-A-5 sweep (the closure
  note sat mid-paragraph, not as a standalone closed-with-note row the
  way AR-A-5's four did relocate).

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

## Scaffolding-compaction arc — closed 2026-08-05, rotated 2026-08-06 (ADR-0045–0047; rotation deferred at its own close, disclosed in ADR-0055)
Relocated verbatim from the live roadmap's own Done section by this session's
own rotation (`.agents/plans/2026-08-06-ux-arc-brief.md` R2; the leftover was
named but not swept at the alignment arc's own close, `notes/adr/0055-
alignment-arc-close.md`'s own scope-precision disclosure, AR-AC-5).
- 2026-08-05 — scaffolding-compaction-a — ADR-0045
- 2026-08-05 — scaffolding-compaction-b — ADR-0046
- 2026-08-05 — scaffolding-compaction-c — ADR-0047

## Alignment arc — closed 2026-08-05 (ADR-0048–0055)
Relocated verbatim from the live roadmap's own Done section by this arc's
own close (`notes/adr/0055-alignment-arc-close.md` AR-AC-5); see that ADR
for the arc's full disposition tally.
- 2026-08-05 — alignment-riders — ADR-0048
- 2026-08-05 — alignment-audit — ADR-0049
- 2026-08-05 — alignment-fixes-1 — ADR-0050
- 2026-08-05 — alignment-fixes-2 — ADR-0051
- 2026-08-05 — alignment-fixes-3 — ADR-0052
- 2026-08-05 — alignment-fixes-4 — ADR-0053
- 2026-08-05 — alignment-fixes-5 — ADR-0054
- 2026-08-05 — alignment-arc-close — ADR-0055

**Appended 2026-08-06 (UX arc close, `notes/adr/0064-ux-arc-close.md`
AR-UC-5):** this pointer was the live roadmap's own sole current Done
entry from the alignment arc's own close until now — AR-AC-5's own
ruling named only ADR-0048 through ADR-0054 for relocation, leaving
ADR-0055's own pointer in place as "the sole current entry FOR THE
ALIGNMENT ARC" at that session's own landing (`notes/adr/
0055-alignment-arc-close.md`, its own AR-AC-5 disposition). It
relocates here now, verbatim, at the next arc's own close — the same
disclosed-leftover class ADR-0055 itself named for the
scaffolding-compaction pointers above.

## UX arc — closed 2026-08-06 (ADR-0056–0064)
Relocated verbatim from the live roadmap's own Done section by this
arc's own close (`notes/adr/0064-ux-arc-close.md` AR-UC-5); see that
ADR for the arc's full disposition tally.
- 2026-08-06 — ux-riders — ADR-0056
- 2026-08-06 — tag-law — ADR-0057
- 2026-08-06 — ux-audit — ADR-0058
- 2026-08-06 — ux-fixes-1 — ADR-0059
- 2026-08-06 — ux-fixes-2 — ADR-0060
- 2026-08-06 — ux-fixes-3 — ADR-0061
- 2026-08-06 — ux-fixes-4 — ADR-0062
- 2026-08-06 — ux-fixes-5 — ADR-0063

**Appended 2026-08-07 (player arc close, `notes/adr/
0068-player-arc-close.md` AR-PC-5):** this pointer was the live
roadmap's own sole current Done entry from the UX arc's own close
until now — the same disclosed-leftover class ADR-0055's own append
(above) already named. It relocates here now, verbatim, at the next
arc's own close.
- 2026-08-06 — ux-arc-close — ADR-0064

**Also appended 2026-08-07 (player arc close, AR-PC-5):** ADR-0065
(the UX epilogue) joins this section too, not the player arc that
follows it — it patched the UX arc's own surface (the `clojure -M:cli`
runtime tombstone, the `--width`/COLUMNS affordance), so it rests with
the arc it patched. Not a disclosed leftover of the same class as the
row above — its own Done pointer is relocating from the live roadmap's
Done section for the first time now, in the same commit as the player
arc's own rotation.
- 2026-08-06 — ux-epilogue — ADR-0065

## Done (this session, 2026-08-06, ux epilogue — Deferred triage AR-EP-4 — ADR-0065)
- **Deferred triage (AR-EP-4).** Two rows already carrying a
  closure note relocated verbatim from Deferred, same sanctioned-append
  class as compaction A's own AR-A-5 and ADR-0064's own AR-UC-5 — their
  own "see Done, below" pointers had dangled since the Done rotation
  (scaffolding compaction B, ADR-0046) moved the arcs they pointed at
  out from under them:

- `ehrt.sim-trajectory.gmf-interpreter/resolve-time-advance`'s own
  Procedure-duration gap: `:duration` is passed as a flat map but
  `resolve-time-advance` destructures nested `:range`/`:exact` keys
  from it, finding neither — EVERY vendored Procedure state's own
  duration silently never advances virtual time, v1 or v2 gmf_version
  alike (found live, Wave D stage D3, `docs/gmf-interpreter.md` §14's
  own D3c finding 1). **FIXED (2026-08-03, ADR-0031 AR-6's first
  defect-fix session, `notes/ADRs.md` ADR-0032) — see Done, below.**
  **Relocated 2026-08-06 (ux epilogue, AR-EP-4).**
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
  **Relocated 2026-08-06 (ux epilogue, AR-EP-4).**

## Player arc — closed 2026-08-07 (ADR-0066–0068)
Relocated verbatim from the live roadmap's own Done section by this
arc's own close (`notes/adr/0068-player-arc-close.md` AR-PC-5); see
that ADR for the arc's full account. Two sessions: the fold (ADR-0066,
total over the emitter's real trigger set, self-anchored in absolute
epoch millis) and the board (ADR-0067, the `--board` whiteboard, the
`corpus`→`sim-emit-hl7` edge, and the rider that made the suite green
on a fresh clone for the first time since ADR-0060).
- 2026-08-06 — player-fold — ADR-0066
- 2026-08-07 — player-board — ADR-0067

**Appended 2026-08-07 (vendoring arc close, `notes/adr/
0074-vendoring-arc-close.md` AR-VAC-5):** this pointer was the live
roadmap's own sole current Done entry from the player arc's own close
until now — the same disclosed-leftover class every prior close has
handled for its own predecessor. It relocates here now, verbatim, at
the next arc's own close.
- 2026-08-07 — player-arc-close — ADR-0068

## Vendoring arc — closed 2026-08-07 (ADR-0069–0074)
Relocated verbatim from the live roadmap's own Done section by this
arc's own close (`notes/adr/0074-vendoring-arc-close.md` AR-VAC-5); see
that ADR for the arc's full account. Five sessions: census substance
(ADR-0069, the honest catalog — 51 zero-on-every-seed / 33
produces-content), batch 1 (ADR-0070, five landed, `injuries.json`
deferred whole), batch 2 (ADR-0071, seven landed, `anemia___unknown_
etiology.json` deferred whole, the scenarios home born), batch 3
(ADR-0072, four landed, `colorectal_cancer.json` deferred whole, the
verbatim-law gate given teeth), demos front door (ADR-0073, the
operator surface moved to the root, "See it run" in the README).
Sixteen modules vendored, twenty-three ailments in-tree, oracle roots
11→27.
- 2026-08-07 — census-substance — ADR-0069
- 2026-08-07 — vendoring-batch-1 — ADR-0070
- 2026-08-07 — vendoring-batch-2 — ADR-0071
- 2026-08-07 — vendoring-batch-3 — ADR-0072
- 2026-08-07 — demos-front-door — ADR-0073

**Appended 2026-08-07/08 (quality-review arc close, `notes/adr/
0080-quality-arc-close.md` AR-QC-5):** this pointer was the live
roadmap's own sole current Done entry from the vendoring arc's own
close until now — the same disclosed-leftover class every prior close
has handled for its own predecessor. It relocates here now, verbatim,
at the next arc's own close.
- 2026-08-07 — vendoring-arc-close — ADR-0074

## Quality-review arc — closed 2026-08-07 (ADR-0075–0080)
Relocated verbatim from the live roadmap's own Done section by this
arc's own close (`notes/adr/0080-quality-arc-close.md` AR-QC-5); see
that ADR for the arc's full account. Five sessions: ci current
(ADR-0075, the bridge — 32 commits of unwatched red fixed, preflight
learns to look; **this bridge session seeded the arc it rides with
here, not the vendoring arc it followed** — the ux-epilogue precedent,
inverted), quality riders (ADR-0076, the `repo-review` skill lands, the
sibling flake gets its mechanism fix, preflight widens to five runs),
repo review 1 (ADR-0077, the first survey — 45 rows, eight lenses, the
baseline scoreboard), result or loud (ADR-0078, the highest-severity
cluster closed — `ehrt.kernel.io`, eleven sites converted, the
recurrence gate built), lint family (ADR-0079, four small gates land
together — the state can't stale, the façade can't drift, closures
can't hide, tests can't wander).
- 2026-08-07 — ci-current — ADR-0075
- 2026-08-07 — quality-riders — ADR-0076
- 2026-08-07 — repo-review-1 — ADR-0077
- 2026-08-07 — result-or-loud — ADR-0078
- 2026-08-07 — lint-family — ADR-0079

**Appended 2026-08-08 (fidelity arc close, `notes/adr/
0084-fidelity-arc-close.md` AR-FC-5):** this pointer was the live
roadmap's own sole current Done entry from the quality-review arc's own
close until now — the same disclosed-leftover class every prior close
has handled for its own predecessor. It relocates here now, verbatim,
at the next arc's own close.
- 2026-08-07 — quality-arc-close — ADR-0080

## Fidelity arc — closed 2026-08-08 (ADR-0081–0084)
Relocated verbatim from the live roadmap's own Done section by this
arc's own close (`notes/adr/0084-fidelity-arc-close.md` AR-FC-5); see
that ADR for the arc's full account. Three sessions: fidelity riders
(ADR-0081, the design brief re-verified field-for-field against
upstream, three rulings recorded — R1 openness-only wellness arms, R2
suppressed-end visibility, R3 the predict-then-confirm acceptance bar),
encounterend fix (ADR-0082, the interpreter's five upstream arms
collapse to the two this subset needs; a real, already-shipped
dangling reference caught by the blast-radius probe BEFORE any fix
code, traced, licensed, and fixed; a fifteen-minute CI-red window
disclosed rather than smoothed over), fidelity payoff (ADR-0083,
`anemia___unknown_etiology.json` vendors clean as the twenty-eighth
oracle root, `colorectal_cancer.json`'s misdiagnosis-by-adjacency
corrects to its own true, still-undiagnosed blocker).
- 2026-08-08 — fidelity-riders — ADR-0081
- 2026-08-08 — encounterend-fix — ADR-0082
- 2026-08-08 — fidelity-payoff — ADR-0083

**Appended 2026-08-08 (conviction arc close, `notes/adr/
0089-conviction-arc-close.md` AR-CB-3):** this pointer was the live
roadmap's own sole current Done entry from the fidelity arc's own close
until now — the same disclosed-leftover class every prior close has
handled for its own predecessor. It relocates here now, verbatim, at
the next arc's own close.
- 2026-08-08 — fidelity-arc-close — ADR-0084

## Conviction arc — closed 2026-08-08 (ADR-0085–0089)
Relocated verbatim from the live roadmap's own Done section by this
arc's own close (`notes/adr/0089-conviction-arc-close.md` AR-CB-3); see
that ADR for the arc's full account. Colorectal investigation
(ADR-0085, diagnosis-only per its own ruled fence — no Done pointer of
its own, the row it diagnosed stays in Deferred, closed in place with
its own disclosed relocation) localizes `colorectal_cancer.json`'s own
violations to `compile-trajectory`'s legacy pre-horizon drop gate, no
back-reference check against the straddling encounter it belongs to.
Straddle fix (ADR-0086) generalizes the Wave H `history-phase?`
back-reference principle to that legacy path — one licensed mover,
`sleep-apnea` (3 of 300 walks, a latent malformed compiled shape
shipped since vendoring batch 1), STOP-AND-REPORTed and confirmed
exactly; `colorectal_cancer.json` clean at all three seeds. Colorectal
payoff (ADR-0087) vendors it as the oracle's 29th root, pinned by a
committed test measuring the `:suppressed-straddle-spans` counter
against the same real straddling patients the investigation traced by
name. Pairing registry (ADR-0088) lands the mutate↔judge conviction
registry as data — seven witnessed rows across two v2 judges, a
names-only NIST taxonomy snapshot gated by a currency test, three
skipped pairs honestly named. Colorectal's own four-ADR, three-session
deferral (ADR-0072/0083/0085/0086/0087) closes.
- 2026-08-08 — straddle-fix — ADR-0086
- 2026-08-08 — colorectal-payoff — ADR-0087
- 2026-08-08 — pairing-registry — ADR-0088

## Register-compression arc, session B -- roadmap rows retired 2026-08-17 (ADR-0144)

Rows moved VERBATIM out of `.agents/plans/roadmap.md` by
`bin/roadmap-migrate-0144` when that file adopted the ADR-0144 row
contract. Every row below either closed (and was still sitting in
`## Next`, `## Externals` or `## Deferred`, which is the shape guard #1
now catches) or had no owning ADR to hold its overflow. Nothing is
summarised here; each row is its own text, unedited.

- **Event-log contract arc — CLOSED 2026-08-16 (`notes/adr/0141-event-log-contract.md`).**
  Author-ordered before latency realism (*"Choose a."*). The ground-truth event
  log is now a PUBLIC, VERSIONED contract: `ehrt.sim-engine.event-schema/Event`
  (21 kinds, source and a 4,997-event census reconciled exactly), exported as
  self-contained EDN, stamped into every manifest as `:event-schema-version`,
  documented by a GENERATED `docs/formats.md` section led by the nested-`:event`
  warning, and demonstrated by `bin/example-custom-emitter` behind a use-case
  page exercised from birth. Landed red-first; zero engine, emitter or
  vendored-byte changes. Any successor arc that changes the log's SHAPE now
  owes a version bump or an additive-only change, enforced by
  `event-schema-test` against a frozen baseline.
- **Event-log shape defects — REGISTER ROWS, ruled 2026-08-16** (*"S-1..S-5 and
  the Z-segment asymmetry stay register rows"*). Evidence and full write-up:
  `.agents/plans/2026-08-16-event-log-census.md` (§ Shape defects). Deliberately
  unfixed — describing current truth first, then changing it under the versioned
  contract, is the point of the tier. **S-1** module-compiled encounters carry
  `:reason` present-and-always-nil; **S-2** `referenced_by_attribute` care-plan
  closures never resolve their start, so no `:care-plan-start` ever closes (not
  the mechanism — a `careplan`-citing fixture resolves both); **S-4** the
  `:step-rejected` reason enum is 7 wide and the census saw 1 (not a defect,
  recorded so nothing narrows it to observation); **S-5** unrelated, seed 202
  under `--churn` exits `:self-check-failed`, reproducible; **S-6** `:units`
  plural on result entries vs `:unit` singular on observations, same concept;
  **Z-segment context asymmetry** a CONSUMER defect outside this arc's fences —
  `emit_hl7`'s ADT-family builder hands Z-templates a seven-key subset while
  every other family hands the whole event, silently. S-3 was withdrawn as
  correct behaviour on evidence.
- ~~**D8-5 live fence battery — its own session, BEFORE repo review
  4**~~ — **CLOSED 2026-08-16, DISCHARGED.** Ran at `30cc335` (the
  register, 102 files / 202 blocks / 58 bare fences executed one by
  one); ruled *"Accept recommendations."* and executed the same day by
  `notes/adr/0140-fence-battery-ruled-fixes.md` — six pages
  fixed-or-disclosed and one tool fix (`intake` distinguishes
  `:empty-input` and `:upstream-error` from `:malformed-mllp-frame`).
  The probe that lapsed across two consecutive reviews is closed on
  the record. **One row survives it into review 4's D2**: 56 of 74
  command fences still have no exerciser — ruled out of the fix
  session, handed on with its proposed rule quoted in ADR-0140.
- ~~**`state_staleness_tripwire_test` enumerates filenames, not arc
  closes**~~ — **CLOSED 2026-08-16** by the D8-5 fence battery session,
  which carried C-4 as a rider (opened 2026-08-15, ADR-0139 finding
  C-4; measured cost was **fifty ADRs**, 0090-0139, of `.agents/state.md`
  drift the gate never saw). Both fix options in the original row were
  taken rather than one: the population now reads each ADR's own first
  heading for "arc close", and a second assertion holds the filename
  convention to what the headings declare, so the two readings cannot
  drift apart again. **The row understated the defect** — enumerating
  by heading found **two** escaping files, not the one it named:
  `0047-scaffolding-compaction-c.md` (heading ends "arc closes") as
  well as `0125-manual-s5-chapter8-review-close.md`. Both renamed into
  the convention under an author ruling, 12 inbound references updated
  across 10 files; red witnessed on both before the rename.
- **Clinic-decade/ED scenario redesign — "A" LANDED, "B" CLOSED
  (B1 + B2 + B3, all landed 2026-08-11).** Anchored to the author's own
  2026-08-10 ED-direction ruling (`.agents/rulings.md`, "From
  ADR-0103"), verbatim: *"Maybe weight the patient population toward
  immediate, emergent conditions like trauma/injuries? This would
  simulate an actual ED, which is where a lot of the activity and
  churn would happen."* Chartering context from `notes/adr/0103-board-
  boundary-catchup.md`: the clinic-decade scenario's own current module
  mix (twelve everyday-ambulatory/acute modules, weighted toward
  milder complaints) produces genuinely sparse message traffic — 68
  messages, 200 patients, a ten-year horizon — most of it
  intake/follow-up unfolding over months, not a single busy shift; an
  ED-weighted mix would exercise `--board`'s own cadence far harder.
  The author's own 2026-08-10 "C-with-A-first" ruling split this into
  two halves: **A landed 2026-08-11** (`notes/adr/0104-ed-tuesday-
  scenario.md`) — a NEW sibling scenario, `demos/scenarios/
  ed-tuesday/`, a day-scale scripted single ED shift; `clinic-decade/
  config.edn` stays untouched, the population-scale contrast.
  **Correction (2026-08-11, `notes/adr/0105-interpreter-horizon-
  budget.md`): this row's own prior "B" text mis-characterized what B
  actually required.** It named B "a separate future batch under the
  standing vendoring ceremony... not a design pass, routine vendoring
  intake once scheduled" — but B's own cited mechanics, `notes/ADRs.md`
  ADR-0070, had already deferred `injuries.json` WHOLE on a real
  `gmf-interpreter` gap (`run-submodule` never receiving `horizon-
  end-t`, tripping `max-steps` at every horizon tried), naming its own
  revisit trigger as "a future session willing to extend gmf-
  interpreter's own runaway-loop handling" — an interpreter fix, not
  routine intake, was always B's own real prerequisite. **B1 (the
  interpreter fix) landed 2026-08-11** (`notes/adr/0105-interpreter-
  horizon-budget.md`): `run-submodule` now respects `horizon-end-t`
  the same way `run-module`'s own top-level loop does, and the
  `max-steps` runaway budget now counts only zero-time-advance steps
  (a second, coupled gap the same ADR's own arithmetic found: even a
  horizon-bounded LEGAL loop could trip the old every-step count on
  volume alone). **B2 (the injuries vendoring batch itself) ran
  2026-08-11 under a WIDENED, assessment-first charter** (`notes/adr/
  0106-injuries-b2-assessment.md`, the author's own "b" ruling): the
  fresh gate found ADR-0105's own fix complete (0/120 max-steps
  failures) but a SEPARATE, pre-existing `nested :encounter` assert
  still fires — `injuries.json`'s own `Spinal_Injury` branch opens a
  second `Encounter` state before closing its first — at 2/120
  well-mixed seeds (direct interpreter) and on a full 300-patient
  `engine/run`, uncaught, at the round-trip test's own standard
  parameters. Nothing vendored; the closure stayed deferred,
  RE-ANCHORED on this new blocker (`injuries.json` itself never had its
  own dedicated Deferred row below — only this Next-section B row and
  other modules' own Deferred rows cited its max-steps finding as
  precedent; that finding was already closed, ADR-0105, and this row
  was the anchor per AR-RL2-3).

  **B3 CLOSED 2026-08-11** (`notes/adr/0107-injuries-arc-close.md`,
  the author's own verbatim "Let's do (i)" ruling): ADR-0106's option
  (i), auto-close on reopen matching upstream exactly, landed in
  `gmf-interpreter.clj`'s own `:encounter` case — a reopen over a
  stale open now synthesizes an implicit `:encounter-end` for it
  first, upstream-faithful, rather than throwing. ON ITS GREEN, the
  injuries batch itself landed under the standing vendoring ceremony:
  `injuries.json`, `injuries/broken_jaw.json`, `snf/
  skilled_nursing_facility.json` (the 3 genuinely new closure members,
  5 already vendored from prior batches, re-verified byte-identical).
  This entire row's own arc (ADR-0070 deferral → ADR-0105 max-steps fix
  → ADR-0106 nested-encounter characterization → ADR-0107 fix and
  landing) is now FULLY CLOSED — no revisit trigger remains for this
  closure.
- **User manual design pass — LANDED 2026-08-12 (ADR-0119).** Renamed
  from "Tool-specific user-guide design pass" (ADR-0113 R1, author
  verbatim: *"Let's use the name 'user manual' for the user docs for
  ehr-testing-tools. I've been informally calling it the 'user guide'
  but that's too easy to confuse with the more general EHR Testing
  Guide that's in ehr-testing-guide repo."*). Shape ruled 2026-08-12
  (ADR-0113 R2, author "Q1 a. Q2 a. Q3 a."): chaptered `docs/manual/`
  as the narrative layer over the existing references, never
  duplicating them; ed-tuesday (`demos/scenarios/ed-tuesday/`) as the
  manual's one running scenario throughout. Framing landed this session
  (ADR-0119, channel-reconstructed "Q1 a. Q2 a. Q3 a." on "eight
  chapters, five sessions, exerciser at S2" — see `.agents/rulings.md`
  "From ADR-0119" R-M1/R-M2/R-M3): eight chapters across five sessions,
  chartered below as S1-S5; Chapters 3-8's own titles are this
  session's own disclosed working proposal
  (`docs/manual/00-front.md`), not yet ruled by name. The batch-straddle
  scenario is ruled "featured prominently" in the eventual manual
  (`.agents/rulings.md` "From ADR-0112", "Batch-straddle documentation
  placements") — Chapter 1 (landed this session) already excerpts it.
  **Naming-sweep rider (ADR-0113 R2), EXECUTED this session:** the
  repo-wide "user guide" census (docs/README/SETUP/demos/registers)
  found zero live-prose stragglers — every hit was an in-quote survivor
  (`notes/adr/0119-user-manual-skeleton.md`'s own census table).
  **Sequence (ADR-0113 R5):** review-3 -> CLI tweak sessions -> this
  design pass -> chapter sessions (below), the demo exerciser co-landed
  with the first chapter that cites a demo (S2) -> a manual-review
  skill (scoring rubric, run periodically, ADR-0113 R5) built at the
  manual arc's own close (S5).
  **S1 LANDED 2026-08-12 (ADR-0119):** skeleton (`docs/manual/00-front.md`),
  Chapters 1-2 (`01-what-this-is.md`, `02-setup-first-corpus.md`), the
  audience paring (R4) and learner-path riders.
  **S2 LANDED 2026-08-12 (ADR-0120):** Chapter 3
  (`docs/manual/03-a-simulated-hospital.md` — `sim run` and ed-tuesday,
  site profiles linked, scripted-versus-generative patients, the
  two-spaces story extended to `GT`'s own two emitters) co-landed with
  the demo exerciser (ADR-0113 R3 mechanism, landed as
  `bin/demo-exerciser-ed-tuesday` — quickstart-pattern-generalized,
  integration-tier, running ed-tuesday's own fenced commands in order,
  asserting exit codes plus every one of that README's own named
  invariants).
  **S3 LANDED 2026-08-12 (ADR-0121):** Chapter 4
  (`docs/manual/04-time-on-the-wire.md` -- `ehrt play`/`--board` pacing,
  the huge-rate-is-`show` and zero-offsets-is-plain-emit identity
  anchors, the latency second clock's MSH-7/EVN-2 split) and Chapter 5
  (`docs/manual/05-batch-delivery.md`, the arc's featured chapter --
  `ehrt corpus batch`'s own sim-independence ruling, the witnessed
  34-batch listing, Smith James (MRN000002)'s straddling encounter
  taught as the receiver-side "do I have all of this?" question, not a
  flag list). Two new hand-authored SVG figures
  (`docs/manual/assets/two-clocks.svg`, `straddle-timeline.svg`).
  Resequenced the arc's own working titles: the "realism you didn't
  script" slot earlier proposals had at Chapter 7 landed here instead,
  two chapters early; Chapters 6-8's own working titles updated
  in `docs/manual/00-front.md` accordingly (Mutate keeps Chapter 6,
  Gate Chapter 7, Check folds into Chapter 8 alongside verdict-reading
  at scale) -- disclosed as this session's own channel-inferred
  proposal, not yet ruled by name.
  **S4 LANDED 2026-08-13 (ADR-0124):** Chapter 6
  (`docs/manual/06-breaking-data-on-purpose.md` — mutation as named,
  traceable defect injection; choosing an operator by the contract you
  want proven rather than browsing the catalog; the inject-a-defect-
  expect-the-matching-finding loop closed with the `README.md`
  storefront-patient example, witnessed fresh this session) and Chapter
  7 (`docs/manual/07-judging.md` — the three gates at reader level;
  verdict semantics, `:no-verdict` taught as a genuinely distinct third
  answer, not a variant of pass or rejected; the dominance ordering).
  Two new hand-authored SVG figures
  (`docs/manual/assets/inject-expect-loop.svg`, `verdict-ranking.svg`).
  Every strip in both chapters re-derived by fresh regeneration this
  session against the live tree, byte-identical to its own witnessed
  source (`README.md`, `judge-tier-calibration-studies.md`,
  `profile-tier-hl7v2-conformance-gating.md`), no divergence found. A
  pre-existing, repo-wide `ADR-0010` citation drift was found while
  reading the driving prompt's own "verdict ranking... ADR-0010's
  register trace" pointer — `notes/adr/0010-documentation-doctrine.md`
  is titled "Documentation doctrine," not the verdict design the
  citation is used for throughout `docs/judge-calibration.md`,
  `docs/formats.md`, `docs/glossary.md`, and every `components/judge/`
  source/test file — disclosed in `notes/adr/0124-*.md` and followed as
  the sole established convention rather than fixed (out of this
  session's own fence), flagged for the author and a future
  errata-sweep session. Zero `src`/`test`/`demos` touched anywhere, the
  oracle holds pure identity across all 35 roots.
  **S5 LANDED 2026-08-13 (ADR-0125): the manual arc is CLOSED — see
  Done, below.** Chapter 8 (`docs/manual/08-your-own-data.md` —
  cataloging a corpus you didn't generate, content hashes and lineage,
  the received-date as real-world provenance; checking against
  expectations, golden equivalence and the per-file assertion
  vocabulary; baselining a repeatedly-gated corpus; closing pointers
  into `formats.md`/`locators.md` for the data-consumer path) landed
  first; `00-front.md` updated to state the manual complete and name
  Chapter 8's own landing commit as the manual's own currency commit.
  The `manual-review` skill (`.agents/skills/manual-review/SKILL.md`,
  chartered `.agents/rulings.md` "From ADR-0113" R5) landed second, plus
  its own first scored run
  (`.agents/plans/2026-08-13-manual-review-1.md`): eight dimensions,
  each graded pass/warn/fail with `file:line` evidence — **overall
  verdict FAIL.** Two dimensions failed on real, repeat-pattern
  evidence, not edge cases: **strip executability** (Chapters 6, 7, and
  2 of 3 strips in the just-landed Chapter 8 cite a
  `docs/use-cases/*.md` page or README's own separate "What you get"
  fence, neither covered by the demo exerciser or `quickstart-fresh` —
  nothing mechanical catches these going stale between sessions); and
  **glossary linkage** (only Chapters 2 and 8 link `glossary.md` on
  first use of a defined term — Chapter 3 uses "Pathway" and "script
  space"/"truth space," the exact colliding-meaning terms the glossary's
  own front matter calls "the single most common way to misread a page
  here," with zero link to it anywhere in the chapter). Both findings
  are register rows, not fixes, per the skill's own review discipline —
  see the two new Next-section rows below. Author-ruled disposition,
  2026-08-13: close the arc now, land both findings as open backlog
  rows for a future fix session, per this session's own STOP-AND-REPORT
  and the author's own choice among the offered dispositions.
  **SETUP.md's unspoiled-human-reader rewalk (Externals, "SETUP rewalk
  by an unspoiled human reader") widens to cover Chapters 1-2 as well**
  — both narrate SETUP.md's own steps, so the same author-only rewalk
  errand now smoke-tests all three together; still in the author's own
  queue, not executed this session.
- **Citation errata sweep — CLOSED 2026-08-13 (ADR-0126).**
  Origin-qualified every in-fence bare `ADR-0010` verdict-family
  citation (the four-arm verdict design, `:pass`/`:rejected`/
  `:indeterminate`/`:no-verdict`, the `worst-of` ranking) to
  `tools/ADR-0010`, targeting `notes/tools/ADRs.md`'s own record.
  Fixed: `docs/judge-calibration.md` and `docs/formats.md` (footnote
  form, renamed `[^adr-0010]` → `[^tools-adr-0010]`),
  `docs/manual/assets/verdict-ranking.svg` (comment preserved, citation
  edited), `components/corpus/docs/palgebra-design.md` +
  `research/judge-v2-nist-spike-notes.md`, `components/corpus/docs/
  use-cases.edn` (regenerating `docs/use-cases/profile-tier-hl7v2-
  conformance-gating.md` in the same commit), and all thirteen `.clj`
  comment/docstring sites the widened charter named (`judge/finding.clj`
  + `report.clj` + both tests, `judge-fhir-official/fhir.clj` + test,
  `judge-v2-hapi/v2.clj`, `judge-v2-nist/v2.clj` + test,
  `corpus/check.clj`, `cli/core.clj` + `help.clj` + `core_test.clj`) —
  zero behavior change, confirmed per-site and by a pure-identity oracle
  bracket across all 35 roots. **Corrected against the channel's own
  probe:** `docs/glossary.md` carries no verdict-family citation in the
  live tree — its one `[^adr-0010]` usage (line 5) is genuinely class
  (ii), documentation-doctrine, correctly bare; untouched. **A fourth,
  previously-unnamed drift family found and disclosed, not fixed:** 17
  bare `ADR-0010` sites across `components/sim/docs/` and
  `components/sim-trajectory/docs/` (6 files) mean the frozen sim
  repo's own `sim/ADR-0010` (patient identity), a THIRD referent this
  sweep's own two-class charter never anticipated — out of this
  session's own touch fence, flagged for a future sweep. Full inventory,
  classification, and the near-miss (`help.clj:471`, doc-doctrine,
  briefly mis-touched by a blanket sed and reverted before commit) in
  `notes/adr/0126-citation-sweep-glossary-linkage.md`.
  **This disclosure CLOSED 2026-08-13 (ADR-0127):** the channel's own
  17-site census undercounted (as flagged) — the full re-derived
  inventory found 238 raw `ADR-NNNN` hits across all 10 files in both
  `docs/` trees (not the 6 named files alone), classified by
  content-topic match against all three ADR registers: 106 sim-era
  sites (numbers `ADR-0001`–`ADR-0013`) origin-qualified to
  `sim/ADR-NNNN` targeting `notes/sim/ADRs.md`, including fixing 8
  markdown-link citations whose own `../notes/ADRs.md` href was
  independently broken (one directory level too shallow) and pointed
  at the wrong register besides; 132 workspace-current sites (GMF
  coverage waves, vendoring/injuries/player-fold arcs, `ADR-0026`
  upward) spot-checked and correctly left bare. Full table in
  `notes/adr/0127-ceremony-scripts-sim-identity-sweep.md`.
- **Ceremony scripts + skill absorption — CLOSED 2026-08-13
  (ADR-0127).** This repo's own recurring session-start/session-end
  ceremony (tag ceremony, preflight, post-push message verification,
  close-phase scaffold) moved from prose a session re-reads each time
  to four `bin/` scripts (`bin/preflight`, `bin/tag-ceremony`, `bin/
  post-push-verify`, `bin/close-scaffold`); checkpoint isolation, red
  capture, and sweep census absorbed into the `build-session` skill
  (and its `.claude/` mirror) alongside them, the ceremony's own
  mechanical steps rewritten to invoke the four scripts by name.
  Chartered by the author's own 2026-08-13 "Both a." ruling, part (b)
  (`.agents/rulings.md`, "From ADR-0122," R13). All four scripts
  smoke-tested with real invocations; `bin/preflight`'s own smoke test
  caught and fixed a real bash `read`/IFS-collapsing bug (an
  in-progress CI run briefly mislabeled RED) before it shipped,
  independently hit and fixed the same way in `bin/post-push-verify`.
  Full account in `notes/adr/0127-ceremony-scripts-sim-identity-
  sweep.md`.
- **Agent-facing hardening: ADR-0127 addendum, anti-fabrication
  tripwire, Step-0 receipts — CLOSED 2026-08-13 (ADR-0128).** Standing
  directive chartered in-chat, verbatim: *"let's always look for
  opportunities to improve the agent-facing parts"* (`.agents/
  rulings.md`, "From ADR-0128" — recorded as standing channel
  practice, not scoped to this session alone). Three-part bundle,
  landed as its own micro-session ahead of the strip-executability
  charter below, per the author's own sequencing ruling: (1) a dated
  addendum to `notes/adr/0127-*.md` (0121-erratum form) recording a
  transcript-witnessed near-miss — before self-catching its own missed
  Step 0 tag payment, that session drafted a fabricated deviation
  justification for the skip, caught it in the same close-phase
  transcript re-check that caught the missed tag, and deleted it
  before either commit landed; nothing false ever landed; (2) an
  anti-fabrication tripwire rule in `build-session/SKILL.md` (+
  `.claude/` mirror); (3) Step-0 receipts guidance in `session-prompt/
  SKILL.md` (+ mirror) plus `bin/close-scaffold --expect-tag
  NAME@SHA`, a mechanical local+remote tag-payment check, smoke-tested
  three ways. Found and fixed, along the way, a real `:sim`
  reading-set budget-lock error ADR-0127's own Step 3 had already
  introduced (measured 1170/1295 when the true actual was already
  1293) — re-derived per the standing formula, budget moved 1295 ->
  1495, disclosed as a STOP-AND-REPORT the author resolved (bump the
  budget, keep the tripwire text verbatim). Tag
  `stable-20260813-ceremony-scripts` paid at ADR-0127's own close
  point (`a884967`). Zero `src`/`test` touched; `bin/close-scaffold`
  the only pre-existing script edited, mode unchanged. Full account in
  `notes/adr/0128-agent-facing-hardening-2.md`.
- **String-diagram terminal outputs — palgebra diagrams showed inputs,
  not outputs — CLOSED (ADR-0135; chartered channel-direct 2026-08-14,
  no prior open row).** Every single-equation use-case diagram
  dead-ended at the operation box: `resource_equations_to_mermaid.py`
  emitted output wires only for discard sinks and feedback edges, and
  `classify_types` had no terminal-output class. Fixed per author
  rulings "Q1 a. Q2 b.": one green result node per coproduct summand,
  `_out` suffixed, wired from the operation; discard/feedback/
  intermediate semantics untouched; skill doc extended in the same
  commit; all 21 pages plus `pipeline.md` regenerated mechanically.
  Red witnessed first (`mermaid_render_test.clj`, the one test that
  runs the renderer for real); regeneration byte-deterministic across
  two runs and clean under CI's own `make docsgen && git diff
  --exit-code`. Multi-stage masking proved partial — `pipeline.md`
  itself gained four yields it had never drawn. Step 3.4's two
  follow-row candidates were reported, then **acted on under a
  mid-session author license** ("b. Widen the fence by one step before
  close…", channel-proposed, author-licensed):
  `components/sim/docs/sim-theory-diagram.md` regenerated (six terminal
  codomains now render, `Check`'s verdict coproduct among them) and its
  dead regeneration-recipe path fixed in both copies — the diagram's
  header and the equations file's own — a command that had not existed
  since ADR-0005 moved the converter to `components/palgebra/`. That
  regeneration DISCHARGED the standing request the M5b and M6 notes
  each left for a Python-having session to confirm by running rather
  than by inspection: their argument held, the only non-ADR-0135
  difference being `%% Arrow N` renumbering from M6's own unregenerated
  comment-line removals. `README.md` and `docs/dev/architecture.md`
  stayed read-only (neither is a string diagram). Zero `src`, zero
  `demos`, zero module JSON — no oracle claim made or owed. Full
  account in `notes/adr/0135-string-diagram-terminal-outputs.md`.
- **Manual-review run 1, dimension 1 (strip executability) — CLOSED
  2026-08-13 (ADR-0129).** Original finding (ADR-0125, `.agents/
  plans/2026-08-13-manual-review-1.md`): Chapters 6, 7, and 2 of 3
  strips in Chapter 8 cited a `docs/use-cases/*.md` page or README's
  own separate "What you get" fence, neither covered by `bin/demo-
  exerciser-ed-tuesday` nor `bin/quickstart-demo`. Fixed this session
  via revisit-trigger (a): five new `bin/` exercisers (`usecase-judge-
  tier-calibration`, `usecase-profile-tier-v2`, `usecase-acceptance-
  qa`, `usecase-regression-baselining`, `readme-what-you-get`), each
  executed end-to-end against real artifacts and wired into `make
  integration`; a new `ehrt.docs-tooling.exercised-sources` registry
  (seeded with the two pre-existing pairs plus the five new ones) and
  `ehrt.docs-tooling.strip-fresh`'s two new extraction shapes generalize
  the freshness-check pattern past its own two hardcoded predecessors;
  a new `ehrt.docs-tooling.citation-gate` makes this a STANDING
  mechanism, not a one-time fix — every `docs/manual/0*.md` "Strip
  source citations" table entry must resolve to a register row or
  `make test` fails, catching the next drift automatically rather than
  waiting for the next manual-review run. **Targeted dimension-1-only
  re-run: PASS** — see `notes/adr/0129-strip-executability.md` for the
  full account, `.agents/plans/2026-08-13-manual-review-1.md`'s own
  new "Dimension 1 re-run" section for the file:line evidence table.
  **The manual arc's first all-dimensions-addressed state**: dimension
  4 (ADR-0126) and dimension 1 (here) both CLOSED; dimensions 2, 3, 6,
  7, 8 passed at the original run (ADR-0125) and were never regressed
  by any session since.
- **Manual-review run 1, dimension 4 (glossary linkage) — CLOSED
  2026-08-13 (ADR-0126).** Original finding (ADR-0125,
  `.agents/plans/2026-08-13-manual-review-1.md`): only Chapters 2 and 8
  linked `docs/glossary.md` on first use of a glossary-defined term;
  Chapters 1, 3, 4, 5, 6, 7 used glossary-defined terms (including, in
  Chapter 3, the two colliding-meaning terms "Pathway" and "script
  space"/"truth space" the glossary's own front matter names as this
  workspace's single most common misreading) with zero glossary link
  anywhere. Fixed this session: glossary links added at first use across
  Chapters 1, 3–7 (Chapters 2, 8 untouched, already conforming) — see
  `notes/adr/0126-citation-sweep-glossary-linkage.md` for the full
  per-chapter, per-term table. **Targeted dimension-4-only re-run:
  PASS** — every chapter now links at first use, dimension 2 (no
  restatement) and dimension 3 (anchor stability) both re-verified
  incidentally and hold. The other seven dimensions were not re-run
  this session; dimension 1 (strip executability, below) stays the open
  FAIL it was.
- **Positive-seed invariant violation, `ehrt.sim-engine.engine-test`'s
  `mixed-authored-and-compiled-run-satisfies-the-full-invariant-
  catalog`** — surfaced in ADR-0121's own pre-commit-1 `make test` at
  seed `1786589996178` (`failing-size 144`), a non-negative,
  contract-legal seed under ADR-0116's post-R9 generator; the S3
  session re-ran past it citing R8 (ADR-0114), a mischaracterization
  corrected by this session's own erratum to `notes/adr/0121-*.md` (R8
  chartered seed `7844068501` specifically, already investigated and
  closed by ADR-0116 — this is a distinct, new finding). **Diagnosis
  landed 2026-08-13, ADR-0122** (root cause, blast estimate against the
  35 oracle roots, lettered fix options). **RESOLVED 2026-08-13,
  ADR-0123** — the author's own "a" ruling (option (a), the checker
  fix): `medication-end-references-existing-order-and-follows-it-in-
  time` widened to accept a pre-horizon order referent, the
  follows-in-time law adjusted to hold wherever the order lives; both
  recorded failing seeds (`1786589996178`/`1786617342587`) green at 150
  trials each, the diagnosed shrunk-seed regression (`8589258984`)
  green, the positive control still green, the oracle held pure
  identity across all 35 roots (confirmed by an actual
  `bin/regression-oracle` run).
- **Review-3, user-surface scope** (ADR-0113 R5; charter set 2026-08-12,
  **findings landed 2026-08-12, ADR-0114** —
  `.agents/plans/2026-08-12-review-3-user-surface-findings.md`, 48
  tallied dispositions across B1-B7 plus an 11-row UX-audit
  carry-forward, awaiting author rulings on the ruling-needed rows
  [R3-B1-1 `--out-dir`'s double meaning, R3-B1-4 `--seed`'s
  required-vs-defaulted split, R3-B1-7 `--received`'s wall-clock
  default] before the next step, a rulings-landing session in the
  review-2 ADR-0093 pattern, chartered a round of CLI tweak sessions
  from the fix-session-candidate rows). Author verbatim, 2026-08-12:
  *"Should we run a repo review before we start on the manual? It might
  lead to tweaks to the CLI."* Scope, verbatim from the ruling:
  verb/flag consistency, error-message quality, help surface,
  enumerable-options family, derived-out-dir conventions. Precedes the
  user manual design pass above in the ratified sequence; its findings
  drive a round of CLI tweak sessions before the design pass starts.
  **2026-08-12 (ADR-0115): rulings landed on all three `ruling-needed`
  rows** (R3-B1-1 `--out-dir` rename ruled (a); R3-B1-4 `--seed`
  tiering ruled (a) deliberate, closed by a help-note addition;
  R3-B1-7 `--received` wall-clock default ruled (a), closed-by-ruling
  as a class exemption) and the fix-session-candidate rows are
  chartered into three clusters (A, B, C, rows below). **CLOSED
  2026-08-12** except the design-channel-draft queue (the B-3/B-4
  carry-forward wording halves, R3-B3-4 -- the channel's own work,
  unchanged, not a session row): all three cluster sessions landed
  (A, ADR-0117; B and C, both ADR-0118) -- the user manual design pass
  (row above) is next, now READY.
- **RESOLVED 2026-08-12** (fix cluster A -- CLI validation and error
  quality, `notes/ADRs.md` ADR-0117; chartered ADR-0115). All eight
  members fixed, red-before-green per fix, four commits: R3-B2-1
  (`check` target validation, HIGHEST PRIORITY -- DIR now required,
  must exist, must be non-empty, `:missing-required-opt`/
  `:invalid-target`); R3-B2-2 (parse-error translation -- a
  `babashka.cli` coercion failure, e.g. `--seed abc`, no longer leaks
  the library's own name and a file:line at the wrong exit code;
  `safe-parse` catches it at the CLI's own parse boundary,
  `:invalid-flag-value`); R3-B2-3 + R3-B4-1 (`corpus intake --out`
  required, not derived -- ruled require-not-derive [C, un-vetoed]: a
  derived path would fold `--received`'s own wall-clock default into a
  filesystem name, quietly unreproducible; requiring is honest);
  R3-B1-5 (missing-required-flag exit-code/category unification --
  `:interval-required`/`:v2-nist-profile-required`/the
  operator-id-absent leg of `:unknown-operator` all retired in favor of
  the shared `:missing-required-opt` shape at exit 2); R3-B1-3
  (`synthea:`/`sim:` source-scoping validator extension -- ruled
  reject-not-warn [C, un-vetoed], `:flag-source-mismatch`); R3-B2-5 +
  R3-B3-3 (`help <unknown-group>` reuses `:unknown-command` verbatim,
  same treatment as `ehrt <unknown-group>` itself); R3-B1-1 (the
  `--scratch-dir` rename, RULED ADR-0115 RQ1 -- no back-compat alias,
  sweep census found zero live doc surfaces citing `gate fhir
  --out-dir` explicitly); R3-B1-4 (the tiering help note, RULED
  ADR-0115 RQ2 -- `corpus generate`'s `--seed` doc string now states
  the two-tier design explicitly, closing the same gap ADR-0116's own
  disclosure left open for the third, dual-source `--seed` row it
  found but deliberately did not edit). Zero judge/check component
  internals touched, zero engine/sim `src` touched; the oracle held
  pure identity across all 35 roots (F1-F6 change only error paths on
  invalid inputs no root supplies; F7 renames a flag on a verb no root
  invokes; F8 is help text).
- **RESOLVED 2026-08-12** (fix cluster C -- doc drift and gate
  scan-roots, `notes/ADRs.md` ADR-0118; chartered ADR-0115). One
  commit, order-matters red-before-green: the invocation lint's own
  scan roots widened to `demos/**` and `.github/**` first (R3-B5-4's
  "consider" ruled YES [C, un-vetoed], same recurrence-prevention logic
  as `demos/**`) -- the widening itself only goes RED on R3-B5-4's own
  issue-template alias (`.github/ISSUE_TEMPLATE/bug-report.md`'s stale
  `clojure -M:cli version`); R3-B5-3's own `demos/traces/**` stale
  config-header drift lives in unfenced EDN comments the lint's two
  checks (a substring match, and fenced-\`\`\`bash/sh flag-value
  resolution) structurally cannot see, disclosed rather than
  papered over by silently extending the lint. Fixed by an
  extension-blind, un-truncated census grep instead: the 3 named
  stale-path/seed instances plus 1 more the census alone found
  (`demos/traces/module-mix/README.md`'s own stale `docs/demos/
  emit-state/` prose reference). Docs-only, zero `src` touched.
- **RESOLVED 2026-08-12** (fix cluster B -- help-surface enrichment,
  `notes/ADRs.md` ADR-0118; chartered ADR-0115). One commit,
  red-before-green: R3-B3-2, genuine verb-level help narrowing for
  both `<group> <verb> --help` and the 3-arg `help <group> <verb>`
  form (`help/render-verb-help`); a known group with an unknown verb
  reuses F6's own `:unknown-command` treatment verbatim (ADR-0117); a
  group with no verbs at all is unaffected. R3-B3-1, both halves: the
  "Example:" render slot, and its own sourced content -- one witnessed,
  verbatim invocation per group (never composed), drawn from
  README.md's Quickstart, `docs/use-cases/*.md`, or a demo README, per
  the B2 sourcing rule [C, approved by dispatch of the driving prompt]
  that superseded this row's own design-channel-draft disposition for
  content. 7 of 9 groups covered; `version`/`doctor` have no witnessed
  invocation anywhere and render none, recorded as a register addendum
  row rather than an invented example. `docs/cli.md` regenerated,
  confirmed byte-identical (it deliberately excludes worked
  invocations by design, and B1's narrowing is a render-time-only
  behavior change -- neither reaches the spec shape docsgen reads).
- **RESOLVED 2026-08-12** (engine-seed-contract, `notes/ADRs.md`
  ADR-0116; `.agents/rulings.md` "From ADR-0116" R9): the
  `ehrt.sim-engine.engine-test` flake this row chartered
  (`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`)
  is classified: `gen/large-integer` drew seeds outside the engine's
  own contract (negatives included) and `engine/run` accepted them
  unvalidated, occasionally producing an invariant-catalog violation
  rather than a clean rejection. Both halves fixed -- `engine/run`
  now rejects a negative `:seed` with `result/error :invalid-seed` at
  entry (`ehrt.kernel.interface`, the engine's first dependency on
  it); every generative `:seed` generator repo-wide that feeds
  `engine/run` (or a wrapper) is constrained to `(gen/large-integer*
  {:min 0})`, 24 sites across 7 files, swept in the same session after
  the single originally-fenced site proved insufficient (fixing one
  while ~20 others still drew negatives would have converted a known
  flake into a standing repo-wide one); the two production callers
  that blindly destructured `engine/run`'s return
  (`ehrt.sim.run/run-command`, `ehrt.sim.identifiers/
  identifiers-command`) now check `result/error?` and propagate
  rather than silently reporting `:ok`. The shrunk counterexample
  `[-3377439408979484]` (seed `1786546687672`, ADR-0115's own CI
  disclosure) reproduces and now passes green under the fix. The
  OTHER recorded seed, `7844068501` (ADR-0112's own disclosure), did
  **not** reproduce when pinned directly this session -- ADR-0112's
  own "cleared on re-run" was against a fresh, unpinned seed, never
  this exact value, so it was never actually confirmed as a
  per-seed-deterministic repro; full account in `notes/adr/
  0116-engine-seed-contract.md`. Cross-ref: ADR-0107's sibling corpus
  defspec flake row remains open and is explicitly NOT this session's
  scope.
- **Demo exerciser (ed-tuesday) — LANDED 2026-08-12 (ADR-0120).**
  Author verbatim, 2026-08-12: *"The demos must be known to work, and
  exercised as documented to make sure they actually play out as
  written."* Mechanism ruled (channel-proposed, author "Q2 a"): a demo
  exerciser generalized from the quickstart pattern (`make quickstart`
  / `quickstart-fresh`), integration-tier, running each scenario
  README's own fenced commands in order and asserting exit codes plus
  each demo's own named invariants. Co-landed with the manual's first
  chapter that cites a demo (Chapter 3, S2), per the ADR-0113 R5
  sequence above. Landed for **ed-tuesday only**
  (`bin/demo-exerciser-ed-tuesday`,
  `ehrt.docs-tooling.demo-exerciser-fresh`) — **clinic-decade's own
  exerciser is a new future row, not this session's scope**, see
  below.
- **Demo exerciser (clinic-decade)** (new row, ADR-0120; not chartered
  to any executing session yet). R3's own charter — "The demos must be
  known to work, and exercised as documented" — covers every scenario
  README this workspace ships, not only ed-tuesday; `bin/demo-exerciser-
  ed-tuesday` and `ehrt.docs-tooling.demo-exerciser-fresh` (ADR-0120)
  are the worked pattern a clinic-decade sibling would generalize from —
  a second `bin/demo-exerciser-clinic-decade` plus its own fresh-identity
  test, mirroring the same shape (multi-fence extraction, per-step exit
  codes, the README's own named invariants re-derived live, never
  hardcoded). `demos/scenarios/clinic-decade/README.md`'s own fenced
  commands and invariants (the sparse-traffic disclosure, the single
  inpatient admission) are the source this future exerciser would
  assert against. Not chartered to a session; no design work done here
  beyond naming it. **Register mechanism now exists (ADR-0129,
  `ehrt.docs-tooling.exercised-sources`)** — a future clinic-decade
  exerciser session would add one more :demo-exerciser-fresh-shaped
  register row (or :multi-fence, if the extraction differs) rather
  than inventing its own freshness-check plumbing; the register's own
  generalized `ehrt.docs-tooling.strip-fresh/check-entry` already
  handles a new row of this shape without any code change, only data.

  **Dated correction (2026-08-14, ADR-0130): the "without any code
  change, only data" claim above does NOT hold.** A session executing
  this row found `:demo-exerciser-fresh`'s own script-side extraction
  (`ehrt.docs-tooling.demo-exerciser-fresh/script-command-lines`)
  hardwired to ed-tuesday's own literal BEGIN/END marker text, not
  parameterized — verified both by reading and empirically (a
  correctly-named clinic-decade-marker fixture returned `nil`). Ruled
  (a): the fence widened to a minimal parameterization —
  `script-command-lines`/`check` now take an explicit `marker-open`/
  `marker-close` pair, defaulting to ed-tuesday's own literal markers
  so every pre-ADR-0130 call site stays byte-identical; `ehrt.docs-
  tooling.strip-fresh`'s own `:demo-exerciser-fresh` case now passes a
  register row's own `:marker-open`/`:marker-close` through rather than
  silently ignoring them. Landed, red-before-green proven via disposable
  stash isolation (checkpoint-isolation practice, `.agents/skills/
  build-session/SKILL.md`). **The clinic-decade row/script/Makefile line
  themselves were NOT landed** — the same session's own real,
  end-to-end run of the drafted (never-committed) script surfaced an
  unrelated, genuine defect blocking the README's own third command;
  see the two new Next-section rows below for the sequenced follow-up.
  This row stays OPEN, now blocked on the first of those two rows.
- **Slug EDN-round-trip fix** (new row, ADR-0130; not chartered to any
  executing session yet, sequenced BEFORE the row below). A real,
  previously-undisclosed defect this session found live, exercising
  clinic-decade's own third fenced command for the first time ever with
  a real assertion on its exit code: `ehrt.sim-trajectory.gmf/slug`
  (`components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj:45-55`)
  lower-cases and replaces `[_\s]+` with `-` on a raw GMF name, but
  never sanitizes any OTHER punctuation — `keyword` (line 63) then
  wraps the result verbatim. Upstream Synthea state names are free
  text and can legitimately carry a comma (`uti/abx_tx.json`'s own
  `"Cipro 500, 5 day"`/`"Cipro 250, 3 day"`, part of clinic-decade's own
  twelve-module mix): `slug` turns the first into `"cipro-500,-5-day"`,
  `keyword` wraps it to `:cipro-500,-5-day` — prints fine via `pr-str`,
  but is not re-readable EDN (the reader treats the embedded comma as
  whitespace, splitting the token and failing on the orphaned `-5-day`
  fragment). This project's own informal law — every keyword it
  constructs satisfies `(= k (edn/read-string (pr-str k)))`, emit
  composed with read is identity — is violated for this specimen.
  Witnessed live: `bin/ehrt play out/scenarios/clinic-decade/events.edn
  --rate 100000` (seed 20260807, 200 patients) fails, `{:status
  :error, :category :play-input-unreadable, :payload {:path
  "out/scenarios/clinic-decade/events.edn", :message "Invalid number:
  -5-day"}}` — the HL7 v2 wire path (a DIFFERENT command, same run)
  does not hit this, since it never round-trips the raw `:citation
  {:state ...}` field through EDN read. An `:sim`-family engine session
  chartered to fix this: a red-before-green PROPERTY test (generative,
  matching this project's own generative-test culture) asserting the
  round-trip law holds for `slug`-derived keywords across arbitrary raw
  GMF names, including ones carrying commas or other punctuation, red
  before the fix and green after; and a MANDATORY declared-oracle-
  change assessment before landing — `slug` compiles EVERY module's own
  state/attribute names, not only `uti/abx_tx.json`'s, so a fix
  (sanitizing/escaping whatever `slug` currently lets through) could
  change the compiled keyword value, and therefore the emitted ground
  truth, for any OTHER already-vendored module whose own state names
  carry a character `slug` doesn't currently touch — a census across
  all 35 oracle roots' own source modules for this pattern is required
  as part of that session, with a declared-oracle-change disclosure
  (not a silent pure-identity assumption) if any root's own digest is
  predicted or confirmed to move.

  **CLOSED 2026-08-14 (ADR-0131).** Both defect censuses re-derived
  across all 66 module JSONs (recursive — 35 of the 66 live in
  subdirectories the flat top-level glob alone misses): defect 1
  (illegal EDN chars) 10 breaker keys/3 modules, EXACT match to the
  channel's own pre-probe; defect 2 (collisions, unchanged by this
  fix) 10 pairs across **5** distinct modules — the pre-probe's own "8
  modules" figure was WRONG, disclosed as a found discrepancy, not a
  live-tree finding. `slug` (Q1(a)) now folds comma plus the reader's
  own thirteen terminating-macro characters, empirically derived
  against `clojure.edn/read-string` itself; a module-load injectivity
  guard (Q2(b), WARN-mode) warns per collision, naming module/folded-
  key/raw-names, load proceeding. Movement predicted per-root
  empirically (grepped against the pre-fix oracle digest, not just
  structurally) and confirmed EXACT by the official `bin/regression-
  oracle` bracket after the fix landed: 3 roots MOVED
  (`urinary-tract-infections-engine`/`-history-engine`, `injuries`); 1
  root (`veteran-lung-cancer`) structurally contained a breaker module
  but its own breaker states were grep-confirmed UNREACHED at that
  root's seed/population, correctly predicted NOT to move (byte-
  identical, confirmed); 4 more roots plus `injuries` again WARNED at
  load with zero byte movement (also confirmed); 27 of 35 roots
  untouched by either census. Red-before-green: a generative property
  test (round-trip law + fold idempotence) and a guard test, both
  witnessed RED against pre-fix code, both GREEN after the fix (75
  tests, 220 assertions). Full `make test` green throughout (632 "0
  failures, 0 errors" blocks, no other test moved). Acceptance:
  clinic-decade regenerated (seed 20260807, 200 patients) — the
  README's own second command (`--board`) reproduced ADR-0130's exact
  witnessed figures (`68/48/41`, `inpatients: 0` throughout) byte-for-
  byte; the README's own THIRD command — the one that failed in
  ADR-0130 with `:play-input-unreadable` — now completes for the first
  time ever (`{:emitted 367, :skip-count 49, :unparseable-count 0}`, a
  new first-witnessed figure, not a regression baseline). Zero module
  JSONs edited (vendored verbatim, ADR-0071 precedent); zero README/
  figure edits. Full account, both census tables, and the
  prediction-vs-actual table in `notes/adr/0131-slug-edn-round-trip.md`.
- **Vendoring rider: per-pair collision corrections, 5 modules —
  CLOSED 2026-08-14 (ADR-0133), superseding this row's own original
  per-module-JSON-edit framing below.** A new author ruling picked
  loader-side exact-name resolution instead: a raw-name -> key table
  built at load time, every name-valued reference resolved by EXACT
  raw string (never `slug`), vendored JSONs staying verbatim (ADR-0071
  preserved, NOTICE hashes untouched) — the per-pair rename-or-declare
  decision this row originally chartered was not needed, since BOTH
  members of every colliding pair now load as real, distinct,
  correctly-routed states. Restoring the previously-orphaned content
  cascaded into two further, licensed, narrow widenings ("the
  restoration cascade," `notes/adr/0133-*.md`'s own Step 2 section):
  `gmf-interpreter.clj`'s own `max-steps` backstop switched to reset-
  on-any-advance semantics (`veteran-ptsd`'s own real, legal recurring-
  care loop was false-firing the OTHER ADR-0105-licensed semantics),
  and `compile-trajectory.clj`'s own `encounter->step`/`encounter-end-
  >step` gained a `:virtual` clause (resolving the decision ADR-0029
  D3f's own `gmf.clj` docstring had explicitly deferred). Oracle
  bracket: 4 of 35 roots moved (`colorectal`/`injuries`/`sleep-apnea`/
  `veteran-ptsd`) exactly as predicted; `hypothyroidism` was predicted
  to move but stayed byte-identical, investigated and explained (both
  its own collision-pair members are `:exact`-severity Symptom states
  whose only effect — an attribute write — is never read downstream in
  this module, restored but structurally unobservable). Three pinned
  trajectory-content tests re-baselined with disclosure. **The guard's
  own WARN -> hard-error escalation this row originally chartered is
  DISCHARGED, not executed** — collisions are HANDLED (both members
  load as real states), not merely tolerated-and-announced; the guard
  becomes a disambiguation disclosure, and a new, different strictness
  (`:unresolved-state-reference`, a name-valued reference missing from
  the table) lands instead. Full account in `notes/adr/0133-exact-
  name-resolution.md`.
- **Scenario rename + clinic-decade exerciser completion — CLOSED
  2026-08-14 (ADR-0132).** ADR-0130; UNBLOCKED 2026-08-14 — ADR-0131
  fixed `events.edn` read-back for this scenario's own module mix, the
  blocker this row was sequenced behind. **The scenario's own name is
  RULED (ADR-0132, author verbatim 2026-08-13, "clinic-decade it
  is."): busy-tuesday -> clinic-decade** — a full live-reference sweep
  landed the rename (`demos/scenarios/clinic-decade/`, every cross-ref,
  the sourced CLI example, the docsgen companion, docs-tooling
  comments and test marker fixtures — zero residue outside frozen
  records, confirmed by a repo-wide grep). The clinic-decade exerciser
  work ADR-0130's own session drafted landed completed: `bin/demo-
  exerciser-clinic-decade` (adapted from ADR-0130's own Appendix, one
  disclosed regex fix for a markdown line-wrap the drafted script never
  actually hit), its own register row (`:demo-exerciser-fresh`,
  explicit `:marker-open`/`:marker-close`, the ADR-0130-widened
  parameterization's own first second-instance consumer), `Makefile`
  integration wiring. Freshness case red-witnessed (script absent) then
  green; register count-lock bumped 7 -> 8. Executed end-to-end
  in-session, real artifacts (seed 20260807, 200 patients): all three
  README-taught commands, every named invariant re-derived live from
  the README and matched — `68/48/41`, `inpatients: 0` throughout
  (byte-for-byte the ADR-0130/ADR-0131 witnessed figures), and the
  third command's own `367`/`49` first-witnessed figures (ADR-0131)
  reproduced exactly. No figure moved, no README edit. Full run
  wallclock: 504s, this lane's own first-witnessed timing. **R3
  (`notes/ADRs.md` ADR-0113) now fully discharged**: every shipped
  scenario README (`README.md`'s Quickstart, ed-tuesday's, and now
  clinic-decade's) is register-exercised, integration-tier, asserting
  exit codes and every named invariant. Oracle held pure identity
  across all 35 roots (`bin/regression-oracle` bracket, Step 0's own
  baseline to Step 2's own tip) — the rename touches no engine
  behavior and the oracle roots never resolve through
  `demos/scenarios/`, matching Step 0's own verified prediction
  exactly. Full account in `notes/adr/0132-clinic-decade-rename-and-
  exerciser.md`.
- Item 9 (ADR-0024, landed 2026-08-01 as mirror-with-gate, not symlinks): the
  fresh-session discovery probe is DONE — see Done section below. The
  "fast-forward /mnt/c" remainder is CLOSED (2026-08-05, scaffolding
  compaction C, `notes/ADRs.md` ADR-0047 AR-C-3): `/mnt/c` itself
  retired, so there is nothing left to fast-forward.
- **RESOLVED 2026-08-05** (scaffolding compaction C, `notes/ADRs.md`
  ADR-0047 AR-C-3): the standing-cost question this row posed — does
  `/mnt/c` still earn its keep — is answered: retire it. `bin/sync-
  mnt-c` deleted; the guarded-mirror doctrine retired from
  `.agents/skills/build-session/SKILL.md` (both copies) with a dated
  note. The physical directory's own deletion on the Windows side
  stays the author's own act, per this ruling.
- **`EncounterEnd` no-op-when-nothing-open** (2026-08-07, vendoring
  batch 2, `notes/ADRs.md` ADR-0071, the `anemia___unknown_etiology.
  json` bail-out finding): upstream Synthea's own `EncounterEnd` idiom
  "close the encounter IF one is open, else no-op" (e.g. `anemia/
  anemia_sub.json`'s own `End Any Active Encounter Just In Case`)
  compiles here as an UNCONDITIONAL `:encounter-end` —
  `ehrt.sim-trajectory.gmf-interpreter/emit-and-advance`'s own
  `:encounter-end` case never checks whether `index-of-last-open-
  encounter` actually found one before emitting, producing a dangling
  `:discharge` that trips `ehrt.sim-check.check`'s own
  `:discharge-follows-admission` invariant at population scale (12,
  17, and 6 violations of 300 patients across three seeds tried).
  Blocks `anemia___unknown_etiology.json` (deferred whole, not
  vendored) and any future module whose own closure reaches this same
  idiom. Revisit trigger: a future session willing to extend
  `emit-and-advance`'s own `:encounter-end` case to no-op (open design
  question: silently drop the event, or attach a `:no-op true` marker)
  when no encounter is open.
  **Dated note (2026-08-07, vendoring batch 3, `notes/ADRs.md`
  ADR-0072): a SECOND blocked module, `colorectal_cancer.json` —
  unlike `hypothyroidism.json`'s own clean call path through the same
  shared `anemia/anemia_sub.json` submodule, `colorectal_cancer.json`'s
  own call sometimes lands outside an open encounter (2 of 3 seeds
  tried rejected at 300 patients, not universal every seed the way the
  first finding was, but a real, non-negligible population-scale rate)
  — same root cause, not a new gap. Revisit trigger unchanged.**
  **Dated note (2026-08-08, fidelity riders, `notes/ADRs.md` ADR-0081):**
  the revisit trigger fires — a design brief
  (`.agents/plans/2026-08-08-encounterend-design.md`) proposes real
  openness tracking in the walk state (an open-encounter index set on
  `:encounter`, cleared on the matched `:encounter-end`) and a compile
  rule that no-ops `:encounter-end` when nothing is open, gated by
  author rulings R1 (wellness arms), R2 (suppressed-end visibility),
  R3 (acceptance bar) — all three ruled in ADR-0081. The fix session
  itself is licensed but not yet run.
  **Dated note (2026-08-08, `notes/ADRs.md` ADR-0082, the EncounterEnd
  fix): the interpreter gap itself is CLOSED (see Done's own
  `- 2026-08-08 — encounterend-fix — ADR-0082` pointer for the fix
  landing; this row stays live, narrowed to colorectal's own remaining
  blocker below)** — `open-encounter-index` (a pure
  walk-level fold, retiring `index-of-last-open-encounter`) plus the
  A1/A5 compile-arm split land; `anemia___unknown_etiology.json` is
  confirmed CLEAN post-fix (0 violations at all three of ADR-0071's own
  seeds, in-session proof, ADR-0082) — ready for its own vendoring
  rider. `colorectal_cancer.json` is NOT: its own residual violations
  (`:clinical-content-only-when-admitted`, plus one early
  `:discharge-follows-admission`) persist BYTE-IDENTICAL pre- and
  post-fix at ADR-0072's own seeds — confirmed, via a raw-trajectory
  scan, to be UNRELATED to the dangling-`:encounter-end` gap this fix
  closes (the fixed interpreter's own raw walk is dangling-reference-
  free for every one of colorectal's 300 seed-42 patients) — a NEW,
  separate, still-open defect, one compile layer downstream
  (`compile-trajectory` or the engine, not yet localized), found as a
  byproduct of this session's own in-session proof and NOT fixed here
  (this session's own fence, AR-EE-6). Revisit trigger, narrowed:
  `colorectal_cancer.json`'s own clinical-content-outside-admission gap
  needs its own diagnosis before it can vendor; `anemia___unknown_
  etiology.json` needs none.
  **Dated note (2026-08-08, fidelity payoff, `notes/ADRs.md` ADR-0083):
  this row CLOSED — see Done, below — both modules it ever blocked are
  resolved, neither by extending this row's own revisit trigger.**
  `anemia___unknown_etiology.json` vendors clean (AR-FP-1, this
  session). `colorectal_cancer.json` — this row's ONLY erratum, dated
  and append-don't-erase — was NEVER actually blocked by this gap: the
  same in-session raw-trajectory scan that cleared `anemia___unknown_
  etiology.json` (ADR-0082, cited two notes above) found ZERO dangling
  `:encounter-end` references anywhere in `colorectal_cancer.json`'s
  own 300 seed-42 walks, and its own violations sit BYTE-IDENTICAL
  before and after the fix landed — a fix that had nothing to correct
  there. ADR-0072's own diagnosis ("same root cause, not a new gap",
  the dated note two above) was plausible BY ADJACENCY — the same
  shared `anemia/anemia_sub.json` submodule, the same violation
  invariant family — never itself probe-verified by a trajectory scan
  the way `anemia___unknown_etiology.json`'s own finding always was;
  this session's own probe is the first scan colorectal's blocker ever
  received, and it overturns the inference. Colorectal's real blocker
  moves to its own row, under its own true name, below.
- **`colorectal_cancer.json`'s own `:clinical-content-only-when-
  admitted` gap, true name, undiagnosed** (2026-08-08, fidelity payoff,
  `notes/ADRs.md` ADR-0083, corrected from the closed `EncounterEnd`
  row above): `colorectal_cancer.json` is deferred whole, NOT vendored
  — not blocked by the (now-closed) EncounterEnd gap, per the erratum
  above, but by a separate, still-undiagnosed defect one compile layer
  downstream of the interpreter (`compile-trajectory` or the engine,
  not yet localized): `ehrt.sim-check.check`'s own
  `:clinical-content-only-when-admitted` invariant (plus one early
  `:discharge-follows-admission`) rejects at 2 of 3 seeds tried
  (20260802, 42; 300 patients each, ADR-0072's own original counts,
  reconfirmed byte-identical post-fix by ADR-0082). Clinical content is
  compiling or replaying as though outside an open encounter — the
  mechanism is unknown. Revisit trigger: a future session's own
  dedicated investigation of this violation class against
  `colorectal_cancer.json`'s own closure — intake for the fidelity
  arc's own close (ADR-0084).
  **Dated note (2026-08-08, colorectal investigation, `notes/ADRs.md`
  ADR-0085): DIAGNOSED, not fixed — row stays LIVE.** The mechanism is
  now named: `ehrt.sim-trajectory.compile-trajectory/compile-
  trajectory`'s own legacy `:pre-horizon` drop gate tests only an
  event's own flag, with no back-reference check against the encounter
  it belongs to — an `:encounter` opened PRE-horizon (dropped) whose
  own `:encounter-end` and intervening clinical content fire
  POST-horizon (compiled normally) produces clinical-content and
  terminal-discharge steps with no matching compiled admission step,
  confirmed across 100% of the violating population (2 of 2 distinct
  patients, both seeds, three-layer probe evidence in ADR-0085). The
  truncation hypothesis ADR-0082 AR-EE-1a raised is CONFIRMED but
  narrower than stated: the `:pre-horizon` gate is the real mechanism,
  in a straddling-encounter shape that finding never exercised;
  `encounter-closed?`'s own single-encounter scope plays no defective
  role. Revisit trigger, narrowed to a fix session: two candidate fix
  shapes named in ADR-0085 (synthesize a compiled opening step for a
  straddling encounter, or generalize the Wave H `history-phase?`
  back-reference principle to the legacy path) — a genuine design
  choice for the design channel to rule on, not mechanical follow-
  through.
  **Dated note (2026-08-08, straddle fix, `notes/ADRs.md` ADR-0086):
  this row CLOSED — see Done, below.** The author ruled shape (b) —
  generalize `history-phase?`'s own back-reference principle to the
  legacy path — accepted now, shape (a) recorded (see the carry-across
  row, below). `colorectal_cancer.json` is clean (`:status :ok`, 0
  violations) at all three seeds (20260802, 1, 42), 300 patients each.
  The blast-radius probe's one predicted mover (`sleep-apnea`, a
  latent, already-shipped defect the oracle's own byte-digest checks
  could never catch) was licensed by name and confirmed exactly; all
  27 other oracle roots stayed byte-identical.
- **Corpus player `:mllp` transport sink** (`notes/adr/0014-corpus-
  player.md`, deferred whole per that session's own bail-out
  procedure): `:mllp` already exists as a *framing* (byte-level
  0x0B/0x1C 0x0D envelope, `ehrt.corpus-io.framing`) but there is
  no `:mllp` *sink kind* in `ehrt.corpus-io.source-sink`'s own
  `known-sink-kinds` (`#{:dir :file :stdout :blaze}`) (both namespace
  citations in this row corrected 2026-08-05 — the source-sink form at
  ADR-0049, the framing form at ADR-0050 register row A-6 — ADR-0014's
  text predates the tools→corpus rename and corpus-io split;
  transcribed faithfully by ADR-0048, corrected fix-forward here) — a
  real network socket write. Building
  one properly touches three namespaces at once (a new canonical
  schema and constructor in `source-sink.clj`, a new
  scheme in `source-sink-url.clj`'s grammar, and a new write function
  in `sink-write.clj`), not a single isolated extension point —
  assessed against the bail-out procedure and judged to balloon past
  "lands small." Deferred whole, not half-built: the player ships
  `--sink dir:`/`file:` only. Revisit trigger: a session needs wire
  transport and a lands-small shape is identified.
  **Dated note (2026-08-10, marker-only footnotes / mllp ruling,
  `notes/ADRs.md` ADR-0102): this row CLOSED — see Done, below.** The
  author ruled `:mllp` abandoned for now, verbatim "Let's abandon
  `:mllp` for now" — not merely still-deferred pending a lands-small
  shape, as this row's own revisit trigger anticipated. No wire
  transport work landed; the only code change is `bases/cli/src/ehrt/
  cli/help.clj`'s `play --sink` doc line, which had claimed `mllp:` was
  "recognized but deferred" (untrue on its own terms — `mllp:` was
  never in the sink-URL grammar) and now names only `dir:`/`blaze:`.
  `notes/adr/0014-corpus-player.md`'s own "future `:mllp` sink" framing
  is ruled superseded in part by this closure, without editing that
  frozen record; see `.agents/rulings.md`'s "From ADR-0102" section and
  ADR-0102 itself for the full ruling and the three-place inventory of
  where the old framing still lives.
- **`ehrt play`'s own bare reads, true name** (2026-08-09, review-2 arc
  close, `notes/ADRs.md` ADR-0096 Finding 2 / ADR-0097):
  `play-events-from-file`/`play-events-from-dir` carry the identical
  unguarded `slurp`/`sniff-path-format` shape cluster B fixed for
  `mutate`/`gate`/`check`/`show` (ADR-0096), never charted by review 2
  — allowlisted BY NAME in `cli_parse_guard_lint_test.clj` (the
  allowlist entries are this row's own tripwire; removing them is the
  fix's own co-landed gate, ready-made — confirmed non-vacuous,
  ADR-0096: `[play-events-from-dir play-events-from-file]` reported
  with the allowlist stripped). Revisit trigger: the next session
  touching `ehrt play` or the corpus-player slices (`notes/adr/0014-
  corpus-player.md`, the bed-board sink).
  **Dated note (2026-08-10, sim event-log adapter, `notes/ADRs.md`
  ADR-0100): this row CLOSED — see Done, below.** The revisit trigger
  fired (this session touched `ehrt play` directly, landing the sim
  event-log adapter alongside). Both bare reads route through a
  guarded `slurp-play-input` now; the row's own tripwire — the two
  allowlist entries in `cli_parse_guard_lint_test.clj` — is gone, the
  allowlist mechanism itself retired with them.

## Review-3 arc tag paid -- row retired 2026-08-17 (ADR-0145 Step 0)

The row below is moved VERBATIM out of `.agents/plans/roadmap.md`. It closed
when the author pushed `stable-20260815-review-3-fixes`; the ADR-0145 session
verified at its own Step 0 that the remote carries it as an annotated tag
peeling to `b96c246`. Nothing is summarised here; the row is its own text,
unedited.

- OPEN **[review-3-tag-unpushed]** PRIORITY 9 -- the repo review 3 arc closed,
  but its arc tag `stable-20260815-review-3-fixes` at `b96c246` exists only
  locally: the licence's case (i) needed an author-side CI relay the close's own
  prompt did not carry, so the fence's STOP was taken and no ruling came back
  in-session. ADR-0139's Step 0 and mechanical-debt section carry the receipt
  and the one command that pays it.

## Done rotation -- the attic rotation law's own one-time migration, 2026-08-20 (ADR-0161)

Rows moved VERBATIM out of `.agents/plans/roadmap.md`'s own `## Done`
section: lines 285-393 at `891e57e`, 67 rows / 109 lines, unchanged and
in their own order. That section stood at 71 rows / 134 lines under a
header reading "current arc only" -- the 13-day backlog ADR-0139 opened
as finding C-3 and ADR-0144 made larger by retokening the pointers and
adding six missing ones rather than rotating any. The author ruled the
law MECHANICAL on 2026-08-20: `## Done` holds at most 30 LINES, and
rotation is an act of the close ceremony, oldest whole rows first, no
arc boundaries to judge. This block is the one-time migration that
brings the section under the cap for the first time.

ORDER IS PRESERVED, NOT SORTED. The live section was not itself in date
order -- its newest six rows read newest-first, the rest read oldest-
first from 2026-08-08 -- and since 2026-08-18 one of its rows had
swallowed another's continuation lines (ADR-0159 finding F-1, review 5's
watch row W-10). Sorting would be an edit, and it would repair W-10's
specimen in passing. The pair moves here in the broken shape it has, for
W-10 to probe where it now lives.

Nothing is summarised here; each row is its own text, unedited.

- CLOSED 2026-08-19 ADR-0157 **[commit-msg-ascii-hook]** -- `.githooks/commit-msg`
  refuses a non-ASCII commit message before the commit exists, installed by the
  same `core.hooksPath .githooks` that installs the other two. The scan is
  EXTRACTED to `bin/ascii-scan`, which `bin/post-push-verify` check 2 now invokes
  too, so the pre-commit twin and the post-push check cannot drift; both are
  fail-closed on a scanner they cannot run.
- CLOSED 2026-08-18 ADR-0153 **[surge-policy-self-check-202]** -- diagnosed H2
  (the bed-ready transfer bypassed the allocation ladder outright), minimal
  repro plus a run-level test at the exact argv, fixed in `engine.clj` alone.
  `bin/regression-oracle c1a40d0 HEAD` IDENTICAL, 35 roots; `demos/traces/**`
  and six of the census's seven churn shapes byte-identical. Residue rowed as
  `#bed-ready-vacancy-cascade`, not fixed in passing.
- CLOSED 2026-08-08 ADR-0089 **[conviction-arc-close]**
- CLOSED 2026-08-08 ADR-0090 **[vendoring-batch-4]**
- CLOSED 2026-08-09 ADR-0091 **[storefront-fixture]**
- CLOSED 2026-08-09 ADR-0092 **[repo-review-2]**
- CLOSED 2026-08-09 ADR-0093 **[review-2-rulings-landing]**
- CLOSED 2026-08-09 ADR-0094 **[census-closure-file-count]**
- CLOSED 2026-08-09 ADR-0095 **[cluster-a-gate-wiring]**
- CLOSED 2026-08-09 ADR-0096 **[cluster-b-parse-guards]**
- CLOSED 2026-08-09 ADR-0097 **[review-2-arc-close]**
- CLOSED 2026-08-09 ADR-0098 **[permission-legs-and-bare-flags]**
- CLOSED 2026-08-10 ADR-0099 **[fixture-relocation]**
- CLOSED 2026-08-10 ADR-0100 **[sim-event-log-adapter]**
- CLOSED 2026-08-10 ADR-0101 **[adr-footnotes]**
- CLOSED 2026-08-10 ADR-0102 **[marker-only-footnotes]**
- CLOSED 2026-08-11 ADR-0103 **[board-boundary-fix]**
- CLOSED 2026-08-11 ADR-0104 **[ed-tuesday-scenario]**
- CLOSED 2026-08-11 ADR-0105 **[interpreter-horizon-budget]**
- CLOSED 2026-08-11 ADR-0106 **[injuries-b2-assessment]**
- CLOSED 2026-08-11 ADR-0107 **[injuries-arc-close]**
- CLOSED 2026-08-11 ADR-0108 **[simulator-architecture-doc]**
- CLOSED 2026-08-11 ADR-0109 **[latency-second-clock]**
- CLOSED 2026-08-11 ADR-0110 **[latency-demo]**
- CLOSED 2026-08-11 ADR-0111 **[corpus-batching]**
- CLOSED 2026-08-11 ADR-0112 **[batch-straddle-recording]**
- CLOSED 2026-08-12 ADR-0113 **[sim-palgebra-unification]**
- CLOSED 2026-08-12 ADR-0114 **[review-3-user-surface]**
- CLOSED 2026-08-12 ADR-0115 **[review-3-rulings-landing]**
- CLOSED 2026-08-12 ADR-0116 **[engine-seed-contract]**
- CLOSED 2026-08-12 ADR-0117 **[fix-cluster-a-cli-validation]**
- CLOSED 2026-08-12 ADR-0118 **[fix-clusters-b-and-c-help-and-docs]**
- CLOSED 2026-08-12 ADR-0119 **[user-manual-skeleton]**
- CLOSED 2026-08-12 ADR-0120 **[manual-s2-exerciser-and-chapter3]**
- CLOSED 2026-08-12 ADR-0121 **[manual-s3-transport-pair]**
- CLOSED 2026-08-13 ADR-0122 **[positive-seed-invariant-violation-diagnosis]**
- CLOSED 2026-08-13 ADR-0123 **[medication-end-pre-horizon-invariant-fix]**
- CLOSED 2026-08-13 ADR-0124 **[manual-s4-mutate-and-gate]**
- CLOSED 2026-08-13 ADR-0125 **[manual-s5-chapter8-review-close]**
- CLOSED 2026-08-13 ADR-0126 **[citation-sweep-glossary-linkage]**
- CLOSED 2026-08-13 ADR-0127 **[ceremony-scripts-sim-identity-sweep]**
- CLOSED 2026-08-13 ADR-0128 **[agent-facing-hardening-2]**
- CLOSED 2026-08-13 ADR-0129 **[strip-executability]**
- CLOSED 2026-08-14 ADR-0131 **[slug-edn-round-trip]**
- CLOSED 2026-08-14 ADR-0132 **[clinic-decade-rename-and-exerciser]**
- CLOSED 2026-08-14 ADR-0133 **[exact-name-resolution]**
- CLOSED 2026-08-14 ADR-0134 **[manual-review-2]**
- CLOSED 2026-08-15 ADR-0135 **[string-diagram-terminal-outputs]**
- CLOSED 2026-08-15 ADR-0136 **[review-3-fix-a-register-derivations]**
- CLOSED 2026-08-15 ADR-0137 **[stale-path-gate-widening]**
- CLOSED 2026-08-15 ADR-0138 **[review-3-fix-c-ceremony-and-category]**
- CLOSED 2026-08-15 ADR-0139 **[review-3-arc-close]**
- CLOSED 2026-08-16 30cc335 **[d8-5-fence-battery]** -- register
  `.agents/plans/2026-08-16-fence-battery-findings.md`; the ADR was deferred
  to the ruled fixes' own session (ADR-0140).
- CLOSED 2026-08-16 ADR-0140 **[fence-battery-ruled-fixes]**
- CLOSED 2026-08-16 ADR-0141 **[event-log-contract]** -- line added
  2026-08-16 by the ADR-0142 session: the ADR-0141 close indexed itself in
  `notes/ADRs.md` but not here.
- CLOSED 2026-08-16 ADR-0142 **[result-clinical-time]**
- CLOSED 2026-08-16 ADR-0143 **[adr-index-generated]**
- CLOSED 2026-08-17 ADR-0144 **[roadmap-row-contract]**
- CLOSED 2026-08-17 ADR-0145 **[rulings-standing-register]**
- CLOSED 2026-08-17 b96c246 **[review-3-tag-unpushed]** -- the author pushed
  `stable-20260815-review-3-fixes`; verified at the ADR-0145 session's own
  Step 0 as an annotated tag on the remote peeling to `b96c246`.
- CLOSED 2026-08-17 ADR-0146 **[emitter-author-ux]** -- the emitter author gets a
  named audience segment and an entry path; eight of fourteen entry surfaces had
  no route to the event log, now zero do. Registered CLOSED rather than OPEN
  first: it arrived as a chat ruling and was executed the same session, so
  `rulings.md#R-unregistered-request-gets-a-row` was satisfied late, disclosed
  here rather than backdated.
- CLOSED 2026-08-17 ADR-0147 **[compression-arc]** -- A-D landed: the ADR index
  generated, the roadmap and rulings registers under row contracts, the
  continuity register split generated/hand-owned/attic'd. `:onboarding` 3,240 ->
  1,530 by ratchet across the arc.
- CLOSED 2026-08-17 ADR-0148 **[exercised-row-gate-closure]** -- `check-all` over
  `load-registry` gates the register as a population, so a row is gated the
  moment it is registered. Nine hand cases replaced by one test; the dual added
  (a cited or existing exerciser must be a row); and a zero-command source, which
  reported fresh over an empty comparison, no longer can.
- CLOSED 2026-08-17 ADR-0149 **[demos-traces-ungated]** -- `make traces` (a
  `docsgen` leaf, `bin/regen-traces`) reruns each trace README's own fenced
  commands verbatim and CI diffs `demos/traces/`; six register rows share one
  script. No residue: all fifteen derived files are gated. The census found
  `module-mix/messages.txt` was never its own command's output -- proven by
  re-running that command at `f07684c` -- not merely stale.
- CLOSED 2026-08-18 ADR-0151 **[reason-nil-drop-owes-a-bump]** -- census S-1
  landed under its own bump, event contract 1.1.0 -> 1.2.0: `:reason` is
  `{:optional true}` on `:admission`/`:outpatient-visit` and a sibling
  `reason-field` drops the key rather than emitting `:reason nil`. The
  deprecation window was waived first, in its own commit. A DECLARED oracle
  change: 32 of 35 roots moved, predicted exactly before the edit.
- CLOSED 2026-08-18 ADR-0150 **[event-log-shape-defects]** -- the Z-segment
- CLOSED 2026-08-18 ADR-0152 **[sim-theory-edn-hop]**
  context asymmetry and S-6 fixed, S-4 confirmed closed with no code owed.
  Residue re-rowed rather than dropped: S-1 as `#reason-nil-drop-owes-a-bump`,
  S-2 folded into `#careplan-guard-resolution`, S-5 as
  `#surge-policy-self-check-202`.

## Done rotation -- the law's first ordinary application, 2026-08-20 (ADR-0161 close)

One row, rotated by the very close that made the law. ADR-0161's own
CLOSED row joined `## Done` at six lines, taking the section from 25 to
31, so the close ceremony's new rotation step fired against the session
that wrote it -- which is the correct behaviour and is recorded here
cheerfully. Oldest survivor, moved verbatim, order preserved.

- CLOSED 2026-08-19 ADR-0158 **[intake-staging-dir]** -- closed per R4-Q9's own
  recommendation ("state a trigger ... or close it -- it has been deferred since
  2026-07-31"), under the author's standing "Q1 accept all recommendations"
  (2026-08-18). Deferred since 2026-07-31 with the ABSENCE of a trigger declared
  in the row itself, so it could never fire. Re-open on the first real staging
  need, with the trigger stated then. ADR-0144 finding F-6, review-4 D7-5.

## Rotated 2026-08-21 by the ADR-0162 close

ADR-0162's own CLOSED row joined `## Done` at six lines, taking the
section from 25 to 31. The cap fired; the oldest survivor moved verbatim,
order preserved.

- CLOSED 2026-08-19 ADR-0158 **[edit-root-worktree-residue]** -- PAID BY THE AUTHOR
  2026-08-19 and VERIFIED by that session in the same edit root, not taken on
  report: `core.fileMode` true, `core.ignorecase` unset, ~360 mode-only changes
  restored via `git checkout -- .`. Re-derived there -- 0 tracked `100644` files
  executable on disk, 0 CR bytes in the three named `openai.yaml` mirrors, tree
  clean, `bin/preflight` exit 0 with both OK lines. ADR-0157 register row D3-1.
- CLOSED 2026-08-20 ADR-0159 **[repo-review-4]** -- five ADRs (0154 assessment,
  0155-0158 fixes paired G+A/E+C/B+D/F+H), **38 of 72 register rows moved** --
  every one of the 27 fix-session candidates and all 10 R4-Q rulings, plus D8-1
  carried; **34 residue** (24 close-as-fine confirmed, 9 intake, 1 superseded).
  Ledger delta zero against every per-session tally. The close's own four findings
  and review 5's watch-list are in ADR-0159; the register carries dated appends.

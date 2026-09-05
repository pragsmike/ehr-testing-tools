# Roadmap -- rolling plan and backlog

Updated by sessions in the same commit as the work that changes a row. Row
contract (ADR-0144, gated by `ehrt.docs-tooling.roadmap-lint-test`): the first
token is `OPEN` | `CLOSED <date> <ADR-NNNN|sha>` | `DEFERRED (trigger: ...)` |
`EXTERNAL`, and `CLOSED` lives only under `## Done`; then a stable `**[slug]**`,
cited from elsewhere as `roadmap.md#<slug>` and never by line number; `## Next`
rows carry `PRIORITY n`, ascending, so `head` is what is next.

De-scaffold ruling, 2026-08-25: `## Next` holds payload work only -- the
traffic-scale arcs, the performance residue, the player slices. Twenty-five
OPEN rows that were findings, errata, named futures and review bookkeeping
were retired the same day; each keeps one line at the bottom so the slugs
that cite them still resolve, and each row's substance is in the ADR or
record it names. The six-line row cap and the `## Done` rotation both went
with them.

## Next (backlog, no session scheduled)
- OPEN **[performance-residual-sites]** PRIORITY 1 -- what ADR-0169 saw and left
  (`rulings.md#R-move-not-improve`): the **14 independent `engine/replay` calls**
  in `check.clj` (~40% of the post-arc-0 7.26 s check phase), `occupancy-board`
  folding every patient ever created, `decide :discharge`'s boarder `sort-by`, and
  `last-uncancelled-index` (cannot ride either arc-0 carrier without a second code
  path, ADR-0169 F-3). Site ranking within generate NOT re-profiled.
  PARTLY PAID 2026-09-05 (`642d70a`): the per-call `m/validate` compile behind
  `valid-event?`/`valid-persona?`/`valid-ground-truth?` is hoisted to a
  validator built once at load, taking `check-all` on dense-7500 @20 from
  22.70 s to 2.74 s and `make test` from 2,043 s to 1,235 s, so the
  `engine/replay` share above must be re-read against that wall, not the
  7.26 s one it was written for.
- OPEN **[person-simulator]** PRIORITY 3 -- traffic-scale arc 2. New component,
  sibling charter discipline to `patient-simulator`: bespoke hazard-rate life-arc
  processes (`rulings.md#R-mix-1`), households and pregnancy->delivery
  (`R-mix-2`), identification flows (`R-mix-4`), producing the demographic-delta
  stream the engine folds. Four open questions carried for its charter ADR.
  Was blocked on traffic-scale arc 1; UNBLOCKED 2026-08-25, when ADR-0171's
  stream partition landed and discharged
  `rulings.md#R-per-person-streams-before-generator-fixes`. ADR-0168 section 4.
  CHARTERED 2026-08-25 by ADR-0172 (front door, 14 event kinds, 11 gated
  limitations, seven rulings A-G open); arc 2b implements it once ruled.
  COMPONENT LANDED 2026-08-25 by arc 2b under rulings A1 B1 C1 D1 E1 F1 G1:
  all 14 kinds with a counted witness, 11 gated limitations, 18 draws per
  person-year from the `:person` family alone, and the corpus provably
  untouched (ruling F1). What was left on this row was the ENGINE'S FOLD --
  nothing called the component, and nothing might until
  `roadmap.md#engine-fold-extensions` (arc 3) landed it. THAT SENTENCE IS
  NOW SPENT: arc 3a landed the fold across four parts (ADR-0173) and arc 3
  closed 2026-08-27, so the engine DOES call this component and `:persons`
  IS a config key, on in all six gated corpora. Corrected here rather than
  left standing -- it is precisely the shape repo review 5 named, a claim
  true when written that nothing keeps true. What remains on this row is
  whatever the component still owes on its own terms, not a dependency on
  a row that no longer exists.
- OPEN **[oru-control-id-collision]** PRIORITY 4 -- `control-id-for` not
  injective over `:result-available` -- 6 live duplicate MSH-10s in seed-424242,
  1 in clinic-decade demo (sweep-3 record :290); fix moves every corpus, its own
  declared sweep; sweep 5's fan-out must either wait for it or derive from log
  indices.
- OPEN **[cancel-discharge-reopens-an-encounter-that-never-closes]** PRIORITY 9 --
  MEASURED 2026-08-29 while tracing `roadmap.md#ts-3-outpatient-opens-over-an-encounter`,
  and it is a population fact rather than one patient's: a legal
  `:cancel-discharge` re-opens the encounter its own `:discharge` closed
  (`evolve`'s `reopen-encounter`, deliberate -- a reinstated stay is ONE
  encounter), and NOTHING EVER RE-QUEUES A CLOSER FOR IT. At v2 10^5, 55 of
  55 cancel-discharges re-open and 54 have no closer of any kind for the
  remaining ~144,000 events; the 55th is TS-3's patient, whose only "closer"
  is the illegitimate second encounter's own `:outpatient-visit-end`.
  seed-202-ed-tuesday (`PID-000071-e552a7cc`, t=98100) and demo-ed-tuesday
  (`PID-000039-77bfc3a1`, t=128100) each carry one, same shape. It is
  STRUCTURAL, not accidental: `churn/applicable?` gates `:cancel-discharge`
  on `:has-uncancelled-discharge?`, which the static oracle sets only after
  the pathway's own `:discharge` -- the last authored step of both dense
  pathways and of ed-tuesday's -- so the insertion can only land in the end
  gap, with nothing behind it. THE CATALOG PERMITS IT BY CONSTRUCTION:
  `every-encounter-is-opened-and-closed-or-still-open` reads "or still
  open", so a stay that never ends is green, and the 54 stay `:class
  :inpatient` holding the bed the reinstatement gave back, which
  `admitted-occupies-one-slot` requires. So this row is INVISIBLE to every
  gate today and produced no red anywhere -- it is a fidelity question (does
  this repository want reinstated stays that outlive the run?), not a
  correctness one, and it is NOT a candidate fix for TS-3: see that row's
  option (B), rejected there for reasons that apply here too. Any fix is
  draw-affecting and owes its own declared sweep. Record:
  `.agents/session-records/2026-08-29-ts-3-compiled-opener.md`.
- OPEN **[corpus-player-slices]** PRIORITY 10 -- the corpus-player slices chartered
  by ADR-0014. RE-DERIVED 2026-08-29 against the live tree, and the row is now
  TWO items where it was once a list: everything else in it has shipped.
  - **The board accumulator's final state, as an output.** `ehrt.corpus.board`
    already folds a paced v2 stream through `sim-emit-hl7/fold-message`
    (ADR-0175 section 2(f) measured the fold); what has never been built is
    exposing that accumulator's end state as something other than a
    whiteboard render.
  - **`--board`'s event-input blind spot** (ADR-0174 section 2(c)):
    `ehrt play PATH --board` takes message input only
    (`:play-board-unsupported-for-events`), so a bed the cycle marks dirty or
    cleaning stays invisible until a message carries it.

  What has SHIPPED, so that nobody re-derives it a third time: the **bed-board
  sink** under ADR-0067 (2026-08-07, `ehrt play PATH --board`), noticed
  2026-08-26; and **`:mllp` as a sink kind** in arc 4 sweep 5 (2026-08-28,
  ADR-0175 design (g)) -- `known-sink-kinds` has a socket,
  `ehrt play --sink mllp://host:port` frames each message and reads its ACK
  back, positional pairing with MSA-2 checked per pair. ADR-0014's
  three-namespace assessment held exactly.

  This row is the review-5 pattern's own exhibit -- a claim true when written
  that nothing keeps true -- which is why each re-derivation is dated and says
  what it checked. Kept rather than retired because
  `rulings.md#R-unregistered-request-gets-a-row` puts visibility first.
  ADR-0158 (review-4 D7-5); ADR-0175 section 2(f)/2(g) designed both halves.
- OPEN **[corpus-io-ephemeral-port-flake]** PRIORITY 11 --
  `ehrt.corpus-io.mllp-test`'s `a-refused-connection-is-an-error-not-a-throw`
  (`mllp_test.clj:214`/`:215`) is a RACE on a loaded runner, not a defect in
  the code it tests: `ack-server!` binds an EPHEMERAL port, the test stops it,
  and then asserts that connecting to that port is refused -- but a just-freed
  ephemeral port can still accept between `stop!` and `open-sink!`, so
  `mllp/open-sink!` succeeds, `(kernel/rejected? r)` is false and `(:category
  r)` is nil. FIRST AND ONLY SIGHTING 2026-08-31, CI run 33439854438 at
  `8370ead`: 2 failures in 83 assertions, the first non-green CI run of the
  extraction program after twenty-four consecutive successes, and `gh run
  rerun --failed` on the SAME tree went green -- which is the evidence that
  the tree was never the cause. The same namespace ran green in that session's
  own local `make test` in both projects. The test's own file explains why
  ephemeral ports were chosen ("a fixed port in a test suite is a collision
  waiting for a busy host"); this one assertion is where that choice cuts the
  other way, so the fix is a design question (a port the test can prove is
  dead, or a refusal assertion that does not depend on one) rather than a
  one-liner. Rowed here rather than fixed because the finder was fenced out of
  `corpus-io` and the row that would have carried it had no headroom; named so
  the next red in this test is recognised as the SECOND sighting.
  Record: `.agents/session-records/2026-08-31-emit-extraction-planners.md`
  section 8.

## Externals (author-only)
- EXTERNAL **[ci-failure-email]** -- enable GitHub's workflow-failure
  notification email for this repository (one settings toggle); closes the
  nobody-watching gap at zero session cost. ADR-0075, named quality riders
  AR-QR-3, 2026-08-07.
- EXTERNAL **[nist-licensing]** -- send the drafted NIST licensing gist,
  retiring the confirmation-pending posture cited on the storefront Gate row.
- EXTERNAL **[ig-pinning]** -- choose and commit the profile-tier conformance
  target (the Gate row's other caveat).
- EXTERNAL **[clojars-publish]** -- publish to Clojars when satisfied with the
  product (ruled 2026-07-31); ends the greenfield era, output formats freeze
  harder after the first tag. This row IS the Clojars-vs-Maven-Central ruling
  and closes that half of ADR-0001's H5; the group/coordinates naming half and
  publication itself both stay open.
- EXTERNAL **[setup-rewalk]** -- SETUP.md rewalked by an unspoiled human reader
  (F3 superseded-pending-rewalk), widened 2026-08-12 to cover `docs/manual/`
  Chapters 1-2, which narrate the same steps; one rewalk smoke-tests all three.
  Still owed, not executed. ADR-0119.
- EXTERNAL **[guide-ch24-notes]** -- EHR Testing Guide Ch 24 "completeness
  illusion" section notes, the batch-straddle scenario's guide-side treatment
  (placement (c)); the channel may draft on request, grounded in the ADR-0111
  demo's witnessed run. The guide itself lives outside this workspace. ADR-0112.
- EXTERNAL **[upstream-adaptation-skill]** -- upstream the adapted
  repo-adaptation skill to pragsmike/skills (and cyberneutics if wanted); named
  AUTHOR ACTION 2026-08-01.
- EXTERNAL **[design-channel-draft-queue]** -- the B-3/B-4
  carry-forward wording halves (R3-B3-4) only; R3-B3-1's own Example-line
  content resolved at ADR-0118. The channel drafts, the author rules, no session
  until then. ADR-0115.

## Deferred (explicitly, with revisit triggers)
Rows here are LIVE, each owing a revisit trigger in its own token.
- DEFERRED (trigger: a non-epoch-aligned schedule need; a session simulating a
  receiver noticing a missing scheduled delivery; or a need to bundle batches
  into one file-level transfer) **[transport-batching-deferrals]** -- `ehrt
  corpus batch` landed 2026-08-11; three v1 deferrals survive it (`--anchor`,
  interior empty-batch realism, FHS/FTS file-level wrappers), plus a named,
  unresolved taxonomy question about where message loss sits. ADR-0111.
- DEFERRED (trigger: a session willing to widen `:max-tries` or broaden
  `safe-filename-gen`'s own range) **[sink-composability-flake]** --
  `dir-sink-write-then-intake-hash-identity-property-test` occasionally throws
  `Couldn't generate enough distinct elements!`; witnessed once, confirmed
  unrelated to that session's changes and non-reproducing on re-run. ADR-0107.
- DEFERRED (trigger: a session willing to characterize whether
  `MedicationEnd`/`referenced_by_attribute` should itself be idempotent as a
  general interpreter rule) **[veteran-hyperlipidemia]** -- deferred whole, not
  vendored: the module's annual reassessment loop never clears
  `statin_initial`, so it re-fires `MedicationEnd` against an already-ended
  order and fails the medication-end invariant at population scale. ADR-0090.
- DEFERRED (trigger: a session willing to extend the runaway-loop backstop to
  distinguish a real-time-advancing cycle from a zero-advance one)
  **[veteran-mdd-max-steps]** -- deferred whole and BLOCKED, not vendored: a
  legitimate multi-decade recurring-therapy cycle exhausts `max-steps` at every
  horizon tried. Same backstop tension `injuries.json` named first. ADR-0090.
- DEFERRED (trigger: a test scenario needing mid-stay-at-window-open realism)
  **[carry-across-emission]** -- a straddling encounter yields no in-window wire
  traffic under Wave H's own pre-roll, though real hospital censuses do show
  patients mid-stay at window open. This row's compile-layer half is shape (a)
  from ADR-0085, recorded and not built. ADR-0042 AR-2, ADR-0086 AR-SF-5.
- DEFERRED (trigger: a session ranking calibration fidelity for the chronic
  cluster, or a finding that the cap's absence materially skews a census result)
  **[wellness-chronic-meds-cap]** -- upstream's chronic-medications annual
  wellness cap is excluded from `next-wellness-tick` BY RULING, not omitted by
  oversight; a register item, not a design question. ADR-0037 AR-1.
- DEFERRED (trigger: a real consumer for pre-window messages appears)
  **[backload-named-future]** -- pre-roll stays emit-nothing, reaffirmed: the
  backload need is a TOOLS-SIDE construction over sim output, fault-injection's
  own sibling, not a sim feature. ADR-0031 AR-3.
- DEFERRED (trigger: a second consumer, or never) **[verdict-cache-placement]**
  -- verdict-cache placement revisit. ADR-0011.
- DEFERRED (trigger: Wave E's own scheduling, with stroke as the risk-attribute
  register's first consumer) **[imagingstudy-stroke-risk]** -- `ImagingStudy`
  (R5, CHF trigger) and the stroke-risk data source (R7). The DATA-SOURCE half
  is ruled -- curated calibration content, not a ported calculation -- so what
  remains here is scheduling, not an open design question. ADR-0031.
- DEFERRED (trigger: a session extending the census tool itself, not a
  vendoring session) **[census-tool-refinements]** -- refinement (b), no
  per-module census-seed override, stands untouched; (a) and (c) closed at
  ADR-0069. Beside it sit two dated intakes: `:closure-file-count` never counts
  lookup-table CSVs, and the three-seed sample misses population-scale failures.
  ADR-0035, ADR-0036, ADR-0071 AR-VB2-4.
- DEFERRED (trigger: a session needing Observation's own v2 timing/value
  distributions for real) **[uti-o2-distribution]** -- UTI's `ed_bundle.json`
  O2-saturation Observation states carry a `gmf_version 2` `:distribution` this
  loader has never normalized; a stray raw field the procedure gate correctly
  ignores. Disclosed, not built; no vendored module reads the value back.
  ADR-0035.
- DEFERRED (trigger: Wave E's own design session, or whichever session first
  needs a real vital-sign baseline) **[vital-sign-channel]** -- the `VitalSign`
  state type and the `:vital-sign` condition type both need a vital-sign
  REGISTER with baseline values this project does not yet supply. Post-Wave-VS
  the blockage is partial: `covid19` alone is still fully blocked. ADR-0036
  AR-7, ADR-0069.
- DEFERRED (trigger: whichever session next touches the schema-invalid family's
  own `time` gap) **[lookup-column-time-open]** -- Wave LC does special-case a
  `time` lookup-table COLUMN and that evidence is real, but by author ruling
  (compaction A, AR-A-5) it does not resolve the separate schema-invalid
  concern, so this row stays explicitly LIVE pending a session that reconciles
  the two. ADR-0038, ADR-0039.
- DEFERRED (trigger: a session ready to reconcile upstream's own wellness
  machinery with this engine's wellness-cadence design) **[wellness-encounters]**
  -- a NAMED DESIGN ITEM, never routine vendoring; it waits its own pass. This
  row is the anchor it survived three consecutive closes without. ADR-0092,
  ADR-0093 ruling 3 (D7-7).
- DEFERRED (trigger: a session willing to extend the gate's parser to the
  v2-nist 2-column table shape and the simhospital prose-hash shape)
  **[notice-verbatim-coverage]** -- both hashes are still manually verified
  correct, so this is a coverage gap, not an active drift; judged at ADR-0080 to
  balloon past "lands small". ADR-0092, ADR-0093 ruling 3 (D7-8).
- DEFERRED (trigger: the next content-vendoring session with a
  vital-sign-adjacent candidate) **[wave-e-parked]** -- PARKED rather than
  scheduled: four consecutive closes restated it with zero movement, and only
  `covid19` is still genuinely blocked (`congestive-heart-failure` and
  `contraceptives` both produce content post-Wave-VS). ADR-0092, ADR-0093
  ruling 4 (D7-13).
- DEFERRED (trigger: a session with a Synthea checkout available)
  **[synthea-demographics]** -- `given-names.edn`, `surnames.edn` and
  `places.edn` are hand-curated ORIGINALS, replaceable wholesale by a real
  extraction with `ehrt.sim.persona`'s readers unchanged. Not a correctness
  defect: an intention that had stood unregistered since 2026-08-05 and is now
  registered. ADR-0136 finding D7-3(a).
- DEFERRED (trigger: the next session that owns test-suite hygiene, or any
  recurrence) **[mutate-loopback-flake]** -- first recorded 2026-07-28, carried
  18 days in `state.md` alone, which is regenerated at every arc close and was
  never a durable anchor. Closing bar, so the soak can end: if no recurrence
  appears by the next repo review, close this row and D3-2 together against the
  accumulated green runs. ADR-0136 finding D7-4.
- DEFERRED (trigger: a receiver case needing specimen time distinct from result
  time; FHIR-side latency; late amendments or trailing A08s; or order
  transaction time on the wire) **[downstream-latency]** -- the
  latency mechanism, its demo, and the OBR-7/OBX-14 clinical-time increment all
  landed; what survives this row is four named deferrals, each with its own
  revisit trigger. ADR-0109, ADR-0110, ADR-0142 hold them.
- DEFERRED (trigger: a future session that assigns more than one module to the
  same patient) **[wave-g-attachment]** -- upstream's own
  all-waiting-modules-attach-to-one-visit semantics diverges from this project's
  per-module wait only under concurrent modules, which the engine's
  one-module-per-patient assignment never produces. ADR-0037 AR-4.

## Done

Closed work is the ADR record: [`notes/ADRs.md`](../../notes/ADRs.md). This
section carries no per-row ledger and no rotation; rows closed before
2026-08-25 are in `.agents/plans/roadmap-done-2026-08.md` and
`roadmap-done-2026-07.md`, which stay append-only history.

## Done -- retired 2026-08-25 (de-scaffold)

One line a row. `CLOSED` here means "no longer a roadmap row", not "the work
was done" -- each line says which. The section is named `## Done` because that
is where `ehrt.docs-tooling.roadmap-lint-test` requires a `CLOSED` row to live.

- CLOSED 2026-09-05 97d0c31 **[orphan-participant-shape-gap]** -- DONE under R-split, reading 1 of the three ADR-0176 section 8 offered: the fifth invariant keys on a LOG FACT -- the reattributed event is a span start some end cites -- so the fact went into the site predicate rather than a register. The 14 sites given up are taken by two new operators, catalog 26 -> 28. Record: `.agents/session-records/2026-09-05-orphan-participant-split.md`.
- CLOSED 2026-09-05 8c5379a **[event-mutation-catalog-gate]** -- DONE, both follow-ons: the catalog-wide gate runs every sited (operator, population) pair, and the `:expected-findings` vocabulary check landed as a corpus-brick TEST, so Q11(a)'s block dissolved without widening `ehrt.sim-check.interface`. It found a shape gap on its first run: `roadmap.md#orphan-participant-shape-gap`. Record: `.agents/session-records/2026-09-05-q11c-catalog-wide-gate.md`.
- CLOSED 2026-09-05 74c6d87 **[referential-corpus-population]** -- DONE, all 14 cells, and the corpus this row PRICED was never needed: `demos/scenarios/dense-7500/config.edn` already carried all three carrier columns, so the catalog went 12 -> 26 with no new config and 14 of 14 convicting exactly under Q5(a) in both directions. Record: `.agents/session-records/2026-09-05-p7-referential-columns.md`.
- CLOSED 2026-09-05 ADR-0178 **[cancel-invariant-has-no-time-clause]** -- DONE as R-time: the row's diagnosis survived derivation unchanged and the fix is a fifth disjunct, `(< (:t event) (:t target))`, equality deliberately permitted and pinned by a boundary test. Column A's fourth mutation cell becomes witnessable. Record: `.agents/session-records/2026-09-05-window-close-t-absent-not-nil.md`.
- CLOSED 2026-09-05 ADR-0178 **[window-close-t-present-as-nil]** -- CREATED AND CLOSED IN ONE ENTRY, and DISCLOSED AS CREATED. `:window-close-t` is now ABSENT rather than nil when a placeholder window never resolves, and `check-all` gains `every-event-is-schema-valid` first in reporting order, catalog pin 45 -> 46. Record: `.agents/session-records/2026-09-05-window-close-t-absent-not-nil.md`.
- CLOSED 2026-09-04 963902d **[dense-scale-profile]** -- CREATED AND CLOSED IN ONE ENTRY, and DISCLOSED AS CREATED: `demos/scenarios/dense-7500/` is the runnable artefact `docs/consuming-ground-truth.md#scale` needed, and Little's Law named the skeleton's defect exactly rather than bounding it. All eight timed runs exit 0 and both runs of every cell agree to the event. Records: `.agents/session-records/2026-09-04-dense-7500-scale-cell.md` and `-b.md`.
- CLOSED 2026-09-03 ADR-0177 **[cancel-transfer-reinstates-a-new-subject]** -- DONE, the engine half, and the supersession is KIND-AWARE rather than one set grown by a member: `:new` supersedes a `:cancel-transfer` and deliberately NOT a `:cancel-discharge`, this row's own proposed fix having been refuted by trace before any code. Record: `.agents/session-records/2026-09-03-a1-new-supersedes-reinstatement.md`.
- CLOSED 2026-09-01 cfea631 **[event-stream-mutation]** -- DONE, catalog at twelve operators, each carrying its own closed oracle loop under Q5(a) set equality, and the injection stage is POST-RUN and outside `engine/run` entirely. MEASUREMENT BEAT THE DESIGN DOC: the population ledger refuted ADR-0176 three times, all three corrections riding as dated addenda. Records: `.agents/session-records/2026-09-01-event-stream-mutation-design.md`, `-spine.md`, `-breadth.md`.
- CLOSED 2026-09-01 3ec147f **[sim-check-takes-no-facility-config]** -- DONE, `ehrt sim check --config PATH` under ruling Q14(a), honored through the EXISTING `ehrt.sim-check.interface` seam so the Q11(a) fence stands untouched. It threads `:facility` and `:warm-up-seconds` and deliberately not `:order-profiles`, which would break the pipe in the other direction. Record: `.agents/session-records/2026-09-01-sim-check-facility-config.md`.
- CLOSED 2026-09-01 ec52471 **[engine-namespace-extraction-and-apply-unification]** -- DONE, 38 of its 39 (site x accumulator) cells at FULL PRODUCT, the 39th a MEASURED PERMANENT OMISSION under ruling A2(b). The extraction half has its own row, `roadmap.md#engine-emit-namespace-extraction`. Census: `.agents/plans/apply-unification-census.md`. Records: `.agents/session-records/2026-09-01-apply-unification-stage-1.md`, `-stage-2.md`, `-landing.md`.
- CLOSED 2026-08-31 bee0d69 **[engine-emit-namespace-extraction]** -- DONE, and BOTH FILES END PURE FACADES: eighteen census-ordered landings over seventeen sessions, every one with `bin/regression-oracle` and `bin/ground-truth-bracket` IDENTICAL and no declaration. Census: `.agents/plans/engine-extraction-census.md`. Records: `.agents/session-records/2026-08-29-engine-extraction-opener.md` and its sixteen successors through `2026-08-31-emit-extraction-facade.md`.
- CLOSED 2026-08-29 06ce007 **[post-partition-narrative-refresh]** -- DONE, and THE ROW'S OWN COUNT WAS THE FIRST THING THE WORK REFUTED: it said 43 tokens over 6 files and ~72 actually moved. The demo README was almost entirely fresh and the manual was not, an asymmetry that is STRUCTURAL -- an exerciser re-runs the demo's own commands, and nothing gates a manual excerpt. Record: `.agents/session-records/2026-08-29-consumer-contract.md`.
- CLOSED 2026-08-29 62dd9b3 **[ts-4-placeholder-unresolved]** -- DONE, and the row is RIGHT IN EVERY PARTICULAR IT ASSERTED. Option (A) taken: the invariant is now resolved-or-CONSUMED-or-still-open, cause-blind but NOT merge-blind, because an erroneous merge eating a John Doe is a real MPI failure shape the corpus is telling the truth about. Both 10^5 add-on cells now self-check clean. Record: `.agents/session-records/2026-08-29-ts-4-consumed-placeholder.md`.
- CLOSED 2026-08-29 c156690 **[ts-3-outpatient-opens-over-an-encounter]** -- DONE, the row's own MECHANISM CORRECTED IN ITS PARTICULARS by the trace it asked for while its CONCLUSION survived intact. Option (A') ruled: re-bracket a compiled list so each encounter it carries sits behind ONE `:repeat-arrival` step, putting the existing unchanged guard in charge of the whole span. Record: `.agents/session-records/2026-08-29-ts-3-compiled-opener.md`.
- CLOSED 2026-08-29 c5e5f2b **[cancel-transfer-reinstates-a-discharged-patient]** -- DONE: a cancel may not reinstate state a later event superseded, where "superseded" is measured RELATIVE TO THE STATUS THE CANCELLED EVENT ITSELF LEAVES BEHIND. The channel's own option (A) was refuted by that same measurement, which is the session's main finding. Record: `.agents/session-records/2026-08-29-ts-5-superseded-cancel.md`.
- CLOSED 2026-08-29 19a4931 **[ts-1-seventh-bed-arc]** -- DONE: the bed relation grew a SEVENTH arc, `cleaning -> occupied`, ratified into ADR-0174 section 2(c). CHECK-SIDE ONLY -- the engine was already correct -- and gated by an AUTHORED hand-built witness, the shape being zero-frequency in every shipped corpus and unsampleable. Record: `.agents/session-records/2026-08-29-ts-defects-and-blocked-cells.md`.
- CLOSED 2026-08-29 1b4e264 **[ts-2-outpatient-holds-a-bed]** -- DONE for the root it named, and the close's own DIAGNOSIS CORRECTED: `waiting-boarder` never asked whether a candidate was IN A BED, so an open outpatient encounter answered its `not=` test yes. One `some?` clause, draw-neutral. The remainder is a different root. Record: `.agents/session-records/2026-08-29-ts-defects-and-blocked-cells.md`.
- CLOSED 2026-08-29 6eb4aa6 **[emission-add-ons]** -- DONE, not retired: traffic-scale ARC 4 IS COMPLETE, six sweeps of six, and with it ADR-0168's whole five-arc programme. The arc's headline is that the add-ons are worth 1.63x-1.89x the message volume per event. Two things it did NOT do are handed on rather than buried. Record: `.agents/session-records/2026-08-29-traffic-scale-close.md`.
- CLOSED 2026-08-27 ADR-0174 **[engine-fold-extensions]** -- DONE, not retired: traffic-scale ARC 3 IS COMPLETE, each sweep dark-then-on with an IDENTICAL oracle on the dark half. The oracle went 32 roots to 39 and the event contract 1.2.0 to 1.7.0. Two things arc 3 deliberately did NOT close stay named in ADR-0174. Record: `.agents/session-records/2026-08-27-arc-3b-scheduling.md`.
- CLOSED 2026-08-26 ADR-0174 **[multi-encounter-horizon]** -- DONE, not retired: arc 3b sweep 1 lifted it. `admission-only-when-new` became `admission-only-when-no-open-encounter`, and 64 encounters were recovered across the six opted-in corpora. Multiple CONCURRENT open encounters per patient is NOT lifted and stays named in ADR-0174. Record: `.agents/session-records/2026-08-26-arc-3b-encounter-horizon.md`.
- CLOSED 2026-08-25 d6ad63a **[gated-corpus-churn-and-citation-depth]** -- retired: de-scaffold; its counted-witness half landed as a gate in `run_test.clj` on 2026-08-25.
- CLOSED 2026-08-25 d6ad63a **[repo-review-5]** -- retired: de-scaffold; the register and plan stand as dated documents, the arc does not.
- CLOSED 2026-08-25 d6ad63a **[register-gate-row-ownership]** -- retired: de-scaffold.
- CLOSED 2026-08-25 d6ad63a **[ed-tuesday-module-tail-inert]** -- retired: de-scaffold; measurement kept in ADR-0165.
- CLOSED 2026-08-25 d6ad63a **[generator-coverage-depth]** -- retired: de-scaffold; the gate itself survives in `run_test.clj`.
- CLOSED 2026-08-25 d6ad63a **[bed-ready-vacancy-cascade]** -- retired: de-scaffold; realism gap, described in ADR-0153.
- CLOSED 2026-08-25 d6ad63a **[oracle-coverage-extractor-dedup]** -- retired: de-scaffold; the duplication is cross-cited in both tests.
- CLOSED 2026-08-25 d6ad63a **[manual-dimension-5]** -- retired: de-scaffold; the WARN stands in the manual-review report.
- CLOSED 2026-08-25 d6ad63a **[audience-register-paring]** -- retired: de-scaffold.
- CLOSED 2026-08-25 d6ad63a **[lookup-column-time-next]** -- retired: de-scaffold; `roadmap.md#lookup-column-time-open` still carries the live half.
- CLOSED 2026-08-25 d6ad63a **[nightly-quickstart-workflow]** -- retired: de-scaffold.
- CLOSED 2026-08-25 d6ad63a **[generator-source-split]** -- retired: de-scaffold; named future, ADR-0017.
- CLOSED 2026-08-25 d6ad63a **[corpus-display-placement]** -- retired: de-scaffold; named future, ADR-0018.
- CLOSED 2026-08-25 d6ad63a **[markdown-table-dedup]** -- retired: de-scaffold; named future, ADR-0018.
- CLOSED 2026-08-25 d6ad63a **[corpus-generate-engine]** -- retired: de-scaffold; an open question with no disposition owed.
- CLOSED 2026-08-25 d6ad63a **[strip-fresh-hand-case-retirement]** -- retired: de-scaffold; test-compaction judgement, no defect.
- CLOSED 2026-08-25 d6ad63a **[setup-md-hook-citations]** -- retired: de-scaffold; errata, ADR-0157.
- CLOSED 2026-08-25 d6ad63a **[two-clocks-asset-field-audit]** -- retired: de-scaffold as a ROW only -- the finding itself still stands in `hand-owned-assets.edn` (`:verdict :stale`), which is what keeps it visible.
- CLOSED 2026-08-25 d6ad63a **[reader-path-fence-battery]** -- retired: de-scaffold.
- CLOSED 2026-08-25 d6ad63a **[backtick-shorthand-and-denylist-widening]** -- retired: de-scaffold.
- CLOSED 2026-08-25 d6ad63a **[oracle-coverage-roots]** -- retired: de-scaffold; priced and not taken, ADR-0156.
- CLOSED 2026-08-25 d6ad63a **[stale-path-retired-namespace-addendum]** -- retired: de-scaffold.
- CLOSED 2026-08-25 d6ad63a **[careplan-guard-resolution]** -- retired: de-scaffold; `components/patient-simulator/docs/limitations.md` is the authority.
- CLOSED 2026-08-25 d6ad63a **[vendored-module-emission-floor]** -- retired: de-scaffold; the census stands in the 2026-08-24 throughput-spike record.
- CLOSED 2026-08-25 d6ad63a **[no-eligible-provider-throws]** -- retired: de-scaffold; a real rough edge, described in the 2026-08-24 throughput-spike record.

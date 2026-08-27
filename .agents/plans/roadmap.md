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
- OPEN **[post-partition-narrative-refresh]** PRIORITY 2 -- six hand-owned
  narrative documents still quote pre-partition sim output, 43 tokens over 6
  files: `demos/scenarios/ed-tuesday/README.md` (19),
  `docs/manual/05-batch-delivery.md` (10), `docs/manual/01-what-this-is.md` (8),
  `docs/manual/04-time-on-the-wire.md` (4), `docs/manual/00-front.md` (1),
  `docs/use-cases/supply-batch-straddling-traffic.md` (1). Nothing gates them --
  the ADR-0170 species. The trace READMEs beside `make traces` WERE re-derived;
  these were left, deliberately, rather than re-narrating six documents at the
  tail of the migration commit.
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
- OPEN **[emission-add-ons]** PRIORITY 5 -- traffic-scale arc 4
  (`rulings.md#R-mix-7`): re-statement chatter (A08/A31/A28, IN1-only),
  order/result status ladders, DFT^P03 charges, the fan-out/subscriber table;
  rides `roadmap.md#corpus-player-slices`. Reshuffles NOTHING and needs no stream
  work, so it is the one arc that may proceed independently now that arc 3's
  skeleton contract is stable. DESIGNED 2026-08-27 by ADR-0175 (Proposed): the
  census, each add-on classified under `rulings.md#R-skeleton-or-emission` with
  its rejected alternatives, and five rulings A-E awaiting the author -- MSH-12
  2.3-vs-2.4, the v1 add-on set, where chatter ratios live, the
  sampled/stratified gating policy at scale (the ruling this row already owed),
  and the dark-then-on landing shape. Two measured findings the design turned up
  rather than a sweep: at MSH-12 "2.3" every message this project emits resolves
  to HAPI's own generic message, so the base-structural tier is structurally
  vacuous over our own corpus (and one PID-13 rendering fix is all that stands
  between the tree and a clean 2.4 declaration); and NK1 is not available to this
  arc at all, because household state reaches no ground-truth event, so ADR-0172
  limitations row 8 stands and arc 4 lifts none of it.
- OPEN **[corpus-player-slices]** PRIORITY 7 -- the corpus-player slices
  chartered by ADR-0014 that have never had a row in any register. CORRECTED
  2026-08-26 (ADR-0174 section 1(ii)'s disclosure): the **bed-board sink LANDED
  under ADR-0067** (2026-08-07) and ships as `ehrt play PATH --board` --
  `components/corpus/board.clj` folds a paced v2 stream and renders occupied
  beds by ward. This row's original wording named it among the never-rowed
  slices, which was the review-5 pattern -- a claim true when written that
  nothing keeps true. What is actually left, and still UNPRICED and unscheduled
  pending their own author ruling: **`:mllp`** and **accumulator wiring**, plus
  the board's own blind spot ADR-0174 section 2(c) names -- `--board` is message
  input only (`:play-board-unsupported-for-events`), so a bed the cycle marks
  dirty or cleaning stays invisible on the whiteboard until a message carries it.
  Rowed rather than retired because `R-unregistered-request-gets-a-row` puts
  visibility first, and a charter with no row is exactly what that rule exists to
  catch. ADR-0158 (review-4 D7-5). ADR-0175 section 2(f)/2(g) designs both
  remaining halves and re-prices the second: `:mllp` is a new SINK KIND (the
  framing codec and its property tests already exist; `known-sink-kinds` has no
  socket), reachable as `ehrt play --sink mllp://host:port` through the
  designator vocabulary rather than a new flag; and the "accumulator wiring"
  half is mostly built already -- `ehrt.corpus.board` folds a paced stream
  through `sim-emit-hl7/fold-message`, so what is actually left is exposing that
  accumulator's final state as an output rather than only as a whiteboard.

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

- CLOSED 2026-08-27 ADR-0174 **[engine-fold-extensions]** -- DONE, not retired: traffic-scale ARC 3 IS COMPLETE. Arc 3a landed the demographic fold, the two clinical hooks and the identification flow (ADR-0173, four parts); arc 3b landed the encounter horizon (sweep 1), the bed-status cycle plus ADT^A20 (sweep 2) and scheduling state (sweep 3), each dark-then-on with an IDENTICAL oracle on the dark half. The oracle went 32 roots to 39 and its witnessed vocabulary 13 of 21 kinds to 26 of 28; the event contract went 1.2.0 to 1.7.0. Two things arc 3 deliberately did NOT close, both for arc 4: MSH-12 "2.3" against the v2.4 SIU structures, so scheduling's four kinds reach ground truth and never the wire (ADR-0174 ruling C); and whether `:exhausted` should degrade to a visible rejection instead of HALTING a run -- sweep 2 raised it, sweep 3's capacity gate had to work around it, neither answered it. Per-sweep narrative in `.agents/session-records/2026-08-2{5,6,7}-arc-3*.md`.
- CLOSED 2026-08-26 ADR-0174 **[multi-encounter-horizon]** -- DONE, not retired: arc 3b sweep 1 lifted it. `admission-only-when-new` became `admission-only-when-no-open-encounter`, a repeat arrival with no open encounter opens a second one, and PV1-19 renders its id. Max encounter openers per patient was ONE at every corpus this repo had; it is now 2-4 across the six opted-in corpora, 64 encounters recovered in all. What is NOT lifted, and stays named in ADR-0174 rather than as a row: multiple CONCURRENT open encounters per patient.
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

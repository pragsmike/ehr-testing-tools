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
- OPEN **[engine-namespace-extraction-and-apply-unification]** PRIORITY 5 --
  intra-brick extraction of `engine.clj` (4,884 lines at `da21a28`; the ruling's
  4,705 was true at `1b4e264`, 179 lines earlier the same day) and `emit_hl7.clj`
  (2,498) into cohesive namespaces behind unchanged interfaces, FOLLOWED by
  application-path unification (decide-drawn / module-compiled / churn-injected
  events through one apply choke point); one program, each commit
  output-identical and bracket-proven; TS-3's A' span-gate paid part; the
  unified path is also event-stream mutation's injection point. Author-ruled
  2026-08-29. OPENED 2026-08-29: the census is
  `.agents/plans/engine-extraction-census.md` -- every top-level form of both
  files assigned to exactly one proposed namespace, the cross-seam edges, and
  the apply-path inventory that also serves `roadmap.md#event-stream-mutation`.
  **PHASE NOTE, 2026-08-30: `engine.clj`'s EXTRACTION IS COMPLETE.** TEN clusters --
  `streams`, `state`, `encounters`, `evolve`, `fold`, `log-index`, `config`,
  `assignment`, `decide`, `run` -- landed in census order, each with both gates
  IDENTICAL and no declaration; `engine.clj` ends a PURE FACADE (C4(b)), 741 lines /
  43 delegating defs, 202 forms / eleven namespaces. DETAIL: the nine records below (C6(a)).
  **EMIT PHASE OPENED 2026-08-30** (census 2a): order DERIVED, 3b's sixteen edges reproduce
  EXACTLY, the leaves THREE (`hl7-time`, `registry`, `timelines`), `er7` not among them.
  SIX landed -- forms/form-lines/defs/widenings: ELEVENTH `hl7-time` 7/47/3/1; TWELFTH
  `registry` 13/278/10/2 (seven defs `interface.clj` re-exports); THIRTEENTH `timelines`
  5/151/0/5, no re-export, no test site, no `:require`, ZERO edges even inside the cluster;
  FOURTEENTH `er7` 19/193/3/11, the FIRST non-leaf, requiring the SIBLING `timelines` (3b's
  lone `er7`->`timelines` edge); FIFTEENTH `segments` 15/518/3/14, the most FORMS, EIGHT
  regions, THREE landed siblings, ZERO internal edges so ALL widen and none stays private;
  SIXTEENTH `messages` 13/578/1/2, the HEAVIEST (578 form-lines, 122 distinct crossings into
  FIVE landed siblings), THREE regions, the fifteenth's reverse shape -- TEN of twelve private
  movers STAY private because every caller travelled.
  All gates IDENTICAL. TWO left, census 2a's order. NEW WITH THE THIRTEENTH: 3b counts DISTINCT
  (caller, callee) PAIRS, not sites (18 vs 19, then 34 vs 43, 66 vs 70, now 122 vs 137); a sweep
  finds claims stale BEFORE the move, which constraint 6 has no rule for -- disclose, backlog (3).
  NEW WITH THE FOURTEENTH: the caller-travels shape ARRIVES, SIX movers at once staying private
  (`weighted-pick` at scale), and a `#'` var in a C1(a)-fenced test file forces the first
  `^:private` def -- C7.
  NEW WITH THE FIFTEENTH: C7 applied TWICE; NO banner travels, all four heading a section
  the cluster SPLITS; the MOVED TEXT itself needed requalifying (five names that resolved
  through this file's own defs); TWO requires went dead, a first; and a PRIOR cluster's
  banner can be FALSIFIED by a later move -- three were, paid in the move commit.
  NEW WITH THE SIXTEENTH, every item a PRIOR CLASS AT ITS LIMIT rather than a new one:
  requalification goes 5 sites to SIXTY-FOUR, depth being what drives it; the residue-claim class
  widens past banners into a SIBLING `ns` DOCSTRING (six claims over four banners plus
  `segments.clj`); banners TRAVEL again, and they are the exact four the fifteenth left because
  their builders stayed; constraint 5's prohibition reaches TEN movers; and the `#'` class is
  EXHAUSTED -- 106 sites re-read whole, none naming a mover, so NO C7 def is owed. The moved
  prose travelled UNTOUCHED, carrying no `below`, `this file` or `this namespace` -- a first.
  THE HAND-OWNED-ASSET TRIPWIRE IS A RECIPE, not a lucky catch: read that
  registry's own SOURCES during the pre-move sweep. FIVE of the ten engine
  sessions fired it, each RED-FIRST with a successor bumping `gt-emitters.svg`'s
  `:reviewed-at`; the sixth, ELEVENTH and TWELFTH read them and fired nothing --
  `docs/dev/simulator-architecture.md` names ENGINE forms by DEFINING FORM but
  EMITTER forms as BARE NAMES. A GREEN LOCAL SUITE IS NOT EVIDENCE THE ROW WILL
  STAY GREEN -- the test reads `git log -1` on the SOURCE and cannot see an
  uncommitted edit. NEW WITH THE TENTH: a sweep can be forced to edit
  `components/oracle/src/ehrt/oracle/digest.clj`, the BRACKET'S OWN SOURCE, and
  the bracket then aborts its soundness check over a comment -- bracket the MOVE
  alone (sound, no declaration) and prove the sweep output-inert from its diff.
  NEW WITH THE ELEVENTH: THE INSTRUMENTS SWAP AT THE EMISSION LAYER -- the bracket
  excludes the `:hl7` half BY CONSTRUCTION, so the ORACLE is load-bearing. NEW WITH
  THE TWELFTH: CENSUS CONSTRAINT 6 FIRED, RED-FIRST -- a charter row pinned a
  FOUR-WORD phrase, under the shingle floor, found only by HAND-READING both registers.
  THE CENSUS IS CORRECTED BY SESSION -- the sixth, ninth and tenth each corrected it,
  and the tenth confirmed 4a's sole-producer claim and 4b's ten-step fold exactly.
  CONSTRAINT 5 IS READ AS A PROHIBITION, not an instruction to widen: a private mover
  stays `defn-` unless a caller stays behind, and after the tenth nothing could call.
  A FACADE MAY REQUIRE ITS IMPLEMENTATIONS; AN IMPLEMENTATION MAY NOT REQUIRE ITS
  FACADE -- why the tenth qualified bare names the nine before it could leave alone,
  and why census constraint 1's `stream` is paid with a lazily-resolved shim in
  `ehrt.sim-engine.run` rather than a `:require`; both directions asserted live.
  TWO BACKLOGS FOR THE RULED REPOINT PASS. (1) FENCED CITATIONS: C1(a) forbids touching
  test files; the ninth alone left TWELVE stale `engine.clj`-by-file citations across ten
  test files, one a NAMESPACE claim about a private mover, and the tenth adds one more.
  The emit phase's first FENCED row is the thirteenth's, `person-simulator/limitations_test.clj:152`;
  the SIXTEENTH adds THREE more, all alias-qualified prose naming `emit-hl7/siu-message`,
  a mover that stays private: `siu_test.clj:11`/`:72` and `sim/siu_run_test.clj:106`.
  (2) RETIREMENT CANDIDATES: FOURTEEN of the facade's 43 delegating defs now have no named
  caller anywhere -- eleven already so, three made so by the tenth move exactly as the ninth
  priced. The FACADE RULE kept every one: retiring a def is the repoint pass's business.
  (3) STALE-BEFORE-THE-MOVE, a new class, neither fenced nor its finder's: `emit_hl7.clj:514`
  (now `segments.clj`'s `pv1-segment`) cites a registry comment that left for `registry.clj:41`
  with the TWELFTH move (so that session's disposition of `emit_hl7_test.clj:1306` to cluster 5
  is wrong -- already stale); `encounter-spans` cites `engine/stamp-encounter`, unresolvable
  since the THIRD engine session; and a FAMILY of un-re-depthed `docs/` citations of files that
  really live under `components/sim/docs/` -- `operational-models.md` in 26 live files,
  `patient-state-model.md` in 30, a `research/` path -- `er7.clj` carries one, `segments.clj` five.
  COVERAGE, disclosed: neither bracket reaches a cancel decide and the gated corpora resolve
  no citation, so two engine brackets are blind to six forms each; the suite and live `-M:dev`
  seam checks carry them. ENGINE-PHASE INSTANCE DETAIL for every claim above -- corrected census
  sections, widening and `weighted-pick` counts, the `gt-emitters.svg` red-first history, both
  backlogs' per-form enumerations -- is in the nine engine records below (C8(a), 2026-08-31).
  Records:
  `.agents/session-records/2026-08-29-engine-extraction-opener.md`,
  `.agents/session-records/2026-08-30-engine-extraction-state.md`,
  `.agents/session-records/2026-08-30-engine-extraction-encounters.md`,
  `.agents/session-records/2026-08-30-engine-extraction-evolve.md`,
  `.agents/session-records/2026-08-30-engine-extraction-fold.md`,
  `.agents/session-records/2026-08-30-engine-extraction-log-index.md`,
  `.agents/session-records/2026-08-30-engine-extraction-config-assignment.md`,
  `.agents/session-records/2026-08-30-engine-extraction-decide.md`,
  `.agents/session-records/2026-08-30-engine-extraction-run.md`,
  `.agents/session-records/2026-08-30-emit-extraction-hl7-time.md`, `.agents/session-records/2026-08-30-emit-extraction-registry.md`, `.agents/session-records/2026-08-31-emit-extraction-timelines.md`, `.agents/session-records/2026-08-31-emit-extraction-er7.md`, `.agents/session-records/2026-08-31-emit-extraction-segments.md`, `.agents/session-records/2026-08-31-emit-extraction-messages.md`.
- OPEN **[event-stream-mutation]** PRIORITY 6 -- mutation moves to the
  ground-truth event stream (emitters inherit mutations); ADR-0166's test-side
  event mutations promoted to a shipped operator catalog with `check` as oracle;
  file-level operators remain only for lowering-layer faults; AFTER
  `roadmap.md#engine-namespace-extraction-and-apply-unification`; design ADR
  first. Author-ruled 2026-08-29.
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

- CLOSED 2026-08-29 06ce007 **[post-partition-narrative-refresh]** -- DONE, and THE ROW'S OWN COUNT WAS THE FIRST THING THE WORK REFUTED. It said "43 tokens over 6 files", counted 2026-08-25; four sweeps re-witnessed part of what it counted and created staleness it could not have known about, so ~72 tokens actually changed and the row UNDERSTATED the work. Everything re-derived against runs regenerated at `2e141f2` -- `corpus generate sim` at seed 20260811 x 100 patients with `config.edn` and `config-latency.edn`, `corpus batch --interval 60`, and `play --board 60 --rate 10000000` over each. Per file, rowed -> actual: ed-tuesday README 19 -> 10, `01-what-this-is.md` 8 -> ~16, `04-time-on-the-wire.md` 4 -> ~25, `05-batch-delivery.md` 10 -> ~18, `00-front.md` 1 -> 2, `supply-batch-straddling-traffic.md` 1 -> 1 (via `use-cases.edn`, which is its docsgen source). THE DEMO README WAS ALMOST ENTIRELY FRESH AND THE MANUAL WAS NOT, and the asymmetry is STRUCTURAL rather than luck: `bin/demo-exerciser-ed-tuesday` re-runs the demo's own commands and asserts against them, and nothing gates a manual excerpt. The README reproduced exactly at HEAD in every structural figure checked -- 1,269 events, 1,554 messages, 147 encounter openers across 111 patients (30 with more than one, max 3), 141 bed turnovers, 35 openers naming an appointment, all four SIU families, the 620-batch listing to the count, both quoted board snapshots byte-for-byte, the ground-truth-invariance digest, the wrapper's own `head -c 100`/`tail -c 45` bytes, and both Hernandez MSH segments with their EVN-2 times. TWO FILES CONTRADICTED THEMSELVES, which is what a PARTIAL re-witness leaves behind: chapter 5 opened on "283 messages across 34 occupied hourly buckets" and closed on ":verified true on all 615" -- neither true of any run at this commit (1,554 across 620) -- and the README's PV1 paragraph said "630 of this run's 631" while a dated note two paragraphs below already said 681. In each case a sweep updated the paragraph it was about and left the arithmetic around it. Measured corrections beyond those: dirty/cleaning board lines 43 -> 44 (15 and 29), first snapshot 4 -> 3 occupied beds (which contradicted a transcript printed further down the same file), census peak 14 -> 12, and a SECOND blank PV1-19 whose cause is the status ladder -- a rung restating a pending lab is rendered by the same builder as the result it restates, so it inherits the same absent encounter. Chapter 4's invariance digest moved `d00bf49c` -> `fe13a7ba` and its event count 383 -> 1,269, both from the four arc-3 opt-in keys `config.edn` took on 2026-08-26/27; its two-clocks cast and its ORU pair were both pre-partition witnesses and are re-derived rather than adjusted. `straddle-timeline.svg`'s tripwire fired an eleventh time and NOT ONE VALUE IT DRAWS MOVED -- `:reviewed-at` bumped in the refresh's own successor commit, predicted rather than discovered. Record: `.agents/session-records/2026-08-29-consumer-contract.md`.
- CLOSED 2026-08-29 62dd9b3 **[ts-4-placeholder-unresolved]** -- DONE, and the row is RIGHT IN EVERY PARTICULAR IT ASSERTED, which the two rows before it were not. Reproduced at both 10^5 add-on cells at `23901f4`: `PID-007500-e98926c1` registers a placeholder John Doe at t=37017 with `:window-close-t 382617`, is admitted as "Unidentified patient" into ED-98 and discharged at t=80217, and at t=177420 is drawn out of a 666-strong eligible set as the `:merged` participant of an ORDINARY churn `:merge` carrying no `:cause` -- one violation in 129,415 events at `nobed` and in 171,864 at `v2`, same patient, same instants. FOUR THINGS THE TRACE ADDED. (i) What churn destroyed is a FILL: the seeded resolution is `{:branch :fill :survivor-patient-id nil}`, the person having no prior identified patient, so the row's "can never be filled or identification-merged" is right and the thing actually lost is one `:identity-fill`. (ii) "Can never" is MEASURED, not reasoned -- 1,062 resolution steps seeded and 1,061 decided at each cell, the missing one being this patient's, swallowed by the run loop's `:merged` short-circuit, so the second latent defect fires in NEITHER 10^5 log. (iii) IT IS REAL IN THE CODE ALL THE SAME: `identity-fill-outcome` refuses only on the demographics still saying `:placeholder` and `evolve :merge` leaves the demographics untouched, so only that one `if` in `run` stops a fill on a consumed record, and `decide :identification-merge` guards the SURVIVOR's status and never the placeholder's own. (iv) THE SOURCE CARRIES AN ARGUMENT THIS PATIENT REFUTES -- `decide :identification-merge`'s docstring says churn's lottery needs no change because `never-mergeable?` excludes `:new` "and a placeholder patient who registered and was never admitted is exactly `:new`", which holds only for placeholders that are never admitted, and the `:identity-unavailable` hook mints an ARRIVAL that the same hook then ADMITS. LETTERED, and option (A) taken on the channel's recommendation, the trace having confirmed the row: the invariant is now resolved-or-CONSUMED-or-still-open -- cause-blind but NOT merge-blind, a consuming merge must land at or before the due close -- because an erroneous merge eating a John Doe is a real MPI failure shape, the corpus is telling the truth about it, and the engine did nothing wrong. All three riders landed: the docstring names the shape and carries the witness, `no-resolution-after-a-placeholder-is-consumed` is the companion that makes the clause safe (born with three firing mutations), and `placeholder-dispositions` counts six disjoint columns of which `consumed-by-churn` is one, so the tolerated shape is COUNTED. OPTION (B) REJECTED, and its draw analysis CORRECTS THE LETTER'S OWN PREDICTION: it would move no shipped corpus at all -- in all 44 gated and bracketed corpora zero churn merges have an open-window placeholder among their candidates, and not one bracketed root even has both a placeholder and a churn merge -- but it would move the scratch traffic-scale cells at 694 of 747 churn merges (`nobed`) and 697 of 750 (`v2`), since `uniform-choice` is `(.nextInt rng (count candidates))`. It is also censorship: churn routing around placeholders would make the simulator structurally incapable of emitting the shape. `bin/ground-truth-bracket 23901f4 62dd9b3` IDENTICAL over 38 roots, so nothing re-pinned and no sweep spent; `arc0-invariant-catalog` re-pinned 43 -> 44 with the standard disclosure. **BOTH 10^5 ADD-ON CELLS ARE NOW MEASURED AND SELF-CHECK CLEAN** -- `nobed` 129,415 events / 165,946 messages / **msg/event 1.2823** / 232.67 s, `v2` 171,864 / 233,286 / **1.3574** / 270.37 s, with emit and spool running at 10^5 on an add-on corpus for the first time in this programme. The completed `v2` series climbs **1.050 -> 1.217 -> 1.357** msg/event and is still climbing; check is sub-linear at **0.914**, the first honest check exponent on an add-on corpus, confirming TS-6 by measurement; generate stays at **1.624**, unmoved by a check-side fix. The 10^6 cell stays DECLINED on the completed series' own arithmetic (F3): emit's peak heap projects to 14.5 GB against a 3.88 GB ceiling. Record: `.agents/session-records/2026-08-29-ts-4-consumed-placeholder.md`.
- CLOSED 2026-08-29 c156690 **[ts-3-outpatient-opens-over-an-encounter]** -- DONE, and the row's own MECHANISM CORRECTED IN ITS PARTICULARS by the trace it asked for while its CONCLUSION survives intact. The row said a module-compiled `:admission` was followed by "the module's own LATER compiled `:outpatient-visit`". Neither half is so: `compile-trajectory` short-circuits on `encounter-closed?` after the first horizon-phase `:encounter-end`, so a compiled step list holds AT MOST ONE encounter, and the `:admission` at t=240300 is the first step of the authored `dense_fast` pathway carried by a REPEAT ARRIVAL, correctly routed through `decide :repeat-arrival`. Bronchitis compiles `[:delay 1,676,160 min -> :outpatient-visit -> ... -> :outpatient-visit-end]` and 40,260 + 100,569,600 = 100,609,860 to the second. THE STRUCTURAL FACT THE FIRST DIAGNOSIS COULD NOT SEE FROM THE LOG is that the patient holds TWO CONCURRENT QUEUE ENTRIES -- the module's is parked at t=100,609,860 from t=40,260 by its own compiled delay, and the whole inpatient episode happens on a different entry inside that gap, so "nothing left in the queue to close it" is true of the wrong queue. The conclusion stands and is what was fixed: a compiled encounter opener is attached raw by `decide :registered` and never asks `encounter-openable?`; both wrappers that DO ask ran for this patient and both correctly refused. LETTERED, and the author ruled option (A') -- re-bracket the compiled list so each encounter it carries sits behind ONE `:repeat-arrival` step, opener through closer, putting the EXISTING unchanged guard in charge of the whole span, with everything before the opener (the parking delay) left outside it. BARE OPTION (A), a guard on the opener alone, WAS REFUTED BY ITS OWN DANGLING-STEP ANALYSIS: the tail would still run, its clinical content would be stamped with whatever other encounter was open, and its trailing `:outpatient-visit-end` would close that other encounter -- passing every row in the catalog, so 33,950 red rows would have become zero red rows and a silently false log. Option (B) close-at-reopen was rejected (unaskable predicate, draw-affecting, and it fixes TS-3 by TIMING rather than by law) and option (C) compile-time was struck. `bin/ground-truth-bracket 11765bb c156690` IDENTICAL over 38 roots, so NO declaration and NOTHING re-pinned; `make docsgen` moved no generated file at all and the event contract stays 1.8.0. MEASURED AFTER, and the fix fires ONCE in the whole population: 423 compiled spans at nobed with ZERO refused and the corpus byte-count unchanged at 129,415 events, 424 at v2 with exactly ONE refused. At v2 10^5 `admission-only-when-no-open-encounter` goes 1 -> **0** and `outpatient-patients-occupy-no-bed` 33,950 -> **0**, leaving ONE violation in 171,864 events. BOTH 10^5 cells now stay BLOCKED on the SAME single row, `roadmap.md#ts-4-placeholder-unresolved`, same patient and same instant at each. Record: `.agents/session-records/2026-08-29-ts-3-compiled-opener.md`.
- CLOSED 2026-08-29 c5e5f2b **[cancel-transfer-reinstates-a-discharged-patient]** -- DONE, and the design question the row refused to answer is ANSWERED: a cancel may not reinstate state a later event superseded, where "superseded" is measured RELATIVE TO THE STATUS THE CANCELLED EVENT ITSELF LEAVES BEHIND. MECHANISM MEASURED BEFORE ANY FIX, which is what the row asked for and what settles between its two candidates: a probe wrapping the `decide` VAR reports the subject `:status :discharged`, `:location` nil AT DECIDE TIME at both of the row's witnesses (`PID-004302-fa1ab125` t=303660 into SURGERY-91; `PID-005562-03ed543c` t=363060 into ED-31). So the discharge is already folded when the cancel is decided -- the run loop folds each decide's events into `world` before popping the next queue entry, and there is no simultaneous batch for a decide-time test to miss. It is the reinstate-index reading, not the batch-order one. `bed-reoccupied-by-someone-else?` passes it because the occupancy board reads NIL for both beds: the patient is gone, not displaced. THE CHANNEL'S OWN OPTION (A) WAS REFUTED BY THAT SAME MEASUREMENT and the correction is the session's main finding -- rejecting every `:discharged` subject would reject every `:cancel-discharge` in the repository (55 of 55 at nobed 10^5), since `:discharged` is the status a cancel-discharge exists to find. DRAWS ZERO ON BOTH PATHS, asserted by a test against a pristine `Random` rather than read off the source. `bin/ground-truth-bracket a4e8698 c5e5f2b` IDENTICAL over 38 roots, so NO declaration and NOTHING re-pinned, and the per-corpus count explains it exactly: the only cancel DECIDE in the whole gated population is one legal `:cancel-discharge` in seed-202, whose other 7 reinstating cancels all carry `:in-error true` and come from `decide :transfer-in-error`, which never routes through `decide :cancel-transfer`. MEASURED AFTER: `outpatient-patients-occupy-no-bed` goes 372,123 -> **0** at nobed 10^5 (all 11 patients cleared) and 495,205 -> 33,950 at v2 10^5 (11 of 12 cleared; the residue is one patient, TS-3's). THE ROW'S OWN "BLOCKS both 10^5 v2 cells, alone" IS CORRECTED: it never did. `roadmap.md#ts-4-placeholder-unresolved` blocks the nobed cell alone and `roadmap.md#ts-3-outpatient-opens-over-an-encounter` blocks the v2 cell alone, so both cells stay BLOCKED. Contract 1.7.0 -> 1.8.0 for the two new rejection reasons. Record: `.agents/session-records/2026-08-29-ts-5-superseded-cancel.md`.
- CLOSED 2026-08-29 19a4931 **[ts-1-seventh-bed-arc]** -- DONE: the bed relation grew a SEVENTH arc, `cleaning -> occupied`, ratified into ADR-0174 section 2(c) as its fourth ratification. Reproduced at the close's own seed first (16,322 events, the same two beds at the same instants), which confirmed the close's mechanism exactly and added one detail it did not have: at ED-176 the cancel reinstates its patient into a bed a DIFFERENT patient has used and vacated in between, so the arc is not "the same occupant returns". CHECK-SIDE ONLY -- the engine was already correct, `decide :bed-ready`'s guard no-ops on the bed the cancel leaves, and `bin/ground-truth-bracket` reads IDENTICAL over the whole session. Gated by an AUTHORED hand-built witness whose `[:cleaning :occupied]` arc count is pinned `pos?`, because the shape is zero-frequency in every shipped corpus and could not be sampled.
- CLOSED 2026-08-29 1b4e264 **[ts-2-outpatient-holds-a-bed]** -- DONE for the root it named, and the close's DIAGNOSIS CORRECTED. Not "the authored pathway walk is not gated on encounter class": the close's own witness (log index 92836, reproduced here) carries `:bed-ready true`, a field only `bed-ready-transfer-event` writes, and the reproducer that proves it has no `:transfer` step in any pathway. `waiting-boarder` never asked whether a candidate was IN A BED, so an open outpatient encounter -- `:status :admitted` from its opener, `:location` nil, `:home-ward` stale from an earlier inpatient stay -- answered its `not=` test yes and was handed the next bed to free in that ward, ranked FIRST because its `:admitted-at` was stale too. One `some?` clause. Draw-neutral: the branch it takes more often is the pre-existing zero-draw one. The v2 10^4 cell, BLOCKED since the close, now self-checks CLEAN at the same 16,322 events; the 10^5 cells drop from 24/25 offending patients to 12/13, and the remainder is a different root, rowed as `roadmap.md#cancel-transfer-reinstates-a-discharged-patient`.
- CLOSED 2026-08-29 6eb4aa6 **[emission-add-ons]** -- DONE, not retired: traffic-scale ARC 4 IS COMPLETE, six sweeps of six, and with it ADR-0168's whole five-arc programme. Sweep 1 flipped MSH-12 to "2.4" so every message resolves to a real v2.4 structure instead of `GenericMessage$V23`; sweeps 2-4 landed ruling B1's three tranches -- re-statement chatter plus DFT^P03 charges, order/result status ladders, and SIU -- each behind its own opt-in key, each dark-then-on with both `bin/ground-truth-bracket` brackets IDENTICAL on the dark half; sweep 5 landed the fan-out subscriber table and `:mllp` as a sink kind, the first sweep with BOTH brackets identical at every commit; sweep 6 (2026-08-29) was the measurement close, which priced design (h)/ruling D1's `gate v2 --sample-add-ons` at 2.23 ms/message and a 25.2% wall saving, and measured the arc's own headline: the add-ons are worth 1.63x-1.89x the message volume per event (0.643 msg/event before, 1.05-1.21 after). Two things arc 4 did NOT do, both handed on rather than buried: NK1 stays unavailable because household state reaches no ground-truth event (ADR-0172 limitations row 8, untouched by any sweep); and sweep 6's rerun found that the nine-key configuration does not complete its own self-check at 10^4 or above -- four invariant families red, diagnosed and rowed in `.agents/session-records/2026-08-29-traffic-scale-close.md`, none reachable by any shipped corpus. Per-sweep narrative in `.agents/session-records/2026-08-2{7,8,9}-arc-4*.md` and `2026-08-29-traffic-scale-close.md`.
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

# Roadmap -- rolling plan and backlog

Updated by sessions in the same commit as the work that changes a row. Row
contract (ADR-0144, gated by `ehrt.docs-tooling.roadmap-lint-test`): the first
token is `OPEN` | `CLOSED <date> <ADR-NNNN|sha>` | `DEFERRED (trigger: ...)` |
`EXTERNAL`, and `CLOSED` lives only under `## Done`; then a stable `**[slug]**`,
cited from elsewhere as `roadmap.md#<slug>` and never by line number; `## Next`
rows carry `PRIORITY n`, ascending, so `head` is what is next; six lines a row.

## Next (backlog, no session scheduled)
- OPEN **[performance-residual-sites]** PRIORITY 1 -- what ADR-0169 saw and left
  (`rulings.md#R-move-not-improve`): the **14 independent `engine/replay` calls**
  in `check.clj` (~40% of the post-arc-0 7.26 s check phase), `occupancy-board`
  folding every patient ever created, `decide :discharge`'s boarder `sort-by`, and
  `last-uncancelled-index` (cannot ride either arc-0 carrier without a second code
  path, ADR-0169 F-3). Site ranking within generate NOT re-profiled.
- OPEN **[gated-corpus-churn-and-citation-depth]** PRIORITY 2 -- ADR-0169 F-1/F-2,
  measured while building arc 0's gates. Of the four `gated-runs` corpora only
  seed-202 carries a reinstating cancel (ten events), and the ONLY two cited end
  events across all four resolve to nil -- so no gated corpus witnesses ADR-0164's
  resolution SUCCEEDING. Covered by co-landed defspecs, counts asserted so a drift
  to zero goes red; the gated population itself stays one run deep.
- OPEN **[repo-review-5]** PRIORITY 3 -- chartered roughly 15 ADRs past the
  review-4 close by ADR count, not calendar (`rulings.md#R-review-cadence-in-adrs`,
  measured from the prior CLOSE as ADR-0139 worked it): **approximately ADR-0174**.
  Inherits a THIRTEEN-row watch-list carried in `notes/adr/0159-review-4-arc-close.md`,
  not here: W-1 born-red gates, W-2 the `exempt` disposition's missing ratchet, W-3
  row-id citations, W-4..W-13 including two register gates ADR-0159 found narrow.
- OPEN **[register-gate-row-ownership]** PRIORITY 4 -- both register contracts gate
  row SHAPE, not row OWNERSHIP, and both are green over a live defect. `roadmap.md`:
  `c509e46` inserted the ADR-0152 row inside the ADR-0150 row, so five continuation
  lines now sit under the wrong slug (ADR-0159 F-1). `rulings.md`:
  `R-full-suite-before-push` gained its whole wrapper clause at ADR-0155 and names no
  ADR for it, while the arc's other two widenings do (F-2). Found, rowed, not fixed.
- OPEN **[ed-tuesday-module-tail-inert]** PRIORITY 5 -- `demos/scenarios/ed-tuesday`
  declares TEN emittable ground-truth event types through its four-module tail and
  produces ZERO of them: 407 events at seed 202, none carrying a `:citation`, all
  from the five hand-authored ED pathways. Its own config header discloses the
  low-incidence mechanism; ADR-0165's coverage gate is what measured the
  consequence. Green only because the gate asks for a union across corpora.
- OPEN **[generator-coverage-depth]** PRIORITY 6 -- ADR-0165's gate asks whether a
  type appears AT ALL. Three of its eleven -- `:admission`, `:discharge`,
  `:diagnostic-report` -- are covered by exactly ONE cited event in exactly ONE
  gated run (seed 5 over clinic-decade), so a single population reshuffle takes
  all three dark at once. The same one-root-deep fragility ADR-0156 named for the
  oracle's capacity witness, now measured on the generator side.
- OPEN **[bed-ready-vacancy-cascade]** PRIORITY 7 -- a bed-ready transfer
  vacates its own ORIGIN bed and nothing looks for a boarder waiting on that
  ward: only `decide :discharge` runs the search. Witnessed at seed 202,
  `t 78060` -- RENAL-04 freed by a bed-ready pull, a Renal boarder still in ED
  surge 420s later. Realism gap, not an invariant violation once ADR-0153
  landed. Class exposed by ADR-0153's diagnosis, rowed rather than fixed there.
- OPEN **[oracle-coverage-extractor-dedup]** PRIORITY 8 -- the two halves of the
  oracle-coverage gate each carry their own copy of the `(def <name>` /
  `(def ^:private <name>` extractor: `ehrt.docs-tooling.oracle-coverage-test`'s
  `def-form` and `ehrt.integration.oracle-coverage-test`'s `committed`. Sharing
  needs `projects/integration` to compose `docs-tooling`, which its deps.edn
  refuses twice (AR-3) -- so the copies stand, cross-cited. ADR-0160.
- OPEN **[manual-dimension-5]** PRIORITY 9 -- manual-review run 2 passed with
  warns, and dimension 5 (running-example continuity) stays WARN as the manual's
  one standing open row: `ed-tuesday` is HL7v2-only and structurally cannot
  supply Chapters 6-8 their FHIR mutation, FHIR-gate calibration, or
  foreign-corpus material. Disclosed, not silently substituted; not a defect
  under the dimension's own reading. ADR-0134.
- OPEN **[audience-register-paring]** PRIORITY 10 -- `docs/dev/AUDIENCES.md`
  pares to five behavioral segments and its own "Seven segments" header is
  corrected in the same edit. Ruled 2026-08-12 (ADR-0113 R4, author "Q1 a");
  execution deferred to a later docs session, not chartered.
- OPEN **[lookup-column-time-next]** PRIORITY 11 -- the lookup-column `time`
  gap in the schema-invalid family, ratified as real (2026-08-06) and still
  untouched; bulk vendoring batched by closure family follows once the catalog
  fully walks. Deliberately distinct from `roadmap.md#lookup-column-time-open`
  below, which the author ruled stays live regardless. ADR-0039, ADR-0066.
- OPEN **[nightly-quickstart-workflow]** PRIORITY 12 -- `make quickstart` gains
  a nightly integration workflow plus the single-sh-fence guard in README
  (`quickstart_fresh`'s own docstring corrected in the same change).
- OPEN **[generator-source-split]** PRIORITY 13 -- the generator-source
  three-concerns split, a named future. ADR-0017.
- OPEN **[corpus-display-placement]** PRIORITY 14 -- `ehrt.corpus.display`'s
  placement is presentation-leaning, a named future. ADR-0018.
- OPEN **[markdown-table-dedup]** PRIORITY 15 -- markdown-table helper dedup, a
  named future. ADR-0018.
- OPEN **[corpus-generate-engine]** PRIORITY 16 -- should `corpus generate` grow
  an `--engine` flag now that the generator registry names more than one engine
  kind (`synthea`, `sim`)? Registered for visibility 2026-08-15, disposition
  deliberately not taken; it sits here rather than in Deferred because a
  Deferred row owes a revisit trigger and this one has none yet. Resolving it
  updates this row and OPEN-4 together. ADR-0136 finding D7-3(b).
- OPEN **[strip-fresh-hand-case-retirement]** PRIORITY 17 -- the nine live
  per-row `check-entry` cases in `strip_fresh_test.clj` had their `:ok?` half
  subsumed by `exercised_sources_coverage_test` (ADR-0148) and are kept, not
  deleted, this session. Their pinned `:readme-count`s are NOT subsumed and
  carry a real distinct signal, so the retirement is judgement about where the
  pins should live, not a deletion. Next docs-tooling test compaction.
- OPEN **[setup-md-hook-citations]** PRIORITY 18 -- three live surfaces cite
  `SETUP.md` for hook and gitleaks instructions it does not contain: hooks are
  documented in `AGENTS.md` and `AUTHORS-GUIDE.md` SS1 only. `.githooks/pre-push:14`
  ("See SETUP.md for hook installation and gitleaks install instructions"), the
  same file's :39 gitleaks line, and `cli/core.clj:360` ("SETUP.md section 1's
  maintainer-tools row"). Found in passing by ADR-0157; errata, not behavior.

- OPEN **[two-clocks-asset-field-audit]** PRIORITY 19 -- `docs/manual/assets/
  two-clocks.svg`'s banner claims "exactly two timestamp-bearing fields this
  workspace's emitter renders today are MSH-7 ... and EVN-2". ADR-0142 made that
  FALSE: OBR-7 and OBX-14 now render on all three ORU shapes. The drawing itself
  (one ADT^A01, two fields) is still right for ADT; the audit sentence is not.
  Found by its own new tripwire, ADR-0158 (`hand-owned-assets.edn`, :verdict :stale).
- OPEN **[reader-path-fence-battery]** PRIORITY 20 -- R4-Q4 (a) gated the front
  door (README+SETUP) at zero bare fences and DEFERRED the rest of the reader
  path to its own session: the manual's 21 and use-cases' 13, 34 fences measured
  at ADR-0154. Priced real, not cheap: several manual fences need a primed
  artifact cache, which is why D8-5 lapsed twice. Expect the front door's own
  ratio -- some will be exercised, some will need declared exemptions. ADR-0158.
- OPEN **[backtick-shorthand-and-denylist-widening]** PRIORITY 21 -- D1-9
  (backticked-path shorthand) and D1-10 (denylist-family widening), ruled
  fix-session candidates together as R-B2/R-B3 on 2026-08-15 (ADR-0137) and
  carried with NO register home through one arc close and fourteen ADRs. This
  row is the remedy `rulings.md#R-unregistered-request-gets-a-row` names:
  visibility first, disposition later. Rowed by ADR-0158 (review-4 D7-3).
- OPEN **[corpus-player-slices]** PRIORITY 22 -- the corpus-player slices
  chartered by ADR-0014 (bed-board sink, `:mllp`, accumulator wiring) have never
  had a row in any register. UNPRICED and unscheduled: they need their own author
  ruling before a session takes them. Rowed rather than retired because
  `R-unregistered-request-gets-a-row` puts visibility first, and a charter with
  no row is exactly what that rule exists to catch. ADR-0158 (review-4 D7-5).
- OPEN **[oracle-coverage-roots]** PRIORITY 23 -- R4-Q6 (ii) (b): add oracle roots
  reaching the capacity and order->result paths (a churn root, a pathway root), so the
  13-of-21 witnessed-kind set widens. PRICED, NOT TAKEN: each new root is a declared
  oracle change AND a permanent per-session cost on every bracket (today's 35 cost 114s
  a side, measured); ADR-0156's COVERAGE block makes the purchase visible. Moved 3 -> 22
  at the arc close, below live work: proposed 2026-08-19, author-seen. ADR-0159.
- OPEN **[stale-path-retired-namespace-addendum]** PRIORITY 24 -- `ehrt.sim-trajectory.`
  is retired (ADR-0162) and did NOT join `stale_path_test`'s retired-namespace
  denylist, which every prior namespace retirement in that family joined in its own
  commit (S2/S3/M2/M3/M4 addenda). No live surface carries the old form today; this
  buys the gate that stops it coming BACK. Registered, not built: the fence of
  ADR-0162 allowed no new gate, and a new gate owes its own red.
- OPEN **[careplan-guard-resolution]** PRIORITY 25 -- a DECLARED LIMITATION since
  2026-08-21 (ADR-0162), not a queued defect;
  `components/patient-simulator/docs/limitations.md` is the authority and holds the
  evidence. Fix owed when an emitter renders care-plan state: a FHIR CarePlan
  resource, OR a render-time patient-context feature reachable by site-profile Z
  bindings. Priced there too. ADR-0139 C-2's Guard half is absorbed.

- OPEN **[stream-partition-design]** PRIORITY 26 -- traffic-scale arc 1, and the
  Q3(b) CONVERSION: per-patient/per-person RNG streams plus the from==to
  delay-draw skip go from named limitation (deferred, disposition (a)) to
  CALLED FOR, the scale target having met the recorded trigger. Design ADR only,
  no code: draw-site classification, newborn derivation, provenance stream-version
  marker, migration test obligations. Plan: `2026-08-24-traffic-scale-program.md`.
- OPEN **[person-simulator]** PRIORITY 27 -- traffic-scale arc 2. New component,
  sibling charter discipline to `patient-simulator`: bespoke hazard-rate life-arc
  processes (`rulings.md#R-mix-1`), households and pregnancy->delivery
  (`R-mix-2`), identification flows (`R-mix-4`), producing the demographic-delta
  stream the engine folds. Four open questions carried for its charter ADR.
  Blocked on `roadmap.md#stream-partition-design`. ADR-0168 section 4.
- OPEN **[engine-fold-extensions]** PRIORITY 28 -- traffic-scale arc 3. Demographic
  timeline, scheduling state (`rulings.md#R-mix-5`), bed-status cycle (`R-mix-6`),
  new invariant families. SCOPE NARROWED 2026-08-24 (ADR-0169, ruling S1): the
  quadratic removals the 08-24 spike measured are NOT draw-affecting and left for
  `roadmap.md#performance-arc-0`, which lands ahead of arc 1. What remains here is
  the draw-affecting half, which still waits on the stream migration.
- OPEN **[emission-add-ons]** PRIORITY 29 -- traffic-scale arc 4
  (`rulings.md#R-mix-7`): order/result status ladders, DFT P03 charges,
  re-statement chatter under config ratios, fan-out/subscriber table; rides
  `roadmap.md#corpus-player-slices`. Reshuffles NOTHING and needs no stream work,
  so it is the one arc that may proceed independently once arc 3's skeleton
  contract is stable. Gating policy at scale owes a ruling here.
- OPEN **[vendored-module-emission-floor]** PRIORITY 30 -- censused 2026-08-24 by
  the throughput spike: of 31 vendored modules at 20 patients / 3,650 days / seed 7,
  **nineteen emit 1.00 events per patient -- the `:registered` event and no clinical
  content at all**, and the best in the tree yields 3.4. Generalises the single-
  scenario `roadmap.md#ed-tuesday-module-tail-inert` to the whole vendored set, and
  bounds how much traffic realism modules can carry. Measured, not fixed.
- OPEN **[no-eligible-provider-throws]** PRIORITY 31 -- a `:facility` naming a ward
  no `:providers` entry covers reaches `sim-model/choose-attending` with an empty
  eligible vector and dies on a bare `IllegalArgumentException: bound must be
  positive` from `Random.nextInt`, three frames deep, naming neither ward nor
  facility. Every sibling config error is a structured rejection before
  `engine/run` (sim/ADR-0116's own shape). Found by the 08-24 spike, not patched.

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

## Done (at most 30 LINES; the close ceremony rotates oldest whole rows verbatim to `.agents/plans/roadmap-done-<yyyy-mm>.md`, ADR-0161)
- CLOSED 2026-08-25 ADR-0169 **[performance-arc-0]** -- three quadratic families
  removed under EQUIVALENCE PROOF, not red-before-green: byte + value identity on
  four gated corpora, byte identity on the whole **104,851-event** corpus (same
  SHA-256 across two worktrees), oracle IDENTICAL and undeclared. 10^5 cell
  **17.3 min -> 1.81 min (9.58x)**; check alone 711.1 s -> 7.26 s. Suite run twice,
  MAKE_EXIT=0 both, 14m35s / 14m17s, 370/4,166/18,690. Residual sites rowed.
- CLOSED 2026-08-24 ADR-0167 **[suite-time-residual]** -- the 1.32x that survived the
  orphan kill did NOT survive a REBOOT. One clean run on a verified-quiet penny:
  **13m59s**, `MAKE_EXIT=0`, 370/4,142/18,450 reconciling exactly -- inside the
  14m03s-14m48s era and 23s UNDER 2026-08-21's own 14m22s. Class: host process-state
  contention, cured by reboot and recurrable; the health record is the re-probe.
- CLOSED 2026-08-24 ADR-0167 **[suite-time-doubling-diagnosed]** -- diagnosis only,
  no src/test/module change. An orphaned `wslhost.exe` (PID 116424, parent dead)
  spinning SIX threads at 99% of a core each -- one hyperthread on every one of
  penny's six physical cores, 68.7 CPU-hours accrued -- took half the machine.
  CI flat (525-555s) exonerated content; the per-namespace profile was uniform
  1.18-1.44x, not concentrated. Killed: 27m09s -> 19m02s, green, reproducible.
- CLOSED 2026-08-23 ADR-0165 **[generator-side-event-type-coverage]** -- two commits.
  A per-push gate asserting the gated runs collectively produce every ground-truth
  event type their modules can drive, counting only citation-bearing events; its
  FIRST execution found the hole ADR-0163's own drop rule left -- neither
  `:medication-end` nor `:care-plan-end` produced anywhere. ADR-0166 then closed the
  `:care-plan-end` invariant gap seed 5 exposed. 11/11 covered, zero waivers.
- CLOSED 2026-08-23 ADR-0163 **[unpaired-end-step-and-citation-scope]** -- two
  commits, real defect first. A `referenced_by_attribute` naming a submodule the walk
  never entered resolved to nil, compiling an unpaired `:medication-end` (seed 424242,
  `PID-000089-c02fd3a8` @ `:t 5629740`); "no orphaned reference" now extends to "no
  reference ever existed", `:care-plan-end` joining as R3's twin. ADR-0164 then scoped
  both decide-time citation scans by patient, on direct assertion. Both sweeps clean.

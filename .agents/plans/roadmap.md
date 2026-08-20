# Roadmap -- rolling plan and backlog

Updated by sessions in the same commit as the work that changes a row. Row
contract (ADR-0144, gated by `ehrt.docs-tooling.roadmap-lint-test`): the first
token is `OPEN` | `CLOSED <date> <ADR-NNNN|sha>` | `DEFERRED (trigger: ...)` |
`EXTERNAL`, and `CLOSED` lives only under `## Done`; then a stable `**[slug]**`,
cited from elsewhere as `roadmap.md#<slug>` and never by line number; `## Next`
rows carry `PRIORITY n`, ascending, so `head` is what is next; six lines a row.

## Next (backlog, no session scheduled)
- OPEN **[repo-review-5]** PRIORITY 2 -- chartered roughly 15 ADRs past the
  review-4 close by ADR count, not calendar (`rulings.md#R-review-cadence-in-adrs`,
  measured from the prior CLOSE as ADR-0139 worked it): **approximately ADR-0174**.
  Inherits a THIRTEEN-row watch-list carried in `notes/adr/0159-review-4-arc-close.md`,
  not here: W-1 born-red gates, W-2 the `exempt` disposition's missing ratchet, W-3
  row-id citations, W-4..W-13 including two register gates ADR-0159 found narrow.
- OPEN **[register-gate-row-ownership]** PRIORITY 3 -- both register contracts gate
  row SHAPE, not row OWNERSHIP, and both are green over a live defect. `roadmap.md`:
  `c509e46` inserted the ADR-0152 row inside the ADR-0150 row, so five continuation
  lines now sit under the wrong slug (ADR-0159 F-1). `rulings.md`:
  `R-full-suite-before-push` gained its whole wrapper clause at ADR-0155 and names no
  ADR for it, while the arc's other two widenings do (F-2). Found, rowed, not fixed.
- OPEN **[careplan-guard-resolution]** PRIORITY 4 -- a closure's next
  prerequisite is CarePlan, "unowned by any wave until a future session extends
  Guard's own condition-resolution machinery" (ADR-0139 C-2). NOW ALSO OWNS
  census S-2 (folded 2026-08-18, ADR-0150): 7/7 `:care-plan-end` events resolve
  neither field because 4 of 12 vendored `CarePlanEnd` states cite by
  `referenced_by_attribute`, a shape the declared D2 scope never exercised.
- OPEN **[attic-rotation-law]** PRIORITY 5 -- `## Done` holds the current arc
  only by law, and its pointers have not rotated to the attic since the
  conviction arc closed 2026-08-08; deciding a dozen intervening arcs'
  boundaries is judgement work outside a records-only close. ADR-0144 retokened
  those pointers and added six missing ones rather than rotating them, so the
  backlog this row names is larger, not smaller. ADR-0139 finding C-3.
- OPEN **[bed-ready-vacancy-cascade]** PRIORITY 6 -- a bed-ready transfer
  vacates its own ORIGIN bed and nothing looks for a boarder waiting on that
  ward: only `decide :discharge` runs the search. Witnessed at seed 202,
  `t 78060` -- RENAL-04 freed by a bed-ready pull, a Renal boarder still in ED
  surge 420s later. Realism gap, not an invariant violation once ADR-0153
  landed. Class exposed by ADR-0153's diagnosis, rowed rather than fixed there.
- OPEN **[oracle-coverage-extractor-dedup]** PRIORITY 7 -- the two halves of the
  oracle-coverage gate each carry their own copy of the `(def <name>` /
  `(def ^:private <name>` extractor: `ehrt.docs-tooling.oracle-coverage-test`'s
  `def-form` and `ehrt.integration.oracle-coverage-test`'s `committed`. Sharing
  needs `projects/integration` to compose `docs-tooling`, which its deps.edn
  refuses twice (AR-3) -- so the copies stand, cross-cited. ADR-0160.
- OPEN **[manual-dimension-5]** PRIORITY 8 -- manual-review run 2 passed with
  warns, and dimension 5 (running-example continuity) stays WARN as the manual's
  one standing open row: `ed-tuesday` is HL7v2-only and structurally cannot
  supply Chapters 6-8 their FHIR mutation, FHIR-gate calibration, or
  foreign-corpus material. Disclosed, not silently substituted; not a defect
  under the dimension's own reading. ADR-0134.
- OPEN **[audience-register-paring]** PRIORITY 9 -- `docs/dev/AUDIENCES.md`
  pares to five behavioral segments and its own "Seven segments" header is
  corrected in the same edit. Ruled 2026-08-12 (ADR-0113 R4, author "Q1 a");
  execution deferred to a later docs session, not chartered.
- OPEN **[lookup-column-time-next]** PRIORITY 10 -- the lookup-column `time`
  gap in the schema-invalid family, ratified as real (2026-08-06) and still
  untouched; bulk vendoring batched by closure family follows once the catalog
  fully walks. Deliberately distinct from `roadmap.md#lookup-column-time-open`
  below, which the author ruled stays live regardless. ADR-0039, ADR-0066.
- OPEN **[nightly-quickstart-workflow]** PRIORITY 11 -- `make quickstart` gains
  a nightly integration workflow plus the single-sh-fence guard in README
  (`quickstart_fresh`'s own docstring corrected in the same change).
- OPEN **[generator-source-split]** PRIORITY 12 -- the generator-source
  three-concerns split, a named future. ADR-0017.
- OPEN **[corpus-display-placement]** PRIORITY 13 -- `ehrt.corpus.display`'s
  placement is presentation-leaning, a named future. ADR-0018.
- OPEN **[markdown-table-dedup]** PRIORITY 14 -- markdown-table helper dedup, a
  named future. ADR-0018.
- OPEN **[corpus-generate-engine]** PRIORITY 15 -- should `corpus generate` grow
  an `--engine` flag now that the generator registry names more than one engine
  kind (`synthea`, `sim`)? Registered for visibility 2026-08-15, disposition
  deliberately not taken; it sits here rather than in Deferred because a
  Deferred row owes a revisit trigger and this one has none yet. Resolving it
  updates this row and OPEN-4 together. ADR-0136 finding D7-3(b).
- OPEN **[strip-fresh-hand-case-retirement]** PRIORITY 16 -- the nine live
  per-row `check-entry` cases in `strip_fresh_test.clj` had their `:ok?` half
  subsumed by `exercised_sources_coverage_test` (ADR-0148) and are kept, not
  deleted, this session. Their pinned `:readme-count`s are NOT subsumed and
  carry a real distinct signal, so the retirement is judgement about where the
  pins should live, not a deletion. Next docs-tooling test compaction.
- OPEN **[setup-md-hook-citations]** PRIORITY 17 -- three live surfaces cite
  `SETUP.md` for hook and gitleaks instructions it does not contain: hooks are
  documented in `AGENTS.md` and `AUTHORS-GUIDE.md` SS1 only. `.githooks/pre-push:14`
  ("See SETUP.md for hook installation and gitleaks install instructions"), the
  same file's :39 gitleaks line, and `cli/core.clj:360` ("SETUP.md section 1's
  maintainer-tools row"). Found in passing by ADR-0157; errata, not behavior.

- OPEN **[two-clocks-asset-field-audit]** PRIORITY 18 -- `docs/manual/assets/
  two-clocks.svg`'s banner claims "exactly two timestamp-bearing fields this
  workspace's emitter renders today are MSH-7 ... and EVN-2". ADR-0142 made that
  FALSE: OBR-7 and OBX-14 now render on all three ORU shapes. The drawing itself
  (one ADT^A01, two fields) is still right for ADT; the audit sentence is not.
  Found by its own new tripwire, ADR-0158 (`hand-owned-assets.edn`, :verdict :stale).
- OPEN **[reader-path-fence-battery]** PRIORITY 19 -- R4-Q4 (a) gated the front
  door (README+SETUP) at zero bare fences and DEFERRED the rest of the reader
  path to its own session: the manual's 21 and use-cases' 13, 34 fences measured
  at ADR-0154. Priced real, not cheap: several manual fences need a primed
  artifact cache, which is why D8-5 lapsed twice. Expect the front door's own
  ratio -- some will be exercised, some will need declared exemptions. ADR-0158.
- OPEN **[backtick-shorthand-and-denylist-widening]** PRIORITY 20 -- D1-9
  (backticked-path shorthand) and D1-10 (denylist-family widening), ruled
  fix-session candidates together as R-B2/R-B3 on 2026-08-15 (ADR-0137) and
  carried with NO register home through one arc close and fourteen ADRs. This
  row is the remedy `rulings.md#R-unregistered-request-gets-a-row` names:
  visibility first, disposition later. Rowed by ADR-0158 (review-4 D7-3).
- OPEN **[corpus-player-slices]** PRIORITY 21 -- the corpus-player slices
  chartered by ADR-0014 (bed-board sink, `:mllp`, accumulator wiring) have never
  had a row in any register. UNPRICED and unscheduled: they need their own author
  ruling before a session takes them. Rowed rather than retired because
  `R-unregistered-request-gets-a-row` puts visibility first, and a charter with
  no row is exactly what that rule exists to catch. ADR-0158 (review-4 D7-5).
- OPEN **[oracle-coverage-roots]** PRIORITY 22 -- R4-Q6 (ii) (b): add oracle roots
  reaching the capacity and order->result paths (a churn root, a pathway root), so the
  13-of-21 witnessed-kind set widens. PRICED, NOT TAKEN: each new root is a declared
  oracle change AND a permanent per-session cost on every bracket (today's 35 cost 114s
  a side, measured); ADR-0156's COVERAGE block makes the purchase visible. Moved 3 -> 22
  at the arc close, below live work: proposed 2026-08-19, author-seen. ADR-0159.

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

## Done (current arc only; older arcs rotate to `.agents/plans/roadmap-done-2026-08.md`, ADR-0046/ADR-0055)
- CLOSED 2026-08-20 ADR-0160 **[oracle-coverage-gate-integration-half]** -- the
  extractor now matches `(def ^:private <name>` as well as `(def <name>`, the same
  two-prefix `some` ADR-0156 gave the docs-tooling half, and returns nil on a miss so
  the gate fails as a claim rather than as an NPE. First green execution in the gate's
  life: `Integration` run 32402746494 @ `8c53475`, `success`, 8 assertions where the
  red witness 32344505291 reached 6 and errored on the first equality. ADR-0159 F-5.
- CLOSED 2026-08-20 ADR-0159 **[repo-review-4]** -- five ADRs (0154 assessment,
  0155-0158 fixes paired G+A/E+C/B+D/F+H), **38 of 72 register rows moved** --
  every one of the 27 fix-session candidates and all 10 R4-Q rulings, plus D8-1
  carried; **34 residue** (24 close-as-fine confirmed, 9 intake, 1 superseded).
  Ledger delta zero against every per-session tally. The close's own four findings
  and review 5's watch-list are in ADR-0159; the register carries dated appends.
- CLOSED 2026-08-19 ADR-0158 **[edit-root-worktree-residue]** -- PAID BY THE AUTHOR
  2026-08-19 and VERIFIED by that session in the same edit root, not taken on
  report: `core.fileMode` true, `core.ignorecase` unset, ~360 mode-only changes
  restored via `git checkout -- .`. Re-derived there -- 0 tracked `100644` files
  executable on disk, 0 CR bytes in the three named `openai.yaml` mirrors, tree
  clean, `bin/preflight` exit 0 with both OK lines. ADR-0157 register row D3-1.
- CLOSED 2026-08-19 ADR-0158 **[intake-staging-dir]** -- closed per R4-Q9's own
  recommendation ("state a trigger ... or close it -- it has been deferred since
  2026-07-31"), under the author's standing "Q1 accept all recommendations"
  (2026-08-18). Deferred since 2026-07-31 with the ABSENCE of a trigger declared
  in the row itself, so it could never fire. Re-open on the first real staging
  need, with the trigger stated then. ADR-0144 finding F-6, review-4 D7-5.
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

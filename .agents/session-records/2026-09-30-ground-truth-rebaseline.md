# The ground-truth rebaseline: reinstated stays close, churn candidates in patient-id order (ADR-0185)

2026-09-30. Base `c19b1024`. Ceremony: R30, taken from the session
prompt, which stated no prepare-only mode. `bin/preflight` exit 1 with
ONE finding: the tree was not clean -- the allergy session's own
`## Close` section (CI 36800737180 success at `c19b1024`) had been left
uncommitted. It was committed first and on its own, `cda0f2a0`, the
`6641c017` precedent; every other check was OK (last five CI runs green,
HEAD matched origin/main, HEAD not tagged stable-*, disclosed). All
commits were held locally and pushed together with this record, so the
red commit `7c6e2368` reached the remote only alongside its green
successors.

## For downstream

**Ground truth moves on every churn-bearing corpus: reinstated stays
now close after a delay, and churn's candidate choice is in patient-id
order; schema unchanged (no new kind); your retained corpora remain
self-check-valid as pre-rebaseline artifacts; regenerate for the new
bytes.**

Concretely: a legal `:cancel-discharge` is now followed, 4 to 24 hours
later, by a second `:discharge` of the same patient from the bed the
cancel restored. The 28,024 cell writes 264,570,024 bytes, sha-256
`ee59219eb81eb07fb18aa854781ace63e6b210909417762627675e8c96dd29a4`
(671,589 events); the `589600c5...` log you reproduced is the
pre-rebaseline artifact and `figures.edn` keeps it under
`:pre-rebaseline`. Expect far fewer ADT^A08 on the wire: the periodic
chatter a never-closing stay emitted to the end of the run is gone
(52,091 -> 5,520 at dense-7500's 7,500 cell). `reinstated-stay-without-closer`
can still read non-zero -- 4 of 227 at 28,024 -- and every such row is a
stay a churn `:merge` absorbed during the delay (see Findings).

## Author rulings

2026-09-30, design channel, as quoted by the prompt: "Accept
recommendation" -- same discharge, fixed authored delay range, every
reinstated stay. "Prioritize simplicity of design over strict
preservation of determinism" -- an extra draw is acceptable;
byte-identity with prior corpora is NOT owed; determinism itself is
unchanged and gated.

## What landed

| commit | what |
|---|---|
| `cda0f2a0` | the allergy session's uncommitted close note (preflight finding) |
| `7c6e2368` | red tests (a)-(d) |
| `fecbe43b` | the closer, the sort, the declared re-pins, the fleet reorder, regenerated examples |
| `a7de4346` | the full-capability conformance baseline, 212 -> 215 (its own commit, R-pins) |
| `1b2c92e5` | the cancel-index witness seed re-point (its own commit, R-pins) |
| `8912fa47` | ADR-0185, ADR-0180 supersession note, AGENTS.md, roadmap rows 1 and 4 CLOSED, figures + README + Scale table + root README, consuming-ground-truth known-open bullet, this record and the prompt archive |
| `1981e7f1` | the ed-tuesday re-witness: its README, root README, manual 01/04/05, use-case catalog, exerciser literals |
| `3ff9f688` | the straddle-timeline tripwire reviewed fresh (successor commit) |
| (the commit carrying this record) | the integration result and the close |

**Row 4.** `decide :cancel-discharge`'s success arm returns
`:prepend-steps [{:type :delay :from 240 :to 1440} {:type :discharge}]`,
the pair being `churn/reinstated-stay-delay-minutes` with its rationale
docstring. **The 240/1440 values are this session's choice**: the ruling
fixed the shape and no range was named in the tree or the 2026-09-29
handoff ("row 4 first needs mg's ruling on which closer, what delay,
which stream" -- the ruling answered closer and stream). Disclosed in
ADR-0185 decision 2. No new kind, `churn/inject` and `strip` untouched,
no invariant text changed.

**Row 1.** `fold/merge-eligible` and `fold/swap-eligible` sort by
`:patient-id` before `uniform-choice`. ADR-0180's R-hash-order section
carries a dated supersession note; `eligible_index_test`'s colliding-pair
test flipped to order-independence (now
`at-a-hasheq-collision-the-views-are-order-independent`), the carrier
test became `the-carriers-iteration-order-no-longer-reaches-the-view`,
and `permuting-insertion-order-leaves-both-views-equal` is new; AGENTS.md
says "no hash-order dependence" again and the row pointer is gone.

**Census, every collection-resolving draw** (`grep` for `uniform-choice`,
`rand-nth`, and `(nth ... (.nextInt rng (count ...)))` over every src
tree): `decide.clj:1457` (bed-swap) and `:1522` (merge) -- the two views,
now sorted; `decide.clj:1627` -- `:outpatient-visit` over `:providers`,
a config vector; `sim-model/facility.clj:151` `choose` -- callers pass
`free` over `licensed-bed-ids`/`surge-slot-ids` (ward-config order,
`remove`/`filter` preserve it) and a `filterv` over the providers vector;
`person-simulator/process.clj:95` `pick` -- three authored enum vectors
and the household roster, built in time order; `corpus/mutate.clj:166`
-- log-index sites, outside the engine. None reads a hash map's order.

## Red, captured before any src edit

The tests were written and run against the unmodified src, so the red
run exercised exactly the unfixed code (no stash needed: src was
untouched at the time).

- (c) `ehrt.sim-engine.eligible-index-test`: `Ran 10 tests containing 56
  assertions. 8 failures, 0 errors.` -- the collision test (`[early
  late]` expected, views answered `[late early]`), the carrier test x2
  (views answered hash order), the permutation test x2, the corpus-shape
  test's "every world agrees" x2 and the property, each because the
  definition is now the scan SORTED. No false positives.
- (a) `a-reinstated-stay-is-discharged-again-after-a-delay`: 4 failures
  and 1 error -- `(= 2 (count discharges))` actual 1; the NPE on the
  missing second discharge's `:t`; `:status` `:admitted` not
  `:discharged`; the bed `RENAL-09` still held. `check-all` was GREEN,
  which is the row's point: the catalog permits the stay.
- (b) `no-reinstated-stay-outlives-its-corpus`: `(not (= 0 1))` at
  seed-202-ed-tuesday, `(not (= 0 6))` at the dense 750 cell.
- (d) `the-dense-750-cell-is-byte-identical-across-two-runs`: green
  before, green after.

All four green after `fecbe43b` (`Ran 10 tests ... 0 failures`; 7, 4
and 2 passes for the three run-level vars).

## Step 4: predicted, then measured

Prediction (from base corpora, before either script ran -- replay each
base root to the first legal `:cancel-discharge` or churn swap/merge
whose drawn position holds a different id once sorted; script and
output in the session scratch): **DIFFERS = {bed-cycle,
encounter-horizon, scheduling}**, the three `--churn` roots; every
other root IDENTICAL. `chatter-charges` (2 merges) and `demographic-fold`
(2) carry identification merges only.

`bin/ground-truth-bracket c19b1024 fecbe43b --declared-digest-change`:
exit 1, `--- coverage: 40 roots carry :ground-truth and are digested; 3
skipped (no such key): appendicitis.edn, ear-infections.edn,
sore-throat.edn ---`, DIFFERS on exactly `bed-cycle.edn`,
`encounter-horizon.edn`, `scheduling.edn`. IDENTICAL (37): allergic-rhinitis,
allergy, anemia, asthma, attention-deficit-disorder, bronchitis,
chatter-charges, colorectal, death-fixture, dementia, demographic-fold,
dermatitis, ear-infections-engine, ear-infections-history-engine,
fibromyalgia, hypothyroidism, immunization, injuries, med-rec,
metabolic-syndrome-care, order-pathway, osteoarthritis, osteoporosis,
rheumatoid-arthritis, sepsis, sinusitis, sleep-apnea,
total-joint-replacement-engine, urinary-tract-infections-engine,
urinary-tract-infections-history-engine, veteran-lung-cancer,
veteran-prostate-cancer, veteran-ptsd, veteran-self-harm,
veteran-substance-abuse-treatment, vhd-pulmonic, vhd-tricuspid.

`bin/regression-oracle c19b1024 fecbe43b --declared-digest-change`: exit
1, DIFFERS on the same three (`bed-cycle` cc0ad7bd -> f22d4f13,
`encounter-horizon` 895985b2 -> f94e8d55, `scheduling` 3fe44c5b ->
451c7183); the other 40 IDENTICAL -- the 37 above plus the three batch
roots. **Both lists equal the prediction.** Both scripts print "STOP,
escalate" on any DIFFERS; that banner is written for a sweep whose
ground truth must not move, and this one is declared.

## Step 5: figures

Re-measured 2026-09-30 at `fecbe43b` (src identical to the working
tree), the provenance method: one warm-up and two timed runs per cell,
`/usr/bin/time -v`, JVM defaults; Windows LoadPercentage sampled before
every run (values 0-14, one 26). **The 7,500 cell's first t2 started at
26 and was discarded** (wall 2:03.77, kept as `-CONTAMINATED`) and
re-run at 0 (2:07.97). Every cell's timed runs wrote byte-identical
`events.edn`.

| cell | t1 / t2 wall | mean | RSS MiB t1 / t2 | events | messages | events sha-256 |
|---|---|---|---|---|---|---|
| config 7,500 | 124.23 / 127.97 | 126.10 | 1845 / 1859 | 167,715 | 176,971 | `7849b0a1...` |
| nobed 7,500 | 107.58 / 104.50 | 106.04 | 1704 / 1658 | 125,700 | 134,886 | `30961636...` |
| bare 7,500 | 52.85 / 50.41 | 51.63 | 1118 / 1030 | 101,120 | 65,643 | `39102e11...` |
| config 750 | 58.31 / 57.17 | 57.74 | 1048 / 1078 | 33,296 | 35,926 | `6912f3fd...` |
| 28,024 (one run, -Xmx8g) | 386.61 | -- | 3769 | 671,589 (32,080 subjects) | -- | `ee59219e...` (264,570,024 bytes) |

`bin/demo-exerciser-dense-7500` then reproduced 167,715 / 176,971 /
33,296 independently and rewrote `:asserted` itself; its only failure
was the tree-clean postcondition, expected mid-session. **The walls rose
14-22% in every cell, the bare one included** (whose counts barely
moved), so the rise is recorded and NOT attributed to the rebaseline.
`describe` over the new 750 cell: `reinstated-stay-without-closer` 0.

**The message drop is A08 chatter.** Per-trigger at the 7,500 cell,
base worktree (`c19b1024`, untimed) against new: ADT^A08 52,091 ->
5,520; every other trigger within a few percent (A20 41,399 -> 41,722
top-level). That is why msg/event no longer climbs 750 -> 7,500, and the
README's "bed cycle is message-richer" and "still climbing" paragraphs
were rewritten rather than re-numbered. The module-only cohort row is
unmoved (937 / 877 at 7,500; 937 / 892 at 28,024, read over a `sim run`
envelope); `veteran_substance_abuse_treatment` moved 174/88 -> 178/90 at
7,500, and consuming-ground-truth's example now carries 178/90.

## Findings (one line each)

- `reinstated-stay-without-closer` residuals -- 1 at config 7,500, 2 at
  bare, 4 of 227 at 28,024 -- are all stays a churn `:merge` absorbed
  (`:role :merged`) during the closer's delay; `:merged` is terminal and
  the measure counts only a `:discharge` as closer. Not a defect; a
  describe change if anyone wants it.
- The event fleet's `churn-run` authored `:bed-swap`/`:merge` AFTER its
  `:cancel-discharge`; with the closer prepended they swapped a
  discharged patient (`check-all` :rejected: schema, slot,
  both-admitted, non-admitted-holds-no-bed). The two steps now precede
  the discharge; formats.md's `:cancel-discharge` example regenerated
  (index 11 -> 13, bed RENAL-01 -> RENAL-H01). An authored pathway that
  continues a stay after a cancel-discharge now runs after the closer.
- A `make test` JVM from another Claude session (parent bash started
  21:37:50, before this session) was alive and idle (0% CPU, ~1.5 GB
  RSS) through this session's measurements; not this session's, left
  alone, disclosed. It shares `out/test-tmp`, which every `make test`
  here wiped.
- A knife-edge witness went vacuous, as the arc-1 lesson predicts:
  `cancel-index-test/the-corpus-actually-reaches-every-cancel-shape`'s
  pinned seed 20260906 left its bed-cycle half with 4 cancels and no
  patient holding two events of one class (`repeats` 0). Swept seeds
  20260907..20260946 with every assertion of that test; 20260907 is the
  first reaching all seven shapes in both settings (repeats 12 and 2).
  Re-pointed with the reason in a comment. Every other pinned witness in
  sim-engine and sim-check passed (21 namespaces run past failures).
- NOT PREDICTED: `projects/conformance`'s
  `sim-v2-full-capability-baseline.edn` (a 60-patient `--churn` corpus,
  judge.v2 report) moved 212 -> 215 messages -- 3 files added, 0
  changed verdicts, no codes appeared or disappeared, all 215 pass.
  Step 4's prediction covered oracle roots and gated corpora, not this
  gate. Regenerated by pprint under a dated header, its own commit per
  R-pins.
- NOT PREDICTED, the largest: `make integration`'s ed-tuesday exerciser
  failed `expected 620 ':verified true' entries, got 491`. ed-tuesday
  runs `--churn`, so its corpus moved with the others, and every
  document quoting it followed (`1981e7f1`). Method: the README's own
  commands at base (`c19b1024` worktree) and at the new tree, every
  cited figure computed on both, changed only where the base reproduced
  the README's current value. First difference event 123 (08:00, a
  bed-swap peer), so the 01:12 snapshot, the straddle, the first three
  batches, the wrapper transcript and the interior gap are unchanged.
  Moved: 1,269 -> 1,267 events, 1,554 -> 1,426 messages, 579 -> 450
  snapshots, 620 -> 491 batches, A08 135 -> 4 (134 were MRN000040's one
  reinstated stay, re-stated for twenty years; it now closes 17.3 h
  after the cancel and her originally booked follow-up happens). The
  latency walkthrough recast from MRN000095 to MRN000005 (6 of 111
  out-of-order admissions, was 5). Root README, manual chapters 01, 04
  and 05, the use-case catalog and the exerciser's literals follow; the
  straddle SVG's tripwire fired and was reviewed fresh, value by value
  (`3ff9f688`, its own successor commit).
- Corrected while re-witnessing, measured: ed-tuesday's README and the
  root README called the run's one merge an IDENTIFICATION merge; its
  event carries no `:cause` at base or after -- a churn merge. The
  README's "4 to 41 messages each" was already 8 to 44 at base (now 8 to
  42).
- Left stale, outside this change: manual chapter 03 says occupied beds
  "climbing from 4" and "peak-21-inpatients"; ed-tuesday's README has
  said 3 and 12 since 2026-08-29, and neither figure moved here.
- Test (b) lives in `ehrt.sim.run-test`, not a sim-check namespace: both
  corpora are generated there already, and sim-check's classpath has no
  config-file merging or module resolution.

## Suite

Full `make test`, unpiped, to a log, the wrapper ending `exit
"$MAKE_EXIT"`. Eight runs; the first seven each stopped at the first
failing namespace, and each failure was a pin this change owed:

1. `ehrt.sim.run-test` -- seed-202's arc-0 digest/baseline, the
   re-pin-exactly-once tripwire that reads it, and the two dense-cell
   floors (1,019 -> 1,018).
2. `state-derived-md-matches-a-fresh-render-test` -- the new ADR file
   (`make docsgen`).
3. `no-visible-adr-token-in-prose-test` -- a bare `ADR-0185` in
   consuming-ground-truth.md; now a footnote.
4. `done-rows-are-pointers-not-ledgers-test` -- a 531-char Done row;
   compacted to 385.
5. `state-derived` again -- the roadmap's line count.
6. `cancel-index-test` -- the vacuous witness (Findings).
7. `sim-full-capability-gate-test` -- the conformance baseline
   (Findings).

Run 8, at `8912fa47`'s tree: **`MAKE_EXIT=0`** -- 432
namespace result lines, 5,109 tests, 29,477 assertions, 0 failures, 0
errors (summed from the log's own `Ran` lines; this record carries
the figure). Run 9, at the tree this record ships with (the ed-tuesday
re-witness, the tripwire review and these record/ADR additions):
**`MAKE_EXIT=0`**, the same 432 namespaces, 5,109 tests, 29,477
assertions, 0 failures, 0 errors.

## Integration

`make integration` at `3ff9f688`, clean tree, wrapper ending `exit
"$MAKE_EXIT"`: **`MAKE_EXIT=0`** -- the integration project's poly
tier, all three demo exercisers ("tree clean" each; dense-7500's
`figures.edn` rewrite left the tree unchanged, so every published count
holds), the six use-case scripts and `readme-what-you-get`. The first
run, at `8912fa47`, failed on the ed-tuesday exerciser (Findings);
every other step passed when run individually.


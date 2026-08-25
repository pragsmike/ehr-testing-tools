# 2026-08-25 -- arc 0: quadratic removals under equivalence proof

Build session (traffic-scale program, ADR-0169), penny / WSL2 / JDK 21.
HEAD at start `d49f1c6`, tree clean, `bin/preflight` exit 0. Author
rulings **S1** and **S2** (design channel, 2026-08-24) commission this arc
one session, ahead of arc 1. Five commits, local only -- no push, no tag.

**Result, one sentence:** the three site families the 2026-08-24
throughput spike named are removed under a co-landed equivalence proof --
byte-identical corpora and identical findings at every gated seed, and
byte-identical at **104,851 events** by a two-worktree digest bracket --
which takes the 10^5 cell from **17.3 min to 1.81 min (9.58x)**, check
alone from **711.1 s to 7.26 s (97.9x)**, with the regression oracle
IDENTICAL and no `--declared-digest-change` claimed.

## Step 0 -- environment and health record

    $ git log --oneline -1     -> d49f1c6 (the prompt's own base)
    $ git status --porcelain   -> empty
    $ bin/preflight            -> exit 0, no findings; CI last five green
    $ uptime -s                -> 2026-08-24 15:10:46 (the post-reboot VM
                                  ADR-0167's residual probe measured 13m59s on)
    $ nproc                    -> 12       $ free -h -> 15Gi, 0B swap
    $ df -hT .                 -> /dev/sdd ext4 251G, 26% used (NOT /mnt/c)
    $ java -version            -> openjdk 21.0.7 (Ubuntu)
    MaxHeapSize                -> 4,162,846,720 = 3.88 GB (ergonomic; `bin/ehrt`
                                  sets no JVM options -- the shipped ceiling)

Host samples, Windows side, taken at each point a figure was recorded
(F7). `wslhost.exe` thread counts were 1/1/4/1/4 at every sample, all
processes started 15:10:49 / 15:13:06 with live parents -- **no orphan**,
i.e. not the ADR-0167 condition.

| when | Windows `LoadPercentage` | what was measured under it |
| --- | --- | --- |
| 22:36 session start | 23 / 16 / 29 | nothing timed |
| 23:35 pre-suite | **1 / 4 / 4** | the full `make test` below |
| 00:01 pre-cell-C | 21 / 30 / 25 | **cell C** -- see the disclosure below |

**Disclosed, and it matters (F7).** Cell C ran at host load 21-30%, not
the 4/3/3 the spike's own timed cells enjoyed; the load is this session's
own Windows-side agent processes, the same condition the spike disclosed
for its attribution run (29/20/14). The bias runs **against** the claim,
not for it: the post-arc-0 figures below are taken under worse conditions
than the baseline they are compared to, so a real speedup can only be
understated. Power scheme High performance, on AC, 2592/2592 MHz at every
sample.

## What was ruled, and the channel error it corrects

**S1, verbatim: "a"** -- output-identical refactors (byte-identical corpus
+ identical findings at fixed seeds) are EXEMPT from the reshuffle-era
constraint; only draw-affecting changes wait for the stream migration.

**S2, verbatim: "a"** -- commission arc 0 (performance) ahead of arc 1,
one session: (i) six occupancy/churn invariants -> fold-carried
incremental state; (ii) replay-per-cancel -> read the one element in hand;
(iii) fold-carried order indexes retiring the ADR-0164 decide scans. All
gated by byte-identity + findings-identity tests co-landed.

S1 corrects `.agents/plans/2026-08-24-traffic-scale-program.md` line 43 at
`d49f1c6`, which read "a generator change, so it lands within this era,
never before arc 1" -- conflating *generator change* with *draw-affecting
change*. The plan keeps that sentence quoted rather than silently
rewritten. Full reasoning: ADR-0169.

## The equivalence proof, per site family

Nothing here is red-before-green: a pure refactor has no new behaviour to
witness failing. Every gate below landed in its own commit, BORN GREEN on
the unrefactored tree, and only then was the code it gates changed.

### The corpus gates (commit 2, `41e5c1a`)

| gate | what it pins |
| --- | --- |
| byte identity | sha256 of `(pr-str ground-truth)` -- verbatim what BOTH shipped writers emit (`ehrt.cli.core/sim-ground-truth-bare-text`, `ehrt.corpus.generators/spool-sim-output!`), never a `pr-str` invented for a test |
| value identity | `=` against the whole committed baseline, failing with the **first differing event index** and both events |
| F3 tripwire | an assertion that the two gates AGREE; a disagreement is STOP-AND-REPORT, not a re-pin |

Four corpora committed whole (~812 KB, disclosed -- the weight class of
`demos/traces/module-mix/ground-truth.edn` at 318 KB and the four synthea
census EDNs at 300-314 KB each, all already tracked). Round-trip verified
when pinned: value AND bytes both survive `pr-str` -> `edn/read-string` ->
`pr-str` for all four, so the baseline is faithful for both gates.

### Family (i) -- six check-side invariants (commit 3, `40223bd`)

Rewritten to carry incremental state through the fold. **The design rule
that makes order-identity a theorem rather than a hope:** three of the
four occupancy findings name a `:bed` or `:patient-id` whose ORDER came
from the iteration order of a Clojure hash map. A carried index with the
same keys need not iterate them the same way (array-map holds insertion
order below 8 entries, hash-map does not hold it at all). So those three
use the index **only as a guard** -- a boolean, which has no order -- and
emit from the ORIGINAL EXPRESSION over `world-after` when it fires.
`occupancy-within-capacity` is the exception and emits from its index
directly: its loop order comes from the `:wards` vector and its payload is
scalar.

Cost stated honestly: on a CLEAN log the O(P) walk is never paid. On a log
with a persistent violator the guard fires every event and the walk
returns -- a run that has already failed its self-check. The fast path is
the passing path, deliberately.

Proof co-landed: the six ORIGINAL bodies kept verbatim as `naive-*`
reference oracles, plus

* `fast-invariants-equal-their-naive-reference-implementations` -- defspec,
  120 trials, **pinned seed 20260825**, asserting `(= (naive-x log)
  (fast-x log))` as SEQUENCE equality (so order, not just content) on both
  a clean generated run and a mutated one, for all six plus capacity under
  a tightened facility view.
* `the-mutations-actually-make-all-six-invariants-fire` -- the mechanism
  check, because comparing two empty seqs proves nothing. It required
  three corrections found by measuring rather than assuming: the shared-bed
  pool indexed by `i` sent 100% of collisions to one bed (`quot i n`
  fixes it); relabelling an admission to `:outpatient-visit` left the
  patient with no location, so the visit is now INSERTED after it; and
  `occupancy-within-capacity` cannot be made to fire by mutating a log at
  all -- the engine never over-fills a ward and the mutations move
  patients between beds, not wards -- so it fires the way its own
  discrimination test does, against a declared capacity the log exceeds.
* `fast-invariants-equal-their-naive-references-on-every-small-fixture` --
  ten hand-written logs, including a two-merge interleaved-zombie fixture
  that pins emission as `["P2" "P2" "P4" "P4"]`, merge-major, which is NOT
  the log order `P2,P4,P2,P4`. A single forward pass would be caught.
* The six discrimination tests, tightened from `(is (seq ...))` to the
  FULL finding map, before the refactor.

Measured, naive vs fast, 6,323 events, self-check clean, findings EQUAL:

| invariant | naive | fast | speedup |
| --- | --- | --- | --- |
| `occupancy-within-capacity` | 4,414.1 ms | 27.9 ms | **158.5x** |
| `no-double-occupancy` | 1,673.0 ms | 72.7 ms | 23.0x |
| `admitted-occupies-one-slot` | 1,205.7 ms | 30.6 ms | 39.5x |
| `outpatient-patients-occupy-no-bed` | 1,104.7 ms | 22.9 ms | 48.3x |
| `cancel-references-existing-uncancelled-event` | 110.6 ms | 4.9 ms | 22.6x |
| `no-events-after-merged-terminal` | 54.7 ms | 14.0 ms | 3.9x |

### Family (ii) -- replay-per-cancel (commit 4, `878b638`)

`run` now records, under each `:transfer`/`:discharge` event's own log
index, the state its subject was in immediately before it -- the same
value `replay` computes, produced by the same `evolve` fold the run loop
was already running, in the same pass. The decide reads a map.

Two premises were CHECKED, not assumed, because the arc's scope named them
as candidates: `:cancel-admit` reads no prior state at all, and
`:transfer-in-error` decides its own cancel atomically off the live
pre-transfer patient. Neither ever replayed; neither is carried.

**Fallback on the KEY, never on a missing entry.** A world with no
`:reinstate-index` key -- hand-built, as most of engine-test's direct
`decide` calls are -- keeps the replay path unchanged. A world that `run`
built with an entry nevertheless missing reads nil, changes the emitted
event, and fails the byte gate. Silently replaying instead would hide it.

Proof co-landed, post hoc against the `replay` the decide no longer calls
-- never as an assertion inside the decide, which would reinstate the very
cost removed and make the claim unfalsifiable in the configuration that
matters:

* `ehrt.sim.run-test/cancel-decides-reinstate-exactly-what-replay-would-hand-back`
  over all four gated corpora.
* `ehrt.sim-engine.engine-test/cancel-reinstatement-survives-the-fold-carried-index`
  -- defspec, 150 trials, pinned seed, over churn-driven runs -- plus its
  own vacuity check and a carrier-coverage test.

### Family (iii) -- the two ADR-0164 scans (commit 5, `b9d5178`)

`{[opening-type patient-id citation] -> last-index}` carried in world,
written as events are appended, so a later occurrence overwrites an
earlier one -- which is precisely what the `last` in the scan meant. Same
key-presence fallback rule as family (ii).

One detail that had to be right: the post-hoc recomputation scans the
**prefix** `(subvec log 0 idx)`, not the whole log, because that is what
the decide saw -- `decide` reads `(:ground-truth world)`, a mirror of the
log SO FAR. A whole-log recomputation would be a different claim, and a
wrong one.

Proof co-landed: `citation-resolution-matches-the-whole-log-scan` on every
gated corpus including seed 424242, and
`citation-index-resolves-exactly-what-the-scan-resolved` (defspec, 120
trials, pinned seed) over a pathway that opens each citation TWICE per
patient -- the case that tells `last` from `first` -- with explicit
assertions that each resolution names an event that patient participates
in (ADR-0164's own case) and equals `(last own-orders)`.

**ADR-0163's compile-time drop and the citation shape were not touched.**

### The 10^5 byte-identity bracket (beyond the prompt -- deviation 7)

The committed gated corpora are 343-407 events, and `adhd-seed-2` is 12.
The arc exists to serve 10^5. So the same dense scenario the throughput
spike measured was generated in TWO git worktrees -- `d49f1c6` and this
arc's HEAD -- and each side's ground-truth digested as the shipped writer
serialises it:

    d49f1c6   EVENTS 104851  BYTES 51680494  SHA256 8a18597d6c575ffd...86dca
    HEAD      EVENTS 104851  BYTES 51680494  SHA256 8a18597d6c575ffd...86dca

**Identical: 51,680,494 bytes, same SHA-256, 104,851 events.** Not a
sample, not a count, not a spot check -- the whole 10^5 corpus, byte for
byte, across all three site families at once. This is the single strongest
piece of evidence the arc produced, and it is the one the gated fixtures
could not give.

## Step 6 -- the two brackets

### `bin/regression-oracle d49f1c6 HEAD`

**IDENTICAL: every root's digest matches**, `--declared-digest-change:
no`, soundness check passed outside the leading docstring. Run twice: once
at step 2 on the docs-and-tests-only delta (a soundness check of this
session's own baseline, per `rulings.md#R-oracle-script-contract`) and once
over the whole arc. F2 satisfied; no declared change was needed or claimed.

**The oracle is NECESSARY, NOT SUFFICIENT here, and was never leaned on.**
`components/oracle/src/ehrt/oracle/digest.clj`'s own vacuous-set note
records that the 35 golden roots reach **none** of the cancel family,
**not** `engine/replay`, and **not `sim-check` in its entirety** -- which
is all three of this arc's three site families. An IDENTICAL verdict from
the oracle says nothing at all about the work done here. The corpus and
findings gates are what carry the claim.

### Full `make test`, unpiped, wrapper ending in `exit "$MAKE_EXIT"`

Host verified quiet first (Windows `LoadPercentage` **1 / 4 / 4**, no
orphan `wslhost`), per `rulings.md#R-full-suite-before-push`.

    MAKE_EXIT=0     WALL_SECONDS=875  =  14m35s
    370 namespaces / 4,166 tests / 18,690 assertions, 0 failures, 0 errors

| | namespaces | tests | assertions | wall |
| --- | --- | --- | --- | --- |
| ADR-0167 baseline (post-reboot, quiet) | 370 | 4,142 | 18,450 | **13m59s** |
| this arc | 370 | **4,166** | **18,690** | **14m35s** |

**+24 tests, +240 assertions, +36s.** The delta is the equivalence gates'
own cost, not a regression: two new defspecs at 120 and 150 trials, the
naive-vs-fast comparison running every invariant TWICE per trial, and four
gated corpora now digested and value-compared as well as self-checked. The
suite got slower because the proof was added to it; the code it proves got
faster.

## Step 7 -- cell C rerun at 10^5, MEASURED

The spike's scratch **survived on penny** in full (`dense-7500.edn`,
`driver.clj`, `cell.sh`, `run.sh`), so the prompt's re-authoring budget
was not spent. The driver was copied into this session's own scratch
rather than run in place, so the spike's recorded results are untouched.
Same config, same seed 20260824, same 7,500 patients, same driver, same
machine: warm-up plus two timed, `/usr/bin/time -v` on each.

**The corpus did not move.** Both timed runs produced **104,851 events** --
the spike's own figure to the event -- with `self-check-ok true`,
`exhausted false`, and `state-history-entries 108,319`.

### Walls (mean of two timed)

| phase | spike, `d49f1c6` | this arc | speedup |
| --- | --- | --- | --- |
| generate | 324.1 s | **101.2 s** | **3.20x** |
| check | 711.1 s | **7.26 s** | **97.9x** |
| **total** | **17.3 min** | **1.81 min** | **9.58x** |

| phase | throughput before | throughput after |
| --- | --- | --- |
| generate | 324 ev/s | **1,036 ev/s** |
| check | 147 ev/s | **14,442 ev/s** |

Per-run: generate 102.137 / 100.223 s, check 7.111 / 7.408 s; process wall
1:55.83 / 1:54.10 (warm-up 1:57.26).

### The profile's own arithmetic, checked against the outcome

This is the strongest evidence that the spike attributed the cost
correctly and that this arc removed exactly what it named -- neither
figure was available to the other.

* **Generate.** 324.1 s with `replay`-per-cancel at 35.3% (114.4 s), the
  `:medication-end` scan at 21.3% (69.0 s) and its `:care-plan-end` twin
  at 10.9% (35.3 s) predicts a residual of **105.4 s**. Measured:
  **101.2 s** -- 4% apart, and on the fast side despite the elevated host.
* **Check.** 711.1 s with the six invariants at 99.4% (706.8 s) predicts
  a residual of **4.3 s**. Measured **7.26 s**, and the ~3 s gap is
  accounted for: each of the six still makes its own `engine/replay`
  call, which the arc did NOT consolidate (F4 -- the 14 replays are out of
  scope), at roughly half a second apiece.

### Memory -- unchanged where it should be, halved where it should be

| quantity | spike, `d49f1c6` | this arc |
| --- | --- | --- |
| retained after generate (two settling GCs) | 109.0 MB | **109.3 MB** |
| peak heap, generate phase | 845-941 MB | **425-439 MB** |
| peak heap, check phase | 679-1,388 MB | 689-1,384 MB |
| peak process RSS | 1.29-2.18 GB | 1.27-2.19 GB |

Retained memory is identical to 0.3% -- a third independent corroboration
that the corpus did not move. Generate's peak heap **halves**, because the
`replay` call removed from the cancel decides was materialising a vector
of N maps per cancel. Check's peak heap is unchanged, exactly as it should
be: its allocation is dominated by the 14 independent `replay` vectors
this arc deliberately did not touch.

### F3-1 (finding, premise correction): there is no 10^5 PROJECTED figure

Step 7 asks to "convert the plan's 10^5 PROJECTED figure to MEASURED".
There is no such figure. Every 10^5 entry in
`.agents/plans/2026-08-24-traffic-scale-program.md`'s appendix is already
labeled MEASURED; **both** PROJECTED entries are 10^6 ("10^6 events on
today's generator", "10^6 with the quadratics removed"). No label was
therefore flipped. What cell C produces instead is a NEW measured row --
the post-arc-0 10^5 figure, which did not previously exist in any form --
appended to the appendix as MEASURED, alongside a note on what it says
about the 10^6 projection. Per F3, nothing interpolated was promoted.

## Findings (rowed, not taken -- `rulings.md#R-move-not-improve`)

**F-1: the gated corpora exercise the cancel family one run deep.** Of the
four, only `seed-202-ed-tuesday` carries a reinstating cancel at all --
9 `:cancel-transfer` + 1 `:cancel-discharge` = **ten events**; both
clinic-decade runs and `adhd-seed-2` carry none, and seed-202's own 2
`:cancel-admit` reinstate nothing. The gated-corpus half of family (ii)'s
gate therefore rests on ten events in one run, which is why a 150-trial
population-scale defspec had to join it. Same one-root-deep fragility
class as `roadmap.md#generator-coverage-depth`. Disclosed inside the test
itself, which asserts the count so a silent drift to zero goes red.

**F-2: the gated corpora do not exercise ADR-0164's positive path at all.**
Exactly two cited end events exist across all four corpora, both in
`adhd-seed-2`, and **both resolve to `nil`** -- their opening
`:medication-order` and `:care-plan-start` fall in the history phase and
never enter the log, which is the designed pre-horizon straddle ADR-0165
chose that run for. The two clinic-decade runs carry 20 and 21
`:medication-order` events and no cited end whatsoever. So no gated corpus
can witness the citation index FINDING a resolution -- only that it does
not invent one. Again covered by a co-landed population-scale defspec, and
again asserted in the test so the population cannot drift unnoticed.

**F-3: `last-uncancelled-index` cannot ride either carrier.** Step 4 makes
it conditional -- it may ride "IF the carrier answers its query without a
second code path". It does not. Its query is "the most recent
`event-type` event naming `patient-id` that is NOT already the target of
an earlier `cancel-type` event", which needs a per-patient/per-type
POSITION index plus a cancelled-target set -- two structures neither the
reinstate index (log-index -> state) nor the citation index
(citation-key -> index) holds. Left untouched per the prompt's own
conditional and R-move-not-improve. It was 5.9% of the OLD generate
phase; as a fraction of the new one it is roughly **19%**, so it is now
a materially bigger share than it was.

**F-4: the residual performance sites need a home.** ADR-0169 narrowed
`roadmap.md#engine-fold-extensions` to arc 3's draw-affecting half, which
leaves the sites this arc deliberately excluded without a row. A new OPEN
row `roadmap.md#performance-residual-sites` carries them: the 14
independent `engine/replay` calls in `check.clj` (now ~40% of a 7.26 s
check phase, and the reason its predicted 4.3 s came out at 7.26 s),
`sim-model/occupancy-board` folding every patient ever created,
`decide :discharge`'s waiting-boarder `filter` + `sort-by`, and
`last-uncancelled-index` per F-3.

**F-5: the two phases have swapped places.** Before this arc, check was
69% of the 10^5 cell and the larger of the two quadratics. After it, check
is **6.7%** (7.26 s of 108.4 s) and generate is 93.3%. Any further
performance work at this scale is generator-side; the check half is done
for now. **Not measured:** the new site ranking WITHIN generate. The
percentages quoted in F-3 are the spike's own attribution rescaled
arithmetically, not a fresh profile, and are labeled as such.

## Deviations

1. **Step 7's premise is wrong about the tree** -- there is no 10^5
   PROJECTED figure to convert. Reported as F3-1 above; a new MEASURED row
   is added instead of a label being flipped. (F5.)
2. **Cell C ran at host load 21-30%**, not the 4/3/3 the spike's timed
   cells had, because this session's own Windows-side processes were
   active. Disclosed at the health record with the direction of the bias
   (against the claim). Not corrected for.
3. **All four gated corpora committed whole** (~812 KB), where step 2(a)
   said "decide, disclose". Decided yes for all four: the first-differing-
   index diagnostic the step requires cannot be computed without the
   baseline values, and the F3 byte-vs-value tripwire needs both gates on
   the same corpus to be able to disagree at all.
4. **Three of the four occupancy invariants do NOT emit from their carried
   index**, though step 3 says "emitting the SAME finding maps". They emit
   from the original expression when the carried guard fires. This is not
   a weakening -- it is what makes order-identity a theorem rather than a
   hope, since their order came from hash-map iteration. Reasoned in full
   in `check.clj`'s own ADR-0169 comment.
5. **The `=`-of-`check-all` gate lives in `run_test`, not `check_test`.**
   Step 3 puts it with the defspec, but Polylith builds a per-brick
   classpath and `components/sim/test` -- where the corpora are -- is not
   on `sim-check`'s. The gate is with the data.
6. **Both generate-side gates gained population-scale companions in
   engine-test** beyond what steps 4 and 5 specify. Forced by F-1 and F-2:
   the gated-corpus versions the steps name are real but too thin to carry
   the claim alone. An addition, not a substitution -- both gated-corpus
   gates were written and are green.
7. **A two-worktree digest bracket at 10^5 was added**, beyond the prompt.
   It extends byte-identity from the 343-407-event gated corpora to
   **104,851 events**, which is the scale the arc exists to serve.
8. `no-events-after-merged-terminal` **already had** a firing test
   (`check_test.clj` `-detects-a-zombie-event`), so step 2(b)'s "if it has
   no firing test, write one now" did not apply. It was tightened to the
   full finding map like the other five.
9. **Step 5 names the care-plan twin as `:order-event-id`'s counterpart
   without naming it**; it is `:start-event-id`. Stated here so the record
   matches the tree.
10. **The mechanism check for the mutation battery needed three
    corrections** found by running it rather than reasoning about it (the
    shared-bed index collapse, the outpatient relabel losing the location,
    and capacity being unreachable by log mutation at all). Detailed under
    family (i).

## Commits

| sha | what |
| --- | --- |
| `301425b` | ADR-0169; plan line 43 amended (original quoted, not rewritten); roadmap `[performance-arc-0]` PRIORITY 1 and `[engine-fold-extensions]` narrowed; `rulings.md#R-output-identical-exempt-from-reshuffle-era` |
| `41e5c1a` | equivalence gates BORN GREEN: four corpora pinned by shipped-writer digest and value identity; six discrimination tests tightened to the full finding map |
| `40223bd` | family (i) -- six check-side invariants fold-carried |
| `878b638` | family (ii) -- cancel decides read a fold-carried reinstate index |
| `b9d5178` | family (iii) -- fold-carried citation indexes retire the ADR-0164 scans |

## Fences

* **F1 -- no draw, event, order or finding moved.** Held, and proven three
  ways: byte + value identity on four gated corpora, byte identity on the
  whole 104,851-event corpus, and `=` of `check-all`'s full result. No
  gate was ever re-pinned; none went red.
* **F2 -- oracle without `--declared-digest-change`.** Held. IDENTICAL,
  twice, and no declared change was requested by the script or claimed.
* **F3 -- byte vs value identity.** The two never disagreed. The tripwire
  that would have stopped the session is committed and green.
* **F4 -- R-move-not-improve.** One improvement per site. The 14 replays,
  `occupancy-board`, the boarder `sort-by`, `last-uncancelled-index` and
  the bare `Random.nextInt` death were all seen and none touched; they are
  rowed as `roadmap.md#performance-residual-sites`.
* **F5 -- premise corrections are findings.** Two taken: F3-1 (no 10^5
  PROJECTED figure exists) and F-3 (`last-uncancelled-index` cannot ride
  either carrier). Neither was executed as described.
* **F6 -- no vendored-module, schema or history change.** Held.
* **F7 -- health record with the Windows-side sample on every timed
  figure.** Held, including the disclosure that cell C ran at elevated
  host load and which way that biases the claim.
* **F8 -- own dates.** This record, the ADR and the prompt archive are
  dated 2026-08-25; the rulings they carry are dated 2026-08-24.

## Close

Arc 0 is done: three site families removed, equivalence proven rather than
asserted, 9.58x at the program's own 10^5 skeleton target. Arc 1
(stream-partition design) is next and unaffected -- S1 exempted this work
from the reshuffle era precisely because it could not perturb it, and the
byte-identical 10^5 corpus is the receipt.

Local only -- five commits, no push, no tag.

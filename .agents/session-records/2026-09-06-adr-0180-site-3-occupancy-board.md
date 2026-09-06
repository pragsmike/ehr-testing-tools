# 2026-09-06 — ADR-0180 site 3: `occupancy-board` rides the fold

Site 3 of the generate-quadratic program, `roadmap.md#performance-residual-sites`
PRIORITY 1. Ceremony mode: R30 (commit and push at each checkpoint), taken from
the prompt. Prompt archived at
[`../prompts/2026-09-06-adr-0180-site-3-occupancy-board.md`](../prompts/2026-09-06-adr-0180-site-3-occupancy-board.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, section
`3. sim-model/occupancy-board`. No ADR was written and none was owed — this
session ENACTS a charter that was already ruled. It does AMEND that charter's
own text, in a dated section, under two rulings that commissioned exactly that.

Rulings in force: **R-fold-carrier**, **R-equivalence**, **R-order**,
**R-double-occupancy**, **R-charter-same-commit**, **R-min-clamp-correction**,
**R-membership-from-post-state**, **R-move-not-improve**, **R-pins**,
**R-edit**, **R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked files,
local HEAD `2149db0a` equal to `origin/main`. Session start and every bracket
baseline is that sha.

The instrument's own zero ran before any edit: `bin/ground-truth-bracket
2149db0a 2149db0a` reported IDENTICAL on all 38 digested roots (3 skipped, no
`:ground-truth` key).

Both baseline cells reproduced the digests sites 1 and 2 recorded, which is what
makes this session's before-column the same corpus as those: `a7500-persons`
sha256 `3018299a…0d3bd3` at 164.70 s, `a2500-nopersons` `c22d6573…a55208` at
30.25 s.

## 1. What landed

**`cd653c70` — `engine: :board concern at the fold, with its from-scratch law`.**
The carrier and its gate; the FIVE SITES UNTOUCHED. `fold/apply-events` gains a
fifteenth concern, `:board` — `bed-id -> patient-id` — maintained off the same
pre/post participant pair `:bed-index` and `:boarder-index` already read, a
third map in the one pass. Three new public vars in `fold`: `bed-of`
(membership from ONE patient state), `update-board` (the per-event reconcile),
`board-without` (the mask answering the second query shape). `run`'s
`init-world` seeds `:board {}`. `ehrt.sim-engine.board-index-test` is new and
carries the law; `apply-projection-test` transcribes the widened closure.

**`24f7915e` — `engine: occupancy-board reads :board; sim-model charter`.**
The repoint and the charter amendment, in one commit per R-charter-same-commit.
Four `decide` sites and `log-index/bed-reoccupied-by-someone-else?` read the
index; `log-index`'s `sim-model` require retires with it. Four prose sites that
went FALSE under the change are fixed in the same commit (section 4). Five
hand-built test worlds gain the seed, and `sim-emit-hl7`'s hand-rolled fold —
disclosed and left standing by both prior sites — is rewritten through the choke
point because this change made it live.

**`b66da365` — `docs: ADR-0180 — min-clamp sentence corrected; site cites
refreshed`.** A dated corrections section at the end of the charter.

**`ff8c81ae` — `plans: site 3 measured`.** `measurements.md` gains a dated
site-3 section; the P1 row's site-3 clause gains the figures and its site-4
clause the new share; the two JFR reports land in `raw/`.

## 2. The measurement

Both cells reproduced their pre-change log BYTE-FOR-BYTE — the same two digests
as in section 0 — and `bin/ground-truth-bracket` reported IDENTICAL on all 38
digested roots at the clean tip, at `cd653c70` and at `24f7915e`.

| cell | before | after | delta | corrected |
|---|---|---|---|---|
| `a7500-persons` | 164.70 s | **136.32 s** | -28.38 s, -17.2% | **-18.1%** |
| `a2500-nopersons` | 30.25 s | 29.61 s | -0.64 s, -2.1% | -2.9% |

JFR at 7,500 arrivals, inclusive share, `--stack-depth 2048`:
`occupancy-board` **17.87% -> 0.00%** — zero of 9,238 samples, and gone from the
innermost-project-frame table where it had held both rows it appears in at all
(`occupancy_board$fn` 11.87% and `occupancy_board.invokeStatic` 5.83%, 17.70
points between them). That table's new top row is `last_uncancelled_index$fn` at
9.76%, which is site 4. `decide` as a whole went 59.44% -> 51.17%;
`last-uncancelled-index` 17.63% -> 21.63%; `person-simulator` 12.01% -> 15.33%.

**`apply-events` absorbed part of it, 13.06% -> 15.43%** — the index's own cost
charged where the charter said it would be: 17.9 points became 2.4, and
`fold$apply_events$fn` appears in the innermost table at 4.76%. This is SITE 1's
pattern and not site 2's, and the contrast is the point: an in-fold index is
per-event work that lands in the fold's own frame, where site 2's once-at-
`prelude` sweep had no row to appear in at all.

Two things the `measurements.md` section says out loud:

* **`a2500-nopersons` IS NOT A CONTROL HERE**, and a reader coming from the
  section above would take it for one. Site 2's small-cell row was a control
  because that cell has no `:persons` and never called the replaced code. The
  board is on the SHARED path and that cell does run it; -2.1% is a real if
  small win. It is small because the scan is O(patients) per placement over a
  map holding the whole arrival population from t 0, so total cost goes as
  arrivals squared — a third of the arrivals is a ninth of the work over a fifth
  of the wall. A quadratic site pays back in proportion to the square, which is
  why this program is sequenced by share at the TOP cell.
* **The wall figures are one JVM each and carry the instrument's own noise.**
  This session's `a7500-persons` baseline reads 164.70 s at the very commit
  where the site-2 section measured 158.90 s — 3.7% on identical code.

Peak RSS fell at the large cell (2,248 -> 2,197 MB) and ROSE at the small one
(959 -> 1,102 MB) while doing strictly less allocation. Recorded, not claimed,
for the third session running.

## 3. Judgment calls

**(a) The read is RAW `(:board world)`, with no fall back to the definition, and
`run`'s `init-world` SEEDS `:board {}` to pay for that.** A read written
`(or (:board world) (occupancy-board (:patients world)))` could never be wrong —
the fallback IS the definition — but it can be silently slow, and it would have
hidden every hand-built world that does not carry the index behind a path that
quietly does the O(P) work anyway. The raw read fails LOUDLY instead:
`sim-model/free`'s `(remove board ids)` throws a `NullPointerException` on a
missing board rather than reading it as empty. That is the choice site 1's own
finding argues for — a hand-rolled copy of the choke point is a second
definition, and it should announce itself. THE SEED IS WHAT KEEPS THE SHIPPED
PATH FROM EVER MEETING THE THROW: `decide` runs BEFORE the batch that would open
the index, so the world the first `decide` reads has folded nothing. `{}` is not
a stand-in there — it is the definition's own answer, every patient `init-world`
carries being `state/initial-patient`, which names no location, and the law test
pins both halves. `:boarder-index` needs no seed and does not get one; the
asymmetry is a property of the READERS (`first-boarder` reads a missing key as
nil, which is the answer) and is stated in `apply-events`' own docstring.

**(b) `log_index:182` reads the index; the reinstated projection does NOT carry
`:board`.** The prompt left this open and asked for the decision recorded. Both
are correct; this one is chosen because `bed-reoccupied-by-someone-else?` is
handed `run`'s LIVE world by its two `decide` callers — the same world, asking
the same question, one `get` away from the same map. Keeping the definition
there would have left two answers to one question inside one namespace pair, and
left a whole-population rebuild on the generate path for every reinstating
cancel. The `reinstated-state` fallback fold is a different site and stays out:
it returns a PATIENT STATE, an occupancy board is not in one, and its world is
`{:patients {}}`. That absence is asserted positively in
`apply-projection-test`, as is `replay`'s.

**(c) The apply matrix is now 40 of 45 with five ruled absences.** The
apply-unification arc's own 38-of-39 arithmetic is unmoved and still checked
separately, which is what keeps ADR-0180's two additions from being mistaken for
that arc's unfinished cells.

**(d) ADR-0180 ITSELF WAS EDITED, where site 1 chose not to.** Site 1's record
names its own non-edit as a judgment call. This session's prompt commissioned
two specific corrections (R-min-clamp-correction, R-pins), so the charter gains
a dated corrections section rather than being left to be read against a record.
The sections above it are untouched: a charter records what was decided, and a
dated section is where a decision is read against what running it found.

## 4. Findings

**The `sim-emit-hl7` hand-rolled fold went LIVE, and this session fixed it.**
Sites 1 and 2 both disclosed `emit_hl7_test.clj`'s hand-rolled
`fold-events`/`admit` pair and left it standing on the grounds that it asked no
boarder question. `:board` is what made it live: every scripted `admit` there
drives `decide :admission`, which reads the board off the world. It now calls
`fold/apply-events` with a declared projection — the same five concerns
`engine_test.clj`'s helper declares — and the pre-existing
unfiltered-participants defect goes with the rewrite, disclosed in the docstring
rather than absorbed. **The lesson is site 1's, one wave later and confirmed**:
a copy of the choke point does not stay a faithful copy, and the interval before
it is caught is however long it takes for the fold to grow a concern the copy's
callers happen to read.

**Two of the five hand-built worlds announced themselves as NPEs.**
`step-rejected-event-renders-no-message` and
`hand-built-worlds-without-a-registered-event-fall-back-to-legacy-pid`, both in
`emit_hl7_test.clj`, threw out of `sim-model/free`'s `(remove board ids)`. That
is the no-fallback read working exactly as judgment call (a) intends, and it is
recorded because the same two tests under a fallback read would have passed
while quietly rebuilding the board.

**ADR-0180's site-3 census was ONE CALL SITE SHORT.** It names "four call sites
in `decide` and one in `log_index.clj:182`". There is a sixth:
`ehrt.sim-check.check`'s `surge-only-when-earlier-rungs-exhausted`
(`check.clj:935`). It is in the CHECK phase and calls the definition over a
REPLAY ENTRY's `:world-before` rather than over a world, so — `replay`
deliberately not carrying `:board` — it is a site the index does not reach and
correctly still rebuilds. Nothing was decided differently, because it was never
in scope. It is written into the charter's dated section because a reader
counting call sites against the tree would otherwise find one the record does
not mention, and because it is the reason `occupancy-board` stays LIVE code
rather than becoming a test-only reference.

**`clojure -M:poly test brick:A brick:B` runs only ONE of them, silently.**
A run of `brick:sim-engine brick:sim-emit-hl7 brick:sim-model` exited 0 having
tested `sim-model` in three projects, three times, and reported twelve
`Test results:` lines that looked like coverage. The bricks were re-run one
invocation each. This is the same family as the already-known `:brick X`
(colon-prefixed) trap and is worth the same suspicion: count what ran, do not
read the exit code.

## 5. The law, and that it is not vacuous

`ehrt.sim-engine.board-index-test` keeps `sim-model/occupancy-board`'s body
VERBATIM as `naive-board` (R-move-not-improve — a COPY here rather than a move,
because the definition does not retire) and asserts at EVERY intermediate world
of a churn-bearing generated log that the WHOLE MAP is `=`, not just the key
set. Value equality is separately owed and this is where: everywhere else the
board is a PREDICATE over a derived id list, and at
`bed-reoccupied-by-someone-else?` its value is an occupant id. The masked
`dissoc` query is driven for every patient at every world plus one absent id.

**Red→green, run in-process against the same law** by rebinding `update-board`
and re-running the namespace:

| index | failures |
|---|---|
| shipped | **0** |
| retirement without the value guard | 3 |
| no retirement at all | 5 |
| membership from the PRE-event state | 9 |
| restored | **0** |

The first mutation is the one worth the row: `update-board`'s value guard is
LOAD-BEARING rather than defensive, because a `:bed-swap` moves two patients'
beds under ONE event and whichever participant reconciles first files its new
bed over a key the other still holds. Unguarded, the second retires the key the
first just filed. The law catches it.

Non-vacuity, counted over two pinned cases:

| | bed-cycle off | bed-cycle on |
|---|---|---|
| worlds | 48 | 31 |
| worlds with a non-empty board | 46 | 29 |
| biggest board | 6 beds | 5 beds |
| worlds where the board SHRINKS | 6 | 5 |
| events with TWO patient participants | 10 | 7 |
| masked queries that remove a key | 144 | 77 |

R-double-occupancy is enacted rather than assumed away. A pinned test admits two
patients into one bed and records what the index answers there:
last-writer-by-EVENT-order, where the definition answers last-in-seq-order, and
the mask drops the key where the definition hands the bed to the other occupant.
Only the index side is pinned — the definition's answer there is a hash-order
artefact and pinning it would be pinning an accident.

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `cd653c70` | 420 | 27,859 | 0 | 0 |
| `24f7915e` | 420 | 27,859 | 0 | 0 |
| `b66da365` | 420 | 27,859 | 0 | 0 |
| `ff8c81ae` | 420 | 27,859 | 0 | 0 |

The baseline at `2149db0a` was 418 lines and 27,797 passes (site 2's record,
section 6). The +2 lines are the one new namespace counted once per project it
belongs to; the +62 passes are its own assertions plus `apply-projection-test`'s
new negative ones. Nothing moved at the repoint, which is what an
output-identical refactor should look like from here.

`:onboarding` reading-set headroom is UNCHANGED at 35 against an unchanged 1,530
budget: the roadmap edit is net-zero at 20 lines in, 20 lines out, sites 1 and 2's
clauses compacted to the figures a register owes now that their detail lives in
the dated sections the row cites.

## 7. Background processes

Nine harness-tracked background jobs, each of which exited on its own with its
exit code recorded: three `bin/ground-truth-bracket` runs (the instrument's own
zero at the clean tip, then one per CODE commit — the two docs commits owed
none), two timed cell runs (before, after), three `make test` runs, one JFR
profile run. Everything else — `bin/preflight`, one `make docsgen`, two `make
state-derived` runs, three `clojure -M:poly test brick:…` runs, and four
throwaway `clojure -M:dev:test` probes (the disagreement locator, the
non-vacuity counter, the negative control, and one namespace-pair run) — ran in
the foreground. Several `sleep` waiters were moved off the foreground by the
harness when they outran their timeouts; each exited on its own. None was left
running at close.

## 8. HEAD landed

The measurement commit is the last payload commit; this record and its prompt
archive land on top of it, and a close-marker commit follows once CI is verified
green with `gh run view`. Baseline for every bracket in this session was
`2149db0a`.

## 9. CI

Filled in at close.

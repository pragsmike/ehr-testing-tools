# 2026-09-07 — ADR-0180 site 7: `check-all` folds once; the run hands its own projection

Site 7 of the generate-quadratic program, the LAST of the three the 2026-09-06
addendum chartered and the last of the program's seven —
`roadmap.md#performance-residual-sites` PRIORITY 1, now CLOSED. Ceremony mode:
R30 (commit and push at each checkpoint), taken from the prompt. Prompt archived
at
[`../prompts/2026-09-07-adr-0180-site-7-check-folds-once.md`](../prompts/2026-09-07-adr-0180-site-7-check-folds-once.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, its
`Addendum, 2026-09-06` section 7. No ADR was written and none was owed — this
session ENACTS a charter already ruled and made no correction to it.

Rulings in force: **R-check-once**, **R-board-in-entry** (new with this prompt),
**R-verdict-bracket** (new), **R-bootstrap-mrn**, **R-warm-up-omission**
(A2(b), untouched), **R-no-second-path**, **R-raw-read**, **R-move-not-improve**,
**R-pins**, **R-edit**, **R-cap**, **R-rider-hint** (new), **R-sentinel**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked files,
local HEAD `ee7d0070` equal to `origin/main`. Session start and every bracket
baseline is that sha.

`bin/ground-truth-bracket ee7d0070 ee7d0070` reported IDENTICAL on all 38
digested roots before any edit. The three baseline cells and the matched
before-half JFR pair were also taken before any edit.

**ONE BASELINE RUN DIED AND WAS RE-TAKEN.** The first background job was killed
mid-way through the 22,500 cell — WSL restarted under it, truncating a 174 MB
log that had already been hashed correctly to 88 MB. Sections D and E were
re-run from scratch rather than salvaged; the re-run's digest matched the
committed `7d105743…78e0a` again.

## 1. What landed

**`78f228f7` — `bin: check-verdict-bracket`.** The instrument R-verdict-bracket
names, landed BEFORE any repoint and run at the clean tip to record its own
zero: 42 rows IDENTICAL, `handed arity: ABSENT`. Section 3(a) is its design.

**`78226b5a` — `bin: check-verdict-bracket-src is mode 755 in the index`.**
Fix-forward for the `ehrt.cli.executable-bits-test` gate; see section 4(a).

**`c2e62012` — `check: check-all folds once; :929 reads :board`.** Every
replay-reading invariant takes a trailing `records` and declares
`{::records true}`; `check-all` folds once and passes the one seq. `:929` reads
`:board` off the entry, `replay-projection` opts the `:board` concern in, and
`replay`'s world gains a `{}` board seed. `sim-model/occupancy-board` loses its
last `src` caller.

**`e6cd1dbb` — `sim: run hands check-all its own projection`.** `engine/run`
threads the entries it already builds, returns them as `:entries`, and
`ehrt.sim.run` passes them to `check-all`'s 5-arity. The in-run catalog folds
the log ZERO times. Rider: `sim-model.facility/choose`'s `rng` is hinted.

**`9f4ddc71` — `plans: site 7 measured`.** The dated measurements section, the
program's close table, and the roadmap P1 row rotated to `## Done`.

## 2. The measurement

All three cells reproduced their pre-change logs BYTE-FOR-BYTE —
`a7500-persons` `3018299a…0d3bd3`, `a2500-nopersons` `c22d6573…a55208`,
`a22500-nopersons` `7d105743…78e0a` over the same 174,866,696 bytes — and
`bin/ground-truth-bracket` reported IDENTICAL on all 38 digested roots at BOTH
code commits.

| cell | phase | before | after | delta |
|---|---|---|---|---|
| `a7500-persons` | generate | 108.66 s | **70.10 s** | **-35.5%** |
| `a7500-persons` | check | 53.31 s | **26.29 s** | **-50.7%** |
| `a2500-nopersons` | generate | 28.03 s | **19.94 s** | **-28.9%** |
| `a2500-nopersons` | check | 21.39 s | **13.67 s** | **-36.1%** |
| `a22500-nopersons` | generate | 169.22 s | **98.66 s** | **-41.7%** |
| `a22500-nopersons` | check | 112.28 s | **54.17 s** | **-51.8%** |

JFR at 7,500 with `:persons`, a matched pair recorded in this session (the
before half at `ee7d0070` before any edit): `replay` **20.55% → 0.00%** of
generate — a frame not on any stack, which is what "folds zero times" looks
like in an instrument — and **48.17% → 5.16%** of check. `sim-check` 31.13% →
15.30% of generate and 72.32% → 52.73% of check.

Peak RSS fell at five of six cell-phases (the exception is the 2,500 generate,
+28 MB on a byte-identical log), and at the top cell 3,476 → 1,985 MB. The
`-Xlog:gc*` floor at 7,500 fell with it: live set at the end of the run 562 →
409 MB, peak PRE-GC 1,916 → 915 MB, heap capacity 1,880 → 960 MB.

**THE PROGRAM.** Seven sites, 22,500-arrival generate **1,472.56 s → 98.66 s,
14.9x**, over a log byte-identical to the committed 2026-09-05 digest.

## 3. Judgment calls

**(a) THE INSTRUMENT'S POPULATION IS THREE-PART, AND ONLY ONE PART HAS TEETH AT
EACH COMMIT.** R-verdict-bracket asks for the violation vector "computed via the
handed projection and via a fresh replay". For an oracle ROOT there is no run to
hand anything, so the handed side is an `engine/replay` the instrument computes
— which gates the threading of `c2e62012` and says nothing about the population
question. For a CELL the handed side is the IN-RUN self-check's own verdict and
the replayed side is `sim check` over the log that run emitted, which is exactly
the population question and has teeth only at `e6cd1dbb`. Both halves are real
user-facing paths (`ehrt.sim.interface/run-command` and `/check-command`), so
the instrument needs no harness of its own — R-no-second-path.

**(b) THE NEGATIVE CONTROL REFUSED ITS OWN FIRST FORM.** Every log in the roots
and cells populations is one the checker passes, so without a control the
instrument compares `[]` with `[]` — `R-empty-population-is-red`'s shape. The
first control dropped a root's head event and convicted NOTHING (these roots
walk a module; their first event strands no reference), and the instrument
STOPPED on itself. The perturbation was made stronger — the largest root
REVERSED, 28 violations — rather than the guard weaker.

**(c) R-BOARD-IN-ENTRY WAS READ AS REMEDY (a), "carry `:board` in the entry",
AND THE ENTRY KEY IS WHERE IT LANDED.** The ruling's phrasing is "`:929` reads
`(:board world-before)`". A replay entry's `:world-before` is `(:patients w)`
and never the world, so a literal reading would have required changing what
`:world-before` MEANS at ten other reading sites; the addendum's own remedy list
says "carry `:board` in the entry", so the entry gains a `:board` key and `:929`
destructures it. One defensible reading, so fix-forward WITH DISCLOSURE rather
than STOP (`rulings.md#R-stop-only-on-two-defensible-readings`).

**(d) `check-all` DISPATCHES ON METADATA, NOT ARITY.** `{::records true}` on the
var, because `warm-up-mark-matches-window` and
`result-analytes-match-order-profile` also take a second argument and neither
reads a record. A row that grows a record read and forgets the key replays for
itself, which is correct and slow, never wrong — the failure mode is a
performance regression, not a wrong answer.

**(e) `bed-fold` STOPPED RETURNING `:records`.** Its own docstring justified that
key by the SECOND `engine/replay` it saved invariant 5; the records are handed in
now, so a key that hands them back to the caller who supplied them went out with
the replay that made it necessary. A private helper, no caller outside this file.

**(f) THE PROMPT SAID "FOURTEEN CALL SITES"; THE FIX TOUCHED 20 INVARIANTS.** The
fourteen are TEXTUAL, as the addendum says: `:399` is inside `fold-records`
(five invariants) and `:731` inside `bed-fold` (four readers). Eighteen base-
catalog rows and two facility rows carry the new parameter, plus four private
helpers.

## 4. Findings

**(a) A NEW FILE UNDER `bin/` MUST BE 755 IN THE INDEX, INCLUDING A `.clj`.**
`ehrt.cli.executable-bits-test` covers every tracked file under `bin/`, not just
the shell wrappers — `bin/ground-truth-bracket-src/manifest.clj` and
`bin/event-census-src/…/census.clj` are both 755. The instrument's source landed
644 and the gate caught it on the next full suite run. Fixed forward in its own
commit rather than by amending, per
`rulings.md#R-amend-unpushed-message-only`.

**(b) THE CENSUS MATRIX WENT RED, AS IT SHOULD HAVE — a declared baseline edit.**
`ehrt.sim-engine.apply-projection-test` pinned site 2 at twelve concerns and
pinned `:board` ABSENT from it, in four assertions (`:148`, `:163`, `:204`,
`:207`). The column reads thirteen now and the whole matrix 43 of 51. The arc's
own 38-of-39 arithmetic is UNTOUCHED, because `:board` was never one of the
apply-unification arc's thirty-nine cells.

**(c) A SECOND GATE FOUND A CALL SITE THE FIRST DID NOT.**
`ehrt.sim-engine.apply-restamp-identity-test`'s `entries-under` exists to mirror
`replay`'s own accumulator, and its third assertion says `replay` ITSELF is the
fold under test. Seeding `:board {}` in `replay` and not in the mirror made that
assertion compare two call sites instead of two projections. Co-landed.

**(d) FOUR OCCUPANCY ROWS WALK THE WHOLE WORLD UNDER A POSITIVE GUARD, and the
addendum's reading table does not name them.** `no-double-occupancy`,
`admitted-occupies-one-slot`, `outpatient-patients-occupy-no-bed` and
`non-admitted-patients-hold-no-bed` emit from `world-after` — but only once
their own index already says somebody is offending. Their finding SET is
identical under either projection (a `state/initial-patient` names no bed, holds
no slot and is not `:outpatient`), and their ORDER is that map's seq order,
which for a `PersistentHashMap` is fixed by key hashes and therefore also
identical — except inside a hash collision, and below nine entries where a grown
map is an insertion-ordered `PersistentArrayMap`. THE RESIDUE IS UNREACHABLE ON
ANY LOG A RUN SHIPS: these rows produce nothing on a log the checker passes, and
a log it does not pass is a run that returns `:self-check-failed` and no corpus.
Recorded rather than fixed: fixing it would change verdict order relative to the
pre-session tip, which is the one thing this session may not do.

**(e) THE ADDENDUM'S HEAP HYPOTHESIS IS REFUTED AS STATED.** It reasoned from a
3,863 MB transient to 67,500 arrivals being unreachable and left it for this
session to test. Generate's peak at 22,500 is 1,985 MB against a 3.88 GB
`MaxHeapSize` — headroom 12% → 49% — and the post-collection floor, the
instrument the addendum named for the question, fell with it rather than merely
following the budget.

**(f) THE SCALE TABLE IS STALE AND THREE SITES MADE IT SO.**
`docs/consuming-ground-truth.md`'s three `corpus generate` process walls were
measured at `4ddf62c2`, before sites 5, 6 and 7 each shortened that phase at
that very configuration. Nothing gates them — which is
`roadmap.md#dense-7500-gate-gaps`' own gap (a). Disclosed, not re-measured:
re-running the table is that row's work, not a site's, and a paragraph naming
the concrete staleness was added to it.

**(g) THE 7,500 RE-BASELINE READ 23% HIGH, not the 3-6% the P1 row records.**
108.66 s against site 6's 87.95 s on code neither session changed. Every
percentage in section 2 is measured against the higher number, so the 7,500
deltas are conservative; the other two cells' before halves were taken in the
same session on the same machine and carry no such caveat.

## 5. The law

`ehrt.sim-engine.handed-projection-test` (178 assertions) is the new gate. It
asserts, over four generated configurations (churn; churn + bed cycle; churn +
encounters; churn + encounters + scheduling), that `run`'s entries and
`fold/replay`'s entries agree at every site the addendum's reading table names:
`:event` entry for entry AND equal to the log itself, `:patient-id`/`:before`/
`:after`/`:board` entry for entry, and `:world-before`/`:world-after` at EVERY
PARTICIPANT of every event. R-bootstrap-mrn is its own deftest.

**AND IT ASSERTS THE DIVERGENCE IS REAL** —
`the-population-divergence-is-real` requires the two `:world-before` vectors to
be UNEQUAL and `run`'s to be strictly larger at the first event. A law over two
projections that turned out to be equal would be a law about nothing.

Red→green, honestly: the namespace's own first run went red on
`churn+scheduling+bed-cycle`, because `:scheduling` is a CONFIG MAP and `true`
makes `run` answer `result/error :invalid-scheduling` and produce no log at all.
The assertion that caught it is `(is (seq entries))`. The fourth configuration
carries `scheduling_test.clj`'s own fixture rates now.

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `c2e62012` (the check-side fold) | 424 | 28,057 | 0 | 0 |
| `e6cd1dbb` (the in-run handoff) | 426 | 28,415 | 0 | 0 |
| docs tip (over `9f4ddc71`) | 426 | 28,415 | 0 | 0 |

Site 6's close was 424 / 28,057. `c2e62012` moves NEITHER figure: it edits
assertions in place and adds none. `e6cd1dbb` adds one test namespace, which
runs under two projects, plus its lint assertion.

`:onboarding` reading-set headroom went **16 → 48** against an unchanged 1,530
budget, on the roadmap row's rotation to a one-line `## Done` entry. `:docs` is
untouched and stays at its standing headroom of 0.

## 7. Background processes

Harness-tracked background jobs, each of which exited on its own: one baseline
run (killed by a WSL restart, section 0), its re-run, one before-half JFR pair,
two full `bin/check-verdict-bracket` runs, two combined gate runs (`make test` +
verdict bracket), two further `make test` runs, and one combined measurement run
(three cells, the gc log, the after JFR pair). `bin/preflight`, both
`bin/ground-truth-bracket` invocations, three brick-scoped test runs, two `make
docsgen` runs, `git`, `gitleaks` and `bin/post-push-verify` ran in the
foreground.

**R-sentinel, honestly.** Every wait for a harness-tracked job was a completion
notification or a single bounded `sleep` inside one foreground call, never a
poll loop that wakes the session per iteration.

At close, `ps` reports no `java`, no `make` and no `sleep` belonging to this
session, and `git worktree list` shows only the main clone — both bracket runs
removed their worktrees as they finished.

## 8. HEAD landed

The measurement commit `9f4ddc71` is the last payload commit; this record and
its prompt archive land on top of it, and a close-marker commit follows once CI
is verified green with `gh run view`. Baseline for every bracket in this session
was `ee7d0070`.

## 9. CI

Two pushes: `ee7d0070..c2e62012` and `c2e62012..e6cd1dbb`, each verified with
`bin/post-push-verify` immediately after. The remaining commits go out in one
final push, and section 9's table is completed from `gh run view` rather than
assumed.

CI green at the tip is the marker site 7 closed — and with it ADR-0180's
generate-quadratic program, all seven sites.

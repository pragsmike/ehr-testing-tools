# 2026-09-06 — ADR-0180 site 4: `last-uncancelled-index` rides the fold

Site 4 of the generate-quadratic program, and its LAST site --
`roadmap.md#performance-residual-sites` PRIORITY 1. Ceremony mode: R30 (commit
and push at each checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-06-adr-0180-site-4-last-uncancelled-index.md`](../prompts/2026-09-06-adr-0180-site-4-last-uncancelled-index.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, section
`4. log-index/last-uncancelled-index`. No ADR was written and none was owed —
this session ENACTS a charter that was already ruled, and unlike site 3 it was
commissioned to make no correction to that charter, so ADR-0180's own text is
untouched.

Rulings in force: **R-fold-carrier**, **R-equivalence**, **R-order**,
**R-no-second-path** (ADR-0169 F-3), **R-raw-read** (site 3, standing),
**R-move-not-improve**, **R-pins**, **R-edit**, **R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked files,
local HEAD `228e2c62` equal to `origin/main`. Session start and every bracket
baseline is that sha.

The instrument's own zero ran before any edit: `bin/ground-truth-bracket
228e2c62 228e2c62` reported IDENTICAL on all 38 digested roots (3 skipped, no
`:ground-truth` key).

Both baseline cells reproduced the digests sites 1 to 3 recorded, which is what
makes this session's before-column the same corpus as theirs: `a7500-persons`
sha256 `3018299a…0d3bd3` at 144.77 s, `a2500-nopersons` `c22d6573…a55208` at
30.44 s.

## 1. What landed

**`28fe10b5` — `engine: :cancel-index concern at the fold, with its
from-scratch law`.** The carrier and its gate; the THREE CALL SITES UNTOUCHED.
`fold/apply-events` gains a sixteenth concern, `:cancel-index` — two maps in
one key: `[patient-id event-type] -> [log index …]` in log order, and
`cancel-type -> #{consumed index …}`. Two new public vars in `fold`:
`update-cancel-index` (the per-event maintenance) and `last-uncancelled` (the
ordered lookup). `run`'s `init-world` seeds `:cancel-index {}`.
`ehrt.sim-engine.cancel-index-test` is new and carries the law;
`apply-projection-test` transcribes the widened closure.

**`5ac15dce` — `engine: cancel decides read :cancel-index`.** The repoint.
`log-index/last-uncancelled-index` keeps its name, arity and nil contract; its
BODY becomes `fold/last-uncancelled` and its first parameter changes from
`ground-truth` to `world`. `decide :cancel-admit` loses its `ground-truth`
binding entirely. Both test-side `fold-events` helpers gain `:cancel-index` AND
`:log-ordinal`; four hand-built worlds gain the seed. Three prose sites that
went false are fixed in the same commit.

**`edecb970` — `plans: site 4 measured; ADR-0180 program summary`.**
`measurements.md` gains a dated site-4 section plus the program's own summary
table; the roadmap P1 row's four site clauses collapse to one cumulative line.
Two JFR reports land in `raw/`.

## 2. The measurement

Both cells reproduced their pre-change log BYTE-FOR-BYTE — the same two digests
as in section 0 — and `bin/ground-truth-bracket` reported IDENTICAL on all 38
digested roots at the clean tip, at `28fe10b5` and at `5ac15dce`.

| cell | before | after | delta | corrected |
|---|---|---|---|---|
| `a7500-persons` | 144.77 s | **112.29 s** | -32.48 s, -22.4% | **-23.7%** |
| `a2500-nopersons` | 30.44 s | **26.92 s** | -3.52 s, -11.6% | **-15.7%** |

JFR at 7,500 arrivals, inclusive share, `--stack-depth 2048`:
`last-uncancelled-index` **21.63% -> 0.00%** — zero of 7,470 samples, and gone
from the innermost-project-frame table where it had been the TOP ROW at 9.76%.
`decide` as a whole went 51.17% -> 36.85%; `apply-events` 15.43% -> 20.05%,
absorbing 4.6 of the 21.6 points as the index's own cost, which is sites 1 and
3's pattern for the same reason.

**THE PROGRAM, cumulatively.** All four sites now read 0.00%–0.01% where each
was 25.23%, 19.17%, 17.87% and 21.63% at the head of its own session;
`apply-events` went 7.56% -> 20.05%. `a7500-persons` 256.15 s -> 112.29 s,
**-58.0% corrected**; `a2500-nopersons` 34.85 s -> 26.92 s, -29.5%. That is a
LOWER BOUND: every session's re-baseline read 3–6% HIGHER than the previous
session's after on identical code (189.79 -> 195.48, 158.90 -> 164.70,
136.32 -> 144.77), so the machine drifted slower across the day.

**AND THE TOP OF THE DECADE, which is this session's own finding rather than an
extrapolation.** One timed generate of `a22500-nopersons`: **1,472.56 s ->
396.18 s**, 3.7x — and the log is BYTE-IDENTICAL to the committed 2026-09-05
digest, `7d105743…78e0a` over the same 174,866,696 bytes. 431,677 events
agreeing to the byte is an order of magnitude past what the 38 oracle roots and
the two bracketed cells reach, so the four-site program is output-identical at
the scale it was commissioned for and not only at the scale it was gated at.
The WALL half of that comparison is an order of magnitude and nothing finer:
the committed figure is the mean of two JVMs with `run-cells.sh`'s own warm-up
and this is one JVM after a profile run.

Peak RSS ROSE at both 7,500/2,500 cells (2,064 -> 2,303 MB; 884 -> 903 MB) and
FELL at 22,500 (3,732 -> 3,599 MB). Recorded, not claimed, for the fourth
session running — but this is the one site where a rise is the EXPECTED
direction rather than a puzzle, because `:cancel-index` is the only one of the
four indexes that never evicts.

## 3. Judgment calls

**(a) The prompt's step-3 fork: `log-index/last-uncancelled-index` KEEPS ITS
NAME and its body becomes the index read, rather than the three `decide`
methods calling `fold/last-uncancelled` directly with the definition deleted.**
Both satisfy R-no-second-path — `src` holds exactly one implementation either
way. This one is SITE 1'S OWN PATTERN: `decide/waiting-boarder` kept its name
and its prose and became `fold/first-boarder`'s ordered lookup. It keeps the
DEFINITION of the answer where a reader of `log-index` will look for it, in the
namespace whose whole subject is queries over the log, and it makes the repoint
a three-line diff at the call sites instead of a rewrite. The one thing it could
not keep is the SIGNATURE: a world is what carries the index, so the first
parameter is `world` where it was `ground-truth`. The prompt's "same signature"
is read as same arity and same nil contract, which is what it has.

**(b) The read is RAW `(:cancel-index world)` and a missing index THROWS —
explicitly, with `ex-info`, not by accident.** R-raw-read is site 3's standing
choice and it transfers, but the failure MODE does not transfer for free. Site
3's raw read fails loudly on its own: `sim-model/free` calls the board as a
predicate and throws on nil. A raw read here would have returned nil silently,
and nil is this query's own "no such event" answer — so a world without the
index would have turned every legal cancel into a `:step-rejected` and moved
every draw after it, quietly. The explicit throw is what makes the no-fallback
read honest at this site. `run`'s `init-world` and four hand-built test worlds
seed `{}` so the shipped path never meets it.

**(c) THE TEST HELPERS NEEDED `:log-ordinal` AS WELL, and that is a real trap
rather than bookkeeping.** `engine_test`'s and `emit_hl7_test`'s `fold-events`
fold ONE BATCH AT A TIME. Without `:log-ordinal`, `base-idx` is 0 for every
batch, so a scripted admit-then-transfer files both events at index 0 and the
cancel behind them names the wrong one. It would not have thrown; it would have
returned a plausible integer. Two engine tests pin `(= 0 (:cancels-event-id …))`
and `(= 1 …)` and are what would have caught it — but only because those
particular assertions exist.

**(d) Only the worlds that DRIVE a cancel get the seed.** Four of the eight
hand-built worlds carrying `:board {}` gained `:cancel-index {}`; the other four
drive no cancel decide and got nothing. That asymmetry is the charter's
guard-by-membership contract showing through on the READER side, and it is
stated in the code rather than left to be inferred.

**(e) ADR-0180 itself is UNTOUCHED, where site 3 edited it.** Site 3's prompt
commissioned two specific corrections; this one commissioned none, and the
charter's site-4 section survives its own session without a correction being
owed. The program summary went into `measurements.md`, where the figures it
summarises live, rather than into the charter.

**(f) The two docs commits share one suite run.** AGENTS.md's de-scaffold ruling
exempts a docs-only diff from a local `make test` entirely; both `edecb970` and
this record's own commit are docs-only, and one `make test` over the final tree
is more than that rule asks for rather than less. Section 6 has the figures.

**(g) The prompt's step-4 wording asked for a program-summary table "in
measurements.md"; the roadmap got the CUMULATIVE LINE and nothing more.** The
table is four rows of shares and walls, which is register-sized detail; the row
carries the two numbers a reader of the roadmap actually needs — -58.0% at 7,500
and 3.7x at 22,500 — and points at the file. That is what let the collapse be
net-negative rather than net-zero.

## 4. Findings

**THE PROPERTY IS BLIND TO PARTICIPANT FILING, and only a pinned case decides
it.** The negative control below runs a mutation that files each event under the
FIRST patient participant instead of all of them. It fails 2 assertions — both
in the hand-built `an-event-with-two-patient-participants-is-filed-under-both`
— and the generated-corpus property does not notice. The reason is structural
and worth writing down: no class the three caller pairs ask about
(`:admission`, `:transfer`, `:discharge`) is ever multi-participant. `:merge`
and `:bed-swap` are, and nobody queries them. The property quantifies over a
corpus that cannot reach the distinction, which is exactly the case ADR-0180's
law point 2 is about at one remove — the corpus is not vacuous, but it is
vacuous FOR THIS CLAUSE.

**A ONE-BED FIXTURE COULD NOT REACH THE LAW'S OWN SUBTLETY.**
`board_index_test.clj`'s facility, reused as-is, starves under `:bed-cycle`:
the ward is never free, the run exhausts inside thirty events, and NO patient
reaches a second admission — so `repeats` was 0 and the non-vacuity gate said
so on its first run. Two licensed Renal beds fixes it (3 and 1 repeats over the
two pinned cases). The gate caught its own fixture, which is what a non-vacuity
assertion is for.

**`:cancel-index` NEVER EVICTS, and that is a memory statement, not just an
elegance one.** The other three ADR-0180 indexes reconcile a membership from a
pre/post patient pair and can shrink. This one is event-derived: a log index is
immutable once written and `:cancels-event-id` never moves, so the index only
grows — one integer per event per patient participant, for the whole run. Peak
RSS rose at both 2026-09-06 cells and this is the expected direction. It also
means there is no eviction case to enumerate and none to miss, which is why
this site needed no `R-membership-from-post-state` argument at all.

**AN EVENT WITHOUT A `:cancels-event-id` KEY, disclosed rather than absorbed.**
The scan's `(map :cancels-event-id)` over a cancel-type event lacking the key
would have put `nil` in its consumed set; the index keys on the key's PRESENCE
and puts nothing. The two agree because that set is only ever tested against an
integer log index. Stated in `update-cancel-index`'s docstring because it is a
difference, even though it is not a divergence.

**A `run_in_background` COMPLETION NOTIFICATION FIRED WHILE THE COMMAND WAS
STILL RUNNING.** The final `make test` was reported "completed (exit code 0)"
by the harness at a point where the log carried 233 of its 422 `Test results:`
lines, no `EXIT=` marker, and a live `java` at 13 minutes' `etime`; it ran to
completion 10 minutes later. The outer `wsl` wrapper returned while the inner
`make` kept going. THE LESSON IS THE STANDING ONE ONE STEP FURTHER ON: the
memory entry about background logs says to `rm -f` the log first and check its
mtime, because a stale log can read as a false green. This is the same failure
from the other side — a fresh log plus a completion notification can ALSO read
as a false green, and the only sound signal is the sentinel the command writes
itself. Every suite figure in section 6 is taken from an `EXIT=` line that the
command wrote, never from a notification.

**ONE PRE-EXISTING SENTENCE CORRECTED, under the R-move-not-improve boundary.**
`apply-events`' docstring read "the two world-carried indexes" while listing
THREE — a count left behind by site 3's own addition. Site 4 had to rewrite that
sentence to name a fourth either way, so the count went with the rewrite rather
than being left false. Disclosed in the docstring itself.

## 5. The law, and that it is not vacuous

`ehrt.sim-engine.cancel-index-test` keeps `log-index/last-uncancelled-index`'s
pre-change body VERBATIM as `naive-last-uncancelled-index` (R-move-not-improve
— a MOVE here rather than site 3's copy, because `src` keeps no second
implementation) and asserts at EVERY intermediate world of a churn-bearing
generated log that the two agree, for every patient the log names plus one it
does not, and for all three (event-type, cancel-type) pairs `decide` asks. The
projection is `#{:log-ordinal :log-mirror :cancel-index}` and the two absences
are a claim: the index is event-derived, so no patient state is among its
inputs.

**Red→green, run in-process against the same law** by rebinding
`update-cancel-index`/`last-uncancelled` and re-running the namespace:

| index | failures |
|---|---|
| shipped | **0** |
| read forwards (first, not last) | 2 |
| one flat cancelled set, not keyed by cancel type | 3 |
| no cancelled filter at all | 7 |
| filed under the subject only | 2 |
| restored | **0** |

The first mutation is the one the site's own prose is about: reading the vector
forwards agrees with the scan on every patient admitted once, which is nearly
all of them, and the pinned re-admit case is what convicts it.

Non-vacuity, counted over two pinned cases (16 patients, churn 0.4):

| | bed-cycle off | bed-cycle on |
|---|---|---|
| events / worlds / probe ids | 95 / 96 / 17 | 42 / 43 / 10 |
| non-nil answers | 1,395 | 248 |
| nil because ALREADY CANCELLED | 555 | 84 |
| nil because NEVER EXISTED | 2,946 | 958 |
| indices actually consumed | 17 | 4 |
| patients carrying >1 event of a cancellable class | 3 | 1 |
| events with TWO patient participants | 18 | 7 |

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `28fe10b5` | 422 | 27,935 | 0 | 0 |
| `5ac15dce` | 422 | 27,935 | 0 | 0 |
| docs tip (over `edecb970` + this record) | 422 | 27,935 | 0 | 0 |

The baseline at `228e2c62` was 420 lines and 27,859 passes (site 3's record,
section 6). The +2 lines are the one new namespace counted once per project it
belongs to; the +76 passes are its own assertions plus `apply-projection-test`'s
three new negative ones. NOTHING MOVED AT THE REPOINT, which is what an
output-identical refactor should look like from here.

`:onboarding` reading-set headroom went **35 -> 39** against an unchanged 1,530
budget, on the roadmap edit's net -4 lines alone. The prediction that this
record and its prompt archive would spend 2 of those 4 was WRONG and is
corrected here: the per-file rows those two directories' `README.md` files used
to carry are gone, `INDEX.md` is GENERATED by `make state-derived`, and neither
`INDEX.md` is in the `:onboarding` set. A session record costs that budget
nothing.

## 7. Background processes

Harness-tracked background jobs, each of which exited on its own with its exit
code recorded: three `bin/ground-truth-bracket` runs (the instrument's own zero
at the clean tip, then one per CODE commit — the two docs commits owed none),
two timed cell-pair runs (before, after), one timed `a22500-nopersons` generate,
one JFR profile run, and the `make test` runs of section 6. Everything else —
`bin/preflight`, two `make state-derived` runs, and several throwaway
`clojure -M:dev:test` probes (the namespace smoke runs, the corpus-shape probes,
the negative control) — ran in the foreground. Several `sleep` waiters were
moved off the foreground by the harness when they outran their timeouts; each
exited on its own. None was left running at close.

## 8. HEAD landed

The measurement commit `edecb970` is the last payload commit; this record and
its prompt archive land on top of it, and a close-marker commit follows once CI
is verified green with `gh run view`. Baseline for every bracket in this session
was `228e2c62`.

## 9. CI

All four commits went out in ONE push, so GitHub Actions ran once, at the tip.
Verified with `gh run view` rather than assumed:

| commit | run | conclusion |
|---|---|---|
| `08a41efd` (tip, covering `28fe10b5`, `5ac15dce`, `edecb970`) | 34065721441 | **success** |

`bin/post-push-verify` ran immediately after the push: remote tip matches HEAD,
every commit message in `228e2c62..08a41efd` is pure ASCII, and the CI run was
reported once rather than awaited (AR-CI-4) — this section is where it was
awaited. `gitleaks` scanned 1,482 commits and 43.94 MB at the push hook and
found no leaks. Each of the four pushed messages was diffed against the file
that produced it; every diff was exactly one trailing blank line, which is
`git log --format=%B`'s own formatting artefact and not a mismatch.

**`gh run watch --exit-status` REPORTED COMPLETE WHILE THE RUN WAS
`in_progress`** — the same premature-notification shape section 4 records for
`make test`, twice in one session and on two different commands. The
conclusion above is `gh run view`'s own `status`/`conclusion` pair, polled
until it read `completed success`, not the watcher's exit code.

CI green at the tip is the marker this arc closed; no tag was paid (the
de-scaffold ruling, 2026-08-25). ADR-0180's four sites are done, and with them
the generate-quadratic program's site work; the one row left on
`roadmap.md#performance-residual-sites` is check's fourteen `engine/replay`
calls, which R-order puts last.

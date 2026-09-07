# 2026-09-07 — ADR-0180 site 5: `decide :merge` rides the `:eligible-index`

Site 5 of the generate-quadratic program, the first of the three the
2026-09-06 addendum chartered — `roadmap.md#performance-residual-sites`
PRIORITY 1. Ceremony mode: R30 (commit and push at each checkpoint), taken from
the prompt. Prompt archived at
[`../prompts/2026-09-07-adr-0180-site-5-eligible-index.md`](../prompts/2026-09-07-adr-0180-site-5-eligible-index.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, its
`Addendum, 2026-09-06` sections 5, the `:eligible-index` concern, R-hash-order
and R-already-merged. No ADR was written and none was owed — this session
ENACTS a charter that was already ruled, and like site 4 it was commissioned to
make no correction to it, so ADR-0180's own text is untouched.

Rulings in force: **R-fold-carrier**, **R-equivalence**, **R-no-second-path**,
**R-membership-from-post-state**, **R-order-2**, **R-hash-order**,
**R-already-merged**, **R-empty-carrier** and **R-read-throws** (the last two
new with this prompt), **R-agents-sentence** (rider 1),
**R-roadmap-compact** (rider 2), **R-move-not-improve**, **R-pins**, **R-edit**,
**R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked files,
local HEAD `16f825b7` equal to `origin/main`. Session start and every bracket
baseline is that sha.

The instrument's own zero ran before any edit: `bin/ground-truth-bracket
16f825b7 16f825b7` reported IDENTICAL on all 38 digested roots (3 skipped, no
`:ground-truth` key).

Both baseline cells reproduced the digests sites 1 to 4 recorded, which is what
makes this session's before-column the same corpus as theirs: `a7500-persons`
sha256 `3018299a…0d3bd3` at 110.95 s, `a2500-nopersons` `c22d6573…a55208` at
26.97 s.

## 1. What landed

**`24ed60a1` — `engine: :eligible-index concern at the fold, with its
from-scratch law`.** The carrier and its gate; `decide` UNTOUCHED.
`fold/apply-events` gains a seventeenth concern, `:eligible-index` — ONE sub-map
of `:patients`, `patient-id -> bed-swap-eligible?` over the merge-eligible
patients, from which TWO views are read. Five new public vars in `fold`:
`empty-eligible-index`, `eligible-entry`, `update-eligible`, `merge-eligible`,
`swap-eligible`. `run`'s `init-world` seeds `:eligible-index`.
`ehrt.sim-engine.eligible-index-test` is new and carries the law;
`apply-projection-test` transcribes the widened closure.

**`b8c8ff2d` — `engine: decide :merge reads :eligible-index; already-merged?
retired as redundant`.** The repoint, plus rider 1. Two whole-collection scans
left `decide :merge`'s `let`: `eligible` became `fold/merge-eligible`, and
`already-merged?` was DELETED with no fallback. `decide :bed-swap` untouched.
Both scripted-test `fold-events` helpers gained `:eligible-index` and both
`world-of` builders gained the seed. AGENTS.md's determinism sentence stopped
overclaiming.

**`c83ea961` — `engine: pin the swap view's :location term`.** Co-landing, and
the session's own finding; section 4(a).

**`09f51c47` — `plans: site 5 measured; P1 compacted`.** `measurements.md`
gains a dated site-5 section; `profile-cell.sh` gains an `update-eligible`
concern row; the roadmap P1 row collapses per rider 2.

## 2. The measurement

Both cells reproduced their pre-change log BYTE-FOR-BYTE — the same two digests
as in section 0 — and `bin/ground-truth-bracket` reported IDENTICAL on all 38
digested roots at the clean tip, at `24ed60a1` and at `b8c8ff2d`.

| cell | before | after | delta |
|---|---|---|---|
| `a7500-persons` | 110.95 s | **96.98 s** | -13.97 s, **-12.6%** |
| `a2500-nopersons` | 26.97 s | **24.74 s** | -2.23 s, **-8.3%** |

Peak RSS FELL at both generate cells (2,353 -> 2,010 MB; 969 -> 933 MB), which
is the first ADR-0180 site where a fall is the expected direction rather than a
puzzle: the repoint deletes a per-merge allocation as well as a per-merge scan.
The check column did not move beyond run-to-run spread, which is
`replay-projection`'s own decline showing up in the instrument.

JFR at 7,500 arrivals, inclusive share, `--stack-depth 2048`, **and it is a
MATCHED PAIR** — the before half recorded from a worktree at `16f825b7` with the
same driver and the same cell, so the two columns are one scale rather than two:
`decide :merge` **12.40% -> 0.43%**, the whole `decide` dispatch 37.30% ->
28.46%, and `:eligible-index`'s own cost 0.21% of the phase. **Site 5 traded
12.40 points for 0.21.** Allocation from the same recordings: `decide :merge`
5.10 GB (8.51%) -> 0.14 GB (0.26%), phase total 59.9 -> 54.1 GB.

**AND THE TOP OF THE DECADE.** One timed `a22500-nopersons` generate: **396.18 s
-> 250.62 s**, and **1,472.56 s -> 250.62 s** across the whole program, 5.9x —
over a log BYTE-IDENTICAL to the committed 2026-09-05 digest, `7d105743…78e0a`
over the same 174,866,696 bytes and 431,677 events.

## 3. Judgment calls

**(a) `eligible-entry` returns a BOOLEAN, not a patient state, and the sub-map
is `patient-id -> bed-swap-eligible?`.** The addendum says "one sub-map of
`(:patients w-next)`"; a sub-map whose VALUES were patient states would carry
the same keys and answer the same two views, at the cost of a write on every
state change rather than on every membership-or-flag change. The boolean is the
smallest value that serves both views, and the order argument is a statement
about the KEY SET, which values cannot disturb. What the choice costs is
extensibility: a third view wanting some other patient field would need the
value widened, and that is a visible edit rather than a silent one.

**(b) `eligible` is still bound EAGERLY, even on the `:with` path that never
reads it.** The scan was eager and the index read is eager, which is
R-move-not-improve applied to the shape and not only to the text. It has a
consequence the session had to pay for: a `:with`-named merge against a world
carrying no index now THROWS where it used to compute an unused vector, so the
two scripted-test `world-of` builders had to seed. Making the read lazy would
have hidden a missing seed instead of surfacing it, which is the opposite of
what R-read-throws is for.

**(c) The read is RAW and a missing index THROWS, and unlike site 4 the failure
mode did NOT need a special argument.** Site 4 had to argue that nil was a wrong
answer rather than an absent one. Here an absent index read as `[]` is a LEGAL
answer — an empty candidate list — that turns every merge into a
`:step-rejected` and moves every draw after it, which is the same failure at one
remove. The throw is `ex-info` with the view named, so the two views' messages
are distinguishable.

**(d) The seed is a named var, `fold/empty-eligible-index`, and not `{}` at any
of its four sites.** R-empty-carrier is the reason and the code says so in all
four places (`update-eligible`'s `or`, `run`'s `init-world`, and the two test
`world-of` builders). A literal that "looks right" is exactly what this hazard
rewards — `{}` is a `PersistentArrayMap`, iterates in insertion order below nine
entries, and would move the FIRST merge of every run.

**(e) The bed-swap view is built and PROVEN this session, though nothing reads
it until site 6.** The addendum asks for one sub-map serving both, and the
alternative — build the merge view now, widen it at site 6 — would make site 6
re-open the hash-order argument for a structure that had already shipped. The
cost is disclosed rather than absorbed: until site 6, `decide :bed-swap`'s
inline scan is a second implementation of `swap-eligible`'s answer, which is the
one place this concern does not meet R-no-second-path. `fold/swap-eligible`'s
own docstring says so.

**(f) Rider 1 came in a REWRAPPED bullet, because the ruled sentence would
otherwise have broken a budget.** AGENTS.md is in all five reading sets and
`:docs` stood at headroom **0**; the ruled text is two lines longer than what it
replaces, so wrapped naively it put `:docs` at **-2**, which is
`rulings.md#R-budget-stop`. The whole "Discipline inherited from sim" bullet is
rewrapped in the same edit — 10 lines in, 10 lines out — so the sentence pays
for itself and every set returns to its prior headroom. The ruled words are
unchanged.

**(g) The four site-1..4 clauses were collapsed to a POINTER, not to a shorter
list.** R-roadmap-compact asks for one pointer line; what actually made the row
net-negative was deleting the "THAT LAST SENTENCE IS NOW WRONG and site 5 is
underpriced" meta-paragraph, which existed only to correct a sentence this
rewrite removes. -7 lines, and `:onboarding` headroom went **8 -> 15** rather
than merely not falling.

**(h) The pinned `:location` case is its OWN commit rather than part of the
docs commit.** It is a test change found by a measurement step, and the docs
commit stays docs-only — which is also what lets its `make test` be the gate for
the code rather than for prose.

## 4. Findings

**(a) THE LAW WAS BLIND TO ONE OF ITS OWN CLAUSES, and only the negative control
said so.** Dropping the `(some? (:location p))` term from the swap view's flag
failed **zero** assertions against the generated property. The reason is
structural: no generated log reaches an `:admitted` patient without a location —
`evolve :admission`, `:transfer` and `:bed-swap` all write one, and
`:discharge` clears the location and the status together. That makes the term
unfalsifiable BY CORPUS, not unnecessary: the index is specified to equal the
DEFINITION on every world, hand-built ones included, and the definition tests
both terms. `c83ea961` constructs the world; the mutation now fails 3. This is
`cancel_index_test`'s participant-filing finding one site on, and it is the
second consecutive session where the corpus was non-vacuous overall and vacuous
for one clause.

**(b) THE NON-VACUITY GATE CAUGHT ITS OWN FIXTURE, as it did at site 4.** At
three licensed Renal beds the `:bed-cycle` half of the corpus exhausts inside 61
events and the carrier tops out at EIGHT members — measured at 20, 30, 40 and 60
arrivals, all the same — so the property never once exercised the hash-map side
of the nine-entry `PersistentArrayMap` boundary that R-empty-carrier is about.
Six beds reaches twelve. The fixture's docstring carries the measurement rather
than the conclusion.

**(c) THE `{}`-CARRIER MUTATION IS WEAKER THAN IT LOOKS, and the record says so
rather than letting a 1 read as a 7.** It substitutes `{}` only where the index
is ABSENT, and the trace seeds it, so the generated property cannot see that
mutation at all — the single failure it produces is the dedicated
`the-carrier-is-a-hash-map-from-empty`, which folds one batch under each seed and
compares the two answers directly. A property that seeded its own trace from
`{}` would be testing a world `run` never builds.

**(d) THE GENSYM `defmethod` FRAMES ARE ATTRIBUTABLE MECHANICALLY, which the
post-program session did by hand for two of them.** A `defmethod`'s compiled
class carries the eval ordinal of the top-level form that produced it; that
ordinal shifts with the classpath, but the GAPS between one file's methods are
fixed by that file — so a single offset maps a `clojure -M:dev` resolution of all
32 `decide` methods onto a whole recording. Both recordings resolved at **+2999**
independently and matched 21 and 24 of the 32 (the rest have zero samples), which
is the attribution checking itself. The script is session scratch, not committed;
what is committed is the pair of reports it read and the figures it produced.

**(e) `decide :bed-swap` IS NOW THE RECORDING'S TOP PROJECT FRAME**, at 12.00%
inclusive and 13.82% of allocation, on code this session did not touch. Its
absolute sample count FELL with the phase (791 -> 745); the share rose because
the phase got shorter. That is site 6 sitting in the instrument, and its view is
already built and proven.

**(f) THE COLLISION HOLE IS PINNED WHERE EVERY OTHER GATE IS BLIND.** The
addendum measured the first colliding patient-id pair of the shipped seed at
45,000 arrivals; the largest committed cell is 22,500, so the bracket, the oracle
and all three timed cells cannot see it. `at-a-hasheq-collision-the-two-orders-
diverge` builds that pair, asserts the two ids really do share a `hasheq`, and
pins what each side answers — the definition in registration order, the index in
eligibility order. The test DOCUMENTS the divergence; it does not assert the two
agree, because they do not.

## 5. The law, and that it is not vacuous

`ehrt.sim-engine.eligible-index-test` keeps `decide :merge`'s `eligible`
construction VERBATIM as `naive-merge-eligible` (a MOVE — after the repoint `src`
holds no second implementation), `decide :bed-swap`'s as `naive-swap-eligible`
(a COPY until site 6) and `already-merged?` as `naive-already-merged?`, and
asserts at EVERY replay entry of churn-bearing generated logs that both views
agree — vectors, ORDER included — for every patient the world holds plus one it
does not. The projection is `#{:patient-bootstrap :patient-state :log-mirror
:eligible-index}`, and the trace seeds `:patients` as a `PersistentHashMap` of
the whole population the way `init-world` does, because the equality being
asserted is between two HASH orders and a bootstrap-grown parent would be an
array-map for its first eight members.

Red→green, run in-process against the same law by rebinding the shipped
functions and re-running the namespace:

| mutation | failures |
|---|---|
| shipped | **0** |
| carrier grown from `{}` (array-map) | 1 |
| membership from the event, never evicts | 7 |
| merge view SORTED | 6 |
| swap flag ignores `:location` | 0 → **3** |
| restored | **0** |

The sorted-view row is R-hash-order's deferred oracle change run as a mutation:
the law convicts it, which is what makes "preserve the order" a checked claim
rather than an intention.

Non-vacuity, over two pinned cases (20 patients, churn 0.4): the corpus reaches
indexes of more than one member (twelve at the widest), reaches eviction as well
as admission, reaches a swap view that is non-empty and STRICTLY NARROWER than
its merge view, reaches carriers on both sides of the nine-entry boundary, and
reaches real `:merge` events — the last being what makes the R-already-merged
implication (`the-log-scan-and-the-world-status-are-the-same-answer`) decide
something rather than compare two constant falses.

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `24ed60a1` | 424 | 27,935 + 106 = 28,041 | 0 | 0 |
| `b8c8ff2d` | 424 | 28,041 | 0 | 0 |
| `c83ea961` | 424 | 28,057 | 0 | 0 |
| docs tip (over `09f51c47`) | 424 | 28,057 | 0 | 0 |

The baseline at `16f825b7` was 422 lines and 27,935 passes (site 4's record,
section 6). The +2 lines are the one new namespace counted once per project it
belongs to; the +106 passes at `24ed60a1` are its own assertions plus
`apply-projection-test`'s three new negative ones, and the +16 at `c83ea961` are
the pinned `:location` case. **NOTHING MOVED AT THE REPOINT** — `24ed60a1` and
`b8c8ff2d` read identically — which is what an output-identical refactor should
look like from here.

`:onboarding` reading-set headroom went **8 -> 15** against an unchanged 1,530
budget, on the roadmap edit's net -7 lines; AGENTS.md's rider is net zero by
construction (section 3(f)) and `:docs` returns to its standing headroom of 0.

## 7. Background processes

Harness-tracked background jobs, each of which exited on its own with its exit
code recorded: three `bin/ground-truth-bracket` runs (the instrument's own zero
at the clean tip, then one per CODE commit — `c83ea961` and `09f51c47` owed
none, being test- and docs-only), two timed cell-pair runs (before, after), one
combined JFR-plus-`a22500` run, one baseline JFR run in a worktree, and four
`make test` runs. Everything else — `bin/preflight`, three `make state-derived`
runs, the profile re-aggregations, the `jfr print` extractions and several
throwaway `clojure -M:dev:test` probes (namespace smoke runs, the fixture-size
probes, the method resolver, the negative control) — ran in the foreground.

**ONE `sleep`, not seventy-two, and the distinction is R-sentinel's own.** Site
4's finding was 72 background `sleep` waiters, one per poll, each waking the
session into another poll so the count grew while the wall did not shrink. Every
wait for work in this session was a harness completion notification instead. The
single exception is the CI wait at close, which is one background job that polls
`gh run view` on an internal interval and notifies ONCE when the run reaches
`completed` — the shape the harness documents for a single-notification wait, and
the shape site 4's record was reaching for. Recorded as one rather than claimed
as zero. One `find /` probe ran long and was stopped with `TaskStop` rather than
waited out. At close, `ps` reports no `java`, no `make` and no `sleep` belonging
to this session, and `git worktree list` shows only the main clone — the
baseline-profile worktree was removed as soon as its recording was read.

**AND ONE THING THE PREVIOUS SESSION'S FINDING PREDICTED.** Every suite figure
in section 6 is taken from an `EXIT=` line the command wrote itself, never from
a completion notification; the notifications were used only to decide when to
look.

## 8. HEAD landed

The measurement commit `09f51c47` is the last payload commit; this record and its
prompt archive land on top of it, and a close-marker commit follows once CI is
verified green with `gh run view`. Baseline for every bracket in this session was
`16f825b7`.

## 9. CI

All five commits went out in ONE push, so GitHub Actions ran once, at the tip.
Verified with `gh run view` rather than assumed:

| commit | run | conclusion |
|---|---|---|
| `9d93260c` (tip, covering `24ed60a1`, `b8c8ff2d`, `c83ea961`, `09f51c47`) | 34098328708 | **success** |

`bin/post-push-verify` ran immediately after the push: remote tip matches HEAD,
every commit message in `16f825b7..9d93260c` is pure ASCII, and the CI run was
reported once rather than awaited (AR-CI-4) — this section is where it was
awaited, by polling `status`/`conclusion` to `completed success` rather than by
trusting a watcher's exit code, which is site 4's own finding applied rather than
rediscovered. `gitleaks` scanned 1,498 commits and 44.19 MB at the push hook and
found no leaks.

CI green at the tip is the marker site 5 closed. The generate-quadratic program
has TWO sites left, both chartered and sequenced: `decide :bed-swap` (site 6),
which reads the second view of the sub-map this session built and proved and is
now the 7,500 recording's top project frame at 12.00%, and the `check-all`
replays (site 7).

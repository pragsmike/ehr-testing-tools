# 2026-09-06 — ADR-0180 site 1: `waiting-boarder` rides the fold

Site 1 of the generate-quadratic program, `roadmap.md#performance-residual-sites`
PRIORITY 1. Ceremony mode: R30 (commit and push at each checkpoint), taken
from the prompt. Prompt archived at
[`../prompts/2026-09-06-adr-0180-site-1-waiting-boarder.md`](../prompts/2026-09-06-adr-0180-site-1-waiting-boarder.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, section
`1. waiting-boarder`. No ADR was written and none was owed — this session
ENACTS a charter that was already ruled, and the de-scaffold ruling reserves
an ADR for a payload-behaviour or contract decision.

Rulings in force: **R-fold-carrier**, **R-equivalence**, **R-order**,
**R-membership-from-post-state**, **R-move-not-improve**, **R-pins**,
**R-edit**, **R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main`
all green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`),
`core.fileMode` true, `core.ignorecase` unset, working tree clean including
untracked files, local HEAD `bba3a63c` equal to `origin/main`. Session start
and every bracket baseline is that sha.

## 1. What landed

**`c424e373` — `engine: :boarder-index concern at the fold, with its from-scratch law`.**
The carrier and its gate; `decide` untouched, shipped behaviour unchanged.
`fold/apply-events` gains a fourteenth concern, `:boarder-index`:
`home-ward -> sorted-set of [admitted-at patient-id]`, maintained off the
same pre/post participant pair the `:bed-index` concern already reads. Three
new public vars in `fold`: `boarder-entry` (membership from ONE patient
state), `update-boarders` (the per-event reconcile), `first-boarder` (the
ordered lookup). `ehrt.sim-engine.boarder-index-test` is new and carries the
law.

**`75b4a868` — `engine: waiting-boarder reads :boarder-index`.**
The repoint. `waiting-boarder`'s body becomes `fold/first-boarder`; nothing
else at either call site changes. Also fixes four scripted tests in
`engine_test.clj` that went red or vacuous (section 4).

**`e23a2789` — `plans: site 1 measured`.** `measurements.md` gains a dated
site-1 section; the P1 roadmap row's site-1 line gains the figure; the two
new JFR profile reports land in `raw/`.

## 2. The measurement

Both cells reproduced their pre-change log BYTE-FOR-BYTE —
`a7500-persons` sha256 `3018299a…0d3bd3`, `a2500-nopersons`
`c22d6573…a55208` — and `bin/ground-truth-bracket` reported IDENTICAL on
all 38 digested roots (3 skipped, no `:ground-truth` key) at the clean tip,
at `c424e373` and at `75b4a868`.

| cell | before | after | delta | corrected |
|---|---|---|---|---|
| `a7500-persons` | 256.15 s | **189.79 s** | -66.36 s, -25.9% | **-26.7%** |
| `a2500-nopersons` | 34.85 s | **30.08 s** | -4.77 s, -13.7% | **-17.8%** |

JFR at 7,500 arrivals, inclusive share, `--stack-depth 2048`:
`waiting-boarder` **25.23% -> 0.00%** — zero of 12,013 samples, and gone
from the innermost-project-frame table where it had been the top row at
12.67%. `fold/apply-events` rose 7.56% -> 9.76%, which is the index's own
cost charged where the charter said it would be: 25.2 points became 2.2.
`decide` as a whole went 62.02% -> 47.22%.

Two things the section in `measurements.md` says out loud rather than
leaving a reader to infer:

* **These walls are not the decade table's and must not be read against
  it.** The table is a two-JVM mean; these are one JVM each, this session's
  machine, this session's load. The `a7500-persons` baseline here is
  256.15 s where the committed table records 235.09 s for the same cell and
  seed. Only before-against-after within this session carries anything.
* **The three remaining sites RISE in share and none of them regressed.**
  `select-person` 14.17% -> 19.17%, `occupancy-board` 11.65% -> 12.51%,
  `last-uncancelled-index` 10.29% -> 14.68%. The denominator shrank by a
  quarter. That is what R-order predicted and the reason it sequenced them.

**The index costs memory.** Peak RSS at `a7500-persons` went 2,006 ->
2,151 MB (+7.2%); at `a2500-nopersons`, 945 -> 949 MB. A single-JVM RSS
reading against a 3.88 GB heap is a noisy instrument, so the claim made is
directional — the index costs memory in the direction and rough magnitude
expected — not that it costs exactly 145 MB.

## 3. Judgment calls

**(a) Sites 2 and 3 do not opt into `:boarder-index`, and the two absences
are asserted.** ADR-0180's consequences section states the contract: "Each
new index is guarded by its own membership test and by nothing else — the
contract that keeps a site which does not opt in paying nothing for one it
never reads." `replay` returns entries and `reinstated-state` returns a
patient state; a boarder index is in neither, and `replay` is 50.97% of the
check phase, so opting them in would have charged the check phase for a
generate-side index. One defensible reading, so not a stop.
`apply-projection-test` now transcribes TWO closures — the
apply-unification arc's own thirteen, whose 38-of-39 arithmetic is unmoved,
and that set plus the fourteenth, 39 of 42 — and asserts the two absences
positively, so a later session that "completes" either column has undone a
decision rather than finished an arc.
`.agents/plans/apply-unification-census.md` carries a dated addendum saying
the closure grew past it, which is what that gate's own docstring asks for.

**(b) `ADR-0180` itself was NOT edited.** Its sequencing list still reads
"five sessions that have not run". That is a statement about when the
charter was written, not live status; the roadmap row is the register that
carries what has landed, and it does. Named here so the choice is visible
rather than an omission.

**(c) The roadmap addition costs eight lines of `:onboarding` headroom**,
34 -> 26 against an unchanged 1,530 budget. Within budget, ratchet not
touched, but the next session should know the number is 26.

## 4. A finding, fixed in the same commit because it went red

Four scripted tests in `engine_test.clj` hand-rolled the run loop's fold —
`update-in [:patients pid] evolve/evolve` plus a `:ground-truth` append —
under docstrings claiming they did what the loop does. That was true until
the loop gained a fourteenth concern.

* `bed-ready-transfer-scripted-two-patients` — **red**, 8 failures and an
  uncaught `evolve` dispatch on nil.
* `bed-ready-transfer-obeys-the-allocation-ladder` (through the `advance`
  helper) — **red**, 5 failures.
* `expired-disposition-discharge-suppresses-the-bed-ready-transfer-coupling`
  — **passing VACUOUSLY**, and this is the one worth the finding. It asserts
  that no `:transfer` is emitted. A world with no index answers nil, so it
  would have passed with the suppression broken.
* `fold-events`, the shared helper the last of those and a dozen other
  scripted tests use.

All four now call `fold/apply-events` with a declared projection
(`#{:patient-bootstrap :patient-state :boarder-index :log-mirror}`), which
is the one definition they cannot drift from. **The lesson is not about
this index**: a hand-rolled copy of the choke point is a second definition
of what applying an event means, and it will fall behind the first every
time the first grows.

One pre-existing latent defect goes with the rewrite, disclosed in the
helper's docstring rather than absorbed: the hand-rolled fold mapped over
`(:participants ev)` unfiltered, so a `:bed-status-change` — whose
participant names a BED and carries `:patient-id` nil — would have evolved a
nil-keyed phantom patient. No test there emits one, so it never fired.

**DISCLOSED, NOT FIXED.** `components/sim-emit-hl7`'s test tree carries the
same hand-rolled `fold-events`/`admit` pair
(`emit_hl7_test.clj:163-176`). It asks no boarder question, so it is a
latent shape rather than a live defect, and it is outside this session's
fence.

## 5. The law, and that it is not vacuous

`ehrt.sim-engine.boarder-index-test` keeps `waiting-boarder`'s scan body
verbatim as `naive-waiting-boarder` (R-move-not-improve) and asserts
`(= naive index)` at EVERY intermediate world of a churn-bearing generated
log, for every ward in play plus one that exists nowhere, under three
exclusions: nil, the ward's own current head, and an absent id.
`:bed-cycle` is driven both ways because the two callers are mutually
exclusive in practice — `decide :discharge` asks only when `(:beds world)`
is nil, `decide :bed-ready` exists only when it is not.

A separate test counts the shapes the corpus actually reaches, because
`(= nil nil)` at every entry would pass forever. On its two pinned cases:

| | bed-cycle off | bed-cycle on |
|---|---|---|
| worlds | 48 | 31 |
| world x ward triples | 94 | 60 |
| non-nil answers | 31 | 23 |
| excluding the head gives a DIFFERENT id | 20 | 14 |
| excluding the head gives nil | 11 | 9 |

A third test shows `decide :bed-ready`'s `world'` argument irrelevant rather
than arguing it: real folded worlds, that call site's own
`(assoc-in world [:beds bed :status] :ready)`, and `:patients`,
`:boarder-index` and both functions' answers all unmoved.

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `c424e373` | 416 | 27,705 | 0 | 0 |
| `75b4a868` | 416 | 27,753 | 0 | 0 |
| `e23a2789` | 416 | 27,753 | 0 | 0 |

The +48 at `75b4a868` is that commit's own new assertions. Line count is
unchanged at 416 because the one new namespace landed with `c424e373`,
whose own +1 is against `bba3a63c`'s 415.

## 7. Background processes

Ten harness-tracked background jobs, each of which exited on its own with
its exit code recorded: two timed cell runs (before, after), three
`bin/ground-truth-bracket` runs (the instrument's own zero, then one per
code commit), three `make test` runs, one `clojure -M:poly test
brick:sim-engine`, one JFR profile run. Everything else — `bin/preflight`,
two `make docsgen` runs, two further brick runs, one throwaway
`clojure -M:dev:test` script that measured the non-vacuity counts in
section 5 — ran in the foreground. None was left running at close.

## 8. HEAD landed

`e23a2789` is the last payload commit; this record and its prompt archive
land on top of it, and a close-marker commit follows once CI is verified
green with `gh run view`. Baseline for every bracket in this session was
`bba3a63c`.

## 9. CI

All four commits went out in ONE push, so GitHub Actions ran once, at
the tip. Verified with `gh run view` rather than assumed:

| commit | run | conclusion |
|---|---|---|
| `69459421` (tip, covering `c424e373`, `75b4a868`, `e23a2789`) | 34035159696 | **success** |

`bin/post-push-verify` ran immediately after the push: remote tip matches
HEAD, every commit message in `bba3a63c..69459421` is pure ASCII, and the
CI run was reported once rather than awaited (AR-CI-4) — this section is
where it was awaited.

CI green at the tip is the marker this arc closed; no tag was paid (the
de-scaffold ruling, 2026-08-25).

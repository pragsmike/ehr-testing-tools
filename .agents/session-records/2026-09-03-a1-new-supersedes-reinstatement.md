# 2026-09-03 — A1: `:new` supersedes a cancel-transfer reinstatement

**Ceremony:** R30 standing default, with the prompt's own sequencing:
commits at the named steps, push at step 8, CI verified with
`gh run view`, close-marker commit after.
**Fences:** no change under `components/sim-check/src` or
`components/sim-engine/src/ehrt/sim_engine/churn.clj`;
`dense-7500-nobed.edn` is not in the tree and is NOT reconstructed —
the `nobed` 10^5 cell is recorded as unreproducible (section below).
**Driving prompt:** [`.agents/prompts/2026-09-03-a1-new-supersedes-reinstatement.md`](../prompts/2026-09-03-a1-new-supersedes-reinstatement.md).
**Predecessors:** [`2026-09-02-downstream-self-check-failed.md`](2026-09-02-downstream-self-check-failed.md)
(the STOP record that measured the mechanism),
[`2026-09-03-b2-b1-stale-hold.md`](2026-09-03-b2-b1-stale-hold.md)
(the catalog half — B2 convicts what this session prevents),
[`2026-08-29-ts-5-superseded-cancel.md`](2026-08-29-ts-5-superseded-cancel.md)
(the session that wrote the guard and declared the `:new` exclusion).

## Headline

**The calibration fixture is green at every arrival count, and the
downstream's own hashes prove the fix touched nothing they measured.**
`subject-superseded?`'s superseding set is per-kind (ADR-0177,
R-A1-scope): `:new` supersedes a `:cancel-transfer` and deliberately
not a `:cancel-discharge`, so the same-batch
`:cancel-admit`+`:cancel-transfer` shape that left a `:status :new`
patient holding a bed forever is now refused at decide time —
`--patients` 500, 1000, 1984 and 2000 all exit 0, the 500/1000 outputs
SHA-256-identical to the downstream's published values, M6 Task 2
green throughout, and both sweep instruments IDENTICAL vs `c16bb26`
with no declaration owed.

## What landed

| commit | contents |
|---|---|
| `a7ec6f1` | step 2 — ADR-0177, `notes/ADRs.md` regenerated |
| `193a54e` | step 3 — RED: `a-cancel-transfer-may-not-reinstate-a-new-subject` |
| `e8016e7` | step 4 — GREEN: per-kind table in `log_index.clj`; boundary pin rewritten |
| `38341c2` | step 6 — reason docs carry the `:new` case; sweep recorded |
| *(this)* | step 7 — record, prompt archive, roadmap rotation, regenerated indexes |

## Step 1 — the derivation, before code

### (a) The churn division of labour, cited

`churn.clj:126-140` states it in the applicability oracle's own
docstring: the oracle is **STATIC** — "it folds step TYPES over a
pathway and has no view of the live world at the instant a step
finally runs" — "so it can and does insert a :cancel-transfer at a gap
AFTER the pathway's own :discharge, where the reinstatement it
authorises would land on a patient who has already left. That is not a
defect in this oracle and is not fixable here: the same insertion is
legal or not depending on runtime state this namespace cannot see. It
is caught where the state exists, by
`ehrt.sim-engine.log-index/subject-superseded?` at decide time … and
shows up in the log as an :illegal-cancel-*-subject-superseded
rejection." The static half inserts; the decide-time guard rejects.
A1 is therefore a change to the GUARD (`log_index.clj`) and to nothing
in `churn.clj` — the oracle's insertion of a `:cancel-transfer` behind
a `:cancel-admit` stays legal to *attempt*, and the decide-time guard
is the one place the subject's live `:status :new` exists to be asked.

### (b) Plain `:new`-in-set rejects M6's cancel-discharge — the trace

The naive fix the roadmap row's own wording suggests ("admit `:new` to
`log-index/statuses-that-supersede-a-reinstatement`") breaks M6 Task 2.
Traced conjunct by conjunct against
`engine-test/cancel-discharge-restores-class-even-after-a-preceding-cancel-admit-stripped-it`
(`engine_test.clj:649-669`):

That test's `world3` is built admit(t=0, Renal) → `:discharge`(t=10) →
`:cancel-admit`(t=20). After the cancel-admit, P1 carries
`:status :new` with `:class` stripped (the test's own sanity assertion
at `:666`). The test then requires the `:cancel-discharge` at t=30 to
be **applied**, restoring `:class :inpatient` (`:669`).

The guard (`log_index.clj:254-257`) is two conjuncts:

    (and (statuses-that-supersede-a-reinstatement status)
         (not= status (status-a-cancel-target-leaves kind)))

With `:new` admitted to the plain set `#{:discharged :expired :merged}`
and `world3`'s patient under `kind :cancel-discharge`:

1. `(statuses-that-supersede-a-reinstatement :new)` → `:new` is now a
   member → **truthy**.
2. `(not= :new (status-a-cancel-target-leaves :cancel-discharge))` =
   `(not= :new :discharged)` (`log_index.clj:198-199`) → **true**.

Both conjuncts hold, so `decide :cancel-discharge`'s first `cond`
branch (`decide.clj:1697-1699`) fires
`:illegal-cancel-discharge-subject-superseded`, `world4` never sees a
`:cancel-discharge` event, and `:669`'s `(= :inpatient (:class …))`
fails. M6 Task 2 goes red — hence **R-A1-scope**: the supersession
must be KIND-AWARE. `:new` supersedes a `:cancel-transfer`
reinstatement (the subject's very presence in the record has been
corrected away, so putting a bed back on them re-creates the state B2
convicts) and does NOT supersede a `:cancel-discharge` (the
record-correction reading M6 Task 2 rests on: a cancel-discharge onto
a `:new` subject reinstates `:admitted` + bed + class, coherently).

**Gate (step 1): both traced to line.** Done, no commit.

## Step 2 — ADR-0177 (`a7ec6f1`)

`notes/adr/0177-new-supersedes-a-cancel-transfer-reinstatement.md`:
the decision per R-A1-scope (quoted verbatim), the coherence argument
(a cancel-discharge onto `:new` reinstates `:admitted` + bed +
`:class` as a whole, M6; a cancel-transfer onto `:new` reinstates only
location/home-ward, B2's convicted state), the payload effect (such
cancel-transfers become `:step-rejected`
`:illegal-cancel-transfer-subject-superseded`, decide's `:rejected`
map carrying `{:status :new}`; no event-schema change and no bump —
both reasons entered the enum at 1.8.0 and the status detail rides the
return value, never the event), and the TS-5 exclusion superseded with
its docstring paragraph cited and its history kept. `notes/ADRs.md`
regenerated by `make adr-index`. Gate: link-footnote + adr-index
suites green (13 tests, 196 assertions, 0 failures).

## Step 3 — RED (`193a54e`)

`a-cancel-transfer-may-not-reinstate-a-new-subject`, M6-harness style:
admit(0, Renal) → transfer(10, ED) → discharge(20) → cancel-admit(30)
— sanity: `:status :new`, no bed — → cancel-transfer(40) must be
rejected `:illegal-cancel-transfer-subject-superseded` with
`{:status :new}` and the bed NOT reinstated. Red against the unfixed
guard: **6 failing assertions, all inside the new deftest** (110
tests, 718 assertions, 6 failures, 0 errors — everything else green,
M6 at `:649` untouched and green). Gate: exactly the new test red.

## Step 4 — GREEN (`e8016e7`)

`statuses-that-supersede-a-reinstatement` becomes a per-kind table —
`{:cancel-transfer #{:new :discharged :expired :merged},
:cancel-discharge #{:discharged :expired :merged}}` — and
`subject-superseded?` looks the set up by `kind`; the comparison shape
is otherwise unchanged. The "deliberately ABSENT" paragraph is
rewritten to record the reversal with the history kept (measured
2026-08-29, reached 2026-09-02, reversed for `:cancel-transfer` alone
by ADR-0177; the exclusion stands for `:cancel-discharge`, which is
why it is a table and not one set grown by a member). `decide.clj`
untouched — both call sites already pass `kind`, and the TS-5
subject-before-bed branch order stands.

The TS-5 boundary pin
`a-cancel-transfer-against-a-cancel-admitted-subject-is-still-applied`
was **witnessed red under the new guard before being touched** —
exactly 1 failing test in the namespace run — then rewritten as
`...-is-rejected`, the decision its own docstring demanded ("pinned so
that widening it is a decision and not a drift"), keeping its
no-discharge world (admit → transfer → cancel-admit → cancel-transfer)
as the second witness shape. Old name now cited only in frozen
history.

Gate: sim-engine + sim-check bricks green in all three projects
(conformance / ehrt-cli / integration; engine-test 110 tests, 724
assertions; check-test 140 passes; 0 failures, 0 errors anywhere), M6
included.

## Step 5 — the witness, at a real shell

Fixture identity verified first: `config.edn` sha256 `4dd4a5c0…f07e02`
matches `PROVENANCE.md`. Then at tip `e8016e7`, seed 424242,
`--reference-date 2026-08-31 --churn --format ground-truth`:

| run | exit | output |
|---|---|---|
| `--patients 500` | **0** | 9,751,861 bytes, sha256 `434232a9…4a03d5` — **matches the downstream's value exactly** |
| `--patients 1000` | **0** | 11,966,511 bytes, sha256 `ddcfc319…2deb11` — **matches the downstream's value exactly** |
| `--patients 1984` | **0** | 16,519,754 bytes (formerly exit 2, 39,684 B2 rows) |
| `--patients 2000` | **0** | 16,602,822 bytes (formerly the downstream's `:self-check-failed` and B2's 34,600 + 2 rows) |
| ed-tuesday (`--seed 202 --patients 100 --churn`) | **0** | see the byte-count note in Findings |

**Match, not move**, on both hash cells: no `:new` reinstatement
occurred in the 500- or 1000-arrival runs, so the fix changes nothing
the downstream measured at the sizes that completed for them — while
the two arrival counts that failed now run clean, because the only
difference the guard makes is refusing the reinstatement whose stale
hold B2 was convicting. The PROVENANCE JDK caveat (their 17, this
machine's 21) turned out not to matter: the hashes agree anyway.
Gate: four exit codes, all 0.

## Step 6 — the sweep, both halves, and the reason docs

`bin/regression-oracle c16bb26 e8016e7`: **IDENTICAL — every root's
digest matches** (its own closing line, exit 0; soundness check on
`digest.clj` passed, "no" declared change). `bin/ground-truth-bracket
c16bb26 e8016e7`: **IDENTICAL across all 38 digested roots'
`:ground-truth`** (exit 0; 3 skipped for carrying no `:ground-truth`
key — `appendicitis.edn`, `ear-infections.edn`, `sore-throat.edn`, the
same three as at TS-5; not a regression-oracle claim, per its own
banner). **The declared sweep is UNSPENT** — no root moved, so no
declaration was owed, the TS-5 precedent exactly. The two gated
ed-tuesday roots are among the 38, which also settles the byte-count
question in Findings. `make docsgen` regenerated `demos/traces/**` and
**no trace moved** — A1 changes no demo corpus.

Reason docs: `docs/formats.md`'s `:step-rejected` section is inside
the generated event-log block and the reason enum did not change, so
the one hand-written site is `components/sim/docs/patient-state-model.md`'s
`:cancel-*` validity row — it gains the `:new` case (extended
2026-09-03, A1, ADR-0177: supersedes `:cancel-transfer`, deliberately
not `:cancel-discharge`) in both its legality cell and its
illegal-example cell.

Gate: full `make test` green over the whole tree — exit 0, 414 "Test
results" lines, every one `0 failures, 0 errors`, run only after the
tree had settled (judgment call 4).

## Step 7 — record, roadmap rotation, indexes

The roadmap row rotates to
`- CLOSED 2026-09-03 ADR-0177 **[cancel-transfer-reinstates-a-new-subject]**`
at the top of the retired-`## Done` list, citing the trace that
refuted the row's own proposed one-set fix, the witness table, both
sweep verdicts, and the unreproducible cell. `## Next` keeps
PRIORITY 1 and 3 (ascending, unique — the lint's requirement; it does
not require contiguity). Indexes regenerated (`make docsgen`):
session-records INDEX, prompts INDEX, `state-derived.md`,
`notes/ADRs.md` (step 2).

## Judgment calls, and their ratification status

1. **The boundary pin's rewrite rode the GREEN commit** (step 4), not
   a separate test commit: the prompt named only the new RED test, but
   `a-cancel-transfer-against-a-cancel-admitted-subject-is-still-applied`
   pinned the exact behaviour being reversed and could not stay green
   past the src change. Witnessed red first (exactly 1 failing test),
   rewritten as `...-is-rejected` in the same commit as the reversal
   it pins, disclosed in that commit's message. Its docstring's own
   "widening it is a decision and not a drift" is the licence read.
2. **The per-kind MAP, not a set plus a special case** — "extend the
   table's asymmetry" read as: make the superseding statuses a
   two-entry table exactly like `status-a-cancel-target-leaves` above
   it, so the asymmetry lives in data both times. The
   `:cancel-discharge` entry keeps `:discharged` (excluded by the
   second conjunct as before) so the entry reads as "the same set as
   ever" and the diff is exactly one member in one entry.
3. **ed-tuesday was first run without `--format ground-truth`**
   (mirroring the B2 record's parenthetical, which omits the flag) and
   produced 429,551 bytes vs B2's reported 424,772 — chased rather
   than hand-waved: re-run WITH the flag produces exactly 424,772
   bytes at this tip, so B2's figure was the ground-truth EDN and the
   default-format run was a different artifact (HL7 wire), not a
   payload move. The bracket's IDENTICAL verdict on both gated
   ed-tuesday roots is the authoritative half of that argument.
4. **The suite was started once against a moving tree and its result
   discarded** — a `make test` launched in the background while this
   record and the roadmap were still being edited would have read
   mid-edit register state into the freshness gates; killed, and the
   gate's run is the one below, started only after the tree settled.
   Same shape as B2's own step-5 sequencing disclosure.

## Findings

1. **The `nobed` 10^5 cell is unreproducible, per fence.** The TS-5
   measurement that counted "2 cancel-transfers against a `:new`
   subject left alone" ran on `dense-7500-nobed.edn` (seed 20260824),
   a scratch config that is not in the tree; the fence rules it not
   reconstructed. Under ADR-0177 those 2 would be rejected — a
   statement this session makes from the mechanism (the guard asks
   only `:status` and `kind`), not from a re-measurement. The
   docstring history in `log_index.clj` says so in one clause.
2. **`check.clj:573`'s docstring sentence is now historical** — B2's
   `non-admitted-patients-hold-no-bed` docstring says the guard's
   "deliberate `:new` exclusion is precisely the seam under judgment
   here", which was true when written and is superseded by ADR-0177.
   `components/sim-check/src` is fenced this session, so the sentence
   stands as-written; one line here per the de-scaffold ruling rather
   than a roadmap row. The invariant itself is UNCHANGED and correct —
   it judges the log, and now guards against a hand-authored or
   pre-A1 log rather than anything this engine emits.
3. **The downstream's hash table doubled as a no-regression oracle.**
   Their 500/1000 SHA-256s, vendored in `PROVENANCE.md`, were computed
   on their machine at their commit — that both reproduce here at
   `e8016e7` is a cross-machine, cross-JDK, cross-88-commits
   byte-identity witness the repository's own instruments could not
   have supplied.

## Close

Final full `make test`: exit 0, 414 result lines, all
`0 failures, 0 errors`, on the settled tree with every edit of this
session in place. `clojure -M:poly check` runs green through the
pre-push hook. CI verification and post-push message verification are
appended by the close-marker commit, per the B2 precedent.

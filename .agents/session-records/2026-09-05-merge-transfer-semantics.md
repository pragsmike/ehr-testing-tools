# 2026-09-05 — merge transfer semantics: the bed released, the result carried

Reasoning-of-record: `notes/adr/0179-merge-transfer-semantics.md`.
Derivation: `.agents/plans/2026-09-05-adr-0179-merge-census.md`. Prompt
archived at
[`../prompts/2026-09-05-merge-transfer-semantics.md`](../prompts/2026-09-05-merge-transfer-semantics.md).
Ceremony mode: **R30** (commit and push at each checkpoint), taken from
the prompt. Session start `007deea6`, equal to `origin/main`.

Rulings in force: **R-bed**, **R-queue**, **R-inv**, **R-loc**,
**R-pins**, **R-edit**.

## 0. Preflight

`bin/preflight` ran first, before any git operation, and exited **0**
with no findings. Last five CI runs on `main` all green
(`007deea6`, `fb6a3442`, `4cfa570c`, `99320589`, `6fe6811e`); edit root
`/home/mg/src/ehr-testing-tools`, not under `/mnt/`; `core.fileMode`
true; `core.ignorecase` unset; working tree clean including untracked;
local HEAD equal to `origin/main`. One DISCLOSED line: HEAD is not
tagged `stable-*`, which is expected — no session pays a tag since the
de-scaffold ruling.

## 1. What landed

Nine commits, `007deea6..HEAD`.

**`ffaa923e` — `docs: ADR-0179 derivation -- oracle roots reaching :merge enumerated`.**
Step 1's counts, written before any engine edit.

**`1f4989b0` — `docs: ADR-0179 merge transfer semantics`.**
The ADR and its generated index row.

**`621a257e` — `test: ADR-0179 red -- bed release, followup re-queue, invariant widening`.**
Three tests, one per ruling, all red.

**`f79ddeea` — `engine: merge releases the bed and re-queues result followups (ADR-0179)`.**
R-bed on the `:merged` arm, `:merged-into` on `PatientState`, R-queue's
carve-out in `run.clj`'s M2b branch.

**`8a566a4c` — `check: order reference resolves through a merge (ADR-0179)`.**
R-inv, two private helpers, no new catalog row.

**`ceed1f5d` — `emit-hl7: the A40 tombstone follows the merged arm's bed release (ADR-0179)`.**
Not a step the prompt names. The full suite forced it; see §5.

**`d95593ef` — `test: downstream calibration re-measured under ADR-0179`.**
Every measured number into the ADR that predicted them.

**`8fdadb4c` — `docs: merge semantics documented`.**
The `:merge` validity row, the accumulator table, the consumer guide.

**`3e1d8346` — `test: the full-capability baseline moves, 210 -> 212 messages (ADR-0179)`.**
Its own commit per `rulings.md#R-pins`. Also not predicted; see §5.

## 2. Step 1 — the derivation, as recorded evidence

`ehrt.oracle.digest`'s 41 roots run to EDN before any edit. **38 carry a
`:ground-truth`**; three are interpreter-layer batches that carry no such
key (`appendicitis`, `ear-infections`, `sore-throat`).

**Four roots reach a `:merge`**, seven merges in all. Bed-holding read
from `engine/replay`'s `:world-before` at each merge record, not inferred
from event kinds:

| root | events | merges | absorbed `:status` | holds a bed | holds a `:home-ward` |
|---|---:|---:|---|---:|---:|
| `chatter-charges` | 477 | 2 | `:discharged` x2 | 0 | 2 |
| `demographic-fold` | 671 | 2 | `:discharged` x2 | 0 | 2 |
| `encounter-horizon` | 170 | 2 | `:admitted` x2 | **2** | 2 |
| `scheduling` | 487 | 1 | `:discharged` | 0 | 1 |

Five carry `:cause :identification`; two carry no `:cause` (churn), and
**both bed-holders are churn merges, both in `encounter-horizon`** —
which made `{encounter-horizon}` the predicted mover set.

**Result-followups queued on an absorbed patient-id: ZERO**, in all 38
roots, and also in `test-fixtures/downstream-calibration` at 500 (48
merges) and 1,000 (77 merges). `:order-placed` and `:result-available`
are equinumerous in both fixture runs — **189/189** and **392/392**.

Which is a finding worth stating on its own: **the oracle is blind to
R-queue.** An IDENTICAL bracket verdict on the follow-up half would be
vacuous, the shape `2026-09-01-event-stream-mutation.md` already recorded
for `engine/replay`. R-queue's whole proof is the hand-built run-loop
test in `621a257e` and nothing else.

## 3. Red, with the real output

`make test` **cannot show all three red at once**, and this is disclosed
rather than reported as a met gate: the suite halts at the first failing
brick. The full red run (`/tmp` log, this session) reported only test
(c)'s pair — `Test results: 148 passes, 2 failures, 0 errors.` in
`ehrt.sim-check.check-test`, inside the `conformance` project — and never
reached `sim-engine`, where (a) and (b) live. Two targeted runs captured
those.

**(a) `merge-releases-the-absorbed-patients-bed`** — `Ran 1 tests
containing 10 assertions. 5 failures, 0 errors.`

```
expected: (nil? (:location merged))
  actual: (not (nil? {:ward "Renal", :bed "RENAL-H01", :placement :surge}))
expected: (nil? (:home-ward merged))
  actual: (not (nil? "Renal"))
expected: (not (contains? merged :location))
  actual: (not (not true))
expected: (not (contains? merged :home-ward))
  actual: (not (not true))
expected: (= "P1" (:merged-into merged))
  actual: (not (= "P1" nil))
```

**(b) `a-pending-result-follows-the-survivor-across-a-merge`** —
`Test results: 26 passes, 6 failures, 0 errors.` in
`ehrt.sim-engine.churn-scenarios-test`, the first being
`the follow-up survived the merge`: there is no `:result-available` in
the log at all.

**(c)** two of the four new `check_test` vars red —
`result-reference-resolves-through-a-merge` and
`result-reference-resolves-through-a-chain-of-merges`, both
`actual: (not (empty? ({:invariant
:result-references-existing-order-and-follows-it-in-time, :patient-id
"P1", :at 100})))`. The other two —
`...-still-convicts-an-unrelated-patient` and
`...-respects-the-time-clause` — are **green before and after**, which is
the point of including them: they are what proves the widening is narrow.

Green after: (a) 10 assertions 0 failures, (b) 13 assertions 0 failures,
(c) all four vars, 5 assertions 0 failures.

## 4. Step 4 — the bracket

`bin/ground-truth-bracket 007deea6 f79ddeea`, **exit 1, DIFFERS,
declared in advance by the prompt**. **Exactly one root moves and it is
`encounter-horizon`** — step 1's predicted mover, an upper bound the
bracket narrowed to itself. The other 37 digests identical, the same
three batch roots skipped on both sides, soundness `yes`.

Re-run at the final code tip, `bin/ground-truth-bracket 007deea6
8fdadb4c`: **same single root, same target digest `8730498c…`**. Every
commit after `f79ddeea` touches a judge or a consumer, and the bracket
says so rather than my saying so.

The moved root, reclassified: **170 events becomes 173**, the three
additions all `:transfer` (25 -> 28), every other kind identical in count
(36 `:admission`, 34 `:discharge`, 27 `:demographic-update`, 20
`:registered`, 17 `:coverage-change`, 4 `:bed-swap`, 4
`:cancel-transfer`, 2 `:merge`, 1 `:cancel-admit`). **110 of 173
positions differ**, and the first is index 35 — the event immediately
after the first merge, an `:admission` at `t=36660` taking `RENAL-02`
(licensed) where it took `ED-H01` (surge). One freed bed, one rung-1
success, and the rest is cascade: every later bed id shifts and three
patients who used to board are placed for real.

## 5. Two movers the ADR did not foresee, both found by the full suite

Recorded prominently because the ADR reasoned carefully about oracle
roots and the downstream fixture and got both right, and then missed two
things that only running everything finds.

**(i) The wire-side mirror** (`ceed1f5d`).
`emitter-coherence-reconstructed-state-matches-the-log-fold-at-every-boundary`
convicted. `v2-replay/fold-merge`'s own docstring declares it "the
wire-side mirror of `evolve :merge`'s `:merged` arm"; R-bed moved the arm
and the mirror did not follow. Reproduced at the shrunk case (seed 0, 2
patients, churn on, order on) at the `:merge` at `t=1800` — one field,
one participant:

```
truth  {... :status :merged ...}                          (no :location)
wire   {... :status :merged
            :location {:ward "Renal" :bed "RENAL-01"} ...}
```

Fixed by having the tombstone release the bed too. That release is an
inference from MRG-1, not a read of PV1 — an A40 carries no PV1 — and it
is the same inference the function already made for `:status :merged`.
Declining it would have left a `:merged` consumer-side entry holding a
bed: the census ghost R-bed exists to kill, alive again downstream. One
defensible reading, so fix-forward with disclosure rather than
STOP-AND-REPORT.

**(ii) The full-capability baseline** (`3e1d8346`), its own commit per
R-pins. `sim-v2-full-capability-baseline.edn` went 210 -> 212 messages,
`:files-added` `msg-210`/`msg-211`, `:files-removed` `[]`,
`:changed-verdicts` `[]`, `:codes-appeared`/`:codes-disappeared` `[]`,
`:totals {:pass 212 …}`. The verdict picture is unchanged; only the file
count moved, and by the same mechanism as `encounter-horizon`'s three
extra transfers. Its sibling `sim-v2-gate-baseline.edn` did **not** move
and is untouched — that loop's population has no merge absorbing a
bed-holder, and `sim-gate-loop-test` stayed green in the very run that
convicted the other one.

## 6. Step 6 — the downstream fixture does not move

Regenerated at both calibrated counts with the identical invocation
before and after:

| arrivals | sha256 | before vs after | bytes |
|---|---|---|---|
| 500 | `cd40af263c0c639266cd043fd2fe91b44a4fd2d6a8e66a1fc1224e3645bdb27c` | **identical** | 9,751,714 |
| 1,000 | `b431cfffcd45d5470c719abf4586da02219934dad1856c8fdb6115e9843d5301` | **identical** | 11,966,364 |

At 500, no merge absorbs a bed-holder. At 1,000 **two do**, and the
output is still byte-for-byte identical — which is the mechanism showing
up as a measurement rather than an argument. That config runs
`:bed-cycle` **on**, so `sim-model/free` applies the `:ready` gate; the
released bed never reaches `:ready`, no later allocation sees it, and
nothing shifts. `encounter-horizon` moves and this does not, for the same
one reason.

Both figures are **147 bytes** below the downstream team's published
9,751,861 / 11,966,511 — exactly their 7 `:window-close-t nil` pairs at
21 bytes each, confirming ADR-0178 from the other side.
`test-fixtures/downstream-calibration/PROVENANCE.md` records THEIR values
and stays unedited.

## 7. Judgment calls, with ratification status

1. **R-inv implemented transitively.** The ruling's wording is one hop.
   A survivor is itself `:admitted` and so itself mergeable, so R-queue
   can carry one follow-up across more than one merge, and the literal
   reading would convict a log the engine now writes. Taken as
   fix-forward with disclosure
   (`rulings.md#R-stop-only-on-two-defensible-readings`: a mechanical
   conflict with one defensible reading). **AWAITING RATIFICATION**, and
   named in ADR-0179's own R-inv section, not only here.

2. **`statuses-entitled-to-a-location` keeps `:merged`.** After R-bed the
   engine cannot produce a merged bed-holder, so the entry is now
   tolerance for a hand-authored log rather than a description. Removing
   it would create a conviction class no ruling licenses. The docstring's
   claim that the merged arm "touches only `:status`" — the sentence
   R-bed falsifies — is corrected in the same commit. **Judgment call,
   disclosed.**

3. **The step-1 derivation needed a home the prompt does not name.** Its
   commit message is `docs:`, so the counts went to
   `.agents/plans/2026-09-05-adr-0179-merge-census.md`, the shape
   `2026-09-01-event-mutation-population-ledger.md` sets.

4. **The wire-side mirror and the moved baseline** — §5, both
   fix-forward with disclosure.

5. **One adjacent erratum fixed rather than left.**
   `patient-state-model.md`'s accumulator table typed `:status` as
   `[:enum :new :admitted :discharged :expired]`, omitting `:merged`,
   which the code has carried since M2b. Fixed because the same commit
   adds a `:merged-into` row reading "absent unless `:status = :merged`";
   shipping the two adjacent would have *introduced* a contradiction
   rather than inherited one.

6. **The `:merge` validity row's precondition cell corrected while being
   marked landed.** It read "at least the surviving patient-id is
   `:admitted` or reachable", which no code enforces — the gate is on the
   absorbed partner. Marking a row landed over a stale precondition would
   have laundered it.

## 8. Findings, one line each — not roadmap rows

- **The bed a merge frees is never returned to housekeeping on
  `:bed-cycle` runs.** `:merge` is not in
  `fold/bed-correction-event-types`, so with a bed index present the
  vacated bed stays `:occupied` forever and the ward quietly loses a
  slot; with no index — the default — `free` reads the occupancy board
  and the bed is immediately allocatable, which is why
  `encounter-horizon` moves and the downstream fixture does not. Nothing
  convicts either way. Open in ADR-0179 beside R-loc; no ruling covers
  emitting a `:bed-status-change` from a merge.
- **R-loc is genuinely open.** A consumer reading a result's PV1 context
  as "where this patient is now" sees the absorbed patient's last bed on
  a message whose PID is the survivor. Asked, not answered.
- **`.agents/session-records/2026-09-03-b2-b1-stale-hold.md:68` carries
  the sentence R-bed falsifies and was correctly left alone** — a session
  record is what that session found on the day it ran. The sweep census
  for that claim found three live occurrences (`check.clj:546`,
  `v2_replay_test.clj:137`, that record); the first two are fixed, the
  third is history.

## 9. The suite at the tip

`make test` at `3e1d8346`, through a wrapper ending in
`exit "$MAKE_EXIT"`: **MAKE_EXIT=0**, **4,855 tests, 27,667 assertions,
0 failures, 0 errors**, `clojure -M:poly check` green and
`bin/verify-nist-lock` clean. That is the figure this session closes on;
it lives here, not in the ADR (`build-session` SKILL item 14).

The only commits after that tip are this record, the prompt archive and
the regenerated indexes.

## 10. CI at the pushed tip

`gh run view 33982988352` -- **status `completed`, conclusion
`success`**, head sha `1ecff673c2bfd343eabbbd9ca3f532009811afe0`, the
tip of `007deea6..1ecff673`
(<https://github.com/pragsmike/ehr-testing-tools/actions/runs/33982988352>).
All three CI legs green: `poly test :all skip:integration`,
`verify-nist-lock`, and the generated-doc freshness regen+diff. That is
this arc's close marker (`rulings.md#R-session-verifies-ci-via-gh`); no
tag was paid.

`bin/post-push-verify 007deea6 1ecff673` reported all three of its own
checks before that: remote tip matches, every commit message in the
range is pure ASCII, and the CI run reported once rather than awaited.

## 11. Close ceremony

**Background processes started by this session, all Bash jobs, all
completed, none left running:** the oracle digest at HEAD (three
invocations — two failed on a missing test resource before the right
path was found); the downstream fixture generation, before and after;
two targeted red brick runs; four full `make test` runs; two
`bin/ground-truth-bracket` runs. **No server, watcher, or daemon was
started**, and `bin/ground-truth-bracket`'s own `trap` removed its
worktrees; `git worktree list` shows only the main clone.

Every gate run went to a full log with its exit code captured
explicitly, through a wrapper ending in `exit "$MAKE_EXIT"`.

# 2026-09-10 -- same-instant encounter openers: the gate sees PENDING openers

Ceremony mode: R30 (commit and push at each checkpoint), taken from the prompt,
which states no prepare-only exception. Prompt archived at
[`../prompts/2026-09-10-same-instant-openers.md`](../prompts/2026-09-10-same-instant-openers.md).
No ADR was written and none was owed: the prompt's step 6 rules it out on the
grounds that a valid payload does not change, and the bracket confirmed that
literally -- 38 of 38 digested roots IDENTICAL. No roadmap row moved.

## 1. Scope

`bin/demo-exerciser-dense-7500` went exit 2 / `:self-check-failed` at
`--patients 6000 --seed 20260824 --churn`, on
`admission-only-when-no-open-encounter`. The author ruled "yes -- draft on the
recommended shape B" (2026-09-10); the question shape A raises (is a same-minute
reselection a second arrival at all?) was explicitly NOT ruled and is untouched
here.

**The finding, as one line:** the encounter gate is asked at DECIDE time but its
opener is DEFERRED through the queue, so two producers landing at one `t` on one
patient both see `(:encounter patient)` nil and both open.

The mechanism in full. Arrival ordinals 1897 and 1898 land at t 114720 and both
bind PERSON-014064, whose patient is `PID-001897-8080f9f2` (first-ordinal 1897 --
confirmed from `person-plan`, section 4 below). 1897 is that person's FIRST
arrival: its `:registered` decides, and the rest of its pathway -- `:admission`
at the head -- is re-pushed at `[t seq-no]` with a seq-no past `seq-start`.
1898's `:repeat-arrival` still sits at `[t 1898]`, therefore sorts FIRST, asks
`encounter-openable?` of a patient whose admission is queued and unfolded, and
prepends a second `:admission`. Both fire.

All three gated openers share the pattern -- `decide :repeat-arrival`,
`decide :person-encounter`, and `decide :appointment`'s routed visit (which
routes through `:repeat-arrival` precisely to reuse this guard) all answer with
STEPS rather than with an event. A first-arrival walk-in `:admission` has no
guard at all, which is why the reservation had to be added off `remaining'`
rather than off `:prepend-steps` -- see section 3.

**What landed** (`c282409f`): a loop-local reservation in
`ehrt.sim-engine.run`'s main loop -- the patient-ids whose next queued step is an
encounter opener -- overlaid on the world handed to `decide/decide` at its ONE
call site, and read by `encounters/encounter-openable?`. No new event kind.
Nothing new reaches `fold/apply-events`, `ground-truth`, `state-history`,
`entries` or the log, so `engine/replay` and every consumer of a finished log are
untouched by construction. The guard rides the `:encounter-minting` arm only, so
no byte of a run without `:encounters` can move through it.

## 2. Red -> green evidence

Both tests red at `913be926`, with exactly the predicted shape -- two
`:admission` events at one instant for one patient, and
`admission-only-when-no-open-encounter` firing on the log.

| test | red at 913be926 | green at c282409f |
| --- | --- | --- |
| `two-walk-in-arrivals-at-one-instant-open-one-encounter` | `(not (= 1 2))`, ids `ENC-000000-00-...` and `ENC-000000-01-...` both at t 0; one violation at t 0; `check-all` `:rejected` | pass |
| `a-hook-and-a-repeat-arrival-at-one-instant-open-one-encounter` | same three, both ids at t 4620 | pass |

Counts, `clojure -M:poly test brick:sim-engine`, `encounters-test` namespace
only: **64 passes / 6 failures** red (two new deftests, three assertions each) ->
**70 passes / 0 failures** green. Whole brick green, `brick:sim-check` green
(90 / 150 / 56 across its three namespaces), `clojure -M:poly check` **OK**.

**`bin/ground-truth-bracket 913be926 c282409f`, no `--declared-digest-change`:
IDENTICAL on every digested root (38 of 41; 3 skipped for carrying no
`:ground-truth` key -- `appendicitis.edn`, `ear-infections.edn`,
`sore-throat.edn`).** This is not a regression-oracle claim and the script's own
banner says so: the `:hl7` half of every root is excluded by construction. The
bracket's own `--declared-digest-change: no` line stands.

The prediction the bracket tests is the one the prompt made: a VALID corpus never
has two pending openers at one instant, so shape B changes no byte of one. It
held.

Note what the bracket is BLIND to here, stated rather than left implied: its 38
roots are fixed-seed golden runs, and none of them is a dense multi-thousand-
arrival run with a person pool. The dense-6000 run in section 4 is what covers
the cell the defect actually lived in, and it is not a committed cell.

## 3. Judgment calls, and their ratification status

**(a) The reservation is added off `remaining'`, not off `:prepend-steps`.**
UNRATIFIED -- disclosed here and in the code. The prompt's step 3 says "added
when a decide returns `:prepend-steps` whose first step is an encounter opener".
That rule names 1898's half and MISSES 1897's: 1897's opener is the head of its
own re-pushed pathway tail after `decide :registered`, and was prepended by
nothing. Measured, not reasoned: under the literal rule, red test (i) stays red,
because its first arrival is the 1897 analogue. `remaining'` IS
`(into (vec prepend-steps) remaining)`, so it is the one expression that names
both halves, and it is the same `let` binding the loop already re-queues off.

**(b) Cleared on apply, never on a re-queue whose head stopped being an opener.**
UNRATIFIED. This is the prompt's own clearing rule and I kept it rather than the
tempting "recompute membership per pop": a patient may hold a SECOND queue entry
(a `:result-followup` rides the queue too), and recomputing would let the wrong
entry retract a live reservation. The cost is a reservation that never clears if
an opener step is dropped without emitting; the only such path is a `:merged`
patient's abandoned queue, and `encounter-openable?` already refuses `:merged` on
`:status` first, so it is inert. `decide :admission` and `decide
:outpatient-visit` always emit (the exhausted arm halts the run), checked.

**(c) `encounters` is required back into `run.clj`.** DISCLOSED, and the ns
docstring's own sentence ("Three requires went dead with the fold that used them
-- `encounters`, `evolve` and `log-index`") is amended in place rather than left
false. The two opener names live in `encounters` beside the guard that reads the
reservation; naming them a second time in `run` is how the two would drift.

**(d) Red test (ii) needed a fixture the plain helpers could not build.** The
prompt allowed continuing with (i) alone if so; it turned out buildable, via
`assign-pathway`'s explicit `{:patient-ordinal i :pathway ...}` override.
`hook-plan` only puts an encounter on a CLINICALLY IDLE patient (whose whole
queue is their `:registered`), while a repeat arrival only queues anything if its
pathway HAS steps -- and one `:pathway` key cannot be both. Ordinal 0 walks
nothing, ordinal 1 walks the brief stay. The hook's instant is READ off the
fixture (`prelude`'s own `:arrivals`) rather than written into it, because
`after-own-arrival?` is strict and `:arrival-gap 0` would defeat it.

**(e) `bin/preflight` ran AFTER checkpoint 1, not before it.** DISCLOSED, a
deviation from R-preflight-fail-closed's intent. Its only FINDING was the one my
own commit had just created (local HEAD != origin/main); every other check was
OK, including tree-clean-at-session-start and the last five CI runs on `main`
all green.

## 4. The dense-6000 run

`bin/ehrt sim run --seed 20260824 --patients 6000 --churn --config
demos/scenarios/dense-7500/config.edn --format ground-truth >
out/dense-6000.edn` -- **exit 0**, so the run's own self-check passed. This is
the gate; `out/` is gitignored and NO `figures.edn` cell moved, because this is
not a committed cell.

Confirmed in-tree, against the live tree at `c282409f`:

* `person-plan`'s `:bindings` -- ordinal 1897 -> `PERSON-014064`, ordinal 1898 ->
  `PERSON-014064`. Same person. `:person-index` for that person:
  `{:patient-id PID-001897-8080f9f2, :first-ordinal 1897, :active-mrn MRN001898,
  :placeholders #{}}`.
* **Exactly one `:admission` at t 114720 for `PID-001897-8080f9f2`**
  (`ENC-001897-00-d6ebe12c`) -- and exactly one in the WHOLE log.
* No `PID-001898-*` participant appears anywhere: a repeat arrival mints no
  second patient, which is `registered-is-every-patients-first-event` holding by
  construction.
* Twelve events share t 114720; the `:registered` and the `:admission` above are
  this patient's two.
* Log size 137,460 events, 7,440 distinct first-participant patient-ids (the
  count includes the `nil` of `:bed-status-change`, which names no patient).

**The alive count at t 114720: 15,000 of a 15,000-person population.** The word
is the engine's own -- `:alive` is the map of compiled death instants
`select-person`'s half-open filter reads. 5,101 of the 15,000 carry such an
instant at all, and every one of those falls AFTER t 114720, so nobody in the
pool is dead at the instant the two ordinals collided. Stated because the prompt
asked for it: the collision is not a scarcity artifact of a shrinking pool.

## 5. Findings and HEAD landed

* The defect above, closed.
* `decide :person-encounter`'s docstring cites `prelude`'s `encounter-free?` as
  the static half of its guard. **That var does not exist in the live tree** --
  the static half is `clinically-idle?`. Found while building red test (ii);
  NOT fixed, as out of this session's fence. Named here for a later errata
  sweep.
* The open shape-A question, carried forward unruled: **is a same-minute
  reselection a second arrival at all?** Shape B makes the second one open
  nothing, which is the conservative answer and preserves every invariant; it
  does not answer whether `select-person` should have bound that person twice at
  one instant in the first place. Left to the design channel.

HEAD landed: `c282409f` (payload), plus this record.

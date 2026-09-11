# Session prompt -- same-instant encounter openers (shape B)

Repo `ehr-testing-tools`, ext4 clone of record `/home/mg/src/ehr-testing-tools`,
HEAD at session start `913be926`, working tree clean. Ceremony mode: R30, the
standing default -- the prompt states no prepare-only exception. Paired record:
[`../session-records/2026-09-10-same-instant-openers.md`](../session-records/2026-09-10-same-instant-openers.md).

## The prompt, verbatim

> # Same-instant encounter openers: the gate sees pending openers (shape B)
>
> Context: downstream hit exit 2 / :self-check-failed on dense-7500 at --patients 6000, seed 20260824,
> --churn. Ordinals 1897 and 1898 arrive at t=114720, bind one person (PID-001897-8080f9f2), both walk-ins.
> Ordinal 1897's :admission is re-pushed at [t N>=seq-start]; ordinal 1898's :repeat-arrival sits at
> [t 1898], sorts first, asks encounter-openable? (no encounter yet), prepends a second :admission. Both
> fire. The gate is asked at decide time but its opener is deferred through the queue; all three gated
> openers share the pattern. A first-arrival walk-in :admission has no guard at all.
>
> Read first:
> - components/sim-engine/src/ehrt/sim_engine/run.clj:111-117, 1258-1279, 1556-1664
> - components/sim-engine/src/ehrt/sim_engine/decide.clj:528-604, 645-700, 1029-1046
> - components/sim-engine/src/ehrt/sim_engine/encounters.clj:33-52
> - components/sim-engine/src/ehrt/sim_engine/evolve.clj:200-232
> - components/sim-check/src/ehrt/sim_check/check.clj:199-252
> - components/sim-engine/test/ehrt/sim_engine/persons_test.clj:78-86, encounters_test.clj:31-60
> - bin/ground-truth-bracket (usage line), .agents/session-records/README.md
>
> Author ruling 2026-09-10: "yes" -- draft on the recommended shape B. The question shape A raises (is a
> same-minute reselection a second arrival at all?) is OPEN and not ruled; do not decide it here.
>
> Steps:
> 1. Red test (i) in encounters_test.clj: 2 patients, a 1-person pool (reuse persons_test's helper
>    shape), :arrival-gap 0, :encounters true, walk-in pathway [:admission :delay :discharge].
>    Invariant: red at 913be926 -- two :admission events at t=0 for one patient, and
>    admission-only-when-no-open-encounter fires on the log.
>    Gate: clojure -M:poly test brick:sim-engine shows exactly this new failure.
> 2. Red test (ii): a hook :person-encounter and a :repeat-arrival at one instant on one patient, same
>    expectation. This is an expectation, not a ruling: if the fixture is not buildable from the existing
>    helpers in this session, say so in the record and continue with (i) only.
>    Invariant: red before step 3, or a recorded reason.  Gate: same command.
> 3. The reservation, loop-local in run.clj: a set of patient-ids with a pending opener, added when a
>    decide returns :prepend-steps whose first step is an encounter opener, cleared when a batch applies
>    an opener event for that patient; overlaid on the world passed to decide/decide (:1561) only.
>    encounter-openable? reads it.
>    Invariant: no new event kind; nothing new reaches fold/apply-events, ground-truth, state-history,
>    entries or the log -- engine/replay is untouched.
>    Gate: both red tests green; clojure -M:poly check and test brick:sim-engine, brick:sim-check green.
> 4. bin/ground-truth-bracket 913be926 <HEAD> with no --declared-digest-change.
>    Invariant: IDENTICAL on every oracle root (a valid corpus never has two pending openers at one
>    instant, so B changes no byte of one).  Gate: the bracket's own banner.
> 5. bin/ehrt sim run --seed 20260824 --patients 6000 --churn --config demos/scenarios/dense-7500/config.edn
>    --format ground-truth > out/dense-6000.edn. Confirm in-tree that ordinals 1897 and 1898 bound one
>    person and record the alive count at t=114720. No figures.edn change -- this is not a committed cell.
>    Invariant: exit 0, self-check passes, exactly one :admission at t=114720 for PID-001897-8080f9f2.
>    Gate: the run's own self-check.
> 6. Session record .agents/session-records/2026-09-XX-same-instant-openers.md: the finding as one line,
>    the alive count from step 5, and the open shape-A question. No ADR: a valid payload does not change.
>    If you judge otherwise, stop and report rather than write one. Push; gh run view the tip; CI green is
>    the close marker.

## Deviation record

1. **Step 3's reservation is added off `remaining'`, not off `:prepend-steps`.**
   The prompt's literal rule names 1898's half of the collision and misses
   1897's, whose opener is the head of its own re-pushed pathway tail after
   `decide :registered` and was prepended by nothing -- so under the literal rule
   red test (i) stays red. Measured, not reasoned. `remaining'` is
   `(into (vec prepend-steps) remaining)`, the one expression naming both halves.
   The clearing rule is the prompt's own, unchanged. Full reasoning in the
   record, section 3(a).

2. **Step 2's fixture WAS buildable**, so (ii) landed rather than being recorded
   as unbuildable -- but not from the plain helpers: it needed `assign-pathway`'s
   explicit `{:patient-ordinal i :pathway ...}` override, because `hook-plan`
   only puts an encounter on a CLINICALLY IDLE patient while a repeat arrival
   only queues anything if its pathway HAS steps. Record, section 3(d).

3. **`bin/preflight` ran after checkpoint 1 rather than before the first edit.**
   Its only FINDING was the one that commit had itself created. Record, 3(e).

4. **`encounters` was required back into `run.clj`**, which the prompt did not
   name; `run.clj`'s ns docstring said those requires had gone dead and is
   amended in place rather than left false. Record, 3(c).

5. **No ADR was written**, per step 6's own instruction, and the bracket
   confirmed its premise literally: 38 of 38 digested roots IDENTICAL.

6. The **open shape-A question was not decided**, per the author ruling, and is
   carried forward in the record's section 5.

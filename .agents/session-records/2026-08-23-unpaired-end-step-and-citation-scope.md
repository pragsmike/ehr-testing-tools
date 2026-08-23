# 2026-08-23 -- unpaired end-step drop (ADR-0163) plus patient-scoped citation resolution (ADR-0164)

Two commits, real defect first, per R1(3). Local commits only -- no
push, no tag, as the prompt directed.

- `62a63d2` -- ADR-0163, the real defect (compile-time drop)
- `428eaed` -- ADR-0164, the latent defect (decide-time patient scope)

## Step 0 -- environment and baseline

penny, WSL2, JDK 21.0.7. HEAD at session start was exactly
`7a3ffd84c3e75fbdba03b1177f4923a8af6d649d`, tree clean.

`bin/preflight`, exit 0, no findings: last five CI runs on `main` all
green (the top three at `7a3ffd84` itself); edit root
`/home/mg/src/ehr-testing-tools`, not under `/mnt/`; tree clean
including untracked; local HEAD == `origin/main`; last tag
`stable-20260821-patient-simulator-charter` @ `6ce2160c`, HEAD
untagged and none owed.

Baseline `make test`, unpiped, `MAKE_EXIT=0`: **368** zero-failure
blocks / **4,100** tests / **18,378** assertions -- reconciling exactly
against the tip commit `7a3ffd84`'s own recorded figures.
`clojure -M:poly check` **OK**.

## make-test wall clock -- a FINDING, not a stale-doc correction

Step 12 asked to "update `state.md`'s stale ~14.5min figure". **That
premise does not hold and no such edit was made:** `.agents/state.md`
carries no make-test runtime figure at all (grepped for minutes /
runtime / wall-clock / `~NNm` -- nothing). The ~14.5 min figure lives
in the executing agent's own session memory, not in any tracked doc.

More to the point, ~14.5 min was **correct** when it was written. The
tracked session records carry poly's own `Execution time` lines: **14
minutes 48 seconds** (2026-08-20), **14 minutes 15 seconds** and **14
minutes 3 seconds** (2026-08-20), **14 minutes 22 seconds** and **14
minutes 34 seconds** (2026-08-21). Three full runs this session, all
`MAKE_EXIT=0`, all unpiped to a log with the exit code captured
explicitly:

| run | tree | elapsed | poly's own `Execution time` |
| --- | --- | --- | --- |
| baseline | HEAD `7a3ffd84`, unmodified | **1864s (31m04s)** | 29 minutes 46 seconds |
| commit 1 | fix + tests + regenerated artifacts | **1713s (28m33s)** | -- |
| commit 2 | src + ADR-0164 | **1698s (28m18s)** | -- |
| final | completed tree, close docs included | **1759s (29m19s)** | -- |

So the suite now takes roughly **2x** what it took two days ago, on the
same machine. The baseline run overlapped four concurrent
reproduction/analysis JVMs and is the outlier, but it also ran against
**unmodified HEAD** -- so the slowdown is not attributable to this
session's commits, and the two later runs, with nothing else running,
still came in at ~28.5 min. The prompt's own "~27min" and "26m39s"
figures sit in the same new band, which suggests the design channel had
already observed it.

**Cause not determined this session** -- out of scope, and nothing here
turned on it. Recorded as an open observation rather than silently
written into a doc as though it were a known quantity. ~28.5 min is the
honest figure to plan against today.

## Step 1 -- the failure re-confirmed live

Both runs re-executed at `7a3ffd84`, matching the prior session's
record exactly, so no FINDING was raised:

| seed | exit | result |
| --- | --- | --- |
| 424242 | **2** | `:self-check-failed`, one violation, `:medication-end-references-existing-order-and-follows-it-in-time`, `PID-000089-c02fd3a8` at `:t 5629740` |
| 5 | **0** | clean |

Reading the log directly confirmed the compiled shape: the seed-424242
log's **only** `:medication-end` sits at index 200 with
`:order-event-id nil` and `:order-citation nil`.

## Step 2 -- the R3 probe: `:condition-end` does NOT share the shape

**Reported answer: not identical; the fix was NOT extended to it.**

- `gmf_interpreter.clj:1848-1851` -- `:condition-end` resolves
  `:references` through `index-of-citation`
  (`gmf_interpreter.clj:1225-1236`) alone. It has **no**
  `referenced_by_attribute` form at all, so the never-written-attribute
  route cannot reach it.
- `compile_trajectory.clj:560` -> `annotate-condition`
  (`compile_trajectory.clj:357-382`) -- it compiles to an
  **annotation** on an existing encounter step, never a standalone IR
  step, so a nil referent produces no unpaired end-step at all.
  `annotate-condition` already handles the nil referent without
  fabricating anything.

`:care-plan-end` by contrast IS a genuine twin, confirmed by live
evidence rather than inference: seed 5's pre-fix log already carried
**two** unpaired `:care-plan-end` events (`PID-000045-03ebff87` at `:t
3636360`, `PID-000187-899c715a` at `:t 27417360`), passing only because
`check.clj` has no `:care-plan-end` invariant at all.

## Commit 1 -- ADR-0163

Red captured under stash isolation for every new test. The two
nil-referent unit tests failed showing the defect itself:

```
actual: (not (empty? [{:type :delay, :from 14400, :to 14400}
                      {:type :medication-end, :citation {:module "m", :state :end-rx}}]))
```

-- an end-step with no `:order-citation`. Both designed-straddle guard
tests passed before AND after, as required.

Both new `run_test.clj` scenario gates also red first, with the src fix
stashed: seed 424242 reproduced the exact reported violation, and seed
5's red output named both unpaired `:care-plan-end` events in full.
That second gate asserts the **shape**, not the exit code -- seed 5
exits 0 in both directions, so exit code alone could never catch it.

Post-fix: seed 424242 exits **0** (13.09s), seed 5 exits **0**
(12.81s).

Oracle sweep, `make docsgen` exit 0: **no generated artifact changed**
by the fix itself. The subsequent regeneration moved exactly two lines,
both from adding an ADR file -- `state-derived.md`'s ADR count 160 ->
161 and the new `notes/ADRs.md` index row.

`make test` over the commit-1 tree: `MAKE_EXIT=0`, **368 / 4,112 /
18,406** (+12 tests, +28 assertions over baseline -- six new tests
carrying 14 assertions, each counted once per project that includes the
brick).

## Commit 2 -- ADR-0164

Red first, real output, both tests:

```
expected: (= "A" (:patient-id (first (:participants resolved))))
  actual: (not (= "A" "B"))
```

Oracle sweep, `make docsgen` exit 0: **no artifact changed**.

`make test` after commit 2's src and ADR: `MAKE_EXIT=0`, **368 / 4,116 /
18,414**. `clojure -M:poly check` **OK**.

## Step 12 -- final gate over the completed tree

`make test` re-run over the whole completed tree, close documentation
included: `MAKE_EXIT=0`, **368** zero-failure blocks / **4,116** tests /
**18,414** assertions -- unchanged from the commit-2 figures, as
expected for a docs-only delta. `clojure -M:poly check` **OK**.

## R5 disclosures

Both required, both made in their own ADR as well as here.

- **Commit 1.** The compile layer emits IR steps and has no
  ground-truth log of its own, so "the checker invariant is clean over
  the resulting log" cannot be asserted at that layer under any
  scaffold. Rather than build a synthetic one, that half is discharged
  by the two `run_test.clj` gates, which run the real pipeline end to
  end through `check-all` -- stronger evidence than a scaffolded
  assertion.
- **Commit 2.** The `world-of`/`admit`/`fold-events` scaffold cannot
  produce `:registered`, so both engine tests assert the resolved index
  **directly**, per `engine_test.clj:1133-1147`'s own convention.

## Deviations and disclosures

1. **The prior session's archived report is not in the tree.** The
   prompt's "Read first" item 1 names a report "archived alongside this
   prompt"; it is absent from `.agents/session-records/`,
   `.agents/prompts/`, and every adjacent path -- nothing anywhere in
   the repo mentions seed 424242. Not treated as blocking: the prompt's
   own Context section restates the trace, and Step 1 plus the Step 2
   probe re-derived every link of it independently against the live
   tree, which is what the report would have been read for.

2. **Seed 5 landed as a gated test, not "ADR only".** The prompt
   offered either. It went in because it turned out to be the *only*
   population-scale exercise of the `:care-plan-end` half and is
   genuinely red-capable -- its pre-fix log carried two unpaired
   `:care-plan-end` events that no invariant covers. Cost: ~13s on the
   per-push tier, beside the existing ~1s seed-202 gate. Both new gates
   assert the unpaired-end shape directly rather than the exit code.

3. **A wider blast radius than "removed end-steps and knock-on
   indices", investigated rather than escalated.** Seed 424242's log
   went 357 -> 343 events with a population-wide reshuffle
   (`:admission` 1->0, `:diagnostic-report` 4->0, `:observation`
   16->10, and more), which is more than the fix nominally removes. The
   step-6 escalation clause is scoped to the **docsgen artifacts**, and
   those changed not at all -- so the gate did not trip. The mechanism
   was traced rather than assumed: `engine.clj:1490` seeds **one**
   shared `Random` consumed in work-queue order, and `decide :delay`
   (`engine.clj:414-421`) draws unconditionally even when `:from`
   equals `:to`. Dropping an end-step with a nonzero preceding gap also
   drops its `:delay`, removing one draw and reshuffling every later
   decide. Seed 5 confirms the mechanism from the other side: its two
   dropped ends had a **zero** preceding gap, so no `:delay` and no
   draw were removed, and its log changed by exactly those two events
   with every other event kind identical. Inherent to the engine's
   single-shared-RNG design, not to this fix, and not something a fence
   permits working around. Recorded in ADR-0163 because "seed 424242
   now exits 0" is a weaker claim than it looks: that run's population
   genuinely differs.

4. **A stash pop was silently skipped by a shell syntax error**, and
   one "green" run was consequently executed against a tree with the
   src fix still stashed. Caught by reading `git status` rather than
   trusting the run; the stash was popped and the green re-run
   properly. The invalid run is disclosed rather than discarded
   quietly.

5. **`state-derived.md` staleness caught a real ordering mistake of
   mine.** The first commit-1 suite run was started after the ADR file
   was created but before `make docsgen` regenerated the indexes, and
   failed at 84s on `state-derived-md-matches-a-fresh-render-test` (ADR
   count 160 vs 161). The gate was right; the run was re-done after
   regeneration. No drafted justification for skipping a step was
   entertained (ADR-0128) -- there was nothing to skip, only to reorder.

6. **`core.fileMode` is now `true` in this clone.** Standing memory
   recorded it as `false` (the ADR-0149 note about needing `git
   update-index --chmod=+x` for new `bin/` scripts). `bin/preflight`
   reports `true` today. No `bin/` script was added this session, so
   nothing turned on it.

## Fences honoured

`check.clj` untouched in both commits. Citation shape untouched.
Vendored modules byte-identical (ADR-0071). Interpreter nil-resolution
untouched. No seed special-casing, no invariant weakening, no history
rewrites. No sanctioned improvement was taken beyond the rulings --
none was offered. Local commits only; no push, no tag.

## Close ceremony

`bin/close-scaffold 2026-08-23 unpaired-end-step-and-citation-scope`
created this record and its paired prompt archive, and regenerated both
record indexes. Stubs filled in; the prompt is archived verbatim under
its own header.

**Register hygiene.** This session's row added to `## Done`; the oldest
whole row (ADR-0159, `[repo-review-4]`, 6 lines) rotated **verbatim**
into `.agents/plans/roadmap-done-2026-08.md`, leaving `## Done` at **24
lines**, under the 30-line cap (`R-done-attic-rotation`, ADR-0161).

The row was written wrong the first time and `roadmap_lint_test` caught
it -- three failures at once: 10 lines against the six-line cap
(ADR-0144 Q3), and a `CLOSED 2026-08-23 ADR-0163/ADR-0164` token, where
the contract admits exactly one `ADR-\d{4}` or one sha. Rewritten to six
lines tokened on ADR-0163 with ADR-0164 named in the body, the shape the
ADR-0159 row already uses for a five-ADR arc. Disclosed rather than
quietly fixed, because it is the second time this session a generated-
register gate caught an ordering or format mistake of mine before the
suite did (the first was `state-derived.md`'s ADR count). The rotation
was performed against the 34-line intermediate, so with the corrected
six-line row `## Done` would have landed at exactly 30 -- at the cap,
not over it. The rotation is left in place regardless: the attic is
append-only and un-rotating would delete a line from it.

**Reading sets, re-measured at the close** from the generated
`state-derived.md` rather than from prose — all five under budget, none
compacted, none bumped:

| set | actual | budget | baseline | headroom |
| --- | --- | --- | --- | --- |
| `:corpus` | 1836 | 2045 | 2045 | 209 |
| `:docs` | 739 | 785 | 785 | 46 |
| `:judge` | 926 | 1000 | 1000 | 74 |
| `:onboarding` | **1410** | 1530 | 1530 | 120 |
| `:sim` | 1278 | 1405 | 1405 | 127 |

`:onboarding` moved 1406 -> 1410, the net of this session's roadmap row
minus the rotated one. No other set moved.

## Adjacent row NOT closed by this work

`.agents/plans/roadmap.md`'s DEFERRED **[veteran-hyperlipidemia]** row
(ADR-0090) is adjacent and deliberately left open. It describes a
module whose annual reassessment loop never clears `statin_initial`, so
it re-fires `MedicationEnd` against an **already-ended** order. That
end's attribute WAS written and still resolves a referent, so this
session's drop rule never fires on it, and its trigger — whether
`MedicationEnd`/`referenced_by_attribute` should be idempotent as a
general interpreter rule — is untouched here. Recorded so a later
reader does not mistake ADR-0163 for having closed it.

## Ceremony not performed, by instruction

No push and no tag: the prompt directed local commits only. HEAD is
therefore **ahead of `origin/main` by three commits** and no
`bin/post-push-verify` run applies. CI has not seen this work.

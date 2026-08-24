## ADR-0166 — the `:care-plan-end` referential invariant: the silence ADR-0163 found by hand, closed

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-23.

### Context

ADR-0163 recorded a fact and could not act on it in that commit's own
fences:

> seed 5's log already carries **two** such unpaired `:care-plan-end`
> events today (`PID-000045-03ebff87` at `:t 3636360`,
> `PID-000187-899c715a` at `:t 27417360`) ... and exits 0 only because
> **no invariant covers `:care-plan-end`** — `check.clj` has no analogue
> of the `:medication-end` rule.

Two structurally identical defects, in the same run family, on the same
day. One exited 2 and was reported by a consumer. The other exited 0
and was found only because a human read the log. The difference was not
severity — it was that one span had an invariant and the other did not.

`:care-plan-end` was the ONE paired terminal event type the catalog did
not cover. This ADR covers it.

> Q1 / P4: (a) — Q1's care-plan-end invariant rides along as commit 2.

### Step 7 — the probe, run rather than inferred

The prompt required the pre-horizon escape to match what the compiler
actually promotes, "not what the medication twin suggests." Probed, and
the twin turns out to be exact. Four independent reads:

1. `compile_trajectory.clj:367-379` — `pre-horizon-fact-types` is
   `#{:condition-onset :condition-end :medication-order :medication-end
   :care-plan-start :care-plan-end}`. The care-plan pair joined at
   ADR-0029 D2, in the SAME "ongoing therapeutic content" class as the
   medication pair, explicitly contrasted there against the ephemeral
   class `:observation`/`:procedure`/`:encounter` sit in.
2. `compile_trajectory.clj:531-534` — ONE shared clause promotes any
   member of that set to `{:event :codes :citation :references}` on
   `:registration-facts`. There is no care-plan-specific branch to
   differ.
3. `event_schema.clj:186-191` — `PreHorizonFact`'s own `:event` enum
   carries all six, and its docstring names the nested-`:event` hazard:
   four of the six are ALSO top-level log kinds with different key
   sets.
4. `engine.clj:887-907` — `decide :care-plan-end` emits
   `:start-event-id` + `:care-plan-citation`, the exact twin of
   `decide :medication-end`'s `:order-event-id` + `:order-citation`,
   both patient-scoped since ADR-0164.

So the invariant mirrors its twin exactly, and the mirroring is
grounded in the compiler rather than in the resemblance.

### The invariant

`care-plan-end-references-existing-start-and-follows-it-in-time`, added
to `check.clj` beside its mirror and to `catalog` in the adjacent
position (the catalog documents itself as being in reporting order; a
reader comparing the two should find them adjacent). Its docstring
carries ADR-0163's finding — seed 5's two silent unpaired ends, by
patient id and `:t` — as its stated origin.

Its helper `pre-horizon-care-plan-start-citations-by-patient` mirrors
`pre-horizon-medication-order-citations-by-patient`, keying on the
fact's own `:event` being `:care-plan-start` — the discrimination the
nested-`:event` hazard makes load-bearing, and which has a test of its
own below.

### Red before green, in both directions

**Red 1 — the trivial sense, recorded because it is not evidence.**
Tests landed first; the run errored `No such var:
check/care-plan-end-references-existing-start-and-follows-it-in-time`.
That proves only that the var was absent.

**Red 2A — the invariant must actually reject.** With the reporting
guard neutered (`:when (and false ...)`) so the function can never
report, ALL SEVEN rejection tests fail and BOTH acceptance tests still
pass:

```
FAIL care-plan-end-...-detects-phantom-start
FAIL care-plan-end-...-detects-a-nil-start
FAIL care-plan-end-...-detects-another-patients-start
FAIL care-plan-end-...-detects-a-start-that-follows-its-end
FAIL care-plan-end-...-detects-a-target-of-the-wrong-kind
FAIL care-plan-end-...-detects-a-phantom-start-even-with-unrelated-pre-horizon-facts
FAIL care-plan-end-pre-horizon-escape-does-not-accept-a-medication-order-fact
Ran 74 tests containing 79 assertions.  7 failures, 0 errors.
```

**Red 2B — the escape must actually be needed.** With
`pre-horizon-referent?` forced false, the designed straddle is rejected
— in the scripted fixture AND, more usefully, in a real corpus:

```
FAIL care-plan-end-...-allows-the-pre-horizon-straddle
Ran 74 tests containing 79 assertions.  1 failures, 0 errors.

adhd-seed-2 ok? false -> {:violations [{:invariant :care-plan-end-references-existing-start-and-follows-it-in-time,
                                        :patient-id "PID-000005-939736f8", :at 19016340}]}
```

That is ADR-0165's newly-gated `attention_deficit_disorder` seed-2 run
naming the exact straddle patient. The escape is not defensive coding
for a case that cannot happen; it is required by a corpus this suite
now runs on every push. Both mutations reverted, both restorations
verified green.

The nine scripted tests cover: phantom index, nil target with a
citation (ADR-0163's own shape at the checker), another patient's start
(ADR-0164's hazard at the checker), a start that follows its end, a
target of the wrong kind, the sound log, the designed straddle, an
unrelated pre-horizon fact NOT making the checker permissive
(ADR-0123's lesson carried onto the twin), and a `:medication-order`
pre-horizon fact under the same citation NOT excusing a care-plan end
(the nested-`:event` hazard).

### DECLARED ORACLE CHANGE, and its outcome

Widening `catalog` re-judges every log every `check-all` caller
produces. Declared before running, and the population was ENUMERATED by
grep rather than assumed — 44 files reference `check-all` or the
catalog. The 32 test namespaces that build a corpus and judge it were
re-run as one batch: all 28 `sim-emit-hl7` `vendored_*_test`
namespaces — among them `asthma`, `bronchitis`, `colorectal`,
`dermatitis`, `injuries`, `rheumatoid_arthritis`, `tjr`, `veteran_ptsd`
and `attention_deficit_disorder`, i.e. every module carrying a
CarePlanEnd state at all — plus `engine_test`, `corpus.sim-adapter-test`,
`event-conformance-test` and `sim.interface-surface-test`.

**Outcome: no gated corpus newly fails.**

```
Ran 138 tests containing 591 assertions.
0 failures, 0 errors.
```

`check_test` (74 tests / 79 assertions, including the 300-run
`every-m1-run-satisfies-the-invariant-catalog` defspec) and `run_test`
(27 / 89) were run separately and are also clean. The whole-tree figure
lives in this session's own record rather than here, per ADR-0158 D1-1.

Not a surprise, and the reason is readable: ADR-0163's compile-time
drop already removed the only unpaired `:care-plan-end` events these
corpora ever carried. Had this invariant landed BEFORE that fix, seed 5
would have failed — which is the point. The order of the two arcs is
why this widening is quiet.

### Oracle sweep

`make docsgen`, exit 0. ONE artifact moved:
`.agents/state-derived.md`'s generated ADR-file count, 163 → 164. No
`demos/traces/**` ground truth, no `event-examples.edn`, no schema
export — as predicted, and for the plainest possible reason: an
invariant JUDGES a log and never produces one, so nothing generated
from the generator can move.

### Fences honored

`check.clj` changed in this commit ONLY, and only by the new invariant,
its helper, and one catalog entry. No existing invariant was touched.
No vendored module was touched.

### Error ledger

- **The original gap, named for what it was.** `:medication-end` got a
  referential invariant when ADR-0029 D2 landed the medication span;
  `:care-plan-start`/`:care-plan-end` landed in the SAME wave, in the
  same ruling, described in `check.clj`'s own comments as
  "structurally identical to MedicationOrder/MedicationEnd" — and the
  invariant was not mirrored across. The asymmetry sat unnoticed from
  2026-08-02 to 2026-08-23, and what finally exposed it was not the
  catalog but a person reading a log.
- **What "exit 0" was worth here.** Seed 5 exited 0 before ADR-0163 and
  after it, with genuinely different content each time. An exit code is
  only as strong as the catalog behind it, and this ADR is one more
  reason ADR-0165's coverage meter is a companion to it rather than a
  separate concern.

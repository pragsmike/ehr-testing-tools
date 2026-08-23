## ADR-0163 — an end-step whose referenced order never fired is dropped at compile time: "no orphaned reference" extended to "no reference ever existed"

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-23.

### Context

A consumer-reported failure, reproduced at `7a3ffd84`: seed 424242 over
`demos/scenarios/clinic-decade` exits 2 with exactly one violation of
`:medication-end-references-existing-order-and-follows-it-in-time`,
patient `PID-000089-c02fd3a8` at `:t 5629740`. Seed 5 over the same
scenario exits 0.

The prior session traced the mechanism; this session re-confirmed every
link of it against the live tree before touching any `src` (Step 1 and
the Step 2 probe below).

1. `urinary_tract_infections.json`'s own `End UTI Tx` MedicationEnd
   resolves through `referenced_by_attribute: "UTI_Tx"`, an attribute
   written only by MedicationOrder states inside the submodule
   `uti/abx_tx.json`. This walk went telemed -> referral-to-ambulatory
   -> end, never entering `abx_tx.json` at all.
2. `gmf_interpreter.clj:1926` reads that attribute with a plain `get`,
   so a never-written attribute resolves to `nil` — the deliberate,
   documented departure from upstream's fail-loud `RuntimeException`
   ("referenced but not set"), recorded at `gmf_interpreter.clj:1677`
   and left **unchanged** by this ADR (R2).
3. `compile_trajectory.clj:271`'s `medication-end->step` then has
   `referenced-event` resolve nothing, its `cond->` skips
   `:order-citation`, and an **unpaired** `:medication-end` IR step is
   compiled. No drop rule covered this: every existing rule
   (`history-phase?`, `straddle-open?`, AR-2's inheritance) covers an
   antecedent that WAS minted and then dropped FROM the trajectory —
   none covered an antecedent that was never minted at all.
4. `decide :medication-end` turns the absent citation into
   `:order-event-id nil`, and `check.clj`'s invariant correctly fires:
   a nil target, and no citation with which to match the
   pre-horizon-fact branch.

### Author rulings (verbatim)

> R1: (3) — both defects, separate commits with separate ADRs, real
> defect first.

> R2: (a) — compile-time drop: an end-step whose
> referenced_by_attribute resolved to no referent (and therefore
> carries no citation) is dropped in compile_trajectory, extending the
> existing "no orphaned reference" principle to "no reference ever
> existed." Raw trajectory keeps the event. Interpreter nil-resolution
> unchanged. Vendored modules untouched.

> R3: (a) — :medication-end plus :care-plan-end as declared twins;
> PROBE whether :condition-end shares the identical nil-referent shape
> and report; extend to it only if the probe confirms.

### Step 1 — the failure re-confirmed live at HEAD

Both runs re-executed at `7a3ffd84`, matching the prior session's
record exactly:

| seed | exit | result |
| --- | --- | --- |
| 424242 | **2** | `:self-check-failed`, one violation, `PID-000089-c02fd3a8` at `:t 5629740` |
| 5 | **0** | clean |

Reading the violating event off the live log confirms the compiled
shape: the log's **only** `:medication-end` is at index 200,
`:order-event-id nil`, `:order-citation nil`.

### Step 2 — the R3 probe: `:condition-end` does NOT share the shape

**Answer: not identical. The fix is NOT extended to it.** Two
independent reasons, both read off the tree:

- **Reference route differs.** `:condition-end`
  (`gmf_interpreter.clj:1848-1851`) resolves `:references` through
  `index-of-citation` (`gmf_interpreter.clj:1225-1236`) alone. It has
  **no** `referenced_by_attribute` form at all, so the never-written-
  attribute route that produced this defect cannot reach it. (It can
  still resolve nil, when the cited ConditionOnset state never fired —
  the same route `:care-plan-end` takes.)
- **Compile treatment differs, and is the load-bearing difference.**
  `:condition-onset`/`:condition-end` compile to an **annotation** on
  an already-compiled encounter step (`compile_trajectory.clj:560` ->
  `annotate-condition`, `:357-382`), never to a standalone IR step.
  There is therefore no unpaired end-STEP for a nil referent to
  produce. `annotate-condition` already handles the nil referent
  without fabricating anything: `(or (:codes event) (:codes
  (referenced-event trajectory event)))`, and no compiled encounter
  step at all leaves `steps` unchanged.

`:care-plan-end` by contrast IS a genuine twin on the dimension that
matters: it reaches nil by `index-of-citation` rather than by
attribute, but arrives at the **identical** compile-time shape — a
standalone terminal IR step whose `cond->` omits its citation. Live
evidence, not inference: seed 5's log already carries **two** such
unpaired `:care-plan-end` events today (`PID-000045-03ebff87` at `:t
3636360`, `PID-000187-899c715a` at `:t 27417360`, both citing
`{:module "bronchitis", :state :end-bronchitis-careplan}`), and exits 0
only because **no invariant covers `:care-plan-end`** — `check.clj` has
no analogue of the `:medication-end` rule. That silence is why the
regression gate for this half asserts the shape directly rather than
the exit code.

### The fix

One `cond` clause in `compile_trajectory.clj`'s own loop, placed after
the history/pre-horizon clauses so a pre-horizon end keeps its existing
registration-fact disposition unchanged:

```clojure
(and (#{:medication-end :care-plan-end} event-type)
     (nil? (referenced-event trajectory event)))
(recur more steps registration-facts last-t encounter-closed? straddle-open? suppressed-straddle-spans)
```

Following the existing drop-rule idiom: the clause carries its own
reasoning comment, and the namespace docstring gains a paragraph
stating the principle's extension. **No counter** — every sibling drop
this generalizes (`history-phase?`, the ConditionEnd no-attachment
case, `:supply-list`) carries none either;
`:suppressed-straddle-spans` exists because a SPAN has no other trace,
while the raw, uncompiled trajectory the caller still holds in full is
this event's own glass-box record (AR-1). Mirrored, not invented.

Untouched, per the fences: `check.clj`, citation shape, the
interpreter's nil resolution, and every vendored module (ADR-0071).

### The drop keys on the absent referent, never on straddling

The designed straddle — an order crossed during history phase,
promoted to a registration-time fact, its end landing in horizon — is
the branch `check.clj`'s own invariant docstring carries explicitly,
and it must keep compiling. It does: `referenced-event` resolves
against the **raw** trajectory, which still carries that order, so its
referent is PRESENT. Two guard tests pin this (one per twin) and passed
**before and after** the change.

### Regression shape

Red-before-green captured for all four new unit tests and both new
scenario gates, with real output.

- `compile_trajectory_test.clj` — four tests: the two nil-referent
  drops (red: `[{:type :delay, :from 14400, :to 14400} {:type
  :medication-end, :citation {:module "m", :state :end-rx}}]` — the
  unpaired step itself), and the two designed-straddle guards (green
  throughout).
- `run_test.clj` — two population-scale gates beside the existing
  seed-202 one, both red first: seed 424242 (`:self-check-failed`,
  the reported violation) and seed 5 (whose red output named both
  unpaired `:care-plan-end` events). Each ~13s warm against tracked
  demo configs. The seed-5 gate asserts the **shape**, not the exit
  code, because exit code alone never could have caught it.

**R5 disclosure.** The compile layer has no ground-truth log of its
own — it emits IR steps — so "the checker invariant is clean over the
resulting log" cannot be asserted at that layer under any scaffold.
Rather than scaffold a synthetic one, that half is discharged by the
two `run_test.clj` gates, which run the **real** pipeline end to end
through `check-all`. Stronger evidence than a scaffolded assertion,
and stated here rather than left implicit.

### Blast radius: one shared RNG, disclosed

`engine.clj:1490` seeds **one** `Random` for the whole run, consumed in
work-queue order. `decide :delay` (`engine.clj:414-421`) draws
unconditionally, even when `:from` equals `:to`. So dropping an
end-step that had a nonzero preceding gap also drops its `:delay`, and
that removes one draw from the shared stream — reshuffling every
subsequent decide across **all** patients. Measured, both seeds:

| seed | events before | after | change |
| --- | --- | --- | --- |
| 5 | 365 | 363 | **exactly** the two `:care-plan-end` events; every other kind identical (both had a zero preceding gap, so no `:delay` and no draw removed) |
| 424242 | 357 | 343 | the unpaired `:medication-end` **plus** a population-wide reshuffle: `:admission` 1->0, `:discharge` 1->0, `:diagnostic-report` 4->0, `:observation` 16->10, `:procedure` 30->28, `:outpatient-visit` 37->38, `:care-plan-start` 10->9 |

This is inherent to the engine's single-shared-RNG design — the same
blast radius **any** step-composition change has here — not a property
of this fix, and not something a fence permits working around (no seed
special-casing, no RNG change). It is recorded because "seed 424242
now exits 0" is a weaker claim than it looks: that run's population
genuinely differs from the pre-fix one.

### Oracle sweep

`make docsgen`, exit 0: **no artifact changed at all** — not
`demos/traces/emit-state/ground-truth.edn`, not
`demos/traces/order-result/ground-truth.edn`, not
`components/sim-engine/resources/sim-engine/event-examples.edn`, not
any other generated path. Predicted before running, and the prediction
holds for a readable reason: the only committed trace carrying these
events is `demos/traces/module-mix/ground-truth.edn`, whose 180
`:medication-end` events **all** carry a non-nil `:references` (and it
has no `:care-plan-end` at all), so nothing is dropped and no draw is
removed. `event-examples.edn`'s fixtures cite orders and plans that do
fire.

### Error ledger

- **Original landing — unearned specificity.** The end-step compile
  assumed a MedicationEnd's referenced order always fired on the same
  walk. It does not: a `referenced_by_attribute` naming a submodule
  the walk never entered resolves to nothing, and the code had no
  branch for that.
- **Design channel, this arc — unearned specificity.** The observed
  failure was attributed to the unscoped decide-time citation scan
  (ADR-0164) from mechanism plausibility, without tracing the
  violating event. The trace shows that scan was never reached: the
  violating event carries `:order-citation nil`, so the scan is
  guarded out entirely. Both defects are real; only one caused this.

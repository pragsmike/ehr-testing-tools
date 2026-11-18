# The second ground-truth rebaseline: HALTED at step 5 -- the reference fix is built and its prediction held; dense-7500 cannot take authored inpatient dispenses and keep the erasure law

2026-11-18. Base `164fae67`. `bin/preflight` exit 0, no findings (HEAD
matched origin/main; tree clean; HEAD not tagged stable-*, disclosed;
last five CI runs green).

**NOTHING IS PUSHED.** This session's commits are local on `main`,
ahead of `origin/main`. Steps 1 to 4 are done. Step 5's invariant
cannot hold as written, which the prompt makes a HALT; steps 5 to 8
were not done, and no ADR was written. The next session's preflight
will report HEAD ahead of origin until the author rules.

## Why nothing is pushed

The ruling of 2026-11-18 ("3 accept") is one dense-7500 change, one
notice, one new pin. The fix alone moves the dense cells (measured
below), so a tip carrying the fix without step 6 would carry consumer
figures that are false and an integration tier that is red; and a tip
carrying the fix WITH regenerated figures would be the first of two
pins. Neither is what was ruled. The commits are held so the author
can choose.

The suite at the fix is also red, on pins that are owed a re-pin
inside the rebaseline and were not re-pinned here (below).

## The HALT: step 5's erasure law fails, for two separate reasons

The prompt: `dense-inpatient` gains one authored `:medication-dispense`
step per order, "NO new delay step and no draw", and "the erasure law
holds on the 750 cell across this commit -- delete the dispenses,
renumber, and step 3's log is left, event for event."

It does not. Measured at `0c4bc797` with scratch configs outside the
tree (`config.edn` plus the two steps, placed where the prompt says;
with and without `:dispense true`), seed 20260824, 750 patients:

| run | baseline events | with the steps, dispenses deleted | equal | first difference |
|---|---|---|---|---|
| `--churn`, the gated cell | 33,296 | 33,379 | no | index 41 |
| no churn | 32,279 | 32,279 | no | index 399 |

**Reason 1, with churn: an authored step costs draws.**
`churn/inject` rolls every churn type at every gap, and n steps have
n+1 gaps (`churn.clj`, `roll-gap` and `inject`; the namespace docstring: "every churn step type draws exactly one
`.nextDouble` at every insertion point"). Two more steps are two more
gaps, twelve more draws from that patient's stream, before any step is
decided. Every later draw of that patient moves. The erased log is not
a permutation of the baseline: 21,718 of the baseline's events have no
equal in it, and the sequence of instants differs.

**Reason 2, without churn: an authored step costs a queue turn.** The
erased log is then exactly the baseline's events -- the same multiset,
log indices masked, and the same sequence of instants -- in a
different order at 372 positions. Each step is a decide, and after
each decide the patient's remaining steps are requeued with a fresh
sequence number. One more decide puts that patient's NEXT event behind
any other patient's entry queued in between at the same instant. The
first difference is that: a `:order-placed` at `t` 9420 changes places
with another patient's `:appointment` at `t` 9420.

ADR-0187's derived fills do not have reason 2: the fill is queued
after the close, and the patient's remaining steps are requeued in the
same breath, so nothing gets between them.

**The `:dispense` key alone does hold**, on dense as on its own roots
(step 4 below). Only the authored half fails.

### What the author could rule (not chosen here)

- **Accept that dense is reshuffled by the adoption.** Drop step 5's
  erasure invariant for the authored half; state the equality for the
  key alone. The notice then says dense is a new population, which the
  `--churn` cells are.
- **Derive the inpatient dispenses too.** A second rule under the key,
  queued the way the community fill is. It would hold the erasure law.
  It contradicts the ruling of 2026-11-17 that inpatient dispenses are
  authored steps.
- **Change the engine so the step is free.** Churn would have to open
  no gap beside a `:medication-dispense`, which bends the
  fixed-consumption law `churn.clj` states, and the step would have to
  take no queue turn, which no step does today. Reason 2 is the harder
  half.
- **Turn on the key in dense and author nothing.** Dense then carries
  community fills only: 3 at the 750 cell, 48 at 7,500.

## Step 1 -- measured before any edit

Scripts at the end. Tree untouched: the judge rebinds
`gmf-interpreter/run-module` and reads trajectories.

### (a) Where first-match differs from latest-open

Every MedicationEnd, ConditionEnd and CarePlanEnd in every distinct
walk of all 45 oracle roots. `:differs` -- the reference is not the
latest opening of its state that no earlier end (under the new rule)
has taken. `:none-open` -- every opening of the state is already
taken. `:nil` -- the end carries no reference. `:same` -- the two
rules agree.

| root | ends | differs | none-open | nil | same | what differs |
|---|---|---|---|---|---|---|
| bronchitis | 6,184 | 1,766 | 0 | 2,054 | 2,364 | condition `bronchitis-subsides` |
| demographic-fold | 1,185 | 108 | 160 | 835 | 82 | medication `sinusitis end-antibiotic` |
| dermatitis | 35 | 2 | 0 | 16 | 17 | medication `end-corticosteroid` |
| ear-infections (batch) | 367 | 51 | 0 | 0 | 316 | condition `ear-infection-end` |
| ear-infections-engine | 1,030 | 142 | 0 | 0 | 888 | condition `ear-infection-end` |
| ear-infections-history-engine | 1,030 | 142 | 0 | 0 | 888 | condition `ear-infection-end` |
| injuries | 7,340 | 816 | 0 | 3,613 | 2,911 | condition, 7 `end-*-onset` states and `end-shoulder-injury`, `end-whiplash-injury` |
| med-rec | 18,097 | 17,965 | 0 | 0 | 132 | condition `end-medication-review-due` |
| sinusitis | 247 | 10 | 22 | 194 | 21 | medication `end-antibiotic` |
| total-joint-replacement-engine | 410 | 110 | 0 | 0 | 300 | care plan `end-post-op-careplan` |
| veteran-prostate-cancer | 279 | 142 | 0 | 0 | 137 | condition `end-suspected-prostate-cancer` |
| urinary-tract-infections-engine | 673 | 0 | 3 | 359 | 311 | -- |
| urinary-tract-infections-history-engine | 681 | 0 | 3 | 369 | 309 | -- |

The other 32 roots hold no `:differs` and no `:none-open`: each holds
no such end at all, or only `:same` and `:nil` ones.

So **all three end types are affected**, across eleven roots, not
`sinusitis :end-antibiotic` alone. Condition ends are the
bulk: `med-rec` alone has 17,965.

Dense, `--churn`, seed 20260824:

| cell | ends | differs | none-open | modules |
|---|---|---|---|---|
| 750 | 786 | 324 | 18 | med_rec 273, bronchitis 20, injuries 20, sinusitis 11 |
| 7,500 | 7,452 | 3,310 | 125 | med_rec 2,859, bronchitis 222, injuries 145, sinusitis 84 |

**What an end does today when nothing of its state is open.** Two
cases, both now asserted by `end-reference-test`. Every opening
already ended: it references the state's FIRST opening again. The
state never fired: no reference. A `:nil` in the table is mostly a
third thing -- an end that names its opening by codes or by attribute,
which never went through the function at all.

### (b) Visits holding two medication orders of one citation

| cell | in-log orders | in an admission | in an outpatient visit | encounters with two orders of one citation |
|---|---|---|---|---|
| 750 | 492 | 489 | 3 | **0** |
| 7,500 | 4,986 | 4,938 | 48 | **0** |

Zero, so no HALT here. ADR-0187's first finding has no instance in
dense.

### (c) The prediction

Written into commit `c8b83cc1`'s message, before the fix existed.

DIFFERS, 10: bronchitis, demographic-fold, dermatitis, ear-infections,
ear-infections-engine, injuries, med-rec, sinusitis,
total-joint-replacement-engine, veteran-prostate-cancer.

IDENTICAL, 35: every other root. Three were not obvious.
`ear-infections-history-engine` holds the same 142 differing ends as
`ear-infections-engine`, but in history mode its log carries no
reference at all, `ear_infections.json` routes on no `active`
condition, and the one differing end inside a compiled visit moves
from one history-phase onset to another, so it is dropped either way.
Both `urinary-tract-infections` roots hold only `:none-open` ends,
which keep the reference they have.

## Step 2 -- red (`c8b83cc1`)

`ehrt.patient-simulator.end-reference-test`, four tests over
hand-built modules, each run for medication, condition and care plan.
At `164fae67`: 12 failures, 0 errors -- open, end, open, end gave
`[nil 0 nil 0]`; open, open, end, end gave `[nil nil 0 0]`; and the
matching `active` condition was true after both. The fourth test,
nothing open, was green as written: it asserts what step 1 measured.

## Step 3 -- the fix (`0c4bc797`, alone)

`index-of-open-citation`: the latest opening of the state that no end
already in the trajectory references; the first opening when all are
ended; nil when there is none. The three ends by state name call it.
`brick:patient-simulator` exit 0 with no existing test edited.

**The 1690 caller is unchanged.** `death-cause-codes` reads the named
ConditionOnset's `:codes`. Those belong to the state and are the same
whichever firing is read, so first-match was right there.
`index-of-citation` stays for that one caller, and its docstring now
says so.

### The gate: prediction against result

Both instruments, `164fae67` -> `0c4bc797`, `--declared-digest-change`.

`bin/regression-oracle`, exit 1, DIFFERS, 10 roots:

    bronchitis  demographic-fold  dermatitis  ear-infections
    ear-infections-engine  injuries  med-rec  sinusitis
    total-joint-replacement-engine  veteran-prostate-cancer

`bin/ground-truth-bracket`, exit 1, DIFFERS, 9 roots: the same set
less `ear-infections`, a batch root it skips by construction
("coverage: 42 roots carry :ground-truth and are digested; 3
skipped"). "declared-digest-change: yes (soundness: yes outside the
leading docstring)".

**The measured set equals the predicted set, root for root.** The 35
predicted identical are identical, the three non-obvious ones among
them.

### What the fix does to dense 750

Same cell, the base interpreter loaded over the fixed one in one
process:

- 33,296 events both sides. 39 differ: 32 `:registered`, in
  `:pre-horizon-facts` only, and 7 `:outpatient-visit`.
- `:medication-end` facts 108 -> 93. The 15 that went are ends
  `sinusitis` emitted for a medication it had not prescribed.
- No other fact count moved, and no event changed position.

The prompt expected routing changes to move in-log events of affected
patients. At this cell none did beyond those 7 visits.

## Step 4 -- the guard

`config.edn` plus `:dispense true`, a scratch file, at `0c4bc797`:

| cell | events without / with the key | derived fills | carrying an `:encounter-id` | erasure equal |
|---|---|---|---|---|
| 750 | 33,296 / 33,299 | 3 | **0** | yes |
| 7,500 | 167,715 / 167,763 | 48 | **0** | yes |

Zero at both. ADR-0187's second finding has no instance in dense.

## What else the fix moves (found, not re-witnessed)

`make docsgen` at the fix regenerates
`demos/traces/module-mix/ground-truth.edn`: 164 `:medication-end`
occurrences become 83. Committed here as its own commit, since it is
the tree's own generator's output for the code as it stands.

Full `make test` on that tree: **`MAKE_EXIT=2`.** One project fails,
in `components/sim/test/ehrt/sim/run_test.clj`, 9 assertions:

- `arc0-gated-corpora-are-byte-and-value-identical-to-the-pinned-baseline`
  for three of the four gated corpora: `:seed-202-ed-tuesday`,
  `:seed-424242-clinic-decade`, `:seed-5-clinic-decade`;
- `gated-corpora-re-pin-exactly-once-under-the-new-scheme`, the same
  three.

These are the pins a rebaseline re-pins (ADR-0185 did). They were NOT
re-pinned: that belongs with the ADR and the notice. Poly stops at the
first failing project, so what later projects would show is unknown.
`make integration` was not run.

## Where the prompt's premises differed

1. **Step 5's erasure invariant is false**, for the two reasons above.
   "No draw" is not true of an authored step under churn, and "no new
   delay" does not keep the log's order.
2. **Three end types are wrong, not one.** The prompt had this as
   unmeasured; measured, condition ends dominate.
3. **The fix did not visibly re-route dense 750.** 39 events moved,
   all in facts and visit annotations.
4. **The 1690 caller is right as it is**, as the prompt suspected.
5. **`config-bare.edn` is not bare of the pathway.** It is
   `config.edn`'s byte prefix through `:module-horizon-days`, so it
   takes any step authored into `dense-inpatient` and stays bare only
   of the key. The prompt's wording allows this; it matters for which
   cells an adoption moves.
6. **ed-tuesday and clinic-decade move too.** The prompt named dense;
   three gated-corpus pins outside it go red on the fix.

## Scripts

Run as `clojure -M:dev <script> ...` from the workspace root; the
oracle-root judge also needs `components/patient-simulator/test` and
`components/sim-engine/test` on the path for two fixture roots.

### judge (the per-end verdict)

```clojure
(def opening {:medication-end :medication-order :condition-end :condition-onset :care-plan-end :care-plan-start})
(defn judge [traj]
  (let [cut (first (keep-indexed (fn [i e] (when (and (= :encounter-end (:event e)) (not (:pre-horizon e))) i)) traj))]
    (loop [i 0 taken {} out []]
      (if (>= i (count traj)) out
        (let [e (nth traj i)]
          (if-let [otype (opening (:event e))]
            (let [r (:references e)
                  o (when r (nth traj r nil))
                  same (when o (keep-indexed (fn [j x] (when (and (< j i) (= otype (:event x)) (= (:module o) (:module x)) (= (:state o) (:state x))) j)) traj))
                  latest-open (last (remove taken same))
                  verdict (cond (nil? r) :nil (nil? latest-open) :none-open (= r latest-open) :same :else :differs)]
              (recur (inc i) (if latest-open (assoc taken latest-open true) taken)
                     (conj out {:kind (:event e) :state [(:module e) (:state e)] :verdict verdict
                                :pre (boolean (:pre-horizon e)) :before-cut (or (nil? cut) (<= i cut))})))
            (recur (inc i) taken out)))))))
```

Applied to every distinct trajectory a root's own function produces,
collected by `(with-redefs [gi/run-module traced] (root-fn))`, and to
`ehrt.cli.core/sim-run-command` at
`{:seed 20260824 :patients N :churn true :config "demos/scenarios/dense-7500/config.edn"}`
for the dense cells.

### the erasure check

The erase and renumber of
`ehrt.sim-engine.dispense-derivation-test/erase-dispenses`, restated,
applied to a run of a scratch config and compared with `=` to a run of
`config.edn`. The permutation check masks every `*-event-id` integer
and compares `frequencies` of the two logs, then their `:t` sequences.

The scratch configs are `config.edn` with ` :dispense true` inserted
before ` :encounters true`, and, for the authored half,
`{:type :medication-dispense :setting :inpatient :order-citation {:module "dense-inpatient" :state :antibiotic-start}}`
after the delay that follows the transfer and the same step for
`:analgesia-start` directly after its order.

## Close

Not closed. Two local commits beyond the two above: the regenerated
module-mix trace, and this record with the archived prompt. No push,
no `bin/post-push-verify`, no CI run. Background processes this
session started have all exited.

# The second ground-truth rebaseline (ADR-0188): an end references the latest open opening; dense-7500 takes the `:dispense` key, key only, after a HALT at step 5

2026-11-18. Base `164fae67`. `bin/preflight` exit 0, no findings (HEAD
matched origin/main; tree clean; HEAD not tagged stable-*, disclosed;
last five CI runs green).

**This record has two halves.** The session halted at step 5 and
reported; that half is below, unchanged except for this head, and its
"nothing is pushed" was true when written. The author then ruled
**"Key only"** (2026-11-18): dense-7500 takes the engine key and
authors no dispense. The second half, "Resumed under 'Key only'", is
steps 5 to 8 under that ruling.

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

## Resumed under "Key only"

The author's answer to the halt, verbatim: "Key only".

### Step 5 as ruled (`3ba95de9`)

`config.edn` gains `:dispense true`, appended after `:fan-out` as the
tenth opt-in key. The three derivation rules in the scenario README
were re-run and `cmp` clean against the committed siblings;
`config-bare.edn` stays bare of the key. "Nine opt-in keys" became ten
in the configs' header, both READMEs, the Scale table's row labels,
the figures gate's label map and the exerciser's failure text. The
bare prefix is one byte shorter, 154,195, sha-256 `a47c5b8c...`.

Gate, 750 cell, seed 20260824, `--churn`, against the config as
committed at `3a6d150f`:

- 33,296 -> 33,299 events; 492 orders; 3 dispenses, all `:community`,
  0 `:inpatient`, 0 carrying `:encounter-id`.
- Erasure: dispenses deleted and renumbered, equal to the prior log
  event for event.
- `check-all` `:ok` over 33,299 events, 47 invariants.
- `describe` 1.3.0: `:medication-dispense-community` observed 3,
  `:medication-dispense-inpatient` observed 0. Read over a bare log,
  so `:configured` is `:unknown` on every opt-in row.

The prompt's gates ":inpatient dispenses equal dense-inpatient orders"
and "both dispense rows configured and observed" do not apply under
the ruling: nothing inpatient is authored.

### Step 6: figures, once

Measured at `3ba95de9`, provenance method (one warm-up, two timed runs
per cell, `/usr/bin/time -v`, JVM defaults; the 28,024 cell one run
with `-Xmx8g`). Windows-side LoadPercentage before each run: 12, 10,
16; 20, 6, 5; 2, 13, 4; 12, 6, 1; 1. None discarded. All three runs of
every cell wrote a byte-identical `events.edn`.

| cell | t1 / t2 wall | mean | RSS MiB t1 / t2 | events | messages | dispenses | events sha-256 |
|---|---|---|---|---|---|---|---|
| config 7,500 | 167.11 / 150.66 | 158.89 | 1736 / 1865 | 167,763 | 176,973 | 48 | `5cbc248e...` |
| nobed 7,500 | 139.21 / 115.55 | 127.38 | 1546 / 1625 | 125,748 | 134,891 | 48 | `bde46ea5...` |
| bare 7,500 | 52.75 / 61.43 | 57.09 | 1057 / 986 | 101,120 | 65,643 | 0 | `56559a11...` |
| config 750 | 65.76 / 62.55 | 64.16 | 1019 / 1014 | 33,299 | 35,925 | 3 | `99d31898...` |
| 28,024 | 382.87 | -- | 4906 | 671,646 (32,080 subjects) | -- | 57 | `5fb8187b...` (264,575,690 bytes) |

Every event-count movement is the dispenses; the reference rule moved
no count. Messages moved +2, +5, 0, -1, which is the rule's doing: a
dispense renders no message. The walls rose 11 to 26% in every cell,
the bare one included, whose counts did not move; not attributed. The
two timed walls of the two large cells differ by 16 and 24 seconds,
so read the means loosely. Peak RSS at 28,024 rose 3,769 -> 4,906 MiB
under the same `-Xmx8g`; one run, a GC high-water mark, not attributed.

Orders at 7,500: 4,986 in the keyed cells. `describe` over the 28,024
log: `reinstated-stay-without-closer` 4, as before.
`veteran_substance_abuse_treatment` at 7,500 reads 178 / 90, the
example consuming-ground-truth already carries.

`bin/demo-exerciser-dense-7500` then reproduced 167,763 / 176,973 /
33,299 and rewrote `:asserted` itself; its only failure was the
tree-clean postcondition, expected mid-session.

NOT re-measured: the scenario README's five-column carrier table
(dated 2026-09-06 there) and consuming-ground-truth's 2026-08-29
paragraphs, which say so themselves.

### The re-pin (`bc516435`)

Three gated corpora, digests and baselines. Differing events, all
`:registered` `:pre-horizon-facts`: seed-202-ed-tuesday 2 of 1,214,
seed-424242-clinic-decade 15 of 1,774, seed-5-clinic-decade 27 of
1,412. adhd-seed-45 byte-identical.

### Downstream line

Corpora that move: every corpus that walks a module ending a state it
opens more than once -- among shipped scenarios `dense-7500` (all five
cells), `ed-tuesday` and `clinic-decade`; among oracle roots the ten
named above. Why: an end's `:references` now names its own opening,
and `dense-7500`'s keyed cells gain community dispenses (48 at 7,500,
3 at 750, 57 at 28,024). Event schema 1.12.0, unchanged. A retained
corpus is a valid artifact of the earlier pin. The new 28,024 log is
264,575,690 bytes, sha-256
`5fb8187b5aef0bef2f518b61becc5ddc21ce042eafca6bd7f861beef5d8cc6c0`.
`dense-7500` now carries dispenses in the COMMUNITY setting only; the
inpatient half is unruled (ADR-0188, owed next).

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

The halt half closed with nothing pushed. The resumed half's suite,
push and CI results are appended below by the close commit.

### The resumed half: suite and integration

Five runs of the gates over the resumed commits, nothing written into
the tree while any of them ran.

| run | tree | result | cause |
|---|---|---|---|
| `make test` 1 | `1aa912f6` | MAKE_EXIT=2 | `no-visible-adr-token-in-prose-test`: the consumer doc named this ADR inline. Fixed by footnote, `c51ab389`. |
| `make test` 2 | `c51ab389` | MAKE_EXIT=2 | `the-front-door-quotes-the-figures-file-test`: the root README still quoted 1.0552. Fixed, `3bc83631`. |
| `make test` 3 | `3bc83631` | MAKE_EXIT=0 | 30 min 13 s. |
| `make integration` 1 | `3bc83631` | MAKE_EXIT=2 | `bin/usecase-ground-truth-oracle`: "expected 46 invariants checked, got 47". Fixed, `f8df1749`. |
| `make test` 4 | `f8df1749` | MAKE_EXIT=0 | 29 min 21 s. |
| `make integration` 2 | `f8df1749` | MAKE_EXIT=0 | every exerciser "tree clean". |

The third red is not this session's change. The check catalog went from
46 to 47 invariants under ADR-0186, and the pin in
`bin/usecase-ground-truth-oracle`, with the two figures in
`components/corpus/docs/use-cases.edn` and the "46" in
`docs/consuming-ground-truth.md`, stayed at 46. `make test` does not run
that script, so the pin was red on `main` from ADR-0186 until now and
nothing reported it. Corrected here because `make integration` could not
go green past it.

### Commits, on `164fae67`

- `c8b83cc1` red tests and the prediction
- `0c4bc797` the reference rule
- `1fa427c8` the module-mix trace regenerated
- `3a6d150f` the halt record and the archived prompt
- `3ba95de9` `:dispense true` joins the opt-in keys, key only
- `bc516435` three gated corpora re-pinned
- `0640af98` figures, once
- `1aa912f6` ADR-0188 and this record's resumed half
- `c51ab389` the ADR cited by footnote
- `3bc83631` the front door's msg/event figure
- `f8df1749` the 47-deep catalog pin

The CI run id is appended below by the commit that follows the push.

## ADR-0165 — the generator side gets a coverage meter, and its first execution finds that the arc's own fix removed the only exercise of both end types

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-23.

### Context

ADR-0163's defect was invisible to a green 368-namespace suite, and the
reason was never that the invariant was wrong. It was right, and it
fired the moment a run reached it. The problem was the population it
ran over.

Exactly one population-scale self-check gate existed before that arc
(seed 202, 100 patients, `ed-tuesday`); `clinic-decade` had been
live-probed once and never gated. Empirically those gated runs produce
zero-to-one `:medication-end` events. So
`medication-end-references-existing-order-and-follows-it-in-time` had
almost nothing to judge, and nothing anywhere measured that fact.

ADR-0160 gave the JUDGE side a coverage gate — every oracle root is
exercised. This ADR lands the generator-side analogue: a per-push gate
asserting the gated scenario runs COLLECTIVELY produce every
ground-truth event type the vendored closures they name can drive.

### Author rulings (verbatim)

> Coverage instrument: (a) — event-type production coverage gate over
> the gated runs; explicit waivers otherwise.

> P1: (a) — emittable set derived as a declared state-type→event-type
> table, co-landed with a test asserting it matches the interpreter's
> actual dispatch.

> P2: (a) — shared once-fixture generating each gated corpus once, all
> gates reading it. THE one sanctioned improvement this session;
> existing gate assertions move verbatim.

> P3: (a) — a type resisting a bounded seed-hunt is waived with a named
> queue row; gate lands green.

### The P1 table, and the one reading the prompt left open

`ehrt.patient-simulator.emittable-events/state-type->emittable` carries
one row per state type `gmf-interpreter/step` dispatches on — 30 of
them — with TWO columns rather than one. That is a departure from the
literal ruling, and it is forced, not preferred.

P1 names a state-type→event-type table gated against "the
interpreter's actual dispatch." The interpreter's dispatch produces
TRAJECTORY event types. But a trajectory is not observable from a
gated run: `engine.clj:346`'s `decide :registered` walks the module,
compiles the result, and discards the trajectory — only the compiled
steps survive, as ground-truth events. Six trajectory event types can
therefore never appear in any log at all, by construction and not by
seed:

- `:condition-onset`/`:condition-end` compile to an ANNOTATION on an
  already-compiled encounter step (`annotate-condition`), never a
  standalone step — the fact ADR-0163's own Step 2 probe turned on.
- `:supply-list` has an explicit no-IR-step clause, any phase.
- `:allergy-onset`/`:vaccine` reach `compile-trajectory`'s `:else`.
- `:imaging-study` compiles through `procedure->step`, so it is
  INDISTINGUISHABLE from a Procedure in the log.

Gating raw trajectory types would therefore need six permanent
structural waivers — which is not what P3(a) describes ("a type
resisting a bounded seed-hunt"). So each row carries both the
trajectory event the interpreter emits and the ground-truth types that
event reaches through `compile-trajectory` + `decide`. The emittable
set the gate consumes is the second column.

**Both interpreter-facing columns are gated, not asserted.**
`ehrt.patient-simulator.emittable-events-test` reads
`gmf_interpreter.clj` with the Clojure reader (`*read-eval*` false —
the `sim-purity-lint-test` discipline, never a regex over raw text) and
fails on two divergences: the table's key set against `step`'s own
`case` dispatch constants, and its `:trajectory-event` value set
against the event-type keywords the interpreter's own
`emit-and-advance`/`trajectory-event` call sites pass.

**The INVARIANT, demonstrated rather than claimed.** A temporary
`:temporary-mutation-for-adr-0165` clause was added to `step`'s `case`
and the gate run. Real output:

```
FAIL in (table-covers-every-state-type-the-interpreter-dispatches-on)
state types in `step` but not in the table: (:temporary-mutation-for-adr-0165);
in the table but not in `step`: ()
```

Reverted; `git status` clean for that file, verified before proceeding.

**What the `:ground-truth` column is NOT.** It is declared against
`compile-trajectory`'s clause set with a per-row citation, not derived
by a second source walk. Four state types delegate to helpers OUTSIDE
`step`'s own case (`death-step`, `wellness-wait-step`, `guard-step`,
`call-submodule-step`), so a per-branch syntactic walk would silently
under-report `:death` and `:wellness-wait`. Its proof obligation is
discharged empirically instead: the gate reads real corpora, so a row
claiming a type the pipeline cannot produce becomes a permanently
unsatisfiable gate, and a row claiming `#{}` for a type that does
appear shows up in the matrix below.

### The citation filter, and why the gate needs it

An event counts toward coverage only when it carries a `:citation`.
`engine.clj`'s own `citation-fields` attaches one only when the step it
came from carried one, and only `compile-trajectory`'s `->step`
functions ever set it — never a hand-authored `:pathways` step, never a
churn-injected one.

This is load-bearing, not tidiness. Measured: seed-202 over
`ed-tuesday` emits **92 `:admission` and 90 `:discharge` events, of
which ZERO are cited** — all five of its ED pathways are hand-authored,
and its 8-patient module tail produces no compiled content at all (that
scenario's own config header already discloses the low-incidence
mechanism). Without the filter, that run would satisfy
`:admission`/`:discharge` coverage that no vendored module ever
produced, and the gate would be measuring the scenario author instead
of the generator.

### The P2 fixture

One `:once` fixture generates each gated corpus once into `corpora`;
every gate reads it. The three existing gates' assertions moved
VERBATIM — the whole deletion side of that file's diff is the `ns`
require line and three `let` bindings:

```
-  (:require [clojure.string :as str]
-            [clojure.test :refer [deftest is testing]]
-    (let [r (run/run-command {:seed 202 :patients 100 :churn true
-                              :config "demos/scenarios/ed-tuesday/config.edn"})]
-    (let [r (run/run-command {:seed 424242 ...})]
-    (let [r (run/run-command {:seed 5 ...})]
```

Not a saving today — three gates, three runs, before and after. It
exists so the coverage gate reads the SAME corpora the self-check gates
judge instead of generating a second copy of all three.

### Step 3's measurement, and what it found

RED first, before any hunt (real output):

```
emittable by the gated scenarios' own modules but produced by NO gated corpus:
[:care-plan-end :medication-end]
Produced: [:admission :care-plan-start :diagnostic-report :discharge
           :medication-order :observation :outpatient-visit
           :outpatient-visit-end :procedure]
```

**The two missing types are exactly the two ADR-0163's drop rule
removes.** The prompt predicted `:medication-end`; `:care-plan-end`
came with it. That arc made seed 424242 clean by dropping its only
`:medication-end`, and made seed 5's log honest by dropping both of its
unpaired `:care-plan-end` events — and in doing so removed the only
population-scale exercise of both end types anywhere in the per-push
suite. The coverage gate's first execution is what says so.

### The hunt (step 5)

Bounded, and logged. 180 `clinic-decade` variations (seeds 1-60 ×
{25, 50, 100} patients, ~84s):

- `:medication-end` — found readily; 9 hits, smallest seed 35 @ 25
  patients, self-check clean, 0 unpaired.
- `:care-plan-end` — **zero hits in 180 runs.** Read off the modules
  rather than left as bad luck: of `clinic-decade`'s twelve, only
  `asthma`, `bronchitis` and `total_joint_replacement` carry a
  CarePlanEnd, and asthma/bronchitis reach theirs ONLY through
  `referenced_by_attribute` (`asthma_careplan`, `bronchitis_careplan`)
  — the never-written-attribute route ADR-0163 now drops. Seed 5's two
  historical `:care-plan-end` events were of exactly that kind.

So the covering run had to come from a module those scenarios do not
name. Single-module sweeps (7 modules × 40 seeds, ~69s) found
`dermatitis`, `rheumatoid_arthritis` and `attention_deficit_disorder`;
minimizing over ADD (7 patient counts × 60 seeds, ~17s) gave the run
landed:

**`attention_deficit_disorder`, seed 2, TEN patients, ~19-25ms** —
produces both missing types, both PAIRED, self-check clean.

Its shape is worth naming, because it is not the ordinary case:

```
{:event :care-plan-end  :start-event-id nil
 :care-plan-citation {:module "attention_deficit_disorder" :state :adhd-careplan}}
{:event :medication-end :order-event-id nil
 :order-citation     {:module "attention_deficit_disorder" :state :ritalin}}
:pre-horizon-facts on that patient's own :registered --
  {:event :care-plan-start   :citation {... :state :adhd-careplan}}
  {:event :medication-order  :citation {... :state :ritalin}}
```

One patient whose ADHD care plan and Ritalin order both fall in history
phase, with both ends landing in horizon: the DESIGNED pre-horizon
straddle. So this run does not merely tick two boxes — it is the first
population-scale exercise of both end-invariants' pre-horizon escape
branch, which until now lived only in scripted fixtures.

### The matrix, as landed

Cell = cited (compiled-content) events of that type in that run; `-` =
that run's own closure cannot emit it; `0` = emittable there, produced
zero times.

| event type | seed-202 ed-tuesday | seed-424242 clinic-decade | seed-5 clinic-decade | adhd seed-2 | covered |
| --- | --- | --- | --- | --- | --- |
| `:admission` | 0 | 0 | **1** | 0 | YES |
| `:care-plan-end` | 0 | 0 | 0 | **1** | YES |
| `:care-plan-start` | 0 | **9** | **5** | 0 | YES |
| `:diagnostic-report` | - | 0 | **1** | - | YES |
| `:discharge` | 0 | 0 | **1** | 0 | YES |
| `:medication-end` | 0 | 0 | 0 | **1** | YES |
| `:medication-order` | 0 | **20** | **21** | 0 | YES |
| `:observation` | 0 | **10** | **25** | - | YES |
| `:outpatient-visit` | 0 | **38** | **46** | 0 | YES |
| `:outpatient-visit-end` | 0 | **38** | **46** | 0 | YES |
| `:procedure` | 0 | **28** | **17** | 0 | YES |

| run | wall | ok | total events | cited events |
| --- | --- | --- | --- | --- |
| seed-202 ed-tuesday | 1,040ms | true | 407 | **0** |
| seed-424242 clinic-decade | 2,025ms | true | 343 | 143 |
| seed-5 clinic-decade | 1,352ms | true | 363 | 163 |
| adhd seed-2 | 19ms | true | 12 | 2 |

**Waivers: NONE.** All 11 emittable types are produced. `coverage-waivers`
lands as an empty map with its contract documented, so the first type
that ever needs one has a declared home.

### Two holes the gate is green over, rowed rather than waived

Both are DISCLOSED here because a green gate is exactly where a
weakness hides.

1. **seed-202 `ed-tuesday` contributes nothing.** It declares 10
   emittable types and covers zero of them — 407 events, none cited.
   The gate stays green because it asks for a UNION across corpora, not
   per-run coverage. Rowed as `roadmap.md#ed-tuesday-module-tail-inert`.
2. **Three types are one event deep.** `:admission`, `:discharge` and
   `:diagnostic-report` are each covered by exactly ONE cited event in
   exactly ONE run (seed 5). That is the same one-root-deep fragility
   ADR-0156 named for the oracle's capacity witness. Rowed as
   `roadmap.md#generator-coverage-depth`.

### Suite-time delta

`ehrt.sim.run-test`: 26 tests / 87 assertions → 27 / 89; namespace wall
13.7s (the new run adds ~19ms; the fixture adds no fourth generation of
anything already generated). New namespace
`ehrt.patient-simulator.emittable-events-test`: 3 tests / 5 assertions,
pure source reading, no simulation. Whole-suite figures are in this
session's own record.

### Oracle sweep

`make docsgen`, exit 0. ONE artifact moved, and it is not the oracle:
`.agents/state-derived.md`'s generated test-namespace count, 198 → 199,
the mechanical consequence of adding one test namespace. No
`demos/traces/**` ground truth changed, no
`event-examples.edn`, no event-schema export. Predicted, and for a
readable reason: this commit adds a test, a data table and a fixture
relocation, and changes no engine, compile or interpreter behavior at
all.

### Error ledger

- **The design channel's own gap, named.** The prompt specified a
  state-type→event-type table without stating which event vocabulary,
  and the two candidate readings are not interchangeable — one of them
  is unsatisfiable without six structural waivers. Resolved by
  measurement (the trajectory is discarded at `engine.clj:346`, so only
  the ground-truth vocabulary is observable from a gated run) and
  recorded here rather than silently picked.
- **A gate that would have measured the wrong thing.** The first
  formulation counted every event of a type. ed-tuesday's 92
  hand-authored admissions would have satisfied `:admission` coverage
  with zero module content behind it. The citation filter is the
  difference between a generator-side meter and a scenario-author-side
  one, and it was found by looking at the numbers, not by reasoning
  about them.

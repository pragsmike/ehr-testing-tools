# Engine namespace extraction, 10 of N: the `run` cluster, and the facade

Session record, 2026-08-30. HEAD at start `cd7302e`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5). Author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, private movers widen ONLY where a call site
stays behind, no test file changes), C4(b) (2026-08-30 -- the ruling
that decided this extraction, answering the question the ninth
session's section 6 put to the author), and S1(a) (an equivalence proof
replaces red-before-green).

This is the LAST of `engine.clj`'s ten clusters. After it `engine.clj`
holds no executable code of its own.

`bin/preflight` exit 0, **no findings** -- the first clean preflight of
the program, because this session's own first act was the derivation
rather than an edit. One DISCLOSED line, expected: "HEAD is not
currently tagged stable-*" (tags retired by the de-scaffold ruling of
2026-08-25).

## 1. Step 1 -- the tip, the movers, the markers, the edges

Everything below was re-derived from THIS tree, with Clojure's own
reader for spans and a whole-symbol scanner for edges. The census's
numbers are at `517a96d`, **nine** extractions ago.

`engine.clj` is **1,968 lines / 47 top-level forms plus the `ns`** at
`cd7302e`, which is the figure the extraction-9 record closed on,
unchanged.

### The mover set is exactly six, and it is the whole of the real code

Derived, not transcribed: of the 47 forms, 41 are delegating defs whose
whole body is `alias/name`, and SIX are real. The six, with spans and
markers, every one confirming the ninth record's own measurement to the
line:

| form | span | lines | marker |
|---|---|---:|---|
| `pop-min` | 574-580 | 7 | `defn-` |
| `placeholder-registration` | 645-675 | 31 | `defn-` |
| `select-person` | 677-704 | 28 | `defn-` |
| `prelude` | 706-1316 | 611 | `defn-` |
| `person-plan` | 1318-1373 | 56 | `defn` |
| `run` | 1375-1968 | 594 | `defn` |

**1,327 lines in two non-contiguous regions** -- `pop-min` alone
between `assign-module` and the `config` banner, the other five in one
block running to the end of the file. Two public, four private.

### `interface.clj`: two of the four the prompt named are movers

The prompt expected `run`, `person-plan`, `compile-patient` and
`valid-persons?` to resolve through `engine`. All four do, and the
derivation splits them: `run` (`interface.clj:45`) and `person-plan`
(`:80`) are MOVERS and need new delegating defs; `compile-patient`
(`:62`) and `valid-persons?` (`:82`) are ALREADY delegating defs, left
by the ninth and seventh extractions, and are untouched. `interface.clj`
was not edited, per the fence.

### Test-file call sites: the largest surface any mover has had

`engine_test.clj` calls `engine/run` at **forty-nine `(engine/run ...)`
call forms** (53 textual occurrences, four of them prose), and
`persons_test.clj` calls `engine/person-plan` at **seven**. Both files
alias `ehrt.sim-engine.engine`, verified rather than assumed. C1(a)
forbids touching either, so both delegating defs are load-bearing twice
over -- once for `interface.clj`, once for the test tree.

The four private movers have **no reference anywhere outside
`engine.clj`'s own text** except prose: `person_fold.clj` names
`engine/prelude` twice (repointed, section 2), `decide.clj` names
`prelude` in three sentences (repointed), and
`sim/run_test.clj:1588` names `engine/placeholder-registration` --
FENCED, and section 2j's one addition to the backlog.

### Outgoing edges: the driver touches everything, and one edge is new

Scanned whole-symbol over the mover text with strings and comments
stripped. Qualified edges, all pre-existing: `sim-model` (9),
`person-fold` (9), `result`/kernel (3), `streams` (2), `log-index` (2),
`decide` (1), `churn` (1), `order-profiles` (1), `encounters` (1),
`fold` (1). Plus `java.util.Random`, used by `select-person`'s and
`prelude`'s type hints and by nothing in the residue.

**The census expected `patient-simulator` and it is not there.** Zero
occurrences in the movers AND zero in the residue: `engine.clj` has
carried a dead `[ehrt.patient-simulator.interface :as patient-simulator]`
require since the ninth extraction took `decide`, along with dead
`[malli.core :as m]` and `[malli.util :as mu]`. Pre-existing, disclosed,
NOT fixed -- see section 6's requires note.

### FOURTEEN bare names in the movers resolve in `engine.clj`

This is the finding that shaped the whole session. Scanned name by name
against the 47-form list:

| name | sites | resolves to |
|---|---:|---|
| `stream` | 4 | `streams/stream`, via the delegating def |
| `patient-id-for` | 1 | `streams/` |
| `assign-pathway`, `assign-module` | 1 each | `assignment/` |
| `compile-patient`, `decide` | 1 each | `decide/` |
| `delivery-stay-minutes` | 2 | `decide/` |
| `injury-stay-minutes` | 2 | `decide/` |
| `unidentified-stay-minutes` | 1 | `decide/` |
| `valid-persons?`, `valid-scheduling?` | 1 each | `config/` |
| `initial-patient` | 1 | `state/` |
| `evolve` | 1 | `evolve/` |

The prompt says "anything still resolving in `engine.clj` is
stop-and-report". Read literally that is eighteen call sites and the
session ends here. Read as the prompt's own sentence means -- a mover
reaching a form that STAYS BEHIND as real code -- it is zero, because
nothing real stays behind. What these eighteen reach is delegating
defs, and the whole question is whether they can keep reaching them.

**They cannot, and the reason is a law rather than a preference: a
facade may require its implementations, an implementation may not
require its facade.** `engine.clj` must require `ehrt.sim-engine.run`
to build `(def run run/run)`, so `ehrt.sim-engine.run` may not require
`ehrt.sim-engine.engine`. Every earlier cluster kept its bare names for
one reason nobody had to say out loud: `run` STAYED, and could reach
the defs directly. This is the first move where the CALLER travels.

So fourteen of the eighteen are qualified to the namespace that owns
the form -- the same object the delegating def holds, asserted
`identical?` live for each. The other four are `stream`, and they are
census constraint 1.

### Constraint 1, and the two things the census got right and one it did not

`engine_test.clj:2505`
(`mutating-one-patients-stream-seed-moves-only-that-patient`) perturbs
the RNG partition with `with-redefs` on the var
`ehrt.sim-engine.engine/stream`. Confirmed at this tip, and confirmed
that it is the ONLY `with-redefs` in the whole tree that names an
`engine` var: `person_simulator/consumption_test.clj:44`/`:142` redefine
`ehrt.sim-engine.interface/stream` -- a different var, exactly as the
census says, verified by reading that file's own `ns` alias -- and no
test anywhere uses `alter-var-root` or `intern`.

What the census did NOT anticipate is its own remedy. Constraint 1 ends
"the fix is to keep the delegating var and leave the call sites
unqualified", which silently assumes `run` stays in `engine.clj`. This
move is the case that assumption excludes: the call sites leave, the
var stays, and a `:require` back is a cycle. **Census correction, and
the first one that is about a constraint rather than a count.**

## 2. Step 2 -- the constraint-6 sweep (`3739084`)

Run at all four levels before the move.

### 2a. Level 2 -- thirteen repoints in eight files

Every one states something true at `cd7302e` and false the moment the
forms leave, and every one is of a class no delegating def can forward.

**PATH claims** (`engine.clj` plus a mover):

| file | text |
|---|---|
| `docs/dev/simulator-architecture.md:56` | the `sim-engine` row's "`run` -> `engine.clj`'s `defn run`" |
| `docs/dev/simulator-architecture.md:154` | "the one deliberate impurity ... (`engine.clj`'s own `ns` form: `(:import [java.util Random])`)" |
| `oracle/digest.clj:291` | "engine.clj's own `:history` default `false`" |
| `ed-tuesday/config.edn:25` | "the documented module-only-patient pattern, engine.clj's own `run` docstring" |
| `ed-tuesday/config.edn:115`, `config-latency.edn:94` | "(engine.clj's own module-only-patient pattern)" |

**NAMESPACE claims about a PRIVATE mover** -- unforwardable for the
other reason, that constraint 5 forbids the def that would forward
them: `person_fold.clj:178` and `:205`, both naming `engine/prelude`.

**SIBLING BANNERS whose subject leaves**: `decide.clj` three times (the
three `*-stay-minutes` tables' only caller; the one forced widening's
caller; and the list of names that "stay behind", which named `run` and
`prelude`), `evolve.clj` once, `log_index.clj` once.

### 2b. One pre-existing error corrected, because the sentence had to move anyway

`log_index.clj:41` said "`engine.clj`'s thirteen remaining call sites
are `log-index/`-qualified instead". That was already wrong at
`cd7302e`: eleven of the thirteen left with the ninth extraction, and
`decide.clj` holds exactly eleven `log-index/` call forms today,
counted. The last two are in `run`'s in-loop fold and leave here. The
sentence now reconciles 11 + 2 = 13 and says `engine.clj` holds none.

### 2c. Level 1 -- 2,387 phrases against 14,818 files, and no citation

The moved text's docstrings and comments were cut into 2,387 six-word
shingles and searched across the whole non-frozen tree. Hits in 24
files; every one read.

* The dominant pattern is again the SHARED BANNER `;; ARC 3B SWEEP <n>
  (ADR-0174 ...)`, in `check.clj`, `sim/run.clj`, `config.clj`,
  `decide.clj`, `encounters.clj`, `state.clj`, three `sim-model` files
  and three demo configs. It names an ADR and an arc, never a path and
  never a mover.
* The second is SHARED PROSE between siblings, and the nearest miss of
  the session is `sim-engine/interface.clj` (12 phrases): its
  `person-plan` comment restates ruling C1's ordering argument in
  `prelude`'s own words -- "the compiled trajectory's death instant is
  a t0 parameter of the process that produces the person stream". It
  pins no path and makes no positional claim, so it stays TRUE, which
  is fortunate: this session's fence forbids editing it.
* `person_fold.clj`, `persons_test.clj`, `operational-models.md`,
  `patient-state-model.md`, `sim-theory.edn` and `event_fleet.clj` each
  share one to a few sentences. None is paired with a path.

### 2d. Level 3 -- the registries

* **`hand-owned-assets.edn`.** All five rows' sources read. The tripwire
  fires on `gt-emitters.svg`, source `docs/dev/simulator-architecture.md`.
  **Fifth fire in six extractions**, predicted by the prompt by name.
  `two-clocks.svg` shares that source and does NOT fire: claim (d)
  reads `:verdict :fresh` rows only, and that row is already `:stale`
  with a live roadmap anchor -- read in the test rather than assumed.
* **Both charter registers.** `person-simulator/docs/limitations.md`
  rows 1 and 10 and `patient-simulator/docs/limitations.md`'s row pin
  `streams.clj` and `compile_trajectory.clj`, repointed by the FIRST
  extraction and by their own component. Constraint 6's original class
  does not arise. Constraint 7's bare token scan is over
  `person-simulator`'s own `src` and is untouched.
* **`exercised-sources.edn`.** Pins `demos/scenarios/ed-tuesday/README.md`
  and `clinic-decade/README.md` -- not the `config.edn` files this
  sweep edited. Untouched.
* **No surface in the tree enumerates `sim-engine`'s namespaces**, so a
  tenth namespace needs no registry row. Checked by searching for
  `sim_engine/decide.clj`, `assignment.clj` and `config.clj` as paths:
  zero hits outside frozen directories.

### 2e. What the sweep left alone, and why

Three claims in the residue's own banners were ALREADY FALSE at
`cd7302e` and are recorded, not repaired
(`rulings.md#R-move-not-improve`), because this move neither causes nor
worsens them:

* The `state` banner's "Its three call sites below -- `decide
  :observation`, `evolve :observation`, `evolve :diagnostic-report` --
  are `state/`-qualified instead": those sites left with the fourth and
  ninth extractions.
* `event_schema.clj:443`'s "`engine.clj`'s one result-construction
  site": that site is `decide.clj:1439`, since the ninth extraction.
* `gmf-interpreter.md:399`'s "`engine.clj`'s own header already states
  'this project's encounter-horizon discrete-event engine has no
  equivalent tick loop'": that sentence lives in
  `components/sim/docs/patient-state-model.md:80` and never lived in
  `engine.clj`. A misattribution, not a staleness.

### 2f. Level 4 and the gate

`make test` GREEN, 21m35s, exit 0 over the sweep's own tree -- which is
NOT evidence the tripwire would stay green, for the reason the P5 row
already states and this session paid: the test reads `git log -1` on the
SOURCE and cannot see an uncommitted edit. It went red the moment the
sweep was committed, and the run aborted there.

`make state-derived` run LAST, after the final hand edit -- the ninth
extraction's own mechanical lesson. A no-op: both
`simulator-architecture.md` repoints are line-neutral, so `:onboarding`
stayed at 1512 of 1530.

### 2g. Twelve test-file citations stay stale, plus one new

C1(a) forbids touching test files. The ninth extraction's twelve stand,
and this session adds one of the same rare class it named as new:
`components/sim/test/ehrt/sim/run_test.clj:1588`, "`engine/placeholder-
registration` carries the ..." -- a NAMESPACE claim about a PRIVATE
mover, which no delegating def can forward. The fenced-citation backlog
is the ruled repoint pass's largest single artefact.

## 3. Step 3 -- `ehrt.sim-engine.run` (`bdbd319`)

### The name, collision-checked

`clojure.core` has `run!` and no `run`, so `(defn run ...)` needs no
`:refer-clojure :exclude` and `run!` stays reachable unqualified inside
the new namespace. In `engine.clj` the namespace is aliased `run` while
a var named `run` is also defined; aliases and vars live in separate
tables, so `run/run` resolves through the alias and `run` through the
var. That is not a novelty -- `(def decide decide/decide)` has had the
same shape since the ninth extraction -- so the collision is a
precedent, not a risk. Chosen: `ehrt.sim-engine.run`, as the prompt
named it.

### The moved body diffs as FOURTEEN lines, and no others

Verified by diffing the body as a BLOCK against `3739084`'s own
`engine.clj` (regions `574-580` and `645-1968`, 1,332 lines with the
region separator), not inferred from hunk headers. `diff` reports 14
changed lines and nothing else -- filtering the diff for any line NOT
naming one of the fourteen qualified symbols returns empty.

All fourteen are the same change: `patient-id-for` -> `streams/`;
`assign-pathway`, `assign-module` -> `assignment/`; `compile-patient`,
`decide`, `delivery-stay-minutes` (x2), `injury-stay-minutes` (x2),
`unidentified-stay-minutes` -> `decide/`; `valid-persons?`,
`valid-scheduling?` -> `config/`; `initial-patient` -> `state/`;
`evolve` -> `evolve/`.

**Not one docstring or comment line differs.** Every interior
positional claim the moved text carries -- "`person-plan` (below)",
"`compiled-patients` below", "the loop below", "the four indexes
above", nineteen of them -- survives unedited, because the six forms
are over there in the order they stood in here. That is the cleanest
prose result of the ten moves, and it is a consequence of moving a
CONTIGUOUS TAIL rather than a scattered cluster.

### The shim, which is the one form that did not move

```clojure
(def ^:private engine-stream
  (delay (requiring-resolve 'ehrt.sim-engine.engine/stream)))

(defn- stream [master family id-tag]
  (@engine-stream master family id-tag))
```

`requiring-resolve` returns the VAR; the delay caches the var and not
its value; invoking a Var derefs it on every call. So a `with-redefs`
in force during the call is seen exactly as it was when these lines
stood in `engine.clj`, and `prelude`'s and `run`'s four `stream` call
sites stay bare -- verbatim, which is why they are not among the
fourteen. It adds no type hint, for the same reason census constraint 3
gives for `streams/stream` being unhinted.

### Asserted live under `-M:dev`, not argued

**Sixteen seam identities**, every one `true`: `engine/run` =
`run/run` = `sim-engine/run`; `engine/person-plan` = `run/person-plan`
= `sim-engine/person-plan`; and each of the ten distinct qualification
targets (`decide/decide`, `evolve/evolve`, `assignment/assign-pathway`,
`assignment/assign-module`, `config/valid-persons?`,
`config/valid-scheduling?`, `state/initial-patient`,
`streams/patient-id-for`, `decide/compile-patient`, the three
`*-stay-minutes`) `identical?` to the object `engine.clj`'s def holds.

**Constraint 1, BOTH DIRECTIONS**, because the positive half alone
would not be evidence:

* `with-redefs` on `engine/stream` IS seen by `run` -- all five streams
  pass through it (`[:world 0] [:patient 0] [:patient 1] [:patient 2]
  [:facility 0]`) -- and perturbing one moves the output.
* `with-redefs` on `streams/stream` is NOT seen -- zero calls. The
  negative control that makes the first result mean something.

**Constraint 5's prohibition**: `engine/pop-min`, `engine/prelude`,
`engine/select-person` and `engine/placeholder-registration` do not
resolve; all four are `:private` in `ehrt.sim-engine.run`, as are the
two shim forms; and that namespace's entire public surface is
`{person-plan, run}`. ALL FOUR private movers stay `defn-` -- the
`weighted-pick` precedent in its limiting case, since after this move
nothing stays behind that could call anything.

**The facade**: `ehrt.sim-engine.engine` interns 43 vars, 43 of them
public and every one a delegating def. Zero private interns, zero real
forms.

### The residue's banners, brought true in the MOVE commit

Nine claims in `engine.clj`'s own eight comment blocks go false with
this move, and all nine were fixed HERE rather than in the sweep --
because restating them a commit early would have made them false in the
interim, which is the ninth extraction's own rule for prose inside the
moving text, applied to prose ABOUT it. They are: the `encounters`
block's "every call site below"; `stream`'s own docstring (rewritten to
carry the shim, and to say **do not retire this def**); the `decide`
block's producer paragraph and both its `prelude` paragraphs; the
`evolve` block's "`run` below still calls `evolve` unqualified"; the
`fold` block's `update-beds` site; the `assignment` block's two call
sites; and the `config` block's two guards.

### Gates, and the suite delta explained to the assertion

`clojure -M:poly check` OK. `bin/regression-oracle 3739084 bdbd319`:
**IDENTICAL: every root's digest matches**, 41 roots,
`declared-digest-change: no`.

`make test` at the close-out tree is GREEN, exit 0, and its delta
against this session's own pre-move green run is **+2 assertions over an
unchanged 4,751 tests** -- which is ONE assertion, counted twice.
`ehrt.docs-tooling.io-vocabulary-lint-test` goes 130 -> 131 in EACH of
the two projects that run `docs-tooling`'s suite (`conformance` and
`ehrt-cli`), and the reason is its own population: its
`no-bare-guarded-io-call-outside-the-kernel-io-allowlist-test` is a
`doseq` over every production `.clj` under `components/*/src` and
`bases/*/src` with one `is` per file, so a new source file grows it by
exactly one. `run.clj` is that file. Located by diffing per-namespace
assertion counts between the two logs, not inferred from the total.

Two things that delta is NOT. It is not the allowlist: that gate
exempts `ehrt.kernel.io` and `ehrt.sim.run` BY NAMESPACE, and
`ehrt.sim-engine.run` is neither -- it is asserted like every other file
and passes because it calls none of the five forbidden `java.io.File`
methods. And it is not a new test: the test count is identical.

## 4. Step 4 -- the bracket, and a new class of obstacle

`bin/ground-truth-bracket cd7302e bdbd319` -- spanning the sweep AND
the move in one bracket, the shape the ninth extraction used --
**ABORTS**, exit 1, on its own soundness check. The reason is worth the
roadmap line it got: this session's citation sweep repointed a comment
inside `components/oracle/src/ehrt/oracle/digest.clj`, which is the
BRACKET'S OWN SOURCE. The check diffs that file whole minus its leading
`ns` docstring and demands `--declared-digest-change` for any other
difference, a comment included (ADR-0156, R4-Q6 (iii) (c), which
deliberately widened it to catch require changes).

**A NEW CLASS: `digest.clj` is a live source, so a citation sweep can
be forced to edit the very file the bracket refuses to see move.** The
disposition taken, rather than a declaration:

* Bracket the MOVE alone. `bin/ground-truth-bracket 3739084 bdbd319`:
  **IDENTICAL: every digested root's `:ground-truth` matches (38
  roots)**, 3 skipped for carrying no `:ground-truth` key
  (appendicitis, ear-infections, sore-throat), `declared-digest-change:
  no`. Sound, undeclared, and it is the commit the gate is about.
* Prove the sweep output-inert from its own diff rather than from a
  bracket: all 66 changed lines of `3739084` are `;;` comments,
  markdown table rows or markdown prose. **Not one Clojure code line.**
  Checked mechanically, by filtering the diff.

## 5. The tripwire successor, and the RED-FIRST pair

`hand-owned-assets.edn`'s `gt-emitters.svg` row bumped `76b0e56` ->
`3739084`, `:verdict` staying `:fresh`, with the review note the row's
own doctrine asks for:

* Both `simulator-architecture.md` hunks are LINE-NEUTRAL, in section 1
  and section 3.
* Section 4's equation block -- what the row's trigger actually names --
  is BYTE-IDENTICAL, verified by extracting lines `194-373` from both
  sides and comparing sha256
  (`111329cbeedcddabdfd8c9c45b7e532826530ab9f191ca43c435b479df0224df`
  either way), not inferred from a hunk header. The span is the same on
  both sides precisely because the edits are line-neutral.
* The move behind it changes no output: the oracle and bracket results
  above, both recorded in the note.

The pair is `3739084` (red) and this commit (green), pushed together
and never alone (`rulings.md#R-red-pushed-with-green`).

## 6. Closing arithmetic, and what the facade is

### The partition closes

Counted with Clojure's own reader.

| namespace | lines | forms (+ ns) |
|---|---:|---:|
| `engine` | 741 | 43 |
| `run` | 1,436 | 8 |
| `decide` | 1,706 | 58 |
| `streams` | 331 | 16 |
| `state` | 441 | 14 |
| `encounters` | 243 | 10 |
| `evolve` | 471 | 32 |
| `fold` | 155 | 3 |
| `log-index` | 304 | 10 |
| `config` | 187 | 5 |
| `assignment` | 144 | 3 |

**202 forms across eleven namespaces**: 157 real forms, `engine.clj`'s
43 delegating defs, and the two-form constraint-1 shim. `engine.clj`
went 47 forms to 43 by losing six and gaining two, and 1,968 lines to
741.

### The facade's final form

**`ns` + 43 delegating defs + nine explanatory comment blocks. Nothing
else.** No `defn`, no `defn-`, no private var, no executable
expression. Eight of the nine blocks are `moved to` narratives (seven
carry the `;; --- moved to ...` header; the `encounters` one, written at
the third extraction, is a plain block, and `log-index` left none at
all); the ninth is this session's.

### Retirement candidates for the ruled repoint pass -- FOURTEEN

Computed, not guessed: for each of the 43 defs, every file in
`components`/`bases`/`projects` that names `ehrt.sim-engine.engine` (78
of them) was searched for `<alias>/<name>` and for the fully-qualified
form. Fourteen have no named caller anywhere:

`ConditionRecord`, `MedicationOrderRecord`, `CarePlanRecord`,
`placeholder-demographics`, `PatientLocation`, `EncounterRecord`,
`AppointmentRecord`, `next-encounter-ordinal`,
`next-appointment-ordinal`, `delivery-stay-minutes`,
`injury-stay-minutes`, `unidentified-stay-minutes`, `Persons`,
`Scheduling`.

Eleven were already caller-less before this session (C1(a) owes a def
for a moved PUBLIC var, and reading that as "public vars someone calls"
would be an exception the ruling does not grant). **The three
`*-stay-minutes` tables were made caller-less by THIS move**, exactly
as the ninth session priced it: their only caller was `prelude`, which
now names `decide/`. The FACADE RULE kept every one -- retiring a def is
the repoint pass's business, not an extraction's -- and the prompt asked
for the count and the names instead, which is this list.

Two more the pass should weigh, of a different kind: `valid-persons?`
is live only through `interface.clj:82` and `valid-scheduling?` only
through `scheduling_test.clj`. Neither is a candidate; both are
one-caller-deep.

### The requires this move made dead, NOT pruned

`engine.clj`'s `ns` still requires `sim-model`, `churn`, `encounters`,
`order-profiles`, `person-fold` and `kernel`, and still imports
`java.util.Random` -- none of which any delegating def uses now. It
also still carries `patient-simulator`, `malli.core` and `malli.util`,
which the NINTH extraction left dead. All nine were left in place on the
ninth extraction's own precedent ("every require is one `engine.clj`
already had"; no `deps.edn`, no `workspace.edn` change) and on the same
reading of the FACADE RULE that keeps the caller-less defs: pruning is a
retirement, and retirement is the repoint pass's. **Named here so the
pass can take them together with the fourteen defs.** Nothing breaks
meanwhile: an unused require is inert, and component-level dependencies
are unchanged because `run.clj` takes every one of them.

### Census corrections, one sentence each

* Section 5's **constraint 1** states a remedy -- "keep the delegating
  var and leave the call sites unqualified" -- that assumes `run` stays
  behind. This move is the case it excludes, and the remedy it needs is
  the shim. The constraint's SUBSTANCE is right and was honoured.
* Section 5's constraint 1 is right about `person_simulator/
  consumption_test.clj:44`/`:142` redefining
  `ehrt.sim-engine.interface/stream` rather than the engine's var --
  confirmed by reading that file's `ns`.
* Section 5's constraint 1's four `stream` call-site line numbers
  (`:3670 :3675 :4059 :4543`) are stale by nine extractions; they are
  `:754 :759 :1143 :1627` at `cd7302e`.
* Section 3a's expected `patient-simulator` edge for this cluster does
  not exist; `engine.clj` has carried the require dead since the ninth
  extraction.
* Section 4a is confirmed exactly a second time: `run`'s `(decide ...)`
  is the sole event producer, and this move relocated the call site
  itself for the first time -- the ninth moved the producer and left the
  site.
* Section 4b's ten-step in-loop fold is confirmed step for step, and
  moved verbatim: decoration, log index, reinstate index, citation
  index, registration index, patient state, bed index, the persistent
  log mirror, the transient accumulator, state history. Nothing added,
  removed or reordered.

### What is left in this program

`engine.clj`'s extraction is COMPLETE. What remains under P5 is
`emit_hl7.clj` (census section 2) and then the application-path
unification the row's second half names -- for which this session's
result is the precondition: apply site 1 now lives in a namespace of its
own, beside apply sites 2 and 3 in `ehrt.sim-engine.fold` and
`ehrt.sim-engine.log-index`.

### Budget

`.agents/plans/roadmap.md` is an `:onboarding` member at 1512 of 1530.
The P5 rewrite -- a tenth landing, the PHASE NOTE, the facade's form
count, the retirement list, the `digest.clj` hazard and the new census
correction -- replaces 83 lines with 92, so `:onboarding` goes 1512 ->
1521 of 1530, nine lines of headroom. Compacted twice to fit: the
per-session narrative belongs in these records and is here in sections
1-5, and what the row keeps is the standing shape of each landing plus
the lessons that generalise.

### CI at the pushed tip -- the close marker

`gh run watch 33334936271` reports **completed / success** in 13m27s at
`ccbea557844869b6eae5b012963e21b1a1df6a8b`, the pushed tip
(https://github.com/pragsmike/ehr-testing-tools/actions/runs/33334936271).
That is the close marker under `rulings.md#R-session-verifies-ci-via-gh`,
which the de-scaffold ruling of 2026-08-25 retired as a TAG condition and
kept as this. No tag was paid.

It also settles the RED-FIRST PAIR from the outside. `3739084` (the
sweep) reddened `hand-owned-asset-freshness-test`; `bdbd319` (the move)
inherited that red, because the tripwire reads `git log -1` on the SOURCE
and the move does not touch it; `2fb0eee` cleared it. All four commits
were pushed together, and CI -- which sees committed state and therefore
CAN see what the local suite could not -- is green over the range
containing all of them. `bin/post-push-verify cd7302e ccbea55` also
reports the remote tip matching HEAD and every commit message in range
pure ASCII.

**`engine.clj`'s EXTRACTION IS COMPLETE.** Ten clusters, ten
output-identical moves, and a facade that is now nothing but its `ns`,
43 delegating defs and nine banners. What is left under P5 is
`emit_hl7.clj` and then the application-path unification -- for which
this session's result is the precondition, since apply site 1 now lives
in a namespace of its own beside sites 2 and 3.

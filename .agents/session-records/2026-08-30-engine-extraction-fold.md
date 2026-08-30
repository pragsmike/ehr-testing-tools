# Engine namespace extraction, 5 of N: the `fold` cluster

Session record, 2026-08-30. HEAD at start `5b6ab85`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, private movers widen with none, no test file
changes) and S1(a) (an equivalence proof replaces red-before-green).

The census's DAG (`.agents/plans/engine-extraction-census.md` section
3a) puts `fold` after `evolve`, extracted one session ago, and before
`log-index`. This move is the first of the five to move an APPLY SITE:
`replay` is census section 4c's apply site 2, and its divergence from
`run`'s in-loop fold (section 4b, six omitted concerns) is RULED to be
paid at unification, not here. `replay` moves verbatim; nothing it
folds is added, removed or reordered.

## 1. Step 1 -- the tip, the spans, the privacy markers, the re-export

`5b6ab85`. `bin/preflight` exit **0**, no findings; its one DISCLOSED
line is "HEAD is not currently tagged stable-*", which is expected
under the de-scaffold ruling of 2026-08-25 (tags retired). Last five CI
runs on `main` all green.

Every span below was re-derived from THIS tree by paren balance, not
transcribed from the census, whose own numbers are at `517a96d` --
**four** extractions ago.

`engine.clj` is **3,844 lines / 110 top-level forms plus the `ns`**
here, against the census's 4,884 / 157. The partition still closes:
110 + `streams`' 16 + `state`'s 14 + `encounters`' 10 + `evolve`'s 32 =
**182**, which is 181 plus the evolve move's one delegating def --
exactly the arithmetic the evolve record left standing.

**The cluster is ONE contiguous block, `1990-2092`**, 103 lines and 3
forms, with **ZERO interior comment blocks** -- the first extraction of
the five for which that is true.

| form | census (`517a96d`) | here (`5b6ab85`) | privacy |
|---|---|---|---|
| `def ^:private bed-correction-event-types` | `3030-3063` | `1990-2022` | **`^:private`** |
| `defn- update-beds` | `3064-3097` | `2024-2056` | `defn-` |
| `defn replay` | `3098-3139` | `2058-2092` | **public** |

**First census correction, and it is a PRIVACY MARKER the rendering
drops.** Section 1 lists the first form as `def bed-correction-event-
types`, unmarked. The tree says `(def ^:private bed-correction-event-
types` at `1990`. The census's section-1 renderings omit `^:private`
systematically -- `reinstatable-event-types` and `cited-opening-event-
types` are rendered the same way and carry the same marker in the tree
-- so this is the rendering's convention, not a claim about this var,
and it is why the prompt says to derive each mover's marker from the
tree. The consequence is real: **two of the three movers are private**,
so constraint 5 governs them and C1(a)'s delegating-def obligation
arises exactly once.

**One delegating def owed, and `interface.clj` is why it is
load-bearing.** `components/sim-engine/src/ehrt/sim_engine/
interface.clj:89` is `(def replay engine/replay)` -- so `replay` IS on
the re-export list, as the prompt expected and as census constraint 4
records. Verified, not trusted: the line was read. That export resolves
through `ehrt.sim-engine.engine/replay`, so the delegating def is what
keeps it resolving, and `interface.clj` is not opened (this session's
fence, and constraint 4 says it keeps naming `engine/...`).

The two private movers widen to public in `ehrt.sim-engine.fold` and
get NO def in `engine.clj` -- constraint 5, and the treatment the
`evolve` session gave its four helpers.

**The outgoing edges, confirmed mechanically.** A whole-symbol scan of
the cluster body (line comments and string literals stripped) against
all 78 distinct top-level names `engine.clj` defines returns six, of
which three are the cluster's own (`bed-correction-event-types`,
`update-beds`, `replay`). Of the remaining three, `bed-status-change`
is a FALSE POSITIVE -- `defn- bed-status-change` is a real top-level
name at `1073`, but every occurrence inside the cluster is the KEYWORD
`:bed-status-change` (five of them, at cluster-relative `5`, `40`,
`46`, `53`, `87`), and none is the symbol. Checked, not assumed.

So the edges that leave are exactly the two the prompt predicted:

| name | occurrences | real home | how it appears today |
|---|---:|---|---|
| `evolve` | 1 (`replay`) | `ehrt.sim-engine.evolve` | delegating def in `engine.clj` |
| `initial-patient` | 1 (`replay`) | `ehrt.sim-engine.state` | delegating def in `engine.clj` |

**ZERO symbols still resolve in `engine.clj` itself**, so the prompt's
stop-and-report condition does not fire. Both are delegating defs whose
definitions live in already-extracted namespaces, and each is taken
DIRECTLY into its real home rather than routed back through the facade
-- the treatment every prior extraction gave its own edges. `fold`'s
`:require` set is therefore exactly **`evolve`, `state`**: no
`sim-model`, no malli, no `clojure.*`, no `result`, no `streams`, no
`encounters`.

**The two inbound edges from what stays**, both census section 3a rows,
and they are treated differently because the markers differ:

| caller | callee | site | treatment |
|---|---|---|---|
| `reinstated-state` (`log-index`) | `replay` | `engine.clj:2231` | UNQUALIFIED, resolving through the delegating def |
| `run` | `update-beds` | `engine.clj:3778` | **must become `fold/update-beds`** -- private mover, no def |

## 2. Step 2 -- the pre-move citation sweep (constraint 6)

`.agents/plans/engine-extraction-census.md` section 5 item 6, run
BEFORE any form moved, at both recipe levels.

Method, level 1: every docstring and comment line of the 3 moving forms
was whitespace-normalised and cut into every distinct six-word window
of 28+ characters -- **455 phrases** -- and each searched, whitespace-
normalised, against every `.md`/`.clj`/`.cljc`/`.cljs`/`.edn`/`.txt`/
`.yml`/`.yaml`/`.json`/`.sh`/`.svg` file in the tree (**1,428 files**,
`engine.clj` itself excluded, editor and linter caches excluded). A
whole-symbol name scan was run beside it, for the three moving NAMES
over the same population.

### 2a. Path-pinned snippet citations -- the class that cost the streams session a red

**ZERO, checked rather than assumed.** The gated shape is
`` `path` "snippet" `` (`patient-simulator-charter-test`'s and
`person-simulator-charter-test`'s own `citation-pattern`, resolved by
`slurp` against the named file). Both registers were read row by row:

| register | rows citing a `sim-engine` path | disposition |
|---|---|---|
| `components/person-simulator/docs/limitations.md` | rows 1 and 10, both `.../sim_engine/streams.clj`; others cite `check.clj`, `persona.clj`, `hazards.clj`, `process.clj`, `compile_trajectory.clj`, ADR files | **safe** -- no snippet is in the moving text |
| `components/patient-simulator/docs/limitations.md` | none -- its rows cite `gmf.clj`, `gmf_interpreter.clj`, `compile_trajectory.clj`, `persona.clj`, `emit_hl7.clj`, `check.clj`, `gmf-interpreter.md` | **safe** |

A tree-wide grep for a backticked path containing `engine.clj` followed
by whitespace and a quote returns nothing. **No gated citation anywhere
names `engine.clj` as a path** -- the fourth extraction running.

### 2b. Whole-symbol references to a moving name, outside `engine.clj`

**`replay` is PUBLIC and keeps its delegating def**, so every external
reference continues to resolve against `ehrt.sim-engine.engine/replay`.
That is a large population and it is dispositioned as one class: ~60
call sites and prose mentions across `check.clj` (11 call sites plus
its own header docstring), `emit_fhir.clj`, `identifiers.clj`,
`interface.clj:89`, `engine_test.clj`, `check_test.clj`,
`emit_fhir_test.clj`, `event_conformance_test.clj`, `run_test.clj`,
`v2_replay_test.clj`, and every ADR/record/roadmap prose mention. This
is the state session's `check.clj:1005` disposition exactly: a
path-or-namespace claim is safe when a delegating def forwards it.
**No `with-redefs` on `replay` exists anywhere in the tree** -- checked,
because census constraint 1 records that exact hazard for `stream`.

The two PRIVATE movers get no def, so their references are swept
individually:

| name | hits outside `engine.clj` | disposition |
|---|---|---|
| `update-beds` | census section 1/3a/4b; `.agents/session-records/2026-08-29-traffic-scale-close.md:472`; `notes/adr/0174-...md:679`; `check.clj:523` | three frozen documents **safe**; `check.clj:523` **REPOINT** (2c) |
| `bed-correction-event-types` | census section 1; `.agents/session-records/2026-08-27-arc-3b-bed-cycle.md:202`; `.agents/session-records/2026-08-30-engine-extraction-evolve.md:500`; `notes/adr/0174-...md:641`; `check.clj:583`, `:598`, `:629`, `:679` | records and ADR-0174 **safe** (bare names, no namespace or path attribution); `check.clj:598/629/679` are that namespace's OWN private def and its use -- **safe**; `check.clj:583` and `:600` **REPOINT** (2c) |

Session records are frozen by construction; the census is the document
this session corrects, in its own section 6; ADR-0174's two mentions
are bare names inside a frozen ruling.

### 2c. Positional and attribution citations -- recipe level 2

Run in both directions.

**Out of the cluster: FIVE repoints owed, and they land with the move.**

| hit | text | disposition |
|---|---|---|
| `engine.clj:1976` | the EVOLVE EXTRACTION'S OWN BANNER: "`replay` and `run` below still call `evolve` unqualified exactly as they did" | **REPOINT** -- `replay` is not below any more and will call `evolve/evolve`; `run` still does both |
| `docs/dev/simulator-architecture.md:56` | "`replay` -> its `defn replay`" (the `sim-engine` brick row) | **REPOINT** -- and it fires the hand-owned-asset tripwire; see 2e |
| `docs/dev/simulator-architecture.md:100` | "`replay` (`engine.clj`'s `defn replay`) is that fold, re-run" | **REPOINT** -- same file, same tripwire |
| `components/sim-check/src/ehrt/sim_check/check.clj:522-523` | "THE FOLD BELOW IS DELIBERATELY NOT `ehrt.sim-engine.engine`'s OWN `update-beds`" | **REPOINT** -- a private mover with no def; nothing forwards this attribution |
| `components/sim-check/src/ehrt/sim_check/check.clj:583`, `:600` | "The reason lives in `ehrt.sim-engine.engine`'s own `bed-correction-event-types`"; "`ehrt.sim-engine.engine`'s own set, restated here" | **REPOINT** -- same class, same reason |

The three `check.clj` hits are the sharpest finding of this sweep and
are NOT the class a delegating def covers. `replay`'s many prose
mentions are safe precisely BECAUSE a def forwards them; these three
name the namespace a **private** var lives in, and constraint 5 forbids
the def that would keep them true. Left alone they would be ADR-0170's
own pattern -- a claim true when written that nothing keeps true. The
edits are comment and docstring text only, in a `src` file, changing no
behaviour; C1(a)'s fence is on TEST files and `check.clj` is not one.
Neither passage is a snippet any charter row pins (2a).

`docs/dev/simulator-architecture.md:107` quotes `check.clj`'s own
header docstring, `ehrt.sim-engine.engine/replay` -- **safe**, the def
forwards it. `:59` ("built on `sim-engine/replay`") and `:218`
("`replay` is `sim-engine`'s own function of that name") are
BRICK-level, still true. **Section 4's EQUATION BLOCK at `:200-206` --
`walk`, `engine`, `emitH`/`emitF`, `replay`, `check` -- is untouched by
every one of these repoints**, which is what this tripwire's trigger
actually names.

Everything else in `engine.clj` that names a moving name and stays
behind was read line by line and is **safe**:

| line | text | why safe |
|---|---|---|
| `484` | "`replay` (below) bootstraps ... via `(initial-patient pid (:active-mrn event))`" | the delegating def stands exactly where the cluster stood, so "below" holds; the mechanism claim is about the FUNCTION, which is unchanged |
| `489` | "`replay`'s own bootstrap" | bare name, no position |
| `1082` | "`replay` and the run loop's own two folds, below" | the def is below `1082`; position holds |
| `1507`, `1783`, `1860`, `2758`, `3557`, `3564` | "replayed"/"replaying"/"only replay it" | the English verb, not the var |
| `2094-2098` | "M2b cancel-transfer/cancel-discharge: defined here, AFTER `replay`, because their decide methods query it directly" | the def stands above it, so AFTER holds, and `reinstated-state` still queries `replay` directly through it |
| `2192`, `2194`, `2203`, `2213`, `2220`, `2226`, `2231` | `reinstated-state`'s own prose and its call site | all resolve through the delegating def |

**Into the cluster: ONE positional claim travels with the text, ALREADY
FALSE where it stands.** Named here and moved VERBATIM rather than
corrected, which is the disposition the encounters and evolve sessions
both gave this class:

| line | text | why it is safe to move unchanged |
|---|---|---|
| `2007` | `bed-correction-event-types`' docstring: "the guard in `decide :bed-ready` **below** already handles the bed it leaves" | `defmethod decide :bed-ready` is at `1467`, ABOVE it -- already false, and it stays false in a new way (`decide` is not in `fold.clj` at all) rather than becoming false |

`update-beds`' own "`bed-correction-event-types` **above**" (`2033`) is
TRUE here and TRUE there: the two forms move in order, so the claim
survives the move intact.

### 2d. Phrase hits that are shared prose, not citations

The 455-phrase sweep returned hits in **7 files**. Every hit was
inspected:

| file | phrases | disposition |
|---|---:|---|
| `components/sim-check/src/ehrt/sim_check/check.clj` | 13 | the bed-cycle prose, deliberately restated by the independent judge; three of them are the attributions repointed in 2c, the other ten are shared doctrine -- **safe** |
| `.agents/session-records/2026-08-27-arc-3b-bed-cycle.md` | 13 | frozen record -- **safe** |
| `components/sim-engine/test/ehrt/sim_engine/bed_cycle_test.clj` | 12 | a test file, which C1(a) forbids touching; it names no namespace but the `engine` alias in its `ns` -- **safe** |
| `components/sim/docs/operational-models.md` | 11 | ADR-0174's bed-cycle prose, the third of the "three places" that ADR names; no `engine.clj`, no `update-beds`, no `bed-correction-event-types` occurs in it at all -- **safe** |
| `notes/adr/0174-...md` | 11 | the frozen ruling this prose restates -- **safe** |
| `components/sim-engine/src/ehrt/sim_engine/event_schema.clj` | 4 | `replay`'s `sim/ADR-0010` doctrine sentence ("a patient's state folds exactly the events they participate in"), which that file restates because it is the event contract -- **safe** |
| `docs/dev/simulator-architecture.md` | 1 | `{:event :patient-id :before :after :world-before :world-after}`, `replay`'s own record shape, at `:102` -- a SHAPE claim, unchanged by the move; the file is repointed for its two ATTRIBUTION claims instead (2c) |

None pairs a phrase with a path; none is read by a gate.
**Disposition: safe, as a class** -- the finding the streams, state,
encounters and evolve sessions each recorded, met a fifth time.

### 2e. Gates and tripwires checked, and ONE fires -- for the second extraction running

* **The hand-owned-asset tripwire.** `hand-owned-assets.edn`'s four
  sources are `docs/dev/simulator-architecture.md` (cited by TWO rows),
  `components/corpus/docs/palgebra-design.md`,
  `components/corpus/docs/pipeline.edn` and
  `demos/scenarios/ed-tuesday/README.md`. `simulator-architecture.md`
  is repointed at `:56` and `:100` (2c), so `gt-emitters.svg`'s row
  (`:verdict :fresh`, `:reviewed-at "5637cbe5"`) owes a `:reviewed-at`
  bump. `two-clocks.svg` cites the same source but is `:verdict
  :stale`, and claim (d) of
  `ehrt.docs-tooling.hand-owned-asset-freshness-test` skips
  non-`:fresh` rows, so it owes nothing. The other three sources
  contain no moving name.
  **The bump cannot ride the commit that edits the source** -- the test
  reads `git log -1 -- <source>`, so no commit can carry the sha that
  names itself. The move commit is therefore a RED-FIRST commit under
  `rulings.md#R-red-pushed-with-green`, pushed together with its
  one-line successor and never alone. PREDICTED, not discovered: the
  registry's sources were read before the doc was touched.
* **Namespace collision.** `components/sim-engine/src/ehrt/sim_engine/`
  holds `churn`, `encounters`, `engine`, `event_schema`, `evolve`,
  `interface`, `order_profiles`, `person_fold`, `state` and `streams`
  -- no `fold` of any kind. `person_fold.clj` is
  `ehrt.sim-engine.person-fold`, a different name. No
  `ehrt.sim-engine.fold` and no `sim_engine/fold.clj` is referenced
  anywhere in the tree, and there is no `fold_test.clj` either.
* **`ehrt.docs-tooling.sim-purity-lint-test`** globs each sim-family
  brick's whole `src` tree, so `fold.clj` is scanned automatically and
  needs no registration. It introduces no
  `atom`/`ref`/`agent`/`volatile!`/`set-validator!` -- the moving text
  is a set literal, a `reduce`, and a `loop`/`transient` accumulator.
* **`stale-path-test`** asserts only that `engine.clj`'s path trips no
  retired-prefix rule. The path still exists; a new sibling file is
  invisible to it, as it was to the four prior extractions.
* **No registration surface owes a row.** The only files that had to
  learn about `evolve.clj` when it landed were `engine.clj` (the
  `:require`), the file itself, `roadmap.md` (the row) and
  `hand-owned-assets.edn` (the note). `fold.clj` is the same shape.
* **`engine.clj:NNN` line citations.** Unchanged from the evolve sweep:
  three in `engine_test.clj` (`:2440`, `:2446`, `:2543`), all stamped
  "at ADR-0171's design HEAD `c1b996e`", plus `streams.clj:57` quoting
  ADR-0171's `engine.clj:225`. All explicitly historical, none resolved
  by a gate. Untouched.
* **`interface.clj` DOES re-export `replay`** (`:89`), which is exactly
  why the delegating def is owed. Constraint 4's list is unaffected and
  the file is not opened, per this session's fences.
* **Reading sets.** `docs/dev/simulator-architecture.md` is a `:sim`
  member (1340/1405, headroom 65), so `.agents/state-derived.md` is
  regenerated with the move. `check.clj` and `engine.clj` are in no
  reading set. The `:docs` set's headroom of **-2** is PRE-EXISTING,
  carried forward from the streams session through four extractions,
  untouched by anything here.

**Gate for step 2: this hit list is committed before the move.**

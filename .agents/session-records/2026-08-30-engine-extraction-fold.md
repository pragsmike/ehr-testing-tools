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

## 3. Step 3 -- the extraction (`ff26bb6`)

`ehrt.sim-engine.fold`, 3 forms, **153 lines** (103 moved, 50 of `ns`).
No collision, checked in section 2e before the file was written.

**The verbatim claim is proven, not asserted.** `sed -n '1990,2092p'`
of `engine.clj` at `c3c414e`, diffed against `fold.clj` lines `51-153`,
is 103 lines against 103 differing on **exactly four**:

| difference | count | why it is required |
|---|---:|---|
| `(def ^:private X` -> `(def X` | 1 | constraint 5 -- a private mover becomes public in its new namespace |
| `(defn- X` -> `(defn X` | 1 | constraint 5 |
| `(initial-patient ...)` -> `(state/...)` | 1 | edge into `state`, taken DIRECTLY rather than through `engine.clj`'s delegating def |
| `(update ps pid evolve event)` -> `(... evolve/evolve ...)` | 1 | edge into `evolve`, the same |

Nothing else differs. **Nothing `replay` folds was added, removed or
reordered** -- the two repointed lines resolve the SAME two functions
through a different path, and the loop, its bootstrap condition, its
participant filter, its accumulator and its record shape are the file's
own untouched text. That is what this session's fence on apply site 2
asks for.

**ONE delegating def, and it is load-bearing rather than ceremonial.**
Verified live in a `-M:dev` load rather than assumed:

* `(identical? engine/replay fold/replay)` is **true**, and so is
  `(identical? sei/replay fold/replay)` -- `ehrt.sim-engine.interface`'s
  own re-export at `interface.clj:89` resolves through the delegating
  def to the moved function, which is exactly what constraint 4 needs
  and why `interface.clj` was not opened;
* `update-beds` and `bed-correction-event-types` resolve to **NOTHING**
  in `ehrt.sim-engine.engine` (`ns-resolve` returns nil for both) and
  are **public** in `ehrt.sim-engine.fold` -- constraint 5 in both
  directions;
* folding a two-event log (`:registered` then `:admission`) through
  `engine/replay`, `fold/replay` and `sei/replay` returns three EQUAL
  record seqs, last `:after` `:status :admitted`;
* `fold/update-beds` and `fold/bed-correction-event-types` are callable
  and return what they always did;
* `fold`'s alias set, read off the LOADED namespace rather than off its
  `ns` form, is exactly **`(evolve state)`** -- the two the prompt
  expected, and no third.

**The whole `engine.clj` diff is FOUR hunks and nothing else**: the
`:require` insertion, the evolve banner's repoint, the block replaced
by a banner plus the delegating def, and `run`'s ONE call site
qualified to `fold/update-beds` (with its continuation line re-indented
five columns to stay aligned under the longer callee -- whitespace, no
behaviour). `reinstated-state`'s call at `:2231` needed no qualification
at all and got none: it calls `replay`, and a delegating def is what it
calls.

`engine.clj` is 3,844 -> **3,794 lines**, 110 forms plus `ns` -> **108**.
The partition closes at **183** -- 182 before, plus this move's one
delegating def -- and the per-session arithmetic holds end to end:
157 - 16 + 11 = 152, - 14 + 13 = 151, - 10 + 0 = 141, - 32 + 1 = 110,
- 3 + 1 = 108, and 108 + 16 + 14 + 10 + 32 + 3 = 183.

`ehrt.sim-engine.interface` was not opened, per this session's fences.

### The three repoints outside `engine.clj`, and why they are a different class

`check.clj:522`, `:583` and `:600` attributed the two PRIVATE movers to
`ehrt.sim-engine.engine`. This is NOT the class the prior extractions
dispositioned as safe. `replay`'s own ~60 prose and call-site
references ARE safe, and they are safe for a reason that does not
generalise: a delegating def forwards them. Constraint 5 forbids the
def that would forward these three, so left alone they would be exactly
`notes/adr/0170`'s pattern -- a claim true when written that nothing
keeps true. Repointed to `ehrt.sim-engine.fold`; comment and docstring
text only, no behaviour, and neither passage is a snippet any charter
row pins (section 2a).

`check.clj:505` and `:561` were READ and left alone rather than
repointed by pattern-match: `:505` names `ehrt.sim-engine.engine`'s own
`:disposition` FIELD, and `:561` names a comment which a grep locates
at `engine.clj:1444`, inside the `decide :bed-ready` region, which
stays. Both attributions remain true.

### The derived count, predicted rather than discovered

`docs/dev/simulator-architecture.md` is one line longer after the
repoint, and that file is a `:sim` reading-set member: the set moved
**1340 -> 1341** lines, headroom **65 -> 64** against an unchanged 1405
budget, so `.agents/state-derived.md` is regenerated in the same commit.
The evolve session found this by a RED gate on its first full run and
recorded it as `feedback_repo_gate_ordering` met head-on. This session
predicted it from that record and regenerated BEFORE the first suite
run, which is why the run below was green the first time.

### Gates

* **Suite, run TWICE and green both times.** The close figure is the
  SECOND run, over the tree this session pushes -- extraction, tripwire
  bump, record, prompt archive, roadmap row and regenerated docs all
  present, so every derived-count gate saw its final input:
  `make test` exit **0**, zero failures and zero errors, **408
  namespaces, 4,751 tests, 24,119 assertions**, 15 min 53 s. The FIRST
  run, over the extraction alone before any of the closing documents
  existed, reported the identical 408 / 4,751 / 24,119 at exit 0 in
  16 min 17 s.

  **The first run being green is itself the disclosure.** The evolve
  session's first run was RED on `ehrt.docs-tooling.state-derived-test`,
  because its `simulator-architecture.md` repoint added a line to a
  `:sim` reading-set member and nothing in the diff showed it. This
  session made the same edit with the same consequence and regenerated
  `.agents/state-derived.md` BEFORE running, from that record's own
  warning -- so the gate that caught the fourth extraction never fired
  on the fifth. Named because a green gate proves nothing about whether
  it was going to fire.

  Namespaces and tests are IDENTICAL to the evolve extraction's
  own recorded run (408 / 4,751); assertions are **+2** against its
  24,117, and the +2 is explained to the assertion rather than waved at.
  `ehrt.docs-tooling.io-vocabulary-lint-test` is a `doseq` over every
  production source file with one `is` per file plus a population guard;
  it reported **126** assertions in EACH of the two projects it runs in,
  against **125** in the evolve session, and the tree gained one file.
  One new file is +1 twice. The same benign class the streams, state,
  encounters and evolve sessions each recorded, and the class this
  session's own prompt named in advance. Both execution times above are
  on an UNSAMPLED host -- reported as each run's own line, not offered
  as timing claims, and in particular the 24-second gap between them is
  NOT a measurement of anything.
* **`bin/regression-oracle c3c414e ff26bb6`** -- the script's own
  output: `IDENTICAL: every root's digest matches between c3c414e and
  ff26bb6`, over **41 roots**, `declared-digest-change: no (soundness:
  yes outside the leading docstring)`. No declaration was owed and none
  was made.
* **`clojure -M:poly check`** OK. Because `poly check` does not compile
  -- a standing finding of the arc-4 sweeps, not new here -- the real
  check is the `-M:dev` load above, which resolved and folded through
  all three vars.

## 4. Step 4 -- the ground-truth bracket

**`bin/ground-truth-bracket c3c414e ff26bb6`** -- `IDENTICAL: every
digested root's :ground-truth matches between c3c414e and ff26bb6 (38
roots)`, coverage `38 roots carry :ground-truth and are digested; 3
skipped (no such key): appendicitis.edn, ear-infections.edn,
sore-throat.edn`; `declared-digest-change: no`.

Both brackets IDENTICAL with no declaration, for the **fifth**
consecutive extraction. It carries more weight here than in the four
before it: this is the first move to relocate an APPLY SITE, and
`replay` is the fold `ehrt.sim-check.check`'s entire invariant catalog
and `sim-emit-fhir`'s whole snapshot path are built on. A change to
what it folds would surface as a differing digest on the ground-truth
half specifically, which is the half `bin/ground-truth-bracket` isolates
by construction.

**Named against over-reading it, per `project_oracle_blind_to_cancel_
replay_simcheck`:** all 41 oracle roots pass a module-only pathway and
reach none of the cancel family, and `sim-check` is off the oracle
classpath entirely -- so `IDENTICAL` here is NOT evidence that
`reinstated-state`'s `replay` fallback still works. That is covered
instead by the suite, which runs `check.clj`'s catalog and
`engine_test`/`run_test`'s own `replay` assertions, and by the live
`identical?` checks in section 3.

## 5. Step 5 -- the tripwire, the push, and what the next session takes

### The tripwire, and why this extraction is again a RED-FIRST commit

`docs/dev/simulator-architecture.md` is `gt-emitters.svg`'s `:source`
in `components/docs-tooling/resources/docs-tooling/hand-owned-
assets.edn`, and `ehrt.docs-tooling.hand-owned-asset-freshness-test`
claim (d) compares `:reviewed-at` against `git log -1` on the SOURCE.
No commit can carry the sha that names itself, so `ff26bb6` is a
RED-FIRST commit under `rulings.md#R-red-pushed-with-green` and
`4da2f0c` is its immediate successor, pushed with it and never alone.

The review was done, not skipped: the source diff is TWO hunks, in
sections 1 and 2; section 4's own EQUATION BLOCK -- `walk`, `engine`,
`emitH`/`emitF`, `replay`, `check` -- was diffed DIRECTLY (not inferred
from the hunk headers) and is byte-identical. `replay` is still a name
in that block and still means the same function; what moved is which
file defines it. Both brackets IDENTICAL is the positive evidence that
no arrow on the drawing moved. Claim (d) was then re-run row by row
over the live registry: `gt-emitters` `true`, `inject-expect-loop`
`true`, `straddle-timeline` `true`, `verdict-ranking` `true`,
`two-clocks` skipped as `:stale`.

**This is the SECOND consecutive extraction to fire this tripwire, and
the first to fire it by RECIPE rather than by catch.** The evolve
session fired it because `defmulti evolve` was named in that file by
DEFINING FORM, and recorded that a `defn`-name citation is more durable
than a line citation and still not durable against an extraction. This
session read that record, swept the registry's four sources during its
own pre-move sweep at `c3c414e`, and found `defn replay` named the same
way in the same file -- twice. `two-clocks.svg` cites the same source
but is `:verdict :stale`; claim (d) skips non-`:fresh` rows, so it owed
nothing.

### The DAG, and what the next session takes

**No back-edge into the remaining `engine.clj` from `fold`. Confirmed
two ways.** Structurally: `fold.clj`'s alias set is exactly `evolve`
and `state`, read off the LOADED namespace. Mechanically: the pre-move
whole-symbol scan against all 78 of `engine.clj`'s top-level names
returned six, three of them the cluster's own, one a keyword false
positive, and two delegating defs whose definitions live in
already-extracted namespaces.

The census's remaining order is unchanged: `config` and `assignment`
are the last leaves, then **`log-index`** (10 forms -- `events-for-
patient`, the two cancel/citation scans and the reinstatement
machinery -- whose `reinstated-state` call into `replay` is now a call
into an extracted namespace, reachable through `engine.clj`'s
delegating def or directly), then `decide`, with `run` last as the
facade's residue. Application-path unification stays last, against
section 4 of the census, and is also
`roadmap.md#event-stream-mutation`'s injection point.

**A note the unification session should have.** This move did not
narrow the apply-path problem, and it was not supposed to: `replay`
(site 2) now lives in `fold.clj` while `run`'s in-loop fold (site 1)
and `reinstated-state`'s replay fallback (site 3) both still live in
`engine.clj`. The three sites are now split across two namespaces
rather than one. That is the census's dependency order doing what it
says, not a regression -- `run` and `log-index` are the LAST two
clusters, so the sites reconverge when they land.

## 6. Census corrections, one sentence each

1. **`bed-correction-event-types` is `^:private`**, which section 1's
   rendering drops -- the same rendering drops it from
   `reinstatable-event-types` and `cited-opening-event-types` too, so
   it is the rendering's convention rather than a claim about this var,
   and the consequence is that TWO of the three movers are private and
   this extraction owes exactly ONE delegating def rather than three.
2. **The cluster has ZERO interior comment blocks**, the first of the
   five extractions for which that is true -- the comment-block recipe
   that fired in three consecutive sessions found nothing to fire on
   here, which is itself worth recording: the recipe's value is that it
   is run, not that it always hits.
3. **The prior extraction's banner WAS a mover candidate and turned out
   to be a REPOINT, not a move**: `engine.clj:1977`'s "`replay` and
   `run` below still call `evolve` unqualified exactly as they did" is
   the evolve session's own text, and it is a positional claim about
   `replay` -- but the banner belongs to the `evolve` delegating def,
   which stays, so the sentence is corrected in place rather than
   travelling with the cluster. The prompt was right to name the class
   and right not to predict which way it would resolve.
4. **`bed-status-change` is a whole-symbol FALSE POSITIVE** in an edge
   scan: `defn- bed-status-change` is a real top-level name at
   `engine.clj:1073`, but all five occurrences inside the cluster are
   the KEYWORD `:bed-status-change`, so the cluster's outgoing edges
   are two and not three.
5. **The census's sizes are now stale by FOUR extractions, as designed**:
   `engine.clj` is 3,844 lines / 110 forms plus `ns` at this session's
   start against section 1's 4,884 / 157, and 3,794 / 108 at its end.

## 7. Disclosures

* **No premise mismatch in this session's prompt.** All three things it
  asked to be corrected on from the tree resolved: the spans were
  four extractions stale as it warned; the privacy markers differ from
  the census's rendering (correction 1); and `replay` IS on
  `interface.clj`'s re-export list, which the prompt asked to be
  verified rather than trusted -- it is, at `:89`, and that is what
  makes the one delegating def load-bearing. The prompt's expected
  edge set (evolve, state) was exactly right.
* **Three pieces of text outside `engine.clj` were edited**, all
  dispositioned in section 2c BEFORE the move rather than discovered
  during it: `check.clj`'s three namespace attributions and
  `simulator-architecture.md`'s two `defn replay` attributions. The
  `check.clj` edits are the first time an engine extraction has had to
  repoint prose in ANOTHER BRICK, and the reason is structural: this is
  the first cluster whose private movers are named by namespace from
  outside `sim-engine`, because `sim-check` deliberately reimplements
  the bed index and says whose implementation it is not copying.
* **`replay` is an apply site and its six-concern divergence from
  `run`'s in-loop fold is UNCHANGED by this move.** Named again here
  because the move relocates the site: no encounter stamp, no warm-up
  mark, no bed index, none of the three log indexes. Ruled payable at
  unification, not here.
* No fresh clone; the existing clone was at `5b6ab85`, clean, and equal
  to `origin/main`. Every span was re-derived, by paren balance.
* `make test` runs `poly test :all skip:integration`, so the integration
  tier did not execute here. That is the standing W-1 disclosure, not
  new to this session.
* **No timing figure is offered.** The Windows side was not sampled
  before the suite run, and
  `reference_measurement_host_contamination`'s rule is that an
  unsampled host cannot carry a timing claim. The 16 min 17 s above is
  the run's own line, not a measurement.
* `.agents/state-derived.md` still records the `:docs` reading set at
  787 lines against a 785 budget, headroom **-2**. PRE-EXISTING,
  carried forward from the streams session through four extractions,
  untouched by anything here, and named again rather than left to be
  rediscovered. The `:sim` set moved 1340 -> 1341 in this session and
  stays 64 under its own budget.
* `make state-derived` was run for the generated
  `INDEX.md`/`state-derived.md` rows. `make traces` was not run and no
  trace moved: a namespace split that both brackets call IDENTICAL
  cannot move a derived capture.
* **The oracle's blind spots are named rather than papered over** in
  section 4: `IDENTICAL` across 41 roots does not witness the cancel
  family, `reinstated-state`'s `replay` fallback, or `sim-check` at
  all. The suite is what covers those.

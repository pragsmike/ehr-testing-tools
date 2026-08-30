# Engine namespace extraction, 4 of N: the `evolve` cluster

Session record, 2026-08-30. HEAD at start `54551d7`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, no test file changes) and S1(a) (an
equivalence proof replaces red-before-green).

The census's DAG (`.agents/plans/engine-extraction-census.md` section
3a) puts `evolve` after `streams`, `state` and `encounters`, all three
already extracted, and before `fold`, whose `replay` calls it. Its one
former back-edge, `observation-value-fields`, was broken by the state
session.

## 1. Step 1 -- the tip, and the spans re-derived

`54551d7`. Every span below was re-derived from THIS tree with a
form-span scanner, not transcribed from the census, whose own numbers
are at `517a96d` -- three extractions ago.

`engine.clj` is **4,234 lines / 141 top-level forms plus the `ns`**
here, against the census's 4,884 / 157, exactly what the encounters
record predicted. The partition still closes: 141 + `streams`' 16 +
`state`'s 14 + `encounters`' 10 = **181**.

**The cluster is ONE contiguous block, `1954-2378`**, 425 lines and 32
forms. The census's two blocks (`2541-2586` and `2648-3029`) did become
contiguous when `encounters` left from between them, as this session's
prompt anticipated -- but not quite: what stands between
`fold-conditions` and `defmulti evolve` today is a SIX-LINE COMMENT
BLOCK the encounters extraction itself wrote (see correction 2).

| form | census (`517a96d`) | here (`54551d7`) | privacy |
|---|---|---|---|
| `defn- fold-condition-annotation` | `2541-2571` | `1954-1983` | `defn-` |
| `defn- fold-conditions` | `2572-2586` | `1985-1989` | `defn-` |
| comment block | -- | `1991-1996` | -- |
| `defmulti evolve` | `2648-2658` | `1998-2007` | **public** |
| `defmethod evolve :registered` | `2659-2694` | `2009-2032` | -- |
| comment block | -- | `2034-2043` | -- |
| `defmethod evolve :demographic-update` | `2695-2711` | `2045-2060` | -- |
| `defmethod evolve :coverage-change` | `2712-2716` | `2062-2065` | -- |
| `defn- resolve-appointment` | `2717-2749` | `2067-2098` | `defn-` |
| `defn- keep-appointment` | `2750-2756` | `2100-2105` | `defn-` |
| `defmethod evolve :admission` | `2757-2778` | `2107-2127` | -- |
| `defmethod evolve :transfer` | `2779-2782` | `2129-2131` | -- |
| `defmethod evolve :discharge` | `2783-2808` | `2133-2155` | -- |
| comment block | -- | `2157-2157` | -- |
| `defmethod evolve :cancel-admit` | `2809-2815` | `2159-2164` | -- |
| `defmethod evolve :cancel-transfer` | `2816-2819` | `2166-2168` | -- |
| `defmethod evolve :cancel-discharge` | `2820-2842` | `2170-2191` | -- |
| `defmethod evolve :bed-swap` | `2843-2846` | `2193-2195` | -- |
| `defmethod evolve :merge` | `2847-2856` | `2197-2202` | -- |
| comment block | -- | `2204-2205` | -- |
| `defmethod evolve :step-rejected` | `2857-2864` | `2207-2209` | -- |
| comment block | -- | `2211-2213` | -- |
| `defmethod evolve :order-placed` | `2865-2868` | `2215-2217` | -- |
| `defmethod evolve :result-available` | `2869-2890` | `2219-2231` | -- |
| comment block | -- | `2233-2239` | -- |
| `defmethod evolve :outpatient-visit` | `2891-2901` | `2241-2250` | -- |
| `defmethod evolve :outpatient-visit-end` | `2902-2913` | `2252-2256` | -- |
| comment block | -- | `2258-2262` | -- |
| `defmethod evolve :appointment` | `2914-2933` | `2264-2282` | -- |
| `defmethod evolve :reschedule` | `2934-2946` | `2284-2295` | -- |
| `defmethod evolve :appointment-cancel` | `2947-2950` | `2297-2299` | -- |
| `defmethod evolve :no-show` | `2951-2959` | `2301-2303` | -- |
| comment block | -- | `2305-2308` | -- |
| `defmethod evolve :procedure` | `2960-2967` | `2310-2310` | -- |
| comment block | -- | `2312-2316` | -- |
| `defmethod evolve :observation` | `2968-2978` | `2318-2322` | -- |
| comment block | -- | `2324-2327` | -- |
| `defmethod evolve :diagnostic-report` | `2979-2986` | `2329-2335` | -- |
| `defmethod evolve :medication-order` | `2987-2992` | `2337-2341` | -- |
| `defmethod evolve :medication-end` | `2993-3014` | `2343-2358` | -- |
| comment block | -- | `2360-2363` | -- |
| `defmethod evolve :care-plan-start` | `3015-3021` | `2365-2370` | -- |
| `defmethod evolve :care-plan-end` | `3022-3029` | `2372-2378` | -- |

Spans are paren-balanced, so a comment block standing between two forms
is its own row rather than absorbed into the form above it -- which is
how correction 2 below was found. **Eleven** such blocks sit inside the
cluster; all eleven are interior to `1954-2378` and move with it.

**First census correction, and it is again a PREMISE MISMATCH against
this session's own prompt.** The prompt says "defmulti evolve, its 28
defmethods, and the private helpers". There are **27** defmethods, not
28: `(grep -c '^(defmethod evolve ' )` over the cluster returns 27, and
the dispatch keys are the 27 listed above. The census's own section-1
list also enumerates 27, so the miscount is the prompt's alone -- but
the census's SUMMARY TABLE row says "the fold multimethod and its 27
methods, plus **three** private fold helpers" and then lists **four**
(`fold-condition-annotation`, `fold-conditions`, `resolve-appointment`,
`keep-appointment`). 1 + 27 + 4 = 32, the census's own form count, so
the table's "three" is the error and the arithmetic proves it.

Fixed forward with disclosure rather than stopped on, per
`rulings.md#R-stop-only-on-two-defensible-readings`: a form count is
mechanical, not a choice between two defensible readings, and the
cluster is enumerated form by form above.

**Second census correction, and the comment-block recipe fires a THIRD
time -- this time on a block the PREVIOUS extraction wrote.** The gap
between `fold-conditions` (ends 1996) and `defmulti evolve` (starts
1998) is not blank: lines **1991-1996** carry

```
;; --- ADR-0174 section 2(a) (arc 3b sweep 1): the encounter's fold now
;; lives in `ehrt.sim-engine.encounters`, with the gate and the stamp it
;; belongs beside -- `open-encounter`, `close-encounter`,
;; `cancel-open-encounter` and `reopen-encounter`, moved verbatim along
;; with the header comment that introduced them. The methods below call
;; them `encounters/`-qualified; nothing else about this fold changed.
```

which `dd956b0` wrote one session ago. It moves WITH the cluster, and
the reason is its own last sentence: "The methods below call them
`encounters/`-qualified" is a positional claim about the 27 defmethods,
so the block is false the moment they leave and true again the moment
it follows them. `engine.clj` does not lose the record of the encounters
move by letting it go -- the file's own header banner at `262-278`
already carries it, in full, including "The four lifecycle folds
`evolve` applies moved with them, from further down this file."

The state session found this class at a header comment `PatientState`'s
docstring cited; the encounters session found it at a header comment no
docstring cited at all; this session finds it at a comment a PREVIOUS
EXTRACTION LEFT BEHIND. Three for three. Block tops are not an
anecdote, and a form-span census cannot see any of them.

**Third census correction: `defmulti evolve` is PUBLIC, and it is the
only public mover.** Derived from the tree, not from section 1's
rendering: the four helpers are `defn-` and the defmulti carries no
`^:private`. So this extraction owes **exactly one** delegating def,
where `encounters` owed zero and `state` owed thirteen.

**The outgoing edges, confirmed mechanically.** A whole-symbol scan of
the cluster body (line comments and string literals stripped) against
every one of the 82 distinct top-level names `engine.clj` defines
returns eight, of which five are the cluster's own
(`evolve`, `fold-condition-annotation`, `fold-conditions`,
`resolve-appointment`, `keep-appointment`). The three that leave are

| name | occurrences | real home | how it appears today |
|---|---:|---|---|
| `demographics-from-persona` | 2 | `ehrt.sim-engine.state` | delegating def in `engine.clj` |
| `placeholder-demographics` | 1 | `ehrt.sim-engine.state` | delegating def in `engine.clj` |
| `next-appointment-ordinal` | 1 | `ehrt.sim-engine.streams` | delegating def in `engine.clj` |

plus the qualified symbols already standing in the text:
`state/observation-value-fields` x2, `encounters/open-encounter` x2,
`encounters/close-encounter` x2, `encounters/cancel-open-encounter`,
`encounters/reopen-encounter`.

**ZERO symbols still resolve in `engine.clj` itself**, so the prompt's
stop-and-report condition does not fire. Every one of the three
unqualified names is a delegating def whose definition lives in an
already-extracted namespace, and each is taken DIRECTLY into its real
home rather than routed back through the facade -- the treatment the
encounters session gave `next-encounter-ordinal`. `evolve`'s
`:require` set is therefore exactly **`encounters`, `state`, `streams`**,
the three the prompt expected: no `sim-model`, no malli, no
`clojure.*`, no `result`.

## 2. Step 2 -- the pre-move citation sweep (constraint 6)

The recipe `.agents/plans/engine-extraction-census.md` section 5 item 6
mandates, run BEFORE any form moved, at both recipe levels the state
session's correction 2 established.

Method, level 1: every docstring and comment line of the 32 moving
forms and the moving comment block was whitespace-normalised and cut
into every distinct six-word window of 28+ characters -- **1,509
phrases** -- and each searched, whitespace-normalised, against every
`.md`/`.clj`/`.cljc`/`.cljs`/`.edn`/`.txt`/`.yml`/`.yaml`/`.json`/`.sh`
file in the tree (**1,427 files**, `engine.clj` itself excluded, editor
and linter caches excluded). A whole-symbol name scan was run beside
it, for the five moving NAMES over the same population.

### 2a. Path-pinned snippet citations -- the class that cost the streams session a red

**ZERO, checked rather than assumed.** The gated shape is
`` `path` "snippet" `` (`patient-simulator-charter-test`'s and
`person-simulator-charter-test`'s own `citation-pattern`,
`#"`([^`\n]+)`\s+\"([^\"\n]+)\""`, resolved by `slurp` against the
named file). Both registers were read row by row:

| register | rows citing a `sim-engine` path | disposition |
|---|---|---|
| `components/person-simulator/docs/limitations.md` | rows 1 and 10, both `.../sim_engine/streams.clj`; one more cites `sim_check/check.clj` | **safe** -- no snippet is in the moving text |
| `components/patient-simulator/docs/limitations.md` | none -- its rows cite `gmf.clj`, `gmf_interpreter.clj`, `compile_trajectory.clj`, `persona.clj`, `emit_hl7.clj`, `check.clj`, `gmf-interpreter.md` | **safe** |

A tree-wide grep for a backticked path containing `engine.clj` followed
by whitespace and a quote returns nothing. **No gated citation anywhere
names `engine.clj` as a path** -- the third extraction running.

### 2b. Whole-symbol references to a moving name, outside `engine.clj`

The four PRIVATE movers have **no reference of any kind** outside
`engine.clj` except two frozen documents:

| name | hits outside `engine.clj` | disposition |
|---|---|---|
| `fold-condition-annotation` | census section 1 only | **safe** -- the census is the document this session corrects, in its own section 6 |
| `fold-conditions` | census section 1 only | **safe** |
| `resolve-appointment` | census section 1, and `.agents/session-records/2026-08-27-arc-3b-scheduling.md:165` | **safe** -- a record is frozen by construction |
| `keep-appointment` | census section 1 only | **safe** |

`evolve` itself is public and KEEPS its delegating def, so every
external reference continues to resolve against
`ehrt.sim-engine.engine/evolve`. That covers the two test call sites
(`persons_test.clj:303,308,315,322,324` and
`emit_hl7_test.clj:158,764`, all `engine/evolve`, and C1(a) forbids
touching a test file anyway) and every prose mention of the VAR
(`churn.clj:154`, `check.clj:23`,`:209`, `v2_replay.clj:31`,`:293`,
`:408`, `state.clj`, `encounters.clj`, `event_schema.clj`,
`pathway.clj`, `digest.clj`). This is the state session's `check.clj:
1005` disposition exactly: a path-or-namespace prose claim is safe when
a delegating def forwards it.

**No `with-redefs` on `evolve` exists anywhere in the tree** -- checked,
because constraint 1 records that exact hazard for `stream`. So the
delegating def carries no test's redefinition, and `replay`'s and
`run`'s own call sites stay unqualified, resolving through it.

### 2c. Positional citations -- recipe level 2

Run in both directions.

**Out of the cluster: three repoints owed, and they land with the move.**

| hit | text | disposition |
|---|---|---|
| `engine.clj:975` | inside `decide :identity-fill`, which STAYS: "`evolve` **below** rebuilds the whole state from it" | **REPOINT** -- the fold is not below any more |
| `engine.clj:1546` | inside `rejected-outcome`, which STAYS: "folded via `evolve`'s own identity method for this type, **below**" | **REPOINT** -- same shape, and it names `evolve :step-rejected` specifically |
| `docs/dev/simulator-architecture.md:92` | "(`engine.clj`'s `defmulti evolve`, dispatching on `(:event event)`)" | **REPOINT** -- and it fires the hand-owned-asset tripwire; see 2e |

Everything else in `engine.clj` that names `evolve` and stays behind was
read line by line and is **safe**: `:5`, `:19`, `:22` (`ns` docstring
doctrine, bare names), `:102`, `:116`, `:269-270` (the state and
encounters banners, which name `evolve` as a program landmark, not a
position), `:189`, `:203`, `:340`, `:531`, `:1154`, `:1157`, `:1914`,
`:2449`, `:2456`, `:2526`, `:2594`, `:2602`, `:3081`, `:3819` (bare
names, no positional claim), `:1697` ("downstream", a pipeline word),
and `:1925-1926` ("two defmethod-pairs up", which points at `decide
:medication-order`/`:medication-end` -- both STAYING, so it is
untouched).

**Into the cluster: four positional claims travel with the text, all
pre-existing and none made worse by the move.** Named here and moved
VERBATIM rather than corrected, which is the disposition the encounters
session gave "These three are the whole of the encounter's fold":

| line | text | why it is safe to move unchanged |
|---|---|---|
| `2223` | "(EmitState, **below**)" | `EmitState` is a `docs/sim-theory.md` functor and appears nowhere in `engine.clj` as a form; the "below" resolves to nothing today and to nothing after |
| `2261` | "(`keep-appointment`, **below**)" | `keep-appointment` is at `2100`, ABOVE it -- already false, and both move together in order |
| `2327` | ":result-available's own per-analyte flattening already establishes (**below**)" | `evolve :result-available` is at `2219`, ABOVE it -- already false, same treatment |
| `2348` | "riding the ground-truth event unchanged since `decide`, **below**" | `decide :medication-end` is at `1894`, ABOVE it -- already false |
| `2314` | "`ehrt.sim-engine.state`'s own header comment above `PatientState`" | a claim about `state.clj`'s internal layout, already repointed by the state session; this move does not touch it |

### 2d. Phrase hits that are shared prose, not citations

The 1,509-phrase sweep returned hits in **47 files** (an editor cache
excluded). It is dominated by three boilerplate provenance stamps --
`GMF coverage Wave D stage D1/D2 (2026-08-02, ADR-0029 ...)` (23
files), `ADR-0174 section 2(a) (arc 3b sweep 1)` (6 files) and the
`evolve` defmulti docstring's own doctrine sentences, which
`docs/dev/simulator-architecture.md`, `notes/sim/ADRs.md`,
`components/sim/docs/sim-theory.md`, `components/sim/docs/event-
sourcing.md` and `components/patient-simulator/docs/trajectory-
computation.md` each restate because that doctrine is `sim/ADR-0008`
and restating it is what those documents are for. Every hit was
inspected: none pairs a phrase with a path, and none is read by a gate.
**Disposition: safe, as a class** -- the same finding the streams,
state and encounters sessions each recorded, met a fourth time at
nearly double the phrase count.

### 2e. Gates and tripwires checked, and ONE fires

* **The hand-owned-asset tripwire, and this is the first extraction to
  trip it.** `hand-owned-assets.edn`'s four sources are
  `docs/dev/simulator-architecture.md` (cited by TWO rows),
  `components/corpus/docs/palgebra-design.md`,
  `components/corpus/docs/pipeline.edn` and
  `demos/scenarios/ed-tuesday/README.md`. The encounters session could
  record "no moving name occurs in any of them"; this one cannot.
  `simulator-architecture.md:92` names `engine.clj`'s `defmulti evolve`
  by defining form, and the move makes that false -- exactly register
  row L2-17's class, the one the de-scaffold session converted twelve
  stale `engine.clj:NNN` citations to `defn` names to close.
  So the doc is repointed, and `gt-emitters.svg`'s row
  (`:verdict :fresh`, `:reviewed-at "75cde83f"`) owes a
  `:reviewed-at` bump. `two-clocks.svg` cites the same source but is
  `:verdict :stale`, and claim (d) of
  `ehrt.docs-tooling.hand-owned-asset-freshness-test` skips non-`:fresh`
  rows, so it owes nothing.
  **The bump cannot ride the commit that edits the source** -- the test
  reads `git log -1 -- <source>`, so no commit can carry the sha that
  names itself. The move commit is therefore a RED-FIRST commit under
  `rulings.md#R-red-pushed-with-green`, pushed together with its
  one-line successor and never alone. PREDICTED, not discovered: the
  registry's sources were read before the doc was touched.
* **`engine.clj:NNN` line citations.** Unchanged from the encounters
  sweep: three in `engine_test.clj` (`:2440`, `:2446`, `:2543`), all
  stamped "at ADR-0171's design HEAD `c1b996e`", plus `streams.clj:57`
  quoting ADR-0171's `engine.clj:225`. All explicitly historical, none
  resolved by a gate. Untouched.
* **`ehrt.docs-tooling.sim-purity-lint-test`** globs each sim-family
  brick's whole `src` tree, so a new file under
  `components/sim-engine/src` is scanned automatically and needs no
  registration. `evolve.clj` introduces no
  `atom`/`ref`/`agent`/`volatile!`/`set-validator!`.
* **`stale-path-test`** asserts only that `engine.clj`'s path trips no
  retired-prefix rule. The path still exists; a new sibling file is
  invisible to it, as it was to the three prior extractions.
* **Namespace collision.** `components/sim-engine/src/ehrt/sim_engine/`
  holds `churn`, `encounters`, `engine`, `event_schema`, `interface`,
  `order_profiles`, `person_fold`, `state` and `streams` -- no `evolve`
  of any kind, and no `ehrt.sim-engine.evolve` is referenced anywhere in
  the tree. Unlike `encounters`, there is not even a test namespace of
  that name.
* **`interface.clj` does NOT re-export `evolve`** -- its sim-engine
  re-exports are `run`, `config-keys`, `compile-patient`, `person-plan`,
  `valid-persons?`, `replay`, `documented-step-rejection-reasons`,
  `mix64`, `stream-scheme`, `stream-seed`, `stream`, `newborn-id-tag`.
  Constraint 4's list is unaffected and the file is not opened, per this
  session's fences.

**Gate for step 2: this hit list is committed before the move.**

## 3. Step 3 -- the extraction (`5637cbe`)

`ehrt.sim-engine.evolve`, 32 forms plus the eleven interior comment
blocks, **469 lines** (425 moved, 44 of `ns`). No collision:
`components/sim-engine/src/ehrt/sim_engine/` held `churn`, `encounters`,
`engine`, `event_schema`, `interface`, `order_profiles`, `person_fold`,
`state` and `streams`, and no `ehrt.sim-engine.evolve` was referenced
anywhere in the tree.

**The verbatim claim is proven, not asserted.** `sed -n '1954,2378p'` of
`engine.clj` at `315263f`, diffed against `evolve.clj` lines `45-469`,
is 425 lines against 425 differing on **exactly eight**:

| difference | count | why it is required |
|---|---:|---|
| `(defn- X` -> `(defn X` | 4 | constraint 5 -- a private mover becomes public in its new namespace |
| `(placeholder-demographics ...)` -> `(state/...)` | 1 | edge into `state`, taken DIRECTLY rather than through `engine.clj`'s delegating def |
| `(demographics-from-persona ...)` -> `(state/...)` | 2 | the same |
| `(next-appointment-ordinal ...)` -> `(streams/...)` | 1 | edge into `streams`, the same |

Nothing else differs. Every bare-name mention of those three inside a
docstring or a comment is left alone -- `:2021`, `:2071` and `:2266` at
their old numbers -- exactly as the encounters move left its own.

**ONE delegating def, and it is a multimethod, so the sharing was
verified live rather than assumed.** In a `-M:dev` load:

* `(identical? engine/evolve evolve/evolve)` is **true** -- one
  `clojure.lang.MultiFn` object, not two;
* `(count (methods engine/evolve))` is **27**, equal to
  `(count (methods evolve/evolve))`, and the two dispatch-key sets are
  `=`;
* folding an `:admission` through `engine/evolve` returns
  `:status :admitted` -- the method registered in the NEW namespace,
  dispatched through the OLD var;
* `engine.clj`'s `replay` and `run` therefore keep calling `evolve`
  unqualified through that def, exactly as they did, and no test file
  changed;
* the four helpers resolve public in `ehrt.sim-engine.evolve`
  (`[true true true true]`) and resolve to NOTHING in
  `ehrt.sim-engine.engine` (`[false false false false]`) -- constraint 5
  in both directions;
* `evolve`'s alias set, read off the LOADED namespace rather than off
  its `ns` form, is exactly `(encounters state streams)`.

**The whole `engine.clj` diff is FOUR hunks and nothing else**: the
`:require` insertion, the two repoints section 2c found owing
(`:975`, `:1546`), and the block replaced by a banner plus the
delegating def. **ZERO qualified call sites**, where the encounters move
needed ten -- the only mover `engine.clj` still calls is `evolve`
itself, and a delegating def is what it calls. A whole-symbol scan of
the file's CODE (comments and string literals stripped) returns zero
unqualified references to any of the four helpers.

`engine.clj` is 4,234 -> **3,844 lines**, 141 forms plus `ns` -> **110**.
The partition closes at **182** -- 181 before, plus this move's one
delegating def -- and the per-session arithmetic holds end to end:
157 - 16 + 11 = 152, - 14 + 13 = 151, - 10 + 0 = 141, - 32 + 1 = 110,
and 110 + 16 + 14 + 10 + 32 = 182.

`ehrt.sim-engine.interface` re-exports none of the five movers and was
not opened, per this session's fences.

### The derived count the diff does not show

`docs/dev/simulator-architecture.md:92` is one line longer after the
repoint, and that file is a `:sim` reading-set member: the set moved
**1339 -> 1340** lines, headroom **66 -> 65** against an unchanged 1405
budget, so `.agents/state-derived.md` is regenerated in the same commit.
**Found by the suite, not by inspection** --
`ehrt.docs-tooling.state-derived-test` went red on the first full run of
this session and was the ONLY failure in it. That is the standing
`feedback_repo_gate_ordering` lesson met head-on: a docs edit's
consequence was invisible in the diff and only a whole-suite run saw it.

### Gates

* **Suite.** `make test` exit 0, zero failures and zero errors:
  **408 namespaces, 4,751 tests, 24,117 assertions**, run over the tree
  this session pushes -- extraction, tripwire bump, record, prompt
  archive, roadmap row and regenerated docs all present, so every
  derived-count gate saw its final input. Namespaces and tests are
  IDENTICAL to the encounters extraction's own recorded run (408 /
  4,751); assertions are **+2** against its 24,115, and the +2 is
  explained to the assertion rather than waved at.
  `ehrt.docs-tooling.io-vocabulary-lint-test` is a `doseq` over every
  production source file with one `is` per file plus a population guard;
  it reported **125** assertions in EACH of the two projects it runs in,
  against **124** in the encounters session, and the tree gained one
  file. One new file is +1 twice. The same benign class the streams,
  state and encounters sessions each recorded, and the class this
  session's own prompt named in advance. Execution time 16 min 14 s, on
  an UNSAMPLED host -- reported as the run's own line, not offered as a
  timing claim.

  **The FIRST run of this session was RED**, and the disclosure matters
  more than the green one: `ehrt.docs-tooling.state-derived-test` failed
  on a stale `.agents/state-derived.md`, because the
  `simulator-architecture.md` repoint added one line to a `:sim`
  reading-set member. One failure in the whole suite, in a gate no part
  of the refactor touches, from a docs edit of two lines.
* **`bin/regression-oracle 315263f 5637cbe`** -- the script's own
  output: `IDENTICAL: every root's digest matches between 315263f and
  5637cbe`, over **41 roots**, `declared-digest-change: no (soundness:
  yes outside the leading docstring)`. No declaration was owed and none
  was made.
* **`clojure -M:poly check`** OK. Because `poly check` does not compile
  -- a standing finding of the arc-4 sweeps, not new here -- the real
  check is the `-M:dev` load above, which resolved and dispatched
  through both vars.

## 4. Step 4 -- the ground-truth bracket

**`bin/ground-truth-bracket 315263f 5637cbe`** -- `IDENTICAL: every
digested root's :ground-truth matches between 315263f and 5637cbe (38
roots)`, coverage `38 roots carry :ground-truth and are digested; 3
skipped (no such key): appendicitis.edn, ear-infections.edn,
sore-throat.edn`; `declared-digest-change: no`.

Both brackets IDENTICAL with no declaration, for the **fourth**
consecutive extraction. It is what ruling S1(a)'s equivalence proof asks
for in place of red-before-green, and taking the clusters in the
census's dependency order is what keeps earning it.

## 5. Step 5 -- the tripwire, the suite at the tip, and what the next session takes

### The tripwire, and why this extraction is a RED-FIRST commit

`docs/dev/simulator-architecture.md` is `gt-emitters.svg`'s `:source` in
`components/docs-tooling/resources/docs-tooling/hand-owned-assets.edn`,
and `ehrt.docs-tooling.hand-owned-asset-freshness-test` claim (d)
compares `:reviewed-at` against `git log -1` on the SOURCE. No commit
can carry the sha that names itself, so `5637cbe` is a RED-FIRST commit
under `rulings.md#R-red-pushed-with-green` and `f156ff9` is its
immediate successor, pushed with it and never alone.

The review was done, not skipped: the source diff is ONE hunk, in
section 2; section 4's own EQUATION BLOCK -- `walk`, `engine`,
`emitH`/`emitF`, `replay`, `check`, which is what this row's trigger
names -- is byte-identical across it, and `replay` is still
`engine.clj`'s own `defn`. Both brackets IDENTICAL is the positive
evidence that no arrow on the drawing moved. `two-clocks.svg` cites the
same source but is `:verdict :stale`, and claim (d) skips non-`:fresh`
rows, so it owed nothing.

**This is the FIRST of the four engine extractions to fire this
tripwire.** The three before it could each record that no moving name
occurred in any of the four registry sources. This one could not,
because `defmulti evolve` was named there by DEFINING FORM -- which is
register row L2-17's own class, the one the de-scaffold session closed
by converting twelve stale `engine.clj:NNN` citations into `defn` names.
A `defn`-name citation is more durable than a line citation and still
not durable against an extraction. Predicted, not discovered: the
registry's sources were read during the pre-move sweep, at `315263f`,
before the doc was touched.

### The DAG, and what the next session takes

**No back-edge into the remaining `engine.clj` from `evolve`. Confirmed
two ways.** Structurally: `evolve.clj`'s `ns` requires exactly
`encounters`, `state` and `streams`, read off the LOADED namespace.
Mechanically: the pre-move whole-symbol scan against all 82 of
`engine.clj`'s top-level names returned eight, five of them the
cluster's own and three delegating defs whose definitions live in
already-extracted namespaces.

So the census's remaining order is unchanged: `config` and `assignment`
are the last leaves, then **`fold`** (3 forms -- `replay`,
`update-beds`, `bed-correction-event-types` -- whose `replay` call into
`evolve` is now a call into an extracted namespace), then `log-index`,
then `decide`, with `run` last as the facade's residue.
Application-path unification stays last, against section 4 of the
census, and is also `roadmap.md#event-stream-mutation`'s injection
point.

## 6. Census corrections, one sentence each

1. **`evolve` has 27 defmethods, not 28**, and the census's own section-1
   list agrees at 27 while its SUMMARY TABLE says "27 methods plus THREE
   private fold helpers" and then lists four -- 1 + 27 + 4 = 32 is the
   census's own form count, so the table's "three" is the error, and
   this session's prompt inherited the miscount from somewhere other
   than the list.
2. **The cluster's forms are separated by ELEVEN comment blocks, one of
   which -- `1991-1996` -- the PREVIOUS EXTRACTION WROTE**, and it moves
   with the cluster because its last sentence ("The methods below call
   them `encounters/`-qualified") is a positional claim about the 27
   methods; the comment-block recipe has now fired in three consecutive
   extractions, on a block a docstring cited, a block nothing cited, and
   a block a prior mover left behind, which retires any reading of it as
   an anecdote.
3. **`defmulti evolve` is PUBLIC and is the cluster's only public mover**,
   so this extraction owes exactly one delegating def -- and because a
   `def` of a multimethod shares the one multifn object, that single def
   carries all 27 methods across the seam, which no prior extraction's
   delegating def had to do.
4. **Four positional `below`s inside the moving text were ALREADY FALSE
   where they stood** (`:2223` `EmitState`, `:2261` `keep-appointment`,
   `:2327` `:result-available`, `:2348` `decide`), each naming something
   ABOVE it or nothing in the file at all; moved verbatim and named here
   rather than corrected inside a commit whose whole claim is that the
   moved text is unchanged -- the encounters session's own disposition
   for "These three are the whole of the encounter's fold".
5. **The census's sizes are now stale by THREE extractions, as designed**:
   `engine.clj` is 4,234 lines / 141 forms plus `ns` at this session's
   start against section 1's 4,884 / 157, and 3,844 / 110 at its end.

## 7. Disclosures

* **A premise mismatch in this session's own prompt, fixed forward with
  disclosure rather than stopped on.** The prompt's "its 28 defmethods"
  does not hold; there are 27. Under
  `rulings.md#R-stop-only-on-two-defensible-readings` STOP-AND-REPORT
  binds where two readings are both defensible; a form count is
  mechanical, and the cluster is enumerated form by form in section 1.
  The consequence is a SMALLER cluster than the prompt anticipated, not
  a larger one. The prompt's other two hedges were both right: it asked
  to be corrected from the tree about the delegating defs (one, the
  defmulti, exactly as it guessed), and it asked for the multifn
  delegation to be verified live rather than assumed, which section 3
  does.
* **The census's two blocks did become contiguous**, as the prompt
  anticipated -- with the six-line banner of correction 2 standing in
  the seam.
* No fresh clone; the existing clone was at `54551d7`, clean, and equal
  to `origin/main`. Every span was re-derived, by paren balance.
* `make test` runs `poly test :all skip:integration`, so the integration
  tier did not execute here. That is the standing W-1 disclosure, not
  new to this session.
* **No timing figure is offered.** The Windows side was not sampled
  before either suite run, and `reference_measurement_host_contamination`'s
  rule is that an unsampled host cannot carry a timing claim.
* Three pieces of text that did not move were edited, all three because
  the move makes them false, and all three dispositioned in section 2c
  BEFORE the move rather than discovered during it: `engine.clj:975`'s
  and `:1546`'s "below", and `docs/dev/simulator-architecture.md:92`'s
  "engine.clj's `defmulti evolve`". The streams, state and encounters
  sessions each set this precedent for the first two; the third is new.
* `.agents/state-derived.md` still records the `:docs` reading set at
  787 lines against a 785 budget, headroom **-2**. PRE-EXISTING, carried
  forward from the streams session through the state and encounters
  sessions, untouched by anything here, and named again rather than left
  to be rediscovered. The `:sim` set moved 1339 -> 1340 in this session
  and stays 65 under its own budget.
* `make state-derived` was run for the generated
  `INDEX.md`/`state-derived.md` rows. `make traces` was not run and no
  trace moved: a namespace split that both brackets call IDENTICAL
  cannot move a derived capture.

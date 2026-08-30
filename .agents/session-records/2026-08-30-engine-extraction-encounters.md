# Engine namespace extraction, 3 of N: the `encounters` cluster

Session record, 2026-08-30. HEAD at start `3e0b65a`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) (`engine.clj` stays the facade, moved PUBLIC
vars get delegating defs, no test file changes) and S1(a) (an
equivalence proof replaces red-before-green).

The census's DAG (`.agents/plans/engine-extraction-census.md` §3a) puts
`encounters` after `streams` and `state`, both already extracted, and
before `evolve`, which calls four of its forms.

## 1. Step 1 -- the tip, and the spans re-derived

`3e0b65a`. Every span below was re-derived from THIS tree with a
form-span scanner, not transcribed from the census, whose own numbers
are at `517a96d` -- two extractions ago.

`engine.clj` is **4,419 lines / 151 top-level forms plus the `ns`**
here, against the census's 4,884 / 157. The partition still closes:
`streams` took 16 forms and left 11 delegating defs (157 - 16 + 11 =
152), `state` took 14 and left 13 (152 - 14 + 13 = 151), and
151 + `streams`' 16 + `state`' 14 = **181** = 157 + 11 + 13. The 24
delegating defs standing in `engine.clj` today are exactly those two
sessions' 11 + 13, counted in the file.

| form | census (`517a96d`) | here (`3e0b65a`) | privacy |
|---|---|---|---|
| header comment (moves with the block) | -- | -- | -- |
| `defn- encounter-openable?` | 630-650 | 261-280 | `defn-` |
| `def ^:private compiled-encounter-openers` | 651-657 | 282-287 | `def ^:private` |
| `def ^:private compiled-encounter-closers` | 658-666 | 289-296 | `def ^:private` |
| `defn- gate-compiled-encounters` | 667-722 | 298-352 | `defn-` |
| `def ^:private two-encounter-event-types` | 723-734 | 354-364 | `def ^:private` |
| `defn- stamp-encounter` | 735-771 | 366-401 | `defn-` |
| comment block, "the encounter, folded" | -- | 2113-2120 | -- |
| `defn- open-encounter` | 2587-2600 | 2122-2134 | `defn-` |
| `defn- close-encounter` | 2601-2619 | 2136-2153 | `defn-` |
| `defn- cancel-open-encounter` | 2620-2631 | 2155-2165 | `defn-` |
| `defn- reopen-encounter` | 2632-2647 | 2167-2181 | `defn-` |

**First census correction, and it is a PREMISE MISMATCH against this
session's own prompt.** The prompt says "six of these are `defn-` ...
the defs that are public get delegations". Neither half holds against
the live tree. **Seven** are `defn-` (`encounter-openable?`,
`gate-compiled-encounters`, `stamp-encounter`, `open-encounter`,
`close-encounter`, `cancel-open-encounter`, `reopen-encounter`), and
the remaining three are not public `def`s but `def ^:private` --
which the census's own §1 list also renders as bare `def`, so the
prompt inherited the error rather than introducing it. **All ten
movers are private, so this extraction owes ZERO delegating defs** and
`engine.clj`'s public surface is unchanged by construction rather than
by inspection. Every one of the ten becomes public in the new
namespace per constraint 5, and every call site qualifies.

Fixed forward with disclosure rather than stopped on, per
`rulings.md#R-stop-only-on-two-defensible-readings`: there is only one
defensible reading of constraint 5 for a private mover, and the
mismatch is mechanical (a privacy census, run and tabled above), not a
choice between two readings.

**Second census correction.** The cluster is NOT contiguous, as the
prompt says -- but its two blocks are `261-401` and `2113-2181`, and
the second block's true top is a **comment block at 2113-2120**, not
`open-encounter` at 2122. `;; --- ADR-0174 section 2(a) (arc 3b sweep
1): the encounter, folded ---` plus six lines of prose introduces the
four lifecycle forms and belongs to them. This is exactly census
correction 2 of the state session, met a second time: a form-span
census enumerates forms, and the gap between two forms is invisible to
it. The block moves with the cluster.

**Third census correction.** That comment block says "These three are
the whole of the encounter's fold" and there are **four** forms under
it -- `open-encounter`, `close-encounter`, `cancel-open-encounter`,
`reopen-encounter`. Wrong in the live tree and wrong before this
session (`reopen-encounter` arrived with `:cancel-discharge`'s
reinstatement after the block was written). Named here and moved
verbatim, not fixed: the move commit's whole claim is that the moved
text is unchanged, and correcting an unrelated pre-existing error
inside it would make the diff harder to audit.

**The one outgoing edge, confirmed mechanically.** A whole-symbol scan
of the cluster body (line comments and string literals stripped) for
every one of the 92 distinct top-level names `engine.clj` defines
returns exactly **one**: `next-encounter-ordinal`, called once by
`open-encounter`. That var lives in `ehrt.sim-engine.streams` and is a
delegating def in `engine.clj`, so the new namespace requires
`streams` DIRECTLY rather than routing back through the facade, as the
prompt instructs. The cluster body uses **no qualified symbol at all**
-- so `encounters`' only `:require` is `ehrt.sim-engine.streams`,
narrower than the prompt's "beyond clojure/malli/sim-model": it needs
none of those three either.

## 2. Step 2 -- the pre-move citation sweep (constraint 6)

The recipe `.agents/plans/engine-extraction-census.md` §5 item 6
mandates, run BEFORE any form moved, at **both** recipe levels the
state session's correction 2 established: docstring phrases, and
positionally-cited comment blocks adjacent to the moving forms.

Method, level 1: every docstring and comment line of the ten moving
forms and the moving comment block was whitespace-normalised and cut
into every distinct six-word window of 28+ characters -- **829
phrases** -- and each searched, whitespace-normalised, against every
`.md`/`.clj`/`.cljc`/`.cljs`/`.edn`/`.txt`/`.yml`/`.yaml`/`.json`/`.sh`
file in the tree (**1,672 files**, `engine.clj` itself excluded, editor
and linter caches excluded). A whole-symbol name scan was run beside
it, for the ten names over the same population.

### 2a. Path-pinned snippet citations -- the class that cost the streams session a red

**ZERO, checked rather than assumed.** The gated shape is
`` `path` "snippet" `` (`patient-simulator-charter-test`'s and
`person-simulator-charter-test`'s own `citation-pattern`,
`#"`([^`\n]+)`\s+\"([^\"\n]+)\""`, resolved by `slurp` against the
named file). Both registers were read row by row:

| register | rows citing a `sim-engine` path | disposition |
|---|---|---|
| `components/person-simulator/docs/limitations.md` | rows 1 and 10, both `.../sim_engine/streams.clj` | **safe** -- already repointed by the streams session; neither snippet is in the moving text |
| `components/patient-simulator/docs/limitations.md` | none -- its rows cite `gmf.clj`, `gmf_interpreter.clj`, `compile_trajectory.clj`, `persona.clj`, `emit_hl7.clj`, `check.clj`, `locators_doc_test.clj`, `process.clj`, `hazards.clj` and three ADRs | **safe** |

**No gated citation anywhere in the tree names `engine.clj` as a
path.** Checked by scanning both registers for the pattern and by
grepping every `components/*/docs` and `docs/` markdown file for a
backticked `engine.clj` path followed by a quoted snippet: no hit.

### 2b. Whole-symbol references to a moving name, outside `engine.clj`

**No source-code reference exists.** All ten movers are private, so
nothing outside the namespace could call them, and the scan confirms
it: every hit outside `engine.clj` is prose, a frozen record, or a
tool cache. Dispositioned by class, with the one exception broken out:

| class | files | disposition |
|---|---|---|
| frozen historical records and prompts (`.agents/session-records/*`, `.agents/prompts/*`, `.agents/plans/*`, `notes/adr/0174-*`) | 11 | **safe** -- a record is frozen by construction; the census itself is the document this session corrects, in its own §6 |
| test-file comments naming a var by bare name (`engine_test.clj` ×4, `encounters_test.clj` ×5, `check_test.clj`) | 3 | **safe** -- prose, not calls; and C1(a) forbids touching a test file |
| sibling prose restating the same ADR fact (`state.clj:384-391`, `event_schema.clj`, `config.clj:241`, `compile_trajectory.clj`, `gmf_interpreter.clj`, `patient-state-model.md`, `docs/consuming-ground-truth.md`) | 7 | **safe** -- none pairs a phrase with a path, none is read by a gate |
| `components/sim-check/src/ehrt/sim_check/check.clj:90` | 1 | **REPOINT** -- see below |

`check.clj:90` reads "`run`'s stamp reads the first participant's own
open encounter (``engine.clj``'s `stamp-encounter`)". It is NOT the
gated shape -- the backticked path is followed by `'s`, not by
whitespace and a quote, so `citation-pattern` does not match it -- but
it is a path-plus-name prose claim that the move makes **false**, and
unlike the state session's structurally identical `check.clj:1005`
(""engine.clj's own `PatientState` status enum"", safe because
`PatientState` KEPT a delegating def) this one has no delegating def to
forward it: `stamp-encounter` is private and constraint 5 forbids one.
Repointed to `encounters.clj` by the move commit.

### 2c. Positional citations -- recipe level 2

The state session's correction 2 says to sweep for comment blocks
adjacent to moving forms too. Run in both directions.

**Into the cluster.** `engine.clj` was scanned for the positional
vocabulary (`(above)`, `(below)`, `just above`, `just below`, `header
comment`, `comment block`, `this namespace's own`) on every line
mentioning a moving name:

| hit | text | disposition |
|---|---|---|
| `engine.clj:284` | "`gate-compiled-encounters` (below) is the only reader" | **safe** -- inside the cluster, and both forms move together in order |
| `engine.clj:675-677` | "`gate-compiled-encounters` **above** says why the re-bracket belongs here" | **REPOINT** -- `decide :registered` stays in `engine.clj` and the gate does not, so "above" becomes false. The exact shape of the state session's own `:2777` repoint |
| `engine.clj:861` | "the `:encounters` opt-in: `encounter-openable?` is the same question" | **safe** -- names a var by bare name with no positional claim |
| `engine.clj:2293`, `:2299` | `evolve :admission`'s comment on `open-encounter`'s read order | **safe** -- bare name, no positional claim |

**Out of the cluster.** The moving comment block (2113-2120) and the
ten docstrings were searched for anything citing them by position from
elsewhere. `"These three are the whole"` and `"the encounter, folded"`
occur **nowhere but `engine.clj` itself**, so unlike `state`'s `M6
Task 1` block this one anchors no cross-form citation and moves
because it belongs to the forms, not because a docstring pins it.

### 2d. Phrase hits that are shared prose, not citations

The 829-phrase sweep returned hits in **19 files**, dominated by two
boilerplate provenance stamps -- `ARC 3B SWEEP 3 (ADR-0174 section
2(b))` and `ADR-0174 section 2(a) (arc 3b sweep 1)` -- which appear as
section banners in `state.clj`, `check.clj`, `run.clj`,
`emit_fhir_test.clj`, `check_test.clj`, `run_test.clj` and three
`demos/scenarios/*/config.edn` files. `roadmap.md`'s TS-3 row (20
phrases) restates `gate-compiled-encounters`' docstring, which is what
a roadmap row is for. Every one inspected is a SIBLING restating the
same ADR language about the same fact: none pairs the phrase with a
path, and none is read by a gate. **Disposition: safe, as a class** --
the same finding the state session recorded about the recipe's own
yield, met again at half the phrase count.

### 2e. Gates and tripwires checked, and clear

* **The hand-owned-asset tripwire.** Its four sources are
  `docs/dev/simulator-architecture.md`, `components/corpus/docs/
  palgebra-design.md`, `components/corpus/docs/pipeline.edn` and
  `demos/scenarios/ed-tuesday/README.md`. **No moving name occurs in
  any of them** -- `simulator-architecture.md` cites `defn` names
  rather than `engine.clj:NNN` lines since the de-scaffold session, and
  none of the thirteen it names is in this cluster. Nothing this
  session edits is a tripwire source, so no `:reviewed-at` bump is
  owed.
* **`engine.clj:NNN` line citations.** Three live in
  `engine_test.clj` (`:2440`, `:2446`, `:2543`), all naming
  `engine.clj:480` and all explicitly stamped "at ADR-0171's design
  HEAD `c1b996e`" -- already historical, already stale by two
  extractions, and no gate resolves them. `streams.clj:57` quotes
  ADR-0171's own `engine.clj:225` the same way. Untouched.
* **`stale-path-test`** asserts only that
  `components/sim-engine/src/ehrt/sim_engine/engine.clj` trips no
  retired-prefix rule. The path still exists; a new sibling file is
  invisible to it, as it was to the streams and state sessions.
* **Namespace collision.** `components/sim-engine/src/ehrt/sim_engine/`
  holds `churn`, `engine`, `event_schema`, `interface`,
  `order_profiles`, `person_fold`, `state` and `streams` -- no
  `encounters` of any kind. A test namespace
  `ehrt.sim-engine.encounters-test` DOES already exist
  (`test/.../encounters_test.clj`, arc 3b sweep 1's own gate, requiring
  `ehrt.sim-engine.engine`). That is a different namespace and no gate
  pairs src with test in either direction -- `state.clj` shipped with
  no `state_test.clj`, and `encounters-test` has shipped with no
  `encounters.clj` since sweep 1. It is not touched, per C1(a).

**Gate for step 2: this hit list is committed before the move.**

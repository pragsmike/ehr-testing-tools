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

## 3. Step 3 -- the extraction (`dd956b0`)

`ehrt.sim-engine.encounters`, ten forms plus the header comment block,
243 lines. No collision: `components/sim-engine/src/ehrt/sim_engine/`
held `churn`, `engine`, `event_schema`, `interface`, `order_profiles`,
`person_fold`, `state` and `streams`.

**The verbatim claim is proven, not asserted.** `sed -n '261,401p'` and
`sed -n '2113,2181p'` of `engine.clj` at `cdd49a6`, diffed against
`encounters.clj` lines 33-173 and 175-243, are 141 and 69 lines that
differ on **exactly eleven lines**:

| difference | count | why it is required |
|---|---:|---|
| `(defn- X` -> `(defn X` | 7 | constraint 5 -- a private mover becomes public in its new namespace |
| `(def ^:private X` -> `(def X` | 3 | the same, for the three `def`s the census renders as bare `def` |
| `(next-encounter-ordinal patient)` -> `(streams/next-encounter-ordinal patient)` | 1 | the cluster's single outgoing edge, taken DIRECTLY into `streams` rather than back through `engine.clj`'s delegating def, per this session's prompt |

Nothing else differs on either block. The comment block moved
untouched, "These three" and all.

**ZERO delegating defs**, and that is not an omission but the
consequence of §1's first correction: C1(a) owes a delegating def for a
moved PUBLIC var, all ten movers were private, and constraint 5
positively forbids a def that would widen the engine's public surface.
Verified live rather than assumed, in a `-M:dev` load:

* all ten `ns-resolve` public in `ehrt.sim-engine.encounters` --
  `[true true true true true true true true true true]`;
* **none** resolves in `ehrt.sim-engine.engine` --
  `[false false false false false false false false false false]`;
* `encounters`' alias set, read off the LOADED namespace rather than
  off its `ns` form, is exactly `(streams)`.

`ehrt.sim-engine.interface` re-exports none of the ten and was not
opened. No test file changed, `encounters_test.clj` included.

**The whole `engine.clj` diff is TWELVE hunks and nothing else**: the
`:require` insertion, the two moved blocks replaced by banners, the two
repoints §2 found owing, and ten `encounters/`-qualified call sites
across eight hunks (`decide :registered`'s gate call, the two
`encounter-openable?` guards, `evolve :admission`/`:outpatient-visit`'s
two `open-encounter`s and two `close-encounter`s, `:cancel-admit`'s
`cancel-open-encounter`, `:cancel-discharge`'s `reopen-encounter`, and
`run`'s `stamp-encounter`). A whole-symbol scan of the file's CODE
(comments and string literals stripped) for the ten names returns **zero
unqualified references remaining**.

`engine.clj` is 4,419 -> **4,234 lines**, 151 forms plus `ns` -> **141**.
The partition still closes: 141 + `streams`' 16 + `state`'s 14 +
`encounters`' 10 = **181**, the same 181 as before.

### Gates

* **Suite.** `make test` exit 0, zero failures and zero errors:
  **408 namespaces, 4,751 tests, 24,115 assertions**. Namespaces and
  tests are IDENTICAL to the state extraction's own recorded run
  (408 / 4,751); assertions are **+2** against its 24,113, and the +2 is
  explained to the assertion rather than waved at.
  `ehrt.docs-tooling.io-vocabulary-lint-test` is a `doseq` over every
  production source file with one `is` per file plus a population guard;
  it reported **124** assertions in EACH of the two projects it runs in,
  against **123** in the state session, and the tree gained one file.
  One new file is +1 twice. The same benign class the streams and state
  sessions each recorded.
* **`bin/regression-oracle cdd49a6 dd956b0`** -- the script's own
  output: `IDENTICAL: every root's digest matches between cdd49a6 and
  dd956b0`, `declared-digest-change: no (soundness: yes outside the
  leading docstring)`. No declaration was owed and none was made.
* **`clojure -M:poly check`** OK. Because `poly check` does not compile
  -- a standing finding of the arc-4 sweeps, not new here -- the real
  check is the `-M:dev` load above, which also loaded
  `ehrt.sim-check.check` (the file this session repointed) and
  evaluated `gate-compiled-encounters` on `nil` and `[]` and
  `encounter-openable?` on the legacy `:new` arm.

## 4. Step 4 -- the ground-truth bracket

**`bin/ground-truth-bracket cdd49a6 dd956b0`** -- `IDENTICAL: every
digested root's :ground-truth matches between cdd49a6 and dd956b0 (38
roots)`, coverage `38 roots carry :ground-truth and are digested; 3
skipped (no such key): appendicitis.edn, ear-infections.edn,
sore-throat.edn`; `declared-digest-change: no`.

Both brackets IDENTICAL with no declaration is the strongest shape a
pure-refactor commit can report, and it is what ruling S1(a)'s
equivalence proof asks for in place of red-before-green. It is also the
third such bracket in a row for this program, which is the point of
taking the clusters in the census's order.

## 5. Step 5 -- the DAG, and what the next session takes

**The census DAG has NO back-edge into the remaining `engine.clj` from
`encounters`. Confirmed, two ways.** Structurally: `encounters.clj`'s
`ns` form requires `ehrt.sim-engine.streams` and NOTHING else -- not
`sim-model`, not malli, not `clojure.*` -- which is narrower than this
session's prompt anticipated. Mechanically: a whole-symbol scan of the
cluster body (comments and string literals stripped) against every one
of the 92 distinct top-level names `engine.clj` defines returned exactly
one, `next-encounter-ordinal`, which is `streams`'.

So the DAG's remaining order is unchanged: `config` and `assignment`
are the last leaves, then **`evolve`** (32 forms, the natural next
cluster -- its edges into `encounters` and `state` are now both into
extracted namespaces, and its one former back-edge through
`observation-value-fields` was broken by the state session), then
`fold`, then `log-index`, then `decide`, with `run` last as the
facade's residue.

Application-path unification stays last, against §4 of the census, and
is also `roadmap.md#event-stream-mutation`'s injection point.

## 6. Census corrections, one sentence each

1. **All ten `encounters` movers are PRIVATE -- seven `defn-` and three
   `def ^:private` -- so the cluster owes ZERO delegating defs**, where
   §1's span list renders the three as bare `def` and this session's
   prompt, inheriting that, said six were `defn-` and the rest public;
   a privacy census is one grep and it should be in the census beside
   the spans.
2. **The cluster's second block begins at a COMMENT BLOCK, not at a
   form**: `;; --- ADR-0174 section 2(a) (arc 3b sweep 1): the
   encounter, folded ---` plus six lines (2113-2120) introduces the four
   lifecycle folds and moves with them -- the second time in three
   extractions that a form-span census's structural blind spot for the
   gap between two forms has cost a cluster a form, which makes it a
   recipe item rather than an anecdote.
3. **That comment block says "These three are the whole of the
   encounter's fold" and there are FOUR forms under it**
   (`reopen-encounter` arrived with `:cancel-discharge`'s reinstatement
   after the block was written); wrong before this session, moved
   verbatim and named here rather than corrected inside a commit whose
   whole claim is that the moved text is unchanged -- the identical
   disposition the state session gave `observation-value-fields`'
   docstring.
4. **`encounters` reaches NOTHING but `streams`**, not even the
   `sim-model`/malli floor the two prior extractions had, so the
   census's §3a edge count of one for this cluster is exactly right and
   the namespace is a near-leaf.
5. **The census's sizes are now stale by TWO extractions, as designed**:
   `engine.clj` is 4,419 lines / 151 forms plus `ns` here against §1's
   4,884 / 157, and the partition closes at 181 both before and after
   this session's move -- a later session should re-derive rather than
   transcribe, as all three have.

## 7. Disclosures

* **A premise mismatch in this session's own prompt, fixed forward with
  disclosure rather than stopped on.** The prompt's "six of these are
  `defn-` ... the defs that are public get delegations" does not hold:
  seven are `defn-`, the other three are `def ^:private`, and no mover
  is public. Under `rulings.md#R-stop-only-on-two-defensible-readings`
  STOP-AND-REPORT binds where two readings are both defensible; here
  constraint 5 has exactly one reading for a private mover, and the
  mismatch is mechanical -- a privacy census, run and tabled in §1 --
  so this is the fix-forward-with-disclosure arm. The consequence is
  that the extraction owes zero delegating defs, which is a SMALLER
  change than the prompt anticipated, not a larger one.
* No fresh clone; the existing clone was at `3e0b65a`, clean, and equal
  to `origin/main`. Every span was re-derived.
* `make test` runs `poly test :all skip:integration`, so the
  integration tier did not execute here. That is the standing W-1
  disclosure, not new to this session.
* **No timing figure is offered.** The Windows side was not sampled
  before the suite ran, and `reference_measurement_host_contamination`'s
  rule is that an unsampled host cannot carry a timing claim. The run is
  recorded only as having completed, exit 0.
* Two pieces of text that did not move were edited, both because the
  move makes them false, and both dispositioned in §2 BEFORE the move
  rather than discovered during it: `engine.clj:675`'s "above" and
  `check.clj:90`'s "`engine.clj`'s `stamp-encounter`". The streams and
  state sessions each set this precedent.
* `.agents/state-derived.md` still records the `:docs` reading set at
  787 lines against a 785 budget, headroom -2. PRE-EXISTING, carried
  forward from the streams session through the state session, untouched
  by anything here, and named again rather than left to be rediscovered.
* `make docsgen` was run for the generated `INDEX.md`/`state-derived.md`
  rows; `make traces` produced no diff, as a docs-and-refactor session
  should not move a trace.

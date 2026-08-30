# Engine namespace extraction, 2 of N: the `state` cluster

Session record, 2026-08-30. HEAD at start `867e73a`, confirmed equal to
`origin/main` after a `git fetch`; working clone clean, no fresh clone
taken. Ceremony R30. Program:
`roadmap.md#engine-namespace-extraction-and-apply-unification` (P5),
author rulings C1(a) (`engine.clj` stays the facade, moved public vars
get delegating defs, no test file changes) and S1(a) (an equivalence
proof replaces red-before-green).

The cluster order is not a free choice: the census's DAG
(`.agents/plans/engine-extraction-census.md` §3a) puts `state` second,
after `streams`, and `state` must land before `evolve` or a later
session creates a namespace it has to un-create.

## 1. Step 1 -- the tip, and the spans re-derived

`867e73a`. Every span below was re-derived from THIS tree with a
form-span script, not transcribed from the census, and the census's own
numbers are at `517a96d` -- two commits and one extraction ago.
`engine.clj` is **4,697 lines / 152 top-level forms plus the `ns`**
here, against the census's 4,884 / 157: the streams extraction took
sixteen forms out and put eleven delegating defs back, so -5 exactly
(`stream-family-tag` stayed private in `streams` and one of the five
private movers therefore owed no def). `streams.clj` is 331 lines / 16
forms plus its `ns`, and 152 + 16 = 168 against the census's 157 + its
own 11 = 168. The partition still closes. Sizes are a re-derivation,
not a copy.

| form | census (`517a96d`) | here (`867e73a`) |
|---|---|---|
| `ns` | 1-101 | 1-91 |
| header comment (moves with the block) | -- | 93-101 |
| `def ConditionRecord` | 102-124 | 103-124 |
| `def ObservationRecord` | 125-149 | 126-149 |
| `def MedicationOrderRecord` | 150-163 | 151-163 |
| `def CarePlanRecord` | 164-180 | 165-180 |
| `def Demographics` | 181-234 | 182-234 |
| `defn demographics-from-persona` | 235-255 | 236-255 |
| `defn placeholder-demographics` | 256-270 | 257-270 |
| `def PatientLocation` | 271-280 | 272-280 |
| `def EncounterRecord` | 281-333 | 282-333 |
| `def AppointmentRecord` | 334-362 | 335-362 |
| `def PatientState` | 363-450 | 364-450 |
| `defn valid-patient?` | 451-456 | 452-456 |
| `defn initial-patient` | 457-466 | 458-466 |
| `defn- observation-value-fields` | 2360-2376 | 2173-2188 |

**First census correction.** The thirteen `state` forms are
CONTIGUOUS, 103-466, and the census's own §1 line-span list does not
say so; it also does not list the nine-line header comment (93-101,
"M6 Task 1: the clinical-content accumulator") that `PatientState`'s
own docstring cites as "this namespace's own header comment just above
`PatientState`". That comment is part of the cluster and moves with it,
or the docstring citation it anchors stops resolving. A form-span
census sees only forms; a comment BLOCK between two forms is invisible
to it, which is the same blind spot §5's seventh constraint names for
docstrings pinned from another file.

**Second census correction.** `observation-value-fields` is
`2173-2188` here (16 lines), and its call sites are `2194` (`decide
:observation`), `2784` (`evolve :observation`) and `2796` (`evolve
:diagnostic-report`) -- three, exactly as the prompt says. Its own
docstring says it is "shared by `decide :observation` and `decide
:diagnostic-report`", and that is WRONG in the live tree and was wrong
before this session: `decide :diagnostic-report` (`2205-2212`) does not
call it: it passes `:observations` through whole, and the per-child
flattening happens at fold time in `evolve :diagnostic-report`. Named
here, moved verbatim, not fixed -- correcting an unrelated pre-existing
docstring error inside a commit whose whole claim is "verbatim" would
make the diff harder to audit, not easier.

## 2. Step 2 -- the pre-move citation sweep (constraint 6)

The recipe §5 item 6 mandates, run BEFORE any form moved. Method: every
docstring and comment line of the fourteen moving forms (plus the
header comment block) was reduced to whitespace-normalised text, cut
into every distinct six-word window of 28+ characters -- **1,861
phrases** -- and each searched, whitespace-normalised, against every
`.md`/`.clj`/`.cljc`/`.edn`/`.txt`/`.yml`/`.json`/`.sh` file in the
tree (**1,715 files**, `engine.clj` itself excluded). A name-only grep
was run beside it, for the fourteen names and for the path form
`components/sim-engine/src/ehrt/sim_engine/engine.clj`.

### 2a. Path-pinned snippet citations -- the class that cost the streams session a red

**ZERO, and that is a checked result rather than an absence of
evidence.** The gated shape is `` `path` "snippet" ``
(`patient-simulator-charter-test`/`person-simulator-charter-test`'s own
`citation-pattern`, `#"`([^`\n]+)`\s+\"([^\"\n]+)\""`), resolved by
`slurp` against the named file and required to occur EXACTLY ONCE. Both
registers were read row by row:

| register | rows citing a `sim-engine` path | disposition |
|---|---|---|
| `components/person-simulator/docs/limitations.md` | rows 1 and 10, both `components/sim-engine/src/ehrt/sim_engine/streams.clj` | **safe** -- the streams session already repointed both off `engine.clj`; neither snippet ("pinned at 0 for as long as", "arc 2's demographic/life-arc layer. ZERO draw sites") is in the moving text |
| `components/patient-simulator/docs/limitations.md` | none -- its eight rows cite `gmf.clj`, `gmf_interpreter.clj`, `compile_trajectory.clj`, `persona.clj`, `emit_hl7.clj`, `check.clj` and `gmf-interpreter.md` only | **safe** |

### 2b. Var-by-namespace references to a moving form -- safe by delegating def

Five in docs, one in source prose. Every one names
`ehrt.sim-engine.engine/<var>`, which C1(a)'s delegating def keeps
resolving, so none is repointed.

| hit | names | disposition |
|---|---|---|
| `components/sim/docs/patient-state-model.md:157` | `ehrt.sim-engine.engine/PatientState` (malli) | safe |
| `components/sim/docs/patient-state-model.md:188` | `ehrt.sim-engine.engine/PatientState` carries every field | safe |
| `components/patient-simulator/docs/trajectory-computation.md:46` | never touch `ehrt.sim-engine.engine/PatientState` | safe |
| `components/patient-simulator/docs/trajectory-computation.md:330` | `ehrt.sim-engine.engine/PatientState`'s `:status` enum | safe |
| `components/patient-simulator/docs/gmf-interpreter-findings.md:457` | `ehrt.sim-engine.engine/PatientState`'s `:status` enum | safe |
| `components/sim-model/src/ehrt/sim_model/pathway.clj:87` | `ehrt.sim-engine.engine/ObservationRecord` already establishes | safe |

### 2c. Prose references by path, ungated

| hit | text | disposition |
|---|---|---|
| `components/sim-check/src/ehrt/sim_check/check.clj:1005` | "engine.clj's own `PatientState` status enum" | **safe** -- names a var this namespace keeps; no snippet is pinned |
| `notes/adr/0042-wave-h-pre-roll.md:109` | "`engine.clj`'s `ConditionRecord` docstring" | **safe** -- an ADR is a frozen historical record, and the var still resolves at `engine.clj` |
| `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj:383` | asserts the `engine.clj` path trips no retired-prefix rule | **safe** -- the path still exists; a sibling file was added last session with no effect here |

### 2d. Phrase hits that are shared prose, not citations

The 1,861-phrase sweep returned hits in 30-odd files -- `check.clj`
(27 phrases), `event_schema.clj` (47), `emit_hl7.clj` (16),
`notes/adr/0173-*.md` (24), `notes/adr/0174-*.md` (17), and a long
tail dominated by the boilerplate provenance stamps "GMF coverage Wave
D stage D1/D2" and "ADR-0174 section 2(a) (arc 3b sweep 1)". Every one
inspected is a SIBLING restating the same ADR language about the same
fact, not a citation INTO `engine.clj`: none pairs the phrase with a
path, and none is read by a gate. **Disposition: safe, as a class.**
This is the sweep's own finding about its own method -- the recipe is
tuned for recall, so its output is mostly shared vocabulary, and the
work is the dispositioning, not the grep.

### 2e. In-file references that go stale, and the one repoint owed

Nineteen lines of `engine.clj` outside the moving spans mention a
moving name. Seventeen are either a call site resolving through a
delegating def (`initial-patient` at `2938`/`4390`,
`placeholder-demographics` at `2493`, `demographics-from-persona` at
`2494`/`2520`) or prose naming a var by bare name, which stays correct.
Two need handling:

| hit | disposition |
|---|---|
| `engine.clj:2194`, `:2784`, `:2796` -- the three `observation-value-fields` call sites | **qualify** to `state/`; per constraint 5 the var becomes public in the new namespace and gets NO delegating def, so `engine.clj`'s public surface is unchanged |
| `engine.clj:2777` -- "(this namespace's own header comment above `PatientState`)" | **repoint**: after the move that comment is in `ehrt.sim-engine.state`, not this namespace. Corrected, and disclosed here, on the streams session's precedent -- a comment that stops being true when the file changes under it is the exact pattern review 5 exists to name |

No test file is touched, and `interface.clj` is not touched: it
re-exports none of the fourteen. All 99 test-side uses of a moving var
(`initial-patient` 83, `Demographics` 13, `demographics-from-persona`
3, `PatientState` 2, `ObservationRecord` 1) go through
`engine/<name>`, and no test reaches `observation-value-fields` at all
(checked, including the `#'engine/` var-quote form).

**Gate for step 2: this hit list is committed before the move.**

## 3. Step 3 -- the extraction (`efe2b26`)

`ehrt.sim-engine.state`. No collision: `components/sim-engine/src/ehrt/
sim_engine/` held `churn`, `engine`, `event_schema`, `interface`,
`order_profiles`, `person_fold` and `streams`, and no `state` of any
kind. Fourteen forms plus the header comment block.

**The verbatim claim is proven, not asserted.** `sed -n '93,466p'` of
`engine.clj` at `6fb698a` diffed against `state.clj` lines 37-410 is
**IDENTICAL, 374 lines**. The cycle breaker diffed against its own
former span differs on exactly one line -- `(defn- ` becomes `(defn ` --
which is constraint 5's required widening and nothing else.

**Thirteen delegating defs** in `engine.clj`, in the order the
originals stood in, each a short summary plus "Delegates to
`ehrt.sim-engine.state/<var>`, which carries the contract".
`interface.clj` re-exports none of the thirteen and was not opened.
No test file changed.

**`observation-value-fields` gets no delegating def**, per constraint
5, and is `state/`-qualified at its three sites. Verified live rather
than assumed: `(resolve 'ehrt.sim-engine.engine/observation-value-
fields)` returns `nil`, so the engine's public surface is exactly what
it was.

The whole `engine.clj` diff is FOUR hunks and nothing else: the
`:require` insertion, the moved block replaced by the banner and the
thirteen defs, the cycle breaker's deletion plus its one qualified site,
and the two remaining qualified sites with the repointed comment
between them. `engine.clj` is 4,697 -> 4,419 lines.

### Gates

* **Suite.** `make test` exit 0, zero failures and zero errors in every
  namespace: **408 namespaces, 4,751 tests, 24,113 assertions**.
  Namespaces and tests are IDENTICAL to the streams extraction's own
  recorded run (408 / 4,751); assertions are +2 against its 24,111, and
  the +2 is explained to the assertion rather than waved at.
  `ehrt.docs-tooling.io-vocabulary-lint-test` is a `doseq` over every
  source file with one `is` per file (`io_vocabulary_lint_test.clj:94`,
  the `is` at `:98`) plus a population guard at `:90`; it reported
  3 tests / 123 assertions in EACH of the two projects it runs in, and
  the tree gained one file. One new file is +1 twice. This is the same
  benign class the streams session recorded.
* **`bin/regression-oracle 6fb698a efe2b26`** -- the script's own
  output: `IDENTICAL: every root's digest matches between 6fb698a and
  efe2b26`, `declared-digest-change: no (soundness: yes outside the
  leading docstring)`. No declaration was owed and none was made.
* **`poly check`** OK. `clojure -M:dev` loads `ehrt.sim-engine.state`,
  `ehrt.sim-engine.engine` and `ehrt.sim-engine.interface`, and
  evaluates `initial-patient`, `valid-patient?`,
  `demographics-from-persona` and `PatientState` through the delegating
  defs -- the last by value identity against `state/PatientState`.
  (`poly check` does not compile, so the load is the real check; that
  is a standing finding of the arc-4 sweeps, not new here.)

## 4. Step 4 -- the ground-truth bracket

**`bin/ground-truth-bracket 6fb698a efe2b26`** -- `IDENTICAL: every
digested root's :ground-truth matches between 6fb698a and efe2b26 (38
roots)`, coverage `38 roots carry :ground-truth and are digested; 3
skipped (no such key): appendicitis.edn, ear-infections.edn,
sore-throat.edn`; `declared-digest-change: no`.

Both brackets IDENTICAL with no declaration is the strongest shape a
pure-refactor commit can report, and it is what ruling S1(a)'s
equivalence proof asks for in place of red-before-green.

**CI.** `gh run view 33302680710` -- workflow `test`, head
`efe2b26c`, completed, conclusion **success**: the extraction, green on
CI's own runner and not only on this host. `bin/post-push-verify`
reported checks 1 and 2 OK over the range `867e73af..efe2b26c` (remote
tip matches HEAD; every commit message pure ASCII) and DISCLOSED check
3 as reported-not-awaited per AR-CI-4 -- it was awaited here instead,
above.

## 5. Step 5 -- the DAG, and what the next session takes

**The census DAG has NO back-edge into the remaining `engine.clj` from
`state`. Confirmed, two ways.** Structurally: `state.clj`'s `ns` form
requires `ehrt.sim-model.interface`, `malli.core` and `malli.util`, and
no `sim-engine` namespace at all -- the same shape that made `streams`
provably a leaf. Mechanically: a whole-symbol scan of `state.clj`'s
body (line comments and string literals stripped) for every one of the
**79** distinct top-level names still defined in `engine.clj` that
`state` does not itself define returns **NONE**. (79 and not 151: the
file's 151 forms carry only 92 distinct names, because its 32 `decide`
methods share one name and its 27 `evolve` methods another, and 13 of
those 92 are the delegating defs `state` also defines.)

So `state` joins `streams` as a leaf, and the census's §3a order holds
unchanged for the rest: `config` and `assignment` are the remaining
leaves, then `encounters`, then `evolve`, then `fold`, then
`log-index`, then `decide`, with `run` last as the facade's residue.
The natural next cluster is `encounters` (10 forms; its only outgoing
edge is one call into `streams`, which is already extracted).

Application-path unification stays last, against §4 of the census, and
is also `roadmap.md#event-stream-mutation`'s injection point.

## 6. Census corrections, one sentence each

1. **The thirteen `state` forms are CONTIGUOUS** (`103-466` at this
   sha), which §1's span list does not say, and moving them is
   therefore one cut rather than thirteen.
2. **The cluster includes a COMMENT BLOCK, and §1 cannot see it**: the
   nine-line `M6 Task 1` header above `ConditionRecord` (`93-101`) is
   cited by position from inside `PatientState`'s own docstring, so it
   had to move with the forms -- a form-span census enumerates forms,
   and the gap between two forms is invisible to it, which is the same
   class of blind spot §5 item 7 already names for cross-file docstring
   pins.
3. **`observation-value-fields`' docstring is wrong about its own
   callers, and was wrong before this session**: it says "shared by
   `decide :observation` and `decide :diagnostic-report`", but `decide
   :diagnostic-report` passes `:observations` through whole and never
   calls it -- the live sharers are `decide :observation` and `evolve
   :observation`/`:diagnostic-report`, exactly the three the prompt
   named. Moved verbatim and named here rather than corrected inside a
   commit whose whole claim is that the moved text is unchanged.
4. **Constraint 6's recipe works, and its yield is mostly noise**: 1,861
   phrases over 1,715 files returned hits in about thirty files, and
   every one was a sibling restating the same ADR language rather than a
   citation, so the cost of the recipe is the dispositioning, not the
   grep. It found ZERO path-pinned citations this time -- which is a
   checked zero, corroborated by a second, independently written grep
   implementation, not an absence of looking.
5. **The census's sizes are stale by one extraction, as designed**:
   `engine.clj` is 4,697 lines / 152 forms plus `ns` here against §1's
   4,884 / 157, which is exactly the streams extraction's -16 +11, and
   152 + `streams`' own 16 = 168 = 157 + the 11 defs it left behind.
   The partition still closes; a later session should re-derive rather
   than transcribe, as this one did.

## 7. Disclosures

* No fresh clone; the existing clone was at `867e73a`, clean, and equal
  to `origin/main` after a `git fetch`. Every span was re-derived.
* `make test` runs `poly test :all skip:integration`, so the
  integration tier did not execute here. That is the standing W-1
  disclosure, not new to this session.
* The suite's 15m51s execution time (16m30s wall) is NOT offered as a
  measurement: the Windows side was not sampled before the run, and
  `reference_measurement_host_contamination`'s own rule is that an
  unsampled host cannot carry a timing claim. It is recorded only as
  evidence the run completed.
* One comment inside `engine.clj` was corrected rather than left --
  `:2777`'s "this namespace's own header comment above `PatientState`",
  which the move makes false. Disclosed on the streams session's
  precedent; it is the one edit in this commit to text that did not
  move.
* `.agents/state-derived.md` still records the `:docs` reading set at
  787 lines against a 785 budget, headroom -2. PRE-EXISTING, carried
  forward from the streams session's own disclosure, untouched by
  anything here, and named again rather than left to be rediscovered.

## 8. Close marker

`gh run view 33303199019` -- workflow `test`, head `24ee23a7`,
completed, conclusion **success**. That is this session's CLOSE MARKER:
CI green at the pushed tip, which under the de-scaffold ruling of
2026-08-25 is what marks a landing rather than a tag.

Three commits: `6fb698a` the pre-move sweep, `efe2b26` the extraction,
`24ee23a` the record and prompt archive. All pushed. No tag, no ADR --
this is a refactor under an existing roadmap row and an existing
program ruling, and the de-scaffold ruling gives process work neither.

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

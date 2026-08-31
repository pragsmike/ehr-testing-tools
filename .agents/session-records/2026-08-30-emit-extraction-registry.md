# Emit namespace extraction, 2 of 8: the `registry` cluster

Session record, 2026-08-30. HEAD at start `b3a79cf`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed the same day in ten sessions and
whose emit half opened with `hl7-time`. Author rulings C1(a)
(`emit_hl7.clj` stays the facade, moved PUBLIC vars get delegating defs,
private movers widen ONLY where a caller stays behind, no test file
changes) and S1(a) (an equivalence proof replaces red-before-green).

`bin/preflight` exit 0, **no findings** -- the third clean preflight of
the program, and for the same reason as the first two: this session's
own first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

**C6(a) IS NOT RULED, so the prompt's bracketed second task did not
fire.** A whole-tree search for `C6(a)` returns nothing, in
`.agents/rulings.md` or anywhere else. The P5 row's engine-phase
narrative was therefore NOT compacted, and this session paid its
one-line budget the hard way instead -- section 7.

## 1. Step 1 -- the derivations, and where the prompt is wrong

### Thirteen forms, 278 form-lines, five regions

Derived at `b3a79cf` with a char-level scanner for every top-level
form's true span, and a whole-symbol scan over each form's body with
string literals, character literals and line comments stripped. The
scanner reproduces the eleventh session's own count exactly -- **82
def-forms** in the file, which is what that session left behind (86
minus seven movers plus three delegating defs).

| form | span | lines | marker |
|---|---|---:|---|
| `message-type-registry` | 59-190 | 132 | `def` |
| `skeleton-message-types` | 192-208 | 17 | `def` |
| `add-on-message-types` | 210-226 | 17 | `def` |
| `emittable-message-types` | 228-237 | 10 | `def` |
| `siu-event-kinds` | 899-906 | 8 | `def` |
| `siu-renders?` | 908-930 | 23 | `defn` |
| `siu-filler-status` | 932-942 | 11 | `def ^:private` |
| `room-and-board-code` | 1535-1543 | 9 | `def` |
| `charge-closing-kinds` | 1545-1552 | 8 | `def ^:private` |
| `chatter-event-kinds` | 1920-1935 | 16 | `def` |
| `order-status-ladder` | 2227-2240 | 14 | `def` |
| `result-status-ladder` | 2242-2250 | 9 | `def` |
| `final-result-stage` | 2252-2255 | 4 | `def ^:private` |

**278 form-lines, which is census 2a's own figure to the line.** Ten
public, three private, and all three privates are `def ^:private` --
not one `defn-` in the cluster.

No banner comment travels. Three banners sit next to the regions --
the ARC 4 SWEEP 4 SIU header, the charges header and the ladder header
-- and every one of them heads a MIXED region whose other members
(`sch-segment`, `siu-message`, `charge-concept`, `ft1-segment`,
`plan-charges`, `ladder-stage`, `plan-ladders`) stay behind. All three
stay. Every inter-form gap inside the five regions is a single blank
line, so 286 source lines carry 278 form-lines plus 8 separators.

### THE PROMPT'S EDGE CLAIM IS WRONG, AND THE CENSUS IS RIGHT

The prompt says "census says registry -> hl7-time only; correct from
the tree". **The census says no such thing, and neither does the tree.**
Section 2a names `registry` as one of THREE LEAVES with zero outgoing
edges, and section 3b's table carries no `registry`-as-caller row at
all. The tree confirms the census: `registry` has **ZERO outgoing
edges**. Its only four cross-form edges are INTERNAL to the cluster --
`skeleton-message-types` and `siu-event-kinds` read
`message-type-registry`, `emittable-message-types` reads the two
vocabularies, and `siu-renders?` reads `siu-event-kinds`.

It is a stricter leaf than `hl7-time` was, and the strictness is
load-bearing rather than decorative: a whole-symbol scan over the moved
text finds **zero alias-qualified symbols and zero dotted symbols**, so
`registry.clj` carries **NO `:require` and NO `:import` at all**.
`hl7-time` needed one.

### `interface.clj` re-exports SEVEN of the ten public movers

Read from `interface.clj` rather than carried from the census, and it
is exactly section 2a's figure: `skeleton-message-types`,
`add-on-message-types`, `emittable-message-types`, `siu-event-kinds`,
`siu-renders?`, `room-and-board-code`, `chatter-event-kinds`. The
heaviest re-export cluster in the file, which is why 2a placed it
second -- C1(a)'s obligation exercised at scale early.

The other three public movers are owed defs by the TREE, not by
`interface.clj`. Every requiring file's own `ns` alias was read, not
guessed: **sixty-five call sites** reach a registry mover through the
IMPLEMENTATION namespace, `message-type-registry` alone accounting for
**thirty-four across six test files** (`emit_hl7_test.clj` 22,
`siu_test.clj` 6, `chatter_test.clj` 2, `event_conformance_test.clj` 2,
`ladders_test.clj` 1, `v2_replay_test.clj` 1).
`order-status-ladder`/`result-status-ladder` have no external site but
are read by `plan-ladders`, which stays behind.

### `final-result-stage` IS DEAD, and that is a finding

`(def ^:private final-result-stage ... :final)` has **NO READER
ANYWHERE IN THE TREE**. Established by a whole-repo `git grep` over
every file type rather than over `.clj` alone: the only two hits are
its own defining line and census section 2's inventory row. The result
ladder's terminal stage is reached through `ladder-stage`'s saturating
`nth` instead, so the constant is genuinely unused.

It is MOVED, not deleted (`rulings.md#R-move-not-improve`: an
extraction moves equipment), stays `^:private` in `registry` because
constraint 5 is a prohibition and nothing calls it, and is rowed as a
RETIREMENT CANDIDATE for the ruled repoint pass instead.

## 2. Step 2 -- the sweep, which is NOT empty, and the commit is RED (`3d918ce`)

The eleventh session's sweep was empty at all four levels and took no
commit. **This one is not, and the prompt predicted exactly where.**

### 2a. Level 3 first, because it is where the hit is

Both charter registers were read BY HAND, row by row, as census
constraint 6 demands -- **36 citations, every one resolved against the
tree**. `patient-simulator/docs/limitations.md` carries 12, of which
**exactly one names a `sim-emit-hl7` file**: the care-plan row's

```
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj`
   "no real CarePlan-equivalent segment"
```

That phrase lives at `emit_hl7.clj:168` -- resolved to its owning form
by span rather than by eye, and the owner is **`message-type-registry`,
a mover**. `person-simulator/docs/limitations.md`'s 24 citations name
no `sim-emit-hl7` file at all; its two `streams.clj` rows were
repointed by the FIRST extraction and still resolve.

`ehrt.docs-tooling.patient-simulator-charter-test` parses those
`` `path` "snippet" `` pairs and asserts each snippet occurs in the
named file EXACTLY ONCE. The move breaks it, so the citation was
repointed to `registry.clj` in a commit of its own, **which is RED on
purpose**:

```
FAIL in (every-charter-citation-resolves-test)
1 charter citation(s) do not resolve:
  ["...registry.clj :: \"no real CarePlan-equivalent segment\"
    -- file does not exist"]
Ran 8 tests containing 19 assertions. 1 failures, 0 errors.
```

Run alone on a minimal classpath (the gate requires only
`clojure.test`/`io`/`string`, so it needs no project alias). The
successor commit clears it: **19 of 19, 0 failures**. That red is the
proof the gate is live and the repoint is load-bearing -- not an
assertion that it would have been.

**THE PHRASE IS FOUR WORDS.** The eleventh session established the
shingle sweep's six-word floor and named this exact row as the class a
shingle scan provably cannot see. It was right, and the marker half of
the same gate would not have caught it either: `markers` globs only
`components/patient-simulator/src`, never the emitter tree.

### 2b. Level 1 -- 1,457 shingles over 1,529 files, and why every hit is noise

The thirteen movers' docstrings and `;;` comments were cut into 1,457
six-word shingles and searched across the whole tracked tree.
Fifty-eight files carry a hit, and **not one is a positional claim
about a mover.** Two classes, both read rather than counted:

* **Shared provenance boilerplate** -- "ARC 4 SWEEP 2 (ADR-0175 design
  (h),", "GMF coverage Wave D stage D1 (2026-08-02,", "ARC 3B SWEEP 2
  (ADR-0174 ruling", "(arc 4 sweep 1, ADR-0175 ruling A1, commit". These
  are citation strings this repo repeats verbatim by design; they say
  nothing about where a form lives.
* **Deliberate doctrine echoes** -- `emittable-message-types`' allow-list
  law restated in `fan_out.clj`, `sim/run.clj` and three demo configs;
  `result-status-ladder`'s `:preliminary` gloss restated in
  `site_profile.clj`. Each is the same rule stated where it is
  enforced, which is the point of stating it twice.

The instrument's floor was confirmed a second time from the other
direction: the sweep's 1,457 shingles include `message-type-registry`'s
own 531, and **not one of them reaches the four-word charter phrase**.

### 2c. Level 2 -- paths and namespaces, classified

**PATH claims** naming `emit_hl7.clj`, live tree: **seven**, every one
read. Besides the charter row, three name the path while meaning a form
in ANOTHER cluster, and each is dispositioned to the session that will
own it rather than repointed now:

| site | the form it means | cluster |
|---|---|---|
| `emit_hl7_test.clj:1306` | `pv1-segment` (`:657`) | 5, `segments` |
| `sim-engine/assignment.clj:19` | `plan-latency` (`:1801`) | 7, `planners` |
| `person-simulator/limitations_test.clj:152` | `demographics-timeline` | 3, `timelines` |

`emit_hl7_test.clj:1306` deserves its own line, because it reads like a
hit and is not: it says "`emit_hl7.clj`'s own **registry** comment's
definition of a failure mode: traffic invisible to every consumer". The
phrase it means is at `:657`, **inside `pv1-segment`** -- "registry" is
being used doctrinally, not positionally. Resolved by span, not by the
word. Two of the three sit in TEST FILES, which C1(a) fences; neither
goes stale this session, so neither is backlogged yet.

The other three path claims are the eleventh session's own moved-to
banner in `hl7_time.clj`.

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<var>`
naming a registry mover: **eleven**, and every one names a PUBLIC mover
that keeps a delegating def, so **every one stays true** --
`message-type-registry` 2 (`v2_replay.clj:579`,
`sim/docs/third-party-sources.md:15`), `room-and-board-code` 5
(`sim-model/config.clj:271` plus three demo configs plus
`interface.clj`), and six more in `interface.clj`'s own docstrings.

**ZERO name a private mover.** The rarest class -- the one the
eleventh session hit and had to fence
(`v2_replay_test.clj:194`'s claim about `reference-instant`) -- does
not arise here, checked per-mover rather than in aggregate:
`siu-filler-status` 0, `charge-closing-kinds` 0, `final-result-stage` 0.

### 2d. Level 3, the rest -- and the tripwire that again does not fire

* **`hand-owned-assets.edn`.** All five rows read, and all four distinct
  sources read in full: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/pipeline.edn`,
  `demos/scenarios/ed-tuesday/README.md`,
  `components/corpus/docs/palgebra-design.md`. **Not one names a
  registry mover or an `emit_hl7.clj` path.** Nothing was edited, so no
  `:reviewed-at` is bumped and the tripwire does not fire -- the third
  session in the program to read them and fire nothing, after the sixth
  and the eleventh, and for the eleventh's structural reason.
* **`exercised-sources.edn`** pins no emitter source. Untouched.
* **No surface enumerates this component's namespaces.**
  `simulator-architecture.md:38` names `{interface,v2_replay}` and is a
  DATED record; `:57`'s bricks table names `interface.clj` only. A ninth
  namespace needs no registry row.
* **No build surface names the file.** `Makefile`, `.gitattributes`,
  `workspace.edn` and `deps.edn` carry no `emit_hl7.clj` path, so
  `registry.clj` needs no registration anywhere.

### 2e. Claims INSIDE `emit_hl7.clj`

Both directions were scanned, not just the moved half.

**In the MOVED text, three claims go false** and are paid in the MOVE
commit rather than the sweep, on the ninth and tenth extraction's rule
that restating them a commit early makes them false in the interim.
Every stay-behind form named in the moved prose was enumerated
mechanically -- six of them -- and only one carries a positional word:

* `add-on-message-types`: "`chatter-trigger`'s own rule **below**
  picks between A08 and A31". `chatter-trigger` stays behind. The word
  "below" is dropped; nothing else moves.
* `siu-renders?`: "every other emission profile in **this namespace**".
* `room-and-board-code`: "no default price anywhere in **this
  namespace**". Both become "the emitter".

The other five -- `single-subject-message`, `bed-status-message`,
`control-id-for`, `event->messages`, `demographics-timeline` -- are bare
names carrying no positional claim, and stay true across the seam, the
same standing `obr-segment`'s bare `hl7-timestamp` had in the eleventh.
So does `add-on-message-types`' "complement of `skeleton-message-types`
**above**": both travelled, in that order.

**In the RESIDUE, nothing goes false.** Every mover mention outside the
five regions was listed with its owning line and read. The four comment
mentions of `message-type-registry` and the one of `room-and-board-code`
are bare names a delegating def forwards. `event->messages`' own "The
gate lives here rather than in the registry" is not merely still true
-- it is MORE true, the registry now being a namespace rather than a
map three screens up.

## 3. Step 3 -- `ehrt.sim-emit-hl7.registry` (`f6c6270`)

### The name

No collision: nothing in the tree names `sim-emit-hl7.registry` or
`sim_emit_hl7/registry`, checked before writing the file. The alias
`registry` was unused in `emit_hl7.clj`.

### The moved body diffs as SIX lines, and no others

Verified by diffing the moved text as a BLOCK against `3d918ce`'s own
`emit_hl7.clj` -- the five regions plus their separators, **290 lines
either side** -- not inferred from hunk headers. `diff` reports exactly
six changed lines: the two `^:private` drops, the "below", and the
three lines of the two "this namespace" sentences. **Not one other
docstring or comment line differs**, across 290 lines of the
prose-heaviest cluster in the file.

### The two widenings, and the one form that gets nothing

C1(a)'s "widen only where a caller stays behind" has two literal cases
here and one deliberate non-case:

| private mover | callers staying behind | disposition |
|---|---|---|
| `siu-filler-status` | `sch-segment` (1 site) | public in `registry`, no def |
| `charge-closing-kinds` | `event->messages`, `plan-charges` (2 sites) | public in `registry`, no def |
| `final-result-stage` | none, anywhere | stays `^:private`, no def |

The three call sites are qualified `registry/...`. Neither widened var
gains a delegating def: widening `emit_hl7.clj`'s own public surface is
not what C1(a) asks for, and `poly check` would not catch it.

### Asserted live under `-M:dev`, not argued

* All ten delegating defs hold the **IDENTICAL object** as their
  `registry` counterparts. Ten of ten.
* `interface.clj` still resolves through `emit_hl7.clj`: six of the
  seven re-exports `identical?` to the facade's vars, and
  `siu-renders?` -- a `defn` wrapper rather than a `def` -- agrees
  behaviourally.
* **Constraint 5's prohibition**: `emit-hl7/siu-filler-status`,
  `/charge-closing-kinds` and `/final-result-stage` all fail to
  resolve. Three of three.
* `registry`'s entire public surface is the twelve expected -- the ten
  public movers plus the two widenings -- and `final-result-stage`
  alone is private.
* **`emit_hl7.clj`'s public surface is 24 vars before and 24 after,
  zero gained and zero lost**, both sides derived by the same scanner.
* `registry` holds **zero namespace aliases**, confirming the
  no-`:require` derivation from the loaded namespace rather than from
  the source text alone.

`clojure -M:poly check` OK.

### The require set, re-derived in BOTH directions

`registry.clj` needs nothing, derived from the moved text's own
qualified symbols (zero) and dotted symbols (zero) rather than carried
forward from `emit_hl7.clj`'s `ns`.

`emit_hl7.clj` gained ONE require and lost none, checked rather than
assumed: all six aliases are still called in the residue -- `parser`
170, `site-profile` 17, `hl7-time` 17, `registry` 14, `str` 4,
`sim-model` 4. **This move leaves no dead require behind**, which is
now two for two in the emit phase against the engine phase's nine.

## 4. Step 4 -- the gates

`bin/regression-oracle b3a79cf f6c6270`: **IDENTICAL: every root's
digest matches**, 41 roots, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** (the eleventh session's finding,
now applied rather than rediscovered) and it covers the rendered
messages themselves.

`bin/ground-truth-bracket b3a79cf f6c6270`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0. **Near-vacuous here and it says
so in its own output** -- "THIS IS NOT A REGRESSION-ORACLE CLAIM: the
`:hl7` half of every root is excluded by construction". For a cluster
inside the emitter it proves the ground-truth log did not move, which
an emitter-only change could not have moved anyway. Run and reported
because the prompt asks for it, not because it discriminates.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj`, the tenth extraction's
hazard class.

Both gates span `b3a79cf` -> `f6c6270`, the WHOLE session's code change,
rather than the sweep commit -> move commit pair -- the sweep commit is
a one-line doc edit that cannot move a digest, and the wider range is
the stricter claim.

### The suite delta, measured IN-CLONE

`make test` unpiped through a wrapper ending in `exit "$MAKE_EXIT"`,
run THREE times **in the same clone** -- at `f6c6270`, at the session's
start tip `b3a79cf` by `git checkout` rather than a worktree, and again
at the close-out tree once the docs landed. All three `MAKE_EXIT=0`, all
three **408 zero-failure blocks / 4,751 tests**, and 4,751
is the count the eleventh extraction closed on: **this move adds no
`deftest`**, confirmed per namespace rather than from the total.

Assertions go **24,133 -> 24,137, +4**, attributed per namespace by
diffing per-namespace counts out of the two logs. **Exactly two
namespaces move, and both were predicted before the move:**

| namespace | delta | why |
|---|---:|---|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | 264 -> 266 | `doseq` over every production `.clj` under `components/*/src` and `bases/*/src`, one `is` per file |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | 24 -> 26 | `doseq` over every `.clj` under `components/sim-emit-hl7/src`, one `is` per file |

Each moves by ONE file and is counted TWICE, because `conformance` and
`ehrt-cli` both run `docs-tooling`'s suite. `registry.clj` is the one
new production source file and both gates count files; it passes the
second on its merits, requiring nothing at all.

**+4 is the WHOLE delta, and that is the eleventh session's hazard
paid rather than repeated.** That session measured its baseline in a
`git worktree`, where `.git` is a FILE, so `ehrt.sim.version/git-sha`
returned `nil`, `version-test`'s last `is` did not run, and its
reported +6 carried +2 of pure instrument. This session's baseline ran
IN THE CLONE, as the prompt required, and `ehrt.sim.version-test` does
not appear in the per-namespace diff at all -- the contamination is
absent rather than argued away. Code-attributable delta and measured
delta are the same number for the first time in the emit phase.

**The close-out run is assertion-for-assertion identical to the move
run** -- 24,137 either way, per-namespace diff empty -- so this
session's doc additions (a record, a prompt archive, the P5 rewrite, the
two regenerated indexes and `state-derived.md`) move no gate's
population.

## 5. Closing arithmetic

### The partition, cluster 2 of 8

`emit_hl7.clj` goes 2,473 lines to 2,220, and 82 def-forms to 79
-- it lost thirteen and gained ten. `registry.clj` is 324 lines / 13
forms plus its `ns`, and is the first namespace in either file to need
no `:require` at all.

### Census corrections

* Section 2's `registry` row says "13 forms, 291 lines"; the forms are
  13 and the FORM-lines are **278**, which is section 2a's own figure,
  confirmed to the line.
* Section 2a's "three leaves" is confirmed for the SECOND of them:
  `registry` has zero outgoing edges at `b3a79cf`, and its only four
  cross-form edges are internal to the cluster.
* Section 3b's having no `registry`-as-caller row is confirmed exactly.
* Section 2a's "seven `interface.clj` re-exports" for this cluster is
  confirmed exactly, read from `interface.clj`.
* **New, and not in the census at all**: `final-result-stage` is dead
  code. Section 2 inventories it without noticing it has no reader.

### The backlogs

* **RETIREMENT CANDIDATES** gains `ehrt.sim-emit-hl7.registry/
  final-result-stage` -- not a caller-less DELEGATING DEF like the
  engine phase's fourteen, but a caller-less MOVED FORM, a class the
  program has not seen before.
* **FENCED CITATIONS**: none added. Every namespace claim about a
  registry mover names a public one and keeps resolving. The two
  test-file path claims read during the sweep
  (`emit_hl7_test.clj:1306`, `person-simulator/limitations_test.clj:152`)
  name forms in clusters 5 and 3 and are not stale yet; they are
  dispositioned in section 2c so those sessions do not have to
  rediscover them.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt's edge claim is wrong and the census is right.**
   "Census says registry -> hl7-time only" -- the census says
   `registry` is a LEAF, section 3b has no `registry` caller row, and
   the tree gives zero outgoing edges. Corrected from the tree, as the
   prompt itself instructs.
2. **C6(a) is not ruled**, so the bracketed P5 compaction did not fire.
   Searched for, not assumed. See section 7.
3. **A sweep commit DOES exist this time**, unlike the eleventh's, and
   it is deliberately RED. The prompt's "predicted reds RED-FIRST with
   successor" is followed literally.
4. **Three lines of the moved text are not verbatim** beyond the two
   marker widenings. All three are named before the move in section 2e,
   all three are the relocation's own debris rather than fixes found in
   passing, and `rulings.md#R-move-not-improve` is not strained.

## 6. What is left in this program

Six emit clusters (`timelines`, `er7`, `segments`, `messages`,
`planners`, `facade`), then the apply-path unification. The next
session takes `timelines` -- five forms, **all five private**, so C1(a)
owes no def and constraint 5 forbids one, and every mover widens
because all four of its consumers stay behind. Its constraint-6 site is
already located: `person-simulator/limitations_test.clj:152` names
`emit_hl7.clj`'s `demographics-timeline` by path, in a TEST FILE, so it
is fenced rather than repointable -- the first of that class in the
emit phase.

## 7. ESCALATION: the P5 row is now AT its budget, with six clusters to go

`.agents/plans/roadmap.md` is an `:onboarding` member. The set was at
**1529 of 1530** when this session opened -- the one line the eleventh
left -- and `.agents/reading-sets-baseline.edn`'s ratchet caps
`:budget-lines` at 1530, editable only by a compaction ADR.

This session's P5 rewrite was made to fit in **+1** by compacting only
the EMIT-phase text this phase's own sessions wrote: the emit-phase
opener and the eleventh's landing sentence were re-flowed to carry BOTH
landings in the same six lines; the tripwire recipe absorbed the twelfth
and lost a line; and the twelfth's own record path was folded onto the
eleventh's line rather than taking one of its own. The set now stands at
**1530 of 1530: NO headroom at all.**

**The next emit session cannot add a line to this row. It must compact
first, or stop** (`rulings.md#R-budget-stop`: compact or stop, never
bump). What remains to compact is what the eleventh already named --
the ENGINE phase's closed narrative in the same row, roughly forty lines
of per-cluster detail already recorded in nine session records, which
`rulings.md#R-roadmap-row-destinations` says may move verbatim into the
row's owning ADR.

**That move is re-triage and therefore the author's**
(`R-section-retriage-is-author-judgement`). The eleventh named it and
did not make it. This session searched for the ruling that would
authorize it, found none, and did not make it either. **It is now
blocking**: the eleventh could leave one line for a successor, and this
session cannot. AUTHOR ACTION: rule C6(a), or the thirteenth session's
first act is a stop.

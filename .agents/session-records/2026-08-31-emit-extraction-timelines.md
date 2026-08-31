# Emit namespace extraction, 3 of 8: the `timelines` cluster, and the C6(a) compaction

Session record, 2026-08-31. HEAD at start `e3ce663`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed 2026-08-30 in ten sessions and
whose emit half has now landed three of eight clusters. Author rulings
C1(a) (`emit_hl7.clj` stays the facade, moved PUBLIC vars get delegating
defs, private movers widen ONLY where a caller stays behind, no test
file changes), S1(a) (an equivalence proof replaces red-before-green)
and -- NEW THIS SESSION -- C6(a).

`bin/preflight` exit 0, **no findings** -- the fourth clean preflight of
the program, and for the same reason as the first three: this session's
own first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 0. C6(a) FIRED, and it was the precondition for everything else (`e940f24`)

The twelfth session closed with the `:onboarding` reading set at
**1530 of 1530 -- zero headroom** -- and escalated it as BLOCKING:
`rulings.md#R-budget-stop` says compact or stop, never bump, so the
thirteenth session's first act had to be a compaction or a stop. It
ruled AUTHOR ACTION: rule C6(a), or stop.

**C6(a) is ruled**, by the author in this session's prompt: compact the
P5 row's ENGINE-phase closed narrative to a summary of at most five
lines whose pointer is the nine engine session records. It is a
PROMPT-level session ruling, not a `rulings.md` row -- that register has
been FROZEN since 2026-08-25 and does not grow, which is why the twelfth
session's whole-tree search for `C6(a)` correctly returned nothing and
why this session added no row either.

Executed as its own docs commit, before any other edit:

* The twenty-four-line per-cluster landing account -- `streams` through
  `run`, each cluster's form/def counts and its own findings -- became
  **five lines** naming the ten clusters, the both-gates-IDENTICAL
  claim, the pure-facade outcome under C4(b) and the partition's closing
  figures, pointing at the nine engine records the row already lists.
* The `**EMIT PHASE OPENED` fragment the deletion orphaned was reflowed.

**DELIBERATELY NOT COMPACTED, and the restraint is the ruling's own
scope.** C6(a) names the CLOSED NARRATIVE. It does not name the row's
standing doctrine, which is what the six remaining emit clusters still
have to follow: the hand-owned-asset tripwire recipe, the
bracket-source hazard, the instruments-swap-at-the-emission-layer
finding, the census-corrected-by-session rows, constraint 5 as a
prohibition, the facade/implementation require direction, both
repoint-pass backlogs, and the coverage disclosure. Re-triaging those is
`rulings.md#R-section-retriage-is-author-judgement` and was not
authorized. Some of them do carry engine-phase INSTANCE detail that a
wider reading of C6(a) would have taken; the narrower reading was
chosen and is disclosed here rather than absorbed.

**Headroom, from the generated table rather than by hand: 1530 of 1530
(0) before, 1510 of 1530 (20) after.** `.agents/state-derived.md` was
regenerated in the same commit because `roadmap.md` is a line-counted
`:onboarding` member and the currency gate would otherwise have gone
red. Gate: `ehrt.docs-tooling.roadmap-lint-test`, 20 tests / 32
assertions, 0 failures 0 errors.

**DISCLOSED, pre-existing and untouched**: the `:docs` reading set reads
787 against a 785 budget (**-2**) at the session's start tip and still
does. It is not this session's, and the ratchet caps the BUDGET rather
than the usage, so nothing is red.

## 1. Step 1 -- the derivations, and the channel's third edge error

### Five forms, 151 form-lines, two regions

Derived at `e940f24` with a char-level scanner for every top-level
form's true span, and a whole-symbol scan over each form's body with
string literals, character literals and line comments stripped. The
scanner reproduces the twelfth session's own count exactly -- **79
def-forms** in the file, which is what that session left behind (82
minus thirteen movers plus ten delegating defs).

| form | span | lines | marker |
|---|---|---:|---|
| `demographics-timeline` | 361-440 | 80 | `defn-` |
| `demographics-at` | 442-456 | 15 | `defn-` |
| `encounter-spans` | 1655-1682 | 28 | `defn-` |
| `mrn-timeline` | 1684-1702 | 19 | `defn-` |
| `mrn-at` | 1704-1712 | 9 | `defn-` |

**151 form-lines, which is census 2a's own figure to the line.** All
five private, and all five `defn-` -- not one `def ^:private`, the exact
mirror of `registry`, which was all `def ^:private` and not one `defn-`.

No banner travels, and there is no banner to travel: the three
inter-form gaps (`:441`, `:1683`, `:1703`) are all BLANK LINES, checked
with `cat -A` rather than by eye, so 154 source lines carry 151
form-lines plus 3 separators. The ARC 4 banner at `:1644-1646` heads
`restatement-day-seconds`, a planner that stays.

### THE PROMPT'S EDGE EXPECTATION IS WRONG FOR THE THIRD TIME, AND ZERO IS THE ANSWER

The prompt says "expect `hl7-time` possible, nothing else -- correct
from the tree". **The tree says NOTHING, not even `hl7-time`.**
`timelines` has **ZERO outgoing edges**, which is census 2a's third leaf
confirmed. Established in both directions, because a bare-name scan
alone would have been blind to the two clusters that have already left
the file:

* Bare names: no timelines mover references any def-form in
  `emit_hl7.clj`.
* Qualified and dotted symbols: a whole-symbol scan of the moved text
  with strings, character literals and line comments stripped finds
  **ZERO alias-qualified symbols and ZERO dotted symbols**. So no
  `hl7-time/...`, no `registry/...`, no Java class, and
  `timelines.clj` carries **NO `:require` and NO `:import` at all**.

That is the SECOND no-require cluster after `registry`, and it is
stricter than `registry` in one respect the design channel has not had
before: `registry` had four cross-form edges INSIDE the cluster, and
**`timelines` has NONE**. The five are five independent folds and
lookups over one argument. `demographics-at` does not call
`demographics-timeline`; it receives its output.

### Incoming: 19 call sites, 16 caller forms -- and census 3b is right on its own accounting

| mover | callers staying behind | sites |
|---|---|---:|
| `demographics-at` | `context-for-event`; `single-subject-message`, `bed-swap-message` (x2), `siu-message`, `merge-message`, `orm-message`, `oru-message`, `observation-message`, `diagnostic-report-message`, `dft-message`, `chatter-message` | 12 |
| `demographics-timeline` | `emit`, `emit-wire` | 2 |
| `encounter-spans` | `plan-chatter`, `plan-charges`, `emit-wire` | 3 |
| `mrn-timeline` | `plan-chatter` | 1 |
| `mrn-at` | `periodic-chatter` | 1 |

Census 3b reads `messages` 10, `planners` 4, `facade` 3, `er7` 1 =
**18**. Both numbers are right and the difference is one form:
**section 3b counts DISTINCT (caller-form, callee-form) PAIRS, and
`bed-swap-message` calls `demographics-at` twice.** On its own
accounting 3b reproduces EXACTLY, all four rows. The distinction is
recorded because it is the first time in the program the two
accountings have diverged, and a later cluster reading "10" as a site
count would be off by one.

### FIVE WIDENINGS, ZERO DELEGATING DEFS -- and the weighted-pick shape cannot arise

C1(a) owes no def: all five movers are private. Constraint 5 fires for
all five: **every caller of every mover stayed behind.** The prompt
asked which movers might have all callers travelling -- the
`weighted-pick` shape. **None can, and the reason is structural rather
than lucky: the cluster has no internal edges at all**, so there is no
mover whose only caller is another mover. Where `registry` had two
widenings and one form that got nothing (`final-result-stage`, dead),
`timelines` has five of five.

### `interface.clj` owes nothing, read rather than carried

Census 2a says `timelines` owes none of the sixteen re-exports. Read
from `interface.clj`: **zero**. A whole-tree `git grep` per mover
confirms it from the other side -- `interface.clj` names none of the
five anywhere.

### ZERO test call sites, which is a first for the program

A whole-repo `git grep` over every file type, per mover, finds **no test
anywhere reaches any of the five**, by `#'`, by `@#'`, or at all.
`emit_hl7_test.clj:1165`'s `demographics-at-answers-state-at-t-test`
reads like one and is not: it drives `emit-hl7/emit` and asserts on the
rendered PID-11. The name is the only reference. So unlike `registry`'s
sixty-five implementation-namespace call sites, this cluster owes no def
to a test either.

### No collision

Nothing in the tree names `sim-emit-hl7.timelines` or
`sim_emit_hl7/timelines`, checked before writing the file. The alias
`timelines` was unused in `emit_hl7.clj`.

## 2. Step 2 -- the sweep, which owes ZERO repoints, and the two findings it turned up anyway

**No sweep commit precedes the move**, and the absence is derived rather
than assumed -- the eleventh's precedent, not the twelfth's. Every level
was run and every level is reported.

### 2a. Both charter registers, hand-read row by row

Census constraint 6's own level, and the one that cost the twelfth
session a deliberate red. **36 citations, every one resolved
mechanically against the tree, all 36 present exactly once.**

`components/patient-simulator/docs/limitations.md` carries 12, of which
exactly one names a `sim-emit-hl7` file -- and it is the twelfth
session's own repoint, `registry.clj` :: "no real CarePlan-equivalent
segment", which still resolves. `components/person-simulator/docs/
limitations.md`'s 24 name no `sim-emit-hl7` file at all; its two
`streams.clj` rows, repointed by the FIRST extraction, still resolve.

**ZERO charter citations name a timelines mover or `emit_hl7.clj`.** The
level is empty, which the twelfth session's hit makes worth stating as a
result rather than a silence.

### 2b. Level 1 -- 674 shingles over 1,534 files, twelve hits, zero positional

The five movers' docstrings and `;;` comments cut into 674 six-word
shingles and searched across the whole tracked tree. Twelve files carry
a hit and **not one is a positional claim about a mover**; every one was
read, not counted. Three classes:

* **The struck-row quotation** -- "a delta folded onto patient state is
  invisible to every message", ADR-0172 limitations row 6's own
  substance, quoted in three session records, two ADRs and
  `emit_hl7_test.clj:1124`'s comment. A claim about BEHAVIOUR, and one
  the fold made false on purpose; it says nothing about where the fold
  lives.
* **Doctrine echoes** -- "every run that did not opt into `:encounters`"
  restated in `check.clj`, `emit_fhir.clj`, `event_schema.clj`,
  `state.clj` and `patient-state-model.md`; sim/ADR-0012's "a stage's
  own state is recoverable by scanning the log" quoted in ADR-0173.
  Each is the same rule stated where it is enforced.
* **Coincidence** -- `citation_gate.clj:54`'s "boolean flag for all of
  them -- an earlier version collapsed".

The instrument's known floor was not reached this time: the twelfth
session's four-word charter phrase is the class a six-word shingle
cannot see, and this cluster has no charter phrase at all, so the
hand-read above is what carries the level rather than the shingles.

### 2c. Level 2 -- paths and namespaces

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<mover>`
naming any of the five: **ZERO**, checked per mover rather than in
aggregate. The rarest and most dangerous class -- a namespace claim
about a PRIVATE mover, which the eleventh session hit and had to fence
-- does not arise.

**PATH claims** naming `emit_hl7.clj` in a LIVE surface: four, plus the
two prior moved-to banners.

| site | disposition |
|---|---|
| `person-simulator/limitations_test.clj:152` | **STALE WITH THIS MOVE. C1(a)-FENCED.** See 2f |
| `emit_hl7_test.clj:1306` | cluster 5, `segments` -- the twelfth's disposition, unchanged |
| `sim-engine/assignment.clj:19` | cluster 7, `planners` -- the twelfth's disposition, unchanged |
| `emit_hl7.clj:514` | **ALREADY FALSE at HEAD.** See 2e, finding (A) |

The two prior banners were read in full. `hl7_time.clj`'s names no
timelines mover. `registry.clj:152-158` names `demographics-timeline`
-- "`demographics-timeline` folds both, so every message the patient
receives after one renders the new PID" -- as a BARE NAME carrying no
positional word, in a claim about behaviour. It stays true across the
seam and was not touched.

The ADRs that name `emit_hl7.clj` by path and line (0172 at `:77-82`,
0173, 0174, 0142, 0150, 0171) are DATED RECORDS, and several of their
line pins were already stale before this program opened. Not repointed,
on the standing treatment of `simulator-architecture.md:38`.

### 2d. Level 3, the rest -- and the tripwire that again does not fire

* **`hand-owned-assets.edn`.** All five rows read, and all four distinct
  sources read: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/pipeline.edn`,
  `demos/scenarios/ed-tuesday/README.md`,
  `components/corpus/docs/palgebra-design.md`. **Not one names a
  timelines mover or an `emit_hl7.clj` path.** `simulator-architecture.
  md` names emitter files only as `{interface,v2_replay}` and
  `interface.clj`. Nothing was edited, so no `:reviewed-at` is bumped
  and the tripwire does not fire -- the FOURTH session in the program to
  read them and fire nothing, after the sixth, eleventh and twelfth, and
  for the eleventh's structural reason: that page names ENGINE forms by
  DEFINING FORM but EMITTER forms as BARE NAMES.
* **No surface enumerates this component's namespaces.** A tenth
  namespace needs no row.
* **No build surface names the file.** `Makefile`, `.gitattributes`,
  `workspace.edn` and `deps.edn` carry no `emit_hl7.clj` path, so
  `timelines.clj` needs no registration anywhere.

### 2e. TWO PRE-EXISTING FALSE CLAIMS FOUND, NEITHER FIXED

Both are disclosed rather than absorbed, both are backlogged, and
neither is caused by this move. `rulings.md#R-move-not-improve` is the
reason they are not fixed here: an extraction moves equipment.

**(A) `emit_hl7.clj:514` -- FALSE since the TWELFTH session's own move,
and it corrects that session's record.** `pv1-segment`'s docstring says
"`emit_hl7.clj`'s own registry comment calls traffic invisible to every
consumer a failure mode". The phrase it cites -- "produces traffic
invisible to every consumer" -- **now lives at `registry.clj:41`**, and
ADR-0174:313 pins it at the old `emit_hl7.clj:51`. It left
`emit_hl7.clj` with `message-type-registry`.

The twelfth session read the TEST-side twin of this citation
(`emit_hl7_test.clj:1306`) and resolved that the phrase it means "is at
`:657`, inside `pv1-segment` -- 'registry' is being used doctrinally,
not positionally". **That resolution is wrong**: `:657` is where the
phrase is QUOTED, and `registry.clj:41` is where it is SAID. So the
disposition of `emit_hl7_test.clj:1306` to cluster 5 is also wrong --
the claim is already stale now, and no later cluster will make it so.
Both sites are rowed below; the test-side one is C1(a)-fenced in any
case.

**(B) `encounter-spans`' own docstring -- FALSE since the THIRD ENGINE
session, and it travelled into `timelines.clj` verbatim.** It cites
`ehrt.sim-engine.engine/stamp-encounter`. **That var does not resolve**:
`engine.clj` mentions `stamp-encounter` in two comments and defines
nothing, and the form is `ehrt.sim-engine.encounters/stamp-encounter`
(`encounters.clj:138`, public -- one of that cluster's widenings). The
engine phase's `encounters` extraction made it stale on 2026-08-30 and
its sweep did not see it, because the engine sessions swept the ENGINE
tree and this claim sits in the emitter.

Its sibling in the same moved text, `ehrt.sim-engine.engine/
Demographics`, **does** resolve -- `engine.clj:153`, a delegating def --
so the claim was checked per-name rather than in aggregate, and only one
of the two is stale.

Moving (B) verbatim carries a known-false claim into a new file, which
is stated plainly rather than glossed. The alternative -- repointing it
in the move commit -- would have been a fix found in passing, in a
cluster whose whole warrant is that its 155 lines diff as eight.

### 2f. THE FENCED CITATION -- the emit phase's first

`components/person-simulator/test/ehrt/person_simulator/limitations_
test.clj:152-155`, the site the twelfth session pre-located, reads:

> the row's substance is now FALSE BY DESIGN: `emit_hl7.clj`'s
> `demographics-timeline` folds `:demographic-update` and
> `:coverage-change` into a t-ascending per-patient timeline, and
> `demographics-at` answers state-at-t.

A PATH claim plus two mover names, and **this move makes the path
claim false**. It is a comment in a TEST FILE, which **C1(a) forbids
touching**, so it is disclosed and backlogged, not repointed. No gate
parses it -- the charter gate parses `limitations.md`, not this file --
so nothing goes red and the staleness is prose-only. **The first FENCED
CITATION of the emit phase**, after the engine phase's thirteen.

### 2g. Claims INSIDE `emit_hl7.clj`, both directions

**In the MOVED text, two claims go false**, both paid in the MOVE commit
on the ninth and tenth extraction's rule that restating them a commit
early makes them false in the interim. Every stay-behind form named in
the moved prose was enumerated mechanically -- `emit`, `pid-segment`,
`context-for-event`, `periodic-chatter` -- and only two carry a
positional word:

* `demographics-at`: "the single lookup shape every PID-rendering site
  in **this namespace** goes through". Every site it means stayed
  behind. Becomes "in the emitter", the twelfth session's own
  substitution.
* `encounter-spans`: "so the periodic half **below** has no census".
  `periodic-chatter` stayed behind. The word is dropped; the sentence
  re-flows across two lines.

`demographics-timeline`'s "**This namespace** may not depend on
sim-engine at all" was read and **left verbatim**: the constraint is
stated component-wide in the same sentence
(`components/sim-emit-hl7` depends on `components/sim-model` and nothing
else), and it is as true of `timelines.clj` as of `emit_hl7.clj`. The
other bare names -- `demographics-at`, `pid-segment`,
`context-for-event`, `emit`, `demographics-timeline`, `mrn-timeline` --
carry no positional claim and stay true across the seam.

**In the RESIDUE, nothing goes false.** All three prose mentions outside
the two regions were listed with their owning line and read:
`:323` inside `pid-segment` ("It reaches here only through
`demographics-timeline`'s own fold" -- "here" is `pid-segment`, which
stays), `:1759` ("over the encounter intervals `encounter-spans`
derives") and `:1867` ("of it is `demographics-at` of a patient at an
instant"). All three are bare names in behavioural claims.

## 3. Step 3 -- `ehrt.sim-emit-hl7.timelines` (`5aa2ac4`)

### The moved body diffs as EIGHT lines, and no others

Verified by diffing the moved text as a BLOCK against `e940f24`'s own
`emit_hl7.clj` -- the two regions plus one separator, **155 lines either
side** -- not inferred from hunk headers. Five are the `defn-` -> `defn`
markers; three are the two prose corrections named in 2g, the second of
which re-flows onto two lines. **Not one other docstring or comment line
differs.**

### The five widenings

| private mover | callers staying behind | disposition |
|---|---:|---|
| `demographics-timeline` | 2 | public in `timelines`, no def |
| `demographics-at` | 12 | public in `timelines`, no def |
| `encounter-spans` | 3 | public in `timelines`, no def |
| `mrn-timeline` | 1 | public in `timelines`, no def |
| `mrn-at` | 1 | public in `timelines`, no def |

All nineteen call sites are qualified `timelines/...`, counted per
mover by the rewriting script and reconciled against the derivation:
12 + 2 + 3 + 1 + 1 = 19.

### Asserted live under `-M:dev`, not argued

* `timelines`' public surface is **exactly the five**, and `ns-interns`
  minus `ns-publics` is EMPTY -- nothing in it is private.
* **Constraint 5's prohibition**: `emit-hl7/demographics-timeline`,
  `/demographics-at`, `/encounter-spans`, `/mrn-timeline` and `/mrn-at`
  all fail to resolve. **FIVE of five.**
* `timelines` holds **zero namespace aliases**, confirming the
  no-`:require` derivation from the LOADED namespace rather than from
  the source text alone.
* **`emit_hl7.clj`'s public surface is 24 vars before and 24 after**,
  zero gained and zero lost, both sides derived by the same scanner.
* `interface.clj` still resolves, 20 publics, and re-exports none of the
  five.

`clojure -M:poly check` OK.

### The require set, re-derived in BOTH directions

`timelines.clj` needs nothing. `emit_hl7.clj` gained ONE require and
lost none, checked rather than assumed: all seven aliases are still
called in the residue -- `parser` 173, `timelines` 19, `hl7-time` 16,
`site-profile` 16, `registry` 14, `sim-model` 5, `str` 4. **This move
leaves no dead require behind**, which is now three for three in the
emit phase against the engine phase's nine.

## 4. Steps 3-4 -- the gates

`bin/regression-oracle e3ce663 5aa2ac4`: **IDENTICAL: every root's
digest matches**, 41 roots, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** -- the eleventh session's finding
-- and it covers the rendered messages themselves.

`bin/ground-truth-bracket e3ce663 5aa2ac4`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0. **Near-vacuous here and it says so
in its own output** -- "THIS IS NOT A REGRESSION-ORACLE CLAIM: the
`:hl7` half of every root is excluded by construction". Run and reported
because the prompt asks for it, not because it discriminates.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj`, the tenth extraction's
hazard class.

Both gates span `e3ce663` -> `5aa2ac4`, the WHOLE session's change
rather than just the move -- the wider range is the stricter claim, and
`e940f24` between them is docs-only.

### The suite delta, measured IN-CLONE

`make test` unpiped through a wrapper ending in `exit "$MAKE_EXIT"`, run
at `e940f24` (the pre-move tree, in the clone, no worktree) and at
`5aa2ac4`. Both `MAKE_EXIT=0`, both **408 zero-failure blocks / 4,751
tests** -- so **this move adds no `deftest`**, confirmed per namespace
rather than from the total. 4,751 is the count the twelfth extraction
closed on.

Assertions go **24,137 -> 24,141, +4**, attributed per namespace by
diffing per-namespace counts out of the two logs. **Exactly two
namespaces move, and both were predicted before the move:**

| namespace | delta | why |
|---|---:|---|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | 266 -> 268 | `doseq` over every production `.clj` under `components/*/src` and `bases/*/src`, one `is` per file |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | 26 -> 28 | `doseq` over every `.clj` under `components/sim-emit-hl7/src`, one `is` per file |

Each moves by ONE file and is counted TWICE, because `conformance` and
`ehrt-cli` both run `docs-tooling`'s suite. `timelines.clj` is the one
new production source file and both gates count files; it passes the
second on its merits, requiring nothing at all. **+4 is the WHOLE
delta**, and code-attributable and measured delta agree for the second
session running -- the eleventh's worktree contamination stays absent
rather than argued away.

**The close-out run is assertion-for-assertion identical to the move
run** -- 24,141 either way, 408 blocks either way, per-namespace diff
EMPTY -- so this session's doc additions (the record, the prompt
archive, the P5 rewrite, the two regenerated indexes and
`state-derived.md`) move no gate's population.

The baseline was taken at `e940f24` rather than at the session's start
tip `e3ce663`, which is a deviation from the twelfth's practice and a
deliberate one: `e940f24` is docs-only, it is the tree the move actually
departs from, and taking it there avoids a `git checkout` in a clone
that is the sole clone of record. The two tips are gate-population
identical, which the run itself proves: `e940f24` measured 4,751 /
24,137, the twelfth session's closing figures to the assertion.

## 5. Closing arithmetic

### The partition, cluster 3 of 8

`emit_hl7.clj` goes 2,220 lines to **2,065**, and 79 def-forms to
**74** -- it lost five and gained NONE, **the first cluster in either
file to gain nothing**. `timelines.clj` is 187 lines / 5 forms plus its
`ns`, and is the second namespace in the emitter to need no `:require`
at all.

### Census corrections

* Section 2's `timelines` row says "5 forms, 130 lines" in the summary
  table and "156 lines" in the form list; the forms are 5 and the
  FORM-lines are **151**, which is section 2a's own figure, confirmed to
  the line. Section 2a is right and both of section 2's figures are
  measuring something else.
* Section 2's line spans for this cluster are stale by the two prior
  moves, as expected: `515-595` is now `361-440`, `1886-1914` now
  `1655-1682`, and so on. The spans are re-derived per session by
  design (`.agents/plans/README.md` says so), so this is not an erratum.
* Section 2a's "three leaves" is confirmed for the THIRD and last of
  them: `timelines` has zero outgoing edges at `e940f24`, in BOTH the
  bare-name and the qualified-symbol scan.
* **Section 3b's four `timelines`-as-callee rows reproduce EXACTLY on
  section 3b's own accounting**, which is DISTINCT (caller, callee)
  pairs -- 18. The raw call-site count is **19**; `bed-swap-message`
  calls `demographics-at` twice. First divergence of the two
  accountings in the program.
* Section 2a's "timelines owes no `interface.clj` re-export" is
  confirmed exactly, read from `interface.clj`.
* **New, and not in the census at all**: `timelines` has **zero internal
  edges**, which is why the caller-travels/weighted-pick shape cannot
  arise for any of its five movers. Section 2a says all five widen
  "because all four of its consumers stay behind"; the stronger reason
  is that no mover has a mover for a caller.
* **New, and not in the census at all**: census constraint 6 is written
  for citations that a MOVE makes stale. Two claims found this session
  were stale BEFORE it (section 2e) -- one from the engine phase's third
  session, one from the twelfth's. A pre-move sweep finds them and has
  no rule for them; this session's rule was disclose-and-backlog.

### The backlogs

* **FENCED CITATIONS** gains its first emit-phase row:
  `person-simulator/limitations_test.clj:152` (path claim naming
  `demographics-timeline` and `demographics-at`, stale with this move,
  test file, C1(a)-fenced).
* **STALE-BEFORE-THIS-MOVE**, a class the program has not had before,
  two rows: `emit_hl7.clj:514` (src, "`emit_hl7.clj`'s own registry
  comment"; referent is `registry.clj:41`) and
  `timelines.clj`'s `encounter-spans` docstring
  (`ehrt.sim-engine.engine/stamp-encounter` does not resolve; the form
  is `ehrt.sim-engine.encounters/stamp-encounter`). Neither is
  C1(a)-fenced -- both are production source -- and neither is this
  cluster's to fix.
* **RETIREMENT CANDIDATES**: none added.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt's edge expectation is wrong for the third time.**
   "Expect `hl7-time` possible, nothing else" -- the tree gives ZERO
   outgoing edges, no `:require` and no `:import`. The prompt was right
   that the channel has erred on edges twice and right to say correct
   from the tree; the correction is that the census was already right
   and the channel's hedge was not.
2. **The prompt's "some may have all callers travelling, the
   weighted-pick shape"** cannot arise, for a structural reason the
   prompt did not have: zero internal edges.
3. **No sweep commit exists**, because the sweep owes zero repoints.
   The prompt's "hit list or disclosed absence first" is answered with
   the absence, and every level's result is in section 2.
4. **The baseline was measured at `e940f24`, not `e3ce663`** -- section
   4, with the reason and the evidence the two are gate-identical.
5. **Two lines of the moved text are not verbatim** beyond the five
   marker widenings. Both are named before the move in section 2g and
   `rulings.md#R-move-not-improve` is not strained.
6. **C6(a)'s narrower reading was taken** -- section 0.

## 6. What is left in this program

Five emit clusters (`er7`, `segments`, `messages`, `planners`,
`facade`), then the apply-path unification. The next session takes
`er7` -- 19 forms, census 2a's fourth, and the first cluster whose
dependency has already landed rather than being a leaf: its single
outgoing edge is `context-for-event` -> `demographics-at`, which now
crosses into `timelines`, so `er7.clj` will be the first emitter
namespace to carry a `:require` on a SIBLING extraction rather than on
`emit_hl7.clj`. Two of its nineteen forms are public (`escape-er7`,
`unescape-er7`), both of which `emit_hl7.clj` keeps in its public
surface today, so C1(a) owes two delegating defs.

Its constraint-6 exposure is not yet located and should not be assumed
empty: `escape-er7`/`unescape-er7` are named in
`notes/sim/facts-register.md`'s F9 row by an ANCIENT path
(`src/ehr_testing_sim/emit_hl7.clj`), which is already stale and is a
citation register rather than a charter register.

## 7. The budget, after C6(a)

C6(a) recovered **20 lines**; the P5 row's thirteen-landing rewrite --
the emit landings sentence, the two new findings' backlog row, and the
record path -- spends **12** of them. `:onboarding` closes at
**1522 of 1530, 8 lines of headroom**, and the fourteenth emit session
inherits real headroom for the first time since the eleventh. (A first
draft of the row update spent 17 and left 3; it was compacted again
before the commit rather than shipped at the margin.) What remains compactable, if a future session
needs it, is what section 0 deliberately left: the row's standing
doctrine still carries engine-phase INSTANCE detail (which session found
which census error, which cluster left `weighted-pick` private), and
compacting that is re-triage and therefore the author's
(`rulings.md#R-section-retriage-is-author-judgement`).

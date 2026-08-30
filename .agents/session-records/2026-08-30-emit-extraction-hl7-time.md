# Emit namespace extraction, 1 of 8: the order, and the `hl7-time` cluster

Session record, 2026-08-30. HEAD at start `74a8e6a`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed the same day in ten sessions.
Author rulings C1(a) (`emit_hl7.clj` stays the facade, moved PUBLIC vars
get delegating defs, private movers widen ONLY where a caller stays
behind, no test file changes) and S1(a) (an equivalence proof replaces
red-before-green).

`bin/preflight` exit 0, **no findings** -- the second clean preflight of
the program, and for the same reason as the first: this session's own
first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 1. Step 0 -- the order, derived rather than transcribed (`892c416`)

The prompt asked for the DAG and an order committed into the census as a
section 2a addendum. Both were derived from THIS tree at `74a8e6a` with
a char-level scanner for every top-level form's true span and a
whole-symbol scan over each form's body with string literals, character
literals and line comments stripped. Census section 3b was the prior.

**Section 3b is confirmed EXACTLY, with nothing to correct in it** --
which no section of the census's ENGINE half managed across ten
sessions, though the tenth confirmed 4a and 4b that way.
All sixteen edges, and every
one of the sixteen edge counts (62, 21, 18, 16, 13, 10, 6, 4, 4, 3, 3,
2, 2, 1, 1, 1), reproduce. Section 2's population closes both ways: 86
def-forms in the tree, 86 names in section 2's map, zero in either and
not the other. No back edge, so 3b's "already a DAG, no breaker needed"
holds without qualification.

### Three corrections, and one of them answers the channel's question

* **THREE leaves, not one.** `hl7-time`, `registry` and `timelines` each
  have zero outgoing edges. The channel expected `hl7-time` or `er7`;
  **`er7` is NOT a leaf** -- it carries one edge into `timelines`
  (`context-for-event` -> `demographics-at`), which is the very edge
  section 3b names as its reason for placing `context-for-event` there.
  Half the channel's guess was right and it named the reason it was
  wrong in its own text.
* **`planners` has zero INCOMING edges.** Nothing in `emit_hl7.clj`
  calls a planner; every caller is `interface.clj` or `ehrt.sim.run`.
  Its position in the order is judgment, not graph -- stated as such in
  the addendum rather than dressed up as derived.
* **Section 2's spans run to the NEXT FORM'S START**, so they include
  the banner comment blocks between forms: `evn-segment` is `320-325`,
  not `320-341`. Every cluster's form-line total is smaller than section
  2's table says, and the addendum records both, because they measure
  different things and the smaller one is what a mover moves.

### The order, and the hazard that does not arise yet

```
hl7-time -> registry -> timelines -> er7 -> segments
   -> messages -> planners -> facade
```

`hl7-time` first because it is a leaf and the smallest cluster in the
file -- the least code that can prove the seam. **The caller-travels
require-cycle the `run` extraction paid does not arise in clusters 1-7**:
the callers all STAY in `emit_hl7.clj`, so their bare names keep
resolving through the delegating defs and no `requiring-resolve` shim is
needed. It can only arise if the `facade` cluster itself ever moves --
whether `emit_hl7.clj` ends a pure facade the way `engine.clj` did under
C4(b) is the author's call, and the addendum says so rather than
assuming it.

`interface.clj` re-exports SIXTEEN `emit-hl7` forms, distributed
`hl7-time` 2, `registry` 7, `segments` 1, `planners` 4, `facade` 2;
`timelines`, `er7` and `messages` owe none. Derived by reading
`interface.clj`, which the channel had not probed.

## 2. Step 1 -- the movers, and where every one of them is named

### Seven forms, three regions, 47 form-lines

| form | span | lines | marker |
|---|---|---:|---|
| `default-reference-date` | 30-36 | 7 | `def` |
| `default-utc-offset` | 38-43 | 6 | `def` |
| `hl7-timestamp-formatter` | 225-226 | 2 | `def ^:private` |
| `reference-instant` | 228-230 | 3 | `defn-` |
| `hl7-offset-suffix` | 232-236 | 5 | `defn-` |
| `hl7-timestamp` | 238-249 | 12 | `defn` |
| `transmit-seconds` | 762-773 | 12 | `defn-` |

Three public, four private. No banner comment is attached to any of
them -- the cluster is the only one in either file whose regions are
bounded by blank lines alone, so the move takes no prose with it and
leaves no orphaned header.

### `interface.clj`: two of the three public movers, and the third is owed to the tree

`default-reference-date` (`:23`) and `default-utc-offset` (`:24`) are
re-exported and need delegating defs. **`hl7-timestamp` is not
re-exported at all and needs one anyway**, for a reason derived rather
than assumed: fourteen files require the IMPLEMENTATION namespace
`ehrt.sim-emit-hl7.emit-hl7`, and three of them call
`emit-hl7/hl7-timestamp` at **thirteen sites** -- `emit_hl7_test.clj`
(6), `result_clock_test.clj` (5), `latency_test.clj` (2). Each file's
own `ns` alias was read, not guessed. Twenty-one bare-name sites inside
`emit_hl7.clj` resolve through the same def.

`components/sim/{identifiers,run}.clj` alias `ehrt.sim-emit-hl7.interface`,
NOT the implementation -- confirmed by reading both `ns` forms, so the
two `default-*` re-exports are the whole of sim's exposure.

### The one forced widening, and the four call-site classes

`transmit-seconds` is private and **eleven forms that stay behind call
it, at twelve sites** (ten in `messages`, two in `emit-wire`). That is
C1(a)'s "widen only where a caller stays behind" in its literal case:
it becomes `defn` in `hl7-time` and gains NO delegating def, because
widening `emit_hl7.clj`'s own public surface is not what C1(a) asks
for. The twelve sites are qualified `hl7-time/transmit-seconds`.

The other three private movers -- `hl7-timestamp-formatter`,
`reference-instant`, `hl7-offset-suffix` -- have **no caller anywhere
outside the cluster**. Only `hl7-timestamp` calls them, and it travels
with them. All three stay `defn-`/`^:private`: constraint 5 read as a
prohibition, the eighth-through-tenth extraction's own reading.

## 3. Step 2 -- the sweep, which is EMPTY, and how that was established

Run at all four levels before the move. **Not one repoint was owed
outside `emit_hl7.clj` itself, so there is no sweep commit.** A commit
with no changes is not a commit; the accounting is here instead.

### 3a. Level 1 -- 245 shingles over 853 files, zero hits, with a positive control

The seven movers' docstrings and comments were cut into 245 six-word
shingles and searched across the whole non-frozen tree. **Zero hits.**

A zero result from an instrument nobody has watched work is not
evidence, so the scanner was run as a POSITIVE CONTROL over a NON-mover:
`message-type-registry`'s own prose yields 1,218 shingles and finds
**15 hits in `emit_hl7_test.clj`**. The instrument is live; the zero is
real.

**The control also exposed the instrument's floor, which is a finding
rather than an aside.** It finds nothing in
`patient-simulator/docs/limitations.md`, which pins `emit_hl7.clj`
together with the phrase `"no real CarePlan-equivalent segment"` -- five
words, below a six-word shingle's reach. **A level-1 shingle sweep
cannot see a charter register's pinned phrase.** Level 3's direct
register read is therefore coverage, not redundancy, and census
constraint 6 is right to demand it separately.

### 3b. Level 2 -- every path and namespace claim in the tree, classified

Counted at `892c416` with `git grep` against that commit, so this
session's own edits cannot contaminate the numbers.

PATH claims naming `emit_hl7.clj`: **twenty-two** in the live tree,
every one read. **None pairs the path with an hl7-time mover.** The
nearest is
`patient-simulator/docs/limitations.md`'s charter row, whose pinned
phrase lives at `emit_hl7.clj:154` -- inside `message-type-registry`, a
`registry` form. **Constraint 6's class arrives at cluster 2, not
here**, and the next session should expect it.

NAMESPACE claims of the form `ehrt.sim-emit-hl7.emit-hl7/<var>`:
**forty-five**. Four name an hl7-time mover:

| site | names | disposition |
|---|---|---|
| `sim-model/persona.clj:167` | `default-reference-date`'s own YEAR | public mover, delegating def forwards it -- stays TRUE |
| `sim/docs/sim-theory.edn:110` | `default-reference-date` | same |
| `sim-emit-fhir/emit_fhir.clj:101` | `hl7-timestamp` | same |
| `sim-emit-hl7/test/.../v2_replay_test.clj:194` | `reference-instant` | PRIVATE mover, no def can forward it -- **FENCED**, backlog |

A third class was read in full rather than counted and dismissed:
**forty-three BARE mentions of `ehrt.sim-emit-hl7.emit-hl7`** with no
`/var` after them -- fourteen `:require` aliases and two `ns`
declarations, all still valid because the namespace still exists, and
twenty-seven prose sentences
about the emitter in general (`single-subject-message`,
`observation-obx-segment`, the registry, PID enrichment). **Not one
names an hl7-time mover or makes a positional claim about one.**

Census constraint 2's class -- a doc pinning a mover's own DOCSTRING as
its authority -- was checked directly and **does not arise**: a grep for
"own docstring" paired with any of the seven names returns nothing.
`persona.clj` pins the VALUE ("`default-reference-date`'s own year"),
which a delegating def preserves exactly, verified `identical?` live.

### 3c. Two claims INSIDE `emit_hl7.clj`, both paid in the move commit

* `event->messages`'s docstring: "since `transmit-seconds` (**this
  namespace, private**) falls back to a 0 offset". Both halves go false.
* `transmit-seconds`'s own docstring: "sampling stays out of emit, per
  **this namespace's** own renders-only doctrine". "This namespace"
  changes referent the moment the form moves.

Both were fixed in the MOVE commit rather than the sweep, on the ninth
and tenth extraction's own rule: restating them a commit early would
have made them false in the interim.

`obr-segment`'s docstring names `hl7-timestamp` bare and was left alone
-- the delegating def forwards it and it stays true.

### 3d. Level 3 -- the registries, and a prediction corrected from the tree

* **`hand-owned-assets.edn`.** All five rows read, and their three
  distinct sources read. **The `gt-emitters.svg` tripwire does NOT
  fire**, which is the prompt's own prediction corrected from the tree.
  `docs/dev/simulator-architecture.md` names exactly two movers,
  `hl7-timestamp` (`:428`) and `transmit-seconds` (`:431`), and both are
  BARE NAMES in the ADR-0142 addendum's prose, pinned to no path and no
  defining form. Both stay true, nothing was edited, and the row's
  `:reviewed-at "3739084"` still equals that file's last-change commit,
  checked with `git log -1` rather than assumed. **The engine's
  five-in-ten fire rate does not carry into the emit phase, and the
  reason is structural**: that page names ENGINE forms by defining form
  and EMITTER forms by bare name.
  `two-clocks.svg` shares the source and is already `:stale` with a live
  roadmap anchor; claim (d) reads `:fresh` rows only.
  The other three sources (`palgebra-design.md`, `pipeline.edn`,
  `ed-tuesday/README.md`) name no mover -- counted, zero each.
* **Both charter registers.** `person-simulator/docs/limitations.md` has
  no `emit_hl7.clj` row at all; its rows 1 and 10 pin `streams.clj`,
  repointed by the FIRST extraction. Its bare-token scan
  (`limitations_test.clj:225`) asserts `#{"ehrt.sim-engine.interface"}`
  over person-simulator's OWN `src` -- a different tree, invisible to an
  added emitter namespace. `patient-simulator`'s one row is 3b's
  `registry` phrase.
* **`exercised-sources.edn`.** Pins READMEs and use-case pages, no
  emitter source. Untouched.
* **No surface enumerates this component's namespaces.**
  `simulator-architecture.md:38` looks like one but is a DATED record of
  what was read on 2026-08-11, not a live list; the bricks table names
  `interface.clj` only. So a ninth namespace needs no registry row --
  checked by searching for `sim_emit_hl7/` as a path, all eight hits
  read.
* **No build surface names the file.** `Makefile`, `.gitattributes`,
  `workspace.edn` and `deps.edn` carry no `emit_hl7.clj` path, so
  `hl7_time.clj` needs no registration anywhere.

### 3e. Level 4

Nothing outside `emit_hl7.clj` was edited, so `make state-derived` had
nothing to regenerate until the close-out docs landed, where it is run
LAST.

## 4. Step 3 -- `ehrt.sim-emit-hl7.hl7-time` (`62a6877`)

### The name

No collision: nothing in the tree names `sim-emit-hl7.hl7-time` or
`sim_emit_hl7/hl7_time`, checked before writing the file. The alias
`hl7-time` is unused in `emit_hl7.clj`.

### The moved body diffs as TWO lines, and no others

Verified by diffing the moved text as a BLOCK against `892c416`'s own
`emit_hl7.clj` -- the three regions plus their separators, 53 lines
either side -- not inferred from hunk headers. `diff` reports exactly
two changed lines:

* `(defn- transmit-seconds` -> `(defn transmit-seconds`, the one forced
  widening.
* `emit, per this namespace's own renders-only doctrine (docs/dev/` ->
  `emit, per the emitter's own renders-only doctrine (docs/dev/`.

**Not one other docstring or comment line differs**, including every
interior claim the moved text carries -- `default-utc-offset`'s "see
`hl7-timestamp`", `transmit-seconds`' "`plan-latency` is the one place
offsets are ever sampled" -- because both referents travelled with it or
are still one namespace away.

### The residue

One `:require`, three delegating defs under one `;; --- moved to ...`
block, twelve qualified call sites, and the one docstring repair.
The block is placed where region 1 stood, at the top of the file, which
is also what the load order requires: `hl7-timestamp`'s def has to
precede its first bare-name use at what was line 801.

### Asserted live under `-M:dev`, not argued

* The three delegating defs hold the **IDENTICAL object** as their
  `hl7-time` counterparts. Three of three.
* `interface.clj` still resolves through `emit_hl7.clj`: both re-exports
  `identical?` to the facade's vars.
* **Constraint 5's prohibition**: `emit-hl7/hl7-timestamp-formatter`,
  `/reference-instant`, `/hl7-offset-suffix` and `/transmit-seconds` all
  fail to resolve. Four of four.
* In `hl7-time`, three of the four stay private and `transmit-seconds`
  alone is public; the namespace's entire public surface is
  `{default-reference-date, default-utc-offset, hl7-timestamp,
  transmit-seconds}`.
* **`emit_hl7.clj`'s public surface is 24 vars before and 24 after,
  zero gained and zero lost** -- the pre-move set derived from the file
  by the same scanner, not transcribed.
* The clock renders identically through both paths, and
  `transmit-seconds` is still the identity on `{}`.

`clojure -M:poly check` OK.

### The require set, re-derived in BOTH directions

`hl7_time.clj` needs `[clojure.string :as str]` and nothing else --
re-derived from the moved text's own qualified symbols, not carried
forward from `emit_hl7.clj`'s `ns`: the only other outside references
are `java.time.format.DateTimeFormatter` and `java.time.LocalDate`,
both written fully qualified, so no `:import` travels either.

`emit_hl7.clj` gained ONE require and lost none, and that is checked
rather than assumed: all five of its aliases are still called in the
residue -- `parser` 169, `site-profile` 15, `hl7-time` 15, `str` 4,
`sim-model` 4. **This move leaves no dead require behind**, unlike the
engine phase, which finished with nine and named them for the repoint
pass. No `deps.edn`, `workspace.edn` or `.gitattributes` change: the
component gains a namespace, not a dependency.

## 5. Step 4 -- the gates, and WHICH ONE IS LOAD-BEARING HERE

`bin/regression-oracle 892c416 62a6877`: **IDENTICAL: every root's
digest matches**, 41 roots, `declared-digest-change: no`, soundness
confirmed outside the leading docstring.

`bin/ground-truth-bracket 892c416 62a6877`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`. No soundness abort: nothing in this
session touched `components/oracle/src/ehrt/oracle/digest.clj`, the
tenth extraction's new hazard class.

**THE INSTRUMENTS SWAP AT THE EMISSION LAYER, and the bracket says so in
its own output.** It prints "THIS IS NOT A REGRESSION-ORACLE CLAIM: the
`:hl7` half of every root is excluded by construction" -- so for a
cluster inside the emitter the bracket is close to vacuous: it proves
the ground-truth log did not move, which an emitter-only change could
not have moved anyway. The load-bearing gate here is the ORACLE, whose
41-root digest covers the rendered messages themselves. That is the
reverse of the engine phase, where the bracket isolated ground truth
from emission and was the sharper instrument. **Every later emit cluster
should read the two gates in this order.**


### The suite delta, explained to the assertion -- and a measurement hazard found doing it

`make test` unpiped through a wrapper ending in `exit "$MAKE_EXIT"`,
run THREE times: at the pre-move commit `892c416`, at `62a6877`, and
again at the close-out tree once the docs landed. All three
`MAKE_EXIT=0`, all three **408 zero-failure blocks / 4,751 tests**, and
4,751 is the same test count the tenth extraction closed on -- this move
adds no `deftest`. **The close-out run is assertion-for-assertion
identical to the move run** -- 24,133 either way, per-namespace diff
empty -- so this session's doc additions (a record, a prompt archive,
the P5 rewrite, the regenerated indexes) move no gate's population.

Assertions go **24,127 -> 24,133, +6**, attributed per namespace by
diffing per-namespace counts out of the two logs rather than reasoning
from the totals. Three namespaces move, each by one, each counted twice
because `conformance` and `ehrt-cli` both run `docs-tooling`'s suite:

| namespace | delta | why |
|---|---:|---|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | 131 -> 132 | `doseq` over every production `.clj` under `components/*/src` and `bases/*/src`, one `is` per file |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | 11 -> 12 | `doseq` over every `.clj` under `components/sim-emit-hl7/src`, one `is` per file |
| `ehrt.sim.version-test` | 5 -> 6 | **not the change -- an artifact of the baseline TREE**; see below |

The first two are predicted and are the whole of this move's real cost:
`hl7_time.clj` is one new production source file and both gates count
files. `hl7_time.clj` passes the second on its merits, requiring only
`clojure.string`.

**The third is a measurement hazard, and it is worth more than the
assertion it moved.** The baseline was run in a `git worktree`, where
`.git` is a FILE (`gitdir: ...`) rather than a directory.
`ehrt.sim.version/git-sha` reads `(io/file ".git" "HEAD")` with no
subprocess, by design, so in a worktree it returns `nil` --
**asserted directly in both trees rather than inferred**: `nil` in
`/tmp/wt-baseline`, `"62a6877e..."` in the clone. `version-test`'s last
`is` sits inside `(when (version/git-sha) ...)`, so it simply does not
run in a worktree.

**A git worktree is therefore NOT a valid baseline tree for a
suite-assertion-delta measurement.** The engine phase's practice --
compare two runs in the SAME clone -- is right, and this session's
shortcut cost one contaminated row. The code-attributable delta is
**+4, exactly as predicted**; +2 of the +6 is the instrument.

`bin/regression-oracle` is unaffected and that was checked rather than
assumed: it runs BOTH sides in worktrees, so the placeholder is
symmetric and cancels -- which is consistent with its IDENTICAL result
over 41 roots.

## 6. Closing arithmetic

### The partition, cluster 1 of 8

`emit_hl7.clj` goes 2,498 lines to 2,473, and 86 def-forms to 82 -- it
lost seven and gained three. `hl7_time.clj` is 73 lines / 7 forms plus
its `ns`. The file-level total grows by 48 lines, all of it the new
`ns` docstring and the moved-to block, which is the cost of making the
seam legible and is paid once per cluster.

### Census corrections, one sentence each

* Section 3b's sixteen edges and sixteen counts are confirmed EXACTLY at
  `74a8e6a`; nothing in it needed correcting, which is a first.
* Section 3b's DAG claim holds: no back edge, no breaker.
* Section 2's cluster line totals count the banner comments between
  forms, because its spans run to the next form's start; the addendum
  records the form-line totals beside them.
* Section 3b's own leaf reasoning is right about `context-for-event` and
  therefore implies what it does not say: `er7` is not a leaf, and the
  leaves are three.
* Section 2's `hl7-time` row says "7 forms, 54 lines"; the forms are 7
  and the FORM-lines are 47.

### The backlogs

* **FENCED CITATIONS** gains one of the rarest class:
  `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj:194`,
  a NAMESPACE claim about a PRIVATE mover (`ehrt.sim-emit-hl7.emit-hl7/
  reference-instant`) that no delegating def can forward. C1(a) forbids
  touching it.
* **RETIREMENT CANDIDATES**: none added. All three of `hl7-time`'s
  delegating defs have named callers -- two through `interface.clj`, one
  through thirteen test-file sites.

### Deviations from the prompt, all disclosed rather than absorbed

1. **No sweep commit exists.** The prompt names one ("docs: the
   `<cluster>` cluster's pre-move citation sweep") and its gate is "hit
   list first". The hit list outside `emit_hl7.clj` is EMPTY at all four
   levels, and the two claims inside it belong to the move commit by the
   ninth and tenth extraction's own rule. A commit with no changes is
   not a commit; section 3 is the accounting the commit would have
   carried. The bracket-collision half of that gate was still checked:
   `oracle/digest.clj` is untouched by this session, so the hazard the
   tenth extraction found did not arise and nothing needed isolating.
2. **The channel's leaf expectation was half wrong** and is corrected in
   census 2a: `er7` is not a leaf, and there are three.
3. **The prompt's `gt-emitters` prediction did not hold** and is
   corrected from the tree, with the structural reason, in section 3d.
   No red-first pair was created, because nothing went red.
4. **Two lines of the moved text are not verbatim.** Both are forced by
   the move, both were named before it, and both are in section 4.
   `rulings.md#R-move-not-improve` is not strained: neither is a fix
   found in passing, they are the relocation's own debris.

### ESCALATION: the P5 row has ONE line of budget left, and seven clusters to go

`.agents/plans/roadmap.md` is an `:onboarding` member, and that set was
at **1521 of 1530** when this session opened -- nine lines. The P5
rewrite here (the emit phase note, the eleventh landing, the tripwire
correction, the instruments-swap lesson, one Records line) was compacted
to **+8**, landing the set at **1529 of 1530: one line of headroom.**

**The next emit session cannot add a line to this row without
compacting first**, and there are SEVEN clusters left, each of which has
historically earned one. `rulings.md#R-budget-stop` says compact or
stop, never bump, and `.agents/reading-sets-baseline.edn`'s ratchet caps
`:budget-lines` at 1530 and is editable only by a compaction ADR. This
session compacted what was its own to compact; what is left is the
ENGINE phase's closed narrative in the same row -- roughly forty lines
of per-cluster detail already recorded in nine session records, which
`rulings.md#R-roadmap-row-destinations` says may move verbatim into the
row's owning ADR. **That move is re-triage and therefore the author's,
not a build session's** (`R-section-retriage-is-author-judgement`), so
it is named here rather than made.

### What is left in this program

Seven emit clusters (`registry`, `timelines`, `er7`, `segments`,
`messages`, `planners`, `facade`), then the apply-path unification.
The next session takes `registry` -- thirteen forms, ten public, SEVEN
of them `interface.clj` re-exports, and the first to meet census
constraint 6 for real, at `patient-simulator/docs/limitations.md`'s
pinned five-word phrase inside `message-type-registry`.

### CI at the pushed tip -- the close marker

`gh run watch 33342013524 --exit-status` exits 0; the run is **completed
/ success** in 12m33s at `f766d65f45efc591585284d61d894ecc5256195b`, the
pushed tip
(https://github.com/pragsmike/ehr-testing-tools/actions/runs/33342013524).
That is the close marker under `rulings.md#R-session-verifies-ci-via-gh`,
which the de-scaffold ruling of 2026-08-25 retired as a TAG condition and
kept as this. No tag was paid.

**No red-first pair exists this session, and that is the substantive
difference from five of the ten engine sessions.** Nothing went red
locally, so nothing had to be pushed in a pair to clear it: the
`gt-emitters` tripwire never fired, because no source it names was
edited. `bin/post-push-verify 74a8e6a f766d65` reports the remote tip
matching HEAD and every commit message in range pure ASCII, and the
push's own gate (`gitleaks detect` over 1,267 commits, `clojure -M:poly
check`) was clean.

**Three commits, and the middle one is the whole claim**: the census
order (`892c416`), the output-identical move (`62a6877`), and this
close (`f766d65`). The emit phase is open and its first cluster has
landed with both gates IDENTICAL and no declaration.

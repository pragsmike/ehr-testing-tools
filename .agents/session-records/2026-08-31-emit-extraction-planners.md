# Emit namespace extraction, 7 of 8: the `planners` cluster, and two docs rulings

Session record, 2026-08-31. HEAD at start `05afc27`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed 2026-08-30 in ten sessions and
whose emit half has now landed seven of eight clusters. Author rulings
C1(a) with its C7 extension, constraint 5 as a PROHIBITION, S1(a), and
two NEW docs rulings taken FIRST, each as its own commit: **C9(a)** and
**C10(b)**.

`bin/preflight` exit 0, **no findings** -- the eighth clean preflight of
the program, and for the same reason as the first seven: this session's
own first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 0a. Step 0a -- the C9(a) compaction (`aff2ec9`)

The sixteenth session closed the P5 row at **1529 of 1530, ONE line of
headroom**, and escalated rather than bumped. C9(a) is the emit-phase
twin of C8(a) and equally narrow: the row's remaining EMIT-phase
INSTANCE detail may become pointers at the six emit records; standing
doctrine stays.

**Compacted, all to pointers**: the six-landing enumeration
(per-cluster forms/form-lines/defs/widenings, regions, edge counts);
the four `NEW WITH THE THIRTEENTH/FOURTEENTH/FIFTEENTH/SIXTEENTH`
blocks, reduced to their doctrine with every per-session tally dropped;
the emit half of the FENCED CITATIONS backlog; and the
STALE-BEFORE-THE-MOVE backlog's per-member file-and-line detail.

**Kept, in substance**: every rule the emit phase established -- 3b
counts DISTINCT pairs not sites; stale-before-the-move is disclosed and
backlogged; the caller-travels shape; C7 and its exhaustion;
requalification and that DEPTH drives it; a require can go dead; the
residue-claim class reaching both a banner and its own `ns` docstring;
and that a banner's real object is a SECTION whose ownership changes
hands.

**Headroom, before and after, as the prompt required**: `:onboarding`
**1529 of 1530 (ONE)** before, **1513 of 1530 (SEVENTEEN)** after;
`roadmap.md` 397 -> 381 lines. Sixteen lines recovered against the
fourteen C8(a) recovered, and the two compactions together have taken
the row from 1530/1530 to 1513/1530 without deleting one standing rule.

## 0b. Step 0b -- the C10(b) prose correction (`0c4b83f`)

C10(b) rules the four files still citing gates `e189418` deleted
(de-scaffold, 2026-08-25) corrected -- named as conventions or the
citation removed -- and rules OUT restoring any gate. None is restored.
`rulings.md` and `AGENTS.md` had already been corrected by that ruling's
own partial sweep; these four had not.

**Lines re-derived at `aff2ec9` rather than carried from the finding**,
and two of the four pointers moved:

| ruled | actual | what it said |
|---|---|---|
| `.agents/reading-sets.edn:3` | `:3` **and `:15`** | the budget gate, plus a 20-comment-line header cap the SAME gate held |
| `.agents/prompts/README.md:18` | `:18` | `prompt-record-pairing-test` "fails the build in either direction" |
| `.agents/reading-sets-baseline.edn:92` | `:92` **and `:6`** | `state-residue-test`; and, at `:6`, the budget gate's own enforcement claim in the same present tense |
| `state_derived_test.clj:132` | **`:133`** | the `testing` docstring, "the number the budget gate enforces" |

**`state_derived_test.clj` is a TEST file and editing it is an
author-ruled exception to C1(a)'s fence**, scoped by C10(b) to that one
docstring. It is also why the tree-wide grep for the six gate names
never hit this file: the citation names the gate by DESCRIPTION, not by
symbol. The `deftest` NAME at `:132` still reads
`line-count-is-the-budget-gates-own-measurement-test` and is
deliberately left -- renaming it moves a test name, outside what the
exception grants.

**`.agents/prompts/README.md` is an `:onboarding` path, so its edit is
LINE-NEUTRAL by construction** -- 36 lines before and after -- or it
would have spent headroom C9(a) had just recovered one commit earlier.
`reading-sets.edn`'s header still measures exactly 20 comment lines,
now by convention rather than by a gate.

**FOUND BEYOND THE RULED FOUR, disclosed and NOT fixed**, because
C10(b) names four files and a session does not widen its own ruling.
Five more live surfaces still cite a deleted gate in the present tense:
`docs_tooling/state_derived.clj:77` (`reading-set-budget-test`) and
`:173` (`rulings-lint-test`), both `defn` docstrings on a SRC path;
`Makefile:256` (`state-residue-test`); `notes/prompts/README.md:14`
(`notes-prompts-frozen-test`); and `adr_index_test.clj:21`
(`reading-set-budget-test`, a test file and C1(a)-fenced in any case).
**The backlog row goes from four members to nine, four of them now
paid.** Two of the five sit in `state_derived.clj`, the very renderer
whose test docstring C10(b) did correct -- the same file's `src` half is
still wrong.

Gate for both step-0 commits: `clojure -M:poly test brick:docs-tooling`
green, 96 zero-failure blocks, roadmap-lint and the state-derived parity
pair inside it. `make state-derived` moved exactly one cell, the
`:onboarding` budget row.

## 1. Step 1 -- the derivations

### Eleven forms, 364 form-lines, ONE region

Derived at `0c4b83f` with a char-level scanner for every top-level
form's true span, and a whole-symbol scan over each form's body with
string literals, character literals and line comments stripped. The
scanner reproduces the sixteenth session's own closing count exactly --
**34 def-forms** in the file plus the `ns`.

| form | span | lines | marker |
|---|---|---:|---|
| `plan-latency` | 314-352 | 39 | `defn` **public** |
| `restatement-day-seconds` | 371-376 | 6 | `def` **public** |
| `chatter-trigger` | 378-381 | 4 | `defn-` |
| `event-driven-chatter` | 383-408 | 26 | `defn-` |
| `periodic-chatter` | 410-470 | 61 | `defn-` |
| `assign-restatement-ordinals` | 472-502 | 31 | `defn-` |
| `plan-chatter` | 504-527 | 24 | `defn` **public** |
| `plan-charges` | 529-605 | 77 | `defn` **public** |
| `ladder-stage` | 634-639 | 6 | `defn-` |
| `rung-instant` | 641-647 | 7 | `defn-` |
| `plan-ladders` | 649-731 | 83 | `defn` **public** |

**364 form-lines, census 2a's figure to the line**, the SEVENTH cluster
running where 2a is right and section 2's own two figures (335 in the
summary table, 419 in the form list) measure something else.

**ONE REGION, and that is a first for either file.** Lines 302-731 were
checked line by line and contain nothing but these eleven forms, the
three banners heading them, and thirteen blank separators -- **zero
other content**. Every prior cluster gathered from three regions or
more; the fifteenth gathered from eight. A cluster with no incoming edge
tends to have this shape, because nothing above it needed to interleave.

### THREE BANNERS TRAVEL, all three whole

| banner | span | heads | disposition |
|---|---|---|---|
| ADR-0109: the second clock | `:302-312` | `plan-latency` | TRAVELS |
| ARC 4 SWEEP 2: re-statement chatter | `:354-369` | the chatter block | TRAVELS |
| ARC 4 SWEEP 3: status ladders | `:607-632` | the ladder block | TRAVELS |

None had to be split, because each heads a section wholly this
cluster's -- the same reason the sixteenth's five travelled, arrived at
from the other direction: those were sections a PRIOR cluster had split,
these were never split at all.

### The edges: 3b reproduces EXACTLY, and pairs EQUAL sites for the first time

**OUTGOING**, by distinct (caller, callee) pair, which is 3b's own
accounting:

| callee cluster | bare-name pairs | qualified pairs | total | census 3b |
|---|---:|---:|---:|---:|
| `registry` | 5 | 1 | **6** | 6 |
| `timelines` | 0 | 4 | **4** | 4 |
| `segments` | 2 | 1 | **3** | 3 |

**INCOMING: ZERO**, derived rather than trusted -- every one of the
twenty-three residue forms was scanned for every one of the eleven mover
names, and not one hit. Census 2a's claim confirmed by measurement.

**INTERNAL: nine edges**, and they are why six of six privates stay
private: `plan-chatter` calls three of its helpers, `plan-ladders`
three, `periodic-chatter` and `plan-charges` read
`restatement-day-seconds`, and `event-driven-chatter` calls
`chatter-trigger`.

**All three rows EXACT, and 13 distinct pairs against 13 raw sites.**
That is the FIRST cluster in the program where the two accountings
agree; the prior four diverged 18/19, 34/43, 66/70 and 122/137. The
reason is structural rather than lucky: every crossing here is a single
reference, because a planner reads a table or mints an id ONCE.

### SIX OF SIX PRIVATE MOVERS STAY PRIVATE -- the whole set

Constraint 5 read as a PROHIBITION, at its limit. The engine's ninth
left eighteen of nineteen and the sixteenth left ten of twelve; this is
the first cluster in either file where the WHOLE private set survives,
and the reason is the zero incoming edge count: there is nothing to
widen for, because no caller was ever outside these eleven forms.

* **FOUR public delegating defs, C1(a)**: `plan-latency`,
  `plan-chatter`, `plan-charges`, `plan-ladders`.
* **ZERO widenings.** No private mover becomes public anywhere.
* **`restatement-day-seconds` travels PUBLIC and gains no def** -- a
  shape new to the program: a public mover with no caller anywhere
  outside its own cluster. Its two callers, `periodic-chatter` and
  `plan-charges`, both travelled.
* **ZERO `^:private` defs**, and it is a closure rather than an absence
  -- see the `#'` census.

### The four `interface.clj` re-exports, and why they are load-bearing twice

`plan-latency` (`:48-52`), `plan-chatter` (`:67-71`), `plan-charges`
(`:73-77`) and `plan-ladders` (`:84-88`) -- census 2a's four exactly,
read out of `interface.clj` rather than out of the census. All four are
`defn` wrappers calling `emit-hl7/plan-...` at RUNTIME, so the chain
`ehrt.sim.run` -> `interface.clj` -> the delegating def -> `planners`
must hold at every link, and C1(a) fences `interface.clj` from edits.
`ehrt.sim.run` aliases the INTERFACE (`run.clj:33`), read from its own
`ns` rather than guessed, so it never touches the implementation.

**Each def is owed TWICE over**, because four `sim-emit-hl7` test files
alias the IMPLEMENTATION directly -- `charges_test.clj:19`,
`chatter_test.clj:28`, `latency_test.clj:24`, `ladders_test.clj:25`,
all `[ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]` -- and call the same
four names at twenty-eight sites.

**A PROMPT CLAIM CORRECTED.** The prompt called this "the FIRST emit
cluster owing interface-load-bearing delegating defs". It is the
FOURTH: `hl7-time` owed two, `registry` seven and `segments` one, and
all sixteen re-exports are load-bearing at runtime. What IS first here
is the SHARE -- four of this cluster's five publics, against one of
`segments`' one and seven of `registry`'s ten -- and that every one of
the four is reached through a `defn` wrapper rather than a value
re-export.

### The `#'` census: still EXHAUSTED

Re-run over the whole tree rather than trusted. **108 `#'` sites exist
in the tracked tree** (the sixteenth counted 106; the tree has grown by
this program's own records, and the figure is re-measured, not carried).
Filtered against `emit-hl7` and against all eleven mover names: **ZERO**.
Widened to `resolve`, `ns-resolve`, `requiring-resolve`, `with-redefs`,
`alter-var-root`, `intern`, `find-var` and `(var ...)`: **also zero**.

So **no C7 def is owed**, and the fifteenth session's prediction --
"`messages`, `planners` and `facade` owe no `^:private` def unless a
test file changes" -- holds for the second of the three.

### SEVEN REQUALIFICATIONS, over FIVE names, named in advance

| bare name | sites | forms | becomes |
|---|---:|---:|---|
| `chatter-event-kinds` | 2 | 2 | `registry/chatter-event-kinds` |
| `control-id-for` | 2 | 2 | `segments/control-id-for` |
| `room-and-board-code` | 1 | 1 | `registry/room-and-board-code` |
| `order-status-ladder` | 1 | 1 | `registry/order-status-ladder` |
| `result-status-ladder` | 1 | 1 | `registry/result-status-ladder` |

**Exactly the five names cluster 6 predicted, by name, in its own
close-out** -- and the prompt asked for "~five, named in advance", which
is what the census scan produced before the move. The rewriter then
produced 2/1/1/1/2 INDEPENDENTLY and reported its own hits: **TOTAL 7**,
the same five numbers twice from two different programs. The rewriter is
a character state machine tracking in-string, in-character-literal and
in-comment state and rewriting in CODE positions only, so **not one word
of prose moved with them**.

**The class FALLS, 64 sites to 7, and depth is not the whole story.**
The sixteenth's lesson was that depth drives it; this cluster sits
DEEPER still (seventh) and pays a ninth of the bill. The other half of
the rule is how much of a cluster was ALREADY qualified before the seam:
`plan-charges` alone carries `timelines/encounter-spans`,
`segments/charge-concept` and `registry/charge-closing-kinds`, all
written qualified when their siblings landed.

### No collision

Nothing in the tree names `sim-emit-hl7.planners` or
`sim_emit_hl7/planners`, checked with an unpiped `git grep` in both
spellings. The token `planners` does not appear in `emit_hl7.clj` at
all. It DOES appear in `timelines.clj`'s `ns` docstring twice, both as
the CLUSTER name in census terms ("the arc-4 planners", "in `er7`,
`messages`, `planners` and `facade`") -- claims this move makes more
true, not less. No build surface enumerates this component's
namespaces, so a TWELFTH file needs no row.

## 2. Step 2 -- the sweep, which owes ONE repoint

**No sweep commit precedes the move, and the absence is derived rather
than assumed.** Every level ran and every level is reported. No red was
predicted and none occurred. The one repoint the sweep owes is TRUE
until the seam, so restating it a commit early would make it false in
the interim -- the ninth and tenth extraction's rule -- and it is paid
in the move commit.

### 2a. Both charter registers, hand-read

**ZERO of the eleven movers is named in either register.** Counting
path-like tokens with this session's own rule
(`[A-Za-z0-9_./-]+\.(clj|md|edn|json|svg)`, DEDUPLICATED): 17 in
`components/patient-simulator/docs/limitations.md` and 12 in
`components/person-simulator/docs/limitations.md`. **DISCLOSED: those
figures are not the sixteenth session's 15 and 13** -- neither file has
been edited since, so the whole difference is the dedup step, and this
record states its own rule rather than reproducing a number it cannot
reconstruct. The material result does not depend on the rule: the level
is EMPTY.

The one `emit_hl7`-shaped token in either register is still the
component DIRECTORY inside
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/registry.clj`, the
twelfth session's own repoint, which still resolves.

### 2b. Level 1 -- 1,674 shingles, 25 files hit, zero positional

The eleven movers' docstrings and `;;` comments, plus the three
travelling banners, cut into 1,674 distinct six-word shingles and
searched across the whole tracked tree with a single `git grep -F -f`.
**162 hit lines across 25 files, and not one is a positional claim about
a mover.** Four classes, the same the fifteenth and sixteenth named:

* **Session-marker phrases**: `notes/adr/0175` (10 hits, the largest
  single file), the two arc-4 sweep prompts and their records.
* **Doctrine echoes in TEST files**, C1(a)-fenced in any case:
  `charges_test.clj`, `chatter_test.clj`, `ladders_test.clj`.
* **Doctrine echoes in LIVE surfaces**: `sim-model/config.clj` (6),
  `sim/run.clj` (5), `messages.clj` (2), `fan_out.clj` (1),
  `docs/dev/simulator-architecture.md`, `docs/consuming-ground-truth.md`
  and the two ed-tuesday configs. All bare names in behavioural claims;
  every one read.
* **Coincidental prose**: `judge/sampling.clj`, `corpus/lineage_test.clj`
  and the two MLLP tests share sentences with the ladders banner about
  content-addressing and same-second collisions. Checked and unrelated.

### 2c. Level 2 -- paths and namespaces, and NOTHING GOES FALSE

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<mover>`,
checked per mover rather than in aggregate: **NINE**, and **ZERO go
false**, because every one names a public mover that keeps a delegating
def.

| site | names | disposition |
|---|---|---|
| `interface.clj:50`/`:69`/`:75`/`:86` | all four planners | FENCED from edits, and TRUE -- the defs keep them resolving |
| `sim-model/config.clj:215`/`:235`/`:263`/`:295` | all four planners | live `src`, and TRUE for the same reason. **No repoint owed** |
| `notes/adr/0142-result-clinical-time.md:610` | `plan-latency` | dated ADR record, standing treatment |

That is the exact inverse of cluster 6, where `merge-message` was
private, no def answered, and `v2_replay.clj:403` had to be repointed.
**A cluster whose publics are all re-exported owes the sweep nothing at
the namespace level**, and C1(a) is why.

**ALIAS-qualified prose** `emit-hl7/<mover>` outside the file: all
resolve. `sim/run.clj:485`/`:495` alias the INTERFACE, which has all
four. `demos/scenarios/ed-tuesday/README.md:360` and
`docs/dev/simulator-architecture.md:393` name `emit-hl7/plan-latency`
component-alias-style, which the def keeps true.

**PATH claims naming `emit_hl7.clj` about a mover: ONE, and it GOES
FALSE.** `components/sim-engine/src/ehrt/sim_engine/assignment.clj:19`
lists "Eight files across five bricks" citing the
`assign-pathway`/`assign-module` pair as the fixed-consumption
precedent, and names `emit_hl7.clj`. All FOUR of that file's citations
of the pair or the law sit inside movers -- `:317-318` in
`plan-latency`, `:385` in `event-driven-chatter`, `:620` and `:624` in
the ladders banner. After this move `emit_hl7.clj` cites the pair not at
all. It is a `src` file, so C1(a) does not fence it, and the repoint is
OWED -- mechanically the same reasoning that unfenced `v2_replay.clj:403`
for the sixteenth. **REPOINTED to `planners.clj`.** Still eight files,
still five bricks.

The thirteenth, fifteenth and sixteenth each read this line and each
deferred it to "cluster 7", by name. It is the only cross-session
prediction in the program that named its own payer.

`notes/adr/0171:181` names `emit_hl7.clj:987` for `plan-latency`'s draw
-- a PATH AND LINE claim, dated ADR, standing treatment, and already
stale by hundreds of lines before this session opened (`plan-latency`
sat at 314). Named here so the next reader does not rediscover it.

### 2d. Level 3, the rest -- and the tripwire that fires for a NEW reason

* **`hand-owned-assets.edn`.** All rows read and all four distinct
  sources read: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/palgebra-design.md`,
  `components/corpus/docs/pipeline.edn` and
  `demos/scenarios/ed-tuesday/README.md`. **TWO OF THE FOUR NAME A
  MOVER** -- `simulator-architecture.md` at `:393`, `:400` and `:430`
  and `ed-tuesday/README.md` at `:360`, all `plan-latency`. **That has
  not happened before in the emit phase**, and the roadmap's own
  structural reason ("EMITTER forms as BARE NAMES") is why it did not
  matter: every one of the four is a bare name or an
  `emit-hl7/plan-latency` component-alias reference, and the delegating
  def keeps all four TRUE. Neither file has a `emit_hl7.clj` path claim
  at all. **Nothing was edited, no `:reviewed-at` is bumped, and the
  tripwire does not fire -- the EIGHTH session in the program to read
  them and fire nothing, and the FIRST to do so while the sources
  actually named a mover.** The recipe still earned its keep: absence
  was not what made it safe this time, C1(a) was.
* **Docstring-as-authority (census constraint 2).** Every backtick-cited
  token inside the moved text was enumerated and resolved:
  `ehrt.sim-emit-hl7.event-conformance-test`,
  `ehrt.sim-emit-hl7.site-profile` (x3),
  `ehrt.sim-engine.engine/assign-pathway`,
  `ehrt.sim-engine.engine/config-keys`, `v2-replay/evolve-entry`,
  `docs/dev/simulator-architecture.md`, and both
  `rulings.md#R-...` anchors. All resolve -- **except one, and it was
  already false; see 2e.** No form left in the residue cites a mover's
  docstring, checked rather than assumed.
* **The moved text carries NO un-re-depthed `docs/` citation**, so that
  backlogged family does not travel: its one path claim,
  `docs/dev/simulator-architecture.md`, resolves.

### 2e. PRE-EXISTING FALSE CLAIMS: one TRAVELS, a first

`rulings.md#R-move-not-improve` is why none is fixed. The sixteenth's
four stand unchanged. **What is new is that this move RELOCATES one of
them**, which no prior move did: `plan-charges` (`emit_hl7.clj:541`)
cites `ehrt.sim-engine.engine/stamp-encounter`, and that has not
resolved since the third engine session -- `stamp-encounter` lives in
`ehrt.sim-engine.encounters` (`:138`) and `engine.clj`'s facade
re-exports `assign-pathway` and `config-keys` but not it, confirmed by
reading the facade rather than assuming.

**And the class is wider than the backlog says.** Four sites, three
files: `timelines.clj:137` (the backlogged one), `plan-charges` (now
`planners.clj`), and `messages.clj:223` and `:238`, which name it
`engine/stamp-encounter` in the shorter form the backlog's grep shape
would miss. Disclosed, not fixed; the row is widened from one site to
four.

### 2f. THE RESIDUE-CLAIM CLASS FIRES A THIRD TIME -- three claims, two places again

Every count re-derived from the tree, never adjusted by arithmetic on
the old number.

| where | claim | why it goes false | now |
|---|---|---|---|
| `registry` banner `:74-76` | "plus the bare-name sites below that keep resolving through these defs" | the last five bare sites are `chatter-event-kinds` x2, `room-and-board-code`, `order-status-ladder`, `result-status-ladder` -- all in movers | **ZERO**; all ten defs now stand for the test tree and `interface.clj` alone |
| `segments` banner `:143-145` | "`plan-charges`' `segments/charge-concept` is the one that remains" | `plan-charges` leaves | **NONE remains** |
| `messages` banner `:255-256` | "`emit_hl7.clj` builds no message text of its own any more -- it plans, and delegates the rendering" | the planning leaves too | retold in the past tense: it now delegates BOTH |

**The two-places sub-class, confirmed a second time.**
`segments.clj`'s OWN `ns` docstring carries the identical
`charge-concept` sentence, and it goes false the same way. Cluster 6
found this class; cluster 7 confirms it is not a one-off. Both are
corrected in the move commit.

**Checked and LEFT, with the reason stated rather than assumed:**

* `registry` banner `:79-81` and `registry.clj`'s twin -- "`sch-segment`,
  `event->messages` and `plan-charges` still call them". `plan-charges`
  is the LAST of the three to leave the file. The sentence is phrased in
  FORMS, and all three forms still call them from wherever they now
  live, so it survives -- the fifteenth's and sixteenth's own ruling on
  this exact sentence, applied a third time. **A judgment call, and the
  strain is now visible: zero of the three forms are in this file.**
  Named for the eighteenth session, which may want to retire it.
* `segments` banner `:127-128` -- "`control-id-for` ... its def is owed
  twice over". Still true, and now also true in-file: `emit-wire:806`
  keeps a live bare call, the ONLY in-file caller of any delegating def
  this move leaves behind.
* `hl7-time` banner `:53-57` -- "`emit-wire` alone still names
  `hl7-time/transmit-seconds`, twice". No mover names `transmit-seconds`
  at all. SURVIVES.
* `er7` banner `:185-186` -- "NOT ONE call site is left in this file".
  The moved text uses zero `er7/` sites. SURVIVES.
* `timelines.clj`'s "nineteen call sites across sixteen forms in `er7`,
  `messages`, `planners` and `facade`" -- cluster terms, SURVIVES
  exactly, and this session is the one that makes `planners` a real
  namespace rather than a census word.
* **The historical-landmark class**, read and left again: the `messages`
  banner's "`chatter-message` from between `plan-chatter` and
  `plan-charges`", and `timelines.clj`'s account of its own two
  corrections. Past-tense accounts of where a form sat at ITS move,
  accurate as history.

### 2g. Claims INSIDE the moved text: THREE go false

Every positional word in the moved text was enumerated mechanically --
"below", "above", "this file", "this namespace", "this section", "here",
"earlier", "later", "elsewhere". **Sixteen occurrences; THREE go
false**, all paid in the move commit under the fifteenth session's
precedent:

* the ADR-0109 banner's "**this namespace's** own `emitH` consumption of
  it" -> "the emitter's". `planners` consumes no `emitH`.
* the same banner's "Sampling itself stays OUT of **this namespace**
  (**this file's** own renders-only doctrine ...)" -> "OUT of the
  emitter (`emit_hl7.clj`'s own ...)". Two words in one sentence.
* the ARC 4 SWEEP 2 banner's "keeps **this namespace's** renders-only
  doctrine intact" -> "the emitter's".

All three name the RENDERS-ONLY doctrine, which is the emitter's and
which `planners` does not have -- it plans, and renders nothing. That is
the shape of the class here, and it is a new one: cluster 5's two were
`below` words about form ORDER; these are about namespace IDENTITY.

**Thirteen were checked and left VERBATIM**: "Nothing below needs a
fixed-consumption law because nothing below consumes" (`plan-ladders` is
last in `planners.clj`, so it is true vacuously, and it was true of
`emit-wire` before); "the event-driven half above" (both halves travel,
in order); six `here`s that scope inside their own form; "the
comprehension above", nine lines up in its own form; "later
re-discharged" and "elsewhere", neither positional.

### 2h. Fenced citations: ZERO ADDED

The first emit cluster to add none since the fourteenth. No test file
makes a claim about a mover that this move falsifies, for the same
reason 2c gives: the four public movers keep defs and the six privates
are named in no test file at all.

## 3. Step 3 -- `ehrt.sim-emit-hl7.planners` (`6b5d2f1`)

### The moved body diffs as TEN lines, and no others

Verified by diffing the moved text as a BLOCK against `0c4b83f`'s own
`emit_hl7.clj` -- **430 lines either side** -- not inferred from hunk
headers. 7 are the requalifications; 3 are the forced prose corrections
of 2g. **Not one other docstring, comment or code line differs**, and
the diff is line-for-line symmetric (10 removed, 10 added).

**ZERO MARKER CHANGES, a program first.** No `defn-` becomes `defn`
anywhere, because six of six privates stay private and the five publics
were already public. Every prior cluster widened at least one.

### The residue's non-comment changes are SIX lines

Read off `git diff -U0`: the `:require` block gains one alias (and loses
none), and the four delegating defs. Every other addition in the residue
is a `;;` line -- the three banner corrections of 2f and the new 47-line
moved-to banner.

### The dispositions, asserted live under `-M:dev` rather than argued

* `planners` has **11 interns and 5 publics**; `ns-interns` minus
  `ns-publics` is exactly the SIX helpers.
* **Constraint 5's prohibition, asserted per name**: `emit-hl7/chatter-
  trigger`, `/event-driven-chatter`, `/periodic-chatter`,
  `/assign-restatement-ordinals`, `/ladder-stage`, `/rung-instant` and
  `/restatement-day-seconds` all fail to resolve. **SEVEN of eleven**,
  and the seventh is a PUBLIC form -- the first time a public mover
  leaves this file without a def.
* All four of `plan-latency`, `plan-chatter`, `plan-charges` and
  `plan-ladders` resolve in `emit-hl7`, are PUBLIC, and each is
  `identical?` to its `planners` var.
* **`interface.clj` still resolves, 20 publics, and re-exports exactly
  the four**, confirmed by intersecting the interface's publics against
  the mover set rather than by reading the census. All four called
  end-to-end on an empty log through `interface.clj` and answered.
* `emit_hl7.clj`'s public surface is **24 vars before and 23 after** --
  it lost five publics and kept four defs, and the arithmetic works
  because `restatement-day-seconds` is the fifth.
* `planners` holds **exactly three** namespace aliases (`registry`,
  `timelines`, `segments`) and `emit-hl7` **eight**, both read off the
  LOADED namespaces rather than the source. Three is the NARROWEST
  require set of any non-leaf cluster in this file.

`clojure -M:poly check` **OK**. No Error 104: all three of `planners`'
requires are intra-component siblings.

### The require set, re-derived in BOTH directions -- NONE goes dead

`planners.clj` needs three and uses all three: `registry` 6,
`timelines` 4, `segments` 3. It needs no `clojure.string`, no
`sim-model`, no `site-profile`, no HL7 parser, and no `:import` --
`Math/round` is `java.lang`.

`emit_hl7.clj` gained ONE (`planners`) and **lost none**, going seven
aliases to eight. Checked per alias in the residue rather than assumed:
`registry` 10, `hl7-time` 5, `sim-model` 4, `timelines` 3, `er7` 3,
`segments` 3, `messages` 3, `planners` 4. **The `planners` require
serves the four delegating defs and NOTHING ELSE** -- there is no call
site, because there was never an incoming edge. That is a require whose
whole justification is C1(a), and it is the clearest statement of what a
facade edge costs.

### TWENTY of the twenty-four delegating defs now have no in-file caller

Derived per def in both directions. FIVE **lose their last in-file
caller to this move** -- `room-and-board-code`, `chatter-event-kinds`,
`order-status-ladder`, `result-status-ladder` (all `registry`) and
`charge-concept`'s `segments/` qualified site -- joining the thirteen
the sixteenth counted plus its own. What remains with an in-file caller
is a SHORT list, and `emit-wire` is nearly all of it:
`default-utc-offset`, `control-id-for` (`:806`), `event->messages`,
and `hl7-time/transmit-seconds` twice.

**The four defs this move ADDS have no in-file caller from the moment
they are written** -- another first. Every prior cluster's defs answered
at least one bare name below them.

## 4. Steps 3-4 -- the gates

`bin/regression-oracle 0c4b83f 6b5d2f1`: **IDENTICAL: every root's
digest matches**, **41 roots**, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** -- the eleventh session's finding
-- and it is load-bearing in a particular way here. These eleven forms
are the ADD-ON layer: latency offsets, chatter, charges and ladder
rungs. A misplaced requalification among the seven would have moved no
plain ADT message at all, but it would have moved every arc-4 add-on in
every root that configures one, and the oracle digests the `:hl7` half
whole. Nothing moved.

`bin/ground-truth-bracket 0c4b83f 6b5d2f1`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0.

**What it can see here, since the prompt asks: almost nothing, and its
own output says so.** It prints "THIS IS NOT A REGRESSION-ORACLE CLAIM:
the `:hl7` half of every root is excluded by construction", and this
cluster produces nothing else -- a planner's entire output is an
instruction map that only `emit-wire` consumes, on the excluded side.
What it does prove is a NEGATIVE that still matters: that the move did
not reach across the component boundary and perturb ground truth, which
is exactly the class the tenth extraction's `digest.clj` hazard made
real. Reported because it was asked for, not because it discriminates.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj`, the tenth extraction's
hazard class.

### The suite delta, measured IN-CLONE

The baseline is the STEP-0b COMMIT itself, `0c4b83f`, which is an
in-clone baseline needing no stash: because C9(a) and C10(b) landed as
their own docs commits, `0c4b83f` IS the pre-move tree. Both runs
`MAKE_EXIT=0`, both **408 zero-failure blocks over 216 distinct
namespaces / 4,751 tests** -- so **this move adds no `deftest`**,
confirmed by differencing the namespace sets, which are equal.

Assertions go **24,153 -> 24,157, +4**, and the delta was PREDICTED from
the two file-globbing gates' own populations before the final run, with
the populations counted rather than reasoned:

| namespace | population | delta |
|---|---|---:|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | `doseq` over `components/*/src` + `bases/*/src` `.clj`, 128 -> **129** | +1, x2 projects |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | `doseq` over `components/sim-emit-hl7/src` `.clj`, 11 -> **12** | +1, x2 projects |

Each runs in both `conformance` and `ehrt-cli`, so 2 x (1 + 1) = **+4**,
and measured equals predicted exactly. `planners.clj` is the one new
production source file and it passes the dependency gate on its merits
-- all three of its requires are intra-component siblings.

The baseline itself is worth one line: `0c4b83f` measured **24,153**,
assertion-for-assertion identical to the sixteenth session's own close,
which is what two docs commits and a single test DOCSTRING edit should
be.

**The record and prompt archive move no gate's population**, confirmed
rather than assumed: `index_completeness_test`'s two `doseq`s iterate
DIRECTORIES, not files, and the pairing gate that once read
`.agents/prompts` was deleted by `e189418` -- which is the finding
C10(b) exists to answer, arriving here as a convenience.

**DISCLOSED, because the alternative would be a false claim.** This run
was taken on the final tree with THREE PLACEHOLDER LINES still in this
record -- this section's, the budget section's and the CI section's --
which were filled afterwards, and before the prompt archive was
transcribed to ASCII. No gate reads this file's CONTENT: the
prompt/record pairing gate is deleted, the index gates count directory
entries, and the two gates that moved glob `src` paths only. The
alternative is to state numbers before measuring them, which is worse.

**No red was seen at any point**, in either the baseline or the final
run. The sixteenth session's sequencing red -- a `state-derived.md`
parity failure from running the suite across an uncommitted
`roadmap.md` edit -- did not recur, because `make state-derived` ran
after the roadmap edit and before the suite, which is the order that
session's own disclosure prescribed.

## 5. Closing arithmetic

### The partition, cluster 7 of 8

At the move commit `6b5d2f1`, `emit_hl7.clj` goes 828 lines to **458**
and 34 def-forms to **23** -- it lost eleven and gained four -- and
`planners.clj` is **507** lines / 11 forms plus its `ns`.

Arithmetic, checked rather than asserted: 828 - 430 removed + 47 banner
lines + 4 defs + 2 blanks + 1 `:require` line + 6 net banner-correction
lines = **458**, which is `wc -l` on the file at that commit.

### Census corrections

* Section 2's `planners` row says "11 forms, 335 lines" in the summary
  table and "419 lines" in the form list; the forms are 11 and the
  FORM-lines are **364**, which is section 2a's own figure, confirmed to
  the line. **SEVEN clusters running** where 2a is right and both of
  section 2's figures measure something else.
* Section 2's line spans for this cluster are stale by the six prior
  moves, as expected and by design.
* **Section 3b's THREE `planners` rows all reproduce EXACTLY** --
  `registry` 6, `timelines` 4, `segments` 3 -- and its ABSENCE of a
  `planners`-as-callee row is confirmed by measurement, not inferred.
* Section 2a's "`planners` has zero INCOMING edges" and "four of its
  five public forms are `interface.clj` re-exports": both exact.
* Section 2a's placement judgment holds. It put `planners` seventh
  "because it is the arc-4 add-on layer", conceding the graph left it
  free. Nothing in the move needed the position: all three dependencies
  had landed by cluster 5, so this cluster could have gone fifth, sixth
  or seventh unchanged.
* **New, and not in the census**: a cluster with ZERO incoming edges
  tends to be ONE CONTIGUOUS REGION, because nothing above it had a
  reason to interleave with it. The census models regions per cluster
  and never connects the two facts.
* **New, and not in the census**: distinct PAIRS and raw SITES can be
  EQUAL, and it is a signal rather than a coincidence -- it says every
  crossing is a single reference, which is what a layer that reads
  tables and mints ids looks like.
* **New, and not in the census**: a require can exist for the delegating
  defs ALONE. The census's edge model has no vertex for that, because
  the edge is created by C1(a) rather than found in the tree.

### The backlogs

* **FENCED CITATIONS: ZERO added.** Still four rows, the thirteenth's
  and the sixteenth's three.
* **RETIREMENT CANDIDATES**: five more delegating defs lose their last
  in-file caller (`room-and-board-code`, `chatter-event-kinds`, the two
  status ladders, `charge-concept`'s site). Twenty of twenty-four now
  have none. All correctly kept by the facade rule.
* **STALE-BEFORE-THIS-MOVE**: unchanged at four rows, but one of them
  is now WIDER (`engine/stamp-encounter` at four sites in three files,
  not one) and one has TRAVELLED (into `planners.clj`), which no prior
  move did.
* **DELETED GATES STILL CITED**: four of nine paid by C10(b); five
  remain, two of them in `state_derived.clj`'s own `src` half.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt's "the FIRST emit cluster owing interface-load-bearing
   delegating defs" does not survive derivation.** It is the fourth;
   what is first is the SHARE and the `defn`-wrapper shape. Section 1
   states the measured version.
2. **The prompt asked for the requalification count named in advance,
   "expect ~five".** Five NAMES and seven SITES, named before the move
   and then reproduced independently by the rewriter.
3. **The prompt's expected "predicted reds RED-FIRST" did not arise.**
   No gate reads anything this session changed: the hand-owned-asset
   sources name a mover but no claim of theirs goes false, and the
   charter registers name none. No deliberate red, so none to pair --
   the third session running with none.
4. **The moved text is NOT verbatim**, and the deviation is named: seven
   requalifications (licensed by the prompt) and three prose corrections
   (not). The three are forced by the seam under the fifteenth's own
   precedent -- a claim the move itself falsifies is the mover's to pay,
   in the move commit -- and they are the smallest word-level edits that
   make the sentences true.
5. **C10(b) was executed at four files and found five more.** Not
   widened; disclosed and backlogged.
6. **`reading-sets-baseline.edn` carried TWO citations, not the one the
   ruling's line pointer named**, and `state_derived_test.clj`'s was at
   `:133` rather than `:132`. Both re-derived, as the ruling asked.
7. **A tooling hazard hit and named** so the next session does not pay
   it again: `$( ... )` inside a double-quoted `wsl -e bash -lc "..."`
   expands in the OUTER shell, which made a `grep` for a file that
   existed report that it did not. Single-quote the wrapper argument
   when a command substitution matters -- the same class as the `$?`
   hazard already on record.

## 6. What is left in this program

ONE emit cluster (`facade`), then the apply-path unification.

**The facade cluster, factually, for the author's C11 ruling.** It is
three forms and **136 form-lines** -- census 2a's figure to the line --
`default-providers` (`:263-269`, 7 lines, a public `def` with NO caller
anywhere in the tree outside this file), `emit` (`:271-303`, 33) and
`emit-wire` (`:363-458`, 96). `interface.clj` re-exports `emit` and
`emit-wire`, census 2a's two, both as `defn` wrappers with three and two
arities. If cluster 8 ran, `emit_hl7.clj` would keep its `ns`, its
twenty-four existing delegating defs and up to three new ones, and hold
**ZERO forms of its own** -- the pure-facade shape `engine.clj` reached
under C4(b), at roughly 330 lines against `engine.clj`'s 741. The
difference from every cluster so far is the one census 2a flagged as
OPEN: `emit` and `emit-wire` are the CALLERS, so if they travel, the
caller travels, and `facade.clj` would need `emit_hl7.clj`'s twenty-four
delegating defs to resolve from a namespace that may not require its own
facade. The engine paid that with a `requiring-resolve` shim in
`ehrt.sim-engine.run`; here it would be paid by requalifying `emit` and
`emit-wire`'s bare names against seven siblings instead -- a
requalification bill this session's own instruments can price before the
author rules. **Whether `emit_hl7.clj` ends a pure facade is C11, and
this session does not presume it.**

## 7. The budget

C9(a) recovered **sixteen** lines (1529 -> 1513). This session's own P5
row update spends **thirteen** of them -- one for the seventh landing,
eight for the NEW WITH block, three for the two rulings, one for the
widened stale-before-the-move row -- and closes at **1526 of 1530, FOUR
lines of headroom**. `roadmap.md` 397 -> 381 -> 394.

The Records list absorbed this session's path at ZERO cost, on the
existing last line, which now carries seven emit records.

**ENGINE DOCTRINE WAS NOT TOUCHED.** The NEW WITH THE SEVENTEENTH block
is doctrine only, by C9(a)'s own principle: not one per-cluster count,
region tally or file-and-line pointer entered the row, and the instance
detail for every claim in it is here.

**ESCALATED, and stated plainly.** Four lines is more than the one the
sixteenth escalated on, and it is still not a landing. The eighteenth
session -- whichever shape C11 gives it -- cannot both close the emit
phase and write its own NEW WITH block inside four lines. What remains
compactable in this row is now the ENGINE phase's surviving doctrine
paragraphs (the tripwire recipe, the bracket-source hazard, the
instruments swap, the facade/implementation law, the coverage note),
which C8(a) deliberately KEPT, and re-triaging them is the author's
under `rulings.md#R-section-retriage-is-author-judgement`. Two
compactions in two sessions have taken this row from 1530/1530 to
1526/1530; the third would have to cut doctrine rather than instance,
which is a different decision and not a session's to make.

## 8. CI at the pushed tip -- the close marker

CI_PLACEHOLDER

# Emit namespace extraction, 8 of 8: the facade cluster, and the extraction phase closes

Session record, 2026-08-31. HEAD at start `0bd9ddc`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed 2026-08-30 in ten landings and
whose emit half completes here at eight. Author rulings C1(a) with its
C7 extension, constraint 5 as a PROHIBITION, S1(a), and **C11(a)**, the
ruling this session exists to execute: the three facade-cluster forms
move out and `emit_hl7.clj` ends as delegating defs only.

`bin/preflight` exit 0, **no findings** -- the ninth clean preflight of
the program, and for the same reason as the first eight: this session's
own first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 0. The name, justified in one line as the prompt asked

**`ehrt.sim-emit-hl7.emit`**, because a namespace here is named for the
form it holds and `ehrt.sim-engine.run` -- the engine's own last cluster,
under the ruling this one mirrors -- was named by exactly that rule.
`assemble` was the alternative the prompt offered and it names a verb no
form in the file uses; `facade` is now wrong for either file, which is
the prompt's own observation and the reason it asked.

Checked rather than assumed: nothing in the tree names
`sim-emit-hl7.emit` or `sim_emit_hl7/emit.clj`, in either spelling. The
one near-match is `ehrt.sim-emit-hl7.emitter-order-independence-test`,
a different name. `(:require [ehrt.sim-emit-hl7.emit :as emit])`
followed by `(def emit emit/emit)` is legal and is not novel: aliases
and interned vars live in different maps, and `engine.clj` has carried
`[ehrt.sim-engine.run :as run]` with `(def run run/run)` since
2026-08-30.

## 1. Step 1 -- the derivations

### Three forms, 136 form-lines, TWO regions

Derived at `0bd9ddc` with a char-level scanner for every top-level
form's true span, and a whole-symbol scan over each form's body with
string literals, character literals and line comments stripped.

| form | span | lines | marker |
|---|---|---:|---|
| `default-providers` | 263-269 | 7 | `def` **`^:private`** |
| `emit` | 271-303 | 33 | `defn` **public** |
| `emit-wire` | 363-458 | 96 | `defn` **public** |

**136 form-lines, census 2a's figure to the line** -- the EIGHTH cluster
running where 2a is right and section 2's own figure (151) measures
something else.

**TWO REGIONS, and NO BANNER TRAVELS.** `default-providers` and `emit`
stood between the `event->messages` delegating def and the `planners`
banner; `emit-wire` stood below the four planner defs. Lines 260-263 and
357-363 were read line by line: each region is preceded by a blank line
and a `def`, not by a comment block. `segments` (cluster 5) was the only
prior cluster to travel with no banner, and it travelled with none
because it SPLIT four sections whose builders stayed; this one travels
with none because its two regions never had one.

**A CENSUS AND RECORD CLAIM CORRECTED, and it is the first derivation
this session ran.** Census section 2 lists `def default-providers` and
the seventeenth session's record calls it "a public `def` with NO caller
anywhere in the tree outside this file". The second half holds -- the
token `default-providers` appears in exactly four files, and only
`emit_hl7.clj` is code -- but **it is `^:private` and has been for as
long as the scanner can see it at this sha**. The correction matters
because it decides the cluster's whole disposition: a public mover with
no caller would still have been a judgment call under C1(a), while a
private one is settled by constraint 5's prohibition with nothing to
weigh.

### The edges: 3b's four facade rows reproduce EXACTLY

**OUTGOING**, by distinct (caller, callee) pair, which is 3b's own
accounting:

| callee cluster | pairs | which | census 3b |
|---|---:|---|---:|
| `messages` | 4 | `event->messages` x2, `chatter-message`, `ladder-message` | 4 |
| `timelines` | 3 | `demographics-timeline` x2, `encounter-spans` | 3 |
| `hl7-time` | 2 | `default-utc-offset`, `transmit-seconds` | 2 |
| `segments` | 1 | `control-id-for` | 1 |

**All four rows EXACT, and 10 distinct pairs against 11 raw sites** --
the single divergence is `hl7-time/transmit-seconds`, which `emit-wire`
names twice. Cluster 7 was the first where pairs equalled sites; this
one is off by one, and the reason is visible in the form: a planner
reads a table once, while `emit-wire` computes a transmit instant on two
different lanes.

**INCOMING: ZERO**, derived rather than trusted -- every one of the
twenty-four residue forms was scanned for `emit`, `emit-wire` and
`default-providers` in code positions, and not one hit. Census 3b's
ABSENCE of a `facade`-as-callee row is confirmed by measurement.

**INTERNAL: two edges**, both `emit` -> `default-providers`, which is
why the private mover travels without widening.

The cluster also names `ehrt.sim-model.interface` at five sites
(`materialize-providers`, `default-provider-templates`,
`default-facility` x2, and its use inside `default-providers`), which is
not a cluster and does not appear in 3b.

### THE CALLER TRAVELS -- the census's own open question, answered

Census 2a: "The caller-travels hazard does not arise until the end ...
The analog here arises only if the `facade` cluster itself ever moves,
which is an OPEN question this order does not settle." C11(a) settled
it, and this is the measurement the prompt asked for in advance.

**FOUR requalifications over THREE names**, named before the move:

| bare name | sites | becomes |
|---|---:|---|
| `event->messages` | 2 | `messages/event->messages` |
| `default-utc-offset` | 1 | `hl7-time/default-utc-offset` |
| `control-id-for` | 1 | `segments/control-id-for` |

The rewriter -- a character state machine tracking in-string,
in-character-literal and in-comment state, rewriting in CODE positions
only -- then produced 2/1/1 INDEPENDENTLY and reported its own hits:
**TOTAL 4**, the same three numbers twice from two different programs,
and **not one word of prose moved with them**.

**The class falls again, 64 -> 7 -> 4**, and the deepest cluster in the
file pays the least. Depth is not the whole story (cluster 7 established
that), and neither is prior qualification: what makes this one cheap is
that `emit` and `emit-wire` were ALREADY calling five of their seven
sibling clusters through qualified names, because those siblings had
landed underneath them one at a time over the seven prior sessions.

**NO SHIM IS NEEDED, and the absence is derived rather than hoped for.**
The prompt was explicit that a cycle would be a stop-and-report. The
engine's caller-travels move had to pay one, because census constraint 1
requires `run`'s four `stream` call sites to keep resolving to the var
`engine_test/mutating-one-patients-stream-seed-moves-only-that-patient`
perturbs by `with-redefs`, and an implementation may not require its
facade. Nothing here has that shape:

* **108 `#'` sites** in the tracked tree, re-read whole (the seventeenth
  counted 108 too; the tree has not grown a var-quote since). Filtered
  against `emit-hl7` and against all three mover names: **ZERO**.
* Widened to `resolve`, `ns-resolve`, `requiring-resolve`,
  `with-redefs`, `alter-var-root`, `intern`, `find-var` and `(var ...)`
  across `.clj`/`.cljc`/`.edn`: **also zero**. The only `with-redefs`
  forms in this component's test tree target
  `patient-simulator/compile-trajectory` and `/run-module`.

So **no C7 def is owed**, and the fifteenth session's prediction --
"`messages`, `planners` and `facade` owe no `^:private` def unless a
test file changes" -- holds for the third and last of the three. The
class is EXHAUSTED, and the emit phase ends having added two `^:private`
defs in total (`tn-field`, cluster 4; `msh-segment`/`pid-segment`,
cluster 5) and none after.

### The two `interface.clj` re-exports, verified as the prompt asked

The channel expected `emit` and `emit-wire` to be the brick's
load-bearing entry points. **Confirmed, read out of `interface.clj`
rather than out of the census.**

* `emit` (`interface.clj:28-34`): a `defn` with THREE arities (3, 5, 6),
  each calling `emit-hl7/emit` at RUNTIME. The 2-arity is deliberately
  unexported, which that file's own `ns` docstring records.
* `emit-wire` (`interface.clj:54-61`): a `defn` with TWO arities (7, 8),
  both calling `emit-hl7/emit-wire` at RUNTIME.

So the chain `ehrt.sim.run` -> `interface.clj` -> the delegating def ->
`emit.clj` must hold at every link, and C1(a) fences `interface.clj`
from edits. Census 2a's "`facade` 2" is exact.

**Each def is owed several times over besides**, and one of the debts is
unlike anything the seven prior clusters carried:

| caller | sites | why it matters |
|---|---:|---|
| `emit_hl7_test.clj` | 61 | the single largest test debt of the emit phase |
| `components/oracle/src/ehrt/oracle/digest.clj:228` | 1 | **the regression oracle's own instrument calls the moved form** |
| `sim/test/ehrt/sim/run_test.clj:169`,`:281` | 2 | namespace claims, one per mover |
| `sim-engine/src/ehrt/sim_engine/run.clj:1015` | 1 | a live `src` namespace claim |
| the six `sim-emit-hl7` add-on test files | 35 | `emit-wire` at charges/chatter/ladders/latency/result-clock/siu |

`digest.clj:228` is worth its own line. The tenth extraction found that
EDITING that file aborts the bracket's soundness check; this session
found that the file also CALLS the form being moved. It needed no edit,
because the delegating def keeps `emit-hl7/emit` resolving -- but if
C1(a) had not been in force, the instrument and the thing it measures
would have had to move in the same commit.

### Markers and dispositions, named in advance

* **TWO public delegating defs, C1(a)**: `emit`, `emit-wire`.
* **ZERO widenings.** No private mover becomes public anywhere.
* **ZERO `^:private` defs**, and it is a closure rather than an absence
  -- see the `#'` census above.
* **`default-providers` stays `^:private` and gains no def.** Both its
  callers are `emit`'s own lower arities and both travel. Constraint 5's
  prohibition reaches **one of one**, after cluster 7's six of six.
* **ZERO MARKER CHANGES**, the second cluster running and the second in
  the program.

### The requires, derived in BOTH directions

`emit.clj` needs FIVE and uses all five: `messages` 4, `timelines` 3,
`hl7-time` 3 sites, `segments` 1, `sim-model` 5. It needs no `registry`,
no `er7`, no `planners`, no `clojure.string`, no HL7 parser and no
`:import`.

`emit_hl7.clj` **loses TWO and gains one**, checked per alias in the
residue rather than assumed: every `timelines/` site (3) and every
`sim-model/` site (5) in this file was inside a mover, so both requires
go DEAD; `emit` is added for the two delegating defs. Eight aliases to
seven, and it is the first move in either file to kill TWO requires at
once (cluster 5 killed two as well -- `clojure.string` and
`site-profile` -- so it is the second, and the class now has four
members in this file).

## 2. Step 2 -- the sweep, which owes NO repoint

**No sweep commit precedes the move, and the absence is derived rather
than assumed.** Every level ran and every level is reported. No red was
predicted and none occurred. Unlike the seventeenth, this sweep owes not
even a deferred repoint: every path and namespace claim about a mover
stays TRUE, and C1(a)'s defs are the whole reason.

### 2a. Both charter registers, hand-read

**ZERO of the three movers is named in either register.** Counting
path-like tokens with the seventeenth's own rule
(`[A-Za-z0-9_./-]+\.(clj|md|edn|json|svg)`, DEDUPLICATED): **17** in
`components/patient-simulator/docs/limitations.md` and **12** in
`components/person-simulator/docs/limitations.md` -- the seventeenth's
figures to the token, and neither file has been edited since.

The one `emit_hl7`-shaped token in either register is still the
component DIRECTORY inside
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/registry.clj`, the
twelfth session's own repoint, which still resolves. Level EMPTY.

### 2b. Level 1 -- 733 shingles, SIX hit lines, zero positional

The three movers' docstrings and `;;` comments cut into **733 distinct
six-word shingles** and searched across the whole tracked tree with a
single `git grep -F -f`. **Six hit lines across six files outside
`emit_hl7.clj` itself** -- by far the smallest level-1 result of the emit
phase, against the seventeenth's 162 across 25 -- and not one is a
positional claim about a mover.

| site | class |
|---|---|
| `.agents/prompts/2026-08-11-latency-second-clock.md:54` | session-marker phrase (`emit-wire`'s own signature line) |
| `.agents/session-records/2026-08-28-arc-4-sweep-3-status-ladders.md:187` | session-marker phrase |
| `.agents/session-records/2026-08-31-emit-extraction-segments.md:247` | session-marker phrase |
| `interface.clj:55` | LIVE surface -- `emit-wire`'s signature, copied into the re-export's own docstring; C1(a)-fenced, and a signature rather than a positional claim |
| `chatter_test.clj:255` | doctrine echo in a TEST file, C1(a)-fenced |
| `ladders_test.clj:81` | doctrine echo in a TEST file: "`emit`'s exact order and therefore its exact bytes", which the move does not touch |

The small number is structural rather than lucky. Seven prior clusters
have already taken every sentence of this file that a later session
would echo; what is left is two docstrings that describe a SIGNATURE,
and a signature is quoted in exactly the places that re-export it.

### 2c. Level 2 -- paths and namespaces, and NOTHING GOES FALSE

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<mover>`,
checked per mover rather than in aggregate: **FIVE**, and **ZERO go
false**, because every one names a public mover that keeps a delegating
def.

| site | names | disposition |
|---|---|---|
| `interface.clj:57` | `emit-wire` | FENCED from edits, and TRUE -- the def keeps it resolving |
| `sim-engine/src/ehrt/sim_engine/run.clj:1015` | `emit` | live `src`, and TRUE for the same reason. **No repoint owed** |
| `sim/test/ehrt/sim/run_test.clj:169`,`:281` | `emit`, `emit-wire` | test files, C1(a)-fenced, and TRUE anyway |
| `docs-tooling/test/.../stale_path_test.clj:368` | `emit` | a PATTERN FIXTURE, not a resolution against the tree: it asserts that the string `"see ehrt.sim-emit-hl7.emit-hl7/emit"` trips no retired-prefix rule. Unaffected in either direction |

**PATH claims naming `emit_hl7.clj` about a mover: THREE, all in dated
ADRs, all ALREADY STALE, none this session's.** Standing treatment.

* `notes/adr/0171:182` -- `` `emit_hl7.clj:908` `default-providers` `` in
  the RNG-partition table. `default-providers` sat at `:263`.
* `notes/adr/0173:94` -- "through `emit` (`emit_hl7.clj:940`),
  `emit-with-offsets` (`:1016`)". `emit` sat at `:271`, and
  `emit-with-offsets` has not existed under that name since ADR-0109.
* `notes/adr/0172:81` -- `emit_hl7.clj:302`, about
  `personas-by-patient-id`, which is not a mover and no longer exists.

Named here so the next reader does not rediscover them, exactly as the
seventeenth named `notes/adr/0171:181`.

**ALIAS-qualified prose** `emit-hl7/<mover>` outside the file: all
resolve. `fan_out.clj:7` names `emit-hl7/emit-wire` in a docstring;
`sim/run.clj:14` names `emit-hl7/emit`; `sim/run.clj:816` CALLS
`emit-hl7/emit` through the INTERFACE alias it declares in its own `ns`.
Every one is kept true by a def.

### 2d. Level 3, the rest

* **`hand-owned-assets.edn`.** All rows read and all four distinct
  sources read: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/palgebra-design.md`,
  `components/corpus/docs/pipeline.edn` and
  `demos/scenarios/ed-tuesday/README.md`. **TWO OF THE FOUR NAME A
  MOVER** -- `simulator-architecture.md` at `:344`, `:347`, `:397`,
  `:406`, `:429`, `:430` and `ed-tuesday/README.md` at `:361`, `:413`,
  all `emit-wire`, as a bare name or as `emit-hl7/emit-wire`. Every one
  is kept TRUE by the delegating def; neither file carries an
  `emit_hl7.clj` path claim about a mover. **Nothing was edited, no
  `:reviewed-at` is bumped, and the tripwire does not fire -- the NINTH
  session in the program to read them and fire nothing, and the SECOND
  to do so while the sources actually named a mover.** (Disclosed, and
  pre-existing: that file's `docs/dev/simulator-architecture.md` row at
  `:629` already carries `:verdict :stale` from the two-clocks asset
  field audit. Untouched.)
* **Docstring-as-authority (census constraint 2).** Every backtick-cited
  token inside the moved text was enumerated and resolved:
  `docs/site-profiles.md`, `check.clj`,
  `ehrt.sim-engine.engine/config-keys`, `ehrt.sim-engine.engine/run`,
  `ehrt.sim.run`, `message-type-registry`, `event->messages`,
  `plan-latency`, `plan-ladders`, `sim-engine`, ADR-0109 and ADR-0175.
  **All resolve.** `engine/config-keys` and `engine/run` both survive
  because `engine.clj`'s own facade re-exports them -- confirmed by
  reading that facade, which is the check the seventeenth ran and where
  `stamp-encounter` failed it.
* **The CONVERSE check, and it is why the two delegating defs are
  BARE.** No live surface anywhere names `emit`'s or `emit-wire`'s own
  DOCSTRING as authority: every "own docstring" citation in the tracked
  tree was enumerated and none is theirs. Constraint 2 therefore does
  not fire, and the defs follow this file's own established style --
  twenty-four bare one-line defs, including the four the seventeenth
  wrote for forms `interface.clj` re-exports with "See
  ehrt.sim-emit-hl7.emit-hl7/plan-..." pointers. `engine.clj` chose the
  other convention and gave its 43 defs docstrings; both are recorded
  in the roadmap's Done entry as a visible difference rather than left
  for someone to notice.
* **The un-re-depthed `docs/` family does NOT travel.** The moved text's
  one `docs/` citation is `docs/site-profiles.md`, and that file really
  is at `docs/site-profiles.md`. Two members of the family DO sit in the
  residue and stay there, in `emit_hl7.clj`'s own `ns` docstring:
  `docs/sim-theory.edn` and `docs/operational-models.md` both live under
  `components/sim/docs/`. Backlogged, not this move's, and named because
  the facade's `ns` docstring is now the file's entire content besides
  defs and banners.

### 2e. PRE-EXISTING FALSE CLAIMS: none travels

`rulings.md#R-move-not-improve` is why none is fixed. The seventeenth's
four stand unchanged and **this move relocates none of them**: the moved
text cites no `engine/stamp-encounter`, carries no un-re-depthed `docs/`
path, and is not `emit_hl7_test.clj`. The class the seventeenth opened
(a member reaching MOVED TEXT) does not recur.

### 2f. THE RESIDUE-CLAIM CLASS FIRES A FOURTH TIME -- three claims, and the two-places sub-class again

Every count re-derived from the tree.

| where | claim | why it goes false | now |
|---|---|---|---|
| `hl7-time` banner `:58` | "`emit-wire` alone still names `hl7-time/transmit-seconds`, twice" | `emit-wire` leaves | retold in the past tense, with "no form in this file names it now" |
| `planners` banner `:351-356` | "This move leaves NO dead require: `registry`, `timelines` and `segments` all keep in-file uses ... the three forms that remain -- `default-providers`, `emit` and `emit-wire` -- are census 2's `facade` cluster exactly" | `timelines` loses its last in-file use to THIS move, and nothing remains | retold in the past tense throughout, ending "and cluster 8 took them too" |
| `planners.clj:6` (`ns` docstring) | "`emit-wire`, which stays behind, is what renders them" | it does not stay behind | "which stayed behind at this move and left in cluster 8 for `ehrt.sim-emit-hl7.emit`" |

**The two-places sub-class, confirmed a third time**, and with a
difference worth naming: cluster 6 and cluster 7 each found the twin of
a RESIDUE BANNER in a sibling's `ns` docstring. Here the falsified
sentence is in the sibling's docstring and has **no twin in the residue
banner** -- the `planners` banner in `emit_hl7.clj` never said
`emit-wire` stays behind. So the class is not "banners are duplicated";
it is "a cluster's account is written in two places and the two are not
copies", which is harder to sweep for, because a grep tuned to one
wording misses the other.

**Checked and LEFT, with the reason stated rather than assumed:**

* `registry` banner `:87` and `registry.clj`'s twin -- "`sch-segment`,
  `event->messages` and `plan-charges` still call them". The seventeenth
  named this for this session, "which may want to retire it". It is
  still TRUE: the sentence is phrased in FORMS, and all three call those
  tables from wherever they now live. **A true sentence is not a
  correction's business** (`rulings.md#R-move-not-improve`), and
  retiring it would be an improvement rather than a repair, so it
  stands. The strain the seventeenth flagged is real and unchanged; it
  belongs to the repoint pass along with the defs.
* `messages` banner `:244-245` and `messages.clj:46`'s twin --
  "`chatter-message` and `ladder-message` widen ... because `emit-wire`
  stayed behind and calls both". Past tense, an account of the reason at
  CLUSTER 6's move, and still accurate; the two call sites travel with
  `emit-wire` and still name them `messages/...`. SURVIVES, in both
  places.
* `messages` banner `:219` and `planners` banner `:310` -- "`ladder-
  message` from just above `emit-wire`", "everything between `emit` and
  `emit-wire`". The historical-landmark class, read and left for the
  third time: past-tense accounts of where a form sat at ITS OWN move,
  accurate as history even though neither anchor is in the file now.
* `messages` banner `:257-259` -- "`emit_hl7.clj` built no message text
  of its own after that move -- it planned, and delegated the rendering.
  Cluster 7 took the planning too, so what it does now is neither: it
  delegates both." SURVIVES, and this move makes it MORE true.
* `segments` banner `:129-131` -- `control-id-for`'s def "owed twice
  over". Still true (`interface.clj` plus the test tree). The
  seventeenth's added sentence about `emit-wire:806` being the only
  in-file caller is NOT in the banner and needed no correction.
* `planners.clj:84` -- "(`emit_hl7.clj`'s own renders-only doctrine,
  restated in that same section)". SURVIVES: that doctrine is stated in
  `emit_hl7.clj`'s `ns` docstring, which stays.

### 2g. Claims INSIDE the moved text: ZERO go false -- a program first

Every positional word in the moved text was enumerated mechanically --
"below", "above", "this file", "this namespace", "this section", "here",
"earlier", "later", "elsewhere". **SEVEN occurrences, and NOT ONE goes
false**, so unlike clusters 5 (two corrections) and 7 (three) this move
corrects no sentence inside the text it moves. The moved body is
verbatim but for the four requalifications.

| site | word | why it survives |
|---|---|---|
| `:290` | "elsewhere ... in this namespace" | the referent is ADR-0109's own additions, and `emit-wire` -- the one of them this text points at -- travels into the SAME new namespace |
| `:291` | "`emit-wire`, below" | relative order is preserved: `emit-wire` is still below `emit` in `emit.clj` |
| `:371` | "a later event's own" | not positional about the file |
| `:381` | "this namespace's own renders-only doctrine" | **the one that needed a decision.** See below |
| `:387` | "the seven-argument arity below" | scopes inside its own form |
| `:404` | "unlike the three above" | scopes inside its own docstring |

`:381` is the sentence cluster 7 met three times and rewrote three
times: "sampling stays OUT of THIS NAMESPACE", where the namespace meant
was the emitter and `planners` is not it. **Here the opposite holds, and
the difference is substantive rather than convenient.** `planners`
plans and renders nothing, so the renders-only doctrine was never its;
`emit` and `emit-wire` render and take no RNG at all, so the doctrine is
precisely and only theirs. `emit.clj`'s own `ns` docstring therefore
STATES it -- "no RNG, no wall clock -- which is THIS NAMESPACE'S OWN
RENDERS-ONLY DOCTRINE" -- and the moved sentence is true where it lands
without a word changing. **Disclosed as a judgment call**, because the
alternative reading (rewrite it to "the emitter's", as cluster 7 did)
was available and was not taken: writing a docstring that makes a moved
sentence true is only honest if the docstring is itself true, and this
one is checkable -- neither form takes an RNG argument.

### 2h. Fenced citations: ZERO ADDED

The backlog stands at FOUR rows: the thirteenth's
(`person-simulator/limitations_test.clj:152`) and the sixteenth's three
(`siu_test.clj:11`, `siu_test.clj:72`, `sim/siu_run_test.clj:106`). No
test file makes a claim about a mover that this move falsifies, for the
reason 2c gives: both public movers keep defs, and the private one is
named in no test file at all. The emit phase ends where it stood.

## 3. Step 3 -- `ehrt.sim-emit-hl7.emit` (`bee0d69`)

### The moved body diffs as FOUR lines, and no others

Verified by diffing the moved text as a BLOCK against `0bd9ddc`'s own
`emit_hl7.clj` -- **137 lines before, 138 after** -- not inferred from
hunk headers. All four are the requalifications. The one added line is a
BLANK: the two source regions were separated by 59 lines of banner and
defs and are now adjacent, so one separator stands between `emit`'s
closing paren and `(defn emit-wire`. **Not one docstring, comment or
other code line differs.**

**ZERO MARKER CHANGES**, the second cluster running: `default-providers`
stays `def ^:private`, `emit` and `emit-wire` stay `defn`.

### The residue's non-comment changes are FIVE lines

Read off `git diff -U0`: three `:require` lines (the block loses
`sim-model` and `timelines`, gains `emit`) and the two delegating defs.
Every other addition in the residue is a `;;` line -- the two banner
corrections of 2f and the new 57-line moved-to banner.

### The dispositions, asserted live under `-M:dev` rather than argued

* `emit` has **3 interns and 2 publics**; `ns-interns` minus
  `ns-publics` is exactly `default-providers`.
* **Constraint 5's prohibition, asserted by name**:
  `emit-hl7/default-providers` fails to resolve.
* `emit` and `emit-wire` both resolve in `emit-hl7`, are PUBLIC, and are
  each `identical?` to their `emit.clj` var's value.
* **`interface.clj` still resolves, 20 publics**, and both entry points
  answer end-to-end on an empty log THROUGH the interface -- `emit` at
  its 3-arity and `emit-wire` at its 7-arity, each returning `[]`.
* `emit_hl7.clj`'s public surface is **23 vars before and 23 after** --
  a program first. Every prior cluster's residue surface moved; this one
  loses two publics and gains defs for exactly those two.
* `emit` holds **exactly five** namespace aliases and `emit-hl7`
  **seven** (`emit`, `er7`, `hl7-time`, `messages`, `planners`,
  `registry`, `segments`), both read off the LOADED namespaces rather
  than the source.

`clojure -M:poly check` **OK**, run twice -- before and after the
banner's own edge-count correction.

### The partition CONSERVES ITS POPULATION TO THE FORM

Measured across all eight extracted namespaces with the same scanner:

| namespace | forms | form-lines | file lines |
|---|---:|---:|---:|
| `hl7-time` | 7 | 47 | 73 |
| `registry` | 13 | 278 | 324 |
| `timelines` | 5 | 151 | 188 |
| `er7` | 19 | 193 | 298 |
| `segments` | 15 | 518 | 594 |
| `messages` | 13 | 578 | 697 |
| `planners` | 11 | 364 | 508 |
| `emit` | 3 | 136 | 206 |
| **total** | **86** | **2,265** | |

**86, which is census section 2's own count of `emit_hl7.clj`'s top-level
forms at `517a96d`, exactly.** Nothing was left out and nothing was
invented across eight sessions. The facade itself holds 26 forms, all of
them defs the extraction wrote.

### `emit_hl7.clj` IS NOW A PURE FACADE

**383 lines, 27 top-level forms: the `ns`, and 26 delegating defs whose
form-lines total 26 -- one line each -- and no executable code of its
own.** Seven explanatory comment blocks, one per cluster.

**NOT ONE of the 26 defs has an in-file caller.** The seventeenth left
four with one (`default-utc-offset`, `control-id-for`,
`event->messages`, and `hl7-time/transmit-seconds`' two qualified
sites); all four were inside the three movers, so the count goes 20 of
24 to **26 of 26**. That is what a pure facade means measured rather
than asserted, and it is the retirement inventory's whole emit half.

### The two facades, side by side

|  | `engine.clj` | `emit_hl7.clj` |
|---|---:|---:|
| ruling | C4(b) | C11(a) |
| lines | 741 | 383 |
| top-level forms | 44 | 27 |
| delegating defs | 43 | 26 |
| def form-lines | 268 | 26 |
| namespaces in the partition | 11 (202 forms) | 9 (86 + 26 forms) |
| shim owed | one, `stream`, in `ehrt.sim-engine.run` | none |

The def form-line column is the visible difference and each is its own
file's established style: `engine.clj` gives every def a "Delegates to
..." docstring, `emit_hl7.clj` gives none, and 2d records why the second
is safe here.

## 4. Steps 3-4 -- the gates

`bin/regression-oracle 0bd9ddc bee0d69`: **IDENTICAL: every root's
digest matches**, **41 roots**, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** -- the eleventh session's finding
-- and it is load-bearing here in the sharpest way of the eight: `emit`
is the function `digest.clj` calls to produce the `:hl7` half of every
root. A wrong requalification would not have moved one message; it would
have failed to load, or moved every message in all 41. Nothing moved.

`bin/ground-truth-bracket 0bd9ddc bee0d69`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0.

**What the bracket can see here: nothing, and its own output says so.**
It prints "THIS IS NOT A REGRESSION-ORACLE CLAIM: the `:hl7` half of
every root is excluded by construction", and the `:hl7` half is the
entirety of what these three forms produce. What it proves is the
NEGATIVE that still matters -- that the move did not reach across the
component boundary and perturb ground truth. Reported because it was
asked for, not because it discriminates. This is the instruments-swap
finding of the eleventh session at its limit: over the whole emit phase
the bracket has been the weaker instrument, and on the last cluster it
is blind entirely.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj` -- which is notable
precisely because that file CALLS `emit-hl7/emit`, and C1(a)'s def is
the only reason it needed no edit.

### The suite delta, measured IN-CLONE -- and the method itself produced a finding

Both runs `MAKE_EXIT=0`, no red at any point.

**BASELINE at `0bd9ddc`**, taken in a disposable `git worktree` of this
clone rather than carried from the seventeenth's record: **408
zero-failure blocks over 216 distinct namespaces / 4,751 tests / 24,155
assertions**. **AFTER, at `bee0d69`** in the clone itself: **408 / 216 /
4,751 / 24,161**. The blocks, namespaces and tests are equal, so **this
move adds no `deftest`**, confirmed by differencing the namespace sets,
which are equal in both directions.

**The raw total delta is +6, and the prediction was +4. The two extra
assertions are an artefact of the baseline METHOD, not of the move**, and
the per-namespace difference is what proves it -- three namespaces move
and no others:

| namespace | assertions | tests | why |
|---|---|---|---|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | 276 -> 278 | 6 -> 6 | `doseq` over `components/*/src` + `bases/*/src` `.clj`, 129 -> **130** |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | 36 -> 38 | 6 -> 6 | `doseq` over `components/sim-emit-hl7/src` `.clj`, 12 -> **13** |
| `ehrt.sim.version-test` | 10 -> 12 | 10 -> 10 | **the method, not the move** -- see below |

The first two are the predicted +4 exactly: each gate runs in both
`conformance` and `ehrt-cli`, so 2 x (1 + 1) = 4, and the populations
were counted before the move rather than reasoned about after it.
`emit.clj` is the one new production source file and it passes the
dependency gate on its merits -- four of its five requires are
intra-component siblings and the fifth is `ehrt.sim-model.interface`,
which that gate's allow-list names explicitly.

**A `git worktree` IS NOT A VALID SUITE BASELINE without this
correction, and the mechanism is exact.** `ehrt.sim.version/git-sha`
reads `(io/file ".git" "HEAD")` directly, deliberately without a
subprocess (`version.clj:30`). In a linked worktree `.git` is a FILE
(`gitdir: ...`), not a directory, so `.git/HEAD` does not exist,
`git-sha` returns `nil`, and
`generator-sha256-is-not-the-all-zero-placeholder-when-git-is-present`
-- whose single assertion sits inside `(when (version/git-sha) ...)` --
contributes ZERO instead of one. One per project, **two in all**.
Confirmed by reading both the guard and the worktree's own `.git`, which
is a 66-byte file.

**So the baseline in a normal checkout is 24,155 + 2 = 24,157**, which is
the seventeenth session's own closing figure reproduced to the
assertion, and the delta attributable to this move is **+4, measured
equals predicted**. The per-namespace table is the stronger evidence in
any case: it shows the move touched the two gates it was predicted to
touch and nothing else, which a matching total alone would not.

**This is a NEW hazard and it is named so the next session does not
rediscover it.** It is the same family as the tenth extraction's finding
that a sweep editing `components/oracle/src/ehrt/oracle/digest.clj`
makes the bracket abort its own soundness check: an INSTRUMENT can be
perturbed by the technique used to run it. `bin/regression-oracle` and
`bin/ground-truth-bracket` are unaffected -- neither digests anything
`version.clj` touches, and both were IDENTICAL here -- but a suite
baseline taken the same way owes this two-assertion correction, stated
rather than silently absorbed.

**DISCLOSED, and it is a method note rather than a finding.** The
post-move run above was taken at `bee0d69`, before the docs commit
existed; the run reported in section 8 is the one taken on the pushed
tree. No gate reads this record's or the prompt archive's CONTENT: the
prompt/record pairing gate was deleted by `e189418`, the index gates
count directory entries, and the two gates that moved glob `src` paths
only. `make state-derived` ran AFTER the roadmap edit and BEFORE the
final suite, which is the order the sixteenth session's own disclosure
prescribes.

**No red was seen at any point.** In particular
`ehrt.corpus-io.mllp-test` -- the ephemeral-port flake this session rows
-- ran green in both projects in both runs.

## 5. Closing arithmetic

### The partition, cluster 8 of 8

At the move commit `bee0d69`, `emit_hl7.clj` goes 458 lines to **383**
and 27 def-forms to **26** -- it lost three and gained two -- and
`emit.clj` is **206** lines / 3 forms plus its `ns`.

Arithmetic, checked rather than asserted: 458 - 137 moved - 2 blanks
lost + 1 net `:require` line lost + 2 defs + 57 banner lines + 1 blank +
2 net banner-correction lines + 1 blank = **383**, which is `wc -l` on
the file at that commit.

### Census corrections

* **Section 2's `facade` row says "3 forms, 151 lines"; the forms are 3
  and the FORM-lines are 136**, which is section 2a's own figure,
  confirmed to the line. **EIGHT clusters running** where 2a is right
  and section 2's figure measures something else -- the span-to-next-
  form measure 2a itself diagnosed. The census's own model is now
  vindicated on every cluster of the file.
* **Section 2 lists `default-providers` as a bare `def`; it is
  `^:private`**, and the seventeenth's record calls it public. Both
  corrected here. This is the FIRST census marker error the emit phase
  has found, against several span errors.
* **Section 3b's FOUR `facade` rows all reproduce EXACTLY** --
  `messages` 4, `timelines` 3, `hl7-time` 2, `segments` 1 -- and its
  ABSENCE of a `facade`-as-callee row is confirmed by measurement. With
  this cluster, **all sixteen of 3b's edge counts have now been
  re-derived from the tree by the session that moved them, and all
  sixteen held.**
* Section 2a's caller-travels OPEN QUESTION is answered: the caller does
  travel, and it costs FOUR requalifications and no shim. 2a was right
  that the hazard could only arrive last and right that it was the
  author's to settle.
* **New, and not in the census**: a cluster can pay its caller-travels
  bill in requalifications ALONE. The engine's shim was forced by a
  test's `with-redefs`, not by the shape of the move, so "the caller
  travels" and "a shim is owed" are independent facts -- the census
  models the first and the constraint list models the second, and
  nothing connects them.
* **New, and not in the census**: the extraction's OWN INSTRUMENT can be
  a caller of the moved form. `digest.clj:228` calls `emit-hl7/emit`;
  the census's cross-seam model does not reach outside the file, and the
  constraint list names `digest.clj` only as a file a sweep must not
  EDIT.

### The backlogs, as the extraction phase hands them on

* **FENCED CITATIONS: ZERO added.** Four rows, unchanged.
* **RETIREMENT CANDIDATES**: `emit_hl7.clj`'s inventory closes at **26
  of 26** -- every delegating def in the file now lacks an in-file
  caller. Four gained that status at this move. All correctly kept by
  the facade rule.
* **STALE-BEFORE-THE-MOVE**: unchanged at four rows, none of them this
  cluster's and none relocated by it.
* **DELETED GATES STILL CITED**: unchanged at five, two of them in
  `state_derived.clj`'s own `src` half.

All four are now recorded on the LIVE P5 row, grouped under the three
headings the prompt named, so that the repoint pass reads one row rather
than seventeen records.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt's census summary "-> registry?" does not resolve.** The
   `facade` cluster has NO edge into `registry`: 3b gives it four rows
   and `registry` is not among them, confirmed by scanning all three
   movers for all thirteen `registry` names -- zero. The prompt's
   question mark was the right instinct.
2. **The prompt (via the seventeenth's record) called `default-providers`
   public.** It is `^:private`. Section 1 states the measured version.
3. **The prompt's "3 forms, 136 form-lines" is exact**, and is section
   2a's figure rather than section 2's.
4. **The requalification count was named in advance** (3 names, 4 sites)
   and reproduced independently by the rewriter.
5. **No cycle appeared**, so the prompt's stop-and-report branch was not
   taken; the absence is derived in section 1 rather than assumed.
6. **The moved text is NOT byte-identical**, and the deviation is named:
   four requalifications, licensed by the prompt, and one blank
   separator. Zero prose corrections -- a first.
7. **The ruling label "C5"** in the prompt's step 5 resolves to no other
   appearance in the tracked tree; the repoint pass is chartered in the
   roadmap and the census as "the ruled repoint pass" without a letter.
   The new row uses the prompt's own label and names the pass, so a
   reader who has the ruling can match it and one who does not still
   knows what is owed. Named as a deviation because the alternative was
   to invent a citation.
8. **A CLAIM OF THIS RECORD'S OWN WAS WRONG AND IS CORRECTED HERE
   RATHER THAN QUIETLY.** Section 6 first said the P5 slug is cited by
   "eight live surfaces", a number taken from the shape of
   `roadmap-lint`'s own include-list rather than measured. Measured, it
   is 41 files, and a plain `git grep` sees only 33 of them. The
   corrected sentence carries both numbers and the reason for the gap.
9. **`.agents/state-derived.md` shows `:docs` at headroom -2** (787
   against a 785 budget), and it is NOT this session's: no path in that
   set was touched, and the condition predates this session's first
   commit. Named because `rulings.md#R-budget-stop` makes an over-budget
   set a stop-or-compact for whoever owns it, and nothing in the tree
   escalates this one.

## 6. The close-out, step 5

### (i) The Done migration

The C8-flagged migration executes here. `roadmap.md` gains
**`roadmap.md#engine-emit-namespace-extraction`**, one line in the
`## Done` section per this file's own rotation -- the shape every
closure since 2026-08-29 has taken -- carrying eighteen landings, both
facades' final counts, the census and its corrections, the doctrine the
phase established, the caller-travels shape in both halves, the
instruments swap, the tripwire recipe, the coverage disclosure, and all
seventeen session records by name.

The LIVE P5 row keeps its slug, and the reason is measured rather than
assumed: **41 files in the tracked tree cite
`roadmap.md#engine-namespace-extraction-and-apply-unification`** -- 20
production `src` files (eleven `sim-engine`, nine `sim-emit-hl7`), the
census, `docs/consuming-ground-truth.md`, `roadmap.md` itself, and every
session record of the program. **A single-line `git grep` finds only 33
of them**: ten of the `src` citations WRAP the slug across a line break
inside a docstring, and a wrap-tolerant scan is what finds those. Named
because a future session retiring or renaming this slug would, with the
obvious grep, miss ten live surfaces.

The row slims to the unification arc: census 4a's own correction (module-compiled
and churn-injected work enters as STEPS, so unification's subject is the
three APPLY sites rather than three event sources), the staged plan as
ruled, and the three standing backlogs.

**The row goes from 97 lines to 46**, and the first sentence carries no
closure word, which the roadmap-lint DUAL requires of a row not tokened
`CLOSED`.

### (ii) The flake row

`roadmap.md#corpus-io-ephemeral-port-flake`, PRIORITY 11 -- the
seventeenth session's finding, rowed at last. It carries the mechanism,
the single sighting with its run id, the green-on-rerun evidence that
the tree was never the cause, and the reason it is a design question
rather than a one-liner.

### (iii) The facade form counts

Recorded in three places, each for a different reader: the Done entry
(both files, side by side, with the def-docstring difference named),
this record's section 3 table, and the `emit_hl7.clj` banner itself.

### Headroom

`make state-derived` re-measures `:onboarding` at **1494 of 1530, 36
lines of headroom**, from 1526 and FOUR. The whole recovery is
`roadmap.md`, which goes **394 -> 362**: the P5 row sheds 51 lines to
the Done section, where an entry of any length costs ONE, and the new
flake row spends 22 of them back. That is the rotation convention doing
exactly what it is for, and it is the first time in the program that a
close-out has ENDED with more headroom than it started.

Roadmap rows go 69 -> 71; `## Next` 7 -> 8; `## Done` 34 -> 35.

## 7. What is left in this program

The application-path unification, and nothing else. Every cluster of
both files has landed.

## 8. CI at the pushed tip -- the close marker

Recorded in this session's own last commit, after the push. See the
commit that names it.

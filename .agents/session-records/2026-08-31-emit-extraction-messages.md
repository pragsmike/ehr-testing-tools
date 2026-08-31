# Emit namespace extraction, 6 of 8: the `messages` cluster, the heaviest

Session record, 2026-08-31. HEAD at start `a1380fa`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed 2026-08-30 in ten sessions and
whose emit half has now landed six of eight clusters. Author rulings
C1(a) with its C7 extension, constraint 5 as a PROHIBITION, S1(a), and
**C8(a)** -- new this session, and taken FIRST as its own commit.

`bin/preflight` exit 0, **no findings** -- the seventh clean preflight of
the program, and for the same reason as the first six: this session's own
first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 0. Step 0 -- the C8(a) compaction (`d27460f`)

The fifteenth session closed the P5 row at **1530 of 1530, ZERO
headroom**, and escalated rather than bumped: `rulings.md#R-budget-stop`
says a session over budget compacts or STOPs, and what remained
compactable was re-triage, which
`rulings.md#R-section-retriage-is-author-judgement` makes the author's.
C8(a) is the ruling that came back, and it is narrow: the row's remaining
ENGINE-phase INSTANCE detail may become pointers at the nine engine
records; standing doctrine stays.

**Compacted, all to pointers**: the census-corrected-by-session
enumeration (which section each of the sixth, ninth and tenth corrected),
constraint 5's per-session widening counts, the facade/implementation
law's worked instance, both backlogs' per-form enumerations, and the
coverage note's per-session bracket blindness.

**Kept, in substance and mostly verbatim**: the hand-owned-asset tripwire
recipe and its green-suite-is-not-evidence corollary; the bracket-source
hazard and the bracket-the-move-alone remedy; the instruments-swap
finding; constraint 6's red-first firing; constraint 5 AS A PROHIBITION;
the facade/implementation law itself; and all three backlogs.

One line was ADDED that was not there before, naming what moved and where
it went, so the compaction is recoverable rather than merely smaller.

**Headroom, before and after, as the prompt required**: `:onboarding`
**1530 of 1530 (ZERO)** before, **1516 of 1530 (FOURTEEN)** after;
`roadmap.md` 398 -> 384 lines. Gate: `make test` `MAKE_EXIT=0`, **408
zero-failure blocks over 216 namespaces / 4,751 tests / 24,149
assertions** -- assertion-for-assertion identical to the fifteenth
session's own close, which is what a docs-only change should be, and
roadmap-lint is green inside it.

**A FINDING, found not caused, disclosed and backlogged -- and it is
larger than the one thread that led to it.** The budget C8(a) is about is
**rendered but no longer enforced**: `.agents/reading-sets.edn`'s own
header still says "`ehrt.docs-tooling.reading-set-budget-test` resolves
every path (a ghost is red), sums real line counts, and fails any set
over its own budget", and that namespace does not exist. The observable
proof was already on the page -- `.agents/state-derived.md` has rendered
`:docs` at **-2, over budget**, through every green CI run since.

Pulling that thread found **SIX deleted gate namespaces still cited by
name**, every one removed by the same commit, `e189418` (de-scaffold,
2026-08-25), confirmed by `git show --name-status`:
`attic-rotation-test`, `notes-prompts-frozen-test`,
`prompt-record-pairing-test`, `reading-set-budget-test`,
`rulings-lint-test` and `state-residue-test`.

**The shape of the finding is that the de-scaffold's prose sweep was
PARTIAL, not absent.** `.agents/rulings.md:22` was updated and is exactly
right -- "its lint ... was deleted with the freeze -- nothing enforces
the shape now". Three live surfaces were not, and still speak in the
present tense: `.agents/reading-sets.edn:3`,
`.agents/prompts/README.md:18` ("`prompt-record-pairing-test` fails the
build in either direction if the two drift apart" -- it cannot),
`.agents/reading-sets-baseline.edn:92`, plus
`state_derived_test.clj:132`'s own docstring. Citations inside
`.agents/plans/` and `roadmap-done-2026-08.md` are dated records and are
fine as history.

This matters to the design channel rather than to the emitter: three
disciplines it treats as gated -- the reading-set budget behind
`R-budget-stop`, the prompt/record pairing behind R-A, and the attic
rotation -- are conventions today. **Nothing was fixed here.** The
deletions were deliberate, the stale prose is not this session's, and
whether to restore the gates or correct the prose is a ruling, not a
sweep.

## 1. Step 1 -- the derivations

### Thirteen forms, 578 form-lines, THREE regions

Derived at `a1380fa` with a char-level scanner for every top-level form's
true span, and a whole-symbol scan over each form's body with string
literals, character literals and line comments stripped. The scanner
reproduces the fifteenth session's own closing count exactly -- **46
def-forms** in the file plus the `ns`.

| form | span | lines | marker |
|---|---|---:|---|
| `single-subject-message` | 202-248 | 47 | `defn-` |
| `bed-swap-message` | 250-281 | 32 | `defn-` |
| `bed-status-message` | 283-333 | 51 | `defn-` |
| `siu-message` | 339-404 | 66 | `defn-` |
| `merge-message` | 406-428 | 23 | `defn-` |
| `orm-message` | 434-492 | 59 | `defn-` |
| `oru-message` | 494-557 | 64 | `defn-` |
| `observation-message` | 568-596 | 29 | `defn-` |
| `diagnostic-report-message` | 606-632 | 27 | `defn-` |
| `dft-message` | 648-690 | 43 | `defn-` |
| `event->messages` | 692-774 | 83 | `defn` **public** |
| `chatter-message` | 1045-1077 | 33 | `defn-` |
| `ladder-message` | 1283-1303 | 21 | `defn-` |

**578 form-lines, census 2a's figure to the line**, and the SIXTH cluster
running where 2a is right and section 2's own two figures (549 in the
summary table, 623 in the form list) measure something else.

Only THREE regions, against the fifteenth's eight, because eleven of the
thirteen are consecutive: `202-774` is a solid mover block once
`segments` took every segment builder out of it. The other two are
`chatter-message`, between `plan-chatter` and `plan-charges`, and
`ladder-message`, just above `emit-wire`.

### FIVE BANNERS TRAVEL -- and they are the exact four the fifteenth left

The finding that most cleanly inverts a predecessor. The fifteenth
session's banner table named four comment blocks and left all four,
each because it "heads a section this cluster SPLITS" and "names a
MESSAGE type whose builder stays". The builders are what leaves now, so
every one of those sections is wholly this cluster's:

| banner | span | now heads | disposition |
|---|---|---|---|
| ARC 4 SWEEP 4: SIU^S12 | `:336-337` | `siu-message` alone | TRAVELS |
| M3: ORM^O01 + ORU^R01 | `:430-431` | `orm-message`, `oru-message` | TRAVELS |
| M5b: :observation | `:559-566` | `observation-message` | TRAVELS |
| the D1 ORC+OBR note | `:598-604` | `diagnostic-report-message` | TRAVELS |
| ARC 4 SWEEP 2: DFT^P03 | `:635-646` | `dft-message` | TRAVELS |

The fifth is not one of the fifteenth's four: the D1 ORC+OBR note heads
`diagnostic-report-message` and never headed a segment mover, so no prior
session had to rule on it. `chatter-message` and `ladder-message` carry
no banner at all.

### The edges: 3b reproduces EXACTLY in BOTH directions, all SIX rows

**OUTGOING**, by distinct (caller, callee) pair, which is 3b's own
accounting:

| callee cluster | bare-name pairs | qualified pairs | total | census 3b |
|---|---:|---:|---:|---:|
| `segments` | 31 | 31 | **62** | 62 |
| `hl7-time` | 11 | 10 | **21** | 21 |
| `er7` | 0 | 16 | **16** | 16 |
| `registry` | 12 | 1 | **13** | 13 |
| `timelines` | 0 | 10 | **10** | 10 |

**INCOMING**: `emit` -> `event->messages`; `emit-wire` ->
`event->messages`, `chatter-message`, `ladder-message`. **FOUR**, which
is 3b's `facade -> messages` row, and there is no other -- `planners`
calls no message builder, which 3b also says by having no row.

**All six rows EXACT.** 122 distinct pairs against 137 raw sites -- the
fourth divergence of the two accountings (18/19, 34/43, 66/70, now
122/137).

**INTERNAL: TWELVE edges**, and they are the whole of why this cluster
looks the way it does. `event->messages` calls all ten single-purpose
builders; `ladder-message` calls `orm-message` and `oru-message`.

`parser` (33 sites) is not a cluster, so 3b has no row for it and its
absence there is not an omission.

### TEN OF TWELVE PRIVATE MOVERS STAY PRIVATE

The direct consequence of those twelve internal edges, and the largest
application of constraint 5's prohibition in either file -- the engine's
ninth session left eighteen of nineteen, but across a bigger cluster; ten
of twelve is the highest proportion the emitter has produced.

* **ONE public delegating def, C1(a)**: `event->messages`. It is owed to
  the TREE rather than to `interface.clj`: five `emit-hl7/event->messages`
  call sites in `emit_hl7_test.clj` (`:196`, `:334`, `:371`, `:525`,
  `:544`) and one in `sim-engine`'s `bed_cycle_test.clj:145`. Both files'
  `emit-hl7` aliases were read out of their own `ns` forms rather than
  guessed -- both are `ehrt.sim-emit-hl7.emit-hl7`.
* **TWO widenings with no def**: `chatter-message` and `ladder-message`,
  because `emit-wire` stayed behind and calls both.
* **TEN stay private**: every caller of `single-subject-message`,
  `bed-swap-message`, `bed-status-message`, `siu-message`,
  `merge-message`, `orm-message`, `oru-message`, `observation-message`,
  `diagnostic-report-message` and `dft-message` travelled.
* **ZERO `^:private` defs**, and that is a closure rather than an absence
  -- see below.

### The `#'` census: the class is EXHAUSTED, as the fifteenth predicted

Re-run over the whole tree rather than trusted. **106 `#'` sites exist in
the tracked tree** (the fifteenth counted 102; the tree has grown since,
and the figure is re-measured, not carried). Filtered against `emit-hl7`
and against all thirteen mover names: **ZERO**. Widened past `#'` to
`resolve`, `ns-resolve`, `requiring-resolve`, `with-redefs`,
`alter-var-root`, `intern`, `find-var` and `(var ...)`: **also zero**,
anywhere in the tree.

So **no C7 def is owed**, and the fifteenth session's prediction --
"`messages`, `planners` and `facade` owe no `^:private` def unless a test
file changes" -- holds for the first of the three.

### SIXTY-FOUR REQUALIFICATIONS, named and counted in advance

The requalify-through-facade class, at five names and five sites in
cluster 5, is at **seven names and sixty-four sites** here. Every one is
a bare name in the moved text that resolved only through
`emit_hl7.clj`'s own delegating defs, which an implementation may not
reach:

| bare name | sites | forms | becomes |
|---|---:|---:|---|
| `hl7-timestamp` | 20 | 11 | `hl7-time/hl7-timestamp` |
| `msh-segment` | 11 | 11 | `segments/msh-segment` |
| `pid-segment` | 11 | 10 | `segments/pid-segment` |
| `control-id-for` | 10 | 10 | `segments/control-id-for` |
| `message-type-registry` | 10 | 10 | `registry/message-type-registry` |
| `siu-event-kinds` | 1 | 1 | `registry/siu-event-kinds` |
| `siu-renders?` | 1 | 1 | `registry/siu-renders?` |

The count was predicted from the census scan and then produced
INDEPENDENTLY by the rewriter, which reports its own hits: 20/11/11/10/
10/1/1, **TOTAL 64**, the same seven numbers twice from two different
programs. The rewriter is a character state machine that tracks
in-string, in-character-literal and in-comment state and rewrites in CODE
positions only, so **not one word of prose moved with them**.

There is no mystery about why the number grew: depth drives it. Cluster 5
sat under three landed siblings; this one sits under five.

### No collision

Nothing in the tree names `sim-emit-hl7.messages` or
`sim_emit_hl7/messages`, checked with an unpiped `git grep` in both
spellings before writing the file (a piped check would have reported
`head`'s exit code, not `git grep`'s). The token `messages` appears in
`emit_hl7.clj` six times and every one is the English plural in prose, so
the alias was free. No build surface enumerates this component's
namespaces, so an ELEVENTH needs no row.

## 2. Step 2 -- the sweep, which owes ONE repoint

**No sweep commit precedes the move, and the absence is derived rather
than assumed.** Every level ran and every level is reported. No red was
predicted and none occurred. The one repoint the sweep owes is TRUE until
the seam, so restating it a commit early would make it false in the
interim -- the ninth and tenth extraction's rule -- and it is paid in the
move commit.

### 2a. Both charter registers, hand-read

**ZERO of the thirteen movers is named in either register.** Counting
path-like tokens with this session's own rule (`[A-Za-z0-9_./-]+\.(clj|
md|edn|json|svg)`): 15 in `components/patient-simulator/docs/
limitations.md` and 13 in `components/person-simulator/docs/
limitations.md`. **DISCLOSED: those figures are not the fifteenth
session's 21 and 27** -- neither file has been edited since, so the
difference is entirely a counting rule, and this record states its own
rather than reproducing a number it cannot reconstruct. The material
result does not depend on the rule: the level is EMPTY.

The one `emit_hl7` token in either register is still the component
DIRECTORY inside `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/
registry.clj`, the twelfth session's own repoint, which still resolves.

### 2b. Level 1 -- 1,853 shingles over 1,543 files, 60 files hit, zero positional

The thirteen movers' docstrings and `;;` comments, plus the five
travelling banners, cut into 1,853 distinct six-word shingles and
searched across the whole tracked tree with a single `git grep -F -f`.
**376 hit lines across 60 files, and not one is a positional claim about
a mover.** The same four classes the fifteenth session named:

* **Session-marker phrases, the bulk.** "GMF coverage Wave D stage D1
  (2026-08-02, ADR-0029 P6)" and "ARC 4 SWEEP 2/3/4 (ADR-0175 ...)"
  appear in every file the corresponding stage touched --
  `sim/run.clj` carries eleven and all eleven are of this shape.
* **Doctrine echoes in TEST files**, C1(a)-fenced in any case:
  `emit_hl7_test.clj`, `siu_test.clj`, `charges_test.clj`,
  `vendored_sepsis_test.clj`.
* **Doctrine echoes in LIVE surfaces**: `registry.clj`, `er7.clj`,
  `site_profile.clj`, `v2_replay.clj`, `interface.clj`, `fan_out.clj`,
  `sim-theory.edn`, `gmf-interpreter.md`, the demo configs. All bare
  names in behavioural claims.
* **Dated records**: `notes/adr/0175`, `gmf-interpreter-findings.md`,
  `.agents/session-records/2026-08-28-arc-4-sweep-4-siu.md` (16 hits, the
  largest single file).

**One hit carries a real positional claim, and it is disclosed rather
than repointed.** `.agents/prompts/2026-08-06-player-fold.md` names
"`emit_hl7.clj` `bed-swap-message` (~474 ...) and `merge-message`
(~499 ...)" -- a path AND a line claim about two movers. It is an
ARCHIVED PROMPT, a dated record under the standing treatment, and its
line numbers were already stale by hundreds of lines before this session
opened (`bed-swap-message` sat at 250). Not repointed; named here so the
next reader does not rediscover it as fresh.

### 2c. Level 2 -- paths and namespaces

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<mover>`,
checked per mover rather than in aggregate: **two**, and **one goes
false**.

| site | names | disposition |
|---|---|---|
| `v2_replay.clj:403` | `merge-message` | **GOES FALSE.** A private mover with no `#'` reach, so no def answers it. **REPOINTED** to `ehrt.sim-emit-hl7.messages/merge-message` |
| `notes/adr/0066-player-fold.md:84` | `merge-message` | dated ADR record, standing treatment -- not repointed |

`v2_replay.clj` is a `src` file and C1(a) fences TEST files, so the
repoint is owed rather than fenced -- mechanically the same reasoning
that unfenced `v2_replay.clj:207` for the fifteenth.

**ALIAS-qualified claims in TEST files: three, all fenced.**
`siu_test.clj:11` and `:72` name `emit-hl7/siu-message` in prose, and
that file's `ns` really does alias `ehrt.sim-emit-hl7.emit-hl7`, so both
go false. `components/sim/test/ehrt/sim/siu_run_test.clj:106` names the
same thing with no such alias in its `ns` at all -- prose convention
rather than a resolvable claim, but stale in the same sense. All three
are test files. **FENCED CITATIONS backlog, +3, the emit phase's second,
third and fourth rows.**

**PATH claims** naming `emit_hl7.clj` in a LIVE surface: three, plus five
sibling `ns` docstrings.

| site | disposition |
|---|---|
| `person-simulator/limitations_test.clj:152` | names `demographics-timeline`/`demographics-at`, TIMELINES movers -- the thirteenth's fenced row, unchanged and not this cluster's |
| `sim-engine/assignment.clj:19` | cites this file for the fixed-consumption law; **no mover cites the assigner pair**, confirmed by scanning the moved text for `assign-pathway`/`assign-module`/`fixed-consumption` -- zero. The citing form is `plan-latency`, cluster 7 |
| `emit_hl7_test.clj:1306` | test file, C1(a)-fenced; stale since the twelfth, confirmed for the third time |

The five sibling docstrings ("Extracted VERBATIM from `emit_hl7.clj`")
are provenance and always true.

### 2d. Level 3, the rest -- and the tripwire that STILL does not fire

* **`hand-owned-assets.edn`.** All rows read and all four distinct
  sources read: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/palgebra-design.md`,
  `components/corpus/docs/pipeline.edn` and
  `demos/scenarios/ed-tuesday/README.md`. **Not one names a mover** --
  zero, in all four, checked per source. Nothing was edited, no
  `:reviewed-at` is bumped, and **the tripwire does not fire -- the
  SEVENTH session in the program to read them and fire nothing.** The
  eleventh session's structural reason now has a sharper form: that page
  names ENGINE forms by DEFINING FORM, EMITTER segment builders as BARE
  NAMES (the fifteenth found `evn-segment` and `obr-segment` that way),
  and message builders **not at all**.
* **Docstring-as-authority (census constraint 2).** Nine citations sit
  inside the moved text and every one resolves: `in1-segment`'s and
  `z-segments-for`'s own docstrings (both bare names, both in landed
  siblings), `single-subject-message`'s twice and `BedSwapSide`'s, plus
  four self-referential ones. **No form left in the residue cites a
  mover's docstring**, checked rather than assumed.
* **Cross-namespace claims INSIDE the moved text.** Exactly ONE:
  `ehrt.sim-engine.engine/config-keys`, in the DFT banner. `engine.clj`
  is a pure facade and `config-keys` is one of its 43 delegating defs
  (`engine.clj:624`), so it resolves.

### 2e. PRE-EXISTING FALSE CLAIMS: none new, and none fixed

`rulings.md#R-move-not-improve` is why. The fifteenth session's four
stand unchanged and none is this cluster's: the relocated registry-comment
claim now inside `segments.clj:248`; `timelines.clj`'s unresolvable
`engine/stamp-encounter`; the un-re-depthed `docs/` family; and
`emit_hl7_test.clj:1306`. **This move relocates none of them and adds
none.** The `docs/`-family citations do not travel either -- the movers
carry none.

### 2f. THE RESIDUE-CLAIM CLASS FIRES AGAIN -- and widens past banners

The fifteenth opened this class on the prior moved-to banners in the
residue. This session confirms it fires again, harder, exactly as that
session predicted -- and finds it does not stop at banners. **Six claims
across four prior banners AND one sibling `ns` docstring.** Every count
was re-derived from the tree, never adjusted by arithmetic on the old
number.

| where | claim | why it goes false | now |
|---|---|---|---|
| `hl7-time` banner `:49-50` | `hl7-timestamp` has "twenty bare-name sites in this file" | all twenty are in message builders | **ZERO**; the def stands for the test tree alone |
| `hl7-time` banner `:53-55` | `transmit-seconds`: "eleven forms here still call it; those twelve call sites" | ten of the eleven forms leave | **ONE form, TWO sites** -- `emit-wire` |
| `segments` banner `:110-114` | "NO BANNER TRAVELS ... each names a MESSAGE type whose builder stays" | the builders leave | retold in the past tense, pointing at cluster 6 |
| `segments` banner `:132` | "their twenty-two call sites below keep resolving through them unqualified" | all 22 are `msh`/`pid` sites in builders | **ZERO**; both defs join `tn-field`'s shape |
| `segments` banner `:139-140` | "Their thirty-five call sites below name them `segments/...`" | 34 of 35 leave | **ONE** -- `plan-charges`' `charge-concept` |
| `er7` banner `:177` | "SEVENTEEN across ten forms are still below" | all seventeen are in builders | **NOT ONE `er7/` call site is left in this file** |

**The new sub-class, and it is a real widening of the level**:
`segments.clj`'s OWN `ns` docstring carries the same two counts as its
banner ("twenty-two remaining call sites there", "their thirty-five call
sites there"), and both go false the same way. A prior extraction writes
its account into TWO places -- the residue's banner and its own
namespace's docstring -- and a later move must correct both. Both are
corrected in the move commit.

**Checked and LEFT, with the reason stated rather than assumed:**

* `registry` banner `:81-85` -- "`siu-filler-status` and
  `charge-closing-kinds` are public in `registry` instead, because
  `sch-segment`, `event->messages` and `plan-charges` still call them".
  `event->messages` is the second of those three to leave the file
  (`sch-segment` went with cluster 5). The sentence is phrased in FORMS,
  and all three forms still call them from wherever they now live, so it
  survives -- the fifteenth session's own ruling on this exact sentence,
  applied a second time. A judgment call, named as one.
* `registry` banner `:77-78` -- "plus the bare-name sites below that keep
  resolving through these defs". Uncounted, and some remain:
  `room-and-board-code`, `chatter-event-kinds` (twice),
  `order-status-ladder` and `result-status-ladder` still have in-file
  callers. Still true.
* `er7.clj`'s `ns` docstring -- "eight in `segments` and ten in
  `messages`". **Phrased in CLUSTER terms and therefore still exactly
  right**, and this session's own scan confirms the ten: `er7/` sites
  appear in exactly ten of the thirteen movers. That is the lesson of the
  class, now confirmed from the other side.
* `timelines.clj`'s "nineteen call sites across sixteen forms in `er7`,
  `messages`, ..." -- cluster terms, survives.
* **The historical-landmark class**, read and left again: `hl7-time`'s
  "`transmit-seconds` from just above `single-subject-message`",
  `er7`'s "the site-profiles Task 3 section from just above
  `single-subject-message`", and `segments`' region list. All are
  past-tense accounts of where a form sat at ITS OWN move and remain
  accurate as history.

### 2g. Claims INSIDE the moved text: ZERO go false, a first

Every positional word in the moved text was enumerated mechanically --
"below", "above", "this file", "this namespace", "this section", "here",
"earlier", "later", "elsewhere". **Sixteen occurrences, and NOT ONE goes
false.**

The mechanical reason is worth stating because it is what makes the
result cheap to trust: the moved text contains **zero** occurrences of
`below`, `this file` or `this namespace` -- the three words that
falsified cluster 5's prose. What it has is fourteen `here`s and two
`above`s, and every one resolves inside its own form (`there is no
clinical-time field here`, `no re-sorting here`, `the docstring above`)
or inside the travelling set (`the same split clock every builder here`
-- all thirteen builders travel together; `the gate lives here rather
than in the registry` -- `event->messages` travels and `registry` is a
file).

So, unlike every cluster since the fourth, **the prose travels
untouched**, and the moved body's only differences are the sixty-four
requalifications and two markers.

### 2h. Fenced citations: THREE ADDED

The three `emit-hl7/siu-message` prose claims of 2c. They are the emit
phase's second, third and fourth fenced rows, and the first fenced rows
that are ALIAS-qualified prose rather than a path or a namespace claim.

## 3. Step 3 -- `ehrt.sim-emit-hl7.messages` (`e17be4a`)

### The moved body diffs as SIXTY-SIX lines, and no others

Verified by diffing the moved text as a BLOCK against `a1380fa`'s own
`emit_hl7.clj` -- **629 lines either side** -- not inferred from hunk
headers. 64 are the requalifications; 2 are the `defn-` -> `defn` marker
widenings on `chatter-message` and `ladder-message`. **Not one other
docstring, comment or code line differs**, and the diff is line-for-line
symmetric (66 removed, 66 added).

### The residue's non-comment changes are FOUR lines

Read off `git diff -U0` rather than eyeballed: the `:require` block (one
alias dropped, one added), the ONE delegating def, and `emit-wire`'s two
call sites becoming `messages/chatter-message` and
`messages/ladder-message`. Every other addition in the residue is a `;;`
line -- the six banner corrections of 2f and the new 50-line moved-to
banner.

### The dispositions, asserted live under `-M:dev` rather than argued

* `messages` has **13 interns and 3 publics**; `ns-interns` minus
  `ns-publics` is exactly the TEN builders. The first cluster in either
  file where most of the namespace is private.
* **Constraint 5's prohibition, asserted per name**: `emit-hl7/single-
  subject-message`, `/bed-swap-message`, `/bed-status-message`,
  `/siu-message`, `/merge-message`, `/orm-message`, `/oru-message`,
  `/observation-message`, `/diagnostic-report-message`, `/dft-message`,
  `/chatter-message` and `/ladder-message` all fail to resolve.
  **TWELVE of twelve.**
* `event->messages` resolves in `emit-hl7`, is PUBLIC, and its value is
  `identical?` to `messages/event->messages`.
* `emit_hl7.clj`'s public surface is **24 vars before and 24 after** --
  it lost thirteen names and kept one def, and the arithmetic works
  because twelve of the thirteen were private.
* `messages` holds **exactly six** namespace aliases (`parser`,
  `hl7-time`, `registry`, `timelines`, `er7`, `segments`) and `emit-hl7`
  **seven**, both read off the LOADED namespaces rather than the source.
  Six ties `segments` for the widest require set this program has
  extracted.
* `interface.clj` still resolves, 20 publics, and re-exports **exactly
  ZERO** of the thirteen -- census 2a's "`messages` owes none", confirmed
  by intersecting the interface's publics against the mover set rather
  than by reading the census.

`clojure -M:poly check` **OK**. No Error 104: every one of `messages`'
five `ehrt.` requires is an intra-component sibling.

### The require set, re-derived in BOTH directions -- and ONE GOES DEAD

`messages.clj` needs six and uses all six: `parser` 33, `segments` 34,
`er7` 17, `timelines` 11, `hl7-time` 10, `registry` 1. It needs neither
`clojure.string`, nor `sim-model`, nor `site-profile`, and no `:import`.

`emit_hl7.clj` gained ONE (`messages`) and **LOST ONE**
(`com.nervestaple.hl7-parser.parser`), so its require set is seven either
side. Checked per alias in the residue rather than assumed: `registry`
11, `timelines` 7, `hl7-time` 5, `segments` 4, `sim-model` 4, `er7` 3,
`messages` -- and `parser` **0**. All 33 parser sites were inside the
thirteen movers, which is the substantive fact behind the dead require:
**`emit_hl7.clj` builds no message text of its own any more.** It plans,
and delegates the rendering.

### THIRTEEN of the nineteen delegating defs now have no in-file caller

Derived per def in both directions, and it is the sharpest measure of
what this move did. SIX **lose all their in-file callers to this move**
-- `hl7-timestamp` (20 sites), `msh-segment` (11), `pid-segment` (11),
`message-type-registry` (10), `siu-event-kinds` (1), `siu-renders?` (1)
-- joining SEVEN that were already caller-less. SIX keep callers:
`default-utc-offset`, `room-and-board-code`, `chatter-event-kinds`,
`order-status-ladder`, `result-status-ladder` and `control-id-for`.

Every one of the thirteen is correctly KEPT by the facade rule; what
changed is why. `msh-segment` and `pid-segment` in particular now join
`tn-field` in the shape the fifteenth session flagged: **a delegating def
kept alive only by prose and a test.** That backlog row goes from one
member to three.

## 4. Steps 3-4 -- the gates

`bin/regression-oracle a1380fa e17be4a`: **IDENTICAL: every root's digest
matches**, **41 roots**, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** -- the eleventh session's finding
-- and it is more load-bearing here than for any cluster before it. These
thirteen forms ARE the message layer: every ER7 string this project emits
comes out of one of them, and a single misplaced requalification among
the sixty-four would move every rendered message in every root. It did
not.

`bin/ground-truth-bracket a1380fa e17be4a`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0. **Near-vacuous here, and more so
than for any prior cluster** -- its own output says why, "THIS IS NOT A
REGRESSION-ORACLE CLAIM: the `:hl7` half of every root is excluded by
construction", and this cluster is nothing BUT the `:hl7` half. Run and
reported because the prompt asks for it, not because it discriminates.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj`, the tenth extraction's
hazard class.

### The suite delta, measured IN-CLONE

The baseline is the STEP-0 COMMIT itself, which is cleaner than the
fifteenth session's `git stash push -u`: because C8(a) landed as its own
docs commit, `d27460f` IS the pre-move tree, and `make test` there is an
in-clone baseline needing no stash. Both runs `MAKE_EXIT=0`, both **408
zero-failure blocks over 216 distinct namespaces / 4,751 tests** -- so
**this move adds no `deftest`**, confirmed by differencing the namespace
sets, which are equal.

Assertions go **24,149 -> 24,153, +4**, and the delta was PREDICTED from
the two file-globbing gates' own populations before the final run, with
the populations counted rather than reasoned:

| namespace | population | delta |
|---|---|---:|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | `doseq` over `components/*/src` + `bases/*/src` `.clj`, 127 -> **128** | +1, x2 projects |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | `doseq` over `components/sim-emit-hl7/src` `.clj`, 10 -> **11** | +1, x2 projects |

Each runs in both `conformance` and `ehrt-cli`, so 2 x (1 + 1) = **+4**,
and measured equals predicted exactly. `messages.clj` is the one new
production source file and it passes the second gate on its merits.

**The record and prompt archive move no gate's population**, confirmed
rather than assumed: `index_completeness_test`'s two `doseq`s iterate
DIRECTORIES, not files, and `notes-prompts-files-match-the-dated-
convention-test` iterates `notes/prompts`, the frozen archive, not
`.agents/prompts`. This run is the FINAL tree -- record, prompt,
regenerated indexes, `state-derived.md` and the two prose corrections all
included.

**ONE RED WAS SEEN AND IS DISCLOSED, because it was mine and it was
sequencing rather than substance.** An earlier `make test` taken at the
move commit returned `MAKE_EXIT=2` on a single failure,
`state-derived-md-matches-a-fresh-render-test`, and the diff was exactly
one cell: the `:onboarding` row read 1516/14 in the committed file
against 1529/1 from the live tree. That run overlapped an uncommitted
`roadmap.md` edit -- the sixteenth landing -- which the prompt's own
step 2 sequences LAST, before `make state-derived`. Nothing in `e17be4a`
caused it and nothing in the extraction was implicated; the run above,
taken after the regeneration, is the one that measures this session.

## 5. Closing arithmetic

### The partition, cluster 6 of 8

At the move commit `e17be4a`, `emit_hl7.clj` goes 1,400 lines to **827**
and 46 def-forms to **34** -- it lost thirteen and gained one --
and `messages.clj` is **695** lines / 13 forms plus its `ns`, the largest
namespace this program has extracted from the emitter by form-lines.

Arithmetic, checked rather than asserted: 1,400 - 629 removed + 50
inserted + 6 banner-correction lines + 0 for the `:require` block, which
swapped one alias for another and stayed seven lines, = **827**, which is
`wc -l` on the file at that commit.

At the FINAL tree the two prose corrections of section 3 add one line to
`emit_hl7.clj` (**828**) and two to `messages.clj` (**697**). Both are
`;;`-comment and ns-docstring lines; neither file's form count or code
changes, and the move commit is where the arithmetic above is checkable.

### Census corrections

* Section 2's `messages` row says "13 forms, 549 lines" in the summary
  table and "623 lines" in the form list; the forms are 13 and the
  FORM-lines are **578**, which is section 2a's own figure, confirmed to
  the line. **That is now SIX clusters running** where 2a is right and
  both of section 2's figures measure something else.
* Section 2's line spans for this cluster are stale by the five prior
  moves, as expected and by design.
* **Section 3b's SIX `messages` rows all reproduce EXACTLY** -- five as
  caller (`segments` 62, `hl7-time` 21, `er7` 16, `registry` 13,
  `timelines` 10) and one as callee (`facade` 4).
* Section 2a's "`messages` owes NO `interface.clj` re-export" is
  confirmed exactly: zero.
* Section 2a's placement judgment holds: it put `messages` sixth because
  it "depends on five clusters, all landed". All five were needed, and
  the widenings of clusters 2, 4 and 5 in particular were load-bearing --
  had `er7/provider-by-id`, `segments/pv1-segment` or
  `registry/charge-closing-kinds` stayed private, this move would have
  been blocked.
* Section 2a predicted "62 crossings into `segments` alone" for this
  cluster and that is exactly what the tree gives.
* **New, and not in the census**: a prior extraction's account lives in
  TWO places -- the residue's banner and its own namespace's `ns`
  docstring -- and a later move must correct both. The fifteenth found
  the first; this one found the second.
* **New, and not in the census**: banners can travel for a LATER cluster
  precisely because an earlier one left them. The census's model of a
  banner is per-move; the real object is a section, whose ownership can
  change hands.

### The backlogs

* **FENCED CITATIONS: THREE added**, the emit phase's second through
  fourth rows and the first that are alias-qualified prose:
  `siu_test.clj:11`, `siu_test.clj:72` and `sim/siu_run_test.clj:106`,
  all naming `emit-hl7/siu-message`.
* **RETIREMENT CANDIDATES**: no new member, but the shape the fifteenth
  opened goes from ONE to THREE. `msh-segment` and `pid-segment` join
  `tn-field` as delegating defs with no caller in `emit_hl7.clj` at all,
  kept alive by a test's `#'` access and a namespace claim. All three are
  correctly kept; all three are what the repoint pass should look at
  first.
* **STALE-BEFORE-THIS-MOVE**: unchanged, four rows, none of them this
  cluster's and none relocated by it.
* **NEW, row 4 -- SIX GATES THAT NO LONGER EXIST, still cited by name.**
  `attic-rotation-test`, `notes-prompts-frozen-test`,
  `prompt-record-pairing-test`, `reading-set-budget-test`,
  `rulings-lint-test` and `state-residue-test`, all deleted by `e189418`
  (de-scaffold, 2026-08-25). `rulings.md` was updated and says so;
  `.agents/reading-sets.edn:3`, `.agents/prompts/README.md:18`,
  `.agents/reading-sets-baseline.edn:92` and
  `state_derived_test.clj:132` were not, and still describe them as
  enforcing. `:docs` has rendered at -2 (over budget) ever since with no
  red. Found not caused, disclosed not fixed -- restoring a gate or
  correcting the prose is a ruling.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt asked for the sweep's dispositions "committed before the
   move or absence disclosed".** Absence: the one repoint
   (`v2_replay.clj:403`) is TRUE until the seam, so it is paid in the
   move commit and there is no sweep commit. Same reasoning as the
   fifteenth.
2. **The prompt named the requalify-through-facade class and asked for it
   counted and named in advance.** Done -- seven names, sixty-four sites,
   named before the move and then produced independently by the rewriter,
   which agreed on all seven figures.
3. **The prompt's expected "predicted reds RED-FIRST with successor" did
   not arise.** No gate reads anything this session changed: the
   hand-owned-asset tripwire's four sources name no mover, and the charter
   registers name none either. No deliberate red, so none to pair.
4. **A found-not-caused finding outside the emitter entirely** -- six
   deleted gates still cited by name, three of them on live surfaces.
   Reported under the prompt's own "including found-not-caused
   (disclose-and-backlog)", and not fixed.
5. **The charter-register token counts differ from the fifteenth
   session's** (15/13 against 21/27), and this record states its own
   counting rule rather than reproducing a figure it cannot reconstruct.
   Neither file changed; only the rule did.
6. **The residue-claim level found a sub-class the prompt did not name**
   -- sibling `ns` docstrings, not only residue banners.
7. **Two tooling hazards were hit and are named** so the next session
   does not pay them again: a regex-based comment/string stripper
   overflows the stack on this file's long docstrings (replaced with a
   character state machine), and a heredoc through the `wsl -e bash -lc`
   wrapper mangles backslashes (scripts were written to files instead,
   which is the standing rule this session briefly forgot).

## 6. What is left in this program

Two emit clusters (`planners`, `facade`), then the apply-path
unification. The next session takes `planners` -- 11 forms, census 2a's
seventh, 364 form-lines, and the cluster with **zero incoming edges**:
nothing inside `emit_hl7.clj` calls a planner, so every caller is
`interface.clj` or `ehrt.sim.run`.

Four things it should expect, all located rather than guessed:

* **FOUR of its five public forms are `interface.clj` re-exports**
  (`plan-latency`, `plan-chatter`, `plan-charges`, `plan-ladders`), so
  C1(a)'s delegating-def obligation is heavier there than anywhere since
  cluster 2, and `interface.clj` must keep resolving through them.
* **Its requalification bill is small.** The residue's qualified sites
  are now concentrated in the planners: `plan-charges` alone carries
  `timelines/encounter-spans`, `segments/charge-concept` and
  `registry/charge-closing-kinds`, all already qualified. The bare names
  it must requalify are the delegating-def ones -- `room-and-board-code`,
  `chatter-event-kinds`, `order-status-ladder`, `result-status-ladder`,
  `control-id-for` -- a handful, not sixty-four.
* **The residue-claim class will fire a third time**, on this move's own
  banner among others, and this banner is written to be recountable for
  that reason: its counted claims ("thirty-three call sites", "two call
  sites in `emit-wire`") name the form they live in.
* **After `planners`, `facade` is the caller-travels case** the census's
  section 2a flagged as OPEN and the author's: whether `emit_hl7.clj`
  ends a pure facade the way `engine.clj` did under C4(b).

## 7. The budget

C8(a) recovered **fourteen** lines (1530 -> 1516). This session's own P5
row update spends **thirteen** of them -- eleven for the sixteenth
landing and its NEW WITH block, two for the three fenced-citation rows --
and closes at **1529 of 1530, ONE line of headroom**.

The Records list absorbed this session's path at ZERO cost, on the
existing last line, which now carries six emit records.

**ENGINE DOCTRINE WAS NOT TOUCHED** beyond the C8(a) compaction the
author ruled, which is itself the reason there was room at all.

**ESCALATED, and stated plainly rather than deferred.** One line of
headroom does not carry the seventeenth landing, let alone the
eighteenth and the apply-unification arc after it. C8(a) compacted the
ENGINE phase; the EMIT phase's own instance detail is now the largest
compactable block in the row, and compacting it is the same class of
re-triage -- the author's, under
`rulings.md#R-section-retriage-is-author-judgement`. **The design channel
should rule before the seventeenth session opens**, or that session's
first act will be a STOP.

## 8. CI at the pushed tip -- the close marker

`gh run watch 33422475527 --exit-status` exits 0; the run is
**completed / success** at `3a4c9e4dbad5cc01f8c5243361d62b8db7158547`,
the pushed tip
(https://github.com/pragsmike/ehr-testing-tools/actions/runs/33422475527).
That is the close marker under `rulings.md#R-session-verifies-ci-via-gh`,
which the de-scaffold ruling of 2026-08-25 retired as a TAG condition and
kept as this. No tag was paid.

`gh run list` shows exactly ONE run for the THREE-commit push, at the tip
-- the one-CI-run-per-push fact the twelfth session measured, confirmed
for the fifth session running, and this is the first time it has been
confirmed over a three-commit push rather than a two.

`bin/post-push-verify a1380fa 3a4c9e4` reports the remote tip matching
HEAD and every commit message in range pure ASCII, with the expected
DISCLOSED line that it reports the CI run once rather than awaiting it
(AR-CI-4); the awaiting is this section's own `gh run watch`. All three
pushed messages were additionally diffed against the files that produced
them: each differs by exactly one trailing blank line, which is
`git log --format=%B`'s own artifact and not a wrapper mangling.

This session had no red-first pair to spend the run on. The sweep owed
one repoint and no gate reads the docstring it lives in, so there was no
deliberate red to pair -- the second session running with none.

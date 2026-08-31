# Emit namespace extraction, 5 of 8: the `segments` cluster, the largest

Session record, 2026-08-31. HEAD at start `386e738`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed 2026-08-30 in ten sessions and
whose emit half has now landed five of eight clusters. Author rulings
C1(a) (`emit_hl7.clj` stays the facade, moved PUBLIC vars get delegating
defs, private movers widen ONLY where a caller stays behind, no test
file changes), **C1(a)'s C7 extension** (a private var reached by `#'`
from a C1(a)-fenced test file gets a `^:private` delegating def -- the
`tn-field` precedent, ratified since), constraint 5 as a PROHIBITION
(the `weighted-pick` reading), and S1(a) (an equivalence proof replaces
red-before-green).

`bin/preflight` exit 0, **no findings** -- the sixth clean preflight of
the program, and for the same reason as the first five: this session's
own first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 1. Step 1 -- the derivations

### Fifteen forms, 518 form-lines, EIGHT regions

Derived at `386e738` with a char-level scanner for every top-level
form's true span, and a whole-symbol scan over each form's body with
string literals, character literals and line comments stripped. The
scanner reproduces the fourteenth session's own closing count exactly --
**58 def-forms** in the file plus the `ns`.

| form | span | lines | marker |
|---|---|---:|---|
| `control-id-for` | 99-141 | 43 | `defn` **public** |
| `msh-segment` | 143-166 | 24 | `defn-` |
| `evn-segment` | 168-173 | 6 | `defn-` |
| `pid-segment` | 222-278 | 57 | `defn-` |
| `in1-segment` | 280-302 | 23 | `defn-` |
| `mrg-segment` | 305-311 | 7 | `defn-` |
| `pv1-segment` | 314-365 | 52 | `defn-` |
| `npu-segment` | 449-459 | 11 | `defn-` |
| `sch-segment` | 517-570 | 54 | `defn-` |
| `orc-segment` | 667-700 | 34 | `defn-` |
| `obr-segment` | 702-760 | 59 | `defn-` |
| `obx-segment` | 762-807 | 46 | `defn-` |
| `observation-obx-segment` | 937-988 | 52 | `defn-` |
| `charge-concept` | 1076-1086 | 11 | `defn-` |
| `ft1-segment` | 1089-1127 | 39 | `defn-` |

**518 form-lines, which is census 2a's own figure to the line** -- the
fifth cluster running where 2a is right and section 2's own table
(here "434 lines", and "559 lines" in its form list) measures something
else. ONE public and fourteen private, every one of the fourteen a
`defn-`; this is the first emit cluster with no `def` at all.

The moved text is **527 source lines across eight regions** -- 518
form-lines and 9 blank separators, every gap checked with `cat -A`
rather than by eye. **Eight regions is the most of any cluster in
either file**, and the reason is structural: `messages` interleaves with
`segments` form by form through the second half of the file.

### NO BANNER TRAVELS -- and the split-region case is now the RULE

The fourteenth session found the program's FIRST split-region banner
(the M3 header) and left it. **This cluster has FOUR, and not one
comment block travels.** Every comment block adjacent to a moved region
is separated from it by a blank line -- so none is form-attached -- and
every one heads a section this cluster splits:

| banner | span | heads | why it stays |
|---|---|---|---|
| ARC 4 SWEEP 4: SIU^S12 | `:514-515` | `sch-segment` + `siu-message` | names the SIU family; `siu-message` stays |
| M3: ORM^O01 + ORU^R01 | `:663-664` | `orc`/`obr`/`obx` + `orm-message`/`oru-message` | the fourteenth's own row, unchanged |
| M5b: :observation | `:934-935` | `observation-obx-segment` + `observation-message` | names the event kind whose builder stays |
| ARC 4 SWEEP 2: DFT^P03 | `:1063-1074` | `charge-concept`/`ft1-segment` + `dft-message` | twelve lines of charge doctrine, all about the DFT |

Every prior cluster took its banners all-or-nothing except the
fourteenth, which took two and left one. **This one takes none**, and
the pattern the census did not anticipate is now the ordinary case for
the back half of the file.

### The edges: 3b reproduces EXACTLY in BOTH directions, six rows

Established in both directions, because five clusters have now left the
file and a bare-name scan alone would be blind to them.

**OUTGOING.** The qualified-symbol scan of the moved text finds
`parser/` 127, `er7/` 23, `site-profile/` 15, `registry/` 1 and `str/`
1 -- and no `timelines/`, no `hl7-time/`, no `sim-model/`, and no
`:import`-worthy dotted name. The bare-name scan against the 43
stay-behind names finds **FIVE**: `message-type-registry`
(`control-id-for`), `escape-er7` (`in1-segment`, `sch-segment`),
`tn-field` (`pid-segment`) and `hl7-timestamp` (`ft1-segment`) -- every
one of them a DELEGATING DEF rather than a form that really lives here.

Folded into 3b's own accounting -- DISTINCT (caller, callee) pairs --
that is `segments -> er7` **18**, `segments -> registry` **2** and
`segments -> hl7-time` **1**: census 3b's three `segments`-as-caller
rows, **all three EXACT**. `site-profile`, `parser` and `str` are not
clusters, so 3b has no row for them and their absence is not an
omission.

**INCOMING.** 70 raw call sites across 15 forms.

| mover | stay-behind callers | sites |
|---|---|---:|
| `control-id-for` | 10 message builders, `plan-latency`, `plan-ladders`, `emit-wire` | 13 |
| `msh-segment` | 10 message builders + `chatter-message` | 11 |
| `pid-segment` | 9 builders + `chatter-message` (`bed-swap-message` twice) | 11 |
| `pv1-segment` | 9 builders + `chatter-message` (`bed-swap-message` twice) | 11 |
| `evn-segment` | 5 builders + `chatter-message` | 6 |
| `orc-segment` | `orm-message` (x2), `oru-message`, `diagnostic-report-message` | 4 |
| `obr-segment` | `orm-message`, `oru-message` (x2), `diagnostic-report-message` | 4 |
| `in1-segment` | `single-subject-message`, `chatter-message` | 2 |
| `observation-obx-segment` | `observation-message`, `diagnostic-report-message` | 2 |
| `mrg-segment` / `npu-segment` / `sch-segment` / `obx-segment` / `charge-concept` / `ft1-segment` | one each | 6 |

**66 distinct pairs against 70 raw sites**, and 66 is `messages` 62 +
`planners` 3 + `facade` 1 -- **all three of census 3b's
`segments`-as-callee rows, EXACTLY**, and their split confirmed by
caller rather than assumed. Six of 3b's sixteen rows reproduce in this
session alone.

**INTERNAL: ZERO.** Not one of the fifteen forms calls another --
checked per form, not in aggregate. `er7` had nine internal edges and
six unwidened movers because of them; this cluster has none, so
**every private mover here widens and none stays private**. That is the
first cluster in either file whose new namespace has nothing private in
it at all.

### The `#'` census, re-run whole rather than trusted

The fourteenth session's scan was the first of its kind, so this one
re-ran it over the whole tree rather than taking its two predictions on
trust. **102 `#'` sites exist in the tracked tree.** Filtered against
`emit-hl7` and against all fifteen mover names, **exactly three remain,
all three in `sim-emit-hl7` tests**: `tn-field`
(`v2_replay_test.clj:261`, cluster 4's, already answered) and
`msh-segment`/`pid-segment` at `emit_hl7_test.clj:688`/`:690`. **The
prediction is exact, and there is no third site.**

Widened deliberately past `#'`, because a var can be reached other
ways: a scan for `resolve`, `ns-resolve`, `requiring-resolve`,
`with-redefs`, `alter-var-root`, `intern`, `find-var` and `(var ...)`
against the fifteen names finds **NOTHING**, anywhere in the tree. The
`#'` class is the whole class.

### THREE DEFS, FOURTEEN WIDENINGS, NONE PRIVATE

* **ONE PUBLIC delegating def, C1(a)**: `control-id-for`. It is owed
  twice over -- `interface.clj:26` re-exports it (census 2a's
  "`segments` 1", read from `interface.clj` and confirmed by
  intersecting its publics against `segments`' interns), six test files
  call `emit-hl7/control-id-for`, and `corpus_io/er7_fields.clj:129`
  names it by namespace. `sim/identifiers.clj:79` looks like a seventh
  but is not: its `emit-hl7` alias is `ehrt.sim-emit-hl7.interface`,
  checked in its own `ns` form rather than guessed from the alias.
* **TWO `^:private` delegating defs, the C7 extension applied TWICE.**
  `msh-segment` and `pid-segment` widen like the other twelve, and
  additionally owe a def, for exactly the reason `tn-field` did. The
  fourteenth session predicted both by name; both are confirmed, and
  the prediction's location (`emit_hl7_test.clj:688`/`:690`) is right
  to the line.
* **TWELVE widenings with no def**: `evn-segment`, `in1-segment`,
  `mrg-segment`, `pv1-segment`, `npu-segment`, `sch-segment`,
  `orc-segment`, `obr-segment`, `obx-segment`,
  `observation-obx-segment`, `charge-concept`, `ft1-segment`.
* **ZERO stay private**, because there is nothing for them to stay
  private for: constraint 5's prohibition has no case here.

### A `pid-segment` def answers a claim it was not owed for

Worth its own line, because it is the second time in two sessions that
a C7 def pays for something else as well. `v2_replay.clj:95` and
`:559` both name `ehrt.sim-emit-hl7.emit-hl7/pid-segment` -- namespace
claims about a PRIVATE mover, the rarest and most dangerous class in
the census. The `^:private` def keeps the var in that namespace, so
both **stay TRUE** and neither is fenced.
`demos/traces/order-result/README.md:69` names the same var and is
answered the same way.

### THE MOVED TEXT ITSELF MUST BE REQUALIFIED -- a program first

The five bare names above resolved in `emit_hl7.clj` only through that
file's own delegating defs. A facade may require its implementations;
an implementation may not require its facade (the tenth engine
session's law), so `segments.clj` cannot keep them bare. They name
their real homes instead: `registry/message-type-registry`,
`er7/escape-er7` twice, `er7/tn-field`, `hl7-time/hl7-timestamp`.

Four clusters before this one moved their text VERBATIM apart from
marker widenings and prose. **This is the first whose code had to
change to survive the seam**, and it is the direct consequence of being
the first cluster to sit downstream of THREE landed siblings.
`er7/tn-field` resolves because cluster 4 widened it; had it stayed
private there, this move would have been blocked.

### No collision

Nothing in the tree names `sim-emit-hl7.segments` or
`sim_emit_hl7/segments`, checked before writing the file; the token
`segments` appears nowhere in `emit_hl7.clj` as a bare symbol (only in
`obx-segments`, `z-segments` and prose), so the alias was free. No
build surface names `emit_hl7.clj` -- `Makefile`, `.gitattributes`,
`workspace.edn` and `deps.edn` carry the COMPONENT -- and no live
surface enumerates this component's namespaces, so a TENTH needs no
row. `deps.edn`'s two comments name the S3 trio
(`emit-hl7`/`v2-replay`/`site-profile`) as dated provenance, not as a
population.

## 2. Step 2 -- the sweep, which owes ONE repoint

**No sweep commit precedes the move**, and the absence is derived
rather than assumed. Every level was run and every level is reported.
**No red was predicted and none occurred.** The one repoint the sweep
owes is a claim that is TRUE until the seam, so restating it a commit
early would make it false in the interim -- the ninth and tenth
extraction's rule -- and it is paid in the move commit.

### 2a. Both charter registers, hand-read row by row

Census constraint 6's own level, and the one that cost the twelfth
session a deliberate red. Counting every `path.ext` token: 21 in
`components/patient-simulator/docs/limitations.md` and 27 in
`components/person-simulator/docs/limitations.md`, the same counting
rule and the same figures the fourteenth session recorded, neither file
having been edited since. **Every full path was resolved mechanically
against the tree and all twelve distinct full paths resolve.**

**ZERO name a `segments` mover.** Exactly one `emit_hl7` token appears
in either register, and it is the COMPONENT directory inside
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/registry.clj` -- the
twelfth session's own repoint, in patient-simulator's care-plan row,
which still resolves and whose pinned phrase ("no real CarePlan-
equivalent segment") is still in `registry.clj`. The level is EMPTY,
stated as a result rather than a silence.

### 2b. Level 1 -- 2,394 shingles over 1,539 files, 67 hits, zero positional

The fifteen movers' docstrings and `;;` comments cut into 2,394
six-word shingles -- the largest shingle set of the program, 52% more
than cluster 4's -- and searched across the whole tracked tree. **67
files carry a hit and not one is a positional claim about a mover.**
Four classes:

* **Session-marker phrases, the bulk.** "GMF coverage Wave D stage D1
  2026-08-02 ADR-0029", "ARC 4 SWEEP 3 ADR-0175 design (b)" and "ARC 3B
  SWEEP 1 ADR-0174 ruling" appear in every file the corresponding stage
  touched -- date-and-stage labels, not claims about anything.
* **Doctrine echoes in TEST files**, C1(a)-fenced in any case:
  `siu_test.clj` (15 hits) restates `sch-segment`'s SCH-11/reschedule
  reasoning, `result_clock_test.clj` (8) restates `obr-segment`'s
  ADR-0142 clock, `charges_test.clj` (8) restates `ft1-segment`'s
  FT1-4 clinical-instant law, `emit_hl7_test.clj` (18) several.
* **Doctrine echoes in LIVE surfaces**: `sim-theory.edn` and
  `docs/site-profiles.md` restate `msh-segment`'s MSH-3/4/5/6/12 list,
  the demo READMEs restate `pid-segment`'s homeless-address rule and
  `pv1-segment`'s "every message this project had ever produced". All
  bare names in behavioural claims.
* **Dated records**: `notes/adr/0142` (37 hits, the largest single
  file), 0173, 0174, 0175, `gmf-interpreter-findings.md` -- the last of
  which says of itself that it is "historical record ... not the
  current state". Not repointed, on the standing treatment.

### 2c. Level 2 -- paths and namespaces

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<mover>`,
checked per mover rather than in aggregate: **five**, and **four
survive**.

| site | names | disposition |
|---|---|---|
| `corpus_io/er7_fields.clj:129` | `control-id-for` | PUBLIC mover, delegating def forwards -- **stays TRUE** |
| `v2_replay.clj:95` | `pid-segment` | private mover with a `^:private` def -- **stays TRUE** |
| `v2_replay.clj:559` | `pid-segment` | same -- **stays TRUE** |
| `demos/traces/order-result/README.md:69` | `pid-segment` | same -- **stays TRUE** |
| `v2_replay.clj:207` | `in1-segment` | **GOES FALSE.** A private mover with NO `#'` reach, so no def answers it. **REPOINTED** to `ehrt.sim-emit-hl7.segments/in1-segment` |

That last row is the one the eleventh session had to FENCE and the
fourteenth escaped for free. It is **not** fenced here, and the reason
is mechanical rather than lucky: `v2_replay.clj` is a `src` file, and
C1(a) fences TEST files. The repoint is one line, in the move commit.

**PATH claims** naming `emit_hl7.clj` in a LIVE surface: four, plus
four sibling `ns` docstrings.

| site | disposition |
|---|---|
| `person-simulator/limitations_test.clj:152` | names timelines movers; the emit phase's first FENCED row, still fenced |
| `emit_hl7_test.clj:1306` | test file, C1(a)-fenced; already stale -- confirmed AGAIN, see 2e |
| `sim-engine/assignment.clj:19` | cites this file for the fixed-consumption law; the citing form is `plan-latency` (`:1311`), cluster 7. No mover cites the assigner pair |
| `emit_hl7.clj:336` | **ALREADY FALSE at HEAD**, and it is INSIDE `pv1-segment`, a mover. See 2e |

The four sibling docstrings (`hl7_time.clj`, `registry.clj`,
`timelines.clj`, `er7.clj`, each "Extracted VERBATIM from
`emit_hl7.clj`") are provenance and always true.

**The four prior moved-to banners were read in full**, and this is the
level where this cluster differs from every predecessor -- see 2f.

### 2d. Level 3, the rest -- and the tripwire that STILL does not fire

* **`hand-owned-assets.edn`.** All five rows read, and all four
  distinct sources read. **`docs/dev/simulator-architecture.md` names
  TWO movers -- `evn-segment` at `:428` and `obr-segment` at `:438` --
  the FIRST time in this program a hand-owned-asset source has named an
  emitter mover at all.** Both are BARE NAMES in behavioural claims
  ("through `hl7-timestamp` exactly as `evn-segment` receives
  `clinical-ts`"; "`obr-segment` renders there too, but OBR-7 means
  observation time"), carrying no positional or path word, so both stay
  true across the seam. Nothing was edited, so no `:reviewed-at` is
  bumped and **the tripwire does not fire -- the SIXTH session in the
  program to read them and fire nothing.** The eleventh session's
  structural reason is now confirmed by a positive instance rather than
  by absence: that page names ENGINE forms by DEFINING FORM and EMITTER
  forms as BARE NAMES.
* **Docstring-as-authority (census constraint 2).** Three exist, and
  all three survive. `emit_hl7.clj:376` (`single-subject-message`,
  which stays) cites "`in1-segment`'s own docstring"; `:643`
  (`merge-message`, which stays) cites `single-subject-message`'s and
  names `control-id-for`; `v2_replay.clj:189` cites "`pid-segment`'s
  own docstring". All bare names, and every cited docstring travels
  with its form.
* **Cross-namespace claims INSIDE the moved text, checked per name.**
  Four, and all four resolve: `ehrt.sim-emit-hl7.site-profile/
  default-msh` (`site_profile.clj:44`), `ehrt.sim-engine.engine/run`
  (`engine.clj:735`, a delegating def), `ehrt.sim-engine.order-
  profiles/abnormal-flag` (`order_profiles.clj:102`) and
  `ehrt.sim-model.persona/Persona` (`persona.clj:101`).

### 2e. FOUR PRE-EXISTING FALSE CLAIMS, NONE FIXED

Disclosed rather than absorbed, backlogged, and none caused by this
move. `rulings.md#R-move-not-improve` is why they are not fixed here.

**(A) is the thirteenth session's row, and it is now INSIDE THE MOVED
TEXT.** `emit_hl7.clj:514` -> `:418` -> **`:336`** still says
"`emit_hl7.clj`'s own registry comment calls traffic invisible to every
consumer a failure mode". The phrase lives in `registry.clj`'s
`message-type-registry` DOCSTRING, verified by grep -- two hits in the
tree, this one and `emit_hl7_test.clj:1307`, and neither is an
`emit_hl7.clj` comment. It sits in `pv1-segment`, so **this move
relocates it into `segments.clj`**, exactly as cluster 4 relocated
`location-field`'s stale path. Stated plainly rather than glossed.

**(B) is the thirteenth's second row, still open.** `timelines.clj`'s
`encounter-spans` still cites `ehrt.sim-engine.engine/stamp-encounter`,
which does not resolve. Not this cluster's.

**(C) The fourteenth session's tree-wide `docs/operational-models.md`
finding is a FAMILY, not a single path.** Two movers carry
`docs/operational-models.md` (`in1-segment`, `obx-segment`), and the
sweep found **two more members of the same un-re-depthed-relocation
class**: `docs/patient-state-model.md`, carried twice by `pv1-segment`,
and `docs/research/SimHospital-Synthea-limitations-considered.md`,
carried once by `in1-segment`. All three files really live under
`components/sim/docs/`. Counted with the rule "live-surface files
citing the bare `docs/`-rooted path, excluding those that also cite the
correct prefix, excluding `.agents/` and `notes/`": **26 for
`operational-models.md` and 30 for `patient-state-model.md`.** The 26
differs from the fourteenth session's 21 by COUNTING RULE, not by
change -- this count includes the four `components/sim/docs/*.md`
files and one more test file that its enumeration did not list; neither
figure is wrong and both measure a real set. **Five citations across
three files leave with `segments.clj`.** Neither fenced nor this
cluster's to fix.

**(D) `emit_hl7_test.clj:1306`, confirmed stale for the second time.**
Its "registry comment" phrase is `registry.clj`'s, and its `pv1-segment`
mentions are bare names in behavioural claims. The twelfth session's
disposition of it to cluster 5 was wrong; there is nothing for cluster
5 to repoint, and it is C1(a)-fenced regardless. The fourteenth session
said so; this one is the cluster it was deferred to, and confirms it.

### 2f. THE RESIDUE LEVEL FIRES -- a prior banner can be FALSIFIED

**The finding of the session, and it is a new class the census has no
rule for.** Every prior session read the prior moved-to banners looking
for mover NAMES. This one read them for CLAIMS, and found three that
this move makes false. All three are in the residue, all three are
paid in the move commit, and all three are mechanical rather than
stylistic.

| banner | claim | why it goes false |
|---|---|---|
| `hl7-time`, `:50` | `hl7-timestamp` has "twenty-one bare-name sites in this file" | 21 is right at HEAD (20 in stay-behinds plus `ft1-segment`'s). `ft1-segment` leaves. **Twenty** |
| `er7`, `:199-203` | the eleven widenings have "forty-one call sites across eighteen forms below", of which "forty now name them `er7/...`" | 26 of the 41 sites, in 8 of the 18 forms, leave with `segments`. **Seventeen across ten forms remain.** And the one site that was NOT `er7/`-qualified -- `pid-segment`'s `tn-field` call -- leaves and is requalified, so **all forty-one now name them `er7/...`** |
| `er7`, `:215-216` | "`pid-segment`'s own call site keeps resolving through it unqualified" | `pid-segment` leaves. The `^:private tn-field` def now has NO caller in this file at all; what it stands for is `v2_replay_test.clj:261`'s var access and `v2_replay.clj:166`'s namespace claim |

Every count above was re-derived from the tree, not adjusted by
arithmetic on the old numbers. The other claims in all four prior
banners were checked and left: the hl7-time banner's
`transmit-seconds` sentence (movers use `hl7-time/` zero times), the
registry banner's `siu-filler-status` sentence (`sch-segment` still
calls it and still names it `registry/...`, from its new home), and
`er7.clj`'s own `ns` docstring, which says "eight in `segments` and ten
in `messages`" -- **phrased in CLUSTER terms rather than file terms,
and therefore still exactly right**, 8 and 10 confirmed by this
session's own scan. That is the lesson the class carries: a banner
phrased in clusters survives; a banner phrased in "below" and "this
file" does not.

**The historical-landmark class was read and LEFT.** The hl7-time
banner locates its movers "from just above `control-id-for`" and the
er7 banner "from just above `mrg-segment`" and "from just above
`ft1-segment`". All three landmarks leave with this cluster. These are
past-tense accounts of where forms sat at their own move and remain
accurate as history, so they are not touched -- disclosed as a judgment
call rather than silently kept.

### 2g. Claims INSIDE the moved text

Every positional word in the moved text was enumerated mechanically --
"below", "above", "this file", "this namespace", "this section",
"here", "earlier", "later" -- eleven occurrences, each resolved against
what travels. **TWO go false**, both paid in the move commit:

* `control-id-for`: "the SAME construction every message-builder call
  site **below** uses". All thirteen builders stayed behind. The word
  is dropped.
* `sch-segment`: "which is why `siu-message` **below** renders PID and
  PV1 in that order". `siu-message` stayed behind. The word is dropped.

**Nine were checked and left VERBATIM**: `control-id-for`'s "the only
family here" and `pid-segment`'s "reaches here" (both scoped inside
their own form); `pv1-segment`'s "one of the 28 blanks below" and "the
SAME empty field that stood here before" (both about field positions
inside the form); `sch-segment`'s "not an omission here" and "this
renders-only namespace does not own" (`segments.clj` is renders-only
too); `orc-segment`'s "what ORC-2 has always carried here";
`obr-segment`'s "those are per-OBX below" (`obx-segment` is still below
it, both travelling in order) and "not padding invented here"; and
`observation-obx-segment`'s "the 'never a positional pad' sentence
above", which is nine lines up in its OWN docstring.

**In the RESIDUE prose, nothing else goes false.** Every mention of a
mover outside the eight regions was listed with its owning form and
read: `:620` (`siu-message`), `:991`/`:992`/`:1030` (the
`observation-obx-segment` comments heading `observation-message` and
`diagnostic-report-message`), `:1207`, `:1220`, `:1330` and `:1702`
(`control-id-for`'s non-injectivity, four times). All are bare names in
behavioural claims.

### 2h. Fenced citations: NONE ADDED

The thirteenth session opened the emit phase's FENCED CITATIONS
backlog and the fourteenth added none. This one adds none either, and
for two reasons rather than one: the two `pid-segment` namespace claims
are answered by the C7 def, and the one claim that does go false
(`v2_replay.clj:207`) is in a `src` file, which C1(a) does not fence.

## 3. Step 3 -- `ehrt.sim-emit-hl7.segments` (`f4304a6`)

### The moved body diffs as TWENTY-ONE lines, and no others

Verified by diffing the moved text as a BLOCK against `386e738`'s own
`emit_hl7.clj` -- eight regions plus seven separators, **534 lines
either side** -- not inferred from hunk headers. Fourteen are the
`defn-` -> `defn` marker widenings, five are the requalifications of
section 1, two are the prose corrections of 2g. **Not one other
docstring, comment or code line differs.**

### The residue diffs as 32 lines, one require block and three banners

Classified line by line against `386e738`'s own residue rather than
eyeballed: the surviving 1,346 lines of `emit_hl7.clj` were compared
against the new file with the inserted banner block removed. **Outside
the eight regions, exactly 32 lines carry the 35 name-qualifications**
(three lines carry two calls each), plus the `ns` `:require` block and
the three prior-banner corrections of 2f. **Nothing else moves.**

The additions are the 52-line block inserted where region 1 was -- a
48-line moved-to banner, a blank, and the three delegating defs.

### The dispositions, asserted live under `-M:dev` rather than argued

* `segments`' public surface is **exactly FIFTEEN**, and `ns-interns`
  minus `ns-publics` is **EMPTY** -- the first cluster in either file
  whose new namespace has nothing private in it.
* **Constraint 5's prohibition**: `emit-hl7/evn-segment`,
  `/in1-segment`, `/mrg-segment`, `/pv1-segment`, `/npu-segment`,
  `/sch-segment`, `/orc-segment`, `/obr-segment`, `/obx-segment`,
  `/observation-obx-segment`, `/charge-concept` and `/ft1-segment` all
  fail to resolve. **TWELVE of twelve.**
* **The two exceptions, asserted rather than assumed**:
  `#'emit-hl7/msh-segment` and `#'emit-hl7/pid-segment` both resolve,
  both vars ARE private, both are `identical?` to their `segments`
  counterparts, and calling them by hand returns the MSH and PID maps
  `emit_hl7_test.clj:688`/`:690` assert on.
* `control-id-for` is public here, `identical?` to
  `segments/control-id-for`, and `interface.clj/control-id-for` still
  answers.
* `segments` holds **exactly six namespace aliases** (`parser`, `str`,
  `hl7-time`, `registry`, `er7`, `site-profile`) and `emit-hl7`
  **seven**, both read off the LOADED namespaces rather than the source
  text. Six is the widest require set of any namespace this program has
  extracted.
* **`emit_hl7.clj`'s public surface is 24 vars before and 24 after**,
  both sides derived by the same scanner.
* `interface.clj` still resolves, 20 publics, and re-exports **exactly
  ONE** of the fifteen -- `control-id-for`, census 2a's own figure.

`clojure -M:poly check` **OK**. No Error 104: every one of `segments`'
five `ehrt.` requires is an intra-component sibling.

### The require set, re-derived in BOTH directions -- and TWO GO DEAD

`segments.clj` needs six and uses all six. `emit_hl7.clj` gained ONE
(`segments`) and **LOST TWO**, which is the first dead require the emit
phase has produced against the engine phase's nine. Checked per alias
in the residue rather than assumed: `parser` 33, `er7` 20, `timelines`
18, `hl7-time` 15, `registry` 12, `sim-model` 4, `segments` 35 -- and
`str` **0** and `site-profile` **0**. Both are dropped.

DISCLOSED: `bed-status-message` stays behind and its docstring still
names `site-profile/default-msh` in prose. That is a citation, not a
code reference, and needs no alias; it is not repointed because the var
it names is exactly where it says.

## 4. Steps 3-4 -- the gates

`bin/regression-oracle 386e738 f4304a6`: **IDENTICAL: every root's
digest matches**, 41 roots, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** -- the eleventh session's
finding -- and it is more load-bearing for this cluster than for any
before it: these fifteen forms ARE the wire, and a single misplaced
requalification among the five would move every rendered message.

`bin/ground-truth-bracket 386e738 f4304a6`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0. **Near-vacuous here and it says
so in its own output** -- "THIS IS NOT A REGRESSION-ORACLE CLAIM: the
`:hl7` half of every root is excluded by construction". Run and
reported because the prompt asks for it, not because it discriminates:
every one of the fifteen movers exists to serve the wire side, which is
the half this gate excludes. It is as nearly vacuous for this cluster
as for the fourteenth, and for the same reason.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj`, the tenth extraction's
hazard class.

### The suite delta, measured IN-CLONE

`make test` run at `386e738` (the pre-move tree, in the clone, no
worktree, reached by `git stash push -u`) and at `f4304a6`. Both
`MAKE_EXIT=0`, both **408 zero-failure blocks over 216 distinct
namespaces / 4,751 tests** -- so **this move adds no `deftest`**,
confirmed by differencing the namespace sets, which are equal. 4,751
and 24,145 are the figures the fourteenth extraction closed on,
reproduced here to the assertion before anything moved.

Assertions go **24,145 -> 24,149, +4**. The delta was PREDICTED before
the move from the two file-globbing gates' own populations, and the
populations were counted rather than reasoned:

| namespace | population | delta |
|---|---|---:|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | `doseq` over `components/*/src` + `bases/*/src` `.clj`, 126 -> **127** | +1, x2 projects |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | `doseq` over `components/sim-emit-hl7/src` `.clj`, 9 -> **10** | +1, x2 projects |

Each runs in both `conformance` and `ehrt-cli` (`deps.edn` adds
`components/docs-tooling/test` to both), so 2 x (1 + 1) = **+4**, and
measured equals predicted exactly. `segments.clj` is the one new
production source file and it passes the second gate on its merits,
requiring only the parser, `clojure.string` and four intra-component
siblings.

**METHOD CORRECTION, disclosed.** The fourteenth session's record
describes attributing the delta "by diffing per-namespace counts out of
the two logs". `make test`'s output carries no per-namespace assertion
count -- it prints `Testing <ns>` lines and one `Ran N tests containing
M assertions` per project -- so this session could not reproduce that
method and used the population derivation above instead. The two agree
on the answer; the method is stated so the next session does not look
for output that is not there.

**The close-out run is assertion-for-assertion identical to the move
run** -- 24,149 either way, 4,751 tests either way, 408 blocks either
way, 216 namespaces either way -- so this session's doc additions (the
record, the prompt archive, the P5 rewrite, the two regenerated
indexes and `state-derived.md`) move no gate's population. That run was
taken against the FINAL tree, after `make state-derived`.

## 5. Closing arithmetic

### The partition, cluster 5 of 8

`emit_hl7.clj` goes 1,881 lines to **1,400**, and 58 def-forms to
**46** -- it lost fifteen and gained three. `segments.clj` is 592 lines
/ 15 forms plus its `ns`, the largest namespace this program has
extracted from the emitter and the widest by require set.

Arithmetic, checked rather than asserted: 1,881 - 536 removed + 52
inserted + 3 net prose lines - 1 require line = **1,400**, which is
`wc -l` on the file.

### Census corrections

* Section 2's `segments` row says "15 forms, 434 lines" in the summary
  table and "559 lines" in the form list; the forms are 15 and the
  FORM-lines are **518**, which is section 2a's own figure, confirmed
  to the line. **That is now FIVE clusters running** where 2a is right
  and both of section 2's figures measure something else.
* Section 2's line spans for this cluster are stale by the four prior
  moves, as expected and by design.
* **Section 3b's SIX `segments` rows all reproduce EXACTLY** -- three
  as caller (`er7` 18, `registry` 2, `hl7-time` 1) and three as callee
  (`messages` 62, `planners` 3, `facade` 1) -- on 3b's own accounting
  of DISTINCT pairs. 66 pairs against 70 raw sites, the third
  divergence of the two accountings (18/19, then 34/43, now 66/70).
* Section 2a's "`segments` owes ONE `interface.clj` re-export" is
  confirmed exactly: `control-id-for`.
* Section 2a's placement judgment holds: it put `segments` fifth
  because it "depends on `er7`, `registry` and `hl7-time`, all landed".
  All three dependencies were needed, and `er7`'s widening of
  `tn-field` in particular was load-bearing.
* **New, and not in the census at all**: a MOVED CLUSTER'S OWN TEXT can
  require requalification. The census's constraint list assumes the
  moved text is verbatim apart from markers; from cluster 5 on it is
  not, because the facade's delegating defs are unreachable from the
  implementation side.
* **New, and not in the census at all**: a PRIOR cluster's moved-to
  banner can be FALSIFIED by a later move. Constraint 6 covers claims
  in other files and section 2g's practice covers claims in the moved
  text; nothing covered claims in text a previous extraction wrote into
  the residue. Three fired here. The rule this session used: recount
  from the tree and pay in the move commit, exactly as for moved-text
  prose.
* **New**: a cluster can leave a DEAD REQUIRE behind in the emit phase.
  Four moves did not; this one leaves two.

### The backlogs

* **FENCED CITATIONS**: **none added**, and 2h says why.
* **STALE-BEFORE-THIS-MOVE** gains a fourth row and widens its third:
  `docs/operational-models.md` is one member of a FAMILY of
  un-re-depthed `docs/`-rooted citations of files that really live
  under `components/sim/docs/`; `docs/patient-state-model.md` (30 live
  files) and `docs/research/SimHospital-Synthea-limitations-
  considered.md` are two more, and five such citations across three
  paths leave with `segments.clj`. Neither fenced nor this cluster's.
* **RETIREMENT CANDIDATES**: **one added, and it is a new shape.** The
  `^:private tn-field` def in `emit_hl7.clj` now has **no caller in
  that file at all** -- `pid-segment` took its one call site. It is
  kept, correctly, by the FACADE RULE and by C7: `v2_replay_test.clj`
  still reaches the var and `v2_replay.clj:166` still names it. But it
  is the first delegating def in this program kept ALIVE ONLY BY PROSE
  AND A TEST, and the repoint pass should see it as such.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt's two PREDICTED `#'` sites are both confirmed, to the
   line**, and the widened sweep it asked for found no third -- not
   under `#'`, and not under any reflective var-access form either.
2. **The prompt's expectation that the sweep would produce a hit list
   is half right.** Every level ran and four produced material, but
   only ONE repoint is owed (`v2_replay.clj:207`), and it cannot be
   committed before the move because it is true until the seam. So
   there is no sweep commit; the gate is answered with the derived
   absence and every level's result in section 2.
3. **A THIRD class of correction appears that the prompt's list names
   but no prior session has had to pay: prior banners.** Three claims
   in two prior clusters' moved-to banners go false and are corrected
   in the move commit. Section 2f.
4. **The moved text is NOT verbatim**, for the first time in the
   program: five bare names are requalified. Forced, not chosen --
   section 1 states why, and `rulings.md#R-move-not-improve` is not
   strained, because leaving them would not compile.
5. **Two requires are dropped from `emit_hl7.clj`.** No prior emit
   session dropped one; four for four ADDED only. Derived per alias in
   both directions.
6. **The suite-delta attribution method differs from the fourteenth
   session's stated method**, and section 4 says why and what was used
   instead.
7. **The `docs/operational-models.md` live-file count differs from the
   fourteenth session's (26 vs 21).** It is a COUNTING RULE difference,
   stated in 2e, not a change in the tree.

## 6. What is left in this program

Three emit clusters (`messages`, `planners`, `facade`), then the
apply-path unification. The next session takes `messages` -- 13 forms,
census 2a's sixth, **578 form-lines and the heaviest crossing count in
the file** (62 into `segments` alone, which have just become
`segments/...` qualifications rather than bare names).

Four things it should expect, all located rather than guessed:

* **Every one of its 62 `segments` call sites is ALREADY QUALIFIED.**
  This move rewrote 35 of the 70 and the other 35 resolve through
  delegating defs; when `messages` leaves, the 13 `control-id-for`, 11
  `msh-segment` and 11 `pid-segment` sites among them become
  `segments/...` too, and the two `^:private` defs will lose their last
  in-file callers exactly as `tn-field` just did.
* **The `#'` class is EXHAUSTED.** The whole-tree scan found three
  sites and all three are now answered. `messages`, `planners` and
  `facade` owe no `^:private` def unless a test file changes.
* **The residue-banner class will fire again, and harder.** Five
  banners will stand above `messages` when it moves, and this move's
  own banner makes counted claims ("thirty-five call sites below",
  "twenty-two call sites below") that `messages` will falsify. They are
  written to be recountable for that reason.
* **The P5 row has NO headroom left.** See section 7.

## 7. The budget

The fourteenth session's update closed at **1526 of 1530, 4 lines of
headroom**. This session's P5 row update spends **all four**, and
closes at **1530 of 1530, ZERO headroom**.

The +4 is the smallest that would carry the landing honestly, and it
was bought rather than simply spent: the ELEVENTH-through-FOURTEENTH
landings were compressed again (parenthesised clauses for `registry`
and `er7`, "in either file" dropped, the fourteenth's own C7 sentence
shortened to a named ruling), and the THIRTEENTH/FOURTEENTH "NEW WITH"
block was repacked to five lines from six. That paid for the fifteenth
landing's own line inside the existing landings paragraph, a four-line
"NEW WITH THE FIFTEENTH", and one line in backlog (3). **The Records
list absorbed this session's path at ZERO cost**, on the existing last
line, which already carries four emit records.

**ENGINE DOCTRINE WAS NOT TOUCHED**, as the prompt fenced and as C6(a)
fenced before it.

**ESCALATED, and it is the reason this section is not a formality.**
The sixteenth session cannot land `messages` -- the heaviest cluster in
the file, with the most to say -- without going over budget, and
`rulings.md#R-budget-stop` says it must compact or STOP rather than
bump. What remains compactable in the P5 row is what the thirteenth and
fourteenth deliberately left: the row's standing doctrine still carries
engine-phase INSTANCE detail (the `weighted-pick` counts, the
`gt-emitters.svg` red-first history, the two backlogs' per-form
enumerations). Compacting that is re-triage and therefore the author's
(`rulings.md#R-section-retriage-is-author-judgement`). **The design
channel should rule before the sixteenth session opens**, or that
session's first act will be a STOP.

## 8. CI at the pushed tip -- the close marker

`gh run watch 33408257330 --exit-status` exits 0; the run is
**completed / success** at `c59829e30256038b4f544d2cee487f5685544f99`,
the pushed tip
(https://github.com/pragsmike/ehr-testing-tools/actions/runs/33408257330).
That is the close marker under `rulings.md#R-session-verifies-ci-via-gh`,
which the de-scaffold ruling of 2026-08-25 retired as a TAG condition
and kept as this. No tag was paid.

`gh run list` shows exactly ONE run for the two-commit push, at the tip
-- the one-CI-run-per-push fact the twelfth session measured, confirmed
for the fourth session running. This session had no red-first pair to
spend it on: the sweep owed one repoint and no gate reads the docstring
it lives in, so there was no deliberate red to pair.

`bin/post-push-verify 386e738 c59829e` reports the remote tip matching
HEAD and every commit message in range pure ASCII, with the expected
DISCLOSED line that it reports the CI run once rather than awaiting it
(AR-CI-4); the awaiting is this section's own `gh run watch`.

CI covers what a green local `make test` cannot: `make docsgen`
freshness, `verify-nist-lock`, and the generated-doc diff. The
`state-derived.md` regeneration this session paid is exactly the class
that gate exists for, and it is green.

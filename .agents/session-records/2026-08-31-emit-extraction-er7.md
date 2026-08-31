# Emit namespace extraction, 4 of 8: the `er7` cluster, the first non-leaf

Session record, 2026-08-31. HEAD at start `04b1e9f`, confirmed equal to
`origin/main`; working clone clean, no fresh clone taken. Ceremony R30.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), whose `engine.clj` half completed 2026-08-30 in ten sessions and
whose emit half has now landed four of eight clusters. Author rulings
C1(a) (`emit_hl7.clj` stays the facade, moved PUBLIC vars get delegating
defs, private movers widen ONLY where a caller stays behind, no test
file changes), S1(a) (an equivalence proof replaces red-before-green)
and constraint 5 as the `weighted-pick` precedent reads it -- which is
the ruling this session actually leans on, six times.

`bin/preflight` exit 0, **no findings** -- the fifth clean preflight of
the program, and for the same reason as the first four: this session's
own first act was a derivation rather than an edit. One DISCLOSED line,
expected: "HEAD is not currently tagged stable-*" (tags retired by the
de-scaffold ruling of 2026-08-25).

## 1. Step 1 -- the derivations

### Nineteen forms, 193 form-lines, SIX regions

Derived at `04b1e9f` with a char-level scanner for every top-level
form's true span, and a whole-symbol scan over each form's body with
string literals, character literals and line comments stripped. The
scanner reproduces the thirteenth session's own closing count exactly --
**74 def-forms** in the file plus the `ns`.

| form | span | lines | marker |
|---|---|---:|---|
| `er7-escape-table` | 189-193 | 5 | `def ^:private` |
| `escape-er7` | 195-206 | 12 | `defn` **public** |
| `er7-decode-map` | 208-209 | 2 | `def ^:private` |
| `unescape-er7` | 211-228 | 18 | `defn` **public** |
| `xpn-field` | 230-236 | 7 | `defn-` |
| `xad-field` | 238-245 | 8 | `defn-` |
| `tn-field` | 247-278 | 32 | `defn-` |
| `location-field` | 362-371 | 10 | `defn-` |
| `provider-field` | 373-378 | 6 | `defn-` |
| `provider-by-id` | 380-382 | 3 | `defn-` |
| `blank-fields` | 392-394 | 3 | `defn-` |
| `context-for-event` | 454-466 | 13 | `defn-` |
| `render-z-field` | 468-478 | 11 | `defn-` |
| `z-segment-for` | 480-483 | 4 | `defn-` |
| `z-segments-for` | 485-510 | 26 | `defn-` |
| `cwe-field` | 810-818 | 9 | `defn-` |
| `code-system->hl7-table-0396` | 825-834 | 10 | `def ^:private` |
| `coded-value-field` | 836-841 | 6 | `defn-` |
| `money` | 1264-1271 | 8 | `defn-` |

**193 form-lines, which is census 2a's own figure to the line.** Two
public and seventeen private; the seventeen are fourteen `defn-` and
three `def ^:private`, the three being the data tables
(`er7-escape-table`, `er7-decode-map`,
`code-system->hl7-table-0396`).

### The banners, and the program's FIRST SPLIT REGION

The moved text is 231 source lines across six regions -- 193 form-lines,
23 banner/comment lines and 15 blank separators, every gap checked with
`cat -A` rather than by eye. THREE comment blocks travel, because
everything each of them heads travels:

* the **M4 Task 4: ER7 escaping** header, `:174-187`, fourteen lines;
* the **Milestone site-profiles Task 3: Z-segment templates** header,
  `:449-452`, four lines;
* `code-system->hl7-table-0396`'s own five-line GMF Wave D comment,
  `:820-824`, which is form-attached (no blank line separates it from
  the `def`) -- distinct from the ARC-4 comment at `:828-833`, which is
  INSIDE the `def` and travels as part of the form.

**One banner STAYS, and it is the first split region in the program.**
The `;; --- M3: ORM^O01 + ORU^R01` header at `:807-808` heads a section
whose first three forms (`cwe-field`, `code-system->hl7-table-0396`,
`coded-value-field`) are `er7` and whose remaining five (`orc-segment`,
`obr-segment`, `obx-segment`, `orm-message`, `oru-message`) are
`segments` and `messages`. It names the two MESSAGE types, which stay,
so it stays. Every prior cluster's banners went all-or-nothing.

### ONE outgoing edge, and the census is exactly right

Established in both directions, because four clusters have now left the
file and a bare-name scan alone would be blind to them:

* **Bare names**: no `er7` mover references any def-form that stays
  behind in `emit_hl7.clj`. Checked per form against the 55 stay-behind
  names, not in aggregate.
* **Qualified and dotted symbols**: the whole-symbol scan of the moved
  text finds `parser/` (12), `str/` (3), `timelines/` (1) and
  `java.math.RoundingMode/HALF_UP`, and NOTHING else -- no `hl7-time/`,
  no `registry/`, no `site-profile/`, no `sim-model/`.

So the ONE edge is `context-for-event` -> `timelines/demographics-at`,
which is census 3b's own `er7 | timelines | 1` row, and **the prompt's
expectation is right for the first time in four sessions.** `er7.clj`
takes three requires (`parser`, `str`, `timelines`) and NO `:import`:
`money`'s `RoundingMode` is named in full and `unescape-er7`'s
`^String` hint is `java.lang`.

**This is the first emitter namespace to require a SIBLING extraction**
rather than only the parser and `clojure.string`, and the first cluster
in either file that is not a leaf.

### Incoming: 43 call sites, 19 caller forms -- and census 3b reproduces EXACTLY

| mover | stay-behind callers | sites |
|---|---|---:|
| `escape-er7` | `in1-segment`, `sch-segment` | 2 |
| `xpn-field` / `xad-field` / `tn-field` | `pid-segment` | 3 |
| `location-field` | `pv1-segment` (x2), `npu-segment` | 3 |
| `provider-field` | `pv1-segment` | 1 |
| `provider-by-id` | 8 message builders | 9 |
| `blank-fields` | `pv1-segment`, `sch-segment`, `obr-segment`, `ft1-segment` | 7 |
| `z-segments-for` | 8 message builders | 8 |
| `cwe-field` | `obr-segment`, `obx-segment`, `observation-obx-segment` | 5 |
| `coded-value-field` | `observation-obx-segment`, `ft1-segment` | 3 |
| `money` | `ft1-segment` | 2 |

Census 3b reads `segments -> er7` **18** and `messages -> er7` **16**.
Both reproduce **EXACTLY**, on 3b's own accounting -- DISTINCT
(caller, callee) pairs, which the thirteenth session established. 18 +
16 = **34 pairs** against **43 raw sites**, the second divergence of the
two accountings and a wider one than the thirteenth's (18 vs 19).
**`er7-escape-table`, `er7-decode-map`, `unescape-er7`,
`context-for-event`, `render-z-field`, `z-segment-for` and
`code-system->hl7-table-0396` have ZERO stay-behind callers**, and
3b has no `planners -> er7` and no `facade -> er7` row -- also exact.

### THREE DEFS, ELEVEN WIDENINGS, SIX LEFT PRIVATE -- the weighted-pick shape ARRIVES

The thirteenth session looked for the caller-travels shape and found it
structurally impossible. Here it arrives **six times at once**, and the
reason is structural too: this cluster has NINE internal edges, and six
of its nineteen forms have no caller outside it at all. (`unescape-er7`
has none either, inside or out, but it is PUBLIC and the tree calls it,
so C1(a) owes it a def regardless.)

* **TWO delegating defs, C1(a)**: `escape-er7` and `unescape-er7` are
  the public movers. `interface.clj` re-exports NEITHER -- census 2a's
  "er7 owes none of the sixteen" read from `interface.clj` and confirmed
  by a whole-tree grep from the other side. **This is the first cluster
  of this file whose defs are owed to the TREE alone**: `v2_replay.clj`
  `:152`/`:215`, and five occurrences across four `emit_hl7_test.clj`
  lines (`:787`, `:791`, `:796` twice, `:809`).
* **ELEVEN widenings, constraint 5's first sentence**: `xpn-field`,
  `xad-field`, `tn-field`, `location-field`, `provider-field`,
  `provider-by-id`, `blank-fields`, `z-segments-for`, `cwe-field`,
  `coded-value-field`, `money`. Each has at least one caller that stayed
  behind, so each is public in `er7` and gains no def.
* **SIX stay PRIVATE, constraint 5 read as a PROHIBITION**:
  `er7-escape-table`, `er7-decode-map`, `context-for-event`,
  `render-z-field`, `z-segment-for`, `code-system->hl7-table-0396`.
  Every one of their callers travelled. This is the eighth engine
  session's `weighted-pick` reading applied at scale, and the first time
  the emitter has had the case.

### `tn-field` -- the program's FIRST `^:private` DELEGATING DEF

**The sharpest finding of the session, and it is load-bearing code
rather than prose.**
`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj:261`
reads:

```clojure
rendered (first (:content (#'emit-hl7/tn-field phone)))
```

inside `pid-13-renders-and-reads-back-as-the-persona-shape`, a 200-case
`defspec`. It is a VAR ACCESS on a PRIVATE var in the namespace the
mover is leaving. Nothing about the move carries it: `tn-field` widens
(because `pid-segment` stays behind), but a PUBLIC delegating def would
widen `emit_hl7.clj`'s own surface, which C1(a) does not ask for, and
C1(a) forbids editing the test file.

`(def ^:private tn-field er7/tn-field)` is the resolution, and it is
exact on every count: the var stays in `ehrt.sim-emit-hl7.emit-hl7`, so
`#'emit-hl7/tn-field` resolves and calls the same function object;
`emit_hl7.clj`'s public surface is 24 before and 24 after; and
`pid-segment`'s own call site keeps resolving through it unqualified,
the same mechanism the two PUBLIC defs give `in1-segment` and
`sch-segment` for `escape-er7`. Forty of the forty-one widened-mover
call sites are rewritten `er7/...`; `tn-field`'s one is not, and that is
why.

The engine phase never met this class, and that is checked rather than
assumed: a whole-repo scan for `#'` against `emit-hl7` and against every
one of the eleven `sim-engine` implementation namespaces finds **exactly
three code sites, all three in `sim-emit-hl7` tests** -- `tn-field`
(this cluster), and `msh-segment` and `pid-segment`, both `segments` --
and **NONE anywhere on the engine side**, which the `state` session had
already checked and recorded for `#'engine/`. **Cluster 5 will meet it
twice.**

### No collision

Nothing in the tree names `sim-emit-hl7.er7` or `sim_emit_hl7/er7`,
checked before writing the file; the alias `er7` was unused in
`emit_hl7.clj` (the only `er7/` match in that file was inside the phrase
`escape-er7/unescape-er7`). No build surface names `emit_hl7.clj` --
`Makefile`, `.gitattributes`, `workspace.edn` and `deps.edn` carry the
COMPONENT, not the file -- so `er7.clj` needs no registration anywhere,
and no surface enumerates this component's namespaces, so a NINTH --
`er7` joins `emit-hl7`, `fan-out`, `hl7-time`, `interface`, `registry`,
`site-profile`, `timelines` and `v2-replay` -- needs no row.

## 2. Step 2 -- the sweep, which owes ZERO repoints

**No sweep commit precedes the move**, and the absence is derived rather
than assumed -- the eleventh's and thirteenth's precedent. Every level
was run and every level is reported. **No red was predicted and none
occurred**, so there was no red-first pair to land.

### 2a. Both charter registers, hand-read row by row

Census constraint 6's own level, and the one that cost the twelfth
session a deliberate red. Counting every `path.ext` token: 21 in
`components/patient-simulator/docs/limitations.md` (14 inside the table
rows) and 27 in `components/person-simulator/docs/limitations.md` (25
inside the rows). **Every full path was resolved mechanically against
the tree and every one resolves**; the eleven bare filenames are
component-local references and resolve within their own component. The
thirteenth session counted 12 + 24 for the same two files, which is a
different counting rule rather than a change: NEITHER register has been
edited since `3d918ce` (patient) and `16fe24c` (person), both older than
that session.

**ZERO name an `er7` mover and ZERO name `emit_hl7.clj`.** Exactly one
names a `sim-emit-hl7` file at all -- patient-simulator's care-plan row,
pointing at `registry.clj`, the twelfth session's own repoint, which
still resolves. The level is EMPTY, stated as a result rather than a
silence.

### 2b. Level 1 -- 1,572 shingles over 1,537 files, 41 hits, zero positional

The nineteen movers' docstrings and `;;` comments (including the three
travelling banners) cut into 1,572 six-word shingles and searched across
the whole tracked tree. **41 files carry a hit and not one is a
positional claim about a mover**; every one was read. Four classes:

* **Coincidence, 30 files** -- 29 whose ONLY hit is "GMF coverage Wave
  D stage D1", the session-marker phrase from
  `code-system->hl7-table-0396`'s form-attached comment, which appears
  in every file that stage touched; a date-and-stage label, not a claim
  about anything. Plus `oracle/digest.clj:769`'s "for the same reason it
  is", the same class against `money`.
* **Doctrine echoes in TEST files, 4** -- `charges_test.clj:234` restates
  `money`'s de-DE/BigDecimal rationale in its own words;
  `chatter_test.clj:21` and `emit_hl7_test.clj:666` restate `tn-field`'s
  "`bin/ground-truth-bracket` proves that per commit rather than this
  docstring asserting it"; `v2_replay_test.clj:270` calls itself "the
  mirror of `tn-field`'s own verbatim fallback". All bare names in
  behavioural claims, and C1(a)-fenced in any case.
* **Doctrine echoes in LIVE surfaces, 4** -- `sim_model/config.clj:271`
  and the three demo `config.edn`s share "no log fact carries a code
  for" with the ARC-4 comment. The same rule stated where it is
  enforced; `config.clj`'s own namespace claim is on
  `room-and-board-code`, a `registry` mover its delegating def forwards.
* **The F9 register family, 3** -- `components/sim/docs/third-party-
  sources.md:34`, `notes/sim/facts-register.md`'s F9 row, and
  `notes/sim/agents/plans/roadmap.md:313`. Dispositioned in 2c and 2e.
  (`emit_hl7_test.clj:737-748`'s M4 banner restates the same F9 fact and
  is counted in the test-file class above, where its other four shingles
  put it.)

29 + 1 + 4 + 4 + 3 = **41**, the whole hit set accounted for.

### 2c. Level 2 -- paths and namespaces

**NAMESPACE claims** of the form `ehrt.sim-emit-hl7.emit-hl7/<mover>`,
checked per mover rather than in aggregate: **three**, and all three
survive.

| site | names | disposition |
|---|---|---|
| `sim/docs/third-party-sources.md:34` | `escape-er7`, `unescape-er7` | PUBLIC movers, delegating defs forward them -- **stays TRUE** |
| `demos/traces/persona-enriched/README.md:77` | `escape-er7` | same -- **stays TRUE** |
| `sim_emit_hl7/v2_replay.clj:166` | `tn-field` | a namespace claim about a PRIVATE mover, the class the eleventh session had to FENCE -- but the `^:private` delegating def keeps the var in that namespace, so it **stays TRUE**. Read and left alone |

That last row is worth the space: it is the rarest and most dangerous
class in the census, it arrived, and the def this cluster owed for an
unrelated reason answered it for free.

**PATH claims** naming `emit_hl7.clj` in a LIVE surface: four, plus the
three prior moved-to banners. All four are the thirteenth session's own
rows and NONE is `er7`'s.

| site | disposition |
|---|---|
| `person-simulator/limitations_test.clj:152` | names timelines movers; already the emit phase's first FENCED row |
| `emit_hl7_test.clj:1306` | test file, C1(a)-fenced; already stale, see 2e |
| `sim-engine/assignment.clj:19` | cites this file for the fixed-consumption law; the citing form is `plan-latency` (`:1499`), cluster 7 |
| `emit_hl7.clj:418` | **ALREADY FALSE at HEAD**, see 2e; inside `pv1-segment`, which stays |

The three prior banners were read in full. `hl7_time.clj`'s and
`registry.clj`'s name no `er7` mover. `timelines.clj:55` names
`context-for-event` -- "a site profile's Z-segment templates bind
`[:persona ...]` paths against this exact value (`context-for-event`)"
-- as a BARE NAME carrying no positional word, in a claim about
behaviour. It stays true across the seam and was not touched.

### 2d. Level 3, the rest -- and the tripwire that again does not fire

* **`hand-owned-assets.edn`.** All five rows read, and all four distinct
  sources read: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/pipeline.edn`,
  `demos/scenarios/ed-tuesday/README.md`,
  `components/corpus/docs/palgebra-design.md`. **Not one names an `er7`
  mover or an `emit_hl7.clj` path.** Nothing was edited, so no
  `:reviewed-at` is bumped and the tripwire does not fire -- the FIFTH
  session in the program to read them and fire nothing, and for the
  eleventh's structural reason: that page names ENGINE forms by DEFINING
  FORM but EMITTER forms as BARE NAMES.
* **Docstring-as-authority (census constraint 2).** One EXISTS this time,
  where the eleventh found none: `single-subject-message`'s own
  docstring at `:525` cites "Z-segment surfaces -- `z-segments-for`'s own
  docstring". It is a BARE NAME with no path and no namespace, and the
  docstring travels with the form, so the citation still resolves. Rowed
  because the class is live in the emitter now and the next two clusters
  should expect it.
* **Cross-namespace claims INSIDE the moved text, checked per name.**
  `context-for-event` cites `ehrt.sim-engine.engine/replay` --
  **resolves**, `engine.clj:530`, a delegating def. `tn-field` and
  `xpn-field` cite `ehrt.sim-model.persona` and its `:phone` contract
  regex -- **resolves**, `persona.clj:108`, character for character.
  This is the check the thirteenth session's finding (B) proves is not
  optional.

### 2e. THREE PRE-EXISTING FALSE CLAIMS, NONE FIXED

Disclosed rather than absorbed, backlogged, and none caused by this
move. `rulings.md#R-move-not-improve` is the reason they are not fixed
here.

**(A) and (B) are the thirteenth session's two rows, both still open.**
`emit_hl7.clj:514` is now `:418` (the timelines move shortened the file
above it) and still says "`emit_hl7.clj`'s own registry comment" for a
phrase that lives at `registry.clj:41`. `timelines.clj`'s
`encounter-spans` still cites `ehrt.sim-engine.engine/stamp-encounter`,
which does not resolve. Neither is `er7`'s.

**(C) NEW, and TREE-WIDE: `docs/operational-models.md` has not existed
since the sim merge.** `location-field`'s docstring cites it --
"docs/operational-models.md's transfer/A02 spec" -- and the real path is
**`components/sim/docs/operational-models.md`**. Traced in history
rather than guessed: `aa1bb64` created the file at
`docs/operational-models.md` in the vendored sim repo, and `c0b5b0a`
("land sim as components/sim + bases/sim-cli") relocated it two segments
down without re-depthing the citations that named it. That is exactly
the un-re-depthed-relocation defect
`ehrt.docs-tooling.stale-path-test`'s 2026-08-15 addendum fixed for
markdown LINKS, and this form is not a link, so no gate sees it.

**TWENTY live-surface files carried it before this move and twenty-one
carry it after**, counted by excluding the two that cite the correct
`components/sim/docs/` prefix: `emit_hl7.clj`'s own `ns` docstring,
`er7.clj`, `check.clj`, `site_profile.clj`, `v2_replay.clj`,
`decide.clj`, `engine.clj`, `state.clj`, `event_schema.clj`, four
`sim-model` sources, `sim/run.clj`, five test files, a NOTICE and
`docs/site-profiles.md`. It is neither this cluster's fault nor this
cluster's to fix, and fixing one instance while twenty stand would be
worse than leaving it; but `location-field` carries it into a new file,
which is stated plainly rather than glossed.

**The ancient-namespace family, read and left.**
`notes/sim/facts-register.md`'s F9 row cites
`src/ehr_testing_sim/emit_hl7.clj` and `ehr-testing-sim.emit-hl7/
escape-er7`, and `notes/sim/agents/plans/roadmap.md:313` the same
namespace. Both were stale from the sim split, long before this program;
`notes/sim/` is frozen provenance ("untouched by law", ADR-0143) and no
gate reads it. The prompt predicted this exposure exactly.

### 2f. NO FENCED CITATION -- an emit-phase first for a non-trivial cluster

The thirteenth session opened the emit phase's FENCED CITATIONS backlog.
This cluster adds NONE, and the reason is the `tn-field` def: the two
claims that would have been fenced -- `v2_replay.clj:166`'s namespace
claim and `v2_replay_test.clj:261`'s var access -- are both answered by
a var that is still there.

### 2g. Claims INSIDE `emit_hl7.clj`, both directions

**In the MOVED text, THREE claims go false**, all paid in the MOVE
commit on the ninth and tenth extraction's rule that restating them a
commit early makes them false in the interim. Every positional word in
the moved text was enumerated mechanically -- "below", "above", "this
file", "this namespace", "this section", "here" -- and each was resolved
against what travels:

* the **M4 Task 4 header**: "encode on write (**below**, at every
  persona-derived free-text field)". Five forms call `escape-er7`; three
  travel (`xpn-field`, `xad-field`, `render-z-field`) and TWO stay
  (`in1-segment`'s payer name, `sch-segment`'s reason). The word is
  dropped.
* `render-z-field`: "every other free-text-carrying field **this
  namespace** renders (persona names, addresses, payer names)". Payer
  names are `in1-segment`'s, which stayed. Becomes "the emitter", the
  thirteenth session's own substitution.
* `z-segments-for`: "at every call site **below**". All eight stayed
  behind. The word is dropped.

**Four positional claims were checked and left VERBATIM**, because they
stay true: `escape-er7`'s "(unlike decode, below)" -- `unescape-er7` is
still below it; `unescape-er7`'s "this namespace's own documented
workaround" and "see this section's header comment" -- both halves and
the header all travel; `xpn-field`'s "see this file's Task 4 section" --
the Task 4 section travels with it; and `cwe-field`'s
"`coded-value-field`, below" -- it is still below.

**In the RESIDUE, nothing goes false.** All four prose mentions outside
the six regions were listed with their owning form and read: `:286`
(`pid-segment`, "PID-13 (TN phone, `tn-field`)"), `:525`
(`single-subject-message`, the docstring citation of 2d), `:595`
(`npu-segment`, "the SAME `location-field` rendering PV1-3 uses") and
`:1121` (`observation-obx-segment`, "`coded-value-field`, OBX-5"). All
four are bare names in behavioural claims.

## 3. Step 3 -- `ehrt.sim-emit-hl7.er7` (`c7fbcb5`)

### The moved body diffs as FIFTEEN lines, and no others

Verified by diffing the moved text as a BLOCK against `04b1e9f`'s own
`emit_hl7.clj` -- six regions plus five separators, **236 lines either
side** -- not inferred from hunk headers. Eleven are the `defn-` ->
`defn` marker widenings; four are the three prose corrections of 2g, the
middle one of which spans two lines. **Not one other docstring, comment
or code line differs.**

### The residue diff is 40 qualifications, one require and three defs

Classified line by line rather than eyeballed. Outside the six removed
regions, **exactly 40 lines change and every one is a bare name becoming
`er7/<name>`**; nothing else in 1,881 lines moves. The additions are the
`:require`, and one 46-line block -- the 42-line moved-to banner, a
blank, and the three delegating defs -- inserted where region 1 was.

**Disclosed, and deliberately not fixed**: in `sch-segment`, three
`blank-fields` call sites carry trailing `; SCH-n .. SCH-m` comments
that were column-aligned with three neighbours, and `er7/` pushes them
four columns right. Re-aligning the three neighbours would change lines
the move does not otherwise touch, and the stronger, checkable claim --
that the residue diff is exactly 40 name-qualifications -- is worth more
than the alignment.

### The dispositions, asserted live under `-M:dev` rather than argued

* `er7`'s public surface is **exactly THIRTEEN** -- the two public
  movers plus the eleven widenings -- and `ns-interns` minus
  `ns-publics` is **exactly the six** left private.
* **Constraint 5's prohibition**: `emit-hl7/xpn-field`, `/xad-field`,
  `/location-field`, `/provider-field`, `/provider-by-id`,
  `/blank-fields`, `/z-segments-for`, `/cwe-field`, `/coded-value-field`
  and `/money` all fail to resolve. **TEN of ten.**
* **The exception, asserted rather than assumed**: `#'emit-hl7/tn-field`
  DOES resolve, its var IS private, `@#'emit-hl7/tn-field` is
  `identical?` to `@#'er7/tn-field`, and calling it on `"303-292-0567"`
  returns `["(303)292-0567"]` -- the `defspec`'s own assertion, run by
  hand.
* `er7` holds **exactly three namespace aliases** (`parser`, `str`,
  `timelines`), confirming the require derivation from the LOADED
  namespace and not from the source text alone.
* **`emit_hl7.clj`'s public surface is 24 vars before and 24 after**,
  both sides derived by the same scanner, and the two delegating defs
  are `identical?` to the moved functions.
* `interface.clj` still resolves, 20 publics, and re-exports **none of
  the nineteen** -- checked by intersecting its public names against
  `er7`'s interns, which is empty.

`clojure -M:poly check` **OK**. No Error 104: `er7` -> `timelines` is
an intra-component edge.

### The require set, re-derived in BOTH directions

`er7.clj` needs three and uses all three (`parser` 12, `str` 3,
`timelines` 1). `emit_hl7.clj` gained ONE and lost none, checked rather
than assumed: all eight aliases are still called in the residue --
`parser` 160, `er7` 43, `timelines` 18, `hl7-time` 15, `site-profile`
15, `registry` 13, `sim-model` 4, `str` 1. **This move leaves no dead
require behind**, which is now four for four in the emit phase against
the engine phase's nine.

## 4. Steps 3-4 -- the gates

`bin/regression-oracle 04b1e9f c7fbcb5`: **IDENTICAL: every root's
digest matches**, 41 roots, `declared-digest-change: no`, soundness
confirmed outside the leading docstring, exit 0. **This is the
load-bearing gate for the emit phase** -- the eleventh session's finding
-- and it covers the rendered messages themselves, which is exactly what
an escaping layer could break.

`bin/ground-truth-bracket 04b1e9f c7fbcb5`: **IDENTICAL: every digested
root's `:ground-truth` matches (38 roots)**, 3 skipped for carrying no
`:ground-truth` key (appendicitis, ear-infections, sore-throat),
`declared-digest-change: no`, exit 0. **Near-vacuous here and it says so
in its own output** -- "THIS IS NOT A REGRESSION-ORACLE CLAIM: the
`:hl7` half of every root is excluded by construction". Run and reported
because the prompt asks for it, not because it discriminates. It is more
nearly vacuous for this cluster than for any before it: every one of the
nineteen movers exists to serve the wire side, which is the half this
gate excludes.

No soundness abort: nothing this session touched
`components/oracle/src/ehrt/oracle/digest.clj`, the tenth extraction's
hazard class.

### The suite delta, measured IN-CLONE

`make test` unpiped through a wrapper ending in `exit "$MAKE_EXIT"`, run
at `04b1e9f` (the pre-move tree, in the clone, no worktree) and at
`c7fbcb5`. Both `MAKE_EXIT=0`, both **408 zero-failure blocks over 216
distinct namespaces / 4,751 tests** -- so **this move adds no
`deftest`**, confirmed per namespace rather than from the total. 4,751
is the count the thirteenth extraction closed on, and 24,141 is its
closing assertion count, reproduced here to the assertion before
anything moved.

Assertions go **24,141 -> 24,145, +4**, attributed per namespace by
diffing per-namespace counts out of the two logs. **Exactly two
namespaces move, and both were predicted before the move:**

| namespace | delta | why |
|---|---:|---|
| `ehrt.docs-tooling.io-vocabulary-lint-test` | 268 -> 270 | `doseq` over every production `.clj` under `components/*/src` and `bases/*/src`, one `is` per file |
| `ehrt.docs-tooling.sim-emit-hl7-dependency-test` | 28 -> 30 | `doseq` over every `.clj` under `components/sim-emit-hl7/src`, one `is` per file |

Each moves by ONE file and is counted TWICE, because `conformance` and
`ehrt-cli` both run `docs-tooling`'s suite. `er7.clj` is the one new
production source file and both gates count files; it passes the second
on its merits, requiring only `parser`, `str` and an intra-component
sibling. **+4 is the WHOLE delta**, and code-attributable and measured
delta agree for the third session running.

**The close-out run is assertion-for-assertion identical to the move
run** -- 24,145 either way, 408 blocks either way, per-namespace diff
EMPTY -- so this session's doc additions (the record, the prompt
archive, the P5 rewrite, the two regenerated indexes and
`state-derived.md`) move no gate's population. That run was taken
against the FINAL tree: two earlier close-out runs were started and
stopped on purpose after later verification turned up prose corrections
to this record, and only the third, over bytes nothing changed
afterwards, is reported here.

## 5. Closing arithmetic

### The partition, cluster 4 of 8

`emit_hl7.clj` goes 2,065 lines to **1,881**, and 74 def-forms to
**58** -- it lost nineteen and gained three. `er7.clj` is 298 lines / 19
forms plus its `ns`, and is the first namespace in the emitter to
require a SIBLING extraction.

**ERRATUM, fixed forward rather than by amending a gated commit.** The
move commit's own message says "goes 2,065 lines to 1,882". **1,881 is
the figure**, `wc -l` on the file, and it is what the arithmetic gives:
2,065 - 231 moved-out + 46 inserted + 1 require. The scanner that
produced the message counted a trailing element its `split` created.
Nothing else in that message depends on it, and `c7fbcb5` is the commit
both gates are pinned to, so it stands as written.

### Census corrections

* Section 2's `er7` row says "19 forms, 176 lines" in the summary table
  and "217 lines" in the form list; the forms are 19 and the FORM-lines
  are **193**, which is section 2a's own figure, confirmed to the line.
  Section 2a is right and both of section 2's figures measure something
  else. That is now FOUR clusters running.
* Section 2's line spans for this cluster are stale by the three prior
  moves, as expected: `342-347` is now `189-193`, `1591-1599` now
  `1264-1271`, and so on. Re-derived per session by design
  (`.agents/plans/README.md` says so), so this is not an erratum.
* **Section 2a's `er7` claim is confirmed EXACTLY, and it is the first
  edge claim in the emit phase the channel has got right**: "`er7` is
  NOT a leaf -- it carries one outgoing edge into `timelines`". One, and
  only one, in both the bare-name and the qualified-symbol scan.
* **Section 3b's two `er7`-as-callee rows reproduce EXACTLY** on 3b's
  own accounting (18 and 16), and its ABSENCES do too -- no `planners`
  and no `facade` edge. Raw sites are 43 against 34 pairs, the second
  divergence of the two accountings.
* Section 2a's "`er7` owes no `interface.clj` re-export" is confirmed
  exactly, read from `interface.clj` and from a whole-tree grep.
* **New, and not in the census at all**: a banner can head a region a
  cluster SPLITS. The M3 header does, and stays.
* **New, and not in the census at all**: constraint 5 has a case neither
  the first sentence nor the prohibition covers -- a private mover
  reached by `#'` from a C1(a)-fenced test file. It needs a `^:private`
  delegating def, which is neither a widening nor a public def. Cluster
  5 meets it twice (`msh-segment`, `pid-segment`).

### The backlogs

* **FENCED CITATIONS**: **none added**, and section 2f says why.
* **STALE-BEFORE-THIS-MOVE** gains a third row and its first TREE-WIDE
  one: `docs/operational-models.md` cited by TWENTY live files before
  this move (including `location-field`'s docstring, which the move
  relocates, making it twenty-one) for a file whose real path is
  `components/sim/docs/operational-models.md`. Neither fenced nor this
  cluster's to fix.
* **RETIREMENT CANDIDATES**: none added. All three of this cluster's
  delegating defs have named callers -- two through `v2_replay.clj` and
  the test tree, one through `v2_replay_test.clj:261` and
  `pid-segment`.

### Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt's edge expectation is RIGHT, for the first time in four
   sessions.** One outgoing edge, `context-for-event` ->
   `demographics-at`, into `timelines`. No second edge, so no census
   correction on that head, and no symbol still resolving in
   `emit_hl7.clj` -- the stop-and-report condition did not arise.
2. **The prompt's "expect the docstring-citation sweep to be non-empty"
   is half right.** The sweep found citations in quantity -- 41 shingle
   files, three namespace claims, a docstring-as-authority citation --
   and **not one of them owes a repoint**. So there is no hit list and
   no sweep commit; the gate is answered with the disclosed absence and
   every level's result in section 2.
3. **A THIRD prose correction beyond the eleven marker widenings**,
   where the thirteenth session had two. All three named before the move
   in 2g; `rulings.md#R-move-not-improve` is not strained.
4. **The `^:private` delegating def is an extension of C1(a)**, not an
   application of it, and is disclosed as such in section 1 and in the
   commit message. It was the only resolution that kept the suite green,
   the public surface at 24, and the test file untouched.
5. **Three trailing comments in `sch-segment` are left misaligned** --
   section 3, with the reason.
6. **The baseline was measured at `04b1e9f`, the session's start tip**,
   which IS the pre-move tree here (no docs commit precedes the move
   this session), so the thirteenth session's deviation does not recur.

## 6. What is left in this program

Four emit clusters (`segments`, `messages`, `planners`, `facade`), then
the apply-path unification. The next session takes `segments` -- 15
forms, census 2a's fifth, the second-largest cluster in the file (518
form-lines) and the first to depend on THREE landed siblings (`er7`,
`registry`, `hl7-time`).

Two things it should expect, both located rather than guessed:

* **The `#'` class arrives twice.** `emit_hl7_test.clj:688` and `:690`
  reach `#'emit-hl7/msh-segment` and `#'emit-hl7/pid-segment`, both
  private movers of that cluster, both in a C1(a)-fenced test file. This
  session's `^:private` delegating def is the precedent; two of them
  will be owed.
* **`emit_hl7_test.clj:1306` is already stale**, which the thirteenth
  session's finding (A) established and this one confirms: its "registry
  comment" phrase lives at `registry.clj:41`, not in any `segments`
  form. Its disposition to cluster 5 was wrong; there is nothing for
  cluster 5 to repoint, and it is C1(a)-fenced regardless.

## 7. The budget

The thirteenth session's C6(a) compaction left `:onboarding` at **1522
of 1530, 8 lines of headroom**. This session's P5 row update spends
**4** of them, and closes at **1526 of 1530, 4 lines of headroom** --
the first session in the emit phase to hand its successor MORE than it
would need for a bare landing sentence.

**The record and prompt archive cost ZERO reading-set lines, and that
is a measured fact rather than an assumption.** `.agents/session-
records/README.md` and `.agents/prompts/README.md` are in the set, but
neither LISTS files: ADR-0147 moved the listing into `INDEX.md`, which
is generated by `make state-derived` and is NOT a set member. So the
whole 8 lines were available to the row, and this session budgeted 2 of
them for index entries before checking and finding there are none.

The row update is +4 net across two edits: the emit-phase block goes 11
lines to 13 (the eleventh through thirteenth landings compressed to a
`forms/form-lines/defs/widenings` shorthand to pay for the fourteenth),
and the backlog (3) paragraph goes 5 to 7 for its third and first
tree-wide row. The Records list absorbed this session's own path on its
existing last line, at zero cost. **Engine doctrine was not touched**:
C6(a) fenced it, and the prompt fenced it again.

What remains compactable, if a future session needs it, is what the
thirteenth deliberately left: the row's standing doctrine still carries
engine-phase INSTANCE detail, and compacting that is re-triage and
therefore the author's
(`rulings.md#R-section-retriage-is-author-judgement`). Four clusters
remain and 4 lines with them, so a fifteenth session landing `segments`
can pay a one-line sentence without a compaction, and a sixteenth
probably cannot.

## 8. CI at the pushed tip -- the close marker

`gh run watch 33392753142 --exit-status` exits 0; the run is
**completed / success** at `d0be9cf4d71f7b203022f4697c92e357c566aeea`,
the pushed tip
(https://github.com/pragsmike/ehr-testing-tools/actions/runs/33392753142).
That is the close marker under `rulings.md#R-session-verifies-ci-via-gh`,
which the de-scaffold ruling of 2026-08-25 retired as a TAG condition
and kept as this. No tag was paid.

`gh run list` shows exactly ONE run for the two-commit push, at the tip
-- the one-CI-run-per-push fact the twelfth session measured, confirmed
for the third session running. This session had no red-first pair to
spend it on: the sweep owed zero repoints, so there was no deliberate
red to pair.

`bin/post-push-verify 04b1e9f d0be9cf` reports the remote tip matching
HEAD and every commit message in range pure ASCII, with the expected
"reported once, not awaited to conclusion (AR-CI-4)" disclosure at the
moment it ran. Both pushed messages were diffed against the files that
produced them: each differs by exactly one trailing blank line, which is
`git log --format=%B`'s own formatting artifact and not a wrapper
mangling. The push's own gate (`gitleaks detect` over 1,278 commits,
`clojure -M:poly check`) was clean.

**Two commits, and the first is the whole claim**: the output-identical
move (`c7fbcb5`) and this close (`d0be9cf`). The emit phase's fourth
cluster, the program's fourteenth landing, and the first namespace in
either file that is not a leaf.

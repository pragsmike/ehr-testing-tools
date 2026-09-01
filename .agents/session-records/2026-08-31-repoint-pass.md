# The ruled repoint pass: citations, retirements, residuals

Session record, 2026-08-31. HEAD at start `9dffb2b`, confirmed equal to
`origin/main`; working clone clean. Ceremony R30. Program:
`roadmap.md#engine-namespace-extraction-and-apply-unification` (P5), its
three standing backlogs. Ruling **C12(b)**: C1(a)'s test-file fence
LIFTS for this session only and resumes at its close. Doctrine:
`roadmap.md#engine-emit-namespace-extraction` under `## Done`.

`bin/preflight` exit 0, no findings. One DISCLOSED line, expected --
"HEAD is not currently tagged stable-*" (tags retired by the de-scaffold
ruling of 2026-08-25).

## 0. What the pass did, in one paragraph

Four commits. `engine.clj` ends at **12 delegating defs in 527 lines**,
down from 43 in 741; `emit_hl7.clj` at **17 in 398**, down from 26 in
383 -- it gains lines because a retirement that does not retell the
banners it falsifies is not finished. 109 files changed, +1,892/-1,421.
`bin/regression-oracle 9dffb2b <commit>` IDENTICAL at every one, with no
`--declared-digest-change`, and `make test` closed at **4,751 tests /
24,161 assertions across 408 namespaces** -- the baseline figure at
`9dffb2b`, unmoved in all three, which is what a pass that moves no test
and adds no test file owes.

## 1. The manifest, and why it came first

`.agents/plans/repoint-pass-manifest.md` (`cb89f16`), 579 lines, derived
at `9dffb2b` before the first edit. Its instrument is a **reader-aware
scanner that separates CODE from PROSE in one pass** -- string and
`;`-comment spans blanked in one copy of the text and kept in the other
-- because a `git grep` cannot tell a docstring citation from a call
site, and this pass treats the two completely differently. The facade
def tables were read out of the two files rather than transcribed from
the census, whose figures are eighteen landings stale.

**The manifest corrected the records it was derived from, three times:**

| the records said | measured |
|---|---|
| prose citations: twelve fenced, four more | **267** -- 176 fully qualified, 84 alias-shorthand, 7 WRAPPED |
| retirement inventory: fourteen | **40**, once (ii) lands |
| dead requires: nine | **13 and an import** -- four made dead BY the retirement |

The retirement number is the sharpest of the three and the reason the
manifest was worth its own commit: **fourteen was fourteen only while
the tests still reached the facade.** The run session counted defs with
no caller anywhere; the repoint pass creates the condition it was
counting for. Nobody was wrong -- the question changed under the answer.

## 2. Three things the manifest found that no record had

**(a) ALIAS-SHORTHAND PROSE, a second population the qualified scan
cannot see.** 150 tokens of the form `engine/decide`, `emit-hl7/emit` in
live prose. They split three ways and the split is the whole finding:
**66 name an INTERFACE, not a facade** -- the file binds `engine` to
`ehrt.sim-engine.interface` -- so they are TRUE and stay; 25 are a
MOVER'S OWN BANNER counting what the test tree calls; 59 are plain
citations. A blind alias rewrite would have taken all 66 of the first
group.

**(b) WRAPPED CITATIONS.** The eighteenth session recorded this class
about the P5 slug itself -- ten of forty-one live citations wrap across
a line break and a plain `git grep` finds only thirty-three. It applies
to the facades too. A wrap-tolerant join of every adjacent line pair
found fifteen straddling occurrences, **seven of them invisible to the
line-local scan** -- `evolve.clj:26`, `er7.clj:207`,
`latency_test.clj:3` and four demo `config.edn` comments.

**(c) THE `docs/operational-models.md` POPULATION IS A DIRECTORY
CONVENTION, not one file's defect.** The er7 record found the path has
not existed since the sim merge and counted twenty-one live files. There
are 65 sites in 28. But **inside `components/sim/docs/` every sibling is
cited the same way** -- `docs/sim-theory.md`,
`docs/patient-state-model.md`, `docs/event-sourcing.md` -- each with a
working relative link target beside it, a convention inherited whole
from the vendored repo. Repointing one member of it would leave the
directory internally inconsistent. The 43 sites OUTSIDE that directory
were repointed; the 23 inside were not, and are named on the row.

## 3. The one site that must not move, and the one that must not be automated

**Census constraint 1, paid in full.** `engine_test.clj`'s `with-redefs
[engine/stream ...]` stays on the facade var. `run.clj` resolves
`'ehrt.sim-engine.engine/stream` through a lazy shim precisely so that
redefinition is seen; `streams/stream` is a different var and the
perturbation would reach nothing. The `stream-seed` read INSIDE that
`with-redefs` body is a plain call and did move. For the same reason
`engine/stream` is the one name the prose pass keeps: it is a var `run`
actually invokes, not merely a delegating def, and `engine.clj`'s own
docstring already said *do not retire this def*.

**THE AUTOMATED BARE-ALIAS REWRITE IS UNSAFE, found live and reverted.**
A bare `engine/replay` or `emit-hl7/emit` in prose very often quotes
ANOTHER file's call site, and that file may bind the alias to the
INTERFACE. Three true sentences were made false before this was caught:
`emit_hl7.clj`'s account of `digest.clj:228` (which binds `emit-hl7` to
`ehrt.sim-emit-hl7.interface`), `emit.clj`'s account of `interface.clj`'s
runtime calls, and `fold.clj`/`engine.clj`'s VERBATIM QUOTE of
`interface.clj:89`, `(def replay engine/replay)` -- a line the fence
guarantees will keep saying exactly that. The whole pass was reverted and
re-applied under a narrowed rule: **a bare token is rewritten only where
the citing file itself binds the facade alias.** The other 50 were read
by hand, and the ones naming a def step 3 retires were paid in step 3.

**The attic was briefly in scope and is now fenced.**
`.agents/plans/roadmap-done-2026-08.md` is moved-verbatim under the
rotation law (ADR-0161); `roadmap.md`'s own "14 independent
`engine/replay` calls" counts `check.clj`'s calls, which go through
`ehrt.sim-engine.interface` and are untouched by this pass. Neither was
edited.

**Form is preserved, not improved.** A bare citation repoints to the
owner's short name only where that alias is BOUND, and fully-qualified
otherwise: `run/run` in a document that binds no aliases resolves to
less than `engine/run` did.

## 4. The fenced-citation backlog, cleared at 22 -- eight of them new

The records enumerated twelve plus one. Re-derived rather than
transcribed, the class has **22 members**, and the eight additions are
what re-deriving buys: `churn_scenarios_test.clj:57`,
`pathway_test.clj:73`, `consumption_test.clj:17`,
`limitations_test.clj:152`, `segments.clj:250`, and `engine_test.clj`'s
three `engine.clj:480` citations -- which named a LINE, unresolvable to
an owner by line at all, and now name the form and the file that holds
it (`bed-ready-location`, `decide.clj`) with ADR-0171's design HEAD kept
as the historical pointer it always was.

## 5. The retirement, and its one asymmetry

40 of 69. What keeps the other 29 was read out of the two
`interface.clj` files, not assumed: 12 engine defs and 16 emit defs are
on a re-export list. **The twenty-ninth is `unescape-er7`**, and it is
the one asymmetry in the whole pass: `escape-er7` beside it goes,
because only `emit_hl7_test.clj` reached it, while `unescape-er7` has a
SRC caller this pass does not touch -- `v2_replay.clj`, a sibling
implementation reaching its own facade. Manifest row (ii) is about TEST
reaches; a src reach is not this pass's to move.

**THE THREE C7 DEFS ARE GONE, which is the class C12(b) existed to
close.** `^:private` defs of `msh-segment`, `pid-segment` and `tn-field`
existed ONLY to keep a `#'` var access resolving from a fenced test file.
The fence lifted, the three accesses now read `#'segments/msh-segment`,
`#'segments/pid-segment` and `#'er7/tn-field`, and a `^:private` def
with no var access left to answer is nothing at all.

**NINETEEN BANNER CORRECTIONS** are the bulk of step 3's work -- the
RESIDUE-CLAIM class at the scale a retirement makes it. Every cluster's
banner states how many delegating defs it left and why; retiring them
falsifies each of those sentences. In every case the banner had already
named the thing holding its defs up, so **the split it drew is the split
the retirement made** -- which is the phase's own doctrine coming back
as a prediction that held.

## 6. Two red-first findings

**THE HAND-OWNED-ASSET TRIPWIRE FIRED.** Step 2 edited
`docs/dev/simulator-architecture.md`, a registered source for
`gt-emitters.svg`, and this pass did not read that registry's own
SOURCES during its sweep -- which is precisely the RECIPE the extraction
phase recorded, skipped. Reviewed rather than waved through: ONE hunk,
line-neutral, at line 106, inside section 2; the trigger names *section
4's own equations* and that block is BYTE-IDENTICAL across the diff.
Verdict kept `:fresh`, `:reviewed-at` bumped to `b0642355` with the
reasoning. It fired at STEP 3's suite rather than step 2's because the
tripwire reads `git log -1` on the source and cannot see an uncommitted
edit -- register row L1-8/D3-2, met **one commit earlier than the
2026-08-30 precedent**, which CI caught instead. The second row on the
same source is already `:verdict :stale` with a `:stale-row` and owes no
bump.

**A VERIFIER RUN AFTER THE EDITS CAUGHT SIX SITES still naming a retired
def -- THREE OF THEM IN PROSE STEP 3 HAD JUST WRITTEN.** Retelling a
banner as *"`engine_test.clj` called `engine/decide` at ninety sites"*
names a var the same commit deletes. That is why the check runs after
the edits and not instead of them. It closes at ZERO across code and
prose, frozen surfaces out of population, wrap-tolerant so a citation
split across a line break cannot hide.

## 7. Manifest-vs-landed reconciliation

| manifest row | priced | landed |
|---|---|---|
| (i-a) prose, ns-qualified | 176 / 63 files | **LANDED** |
| (i-a2) prose, bare alias | 84 / 42 files | **PARTLY** -- 34 automated where the alias is bound; 50 read by hand, paid where the pass falsified them, LEFT where they stay true. Deviation, section 3 |
| (i-a3) prose, wrapped | 7 / 6 files | **LANDED**, by the qualified pass, which rewrites the namespace token alone |
| (i-b) prose, by-file | 18 priced | **LANDED at 22** -- 4 more found while reading |
| (ii) test code | 976 / 52 files | **LANDED**, 1 site deliberately not moved |
| (iii) retirement | 40 defs | **LANDED**, 31 + 9 |
| (iv) dead requires + form | 13 + import + 1 + 1 | **LANDED** |
| (v) C10 residuals | 5 | **LANDED** |
| (v) found-not-caused | 6 classes | **LANDED**, except the 23 in-directory `operational-models.md` citations -- REFUTED as a defect, they are a convention |

**Nothing was deferred for want of reach.** Two things are handed to the
design channel because they need a ruling and not a session: the
in-directory convention above, and the fact that **the C10 population
was never nine** -- 8 further live citations of a gate `e189418` deleted
sit in `docsgen.clj`, `bases/cli`'s `help_voice_test.clj`, two one-shot
`bin/` migration scripts and a skill `HISTORY.md`. C10(b) named four
files and the planners session refused to widen its own ruling; this
session refuses on the same precedent, and rows the number instead.

## 7b. The budget

`:onboarding` closes at **1500 of 1530, 30 lines of headroom**, down
from 36 at session start: the plan-index row for the manifest costs one
and the P5 row's rewrite costs five. The row got LONGER, which is
unusual for a close, and the reason is that clearing three backlogs
still has to say what was NOT paid -- two things needing a ruling are
worth more than the lines they cost. `:docs` is untouched at -2.

## 8. Deviations from the prompt, all disclosed rather than absorbed

1. **The prompt predicted "test counts should NOT change; assertion
   deltas only from file-population gates if any file is added or
   removed -- expect none".** Exact: 4,751 / 24,161 / 408 at baseline,
   after step 2, and after step 3.
2. **The prompt's "(iii) delegating defs that become caller-less AFTER
   (ii)" is 40, not the row's fourteen.** Section 1.
3. **The prompt's step-2 gate says "oracle IDENTICAL".** Run at each of
   the two code commits separately rather than once at the end, so a
   delta would have been attributable to one of them.
4. **Step 4's own gate, "state-derived regenerated LAST", is met** --
   and it moves no cell, because every prose edit in this pass is
   LINE-COUNT NEUTRAL BY CONSTRUCTION: the rewriter replaces the
   namespace or alias token alone and never rewraps.
5. **One edit was NOT line-neutral and is named**: `engine_test.clj`'s
   `bed-ready-location` passage, where correcting three line citations
   into one form citation needed the paragraph rewrapped. It is a test
   docstring, in no reading set.
6. **THE SESSION CROSSED LOCAL MIDNIGHT and is dated by its start.**
   `cb89f16`, `b064235` and `e0dabb4` carry 2026-08-31 -0400; the
   residuals commit and this close-out fall after 00:00 on 2026-09-01.
   Ruling C12(b) is dated 2026-08-31 and three of the four code commits
   are, so the record and its prompt archive keep that date and the
   pairing stays one slug.
7. **`.agents/state-derived.md` still shows `:docs` at headroom -2.** Not
   this session's -- no path in that set was touched, and the condition
   predates the first commit. Named because `rulings.md#R-budget-stop`
   makes an over-budget set a stop-or-compact for whoever owns it, and
   nothing in the tree escalates this one. It was named the same way by
   the eighteenth session.

## 8b. CI, the close marker

Pushed `9dffb2b..9b328fd`, five commits. `bin/post-push-verify` clean on
all three checks: remote tip matches HEAD, every message in range is
pure ASCII, and the CI run was reported rather than awaited (AR-CI-4).
Each of the five messages was then diffed against the file that produced
it -- the only delta on any of them is one trailing blank line, which is
`git log --format=%B`'s own formatting artifact and not a mismatch.

**CI GREEN at `9b328fd`**, run
[33470321378](https://github.com/pragsmike/ehr-testing-tools/actions/runs/33470321378),
`status=completed conclusion=success` -- `poly check`, `poly test :all
skip:integration`, the NIST supply-chain lock and the generated-doc
freshness regen-and-diff all passing. The freshness step is the one that
matters most to this pass, because it is the gate a prose sweep of this
size could most easily have moved, and it did not.

ONE CEREMONY HAZARD FIRED AND IS RECORDED so it is not rediscovered:
both new archive files landed **`100755`** because they were copied in
from `/mnt/c`, and were `chmod 644`'d before the commit. The staging
check that caught it -- `git diff --cached --summary | grep -i mode` --
is the one the workspace already prescribes for exactly this.

## 9. C1(a)'s FENCE RESUMES AT THIS SESSION'S CLOSE

C12(b) lifted it for this session alone. **It is back in force from this
record onward**: a public mover keeps a delegating def and no test file
changes, until and unless another pass is ruled. What this session
changed is that there is now much less facade for a future mover to keep
-- 12 defs and 17, all but one of them owed to an `interface.clj`.

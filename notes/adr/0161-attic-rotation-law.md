## ADR-0161 — the attic rotation law lands mechanical, and the 13-day backlog rotates

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-20.

### Context

`roadmap.md#attic-rotation-law`, PRIORITY 5, quoted as it stood at
`891e57e`:

> `## Done` holds the current arc only by law, and its pointers have not
> rotated to the attic since the conviction arc closed 2026-08-08;
> deciding a dozen intervening arcs' boundaries is judgement work
> outside a records-only close. ADR-0144 retokened those pointers and
> added six missing ones rather than rotating them, so the backlog this
> row names is larger, not smaller. ADR-0139 finding C-3.

The row states its own cause precisely, and it is worth naming again
because it is the whole point of what changed here. The rotation law was
real (ADR-0055 AR-AC-5, restated in the roadmap's own `## Done` header),
and it was never once executed after 2026-08-08 — because its UNIT was
the arc. A session that closes one ADR cannot say where a dozen earlier
arcs began and ended without doing history, and a records-only close is
chartered not to. So every close read the law, agreed with it, and
deferred it. ADR-0144's migration hit the same wall from the other side:
it retokened all 49 pointers to the new row contract and ADDED six that
were missing, but rotated none, for the same reason a script cannot make
a judgement call either.

The count over that window, from ADR-0159's own table plus this
session's Step 0: **44** pointers at review 3's close, **66** at review
4's finding day, **69** at ADR-0159, **71** today. A section whose
header said "current arc only" held thirteen days and five arcs.

### The author's ruling, 2026-08-20

Verbatim, and encoded here as the law of record:

- `## Done` holds at most **30 LINES** (not rows). The unit is lines
  because the reading-set budget counts lines.
- Rotation is an act of the **CLOSE CEREMONY**: after a session adds its
  CLOSED row(s), if `## Done` exceeds 30 lines, the session rotates
  oldest rows (whole rows, never split) into the current month's attic
  file until <= 30, appending verbatim, chronological order preserved.
- Attic files are **append-only**, one per month, flat under
  `.agents/plans/`; a new month's first rotation creates the file with
  the same header shape as the existing two.
- Nothing pins: a rotated row is recorded **twice over** — its closing
  ADR, and its verbatim attic copy.

And the question that settles the last point, verbatim: *"Anything worth
keeping would already be recorded elsewhere, right?"*

**The answer is yes, and it is structural rather than a hope.** Every
row in `## Done` is tokened `CLOSED <date> <ADR-NNNN|sha>` by the
ADR-0144 row contract, which `ehrt.docs-tooling.roadmap-lint-test`
gates. `ehrt.docs-tooling.done-pointer-adr-test` then gates that the
citation RESOLVES — every Done bullet carries an ADR number or a sha
(its non-vacuity assertion, hardened by ADR-0158), and every ADR number
it carries exists in `notes/ADRs.md`'s own generated index. So a row
cannot reach `## Done` without naming the document that holds its
reasoning, and cannot sit there while that document is missing. The
attic keeps the bytes; the ADR keeps the meaning; the gate keeps the
link between them. Rotation destroys neither.

What the ruling removes is the judgement. Lines, not arcs. This close,
not "the next close that can see a boundary". Oldest first, whole rows.
There is nothing left to decide, so there is nothing left to defer.

### Step 0

`bin/preflight` exit 0, all four OK lines, one DISCLOSED (HEAD not
tagged — no tag was owed). Baseline `make test` unpiped into a log with
`MAKE_EXIT` captured and the wrapper ending `exit "$MAKE_EXIT"`
(`rulings.md#R-full-suite-before-push`): **MAKE_EXIT=0, 364 blocks /
4,070 tests / 18,304 assertions**, reconciling EXACTLY against
ADR-0160's own close figure. `clojure -M:poly check` green (it is
`make test`'s own first line). Reading sets all green, `:onboarding`
1,508 / 1,530 — 22 lines of headroom, the tightest of five for the third
review running.

**The Done section, measured.** 71 rows, 134 lines counting the header,
2026-08-08 to 2026-08-20.

**The reader census** — every consumer of the `## Done` section, because
a rotation must not starve a reader whose population assumes the full
history. Grepped over `*.clj` and `*.edn` for `## Done`, `done-section`
and `roadmap`:

| reader | what it reads | after rotation |
|---|---|---|
| `ehrt.docs-tooling.done-pointer-adr-test` | the LIVE Done section, extracted at read time from the `## Done` header to end of file | **derived, not pinned.** Its population shrinks to the survivors and every one of them still carries a resolving ADR. No edit owed — the one-line re-read the prompt allowed for was not needed. |
| `ehrt.docs-tooling.roadmap-lint-test` | every row of every section, grouped from the file | **derived.** Fewer rows, same contract. Its `every-cited-slug-resolves-test` was the one real risk: a live surface citing a rotated slug would go red. Checked before acting — the three live slug cites (`#lookup-column-time-open`, `#downstream-latency`, `#two-clocks-asset-field-audit`) are all OPEN rows in other sections, none in the rotate set. |
| `ehrt.docs-tooling.state-derived/parse-roadmap-rows` | rows by section and token, rendered into `.agents/state-derived.md` | **derived and regenerated.** `roadmap rows (all sections)` 120 -> 53, `Done` 71 -> 4. |
| `ehrt.docs-tooling.reading-set-budget-test` | `roadmap.md`'s LINE COUNT as an `:onboarding` path | the point of the exercise; see the ratchet section. |
| `ehrt.docs-tooling.stale-path-test` | `roadmap.md` as a live plan file | narrower after, not wider: the rotated bytes were already inside a scanned file and the attic is explicitly out of that test's include-list. A move into an equal-or-narrower population cannot open a violation. |
| `hand-owned-assets.edn` `:stale-row` | the anchor `roadmap.md#two-clocks-asset-field-audit` must still exist | untouched — an OPEN row. |

**No reader assumes the full history.** Zero findings, so the prompt's
STOP condition never armed.

### The partition

Predicted before acting, then executed. Survivors are the newest whole
rows that fit under 30 lines with the header counted:

| | rows | lines |
|---|---|---|
| `## Done` before | 71 | 134 |
| survivors | 4 | 25 (header + 24) |
| rotated | 67 | 109 |

The survivor set, named: **ADR-0160** `#oracle-coverage-gate-
integration-half`, **ADR-0159** `#repo-review-4`, **ADR-0158**
`#edit-root-worktree-residue`, **ADR-0158** `#intake-staging-dir`. A
fifth row would have taken the section to 31.

The boundary is not clean by DATE and the tie-break matters: five rows
carry 2026-08-19 or 2026-08-20 and only four fit. **ADR-0157**
`#commit-msg-ascii-hook` is also 2026-08-19, and it rotates — it is the
oldest of its own day by file order, and file order is age order here
(the section reads newest-first at its head). Nothing was split.

It falls out that the rotation is a **contiguous tail cut**: the live
file's own lines 285-393 at `891e57e`, moved whole.

### Byte preservation, proved three ways

Not a diffstat. A diffstat says how many lines moved; it does not say
they are the same lines.

1. **The appended attic tail is byte-identical to the pre-rotation
   roadmap's lines 285-393.** `cmp` over 109 lines, exit 0.
2. **The pre-rotation attic is an unchanged byte PREFIX of the new
   one.** `cmp` over its first 2,547 lines, exit 0 — nothing above the
   append moved.
3. **Sorted union of (live Done body + rotated block) == sorted
   pre-rotation Done body.** 133 lines, `cmp` exit 0. This is the
   prompt's own read-back and it is the strongest of the three, because
   it is blind to order and therefore cannot be satisfied by an edit
   that happens to preserve position.

The same three ran again for the close's own second rotation, below.

**ORDER IS PRESERVED, NOT SORTED,** and this is a deliberate reading of
"chronological order preserved". The live section was not itself in date
order — its newest six rows read newest-first, the remainder read
oldest-first from 2026-08-08 — so sorting would have been an EDIT, which
the fence forbids outright. It would also have quietly repaired
something that must not be repaired here: since 2026-08-18 one Done row
has been carrying another row's continuation lines (`eeb0299` wrote
ADR-0150's four-line row; `c509e46` inserted ADR-0152's one-line row
after its first line). That is ADR-0159 finding **F-1** and review 5's
watch row **W-10**, registered and deliberately unfixed. Sorting would
have separated the pair and dissolved the specimen. It moves to the
attic in the broken shape it has, and the attic section header says so,
so W-10's probe still has something to find.

### The gate

New namespace `ehrt.docs-tooling.attic-rotation-test`, beside the
row-contract suite ADR-0144 built. Landed **red first** and pushed with
its green successor (`rulings.md#R-red-pushed-with-green`). The red run,
in full: 7 tests, 16 assertions, **1 failure** — `## Done` at 134 lines,
over the 30-line cap by 104. Every other assertion, including the whole
append-only history walk, was green at the red commit.

**(i) The cap.** `## Done` from its header line to the next `## `
heading or end of file, at most 30 lines. Header counted, which is what
makes the number reconcile with the 134 this session measured and with
the extent `done-pointer-adr-test` already reads.

**(ii) Append-only, as "no revision ever deleted a line" rather than
"each revision is a byte prefix of the next".** The prompt offered the
prefix shape and asked which was enforceable. It is not the prefix one,
and the evidence is the attic's own history: of the twelve committed
revisions of `roadmap-done-2026-08.md`, **two insert into the middle of
the file** — `2991a70` (the scaffolding-compaction arc's rotation coming
home a day late, ADR-0055's own disclosed leftover) and `0ebca6d` (the
player arc's close appending two UX-arc pointers into the UX arc's own
section). Both are legitimate; both would make a prefix test red on
history no session may rewrite. What IS true of all twelve, re-derived
with `git diff --numstat` over every consecutive pair, is that **none of
them deletes a line**. That is the enforceable form of "rows verbatim,
append-only", and it is what the gate walks — the file's whole committed
history plus HEAD-versus-working-tree, so the gate depends neither on
`HEAD~1` existing nor on which commit of a push happens to carry a
rotation.

`--numstat` rather than a scan of the unified diff, deliberately: an
attic row begins `- `, so a DELETED row renders as `-- CLOSED ...` and
the obvious `^-[^-]` scan misses precisely what the gate exists to
catch. That is the ADR-0144 F-11 class — a pattern that cannot match its
own subject — avoided by not writing it.

**(iii) The byte-preservation read-back is NOT a test.** The prompt
allowed either. It is a statement about a TRANSITION between two
commits, not a property of a tree. As a test it would either freeze one
migration's constants into a permanent gate whose population can never
grow — the "population is a registry rather than the tree" class ADR-0139
named and this workspace keeps re-finding — or degenerate into (ii). It
is the table above instead.

**Population is the tree, not a list.** The gate enumerates every
`.agents/plans/roadmap-done-<yyyy-mm>.md`, so a new month's first
rotation is gated the moment its file exists, with no test edit
(`rulings.md#R-register-gated-by-its-own-loader`'s own principle). Non-vacuity is asserted directly, in the shape
`rulings.md#R-empty-population-is-red` requires: a gate over zero attic
files, or over a shallow clone with no history to walk, would find
nothing wrong with any of them and must say so. CI checks out at
`fetch-depth: 0`, verified in both workflows.

**Both failure branches are proven against real git**, at one pinned
pair: `5b6e439` (ADR-0144's own migration) cut 1,676 lines out of the
roadmap and moved 939 into the attic, deleting none. The detector sees
the first and does not mistake the second.

### The law's first ordinary application — at its own close

Register hygiene retired `#attic-rotation-law` from `## Next` and wrote
its CLOSED row into `## Done` at six lines. That took the section from
25 to **31**, and the rotation step fired against the session that wrote
it. **ADR-0158 `#intake-staging-dir`, the oldest survivor, rotated** —
verbatim, its own three read-backs green — and the section returned to
25 lines / 4 rows.

Recorded cheerfully, because it is the correct behaviour and it is the
cheapest possible demonstration that the law has no exemption for the
close that made it. The attic therefore carries **two** ADR-0161
sections: the one-time migration of the backlog, and the law's first
ordinary act. They are different things and are headed differently.

### The ratchet — it does not move, and here is the arithmetic

This is a **disclosure**, not the outcome the session prompt predicted.
The prompt said to ratchet `:onboarding` DOWN, and in the same sentence
said to read how ADR-0147 built the mechanism and use it rather than
invent one. Used as built, it does not move.

| | lines |
|---|---|
| `:onboarding` actual at Step 0 | 1,508 |
| after the rotation and the skill sentence | 1,403 |
| after the close's own edits (final) | **1,400** |
| ADR-0147's standing formula: actual x 1.15, round up to nearest 5 | **1,610** |
| committed baseline | **1,530** |

1,610 is ABOVE 1,530, and `.agents/reading-sets-baseline.edn` says a
baseline may fall and may never rise. So `:onboarding` **HOLDS at
1,530** — exactly the shape ADR-0145 disclosed for this same set, for
the same reason, and did not absorb.

Why the formula cannot fall here: the set has grown **180 lines since
ADR-0147 measured it at 1,328**, and this rotation returns 109 of them.
The baseline moves again at an actual of **1,326 or below** — 74 more
lines out of `:onboarding`'s ten paths — and that is the number a future
compaction session should aim at, rather than re-deriving it.

What the session DID buy is real and is the thing `R-budget-stop`
exists for: **headroom 22 -> 130 lines, paid by compaction rather than
by a bump.** Review 5's watch row **W-13** reads *"Under ~30 lines —
expect to compact, and note that `R-budget-stop` makes the bump
unavailable."* It fired at 22 and it was answered by compacting. The
watch-list mechanism paying for itself, twice in two arcs.

The other four sets each gain the same 4 lines (`build-session`'s
SKILL.md is a path in all five) and each holds, all green with room:
`:corpus` 1,836/2,045, `:docs` 739/785, `:judge` 926/1,000, `:sim`
1,278/1,405.

### Surfaces

`rulings.md#R-law-surface-propagation` — an amendment to standing law
lands on every surface stating that law, in the session that rules it.
Four surfaces, all in this session:

- **`.agents/rulings.md`** — `R-done-attic-rotation`, three lines,
  appended (`rulings.md#R-anchored-register-edits`).
- **`.agents/plans/roadmap.md`'s own `## Done` header** — it stated the
  old arc-unit law; it now states the new one and its own cap.
- **`.agents/skills/build-session/SKILL.md` step 15** — register hygiene
  at close now carries the rotation in one sentence, with the history of
  why in the sibling `HISTORY.md` (ADR-0145's split). Both mirrored to
  `.claude/skills/`, byte-equal, as `skill-mirror-currency-test`
  requires.
- **`.agents/plans/README.md`** — the attic file's own index line
  enumerated its two prior rotations; it names the third and says what
  changed about the mechanism, rather than leaving a live register
  asserting a population it never re-enumerated (review 5's **W-12**
  class, avoided).

### Registers

- `roadmap.md#attic-rotation-law`: **OPEN -> CLOSED**, ADR-0161. Its
  PRIORITY 5 slot is simply vacated; the row-contract gate requires
  unique and ascending priorities, not contiguous ones, so nothing else
  renumbers.
- **Review 3 finding C-3 is DISCHARGED.** ADR-0159's disposition table
  carried it as *"ROWED, OPEN, and worse each close"* and did NOT put it
  on review 5's thirteen-row watch-list — the roadmap row was its only
  register home. Closing the row closes the finding, and this ADR is the
  record ADR-0159's table pointed forward to.
- Review 5's watch-list is otherwise untouched, with two rows moved by
  this session's evidence rather than by its edits: **W-13** fired and
  was answered (above), and **W-10**'s specimen relocated intact to the
  attic (above), where its probe — *"does every continuation line sit
  under the row whose subject it names?"* — still runs.

### Close verification

Full `make test`, unpiped, `MAKE_EXIT` captured, wrapper ending
`exit "$MAKE_EXIT"`: **MAKE_EXIT=0, 366 blocks / 4,084 tests / 18,336
assertions**, 14 minutes 3 seconds. `clojure -M:poly check` OK,
`bin/verify-nist-lock` OK.

The delta against Step 0 was reconciled **per namespace rather than by
subtraction** — a `diff` of the two runs' own `<namespace> <tests>
<assertions>` tallies reports exactly two ADDED lines and no changed
ones:

    > ehrt.docs-tooling.attic-rotation-test 7 16
    > ehrt.docs-tooling.attic-rotation-test 7 16

`364 / 4,070 / 18,304` -> `366 / 4,084 / 18,336` is therefore the new
lint namespace and nothing else. The session's own prediction was right
in kind and under-counted the multiplicity by half: every docs-tooling
namespace is executed TWICE by `poly test :all` — once in the
component's own run and once in the run that carries it downstream —
which the Step 0 log already showed for `done-pointer-adr-test` and
which the prediction did not read off it. Recorded because a figure
predicted and then quietly corrected to match is not a prediction (D1-4,
"compare the SETS, not their cardinalities" — the per-namespace diff is
that comparison, and it is what makes the correction checkable).

### Fences honoured

Files touched: `roadmap.md`, `roadmap-done-2026-08.md`, the one new test
namespace, `build-session` SKILL + HISTORY + both mirrors, `rulings.md`
(one row), `.agents/plans/README.md` (one line), `.agents/state-
derived.md` and the two record INDEXes (generated), `notes/ADRs.md`
(generated), this ADR, the session record and the archived prompt.

`done_pointer_adr_test` was NOT edited — the prompt allowed a one-line
re-read of its population scoping if the rotation needed it, and it did
not: that gate derives its population from the file at read time.
`.agents/reading-sets.edn` and `.agents/reading-sets-baseline.edn` were
NOT edited, for the arithmetic reason given above; a compaction ADR that
edits them without a falling formula value would be the bump the ratchet
exists to refuse.

Zero `src`. Zero test deletions. No other roadmap section touched beyond
the retired OPEN row and the new CLOSED one. **NO ROW TEXT WAS EDITED
DURING ROTATION** — the three read-backs are the proof, not the claim.
The oracle was neither touched nor run and no oracle claim is made or
owed: this session changed docs and one test namespace.

### Addendum, 2026-08-20 — CI green at the tip, close tag paid in session

`gh run view 32424193086` at `171ccb67`: **status `completed`,
conclusion `success`**, the run for `docs: ADR-0161 -- attic rotation
law, close`. That is the tag licence's CI condition met by THIS
session's own verification, id and conclusion recorded
(`rulings.md#R-session-verifies-ci-via-gh`), so the close tag is paid
here rather than deferred to a successor's Step 0.

`bin/tag-ceremony stable-20260820-attic-rotation-law 171ccb67 <msg>
--push`: annotated tag created, pushed, and the remote **peeled ref
verified** to `171ccb67c4c998330500625949eaf22d0535db78` — matching the
target exactly.

`bin/post-push-verify 891e57e 171ccb67`, all three checks: remote tip
matches HEAD; every commit message in the range pure ASCII; the CI run
reported once. Only the tip commit drew a run — the red commit
`f834286` was pushed with its green successor and GitHub raised no run
against it, so `bin/preflight`'s five-run window at the next Step 0 is
all green with no red-first artefact to explain away.

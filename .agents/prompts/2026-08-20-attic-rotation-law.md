# Archived prompt: attic-rotation-law (2026-08-20)

Session prompt -- attic rotation law: `## Done` line-capped at 30,
rotation owned by the close ceremony, the 13-day backlog rotated,
the lint co-landed -- ADR-0161

## Context

Claude Code under R30 in ehr-testing-tools. HEAD at handoff: 891e57e
(ADR-0160 addendum; tree clean; CI green at tip; last tag
`stable-20260820-oracle-coverage-integration-half` @d5edf8a, no tag
owed). Roadmap row `roadmap.md#attic-rotation-law` (OPEN PRIORITY 5) --
quote it; ADR-0139 C-3, worsened by ADR-0144 (retokened + added pointers
rather than rotating). Author ruling 2026-08-20 settles the judgement
the row said was owed: the law is MECHANICAL, no arc boundaries.

Channel anchors at 891e57e (re-derive):

* `## Done` today: 71 CLOSED rows / 134 lines, 2026-08-08..08-20.
* Attic files are FLAT: `.agents/plans/roadmap-done-2026-07.md`,
  `-2026-08.md` (ADR-0144 F-1: "attic" is a role, not a path). Read both
  files' current headers/format before appending.
* `done-pointer-adr-test` gates Done rows (distinct-ADR vs bullet
  arithmetic, per ADR-0158's vacuity fix) -- its population must be
  re-read: after rotation it covers the LIVE Done section only; the
  attic needs its own (weaker) integrity: append-only, rows verbatim.
* Roadmap lint suite (ADR-0144): row contract, six-line cap, slug
  anchors -- the new law joins that suite, same file, same shape.
* `:onboarding` 1508/1530. Rotation REMOVES ~104 lines from roadmap;
  budgets are decrease-only ratchets -- ratchet DOWN after
  (`R-budget-stop`'s ratchet direction; read how ADR-0147 set the
  mechanism and use it, do not invent one).

## The law (author-ruled 2026-08-20, encode verbatim in the ADR)

* `## Done` holds at most 30 LINES (not rows). The unit is lines because
  the reading-set budget counts lines.
* Rotation is an act of the CLOSE CEREMONY: after a session adds its
  CLOSED row(s), if `## Done` exceeds 30 lines, the session rotates
  oldest rows (whole rows, never split) into the current month's attic
  file until <= 30, appending verbatim, chronological order preserved.
* Attic files are append-only, one per month, flat under
  `.agents/plans/`; a new month's first rotation creates the file with
  the same header shape as the existing two.
* Nothing pins: a rotated row is recorded twice over (its closing ADR,
  gated by done-pointer-adr; its verbatim attic copy). Author: "Anything
  worth keeping would already be recorded elsewhere" -- confirmed
  structurally in the ADR.

## Read first

1. The row; ADR-0139 C-3 (the finding); ADR-0144 whole (the row contract
   + F-1/F-2 -- the attic path and the frozen-population lesson);
   ADR-0147 (the ratchet mechanism); ADR-0158 (done-pointer vacuity fix).
2. `roadmap.md` whole; both attic files; the roadmap lint tests (find
   them: `roadmap_*_test.clj` in docs-tooling) and `done_pointer_adr_
   test`; `reading-sets.edn` + the ratchet's home.
3. `rulings.md#R-register-hygiene-at-close`, `#R-budget-stop`,
   `#R-full-suite-before-push`, `#R-red-pushed-with-green`,
   `#R-session-verifies-ci-via-gh`, `#R-law-surface-propagation` (the
   close ceremony's surface: build-session skill -- the rotation step is
   one sentence there, both mirrors); build-session skill.

## Author rulings, verbatim

* "line-cap at 30 [lines -- channel-confirmed unit] ... Anything worth
  keeping would already be recorded elsewhere, right?" (2026-08-20; the
  channel answered yes, structurally, and the ADR proves it: every Done
  row cites an ADR, the gate enforces the citation, the attic keeps the
  bytes.)
* Tag: no tag owed at Step 0. Close tag: pay in-session if the tip run
  concludes success while open, else next Step 0 -- say which.

## Step 0

Fresh clone, tip 891e57e; `bin/preflight`; baseline `make test` unpiped,
MAKE_EXIT captured, wrapper ends `exit "$MAKE_EXIT"`, reconcile vs
ADR-0160's 364 blocks / 4,070 tests / 18,304 assertions; `poly check`;
budgets. Measure: Done line/row count; the exact rows that rotate (all
but the newest ~30 lines' worth -- list the survivor set, predict it:
0159/0160's rows and whatever else fits); both attic files' formats;
every reader of the Done section (grep for consumers: done-pointer test,
state-derived collectors, anything else -- the rotation must not starve a
reader whose population assumes the full history; a reader that does is a
finding and a STOP if its fix is not one line).

## Step 1 -- red

Lint test beside the roadmap suite: (i) `## Done` <= 30 lines -- red
today (134); (ii) every attic file append-only relative to HEAD~1
(shape: the test compares `git show HEAD:<attic>` prefix -- or a simpler
standing property: attic rows are never edited, asserted as each row
still byte-present; choose the enforceable one and say why); (iii)
rotation preserved bytes: the union of live Done rows + attic rows for
2026-08 equals the pre-rotation Done set (this assertion is the one-time
migration's read-back, expressed as a test that then degenerates to (ii)
-- if that dual role is awkward, make (iii) the ADR's read-back table
instead and keep the test to (i)+(ii); say which). Commit: "test: red --
Done line-capped at 30, attic append-only (ADR-0161, C-3)"

## Step 2 -- green

The rotation: 71 - survivor rows into `roadmap-done-2026-08.md`,
verbatim, chronological; `## Done` <= 30 lines; `done-pointer-adr` green
over the survivors; the build-session skill's close-step sentence
(+HISTORY, mirror); budgets re-measured and the `:onboarding` ratchet
moved DOWN by the mechanism ADR-0147 built (state the old and new
budget); `state-derived` regenerated. Full `make test`; push red+green
together. Commit: "feat: Done rotates at 30 lines, close-ceremony-owned;
13-day backlog rotated verbatim to the 2026-08 attic; onboarding
ratcheted down (ADR-0161, C-3)"

## Step 3 -- register hygiene

Roadmap `#attic-rotation-law` -> CLOSED (its own row then immediately
subject to the law it made -- if the cap forces rotation at this very
close, do it and note the recursion cheerfully). Register: review-3
watch row C-3 (wherever ADR-0159's inheritance carries it -- read the
review-5 watch-list; if C-3 is on it, mark discharged with the ADR).
`rulings.md`: `R-done-attic-rotation` row, three lines, the law
compressed, cites ADR-0161.

## Close (self-archive FIRST)

Archive to `.agents/prompts/2026-08-20-attic-rotation-law.md`; session
record; ADR-0161 (the law verbatim; the survivor/rotated partition with
counts; the byte-preservation read-back; the ratchet old/new; the reader
census from Step 0), roadmap, session record with `gh run view`
id/conclusion, full `make test` reconciled vs Step 0 (delta = the new
lint namespace only -- predict it), `bin/post-push-verify`, tag per
ruling. Commit: "docs: ADR-0161 -- attic rotation law, close"

## Fences

Files: roadmap, the 2026-08 attic file, the new lint test ns,
`done_pointer_adr_test` ONLY if its population scoping needs the
one-line re-read, build-session skill (+HISTORY, mirror), `rulings.md`
(one row), `reading-sets.edn`/ratchet home, registers; NO row TEXT
edited during rotation -- bytes move, never change (the read-back proves
it: sorted union pre == sorted union post); NO other roadmap section
touched beyond the CLOSED row; NO src outside the one test ns; oracle
untouched-unrun (docs+test only; unclaimed); no test deletions; exit
codes unpiped; ASCII; anchored edits; R-RP. READ-BACK: the partition
counts; the byte-preservation proof; Done's line count after; the
ratchet delta.

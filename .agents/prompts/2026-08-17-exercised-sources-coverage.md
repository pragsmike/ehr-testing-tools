# Archived prompt: exercised-sources-coverage (2026-08-17)

Archived verbatim as received. Three of its premises did not hold against
the live tree and were corrected rather than adapted around -- the register
holds nine rows, not ten; the `bash -c` wrapper was already caught, and its
genuinely-silent sibling (a source yielding zero taught commands) is what
needed the fix; and step (d) as scoped would have been vacuous, so it was
widened. Reasoning in `notes/adr/0148-exercised-sources-coverage.md`
findings F-1/F-2/F-3, ceremony in
`.agents/session-records/2026-08-17-exercised-sources-coverage.md`. The
commit-message wording for steps 2 and 3 departs from the prompt for the
F-2 reason, disclosed in the red commit's own body.

---

# Session prompt -- exercised-sources coverage: every register row is
# gated by construction -- ADR-0148

## Context
Claude Code under R30 in ehr-testing-tools. HEAD at handoff: 5c1d73e
(ADR-0147, compression arc closed). Roadmap row `roadmap.md
#exercised-row-gate-closure` (OPEN PRIORITY 7): "nothing asserts that
EVERY exercised-sources.edn row has a live check-entry freshness case,
so the next row added can go ungated silently, as one of nine already
had. Wanted: a coverage test over the register per rulings.md
#R-population-closure, not more hand-written cases." Channel probe at
5c1d73e (re-derive): the register has 10 rows; `strip-fresh/check-all`
exists (`strip_fresh.clj:218`) and is called by NO test; the per-row
live cases live in `strip_fresh_test.clj` (:162-190) and elsewhere.
Small session, one mechanism, docs-tooling only.

## Read first
1. `components/docs-tooling/resources/docs-tooling/exercised-sources.edn`
   (header contract, all rows); `exercised_sources.clj` (`load-registry`,
   `by-source`); `strip_fresh.clj` (`check-entry`, `check-all`, the
   result shape each extraction returns -- what "fresh" vs "diverged"
   vs "absent script" look like).
2. ADR-0146 U-15 (the two-layered cause: no test + `bash -c` wrapper),
   ADR-0129/0130/0132 (register birth, the extraction kinds).
3. `strip_fresh_test.clj`, `exercised_sources_test.clj`, and every other
   test that calls `check-entry` on a live row (grep) -- these are the
   hand cases the coverage test subsumes.
4. `rulings.md#R-population-closure`, `#R-exercised-implies-gated`,
   `#R-taught-shell-lines-use-expect-eval`; build-session skill.

## Author rulings, verbatim
- Next: "go" on `[exercised-row-gate-closure]` (channel-recommended,
  author-approved). Standing: sessions verify CI via gh; F-3 narrowed.
- Tag: 5c1d73e is ADR-0147's addendum; the arc tag sits at 9b3432a and
  5c1d73e's run 32069841972 is success (author relay + your own gh
  read). No further tag owed at Step 0. This session's own close tag:
  pay in-session if its tip run concludes success while open, else
  next Step 0 -- say which.

## Step 0
Fresh, tip 5c1d73e; `bin/preflight`; baseline `make test` unpiped
MAKE_EXIT captured, reconcile vs ADR-0147's recorded 342 blocks /
3,890 tests / 17,496; `poly check`; reading sets vs baselines.

## Step 1 -- census (docs-only commit)
Every register row: id/source/script/extraction; which test(s) call
`check-entry` on it live (file:test-name), or NONE; the current
`check-entry` result for each (run them: fresh? diverged? absent?).
Every fn in `strip_fresh.clj` reachable from `check-all` and whether a
test exercises it. Open ADR-0148. Commit: "docs: ADR-0148 opens --
exercised-sources coverage census: rows vs live cases, check-all
unreached"

## Step 2 -- red first (tests only)
`exercised_sources_coverage_test`:
- (a) `(check-all (load-registry))` returns one result per row, every
  result :fresh (or the register's own success predicate); a failure
  names the row id, source, script, and the first diverging line.
- (b) sanity: a synthetic registry of two rows, one seeded diverged,
  returns exactly one failure naming that row (B's `\b` lesson: the
  instrument must be shown to fail).
- (c) sanity: a synthetic row whose script wraps a taught line in
  `bash -c` is reported as :unreadable (or the honest name), not
  silently fresh -- U-15's second layer, made a permanent case.
- (d) population: every `bin/usecase-*` and `bin/*-exerciser*` script
  the tree contains that a `docs/**` page cites as its exerciser IS a
  register row (enumerate scripts from the tree, cites from docs, no
  list in the test) -- the dual: a page can't claim "exercised" by a
  script the register never gates.
RED expected on (b)/(c) only if the mechanism lacks the honest
:unreadable classification (probe first; if `check-entry` already
distinguishes it, (c) is green-on-arrival -- say so); (d) may be red
if any page cites an exerciser outside the register (finding). Witness
counts. Commit: "test: red -- exercised-sources coverage over the
register: check-all reaches every row, unreadable wrappers named,
cited exercisers are rows"

## Step 3 -- green
Minimal src: only what (b)/(c)/(d) require (e.g. `check-entry` returns
a distinct classification for a script whose taught lines cannot be
unwrapped, rather than an empty command list that trivially matches).
Do NOT delete the existing hand-written live cases this session (they
are now redundant, not wrong); mark each with a one-line comment
"subsumed by exercised_sources_coverage_test (ADR-0148); retire at the
next docs-tooling test compaction" and open a roadmap row for the
retirement -- move-don't-improve. Green: coverage test, full `make
test` unpiped MAKE_EXIT=0 reconciled; oracle IDENTICAL 35/35; docsgen
no drift.
Commit: "feat: exercised-sources register gated by construction --
check-all over every row, unreadable wrappers classified, cited
exerciser scripts must be rows (ADR-0148)"

## Step 4 -- records
ADR-0148 (census, red/green, any (d) findings and their fixes or rows).
Rulings row: "a register that gates a population is itself covered by
one test over `load-registry`, never per-row hand cases" (or the
existing R-population-closure gains this as its instance -- read it
first). Roadmap: `[exercised-row-gate-closure]` CLOSED -> Done; the
hand-case retirement row OPEN. Session record; prompt archived; tag
per ruling. Commit: "docs: ADR-0148 -- exercised-sources coverage
close"

## Fences
src: docs-tooling only; oracle IDENTICAL; no register row edits except
those (d) forces (disclosed); no test deletions; exit codes unpiped;
`out/` cleared before exerciser runs (they write to it); anchored
register edits; R-RP. STOP (F-3 narrowed) on: a row whose live
check-entry is DIVERGED today (that is a real drift, report before
"fixing" the doc or the script -- which side moved is a ruling); a (d)
finding whose fix needs a new extraction kind.

## Self-archive
`.agents/prompts/2026-08-17-exercised-sources-coverage.md` in Step 4.

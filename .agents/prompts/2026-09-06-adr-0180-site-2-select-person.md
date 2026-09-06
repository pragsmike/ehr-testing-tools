# Session prompt — 2026-09-06 — ADR-0180 site 2: select-person rides a rank/select sweep

Archived verbatim. Record:
[`../session-records/2026-09-06-adr-0180-site-2-select-person.md`](../session-records/2026-09-06-adr-0180-site-2-select-person.md).

---

Session: ADR-0180 site 2 -- select-person rides a rank/select sweep -- 2026-09-06

Context: run/select-person is 19.98% of top-of-decade generate: a filterv over the whole
:population per arrival, O(arrivals^2) under the 2x :persons rule. ADR-0180 s."2."
(notes/adr/0180-indexes-ride-the-fold.md:151-196) states the scan, the shrink-only
property, and the obligation: at EVERY arrival the index yields a vector = to
(filterv alive? population) -- same elements, same ORDER, same count -- because the one
uniform draw resolves positionally through (nth candidates (min ...)). Carrier is
prelude's arrival sweep, NOT apply-events (the ADR's disclosed narrowing). Output-
identical refactor, every commit bracket-proven. Fresh clone at d9088a91 or later.
WSL only. No sub-agents.

Read first: AGENTS.md; SKILL.md (:87-88, :142); ADR-0180 :151-196, :284-334; run.clj
:160-180 (select-person), :230-260 (prelude, arrivals t-ascending), :510-522 (the
second statement of the half-open convention); the site-1 session record (its
test-fold finding); persons_test.clj and engine_test.clj greps for evolve/evolve;
.agents/plans/2026-09-05-performance-measurement/ (driver, profiler, cells).

Author rulings, verbatim and binding:
 R-fold-carrier (law), R-equivalence, R-order (ADR-0180; site 2 enacted by this prompt).
 R-positional: "the replacement answers (count alive) and (kth-alive k) over the
   population's OWN index order; the half-open death test and the min clamp move
   verbatim with the site." Channel expectation for the structure: a Fenwick tree over
   population indices, deletions applied by a cursor over a death-instant-sorted copy
   as t advances; a mutable array local to prelude is acceptable since nothing escapes
   and the operation sequence is determined by population and arrivals alone. Correct
   from the tree if a persistent structure serves.
 R-move-not-improve (ADR-0169). R-pins, R-edit, R-cap (standing).
 R-test-fold (site-1 record, standing for sites 2-4): "tests that hand-roll the loop's
   fold route through fold/apply-events before the site's tests are trusted."

Steps (one gate each; commit message given):
1. bin/preflight. Baseline: a7500-persons and a2500-nopersons, generate only, one timed
   JVM each; shas recorded (expect site-1's: 3018299a..., c22d6573...). Gate: bracket
   IDENTICAL at the clean tip. No commit.
2. Test-fold sweep. persons_test.clj (and engine_test.clj residue) hand-rolled folds
   route through fold/apply-events per R-test-fold; a test that goes red or is found
   vacuous is fixed and named in the record. Invariant: no assertion weakened.
   Gate: make test. Commit: "test: persons tests route through the choke point
   (R-test-fold, ADR-0180 site 2)".
3. Law first, site untouched. Test namespace keeps select-person's filterv VERBATIM as
   naive-candidates; the new rank/select sweep is exercised in a pinned-seed property
   over populations with ties in death instant, deaths AT an arrival instant (half-open),
   nil death, and draws near 1.0: at every arrival, (= (naive t) (vec (alive-in-order)))
   and the selected id equal. Also assert the population is FIXED after prelude starts
   (the ADR's stated-not-assumed fact). Gate: make test. Commit: "engine: rank/select
   sweep with its from-scratch law (ADR-0180 site 2)".
4. Repoint. select-person reads the sweep; draw consumption unchanged (one .nextDouble
   per arrival, unconditionally). Invariant: both step-1 cell logs reproduce byte-for-
   byte. Gate: bracket IDENTICAL + make test. Commit: "engine: select-person reads the
   rank/select sweep (ADR-0180 site 2)".
5. Measure. Same two cells; before/after wall and the site's JFR share at 7,500; dated
   site-2 section in measurements.md; roadmap P1 site-2 line (R-cap; :onboarding
   headroom is 26 lines -- the row edit is net-zero or pays by compaction in the row).
   Gate: make test. Commit: "plans: site 2 measured -- select-person <before> -> <after>".
6. Session record; archive prompt; close ceremony; push; verify CI; close-marker commit.
   Commit: "docs: record CI success at <sha> -- ADR-0180 site 2 close".

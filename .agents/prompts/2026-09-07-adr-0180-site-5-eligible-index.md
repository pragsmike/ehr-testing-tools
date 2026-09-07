# Session prompt — 2026-09-07 — ADR-0180 site 5: decide :merge rides the :eligible-index

Archived verbatim. Record:
[`../session-records/2026-09-07-adr-0180-site-5-eligible-index.md`](../session-records/2026-09-07-adr-0180-site-5-eligible-index.md).

---

Session: ADR-0180 site 5 -- decide :merge rides the :eligible-index -- 2026-09-07

Context: decide :merge is 34.06% of generate at 22,500 (measurements.md:936-996): a
full :patients scan per merge into `eligible` (drawn POSITIONALLY by uniform-choice,
streams.clj:47-49) plus already-merged?, a `some` over the whole log. ADR-0180's
addendum (notes/adr/0180-indexes-ride-the-fold.md:489-521 site 5; :545-571 the
:eligible-index concern; :572-651 R-hash-order, argument, hole, and the hole measured;
:652-703 R-already-merged proof) is the charter; this prompt enacts it for site 5 only.
Output-identical refactor; every commit bracket-proven. Fresh clone at 16f825b7 or
later. WSL only. No sub-agents. R-sentinel: no sleep waiters.

Read first: AGENTS.md (:355-360 -- the sentence rider 1 corrects); SKILL.md (:87-88,
:142); the addendum sections above, in full; decide.clj:1449-1475 (:merge) and
:1414-1432 (:bed-swap -- NOT repointed this session; its view is asserted equal in the
law only); evolve.clj:290-305; fold.clj:540-600 (the concerns), :594-596 (full-algebra);
run.clj:1280-1297 (init-world seeds); the site-3 and site-4 records (raw-read vs throw);
roadmap.md:19-45 (P1 row, rider 2).

Author rulings, verbatim and binding:
 R-fold-carrier, R-equivalence, R-no-second-path, R-membership-from-post-state,
   R-order-2, R-hash-order, R-already-merged (ADR-0180 and addendum; enacted here).
 R-empty-carrier: "the sub-map is seeded from PersistentHashMap/EMPTY, never from {}
   (PersistentArrayMap below nine entries iterates in insertion order -- the addendum's
   second hole)." The law test includes a world of fewer than nine eligible patients.
 R-read-throws: "an absent :eligible-index throws; nil is not a legal answer (addendum
   :565-571)."
 R-agents-sentence (ruling 1a, 2026-09-07): AGENTS.md:358 "no hash-order dependence"
   becomes "no wall-clock; hash-order dependence confined to the churn draws in
   decide :merge and decide :bed-swap, rowed [determinism-hash-order-dependence]". Same
   commit as the repoint. sim/ADR-0002's cite stays; the sentence stops overclaiming it.
 R-roadmap-compact (ruling 2c): P1 row's four site-1..4 lines collapse to one pointer
   line; :onboarding headroom (now 8) must not fall; record the figure.
 R-move-not-improve, R-pins, R-edit, R-cap (standing).

Steps (one gate each; commit message given):
1. bin/preflight. Baseline the two cells (expect 3018299a..., c22d6573...); bracket
   IDENTICAL at the clean tip. No commit.
2. Concern + law, decide untouched. apply-events gains :eligible-index per addendum
   :545-571 (one sub-map, PersistentHashMap/EMPTY seed, membership from post-state);
   generate matrix only, replay and reinstated projections stay out, asserted. Test
   namespace keeps :merge's eligible construction VERBATIM as naive-merge-eligible and
   :bed-swap's as naive-swap-eligible; pinned-seed property over churn-bearing logs
   asserts at EVERY replay entry (= (naive-merge-eligible w pid) (merge-view idx pid))
   and the same for the swap view -- vectors, ORDER included -- plus: the <9-entry world
   (R-empty-carrier); a hand-built world with two ids of equal `hasheq` (construct them:
   the addendum names the minted shape) where the test DOCUMENTS the divergence at the
   collision node rather than asserting equality; and the R-already-merged implication:
   for every :merge event in a generated log, the merged-away id had :status :merged
   in :before iff already-merged? would have been true. Gate: make test + bracket
   IDENTICAL. Commit: "engine: :eligible-index concern at the fold, with its
   from-scratch law (ADR-0180 site 5)".
3. Repoint + riders, one commit. decide :merge reads the merge view; already-merged?
   deleted (R-already-merged; no fallback, R-no-second-path); :bed-swap untouched.
   AGENTS.md:358 per R-agents-sentence. Invariant: both cell logs byte-identical.
   Gate: bracket IDENTICAL + make test. Commit: "engine: decide :merge reads
   :eligible-index; already-merged? retired as redundant (ADR-0180 site 5)".
4. Measure + compact. Two cells, JFR share at 7,500; a22500-nopersons once (digest must
   equal 7d105743...). Dated site-5 section; roadmap P1 per R-roadmap-compact plus the
   site-5 figure. Gate: make test; :onboarding headroom >= 8, recorded.
   Commit: "plans: site 5 measured -- decide :merge <before> -> <after>; P1 compacted".
5. Session record; archive prompt; close ceremony (ps: zero sleep, zero java); push;
   verify CI by sentinel; close-marker commit.
   Commit: "docs: record CI success at <sha> -- ADR-0180 site 5 close".

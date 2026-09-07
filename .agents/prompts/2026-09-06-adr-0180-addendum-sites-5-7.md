# Session prompt — 2026-09-06 — ADR-0180 addendum: sites 5-7

Archived verbatim. Record:
[`../session-records/2026-09-06-adr-0180-addendum-sites-5-7.md`](../session-records/2026-09-06-adr-0180-addendum-sites-5-7.md).

---

Session: ADR-0180 addendum -- sites 5-7: merge, bed-swap, in-run check-all -- 2026-09-06

Context: the post-program profile (.agents/plans/2026-09-05-performance-measurement/
measurements.md:923-1140) puts decide :merge at 34.06% and decide :bed-swap at 31.38% of
generate at 22,500, and the in-run check-all (sim/run.clj:783) at 18.79% of generate,
49.85% of check, and the source of the heap peak. All three are the ADR-0180 shape.
This session writes the addendum that charters them; it enacts nothing. Fresh clone at
13bbad53 or later. WSL only. No sub-agents. R-sentinel: no sleep waiters.

Read first: AGENTS.md; SKILL.md (:87-88, :142); ADR-0180 (whole; :284-334 the law,
:390-end dated corrections); measurements.md:936-1140; decide.clj:1414-1432 (:bed-swap),
:1449-1475 (:merge; already-merged?); evolve.clj:290-305 (the :merged arm); streams.clj
:47-49 (uniform-choice draws POSITIONALLY); run.clj:385 (patient-id-for); check.clj
:925-935 (the (:records folded) carrier); sim/run.clj:775-790 (check-all in-run);
fold.clj:540-600 (the concerns); the four site session records (what each learned).

Author rulings, verbatim and binding:
 R-fold-carrier, R-equivalence, R-raw-read, R-no-second-path, R-membership-from-post-
   state (ADR-0180 and its sites; apply to sites 5-7).
 R-order-2: "merge, then bed-swap, then in-run check-all."
 R-hash-order: "sites 5 and 6 stay output-identical: eligible's ORDER is the hash-map
   iteration order of :patients and is preserved, not replaced. The addendum states the
   argument (relative order of two keys is fixed by their hash bits; collision nodes
   order by insertion) AND its hole (full 32-bit collisions among ~45k minted ids are
   plausible; a sub-map inserted in eligibility order can differ from :patients
   inserted in registration order at such a node), and names the law test at every
   replay entry as the detection. Replacing hash order with sorted order is a declared
   oracle change, deferred to its own row."
 R-already-merged: "already-merged? is removed as provably redundant -- :status :merged
   is set by the :merged arm and excluded by never-mergeable? on both the eligible path
   and the :with path; the addendum carries the proof and the law test states the
   implication over generated logs."
 R-check-once: "site 7 replays zero times in-run: the run hands check-all its own
   projection through the (:records folded) carrier; standalone sim check replays ONCE
   and every invariant reads the shared projection."
 R-move-not-improve, R-pins, R-edit, R-cap (standing; :onboarding headroom 34).

Steps (one gate each; commit message given):
1. bin/preflight. Write the addendum as a dated section of ADR-0180 (not a new ADR):
   sites 5-7 in the s."The four sites" format -- scan, index, draw/allocation status,
   equivalence argument, obligation; the :eligible-index concern as ONE sub-map with
   two views (merge: status not in #{:new :merged}; bed-swap: admitted with location);
   R-hash-order's argument and hole verbatim; R-already-merged's proof; site 7's design
   (projection handed in-run; replay-once standalone; what the fourteen invariants
   read; the :warm-up-mark omission at the replay site stays). Sequencing per R-order-2.
   Every line-cite verified against the tree (site-4 lesson). Gate: make state-derived
   diff clean. Commit: "docs: ADR-0180 addendum -- sites 5-7 chartered".
2. Rows. roadmap P1: the three sites as one pointer line. NEW row [determinism-hash-
   order-dependence] (churn draws depend on PersistentHashMap iteration order; a
   Clojure-version change could move every churn corpus; retire only as a declared
   oracle change) citing the addendum. NEW row or existing-row line for the two
   redundant-scan findings the profile made (:bed-swap and :merge patients scans,
   already-merged? log scan). Gate: make test. Commit: "docs: roadmap -- sites 5-7
   pointer; determinism hash-order hazard row".
3. Session record; archive prompt; close ceremony (ps: zero sleep, zero java); push;
   verify CI by sentinel; close-marker commit.
   Commit: "docs: record CI success at <sha> -- ADR-0180 addendum close".

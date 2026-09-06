# Session prompt — 2026-09-06 — ADR-0180 site 1: waiting-boarder rides the fold

Archived verbatim. Record:
[`../session-records/2026-09-06-adr-0180-site-1-waiting-boarder.md`](../session-records/2026-09-06-adr-0180-site-1-waiting-boarder.md).

---

Session: ADR-0180 site 1 -- waiting-boarder rides the fold -- 2026-09-06

Context: waiting-boarder is 30.54% of top-of-decade generate (measurements.md:216-240),
a per-call scan of (:patients world) at two callers. ADR-0180 s."1. waiting-boarder"
(notes/adr/0180-indexes-ride-the-fold.md:103-150) states the index, the draw-affecting
argument and the equivalence obligation: IDENTITY of the returned id, nil-vs-non-nil
included. This is an output-identical refactor (S1(a) exempt): every commit bracket-
proven, ground truth moves by zero bytes. Fresh clone at bba3a63c or later. WSL only.
No sub-agents.

Read first: AGENTS.md; SKILL.md (:87-88, :142); ADR-0180 :96-150, :284-334; decide.clj
:965-995 (waiting-boarder), :1190-1200 and :1270-1285 (the two callers); fold.clj:540-600
(the :bed-index concern -- the pattern to copy); ADR-0169 (naive-* idiom, R-move-not-
improve); .agents/plans/2026-09-05-performance-measurement/ (driver, cells).

Author rulings, verbatim and binding:
 R-fold-carrier, R-equivalence, R-order (ADR-0180, enacted for site 1 by this prompt).
 R-membership-from-post-state: "the concern recomputes each participant's boarder
   membership from its post-state after every event -- never per-event-kind add/remove
   logic." Eviction cases are then not enumerated, and cannot be missed.
 R-move-not-improve (ADR-0169): the scan body moves to the test namespace verbatim.
 R-pins, R-edit, R-cap (standing).

Steps (one gate each; commit message given):
1. bin/preflight. Baseline: driver's a7500-persons and a2500-nopersons cells, generate
   only, one timed JVM each; log sha256s recorded. Gate: bin/ground-truth-bracket at the
   clean tip reports IDENTICAL on every root (the instrument's own zero). No commit.
2. Concern + law, decide untouched. fold/apply-events gains :boarder-index -- home-ward
   -> sorted-set of [admitted-at patient-id] -- as a projection concern in full-algebra,
   maintained per R-membership-from-post-state from the same pre/post participant pair
   :bed-index reads. Test namespace keeps waiting-boarder's scan body VERBATIM as
   naive-waiting-boarder; a pinned-seed property over churn-bearing generated logs
   asserts, at EVERY replay entry and for every ward, (= (naive ...) (index-answer ...))
   including the excluded-id case and nil. Invariant: shipped behaviour unchanged (decide
   still scans). Gate: make test AND bracket IDENTICAL. Commit: "engine: :boarder-index
   concern at the fold, with its from-scratch law (ADR-0180 site 1)".
3. Repoint. Both callers read the index: ffirst of the home-ward's set minus the excluded
   id; :1275's world' argument is shown irrelevant (:patients-identical) in a test.
   Nothing else at either call site changes. Invariant: the two step-1 cell logs
   reproduce byte-for-byte. Gate: bracket IDENTICAL + make test.
   Commit: "engine: waiting-boarder reads :boarder-index (ADR-0180 site 1)".
4. Measure. Same two cells, generate only; record before/after wall and the site's JFR
   share at 7,500 (profiler from the measurement dir). Append to measurements.md as a
   dated site-1 section; roadmap P1 row's site-1 line gets the figure (R-cap).
   Gate: make test. Commit: "plans: site 1 measured -- waiting-boarder <before> -> <after>".
5. Session record; archive prompt; close ceremony; push; verify CI; close-marker commit.
   Commit: "docs: record CI success at <sha> -- ADR-0180 site 1 close".

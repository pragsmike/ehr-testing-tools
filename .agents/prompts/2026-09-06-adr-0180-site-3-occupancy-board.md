# Session prompt — 2026-09-06 — ADR-0180 site 3: occupancy-board rides the fold

Archived verbatim. Record:
[`../session-records/2026-09-06-adr-0180-site-3-occupancy-board.md`](../session-records/2026-09-06-adr-0180-site-3-occupancy-board.md).

---

Session: ADR-0180 site 3 -- occupancy-board rides the fold -- 2026-09-06

Context: sim-model/occupancy-board is the top innermost-project frame at 7,500 after sites
1-2 (11.87%; 17.11% inclusive at top of decade): an into {} over the whole :patients map
per placement, P = whole population from the first event. ADR-0180 s."3." (notes/adr/
0180-indexes-ride-the-fold.md:199-247) states the index, the value-sense allocation
argument, the two wrinkles, and the same-commit charter amendment. Output-identical
refactor; every commit bracket-proven. Fresh clone at 2149db0a or later. WSL only.
No sub-agents. ADR line-cites into decide.clj are 23 lines stale; use the ones below.

Read first: AGENTS.md; SKILL.md (:87-88, :142); ADR-0180 :199-247, :284-334; facility.clj
:40-90 (board, free), :113-118 (choose), :145-158 (ward-census); decide.clj :1028-1036,
:1082-1090, :1155-1162 (the dissoc query), :1360-1368; log_index.clj:170-200 (:182 reads
board VALUES of a reinstated world); fold.clj:540-600 (:bed-index and :boarder-index
concerns -- copy the pattern), :400-415 (reinstated projection, "inert twice over");
components/sim-model/docs/charter.md:170-182; the site-1 and site-2 session records.

Author rulings, verbatim and binding:
 R-fold-carrier, R-equivalence, R-order (ADR-0180; site 3 enacted by this prompt).
 R-double-occupancy: "the index is specified on no-double-occupancy worlds; the equality
   law quantifies over generated logs; on a double-occupancy input the index answers
   last-writer-by-event-order and the record says so."
 R-charter-same-commit (ADR-0180): sim-model charter :170-182 is amended in the SAME
   commit as the repoint -- the board stays a definition; the index is proven equal to it.
 R-min-clamp-correction: "ADR-0180's sentence that the min clamp 'keeps the index in
   range' gets a dated correction: defensive, never fires in IEEE-754; the clamp stays."
 R-membership-from-post-state (site 1), R-move-not-improve, R-pins, R-edit, R-cap.

Steps (one gate each; commit message given):
1. bin/preflight. Baseline the two cells (expect shas 3018299a..., c22d6573...); bracket
   IDENTICAL at the clean tip. No commit.
2. Concern + law, sites untouched. apply-events gains :board (bed-id -> patient-id) from
   the same pre/post participant pair, per R-membership-from-post-state; opt-in for the
   generate matrix sites only (replay stays out, as sites 1-2 chose -- assert the absence).
   Decide whether the reinstated projection (fold.clj:400-415) carries :board live or
   log_index:182 keeps the definition; either is correct, record which and why. Test
   namespace keeps occupancy-board VERBATIM as naive-board; pinned-seed property over
   churn-bearing logs asserts (= (naive-board (:patients w)) (:board w)) at EVERY replay
   entry, plus the dissoc query shape (= (naive-board (dissoc patients pid)) (masked
   index pid)), plus one explicit double-occupancy case documented per
   R-double-occupancy. Gate: make test + bracket IDENTICAL.
   Commit: "engine: :board concern at the fold, with its from-scratch law (ADR-0180 site 3)".
3. Repoint + charter, one commit. The four decide sites read (:board world); :1159's
   dissoc becomes the masked read; log_index:182 per step 2's decision. Charter :170-182
   amended in this commit. Invariant: both cell logs byte-identical.
   Gate: bracket IDENTICAL + make test. Commit: "engine: occupancy-board reads :board;
   sim-model charter -- the board is a definition, proven equal (ADR-0180 site 3)".
4. Docs riders (script file, R-edit): ADR-0180 dated correction per R-min-clamp-
   correction; ADR-0180's four decide cites refreshed to current lines in the same dated
   note. Gate: make state-derived diff clean; make test.
   Commit: "docs: ADR-0180 -- min-clamp sentence corrected; site cites refreshed".
5. Measure. Same two cells; before/after wall; JFR share at 7,500; dated site-3 section in
   measurements.md; roadmap P1 site-3 line (R-cap; :onboarding headroom is the budget --
   net-zero or compact within the row). Gate: make test.
   Commit: "plans: site 3 measured -- occupancy-board <before> -> <after>".
6. Session record; archive prompt; close ceremony; push; verify CI; close-marker commit.
   Commit: "docs: record CI success at <sha> -- ADR-0180 site 3 close".

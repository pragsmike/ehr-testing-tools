# Session prompt — 2026-09-06 — ADR-0180 site 4: last-uncancelled-index rides the fold

Archived verbatim. Record:
[`../session-records/2026-09-06-adr-0180-site-4-last-uncancelled-index.md`](../session-records/2026-09-06-adr-0180-site-4-last-uncancelled-index.md).

---

Session: ADR-0180 site 4 -- last-uncancelled-index rides the fold -- 2026-09-06

Context: log-index/last-uncancelled-index is the largest named site under decide after
sites 1-3 (21.63% at 7,500): two whole-log passes per cancel decide. ADR-0180 s."4."
(notes/adr/0180-indexes-ride-the-fold.md:250-283) states the index, the not-draw-
affecting argument, and F-3's condition: the SAME carrier answers the query with NO
second code path. Output-identical refactor; every commit bracket-proven. Fresh clone at
228e2c62 or later. WSL only. No sub-agents.

Read first: AGENTS.md; SKILL.md (:87-88, :142); ADR-0180 :250-283, :284-334; log_index.clj
:96-116 (the scan: already-cancelled is a set of :cancels-event-id over ALL cancel-type
events; the result index is what decide writes back INTO :cancels-event-id); decide.clj
:1372-1382, :1715-1725, :1746-1756 (the three callers); fold.clj :400-415, :526-541
(last-cited-index's fallback shape -- what NOT to copy), :540-600 (the concerns), :594-596
(full-algebra); run.clj init-world (:board {} seeding from site 3); the site-3 record
(raw-read decision; poly test one-brick-per-invocation).

Author rulings, verbatim and binding:
 R-fold-carrier, R-equivalence, R-order (ADR-0180; site 4 enacted by this prompt).
 R-no-second-path (F-3, ADR-0169): "the index is the only implementation in src; the
   from-scratch scan lives in the test namespace as naive-*. No fallback in src."
 R-raw-read (site 3, standing): reads are (:cancel-index world) with init-world seeding;
   a missing index throws, it does not rebuild.
 R-move-not-improve, R-pins, R-edit, R-cap (standing).

Steps (one gate each; commit message given):
1. bin/preflight. Baseline the two cells (expect shas 3018299a..., c22d6573...); bracket
   IDENTICAL at the clean tip. No commit.
2. Concern + law, callers untouched. apply-events gains :cancel-index, one concern with
   two maps maintained per event from the event itself (not from patient state):
   [patient-id event-type] -> vector of log indices, appended in log order for every
   participant patient-id; cancel-type -> set of :cancels-event-id. Query: last element
   of the vector not in the set (walk from the end). Generate-matrix opt-in only; replay
   and the reinstated projection stay out, asserted positively. init-world seeds it.
   Test namespace keeps the scan VERBATIM as naive-last-uncancelled-index; pinned-seed
   property over churn-bearing logs asserts equality (integer or nil) at EVERY replay
   entry for every (patient, event-type, cancel-type) the callers ask, including the
   already-cancelled case, the never-existed case, and a re-admit after cancel-admit
   (the LAST-not-first subtlety). Invariant: shipped behaviour unchanged (decide still
   scans). Gate: make test + bracket IDENTICAL.
   Commit: "engine: :cancel-index concern at the fold, with its from-scratch law
   (ADR-0180 site 4)".
3. Repoint. The three callers read the index; last-uncancelled-index's src body becomes
   the index read (same signature, same nil contract) or the callers call the index
   directly -- one of the two, per R-no-second-path; the record says which. Invariant:
   both cell logs byte-identical. Gate: bracket IDENTICAL + make test.
   Commit: "engine: cancel decides read :cancel-index (ADR-0180 site 4)".
4. Measure. The two cells; JFR share at 7,500; AND one timed generate of a22500-
   nopersons (its 007deea6-era figure is 1,472.56 s) to give the program's first post-
   fix local slope. Dated site-4 section plus a program-summary table (site by site,
   cumulative at 7,500) in measurements.md; roadmap P1 row: the four site lines collapse
   to one cumulative line (net-negative on :onboarding). Gate: make test.
   Commit: "plans: site 4 measured; ADR-0180 program summary -- <cumulative>".
5. Session record; archive prompt; close ceremony; push; verify CI; close-marker commit.
   Commit: "docs: record CI success at <sha> -- ADR-0180 site 4 close".

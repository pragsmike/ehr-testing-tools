# 2026-09-05 — performance measurement (prompt as issued)

Archived verbatim. The session's own record is
[`../session-records/2026-09-05-performance-measurement.md`](../session-records/2026-09-05-performance-measurement.md).

---

Session: performance measurement -- generate vs check slopes, profile, scenario census -- 2026-09-05

Context: ADR-0169's generate slope 1.786 and check slope 1.814 were measured 2026-08-29
on a configuration that no longer exists, before the validator hoist (642d70a) and the
board/cancel-index sites were ranked by inspection only. The Scale table's wall is
`corpus generate` WITH emission; nothing measures `sim run --format ground-truth`, which
is the prime audience's invocation. This session measures; it changes no engine code.
Fresh clone at 3114dbfe or later. WSL only. No sub-agents. Everything this session
produces is COMMITTED -- the 2026-08-29 cells died in scratch and the P1 row says so.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md (:87-88, :142); roadmap.md:19-30
(P1 row); notes/adr/0169-*.md (method, the site list); consuming-ground-truth.md:556-600
(what the Scale wall is and is not); demos/scenarios/dense-7500/README.md and config.edn
(:57-92 the facility); fold.clj:540-600 (replay projection: world-before/after per entry);
docs/future-features.md:134-145 (summarize entry); .agents/reading-sets.edn (:docs 785/785).

Author rulings, verbatim and binding:
 R-measure-first: "measurements before the fix; no engine change this session."
 R-commit-cells: cell configs, driver, census script and raw timings are committed under
   .agents/plans/2026-09-05-performance-measurement/ -- nothing lives in scratch.
 R-summarize-widen: future-features.md's summarize entry widens to a scenario inventory;
   the :docs reading set is decrease-only -- pay by compaction within the same file.
 R-edit, R-cap (standing).

Steps (one gate each; commit message given):
1. bin/preflight. Cells: dense-7500's config.edn at arrivals 7,500 / 22,500 / 67,500, wards'
   :beds and :surge-slots scaled x1/x3/x9 so no cell halts on :capacity-exhausted (if one
   does, halve its arrivals and record it). Two variants: as-is, and :persons removed.
   Commit the six configs with a README naming the derivation. Gate: `sim run` of the
   smallest cell exits 0. Commit: "plans: performance-measurement cells committed".
2. Driver script: per cell, warm-up + two timed JVMs of `sim run --format ground-truth >
   log.edn` and two of `sim check` over that log, /usr/bin/time -v, event count, per-kind
   census (bin/event-census), heap max. Table in the plan file: cell, events, generate
   wall, check wall, slope generate and check over the decade, persons on/off. Invariant:
   both timed runs of a cell produce byte-identical logs. Gate: the table has all cells.
   Commit: "plans: generate and check slopes re-measured, persons on and off".
3. Profile the 67,500 generate and check runs with JFR (zero-dep: channel expectation
   `clojure -J-XX:StartFlightRecording=filename=X.jfr,settings=profile ...`, then
   `jfr print --events jdk.ExecutionSample` aggregated by top frame -- correct from the
   JDK 21 docs). Record the top-15 self-time frames per phase, and name which of
   ADR-0169's sites and the 2026-09-05 inspection's three (occupancy-board fold,
   :discharge sort-by, last-uncancelled-index) appear, with share. Commit the .jfr files
   only if < 5 MB each; otherwise the aggregated table. Gate: table present for both
   phases. Commit: "plans: JFR profiles, generate and check, 67,500 arrivals".
4. Scenario census: a committed Clojure script over fold/replay's projection counting, per
   cell: results landing after the subject's transfer / discharge / merge (compare the
   result's stamped :location and subject against :before), merges absorbing a
   bed-holder, cancels reinstating a bed, peak ward census vs capacity. Table in the plan
   file. Invariant: script is pure over the log; same log, same counts. Gate: the merge
   column is zero on every cell OR the record says which cell has the first witness.
   Commit: "plans: scenario census over the measurement cells".
5. Docs. future-features.md summarize entry widens per R-summarize-widen (script file,
   R-edit), citing the step-4 script as the hand-run precursor; compaction elsewhere in
   the file so :docs stays <= 785. roadmap.md P1 row: replace "NOT re-profiled" with the
   step-2/3 figures and the ranked next fix. make state-derived. Gate: make test.
   Commit: "docs: summarize entry widened to scenario inventory; P1 row re-ranked".
6. Session record (timings and shares as recorded evidence; the ranked fix as a
   recommendation for author ruling, NOT enacted); archive prompt; close ceremony;
   push; verify CI; close-marker commit recording the CI success sha.
   Commit: "docs: record CI success at <sha> -- performance measurement close".

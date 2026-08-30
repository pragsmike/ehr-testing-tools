# Engine extraction 7 of N — config and assignment

Archived verbatim. Paired record:
[`.agents/session-records/2026-08-30-engine-extraction-config-assignment.md`](../session-records/2026-08-30-engine-extraction-config-assignment.md).

---

SESSION: engine extraction 7 of N — config and assignment (two small
leaves, one commit each)
Repo: pragsmike/ehr-testing-tools, tip (b177982 or descendant).
Roadmap row P5. Rulings: C1(a) — public movers get delegating defs,
private movers none; S1(a). This session lands TWO namespaces in TWO
separate extraction commits, each independently bracket-proven.

READ FIRST
- Census §1 `assignment` (weighted-pick, assign-pathway,
  assign-module) and `config` (config-keys, Persons, Scheduling,
  valid-scheduling?, valid-persons?), §3a, §5.
- The log-index record: the census's privacy renderings erred 4×
  there — derive every marker; and a delegating def can be owed to a
  TEST file even when interface.clj owes nothing (check
  engine_test.clj and friends for every public mover).

DERIVE FROM THE TREE: markers; spans (six extractions stale);
interface.clj re-exports (channel expects config-keys and
valid-persons? are ON the list — verify); test-file call sites of
every public mover; each cluster's outgoing edges by whole-symbol
scan (channel expects assignment → streams possible, config → none;
correct from the tree) — anything still resolving in engine.clj is
stop-and-report.

STEPS (one gate each; full make test before every push)
1. Confirm tip; re-derive everything above for BOTH clusters.
   Gate: recorded.
2. ONE constraint-6 sweep covering both clusters, all levels
   including cross-brick prose attributions of private movers and
   prior-extraction banners; dispositions committed before either
   move; predicted reds RED-FIRST with successor. Commit: "docs: the
   config and assignment clusters' pre-move citation sweep".
   Gate: hit list first.
3. Extract ehrt.sim-engine.config. Commit: "refactor: extract config
   namespace from engine.clj — output-identical". Gate: suite delta
   zero or explained to the assertion (io-vocabulary
   +1-per-file-per-project known benign); bin/regression-oracle
   IDENTICAL, no declaration — delta = defect, stop and report.
4. Extract ehrt.sim-engine.assignment. Same commit shape, same
   gates, plus bin/ground-truth-bracket IDENTICAL spanning the sweep
   sha → this sha (both moves inside one bracket is fine; name it).
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections one sentence each; P5 row → eight
   landings; require sets confirmed against step 1. Note for the
   record: after this session decide is the LAST cluster before the
   run/residual question — flag anything you see that bears on
   whether run should extract or remain as engine.clj's residue; the
   channel owes the author that choice before session 8.
FENCES: no interface.clj edits; no behavior or draw-order changes; no
var renames; no emit_hl7.clj; oracle IDENTICAL or stop; if either
cluster surprises (unexpected edge, unexpected public surface), land
what's proven and stop rather than force the second move.
SELF-ARCHIVE: prompt and record in the final push.

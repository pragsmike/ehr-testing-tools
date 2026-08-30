# Session prompt -- engine-namespace-extraction opener (2026-08-29)

Archived verbatim (`rulings.md` self-archive convention). Record:
`.agents/session-records/2026-08-29-engine-extraction-opener.md`.

```
SESSION: engine-namespace-extraction opener -- charter + first extraction
Repo: pragsmike/ehr-testing-tools, work from tip (517a96d or descendant).
Roadmap row: [engine-namespace-extraction-and-apply-unification] (P5).
This session charters the namespace map FROM THE TREE and extracts the
least-entangled namespace as proof of method. Later sessions take one
namespace each; application-path unification comes last, against the
census this session records. S1(a) is in force: equivalence proof
replaces red-before-green for pure refactors.

READ FIRST
- .agents/plans/roadmap.md -- the P5 and P6 rows (P6 names the unified
  apply path as its injection point; your census serves both).
- components/sim-engine/src/ehrt/sim_engine/engine.clj ns docstring.
- docs/consuming-ground-truth.md section on determinism (warranty you
  must not move).

AUTHOR RULINGS (verbatim, binding)
- "intra-brick extraction of engine.clj ... and emit_hl7.clj ... into
  cohesive namespaces behind unchanged interfaces, FOLLOWED by
  application-path unification ...; one program, each commit
  output-identical and bracket-proven" (roadmap P5, 2026-08-29).
- C1(a): engine.clj remains the namespace every existing requirer
  resolves against; moved public vars get delegating defs; no test
  file changes this program until a ruled repoint pass.
- C2(b): first extraction is the RNG-stream + id-minting cluster.

STEPS (one gate each; full `make test` before every push)
1. Fresh clone; re-derive all line numbers yourself (channel's are at
   517a96d and stale on arrival). Gate: tip confirmed in the record.
2. Census, committed as .agents/plans/engine-extraction-census.md:
   (i) defn clustering of engine.clj and emit_hl7.clj with proposed
   namespace names + line spans at your sha; (ii) cross-seam call
   census (which cluster calls which); (iii) apply-path inventory --
   every site where events enter the ground-truth log or a state fold
   (run's in-loop fold, replay, prelude's pre-loop paths, any others
   you find), each with file:line at your sha. This doubles as the
   unification census. Commit: "docs: engine extraction census -- the
   namespace map and the apply-path inventory". Gate: census names
   every decide/evolve method and top-level def exactly once.
3. Extract the RNG-stream + id-minting cluster to
   ehrt.sim-engine.streams (name yours if census disagrees; say why).
   engine.clj requires it and delegates; no other file changes; no
   reformatting of unmoved code. Commit: "refactor: extract streams
   namespace from engine.clj -- output-identical". Gate: suite delta
   zero; `bin/regression-oracle` IDENTICAL (no declaration -- an
   oracle delta here is a defect, stop and report).
4. `bin/ground-truth-bracket` as second instrument. Gate: IDENTICAL.
5. Push; verify CI green via `gh run view`. Gate: CI green at tip is
   the close marker. Session record notes census corrections to the
   channel's cluster read, one sentence each.

FENCES: no interface.clj edits; no draw-order or seam changes; no var
renames; no emit_hl7.clj changes this session; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt to .agents/prompts/, record to
.agents/session-records/, both in the final push.
```

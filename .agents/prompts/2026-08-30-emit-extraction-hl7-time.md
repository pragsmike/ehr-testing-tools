# Session prompt: emit_hl7 extraction 1 -- order derivation + first cluster

Archived verbatim, 2026-08-30. The session it drove is
`.agents/session-records/2026-08-30-emit-extraction-hl7-time.md`, which
landed the P5 program's ELEVENTH namespace and the FIRST of
`emit_hl7.clj`'s eight -- `hl7-time` -- after committing the emit
extraction order into the census as section 2a.

Two of the prompt's own expectations were corrected from the tree
rather than met, and both corrections are in the record: the channel
offered "hl7-time or er7" as the leaf and `er7` is not one (the leaves
are three), and the prompt predicted the `gt-emitters` tripwire would
fire for a sixth time, which it does not -- `simulator-architecture.md`
names EMITTER forms as bare names, never by defining form.

---

SESSION: emit_hl7 extraction 1 — order derivation + first cluster
Repo: pragsmike/ehr-testing-tools, tip (74a8e6a or descendant).
Roadmap row P5, emit phase. Rulings: C1(a) — emit_hl7.clj is the
facade, public movers get delegating defs, private movers widen only
where a caller stays behind, no test file changes; S1(a).

READ FIRST
- Census §2 (the eight-cluster map) and §3b (16 edges) — but the
  census is TEN extractions old with a known privacy-rendering
  defect; derive, don't trust.
- The run session record: the caller-travels require-cycle shape and
  its shim remedy (you may hit the analog when the emit facade's own
  entry points move late); the sweep-vs-bracket collision hazard
  (check whether any repoint touches oracle/digest.clj or bracket
  sources BEFORE committing the sweep, and split it out if so).

STEP 0 — ORDER: from a whole-symbol edge scan of the tree (census
§3b as prior, not authority), derive the emit DAG and commit the
extraction order into the census file as a §2 addendum with a
one-line justification per cluster. Channel expects hl7-time or er7
as the leaf; correct from the tree. Gate: addendum committed.

THEN the standard shape for the FIRST cluster in your derived order:
1. Derive markers, spans, interface re-exports (emit_hl7's own
   interface.clj list — the channel has NOT probed it; derive
   everything), test-file call sites, edges. Gate: recorded.
2. Constraint-6 sweep, all levels (ten sessions of banners;
   cross-brick prose; hand-owned-asset sources — predict the
   gt-emitters tripwire, which names emitter internals and has fired
   five times; state-derived regeneration LAST). Commit: "docs: the
   <cluster> cluster's pre-move citation sweep". Gate: hit list
   first; if any repoint touches bracket/oracle sources, isolate it
   in its own disclosed commit.
3. Extract to ehrt.sim-emit-hl7.<cluster>: verbatim; widenings only
   where forced. Commit: "refactor: extract <cluster> namespace from
   emit_hl7.clj — output-identical". Gate: suite delta zero or
   explained to the assertion; bin/regression-oracle IDENTICAL, no
   declaration — delta = defect, stop and report. NOTE: this file is
   the EMISSION layer — the ground-truth bracket is the weaker
   instrument here and the oracle the stronger one, the reverse of
   the engine phase; say so in the record.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; close marker. Record: census §2
   corrections one sentence each; P5 row → eleven landings; require
   set confirmed.
FENCES: no interface.clj edits; no behavior change — emitted bytes
identical is the whole claim; no var renames; no engine-side edits;
oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.

# Emit namespace extraction, 8 of 8: the facade cluster

Driving prompt, archived verbatim (transcribed to ASCII: the original's
em dashes and curly quotes are rendered `--` and `'`). Session record:
[`2026-08-31-emit-extraction-facade.md`](../session-records/2026-08-31-emit-extraction-facade.md).

---

SESSION: emit_hl7 extraction 8 of 8 -- the facade cluster; emit_hl7.clj
becomes a pure facade; the extraction phase CLOSES
Repo: pragsmike/ehr-testing-tools, tip (0bd9ddc or descendant).
Roadmap row P5. Rulings: C1(a) with C7; constraint 5 as prohibition;
S1(a); C11(a) -- the 3 facade-cluster forms move out, emit_hl7.clj
ends as delegating defs only, the engine.clj mirror; and the
C8-flagged Done-migration executes at THIS close (see step 5).

READ FIRST
- Census section 2 `facade` (3 forms, 136 form-lines; callers of
  everything -- the caller-travels case), section 2a, section 3b (facade -> segments
  1, -> messages 4, -> registry?, derive all).
- The planners and run session records: requalification counting;
  the run session's caller-travels shape (no shim expected here --
  after this move nothing REAL remains in emit_hl7.clj to cycle on;
  if a cycle DOES appear, stop and report rather than improvise one).

NAMING: "facade" is the wrong name for the new namespace since
emit_hl7.clj IS the facade. Choose (e.g. ehrt.sim-emit-hl7.assemble
or .emit) and justify in one line.

DERIVE: markers; spans; interface re-exports among the three
(channel expects emit and emit-wire are the brick's load-bearing
entry points -- their delegating defs are what interface.clj resolves
through; verify); test + #' sites; edges/requalifications named in
advance.

STEPS (one gate each; full make test per push)
1. Derivations. Gate: recorded.
2. Constraint-6 sweep, all established levels; dispositions or
   absence first; reds RED-FIRST; state-derived LAST. Gate: met.
3. Extract per C11(a): verbatim except forced requalifications;
   delegating defs for the public movers. Commit: "refactor: extract
   <name> namespace from emit_hl7.clj -- the emitter is now a pure
   facade". Gate: suite delta explained in-clone;
   bin/regression-oracle IDENTICAL, no declaration -- delta = defect,
   stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; CI via gh; close marker. THE CLOSE IS BIGGER THIS TIME, in
   one docs commit before the marker:
   (i) P5 row: the ENTIRE extraction narrative (engine + emit)
   migrates to the roadmap's Done section per its rotation
   convention -- a compact entry naming 18 landings, both facades,
   the census, and the session-record trail; the LIVE row slims to
   the unification arc (staged plan as ruled: stage 1 choke point
   with explicit projections, stage 2 per-pair enablement, deltas
   as findings) plus the standing backlogs (C5 repoint pass, C10
   residual surfaces, retirement inventory).
   (ii) A new roadmap row for the corpus-io ephemeral-port flake the
   17th session backlogged without headroom.
   (iii) Record the facade's final form counts for both files.
   Gate: roadmap-lint green; headroom recorded (expect large).
FENCES: no interface.clj edits; emitted bytes identical; no var
renames; no engine-side edits; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.

# SESSION: emit_hl7 extraction 2 -- the registry cluster

Archived verbatim, 2026-08-30. Paired record:
`.agents/session-records/2026-08-30-emit-extraction-registry.md`.

---

Repo: pragsmike/ehr-testing-tools, tip (b3a79cf or descendant).
Roadmap row P5, emit phase, order per census section 2a addendum. Rulings:
C1(a) -- emit_hl7.clj is the facade; public movers get delegating
defs; private movers widen only where a caller stays behind; S1(a).
[IF C6(a) RULED: this session also compacts the P5 row's engine-phase
narrative to a <=5-line summary pointing at the nine session records --
author-authorized re-triage, done in the record commit.]

READ FIRST
- Census section 2 `registry` (13 forms, 291 span-lines -- 2a's form-line
  figure is the honest one), section 3b, section 2a addendum.
- The hl7-time session record: the shingle sweep's six-word floor --
  patient-simulator/docs/limitations.md pins emit_hl7.clj with a
  FIVE-word phrase inside message-type-registry, which the sweep
  provably cannot see. THIS cluster is where that lands: read both
  charter registers BY HAND, row by row, and disposition that pin
  explicitly. Also: baseline suite runs share the clone -- never a
  git worktree (version-test contamination).

DERIVE FROM THE TREE: markers; spans; emit interface re-exports among
movers; test-file call sites (message-type-registry has heavy test
prose -- the positive control found 15 hits in emit_hl7_test.clj;
those are C1(a)-fenced, disclose as stale-by-fence if they name the
file); edges (census says registry -> hl7-time only; correct from the
tree). Anything still resolving in emit_hl7.clj is stop-and-report.

STEPS (one gate each; full make test before every push)
1. Derivations. Gate: recorded.
2. Constraint-6 sweep: shingles PLUS hand-read registers PLUS
   hand-owned-asset sources PLUS prior banners; dispositions
   committed before the move (or no-sweep-commit disclosed if
   genuinely empty -- the limitations.md pin means it likely is NOT
   empty this time); predicted reds RED-FIRST with successor;
   state-derived regen LAST. Gate: hit list or its absence recorded
   first.
3. Extract to ehrt.sim-emit-hl7.registry: verbatim; widenings only
   where forced. Commit: "refactor: extract registry namespace from
   emit_hl7.clj -- output-identical". Gate: suite delta explained to
   the assertion measured IN-CLONE; bin/regression-oracle IDENTICAL
   (the load-bearing gate this phase), no declaration -- delta =
   defect, stop and report.
4. bin/ground-truth-bracket (near-vacuous here; run and say so).
   Gate: IDENTICAL.
5. Push; CI via gh; close marker. Record: census corrections; P5
   row -> twelve landings [+ the C6 compaction if ruled]; require
   set confirmed.
FENCES: no interface.clj edits; emitted bytes identical is the whole
claim; no var renames; no engine-side edits; oracle IDENTICAL or
stop.
SELF-ARCHIVE: prompt and record in the final push.

---

## Two corrections this session owes the prompt

1. **"census says registry -> hl7-time only"** is not what the census
   says. Section 2a names `registry` as one of THREE LEAVES with zero
   outgoing edges, and section 3b's table carries no `registry`-as-
   caller row. The tree agrees with the census: zero outgoing edges.
   Corrected from the tree, as the prompt itself instructs.

2. **"a FIVE-word phrase"** -- the pinned phrase is "no real
   CarePlan-equivalent segment", FOUR words. The prompt's substantive
   point holds either way: it is under the six-word shingle floor, and
   only the hand-read of both registers found it.

**C6(a) is not ruled** anywhere in the tree, so the bracketed second
task did not fire. See the record's section 7 -- the P5 row is now AT
its budget ceiling, and that ruling is what unblocks the thirteenth
session.

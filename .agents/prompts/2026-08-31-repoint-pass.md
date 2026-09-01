# The ruled repoint pass: citations, retirements, residuals

Driving prompt, archived verbatim (transcribed to ASCII: the original's
em dashes and curly quotes are rendered `--` and `'`). Session record:
[`2026-08-31-repoint-pass.md`](../session-records/2026-08-31-repoint-pass.md).

---

SESSION: the ruled repoint pass -- citations, retirements, residuals
Repo: pragsmike/ehr-testing-tools, tip (9dffb2b or descendant).
Roadmap row P5's backlogs. RULING C12(b), 2026-08-31: C1(a)'s
test-file fence LIFTS for this session only and resumes at its close;
the pass repoints, retires, and corrects -- it does not improve.
Output-identical throughout: bin/regression-oracle IDENTICAL at every
commit, no declaration; a delta = defect, stop and report.

READ FIRST
- roadmap Done entry #engine-emit-namespace-extraction (the doctrine)
  and the live P5 row's three backlogs.
- Session records' disclosed backlog items: the stale test citations
  (decide session: twelve across ten files; fold: check.clj class;
  er7/segments: #' sites; timelines/er7: found-not-caused items incl.
  encounter-spans' stamp-encounter docstring and emit_hl7.clj:514's
  orphaned citation; operational-models.md un-re-depthed in 21 files),
  the retirement inventories (engine: 14 caller-less defs, 9 dead
  requires; run session's list), final-result-stage (dead form),
  and session 17's five C10 residual surfaces.

STEPS (one gate each; full make test before every push)
1. MANIFEST, committed before any edit: derive the complete pass
   inventory from the tree -- (i) every prose citation naming a
   facade for a moved form (src, test, docs), each with its owning
   namespace; (ii) every test CODE reach through a facade
   (engine/x, emit-hl7/y, #' forms), each with its repoint target;
   (iii) delegating defs that become caller-less AFTER (ii),
   EXCLUDING any var interface.clj resolves through -- those stay;
   (iv) dead requires and dead forms; (v) C10 residuals and
   found-not-caused prose. Commit: "docs: the repoint pass manifest
   -- derived at <sha>". Gate: manifest first; counts stated.
2. Repoint commit: (i) and (ii) applied exactly per manifest; #'
   reaches repoint to owning namespaces. Commit: "refactor: repoint
   citations and test reaches to owning namespaces -- the ruled
   pass". Gate: suite green with per-namespace delta explained
   (test counts should NOT change; assertion deltas only from
   file-population gates if any file is added/removed -- expect
   none); oracle IDENTICAL.
3. Retirement commit: manifest (iii)+(iv) -- caller-less delegating
   defs (incl. the three C7 ^:private defs once their #' reaches
   repointed), dead requires, final-result-stage. interface.clj
   UNTOUCHED; anything it resolves through stays. Commit: "refactor:
   retire caller-less facade surface -- the ruled pass". Gate: suite
   green; oracle IDENTICAL; a real -M:dev load of both facades,
   both interfaces, and every repointed test namespace.
4. Residuals commit: manifest (v). Gate: suite green; state-derived
   regenerated LAST.
5. Push; CI via gh; close marker. Record: manifest-vs-landed
   reconciliation (every row landed, deferred-with-reason, or
   refuted); P5 backlogs cleared or slimmed in the roadmap; final
   facade form counts. FENCE RESUMES at this session's close --
   say so in the record.
FENCES: no interface.clj edits; no behavior change; no renames; no
improvements beyond the manifest; oracle IDENTICAL or stop; if the
manifest exceeds one session's honest reach, land manifest + steps
2-3 and hand step 4 back with the remainder priced.
SELF-ARCHIVE: prompt and record in the final push.

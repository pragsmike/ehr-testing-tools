2026-08-05 — scaffolding compaction A: riders, vestige retirements, Deferred triage
Session prompt (design channel, 2026-08-05). Prior: promotion session complete + verified (`2d1dcf3`). This is session A of the scaffolding-compaction arc (B = the ADR/roadmap restructure, C = the continuity register — both pending author rulings, NOT this session's scope). R30 ceremony throughout.
Context
Three riders from the promotion verification, two code-level vestige retirements from the design channel's audit, and the Deferred-section triage: 19 rows, of which the author has ruled dispositions per the classification below. Design-channel evidence at `2d1dcf3` (session re-derives fresh, both-direction deltas recorded):

* `census_test.clj` lines 12 and 41 attribute the poly-test invisibility to "the roadmap's own Wave I finding" — ADR-0044 explicitly corrects that citation as unfindable (GMF Wave I is an unrelated arc). The shipped docstrings and the ADR disagree.
* The "Reading-set budget numbers (charter §6)" Deferred row was closed in substance by AR-D-3 (2026-08-05 re-baseline) and is the only closed row without a closure note.
* `sim_adapter.clj:45,61`: legacy option keys (`:sim-dir`, `:env-sim-dir-fn`, `:default-dir`) accepted-and-dissoc'd — a tolerance shim for the retired sibling-checkout era (the docstring itself says the discovery is gone). `sim_adapter_test.clj` exercises the tolerance.
* `intake_test.clj:196`: `sample-manifest` builds a `:stage :simulated` fixture stamped `:generator {:name "ehr-testing-sim" ...}` — the dead repo's name on sim's own stage.
* Roadmap Deferred triage (author-ruled classification): LIVE, keep untouched (~12 rows: carry-across, chronic-meds cap, backload, P2-5, verdict-cache, ImagingStudy-R5/stroke, myocardial blockers, census refinements, UTI O2-sat, vital-sign channel, lookup `time`, resolve-time-advance, full-pipeline gap). CLOSED-WITH-NOTE, move to Done with annotations intact (S4, J2, sim-manifest interop, docs pass — relocation has Done-history precedent, "S3/S4 rows moved to Deferred"). STALE-AUDIT, close iff evidence confirms (budget numbers per AR-D-3; `Active CarePlan` iff the interpreter's condition clause exists — cite the landing wave/ADR; the `race` half of the lookup-column row iff the column landed — the `time` half stays live either way).

Read first

* `notes/ADRs.md` ADR-0044 (the citation correction this session propagates), ADR-0043 tail, ADR-0012 (in-process adapter ruling).
* `components/sim-trajectory/test/ehrt/sim_trajectory/census_test.clj` lines 1–50.
* `components/corpus/src/ehrt/corpus/sim_adapter.clj` + `test/.../sim_adapter_test.clj` + `intake_test.clj` 185–210.
* `.agents/plans/roadmap.md` Deferred section, every row.
* `components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj` — the Active CarePlan and race-column evidence sites.

Author rulings (record verbatim in a new ADR entry or ADR-0044 appendix — session's choice, recorded)

1. AR-A-1 (citation propagation). The two census_test docstring attributions to "Wave I" are corrected to match ADR-0044's provenance (the invisibility was confirmed by live before/after evidence, not a roadmap row); each gets a dated one-line note. The lesson generalizes and is recorded: intra-session artifacts written before a later step's discovery must be swept in the same session — a gate for this is session-B/C material, not built now.
2. AR-A-2 (budget-numbers row). Closure note citing AR-D-3, then the row moves to Done per AR-A-5.
3. AR-A-3 (sim_adapter tolerance retirement). IFF fresh grep confirms zero callers pass the legacy keys outside the adapter's own test: the keys stop being accepted (the dissoc and its docstring tolerance retire), the test updates to the current contract, dated note cites ADR-0012 and the M1 sweep. If a real caller exists: KEEP, record the caller, the finding stands.
4. AR-A-4 (intake fixture). The fixture's generator name aligns with what sim's `build` actually stamps today (Step 0 reads it from `components/sim/src/ehrt/sim/manifest.clj`, not from memory); dated note. Pure test-data change; if any assertion keyed on the old name, it updates in the same commit.
5. AR-A-5 (Deferred triage). Per the classification above: LIVE rows untouched; CLOSED rows relocate to Done with annotations intact (relocation, not rewrite — precedent cited in the note); STALE-AUDIT rows close-and-relocate iff Step 0's evidence confirms, each citing its landing evidence, else stay with the audit finding recorded. The Deferred section header gains one line: "Rows here are LIVE. Closed rows move to Done with their notes."

Steps
Step 0 — Characterize. Verify tip = `2d1dcf3` (STOP-AND-ESCALATE on mismatch). Fresh grep: legacy-key external callers; sim's current generator stamp; Active CarePlan interpreter clause + its landing ADR; race-column status + landing evidence. Form-anchored deftest counts. Record.
Step 1 — Riders (AR-A-1, AR-A-2). The two docstring corrections; the budget-numbers closure note (relocation rides Step 3). Full suite green. Commit: `docs: Wave I citation propagates to the census docstrings; budget row closes (compaction A, AR-A-1/2)`
Step 2 — Vestige retirements (AR-A-3, AR-A-4). Evidence-gated per the rulings. `poly check` clean, full suite green — the adapter test update is the behavioral proof. Commit: `refactor(corpus): the sibling-era tolerance retires; the intake fixture stops impersonating a dead repo (compaction A, AR-A-3/4)`
Step 3 — Deferred triage (AR-A-5). Relocations + conditional closures per Step 0's evidence; the section-header line. Commit: `docs(roadmap): Deferred means live — closed rows join the Done history they belong to (compaction A, AR-A-5)`
Step 4 — ADR + record. Rulings verbatim, dispositions with evidence, the AR-A-1 lesson. Roadmap bookkeeping for this session itself. Session record `.agents/session-records/2026-08-05-scaffolding-compaction-a.md`; prompt self-archives. Oracle bracket (`bin/regression-oracle 2d1dcf3 <tip>`): all ELEVEN batches byte-identical (the adapter change is corpus-side, outside every digest path — identity expected; any change escalates). Deftest parity: wash or explicitly-ledgered test updates only. All gates green. Final commit: `docs: compaction A session record — the scaffolding tells the truth again`
Fences
No restructure work (sessions B/C — pending author rulings). No Deferred-row deletions — relocation and annotation only. No adapter behavior changes beyond the ruled tolerance retirement. No new gates. Frozen archives untouched. Evidence over ruling everywhere: every iff-clause resolves by fresh grep, both directions recorded.

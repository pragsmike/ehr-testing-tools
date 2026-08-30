SESSION: engine extraction 2 of N -- the state cluster
Repo: pragsmike/ehr-testing-tools, work from tip (867e73a or
descendant). Roadmap row: [engine-namespace-extraction-and-apply-
unification] (P5). Program rulings in force: C1(a) -- engine.clj stays
the facade, moved PUBLIC vars get delegating defs, no test file
changes; S1(a) -- equivalence proof replaces red-before-green.

READ FIRST
- .agents/plans/engine-extraction-census.md -- section 1's `state` form
  list, section 3a's edge table and cycle note, section 5 (all seven
  constraints; item 6's docstring-phrase-grep recipe is mandatory
  below).
- .agents/session-records/ -- the streams-extraction record, for the
  delegation pattern and the limitations.md edge it hit.

WHAT MOVES (census names, spans at 517a96d -- re-derive at your sha;
engine.clj has shifted since):
- The 13 `state` forms: ConditionRecord, ObservationRecord,
  MedicationOrderRecord, CarePlanRecord, Demographics,
  demographics-from-persona, placeholder-demographics,
  PatientLocation, EncounterRecord, AppointmentRecord, PatientState,
  valid-patient?, initial-patient.
- PLUS `observation-value-fields` (private, census section 3a's one
  cycle breaker; its natural home per the census). Per constraint 5 it
  becomes public in the new namespace and gets NO delegating def --
  engine.clj's `decide :observation` / `evolve :observation` /
  `evolve :diagnostic-report` call sites qualify to the new ns.

STEPS (one gate each; full `make test` before every push)
1. Confirm tip; re-derive every span from your tree. Gate: recorded.
2. Before moving ANY form: constraint 6's recipe -- grep the whole
   repo for a distinctive phrase from each form's docstring, not only
   its name; check both limitations.md registers and any doc that
   cites a docstring by path (consuming-ground-truth.md,
   docs/patient-state-model.md). Gate: the hit list is in the record
   BEFORE the move commit, each hit dispositioned (repoint | safe).
3. Extract to ehrt.sim-engine.state (name yours if you find a
   collision; say why): forms verbatim except dispositioned repoints;
   delegating defs for the 13 public vars; qualified call sites for
   the cycle breaker; no reformatting of unmoved code. Commit:
   "refactor: extract state namespace from engine.clj --
   output-identical". Gate: suite delta zero or explained to the
   assertion (streams session's io-vocabulary +1-per-file class is
   the known benign delta); `bin/regression-oracle` IDENTICAL, no
   declaration -- a delta is a defect, stop and report.
4. `bin/ground-truth-bracket`. Gate: IDENTICAL.
5. Push; verify CI green via `gh run view`; CI green at tip is the
   close marker. Record: census corrections one sentence each, and
   confirm (or refute) that the census DAG now has no back-edge into
   the remaining engine.clj from state.
FENCES: no interface.clj edits; no schema content changes; no var
renames; no emit_hl7.clj; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt to .agents/prompts/, record to
.agents/session-records/, final push.

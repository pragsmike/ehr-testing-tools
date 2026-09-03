# Session: A1 -- `:new` supersedes a cancel-transfer reinstatement (2026-09-03)

Roadmap row `cancel-transfer-reinstates-a-new-subject` (PRIORITY 2).
B2 (`non-admitted-patients-hold-no-bed`, landed 2026-09-03) convicts the
downstream calibration fixture at --patients 1984 and 2000; this session
removes the state at its creation so both go green. The seam is the
TS-5 guard `log-index/subject-superseded?` -- churn.clj:125-145 states
the division of labour (static oracle inserts; decide-time guard
rejects), so churn is NOT touched. Payload-behavior: ADR first. No
sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; records
2026-09-02-downstream-self-check-failed.md (## Step 6) and
2026-09-03-b2-b1-stale-hold.md; 2026-08-29-ts-5-superseded-cancel.md
(:22-45, :235-292 -- the declaration protocol); components/sim-engine/
src/ehrt/sim_engine/log_index.clj :185-262; decide.clj :1655-1700;
churn.clj :125-145; engine_test.clj :649-670 (M6 Task 2 -- the fence);
bin/oracle-lib.sh :224-260; docs/formats.md and components/sim/docs/
patient-state-model.md where `illegal-cancel-transfer-subject-
superseded` is documented; test-fixtures/downstream-calibration/.

Author rulings, verbatim and binding:
- R-A1-scope (2026-09-03): `:new` supersedes a `:cancel-transfer`
  reinstatement and does NOT supersede a `:cancel-discharge`. M6 Task
  2 (engine_test.clj:649) stands; it goes green or the session STOPs.
- R-sweep (TS-5, standing): one declared sweep at most. Expected
  IDENTICAL on roots per TS-5 precedent; a moving root is declared
  per oracle-lib's protocol with a soundness argument, or STOP.
- Fence: `dense-7500-nobed.edn` is not in the tree. Do not
  reconstruct it; record the cell as unreproducible.

Steps:
1. Derivation in the record, before code: (a) the churn division of
   labour, cited; (b) that plain `:new`-in-set rejects M6's cancel-
   discharge (trace the guard's two conjuncts against :649's world3)
   -- hence R-A1-scope. Gate: both traced to line. No commit.
2. ADR (next free number; 0176 is the latest at drafting): decision
   per R-A1-scope; the coherence argument (cancel-discharge onto :new
   -> :admitted+bed+class, M6; cancel-transfer onto :new -> B2's
   state); payload effect (such cancel-transfers become :step-rejected
   :illegal-cancel-transfer-subject-superseded {:status :new});
   TS-5's exclusion superseded, its docstring paragraph cited.
   Gate: link-footnote gate. Commit: docs: ADR -- :new supersedes a
   cancel-transfer reinstatement
3. RED: engine_test, M6-harness style: admit -> transfer -> discharge
   -> cancel-admit -> cancel-transfer is rejected with that reason and
   {:status :new}; M6 :649 untouched. Gate: exactly the new test red.
   Commit: test: cancel-transfer onto a :new subject is rejected -- RED
4. GREEN: kind-aware supersession in log_index.clj (extend the table's
   asymmetry; rewrite the "deliberately ABSENT" paragraph to record
   the reversal with citation, history kept). decide.clj unchanged
   unless the trace forces it -- disclose if so. Gate: sim-engine and
   sim-check bricks green, M6 included.
   Commit: fix(sim-engine): :new supersedes a cancel-transfer reinstatement (ADR-NNNN)
5. Witness at a real shell, fixture config, seed 424242,
   --reference-date 2026-08-31 --churn --format ground-truth:
   --patients 500, 1000, 1984, 2000 all exit 0; sha256 of 500/1000
   vs the downstream's 434232a9... / ddcfc319... -- record match or
   move (a move means a :new reinstatement occurred in that run; name
   its :t). ed-tuesday exits 0. Gate: four exit codes.
6. Sweep: bin/regression-oracle and bin/ground-truth-bracket vs
   c16bb26 per R-sweep. Reason docs: add the `:new` status case where
   the reason is documented. Gate: full make test green.
   Commit: docs: reason docs carry the :new case; sweep recorded
7. Record (incl. the unreproducible-cell finding); roadmap row ->
   CLOSED with citations; regenerate indexes; archive prompt.
   Fences: no change under sim-check src or churn.clj.
   Commit: docs: A1 session record (archives prompt)
8. Push; verify CI yourself (gh run view); close-marker commit.

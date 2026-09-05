# Session: P7 -- referential columns A, B2, C populated and shipped (2026-09-05)

Roadmap row `referential-corpus-population` (PRIORITY 7). The STOP
record 2026-09-05-p7-stop-derivation.md did step 1: 14 cells named,
population dense-7500/config.edn at --patients 20 (~26 s, flat in
--patients), 13 convicting exactly, A/inverted-span unwitnessable
because the cancel invariant had no time clause. ADR-0178 (753d320)
fixed the population's schema defect and gave the invariant its
clause: the population is sound and all 14 are witnessable. Rider:
`:sim-check` stage in pipeline.edn (ruled (b) 2026-09-04). No
sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; the STOP
record in full; .agents/plans/2026-09-01-event-mutation-population-
ledger.md sections 5-7; notes/adr/0176-*.md (Q5, Q6, Q9), 0178;
components/corpus/src/ehrt/corpus/operators.clj :380-612;
components/corpus/test/ehrt/corpus/event_mutate_test.clj (populations
:76-99, loop tests :190-310); sim_check/check.clj :940-1045 (the
clause, its equality boundary); components/corpus/docs/pipeline.edn
and use-cases.edn's :ground-truth-as-a-test-oracle; .agents/
reading-sets.edn.

Rulings, binding:
- Q5(a), Q6(a), Q9(a) standing. R-pins: a moved count pin is its own
  commit naming the count.
- R-population: third harness population = dense-7500/config.edn at
  --patients 20, seed and :churn per the STOP record. No new config.
- R-target (finding 3, fix-forward): `:target` becomes keyword-or-map,
  read through one `target-kind` helper every shape calls; column A's
  three carriers cancel three classes. Disclose in the record.
- R-rider: pipeline.edn gains `:sim-check` ("Sim check": the invariant
  catalog over the ground-truth log, distinct from :check's
  expectation judge); the oracle use case's equation repoints.
  lint.clj untouched.
- Declared sets: per column, the ledger section 6 invariant, plus
  A/inverted-span's set as MEASURED under ADR-0178's clause (declare
  what convicts; Q5(a) equality decides).

Steps:
1. Re-measure the 14 cells' site counts at 20 on the fixed population
   (expect the STOP record's numbers -- keys were removed, no events
   added) and A/inverted-span's observed set. Gate: 14 populated,
   inverted-span convicts referentially. No commit.
2. RED: population delay; 14 conviction expectations in the loop
   tests' idiom. Gate: exactly the 14 red; every-population-checks-
   clean green on the new population. Commit: test: columns A, B2, C
   -- 14 conviction witnesses, RED
3. GREEN: R-target helper; three entries in referential-columns;
   nothing else. `make docsgen` (operators.md; cli.md only if `sim
   mutate` enumerates operators via help.clj :doc). Gate: corpus brick
   green in every project. Commit: feat(corpus): referential columns
   A, B2, C shipped (ADR-0176)
4. Rider per R-rider; `make use-cases` and the pipeline renderer.
   Gate: usecases + lint tests. Commit: docs: sim check is its own
   pipeline stage
5. Ledger section 6 rows -> measured, "shipped"; section 7 dated
   note; ADR-0176's "14 population gaps" sentence gains a dated
   addendum, verbatim kept. Full make test; pins per R-pins. Gate:
   full suite green. Commit: docs: population ledger closed
6. Oracle + bracket vs 753d320: IDENTICAL expected (mutate is post-
   run). Record; row -> CLOSED as a two-clause pointer at the record,
   not a narrative; indexes; archive. Fences: no sim, sim-engine,
   sim-check src; event-mutation-catalog-gate stays BLOCKED.
   Commit: docs: P7 session record (archives prompt)
7. Push; verify CI yourself (gh run view); close-marker commit.

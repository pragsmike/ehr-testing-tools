# Session prompt -- arc 3a-2: the fold's refactors, output-identical (ADR-0173 §2, part 2 of 4)

**Archived verbatim.** Executed 2026-08-26, base `c45ddb9`; the record is
[`../session-records/2026-08-26-arc-3a-fold-refactors.md`](../session-records/2026-08-26-arc-3a-fold-refactors.md).

---

Context. HEAD c45ddb9. ADR-0173 is Accepted (A1 B1 C1 D1 E1); part 1
landed `:residence-loss`. The author ruled the remaining fold into three
sessions, each proven IDENTICAL: **this one is the refactors** -- no
`:persons` key, no person event reaches the engine, no new event kind.
Everything here is exempt from red-before-green under
`R-output-identical-exempt-from-reshuffle-era`; the obligation is the
ADR-0169 proof: `bin/regression-oracle c45ddb9 HEAD` IDENTICAL with no
declaration, every pinned fixture byte-equal, suite delta = new tests only.
A moved byte is a STOP. Read first: ADR-0173 §2(a)-(b) and the sizing
table in `.agents/session-records/2026-08-25-arc-3a-residence-loss.md`
("What steps 2-3 still owe") -- it names every seam; re-derive each line.

Step 1. C1 ordering -- the byte-identity risk, gated hardest. Today
`decide :registered` (`engine.clj:483-517`) draws Persona and runs the
module walk + `compile-trajectory` from the patient's OWN `:patient`
stream at arrival. Move the walk to run start: `run` compiles every
patient's trajectory before the fold, keyed by patient id, and
`:registered` reads the compiled result. Invariant that makes this legal
-- state it in the docstring and PROVE it in a test before moving
anything: every input to the walk and to `compile-trajectory` is
arrival-time-independent. Enumerate them from the tree (`rng` = the
patient's stream, `(:persona-config world)`, closure/root/modules/
initial-attributes/tables, `reg-t = reference-today-epoch-day`,
`horizon-end-t`, `(:facility world)`, `history?`) and for each say why
it cannot differ between run start and arrival. If ONE cannot be shown
t-independent, STOP-AND-REPORT -- do not move the walk. Then export the
compile as `sim-engine.interface/compile-patient` (or the name the ADR
uses) so `ehrt.sim.run` can call it in part 3 to obtain `:deaths` (C1).
Gate: a test that compiles at run start and at arrival for the same
patient and asserts `=` on the compiled result, over the four
`gated-runs`. Commit; oracle IDENTICAL named in the message.

Step 2. `:person-index` in `init-world` (`engine.clj:1853-1889`, beside
`:reinstate-index`/`:citation-index`), empty, with the same
`(contains? world ...)` hand-built-world tolerance `:citation-index` has
at `:1068`. Unit test only. IDENTICAL.

Step 3. `PatientState` (`engine.clj:166`) gains `:demographics`, seeded
at `:registered` from today's Persona and READ nowhere yet except by
its own test (a `:persona` reader is not yet re-pointed). Malli schema
extended; `event-schema` untouched (no wire change). IDENTICAL.

Step 4. The re-key: `personas-by-patient-id` (43 references in
`emit_hl7.clj`) -> `demographics-at`, one lookup shape returning today's
persona for every `t` (the fold arrives in part 3). Twelve threading
signatures per the sizing table; `personas-are-keyed-by-patient-id-
alone-test` (person-simulator limitations_test :116) must go RED on
the re-key by design -- update its assertion to the new shape, say so.
Emitter tier unchanged in output: `make integration` green,
conformance baselines byte-equal, `make traces` produces no diff.

Step 5. Proof and close. `make test` + `make integration` unpiped on a
quiet machine, counts reconciled vs c45ddb9 (name each new test);
`bin/regression-oracle c45ddb9 HEAD` IDENTICAL, no declaration, 35 roots;
`git diff --stat` empty on `arc0_gated_*`, `pinned_seed_42`, both
conformance baselines, `demos/traces`; `poly check` OK; `sim-engine`
still requires no `person-simulator` namespace. Push; CI is the gate;
no tag. Record one page: the t-independence table from step 1, the
twelve signatures re-keyed, oracle line, delta. Roadmap row one line.

Fences. No `:persons` in `config-keys`. No new event kind, no contract
bump. No change to `person-simulator`, `sim-model`, `sim-check` except
the one test assertion named in step 4. No draw added, removed, or
reordered relative to any stream -- the walk moves in TIME, not in the
stream it reads. One reshuffle is zero.

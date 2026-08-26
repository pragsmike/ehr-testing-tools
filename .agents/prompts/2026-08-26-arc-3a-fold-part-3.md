# Session prompt -- arc 3a-3: the fold, landed dark (ADR-0173 §2, part 3 of 4)

**Date:** 2026-08-26
**Record:** [`.agents/session-records/2026-08-26-arc-3a-fold-part-3.md`](../session-records/2026-08-26-arc-3a-fold-part-3.md)

Archived verbatim, as issued.

---

Context. HEAD a5d2239. Parts 1-2 landed `:residence-loss` and the
output-identical refactors (walk at run start via `compile-patient`,
`:person-index` empty, `PatientState :demographics`, `demographics-at`).
This session lands EVERYTHING ADR-0173 §2 says except the two hooks and
identification (part 4), with `:persons` ABSENT from every existing config
-- so the proof is still `bin/regression-oracle a5d2239 HEAD` IDENTICAL,
no declaration, every pinned fixture byte-equal, suite delta = new tests.
Behaviour is exercised ONLY by unit tests on configs WITH `:persons`.
Read first: ADR-0173 §2(a),(b),(e),(f) and the sizing table in
`.agents/session-records/2026-08-25-arc-3a-residence-loss.md`; part 2's
record for what already exists. Re-derive every line.

Ride-along first, own commit: ADR-0172 row 10's gate
(`person_simulator/limitations_test.clj`) -- make the reverse half
position-matched like the forward half, so a docstring naming the person
component is not a feedback edge. Proved red by mutation (a real require
added, then removed). Born green.

Step 1. RED. On a config `{... :persons {:count 12 ...}}` (the ADR's shape,
:163-178) tests for each of: `run` accepts `:persons` and rejects a malformed
one; `ehrt.sim.run/run-command` calls `person-simulator/persons` iff
`:persons` present and hands `run` the t-ordered event vector plus
`:deaths` from `compile-patient` (C1); A1 selection -- each arrival binds
a person alive at that instant by ONE `:world` draw, `:person-index` grows
`person-id -> patient-id`, a second arrival of the same person resolves to
the same patient; the fold -- residence/coverage/identity-correction
events applied to `:demographics` in t-order interleaved with engine
events (queue-seeding into the `sorted-map` keyed `[t seq-no]`), so
`demographics-at` returns state-at-t; `:person-death` mints nothing
(row 4); the two kinds `:demographic-update` and `:coverage-change` on the
wire with `demographic-update-reports-a-real-change`; each of §2(e)'s six
invariants firing on a mutated corpus AND clean on the `:persons` run;
provenance stamps `:persons` and `:persona-config` iff present. Every
test names its counted witness (`pos?`). All RED on a5d2239 for "no
fold". Commit RED.

Step 2. GREEN, per §2. `:persons` joins `config-keys` (its docstring's own
law); `run.clj` plumbing (the `:modules` layering); selection; queue-
seeding; `evolve` siblings; contract **1.2.0 -> 1.3.0** in `event_schema.clj`
with the two kinds in the kind enum (:188) -- log-validates-unchanged for
1.2.0 corpora, say how; a FIFTH fleet fixture in `sim_engine/event_fleet.clj`
carrying `:persons` so `make event-schema-examples` can lift one real event
per new kind (the ordering part 1 found); six invariants registered in
`check-all`; ADR-0172 row 6 STRUCK with its gate deleted (its substance is
now false by design -- say so in the ADR's table). `sim-engine` still
requires no `person-simulator` namespace: events arrive as data.

Step 3. Proof. `bin/regression-oracle a5d2239 HEAD` IDENTICAL, 35 roots,
no declaration -- the fleet fixture and the unit configs are NOT oracle
roots; if the oracle differs, a dark path leaked, STOP. `git diff --stat`
empty on `arc0_gated_*`, `pinned_seed_42`, both conformance baselines,
`demos/traces`. `docs/formats.md` regenerated (declared docs change, the
only generated surface that moves); `make test` + `make integration`
unpiped, counts reconciled (name each new test); `poly check` OK.

Step 4. Push; CI is the gate; no tag. Record one page: witness counts per
kind on the `:persons` unit run, the 1.3.0 delta, oracle line, row 6
struck, ADR premises the tree contradicted. Roadmap row one line.

Fences. No hook, no identification event, no placeholder MRN -- part 4.
No existing config, scenario, or oracle root gains `:persons`. No hazard
or draw change in `person-simulator`. A moved byte in any pinned
artifact is a STOP. If `:persons` cannot be admitted to `config-keys`
without changing a byte-identical path, STOP-AND-REPORT with the diff.

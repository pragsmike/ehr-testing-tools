# Architecture Decision Records — ehr-testing-sim

Numbered, append-only. Never silently revert an Accepted ADR;
supersede it with a new numbered record.

---

## ADR-0001 — Standalone library with a mountable CLI group; dependency arrow points tools → sim only

**Status:** Accepted (2026-07-26)

**Context.** This repo must be usable independently (own `sim` CLI)
and mountable inside ehr-testing-tools' `ehr` CLI as the `sim`
subcommand, without either repo forking the other's conventions.
Tools' CLI architecture (its ADR-0004): thin printing shell over pure
capability functions following the result-not-throw doctrine, `[group
action]` dispatch with injectable `-fn` keys, EDN canonical with
`--json` projection, exit codes 0/1/2(/3).

**Decision.**
1. **Dependency direction:** ehr-testing-tools may depend on
   ehr-testing-sim; ehr-testing-sim never depends on
   ehr-testing-tools.
2. **Embedding contract:** `ehr-testing-sim.cli` exports three public
   values — `cli-spec` (babashka.cli coercions, mergeable),
   `help-group` (one entry in tools' help-data shape), and
   `dispatch-action` (`(fn [action opts]) → Result`). A host mounts
   the group with one dispatch arm plus a spec merge and a help-group
   registration.
3. **Standalone = embedded:** the standalone `sim` shell wraps the
   same `dispatch-action`, so the two modes cannot drift.
4. **Copied, not shared, result vocabulary:** the ~30-line
   result-not-throw ns is duplicated (own namespace, identical
   structure). Result maps are structurally typed, so hosts consume
   them regardless of constructing namespace. A shared microlib is
   deferred until a third consumer exists.
5. **Mirrors carry tripwires, contracts live with the host:** shapes
   this repo must match (tools' manifest schema, help-data shape) are
   mirrored here with validating tests as tripwires; the *binding*
   cross-repo contract tests belong in tools' integration tree, where
   both codebases share a classpath.

**Consequences.** Sim stays independently useful and testable; tools'
tests can stub the whole sim group via the injectable `-fn` pattern
without loading the engine; schema drift between the repos surfaces as
a failing test in tools, not a runtime surprise.

---

## ADR-0002 — Ground-truth log is primary; wire formats are emitters; pathways are a common intermediate representation

**Status:** Accepted (2026-07-26)

**Context.** The library composes two mined designs: Google Simulated
Hospital's operational model (scripted pathway steps incl. ADT churn,
discrete-event queue, HL7v2 messaging) and Synthea's generative
clinical modules (probabilistic state machines with embedded SNOMED/
LOINC/RxNorm codes). See `.agents/memory/architecture.md` for the
mining record and `docs/problem-statement.md` for the black-box
contract.

**Decision.**
1. **Intermediate pathway representation (IR):** hand-authored
   scenario scripts and generated trajectories (later: compiled from
   Synthea-GMF-style modules) share one EDN pathway format; the engine
   executes only the IR and cannot distinguish the sources.
2. **Ground-truth log is the primary output:** the engine emits a
   format-free, time-ordered event log. HL7v2 messages (and later
   FHIR/CDA state renderings) are emitters consuming the log. Test
   assertions and the invariant catalog target the log.
3. **Codes are state, not format:** clinical concepts ride the IR and
   log as `{:system :code :display}` triplets; each emitter renders
   them natively.
4. **Determinism:** all randomness flows from one seeded RNG with a
   totally ordered event queue; same config + seed ⇒ byte-identical
   serialized output. Enforced by property tests.
5. **Invariants co-land with steps:** every step type added to the
   engine ships with its invariants in the catalog in the same change.

**Consequences.** State-based emitters become renderings over retained
state history rather than a redesign; the validation program's
internal-consistency claims are cheaply machine-checkable at scale;
churn injection (operational noise) becomes an IR-to-IR transform,
cleanly separated from clinical trajectory generation.

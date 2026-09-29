## ADR-0182 — `:immunization` joins the log: the Vaccine fact becomes a ground-truth kind

**Status:** Accepted 2026-09-29 (author ruling "For B, before describe";
build session same day, R30). Event schema **1.10.0**.

### Context

Every line number below is at `01a87e11`, read this session.

The GMF interpreter has emitted a `:vaccine` trajectory event since Wave I
(ADR-0040 AR-5; `gmf_interpreter.clj:2046`), carrying `:codes` and a
`:series` it zero-defaulted when the module stated none.
`compile-trajectory` had no clause for it, so it fell to the loop's
`:else` and compiled to nothing; `emittable_events.clj:93-98` named the
drop alongside `:allergy-onset`. No log this project has produced has
ever held an immunization.

Two facts make that drop invisible to every instrument the tree runs:
none of the 31 vendored modules carries a Vaccine state, and `:vaccine`
is not a pre-horizon fact kind. So nothing shipped can reach the kind,
and promoting it moves no existing byte. It also means the kind needs a
**producer** before any instrument can exercise it.

Downstream asked on 2026-09-29 for the fact as ground truth: subject,
time, coded product, series when present, nothing synthesized.

### Decision

1. **The kind.** `:immunization` — `:active-mrn`, `:codes` (a vector of
   `Concept`, CVX in practice), `:series` OPTIONAL `:int`, `:citation`
   optional. "No state change; the log itself is the record": `evolve`
   is the identity, the `:procedure` precedent, because nothing renders
   an immunization from folded state yet.
2. **Both routes to it.** A pathway IR step of the same name, author-
   facing like `:order`, so a scenario can write one today; and
   `vaccine->step` at the compile dispatch, so a module's Vaccine state
   reaches it. `decide :immunization` takes no draw (`_streams`), so no
   new draw site exists; `run.clj` is untouched.
3. **`:series` when stated, and only then.** Absent when the module or
   the author states none — never a defaulted 0, never nil (ADR-0178).
   This SUPERSEDES Wave I's interpreter zero-default: that default was
   harmless while the event died at `:else`, and becomes a false
   statement the moment it reaches a log a consumer reads.
4. **It is clinical content.** `clinical-content-only-when-admitted`
   gains it, and with it the `orphan-participant` operator's derived
   kind set. A vaccine is administered at a visit. `:vaccine` joins
   `pre-horizon-dropped-types`: a history-phase vaccine was dropped
   before (by `:else`) and stays dropped, never promoted to a
   registration fact.
5. **No message, no resource.** No HL7 `message-type-registry` entry —
   absent from the registry is the shipped no-message pattern
   `:procedure` and `:outpatient-visit-end` already follow — and no FHIR
   Immunization resource. The kind is ground truth only in 1.10.0, and
   the schema `:doc` says so.
6. **The version.** `classify-change` against the frozen 1.9.0 baseline
   returns `{:additive? true, :breaking []}`. No bump is owed; 1.10.0 is
   taken anyway, for 1.7.0's reason — the version is a consumer's only
   handle on which kinds a log can contain.

### The producer

The prompt placed the producer in `demos/scenarios/clinic-decade`: one
authored `:immunization` step in a booked visit. **That premise did not
hold against the tree**, and the author ruled twice on 2026-09-29:

- clinic-decade authors no steps at all (`:pathway {:name
  "clinic-decade" :steps []}`) -- every visit it holds, booked or not,
  is compiled from a vendored module, and its follow-up visit's steps
  are hardcoded in `decide :discharge`. Any route that ADDS a visit
  draws the shared `:facility` stream (the attending), so no clinic-
  decade diff could be "exactly the new events". And no oracle root
  reads that config.
- `gated-runs` (the sim brick's population gates) does not feed the
  oracle, and a member owes a committed arc-0 baseline plus an
  ADR-0171 pre-partition digest a new run never had.

**Ruled: one run, both producers, as an oracle root.** A hand-authored
fixture module, `immunization-fixture.json` (patient-simulator's test
fixtures, beside `death-fixture.json`; CVX 115 Tdap stating
`"series": 1` and CVX 33 PPSV23 stating none, both codes already in the
tree), walked by ordinals 0-3; every other ordinal takes an authored
Renal stay carrying `{:type :immunization :codes [Tdap] :series 1}`.
It lands as the oracle's 42nd root, `immunization`, through
`engine-pair` (the `death-fixture` precedent -- `run-command` resolves
modules only by name from `sim/modules/`, which a test fixture must not
shadow), a FIRST BASELINE; and as
`ehrt.sim-engine.engine-test/both-producers-reach-the-log-and-self-check-clean`,
the same config through `run/run` + `check-all`. It stays OUT of
`gated-runs`, the `dense-7500-cell` precedent.

Measured, and as predicted: 52 events, 16 `:immunization` -- 8 compiled
(4 Tdap carrying `:series 1`, 4 PPSV23 carrying no `:series` key) and 8
authored (all `:series 1`) -- `check-all` clean, MSH-9s 8 ADT^A01 /
8 ADT^A03 / 4 ADT^A04, no `:transfer`. `witnessed-event-kinds` returns
to total at 29 of 29; `witnessed-message-types` does not move.

One fixture finding, recorded: the first draft held the walk with a
`Date` Guard and compiled NOTHING. The interpreter jumps time only for
`:age` guards (`age-guard-jump-days`); a failing `:date` guard blocks
the walk for good. The fixture loops a year at a time instead.

**No existing corpus byte moves**: the bracket and the oracle are
IDENTICAL on every existing root, and the new root appears on the
target side only (figures in the session record). Downstream produces
the kind today by authoring the step in its own pathway.

### Follow-ons owed (not taken here)

- **VXU^V04.** The HL7 v2 immunization message. Needs its own registry
  entry, segment builder (RXA), and a judge pass.
- **FHIR Immunization.** Needs an accumulator in `PatientState` (the
  fold is the identity today) and a resource in `sim-emit-fhir`.
- **Allergy by the same path.** `:allergy-onset` is still the other half
  of the pair `emittable_events` named: a real trajectory event dropped
  at `:else`. The same slice shape applies.
- **A shipped generator.** A vendored Vaccine-bearing module, or a demo
  scenario, when downstream wants to generate the kind without authoring
  it themselves.

### What this does not do

- Rows nothing on the roadmap: this record is the record.
- Adds no `gmf-interpreter.md` §1 row for `AllergyOnset` — that table
  never carried one, and neither did it carry Vaccine until this ADR.

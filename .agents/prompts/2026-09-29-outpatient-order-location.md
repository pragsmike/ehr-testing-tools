# Outpatient orders: :location optional on order-placed/result-available (schema 1.9.0)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-29-outpatient-order-location.md`,
> which records where the live tree differed from this prompt — the
> "run ... (self-check on)" of step 1 lives in `ehrt.sim.run`, not in
> brick:sim-engine, and `docs/patient-state-model.md` is
> `components/sim/docs/patient-state-model.md`.

Context: an authored pathway `:outpatient-visit` -> `:order` fails self-check with `invalid-paths
[[:location]]`. `evolve :outpatient-visit` leaves no `:location` (the schema's own "one sanctioned
admitted-without-a-bed case"); `order-only-when-admitted` passes; but `:order-placed` and
`:result-available` require a closed `Location` and `decide :order` writes `(:location patient)` = nil
into both. The engine emits what its schema refuses. Also: `orm-message` passes PV1-2 `:inpatient`
unconditionally. No shipped scenario authors an order under an outpatient visit -- expect every bracket
IDENTICAL. Reported by downstream 2026-09-29.

Read first:
- components/sim-engine/src/ehrt/sim_engine/event_schema.clj:393-399, 836-875, 341 (version)
- components/sim-engine/src/ehrt/sim_engine/decide.clj:1529-1580; evolve.clj:359-375
- components/sim-check/src/ehrt/sim_check/check.clj:1222-1250
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/messages.clj:301-435 (orm/oru), segments.clj (pv1-segment)
- components/sim-model/src/ehrt/sim_model/pathway.clj:190-200; notes/adr/0178-*.md (absent vs nil)

Author ruling 2026-09-29: "(A)" -- defect; location is OPTIONAL on the two order kinds (downstream's
reading 2); no manufactured non-bed location (reading 1 refused).

Steps:
1. Red: a run with pathway [:outpatient-visit :order :cbc :delay :outpatient-visit-end] (self-check on)
   returns :self-check-failed with invalid-paths [[:location]] at 9f60bb18; a second assertion that the
   same log passes `check-all`'s invariants when the schema step is bypassed (the catalog already admits
   it).  Invariant: red for the schema reason only.  Gate: brick:sim-engine shows exactly this.
2. Schema: `[:location {:optional true} Location]` on :order-placed and :result-available; version
   "1.8.0" -> "1.9.0" with the changelog line; `decide :order` nil-drops :location on both events
   (ADR-0178: absent, not present-and-nil); inpatient orders unchanged.
   Invariant: the schema still REJECTS a present-and-nil :location.  Gate: step 1 green; brick:sim-engine,
   brick:sim-check, brick:sim green; poly check OK.
3. Emission: ORM/ORU derive PV1-2 from the event -- :outpatient when :location is absent, :inpatient
   otherwise; PV1-3 empty when absent. Render the step-1 log; `judge v2` HAPI and NIST tiers.
   Invariant: no new finding class; PV1-2 = O on both messages.  Gate: brick:sim-emit-hl7 green + judge.
4. `bin/ground-truth-bracket 9f60bb18 HEAD` no flag; `bin/regression-oracle 9f60bb18 HEAD` no flag.
   Invariant: IDENTICAL on every root, both instruments (no shipped root reaches the changed arm).
   Gate: banners.
5. Docs: docs/formats event-log section and docs/patient-state-model.md's validity table name the
   outpatient order; `make docsgen && git diff --exit-code` clean. No ADR: a required key relaxed for a
   case the schema already sanctioned in prose; if you judge a new kind or law is needed, stop and report.
6. Full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`. Record + prompt
   archive. Push; gh run view the tip; CI green is the close marker. Record carries the downstream line:
   schema 1.9.0, commit, "no existing corpus byte moves".

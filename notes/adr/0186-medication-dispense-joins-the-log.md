## ADR-0186 — `:medication-dispense` joins the log as an authored step: one kind, two settings

**Status:** Accepted 2026-11-17 (author rulings "Agree on all. Go." and,
earlier the same day, "b"; build session same day, R30). Event schema
**1.12.0**; describe report **1.3.0**.

### Context

Every line number below is at `dc58500f`, read this session.

The log has carried `:medication-order` and `:medication-end` since M5b
and nothing between them. Downstream asked on 2026-11-17 for dispenses
linked to their orders, in both settings: an inpatient pharmacy dispense
and a community fill of an outpatient prescription. The disclosure
session of that date said plainly, in two consumer docs, that the log
held neither.

The census of the same date
(`.agents/session-records/2026-11-17-dispense-census.md`) measured what
the shipped roots hold. At dense-7500's 7,500 cell, 4,907 of 4,986
in-log orders are two hand-authored steps of the `dense-inpatient`
pathway and 79 are module-authored; no shipped log holds one order a
vendored module marks `chronic: true`. So the orders that exist to be
dispensed are overwhelmingly authored ones, and a rule deriving fills
from module-authored prescription detail would have had almost nothing
to act on.

Three rulings stand behind this slice, all the author's, 2026-11-17:
one kind with a required `:setting` of `:inpatient` or `:community`;
authored steps need no config key, and derived fills, when they come,
get an engine key `:dispense` gating only the derived half; and a new
small oracle root is the first baseline, leaving `dense-7500`
byte-identical for now. A fourth, earlier that day, rules how chronic
orders will refill (a fixed period, no draw) and belongs to a later
slice.

This is slice 1a: the KIND, authorable by a pathway, the way
`:immunization` (ADR-0182) and `:allergy-onset` (ADR-0184) arrived.

### Decision

1. **The kind.** `:medication-dispense` — `:active-mrn`, `:setting`,
   `:order-event-id`, `:order-citation`, and `:citation` when the step
   states one, plus the four keys every event carries. `evolve` is the
   identity: the log is the record, and the order's own
   `:medication-orders` entry is untouched — a fill neither opens nor
   closes it.
2. **`:setting` is stated, never inferred.** `:inpatient` or
   `:community`, required on the step and on the event. The engine
   could have read it off whether an encounter is open, and that would
   have been wrong twice. An order written at an outpatient visit can
   be filled in clinic before the visit closes, which is still a
   community fill; and an inference would turn a mis-sequenced
   inpatient dispense into a silently legal community one, erasing the
   very error the admitted-only law exists to convict. The two are
   different facts with different sources and different messages, and
   the pathway author is the one who knows which this is.
3. **The order is resolved by citation, and its log index is carried.**
   `decide :medication-dispense` calls the same `last-cited-index`
   query `decide :medication-end` calls (`decide.clj` 1745-1775), with
   the same patient predicate, and `:order-event-id` is typed
   `[:maybe :int]` exactly as the end's is. The nil allowance is kept
   on purpose: an order may be a `:pre-horizon-facts` entry on
   `:registered` rather than a log event, and a later slice that fills
   such an order then needs no schema change. No producer writes a nil
   there in 1.12.0. `:order-citation` is NOT nilable, unlike the
   end's: the step requires it, since a dispense of nothing is not a
   step.
4. **No copied codes, no fill number.** The order carries the
   medication's `:codes`; the reference is the link, and a copy could
   disagree with what it copies. A fill number is a count a consumer
   can take over the log, and stating one would commit the log to an
   ordering rule before any rule for repeated fills exists.
5. **No draw.** `decide` takes `_streams`; `run.clj` is untouched.
6. **Two laws.**
   - A new catalog row,
     `medication-dispense-references-existing-order-and-follows-it-in-time`,
     placed beside the end's law. The dispense's `:order-event-id` is a
     real `:medication-order` of the same patient — the participant
     test the end's law applies, read the same way — at or before the
     dispense, and the dispense is at or before that order's end when
     the log ends it. Equality is legal at both edges. An order that
     several ends cite is closed at the earliest. Or the order is a
     pre-horizon fact on that patient's `:registered`, the end's own
     allowance; in that branch the end is not looked for, since an end
     of a pre-horizon order carries no index to find it by.
   - `clinical-content-only-when-admitted` gains an `:inpatient`
     dispense and not a `:community` one, for `:medication-end`'s own
     reason: a prescription outlives the visit that wrote it.
   The catalog is 47 rows.
7. **No message, no resource.** No HL7 `message-type-registry` entry and
   no FHIR MedicationDispense. The schema `:doc` says so.
8. **The version: a bump taken, not owed.** `classify-change` against
   the frozen 1.11.0 baseline, before the re-freeze, returned

       {:additive? true, :breaking []}

   A new kind is additive. The bump is taken for 1.10.0's reason: the
   version is a consumer's only handle on which kinds a log can
   contain. 1.11.0 -> 1.12.0. A 1.11.0-era log validates unchanged
   against 1.12.0.

### The producer

No module can author the step, so the producer is authored whole: the
oracle's 44th root, `dispense`, through `engine-pair`, a FIRST BASELINE,
and the same config as
`ehrt.sim-engine.engine-test/both-dispense-settings-reach-the-log-and-self-check-clean`
through `run/run` + `check-all`. Even ordinals take a Renal stay: order,
an `:inpatient` dispense, the order's end, discharge. Odd ordinals take
an outpatient visit whose order is filled `:community` two hours after
the visit closes and is never ended.

Measured: 66 events, 12 `:medication-dispense` (6 `:inpatient`, 6
`:community`), every `:order-event-id` a real order of the same
patient; `check-all` clean. `witnessed-event-kinds` returns to total at
31 of 31; `witnessed-message-types` does not move.

**No existing corpus byte moves.** No shipped config and no vendored
module authors the step; the ground-truth bracket and the regression
oracle each differ from `dc58500f` by the one added root (banners in
the session record). Downstream produces the kind today by authoring
the step in its own pathway.

### The describe rows

`describe` gains two `:opt-in` rows, `:medication-dispense-inpatient`
and `:medication-dispense-community`, witness field `:order-event-id`.
One kind, two rows, because a row's `:observed-by` is already any
predicate: the report counts by setting with no new mechanism, where a
single row would add two different facts together. `:describe-version`
1.2.0 -> 1.3.0.

### Follow-ons owed (not taken here)

- **Fills DERIVED for module-authored orders, under an engine key
  `:dispense`** (ruled 2026-11-17). The key gates only the derived
  half; an authored step needs none.
- **The chronic refill period.** Ruled in principle ("b": orders a
  module marks `chronic: true` refill at a fixed period until
  `:medication-end` or the horizon, no draw, one authored-provisional
  constant). The constant is open. The census found no chronic order
  in any shipped log, so this rule has no population to act on until a
  root reaches one.
- **Authoring the step into `dense-7500`**, with notice to downstream:
  that moves a shipped corpus, which this slice deliberately does not.
- **RDE / RDS and FHIR MedicationDispense.** The wire rendering.
  `:medication-order` itself renders no HL7 v2 message either.
- **A medication-administration kind.** The third thing "dispense" is
  taken to mean, and a different fact from both settings here.
- **The mutation column.** `operators.clj` gains nothing: no
  referential column for the dispense's `:order-event-id`, and the
  orphan operators' kind set does not gain the kind (reattributing a
  dispense also trips its own referential law, a fifth finding outside
  their declared four). The acceptance gate is keyed by field name and
  `:order-event-id` is covered, and no shipped log carries a dispense to
  calibrate a column against. A column is owed when a shipped corpus
  carries the kind.
- **The end of a pre-horizon order.** Decision 6's law does not bound a
  dispense of a pre-horizon order by that order's end. Owed with the
  slice that first produces such a dispense.

### What this does not do

- Rows nothing on the roadmap: this record is the record.
- Adds no config key and derives nothing: with no authored step, a
  1.12.0 run is a 1.11.0 run.

## ADR-0184 — `:allergy-onset` joins the log and the registration facts: the AllergyOnset fact becomes ground truth

**Status:** Accepted 2026-09-30 (author ruling "Allergy"; build session
same day, R30). Event schema **1.11.0**; describe report **1.2.0**.

### Context

Every line number below is at `6641c017`, read this session.

The GMF interpreter has emitted an `:allergy-onset {:codes}` trajectory
event since Wave I (ADR-0040 AR-5; `gmf_interpreter.clj:2040`).
`compile-trajectory` had no clause for it, so it fell to the loop's
`:else` and compiled to nothing — the other half of the pair whose
`:vaccine` half ADR-0182 promoted to `:immunization`. None of the 31
vendored modules carries an AllergyOnset or AllergyEnd state, so nothing
shipped reaches the kind, and promoting it moves no existing byte. Like
`:immunization`, it needs a producer before any instrument can exercise
it.

Downstream named allergy / substance / reaction as its first-priority
domain fact on 2026-09-18: subject, time, coded substance, the stated
type and category, nothing synthesized.

**A premise corrected.** The session prompt said the loader "reads only
`codes` and `target_encounter`", leaving `allergy_type`, `category` and
`reactions` dead past it. Measured: the loader's state schema is an open
map, so all three already passed through — undeclared, and with each
reaction in upstream's raw shape, its `reaction` Code still carrying the
JSON's `"SNOMED-CT"` system string, un-normalized. What was dead was
everything after the loader: the interpreter emitted `{:codes}` alone.
Step 1's loader test asserts the normalized shape for that reason.

### Decision

1. **The kind.** `:allergy-onset` — `:active-mrn`, `:codes` (the
   substance, a vector of `Concept`), and three OPTIONAL fields:
   `:allergy-type` and `:category` (strings as authored — upstream's own
   `String` fields, and the `:observation` `:category` precedent), and
   `:reactions`, a vector of closed `{:codes [Concept]}` maps.
   `:citation` optional. `evolve` is the identity: the log is the record,
   the `:immunization` precedent, since nothing renders an allergy from
   folded state yet.
2. **Each optional field when stated, and only then.** Absent when the
   module or the author states none — never nil (ADR-0178). An empty
   `reactions` list is not a statement: upstream's own
   `AllergyOnset.diagnose` records reactions only when the list is
   non-empty, and `allergies.json`'s own Allergy_Unspecified authors
   `"reactions": []`.
3. **Reactions without severity, and what that makes them mean.**
   Upstream's `ReactionProbabilities.generateSeverity` draws, per
   reaction, one level from `possible_severities` — and a `none` draw
   DROPS that reaction from the record. That is a draw site, and this
   slice takes none: `decide :allergy-onset` takes `_streams`, the
   interpreter step consumes no rng (asserted by a counting proxy), and
   `run.clj` is untouched. So `:reactions` lists the reactions the
   source NAMES for the substance — not the ones this patient
   manifested, and not with any severity. The schema `:doc` and its
   field comment say so. Inventing a severity would be synthesis;
   reporting every listed reaction as manifested would be a false
   statement about the patient; this reading is neither, and it is the
   one a later severity draw can refine without changing the key.
   The loader keeps `possible_severities` (declared, read by nothing) so
   that draw has its input when it comes.
4. **Both routes to the kind, and the pre-horizon route.** A pathway IR
   step of the same name, author-facing like `:immunization`; and
   `allergy-onset->step` at the compile dispatch. A pre-horizon
   AllergyOnset joins `pre-horizon-fact-types` and becomes a
   `:registered` event's `:pre-horizon-facts` entry — the opposite of
   ADR-0182's `:vaccine`, which joined `pre-horizon-dropped-types`. A
   vaccine is an ephemeral event; an allergy is standing history the
   patient arrives with, the `:condition-onset` class. The fact carries
   the substance `:codes` only, the uniform shape every entry there has;
   `:allergy-type`, `:category` and `:reactions` ride the in-horizon kind
   alone.
5. **It is clinical content.** `clinical-content-only-when-admitted`
   gains it, and with it the `orphan-participant` operator's derived
   kind set and `describe`'s `during-encounter-kinds`.
6. **No message, no resource.** No HL7 `message-type-registry` entry —
   absent from the registry is the shipped no-message pattern — and no
   FHIR AllergyIntolerance. The schema `:doc` says so.
7. **The version: a bump OWED this time.** `classify-change` against the
   frozen 1.10.0 baseline, before the re-freeze, returned

       {:additive? false,
        :breaking [":registered: key changed: :pre-horizon-facts (value schema changed)"]}

   The new kind alone is additive. Widening `PreHorizonFact`'s nested
   `:event` enum is not, by the function's deliberately conservative
   rule — and the rule is right here: a consumer dispatching
   exhaustively on a fact's `:event` meets a value it has never seen.
   1.10.0 -> 1.11.0. A 1.10.0-era log validates unchanged against 1.11.0.
   The nested-`:event` collision set grows from four values to five.

### The producer

ADR-0182's shape, one route wider. A hand-authored fixture module,
`allergy-fixture.json` (patient-simulator's test fixtures, beside
`immunization-fixture.json`): Bee venom AT BIRTH — always before the
horizon, so a registration fact — then, inside one wellness visit,
Peanut stating `allergy_type`, `category` and two reactions (upstream's
own reaction/`possible_severities` shape), and Grass pollen stating none.
No substance or reaction code was in the tree; each was added with the
display upstream's `allergies/*_incidence.json` modules carry (pin
`7e08387c`). Ordinals 0-3 walk it; every other ordinal takes an authored
Renal stay with `{:type :allergy-onset :codes [Latex] :category
"environment"}`. It lands as the oracle's 43rd root, `allergy`, through
`engine-pair`, a FIRST BASELINE, and as
`ehrt.sim-engine.engine-test/all-three-allergy-routes-reach-the-log-and-self-check-clean`,
the same config through `run/run` + `check-all`. It stays out of
`gated-runs`, the `immunization` precedent.

Measured, as predicted: 52 events, 16 `:allergy-onset` — 8 compiled
(4 Peanut carrying all three optional fields, 4 Grass pollen carrying
none) and 8 authored (`:category` only) — plus 4 Bee venom
`:pre-horizon-facts` entries and no Bee venom log event. `check-all`
clean, no `:transfer`. `witnessed-event-kinds` returns to total at 30
of 30; `witnessed-message-types` does not move.

**No existing corpus byte moves**: the ground-truth bracket and the
regression oracle each differ from `6641c017` by exactly one added line,
the new root (banners in the session record). Downstream produces the
kind today by authoring the step in its own pathway; a pre-horizon
allergy rides `:registered`'s `:pre-horizon-facts`.

### The describe row

`describe` gains an `:opt-in :allergy-onset` family row beside
`:immunization`'s, witness field `:category`; `:describe-version`
1.1.0 -> 1.2.0, an additive row being minor by the report's own semver.

### Follow-ons owed (not taken here)

- **Severity sampling.** Upstream's per-reaction draw over
  `possible_severities`, including the `none` level that drops a
  reaction. A new draw site, so a stream-partition question and its own
  ruling; it would change what `:reactions` means for a compiled onset
  (listed -> manifested) and would add a severity key.
- **`:allergy-end`.** AllergyEnd, its own kind, citing the onset.
- **`Active Allergy` reading a register.** The condition is always false
  today (`gmf_interpreter.clj:837`); with onsets in the trajectory it
  could read them, the way `Active Condition` reads conditions.
- **`target_encounter`.** Upstream defers an onset's record to its
  target encounter; this interpreter emits at once (the M5a
  ConditionOnset simplification). Upstream's own incidence modules fire
  AllergyOnset with no encounter open, so vendoring one would compile
  onsets outside an encounter and trip
  `clinical-content-only-when-admitted`. Honouring the deferral comes
  before any such module is vendored.
- **AL1 / IAM^A05 and FHIR AllergyIntolerance.** The HL7 v2 rendering
  (an AL1 segment on an ADT, or the IAM message) and the FHIR resource,
  each needing an accumulator in `PatientState` first.

### What this does not do

- Rows nothing on the roadmap: this record is the record.
- Carries `:allergy-type`/`:category`/`:reactions` on no pre-horizon
  fact (decision 4).

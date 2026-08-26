# person-simulator: scope and declared limitations

**The person process exists so that demographic and identity traffic is
realistic; a person's life is relevant only inasmuch as it changes a
message.**

That sentence is this component's charter, and it is the front door
because every other question about scope is narrower than it. It is why
this component models a residence move (one A08 and a changed PID-11)
and does not model a commute; why it models an employment change (a
coverage change, an IN1, and an occupational-injury hazard) and does
not model a job title. It is deliberately the same shape as
`patient-simulator`'s own mission sentence, for the same reason: it is
the sentence that settles arguments about scope before they start.

## Dependency direction

The engine CONSUMES the person stream. The person process knows nothing
of encounters, beds, wards or messages. It depends on `sim-model`
(`Persona`, `places`, the payer pools) and on `sim-engine`'s
stream-partition surface for `stream` / `newborn-id-tag` ONLY, and no
`sim-engine` namespace requires it. That is limitations row 10, and its
gate is what makes "engine -> person: none in v1" a structural fact
rather than a discipline.

So a limitation here is a limitation in the *demographic and identity*
input to traffic generation, and the honest question to ask of each one
below is not "is the life wrong?" but "does the message traffic come
out less realistic because of it?"

## Deliberate limitations

Twelve rows, each a gap declined ON PURPOSE by ADR-0172 section 4, with
the reason, the citation that records the decision, and -- the column
this table adds to `patient-simulator`'s own -- **the gate that goes red
if the decline is silently lifted.** Each of those gates is a test that
can be born red, which is the only kind worth writing.

`ehrt.docs-tooling.person-simulator-charter-test` gates this table four
ways: the mission sentence above occurs verbatim in this file, in
`README.md` and in `interface.clj`'s own SCOPE section; every row below
is mirrored by a row in ADR-0172 section 4; every citation resolves and
anchors exactly one place in its own file; and every deliberate-
limitation marker in this component's own `src` -- `PROVISIONAL`,
`UNDECLARED`, `DELIBERATELY`, `not ported` -- is covered by a citation
landing inside its own comment block. A new marker with no row here is
red.

| # | Limitation | Why declined | Citation | Gate |
| --- | --- | --- | --- | --- |
| 1 | **Twins and multiples are excluded.** One `:pregnancy` yields one `:delivery` yields one newborn. | The key is already reserved, so admitting multiples later costs no renumbering: `newborn-id-tag`'s ordinal is the PAIR `(parity-index, within-delivery-index)`, and ADR-0171 ruling B1 took the pair from the start precisely so that widening a bare parity index would never renumber every existing singleton's stream. | `components/sim-engine/src/ehrt/sim_engine/engine.clj` "pinned at 0 for as long as" | `every-delivery-is-a-singleton-test` -- every `:delivery` carries exactly one `:newborn-person-id` and `:within-delivery-index` `0`. A second newborn on any delivery is red. |
| 2 | **Immigration and emigration are excluded.** The population is CLOSED: no person enters except by birth, none leaves except by death. | A migrating population buys no message class this project can render. What reaches the wire is a registration, a demographic update or a merge -- all of which the closed population already produces. Migration would only change WHO produces them. | `components/sim-check/src/ehrt/sim_check/check.clj` "this run actually created -- i.e. appears as a participant on at" | `the-person-population-is-closed-test` -- every `:person-id` appearing in the stream is either in the t0 population or is the `:newborn-person-id` of a `:delivery` in the same stream. The shape of `participant-ids-exist-in-run`. |
| 3 | **Foster placement and adoption are excluded.** Household membership is birth- or cohabitation-derived only. | Both produce exactly the traffic a birth-derived or cohabitation-derived membership already produces -- an address, a guarantor, a coverage link. The distinguishing facts (legal custody, placement authority) reach no segment any emitter here writes. | `notes/adr/0172-person-simulator-charter.md` "Household membership is birth- or cohabitation-derived only." | `minors-join-households-only-by-birth-or-formation-test` -- a `:household-join` for a person under 18 references a `:delivery` or a `:household-form`. |
| 4 | **A death outside care mints no wire event.** `:person-death` stops the person's other processes and nothing else. The only death that reaches a message is the GMF one's expired discharge. | Ruling C1: the GMF death is authoritative for anything wire-visible, always. The patient-simulator already compiles a death inside an encounter to a `:discharge` with `:disposition :expired` and adds no death-specific IR step type; a second, person-side death event would be an invention with no wire counterpart, and would move every death fixture in the gated corpora -- which is the oracle's ONLY capacity-pressure coverage. | `components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj` "NO new IR step type" | `person-death-emits-no-ground-truth-event-test` -- folding a stream containing `:person-death` leaves the count of `:discharge` events with `:disposition :expired` unchanged. |
| 5 | **A legal name change and a data-entry correction are collapsed.** Both are `:identity-correction`; only real HL7v2 practice distinguishes them (A08 vs A31 usage), and v1 does not. | The distinction is a trigger-event choice at the emitter, not a fact about the person, and the emitter has no A31 path to choose. Modelling the cause here would put a field in ground truth that nothing downstream can read. | `notes/adr/0172-person-simulator-charter.md` "only real HL7v2 practice distinguishes them (A08 vs A31 usage), and v1 does not." | `identity-correction-carries-no-cause-test` -- `:field` is a closed set `{:name :dob}` and no `:cause` key exists. Red the day the distinction is added without a row. |
| 6 | **Demographics reach the wire through ONE per-run lookup.** `personas-by-patient-id` is keyed by patient-id alone. Until arc 3 re-keys it, a delta folded onto patient state is invisible to every message. | This is a limitation of the **engine and emitter**, not of this component, and it is tabled here because this component's whole output is invisible until it is lifted. A charter that tabled only its own gaps would let arc 2b ship a stream nothing reads and call it done. | `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` "call and threaded down to every segment builder" | `personas-are-keyed-by-patient-id-alone-test` -- asserts the map's key shape. Red the day it becomes `(patient-id, t)`, which is exactly when this row should be struck. |
| 7 | **Geography stays the 24-row `places.edn` pool** (`rulings.md#R-mix-3`). A residence move draws a whole new row from the same flat weighted pool: no adjacency, no distance, no local-versus-cross-country move. | The pool is hand-curated and synthetic by construction, and a move's whole wire consequence is a changed PID-11. Distance realism would change no segment. Reusing the pool also keeps this component from forking `sim-model`'s demographic tables. | `components/sim-model/src/ehrt/sim_model/persona.clj` "pool of full address rows" | `every-residence-address-is-a-places-row-test` -- every `:residence-move` `:address` is a member of `sim-model`'s own `places` pool. A synthesized address is red. Population asserted non-empty first (`rulings.md#R-empty-population-is-red`). |
| 8 | **Household structure has no wire surface.** No emitter writes an NK1 segment; `NK1` occurs in no `src` file anywhere in the tree. Households exist to correlate moves and coverage, not to be rendered. | Found by census, not assumed: `NK1`'s only live occurrences in this repo are a test's list of deliberately-unresolvable locator paths and four descriptive lines in a vendored research reference. Building a household rendering would mean building the segment first, which is an emitter arc, not this one. | `components/corpus/test/ehrt/corpus/locators_doc_test.clj` "documented-unresolvable-v2-locators-parse-but-do-not-resolve-test" | `no-emitter-writes-nk1-test` -- the emitters' segment vocabulary contains no NK1. Red the day one is added, which is the day households owe a rendering row. |
| 9 | **Every hazard rate is authored-provisional.** No cited table stands behind any number in this component. Each is a general-knowledge order of magnitude, with no table read and no source cited. | Ruling E1. The mission sentence is the argument: these rates exist to make traffic realistic, and traffic realism is insensitive to whether the move rate is 0.11 or 0.13, while it is very sensitive to whether moves happen at all. The marker-and-row mechanism keeps an unsourced rate visible forever instead of letting it silently become folklore -- the ADR-0170 species this repo has already been bitten by, a claim true when written that nothing keeps true. | `notes/adr/0172-person-simulator-charter.md` "Every rate in this section is AUTHORED, PROVISIONAL"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "moves per person-year overall"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "the retirement concentration is an authored shape"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "legal name change reaches the same PID-5 by"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "authored registrar-error knob"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "the branch a person takes is determined by"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "hard, not tapered: a tapered band is a second authored shape on top"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "plus a jitter draw uniform over"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "they are NOT collinear in log space"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "The conditioning is the load-bearing"; `components/person-simulator/src/ehrt/person_simulator/hazards.clj` "A defect-surface knob, not a world rate" | `every-provisional-rate-is-tabled-test` -- each rate constant in `src` carries a `PROVISIONAL` marker and every marker is covered by a citation into its own comment block. The ADR-0162 drift mechanism, with a fourth token. |
| 10 | **The engine tells the person process nothing.** No feedback edge in v1. So: a GMF `:death` cannot end a person's residence, employment or coverage process; an admission cannot delay a move, a job change or a delivery; a discharge disposition cannot become a residence fact; and an `:identity-unavailable` window cannot be *caused* by the clinical state that would really cause it. | One-way is what makes this component provable. The `:person` family has zero draw sites, so a component drawing only from it cannot move a byte of any existing corpus -- and a feedback edge would forfeit that immediately. The four consequences above are the price, and each is a traffic difference no message can show. | `components/sim-engine/src/ehrt/sim_engine/engine.clj` "arc 2's demographic/life-arc layer. ZERO draw sites" | `person-simulator-requires-no-engine-namespace-test` -- this component's `ns` forms name no `sim-engine` namespace but the stream-partition surface, and no `sim-engine` namespace requires `person-simulator`. A structural fact, not a discipline. |
| 11 | **Every pregnancy reaches a delivery.** No loss, no termination, no non-delivery outcome. | A non-delivery outcome produces an encounter, which is the patient-simulator's business, and produces no newborn, which is the only thing the delivery hook exists to hand the engine. Modelling it here would add a branch whose whole effect is to withhold traffic. | `notes/adr/0172-person-simulator-charter.md` "No loss, no termination, no non-delivery outcome." | `pregnancy-and-delivery-are-in-bijection-test` -- per person, `:pregnancy` and `:delivery` counts are equal and each delivery's `:pregnancy-event-id` is distinct. |
| 12 | **A parent may head more than one household.** A parent with no household at their delivery gets one constituted BY the birth; if their own household hazard fires later, they head TWO. | The births pass runs AFTER the walk that decides household transitions and cannot be seen by it, so a parent's own later `:household-form` does not know a birth already constituted one. Fixing it needs a second walk pass or a feedback edge from the births pass into the walk, and neither buys a message: both households have members, a move by the head propagates into both, and household structure has no wire surface at all (row 8). Stated in `persons`' own docstring rather than left to be discovered. | `components/person-simulator/src/ehrt/person_simulator/process.clj` "heading two: the one the birth constituted" | `a-parent-may-head-more-than-one-household-test` -- red the day the artefact is fixed, which is the day this row should be struck. Four such parents in this component's own witness population, pinned, with `pos?` asserted separately so the gate cannot pass by going empty. |

## What this table is not

It is not a coverage report. What this component's fourteen event kinds
are, which field of each names its antecedent, and what invariant those
referential fields must satisfy are in ADR-0172 sections 2 and 3. The
census of every place demographics are read today -- thirty-one sites,
nineteen of them time-varying -- is ADR-0172 section 1, and it is arc
3's sizing, not this component's.

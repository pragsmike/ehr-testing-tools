# Session prompt — arc 4 sweep 4: SIU

Archived verbatim. Delivered 2026-08-28; the session it drove is
[`../session-records/2026-08-28-arc-4-sweep-4-siu.md`](../session-records/2026-08-28-arc-4-sweep-4-siu.md).

---

Session prompt -- arc 4 sweep 4: SIU (ADR-0175 ruling B1, "SIU only after A" -- A is paid)

Context. HEAD a0eadb0. The 2.4 flip removed SIU's version blocker; the
emitter's own registry block (`emit_hl7.clj:92-110`) records the history
and the mapping it expects: `:appointment` -> S12, `:reschedule` -> S14,
`:appointment-cancel` -> S15, `:no-show` -> S26 -- all instantiating
`SIU_S12` (`2.4.properties` maps S13..S24, S26 -> SIU_S12; ADR-0174
:697-701: segments `[MSH SCH NTE PATIENT RESOURCES]`, PATIENT holding
PID/PV1). Verify the trigger mapping against the jar, not the comment --
if S14 vs S13 is wrong for "reschedule notification", the jar wins and
the comment is corrected in the same commit. Behind `:siu`, absent =
today byte-for-byte. Ground truth never moves (the four kinds are
skeleton, landed by arc 3b sweep 3; this sweep only renders them).
Read: ADR-0175 (B1's ordering + the :54 registry row), ADR-0174 §2(b)
(:376-392: the kinds' fields; SCH-1/SCH-2 stable placer/filler ids
across an appointment's family) and §2(d); sweep 2's control-id scheme;
sweep 3's record :285-305 (the MSH-10 collision -- SIU control ids must
use the four-part key from day one).

Step 0, ride-along, own commit: the roadmap row sweep 3 said it made
and did not -- one `## Next` line `[oru-control-id-collision]`:
"`control-id-for` not injective over `:result-available` -- 6 live
duplicate MSH-10s in seed-424242, 1 in clinic-decade demo (sweep-3
record :290); fix moves every corpus, its own declared sweep; sweep 5's
fan-out must either wait for it or derive from log indices." Docs-only.

Step 1. RED then GREEN, dark. Unit configs WITH `:siu` (shape `{...}`
minimal -- an on/off with an optional trigger allow-list; defaults
disclosed): (i) the four registry entries; `siu-message` as a sibling
builder (the PID-less A20 precedent does not apply here -- SIU carries
PATIENT, so it CAN share `single-subject-message` if the seam fits;
choose from the tree and say why); SCH-1/SCH-2 = the appointment's
placer/filler EI, STABLE across its S12->S14/S15/S26 family (assert on
a reschedule chain); SCH timing from the event; PID from
`demographics-at` at the message's t; PV1 only when an encounter is
open (a booking precedes its encounter -- assert an S12 before arrival
has no PV1); (ii) a `:no-show` S26 references the same filler id and
carries no encounter; (iii) control ids on the four-part key, MSH-10
unique at a t collision; (iv) the sampler's strata absorb SIU (n/gated
printed for the new MSH-9s); (v) `v2-replay`: SIU is not an ADT --
state what replay does (skip by family, like the board's foreign-
trigger skip) and assert round-trip coherence is unaffected; (vi) the
judge tier resolves every SIU to `SIU_S12`, zero `GenericMessage`;
(vii) `classify-change`: zero, or STOP. PROOF: `bin/ground-truth-
bracket <step-0 sha> HEAD` IDENTICAL; `bin/regression-oracle` IDENTICAL
(no root opts in; `digest.clj` untouched -- no declaration owed, the
sweep-3 middle-bracket shape). Commit dark.

Step 2. ON. Opt in the six corpora (+ `config-latency.edn`) and the
`scheduling` root (its purpose -- the root that produces all four
kinds); 41 others stay. Brackets: ground truth IDENTICAL; oracle
declared, movers = exactly the opted-in roots (name non-movers, the
zero-message pair, and any corpus with zero appointments -- count
appointments per corpus FIRST). Re-pin the message-side list; tripwire
pre-push. Witness table per corpus, `pos?` where the population
exists: S12/S14/S15/S26 counts, reschedule chains sharing one filler
id, pre-arrival S12s (no PV1), sampled strata. `make test` +
`make integration` (the coverage claims move: new MSH-9s). Commit ON.

Step 3. Push; CI; no tag. Record one page: bracket lines, witness
table, the trigger-mapping verification, builder-seam choice, ADR
premises contradicted. Roadmap: sweep 4 of 6; fan-out + MLLP next,
gated on the step-0 row's question.

Fences. No ground-truth change (bracket-enforced). No new event kind,
no schema diff (STOP). No fan-out, no MLLP, no NK1. No control id off
the four-part key. Structure claims cite the jar. One declared sweep.

# Wire fidelity: order-less ORU PV1-3 from the patient's bed at t

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-wire-fidelity-pv1-3.md`, which
> records where the live tree differed from this prompt — the lookup is
> keyed by LOG INDEX, not `t`: "the most recent placement at or before
> `t`" disagrees with the prompt's own replay-equality invariant on 297
> of the 1,019 observations/reports taken in a bed at the 750 cell.

Context: observation and diagnostic-report events carry no `:location` by design (the order-less ORU
shape), and their builders pass the event's absent location straight to PV1-3, so every observation or
report sent DURING AN ADMISSION renders an empty assigned-patient-location -- 1,019 of them at the
dense-7500 750 cell (found 2026-09-30, recorded, not fixed). Yesterday's `class-timeline` (timelines.clj:
132-175) derived PV1-2 the same way this derives PV1-3: a per-patient fold over the log, computed once
in `emit`/`emit-wire`, mirroring what `evolve` sets. Wire-only; ground truth untouched; one declared HL7
change. Baseline b1facded.

Read first:
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/timelines.clj:120-200 (class-timeline, class-at; the fold shape)
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/messages.clj:445-530 (the two builders), 560-590 (dft: reads the closing event's own location -- unchanged)
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj:331-346 (pv1-segment: location, from); er7.clj (location-field)
- components/sim-engine/src/ehrt/sim_engine/evolve.clj:200-300, 359-375 (every arm that touches :location)
- components/sim-engine/src/ehrt/sim_engine/event_schema.clj:393-399 (Location), 700-745 (cancel-* carry the restored location; bed-swap :swap)
- components/sim-engine/src/ehrt/sim_engine/fold.clj:1340-1370 (replay records: before-state :location -- the oracle for the fold)
- components/sim/test/ehrt/sim/run_test.clj:427-470 (gated-runs), the 2026-09-30 class tests (the corpus-scale test shape)
- notes/adr/0174-*.md:964-990 (yesterday's dated note; this one sits beside it)

Author ruling 2026-09-30: "next". Channel expectations (not rulings; correct from the tree): PV1-3 for
the order-less ORU kinds is the patient's location AS IT STOOD AT `t` -- the fold's most recent
placement at or before `t`, absent when none (today's byte, so outpatient and pre-admission events move
nothing); the fold mirrors `evolve` arm for arm and is PROVEN equal to replay, not asserted.

Steps:
1. Red tests. (a) sim-emit-hl7: a hand-built log -- admission, observation, transfer, observation,
   cancel-transfer, observation, discharge, observation -- renders PV1-3 = the admitted bed, the
   transferred bed, the restored bed, empty; red today (all empty). (b) sim brick, corpus-scale: for every
   gated root and the dense 750 cell, `timelines/location-at` equals the `:location` of `engine/replay`'s
   before-state at every event, for every patient -- red today because the fn does not exist. (c) the
   dense 750 cell: count of observation/report messages with a populated PV1-3 after == count of those
   events whose replay before-state has a location.  Invariant: red for the stated reasons only.
   Gate: brick:sim-emit-hl7 and brick:sim show these only.
2. `timelines/location-timeline` + `location-at`: one pass, per patient, mirroring evolve --
   :admission/:transfer set from the event; :discharge clears unless `:disposition :expired`;
   :cancel-admit clears; :cancel-transfer/:cancel-discharge set from the event's own restored
   `:location` (nil clears); :bed-swap sets each participant from `:swap`; :merge clears the absorbed
   side; computed unconditionally beside class-timeline; the two builders pass `(location-at ...)` as the
   pv1 `location` arg. Nothing else reads it.
   Invariant: 1(b) green on every root -- the fold IS replay's location, event by event; `git diff --
   components/sim-engine` empty; ORM/ORU/DFT/ADT builders unchanged.  Gate: 1(a)(c) green; both bricks.
3. Measure at the 750 cell, b1facded vs HEAD: messages whose PV1-3 changed, by kind; every change is
   empty -> populated on an observation/report during an admission; blanking exactly those fields
   reproduces the before-wire byte for byte. `judge v2` HAPI and NIST on both: no NEW finding class (a
   class that shrinks because PV1-3 is now present is reported as such).  Gate: counts + reconstruction
   + judge diffs, in the record.
4. `bin/ground-truth-bracket b1facded HEAD` (no flag) IDENTICAL on every root; `bin/regression-oracle
   b1facded HEAD --declared-digest-change` DIFFERS on exactly the roots carrying an observation or
   report under an admission, IDENTICAL on every other; predict the set before the banner and record
   both.  Gate: banners.
5. Docs: ADR-0174 dated note beside yesterday's (PV1-3 for the order-less ORU kinds derives from the
   patient's bed at t, proven equal to replay); formats/consuming-ground-truth only where PV1-3 is
   stated. No roadmap row (a payload finding is a record line).  Invariant: `make docsgen && git diff
   --exit-code` clean; poly check OK.
6. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries the
   downstream line: "wire-only; ORU observations/reports during admissions now carry the patient's bed
   in PV1-3; nothing else moves; ground truth unchanged."

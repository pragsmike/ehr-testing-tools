# Wire fidelity: observation/report PV1-2 from the encounter's class; DFT^P03 carries the log index

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-wire-fidelity-pv1-dft.md`, which
> records where the live tree differed from this prompt — the ADR-0181
> gate lives in `components/sim/test/ehrt/sim/run_test.clj` and already
> read RENDERED MSH-10s (so step 1(c)'s injectivity half was green at
> baseline; a shape clause is what went red), and
> `seed-424242-clinic-decade`'s 19 observations are 13 under an
> outpatient opener and 6 under admissions.

Context: two emission defects, both live in downstream's 28k wire corpus, both rowed/recorded 2026-09-29.
(1) `observation-message` and `diagnostic-report-message` pass PV1-2 `:inpatient` unconditionally
(messages.clj:471, 506); module-compiled wellness/ambulatory encounters therefore render ORU observations
as class I. (2) `dft-message` mints `mrn-P03-t` inline (messages.clj:549) -- outside ADR-0181's index
suffix, non-injective for two closes of one patient in one second, and unseen by the 0181 gate, which
counts `control-id-for` over EVENTS, not rendered messages. Ground truth untouched throughout; one
declared HL7 change. Baseline 64d55fec.

Read first:
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/messages.clj:300-310 (order-patient-class), 445-565 (the three builders)
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/timelines.clj (demographics-timeline, encounter-spans:131-175)
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj (control-id-for, the marker/index helper, pv1-segment:331-346)
- components/sim-engine/src/ehrt/sim_engine/evolve.clj:200-232, 359-375 (what :class the engine sets)
- components/sim-emit-hl7/test/ehrt/sim_emit_hl7/emit_hl7_test.clj (the ADR-0181 injectivity gate), charges tests
- notes/adr/0181-*.md:182-245; notes/adr/0174-*.md (the A04/outpatient PV1 note); .agents/plans/roadmap.md (dft-control-id-collision)
- components/sim/test/ehrt/sim/run_test.clj:427-470 (gated-runs; seed-424242-clinic-decade carries 19 outpatient observations)

Author ruling 2026-09-30: "go". Channel expectations (not rulings; correct from the tree): PV1-2 for the
order-less ORU kinds is the patient's class at `t` -- most recent opener at or before `t`, `:inpatient`
when none (today's byte, so nothing odd moves); DFT's id is `mrn-P03-t#<index>` via the shared suffix
helper; DFT's own class rule (`:outpatient-visit-end` -> O) is untouched.

Steps:
1. Red tests. (a) render seed-424242-clinic-decade; every observation/diagnostic-report message whose
   patient's most recent opener is `:outpatient-visit` carries PV1-2 = O -- red today (I); an inpatient
   control stays I. (b) a hand-built log with charges and two `:discharge` events for one MRN at one `t`
   renders two DFT^P03 with DISTINCT MSH-10s, each with the 0181 prefix law (`mrn-P03-t` a strict prefix)
   -- red today. (c) the 0181 gate widened: MSH-10 distinct over every RENDERED message (DFT, restatements,
   rungs included) for the gated roots and the dense-7500 750 cell -- confirm it is red or green at
   baseline and say which.  Invariant: red for the stated reasons only.  Gate: brick:sim-emit-hl7.
2. `timelines/class-timeline` + `class-at`: per-patient fold of opener kinds (admission/outpatient-visit;
   cancel-admit clears), one pass, computed unconditionally in `emit` and `emit-wire` beside
   `demographics-timeline`; the two builders read it; the `:inpatient` literal leaves both call sites.
   Invariant: ORM/ORU builders (order-patient-class) unchanged; `git diff -- components/sim-engine` empty.
   Gate: 1(a) green; brick:sim-emit-hl7 green.
3. DFT arm: `mrn-P03-t` + the shared `#<index>` suffix, from the stamped event; the latency lookup keeps
   using the event's own id as today. The charges fixture that hand-authors a bed location on an outpatient
   order becomes a real bedless one (schema 1.9.0 no longer produces the old shape).
   Invariant: every DFT id is a strict extension of its pre-change id.  Gate: 1(b)(c) green.
4. Measure: render the 750 cell at 64d55fec and HEAD; count messages whose PV1-2 flipped I->O by kind, and
   DFT ids changed; stripping `#<digits>` and restoring I on exactly those flips reproduces the before-wire
   byte for byte. `judge v2` HAPI and NIST tiers on both: no new finding class.
   Invariant: the flip count equals the count of observation/report events under outpatient openers.
   Gate: the counts and the byte-for-byte reconstruction, in the record.
5. `bin/ground-truth-bracket 64d55fec HEAD` (no flag) IDENTICAL on every root; `bin/regression-oracle
   64d55fec HEAD --declared-digest-change` DIFFERS on exactly the roots carrying an outpatient observation/
   report or a DFT, IDENTICAL on every other; both lists in the record.  Gate: banners.
6. Docs: ADR-0181 dated amendment (DFT joins the index rule; the gate now reads rendered MSH-10s);
   ADR-0174 dated note (PV1-2 for order-less ORU derives from the encounter's class); roadmap row
   `dft-control-id-collision` -> CLOSED; formats/consuming-ground-truth wherever the DFT id shape or PV1-2
   is stated.  Invariant: `make docsgen && git diff --exit-code` clean; poly check OK.
7. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries the
   downstream line: "wire-only; ORU observations/reports in outpatient encounters now PV1-2 = O; DFT^P03
   ids gain `#<index>`, old id a strict prefix; ground truth unchanged."

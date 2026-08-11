# 2026-08-11 — ehr-testing-tools: latency realism, the second clock (ADR-0109)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `d6ed674` (ADR-0108's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim; a deviation record follows that.

## Original prompt (verbatim)

# Session prompt -- latency realism: the second clock (ADR-0109)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session lands the mechanism for the author's
latency-realism direction under the ruled option (a) (2026-08-11,
author verbatim "I like a. go"): the second clock lives in the emitter
seam -- ground truth stays pure, `GT x LatencyParams -> TimedWire` --
so downstream receivers of the HL7 traffic can be supplied with
realistically incomplete/late/out-of-order records. HEAD at handoff:
d6ed674. This session's ADR is ADR-0109. The end-to-end demo is a
FUTURE session (the user-guide trigger's other half); this session is
mechanism + tests + doc.

THE DESIGN (channel-specified under the ruling; verify every claimed
fact against the tree):

1. **Two clocks, split at the field level.** Today every builder in
   emit_hl7.clj derives ONE `ts` from the event's clinical `:t` and
   uses it for MSH-7 AND the clinical fields. The fix: MSH-7 becomes
   TRANSMIT time (clinical t + latency offset); clinically-anchored
   fields (EVN-2, PV1 admit/discharge datetimes, OBR-7 observation
   datetime, OBX-14, etc.) KEEP clinical t. STEP ONE of this session
   is the field audit: walk every builder, classify every timestamp
   field message-time vs clinical-time with the HL7 v2 semantic
   justification, and record the table in the ADR -- the audit is a
   deliverable, not scaffolding. Where a field is genuinely ambiguous,
   classify conservatively (clinical) and note it.

2. **Sampling stays out of emit** (the namespace's own renders-only
   doctrine, restated in docs/dev/simulator-architecture.md -- this
   session must not contradict the doc it just gained). A new pure
   planning fn, `plan-latency : RNG x GT x LatencyProfile ->
   offsets` (a map keyed per-event, e.g. by the event's control-id
   basis), with FIXED RNG consumption per the RNG-path law: exactly
   one draw per ground-truth event regardless of profile coverage
   (draw-and-discard for uncovered event types), so adding a profile
   entry never shifts other events' draws. Offsets sample uniformly
   from per-event-type {:from-minutes :to-minutes} ranges.

3. **The stage function.** `emit-wire : GT x reference-date x
   utc-offset x facility x providers x site-profile x offsets ->
   TimedWire`, where TimedWire = messages SORTED BY TRANSMIT TIME
   (out-of-order clinical arrival falls out naturally). Identity law,
   site-profile precedent: absent/nil/{} offsets render byte-identical
   to today's `emit` output in today's order -- co-land the property
   test asserting it. Plain `emit`'s signature and output are
   UNCHANGED (existing callers untouched); emit-wire composes the
   split-clock rendering. Whether emit-wire shares builders via an
   offsets-aware internal parameter or wraps differently is yours --
   but plain emit's bytes are frozen (the oracle is the witness).

4. **Config entry.** Sim config gains optional `:latency` (a
   LatencyProfile: per-event-type ranges; schema in sim-model, small),
   threaded run -> payload so `corpus generate sim` scenarios can
   author it (ed-tuesday precedent). ABSENT = today's behavior
   everywhere, byte-identical. When present, run's :messages come
   from emit-wire's ordering and the spooler's msg-%03d follows wire
   order -- the whole downstream chain (play pacing by MSH-7, the
   board, gates) then sees transmit-time reality with zero changes of
   their own. If threading :latency through run.clj requires more
   than config-schema + one emit-call-site change, STOP-AND-REPORT
   the actual shape before widening.

5. **Downstream tolerance is OBSERVED, not fixed.** The board fold
   meeting out-of-order ADT (a transfer arriving before its lagged
   admission) is exactly the class of downstream behavior the author
   wants to test IN OTHERS -- probe fold-message's behavior on one
   disordered sequence, record what it does in the ADR as a finding,
   fix NOTHING in the fold this session (its behavior under disorder
   is data; changing it is a future ruling if wanted).

6. **Scope fences.** FHIR-side latency: named deferral. Late
   amendments (trailing A08s): named deferral -- they are new EVENTS,
   which is GT-side and outside ruling (a)'s seam. The architecture
   doc's extension-point sentence gets its dated addendum (the arrow
   now exists).

ORACLE BRACKET, with its reasoning: pure identity on all 35 roots
expected -- plain emit's bytes are frozen by design, no oracle root
enables :latency, and the identity property test is the local witness
of the same fact. Movement = STOP-AND-REPORT.

## Read first

- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj -- ALL
  builders (the field audit's subject), the ns doctrine, emit's
  arities, the site-profile identity precedent's wording
- docs/dev/simulator-architecture.md -- the doctrine and extension
  point this session instantiates (and its purity lint: your
  plan-latency fn takes an explicit RNG, no atoms)
- .agents/rulings.md -- the RNG-path law verbatim (fixed consumption),
  tag law, ASCII verification
- components/sim/src/ehrt/sim/run.clj -- the emit call site and
  config threading
- components/sim-model/src/ehrt/sim_model/config.clj -- schema home
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/interface.clj and a
  fold-message consumer test -- for step 5's probe
- notes/adr/0108-*.md -- the doc's contracts

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim "I like a. go" (to: the second
  clock lives in the emitter seam -- `GT x LatencyParams ->
  TimedWire` -- keeping ground truth pure; the arrow the
  architecture doc names as the extension point).
- [A] The chartering direction (already in the roadmap row,
  verbatim): supplying downstream receivers with realistically
  incomplete/late traffic is the point; their handling is not this
  workspace's problem to solve.
- [C] Everything under THE DESIGN above: verify-then-act; the field
  audit's classifications are the session's own reads of the HL7 v2
  semantics, recorded with justification.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0108 landing at `d6ed674` by fresh
   public clone. Tag `stable-20260811-simulator-architecture-doc` at
   `d6ed674`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **The field audit** (Design 1). The table lands in the ADR before
   any code.

3. **The mechanism, one commit**: LatencyProfile schema;
   plan-latency with fixed-consumption tests (adding a profile entry
   does not shift other events' draws -- test it); emit-wire with
   the split-clock rendering per the audit; the identity property
   test (absent offsets => byte-identical to emit, in emit's order);
   a nonzero-latency test asserting MSH-7 moved, clinical fields
   did not, and output order is transmit order; the run.clj/config
   threading; the step-5 disorder probe recorded as a finding.
   Design-doc dated addendum.
   Commit message (ASCII only):
   `feat: latency realism -- the second clock in the emitter seam (ADR-0109)`

4. **Oracle bracket.** All 35 identical per Context.
   Movement = STOP-AND-REPORT.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock, the sim purity lint explicitly.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0109
   (the audit table, the identity/consumption evidence, the disorder
   finding, the two named deferrals, deviations dated); roadmap: the
   latency row's mechanism half landed, the demo half re-anchored as
   the remaining work (and the user-guide trigger's progress noted);
   .agents/rulings.md records the "I like a. go" ruling verbatim;
   notes/ADRs.md index row; notes/adr/README.md count 106 -> 107;
   session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- latency mechanism (ADR-0109)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: components/sim-emit-hl7/{src,test},
  components/sim-model/{src,test} (schema), components/sim/src/ehrt/
  sim/run.clj (the threading) + its test, docs/dev/
  simulator-architecture.md (dated addendum), notes/adr/0109-*.md,
  notes/ADRs.md, notes/adr/README.md, .agents/* close-phase files.
  The sweep RULE governs over this list (ADR-0099 precedent).
- Plain emit's output is BYTE-FROZEN (the oracle and the identity
  test are the dual witnesses). GT, engine, interpreter, check,
  replay, fold-message, player, board, corpus, cli: all untouched.
- No FHIR-side changes; no new GT event types; no scenario configs
  this session (the demo session authors those).
- The purity lint must pass with your additions unallowlisted.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims are verify-then-act; the field audit is yours to
  earn from the builders and the standard, not from this prompt.

After landing: fresh clone; I'll re-derive the identity claim myself where feasible (the property test's structure, and the frozen-emit argument against the diff), audit the field-audit table against the actual builder diffs (every field the code moved must be classified message-time in the table, and vice versa), check the fixed-consumption test's non-vacuity, and read the disorder finding — which feeds directly into the demo session's design, where your downstream-receiver stand-in finally gets its realistic, incomplete, out-of-order Tuesday.

## Deviation record

None. Every step executed as specified: the tag ceremony (Step 1) ran
first and clean (remote unmoved); the field audit (Step 2) walked
every builder and found a narrower set of rendered timestamp fields
than the Design section's own examples named (PV1 admit/discharge,
OBR-7, OBX-14 are simply not rendered by this project's emitter at
all) — disclosed in the ADR's own audit table and this session's own
record, not a deviation from the prompt's own verify-then-act
instruction. The `:latency` threading through `run.clj` stayed within
the "config-schema + one emit-call-site change" budget Design 4 set as
the STOP-AND-REPORT threshold, so no report was needed. The oracle
bracket held pure identity across all 35 roots, exactly as the
Context's own pre-analysis predicted.

# 2026-08-08 — ehr-testing-tools: colorectal comes home (vendoring payoff)

## Context

Conventions read at HEAD `eb4b339` (straddle fix, ADR-0086), design
channel, 2026-08-08, verified by fresh public clone (tip, per-commit
file sets, fix diff, all four license terms, erratum, roadmap
dispositions). This session executes the roadmap's own Next-section
intake row "**Colorectal vendoring payoff**" (entered by ADR-0086):
`colorectal_cancer.json` is clean at all three deferral seeds post-fix
but NOT yet vendored — the fix session's fence held. Deferred at
vendoring batch 3 (ADR-0072), carried through the quality-review and
fidelity arcs, diagnosed (ADR-0085), fixed (ADR-0086) — this session
pins it.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward, record
HEAD (expect `eb4b339`; later escalates unless explained). Commits land
green; roadmap rows land same-commit.

## Read first

1. `notes/adr/0083-fidelity-payoff.md` — the anemia payoff, this
   session's own shape precedent (vendor, NOTICE, root, test, bracket).
2. `notes/adr/0086-straddle-fix.md` — the fix this payoff collects on;
   the tag debt; the intake finding.
3. `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_anemia_test.clj`
   — the committed two-deftest shape to mirror (engine round trip at
   the deferral's own seeds + a pinned zero-cost counter).
4. `notes/adr/0070-vendoring-batch-1.md` — the vendoring mechanics
   (byte-verbatim at pin, NOTICE hashing, the CSV/data-file lesson) and
   `.agents/rulings.md`'s vendored-bytes and population-scale-gate
   laws.
5. `components/oracle/src/ehrt/oracle/digest.clj` — the roots map
   (28 entries, `anemia-pair` the current last) and `anemia-pair`'s own
   producer shape (~444-454) to mirror.

## Author rulings

- **AR-CP-0 [A]** (ADR-0086, "Successor tag debt"): tag
  `stable-20260808-straddle-fix` at `eb4b339`, Step 0, standing
  ceremony (design-channel verified 2026-08-08 by fresh public clone).
  Verify-and-disclose if present; deferral is the deviation.
- **AR-CP-1 [A]** (the ruled queue, 2026-08-08; the ADR-0086 intake
  row): vendor `colorectal_cancer.json`'s closure under the
  vendored-bytes law. Enumerate the closure FRESH against the pin
  checkout (`/home/mg/synthea-checkout`, pin
  `7e08387c68a7f0e21d13076609a159fd473fc902` verified FIRST), including
  any non-JSON data files (the ADR-0070 CSV lesson — a lookup table in
  the closure vendors too, with its own NOTICE row). Copy byte-verbatim
  ONLY the members not already in the tree
  (`anemia/anemia_sub.json` is already vendored — expected overlap);
  one NOTICE provenance row + sha256 per NEW file; confirm (don't
  assume) the `.gitattributes` `components/sim/resources/sim/modules/**
  -text` rule covers every new path; confirm `notice_verbatim_test`
  actually verifies the new rows (its known two-shape coverage gap must
  not silently swallow them). No `:persona-config` override — the
  module's `Initial` state is not Race-gated (ADR-0082, confirmed by
  inspection there).
- **AR-CP-2 [C]** (population-scale gate law + the R2/AR-SF-7 counter
  precedent): a committed `vendored_colorectal_test.clj` mirroring the
  anemia test's two-deftest shape: (i) the full
  compile/engine/check/emit round trip at all three deferral seeds
  (20260802, 1, 42; 300 patients), asserting REAL compiled content
  (content-producing, not merely violation-free), a clean invariant
  pass, and real rendered HL7; (ii) a pinned
  **`:suppressed-straddle-spans`** count across a well-mixed seed sweep
  — this module's own straddling patients are exactly what the new
  counter exists to witness, so a future regression in the straddle
  gate shows as a moved integer, not a silent pass. Written RED-capable
  (assert the pinned values only after measuring them in-session;
  disclose the measured numbers in the ADR).
- **AR-CP-3 [C]** (the oracle): `colorectal-pair` joins `digest.clj` as
  the TWENTY-NINTH root, FIRST BASELINE, purely additive, mirroring
  `anemia-pair`'s producer shape (minus the persona-config). Bracket:
  `bin/regression-oracle eb4b339 <tip> --declared-digest-change`
  declaring the additive root — all 28 pre-existing roots IDENTICAL,
  `colorectal-pair` present at target only. Anything else is a fresh
  STOP-AND-REPORT.
- **AR-CP-4 [C]** (STOP-AND-REPORT conditions, explicit): the closure
  enumerating members beyond {already-vendored files,
  `colorectal_cancer.json`, disclosed data files}; ANY violation at ANY
  of the three seeds; any pre-existing root moving; any NOTICE/hash
  mechanism friction. Halt and report, don't improvise.

## Steps

**Step 0 — Preflight + tag (AR-CP-0).** Standard preflight (clean tree,
HEAD `eb4b339`, untracked disclosure, `clojure -M:poly check`, oracle
pre-digest `eb4b339 eb4b339` — 28 IDENTICAL, last-five CI disclosed).
Tag. No commit.

**Step 1 — Vendor (AR-CP-1).** Pin verification, fresh closure
enumeration (disclose the full member list), byte-verbatim copy of new
members, NOTICE rows + hashes, gate coverage confirmed. No commit yet —
Step 2 lands the vendoring and its protections together (co-landed
invariants law).

**Step 2 — Pin it (AR-CP-2/3).** The committed test (measured pins
disclosed), the 29th root, full suite green
(`clojure -M:poly test :all skip:integration`; the loopback flake, if
it fires once, disambiguates by an independent second run, disclosed,
untouched), the bracket per AR-CP-3, `gitleaks` clean. Commit:

    feat: colorectal comes home — three arcs deferred, one diagnosis and one fix later, pinned forever (colorectal payoff, AR-CP-1/2/3)

Push; verify message; watch CI to conclusion.

**Step 3 — Record.** `notes/adr/0087-colorectal-payoff.md` (closure
member table, NOTICE row count delta, the measured counter pins, the
bracket manifest, the tag act); index line in `notes/ADRs.md`;
`notes/adr/README.md` count 84→85; roadmap — the payoff Next row's
disposition per the live gated precedent (closure disclosure + Done
pointer `- 2026-08-08 — colorectal-payoff — ADR-0087`); this session's
own successor tag debt recorded in the ADR. Commit:

    docs: the colorectal payoff recorded — the twenty-ninth root, and the straddle counter finds its witness (ADR-0087)

Push; verify; watch CI.

**Step 4 — Ceremony.** Session record + prompt archived verbatim
(`2026-08-08-colorectal-payoff.md`), both READMEs, same commit:

    docs: session record and prompt archive — colorectal payoff

## Fences

One module's closure only — no other vendoring, however tempting the
veteran family looks (batch 4 is its own ruled session). No
interpreter/compile/engine/emitter edits. No census-tool, loopback-
flake, or pairing-as-data work. No state.md regeneration.

## Close-out

Session record: HEAD start/end, tag act, closure member table, NOTICE
delta, measured counter pins, bracket manifest, suite shape, shas,
post-push verification, CI conclusions. Echo to chat: closure members,
counter pins, bracket result, shas, CI status.

## Deviation record

- **AR-CP-2's own counter sweep, adapted not followed literally.** The
  prompt asked for "a well-mixed seed sweep" mirroring the anemia
  test's own interpreter-layer idiom — tried first, verbatim (150
  mixed seeds x 2 sexes per deferral seed, `reg-t` = DOB + 25 years, a
  100-year horizon): measured **zero** `:suppressed-straddle-spans` at
  all three deferral-seeds-as-mixer-seeds, 900 walks total. Disclosed
  as a real methodological finding rather than silently reported as
  the pin: `:suppressed-straddle-spans` lives on `compile-trajectory`'s
  own return map (compile-layer), not the interpreter layer alone —
  unlike the A5 arm's own `:suppressed-encounter-ends` (invisible to
  `engine/run`), `engine.clj`'s own `:registered` decide method calls
  `compile-trajectory` directly for every patient, so the counter is
  reachable through the SAME `engine/run` population the round-trip
  test already exercises. Adopted instead: `with-redefs` interception
  of `ehrt.sim-trajectory.interface/compile-trajectory` around the
  round-trip test's own three-seed, 300-patient `engine/run`
  populations (the colorectal investigation's own technique, ADR-0085
  AR-CI-2) — measured 1/0/1 across seeds 20260802/1/42, matching
  ADR-0085's own diagnosis exactly (the SAME two named patients). Both
  attempts, and the reasoning for the switch, are recorded in full in
  `notes/adr/0087-colorectal-payoff.md`'s own Measurement section.
- **The roadmap's own colorectal Deferred row** was NOT moved to Done
  this session (unlike AR-A-5's own relocation law, applied by
  ADR-0086 when the straddle fix landed) — that row already closed at
  ADR-0086's own session, with its own dated closure note left in
  place there. This session's only roadmap edit was removing the
  Next-section intake row (now executed) and adding the Done pointer
  for ADR-0087 itself — no double-closure attempted.

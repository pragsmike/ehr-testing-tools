# 2026-08-11 — Latency realism: the second clock in the emitter seam (ADR-0109)

## Scope

Author ruling, 2026-08-11, verbatim "I like a. go" (option (a): the
second clock lives in the emitter seam, `GT × LatencyParams →
TimedWire`, keeping ground truth pure — the extension point ADR-0108's
own section 5 already named). This session lands the mechanism: a
field audit of every timestamp-bearing segment builder in
`ehrt.sim-emit-hl7.emit-hl7`; two new pure functions (`plan-latency`,
`emit-wire`); a `LatencyProfile` schema in `ehrt.sim-model.config`; an
optional `:latency` opt threaded through `ehrt.sim.run`; the identity
property proving plain `emit` stays byte-frozen; and a disorder probe
against `fold-message`, disclosed as a finding, fixed nothing. The
end-to-end demo (a `:latency`-bearing scenario, one witnessed run) is a
FUTURE session — the user-guide trigger's other half. Two commits:
`dc5ebad` (mechanism) and this record's own close-phase commit.

## Red→green evidence highlights

`plan-latency-adding-a-covered-event-type-never-shifts-another-types-
own-offset`: churn-enabled ground truth (seed 7, 6 patients), offsets
planned under a narrow profile and a wider one from the SAME fresh
`Random. 123` — every already-covered `:admission` offset is identical
between the two maps, and the wider map is strictly larger (proving
the wider profile was actually live, not vacuously equal-empty).

`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`
(100 trials, seed/patients/churn generated): `emit`, `emit-wire` with
`nil`, and `emit-wire` with `{}` all produce the identical vector, byte
for byte, same order.

`emit-wire-orders-messages-by-transmit-time-reordering-a-lagged-event-
past-its-followers`: a +999h offset moves an admission from first (log
order) to last (wire order); the whole wire output stays MSH-7
non-decreasing; the same clinical events survive by trigger+EVN-2
(never dropped/duplicated).

The disorder probe (live, seed 1, admission +2h): wire order becomes
transfer → discharge → admission; folded through `fold-message` and
diffed against the same messages folded in log order, the reconstructed
state comes back internally inconsistent — `:status :admitted`
alongside a non-nil `:discharged-at`, `:location` reverted to the
pre-transfer ward. Disclosed in `notes/adr/0109-*.md`; `fold-message`
itself untouched — this is data about downstream-receiver behavior, not
a bug in this workspace's own fold.

Full local suite (`clojure -M:poly test :all skip:integration`): 612
occurrences of "0 failures, 0 errors," zero `FAIL`/`ERROR` anywhere, 3
minutes 58 seconds — the delta from ADR-0108's own 608-occurrence
baseline matches exactly the new/grown test namespaces this session
added (`ehrt.sim-emit-hl7.latency-test` new; `ehrt.sim-model.config-
test`/`ehrt.sim.run-test` grown). `clojure -M:poly check`: OK.
`ehrt.cli.cli-parse-guard-lint-test`: 4/22, 0/0 (unchanged — `bases/
cli` untouched). `ehrt.docs-tooling.sim-purity-lint-test`: 5/14, 0/0
(unchanged — this session's own `(java.util.Random. seed)`
constructions are object creation, not an atom/ref/agent/volatile).
`bin/verify-nist-lock`: OK, 6 coordinates matched. Oracle bracket
(`bin/regression-oracle d6ed674 dc5ebad`): `IDENTICAL: every root's
digest matches` — all 35 roots, matching the pre-analysis exactly (no
oracle root enables `:latency`; plain `emit`'s call sites are
unchanged).

## Judgment calls and their ratification status

- **The field audit found only two rendered timestamp fields, not the
  larger set the driving prompt's own Design section named as
  examples** (PV1 admit/discharge datetimes, OBR-7, OBX-14). Verified
  directly, not assumed: every segment builder's own parameter list
  and `create-field` calls were read in full before any code changed;
  none of the four takes or computes a `:t`-derived value. Classified
  conservatively per the prompt's own instruction, though in each case
  there was no live rendering to be ambiguous about — recorded in the
  audit table as "would be clinical time if ever added." Disclosed, not
  a departure from the prompt's own verify-then-act instruction.
- **`offsets` threaded as a new positional parameter to every builder**
  (after `site-profile`, before the event map), rather than a
  render-context map or a second `emit-wire`-only code path. Chosen
  because every existing builder already takes a long, uniform
  positional-argument list (`site-profile` itself set this precedent);
  `plain emit` calls every builder with `offsets {}`, so its own bytes
  never move — the prompt's own "whether emit-wire shares builders via
  an offsets-aware internal parameter or wraps differently is yours"
  license, exercised in the shared-parameter direction.
- **`plan-latency`'s RNG is a fresh, independently-seeded
  `java.util.Random.` seeded from the SAME run `:seed`**, not a
  distinct CLI-facing seed of its own. `run.clj`'s own docstring states
  this explicitly (a second, independently-seeded stream, never the
  engine's own sealed RNG) — chosen so a `:latency`-bearing run stays
  reproducible from `--seed` alone, the same single-knob determinism
  every other config surface in this project already promises, without
  adding a second CLI flag the driving prompt's own "one emit-call-site
  change" budget didn't ask for.
- **The disorder probe's own script is disposable**, not committed as
  a permanent regression test — the driving prompt's own Design section
  5 asks to "probe... record what it does... fix NOTHING," not to gate
  `fold-message`'s own disordered behavior going forward (that
  behavior is data for a future ruling, not a contract this session
  should freeze in a test). The finding's own reproduction recipe
  (admission control-id, +2h offset, the three-step pathway) is
  recorded in `notes/adr/0109-*.md` instead.

## Findings and HEAD landed

No discrepancies found between the driving prompt's own Design section
and the live tree that would have forced a STOP-AND-REPORT — the field
audit surfaced a narrower set of rendered fields than the prompt named
as examples, disclosed above as a judgment call, not a premise
mismatch (the prompt's own Design 1 anticipated exactly this: "where a
field is genuinely ambiguous, classify conservatively"). The tag
`stable-20260811-simulator-architecture-doc` was created at `d6ed674`
(this session's own Step 1), peeled ref verified exact match, remote
unmoved.

**HEAD landed**: `dc5ebad` (mechanism), plus this close-phase commit,
both pushed. The last five `main` CI runs at session start were all
`completed`/`success`; this session's own push also confirmed
`completed`/`success` post-push.

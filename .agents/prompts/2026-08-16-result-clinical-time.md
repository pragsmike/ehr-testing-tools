# Session prompt -- latency realism increment: clinical time on the
# result wire (OBR-7 / OBX-14) -- ADR-0142

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-
tools workspace. HEAD at handoff: c90c9bd (event-log contract arc
closed, ADR-0141; Event schema v1.0.0). This session's ADR is
ADR-0142. It executes the fidelity increment ADR-0109 and
`.agents/plans/roadmap.md`'s downstream-latency-realism row both name
as "a future, declared-oracle-change session of its own": render
HL7v2's own clinical-time fields on result messages -- OBR-7 wherever
`obr-segment` renders, OBX-14 wherever any OBX renders -- so that a
latency-shifted ORU (ADR-0109's `emit-wire`) carries BOTH clocks on
the wire, the way real feeds do. Today it carries only MSH-7
(transmit); a downstream receiver cannot back-date a late result.

This is an EMITTER-SEAM change and a DECLARED ORACLE CHANGE: plain
`emit`'s own frozen bytes move on every root that emits an ORU. It is
CONTRACT-NEUTRAL: the ground-truth event log's SHAPE does not change
-- `event-schema-test` must stay green with NO version bump, and
`resources/sim-engine/event-schema*.edn` are not touched. Anything
that wants to enter the log is a schema change under ADR-0141 Q-A's
versioning, ruled separately, not here.

## Read first

1. `notes/adr/0109-latency-second-clock.md` -- the field audit table
   (OBR-7/OBX-14 "not rendered"), the split-clock law, Named
   deferrals.
2. `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` --
   `hl7-timestamp` (:112), `transmit-seconds` (:435), the three ORU
   builders and their segment fns: `obr-segment` (:591),
   `obx-segment` (:602), `observation-obx-segment` (:679), the
   `:result-available` / `:observation` / `:diagnostic-report`
   message builders (:626-770). Verify every line cite; the tree wins.
3. `notes/adr/0131-slug-edn-round-trip.md` -- the declared-oracle-
   change discipline: predict movers BEFORE any src edit, PRE-digest
   all 35 roots, bracket with `bin/regression-oracle`, prediction must
   match exactly.
4. `docs/dev/simulator-architecture.md` §5 (extension point) and
   `docs/manual/04-time-on-the-wire.md` (the two-clock chapter --
   its EVN-2/MSH-7 story is what this session extends to results).
5. `.agents/skills/build-session/SKILL.md`; `.agents/rulings.md`
   "From ADR-0109", "From ADR-0141".

## Author rulings, verbatim

- Session scope: option (a) of the latency follow-on set -- OBR-7/
  OBX-14 clinical-time increment. FHIR-side latency and late
  amendments/A08 stay named deferrals; do not touch `emit-fhir` or
  `sim-engine`.
- Q1 (OBR-7 value): "a" -- the result event's own `:t`, rendered via
  `hl7-timestamp` exactly as EVN-2's `clinical-ts` is; the
  order-placed-`:t`/OBR-22 variant is a named revisit, not this
  session.  [EDIT HERE IF RULED OTHERWISE]
- Q2 (OBX-14 in `observation-obx-segment`): "a" -- render it in all
  three ORU shapes; the positional pad OBX-9..13 (and 7-8 when the
  observation carries neither) is accepted and disclosed in the ADR
  and in that builder's docstring, superseding its "never a positional
  pad" sentence for OBX-14 only.  [EDIT HERE IF RULED OTHERWISE]
- Tag licenses (case i, channel fresh-clone verification + author CI
  relay 2026-08-16 via `gh run list`): "Pay it, message verbatim" --
  `24f351d` (run 31961309197) and `c90c9bd` (run 31975476669).

## Step 0 -- open

- Fresh state: `git status --short | wc -l` = 0; `git log --oneline
  -3` shows c90c9bd at tip.
- Pay BOTH tags, in date order, ADR-0134 pattern:
  `bin/tag-ceremony stable-20260816-fence-battery 24f351d` then
  `bin/tag-ceremony stable-20260816-event-log-contract c90c9bd`
  (read the script's own usage first; do not guess its arguments).
  Push tags. Verify with `git tag --points-at` on both shas.
- Full-suite baseline: `make test` UNPIPED, full log to a file,
  `MAKE_EXIT=$?` captured; count "0 failures, 0 errors" blocks and
  compare to ADR-0141's own 332-block / 17,054-pass baseline; report
  the reconciliation. `clojure -M:poly check` OK.

## Step 1 -- census and oracle prediction (docs-only commit)

1. Field audit, re-derived: for each of the three ORU builders,
   record which segments/fields render today (cite line), confirm
   OBR-7 and OBX-14 are absent everywhere in the emitter (grep the
   whole namespace; also `orc-segment` -- ORC-9 stays OUT of scope,
   say so).
2. Mover prediction from the LIVE tree, population-closure law: for
   every one of the 35 oracle roots, does its digest contain any
   ORU^R01? Enumerate from `components/oracle` (the roots list) and
   the PRE-digest output, never from a list in this prompt or in any
   ADR. Predicted movers = roots with >=1 ORU; predicted identical =
   the rest. Record the split.
3. Doc/demo strip census: enumerate every committed file embedding an
   OBR| or OBX| strip (`grep -rln` over `docs/**`, `components/*/docs/**`,
   `demos/**`, `notes/**` -- scan-root class) and, for each, whether a
   gate regenerates it or freshness-diffs it (`Makefile`, `.github/`,
   `demo-exerciser-*`). Known-from-clone hits to CONFIRM, not trust:
   `demos/traces/README.md`, `demos/traces/emit-state/README.md`,
   `demos/traces/order-result/README.md`. Anything ungated is a
   fix-forward-with-disclosure item in Step 4.
4. Run the oracle PRE-digest over all 35 roots and record it as the
   baseline. Open `notes/adr/0142-result-clinical-time.md` with the
   audit, the prediction, and the strip census.
   Commit: "docs: ADR-0142 opens -- OBR-7/OBX-14 field audit, oracle
   mover prediction, result-strip census (declared oracle change)"

## Step 2 -- red first (tests only, witnessed RED)

In `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj`
(or a sibling `result_clock_test.clj` if latency_test's own scope
reads wrong -- your call, say why):
- For each ORU shape (`:result-available`, `:observation`,
  `:diagnostic-report`): plain `emit` renders OBR-7 (where OBR
  present) and OBX-14 on every OBX, equal to `hl7-timestamp` of the
  event's own `:t`, equal to MSH-7 (identity: no offsets).
- Under `emit-wire` with a covering `LatencyProfile` for the result
  event types: MSH-7 shifts, OBR-7 and OBX-14 do NOT -- the split-
  clock assertion ADR-0109 makes for EVN-2, made for results.
- `emit-wire` with {} offsets stays byte-identical to `emit` (extend
  or re-run the existing 100-trial identity property; it must remain
  green THROUGH this change -- both sides move together).
- Assert `event-schema-test` and the committed EDN export are
  untouched (no bump) -- a negative test on `schema-version` = "1.0.0"
  is enough.
Run: RED captured in the log with the failing assertion text.
Commit: "test: red -- OBR-7/OBX-14 clinical time on all three ORU
shapes, unshifted under emit-wire"

## Step 3 -- green (src)

- `obr-segment` gains OBR-7 = clinical-ts (Q1 a): thread `clinical-ts`
  from each caller exactly as `evn-segment` receives it; OBR-5/6 are
  empty positional fields.
- `obx-segment` and `observation-obx-segment` gain OBX-14 =
  clinical-ts; positional pad OBX-9..13 (Q2 a); the
  `observation-obx-segment` docstring's "never a positional pad"
  sentence amended in place with an ADR-0142 date, not deleted.
- Each ORU builder's own ADR-0109 docstring sentence ("OBR-7/OBX-14
  … not rendered") corrected in place, dated.
- No change to `plan-latency`, `emit-wire`, `transmit-seconds`,
  `msh-segment`, `evn-segment`, any ADT/ORM builder, `emit-fhir`,
  `sim-engine`.
- Green: the Step 2 tests, then full `make test` unpiped, MAKE_EXIT=0,
  block count reconciled against Step 0. Vendored/pinned tests that
  assert ORU bytes or field counts: re-baseline WITH disclosure per
  test (name, old, new, why) in the ADR -- never a silent edit.
- Oracle bracket: `bin/regression-oracle c90c9bd HEAD` -- expect
  DIFFERS; the mover set must equal Step 1's prediction EXACTLY. Any
  mismatch is a STOP-AND-REPORT with the diff, not a re-prediction.
Commit: "feat: clinical time on the result wire -- OBR-7 and OBX-14
render the event's own :t; MSH-7 alone shifts under emit-wire
(ADR-0142, declared oracle change, movers as predicted)"

## Step 4 -- docs, gated

- `docs/dev/simulator-architecture.md` §5: dated addendum, one
  paragraph -- the two-clock story now shows on ORU messages; name the
  fields.
- `docs/manual/04-time-on-the-wire.md`: extend with the result-message
  case (one witnessed strip: a lagged result's MSH-7 vs OBX-14, from
  the ed-tuesday latency config, regenerated THIS session, seed
  20260811, cited to the run). If the ed-tuesday latency profile
  covers no result event type today, add coverage to
  `config-latency.edn` ONLY IF the demo-exerciser and README claims
  survive byte-for-byte on `events.edn` (they must -- ground truth is
  invariant by construction; the exerciser asserts it); otherwise
  STOP-AND-REPORT rather than widen.
- Every strip Step 1 found: regenerate through its gate; ungated ones
  get regenerated by hand AND an errata/dated note, and a roadmap row
  naming the missing gate.
- `docs/formats.md`: only if it describes ORU field lists (Step 1
  census decides); no event-log section edits.
- Roadmap: latency row gains "OBR-7/OBX-14 increment LANDED
  (ADR-0142)"; two named revisits stay: FHIR-side latency, late
  amendments/A08; NEW named revisit: OBR-7 = order-placed `:t` /
  OBR-22 = result `:t` (Q1 b), trigger "a downstream-receiver case
  that needs specimen time distinct from result time".
- ADR-0142 finished: rulings verbatim, audit, prediction vs actual,
  re-baselines, strip census, footprint. `notes/ADRs.md` index row.
  `.agents/rulings.md` "From ADR-0142" (Q1, Q2, scope, the two tag
  payments). Session record; prompt archived to `.agents/prompts/`.
Commit: "docs: ADR-0142 -- clinical time on the result wire; manual
ch. 4 result case, architecture §5 addendum, roadmap and rulings"

## Fences

- src touched: `emit_hl7.clj` ONLY. test touched: sim-emit-hl7 tests
  (+ any pinned test re-baselined with disclosure). No `sim-engine`,
  `sim-emit-fhir`, `sim-model`, `corpus-*`, vendored module JSON,
  event-schema files. `plan-latency`/`emit-wire` signatures frozen.
- Event schema version stays "1.0.0"; the export is byte-identical.
- Oracle: DIFFERS is expected and must match the Step 1 prediction;
  the 27-ish predicted-identical roots must be IDENTICAL.
- Exit codes: every gate unpiped, `MAKE_EXIT` captured, never through
  tail/head. `out/` cleared before any fence or exerciser re-run.
- Register edits are anchored insertions or per-row replacements;
  diffstat checked against intended line count before every commit
  (ADR-0141 near-miss).
- Push at each checkpoint (R30); `bin/post-push-verify` with no
  arguments after each push. Tags beyond Step 0's two: none -- this
  session's own close tag is the author's, deferred to the next
  channel verification.
- STOP-AND-REPORT on: prediction mismatch; any `events.edn` byte
  movement anywhere; a pinned test whose re-baseline you cannot
  explain from the field change alone; ORC-9 or any ADT/ORM field
  tempting you.

## Self-archive

Copy this prompt verbatim to `.agents/prompts/2026-08-16-result-
clinical-time.md` in the Step 4 commit.

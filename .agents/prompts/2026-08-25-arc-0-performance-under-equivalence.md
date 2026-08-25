# Session prompt — arc 0: quadratic removals under equivalence proof
# (traffic-scale program; precedes arc 1; rulings S1(a)/S2(a) 2026-08-24)

## Context

The throughput spike (record `.agents/session-records/2026-08-24-throughput-spike.md`,
landed at d49f1c6) measured generate at slope 1.786 and check at 1.814 over the 10^4→10^5
decade and named the sites by source line. Check: six of 29 invariants are 99.4% of the
phase (`occupancy-within-capacity` alone 54.9%). Generate: `replay` inside the two cancel
decides is 35.3%, the two ADR-0164 citation scans 21.3% + 10.9%. Memory is linear; the
quadratics are time only.

The author has ruled (S1, a) that OUTPUT-IDENTICAL refactors are exempt from the
reshuffle-era constraint that `R-per-person-streams-before-generator-fixes` imposes: only
draw-affecting changes wait for the stream migration. This corrects a recorded channel
error — the plan's "a generator change, so it lands within this era, never before arc 1"
(plan :43 at d49f1c6) conflated "generator change" with "draw-affecting change". And
(S2, a): arc 0 is commissioned, one session, ahead of arc 1, scope exactly the three site
families below.

The obligation for a pure refactor is an EQUIVALENCE PROOF, which is stronger than
red-before-green: byte-identical corpus and identical findings at every gated seed,
asserted by tests that land in the SAME commit as the refactor. Nothing in this session
may move a single draw, event, or finding. If a site cannot be made fast without changing
output, that is a FINDING, rowed, not done.

Design channel is static-read only (no Clojure execution); every line number below is from
a fresh clone at d49f1c6 and will shift as your own edits land — re-derive before citing in
the record.

## Read first

1. `.agents/session-records/2026-08-24-throughput-spike.md` — Step 3's site tables
   (generate :344-349, check :295-299 and :374-386) are the scope.
2. `notes/adr/0168-traffic-scale-program.md` and `.agents/plans/2026-08-24-traffic-scale-program.md`
   — the doctrine you amend.
3. `components/sim-engine/src/ehrt/sim_engine/engine.clj` — `replay` :1142;
   `decide :cancel-transfer` :1194-1208 and `:cancel-discharge` :1210-1222 (each calls
   `(nth (replay ground-truth) idx)` for ONE `:before` state); `last-uncancelled-index` :525;
   the `:medication-end` scan :857 and its `:care-plan-end` twin :897; the run loop's
   `init-world` :1562 — note `:ground-truth` is kept in world as a persistent mirror "so
   decide can nth/filter/keep-indexed over it".
4. `components/sim-check/src/ehrt/sim_check/check.clj` — `no-double-occupancy` :147,
   `admitted-occupies-one-slot` :156, `outpatient-patients-occupy-no-bed` :185,
   `occupancy-within-capacity` :215, `cancel-references-existing-uncancelled-event` :268,
   `no-events-after-merged-terminal` :325; `check-all` :648; the catalogs :630-646.
5. `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` :1376
   `pinned-seed-survives-decide-evolve-refactor` — the repo's own precedent for "refactor
   proven by pinned output".
6. `components/sim/test/ehrt/sim/run_test.clj` :394 `gated-runs` — the four population-scale
   runs (three with `:churn true`) every self-check gate judges; the shared fixture you will pin.
7. `bin/regression-oracle` header — what a regression-oracle claim MEANS here, and
   `components/oracle/src/ehrt/oracle/digest.clj` :575-588 — what the 35 roots do NOT reach:
   the cancel family, `engine/replay`, and `sim-check` entirely. The oracle is therefore
   NECESSARY, NOT SUFFICIENT, for this arc.
8. `components/sim-check/test/ehrt/sim_check/check_test.clj` — the discrimination tests for
   the six invariants (:76, :83, :87, :382, :413, :420 at d49f1c6; find the merged-terminal
   one yourself).
9. `.agents/rulings.md` — `R-move-not-improve`, `R-oracle-script-contract`,
   `R-full-suite-before-push`, `R-per-person-streams-before-generator-fixes`.

## Author rulings, verbatim

* S1: "a" — output-identical refactors (byte-identical corpus + identical findings at fixed
  seeds) are EXEMPT from the reshuffle-era constraint; only draw-affecting changes wait for
  the stream migration.
* S2: "a" — commission arc 0 (performance) ahead of arc 1, one session: (i) six
  occupancy/churn invariants → fold-carried incremental state; (ii) replay-per-cancel → read
  the one element in hand; (iii) fold-carried order indexes retiring the ADR-0164 decide
  scans. All gated by byte-identity + findings-identity tests co-landed.

## Steps

0. Standing checks: `bin/preflight`; HEAD d49f1c6 or descendant; tree clean. Health record
   per the spike's own Step 0 shape (Linux AND Windows-side sample) — you will run the full
   suite at least twice and, conditionally, a 10^5 timed cell.
1. Paper first — ADR-0169 (next free number; confirm) "arc 0: quadratic removals under
   equivalence proof". It records S1 and S2 verbatim, the channel error S1 corrects (quote
   plan :43), the equivalence obligation (what "byte-identical" and "identical findings" mean
   operationally — step 2 defines them), and scope: exactly the three families, everything
   else the spike named (`occupancy-board` :502/:623/:1200, the `decide :discharge` boarder
   `sort-by` :501-505, the 14 independent `replay` calls in check.clj) is OUT — rowed if not
   already, not touched (R-move-not-improve). Edits in the same commit: plan :43 sentence
   amended (keep the original text struck or quoted, do not silently rewrite); roadmap
   `[engine-fold-extensions]` row loses the quadratic removals; new `## Next` row
   `[performance-arc-0]` PRIORITY 1 (1 and 2 are free; lint wants unique ascending, not
   consecutive); rulings register gains `R-output-identical-exempt-from-reshuffle-era` under
   ADR-0169. `make adr-index` / `make state-derived` as the tree's conventions require.
   Commit: `docs: arc 0 commissioned -- output-identical refactors exempt from the reshuffle
   era, S1 corrects the plan's :43 claim, scope fixed to the spike's three site families
   (ADR-0169)`
2. Equivalence gates, BORN GREEN on the unrefactored tree, own commit. (a) Pinned gated
   corpora: for each `gated-runs` entry, pin the sha256 of the ground-truth as the shipped
   writer serialises it (the regression-oracle idiom: digest what `ehrt sim run` writes, not
   `pr-str` in a test) AND assert value-identity `=` of `:ground-truth` against the same
   committed baseline where a full EDN is small enough (adhd-seed-2 is ~10 patients; the
   three scenario runs are 343-407 events each — decide, disclose). Baseline resource under
   the component's own `test/.../fixtures/` beside the existing
   `pinned_seed_42_patients_5.edn` pattern. Failure output must name the first differing
   event index, not just "digest mismatch". (b) Findings identity: the self-check gates
   already assert CLEAN on the gated runs, so "identical findings" there is "still clean".
   For the non-empty case, the six discrimination tests in check_test.clj must assert the
   FULL finding map (`:invariant`, `:ward`, `:at`, ids — the shape each invariant emits at
   d49f1c6), not merely non-emptiness. Tighten any that only checks presence — before the
   refactor, so the tightening is itself born green. If `no-events-after-merged-terminal` has
   no firing test, write one now. (c) Run `bin/regression-oracle d49f1c6 HEAD` once here to
   confirm the harness is green on a docs-and-tests-only delta (a soundness check of your own
   baseline, per R-oracle-script-contract). Commit: `test: equivalence gates for arc 0 born
   green -- gated corpora pinned by shipped-writer digest and value identity, six invariants'
   discrimination tests assert the full finding map (ADR-0169)`
3. Check side, (i). Rewrite the four O(N×P) invariants and the two O(C×N)/O(M×N) churn
   invariants to carry incremental state through ONE fold over the log (per-ward occupant
   index / bed→patient map updated by delta; cancelled-id set and merged-terminal set carried
   forward), emitting the SAME finding maps in the SAME order. Invariant alongside command:
   each finding is a function of (event, state-before-event); if you find one that reads
   `world-after` for something the delta cannot supply, that is a premise finding — report
   the exact read, do not approximate it. Gate co-landed: keep the six ORIGINAL bodies
   verbatim in check_test.clj as `naive-*` reference implementations and add a `defspec`
   (pinned seed, per the ADR-0076 discipline engine_test.clj :583 cites) that generates
   churn-bearing logs and asserts `(= (naive-x log) (fast-x log))` for all six, plus `=` of
   `check-all` findings on every gated corpus and on every check_test mutated fixture.
   Suite-time delta of `sim-check` recorded. Commit: `perf(sim-check): occupancy and churn
   invariants fold-carried -- O(N) not O(N x P); six naive bodies retained as reference
   oracles, findings identical by defspec and on every gated corpus (ADR-0169)`
4. Generate side, (ii). The cancel decides need ONE patient's pre-event state at log index
   `idx`; today they rebuild every patient's state at every index to read it. Carry, in
   world, what `replay` would have handed back for the reinstatable event classes only
   (`:transfer`, `:discharge`, and whatever `:cancel-admit`/`:transfer-in-error` read — check
   their decides, they were not profiled as sites but share the family), keyed so `idx`
   resolves in O(1). `last-uncancelled-index` :525 (5.9%, same family) may ride the same
   carrier IF the carrier answers its query without a second code path; if not, leave it —
   one sanctioned improvement per site. Invariant: `:cancels-event-id`, `:home-ward`,
   `:location`, `:attending` on every emitted cancel event are byte-equal to what
   `(nth (replay ground-truth) idx)` gives post hoc — write that as a test over every gated
   corpus, not as an assertion in the decide. Commit: `perf(sim-engine): cancel decides read
   the reinstated state from a fold-carried index, not a whole-log replay per cancel;
   equivalence asserted post hoc against replay on every gated corpus (ADR-0169)`
5. Generate side, (iii). The `:medication-end` (:857) and `:care-plan-end` (:897) decides
   scan the whole log for the LAST `{patient, citation}` match of `:medication-order` /
   `:care-plan-start`. Carry `{patient-id {citation last-log-index}}` for those two event
   types in world, updated where events are appended. Invariant: `:order-event-id` / the
   care-plan twin on every emitted end event equals the scan's answer recomputed post hoc —
   the ADR-0164/0166 referential invariants already check target existence/patient/citation;
   add the INDEX equality explicitly on every gated corpus and on the ADR-0163 seed-424242
   run, which is in `gated-runs`. Do NOT touch the ADR-0163 compile-time drop or the citation
   shape. Commit: `perf(sim-engine): fold-carried order/care-plan citation indexes retire the
   two ADR-0164 whole-log scans; index equality asserted post hoc on every gated corpus incl.
   seed 424242 (ADR-0169)`
6. After steps 3-5, once: `bin/regression-oracle d49f1c6 HEAD` — must be green with NO
   `--declared-digest-change` (this arc declares none); then full `make test` unpiped on a
   verified-quiet machine (R-full-suite-before-push), wall recorded against the 13m59s
   baseline.
7. Measurement, CONDITIONAL. The spike's driver and dense configs were scratch (record
   appendix :590-615) and are not in the tree. If the scratch directory survives on penny,
   rerun cell C (7,500 patients, 104,851 events) once, warm-up + two timed, with the full
   health record, and convert the plan's 10^5 PROJECTED figure to MEASURED — that label
   change is the only edit permitted to the appendix. If it does not survive, re-author a
   minimal timing driver as scratch within a 45-minute wall budget, disclosed; if that budget
   is exceeded, PROJECTED stands and the record says why. No interpolation is ever promoted
   to MEASURED (F3).
8. Close. Session record per the standing structure (health records, the equivalence evidence
   per site, suite-time before/after, cell-C figure or its absence, deviations by number,
   findings rowed). Archive this prompt at
   `.agents/prompts/<your-date>-arc-0-performance-under-equivalence.md`; regenerate both
   INDEX files and `state-derived`; rotate `## Done` if the new CLOSED row takes it past 30
   lines (currently 23). `bin/close-scaffold` per its usage line. Commit: `docs: close record
   and prompt archive for arc 0 -- three quadratic families removed under equivalence proof,
   oracle undeclared and green, suite and 10^5 timings recorded; deviations disclosed`

## Fences

* F1: NO change to any draw, draw order, event content, event order, or finding
  content/order. The gates of step 2 are the definition. A gate that goes red is a STOP,
  never a reason to re-pin.
* F2: `bin/regression-oracle` runs WITHOUT `--declared-digest-change`. If it demands one,
  STOP-AND-REPORT with the diff.
* F3: byte-identity vs value-identity: if step 2(a)'s digest and `=` gates ever disagree (a
  map re-keyed in a different order prints differently but is `=`), STOP-AND-REPORT — whether
  key order is part of "byte-identical" is an author ruling, not yours.
* F4: R-move-not-improve. One improvement per site; anything else you see (the 14 replays,
  `occupancy-board`, the boarder `sort-by`, the bare `Random.nextInt` death already rowed) is
  a finding.
* F5: premise corrections are findings. If a decide reads something the fold cannot carry, or
  a line above is not what it is described as, report the tree, do not execute the
  description.
* F6: no vendored-module changes, no schema changes, no history rewrites.
* F7: every timed figure carries a health record incl. the Windows-side sample; unpiped
  invocations.
* F8: you date your own artifacts; this prompt carries no template dates.

# 2026-08-13 — Positive-seed invariant violation: diagnosis (ADR-0122)

## Scope

DIAGNOSIS-ONLY session per its own driving prompt: zero `src`, zero
test-code commits — root-cause diagnosis and lettered fix options for
a genuine `clojure.test.check` invariant-catalog violation ADR-0121's
own gate hit, chartered by the author's own 2026-08-13 "Both a."
ruling ((a): diagnose before any fix session runs). Two commits:
commit 1 (records — the ADR-0121 erratum, `.agents/rulings.md` "From
ADR-0122," two roadmap rows); commit 2 (this record's own commit — the
full diagnosis, ADR-0122, plus the close-phase scaffold). No fix of
any kind lands.

## Red→green evidence highlights

Not a red→green session in the code-fix sense — the diagnosis's own
"red" is the reproduction itself, and nothing is fixed. The evidence
chain:

- **Repro.** `clojure.test.check/quick-check`, 150 trials, `:seed
  1786589996178` (the property reconstructed verbatim from
  `engine-test.clj`'s own defspec) — reproduces exactly: same seed,
  same `failing-size 144`. Shrunk minimal counterexample: seed
  `8589258984`.
- **Direct witness.** `engine/run` at the shrunk seed, `check/
  check-all` on the result: one violation,
  `medication-end-references-existing-order-and-follows-it-in-time`,
  patient `PID-000003-fd6d262d`, `:at 436440`. The violating patient's
  own DOB (`2024-12-26`, age 0) sits essentially at this run's own
  fixed registration anchor — a near-birth medication episode whose
  order compiles as a pre-horizon `:registered` fact (history phase)
  while its own end lands in-horizon as a normal ground-truth event
  with `:order-event-id nil`.
- **Root cause.** `decide :medication-end`
  (`components/sim-engine/src/ehrt/sim_engine/engine.clj:774-791`)
  resolves `order-event-id` by scanning only top-level `:medication-
  order` ground-truth events — never the patient's own `:registered`
  event's `:pre-horizon-facts`, which is exactly where a legitimately
  straddling order lives per `compile_trajectory.clj`'s own ratified
  `pre-horizon-fact-types` design (medication order/end pairs are the
  one class explicitly allowed to straddle the registration boundary
  as "ongoing therapeutic content"). The checker itself is confirmed
  genuinely firing on its own current specification — not a false
  positive — but that specification is incomplete against a straddle
  case the compile layer already legitimately produces. A related,
  out-of-scope second-order gap is also named: `evolve :medication-
  end`'s own fold-time resolution silently no-ops for the same reason,
  so the patient's own folded state never shows this medication
  episode at all.
- **Blast estimate.** All 32 of the 35 oracle roots that reach
  `engine/run` (the other 3 are interpreter-layer batches, structurally
  unreachable) were run directly at their own pinned seed/population
  and checked against the violated invariant: **zero emit even one
  `:medication-end` event**. A fix can hold pure oracle identity by
  construction. A grep of vendored module JSON confirms the class is
  real (14 modules author a medication order/end pair) but none of the
  checked roots' own populations happen to land the straddle at their
  pinned seeds.
- **Fix options, lettered, no fix executed.** (a) checker fix,
  RECOMMENDED — widen the invariant to accept a pre-horizon-fact match,
  zero `sim-engine` change, provably oracle-invisible since the checker
  never runs during ground-truth/HL7 generation; (b) engine fix — widen
  `decide`'s own search and add a new event field, oracle-neutral today
  but a live future-drift surface; (c) compile-layer fix, NOT
  recommended — would regress the straddle design's own stated intent
  by discarding real content.

## Judgment calls and their ratification status

- **R8's own scope, clarified [C, channel-inferred from the author's
  own ruling text].** R8 named ONE seed (`7844068501`) as its repro
  handle; ADR-0116 already exercised that license (pinning it found it
  passed clean); a failure at any OTHER seed was always a new finding,
  never covered by R8's retired scope. This clarification, and the
  standing gate policy it grounds (any future generative failure in
  this defspec is a new finding to STOP on, never a re-run), both land
  in `.agents/rulings.md` "From ADR-0122" — un-vetoed, the author may
  strike or correct.
- **The blast estimate ran empirically, not just structurally.** The
  driving prompt allowed either; this session ran all 32 reachable
  oracle-root producers directly rather than resting on the structural
  argument alone, since `digest.clj`'s own equipment made this cheap.
- **The checker-vs-engine framing.** The driving prompt's own Step 2.3
  named "the checker could be wrong -- say so if so." This session's
  own finding is more nuanced than a binary: the checker is not wrong
  about what it currently checks, but its own coverage is incomplete
  against a legitimate, already-designed compile-layer straddle case —
  recorded as such rather than forced into either box.

## Findings and HEAD landed

**One real, genuine invariant-catalog violation, fully diagnosed, not
fixed** (per this session's own explicit charter): root cause traced
to `decide :medication-end`'s own incomplete citation search; the
checker confirmed firing correctly on an incomplete specification;
blast radius empirically zero across all 35 oracle roots; three
lettered fix options recorded with oracle consequences, (a)
recommended.

**The S3 gate event recharacterized**: ADR-0121's own erratum (commit
1) corrects "pre-existing flake, self-cleared" to "genuine new finding,
should have STOPPED" — R8's own scope was narrower than the carry-
forward shorthand implied.

**A ceremony-scripts session chartered** (the author's own "Both a."
ruling, part (b)): tag ceremony, preflight, post-push verification, and
the close-phase scaffold move from re-read prose to scripts, absorbed
into `build-session`, scheduled post-manual-arc.

**Oracle bracket held pure identity** — zero `src`/`test`/`resources`
change this session touches at all; every diagnosis run wrote to
disposable in-process scratch, nothing committed.

**Tag paid forward:** `stable-20260812-manual-s3` tagged at `f483ab7`
(Step 0, this session — the driving prompt's own case (i) license),
peeled ref verified exact match.

**HEAD landed:** commit 1 (`3f0db5e`, the erratum/rulings/roadmap
records) pushed and post-push-verified; this record's own close-phase
commit follows.

# 2026-08-08 — ehr-testing-tools: vendoring batch 4 (the veteran family)

## Context

Conventions read at HEAD `a6640e9` (conviction arc close B, ADR-0089),
design channel, 2026-08-08, verified by fresh public clone. The author
ruled 2026-08-08, verbatim: **"Batch 4"** — the veteran family, the
horizon's first item. Nine candidates (wave-f census, 2026-08-03):
`veteran.json`, `veteran_hyperlipidemia.json`,
`veteran_lung_cancer.json`, `veteran_mdd.json`,
`veteran_prostate_cancer.json`, `veteran_ptsd.json`,
`veteran_self_harm.json`, `veteran_substance_abuse_conditions.json`,
`veteran_substance_abuse_treatment.json`. Their census verdicts predate
Waves G/I/VS/H and the straddle fix (ADR-0086) — treat them as a prior
map, never as current evidence; every disposition this session rests on
FRESH gates.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward,
record HEAD (expect `a6640e9`; later escalates unless explained).
Commits land green; roadmap rows land same-commit.

## Read first

1. `notes/adr/0070…0072` — the batch mechanics (closure enumeration
   incl. non-JSON data files, byte-verbatim at pin, NOTICE hashing,
   defer-with-true-name for failers) and batch 3's disposition table
   format.
2. `notes/adr/0087-colorectal-payoff.md` — the current per-module
   payoff shape: round-trip test at population scale, measured counter
   pins, additive FIRST-BASELINE root.
3. `.agents/rulings.md` — vendored-bytes law, population-scale gate
   ("zero-substance modules are not vendorable"), multi-seed law
   (2–3 well-mixed seeds; THREE once flagged), co-landed invariants,
   and the conviction arc's two new laws (measured-then-pinned;
   licenses bind at their own granularity).
4. `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387-wave-f.edn`
   — the stale prior verdicts, for diffing old vs fresh in ADR-0090.
5. The vendored test family and `digest.clj`'s roots map — the shapes
   every passer's test and root must mirror.

## Author rulings

- **AR-VB4-0 [A]** (ADR-0089, "mechanical debt"): tag
  `stable-20260808-conviction-close` at `a6640e9`, Step 0, ANNOTATED,
  standing ceremony (design-channel verified 2026-08-08).
  Verify-and-disclose if present.
- **AR-VB4-1 [A]** (the batch, ruled "Batch 4"): gate all nine
  candidates FRESH at the pin
  (`/home/mg/synthea-checkout`, `7e08387c…` verified FIRST): closure
  enumerated fresh per candidate (data files included), full
  compile/engine/check round trip, 300 patients, 2–3 well-mixed seeds
  (draw from the standing set; THREE seeds for any candidate that
  flags anything, per the multi-seed law). Disposition per candidate:
  **vendorable** (clean AND substance-producing), **deferred** (fails
  a gate — new Deferred row under its TRUE NAME, the exact invariant
  or gap, never a euphemism), or **blocked** (walk/closure failure —
  same, with the error verbatim). Failures are dispositions, not
  stops; STOP-AND-REPORT is reserved for mechanism surprises (a NEW
  invariant class, an interpreter/compile gap that would need a src
  change to resolve — name it, defer it, do not fix it).
- **AR-VB4-2 [C]** (the attribute gate, this family's own hazard):
  upstream gates veteran modules on the `veteran` patient attribute.
  For each candidate, read its `Initial`/guard structure FIRST; where
  veteran-gated, use the interpreter's existing `:persona-config`
  mechanism (the established precedent for attribute-gated modules) —
  and the persona-config used becomes part of that module's oracle
  root and test, pinned and disclosed. A module whose substance
  requires persona machinery that does NOT exist is a deferred row
  naming the missing attribute — never an interpreter edit, never a
  silent zero-substance vendor. Substance is witnessed at the ENGINE
  layer (real compiled content), not merely a clean walk.
- **AR-VB4-3 [C]** (per-passer landing, the ADR-0087 shape): each
  vendorable module lands byte-verbatim (upstream sha256 in its
  NOTICE row; `.gitattributes` coverage confirmed not assumed) with
  its own round-trip test (its gate seeds; content assertions;
  `:suppressed-straddle-spans` pinned wherever measured nonzero —
  measured-then-pinned, conviction-arc law) and its own additive
  FIRST-BASELINE oracle root — co-landed, grouped into self-contained
  checkpoint commits (a module's bytes, NOTICE, test, and root travel
  together; an interruption must leave a coherent tree — the
  pre-split lesson applied to a batch).
- **AR-VB4-4 [C]** (the bracket): final bracket
  `bin/regression-oracle a6640e9 <tip> --declared-digest-change`
  declaring EXACTLY the additive roots landed — all 29 pre-existing
  roots IDENTICAL, each new root present at target only. Licenses
  bind at their own granularity: any pre-existing root moving is a
  fresh STOP-AND-REPORT.
- **AR-VB4-5 [C]**: no census-tool changes (the closure-file-count
  undercount stays); no pairing-registry rows (future witnessing);
  the stale-census diff (old verdict vs fresh) is an ADR-0090 table,
  and any old `blocked` verdict now passing is named as which wave or
  fix un-blocked it, evidence not guess — "unknown" is an acceptable
  entry, an unearned attribution is not.

## Steps

**Step 0 — Preflight + tag (AR-VB4-0).** Standard preflight (clean
tree, HEAD `a6640e9`, untracked disclosure, `clojure -M:poly check`,
oracle pre-digest `a6640e9 a6640e9` — 29 IDENTICAL, last-five CI
disclosed). Tag. No commit.

**Step 1 — Fresh gates.** Pin verified; nine candidates gated per
AR-VB4-1/2; the disposition table drafted (candidate → closure →
seeds → result → disposition → old-census diff). No commit.

**Step 2 — Land the passers (AR-VB4-3).** Checkpoint commits, each
self-contained, message pattern (one per group, N filled):

    feat: the veteran family comes home, group <N> — <modules>, gated fresh and pinned (batch 4, AR-VB4-1/2/3)

Full suite green before the FINAL group commit (loopback flake: one
independent re-run disambiguates, disclosed, untouched); `gitleaks`
clean per commit; the AR-VB4-4 bracket after the last group; push and
watch CI per checkpoint.

**Step 3 — Record.** `notes/adr/0090-vendoring-batch-4.md` (the
disposition table, the old-vs-fresh census diff, per-passer counter
pins and persona-configs, the bracket manifest, this session's own
successor tag debt); Deferred rows for failers under true names;
index line; README count 87→88; Done pointer. Commit:

    docs: vendoring batch 4 recorded — the veterans gated fresh, passers pinned, failers named true (ADR-0090)

Push; verify; watch CI.

**Step 4 — Ceremony.** Session record + prompt archived
(`2026-08-08-vendoring-batch-4.md`), both READMEs, same commit:

    docs: session record and prompt archive — vendoring batch 4

## Fences

No interpreter/compile/engine/emitter src edits — a module needing one
defers under its true name. No census-tool changes. No pairing rows.
No modules outside the nine. No state.md regeneration.

## Close-out

Echo to chat: the full disposition table, per-passer counter pins and
persona-configs, the bracket result, deferred rows created, shas, CI
status.

## Deviation record

- **AR-VB4-2's own mechanism name, corrected in the open.** The
  ruling named `:persona-config` as "the established precedent for
  attribute-gated modules." Direct inspection of `gmf_interpreter.
  clj`'s own `attribute-condition-holds?` (reads only `(:attributes
  ctx)`, root-namespaced) and `sim_model/persona.clj`'s own `persona`
  function (config-gated draws reach only `:race`/
  `:socioeconomic-category`/`:state`, never a generic Attribute) found
  this premise did not hold: every one of the nine candidates gates on
  a generic `Attribute` condition (`veteran`), which `:persona-config`
  cannot reach at all. The real established precedent is
  `:initial-attributes` (ADR-0033 AR-1, `total_joint_replacement.
  json`'s own `vendored_tjr_test.clj`) — used instead, disclosed in
  NOTICE's own dated section and every new test's own docstring,
  before any gate ran on the wrong premise. Per this repo's own
  fix-forward-with-disclosure discipline (`docs/dev/way-of-working.md`
  §2) rather than a silent substitution or a STOP-AND-REPORT — this is
  a naming correction inside the ruling's own clear intent (seed the
  attribute the interpreter actually checks, disclosed and pinned),
  not a new invariant class or an interpreter gap.
- **Two Deferred rows added to the roadmap for the real, non-zero-
  substance failures** (`veteran_hyperlipidemia.json`'s stale-
  `statin_initial` double-reference; `veteran_mdd.json`'s recurring-
  encounter max-steps exhaustion) — the ruling required "a new
  Deferred row under its TRUE NAME" for a failed gate; this session
  judged both real enough (population-scale, non-seed/horizon-tunable,
  a generalizable defect class rather than a one-off) to warrant their
  own roadmap rows with revisit triggers, matching the weight the
  EncounterEnd/colorectal rows received in prior batches — while the
  two zero-substance candidates (`veteran.json`, `veteran_substance_
  abuse_conditions.json`) get NO dedicated roadmap row, matching batch
  3's own precedent for that class (recorded in the ADR/NOTICE only).
- **Seed counts per candidate, judgment applied within the ruling's
  own 2–3 range.** `veteran_prostate_cancer`/`veteran_ptsd` ran three
  seeds because a nonzero `:suppressed-straddle-spans` measurement
  (not itself a gate failure) was worth a fuller picture;
  `veteran_substance_abuse_treatment` ran three seeds as an
  above-and-beyond disclosed choice given its own real prior census
  instability, not because this session's own fresh gate flagged it.
  The other three vendorable candidates and both zero-substance/
  deferred candidates ran the family's own 2-seed baseline. None of
  this was strictly REQUIRED by AR-VB4-1's own "THREE once flagged"
  clause (a check-all pass is not a flag) — disclosed as judgment, not
  rule-following.
- **`veteran_substance_abuse_treatment.json`'s own old-verdict
  attribution is `unknown`**, per AR-VB4-5's own explicit allowance —
  not bisected between the EncounterEnd fix and the straddle fix (both
  landed the same day, both before this session), which would have
  required a pre-fix worktree and a targeted re-run outside this
  session's own scope.
- **One checkpoint commit for all five passers, not one per module.**
  The ruling's own message-pattern template ("group <N>") anticipated
  possibly more than one group; five modules of comparable size and
  mutual independence landed together in one self-contained commit
  (bytes, NOTICE, tests, and roots all co-landed), matching batch 1's
  own five-modules-in-one-commit precedent rather than splitting
  further.

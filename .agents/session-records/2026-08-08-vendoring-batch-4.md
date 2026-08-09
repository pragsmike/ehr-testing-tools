# 2026-08-08 — Vendoring batch 4: the veteran family comes home, five of nine, and a mechanism name gets corrected in the open

## Scope

Session prompt naming AR-VB4-0 through AR-VB4-5, executing the
author's own ruling "Batch 4" — the veteran family, nine candidates
named from the 2026-08-03 wave-f census, gated FRESH at the pin (the
census verdicts predate Waves G/I/VS/H and the straddle fix, treated
as a prior map, never current evidence).

Preflight: working directory confirmed the ext4 clone, HEAD `a6640e9`
exactly (conviction arc close B, ADR-0089), branch up to date with
`origin/main`, working tree clean. `clojure -M:poly check`: OK. Oracle
self-bracket (`bin/regression-oracle a6640e9 a6640e9`): all 29 roots
IDENTICAL, byte-for-byte, both sides the same commit. Last five CI
runs on `main` all `success`: `31288621176`, `31288276758`,
`31287834460`, `31286768535`, `31286289031` — no red window.

## Step 0 — Tag (AR-VB4-0)

`stable-20260808-conviction-close` did not already exist locally or on
the remote. Created annotated at `a6640e9`, message "conviction arc
close B landed, design-channel-verified 2026-08-08 (ADR-0089)";
pushed; peeled ref verified — resolves exactly to `a6640e9`. No commit
this step.

## Step 1 — Fresh gates (AR-VB4-1/2)

Pin re-confirmed (`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`, working tree clean). A
scratch harness (a `resolve-fn`/`table-resolve-fn` reading the
checkout directly, never landed) ran the closure enumeration and full
compile/engine/check(/emit) round trip per candidate.

**The attribute-gate hazard, confirmed by inspection before any gate
ran:** all nine candidates gate on the upstream `veteran` Person
attribute via the generic `Attribute` condition type. Direct reading
of `gmf_interpreter.clj`'s own `attribute-condition-holds?` (reads
only `(:attributes ctx)`, root-namespaced) and `sim_model/persona.clj`
found the prompt's own named mechanism, `:persona-config`, cannot
reach this condition type at all — it only gates `Race`/
`Socioeconomic`/`State`. The real established precedent is
`:initial-attributes` (ADR-0033 AR-1, the `total_joint_replacement.
json` closure). Corrected in the open; see the archived prompt's own
Deviation record for the full reasoning.

**Disposition table** (full table, both-ways attribute testing, and
horizon-sweep evidence recorded in full in `notes/adr/0090-vendoring-
batch-4.md`):

| Candidate | Result | Disposition |
|---|---|---|
| `veteran.json` | Clean walk, zero clinical states in the file | DEFERRED — zero-substance |
| `veteran_hyperlipidemia.json` | Clean walk, `check-all` FAILS: `:medication-end-references-existing-order-and-follows-it-in-time`, 20+/300 patients, non-horizon-tunable (16000/18000/20000 days) | DEFERRED WHOLE — real invariant violation |
| `veteran_lung_cancer.json` | Clean, real content, HL7 renders | VENDORABLE |
| `veteran_mdd.json` | `run-module` throws max-steps at every horizon tried (36500/18250/3650) | BLOCKED — real interpreter gap |
| `veteran_prostate_cancer.json` | Clean, real content, HL7 renders, `:suppressed-straddle-spans` 2/0/0 | VENDORABLE |
| `veteran_ptsd.json` | Clean, real content, HL7 renders, `:suppressed-straddle-spans` 14/6/7 | VENDORABLE |
| `veteran_self_harm.json` | Clean, real content, HL7 renders (closure incl. new submodule `veterans/veteran_suicide_probabilities.json`) | VENDORABLE |
| `veteran_substance_abuse_conditions.json` | Clean walk, zero clinical states in the file | DEFERRED — zero-substance |
| `veteran_substance_abuse_treatment.json` | Clean, real content, HL7 renders, seeded AND unseeded identically | VENDORABLE |

**Old-census-verdict diff, evidence not guess (AR-VB4-5):**
`veteran_substance_abuse_treatment.json` was `:walk-failed` (the same
max-steps exception) on all three wave-f census seeds; clean this
session both seeded and unseeded. Attribution: `unknown` — not
bisected between the EncounterEnd fix (ADR-0082) and the straddle fix
(ADR-0086), both landed 2026-08-08 before this session, per AR-VB4-5's
own explicit allowance for an evidenced non-attribution.

Five vendorable, four not: two zero-substance, one real invariant
violation (hyperlipidemia), one real interpreter max-steps exhaustion
(mdd). No commit this step.

## Step 2 — Land the passers (AR-VB4-1/2/3), commit `7767326`

Five modules copied byte-verbatim (`veteran_lung_cancer.json`,
`veteran_prostate_cancer.json`, `veteran_ptsd.json`,
`veteran_self_harm.json` plus its own called `veterans/veteran_
suicide_probabilities.json`, `veteran_substance_abuse_treatment.
json`) — `.gitattributes`' own `-text` rule confirmed by grep to cover
the new `veterans/` subdirectory. NOTICE gained six new provenance
rows plus a dated batch-4 section (the disposition table, the
mechanism correction, both true-named deferrals, the old-census diff).

Five `vendored_veteran_<module>_test.clj` files authored: three
single-deftest round trips (`lung_cancer`/`self_harm`/`substance_
abuse_treatment`, `:suppressed-straddle-spans` measured zero — no
counter pin, "no third bucket for the common case"); two two-deftest
files (`prostate_cancer`/`ptsd`) additionally pinning
`:suppressed-straddle-spans` (2/0/0 and 14/6/7 respectively),
measured via `with-redefs` interception at the `ehrt.sim-trajectory.
interface/compile-trajectory` boundary, the ADR-0087 colorectal
precedent. Witnessed green in-session before staging: 7 tests, 50
assertions, 0/0.

`digest.clj` gained five new producer functions and five new `roots`
map entries — purely additive, every existing entry byte-unchanged
(confirmed by diff before staging); each new root run directly and
cross-checked against the committed tests' own live numbers.

`ehrt.docs-tooling.notice-verbatim-test` re-run green: 4 tests, 157
assertions (up from 145). Full suite (`clojure -M:poly test :all
skip:integration`): every project block 0 failures/0 errors, confirmed
by grepping the ENTIRE run output (not just the tail); both new test
files present twice across project groupings. `clojure -M:poly check`:
OK. `gitleaks git --staged -v`: clean. Staging hygiene: `git diff
--cached --stat` showed exactly the thirteen intended files.

**Oracle bracket** (`bin/regression-oracle a6640e9 7767326
--declared-digest-change`): manifest diff confined to exactly five
ADDED lines (`veteran-lung-cancer`/`veteran-prostate-cancer`/`veteran-
ptsd`/`veteran-self-harm`/`veteran-substance-abuse-treatment`) — all
29 pre-existing roots byte-identical.

Committed `7767326` ("feat: the veteran family comes home, group 1 —
lung cancer, prostate cancer, ptsd, self harm, substance abuse
treatment, gated fresh and pinned (batch 4, AR-VB4-1/2/3)"). Pushed;
post-push verification: one delta, the known trailing-blank-line
artifact. CI watched to conclusion: run `31291802190`, `success`,
4m3s.

## Step 3 — Record (`889287d`)

`notes/adr/0090-vendoring-batch-4.md` authored in full (the full
disposition table, both true-named deferrals with their own state-
machine diagnoses, the old-census diff, the mechanism correction, the
bracket manifest). `notes/ADRs.md` gained its index line. `notes/adr/
README.md`'s own file count corrected 87→88 (`ls notes/adr/*.md |
grep -v README | wc -l`, not arithmetic). Roadmap: two new Deferred
rows under their true names (`veteran_hyperlipidemia.json`'s stale-
reference bug, `veteran_mdd.json`'s max-steps exhaustion, each with
its own revisit trigger); the "Now" section updated to this session's
own close; the Done pointer (`- 2026-08-08 — vendoring-batch-4 —
ADR-0090`) added.

`clojure -M:poly check`: OK. Full suite re-run before committing:
clean, 0 failures/0 errors (same full-log grep as Step 2), including
`ehrt.docs-tooling.done-pointer-adr-test`/`index-completeness-test`/
`roadmap-deferred-closure-lint-test`/`notes-prompts-frozen-test` all
green. `git diff --cached --stat` reviewed: exactly the four intended
files (`roadmap.md`, `ADRs.md`, `README.md`, the new ADR file).
`gitleaks git --staged -v`: clean.

Committed `889287d` ("docs: vendoring batch 4 recorded — the veterans
gated fresh, passers pinned, failers named true (ADR-0090)"). Pushed;
post-push verification: one delta, the known trailing-blank-line
artifact. CI watched to conclusion: run `31292291707`, `success`.

## Step 4 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-08-vendoring-batch-4.md` (the
driving prompt, archived verbatim, with a deviation record covering
the mechanism-name correction, the two roadmap rows, the per-candidate
seed-count judgment calls, and the single-commit-for-five-modules
choice) land together, indexed in both READMEs' own entry lists.

## Successor tag debt

Recorded in `notes/adr/0090-vendoring-batch-4.md`: the next session
that opens fresh work tags `stable-20260808-vendoring-batch-4` at this
session's own closing tip.

## Judgment calls and their ratification status

- **The `:persona-config` → `:initial-attributes` mechanism
  correction (AR-VB4-2).** Not separately ratified — found by direct
  inspection before any gate ran, corrected in the open per this
  repo's own fix-forward-with-disclosure discipline rather than a
  STOP-AND-REPORT (the ruling's own clear intent — seed the attribute
  the interpreter actually checks — was never in doubt, only its
  named mechanism). Full reasoning in NOTICE's own dated section,
  every new test's own docstring, and the archived prompt's own
  Deviation record.
- **Two roadmap Deferred rows for the real failures, none for the
  zero-substance pair.** A judgment call on which failures warrant a
  standing revisit-trigger row versus record-in-the-ADR-only,
  calibrated against batch 3's own precedent for zero-substance
  siblings (no dedicated row) and the EncounterEnd/colorectal rows'
  own precedent for generalizable defect classes (dedicated rows).
- **Per-candidate seed counts inside the ruling's own 2–3 range.**
  Three seeds for `veteran_prostate_cancer`/`veteran_ptsd` (a nonzero
  straddle-span measurement, not a gate failure, judged worth a fuller
  picture) and for `veteran_substance_abuse_treatment` (its own real
  prior census instability); two for the rest. None of this was
  strictly required by the multi-seed law's own "THREE once flagged"
  clause — disclosed as judgment.
- **One checkpoint commit for all five passers.** The prompt's own
  message template anticipated possibly multiple groups; five modules
  of comparable size and mutual independence landed together, matching
  batch 1's own five-in-one-commit precedent.

## Findings, disclosed not acted

- **`veteran_hyperlipidemia.json`'s own annual-reassessment loop never
  clears `statin_initial` after ending it once** — a real upstream
  module-authoring pattern (not this project's interpreter inventing
  behavior) that compiles faithfully into a repeated, population-scale
  invariant violation. Named under its true name, not fixed (the
  standing fence).
- **`veteran_mdd.json`'s own recurring therapy-visit cycle genuinely
  advances real time each iteration but still exhausts the
  interpreter's fixed `max-steps` backstop over a multi-decade
  horizon** — the SAME backstop-vs-legitimate-long-loop tension
  `injuries.json`'s own dangling-`dental_referral` gap first named
  (ADR-0070), a structurally different mechanism (that one a true
  zero-advance spin; this one a real, bounded-but-long schedule).
  Named under its true name, not fixed.
- **`veteran_substance_abuse_treatment.json`'s own un-blocking is
  unattributed.** Two candidate fixes (EncounterEnd, straddle) both
  landed the same day, both before this session; which one (or both)
  actually closed the prior loop was not bisected — named `unknown`,
  an evidenced non-attribution per AR-VB4-5's own explicit allowance.

## HEAD landed

`889287d` (Step 3's own commit — Step 4's own commit lands after this
record, in the same push as the prompt archive).

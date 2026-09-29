# `sim describe`: what a corpus proves, beside what it was configured to make possible (ADR-0183)

2026-09-29. Base `1880b76d` (schema 1.10.0). Ceremony: R30, taken from
the session prompt, which stated no prepare-only mode. All commits were
held locally and pushed together with this record, so the RED commit
never reached CI as a tip.

## For downstream

**The verb:** `ehrt sim describe`. **Input** on stdin: the bare
ground-truth vector (`ehrt sim run --format ground-truth | ehrt sim
describe`), or the full `ehrt sim run` envelope. Give it the envelope:
its manifest's `:invocation :opts` records every option the run had,
so each family can say whether it was configured. A bare log says
nothing about configuration, so every row reads `:configured :unknown`.
`--config PATH` is accepted beside a **bare** log only, as your
assertion, and churn still reads `:unknown` there because a config file
cannot record `--churn`. `--witnesses N` defaults to 3, and `--format
text` gives a human view. **Output:** a `result/ok` EDN map carrying its
own **`:describe-version "1.0.0"`** (semver: an added key or row is
minor; a removed key or changed meaning is major), sorted throughout,
so equal inputs give equal bytes. Shape: `docs/formats.md`, "The
describe report". Reading guide: `docs/consuming-ground-truth.md`,
"Describing a corpus".

**The module-cohort row it reports** (dense-7500 `config.edn`, 7,500
arrivals, seed 20260824, `--churn`):
`{:group :module :family "veteran_substance_abuse_treatment" :configured :yes :configured-detail {:assigned-ordinals 134 :ordinal-range [48 7496] :patients 7500 :weighted false} :observed 174 :subjects 88 ...}`.
The config's `:module-assignment` is 937 explicit ordinals, and the
highest is 7,496. At `--patients 28000` the same `:ordinal-range` sits
beside `:patients 28000`, so the cohort does not grow: 3.3% of arrivals
instead of 12.5%.

## Author rulings

- 2026-09-29, "Go": slice 1 lands now (the prompt).
- 2026-09-29, asked mid-session before any façade edit: "widen
  ehrt.sim.interface by `describe-command`, arity 2, additive only.
  Record it as BASELINE EDIT 2 in the gate literal and in ADR-0183,
  mirroring check-command (2026-09-01)." The prompt had named that
  export as a channel expectation, "not rulings"; the surface is frozen
  by AR-M4-3, and its gate turns red on any added var.

## Commits

| sha | what |
|---|---|
| `9c7cad08` | RED: step 1's gates (sim-check `describe_test.clj` + golden; sim `run_test.clj`) |
| `e9118fa8` | the report, the catalog, `describe-command`, the ruled façade edit |
| `e052eb81` | the CLI verb, corpus adapter, help entry, `docs/cli.md` |
| `c6fd00ea` | docs: consuming-ground-truth "Describing a corpus", formats "The describe report" |
| `3f642b38` | ADR-0183 + `notes/ADRs.md` |
| this commit | record + prompt archive (+ generated indexes) |

## Where the tree differed from the prompt

1. **`sim check` does not read the envelope.** `read-ground-truth-stdin`
   rejects any non-vector as `:malformed-input`, so "same input
   contract as `sim check` ... bare vector or envelope" was only half
   true. `describe` reads both: the reader gained a 1-arity that lets a
   map through, and `sim check`/`sim mutate` keep the 0-arity
   unchanged.
2. **`ehrt.sim.interface` is frozen**, so the author ruled the edit
   (above). `interface_surface_test` went red on the widened façade
   before the baseline literal moved.
3. **Three of step 3's five measure names had no committed
   definition.** `result-after-merge`, and in effect
   `pending-result-at-discharge` (the census's `result-after-discharge`),
   come from `.agents/plans/2026-09-05-performance-measurement/census-src/`.
   `bed-reoccupied-by-someone-else` is a decide-time guard
   (`log_index.clj:217`); the log carries it only as the two
   `-bed-reoccupied` `:step-rejected` reasons, and that is what the row
   counts. `reinstated-stay-without-closer` is defined as a
   `:cancel-discharge` whose subject is never discharged again, citing
   `every-encounter-is-opened-and-closed-or-still-open`.
   `transfer-between-order-and-result` is a `:transfer` of the result's
   subject logged between the cited order and the result. That is
   narrower than the census's location-moved `result-after-move`,
   because the row's name says transfer.
4. **`:citation :module` is not only a module's stamp.** At the 750
   cell, the first cut reported `dense-inpatient` (an authored pathway
   step's own `:citation`) and `medications/otc_pain_reliever` (a
   submodule) as `:configured :no`, which is a claim the log does not
   support. Both now read `:unknown` with the reason, and a test covers
   it. The pathway row's reason was also reworded: an authored citation
   can coincide with a pathway name, so "no event carries the name" was
   false as written.
5. **The manifest's `:config` is always `{:path "(inline)" :sha256
   "000…"}`.** `describe` copies it as written, and ADR-0183 names a
   real config hash as slice-2 work.
6. **The `immunization` root is an engine-level run** (`engine-pair`,
   no manifest), so the golden describes a bare log. Its producer config
   is a third hand-kept copy (after `digest.clj` and `engine_test.clj`).
   The golden pins the log's sha256, so drift fails the test rather than
   passing.

## Gates, red then green

- Step 1, red at baseline: in a disposable worktree at `1880b76d`,
  requiring `ehrt.sim-check.describe-test` fails with
  `FileNotFoundException` (no `ehrt/sim_check/describe.clj`), and
  `ehrt.sim.run-test` fails with `No such var: run/describe-command`.
- `brick:sim-check :all skip:integration`: `describe-test` passes 70
  assertions, and every namespace is green.
- `brick:sim :all skip:integration`: red first on
  `sim-interface-surface-matches-its-frozen-baseline-test` (façade
  widened, literal unmoved), then green after BASELINE EDIT 2.
- `brick:cli :all skip:integration`: four registries went red on the
  new verb (`every-spec-command-pair-actually-routes-in-dispatch-test`,
  `spec-command-pairs-match-dispatchs-known-routes-test`,
  `cli-md-is-current-test`, and
  `dispatch-help-three-arg-form-unknown-verb-is-the-f6-treatment-test`),
  then green.
- Step 4: in `predicate-fails-equal-the-invariants-violations-on-the-same-log`,
  every predicate's `:fails` equals its invariant's violation count from
  `check-all`, on a clean churned log (40 patients, merges present) and
  on the same log with a defect planted for each predicate. Each defect
  is seen, so the equalities are not 0 = 0.

## Measured (step 6)

`demos/scenarios/dense-7500/config.edn`, seed 20260824, `--churn`, on
the WSL2 i7-10750H host `figures.edn` names. Two timed runs per verb
per cell under `/usr/bin/time -v`; describe read the envelope, and
check read the bare vector with `--config`.

| cell | events | verb | wall (s) | peak RSS (MiB) |
|---|---|---|---|---|
| 750 | 33,306 | describe | 14.22 / 14.31 | 562 / 508 |
| 750 | 33,306 | check | 14.41 / 17.62 | 457 / 541 |
| 7,500 | 167,197 | describe | 29.63 / 30.69 | 962 / 1473 |
| 7,500 | 167,197 | check | 33.35 / 34.39 | 1334 / 1195 |

Mean RSS is 535 vs 499 MiB at 750 and 1218 vs 1265 MiB at 7,500
(describe vs check). Two identical runs of one verb differ by up to
53%, so this shows no report-sized memory term, but it cannot bound
one finer than that noise. Describe's two runs at each cell were
byte-identical. The 7,500 cell's `:log-sha256` is `096959857076…c2b2`,
the `events.edn` digest `figures.edn` recorded, and its
`result-after-merge` reads 21, matching the docs' hand census.

## Close-out

- **Regression oracle:** `bin/regression-oracle 1880b76d c6fd00ea`
  (the last code commit) reports "IDENTICAL: every root's digest
  matches", exit 0. No corpus byte moves.
- **`make docsgen`**, run after this record existed, regenerated only
  the two INDEX.md files and `.agents/state-derived.md`: one more test
  namespace, ADR, record and prompt, `:sim` headroom 58 → 51, and
  `:corpus` headroom 145 → 144. All three are committed with this
  record.
- **Full `make test`**, unpiped, at `3f642b38` with this commit's
  generated files in the tree: the wrapper recorded `MAKE_EXIT=0` and
  ended `exit "$MAKE_EXIT"`. `poly check` printed OK. There were 432
  `Test results` lines summing to 29,049 passes, 0 failures and
  0 errors.
- Push, post-push message verification and CI: recorded in the reply
  that closes the session. CI green on the tip is the close marker.

## Not done, named

- **Slice 2:** a manifest assignment record (per-ordinal pathway and
  module), which would make pathway rows observable; and a real config
  hash in the manifest.
- **Pre-existing, not this session's:** `bin/charter-completeness`
  reports `sim-emit-hl7/stamp-log-index` (from ADR-0181) missing from
  `components/sim-emit-hl7/docs/charter.md`. The three charters this
  session touched (`sim`, `sim-check`, `corpus`) are complete.

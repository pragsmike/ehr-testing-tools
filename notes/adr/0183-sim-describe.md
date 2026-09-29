## ADR-0183 — `sim describe`: what a corpus proves, beside what its configuration made possible

**Status:** Accepted 2026-09-29 (author ruling "Go" for slice 1; build
session same day, R30). Report version **`:describe-version "1.0.0"`**.

### Context

Downstream asked on 2026-09-29 for a deterministic, versioned,
machine-readable answer to "what did this corpus actually contain,
versus what was merely configured to be possible?". Its first concrete
case is `demos/scenarios/dense-7500`: that config's `:module-assignment`
is 937 per-ordinal entries whose highest ordinal is 7,496. A 28,000
arrival run of the same config therefore has the module cohort of a
7,500 run: 3.3% of its patients rather than 12.5%. Nothing in the tree
said so. The consumer doc lists which config keys make which kinds
*possible*, and `bin/event-census` tabulates shape across schema
versions. Neither sets configured beside observed.

`ehrt sim check` answers a different question: whether the log is
internally consistent, not what it contains.

### Decision

1. **The surface.** `ehrt sim describe` reads the same stdin as `sim
   check`, and also accepts the `sim run` `:ok` envelope, whose manifest
   identifies the run. (`sim check` accepts the bare vector only; the
   prompt's "same input contract, bare vector or envelope" was
   corrected from the tree.) It prints a `result/ok` EDN map carrying
   its own `:describe-version`. `--format text` renders the same map for
   a human. `--witnesses N` (default 3) sets how many witnesses each row
   keeps. The call path is the CLI → corpus `sim-describe!` →
   `ehrt.sim.interface/describe-command` → `ehrt.sim.run/describe-command`
   → `ehrt.sim-check.interface/describe`.
2. **The frozen façade moves, by ruling.** `ehrt.sim.interface` is
   frozen (AR-M4-3). `describe-command`, arity 2, is **BASELINE EDIT 2**
   of `interface_surface_test`. The author ruled it on 2026-09-29 as
   additive only, on the same ground as edit 1 (`check-command`):
   `describe` needs `merge-config-file`, and the sim verbs keep one pair
   of `:config-not-found`/`:config-unreadable` rejections. The session
   asked before widening, and the gate went red on the widened façade
   before its literal moved.
3. **Where configuration comes from.** An envelope's manifest records
   the run's merged options in `:invocation :opts`, config file and
   flags together, so it is complete (`:configured-from
   :manifest-invocation`). `--config` goes with a **bare** log only
   (`:caller-config`), and it stands for the caller's assertion. A
   config file cannot record `--churn`, so churn reads `:unknown`
   unless the file names `:churn` or `:churn-profile`. An envelope plus
   `--config` is rejected (`:config-with-envelope`). A bare log with no
   config is `:configured-from :none`.
4. **The unknown/unprovable rule.** The report never contains a claim
   the log does not carry. If the input doesn't say something, the
   report says `:unknown`, never a guess. If no log could say it, the
   report says `:unprovable`, with the reason. A measure (a situation
   no key turns on by itself) is `:configured :emergent`. A `:citation
   :module` the configuration does not list is `:unknown`, not `:no`,
   because it may be an authored step's own citation or a submodule a
   listed module calls, and the log does not say which. dense-7500 has
   both.
5. **One pass, bounded accumulators.** `describe` folds
   `engine/replay`'s records once. It keeps counts, per-kind and
   per-module subject sets, three per-patient facts and at most `k`
   witnesses per row, in ascending log index. It makes two cheap walks
   of the raw log besides: the input hash, and `check/merges-forward`.
   That second walk is needed because a merge can share a `:t` with a
   result logged before it, so an incremental reading would disagree
   with the checker.
6. **The family catalog is data** in `describe_catalog.clj`. Each row
   holds `:configured-by`, `:observed-by`, `:witness-fields` and
   `:cites`, and every observed predicate cites the invariant or engine
   rule it reads. Slice 1's rows:
   - churn, six rows;
   - scheduling outcomes, four rows;
   - `:bed-cycle`, repeat encounter, persons-bound, placeholder and
     `:immunization`;
   - cited modules, observed by `:citation :module` against `:modules`;
   - pathway names: configured `:yes`, observed `:unprovable`;
   - five measures: result-after-merge, pending-result-at-discharge,
     transfer-between-order-and-result, bed-reoccupied-by-someone-else
     and reinstated-stay-without-closer.

   Three of the measure names had no committed definition, so each is
   defined in the row. `bed-reoccupied-by-someone-else` is a decide-time
   guard; the log shows it as the two `-bed-reoccupied`
   `:step-rejected` reasons. `reinstated-stay-without-closer` is a
   `:cancel-discharge` whose subject is never discharged again.
   `transfer-between-order-and-result` is a `:transfer` of the result's
   subject logged between the cited order and the result.
7. **Four predicates, each counted both ways.** They are
   same-subject opener/closer, order-before-result,
   result-belongs-to-order (through merges) and during-encounter. They
   read the relationships through `check.clj` itself. Its six helpers
   (`encounter-openers`, `encounter-closers`, `encounter-id-of`,
   `carried-encounter-is-not-the-open-one?`, `merges-forward`,
   `resolves-through-merges?`) went public for this. The contract is
   that `:fails` IS the invariant's violation count, so this is not an
   independent instrument (unlike the perf census, which deliberately
   re-derives). `describe-test` asserts the equality against `check-all`
   on a clean churned log and on one with a planted defect per
   predicate.
8. **Witnesses** are `{:index :t :patient-id :encounter-id :related}`,
   and absent fields are left out. `:index` is the log position, stable
   and the suffix of every MSH-10 the event lowers to (ADR-0181).
9. **The version contract.** Semver on `:describe-version`: an added
   key or row is minor, and a removed key or changed meaning is major.
   Every map in the report is sorted, so equal inputs print equal
   bytes; the golden report for the `immunization` root is asserted
   `=`.

### Slice 2, named

**A manifest assignment record**: which pathway and which module each
arrival ordinal was actually assigned, written by the run. With it, the
pathway rows stop being `:unprovable` and the module cohort is observed
rather than inferred from `:configured-detail`. A second slice-2 item:
the manifest's `:config` is always `{:path "(inline)" :sha256 "000…"}`.
The report copies it as written; a real config hash belongs to the run.

### Measured

`demos/scenarios/dense-7500/config.edn`, seed 20260824, `--churn`, on
the machine `figures.edn` names (WSL2, i7-10750H, JVM defaults). There
were two timed runs per verb per cell, one JVM each, under `/usr/bin/time
-v`. `describe` read the envelope, and `sim check --config` read the
bare vector of the same run.

| cell | events | verb | wall (s), run 1 / 2 | peak RSS (MiB), run 1 / 2 |
|---|---|---|---|---|
| 750 | 33,306 | `sim describe` | 14.22 / 14.31 | 562 / 508 |
| 750 | 33,306 | `sim check` | 14.41 / 17.62 | 457 / 541 |
| 7,500 | 167,197 | `sim describe` | 29.63 / 30.69 | 962 / 1473 |
| 7,500 | 167,197 | `sim check` | 33.35 / 34.39 | 1334 / 1195 |

**Memory is replay's, not the report's, to the resolution this method
has.** The mean peak RSS is 535 vs 499 MiB at 750 (describe vs check)
and 1218 vs 1265 MiB at 7,500. Two identical runs of one verb differ by
up to 53% (describe at 7,500: 962 vs 1473), because a default-heap JVM's
RSS reflects when it chose to grow the heap, not what it retained. So
this shows no report-sized term, but it cannot bound one finer than
that noise. The walls favour describe at 7,500 (30.2 vs 33.9 s mean). Describe's two runs at
each cell wrote byte-identical reports.

The 7,500 cell's `:log-sha256` is `09695985…c2b2`, the `events.edn`
digest `figures.edn`'s provenance recorded for that cell. That
independently confirms the hash covers the log's own bytes.
`result-after-merge` reads 21 there, which matches the hand census the
docs already quote.

### Consequences

- The consumer doc gains "Describing a corpus", which uses the
  dense-7500 module cohort as its configured-versus-observed row, and
  `docs/formats.md` gains the report as a versioned format.
- `ehrt.sim.interface` has 12 vars. `sim-check`'s seam gains
  `describe`/`describe-text`, and corpus gains `sim-describe!`.
- **No corpus byte moves.** `bin/regression-oracle 1880b76d c6fd00ea`
  reports "IDENTICAL: every root's digest matches" (exit 0), with the
  `immunization` root included. No generator code changed, and the
  check.clj edits only change six vars from private to public.
- No roadmap row: slice 2 is named here, not queued.

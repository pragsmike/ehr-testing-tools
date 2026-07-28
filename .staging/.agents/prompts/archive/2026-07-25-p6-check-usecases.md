# P6 — Check capability, union notation, use-case doc, baseline gating

You are working in `ehr-testing-tools` (public). This session adds the
second judge (Check: assertions and golden-equivalence against
expectations, complementing Gate's standards), extends the notation with
union resources (recording pattern #13's promotion), makes gating usable
on messy real-world corpora (baseline-relative mode), and ships the
formal use-cases document with equations and diagrams per case.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md`,
`.agents/memory/patterns.md`, `docs/notation.md`, `docs/pipeline.edn` and
the generator in `src/…/pipeline.clj`, `docs/gate-calibration.md`,
EXP-C5 results, `src/ehr_testing_tools/gate/` and `corpus/`,
`docs/use-cases.md` if any exists (none expected). Test-first per
ADR-0006; red→green evidence. Ritual (note Step 0 changes it): commit →
`git push origin`. Save this prompt to
`.agents/prompts/2026-07-25-p6-check-usecases.md`; final commit archives
it.

Author rulings in effect: pack-push retired from the ritual; pattern #13
promoted to validated with the tier-1 lint reclassified to a built-now
deliverable (Step 5) rather than a promotion criterion; union resources
adopted; Check is gate-kind; equivalence is defined as equality of
canonicalized forms under a declared canonicalizer list.

## Step 0 — Ritual simplification (packs retired from ceremony)

Both repos are public; the design channel clones directly. Amend
AUTHORS-GUIDE and AGENTS.md: the session ritual is now commit →
`git push origin` — `make pack`/`pack-skills` remain as utilities for
feeding non-git AI surfaces (say exactly that), and `pack-push` plus the
packs repo are dormant, not deleted. Add one README line to `~/.packs`'
repo? No — out of scope; just note dormancy in the Makefile comment.

Commit: `Ritual: retire pack-push (repos public; direct clone)`.

## Step 1 — Union resources; #13 promotion recorded

1. `docs/notation.md`: define union resources — a named resource
   declared as the union of others (`{:resource :datum :union-of
   [:generated-datum :mutant :intaken-datum]}`); a stage consuming the
   union accepts any member; the diagram renders a merge node feeding
   the consumer; type-binding: a union's soft/hard type is the union of
   members' types. Also define the external stage marker for black-box
   stages the repo doesn't implement (a user's transform): rendered
   dashed (the Gate-planned styling precedent), carrying inputs/outputs
   but no laws.
2. `docs/pipeline.edn` schema + generator: support both constructs,
   test-first (schema tests; a rendering test asserting the merge node
   and dashed style appear). Rewire Gate's input to `:datum` — this
   resolves the disconnected-wire gap P5 flagged. Regenerate
   `docs/pipeline.md`.
3. `.agents/memory/patterns.md` #13: status → validated; evidence: P4
   diagram catch (cross-stage wiring), P5 authoring catch (format-
   dispatch forced pre-implementation), conformance across isolated
   sessions, single-source held; refinements ratified: fourth catalytic
   target (P4), union resources and external stages (this session);
   tier-1 lint delivered in Step 5.

Commit: `Notation: union resources, external stages; #13 validated`.

## Step 2 — The Check capability (test-first)

`ehr-testing-tools.check` — dataset vs expectations. Design:

* Assertions are data (EDN, Malli-schema'd, versioned as a set): v1
  vocabulary, deliberately small:
   * `{:kind :matches-expected}` — corpus-level golden equivalence
     (below);
   * `{:kind :present :locator L}` / `{:kind :absent :locator L}`;
   * `{:kind :value :locator L :expected v}`;
   * `{:kind :count :locator L :op :=|:<=|:>= :value n}` (count of
     elements at a locator path);
   * `{:kind :schema :malli <schema-ref>}` — validates each datum
     against a named Malli schema from a registry entry (fourth
     catalytic target: reference by id+version).
* Equivalence (`:matches-expected`): candidate corpus vs expected
  corpus; files paired by relative path (default) or by content-hash
  identity (`:pair-by` option); each pair compared as `(= (canon x cs)
  (canon y cs))` for a declared, ordered canonicalizer list `cs`
  (ids+versions recorded in the report; empty list = byte equality);
  differences reported with locator paths via the existing diff
  machinery; unpaired files reported as missing/extra findings.
* Verdict semantics identical to Gate (it is gate-kind): any failed
  assertion → `:rejected`; nothing maps to `:indeterminate` in v1
  (state it); findings use the shared envelope (`gate.finding`),
  `:engine {:name "check" :version …}`.
* Gate-kind law test: inputs byte-identical after checking.
* Report: reuses `gate.report` aggregation (verify it's format-agnostic
  enough; refactor minimally if not — report any refactor).
* CLI: `ehr check DIR --expected DIR --assertions FILE
  [--canonicalizers id@v,…] [--pair-by path|hash] [--report …] [--json]`;
  exit codes standard. `--assertions` optional when `--expected` given
  (implies `[{:kind :matches-expected}]`).
* Pipeline: add the Check stage equation to `pipeline.edn` (consumes
  `:datum`; catalytic: expected-corpus, assertion-set, canonicalizer-set
  — note expected-corpus and assertion-set resolve as hashed repo-
  authored config or intaken artifacts); regenerate.
* Tests must include: golden-equivalence pass and fail (with a
  canonicalizer making an "inequivalent" pair equivalent —
  demonstrating equivalence-is-canonical-equality); each assertion kind
  red→green; pairing edge cases (missing/extra).

Commit(s): `check: assertion vocabulary, golden equivalence, CLI`.

## Step 3 — Baseline-relative gating

Motivated by EXP-C5's discovery (profile-stamped corpora carry
pre-existing findings; file-level verdicts can't discriminate):

* `ehr gate fhir|v2 DIR --baseline report.edn`: gate as normal, then
  compute per-file verdicts relative to the baseline report — a finding
  counts toward rejection only if not present in the baseline for that
  file, matched by exact `{severity, code, locator-path}` triple
  (document the exact-match limitation and why fuzzier matching is
  future work). The absolute findings remain in the report
  (`:absolute` section) alongside `:relative`; exit code follows the
  relative verdict.
* Implemented in `gate.report` (it owns diffing), test-first with
  fixture reports; one integration-tagged test against real validator
  output reusing EXP-C5 artifacts if present, else a fresh small run.
* `docs/gate-calibration.md`: a short dated section explaining when to
  use baseline mode (real-world corpora, validator upgrades, drift
  detection) and its matching semantics.

Commit: `gate: baseline-relative verdicts (report-diff powered)`.

## Step 4 — The use-cases document (equation-anchored)

1. `docs/use-cases.edn`: one entry per use case — id, title, audience
   line, "you bring", "you get", maturity, and an equation set (reusing
   pipeline stage names; external stages for user components; union
   resources where sources vary). The cases (author-specified plus
   design-channel additions — keep each terse):
   1. Generate conforming synthetic data (unspecified downstream use)
   2. Generate controlled-fault data (unspecified downstream use)
   3. Test a validator/gate with generated + mutated data (contract
      pairing as the exemplar)
   4. Judge user-supplied data: intake → gate → report (with baseline
      mode noted)
   5. Black-box transform surround: generate → external Transform →
      gate + check-vs-expected
   6. Mutation-adequacy of the user's own checks (mutants through their
      validation; do they catch?)
   7. Regression baselining / drift detection (periodic gate runs,
      report diff over time)
   8. Differential A/B of two transform versions (same corpus, check
      new-output against old-output as expected)
   9. Acceptance QA of delivered/vendor corpora
   10. Reproduction packages (manifest as a shareable regenerate-
       exactly-this)
   11. Audit/regulatory evidence trail (lineage + manifests + reports)
   12. Gate-tier calibration studies (gate-calibration.md as the first
       instance)
   13. Training material (labeled defects for teaching)
   14. Bring-your-own-generator augmentation (foreign corpus + our
       faults/lineage)
2. Generator: extend the pipeline renderer (or a sibling) to render
   `docs/use-cases.md` from the EDN — per case: title, audience/ bring/
   get lines, the equation(s), the mermaid diagram, maturity. `make
   use-cases` target; generated-file header; test that rendering runs
   and contains each case id.
3. `docs/README.md` reading order: use-cases.md added prominently
   (second, after SETUP); README links it from the audience paragraph
   ("what you can do with this, formally: use cases").

Commit: `docs/use-cases: equation-anchored catalog + renderer`.

## Step 5 — Tier-1 pipeline lint

`make lint-pipeline` (and a unit-test harness): every catalytic
resource in `pipeline.edn` and `use-cases.edn` resolves to one of the
four targets — `artifacts.lock.edn` entry, `deps.edn` coordinate, hashed
repo-authored config path, or in-repo registry `{id,version}`
(operator/canonicalizer/schema registries); external stages exempt.
Test by seeding a deliberate violation in a test fixture and asserting
the lint catches it. Note CI wiring in the enforcement-wave plan entry
(don't add to CI yet).

Commit: `lint-pipeline: catalytic resources resolve (tier 1)`.

## Step 6 — Bookkeeping and finalize

* README maturity table: `check` added as experimental (one honest
  scope line); nursery: any evidence-backed notes (conservative); plan
  file: P6 done; P7 sketched — v2 mutation (awaiting a foreign corpus
  sample), IG pinning (awaiting the team's target profile), EXP-D3,
  enforcement wave items now including lint-in-CI.
* `make test` green; `make coverage` headline (justify regressions);
  archive prompt; commit → push origin (verify up-to-date). No
  pack-push (Step 0).
* Report: assertion vocabulary as committed; the equivalence-via-
  canonicalizer test description; baseline-mode matching semantics; the
  use-cases catalog (ids + one-liners); lint seeded-violation evidence;
  union/external rendering evidence; #13 promotion text as recorded;
  red→green; coverage; commits.

## Out of scope

No v2 mutation, no IG pinning, no EXP-D3, no assertion kinds beyond the
v1 vocabulary (no arbitrary predicate functions in v1 — data only), no
fuzzy baseline matching, no guide-repo edits, no CI changes beyond the
plan note, no deletion of pack machinery or the packs repo.

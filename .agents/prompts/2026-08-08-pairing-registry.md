# 2026-08-08 — ehr-testing-tools: pairing-as-data lands (the registry)

## Context

Conventions read at HEAD `5168d3b` (colorectal payoff, ADR-0087),
design channel, 2026-08-08, verified by fresh public clone (including
upstream-at-pin byte verification of the vendored module). This session
lands the pairing-as-data design pass — ruled IN at the quality review
(D7-5, ADR-0080), carried two closes, its four shape questions ruled by
the author 2026-08-08 in the design channel ("Approve recommendations"
over the four recommendations quoted verbatim in the rulings below).
The pass's thesis, standing since the NIST spike: the mutate↔judge
pairing (defect class injected → finding class expected) becomes DATA,
checked by execution, not prose.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward,
record HEAD (expect `5168d3b`; later escalates unless explained).
Commits land green; roadmap rows land same-commit.

## Read first

1. The prior art its own envelope already cites: `ehrt.judge.finding`'s
   `Severity` docstring names "P5's contract-pairing exercise" —
   locate that record (grep the ADRs/plans for it), read it, and cite
   it in ADR-0088; this session REGISTERS what that exercise witnessed
   ad hoc, and must not contradict it silently.
2. `components/corpus/src/ehrt/corpus/operators.clj` — the catalog:
   per-entry `:contract` (with `:target` naming the violated base-spec
   constraint), `:default-locator` ("canonical conviction target",
   D12/ADR-0019), the v2 operator family.
3. `components/judge/src/ehrt/judge/finding.clj` — the shared envelope
   (Severity/Verdict/Cause) the registry's expected classes must speak.
4. `components/judge-v2-hapi/` and `components/judge-v2-nist/` — each
   judge's finding-category vocabulary as actually emitted (read the
   src, then MEASURE in-session; pin nothing unmeasured).
5. Fixtures: `components/corpus/test-fixtures/v2/` (the ADT trio) and
   `components/corpus/test-fixtures/v2-nist/COVID19_ELR-v2.3.1` +
   `covidELR` (the committed Π fixture, with its NOTICE.md).
6. `.agents/rulings.md` — co-landed invariants; the resource-nesting
   convention (confirm the exact nesting from a sibling component
   before creating any resource file).

## Author rulings (all four ruled 2026-08-08, "Approve recommendations")

- **AR-PD-0 [A]** (ADR-0087, "Successor tag debt"): tag
  `stable-20260808-colorectal-payoff` at `5168d3b`, Step 0, standing
  ceremony (design-channel verified 2026-08-08). Verify-and-disclose if
  present.
- **AR-PD-1 [A]** (Q1 — granularity): the registry is PER-OPERATOR
  WITNESSED ROWS, never a matrix. Row shape:
  `{:operator {:id … :version …} :judge … :expected #{…finding
  classes…} :fixture "…path…" :witness {:adr "0088" :date …}}` — a row
  lands ONLY when exercised in-session (mutate → judge → observed
  class). Unwitnessed cells do not exist; a full operators × judges ×
  categories matrix is unearned specificity by construction. Landing
  spot: the `judge` component — a committed EDN resource (nested per
  the live convention) + an `ehrt.judge.pairing` namespace (loader,
  malli schema, interface re-export). Operators reference the registry
  never the reverse — the dependency arrow keeps its existing
  direction.
- **AR-PD-2 [A]** (Q2 — ordering): v2 FIRST, on the existing committed
  fixtures — the ADT trio for judge-v2-hapi, the covidELR Π fixture
  (and its test message) for judge-v2-nist. NO FHIR rows this session:
  the storefront-fixture session lands those as its own acceptance
  proof, later. Rows this session: each v2 operator the session can
  genuinely witness against each of the two v2 judges — witnessed
  subset, not forced completeness; an operator/judge pair that can't be
  cleanly witnessed on existing fixtures is SKIPPED and named in the
  ADR, not forced.
- **AR-PD-3 [A]** (Q3 — the taxonomy snapshot): a NAMES-ONLY EDN
  snapshot of the NIST engine's finding vocabulary (classifications +
  category names, engine version recorded, NO config bodies), committed
  in `judge-v2-nist`'s resources (nested per convention), plus a
  CURRENCY GATE test that re-derives the names from the resolved jar's
  own `reference.conf` on every run and fails on drift — the
  `notice_verbatim` shape applied to a vocabulary. Registry rows whose
  `:judge` is the NIST judge must draw `:expected` from this snapshot
  (a schema-level check). The snapshot is factual reference; note it in
  the pending NIST licensing inquiry's scope in the ADR rather than
  assuming it settled.
- **AR-PD-4 [A]** (Q4 — consumers, two tiers): TIER ONE, gated,
  co-landed with the rows it protects: a per-row executable test (in
  `judge`'s test tree; test context may cross bricks) — for every
  registry row: load `:fixture`, apply `:operator` via the corpus
  interface, run `:judge`, assert at least one `:expected` class among
  the findings. The inject-X-expect-X loop, closed by execution.
  TIER TWO, REPORT-ONLY: adequacy-as-coverage (operators lacking any
  witnessed row) is COMPUTED (a pure helper fn) and RECORDED in
  ADR-0088 as a table — it does NOT gate; promoting it to a gate is a
  future dated ruling once FHIR rows exist. A tier-two test that fails
  on missing coverage is explicitly out of scope.
- **AR-PD-5 [C]** (measurement discipline, the counter-pin precedent):
  every `:expected` set is MEASURED in-session before it is written —
  run the mutation, run the judge, transcribe the observed classes into
  the ADR, then pin. If an observed class CONTRADICTS the operator's
  own `:contract` prose, that is a FINDING about the catalog, recorded
  in ADR-0088 and STOP-AND-REPORTED if it would require changing the
  operator — the catalog is not this session's to edit.
- **AR-PD-6 [C]** (STOP-AND-REPORT conditions): reference.conf not
  cleanly derivable to names-only; any needed change to operator or
  judge SRC behavior; the P5 prior art contradicting a measured row;
  fixture insufficiency that tempts authoring new fixtures beyond a
  trivially-derived mutant input (new fixture authoring is the
  storefront session's job).

## Steps

**Step 0 — Preflight + tag (AR-PD-0).** Standard preflight (clean
tree, HEAD `5168d3b`, untracked disclosure, `clojure -M:poly check`,
oracle pre-digest `5168d3b 5168d3b` — 29 roots IDENTICAL now,
last-five CI disclosed). Tag. No commit.

**Step 1 — Prior art + measurement.** Locate and read the P5
contract-pairing record. Measure: for each candidate v2 operator ×
judge × fixture, run the loop in-session, transcribe observed finding
classes. Derive the NIST names-only snapshot from the resolved jar's
`reference.conf`; disclose the derivation. No commit.

**Step 2 — The registry lands (AR-PD-1/2/3/4 tier one).** The EDN
registry with its measured rows; `ehrt.judge.pairing` (schema, loader,
interface); the taxonomy snapshot + currency gate; the tier-one
per-row test — all ONE commit (co-landed invariants law). Full suite
green (loopback flake: one independent re-run disambiguates,
disclosed, untouched); `gitleaks` clean; oracle bracket
`bin/regression-oracle 5168d3b <tip>` — all 29 roots IDENTICAL
expected (this session touches no sim/compile/engine path; any
non-identical root is a STOP-AND-REPORT). Commit:

    feat: the pairing registry lands — inject X, expect X, on the record (pairing-as-data, AR-PD-1/2/3/4)

Push; verify message; watch CI to conclusion.

**Step 3 — Record.** `notes/adr/0088-pairing-registry.md`: the P5
prior-art citation, the measurement tables (observed classes per row,
including any skipped pairs and why), the tier-two coverage table
(report-only), the taxonomy derivation, the licensing-inquiry note,
this session's own successor tag debt. Index line; README count 85→86.
Roadmap: the pairing-as-data horizon item's disposition per the live
gated precedent + Done pointer. Commit:

    docs: the pairing registry recorded — the carry ends at two closes, witnessed rows only (ADR-0088)

Push; verify; watch CI.

**Step 4 — Ceremony.** Session record + prompt archived verbatim
(`2026-08-08-pairing-registry.md`), both READMEs, same commit:

    docs: session record and prompt archive — pairing registry

## Fences

No FHIR rows, no new fixture authoring, no operator/judge src changes,
no tier-two gating, no mutation-operator catalog edits, no
sim/compile/engine/emitter touches, no state.md regeneration.

## Close-out

Session record: HEAD start/end, tag act, the measurement tables, the
skipped-pairs list, snapshot derivation evidence, bracket result (29
identical), suite shape, shas, CI conclusions. Echo to chat: rows
landed (count and list), skipped pairs, coverage table, bracket
result, shas, CI status.

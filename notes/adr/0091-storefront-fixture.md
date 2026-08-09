## ADR-0091 — The storefront opens: one clean fixture, a real flip, and every operator on the record

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

Prior: the pairing registry landed (`notes/adr/0088-pairing-registry.md`,
tip `948f5e5`, 2026-08-08) — seven witnessed rows across two v2 judges,
zero FHIR rows by design (AR-PD-2), and named this session's own
landing spot for the FHIR rows and the tier-two coverage-to-gate
promotion. Vendoring batch 4 closed the same day (`notes/adr/
0090-vendoring-batch-4.md`, tip `b7a1dc8`, 23:34 EDT). The driving
prompt for this session carries a 2026-08-08 header (authored the
same evening) but actual execution is 2026-08-09 local time — a
one-day authorship/execution gap, disclosed here rather than silently
absorbed into a stale filename; every artifact this session produces
(witness dates, this ADR's own Status line, the session record) is
dated 2026-08-09, the date the work actually happened.

The roadmap's own Next row named the target directly: "Storefront demo
fixture: minimal clean-gating FHIR fixture so the README's mutate demo
shows a real accepted→rejected flip (2026-08-01 capture session
finding). Also the named landing spot for the pairing registry's own
FHIR rows and the tier-two coverage-to-gate promotion." The README's
own honesty paragraph (pre-session) named the specific problem: the
mutate demo mutated `Patient.gender` with `remove-required-element`,
but base FHIR does not require `Patient.gender` (`Element.min` is 0) —
the demo's own rejection was earned by pre-existing profile noise in a
generated Synthea patient, not by the mutation shown.

Read first (per the driving prompt's own instruction): the README's
mutate demo section and its honesty paragraph (the standard this
session had to meet, then rewrite); `notes/adr/0088-pairing-registry.md`
(registry mechanics, the `:locator`/`:profile` row-shape additions,
measured-then-pinned discipline, the tier-one conviction test, the
tier-two coverage fn); `components/corpus/src/ehrt/corpus/operators.clj`
(the FHIR operator family and each `:contract`'s own `:target` prose);
`components/judge-fhir-official/src/ehrt/judge_fhir_official/fhir.clj`
(the verdict-mapping DATA, v2, cited to EXP-C5, and the finding
vocabulary — `:severity`/`:code` — rows draw from);
`components/judge/resources/judge/pairing-registry.edn` +
`ehrt.judge.pairing` (the append target and schema); `.agents/
rulings.md` (the conviction-arc laws: witnessed rows only,
measured-then-pinned, tier promotions only by dated ruling).

### Preflight finding, fixed forward before Step 0's own tag (not part
of this session's own design work)

The last-five-CI-runs check (build-session skill, standing) found one
red: the scheduled `Integration` workflow (`31301880957`,
2026-08-09T07:45), the first run to exercise `948f5e5`'s own new
`pairing_conviction_test.clj` (it requires
`ehrt.judge-v2-nist.interface` directly). `projects/integration/
deps.edn` had dropped `poly/judge-v2-nist` on 2026-07-31 on the
premise that nothing on that project's classpath required it — a
premise `948f5e5` broke silently, uncaught because `integration` is
scheduled/workflow_dispatch-only, not run per push. Fixed forward
(`2088763`, `poly/judge-v2-nist` and its `nist-hit` `:mvn/repos` entry
re-added, earning edge named), proven red (the actual CI failure log)
then green (local run with CI's own exact command, then a fresh
`workflow_dispatch` run on GitHub Actions itself, `31308023126`,
success). `stable-20260808-vendoring-batch-4` tagged at `b7a1dc8`
immediately after (AR-SD-0), oracle pre-digest `b7a1dc8 b7a1dc8`: 34
roots IDENTICAL.

### Decision

Author rulings, recorded verbatim (the driving prompt, 2026-08-08/09).
`[A]` author-ruled, `[C]` fence.

**AR-SD-0 `[A]`** (ADR-0090, "Successor tag debt"). Tag
`stable-20260808-vendoring-batch-4` at `b7a1dc8`, annotated, standing
ceremony. **Executed** (above).

**AR-SD-1 `[A]`** (the fixture). Author a MINIMAL, original,
project-authored FHIR fixture, committed under the current convention
(`components/corpus/test-fixtures/fhir/`, a new sibling of `v2/` and
`v2-nist/`), that gates `:accepted` from `judge-fhir-official` in a
REAL offline run. Design constraint: every FHIR operator must have a
locator in this fixture where its own `:target` contract genuinely
applies. **Executed** — see Fixture design, below.

**AR-SD-2 `[A]`** (the FHIR rows). Witness every FHIR operator against
`judge-fhir-official` on the storefront fixture — measure first, then
pin; `:expected` keeps the delta-vs-baseline semantics. An operator
that cannot be cleanly witnessed is skipped and named. The tier-one
conviction test must cover the new rows automatically, proven red
then green. **Executed** — all five operators witnessed cleanly, zero
skips (see Measurement, below).

**AR-SD-3 `[A`** — ratified by the author's paste of the driving
prompt`]`** (the promotion). The tier-two coverage check promotes from
report-only to a gating test: every operator in the catalog has at
least one witnessed registry row, any judge. If any operator would
fail coverage at landing, STOP-AND-REPORT. **Executed** — 10/10
operators covered, no STOP-AND-REPORT fired (see Coverage, below).

**AR-SD-4 `[A]`** (the README). Rewrite the mutate demo with a REAL
transcript from REAL runs against the committed fixture — `:accepted`
on the clean gate, `:rejected` after one mutation, both pasted as
executed. `bin/ehrt` invocation form throughout. The generated-patient
example may remain alongside as the realistic-corpus illustration.
**Executed** — see README before/after, below.

**AR-SD-5 `[C]`** (fences). No v2 rows; no retry of the three
NIST-skipped cells; no operator or judge SRC changes; no fixture
relocation; no sim/compile/engine path touches. **Held** — the one
judge-adjacent file this session touched twice
(`pairing_conviction_test.clj`) was a TEST-tree correction (a
hermeticity split, see Mid-session correction, below), never a change
to operator or judge behavior; oracle bracket confirms no sim/compile/
engine path moved.

**AR-SD-6 `[C]`**. One-line disclosure: batch 4's own commit messages
flattened em-dashes to plain characters (channel report, 2026-08-08);
this session adopts the same ASCII-only practice for its own commit
messages (four landed this session — the preflight fix, the storefront
feature commit, the hermeticity-split fix, and this record — all
ASCII-only, `--` in place of `—`).

### Fixture design (AR-SD-1)

`components/corpus/test-fixtures/fhir/storefront-patient.json`: a
minimal `Bundle` (type `collection`) holding one `Patient` entry, no
`meta.profile` declared anywhere — the specific mechanism EXP-C5 found
makes a generated Synthea file noisy (the official validator
auto-validates against any IG a resource's own `meta.profile`
declares, even with no `-ig` flag given). Provenance: project-authored
this session, not vendored bytes — no upstream hash, disclosed in this
file's own header comment.

First-attempt honesty, not smoothed over: the first draft (a bare
`entry.resource`, no `fullUrl`) gated `:rejected` at baseline —
genuinely, on two real base-spec rules this session had not
anticipated, not fixture-author error masked as success:
- `bdl-5`-adjacent: "Except for transactions and batches, each entry
  in a Bundle must have a fullUrl" (`:invalid`, `:error`).
- A `urn:uuid:` `fullUrl` must itself be a valid, lowercase UUID
  (rejected `urn:uuid:storefront-patient`, accepted
  `urn:uuid:8f14e45f-ceea-467e-adc2-4e28c39185cf`).

Both fixed, then the clean fixture gated for real:

```
bin/ehrt gate fhir components/corpus/test-fixtures/fhir/storefront-patient.json
→ :status :ok, :totals {:pass 1, :rejected 0, ...}, :by-code {"invariant" 1}
```

One finding survives: `dom-6` ("A resource should have narrative for
robust management"), `:severity :warning`, `:disposition :pass` — a
best-practice advisory, not a rejection. This is the ONE finding every
FHIR row's own `:expected` set had to stay distinguishable from (see
Measurement).

**Design finding, not preempted:** base `Patient` alone hosts every
FHIR operator's own contract genuinely — AR-SD-1's own Bundle+
Observation escape valve was never needed. Every locator below
reproduces exactly (operator, locator) pairs `contract_pairing_test.clj`
already established against a *noisy* Synthea baseline (that suite's
own EXP-C5-derived choices, e.g. `Patient.resourceType` over
`Patient.gender` for `remove-required-element`) — re-measured here
against a genuinely *clean* baseline, not assumed transferable.

### Measurement (AR-SD-2, disclosed in full)

All five FHIR operators, measured against the storefront fixture, one
real `bin/ehrt corpus mutate` + `bin/ehrt gate fhir` run each:

| operator | locator | contract this locator genuinely triggers | verdict | observed NEW `:code` (vs. baseline's sole `"invariant"`/`:warning`/`:pass`) |
|---|---|---|---|---|
| `:remove-required-element` | `entry[0].resource.resourceType` | every FHIR resource requires `resourceType` (`Element.min>=1`); `Patient.gender` (the old demo's locator) is NOT actually required, so this locator — not that one — is what makes the operator's own contract true | `:rejected` | `:fatal`/`"invalid"` ("Unable to find resourceType property") + `:error`/`"invariant"` (`bdl-5`, distinct occurrence from the baseline's `dom-6`) |
| `:duplicate-element` | `entry[0].resource.gender` | wraps a singular (max-cardinality-1) value in a JSON array, violating the FHIR JSON singular-representation rule | `:rejected` | `:error`/`"invalid"` ("must be a simple value, not an Array") |
| `:invalid-code-value` | `entry[0].resource.gender` | `Patient.gender` is bound to `AdministrativeGender`, a small ValueSet bundled with base FHIR — checkable fully offline (EXP-C5's own finding, contrary to the a-priori terminology-suppression hypothesis) | `:rejected` | `:error`/`"not-found"` + `:error`/`"code-invalid"` |
| `:malformed-date` | `entry[0].resource.birthDate` | `birthDate` is date-typed; a lexically invalid date string fails the base FHIR date regex | `:rejected` | `:error`/`"invalid"` ("Not a valid date format") |
| `:wrong-type-value` | `entry[0].resource.multipleBirthBoolean` | `multipleBirth[x]` is boolean-typed in this choice; a wrong-JSON-type value fails FHIR's own type constraint | `:rejected` | `:error`/`"invalid"` (twice: a JSON-parse-level type error, and a `.ofType(boolean)` type error) |

Zero skips. `:expected` sets pinned to the `:code` values above (the
`hl7-hapi`/`hl7-nist` rows' own precedent — bare `:code`, not the
`{severity, code}` pair) EXCEPT deliberately excluding `"invariant"`
from `:remove-required-element`'s own `:expected` — that code recurs
in the baseline too (a different occurrence, `dom-6` vs. `bdl-5`, but
the same bare string), so `"invalid"` alone is the unambiguous witness;
`some` only needs one match.

### Registry rows landed (AR-SD-2)

Five rows, `components/judge/resources/judge/pairing-registry.edn`,
`:judge :judge-fhir-official`, `:witness {:adr "0091" :date
"2026-08-09"}` — alongside the seven existing v2 rows (unchanged).

### Coverage at promotion (AR-SD-3)

`ehrt.judge.pairing/coverage` against the live catalog (10 operators:
5 v2, 5 FHIR — `ehrt.corpus.interface/operator-entries`, filtered to
entries carrying `:doc`, the same signal `ehrt.corpus.operators-test`'s
own comment already uses to set aside that suite's throwaway registry
entries in the same shared, global, mutable atom):

| operator | `:judge-v2-hapi` | `:judge-v2-nist` | `:judge-fhir-official` |
|---|---|---|---|
| `:blank-required-field` | witnessed | witnessed | n/a (v2 operator) |
| `:corrupt-encoding-characters` | witnessed | not witnessed (ADR-0088 skip) | n/a |
| `:corrupt-segment-name` | witnessed | not witnessed (ADR-0088 skip) | n/a |
| `:malformed-datetime-value` | witnessed | not witnessed (ADR-0088 skip) | n/a |
| `:truncate-segment-fields` | witnessed | witnessed | n/a |
| `:remove-required-element` | n/a (FHIR operator) | n/a | witnessed |
| `:duplicate-element` | n/a | n/a | witnessed |
| `:invalid-code-value` | n/a | n/a | witnessed |
| `:malformed-date` | n/a | n/a | witnessed |
| `:wrong-type-value` | n/a | n/a | witnessed |

10/10 operators covered by at least one witnessed row. AR-SD-3's
STOP-AND-REPORT condition never fired; the promotion landed as a live
gate, `every-catalog-operator-has-at-least-one-witnessed-row-test`
(`components/judge/test/ehrt/judge/pairing_conviction_test.clj`).

### Mid-session correction (disclosed, not smoothed over)

The first landing attempt (`cd08b20`) added a `:judge-fhir-official`
arm directly to `pairing_conviction_test.clj`, in `judge`'s own test
tree. That tree is composed by EVERY project, including `conformance`
and `ehrt-cli`, whose ordinary push-triggered CI lane never primes the
artifact cache (AGENTS.md's hermetic-test-suite rule — only
`integration`'s own scheduled/`workflow_dispatch` lane runs `ehr
artifact fetch`). It passed locally (this session's own artifact
cache was already warm from the manual measurement runs above) and
failed in CI's fresh environment: `:not-cached`, `fhir-validator-cli`
(run `31311258218`).

Fixed forward, same session, before Step 2 was considered landed
(`c690ec3`): `pairing_conviction_test.clj` witnesses only the
artifact-independent judges again (`:judge-v2-hapi`/`:judge-v2-nist`),
explicitly excluding `:judge-fhir-official` rows by name in its own
docstring and code. A new file,
`projects/integration/test/ehrt/integration/pairing_conviction_fhir_
test.clj`, witnesses the FHIR rows instead — the same placement
`contract_pairing_test.clj`/`baseline_gating_test.clj` already use for
the identical reason. Tier-two coverage stayed in `judge`'s own test
tree unmoved (it only reads the registry and the live catalog, never
gates a mutant, so it was never artifact-dependent).

Verified hermetic FOR REAL, not just by construction: `poly test :all
skip:integration` run green with `~/.cache/ehr-testing-tools/artifacts`
renamed out of the way entirely (a genuinely cold cache), restored
after. `poly test :all project:integration` green with the cache
back. Both confirmed again in real CI: the ordinary push lane
(`31312272026`) green, and a fresh `workflow_dispatch` of `Integration`
(`31312458033`) green, including the new FHIR conviction file.

### README before/after (AR-SD-4)

**Before:** `remove-required-element` at `Patient.gender` against a
generated Synthea patient — rejected, but (the README's own honest
disclosure) not because of the mutation shown; 2560 pre-existing
findings the aggregate verdict couldn't discriminate from. The pasted
example output (`totals:`/`by-code:` prose lines) additionally turned
out to be stale — no code anywhere in the repo produces that format;
the CLI's real default output is the raw `{:status ...}` map.

**After:** the clean storefront fixture gates `:accepted` (one
`:pass`-disposition advisory finding, pasted verbatim); the SAME
fixture, mutated with `remove-required-element` at
`entry[0].resource.resourceType`, gates `:rejected` (two genuinely new
findings, pasted verbatim, both real base-spec rules the mutation
alone triggers). The old, honest-but-unearned example is gone; the
generated-patient Quickstart demo (already honestly framed —
"gate fhir exits 1 here -- rejected, but not because of this
mutation") stays as the realistic-corpus illustration, per AR-SD-4's
own text.

### Fixture-relocation backlog, one more member

The roadmap's own unruled "Fixture relocation" row (2026-08-08,
fidelity riders, ADR-0081) names `v2/simhospital` and `v2-nist` as
candidates for a future top-level move. `fhir/` (this session's own
new sibling) is now a third — noted here, not preempted; the relocation
session itself remains unscheduled.

### Verification

- `clojure -M:poly check`: OK, every checkpoint.
- Full suite (`clojure -M:poly test :all skip:integration` and `clojure
  -M:poly test :all project:integration`): green throughout, including
  a genuinely cold-artifact-cache run of the former (Mid-session
  correction, above).
- Red→green witnessed directly, in-session, for both new gates:
  - Tier-one FHIR conviction: corrupted `:duplicate-element`'s own
    `:expected` set to a nonsense code — failed naming the real
    observed classes (`#{"invalid" "invariant"}`); restored
    byte-identical (diffed against a pre-edit backup), re-ran green.
  - Tier-two coverage: proven satisfiable only after the FHIR rows
    landed (an intermediate state with zero FHIR rows would have left
    five operators uncovered — not separately re-corrupted to
    demonstrate, since the coverage table above already shows the
    would-be-red state's own shape directly).
- `gitleaks`: clean at every push, after two false-positive triggers
  (a `generic-api-key` heuristic tripped by three adjacent short
  judge-id keywords on one line, `JudgeId`'s own enum and its test
  twin) resolved by reformatting those two lines to one keyword
  per line (pure whitespace, zero semantic change) before ever
  reaching the remote.
- Post-push message verification: every push, one delta each time (the
  known harmless trailing-blank-line artifact).
- Oracle bracket: `bin/regression-oracle b7a1dc8 c690ec3` — 34 roots
  IDENTICAL (this session touches no sim/compile/engine path, as
  AR-SD-5 required).
- CI: last-five on `main` at session start disclosed (Preflight
  finding, above — one red, diagnosed and fixed forward before Step 0's
  tag); every subsequent push watched to conclusion (`31307887307`
  success; `31311258218` failure, diagnosed above; `c690ec3`'s own
  `31312272026` success; `Integration` `workflow_dispatch` runs
  `31308023126` and `31312458033`, both success).

### Successor tag debt, recorded here

The next session that opens fresh work tags
`stable-20260809-storefront-fixture` at THIS session's own closing
tip — the same tag-law case (ii) pattern every prior close in this
repo has used for its own predecessor.

### Index line

```
- 2026-08-09 — storefront-fixture — ADR-0091
```

(appended to `.agents/plans/roadmap.md`'s own Done section; the
"Storefront demo fixture" Next row removed.)

`notes/adr/README.md`'s own file count corrects 88→89, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

FHIR joins v2 in the pairing registry: twelve witnessed rows across
three judges, tier-two coverage now a live gate over the full
ten-operator catalog. Untouched, carried forward: the carry-across
emission row's own compile-layer half, Wave E's risk-attribute/
vital-sign register, the census closure-count refinement, publish-prep
(F-5/F-6 + F-7), review 2, `sim-emit-cda`, the fixture-relocation row
(now three members), the sleep-apnea latent-defect intake named for
review 2, and the three NIST-skipped cells (ADR-0088, untouched by
AR-SD-5's own fence).

### Consequence

The README's mutate demo no longer asks a reader to trust that a
rejection means what it says — the fixture that earns it is now
committed, byte-identical to what `bin/ehrt` actually runs, and the
same fixture backs five registry rows checked by execution, not
prose. Tier-two coverage graduates from a table a human had to read to
a test that fails the build the day an operator loses its last
witness. The one genuine defect this session's own first landing
attempt produced — a hermeticity violation invisible locally, visible
only in CI's cold environment — is recorded here rather than smoothed
into "landed clean," the same disclosure discipline this repo's own
transcript-witnessed-is-not-repo-recorded law (ADR-0048) already
requires.

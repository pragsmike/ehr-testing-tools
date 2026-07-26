# DOC-3 — Reference docs: operators + cli (generated), locators + formats (hand-written)

You are working in `ehr-testing-tools` (public). This session executes
DOC-3 of `.agents/plans/user-docs.md`: four reference documents land
in `docs/`, closing the audit's remaining register gaps — the operator
catalog, the CLI reference, the locator grammars, and the output
formats. The author has ratified the plan's recorded recommendations
(record the ratification at close-out): `docs/operators.md` and
`docs/cli.md` are **generated** — from the operator registry and
DOC-1's `cli-spec` respectively, using the same
renderer-plus-freshness-gate pattern as `pipeline.md`/`use-cases.md` —
while `docs/locators.md` and `docs/formats.md` are **hand-written**
with source citations. The author has also chosen to land this now,
pre-release, accepting the soft interface-hardening pressure a full
CLI reference creates: both generated docs carry a one-line
pre-release notice consistent with `README.md`'s maturity language.
This session has two independent phases — A (generated docs + gate
wiring, Steps 0–3) and B (hand-written docs, Steps 4–5). If Phase A's
freshness wiring resists clean extension, stop that phase at the last
green commit, report at close-out, and do Phase B anyway; B depends on
nothing in A.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md` (§6 applies with full
force — these four docs are the agent-read surface), `.agents/plans/
user-docs.md` (DOC-3 section: the per-document decisions and their
reasoning), `src/ehr_testing_tools/cli/help.clj` (whole file — the
`cli-spec` the cli.md renderer consumes), `src/ehr_testing_tools/
corpus/operators.clj` (registry entries + `register!`'s schema — the
schema will grow an optional key), `src/ehr_testing_tools/locator.clj`
and `src/ehr_testing_tools/corpus/er7.clj` (the grammars locators.md
re-expresses — write from what `parse` accepts, not from memory),
`src/ehr_testing_tools/judge/report.clj`, `judge/finding.clj`,
`corpus/manifest.clj`, `lineage.clj` (the Malli schemas formats.md
cites), `notes/ADRs.md` ADR-0009 and ADR-0010 (formats.md cites,
never restates), `Makefile` targets `pipeline` and `use-cases` (the
renderer pattern to copy: `clojure -X` entrypoint → generated file →
`@echo`), `.github/workflows/ci.yml`'s generated-doc freshness step
(the gate the new docs join), `docs/judge-calibration.md` (operators.md
links its blind-spot/dropped-candidates material instead of restating
it), `docs/README.md` and `docs/positioning.md` §Audience segment 5
(both name the formats gap with a plan pointer — Step 6 resolves those
sentences), `docs/pipeline.md`'s header block (the house
"generated file — do not edit" banner to reuse). Ritual: commit →
`git push origin`. Save this prompt to
`.agents/prompts/2026-07-25-doc3-reference-docs.md`; final commit
archives it to `.agents/prompts/archive/`.

Author rulings in effect: **Generated docs are wholly generated** —
`operators.md` and `cli.md` come entirely from their renderers
(preamble text lives as literals in the renderer, like
`write-pipeline-md!`'s); no hand-edited region in a generated file,
ever. **Worked examples are DOC-4's job, not cli.md's** — cli.md
carries usage trees, flags, positional conventions, the exit-code
table, and pointers to `docs/use-cases.md` for "what do I type for my
task"; it does not carry invocation walkthroughs that could rot.
**The registry gains `:doc`** — one sentence per operator, an
optional key in `register!`'s schema, prose distilled from the
namespace docstring's own descriptions; additive metadata, no
behavior change (the DOC-1 contract tripwire applies: any existing
test needing edits means stop and report). **Locator examples are
machine-verified** — every example locator in locators.md is run
through the actual parse functions (a throwaway test or REPL check is
fine; a committed test pinning the doc's examples is better and is
the recommendation) before it lands in prose; no unverified example.
**Format shapes are evidence-backed** — formats.md's field tables
come from the Malli schemas plus at least one real captured output
(run `ehr gate v2` and `ehr check` against fixtures with `--report`
and `--json`; the FHIR gate needs fetched artifacts — if a live FHIR
run is impractical in this session, derive that report's shape from
schema + existing tests and say so in the doc's citation line rather
than pretending a capture). **The golden check grows** — the
canonical incantation gains the two new generated files; every place
the incantation is stated in the repo (grep for it — plans, AGENTS,
Makefile comments, CI) is updated in one dedicated commit, and from
that commit on, this session runs the extended form. **Pre-release
notice** — one line in both generated docs' preambles: interfaces may
move until first release. **No new namespaces beyond renderers** —
renderer entrypoints live beside the existing ones
(`ehr-testing-tools.pipeline` / `.usecases` pattern; a
`docsgen`-style sibling namespace is fine, implementer's discretion).

## Phase A — generated docs

### Step 0 — Evidence

Three verifications recorded in the commit body: (1) `cli-spec`
richness against the pre-registered criterion — for every group/verb:
flags with one-line docs, positional conventions, defaults where they
exist, plus the shared exit-code table; DOC-1's close-out says yes,
verify anyway; if it falls short, list exactly what's missing and add
it to the spec (spec-only change, help output may grow but not
change meaning). (2) Registry entry keys as they exist (`:doc` is
expected absent — confirm). (3) Every statement of the golden-check
incantation in the repo, by grep, listed with paths — Step 3's
worklist. Also capture, into `target/` scratch (not committed), one
real `ehr gate v2 --report --json` output and one `ehr check
--report` output against fixtures — Step 5's raw evidence.

Commit: `DOC-3: evidence — cli-spec richness, registry keys, golden-check statement sites`.

### Step 1 — Registry `:doc`

Optional `:doc` (one sentence, user register: what the operator does
to the input, not how it's implemented) added to `register!`'s schema
and to all ten seed-catalog entries, distilled from the namespace
docstring. `ehr corpus operators` output is unchanged this commit
(the listing verb's row selection stays as-is; whether it later
surfaces `:doc` is DOC-4-adjacent polish, not this session). Full
suite green untouched.

Commit: `DOC-3: registry entries gain :doc (additive; schema + ten sentences)`.

### Step 2 — Renderers and the two documents

`make operators-doc` → `docs/operators.md`: generated banner
(pipeline.md's house form), pre-release line, one table or section
per format (fhir, v2), per operator: id, version, doc sentence,
locator-required?, contract type/target; a closing pointer to
`docs/judge-calibration.md` for measured blind spots and the dropped
v2 candidates (link, don't restate), and to `docs/locators.md` for
how to write the locators the `locator-required?` column demands.
`make cli-doc` → `docs/cli.md`: generated banner, pre-release line,
rendered from `cli-spec` — program synopsis, exit-code table,
per-group sections with verbs/flags/positional conventions, global
flags, and pointers: `ehr help <group>` for the same content at the
shell, `docs/use-cases.md` for task walkthroughs, `docs/formats.md`
for what `--report`/`--json` emit. The renderers share helpers where
natural but stay two make targets. Both generated files committed
with their renderers; running either target twice must be idempotent
(diff-clean on the second run — check before committing).

Commit: `DOC-3: operators-doc + cli-doc renderers; docs/operators.md, docs/cli.md generated`.

### Step 3 — Freshness gate extension (isolated; the session's riskiest commit)

CI's generated-doc freshness step gains the two targets and the two
files; the golden-check incantation everywhere it is stated (Step 0's
list) becomes `make pipeline && make use-cases && make operators-doc
&& make cli-doc && git diff --exit-code docs/pipeline.md
docs/use-cases.md docs/operators.md docs/cli.md`; `make help` and the
Makefile comment block gain the new targets in the house comment
style. Run the extended golden check locally — clean — before
committing. Tripwire: if extension requires touching anything beyond
ci.yml, the Makefile, and the incantation-statement sites, the
mechanism is telling you something — stop Phase A at Step 2's commit,
revert nothing, report the resistance at close-out, proceed to
Phase B.

Commit: `DOC-3: freshness gate covers operators.md + cli.md; golden check extended everywhere it is stated`.

## Phase B — hand-written docs

### Step 4 — docs/locators.md

Hand-written, user register, two sections. FHIR: the path grammar
exactly as `locator.clj`'s parse accepts it (segment syntax, index
syntax, what a path is resolved against — the bundle-entry framing),
with examples spanning what the fhir operators need. v2: the ER7
grammar (`SEG`, field, component, subcomponent forms), the MSH
off-by-one convention stated plainly and *why* (the field-separator
convention, one sentence), examples spanning the v2 operators'
locator needs. Every example machine-verified per the ruling — the
recommended committed test (`locators_doc_test` or similar) pins each
documented example to a successful parse, so the doc's examples join
the suite and can't rot silently. Citation lines point at
`locator.clj`/`er7.clj` as authoritative. Cross-links:
`docs/operators.md` (which operators need locators),
`docs/cli.md`/`ehr help corpus` (where `--locator-path` goes).

Commit: `DOC-3: docs/locators.md — FHIR + v2 locator grammars, examples pinned by test`.

### Step 5 — docs/formats.md

Hand-written, audience-5 register (a Python or SQL reader who never
runs the CLI). Covers, in this order: the judgment/report shape
(verdict arms including `:no-verdict` with `:cause`, findings with
`:disposition` — cite ADR-0009/0010 for the why, one sentence each,
never restate the reasoning), the check report, the corpus manifest,
the lineage record, and the `--json` projection (how EDN
keywords/keyword-values render in JSON — state the actual mapping
from the captured Step 0 output, not an assumed one). Field tables
cite their Malli schema by namespace/var. One honest paragraph on
reading these from Python (`--json` + `json.load` is the supported
path; EDN needs a third-party parser). The FHIR gate report's
citation line follows the ruling: captured if a live run was
practical, schema-and-tests-derived and labeled as such if not.
Cross-links: `docs/cli.md` (`--report`/`--json` flags),
`docs/judge-calibration.md` (how to read verdicts in bulk).

Commit: `DOC-3: docs/formats.md — report/manifest/lineage/judgment shapes + --json projection`.

## Step 6 — Close out

Resolve the gap-namings the new docs satisfy: `docs/positioning.md`
§Audience segment 5's "named gap" sentence becomes a pointer to
`docs/formats.md` (keep the segment's honesty — the gap sentence
turns into its resolution, not silent deletion); `docs/README.md`'s
downstream-consumer path lands on formats.md, and the practitioner
path gains operators.md/locators.md/cli.md at the natural steps.
Link-check every relative link in all touched docs. Plan updates in
`.agents/plans/user-docs.md`: DOC-3 tracker row → Done (or Done-with-
Phase-A-stopped, per what happened) with itemized summary and prompt
path; the Open decisions section records both ratifications
(generated/hand-written split as recommended; sequencing: now,
pre-release, hardening pressure accepted) as decided 2026-07-25.
Extended golden check clean; full suite + both lints green; the
locator-examples test is in the suite count. Archive this prompt.

Commit: `DOC-3 complete: four reference docs landed, freshness-gated (archives prompt)`.

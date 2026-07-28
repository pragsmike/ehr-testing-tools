# P4 — mutation capability + EXP-B2 + interpretation ledger

Task: Execute P4 — mutation capability + EXP-B2 + interpretation ledger
You are working in `ehr-testing-tools`. This session lands the second
capability (controlled mutation with lineage), executes EXP-B2 as its
foundation, trials the resource-equation notation, and applies the
design rulings from EXP-A4's interpretation.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md`,
`.agents/memory/patterns.md`, `docs/experiments/EXP-A4.md` and
`EXP-A4-results.md`, `docs/experiments.md`, `docs/components.md`,
`.agents/plans/corpus-foundations.md`, `artifacts.lock.edn`. Commits
from WSL. Test-first throughout per ADR-0006 (red→green evidence in
the report; property tests for law-bearing constructs). Save this
prompt to `.agents/prompts/2026-07-24-p4-mutation-exp-b2.md` (live);
final commit archives it.

Author rulings in effect this session: JVM becomes a lockfile
artifact; manifest upgrades to v1.1 (`:stage`, `:seeds` map,
`:engine-params`); equations are authored as EDN data; FHIR mutation
proceeds now, v2 mutation defers until v2 generation exists (post-EXP-A3).

## Step 1 — Interpretation ledger from EXP-A4

1. Pattern nursery updates (`.agents/memory/patterns.md`):
   * Promote to validated, each with a one-line evidence citation: #1
     two-step engines (driver hash bug fixed by recomputation over
     preserved outputs, zero regeneration; clinician-seed diagnosed
     from preserved metadata), #3 canonicalizers (two real
     registrations; law harness caught the cross-namespace registry
     pollution), #11 result vocabulary (ubiquitous use).
   * Add #13 — stages as resource equations: pipeline stages written
     as `inputs → outputs [Stage] {catalytic: …}`; five stage kinds
     with laws (transform appends provenance; normalize is an
     idempotent endomorphism = registered canonicalizer; enrich adds
     without altering; gate splits pass/rejected/indeterminate and
     never modifies; feedback carries a round bound); stage-specific
     laws allowed beyond kind laws; catalytic resources must resolve
     to one of three targets — `artifacts.lock.edn`, `deps.edn`, or
     hashed repo-authored config. Status: on trial this session;
     promotion criteria pre-committed (see Step 2).
   * Add #14 — operators carry contracts: mutation and metamorphic
     perturbation are one operation; each operator declares a
     contract (`:violates <constraint>` for defect operators,
     `:preserves <relation>` for metamorphic ones). Status: tentative;
     the `:contract` field ships in the catalog schema this session.
   * Add #15 — provenance is measured at the point of execution:
     environment fields are forced into subprocesses and recorded as
     forced, never sampled from the orchestrator. Evidence: three bugs
     of this species found and fixed in P3 (JVM version, locale,
     timezone). Status: validated (three instances).
2. Engine-onboarding checklist: create `docs/engine-onboarding.md` —
   the short list every future engine wrapper must answer before being
   trusted: all entropy sources enumerated (how many RNG streams? what
   do seeds default to?); environment forced-and-recorded (#15);
   native output preserved verbatim (#1); every external input
   resolved via the three lockfile targets (#13); license row in the
   facts register; components.md section. Cite EXP-A4's clinician-seed
   finding as the motivating case.
3. Facts register F-row: Synthea CLI semantics as verified in P3 —
   `-p` is population size; parallelism is
   `generate.thread_pool_size` (verified no effect on determinism at 4
   threads); `-cs clinicianSeed` is an independent RNG seed defaulting
   to `System.currentTimeMillis()`. Evidence: jar `--help`, extracted
   synthea.properties, EXP-A4 results. Add one sentence to
   components.md's Synthea section noting the two-seed design.
4. JVM as artifact: add the Temurin JDK 17 build already in use
   (`~/.local/jdk/jdk-17.0.19+10`) to `artifacts.lock.edn` as
   `kind :runtime` — real source URL (Adoptium API-resolved asset),
   sha256 of the archive (re-download to compute if the original
   archive wasn't kept), license-status verified (Temurin is
   GPLv2+Classpath-exception — verify and record accurately, with an
   F-row). Teach `corpus.generate` to resolve the JVM through the
   registry (test-first): the java executable path comes from the
   resolved artifact, not from PATH or a hardcoded home path; manifest
   references it by {name, version, sha256} like the generator.
5. Manifest v1.1 (test-first): `:schema-version 2` is wrong — this is
   v1.1, use `:schema-version "1.1"` or bump to 2 consistently (pick
   one, document in the schema docstring, report the choice). Shape:
   top level gains `:stage` (keyword, `:generate` for this
   capability), `:seeds` (map — `{:master 100 :clinician 555}` style),
   `:engine-params` (map for engine-specific parameters:
   `:reference-date` moves here), `:runtime` ({name, version, sha256}
   of the JVM artifact); engine-shaped fields leave the top level.
   Update `corpus.generate`, all tests, and regenerate nothing — note
   in the schema docstring that pre-v1.1 manifests remain valid
   historical records (schema versioning, not migration).
6. CLI test backfill: unit-cover the P3 gap — `-main` exit-code
   mapping and the real command paths via function invocation with
   injected stubs. Target: cli.clj no longer the coverage floor;
   report before/after numbers.

Commits: split sensibly (nursery+checklist+F-row; JVM artifact;
manifest v1.1; CLI backfill).

## Step 2 — Notation trial (pattern #13's test)

1. `docs/notation.md`: the repo-local semantics — equation form;
   resource names bind to types (Malli schema for data,
   template/rubric for prose); catalytic defined (participates
   unconsumed ⇒ must resolve to one of the three lockfile targets);
   the five stage kinds and their one-line laws; stage-specific laws;
   one line on resource contracts (a resource may carry a
   precondition — e.g. a parser's round-trip fidelity — verified by
   experiment and cited by the equation). Cite the string-diagram
   skill for general notation; define only what's local.
2. `docs/pipeline.edn`: the equations as data (author-time source of
   truth). Contents: Generate (as built — catalytic: synthea artifact,
   jdk runtime, config-hash), Normalize (the canonicalizer
   application), Mutate (as designed this session), and Gate/Report as
   planned-status stubs marked `:status :planned`. Schema for the
   equation data itself (Malli, test-first — the notation eats the
   repo's own dogfood).
3. `docs/pipeline.md`: generated from pipeline.edn via the
   string-diagram skill (mermaid), catalytic inputs drawn distinctly,
   with a header stating it is generated — do not hand-edit. Add a
   `make pipeline` target that regenerates it; CI-wave lint noted in
   the plan file (tier 1: every catalytic resource resolves).
4. Trial evidence duty: pattern #13's pre-committed criteria are in
   the design record; your report must state, for criterion (1): what
   (if anything) writing the Mutate equation surfaced before
   implementation answered it. Honesty over vindication — "nothing
   surfaced" is a reportable result.

Commit: `Notation: notation.md, pipeline.edn source, generated diagram`.

## Step 3 — Execute EXP-B2 (round-trip fidelity)

1. Protocol first: `docs/experiments/EXP-B2.md`, same shape as A4.
   Objective: characterize parse→no-op→serialize fidelity for the
   representations mutation will use: (a) FHIR JSON via HAPI FHIR's
   parser, (b) FHIR JSON via plain Clojure data (data.json read/write),
   (c) HL7 v2 ER7 via HAPI HL7v2 PipeParser (known suspect:
   trailing-delimiter canonicalization), on real inputs: FHIR files
   sampled from the P3 corpus (regenerate a small population if `out/`
   is gone — the manifest makes this cheap); v2 via 3–5 hand-authored
   ADT fixture messages committed under `test/fixtures/v2/` (author
   them carefully: MSH/EVN/PID/PV1, realistic field population, v2.4).
2. Add HAPI deps to `deps.edn`, exact-pinned, current stable (verify):
   HAPI FHIR base + R4 structures; HAPI HL7v2 base + v2.4 structures
   (hapi-fhir and hapi-hl7v2 are distinct projects — coordinates differ;
   verify on Maven Central).
3. Execute: for each representation × input set, byte-diff original
   vs re-serialized; classify every difference (none / whitespace-
   canonical / key-reordering / content-normalizing / lossy), with
   examples. Results file per template, rubric self-scored,
   experiments.md row updated.
4. Pre-authorized decision rule (from the design channel, so you apply
   rather than decide): mutation operates on the representation whose
   round-trip is faithful (or faithful-modulo-registered-
   canonicalizer). Expectation: plain-data JSON for FHIR; if HAPI FHIR
   round-trips faithfully too, still prefer plain data (fewer moving
   parts) and record HAPI as the parse-validation aid only. For v2:
   record findings; v2 mutation is out of scope this session.

Commits: `EXP-B2 protocol + fixtures`, `EXP-B2 executed: results`.

## Step 4 — Mutation capability (test-first, FHIR only)

Informed by EXP-B2's applied rule:

1. Operator catalog — `ehr-testing-tools.corpus.operators`: operators
   as data: `{:id, :version, :format, :contract {:type
   :violates|:preserves, :target <constraint-or-relation>},
   :locator-required?, :fn}`; registry like canonicalizers; Malli
   schema; seed catalog of 4–6 FHIR defect operators spanning the
   defect taxonomy (remove required element; cardinality violation via
   duplication; invalid code value; malformed date; wrong-type value),
   each with `:contract {:type :violates :target …}` naming the FHIR
   base-spec constraint it breaks (cite the element/constraint
   precisely enough that a future gate test can pair with it).
2. Locator, made real for FHIR: extend `ehr-testing-tools.locator`
   with the FHIR grammar mutation needs — a data-path form (vector of
   keys/indices into the parsed JSON) with validation; document that
   full FHIRPath is future work and this is the operational subset.
   v2 grammar stays a stub.
3. Lineage — `ehr-testing-tools.lineage`: append-only records `{:id
   (content-hash of the record), :parent (content hash of the base
   datum), :stage :mutate, :transformation {:operator {id, version},
   :locator …, :contract …}, :produced (content hash of the mutant)}`;
   Malli schema; property test: records are content-addressed (hash
   recomputation matches) and reference parents by hash.
   Corrections-are-new-records documented in the docstring.
4. `corpus.mutate` — pure core: `(mutate base-data operator locator)`
   → result-vocabulary map containing mutant + lineage record; the
   Mutate stage law as a test: for every seed operator,
   `diff(canon(base), canon(mutant))` touches exactly the declared
   locator/contract target and nothing else (this is the
   intended-diff-only invariant, property-tested across sampled
   corpus files).
5. CLI: `ehr corpus mutate` — takes an input file/dir, operator id,
   locator, output dir; writes mutant + lineage record (EDN sidecar or
   a `lineage/` subdir — pick, document); exit codes per contract;
   unit tests per the Step 1 backfill standard.
6. First mutation corpus: run the seed operators against a small
   regenerated population (pinned config from EXP-A4's final
   settings); record the run in the report (counts, lineage sample,
   canonicalized hashes). Not committed; lineage records and hashes in
   the report are the evidence.

Commits: operators+locator; lineage; mutate core+law; CLI+first run.

## Step 5 — Finalize

1. `make test` green; `make coverage` with headline numbers and the
   cli.clj before/after; justify any regression per ADR-0006.
2. Plan file: P4 status updated; v2-mutation deferral noted with its
   dependency (EXP-A3); enforcement wave gains the pipeline lint
   (tier 1) and `make pipeline` freshness check.
3. Archive this prompt; `make pack-push`; verify `updated_at`.
4. Report: ledger items landed (with the manifest version choice),
   notation trial evidence for criterion (1) (stated honestly),
   EXP-B2 classification table, the applied decision rule's outcome,
   operator catalog as committed (ids + contracts), lineage sample,
   Mutate-law property test description, red→green evidence
   throughout, coverage, commits.

## Out of scope

No v2 mutation (deferred to post-EXP-A3), no gate code, no
metamorphic comparator (strategy schema noted in nursery only), no
CI/hooks (wave), no guide-repo edits, no edits to ADR-0001..0006, no
interpretation of EXP-B2 beyond the pre-authorized rule.

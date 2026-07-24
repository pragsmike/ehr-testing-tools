# Pattern Nursery

Candidate design patterns observed in design discussion, before they're
proven or ratified. Sessions are executed by agents that cannot see each
other — this file exists so a pattern noticed in one session is visible
to the next one, instead of being silently reinvented (or silently
contradicted) in a different shape.

**Status ladder:** `tentative` (named, not yet built) → `validated`
(a second real occurrence confirmed the shape holds) → `ratified` (an
ADR made it load-bearing). Promote a pattern by writing the ADR, not by
changing its status label here and moving on — the ADR is what makes it
binding. Do not silently implement a variant of a pattern listed here;
if the shape needs to change, say so where you use it and flag it for
the design channel.

1. **Two-step engines (execute/interpret)** — every engine run splits
   into: mechanical execution preserving native output verbatim, and a
   pure, versioned interpreter from native output to canonical data.
   Payoff: re-interpretation without re-execution.
   Status: validated. Evidence: P3's clinician-seed bug was diagnosed
   from preserved run metadata with zero regeneration, and the
   driver-hash fix (EXP-A4) was applied by recomputing over the
   already-preserved native outputs, again with zero regeneration.

2. **Invocation record** — one schema for "a subprocess ran": command,
   args, relevant env, artifact refs by hash, engine version, duration,
   exit code, output digests. Engine-agnostic; engine-specific fields
   are a smell.
   Status: in implementation (P3); the in-process case is now handled
   too (P5) — `gate.v2` has no subprocess (HAPI HL7v2 runs in-process),
   yet still returns an invocation-shaped record (`{:engine {:name
   :version} :input-sha256 ...}`, engine version read from the running
   jar's own packaged pom.properties on the classpath, not hand-copied)
   rather than omitting provenance because there's no PID to record.

3. **Canonicalizers (registry + laws)** — named, versioned
   transformations with three laws: idempotence (property-tested),
   explicit composition (verified confluence or registry-imposed total
   order, recorded in manifests), endomorphism (never cross-format).
   Canonical forms are fixed points; `c@v2` need not agree with `c@v1`.
   Status: validated. Evidence: two real registrations
   (`:strip-run-timestamp-suffix`, `:strip-synthea-run-metadata`, both
   EXP-A4); the idempotence-law harness caught real cross-namespace
   registry pollution during P3 (a test-isolation bug in
   `canonical_test.clj` wiping entries other namespaces registered at
   load time), fixed by the save/restore-snapshot fixture now in place.

4. **Locator** — one type for "a place in a datum," per-format grammar
   (FHIRPath; v2 segment/field/component; table.column; XPath later).
   Shared by mutation records (injection site), findings (violation
   site), canonicalizers (scope). Mandatory column of format support.
   Status: schema stub in P3; first real use P4.

5. **Lineage (immutable, hash-linked)** — every derived datum carries a
   parent reference by content hash plus a transformation record;
   records are append-only, corrections are new records. Merkle-style
   self-verifying chain; forensic/regulatory-grade provenance.
   Status: tentative; first use P4.

6. **Finding envelope, ternary verdicts** — one canonical finding shape
   `{severity, code, locator, message, engine, native-ref}`; verdicts
   are pass / rejected / indeterminate, with indeterminate first-class
   (license-blocked checks, engine limitations).
   Status: validated (P5). Evidence: two real, independent formats
   (`gate.fhir`, `gate.v2`) both consume the exact same
   `ehr-testing-tools.gate.finding` schema and `worst-of` composition
   law, with no per-format dialect -- `gate.fhir` additionally records
   a `:policy` key beyond the envelope's own fields (which format
   classified as which severity), proving the shape is extensible
   without forking it. FHIR's own `IssueSeverity` ValueSet turned out
   to have a fourth value (`:fatal`, alongside `:error`/`:warning`/
   `:information`) that neither format's initial test corpus surfaced
   -- found live, during P5's own contract-pairing exercise, and folded
   into the shared schema rather than left as a gate.fhir-only special
   case.

7. **Corpus recipes** — one EDN value naming a corpus intent (generator
   config, population, seed policy, mutation plan), referenced by hash
   from manifests.
   Status: tentative.

8. **Corpus catalog** — an index of corpus items (id, layer, format,
   lineage ref, tags); meaning lives in data, not filenames.
   Status: in implementation (P5) — `ehr-testing-tools.corpus.intake`:
   `{:id :path :format :layer :source :received}` catalog entries plus
   one batch intake record per source, EDN, Malli-schema'd. `:id` is
   format-aware content hash (the same function `corpus.mutate` itself
   uses for FHIR JSON, so an intaken file's catalog id and its eventual
   mutant's lineage `:parent` are one hash space, no adapter).

9. **Format-support matrix** — adding a format means filling a column
   of obligations (round-trip-verified parser, locator grammar,
   mutation operators, canonicalizers, gate engine, docs), each cell
   with maturity.
   Status: tentative.

10. **Compatibility claims as tested facts** — engine-version ×
    artifact-format claims live in F-rows or a compat table, each
    backed by a pinned smoke test.
    Status: tentative.

11. **Result vocabulary** — the result-not-throw doctrine's shared
    keywords (`:ok` / `:rejected` / `:error` + category), one
    namespace, no dialects.
    Status: validated. Evidence: ubiquitous use — every capability
    namespace landed so far (`artifact`, `invocation`,
    `corpus.generate`, `canonical`, `cli`) returns exclusively
    `result/ok` / `result/rejected` / `result/error`, with no
    namespace-local dialect ever introduced.

12. **Soft types for prose artifacts** — template/rubric pairs with
    scored membership for documents that can't be hard-schema'd
    (protocols, results files, capability doc pages); the prose
    complement of Malli.
    Status: first instance in P3 (EXP results files).

13. **Stages as resource equations** — pipeline stages written in the
    string-diagram skill's notation, `inputs → outputs [Stage]
    {catalytic: …}`. Five stage kinds, each with a law: **transform**
    appends provenance (never drops it); **normalize** is an idempotent
    endomorphism — precisely a registered canonicalizer (pattern #3),
    not a separate concept; **enrich** adds fields without altering
    existing ones; **gate** splits its output into pass / rejected /
    indeterminate and never modifies the datum it judges; **feedback**
    carries a round bound (no unbounded loops). A stage may declare
    laws beyond its kind's — kind laws are a floor, not a ceiling.
    Catalytic resources (participate unconsumed) must resolve to
    exactly one of four targets: `artifacts.lock.edn`, `deps.edn`,
    hashed repo-authored config, or an in-repo code registry referenced
    by `{id, version}` — an equation with a catalytic input that
    resolves to none of the four is malformed. **Gap surfaced by P4's
    trial, now ratified (2026-07-24, `docs/notation.md`):** Mutate's
    catalytic resource, `operator-catalog`, is an in-repo *code
    registry* (`corpus.operators`, like `corpus.canonicalizers`) — it
    fit none of the original three targets cleanly (it's not an
    acquired artifact, not a deps.edn dependency, and "hashed
    repo-authored config" as originally meant data files referenced by
    path+content-hash in manifests, not code). Its real provenance
    mechanism is reference by `{id, version}` tuple into a versioned
    in-repo registry (the same mechanism canonicalizer application
    already used) — not a hash at all. This is now the fourth target,
    named explicitly in `docs/notation.md` rather than left folded into
    "hashed repo-authored config."
    Status: validated (P6, promoted from on-trial). Trial ran through
    the Gate equations (P5) against promotion criteria pre-committed in
    P4: (1) does authoring the
    Mutate equation surface a design question before implementation
    that the equation itself answers or clarifies? (2) does every
    catalytic resource in the four authored equations (Generate,
    Normalize, Mutate, and the planned-status Gate/Report stubs)
    resolve cleanly to one of the [now four] catalytic targets? (3)
    does the generated diagram (`docs/pipeline.md`) match hand-drawn
    intuition well enough that no equation needed correction after
    seeing its diagram? A pattern that meets criteria 2–3 but not 1 is
    still promotable — criterion 1 is evidence-gathering, not a gate.
    P4 evidence: criterion (3) did **not** hold as hoped — the
    generated diagram caught a real cross-stage wiring mismatch
    (Normalize's output shape vs. Mutate's input shape) that hand-drawn
    intuition had missed, which is a stronger result for the pattern's
    value than a clean match would have been, but not what criterion 3
    as originally phrased asked for. Criterion (1) also did not fire as
    hoped: equation authoring anticipated the catalytic-resource
    question but did not itself correct the Mutate design — the gap
    surfaced and was flagged for the design channel, not resolved
    in-equation. The rule gap that flagging produced (this fourth
    target) is now found and ratified, which is real value from the
    trial, but distinct from criteria 1–3 being met; promotion of
    pattern #13 itself stays deferred to P5, when the Gate/Report
    equations move from stub to authored and criteria 2–3 can be
    re-evaluated against real, not planned-status, equations.
    P5 evidence (Gate/Intake/Report move from stub/planned to
    authored/built): criterion (1) fired positively this time, unlike
    P4 — authoring the Gate equation forced a design decision *before*
    `gate.fhir`/`gate.v2` were implemented: one equation for both
    formats (`datum`, format-dispatched at call time) or two separate
    equations. The equation's own `:laws` record the choice and the
    reason (the notation's `×` only expresses product/AND between
    inputs, no operator exists for OR between format alternatives) --
    the equation authoring genuinely clarified the design question, not
    merely anticipated it. Criterion (3) again did **not** hold, and
    surfaced a second, distinct kind of gap from P4's: the generated
    diagram renders `Gate`'s `datum` input as a disconnected source
    node with no wire from `Normalize`'s, `Mutate`'s, or `Intake`'s
    real outputs, and renders `Intake` as a dead-end with no downstream
    consumer at all -- both true to the equations as written, and both
    an honest reflection of a second notation gap: nothing in `×`/`+`
    can express "downstream of any one of several alternative upstream
    stages" (Gate genuinely accepts output from Normalize, Mutate, *or*
    Intake, not their product). Left uncorrected in the equation
    deliberately, per the same discipline P4 established: flag the gap
    for the design channel rather than force a misleading product-typed
    fix into the equation. Criterion (2) holds cleanly across all six
    now-authored equations (Generate, Normalize, Mutate, Intake, Gate,
    Report): every catalytic resource resolves to one of the four
    targets, including `hapi-hl7v2-dep` (target 2, `deps.edn`) and
    `profile-artifact` (target 1, `artifacts.lock.edn`, present but
    unpinned this session -- the target still resolves even though no
    entry exists yet). Net: two real sessions, two real diagram-caught
    gaps, criterion 1 now fired at least once — promotion is a design-
    channel call from here, not updated by this session per the
    original instruction.

    **Status: validated (P6, author-directed promotion).** The P5
    disconnected-wire gap is closed this session, not merely diagnosed:
    `docs/notation.md` now defines union resources (a named resource
    declared as the union of others, e.g. `datum` = the union of
    `canonical-fhir-datum`, `mutant-fhir-datum`, `foreign-file`) and
    external stages (a black-box stage with inputs/outputs but no laws,
    rendered dashed); `docs/pipeline.edn`'s new `:resources` key
    declares `datum` as exactly that union, and the regenerated
    `docs/pipeline.md` shows Gate's `datum` wire now sourced from a
    real `UnionDatum` merge node fed by Normalize, Mutate, and Intake's
    own input -- the P5-flagged gap, closed by notation rather than by
    equation-fudging. Evidence for validation, gathered across the
    pattern's full trial: (a) **P4's diagram-caught cross-stage wiring
    mismatch** (Normalize's output shape vs. Mutate's input shape,
    caught before either was hand-verified); (b) **P5's authoring-forced
    design decision** (format-dispatch for Gate resolved *before*
    `gate.fhir`/`gate.v2` existed, not after); (c) **conformance across
    isolated sessions** -- P4, P5, and this session each authored or
    extended equations without shared context beyond this file and
    `docs/notation.md`, and no session's equations contradicted an
    earlier session's; (d) **single-source held** -- `docs/pipeline.md`
    has never been hand-edited across three sessions, only regenerated
    from `docs/pipeline.edn` via `make pipeline`. Refinements ratified
    across the trial: the fourth catalytic target (in-repo code
    registries, P4); union resources and external stages (this
    session, P6). The tier-1 lint that checks every catalytic resource
    resolves to one of the four targets is delivered this session
    (Step 5, `make lint-pipeline`) as a **built-now deliverable**, not
    as a promotion criterion the pattern was waiting on -- promotion
    itself rests on (a)-(d) above.

14. **Operators carry contracts** — mutation (introducing a defect) and
    metamorphic perturbation (transforming input while preserving an
    expected output relation) are one operation shape wearing two
    hats: both are a function from a base datum plus a locator to a
    transformed datum, and what distinguishes them is only the
    contract they declare. A defect operator declares `{:type
    :violates :target <constraint>}` naming the base-spec constraint
    it breaks; a metamorphic operator declares `{:type :preserves
    :target <relation>}` naming the relation an oracle can check
    without a ground-truth verdict. Same registry, same schema, same
    catalog shape — only the `:contract` payload differs.
    Status: tentative — the `:contract` field ships in
    `corpus.operators`'s catalog schema this session (P4), but only
    `:violates` entries are populated; no `:preserves` (metamorphic)
    entry has been authored yet, so the second half of the pattern is
    still unexercised. A strategy schema for metamorphic comparators is
    noted here, not built.

15. **Provenance is measured at the point of execution** — every field
    that could plausibly vary by host or ambient state (locale,
    timezone, JVM version, and by extension any future entropy source)
    is forced into the subprocess explicitly and the *forced* value is
    what's recorded in the manifest — never a value sampled from the
    orchestrating process's own environment, which can silently differ
    from what the subprocess actually saw.
    Status: validated (three instances). Evidence: three P3/EXP-A4 bugs
    of exactly this species, each found and fixed the same way — (a)
    JVM version: the manifest's `:jvm-version` must describe the
    generator subprocess's JVM (Java 17, this environment), not the
    orchestrating Clojure process's JVM (Java 11) — `real-java-version`
    queries `java-bin` directly rather than reading
    `System/getProperty "java.version"`; (b) locale: forced via
    `-Duser.language`/`-Duser.country` ahead of `-jar` and recorded from
    the forced value, after EXP-A4 found a genuine locale-dependent
    output divergence; (c) timezone: forced via `-Duser.timezone` ahead
    of `-jar` and recorded from the forced value, after EXP-A4 found
    every FHIR `dateTime`/`instant` field's serialized UTC offset
    depends on it.

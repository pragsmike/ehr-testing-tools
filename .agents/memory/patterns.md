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
   Status: in implementation (P3).

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
   Status: tentative; lands with gates.

7. **Corpus recipes** — one EDN value naming a corpus intent (generator
   config, population, seed policy, mutation plan), referenced by hash
   from manifests.
   Status: tentative.

8. **Corpus catalog** — an index of corpus items (id, layer, format,
   lineage ref, tags); meaning lives in data, not filenames.
   Status: tentative.

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
    Status: on trial through the Gate equations (P5) — pattern nursery
    promotion criteria pre-committed in P4: (1) does authoring the
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

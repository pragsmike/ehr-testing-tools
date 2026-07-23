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
   Status: in implementation (P3).

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
   Status: in implementation (P3).

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
    Status: in implementation (P3).

12. **Soft types for prose artifacts** — template/rubric pairs with
    scored membership for documents that can't be hard-schema'd
    (protocols, results files, capability doc pages); the prose
    complement of Malli.
    Status: first instance in P3 (EXP results files).

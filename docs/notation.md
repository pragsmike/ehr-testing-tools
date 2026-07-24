# Pipeline Notation

The repo-local semantics for the resource-equation notation used to
describe this repo's own pipeline (`docs/pipeline.edn`, rendered as
`docs/pipeline.md`). General notation — equation syntax, diagram
conventions, the monoidal-category reading — is the
`.agents/skills/string-diagram` skill's job; this document defines
only what's specific to *this* repo's pipeline, not the notation
itself.

## Equation form

An equation names one pipeline stage:

```
inputs → outputs  [Stage]  {catalytic: …}
```

Read left to right: `inputs` are consumed, `outputs` are produced,
`[Stage]` names the operation, and `{catalytic: …}` (when present)
names resources that participate without being consumed. `×` (or the
ASCII fallback `*`) joins multiple inputs; `+` joins multiple outputs.

## Resource names bind to types

Every resource name in an equation binds to a concrete type:

- **Data resources** bind to a Malli schema — a resource named
  `fhir-datum` means "a value that validates against the schema
  documented at its point of use," not a free-floating name.
- **Prose resources** (protocols, results files, capability doc pages)
  bind to a template/rubric pair (pattern nursery
  [#12](../.agents/memory/patterns.md)) — the prose complement of
  Malli, since a protocol document can't be hard-schema'd but can
  still be scored against a fixed rubric.

An equation whose resource name binds to neither is malformed —
authoring the equation is what forces this binding to be made
explicit, before implementation.

## Catalytic resources resolve to one of four targets

A resource marked `{catalytic: …}` participates in a stage without
being consumed (pattern nursery [#13](../.agents/memory/patterns.md)).
Every catalytic resource must resolve to exactly one of four targets:

1. `artifacts.lock.edn` — an acquired, external, or binary input
   (ADR-0005): an engine distribution, a runtime, a profile package.
2. `deps.edn` — a JVM/Clojure library dependency.
3. Hashed repo-authored config — text this repo wrote and versions
   (a properties file, a module set), referenced by path plus
   content-hash at run time.
4. **In-repo code registries**, referenced by `{id, version}` — a
   versioned catalog of code this repo authors and ships as part of
   itself (the operator catalog, `corpus.operators`; the canonicalizer
   registry, `corpus.canonicalizers`). Ratified 2026-07-24, from a gap
   pattern nursery [#13](../.agents/memory/patterns.md) surfaced in
   P4: Mutate's catalytic resource is code, not an acquired artifact, a
   `deps.edn` dependency, or hashed data — its real provenance
   mechanism is version reference into a registry this repo itself
   defines, the same mechanism canonicalizer application already used
   for target 3's near-neighbor. Distinguished from target 3 by kind,
   not degree: target 3 is data this repo wrote; target 4 is code this
   repo wrote, versioned and referenced the same way data would be.

A catalytic resource that resolves to none of the four is a gap in
the equation, not an oversight to paper over silently.

## The five stage kinds

Every stage in this repo's pipeline is one of five kinds, each with a
one-line law:

| Kind | Law |
|---|---|
| **transform** | Appends provenance — never drops what came before. |
| **normalize** | Is an idempotent endomorphism — precisely a registered canonicalizer (pattern [#3](../.agents/memory/patterns.md)), not a separate concept. |
| **enrich** | Adds fields without altering existing ones. |
| **gate** | Splits its output into pass / rejected / indeterminate and never modifies the datum it judges. |
| **feedback** | Carries a round bound — no unbounded loops. |

A stage may declare laws beyond its kind's own — kind laws are a
floor, not a ceiling. A stage-specific law is anything the stage's own
equation or accompanying prose asserts that a generic stage of that
kind wouldn't otherwise guarantee (e.g. Mutate's intended-diff-only
invariant: the canonicalized diff between base and mutant touches
exactly the declared locator/contract target and nothing else — true
of this particular `transform`, not of `transform` stages in general).

## Resource contracts

A resource may carry a precondition beyond its type — e.g. a parser's
round-trip fidelity (EXP-B2). Such a contract is verified by
experiment, not asserted by fiat, and an equation that depends on it
cites the experiment that verified it (e.g. "Mutate operates on
plain-data JSON because EXP-B2 verified its round-trip is faithful,"
not merely "plain-data JSON is what we use").

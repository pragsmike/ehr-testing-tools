# Pipeline Notation

[docs/pipeline.md](pipeline.md) describes this repo's own pipeline as a
sequence of resource equations. This document is the language that
description is written in — read it before or alongside the pipeline
page if the equation syntax there is unfamiliar.

The notation exists in this repo for a specific reason: sessions
working on this codebase can't see each other, so a pipeline stage
authored by one session has to be checkable and alignable by the next
without either session holding the other's context. Writing a stage as
an equation — fixed inputs, fixed outputs, a named law — turns "does
this stage still do what it's supposed to" into something a later
session (or a reader) can verify by inspection, not something that has
to be remembered or re-explained. General notation — equation syntax,
diagram conventions, the monoidal-category reading — is the
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
   registry, `corpus.canonicalizers`). Distinguished from target 3 by
   kind, not degree: target 3 is data this repo wrote; target 4 is code
   this repo wrote, versioned and referenced the same way data would
   be. (This target was added to close a gap in the original three —
   see pattern nursery [#13](../.agents/memory/patterns.md) for the
   trial history that surfaced it.)

A catalytic resource that resolves to none of the four is a gap in
the equation, not an oversight to paper over silently.

## Union resources

A resource may be declared as the **union** of others: a named
resource whose value, at any point it's consumed, is one of a fixed
set of member resources — not their product. This closes a gap
pattern nursery [#13](../.agents/memory/patterns.md)'s P5 evidence
named directly: the equation notation's `×` expresses product/AND
between inputs, and `+` expresses coproduct/OR between *outputs*, but
nothing expressed "downstream of any one of several alternative
upstream stages" for an *input* — exactly Gate's own situation
(`datum` is genuinely fed by Normalize, Mutate, *or* Intake, never
their product).

A union resource is declared:

```edn
{:resource "datum"
 :union-of ["canonical-fhir-datum" "mutant-fhir-datum" "foreign-file"]}
```

`:resource` and each `:union-of` member are plain resource-name
strings — the same string-typed names `Stage`'s own `:inputs`/
`:outputs` already use, so a union member can be cross-referenced
against a real stage output with no keyword/string adapter layer. A
stage that consumes the union's `:resource` name accepts any member,
unchanged; the union declares no transformation of its own.

**Type-binding:** a union resource's type — soft or hard, per "Resource
names bind to types" above — is the union (sum type) of its members'
types: a Malli `[:or schema-a schema-b ...]` for data resources bound
to hard schemas, or the narrative superset of the member rubrics for
prose resources bound to soft types (pattern nursery
[#12](../.agents/memory/patterns.md)). An equation whose union members
don't share a format is a bug in the union's own declaration, not
something this rule papers over.

**Diagram rendering** reuses the string-diagram skill's existing
funnel/spider annotation (`{spider: funnel}`, many-to-one convergence)
for the union's merge node, rather than inventing new diagram
machinery for a shape the skill can already express: a union renders
as a synthetic operation (named `Union<Resource>`, e.g. `UnionDatum`)
with one wire in from each member and one wire out carrying the
union's own resource name, feeding whatever stage declares that name
as an input. This is a deliberate reuse, not a coincidence of
implementation convenience — a funnel *is* a merge node.

## External stages

An **external stage** marks a black-box operation this repo doesn't
implement — a user's own transform, run outside this repo's own
pipeline, that a use case's equations still need to name (e.g. "the
team's ingestion pipeline" in the black-box-transform-surround use
case). An external stage carries `:inputs`/`:outputs` like any other
stage, but declares no `:kind`, no `:status`, and no `:laws` — this
repo makes no claim about what an external stage does or guarantees,
only that it sits at a named point in the equation with named
resources flowing in and out:

```edn
{:id :transform :label "Transform" :external? true
 :inputs ["canonical-fhir-datum"] :outputs ["transform-output"]}
```

**Diagram rendering:** an external stage's box is rendered **dashed**
(`{external: true}` in the equation-line annotation) — the same visual
device an earlier diagram used for Gate and Report while they were
still `:planned` and not yet built (a hand-maintained distinction,
dropped once the equation-driven generator landed and both stages
moved to `:built`/undashed, per the commit history of this page). This
session revives dashed rendering as generator-supported machinery
(`{external: true}` in `resource_equations_to_mermaid.py`), reused for
a stage that will never be built by this repo at all, not merely one
that isn't built yet. The distinction matters for a reader scanning
the diagram: a solid box is something this repo implements and stands
behind; a dashed box is a named slot in the pipeline that the *use
case's own author* fills in.

## The five stage kinds

Every stage in this repo's pipeline is one of five kinds, each with a
one-line law:

| Kind | Law |
|---|---|
| **transform** | Appends provenance — never drops what came before. |
| **normalize** | Is an idempotent endomorphism — precisely a registered canonicalizer (pattern [#3](../.agents/memory/patterns.md)), not a separate concept. |
| **enrich** | Adds fields without altering existing ones. |
| **judge** | Produces a verdict plus findings over its subject; never modifies what it judges. |
| **feedback** | Carries a round bound — no unbounded loops. |

A stage may declare laws beyond its kind's own — kind laws are a
floor, not a ceiling. A stage-specific law is anything the stage's own
equation or accompanying prose asserts that a generic stage of that
kind wouldn't otherwise guarantee (e.g. Mutate's intended-diff-only
invariant: the canonicalized diff between base and mutant touches
exactly the declared locator/contract target and nothing else — true
of this particular `transform`, not of `transform` stages in general).

**Judge, and the derived `gate`** (ADR-0009,
[`docs/palgebra-design.md`](palgebra-design.md)): the kind's own law is
the judge law above — a `judge` stage never acts on the verdict it
produces. Gate's own three-way output split (pass / rejected /
indeterminate) is not part of the kind's floor; it is the derived,
policy-bearing construct `gate = judge ⨟ route-by-verdict` — a workflow
position (the `ehr gate` CLI verb, whose exit-code mapping is the
policy) built on top of a `judge`-kind stage, not a primitive of the
notation itself. Full notation treatment of sum types on output wires
(routing-in-the-algebra vs. above it) is an open question, deferred to
a later session rather than decided on paper.

## Resource contracts

A resource may carry a precondition beyond its type — e.g. a parser's
round-trip fidelity (EXP-B2). Such a contract is verified by
experiment, not asserted by fiat, and an equation that depends on it
cites the experiment that verified it (e.g. "Mutate operates on
plain-data JSON because EXP-B2 verified its round-trip is faithful,"
not merely "plain-data JSON is what we use").

## Status

This formalization is exploratory, not settled: it is on trial against
the promotion criteria recorded in pattern nursery
[#13](../.agents/memory/patterns.md), and its shape may still change as
more stages move from planned to built. The equation syntax and
diagram-rendering machinery belong to the shared
`.agents/skills/string-diagram` skill (a general monoidal-category
notation, not authored for this repo specifically); what's specific
here is only the pipeline vocabulary this document defines.

Both this notation and the `string-diagram` rendering skill originate in
the author's [cyberneutics](https://github.com/pragsmike/cyberneutics)
methodology — the upstream skill (name and description match the copy
here) lives at
[`.claude/skills/string-diagram/SKILL.md`](https://github.com/pragsmike/cyberneutics/blob/main/.claude/skills/string-diagram/SKILL.md)
in that repo (retrieved HTTP 200, 2026-07-24). Formalization here remains
exploratory, as above.

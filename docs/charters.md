# Brick charters

One page per brick, stating what it owns and what it refuses:
**mission**, **interface contract** (every `interface.clj` var, one
line each), **data shapes owned**, **invariants guaranteed**,
**non-goals**, and **forbidden edges**.

They exist so that an agent can reason about a brick from its
contract — responsibility, interface, invariants — **without reading
its source**. Read the charter first; open the source when the charter
says something you need to change.

Every line in a charter is derived from a shipped surface:
`interface.clj` and the namespaces it delegates to, their own
docstrings, each brick's `docs/limitations.md` where one exists, real
`ns` `:require` vectors, and the ADRs those docstrings cite. Nothing is
invented. Where a contract is genuinely unclear the charter says
**UNCLEAR** and gives the competing readings rather than picking one —
those entries are the author's review queue, not settled doctrine.

**Completeness is gated.** `bin/charter-completeness` checks, per
brick and in both directions, that every public interface var heads
exactly one bullet in its own brick's charter, and that no charter
bullet names a var its brick does not export. It also checks that this
index lists every charter. Today: **20 bricks, 273 interface vars, OK.**

Two charters have ADR ancestry and restate — never supersede — a
scope their ADR already ruled and gated:
[`patient-simulator`](../components/patient-simulator/docs/charter.md)
[^adr-0162] and
[`person-simulator`](../components/person-simulator/docs/charter.md)
[^adr-0172]. Both keep `docs/limitations.md` as the gated authority for
what they decline.

## Components

| brick | mission |
|---|---|
| [`corpus`](../components/corpus/docs/charter.md) | Own the corpus domain: generate, mutate, intake, check, compare, display and play synthetic clinical corpora, plus the operator and generator registries those capabilities are parameterized by. |
| [`corpus-io`](../components/corpus-io/docs/charter.md) | Own the transport and IO seam of the former corpus mega-component — sources, sinks, spooling, framing codecs, wire-level wrappers — and no domain logic at all. |
| [`docs-tooling`](../components/docs-tooling/docs/charter.md) | Own the repository's documentation machinery: generate the docs derived from the tree, and enforce the gates that keep the hand-written ones honest. |
| [`judge`](../components/judge/docs/charter.md) | Own the verdict vocabulary every judge engine reports in — reports, findings, severity ordering, the verdict cache, the pairing registry, stratified sampling — while running no validation engine of its own. |
| [`judge-fhir-official`](../components/judge-fhir-official/docs/charter.md) | Judge FHIR resources with the official HL7 FHIR validator. |
| [`judge-v2-hapi`](../components/judge-v2-hapi/docs/charter.md) | Judge HL7 v2 messages at the base-structural tier, backed by HAPI: does the message parse, and is it well-formed against the base standard. |
| [`judge-v2-nist`](../components/judge-v2-nist/docs/charter.md) | Judge HL7 v2 messages at the profile tier, backed by NIST: check a message against an IGAMT-exported conformance-profile bundle. |
| [`kernel`](../components/kernel/docs/charter.md) | Own the vocabulary and primitives every other brick needs and none should restate: the result envelope, digests, the artifact cache, canonicalizers, locators, invocation, and loud filesystem operations. |
| [`oracle`](../components/oracle/docs/charter.md) | Own the regression oracle's digest side: run a fixed set of seeded simulations and reduce each to a SHA-256 digest, so two commits can be compared byte-for-byte. Dev/CI equipment, not a shipped capability. |
| [`palgebra`](../components/palgebra/docs/charter.md) | Own the pipeline algebra: the signature language in which a stage declares what it consumes and produces, and the lint that checks a pipeline against it. |
| [`patient-simulator`](../components/patient-simulator/docs/charter.md) | Realistic EHR message traffic is the priority; patient-lifetime simulation is relevant only inasmuch as it contributes to realistic traffic. |
| [`person-simulator`](../components/person-simulator/docs/charter.md) | The person process exists so that demographic and identity traffic is realistic; a person's life is relevant only inasmuch as it changes a message. |
| [`provenance`](../components/provenance/docs/charter.md) | Own the provenance-manifest schema family as the single acyclic home two producers can both validate against. |
| [`sim`](../components/sim/docs/charter.md) | Orchestrate a simulation run — config, engine, emission, self-check, manifest — and be the one stable façade everything above the simulator depends on. |
| [`sim-check`](../components/sim-check/docs/charter.md) | Own the invariant catalog: the acceptance criteria a ground-truth event log must satisfy, and the verdict over one. |
| [`sim-emit-fhir`](../components/sim-emit-fhir/docs/charter.md) | Render a ground-truth event log as FHIR bundles. The smallest interface in the workspace. |
| [`sim-emit-hl7`](../components/sim-emit-hl7/docs/charter.md) | Render a ground-truth event log as HL7 v2 messages on a wire: the vocabulary, the timing, the emission add-ons, and fan-out to subscribers. |
| [`sim-engine`](../components/sim-engine/docs/charter.md) | Own the discrete-event simulation core: the priority queue, the decide/evolve loop, the RNG stream partition, and the ground-truth event log — this workspace's public, versioned contract. |
| [`sim-model`](../components/sim-model/docs/charter.md) | Own the nouns of the simulated world — pathways, facilities and beds, personas, run-config profiles — as schemas and seeded samplers, with no dependency on any other brick. |

## Bases

| brick | mission |
|---|---|
| [`cli`](../bases/cli/docs/charter.md) | Be the `ehrt` entry point: parse, call one capability function, print, map the result to an exit code — and nothing else. The only namespace in the workspace that prints. |

## Reading order

The dependency order is also the reading order. The four **root
bricks** depend on nothing and are where the shared vocabulary lives:
`kernel`, `sim-model`, `palgebra`, `provenance`. `oracle` is the
mirror case — the only component **nothing depends on**, which is what
keeps its unusually wide dependency set harmless.

[^adr-0162]: Design record [ADR-0162](../notes/ADRs.md).
[^adr-0172]: Design record [ADR-0172](../notes/ADRs.md).

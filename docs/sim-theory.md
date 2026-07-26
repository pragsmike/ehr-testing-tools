# The simulator's resource theory — the *want*

This document is the prose companion to [`sim-theory.edn`](sim-theory.edn),
which is the author-time source of truth (the same arrangement as
ehr-testing-tools' `pipeline.edn`/`pipeline.md`, whose
[notation](../../ehr-testing-tools/docs/notation.md) this theory is
written in — equation form, the five stage kinds and their laws,
catalytic targets, unions, external stages). The EDN keeps tools'
schema shape exactly, so its equation→Mermaid machinery renders this
theory without adaptation. This page is hand-written for now; if the
generator is ever pointed at the EDN, this prose shrinks to what the
generator can't say.

## The now/next/want reading

One file describes three systems:

- **want** — the whole theory: every stage, law, and wire. Our best
  current guess at the envisioned system; it will change, and changes
  land as edits to the EDN with ADRs where they're structural.
- **now** — the `:status :built` subset: **Execute**, **Check**, and
  **EmitHL7** (each walking-skeleton or v0-slice scope, per their
  `:contract` notes), plus the manifest component inside Package that
  shipped ahead of its stage. Everything the *now* claims is
  property-tested and green (20 tests / 55 assertions, 2026-07-26).
- **next** — the single stage marked `;; NEXT` in the EDN:
  **InjectChurn**, growing the engine's step vocabulary (transfer
  first) under Execute's existing contract on the way to the full
  Simulated-Hospital-derived churn family.

No schema keys were invented for this convention — it lives in
comments and here, keeping the EDN loadable by tools' Pipeline Malli
unchanged. When *next* completes, its stage flips to `:built`, the
`;; NEXT` marker moves, and *now* grows: the three descriptions stay
in one file by construction instead of drifting across three.

## The composite, read left to right

```
sim-config → persona                                  [Persona]      {catalytic: demographics-tables}
persona → clinical-trajectory                         [RunModules]   {catalytic: gmf-module-set, gmf-interpreter}
clinical-trajectory → compiled-pathway                [CompileTrajectory]
pathway-ir = compiled-pathway ∪ authored-pathway      (union)
pathway-ir × churn-profile → operational-pathway      [InjectChurn]
operational-pathway → ground-truth-log + state-history [Execute]
ground-truth-log → pass + rejected                    [Check]        {catalytic: invariant-catalog}
ground-truth-log → hl7v2-stream                       [EmitHL7]      {catalytic: hl7-parser-dep, message-type-registry, snomed-icd10-map}
state-history → state-document                        [EmitState]
hl7v2-stream × ground-truth-log → sim-corpus + run-manifest [Package]
sim-corpus × feed-statistics × churn-profile → churn-profile [Calibrate]  (feedback, round-bounded)
hl7v2-stream → sut-behavior                           [SystemUnderTest]   (external, dashed)
sim-corpus → catalog-entry                            [ToolsCorpusIntake] (external, dashed)
```

Three structural facts the diagram makes visible at a glance:

1. **The log is the waist.** Every output — messages, state documents,
   the corpus, the invariant verdict — is downstream of
   `ground-truth-log` (or its sibling `state-history`, produced by the
   same stage). Nothing downstream of Execute touches the RNG. That
   is ADR-0002 drawn as wires.
2. **The union is the equivalence.** `pathway-ir` as the declared
   union of `compiled-pathway` and `authored-pathway` is ADR-0002
   clause 1 stated algebraically: everything downstream of the merge
   is provably indifferent to whether a scenario was generated or
   hand-written — the same shape as tools' `datum` union, for the
   same reason.
3. **The repo boundary is dashed.** `ToolsCorpusIntake` is external
   *from sim's perspective* even though it's the sibling repo: the
   dependency arrow (ADR-0001) means sim makes no claims about what
   tools does with a corpus — only that a named resource crosses at a
   named point, in the mirrored manifest shape.

## Three catalytics added for the operational resource models

[`docs/operational-models.md`](operational-models.md) names three
resources this composite was previously silent on, and the EDN now
wires all three as catalytic inputs at the stage where each binds:
`order-profiles` and `provider-pool` on `Execute` (target 3, hashed
US-units config each — order/result step types and attending
assignment both land under Execute's existing `:built` contract as
the engine's step vocabulary grows, the same co-landing path
`InjectChurn`'s churn family and `EmitHL7`'s message types already
follow), and `payer-pool`, recorded as a comment at `Persona` rather
than a real wire, since Persona itself is still `:planned` and payer
sampling runs at engine patient-init time until it lands. Adding
`order-profiles` repairs the plan's biggest capture gap to date:
Simulated Hospital's order profiles and the ORM/ORU result cycle were
discussed from this project's first session but, until this pass,
named in no planning artifact.

## Resource type bindings

Per the notation, every resource name binds to a type; an equation
whose name binds to nothing is malformed. Bindings, with build status:

| Resource | Binding | Status |
|---|---|---|
| `sim-config` | Malli, `ehr-testing-sim.config` (black-box Inputs, problem statement) | partial |
| `persona` | Malli, planned | planned |
| `clinical-trajectory` | Malli, planned — dated clinical events, each citing `{module, state}`, codes as `{:system :code :display}` | planned |
| `compiled-pathway`, `authored-pathway`, `pathway-ir`, `operational-pathway` | Malli, `ehr-testing-sim.pathway` — the union binds to `[:or …]` of its members per the notation; `operational-pathway`'s type IS the IR type (the endomorphism law) | v0 built |
| `ground-truth-log` | Malli, planned as data; shape established by `engine/run` and consumed by `check` | de-facto built |
| `state-history` | Malli, planned — per-patient `[t → state]`; today implicit in the pure fold, the want makes it a first-class output | planned |
| `hl7v2-stream` | ER7 messages over the parser's structures | planned |
| `state-document` | FHIR JSON or CDA XML (format dispatch) | planned |
| `run-manifest` | Malli, `ehr-testing-sim.manifest/MirroredManifest` (tools' ManifestV1_1 mirror) | built |
| `sim-corpus` | directory layout + manifest, planned | planned |
| `churn-profile`, `feed-statistics` | Malli, planned; `feed-statistics` is site-supplied summary statistics — never raw feed content (see global laws) | planned |
| `invariant-catalog` | not a wire resource — catalytic, see below | v0 built |

## Catalytic resolution

Every catalytic resource must resolve to one of the notation's four
targets; unresolved is a gap, not an oversight:

| Catalytic | Target | Note |
|---|---|---|
| `demographics-tables` | 3 — hashed repo-authored config | vendored US tables (Synthea-derived, Apache-2.0), referenced by path + content hash |
| `gmf-module-set` | **1 or 3 — OPEN** | vendor-vs-lockfile is the decision ADR-0003 defers to when modules land; the equation names the resource without prejudging |
| `gmf-interpreter` | 4 — in-repo code registry | the GMF interpreter, versioned like data |
| `invariant-catalog` | 4 — in-repo code registry | `ehr-testing-sim.check/catalog`, versioned; the co-landing law couples it to Execute's step set |
| `hl7-parser-dep` | 2 — deps.edn | `org.clojars.cmiles74/clojure-hl7-parser 3.5.1` (facts-register) |
| `message-type-registry` | 4 — in-repo code registry | event→message-type mapping (ADT^A01 …), the emitter's own catalog |
| `snomed-icd10-map` | 1 — artifacts.lock | the pinned NLM map; the one sanctioned code translation in the theory |
| `order-profiles` | 3 — hashed repo-authored config | US-units order/result profiles, binds at `Execute` (`docs/operational-models.md`) |
| `provider-pool` | 3 — hashed repo-authored config | synthetic provider identities, binds at `Execute` (`docs/operational-models.md`) |
| `payer-pool` | 3 — hashed repo-authored config | synthetic payer pool; binds at `Persona` once it lands — comment only today, no wire yet (`docs/operational-models.md`) |

## Global laws

Laws that belong to the composite, not any one stage:

**Determinism (the pipeline is a function).** The entire composite
from `sim-config` (which contains the seed) to every output is a pure
function: the single seeded RNG is the only entropy, consumed in an
order fixed by the total event ordering. Per-stage determinism laws
are local obligations under this one global claim. Property-tested at
the *now* boundary; the obligation extends to each stage as it flips
to `:built`. Corollary worth naming: **sim needs no Normalize stage.**
Tools canonicalizes because its upstream generator isn't deterministic;
sim's determinism is native, so the stage kind `normalize` is
deliberately absent from this theory — a designed-away stage, not a
gap. (The theory uses three of the five kinds: `transform`, `judge`,
`feedback`.)

**No-PHI by construction, visible in the diagram.** No wire enters the
diagram from any real-record source: every input is authored data,
public statistical tables, or Apache-licensed module content. The
safety argument (validation claim #7) is therefore checkable by
*inspecting the diagram* — any future stage proposing a real-data wire
is a visible, reviewable breach of this law, not a quiet code change.
The one subtlety is `feed-statistics` into Calibrate: it is
site-supplied *summary statistics* (rates, mixes, length-of-stay
distributions), never raw feed content, and Calibrate's own law
confines its influence to churn parameters — operational realism may
learn from a site; clinical content never does.

**Emitter coherence.** `hl7v2-stream` and every `state-document` are
renderings of the same ground truth: replaying the message stream
reconstructs `state-history`, and a snapshot at instant *t* agrees
with the state implied by the messages up to *t*. This is the
problem-statement guarantee "every message derivable from the log, and
vice versa," extended across emitters — a *want*-level law that
becomes a cross-emitter property test when EmitState lands.

**Code provenance.** Concept triplets flow unchanged from module JSON
through trajectory, IR, and log; emitters render codes natively and
never translate — except the single sanctioned SNOMED→ICD-10-CM
translation in EmitHL7, which is itself pinned (catalytic target 1)
rather than computed.

## Open questions

Recorded here rather than silently decided:

1. **`gmf-module-set`'s catalytic target** — vendor (3) vs lockfile
   (1); ADR-0003's trigger, decided when modules land.
2. **Does InjectChurn commute with CompileTrajectory?** I.e., is
   churn-then-compile ≡ compile-then-churn for any sensible
   compile-side churn? The theory currently says no by fiat (churn is
   defined on IR, after the union, so it applies to authored pathways
   too — a reason, not just a choice), but the algebraic question is
   worth settling on paper before the compiler exists.
3. **Is `state-history` primitive or derived?** Execute currently
   outputs both log and history; if replaying the log provably
   reconstructs history (emitter-coherence law), history is derived
   and could drop out of Execute's signature, with EmitState consuming
   `ground-truth-log` instead. Deferred until EmitState exists to
   test it.

   **RESOLVED (ADR-0008), ahead of the "deferred until EmitState
   exists" plan above:** `state-history` is derived. The engine's
   `decide`/`evolve` split makes `evolve (world, event) -> world'` the
   only function that ever produces a new patient state, and it is a
   pure fold over the log — `state-history` at any prefix is
   `(reduce evolve initial-world log-prefix)`, a computed projection,
   not independent bookkeeping the engine could let drift from the
   log. The original entry is left standing above rather than deleted,
   per this project's append-don't-erase convention for resolved
   questions; EmitState (M6) still consumes this resolution when it
   lands, it just no longer has to *establish* it.
4. **Sum types on output wires** — inherited open question from
   tools' notation (Gate's routing discussion); sim's Check has the
   same shape (pass/rejected) and defers the same way.
5. **Delivery** (file / stream / MLLP) is deliberately absent — it's
   transport below the theory's level of description, not a stage. If
   paced/real-time emission ever acquires laws of its own (e.g.
   bounded reordering), it earns an equation then.

# The simulator's resource theory — the *want*

This document is the prose companion to [`sim-theory.edn`](sim-theory.edn),
which is the author-time source of truth (the same arrangement as
ehr-testing-tools' `pipeline.edn`/`pipeline.md`, whose
[notation](https://github.com/pragsmike/ehr-testing-tools/blob/main/docs/notation.md) this theory is
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
- **now** — the `:status :built` subset: **Execute**, **Check**,
  **EmitHL7**, **InjectChurn** (M2b), **Persona** (M4), **RunModules**
  and **CompileTrajectory** (M5b; each walking-skeleton, v1-slice, or
  v0-slice scope, per their `:contract` notes), and, as of Milestone M6,
  **EmitState** (FHIR R4 only, CDA deferred with its own contract note)
  — plus the manifest component inside Package that shipped ahead of
  its stage. As of Milestone M3, Execute's own step vocabulary further
  grows (`:order`/`:result-followup`, the `order-profiles` catalytic now
  real) and EmitHL7 gains ORM^O01/ORU^R01. As of the site-profiles
  milestone, EmitHL7 gains a fourth catalytic (`site-profile`) — MSH
  dialect, code-table overrides, and Z-segment templates, all
  property-tested against the dialect-invariance law stated on that
  stage's own equation entry. As of Milestone M5b, Execute's step
  vocabulary grows again (`:outpatient-visit`/`:outpatient-visit-end`/
  `:procedure`/`:observation`/`:medication-order`/`:medication-end`) and
  EmitHL7 gains A04 (outpatient visit) and a second ORU^R01 rendering
  (an unsolicited Observation, no order context). As of Milestone M6,
  `ehrt.sim-engine.engine`'s own fold (`PatientState`) grows a
  clinical-content accumulator (`:conditions`/`:observations`/
  `:medication-orders`/`:discharged-at`) so EmitState can render from
  folded state alone, never the log directly — the concrete mechanism
  behind the now-property-tested emitter-coherence global law, below.
  Everything the *now* claims is property-tested and green (388 tests /
  1015 assertions, 2026-07-27).
- **next** — the single stage marked `;; NEXT` in the EDN: **Calibrate**,
  the feedback stage that fits a run's churn parameters to a site's own
  observed feed statistics — the last stage with nothing built yet.
  EmitState (Milestone M6) is complete: FHIR R4 resources render from
  folded state, the emitter-coherence global law (below) is a real
  property test for the first time, and the `;; NEXT` marker has moved
  on.

No schema keys were invented for this convention — it lives in
comments and here, keeping the EDN loadable by tools' Pipeline Malli
unchanged. When *next* completes, its stage flips to `:built`, the
`;; NEXT` marker moves, and *now* grows: the three descriptions stay
in one file by construction instead of drifting across three.

## The composite, read left to right

```
sim-config → persona                                  [Persona]      {catalytic: demographics-tables, payer-pool}
persona → clinical-trajectory                         [RunModules]   {catalytic: gmf-module-set, gmf-interpreter}
clinical-trajectory → compiled-pathway                [CompileTrajectory]
pathway-ir = compiled-pathway ∪ authored-pathway      (union)
pathway-ir × churn-profile → operational-pathway      [InjectChurn]
operational-pathway → ground-truth-log + state-history [Execute]
ground-truth-log → pass + rejected                    [Check]        {catalytic: invariant-catalog}
ground-truth-log → hl7v2-stream                       [EmitHL7]      {catalytic: hl7-parser-dep, message-type-registry, snomed-icd10-map, site-profile}
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
follow), and `payer-pool`, recorded at the time as a comment at
`Persona` rather than a real wire (Persona itself was still `:planned`)
-- since landed for real, Milestone M4, see that section below. Adding
`order-profiles` repairs the plan's biggest capture gap to date:
Simulated Hospital's order profiles and the ORM/ORU result cycle were
discussed from this project's first session but, until this pass,
named in no planning artifact.

## Site profiles land `site-profile` as a fourth catalytic on EmitHL7

[`site-profiles.md`](../../../docs/site-profiles.md) — the "simulate MY
hospital" config layer — lands as a fourth catalytic input on
`EmitHL7`, the same shape `order-profiles`/`provider-pool` joining
`Execute` already established: no new stage, no new `:inputs`/
`:outputs`, one new catalytic wire on a stage that was already
`:built`. `site-profile` (`ehrt.sim.site-profile/SiteProfile`)
is an optional config value — absent, nil, or `{}` all render
identically to the pre-milestone baseline, property-tested as this
milestone's own determinism anchor — carrying an MSH dialect (version,
sending/receiving app+facility), code-table overrides for PV1-2/PV1-36
(rendering-time substitutions over the SAME underlying state value,
never a new fact), and a Z-segment template DSL (field bindings to
state/persona/event paths, rendered after standard segments, escaped,
unbound paths empty rather than throwing). The milestone's own thesis —
two site profiles over one seed produce the same ground truth in two
accents — is the dialect-invariance law now stated directly on
`EmitHL7`'s own equation entry (`sim-theory.edn`), property-tested
alongside the structural guarantee that `site-profile` never reaches
`Execute` at all (not a member of `ehrt.sim-engine.engine/config-keys`).
`:naming :surge-format`'s migration to the profile is the one
documented exception bound at config-construction time rather than
emit time (`ehrt.sim.site-profile/apply-naming`, a facility-
config transform a caller applies before `Execute`, never auto-wired)
— named here so it isn't mistaken for a second catalytic wire this
stage doesn't actually carry.

## M2a lands under Execute's existing contract — no new stage or wire

[ADR-0010](../notes/ADRs.md#adr-0010) (patient identity, MRNs-as-state,
the `:participants` event shape) and [ADR-0011](../notes/ADRs.md#adr-0011)
(seconds granularity, a pinned UTC offset, a warm-up window) are both
now **landed** (M2a session, test-first, `engine.clj`/`check.clj`/
`emit-hl7.clj`/`run.clj`/`cli.clj`) as engine-internal refactors under
`Execute`'s existing `:built` contract — they change what a
patient-fold key and an event's timestamp/subject shape look like
internally, not `Execute`'s declared inputs, outputs, or catalytic
wires (`provider-pool`, `order-profiles`), so neither decision needed
an EDN edit beyond folding a note into `:execute`'s own `:contract`
string. Since EDN comments/prose are stripped or ignored before the
equation→Mermaid machinery ever sees the structural keys (`:id`
`:kind` `:status` `:inputs` `:outputs` `:catalytic`), this cannot
change what any diagram generated from `sim-theory.edn` renders —
confirmed by inspection rather than by re-running the generator, since
neither change touches a structural key. ADR-0011's **seeded arrival
process** (an alternative to fixed `:patients N`) was sketched but not
built this session — explicitly a stretch item behind the seam, per
the M2a session's own plan; M2b does not depend on it.

## M3 lands under Execute's existing contract — no new stage or wire

`order-profiles` was already a declared catalytic on `Execute` (the
previous section) before this milestone — Milestone M3 makes it real
(`ehrt.sim-engine.order-profiles`, a small hand-curated CBC+BMP starter
set, real LOINC codes verified against loinc.org,
`notes/facts-register.md` F7) and lands the two new step types it
feeds: `:order` (author-facing IR, `{:type :order :profile :cbc}`) and
`:result-followup` (engine-internal only — never hand-authored; an
`:order`'s own `decide` call auto-pairs its result after a
profile-sampled turnaround, the choice recorded over a hand-authored
`:result{:order-ref ...}}` step because it keeps authored pathways
ergonomic, `docs/patient-state-model.md`). `EmitHL7` gains ORM^O01 and
ORU^R01 the same way it gained the churn family's ADT triggers in
M2b — a `message-type-registry` addition, not a redesign. None of this
touches `Execute`'s or `EmitHL7`'s declared `:inputs`, `:outputs`, or
`:catalytic` keys (`order-profiles` was already wired; ORM/ORU are new
registry entries, not new catalytic wires), so — the same argument the
M2a section above already made, confirmed by inspection rather than by
re-running the generator — this cannot change what any diagram
generated from `sim-theory.edn` renders. `:step-rejected` (ADR-0012,
also landed this milestone) renders in **neither** diagram nor
message-type-registry, by design: it is truth about the run, not wire
traffic a real ADT/ORM/ORU feed would ever carry.

## M4 lands Persona -- its own :built stage, folded into Execute's step queue

Unlike M2a/M3 above (additions under an ALREADY-`:built` Execute),
Persona genuinely flips from `:planned` to `:built` this milestone --
a new stage, not an addition to one already landed. Its equation
(`sim-config -> persona`) is satisfied by an engine-internal
`:registered` event `ehrt.sim-engine.engine/run` prepends to every
patient's step queue (never authorable IR, the same treatment
`:result-followup` already gets) -- persona is folded into Execute's
own step-queue mechanism because a patient's persona is needed at the
same init moment Execute already owns (arrival scheduling), not by a
downstream consumer that exists yet (`RunModules`, still `:planned`,
is the eventual real consumer of `persona` as a wire resource). The
diagram stays topologically truthful (`sim-config` still flows to
`persona` as its own box) while `:persona`'s own `:contract` string
records the implementation shape honestly, the same "document the
gap between diagram and code" discipline the M2a/M3 sections above
already establish for their own implementation notes. `payer-pool`
(`docs/operational-models.md`'s payers model) is a real `:catalytic`
wire on `Persona` now, not a comment-only forward reference -- this
also RETIRES the engine-patient-init payer stand-in
`docs/operational-models.md` described (there was no code actually
setting it; payer now lives at `(:payer (:persona patient))`, sampled
with the rest of a patient's demographics). `demographics-tables`
(vendored, `resources/demographics/`) are SMALL and hand-curated, not
extracted from Synthea -- no `../` checkout was available this
session; `resources/demographics/NOTICE` records that decision so a
future session with a checkout can extract for real without any
reader-side schema change.

## IR transforms as the composition layer

Synthea's own composition pain — cross-cutting augmentation requiring
every module to be edited individually (#780); modules surprising users
via always-on global execution and hidden hard-coded Java lifecycle
behavior (#941, #1126) — per
`docs/research/SimHospital-Synthea-limitations-considered.md` §4.2, is
answered structurally by this composite's own shape, not by a feature
added in response to reading about it. **IR→IR transforms between
`CompileTrajectory` and `Execute` are the cross-cutting composition
mechanism; `InjectChurn` (landed, M2b) is the first instance of this
pattern, not a special case bolted on beside it.** "Attach vital signs
to every emergency encounter" is exactly this shape: a transform
written once over `pathway-ir`, touching no module and no per-module
edit, the same way `InjectChurn` inserts churn steps into any
pathway — authored or compiled — without either pathway's own author
knowing a transform ran. A corollary this pattern commits this project
to, named now rather than left implicit until M5's module interpreter
exists to violate it by accident: **no hidden modules** — any lifecycle
behavior this project ever runs (birth, aging, death, whatever M5's GMF
port needs) must be an explicit, listable stage or transform, never an
always-on, invisible pass the way Synthea's built-in Java lifecycle
modules are reported to surprise users who tried to run only their own
custom module set (discussion #1126). `.agents/plans/roadmap.md`'s M5
entry carries this as a roadmap note.

## Resource type bindings

Per the notation, every resource name binds to a type; an equation
whose name binds to nothing is malformed. Bindings, with build status:

| Resource | Binding | Status |
|---|---|---|
| `sim-config` | Malli, `ehrt.sim.config` (black-box Inputs, problem statement) | partial |
| `persona` | Malli, `ehrt.sim.persona/Persona` | v1 built |
| `clinical-trajectory` | de-facto built as data, no formal Malli type yet — dated clinical events, each citing `{module, state}`, codes as `{:system :code :display}` (`ehrt.sim-trajectory.gmf-interpreter/run-module`'s own `:trajectory` output) | de-facto built |
| `compiled-pathway`, `authored-pathway`, `pathway-ir`, `operational-pathway` | Malli, `ehrt.sim.pathway` — the union binds to `[:or …]` of its members per the notation; `operational-pathway`'s type IS the IR type (the endomorphism law). `compiled-pathway` is real as of M5b (`ehrt.sim-trajectory.compile-trajectory`, six new step types plus the `:citation`/`:conditions` provenance fields) | v1 built |
| `ground-truth-log` | Malli, planned as data; shape established by `engine/run` and consumed by `check` | de-facto built |
| `state-history` | Malli, planned — per-patient `[t → state]`; today implicit in the pure fold, the want makes it a first-class output | planned |
| `hl7v2-stream` | ER7 messages over the parser's structures | v1 built |
| `state-document` | FHIR R4 JSON (`ehrt.sim-emit-fhir.emit-fhir`) now; CDA XML deferred with a contract note | M6 built (FHIR arm) |
| `run-manifest` | Malli, `ehrt.sim.manifest/MirroredManifest` (tools' ManifestV1_1 mirror) | built |
| `sim-corpus` | directory layout + manifest, planned | planned |
| `churn-profile` | Malli, `ehrt.sim-engine.churn/ChurnProfile` — step-type → per-insertion-point probability | v1 built |
| `feed-statistics` | Malli, planned; site-supplied summary statistics — never raw feed content (see global laws) | planned |
| `invariant-catalog` | not a wire resource — catalytic, see below | v0 built |

## Catalytic resolution

Every catalytic resource must resolve to one of the notation's four
targets; unresolved is a gap, not an oversight:

| Catalytic | Target | Note |
|---|---|---|
| `demographics-tables` | 3 — hashed repo-authored config | vendored US tables, `resources/demographics/` -- SMALL and hand-curated this milestone (no Synthea checkout available; NOTICE records why), same schema shape a real extraction would use |
| `gmf-module-set` | 3 — hashed repo-authored/derived config | **RESOLVED, ADR-0013** (author-ratified 2026-07-27): a small, curated subset vendored into `resources/modules/`, hashed and provenance-tracked per-module in a `resources/modules/NOTICE` file (the same role `resources/demographics/NOTICE` plays), not a lockfile (target 1) — ADR-0003's own trigger, decided once `components/sim-trajectory/docs/gmf-interpreter.md`'s candidate-module survey gave the question something concrete to be decided against. Explicit revisit trigger: a lockfile, if the vendored set ever grows past roughly ten modules |
| `gmf-interpreter` | 4 — in-repo code registry | the GMF interpreter, versioned like data |
| `invariant-catalog` | 4 — in-repo code registry | `ehrt.sim.check/catalog`, versioned; the co-landing law couples it to Execute's step set |
| `hl7-parser-dep` | 2 — deps.edn | `org.clojars.cmiles74/clojure-hl7-parser 3.5.1` (facts-register) |
| `message-type-registry` | 4 — in-repo code registry | event→message-type mapping (ADT^A01 …), the emitter's own catalog |
| `snomed-icd10-map` | 1 — artifacts.lock | the pinned NLM map; the one sanctioned code translation in the theory |
| `order-profiles` | 3 — hashed repo-authored config | US-units order/result profiles, binds at `Execute` (`docs/operational-models.md`) |
| `provider-pool` | 3 — hashed repo-authored config | synthetic provider identities, binds at `Execute` (`docs/operational-models.md`) |
| `payer-pool` | 3 — hashed repo-authored config | synthetic payer pool, `ehrt.sim.persona/under-65-payers`/`sixty-five-plus-payers` — a real wire on `Persona`, Milestone M4 (`docs/operational-models.md`) |

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

**Emitter coherence — PROPERTY-TESTED, Milestone M6.** `hl7v2-stream`
and every `state-document` are renderings of the same ground truth:
replaying the message stream reconstructs `state-history`, and a
snapshot at instant *t* agrees with the state implied by the messages
up to *t*. This is the problem-statement guarantee "every message
derivable from the log, and vice versa," extended across emitters —
stated as a *want*-level law since this file's own earlier drafts, now
a real property test:
`ehrt.sim.v2-replay-test/emitter-coherence-reconstructed-state-matches-the-log-fold-at-every-boundary`
(150 trials, pathways + order/result + non-two-participant churn) and
its sibling `emitter-coherence-holds-for-module-driven-outpatient-trajectories`
(150 trials, module-driven trajectories), both green 2026-07-27. The
mechanism: `ehrt.sim.v2-replay` folds a run's own emitted ER7
stream through an independent reconstruction (`fold-message`), and
`ehrt.sim.v2-replay/project-to-wire-visible-fields` — a
deliverable in its own right, the formal statement of what the wire
actually carries, sibling of `ehrt.sim.site-profile`'s own
dialect-masking function — projects BOTH the reconstructed and the
log-folded state down to the same comparable shape before comparing.
Documented scope boundary, not silent: the property excludes bed-swap
(A17) and merge (A40), genuinely two-participant messages whose own
wire-identity reconstruction (a shared MRN reassigned mid-run) is real,
separate engineering scope — `ehrt.sim.v2-replay`'s own header
comment and `unsupported-triggers` name this precisely, the same
"deferred with a contract note" treatment EmitState's own CDA arm gets.
One genuine finding surfaced and fixed during this property's own
development: a degenerate but structurally legal churn sequence
(cancel-admit against an already-discharged patient's original
admission, followed by cancel-discharge) left ground truth's own
`:class` absent while the wire always asserts `:inpatient` for that
message family regardless — `ehrt.sim-engine.engine/evolve`'s own
`:cancel-discharge` method now restores `:class` as part of its
reinstatement, closing the gap in ground truth rather than loosening
the projection to hide it.

A named sub-law, surfaced by mining Synthea's own cross-format id
divergence between CDA, FHIR, and CSV exports
(`docs/research/SimHospital-Synthea-limitations-considered.md` §4.1):
**every emitter renders the same event ids and patient ids for the
same ground-truth facts** — no format-local id scheme, no re-derivation
that could drift from another emitter's choice for the same event.
PROPERTY-TESTED alongside the law above:
`ehrt.sim-emit-fhir.emit-fhir-test/fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`
(150 trials, green 2026-07-27) — FHIR `Patient.id` is the same
`patient-id` `ehrt.sim-engine.engine/patient-id-for` assigns, and
`Patient.identifier`'s MRN matches PID-3 on that same patient's own
HL7 messages, over random runs.

**Code provenance.** Concept triplets flow unchanged from module JSON
through trajectory, IR, and log; emitters render codes natively and
never translate — except the single sanctioned SNOMED→ICD-10-CM
translation in EmitHL7, which is itself pinned (catalytic target 1)
rather than computed.

## Open questions

Recorded here rather than silently decided:

1. **`gmf-module-set`'s catalytic target** — vendor (3) vs lockfile
   (1); ADR-0003's trigger, decided when modules land.

   **RESOLVED (ADR-0013), the same "leave the original entry standing,
   append the resolution" convention open question #3's own resolution
   (ADR-0008) already established:** target 3, vendor a small curated
   subset into `resources/modules/`, hashed and provenance-tracked
   per-module — see the Catalytic resolution table above for the full
   citation. `components/sim-trajectory/docs/gmf-interpreter.md`'s own candidate-module survey
   (its appendix) names the first module recommended to vendor under
   ADR-0013's own curation criterion, for author ratification alongside
   the ADR.
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

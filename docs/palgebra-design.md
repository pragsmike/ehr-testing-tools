# Palgebra and the Judge/Gate Factorization

**Status:** design record, pre-implementation. Decisions below are marked settled / provisional / open.
**Scope:** the process-algebra layer (palgebra) of `ehr-testing-tools`, its two modeling layers (abstract and lowered), the judge/gate factorization it induces, and the refactoring both imply.
**Companion:** [`.agents/plans/judge-gate-refactor.md`](../.agents/plans/judge-gate-refactor.md) (the concrete, repo-inventoried execution plan). This document is the *why*; that one is the *what, in what order*.

---

## 0. Decision Register

Each entry: the decision, one line of rationale, status. Cross-references point into Parts I–III.

| # | Decision | Rationale | Status |
|---|----------|-----------|--------|
| D1 | **Three layers: observe → judge → act.** Deciders compute verdicts; enforcers act on them. "Gate" is a workflow role, not a component behavior. | The verdict is reusable under opposite polarities (contract pairing) and different consequences (baseline gating) — so neither belongs in the decider. | Settled (§II.1) |
| D2 | **Judge** is the general decider; **validator** and **checker** are species distinguished by criterion provenance (institutional standard vs. occasional expectation), not by type. | One signature, one verdict type; enforcers stay polymorphic; nothing written twice. | Settled (§II.2) |
| D3 | A **judgment** is ⟨ref(subject), verdict, findings⟩ — it carries its subject's identity. | A verdict floating free of what it's about is an incomplete type; content-hash refs make judgments self-authenticating. | Settled (§II.3) |
| D4 | **Two registries**: institutional (criteria: profiles, IGs — long-lived, cached) and run-scoped (subjects: the coat check — populated at intake, GC'd at run end). Registration hangs on *role and lifetime*, not on catalyticity. | "Catalytic ⇒ register" misfired because it conflated a wire property with a resource role. | Settled (§II.4) |
| D5 | **Two modeling layers**: abstract (content on wires, no cost) and lowered (refs, stores, caches). The lowered layer is the image of the abstract under named transformations, never an independent model. | Semantics and equality live in the abstract layer; optimizations are movement within erasure fibers. | Settled (§I.4) |
| D6 | `lower ⨟ erase = id` is the soundness anchor; each rewrite rule is checked by erasing both sides. Adjoint structure is aspirational, adopted only if a real order emerges. | Fiber-local checks replace whole-compiler proofs; the adjunction's extra bookkeeping isn't yet earned. | Settled (§I.5, §III.4) |
| D7 | Composition is written **`⨟` (diagrammatic order) exclusively; `∘` is banned** from the algebra. Clojure combinators are threading-shaped. | Silent order-reversal bugs; Clojure itself contains both conventions (`comp` vs `->`). | Settled (§I.2) |
| D8 | The diagram→diagram compiler is **`lower`** (plus named **passes**); diagram→Clojure is **`emit`**. The emitter is dumb, trusted-but-traced via source-map metadata. | Optimization is only provable where erasure exists; Clojure has no faithful erasure. | Settled (§I.6) |
| D9 | Palgebra develops **inside `ehr-testing-tools`** under its own namespace root, extracted to its own repo when a second instance or external user appears. Dependency direction `palgebra ↛ ehr` enforced by CI lint from day one. | Avoids lockstep version churn while the signature format is fluid; discipline is cheap while one head writes both halves. | Settled (§I.7) |
| D10 | **Verdict gains a fourth arm**: `no-verdict(cause)`, in a different type position than the verdicts proper. Abstractly the judge is total (three-valued); partiality is introduced by lowering. | "Ambiguous under the criterion" and "could not reach a verdict" demand different policy responses; folding them forces policies to spelunk findings. | Settled (§II.5); *cause taxonomy open (O2)* |
| D11 | The finding field currently named **`:policy`** (gate.fhir's per-issue verdict classification) is renamed **`:disposition`**. | Direct collision with the policy layer (verdict→action); criterion-layer semantics wearing an act-layer word. | Settled, discovered in repo inventory (§II.6) |
| D12 | The CLI verb **`ehr gate` keeps its name**; `gate.*` namespaces, `pipeline.edn`'s `:gate` kind, and `gate-calibration.md` are renamed to judge vocabulary. | The CLI genuinely is a gate (exit-code mapping is its policy; `--baseline` is already an explicit policy argument); the libraries are judges. | Settled (§II.7) |
| D13 | The **signature is data** (EDN the palgebra machinery loads), not protocols the theory extends. Emitter hooks are the one legitimate code-level extension point, registered as data pointing at vars. | If instantiating the language means implementing protocols, the repo split is cosmetic. Precedent: `pipeline.edn` already works this way. | Settled (§I.3) |
| O1 | **Verdict value names**: keep `:pass / :rejected` (act-flavored but serialized everywhere) or rename to verdict-layer words (`:conform / :violate`)? | Renaming is the one change touching serialized reports and both integration suites. | **Open** — plan prices both (§II.5) |
| O2 | **`no-verdict` cause taxonomy**: minimum viable set, and whether `terminology-suppressed` (gate.fhir's current `:indeterminate`) is `no-verdict` or `ambiguous`. | First live specimen of the D10 split; this doc argues no-verdict (§II.5) but the call is not yet made. | **Open** |
| O3 | **Routing-by-verdict: in the algebra or above it?** Sum-type outputs as a first-class construct vs. gates as workflow-level constructs calling algebra-level diagrams. | The union-resource construct answers the input side; the output side is untested. Adjudicated by re-expressing the pipeline (plan step 2), not on paper. | **Open** |
| O4 | Whether **`normalize`'s idempotence** and other kind laws become palgebra-level law forms or stay prose. | Depends on how far law-as-data goes in the signature format v1. | **Open** |
| O5 | The palgebra namespace's public name (working name: `palgebra`). | Naming the future repo; low urgency. | **Open** |

---

## Part I — The Palgebra Language

### I.1 What exists and what it is

The language already lives in this repo, unnamed and scattered. Its current constituents:

- **Notation and rendering**: the `.agents/skills/string-diagram` skill (from cyberneutics) — equation syntax, Mermaid rendering (`resource_equations_to_mermaid.py`), spider/funnel conventions, dashed-external rendering.
- **Repo-local embellishments** (documented in `docs/notation.md`): catalytic annotations with a four-target resolution rule; **union resources** (input-side coproducts, rendered as funnels); **external stages** (dashed boxes, no laws claimed); resource-name-to-type binding (Malli for data, template/rubric for prose); resource contracts verified by experiment.
- **Machinery**: `pipeline.clj` (loads and validates the equation EDN — this is *signature loading*), the tier-1 catalytic lint (`lint.clj`), the generated-diagram Make targets.

The base formalism is Fong & Spivak's: objects, morphisms, monoidal composition, spiders. Diagrams and expressions are isomorphic; "diagram" below means either.

### I.2 Composition order (D7)

The algebra uses **`⨟` (diagrammatic / left-to-right) exclusively**. `gate = judge ⨟ policy` reads: judge the subject, then apply policy to the judgment. `∘` does not appear in palgebra documents or code comments. In Clojure, pipeline combinators are threading-shaped — `(pipeline judge policy)` means judge-then-policy — and any wrapper over `comp` reverses arguments so source reads like diagrams. Rationale: reversed composition of type-compatible stages fails silently; the implementation language natively contains both conventions.

### I.3 What the abstract layer needs (the doodad inventory)

In rough order of load-bearing weight:

1. **Wire roles / sorts.** Subject, criterion, judgment are different *sorts* (a colored prop, not single-sorted). Enforceable consequence: illegal wirings are inexpressible; "criterion enters from the side" becomes a rule, not a drawing convention. Convention: criterion wires enter judges laterally, styled like registry wires, so validator vs. checker is *visible* (side wire from registry box vs. from harness).
2. **Sums on wires.** Verdict is a coproduct; gate routing is a case-split. Input-side merge already exists (union resources / funnels). Output-side branch is the open construct (O3) — the biggest genuine extension over vanilla F&S, adjudicated by plan step 2.
3. **Derived-stage definitions.** `gate = judge ⨟ route-by-verdict` is a macro facility: named diagram patterns, expandable. Also the mechanism for "models, plural" — one signature, many top-level configuration diagrams over a shared stage library.
4. **Law annotations.** Judge purity, subject immutability, policy totality — attached to definitions, citable by name, because lowering rules cite them (I.5).
5. **Provenance decorations** on criterion wires (institutional / occasional). Documentation-grade: changes no equation; marked as decoration, like catalyticity.

Multi-configuration support is a module story, not a notation story: per-use-case *diagrams*, never per-use-case *sorts*.

### I.4 The two layers (D5)

- **Abstract**: files are values, wires carry content, spiders are free, the judge is `subject × criterion → judgment` with the subject literally in the judgment. No refs anywhere. All laws and all equations live here. Two workflows are equal iff their abstract diagrams are equal.
- **Lowered**: the image of abstract diagrams under named transformations. Refs, stores, intake, caches exist here — typed as **infrastructure sorts**, with the closure rule: every well-formed lowered diagram is the image of some abstract diagram, modulo infrastructure; no abstract-sorted wire may depend on infrastructure for its *content*. Anything violating this is editing semantics from the wrong layer.

Vocabulary hygiene: plain words (file, judge, judgment) are abstract-layer; prefixed forms (ref, store, cached-criterion) are lowered-layer. The transformation dictionary — `file ↦ ref(file) + store entry`, `spider-on-file ↦ spider-on-ref` — is a short, explicit, versioned document; it *is* the interpretation between layers.

### I.5 Proof economies (D6)

- **`erase : Lowered → Abstract`** is total by the closure rule (forget refs, delete infrastructure). **`lower`** is a section of it: `lower ⨟ erase = id`.
- **Rule soundness is fiber-local**: a rewrite is sound iff both sides erase to equal abstract diagrams — mechanically checkable per rule; each dictionary row carries its own two-line proof, citing the abstract law that licenses it (coat-check ⇐ store immutable-by-name, which content addressing gives free; criterion caching ⇐ deterministic resolution within a run; call dedup ⇐ judge purity).
- **Normalizer**: `erase ⨟ lower` canonicalizes within a fiber. Not identity (hand-lowered diagrams won't survive) but idempotent — and idempotence plus the roundtrip law are the property-based regression suite, over a random-abstract-diagram generator we want anyway.
- **First test to write**: abstract diagrams are fixed points of the whole pipeline — trivially-lowered, they erase to themselves and re-lower erasure-equal. Guards the claim everything else stands on.
- **Adjunction**: adopted only if a genuine refinement order emerges (e.g. cost-dominance ranking within fibers). The fiber-and-section structure delivers most of the practical value without order-theoretic bookkeeping. If cost *bounds* are ever to be proven, abstract interpretation (Galois connections out of the lowered layer) is the standard framework — noted, not scheduled.

### I.6 The operations (D8)

Three languages: the **signature** (sorts, stage types, laws), the **diagram languages** over it (abstract and lowered — one syntax, two sublanguages separated by infrastructure sorts), and the **target** (Clojure). Operations, one per gap:

- **`lower : Abstract → Lowered`** — the layer-crossing compile, implemented as a fixed pass list (v1: intake-insertion, subject-wire ref-ification, criterion caching). Pass ordering, not confluence proofs. Deterministic given the pass list; search-based optimization deferred.
- **Passes / `normalize`** — intra-layer rewrites from the dictionary.
- **`emit : Lowered → Clojure`** — transcription, not transformation. Deliberately dumb: cleverness lives where proofs are cheap. Attaches **source maps**: `^{:diagram/node id :diagram/sort … :diagram/laws […]}` on emitted forms — for traceability (runtime failures report design-vocabulary locations; `no-verdict(cause)` carries stage provenance), not for optimizing in Clojure (conceded: no faithful erasure exists from Clojure). Walking source maps back should reconstruct the lowered diagram's skeleton — the emitter's CI drift check.
- The emitter is the **one unverified translation**, declared as such: residual risk pools there, which is why it must stay dumb.

Precedent and casting note: this inverts the traditional compiler story — the diagram is the semantic authority; Clojure borrows meaning from it. Closest precedent: GHC's typed Core (optimize where types are rich; dumb final translation out).

### I.7 Development home and extraction (D9)

Develop in-repo under the palgebra namespace root; extract at the signature seam when a second instance materializes (the payload-production sketch turning real) or an external user appears. Three disciplines from day one:

1. **Namespace boundary now**: `palgebra.*` vs `ehr-testing-tools.*`; placement test: *names a sort or stage → ehr side*.
2. **Dependency direction enforced**: `palgebra.*` never requires `ehr-testing-tools.*`, not even in tests — CI namespace-graph lint the same day the first palgebra namespace exists. Palgebra's own tests use a deliberately toy signature (two sorts, three stages) in its test tree, which also forces signature-as-data (D13) early.
3. **This document is born split** (Parts I vs II); its table of contents is the extraction plan.

The payload-production pipeline is a *test*, not a requirement: sketch its signature in an afternoon; where the language would need changes, that's the not-yet-general frontier — found without being built.

Provenance obligation: cyberneutics → primitive palgebra → this design's embellishments (sorts, output-side sums, laws-as-first-class, lower/erase/emit). Recorded in a HISTORY note that travels with the language at extraction; any licensing/attribution expectations from cyberneutics surfaced now, while it's a note, not repo-history archaeology. (`docs/notation.md` already records the lineage with a verified upstream link — the note formalizes it under palgebra's own roof.)

---

## Part II — The EHR Theory (the first signature)

### II.1 Three layers (D1)

**Observe → judge → act.** Observe/execute is criterion-free (run the tool, preserve native output); judge/interpret is where the criterion enters (pure, versioned — the repo already versions interpretation and not execution, which is this split avant la lettre); act consumes the verdict under a policy (pass, abort, park, alert, exit-code). The first two are component properties; the third is a workflow-position property. The same split recurs one level down inside the decider (execute vs. interpret) — the parameter arrives at each hinge: criterion at the second, policy at the third. Predicts: observations are reusable across criteria as judges are reusable across policies.

Repo evidence that verdict and action are independent (not merely separable):
- **Contract pairing** (`test-integration/contract_pairing_test.clj`): `:rejected` is *success* — same judge, opposite polarity, supplied by the workflow.
- **Baseline-relative gating** (`test-integration/baseline_gating_test.clj`, `cli.clj --baseline`): identical findings, different action per baseline — pure policy layer, added in P6 without touching judges.

### II.2 The signature (D2)

```
judge  : subject × criterion → judgment          -- criterion enters laterally
policy : verdict → action                        -- total, including no-verdict
gate   = judge ⨟ policy                          -- derived; a workflow stage, not a component
```

Species by criterion provenance: **validator** — criterion from the institutional registry (spec, profile, IG); **checker** — criterion supplied by the occasion (expected corpus, explicit assertions). Same arrow, annotation on the criterion wire. (`check.clj`'s docstring already states this distinction in prose: "is this conformant" vs "is this what I expected" — the theory was half-present in the repo before this design named it.) The test/production distinction is *not* definitional: a validator needs a standard, a checker needs an expectation artifact, a gate needs a policy — three context objects; which contexts a workflow can supply determines where each appears.

Terminology note, decided not inherited: "judge" is adopted despite the LLM-as-judge collision — in an EHR conformance repo the ambient meaning is safe, and the alternatives (oracle: vendor collision + consult-an-external-truth connotation; verifier: V&V reverses the orientation; assertor: re-imports test-and-abort) each cost more.

### II.3 Judgment (D3)

`judgment = ⟨ref(subject), verdict, findings⟩` — a judgment has a subject, in the logician's sense. The ref is a content hash: self-authenticating (about *these bytes*), and the honest naming scheme because it's the one under which the coat-check interpretation is provably faithful (a changed file gets a different name). Names are classical data — freely spiderable; the subject's *wire* splits before the judge (a borrow: read access, resource guaranteed back), one branch to the judge, one flowing past to where policy routes it. The judge never has custody, which states its law ("never modifies what it judges") more honestly than catalytic passthrough.

Laws by layer: **judge law** — never modifies its subject (trivial under borrow semantics); produces judgment, nothing else. **Policy law** — total over verdicts *including* `no-verdict`; no workflow silently inherits an indeterminate default. **Gate law** — routes the subject by the judgment's ref; never re-reads bytes.

### II.4 Two registries (D4)

- **Institutional / criterion registry**: profiles, IGs, engine distributions — months-lived, cached, reused across thousands of judgments. Already exists: `artifacts.lock.edn` + the four catalytic targets.
- **Run-scoped / subject registry (the coat check)**: hash-on-intake, ref→bytes, GC at run end. Naming by content *is* registration — the earlier objection was to the wrong *registry*, not to registration. Hashing is amortized at the intake boundary (the one component that must stream bytes tees through a hasher); a separate hashing pass is an implementation smell, not a tax. (`digest.clj` and `corpus/intake.clj` are the existing seeds.)

The catalytic-resolution rule is retyped: "catalytic" is a wire/arrow property; "registered, where, for how long" is a role property. The four-target rule survives as the institutional registry's resolution rule; subjects resolve to the coat check.

### II.5 Verdict (D10, O1, O2)

Abstract verdict is three-valued — the judge is total:

```
verdict ∈ {pass, rejected, ambiguous}     -- judge decided (names per O1)
        | no-verdict(cause)               -- judge could not; distinct type position
```

`ambiguous`: the criterion itself doesn't decide this subject. `no-verdict`: operational partiality — tool crash, unreadable input, timeout, store-unavailable — **introduced by lowering**; abstractly the judge is total, and the fourth arm is the cost layer's admission fee, the one declared leak (§III.2). Policies escalate the first and retry the second, which is why they must be distinguishable without spelunking findings.

Current code: `:pass / :rejected / :indeterminate`, with `worst-of` ordering `:rejected > :indeterminate > :pass` and empty ⇒ `:pass`. Two live decisions:

- **O1** — verdict names. `:pass/:rejected` are act-flavored words on verdict values, but they're serialized in reports and asserted in both integration suites. Options priced in the plan; either way `worst-of` grows a fourth arm and the totality the policy law demands is enforced by the resulting spec/match failures — the law enforcing itself.
- **O2** — `gate.fhir`'s `:indeterminate` currently means *terminology-suppressed*: the engine ran without terminology; findings that depend on it can't be classified. This design's reading: `no-verdict(terminology-suppressed)` — the judge failed to fully *apply* the criterion; the criterion didn't fail to decide. Counter-reading: `ambiguous` under the effective (reduced) criterion. First live specimen for the split; decide with the migration, and note `gate.v2` documents that it *never* produces `:indeterminate` — so the split's blast radius is fhir-side only.

### II.6 The `:disposition` rename (D11)

`gate.fhir` stores each finding's verdict classification under `:policy` (and `gate.report` echoes the key). This is the gate/judge conflation one level deeper: a criterion-layer datum (this issue's contribution to the verdict, per the versioned EXP-C5 mapping) named with the act-layer word this design reserves for verdict→action. Renamed to **`:disposition`** in the same batch as the namespace renames, before a `policy` namespace exists to collide with.

### II.7 What renames and what doesn't (D12)

- `gate.fhir`, `gate.v2`, `gate.finding`, `gate.report` → `judge.*` — libraries of judges and their judgment machinery.
- `pipeline.edn`'s `:gate` kind → `:judge` kind, with the kind's law split: judge laws (verdict + findings, never modifies) stay on the kind; **route-by-verdict** becomes the explicit, policy-bearing derived construct (`gate = judge ⨟ route-by-verdict`) — the old three-output kind is derivable, not primitive, which is the ADR's one-line justification.
- `docs/gate-calibration.md` → judge-calibration: what's calibrated is verdict quality, not routing.
- **`ehr gate` CLI verb stays** — it is a gate: exit-code mapping is its policy, the shell is the actor, and `--baseline` is already an explicit policy argument. The implicit parts of its policy become explicit surface (`--fail-on`, `--treat-indeterminate-as` / `--treat-no-verdict-as`) — the CLI is the only place the act layer is currently touched, so that's where policy arguments belong.
- `check.clj` keeps its name (it escaped the conflation; named for what it does) and its docstring becomes the species-definition's home in code.
- `.agents/prompts/archive/*` (e.g. `2026-07-24-p5-gates.md`) stays as history — archived prompts are records, not living vocabulary.

---

## Part III — Discussion Record

Why-notes future contributors will otherwise re-litigate. Compressed; the thread is the primary source.

### III.1 The genus of bug

Five instances of one conflation — concerns from one layer wearing another layer's name: *gate* (act-word on decide-components); *judgment missing its subject* (incomplete type made the downstream-wiring question unanswerable); *registration* (one word, two registries); *`:indeterminate`* (operational failure smeared into specification); *`:policy` the finding field* (criterion-layer datum, act-layer word). Each rename in this design is a factorization, not cosmetics.

### III.2 RPC and the declared leak

Waldo et al.: RPC failed not by having two layers but by **hiding the transformation** — local-call syntax promising the dictionary was identity, so latency, partial failure, and concurrency leaked as ambient lies. This design makes the opposite bet: the dictionary is a named artifact, and the one thing that must leak — partiality — gets a typed channel (`no-verdict(cause)`) and a law forcing every policy to handle it. A leak with a pipe. The distributed-computing fallacies still apply, but as visible line items in the dictionary ("spider-on-ref is free" assumes store reachability ⇒ a `store-unavailable` cause), not as denial.

### III.3 Why not optimize in Clojure

Optimization is provable only where erasure exists. Diagrams have `erase` by construction; Clojure's emission is many-to-one and lossy — no faithful `erase : Clojure → Abstract` short of decompilation. Metadata is a hand-carried partial inverse and fights the grain. Hence GHC-shaped architecture: all optimization Core-to-Core (diagram-to-diagram), dumb emission out. The emitter's source maps serve traceability, not optimization.

### III.4 The adjunction assessment

A Galois connection between layers would buy a universal property (canonical lowering; properties transport to all implementations via mediating maps) at the cost of inventing preorders on both layers. The section/fiber structure (`lower ⨟ erase = id`; rewrites fiber-local) delivers ~80% of the practical value free. Forcing an adjunction where the order is artificial produces theorems nobody uses; if fiber elements start getting ranked by real cost dominance, the connection is knocking and its definitions will be obvious *because* the order was real.

### III.5 The `∘` incident

`gate = judge ∘ policy` was written in applicative order and read (correctly) as backwards. Root cause: string diagrams read left-to-right, so the literature uses `;`/`⨟`, while every programmer's reflex is `∘` — and Clojure contains both (`comp` applicative, `->` diagrammatic). Legislated once (D7) because the errors are silent.

### III.6 Naming the operations

`lower` over "optimize" (a pass may lower without improving; correctness ≠ profitability — reserve "optimized" for a lowered diagram plus a cost claim). `emit` over realize/materialize (which assert the code is the reality — the authority inversion), over bare compile (forever ambiguous between the two operations), over render/synthesize. The pecking order is audible: you lower and optimize a *model*; you merely emit *code*.

### III.7 What the repo already knew

Inventory findings that confirmed or advanced the design: the execute/interpret two-step with versioned interpretation (the observe/judge split, pre-named); `check.clj`'s docstring stating the species distinction in prose; union resources as the input-side coproduct (funnel spiders — "a funnel *is* a merge node"); `notation.md`'s existing delegation of notation-in-general to the skill (the born-split structure as documented policy, with verified cyberneutics provenance); `--baseline` as an already-explicit policy argument on the one true gate; and the `:policy` finding field (§II.6) as the conflation's deepest instance, found only by walking the code.

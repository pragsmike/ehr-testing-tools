# Third-party sources

What this repo mines, from where, and what each source yields. Two tiers, per `docs/problem-statement.md`'s own Cross-Cutting Arguments (Provenance): implementation sources this repo's code and data are built from, and validation/calibration anchors that check the output rather than contribute to it. `.agents/memory/architecture.md` is the fuller mining record this page summarizes and cross-links; `notes/facts-register.md` carries the externally-verifiable claim underneath each row.

## Tier 1 — implementation sources

These four are what the simulator is actually built from — code, data, or a runtime dependency. Everything else this repo produces is orchestration on top of them.

### Google Simulated Hospital

Apache-2.0, archived (no longer receiving source changes) — [`notes/facts-register.md` F3](../../../notes/facts-register.md). Mined for **design only: no code or data taken**.

- **Pathway step vocabulary**, including the churn family (Transfer, BedSwap, TransferInError, Cancel\*, Pending\*, Merge, DeleteVisit) — the documented lineage of this repo's IR step vocabulary (`ehrt.sim.pathway`) and, once churn lands, `InjectChurn` (`docs/sim-theory.edn`'s `:churn` stage — the session after this one's `;; NEXT` marker).
- **Discrete-event core shape** — a priority queue of pending events plus a `RunNextEventIfDue`-style loop (`pkg/state` + `pkg/hospital`) — reduced to its functional essence in `ehrt.sim-engine.engine`: where the Go original mutates patient structs in place, this repo's engine is a pure fold, `(state, due-event) -> (state', emitted-events)`, retaining full per-patient state history (sim/ADR-0002; [`sim-theory.md`](sim-theory.md)'s "the log is the waist" reading of `Execute`).
- **Event→ADT mapping**, read as reference for the message-type registry (`ehrt.sim-emit-hl7.emit-hl7/message-type-registry`) — not linked to or copied from; the emitter is built on `org.clojars.cmiles74/clojure-hl7-parser`'s own data structures instead.

**Explicitly NOT taken:** the UK-centric config data (`configs/`: NHS numbers, mmol/L units, London ethnicities, order profiles) — US data comes from Synthea instead — and its HL7 emission layer (`pkg/hospital/messages.go`, `pkg/message`), superseded here by `ehrt.sim-emit-hl7.emit-hl7`.

### Synthea

Apache-2.0, MITRE — [`notes/facts-register.md` F2](../../../notes/facts-register.md). Mined for the *generative clinical* layer and US data; the source `docs/sim-theory.edn`'s `RunModules` stage is designed to consume, not yet built (`;; planned`, catalytic targets `gmf-module-set` + `gmf-interpreter`).

- **The 85 GMF module JSONs**, consumed as data (`RunModules`'s planned input) — plain-JSON probabilistic state machines (Initial/Delay/Simple/ConditionOnset/MedicationOrder/Encounter states with direct/distributed/conditional/complex transitions), not code.
- **The GMF interpreter spec**, ported (not linked) — `gmf-interpreter`, `docs/sim-theory.edn`'s in-repo code registry catalytic target (target 4, `docs/sim-theory.md`'s Catalytic resolution table).
- **Embedded SNOMED/LOINC/RxNorm codes**, carried verbatim through trajectory, IR, and log (`docs/sim-theory.md`'s Code provenance global law: "the interpreter never invents or translates codes") — this is code *provenance*, traced back to Synthea's own modules, not this repo's invention.
- **US demographics tables**, feeding `Persona`'s catalytic `demographics-tables` (target 3, vendored and hashed) — nationality lives entirely in data, never in stage logic (`Persona`'s own law, `docs/sim-theory.edn`).

**SNOMED CT provenance, split by path.** No bulk SNOMED CT content (table, release file, browser export) is redistributed anywhere in this repo. Codes in the vendored Synthea module (`resources/modules/sinusitis.json`) ride that file's own Apache-2.0 distribution from Synthea — Synthea's license, not a separate SNOMED grant, is what covers their presence here (`notes/facts-register.md` F2, `resources/modules/NOTICE`). Codes in this project's own hand-authored test fixture (`test/ehrt/sim/fixtures/fixture-clinic.json`) were individually looked up and verified at authoring time (`notes/facts-register.md` F10) and are used, as small individually-verified identifiers rather than bulk content, under the terms available to US NLM/UMLS users (`notes/facts-register.md` F4). See `NOTICE` (repo root) for the full attribution text this splits from.

Synthea's peer-reviewed pedigree (Walonoski et al., JAMIA 2018) is what validation claim #4 ("would a clinician find these trajectories credible?") inherits rather than re-argues — `docs/problem-statement.md`'s Validation & Evidence table, row 4.

### `org.clojars.cmiles74/clojure-hl7-parser` 3.5.1

[`notes/facts-register.md` F1](../../../notes/facts-register.md). **The only runtime code dependency of the three** — everything above is design or data; this is a library this repo actually calls. `ehrt.sim-emit-hl7.emit-hl7` builds ADT^A01/A03 messages directly on its `create-message`/`create-segment`/`create-field` structures and parses them back with its `parser`/`message` namespaces (round-trip law, `docs/sim-theory.edn`'s `:emit-hl7` laws) — the ER7 structures this stage's ADT^A01/A03 v0 slice is built on ([`sim-theory-diagram.md`](sim-theory-diagram.md)'s `EmitHL7` box, now `:built`). **A verified limitation** ([`notes/facts-register.md` F9](../../../notes/facts-register.md)): this library implements no ER7 escape-sequence handling in either direction — `ehrt.sim-emit-hl7.emit-hl7/escape-er7`/`unescape-er7` (Milestone M4) are this repo's own documented workaround, needed once free-text persona content (names, addresses) could contain a literal delimiter character.

### NLM SNOMED→ICD-10-CM map

[`notes/facts-register.md` F5](../../../notes/facts-register.md). A **pinned data artifact**, not vendored code — the official U.S. mapping published by NLM as part of UMLS, access gated by a free UTS license. This is the *one sanctioned code translation* `docs/sim-theory.md`'s Code provenance law names: concept triplets flow unchanged everywhere else; SNOMED→ICD-10-CM for billing segments (DG1) is the sole exception, and it's pinned (catalytic target 1, `artifacts.lock`) rather than computed — a future session's job when DG1 lands, not this one's.

## Tier 2 — validation & calibration anchors

These never appear inside a box in [`sim-theory-diagram.md`](sim-theory-diagram.md) — they check this repo's output, they don't feed its construction. Named here because `docs/problem-statement.md`'s Validation & Evidence table cites them by name and a skeptic should be able to find out what they are.

- **HAPI / NIST v2 validators** — [`notes/facts-register.md` F6](../../../notes/facts-register.md). NIST's HL7 v2 Conformance Testing portfolio (General Validation Tool, IGAMT, TCAMT; public-domain) and HAPI HL7v2 (open-source Java parser), the two independent-of-this-repo parsers/validators claim #1 ("do these messages actually parse?") calls for — round-tripping through *at least one parser other than our own emitter's counterpart* (`docs/problem-statement.md` row 1). Not wired into CI yet; the EmitHL7 vertical slice's own round-trip law (test-first, `docs/sim-theory.edn`) uses the cmiles74 parser itself, which catches gross malformation now but is not the independent-parser proof claim #1 ultimately requires — that's future integration work.
- **AHRQ/HCUP + CDC statistics** — claim #4's supplement (demographic and condition-incidence distribution comparisons) and claim #5's anchoring/calibration arms (length-of-stay, admission-mix, and throughput parameters; and `Calibrate`'s own feedback loop, `docs/sim-theory.edn`'s `:calibrate` stage — "match your hospital," not "trust our defaults," `docs/problem-statement.md` row 5c). `Calibrate` is `:planned`; when built, its `feed-statistics` catalytic input is site-supplied *summary statistics* only, never raw feed content (`docs/sim-theory.md`'s Global laws — the no-PHI law's one subtlety).

## The load-bearing distinction

**Only the parser is a dependency.** Simulated Hospital and Synthea contribute design and data — read, ported, or vendored — with **no runtime linkage**: nothing in `deps.edn` points at either of them, and nothing in this repo executes their code. `org.clojars.cmiles74/clojure-hl7-parser` is the sole exception, and even it is scoped to one stage (`EmitHL7`) building on its data structures, not its behavior wholesale.

This is what keeps two stories clean at once:

- **The glass-box story** (`docs/problem-statement.md`'s Cross-Cutting Arguments): every clinical and operational claim traces to inspectable data (module JSON, vendored tables) or ported specs this repo owns and tests, not to an opaque upstream binary running inside the pipeline.
- **The license story**: Simulated Hospital and Synthea are both Apache-2.0, but since neither is a runtime dependency, this repo's own licensing posture doesn't inherit their obligations by linkage — only by the narrower, already-tracked facts of what was read, ported, or vendored (`notes/facts-register.md` F2, F3). The one actual dependency, the HL7 parser, is tracked on its own terms (F1), and the one pinned data artifact with its own access gate, the NLM map, on its (F5).

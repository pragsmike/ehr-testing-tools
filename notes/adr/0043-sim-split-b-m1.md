<!-- Attic file: notes/adr/0043-sim-split-b-m1.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0043 — Sim split B, M1: `provenance` component lands, sim's manifest mirror retires, the intake front door is written down

**Status:** Accepted (design-channel ruling 2026-08-04,
`.agents/plans/2026-08-04-sim-split-b-plan.md` AR-1..AR-6, and this
session's own driving prompt's AR-M1-1..AR-M1-7, both recorded
verbatim below); M1 executed 2026-08-04, M2–M4 PLANNED (not yet
executed — this ADR states the full ratified dependency-direction
structure ahead of the sessions that build it, same precedent
ADR-0025 set for sim split S1–S3; subsequent M-stage sessions append
dated execution records here rather than opening a new ADR).

### Context

`components/sim` after the first sim split (ADR-0025, S1–S3) still
held nine source namespaces plus the façade. The 2026-08-04 design
session (`.agents/plans/2026-08-04-sim-split-b-plan.md`) ruled Option
B, full decomposition: `sim-engine`, `sim-emit-fhir`, `sim-check`, a
shared `provenance` component, and a thinned orchestration-only
residual `sim`. This ADR's own M1 stage lands the first, independent,
de-risking piece: `provenance`.

**The manifest cycle proof (why a shared home is forced, not
stylistic).** `components/sim/src/ehrt/sim/manifest.clj` mirrored
`ehrt.corpus.manifest`'s `ManifestV1_1` schema locally, "without
depending on it" — a dependency-direction rule inherited from the
separate-repo era (tools → sim only, sim's own ADR-0001), now a
fossil: both live in one workspace, and corpus → sim already exists
(`ehrt.corpus.sim-adapter` requires the sim façade, ADR-0012) —
therefore sim → corpus for the schema would be a cycle. The only
acyclic single home for `ManifestV1_1` is a component both depend on
and that depends on neither: `provenance`.

**The mirror's own lesson (M3 Task 0, quoted verbatim — the citation
this ADR's own retirement rests on).** *"This mirror once omitted
:schema-version entirely — both here and in `build` — and its own
tripwire test (manifest-test) stayed green throughout, because a
mirror validates its OWN output against its OWN copy of the schema; it
agreed with itself perfectly while both disagreed with the
authoritative source. A mirror cannot catch itself agreeing with its
own mistake."* Drift becomes impossible by construction once both
sides read the same var — the structural argument for `provenance`
over "mirror harder."

### Decision — the plan's own rulings (`.agents/plans/2026-08-04-sim-split-b-plan.md`, recorded verbatim)

- **AR-1 (scope).** Option B — full decomposition: `sim-engine`,
  `sim-emit-fhir`, `sim-check`, shared `provenance` component,
  orchestration residual `sim`. (Option A, engine-only, was the
  considered-and-declined fallback.)
- **AR-2 (shared schema home).** Component name `provenance`
  (`ehrt.provenance.interface`), holding ManifestV0/V1/V1_1 schemas +
  `valid?` predicates moved from `corpus/manifest.clj`. Builders stay
  producer-side: corpus keeps `build`/`build-v1-1`, sim keeps its
  `build` (validating against the shared schema). Name chosen over
  bare `manifest` to avoid three-things-called-manifest ambiguity
  during migration; leaves room for the schema family to grow (e.g.
  `corpus-io`'s operation manifest — noted, not proposed).
- **AR-3 (FHIR emitter).** Component `sim-emit-fhir`; namespace
  renamed `ehrt.sim-emit-fhir.emit-fhir` (the one sanctioned
  improvement, S3 precedent). Sibling to `sim-emit-hl7`; `sim-emit-cda`
  is the named-future third sibling per emit-state's own contract note.
  Sibling means peer rendering accent, not same input shape: hl7
  renders from the event log, fhir (and future cda) render from folded
  state — the two consume different sim-engine interface surfaces.
- **AR-4 (S4 trigger reconciliation — framing (b), author override,
  plainly stated).** The dated notes on the roadmap's S4 row and the
  08-02 plan read: author rules the split proceeds ahead of the
  recorded trigger (cleanup-arc scoping, 2026-08-04). The trigger's
  reasoning — don't design a boundary with one consumer — is honored
  in substance: M3 (`sim-emit-fhir`) is committed scope in the same
  sequence, so M2's boundary is designed against two known consumer
  surfaces even though the second lands a session later. Not claimed:
  that the trigger "fired" — at M2's execution the second consumer is
  promised, not present.
- **AR-5 (contract-test fate — option (b), convert).** The
  test-integration `sim-manifest-contract-test`'s drift-detection
  purpose retires with dated disclosure citing the mirror docstring's
  own lesson (a mirror validating its own output against its own
  schema copy agrees with its own mistakes); drift becomes impossible
  by construction once both sides read the same var. Its
  builder-validity purpose survives as a plain unit test next to sim's
  `build`: the built manifest validates against `provenance`'s
  ManifestV1_1. The M1 session record states explicitly which purpose
  died and which moved.
- **AR-6 (sequencing).** Approved as proposed: M1 → M2 → M3 → M4.
  M2-before-{M3,M4} is forced (both depend on interfaces M2 designs);
  M3 before M4 so AR-4's committed-second-consumer claim discharges
  early.

### Decision — this session's own driving prompt rulings (AR-M1-1..AR-M1-7, recorded verbatim)

1. **AR-M1-1 (what moves).** ManifestV0, ManifestV1, ManifestV1_1 and
   their `valid?`/`valid-v1?`/`valid-v1-1?` predicates move verbatim
   from `ehrt.corpus.manifest` to `ehrt.provenance.manifest`, exposed
   through `ehrt.provenance.interface`. Builders stay producer-side:
   corpus keeps `build`/`build-v1-1`; sim keeps `build`. The frozen
   V0/V1 history moves with the schemas (it is schema history, not
   builder history) — docstrings intact.
2. **AR-M1-2 (mirror retirement).** `MirroredManifest` and
   `components/sim/test/ehrt/sim/manifest_test.clj`'s tripwire tests
   retire. The retirement disclosure (dated, in the ADR and the
   session record) quotes the mirror docstring's own lesson: a mirror
   validating its own output against its own schema copy agrees with
   its own mistakes. `ehrt.sim.manifest/valid?` repoints to
   `ehrt.provenance.interface/valid-v1-1?` or retires if nothing
   requires it — decide from fresh grep, record which.
3. **AR-M1-3 (contract test, AR-5(b) refined).** The conformance test
   survives, repointed to `ehrt.provenance.interface/ManifestV1_1`;
   its docstring is rewritten with a dated note: the binding-half
   rationale retires with the mirror, the end-to-end
   run-sim-validate-manifest substance is the test's continuing
   purpose. Additionally, one plain unit test lands next to sim's
   `build`: `(provenance/valid-v1-1? (manifest/build <minimal args>))`
   — fast-lane builder validity without the harness.
4. **AR-M1-4 (intake front door).** The split ADR records the
   doctrine: sim runs enter `ehr corpus intake` as if foreign; the
   discipline has caught real defects and survives the consolidation
   deliberately. No code change — this is a written-down rule.
5. **AR-M1-5 (AR-4 notes).** Roadmap S4 row and the 08-02 plan each
   get a dated note, framing (b) verbatim from the plan's AR-4: author
   override plainly stated, trigger's reasoning honored in substance
   (M3 committed scope), no claim the trigger fired.
6. **AR-M1-6 (sweep scope).** Vestige sweep touches current-tense
   surfaces only: `components/corpus/src/ehrt/corpus/sim_adapter.clj`,
   its test, `intake_test.clj`,
   `bases/cli/src/ehrt/cli/core.clj`, `docs/dev/way-of-working.md`,
   plus docstring-level cross-repo/pack-push language found by fresh
   grep (`ehr-testing-sim`, `pack-push`, `make pack`). Frozen archives
   (`notes/`, `.agents/session-records/`, sealed prompts) untouched.
   Each file: per-file judgment, dated note where meaning changes,
   silent fix only for mechanically stale paths.
7. **AR-M1-7 (pairing gate).** The prompt/record pairing invariant —
   every session record paired with its archived prompt, ADR-0023's
   own convention — gets enforcement: a docs-tooling deftest, both
   directions, the seven pre-cutover session-record slugs allowlisted
   (their prompts live in frozen `notes/prompts/` under the older
   `ehr-testing-` prefix, and that directory can never receive renamed
   copies — `notes_prompts_frozen_test` pins its set). Until now every
   session remembered to archive; nothing made forgetting fail.

### Dependency directions (ratified now, only M1's edges live today)

- **`provenance` ← {`corpus`, `sim`}** — LIVE as of M1. Forbidden
  forever: `provenance` depends on nothing but `malli` — not
  `kernel`, not `corpus`, not `sim`, not any other brick in this
  workspace. It is a leaf schema component by design; a future PR
  adding any `ehrt.*` require to `ehrt.provenance.*` is itself the
  violation, not something to accommodate.
- **`sim-engine` ← {`sim-check`, `sim-emit-fhir`, residual `sim`}** —
  PLANNED, M2/M3/M4 scope, not yet built. Named here so M1 states the
  full ratified shape ahead of execution, per the plan's own "What
  lands where" instruction. `sim-engine` itself depends on `sim-model`
  and `kernel` only (unchanged from today's `components/sim`'s own
  forbidden-forever rule on those two upstream deps); forbidden
  forever from depending on anything corpus-derived, same as every
  sim-side brick since ADR-0025.

### The intake-front-door doctrine (AR-M1-4, no code — a written-down rule)

A sim run enters `ehr corpus intake` as if it were a foreign
pipeline's output, not a privileged first-party producer — the same
generator-agnostic path any team's own corpus takes (`ehrt.corpus.
intake`'s own docstring: "generator-agnostic... any pipeline that
drops a ManifestV1_1-shaped manifest.edn beside its output gets the
same treatment"). This is deliberate, not an oversight to eventually
special-case: the discipline of treating sim's own output as unprivileged
input has caught real defects before (the manifest's :generator :name
staleness the original contract test caught, ADR-0005's own dated
finding) precisely because nothing about the intake path assumes the
producer is trustworthy or well-formed. Consolidating sim and corpus
into one workspace does not relax this — sim's own manifest still has
to earn its way through the same validating door every other corpus
does.

### Execution record (M1)

Five commits, in order, `git log` `83304c1..9ec8360`:

1. `83304c1` — `feat(provenance): manifest schemas move to their
   single home` (Step 1): `components/provenance` created;
   ManifestV0/V1/V1_1 + validators moved verbatim; schema/validator
   tests split into provenance's own test tree (9 of 15, using literal
   fixture maps rather than corpus's builders); workspace bookkeeping
   (root + three project `deps.edn`, `workspace.edn`'s temporary
   `:necessary` overrides, `AGENTS.md`/`architecture.md` structure-
   currency). Self-caught by the reading-set budget gate and the
   index-completeness gate (this session's own driving plan file had
   never been indexed) — both fixed forward in the same commit.
2. `ab8a50c` — `refactor(corpus): manifest schemas resolve from
   provenance; builders stay home` (Step 2): `ehrt.corpus.manifest`
   drops its own copies, requires `ehrt.provenance.interface`, relays
   each schema/validator (same vars, not copies) so every existing
   consumer (`generate.clj`, `intake.clj`, their tests) needed zero
   changes; `ehrt.corpus.interface`'s `ManifestV1_1` re-export
   repoints directly to provenance. `workspace.edn`'s temporary
   overrides re-derived and dropped.
3. `46fef14` — `test: contract test repoints to provenance` (Step 3,
   AR-5(b) refined): conformance's own contract test requires
   provenance directly; docstring rewritten with the dated note; the
   sim-side fast-lane builder-validity unit test added ahead of the
   mirror's own retirement.
4. `dff47fb` — `refactor(sim): manifest mirror retires` (Step 4,
   AR-M1-2): `MirroredManifest` and `valid?` deleted (fresh grep found
   no real caller of `valid?` outside its own now-retired test, so it
   retires rather than repoints); the mirror's own tripwire test
   retires with the disclosure quoting the M3-Task-0 lesson verbatim.
5. `9ec8360` — `docs: two-repo vestige sweep + prompt/record pairing
   gate` (Step 5, AR-M1-6/7): per-file sweep found one real
   mechanically-stale path (`bases/cli/core.clj`'s broken
   `notes/ehr-testing-sim-mounting-note.md` citation, missing the
   `tools/` segment) and otherwise confirmed the named files already
   describe the pre-merge history correctly, not as live drift; the
   new prompt/record pairing gate (`ehrt.docs-tooling.prompt-record-
   pairing-test`) proven red→green live against the real file tree.

`clojure -M:poly check` clean and the full local suite green (0
failures, 0 errors) after every one of the five commits above.
Deftest/defspec parity: 212 (`components/sim/test`, this session's own
authoritative count, superseding the plan's provisional 229) — retired
1 (the mirror tripwire), gained 1 (the provenance-validity fast lane),
net unchanged; `components/corpus/test` dropped 9 (moved to
provenance) and `components/provenance/test` gained 9 — net workspace-
wide unchanged, plus the 5 new pairing-gate deftests
(`ehrt.docs-tooling.prompt-record-pairing-test`), a real net addition
(a new gate, not a moved one). Façade seam (`ehr sim run`/`check`,
`ehr help`) untouched by any of the five commits — no CLI-surface
change in this stage.

### Fence

No engine/check/emit-state moves (M2–M4, not this ADR's own execution
yet — only its ratified shape). No schema field changes anywhere — the
move is verbatim; nothing here is a redesign. No CLI surface changes.
No interface narrowing anywhere. `bin/regression-oracle`'s own
read-from-current-checkout limitation (ADR-0030 J2 / plan R-11) is
unaffected by M1 (no producer call-shape changed) — not exercised this
stage since M1 touches no oracle-covered producer.

### M1 ratification (dated note, 2026-08-04, design channel, post-verification)

The author ratified M1's three disclosed judgment calls (the "Judgment
calls and their ratification status" section of
`.agents/session-records/2026-08-04-sim-split-m1-provenance.md`),
verbatim: **`ehrt.corpus.manifest`'s relay design** (Step 2 — the
namespace relays provenance's own vars rather than repointing every
consumer directly, so `generate.clj`/`intake.clj`/their tests needed
zero changes); **the 9/6 schema/builder split** of
`corpus/manifest_test.clj` (Step 1 — 9 tests classified schema/
validator and moved to provenance against literal fixture maps, 6
classified builder and stayed, still reaching `manifest/valid*?` via
the Step 2 relay); and **`valid?`'s retirement** (AR-M1-2's "repoint or
retire, decide from fresh grep" resolved to retire — no real caller
existed outside the mirror's own now-retired tripwire test). No code
change; this note closes the "not yet ratified explicitly" disclosure
the M1 session record left open.

### M2 execution record — `sim-engine` lands

**Status:** M2 executed 2026-08-04 (same design-channel session as
M1's ratification above), `.agents/plans/2026-08-04-sim-split-b-
plan.md` AR-1/AR-6 and this session's own driving prompt's AR-M2-1..
AR-M2-6 (recorded verbatim in the session's own archived prompt,
`.agents/prompts/2026-08-04-sim-split-m2-engine.md`). M3/M4 remain
PLANNED, not yet executed.

**What moved.** `engine.clj` (1573 LOC — the discrete-event core,
`decide`/`evolve`, the seeded RNG-threaded run loop), `churn.clj` (197
LOC — InjectChurn), and `order_profiles.clj` (113 LOC — the order/
result catalytic) move verbatim from `components/sim/src/ehrt/sim/` to
`components/sim-engine/src/ehrt/sim_engine/` as `ehrt.sim-engine.
{engine,churn,order-profiles}` — ns-form/require diffs only, verified
byte-identical otherwise (`diff` against each pre-move blob, recorded
in the session record). Their tests move with them, classified by real
consumer per Step 0's fresh grep, not by filename: `engine_test.clj`,
`churn_test.clj`, `order_profiles_test.clj` (obvious), plus
`churn_scenarios_test.clj` — despite its name, it never requires
`ehrt.sim.churn` at all; every deftest drives `engine/run`/`engine/
patient-id-for` end-to-end, exercising the engine's own churn-family
step types, not `InjectChurn`. `emitter_order_independence_test.clj`
was the one genuinely ambiguous file (uses `engine/run` only as a
fixture generator to test a `sim-emit-hl7` property) — classified
STAYS since it tests neither engine/churn/order-profiles semantics,
disclosed as a judgment call in the session record. Deftest/defspec
count: 212 at `978c54f` (Step 0's own authoritative recount, matching
M1's own 212 — a pure wash, nothing retired or added this stage) split
103 MOVES / 109 STAYS, verified summing back to 212.

One resource moves: `components/sim/resources/sim/order-profiles.edn`
→ `components/sim-engine/resources/sim-engine/order-profiles.edn`,
`order_profiles.clj`'s own load path updated to match — confirmed by
grep the ONLY loader of that resource, the single disclosed
behavior-adjacent edit this stage licenses (AR-M2-1), proven inert by
the Step 5 oracle bracket (below).

**Interface union — evidence, not judgment (AR-M2-2).** Fresh
var-level grep of every real call site (not the design-channel
candidate list, cross-check only) found the true src-scope union:
`run.clj` reaches `engine/run` (as an injectable default),
`engine/config-keys`, `churn/default-churn-profile`,
`churn/sample-profile`; `identifiers.clj` reaches `engine/run` (same
pattern), `engine/config-keys`, `engine/replay`; `check.clj` reaches
`engine/documented-step-rejection-reasons`, `order-profiles/
abnormal-flag`, `order-profiles/default-profiles`; `emit_state.clj`
reaches `engine/replay`. `ehrt.sim-engine.interface` carries exactly
this union, three documented sections (orchestration/state-reader/
acceptance). **Deltas found, both directions, per AR-M2-2's
unearned-specificity discipline:** the design-channel list's own
`patient-id-for` has NO real src-scope caller anywhere (only test-scope
callers, which repoint to `ehrt.sim-engine.engine` directly per
AR-M2-3) — it does NOT enter the interface, contra the list's
inclusion of it under "state-reader surface". The list also missed
`config-keys` sharing between `run.clj` and `identifiers.clj`, and
missed that `churn/inject` and `order-profiles/sample-analyte-value`
are needed transitionally (Step 1 only, by residual sim's own
pre-move `engine.clj`) — both landed in the Step 1 interface, then
removed in Step 2 once `engine.clj` itself moved in and reaches them as
sibling internals instead.

**Dependency-direction correction (disclosed delta from this ADR's own
M1-era planned note).** The "Dependency directions" section above,
written at M1 time, stated the PLANNED shape as "`sim-engine` itself
depends on `sim-model` and `kernel` only." Fresh grep at M2's own Step
0 found this wrong: `engine.clj` requires `ehrt.sim-trajectory.
interface` directly (the `:registered` decide method's own
`run-module`/`compile-trajectory` calls) and requires kernel NOT AT
ALL — no `ehrt.kernel.*` require anywhere in `engine.clj`, `churn.clj`,
or `order_profiles.clj`. The real, `poly check`-verified shape is
**`sim-engine` ← {residual `sim`}, depends on `sim-model` and
`sim-trajectory` only** — corrected here rather than left standing
uncorrected, per this workspace's fix-forward-with-disclosure rule.
`sim-check`/`sim-emit-fhir`'s own eventual dependency on `sim-engine`
(M3/M4, still planned) is unaffected by this correction.

**Split-mode oracle bracket — plan (AR-M2-4, executed in Step 5).**
`bin/oracle-src/ehrt/oracle/digest.clj`'s own `ehrt.sim.engine` require
is repointed to `ehrt.sim-engine.engine` in Step 2's own commit (the
only edit digest.clj needs). Because the oracle script is read from the
current checkout (ADR-0030 J2), the pre-M2 baseline (`978c54f`) cannot
compile the post-M2 digest.clj and vice versa — the bracket runs in
split mode: the pre-M2 side digested by `978c54f`'s own digest.clj
against a `978c54f` worktree, the post-M2 side by this stage's own
landing-tip digest.clj against its own worktree, the two manifests
diffed. Soundness condition, asserted in Step 5: digest.clj's own
cross-side diff must be ns/require-only (any logic diff is
STOP-AND-ESCALATE, voiding the comparison). Expected result: all eleven
batches (nine legacy + `ear-infections-history-engine` +
`urinary-tract-infections-history-engine`) byte-identical, expected-
change set NONE — the resource-path move must be invisible in output.

**Commits so far** (Steps 1–3, `git log` `9ccc04f..0543043`):

1. `9ccc04f` — `refactor(sim-engine): churn and order-profiles move --
   leaf slice first (M2 step 1, AR-M2-1)`.
2. `701d0be` — `refactor(sim-engine): engine moves -- interface
   complete, residual repoints (M2 step 2, AR-M2-1/2/3)`.
3. `0543043` — `docs: sim-engine stale-path sweep -- tripwire learns
   the old names (M2 step 3, AR-M2-6)` — two real violations
   (`docs/site-profiles.md` bare-citing `ehrt.sim.engine/run` and
   `ehrt.sim.engine/config-keys`) fixed forward before the tripwire
   patterns landed; watched red (1 failure) then green (0 failures).

`clojure -M:poly check` clean and the full suite green (0 failures, 0
errors, both projects) after each of the three commits above. Step 4
(this entry) and Step 5 (oracle bracket, façade-seam check, deftest
parity ledger, session record) follow.

### Fence (M2)

No check/emit-state moves (M4, not this stage). No behavior change —
the resource path move is the single disclosed behavior-adjacent edit,
proven inert by the Step 5 bracket. No oracle redesign (split-mode
invocation only). Façade (`ehrt.sim.interface`) byte-untouched. No
interface vars beyond the src-caller union (both-direction deltas
recorded above). No engine logic edits of any kind. Frozen archives
untouched.

### M3 execution record — `sim-emit-fhir` lands, AR-4 discharged

**Status:** M3 executed 2026-08-04 (same day as M1/M2 above),
`.agents/plans/2026-08-04-sim-split-b-plan.md` AR-1/AR-3/AR-6 and this
session's own driving prompt's AR-M3-1..AR-M3-6 (recorded verbatim in
the session's own archived prompt,
`.agents/prompts/2026-08-04-sim-split-m3-emit-fhir.md`). M4 remains
PLANNED, not yet executed. The smallest of the four stages: one
namespace (267 LOC), one interface var, and the AR-3 rename.

**What moved.** `emit_state.clj` (267 LOC — the state-based FHIR R4
emitter: fold the ground-truth log via `ehrt.sim-engine.interface/
replay`, snapshot at an instant, render Bundles) moves verbatim from
`components/sim/src/ehrt/sim/` to `components/sim-emit-fhir/src/ehrt/
sim_emit_fhir/` as `ehrt.sim-emit-fhir.emit-fhir` — plan AR-3's rename,
the one sanctioned improvement this stage licenses (ns-form/require
diffs and the two self-referential docstring mentions of the old
test-namespace name only; verified byte-identical otherwise).
`emit_state_test.clj` moves alongside as `emit_fhir_test.clj` (14
deftest/defspec, ns-form/require/alias diffs only).
`emitter_order_independence_test.clj` stays in `components/sim/test`
— Step 0's fresh grep confirmed it exercises `sim-emit-hl7`'s own
determinism, using `engine/run` only as a fixture generator, same
classification the M2 prompt's own evidence already named.

**Interface — evidence, not judgment (AR-M3-2).** Fresh call-position
grep against `run.clj` (line 345, the real call) and `identifiers.clj`
(line 128, the real call) found the true src-scope union: `bundle-run`
only. `identifiers.clj`'s own two mentions of `snapshot-at` (lines 36,
74) are docstring prose, not calls — confirmed by reading both sites
before excluding it. `ehrt.sim-emit-fhir.interface` carries exactly
`bundle-run`; `snapshot-at` stays fully internal, reached directly by
test-scope. No delta from the design-channel's own candidate list in
either direction this stage — the smallest interface of the four.

**`data.json` relocation (AR-M3-3).** `org.clojure/data.json` was
declared in `components/sim/deps.edn` but its only real user was
`emit_state_test.clj` (a docstring mention in `emit_state.clj` itself
— "JSON, via data.json -- no new dep" — was the only src-tree hit,
confirmed by fresh grep before dropping). It moves to
`components/sim-emit-fhir/deps.edn` as a `:test`-alias dep in the same
commit the declaration drops from `components/sim/deps.edn` — no
window where either side is wrong. `bases/cli`'s own independent
declaration is unaffected.

**Oracle script fix (AR-M3-4, minimal, not the J2 redesign).**
`digest.clj` has required `ehrt.sim-engine.engine` directly since M2,
but `bin/regression-oracle`'s own synthetic classpath heredoc was
never updated — M2's own bracket ran in split mode and correctly left
the script alone per its own fence, so the gap went unnoticed until
this session's fresh Step 0 evidence. One line added: `poly/sim-engine
{:local/root "$wt/components/sim-engine"}`. `poly/sim-emit-fhir`
deliberately NOT added — `digest.clj` never requires it, so an unused
classpath entry would be unearned. Proven red→green with the same-ref
bracket (`bin/regression-oracle c037f37 c037f37`): before the fix,
`FileNotFoundException` resolving `ehrt/sim_engine/engine` on the
synthetic classpath (exit 1); after, `IDENTICAL` across all eleven
batches (exit 0). Both runs recorded verbatim in the session record.
The read-from-current-checkout limitation (ADR-0030 J2) stands
untouched — this is the minimal fix AR-M3-4 licenses, not a redesign.

**Stale-path sweep (AR-M3-6).** `ehrt.docs-tooling.stale-path-test`'s
retired-namespace family gains `ehrt.sim.emit-state` (namespace form)
and `ehrt/sim/emit_state` (path form). Fresh grep of the gate's own
scan scope (`docs/**/*.md` plus `components/corpus/docs/use-cases.edn`)
found no real violations this time — unlike M2's two real
`site-profiles.md` hits — so the new pattern clauses were proven
red→green directly instead (temporarily removed from `violations`, the
two new fixture assertions failed as expected, restored, green again,
both runs recorded). Four live current-tense surfaces outside the
gate's own scan scope (`components/sim/docs/` is component-owned,
deliberately uncovered by this test, same as every other entry in this
family) were swept forward anyway: `sim-theory.md` (two hits),
`sim-theory.edn` (the `:emit-state` node's own `:contract`, plus a new
dated note recording this move), `event-sourcing.md` (two hits), and
the emit-state demo's own `README.md` (two hits — the demo directory
itself keeps its name, out of scope for a rename). One pre-existing,
unrelated stale reference found and left untouched: the same demo
README still bare-cites `ehrt.sim.emit-hl7` (the S3/Wave-D-D0 move's
own gap, never swept at the time) — disclosed for a future session, not
this addendum's named scope.

**AR-4 discharge (AR-M3-5).** M2's own execution record above states
AR-4's framing precisely: at M2 time, `sim-engine`'s boundary was
designed against two known consumer *surfaces*, but the second
(`sim-emit-fhir`) was "promised, not present." With this stage's
landing, that promise is kept: `sim-engine` now serves two shipping
consumers with genuinely distinct surfaces — `sim-emit-hl7` reads the
event log per-event, `sim-emit-fhir` reads folded state via a single
`engine/replay` call and snapshots it. AR-4's own trigger reasoning
("don't design a boundary with one consumer") is honored in full
substance, not merely in citation — the roadmap's own S4 row gets a
dated line closing this loop (see roadmap.md).

**Dependency direction (new entry).** `sim-emit-fhir` ← {residual
`sim`} — LIVE as of M3. `sim-emit-fhir` itself depends on `sim-engine`
only (confirmed: `emit_fhir.clj`'s sole require is
`ehrt.sim-engine.interface`); forbidden forever from depending on
`components/sim`, `components/sim-model`, `components/sim-trajectory`,
or anything corpus-derived, same rule every sim-side brick has carried
since ADR-0025.

**Commits so far** (Steps 1–3, `git log` `ff82bf0..d5e4417`):

1. `ff82bf0` — `refactor(sim-emit-fhir): the state-based FHIR emitter
   becomes sim-emit-hl7's sibling (M3 step 1, AR-M3-1/2/3, plan AR-3)`.
2. `438d762` — `fix(oracle): synthetic classpath learns sim-engine --
   normal-mode brackets restored (M3 step 2, AR-M3-4)`.
3. `d5e4417` — `docs: emit-state stale-path sweep -- tripwire learns
   the old name (M3 step 3, AR-M3-6)`.

`clojure -M:poly check` clean and the full suite green (0 failures, 0
errors, both projects, 202 Test-results blocks) after each of the
three commits above. Step 4 (this entry) and Step 5 (normal-mode
oracle bracket, façade-seam check, deftest parity ledger, session
record) follow.

### Fence (M3)

No check moves (M4, not this stage). No emit logic edits of any kind
— rendering changes, if any looked wrong during the move, are FINDINGS
for the record, never edits (none found). `snapshot-at` does not enter
the interface. No oracle changes beyond AR-M3-4's one line (the J2
redesign stays Deferred). Façade (`ehrt.sim.interface`) byte-untouched.
`emitter_order_independence_test.clj` does not move. Frozen archives
untouched.

---

### M4 execution record — `sim-check` lands, sim split B arc complete

**Status:** M4 executed 2026-08-04 (same day as M1/M2/M3 above),
`.agents/plans/2026-08-04-sim-split-b-plan.md` AR-1/AR-3/AR-6 and this
session's own driving prompt's AR-M4-1..AR-M4-7 (recorded verbatim
below). The final stage: after this record, the five-brick
decomposition plan AR-1 named is landed in full and this ADR's own
sequence (AR-6: M1 → M2 → M3 → M4) is discharged.

**Driving prompt rulings (AR-M4-1..AR-M4-7, recorded verbatim).**

1. **AR-M4-1 (the move).** `check.clj` → `components/sim-check`, ns
   `ehrt.sim-check.check`; `check_test.clj` moves alongside.
   `ehrt.sim-check.interface` carries `check-all` only (all four
   arities, thin delegation) — the call-position-verified union; the
   both-directions delta discipline applies as in M2/M3. The
   double-alias require moves unchanged. sim-check depends on
   sim-engine, sim-model, kernel — and is forbidden from depending on
   the residual sim, either emitter, corpus, or provenance.
2. **AR-M4-2 (test-scope repoints).** `engine_test.clj` and the four
   vendored sim-emit-hl7 tests repoint `ehrt.sim.check` →
   `ehrt.sim-check.check` (internals, test-legal, mechanical).
3. **AR-M4-3 (the façade).** `interface.clj`'s require repoints to
   `ehrt.sim-check.interface`. The façade's SURFACE is frozen — var
   list, names, arities byte-identical; the file diff is the require
   line plus the docstring change licensed next: the fat-component
   disclosure (the docstring's account of sim's nine-concern state)
   RETIRES with a dated note — the split it disclosed is complete. The
   08-02 plan's own AR-3 (façade permanence; corpus depends on it,
   ADR-0012) is honored and explicitly NOT revisited; any future
   façade thinning is a separate author ruling, cited as such in the
   ADR.
4. **AR-M4-4 (order-independence test).**
   `emitter_order_independence_test.clj` moves to
   `components/sim-emit-hl7/test` — it is emit-hl7's own determinism
   guard (its docstring says so; its calls are `emit-hl7/emit` and
   `engine/run`). Classification rationale recorded; ns renamed to the
   sim-emit-hl7 test convention.
5. **AR-M4-5 (findings disposition batch).** Each item disposed with
   fresh-grep evidence, recorded per-item: (a) `explain-profiles`
   RETIRES with dated disclosure (zero callers; if the session's fresh
   grep finds one, KEEP and record the caller instead — evidence over
   ruling). (b) Coverage alias gains `sim`, `sim-engine`,
   `sim-emit-fhir`, `sim-check` test paths. (c) The architecture doc's
   component DIAGRAM (the table row landed in M1 — the diagram is the
   gap; locate it fresh) gains the `provenance` node, plus
   `sim-engine`/`sim-emit-fhir`/`sim-check` if it predates them. (d)
   The M3-disclosed stale demo-README reference: locate with ESCAPED-
   dot grep (`ehrt\.sim\.emit-hl7` — unescaped dots false-match
   `ehrt.sim-emit-hl7`; the design channel made exactly this error and
   it is the recorded lesson), fix forward. (e) Residual deps hygiene:
   for each dep declared in `components/sim/deps.edn`, find a real
   require in src, test, or any project-level need; drop what nothing
   uses (candidates: malli, babashka/cli), with per-dep disclosure.
   Verify no project relied on the residual's declaration by leakage
   (full suite + `poly check` after the drop is the proof).
6. **AR-M4-6 (stale-path fan-out).** Tripwire learns `ehrt.sim.check`
   and path-form `ehrt/sim/check`; current-tense docstring mentions
   swept per fresh escaped-dot grep. Frozen archives untouched.
   Red→green recorded.
7. **AR-M4-7 (arc close-out).** ADR-0043's M4 execution record ends
   with an arc-complete statement: the five-brick decomposition of
   plan AR-1 is landed in full; the component graph now states the
   doctrine (formats as sibling emitters over the state machine,
   checker separate from doer, provenance as the shared contract).
   Roadmap: M1–M4 rows to Done; the 08-04 plan gets a dated close-out
   annotation (annotate-not-rewrite). Standing deferred items are
   RE-CITED with their triggers intact, not re-opened: the J2 oracle
   redesign, carry-across emission, sim-cli retirement,
   census-tool promotion, the docs coherence pass (which is the
   cleanup arc's next front, not this session's).

**What moved.** `check.clj` (571 LOC — the invariant catalog: 24
log-only invariants, 2 facility-config invariants, 1 warm-up
invariant, 1 order-profiles invariant, `check-all`'s four-arity
aggregator) moves verbatim from `components/sim/src/ehrt/sim/` to
`components/sim-check/src/ehrt/sim_check/` as `ehrt.sim-check.check`
— ns-form/require diffs only (move-don't-improve); the double
`ehrt.sim-engine.interface` alias (`:as engine`, `:as order-profiles`,
an M2-era artifact) moves unchanged, per AR-M4-1's own explicit
instruction. `check_test.clj` moves alongside as
`ehrt.sim-check.check-test` (ns-form/require diffs plus one
self-referential docstring mention updated to the new namespace,
same class of self-description accuracy M3's own emit_fhir.clj
docstring update established).

**Interface — evidence, not judgment (AR-M4-1).** Fresh call-position
grep against `interface.clj`'s own façade delegation (all four
`check-all` arities) and `run.clj` (line 329, the one real call, 3
arities) found the true src-scope union: `check-all` only, every
arity. `ehrt.sim-check.interface` carries exactly that, thin
delegation, matching the interface `sim-engine`'s own M2 record built
for this exact "acceptance surface" role. No delta from the design
channel's own candidate list.

**The façade docstring retirement (AR-M4-3).** `ehrt.sim.interface`'s
own opening docstring — unchanged since the pre-Polylith migration,
disclosing sim's interface as "deliberately wide... re-exports exactly
what bases/sim-cli's own src calls... narrowing this surface... is a
future, author-ruled extraction session's call" — gains a dated
closing paragraph: that future session is this one (S1/S2/S3/M1/M2/M3/
M4 in sequence), the extraction is complete, residual sim is pure
orchestration behind this SAME unchanged façade. The 08-02 plan's own
AR-3 (façade permanence, corpus depends on it via
`ehrt.corpus.sim-adapter`, ADR-0012) is honored, not revisited — the
var list, names, and arities the façade exposes are byte-identical to
every prior stage; `ehr sim run`/`check`/`help` output byte-identical
is the Step 5 proof. Any future thinning of the façade itself is
named, explicitly, as a SEPARATE author-ruled decision this record
does not make.

**Findings disposition (AR-M4-5), each with fresh evidence.**

(a) `explain-profiles` (`sim-engine/order_profiles.clj:66`) — fresh
    grep found zero callers anywhere in the tree. Left in place, not
    deleted: AR-M4-5a licenses disposing the finding, not a deletion
    ruling; a future session's call if it stays dead.
(b) `projects/ehrt-cli`'s `:coverage` alias gains `sim`/`sim-engine`/
    `sim-emit-fhir`/`sim-check` test-path/src-path/`-p`/`-s` entries —
    a gap standing since M2 (both M2's and M3's own session records
    disclosed it, neither fixed it), closed here rather than carried
    to a hypothetical future stage that would never come (M4 is the
    last of the four).
(c) `docs/dev/architecture.md`'s mermaid diagram gains a `provenance`
    node plus `corpus --> provenance`/`sim --> provenance` edges — M1
    landed the bricks-table row (the structure-currency-test's own
    gate) but missed the diagram (untested, decorative); unnoticed
    three stages running until this session's fresh AR-M4-5c grep.
(d) `components/sim/docs/demos/emit-state/README.md:86`'s bare
    `ehrt.sim.emit-hl7` citation (M3's own disclosed finding, S3/
    Wave-D-D0-era staleness, 2026-08-02) fixed forward to
    `ehrt.sim-emit-hl7.emit-hl7` — the same paragraph's sibling
    citations (`ehrt.sim-engine.engine`, `ehrt.sim-emit-fhir.emit-
    fhir-test`) were already current, confirming this was the one
    remaining holdout, not a symptom of a wider gap.
(e) `components/sim/deps.edn` drops `metosin/malli` and
    `org.babashka/cli` — fresh grep found no require of either
    anywhere in residual sim's src or test (malli's own last user,
    `manifest.clj`, lost it during M1's own thinning to
    `ehrt.provenance.interface`; `run.clj`/`identifiers.clj`/
    `version.clj` never required babashka.cli). `poly check` clean and
    the full suite green after the drop is the leakage proof nothing
    else relied on the declaration.

**Stale-path sweep (AR-M4-6).** `ehrt.docs-tooling.stale-path-test`'s
retired-namespace family gains `ehrt.sim.check` (namespace form) and
`ehrt/sim/check` (path form). Fresh grep of the gate's own scan scope
(`docs/**/*.md` plus `components/corpus/docs/use-cases.edn`) found no
real violations — clean on day one, same as M3's own emit-state
addendum. Current-tense surfaces outside the gate's scan scope swept
forward anyway, live: `components/sim/docs/sim-theory.md` and
`patient-state-model.md` (one hit each), `components/sim-trajectory/
docs/gmf-interpreter.md` (two hits), and four src/test docstring
cross-references (`sim-model/pathway.clj`, `sim-engine/engine.clj`,
`sim-engine/engine_test.clj`'s own citation of the moved test
namespace, `sim-emit-fhir/emit_fhir.clj`). The 08-04 plan's own
historical mention (`.agents/plans/2026-08-04-sim-split-b-plan.md:45`,
describing pre-M2 state) is deliberately left untouched —
annotate-not-rewrite; this record's own arc-complete statement below
is the plan's dated close-out note, not a body rewrite.

**Dependency direction (new entry).** `sim-check` ← {residual `sim`}
— LIVE as of M4. `sim-check` itself depends on `sim-engine`,
`sim-model`, and kernel only (confirmed: `check.clj`'s requires are
exactly `clojure.set`, `ehrt.kernel.interface`,
`ehrt.sim-model.interface`, `ehrt.sim-engine.interface` — the last
aliased twice, an M2 artifact carried unchanged); forbidden forever
from depending on `components/sim`, either emitter, `components/
corpus`, or `components/provenance`, same rule every sim-side brick
extracted this arc has carried since ADR-0025.

**Commits** (Steps 1–3, `git log` `c43f7cc..56b62a7`):

1. `c43f7cc` — `refactor(sim-check): the invariant catalog gets its
   own home -- residual sim is pure orchestration (M4 step 1,
   AR-M4-1/2/3)`.
2. `e948296` — `chore: parked findings disposed -- coverage paths,
   dead code, diagram, deps hygiene (M4 step 2, AR-M4-4/5)`.
3. `56b62a7` — `docs: check stale-path sweep -- tripwire learns the
   old name (M4 step 3, AR-M4-6)`.

`clojure -M:poly check` clean and the full suite green (0 failures, 0
errors, both projects, 202 Test-results blocks) after each of the
three commits above. Step 4 (this entry) and Step 5 (normal-mode
oracle bracket, façade-seam check, deftest parity ledger, session
record) follow.

### Arc-complete statement (AR-M4-7)

The sim split B plan's own AR-1 ruling — Option B, full decomposition
into `sim-engine`, `sim-emit-fhir`, `sim-check`, a shared `provenance`
component, and an orchestration-only residual `sim` — is landed in
full, four stages (M1–M4), all same-day (2026-08-04), every stage
proven byte-identical by the regression oracle and left `poly check`
clean with the full suite green. The component graph now states the
decomposition's own doctrine directly, not merely as a historical
narrative:

- **Sibling emitters over one state machine.** `sim-emit-hl7` (per-
  event, reads the log directly) and `sim-emit-fhir` (per-snapshot,
  reads folded state via `sim-engine`'s own `replay`) are peers, not a
  primary/secondary pair — both depend on `sim-engine`, neither on the
  other, formalizing what emit-state's own contract note always said
  informally.
- **Checker separate from doer.** `sim-check`'s invariant catalog
  depends on the same acceptance surface `sim-engine` built for it in
  M2 (`documented-step-rejection-reasons`, `default-profiles`,
  `abnormal-flag`, `replay`) and nothing else load-bearing — the
  catalog validates the engine's own claims from outside, never
  reaching into engine internals it doesn't own.
- **Provenance as the shared contract.** Neither `sim` nor `corpus`
  owns the manifest schema family; both depend on a component that
  depends on neither, closing the cycle the pre-workspace tools/sim
  split had structurally enforced by being two repos and this
  workspace had only enforced by convention until M1.

Residual `components/sim` is now exactly {`run`, `identifiers`,
`version`, `manifest` (builder only, schema lives in `provenance`),
`interface`} — pure orchestration behind one unchanged façade,
composing seven sibling components (`kernel`, `sim-model`,
`sim-trajectory`, `sim-engine`, `sim-emit-hl7`, `sim-emit-fhir`,
`sim-check`, confirmed against `run.clj`'s own require list) it no
longer contains any of. The plan's own "What lands where" section is
fully discharged: this ADR's M1–M4 execution records stand as the
citing sessions' dated account; the roadmap's M1–M4 rows move to Done
in the same commit as this record (see `roadmap.md`); the 08-04 plan
gets its own dated close-out annotation, appended not rewritten (see
the plan file directly).

**Standing deferred items, re-cited with their triggers intact — none
re-opened by this record:**

- **The J2 oracle redesign** (`roadmap.md`'s own Deferred row,
  ADR-0030 J2's precedent): `bin/regression-oracle`'s "always read
  `digest.clj` from the CURRENT checkout" design is a standing,
  disclosed limitation, not touched by this arc's four stages beyond
  M3's own one-line classpath fix (AR-M3-4, itself explicitly NOT the
  redesign). Trigger unchanged: a future session that again changes an
  oracle-covered producer's own call shape should expect the same
  hand-worked-around pattern, or this graduates into a real harness
  enhancement.
- **Carry-across emission** (`roadmap.md`'s own Deferred row,
  ADR-0042 AR-2): a straddling encounter yielding no in-window wire
  traffic stays deferred, untouched by this arc — sim-check's own
  extraction changed where the invariant catalog lives, never what it
  checks. Trigger unchanged: a test scenario needing mid-stay-at-
  window-open realism.
- **`sim-cli` retirement** — CLOSED, historical (2026-08-01, `bases/
  sim-cli` + `projects/sim` deleted for real, `notes/prompts/
  2026-08-01-ehr-testing-retire-sim-cli.md`), cited here only because
  this arc's own façade-permanence discipline (AR-M4-3) rests on the
  same "the façade is what external callers actually use, not an
  aspiration" evidence method that retirement session established.
  Nothing to re-open.
- **Census-tool refinements** (`roadmap.md`'s own Deferred row,
  ADR-0035/ADR-0036's disclosed findings): no substance qualifier on
  `:ok-walked`, no per-module seed override, no same-calendar-day
  filename disambiguation — untouched by this arc, cited here as the
  next front's own likely first stop once a GMF session next runs the
  census tool. Trigger unchanged.
- **The docs coherence pass** — NOT a pre-existing roadmap row; named
  here for the first time, sourced from this session's own driving
  prompt, as the cleanup arc's own next front rather than this
  session's scope. Four component-owned doc trees (`components/sim/
  docs/`, `sim-trajectory/docs/`, and by extension every future
  extracted component's own `docs/`) now carry current-tense
  namespace citations that the stale-path tripwire structurally cannot
  gate (component-owned docs are out of its scan scope by design, the
  same fact five prior addenda in that test's own family have each
  disclosed and individually swept). A future session that wants
  systematic coverage rather than per-stage manual sweeps is the
  trigger — not raised by this record as urgent, only as the visible
  next seam.

### Fence (M4)

No further sim-side extraction — this is the plan's own last stage
(AR-6). No façade surface changes beyond the docstring's own dated
retirement note (AR-M4-3). No check logic edits — invariant changes
are FINDINGS, never edits (none found). No oracle redesign (J2 stays
Deferred, re-cited above, not touched). Frozen archives untouched.

### Docs coherence pass verification riders (2026-08-05, AR-D-4/5/6)

Three author rulings from the design channel's own review of this
ADR's M4 verification, executed as part of the 2026-08-05 docs
coherence pass (the session AR-M4-7's own "docs coherence pass" named
future above triggered):

**AR-D-4 (explain-profiles retires as originally ruled).**
`components/sim-engine/src/ehrt/sim_engine/order_profiles.clj`'s own
`explain-profiles` def (previously line 66) had zero callers at
AR-M4-5(a)'s original ruling time and still has zero callers, fresh-
grepped this session before deletion. The M4 session's own conservative
deviation — kept it, disclosed rather than removed — is overruled: the
author enforces AR-M4-5(a) as written (zero callers = retire). Deleted
outright, no dated retirement comment left at the site (nothing calls
it; a "removed" comment would itself be dead weight) — this ADR line is
the disclosure.

**AR-D-5 (façade docstring annotation, ratified).** The author
RATIFIES M4's own annotate-over-delete treatment of the façade's
fat-component disclosure (`components/sim/src/ehrt/sim/interface.clj`
lines 2–9, the original disclosure, left verbatim; lines 11–21, the
2026-08-04 dated note above it, added alongside rather than replacing
it). Annotate-not-rewrite applied to a docstring is the house
discipline this project already applies to code comments and ADR
entries alike — the docs-coherence-pass prompt's own word "retires"
for this treatment was the design channel's imprecision, recorded as
such; nothing about M4's own execution changes.

**AR-D-6 (parity-ledger counting definitions, both verified).** M1–M3's
own parity ledgers (this ADR's execution records) counted both
`^(deftest ` and `^(defspec ` forms; M4's own ledger (above) counted
`deftest` only. Conservation holds under EITHER definition — design-
channel verified: pre-M4 residual `sim` was 95 tests under the
both-forms definition, and M4's own split accounts for it in full
either way (32 `sim-check` + 62 `sim-engine`/siblings + 1 residual
orchestration, both-forms count). Every future parity ledger in this
project states which definition (deftest-only, or deftest+defspec) it
counts, explicitly, rather than leaving the reader to infer it from
context the way this arc's own four stages did.

---


<!-- Attic file: notes/adr/0025-sim-split-s1-s2.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0025 — sim split S1+S2: `sim-model` and `sim-trajectory` extracted from `sim`

**Status:** Accepted (author-ruled 2026-08-02 on AR-1..AR-4 of
`.agents/plans/2026-08-02-sim-split-plan.md`; session executed same
day, S1 committed and pushed by the author at `8d5c86c` before S2
began, per AR-4's own hard boundary).

### Context

`components/sim` landed as one deliberately fat component (ADR-0001
R5), its own `interface.clj` docstring explicitly deferring narrowing
to "a future, author-ruled extraction session." `.agents/plans/2026-08-02-
sim-split-plan.md` is that session's design pass: a require-graph audit
(2026-08-02) found sim's 20 namespaces layer cleanly with no cycles,
motivated not by tidiness but by the GMF coverage gap
(`components/sim-trajectory/docs/gmf-interpreter.md`'s own survey,
CallSubmodule/condition-vocabulary gaps) living entirely in the
loader/interpreter/compile pipeline — extracting that pipeline first
gives the coverage-expansion work (explicitly OUT of this session, R-4)
a bounded component with its own test surface. Method precedent: the
three-stage `tools` split (ADR-0016/0017/0018), same characterize →
extract → verify → records discipline, the `extraction-stage` skill.

### Decision

**S1 — `sim-model`** (AR-1/AR-2 names and scope, ruled as proposed):
`pathway`, `facility`, `persona`, `config` — schemas and sampling with
no dependency on any other sim namespace — move to `components/sim-model`
as `ehrt.sim-model.*`, namespaces renamed `ehrt.sim.X` → `ehrt.sim-model.X`,
bodies byte-identical except the `ns` form (diff-verified for all four).
`resources/sim/demographics/*` (persona.clj's own vendored tables, its
only consumer) moved with it, same relative resource path
(`sim/demographics/...`) preserved under the new `resources/` root so
`io/resource` calls needed no edit. `site-profile` and `version` stay
in the residual per the plan's own emitter-vocabulary/provenance
reasoning — not touched this session.

**S2 — `sim-trajectory`** (AR-4: same session, two commits, S1 green
and committed before S2 began): `gmf`, `gmf-interpreter`,
`compile-trajectory` move to `components/sim-trajectory` as
`ehrt.sim-trajectory.*`, same byte-identical-bodies discipline. The
three component docs (`gmf-interpreter.md`, `gmf-source-model.md`,
`trajectory-computation.md`) and their two fixtures
(`fixture-clinic.json`, `pinned_seed_42_patients_5.edn`) moved with it;
the six test files the plan named (`gmf_test`, `gmf_interpreter_test`,
`gmf_horizon_test`, `compile_trajectory_test`,
`vendored_appendicitis_test`, `vendored_module_test`) moved with it too.

**Interface widths (AR-6, both stages): grep evidence against
`components/sim`'s own src and test trees before each move, not
interface-design judgment** — the same discipline the fat
`ehrt.sim.interface` itself was built with (R5). `ehrt.sim-model.interface`
re-exports `Concept`/`ConditionAnnotation`/`Citation`/`Step`/
`PathwaysConfig`/`valid?`/`valid-pathways-config?`/
`sample-admission-discharge` (pathway), `ward-by-name`/
`licensed-bed-ids`/`surge-slot-ids`/`occupancy-board`/`ward-census`/
`allocate`/`choose-attending` (facility), `Persona`/`valid-persona?`/
`persona`/`reference-today-epoch-day` (persona), `default-facility`/
`default-provider-templates`/`materialize-providers` (config) — every
other def each of the four namespaces carries (`Pathway`,
`PathwayAssignment`, `ForcePlacement`, `explain*`, `ward-by-id`,
`valid-facility?`, the Luhn/NPI internals, ...) had zero real callers
outside sim-model, confirmed by grep, and stays unexported.
`ehrt.sim-trajectory.interface` re-exports `load-module`/
`valid-modules-config?` (gmf), `run-module` (gmf-interpreter,
both its 4-arity and 5-arity forms), `compile-trajectory`
(compile-trajectory) — residual sim's `engine` and `run` confirmed the
only two real external callers, matching the plan's own prediction
exactly; `ModulesConfig` and every other gmf/gmf-interpreter/
compile-trajectory def stays internal, same grep-confirmed absence of
real callers. `ehrt.sim.interface`'s own public surface is unchanged
by either stage (confirmed before touching anything: it requires only
`check`/`identifiers`/`run`/`version`, none of the seven moved
namespaces) — corpus's in-process edge into it
(`ehrt.corpus.sim-adapter`, `notes/tools/ADRs.md` ADR-0012 fulfilled
by ADR-0004) survives untouched.

**Dependency directions, poly-enforced, forbidden-forever the reverse:**
`sim-model → kernel` only. `sim-trajectory → {sim-model, kernel}` only
— never `sim` itself (would invert the extraction). `sim → {kernel,
sim-model, sim-trajectory}`, its pre-existing corpus-derived-never rule
(ADR-0022) unchanged. Any cycle among `{sim-model, sim-trajectory,
sim}` is forbidden. `poly check` is the actual enforcement surface, not
merely this record.

**Move-don't-improve, one deviation caught and fixed before it shipped
(fix-forward, not silent).** The vendored-module test fixtures
(`fixture-clinic.json`, `pinned_seed_42_patients_5.edn`) were first
`git mv`'d to `components/sim-trajectory/test/ehrt/sim_trajectory/
fixtures/`, mirroring the namespace rename — wrong: the `io/resource`
calls that load them are string literals (`"ehrt/sim/fixtures/
fixture-clinic.json"`), unrelated to and unmoved by any namespace
rename, and `engine_test.clj` (which stays in residual sim) reads the
same two files by that same literal path. `poly test :all` caught this
immediately as a compile error (`Could not locate
ehrt/sim/vendored_appendicitis_test__init.class` — a second, related
miss: `vendored_appendicitis_test.clj`'s and `vendored_module_test.clj`'s
own `ns` forms don't contain `gmf`/`compile-trajectory` as a
substring, so the mechanical rename sed that caught every other moved
test file's `ns` form silently skipped these two). Both fixed forward
before any commit: the fixtures directory now sits at
`components/sim-trajectory/test/ehrt/sim/fixtures/` (preserving the
`ehrt/sim/fixtures/...` resource-relative path the literals expect,
even though it now lives under a different component's `test/` root —
a real, intentional cross-component test-resource sharing this
workspace already tolerates, e.g. `components/sim/resources/sim/
modules/*.json` staying put and being read by both residual sim's
`resolve-modules` and sim-trajectory's own vendored-module tests); the
two `ns` forms were hand-corrected. Named here because a census can
show what a namespace requires, but not that a resource string literal
matches a directory that's about to move (`extraction-stage` skill,
step 5's own point, generalized past `poly check` to resource strings).

**Docs and citation sweep (R-5).** Every `docs/gmf-interpreter.md`-style
bare component-relative citation in a file that does NOT own that doc
anymore (`emit_hl7.clj`, `check.clj`, `engine.clj`, `engine_test.clj`,
`emit_hl7_test.clj` in residual sim; `pathway.clj`, `persona.clj`,
`pathway_test.clj` in sim-model) was repointed to the component-adjacent
form (`components/sim-trajectory/docs/gmf-interpreter.md`), the same
citation convention `docs-tooling`'s own stale-path tripwire already
enforces for `components/corpus/docs/experiments/...`. The three moved
docs' own relative links to sim's remaining docs (`patient-state-model.md`,
`sim-theory.md`, `event-sourcing.md`, `sim-theory-diagram.md`,
`sim-theory.edn`, `research/...`) were repointed `../../sim/docs/...`;
their links to each other stayed bare (all three moved together).
`docs/glossary.md`'s three links into the moved docs were repointed.
The stale-path tripwire itself (`ehrt.docs-tooling.stale-path-test`)
gained two new forbidden substrings this session, `ehrt.sim.gmf`
(also catches `ehrt.sim.gmf-interpreter` as a substring, intentionally)
and `ehrt.sim.compile-trajectory`, scanned over the same `docs/` +
`components/corpus/docs/use-cases.edn` surface the existing family
uses — proven caught via the test file's own synthetic
`each-forbidden-pattern-is-actually-caught-test` cases, not against a
real live violation (none existed under `docs/` at the time this
addendum landed, since the citation sweep above ran first).

**Verification baselines, both stages, all met:** `poly check` clean;
`poly test :all skip:integration` 0 failures/0 errors; a fixed-seed
golden run (seed 42, 5 patients, `--emit hl7`) byte-identical before
and after each stage — `ground-truth.edn`/`identifiers.edn` exactly,
`run.edn`'s only diff in each comparison was the manifest's
`:generator :sha256` field, explained in both cases by environment
(the pre-S1 baseline was captured via a throwaway `git worktree`, whose
`.git` is a file not a directory, so `ehrt.sim.version/git-sha` falls
back to its documented zero-placeholder there — see that function's
own docstring) or by a real, expected HEAD change (S1's own commit
landing between the S1 and S2 golden captures), never by a code change;
deftest+defspec count parity, 403 before S1 (`components/sim/test`
alone, recounted this session — supersedes the plan's own "375" figure
per AR-7's "recount at session start" instruction) = 403 after S1
(353 sim + 50 sim-model) = 403 after S2 (274 sim + 50 sim-model + 79
sim-trajectory); `ehrt help`/`ehrt help sim` byte-identical throughout.

### Fence

This ADR originally covered S1+S2 only; **S3 is now executed too**
(2026-08-02, GMF coverage Wave D stage D0, `notes/ADRs.md` ADR-0029 R1 —
see the dated S3-executed note above for the full account). S4
(`sim-engine`: `engine`, `churn`, `order-profiles`, triggered by a
second `engine` consumer appearing or engine work itself needing the
boundary) is not started — the residual `sim` after S3 is `run`,
`check`, `engine`+`churn`+`order-profiles`, `emit-state`, `identifiers`,
`manifest`, `version`, and the unchanged façade `ehrt.sim.interface`.
`config`'s final home (model for now, per AR-2) is unrevisited. The GMF
coverage gap itself (CallSubmodule, condition-vocabulary gaps) — the
plan's own stated motivation for sequencing S2 first — was closed by
Waves A–C (ADR-0026/0027/0028); Wave D's own state-type/emitter-home
work (ADR-0029 R2, stages D1–D3) is the remaining payoff, not part of
this ADR.

### Deviation record

Two mechanical misses, both caught by `poly test` before any commit and
fixed forward, not silently patched — see the fixtures/`ns`-forms
paragraph above for the full account. No ruling in the plan or this
session's own driving prompt was applied differently than written; no
escalation fired (both named escalation surfaces — a moving namespace
re-exported by the façade, and a require-graph edge contradicting the
plan's own audit — came back empty, confirmed by grep before either
stage's first edit).

> **S3 characterization (filled Step 1, 2026-08-02, Wave D stage D0,
> ADR-0029 R1).** Caller map, grepped against the full workspace, not
> judgment: real `:require` edges onto `ehrt.sim.emit-hl7`/
> `ehrt.sim.v2-replay`/`ehrt.sim.site-profile` are `identifiers.clj` and
> `run.clj` (residual-sim src, both requiring `emit-hl7` only) and, in
> residual-sim's own test tree, `churn_scenarios_test.clj`,
> `emit_state_test.clj`, `emitter_order_independence_test.clj`, and
> `identifiers_test.clj` (all four requiring `emit-hl7` directly — a
> same-component test-tree edge Polylith permits today, which becomes a
> cross-component edge once `emit-hl7` leaves and must repoint to
> `ehrt.sim-emit-hl7.interface`). `emitter_order_independence_test.clj`
> stays in residual `sim` per this session's own driving prompt (its own
> subject is a structural property of the emitter's callER, not the
> emitter's own internals). Every other live hit
> (`persona.clj`/`emit_state.clj`/`engine.clj`/`engine_test.clj`/
> `run_test.clj`) is docstring/comment prose, not a `:require` — verified
> line-by-line, not assumed from the grep alone. `ehrt.sim.interface`
> requires exactly `check`/`identifiers`/`run`/`version`, none of the
> three moving namespaces — unchanged by this move, confirmed before any
> edit (AR-3 holds). Internal-to-the-trio edges move together:
> `v2-replay` requires `emit-hl7`; `emit-hl7` requires `site-profile` and
> `ehrt.sim-model.interface` (the one cross-component edge the new
> component keeps, per the plan's own prediction — no `kernel` edge:
> none of the three files requires it). Third-party deps split by real
> usage, not carried wholesale: `org.clojars.cmiles74/clojure-hl7-parser`
> is required ONLY by `emit_hl7.clj`/`v2_replay.clj` (moves entirely to
> the new component's own `deps.edn`, removed from residual sim's); malli
> is required by `site_profile.clj` AND by four residual-sim files
> (`churn`/`engine`/`manifest`/`order_profiles`) — stays in both;
> `org.clojure/data.json` is required only by residual `emit_state.clj`
> — untouched. Golden baseline, seed 42/5 patients/`--emit hl7`, captured
> before any edit: `run.edn` sha256 `6b48814e…a40a5`, `ground-truth.edn`
> (`--format ground-truth`) sha256 `7617d9ca…4f7a3c`, `messages.txt`
> (`--format er7`) sha256 `f8b15266…d391aa`, `identifiers.edn` sha256
> `4473833f…e190ef3`, `ehrt help` sha256 `1cb4de99…1cac6bdac05d`, `ehrt
> help sim` sha256 `34b35f54…dea8d1c35195`. deftest+defspec count,
> recounted live (AR-7's own "recount at session start" instruction,
> supersedes ADR-0025's own 274-post-S2 figure — seven tests landed in
> residual `components/sim/test` across Waves A/B/C since): **281**
> today, of which the three moving test files carry **75**
> (`emit_hl7_test.clj` 53, `v2_replay_test.clj` 9,
> `site_profile_test.clj` 13) — predicted post-move split 206 residual +
> 75 `sim-emit-hl7` = 281. `poly check` clean; `poly test :all
> skip:integration` 0 failures/0 errors, captured to a file (not piped,
> per this workspace's own caught-before lesson).

> **S3 executed (dated note, 2026-08-02, Wave D stage D0, ADR-0029 R1).**
> `emit-hl7`, `v2-replay`, `site-profile` moved to `components/sim-emit-hl7`
> as `ehrt.sim-emit-hl7.*`, bodies byte-identical except the `ns` form and
> the three files' own cross-references to each other (both diff-audited
> — git's own rename detection reported 92–99% similarity on all six
> moved files). **Interface** (AR-6 discipline, grep evidence only):
> `ehrt.sim-emit-hl7.interface` re-exports `emit` (the 3/5/6-arg forms
> only — the 2-arg form has zero real external callers, confirmed by the
> Step-1 characterization above, and stays unexported), `control-id-for`,
> `default-reference-date`, `default-utc-offset`. `v2-replay` and
> `site-profile` have NO real external caller at all (confirmed by the
> same grep) and are fully internal to the new component — narrower than
> either S1 or S2's own interface, and the first of the three split
> stages where an entire moved namespace stays unexported end to end.
> Residual sim's `identifiers.clj`/`run.clj` and its own
> `churn_scenarios_test.clj`/`emit_state_test.clj`/
> `emitter_order_independence_test.clj`/`identifiers_test.clj` repointed
> their `:require` to the new interface (same local alias `emit-hl7`, so
> call-site syntax is unchanged); six further docstring-only citations of
> the old namespace names (`identifiers.clj`, `run.clj`,
> `emit_state.clj`/`emit_state_test.clj`, `engine.clj`, `engine_test.clj`,
> `run_test.clj`, `persona.clj` in `sim-model`) were repointed to the new
> namespace for self-consistency — none of these files require the moved
> code, so the fix is prose-only. **Dependency directions, poly-enforced,
> forbidden-forever the reverse:** `sim-emit-hl7 → sim-model` only (no
> `kernel` edge — measured, not carried forward from the S1/S2 pattern).
> `sim → {kernel, sim-model, sim-trajectory, sim-emit-hl7}` unchanged
> otherwise. Any cycle among `{sim-model, sim-trajectory, sim-emit-hl7,
> sim}` is forbidden. **Verification, all met:** `poly check` clean;
> `poly test :all skip:integration` 0 failures/0 errors; golden run
> (seed 42, 5 patients, `--emit hl7`) byte-identical on `ground-truth.edn`
> (`--format ground-truth`), `messages.txt` (`--format er7`),
> `identifiers.edn`, `ehrt help`, and `ehrt help sim` — `run.edn`'s only
> diff was the manifest's `:generator :sha256` field, explained by the
> real HEAD change between the two captures (several commits landed in
> between), never by a code change; deftest+defspec count 206 residual +
> 75 `sim-emit-hl7` = 281, exactly matching the pre-move total (zero test
> loss, zero duplication). **Deviation record:** none in the code
> extraction itself — no `poly test` compile-time surprise this time,
> unlike S1/S2's own fixture-path/`ns`-form misses. A real, unrelated
> deviation DID occur in this session's own Step 0: this ADR's sibling,
> ADR-0029, was first inserted between ADR-0027 and ADR-0028 (an `Edit`
> anchor matched Wave B's Fence text instead of Wave C's, both similarly
> worded) and was fixed forward in its own commit before Step 1 began —
> full account in the session record, not restated here since it touches
> ADR-0029's own placement, not S3's design or execution.

---


# Architecture Decision Records — ehr-testing-sim

Numbered, append-only. Never silently revert an Accepted ADR;
supersede it with a new numbered record.

---

## ADR-0001 — Standalone library with a mountable CLI group; dependency arrow points tools → sim only

**Status:** Accepted (2026-07-26)

**Context.** This repo must be usable independently (own `sim` CLI)
and mountable inside ehr-testing-tools' `ehr` CLI as the `sim`
subcommand, without either repo forking the other's conventions.
Tools' CLI architecture (its ADR-0004): thin printing shell over pure
capability functions following the result-not-throw doctrine, `[group
action]` dispatch with injectable `-fn` keys, EDN canonical with
`--json` projection, exit codes 0/1/2(/3).

**Decision.**
1. **Dependency direction:** ehr-testing-tools may depend on
   ehr-testing-sim; ehr-testing-sim never depends on
   ehr-testing-tools.
2. **Embedding contract:** `ehr-testing-sim.cli` exports three public
   values — `cli-spec` (babashka.cli coercions, mergeable),
   `help-group` (one entry in tools' help-data shape), and
   `dispatch-action` (`(fn [action opts]) → Result`). A host mounts
   the group with one dispatch arm plus a spec merge and a help-group
   registration.
3. **Standalone = embedded:** the standalone `sim` shell wraps the
   same `dispatch-action`, so the two modes cannot drift.
4. **Copied, not shared, result vocabulary:** the ~30-line
   result-not-throw ns is duplicated (own namespace, identical
   structure). Result maps are structurally typed, so hosts consume
   them regardless of constructing namespace. A shared microlib is
   deferred until a third consumer exists.
5. **Mirrors carry tripwires, contracts live with the host:** shapes
   this repo must match (tools' manifest schema, help-data shape) are
   mirrored here with validating tests as tripwires; the *binding*
   cross-repo contract tests belong in tools' integration tree, where
   both codebases share a classpath.

**Consequences.** Sim stays independently useful and testable; tools'
tests can stub the whole sim group via the injectable `-fn` pattern
without loading the engine; schema drift between the repos surfaces as
a failing test in tools, not a runtime surprise.

---

## ADR-0002 — Ground-truth log is primary; wire formats are emitters; pathways are a common intermediate representation

**Status:** Accepted (2026-07-26)

**Context.** The library composes two mined designs: Google Simulated
Hospital's operational model (scripted pathway steps incl. ADT churn,
discrete-event queue, HL7v2 messaging) and Synthea's generative
clinical modules (probabilistic state machines with embedded SNOMED/
LOINC/RxNorm codes). See `.agents/memory/architecture.md` for the
mining record and `docs/problem-statement.md` for the black-box
contract.

**Decision.**
1. **Intermediate pathway representation (IR):** hand-authored
   scenario scripts and generated trajectories (later: compiled from
   Synthea-GMF-style modules) share one EDN pathway format; the engine
   executes only the IR and cannot distinguish the sources.
2. **Ground-truth log is the primary output:** the engine emits a
   format-free, time-ordered event log. HL7v2 messages (and later
   FHIR/CDA state renderings) are emitters consuming the log. Test
   assertions and the invariant catalog target the log.
3. **Codes are state, not format:** clinical concepts ride the IR and
   log as `{:system :code :display}` triplets; each emitter renders
   them natively.
4. **Determinism:** all randomness flows from one seeded RNG with a
   totally ordered event queue; same config + seed ⇒ byte-identical
   serialized output. Enforced by property tests.
5. **Invariants co-land with steps:** every step type added to the
   engine ships with its invariants in the catalog in the same change.

**Consequences.** State-based emitters become renderings over retained
state history rather than a redesign; the validation program's
internal-consistency claims are cheaply machine-checkable at scale;
churn injection (operational noise) becomes an IR-to-IR transform,
cleanly separated from clinical trajectory generation.

---

## ADR-0003 — Adopt ehr-testing-tools' authoring conventions

**Status:** Accepted (2026-07-26)

**Context.** This repo started as a walking skeleton with no authoring
discipline of its own, while its sibling ehr-testing-tools has already
converged on a working set of conventions for exactly this
situation — a solo author running Windows, doing git work from WSL,
and depending on chat sessions that can't read the filesystem
directly. Reinventing that discipline from scratch would either
duplicate the sibling's reasoning or silently diverge from it for no
reason. ADR-0001 already establishes that copying conventions and text
from tools is fine — only code and data dependencies are barred.

**Decision.**
1. **Git hooks + WSL rule:** `.githooks/pre-commit` and
   `.githooks/pre-push` are adopted from tools (the pre-push hook
   trimmed to this repo's actual Makefile targets — `make test` only;
   tools' `lint-pipeline`/`lint-deps`/`quickstart-fresh` targets don't
   exist here). Activation (`git config core.hooksPath .githooks`) is
   documented in `AGENTS.md`, mirroring tools' placement.
2. **Pack ceremony, with the active-pack-push inversion carried
   forward:** the pack ritual (`make pack`/`pack-skills`/`pack-push`)
   is adopted, but unlike tools — where `pack-push` went dormant once
   the repo and the `pragsmike/packs` transport both went public —
   `pack-push` stays the **active** session-end ceremony here, because
   this repo has no GitHub remote yet and the packs transport is
   currently the only way a chat session can read it. This mirrors the
   arrangement tools itself used before its own ADR-0008. **Trigger for
   revisiting:** when this repo gets a public GitHub remote, demote
   `pack-push` to dormant the way tools did, and record that demotion
   as a new ADR.
3. **ADR rules, unchanged:** never silently revert an Accepted ADR;
   supersede with a new numbered record; ADRs outrank inference about
   why the project is organized a certain way. Already this repo's
   practice (this file's own header); now stated explicitly in
   `AUTHORS-GUIDE.md` too.
4. **Facts-register discipline, adopted:** `notes/facts-register.md`
   holds F-rows only (no C-table) for load-bearing, externally
   verifiable claims (license, version, count, capability), each with
   claim / where-asserted / evidence / last-verified date, following
   the same assert → register → date discipline as tools. Seeded from
   the externally verifiable claims previously embedded in
   `.agents/memory/architecture.md`, which now points at the register
   instead of restating them.
5. **Handoff convention, adopted:** mid-flight multi-session work ends
   with a handoff document in `.agents/handoffs/`; tools' `handoff`
   skill may be used to generate it.
6. **Skills are NOT copied per-repo.** `.agents/skills/` stays empty.
   Deliberation and utility skills (`handoff` and others) remain
   shared at the user level, per tools' own shared-skill-layout
   convention — copying them into this repo would create exactly the
   kind of drift-prone duplication the shared layout exists to avoid.

**Trigger conditions for later adoptions (agreed now, deferred until
each condition is met):**
- **CI** — adopt once this repo has a public GitHub remote to run it
  against.
- **Pinned-artifact or vendoring decision** (tools' `artifacts.lock.edn`
  / vendor-corpus pattern) — adopt once Synthea modules (or another
  large upstream artifact) actually land in this repo.
- **`.agents/plans/` and pattern-nursery-style working files** — adopt
  at first real multi-session use, not speculatively.

**Consequences.** This repo's authoring discipline stays traceable to
a working precedent instead of improvised ad hoc; the one deliberate
divergence (active vs. dormant `pack-push`) is recorded with its own
trigger condition instead of left to be rediscovered later; skills
stay out of per-repo drift by design; facts about upstream sources
are checkable in one place instead of scattered across prose.

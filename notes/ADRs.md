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

---

## ADR-0004 — Test-first, staged enforcement

**Status:** Accepted (2026-07-26)

**Context.** This repo's product *is* test instrumentation — synthetic
traffic for testing EHR integrations — so a test-shy sim repo is
self-refuting in exactly the way tools' ADR-0006 named for itself.
Unlike tools at its ADR-0006, this repo isn't adopting the discipline
ahead of capability code: its headline guarantees are already
property-tested (`determinism-holds-for-all-seeds`,
`every-run-satisfies-invariant-catalog` in
`test/ehr_testing_sim/engine_test.clj`) and `check.clj`'s invariant
catalog already co-lands with new step types by existing convention
(ADR-0002 point 5). What's missing is not the practice but the
written, mechanical rule: sessions are executed by agents that cannot
see each other, so discipline must be stated and partly enforced, not
remembered.

**Decision.** Test-first is a hard rule — a failing test precedes the
implementation it motivates; sessions demonstrate red→green in their
reports; property tests are required for law-bearing constructs, which
in this repo concretely means: determinism, the invariant catalog,
emitter derivability laws (once emitters exist), and schema
round-trips. Coverage is measured (`cloverage` via a `:coverage` alias
and `make coverage`) and regressions require justification in the
session report.

Enforcement is staged: **now** — convention + prompt discipline +
coverage measurement, **and** the pre-push hook already running `make
test` (ADR-0003; unlike tools at its ADR-0006, this repo's mechanical
enforcement is partially live from day one, not planned for a later
wave, since the hook already existed before this ADR). **Later** — CI,
once this repo has a public GitHub remote to run it against (ADR-0003's
existing trigger); a coverage `--fail-threshold` gate, once a baseline
worth ratcheting exists (today's baseline: 81.67% forms / 91.15% lines
overall, `cli.clj` lowest at 50.30%/70.00% as the thin printing shell).

**Rejected.** Full mechanical enforcement immediately (a
`--fail-threshold` today) — there is no baseline yet to set it against,
and an arbitrary number invites gaming rather than measuring. Coverage
as vibes — unmeasured "good coverage" is unfalsifiable.

**Consequences.** Every code-producing session carries the red→green
reporting duty; `check.clj` invariant co-landing and the determinism
property tests, already practiced, are now a stated rule instead of
tacit convention; `AGENTS.md` gains the mechanical anchor (`make test`
+ `make coverage` before any session-final commit) that the sibling
repo's own experience showed is necessary once sessions stop sharing
memory.

---

## ADR-0005 — Copy two load-bearing skills; narrow, not revert, ADR-0003's no-skills-copy clause

**Status:** Accepted (2026-07-26)

**Context.** ADR-0003 decided `.agents/skills/` stays empty, reasoning
that deliberation and utility skills should live at the shared user
level to avoid per-repo drift. That reasoning holds for skills this
repo merely benefits from occasionally. It does not hold for a skill
this repo now has a concrete, load-bearing use for: `string-diagram`
is needed to render `docs/sim-theory.edn` (the resource-theory
equations) to Mermaid, and `handoff` is needed for multi-session
continuity now that plan-scale work (the HL7 emitter) is next — both
uses that exist today, not hypothetical future ones. A shared-level
skill still works when invoked from this repo, but its presence isn't
discoverable from inside this repo's own tree, and "read `AGENTS.md`,
find what you need" is exactly the discoverability property the rest
of this repo's authoring discipline (ADR-0003) is built around.

**Decision.** Copy exactly two skills from
`../ehr-testing-tools/.agents/skills/` into `.agents/skills/` here,
wholesale, each for a stated, current use:

1. `string-diagram` — load-bearing for rendering `docs/sim-theory.edn`
   to Mermaid. Its `SKILL.md` hard-coded relative paths to a converter
   script and example equation sets claimed into ehr-testing-tools'
   own `palgebra/` directory, which is not copied here (out of scope —
   copying it would be a data dependency ADR-0001 doesn't bless just
   because the skill wants it). Adapted the "Step 2" command and the
   "Files" section only, to reference `../ehr-testing-tools/palgebra/...`
   explicitly: using this skill from ehr-testing-sim requires a sibling
   checkout of ehr-testing-tools, same as this adoption session itself
   did. No other content changed.
2. `handoff` — load-bearing for multi-session continuity. Copied
   verbatim; all its paths (`.agents/handoffs/`,
   `.agents/handoffs/archive/`) are already repo-local conventions
   (ADR-0003 point 5), so nothing needed adaptation.

**Provenance.** Both are copies of ehr-testing-tools' own copies, not
fetched from further upstream by this session. `string-diagram` is
part of tools' verified cyberneutics-derived set (tools' own AGENTS.md:
five skills copied and adapted from the public `pragsmike/cyberneutics`
repo, `string-diagram`'s own provenance confirmed there directly
against that repo's `.claude/skills/string-diagram/SKILL.md`). `handoff`
carries a
`license: MIT` / `metadata.author: cyberneutics` frontmatter block in
tools' copy, preserved unchanged here — per tools' own AGENTS.md, its
cyberneutics-derived skill set (which does not include `handoff`, per
tools' own accounting) traces to the public `pragsmike/cyberneutics`
repo; `handoff`'s frontmatter names `cyberneutics` as author without
further upstream citation in tools' own skill file, and this session
did not independently verify a `cyberneutics`-repo source for it
beyond what tools' copy already states — carried forward as-is, not
re-verified.

This partially supersedes ADR-0003 point 6: the no-skills-copy default
still holds for every other skill (the deliberation suite —
`scenarios`, `probe`, `committee`, `review` — and utility skills like
`find-skills`, `repo-adaptation`, `shared-skill-layout`,
`wsl-windows-git-hygiene`), which remain shared/user-level exactly as
ADR-0003 decided. Only `string-diagram` and `handoff` move to
per-repo, and only because each has a stated, current load-bearing use
in this repo today — the bar ADR-0003's reasoning implies but didn't
name explicitly. A future skill earns the same treatment only under
the same bar, not by analogy or convenience.

**Consequences.** `make pack-skills`, not `make pack`, carries this
content — the existing `PACK_ELIDE_PATTERN`
(`^\.agents/skills/|^\.agents/prompts/archive/`) already elides
`.agents/skills/**` from the main pack without any change, verified
this session rather than modified. `.agents/skills/.gitkeep` is
removed (no longer an empty directory placeholder). Two skills now
carry independent copies across sibling repos; a future edit to either
skill's teaching content in one repo does not propagate to the other
without a deliberate sync — an accepted cost of per-repo discoverability,
same tradeoff ADR-0003 already named for hooks and the pack ritual.

---

## ADR-0006 — Private GitHub remote added; session-end ceremony grows a step, `pack-push` stays ACTIVE

**Status:** Accepted (2026-07-26)

**Context.** ADR-0003's trigger for demoting `pack-push` to dormant
(the arrangement tools itself used before its own ADR-0008) was
explicitly **a *public* GitHub remote** — the `pragsmike/packs`
transport exists because a private repo's raw file contents aren't
fetchable by URL, and a private repo doesn't change that. This session
adds a private origin (`git@github.com:pragsmike/ehr-testing-sim.git`)
during an authoring session, ahead of any decision to publish.

**Decision.**
1. **`git remote add origin git@github.com:pragsmike/ehr-testing-sim.git`**
   — this repo now has a GitHub remote for the first time.
2. **Session-end ceremony grows a step:** commit → `git push origin` →
   `make pack-push`, in that order. Pushing to origin first means a
   collaborator with repo access has the code before the pack is
   republished; `pack-push` still runs last (AUTHORS-GUIDE.md's
   ordering caveat is unchanged — the pack header's clean-tree line is
   only meaningful if nothing is left uncommitted afterward).
3. **`pack-push` stays ACTIVE, not dormant.** A private repo's raw URLs
   are not fetchable (`raw.githubusercontent.com` requires the
   requester to be authenticated and authorized for a private repo,
   which a chat session reading via plain HTTP fetch is not) — the
   `pragsmike/packs` transport remains the only way a chat session
   without repo-scoped credentials can read this repo's current state.
   The Makefile's own demotion trigger (ADR-0003, its header comment)
   is unchanged: demote when this repo's GitHub remote is **public**,
   not merely when one exists.

**Consequences.** `git push -u origin main` joins the ceremony without
retiring anything; `AUTHORS-GUIDE.md` section 2 is updated to name the
three-step order. The demotion trigger from ADR-0003 remains open and
unmet — a future session that flips this repo's visibility to public
records that as its own ADR, exactly as ADR-0003 already anticipated.

---

## ADR-0007 — Three-class operational resource taxonomy; occupancy as a derived projection; encounter-horizon scope; payers as an attribute; synthetic NPIs are Luhn-valid

**Status:** Accepted (author-ratified 2026-07-26) — see
[`docs/operational-models.md`](../docs/operational-models.md) for the
full design this ADR records the decisions from.

**Context.** The theory (`docs/sim-theory.edn`) named facility,
provider, and payer resources only implicitly — as config a stage
would eventually need — with no design for how each behaves once
patients start occupying, using, or carrying them. Three questions
needed answers before Milestone M1 (`.agents/plans/roadmap.md`) could
be specified: what kind of thing is a bed, versus a provider, versus a
payer; where does the occupancy board live relative to patient state;
and what identifier scheme do synthetic providers carry. A fourth,
narrower question — how far into a patient's life this simulator's
scope extends — surfaced while writing the roadmap's exclusions and is
recorded here alongside the resource-model decisions since both are
scope-shaping calls a reviewer should ratify together.

**Decision.**

1. **Three-class resource taxonomy**, naming a spectrum of how much
   the engine must track a resource, not just today's three examples:
   **exclusive** resources (occupancy-tracked, capacity-bounded,
   invariant-bearing — beds), **shared** resources (assigned but not
   consumed, no exclusivity invariant — providers), and **attribute
   pools** (sampled once at patient creation, carried as state, never
   tracked as a resource at all — payers). Every future resource this
   simulator models is expected to be one of these three; getting a
   new resource's class right up front is meant to prevent both
   over-building (an occupancy board for something nobody contends
   over) and under-building (a missing double-booking invariant).
2. **Occupancy — and derived projections generally — as the house
   pattern:** the patient's own state is the single authoritative
   record of its location; the occupancy board is a derived index,
   proven consistent by a property test (board ≡ fold over patient
   locations), never a second place facts get written first. This is
   the same shape as `sim-theory.md`'s open question #3 (whether
   `state-history` is primitive or derivable from
   `ground-truth-log`) — one authoritative record, everything else a
   projection with a proven consistency law — applied now to the
   nearer-term occupancy board rather than left as only a future
   question about the log.
3. **Encounter-horizon scope boundary:** this simulator generates
   hospital-operations traffic across a single encounter — admission
   through discharge and its immediate churn — not a patient's
   lifelong longitudinal history. Recorded as constraint 10 in
   `docs/problem-statement.md` and as a named exclusion in
   `.agents/plans/roadmap.md`.
4. **Payers are an attribute pool, not a tracked resource:** sampled
   once (age-linked where trivial — Medicare weighted at 65+, the
   distribution idea mined from Synthea per
   `docs/third-party-sources.md` tier 1), carried as patient state,
   rendered in IN1 once that segment lands (Milestone M4). No board,
   no assignment event, no capacity — the point of naming payers as
   the attribute-pool instance of the taxonomy rather than inventing
   a fourth category for them.
5. **Synthetic provider NPIs are Luhn-valid, not obviously fake.**
   Generated as structurally valid 10-digit NPIs (correct Luhn check
   digit over the `80840` health-industry-issuer prefix) from the
   run's seed, rather than an obviously-fake sentinel format.
   Coincidence with a real NPPES-assigned NPI is possible and
   recorded as harmless: NPPES is itself public data, and an NPI
   identifies a provider, not a patient, so no PHI is implicated.

**Rejected.**

- **A fourth resource class**, or per-resource bespoke design with no
  shared taxonomy — rejected because it was the taxonomy itself, not
  any one model, that was missing; three classes account for beds,
  providers, and payers without strain, and forcing every future
  resource through the same three-way question (tracked-exclusive?
  tracked-shared? untracked-attribute?) is more useful than solving
  each resource's design from scratch.
- **Occupancy as a second authoritative structure**, updated directly
  alongside patient state — rejected because it reintroduces exactly
  the two-sources-of-truth failure mode `sim-theory.md`'s open
  question #3 already flags as a risk for `state-history`; keeping
  the pattern singular (one authoritative record) now avoids arguing
  the same case twice.
- **Obviously-fake NPI format** (option b in
  `docs/operational-models.md`) — rejected because realism against
  systems that validate NPI check digits is a stated product goal,
  and the coincidence risk with a real assignment is both low and,
  on inspection, harmless.

**Consequences.** `docs/operational-models.md` becomes Milestone M1's
spec; a future resource proposal (equipment, order slots, transport)
is expected to state its class under this taxonomy before design
work starts. The occupancy consistency law is a property test M1 must
ship, not an optional nicety. `docs/problem-statement.md` gains
constraint 10 (encounter horizon) as a one-sentence addition, per this
session's own scope-minimality rule for touching that document. No
code accompanied this ADR at acceptance time; Milestone M1
(`.agents/plans/roadmap.md`) is where `docs/operational-models.md`'s
design becomes code.

---

## ADR-0008 — The engine is event-sourced: `decide`/`evolve` replace the fused `transition`; patient state is a fold of the log

**Status:** Accepted (author-directed 2026-07-26)

**Context.** The v0 engine's `transition` multimethod (`engine.clj`)
fused two responsibilities that Milestone M1 is about to put real
pressure on: given a patient's state and a pathway step, it decided
what happens (consulting the RNG) *and* computed the resulting state
*and* produced the ground-truth events, all in one function, one
return value (`{:patient :events :advance}`). This was adequate for a
three-step walking skeleton where every step touched exactly one
patient's own state and nothing else. M1 breaks that: the allocation
ladder must consult *other* patients' locations (the occupancy
projection) before deciding where to place an admission, and the
bed-ready transfer for a boarding patient is triggered by a *different*
patient's discharge — a decision made while processing patient B's
step needs to emit an event that changes patient A's state. A fused
transition has no clean place to put "I looked at the whole facility
and decided something happens to someone else." Splitting the
responsibility now, before M1's step types multiply the surface, is
cheaper than retrofitting it after.

**Decision.**

1. **The ground-truth log is the single primitive.** Patient state
   (all patients', not any one patient's in isolation — the fold
   input the theory calls `state-history`) is not maintained as an
   independent structure the engine mutates; it is what you get by
   folding the log through one pure function. There is no second
   place a fact about a patient's state can originate.
2. **Step application splits into a `decide`/`evolve` pair,**
   replacing the fused `transition` multimethod:
   - **`decide`** `(rng, t, world, mrn, step) → {:events [...] :advance N}`.
     Consults `world` (the current fold of all patient state so far —
     read-only) and the run's single RNG to decide what happens.
     Returns the facts (events); **never returns a new state** and
     never mutates `world`. This is where the allocation ladder and
     the bed-ready cross-patient trigger live: `decide` may return
     events naming a patient other than `mrn` (e.g. patient B's
     discharge `decide` call also returns a transfer event for
     boarding patient A), because deciding what happens is exactly
     where cross-patient coupling belongs.
   - **`evolve`** `(patient-state, event) → patient-state'`. Pure,
     total, and deterministic: no RNG, no knowledge of the step or the
     decision that produced the event, no knowledge of `world` or any
     other patient — dispatch on the event's own `:event` key alone,
     operating on exactly the one patient the event names. This is
     deliberately narrower than `decide`'s view: cross-patient
     coupling is a *decision* (does B's discharge free a bed A is
     waiting on?), never a state-transition rule, so `evolve` never
     needs to see two patients at once even when `decide` does. The
     run loop is what maps an event to the right patient's slice of
     `world` (`(:mrn event)`) and folds `evolve` in there.
   - **The only path by which patient state changes is folding
     emitted events through `evolve`.** The run loop calls `decide`,
     then folds each returned event into `world` via `evolve` at that
     event's own `:mrn`, then appends `events` to the ground-truth
     log, in that order, every time. There is no code path that
     assigns into a patient's state directly.
3. **Consequences for the theory, recorded here and synced into
   `docs/sim-theory.edn`/`.md`:**
   - Log↔state drift — the failure mode of a system that maintains
     state and events as two things a bug can let disagree — becomes
     impossible by construction, not merely tested for: there is only
     one function (`evolve`) that ever produces a new patient state,
     and it is a pure fold over exactly the events already in the log.
   - `sim-theory.md`'s open question #3 (is `state-history` primitive
     or derived?) is **RESOLVED: derived.** `state-history` is
     `(reduce evolve initial-world log-prefix)` at any prefix length —
     a computable projection, not a second output the engine must keep
     in sync with the log by discipline. The authority hierarchy is
     now explicit and three deep: **log → patient states → occupancy
     board**, each stage a projection of the one before it, each with
     its own provable consistency law (the occupancy board's is
     `docs/operational-models.md`'s "board ≡ fold over patient
     locations"; patient states' is this ADR's fold property).
   - EmitState's future implementation is exactly "fold the log to
     instant *t*, render" — the snapshot-at-instant law
     (`sim-theory.edn`'s `:emit-state` stage) stops being aspirational
     prose once `evolve` exists to do the folding.
   - The **emitter-coherence** global law (`sim-theory.md`) gains its
     mechanism: "replaying `hl7v2-stream` reconstructs `state-history`"
     is now a claim about composing `evolve` with message parsing, not
     a hoped-for property with no stated procedure.

**Rejected.** Keeping the fused `transition` and adding cross-patient
awareness as a special case bolted onto it — cheaper today (no API
change), but every step type M1 and M2 add (transfer, bed-swap,
cancel-\*, merge) would widen the surface on which the returned
`:patient` and the emitted `:events` could silently disagree, and a
fused function gives cross-patient effects no principled place to
live except further special-casing. Splitting once, now, is paid down
across every future step type instead of re-argued per step.

**Consequences.** `engine.clj`'s public surface changes: `transition`
is replaced by `decide` and `evolve` (a breaking change to an internal
namespace, not the CLI embedding contract ADR-0001 governs — no
version note needed there). Every existing step type
(`:admission`, `:delay`, `:discharge`) gets a `decide` method and,
where it changes patient state, an `evolve` method; `:delay` has a
`decide` (it samples the RNG) but no `evolve` (it changes no patient
state, so it emits no events for `evolve` to fold). The property test
this ADR requires — patient state, at every event boundary of a run,
equals folding that patient's own event subsequence through `evolve`
from its initial state — is the executable form of "impossible by
construction," not an added nicety. A pinned-seed regression proves
the refactor changes internal structure without changing observable
output for the v0 step set.

---

## ADR-0009 — Seed stability is a within-version guarantee, not a cross-version one; the manifest's generator version is the cross-version key

**Status:** Accepted (2026-07-26)

**Context.** Milestone M1 (docs/operational-models.md) gives
`:admission`'s `decide` two NEW stochastic choices it never had in
v0: a seeded draw among candidate beds (the allocation ladder) and a
seeded draw among ward-eligible providers (attending assignment) —
plus, once per run, generating each provider's synthetic NPI from the
same RNG (ADR-0007's decision (a), explicitly "generated from the run
seed"). None of this existed when the decide/evolve refactor session
captured `test/ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn`
as a regression baseline. Every admission now consumes RNG draws that
didn't exist before, which shifts the entire downstream RNG stream for
ANY pathway using `:admission` — the pinned fixture (and, by the same
logic, any external consumer who had pinned a seed against pre-M1
output) necessarily produces different byte-for-byte output after M1,
even though the *pathway* is unchanged. This was flagged as a live
possibility before M1 landed, with two options on the table: accept
the perturbation and regenerate, or engineer draw isolation to keep
legacy output stable.

**Decision.**

1. **Take option (a): accept the perturbation.** Same config + seed
   still yields byte-identical output (ADR-0002's guarantee is
   unweakened) — but that guarantee is now stated precisely as a
   **within-version** guarantee: identical output for a fixed
   generator version, not across versions where the generator's own
   step vocabulary and stochastic surface have grown. Before this
   library's first published release, growing the step vocabulary
   (this roadmap's whole M1-M6 sequence) is expected to keep shifting
   RNG consumption for existing pathways whenever a step type they use
   gains new stochastic behavior it didn't have before — this is not
   a regression to guard against, it is what "the engine's step
   vocabulary grows" (`sim-theory.edn`'s own `:execute` contract note)
   means in practice.
2. **The manifest's `:generator {:version ...}` field
   (`ehr-testing-sim.manifest`, already shipped) is the cross-version
   key.** A consumer that needs "will this exact byte stream reproduce
   later" should pin generator version alongside seed and config, not
   assume seed alone survives generator upgrades. No schema change
   needed — `MirroredManifest` already carries this field; this ADR
   just names it as the authoritative answer to a question this
   project hadn't had occasion to state a policy on before.
3. **The fixture is regenerated, not silently.** Before/after are both
   recorded in the M1 session's own summary (and in `git log` for
   commit that lands this ADR); the fixture file's header comment is
   updated to say what it now pins (post-M1 output) and to warn that
   the SAME regeneration is expected at each future milestone whose
   step types add stochastic behavior existing pathways now exercise.

**Rejected.** **Option (b): engineer draw isolation** to keep legacy
byte-for-byte output stable across this change — e.g., a separate RNG
stream for NPI generation, or skipping the bed-choice draw whenever
only one candidate exists. Rejected on two grounds: first, making RNG
*consumption* depend on facility *content* (draw only when >1
candidate) is a strictly worse property than "consumption changed
once, for a documented reason" — it would mean two structurally
equivalent configs that merely differ in candidate count diverge in
draw count, a subtler and more surprising coupling than what this ADR
accepts instead. Second, ADR-0007 already specified NPIs as "generated
from the run seed," which is only a meaningful claim if NPI generation
consumes the SAME single RNG stream everything else does — carving out
a second, isolated stream for it would break the "one seed, one RNG,
fully determined consumption order" simplicity ADR-0002 and ADR-0008
already established, in exchange for a backward-compatibility
guarantee this pre-release project doesn't need yet.

**Consequences.** `test/ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn`
is regenerated against M1's engine (see the M1 session summary for the
before/after diff); its header comment and the referencing test's
docstring are updated to describe it as the post-M1 baseline. A future
session whose own pinned-seed regression goes red should read this ADR
before assuming a bug: check whether that session's own new step types
explain the diff first, and if so, follow the same accept-and-record
pattern rather than re-litigating the question.

---

## ADR-0010 — Patient identity becomes an internal `:patient-id`; MRNs move into state; events gain a `:participants` set

**Status:** Accepted (author-directed 2026-07-26) — design capture for
Milestone M2a (`.agents/plans/roadmap.md`); no code lands with this
ADR.

**Context.** `docs/patient-state-model.md`'s accumulator uses `:mrn` as
a stable, never-reassigned identifier, and `ehr-testing-sim.engine`
folds the log and keys its work queue by that same `:mrn`. M2b's churn
family (`docs/sim-theory.edn`'s `InjectChurn`) needs a merge step
(ADT^A34/A40) and, per `docs/clinical-realities.md`'s newborn entry, an
organic merge scenario as well — and a real hospital's MRN is exactly
the identifier that merge *changes*: two records (and their MRNs)
become one patient, one MRN retired from active use but still owed to
every message and log entry that referenced it before the merge. Using
`:mrn` as the fold/queue key doesn't survive that: the key one patient
is folded and scheduled under would have to change mid-run, at exactly
the moment (a merge) when losing continuity of identity is least
acceptable. Separately, M2b's bed-swap step and the merge step itself
are the first step types where a single event legitimately describes
something happening to *two* patients at once, not one — a shape
`ehr-testing-sim.engine`'s current event schema (one implicit subject
per event, keyed by its own `:mrn`) has no room for. Both problems are
solved by the same underlying move (an event needs to be able to name
more than one patient, and a patient needs an identity that survives
what happens to their MRN), so they're decided together here rather
than as two ADRs that would have to cross-reference each other's
assumptions at every turn.

**Decision.**

1. **Identity.** Introduce an internal, deterministic `:patient-id` —
   generated the same way bed ids, provider ids, and NPIs already are
   (from the run's single seeded RNG, `docs/operational-models.md`) —
   as the fold key `evolve` dispatches on and the key the engine's work
   queue schedules by. `:patient-id` is never reassigned and never
   rebinds; it is what MRN was assumed to be before merge complicated
   that assumption.
   - **MRNs become state, not identity.** The accumulator's `:mrn`
     string field is replaced by `{:mrns #{...} :active-mrn ...}`: the
     set of every MRN this patient-id has ever answered to, and which
     one is currently live. A fresh patient starts with a singleton
     set and that MRN as `:active-mrn`.
   - **Merge is an event that rebinds, not a fold that disappears.** A
     merge (A34/A40, landed in M2b) is an ordinary event two patient-
     ids participate in (see point 2): the surviving patient-id's
     `evolve` absorbs the merged identity's `:mrns` set into its own
     and updates `:active-mrn` per the merge's stated direction; the
     merged-away patient-id's own stream ends with a terminal
     merged-into event and folds no further — its `:patient-id` still
     exists as a fold target for every event that named it *before*
     the merge, it simply gains no new state after.
   - **Emitters render `:active-mrn`.** PID-3 and every other MRN-
     bearing field renders whichever MRN is currently active for the
     patient-id a message is about — exactly one value, no set
     leaking into wire format.
   - **The ground-truth log keeps both ids on merge events.** A merge
     event carries both the surviving and the merged-away
     `:patient-id` (and, since MRNs are what a message-parsing
     consumer actually sees, both MRNs) so that message↔truth mapping
     — the problem-statement's own guarantee, "every emitted message
     is derivable from the ground-truth log, and vice versa" — never
     breaks across a merge: a test harness holding only a pre-merge
     MRN can still find its patient's continued log by looking up
     which `:patient-id` that MRN belonged to and following the merge
     event to the surviving id.
   - **Consequence, recorded not actioned here:** every current
     "events-for-mrn" phrasing — in tests, in prose, in any helper
     that takes an MRN and returns a patient's events — becomes
     "events-for-patient" once `:patient-id` is the real key. This
     session does not rename anything (docs/notes only, per this
     session's own scope boundary); M2a's implementation is where the
     rename actually happens.

2. **Multi-participant events.** Events gain a `:participants` field —
   a vector of patient-ids, carrying roles where roles matter (e.g.
   `[{:patient-id ... :role :subject} {:patient-id ... :role :subject}]`
   for a bed-swap's two occupants, `[{:patient-id ... :role :survivor}
   {:patient-id ... :role :merged}]` for a merge). A patient's state is
   the fold of every event in whose `:participants` they appear, not
   only events keyed by a single `:mrn`/`:patient-id` field as today.
   Every event this project has today (`:admission`, `:delay`,
   `:discharge`, M1's `:transfer`) is the **degenerate, single-
   participant case** — a `:participants` vector of length one — so no
   event this project has already shipped needs a *behavioral*
   migration; only the id-key change (point 1) touches them, and that
   touch is mechanical (rename the field, don't change its
   cardinality). Invariants may now assert **cross-participant
   coherence**: a bed-swap must leave both participants placed
   somewhere (neither vanishes from the occupancy projection mid-
   event); a merge must leave exactly one active MRN shared between
   the two participants once folded (never zero, never two actives
   claiming the same identity).

**The SimHospital contrast, reasoned by analogy rather than freshly
mined.** `docs/patient-state-model.md`'s own mining section already
established the shape: `ir.PatientInfo`'s six location fields
(`Location`, `PriorLocation`, `PriorLocationForCancelTransfer`, and
three more) exist because a mutable-state design with no event log
needs a hand-maintained shadow field everywhere a mutation might later
need undoing. Merge is architecturally the same problem one level up —
"what identity did this patient answer to before the merge" is exactly
the kind of prior-value fact a mutable design has nowhere to keep
except another bespoke field or side table. This project does not
re-verify SimHospital's actual merge-handling source this session (no
fresh read; `AGENTS.md`'s "do not invent facts about upstream sources"
applies), so the claim here is inference from the already-verified
location-field pattern, not a new mined fact: expect merge to be
another instance of the same shadow-state-accretion family the log
dissolves, on the strength of the pattern already established, not on
a fresh citation. In this project's design, the log already carries
"what MRN this patient-id answered to before" as an ordinary fact on
the merge event itself — nothing needs its own undo field, the same
argument `docs/patient-state-model.md` already made for location.

**Rejected.**

- **Keeping `:mrn` as the fold/queue key and giving merge special-case
  handling** (e.g., re-keying the queue mid-run, or maintaining a
  redirect table from old MRN to new) — rejected because it reproduces
  exactly the shadow-bookkeeping problem this decision exists to avoid,
  just relocated from patient state into engine plumbing.
- **A single-subject event schema with a separate "linked-events"
  side-table for multi-patient effects** (bed-swap, merge) — rejected
  because it recreates a second place facts about cross-patient
  coupling live, the same failure mode ADR-0008 already rejected for
  patient state generally; `:participants` keeps the single ground-
  truth-log-is-primitive shape (ADR-0008) intact by making multi-
  subject-ness a property of the event's own data, not an escape hatch
  around the event schema.

**Consequences.** M2a's implementation work: rename the accumulator's
`:mrn` field to `{:mrns :active-mrn}`, introduce `:patient-id`
generation alongside bed/provider/NPI generation, thread
`:participants` through the event schema (a vector even for today's
single-subject events), and update every `evolve`/`decide` method and
test helper that assumed a bare `:mrn` key — a mechanical but repo-wide
change, which is exactly why it is scoped as its own milestone (M2a)
ahead of M2b's actual churn step types rather than folded into them.
`check.clj` gains cross-participant coherence as a new invariant
*shape*, not new invariants themselves (those land with M2b's bed-swap
and merge step types, per the co-landing convention). No wire-format
consequence yet: PID-3 already renders a single MRN string; it renders
`:active-mrn` instead of `:mrn` post-M2a, a rename at the render call
site, not a segment redesign.

---

## ADR-0011 — The time model: integer seconds, a pinned UTC offset, a seeded arrival process, and a marked warm-up window

**Status:** Accepted (author-directed 2026-07-26) — design capture for
Milestone M2a (`.agents/plans/roadmap.md`); no code lands with this
ADR.

**Context.** The engine's clock today is implicit — event timestamps
are simulated minutes from run start (`docs/patient-state-model.md`'s
`:admitted-at`, typed `:int, simulated minutes`), there is no timezone
or offset concept anywhere in the pipeline, patient count is a fixed
`N` sampled up front, and a run has no notion of a warm-up period
distinct from steady state. Four gaps surfaced together while scoping
M2a because they share one property: each is cheaper to fix now, before
M2b's churn family and M3's order/result bursts multiply the surface
that depends on the clock, than to fix after every downstream consumer
(emitters, the future log player, `docs/site-profiles.md`'s eventual
config) has already assumed today's shape.

**Decision.**

1. **Granularity: integer seconds from run start, replacing minutes.**
   Every timestamp in the engine — the event queue's ordering key, the
   ground-truth log, `:admitted-at` and any future `:*-at` field — is
   an integer count of seconds since the run began, not minutes.
   Rendering precision (whether an emitter shows `HH:MM` or `HH:MM:SS`)
   is each emitter's own choice at render time, unconstrained by the
   engine's internal grain. **Rationale:** M3's order/result steps
   (`docs/sim-theory.edn`'s `Execute` contract note on the
   `order-profiles` catalytic) plausibly need sub-minute ordering for
   a burst of results returning close together, and the roadmap's
   future log-player consumer (`.agents/plans/roadmap.md`'s consumer
   plan, this session) needs the same fine grain to pace replay
   realistically. Changing grain now costs exactly one seed
   perturbation (every timestamp-consuming draw shifts once) —
   ADR-0009's policy already states that this class of change is
   accepted and regenerated, not guarded against. Changing grain later,
   once M3's order/result content and a log player both exist and
   assume minutes, would cost a migration across every downstream
   consumer instead of one regeneration here.
2. **DST/zone: v1 pins a fixed UTC offset.** `sim-config` gains an
   offset field (default `+00:00`), stated once per run and recorded in
   the run manifest (`ehr-testing-sim.manifest`) alongside the other
   pinned inputs ADR-0007 already established the pattern for (seed,
   engine params, config hash). No timezone database, no daylight-
   saving transition logic, no per-event offset — one offset, fixed for
   the whole run. A deliberately DST-crossing corpus (a run whose
   simulated window spans a spring-forward or fall-back transition) is
   recorded in the roadmap as **premium future test data** — real
   interfaces mishandle DST transitions often enough that it's valuable
   generated traffic — not v1 scope: v1's job is a correct, simple
   clock, not the hardest clock.
3. **Arrival process: a seeded alternative to fixed `:patients N`.**
   Alongside today's fixed patient count, `sim-config` gains a seeded
   **arrival process** — exponential inter-arrival times at a
   configured rate (a Poisson arrival process, the standard queueing-
   theory model for independent arrivals) — as a second way to
   populate a run. This is what makes load-driven realism possible:
   boarding (`docs/operational-models.md`'s allocation ladder rung 4)
   is a function of census pressure, and census pressure is a function
   of how fast patients arrive relative to how fast beds free up, which
   a fixed `N` sampled once can't vary within a run the way a live rate
   can. It's also the mechanism the future log player needs for
   open-ended generation (a corpus with no fixed end, paced against a
   rate rather than exhausted after `N` patients). **Mechanism, API
   shape only — not built this session:** windowed generation, where
   the engine materializes arrivals for a rolling time window rather
   than the whole run up front, and discharged patients are retired
   from the working set (`world`, ADR-0008) once their stream is
   complete, so a long or open-ended run's live working set stays
   bounded rather than growing with total elapsed time. Sketched here
   as the shape M2a's implementation targets; not designed in the
   fuller sense `docs/operational-models.md` designs the allocation
   ladder.
4. **Warm-up: a config window whose events are marked or trimmed.** A
   run beginning from an empty hospital has a cold-start artifact — the
   first stretch of simulated time is systematically less
   representative than steady state (no boarding is possible until
   wards fill, no bed-ready transfers are possible until someone's been
   admitted long enough to discharge). `sim-config` gains a warm-up
   window (a duration from run start); events generated inside it are
   either marked (a `:warm-up true` flag events downstream can filter
   on) or trimmed entirely at packaging time (`docs/sim-theory.edn`'s
   `Package` stage) — both options recorded, the choice between them
   deferred to M2a's implementation rather than decided here, since it
   turns on packaging details (does a consumer want to see the
   cold-start traffic at all, or never receive it) this session doesn't
   have enough information to settle. Either way, a steady-state
   corpus intended for calibration or realism claims (`Calibrate`,
   `docs/sim-theory.edn`) can exclude the cold-start artifact instead
   of silently including it as if it were representative.

**Consequences.** ADR-0009's within-version seed-stability policy
absorbs decision 1's perturbation exactly as that ADR already commits
to: same config + seed still byte-identical, but M2a's landing is
another documented instance of "the generator's stochastic surface
grew," not a regression. `docs/patient-state-model.md`'s `:admitted-at`
type note (currently "simulated minutes") and any other prose
referring to simulated minutes become stale the moment M2a lands
seconds granularity; updating them is M2a's job (a mechanical doc pass
alongside the code change), tracked here so it isn't silently missed
in the gap between this ADR and that implementation. The run manifest
gains the UTC-offset field (decision 2) the same way it already
records seed and config hash — a schema addition, not a new concept,
for `ehr-testing-sim.manifest/MirroredManifest`. The roadmap
(`.agents/plans/roadmap.md`) gains the DST-crossing corpus as a named
future-premium-content item and the arrival-process API sketch as
M2a scope.

**Rejected.**

- **Keeping minutes and adding sub-minute precision only where M3
  needs it** (a mixed-grain clock) — rejected because a clock whose
  grain depends on which step type is running is a subtler and more
  surprising property than "the clock is seconds, always," for a
  saving (avoiding one seed perturbation) ADR-0009 already says isn't
  worth engineering around.
- **A full IANA timezone database with real DST transition rules** —
  rejected for v1 as more machinery than the stated need (a
  DST-crossing corpus as premium *future* content, not baseline
  traffic) justifies; a fixed offset is the simplest thing that lets
  every v1 corpus state its own UTC relationship unambiguously in the
  manifest.
- **Building windowed arrival generation and warm-up trimming this
  session** — rejected because this is a docs/ADR session (see this
  document's own header discipline); the API shape is captured so
  M2a's implementation has a target, not so this session can skip
  ahead of it.

---

## ADR-0012 — Decide-time step rejections become a `:step-rejected` ground-truth event; cancel-reinstatement routes through the allocation ladder (v2)

**Status:** Accepted (author-ratified 2026-07-26) — design capture,
surfaced by M2b's landed session; no code lands with this ADR.

**Context.** M2b's `InjectChurn` property-testing surfaced a real
runtime behavior, already recorded in `.agents/plans/roadmap.md`'s M2b
entry: a churn-inserted step that is statically legal per the
applicability oracle can still be rejected at execution time by live
world state InjectChurn had no visibility into — e.g. a bed a
cancel-discharge would reinstate into was reclaimed by someone else's
admission in the meantime. Today that rejection is a bare no-op: the
step simply doesn't happen, and the ground-truth log carries no record
that it was ever attempted or why it didn't occur. This is a narrower
instance of a more general gap this project's own architecture makes
conspicuous by contrast: `decide` (ADR-0008) already distinguishes
"this step happens" from "this step doesn't," but only the first
outcome leaves a trace. A log that is supposed to be the single
authoritative record of a run (ADR-0002, ADR-0008) has a blind spot
exactly where a tester might most want visibility — a hospital's live
world state rejecting an attempted action is itself an operationally
real fact, not merely an absence of one.

**Decision.**

1. **A `:step-rejected` event enters the ground-truth log** whenever
   `decide` rejects an attempted step at execution time (not merely
   whenever the applicability oracle would have refused it up front —
   this event exists for exactly the runtime-visibility gap M2b's
   cancel/bed-swap/merge rejections surfaced, where the oracle said yes
   and the live world said no). It carries `:participants` (per
   ADR-0010's shape), the attempted step, and a reason. It is **truth
   about the run, not a message-bearing event** — no
   `message-type-registry` entry, ever, by design: a real hospital's
   ADT feed does not carry a message for "the system almost tried to
   do something and didn't," so an emitter rendering one here would be
   inventing wire traffic no real interface would ever see. It exists
   for the ground-truth log's own consumers — `check.clj`'s invariant
   catalog and any test harness reading the log directly — not for
   `EmitHL7`.
2. **Invariants may reference `:step-rejected` events.** A future
   invariant asking "did this run ever attempt an illegal reinstatement
   and silently drop it" becomes answerable by querying the log instead
   of unanswerable by construction, the same glass-box-auditability
   rationale (`docs/problem-statement.md`'s claim 3, "does the stream
   contradict itself") this project already applies to every other
   event type.
3. **v2 refinement, captured alongside: cancel-reinstatement routes
   through the allocation ladder instead of no-opping.** Today's
   conservative behavior (`ehr-testing-sim.churn`'s M2b landing) treats
   a blocked `:cancel-discharge` reinstatement as a dead end — the step
   fails and (until decision 1 lands) leaves no trace. A real hospital
   does not behave this way: cancelling a discharge and finding the
   original bed already reclaimed does not mean the cancellation fails,
   it means the patient needs a *different* bed, so the reinstatement
   re-enters `ehr-testing-sim.facility/allocate`'s existing ladder
   (`docs/operational-models.md`) exactly the way a fresh admission
   would, rather than being special-cased as a no-op. This is captured
   here as the correct future behavior, not built — it depends on
   decision 1 existing first (a rejected first attempt should still be
   visible even when the ladder retry succeeds), so both land together
   whenever this ADR's implementation milestone is scheduled.

**Rejected.**

- **Silently continuing to no-op rejected steps** — rejected because it
  reintroduces, for a different event class, exactly the "state that
  doesn't originate from the log" problem ADR-0008 already eliminated
  for patient state: a rejection is a fact about the run, and a log
  claiming to be authoritative shouldn't have facts it doesn't contain.
- **Rendering `:step-rejected` as an HL7 message** (e.g., a synthetic
  ACK-reject or a house Z-segment) — rejected because no real ADT feed
  carries a message for an attempt that never became a real hospital
  action; inventing one would misrepresent what real wire traffic looks
  like, the opposite of this project's realism goal.
- **Making cancel-reinstatement retry the ladder immediately, ahead of
  `:step-rejected` existing** — rejected because a silent retry that
  happens to succeed would hide the fact that the *first* placement
  attempt failed, which is exactly the visibility gap this ADR exists
  to close; the two decisions are sequenced, not independent.

**Consequences.** `docs/patient-state-model.md` gains a short capture
paragraph describing `:step-rejected`'s shape and its M3-adjacent
implementation timing. No code lands with this ADR: implementation
(the event, its schema, `check.clj` support, and the ladder-retry
refinement) is scheduled M3-adjacent, per `.agents/plans/roadmap.md`,
not in this session. A future session implementing this ADR should
treat `message-type-registry`'s deliberate non-entry for
`:step-rejected` as load-bearing, not an oversight to "fix" by adding
one.

---

## ADR-0013 — GMF module vendoring resolved: a curated, hashed subset in `resources/modules/`, not a lockfile

**Status:** Accepted (author-ratified 2026-07-27) — design capture for
Milestone M5 (`.agents/plans/roadmap.md`); no code or resources land
with this ADR. See [`docs/gmf-interpreter.md`](../docs/gmf-interpreter.md)
for the interpreter design this vendoring decision feeds.

**Context.** [ADR-0003](#adr-0003) named a trigger, deferred rather
than decided at scaffold time: "adopt [a pinned-artifact/vendoring
pattern] once Synthea modules (or another large upstream artifact)
actually land in this repo." `docs/sim-theory.md`'s open question #1
has carried `gmf-module-set`'s catalytic target as **OPEN** since the
theory file first named the resource — target 1 (`artifacts.lock`, a
pinned lockfile fetched at build/run time, the treatment
`snomed-icd10-map` already gets) or target 3 (hashed,
repo-authored-or-derived config vendored directly into the repo, the
treatment `order-profiles`/`provider-pool`/`demographics-tables`
already get) — deliberately left unresolved until a real module
landing forced the question. Milestone M5 (`RunModules` +
`CompileTrajectory`, `docs/sim-theory.edn`) is that landing, and
`docs/gmf-interpreter.md`'s own candidate-module survey (its
appendix) is the first session to read real Synthea module JSON
against this project's actual interpreter scope, giving this decision
something concrete to be decided against rather than decided in the
abstract.

**Decision.**

1. **Target 3: a small, curated, hashed subset vendored into
   `resources/modules/`.** Not target 1 (a lockfile pinning URLs and
   letting a build step fetch module content at need). Synthea's 85
   GMF modules (`notes/facts-register.md` F2) are Apache-2.0 and this
   project takes only a handful — the same shape
   `resources/order-profiles.edn`/`resources/demographics/` already
   established for other config this project vendors rather than
   fetches, extended here to a directory of module JSON files instead
   of a single EDN table.
2. **Rationale, three independent arguments converging on the same
   target:**
   - **Glass-box law.** `docs/problem-statement.md`'s Cross-Cutting
     Arguments (Provenance) and `docs/sim-theory.edn`'s own
     `RunModules` law ("glass-box traceability: every trajectory event
     cites the module id and state name that produced it, so any
     output event is auditable back to inspectable module JSON") both
     presuppose the module content a reader is auditing is sitting in
     this repo's own tree, not resolved indirectly through a lockfile
     and a fetch step at read time. A vendored file is inspectable by
     `git show`; a lockfile entry is inspectable only by also trusting
     whatever the fetch step retrieves matches what the lock pinned.
   - **A curated few keeps review honest.** This project's own
     no-hidden-modules corollary (`docs/sim-theory.md`'s IR-transforms
     section, restated in the M5 roadmap entry) already commits every
     lifecycle behavior this repo runs to being explicit and listable.
     A lockfile pinning all 85 modules (or any large subset) at once
     is exactly the shape that corollary warns against in spirit, even
     though it names always-on invisible *execution* rather than
     vendoring specifically: nobody reviews 85 files in one diff, the
     same way nobody reads a `package-lock.json` line by line. A
     handful of files in one PR, each individually reviewable, is the
     vendoring-side instance of the same discipline.
   - **No artifact-lock machinery exists here to justify building for
     a handful of JSON files.** ADR-0003's own trigger conditions list
     "pinned-artifact or vendoring pattern (tools' `artifacts.lock.edn`
     / vendor-corpus pattern) — adopt once Synthea modules... actually
     land." This project has never built lockfile-resolution
     machinery, and standing it up (a fetch step, a lock schema, a
     verification step against pinned hashes at fetch time) is real
     engineering weight to carry for what this milestone's curation
     criterion (below) keeps to a handful of files — weight that
     buys nothing a flat `resources/modules/` directory with a NOTICE
     file doesn't already buy at zero build-step cost.
3. **Provenance per vendored module, recorded in a
   `resources/modules/NOTICE` file** (the same role
   `resources/demographics/NOTICE` already plays for the demographics
   tables, extended to genuinely-vendored-not-hand-curated content this
   time): for each module JSON vendored, its upstream URL
   (`raw.githubusercontent.com/synthetichealth/synthea/<sha>/src/main/resources/modules/<name>.json`),
   the exact commit SHA it was fetched at, and a content hash
   (SHA-256) of the vendored file as fetched — the same three facts
   `snomed-icd10-map`'s own pinning already requires of target 1,
   applied to target 3's files instead of a lockfile entry. This is
   the concrete meaning of "hashed" in target 3's own name
   (`docs/sim-theory.md`'s Catalytic resolution table) for this
   resource: the hash lives in the NOTICE file, not in a lockfile,
   because there is no lockfile.
4. **Curation criterion: a v1 module must be expressible in the
   interpreter subset `docs/gmf-interpreter.md` defines.**
   Concretely: encounter-bearing (the module reaches at least one
   `Encounter`/`EncounterEnd` pair, so it exercises the trajectory→IR
   mapping this milestone's laws are actually about) and a modest
   state-type surface (its states are drawn overwhelmingly from that
   document's v1 state-type list, with any state types outside it
   confined to a small, excludable, non-load-bearing tail — not
   scattered through the module's core diagnostic or therapeutic
   logic). This is an operational test, not a vibe: `docs/gmf-
   interpreter.md`'s own candidate-module survey applies it to three
   real modules read for this milestone and its own appendix table is
   the worked example of applying it. A module that fails this
   criterion is not vendored in v1, full stop — it waits for whichever
   future milestone extends the interpreter subset enough to cover it,
   the same "don't build machinery ahead of a module that needs it"
   discipline this whole ADR is about.
5. **Explicit path to revisiting: a lockfile, if the vendored set ever
   grows past roughly ten modules.** The curated-few argument above is
   a claim about *this* milestone's scale, not a permanent ceiling —
   if a future milestone's clinical-content ambitions genuinely need
   dozens of modules rather than a handful, the review-honesty and
   glass-box arguments above start trading off against real
   duplication and update-friction costs a lockfile is built to solve,
   and target 1 becomes the better answer. Ten is a round, deliberately
   approximate trigger (not a hard invariant this project checks
   mechanically) — a future session crossing it should read this ADR,
   confirm the tradeoff has actually flipped rather than assuming it
   has, and record the flip as its own superseding ADR per this
   project's standing rule (never silently revert an Accepted ADR).
6. **One hand-written module ships as the interpreter's own unit-test
   fixture — ours, not Synthea's.** The GMF interpreter's red tests
   (test-first, ADR-0004) need a module whose every state this project
   controls, so a test can assert exact trajectory-event output against
   a known-small input without also depending on a vendored file that
   might itself change if this ADR's own curation criterion is later
   revisited. This fixture is hand-authored GMF JSON, covering v1's
   state types directly rather than borrowed from any real vendored
   module; `docs/gmf-interpreter.md` names its intended coverage. It is
   test fixture content (`test/ehr_testing_sim/fixtures/`, the same
   directory the pinned-seed regression already lives in), not vendored
   upstream content, and carries no NOTICE obligation for exactly that
   reason — it is this project's own authored data, not Synthea's.

**Rejected.**

- **Target 1 (a lockfile), resolving the OPEN question the other
  direction.** Rejected on the three grounds in the Decision above —
  glass-box auditability, review-honesty at this milestone's actual
  scale, and no existing lockfile machinery to amortize the build cost
  against. Not rejected as *wrong in general*: point 5 names the
  explicit condition under which this project expects to revisit and
  likely choose it instead.
- **Vendoring all 85 modules now, "to have them available."**
  Rejected for the same review-honesty reason target 1 is rejected,
  applied to target 3 instead of target 1: a curated few is the point,
  not an accident of this milestone's limited time. Vendoring
  everything now would also front-load work the curation criterion
  (point 4) says most of those 85 modules would fail anyway, until the
  interpreter subset grows to cover whatever each of them individually
  needs.
- **Using a real vendored module (rather than a hand-written one) as
  the interpreter's own unit-test fixture.** Rejected because a real
  module's states, once this ADR's curation criterion or the
  interpreter's own v1 subset changes in a future milestone, would
  drag the interpreter's own red-test suite along with it — coupling
  test-suite stability to an upstream content decision this project
  doesn't control. A hand-written fixture is stable by construction:
  this project owns every line of it.

**Consequences.** `docs/gmf-interpreter.md`'s candidate-module survey
(its own appendix) names which single module vendors first under this
ADR's criterion, for author ratification alongside this ADR's own
acceptance. No code or resources land with this ADR: the actual
vendoring (fetching the recommended module's JSON, writing
`resources/modules/NOTICE`, computing its hash) and the hand-written
fixture module are M5b/M5a implementation work respectively, per
`.agents/plans/roadmap.md`'s own M5a/M5b split (this session's Task
3). `docs/sim-theory.md`'s open question #1 is RESOLVED by this ADR
the same way ADR-0008 resolved open question #3: the original entry
stays in place per this project's append-don't-erase convention, with
a resolution note pointing here; `docs/sim-theory.md`'s Catalytic
resolution table gains this ADR's citation in `gmf-module-set`'s own
row, replacing "OPEN."

---

## ADR-0014 — Sim runs no external acceptance instruments; that is the consumer's job

**Status:** Accepted (author-ratified 2026-07-27)

**Context.** Milestone M6's own Task 3 landed
`test/ehr_testing_sim/blaze_integration_test.clj`: skip-when-absent,
but when a local `samply/blaze` FHIR server was reachable, it POSTed a
run's own end-of-run Bundle(s) and asserted round-trip equivalence
against Blaze's own verdict. This was reasoned, at the time, as
EmitState's own instance of "never graded on our own homework"
(`README.md`'s own phrase for claim #1's proof strategy,
`docs/problem-statement.md`). On reflection the analogy doesn't hold:
claim #1's actual proof strategy is validating the emitted CORPUS with
independent tooling **someone else runs** (NIST's HL7v2 conformance
tools, the HAPI parser, `notes/facts-register.md` F6) — this repo has
never itself opened a socket to NIST's or HAPI's own infrastructure to
do that checking; the checking happens in `ehr-testing-tools`, against
artifacts sim produced and handed over as files. The Blaze test broke
that symmetry: it made *this* repo the thing dialing out to an external
server, the one shape claim #1's own proof strategy never asked sim
itself to perform. It also left this repo with its only arbitrary-URL
write path (`BLAZE_BASE_URL`, an environment variable naming a server
this process would `POST` to) — a component this project's own safety
argument (`docs/problem-statement.md`'s claim #7, "no PHI ever," proof
by construction) had never had to account for before, because nothing
else in sim ever sends data anywhere; it only writes files a caller
chooses to read.

**Decision.**

1. **External acceptance instruments live with the consumer, never
   with sim.** This extends claim #1's own already-accepted structure
   (independent-parser validation happens in `ehr-testing-tools`, not
   here) to FHIR: whatever real-server round-trip a state-document
   emitter's output eventually needs is `ehr-testing-tools`' own
   consumer-loop work (its own gate/corpus machinery, the same
   division of labor its own ADRs already establish for v2 —
   `.agents/plans/roadmap.md`'s own "Consumer plan: sim doesn't
   validate itself in a vacuum" section, restated here as a binding
   decision rather than a roadmap note). `test/ehr_testing_sim/
   blaze_integration_test.clj` is deleted; `notes/facts-register.md` F13
   (the JDK-8/HttpURLConnection finding that test motivated) is
   annotated superseded, not deleted, per this register's own
   append-only discipline.
2. **Sim's own in-repo FHIR evidence is the serverless set, and stops
   there.** The emitter-coherence property
   (`ehr-testing-sim.v2-replay-test`, `docs/event-sourcing.md`), the
   cross-emitter id sub-law
   (`emit-state-test/fhir-patient-id-and-active-mrn-resolve-to-the-
   same-hl7-identity`), and shape validation (Bundle JSON round-trips
   through `clojure.data.json`, `emit-state-test`'s own
   `patient-bundle-round-trips-through-clojure-data-json`) are the
   complete list of what this repo itself proves about its FHIR output.
   Anything requiring a real FHIR server's own verdict is out of scope
   here, by design, not merely unbuilt.
3. **Safety consequence, stated plainly:** the simulator writes files
   only. Nothing in `ehr-testing-sim` sends data to any server, and no
   component here accepts an arbitrary server URL as input — the
   `BLAZE_BASE_URL` environment variable this ADR removes was the one
   exception, and it no longer exists. The family's sole POSTing
   component will be `ehr-testing-tools`' own managed `fhir-sink` (a
   consumer-side component that starts what it talks to, rather than
   dialing out to a caller-named URL) — recorded here so a future
   session doesn't reintroduce an arbitrary-URL write path in sim
   without first reading why this one was removed.
4. **The shelved guard-ladder spec is tools' to pick up, not sim's.** A
   guard-ladder design for gating writes to an external URL was
   considered and shelved during this reversal; it is recorded here as
   available reference material for `ehr-testing-tools`, should that
   repo ever add an external-URL mode to its own `fhir-sink` (rather
   than the start-what-it-talks-to shape decision 3 names as the
   current plan) — sim itself has no further use for it, having no
   external-URL write path left to gate.

**Rejected.**

- **Keeping the Blaze test as an opt-in, rarely-run check.** Rejected
  because the objection isn't that the test runs too often or is
  expensive — it's that its very existence means sim POSTs to a
  server at all, the structural fact this ADR removes, not a frequency
  problem a skip-when-absent guard already mitigates.
- **Moving the Blaze test to `ehr-testing-tools` verbatim.** Considered
  and set aside for this session: `ehr-testing-tools` already has its
  own consumer-loop and gate machinery this capability belongs beside
  (`.agents/plans/roadmap.md`'s own "Consumer plan" section, this
  ADR's decision 1); porting this session's own Blaze-specific test
  file verbatim would prejudge that repo's own design for how it wants
  to shape its `fhir-sink` acceptance work, which is that repo's call,
  not this one's to make on its behalf.

**Consequences.** `README.md`, `docs/GLOSSARY.md`, and
`.agents/plans/roadmap.md` are swept of their M6-session Blaze
references (reworded to state this ADR's decision, not merely
deleted, so a reader lands on the reasoning rather than a gap).
`docs/simulate-your-facility.md`'s own "what if synthetic data ever
reached a real system" FAQ answer states the same safety consequence
(decision 3) as its own closing line. Every resource `ehr-testing-sim.
emit-state` produces additionally carries a standard HTEST security
label and a generator/run-id tag (`meta.tag`) — landed alongside this
ADR, not a substitute for it: labeling test data and refusing to POST
it anywhere are two independent safety properties, and this project
now holds both. A new `sim identifiers` CLI verb (config + seed → the
complete EDN inventory of every identifier a run's own output
contains) is this project's own answer to "how would we find and
remove it, if this ever reached a real system regardless" — enumerable
by construction, per this project's own determinism guarantee, rather
than a promise resting on the external-write path this ADR just
removed.

---

## ADR-0015 — Going public: the visibility decision and its pre-recorded triggers, executed in one place

**Status:** Accepted (author-ratified 2026-07-27)

**Context.** ADR-0003 named the trigger for demoting `pack-push` to
dormant ("when this repo gets a public GitHub remote") and for
adopting CI ("once this repo has a public GitHub remote to run it
against"); ADR-0006 reaffirmed that a *private* remote (added that
session) does not meet either trigger and that "a future session that
flips this repo's visibility to public records that as its own ADR,
exactly as ADR-0003 already anticipated." This is that session. A
go-public session necessarily touches several small, previously-decided
facts at once — the pack transport's own reason to exist, the pre-
release amend allowance, and the standing risk of conflating "public"
with "released" — and this project's own convention (never silently
revert an Accepted ADR; supersede with a new numbered record) means
each deserves a decision recorded here rather than a quiet edit to the
file that states it. Task 0 of this session's own audit (git history,
full reachable history of the public `pragsmike/packs` transport, and a
supplementary grep sweep for hostnames/paths/tokens/emails —
`notes/facts-register.md` F15) found nothing that blocks this decision.

**Decision.**

1. **`pack-push` demotes to dormant.** The trigger ADR-0003 named and
   ADR-0006 reaffirmed is now met: this repo's GitHub remote is public.
   Public raw URLs (`raw.githubusercontent.com/pragsmike/ehr-testing-sim/...`)
   are now the chat-read path for any session that needs this repo's
   current state — the same demotion `ehr-testing-tools` recorded at its
   own ADR-0008, for the same reason. `make pack`/`make pack-skills`
   remain available (a local, offline snapshot is still occasionally
   useful) but `make pack-push` is no longer a required ceremony step;
   the session-end ceremony simplifies to **commit → `git push origin`**.
   `Makefile`'s header comment and `AUTHORS-GUIDE.md` section 2 are
   updated accordingly (below); `docs/GLOSSARY.md`'s **Ceremony** entry
   and `.agents/plans/roadmap.md`'s own "Packs demotion" trigger note are
   marked fired, pointing here.
2. **The pre-release amend allowance ends, dated.** `AUTHORS-GUIDE.md`'s
   `--force-with-lease` clause was scoped to end "at first release or
   second contributor" — neither has happened — but a public remote
   introduces a THIRD condition that reasoning didn't anticipate: once
   history is fetchable by anyone, a rewritten commit can silently
   invalidate a clone or fork nobody here knows exists. Public history
   is append-only in practice from this date forward, regardless of
   release status; the allowance closes 2026-07-27, and `AUTHORS-GUIDE.md`
   is updated to say so (below), not silently — the original two
   conditions stay recorded as what the allowance was ORIGINALLY scoped
   to end at, since this ADR adds a condition rather than replacing them.
3. **ADR-0009's clause is restated, not changed, to prevent a
   misreading.** Going public is not a release. ADR-0009's own seed-
   stability policy — same config + seed is byte-identical *within a
   generator version*, and that guarantee is stated to hold only up to
   this library's *first published release* (a Clojars/Maven
   coordinate, per that ADR's own "before this library's first
   published release" framing) — is unweakened and unchanged by this
   ADR. Nothing about today's visibility flip is a release: no tag
   exists, no Clojars/Maven artifact exists, no version number beyond
   `deps.edn`'s own unversioned git-coordinate dependency shape has been
   minted. A future session or external reader who sees a public repo
   and assumes "public" means "released, so seed-stability now extends
   across generator versions" would be wrong; this decision states that
   plainly so the confusion has a canonical answer to point to rather
   than being re-litigated per session.
4. **The release gate stays a deferred, named ledger — not opened
   today.** Going public and cutting a release are different triggers
   (decision 3), and this session deliberately does not fire the
   second one. What a future release-gate session will need to decide,
   named here so it has a checklist rather than a blank page: a version
   tag and its scheme (this project has never minted one), a GitHub
   Release, Clojars/Maven coordinates (`deps.edn` currently has none to
   publish under), a CHANGELOG, and the point at which ADR-0009's
   within-version guarantee starts being read literally by external
   consumers pinning a released coordinate rather than a git SHA. None
   of this is designed or decided today; it is named so "public" and
   "released" stay two separate, non-conflatable facts about this
   repo's history, per decision 3.

**Rejected.**

- **Deleting `pack-push` entirely, rather than demoting it.** Rejected
  for the same reason `ehr-testing-tools` kept its own dormant
  `pack-push` rather than removing it at its ADR-0008: a local, offline
  pack is still occasionally useful (no network dependency, a single
  file to paste into a context window that can't fetch a URL), and the
  mechanism costs nothing to leave in place once it's no longer a
  required step.
- **Editing ADR-0003's, ADR-0006's, or ADR-0009's own bodies to reflect
  today's decision.** Rejected by this project's own standing rule
  (never silently revert an Accepted ADR — supersede with a new
  numbered record). Each of those ADRs' bodies stays exactly as
  originally accepted; this ADR is the supersession, and any place those
  ADRs are read for their own now-fired triggers should be read
  alongside this one, not instead of it.
- **Treating "public" and "released" as the same trigger**, and
  therefore letting ADR-0009's guarantee scope silently extend today.
  Rejected because it would misstate a fact this session has no
  standing to change — no release-gate work (decision 4) happened this
  session — and because the misreading is exactly the kind of thing
  this project's own culture (per `AGENTS.md`'s "do not invent facts")
  exists to prevent from happening by omission.

**Consequences.** `Makefile`'s header comment and recipe help text,
`AUTHORS-GUIDE.md` sections 1–2, `docs/GLOSSARY.md`'s **Ceremony**
entry, and `.agents/plans/roadmap.md`'s "Packs demotion" trigger note
are all updated in this same session, landing alongside this ADR
(this project's own co-landing convention, extended from code+invariant
pairs to decision+mechanism pairs). `notes/facts-register.md` gains no
new F-row from this ADR itself (the audit and schema-validation
findings that informed it are already F15–F18); a future session
reading this repo's own git history for "when did this go public" has
this ADR as the answer, dated and reasoned, rather than inferring it
from a bare commit that flips a GitHub setting this repository's own
tree cannot record.

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

# AGENTS.md

> Primary instruction surface for AI coding agents working in this
> repository. Read by tools that support the `AGENTS.md` convention
> (Codex, OpenCode, and others). Claude Code users: see `CLAUDE.md`,
> which points here.

**Users of the software this workspace builds** (not contributing code
or docs): see [`SETUP.md`](SETUP.md) instead — installation and
verification, none of this file's contribution discipline. This file
governs contribution sessions (PRs, commits, docs edits, migration
work); nothing here applies if you're only running a built artifact.

## Session mode and ceremony: read this before your first git operation

**All git operations — especially `git commit` — must be run from WSL,
never from native Windows**, enforced by `.githooks/pre-commit` once
`git config core.hooksPath .githooks` has been run for a given clone
— that config is per-clone and does not travel with the repo:

```sh
git config core.hooksPath .githooks
```

`.githooks/pre-push` additionally gates on a clean `gitleaks detect`
scan and `clojure -M:poly check` being green -- tests are CI's job, not
the push gate's (`notes/ADRs.md` ADR-0003). See `AUTHORS-GUIDE.md` §1
for the full rationale and gitleaks install instructions.

**A "regression-oracle" claim means `bin/regression-oracle`'s own
SHA-256 digests across a disposable worktree, never a test-count or
assertion-count comparison** (`notes/ADRs.md` ADR-0030 J2, the
`build-session` skill's own VERIFICATION section has the full rule).

**The standing default is R30: a session commits and pushes at each
checkpoint, unattended** (`notes/ADRs.md` ADR-0007, R-F ratified by
ADR-0023, 2026-08-01) — the staging-hygiene ritual (`AUTHORS-GUIDE.md`
§1: `git diff --cached --stat` reviewed before every commit, anything
outside the checkpoint's own scope unstaged first) runs before every
commit regardless. **Prepare-only** (a session prepares the working
tree and proposes commit messages, never itself running `git commit`,
`git push`, `git merge`, or `gh`) is the exception a session's own
prompt must state explicitly, in that chat, at the start — a live,
scoped choice for that session, not a rule either mode carries forward
silently to the next one. One class of action stays the author's alone under either mode without
exception: **repo-level `gh` mutations**
(create/delete/settings/visibility — the `pragsmike/packs` precedent,
`sim's AUTHORS-GUIDE.md` §2), **git surgery**, and **placing external
documents**. **`stable-*` continuity tags are a SESSION ACT** (tag law,
`notes/ADRs.md` ADR-0057 AR-T-1, 2026-08-06 — the law restated once,
canonically, after two dated amendments, ADR-0049's AR-AU-0 and
ADR-0051's AR-F2-0, had left this surface and others out of sync with
each other and with `.agents/rulings.md#R-stable-tag-author-only`; ADR-0003's
original author-only trust-boundary reasoning is superseded in scope
for this one class of tag, not erased — the design channel's own
landing verification is now that boundary, and the tag is its
mechanical consequence): a session creates and pushes a `stable-*` tag
(i) when its own prompt licenses a SPECIFIC tag at a SPECIFIC commit, a
license the design channel issues only after verifying the landing it
names, and (ii) for its own predecessor's design-channel-verified
stable point, as standing ceremony, without bouncing back to the
author. **Deferring a licensed tag is now the deviation** and needs a
disclosed reason — the inverse of the prior default. The author may
always tag directly, licensed or not; a tag already present at the
exact commit and message a session would otherwise have created is
verified and disclosed, never re-created. Every other tag class —
release `v*` tags especially — stays AUTHOR ACTION: publication itself
is author-gated, so its tags are too. Agents working in this
environment hold ambient authenticated `gh` credentials for exactly the
repo-level mutations above — the standing rule, unchanged by either
mode, is that those credentials do not get used off an agent's own
initiative. Git surgery and placing external documents stay the
author's alone, unchanged, regardless of ceremony mode — the
`stable-*` carve-out above narrows nothing else.

## Reading this repo

Per-task-class reading sets (onboarding / corpus / sim / judge / docs,
each a budgeted path list) live in
[`.agents/reading-sets.edn`](.agents/reading-sets.edn) (charter R-D,
`.agents/plans/2026-08-01-agent-ux-charter.md`, landed migration
session 4, 2026-08-02) — `ehrt.docs-tooling.reading-set-budget-test`
gates every path resolving and every set's real line count staying
within its own `:budget-lines`. Budget numbers are the measured
actuals of each set as composed, not yet author-ruled targets (charter
§6). Also start from this file's own Structure section below,
`docs/dev/architecture.md` for the fuller workspace map, and
`.agents/` (routing below) for durable session context.

## Structure

This is a [Polylith](https://polylith.gitbook.io/polylith) monorepo,
top namespace `ehrt`, consolidating the `ehr-testing-*` family into one
workspace, one development REPL, many deployable artifacts. See
[`docs/dev/migration/polylith-brief.md`](docs/dev/migration/polylith-brief.md)
for the Polylith architecture reference this migration was planned
against.

- **Component** — a chunk of domain behind one `interface` namespace
  (`components/<name>/`). Reusable, swappable, invisible to other
  bricks below its own `interface`.
- **Base** — a thin entry point to the outside world: CLI dispatch, an
  HTTP handler (`bases/<name>/`). No interface of its own.
- **Brick** — component or base, interchangeably, when the distinction
  doesn't matter.
- **Project** (`projects/<name>/deps.edn`) — the set of bricks that
  make up one deployable, wired by `:local/root`.
- **Development project** — the root `deps.edn`; its `:dev`/`:test`
  aliases see every brick at once, one REPL for the whole workspace.

**`poly ws get:...` is the agent's primary source of workspace truth**
— the workspace as queryable EDN data, not a report to eyeball. Prefer
it over parsing `poly info`'s pretty-printed output: `clojure -M:poly
ws get:keys`, `get:components:keys`, `get:changes:changed-or-affected-
projects since:release`. `clojure -M:poly check` and `clojure -M:poly
test` are the enforcement surface — a brick that reaches into another
brick's implementation namespace (not its `interface`) fails `check`,
not a code review.

**Components:** `components/kernel` (shared foundation, incl.
`ehrt.kernel.result` — the result-not-throw envelope); `components/judge`
(the verdict vocabulary: report/finding/verdict-cache); the three gate
engines behind it — `components/judge-v2-hapi`, `components/judge-fhir-official`,
`components/judge-v2-nist`; `components/provenance` (the ManifestV0/V1/
V1_1 schema family, moved out of `components/corpus` -- sim split B,
M1, 2026-08-04, `.agents/plans/2026-08-04-sim-split-b-plan.md`; the
single acyclic home both corpus and sim depend on, depending on
neither itself); `components/corpus` (corpus domain:
generate/mutate/intake/operators, interface designed from live
consumers); `components/corpus-io` (transport/IO: sources, sinks,
spooling, framing codecs); `components/docs-tooling` (dev-time-only
doc/lint tooling); `components/palgebra` (conformance-gating notation
and rendering); `components/sim-model` (pathway/facility/persona/config
schemas and sampling, extracted from `components/sim`, sim split S1,
`.agents/plans/2026-08-02-sim-split-plan.md`); `components/sim-engine`
(the discrete-event simulation core plus its churn/order-profiles
catalytics, extracted from `components/sim`, sim split B stage M2,
2026-08-04, `.agents/plans/2026-08-04-sim-split-b-plan.md`,
`notes/ADRs.md` ADR-0043); `components/sim-trajectory`
(GMF module loading/interpretation and CompileTrajectory, extracted from
`components/sim`, sim split S2, same plan); `components/sim-emit-hl7`
(the HL7v2 emitter, the v2-replay wire-side accumulator, and site
profiles, extracted from `components/sim`, sim split S3 / GMF coverage
Wave D stage D0, `notes/ADRs.md` ADR-0029); `components/sim-emit-fhir`
(the state-based FHIR R4 Bundle emitter, sim-emit-hl7's own sibling as
a rendering accent over folded state rather than the event log,
extracted from `components/sim`, sim split B stage M3, 2026-08-04,
`.agents/plans/2026-08-04-sim-split-b-plan.md`, `notes/ADRs.md`
ADR-0043); `components/sim-check` (the invariant catalog, extracted
from `components/sim`, sim split B stage M4, 2026-08-04, same plan,
`notes/ADRs.md` ADR-0043 — the fifth and last brick of the
decomposition; residual `components/sim` is now pure orchestration);
`components/sim`
(deterministic, seeded generator of synthetic hospital traffic);
`components/oracle` (the regression-oracle digest producer, extracted
from `bin/oracle-src`, standing-equipment promotion, 2026-08-05,
`notes/ADRs.md` promotion ADR AR-P-2 — no shipped project depends on
it, `bin/regression-oracle`'s own per-worktree synthetic classpath is
its only real caller).

**Working on any of the seven `sim`/`sim-*` bricks above:** read
[`docs/dev/simulator-architecture.md`](docs/dev/simulator-architecture.md)
first — the decide/evolve doctrine, the mutable-state census (gated by
`ehrt.docs-tooling.sim-purity-lint-test`), and the palgebra reading of
the whole pipeline, an aid to understanding and a guardrail against
feature work drifting from the established theory (`notes/ADRs.md`
ADR-0108). Standing channel practice from this ADR: any session prompt
fencing sim-family `src` carries this doc in its own Read-first list.

**Bases:** `bases/cli` — thin CLI dispatch, `bin/ehrt` ("e-heart",
`ehr` stays reserved for future payload-EHR tooling).
**Projects:** `projects/ehrt-cli` composes every component and the
base above; `projects/conformance` and `projects/integration` are
base-less, exercising sim + corpus + corpus-io (+ the judge engines)
together, split by artifact-fetch dependency.

Full history of how this structure got here — the three-stage `tools`
split into `docs-tooling`/`corpus-io`/`corpus`, the judge-engine
extractions, the `sim-cli` retirement, the CLI rename — is not narrated
in this file; `notes/ADRs.md`'s own numbered records (grep `^## ADR-`
for the index) are the reasoning-of-record, and
`docs/dev/architecture.md`'s bricks table is the current structure map
this file's own tokens above are checked against
(`ehrt.docs-tooling.structure-currency-test`) — keep both in sync when
a brick is added, renamed, or retired.

**Deliberately out of scope, permanently:** `ehr-testing-guide` stays
out of this workspace entirely (ADR-0001, R2) — it is not a future
landing, don't plan namespace or directory shape around it arriving
later.

**Publishing:** `projects/ehrt-cli` will be this family's only
published library artifact, once a future session names Clojars/Maven
Central coordinates (ADR-0001 R3, named hole H5 — still open, author's
call). Everything else in this workspace — including sim — builds an
app artifact or nothing; never treat a component here as a publishable
library in its own right.

### `.agents/` — durable session context, routing

- [`.agents/session-records/`](.agents/session-records/README.md) —
  one dated record per non-trivial session, written as its last
  pre-push act (charter R-A, `notes/ADRs.md` ADR-0023).
- [`.agents/plans/`](.agents/plans/README.md) — what's landed and
  what's next, milestone grain. `roadmap.md` rows follow a gated
  contract (ADR-0144): `OPEN` | `CLOSED <date> <ADR|sha>` |
  `DEFERRED (trigger: ...)` | `EXTERNAL`, then a stable `**[slug]**`,
  then `PRIORITY n` under `## Next`; six lines a row, `CLOSED` only
  under `## Done`, and rows are cited `roadmap.md#<slug>`, never by
  line number.
- [`.agents/rulings.md`](.agents/rulings.md) — standing rules ONLY, one
  gated row each (ADR-0145): `- **R-<slug>** -- <rule> -- ADR-NNNN`,
  optionally `SUPERSEDED-BY R-<slug> (ADR-NNNN)`; three lines a row,
  cited `rulings.md#R-<slug>`. A decision executed once belongs to its
  own ADR, not here.
- [`.agents/memory/`](.agents/memory/README.md) — durable design
  lineage too expensive to re-derive, not decision-of-record (that's
  an ADR) or one verifiable claim (that's `notes/facts-register.md`).
- [`.agents/skills/`](.agents/skills/) — the sim/tools union (ADR-0005):
  `handoff`, `string-diagram`, `committee`, `find-skills`, `probe`,
  `repo-adaptation`, `review`, `scenarios`, `shared-skill-layout`,
  `wsl-windows-git-hygiene`.
- `.agents/prompts/` — session prompts, archived by the session they
  drove, indexed (charter R-A). New home going forward;
  `notes/prompts/*.md` stays the historical archive for prompts
  through 2026-08-01 (its own README points here).

## Discipline surface, mapped (ADR-0006)

- **GENERATED, never hand-edited:** `notes/ADRs.md` — the ADR index is
  rendered from the `notes/adr/` tree by `make adr-index` (ADR-0143);
  edit the ADR, regenerate.
- **Live, current, edit freely:** `notes/adr/` (the architecture
  decisions themselves); `notes/facts-register.md` (externally
  verifiable facts, `AUTHORS-GUIDE.md` §4); `.agents/memory/`,
  `.agents/plans/`, `.agents/session-records/`, `.agents/prompts/`
  (above); `.agents/skills/`.
- **Frozen provenance, read-only, never edited for new paths or
  namespaces:** `notes/sim/` and `notes/tools/` (each parent's own
  ADRs, facts-register, and `.agents/` tree as they stood at the
  merge). Cite them origin-qualified (`sim/ADR-0008`, `tools/F12`) when
  a live document references a pre-merge decision.

## Constraints

- **Dependency direction**: `components/corpus`/`projects/conformance`
  may depend on `components/sim`; `components/sim` must never depend
  on anything corpus-derived, enforced by `poly check`.
  `components/sim` also depends on `components/kernel` (ADR-0022) —
  kernel is not corpus-derived, so this is a new edge, not an
  exception. `components/sim` also depends on `components/sim-model`
  and `components/sim-trajectory` (sim split S1/S2, `notes/ADRs.md`
  ADR-0025), `components/sim-emit-hl7` (sim split S3, ADR-0029), and
  `components/sim-emit-fhir` (sim split B stage M3, ADR-0043) —
  `components/sim-model` must never depend on anything but
  `components/kernel`; `components/sim-trajectory` must never depend on
  anything but `components/sim-model` and `components/kernel`;
  `components/sim-emit-hl7` must never depend on anything but
  `components/sim-model` (never on `components/sim` or
  `components/sim-trajectory` themselves); `components/sim-emit-fhir`
  must never depend on anything but `components/sim-engine` (never on
  `components/sim`, `components/sim-model`, or `components/sim-trajectory`
  themselves); `components/sim-check` (sim split B stage M4, ADR-0043)
  must never depend on anything but `components/sim-engine`,
  `components/sim-model`, and `components/kernel` (never on
  `components/sim`, either emitter, `components/corpus`, or
  `components/provenance`).
- **No PHI, no real-person data, ever** — including in test fixtures
  and docs.
- **No CPT codes** (AMA-licensed). SNOMED CT, LOINC, RxNorm, ICD-10-CM,
  CVX only (`sim/F4`).
- **Fix-forward with disclosure** (ADR-0001, R10): do not invent facts
  about upstream sources or this migration's own history —
  `notes/ADRs.md` and `notes/sim/`/`notes/tools/` are the record; if a
  step's premise doesn't hold against the live tree, stop, record it,
  and ask rather than silently adapting. `docs/dev/way-of-working.md`
  §2 has the worked examples.
- **The fat-component disclosure** (ADR-0001 R5, ADR-0002 R13):
  `components/sim`'s and `components/palgebra`'s public interfaces are
  deliberately wide right now — re-exporting whatever their own
  callers used pre-merge, determined by grep, not interface-design
  judgment. Don't narrow them opportunistically, and don't read their
  width as a decomposition hint. `components/corpus`'s interface is
  the exception: ADR-0018's stage-3 split redesigned it from live
  consumers — its defs ARE design intent.
- **Discipline inherited from sim** (ADR-0001, R4 — sim's form wins
  where conventions differ): result-not-throw (every capability
  function returns `{:status :ok|:rejected|:error ...}`; exceptions are
  for programmer error only); determinism is law (all randomness in
  `components/sim` flows from the single seeded RNG in `engine/run`,
  no wall-clock, no hash-order dependence) (`sim/ADR-0002`); co-landing (a new engine
  step type ships with its invariants in the same change); test-first,
  properties for law-bearing constructs (`sim/ADR-0004`); the CLI-surface rule (demos
  and verification run through `bases/cli`, never through component
  internals directly).

## `.claude/`

Untracked, deliberately (carve-loss audit, author-ruled 2026-07-28: "don't
commit `.claude/settings.json`... `.claude/` stays untracked"). Do not
`git add` anything under it — **except `.claude/skills/`**, carved out
2026-08-01 (ADR-0024, `notes/ADRs.md`): a real-file mirror of
`.agents/skills/`, tracked because Claude Code does not read
`.agents/skills/` for skill discovery, only `.claude/skills/<name>/SKILL.md`.
Edit skills at their canonical home, `.agents/skills/`, never in
`.claude/skills/` directly — the mirror is generated/copied, not
independently authored, and `ehrt.docs-tooling.skill-mirror-currency-test`
fails the build if the two drift.

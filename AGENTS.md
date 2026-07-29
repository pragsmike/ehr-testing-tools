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

## Workspace overview

**Project:** `ehr-testing` — a [Polylith](https://polylith.gitbook.io/polylith)
monorepo consolidating the `ehr-testing-*` family of repos into one
workspace, one development REPL, many deployable artifacts. Top
namespace: `ehrt`. See [`docs/migration/polylith-brief.md`](docs/migration/polylith-brief.md)
for the Polylith architecture reference this migration was planned
against, and `notes/ADRs.md` ADR-0001 for what was actually decided
and why.

**Landed so far:** `components/sim` + `bases/sim-cli` (from
`ehr-testing-sim`) — a deterministic, seeded generator of synthetic
hospital traffic for testing EHR integrations, presented to users via
the `bin/ehrt sim run` mount (below); `sim-cli` itself is DEPRECATED,
not removed (R33, ADR-0009) — it keeps working, its own tests keep
running, but the user path never mentions it and this dev path marks
it deprecated here. **Retirement trigger:** retire `bases/sim-cli` and
`projects/sim` when a review finds no use outside their own tests —
not scheduled, just named, per `notes/facts-register.md`.
`components/tools` + `components/palgebra`
+ `bases/cli` (from `ehr-testing-tools`) — corpus construction
(generation, mutation, provenance) and conformance gating (HL7 v2,
FHIR); `components/kernel` + `components/judge` (ADR-0008) — the
shared foundation layer and the conformance-judging code, extracted out
of `components/tools`' own named hole H4 (ADR-0002 R14).
`projects/ehrt-cli` composes tools + kernel + judge + palgebra + cli,
`projects/conformance` is the base-less project exercising sim + tools
+ palgebra together (see `notes/ADRs.md` ADR-0002, closing named holes
H1–H3).

**The CLI is `ehrt`** ("e-heart", R32/ADR-0009, 2026-07-29) —
`bin/ehrt`, renamed from `ehr`; `ehr` stays reserved for future
payload-EHR tooling, not a stale spelling to clean up if you see it
elsewhere in this file's own citations of pre-rename history.

**Deliberately out of scope, permanently:** `ehr-testing-guide` stays
out of this workspace entirely (ADR-0001, R2) — it is not a future
landing, don't plan namespace or directory shape around it arriving
later.

**Publishing:** `projects/ehrt-cli` will be this family's only
published library artifact, once a future session names Clojars/Maven
Central coordinates (ADR-0001 R3, named hole H5 — still open, author's
call). Everything else in this workspace — including sim — builds an
app artifact or nothing; never treat a component here as a publishable
library in its own right. This resolves Polylith's own
one-library-per-workspace constraint (see the brief, §11 "Building
more than one library from one workspace").

## Before your first git operation: read this

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

**Git operations run under whatever mode the author set for the
session, in that session's own chat (ADR-0001 R6, superseded in place
by ADR-0007 R30).** The default remains R6's: a session prepares
working trees and proposes commit messages, and does not itself run
`git commit`, `git push`, `git merge`, or `gh`, unless the author has
said otherwise for that session, in that chat, in the moment — a
default that does not generalize backward onto earlier turns or
forward onto a future session that hasn't heard it. R30 names the
*standing alternative mode*, not a one-off exception to it: a session
the author has told to commit and push at each checkpoint does so
unattended, the staging-hygiene ritual (`AUTHORS-GUIDE.md` §1) still
run before every commit. Two classes of action stay the author's alone
under either mode: **tags** (the `stable-*` tag is the actual trust
boundary, ADR-0003) and **repo-level `gh` mutations**
(create/delete/settings/visibility — the `pragsmike/packs` precedent,
documented at sim's own `AUTHORS-GUIDE.md` §2). Agents working in this
environment hold ambient authenticated `gh` credentials for exactly
those repo-level mutations — the standing rule, unchanged by either
mode, is that those credentials do not get used off an agent's own
initiative.

## Workspace vocabulary

This is a Polylith workspace, not a plain multi-module repo — the
vocabulary and the tool are both load-bearing:

- **Component** — a chunk of domain behind one `interface` namespace
  (`components/<name>/`). Reusable, swappable, invisible to other
  bricks below its own `interface`.
- **Base** — a thin entry point to the outside world: CLI dispatch,
  an HTTP handler (`bases/<name>/`). No interface of its own.
- **Brick** — component or base, interchangeably, when the distinction
  doesn't matter.
- **Project** (`projects/<name>/deps.edn`) — the set of bricks that
  make up one deployable, wired by `:local/root`.
- **Development project** — the root `deps.edn`; its `:dev`/`:test`
  aliases see every brick at once, one REPL for the whole workspace.

**`poly ws get:...` is the agent's primary source of workspace truth**
— the workspace as queryable EDN data, not a report to eyeball.
Prefer it over parsing `poly info`'s pretty-printed output:
`clojure -M:poly ws get:keys`, `get:components:keys`,
`get:changes:changed-or-affected-projects since:release`. `clojure
-M:poly check` and `clojure -M:poly test` are the enforcement
surface — a brick that reaches into another brick's implementation
namespace (not its `interface`) fails `check`, not a code review.

## The fat-component disclosure

`components/sim`'s public contract, `ehrt.sim.interface`, is
**deliberately wide** right now (ADR-0001, R5) — it re-exports
whatever sim's own CLI, tests, and (previously) Makefile targets
called from outside their own namespaces, determined by grep against
the pre-merge repo, not by interface-design judgment. **Its width is
not design intent.** Don't narrow it opportunistically, and don't
treat the width of the interface as evidence about how sim's internals
should be decomposed into more than one component later — that's a
future, author-ruled extraction session's call, not a standing
invitation for cleanup.

The same discipline, same disclosure, applies to `components/tools`'
`ehrt.tools.interface` and `components/palgebra`'s
`ehrt.palgebra.interface` (ADR-0002, R13) — judge/corpus foundation
extraction is a future, ruled session's call (ADR-0002, R14), not
something this width hints at.

## Discipline inherited from sim (ADR-0001, R4)

Where sim's and tools' conventions differ, sim's form wins; this is
sim's own discipline, unchanged by the move:

- **Result-not-throw**: every capability function returns `{:status
  :ok|:rejected|:error :category ... :payload ...}`. Exceptions are
  for programmer error only.
- **Determinism is law**: all randomness in `components/sim` flows
  from the single seeded RNG in `engine/run`. No wall-clock, no
  hash-order dependence.
- **Co-landing**: a new engine step type ships with its invariants
  (and message type, where relevant) in the same change.
- **Test-first, properties for law-bearing constructs** (determinism,
  the invariant catalog, emitter derivability, schema round-trips).
- **The CLI-surface rule**: demos and verification commands run
  through `bases/sim-cli`, never through `components/sim` internals
  directly.

## Constraints

- **Dependency direction**: `components/tools`/`projects/conformance`
  may depend on `components/sim`; `components/sim` must never depend
  on anything tools-derived (inherited from sim's own ADR-0001; still
  the rule inside one workspace, enforced now by `poly check` rather
  than by being a separate repo). In practice `projects/conformance`
  consumes sim by subprocess only, never a classpath dependency
  (`tools/ADR-0013`, carried forward as provenance) — `poly/sim` does
  not appear in its `deps.edn`.
- **No PHI, no real-person data, ever** — including in test fixtures
  and docs.
- **No CPT codes** (AMA-licensed). SNOMED CT, LOINC, RxNorm, ICD-10-CM,
  CVX only.
- Do not invent facts about upstream sources or about this migration's
  own history — `notes/ADRs.md` and `notes/sim/` are the record; if a
  step's premise doesn't hold against the live tree, stop, record it,
  and ask (ADR-0001, R10) rather than silently adapting.

## Skills

Workspace-level skills live in `.agents/skills/` — the union of sim's
and tools' pre-carve skill sets (ADR-0005, carve-loss recovery session,
2026-07-28, R21): `handoff` and `string-diagram` from sim (sim's own
form wins on collision, per ADR-0001 R4's standing rule; `string-diagram`
also folded in a fix tools' own divergent copy had that sim's didn't —
pointing at `components/palgebra/` directly instead of vendoring a
stale, commit-pinned copy of it), plus eight tools-only skills:
`committee`, `find-skills`, `probe`, `repo-adaptation`, `review`,
`scenarios`, `shared-skill-layout`, `wsl-windows-git-hygiene`. See
ADR-0005 for the full collision-diff record. Handoffs, plans, and
durable memory follow the same `.agents/` layout sim used; see
`docs/way-of-working.md` for how this session's own conventions differ
from sim's 40-session history.

## The discipline surface, mapped (discipline-parity session, ADR-0006)

Where each piece of this workspace's own discipline apparatus actually
lives, since it's now spread across several directories with different
provenance:

- **Live, current, edit freely:** `notes/ADRs.md` (architecture
  decisions); `notes/facts-register.md` (externally verifiable facts,
  `AUTHORS-GUIDE.md` §4); `.agents/memory/`, `.agents/plans/`,
  `.agents/session-records/` (durable design lineage, the rolling plan,
  one dated record per session — `AUTHORS-GUIDE.md` §7 and each
  directory's own `README.md`); `.agents/skills/` (the sim/tools union,
  ADR-0005); `notes/prompts/*.md` (session prompts, flat, self-archived
  in place with a dated deviation note — this workspace's own form,
  distinct from either parent's live/archive split, see
  `notes/discipline-parity-audit.md` row M15).
- **Frozen provenance, read-only, never edited for new paths or
  namespaces:** `notes/sim/` and `notes/tools/` (each parent's own
  ADRs, facts-register, and `.agents/` tree as they stood at the
  merge). Cite them origin-qualified (`sim/ADR-0008`, `tools/F12`) when
  a live document references a pre-merge decision; never edit them to
  "fix" a stale path — the whole point is that they show what was true
  then.

## `.claude/`

Untracked, deliberately (carve-loss audit, author-ruled 2026-07-28: "don't
commit `.claude/settings.json`... `.claude/` stays untracked"). Do not
`git add` anything under it.

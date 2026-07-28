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
namespace: `ehrt`. See [`doc/migration/polylith-brief.md`](doc/migration/polylith-brief.md)
for the Polylith architecture reference this migration was planned
against, and `notes/ADRs.md` ADR-0001 for what was actually decided
and why.

**Landed so far:** `components/sim` + `bases/sim-cli` (from
`ehr-testing-sim`) — a deterministic, seeded generator of synthetic
hospital traffic for testing EHR integrations. `components/tools` +
`components/palgebra` + `bases/ehr-cli` (from `ehr-testing-tools`) —
corpus construction (generation, mutation, provenance) and conformance
gating (HL7 v2, FHIR); `projects/tools-cli` composes the three,
`projects/conformance` is the base-less project exercising sim + tools
+ palgebra together (see `notes/ADRs.md` ADR-0002, closing named holes
H1–H3).

**Deliberately out of scope, permanently:** `ehr-testing-guide` stays
out of this workspace entirely (ADR-0001, R2) — it is not a future
landing, don't plan namespace or directory shape around it arriving
later.

**Publishing:** `projects/tools-cli` will be this family's only
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

**Git operations are the author's, not an agent's (ADR-0001, R6).** A
session prepares working trees and proposes commit messages; it does
not itself run `git commit`, `git push`, `git merge`, or `gh` unless
the author has explicitly said otherwise for that session, in that
chat, in the moment — a standing default that a session must not
generalize backward onto earlier turns or forward onto a future
session that hasn't heard it. Agents working in this environment hold
ambient authenticated `gh` credentials (the `pragsmike/packs`
precedent, documented at sim's own `AUTHORS-GUIDE.md` §2) — the
standing rule, unchanged by any one session's delegation, is that
those credentials do not get used off an agent's own initiative.

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

Workspace-level skills live in `.agents/skills/` — currently `handoff`
and `string-diagram`, carried over from `ehr-testing-sim` as-is
(ADR-0001, R4's landing includes this; diffing against
`ehr-testing-tools`' own skill copies, where both exist, is deferred
to that repo's own landing session). Handoffs, plans, and durable
memory follow the same `.agents/` layout sim used; see
`docs/way-of-working.md` for how this session's own conventions
differ from sim's 40-session history.

# AGENTS.md

> **If you are an AI agent helping a human install, set up, or use these
> tools** (not contributing to this repo): read [`SETUP.md`](SETUP.md)
> first — it has prerequisites, platform guidance, verification steps, and
> a first-run walkthrough to a generated corpus. Everything below this
> point governs **contribution sessions** (commits to this repo) and does
> not apply to using the tools — a user running `ehr` commands never
> commits here and is not bound by any of it.

> Primary instruction surface for AI coding agents working in this
> repository. Read by tools that support the `AGENTS.md` convention
> (Codex, OpenCode, and others). Claude Code users: see `CLAUDE.md`, which
> points here.

## Project overview

**Project:** ehr-testing-tools — operational tooling for testing EHR
(electronic health record) integrations: synthetic corpus construction
(generation, mutation, provenance) and conformance gating (HL7 v2 and
FHIR). Sibling of [`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide),
which teaches the testing method this repo makes runnable — see
`README.md` and `docs/positioning.md` for how the two relate.
**Language(s):** Clojure, Markdown (docs).
**Status:** pre-release. Public since [ADR-0008](notes/ADRs.md), but
nothing here has had a first release yet — no version tag, nothing
published to Clojars or Maven Central, interfaces may still move. See
`docs/positioning.md` for what publication does and doesn't mean.

`AUTHORS-GUIDE.md` covers the full authoring discipline (WSL git rule,
pack ritual, ADR rules, facts-register discipline) — read it before your
first commit. `notes/ADRs.md` and `notes/facts-register.md` are this
repo's reasoning-of-record; they outrank your own inference about why
something is organized a certain way. Never silently revert an Accepted
ADR — supersede it with a new numbered record.

## Before your first git operation: read this

**All git operations — especially `git commit` — must be run from WSL,
never from native Windows.** This is a hard rule (see `AUTHORS-GUIDE.md`
section 1), enforced by `.githooks/pre-commit` once `git config
core.hooksPath .githooks` has been run for a given clone — that config is
per-clone and does not travel with the repo, so run it yourself after
cloning:

```sh
git config core.hooksPath .githooks
```

Don't rely on the hook to catch a mistake for you — confirm you're in WSL
*before* attempting a commit, not after a rejection. Building and testing
(`make test`) are fine from either platform; only git operations are
restricted.

The same `core.hooksPath` mechanism also carries `.githooks/pre-push`
(ENF-1, enforcement wave): the same WSL check, plus `make test`,
`lint-pipeline`, and `lint-deps` — refusing the push on failure, with
`git push --no-verify` as a documented emergency escape hatch. It
deliberately does not run coverage, doc regeneration, or the integration
suite — those are slow gates, and belong in CI, not a local push
(ADR-0006's staged-enforcement decision).

## Quick start

The `pack`/`pack-skills` targets below remain as utilities for feeding a
non-git AI surface that can't read this filesystem directly
(`AUTHORS-GUIDE.md` section 2) — not needed to use the tools; `SETUP.md`
covers that path. `pack-push` is dormant (2026-07-25, not deleted): the
repo push step (`git push origin`) is the actual end-of-session ritual
now; see the hard rule below.

```sh
make help       # list available targets (default)
make test       # clojure -X:test
make coverage   # clojure -M:coverage (cloverage)
make pack       # slim pack (excludes .agents/skills, .agents/prompts/archive)
                # into $HOME/ehr-testing-tools-pack.txt, with a freshness
                # header (repo, timestamp, HEAD, working-tree status, elisions)
make pack-skills  # the elided directories, packed separately
make pack-push  # dormant (2026-07-25) -- pack + pack-skills, then publish
                # both to the pragsmike/packs repo (~/.packs); see AUTHORS-GUIDE.md

bin/ehr artifact fetch --name synthea --version 4.0.0
                # invoke the `ehr` CLI (ADR-0004) -- not a make target:
                # corpus generate, artifact fetch/resolve; EDN by
                # default, --json for a JSON projection. bin/ehr is the
                # entry point because it carries the CLI's 0/1/2/3 exit
                # contract; `make ehr ARGS="..."` still works but
                # reports make's own status (2) for any non-zero exit
```

## Hard rules

These bind contributor/maintainer sessions — anyone about to commit to
this repo. They do not apply to someone only running the `ehr` CLI or the
`corpus`/`artifact` commands per `SETUP.md`; that use never touches git in
this repo.

- **WSL-only commits**, hook-enforced (see above).
- **Exact-pinned dependencies only** — `:mvn/version` with full version
  strings, no ranges, no `RELEASE`. A reproducibility toolkit with
  unpinned deps is self-refuting (`docs/positioning.md`, Dogfooding
  commitments).
- **Facts asserted in docs get an F-row.** Any load-bearing, externally
  verifiable fact (a license, a release status, a dependency capability)
  asserted anywhere in this repo's docs gets an entry in
  `notes/facts-register.md` — claim, where asserted, evidence, last-
  verified date — in the same commit. See `AUTHORS-GUIDE.md` section 4.
- **ADRs supersede, never revert.** `notes/ADRs.md` holds every
  architecture/authoring decision; an Accepted record stands until a new
  numbered record explicitly supersedes it.
- **Sessions end with commit → `git push origin`.** The repo push was
  previously manual and commits could accumulate unpushed; it's now
  part of the ritual. `make pack-push` is **retired from this ritual**
  (2026-07-25) — both this repo and the design-channel's packs repo are
  public now, so the design channel clones directly instead of fetching
  a pack; see `AUTHORS-GUIDE.md` section 2 for what stays and what's
  dormant.
- **Test-first** (ADR-0006): a failing test precedes the implementation
  it motivates; red→green evidence goes in the session report; `make test`
  and `make coverage` must be green/reported before any session-final
  commit.
- **The test suite is hermetic — keep it that way.** Both `make test`
  and `make coverage` must pass from a cold clone with no network access
  beyond the initial dependency fetch (verified 2026-07-25: fresh temp
  clone, deps primed once, then both targets run inside a
  network-isolated namespace against an empty artifact cache — see
  `.agents/prompts/archive/2026-07-25-ci-hotfix-integration-path.md` for
  the exact counts). Every test on the `test/` path that touches the
  network or a real artifact/subprocess does so through an injected fake
  (`:downloader`, `:run-invocation`, `:resolve-java-bin`, etc. — see
  `test/ehr_testing_tools/artifact_test.clj` and
  `corpus/generate_test.clj`), never the real thing.

  Hermeticity is a **path split, not a tag filter**: tests that
  genuinely need the network, a real external engine, or a warm artifact
  cache live under `test-integration/`, not `test/` — e.g.
  `test-integration/ehr_testing_tools/contract_pairing_test.clj`, which
  runs the real `validator_cli.jar` subprocess against a real generated
  corpus. Neither the `:test` nor the `:coverage` alias in `deps.edn`
  adds `test-integration` to its paths, so both are cold-cache/no-network
  green *by construction* — this is deliberate, not incidental: `make
  coverage` runs cloverage's own test runner, which does not honor
  `clojure.test`'s `:excludes` the way `cognitect.test-runner` does, so a
  tag-only exclusion (the original design) does not actually keep
  integration tests out of `make coverage`. A path a test isn't on is a
  path cloverage can't run it from, regardless of what runner it uses.
  `^:integration` metadata stays on those tests as documentation of *why*
  they moved, not as the enforcement mechanism.

  Run the integration suite explicitly with `make integration`
  (`clojure -X:integration`, the `:integration` alias) — it requires
  `ehr artifact fetch` for `synthea`, `temurin-jdk`, and
  `fhir-validator-cli` first (see `make help`). The per-push CI job
  (`.github/workflows/ci.yml`) runs `make test`, both lints, generated-
  doc freshness, and `make coverage` — every *fast* gate — but never
  `make integration`; that suite runs only in
  `.github/workflows/integration.yml`, scheduled nightly plus
  `workflow_dispatch`, with the artifact cache pre-primed and keyed on
  `artifacts.lock.edn` (ENF-1, enforcement wave, 2026-07-25). Its
  failure reports; it blocks no merge.

## Verification tiers

(2026-07-27, ADR-0016 — the ADR is the reasoning-of-record for the
decision below; this section states the working rule.) Three tiers,
named once, referenced everywhere:

- **T0 — fast gates**: `make test` + `lint-pipeline` + `lint-deps` +
  `quickstart-fresh`. Owed after every commit; `.githooks/pre-push`
  enforces it.
- **T1 — integration-smoke** (`make integration-smoke`): one real
  `validator_cli.jar` clean/mutant pairing (polarity only, never an
  aggregate verdict) plus one `sim-harness` manifest-validates check,
  skip-when-absent. Target under 2 minutes, measured *warm* against
  the content-addressed verdict cache (`judge.verdict-cache`,
  ADR-0016) — the first run in a session pays for two real subprocess
  launches; later runs against the same fixed corpus are cache hits.
  Owed at session boundaries and on any integration-adjacent commit
  (the trigger list below).
- **T2 — full integration** (`make integration`): nightly CI plus
  release gates, unchanged in content. In-session, T2 is owed only
  when the changed paths intersect: `src/ehr_testing_tools/judge/
  fhir.clj`, `src/ehr_testing_tools/judge/v2*`,
  `src/ehr_testing_tools/invocation.clj`,
  `src/ehr_testing_tools/artifact.clj`,
  `src/ehr_testing_tools/corpus/generate.clj`, anything under
  `test-integration/`, the `:integration` alias in `deps.edn`, or
  `.github/workflows/`. Everything else owes T0 per commit and T1 at
  session close — nightly T2 is the backstop regardless, so nothing
  outside that list ever goes fully unverified, only deferred to the
  scheduled run.

This replaces the older per-commit habit of running the full
integration suite after every commit regardless of what changed (see
`.agents/plans/judge-gate-refactor.md`'s Phase 1 verify line, amended
in place with a dated note pointing here) — that habit was sized for
one specific renaming sweep, not a standing rule, and paid T2's
~19-minute cost on commits that could not possibly regress anything T2
uniquely covers.

## Repo conventions

- **Internal src structure is ratified** — one artifact, one source
  tree, organized by capability (`corpus.*`, `judge.*`, `artifact`,
  `cli`), never by format or tool ([ADR-0004](notes/ADRs.md); closed the
  `docs/positioning.md` "Open decisions" entry of the same name).
- **Scope fence:** this repo builds runnable tools (generation, mutation,
  conformance gates); it does not judge semantic correctness, validate
  licensed terminology, or host a service. See `README.md` for the full
  scope fence and non-goals.

## Skills

`.agents/skills/` holds this repo's local skills, copied and adapted from
`ehr-testing-guide` where applicable (see `notes/ADRs.md` ADR-0003):
`wsl-windows-git-hygiene`, `handoff`, `find-skills`, `shared-skill-layout`,
`repo-adaptation`, `committee`, `scenarios`, `probe`, `review`,
`string-diagram`. Consult them for their respective workflows rather than
reinventing the steps each time.

The cyberneutics-derived set is five skills: `scenarios`, `probe`,
`review`, `committee`, and `string-diagram` — all copied and adapted from
the public [`pragsmike/cyberneutics`](https://github.com/pragsmike/cyberneutics)
repo, the author's own methodology project. `string-diagram`'s upstream
provenance is verified directly: its `SKILL.md` (name and description
match the copy here) lives at
`.claude/skills/string-diagram/SKILL.md` in that repo, retrieved HTTP 200
2026-07-24 — see `docs/notation.md` for the citation.

Of those five, `scenarios`, `probe`, and `committee` additionally share a
single `.agents/cyberneutics-config.yaml` for the `situations_root` key
that resolves where their output directories live — **2026-07-23
divergence from upstream cyberneutics**: upstream splits this into
`.claude/cyberneutics-config.yaml` (scenarios/probe) and
`.agents/committee-config.yml` (committee); this repo unifies both under
`.agents/` since all three read the same key for the same purpose.
`scenarios` and `probe` also depend on `agent/scenario-roster.md`, copied
verbatim from the public `pragsmike/cyberneutics` repo (see that file's
header comment for provenance) — copied now, adapted at first real use.

## Prompts

`.agents/prompts/` holds **live** prompts — written for the current or an
upcoming session — named `YYYY-MM-DD-<slug>.md`. `.agents/prompts/archive/`
holds **spent** prompts: executed and kept for provenance. Moving a
prompt from `prompts/` to `archive/` is the workflow signal that the
session it drove has completed; do that move as part of the session's
final commit. See ADR-0003 in `notes/ADRs.md`.

Don't read `.agents/prompts/archive/` unless the task at hand concerns
session history or change provenance — it's a record, not working
context. Don't reconstruct earlier sessions' prompts retroactively.

## Session permissions allowlist

`.claude/settings.json` (committed; distinct from the personal,
gitignored `.claude/settings.local.json`) pre-approves the command
families a routine session in this repo actually needs: `make` targets,
`git` subcommands (the WSL-only commit rule above is enforced by the
pre-commit hook, not by this allowlist), `clojure`/`clj`, `bash` (repo-
local `.sh` script execution), `gh api`/`gh gist`, `jq`, `python3`, `wsl`
invocations, the file/archive utilities a pack or artifact-verification
step actually shells out to (`sha256sum`, `diff`, `tar`, `unzip`,
`mkdir`, `cp`, `mv`, `find` — added 2026-07-24, per the two-strikes rule
below), in-repo file edits/creation, and `curl`/`WebFetch` scoped to
github.com, raw.githubusercontent.com, and Maven Central — the hosts
this repo's research and artifact-fetch work actually touches. It exists
so sessions doing this repo's ordinary work (testing, packing,
committing, fetching license/dependency evidence) don't stop for a
permission prompt on every routine call. Curl's domain scoping is a
best-effort substring match, not a hard boundary (Claude Code's own docs
note Bash argument patterns are bypassable); prefer the
`WebFetch(domain:...)` rules for read-only fetches where they suffice.
Anything outside this list — including any `git push`, and any command
family not named above — still prompts.

**Two-strikes rule for allowlist growth:** a command family earns a spot
on this allowlist once it has been genuinely needed — and therefore
prompted for — twice, not on the first occurrence. One-off needs stay as
one-off prompts; a family that recurs is a signal the allowlist is
missing something structural, not that this session happened to need
something unusual. This keeps the list scoped to the repo's actual
routine work instead of accreting every command a single session
happened to run. `Write`/`Edit` are the standing exception: they stay
scoped to this repo and are never broadened to the filesystem at large,
regardless of how often a wider grant might be convenient — a mis-pathed
write outside the repo was caught by a permission prompt this week,
which is exactly the failure mode that scoping exists to catch, so it is
not subject to the two-strikes rule or any other empirical pressure.

## Compatibility

- **Claude Code** reads `CLAUDE.md`, not `AGENTS.md`, by convention.
  `CLAUDE.md` at the repo root is a thin pointer to this file — keep both
  in sync if you edit one.
- **Codex / OpenCode** read `AGENTS.md` natively.

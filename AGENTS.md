# AGENTS.md

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
**Status:** pre-release. Private development; nothing here is stable or
released yet.

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

## Quick start

```sh
make help    # list available targets (default)
make test    # clojure -X:test
make pack    # concatenate every git-tracked file into
             # $HOME/ehr-testing-tools-pack.txt (outside the repo), with a
             # freshness header (repo, timestamp, HEAD, working-tree status)
```

## Hard rules

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
- **Regenerate the pack after the final commit of a session.** The pack's
  header `HEAD` and clean-tree line are only trustworthy if the pack was
  made last, not mid-session.

## Repo conventions

- **Internal src structure is an open decision, not yet ratified** — see
  `docs/positioning.md`, "Open decisions." `src/ehr_testing_tools/core.clj`
  is placeholder scaffolding only; do not add `corpus/`, `gate/`, or other
  capability namespaces without checking whether that decision has been
  made first.
- **Scope fence:** this repo builds runnable tools (generation, mutation,
  conformance gates); it does not judge semantic correctness, validate
  licensed terminology, or host a service. See `README.md` for the full
  scope fence and non-goals.

## Skills

`.agents/skills/` holds this repo's local skills, copied and adapted from
`ehr-testing-guide` where applicable (see `notes/ADRs.md` ADR-0003):
`wsl-windows-git-hygiene`, `handoff`, `find-skills`, `shared-skill-layout`,
`repo-adaptation`, `committee`. Consult them for their respective
workflows rather than reinventing the steps each time.

## Prompt archive

`.agents/prompts/` archives the verbatim text of executed Code prompts —
the chat-designed, agent-executed sessions that are the real provenance
of changes to this repo — named `YYYY-MM-DD-<slug>.md`. See ADR-0003 in
`notes/ADRs.md`. Archive the prompt you're executing as part of the
session's commits; don't reconstruct earlier sessions' prompts
retroactively.

## Compatibility

- **Claude Code** reads `CLAUDE.md`, not `AGENTS.md`, by convention.
  `CLAUDE.md` at the repo root is a thin pointer to this file — keep both
  in sync if you edit one.
- **Codex / OpenCode** read `AGENTS.md` natively.

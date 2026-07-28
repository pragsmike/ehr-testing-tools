---
name: shared-skill-layout
description: Diagnose and standardize where custom skills should live across Windows-native and WSL agent environments. Use when a skill is visible on one side but not the other, when deciding whether a skill belongs in .agents or .codex, when migrating a user skill into a shared library, or when verifying whether a restarted session is needed for skill discovery.
license: MIT
allowed-tools:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - onboarding
    - documentation
  tested-tools:
    - codex
---

# Shared Skill Layout

Use this skill to keep shared custom skills in the right place and to avoid mixing user-authored skills with Codex runtime/system space.

## Intended Scope

This skill documents an opinionated layout that is intended to work well for a mixed Windows-native and WSL setup where:

- custom reusable skills should be shared across environments
- Codex runtime/system material should remain local to each environment
- other agent tools such as Claude Code may also read from the shared custom-skill library

It is not claiming that every tool auto-discovers exactly the same directories by default. It defines the intended layout for this skill library and the expected placement of shared versus local skills.

## Core Rule

- Treat `~/.agents/skills` as the shared home for custom user-authored skills that should work across Windows and WSL.
- Treat `~/.codex/skills` as Codex/runtime space, especially for `.system` and environment-local material.
- Do not move or cross-wire `.system` just to make shared custom skills visible.

## What To Check First

- Which environment is being inspected:
  - Windows-native
  - WSL
- Which directory currently contains the skill:
  - user shared library in `.agents/skills`
  - Codex-local library in `.codex/skills`
- Whether the skill is a custom reusable skill or a system/runtime skill
- Whether the current agent/session predates the skill move or creation

## Placement Decision

Place a skill in `.agents/skills` when:

- it is user-authored
- it should be reusable across Windows-native and WSL sessions
- it is not part of Codex's built-in system skill surface
- it captures durable personal or team workflow knowledge

Leave a skill in `.codex/skills` when:

- it is part of `.system`
- it is clearly app/runtime-managed
- it depends on one environment's local runtime layout and is not meant to be shared

## Shared Layout Model

Typical shared custom-skill layout:

### Windows-native

- `C:\Users\<user>\.agents\skills\<skill-name>`

### WSL

- `~/.agents/skills` should resolve to the shared Windows-backed custom skill library
- example shared target:
  - `/mnt/c/Users/<user>/.agents/skills/<skill-name>`

Codex-local runtime/system layout remains separate:

- Windows-side `.codex`
- WSL-side `~/.codex`
- especially `.codex/skills/.system`

## Supported Access Patterns

This skill is intended to support shared custom skill discovery across these access patterns:

### Windows-native Codex

- should use the Windows-side shared custom skill library in `.agents/skills`
- should keep Codex runtime/system material in `.codex/skills`

### WSL Codex

- should use `~/.agents/skills` for shared custom skills
- should keep WSL-local Codex runtime/system material in `~/.codex/skills`

### Windows-native Claude Code

- should use the shared custom skill library when Claude Code is configured or arranged to read user-authored shared agent skills
- should not depend on Codex-specific `.codex/skills` paths

### WSL Claude Code

- should use `~/.agents/skills` for shared custom skills when that path is part of its configured or arranged user skill discovery
- should not depend on WSL-local `~/.codex/skills`, which is Codex-specific runtime space

## Canonical Shared Skill Rule

For skills meant to be shared across Windows and WSL, and across agent/tool contexts such as Codex and Claude Code:

- `.agents/skills` is the canonical shared library

For publication:

- this should be read as the library's intended convention, not as a claim that every tool discovers `.agents/skills` automatically without configuration

For Codex-specific runtime/system material:

- `.codex/skills` remains local and tool-specific

## Inspection Workflow

1. Determine the active environment.
2. Inspect both `.agents/skills` and `.codex/skills`.
3. Decide whether the skill is custom/shared or runtime/system.
4. If the skill should be shared, move or copy it into `.agents/skills`.
5. Verify the destination contains the full skill file set:
   - `SKILL.md`
   - `agents/openai.yaml` if present
   - any `references/`, `scripts/`, or `assets/`
6. Remove or retire the environment-local copy if it would shadow or confuse discovery.
7. Recommend starting a new agent/session so skill discovery refreshes.

## Migration Rules

- Preserve the whole skill folder when moving.
- Do not strip `agents/openai.yaml` or bundled resources.
- Prefer one canonical shared copy over divergent per-environment copies.
- If one side still needs a local-only variant, rename and document the distinction clearly.

## Discovery Notes

- A currently running agent/session may not see newly created or moved skills.
- When a skill was added, moved, or renamed recently, recommend a new session on each side before diagnosing discovery more deeply.
- Verify actual directory visibility before concluding a discovery bug exists.

## Warnings

- Do not unify `.codex/skills` across Windows and WSL just to share custom skills.
- Do not move `.system` into `.agents/skills`.
- Do not assume a skill visible in WSL `~/.codex/skills` is visible to Windows-native agents.
- Do not assume a symlinked `.agents/skills` helps if the skill was placed only in `.codex/skills`.
- Do not bake machine-specific paths, usernames, or private environment details into a skill that is intended to become part of a published shared skill library.

## References

Read [references/workflow.md](references/workflow.md) when you need a fuller checklist and example diagnostics.

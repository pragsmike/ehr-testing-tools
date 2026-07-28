# Shared Skill Layout Workflow

Use this reference when diagnosing cross-environment skill visibility or deciding where a custom skill should live.

## Checklist

1. Identify the environment:
   - Windows-native
   - WSL

2. Identify the relevant directories:
   - custom shared skill library: `.agents/skills`
   - Codex/runtime skill library: `.codex/skills`

2a. Identify the agent/tool context:
   - Windows-native Codex
   - WSL Codex
   - Windows-native Claude Code
   - WSL Claude Code

3. Ask what kind of skill it is:
   - shared custom skill
   - local runtime/system skill

4. Inspect actual resolution paths:
   - on Windows, inspect the real directories directly
   - on WSL, inspect `readlink -f ~/.agents/skills` and `readlink -f ~/.codex/skills`

5. Check whether the skill exists in:
   - only WSL-local `.codex/skills`
   - only Windows-side `.agents/skills`
   - both places

6. Decide the action:
   - move to shared `.agents/skills`
   - leave in `.codex/skills`
   - remove a stale shadow copy

7. After changes, recommend a new agent/session to refresh discovery.

## Heuristic

If a skill captures reusable user-authored knowledge and should be seen by both Windows-native and WSL agents, the default answer is:

- put it in `.agents/skills`

If it should also be usable across different agent/tool contexts such as Codex and Claude Code, the default answer is still:

- put it in `.agents/skills`

This is a layout convention for this library. If a specific tool requires explicit configuration to read from `.agents/skills`, document that configuration instead of moving the skill into tool-local runtime space.

If it is part of Codex's own runtime/system layer, the default answer is:

- leave it in `.codex/skills`

If the skill depends on `.codex/skills`, assume it is not yet in a tool-neutral shared state.

## Example Diagnosis Pattern

Problem:

- skill visible from WSL but not Windows-native

Likely cause:

- skill exists only in WSL `~/.codex/skills`

Likely fix:

- move the skill into the shared Windows-backed `.agents/skills` location
- confirm WSL sees it through the existing `.agents/skills` path
- start a new session on both sides

## Evidence To Gather

- exact path of the skill folder
- whether `SKILL.md` exists
- whether bundled resources moved intact
- whether `.agents/skills` is a symlink in WSL
- whether the current session predates the move
- which agent/tool context is expected to discover the skill
- whether the skill text contains machine-specific or private details that would block publishing

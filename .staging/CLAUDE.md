# Working with ehr-testing-tools in Claude

Read [`AGENTS.md`](AGENTS.md) first — it is the canonical instruction
surface for this repository, including the WSL-only git rule that applies
before any commit.

Claude-specific notes:

- Nothing in this repo currently requires Claude-specific handling beyond
  what's in `AGENTS.md`.
- Canonical skills live in `.agents/skills/` (see `AGENTS.md`'s "Skills"
  section for the current list) — that is where new repeatable workflows
  belong. If Claude Code auto-discovery under `.claude/skills/` is wanted
  later, add thin pointer wrappers there that reference, and never
  duplicate, the `.agents/skills/` copies.

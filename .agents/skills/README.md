# .agents/skills/ — index

The sim/tools union (ADR-0005), canonical here — edit at this location,
never in the mirror described below. One line each; see each skill's
own `SKILL.md` for the full trigger conditions and workflow.

- **[`build-session/`](build-session/SKILL.md)** — this repo's
  checkpointed commit/push ceremony: WSL-only git, staging hygiene,
  commit-message-via-file, gitleaks, post-push verification, and the
  COMMIT/AUTHOR-ACTION model.
- **[`probe/`](probe/SKILL.md)** — runs the fan→funnel pipeline N times
  over one situation to map the decision landscape's stable vs.
  variable structure.
- **[`scenarios/`](scenarios/SKILL.md)** — divergent scenario generation
  (the fan operation) over a situation, using the bundled roster.
- **[`session-prompt/`](session-prompt/SKILL.md)** — the design
  channel's own preflight for authoring a session prompt: re-read the
  repo at a stated HEAD, then the canonical prompt anatomy.
- **[`string-diagram/`](string-diagram/SKILL.md)** — converts resource
  equations (`A × B → C`) into Mermaid string diagrams.
- **[`wsl-windows-git-hygiene/`](wsl-windows-git-hygiene/SKILL.md)** —
  diagnoses/fixes Git worktree noise from mixed Windows/WSL checkouts
  (CRLF churn, stray editor artifacts).

Six skills, and that is the whole list. Eleven others —
`capture-session`, `committee`, `errata-sweep`, `extraction-stage`,
`find-skills`, `handoff`, `manual-review`, `repo-adaptation`,
`repo-review`, `review`, `shared-skill-layout` — were deleted by the
de-scaffold ruling of 2026-08-25. They are recoverable from git
history; nothing here maintains them. Adding a skill back is an author
ruling, not a session's.

## The `.claude/skills/` mirror

`.claude/skills/` is a real-file, tracked mirror of this directory
(ADR-0024, 2026-08-01) — the only way Claude Code discovers a skill is
`.claude/skills/<name>/SKILL.md`; it never reads `.agents/skills/`
directly. **Edit skills here, never in the mirror** —
`ehrt.docs-tooling.skill-mirror-currency-test` fails the build the
moment the two drift (content or executable-bit), both directions.

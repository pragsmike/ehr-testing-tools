# .agents/skills/ — index

The sim/tools union (ADR-0005), canonical here — edit at this location,
never in the mirror described below. One line each; see each skill's
own `SKILL.md` for the full trigger conditions and workflow.

- **[`committee/`](committee/SKILL.md)** — adversarial committee
  deliberation over a decision space; writes a dated deliberation
  record.
- **[`find-skills/`](find-skills/SKILL.md)** — helps discover and
  install agent skills when asked "is there a skill for X."
- **[`handoff/`](handoff/SKILL.md)** — generates a session handoff
  document for a successor agent, archiving the previous one.
- **[`probe/`](probe/SKILL.md)** — runs the fan→funnel pipeline N times
  over one situation to map the decision landscape's stable vs.
  variable structure.
- **[`repo-adaptation/`](repo-adaptation/SKILL.md)** — inspects a repo,
  determines bootstrap vs. migration, proposes/creates an AGENTS.md +
  `.agents/` layout. This session's own migration report is one of its
  outputs.
- **[`review/`](review/SKILL.md)** — independent review of a committee
  deliberation transcript against the five core rubrics.
- **[`scenarios/`](scenarios/SKILL.md)** — divergent scenario generation
  (the fan operation) over a situation, using the bundled roster.
- **[`shared-skill-layout/`](shared-skill-layout/SKILL.md)** —
  diagnoses/standardizes where a skill should live across Windows-native
  and WSL agent environments.
- **[`string-diagram/`](string-diagram/SKILL.md)** — converts resource
  equations (`A × B → C`) into Mermaid string diagrams.
- **[`wsl-windows-git-hygiene/`](wsl-windows-git-hygiene/SKILL.md)** —
  diagnoses/fixes Git worktree noise from mixed Windows/WSL checkouts
  (CRLF churn, stray editor artifacts).

## The `.claude/skills/` mirror

`.claude/skills/` is a real-file, tracked mirror of this directory
(ADR-0024, 2026-08-01) — the only way Claude Code discovers a skill is
`.claude/skills/<name>/SKILL.md`; it never reads `.agents/skills/`
directly. **Edit skills here, never in the mirror** —
`ehrt.docs-tooling.skill-mirror-currency-test` fails the build the
moment the two drift (content or executable-bit), both directions.

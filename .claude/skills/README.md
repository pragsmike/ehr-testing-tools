# .agents/skills/ — index

The sim/tools union (ADR-0005), canonical here — edit at this location,
never in the mirror described below. One line each; see each skill's
own `SKILL.md` for the full trigger conditions and workflow.

- **[`build-session/`](build-session/SKILL.md)** — this repo's
  checkpointed commit/push ceremony: WSL-only git, staging hygiene,
  commit-message-via-file, gitleaks, post-push verification, and the
  COMMIT/AUTHOR-ACTION model.
- **[`capture-session/`](capture-session/SKILL.md)** — turns a ratified
  decision into `notes/ADRs.md` law: provenance tags, dated amendments
  over rewrites, same-commit doc updates, and a named fence.
- **[`committee/`](committee/SKILL.md)** — adversarial committee
  deliberation over a decision space; writes a dated deliberation
  record.
- **[`errata-sweep/`](errata-sweep/SKILL.md)** — fixes stale or
  contradicted doc claims: the citation-vs-instruction distinction,
  one-to-one accounting, and a co-landed tripwire.
- **[`extraction-stage/`](extraction-stage/SKILL.md)** — the
  characterize→extract→verify→records discipline for splitting a
  Polylith brick with zero behavior change, proven byte-for-byte.
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
- **[`session-prompt/`](session-prompt/SKILL.md)** — the design
  channel's own preflight for authoring a session prompt: re-read the
  repo at a stated HEAD, then the canonical prompt anatomy.
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

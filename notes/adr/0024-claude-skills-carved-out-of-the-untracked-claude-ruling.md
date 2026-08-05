<!-- Attic file: notes/adr/0024-claude-skills-carved-out-of-the-untracked-claude-ruling.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0024 — `.claude/skills/` carved out of the untracked-`.claude/` ruling; mirror-with-gate lands the Claude Code discovery fix

**Status:** Accepted (author-ruled live, in-session), 2026-08-01.

### Context

The migration report (`.agents/plans/2026-08-01-migration-report.md`
item 9) verified this session that `.agents/skills/`'s 10 skill
directories are not a Claude Code discovery path — confirmed again by
this session's own attempt to reproduce the compatibility-matrix's
empirical findings (`.agents/skills/repo-adaptation/references/compatibility-matrix.md`).
The author ruled (charter §7 ties this to AR-1) toward the least
disruptive fix that actually works: dual registration, a real mirror
under `.claude/skills/` synced from `.agents/skills/` (canonical),
guarded by a drift-prevention test. That collides directly with the
carve-loss audit's own standing ruling (2026-07-28, `notes/ADRs.md`
ADR-0006, `.gitignore` line 20): "`.claude/` stays untracked... do not
`git add` anything under it" — every mechanism item 9 named requires
committing content there. Per this file's own fix-forward-with-disclosure
rule (ADR-0001 R10, restated in `AGENTS.md` Constraints), this was
stopped and asked rather than silently resolved; the author ruled live,
in-session, to carve out `.claude/skills/` specifically rather than
defer the whole item.

### Decision

**`.claude/skills/` is tracked; the rest of `.claude/` stays untracked.**
`.gitignore`'s `.claude/` line becomes `.claude/*` plus `!.claude/skills/`
(and `!.claude/skills/**`) so git recurses into the carved-out
subdirectory — a bare directory negation without the parent glob
doesn't work (git's own documented gitignore limitation: a negated
pattern cannot re-include files inside a directory excluded by an
earlier pattern unless the directory entry itself is also negated).
`.claude/settings.local.json` and any other `.claude/`-rooted file stay
exactly as untracked as the carve-loss audit ruled — this is a narrow
carve-out, not a reversal.

**Mechanism: mirror-with-gate (AR-1(iii)), not symlinks (AR-1(ii)).**
Two independent reasons converged, not one: (1) this session had no
`claude` CLI binary reachable from a non-nested process to run the
exact fresh-`claude -p` symlink-discovery probe AR-1 specifies — a
Claude Code session cannot launch a child Claude Code session
(`CLAUDECODE` guard, confirmed this session, not bypassed); (2) even
setting that aside, this very session's own working directory is the
native-Windows `/mnt/c` clone, not the WSL ext4 one — a symlink created
on the WSL filesystem is invisible from there (independent filesystems,
[[feedback-dual-clone-edit-hazard]] in the agent's own memory), and
Windows/drvfs symlink support is the named hazard AR-1 itself flagged
as possibly disqualifying. Real files sync identically across both
clones and CI regardless of platform; symlinks don't. `.claude/skills/<name>/`
now holds real-file copies of all 10 `.agents/skills/` directories,
`.agents/skills/` stays the AGENTS.md-native canonical source (edit
there, not in `.claude/`).

**Gate:** `components/docs-tooling/test/ehrt/docs_tooling/skill_mirror_currency_test.clj`
— walks `.agents/skills/` and `.claude/skills/`, asserts every relative
path under one exists byte-identical under the other, both directions
(present-and-identical, and no orphaned extra files on either side),
same exact-token-both-directions shape this repo's own `1c3d77c` commit
already hardened two other gates into. Runs under `clojure -M:poly
test`, part of CI's `conformance` project (`docs-tooling`'s own moved-tests
placement rule, ADR-0016) — not the push gate itself (`poly check` only,
per ADR-0003), so drift is caught by CI, not blocked at push time; named
here rather than silently narrower than AR-1's "per-push" phrasing.

**Not proven this session: the exact end-to-end fresh-process discovery
proof AR-1 asked for** (a previously-invisible skill appearing in a
freshly spawned Claude Code session's own loaded list). Blocked by the
same nested-session restriction above. Evidence gathered instead: (a)
the compatibility-matrix's own primary-source and cross-repo
corroboration (unchanged, still the strongest evidence `.claude/skills/<name>/SKILL.md`
is the real path); (b) a same-session Agent-tool subagent check (weaker,
same caveat the compat-matrix already recorded — a subagent may inherit
a catalog fixed at spawn time); (c) the mirror's own content is now
byte-identical and gate-verified, so the moment any process does scan
`.claude/skills/` fresh, discovery follows. **AUTHOR ACTION named:** run
`claude -p` (or start an ordinary interactive session) from a shell that
is not itself inside a running Claude Code process, in a clone that has
this ADR's commit checked out, and confirm `wsl-windows-git-hygiene` (or
any of the 10) appears in that session's own Skill listing — the
red→green proof this session could not self-administer. **AUTHOR ACTION
named (unchanged from AR-1):** fast-forward the `/mnt/c` clone to this
commit — real Windows-native Claude Code sessions run with that clone
as their working directory (confirmed: this very session's own cwd),
so the mirror does nothing for them until that clone has it on disk;
this session did not touch that clone, per the dual-clone-hazard memory
and AR-1's own fence.

### Fence

Nothing else about `.claude/`'s untracked status changes. This ADR does
not reopen `.claude/settings.json`'s own carve-loss-audit disposition,
does not touch `.claude/settings.local.json`, and does not claim the
discovery fix is proven end-to-end — see the AUTHOR ACTION items above.

### Deviation record

The migration report's own AR-1 named symlinks as the preferred reading
conditional on the probe; this ADR departs from that preference toward
mirror-with-gate, for the two reasons in the Decision section above,
not because the probe ran and failed — it never ran. Recorded as a
deviation because AR-1's own conditional logic ("prefer symlinks... if
[...] the empirical probe [succeeds]") was never actually satisfied one
way or the other; the choice was made on the surrounding evidence
instead, with the author's live sign-off.

---


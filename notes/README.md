# notes/ — index

What's in this directory, and what it's for. **Open work lives in
[`.agents/plans/roadmap.md`](../.agents/plans/roadmap.md), not here** —
`notes/` is where decisions, facts, and dated audits get recorded once
made; it does not track what's next (item 4, migration session 2,
2026-08-02).

Three zones, by how a file is meant to change over time:

## Current-truth registers (live, edit freely)

- **[`ADRs.md`](ADRs.md)** — every workspace architecture/authoring
  decision, numbered sequentially (`^## ADR-`). Outranks inference
  about why the workspace is organized a certain way — read it before
  restructuring anything (`AUTHORS-GUIDE.md` §3).
- **[`facts-register.md`](facts-register.md)** — externally verifiable,
  load-bearing facts (a license, a release status, a dependency's
  capability), one F-row per claim with evidence and a last-verified
  date (`AUTHORS-GUIDE.md` §4).

## Historical audits and characterizations (dated, not updated after the fact)

Point-in-time findings from a specific session, left as they were
written — fix-forward errata land as new dated entries elsewhere, not
edits to these:

- **[`carve-loss-audit.md`](carve-loss-audit.md)** (2026-07-28) — every
  non-generated path at each parent repo's final tree, diffed against
  the workspace, each row given an explicit disposition.
- **[`discipline-parity-audit.md`](discipline-parity-audit.md)**
  (2026-07-28) — three-way diff of every discipline/agent-infrastructure
  mechanism across sim, tools, and the workspace.
- **[`docs-audit.md`](docs-audit.md)** (2026-07-29) — a disposition row
  for every doc under root `docs/`, `components/sim/docs/`, and
  `components/tools/docs/`.
- **[`judge-engine-extraction-characterization.md`](judge-engine-extraction-characterization.md)**
  (2026-07-29) — the judge-v2-hapi/judge-fhir-official extraction's own
  pre-move behavioral baseline.
- **[`storefront-parity-audit.md`](storefront-parity-audit.md)**
  (2026-07-29) — click-depth comparison from the rendered root README
  to every named audience.
- **[`2026-07-30-refactoring-review.md`](2026-07-30-refactoring-review.md)**
  (2026-07-30) — **the origin of the current refactoring arc**: the
  whole-workspace review whose dated, liftable work items (P1–P3) drove
  the `tools` three-stage split (ADR-0016/0017/0018), the gate-hardening
  session, and — via the agent-UX charter it also fed — this migration
  itself.

## Frozen provenance (byte-identical, never edited for new paths or namespaces)

- **[`prompts/`](prompts/README.md)** — historical session-prompt
  archive through 2026-08-01, sealed 2026-08-02 (item 1: the forward
  pointer to `.agents/prompts/` already landed IS the whole migration;
  its own 29-file set is now pinned by a per-push test). Not frozen
  provenance from a parent repo like the two below, but frozen in the
  same read-only sense from this date forward.
- **`sim/`** and **`tools/`** — each parent's own ADRs, facts-register,
  and `.agents/` tree exactly as they stood at merge time. Cite them
  origin-qualified (`sim/ADR-0008`, `tools/F12`) from a live document;
  never edit them for a new path or namespace (`AGENTS.md` "Discipline
  surface, mapped"). Exempt from the per-directory README-presence gate
  (ruling 6, migration report open question 6) — forcing a README onto
  a directory whose whole charter is "byte-identical, never rewritten"
  would be in tension with that promise.

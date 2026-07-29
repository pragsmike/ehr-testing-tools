# Memory

Durable design lineage: knowledge that's expensive to re-derive and
doesn't belong in an ADR (which records a *decision*, not the research
behind it) or the facts register (which records one externally
verifiable claim per row, not connected reasoning). Modeled on sim's own
`architecture.md` (frozen provenance: `notes/sim/agents/memory/architecture.md`)
— a narrative log of what was verified about an upstream source, a
domain model, or a design constraint, and why it matters, written in
prose rather than table rows.

**What goes in:** durable, non-obvious context a future session
shouldn't have to re-discover — why a domain model is shaped the way it
is, what was learned mining an upstream source, a design constraint's
own reasoning once it's stable enough to stop being ADR-context and
start being background knowledge.

**When:** as knowledge is discovered or verified, not on a session
cadence — unlike `session-records/`, this isn't one file per session.

**Format:** one file per topic (`architecture.md`, `patterns.md`, or a
more specific name once there's enough content to split), narrative
prose, dated where a claim's currency matters. If a specific fact needs
external verification and a re-check date, it belongs in
`notes/facts-register.md` instead, cited from here rather than
duplicated.

Empty at instantiation (2026-07-28, discipline-parity session, R25) —
this workspace hasn't yet accumulated its own durable design lineage
distinct from what `notes/ADRs.md` already records. Sim's and tools'
own pre-merge memory files stay frozen at `notes/sim/agents/memory/` and
`notes/tools/agents/memory/` — read them for their own domain content,
don't copy them here without a workspace-level reason to.

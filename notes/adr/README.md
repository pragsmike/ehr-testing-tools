# notes/adr/ (per-ADR attic)

Every entry `notes/ADRs.md` used to hold inline now lives here, one
file per record, moved byte-for-byte by scaffolding compaction B
(2026-08-05, `notes/ADRs.md` ADR-0046) — proof is that session's own
extraction diff. `notes/ADRs.md` stays the citation index and citation
target (`notes/ADRs.md ADR-NNNN` still resolves the same way it always
did); cite through it, not this directory directly.

**Naming convention:** every file here except this README is
`NNNN-<slug>.md`, one per `notes/ADRs.md` index line, in the order
that file's own index lists them (unchanged from this file's pre-split
entry order — not renumbered). Not restated as a per-file list (60 of
them, as of ADR-0062 — a count that goes stale the moment the next ADR
lands, the exact pattern `.agents/state.md`'s own regeneration contract
exists to catch; see `notes/ADRs.md`'s own index instead of duplicating
it here).

New execution-record appends to an existing ADR (a dated amendment, a
deviation-record entry) land directly in that ADR's own file here from
2026-08-05 forward — the index line in `notes/ADRs.md` updates only
when an arc closes.

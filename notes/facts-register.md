# Facts Register — ehr-testing (workspace)

<!-- Tracks load-bearing, externally verifiable assertions made anywhere in
     this workspace (docs, deps.edn, memory files) about tools, licenses,
     and ecosystem capabilities -- evidence and a last-verified date, so a
     claim can be checked instead of trusted. See AUTHORS-GUIDE.md §4 for
     the assert -> register -> date discipline this file exists to
     support. Instantiated 2026-07-28 (discipline-parity session, R25);
     see notes/discipline-parity-audit.md row M5/M5a for why this file
     didn't exist before this session and why the Index sub-table below
     is tools' own addition, adopted on top of sim's base F-row shape. -->

Fresh sequence, starting at F1 — **not** a continuation of
`notes/sim/facts-register.md`'s or `notes/tools/facts-register.md`'s own
numbering (those are frozen provenance, moved intact, F-rows not
migrated forward automatically per each file's own header). A
workspace-level claim that repeats one of theirs needs its own fresh row
here, re-verified — unless explicitly carried forward with an origin
citation, as F1 below does.

## Index

Hand-maintained, not enforced by a lint or CI check (same choice as
`notes/ADRs.md`'s own lack of a table of contents check). Update this
table in the same commit as any new F-row — one line per row: an
extractive digest (a clause already present in the full row below, not a
new characterization), the last-verified date, and the status column
verbatim.

| # | Claim digest | Last verified | Status |
|---|---|---|---|
| F1 | Sim's manifest generator identity is `"ehrt.sim"` (not `"ehr-testing-sim"`) since ADR-0001's rename; a stale test asserting the old identity went unexecuted until ADR-0005 first ran the path for real | 2026-07-28 | verified (carried forward, origin `notes/ADRs.md` ADR-0005) |

## Register

| # | Claim | Where asserted | Evidence | Last verified | Status |
|---|---|---|---|---|---|
| F1 | `components/sim/src/ehrt/sim/manifest.clj`'s generator identity has reported `"ehrt.sim"` since ADR-0001's own mechanical rename (2026-07-28) — not `"ehr-testing-sim"`, sim's pre-rename self-identity. This is a worked example for why this register exists: `projects/conformance/test/ehrt/tools/sim_manifest_contract_test.clj` asserted the *old* string, but that test path had never actually executed end to end before ADR-0005's in-process `ehr sim` mount — always skipped, local and CI both, for lack of a sibling checkout at the moments it previously ran. The mount is what first let it run for real, and it caught its own staleness on the first real run. Fixed (test corrected to `"ehrt.sim"`, not left red — AUTHORS-GUIDE.md's two-failure-modes discipline: the check misencoded its own invariant, so the check was the thing to fix, not reality). | `components/sim/src/ehrt/sim/manifest.clj:77`, `projects/conformance/test/ehrt/tools/sim_manifest_contract_test.clj` | `notes/ADRs.md` ADR-0005's own deviation record ("A genuine, previously-latent finding, surfaced by the mount, not caused by it") | 2026-07-28 | verified (carried forward from ADR-0005 at register instantiation, discipline-parity session) |

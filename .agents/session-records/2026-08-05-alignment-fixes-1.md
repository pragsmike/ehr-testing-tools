# 2026-08-05 — Alignment fixes 1: the past stops leaking — staleness swept, tripwire hardened, conventions named

## Scope

Session prompt naming AR-F1-0 through AR-F1-6, the first ruled cluster
out of the alignment-audit's 47-row findings register
(`.agents/plans/2026-08-05-alignment-audit-findings.md`, `notes/adr/0049-alignment-audit.md`).
Executed: the staleness sweep (register rows A-6, E-3, E-5, E-9, 8
files, 25 stale-namespace hits fixed forward), the staleness tripwire's
scope widening + `ehrt.sim-cli.` forbidden-list addition (S7, E-7),
`workspace.edn`'s 40-line `:necessary` narrative relocation to ADR-0050
plus the `oracle` reachability fix (S3, A-1), and four
docstring/annotation-only convention notes (B-1, C-5, D-2, F-5). Full
account, rulings verbatim: `notes/adr/0050-alignment-fixes-1.md`.

AR-F1-0 (the second stable tag) was licensed but **not executed** —
tag creation is AUTHOR ACTION in every ceremony mode per `AGENTS.md`'s
own standing rule, and this session's own prompt did not override it.
Exact commands recorded in ADR-0050 for the author to run directly.

## Red→green evidence highlights

- **The tripwire's own gap, proven then closed.** Before landing the
  sweep, the widened `stale_path_test.clj` (include-list scope +
  `ehrt.sim-cli.` addition) was run alone against the still-unswept
  tree: it failed exactly as predicted —
  `.agents/plans/roadmap.md carries stale-path residue:
  [:retired-ehrt-tools-namespace]` — proving the gap S7 named was real.
  After the sweep landed in the same commit: green, 0 failures.
- `ehrt.docs-tooling.stale-path-test`: 8 deftests/153 assertions
  (`989d6cf`) → 9 deftests/166 assertions (this session's tip),
  measured directly against both file versions, not asserted.
- `clojure -M:poly check :dev`: `Warning 207: ... oracle` (before the
  A-1 fix) → `OK` (after).
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  every checkpoint this session, 0 failures/0 errors, no `FAIL`/error
  markers anywhere in any captured log.
- `bin/regression-oracle 989d6cf a9d3bb6`: all eleven vendored-root
  batches byte-identical — expected, since no `src/` logic changed at
  any point this session (docstrings, comments, docs, config comments
  only).

## Judgment calls and their ratification status

- **AR-F1-2's sweep judgment (current-tense vs. historical) resolved
  every one of the 25 hits as current-tense** — none were judged
  explicitly historical, so none kept the old namespace form. This
  matches the precedent this same test's own M2–M4 addenda already set
  for these exact files. Not separately re-ratified this session;
  flagged here as the judgment call AR-F1-2 delegated, made
  consistently with standing precedent rather than case-by-case
  invention. Full per-file table in ADR-0050.
- **A-6's dated note, merged rather than stacked.** The ruling offered
  either a second adjacent dated note or a merge; a second note reading
  badly right next to AR-AU-1's existing one was judged the worse
  option, so the two corrections (source-sink at ADR-0049, framing at
  this ADR) were merged into the row's one existing note. Recorded in
  ADR-0050, not separately ratified.
- **A working-tree ordering mistake, corrected before it reached a
  commit.** The sweep edits were drafted before the tripwire's own
  red-evidence capture, which would have silently destroyed the
  red→green proof the ruling asked for. Recovered via `git stash`
  (moving the sweep out, capturing red on the bare tripwire widening,
  restoring the sweep, confirming green) — no commit was ever made
  in the wrong order. Disclosed in ADR-0050's own Step 1 account per
  fix-forward-with-disclosure, not silently smoothed over.

## Findings and HEAD landed

No findings beyond what ADR-0050 itself already discloses (the sweep's
own hit count came in higher than the register's estimate — 25 hits
across 8 files vs. the register's "12 file-hits" — noted as a
re-derivation delta, not a new defect).

Commits, in order: `430bb5c` (Step 1, sweep + tripwire), `6e27e78`
(Step 2, workspace.edn), `a9d3bb6` (Step 3, conventions), and this
session's own closing records commit (Step 4).

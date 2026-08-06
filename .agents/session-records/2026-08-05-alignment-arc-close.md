# 2026-08-05 — Alignment arc close: the register empties, the state regenerates, the law is appended

## Scope

Session prompt naming AR-AC-0 through AR-AC-6. Prior: alignment fixes 5
landed and was design-channel-verified (`2b3bb2b`,
`notes/adr/0054-alignment-fixes-5.md`); every fix cluster of the
alignment arc (ADR-0048 through ADR-0054) landed and verified. This
session closed the arc: three standing rulings appended to
`.agents/rulings.md`; the first `libs :outdated` report run under the
new A-3 cadence; `.agents/state.md` regenerated in place with every
`[V]` claim re-probed against the live tree; the `:onboarding` reading-
set budget re-derived; the arc's seven Done pointers rotated to the
attic under a dated header; and this closing ADR
(`notes/adr/0055-alignment-arc-close.md`) recording the register's
final 54-row disposition tally. Docs-only throughout. Full account,
rulings verbatim: `notes/adr/0055-alignment-arc-close.md`.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green and untouched,
not a red→green cycle — confirmed at every checkpoint:

- Baseline (Step 0, tip `2b3bb2b`): `clojure -M:poly test :all
  skip:integration` — 216 `Test results:` lines, 0 `FAIL`/`ERROR`/
  `Exception` anywhere. `clojure -M:poly check`: OK. `gitleaks
  detect -v`: 664 commits scanned, no leaks. `bin/regression-oracle
  2b3bb2b 2b3bb2b`: all eleven roots IDENTICAL, soundness "yes outside
  ns form."
- After Step 2's own three-file edit (`state.md` regeneration, budget
  re-derivation, Done rotation): full suite unchanged in shape (216
  `Test results:` lines, 0 failures/0 errors) — the four gates most
  directly implicated by the edit (`reading-set-budget-test`,
  `done-pointer-adr-test`, `index-completeness-test`, `stale-path-
  test`) run directly, not merely inferred: 23 tests, 228 assertions, 0
  failures, 0 errors. Oracle re-run: all eleven roots IDENTICAL again —
  no `src/` touched.
- `clojure -M:poly check`: OK at every checkpoint.
- `gitleaks git --staged -v`: clean at every commit this session.

## Judgment calls and their ratification status

- **AR-AC-5's own Done-rotation scope, read literally over its own
  summary phrasing.** Only ADR-0048 through ADR-0054 relocated to the
  attic, per the ruling's own explicit range — three leftover
  scaffolding-compaction pointers (ADR-0045/0046/0047, a separate,
  already-closed arc) stayed in the live roadmap's own Done section,
  disclosed rather than either silently swept or silently left
  unexplained. Not yet ratified by the author; flagged in the prompt
  archive's own deviation record for design-channel review.
- **ADR-0055's own Done pointer sequenced into Step 3, not Step 2** —
  citing an ADR number before its own index line exists would have
  tripped `done-pointer-adr-test`'s dangling-reference gate. The
  session's own Steps section already stated this order explicitly;
  followed that over AR-AC-5's own more compressed prose. Mechanical,
  not a judgment call requiring ratification.
- **The register's own summary-line arithmetic corrected by fresh
  count** (47/51 stated vs. 54 actual, 12/10 vs. 13/9 ruling-needed/
  fix-session-candidate) — disclosed in ADR-0055, not investigated
  further, matching this arc's own established precedent for small
  count drifts (ADR-0051 Step 0's "13 vs 14 files" note is the closest
  prior instance).
- **D-4 named as an honest, unfixed gap** rather than silently dropped
  from the tally — verified by grepping all five fix-session ADRs for
  its own subject matter (generator-source/corpus.display/table-helper/
  D-4) and finding zero hits. A finding, not an act, per this session's
  own fence.
- **The NIST licensing inquiry's own citation corrected** — ADR-0053's
  stated path does not exist; the real evidence document was located
  by direct search and its "inquiry draft maintained privately by the
  author" framing confirmed by reading its own "Artifacts produced"
  table.

## Findings and HEAD landed

No findings beyond the disclosures above and ADR-0055's own tally.
Three commits, in order: `992e0a5` (Step 1, the rulings appends),
`2afba86` (Step 2, state regeneration + budget re-derivation + Done
rotation), and this session's own closing records commit (Step 3),
landing on the ADR-0055 index line, the roadmap's own new Done pointer,
and this record's own pair. AR-AC-0's tag
(`stable-20260805-alignment-fixes-5`) stays licensed, not executed —
AUTHOR ACTION, commands recorded in `notes/adr/
0055-alignment-arc-close.md`'s own Step 0.

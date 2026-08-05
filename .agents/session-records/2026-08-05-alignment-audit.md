# 2026-08-05 — Alignment audit: the tree examined, findings registered, nothing moved

## Scope

Session prompt naming AR-AU-0 through AR-AU-5, the alignment arc's own
brief §6 Step 1. Prior: the alignment-riders session landed the brief
and adopted stable-tagging (`79b7a55`, `notes/adr/0048-alignment-
riders.md`). Findings-only beyond Step 0's two pre-ruled acts — no
`src/`, no `test/`, no `deps.edn`/`workspace.edn`, no gate changes.
Full account, rulings verbatim, row counts: `notes/ADRs.md` ADR-0049.

Step 0 (preflight) confirmed the working directory is the ext4 clone
(`~/src/ehr-testing-tools`, not `/mnt/c`), tip `79b7a55`. Baseline full
suite green (6 `Test results:` blocks, 0 failures/0 errors). Verified
the live corpus-io namespace (`ehrt.corpus-io.source-sink`) before
editing anything.

**AR-AU-0, executed.** Annotated tag `stable-20260805-alignment-riders`
created at `79b7a55`, pushed via `git push origin <tag>` (primary path
succeeded — no `gh api` fallback needed), verified live on origin via
`git ls-remote --tags origin` (peeled ref resolves to `79b7a55`
exactly).

**AR-AU-1, executed.** `.agents/plans/roadmap.md`'s `:mllp` Deferred
row's `ehrt.tools.corpus.source-sink` corrected to
`ehrt.corpus-io.source-sink` with the prescribed dated note — the
session's only roadmap edit. A second stale reference in the same row
(`ehrt.tools.corpus.framing`) was found and deliberately left
untouched per the ruling's own single-edit fence, recorded as finding
A-6 instead.

Committed `f1ceea7`, pushed, verified.

Step 1–2 (areas A–F) ran the brief's §4 probes in-workspace. Area A
(dependency graph) and Area D (evolution-seam readiness) were probed
directly; Areas B, C, E, F were delegated to independent sub-agents,
each briefed with the exact row format and probe list, and spot-checked
directly against the live tree afterward (façade-caller counts,
resource-nesting evidence, tripwire forbidden-list contents,
`repo-identity`'s hardcoded value) — every spot-check matched the
sub-agent's own claim exactly, no correction needed.

`.agents/plans/2026-08-05-alignment-audit-findings.md` landed with its
`.agents/plans/README.md` index entry: 7 seeded rows (S1–S7, all
re-verified fresh; S2/S4/S6 closed, re-confirmed already executed;
S7 recommendation-only per its own fence) + 40 new rows (A:6, B:13,
C:7, D:4, E:10, F:7) = 47 total. Dispositions: close-as-fine 26,
ruling-needed 12, fix-session-candidate 10, incomplete 3 (all
methodology-caveat rows, not coverage gaps).

Notable findings recorded, not acted on: `poly check :dev`'s
previously-silent `oracle`-unreachable-from-`development` warning
(A-1); the NIST jar `file://`-mirror end-state has not landed, still
hit-nexus-live (A-4); the brief's own §4.4 framing of what a future
`sim-emit-cda` sibling would copy was corrected by evidence — the
named HL7 idioms (site-profile, v2-replay) are wire-stream-format
concerns a document-shaped CDA sibling would not need; `sim-emit-fhir`'s
own snapshot-at pattern is the real analog (D-2); of ADR-0017/0018's 7
named-futures, 4 turned out to already be closed by mechanisms those
ADRs didn't anticipate (D-4); 3 of 4 Apache-2.0-sourced vendored roots
lack the license text itself (F-4); `repo-identity` is a hardcoded
"pre-release" literal even though four `stable-*` tags now exist
(F-5).

Full suite green after Step 2 (511 assertions, 0 failures/0 errors,
same shape as Step 0's baseline). Committed `2246a41`, pushed,
verified.

Step 3 (this record) authored `notes/adr/0049-alignment-audit.md`
directly, appended its index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own file count (46→47, in the same commit that
makes it stale — the same fix-forward precedent ADR-0048's own session
set for this exact file), added the Done pointer (`- 2026-08-05 —
alignment-audit — ADR-0049`), ran the oracle bracket (below), archived
this prompt, and recorded this session.

## Deviations, disclosed

- **None beyond what ADR-0049 itself already discloses.** The S7
  namespace fix's own single-edit fence left a second stale reference
  in the same roadmap row deliberately untouched (recorded as finding
  A-6, not a deviation — the fence was followed exactly, not breached).

## Findings

The full register (47 rows) is the deliverable:
`.agents/plans/2026-08-05-alignment-audit-findings.md`. Not
summarized again here beyond ADR-0049's own "notable findings"
paragraph — the register itself is the citable artifact.

## Verification

- `bin/regression-oracle 79b7a55 2246a41` (baseline: this session's
  own pre-session tip; target: the tip immediately before this
  record's own closing commit — no `src` touched at any point this
  session): `IDENTICAL: every root's digest matches between 79b7a55
  and 2246a41` — all ELEVEN vendored-root batches (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as expected for a docs-only session. No `--declared-digest-change`
  licensed or needed.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (511 assertions, 0 failures/0 errors) and again after
  Step 2, same shape — every edit this arc landed is docs.
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every checkpoint: each showed
  exactly one delta against its own message file — the known,
  harmless trailing-newline artifact prior sessions already name.

Commits, in order: `f1ceea7` (Step 0, tag + S7 fix), `2246a41` (Step
1–2, the register lands), and this session's own closing records
commit (Step 3).

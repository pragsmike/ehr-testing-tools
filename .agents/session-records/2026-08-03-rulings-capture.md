# 2026-08-03 — Rulings capture: parity-plan Q1–Q4, wellness-semantics overturn, plan-status hygiene

## Scope

The design channel (2026-08-03) ruled the four open questions in
`.agents/plans/2026-08-02-gmf-parity-plan.md` §6 (AR-1 through AR-4),
and a live probe against Synthea source at the interpreter doc's own
pin overturned a Wave-B-era survey claim about GMF `wellness: true`
encoding — the third overturned survey row (AR-5). Separately, the
post-Wave-D cleanup session (`64e250f..0cff0d4`, `notes/ADRs.md`
ADR-0030) had returned IDENTICAL byte-digests on both spans (J1,
clearing the parity plan's own approval gate) and confirmed the
closure engine round trip broken in two distinct ways (J3, pinned by
three tests under `components/sim-emit-hl7/test/`). This session
CAPTURED all of that into the repo: `notes/ADRs.md` ADR-0031 (AR-1
through AR-7, verbatim), the parity plan's own status flip
(PROPOSED → APPROVED) and §3/§4/§6 revisions, dated fix-forward status
headers on two stale plan files (`2026-08-02-sim-split-plan.md`,
`2026-08-02-gmf-coverage-plan.md`) plus the plans index, dated
corrections to the wellness documentation (`gmf-interpreter.md` §4 and
its prioritization table, `gmf.clj`'s normalization-clause comment —
docstring/comment text only, zero behavior change, proven by a real
`bin/regression-oracle` run), and roadmap rows sequencing the two
defect-fix sessions AR-6 names ahead of the census. Docs-only: no code
behavior changes anywhere in this session.

## Red→green evidence highlights

- Full non-integration suite (`clojure -M:poly test :all
  skip:integration`) run three times across the session (after Step 3,
  after Step 4, after Step 5): 198 namespaces, 2668 tests, 8451
  assertions, 0 failures/0 errors, IDENTICAL totals across all three
  runs — this session touched no test and no production behavior, so
  an unchanged count is the expected proof, not merely a nice-to-have.
- `poly check` clean before every push (5/5).
- **Step 4's docstring/comment-only edit to `gmf.clj` was verified with
  the literal byte-digest oracle**, not merely the count comparison:
  `bin/regression-oracle 2747757 6846893` — IDENTICAL SHA-256 digests
  on all six pre-existing vendored roots (appendicitis, death-fixture,
  ear-infections, sepsis, sinusitis, sore-throat), same digests as the
  post-Wave-D cleanup session's own baseline table. This is the
  strongest verification this repo's own doctrine (ADR-0030 J2)
  recognizes, chosen deliberately given the fence's own "a digest
  change here is a STOP-AND-ESCALATE, not a fix" stakes.
- Self-caught reading-set-budget trip (the established pattern every
  prior session touching `roadmap.md` has hit): Step 5's own new Next
  rows, Deferred cross-references, and backload item brought
  `:onboarding`'s real measured total from 1210 to 1243 lines;
  `.agents/reading-sets.edn`'s own budget bumped in the same commit,
  red→green live-proven (`ehrt.docs-tooling.reading-set-budget-test`).
- `ehrt.docs-tooling.index-completeness-test` green throughout —
  no new file this session lands outside an already-indexed
  directory except this record and its paired prompt archive, both
  indexed in the same commit as required.

## Judgment calls and their ratification status

- **Overturned prioritization-table row: struck-through-in-spirit,
  cell-embedded dated note, not a separate paragraph.** The session
  prompt said "dated OVERTURNED note per AR-5" without specifying
  placement; this document's own established convention (the
  `CallSubmodule` row's own multi-paragraph dated note, §9) embeds
  dated notes inside the relevant table cell's own column, so the
  wellness row's overturn note follows that precedent — in the
  "content it would unlock" column, with the retired characterization
  left in place under strikethrough markup rather than deleted, so a
  reader sees exactly what was believed before the overturn without
  reading two separate locations. Not ratified verbatim; a
  documentation-style choice consistent with this file's own
  established pattern.
- **§4's dated note treats the mapping-table row as still accurate for
  case (1) (the class-string `"wellness"`), silently narrowed rather
  than struck.** The session prompt's own instruction was "a dated
  note on the mapping-table row distinguishing the two upstream
  constructs" — read as clarifying scope, not retracting the row
  (no vendored root uses the class string at all, so the row's own
  compile-time behavior for case (1) is unaffected; only case (2),
  handled entirely by the loader's separate boolean-idiom clause, was
  ever wrong). Judged as the faithful reading of "distinguishing," not
  "correcting" — flagged here in case the author reads the table
  itself as needing a stronger correction.
- **Parity plan §4 table: two new rows placed ABOVE Census, `E`/`F`/`G`/`H`/`I`
  rows marked "(provisional order)" rather than renumbered.** AR-6's own
  text says "sequencing left to the census ranking" for the wave order
  after Census; renumbering the letters themselves (e.g. Census becomes
  wave "A" of this plan) was judged out of scope — the census hasn't
  run yet to produce a real ranking, so relabeling now would invent
  false precision. The letters stay as originally assigned, annotated
  provisional.
- **Roadmap `Next` rows for the two defect-fix sessions and the census
  were written new** (not lifted verbatim from any prior document) —
  each cites its own Deferred-row provenance and ADR-0031 pointer per
  this project's own citation discipline, but the prose itself,
  including the "sequenced first/second/third" framing, is this
  session's own synthesis of AR-6's ordering rule, not an author
  quotation. Not a design decision (the ORDER is AR-6's own, verbatim,
  quoted in ADR-0031 itself) — only the roadmap-row phrasing is new.
- **`:onboarding` reading-set budget bumped to the exact measured
  total (1243) rather than a rounder or padded number**, following
  every prior session's own established convention on this same file
  (real measured actual, not a target) — no live design decision, a
  mechanical application of precedent.

## Findings and HEAD landed

- No unplanned findings this session — every fact this session's
  rulings act on (the J1 oracle clearance, the J3 closure-engine
  gap, the wellness-semantics probe result) was already established
  before this session began (post-Wave-D cleanup, and the design
  channel's own 2026-08-03 Synthea-source probe that produced AR-5).
  This session's own work was capture, not discovery.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself, each
  verified against its own message file (the message-file/`git log`
  diff's only delta is `git log --format=%B`'s own trailing-newline
  artifact, at every one of the five checkpoints so far).
- Commits, in order: `ebff342` (Step 1, ADR-0031), `dd0acb3` (Step 2,
  parity plan APPROVED), `2747757` (Step 3, stale plan-status
  hygiene), `6846893` (Step 4, wellness documentation corrections,
  byte-digest verified), `80b6b6c` (Step 5, roadmap rows), and this
  commit (Step 6, records — session record + prompt archive, both
  indexed).
- **Fence, explicit (per this session's own prompt and the
  capture-session skill's own "name the fence" step): this session did
  NOT execute the two defect-fix sessions AR-6 names** (the
  Procedure-duration fix, the engine closure-context fix), did NOT
  build the census tool itself, and did NOT touch
  `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj`'s
  wellness-normalization CODE FORM (only its comment, disclosed and
  byte-digest-verified unperturbed) — all of that is Wave G/future-session
  scope, named and sequenced but deliberately left undone here, exactly
  as the session's own Fences section required.

# 2026-08-05 — Alignment riders: small debts paid, the audit brief lands, stable tags go live

## Scope

Session prompt naming AR-R-1 through AR-R-5, opening the alignment
arc. Prior: the scaffolding compaction arc closed and verified
(`89e327f`, `notes/adr/0047-scaffolding-compaction-c.md`). Docs-only —
no `src/` touched at any point. Full account, rulings, the Step 0
verification table, and the ruling-arithmetic disclosure: `notes/
ADRs.md` ADR-0048.

Step 0 (preflight) confirmed the working directory is the ext4 clone
(`~/src/ehr-testing-tools`, not `/mnt/c`), tip `89e327f`, the brief
present at `.agents/plans/2026-08-05-alignment-audit-brief.md` (252
lines, non-empty). AR-R-1's three probeable claims (S1 resource
nesting, S2 tag inventory, S4 roadmap-invisibility) were re-probed
fresh against the live tree — all three HELD, no correction owed.
Baseline full suite: green except one expected failure
(`index-completeness-test`, 42/43 — the not-yet-landed brief missing
its README entry), exactly the red state Step 1 closes.

Step 1 (`eb97f1f`, AR-R-1) landed the brief with its `.agents/plans/
README.md` index entry. Index-completeness gate confirmed green after
(43/43).

Step 2 (`9f20ba3`, AR-R-3/AR-R-4) read `notes/adr/0014-corpus-player.md`'s
own deferral text directly (not this prompt's summary of it) and wrote
three Next rows (bed board / census sink, accumulator wiring, sim
event-log input adapter) plus one Deferred row (the `:mllp` transport
sink, carrying ADR-0014's own three-namespace bail-out reasoning
verbatim-by-citation and the revisit trigger named in this session's
own ruling). The `myocardial_infarction.json` Deferred row — carrying
an in-place "RESOLVED... see Done, below" note rather than having
actually been relocated, a drift ADR-0047 disclosed but left unfixed —
relocated verbatim to `.agents/plans/roadmap-done-2026-08.md`, notes
intact, under a new dated `## Done (this session, 2026-08-05,
alignment riders — ADR-0048)` heading matching the attic's own
AR-A-5-precedent format.

Step 3 (`8cb712f`, AR-R-5) appended two entries to `.agents/rulings.md`
under a new "From ADR-0048" section: the transcript-witnessed ≠
repo-recorded doctrine (citing ADR-0047 Step 0 as the evidencing
event, itself re-verified against `feedback-verification-discipline`'s
own memory record before repeating the "twice" claim), and AR-R-2's
stable-tagging discipline. Both land mid-arc, ahead of the register's
own stated arc-close-only contract — the deviation is disclosed inline
in the register itself and again in ADR-0048, not silently normalized.

Step 4 (this record) authored `notes/adr/0048-alignment-riders.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own stale file count (45→46, the same
staleness class ADR-0047 already named — found while landing, fixed
forward), added the Done pointer (`- 2026-08-05 — alignment-riders —
ADR-0048`), ran the oracle bracket (below), archived this prompt, and
recorded this session.

## Deviations, disclosed

- **AR-R-4's own arithmetic, corrected, ruling text unchanged.** AR-R-4
  states Deferred's live-row count drops 13 → 12. That is the
  relocation's own isolated effect; AR-R-3 lands in the same step and
  adds exactly one Deferred row back in (the `:mllp` sink), netting to
  **13, unchanged**. Both rulings' own row-level actions were executed
  exactly as specified — this is the ruling text's own summary
  arithmetic not netting the two together, not a premise failure
  against the live tree. Disclosed in full in ADR-0048 rather than
  silently editing the ruling's own verbatim text or letting the wrong
  number stand. `.agents/state.md` is not edited (AR-C-1 — regenerates
  at arc close only); it currently states 13 from before this session,
  and 13 is also the number after this session — no drift for a future
  arc-close session to reconcile, though the reason (12-then-13, not a
  flat unchanged 13) is worth knowing if that session diffs closely.
- **AR-R-5's mid-arc register append, licensed not silent.** See
  Scope, Step 3, above, and `.agents/rulings.md`'s own note at its
  ADR-0048 section.
- **`notes/adr/README.md`'s own file count, fixed forward.** Found
  stale (45, pre-dating this session's own ADR-0048 file) while
  landing Step 4 — same staleness class ADR-0047's own Step 0 table
  already named for this exact file. Corrected to 46 in the same
  commit as the file that made it stale, per this workspace's
  fix-forward rule.

## Findings (disclosed, not fixed — out of this session's own fence)

- None beyond AR-R-1's own probe table. No audit work was performed;
  the brief's §4 checklist and its seeded findings S1/S3/S5/S6 remain
  entirely the audit session's own job, per this session's fence.

## Verification

- `bin/regression-oracle 89e327f 8cb712f` (baseline: this session's
  own pre-session tip; target: the tip immediately before this
  record's own closing commit — no `src` touched at any point this
  session): `IDENTICAL: every root's digest matches between 89e327f
  and 8cb712f` — all ELEVEN vendored-root batches (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as expected for a docs-only session. No `--declared-digest-change`
  licensed or needed.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline except the one expected `index-completeness-test`
  failure (42/43); green (43/43) from Step 1 forward through Step 3.
  At Step 4, landing the roadmap's own Done pointer one commit ahead
  of `notes/ADRs.md`'s own ADR-0048 index line produced the EXPECTED
  transient failure in `done-pointer-adr-test`
  ("`.agents/plans/roadmap.md`'s Done section cites ADR number(s) not
  in `notes/ADRs.md`'s own index: [\"ADR-0048\"]") — red→green proof
  the gate actually catches a dangling pointer, not just an assertion
  it does; green immediately after the index line landed. Shape
  otherwise unchanged from Step 0's own baseline throughout.
- `clojure -M:poly check`: OK, this step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every checkpoint (Steps 1–3): each
  showed exactly one delta against its own message file — `git log
  --format=%B -1`'s own trailing-newline artifact, the same known,
  harmless class prior sessions already name.

Commits, in order: `eb97f1f` (Step 1, brief lands), `9f20ba3` (Step 2,
roadmap hygiene), `8cb712f` (Step 3, rulings register append), and
this session's own closing records commit (Step 4).

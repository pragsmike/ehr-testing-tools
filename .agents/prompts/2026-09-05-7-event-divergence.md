# 2026-09-05 — the 7-event divergence (prompt as issued)

Archived verbatim. The session's own record is
[`../session-records/2026-09-05-7-event-divergence.md`](../session-records/2026-09-05-7-event-divergence.md).

---

Session: the 7-event divergence -- one path or one version? -- 2026-09-05

Context: demos/scenarios/dense-7500/README.md:153 reports 167,190 events for
`corpus generate sim` (measured 2026-09-04, pre-ADR-0179). The performance-measurement
record (.agents/plans/2026-09-05-performance-measurement/measurements.md:164-178)
reports 167,197 for `sim run --format ground-truth` at 3114dbfe, same seed, flags and
cmp-identical config, and left it unchased per R-measure-first. Both paths reach
sim/run-command in-process (components/corpus/src/ehrt/corpus/sim_adapter.clj:36-68).
Either the two invocations diverge at one tip (arc 4's emission-does-not-move-ground-
truth law, ADR-0175 s.4, is broken) or the README's figure predates ADR-0179's bed
release at merge (dense-7500 has 68 merges absorbing a bed-holder per the census).
Derivation only; no engine change. Fresh clone at 28bf36b4 or later. WSL only.

Read first: AGENTS.md; SKILL.md (:87-88, :142); measurements.md:164-178; sim_adapter.clj:
36-68; notes/adr/0175-*.md s.4; notes/adr/0179-*.md (Decision, declared oracle change);
dense-7500/README.md:100-160; bin/ground-truth-bracket header.

Author rulings, verbatim and binding:
 R-derive-first: "diff the two paths at ONE tip before any claim about either."
 R-edit, R-cap, R-pins (standing).

Steps (one gate each; commit message given):
1. bin/preflight. At the clone tip, same seed 20260824 --patients 7500 --churn, config.edn:
   (a) `sim run --format ground-truth` to A.edn; (b) `corpus generate sim --out-dir` and
   take its ground-truth artifact as B.edn (channel expectation: the out-dir carries a
   ground-truth file the census can read -- correct from generate.clj). Count both with
   bin/event-census. Invariant: one tip, one JVM each. Gate: both exit 0.
   No commit yet.
2. If A = B byte-for-byte (or event-count-equal with a documented envelope difference):
   the divergence is a VERSION delta. Diff A against the README's basis is impossible
   (its log is gone), so instead regenerate at 007deea6 in a second clone (NOT a
   worktree -- version tests) and diff 007deea6-A against tip-A per event; the delta
   should be explained entirely by ADR-0179's bed release (transfers/placements moving
   after a merge). Record the per-event diff and the first divergent index.
   If A != B: diff per event, name the first divergent event and the code path that
   produced it; STOP after recording -- that is a broken law and needs a ruling.
   Gate: the record states which verdict, with the diff as evidence.
   Commit: "plans: 7-event divergence derived -- <version delta | path divergence>".
3. Docs, verdict-dependent. Version delta: re-measure dense-7500's three README rows and
   the Scale table (consuming-ground-truth.md:576-578) at the tip with the README's own
   protocol, and add ADR-0179's declared-oracle-change list the roots it did not have
   (dense-7500) as an addendum. Path divergence: no docs; the STOP record is the output.
   Gate: make test (row and figure gates). Commit: "docs: Scale table and dense-7500
   README re-measured at <tip> after ADR-0179" (version-delta case only).
4. Session record; archive prompt; close ceremony; push; verify CI; close-marker commit.
   Commit: "docs: record CI success at <sha> -- 7-event divergence close".

---

## Deviation record

Four, all disclosed in
[`../session-records/2026-09-05-7-event-divergence.md`](../session-records/2026-09-05-7-event-divergence.md)
at the section named beside each.

- **One warm-up run for the battery, not one per cell** (§7, D1). The
  warm-up run exited 1, at the witnessed-figures step and nowhere else.
- **Step 3 widened past "three README rows and the Scale table"** (§7,
  D2) to the 750 row, both derived-arithmetic paragraphs, and the
  five-column referential table, two of whose columns moved.
- **All four cells were baselined at `007deea6`, not just the one the
  step names** (§7, D3), because ADR-0178 landed between the README's
  own 2026-09-04 basis and `007deea6` and was a live candidate for part
  of the movement. It moved no count.
- **`measurements.md`'s own "unexplained divergence" section was
  retitled RESOLVED with a pointer** (§5c) — outside the step's named
  file list, and the section's body left exactly as its own session
  wrote it.

One prompt premise was corrected rather than followed: step 2 predicts
the delta "should be explained entirely by ADR-0179's bed release".
R-bed explains four of the five moving kinds; the fifth is R-queue,
whose population ADR-0179 measured as empty in every corpus it could
see and which is **not** empty here (§4).

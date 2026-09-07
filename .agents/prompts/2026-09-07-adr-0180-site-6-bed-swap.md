# Session prompt — 2026-09-07 — ADR-0180 site 6: decide :bed-swap reads the swap view

Archived verbatim. Record:
[`../session-records/2026-09-07-adr-0180-site-6-bed-swap.md`](../session-records/2026-09-07-adr-0180-site-6-bed-swap.md).

---

Session: ADR-0180 site 6 -- decide :bed-swap reads the swap view -- 2026-09-07

Context: decide :bed-swap (decide.clj:1414-1432) is the 7,500 recording's top project
frame at 12.00%, scanning the whole :patients map per bed-swap into `eligible`, drawn
positionally. Its replacement already exists: fold/swap-eligible, the second view of the
:eligible-index concern landed at site 5 (24ed60a1), proven equal to the inline scan at
every replay entry by eligible-index-test (naive-swap-eligible, the pinned :location
world of c83ea961). The docstring at fold.clj:87 discloses the inline scan as the one
place the concern still fails R-no-second-path. This session is the repoint and its
measurement. Output-identical; every commit bracket-proven. Fresh clone at 0b7976db or
later. WSL only. No sub-agents. R-sentinel: no session-waking sleep waiters.

Read first: AGENTS.md; SKILL.md (:87-88, :142); ADR-0180 addendum :522-544 (site 6),
:545-571 (the two views); decide.clj:1414-1432 and :1449-1475 (how :merge reads its
view -- copy the shape); fold.clj (swap-eligible and the :87 disclosure);
eligible-index-test (the law; do not weaken); the site-5 record (matched-profile method,
eval-ordinal attribution; R-budget-stop on :docs at headroom 0).

Author rulings, verbatim and binding:
 R-fold-carrier, R-equivalence, R-no-second-path, R-hash-order, R-read-throws,
   R-order-2 (site 6 enacted here). R-move-not-improve, R-pins, R-edit, R-cap.
 R-view-unchanged: "fold/swap-eligible is not edited this session; if the repoint needs
   it changed, STOP and record why -- the view's law was proven at site 5 and a change
   reopens it."

Steps (one gate each; commit message given):
1. bin/preflight. Baseline the two cells (expect 3018299a..., c22d6573...); bracket
   IDENTICAL at the clean tip. No commit.
2. Repoint. decide :bed-swap reads (fold/swap-eligible world pid) for `eligible`; the
   inline scan is deleted with no fallback; the :87 disclosure is retired in the same
   commit and naive-swap-eligible's docstring becomes "the definition, kept as the gate"
   (the words site 5 used for the merge view). Nothing else in the method changes: same
   draw, same exclusion of self, same :step-rejected path. Invariant: both cell logs
   byte-identical. Gate: bracket IDENTICAL + make test.
   Commit: "engine: decide :bed-swap reads :eligible-index's swap view (ADR-0180 site 6)".
3. Measure. Two cells; matched 7,500 JFR pair (before from a worktree at 0b7976db, the
   site-5 method); a22500-nopersons once (digest must equal 7d105743...). Dated site-6
   section; roadmap P1 site line (:onboarding headroom 15, must not fall below 8).
   Also record, for site 7's benefit: the post-site-6 share of sim/run.clj's in-run
   check-all in generate, and check's replay count at both cells.
   Gate: make test. Commit: "plans: site 6 measured -- decide :bed-swap <before> -> <after>".
4. Session record; archive prompt; close ceremony; push; verify CI by sentinel; close-
   marker commit. Commit: "docs: record CI success at <sha> -- ADR-0180 site 6 close".

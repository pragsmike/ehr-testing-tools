# Archived prompt: stale-path-gate-widening (2026-08-15)

Session prompt as issued, verbatim. Landed as ADR-0137. Two of its
design premises did not survive contact with the tree and were
registered rather than improvised (rulings R-B2, R-B3, recorded at the
end of this file with R-B1).

---

SESSION PROMPT -- review-3 fix session B: widen the scan root, then fix what it finds (D1-2, D1-8)

Drafted by the design channel, 2026-08-15, against a fresh public clone at `15f5943` (fix session A landed and channel-verified: all four converter outputs regenerate byte-identical cross-machine; D5 closed). The channel re-confirmed B's factual inputs from the tree: `notes/facts-register.md` exists (so the six third-party-sources links are mechanical re-depths, not gone-targets), and both gone-targets (`.agents/memory/patterns.md`, `.agents/plans/archive/judge-gate-refactor.md`) are confirmed absent.

Author rulings, verbatim

* "accept all." (2026-08-15) -- adopting the review-3 plan's Session B design as proposed, including its load-bearing exclusion list.
* The disposition of the 6 gone-target links is DELIBERATELY not pre-ruled: this prompt carries a mid-session STOP-AND-REPORT for them (Step 3), because the right fix is per-sentence and needs the author's view with the sentences in hand.

Read first

* `.agents/plans/2026-08-15-repo-review-findings.md` rows D1-2 and D1-8 (the finding and the four false-positive classes the widened gate MUST encode)
* `.agents/plans/2026-08-15-repo-review-3-plan.md` Session B
* `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj` (the gate being widened; its docstring's "Deliberately scoped" sentence is the third scan-root hit and gets retired by this session)
* `components/sim/docs/third-party-sources.md` (the highest-severity instance: a licensing/provenance doc whose six dead links all point at the register its own claims rest on)

Step 0 -- Preflight
`bin/preflight` plain; verify `git rev-parse stable-20260815-result-nodes^{}` = `b139de589083c6b4967c1a4769b2c6a8d17feac4`. No tag owed -- the review arc tags at its step-7 close. Baseline tip must be `15f5943` or a descendant; report what it is.

Step 1 -- Widen the gate, witness red
Widen `stale_path_test.clj`:

1. Scan root: from `docs/` (plus the `use-cases.edn` source) to every tracked `*.md` under `docs/**` AND `components/*/docs/**`, plus the repo-root reader surfaces it already covers. Retire the "Deliberately scoped" docstring sentence and replace it with the population statement and how it is enumerated (the amended rubric's population-closure law, applied to the gate itself).
2. Add dead-markdown-link resolution: every `](relative/path)` link resolved from its file's own directory; every root-anchored link/backticked path from the repo root.
3. Encode ALL FOUR exclusion classes from register row D1-8, or the gate lands noisy and will be weakened later: (a) shorthand backticked citations (`sim/run.clj` style) -- only root-anchored backticked paths are checked, which excludes the convention structurally rather than by list; (b) generator template sources (`components/corpus/docs/ use-cases.edn`) whose links are authored to resolve at the generated output's location; (c) `docs/dev/migration/polylith-brief.md`'s external tutorial example paths; (d) percent-decode (`%20`) before resolution -- an encoding step, not an exclusion, so the spaces-in-filename research doc resolves as any renderer would.
4. Witness RED: exactly 25 hits, all 25 under `components/*/docs/` -- 19 un-re-depthed `../` prefixes + 6 gone-targets, per D1-2. A different count, or any hit outside that root, or any hit inside vendored-verbatim bytes, is a STOP-AND-REPORT, not a recalibration. Record the full 25-hit list in the session record -- it is the fix list and the fence.

Step 2 -- The 19 mechanical fixes
Fix the 19 un-re-depthed links (`../` -> `../../../`, per-link verified against the resolved target's existence, not applied as a blind rewrite). `components/sim/docs/third-party-sources.md` first. Re-run the gate: exactly 6 remaining, all gone-targets.
NOTHING IS COMMITTED YET -- the gate cannot land red, and the fixes co-land with the gate in one commit once all 25 are resolved.

Step 3 -- STOP-AND-REPORT: the 6 gone-target links
For each of the 6, report to the author (via the design channel): file:line, the full sentence containing the link, the gone target, and per-link options with an evidence-based recommendation: (i) re-point -- only if a genuine successor artifact exists in the tree (name it and why it is the successor); (ii) rewrite the sentence linkless, preserving its claim; (iii) delete the sentence, if the claim itself died with the target. Then WAIT. Do not guess a disposition; do not commit.

Step 4 -- After the ruling: green, co-land, close

1. Apply the ruled fixes; re-run the gate -- zero hits.
2. One commit: widened gate + all 25 fixes, red witnessed in the session record. Message (message-via-file, ASCII).
3. Records: ADR (next free number); register rows D1-2 -> FIXED and D1-8 -> encoded-in-gate, ADR cited; session record (with the 25-hit list, the exactly-19/exactly-6 midpoint, and the ruling verbatim); prompt archive.
4. Full `make test` unpiped, `MAKE_EXIT` captured. Expected blocks: 636 (this session widens an existing test namespace; it adds none). Assertions will rise; blocks should not. Explain any block delta rather than absorbing it.
5. Push; `bin/post-push-verify` PLUS the by-hand full-range check (its range defect D1-6 is live -- two sightings now -- and is Session C's subject, not this one's).

Fences

* Touch ONLY: `stale_path_test.clj`, the link-bearing doc files named by the red run's 25-hit list (and no doc file outside that list), the register's two disposition cells, and the close artifacts.
* Zero `src/` outside the one test namespace; zero converter or generator changes; vendored bytes verbatim.
* STOP-AND-REPORT: red count != 25; any hit outside `components/*/docs/`; any hit in vendored bytes; post-Step-2 remainder != 6; any exclusion class needing loosening beyond D1-8's four (that is a new false-positive class and belongs in the register, not silently in the gate); fence pressure of any kind.

---

## Mid-session rulings (2026-08-15), verbatim

* **R-B1** (Step 3, the 6 links): *"Re-point all six (Recommended)"* --
  point the 5 pattern-#15 links at
  `notes/tools/agents/memory/patterns.md` and `palgebra-design.md`'s
  Companion at `notes/tools/agents/plans/archive/judge-gate-refactor.md`,
  updating that line's backticked label to match.
* **R-B2** (the backticked root-anchored half): *"Ship link-half,
  register the rest (Recommended)"*.
* **R-B3** (the retired-name denylist families): *"Register it, keep
  docs/ scope (Recommended)"*.

Both R-B2 and R-B3 answer premise mismatches the prompt could not have
anticipated: the prompt's Step 1.2 and its "retire the Deliberately
scoped sentence" instruction each rest on a premise that does not hold
against the live tree. Registered as new rows D1-9 and D1-10 rather
than improvised into the gate -- the STOP-AND-REPORT fence for "any
exclusion class needing loosening beyond D1-8's four" firing exactly as
written.

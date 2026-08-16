# 2026-08-15 -- Repo-review 3 arc close: step-7 close ADR, post-arc scoreboard, review 4's watch-list

Archived driving prompt, verbatim as supplied by the design channel,
followed by this session's own deviation record.

---

## The prompt as supplied

SESSION PROMPT — review-3 arc close (skill step 7), arc tag, and the three scheduling rulings
Drafted by the design channel, 2026-08-15, against a fresh public clone at `b96c246` (Session C landed and channel-verified — the fixed `bin/post-push-verify` was independently witnessed on a synthetic 3-commit push with a non-ASCII middle message: derived the recorded pre-push tip, checked all three, exit 1; and its loud-fail floor witnessed separately against a fresh clone with no remote-tracking reflog, exit 2 rather than a `tip^1` guess).
Author rulings, verbatim

* "accept all." (2026-08-15) — R-1/R-2/R-3 as recommended.
* "Concur. Go." (2026-08-15) — Q1 a: the step-7 close is its own short session (this one), with its own tag; Q2 a: the D8-5 fence battery is chartered standalone BEFORE review 4; Q3 a: tighter cadence — the next repo review is chartered after roughly 15 ADRs from this close, not on the calendar.

Read first

* `.agents/skills/repo-review/SKILL.md` step 7 (the close law: "findings fixed / ruled / accepted / intake, the scoreboard's movement, and the NEXT review's inherited watch-list") and its Output section
* `.agents/plans/2026-08-15-repo-review-findings.md` (the register, now with 17 FIXED cells across sessions A-C; scoreboard at :327)
* `.agents/plans/2026-08-15-repo-review-3-plan.md`
* ADR-0136, ADR-0137, ADR-0138 (the three fix-session ADRs — the arc's evidence)
* The last arc close in the standing pattern (the UX arc close, ADR range 0056-0064, or the alignment arc close) for the close ADR's shape

Step 0 — Preflight and the arc tag

1. `bin/preflight` plain; verify `git rev-parse stable-20260815-result-nodes^{}` = `b139de589083c6b4967c1a4769b2c6a8d17feac4`; report the baseline tip (`b96c246` or descendant).
2. Pay the arc tag on the fix-session tip `b96c246` (the arc's substantive work; the close ADR this session writes is records- only and follows the tag, exactly as ADR-0133/0135's pattern): `bin/tag-ceremony stable-20260815-review-3-fixes b96c246430038b4d38aa60a391de5e376e61cd24 <message-file> --push`. Message file supplied with this prompt (`tag-message-review-3-fixes.txt`). License case (i): channel fresh-clone verification of all three fix sessions plus the author-side CI check — if the CI relay is absent from this prompt's context, STOP-AND-REPORT before pushing the tag. Peeled-ref verify against the exact SHA is the receipt.

Step 1 — The close ADR (next free number; expected 0139)
Structure, per the step-7 law:

1. Findings by disposition, re-derived from the register's LIVE rows (the skill's arithmetic law applies to the close as much as to the register): count FIXED / registered / ruled / close-as-fine / intake / still-candidate directly from the table cells, and state where each FIXED cell's ADR citation points. Expected shape (verify, do not copy): every fix-session-candidate row opened BY the review is FIXED or registered (D5-3, D5-4, D2-4, D1-2, D1-8, D1-5/D2-5, D7-3, D7-4, D1-6, D2-6, D4-3); the two candidates opened DURING the arc by fix session B (D1-9, D1-10) are NOT this arc's work and move to the watch-list.
2. Scoreboard movement, RE-SCORED against the live tree, not the review-day tree: D5's RED was earned by unregistered derivations that are now registered and gated (ADR-0136) — the close records D5 at its post-fix score with the reason; D1's YELLOW was earned by 25 dead links now fixed under a widened gate (ADR-0137), but D1-9/D1-10 remain open — score it honestly with both facts stated; D2/D7/D8's yellows likewise re-examined against what actually landed. Present as a fourth column ("post-arc") next to the review-3 column, so review 4 inherits both the finding-day score and the close-day score. Rule 9 (repeat-hit classes) applies: the registry-as-population class is now at five recorded instances plus three fix-session sightings — name it in the close as the arc's central finding.
3. The inherited watch-list for review 4, explicit rows: D1-9 (backticked-path shorthand — the basename-shorthand class, with the two real stale citations B surfaced named as its evidence); D1-10 (denylist-family widening: 15 files, triage owed); D8-5 (fence battery — chartered standalone before review 4 per Q2 a; name it as owed, with its own session); D3 (local cold-clone probe substituted by CI's cold runner two reviews running); D1-4's method note (compare the two sets, not their cardinalities); the H-2/H-3 incident classes now gated (build-session law, post-push-verify fix) — watch for recurrence, not for the defect.
4. Cadence and process rulings on the record: Q3 a — the next review is chartered after ~15 ADRs from this close (ADR-0139 → review 4 at ~ADR-0154), stated as the standing rule with its rationale (44 ADRs exceeded one session's coverage; three probes had to be recorded blocked/partial). Q2 a — the D8-5 fence-battery session precedes review 4 regardless of the ADR count. Q1 a — the arc closes in its own session with its own tag, so the arc's record has a discrete endpoint.
5. Channel error ownership, carried into the record (the design channel's own errata this arc, so review 4's history scan finds them in-repo and not only in a chat transcript): sim-theory-diagram misclassified as hand-authored (unearned specificity — prompt fenced it read-only on an inference; ADR- 0135's Step 3.5 corrected it); the six "gone-target" links inherited from the register without a `git log --all` probe (carry-forward — both targets were frozen successors, session B found them); `--expect-tag` attributed to `bin/preflight` (carry-forward — flag exists on a different script); the "read origin/<branch> before the fetch" mechanism (unearned specificity — `git push` advances that ref; session C used the register's reflog alternative); `core.clj` line citations naming `mutate`/`batch` sites as `gate`'s (unearned specificity — grep for the category, not the call sites; session C fixed at the CLI seam). Five items, two classes, all corrected in-session by probing before acting — record them as evidence that the session-side probe discipline held under a channel that erred.

Step 2 — Records and close

1. Register: append a dated close note under the scoreboard pointing at ADR-0139; do NOT rewrite the review-day rows or scores (the register is history; the close ADR carries the post-arc column).
2. Roadmap: the review-3 arc row → CLOSED with the ADR range (0136-0139) and the tag; add the two chartered follow-ons as rows — "D8-5 fence battery (before review 4)" and "repo review 4 (after ~15 ADRs from ADR-0139; D1-9/D1-10 inherited)".
3. Rulings rows: "accept all." and "Concur. Go." verbatim with their glosses.
4. `bin/close-scaffold`; session record (tag receipt, re-derived disposition counts, the post-arc scoreboard); prompt archive.
5. Full `make test` unpiped, `MAKE_EXIT` captured — expected blocks 640 (no test namespace added by a records-only session; explain any delta). Push; `bin/post-push-verify` with no arguments (its fix is live — record the derived range and the count checked, which for a records-only push may be one commit; that is correct, not a regression); by-hand check no longer required now that the fix has three witnesses (C's own push, C's test, the channel's synthetic witness) — its retirement is itself worth one line in the record.

Fences

* Records-only: `notes/adr/0139-*.md`, `notes/ADRs.md` index line, the register's close note, `.agents/plans/roadmap.md`, `.agents/rulings.md`, session record, prompt archive, tag ceremony artifacts, and any index the completeness gate requires (disclose mechanically-required touches in the commit message).
* Zero `src`, zero docs outside the register/roadmap, zero regeneration.
* STOP-AND-REPORT: the CI relay for the tag license absent; any register cell whose live disposition disagrees with what ADR- 0136/0137/0138 claim (report the disagreement, do not reconcile by editing either); block count != 640 unexplained.

### The tag message file, as supplied

```
Repo-review 3 fix sessions A-C, under the amended (population-
closure) rubric adopted at dbbeb1f. A: every string-diagram
derivation registered in the make graph and CI freshness gate, three
stale teaching examples regenerated, inert guard retired, three
standing items registered (ADR-0136). B: stale-path gate widened to
every tracked doc surface, 25 dead links fixed, six "gone" targets
found frozen at notes/tools/agents/ (ADR-0137). C: post-push-verify
derives the pushed range from origin's pre-push reflog and fails
loud when underivable, build-session names explicit exit-code
capture, gate reports :path-unreadable (ADR-0138). Every fix
red-witnessed before landing; every landing channel-verified by
fresh public clone with independent cross-machine re-derivation
where the mechanism allowed (converter outputs byte-identical;
dead-link scan zero; post-push-verify synthetic 3-commit witness).
Tag paid at the arc close under license case (i): channel
verification 2026-08-15 plus author-side CI check.
```

---

## Deviations from this prompt, recorded

Five, all disclosed in `notes/adr/0139-review-3-arc-close.md` and
summarized here so the prompt archive stands alone.

1. **The tag fence fired and the STOP was taken; the push is held.**
   Step 0.2's license, case (i), required the author-side CI relay.
   The channel-verification half is present in this prompt in full;
   the CI half is **absent** — no run id, no `gh` output, no relayed
   conclusion (contrast the register's own Step-0 row, which cited
   "run 31884986962 green on `b139de5`"). The session stopped and
   reported, offering the evidence it could gather itself
   (`bin/preflight` showing CI green at exactly `b96c246`) and naming
   ADR-0134's precedent, where this same fence fired for this same
   missing relay and the author ruled *"Pay it, message verbatim"* on
   session-side preflight evidence. No ruling came back in-session, so
   the annotated tag was created **locally only** and the push held.
   Recorded as this close's own mechanical debt.
2. **"17 FIXED cells" does not re-derive.** The Read-first section's
   figure is wrong by every reading: 11 cells moved this arc, a naive
   marker grep returns 18, and 7 of those 18 mark review-2 findings in
   evidence cells. Corrected in the close's tally per the skill's
   arithmetic law, which this session applied to its own prompt.
3. **"five recorded instances plus three fix-session sightings" is an
   undercount.** Re-derived: five recorded by the review, two more
   opened during the arc (D1-9, D1-10), three more opened by this
   close's own re-scoring (C-1, C-2, C-3) — ten instances, plus the
   three live sightings of D1-6's under-coverage that Session C
   quantified. The close states ten.
4. **The roadmap had no review-3 arc row to flip.** Step 2.2's premise
   does not hold: the arc was chartered channel-direct and its sessions
   registered their findings as rows without one ever being opened for
   the arc itself. Verified by grep before acting. The row is created
   already-closed, in the shape ADR-0135's channel-direct row used.
5. **Three findings were opened by a records-only close** (C-1, C-2,
   C-3), found by re-scoring the scoreboard against the live tree as
   Step 1.2 instructed. All three are registered on the roadmap and in
   the close ADR and **none is fixed** — fixing them is outside the
   fence, and the fence is respected. Not writing them down would have
   been the failure mode this whole arc is about.

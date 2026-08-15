# Archived prompt: review-3 fix session A -- register every derivation (D5-3, D5-4, D2-4), with ruled riders -- 2026-08-15

Driving prompt for the session recorded at
`.agents/session-records/2026-08-15-review-3-fix-a-register-derivations.md`.
Archived verbatim per charter R-A (`notes/ADRs.md` ADR-0023). Drafted
by the design channel, 2026-08-15, against a fresh public clone at
`fca52ec`.

---

SESSION PROMPT — review-3 fix session A: register every derivation (D5-3, D5-4, D2-4), with ruled riders

Drafted by the design channel, 2026-08-15, against a fresh public clone at `fca52ec` (repo-review-3 register and plan landed; review arc OPEN at the skill's step-5 STOP — this is the first step-6 fix session under it). The channel independently confirmed the headline finding by regenerating all three example diagrams itself: `ai-study-flow-v3` 0 committed `_out` vs 3 regenerated, `committee-flow` 0 vs 6, `deliberated-choice-flow` 0 vs 6 — every delta exactly ADR-0135's result-node feature.

Author rulings, verbatim

* "accept all." (2026-08-15) — binding the channel's recommendations as put: R-1 delete `bin/check-palgebra-drift`, with the load-bearing zero-caller inventory recorded at deletion; R-2 register BOTH unregistered standing requests as roadmap rows now — visibility first, disposition later; R-3 D5's RED stands as scored (severity tracks the mechanism, not this instance set's blast radius).
* Session batching and gate design follow the review's own plan (`.agents/plans/2026-08-15-repo-review-3-plan.md`, Session A), which the author's "accept all." adopted.

Read first

* `.agents/plans/2026-08-15-repo-review-findings.md` rows D5-3, D5-4, D2-4, D1-5, D7-3, D7-4 (the evidence this session acts on)
* `.agents/plans/2026-08-15-repo-review-3-plan.md` Session A (the ruled design)
* `Makefile` (docsgen/use-cases/pipeline targets — the pattern the two new targets join)
* `.github/workflows/test.yml` (the generated-doc freshness step being extended)
* `components/sim/docs/sim-theory-diagram.md` header AND `components/sim/docs/sim-theory-equations.txt` header — note the ADR-0135 session's own warning, which this prompt encodes as a constraint below: `%% Arrow N` numbering derives from the equations file's LINE numbering, so a header edit that changes that file's line count silently renumbers every arrow.
* `bin/check-palgebra-drift` (the R-1 deletion subject) and `notes/carve-loss-audit.md` (where its disposition row lands)

Step 0 — Preflight

`bin/preflight` plain (it has no `--expect-tag` flag — verify tag substance directly instead): `git rev-parse stable-20260815-result-nodes^{}` must equal `b139de589083c6b4967c1a4769b2c6a8d17feac4`. No tag is owed by this session: the review arc is open and tags at its step-7 close.

Step 1 — The gate first, witnessed red

1. Add two Makefile targets following the docsgen pattern:
   * `sim-theory`: runs the converter on `components/sim/docs/sim-theory-equations.txt` producing `components/sim/docs/sim-theory-diagram.mermaid`, and verifies (or refreshes) the `.md`'s embedded block against it. A check-only comparison for the embedded block is sufficient if splicing is awkward — the invariant is byte-agreement between the three surfaces (equations -> .mermaid -> embedded block), however enforced.
   * `palgebra-examples`: regenerates the three `components/palgebra/examples/*-flow*.mermaid` from their sibling `*-equations.txt`. Fold both into `docsgen` so the population is one target.
2. Extend CI's generated-doc freshness step to diff these paths alongside the existing five.
3. Witness RED before any regeneration lands: run the freshness check locally against the current tree. It must fail on exactly the three stale examples (sim-theory is fresh as of ADR-0135 and must NOT fail). Exactly-three is the proof the gate has the right population; more or fewer is a STOP-AND-REPORT, not a recalibration.

Step 2 — Green: regenerate and retire the hand recipes

1. Regenerate the three stale examples via the new target; re-run the freshness check — green.
2. Retire the header-recipe workflow: in BOTH `sim-theory-diagram.md` and `sim-theory-equations.txt`, replace the hand-run recipe with a pointer to `make sim-theory`. Constraint: the `sim-theory-equations.txt` edit must preserve that file's exact line count (pad or trim comment lines within the header, the ADR-0135 precedent) so no `%% Arrow N` renumbering occurs and the freshly-registered gate stays green. If line-count preservation proves impossible, the fallback is to absorb the renumbering by regenerating in the same commit WITH the churn disclosed in the commit message — but attempt preservation first, and STOP-AND-REPORT if neither path is clean.
3. Keep ADR-0135's historical disclosure note in the diagram header intact — it quotes the dead path deliberately; it is a record, not a recipe.
4. Commit — gate and fix co-landed (Makefile + CI + regenerated files + headers in one commit; red witnessed in the session record). Message (message-via-file, ASCII):

```
feat: register every string-diagram derivation in the make graph
and CI freshness gate (review-3 D5-3/D5-4/D2-4, closes D5 RED)

Tree-first enumeration found 10 derived artifacts against 5
registered; the 3 palgebra teaching examples were stale against
their own converter (missing ADR-0135's result nodes),
channel-confirmed by independent regeneration. New make targets
sim-theory and palgebra-examples fold into docsgen; CI freshness
now diffs all of them. Red witnessed 3x before regeneration.
Hand recipes retired for make-target pointers; equations-file
line count preserved to avoid Arrow-N renumbering.

```

Step 3 — Ruled riders (R-1, R-2)

1. R-1: delete `bin/check-palgebra-drift`. Re-derive the zero-caller inventory fresh (grep Makefile, both workflows, bin/, .agents/skills/) and record it in the commit message; add the disposition row to `notes/carve-loss-audit.md` (its sibling premise — `../ehr-testing-sim` — retired at the merge).
2. R-2 + D7-4: three roadmap rows:
   * Deferred row for the Synthea demographics extraction (`components/sim-model/resources/sim-model/demographics/NOTICE:26`), revisit trigger stated verbatim: a session with a Synthea checkout available. Add a register pointer line at the NOTICE if that file may be edited without disturbing vendored bytes — if the NOTICE is inside the vendored-verbatim fence, roadmap-row-only and say so.
   * Row for `docs/dev/source-sink-design.md:56` OPEN-4 (`--engine` flag), carrying its open question as the row's own question — disposition deliberately not decided here, per the ruling's "visibility first."
   * Row for the loopback flake (18 days in `state.md` alone — register row D7-4's third-instance pattern). Commit riders separately from Step 2 (message-via-file each or one combined rider commit; cite register rows in the message).

Step 4 — Records and close

Session record + prompt archive via `bin/close-scaffold`; ADR (next free number) recording red/green, the exactly-three witness, the line-count constraint's outcome, and both riders; update the register's D5-3/D5-4/D2-4/D1-5/D7-3/D7-4 rows' dispositions in place (fixed / registered) with this ADR cited — the register is the review arc's working document and stays truthful as fixes land. Full `make test` before push — unpiped, full log, `MAKE_EXIT` captured, block count reconciled against the 636 baseline (this session adds no test namespace; expect exactly 636 unless the tree says otherwise, and explain any delta rather than absorbing it). Push; `bin/post-push-verify` — NOTE its known range defect (register D1-6, Session C's subject): verify the full pushed range by hand as ADR-0135's session did, and say so in the record.

Fences

* Touch ONLY: `Makefile`, `.github/workflows/test.yml`, the five derived artifacts and their two sim-theory headers, `bin/check-palgebra-drift` (deletion), `notes/carve-loss-audit.md`, `.agents/plans/roadmap.md`, the register's disposition cells, and the close artifacts.
* Zero `src/`, zero converter changes — the converter is correct; only its outputs were stale. If any step seems to require a converter change, that is a STOP, not a widening.
* Vendored bytes stay verbatim (the NOTICE caveat above).
* STOP-AND-REPORT: red count != 3; equations line-count preservation and the disclosed-churn fallback both unclean; local freshness check disagreeing with CI's; any fence pressure.

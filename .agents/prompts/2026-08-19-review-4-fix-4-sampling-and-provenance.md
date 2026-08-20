# Archived prompt: review-4-fix-4-sampling-and-provenance (2026-08-19)

Verbatim session prompt, archived per charter R-A. Landed as ADR-0158.

---

Session prompt -- review-4 fix 4/4: sampling adequacy and the owed
rows (plan Session F + R4-Q4 a, R4-Q5 b, R4-Q9), every artifact points
back at its inputs (plan Session H), and the author-paid residue row
closes -- ADR-0158

Context

Claude Code under R30 in ehr-testing-tools, final fix session of the repo-review-4 arc (the arc CLOSE is a separate, later session). HEAD at handoff: bdc10ee (ADR-0157 addendum; tree clean; CI green at the last five runs per the author's own preflight paste 2026-08-19; last tag `stable-20260819-review-4-fix-3-environment-and-result-or-loud` @ae396cf, no tag owed). Author rulings 2026-08-18: "Q1 accept all recommendations. Q2 that order ok. Q3 pair small ones." F and H are the two remaining sessions; they share no surface, so they pair. Rows by id are the findings register; sessions by letter the plan Part 2 (F :330, H :430 -- re-derive line numbers). Quote the row.

AUTHOR ACTION completed 2026-08-19 (transcript-witnessed, to be repo- recorded HERE): the edit-root residue was paid on penny -- `core. fileMode true`, `core.ignorecase` unset, ~360 mode-only changes restored via `git checkout -- .`, `git status` clean, `bin/preflight` exit 0 with `OK: core.fileMode is true` / `OK: core.ignorecase is unset`. Step 0 verifies what a session CAN (the row exists; preflight's two checks exist; CI green) and closes `roadmap.md#edit-root-worktree- residue` citing the author's confirmation, dated 2026-08-19 -- the close text says "author-confirmed", not "session-verified": a session cannot probe penny's edit root and must not claim to.

Channel anchors at bdc10ee (re-derive):

* F/D6-1: `every-m1-run-satisfies-the-invariant-catalog` -- find the defspec (grep; my clone shows it in `sim-engine/test`, file not pinned here on purpose). 150 trials, FIXED facility: ED 0 beds/15 surge, Renal 1 bed/0 surge, no churn. ADR-0153's defect needed one ward with BOTH bed classes + churn: structurally invisible at any trial count. Fix: generate the facility (wards with mixed licensed+surge counts) and put a churn profile on a fraction of trials. The plan's co-landed gate: prove the widened defspec RED against the pre-ADR-0153 engine -- do this in a scratch worktree at `4d6ff78` (the commit before the 0153 fix): cherry-pick the widened test there, run, witness the surge-invariant failure, record seed and trial; then green at HEAD. That is red-first with the red on a historical tree -- say so in the ADR, it is the strongest form the plan names.
* F/D7-3: D1-9 (backticked-path shorthand) and D1-10 (denylist-family widening), ruled R-B2/R-B3 on 2026-08-15 (ADR-0137), no row through one arc close and fourteen ADRs. ONE roadmap row for the pair, rulings cited, priced small.
* F/D1-1: four ADRs cite a suite figure to an ADR that does not carry it (the figure lives in the session record). Recommended remedy per the row: the RULE, not a retro-edit -- a `build-session` close-step sentence: an ADR's reconciliation sentence cites where the figure LIVES (its own Verification section, or the named session record). Do NOT rewrite the four old ADRs (R-RP); list them in ADR-0158.
* R4-Q4 (a): gate `bare-on-README+SETUP = 0` NOW -- 1 README + 3 SETUP fences get exercised (the fence-census tooling from ADR-0140/0148 is the instrument; find the census fn and its register); the manual's 21
   * use-cases' 13 -> ONE roadmap row with its own session, priced real (primed-artifact-cache caveat quoted from the plan).
* R4-Q5 (b): staleness tripwire over the five `docs/manual/assets/*.svg` banners -- a test parsing each banner's cited source and asserting the source file unchanged since the SVG's own last commit (`git log -1 --format=%H -- <asset>` vs the source's last-change; red = source moved after asset). Mermaid block in `trajectory-computation.md`: accepted as hand-owned, one dated line at the block saying so (R4-Q5's (d) scoped to it, per the plan's own text).
* R4-Q9: `#intake-staging-dir` -- the author has NOT stated a trigger; the plan's remedy set is {trigger, convert, close}. Given "accept all recommendations" and the row's 19-day age: CLOSE it, close text citing R4-Q9 and "re-open on the first real staging need, with the trigger stated then". Corpus-player slices: a `## Next` row (chartered ADR-0014; bed-board sink, `:mllp`, accumulator wiring; unpriced, needs its own ruling before a session takes it) -- visibility first, disposition later.
* H/L3-3: `state-derived.md` renderer emits its own input list; find the renderer (docs-tooling). L3-5: `formats.md` banner names both inputs. L3-6: the python converter named in each of its 28 artifacts' banners -- the converter writes the banner, so this is one change in the converter plus regeneration (declare: `.mermaid` files gain a `%%` banner line -- the ADR-0135/0152 arrow-numbering hazard: `%% Arrow N` numbering derives from EQUATION-file line numbers, not mermaid lines -- VERIFY that claim before regenerating, it decides whether 28 artifacts' diffs are banner-only). L3-7: 14 traces get a per-directory note (the traces are byte-exact captures -- the note goes in each trace dir's README, NOT into messages*.txt). L3-8: `AGENTS.md` hand list (4 of 53) -> pointer to the generated list (L3-3's output or the docsgen write-set the closure gate already derives -- say which). L3-11: `demos/traces/README.md` names `make traces`.
* H's watch: L3-3/L3-8 add lines to `state-derived.md`, counted by four reading sets -- measure budgets before/after; compact, never raise.

Read first

1. Register rows D6-1, D7-3, D1-1, D8-1, D8-2, D5-2, D7-5, L3-3, L3-5, L3-6, L3-7, L3-8, L3-11; plan Sessions F, H; R4-Q4/Q5/Q9 in full.
2. The defspec file + `check.clj`'s catalog + `facility.clj` (facility generator shapes -- `sim-model` has generators? grep `gen/` there); ADR-0153 (the defect the widened sample must catch), ADR-0010.
3. The fence census tooling (ADR-0140's instrument; `exercised-sources`
   * whatever counts bare fences -- find it); `docs/SETUP.md`, `README.md` (the 4 fences to exercise); the ADR-0149 exerciser pattern.
4. `state-derived` renderer; the python converter; `AGENTS.md`; `demos/traces/README.md` + per-dir READMEs; `formats.md` banner; `trajectory-computation.md` :255-270; `docs/manual/assets/*.svg` banners (read all five).
5. `rulings.md#R-unregistered-request-gets-a-row`, `#R-empty-population- is-red`, `#R-full-suite-before-push`, `#R-red-pushed-with-green`, `#R-session-verifies-ci-via-gh`, `#R-amend-unpushed-message-only`, `#R-io-result-or-loud`; build-session skill; budgets.

Author rulings, verbatim

* "Q1 accept all recommendations. Q2 that order ok. Q3 pair small ones." (2026-08-18). Edit-root residue: author-paid 2026-08-19, preflight exit 0 pasted; close the row on that confirmation.
* `#intake-staging-dir`: closed per R4-Q9's recommendation as bound by "accept all recommendations"; if you read the recommendation as requiring a fresh author choice among {trigger, convert, close}, STOP-AND-REPORT instead of choosing -- two defensible readings.
* Tag: no tag owed at Step 0. This session's own close tag: pay in- session if its tip run concludes success while open, else next Step 0 -- say which.

Step 0
Fresh clone, tip bdc10ee; `bin/preflight`; baseline `make test` unpiped, MAKE_EXIT captured, wrapper ends `exit "$MAKE_EXIT"`, reconcile vs ADR- 0157's 358 blocks / 4,040 tests / 18,110 assertions; `poly check`; budgets. Then: (a) the defspec's current facility verbatim; (b) the bare-fence count on README+SETUP re-derived (predict 4) and the census instrument named; (c) the five SVG banners' cited sources listed, and for each whether the source moved since the asset's last commit -- PREDICT: if any tripwire is born red, that is a true stale-asset finding: report it, exercise judgment -- a born-red gate lands ONLY with its finding rowed (the asset is stale, someone must redraw), never silenced; (d) the `%% Arrow N` numbering-source claim verified; (e) the 28-artifact converter write set enumerated; (f) `#edit-root-worktree- residue` and `#intake-staging-dir` rows read in full.

Step 1 -- F red (three small reds)
(i) The widened defspec: facility generator (mixed wards) + churn on a fraction of trials, proven RED at 4d6ff78 in a scratch worktree (seed + failing trial recorded), green at HEAD. (ii) README+SETUP fence gate: census assertion `bare-on-{README,SETUP} = 0`, red today (4 bare). (iii) SVG tripwire test, red iff Step 0(c) found a moved source (else born-green with its plant: touch a cited source in a scratch tree, witness red, revert). Commit: "test: red -- defspec facility generalized with churn (proves 0153's defect catchable), README+SETUP fences exercised, SVG staleness tripwire (ADR-0158, review-4 F, R4-Q4 a, R4-Q5 b)"

Step 2 -- F green
Defspec lands green at HEAD (trial count: keep 150 unless wall time says otherwise -- record it). The 4 fences exercised via the ADR-0149 pattern (register rows, `check-all` green). Tripwire green or its finding rowed. Build-session close-step sentence (D1-1's rule); the D1-9/D1-10 row; the manual+use-cases fence row; corpus-player row; `#intake-staging-dir` closed per ruling; `#edit-root-worktree-residue` closed author-confirmed. Full `make test`; push red+green. Commit: "fix: sampling adequacy + owed rows -- generalized defspec, front-door fences gated at zero, SVG tripwire, figures cited where they live (ADR-0158, review-4 F)"

Step 3 -- H red
Tests: (i) `state-derived.md` contains its generated input-list section (assert section header + non-empty + every listed path exists); (ii) every converter-written artifact carries the converter banner (`%%` for mermaid) -- population from the closure gate's write-set derivation, non-empty; (iii) `AGENTS.md` points at the generated list and its hand list is gone (assert the pointer, assert no stale "4 files" claim); (iv) `demos/traces/README.md` mentions `make traces`; (v) `formats.md` banner names `event-examples.edn`. Commit: "test: red -- artifacts name their inputs: state-derived self-list, converter banners, AGENTS.md pointer, traces front door (ADR-0158, review-4 H)"

Step 4 -- H green
Renderer emits the input list; converter writes its banner (regenerate -- per Step 0(d), diffs are banner-only or STOP); per-dir trace README notes; `AGENTS.md` pointer; `formats.md` line; `trajectory-computation. md` hand-owned line. Budgets re-measured; compact if breached. Full `make test`; push red+green. Commit: "fix: every generated artifact points back at its inputs (ADR-0158, review-4 H)"

Step 5 -- register hygiene
Rows D6-1, D7-3, D1-1, D8-1, D8-2 (partial -- say what remains), D5-2, D7-5, L3-3, L3-5, L3-6, L3-7, L3-8, L3-11 -> `FIXED ADR-0158` dated APPEND (D8-2's append names the 34-fence row as the remainder); plan F, H marked landed; roadmap `#repo-review-4` line -> "fix 4/4 (F+H) ADR-0158; arc close owed" (at cap, compact). New rows as Step 2.

Close (self-archive FIRST)
Archive to `.agents/prompts/2026-08-19-review-4-fix-4-sampling-and- provenance.md`; open the session record; then ADR-0158 (the historical red's seed/trial; fence census before/after; tripwire dispositions; the 28-artifact diff class; rows opened/closed table), registers, session record with `gh run view` id/conclusion, full `make test` reconciled per namespace vs Step 0, `bin/post-push-verify`, tag per ruling. Commit: "docs: ADR-0158 -- review-4 fix 4/4: sampling adequacy and artifact provenance, close"

Fences
src: the defspec + one facility-generator helper (sim-model or the test ns -- say which; NO engine/check logic change), `state-derived` renderer, the python converter (banner emission ONLY), docs-tooling tests, the 4 exercised fences' scripts/register, SVG tripwire test, `AGENTS.md`, READMEs, `formats.md` banner, `trajectory-computation.md` one line, registers; regenerated artifacts banner-only diffs (STOP otherwise); NO oracle root/digest change -- oracle: the converter and renderer are off the digest path, predict IDENTICAL and assert; historical-tree work in a scratch worktree, never on HEAD's branch; no test deletions; every planted red withdrawn; exit codes unpiped; ASCII messages (the hook is live); anchored register edits, dated appends; R-RP. READ-BACK: files touched vs this list; the fence census number before/after; per-artifact diff class for the 28; rows opened(3)/closed(4) named.

# Prompt archive — 2026-08-25, de-scaffold

Archived per `.agents/prompts/README.md`. Author ruling of 2026-08-25,
quoted by the prompt itself: **"go as written."** Pasted into a Claude
Code session at `d6ad63a`; one session, one commit, no ADR, no tag.

## The prompt, verbatim

> Session prompt -- de-scaffold (author ruling 2026-08-25: "go as written")
>
> Context. `.agents/`+`notes/adr/` is 125,551 lines against 66,379 of Clojure; in the window since 2026-08-20 scaffolding churned 3 lines per payload line (review 5, ADR-0170, showed the scaffolding generating its own work). The author has ruled a de-scaffold and a moratorium. HEAD d6ad63a. One session, one or several commits, NO ADR, NO tag, push at the end. Read review 5's plan `.agents/plans/2026-08-25-repo-review-5-plan.md` Q-A..Q-J and this prompt; nothing else is read-first. Where a step names a file, `ls` it first; if absent, say so and move on -- do not invent a substitute.
> Steps (each item ends with `make test` green; docs-only steps may rely on CI).
>
> 1. Q-C only. Land `R-witness-population-is-counted` as a GATE, not a row: `run_test.clj`'s two ADR-0163 population-scale gates (`:809`, `:825` at d6ad63a; `unpaired-ends` :772) assert `(empty? (unpaired-ends ...))` over corpora holding zero `:medication-end` / zero `:care-plan-end`. Add a pinned non-zero count of cited end events to each, from a run that has them (arc 0's engine_test companions are the worked example, register row L1-6). Red-before-green. Every other Q-A..Q-J: append one line to the plan, "closed by de-scaffold ruling 2026-08-25", no other edit.
> 2. Delete: `bin/tag-ceremony`, `bin/close-scaffold`, and in `components/docs-tooling/test/ehrt/docs_tooling/`: `tag_law_test`, `prompt_record_pairing_test`, `notes_prompts_frozen_test`, `attic_rotation_test`, `done_pointer_adr_test`, `state_staleness_tripwire_test`, `reading_set_budget_test`, `state_residue_test`, `rulings_lint_test`, and `roadmap_lint_test`'s `no-row-exceeds-six-lines-test`. Remove their source-side helpers only if nothing else calls them (`grep -rn` first). Keep every other gate. `Makefile` targets that only served these go too.
> 3. `rulings.md`: header line "FROZEN 2026-08-25 -- laws now land as gates or not at all; rows are historical." Delete rows whose text a surviving test already enforces (name the test per deleted row in the commit body). `R-full-suite-before-push`: add "docs-only diffs exempt; CI runs the suite." `R-arc-closes-in-own-session`, `R-review-cadence-in-adrs`, the tag rulings: mark RETIRED in place.
> 4. Skills, in `.agents/skills/` AND the `.claude/skills` mirror: keep `build-session`, `probe`, `session-prompt`, `string-diagram`, `wsl-windows-git-hygiene`, `scenarios`, `README.md`; delete the rest. `README.md` lists the six. `skill_mirror_currency_test` must stay green.
> 5. `roadmap.md`: `## Done` becomes one line pointing at `notes/ADRs.md`; rotation prose and cap prose removed; OPEN rows reduced to payload: arc 1-4 rows, `#performance-residual-sites`, corpus-player slices, NIST engine wiring, guide chapters, plus the Q-C gate row if it is not closed by step 1. Every other row: one line each under `## Retired 2026-08-25` (slug + reason "de-scaffold") so cited slugs still resolve.
> 6. `docs/dev/simulator-architecture.md`: replace the 12 `engine.clj:NNN` citations with `defn` names. Same file, no other change.
> 7. `state.md`: keep only hazards that bite at d6ad63a (each must name the command that reproduces it); everything else deleted. `.agents/memory/`: correct what ADR-0170's L-3 found stale; add no maintenance promise.
> 8. `AGENTS.md` (or wherever sessions are told the ceremony): the new rules in one paragraph -- ADRs only for payload-behaviour or contract decisions; no per-arc tags, CI-green at tip is the marker; docs-only pushes need no suite; moratorium on scaffolding-only sessions until arc 1 and one guide chapter land; findings from payload sessions are one record line, not a row.
> 9. Regenerate generated surfaces (`make state-derived`, indexes, ADR index); full `make test` once, unpiped; push; `bin/post-push-verify`; `gh` CI conclusion in the record. Session record: one page, what was deleted (counts), what was kept and why, one line per premise of this prompt that was wrong. Archive this prompt.
>
> Fences. No `src/` change outside step 1's test. No ADR. No tag. No new rulings rows. No new skills. If a deletion breaks a surviving gate, keep the smallest thing that fixes it and say so; never re-add a deleted gate.

## Deviation record

Everything the session did differently from the prompt, and why. The
substance is in `.agents/session-records/2026-08-25-de-scaffold.md`;
this is the index.

1. **Step 5's "every other row" was read as "every other OPEN row."**
   `## Deferred` and `## Externals` were left standing. The sentence
   opens "OPEN rows reduced to payload", and the two keep-list entries
   that match nothing in `## Next` — "NIST engine wiring" and "guide
   chapters" — are both explained by that narrower reading.
2. **The `## Retired 2026-08-25` heading is `## Done -- retired
   2026-08-25 (de-scaffold)`.** `roadmap_lint_test` requires a `CLOSED`
   row to live under a `## Done`-prefixed heading, and that gate was not
   on the delete list.
3. **Two rulings rows that qualified for deletion were kept** —
   `R-io-result-or-loud` and `R-audience-has-entry-path` — because live
   source and test files cite them by slug and `src/` was fenced.
4. **Four kept surfaces were edited beyond the letter of their step**
   (`build-session/SKILL.md`, `session-prompt/SKILL.md`, `bin/preflight`,
   `state_derived_test.clj`), each to remove an instruction to run a
   script this session deleted.
5. **`.agents/memory/` needed no correction** (see premise 4 in the
   record); the out-of-repo agent memory that L3-12 actually names was
   corrected instead.

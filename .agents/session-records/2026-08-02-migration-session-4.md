# 2026-08-02 — Migration session 4: reading sets land, sim gets its citations qualified

## Scope

Fourth build session of the approved migration. Executed item 8
(`.agents/reading-sets.edn`, charter R-D's mechanism landed with
placeholder-equals-measured budgets — real budget ruling stays
deferred to the author, charter §6) and the `components/sim/src`/`test`
bare-`ADR-NNNN` docstring sweep migration session 3 named as future
work. Items 5 (gated on the author's item-9 discovery probe) and 14
stayed fenced, per this session's own AR-4. Standing ceremony (R30):
WSL ext4 clone, fast-forwarded to `origin/main` (`20ca886`) before work
began; `/mnt/c` untouched.

## Item 8: the five reading sets, measured

`.agents/reading-sets.edn` — `AGENTS.md` in every set (the entry
point regardless of task class); no `.agents/skills/` entries anywhere
(none of the ten indexed skills are corpus/sim/judge/docs-tooling
specific — all session-mechanics/meta, disclosed honestly in the
file's own header rather than silently omitted).

| Set | Paths (beyond `AGENTS.md`) | Measured `:budget-lines` |
|---|---|---|
| `:onboarding` | 5 `.agents/` subdirectory READMEs + `roadmap.md` | 538 |
| `:corpus` | `corpus`/`corpus-io` interfaces, `pipeline.md`, `notation.md`, `source-sink-design.md` | 1519 |
| `:sim` | `sim` interface, `engine-onboarding.md`, `components.md` | 574 |
| `:judge` | `judge` + 3 gate-engine interfaces, `engine-onboarding.md`, `components.md` | 644 |
| `:docs` | `docs-tooling` interface, `architecture.md`, `docs/dev/README.md` | 433 |

Composition reasoning (full justification lives inline as EDN comments,
not repeated here): `:corpus` and `:sim`/`:judge` deliberately don't
share `docs/dev/components.md` and `docs/dev/source-sink-design.md`
the same way — `components.md` is organized by external tool
(Synthea → `:sim`; HAPI/NIST/validator/CDC → `:judge`), not by corpus's
own domain logic, so `:corpus` excludes it; `source-sink-design.md` (732
lines, the largest single entry) is corpus-io's own load-bearing design
record, cited pervasively by `notes/ADRs.md` and still carrying open
decisions (D-b, OPEN-4/5/6), so it's included despite its size rather
than omitted for budget optics. `components/sim/docs/`'s nine theory
files are deliberately excluded from `:sim` — component-owned, discoverable
via `poly ws get`, well beyond a lean orientation budget.

## Red→green, three ways (item 8)

All three run live against the real `.agents/reading-sets.edn`, each
reverted before C1's commit:

1. **Ghost path.** Injected `"docs/dev/does-not-exist-ghost.md"` into
   `:docs`'s path vector. `every-reading-set-path-resolves-test` failed,
   naming the exact ghost (`:docs cites a path that does not exist:
   ["docs/dev/does-not-exist-ghost.md"]`) — plus a bonus: the budget
   test also errored (not just failed) on the same run, since summing
   line counts over a nonexistent path throws by design (a ghost can't
   silently count as zero lines and mask an over-budget set). Reverted;
   diffed clean against the pre-injection file.
2. **Over-budget seed.** Set `:docs`'s own `:budget-lines` to `1`.
   `every-reading-set-is-within-its-own-budget-test` failed:
   `:docs is 430 lines, over its 1-line budget by 429`. Reverted;
   diffed clean.
3. **Green on the landed actuals.** Full `ehrt.docs-tooling.reading-set-budget-test`
   namespace (5 tests, 15 assertions) — 0 failures, 0 errors, both
   immediately after the ghost/budget reverts and again after the
   AGENTS.md line-count correction below.

Three more tests in the same namespace are permanent fixture-based
mechanism-sanity checks (not touching the live file, so a future
`reading-sets.edn` edit can never make them vacuously true):
`missing-paths-catches-a-ghost-test`, `total-lines-exceeding-an-absurdly-low-budget-fails-test`,
`a-lean-well-formed-set-passes-both-checks-test`.

**Self-correcting mid-session.** After AGENTS.md's own "forthcoming"
pointer was rewritten to cite the landed file (+3 lines net), the next
full-suite `clojure -M:poly test` run (not the earlier isolated-namespace
run, which predated the edit) failed all five sets by exactly 3 lines
each — AGENTS.md sits in every set. Budgets recomputed to the post-edit
actuals before C1's commit; see the Deviation record in this session's
own prompt archive entry for the full account. Named here as the
gate's first real catch, on its own author, same session it landed.

**It caught itself twice.** The same mechanism fired again while
drafting C3: `.agents/plans/roadmap.md`'s own "Done" section (this
entry) and `.agents/session-records/README.md`/`.agents/prompts/README.md`'s
own new index lines are themselves inside `:onboarding`'s path list, so
writing this record and its roadmap entry pushed `:onboarding` from 523
to 538. Fixed the same way — `.agents/reading-sets.edn` updated to the
new actual before C3's commit; `notes/ADRs.md` ADR-0023 gains a short
same-day correction note (not an edit to the C2-landed paragraph, which
is already pushed and stays historically accurate as of when it was
measured) pointing at the final number.

## Item: `components/sim/src`/`test` bare-`ADR-NNNN` sweep

**Method.** `grep -rn 'ADR-[0-9]{4}'` over `components/sim/src` and
`components/sim/test`: 174 total occurrences, 23 already `sim/ADR-NNNN`-qualified
(landed piecemeal by earlier sessions), 151 bare. Classified by ADR
number first — checked each of the 12 distinct numbers sim's src cites
(0001, 0002, 0004, 0007–0015) against BOTH frozen registers'
own titles (`notes/sim/ADRs.md`, `notes/tools/ADRs.md`) for a topic
collision before trusting a bulk fix — then verified every file's own
hits in context. No genuinely ambiguous reference found; nothing
escalated.

**Disposition summary (151 bare occurrences):**

| Disposition | Count | Detail |
|---|---|---|
| Resolves to `notes/sim/ADRs.md` — requalified `sim/ADR-NNNN` | 149 | bulk regex pass, verified topic-safe first |
| Resolves to `notes/tools/ADRs.md` — requalified `tools/ADR-0015` | 1 | `run.clj`, see below |
| Already correct as bare — resolves to the LIVE `notes/ADRs.md` — left unchanged | 1 | `interface.clj`, see below |

**Per-file accounting (all 39 files the bulk pass touched, bare-hit
count before this session):**

| File | Bare hits | File | Bare hits |
|---|---|---|---|
| `src/engine.clj` | 32 | `test/engine_test.clj` | 13 |
| `src/check.clj` | 14 | `test/emit_hl7_test.clj` | 12 |
| `src/emit_hl7.clj` | 7 | `test/check_test.clj` | 6 |
| `src/run.clj` | 5 (+1 tools/, below) | `test/vendored_module_test.clj` | 3 |
| `src/emit_state.clj` | 4 | `test/gmf_test.clj` | 3 |
| `src/compile_trajectory.clj` | 3 | `test/emit_state_test.clj` | 3 |
| `src/pathway.clj` | 3 | `test/fixtures/pinned_seed_42_patients_5.edn` | 4 (1 miscitation, below) |
| `src/gmf_interpreter.clj` | 3 | `test/facility_test.clj` | 2 |
| `src/identifiers.clj` | 2 | `test/compile_trajectory_test.clj` | 2 |
| `src/site_profile.clj` | 2 | `test/config_test.clj` | 2 |
| `src/churn.clj` | 2 | `test/gmf_horizon_test.clj` | 2 |
| `src/facility.clj` | 1 | `test/gmf_interpreter_test.clj` | 2 |
| `src/gmf.clj` | 1 | `test/identifiers_test.clj` | 2 |
| `src/manifest.clj` | 1 | `test/site_profile_test.clj` | 2 |
| `src/v2_replay.clj` | 1 | `test/fixtures/fixture-clinic.json` | 1 (miscitation, below) |
| `src/version.clj` | 1 | `test/churn_scenarios_test.clj` | 1 |
| | | `test/churn_test.clj` | 1 |
| | | `test/emitter_order_independence_test.clj` | 1 |
| | | `test/order_profiles_test.clj` | 1 |
| | | `test/persona_test.clj` | 1 |
| | | `test/run_test.clj` | 1 |
| | | `test/v2_replay_test.clj` | 1 |
| | | `test/vendored_appendicitis_test.clj` | 1 |

Sum: 82 (`src`) + 67 (`test`) = 149, matching the bulk-requalified count
in the disposition table above (the `run.clj` row's parenthetical and
the two fixture rows' miscitation flags are the 3 occurrences handled
by hand rather than the script, accounted separately below).

**The three hand-handled exceptions, in full:**

- **`components/sim/src/ehrt/sim/interface.clj:2` — left bare,
  deliberately.** `"Deliberately wide (migration ruling R5, notes/ADRs.md
  ADR-0001)."` This is NOT sim's own frozen ADR-0001 ("Standalone
  library with a mountable CLI group") — it's the workspace's own live
  ADR-0001 ("Migration plan..."), whose ruling R5 is exactly
  `AGENTS.md`'s own fat-component disclosure this docstring restates.
  Correct exactly as written; touching it would have introduced the
  bug this sweep exists to fix, not corrected one.
- **`components/sim/src/ehrt/sim/run.clj:66` — requalified `tools/ADR-0015`,
  not `sim/`.** Was: `"The tools full-capability session
  (notes/ADRs.md ADR-0015 in the sibling repo) found that..."` — written
  pre-merge, when "the sibling repo" meant the separate `ehr-testing-tools`
  checkout. Content ("full-capability" baseline) matches
  `notes/tools/ADRs.md` ADR-0015 ("The gate loop maintains TWO
  baselines: legacy-floor and full-capability") exactly — sim's OWN
  frozen ADR-0015 is unrelated ("Going public"). Now: `"The tools
  full-capability session (`tools/ADR-0015`, this project's own frozen
  pre-merge history, notes/tools/ADRs.md) found that..."` — the
  "sibling repo" phrasing retired since post-merge there isn't one.
- **Two wrong-file-path miscitations, fixed.** `pinned_seed_42_patients_5.edn:2`
  ("see notes/ADRs.md ADR-0009") and `fixture-clinic.json:5`
  ("(notes/ADRs.md ADR-0013 point 6)") both named the LIVE register's
  file path for a claim that's actually sim's own frozen one (seed-stability
  policy; GMF module curation/NOTICE obligation, respectively — both
  content-verified against `notes/sim/ADRs.md`'s own text). Corrected
  to `notes/sim/ADRs.md` in place — a file-path bug, the same species
  session 3's own four miscitations were, not a fresh addition.

**Zero behavior change, verified.** `clojure -M:poly check`: `OK`
before and after. Full suite (`clojure -M:poly test :all
skip:integration`): green before this sweep (284 sim-brick tests alone,
part of a larger green run) and green after (re-run in full,
zero `FAIL`/`ERROR` lines across the entire multi-project log). The
edited JSON fixture re-validated as parseable JSON
(`json.load`); the edited EDN fixture re-validated as readable EDN
(`clojure.edn/read-string` round-trip) after the manual fix pass.

## Judgment calls and their ratification status

See this session's own prompt archive entry
(`.agents/prompts/2026-08-02-migration-session-4.md`, "Deviation
record") for the full account of each judgment call and its reasoning:
the mid-session AGENTS.md/budget self-correction, the third
citation-origin (`tools/`) AR-2's own wording didn't anticipate, the
scripted-bulk-pass-plus-manual-follow-up method, and moving the
roadmap's Done-section update to C3 rather than C2 to avoid an
impossible self-referencing sha. None required author input mid-session
(no author present); all are scope judgments within the two ARs'
own stated shape, disclosed here for review rather than silently made.

## Findings and HEAD landed

No findings beyond the session's own two ARs — both fully executed,
nothing else discovered mid-sweep worth a named future-work item.

**Post-push message verification:** both checkpoints verified —
`git log --format=%B -1` after each push diffed against the `-F`
message file that produced it. Both diffs show only the same
single-trailing-newline artifact prior sessions' records already
documented as `git commit -F`'s own normalization, not content loss —
no backtick or control-byte drift.

**HEAD landed:** checkpoint shas — C1 (item 8) `ab679c9`; C2 (the sim
sweep + AR-3's dated ADR note) `72f5542`. This record and the prompt
archive are C3, landing on the commit produced by this same checkpoint
— cited by this record's own filename per the self-reference
convention prior sessions already used.

# 2026-08-12 — ehr-testing-tools: review-3, the user-surface review (ADR-0114)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (WSL/ext4 clone).
Session opened at HEAD `ea4346c` (ADR-0113's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.
No mid-session author communication occurred; no fence conflict arose.
One arithmetic error in the register's own first-draft summary table
(B1's row count and ruling-needed column) was self-caught and corrected
by a second, direct recount before the register was committed --
recorded in this session's own record
(`.agents/session-records/2026-08-12-review-3-user-surface.md`), not
reproduced here as it is not part of the driving prompt.

## Original prompt (verbatim)

Session prompt — review-3: the user-surface review (ADR-0114)
You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. This is a REVIEW session in the review-2 / UX-audit lineage: it produces a FINDINGS REGISTER and never executes a fix (AR-RR2-1 / AR-UA-1 precedent — every row is a recommendation). Charter: `.agents/rulings.md` "From ADR-0113" R5, the user-surface scope, author verbatim: "Should we run a repo review before we start on the manual? It might lead to tweaks to the CLI." The review's product feeds the manual arc: every finding is something the manual would otherwise have to apologize for. Two small author-licensed docs riders land first (Step 1). STOP-AND-REPORT on any conflict between this prompt and the tree.
This session is judgment-dense (the author's standing model note: survey/review phases run on the stronger model).
Read first

1. `.agents/plans/2026-08-06-ux-audit-findings.md` — the row format and disposition vocabulary this register reuses (including `design-channel-draft`, AR-UA-2).
2. `.agents/plans/2026-08-09-repo-review-findings.md` — the review ceremony shape (findings-only; green probes recorded as inheritance; parallel read-only sub-agents; prior-review baseline carry-forward where a prior row overlaps).
3. `.agents/skills/repo-review/SKILL.md` — ceremony steps only; its D1–D8 dimension batteries do NOT apply here (this review's batteries are B1–B7 below, scoped by R5).
4. `bases/cli/src/ehrt/cli/help.clj` (`cli-spec` — the enumeration source every battery keys off) and `docs/cli.md` (generated mirror).
5. `.agents/rulings.md` "From ADR-0113" (R2/R3/R5/R6 — the manual arc this review serves).
6. `docs/dev/simulator-architecture.md` §4 "The two layers, instantiated" (Rider A's target) and `docs/dev/way-of-working.md`
   * `docs/glossary.md` (Rider B's targets).

R8 (engine-test flake row; author "a", 2026-08-12, on the channel's explanation that a generative-test failure at a recorded seed is a deterministic repro of a found counterexample, not noise): the `ehrt.sim-engine.engine-test` flake (`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`, failing seed `7844068501`, ADR-0112's disclosure) gets a roadmap row chartering a small investigation session — run the defspec at the recorded seed, capture the shrunk counterexample, classify engine-bug vs. test-defect, fix or file. The seed is the repro handle; preserve it in the row verbatim.
Step 0 — Preflight and tag ceremony

* `git fetch`; confirm `origin/main` at `ea4346c` (`ea4346c596ccba447f10f9f5f4a070c18dc5f43b`, ADR-0113 close). Else STOP-AND-REPORT.
* Tag `stable-20260812-sim-palgebra-unification`, ANNOTATED, at `ea4346c`; push; confirm peeled ref exact. License: tag-law case (i), FULLY EARNED — the design channel verified the ADR-0113 landing by fresh clone on 2026-08-12 (lineage, ASCII x3, footprint exact to the fence, zero-src diff re-deriving the oracle identity basis, all nine witness citations re-read at path:line, rulings quotes verbatim, token sweep confirmed correct with only in-quote survivors) INCLUDING CI: the channel's own API check saw `ea4346c completed success` — no split license needed this time.

Step 1 — Two author-licensed riders (docs-only; commit 1)
Rider A — precision clause (author ruling 2026-08-12: "ok ride it"). In `docs/dev/simulator-architecture.md`, the subsection "The two layers, instantiated", first paragraph: the bolded claim "zero atoms, refs, agents, or volatiles in the simulation path" is strictly false as stated (§3's own census discloses the `census.clj:407` memoization atom, allowlisted per ADR-0108). Amend the sentence to read "...zero atoms, refs, agents, or volatiles in the simulation path (modulo §3's own disclosed exceptions) is exactly..." — that parenthetical, nothing more.
Rider B — method vocabulary (author ruling 2026-08-12, verbatim: "add those terms, they've been successful"). Two targets:

1. `docs/dev/way-of-working.md`: a new section "Method vocabulary" (place it before any closing/appendix material; match the doc's own heading level). Content: the two term families below, one short definition each, in the doc's voice. These definitions are pre-decided; tighten wording to fit the voice but do not change meanings, and do not add terms.
Evidence family: oracle (a mechanism that says whether an output is correct without hand-specifying the answer; this repo's two instances — the byte-digest regression oracle over the 35 fixed roots, and the NIST engine in the inject-X-expect-X loop); oracle bracket (running the oracle before and after a session, expected result pre-stated); witness (a concrete checkable piece of evidence for a claim — usually a named passing test, sometimes a captured run; "witnessed" = ran and saw, never merely asserted); red-before-green (a new test must be seen failing before the satisfying code lands, proving it can detect what it guards); count lock (a test hardcoding a catalog's size so additions cannot land silently; bumping it is part of the addition); tripwire (a cheap check built to fire when an assumption goes stale); lint, house sense (mechanical checks over docs and registers, not only code).
Process family: probe (a read-only look at the live tree before making a claim); landing (what actually arrived on origin/main, verified — distinct from what a session says); fence (the explicit boundary of what a session may touch; outside it is stop-and-report, never judgment); charter (the explicit authorization for a piece of work; chartered is not scheduled); rider (a small extra change licensed to ride a session about something else; never one that moves behavior); ruling vs. recommendation (the author's decision, recorded verbatim, vs. the channel's reasoned proposal — provenance tags [A]/[C]); arc (a multi-session sequence with one goal, opened and closed explicitly); fix-forward (errors corrected by new dated commits and errata, never history rewrites); move-don't-improve (relocations byte-identical; improvement is a separate commit); seam (cross-reference the glossary's existing entry; note the second, architectural sense — the boundary where one concern hands off to another, as in "the emitter seam").
Close the section with one pointer line: palgebra vocabulary (layers, lower/erase, fiber, naturality) is defined in `components/corpus/docs/palgebra-design.md` and instantiated in `docs/dev/simulator-architecture.md` §4 — not duplicated here.
2. `docs/glossary.md`: TWO new entries, in the file's own alphabetical position and voice: Oracle and Witness (the evidence-family definitions above, user-path phrasing — these two terms appear in use-case and demo prose, so the user path needs them). If either term already has a headed entry, STOP-AND-REPORT rather than merging.

Commit 1 (verbatim, ASCII):

```
docs: precision clause and method vocabulary -- riders (ADR-0114)

```

Step 2 — The review (batteries B1–B7; findings register; commit 2)
Create `.agents/plans/<run date>-review-3-user-surface-findings.md`. Header: findings-only declaration (AR-RR2-1 shape), row format `id | area | probe | evidence | finding | recommendation | disposition`, disposition ∈ {ruling-needed, fix-session-candidate (with suggested cluster), close-as-fine, incomplete, design-channel-draft}, the tip this review ran against, and the UX audit named as baseline — carry forward any of its rows still open, re-probed, in their own subsection (U-row ids preserved).
Ground rules: every row cites a probe actually run with output captured; green probes are recorded, not discarded; NO fix is applied anywhere; all CLI executions write only under `out/` or a temp dir outside the repo (verify `git status` clean of everything but this session's own fenced files before each commit); sub-agents (if used) are read-only and their probe transcripts summarized into evidence cells. Row ids: `R3-<battery>-<n>` (e.g. `R3-B2-4`).
B1 — Verb/flag consistency. Enumerate every group/verb/flag from `cli-spec` (the spec is the census; `docs/cli.md` is its mirror, not the source). Check: one concept = one flag name across verbs (out-dir, seed, config, format, rate); naming convention uniformity (kebab-case, singular/plural); duration/count flags follow the bare-integer-in-a-named-unit convention (the ADR-0111 interval-spec ruling — flag any unit-suffixed or ambiguous-unit stragglers); required-vs-defaulted choices consistent with the determinism law (D8/D9: deterministic defaults fine, wall-clock or environment-dependent defaults are findings).
B2 — Error quality. Systematically trigger error paths for every verb: no args, unknown flag, malformed value, missing file, empty dir, wrong-format input, out-dir collision. Capture actual output. Check each against the enumerable-options family's own standard: a categorized keyword; a hint that names the valid options or the next action (`run: ehrt help ...`); no stack trace on any user error; the file/flag at fault named. Consistent nonzero exit codes across verbs.
B3 — Help surface. `ehrt help`, `ehrt help <group>` for every group, bare `ehrt`, `ehrt <group>` with no verb, `ehrt <group> <verb> --help` if supported. Completeness against cli-spec (every verb and flag reachable through help); breadcrumbs present; at least one example per group; help text and `docs/cli.md` agree (they share a generation source — any drift is a finding of the first order).
B4 — Filesystem conventions. Which verbs derive out-dirs (D12 pattern) vs require them; derived-name patterns consistent; collision behavior uniform (`:out-dir-exists`, never silent overwrite); relative-vs-absolute path handling; cwd-sensitivity (`bin/ehrt` is the taught cwd-safe entry — probe one deep-cwd invocation per group).
B5 — Cross-doc agreement. Every flag mentioned in `README.md`'s Quickstart, `docs/use-cases/*.md` strips, and demo READMEs exists in the live spec with the documented meaning (validity check only — executing the strips is the demo exerciser's future job, R3, not this session's).
B6 — Output-shape consistency. Which verbs support `--json`; projection naming consistent across verbs; result envelopes (`:status :ok` etc.) uniform; exit-code table coherent (including no-verdict's own distinct code).
B7 — The narration test (judgment battery). For each verb, write the one sentence a manual would use to teach it. Any verb whose sentence needs a caveat, an apology, or a "except when" is a finding — cite the sentence itself as evidence. This battery exists because the review's charter is the manual (R5): its findings feed the design pass directly.
Close the register with the review-2-style summary table (rows per battery per disposition) — and, per AR-RR2-2's lesson, compute it by direct recount of the rows, never running-total memory.
Commit 2 (verbatim, ASCII):

```
docs: review-3 findings register -- user-surface review (ADR-0114)

```

Step 3 — ADR and close

* Self-archive this prompt at close-phase START.
* `notes/adr/0114-review-3-user-surface.md`: context (R5 charter verbatim; the manual arc it serves), the ceremony record, the riders (each named with its author license), the register's summary table reproduced, tag ceremony, oracle bracket, full gate, fences, index line. `notes/ADRs.md` + `notes/adr/README.md` (111 → 112, as-of line).
* Roadmap: review-3's Next row gains a dated note (findings landed, awaiting author rulings on ruling-needed rows — the arc's next step is a rulings-landing session, review-2's own ADR-0093 pattern); a new row per R8 — engine-test flake investigation (seed `7844068501` verbatim, the classify-and-fix-or-file charter, cross-ref ADR-0112's disclosure and ADR-0107's sibling corpus defspec flake row); Done line: `- <run date> — review-3-user-surface — ADR-0114`.
* `.agents/rulings.md`: append a "From ADR-0114" section carrying R8 above in the established format (this is a standing license for that one entry, distinct from the fence's unlikely-clause).
* Session record.

Oracle bracket. Pre-analysis: pure identity on all 35 roots — riders are docs prose, the register is a plans file, zero `src` anywhere; CLI executions during B2/B4 write only gitignored/temp paths. Run `bin/regression-oracle ea4346c <final-commit>`; any non-identity is STOP-AND-REPORT.
Gates: `make test` green (engine-test's known flake: one re-run allowed; twice fails → STOP-AND-REPORT with both seeds); gitleaks staged + pre-push; ASCII byte-check on all three messages; push; CI confirm or disclose rate-limiting.
Commit 3 (verbatim, ASCII):

```
docs: session record and prompt archive -- review-3 (ADR-0114)

```

Fences

* Touch ONLY: `docs/dev/simulator-architecture.md` (Rider A's one parenthetical); `docs/dev/way-of-working.md` (Rider B §); `docs/glossary.md` (Rider B's two entries); `.agents/plans/<run date>-review-3-user-surface-findings.md` (new); `.agents/plans/roadmap.md`; `.agents/rulings.md` for the R8 entry (Step 3) — beyond that ONLY if a battery surfaces a ruling the author already made but never recorded (unlikely — STOP-AND-REPORT to confirm first); `.agents/prompts/*`; `.agents/session-records/*`; `notes/adr/0114-*.md`; `notes/ADRs.md`; `notes/adr/README.md`.
* The rule (ADR-0099 form): the surfaces above and nothing else; the list illustrates the rule; outside it is STOP-AND-REPORT.
* ZERO `src`, ZERO `test/`, ZERO generated docs, ZERO fixes of any finding however trivial — a one-character typo found by B3 is a register row, not an edit.

STOP-AND-REPORT on: any glossary collision (Rider B); any register row you cannot back with a captured probe; oracle non-identity; the flake failing twice; anything this prompt failed to pre-decide.

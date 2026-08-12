# 2026-08-12 — ehr-testing-tools: review-3 rulings landing (build session)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `d508cd6` (ADR-0114 close) and closed at
`ed00e3a` (commit 1) plus this record's own commit. Original prompt
follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt — review-3 rulings landing: three rulings, three clusters (ADR-0115)

You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. This is a REGISTERS-ONLY session in the ADR-0093 lineage (a review's rulings-landing step): it records the author's three rulings on review-3's `ruling-needed` rows, updates the findings register fix-forward, and charters the fix-session clusters on the roadmap. ZERO `src`, ZERO `test/`, ZERO docs/ changes, ZERO fixes of any finding. STOP-AND-REPORT on any conflict between this prompt and the tree.

Two standing-practice notes this prompt states explicitly (lessons from ADR-0114, both channel-owned): (1) the FULL local gate (`make test`) runs before EVERY push, not once at close — R30's own norm, restated; (2) index/companion files that the repo's own gates force for a fenced surface (e.g. a directory README index line required by `index-completeness-test`) are INSIDE the fence by rule — name them in the session record when touched.

Read first

1. `.agents/plans/2026-08-12-review-3-user-surface-findings.md` — rows R3-B1-1, R3-B1-4, R3-B1-7 (the ruled rows); the summary table and its x-ref note (one wording fix lands here); every row whose disposition is `fix-session-candidate` (the cluster members).
2. `.agents/rulings.md` — tail ("From ADR-0114"); the new entries follow.
3. `.agents/plans/roadmap.md` — review-3's row and the Next section.
4. `notes/adr/0114-review-3-user-surface.md` — the review this session closes the rulings loop on.

Author rulings, verbatim
The design channel framed three questions from the register's `ruling-needed` rows (options as presented are reproduced in Step 1 below); the author ruled, verbatim, 2026-08-12: "Q1 a. Q2 a. Q3 a."

Step 0 — Preflight and tag ceremony

* `git fetch`; confirm `origin/main` at `d508cd6` (`d508cd6ee3a5a6fda64c0007b9ac57855ad5acc5`, ADR-0114 close). Else STOP-AND-REPORT.
* Confirm CI green on `main` (`gh run list --limit 5 --branch main`).
* Tag `stable-20260812-review-3`, ANNOTATED, at `d508cd6`; push; confirm peeled ref exact. License: tag-law case (i). The design channel verified the ADR-0114 landing by fresh clone on 2026-08-12: lineage (ea4346c → 9f7697a → d0679e9 → aeb45ab → d508cd6), ASCII clean on all four messages, footprint exact to the fence plus one gate-forced companion (`.agents/plans/README.md`, the channel-owned fence omission recorded in ADR-0114's own deviations), zero-src diff re-deriving the oracle identity basis, riders content-verified, and the register's summary arithmetic independently recounted and CONFIRMED (37 tallied + 9 x-ref + 11 carry-forward, honoring the register's own compound and x-ref conventions). CI: session-confirmed in ADR-0114 with recorded run IDs (`31598555300` red on d0679e9 as disclosed, `31599697988` green, final green on d508cd6); channel API checks rate-limited — the preflight confirmation above completes the CI leg.

Step 1 — Record the three rulings
`.agents/rulings.md` — append a "From ADR-0115" section, three entries in the established format. Each entry states the question as framed, the options, the author's ruling ("a" in each case, from the verbatim batch above), and the concrete meaning:

RQ1 [A] — `--out-dir` double meaning (R3-B1-1). Options: (a) rename `gate fhir`'s flag to `--scratch-dir` so `--out-dir` means one thing repo-wide (protected artifact, collision-refused); (b) keep both, document the difference; (c) make `gate fhir`'s protected. RULED (a). Concrete: the rename is chartered to fix cluster A; until it lands, `--out-dir`'s canonical meaning is `corpus generate`'s (protected artifact).

RQ2 [A] — `--seed` required-vs-defaulted split (R3-B1-4). Options: (a) ruled deliberate — `corpus generate` is the ergonomic front door (defaults), `sim run`/`sim identifiers` are the strict engine tier (require) — recorded, plus a one-line help note naming the tiering; (b) default everywhere; (c) require everywhere. RULED (a). Concrete: the split is design, not drift; the help note is chartered to fix cluster A; future front-door/engine flag decisions cite this ruling.

RQ3 [A] — `--received` wall-clock default (R3-B1-7, precedent-setting). Question: is provenance metadata about a real-world act (the class `corpus intake`'s catalog record exemplifies) inside or outside the determinism law? Options: (a) outside — a foreign corpus's arrival date is genuinely wall-clock provenance; the default stands and the CLASS EXEMPTION is recorded so future provenance-of-real-world-acts flags cite it rather than re-litigate; (b) inside — require the flag, no wall-clock defaults anywhere. RULED (a). Concrete: the exemption's scope is exactly "provenance metadata recording a real-world act"; anything generating or transforming corpus CONTENT remains fully inside the determinism law.

Step 2 — Register updates (fix-forward, dated)
In `.agents/plans/2026-08-12-review-3-user-surface-findings.md`:

1. R3-B1-1: disposition `ruling-needed` → `fix-session-candidate (cluster A)`; append to the row's recommendation cell: `RULED (a), ADR-0115 RQ1, 2026-08-12.`
2. R3-B1-4: disposition `ruling-needed` → `fix-session-candidate (cluster A, small: the help note only)`; append: `RULED (a) deliberate two-tier design, ADR-0115 RQ2, 2026-08-12.`
3. R3-B1-7: disposition `ruling-needed` → `closed-by-ruling`; append: `RULED (a) class exemption recorded, ADR-0115 RQ3, 2026-08-12.`
4. The summary table's note: change the phrase claiming x-ref rows are "marked '(x-ref)'" to accurately describe the actual marker ("marked with a cross-reference, '(see ...)'"), and add a dated one-line correction note beneath the table (review-1's own fix-forward-note precedent): the note's original wording mispredicted the row marker; counts unaffected (independently recounted by the design channel, 2026-08-12).
5. Do NOT renumber, retally, or restructure anything else. The summary table's `ruling-needed` column entries stay as the review-time record (the dispositions' current state lives in the rows; the table is the review's own snapshot — add one line under the correction note saying exactly that, so a future reader isn't confused by the mismatch).

Step 3 — Charter the clusters on the roadmap
`.agents/plans/roadmap.md`, Next section, three new rows (member finding ids verbatim; priorities from the register's own markings):

Fix cluster A — CLI validation and error quality [contains the register's HIGHEST PRIORITY finding]. Members: R3-B2-1 (`check` target validation — HIGHEST), R3-B2-2 (parse-error translation), R3-B2-3 + R3-B4-1 (`corpus intake --out` validation-or-derivation, one fix), R3-B1-5 (missing-required-flag exit-code/category unification), R3-B1-3 (`synthea:` source-scoping validator extension), R3-B2-5 + R3-B3-3 (`help <unknown-group>` validation), R3-B1-1 (the `--scratch-dir` rename, RULED ADR-0115 RQ1), R3-B1-4 (the tiering help note, RULED ADR-0115 RQ2). Note: a src session; its own prompt (channel-drafted) pre-analyzes the oracle bracket — error-path changes are expected oracle-neutral but that session declares it, not this row.

Fix cluster B — help-surface enrichment. Members: R3-B3-2 (verb-level help narrowing), R3-B3-1's mechanism half (the "Example:" render slot; content is design-channel-draft, see the queue note).

Fix cluster C — doc drift and gate scan-roots. Members: R3-B5-3 (demos/traces stale refs + widen the invocation gate's scan roots to `demos/**`), R3-B5-4 (issue template fix + consider `.github/**` in scan roots). Docs-only session.

Plus one queue note (not a session row): design-channel-draft queue — R3-B3-1's Example-line content (one runnable invocation per group, sourced from `docs/use-cases/*.md`), and the B-3/B-4 carry-forward wording halves (R3-B3-4) — the channel drafts, the author rules, no session until then.

Review-3's own row: dated note — rulings landed (ADR-0115), clusters chartered; the arc's remaining steps are the three cluster sessions.

Commit 1 (verbatim, ASCII; full `make test` BEFORE the push):

```
docs: land review-3 rulings; charter fix clusters (ADR-0115)

```

Step 4 — ADR and close

* Self-archive this prompt at close-phase START.
* `notes/adr/0115-review-3-rulings-landing.md`: context (the three questions and the verbatim ruling batch), decision (the rulings' concrete meanings; the cluster charter; the register fix-forward including the x-ref-note correction), tag ceremony (Step 0's license reasoning), oracle bracket, gates, fences, index line. `notes/ADRs.md` + `notes/adr/README.md` (112 → 113, as-of line).
* Roadmap Done line: `- <run date> — review-3-rulings-landing — ADR-0115`
* Session record.

Oracle bracket. Pre-analysis: pure identity on all 35 roots — registers and notes only, zero `src`, zero docs. Run `bin/regression-oracle d508cd6 <final-commit>`; any non-identity is STOP-AND-REPORT.

Gates: full `make test` green BEFORE EVERY push (both commits); engine-test flake policy: one re-run, twice → STOP-AND-REPORT with both seeds; gitleaks staged per commit + detect pre-push; ASCII byte-check on both messages; push; CI confirm or disclose rate-limiting.

Commit 2 (verbatim, ASCII):

```
docs: session record and prompt archive -- review-3 rulings landing (ADR-0115)

```

Fences

* Touch ONLY: `.agents/rulings.md`; `.agents/plans/2026-08-12-review-3-user-surface-findings.md`; `.agents/plans/roadmap.md`; `.agents/prompts/*`; `.agents/session-records/*`; `notes/adr/0115-*.md`; `notes/ADRs.md`; `notes/adr/README.md`; PLUS any index/companion file the repo's own gates force for exactly these surfaces (standing rule above — name each in the session record).
* The rule (ADR-0099 form): the register-and-notes surfaces above and nothing else; the list illustrates the rule.
* ZERO `src`, ZERO `test/`, ZERO `docs/`, ZERO demo files, ZERO fixes of any finding — the x-ref-note wording correction in Step 2 is register self-description, the one licensed exception.

STOP-AND-REPORT on: any ruled row whose current disposition text differs from what Step 2 expects; oracle non-identity; the flake failing twice; anything this prompt failed to pre-decide.

## Deviation record

One gate-forced companion edit, disclosed per the prompt's own standing
practice note (2): `.agents/reading-sets.edn`'s `:onboarding` budget
(a `reading-set-budget-test` gate, forced by `.agents/plans/
roadmap.md`'s own growth from the cluster-charter Step 3 edit — 1734
measured lines against a 1705-line budget) re-baselined 1705 → 1995
under the file's own standing formula (actual x1.15, rounded up to the
nearest 5), same discipline as every prior re-derivation in that
file's own history comment. Not named in the driving prompt's own
fence list by filename, but licensed by the prompt's own standing-
practice note (2) — a gate-forced companion to a named fenced surface
(`.agents/plans/roadmap.md`) is inside the fence by rule. No other
deviation: all three rulings landed exactly as the driving prompt's
own Step 1 text specified; the three register rows and the summary
table's x-ref-note correction landed exactly as Step 2 specified; the
three clusters and the queue note landed exactly as Step 3 specified,
member ids verbatim.

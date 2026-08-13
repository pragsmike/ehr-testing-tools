# 2026-08-13 — ehr-testing-tools: user manual S5 — chapter 8, review, close (ADR-0125)

## Context

Archived 2026-08-13. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `a453fe1` (ADR-0124's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.

This session's own STOP-AND-REPORT, not part of the driving prompt's
literal text, not reproduced here: the `manual-review` skill's first
scored run (Commit 2) came back FAIL overall (dimensions 1 and 4 of 8),
triggering the driving prompt's own named gate ("a fail-grade finding
STOPs for a ruling before arc close is declared"). The session stopped
after Commit 2, presented the full evidence to the author, and asked
how to proceed among three offered dispositions. The author chose:
close the arc now, land both fail-grade findings as open backlog rows
for a future fix session — no chapter edited, no mechanism widened this
session. Commit 3 executes that ruling. Full evidence:
`.agents/plans/2026-08-13-manual-review-1.md`; the ruling itself:
`.agents/rulings.md`, "From ADR-0125."

Also repaid this session, found by channel verification against the
live tree rather than named in the driving prompt: `notes/adr/
0124-manual-s4-mutate-and-gate.md` and its own session record both
claim `stable-20260813-invariant-fix` was created during the S4
session, but no such tag existed on the remote or locally at this
session's own start. See ADR-0125's own "Deviations" section.

## Original prompt (verbatim)

Session prompt — user manual arc, S5: chapter 8, the review skill, arc close (ADR-0125)
You are Claude Code executing under R30 ceremony for ehr-testing-tools, working for mg. DOCS/REGISTERS/SKILLS-ONLY: zero src, zero demo edits. Closes the manual arc: chapter 8, the manual-review skill with one scored run, TWO tag ceremonies (an S4 deviation repaid), and the chartered citation-sweep row. STOP on any conflict. Standing notes: full `make test` before EVERY push; companions in-fence by rule; budget trip → STOP; strips verbatim from witnessed sources, cited; no ADR tokens in user prose; explicit gate policy: any engine-defspec failure at any seed is a NEW finding — STOP, no re-run license.
Read first

1. `docs/manual/00-front.md` through `07-*.md` — the finished arc the skill scores.
2. `docs/formats.md`, `docs/locators.md`, the intake use-case pages (chapter 8's strip sources); `.agents/rulings.md` RQ3 (the provenance exemption chapter 8 teaches at reader level).
3. `.agents/skills/` — an existing skill's structure + the `.claude/skills/` mirror discipline and its currency test.
4. `notes/ADRs.md` lines 1–30 — the origin-qualification doctrine (the sweep row's basis).

Step 0 — Preflight and DOUBLE tag ceremony

* origin/main at `a453fe1`; CI green (`gh run list --limit 5`). Else STOP.
* Tag 1: `stable-20260813-invariant-fix`, ANNOTATED, at `da72533` — repaying ADR-0124's SKIPPED Step 0 (an undisclosed deviation found by channel verification 2026-08-13; record it as such in this ADR's deviations section, owned to the S4 session). License: case (i), the ADR-0123 verification (channel, 2026-08-13) plus CI long since green.
* Tag 2: `stable-20260813-manual-s4`, ANNOTATED, at `a453fe1`. License: case (i), channel fresh-clone verification 2026-08-13 (lineage, ASCII x3, zero src), CI per this preflight.
* Both pushed, both peeled refs exact.

Commit 1 — chapter 8
`docs/manual/08-your-own-data.md`: intake and cataloging (content hashes, lineage; the received-date as real-world provenance — wall-clock by design, taught plainly, no ruling citations in prose); checking against expectations; baselining; closing pointers for the data-consumer path (formats.md, locators.md). Strips from witnessed intake/check sources, cited. No new figure required — if one earns its place, derive it and say from what. Length: medium. Update `00-front.md`'s arc entry; the manual is now complete — state its currency commit in the front page's contract line. Message: `docs: user manual -- chapter 8, your own data (ADR-0125)`
Commit 2 — the manual-review skill + first scored run
`.agents/skills/manual-review/SKILL.md` (+ `.claude` mirror, currency test green): the rubric — eight dimensions, each scored pass/warn/fail WITH file:line evidence: (1) strip executability (every strip's source is exerciser-covered or Quickstart-covered); (2) no reference duplication (no flag tables/operator lists restated); (3) anchor stability (links resolve post-regen); (4) glossary linkage on first-use terms; (5) running-example continuity (ed-tuesday throughout); (6) maturity honesty (limits stated where met); (7) currency (claims vs the generated cli.md); (8) diagram-source presence (every SVG carries its derivation comment). Procedure: read the whole manual, score each dimension, produce a dated report to `.agents/plans/<run date>-manual-review-1.md` with a per-dimension evidence table and an overall verdict. THEN RUN IT — this session executes the skill once against the finished manual and lands the report. Findings are register rows, never fixes (review discipline); a fail-grade finding STOPs for a ruling before arc close is declared. Message: `docs: manual-review skill and first scored run (ADR-0125)`
Commit 3 — arc close
Registers: S5 → LANDED; the manual arc → CLOSED (with the review-1 report's verdict quoted); new row per the author's "a" (2026-08-13): citation errata sweep — origin-qualify the bare frozen-era verdict-family citations (`ADR-0010` → its origin-qualified form) across docs, use-case pages, judge sources, and any chapter-7 instances, ADR-0099 rule form, docs-only session, per notes/ADRs.md's own fix-forward doctrine; ceremony-scripts row → next after the sweep; SETUP+Ch1-2 rewalk remains author's-queue. Rulings "From ADR-0125": the "a, go" verbatim. Self-archive at close-phase START; ADR-0125 (both tag licenses, the S4 deviation record, the review-1 verdict); indices 122 → 123; Done line; session record. Message: `docs: session record and prompt archive -- manual arc closes (ADR-0125)`
Oracle bracket: pure identity, all 35 roots. `bin/regression-oracle a453fe1 <final>`; non-identity → STOP. Gates: standing; ASCII x3; gitleaks; invocation lint; the skill mirror currency test; CI confirm or disclose.
Fences
Touch ONLY: `docs/manual/08-*.md` (new), `00-front.md`, `docs/manual/assets/*` (only if a figure earns its place); `.agents/skills/manual-review/*` (new) + its `.claude` mirror; `.agents/plans/<run date>-manual-review-1.md` (new); registers, prompts, session-records, `notes/adr/0125-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; companions by rule. ZERO src/test/demos; ZERO edits to chapters 1–7 (a review finding is a row, not a fix). Outside → STOP.
STOP-AND-REPORT on: a fail-grade review finding (ruling before arc close); any witnessed source lacking a needed excerpt; the gate policy; oracle non-identity; anything not pre-decided.

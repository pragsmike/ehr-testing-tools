## ADR-0125 — User manual S5: chapter 8, the manual-review skill, arc close

**Status:** Accepted (author-directed, autonomous session per R30 with
one in-session STOP-AND-REPORT), 2026-08-13.

### Context

S5, the fifth and final session of the five-session user-manual arc
(ADR-0119's own charter), landing Chapter 8
(`docs/manual/08-your-own-data.md`, cataloging/checking/baselining a
corpus the reader didn't generate), the `manual-review` skill
(`.agents/skills/manual-review/SKILL.md`, chartered `.agents/rulings.md`
"From ADR-0113" R5) with its own first scored run, TWO tag ceremonies
(one repaying an S4 deviation), and the author's own chartered citation
errata sweep row. Read first: `docs/manual/00-front.md` through
`07-*.md`; `docs/formats.md`, `docs/locators.md`, the intake use-case
pages; `.agents/rulings.md` RQ3 (ADR-0115, the provenance-of-a-
real-world-act class exemption, taught at reader level in Chapter 8's
own `--received` section); `.agents/skills/` for an existing skill's
structure and the `.claude/skills/` mirror discipline; `notes/ADRs.md`
lines 1-30 (the origin-qualification citation doctrine).

### Step 0 — Preflight and double tag ceremony

`origin/main` at `a453fe1` (ADR-0124 close) at session start, matching
the driving prompt's own stated premise exactly. Last five `main` CI
runs (`gh run list --limit 5 --branch main`, checked at session start):
all `completed`/`success` — `a453fe1` (4m42s), `b340326` (4m33s),
`b6256b6` (4m43s), `da72533` (3m30s), `f9fbeca` (4m36s) — no red among
the five.

**Tag 1, repaying ADR-0124's own skipped Step 0:** channel verification
of the ADR-0124 record against the live tag list (`git tag -l`) found
no tag at `da72533` — S4's own "Tag ceremony" section records checking
CI and the lineage premise, but no tag creation follows, and the record
discloses no deviation. Since the standing tag law makes a deferred
license the deviation, this is an undisclosed one, owned to the S4
session, repaid here: `stable-20260813-invariant-fix` created ANNOTATED
at `da72533`; pushed; peeled ref confirmed
`da7253389360ed25e2077458ebd0e884e19a685b`, exact. License: case (i),
the ADR-0123 verification (channel, 2026-08-13) plus CI long since
green — re-verified directly this session: `da72533`'s own commit
message is pure ASCII, and its diff against its parent (`f9fbeca`)
touches only `.agents/`/`notes/` paths, zero `src`.

**Tag 2, this session's own predecessor point:** `stable-20260813-
manual-s4` created ANNOTATED at `a453fe1`; pushed; peeled ref confirmed
`a453fe1ff663cf5b326f656414f0d248c21b7eb5`, exact. License: case (i),
channel fresh-clone verification 2026-08-13 (lineage confirmed,
`a453fe1`'s own commit message pure ASCII, diff against its parent
`b340326` touches only `.agents/`/`notes/` paths, zero `src`), CI per
this session's own preflight above.

### Decision

#### Commit 1 (`9592554`) — chapter 8, your own data

`docs/manual/08-your-own-data.md`: `ehrt corpus intake` taught as the
front door for a corpus this workspace didn't generate — content-hash
cataloging, the `:foreign` corpus layer, and why `--received` takes a
real date (provenance about a real-world act, per RQ3's own class
exemption, taught in plain language with no ruling citation in the
prose itself); `ehrt check` taught both in its golden-equivalence mode
(witnessed against a freshly generated five-patient Synthea corpus,
`out/corpus/synthea-s1-p5`) and, by reference to `formats.md`, its
per-file assertion vocabulary; `--baseline` mode on the gates,
witnessed at exit `0` both runs over unchanged `test-fixtures/v2`; and
closing pointers into `formats.md`/`locators.md` for the data-consumer
audience segment. No new figure — nothing in this chapter's own content
earned one beyond what Chapter 6's lineage figure and Chapter 7's
verdict figure already cover. `00-front.md` updated: the eight-chapter
arc marked complete, Chapter 8's entry replaces its working-title
placeholder with firm content, and the currency contract's own
per-chapter witnessing list extends through Chapter 8, naming Chapter
8's own landing commit as the manual's own overall currency commit.

**Witnessed strips, this session.** Every strip is copied verbatim from
an already-published source (`docs/use-cases/acceptance-qa-of-vendor-
corpora.md`, `README.md`'s own Quickstart, `docs/use-cases/regression-
baselining.md`) and re-run directly against this session's own tree:

- `bin/ehrt corpus intake --path test-fixtures/v2 --label acme-delivery
  --received 2026-07-26 --out out/acceptance/intake` — 8 files
  cataloged (the six top-level fixtures plus a `simhospital/` subtree
  intake also walks), `intake-record.edn` matching the use-case page's
  own field shape exactly.
- `bin/ehrt check out/corpus/synthea-s1-p5/fhir --expected
  out/corpus/synthea-s1-p5/fhir` — regenerated the corpus fresh
  (`corpus generate synthea --seed 1 --population 5`, matching
  README's own default out-dir naming exactly) and checked it against
  itself: `{:pass 7, :rejected 0, :indeterminate 0, :no-verdict 0}`.
- `bin/ehrt gate v2 test-fixtures/v2 --report .../baseline.edn` then
  `--baseline .../baseline.edn` — both runs `{:pass 5, :rejected 0,
  :indeterminate 0, :no-verdict 0}`, exit `0` both times, `:relative`
  agreeing with `:absolute` (nothing changed between the two runs).

No divergence found anywhere.

#### Commit 2 (`39282a6`) — the manual-review skill and its first scored run

`.agents/skills/manual-review/SKILL.md` (+ `.claude/skills/` mirror,
+ per-directory `README.md` on both sides, `ehrt.docs-tooling.readme-
presence-test` GREEN): the eight-dimension rubric named by the driving
prompt, each dimension's own operational bar stated precisely enough
to score against, not merely named. `ehrt.docs-tooling.skill-mirror-
currency-test` and `ehrt.docs-tooling.index-completeness-test`: RED
before both `.claude/skills/README.md` and the `.agents/skills/
manual-review/` directory itself existed on both sides, GREEN after —
proven, not asserted.

**The skill's own first scored run**, executed this session against
the finished eight-chapter manual, landed at
`.agents/plans/2026-08-13-manual-review-1.md`: whole manual read before
any dimension was scored; every grade cites `file:line` evidence.
**Overall verdict: FAIL.** Six dimensions pass (2, no reference
duplication; 3, anchor stability — 17 anchor links checked against
every target file's real headings under the GFM slug rule, zero
broken; 6, maturity honesty; 7, currency against a freshly regenerated
`docs/cli.md`, confirmed byte-identical to the tracked copy before
checking, sampled 4 falsifiable claims, zero mismatches; 8,
diagram-source presence, 5/5 SVGs) or warn (5, running-example
continuity — `ed-tuesday` drops out of Chapters 6-8, structurally
explained: it only emits HL7v2, never FHIR). **Two fail outright**, on
real, multi-chapter, repeat-pattern evidence:

- **Dimension 1, strip executability.** Chapters 6, 7, and 2 of 3
  strips in the just-landed Chapter 8 cite a `docs/use-cases/*.md` page
  or README's own separate "What you get" fence (`README.md:84-155`,
  distinct from the one `` ```sh `` Quickstart fence
  `ehrt.docs-tooling.quickstart-fresh-test`/`bin/quickstart-demo`
  walk). Neither `ehrt.docs-tooling.usecases-test` (schema/rendering
  tests only, never executes a strip) nor
  `ehrt.docs-tooling.invocation-lint-test` (a banned-phrase lint, not
  proof of execution) closes this gap.
- **Dimension 4, glossary linkage.** Only Chapters 2 and 8 link
  `docs/glossary.md` on first use of a glossary-defined term.
  `03-a-simulated-hospital.md` uses "Pathway" (lines 63-77) and
  "script space"/"truth space" (lines 95-111) — the exact
  colliding-meaning terms `docs/glossary.md`'s own front matter calls
  "the single most common way to misread a page here" — with zero
  glossary link anywhere in the chapter.

Per the driving prompt's own gate ("a fail-grade finding STOPs for a
ruling before arc close is declared") and this skill's own review
discipline (findings are register rows, never fixes), this session
stopped here — no chapter edited, no mechanism widened — and asked the
author how to proceed. See "Deviations," below, and `.agents/
rulings.md` "From ADR-0125" for the ruling and its disposition.

#### Commit 3 (this file, close)

Registers: `.agents/plans/roadmap.md`'s "User manual design pass" entry
records S5's own landing and the review-1 verdict in full, then closes
— the manual arc is now CLOSED, all eight chapters landed, currency
commit named. Two new Next-section rows record the two fail-grade
findings as open backlog items, per the author's own ruling (close the
arc now, findings as open rows). A third new row charters the citation
errata sweep the author ruled "a, go" on. The pre-existing "Ceremony
scripts + skill absorption" row's own scheduling note updates to run
next after the sweep, rather than duplicating a second copy of the row.
A Done-section line closes the arc's own index entry.
`.agents/rulings.md` gains "From ADR-0125": both tag licenses, the S4
deviation record (owned to the S4 session, repaid here), the review-1
verdict and this session's own STOP-AND-REPORT disposition, and the
citation-sweep charter ruling quoted verbatim. `notes/ADRs.md`'s own
index gains this record's line; `notes/adr/README.md`'s own file-count
parenthetical moves from 122 (as of ADR-0124) to 123 (as of ADR-0125).

**A gate-forced companion edit, in-fence by standing rule.**
`ehrt.docs-tooling.reading-set-budget-test` went RED after the
registers above grew: `:onboarding` measured 2029 lines against its own
1995-line budget. `.agents/reading-sets.edn` re-baselines it under the
file's own standing formula (actual × 1.15, rounded up to the nearest
5) — 2029 × 1.15 = 2333.35 → 2335, budget moves 1995 → 2335 — the same
mechanical re-derivation ADR-0115's own close applied when the same set
tripped the same way. No other reading set's budget moves; none of
them carry `roadmap.md` or any other path this session's own registers
touched.

### Oracle bracket

Pre-analysis: pure identity expected — every file touched this session
is `docs/manual/*` (new/edited docs, no new assets), `.agents/skills/
manual-review/*` and its `.claude/` mirror (new skill, no `src`),
`.agents/plans/*` (new report, roadmap edits), registers, and this ADR/
session-record/prompt-archive set; nothing touches any oracle root's
own `src`.

`bin/regression-oracle a453fe1 39282a6` → **IDENTICAL: every root's
digest matches between `a453fe1` and `39282a6`**, all 35 roots. Matches
the pre-analysis exactly. Run against `39282a6` (this session's own
Commit 2, the last content-bearing commit — Commit 3 touches only
registers and this ADR/session-record/prompt-archive set, none of it
any oracle root's own `src`, so it cannot move any digest the bracket
above already covers), matching ADR-0124's own precedent for where the
bracket's own right edge lands.

### Verification

`clojure -M:poly check`: OK, before each commit. Full `make test`: run
before each push — GREEN all three times (535 assertions, 0 failures, 0
errors, `bin/verify-nist-lock` OK), after one red->green catch: Commit
3's own first `make test` run went RED on
`ehrt.docs-tooling.reading-set-budget-test` (`:onboarding` over its own
budget by 34 lines), fixed by the gate-forced `.agents/reading-sets.edn`
re-baseline above, re-run GREEN before staging. `gitleaks git --staged
-v`: clean, every commit and both tag pushes. `git diff --cached
--stat` reviewed before every commit: exactly the fenced files each
time (`.agents/reading-sets.edn` added to Commit 3's own staging as the
gate-forced companion it is, not a scope creep). `docs/cli.md`
regenerated fresh (`make cli-doc`) before dimension 7 of the review
ran, confirmed byte-identical to the tracked copy — no drift, every
currency check in the report is against a confirmed-current file.
Post-push verification: every pushed commit message diffed against its
own message file — the only delta was `git log --format=%B`'s own
trailing-blank-line formatting artifact, every time; the ASCII-only
check on each commit message and both tag messages empty every time.

### Deviations

**The S4 tag-ceremony gap** (repaid, not merely disclosed — see Step 0
above): owned to the ADR-0124 session, found by this session's own
channel verification, not this session's own deviation.

**This session's own STOP-AND-REPORT, disclosed as license, not
deviation:** the driving prompt's own fence names this exact STOP
condition explicitly ("STOP-AND-REPORT on: a fail-grade review
finding (ruling before arc close)") — stopping after Commit 2 to ask
the author how to proceed is compliance with the driving prompt, not a
departure from it. The author's own ruling and its disposition are
recorded in `.agents/rulings.md` "From ADR-0125" and executed in
Commit 3 above.

**No other premise mismatch.** Every Read-first document matched its
own characterization in the driving prompt; both tag licenses' stated
preflight conditions held exactly; every command excerpted from a
witnessed source ran exactly as written when re-run this session, with
no divergence.

### Fences

Touched: `docs/manual/08-your-own-data.md` (new); `docs/manual/
00-front.md` (arc-completion prose, Chapter 8 entry, currency contract);
`.agents/skills/manual-review/SKILL.md` + `README.md` (new) and their
`.claude/skills/manual-review/` mirror (new); `.agents/skills/README.md`
+ `.claude/skills/README.md` (index line); `.agents/plans/
2026-08-13-manual-review-1.md` (new); `.agents/plans/README.md` (index
line); `.agents/plans/roadmap.md`; `.agents/rulings.md`; `notes/ADRs.md`;
`notes/adr/README.md`; `notes/adr/0125-manual-s5-chapter8-review-
close.md` (this file); `.agents/session-records/*`; `.agents/prompts/*`;
`.agents/reading-sets.edn` (gate-forced `:onboarding` budget
re-baseline, standing formula).
ZERO `src`/`test`/`demos` touched anywhere. ZERO edits to Chapters 1-7
(the review-1 findings are register rows, not fixes, per the driving
prompt's own review discipline).

### Index line

```
- 2026-08-13 — manual-s5-chapter8-review-close — ADR-0125
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

# 2026-08-02 — Migration session 6: the use-cases split (item 14, last)

## Scope

Sixth build session of the approved migration
(`.agents/plans/2026-08-01-migration-report.md`), executing item 14 —
the only item left open after migration session 5. Split
`docs/use-cases.md` at its `components/corpus/docs/use-cases.edn`
source into a generated index (`docs/use-cases.md`, one line per case)
plus one standalone page per case (`docs/use-cases/<id>.md`), per
migration session 1's own ruling 8. Three checkpoints: **C1** —
`ehrt.docs-tooling.usecases` gains the split renderer, `make use-cases`
regenerates both outputs, the CI freshness gate covers the new
directory, content conservation proven one-to-one (`ceca0f7`). **C2** —
every repo citation of `docs/use-cases.md#<case>` repointed, the
migration report retired (header now reads FULLY EXECUTED, item 14's
own body gains a RULED paragraph), the roadmap's "Now" section empties
into a new Done entry (`4b7433f`). **C3** (this record) — session
record and prompt archive, both indexes updated same-commit.

## Red→green evidence highlights

**The renderer split (C1).** `usecases_test.clj` rewritten alongside
`usecases.clj`: renamed/added tests for `case-slug`, `case->body-md`,
`case->page-md`, `case->index-line`, `render-use-cases-index-md`,
`cases->pages`, plus two new dogfooding tests over the real committed
EDN (`every-real-cases-narrative-fields-survive-into-its-own-page-test`,
`every-real-case-is-linked-from-the-generated-index-test`). First test
run was genuinely red — two bugs the rewrite introduced, not
pre-existing: (1) `case->index-line`'s own test asserted a trailing
`\n` the function never emits (test bug); (2) the new dogfooding test's
`{:keys [... get ...]}` destructuring shadowed `clojure.core/get`,
so `(get pages id)` called a case's own `:get` string as a function —
`ClassCastException`. Both fixed (destructure `:get` under an alias,
correct the index-line test's expected string), rerun: `ehrt.docs-tooling.usecases-test`
alone — 28 tests, 207 assertions, 0 failures/errors. Full
`docs-tooling` brick: every namespace green except one pre-existing,
unrelated failure (`structure-currency-test`, see Findings). `clojure
-M:poly check`: `OK`.

**Content conservation, the C1 ruling's own red→green.** A one-time
Python script (not a permanent test — the old single-file rendering it
diffs against stops existing the moment this split lands) reconstructed
each case's pre-split section from `git show HEAD:docs/use-cases.md`,
applied the exact link-rewrite ruleset this session made (9 intra-
catalog anchor→file mappings, the `../`-depth prefix for six
reference-doc link families plus three specific paths), and diffed the
result against the newly generated per-case page. First run: 20/20
cases "mismatched" — every diff was a single leading or trailing blank
line, the script's own section-splitting artifact, not a real
difference. Normalized boundary whitespace (`.strip("\n")` both sides,
matching the ruling's own "modulo heading/index scaffolding" license),
reran: **all 20 cases conserved one-to-one.** This is the genuine
red→green the C1 ruling asked for — the first run's red was in the
checker, confirmed by inspection before assuming the generator was
wrong, then the checker was fixed and reran clean.

**C2's edits** (reading-sets comment, migration report, roadmap,
`docs/simulate-your-facility.md`'s one anchor citation): full
`docs-tooling` brick rerun, same result — everything green except the
one pre-existing `structure-currency-test` failure.

## Judgment calls and their ratification status

See the prompt archive's own Deviation record for the full list and
reasoning; summarized here:

- **`case-slug` = the case's own `:id`**, not a re-derived GitHub
  heading-slug of `:title`. Not author-ratified.
- **The `../`-depth fix for every reference-doc link inside a case's
  own text**, judged in-scope as mechanically required by the file
  move (conservation: same page reached either way) rather than a
  forbidden content edit. Not separately author-ratified.
- **The 9 intra-catalog `#anchor` cross-references inside
  `use-cases.edn` itself** were swept in C1 (load-bearing for the
  split's own correctness), not held for C2's anchor-sweep checkpoint,
  which this session read as covering *other* documents' citations.
- **`reading-sets.edn`'s `:docs` comment** updated for prose accuracy
  even though `:docs` never cited `docs/use-cases.md` as a `:paths`
  entry to begin with — the ruling was already satisfied; the edit is
  cosmetic and budget-neutral (confirmed against
  `reading-set-budget-test`'s own path-only measurement).
- **A pre-existing `structure-currency-test` failure was found, not
  fixed** — see Findings below. Disclosed rather than silently
  patched or silently ignored.

## Findings and HEAD landed

**Content conservation accounting.** 20 cases, all conserved
one-to-one. Anchor sweep: 9 distinct intra-catalog anchors (15
occurrences across `use-cases.edn`'s own `:note` fields) repointed to
sibling per-case files; 1 external citation
(`docs/simulate-your-facility.md`) repointed with its link text
upgraded from a bare filename fragment to the case's own title (the
anchor it pointed at no longer exists to click through on GitHub's
own render, so the file-only link needed real link text). Reference-doc
link depth fix: 6 filename families (`cli.md`, `formats.md`,
`operators.md`, `locators.md`, `judge-calibration.md`,
`dev/source-sink-design.md`) plus 3 specific paths (2
`EXP-*-results.md` experiment links, 1 contract-pairing test link)
gained the extra `../` their new one-directory-deeper home requires —
found by inventorying every markdown link in `use-cases.edn` before
editing, not discovered piecemeal.

**Pre-existing, unrelated finding: stray empty directory trips
`structure-currency-test`.** `bases/sim-cli/resources/sim-cli/` exists
on this `/mnt/c` clone's filesystem — zero files at any depth, not
`git`-tracked, not `git`-ignored (git simply never tracks empty
directories, tracked or not) — left behind after the sim-cli
retirement commit (`4bf9be0`, already landed, already correct in
`git`) deleted its real files but not this now-empty directory shell.
`every-real-brick-is-named-in-agents-and-architecture-test`'s own
`disk-bricks` function does a raw filesystem listing of `bases/`, so it
sees this directory and fails both its presence assertions (`bases/sim-cli`
missing from `AGENTS.md` and `architecture.md`, correctly — it isn't a
real brick). Confirmed present before this session's first edit
(`git log -1 -- bases/sim-cli` shows only the retirement commit; `git
ls-files bases/sim-cli` is empty) and untouched by anything in this
session's own diff — out of this session's fence (item 14 only). An
attempted `rm -rf` of the empty directory was blocked by the harness's
own destructive-action classifier (a reasonable default even though
this specific target held nothing); not retried, left named here for
the author or a future session's own cleanup.

**Stale prior memory, corrected.** This session ran from `/mnt/c`
(the Windows-mounted clone), found it already at `origin/main`
(`86c61eb`) at session start — no fast-forward needed. Migration
session 5's own record and this repo's persisted agent memory both
claimed `/mnt/c` was five sessions behind, at `1dd98f8`, with a
fast-forward still owed as AUTHOR ACTION. That claim no longer holds;
the fast-forward evidently happened between migration session 5's
close and this session's start, outside any recorded session. The
roadmap's Externals section item naming this fast-forward as
outstanding should be considered closed.

**HEAD landed:** the commit this record's own checkpoint produces
(`docs: session record and prompt archive -- migration session 6`),
pushed immediately after per R30. Per-checkpoint shas: C1 `ceca0f7`,
C2 `4b7433f`. HEAD at session start: `86c61eb` (already at
`origin/main`).

**Post-push message verification, both checkpoints.** C1 and C2 each
showed exactly one delta against their own message file — `git log
--format=%B -1`'s own trailing-newline artifact (one blank line), the
known formatting behavior this repo's own ceremony already treats as
non-failure. No fix-forward needed either time.

**Report retirement.** `.agents/plans/2026-08-01-migration-report.md`'s
header now reads **FULLY EXECUTED** (all fourteen Part B items done or
ruled-closed); item 14's own body gained a `RULED 2026-08-02 (migration
session 6)` paragraph and a closing `## RULED 2026-08-02 (migration
session 6)` summary block, matching the shape every prior session's own
closing annotation used. `.agents/plans/roadmap.md`'s "Now" section
holds no items (states so explicitly rather than going empty and
silent); a new "Done (this session, 2026-08-02, migration session 6)"
entry carries the full account.

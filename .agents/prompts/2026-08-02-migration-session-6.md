# 2026-08-02 — Migration session 6 prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd /mnt/c/... && ..."`, per
this repo's own WSL-only-git convention. Clone: the Windows-mounted
`/mnt/c` clone (not the WSL ext4 clone, `~/src/ehr-testing-tools`,
migration session 5 ran from) — at session start it was already at
`origin/main`, `86c61eb`, matching migration session 5's own closing
HEAD, no fast-forward needed. This corrects a stale prior-session
memory note (migration session 5's own record and this repo's own
persisted agent memory) claiming `/mnt/c` was five sessions behind at
`1dd98f8` with a fast-forward still owed as AUTHOR ACTION — it was not,
by session start; that AUTHOR ACTION item is closed, done by the author
outside any recorded session between migration session 5 and this one.

## Prompt, verbatim

> # 2026-08-02 — ehr-testing-tools: migration session 6 — the use-cases split (item 14, last)
>
> ## Context
>
> Conventions read at HEAD `86c61eb`. Final item of the approved migration
> (`.agents/plans/2026-08-01-migration-report.md`). Run under the
> **build-session** skill; the anchor sweep runs under **errata-sweep**.
> On completion the migration report's header is marked fully executed and
> the roadmap's "Now" section empties into Done.
>
> ## Author rulings
>
> - [A] **Scope** (ruling 8, 2026-08-01): split `docs/use-cases.md` at the
>   `use-cases.edn` source into an index plus per-use-case files —
>   generation, anchors, command-strip structural enforcement, and the CI
>   freshness gate all intact (review P3-1,
>   `notes/2026-07-30-refactoring-review.md` §5.2).
> - [C] **Shape**: `docs/use-cases.md` remains and becomes the generated
>   index (every inbound link to the *file* survives); per-case files
>   generate to `docs/use-cases/<slug>.md`; renderer work lives in
>   `ehrt.docs-tooling.usecases`. Index entries: one line per case plus
>   its audience tags.
> - [C] **Content conservation**: one-to-one accounting — each case's body
>   in its new file equals its old section modulo heading/index
>   scaffolding; the concatenation check is the C1 red→green.
> - [C] **Anchor sweep** (errata-sweep skill): every repo citation of
>   `docs/use-cases.md#<case>` repoints to its per-case file; gates or
>   tests that encode the single-file shape (freshness gate output set,
>   stale-path scopes, quickstart-fence extraction if it reads this file)
>   are updated in the same commit as the shape change, never after.
> - [C] **Reading sets**: if `:docs` cites use-cases, it cites the index
>   only; budget delta measured and justified per the standing mechanism.
> - [C] **Fence**: no content edits to any use case (split, don't
>   improve); no budget tightening; findings that tempt content changes go
>   to the roadmap as named-futures.
>
> ## Checkpoints
>
> - **C1** — renderer + split output + gates updated, conservation proven:
>   `feat: use-cases split -- index plus per-case files from the same EDN source, content conserved one-to-one, freshness gate covers the new set (migration item 14)`
> - **C2** — anchor sweep + reading sets + report retired + roadmap:
>   `docs: use-cases citations repointed; migration report fully executed and retired to the roadmap`
> - **C3** — ritual per build-session skill:
>   `docs: session record and prompt archive -- migration session 6`
>
> ## Close-out
>
> Per build-session. Record must include: the conservation accounting, the
> sweep hit list, the budget delta, and the report-retirement annotation.

## Deviation record

- **`case-slug` uses the case's own `:id` keyword, not a GitHub
  heading-slug of `:title`.** Not stated by the prompt either way;
  judged the more robust choice (an already-unique, already-kebab-case
  identifier the schema and its own dogfooding test already pin) over
  re-deriving a slug algorithm from title text. Not author-ratified.
- **The `../`-depth link fix for every reference-doc link inside a
  case's own `:note`/`:get` text (`cli.md`, `operators.md`,
  `locators.md`, `judge-calibration.md`, `formats.md`,
  `dev/source-sink-design.md`, two `EXP-*-results.md` links, one
  contract-pairing test link) was not named by the prompt's own anchor-
  sweep ruling, which speaks only of `docs/use-cases.md#<case>`
  citations.** Treated as in-scope and mechanically required rather
  than a content edit: every per-case page moved one directory deeper
  than `docs/use-cases.md` sat, so an unprefixed relative link that
  worked in the single-file world is a broken link in the split world
  — leaving it unrepaired would have made the split incorrect on day
  one, not merely incomplete. Judged to fall under "conservation" (a
  reader following the link reaches the same page either way) rather
  than under the fence's "no content edits" (no case's own narrative,
  strip, equations, or diagram text changed — only href mechanics). Not
  separately author-ratified; flagged here for review.
- **The 9 intra-catalog `#anchor` cross-references living inside
  `use-cases.edn`'s own `:note` fields were swept in C1, not held for
  C2's "anchor sweep."** Read C2's anchor-sweep ruling as covering
  *other* documents' citations of `docs/use-cases.md#<case>` (the one
  found: `docs/simulate-your-facility.md`); the EDN's own internal
  cross-links are load-bearing for C1's own correctness (a case's page
  would ship with a dead link to another case otherwise) and so were
  fixed as part of "renderer + split output... conservation proven,"
  not deferred. Disclosed here since the prompt's own checkpoint
  grouping could be read either way.
- **`.agents/reading-sets.edn`'s `:docs` comment (not its `:paths`)
  updated to mention the per-case pages, though `:docs` never cited
  `docs/use-cases.md` as a path to begin with** — so the "if `:docs`
  cites use-cases, it cites the index only" ruling was already
  satisfied without a path change; the comment edit is prose accuracy
  only, budget-neutral (confirmed: `reading-set-budget-test` sums
  `:paths`, never comment text), bundled into C2 as the closest-fitting
  checkpoint rather than a separate change.
- **A pre-existing, unrelated `structure-currency-test` failure found
  mid-session, not fixed.** An untracked, empty leftover directory
  (`bases/sim-cli/resources/sim-cli/`, no files, not `git`-tracked —
  debris from the already-landed sim-cli retirement, `4bf9be0`) trips
  `every-real-brick-is-named-in-agents-and-architecture-test`'s raw
  filesystem scan on this clone. Confirmed present before this
  session's own first edit and untouched by anything in this session's
  own diff; out of this prompt's fence. An attempted cleanup (`rm -rf`
  the empty directory) was blocked by the harness's own destructive-
  action classifier; not retried. Left for the author or a future
  session — see the session record.
- No other deviations. All checkpoints executed as stated; the fence
  held (no case's own narrative/strip/equations/maturity text changed;
  no reading-set budget tightened; no use-case content "improved").

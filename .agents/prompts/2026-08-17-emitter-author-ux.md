# Archived prompt: emitter-author-ux (2026-08-17)

Archived verbatim as received. Departures from it are recorded in
`notes/adr/0146-emitter-author-ux.md` (the README paragraph placed in "Where
to start" rather than "What you get", and manual ch. 3 citing its strip in
prose rather than in a citation table -- each with its reason) and in
`.agents/session-records/2026-08-17-emitter-author-ux.md`.

---

# Session prompt -- UX pass for one actor: the emitter author
# (custom emitters from the ground-truth event log) -- ADR-0146

## Context
Claude Code under R30 in ehr-testing-tools. HEAD at handoff: d62ed19
(ADR-0145). Author's ask: a developer who has a target format in
hand and wants to write their own emitter from the ground-truth event
log should find the explanation and the example EASILY. The seam is
well built (ADR-0141: `--format ground-truth`, `formats.md#the-event-
log` generated from the schema, `bin/example-custom-emitter`, all
exercised) and badly signposted -- channel probe at d62ed19, re-derive
before use: README's only route is one sentence (README.md ~:81);
`docs/use-cases.md` row ~28 is the ONE place the actor is named;
manual ch. 3 has zero mentions of the log; ch. 8 "Your own data" is
about foreign corpora coming IN; glossary has no "event log"/"ground
truth" entry and its "event stream ... A planned emitter here" line
(~:123) reads stale; `sim run --help`'s `--format ground-truth` line
does not cite the contract; `docs/dev/AUDIENCES.md` -- check whether
this actor exists there at all. Method: the UX-arc discipline (ADR-
0056..0064): a COLD WALK from every entry surface AS the actor,
findings with hops-to-seam per entry, then ruled fixes; docs-only
plus one exercised script. `docs/`-root law holds: user-facing, never
Polylith or repo history.

## Read first
1. `docs/dev/AUDIENCES.md`; `docs/use-cases/custom-emitter-from-the-
   event-log.md`; `docs/formats.md#the-event-log`; `bin/example-
   custom-emitter`, `bin/usecase-custom-emitter`; ADR-0141 (the seam's
   own record and its "one thing a real emitter must do").
2. `notes/adr/0056-*` .. `0064-ux-arc-close.md` -- the cold-walk
   method and its finding register shape; `.agents/skills/manual-
   review/SKILL.md` (review produces rows, never edits -- the walk is
   a review; fixes are a separate step).
3. `README.md`, `docs/README.md`, `docs/what-is-this.md`, manual
   00-08 TOC + ch. 3 + ch. 8, `docs/glossary.md`, `docs/use-cases.md`
   (GENERATED from `components/corpus/docs/use-cases.edn` -- edit the
   EDN, `make use-cases`), `bases/cli/src/ehrt/cli/core.clj` help
   strings for `sim run --format` (and `docs/cli.md`, generated).
4. `.agents/rulings.md` rows on docs (R-docs-root..., R-cited-implies-
   exercised, R-manual-...), `build-session/SKILL.md`.

## Author rulings, verbatim
- "We should do a UX pass just for that actor." "I want to fix the UX
  path for custom emitters ASAP." Item 3 (a second worked emitter
  targeting a plausible foreign shape): "item 3 ok."
- Standing (ADR-0145 Step 0 rows, apply here): sessions verify CI via
  `gh run view <id>` themselves; F-3 narrowed -- STOP only where two
  readings are both defensible, mechanical conflicts fix-forward with
  disclosure.
- Tags: pay `stable-20260817-roadmap-row-contract` @ e0cd075 (run
  32023934757) and `stable-20260817-rulings-standing-only` @ d62ed19
  (run 32033449792) in Step 0 on `gh run view` success each; record
  id + conclusion. If either run is not `success`, STOP for that tag
  only.

## Step 0
Fresh, tip d62ed19; both tags; baseline `make test` unpiped MAKE_EXIT
captured, reconcile vs ADR-0145's 338 runs / 3,830 blocks / 17,354;
`poly check`; reading sets vs budgets AND baselines (all must be at or
under baseline; `docs/**` files are NOT in `:onboarding` -- verify
which sets they ARE in before you grow them).

## Step 1 -- the cold walk (review; rows only, no edits)
Actor card, written first and quoted in the ADR: "I run a hospital-
adjacent system with its own message format. I want simulated
hospital traffic in MY format. I have never read this repo. I have
ten minutes." Then, for each entry surface, walk AS the actor and
record: hops to (a) knowing the log exists, (b) the contract, (c) the
worked example, (d) the schema-version promise; dead ends; stale or
misleading sentences (quote them). Entry surfaces, enumerated from
the tree (scan-root class), at least: `README.md`; `docs/README.md`;
`docs/what-is-this.md`; `docs/manual/00-front.md` TOC and ch. 3, ch.
8; `docs/use-cases.md`; `docs/glossary.md`; `bin/ehrt --help`, `bin/
ehrt sim --help`, `bin/ehrt sim run --help` (run them); `docs/cli.md`;
`docs/formats.md` top; `docs/dev/AUDIENCES.md`. Also walk the seam
itself once found: is `bin/example-custom-emitter` readable in one
screen; does the page say what a REAL emitter must do (untranslated-
event count) prominently; does anything say how to know your emitter
is complete (21 kinds; the schema file). Findings register U-1..U-n
with file:line evidence and a proposed fix each; hops table.
Open ADR-0146. Commit: "docs: ADR-0146 opens -- cold walk of every
entry surface as the emitter author; findings U-1..U-n, hops table"

## Step 2 -- ruled fixes (docs; propose, then apply)
Apply the fixes the walk proposes, expected at least (the walk decides;
list any of these you found unnecessary and why):
- `docs/dev/AUDIENCES.md`: the emitter author as a named audience with
  their entry path.
- README "What you get" (or the section the walk finds right): one
  paragraph -- the ground-truth event log exists, one command shows
  it, one link to the use case; the manual chapter link.
- Manual ch. 3: a short section "The log underneath every message" --
  `--format ground-truth`, what it is, links to `formats.md#the-event-
  log` and the use case; ch. 8's opening: one sentence disambiguating
  "your own data IN" from "your own format OUT" with the link.
- Glossary: entries "event log", "ground truth" (if absent), "emitter",
  "schema version"; the stale "planned emitter" line corrected in
  place with the standing dated-errata form the glossary uses.
- `sim run --help`: `--format ground-truth` line cites `docs/formats.
  md#the-event-log` and the use case; regenerate `docs/cli.md`.
- `use-cases.edn`: the catalog gains actor grouping or a top "start
  here by actor" table so "I have my own format" is a heading, not
  row 28 -- form per the walk's finding, `make use-cases`.
- The use-case page: whatever the walk found (completeness check,
  untranslated-count prominence, one-screen script).
Every page still under its gates: `make docsgen` no drift, dead-link
scan (both roots), `usecase-custom-emitter` exercised, manual gates,
`docs/`-root law (no Polylith/history words). Commit: "docs: the
emitter author's path -- audience named, README and manual signposts,
glossary entries, sim run --help cites the contract, use-case catalog
by actor (ADR-0146)"

## Step 3 -- item 3: a second worked emitter (red-first)
`bin/example-custom-emitter-jsonl` (or CSV census -- pick the shape the
walk suggests a real consumer most plausibly has; say why): renders
the log into a PLAUSIBLE foreign format, one mapping decision made
visibly (e.g. encounter start/end folded from admitted/discharged
into one record; a field the log has that the target lacks, dropped
and COUNTED; a field the target has that the log lacks, left null and
SAID). Depends on nothing in this repo. Reports untranslated events.
Deterministic (seed 42 fixture output committed as a fixture under
`test-fixtures/` or regenerated by the exerciser -- follow the first
example's pattern exactly). Red first: an exerciser assertion
(`bin/usecase-custom-emitter` extended, or a sibling) that the second
example runs, its output byte-matches the fixture, and its
untranslated count equals the known number for seed 42/5 patients.
Use-case page gains the second example as "a mapping with decisions",
positioned after the seam-only one. Green: exerciser + full `make
test` MAKE_EXIT=0; oracle IDENTICAL 35/35 (no src outside docs-
tooling/bases help strings -- if a help-string edit changes an oracle
digest, that is a STOP: help text should not be in the oracle; report).
Commit: "feat: second worked custom emitter -- a plausible foreign
format with visible mapping decisions, exercised (ADR-0146)"

## Step 4 -- records
ADR-0146: actor card, hops table before/after (re-walk after fixes;
the after-table is the acceptance evidence), U-rows with disposition.
Rulings rows if any standing rule emerged (e.g. "every audience in
AUDIENCES.md has a stated entry path"). Roadmap row `**[emitter-
author-ux]**` CLOSED to Done; item-3 successor if any as OPEN. Session
record; prompt archived. Commit: "docs: ADR-0146 -- emitter-author UX
pass close: hops before/after, findings dispositions"

## Fences
No `.agents/**` edits beyond roadmap row, rulings rows, prompt archive,
session record. No `src` outside `bases/cli` help strings and docs-
tooling. `docs/`-root law. Generated docs regenerated, never hand-
edited. Reading sets: no budget rise; STOP if a set would exceed.
Exit codes unpiped; `out/` cleared; anchored edits; R-RP.
STOP-AND-REPORT (F-3 narrowed) on: a finding whose fix would change
the event-log contract or its page's claims about it; the oracle
moving.

## Self-archive
`.agents/prompts/2026-08-17-emitter-author-ux.md` in Step 4.

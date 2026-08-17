## ADR-0144 — The roadmap gets a row contract: status tokens, slug anchors, a six-line cap, and the lint that holds all three (compression arc, session B)

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-17.

### Context

Session A of the register-compression arc (ADR-0143) made
`notes/ADRs.md` a GENERATED surface: its rows are derived from each
ADR's own heading and Status line, so the shape cannot decay. Session B
was ordered next, against the other register — and
`.agents/plans/roadmap.md` cannot take the same treatment. It is
hand-owned intent: what a session should do next, and why, is not
derivable from anything else in the tree. So B makes it SMALL and
LINTED instead.

The arc's own opening specimen lives here. ADR-0143 deliberately left it
unmoved so B would have a live red rather than a fixture: the
downstream-latency row, whose first sentence reads `MECHANISM LANDED
2026-08-11 (ADR-0109), DEMO LANDED 2026-08-11 (ADR-0110), arc CLOSED`,
sitting under `## Next (backlog, no session scheduled)`. ADR-0143 cited
it as `roadmap.md:222`. **By the time session B read the file it was at
line 237** — fifteen lines of drift in one session, in a citation whose
whole job is to point at a row. That is the second defect this session
gates, and it was found by the first one being cited.

Re-derived against the live tree at `deb9a33`, never recalled:

- **1,684 lines, 123 rows.** `## Next` alone is 1,110 lines across 41
  rows; `## Deferred` 454 across 23; `## Externals` 40 across 9;
  `## Done` 54 across 49; `## Now` 8 across 1.
- **59 of the 123 rows exceed six lines**, against a header law that
  says "one line per item." The longest is 118 lines. That law has sat
  in the file since 2026-08-01 and almost no row has satisfied it as
  written; a rule stated and never gated documents an intention, not a
  constraint.
- **25 rows outside `## Done` open with a closure word** in their first
  sentence (`LANDED`/`CLOSED`/`FIXED`/`DONE`/`RESOLVED`). The channel's
  own probe estimated "13-ish". The real number is 25, and that gap is
  itself the argument for gating the count rather than reading it.
- **8 live-surface line cites into the roadmap**, at least three of
  which the census confirmed already pointed at the wrong row.
- **Zero slug anchors and zero `roadmap.md#` cites** anywhere in the
  tree: there was no stable way to address a row at all.

### The ruled contract (author rulings Q1–Q5, "a. throughout")

Every top-level row, in every section:

    - <TOKEN> **[slug]** [PRIORITY n] <what remains and why> <ADR cite>

- **Q1 — token.** The first token after the bullet is exactly one of
  `OPEN`, `CLOSED <yyyy-mm-dd> <ADR-NNNN|sha>`,
  `DEFERRED (trigger: ...)`, `EXTERNAL`. **Guard #1**: a `CLOSED` row
  outside `## Done` is red. **Its dual**: a closure word in the first
  sentence of a row not tokened `CLOSED` is red.
- **Q2 — slug.** A stable `**[slug]**` right after the token, unique
  file-wide. Rows are cited `roadmap.md#slug`; `roadmap.md:NNN` in a
  live surface is red.
- **Q3 — cap.** Six lines a row, maximum.
- **Q4 — destinations.** A closed row moves VERBATIM to the attic,
  leaving one `## Done` line. A live row's overflow goes VERBATIM to
  the ADR that owns it; a row with no owning ADR overflows to the
  attic.
- **Q5 — priority.** `## Next` rows carry `PRIORITY n`, unique and
  ascending, so `head` answers "what is next".

### Census

Population taken from the tree at `deb9a33` by
`ehrt.docs-tooling.roadmap-lint-test`'s own row parser, so the census
and the gate cannot disagree about what a row is.

#### 1. Sections, before

| section | rows | row lines | longest | over six lines |
|---|---:|---:|---:|---:|
| `## Now` | 1 | 8 | 8 | 1 |
| `## Next` | 41 | 1,110 | 118 | 36 |
| `## Externals` | 9 | 40 | 7 | 2 |
| `## Deferred` | 23 | 454 | 77 | 20 |
| `## Done` | 49 | 54 | 4 | 0 |
| **total** | **123** | **1,666** | **118** | **59** |

#### 2. Per-row disposition

Emitted by `bin/roadmap-migrate-0144 --markdown` (the script lands
with this ADR's own Step 3), from the same table the migration
executes. `old`/`new` are line counts; `verbatim to` is
where the row's own text goes, WHOLE and unedited.

| # | section | old | token | slug | new | verbatim to |
|---|---|----:|---|---|----:|---|
| 1 | Now | 8 | `n/a` | section dropped | 0 | adr:0143 |
| 2 | Next | 24 | `OPEN` | `compression-arc` | 6 | adr:0143 |
| 3 | Next | 11 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 4 | Next | 16 | `OPEN` | `event-log-shape-defects` | 5 | attic |
| 5 | Next | 11 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 6 | Next | 21 | `OPEN` | `repo-review-4` | 5 | adr:0139 |
| 7 | Next | 23 | `OPEN` | `sim-theory-edn-hop` | 6 | adr:0139 |
| 8 | Next | 17 | `OPEN` | `careplan-guard-resolution` | 5 | adr:0139 |
| 9 | Next | 14 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 10 | Next | 18 | `OPEN` | `attic-rotation-law` | 6 | adr:0139 |
| 11 | Next | 65 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 12 | Next | 111 | `DEFERRED` | `downstream-latency` | 6 | adr:0142 |
| 13 | Next | 23 | `OPEN` | `demos-traces-ungated` | 6 | adr:0142 |
| 14 | Next | 118 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 15 | Next | 45 | `OPEN` | `review-3-tag-unpushed` | 6 | adr:0139 |
| 16 | Next | 43 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 17 | Next | 17 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 18 | Next | 28 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 19 | Next | 31 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 20 | Next | 39 | `OPEN` | `manual-dimension-5` | 6 | adr:0134 |
| 21 | Next | 26 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 22 | Next | 17 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 23 | Next | 20 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 24 | Next | 27 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 25 | Next | 32 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 26 | Next | 16 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 27 | Next | 18 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 28 | Next | 4 | `EXTERNAL` | `design-channel-draft-queue` | 4 | nothing (already within cap) |
| 29 | Next | 30 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 30 | Next | 14 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 31 | Next | 42 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 32 | Next | 77 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 33 | Next | 32 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 34 | Next | 35 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 35 | Next | 8 | `OPEN` | `audience-register-paring` | 4 | adr:0113 |
| 36 | Next | 7 | `OPEN` | `lookup-column-time-next` | 5 | adr:0066 |
| 37 | Next | 7 | `DEFERRED` | `wave-g-attachment` | 5 | adr:0037 |
| 38 | Next | 2 | `OPEN` | `nightly-quickstart-workflow` | 3 | nothing (already within cap) |
| 39 | Next | 1 | `OPEN` | `generator-source-split` | 2 | nothing (already within cap) |
| 40 | Next | 1 | `OPEN` | `corpus-display-placement` | 2 | nothing (already within cap) |
| 41 | Next | 1 | `OPEN` | `markdown-table-dedup` | 2 | nothing (already within cap) |
| 42 | Next | 18 | `OPEN` | `corpus-generate-engine` | 6 | adr:0136 |
| 43 | Externals | 4 | `EXTERNAL` | `ci-failure-email` | 4 | nothing (already within cap) |
| 44 | Externals | 2 | `EXTERNAL` | `nist-licensing` | 2 | nothing (already within cap) |
| 45 | Externals | 2 | `EXTERNAL` | `ig-pinning` | 2 | nothing (already within cap) |
| 46 | Externals | 6 | `EXTERNAL` | `clojars-publish` | 5 | nothing (already within cap) |
| 47 | Externals | 5 | `EXTERNAL` | `setup-rewalk` | 4 | nothing (already within cap) |
| 48 | Externals | 7 | `EXTERNAL` | `guide-ch24-notes` | 4 | adr:0112 |
| 49 | Externals | 2 | `EXTERNAL` | `upstream-adaptation-skill` | 3 | nothing (already within cap) |
| 50 | Externals | 5 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 51 | Externals | 7 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 52 | Deferred | 25 | `DEFERRED` | `transport-batching-deferrals` | 6 | adr:0111 |
| 53 | Deferred | 13 | `DEFERRED` | `sink-composability-flake` | 5 | adr:0107 |
| 54 | Deferred | 19 | `DEFERRED` | `veteran-hyperlipidemia` | 6 | adr:0090 |
| 55 | Deferred | 24 | `DEFERRED` | `veteran-mdd-max-steps` | 5 | adr:0090 |
| 56 | Deferred | 77 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 57 | Deferred | 48 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 58 | Deferred | 33 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 59 | Deferred | 17 | `DEFERRED` | `carry-across-emission` | 5 | adr:0086 |
| 60 | Deferred | 11 | `DEFERRED` | `wellness-chronic-meds-cap` | 5 | adr:0037 |
| 61 | Deferred | 6 | `DEFERRED` | `backload-named-future` | 4 | nothing (already within cap) |
| 62 | Deferred | 1 | `DEFERRED` | `intake-staging-dir` | 4 | nothing (already within cap) |
| 63 | Deferred | 1 | `DEFERRED` | `verdict-cache-placement` | 2 | nothing (already within cap) |
| 64 | Deferred | 11 | `DEFERRED` | `imagingstudy-stroke-risk` | 5 | adr:0031 |
| 65 | Deferred | 32 | `DEFERRED` | `census-tool-refinements` | 6 | adr:0071 |
| 66 | Deferred | 10 | `DEFERRED` | `uti-o2-distribution` | 6 | adr:0035 |
| 67 | Deferred | 20 | `DEFERRED` | `vital-sign-channel` | 6 | adr:0069 |
| 68 | Deferred | 17 | `DEFERRED` | `lookup-column-time-open` | 6 | adr:0039 |
| 69 | Deferred | 12 | `DEFERRED` | `wellness-encounters` | 5 | adr:0092 |
| 70 | Deferred | 13 | `DEFERRED` | `notice-verbatim-coverage` | 5 | adr:0092 |
| 71 | Deferred | 12 | `DEFERRED` | `wave-e-parked` | 6 | adr:0092 |
| 72 | Deferred | 19 | `CLOSED` | -> `## Done` pointer | 0 | attic |
| 73 | Deferred | 19 | `DEFERRED` | `synthea-demographics` | 6 | adr:0136 |
| 74 | Deferred | 14 | `DEFERRED` | `mutate-loopback-flake` | 6 | adr:0136 |

The 49 `## Done` pointers are not tabled individually: their transform
is mechanical and total — `- DATE — slug — ADR-NNNN` becomes
`- CLOSED DATE ADR-NNNN **[slug]**`, the Done pointer's existing shape
turning out to already BE the token grammar with its fields reordered.
Two irregular pointers (`d8-5-fence-battery`, which cites a register
file and no ADR, and `event-log-contract`, which carries a
why-this-was-added-late parenthetical) are retokened by hand in the
script, the first using the token's own `<sha>` alternative.

#### 3. `## Now` is dropped, and why

The ruling offered "one row, `OPEN` `**[now]**`, or drop the section if
empty is the norm (census decides)". The census decides drop:

- **39 of the last 40 roadmap revisions** read "Nothing in progress" as
  the section's entire content.
- The one revision that did not was **stale for 32 consecutive
  revisions**: the paragraph named ADR-0115 (review-3 rulings landing,
  2026-08-12) continuously through 2026-08-16 — five days and roughly
  fifteen ADRs — until ADR-0143 refreshed it.
- **Nothing anywhere in the tree references the section.** `git grep`
  over every doc, skill and test finds no reader.

So the section is empty whenever it is honest and stale whenever it is
not, and it has no consumer. Work genuinely in flight is uncommitted
work; the register records what landed. Its one row's text moves
verbatim to ADR-0143, which is what it was about.

#### 4. Line-cite inventory

Every `roadmap.md:NNN` in the tree, resolved BY CONTENT (half of them
no longer resolve by number), with its disposition:

| cite | meant | live surface? | disposition |
|---|---|---|---|
| `.agents/plans/roadmap.md:14` | the latency row | yes | → `roadmap.md#downstream-latency` |
| `.agents/plans/roadmap.md:33` | the latency row | yes | → `roadmap.md#downstream-latency` |
| `.agents/skills/handoff/SKILL.md:81` | the latency row | yes | → `roadmap.md#downstream-latency` |
| `.agents/skills/session-prompt/SKILL.md:106` | the latency row | yes | → `roadmap.md#downstream-latency` |
| `.agents/skills/session-prompt/SKILL.md:113` | the latency row | yes | → `roadmap.md#downstream-latency` |
| `.claude/skills/handoff/SKILL.md:81` | mirror of the above | yes | → rewritten, mirror held byte-equal |
| `.claude/skills/session-prompt/SKILL.md:106` | mirror | yes | → rewritten, mirror held byte-equal |
| `.claude/skills/session-prompt/SKILL.md:113` | mirror | yes | → rewritten, mirror held byte-equal |
| `notes/adr/0143-adr-index-generated.md:10` | the latency row | ADR | → `roadmap.md#downstream-latency` (sanctioned in-place edit) |
| `notes/adr/0143-adr-index-generated.md:40` | the latency row | ADR | → `roadmap.md#downstream-latency` (sanctioned in-place edit) |
| `notes/adr/0119-user-manual-skeleton.md:116` | `roadmap.md:123,168`, a 2026-08-12 term census | ADR | **left verbatim** — see F-3 |
| `notes/adr/0119-user-manual-skeleton.md:119` | `roadmap.md:144,166,183`, same census | ADR | **left verbatim** — see F-3 |
| `notes/adr/0143-adr-index-generated.md:182` | `notes/sim/agents/plans/roadmap.md:43,45,...` | frozen archive | **not this file** — see F-4 |
| `.agents/plans/2026-08-16-fence-battery-findings.md:365` | the repo-review-4 row | dated record | left verbatim — history at authoring time |
| `.agents/prompts/2026-08-16-compression-a-adr-index.md:10,150` | the latency row | archived prompt | left verbatim — history at authoring time |
| `.agents/prompts/2026-08-16-d8-5-fence-battery.md:101` | the repo-review-4 row | archived prompt | left verbatim — history at authoring time |
| `.agents/session-records/2026-08-16-compression-a-adr-index.md:108` | the latency row | session record | left verbatim — history at authoring time |
| `.agents/session-records/2026-08-16-d8-5-fence-battery.md:146` | the repo-review-4 row | session record | left verbatim — history at authoring time |

**Inbound anchors:** `git grep "roadmap\.md#"` over the whole tracked
tree returns **zero hits** before this session. There is no anchor the
new scheme must preserve, and the slug table below is free to be
authored rather than reverse-engineered.

### Findings

**F-1 — the prompt's attic path does not exist.** The session prompt
names `.agents/plans/attic/README.md` and
`attic/roadmap-done-2026-08.md`. There is no `attic/` directory; the
attic files are flat, `.agents/plans/roadmap-done-2026-07.md` and
`-2026-08.md`, and "attic" is the role the roadmap's own `## Done`
header gives them, not a path. Adapted to the real paths; the
destination was never ambiguous.

**F-2 — the cite lint's scan roots are an include-list, not
`.agents/**` + `notes/**`, and this carries ZERO exemptions.** Q2
named those two globs. The census found both contain standing FROZEN
populations a cite lint must not reach: `notes/prompts/` (pinned by
`ehrt.docs-tooling.notes-prompts-frozen-test`), `notes/sim/`
("provenance, untouched by law", ADR-0143), and every dated one-shot
file under `.agents/prompts/`, `.agents/session-records/` and
`.agents/plans/`. A literal reading would have required at least three
dated exemptions, and the fence STOPs at more than one.

The boundary used instead is not invented here. It is the same
live-surface include-list `ehrt.docs-tooling.stale-path-test` has drawn
since 2026-08-05 (register row S7, ADR-0050) for exactly this reason —
"perpetually-live indexes/plans, edited in place session after session,
never frozen at authoring time" — extended to the skills. So the gate
carries **no dated exemption at all**, which the prompt named as the
better outcome. A `roadmap.md:222` inside a dated session record is a
true statement about that day's file; a `roadmap.md:222` in a
perpetually-live skill is a pointer that has already rotted.

**F-3 — two ADR-0119 cites are left verbatim, and this is a judgement,
not an oversight.** `notes/adr/0119-*.md` cites `roadmap.md:123,168`
and `roadmap.md:144,166,183` as *evidence for a 2026-08-12 census of
which term the register used live*. They are measurements of a file
state that no longer exists, not pointers a reader should follow, and
no slug can carry that meaning. Rewriting them would falsify the
census. They sit outside the lint's population by F-2's boundary, so
nothing is exempted to leave them; disclosed here because "every cite
rewritten" would otherwise be read as covering them.

**F-4 — one apparent cite is to a different, frozen file.**
`notes/adr/0143-*.md:182` reads
`notes/sim/agents/plans/roadmap.md:43,45,434,1036`. A naive
`roadmap\.md:\d` regex matches it; it addresses the frozen `notes/sim/`
archive, not this register. Excluded structurally (the lint's
population never contains that path), not by exemption.

**F-5 — a row was carried OPEN whose work had landed.** The
`Demo exerciser (clinic-decade)` row's own dated correction ends "This
row stays OPEN, now blocked on the first of those two rows." Both
blocking rows closed (ADR-0131, ADR-0132), and ADR-0132's own text says
the exerciser "landed completed". `bin/demo-exerciser-clinic-decade`
exists in the tree. The row was closed by work its own successor rows
recorded, and nothing propagated the closure back. Tokened `CLOSED
2026-08-14 ADR-0132`.

**F-6 — a Deferred row owes a revisit trigger and has none.** `P2-5
intake staging-dir behavior (deferred 2026-07-31)` is one line, cites
no ADR, and states no trigger, against the section's own contract. The
token grammar makes this un-writable-around, so the row now says so:
`DEFERRED (trigger: none recorded -- ADR-0144 finding F-6)`. The
author's ruling is owed, not guessed.

**F-7 — six `## Done` pointers were missing.** ADR-0126, ADR-0127,
ADR-0128, ADR-0129, ADR-0132 and ADR-0133 all closed rows that were
left sitting in `## Next` as closed prose instead of being relocated;
no Done pointer was ever written for any of them. Q4's "leave one Done
line each" repairs all six. Three further closures (ADR-0047,
ADR-0083, ADR-0086) belong to arcs already rotated to the attic and
already carry their pointers there; they get no live Done line, which
would violate the current-arc-only law.

**F-8 — five rows resolve to a token that suggests a different
section, and this session does NOT move them.** Q1 constrains exactly
one placement — `CLOSED` belongs under `## Done` — and re-triaging a
row between `## Next`, `## Deferred` and `## Externals` is author
judgement, not mechanical re-sectioning. Left in place, tokened
honestly, listed here for the author:

| row | section | token | would suggest |
|---|---|---|---|
| `downstream-latency` | Next | `DEFERRED` | `## Deferred` |
| `wave-g-attachment` | Next | `DEFERRED` | `## Deferred` |
| `design-channel-draft-queue` | Next | `EXTERNAL` | `## Externals` |
| `lookup-column-time-next` | Next | `OPEN` | possibly merge with `lookup-column-time-open` |
| `repo-review-4` | Next | `OPEN` | ordering vs. `PRIORITY` — see F-9 |

**F-9 — only two `PRIORITY` values are author-ruled; the rest carry the
file's own order, disclosed.** Q5 sources `n` from "the ruled queue
order, from the ADR-0141 handoff as recorded in the compression charter
row". That row records exactly one ordering — the arc's own `A → B → C
→ D` — and ADR-0139 Q3 "a." fixes repo review 4 at ~ADR-0154. No ruling
orders the other seventeen `## Next` rows against each other. Rather
than invent a queue, `PRIORITY` numbers the rows in their existing
top-to-bottom file order, which is the only ordering the register
actually recorded. `PRIORITY 1` (the compression arc, whose session C
is genuinely next) is ruled; the rest is carried, not claimed.

**F-10 — a closure that leaves a live remainder is tokened by the
remainder.** Three rows say CLOSED and are not finished: repo review 3
(arc closed, its tag unpushed), manual-review run 2 (passed, dimension
5 still WARN), and the latency arc (closed, four named deferrals
standing). Splitting each into a closed row plus a new open row is
outside the fence, so each keeps ONE row, retitled and retokened by
what survives, with the closure narrative moved verbatim to its owning
ADR and a Done pointer left where one was owed. Stated as a rule
because it decided three rows the same way.

**F-11 — guard #1's population is created by the migration, not found
by it.** Before tokens exist, no row is tokened `CLOSED`, so guard #1
("a `CLOSED` row outside `## Done`") matched **zero rows** at the red
capture while every other assertion fired. That is not a weak gate: it
is a gate whose subject the same commit introduces. Its dual — closure
words in a non-`CLOSED` row's first sentence — is what carries the
pre-migration population, and it fired on 25.

## ADR-0143 — The ADR index becomes generated: guard #2 (structure by generation), guard #3 (budget ratchet), and the skills rider (compression arc, session A)

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-16.

### Context

The author ordered a four-session register-compression arc (A → B → C →
D, one session each) after a queue item was carried as open five days
after its own arc had closed: `.agents/plans/roadmap.md:222` reads
`**Downstream-latency realism -- MECHANISM LANDED 2026-08-11
(ADR-0109), DEMO LANDED 2026-08-11 (ADR-0110), arc CLOSED;
OBR-7/OBX-14 increment LANDED 2026-08-16 (ADR-0142).**` and sits in the
`## Next (backlog, no session scheduled)` section — a row whose own
words say CLOSED, filed under the heading that means not-yet-started.

That is not a one-off. The scaffolding-compaction arc of 2026-08-05
(ADR-0045/0046/0047) already fixed this register family once, and each
of its guards was written to the one specimen in front of it, then
outgrown by ordinary growth. Verified against the live tree at
`dc13a17`, not recalled:

- **The index regrew.** ADR-0046's own AR-B-1 made `notes/ADRs.md` an
  index whose every line is "number, title, file link, and the entry's
  own status/arc" — one line per ADR. Rows 0043–0048, written that day,
  still are exactly that. Today the file is 140,852 bytes over 190
  lines, 140 rows averaging **977 characters**; ADR-0142's own row is
  8,447 characters. Nothing enforced the shape, so the shape decayed
  one session at a time, each session's row a little longer than the
  last and each one locally defensible.
- **The budget was bumped in place fourteen times, then re-baselined,
  then bumped eleven times more.** `.agents/reading-sets.edn`'s own
  header says so: a 2026-08-05 note that "supersedes all fourteen" bump
  comments, followed by eleven further dated re-derivations through
  2026-08-16. The gate is a ceiling that moves whenever it is hit, so
  it records growth rather than resisting it.
- **The roadmap closure lint covers `## Deferred` only.**
  `ehrt.docs-tooling.roadmap-deferred-closure-lint-test` is scoped to
  the Deferred section; a CLOSED row parked in `## Next` — exactly the
  `roadmap.md:222` specimen — is outside its population. (Guard #1 and
  its dual are chartered to session B, whose red set is that row's own
  population; this session does not touch them.)

Session A lands the first two guards and the skills rider:

- **Guard #2 — structure, achieved by generation.** `notes/ADRs.md`
  stops being hand-edited and becomes a generated surface like
  `docs/cli.md`: `make docsgen` renders it from the `notes/adr/` tree's
  own headings and Status lines, and CI's freshness step diffs it. A
  row cannot regrow, because no one writes a row.
- **Guard #3 — the budget ratchet.** `.agents/reading-sets.edn`'s
  `:budget-lines` may no longer EXCEED a value committed in
  `.agents/reading-sets-baseline.edn`, so a session that hits a ceiling
  must compact or stop; only a compaction ADR moves the baseline.
- **The skills rider (guards #4 and #5)** — queue provenance in
  `handoff`/`session-prompt`, and register hygiene / budget-stop / red-
  push / anchored-edit rules in `build-session`.

### Author rulings, verbatim

- **Compression order:** *"I like that order, after OBR/OBX."* — the
  A → B → C → D sequence, opened by this session.
- **Guards:** *"Ok on all five. Rider ok."*
- **Q1 (index shape): (a)** — generated from ADR headings + Status by
  docsgen, CI freshness-gated.
- **Q2 (narratives): (a)** — each row's narrative moved verbatim into
  its own ADR file, under a dated heading.

### Step 1 census

Measured at `dc13a17`, working tree clean.

#### 1. The `notes/adr/` tree (140 files)

| Check | Result |
|---|---|
| `## ADR-NNNN — Title` heading present | **140 / 140** |
| Heading number equals filename number | **140 / 140**, zero mismatches |
| Heading separator | **em dash (`—`) in all 140** — not `--` |
| `**Status:**` line within 7 lines of the heading | **138 / 140** |
| First status word (cut at the first `,` `(` `.` `;` `:` `—` or ` -- `) | **`Accepted`, all 138** — no `Superseded`, no other value |

The **two exceptions**, found by scanning rather than taken on faith,
are `notes/adr/0021-bases-sim-cli-projects-sim-retired.md` and
`notes/adr/0022-sim-adopts-ehrt-kernel-result.md`: both go straight
from their heading to `### Context`. Both are ADR-0046-era split files
whose pre-split inline entries never carried a Status line; the index
has recorded both as `Accepted` since the split. Resolution (per this
session's own charter): the line is **added**, sourced from the index —
`**Status:** Accepted (status line added ADR-0143, 2026-08-16, from
`notes/ADRs.md`'s own index row).` Two is at the STOP-AND-REPORT
threshold, not over it (`more than 2` is the trip), so the session
proceeds.

#### 2. `notes/ADRs.md`: every non-row line has a destination

190 lines, 140,852 bytes. 140 of those lines are ADR rows; **40 are
non-row, non-blank** (all in lines 1–46); 10 are blank.

| Lines | What | Destination |
|---|---|---|
| 1 | `# Architecture Decision Records — ehr-testing (workspace)` | generated preamble, verbatim |
| 3–6 | the append-only / never-silently-revert paragraph | generated preamble, verbatim |
| 8–11 | the legacy-ADR provenance paragraph (`sim/ADR-0008`, `tools/ADR-0017`) | generated preamble, verbatim |
| 13–29 | **the citation rule** (added 2026-07-30) and the ADR-0012 collision it exists for | generated preamble, verbatim |
| 31 | `---` | generated preamble, verbatim |
| 33 | `## Index` | generated preamble, verbatim |
| 35–46 | the "this file became an index on 2026-08-05" paragraph | generated preamble, verbatim, **plus a dated amendment** (see Finding 3) |
| 2, 7, 12, 30, 32, 34, 47 | structural blank lines between the blocks above | preserved by the renderer |
| 186, 188 | two stray blank lines **between rows** (after the ADR-0139 and ADR-0140 rows) | normalized away — they carry no text, and the generated row block is uniform |
| 190 | trailing newline | preserved |

Nothing in the file is deleted: every non-blank line above is
reproduced by the renderer verbatim.

**Preamble ownership, and why (the charter left this to the session).**
The preamble becomes **string literals in the renderer**, not a
hand-owned `notes/adr/INDEX-PREAMBLE.md` the generator inlines. Three
reasons: (i) `docsgen.clj`'s own docstring already states the DOC-3
doctrine for exactly this case — *"The output is WHOLLY generated
(author ruling, DOC-3) ... There is no hand-edited region, which is
what lets CI's generated-doc freshness step regenerate and `git diff
--exit-code` it"*, with *"Preamble prose therefore lives as string
literals below"* — and this session's charter is to make the index a
generated surface **like `cli.md`**, not a spliced one like
`docs/formats.md`'s marker-delimited event-log section; (ii) a new
hand-owned file under `notes/adr/` would be a fresh doc-tree surface
carrying its own README-index and stale-path obligations, to hold text
that changes roughly once a year; (iii) one place answers "where does
this sentence come from" — the banner names the renderer.

#### 3. Row-by-row: number, title, status, and agreement with the ADR file

All 140 rows parse cleanly against
`^- \*\*ADR-(\d{4})\*\* — (.*?) — \[`(...)`\]\(adr/(...)\) — (.*)$`.
Every row's link text equals its link target, every target exists on
disk, and every target's number equals the row's number — **zero
link-integrity findings**.

- **Status:** all 140 rows say `Accepted`; all 138 ADR files with a
  Status line reduce to `Accepted`. **Zero status disagreements.** The
  generated Status column will be byte-identical to today's for every
  row.
- **Title:** **62 rows** carry exactly the ADR file's own heading title
  — the ADR-0046 one-line form, still intact (rows 0001–0042 largely,
  and 0043–0048 explicitly). **78 rows** do not; those 78 carry
  **117,079 characters** of narrative in the title column — 83% of the
  whole file.
- Of those 78, **67** are the file's own title followed by narrative (a
  clean prefix). The remaining **11 are genuine title disagreements**:
  the row's title text and the ADR file's own heading title diverge
  before the narrative begins.

**The 11 disagreements** (Finding 1 below; resolved, not silently
reconciled):

| ADR | The ADR file's own heading title | How the index row's title differs |
|---|---|---|
| 0070 | `Vendoring batch 1: the everyday ambulatory load — five ailments join the mix, one deferred` | row names the five ailments parenthetically instead of "one deferred" |
| 0071 | `Vendoring batch 2: the chronic clinic tail — seven ailments join the mix, one deferred` | same shape: the seven named inline |
| 0072 | `Vendoring batch 3: the families that travel — and the verbatim law gets teeth` | row punctuates as `travel, and ... teeth —` |
| 0076 | `Quality riders: the review arc opens — the skill lands, the flake gets its fix, preflight widens its gaze` | row expands "the skill" to "the `repo-review` skill ... (its own already-landed commit found off-ceremony and CI-red, fixed forward)" |
| 0077 | `Repo review 1: the first assessment — every lens, nothing moved` | row substitutes the survey's own numbers for "every lens, nothing moved" |
| 0098 | `Permission-denied gate legs categorized across the judge family; bare-level unknown flags routed (D8-4)` | row drops the trailing `(D8-4)` and continues into narrative |
| 0102 | `User-path ADR citations go marker-only, full user path, gate hardened; :mllp sink abandoned` | row drops the word `ADR` from "User-path ADR citations" |
| 0126 | `Manual-arc tag payment, glossary linkage (dimension 4 fix), citation errata sweep` | row drops the `(dimension 4 fix)` parenthetical |
| 0133 | `Exact-name state resolution: collision fix, vendoring-rider row (Steps 1-4, closed)` | row writes `vendoring-rider row CLOSED` instead of the parenthetical |
| 0136 | `Register every string-diagram derivation: the make graph and CI freshness gate get the tree's population, not their own` | row stops at `population` and continues into narrative |
| 0141 | `The ground-truth event log becomes a contract: census, Event schema, generated formats.md section, custom-emitter use case` | row substitutes a different subtitle entirely |

#### 4. Inbound anchors into `notes/ADRs.md`

`git grep -n "ADRs\.md#"` over the whole tracked tree returns **four
hits, all four in the frozen `notes/sim/` archive**
(`notes/sim/agents/plans/roadmap.md:43,45,434,1036`, forms
`(../../notes/ADRs.md#adr-0010)` etc.). All four are frozen-era: their
relative path resolves to `notes/sim/notes/ADRs.md`, which has never
existed in this workspace, and their `#adr-NNNN` anchors address
per-ADR headings this file has not carried since the 2026-08-05 split.
They are provenance, untouched by law.

Every **live** inbound reference is to the file, without an anchor —
the marker-only citation form ADR-0102 established
(`[^adr-0009]: Design record [ADR-0009](../notes/ADRs.md).`) across
`docs/glossary.md`, `docs/formats.md`, `docs/locators.md`,
`docs/site-profiles.md`, `docs/judge-calibration.md`, and
`docs/use-cases/*.md`, plus `docs/glossary.md:66`'s prose link. **There
is no anchor the generated file must preserve**, and no
STOP-AND-REPORT.

Two machine consumers read the file's row shape, both enumerated:

- `ehrt.docs-tooling.done-pointer-adr-test/indexed-adr-numbers` matches
  `(?m)^- \*\*(ADR-\d{4})\*\*` — the row prefix is load-bearing for the
  Done-pointer gate.
- `ehrt.docs-tooling.stale-path-test` explicitly **excludes**
  `notes/ADRs.md` from its denylist scan ("narrate history and
  legitimately cite the old names"), and its dead-markdown-link scan
  covers `docs/**` and `components/*/docs/**` only — `notes/adr/` is
  outside both populations.

#### 5. Reading sets: measured actual vs budget, today

| Set | Paths | Actual | Budget | Headroom |
|---|---|---|---|---|
| `:onboarding` | 8 | 2763 | 3105 | 342 |
| `:corpus` | 7 | 1901 | 2060 | 159 |
| `:sim` | 6 | 1347 | 1495 | 148 |
| `:judge` | 8 | 995 | 1055 | 60 |
| `:docs` | 5 | 808 | 840 | 32 |

**No set is over its budget** — the STOP-AND-REPORT condition for a
measured actual above budget before touching anything does not fire.

**`notes/ADRs.md` is not a `:paths` member of any set** (the charter
asked the census to settle this): the file's 134 KB has never been
counted against any reading budget, so compressing it does **not** drop
any actual. The two guards are independent — guard #2 fixes a register
nobody's budget was measuring, guard #3 fixes the ceiling that keeps
moving.

### Findings

**Finding 1 — 11 index rows disagree with their own ADR's title.**
Resolved from each ADR's own text, per the charter: **the ADR file's
`## ADR-NNNN — Title` heading is authoritative** (it is the record's
own title, in the record itself), and the index row's divergent text is
not discarded — the whole of each row's title-column text moves
verbatim into that ADR's own file under
`### Index summary (moved verbatim from notes/ADRs.md by ADR-0143,
2026-08-16)`. Nothing needed escalating: no disagreement was
unresolvable from the ADR's own text.

**Finding 2 — 2 ADR files carry no Status line.** ADR-0021 and
ADR-0022, both from the ADR-0046 split. Both get one added, marked as
added and sourced.

**Finding 3 — generating the index re-sequences it.** The current index
is in the pre-split entry order — non-sequential (`ADR-0013`, then
`0022`, `0021`, `0018`, `0017`, `0016`, `0014`, `0015`, `0023` …) — and
the preamble's own line 43 states that order is "unchanged, not
renumbered and not re-sequenced". A generator derived from the tree can
only order by ADR number; preserving the historical order would require
a hand-maintained order list, which is the hand-maintained data this
session is retiring. **The index is re-sequenced to ascending ADR
number**, and the preamble sentence is **amended in place with a date**
(not deleted — the ADR-0142 practice), stating that the 2026-08-05
order held until 2026-08-16 and why it moved. Numbering itself is
untouched: no ADR is renumbered, and the append-only rule stands.

**Finding 4 — the row format is kept, deviating from the charter's
sketch.** The charter sketched `- [ADR-NNNN](adr/file.md) -- Title --
Status`. The generator instead emits today's exact shape,
`- **ADR-NNNN** — Title — [`file`](adr/file.md) — Status`, because:
(i) `done-pointer-adr-test` parses `^- \*\*ADR-\d{4}\*\*`, a gate this
session has no reason to disturb; (ii) it is ADR-0046 AR-B-1's own
specified shape; and (iii) it makes **62 rows regenerate byte-identical
to today's**, so the regeneration diff is exactly the compression and
nothing else — a much stronger proof than a diff in which every row
changed.

### Fence

- src: `components/docs-tooling` only. No `sim-*`, `corpus-*`,
  emitters, oracle. The regression-oracle bracket must be IDENTICAL
  across all roots.
- ADR files are **append-only** this session: the moved summary section
  and, for two files, a Status line. No existing sentence is changed.
- Guard #1 (a CLOSED row may not sit under `## Next`) and its dual are
  **session B's**, chartered here, not built here.
- Sessions C and D of the compression arc are chartered, not executed.

### The retired note (moved verbatim from `.agents/reading-sets.edn`, 2026-08-16)

Guard #3 replaces the 2026-08-05 budget re-baseline note that stood at
the head of `.agents/reading-sets.edn`. It is retired here VERBATIM
rather than deleted, in the same commit that removes it, so the
pointer left in its place resolves:

```
;; Budget re-baseline (2026-08-05, docs coherence pass, ADR-0043 AR-D-3).
;; Between migration session 4's zero-headroom seed and this date, every
;; set's budget was bumped in place, session by session, fourteen times
;; total, each bump a dated comment recording exactly what grew and by how
;; many lines -- an honest but increasingly illegible record, since a zero-
;; headroom budget makes ROUTINE growth (AGENTS.md's Components list,
;; roadmap.md's own Now/Done churn) indistinguishable from the unplanned
;; growth the budget test exists to catch. This note supersedes all
;; fourteen: every set's budget below is its own actual line count, measured
;; fresh against the tip this note lands on, times 1.15, rounded up to the
;; nearest 5 -- one formula, applied uniformly, replacing the accumulated
;; per-session bump trail. `:paths` membership is UNCHANGED by this note;
;; only `:budget-lines` values move and the bump-history comments retire.
;; Future growth resumes the same discipline this file always used: a
;; session whose own edit pushes a set over its budget bumps that set's
;; number, with a dated comment recording what grew -- this note is a
;; re-baseline, not a suspension of the practice it replaces.
```

Its closing sentence is the one the ratchet revokes. The formula it
defined is kept, and is what a compaction ADR uses to set a new
baseline. The eleven dated re-derivations that followed it, 2026-08-05
through 2026-08-16, stay in the file as per-session provenance under a
note saying they are history, not instructions.

<!-- Steps 2-4 are recorded below as they land. -->

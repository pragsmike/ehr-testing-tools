## ADR-0145 — The rulings register becomes standing-rules-only: one row per rule, every block moved into its own ADR, the build-session skill split from its history (compression arc, session C)

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-17.

### Context

Session A of the register-compression arc (ADR-0143) made `notes/ADRs.md`
generated. Session B (ADR-0144) gave `.agents/plans/roadmap.md` a row contract
and the lint that holds it, taking that file from 1,684 lines to 290. Both
sessions ended by naming the same unfinished thing: ADR-0143's own Finding 6,
that the growth which moves four reading sets at once lives in the paths every
set shares, and that compacting those was chartered to session C.

C's three targets, re-derived against the live tree at `e0cd075`:

- **`.agents/rulings.md`, 1,757 lines, 55 `## From ...` blocks, 181 bullets.**
  It is a `:paths` member of no reading set, so none of that has ever been
  counted against any budget. Its own header, written at ADR-0047, says the file
  holds "only the STANDING `[A]` rulings" and "is NOT a history". Every block
  appended since roughly ADR-0057 is a history: execution records, tag payments,
  disclosed deviations, per-session scope decisions. The file states a contract
  it has not kept for eighty-odd ADRs, and nothing ever checked.
- **`.agents/skills/build-session/SKILL.md`, 309 lines, 17 steps.** It is in
  ALL FIVE reading sets — the only path that is — and it has grown 162 → 309
  lines across this workspace's life. Its Procedure section is 184 lines for 17
  imperatives, because each step carries the incident that produced it.
- **`.agents/reading-sets.edn`, 531 lines, of which 480 are comment**: a
  415-line header (a seed note, a composition principle, the ratchet, and
  nineteen dated per-session budget re-derivations) plus 65 lines of per-set
  rationale inside the map itself. 48 lines are data.

The register and the skill are the same defect in two shapes: a surface whose
job is to say what to do, carrying instead the story of how it came to say it.

### Step 0

Fresh clone at `e0cd075`, tree clean, HEAD matching `origin/main`.
`bin/preflight` reported one PENDING CI run (this session's own predecessor) and
that HEAD carried no `stable-*` tag; both disclosed below. `make test` unpiped,
`MAKE_EXIT=0`, **336 project runs / 17,278 assertions** — reconciling exactly
with ADR-0144's own recorded 336 / 17,278. `clojure -M:poly check` OK.

#### The tag licence: HELD, and why

The prompt licensed `stable-20260817-roadmap-row-contract` at `e0cd075`, case
(i), to be paid **"ONLY on relayed CI success for run 32023934757; else STOP
with the run id."** No relay reached this session's prompt context. The tag is
therefore **NOT PAID**, and the run id is reported: **32023934757**.

Disclosed, because the session does know more than that. `bin/preflight` found
the run `in_progress` at Step 0, and a later `gh run view` in the same session
reported `conclusion: success`. That is exactly the substitution the licence
forbids: `rulings.md#R-unrelayed-tag-condition-stops` exists because ADR-0134's
R0 hit this same shape, stopped, and was ruled "Pay it, message verbatim" by the
author rather than by the session. This session takes the same course — it
reports rather than deciding for itself — and notes that ADR-0143's conditional
tag was a *state* condition (was the run green yet) which the session could
satisfy by re-checking, whereas this one is a *relay* condition, which it
cannot. Deferring a licensed tag is the deviation (`rulings.md#R-tag-law`); the
disclosed reason is that the licence's own condition was never met.

#### Owed items from session B

1. **`intake-staging-dir`'s revisit trigger — still owed.** The prompt's own
   slot for it was left blank. The row keeps ADR-0144 F-6's honest text,
   `DEFERRED (trigger: none recorded -- ADR-0144 finding F-6)`.
2. **The five mis-sectioned rows — left in place, ruled, listed again here.**
   Re-triage between `## Next`, `## Deferred` and `## Externals` is author
   judgement (`rulings.md#R-section-retriage-is-author-judgement`).

   | row | section | token | would suggest |
   |---|---|---|---|
   | `downstream-latency` | Next | `DEFERRED` | `## Deferred` |
   | `wave-g-attachment` | Next | `DEFERRED` | `## Deferred` |
   | `design-channel-draft-queue` | Next | `EXTERNAL` | `## Externals` |
   | `lookup-column-time-next` | Next | `OPEN` | merge with `lookup-column-time-open`? |
   | `repo-review-4` | Next | `OPEN` | ordering vs. `PRIORITY` |

3. **PRIORITY ordering — nothing moves, and the reason is the ruling's own
   source.** The ruling orders `## Next` "per the ADR-0141 handoff queue as
   recorded in the compression-arc row's own text". That row's text records
   exactly one ordering — the arc's internal `A -> B -> C -> D` — and no
   ordering of the other rows against each other. This is ADR-0144 F-9 read
   back and confirmed, not re-litigated: `PRIORITY 1` is ruled, the rest carries
   the file's own order.
4. **The review-3 arc tag — verified, and the row closed.** The author pushed
   `stable-20260815-review-3-fixes`; `git ls-remote --tags` shows it on the
   remote as an ANNOTATED tag peeling to `b96c246`. Its roadmap row's whole
   reason to exist was discharged, so under `rulings.md#R-register-hygiene-at-close`
   it moved: six lines verbatim to the attic (read-back verified, 6 of 6 lines
   byte-identical against `HEAD`), one `## Done` pointer tokened with the sha.
   Landed as this session's own Step 0 commit, `11a2eff`.

#### A charter premise, corrected: it is five sets, not four

The prompt's context says "four sets sit above their ratchet baselines by
formula (ADR-0144 close)". Measured at `e0cd075`, it is **all five**:

| set | actual | budget | baseline | formula (actual x1.15) |
|---|---:|---:|---:|---:|
| `:onboarding` | 1,449 | 1,665 | 1,665 | **1,670** |
| `:corpus` | 1,961 | 2,245 | 2,245 | **2,260** |
| `:sim` | 1,407 | 1,610 | 1,610 | **1,620** |
| `:judge` | 1,055 | 1,205 | 1,205 | **1,215** |
| `:docs` | 868 | 990 | 990 | **1,000** |

No set is over its budget and no budget is over its baseline, so nothing is red
and nothing STOPs; this is the charter, disclosed, not a failure.

`:onboarding` joined the other four because ADR-0144's own post-compaction
figure for it does not survive re-measurement. That ADR records `:onboarding` at
1,446 and itemises it as `305 + 59 + 65 + 218 + 166 + 33 + 289 + 258`. Those
eight numbers sum to **1,393**, not 1,446, and four of them do not match the
tree at that ADR's own closing commit: `AGENTS.md` is 303 not 305,
`session-records/README.md` 221 not 218, `prompts/README.md` 169 not 166, and
`build-session/SKILL.md` **309 not 258** — the ADR-0143 skills rider had already
taken it to 309. The correct actual at `e0cd075` is 1,449, and 1,449 x 1.15 =
1,665.35, which rounds to 1,670: three lines over the baseline that ADR set from
the mis-itemised number. Recorded as an erratum to ADR-0144's reading-set table,
in ADR-0144's own erratum form (`rulings.md#R-dated-addendum-not-silent-edit`),
not as a silent correction here.

### Census

Population and classification are emitted by `bin/rulings-migrate-0145
--markdown`, from the same tables the migration executes, so the census and the
move cannot disagree about what a block or a bullet is.

#### 1. What the register holds

**55 `## From ...` blocks plus the file's own 26-line header; 181 bullets across
them** (186 classification entries — five bullets carry more than one standing
rule and yield more than one row). By class: **91 STANDING**, **89 ARC-LOCAL**,
**6 SUPERSEDED**.

The per-block table (block, lines, bullets, destination ADR) and the full
per-bullet table (line, class, row, the sentence that decides it) are the
script's own `--markdown` output; they are reproduced in
`.agents/plans/2026-08-17-rulings-census.md` rather than inlined here, because
they run to 250 lines and the script regenerates them on demand.

The classification test is the register's own header, quoted back at it:
*"ongoing rules a future session must still follow, not one-off execution
choices."* Applied strictly, it puts the majority of the file on the ARC-LOCAL
side — 89 of 181 bullets are an execution record of one session, and their text
belongs in that session's ADR, which is where the migration puts it.

#### 2. Blocks by destination

An arc block goes to that arc's own close ADR (`## From the UX arc
(ADR-0056–0064)` → `notes/adr/0064-ux-arc-close.md`); a single-ADR block goes to
that ADR; the file's own header goes to this ADR. Every one of the 56
destinations resolves to a file that exists, asserted by the script before it
writes anything.

#### 3. Inbound references, and which must survive

Scanned over the live-surface include-list `ehrt.docs-tooling.roadmap-lint-test`
already defines (ADR-0144 F-2): `roadmap.md`, `plans/README.md`, `rulings.md`,
`AGENTS.md`, and every `SKILL.md` in both skill trees. Dated one-shot records
and ADR bodies are out of that population by the standing boundary, and their
`rulings.md` cites are true statements about the day they were written.

| cite | what it names | disposition |
|---|---|---|
| `AGENTS.md:53` | `rulings.md`'s own **AR-R-2** | → `rulings.md#R-stable-tag-author-only` |
| `.agents/skills/manual-review/SKILL.md:35` | ADR-0113 R5, via `rulings.md` | → the ADR; R5 is ARC-LOCAL and has no row |
| `.agents/skills/manual-review/SKILL.md:163` | `rulings.md`, "From ADR-0113," **R6** | → `rulings.md#R-diagrams-derive-from-data` |
| `.agents/skills/repo-review/SKILL.md:51` | "enumerate `rulings.md`'s standing rulings and map each to its enforcing test" | **left as is** — it names no block, and the row form is what finally makes that probe cheap |
| `.agents/skills/build-session/SKILL.md:227` | `rulings.md` as an example of a row register | rewritten as part of this session's own split |
| `.claude/skills/**` mirrors of the above | | rewritten identically, mirror held byte-equal |
| `.agents/rulings.md:736`, `:1006` | two blocks citing other blocks of the same file | move WITH their blocks into ADR-0113 / ADR-0122, where they become what they already are: a dated record's cite of the file as it then stood |

**Inbound anchors:** `git grep "rulings\.md#"` over the whole tracked tree
returns **zero hits** before this session. As with ADR-0144's slug table, there
is no existing anchor scheme the new one must preserve.

#### 4. The build-session split map

Per step: lines before, lines the imperative keeps, lines of history moved to
`.agents/skills/build-session/HISTORY.md`. "History" here is the cite-with-story
material — worked examples, near-miss anecdotes, incident narratives, the
provenance chain behind a rule — not the rule.

| step | before | imperative | history |
|---:|---:|---:|---:|
| 1 ceremony mode | 8 | 3 | 5 |
| 2 preflight | 14 | 2 | 12 |
| 3 WSL git | 6 | 2 | 4 |
| 4 staging hygiene | 6 | 2 | 4 |
| 5 secrets scan | 3 | 1 | 2 |
| 6 message via file | 5 | 2 | 3 |
| 7 checkpoint isolation | 20 | 3 | 17 |
| 8 red capture | 9 | 2 | 7 |
| 9 sweep census | 12 | 2 | 10 |
| 10 post-push verify | 9 | 2 | 7 |
| 11 tag ceremony | 21 | 4 | 17 |
| 12 premise mismatch | 5 | 3 | 2 |
| 13 close scaffold | 15 | 3 | 12 |
| 14 register hygiene | 10 | 2 | 8 |
| 15 budget stop | 15 | 2 | 13 |
| 16 red pushed with green | 12 | 2 | 10 |
| 17 anchored edits | 14 | 2 | 12 |
| **Procedure total** | **184** | **39** | **145** |

Four of the seventeen steps state a rule the register now owns (11, 14, 15, 16,
17); those steps keep the imperative and cite the row rather than restating the
rule, which is the one-definition discipline `rulings.md#R-law-surface-propagation`
asks for and the reason the skill can shrink without losing anything.

#### 5. The reading-sets header

| section | lines |
|---|---:|
| seed header (migration item 8, 2026-08-02) | 11 |
| composition principle (AR-1) | 6 |
| migration session 5 note | 17 |
| THE RATCHET (2026-08-16, ADR-0143 guard #3) | 27 |
| nineteen dated budget re-derivations, 2026-08-05 → 2026-08-17 | 354 |
| **header total** | **415** |
| per-set rationale comments inside the map | 65 |
| actual data | 48 |

The nineteen dated re-derivations are per-session provenance — the file's own
header already says so: *"They stay as per-session provenance; they are history,
not instructions."* A file that says that about 80% of its own contents is
describing a move it has not made.

### Findings for the author

**F-1 — the register's own contract was unenforceable as written, and that is
why it decayed.** "Standing rulings only, appended at each arc close" names no
shape a test can check. Twelve of the 55 blocks are not arc closes at all (the
mid-arc appends ADR-0048 and ADR-0098 both declare themselves deviations); the
rest kept the heading form and abandoned the content rule. The row contract is
the same rule stated so that `ehrt.docs-tooling.rulings-lint-test` can hold it.

**F-2 — 89 of 181 bullets are execution records, and this is not sloppiness.**
Every one of them was written by a session honestly recording what it did,
under a register whose contract said to append. The defect is the destination,
not the diligence: `rulings.md#R-session-narrative-hierarchy` (ADR-0046) already
says the ADR is the sole narrative of a session. The migration does not delete
one word of those 89; it moves them where that rule already said they belonged.

**F-3 — one bullet asks a question nobody has answered, and it is 26 ADRs
old.** ADR-0119's own "commit-sequencing STOP-AND-REPORT departure" ends
*"Recorded here so a future session (or the author) can affirm or narrow this
reading of STOP-AND-REPORT for mechanical, no-design-ambiguity conflicts of this
same class."* Nothing ever did. Classified ARC-LOCAL and moved to ADR-0119 with
the rest of its block, because an unaffirmed reading is not a standing rule —
but it is a live question, and it is put here rather than buried.

**F-4 — six rulings were superseded and only two said so.** `AR-R-2` (tag law)
and the ADR-0114 `R8` seed licence carry their own superseding notes; the other
four — this register's append contract, ADR-0101's append-in-place footnote
form, ADR-0108's user-manual deferral, ADR-0131's WARN-mode collision guard —
were superseded silently by a later ruling that simply stated the new rule. All
six now carry a `SUPERSEDED-BY` row naming a successor that exists, and the lint
checks the successor resolves.

**F-5 — two rows are the same rule stated twice, five days apart, and both are
kept.** `rulings.md#R-unrelayed-tag-condition-stops` (ADR-0134) and ADR-0143's
own conditional-tag entry describe the same discipline; the ADR-0143 bullet is
classified ARC-LOCAL because its own condition was a *state* the session could
re-check, not a *relay* it could not. The distinction decided this session's own
Step 0, which is the strongest evidence available that it is a real one.

**F-6 — `.agents/rulings.md` was in no reading set at all.** 1,757 lines of
standing law that no cold session was ever pointed at, while the budget
mechanism spent nineteen sessions measuring the files it was pointed at. Q5
fixes the membership; the compression is what makes the membership affordable.

### What lands

1. `bin/rulings-migrate-0145` moves all 56 blocks verbatim into their ADRs and
   writes the register from the census classes, with `--verify` reading every
   block back out of its destination.
2. `ehrt.docs-tooling.rulings-lint-test` gates the row contract.
3. `build-session/SKILL.md` splits into rules (117 lines) and `HISTORY.md`; the
   `.claude/` mirror is held byte-equal.
4. `.agents/reading-sets.edn`'s header attics to
   `.agents/plans/reading-sets-history.md`, gated at 20 comment lines.
5. `.agents/rulings.md` joins `:onboarding`; every budget and baseline is
   re-derived at the new actuals.

Steps 2 through 4 of this record are appended as they land.

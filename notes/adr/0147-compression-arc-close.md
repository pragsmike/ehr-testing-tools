## ADR-0147 — The compression arc closes: the continuity register re-derived, generated where derivable, capped where hand-owned, attic'd where historical (session D)

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-17.

### Context

Sessions A, B and C of the register-compression arc each took one
register and made its size structural rather than habitual: `notes/ADRs.md`
generated (ADR-0143), `.agents/plans/roadmap.md` given a row contract and
its lint (ADR-0144, 1,684 → 290 lines), `.agents/rulings.md` made
standing-rules-only with the `build-session` skill split from its history
(ADR-0145, 1,757 → 238 lines). C's close chartered D at what was left:
`.agents/session-records/README.md` and `.agents/prompts/README.md`, 390
lines of dated index between them, both `:onboarding` members, growing one
line per session forever — and the register none of the three had touched,
`.agents/state.md`.

D's own charter added that file, and it is the specimen. What follows is
the census, re-derived at tip `0b15e87` before anything was moved.

### The census

#### 1. `.agents/state.md` — 724 lines

**Preamble: 11 dated blocks, lines 3–337** (the region between the title
and the second `---`). One `FULL REGENERATION` block, 2026-08-15, at
ADR-0139's close; ten `CITATION-ONLY update` blocks, 2026-08-09 through
2026-08-14, each recording an ADR that landed without a regeneration.

| # | date | kind | lines |
|---|---|---|---|
| 1 | 2026-08-15 | FULL REGENERATION (ADR-0139) | 3–33 |
| 2 | 2026-08-14 | CITATION-ONLY (ADR-0133) | 37–75 |
| 3 | 2026-08-14 | CITATION-ONLY (ADR-0132) | 77–100 |
| 4 | 2026-08-14 | CITATION-ONLY (ADR-0131) | 102–143 |
| 5 | 2026-08-14 | CITATION-ONLY (ADR-0130) | 145–188 |
| 6 | 2026-08-13 | CITATION-ONLY (ADR-0129) | 190–215 |
| 7 | 2026-08-13 | CITATION-ONLY (ADR-0128) | 217–243 |
| 8 | 2026-08-13 | CITATION-ONLY (ADR-0127) | 245–265 |
| 9 | 2026-08-13 | CITATION-ONLY (ADR-0126) | 267–287 |
| 10 | 2026-08-11 | CITATION-ONLY (ADR-0107) | 289–313 |
| 11 | 2026-08-09 | CITATION-ONLY (ADR-0097) | 315–337 |

**FINDING S-1 (channel figure wrong, disclosed).** The driving prompt
records "340 of preamble (13 dated update blocks)" and "nine sections
stamped `[V @b96c246]`". The line span is right; the block count is
**11**, not 13, and the stamped-section count is **7**, not 9
(`Environment` is `[A, 2026-08-15]`, and `What this repo is` and
`Design-channel contract` carry no tag at all). Probed by enumerating
bold-opening paragraphs above line 340 and `^## ` headings. This is the
`state.md` line of `rulings.md#R-transcript-not-record` doing its job:
the file, not the summary of it, is the population.

**Sections: 10, classified.** DERIVABLE means a tree query answers it;
the source of truth and the gate that would hold it are named.

| section | lines | class | source of truth / gate | currency at `0b15e87` |
|---|---|---|---|---|
| What this repo is | 341–367 | MIXED | prose hand-owned; module count from the modules dir | **stale in part** — the "Since the last regeneration (0090–0139)" narrative is history, not state |
| Component graph `[V]` | 369–402 | DERIVABLE | `ls -d components/*/`, `bases/*/`, `components/*/resources`; `poly check` | **HOLDS** — 18 / 1 / 9 all re-derive |
| Vendored module inventory `[V]` | 404–429 | DERIVABLE | modules dir; `digest.clj`'s `roots`; NOTICE rows | **STALE in one of three** — 31 modules and 80 NOTICE rows re-derive; **34 oracle roots does not, the map holds 35** (erratum below) |
| Where history lives `[V]` | 431–453 | DERIVABLE | `ls notes/adr`; roadmap sections | **STALE** — claims 137 ADR files and 137 index entries; actual **144 / 144**. Claims Next 31 / Done 44; actual **19 / 59** |
| Standing gates `[V]` | 455–524 | DERIVABLE | test dirs; `reading-sets.edn` | **STALE** — claims 36 docs-tooling gates; actual **39**. Claims `:onboarding` 2658 / 2690; actual **1526 / 1665** (ADR-0144/0145 moved both halves) |
| The repo-review instrument `[V]` | 526–558 | HAND-OWNED | the review's own register; a scoreboard is judgement | current as judgement |
| The pairing registry `[V]` | 560–582 | DERIVABLE | `pairing-registry.edn`; `taxonomy.edn` | **HOLDS** — 12 rows, split 5 hapi / 5 fhir-official / 2 nist, engine 1.7.3 |
| Live work `[V]` | 584–634 | MIXED | roadmap for the counts; watch items hand-owned | **STALE in the counts** — Next/Done as above; claims 105 `defspec`, actual **109**. Census artifacts 10: HOLDS |
| Environment `[A]` | 636–703 | MIXED | tags and suite counts derivable; ceremony prose hand-owned | **STALE** — claims 92 `stable-*` tags, actual **98**; claims a 640-block / 16,382-assertion suite, superseded by ADR-0146's own 338 / 3,830 / 17,354 |
| Design-channel contract | 705–724 | HAND-OWNED | — | current |

**Six** of the ten sections carry a stale claim (five at the census as
first written, plus the oracle-root correction below). Every one of the
six is DERIVABLE, and none of the hand-owned sections is stale — which is
the whole finding of this census, and the reason the split falls where it
does.

**Erratum, 2026-08-17, same session, Step 2.** The census above first
recorded the vendored-inventory section as HOLDS on all three of its
figures. It does not: the oracle-root count is **35**, not the 34 that
section claims, and this ADR is corrected rather than rewritten so the
record shows both readings and how the second was reached.

The correction was forced by a defect in this session's own parser, and
the sequence is worth keeping because it is the census's own thesis
happening to the census:

1. The first draft of `parse-oracle-roots` anchored keys at `^\s+"`. The
   map's first key shares the opening brace's line
   (`{"appendicitis"  appendicitis-batch`), so it was skipped. **The
   live-tree sanity case passed** — one fewer root is still non-empty,
   distinct and well-formed — and only the synthetic fixture, whose
   answer is known independently, went red.
2. Fixing that returned 35, contradicting the section. Cross-checked by
   hand against `digest.clj` lines 545–579: **35 keys**, and the prompt's
   own fence ("oracle IDENTICAL 35/35") agrees. The `[V @b96c246]` claim
   of 34 is simply stale — `injuries` joined at ADR-0107 and the section
   was not re-probed.
3. Investigating *that* exposed a second defect: the map's last key
   closes it (`"injuries" injuries-pair})`), so no line begins with `}`
   and the `take-while` terminating on that never fired. The scan ran
   past the form into `-main`'s docstring. It returned 35 anyway, **by
   luck** — no line there happens to hold a complete `"..."` at its
   start. A docstring reflowed by one word would have added a phantom
   root. The window is brace-balanced now, and the fixture carries a
   case for both shapes.

Recorded at length because the moral is the one this ADR is about: a
count nobody re-derives drifts, and a parser checked only against the
tree it parses cannot tell you it is wrong. The mechanism-sanity case is
not ceremony.

**FINDING S-2.** The regeneration contract (`rulings.md#R-state-regeneration`,
ADR-0047 AR-C-1) asks a session to re-probe every `[V]` claim by hand at
each arc close. In fifty ADRs it was met **once**. The remaining eleven
blocks each say so in their own words. The contract was never ignored; it
was performed, at a cost, by hand, and it does not survive contact with a
session that has other work. A claim re-derived by hand at each close is
stale between closes by construction.

#### 2. The two dated indexes

| file | lines | convention prose | per-record rows | rows carrying a hand annotation |
|---|---|---|---|---|
| `.agents/session-records/README.md` | 223 | 1–70 | 149 (lines 75–223) | 22 |
| `.agents/prompts/README.md` | 171 | 1–26 | 142 (lines 30–171) | 22 |

Both are `:onboarding` members, so every cold session of every task class
reads 390 lines of dated listing to learn a filename convention.

**Consumers, by grep over the tree** — `ehrt.docs-tooling.index-completeness-test`
(both directions: every real file indexed, every indexed file real),
`ehrt.docs-tooling.prompt-record-pairing-test` (slug pairing, which reads
the DIRECTORIES, not the READMEs), and `bin/close-scaffold` (appends one
star-bullet line to each README at close).

**Does any consumer need the rows?** No. `index-completeness-test` needs
*a* file per directory listing the tokens; it does not care which file.
The pairing test never reads a README. `close-scaffold` writes the rows
rather than reading them. So the rows can move to a generated sibling
with a one-line change to one gate — which is what this session does.

#### 3. `.agents/state.md`'s own consumers

| consumer | what it actually reads |
|---|---|
| `ehrt.docs-tooling.state-staleness-tripwire-test` | ONE phrase in the header: the `own close (\`notes/adr/NNNN-<slug>-arc-close.md\`)` citation. Currency only, explicitly not content |
| `.agents/skills/repo-review/SKILL.md:49` (+ `.claude/` mirror) | "sample `[V]` claims in `.agents/state.md` live" — i.e. it consumes the file *as a source of claims to falsify* |
| `.agents/rulings.md#R-state-regeneration` | states the contract; reads nothing |

**FINDING S-3 (prompt premise does not hold).** The driving prompt's
read-first list says "`handoff` skill (the design channel reads state.md
at open — keep what it needs)". `.agents/skills/handoff/SKILL.md` contains
**zero** occurrences of `state.md` (`grep -c` → 0). It is not a consumer,
and nothing was kept for it. Recorded rather than adapted around
(`docs/dev/way-of-working.md` §2).

**FINDING S-4.** `AGENTS.md`'s own `.agents/` routing section lists
`session-records`, `plans`, `rulings`, `memory`, `skills` and `prompts` —
and does **not** mention `state.md` at all. The continuity register a new
design session is told to read first is unrouted from the primary
instruction surface. Fixed at this session's close.

#### 4. `:onboarding`, measured and projected

| composition | actual |
|---|---|
| as at `0b15e87` | **1,526** |
| minus the two READMEs' row halves | 1,142 |
| plus a ≤120-line `state.md` (ruled IN, Q4) | **≈1,260** |

Budget 1,665, ratchet baseline 1,665. The projection lands the set far
enough below its baseline that the ratchet can fall — which is the exit
condition ADR-0145 recorded that session C could not meet.

### Step 0 receipts

- **Tag paid.** `stable-20260817-emitter-author-ux` created annotated at
  `0b15e87` and pushed via `bin/tag-ceremony`, licensed by this session's
  prompt on CI run `32041400966` (workflow `test`, `headSha` `0b15e87`,
  conclusion **success**, read with `gh run view` before tagging). Remote
  peeled ref verified equal to `0b15e87`.
- **`bin/preflight`**: last five CI runs green; edit root not under
  `/mnt/`; tree clean including untracked; HEAD matches `origin/main`;
  HEAD not yet tagged (paid immediately after).
- **FINDING S-5 — and its own correction, which went the other way.**
  The prompt asks the baseline to reconcile against "ADR-0146's recorded
  338 blocks / 3,848 tests / 17,422 assertions". ADR-0146 line 81 records
  **338 / 3,830 / 17,354**. At Step 1 this ADR called the prompt a
  channel slip, on the reasoning that the ADR is the artifact. **The clean
  baseline run settles it, and the Step-1 reading was wrong.** Measured at
  `0b15e87` from a disposable worktree, `MAKE_EXIT=0`: **338 blocks /
  3,848 tests / 17,420 assertions**, zero `FAIL`/`ERROR` lines, 338
  zero-failure blocks.

  So the prompt's block and TEST counts are exactly right, and ADR-0146's
  3,830 / 17,354 describes an earlier tree than its own closing tip —
  taken mid-session, before that session's last commits landed. What
  survives as a real discrepancy is small, and is disclosed rather than
  smoothed: **17,420 measured against the prompt's 17,422**, two
  assertions I cannot account for from a single run and will not invent a
  cause for.

  Recorded at this length because the correction is the point. "The ADR is
  the artifact" is a good rule and it produced a wrong answer here,
  because an ADR's figure is a measurement of the tree AT THE MOMENT IT
  WAS TAKEN, not of the commit it ships in. The live tree outranks both
  documents — the same lesson this census reaches about every `[V]` claim
  in `state.md`, arrived at this time against this ADR's own text.
- **FINDING S-6 (this session's own near-miss, recorded).** The first
  baseline run was started before any edit and was still in flight when
  the Step-0 roadmap re-triage and the Step-2 test files landed in the
  working tree. It therefore ran the new tests, went red on
  `state-md-is-an-onboarding-member-test`, and **aborted at 242 of ~338
  blocks with `MAKE_EXIT=2`** — the same shape of aborted run ADR-0146
  records at its own line 100, and caught the same way, by capturing the
  exit code explicitly instead of reading a `tail`. The contaminated run
  is discarded, not reported as a baseline; a clean baseline was taken
  from a disposable `git worktree` detached at `0b15e87`.

### The owed session-B item, executed

ADR-0144's finding F-8 listed five roadmap rows whose status token
suggests a different section and left them in place, that being author
judgement. The author has since ruled: **each moves to the section its
token names, no content change.** Executed here.

| row | from | token | to | moved? |
|---|---|---|---|---|
| `downstream-latency` | Next | `DEFERRED` | Deferred | yes |
| `wave-g-attachment` | Next | `DEFERRED` | Deferred | yes |
| `design-channel-draft-queue` | Next | `EXTERNAL` | Externals | yes |
| `lookup-column-time-next` | Next | `OPEN` | — | **no**: `OPEN`'s own section IS `## Next`. F-8's note suggested merging it with `lookup-column-time-open`, which is content change, which the ruling excludes |
| `repo-review-4` | Next | `OPEN` | — | **no**: same. F-8 flagged it for ordering (F-9), not section |

Two disclosures. **(a) `PRIORITY` is Next-only** — `roadmap-lint-test`
requires it on `## Next` rows and checks it nowhere else — so the three
moved rows drop theirs, which is the only text that changed on any of
them. **(b) The remaining priorities are left with gaps at 7, 11 and 14
rather than renumbered.** The lint requires unique and *ascending*, not
contiguous; ADR-0144 F-9 records that these numbers carry the file's own
order and no ruled queue, which gaps preserve exactly. Renumbering twelve
rows to close three gaps is churn the row contract does not ask for.

`intake-staging-dir`'s missing revisit trigger (ADR-0144 F-6) is
**DEFERRED by the author**; its row is left as it stands, which the lint
permits.

### What this session does

1. **Generate what is derivable.** `ehrt.docs-tooling.state-derived`
   renders `.agents/state-derived.md` plus `.agents/session-records/INDEX.md`
   and `.agents/prompts/INDEX.md` from the live tree, on `make docsgen`,
   diffed by CI's generated-doc freshness step.
2. **Cap and lint what is hand-owned.** `.agents/state.md` becomes a
   ≤120-line residue with a register pointer table, gated by
   `ehrt.docs-tooling.state-residue-test`.
3. **Attic what is historical, verbatim.** `bin/state-migrate-0147`
   moves the whole prior file and both README row-lists to
   `.agents/plans/state-history-2026-08.md`, with a `--verify` read-back.
4. **Reshape and ratchet `:onboarding` down.**

Its own close, the cold-read acceptance probe, and the arc's laws are
below, landed at the close.

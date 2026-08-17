## ADR-0146 — The emitter author's own path: a cold walk of every entry surface, the signposts it was missing, and a second worked emitter

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-17.

### Context

ADR-0141 built a seam and built it well. The ground-truth event log
became a public, versioned contract: `ehrt sim run --format
ground-truth` emits the bare EDN vector, `docs/formats.md`'s "The event
log" section is generated from a committed malli schema, twenty-one
kinds are enumerated with a real example each,
`bin/example-custom-emitter` proves a consumer can render the log into a
format this project has never heard of using nothing off this repo's
classpath, and `bin/usecase-custom-emitter` re-runs the page's own fence
verbatim. Every part of it is exercised. None of it is findable.

The author's ask, verbatim:

> "We should do a UX pass just for that actor."

> "I want to fix the UX path for custom emitters ASAP."

And, on the second worked emitter this session proposed as item 3:

> "item 3 ok."

So this is a UX arc session in the ADR-0056..0064 discipline, aimed at
exactly one actor: a **cold walk** from every entry surface as that
actor, findings as register rows with `file:line` evidence and a
hops-to-seam table, then ruled fixes, then a re-walk whose hops table is
the acceptance evidence. Docs-only, plus one exercised script.

### The actor card

Written before the walk, and quoted here because every finding below is
scored against it and nothing else:

> **"I run a hospital-adjacent system with its own message format. I
> want simulated hospital traffic in MY format. I have never read this
> repo. I have ten minutes."**

Four things this actor needs, in this order:

- **(a)** knowing the log exists *as something they can have* — not as
  an architectural concept, as a command
- **(b)** the contract — `docs/formats.md#the-event-log`
- **(c)** the worked example — the actual mapping code, not a launcher
- **(d)** the schema-version promise — what happens when the log changes

Ten minutes is the binding constraint. A route that exists but costs
four unguided hops is, for this actor, not a route.

### Step 0

Fresh tree at `d62ed19` (ADR-0145), clean including untracked, HEAD
matching `origin/main`. `bin/preflight` reported the last five CI runs on
`main` all green, edit root not under `/mnt/`, and — DISCLOSED — that
HEAD carried no `stable-*` tag, which the two licences below then paid.

**Both tag licences PAID.** The prompt licensed
`stable-20260817-roadmap-row-contract` at `e0cd075` on `gh run view`
success for run **32023934757**, and
`stable-20260817-rulings-standing-only` at `d62ed19` on success for run
**32033449792**. This session verified both itself, per the standing
ADR-0145 Step 0 ruling that sessions run `gh run view` rather than wait
for a relay:

| tag | target | run id | conclusion | peeled ref verified |
|---|---|---|---|---|
| `stable-20260817-roadmap-row-contract` | `e0cd0755` | 32023934757 | `success` | yes, matches exactly |
| `stable-20260817-rulings-standing-only` | `d62ed190` | 32033449792 | `success` | yes, matches exactly |

Both went through `bin/tag-ceremony … --push`; both finished on the
script's own peeled-ref check against the remote. This closes the
ADR-0145 deviation, whose licence was conditioned on a relay its prompt
did not carry — the narrowed ruling replaced the relay with the
session's own `gh run view`, and that is what ran here.

**Baseline suite.** `make test` unpiped to a log, `MAKE_EXIT=0`, zero
failures and zero errors: **338 `Ran …` blocks / 3,830 tests / 17,354
assertions**, `clojure -M:poly check` OK as its first step,
`bin/verify-nist-lock` matching all six coordinates. Execution time 13m51s.

This **reconciles exactly** with the figure the prompt carried — 338
runs / 3,830 blocks / 17,354. Recorded here because the reconciliation
took two runs and one honest correction:

- ADR-0145's own artifact records **336 / 17,278**, and that is not a
  discrepancy: it is that session's *Step 0* number, measured at
  `e0cd075` **before** its own work. ADR-0145 recorded no closing
  figure, so the post-ADR-0145 state (`d62ed19`, +2 blocks / +76
  assertions, its rulings-lint gate) existed only in that session's
  transcript until this run. Under
  `rulings.md#R-transcript-not-record` this session treated the
  prompt's number as unverified and measured rather than assumed; it
  then matched to the digit. The artifact gap is now closed by this
  ADR.
- **DISCLOSED: the first baseline run was contaminated by this session
  and is reported rather than quietly discarded.** `MAKE_EXIT=2`, one
  failure — `notes-adrs-md-is-exactly-what-the-generator-renders-test`
  (`adr_index_test.clj:58`) — caused by this session creating
  `notes/adr/0146-*.md` while the suite was running, which made the
  generated `notes/ADRs.md` stale against the ADR set. `poly test`
  aborted at that project, so that run's own tallies (242 blocks) were
  partial and meaningless. The ADR file was moved aside, tree
  re-verified clean, and the run above taken from scratch. The lesson
  is small and worth the line: with a generated index under a
  no-drift gate, authoring an ADR is a tree mutation, and a baseline
  must be taken before it or after `make adr-index`, never across it.

**Reading sets**, measured at Step 0, all five at or under baseline:

| set | actual | budget | baseline | headroom |
|---|---|---|---|---|
| `:onboarding` | 1501 | 1665 | 1665 | 164 |
| `:corpus` | 1774 | 2045 | 2045 | 271 |
| `:sim` | 1220 | 1405 | 1405 | 185 |
| `:judge` | 868 | 1000 | 1000 | 132 |
| `:docs` | 681 | 785 | 785 | 104 |

`:onboarding`'s 1501 matches ADR-0145's own recorded actual exactly. It
is also the only set this session touches at all — via `.agents/rulings.md`
and `.agents/plans/roadmap.md`, its two members that take rows here —
and 164 lines is the whole budget for that. Verified again at the close.
No `docs/**` file is a member of any set (checked, not assumed: the four
task sets carry `docs/dev/*` paths only, and `:onboarding` carries none),
so the bulk of this session's work is budget-neutral by construction.

### Step 1 — the cold walk

The walk is a **review**: it produces rows, never edits
(`.agents/skills/manual-review/SKILL.md`'s own discipline, and
`rulings.md#R-review-actor-split`). Entry surfaces were enumerated from
the tree rather than chosen, using the stale-path gate's own scan-root
class as the population: `README.md`, `docs/README.md`,
`docs/what-is-this.md`, `docs/manual/00-front.md`'s TOC plus chapters 3
and 8, `docs/use-cases.md`, `docs/glossary.md`, `docs/cli.md`,
`docs/formats.md`'s top, `docs/dev/AUDIENCES.md`, and the three CLI help
surfaces — `bin/ehrt --help`, `bin/ehrt sim --help`, `bin/ehrt sim run
--help` — each actually run, not read out of a source file.

#### The hops table, BEFORE

Hops are navigation steps from that surface: a link followed, a fence
run, a page scanned for a row. `—` means **no route exists from this
surface at all**: not far, absent.

| entry surface | (a) log exists | (b) contract | (c) worked example | (d) version promise |
|---|---|---|---|---|
| `README.md` | 2 | 3 | 3 to a launcher, 4+ unguided to code | 2 |
| `docs/README.md` | 2 | 3 | 3 / 4+ | 2 |
| `docs/what-is-this.md` | — | — | — | — |
| `docs/manual/00-front.md` (TOC) | — | — | — | — |
| `docs/manual/03-a-simulated-hospital.md` | 1, concept only | — | — | — |
| `docs/manual/08-your-own-data.md` | — | — | — | — |
| `docs/use-cases.md` | 1, after scanning 22 rows | 2 | 2 / 3+ | 2 |
| `docs/glossary.md` | 1, concept only | — | — | — |
| `bin/ehrt --help` | — | — | — | — |
| `bin/ehrt sim --help` | 1, as an input to our own checker | — | — | — |
| `bin/ehrt sim run --help` | 1, as an input to our own checker | — | — | — |
| `docs/cli.md` | 1, as an input to our own checker | — | — | — |
| `docs/formats.md` (top) | 1, only by scanning to `## The event log` | 1 | 2 | 1 |
| `docs/dev/AUDIENCES.md` | — | — | — | — |

Eight of fourteen surfaces have **no route at all** to the fact that the
log exists. The one surface that serves this actor superbly —
`docs/formats.md`'s own event-log section — is the only one that does,
and its own page top does not announce it.

#### Findings register

**U-1. `docs/dev/AUDIENCES.md` does not contain this actor** — and it is
the canonical register every routing surface keys off. `docs/README.md`
says so explicitly at `docs/README.md:5-7`: *"`docs/dev/AUDIENCES.md`'s
Audience section is the canonical register these paths are keyed off;
this page just routes."* Five segments are registered. The nearest,
segment 4 (`docs/dev/AUDIENCES.md`, "The downstream data consumer"),
rules this actor out by its own words: *"and never runs the CLI
themselves; a Python or SQL process on the other end of a pipeline."*
The emitter author runs the CLI once, wants no report, and writes code.
**This is the root finding**: every other routing gap below is
downstream of an audience register that has no row to route to.
*Proposed fix:* a named segment with a stated entry path.

**U-2. `README.md` gives this actor no signal the log exists.** The
whole file — 280 lines, "See it run", "The workflow it exists for",
"What you get", "Where to start", Quickstart, Maturity, Scope — never
mentions the event log, `--format ground-truth`, or writing your own
emitter. The only route is a generic one: `README.md:81`,
*"[`docs/use-cases.md`](docs/use-cases.md) walks through each one with
runnable commands."* "Where to start" offers two branches
(`README.md:170`, `README.md:176`) — generate/judge test data, or
maintain the workspace — and this actor is neither. The likeliest
ten-minute outcome from `README.md` alone is the conclusion that this
project emits HL7v2 and FHIR and nothing else, and departure. *Proposed
fix:* one paragraph in "What you get" — the log exists, one command
shows it, one link to the use case, one to the manual chapter.

**U-3. `docs/what-is-this.md:69` actively misleads, by enumeration.**
*"Every stage's output is plain FHIR JSON, HL7v2 ER7 text, and EDN
manifests — readable from Python, SQL, or anything else."* That is a
closed list of what comes out, and the richest thing that comes out is
not on it. The "What it does" bullets (Generate/Mutate/Gate/Check) and
the six-bullet "Audience" list have no room for this actor either.
*Proposed fix:* the enumeration gains the log; the sentence is the
cheapest possible place to stop misleading.

**U-4. The manual's chapter 8 is a name-collision dead end.** The TOC at
`docs/manual/00-front.md:61` reads *"**Your own data** — cataloging a
corpus you didn't generate…"*. To an actor holding a target format,
"your own data" is exactly where they will go, confidently. The chapter
opens `docs/manual/08-your-own-data.md:4`: *"This chapter is about the
other case: a corpus that arrived from somewhere else."* Your own data
**in**; the actor wants their own format **out**. This is the worst
class of dead end — a confident wrong turn, with nothing at the far end
of it pointing back. *Proposed fix:* one disambiguating sentence in the
chapter's opening, with the link.

**U-5. Manual chapter 3 teaches the founding idea and then routes to the
maintainer path.** `docs/manual/03-a-simulated-hospital.md:124` states
it as plainly as this workspace ever does: *"**formats are just
[emitters](../glossary.md) of the patient state machine.**"* `GT` is
named, both emitters are drawn reading it, the naturality square is
cited — and the reader is sent to
`docs/dev/simulator-architecture.md#4-the-palgebra` for the formalism. A
reader who has just been told formats are emitters over one true log is
never told they can *have* that log, or that the seam is documented and
worked. The chapter's own link out goes to dev docs — the maintainer
path — because at the time it was written that was the only place the
log's shape was discussed at all. *Proposed fix:* a short section, "The
log underneath every message", exactly where the founding idea lands.

**U-6. The glossary's entry for the thing routes to a maintainer doc,
and the name every user-facing surface actually uses has no entry at
all.** A first pass of this walk recorded that no headword existed and
that four manual chapters therefore hyperlinked "ground truth" to a page
without the term. **That was wrong, and the correction is the finding.**
`docs/glossary.md:283` does carry **"Ground-truth log."** — *"The
simulator's primary output and single source of truth: a time-ordered,
immutable sequence of events describing everything that happened in a
run. Messages, state snapshots, and test assertions all derive from
it."* That is accurate, and the four links
(`docs/manual/01-what-this-is.md:19`,
`docs/manual/02-setup-first-corpus.md:104`,
`docs/manual/03-a-simulated-hospital.md:116`,
`docs/manual/04-time-on-the-wire.md:53`) do resolve to a real term. Two
real defects remain:

- The entry's only "See" is
  `components/sim/docs/event-sourcing.md` — the *why*, a
  component-adjacent maintainer doc — and it says nothing about
  `--format ground-truth`, the contract, or the fact that a consumer may
  read it. An actor who follows a manual link to this entry learns the
  log is the source of truth and is then routed away from their own
  path.
- **There is no "Event log." headword** (the E entries run
  `ED`, `Emitter`, `Encounter`, `Error`, `Event`, `Event sourcing`), and
  "the event log" is the exact name `docs/formats.md`'s own section
  heading and the use-case title both use. The term this actor arrives
  holding is not in the authoritative definition set.
- No **"Schema version."** headword either, though the versioning
  promise is the fourth thing the actor needs.

*Proposed fix:* an "Event log" headword pointing at the contract;
"Ground-truth log" gains the consumer route; "Schema version" added.

**U-7. The glossary's `Emitter` entry reads as "wait for us to build
it".** `docs/glossary.md:211-216`: *"A pure function from the
ground-truth log (or state history) to a wire format. HL7v2 messages and
FHIR resources are both built emitters over the same truth… CDA is a
still-planned emitter."* Every emitter named is ours; the only path to a
format we do not ship is described as a plan of ours. `docs/glossary.md:120-123`
says the same thing again for CDA — *"A *state-based* format: a snapshot
document, not an event stream. A planned emitter here."* Both lines
predate the seam and neither was revisited when it landed. Read cold by
this actor they are worse than silence: they answer the actor's question
with "not yet." (Checked: no roadmap row for a CDA emitter exists —
`.agents/plans/roadmap.md` has none — so "planned" is also unbacked, but
the staleness that matters here is the missing "and you don't have to
wait".) *Proposed fix:* correct in place, with the glossary's own dated
form, and say the log is consumable now.

**U-8. `--format ground-truth`'s help names our own checker as its only
consumer.** `bases/cli/src/ehrt/cli/help.clj:222`, rendered identically
into `docs/cli.md:248` and all three help surfaces: *"\"ground-truth\":
the bare ground-truth EDN vector -- pipe straight into `ehrt sim
check`."* A flag whose entire purpose for this actor is "this is the
public contract" describes itself as plumbing between two of our own
subcommands. The CLI is where an agent-assisted reader looks first
(`docs/dev/AUDIENCES.md` segment 2's own standing constraint), and it
carries no cite. *Proposed fix:* the line cites the contract and the use
case; `docs/cli.md` regenerated.

**U-9. `docs/use-cases.md` buries the actor's own heading in row 19 of
22.** The catalog is a flat, undifferentiated list in EDN order
(`docs/use-cases.md:10-31`); the emitter case is `docs/use-cases.md:28`.
There is no actor grouping and no "start here" affordance, so finding
the one relevant row costs reading all twenty-two audience sentences.
For a ten-minute actor arriving from `README.md:81`, that is the hop
where they leave. *Proposed fix:* a generated "Start here, by actor"
table at the top, keyed to case ids that must resolve — so "I have my
own format" is a heading, not a row.

**U-10. The use-case page's link to "the worked example" lands on a
launcher with no emitter logic in it.**
`docs/use-cases/custom-emitter-from-the-event-log.md:29` links
*"[`bin/example-custom-emitter`](../../bin/example-custom-emitter)"*.
That file is 32 lines, of which 22 are comment and the operative
content is `exec clojure -Sdeps … -M -m emitter "$@"`. The actual
mapping code — the thing the actor came for — is
`bin/example-custom-emitter-src/emitter.clj`, and a repo-wide grep of
every `.md` outside `notes/` finds **zero** links to it. The actor
follows the one link labelled "worked example" and arrives at a
classpath incantation. *Proposed fix:* link the source directly, and say
how long it is.

**U-11. "What a real emitter must do" is the last clause of a
five-sentence paragraph.** Same line,
`docs/use-cases/custom-emitter-from-the-event-log.md:29`: *"It also does
the thing a real custom emitter most needs to do: it reports how many
events it did *not* translate, rather than dropping them silently."*
This is the page's single most load-bearing instruction to someone about
to write production code, and it is positioned as an afterthought behind
the contract link, the nested-`:event` warning, and a disclaimer about
the toy format being deliberately useless. *Proposed fix:* its own
prominent paragraph.

**U-12. The instrument that answers "is my emitter finished" exists,
ships in `bin/`, and is invisible from every user-facing surface.**
`docs/formats.md` states the closure — *"There are exactly **21** event
kinds. The set is closed; a reader may treat an unknown `:event` value as
a contract violation"* — and the use-case page cites "21 event kinds"
and links `event-schema.edn`. Neither tells the actor how to *check*
their own coverage, and the one worked example handles 2 of the 21. But
`bin/event-census` already does exactly this job: it *"tabulates the
event vocabulary and per-kind key population from EMITTED EDN"*,
deliberately reading the log rather than the schema so it can be pointed
at a log this repo's own schema would reject
(`bin/event-census:1-21`). It was promoted into `bin/` by ADR-0141 under
author licence for precisely this durability reason. A repo-wide grep
finds it cited **only** from `notes/adr/0141-event-log-contract.md` and
`.agents/plans/2026-08-16-event-log-census.md` — zero citations from
`docs/`, `README.md`, or any help surface. The completeness affordance
was built and never signposted, which is this whole session in
miniature. *Proposed fix:* the page teaches `bin/event-census` against
the actor's own log as the completeness step, and the exerciser runs it.

**U-13. `docs/formats.md`'s own top does not list the event log among
what the page covers.** `docs/formats.md:1-27`: the page addresses *"the
reader on the other end of the pipeline: you get a `report.edn`, a
`manifest.edn`, or a directory of lineage records"*, and its "Three
sources of output" table (`docs/formats.md:15`) names stdout,
`--report`, and files on disk — no `events.edn`, no `--format
ground-truth`. An actor who lands here (the plausible mis-route from
`docs/README.md`'s "Downstream data consumer" section, which is the
closest-sounding segment and the wrong one) reads a page top that
promises reports and does not mention the one section that would have
answered them, 238 lines down. *Proposed fix:* the page top names the
event log and links its own section.

**U-14, mechanical, out of this actor's path but found on it.**
`docs/manual/00-front.md:102` cites *"`.agents/rulings.md`, "From
ADR-0112," "Batch-straddle documentation placements""*. ADR-0145 moved
every `## From …` block out of that file; the citation no longer
resolves there. The text is verbatim in
`notes/adr/0112-batch-straddle-recording.md:222-225`. A stale citation
created yesterday by a compression session, on a surface this walk had
to read. Mechanical, one defensible reading, so it fixes forward with
this disclosure rather than stopping (F-3 narrowed).

#### The seam itself, walked once found

Three questions the prompt asked directly:

- **Is `bin/example-custom-emitter` readable in one screen?** The
  *emitter* is: `bin/example-custom-emitter-src/emitter.clj` is 68
  lines, of which 31 are a docstring that earns its length and ~37 are
  code. That is a genuinely good teaching artefact. The problem is
  purely that nothing points at it — U-10.
- **Does the page say what a real emitter must do, prominently?** It
  says it. Not prominently — U-11.
- **Does anything say how to know your emitter is complete?** No —
  U-12.

### U-15, found by walking into it

The walk produced fourteen rows. A fifteenth came from this session's own
Step 2 commit, and it is the most useful finding here because nothing in
the review would have caught it.

Step 2 added `bin/event-census` as a taught command on the use-case
page's fence and **did not add it to `bin/usecase-custom-emitter`**. The
entire docs-gate battery — 106 tests, 1,094 assertions, including
`strip-fresh-test`, whose whole job is proving that a page and its
exerciser teach the identical commands — stayed green. The commit landed
a page teaching four commands and a script running three.

The cause, on inspection, is two-layered:

1. **`bin/usecase-custom-emitter` was the only row of the nine in
   `exercised-sources.edn` with no live `check-entry` test.** The other
   eight all have one: `quickstart-demo`, `demo-exerciser-ed-tuesday`,
   `demo-exerciser-clinic-decade`, and the five ADR-0129 rows
   (`usecase-judge-tier-calibration`, `usecase-profile-tier-v2`,
   `usecase-acceptance-qa`, `usecase-regression-baselining`,
   `readme-what-you-get`), each asserting `:ok?` and a pinned command
   count. ADR-0141 registered the custom-emitter row and wrote the
   exerciser but never added the case. So the one page whose stated
   selling point is *"EXERCISED FROM BIRTH … this one has never existed
   unexercised"* was the one page never proven at per-push tier to teach
   what its script runs.
2. **It could not have passed if added.** The script wrapped the taught
   redirect as `expect 0 bash -c '…'`, and `strip-fresh`'s unwrapper
   handles `expect` and `expect_eval`, not a `bash -c` wrapper — so a
   literal comparison would always have diverged at that line. The row
   was structurally ungateable, not merely ungated.

Red, witnessed before the fix, naming both halves at once:

    FAIL in (check-entry-live-usecase-custom-emitter-test)
    divergence: {:index 1,
                 :readme "bin/ehrt sim run --seed 42 --patients 5 --format ground-truth > out/custom-emitter/events.edn",
                 :script "bash -c 'bin/ehrt sim run --seed 42 --patients 5 --format ground-truth > out/custom-emitter/events.edn'"}
    expected: (true? (:ok? result))
      actual: (not (true? false))
    expected: (= 5 (:readme-count result) (:script-count result))
      actual: (not (= 5 4 3))

Fixed with the workspace's own sanctioned convention rather than by
widening the comparison layer: `expect_eval CODE 'SNIPPET'`, which
`strip-fresh` already unwraps and which `quickstart-demo`,
`demo-exerciser-ed-tuesday` and `usecase-acceptance-qa` already use for
exactly this case (a taught line that needs a shell). The row now proves
page and script teach the identical five commands, and the test pins the
count so a future addition to either side without the other fails by
name.

**Why this is the session's most transferable result.** "Exercised from
birth" was true and was load-bearing in the ADR that landed it — and it
was not the same claim as "gated". The gap between a script that exists
and a script that is *proven to match its page* is invisible from the
page, from the script, and from a green suite. It took a session
carelessly widening the fence to reveal it, which is the honest account
of how it was found.

### Step 2 — the fixes, and the two I did not take as written

Every proposed fix from the walk was applied except as noted. The
surfaces, in the order the actor meets them:

| finding | fix landed |
|---|---|
| U-1 | `docs/dev/AUDIENCES.md` segment 6, with a stated entry path; header count five → six |
| U-2 | `README.md` "Where to start" third branch, with the command inline |
| U-3 | `docs/what-is-this.md`'s closed output enumeration now includes the log |
| U-4 | `docs/README.md` "I have my own format", placed above the section it is confused with; manual ch. 8 opens with one disambiguating sentence; ch. 3's TOC entry names the log |
| U-5 | manual ch. 3, "The log underneath every message", landing where the founding idea does |
| U-6 | glossary gains "Event log" and "Schema version"; "Ground-truth log" gains the consumer route |
| U-7 | "Emitter" and "CDA / C-CDA" corrected in place, dated |
| U-8 | `--format ground-truth`'s help cites the contract and the worked path; `docs/cli.md` regenerated |
| U-9 | gated `:start-here` table, rendered above the flat catalog |
| U-10 | the page links `emitter_jsonl.clj`/`emitter.clj` directly and says which file is the launcher |
| U-11 | "count what you did not translate" now leads the note |
| U-12 | `bin/event-census` is a taught command with its own exerciser invariant |
| U-13 | `docs/formats.md`'s top names the event log and links its own section |
| U-14 | manual footnote repointed to `notes/adr/0112-*.md`, with a note saying where it moved from |
| U-15 | live `check-entry` test for the custom-emitter row; `expect_eval` replaces `bash -c` |

Two departures from the prompt's expected form, both deliberate:

- **The README paragraph went in "Where to start", not "What you get".**
  "What you get"'s fences are an exercised `:paired` strip re-run by
  `bin/readme-what-you-get`; adding a fence there is either picked up by
  that machinery or excluded by an adjacency subtlety, and duplicating an
  unexercised fence into README would cut against
  `rulings.md#R-examples-are-sourced-verbatim`. "Where to start" is also
  the surface U-2's own evidence indicts — its two branches excluded this
  actor by construction — so the fix belongs where the defect is. The
  runnable fence stays on the use-case page, which has an exerciser.
- **Manual ch. 3 cites its new strip in prose, not in a "Strip source
  citations" table.** Chapters 1–3 carry no such table; adding one to
  ch. 3 alone, listing one of its two strips, would be less honest than
  the in-prose form the chapter already uses for its `generate sim` strip
  ("copied verbatim from that README, never composed for the occasion").
  The strip's source is the use-case page, itself a registered exercised
  source, so the provenance is real either way.

The `:start-here` table's **ordering** is a judgement call worth stating:
rows are ordered by how many readers each question serves, so the emitter
author's row is last of six. A "start here" table that led with the
rarest actor would be dishonest signposting for the other five. What
fixes U-9 is that the catalog's first screen is six scannable questions
instead of twenty-two audience sentences — and that this actor now also
has direct routes from `README.md` and `docs/README.md` that skip the
catalog entirely.

### Step 3 — the second emitter, and why JSONL

`bin/example-custom-emitter-jsonl` renders the log into a line-delimited
JSON **encounter feed**. The shape was chosen against the actor card:
a hospital-adjacent system with its own message format is far likelier to
ingest line-delimited JSON over a queue than a CSV census, and — the
deciding reason — the first example is *already* flat pipe-delimited
lines, so a CSV would differ from it on no axis that teaches anything. A
one-object-per-**encounter** feed cannot line up with a
one-map-per-**event** log, so it cannot dodge the decisions a real
mapping forces. All three are made visibly, and all three print real
numbers against seed 42:

1. **Two events fold into one record.** `:admission` opens, `:discharge`
   closes. An encounter the run's window left open is emitted
   `"status":"open"` with a null discharge — not dropped, and not handed
   an invented discharge at end-of-run.
2. **Fields the log has that the target lacks: dropped and counted.**
   `:attending`, `:reason`, `:placement`, on 5 records each. `:warm-up`
   is reported present-vs-true.
3. **A field the target wants that the log lacks: null, and said.** No
   absolute time exists anywhere in a log, so `admission_datetime` is
   `null` and the summary says why rather than anchoring to a date
   nobody supplied.

**One draft defect worth recording, because it is the failure mode this
example exists to teach.** The first version counted dropped fields by
*truthiness* (`(if (:warm-up event) inc identity)`) and picked `:warm-up`
and `:citation` as its examples. Against real seed-42 output both came
back **zero** — `:warm-up` is present-but-`false` on every event, and
`:citation` is absent from admissions entirely — so decision 2 rendered
as a row of zeros and taught nothing. The counters now key off
**presence** (`contains?`), and the fields named are ones the log
actually carries. An emitter that conflates "absent" with "present and
false" is exactly the bug this page warns about, and the first draft
committed it.

Determinism: records sorted by `(admitted-t, patient-id)`, fixed key
order per object, no dependence on map or set iteration order — verified
byte-identical across two runs, and pinned as
`test-fixtures/custom-emitter/seed42-p5-encounters.jsonl`. It depends on
`org.clojure/clojure` and nothing else, and it iterates the top-level
vector only, so it does not walk into `:pre-horizon-facts`.

### The hops table, AFTER — the acceptance evidence

Re-walked against the committed tree. Same four targets, same hop
definition (a navigation action taken after reading the surface).

| entry surface | (a) log exists | (b) contract | (c) worked example | (d) version promise |
|---|---|---|---|---|
| `README.md` | **0** | 1 | 1 to the page, 2 to the code | 1 |
| `docs/README.md` | **0** | 1 | 2 | 2 |
| `docs/what-is-this.md` | **0** | 1 | 2 | 1 |
| `docs/manual/00-front.md` (TOC) | **0** | 2 | 2 | 2 |
| `docs/manual/03-a-simulated-hospital.md` | **0** | 1 | 2 | **0** |
| `docs/manual/08-your-own-data.md` | 1 | 2 | 2 | 2 |
| `docs/use-cases.md` | **0** | 2 | 2 | 1 |
| `docs/glossary.md` | **0** | 1 | 1 | **0** |
| `bin/ehrt --help` | 1 | 2 | 2 | 1 |
| `bin/ehrt sim --help` | **0** | 1 | 1 | **0** |
| `bin/ehrt sim run --help` | **0** | 1 | 1 | **0** |
| `docs/cli.md` | **0** | 1 | 1 | **0** |
| `docs/formats.md` (top) | **0** | 1 | 2 | 1 |
| `docs/dev/AUDIENCES.md` | **0** | 1 | 1 | **0** |

**Before: eight of fourteen surfaces had no route at all. After: zero
do.** Twelve of fourteen now state that the log exists on the surface
itself, at hop 0. The two that don't are both correct as they stand:
`bin/ehrt --help` is a group index and reaching `sim` is the hop it
exists to provide, and manual ch. 8 is a chapter about the opposite
direction whose job is to recover a reader in one hop, which it now does.

### Fences honoured

- **`docs/`-root law**: no Polylith vocabulary and no repo history in any
  `docs/` prose added here. `components/...` appears only as literal
  link targets (`event-schema.edn`, the two emitter sources), the
  sanctioned form.
- **No visible ADR tokens in user-path prose**: every ADR reference in
  this session's `docs/` edits is either absent or in `docs/dev/`, which
  `rulings.md#R-dev-docs-not-user-path` exempts. `README.md`'s own
  provenance-code tripwire (`ADR-\d+`, `EXP-…`, `DOC-\d+`, `\bD\d+\b`)
  is green.
- **Generated docs regenerated, never hand-edited**: `make use-cases`,
  `make cli-doc`, `make adr-index` — every generated file in this
  session's diffs came from its generator.
- **`src` scope**: `bases/cli/src/ehrt/cli/help.clj` (a help string) and
  `components/docs-tooling` only, exactly as fenced.
- **`.agents/**` scope**: roadmap row, rulings rows, prompt archive,
  session record. Nothing else.

### Standing rules this session earned

Four rows, each a rule a future session must follow rather than a thing
done once:

- **`R-audience-has-entry-path`** — every segment in
  `docs/dev/AUDIENCES.md` states its own entry path. U-1 is the whole
  argument: a registered audience without a path is a routing gap on
  every surface keyed off that register, and `docs/README.md` says
  explicitly that it is keyed off it.
- **`R-exercised-implies-gated`** — a row in `exercised-sources.edn`
  needs a live `check-entry` case. "A script exists" and "a script is
  proven to teach its page's commands" are different claims, and only the
  second is a gate. U-15.
- **`R-taught-shell-lines-use-expect-eval`** — a taught line needing a
  shell is exercised via `expect_eval`, never `expect 0 bash -c`, which
  the freshness unwrapper cannot read. The second half of U-15's cause,
  and the reason a test alone would not have been enough.
- **`R-count-by-presence-not-truthiness`** — code reporting which fields
  it dropped counts by presence, not truthiness. Earned from this
  session's own draft defect, and general: `contains?` and `if` answer
  different questions about a field that is present and `false`.

### Roadmap

`**[emitter-author-ux]**` lands CLOSED under `## Done`. Disclosed in the
row itself: it was registered CLOSED rather than OPEN-then-closed,
because it arrived as a chat ruling and executed in the same session —
`rulings.md#R-unregistered-request-gets-a-row` satisfied late, and said
so rather than backdated.

`**[exercised-row-gate-closure]**` opens at PRIORITY 9, U-15's successor
and the part this session did **not** close: it fixed the one ungated row
and added the one missing test, but nothing asserts that *every*
registered row has a live freshness case, so the next row added can
repeat the same silence. `rulings.md#R-population-closure` names the
right shape — enumerate the register and diff it — rather than a tenth
hand-written case. The row was rejected twice by
`ehrt.docs-tooling.roadmap-lint-test` before it was acceptable (a
duplicate `PRIORITY 7`, then eight lines against a six-line cap), which
is the row contract ADR-0144 landed doing exactly its job.

### Verification

- **Regression oracle**: `bin/regression-oracle d62ed19 HEAD` →
  **`IDENTICAL: every root's digest matches between d62ed19 and HEAD`**,
  **35 roots per side**, soundness check `IDENTICAL outside the (ns ...)
  form`, `declared-digest-change: no`. The prompt named an oracle move
  from a help-string edit as a STOP condition; it did not move, which
  confirms help text sits outside the oracle's digest surface.
- **Exerciser**: `bin/usecase-custom-emitter` on a clean tree, exit 0,
  `OK (5 steps, 8 invariants)`.
- **Docs gates**: 106 tests / 1,094 assertions green across the
  docsgen-drift, dead-link, footnote, citation, strip-freshness,
  stale-path, structure-currency, invocation-lint, index-completeness and
  cli-tombstone gates; register lints (roadmap, rulings, reading-set
  budget) green; index-completeness and prompt-record-pairing green after
  `bin/close-scaffold`.
- **Reading sets at the close**, all five under budget, no budget moved:
  `:onboarding` 1524/1665, `:corpus` 1774/2045, `:sim` 1220/1405,
  `:judge` 868/1000, `:docs` 681/785. `:onboarding` fell 164 → 141 lines
  of headroom, spent on four rulings rows and two roadmap rows — the only
  set this session touched, and the reason the `docs/**` work was
  budget-neutral is that no reading set carries a `docs/` file outside
  `docs/dev/`.
- **Final `make test`**: recorded in
  `.agents/session-records/2026-08-17-emitter-author-ux.md`.

**DISCLOSED — a real defect in this session's own Step 3 commit, caught by
a gate and fixed before any push.** The first final `make test` returned
`MAKE_EXIT=2` on
`tracked-scripts-are-executable-in-the-index-test`
(`executable_bits_test.clj:55`):

    Tracked script(s) below are not mode 100755 in the git index -- a fresh
    clone (what CI checks out) will see them as non-executable even if your
    own working tree runs them fine (core.fileMode=false hides the mismatch
    locally).
      {:mode "100644", :path "bin/example-custom-emitter-jsonl"}
      {:mode "100644", :path "bin/example-custom-emitter-jsonl-src/emitter_jsonl.clj"}

`chmod +x` had been run on the launcher and it ran fine locally;
`core.fileMode=false` meant git recorded `100644` anyway. Both files are
`100755` for the first example (`bin/example-custom-emitter`,
`bin/example-custom-emitter-src/emitter.clj`) and for
`bin/event-census-src/…/census.clj`, so the convention was unambiguous and
this commit simply broke it. Consequence had it shipped: the exerciser
would have run green locally and failed in CI on a fresh clone — the
precise class this gate exists for, and a mistake no amount of local
verification would have surfaced. Fixed with
`git update-index --chmod=+x` and folded into Step 3's own commit by
amend rather than added as a follow-up, because a fresh clone *at that
commit* would otherwise be broken; nothing had been pushed, so no
published history was rewritten.

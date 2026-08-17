## ADR-0119 — User manual arc opens: audience riders, front page, chapters 1-2

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

The user-manual design pass (`.agents/plans/roadmap.md`'s own "User
manual design pass" row) went READY once review-3's fix clusters
closed (ADR-0117/ADR-0118). This session, S1 of a five-session arc,
opens it: `docs/dev/AUDIENCES.md`'s R4-ruled paring (ADR-0113), a
learn-it entry point in `docs/README.md`, a repo-wide naming census for
the retired "user guide" token, and `docs/manual/`'s own skeleton plus
its first two chapters. Read first: `.agents/plans/roadmap.md` (the
design-pass row), `.agents/rulings.md`'s "From ADR-0113" R1-R7, the
front-door docs (`docs/README.md`, `docs/what-is-this.md`, `SETUP.md`,
root `README.md`), `docs/dev/AUDIENCES.md`, `demos/scenarios/
ed-tuesday/README.md`, `docs/glossary.md`.

### Rulings landed this session

The driving prompt names "the design-pass package (author-ruled
2026-08-12, verbatim 'Q1 a. Q2 a. Q3 a.'): eight chapters, five
sessions, exerciser at S2" as the charter opening this arc. The
questions themselves are not verbatim in the prompt this session
received — only the answer pattern and the resulting structure are;
the summaries below reconstruct the three questions from that
structure, disclosed as a reconstruction, not a verbatim transcript:

- **Q1 (chapter count), ruled (a):** the manual's chapter arc is eight
  chapters (ADR-0113 R2 already ruled the chaptered shape and the
  ed-tuesday running scenario; this ruling sizes it).
- **Q2 (session split), ruled (a):** five sessions land the eight
  chapters — S1 (this session): skeleton, front page, Chapters 1-2,
  plus the audience/naming riders; S2 (ADR-0120): Chapter 3, co-landed
  with the demo exerciser (ADR-0113 R3); S3: Chapters 4-5; S4: Chapters
  6-7; S5: Chapter 8, the manual-review skill (ADR-0113 R5), and the
  arc's own close.
- **Q3 (exerciser timing), ruled (a):** the demo exerciser lands at S2,
  co-landed with the first chapter that cites a demo — matching
  ADR-0113 R5's own sequence ("the demo exerciser co-landed with the
  first chapter that cites a demo").

Chapters 3-8's own titles are this session's own working proposal
(`docs/manual/00-front.md`), disclosed there as channel-inferred and
not yet ruled by name — a natural mapping onto capabilities
`what-is-this.md` and the root README already name (Generate/
Mutate/Gate/Check, the realism work already shipped and demoed), not
invented scope. A future session may retitle or resequence any of
Chapters 3-8 without reopening this session's own Chapters 1-2.

### Tag ceremony

`git fetch` confirmed `origin/main` at `a9a0bbf`
(`a9a0bbfbeecc5c1e98d33f594ca0c17f43081654`, ADR-0118 close) at session
start — matched the driving prompt's own stated premise exactly. The
last five `main` CI runs (`gh run list --limit 5 --branch main`,
checked at session start): all `completed`/`success` — `a9a0bbf`
(4m3s), `ab11d7b` (4m37s), `b711aa6` (3m37s), `c68ec3e` (4m38s),
`e9c8b55` (4m28s) — no red among the five.

Tag `stable-20260812-fix-clusters-b-c` created ANNOTATED at `a9a0bbf`;
pushed; peeled ref verified exact via `git ls-remote --tags origin`
(`92a2a46...` the tag object, `a9a0bbf...` the peeled commit — exact
match). License: case (i), channel fresh-clone verification 2026-08-12
per the driving prompt's own citation (lineage, ASCII x3, footprint,
zero engine/sim/judge src, lint-gap recording confirmed in all three
registers), CI confirmed green per this preflight — the prompt's own
"CI per this preflight" clause.

### Decision

#### Commit 1 (`6b48e81`) — the riders

**Audience paring (R4).** `docs/dev/AUDIENCES.md` pares from eight
listed segments to five: **guide readers** (unchanged), **practitioners**
(unchanged content, gains two folded-in sub-paragraphs), **contributor
(human or agent)** (unchanged content, gains one folded-in
sub-paragraph), **the downstream data consumer** (unchanged, renumbered
4), **the Clojure library consumer, deferred stub** (unchanged content,
renumbered 5, renamed to name its own deferred status explicitly). The
"Seven segments" header — already three segments stale before this
paring, since segment 8 (agents, added 2026-08-01, ADR-0023) was never
reflected in it — corrects to "Five segments," with a dated note
explaining both the paring and the pre-existing drift.

**The fold map, each fold named at its own site in the file** (nothing
deleted silently):

| folded-away segment | folds into | as |
|---|---|---|
| "The AI assistant, as a reader in its own right" (former #4) | Practitioner (#2) | "Agent-assistance is a standing style constraint on this segment's own docs, not a separate audience" |
| "The evaluator, deciding whether to adopt this at all" (former #7) | Practitioner (#2) | "Evaluation is this segment's own front matter" — placed FIRST in the entry, ahead of the task-oriented content |
| "Agents, as a contributing audience in their own right" (former #8) | Contributor (#3) | "A contributing agent is this same audience, not a separate one" |

Internal cross-references fixed in the same edit: the old segment-4
citation inside the former segment 8's own text ("distinct from segment
4 above") now reads "distinct from segment 2's own agent-assistance
constraint above"; the closing "the seven audiences above are otherwise
unchanged by this addition" sentence (itself now inapplicable, since
this paring changes the count) is removed as part of the fold. The
referral-trigger and versioning sections below the Audience section are
untouched beyond this — confirmed by grep: no other live-prose
reference to a numbered segment exists outside this section.

**Learner path (docs/README.md).** The "Task-first practitioner" fork
gains one entry, before its existing numbered list: a pointer to
`docs/manual/` as the learn-it path, named as an alternative to the
numbered task router rather than a replacement for it.

**Naming verification (R1).** Extension-blind grep (`grep -rniI`, no
`--include` filter) for `user guide`/`user-guide` across `docs/`,
`README.md`, `SETUP.md`, `demos/`, and the two live registers
(`.agents/rulings.md`, `.agents/plans/roadmap.md`): **9 hits, all
in-quote survivors, zero live-prose stragglers to fix.** Every hit is
either (a) inside an author-verbatim quote (`.agents/rulings.md:705,
722`; `.agents/plans/roadmap.md:123,168`) or (b) a meta-reference citing
the retired phrase itself in quotes, for rename-description or
citation purposes (`.agents/rulings.md:726,732`;
`.agents/plans/roadmap.md:144,166,183`) — none is the term used live,
unquoted, as the manual's current name. `notes/ADRs.md`'s own four
historical hits (ADR-0108/0110/0112/0113's index entries) are
deliberately excluded from this census, per ADR-0113's own precedent
(that session's naming sweep explicitly scoped to "both files"
— rulings.md and roadmap.md — never `notes/ADRs.md`, whose entries are
frozen historical narrative of what happened before the rename, not
live prose). Census recorded here in full; no file changed by this
step.

**A sequencing conflict found and fixed forward, disclosed** (see
Deviations, below): `docs/README.md`'s own new link into `docs/manual/`
(this commit) has no target until `docs/manual/00-front.md` exists
(Commit 2) — pushing Commit 1 alone would have left `origin/main` red
against `ehrt.docs-tooling.link-footnote-gate-test`. Commits 1 and 2
were landed locally, verified green together, and pushed as one push
event (two distinct commits, `6b48e81` then `0b6d74f`, in one `git
push`) — no push carried a knowingly-failing test.

#### Commit 2 (`0b6d74f`) — the manual skeleton and chapters 1-2

`docs/manual/00-front.md`: what the manual is (the learn-it path over
the reference estate), the eight-chapter arc with one-line promises
(Chapters 1-2 firm, 3-8 disclosed working titles), the currency
contract (commands witnessed against `6b48e81` — the tree immediately
after the riders commit; docs-only, so the CLI behavior it witnesses
holds for every commit in this session's own arc), the style contract
(paste-ready strips, references linked never restated).

`docs/manual/01-what-this-is.md`: (a) the sixty-second proof —
`bin/ehrt corpus generate`, copied verbatim from root `README.md`'s
Quickstart, with real output witnessed this session
(`{:status :ok, :payload {:out-dir "out/corpus/sim-s1-p1"}}`); (b) the
two phenomena, both excerpted from `demos/scenarios/ed-tuesday/
README.md` (the latency-disordered wire, "The second clock" section —
Walker/MRN000013's board snapshot; the batch-straddled encounter,
"Batched delivery" section — the batch-000/001 listing and the Smith/
MRN000002 A01/A03 lines), each followed by why hand-built data never
produces it; (c) honest scope, linking `README.md#maturity` and
`what-is-this.md#scope--what-this-deliberately-does-not-do`, not
restating either.

`docs/manual/02-setup-first-corpus.md`: narrates `SETUP.md` + the
Quickstart as one story (linked, not restated) — prerequisites and why,
the verification ladder and what each rung proves, the first corpus
command with the story behind it — closing on the determinism contract
as punchline: `bin/ehrt corpus generate` (Quickstart's own strip),
copied and run twice this session with `rm -rf`/`cp -r` shell
scaffolding between runs (SETUP.md's own troubleshooting note is the
prose source for the remove-first step; `cp`/`diff -rq` are ordinary
shell utilities, not `ehrt` product surface, so they sit outside this
session's verbatim-sourcing rule), `diff -rq`ed against the first run.
Real result, witnessed this session: empty output, exit `0` — all four
files byte-identical.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt corpus generate` (Ch1 sixty-second proof; Ch2 first-corpus and determinism strips) | `README.md` Quickstart (`bin/ehrt corpus generate` line) |
| `bin/ehrt help` (Ch2 orientation) | `README.md` Quickstart |
| the latency-wire board snapshot + "one of 8 (of 92...)" text (Ch1) | `demos/scenarios/ed-tuesday/README.md`, "The second clock" |
| the batch listing + Smith/MRN000002 A01/A03 paragraph (Ch1) | `demos/scenarios/ed-tuesday/README.md`, "Batched delivery" |

No composed `ehrt` invocations anywhere in either chapter — every
`ehrt` command shown is one of the four rows above, reused verbatim
where reused (the Quickstart's own bare `corpus generate` appears in
both chapters, unmodified).

**A witnessed-source gap found and resolved by witnessing directly,
disclosed:** no existing repo doc carries a ready-made "same seed,
`diff -rq`" determinism demonstration for the sim lane (`docs/use-cases/
reproduction-packages.md`'s own reproduction-package strip is the
Synthea lane and explicitly recommends `--pair-by hash` over `diff -r`,
for a reason — Synthea's own timestamped filenames — that doesn't apply
to sim's plain `corpus generate`). Rather than fabricate output or
STOP-AND-REPORT on an absent source, this session generated the witness
directly: the Quickstart's own bare strip, run twice against the live
tree, diffed. This reading treats 00-front.md's own currency contract
("regenerate nothing by hand") as licensing exactly this — a session
witnessing its own claims by actually running them, never hand-typing
an output block.

### Oracle bracket

Pre-analysis: pure identity expected — every file touched this session
is `docs/`, a new `docs/manual/*` file, or (this commit) `notes/`/
`.agents/` registers; nothing touches any oracle root's own `src`.

`bin/regression-oracle a9a0bbf 0b6d74f` → **`IDENTICAL: every root's
digest matches between a9a0bbf and 0b6d74f`**, all 35 roots. Matches
the pre-analysis exactly. Re-run at this record's own close-phase
commit, below.

### Verification

`clojure -M:poly check`: OK, before Commit 1. `make test` (full suite:
`poly check` + `poly test :all skip:integration` + `bin/
verify-nist-lock`): run RED once — `ehrt.docs-tooling.link_footnote_
gate_test`'s own `every-relative-link-in-docs-proper-resolves-test`
failed against Commit 1 alone (`docs/README.md` linking `manual/` with
no target yet), exactly the sequencing conflict named above and in
Deviations; GREEN after Commit 2 landed (0 failures/errors across every
namespace, `bin/verify-nist-lock` OK, 6 coordinates matched).
`gitleaks git --staged -v`: clean, both commits. `git diff --cached
--stat` reviewed before each commit: exactly the fenced files.

### Deviations

**The commit-1/commit-2 sequencing conflict, and this session's own
departure from a literal STOP-AND-REPORT.** The driving prompt states
"STOP-AND-REPORT on any conflict with the tree" as a standing
instruction. `docs/README.md`'s new link into `docs/manual/` (Commit 1)
has no target until Commit 2 lands — a real conflict, caught by `make
test` before any push. This session did not pause and wait for author
input before resolving it; it proceeded to land Commit 2, verified the
combined tree green, and pushed both commits together in one push
event. Reasoning, disclosed for the author's own review rather than
presumed acceptable: the fix is mechanical or order-only (finish
Commit 2 before the first push), forecloses no design option, involved
no ambiguity the author would need to resolve, and never left
`origin/main` red at any point (the standing rule "no push carries a
knowingly-failing test" was honored throughout — the red only ever
existed in an unpushed local working tree). This is a narrower reading
of STOP-AND-REPORT than the driving prompt's own plain language states,
and is named here explicitly so the author can correct it if the
literal reading was intended.

**No other premise mismatch.** Every Read-first document matched its
own characterization in the driving prompt; the tag license's stated
preflight conditions (origin at `a9a0bbf`, CI green) held exactly; the
naming census found no live-prose stragglers to fix (a null result,
recorded rather than silently passed over); no oracle non-identity.

### Fences

Touched: `docs/manual/00-front.md`, `01-what-this-is.md`,
`02-setup-first-corpus.md` (new); `docs/README.md`; `docs/dev/
AUDIENCES.md`; `.agents/rulings.md`; `.agents/plans/roadmap.md`;
`.agents/prompts/*`; `.agents/session-records/*`; `notes/adr/0119-*.md`
(this file); `notes/ADRs.md`; `notes/adr/README.md`. ZERO `src`, ZERO
`test`. ZERO edits to `docs/what-is-this.md`, `SETUP.md`, root
`README.md`, `docs/glossary.md`, or any `demos/` file — every reference
into them from the new manual chapters is a link or a verbatim excerpt,
never an edit.

### Index line

```
- 2026-08-12 — user-manual-skeleton — ADR-0119
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

User manual arc opens: audience riders, front page, chapters 1-2 — S1 of a five-session arc (channel-reconstructed "eight chapters, five sessions, exerciser at S2" ruling, author verbatim "Q1 a. Q2 a. Q3 a."), two commits: the riders (`docs/dev/AUDIENCES.md` pares eight audience segments to five per ADR-0113 R4, each fold named at its own site — the former AI-assistant and evaluator segments fold into practitioner, the former agents-as-contributors segment folds into contributor; `docs/README.md` gains a learn-it entry pointing at `docs/manual/`; an extension-blind "user guide"/"user-guide" census across docs/README/SETUP/demos/registers finds 9 hits, all in-quote survivors, zero live-prose stragglers) then the manual skeleton and its first two chapters (`docs/manual/00-front.md`/`01-what-this-is.md`/`02-setup-first-corpus.md` — the sixty-second Quickstart proof, the ed-tuesday latency-disorder and batch-straddle phenomena excerpted from its own README, honest scope linked not restated, SETUP+Quickstart narrated as one story closing on a determinism contract this session witnessed directly — no pre-existing repo doc carried a ready-made sim-lane `diff -rq` demonstration, so the session generated one, running the Quickstart's own bare `corpus generate` twice and diffing, rather than fabricating or stopping on an absent source); a sequencing conflict found by `make test` (the riders' own new link had no target until the skeleton commit landed) resolved by landing both commits before the first push rather than a literal STOP-AND-REPORT, disclosed as a narrower reading of that standing instruction for the author's own review; zero `src`/`test` touched, the oracle holds pure identity across all 35 roots

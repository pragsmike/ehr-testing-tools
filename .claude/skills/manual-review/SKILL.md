---
name: manual-review
description: >
  Score docs/manual/ (the ehrt user manual) against an eight-dimension
  quality rubric -- strip executability, reference duplication, anchor
  stability, glossary linkage, running-example continuity, maturity
  honesty, currency against the generated cli.md, and diagram-source
  presence -- each dimension graded pass/warn/fail with file:line
  evidence, landed as a dated report. Use periodically as the codebase
  and manual evolve, or after any session that touches docs/manual/.
  Do not use this to fix findings directly (review discipline: this
  skill produces register rows, never edits) or to review anything
  outside docs/manual/.
license: MIT
compatibility:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - documentation
    - review
    - session-mechanics
  tested-tools:
    - claude-code
---

# Manual Review

Scores the finished `docs/manual/` arc against an eight-dimension
rubric, periodically, as the codebase and the manual evolve out from
under each other. Chartered by the author, verbatim (ADR-0113 R5,
`notes/adr/0113-sim-palgebra-unification.md`): *"Should we devise a manual-review skill, with
scoring rubric, so we can run it periodically as we evolve the codebase
and manual?"* Built and first run at the manual arc's own close
(ADR-0125).

## Why this exists

A narrative doc like `docs/manual/` rots in ways a generated reference
doc (`docs/cli.md`) structurally can't: a flag gets renamed and the
generated reference regenerates correctly while the manual's own prose
still teaches the old name; a chapter links an anchor that a later
heading edit silently breaks; a term gets used before the glossary
entry that would let a reader look it up. Nothing in this repo's own
test suite reads the manual's prose for sense — `clojure -M:poly
check`/`test` verify the code the manual describes, never the
description itself. This skill is that missing check, run by a human
judgment call rather than a `deftest`, on a cadence the author chooses
rather than every push.

## Use this skill when

- Run periodically, as a standing quality check on `docs/manual/`
  independent of any specific chapter session.
- Run after any session that adds, edits, or resequences a chapter,
  to catch a currency or continuity regression the editing session's
  own narrower fence didn't need to notice.

## Do not use this skill when

- The task is writing or editing a chapter itself — that's a manual
  session running its own driving prompt, not this skill.
- The finding needs a fix, not just a score. This skill's own output
  is a dated report with register rows; landing a fix on top of it is
  a separate, later session's own work (review discipline, the same
  reviewer/actor split `code-review`, `repo-review`, and `errata-sweep`
  all already draw in this repo — a review skill diagnoses, it doesn't
  operate).

## The eight dimensions

Each dimension is graded **pass**, **warn**, or **fail**, independently
— a high score on one dimension never offsets a low score on another.
Every grade cites specific `file:line` evidence; a grade with no
citation is not a grade, it's an assertion.

1. **Strip executability.** Every command strip's own witnessed source
   ("Strip source citations, per strip," or the chapter's own inline
   citation for chapters landed before that convention) is either
   **exerciser-covered** (a scenario README's fenced commands, walked by
   `bin/demo-exerciser-<scenario>` / `ehrt.docs-tooling.demo-exerciser-
   fresh`) or **Quickstart-covered** (a line inside README.md's own
   single ` ```sh ` Quickstart fence, walked by `bin/quickstart-demo` /
   `ehrt.docs-tooling.quickstart-fresh-test`). A strip sourced from a
   `docs/use-cases/*.md` page, or from any OTHER fenced block in
   README.md (there is more than one — "What you get" is a separate,
   uncovered fence from "Quickstart"), has no mechanism that re-runs it
   between sessions; it stays correct only because the session that
   last touched its chapter re-witnessed it by hand. Tabulate every
   chapter's own strip sources against these two mechanisms by name; a
   source that is neither is the finding, not a maybe.

2. **No reference duplication.** No chapter restates a flag table
   (`cli.md`'s own per-verb tables), the operator catalog
   (`operators.md`), or any other reference doc's own enumerable
   content as a table or list of its own. Grep every chapter for a
   markdown pipe-table (`^\|`) and classify each hit: a "Strip source
   citations" table (fine, that's citation, not restatement) versus an
   actual restated flag/operator/code table (the violation). Prose that
   *names* a flag or two while explaining a concept is not duplication;
   a table that could instead be a link is.

3. **Anchor stability.** Every `](path#anchor)` link in every chapter
   resolves against the target file's own real headings, computed by
   the GitHub Flavored Markdown slug rule (lowercase; strip everything
   that isn't a letter, digit, space, or hyphen; spaces become
   hyphens; a doubled space — e.g. either side of an em dash — becomes
   a doubled hyphen) — not eyeballed against the rendered page. List
   every anchor link found, the heading it targets, and the computed
   slug, side by side.

4. **Glossary linkage on first-use terms.** A term this workspace's own
   `docs/glossary.md` defines — especially the four terms its own front
   matter flags as colliding with clinical usage (Pathway, Resource,
   Profile, Diagnosis) — gets linked to the glossary at its first use in
   each chapter that uses it, the way Chapter 2 already does for
   *determinism*/*manifest*/*ground-truth log*. Linking a deeper
   reference doc instead (`operators.md`, `site-profiles.md`,
   `formats.md#the-lineage-record`) teaches the mechanism but doesn't
   give a reader who only wants the one-line definition a way to get it
   without leaving the page — that's a distinct gap from having no link
   at all, and both count against this dimension. Walk each chapter's
   own first use of every glossaried term and record whether it links.

5. **Running-example continuity.** `00-front.md`'s own contract: "One
   scenario, not a new one per chapter, so the reader builds one mental
   model of one hospital." Check every chapter for whether `ed-tuesday`
   is the worked example, and where it drops out, name why — a
   deliberate structural reason (e.g. `ed-tuesday` emits HL7v2 only, so
   a FHIR-specific teaching point structurally cannot draw on it) reads
   differently from an unexplained substitution. Both are worth
   recording; only the second is a defect.

6. **Maturity honesty.** No chapter claims more than the root
   `README.md`'s own Maturity table backs for the capability it's
   teaching (`Generate`/`Mutate`/`Intake`/`Gate`/`Check`, each labeled
   there). This dimension is satisfied by NOT overclaiming and by
   pointing to the authoritative table rather than by repeating it
   per-chapter (repeating it would trip dimension 2) — check the
   stronger, narrower thing instead: wherever a chapter's own narrative
   arrives at a genuine functional limit of what it's teaching (not a
   maturity label, an actual "this doesn't check X" or "this defaults
   to Y because Z"), does the chapter disclose it in place, or does the
   limit go unmentioned until a reader hits it themselves?

7. **Currency against the generated `cli.md`.** Regenerate `docs/cli.md`
   (`make cli.md`, or read the tracked copy if regeneration isn't part
   of this run) and spot-check every specific, falsifiable CLI claim a
   chapter makes — a flag's default, whether it's required, what it
   does — against that file's own current text. A chapter that says a
   flag has no default when `cli.md` now shows one is stale; disclose
   exactly which claims were checked (this dimension is not exhaustive
   by construction — state the sample, don't imply full coverage) and
   what each one was checked against.

8. **Diagram-source presence.** Every SVG under `docs/manual/assets/`
   carries a `<!-- Content derived from ... -->` comment (or
   equivalent) in its own first few lines naming what real doc, config,
   or witnessed run it was derived from — the standing rule this arc's
   own diagram doctrine set (`.agents/rulings.md#R-diagrams-derive-from-data`).
   List every SVG and quote its derivation comment; an SVG with no such
   comment is a fail on its own, regardless of how the rest of the
   manual scores.

## Procedure

1. **Read the whole manual first**, `00-front.md` through the last
   landed chapter, before scoring anything — the same discipline
   `review`'s own committee-transcript rubric already states: score
   after reading everything, not incrementally.
2. **Score each dimension independently**, gathering `file:line`
   evidence as you go (grep is usually faster and more reliable than
   re-reading prose for this; use it for anchors, glossary terms, and
   pipe-tables specifically).
3. **Write the dated report** to
   `.agents/plans/<run date>-manual-review-1.md` (increment the
   trailing number on a re-run within the same review generation, per
   this skill's own future scored runs — `-2`, `-3`, ...), with:
   - a per-dimension evidence table (dimension, grade, evidence
     `file:line` citations, one-line rationale)
   - an overall verdict, stated plainly, not averaged into a false
     precision score the way the five-rubric `review` skill's own
     0-15 sum can be — pass/warn/fail dimensions don't sum cleanly
4. **Findings are register rows, never fixes.** This skill's own job
   ends at the report. A finding that wants fixing is a future
   session's own charter, the same review/fix split `code-review` and
   `repo-review` both already draw in this repo.
5. **A fail-grade finding on any dimension STOPs** whatever session
   invoked this skill for a ruling before declaring the manual (or an
   arc landing it) closed — the same STOP discipline this repo's own
   session ceremony applies to any other genuine, unlicensed finding.
   A warn-grade finding does not stop; it's a register row for the
   author's own queue.

## Output

One dated report, `.agents/plans/<run date>-manual-review-1.md`
(or `-N.md` for a later scored run), with all eight dimensions graded,
evidence cited per grade, and an overall verdict.

## Done when

- [ ] The whole manual was read before any dimension was scored.
- [ ] Every one of the eight dimensions has a grade AND cited
      `file:line` evidence — no grade stands alone.
- [ ] The report states what was and wasn't checked for dimension 7
      (currency) rather than implying exhaustive coverage.
- [ ] A fail-grade dimension, if any, stopped the invoking session for
      a ruling rather than being absorbed silently.
- [ ] The report landed at `.agents/plans/<run date>-manual-review-1.md`
      (or the next free `-N`), dated, before the invoking session
      declares anything closed.

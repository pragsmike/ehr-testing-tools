# Audiences

This document maps how this workspace relates to
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide)
and to the people who will encounter one or the other first. It is a
working doc, revisited as the workspace's shape firms up — not a
manifesto. Revised for the unified `ehr-testing` workspace
(`notes/ADRs.md` ADR-0010, 2026-07-29 development-resumption session);
its content is tools' own pre-merge positioning doc (`notes/tools/ADRs.md`
ADR-0008 is where tools itself went public), broadened to cover sim as
well as tools now that both live in one workspace and relate to the
guide identically. Renamed to this filename (2026-08-01, agent-UX
charter capture session, `notes/ADRs.md` ADR-0023) when **agents**
joined the audience register below as an explicit class — the file's
own audience-register identity now outweighs its original working-doc
name; see the ADR for the prior filename this superseded.

## Audience

This is the canonical audience register for this workspace's
user-facing docs — [`docs/README.md`](../README.md) routes readers to
an entry path keyed off these segments, rather than defining its own.
Five segments arrive here with different on-ramps (pared from eight,
2026-08-12, `notes/ADRs.md` ADR-0119, R4 — the header itself had drifted
to "Seven" three segments behind actual count even before this paring;
every segment folded away is named at its fold site below, its real
content relocated rather than deleted):

1. **Guide readers, arriving method-first.** They've read (or are
   reading) the guide's account of corpus construction and conformance
   gating and want to know which tool serves which chapter. They need
   a map from method to capability, not a re-explanation of the
   method.
2. **Practitioners, arriving task-first** — **domain experts** (EHR
   interface analysts, clinical informaticists) and other
   **informaticists** working task-first, comfortable with Python but
   not necessarily Clojure, often agent-assisted (an AI coding
   assistant driving this workspace on their behalf). They need test
   ADT messages, or need to validate a bundle against a profile, and
   found this workspace before the guide. They need task-oriented
   usage docs first, and a pointer to the guide second — once the task
   is done, the "why" behind it is worth reading. `SETUP.md` exists
   for exactly this cohort, largely on Windows 11/WSL2.

   **Evaluation is this segment's own front matter** (folded from the
   former "evaluator, deciding whether to adopt this at all" segment):
   before any task, this cohort may not yet have decided to adopt the
   workspace at all — needs to know what it actually does, what it
   explicitly doesn't, and how mature each part is before committing to
   it. Served today by `README.md`'s maturity table (the actual
   contract with readers, not a formality), its Scope section, and
   [`docs/what-is-this.md`](../what-is-this.md) — read before the
   task-oriented docs above, not instead of them.

   **Agent-assistance is a standing style constraint on this segment's
   own docs, not a separate audience** (folded from the former "AI
   assistant, as a reader in its own right" segment): this cohort works
   agent-assisted by default, and `SETUP.md` hands the whole onboarding
   job to an assistant via a copy-paste prompt — so an agent reading
   these docs on a human's behalf is not an edge case, it's the
   cohort's default path. What that constrains, distinct from a human
   skimmer's own needs: exact, copy-pasteable commands rather than
   descriptions of commands; heading anchors that stay stable across a
   doc's regeneration; and error text that's self-explanatory without a
   human in the loop to interpret it. The CLI help surface (`ehrt
   help`, `ehrt corpus operators`, and the enumerable-options error
   family naming its valid options plus a `run: ehrt help`-style hint)
   is the deliberate serving of this constraint.
3. **Contributor (human or agent).** They need the scope fence up
   front: PRs that add method content (new properties, new correctness
   arguments, new test-plan guidance) belong in the guide, not here —
   this workspace's contribution surface is tool code, not method.

   **A contributing agent is this same audience, not a separate one**
   (folded from the former "Agents, as a contributing audience in their
   own right" segment, added 2026-08-01, agent-UX charter, `notes/ADRs.md`
   ADR-0023) — distinct from segment 2's own agent-assistance
   constraint above, which is an AI assistant reading *user-facing*
   docs on a human's behalf, task-first, at usage time; this is an
   agent *driving a contribution session* — PRs, commits, docs edits,
   migration work — the same work `AGENTS.md` governs for a human
   contributor. Entry points: [`AGENTS.md`](../../AGENTS.md) (the
   primary instruction surface) and `.agents/` (durable session
   context: skills, memory, plans, session-records, prompts). What this
   half of the segment needs, distinct from a human contributor's own
   reading style: small, budgeted surfaces it can read cold every
   session without exhausting context (per-task reading sets,
   `.agents/reading-sets.edn`, forthcoming — charter R-D); indexes over
   prose narrative wherever a directory accumulates more than a
   handful of files; deterministic commands over hand-run procedures
   (`poly ws get:...`, not `poly info`'s pretty-printed prose); and a
   clear current-truth/archive zone boundary — instruction lives in
   `AGENTS.md` and `.agents/`'s current-truth surfaces, provenance and
   history live in archives (`notes/ADRs.md`, `notes/sim/`,
   `notes/tools/`, session records) and are never mistaken for live
   instruction.
4. **The downstream data consumer.** Reads `report.edn`, `manifest.edn`,
   or lineage records — via the `--json` projection or EDN directly —
   and never runs the CLI themselves; a Python or SQL process on the
   other end of a pipeline. [`docs/formats.md`](../formats.md) is the
   reference — the report, check report, manifest, lineage record, and
   the `--json` projection's actual mapping, each field table citing
   its Malli schema and each shape backed by a real captured output
   rather than by the schema alone. [`docs/judge-calibration.md`](../judge-calibration.md)'s
   "Reading this table" and "No-verdict, operationally" sections remain
   the companion for reading verdicts in bulk;
   [`docs/glossary.md`](../glossary.md) is the companion for the
   vocabulary itself — judge, verdict, findings, gate, baseline — and
   is this family's authoritative definition set (R38, this workspace's
   own merge of sim's and tools' pre-merge glossaries).
5. **The Clojure library consumer, deferred stub.** Post-first-release:
   will `require` this workspace's namespaces directly rather than
   shelling out to the CLI. Today's serving is source docstrings only —
   no public-vs-internal demarcation convention exists yet, and cljdoc
   generation rides on Clojars/Maven Central coordinates that don't
   exist before that release (see "Go-public gate vs. first release"
   below). This segment is mostly deferred by design, not neglect.

## The constellation

Three objects, three jobs:

- **The guide** teaches the method — what correctness means for a lossy
  transform, how a corpus should be layered, where validation sits
  relative to semantic checks.
- **The guide's `companion/`** is illustrative code, pinned to the book's
  prose, that exists to be read alongside a specific chapter. It is not
  meant to be depended on as a library.
- **This workspace** is operational tooling with its own release clock.
  It exists to be run — as a CLI, as a library dependency, in CI.

Placement rule for any new artifact, in either project: **if its value
is explanatory, it belongs in the guide or its companion; if its value
is execution, it belongs here.**

## Referral triggers: guide → this workspace

The method demands a runnable capability at specific points. These are
the moments the guide will eventually point here:

- **Corpus construction** (guide ch. 23: generation, mutation,
  provenance manifests) — the generated and mutation layers of the
  corpus need a generator and a mutator; this workspace supplies them
  (`ehrt corpus generate`/`mutate`, and `ehrt sim run` for hospital
  operational traffic specifically).
- **Conformance gates** (guide ch. 25: v2 and FHIR validation as
  structural gates upstream of semantic checks) — this workspace
  supplies the gate wrappers (`ehrt gate v2`/`fhir`). Motivating
  deployment class for this capability: testing an ingestion pipeline
  whose transforms emit HL7 v2 and FHIR JSON, where the consuming team
  brings its own synthetic corpus rather than one generated here — the
  gates and the mutation/intake layers underneath them treat a foreign
  corpus as first-class for exactly this reason.
- **Data sources for the guide's own experiments** — notably, the
  guide's Experiment 3 will eventually consume this workspace's v2
  projector output.

These become register entries and cross-references in the guide
**only after this workspace's first release** (a version tag and
published coordinates — see "Go-public gate vs. first release" below;
public visibility is not the trigger). Nothing in the guide points here
yet, and that's deliberate — the guide should never cite tooling that
doesn't exist yet as though it does.

## Referral triggers: this workspace → guide

- The root README's relationship section.
- Each tool's own docs, once written, linking the chapter that
  motivates its existence.
- A standing norm for issues filed against this workspace: a question
  about method — "what properties should I write?", "what should my
  corpus contain?" — gets answered with a chapter link, not a docs
  expansion. That boundary is what keeps this workspace from slowly
  reabsorbing the guide's scope one helpful paragraph at a time.

## Versioning and citation contract

This workspace cites guide chapters by stable anchors. The guide, once
it starts citing this workspace, will do so by version, with the same
"entry-last-verified" discipline it already applies to its own
reference part. Releases here declare which guide edition's method
they implement ("implements the method as of guide vN").

Each tool carries a maturity label in its own docs — **experimental →
usable → stable**. The guide registers only released, non-experimental
tools; "experimental" is not citable.

## Dogfooding commitments

A reproducibility toolkit that doesn't practice reproducibility on
itself is self-refuting. This workspace commits to:

- A live facts register (`notes/facts-register.md`, `AUTHORS-GUIDE.md`
  §4).
- Reproducibility manifests for everything generated (`corpus.manifest`
  schema v1).
- Pinned dependencies throughout.
- CI that runs offline (`.github/workflows/test.yml`).

## Go-public gate (history — tools' own pre-merge decision)

Before the merge, `ehr-testing-tools` flipped from private to public
once four conditions held: clean licensing (verified dependency
compatibility, an NIST v2-validation licensing finding resolved); at
least one tool at "usable" maturity with honest docs; CI green and
offline; the referral README in shipped form. Recorded here as
history, not as this workspace's own live gate — the workspace itself
(`ehr-testing`) inherited public visibility at the merge, not through
this gate being re-run. Full record: `notes/tools/ADRs.md`
`tools/ADR-0008`.

## Go-public gate vs. first release

Publication and release are different events, and this workspace is
deliberately public before it has released anything. Publication is a
visibility change: the go-public gate above was about whether the repo
(tools, pre-merge) was honest and safe to look at, not about whether
any capability was finished. First release is a separate, later
milestone — a version tag, Clojars/Maven Central coordinates, and the
point at which the guide's own register may cite this workspace (see
"Referral triggers: guide → this workspace" above, which still applies
unchanged: nothing in the guide points here until *release*, not
merely publication). Until first release: no version tag, no published
artifacts, and every maturity label in `README.md` still means what it
says — "usable" is not "stable," and pre-release expectations govern.

## Open decisions

- **Distribution coordinates** — Clojars vs. Maven Central. Decided at
  first release, not before — there's nothing to publish yet and no
  reason to lock in coordinates early (`AGENTS.md`'s own named hole
  H5).

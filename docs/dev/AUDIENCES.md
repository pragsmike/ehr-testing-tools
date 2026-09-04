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

**Segment 7 is this workspace's PRIME audience** (author ruling,
2026-09-04). Documentation serves it first, and the features it needs
are the prominent, easy-to-discover ones. That ruling is additive and
nothing else: no segment here is renumbered, folded, demoted or
deprecated by it, and no capability is removed — every other segment
keeps the number, the entry path and the serving it had the day before.

Seven segments arrive here with different on-ramps (pared from eight,
2026-08-12, `notes/ADRs.md` ADR-0119, R4 — the header itself had drifted
to "Seven" three segments behind actual count even before this paring;
every segment folded away is named at its fold site below, its real
content relocated rather than deleted; grown back to six 2026-08-17,
`notes/ADRs.md` ADR-0146, by the one segment a cold walk found this
register had never carried — segment 6, the emitter author; grown to
seven 2026-09-04 by author ruling — segment 7, the ground-truth QA team,
recorded in `.agents/session-records/2026-09-04-prime-audience.md` rather
than in an ADR, the ruling's own instruction):

1. **Guide readers, arriving method-first.** They've read (or are
   reading) [the guide](https://github.com/pragsmike/ehr-testing-guide)'s
   account of corpus construction and conformance gating and want to
   know which tool serves which chapter. They need a map from method to
   capability, not a re-explanation of the method --
   [`docs/README.md`](../README.md) is where that map starts.
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
   exist before that release (see
   [Go-public gate vs. first release](#go-public-gate-vs-first-release)
   below, which is this segment's entry path until there is a released
   artifact to point at). This segment is mostly deferred by design, not
   neglect.
6. **The emitter author, arriving with a target format in hand.** They
   run a hospital-adjacent system with its own message format and want
   this workspace's simulated traffic in *that* format. Added
   2026-08-17 (`notes/ADRs.md` ADR-0146) because a cold walk of every
   entry surface as this actor found the register had no row for them,
   and therefore neither did any routing surface keyed off it —
   [`docs/README.md`](../README.md) says explicitly that it routes off
   this register rather than defining its own paths, so a missing
   segment here is a missing path everywhere.

   **Distinct from segment 4 in both directions**, which is why folding
   it in there was considered and rejected: segment 4 never runs the CLI
   and *reads what a run produced* (`report.edn`, `manifest.edn`,
   lineage records), while this segment runs `ehrt sim run` once and
   then writes **code** — an emitter — against a contract. Distinct from
   segment 3 too: the code they write lives in their own repo, not this
   one. They are not contributing an emitter here, and none of this
   workspace's contribution surface is involved.

   **Entry path**, stated because a registered audience without one is
   exactly what the walk found: the root
   [`README.md`](../../README.md)'s "Where to start" third branch →
   [`docs/use-cases/custom-emitter-from-the-event-log.md`](../use-cases/custom-emitter-from-the-event-log.md)
   (the path end to end, with two worked emitters) →
   [`docs/formats.md`](../formats.md#the-event-log)'s "The event log"
   (the contract: 28 closed event kinds, per-kind keys, one real example
   each, generated from `event-schema.edn`). The narrative option is the
   manual's [Chapter 3](../manual/03-a-simulated-hospital.md#the-log-underneath-every-message), "The log
   underneath every message".

   What this segment needs, distinct from segment 4's own: a
   **versioned** contract rather than a described shape
   (`:event-schema-version` in every run's manifest, so a log carries
   the version that produced it); a worked example that depends on
   nothing off this repo's classpath, because one that needed our code
   would prove the opposite of what it claims; and an honest answer to
   *"how do I know my emitter is complete"* — the closed 21-kind
   vocabulary, checkable against their own log with
   [`bin/event-census`](../../bin/event-census).

7. **The ground-truth QA team — the prime audience.** They run a
   downstream system of their own — an interface engine, an EHR
   inbound path, a patient index, a scheduling or bed-management
   module — and use this workspace's ground-truth event log as the
   **semantic oracle** they check that system's behaviour against. The
   log is not test *input* they transform; it is the **expected
   answer** their own system is scored against.

   **What they actually do.** They run `ehrt sim run --format
   ground-truth` and derive their own invariants over the world model
   it carries — patients, encounters, appointments, beds — then
   assert those invariants against what their system did with the same
   traffic. They retain corpora as **versioned QA assets**, pinned by
   the four-field provenance tuple (`:seeds`, `:generator`,
   `:stream-scheme`, `:event-schema-version`, plus the `--config` file
   by content hash —
   [`consuming-ground-truth.md`](../consuming-ground-truth.md#provenance)),
   so a corpus that convicted something last quarter still means the
   same thing this quarter. They use `ehrt sim check` as a **reference
   judge** — the invariant catalog this repository asserts over its own
   log, which is also the calibration for what a green run does and
   does not certify — and `ehrt sim mutate` for **controlled
   negatives**: inject one named defect class into the log and prove
   their own checks report that class and nothing else. And all of it
   runs under **automation**, unattended, in someone else's CI — which
   makes exit codes, stable heading anchors and self-explanatory error
   text load-bearing here rather than merely nice.

   **Distinct from segments 4 and 6**, the two it neighbours. Segment 4
   never runs the CLI and reads what a run *produced*; this segment
   runs it repeatedly, on a schedule, and cares about the run contract
   as much as the shape contract. Segment 6 runs it once and then
   writes an **emitter** — code that renders the log into another
   format, with the log as the input to a transform they own. Neither
   framing reaches what this segment needs, which is the standing right
   to say *my system is wrong* on the strength of the log.

   **Entry path**:
   [`docs/consuming-ground-truth.md`](../consuming-ground-truth.md)
   (the run contract — the invocation, which keys make which kinds
   appear at all, what `ehrt sim check` certifies, and the section
   beside it that says what is explicitly *not* warranted) →
   [`docs/formats.md`](../formats.md#read-the-top-level-vector-only)'s
   "Read the top-level vector only" (the counting rule, read before a
   line of consuming code is written) →
   [`docs/future-features.md`](../future-features.md#scale-ergonomics)'s
   "Scale ergonomics" (where the run sizes an automated consumer
   reaches stop being comfortable — stated up front rather than found
   out).

   **The in-tree witness for this actor**, so the register cites
   something rather than inferring a cohort:
   [`test-fixtures/downstream-calibration/PROVENANCE.md`](../../test-fixtures/downstream-calibration/PROVENANCE.md)
   — a downstream QA team's own controlled calibration of `ehrt sim
   run`, their config vendored byte-for-byte with the seed, reference
   date, flags and event-schema version it was measured at. Their
   report itself is channel-held; that file is what this register
   cites, and it is the whole of what it cites.

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

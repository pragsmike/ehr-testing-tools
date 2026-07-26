# Positioning

This document maps how `ehr-testing-tools` relates to
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide) and
to the people who will encounter one or the other first. It is a working
doc, revisited as the repo's shape firms up — not a manifesto. It also
holds the go-public gate this repo set for itself and the record of
walking it: this repo has been public since [ADR-0008](../notes/ADRs.md)
(publication is a visibility change, not a release — see "Go-public gate
vs. first release" below).

## Audience

This is the canonical audience register for this project's user-facing
docs — `docs/README.md` routes readers to an entry path keyed off these
segments, rather than defining its own. Seven segments arrive here with
different on-ramps:

1. **Guide readers, arriving method-first.** They've read (or are reading)
   the guide's account of corpus construction and conformance gating and
   want to know which tool serves which chapter. They need a map from
   method to capability, not a re-explanation of the method.
2. **Practitioners, arriving task-first.** They need test ADT messages, or
   they need to validate a bundle against a profile, and found this repo
   before the guide. They need task-oriented usage docs first, and a
   pointer to the guide second — once the task is done, the "why" behind
   it is worth reading. The first trial cohort (2026-07-24) sharpens this
   segment: EHR domain experts comfortable with Python but not Clojure,
   working agent-assisted (ChatGPT, Codex), largely on Windows 11/WSL2 —
   `SETUP.md` exists for exactly this cohort.
3. **Contributors.** They need the scope fence up front: PRs that add
   method content (new properties, new correctness arguments, new test-plan
   guidance) belong in the guide, not here. This repo's contribution
   surface is tool code, not method.
4. **The AI assistant, as a reader in its own right.** Segment 2's trial
   cohort works agent-assisted by default (ChatGPT, Codex), and
   `SETUP.md`'s step 5 hands the whole onboarding job to an assistant via
   a copy-paste prompt — so an agent reading these docs on a human's
   behalf is not an edge case, it's the cohort's default path. What this
   reader needs, distinct from a human skimmer: exact, copy-pasteable
   commands rather than descriptions of commands; heading anchors that
   stay stable across a doc's regeneration; and error text that's
   self-explanatory without a human in the loop to interpret it. DOC-1's
   CLI help surface (`ehr help`, `ehr corpus operators`, and the
   enumerable-options error family naming its valid options plus a
   `run: ehr help`-style hint) is the first deliberate serving of this
   audience — earlier CLI errors returned a bare `:unknown-command` map
   naming nothing valid.
5. **The downstream data consumer.** Reads `report.edn`, `manifest.edn`,
   or lineage records — via the `--json` projection or EDN directly —
   and never runs the CLI themselves; a Python or SQL process on the
   other end of a pipeline. This repo's own "readable from Python"
   promise (`README.md`) went unbacked by a format reference until
   DOC-3 (2026-07-25): [`docs/formats.md`](formats.md) is now that
   reference — the report, check report, manifest, lineage record, and
   the `--json` projection's actual mapping, each field table citing
   its Malli schema (`judge.report`, `judge.finding`, `corpus.manifest`,
   `lineage`) and each shape backed by a real captured output rather
   than by the schema alone. `docs/judge-calibration.md`'s "Reading
   this table" and "No-verdict, operationally" sections remain the
   companion for reading verdicts in bulk.
6. **The Clojure library consumer.** Post-first-release: will `require`
   this repo's namespaces directly rather than shelling out to the CLI.
   Today's serving is source docstrings only — no public-vs-internal
   demarcation convention exists yet, and cljdoc generation rides on
   Clojars/Maven Central coordinates that don't exist before that
   release (see "Go-public gate vs. first release" below). This segment
   is mostly deferred by design, not neglect.
7. **The evaluator, deciding whether to adopt this at all.** Doesn't yet
   have a task or a method question — needs to know what this repo
   actually does, what it explicitly doesn't, and how mature each part
   is before committing to it. Served today by `README.md`'s maturity
   table (the actual contract with readers, not a formality), its Scope
   section, and
   [`docs/ehr-testing-tools-problem-statement.md`](ehr-testing-tools-problem-statement.md).

## The constellation

Three objects, three jobs:

- **The guide** teaches the method — what correctness means for a lossy
  transform, how a corpus should be layered, where validation sits
  relative to semantic checks.
- **The guide's `companion/`** is illustrative code, pinned to the book's
  prose, that exists to be read alongside a specific chapter. It is not
  meant to be depended on as a library.
- **This repo** is operational tooling with its own release clock. It
  exists to be run — as a CLI, as a library dependency, in CI.

Placement rule for any new artifact, in either project: **if its value is
explanatory, it belongs in the guide or its companion; if its value is
execution, it belongs here.**

## Referral triggers: guide → tools

The method demands a runnable capability at specific points. These are the
moments the guide will eventually point here:

- **Corpus construction** (guide ch. 23: generation, mutation, provenance
  manifests) — the generated and mutation layers of the corpus need a
  generator and a mutator; this repo supplies them.
- **Conformance gates** (guide ch. 25: v2 and FHIR validation as structural
  gates upstream of semantic checks) — this repo supplies the gate
  wrappers. Motivating deployment class for this capability (P5,
  recorded without identifying anyone): testing an ingestion pipeline
  whose transforms emit HL7 v2 and FHIR JSON, where the consuming team
  brings its own synthetic corpus rather than one generated here — the
  gates and the mutation/intake layers underneath them treat a foreign
  corpus as first-class for exactly this reason.
- **Data sources for the guide's own experiments** — notably, the guide's
  Experiment 3 will eventually consume this repo's v2 projector output.

These become register entries and cross-references in the guide **only
after this repo's first release** (a version tag and published
coordinates — see "Go-public gate vs. first release" below; this repo
being publicly visible is not the trigger). Nothing in the guide points
here yet, and that's deliberate — the guide should never cite tooling
that doesn't exist yet as though it does.

## Referral triggers: tools → guide

- This README's relationship section.
- Each tool's own docs, once written, linking the chapter that motivates
  its existence.
- A standing norm for issues filed against this repo: a question about
  method — "what properties should I write?", "what should my corpus
  contain?" — gets answered with a chapter link, not a docs expansion.
  That boundary is what keeps this repo from slowly reabsorbing the
  guide's scope one helpful paragraph at a time.

## Versioning and citation contract

This repo cites guide chapters by stable anchors. The guide, once it
starts citing this repo, will do so by version, with the same
"entry-last-verified" discipline it already applies to its own reference
part. Releases here declare which guide edition's method they implement
("implements the method as of guide vN").

Each tool carries a maturity label in its own docs — **experimental →
usable → stable** — once tool docs exist. The guide registers only
released, non-experimental tools; "experimental" is not citable.

## Dogfooding commitments

A reproducibility toolkit that doesn't practice reproducibility on itself
is self-refuting. This repo commits to:

- A facts register from day one (`notes/facts-register.md`, per
  [ADR-0002](../notes/ADRs.md)).
- Reproducibility manifests for everything generated (`corpus.manifest`
  schema v1, EXP-A4).
- Pinned dependencies throughout.
- CI that runs offline (`.github/workflows/ci.yml`, verified hermetic —
  see [ADR-0008](../notes/ADRs.md)).

## Go-public gate

The repo flipped from private to public when all four of the following
held. [ADR-0008](../notes/ADRs.md) is the record of that decision and
walks each condition against its evidence; this section is now a record
of the gate as designed, not a future condition — read ADR-0008 for how
each one was actually met.

1. **Licensing is clean** — the [ADR-0007](../notes/ADRs.md) MIT target
   is met (ADR-0007 supersedes ADR-0001's Apache-2.0 target; EXP-SBOM's
   compatibility findings were evaluated against "permissive
   open-source distribution" and transfer unchanged), every dependency
   is verified compatible, and EXP-SBOM (see `docs/experiments.md`) has
   resolved, including [F1](../notes/facts-register.md) (NIST
   v2-validation licensing). Artifacts this repo redistributes must be
   fully license-verified; artifacts users fetch at their own initiative
   from an official source ([ADR-0005](../notes/ADRs.md)'s 2026-07-24
   amendment) clear this gate on the weaker
   `:use-permitted--unstated--confirmation-pending` status instead.
2. At least one tool has reached "usable" maturity with honest docs (no
   overclaiming what it does).
3. CI is green and runs offline.
4. The referral README (the relationship section above, in shipped form)
   is in place.

## Go-public gate vs. first release

Publication and release are different events, and this repo is
deliberately public before it has released anything. Publication
(ADR-0008) is a visibility change: the four conditions above are about
whether the repo is honest and safe to look at, not about whether any
capability is finished. First release is a separate, later milestone —
a version tag, Clojars/Maven Central coordinates, and the point at which
the guide's own register may cite this repo (see "Referral triggers:
guide → tools" above, which still applies unchanged: nothing in the
guide points here until *release*, not merely publication). Until first
release: no version tag, no published artifacts, and every maturity
label in `README.md` still means what it says — "usable" is not
"stable," and pre-release expectations govern.

## Open decisions

Dated so staleness is visible; both resolved no later than generator
kickoff.

- **Internal repo organization** — resolved by
  [ADR-0004](../notes/ADRs.md) (2026-07-23): one artifact, one source
  tree, organized by capability (`corpus.*`, `gate.*`, `artifact`, `cli`),
  not by format or tool.
- **Distribution coordinates** (open as of 2026-07-23): Clojars vs. Maven
  Central. Decided at first release, not before — there's nothing to
  publish yet and no reason to lock in coordinates early.

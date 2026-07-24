# Positioning

This document maps how `ehr-testing-tools` relates to
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide) and
to the people who will encounter one or the other first. It is a working
doc, revisited as the repo's shape firms up — not a manifesto.

## Audience

Three segments arrive here with different on-ramps:

1. **Guide readers, arriving method-first.** They've read (or are reading)
   the guide's account of corpus construction and conformance gating and
   want to know which tool serves which chapter. They need a map from
   method to capability, not a re-explanation of the method.
2. **Practitioners, arriving task-first.** They need test ADT messages, or
   they need to validate a bundle against a profile, and found this repo
   before the guide. They need task-oriented usage docs first, and a
   pointer to the guide second — once the task is done, the "why" behind
   it is worth reading.
3. **Contributors.** They need the scope fence up front: PRs that add
   method content (new properties, new correctness arguments, new test-plan
   guidance) belong in the guide, not here. This repo's contribution
   surface is tool code, not method.

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
  wrappers.
- **Data sources for the guide's own experiments** — notably, the guide's
  Experiment 3 will eventually consume this repo's v2 projector output.

These become register entries and cross-references in the guide **only
after this repo's first public release.** Nothing in the guide points
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
- Reproducibility manifests for its own test fixtures, once fixtures
  exist.
- Pinned dependencies throughout.
- CI that runs offline, once CI exists.

## Go-public gate

The repo flips from private to public when **all** of the following hold:

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

Until then: private, no external references, no announcements.

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

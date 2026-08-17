# The ehrt user manual

This is the learn-it path over `ehrt`'s own reference estate
([`docs/`](../) proper) — narrative order, one running scenario
throughout, for a reader who wants the story before the index. It
never restates what a reference doc already says correctly; every
chapter links out rather than duplicating, and the reference docs stay
the thing to bookmark once you already know what you're looking for.
[`docs/README.md`](../README.md) routes here as the practitioner
fork's own learn-it option, beside its task router.

**The one running scenario.** Every chapter that needs a live example
draws on [`ed-tuesday`](../../demos/scenarios/ed-tuesday/README.md) — a
scripted single ED shift, weighted toward the trauma/injury traffic a
real emergency department actually sees, with real inpatient census,
real churn, and (per Chapter 1) both the phenomena that make this
workspace's traffic harder to ignore than hand-built test data. One
scenario, not a new one per chapter, so the reader builds one mental
model of one hospital rather than re-orienting every chapter.

## The eight-chapter arc

Five sessions landed these, in order; the demo exerciser (a mechanism
that runs every scenario README's own fenced commands and asserts they
still play out as written) co-landed with the first chapter that cites
a demo. **All eight chapters are landed — the manual is complete.**
**Resequenced in an earlier session:** Chapters 4–5 pulled forward the
"realism you didn't script" material earlier working titles had placed
at Chapter 7 — pacing and the latency second clock (Chapter 4), then
schedule batching and the batch-straddle case (Chapter 5) — the
featured-placement ruling `.agents/rulings.md` names.[^featured-placement]

1. **What this is** — sixty seconds to a real, working command; two
   phenomena hand-built test data never produces; the honest scope of
   what this workspace does and doesn't do.
2. **Setup and your first corpus** — installing the prerequisites,
   running the verification ladder, generating a real corpus, and
   proving to yourself that it's exactly reproducible.
3. **A simulated hospital** — `sim run` and the ed-tuesday scenario at
   volume, shaping a facility's own local dialect with a site profile,
   scripted versus generative patients, the two spaces (plan versus
   fact) every patient's traffic passes through, and the ground-truth
   event log underneath every message — the published contract you write
   your own emitter against when you need a format this workspace
   doesn't ship.
4. **Time on the wire** — pacing a corpus against its own timestamps
   with `ehrt play`, and the second, independently seeded clock at the
   emitter seam that lets a message's own wire transmit time (MSH-7)
   diverge from its clinical event time (EVN-2) — the mechanism behind
   Chapter 1's own out-of-order board.
5. **Batch delivery** *(featured)* — hourly/nightly BHS/BTS delivery,
   every batch BTS-1 self-verified, taught through one encounter
   (Smith, James, MRN000002) split cleanly across two adjacent,
   individually transport-clean batch files: transport-complete, yet
   clinically half-there either file alone.
6. **Breaking data on purpose** — `ehrt corpus mutate`, choosing an
   operator by the contract you want proven, full lineage provenance,
   and the inject-a-defect-expect-the-matching-finding loop that makes
   a mutant useful rather than merely broken.
7. **Judging** — the three gates (official FHIR, v2 HAPI, v2 NIST),
   structurally upstream of any semantic check; verdict semantics,
   including why `:no-verdict` exists as a distinct, honest third
   answer.
8. **Your own data** — cataloging a corpus you didn't generate
   (content hashes, lineage, the received-date as real-world
   provenance), checking it against your own expectations (golden
   equivalence, a per-file assertion vocabulary), baselining a corpus
   you gate repeatedly, and closing pointers into the reference estate
   for the reader on the other end of the pipeline.

## The currency contract

Every command strip in this manual is copied verbatim from a witnessed
source (this repo's own README Quickstart, a `docs/use-cases/*.md`
page, or a demo README) — never composed for the occasion — and every
captured output shown alongside it was actually run and observed this
session, against commit `6b48e81` for Chapters 1–2 (the tree
immediately after that session's own riders commit; a docs-only tree,
so the CLI behavior it witnesses holds for every commit in that
session's own arc), against Chapter 3's own landing commit for
Chapter 3, and against each of Chapter 4 through Chapter 8's own
landing commit, respectively, for Chapters 4 through 8 — each
chapter's strips carry their own session's fresh witness, never a
stale carry-forward from an earlier chapter's own pass. **The manual's
own currency commit, as of its completion, is Chapter 8's own landing
commit** (this arc's fifth and final session) — nothing in Chapters
1–7 was re-witnessed or edited to land Chapter 8. Nothing here is
regenerated by hand: if a command's real output ever drifts from
what's printed on this page, the page is wrong and gets re-witnessed,
never hand-edited to match a claim.

## The style contract

Strips are paste-ready: copy the fenced block, run it, get what this
page shows (modulo the things this workspace's own determinism
contract doesn't cover — wall-clock-dependent provenance fields, where
named). References are linked, never restated: a fact that lives in
`docs/what-is-this.md`, `docs/formats.md`, `docs/judge-calibration.md`,
or any other reference doc is cited by link here, not copied into a
second, driftable copy. Voice is second person, evidence-anchored — you
run the command, you read the real output, you draw the conclusion;
this manual doesn't ask you to take a claim on faith it could instead
let you witness yourself.

[^featured-placement]: `notes/adr/0112-batch-straddle-recording.md`, "From ADR-0112," "Batch-straddle documentation placements" — the author's own charter, verbatim: "featured prominently in the tool user guide." (Cited as `.agents/rulings.md` until 2026-08-17, when that register became standing-rules-only and this block moved verbatim into the ADR that owns it.)

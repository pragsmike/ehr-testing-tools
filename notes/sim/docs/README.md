# Reading this documentation

This directory documents a simulator that generates realistic,
deterministic hospital traffic — clinically coded, operationally
messy, safely synthetic — for testing EHR systems and integrations.
Different readers need different doors into it. This page is the
doorman: it tells you who you probably are, what to read in what
order, how much to trust each document, and where the receipts are.

**If any term reads wrong or unfamiliar — start with
[GLOSSARY.md](GLOSSARY.md).** It exists for exactly two situations:
you build software and the healthcare words are new, or you know
healthcare and the software words (or our shorthand) are new. It
opens with the words that *collide* — terms like "pathway,"
"resource," and "profile" that mean different things here than in
your home field.

## Who you are, and your reading path

**A clinician, informaticist, or health-IT domain expert asking "is
this clinically and operationally credible?"**
Read [clinical-realities.md](clinical-realities.md) first — it is
the document most likely to convince you we understand your world
(post-mortem event streams, hallway boarding, Babyboy/Babygirl
merges, results pending at discharge), and each entry says how the
phenomenon is modeled and why testers need it. Then
[operational-models.md](operational-models.md) (beds, surge slots,
providers, payers — how the hospital itself is modeled) and
[patient-state-model.md](patient-state-model.md) (the patient
lifecycle and which events are legal when).
[trajectory-computation.md](trajectory-computation.md) is the
one-page answer to "how does a disease actually turn into a
hospital visit here" — script-space (what a disease module says
should happen) versus truth-space (what a capacity-bounded hospital
actually did), computed by two different, structurally separated
mechanisms. If you want to know where the clinical content comes
from and why you should trust it:
[third-party-sources.md](third-party-sources.md) and the validation
program in [problem-statement.md](problem-statement.md).

**A software or integration engineer asking "what would I actually
get, and how does it work?"**
If you want to run this yourself rather than just read about it,
[../SETUP.md](../SETUP.md) is the installation and verification path —
come back here once it's running. Otherwise, read
[problem-statement.md](problem-statement.md) §Black-Box first
(inputs, outputs, guarantees), then
[event-sourcing.md](event-sourcing.md) — the architecture in three
pages, including why HL7v2 feeds are event streams and FHIR
documents are snapshots, which is the observation the whole design
rests on. Then [sim-theory.md](sim-theory.md) with its
[diagram](sim-theory-diagram.md) for the full pipeline,
[trajectory-computation.md](trajectory-computation.md) for the
execution-order walk through how one patient's traffic actually gets
computed (the piece event-sourcing.md and sim-theory.md each hold
only part of), and [demos/](demos/) for real command lines with real
output. The
engine's rules of legality live in
[patient-state-model.md](patient-state-model.md).

**An interface analyst or QA lead asking "how do I make it simulate
*my* hospital?"**
[simulate-your-facility.md](simulate-your-facility.md) is written
directly for you — a one-page site interview, an annotated example
config, and an honest "what can't it do yet" list, no code required.
[site-profiles.md](site-profiles.md) is the reference underneath its
dialect half: local code values, MSH identity, HL7 version literal,
Z-segments — and the guarantee that changing the dialect never
changes the underlying truth. The knobs that *do* change the truth
(ward layouts, surge naming, churn rates) live in
[operational-models.md](operational-models.md).

**A skeptic — a reviewer, a security or compliance function, or
anyone deciding whether to rely on this.**
Go straight to [problem-statement.md](problem-statement.md)'s
Validation & Evidence table: seven claims, each with its proof
strategy, from syntactic validity through no-PHI-by-construction.
Then [../notes/facts-register.md](../notes/facts-register.md) — every
externally checkable claim (versions, licenses, verified codes,
upstream findings) as numbered rows with evidence and dates — and
[research/](research/), an independently sourced review of the
upstream tools' limitations, including where it *tempers* our own
claims. We keep the corrections, not just the endorsements.

**A contributor — human or AI agent.**
Docs are downstream of decisions: `../notes/ADRs.md` is the
reasoning-of-record and outranks anything here that disagrees with
it; `../AGENTS.md` and `../AUTHORS-GUIDE.md` are the working rules;
`../.agents/plans/roadmap.md` is what's landed and what's next. Read
those before editing anything in this directory. If you're wondering
how roughly forty independent sessions with no shared memory ended up
behaving like one engineering culture — session types, the prompt
discipline, the standing rules and where each one came from, the
failure modes they defend against — [way-of-working.md](way-of-working.md)
is that account.

## The map

**Using this simulator, not reading about it: [`../SETUP.md`](../SETUP.md)**
— installation, verification ladder, first-traffic walkthrough. Not
part of this docs/ tree (it's a repo-root doorway for a different
reader), but the first stop if you haven't run anything yet.

Foundations: [problem-statement.md](problem-statement.md) (the
contract: problem, audiences, constraints, black box, validation
program) · [sim-theory.md](sim-theory.md) +
[sim-theory.edn](sim-theory.edn) (the pipeline as a formal resource
theory; the EDN is the source of truth, read via the now/next/want
convention) · [sim-theory-diagram.md](sim-theory-diagram.md) (the
pipeline diagram, mechanically regenerated from the EDN — never
hand-edit it) · [event-sourcing.md](event-sourcing.md) (the
architecture and its scope — what it buys and, honestly, what it
doesn't, including Milestone M6's own "coherence property, tested"
section: how EmitHL7 and EmitState are checked against each other).

Domain models: [operational-models.md](operational-models.md) ·
[patient-state-model.md](patient-state-model.md) ·
[clinical-realities.md](clinical-realities.md) ·
[trajectory-computation.md](trajectory-computation.md) (the
cross-cutting synthesis: how a patient trajectory is actually
computed, script-space versus truth-space) ·
[gmf-interpreter.md](gmf-interpreter.md) (how Synthea-format disease
modules are executed here, including the as-built record of every
deviation from the original design) ·
[gmf-source-model.md](gmf-source-model.md) (its companion: how a real
Synthea module is structured and run upstream, why most of the
current catalog still blocks here, and the ordered unlock ladder for
what would change that).

Configuration & provenance: [simulate-your-facility.md](simulate-your-facility.md)
(the site-interview FAQ, no code) · [site-profiles.md](site-profiles.md)
(the dialect-layer reference underneath it) ·
[third-party-sources.md](third-party-sources.md) (what was mined
from where, and the load-bearing fact that only one runtime
dependency exists).

Process: [way-of-working.md](way-of-working.md) (the meta-process
behind the forty-odd sessions that built this repo — session types,
prompt discipline, standing rules and their origins, the failure modes
they defend against; also, honestly, positioning material — a repo
that can describe its own working discipline this precisely is itself
part of the case for trusting its output).

Evidence & scratch: [research/](research/) ·
[positioning-notes.md](positioning-notes.md) (raw material for a
future positioning doc — argument drafts, not commitments) ·
[demos/](demos/) (CLI-produced traces; also the future log-player's
fixtures).

Reference: [GLOSSARY.md](GLOSSARY.md).

## How much to trust what you read

Not all documents carry the same authority, and pretending otherwise
would waste your time. The classes:

- **Contract**: problem-statement.md. Changes rarely and
  deliberately.
- **Reasoning-of-record**: the ADRs (in `../notes/`). Append-only;
  never silently reverted; outrank everything else on "why."
- **Specs and as-built records**: the domain-model docs. Written
  *before* their milestones, then annotated as built — deviations
  are recorded with reasons, never silent (gmf-interpreter.md §7 is
  the pattern).
- **Generated artifacts**: sim-theory-diagram.md and the `.mermaid`
  file are produced mechanically from sim-theory.edn. Do not
  hand-edit them; regenerate.
- **Catalogs**: clinical-realities.md and the GLOSSARY grow by
  convention (four-part entries; collision-first ordering).
- **Scratch**: positioning-notes.md and research mining notes —
  honest raw material, clearly marked.
- **Practice, not product**: way-of-working.md. Describes how sessions
  work, not what the simulator does; it changes when practice changes
  and never outranks the ADRs on any question of *why* a structural
  decision was made.

Claims are kept true by machinery, not vigilance: checkable facts
carry facts-register rows with verification dates; the pipeline
diagram regenerates from the theory file; every capability lands
with its invariants in the same change; and an independent
consumer (the sibling ehr-testing-tools repo) exercises the output
against its own conformance gate. Where a document says something
the code contradicts, that is a bug — please report it rather than
assuming the doc is aspirational.

## Status

Pre-release; public since 2026-07-27 (ADR-0015), not released — no
version tag, GitHub Release, or Clojars/Maven coordinate exists yet,
that being a separate, later trigger (ADR-0015 decision 4's own
deferred ledger). The pipeline is built end-to-end through the
FHIR/state emitter (M6) — every theory stage but Calibrate is now both
built and property-tested, including the emitter-coherence law itself.
A full documentation alignment pass was promised here as "next" —
**that was the go-public session (2026-07-27)**: cross-references,
counts, and tenses swept against the tree. A narrower public-polish
pass followed the same day: `SETUP.md` (written between the two
sessions) wired in from every doorway that names it, the top-level
README's own worked excerpt re-verified line-by-line against real
demo output (`docs/demos/boarding-transfer/` is the fix for what
didn't survive that check), and this page's own map and status text
brought current. Where a residual gap remains, treat the ADRs and the
theory EDN as tiebreakers, per house convention. This project
is one of three siblings: the
[guide](https://github.com/pragsmike/ehr-testing-guide) teaches the
testing method, the
[tools](https://github.com/pragsmike/ehr-testing-tools) make it
runnable, and this simulator generates the traffic. Conformance-and-
gating terms (judge, verdict, findings, gate, baseline) reconcile
against tools' own authoritative `docs/GLOSSARY.md` as of 2026-07-27;
other terms and concepts shared with the guide still defer to the
guide's fuller treatment, and a crosswalk reconciliation against the
guide remains future work for those — no trigger is named yet, unlike
the deferred items in
[`.agents/plans/roadmap.md`](../.agents/plans/roadmap.md)'s own
ledger.

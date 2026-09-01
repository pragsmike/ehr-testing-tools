# 2026-09-01 — event-stream mutation (P6) implementation 2: breadth

Archived verbatim, per `AGENTS.md`'s session-record ritual (R-A). The
record this prompt drove is
[`2026-09-01-event-stream-mutation-breadth.md`](../session-records/2026-09-01-event-stream-mutation-breadth.md).

---

SESSION: event-stream mutation (P6) implementation 2 — breadth over
the live population, the gap ledger, the lineage sidecar
Repo: pragsmike/ehr-testing-tools, tip (68ed148 or descendant).
Roadmap row P6. RULINGS in force: ADR-0176 Q1-Q9 all (a); spine
precedents (registry :format :event, Q5 set-equality gate, Q6
refusal path); NEW (2026-09-01): Q10(a) — ship operators ONLY where
a real population exists in a generatable log; columns without one
are recorded as POPULATION GAPS (distinct from Q6 catalog gaps:
convictable in principle, unwitnessable today) in a ledger, and the
population work is ROWED as its own priced item, not begun here.
Q11(a) — :expected-findings stays non-emptiness; the vocabulary
cross-check is rowed pending, sim-check.interface is NOT widened.
Q12(a) — ehrt sim mutate gains --lineage PATH, writing the lineage
envelope as an EDN sidecar; absent flag = today's behavior exactly.
RED-BEFORE-GREEN per operator: each lands with its closed-loop
conviction test red first.

READ FIRST: ADR-0176 (esp. §2(iv), which this session CORRECTS);
the spine record's three weigh-ins and two declared debts; the
spine's acceptance test as the parameterized shape.

STEPS (one gate each; full make test per push)
1. POPULATION MEASUREMENT, committed as the ledger's basis: for
   every candidate operator (the derived referential matrix + the
   ADR's three structural), measure its site population over real
   generated logs across the opt-in configs (ed-tuesday, clinic-
   decade, and whichever demos carry :ladders/:siu/:scheduling —
   derive the set). Gate: a table, operator × config → site count,
   commands shown.
2. THE LEDGER + ADR CORRECTION, one docs commit: populated
   operators listed for this session; empty columns entered as
   population gaps with the invariant that would convict them;
   ADR-0176 §2(iv) corrected to the measured truth with a dated
   addendum (ADRs append, never rewrite); the population work
   rowed on the roadmap, priced. Gate: ADR-index green.
3. BREADTH, red-then-green per operator, one commit pair or one
   commit with red shown in the record per repo convention: every
   populated referential operator + each structural operator whose
   finding set check can convict (Q6 refusal + gap record for any
   it can't — note the duplicate-EVENT semantic: the world had two;
   if check has no invariant that fires, that is a Q6 catalog gap,
   not a reason to invent a finding). Gate per operator: the
   closed loop passes — inject, convict EXACTLY the declared set,
   clean log stays clean.
4. Q12(a): --lineage PATH on sim mutate; sidecar EDN carries
   parent identity, operator id, seed, site; flag absent =
   byte-identical to the spine's behavior. Exerciser run in the
   record. Gate: CLI tests + coverage map updated.
5. Docs: help text for the flag; consuming-ground-truth.md touched
   only if step 3 changed what check reports (derive); state-derived
   LAST. Gate: suite green.
6. Push; CI via gh; close marker. Record: final catalog census
   (shipped / population-gapped / catalog-gapped, counts); both
   oracles IDENTICAL over the span (post-run stage — a delta is a
   defect, stop and report); P6 row updated with what remains.
FENCES: no engine/run, emitter, or fold edits; no sim-check
interface widening; no population-generation work beyond
measurement; mutation absent = byte-identical everywhere.
SELF-ARCHIVE: prompt and record in the final push.

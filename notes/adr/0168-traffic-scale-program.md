## ADR-0168 — the traffic-scale program: event-mix doctrine, Q3(b) conversion, person-simulator intent

**Status:** Accepted (author-ruled in the design channel, 2026-08-24;
landed by docs session 2026-08-24).

### Context

ehrt's corpora are clinically load-bearing but operationally sparse: the
largest witnessed single run is ~900 events (ADR-0090), the gated corpora
run 343–407, and the event vocabulary covers the clinical skeleton only. A
known downstream consumer wants high-volume demographic-update traffic
(A08/A31 class). A busy metropolitan hospital generates on the order of
10^5 unique clinical events and 10^5–10^6 delivered HL7 messages per day
(engineering estimate, basis in the program plan), dominated by
non-clinical streams ehrt does not yet model: demographic churn, order/
result status ladders, charges, scheduling, bed management.

Separately, ADR-0163's fix demonstrated the shared-RNG blast-radius
property at small scale: dropping one step reshuffled the whole corpus.
The channel had recorded this as a named limitation (Q3, disposition (a):
defer), with a trigger clause: the first queued generator fix, or the
first corpus target above ~10^4 events, converts the disposition.

### Decision

1. **The classification principle.** Every traffic family is classified
   skeleton or emission by one test: if downstream invariants or later
   messages' content must respect it, it is skeleton (ground truth,
   generated, judged); if it is derivable restatement, it is emission
   (rendered downstream, unjudged). The family-by-family classification
   lives in docs/dev/traffic-model.md, which is doctrine, not plan.

2. **Rulings R-mix-1..7** (register, verbatim): life-arc dynamics bespoke
   (not GMF); family structure in scope incl. pregnancy→delivery;
   geography small and file-drawn, grown modestly; unidentified-arrival
   and delayed-insurance flows in scope; scheduling is state; bed-status
   is state; chatter and fan-out are emission add-ons downstream of the
   fact generators.

3. **Q3(b) is converted from deferred to called-for.** The scale target
   meets the recorded trigger. Per-patient/per-person RNG streams (with
   deterministic derivation for persons created mid-run, e.g. births) and
   the from==to delay-draw skip are prerequisite to the program's
   generator arcs. The shared-RNG limitation stands, on the record, until
   that arc lands; no generator-touching fix should land before it
   without an explicit author ruling accepting a double reshuffle.

4. **A person-simulator component is intended**: a population/life-arc
   process (residence, employment/coverage, households, pregnancy→
   delivery, mortality, identification flows) producing a timed
   demographic-delta stream the engine folds into a demographic timeline;
   sibling to patient-simulator with the same charter discipline; two
   clinical hooks (occupational injury, delivery) flowing person→engine
   only. Shape and open questions in the program plan; its charter is its
   own future ADR.

### Consequences

The generator enters a declared-reshuffle era: arcs 1–3 of the program
plan each move corpus content, ruled deliberately here rather than
discovered per-arc. Provenance gains a stream-version marker at the Q3(b)
boundary so pre- and post-migration corpora are distinguishable on their
face. Emission-side work (arc 4) reshuffles nothing and may proceed
independently once the skeleton contract it consumes is stable.

### Error-ledger note

The magnitude figures above and the clock-time estimates in the plan are
engineering estimates from general knowledge, labeled as such where they
appear (F3 of the landing prompt). Promoting them to fact without the
program plan's named measurements would be the unearned-specificity class
this channel's ledger tracks.

### Landing deltas, disclosed

The payloads this session landed were authored in the design channel and
land verbatim except for four mechanical fixes, each named here rather
than absorbed:

- **The doctrine document landed at `docs/dev/traffic-model.md`, not
  `docs/traffic-model.md`.** Three convergent reasons, none of them a
  judgement call: `ehrt.docs-tooling.link-footnote-gate-test`'s
  `no-visible-adr-token-in-prose-test` scans `docs/**/*.md` with
  `docs/dev/` excluded and would go RED on the doctrine document's own
  bare `ADR-NNNN` tokens (ADR-0102's ruling); `.agents/rulings.md`'s
  `R-two-voices-two-homes` puts maintainer content in dev docs; and
  `docs/README.md`'s own promise that `docs/` proper carries no Polylith
  vocabulary is one this document cannot keep (it names components).
  Relocating preserves the payload prose verbatim, where footnote
  conversion would have rewritten it.
- **This file's headings are demoted one level from the payload.** The
  ADR index is generated (`make adr-index`) from a `^## ADR-(\d{4})\s*—`
  match, so the record title is `##`, not `#`.
- **There was no Q3 row in `.agents/plans/roadmap.md` to update.** The
  shared-RNG limitation was recorded in ADR-0163's own "Blast radius: one
  shared RNG, disclosed" section and was never rowed anywhere. The
  conversion is therefore carried by the arc-1 row, created already
  stating it — the shape ADR-0139 used when its own close found no row to
  flip.
- **The prompt's reading list cited `docs/operational-models.md`**; the
  sibling doctrine document is at `components/sim/docs/operational-models.md`.

Nothing in the payloads was contradicted by the tree. The four claims
most at risk were re-probed before landing and all four hold: churn's six
step types and namespace (`ehrt.sim-engine.churn`, `#{:cancel-admit
:cancel-transfer :cancel-discharge :transfer-in-error :bed-swap :merge}`)
match exactly; ~900 events is the largest single-run figure in the tree
(the larger 4,997 is across eleven corpora, ADR-0141); 343–407 matches
ADR-0163's seed-424242 run and `roadmap.md#ed-tuesday-module-tail-inert`;
and the decide-time whole-log scans the plan proposes to index away are
real (`engine.clj`, patient-scoped by ADR-0164). The `O(n^2)` label on
those scans is the plan's own characterization and is not measured — the
program plan gates it behind the throughput spike rather than asserting it.

See `.agents/session-records/2026-08-24-traffic-scale-program.md`.

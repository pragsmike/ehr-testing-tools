# Traffic-scale program plan

Anchor: ADR-0168. Doctrine: `docs/dev/traffic-model.md`. Status: arcs not
yet commissioned; this document is intention, revisable by ruling.

## Target

Hospital-realistic traffic: a simulated metro-hospital day on the order
of 10^5 skeleton events rendering to ~10^6 delivered messages with
chatter and fan-out. Priority per the patient-simulator charter: traffic
realism, not lifetime realism.

## Arcs, in dependency order

**Arc 1 — stream-partition design ADR (design session, no code).**
Classify every existing draw site patient/world from the tree (churn's
shared-RNG docstring is one named site); per-person stream derivation
incl. deterministic newborn derivation (master seed × parent ID × birth
ordinal class of scheme — births must not reshuffle the world);
from==to delay-draw skip; corpus stream-version marker in provenance;
migration test obligations: locality property test (mutate one patient,
all others byte-identical) + determinism defspec continuity. Output: the
table and scheme, ruled, before any code.

**Arc 2 — person-simulator.** New component, sibling charter discipline
to patient-simulator (front-door scope + gated limitations table).
Bespoke hazard-rate processes (R-mix-1): residence, employment→coverage,
school enrollment, households incl. pregnancy→delivery (R-mix-2),
mortality (interplay with GMF Death states needs its ruling here),
identification flows (R-mix-4). Consumes Persona as t0; produces the
demographic-delta stream. Clinical hooks person→engine only:
occupational injury, delivery. Geography file small, grown modestly
(R-mix-3). Open questions for its charter ADR: newborns as full persons
(channel leans yes); household propagation semantics (whose stream draws
a family move); twins/multiples (lean: v1-excluded, named limitation);
fill-vs-merge identification weighting.

**Arc 3 — engine fold extensions.** Demographic timeline on patient
state; scheduling state (R-mix-5); bed-status cycle (R-mix-6); the new
invariant families (traffic-model.md lists the shapes). Also the O(n^2)
decide-time scan removals (carry order indexes in fold state, the
:result-available pattern) — REQUIRED at 10^5 scale, and a generator
change, so it lands within this era, never before arc 1.

**Arc 4 — emission add-ons (R-mix-7).** Status ladders, DFT charges,
re-statement chatter with config ratios, fan-out/subscriber table;
rides the corpus-player MLLP transport and bed-board slices. Reshuffles
nothing; may proceed once arc 3's skeleton contract is stable.

## Measurements that gate the program

- **Throughput spike (before arc 1 is commissioned as code, may precede
  or accompany the design ADR):** events/sec at 10^3/10^4/10^5 on a
  dense synthetic scenario; confirm scaling exponent; find any second
  quadratic. Retires the estimates below.
- **Post-arc-3 rerun** of the same spike at target scale.
- **Gating policy at scale** needs a ruling when arc 4 lands: full-width
  NIST validation of 10^6 messages is hours-class; sampled/stratified
  gating (full on skeleton, sampled on chatter) is the expected shape.

## Appendix: estimates (labeled, unverified — F3)

Basis: general industry knowledge + engineering arithmetic, 2026-08-24
channel discussion; no probes. Retired by the throughput spike.

- Metro-hospital day: ~10^5–3×10^5 unique clinical events; delivered
  messages 5–20× via fan-out; engines at 500k–2M msgs/day sustained
  10–30/sec.
- ehrt 1M-message day, penny-class, AFTER arcs 1–3: skeleton gen 1–5
  min; chatter expansion 2–5 min; render+fan-out 5–15 min; streaming
  self-check 1–3 min; total ~10–30 min. Full-width NIST gating +30–60
  min parallelized. WITHOUT the arc-3 scan fix: unknown, plausibly
  pathological (the O(n^2) arithmetic alone suggests hours).
- Scale inversion point for Q3(b)'s reviewability argument: ~10^4
  events, where per-patient diffs become the only reviewable unit.

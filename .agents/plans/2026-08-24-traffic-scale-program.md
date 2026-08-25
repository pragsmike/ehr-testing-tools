# Traffic-scale program plan

Anchor: ADR-0168. Doctrine: `docs/dev/traffic-model.md`. Status: arcs not
yet commissioned; this document is intention, revisable by ruling.

## Target

Hospital-realistic traffic: a simulated metro-hospital day on the order
of 10^5 skeleton events rendering to ~10^6 delivered messages with
chatter and fan-out. Priority per the patient-simulator charter: traffic
realism, not lifetime realism.

## Arcs, in dependency order

**Arc 0 — quadratic removals under equivalence proof (ADR-0169).**
Commissioned 2026-08-24 by author ruling S2, one session, AHEAD of arc 1.
Scope: the three site families the throughput spike named — the six
check-side occupancy/churn invariants, the `replay`-per-cancel read, and
the two ADR-0164 citation scans. Nothing draw-affecting; gated by
byte-identical corpora and identical findings at every gated seed,
asserted by tests co-landed with the refactor.

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
invariant families (traffic-model.md lists the shapes).

AMENDED 2026-08-24 by author ruling S1 (ADR-0169). This row read,
verbatim:

> Also the O(n^2) decide-time scan removals (carry order indexes in fold
> state, the :result-available pattern) — REQUIRED at 10^5 scale, and a
> generator change, so it lands within this era, never before arc 1.

That sentence conflated "generator change" with "draw-affecting change".
S1 rules output-identical refactors EXEMPT from the reshuffle-era
constraint; only draw-affecting changes wait for the stream migration.
The quadratic removals are therefore **arc 0's, not arc 3's**, and land
BEFORE arc 1 rather than never before it. What remains here is the
draw-affecting half named above.

Scope AMENDED 2026-08-24 by the throughput spike, which measured the
sites rather than assuming them (session record
`2026-08-24-throughput-spike.md`). Three additions, in measured cost
order — **the check half is the larger of the two quadratics and was
not previously in scope at all**:

- **(a) The check-side quadratic family — 69% of total work at 10^5, and
  a SECOND quadratic distinct from the decide-time scans.** Six of the 29
  invariants are 99.4% of the check phase, and the top four are
  unconditionally O(N×P) — each walks the whole patient population once
  per event: `occupancy-within-capacity` (386.0 s alone, 54.9%, O(N×P×W)
  by its ward loop), `no-double-occupancy` (104.3 s),
  `admitted-occupies-one-slot` (82.2 s),
  `outpatient-patients-occupy-no-bed` (75.0 s). Remedy shape: one
  incremental occupancy index carried through the fold, replacing four
  independent full-population walks. The other two,
  `cancel-references-existing-uncancelled-event` (34.8 s) and
  `no-events-after-merged-terminal` (16.9 s), are O(C×N)/O(M×N) inner
  full-log loops that an index over cancel targets removes.
- **(b) `replay` called from the two reinstating cancel decides — 35.3%
  of generate, the single largest generator-side cost, larger than both
  ADR-0164 scans combined.** `engine.clj:1201` and `:1216` each evaluate
  `(:before (nth (replay ground-truth) idx))`: a full `evolve` fold of
  the entire log, materialising a vector of N maps, to read ONE element
  at an index already in hand. Not a scan — a full re-simulation with
  allocation, once per cancel event.
- **(c) The ADR-0164 scans, CONFIRMED as suspects and quantified** —
  `engine.clj:857` (`:medication-end`, 21.3%) and `:897`
  (`:care-plan-end`, 10.9%), 32.2% of generate combined. ADR-0164 scoped
  these by patient; it did not shorten them. Two further named sites ride
  along: `sim-model/occupancy-board` (8.1%, folds every patient ever
  created because `run`'s `init-world` seeds `:patients` with all of them
  at t=0) and `decide :discharge`'s waiting-boarder `filter` + `sort-by`
  over all patients (~7.9%).

Memory is NOT part of arc 3's problem at the program's stated 10^5
skeleton target: measured live set there is 190 MB against the shipped
JVM's 3.88 GB default ceiling (4.8%). The streaming premise is confirmed
as necessary at 10^6, not at 10^5 — see the appendix.

**Arc 4 — emission add-ons (R-mix-7).** Status ladders, DFT charges,
re-statement chatter with config ratios, fan-out/subscriber table;
rides the corpus-player MLLP transport and bed-board slices. Reshuffles
nothing; may proceed once arc 3's skeleton contract is stable.

## Measurements that gate the program

- **Throughput spike — DONE 2026-08-24**, before arc 1 was commissioned,
  as intended. Measured events/sec at 10^3/10^4/10^5 on a dense synthetic
  scenario; measured the scaling exponent per phase; **a second quadratic
  was found, on the check side, and arc 3's scope is amended above to
  carry it.** Session record `2026-08-24-throughput-spike.md`; the
  appendix below is updated to measured figures.
- **Post-arc-3 rerun** of the same spike at target scale.
- **Gating policy at scale** needs a ruling when arc 4 lands: full-width
  NIST validation of 10^6 messages is hours-class; sampled/stratified
  gating (full on skeleton, sampled on chatter) is the expected shape.

## Appendix: figures

Each entry is labeled MEASURED, PROJECTED or ESTIMATE. Per F3 a figure is
MEASURED only where it was actually measured; nothing interpolated or
extrapolated is promoted to MEASURED.

**Run parameters common to every MEASURED figure below.** 2026-08-24,
penny (WSL2, 6c/12t i7-10750H, 15 GiB, ext4), Ubuntu OpenJDK 21.0.7,
JVM defaults as shipped (`MaxHeapSize` 3.88 GB — `bin/ehrt` sets no JVM
options); host verified quiet before each cell. Dense synthetic scenario,
seed 20260824, `:arrival-gap 2`, `:churn true`, three weighted authored
pathways plus a 1-in-8 vendored-module cohort at `:module-horizon-days
1825`; ~13.3–14.0 events/patient at all three scale points; **every
measured run self-check clean**. Phases timed in-process around
`ehrt.sim-engine.engine/run` and `ehrt.sim-check.check/check-all`
separately — the two calls `ehrt.sim.run/run-command` itself makes.
One warm-up plus two timed runs per cell (three at 10^3). Full detail:
session record `2026-08-24-throughput-spike.md`.

- **MEASURED (2026-08-24) — walls and throughput at the three scale
  points**, figures of record (median of three at 10^3, mean of two
  elsewhere):

  | events | generate | check | total | generate ev/s | check ev/s |
  | --- | --- | --- | --- | --- | --- |
  | 1,001 | 0.509 s | 0.342 s | 0.85 s | 1,966 | 2,925 |
  | 10,232 | 5.072 s | 10.450 s | 15.5 s | 2,017 | 979 |
  | 104,851 | 324.1 s | 711.1 s | **17.3 min** | **324** | **147** |

  Throughput collapses with scale rather than plateauing: 6× down on
  generate and 20× down on check across two decades.

- **MEASURED (2026-08-24) — the scaling exponents.** Log-log slope
  between consecutive points:

  | step | generate | check |
  | --- | --- | --- |
  | 10^3 → 10^4 | **0.989** | **1.471** |
  | 10^4 → 10^5 | **1.786** | **1.814** |

  Generate is indistinguishable from linear across the first decade and
  only reveals its quadratic in the second — a spike that had stopped at
  10^4 would have reported the all-clear. Both phases are
  quadratic-shaped in the dense regime.

- **MEASURED (2026-08-24) — the two-term model.** Solving `T = aN + bN²`
  on the two dense points gives generate `2.151e-4·N + 2.743e-8·N²` and
  check `3.984e-4·N + 6.088e-8·N²` seconds; combined `6.135e-4·N +
  8.831e-8·N²`. It back-predicts the 10^3 point it was not fitted on at
  0.7 s against 0.85 s measured. **The check-side quadratic coefficient
  is 2.2× the generate-side one** — the larger of the two.

- **MEASURED (2026-08-24) — where the time goes at 10^5.** Generate:
  `replay` from the two reinstating cancel decides 35.3%, ADR-0164's two
  citation scans 32.2% combined, `occupancy-board` 8.1%,
  `decide :discharge`'s all-patients `sort-by` ~7.9%,
  `last-uncancelled-index` 5.9% — ~89% whole-log or whole-population
  work. Check: six of 29 invariants are 99.4%, led by
  `occupancy-within-capacity` at 54.9%; the other 23 invariants together
  are 4.1 s of 711. Arc 3's scope list is amended accordingly.

- **MEASURED (2026-08-24) — memory, and the held-whole question.**
  Retained engine result 1.065 KB/event (109.0 MB at 10^5); one
  materialised `replay` vector 0.883 KB/event (90.5 MB). Both **linear** —
  persistent-map structural sharing means the check-side quadratic is in
  time only, never in space. Peak process RSS 1.29–2.18 GB across four
  identical 10^5 runs (the 1.7× spread is GC scheduling, not workload).
  **At 10^5 the live set is 190 MB, 4.8% of the shipped 3.88 GB ceiling —
  memory is not a constraint at the program's skeleton target.**

- **PROJECTED, not measured — 10^6 events on today's generator.** One
  decade's extrapolation of the two-term fit above: generate 7.7 h +
  check 17.0 h = **~24.7 h, of which 99.3% is the quadratic term**. This
  RETIRES the prior estimate's "WITHOUT the arc-3 scan fix: unknown,
  plausibly pathological (hours)" — the shape is confirmed and the
  magnitude is a day, not hours — but it is a projection from measured
  points, never itself a measurement.

- **PROJECTED, not measured — 10^6 with the quadratics removed.** The
  linear terms alone give 215 s generate + 398 s check = **10.2 min**.
  This substantially SUPPORTS the prior "AFTER arcs 1–3" estimate:
  skeleton gen 3.6 min sits inside its 1–5 min, while self-check at
  6.6 min runs over its 1–3 min. Also PROJECTED: live set 1.9 GB, ~48% of
  the shipped default ceiling before GC headroom, with fourteen
  sequential 863 MB replay vectors behind it — **this is where arc 3's
  streaming premise becomes necessary, and it is not necessary at 10^5.**

- **ESTIMATE (unchanged, unverified) — metro-hospital day:** ~10^5–3×10^5
  unique clinical events; delivered messages 5–20× via fan-out; engines
  at 500k–2M msgs/day sustained 10–30/sec. A domain claim about real
  hospitals; nothing here measures it.

- **ESTIMATE (unchanged, unverified) — the phases this spike did not
  touch:** chatter expansion 2–5 min; render+fan-out 5–15 min; full-width
  NIST gating +30–60 min parallelized. The spike measured generate and
  check only; no emission, rendering or gating figure is measured.

- **ESTIMATE (unchanged, unverified) — scale inversion point** for
  Q3(b)'s reviewability argument: ~10^4 events, where per-patient diffs
  become the only reviewable unit.

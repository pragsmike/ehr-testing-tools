# Traffic-scale program plan

Anchor: ADR-0168. Doctrine: `docs/dev/traffic-model.md`. Status: arcs not
yet commissioned; this document is intention, revisable by ruling.

## Target

Hospital-realistic traffic: a simulated metro-hospital day on the order
of 10^5 skeleton events rendering to ~10^6 delivered messages with
chatter and fan-out. Priority per the patient-simulator charter: traffic
realism, not lifetime realism.

## Arcs, in dependency order

**Arc 0 — quadratic removals under equivalence proof (ADR-0169). DONE
2026-08-25** (session record `2026-08-25-arc-0-performance-under-
equivalence.md`): 10^5 cell 17.3 min → 1.81 min, corpus byte-identical at
104,851 events, oracle IDENTICAL and undeclared.
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
- **Post-arc-3 rerun — DONE 2026-08-29** (session record
  `2026-08-29-traffic-scale-close.md`). The same dense scenario at HEAD is
  within 1.5% of arc 0's generate figure across the four payload arcs
  that followed it; a `dense-<N>-v2.edn` carrying the nine opt-in keys was built as the
  program's own baseline and measured beside it; emit and spool were timed
  for the first time. **10^6 was DECLINED on measured memory arithmetic,
  stated in the appendix.** The rerun's own headline is not a wall: at 10^4
  and above the v2 baseline **does not complete its self-check**, and three
  distinct defects are rowed in that record.
- **Gating policy at scale — DONE 2026-08-29**, ruled D1 and shipped as
  `gate v2 --sample-add-ons` (ADR-0175 design (h), arc 4 sweep 2), and
  PRICED in the same record: 2.23 ms/message marginal, 11.1 s fixed, a
  25.2% wall saving for 32.2% fewer files, byte-identical reports across
  two runs, and a ceiling set by the corpus mix (add-ons are a third of an
  arc-4 corpus and none of a pre-arc-4 one). ADR-0175 section 2(h)'s own
  ~5.3 ms/message is 2.4–2.6× pessimistic against that measurement.

**ADR-0168's program is COMPLETE as of 2026-08-29.** All five arcs landed
(0 performance, 1 stream partition, 2 person-simulator, 3 engine fold, 4
emission add-ons) and both gating measurements above are paid. Closed the
de-scaffold way: this note and one roadmap line, no new ADR, no tag.
What the program did NOT achieve, stated here rather than left to be
inferred: **the target itself.** "10^5 skeleton events rendering to ~10^6
delivered messages" is not demonstrated — the largest corpus that renders
at all is 105,214 events to 67,638 messages, and the ratio that would
carry it to 10^6 (measured at 1.05–1.21 msg/event) is an order of
magnitude short of the 5–20× the target assumed from fan-out.

## Appendix: figures

Each entry is labeled MEASURED, PROJECTED or ESTIMATE. Per F3 a figure is
MEASURED only where it was actually measured; nothing interpolated or
extrapolated is promoted to MEASURED.

**Run parameters common to every MEASURED figure DATED 2026-08-24 or
2026-08-25** (the 2026-08-29 figures have their own preamble further
down, and the two are not interchangeable). 2026-08-24, penny (WSL2, 6c/12t i7-10750H, 15 GiB, ext4), Ubuntu OpenJDK 21.0.7,
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

- **MEASURED (2026-08-25, ADR-0169) — the same 10^5 cell AFTER arc 0.**
  Same driver, same config, same seed 20260824, same machine; warm-up plus
  two timed. **104,851 events, byte-identical to the pre-arc-0 corpus**
  (51,680,494 bytes, same SHA-256, generated in two worktrees and
  digested — session record 2026-08-25).

  | phase | before (`d49f1c6`) | after (arc 0) | speedup |
  | --- | --- | --- | --- |
  | generate | 324.1 s | **101.2 s** | 3.20× |
  | check | 711.1 s | **7.26 s** | 97.9× |
  | total | 17.3 min | **1.81 min** | **9.58×** |

  Throughput: generate 324 → **1,036 ev/s**, check 147 → **14,442 ev/s**.
  Retained memory unchanged (109.0 → 109.3 MB); generate's peak heap
  halves (845–941 → 425–439 MB) with the per-cancel `replay` vector gone;
  check's peak heap is unchanged, because its allocation is the 14
  independent `replay` calls arc 0 deliberately did not touch. Disclosed:
  taken at Windows host load 21–30% against the baseline's 4/3/3, which
  biases against the speedup, not for it.

  **The two phases have swapped places.** Check was 69% of this cell and
  the larger of the two quadratics; it is now 6.7%. Further work at this
  scale is generator-side.

  This row does NOT convert either projection below to MEASURED — both are
  10^6 figures and remain projections (per F3, nothing extrapolated is
  promoted). What it does say about the second one: its "10^6 with the
  quadratics removed" arithmetic assumed the linear terms alone, and at
  10^5 the measured post-arc-0 residual came in within 4% of the profile's
  own prediction on the generate side, so the projection's method is now
  corroborated at one decade below its target.

**Run parameters common to every MEASURED figure dated 2026-08-29.**
penny (WSL2, 6c/12t i7-10750H, 15 GiB, ext4), Ubuntu OpenJDK 21.0.7, JVM
defaults as shipped (`MaxHeapSize` 3.88 GB — `bin/ehrt` sets no JVM
options), HEAD `6eb4aa6`, same boot as ADR-0167's post-reboot baseline
(up 4d13h, `wslhost` orphan check clean). Host sampled on the WINDOWS
side at every cell boundary, not once at session start: 17 samples,
`LoadPercentage` 0–17, AC power, High performance. Seed 20260824
throughout; warm-up plus two timed runs per cell, one JVM per run,
`/usr/bin/time -v` around each; figures are the mean of the two timed
runs. **Three series, and they are not interchangeable** — `old` is
`dense-<N>.edn` unchanged (the 2026-08-24 scenario, no opt-in key);
`v2` is that file plus the nine keys the gated corpora carry; `nobed` is
`v2` minus `:bed-cycle`, an ISOLATION series that exists only because the
v2 cells stop completing, never a baseline. Phases timed by rebinding the
six vars `ehrt.sim.run/run-command` itself calls, around one real call to
it — nothing transcribed. Full detail: session record
`2026-08-29-traffic-scale-close.md`.

- **MEASURED (2026-08-29) — the `old` series, the continuity check
  against arc 0.** Same config, same seed, same machine. The corpus MOVED
  (105,214 events against arc 0's 104,851, +0.35%) because arc 1
  repartitioned the streams, so this is two nearly-identical populations
  rather than one population twice.

  | events | modules | generate | check | emit | spool | in-process | messages |
  | --- | --- | --- | --- | --- | --- | --- | --- |
  | 9,956 | 0.32 s | 3.08 s | 1.24 s | 0.97 s | 0.67 s | 6.38 s | 6,405 |
  | 105,214 | 0.32 s | **99.7 s** | **10.45 s** | **7.38 s** | **5.63 s** | **118.9 s** | 67,638 |

  **Arc 0's generate figure has held to within 1.5%** (101.2 s → 99.7 s;
  throughput 1,036 → 1,056 ev/s) across the four payload arcs that
  followed it, on a config that opts into none of it. Check is +44% (7.26 → 10.45 s) — the arcs'
  new invariant families running, not an old one regressing; check is
  8.8% of the cell against arc 0's 6.7%. **Emit and spool are measured
  here for the first time in this program**: 13.0 s together, 11% of the
  cell, both indistinguishable from linear.

- **MEASURED (2026-08-29) — the post-arc-0 exponents, and generate is
  still super-linear.** Log-log slope over measured event counts, second
  decade only (the 10^3 → 10^4 slopes are startup-contaminated and are in
  the record, not here):

  | series | generate | check | emit | spool | persons |
  | --- | --- | --- | --- | --- | --- |
  | old (10^4 → 10^5) | **1.474** | 0.904 | 0.861 | 0.904 | — |
  | v2 (10^4 → 10^5) | **1.635** | (contaminated) | blocked | blocked | **1.061** |
  | nobed (10^4 → 10^5) | **1.641** | (contaminated) | blocked | blocked | **1.076** |

  Arc 0 removed the quadratics it named; **it did not make generate
  linear**, and the arc-3/arc-4 payload raises the exponent further, to a
  figure the two add-on series agree on independently.
  `roadmap.md#performance-residual-sites` is where the remainder lives.
  The check exponents are NOT reported: the 10^5 add-on cells fail their
  self-check, so their check wall includes materialising 0.76M–0.9M
  violation records. The person layer is linear and costs 13% of the v2
  10^5 cell.

- **MEASURED (2026-08-29) — messages per event, the ratio the program was
  commissioned for.** 0.643 on the pre-arc-4 skeleton, **stable across a
  full decade** (9,956 and 105,214 events give the same figure to three
  places); **1.050** with all nine keys on at 10^3 and **1.214** with
  eight of them on at 10^4. So the arc-4 payload is worth **1.63×–1.89×
  the message volume per event**, and the ratio is still climbing at 10^4
  rather than settled. NOT measured, and not extrapolated: the ratio at
  10^5 on any add-on-bearing corpus — both such cells are blocked.

- **MEASURED (2026-08-29) — memory, and where the 10^6 constraint
  actually is.** Retained after generate 1.124 KB/event (118.2 MB at
  105,214), against arc 0's 1.065 — +5.5%, still linear. Peak heap by
  phase at the `old` 10^5 cell: generate 500 MB, check 807 MB, **emit
  987 MB**, spool 577 MB; peak process RSS 1,553 MB. **The binding
  constraint at 10^6 is the emit phase's message vector, not the event
  log** — which is a correction to the entry below: the retained set
  projects to 1.18 GB and fits comfortably, while emit's peak projects to
  9.87 GB against the shipped 3.88 GB ceiling, and peak RSS to 15.5 GB
  against the machine's 15 GiB. The spike never emitted, so this quantity
  had never been measured.

- **MEASURED (2026-08-29) — gating at scale, ruling D1's policy priced.**
  `gate v2` over a spooled corpus, same corpus and same JVM shape at both
  points: 17,908 files full width in **50.96 s**, 12,147 files sampled at
  cap 5 in **38.13 s** (mean of two). That fits **2.23 ms/message
  marginal and 11.1 s fixed**, i.e. ~450 msg/s. The sampled run saves
  **25.2% of wall for 32.2% fewer files** — smaller than the file saving
  because the fixed term, including the header pass over every file, is
  paid either way. **The ceiling is the corpus mix, not the cap**:
  add-ons are 32.3% of an arc-4 corpus and **0% of a pre-arc-4 one**, so
  over the `old` 10^5 corpus `--sample-add-ons 5` gated 67,638 of 67,638
  and saved nothing. Determinism: the two sampled reports are
  BYTE-IDENTICAL (sha256 `d93ca42c…`), which is what "the sample is
  derived, not drawn" is supposed to mean. 10^6 messages full width
  prices at **~34–37 min** on this measurement, against ADR-0175 section
  2(h)'s ~88 min — that section is 2.4–2.6× pessimistic and should be
  read as an upper bound.

- **NOT MEASURED (2026-08-29) — the v2 baseline above 10^3, and why.**
  The program's own baseline config **fails its own self-check at 10^4
  and at 10^5**, so its emit, spool and message figures do not exist at
  those scales and are not estimated. FOUR invariant families go red, all
  first reachable at volume and none reachable by any shipped corpus: a
  reinstating cancel landing during the `:cleaning` leg needs a seventh
  bed transition (2 of 16,322 events at 750 patients); a `:transfer`
  inside an OUTPATIENT encounter allocates a licensed bed (24–25 patients
  at 10^5, and the reason the isolation series blocks too); and two
  single-instance failures, one of them probably the second defect seen
  from the other side and one not characterised at all. Full diagnosis, probed to the event, in the
  session record. **This is the rerun's largest result** and it is a
  negative one: the program's target scale is not currently reachable by
  the configuration the program itself describes.

- **PROJECTED, not measured — 10^6 events on today's generator.** One
  decade's extrapolation of the two-term fit above: generate 7.7 h +
  check 17.0 h = **~24.7 h, of which 99.3% is the quadratic term**. This
  RETIRES the prior estimate's "WITHOUT the arc-3 scan fix: unknown,
  plausibly pathological (hours)" — the shape is confirmed and the
  magnitude is a day, not hours — but it is a projection from measured
  points, never itself a measurement.

  **SUPERSEDED BASIS, 2026-08-29.** It stays PROJECTED (F3: nothing
  extrapolated is promoted) and its basis is now historical: it
  extrapolates a fit taken on `dense-7500.edn`, which is one of three
  configs today and the only one that still runs to completion. On that
  config the generator is no longer "today's" — arc 0 removed both
  quadratics the fit was built from, so this entry describes a generator
  that no longer exists. Read it as the pre-arc-0 record, not as a
  live projection.

- **PROJECTED, not measured — 10^6 with the quadratics removed.** The
  linear terms alone give 215 s generate + 398 s check = **10.2 min**.
  This substantially SUPPORTS the prior "AFTER arcs 1–3" estimate:
  skeleton gen 3.6 min sits inside its 1–5 min, while self-check at
  6.6 min runs over its 1–3 min. Also PROJECTED: live set 1.9 GB, ~48% of
  the shipped default ceiling before GC headroom, with fourteen
  sequential 863 MB replay vectors behind it — **this is where arc 3's
  streaming premise becomes necessary, and it is not necessary at 10^5.**

  **SUPERSEDED BASIS, 2026-08-29**, and CORRECTED on the memory half. It
  stays PROJECTED, and the 10^6 cell that would have converted it was
  DECLINED on this arithmetic: one run projects to ~52.5 min (generate
  49.5 of it, on the measured 1.474 exponent rather than a linear term),
  so warm-up plus two timed is ~2 h 38 min — affordable — but peak heap in
  the emit phase projects to **9.87 GB against the shipped 3.88 GB
  ceiling**, and peak RSS to **15.5 GB against the machine's 15 GiB**.
  The correction: this entry's 1.9 GB live set was the right quantity to
  worry about and the wrong one to be bounded by. Measured retained
  projects to **1.18 GB**, well inside the ceiling — so on the live-set
  question 10^6 fits, and **the message vector is what does not.** The
  streaming premise is confirmed as necessary at 10^6, but it is needed
  at EMIT rather than at generate/check, which the spike could not see
  because it never emitted.

- **ESTIMATE (unchanged, unverified) — metro-hospital day:** ~10^5–3×10^5
  unique clinical events; delivered messages 5–20× via fan-out; engines
  at 500k–2M msgs/day sustained 10–30/sec. A domain claim about real
  hospitals; nothing here measures it.

- **ESTIMATE — the phases the SPIKE did not touch, two of three now
  MEASURED (2026-08-29) and both were pessimistic:** chatter expansion
  2–5 min — still unmeasured as a phase of its own, since chatter is
  planned inside emit and was never timed separately. **render+fan-out
  5–15 min: measured at 7.4 s emit + 5.6 s spool at 10^5, and linear**,
  so one decade on it is ~2 min, an order of magnitude under the low end
  of this estimate. **Full-width NIST gating +30–60 min parallelized:
  measured at 2.23 ms/message SERIAL**, i.e. ~34–37 min for 10^6 messages
  on one core, so the estimate's range is right for a serial run and its
  "parallelized" qualifier is not needed to reach it. The estimate is
  kept rather than deleted, because its chatter third is still an
  estimate.

- **ESTIMATE (unchanged, unverified) — scale inversion point** for
  Q3(b)'s reviewability argument: ~10^4 events, where per-patient diffs
  become the only reviewable unit.

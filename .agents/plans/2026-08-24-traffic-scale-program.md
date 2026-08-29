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

  **SUPERSEDED IN PART, 2026-08-29 (later the same day), by the two
  entries below.** The 10^4 cell is no longer blocked and is MEASURED;
  the 10^5 cell still is, but by ONE defect rather than four, and the
  "probably the same root" and "not characterised" clauses above are
  both retired -- two of the four diagnoses this entry summarises turned
  out to be wrong. This entry keeps its NOT MEASURED label and its date
  because it is a true record of what was known when it was written;
  read it with the two below.

**Run parameters common to every MEASURED figure dated 2026-08-29 (post-fix).**
The preamble above holds unchanged -- same machine, same boot, same JVM, same
driver (`spike/driver2.clj`, six vars rebound around one real `run-command`
call), same seed 20260824, same warm-up-plus-two-timed protocol, same
`dense-<N>-v2.edn` bytes. What differs: **HEAD is `1b4e264`**, not `6eb4aa6`
— the two fixes below it are the reason these cells run at all. Host sampled
on the Windows side at every cell boundary AND every ten seconds in flight
(the boundary series is the one comparable to the entries above): boundary
readings 4/5/12/15, in-flight 81 samples, mean 9, max 51, and the in-flight
sampler is itself a Windows-side process so its readings are an upper bound.
**A FIRST ATTEMPT AT THESE TWO CELLS WAS DISCARDED**, its host reading 52–65:
its 10^4 wall came out 34–39 s against 20.7–21.0 s on the quiet host, a 1.7×
instrument error. Only the quiet-host figures appear here.

- **MEASURED (2026-08-29, post-fix) — the v2 10^4 cell, UNBLOCKED.** The
  cell the entry above reports as blocked now completes its own
  self-check CLEAN, at the same **16,322 events**, and its emit, spool
  and message figures exist for the first time.

  | phase | modules | persons | generate | check | emit | spool | other | in-process | process wall | peak RSS |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | v2, 750 patients | 0.317 s | 3.149 s | 3.596 s | 2.052 s | 2.693 s | 1.642 s | 0.959 s | 12.77 s | **20.74 s** | 713 MB |

  **16,322 events, 19,862 messages, msg/event 1.2169** — the first
  second decade the v2 series has had. The ratio was 1.050 at the v2
  10^3 cell, so **the v2 series climbs 1.050 → 1.217 across the
  decade**, landing on top of the `nobed` isolation series' own 1.214 at
  the same scale. That agreement is worth stating: the bed cycle adds
  events and messages in nearly the same proportion, so it moves the
  ratio hardly at all. Fan-out spooled 2,218 `:adt-feed` and 4,657
  `:bed-feed` re-deliveries beside the 19,862 base messages.

  Against the same cell's BLOCKED figures in the entry above: persons
  3.149 s against 3.156 s and generate 3.596 s against 3.439 s, so the
  fix is not measurable in the generate phase. Check is 2.052 s against a
  parenthesised 2.005 s — and the new figure is a PASSING check where
  the old one was a failing check materialising two violations, so the
  two are not the same measurement even though the numbers nearly agree.

- **STILL BLOCKED (2026-08-29, post-fix) — the v2 10^5 cell, and the new
  reason.** One defect, not four: a churn `:cancel-transfer` landing in
  the same batch as a `:discharge` reinstates `:location` onto an
  already-discharged patient, and a later `:outpatient-visit` inherits
  the bed
  (`roadmap.md#cancel-transfer-reinstates-a-discharged-patient`). The
  offending population fell from 25 distinct patients to 13 (that row
  alone flags 11 at nobed and 12 at v2, sharing 10 -- the two cells'
  sets are not nested) and the violation count from 897,579 to 495,205; `bed-cycle-transitions-are-legal` is
  clean where it fired 16 times. Two single-instance rows also survive,
  both now diagnosed
  (`roadmap.md#ts-3-outpatient-opens-over-an-encounter`,
  `roadmap.md#ts-4-placeholder-unresolved`). Emit and spool therefore
  still never run and **no message figure exists at 10^5 on any
  add-on-bearing corpus** — the gap the entry above names is unchanged.

  The phases that DO run, for what they are worth: persons **39.93 s**,
  generate **163.05 s**, check **(84.84 s)**, process wall **300.9 s**,
  peak RSS 2,278 MB, retained after generate 157.5 MB, peak heap
  generate 804 MB. Event count **171,913** (recovered by the untimed
  shape probe, since a blocked run discards its payload) against the
  close's 171,925 — **12 fewer events, which is exactly the 12 erroneous
  bed-ready transfers the fix removes.** Generate is 163.05 s against
  161.52 s pre-fix, +0.9%, i.e. unmoved. **The check figure is
  parenthesised and is NOT a check measurement**: it is the wall of a
  failing check materialising 495,205 violation maps, and it is lower
  than the pre-fix 88.81 s only because there are fewer of them to
  materialise.

- **STILL BLOCKED (2026-08-29, post-TS-5) — BOTH 10^5 add-on cells, and
  the reasons are now single-instance rows rather than the row that
  owned the mass.** HEAD `c5e5f2b`, same driver, same scratch, same
  seed, warm-up plus two timed.

  | cell | events | persons | generate | check | emit | spool | wall | peak RSS | self-check |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | `nobed` 10^5 | 129,415 | 39.14 s | 129.12 s | 12.88 s | — | — | 188.63 s | 1,677 MB | **BLOCKED (1 violation)** |
  | `v2` 10^5 | 171,835 | 40.26 s | 162.68 s | (70.24 s) | — | — | 282.02 s | 1,703 MB | **BLOCKED (33,952)** |

  `roadmap.md#cancel-transfer-reinstates-a-discharged-patient` is CLOSED
  (`c5e5f2b`) and `outpatient-patients-occupy-no-bed` went **372,123 →
  0** at `nobed` and **495,205 → 33,950** at `v2`. What remains blocks
  each cell ALONE and neither is TS-5-rooted:
  `roadmap.md#ts-4-placeholder-unresolved` is the only violation in the
  whole `nobed` run, and `roadmap.md#ts-3-outpatient-opens-over-an-encounter`
  is the whole of `v2`'s residue — one patient, `PID-000640-f57cb996`,
  producing all 33,950. **The prior entry's reading that TS-5 was "one
  defect, not four" blocking these cells was true about the MASS and
  false about the BLOCK**, and a cell is blocked by one violation
  exactly as hard as by 372,123.

  Generate at `v2` is **162.68 s against 163.05 s** — unmoved, as a
  guard consuming no draws should be. **Event counts MOVE, in opposite
  directions**: 129,407 → 129,415 (+8) at `nobed` and 171,913 → 171,835
  (−78) at `v2`. The rejection substitutes one `:step-rejected` for one
  `:cancel-transfer` and so accounts for none of that; the delta is
  downstream, from beds that are no longer silently held being allocated
  instead. Recovered by the untimed probe, a blocked run discarding its
  payload. **The `nobed` check figure is a real
  measurement** — 12.88 s materialising ONE violation, the first
  essentially-clean check wall this programme has had at 10^5 — while
  the `v2` figure stays parenthesised for the reason the entry above
  gives. **Emit and spool still never run at 10^5 on either corpus, so
  msg/event at 10^5 remains unmeasured**, two sessions after the gap was
  named.

- **STILL BLOCKED (2026-08-29, post-TS-3) — BOTH 10^5 add-on cells, and
  the SAME single row now blocks both.** HEAD `c156690`, same driver,
  same scratch, same seed, warm-up plus two timed.

  | cell | events | persons | generate | check | emit | spool | wall | peak RSS | self-check |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | `nobed` 10^5 | 129,415 | 39.79 s | 125.45 s | 12.71 s | — | — | 185.48 s | 1,478 MB | **BLOCKED (1 violation)** |
  | `v2` 10^5 | 171,864 | 40.45 s | 165.51 s | 17.91 s | — | — | 231.62 s | 1,918 MB | **BLOCKED (1 violation)** |

  `roadmap.md#ts-3-outpatient-opens-over-an-encounter` is CLOSED
  (`c156690`): `admission-only-when-no-open-encounter` goes 1 → **0** and
  `outpatient-patients-occupy-no-bed` **33,950 → 0** at `v2`. What is left
  is one violation in each cell and it is the SAME one —
  `roadmap.md#ts-4-placeholder-unresolved`, `PID-007500-e98926c1` at
  t=37017, in both.

  **The fix fires ONCE in a 424-span population**: 423 compiled encounter
  spans go through the new wrapper at `nobed` with ZERO refused and the
  corpus byte-count IDENTICAL at 129,415 events; 424 at `v2` with exactly
  one refused, moving that corpus 171,835 → **171,864 (+29)**. The +29 is
  downstream and is a declared corpus change: with the second encounter
  never opening, the reinstated inpatient encounter stays open for the
  rest of the run and this config bills restatement chatter against
  open-encounter patient-days. `bin/ground-truth-bracket 11765bb c156690`
  is IDENTICAL over 38 roots, so no shipped corpus moves.

- **MEASURED (2026-08-29, post-TS-4) — BOTH 10^5 add-on cells, UNBLOCKED,
  and the programme's measurement arc is complete.** HEAD `62dd9b3`,
  same driver, same scratch, same seed, warm-up plus two timed, host
  sampled at every cell boundary (1 / 2 / 30, the 30 being the CLOSING
  sample taken after the last run had already finished, so no figure sits
  inside it).

  | cell | events | messages | msg/event | modules | persons | generate | check | emit | spool | other | in-process | wall | peak RSS | self-check |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | `nobed` 10^5 | 129,415 | 165,946 | **1.2823** | 0.333 s | 42.81 s | 135.44 s | 13.60 s | 17.62 s | 11.16 s | 2.68 s | 212.48 s | **232.67 s** | 1,745 MB | **CLEAN** |
  | `v2` 10^5 | 171,864 | 233,286 | **1.3574** | 0.333 s | 39.10 s | 164.58 s | 17.65 s | 21.57 s | 15.09 s | 2.78 s | 246.01 s | **270.37 s** | 1,935 MB | **CLEAN** |

  `roadmap.md#ts-4-placeholder-unresolved` is CLOSED (`62dd9b3`),
  check-side: a merge that absorbs a placeholder inside its own window
  closes that window whatever the cause. **Both event counts are
  UNCHANGED** — 129,415 and 171,864, the identical figures the blocked
  runs carried — which is the identity claim a check-side fix owes and
  gets for free; `bin/ground-truth-bracket 23901f4 62dd9b3` is IDENTICAL
  over 38 roots.

  **EMIT AND SPOOL RUN AT 10^5 ON AN ADD-ON CORPUS FOR THE FIRST TIME**,
  three sessions after the gap was named, and the msg/event figure the
  programme was commissioned for now exists at every decade. Fan-out
  spooled 23,197 `:adt-feed` + 8,097 `:bed-feed` beside `nobed`'s base
  messages and 23,714 + 49,935 beside `v2`'s. **The `v2` check figure is a
  real measurement** for the first time: every previous one was
  parenthesised as the wall of a failing check materialising violation
  maps (TS-6).

  **The completed `v2` series, 10^3 → 10^4 → 10^5.**

  | | events | messages | **msg/event** | persons | generate | check | emit | spool | wall | peak RSS |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | 75 patients | 1,488 | 1,562 | **1.0497** | 0.543 s | 0.432 s | 0.324 s | 0.445 s | 0.222 s | 8.86 s | 339 MB |
  | 750 patients | 16,322 | 19,862 | **1.2169** | 3.149 s | 3.596 s | 2.052 s | 2.693 s | 1.642 s | 20.74 s | 713 MB |
  | 7,500 patients | 171,864 | 233,286 | **1.3574** | 39.10 s | 164.58 s | 17.65 s | 21.57 s | 15.09 s | 270.37 s | 1,935 MB |

  **msg/event climbs 1.050 → 1.217 → 1.357 across two decades and is
  STILL CLIMBING** — it is not a settled ratio and must not be quoted as
  one. Against the pre-arc-4 skeleton's 0.643, flat across its own
  decade, the payload is worth **2.11× the message volume per event at
  10^5**, up from 1.63× at 10^3. The `nobed` isolation series says the
  same with the bed cycle removed: 1.056 → 1.214 → **1.2823**.

  **The exponents over the completed series** (log-log over measured
  event counts; first-decade slopes are startup-contaminated and printed
  for completeness only).

  | phase | 10^3 → 10^4 | 10^4 → 10^5 | 10^3 → 10^5 |
  | --- | --- | --- | --- |
  | persons | 0.734 | **1.070** | 0.901 |
  | generate | 0.885 | **1.624** | 1.251 |
  | check | 0.771 | **0.914** | 0.842 |
  | emit | 0.752 | **0.884** | 0.817 |
  | spool | 0.835 | **0.942** | 0.888 |
  | process wall | 0.355 | 1.091 | 0.720 |

  Generate's 1.624 is 1.635 pre-fix — unmoved, as a check-side change
  must leave it. **Check is SUB-LINEAR at 0.914**, against the close's
  parenthesised (1.610): the close said in so many words that its figure
  was the cost of materialising 0.9M violation maps rather than a
  property of the phase, and this confirms TS-6 by measurement rather
  than by argument. Emit and spool are sub-linear at 0.884 and 0.942.

- **DECLINED (2026-08-29, re-stated on the completed series) — the 10^6
  cell keeps its label** (F3: nothing extrapolated is promoted). One
  decade on the `v2` series' own 10^4→10^5 exponents: persons 460 s,
  generate **6,927 s (1 h 55 min)**, check 145 s, emit 165 s, spool
  132 s — **one run ~2 h 10 min, warm-up plus two timed ~6.5 h**.
  Retained after generate projects to 1.57 GB, inside the 3.88 GB
  ceiling. **Peak heap in EMIT projects to 14.5 GB, 3.7× that ceiling**,
  and peak process RSS to 18.9 GB against a 15 GiB machine. The close's
  central memory finding survives a completed series and sharpens: the
  binding constraint at 10^6 is the MESSAGE VECTOR, not the event log,
  and the margin is 3.7× rather than the 2.5× the `old`-series
  projection gave. Two figures a reader will otherwise infer wrongly —
  generate would be 88% of a 10^6 run's wall, and the heap wall is
  reached in a phase (emit) that could not be measured on an add-on
  corpus at all until today.

  Generate at `v2` is **165.51 s against 162.68 s** (+1.7%) and at `nobed`
  **125.45 s against 129.12 s** (−2.8%) — both inside a two-run mean's
  noise. **The `v2` wall falls 282.02 → 231.62 s (−17.9%), and 50 of
  those 50.4 seconds are the check phase**: 17.91 s against a
  parenthesised (70.24 s), which are not the same measurement — the old
  one materialised 33,952 violation maps and this one materialises ONE.
  **The parenthesis is retired for this cell**, on the ground the entry
  above retired it for `nobed`. **Emit and spool STILL never run at 10^5
  on either corpus, so msg/event at 10^5 remains unmeasured**, three
  sessions after the gap was named.

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

  **RE-STATED AGAIN 2026-08-29 (post-TS-3), and STILL DECLINED.** The
  reason is unchanged and unchangeable by these cells: emit never ran,
  `:emit-peak-heap-mb` is 0.0 in all four timed runs, and the decline was
  on emit peak heap. What the post-TS-3 cells add is the first
  check-phase heap figure at `v2` that is NOT dominated by a violation
  vector — 1,460.2 MB while building ONE violation, against 1,246.6 MB
  building 33,950 — so a clean 10^6 run's check heap can at last be
  projected off a comparable quantity. None of that reaches emit, which
  is the gate.

  **RE-STATED 2026-08-29 (post-TS-5), and STILL DECLINED — the label and
  this note both stand.** The decline was on EMIT peak heap, and the two
  cells above cannot improve that projection for the plainest possible
  reason: **emit never ran**. `:emit-peak-heap-mb` is 0.0 in all four
  timed runs because both cells fail their self-check before emission,
  so the arithmetic that would license 10^6 is exactly as unmeasured as
  it was. Taking 10^6 on the strength of a generate-phase heap that DID
  shrink would be reasoning from the half that was never the constraint.
  What the new figures do offer whoever gets to take it: generate peaks
  at 703.5 MB (`nobed`) / 707.6 MB (`v2`) against the 3.88 GB ceiling,
  and check at 1,081.6 / 1,246.6 MB while building 1 and 33,950
  violations respectively — so a 10^6 run with a CLEAN self-check would
  carry a check-phase heap far below the `v2` figure here, the violation
  vector being what dominates it. None of that reaches emit, which is
  the gate.
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

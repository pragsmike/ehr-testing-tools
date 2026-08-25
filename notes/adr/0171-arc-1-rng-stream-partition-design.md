## ADR-0171 — arc 1: the RNG stream partition, designed from the tree

**Status:** Accepted (design session 2026-08-25, HEAD `c1b996e`, six
rulings open; **ruled by the author 2026-08-25** -- every section-4
recommendation taken, plus the locality ruling section 3 asked for
without lettering. Executed by the arc-1b migration session, one
commit group, one reshuffle.)

**The author's rulings, quoted.** Seven, in the order they were given:

1. **A1** -- *"reuse `engine.clj:225` `mix64` on `(family-tag, id-tag)`,
   unchanged, promoted from private to the sim-engine interface."*
2. **B1** -- *"mix the pair `(parity-index, within-delivery-index)` from
   the start, with `within-delivery-index` pinned at 0 while multiples
   are excluded."*
3. **C1** -- *"emission joins as a fifth family, `:emission`, derived
   like the rest."*
4. **D1** -- *"a top-level `:stream-scheme` string, sibling of
   `:event-schema-version`."*
5. **E1** -- *"FACILITY, a run-scoped family distinct from WORLD."*
6. **F1** -- *"one session. The partition, the `from` = `to` skip, all
   fixture re-pins, the oracle re-baseline and the three docstring
   corrections land together, because every one of them is
   draw-affecting and splitting them spends a second reshuffle."*
7. **LOCALITY option (a)** -- the weakened property section 2(a) says is
   the only true one: the locality assertion runs over the
   **PATIENT-scoped fields** of other patients' events, with the four
   WORLD sites of section 1b excluded **by name** -- `:480`
   (`bed-ready-location`), `:610` (`:transfer-in-error`'s `allocate`),
   `:643` (`:bed-swap`'s partner pick) and `:672` (`:merge`'s partner
   pick), cited at this ADR's own design HEAD `c1b996e`. A test claiming
   total byte-identity would be false at `engine.clj:480`.

The migration session's own findings against these rulings -- including
where a section-3 test obligation could not be red for the reason
section 3 gives -- are in
`.agents/session-records/2026-08-25-arc-1b-stream-partition-migration.md`,
not here: this ADR records the design and the rulings, not the
execution.

### Context

`ehrt.sim-engine.engine/run` constructs exactly one
`java.util.Random` — `engine.clj:1605`, `(Random. ^long seed)` — and
threads that single instance through provider materialization, arrival
staggering, per-patient pathway/module assignment, churn injection, and
every `decide` call for every patient, in one global `[t seq-no]` queue
order (`engine.clj:1707`'s `loop`, `:1728`'s `(decide rng t world
patient-id step)`). Consumption order is therefore *global event order*,
not per-patient order. Changing any draw at any site — adding one,
removing one, reordering two — shifts every later draw for every later
patient. That is Q3 (ADR-0168 decision 3), and
`rulings.md#R-per-person-streams-before-generator-fixes` makes fixing it
prerequisite to the traffic-scale generator arcs.

Arc 0 (ADR-0169) removed both quadratics under an equivalence proof:
the 10^5 cell went 17.3 min → 1.81 min with a byte-identical corpus, so
the generator is fast enough to *run* at the program's target and the
remaining obstacle to arcs 2–4 is not speed but reshuffle. Arc 2 needs
to add newborns mid-run; arc 3 needs to add draws to existing decides;
arc 4 needs to add emission chatter. Under one shared stream each of
those moves every patient in every gated corpus.

**Seed stability is a within-version guarantee** (`notes/sim/ADRs.md`
ADR-0009 decision 1), with `:generator {:version ...}` named as the
cross-version key (decision 2). This arc's stream-scheme marker rides
that policy: the partition is exactly the kind of documented,
once-only consumption change ADR-0009 decision 1 licenses, and the
marker makes the boundary legible on a corpus's face rather than only
in a version string.

Two things already in the tree do most of the design's work, and both
were found by reading rather than assumed:

* **`engine.clj:225` `mix64`** — a splitmix64-constant 64-bit mix of two
  longs, already used by `patient-id-for` (`:234`) to derive a stable
  per-patient identity from `(seed, ordinal)`, and whose own docstring
  already argues this arc's thesis: *"deliberately OFF the seeded RNG
  stream (identity needs no stochastic behavior, only spread across
  seeds — keeping it off the RNG means identity generation adds no new
  draws for sim/ADR-0009's accounting to track)."* The stream-seed
  derivation is the same function applied to the same kind of key.
* **`components/sim/src/ehrt/sim/run.clj:422`** — the emit-latency
  stream is *already* a second, independently constructed
  `java.util.Random`, outside the engine's sealed RNG. The precedent for
  partition exists and ships. Its derivation, however, is
  `(java.util.Random. seed)` — the master seed **verbatim**, so the
  latency stream replays the engine's own first draws. See ruling C.

---

### 1. The draw-site census

Every `.nextInt` / `.nextDouble` / `.nextLong` / `rand-int-in` /
`uniform-choice` and every `Random.` construction under
`components/*/src`, enumerated by `grep -rn` at `c1b996e`. **Scope** is
whose outcome the draw decides:

* **PATIENT** — this patient's own clinical trajectory; no other
  patient's state is read.
* **PERSON** — arc 2's demographic/life-arc layer. **Zero rows today**;
  `persona` is its nearest ancestor and is classified PATIENT below,
  because today it is sampled *at* `:registered` for exactly one patient.
* **WORLD** — reads or writes cross-patient state; the outcome depends
  on, or lands on, someone else.
* **FACILITY** — provider/ward structure, fixed for the run.
* **EMISSION** — rendering-time; never enters ground truth.

#### 1a. Setup phase — `engine/run`, before the loop

| site | what is drawn | scope | draw count | shared engine RNG? |
|---|---|---|---|---|
| `engine.clj:1605` `run` | *constructs* the one `Random` from `:seed` | — | — | it **is** the shared stream |
| `engine.clj:1610` `run` → `sim-model/materialize-providers` | 9 `.nextInt` per provider template (`config.clj:131` `generate-npi`, digits; check digit computed) | FACILITY | fixed (9 × template count) | yes |
| `engine.clj:1615` `run` → arrivals | `(rand-int-in rng 0 arrival-gap)`, one `.nextInt` per patient after the first | WORLD | fixed (`patients` − 1) | yes |
| `engine.clj:1645` `run` → `assign-module` (`:1381`, draw `:1395`) | 1 `.nextDouble` per patient, **always**, override or not | PATIENT | fixed 1/patient; **0 when `:module-assignment` absent** | yes |
| `engine.clj:1623` `run` → `assign-pathway` (`:1349`, draw `:1370`) | 1 `.nextDouble` per patient, **always**, override or not | PATIENT | fixed 1/patient; **0 when `:pathways` absent** | yes |
| `engine.clj:1629` `run` → `churn/inject` | see `churn.clj` row below | PATIENT | data-dependent | yes |
| `engine.clj:225` `mix64` / `:234` `patient-id-for` | **not a draw** — pure mixing of `(seed, ordinal)` | — | 0 | no, by design |

Setup draws happen in patient-ordinal order (`init-queue`'s
`map-indexed`, `engine.clj:1657`), and within a patient in the order
module → pathway → churn (argument evaluation of
`registered-steps-for`, `:1656`).

#### 1b. Decide phase — `engine/decide`, inside the global queue loop

| site | what is drawn | scope | draw count | shared engine RNG? |
|---|---|---|---|---|
| `engine.clj:340` `decide :registered` → `sim-model/persona` | sex, age, birth month/day, given name, surname, place, area code, exchange, subscriber, SSN group/serial, payer | PATIENT | **13**, plus 1 each for `:race`, `:socioeconomic-category`, `:state` when configured — pinned by `persona_test.clj:113/:117` | yes |
| `engine.clj:346` `decide :registered` → `patient-simulator/run-module` | the whole GMF walk (§1c) | PATIENT | **unbounded**, data-dependent on the walk | yes |
| `engine.clj:403` `decide :admission` → `sim-model/allocate` | 1 `.nextInt` bed choice within the reached ladder rung | **WORLD** — the candidate set is `free` against a board built from *every* patient (`facility.clj:44` `occupancy-board`) | 0 or 1: **0** on `force-placement` and **0** on exhausted (`facility.clj:91` docstring) | yes |
| `engine.clj:407` `decide :admission` → `sim-model/choose-attending` | 1 `.nextInt` among ward-eligible providers | FACILITY | fixed 1 | yes |
| `engine.clj:421` `decide :delay` → `rand-int-in rng from to` | delay length in minutes | PATIENT | fixed 1 — **including when `from` = `to`**, where the draw is arithmetically dead (`.nextInt rng 1` always returns 0). This is scheme item (d). | yes |
| `engine.clj:428` `decide :transfer` → `sim-model/allocate` | bed choice | WORLD | 0 or 1 | yes |
| `engine.clj:480` `bed-ready-location` (called from `decide :discharge`, `:509`) | bed choice **for a different patient** — `waiting-id`, the boarder pulled in by this discharge | **WORLD, and the sharpest locality hazard in the tree**: patient A's discharge draws to place patient B | 0 or 1, conditional on the surge/licensed branch at `:479` | yes |
| `engine.clj:610` `decide :transfer-in-error` → `sim-model/allocate` | bed choice | WORLD | 0 or 1 | yes |
| `engine.clj:643` `decide :bed-swap` → `uniform-choice` (`:629`) | 1 `.nextInt` picking the swap partner | **WORLD** | **0 or 1** — guarded by `(seq eligible)`, and `eligible` is computed over the whole patient map | yes |
| `engine.clj:672` `decide :merge` → `uniform-choice` | 1 `.nextInt` picking the merge partner | **WORLD** | **0 or 1**, same `(seq eligible)` guard | yes |
| `engine.clj:708` `decide :order` → `rand-int-in` | turnaround minutes | PATIENT | fixed 1 | yes |
| `engine.clj:711` `decide :order` → `order-profiles/sample-analyte-value` (`:81`, draws `:95` + `:79`) | 1 categorical + 1 uniform per analyte | PATIENT | fixed **2 per analyte**; analyte count is profile data | yes |
| `engine.clj:774` `decide :outpatient-visit` → `uniform-choice` | 1 `.nextInt` among **all** providers, not ward-filtered | FACILITY | fixed 1 | yes |

The eleven remaining `decide` methods take `_rng` and draw nothing:
`:cancel-admit` (`:593`), `:result-followup` (`:757`),
`:outpatient-visit-end` (`:781`), `:procedure` (`:798`),
`:observation` (`:823`), `:diagnostic-report` (`:838`),
`:medication-order` (`:847`), `:medication-end` (`:906`),
`:care-plan-start` (`:940`), `:care-plan-end` (`:949`),
`:cancel-transfer` (`:1295`), `:cancel-discharge` (`:1310`).

#### 1c. Sub-brick draw sites reached through the above

| site | what is drawn | scope | draw count | shared engine RNG? |
|---|---|---|---|---|
| `churn.clj:154` `roll-gap` (draw `:163`) | 1 `.nextDouble` per churn step type per gap, **always**, applicable or not | PATIENT (injected into one pathway) | **6 × (steps + 1)** — data-dependent on pathway length | yes; `churn.clj:16` states this is deliberate |
| `order_profiles.clj:81` `sample-analyte-value` | categorical (`:95`) then uniform (`:79` `uniform-in`) | PATIENT | fixed 2 | yes |
| `persona.clj:217` `persona` (draws `:283`–`:325`) | the 13/16 above | PATIENT | fixed given config | yes |
| `facility.clj:62` `choose` (used by `allocate` `:91`, `choose-attending` `:144`) | 1 `.nextInt`, "regardless of candidate count" | WORLD / FACILITY per caller | 1 per call | yes |
| `config.clj:127` `rand-digit` → `:131` `generate-npi` → `:140` `materialize-providers` | 9 digits per NPI | FACILITY | fixed | yes |
| `gmf_interpreter.clj:309/:310` `rand-int-in`/`rand-double-in` | primitives | — | 1 each | yes |
| `gmf_interpreter.clj:326` `sample-set-attribute-range` | SetAttribute `:range` | PATIENT | 1 | yes |
| `gmf_interpreter.clj:378` `sample-distribution` (`:397`, `:400`, `:406`, `:410`) | UNIFORM / GAUSSIAN (via `probit-approx` `:352`) / EXPONENTIAL / TRIANGULAR | PATIENT | **0 for `:exact`, exactly 1 otherwise** | yes |
| `gmf_interpreter.clj:416` `resolve-time-advance` (`:425`, `:439`, `:440`) | time advance | PATIENT | 1 | yes |
| `gmf_interpreter.clj:933` `weighted-pick-transition` (draw `:942`) | 1 `.nextDouble` | PATIENT | 1 | yes |
| `gmf_interpreter.clj:1086/:1112` `resolve-lookup-table-transition` / `resolve-transition` | via `weighted-pick-transition` | PATIENT | 1 per transition | yes |
| `gmf_interpreter.clj:1266` `emit-and-advance` (`:1301`) | duration | PATIENT | via `sample-distribution` | yes |
| `gmf_interpreter.clj:1319` `vital-sign-extra` (`:1336`) | vital value | PATIENT | 1 | yes |
| `gmf_interpreter.clj:1339` `sample-observation-extra` (`:1358`) | observation value | PATIENT | 1 | yes |
| `gmf_interpreter.clj:1401/:1408` imaging series/instance counts (`:1405`, `:1419`) | counts | PATIENT | 1–2 | yes |
| `gmf_interpreter.clj:1722` `step` (`:1816`, `:1830`, `:1831`, `:2028`, `:2029`) | the walk's own per-state draws | PATIENT | data-dependent | yes |

Every GMF-interpreter row is reached only through
`decide :registered`'s `run-module` call, so the whole GMF walk is one
contiguous, per-patient draw burst inside the shared stream.

#### 1d. Off the engine stream already

| site | what is drawn | scope | draw count | shared engine RNG? |
|---|---|---|---|---|
| `sim/run.clj:422` | `(java.util.Random. seed)` → `emit-hl7/plan-latency` | EMISSION | 1 `.nextDouble` per ground-truth event, always (`emit_hl7.clj:987`) | **no** — a second stream, seeded with the master seed verbatim |
| `emit_hl7.clj:908` `default-providers` | `(java.util.Random. 0)` at namespace load | EMISSION (fallback pool only) | fixed | **no** — a constant, not a run stream |

#### 1e. Out of scope, named so it is not mistaken for a gap

`components/oracle/src/ehrt/oracle/digest.clj` builds its own
`Random`s — `:126` (persona per root), `:134` (a `.nextLong` mixer),
`:150`/`:151` (per-root module walks). These are the regression
oracle's own fixture harness, not the run path; no shipped project
depends on `components/oracle` (AGENTS.md, Components). **Out of scope
for the partition.** `patient_simulator/census.clj:344`/`:364`/`:367`
is the same class — a dev-time module-census tool with its own
`mixed-seeds` mixer, not a run.

#### 1f. Where the session prompt and the tree disagree

Per the fix-forward-with-disclosure constraint (ADR-0001 R10), stated
rather than described around:

1. **There is no `pick` in `engine.clj`.** The prompt's "`pick` (:631)"
   is `uniform-choice`, defined at `:629` with its draw on `:631`. The
   line is right; the name is not. A separate private `weighted-pick`
   does exist (`:1329`), but it takes an **already-consumed** draw and
   makes none of its own.
2. **`decide :delay` is at `:414`, and its draw is at `:421`.** The
   prompt's `:414` is the `defmethod` line, not the draw line. The
   claim it carries — that `rand-int-in` draws even when `from` = `to`
   — is confirmed: `:421` calls it unconditionally.
3. **`rand-int-in` is defined at `:220`**; `:223` is its body. Both
   citations are used below in the form the tree supports.
4. **The two always-consume-one-draw sites are not decides.** They are
   `assign-pathway` (`:1349`) and `assign-module` (`:1381`), called from
   `run`'s setup, with docstrings and draws spanning `:1360`–`:1395` as
   the prompt says.
5. **`decide :discharge` draws, and the census must say so.** A
   mechanical pass over `defmethod` bodies alone reports `:discharge` as
   draw-free; its draw lives in the helper `bed-ready-location`
   (`:447`, draw `:480`) defined between `:transfer` and `:discharge`.
   This is the row the locality property test in §3 is most exposed to,
   so a census that missed it would have designed the wrong test.
6. **`:seeds` cannot hold the marker as typed.**
   `provenance/manifest.clj:96` declares `[:seeds [:map-of :keyword
   :int]]` — **`:int` values only**. A `:stream-scheme
   <version-keyword>` entry does not validate. See ruling D.
7. **`:seeds` already carries two incompatible vocabularies.** sim
   writes `:seeds {:primary seed}` (`sim/manifest.clj:84`); corpus
   writes `:seeds {:master seed :clinician clinician-seed}`
   (`corpus/generate.clj:307`), where `:clinician` is **Synthea's**
   `-cs` flag (`generate.clj:113`), not a sim stream. The prompt's
   ":master" is corpus's key, not sim's.
8. **`docs/dev/simulator-architecture.md:157` cites the RNG-path law as
   `.agents/rulings.md`, "Measurements sample the claimed population,
   standing," AR-RL2-2.** No row by that title exists; the live slug is
   `rulings.md#R-measure-claimed-population` (ADR-0093). Recorded, not
   fixed here — this ADR touches no doc outside `notes/adr/`.
9. **`emit_hl7.clj:962` cites `engine.clj:1165-1183` for
   `assign-pathway`'s worked example.** After arc 0, `assign-pathway` is
   at `:1349`. A line citation into a live source file that nothing
   gates — the exact species ADR-0170 named. Recorded, not fixed here
   (the fence forbids touching `components/*/src`).

---

### 2. The scheme

#### (a) Stream families

Four families, matching the census's four live scopes:

| family | seeded per | covers |
|---|---|---|
| `:patient` | patient-id | §1b PATIENT rows, all of §1c's GMF walk, churn injection, persona, order turnaround and analytes, the `:delay` draw |
| `:person` | person-id | arc 2's life-arc processes. **Empty today**, declared now so arc 2 adds rows rather than a family |
| `:world` | the run | arrivals, and every cross-patient decision: all four `allocate` calls, `bed-ready-location`, `bed-swap`, `merge` |
| `:facility` | the run | `materialize-providers`, `choose-attending`, `:outpatient-visit`'s uniform provider pick |

**The WORLD family is not a residue bucket; it is the design's whole
difficulty.** Bed allocation, bed-swap partner choice, merge partner
choice and the bed-ready hand-off all read state that other patients
wrote. Their draw *counts* are conditional on the population
(`(seq eligible)`, `(seq home-licensed)`), so no per-patient stream can
own them without making one patient's draw count depend on another's
state — which is precisely the coupling ADR-0009's rejected option (b)
already refused. Giving them their own single world stream keeps that
coupling in one place and makes it nameable, but it does **not** make
them local: a locality test must therefore assert what §3 states, not
"nothing else moves at all."

#### (b) Derivation

```
stream-seed(family, id) = mix64(mix64(master, family-tag), id-tag)
```

using `engine.clj:225` `mix64` unchanged — already in the tree, already
splitmix64-constant, already the function `patient-id-for` trusts for
exactly this shape of key. `family-tag` is a fixed small long per
family (a compile-time constant table, never `(hash keyword)`, whose
value is JVM-stable but not *specified*). `id-tag` is the patient's
arrival ordinal for `:patient` (the same key `patient-id-for` already
uses), and `0` for the run-scoped families.

**Collisions do not matter at 10^6.** Two distinct patients colliding
would mean two patients sharing a draw sequence, which is a *duplicate*
trajectory, not a corrupt one — the corpus stays valid, one patient's
clinical story merely rhymes with another's. At 10^6 ids over a 64-bit
mixed space the expected number of colliding pairs is ~2.7e-8
(birthday approximation n²/2^65 = 10^12 / 3.689e19). The failure mode
is cosmetic and its probability is negligible; engineering around it
would cost a uniqueness check on every stream construction for nothing.

**Rejected: `(Random. (hash [family id]))`.** Clojure's `hash` on a
vector is a 32-bit value, so the seed space is 2^32 and the expected
colliding pairs at 10^6 rise to ~116 (n²/2^33 = 10^12 / 8.590e9) — from
cosmetic to routine, a factor of 4.3 billion. It also makes the
derivation depend on a hash implementation this repo does not own,
against `rulings.md#R-no-derivation-through-nondeterminism`'s spirit
and against `gmf.clj:1465`'s own hash-order caution.

**Rejected: `SplittableRandom`/`splits`.** Java's `SplittableRandom`
gives a genuine split operation, but split streams are keyed by *split
order*, not by a stable id — a patient's stream would depend on how
many patients split before them, which reintroduces the exact
ordinal-coupling the arc exists to remove. `mix64` on a stable id is
order-free.

**Rejected: keeping `java.util.Random` but rewinding.** No.

#### (c) Newborns

```
stream-seed(:person, mix64(parent-person-id-tag, birth-ordinal))
```

A birth derives its stream from the **parent's** stable id and a birth
ordinal, never from a global counter — so a birth occurring anywhere in
the run perturbs no other person's stream, and adding a birth to
patient A's history in a later arc does not move patient B.

**What "ordinal" is when twins are excluded.** The traffic-scale plan's
arc-2 lean is that twins/multiples are a v1-named limitation. Under
that lean, **birth ordinal is the parity index: the parent's *n*-th
delivery, 0-indexed, in delivery-time order.** One delivery, one
newborn, so parity index and child index coincide and the ordinal is
unambiguous. If arc 2's charter ADR later admits multiples, the ordinal
must become the pair `(parity-index, within-delivery-index)` mixed in
that order, so that admitting twins does not renumber any existing
singleton's stream — a change that would otherwise reshuffle every
newborn ever generated. **Arc 2 should mix the pair from the start,
with `within-delivery-index` pinned at 0**, so the scheme survives the
limitation being lifted without a second migration. This is ruling B.

#### (d) The `from` = `to` delay skip

`engine.clj:421` calls `(rand-int-in rng from to)` unconditionally.
When `from` = `to`, `rand-int-in` (`:220`) evaluates
`(.nextInt rng 1)`, which is always `0` — the draw is consumed and its
value is arithmetically dead. Skipping it is free in outcome and costly
in stream position: it is **draw-affecting**.

It therefore **lands with the migration, in the same commit, never
before it**. Landing it first would spend one whole reshuffle
(re-pinning four gated corpora, one engine fixture, two conformance
baselines and fourteen trace captures) to buy nothing; landing it after
would spend a second. `R-per-person-streams-before-generator-fixes`
already says a generator fix landing first owes an author ruling — this
is the canonical instance of that rule, and the answer is "don't."

Note the asymmetry with the fixed-consumption law: that law
(`assign-pathway`, `churn/roll-gap`, `plan-latency`) exists so draw
count never depends on **data**. `from` = `to` is not data — it is the
authored *shape* of a step. Skipping it makes consumption depend on the
IR, which is exactly as visible as the step itself, and which under a
per-patient stream cannot reach any other patient. The law survives.

#### (e) Provenance

Per §1f item 6, `:seeds` is typed `[:map-of :keyword :int]` and cannot
hold a keyword. The tree already solved this once: `:event-schema-version`
is a **top-level** manifest key (`sim/manifest.clj:79`), added
additively because `ManifestV1_1` is an open map, with its own
docstring explaining why it sits at top level rather than inside a
sub-map — *"it describes the artifact, not the tool"*. A stream scheme
describes the artifact identically.

Recommended shape (ruling D):

```clojure
:stream-scheme  "1.0"          ; top-level, string, sibling of :event-schema-version
:seeds          {:primary seed}  ; unchanged, still :int-valued
```

**What ADR-0009 says the marker licenses, exactly.** ADR-0009 decision
1 states seed stability as a *within-version* guarantee and names
growing the stochastic surface as expected, not a regression. Decision 2
names `:generator {:version ...}` as the cross-version key. The
stream-scheme marker adds **nothing to the guarantee** — it does not
promise cross-version stability, and it does not weaken the
within-version one. What it buys is *legibility*: two corpora with the
same seed, config and generator version cannot differ; two corpora with
the same seed and config and *different* stream schemes are expected to
differ, and the marker says so on the artifact's face instead of
requiring a reader to resolve a generator version against a changelog.
It is a discriminator, not a warranty. ADR-0168's consequences section
already asked for exactly this ("Provenance gains a stream-version
marker at the Q3(b) boundary so pre- and post-migration corpora are
distinguishable on their face").

#### (f) What `churn.clj`'s docstring argument becomes

`churn.clj:16` currently reads: *"`inject` takes the run's own
`java.util.Random` (the SAME instance `ehrt.sim-engine.engine/run`
already threads through decide, not a derived or isolated stream) — the
same reasoning sim/ADR-0009 gives for NPI generation, extended here."*

That sentence is a *disclosure of a consequence*, written when the only
alternative on the table was ADR-0009's rejected option (b) — a
one-off isolated stream carved out for one site to preserve one
fixture's bytes. It is not an argument for sharing; it is an argument
against *ad-hoc* carve-outs, and ADR-0009's own rejected-alternative
text says so: the objection is to "a second, isolated stream for it" in
exchange for "a backward-compatibility guarantee this pre-release
project doesn't need yet."

Under this scheme, churn's rows are PATIENT (§1c) and move to the
patient's own stream. **The docstring's paragraph is replaced, not
deleted**, by one that keeps the law it actually protects — fixed
consumption regardless of profile values or applicability state, the
`zero-profile-is-the-identity-transform` property — and re-states its
stream as the patient's rather than the run's. The `rejected` half of
ADR-0009 is superseded *in scope for this arc only*: a principled,
total partition keyed on stable ids is not the ad-hoc carve-out it
refused, and the guarantee it declined to buy is still not being
bought.

`docs/dev/simulator-architecture.md:149-166`'s "one deliberate
impurity" paragraph and `engine.clj:49`'s ns docstring carry the same
claim and need the same treatment in the migration commit. The
**RNG-path law itself is unchanged**: a measurement claiming to
characterize the simulator's output must still draw from the real
seeded, threaded path (`rulings.md#R-measure-claimed-population`) —
that path simply becomes plural.

---

### 3. Migration test obligations

Test names and one-sentence assertions. No code lands with this ADR.

**LOCALITY.**
`mutating-one-patients-stream-seed-moves-only-that-patient` — running
the same config twice with a single patient's `:patient`-family stream
seed perturbed, the ground-truth subsequence of **every other patient**
(`engine/events-for-patient`, `:250`) is byte-identical, and the world
stream's own draw sequence is untouched.

*Stated honestly, because §2(a) says it cannot be absolute:* the
perturbed patient's own WORLD-family interactions — the beds they take,
the peers they swap with, the boarder their discharge pulls in — do
move other patients' *placements*. The assertion is therefore over the
**PATIENT-scoped fields** of other patients' events, with the four
WORLD sites' outputs (`:location`, `:home-ward`, the bed-swap/merge
partner) excluded by name and the exclusion cited to this section. A
test that claimed total byte-identity would be false at
`engine.clj:480` and would be discovered false by the first churned
seed.

`the-world-stream-is-untouched-by-a-patient-stream-perturbation` — the
companion, asserting the converse directly: arrivals and the four
`allocate` call sites consume the same world-stream positions in both
runs.

**DETERMINISM CONTINUITY.**
`gated-corpora-re-pin-exactly-once-under-the-new-scheme` — the four
arc-0 fixtures are regenerated in the migration's own commit, with the
before/after digests both recorded, and the ADR-0169 F3 tripwire
(`run_test.clj:560`, byte gate and value gate disagreeing is its own
finding) **stays in place unchanged**.

*The prompt's premise here does not hold and the obligation is
restated.* `rulings.md#R-defspec-seed-policy` is *"seeds stay unpinned
repo-wide"*; exactly **three** of the repo's **83** `defspec`s pin a
`test.check` `:seed` — `engine_test.clj:1476` and `:1598`, and
`check_test.clj:958`, all `20260825`, all arc-0's own equivalence
properties. A `test.check` seed pins *generator sampling*, not
generator *output*: those three specs assert one implementation equals
another over sampled engine seeds, and both implementations move
together under the partition. **They must stay green with no re-pin at
all**, and a red there is a real finding, not a migration cost. The
migration therefore owes one sweep at unpinned seeds
(`R-multi-seed-once-flagged`'s 2–3 well-mixed seeds at population
scale), not a defspec re-pin.

**WITNESS COUNTS.**
`the-locality-test-asserts-how-many-patients-it-moved` — the locality
test pins the **size** of the moved set (exactly 1 patient's own
subsequence differs; N−1 do not) and the size of the unmoved set, so
the test cannot pass by moving nothing. This is
`R-witness-population-is-counted` applied — landed by the de-scaffold
session of 2026-08-25 as a **gate, not a rulings row** (rulings.md is
FROZEN; the gates are `run_test.clj:846-858`'s
`cited-end-witness` assertions and `:676-684`'s pinned
reinstating-cancel count of 10). The locality test follows their exact
shape: assert the witness population's count first, then the property
over it.

**GATED-CORPORA RE-PIN.**
`arc0-gated-corpora-are-byte-and-value-identical-to-the-pinned-baseline`
(`run_test.clj:534`) moves once: four
`components/sim/test/ehrt/sim/fixtures/arc0_gated_*.edn` values and
four SHA-256s at `run_test.clj:500-503`. The gate's own docstring
already says a red is "a STOP, never a reason to re-pin (fence F1)" —
the migration is the one licensed exception, and it is licensed by
*this* ADR being Accepted, not by the session that hits the red.

#### Expected blast radius

Counted at `c1b996e`, not estimated:

* **1,866** `deftest`/`defspec` blocks across **193** test files (state-derived's own generated figure is **190** `*_test.clj` namespaces; the extra three are fixture/helper `.clj` files under `test/`). *The prompt's "352 test blocks" matches no figure the tree yields; disclosed rather than repeated.*
* **222** literal `:seed N` call sites across **52** test files — the *candidate* population. Most assert structure (event ordering, schema conformance, invariant emptiness) and survive a reshuffle untouched; they are candidates, not movers.
* **Definite movers — pinned generator output:**
  * `components/sim/test/ehrt/sim/fixtures/arc0_gated_*.edn` (4 files) and the four digests at `run_test.clj:500-503`.
  * `components/patient-simulator/test/ehrt/sim/fixtures/pinned_seed_42_patients_5.edn` — ADR-0009's own original fixture, which that ADR already warns "the SAME regeneration is expected at each future milestone."
  * `run_test.clj`'s counted witnesses: the reinstating-cancel count `10` (`:680`) and the `cited-end-witness` counts (`:852`).
  * `projects/conformance/test-fixtures/reports/sim-v2-gate-baseline.edn` and `sim-v2-full-capability-baseline.edn`.
  * `demos/traces/` — **14 derived captures** across six trace directories, seeds 1/41/42/71/202, regenerated by `make traces` (a docsgen leaf, ~84 s) and diffed by CI.
  * `components/sim-engine/resources/sim-engine/event-examples.edn`, via `make event-schema-examples` (`event_fleet.clj`, 3 literal seeds).
  * The regression oracle's **35 roots** — `bin/regression-oracle`, which is **not** in `make test` and must be run deliberately, with the change **declared** (all 35 will differ; an oracle IDENTICAL here would mean the migration did nothing).
* **`make test` skips the integration tier** (`Makefile:49`,
  `skip:integration`), so `projects/integration`'s and
  `projects/conformance`'s seed-bearing tests do **not** run at push
  time. The migration owes an explicit `make integration` run before
  push, or CI is the first thing to see them.

---

### 4. Rulings needed

Lettered options; a recommendation on each. None is executed here.

**A. The mixing function.**
* **A1 — reuse `engine.clj:225` `mix64`** on `(family-tag, id-tag)`, unchanged, promoted from private to the sim-engine interface. **RECOMMENDED.** It is already in the tree, already splitmix64-constant, already trusted by `patient-id-for` for the same key shape, and reusing it means the partition adds no new numeric surface to specify or test.
* A2 — a new, separately specified SplitMix64 with its own golden-gamma increment. Rejected as duplication: the constants would be the same three.
* A3 — `(Random. (hash [family id]))`. Rejected in §2(b): 32-bit seed space, foreign hash implementation.

**B. The newborn ordinal.**
* **B1 — mix the pair `(parity-index, within-delivery-index)` from the start, with `within-delivery-index` pinned at 0 while multiples are excluded.** **RECOMMENDED.** Costs one extra `mix64` per newborn now; saves a full newborn-stream reshuffle if arc 2's charter ever admits twins.
* B2 — mix the bare parity index, and widen later if multiples land. Cheaper now, and it makes lifting the v1 limitation a draw-affecting change requiring its own migration.

**C. Does emission join the scheme, or keep `run.clj`'s precedent?**
* **C1 — emission joins as a fifth family, `:emission`, derived like the rest.** **RECOMMENDED.** `run.clj:422` today constructs `(java.util.Random. seed)` — the **master seed verbatim**, so the latency stream replays the engine's own first draws. That correlation is invisible today only because the two streams are consumed for unrelated purposes; arc 4 adds chatter, fan-out and status ladders to the emission side, and a family-derived seed decorrelates them for the cost of one `mix64`.
* C2 — leave `run.clj:422` exactly as it is; it is already off the engine stream and arc 4 reshuffles nothing. Cheaper, and it leaves a known correlation in place at the moment the emission side stops being one draw per event.

**D. Where the marker lives.**
* **D1 — a top-level `:stream-scheme` string, sibling of `:event-schema-version`.** **RECOMMENDED.** `:seeds` is `[:map-of :keyword :int]` (`manifest.clj:96`) and cannot hold a keyword; `ManifestV1_1` is an open map, so this is additive at the provenance seam with no shared-schema change and no non-sim corpus growing a key that means nothing to it — the exact argument `sim/manifest.clj:73-78` already makes for `:event-schema-version`.
* D2 — widen `:seeds` to `[:map-of :keyword [:or :int :keyword]]` and put `:stream-scheme` inside it. Touches `ManifestV0/V1/V1_1`'s shared schema for a key only sim writes, and `:seeds`'s two existing vocabularies (`sim`'s `:primary`, corpus's `:master`/`:clinician` where `:clinician` is Synthea's flag) are already a legibility problem this would deepen.
* D3 — a new `:seeds {:master n :stream-scheme ...}` map that also renames sim's `:primary` to `:master`. A manifest schema break for a naming preference; would need its own version bump.

**E. Are facility/provider draws WORLD or FACILITY?**
* **E1 — FACILITY, a run-scoped family distinct from WORLD.** **RECOMMENDED.** `materialize-providers` and `choose-attending` read no patient state at all; `:outpatient-visit`'s provider pick reads none either. Separating them means adding a ward or a provider template does not shift arrival gaps or bed choices — a real and frequently-exercised config edit. The one wrinkle: `allocate`'s bed choice is *about* the facility but *depends on* the occupancy board, so it stays WORLD; the split is by what the draw reads, not by what it is named after.
* E2 — fold FACILITY into WORLD; two run-scoped families is one more than the design needs. Simpler, and it makes every facility-config edit reshuffle arrivals.

**F. Migration in one session or two?**
* **F1 — one session.** The partition, the `from` = `to` skip, all fixture re-pins, the oracle re-baseline and the three docstring corrections land together, because every one of them is draw-affecting and splitting them spends a second reshuffle. **RECOMMENDED**, with the disclosure that this is a large single commit whose green is CI's `make test` plus a deliberate `make integration` and `bin/regression-oracle` run.
* F2 — two sessions: partition first with the `from` = `to` skip held back. Pays two reshuffles for one change, against §2(d).

---

### Consequences

* Arcs 2–4 become possible without a corpus-wide reshuffle per change: a draw added to one patient's pathway moves that patient.
* The reshuffle-era constraint
  (`rulings.md#R-per-person-streams-before-generator-fixes`) is
  discharged when the migration lands, not when this ADR is accepted.
* `rulings.md` is FROZEN (de-scaffold ruling, 2026-08-25). Nothing here
  becomes a rulings row; the laws in §2 and §3 land as **gates** in the
  migration's own commit or not at all.
* Two stale citations in live source are recorded in §1f (items 8 and
  9) and **not fixed** — this session's fence forbids touching
  `components/*/src`. They are one line each in the session record, per
  the de-scaffold ruling's finding rule.
* Nothing in this ADR is executable until rulings A–F are answered. The
  roadmap row records that state.

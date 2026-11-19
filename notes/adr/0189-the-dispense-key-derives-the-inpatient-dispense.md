## ADR-0189 — the `:dispense` key derives the inpatient dispense, after a drawn pharmacy turnaround set per ward

**Status:** Accepted 2026-11-19 (author rulings of 2026-11-19, quoted
below; build session same day). Amends ADR-0187. Event schema
**1.12.0**, unchanged; describe report **1.3.0**, unchanged; stream
scheme **"1.0"**, unchanged.

### Context

ADR-0187's key derived a `:community` fill for an order an outpatient
visit wrote. An order written during an admission got nothing, so
`dense-7500` held 48 dispenses against 4,938 admission-written orders
at 7,500 arrivals (ADR-0188). Authoring the dispense as a pathway step
was tried and reshuffles the run (ADR-0188's halt).

Author rulings, 2026-11-19. On stability: "ok with changing population,
draws, etc. ... at this early stage, in favor of realistic data ... At
some point, we will stabilize on a deterministic regime again, but we
needn't make things difficult just for that." Confirmed as: what is
relaxed is stability ACROSS revisions; at one revision the same seed
and config still give the same bytes, and that stays gated. "This is
temporary." On this slice: "use reasonable defaults based on what you
can quickly glean from public sources about real situations, but don't
spend a lot of time researching. If you can make those frequencies be
specified as options in the site config file, do that and give them
reasonable defaults. Per ward class. Go."

### Decision

**1. The inpatient rule.** Under the `:dispense` key, a medication
order written while the patient's open encounter is an admission is
dispensed `:inpatient` after a pharmacy turnaround. The turnaround is
one uniform draw, in minutes, from the range of the ward the patient is
in when the order is written. The dispense is a queue entry at the
order's instant plus the draw.

**2. The undispensed order is a fact of the model.** When the entry is
decided, the dispense is emitted only if the patient is still in the
same admission and the order is still active and is still the patient's
latest order of its citation. Otherwise nothing is emitted: the order
was stopped, or the patient left, before pharmacy filled it. A patient
absorbed by a merge takes the queued entry with them, as with every
queued step but a result. This keeps both dispense laws true without
changing either: `clinical-content-only-when-admitted`, and the
dispense law's bound by the order's end. No invariant was added,
removed or reworded.

**3. Derived, not authored.** A step in the pathway costs the patient
draws under churn and a queue turn without it (ADR-0188). A derived
queue entry costs neither.

**4. The turnaround option.** A ward may state
`:dispense-minutes [lo hi]`, integers with `0 <= lo <= hi`; the schema
refuses anything else. A ward that states none takes its class default:

| ward class | default, minutes | basis |
|---|---|---|
| `:ed` | `[5 25]` | cabinet-stocked doses at a level I trauma centre, median order entry to cabinet dispense 9 to 15 minutes (J Pharm Pract, doi 10.1177/0897190017735390) |
| `:inpatient` | `[15 60]` | pharmacy-processed STAT orders at a community hospital, median 29 minutes, IQR 16-63 (Pharmacy Practice 2016;14(2):647, PMC4930852); pharmacy-prepared IV antibiotics, median order to delivery 43, later 27 minutes (J Pediatr Pharmacol Ther 2025;30(2):191) |

The defaults are **provisional**: a quick reading of three studies,
fitted to none. Uniform is the engine's shape for a range, as bed
turnaround is; the published distributions are right-skewed.

**5. The draw site.** A stream family of its own, `:pharmacy`, tag 7,
keyed by arrival ordinal as `:patient` is and built only under the key.
No existing stream's seed changed, so the scheme marker stays "1.0".
The consequence is that ADR-0187's erasure law still covers the whole
key: delete every dispense, renumber, and the keyless log is left,
event for event. Its test was not edited and is green, its
stays-under-churn case included.

**6. The stability ruling is temporary.** This change needed none of
the latitude it grants for ground truth: no population moved. It is
recorded here because the figures move and because the next slices
(below) will use it.

### What the erasure law does not cover: the wire

A dispense renders no message. But the emitter takes one draw per
ground-truth event to decide restatement chatter and latency, so a log
with dispenses in it fires different restatements than the same log
without them. Measured at the 750 cell on one tree, the key off and on:
35,926 messages against 35,920, the whole difference six ADT^A08.

`docs/consuming-ground-truth.md` said, since ADR-0187, that under the
key "the messages differ only in MSH-10". ADR-0187 measured that on a
root that configures neither chatter nor latency, where it is true; the
doc stated it generally, where it is not. Corrected in this session.
ADR-0188 attributed its message movement to the reference rule and not
to the key; at the 750 cell that movement (-1) was the key's. It
carries a dated correction.

### What moved, predicted and measured

The prediction was committed before either instrument ran (`5065965e`).

**Oracle roots: none, as predicted.** `bin/regression-oracle 046e611c
18a7f84f --declared-digest-change`: IDENTICAL, every root.
`bin/ground-truth-bracket`, same refs: IDENTICAL, 42 roots. One root
sets the key, `osteoporosis-dispense`, and its orders are all written
in outpatient visits.

That is also a gap: **no oracle root exercises the inpatient half.**
It is covered by the engine brick's tests and by `dense-7500`'s gated
figures.

**Gated corpora: the three keyed `dense-7500` configs, as predicted.**
`config-bare.edn`, the four arc-0 pins, `ed-tuesday` and
`clinic-decade` did not move.

`dense-7500`, seed 20260824, `--churn`, measured at `8acd6028`:

| cell | events before | events now | messages before | messages now |
|---|---|---|---|---|
| `config.edn` 7,500 | 167,763 | 170,224 | 176,973 | 176,956 |
| `config-nobed.edn` 7,500 | 125,748 | 128,209 | 134,891 | 134,895 |
| `config-bare.edn` 7,500 | 101,120 | 101,120 | 65,643 | 65,643 |
| `config.edn` 750 | 33,299 | 33,546 | 35,925 | 35,920 |
| `config-28024.edn` 28,024 | 671,646 | 681,741 | -- | -- |

Every event-count difference is the inpatient dispenses.

| cell | orders in admissions | `:inpatient` dispenses | undispensed | ended first | stay closed first | merged first | `:community` |
|---|---|---|---|---|---|---|---|
| `config.edn` 7,500 | 4,938 | 2,461 | 2,477 (50.2%) | 2,444 | 31 | 2 | 48 |
| `config-nobed.edn` 7,500 | 4,938 | 2,461 | 2,477 (50.2%) | 2,444 | 31 | 2 | 48 |
| `config-bare.edn` 7,500 | 5,888 | 0 | no key | | | | 0 |
| `config.edn` 750 | 489 | 247 | 242 (49.5%) | 240 | 2 | 0 | 3 |
| `config-28024.edn` 28,024 | 20,201 | 10,095 | 10,106 (50.0%) | 10,062 | 38 | 6 | 57 |

The cause of an undispensed order is read from the log as the first of
three events after it: its own end, its stay's close, or the merge that
absorbs its patient. Every "ended first" order is `dense-inpatient`'s
analgesia order, whose end shares its instant. Every dispensed order is
that pathway's antibiotic.

Order to dispense, in minutes (min / median / max), by the class of the
ward the patient was in at the order:

| cell | `:ed` | n | `:inpatient` | n |
|---|---|---|---|---|
| `config.edn` 7,500 | 5 / 15 / 25 | 2,410 | 15 / 41 / 59 | 51 |
| `config-nobed.edn` 7,500 | 5 / 15 / 25 | 2,373 | 15 / 42 / 60 | 88 |
| `config.edn` 750 | 5 / 15 / 25 | 235 | 15 / 45 / 54 | 12 |
| `config-28024.edn` 28,024 | 5 / 15 / 25 | 9,950 | 15 / 38 / 60 | 145 |

No lag in any cell is outside its ward's range. Every inpatient
dispense carries its order's `:encounter-id`; no community fill carries
one; no order is dispensed twice.

The 28,024 log is now 267,680,351 bytes, sha-256
`ce0982dec2e5d0dcab2e342a6b8422343a7a09349c1f084b08fa4b0ad9d961ce`. The
log it wrote from ADR-0188 until now, `5fb8187b...`, is kept in
`figures.edn` under `:pre-adr-0189`.

**The erasure equality at dense**, 750 cell: the keyed log with its 250
dispenses deleted and renumbered equals the log the same tree writes
from the config without the key, event for event (33,296 events).
`check-all` is clean over the keyed log, 47 invariants.

### The mutation operators give up dispensed orders

The suite found this, not the prediction. `dense-7500` is a population
of the event-mutation gate, and a dispense is a second citer of a
`:medication-order` under its own law. Two operators went red on set
equality, each convicting the dispense law as one finding more than it
declares: `:orphan-closed-medication-order` and
`:inverted-span-medication-end-order-event-id`.

The remedy is the one that file already states for ends: a predicate,
not a wider declaration. Four site predicates now say that no dispense
cites the order (`:orphan-participant`, the closed-start operators,
the inverted-span shape) or that no dispense of the repointed-to order
is later than the end (the cross-patient shape). No declared finding
set, contract or operator id changed.

So a dispensed order is, today, a site no operator mutates. Nothing
takes those sites until the dispense has a column of its own.

### What did not change

- Event schema 1.12.0. The dispense is ADR-0186's kind, unchanged.
- The describe report's version, 1.3.0. The inpatient row now reads
  `:configured :yes` under the key, as the community row has since
  ADR-0187; no row or key was added, and a report of a run without the
  key reads as it did.
- The checker.
- Determinism at one revision: both timed runs of every cell wrote
  byte-identical logs, and a two-run equality test is in the brick.
- Any run without the key: no code path differs.

### Downstream

`dense-7500` now carries dispenses in both settings. Corpora that move:
the three keyed `dense-7500` configs at every cell, and any corpus
generated with `:dispense` in which an admission writes an order.
Schema unchanged. A retained corpus is a valid artifact of the earlier
pin. New 28,024 sha-256 above. The pharmacy turnaround is set per ward
with `:dispense-minutes [lo hi]` in the facility config, minutes,
uniform; unset, an `:ed` ward takes `[5 25]` and an `:inpatient` ward
`[15 60]`. Under the key, an admission-written order with no dispense
was ended, or its stay closed, before pharmacy filled it. Rendered
message counts move by a handful when the key is set alongside chatter
or latency.

### Owed next

- **The mutation column for the dispense** (carried from ADR-0188, and
  now sharper): the dispense's own `:order-event-id` shapes, and
  operators that take the dispensed-order sites the existing ones gave
  up above.
- **`dense-inpatient`'s own realism.** Its analgesia order and its end
  share an instant, so half of this scenario's admission orders can
  never be dispensed.
- **Community fill lag and unfilled prescriptions**, as config options.
  The community half fills every order, at the visit's closing instant.
- **Chronic refills.** Period and cap unruled (ADR-0187).
- **Scaling `dense-7500`'s module cohort with the arrival count.**
- **An oracle root for the inpatient half.**
- **ED-discharge prescriptions** (ADR-0187): a prescription written at
  an ED discharge is, under this rule, an order whose stay usually
  closes before pharmacy reaches it.
- **Two orders of one citation in one stay.** The earlier one is not
  dispensed once the later is written (decision 2). No shipped corpus
  reaches this.

### What this does not do

- Rows nothing on the roadmap: this record is the record.
- Authors no dispense anywhere.
- Writes `:dispense-minutes` into `default-facility`, which leans on
  the class defaults.
- Makes the wire stable under the key.

### Dated addendum, 2026-11-19: two owed items paid

Session record `2026-11-19-dense-inpatient-analgesia-and-inpatient-root`.
A scenario edit and a new oracle root; no ADR of their own.

**`dense-inpatient`'s realism on this one point.** There was no later
point in the stay for the analgesia order's end, so the order moved
earlier: directly after the care plan starts, ahead of the 60 to 180
minute delay. Same 21 steps, same delays, same codes (`b6fb4ab3`).
Measured at seed 20260824, `--churn`:

| | 7,500 before | 7,500 now | 750 before | 750 now | 28,024 before | 28,024 now |
|---|---|---|---|---|---|---|
| orders written in admissions | 4,938 | 4,943 | 489 | 493 | 20,201 | 20,168 |
| `:inpatient` dispenses | 2,461 | 4,912 | 247 | 491 | 10,095 | 20,121 |
| undispensed: ended at the order's own instant | 2,444 | 0 | 240 | 0 | 10,062 | 0 |
| undispensed: the stay closed first | 31 | 29 | 2 | 2 | 38 | 41 |
| undispensed: merged away first | 2 | 2 | 0 | 0 | 6 | 6 |
| undispensed fraction | 50.2% | 0.6% | 49.5% | 0.4% | 50.0% | 0.2% |
| `:community` fills | 48 | 48 | 3 | 3 | 57 | 59 |
| order to dispense, ED (n; min / median / max) | 2,410; 5 / 15 / 25 | 2,720; 5 / 15 / 25 | 235; 5 / 15 / 25 | 269; 5 / 15 / 25 | 9,950; 5 / 15 / 25 | 10,949; 5 / 15 / 25 |
| order to dispense, inpatient ward | 51; 15 / 41 / 59 | 2,192; 15 / 38 / 60 | 12; 15 / 45 / 54 | 222; 15 / 37 / 60 | 145; 15 / 38 / 60 | 9,172; 15 / 37 / 60 |

The run moved beyond the medication kinds, in every cell, the keyless
one included (101,120 to 101,069 events). Subjects did not move. The
new 28,024 log is 270,551,734 bytes, sha-256
`26e31b4f44df3c9cd52a0429f15f0262f3dde4a6dec7575f2ee8bea845b652b0`;
`ce0982de...` is kept in `figures.edn` under `:pre-order-move`.

**An oracle root for the inpatient half.** `dispense-inpatient`
(`e0bbcdee`), the 46th root, a first baseline: twelve authored stays,
nine dispenses across an ED ward, an inpatient ward on the class
default and one stating its own range, six orders pharmacy never
reaches, `:encounters` on. Both instruments across its commit: every
existing root IDENTICAL, the new root target-side only.

**What the pathway edit cost the mutation gate.** With nearly every
closed order dispensed, the site predicates above left two operators
with no site in any population. A fourth population, `dense-7500` less
its `:dispense` key, sites them again (`8340c4e3`). `brick:corpus` now
runs 17 minutes.

**Still owed:** the pathway's drug codes (an oral penicillin, and
alendronic acid as analgesia for acute abdominal pain); scaling the
module cohort with the arrival count; community lag and unfilled
prescriptions as config options; chronic refills; counting the sites
the mutation operators give up, which would also retire the fourth
population.

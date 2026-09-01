# 2026-09-01 — the event-mutation population ledger

The measurement basis for ADR-0176's operator catalog, and the record
of which candidate operators this repository can convict TODAY versus
which it merely could convict in principle.

Produced by event-stream mutation (P6) implementation 2, under the
author ruling **Q10(a)** (2026-09-01): *ship operators ONLY where a
real population exists in a generatable log; columns without one are
recorded as POPULATION GAPS — distinct from ADR-0176 Q6's catalog gaps
— and the population work is rowed as its own priced item, not begun
here.*

The two gap kinds are not the same object, and the difference is the
whole point of this file:

| kind | what it means | where it lives |
|---|---|---|
| **catalog gap** (Q6) | the operator is *unconvictable*: `check` has no invariant that fires. Registration is REFUSED and the candidate recorded in `ehrt.corpus.operators/catalog-gaps`. | executable, in the registry |
| **population gap** (Q10) | convictable in principle — the invariant exists and would fire — but NO log this repository can generate carries a single candidate site, so the claim is unwitnessable. | this file, section 6 |
| **shape gap** (new) | the shape as ADR-0176 words it does not name a stable defect class: the observed finding set varies site to site, so Q5(a)'s set EQUALITY cannot be declared. Shipped NARROWED, or not at all. | this file, section 5 |

The third kind is this session's own discovery and is argued in section 5.

---

## 1. The population basis, derived rather than assumed

ADR-0176 section 2(iv) named `bin/ground-truth-bracket`'s gated corpora
as "the natural population". The spine session measured that claim
FALSE (spine record finding F1): every engine-layer oracle root runs a
`module-only` pathway and carries zero carriers of all four log-index
reference fields. Section 4 below corrects the ADR.

The real population is the **opt-in demo configs** — the only configs
in the tree that turn on the keys (`:scheduling`, `:ladders`, `:siu`)
that mint referential content. Derived by grep, not recalled:

```bash
grep -rn "ladders\|:siu\|:scheduling" --include=*.edn demos/
```

Three files, and exactly three:

* `demos/scenarios/clinic-decade/config.edn`
* `demos/scenarios/ed-tuesday/config.edn`
* `demos/scenarios/ed-tuesday/config-latency.edn`

**The latency variant is not a third population.** Its ground truth is
byte-identical to `ed-tuesday`'s — 428,889 bytes, `cmp` clean — because
`:latency` is transport-layer emission config reaching no member of
`ehrt.sim-engine.config/config-keys`. It is measured here once and then
dropped from every table below, which is itself a small confirmation of
the arc-wide byte-identity claim.

Each config is measured at **its own documented invocation** — the one
its `bin/demo-exerciser-*` already runs — rather than at a seed chosen
to flatter the numbers (`rulings.md#R-measure-claimed-population`):

```bash
bin/ehrt sim run --seed 20260807 --patients 200 \
    --config demos/scenarios/clinic-decade/config.edn \
    --format ground-truth > clinic-decade.edn      # 1,569 events
```

```bash
bin/ehrt sim run --seed 20260811 --patients 100 \
    --config demos/scenarios/ed-tuesday/config.edn \
    --format ground-truth > ed-tuesday.edn         # 1,235 events
```

### 1a. A blocking discovery about the parent's cleanliness

**`ehrt sim check` reports ed-tuesday's own clean log as violating
`:occupancy-within-capacity`, and the log is not at fault.** `check-all`'s
1-arity defaults `facility-config` to `ehrt.sim-model/default-facility`
(6 ED surge slots); ed-tuesday's config bumps that ward to 16
specifically so its busy-shift pacing holds. The checker is
config-starved, not the corpus dirty — passing the scenario's own
`:facility` to the 2-arity yields `#{}`:

| log | `check-all` 1-arity | `check-all` 2-arity, own `:facility` |
|---|---|---|
| clinic-decade | `#{}` | `#{}` (declares no `:facility`; the default IS its facility) |
| ed-tuesday | `#{:occupancy-within-capacity}` | `#{}` |

This matters here because oracle-loop step 1 — *the parent is clean* —
is what makes step 5's equality a statement about the operator rather
than about the corpus. Every ed-tuesday gate below therefore runs
`check-all` at the 2-arity with the scenario's own facility.

It also names a real consumer-facing gap, recorded and NOT fixed here
(fixing it widens the CLI/sim-check surface, which this session's fences
and Q11(a) both forbid): **`ehrt sim check` takes no facility config, so
a scenario that overrides `:facility` cannot be checked clean at the
shell at all.** Rowed in section 6.

---

## 2. The referential matrix, measured

The derived family is ADR-0176 Q8(a)'s: reference field × defect shape.
This ledger derives it by **carrier**, not by field, which is a
correction the ADR's own table already implies but its arithmetic did
not carry — `:order-event-id` has TWO carriers with DIFFERENT convicting
invariants, so the matrix has five columns, not four (section 4).

| col | field | carrier kind | target kind | convicting invariant |
|---|---|---|---|---|
| A | `:cancels-event-id` | `:cancel-admit` / `:cancel-transfer` / `:cancel-discharge` | per cancel kind | `cancel-references-existing-uncancelled-event` |
| B1 | `:order-event-id` | `:result-available` | `:order-placed` | `result-references-existing-order-and-follows-it-in-time` |
| B2 | `:order-event-id` | `:medication-end` | `:medication-order` | `medication-end-references-existing-order-and-follows-it-in-time` |
| C | `:start-event-id` | `:care-plan-end` | `:care-plan-start` | `care-plan-end-references-existing-start-and-follows-it-in-time` |
| D | `:placeholder-event-id` | `:demographic-update` (`:cause :identity-fill`) | `:registered` | `identity-fill-references-its-placeholder-registration` |

### 2a. Site counts, operator by config

Candidate sites counted by each shape's own predicate over the two real
logs. `n/a (Q9)` marks a cell the event schema forbids: nulling a plain
`:int` produces a schema-INVALID mutant, which Q9(a) excludes because it
would be convicted by Malli rather than by `check` — the loop would
close on the wrong instrument.

**clinic-decade** (1,569 events)

| col | phantom | null | cross-patient | wrong-kind | inverted-span |
|---|---|---|---|---|---|
| A `:cancels-event-id` | 0 | n/a (Q9) | 0 | 0 | 0 |
| B1 `:order-event-id` (result) | 0 | n/a (Q9) | 0 | 0 | 0 |
| B2 `:order-event-id` (med-end) | 0 | 0 | 0 | 0 | 0 |
| C `:start-event-id` | 0 | 0 | 0 | 0 | 0 |
| **D `:placeholder-event-id`** | **21** | **21** | **21** | **21** | **21** |

**ed-tuesday** (1,235 events)

| col | phantom | null | cross-patient | wrong-kind | inverted-span |
|---|---|---|---|---|---|
| A `:cancels-event-id` | 0 | n/a (Q9) | 0 | 0 | 0 |
| **B1 `:order-event-id` (result)** | **25** | n/a (Q9) | **25** | **25** | **25** |
| B2 `:order-event-id` (med-end) | 0 | 0 | 0 | 0 | 0 |
| C `:start-event-id` | 0 | 0 | 0 | 0 | 0 |
| **D `:placeholder-event-id`** | **15** | **15** | **15** | **15** | **15** |

**Two of five columns are populated; three are empty in both.** A is
empty because neither corpus produces a cancel at all; B2 and C because
neither produces a `:medication-end` or a `:care-plan-end` —
clinic-decade emits 5 `:care-plan-start` and 0 `:care-plan-end`. This
confirms and widens the spine record's finding F2, which had measured A
and C empty; B2 joins them.

### 2b. A sixth shape, probed and dropped

A pure-referential alternative to `inverted-span` was probed: repoint
the reference at a real target-kind event for the SAME patient
occurring AFTER the carrier, which trips the invariant's time clause
with no `:t` edit and therefore no `timestamps-monotone` companion.
**Measured population: 0 (D, both configs) and 1 (B1, ed-tuesday).**
Dropped for want of a population — recorded here rather than silently
not built, and the ADR's own `:t`-moving shape shipped instead.

---

## 3. The observed finding sets

Each shape applied at sampled sites, `check` run over each mutant, and
the observed invariant-name set recorded. Q5(a) declares set EQUALITY,
so a shape whose observed set varies site to site cannot ship as one
operator (section 5).

**Referential — every cell stable, every set exactly as derived:**

| operator | log | sites | observed finding set |
|---|---|---|---|
| D / phantom | both | 21 / 15 | `#{identity-fill-…}` |
| D / null | both | 21 / 15 | `#{identity-fill-…}` |
| D / cross-patient | both | 21 / 15 | `#{identity-fill-…}` |
| D / wrong-kind | both | 21 / 15 | `#{identity-fill-…}` |
| D / inverted-span | both | 21 / 15 | `#{identity-fill-…, timestamps-monotone}` |
| B1 / phantom | ed-tuesday | 25 | `#{result-references-…}` |
| B1 / cross-patient | ed-tuesday | 25 | `#{result-references-…}` |
| B1 / wrong-kind | ed-tuesday | 25 | `#{result-references-…}` |
| B1 / inverted-span | ed-tuesday | 25 | `#{result-references-…, timestamps-monotone}` |

The two-element sets are exactly the case ADR-0176 section 2(iv)
predicted when it chose a SET over a singleton: *"inverting a span's
`:t` trips both `timestamps-monotone` and the span's own referential
invariant"*. Measured true.

All disjuncts of both convicting invariants are reached, one shape each
— `(nil? target)` by phantom and null, `(not= <kind> (:event target))`
by wrong-kind, the participant clause by cross-patient, and `(> (:t
target) (:t event))` by inverted-span. The derivation is therefore not
decorative: it covers the invariant.

**Structural — as ADR-0176 section 2(i) words them, none is stable.**
See section 5.

---

## 4. Correction to ADR-0176

Two of the ADR's claims are refuted by measurement. Both are entered as
a dated addendum in `notes/adr/0176-event-stream-mutation.md` — ADRs
append, never rewrite — and are stated here as the evidence behind it.

**(i) Section 2(iv)'s declared population is empty.** The gated corpora
carry zero candidate sites. Corrected to: the population is the opt-in
demo configs of section 1, and a catalog-wide gate must run there.

**(ii) Section 2(i)'s matrix arithmetic under-counts.** The ADR says
"four reference fields × five defect shapes"; its own table in the same
section already splits `:order-event-id` into "two, by carrier kind",
because `:result-available` and `:medication-end` are convicted by
DIFFERENT invariants and typed differently (`:int` versus `[:maybe
:int]`). The matrix is therefore **5 carrier columns × 5 shapes = 25
cells, minus 2 the schema forbids = 23**, not 20 — and the spine
record's forward price of "19 referential operators remaining" should
read **22**.

---

## 5. The shape gap: three structural operators, none shippable as worded

ADR-0176 section 2(i) gives each structural operator a single convicting
invariant. **All three claims are refuted**, and the reason is identical
in each case: a structural edit is not a content fault confined to one
field, so it cascades through the state machine.

Measured at ADR-worded scope (distinct observed sets across sampled
sites):

| operator, as ADR-0176 words it | claimed | measured |
|---|---|---|
| `drop-event` (drop an `:admission`) | `discharge-follows-admission` | **2–6 distinct sets**, 4–8 invariants each |
| `clock-skew` | `timestamps-monotone` | 1 set on clinic-decade, **2** on ed-tuesday |
| `orphan-participant` | `participant-ids-exist-in-run` | **8 distinct sets**, 1–9 invariants each |

Two mechanisms drive it, and both are worth naming because they are
properties of the log format rather than of these operators:

1. **Dropping an event RENUMBERS the log.** Every log-index reference
   past the drop point silently repoints one event earlier, so a drop
   injects a referential fault at every downstream carrier as a side
   effect. Every measurement above repairs the indices after the drop
   (decrement every reference greater than the dropped index); without
   that repair `drop-event` also trips `identity-fill-…` and
   `result-references-…` — a fault injector injecting a defect class it
   did not declare.
2. **Renaming a participant MOVES the event into a phantom patient's
   timeline.** Patient-scoped invariants then convict the phantom for
   having no `:registered` first event, which is correct and is not the
   declared class.

Also measured, and dropped: an `orphan/add` variant (append a phantom
participant rather than replace an existing one) **crashes the replay
machinery** — `ehrt.sim-engine.evolve:293`, `No matching clause:
:subject`. A mutant no consumer can fold is worse than one that convicts
ambiguously, so the replace variant is the only one considered.

### 5a. All three ship NARROWED, with measured sets

The fix is not to weaken Q5(a) but to narrow `:candidate-sites` until
the class IS stable — which is what a candidate-site predicate is for.

| shipped operator | narrowing | sites (cd / ed) | measured set, 100% of samples |
|---|---|---|---|
| `:clock-skew` | site carries no reference field, is not itself referenced, and carries no `:appointment-id` / `:scheduled-t` | 801 / 459 | `#{timestamps-monotone}` |
| `:drop-registration` | a non-placeholder `:registered` whose patient has at least one other event, is named by no merge, and is cited by no reference | 171 / 101 | `#{participant-ids-exist-in-run, registered-is-every-patients-first-event}` |
| `:orphan-participant` | site is therapeutic-intent clinical content, derived from `clinical-content-only-when-admitted`'s own scoped set | 47 / 0 | `#{clinical-content-only-when-admitted, every-encounter-is-opened-and-closed-or-still-open, participant-ids-exist-in-run, registered-is-every-patients-first-event}` |

Each narrowing is a statement, not a fudge:

* **`:clock-skew`**'s third clause exists because two ed-tuesday sites
  also tripped `scheduled-encounter-follows-its-appointment` — an
  encounter whose `:t` is read against its appointment's `:scheduled-t`
  is not a free clock, and excluding it is what makes the operator mean
  "move a clock" rather than "move a clock, and sometimes break a
  schedule".
* **`:drop-registration`** replaces the ADR's `drop-event`. The
  at-least-one-other-event clause is not tidiness: 5 of 33 sampled drops
  of a lone `:registered` produced `#{}` — a fault injector reporting
  success while injecting nothing, which is ADR-0165's own silence and
  exactly what oracle step 7 exists to catch.
* **`:orphan-participant`**'s four-element set is a real defect class,
  not a cascade tolerated: a clinical event attributed to a patient the
  run never registered is unadmitted content, in no encounter, for an
  unknown patient whose first event is not a registration. Those four
  findings ARE that sentence. The set is identical across all 47 sites
  and all five kinds — and the kind list is DERIVED from `check`'s own
  `clinical-content-only-when-admitted` scoping rather than hand-picked,
  so a sixth clinical kind joining that invariant joins this operator
  with it (ADR-0166's error ledger, applied one layer up).

`:orphan-participant` has **no ed-tuesday population** (that corpus
emits no therapeutic-intent clinical content), so its gate runs on
clinic-decade alone. Recorded rather than papered over.

---

## 6. The ledger

**SHIPPED — 11 new operators this session, 12 in the catalog:**

| operator | col | population (cd / ed) |
|---|---|---|
| `:phantom-placeholder-event-id` *(spine, 2026-09-01)* | D | 21 / 15 |
| `:null-placeholder-event-id` | D | 21 / 15 |
| `:cross-patient-placeholder-event-id` | D | 21 / 15 |
| `:wrong-kind-placeholder-event-id` | D | 21 / 15 |
| `:inverted-span-placeholder-event-id` | D | 21 / 15 |
| `:phantom-order-event-id` | B1 | 0 / 25 |
| `:cross-patient-order-event-id` | B1 | 0 / 25 |
| `:wrong-kind-order-event-id` | B1 | 0 / 25 |
| `:inverted-span-order-event-id` | B1 | 0 / 25 |
| `:clock-skew` | structural | 801 / 459 |
| `:drop-registration` | structural | 171 / 101 |
| `:orphan-participant` | structural | 47 / 0 |

**POPULATION-GAPPED — 14 cells, convictable in principle, unwitnessable
today.** Each names the invariant that WOULD convict it, so the day a
population exists the operator is a predicate and a `:fn`, not a design
question.

| col | cells | invariant that would convict | why empty |
|---|---|---|---|
| A `:cancels-event-id` | 4 (null schema-forbidden) | `cancel-references-existing-uncancelled-event` | no corpus emits any `:cancel-admit` / `:cancel-transfer` / `:cancel-discharge` |
| B2 `:order-event-id` (`:medication-end`) | 5 | `medication-end-references-existing-order-and-follows-it-in-time` | no corpus emits a `:medication-end` |
| C `:start-event-id` | 5 | `care-plan-end-references-existing-start-and-follows-it-in-time` | clinic-decade emits 5 `:care-plan-start` and 0 `:care-plan-end`; the vendored closures cite their start by `referenced_by_attribute`, a resolution the GMF interpreter never declared for the CarePlan family |

**SHAPE-GAPPED — 0 shipped as worded, 3 shipped narrowed** (section 5).

**CATALOG-GAPPED (Q6) — 0.** No candidate this session considered was
unconvictable. The refusal path stays exercised by the acceptance
suite's own dummy, as the spine left it.

**Rowed, not begun here** (Q10(a)'s own instruction):

1. **The population work** — authoring or configuring corpora that emit
   cancels, `:medication-end`, and `:care-plan-end`, which is what turns
   14 population gaps into 14 operators. Rowed on `roadmap.md`.
2. **`ehrt sim check`'s missing facility config** (section 1a) — a
   scenario that overrides `:facility` cannot be checked clean at the
   shell. Rowed on `roadmap.md`.
3. **The catalog-wide gate** — ADR-0176 section 2(iv)'s "whole catalog
   against a fixed set of clean logs" is now buildable, because section
   1 gives it a population. Not built here; the per-operator loops are.
4. **`:expected-findings` versus `check`'s own vocabulary** — still
   unchecked at registration (spine record 3(d)); Q11(a) keeps
   `sim-check.interface` un-widened and rows the cross-check.
5. **RNG family tag 6 (`:mutation`)** — still unreserved in
   `streams.clj` (spine record section 5); a sim-engine edit, still
   fenced out.

---

## 7. Re-deriving this ledger

The site counts of section 2a are re-derivable from the shipped
catalog itself: every `:candidate-sites` predicate in
`components/corpus/src/ehrt/corpus/operators.clj` is the authority, and
counting is `(count ((:candidate-sites op) log))`. The throwaway that
produced section 2a predates those predicates and agrees with them by
construction, since section 3's gates run the shipped ones.

The finding sets of sections 3 and 5 need the real catalog:

```bash
clojure -M:dev:test -i <probe.clj>
```

where the probe applies each operator at sampled sites and prints
`(set (map :invariant (:violations …)))` per site. Sampling was 30–40
sites per cell, strided across the log rather than taken from its head.

**Every "100% of samples" claim above is a sample, not a proof over all
sites** — stated plainly because the shipped gates assert the set at
specific seeds, and the population-wide claim is evidence for the
declaration rather than the gate itself. The probes are deliberately
NOT promoted to `bin/`: that is author-licensed fence widening
(`bin/event-census`'s own promotion note), and nothing here needs to run
per push.

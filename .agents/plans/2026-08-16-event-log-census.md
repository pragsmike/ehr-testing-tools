# Ground-truth event log — census

**Date:** 2026-08-16. **Tip at run:** `24f351d` (fence-battery fixes,
ADR-0140). **Charter:** the event-log contract arc's Step 1 — author
rulings *"Ok, add it, and make EDN be primary. JSON can be derived
later."* and *"Choose a."* (both 2026-08-16). This file is the arc's
Step-1 evidence: the event vocabulary and per-kind key population
**derived from the tree**, plus the four built-in consumers' own reads
cross-checked against it.

**This census changes nothing.** The event log's shape is not touched
by this arc; the schema that follows in Step 2 *describes* what the
tree already produces. Shape defects found here are register rows for
a follow-on, not fixes — describing the current truth first, then
changing it under a versioned contract, is the point.

---

## Method

Two populations, reconciled against each other:

**Source-derived.** Every `{:event <kind> …}` construction site in
`components/sim-engine/src/ehrt/sim_engine/engine.clj` — **21 kinds**.
`components/sim-trajectory` contributes no top-level log event of its
own: its `:event`-bearing maps (`compile_trajectory.clj:379`, `:508`)
are *nested facts* inside `:conditions` / `:pre-horizon-facts`, a
separate vocabulary (see "The nested-`:event` collision" below).
`engine.clj:1087`'s `{:event event :patient-id … :before … :after …
:world-before … :world-after …}` is `replay`'s own trace record — a
**derived view wrapping** an event, not an event; its `:before` /
`:after` / `:world-before` / `:world-after` keys are therefore **not**
event keys. (The driving prompt listed them among "common keys
observed"; the tree says otherwise.)

**Corpus-derived.** Eleven runs, **4,997 events**, `out/` cleared
first. The two demo scenarios are run exactly as their READMEs teach;
the rest exist because the demo scenarios alone reach only 17 of the
21 kinds, and a census that stopped there would have declared four
kinds unreachable and one live consumer read dead.

| corpus | invocation | why |
|---|---|---|
| `ed-tuesday` | `corpus generate sim --seed 20260811 --patients 100 --reference-date 2026-08-11 --churn --config demos/scenarios/ed-tuesday/config.edn` | the operational demo, verbatim |
| `clinic-decade` | `corpus generate sim --seed 20260807 --patients 200 --config demos/scenarios/clinic-decade/config.edn` | the ambulatory demo, verbatim |
| `widener` | `sim run --seed 909 --patients 250`, modules sepsis / uti / colorectal_cancer / sore_throat / sinusitis / bronchitis / asthma, 3650d | reaches `:diagnostic-report` |
| `meds` | `sim run --seed 4242 --patients 400`, ten MedicationEnd-bearing modules, 3650d | reaches `:medication-end` |
| `death` | `sim run --seed 8080 --patients 400`, modules sepsis / injuries / colorectal_cancer / dementia / veteran_lung_cancer, 3650d | reaches `:discharge`'s `:disposition` / `:codes` |
| `warmup` | `sim run --seed 3 --patients 100 --churn --warm-up-seconds 3600 --config …/ed-tuesday/config.edn` | reaches `:warm-up true` |
| `edchurn-{3,17,55,777,1234}` | `sim run --seed N --patients 100 --churn --config …/ed-tuesday/config.edn` | reaches `:cancel-admit`, `:step-rejected` |

The tabulator is reproduced verbatim in Appendix A so the numbers below
can be re-derived; it is a plain `clojure -M` script over the emitted
EDN, no repo dependency.

---

## Headline: the two populations reconcile exactly

```
source-constructed kinds: 21
observed kinds:           21
observed but not source-constructed: NONE
source-constructed but never observed: NONE
```

The closed vocabulary, alphabetically:

`:admission` `:bed-swap` `:cancel-admit` `:cancel-discharge`
`:cancel-transfer` `:care-plan-end` `:care-plan-start`
`:diagnostic-report` `:discharge` `:medication-end`
`:medication-order` `:merge` `:observation` `:order-placed`
`:outpatient-visit` `:outpatient-visit-end` `:procedure`
`:registered` `:result-available` `:step-rejected` `:transfer`

`:transfer-in-error` is a **step** type, not an event kind — its
`decide` emits an ordinary `:transfer` plus a `:cancel-transfer`
carrying `:in-error true` (`engine.clj:530-552`). `:delay`,
`:order`, and `:result-followup` are likewise steps that emit no
event of their own name.

### Four universal keys, not five

Present on **every event of every kind**, all 4,997:

- `:event` — keyword, the discriminator
- `:t` — long, seconds from run start
- `:participants` — vector of `{:patient-id :role}`
- `:warm-up` — boolean, stamped by `run`'s own `mark-warmup`
  (`engine.clj:1497`) on every event without exception

**`:active-mrn` is NOT universal.** It is absent from `:bed-swap`
(two subjects, two MRNs — they live inside `:swap`), `:merge` (carries
`:surviving-mrn` / `:merged-mrn` / `:merged-mrns` instead), and
`:step-rejected` (nothing became a real action). Any consumer keying
on `:active-mrn` must handle those three; `emit_hl7.clj`'s
`control-id-for` already does, by explicit `case`.

`:participants` has exactly one entry for every kind except
`:bed-swap` and `:merge`, which have exactly two. Observed roles:
`:subject`, `:survivor`, `:merged`.

### Time

`(apply <= (map :t events))` is **true for all eleven corpora**
individually. Monotonicity is a **run-level** property — a
concatenation of two runs is not monotone, and nothing in the log
marks a run boundary. This is why Step 2 states it as a run-level
property rather than a per-event schema constraint.

---

## The nested-`:event` collision — a contract hazard

`:registered`'s `:pre-horizon-facts` and the encounter kinds'
`:conditions` are vectors of maps that **carry their own `:event`
key**, drawn from a different vocabulary:

- `:conditions` entries: `:condition-onset`, `:condition-end`
- `:pre-horizon-facts` entries: `:condition-onset`, `:condition-end`,
  `:medication-order`, `:medication-end`, `:care-plan-start`,
  `:care-plan-end`

Four of those six names — `:medication-order`, `:medication-end`,
`:care-plan-start`, `:care-plan-end` — are **also** top-level log
event kinds, with different key sets. A consumer that walks the EDN
tree looking for `:event` (rather than iterating only the top-level
vector) will find nested facts and mistake them for log events. This
is the single most likely way a proprietary emitter gets the log
wrong, and it is not written down anywhere today. Step 3's generated
formats.md section must state it explicitly.

---

## Per-kind key population

Read `present` as "on how many of this kind's events the key appears
at all"; `nil` as "of those, how many carry a nil value". A key that
is *always present but always nil* is flagged in the findings below.

_(Tables generated by Appendix A over all eleven corpora.)_

<!-- CENSUS-TABLES-BEGIN -->
#### `:admission` (n=692)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 692 (always) | - | `string` |
| `:attending` | 692 (always) | - | `string` |
| `:citation` | 48/692 | - | `map{module,state}` |
| `:conditions` | 44/692 | - | `vector<map{citation,codes,event,references}>` |
| `:event` | 692 (always) | - | `keyword` |
| `:forced` | 692 (always) | - | `boolean` |
| `:home-ward` | 692 (always) | - | `string` |
| `:location` | 692 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 692 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 692 (always) | 48 | `string` |
| `:t` | 692 (always) | - | `long` |
| `:warm-up` | 692 (always) | - | `boolean` |

#### `:bed-swap` (n=57)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 57 (always) | - | `keyword` |
| `:participants` | 57 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 57 (always) | - | `map{<patient-id> -> map{active-mrn,attending,from,to}}` |
| `:t` | 57 (always) | - | `long` |
| `:warm-up` | 57 (always) | - | `boolean` |

#### `:cancel-admit` (n=6)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 6 (always) | - | `string` |
| `:cancels-event-id` | 6 (always) | - | `long` |
| `:event` | 6 (always) | - | `keyword` |
| `:participants` | 6 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 6 (always) | - | `long` |
| `:warm-up` | 6 (always) | - | `boolean` |

#### `:cancel-discharge` (n=11)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 11 (always) | - | `string` |
| `:attending` | 11 (always) | - | `string` |
| `:cancels-event-id` | 11 (always) | - | `long` |
| `:event` | 11 (always) | - | `keyword` |
| `:home-ward` | 11 (always) | - | `string` |
| `:location` | 11 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 11 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 11 (always) | - | `long` |
| `:warm-up` | 11 (always) | - | `boolean` |

#### `:cancel-transfer` (n=30)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 30 (always) | - | `string` |
| `:cancels-event-id` | 30 (always) | - | `long` |
| `:event` | 30 (always) | - | `keyword` |
| `:home-ward` | 30 (always) | - | `string` |
| `:in-error` | 22/30 | - | `boolean` |
| `:location` | 30 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 30 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 30 (always) | - | `long` |
| `:warm-up` | 30 (always) | - | `boolean` |

#### `:care-plan-end` (n=7)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7 (always) | - | `string` |
| `:care-plan-citation` | 7 (always) | 7 |  |
| `:citation` | 7 (always) | - | `map{module,state}` |
| `:event` | 7 (always) | - | `keyword` |
| `:participants` | 7 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 7 (always) | 7 |  |
| `:t` | 7 (always) | - | `long` |
| `:warm-up` | 7 (always) | - | `boolean` |

#### `:care-plan-start` (n=77)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 77 (always) | - | `string` |
| `:activities` | 77 (always) | - | `vector<map{code,display,system}>` |
| `:citation` | 77 (always) | - | `map{module,state}` |
| `:codes` | 77 (always) | - | `vector<map{code,display,system}>` |
| `:event` | 77 (always) | - | `keyword` |
| `:participants` | 77 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 77 (always) | - | `long` |
| `:warm-up` | 77 (always) | - | `boolean` |

#### `:diagnostic-report` (n=12)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 12 (always) | - | `string` |
| `:citation` | 12 (always) | - | `map{module,state}` |
| `:codes` | 12 (always) | - | `vector<map{code,display,system}>` |
| `:event` | 12 (always) | - | `keyword` |
| `:observations` | 12 (always) | - | `vector<map{category,codes,interpretation,reference-range,unit,value}>` \| `vector<map{category,codes,unit,value}>` \| `vector<map{category,codes,unit,value}|map{category,codes,value-code}>` \| `vector<map{category,codes,value-code}>` |
| `:participants` | 12 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 12 (always) | - | `long` |
| `:warm-up` | 12 (always) | - | `boolean` |

#### `:discharge` (n=689)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 689 (always) | - | `string` |
| `:attending` | 689 (always) | - | `string` |
| `:citation` | 48/689 | - | `map{module,state}` |
| `:codes` | 1/689 | - | `vector<map{code,display,system}>` |
| `:disposition` | 1/689 | - | `keyword` |
| `:event` | 689 (always) | - | `keyword` |
| `:location` | 689 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 689 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 689 (always) | - | `long` |
| `:warm-up` | 689 (always) | - | `boolean` |

#### `:medication-end` (n=2)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2 (always) | - | `string` |
| `:citation` | 2 (always) | - | `map{module,state}` |
| `:event` | 2 (always) | - | `keyword` |
| `:order-citation` | 2 (always) | - | `map{module,state}` |
| `:order-event-id` | 2 (always) | 2 |  |
| `:participants` | 2 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2 (always) | - | `long` |
| `:warm-up` | 2 (always) | - | `boolean` |

#### `:medication-order` (n=134)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 134 (always) | - | `string` |
| `:citation` | 134 (always) | - | `map{module,state}` |
| `:codes` | 134 (always) | - | `vector<map{code,display,system}>` |
| `:event` | 134 (always) | - | `keyword` |
| `:participants` | 134 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 134 (always) | - | `long` |
| `:warm-up` | 134 (always) | - | `boolean` |

#### `:merge` (n=20)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 20 (always) | - | `keyword` |
| `:merged-mrn` | 20 (always) | - | `string` |
| `:merged-mrns` | 20 (always) | - | `set<string>` |
| `:participants` | 20 (always) | - | `vector<map{patient-id,role}>` |
| `:surviving-mrn` | 20 (always) | - | `string` |
| `:t` | 20 (always) | - | `long` |
| `:warm-up` | 20 (always) | - | `boolean` |

#### `:observation` (n=108)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 108 (always) | - | `string` |
| `:category` | 108 (always) | - | `string` |
| `:citation` | 108 (always) | - | `map{module,state}` |
| `:codes` | 108 (always) | - | `vector<map{code,display,system}>` |
| `:event` | 108 (always) | - | `keyword` |
| `:interpretation` | 1/108 | - | `keyword` |
| `:participants` | 108 (always) | - | `vector<map{patient-id,role}>` |
| `:reference-range` | 1/108 | - | `map{high,low}` |
| `:t` | 108 (always) | - | `long` |
| `:unit` | 94/108 | - | `string` |
| `:value` | 94/108 | - | `double` |
| `:value-code` | 5/108 | - | `map{code,display,system}` |
| `:warm-up` | 108 (always) | - | `boolean` |

#### `:order-placed` (n=191)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 191 (always) | - | `string` |
| `:attending` | 191 (always) | - | `string` |
| `:concept` | 191 (always) | - | `map{code,display,system}` |
| `:event` | 191 (always) | - | `keyword` |
| `:location` | 191 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 191 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 191 (always) | - | `keyword` |
| `:t` | 191 (always) | - | `long` |
| `:warm-up` | 191 (always) | - | `boolean` |

#### `:outpatient-visit` (n=221)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 221 (always) | - | `string` |
| `:attending` | 221 (always) | - | `string` |
| `:citation` | 221 (always) | - | `map{module,state}` |
| `:conditions` | 12/221 | - | `vector<map{citation,codes,event,references}>` |
| `:event` | 221 (always) | - | `keyword` |
| `:participants` | 221 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 221 (always) | 221 |  |
| `:t` | 221 (always) | - | `long` |
| `:warm-up` | 221 (always) | - | `boolean` |

#### `:outpatient-visit-end` (n=219)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 219 (always) | - | `string` |
| `:attending` | 219 (always) | - | `string` |
| `:citation` | 219 (always) | - | `map{module,state}` |
| `:event` | 219 (always) | - | `keyword` |
| `:participants` | 219 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 219 (always) | - | `long` |
| `:warm-up` | 219 (always) | - | `boolean` |

#### `:procedure` (n=214)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 214 (always) | - | `string` |
| `:citation` | 214 (always) | - | `map{module,state}` |
| `:codes` | 214 (always) | - | `vector<map{code,display,system}>` |
| `:event` | 214 (always) | - | `keyword` |
| `:participants` | 214 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 214 (always) | - | `long` |
| `:warm-up` | 214 (always) | - | `boolean` |

#### `:registered` (n=1950)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1950 (always) | - | `string` |
| `:event` | 1950 (always) | - | `keyword` |
| `:participants` | 1950 (always) | - | `vector<map{patient-id,role}>` |
| `:persona` | 1950 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 521/1950 | - | `vector<map{citation,codes,event,references}>` |
| `:t` | 1950 (always) | - | `long` |
| `:warm-up` | 1950 (always) | - | `boolean` |

#### `:result-available` (n=189)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 189 (always) | - | `string` |
| `:attending` | 189 (always) | - | `string` |
| `:concept` | 189 (always) | - | `map{code,display,system}` |
| `:event` | 189 (always) | - | `keyword` |
| `:location` | 189 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 189 (always) | - | `long` |
| `:participants` | 189 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 189 (always) | - | `keyword` |
| `:results` | 189 (always) | - | `vector<map{abnormal-flag,concept,reference-range,units,value}>` |
| `:t` | 189 (always) | - | `long` |
| `:warm-up` | 189 (always) | - | `boolean` |

#### `:step-rejected` (n=3)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 3 (always) | - | `map{type}` |
| `:event` | 3 (always) | - | `keyword` |
| `:participants` | 3 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 3 (always) | - | `keyword` |
| `:t` | 3 (always) | - | `long` |
| `:warm-up` | 3 (always) | - | `boolean` |

#### `:transfer` (n=165)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 165 (always) | - | `string` |
| `:attending` | 165 (always) | - | `string` |
| `:bed-ready` | 165 (always) | - | `boolean` |
| `:event` | 165 (always) | - | `keyword` |
| `:forced` | 165 (always) | - | `boolean` |
| `:from` | 165 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 165 (always) | - | `string` |
| `:location` | 165 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 165 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 45/165 | - | `keyword` |
| `:t` | 165 (always) | - | `long` |
| `:warm-up` | 165 (always) | - | `boolean` |
<!-- CENSUS-TABLES-END -->

### Optional keys whose presence is not arbitrary

Three "sometimes" keys turned out to be **perfectly correlated** with
another field, which the schema can therefore state as a rule rather
than as an unexplained optionality:

| kind | key | rule | evidence |
|---|---|---|---|
| `:transfer` | `:placement` | present **iff** `:bed-ready true` | 165 events: `{[present? bed-ready?] → count}` = `{[false false] 120, [true true] 45}`, no exceptions |
| `:admission` | `:reason` | non-nil **iff** `:citation` absent — i.e. hand-authored steps carry a reason, module-compiled ones never do | 692 events: `{[citation? reason-nil?] → count}` = `{[false false] 644, [true true] 48}`, no exceptions |
| `:admission` | `:conditions` | present **only** where `:citation` is present (44 of the 48) | `{[conditions? citation?] → count}` = `{[false false] 644, [false true] 4, [true true] 44}` |
| `:cancel-transfer` | `:in-error` | present **iff** produced by a `:transfer-in-error` step | `engine.clj:548-551` vs `:1122-1124` |
| `:registered` | `:pre-horizon-facts` | present **iff** the compiled module produced registration facts | `engine.clj:362`, a `cond->` on `(seq …)` |

`:citation` / `:conditions` on `:admission` / `:discharge` /
`:outpatient-visit` ride only when the step was module-compiled
(`citation-fields`, `engine.clj:366-375`) — rare in the operational
demos (48/692 admissions), universal in the ambulatory ones.

---

## Consumer cross-check

The prompt's rule: a key a consumer READS that the census never
observed is a **finding**; a key the census observes that no consumer
reads is a **note**.

### Who reads the raw event at all

| consumer | reads raw events? | surface |
|---|---|---|
| `sim-emit-hl7` (`emit_hl7.clj`) | **yes, extensively** | per-kind builders + `message-type-registry` |
| `sim-check` (`check.clj`) | **yes, extensively** | 30-odd invariants |
| `sim-emit-fhir` (`emit_fhir.clj`) | **`:t` only** | everything else via `engine/replay` → `evolve` |
| `corpus play` (`player.clj`) | **`:t` only** | `event-timestamp-ms`, `:51-58` |

**`sim-emit-fhir` is not a direct consumer of the event log.** It
calls `engine/replay` once and projects folded `PatientState`
(`emit_fhir.clj:264-266`); its only raw read is `(:t ground-truth)` for
the `:end` snapshot. Its *transitive* contract is therefore
**`evolve`'s** per-kind reads — which is a distinct surface worth
naming in Step 2, because Step 2's consumer-conformance test for
sim-emit-fhir is really a test that `evolve`'s inputs conform.

`evolve`'s reads, per kind (`engine.clj:869-1058`): `:registered`
→ `:persona`; `:admission` → `:location :home-ward :attending :t
:conditions`; `:transfer` → `:location :home-ward`; `:discharge` →
`:t :disposition`; `:cancel-transfer` → `:home-ward :location`;
`:cancel-discharge` → `:home-ward :location :attending`; `:bed-swap`
→ `:swap`; `:merge` → `:participants :surviving-mrn :merged-mrns`;
`:result-available` → `:t :results`; `:outpatient-visit` →
`:attending :t :conditions`; `:outpatient-visit-end` → `:t`;
`:observation` → `:t :codes` + the value fields;
`:diagnostic-report` → `:t :observations`; `:medication-order` →
`:t :codes :citation`; `:medication-end` → `:t :order-citation`;
`:care-plan-start` → `:t :codes :activities :citation`;
`:care-plan-end` → `:t :care-plan-citation`. `:cancel-admit`,
`:step-rejected`, `:order-placed`, `:procedure` fold as identity.

### Findings — keys read but not observed

**None survived.** One candidate did, and the census had to be widened
to clear it:

- `check.clj:212` reads `(:disposition event)` on `:discharge`
  (`expired-discharge-vacates-no-bed`), and `engine.clj`'s
  `death-disposition-fields` also rides `:codes`. Neither appears in
  either demo scenario, nor in the module-mix or order-result traces.
  The `death` corpus above was built specifically to settle it, and
  **produced both** — 1/689 discharges, from `injuries`'
  `:death-gunshot-wound` state. The read is live, on a path no demo
  covers. **Not a finding; a coverage note** — worth saying out loud
  that the death path is exercised by exactly one event in a
  4,997-event census, and by zero events in anything the docs teach.

### Notes — keys observed but unread by any consumer

Present in the log, carrying real meaning, consumed by nothing today.
These are part of the contract (they are what a proprietary emitter
would reach for), which is precisely the arc's argument:

| kind | unread keys | remark |
|---|---|---|
| `:discharge` | `:disposition`, `:codes` | read by `check.clj` and `evolve`, **never rendered** by either emitter — a death is wire-invisible today |
| `:transfer` | `:home-ward`, `:forced`, `:bed-ready`, `:placement` | `check.clj` reads `:forced` / `:home-ward` for the surge invariant; the HL7 emitter renders none of them |
| `:cancel-*` | `:cancels-event-id`, `:home-ward`, `:in-error` | `check.clj` reads `:cancels-event-id`; the emitter renders none |
| `:order-placed` / `:result-available` | `:profile` | `check.clj` only |
| `:observation` | `:category` | `evolve` folds it into `ObservationRecord` via `observation-value-fields`, so it *reaches state* — and then neither emitter renders it (`observation-obx-segment` and `observation-resources` both destructure past it). Carried, never spoken. |
| `:merge` | `:merged-mrns` | `evolve` reads it; the A40 message carries only `:merged-mrn` |
| every kind | `:citation` | glass-box provenance, read by `evolve` for medication/care-plan pairing and by nothing else |
| every kind | `:warm-up` | read only by `check.clj`'s `warm-up-mark-matches-window` |
| `:registered` | `:pre-horizon-facts` | read by `check.clj`'s medication-end resolution; rendered by neither emitter |

### One genuine defect found in a consumer

**Site-profile Z-segment templates see a different event map depending
on the message family.** `emit_hl7.clj`'s `single-subject-message`
(`:471`) hands `z-segments-for` a **synthesized subset**
`{:event :t :active-mrn :location :from :attending :participants}`
(`:492-494`), while `bed-swap-message`, `merge-message`,
`orm-message`, `oru-message`, `observation-message` and
`diagnostic-report-message` all hand it the **whole event** (`ev`).
`context-for-event` (`:390-400`) resolves a template's `:path` with
`get-in` against whatever it is given, and `render-z-field` renders an
empty field rather than throwing on an unbound path — so the drop is
**silent**.

Reproduced live, one template bound to `:path [:warm-up]` (universal,
so any empty rendering is the bug and not the data) triggered on four
kinds:

```
ADT^A01 (:admission)        … ZWU|          <- empty, subset path
ADT^A03 (:discharge)        … ZWU|          <- empty, subset path
ORM^O01 (:order-placed)     … ZWU|false     <- populated, whole-ev path
ORU^R01 (:result-available) … ZWU|false     <- populated, whole-ev path
```

Consequence: a site profile cannot bind a Z-segment field to
`:reason`, `:home-ward`, `:forced`, `:bed-ready`, `:disposition`,
`:citation`, `:warm-up`, or any other key on any ADT-family event —
which is most of what a real site-specific segment would want — and
gets no diagnostic when it tries. **Register row for a follow-on**,
not a fix here: it is a consumer defect, outside this arc's fences
(zero emitter production changes), and it is exactly the kind of thing
the arc exists to make visible.

---

## Shape defects — register rows, not fixes

Per the arc's own fence: found by describing, fixed later under the
versioned contract.

**S-1 — no module-compiled encounter ever carries a `:reason`, but
every one of them carries the key.** `:outpatient-visit` is
`:reason nil` 221/221. `:admission` is `:reason nil` exactly 48/692 —
and those 48 are *precisely* the 48 that carry a `:citation`, with no
exceptions either way. So the rule is not "outpatient visits have no
reason"; it is **module-compiled encounters have no reason, and say so
with a present-but-nil key**, while hand-authored ones always have a
real string. `decide :admission` / `decide :outpatient-visit`
(`engine.clj:377-390`, `:680-695`) destructure `:reason` off the step
and merge it unconditionally, but `compile_trajectory`'s
`encounter->step` never sets one. Either the compiler supplies an
encounter reason (a module state name would be an honest one), or the
key rides the nil-dropping `cond->` treatment `:citation` already gets
— present-and-nil is the one shape that tells a consumer nothing.

**S-2 — `:care-plan-end` never resolves its own start.** 7/7 events
carry both `:care-plan-citation nil` and `:start-event-id nil`.
`decide :care-plan-end` (`:809-825`) resolves `:start-event-id` by
matching `care-plan-citation` against the log; with the citation nil,
the `when` short-circuits and the resolution can never succeed. So
`evolve :care-plan-end` (`:1052-1058`) never closes a care plan
either — every `:care-plan-start` in the census stays `:active`
forever. The compiled step is not carrying `:care-plan-citation`
through. This is a real broken link, not cosmetics.

**S-3 — `:medication-end`'s `:order-event-id` was nil in both
observed events**, while `:order-citation` was populated. Sample size
2, both from the `meds` corpus; the resolution is the designed
straddle case (`check.clj:479-505` explicitly allows a nil
`:order-event-id` when the order is a `:pre-horizon-facts` entry), so
this may be correct behaviour rather than a defect. **Needs one more
probe before it earns a register row** — recorded here as
undetermined rather than asserted either way.

**S-4 — the `:step-rejected` reason enum is 6 wide; the census
observed 1.** `engine/documented-step-rejection-reasons` names
`:illegal-cancel-admit`, `:illegal-cancel-transfer`,
`:illegal-cancel-transfer-bed-reoccupied`, `:illegal-cancel-discharge`,
`:illegal-cancel-discharge-bed-reoccupied`, `:illegal-bed-swap`,
`:illegal-merge`. Only `:illegal-cancel-transfer-bed-reoccupied`
occurred (3 events across five churn seeds). Not a defect — the
schema should reference the existing var rather than an
observation-derived set, which is what Step 2 will do. Recorded so
the "closed enum from the census" instinct does not narrow it.

**S-5 — an unrelated engine finding, disclosed not pursued.** Seed
202 under `--churn` with the ed-tuesday facility exits `:status
:error :category :self-check-failed`, violation
`:surge-only-when-earlier-rungs-exhausted` at `t 78480`. Reproducible.
Wholly outside this arc; recorded because it was found while running,
and a finding met and not written down is a finding lost.

---

## What Step 2 takes from this

1. The kind enum is **21**, closed, source-and-census agreed.
2. Common keys factor to **four**: `:event :t :participants
   :warm-up`. `:active-mrn` factors to "all but three".
3. Per-kind required/optional is the tables above, with the three
   correlated optionals stated as rules.
4. `:t` monotonicity is a **run-level** property.
5. The nested-`:event` vocabulary needs its own schema entry and its
   own paragraph in formats.md — it is the log's sharpest edge.
6. The consumer-conformance test for `sim-emit-fhir` should validate
   **`evolve`'s** inputs, since that is its real contract surface.
7. `:step-rejected`'s `:reason` schema references
   `engine/documented-step-rejection-reasons`, not the census.

---

## Appendix A — the tabulator

Reproducible with `clojure -Sdeps '{:paths ["<dir>"]}' -M -m census
<events.edn>…`. Kept out of `bin/` deliberately: this arc's fences do
not include a new committed instrument, and the script is a
one-session probe, not a gate. Promoting it to `bin/event-census`
alongside `bin/fence-census` is a reasonable Step-2 rider if the
author wants the census re-runnable by CI.

```clojure
(ns census
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(defn shape [v]
  (cond
    (nil? v) "nil"
    (keyword? v) "keyword"
    (string? v) "string"
    (boolean? v) "boolean"
    (integer? v) "long"
    (float? v) "double"
    (inst? v) "inst"
    (set? v) (str "set<" (str/join "|" (sort (distinct (map shape v)))) ">")
    (vector? v) (if (empty? v) "vector<>"
                    (str "vector<" (str/join "|" (sort (distinct (map shape v)))) ">"))
    (sequential? v) (str "seq<" (str/join "|" (sort (distinct (map shape v)))) ">")
    (map? v) (str "map{" (str/join "," (sort (map #(if (keyword? %) (name %) (str %)) (keys v)))) "}")
    :else (str (type v))))

(defn shape* [v]
  ;; For maps whose keys are data (bed-swap's :swap), collapse to a
  ;; generic description rather than enumerating patient ids.
  (if (and (map? v) (every? string? (keys v)))
    (str "map{<patient-id> -> " (str/join "|" (sort (distinct (map shape (vals v))))) "}")
    (shape v)))

(defn census [events]
  (into (sorted-map)
        (for [[kind evs] (group-by :event events)]
          (let [n (count evs)
                all-keys (reduce into #{} (map keys evs))
                rows (for [k all-keys]
                       (let [present (filter #(contains? % k) evs)
                             p (count present)
                             nils (count (filter #(nil? (get % k)) present))
                             shapes (sort (distinct (map #(shape* (get % k))
                                                         (remove #(nil? (get % k)) present))))]
                         [k {:present p :nils nils :shapes shapes}]))]
            [kind {:n n :keys (into (sorted-map) rows)}]))))

(def source-kinds
  "The 21 kinds constructed in components/sim-engine/src/ehrt/sim_engine/engine.clj."
  #{:registered :admission :transfer :discharge :step-rejected :cancel-admit
    :cancel-transfer :cancel-discharge :bed-swap :merge :order-placed
    :result-available :outpatient-visit :outpatient-visit-end :procedure
    :observation :diagnostic-report :medication-order :medication-end
    :care-plan-start :care-plan-end})

(defn -main [& paths]
  (let [per (for [p paths] [p (edn/read-string (slurp p))])
        per (remove (fn [[_ v]] (map? v)) per)
        all (vec (mapcat second per))
        c (census all)
        observed (set (keys c))]
    (println "## Corpora")
    (doseq [[p evs] per]
      (println (format "- `%s` -- %d events" p (count evs))))
    (println (format "\nTotal: **%d events**, **%d kinds**." (count all) (count c)))
    (println "\n## Vocabulary reconciliation")
    (println (format "- source-constructed kinds: %d" (count source-kinds)))
    (println (format "- observed kinds: %d" (count observed)))
    (println (format "- observed but not source-constructed: %s"
                     (or (seq (sort (remove source-kinds observed))) "NONE")))
    (println (format "- source-constructed but never observed: %s"
                     (or (seq (sort (remove observed source-kinds))) "NONE")))
    (println "\n## Per-kind key population\n")
    (doseq [[kind {:keys [n] :as m}] c]
      (println (format "### `%s` (n=%d)\n" kind n))
      (println "| key | present | nil | value shape |")
      (println "|---|---|---|---|")
      (doseq [[k {:keys [present nils shapes]}] (:keys m)]
        (println (format "| `%s` | %s | %s | %s |"
                         k
                         (if (= present n) (str n " (always)") (format "%d/%d" present n))
                         (if (zero? nils) "-" (str nils))
                         (str/join " \\| " (map #(str "`" % "`") shapes)))))
      (println))
    (println "\n## Per-corpus kind counts\n")
    (doseq [[p evs] per]
      (println (format "- `%s`: %s" p
                       (str/join ", " (for [[k v] (sort (frequencies (map :event evs)))]
                                        (format "%s %d" k v))))))
    ;; universal keys
    (println "\n## Universal keys (present on every event of every kind)\n")
    (let [universal (reduce (fn [acc [_ {:keys [n] :as m}]]
                              (into #{} (filter (fn [k] (and (acc k)
                                                             (= n (:present (get (:keys m) k)))))
                                                acc)))
                            (set (keys (:keys (val (first c)))))
                            c)]
      (doseq [k (sort universal)] (println (format "- `%s`" k))))
    (println "\n## Nested `:event` vocabularies (NOT log events)\n")
    (println (format "- `:conditions` entries: %s"
                     (sort (distinct (mapcat (fn [e] (map :event (:conditions e))) all)))))
    (println (format "- `:pre-horizon-facts` entries: %s"
                     (sort (distinct (mapcat (fn [e] (map :event (:pre-horizon-facts e))) all)))))
    (println "\n## Participant roles\n")
    (println (format "- %s" (sort (distinct (mapcat (fn [e] (map :role (:participants e))) all)))))
    (println "\n## Per-corpus `:t` monotonicity\n")
    (doseq [[p evs] per]
      (println (format "- `%s`: `(apply <= (map :t ...))` = **%s**" p (apply <= (map :t evs)))))))
```

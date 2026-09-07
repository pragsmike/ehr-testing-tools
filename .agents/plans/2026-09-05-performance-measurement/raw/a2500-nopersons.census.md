## Corpora

- `/home/mg/perf-site7/a2500-nopersons.run1.edn` -- 43103 events

Total: **43103 events**, **26 kinds**.

## Vocabulary

- `:admission`
- `:appointment`
- `:appointment-cancel`
- `:bed-status-change`
- `:bed-swap`
- `:cancel-admit`
- `:cancel-discharge`
- `:cancel-transfer`
- `:care-plan-end`
- `:care-plan-start`
- `:diagnostic-report`
- `:discharge`
- `:medication-end`
- `:medication-order`
- `:merge`
- `:no-show`
- `:observation`
- `:order-placed`
- `:outpatient-visit`
- `:outpatient-visit-end`
- `:procedure`
- `:registered`
- `:reschedule`
- `:result-available`
- `:step-rejected`
- `:transfer`

## Per-kind key population

### `:admission` (n=1857)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1857 (always) | - | `string` |
| `:appointment-id` | 1177/1857 | - | `string` |
| `:attending` | 1857 (always) | - | `string` |
| `:citation` | 14/1857 | - | `map{module,state}` |
| `:conditions` | 14/1857 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 1857 (always) | - | `string` |
| `:event` | 1857 (always) | - | `keyword` |
| `:forced` | 1857 (always) | - | `boolean` |
| `:home-ward` | 1857 (always) | - | `string` |
| `:location` | 1857 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 1857 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 1843/1857 | - | `string` |
| `:t` | 1857 (always) | - | `long` |
| `:warm-up` | 1857 (always) | - | `boolean` |

### `:appointment` (n=2157)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2157 (always) | - | `string` |
| `:appointment-class` | 2157 (always) | - | `keyword` |
| `:appointment-id` | 2157 (always) | - | `string` |
| `:event` | 2157 (always) | - | `keyword` |
| `:participants` | 2157 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 635/2157 | - | `string` |
| `:scheduled-t` | 2157 (always) | - | `long` |
| `:t` | 2157 (always) | - | `long` |
| `:warm-up` | 2157 (always) | - | `boolean` |

### `:appointment-cancel` (n=166)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 166 (always) | - | `string` |
| `:appointment-id` | 166 (always) | - | `string` |
| `:event` | 166 (always) | - | `keyword` |
| `:participants` | 166 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 166 (always) | - | `long` |
| `:warm-up` | 166 (always) | - | `boolean` |

### `:bed-status-change` (n=11147)

| key | present | nil | value shape |
|---|---|---|---|
| `:bed` | 11147 (always) | - | `string` |
| `:event` | 11147 (always) | - | `keyword` |
| `:from` | 11147 (always) | - | `keyword` |
| `:last-patient-id` | 3751/11147 | - | `string` |
| `:participants` | 11147 (always) | - | `vector<map{bed-id,role,ward}>` |
| `:t` | 11147 (always) | - | `long` |
| `:to` | 11147 (always) | - | `keyword` |
| `:ward` | 11147 (always) | - | `string` |
| `:warm-up` | 11147 (always) | - | `boolean` |

### `:bed-swap` (n=742)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 742 (always) | - | `keyword` |
| `:participants` | 742 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 742 (always) | - | `map{<patient-id> -> map{active-mrn,attending,encounter-id,from,to}}` |
| `:t` | 742 (always) | - | `long` |
| `:warm-up` | 742 (always) | - | `boolean` |

### `:cancel-admit` (n=13)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 13 (always) | - | `string` |
| `:cancels-event-id` | 13 (always) | - | `long` |
| `:event` | 13 (always) | - | `keyword` |
| `:participants` | 13 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 13 (always) | - | `long` |
| `:warm-up` | 13 (always) | - | `boolean` |

### `:cancel-discharge` (n=14)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 14 (always) | - | `string` |
| `:attending` | 14 (always) | - | `string` |
| `:cancels-event-id` | 14 (always) | - | `long` |
| `:encounter-id` | 14 (always) | - | `string` |
| `:event` | 14 (always) | - | `keyword` |
| `:home-ward` | 14 (always) | - | `string` |
| `:location` | 14 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 14 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 14 (always) | - | `long` |
| `:warm-up` | 14 (always) | - | `boolean` |

### `:cancel-transfer` (n=732)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 732 (always) | - | `string` |
| `:cancels-event-id` | 732 (always) | - | `long` |
| `:encounter-id` | 732 (always) | - | `string` |
| `:event` | 732 (always) | - | `keyword` |
| `:home-ward` | 732 (always) | - | `string` |
| `:in-error` | 497/732 | - | `boolean` |
| `:location` | 732 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 732 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 732 (always) | - | `long` |
| `:warm-up` | 732 (always) | - | `boolean` |

### `:care-plan-end` (n=1147)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1147 (always) | - | `string` |
| `:care-plan-citation` | 1147 (always) | - | `map{module,state}` |
| `:encounter-id` | 1147 (always) | - | `string` |
| `:event` | 1147 (always) | - | `keyword` |
| `:participants` | 1147 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 1147 (always) | - | `long` |
| `:t` | 1147 (always) | - | `long` |
| `:warm-up` | 1147 (always) | - | `boolean` |

### `:care-plan-start` (n=1183)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1183 (always) | - | `string` |
| `:activities` | 23/1183 | - | `vector<map{code,display,system}>` |
| `:citation` | 1183 (always) | - | `map{module,state}` |
| `:codes` | 1183 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 1183 (always) | - | `string` |
| `:event` | 1183 (always) | - | `keyword` |
| `:participants` | 1183 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1183 (always) | - | `long` |
| `:warm-up` | 1183 (always) | - | `boolean` |

### `:diagnostic-report` (n=777)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 777 (always) | - | `string` |
| `:codes` | 777 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 777 (always) | - | `string` |
| `:event` | 777 (always) | - | `keyword` |
| `:observations` | 777 (always) | - | `vector<map{codes,unit,value}>` |
| `:participants` | 777 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 777 (always) | - | `long` |
| `:warm-up` | 777 (always) | - | `boolean` |

### `:discharge` (n=1819)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1819 (always) | - | `string` |
| `:attending` | 1819 (always) | - | `string` |
| `:citation` | 14/1819 | - | `map{module,state}` |
| `:encounter-id` | 1819 (always) | - | `string` |
| `:event` | 1819 (always) | - | `keyword` |
| `:location` | 1819 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 1819 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1819 (always) | - | `long` |
| `:warm-up` | 1819 (always) | - | `boolean` |

### `:medication-end` (n=1552)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1552 (always) | - | `string` |
| `:encounter-id` | 1552 (always) | - | `string` |
| `:event` | 1552 (always) | - | `keyword` |
| `:order-citation` | 1552 (always) | - | `map{module,state}` |
| `:order-event-id` | 1552 (always) | - | `long` |
| `:participants` | 1552 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1552 (always) | - | `long` |
| `:warm-up` | 1552 (always) | - | `boolean` |

### `:medication-order` (n=1597)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1597 (always) | - | `string` |
| `:citation` | 1597 (always) | - | `map{module,state}` |
| `:codes` | 1597 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 1597 (always) | - | `string` |
| `:event` | 1597 (always) | - | `keyword` |
| `:participants` | 1597 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1597 (always) | - | `long` |
| `:warm-up` | 1597 (always) | - | `boolean` |

### `:merge` (n=266)

| key | present | nil | value shape |
|---|---|---|---|
| `:encounter-id` | 266 (always) | - | `string` |
| `:event` | 266 (always) | - | `keyword` |
| `:merged-mrn` | 266 (always) | - | `string` |
| `:merged-mrns` | 266 (always) | - | `set<string>` |
| `:participants` | 266 (always) | - | `vector<map{patient-id,role}>` |
| `:surviving-mrn` | 266 (always) | - | `string` |
| `:t` | 266 (always) | - | `long` |
| `:warm-up` | 266 (always) | - | `boolean` |

### `:no-show` (n=307)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 307 (always) | - | `string` |
| `:appointment-id` | 307 (always) | - | `string` |
| `:event` | 307 (always) | - | `keyword` |
| `:participants` | 307 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 307 (always) | - | `long` |
| `:warm-up` | 307 (always) | - | `boolean` |

### `:observation` (n=2612)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2612 (always) | - | `string` |
| `:category` | 14/2612 | - | `string` |
| `:citation` | 14/2612 | - | `map{module,state}` |
| `:codes` | 2612 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 2612 (always) | - | `string` |
| `:event` | 2612 (always) | - | `keyword` |
| `:participants` | 2612 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2612 (always) | - | `long` |
| `:unit` | 2612 (always) | - | `string` |
| `:value` | 2612 (always) | - | `double` |
| `:warm-up` | 2612 (always) | - | `boolean` |

### `:order-placed` (n=3398)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3398 (always) | - | `string` |
| `:attending` | 3398 (always) | - | `string` |
| `:concept` | 3398 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 3398 (always) | - | `string` |
| `:event` | 3398 (always) | - | `keyword` |
| `:location` | 3398 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 3398 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 3398 (always) | - | `keyword` |
| `:t` | 3398 (always) | - | `long` |
| `:warm-up` | 3398 (always) | - | `boolean` |

### `:outpatient-visit` (n=593)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 593 (always) | - | `string` |
| `:appointment-id` | 421/593 | - | `string` |
| `:attending` | 593 (always) | - | `string` |
| `:citation` | 172/593 | - | `map{module,state}` |
| `:conditions` | 22/593 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 593 (always) | - | `string` |
| `:event` | 593 (always) | - | `keyword` |
| `:participants` | 593 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 421/593 | - | `string` |
| `:t` | 593 (always) | - | `long` |
| `:warm-up` | 593 (always) | - | `boolean` |

### `:outpatient-visit-end` (n=593)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 593 (always) | - | `string` |
| `:attending` | 593 (always) | - | `string` |
| `:citation` | 172/593 | - | `map{module,state}` |
| `:encounter-id` | 593 (always) | - | `string` |
| `:event` | 593 (always) | - | `keyword` |
| `:participants` | 593 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 593 (always) | - | `long` |
| `:warm-up` | 593 (always) | - | `boolean` |

### `:procedure` (n=1867)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1867 (always) | - | `string` |
| `:citation` | 47/1867 | - | `map{module,state}` |
| `:codes` | 1867 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 1867 (always) | - | `string` |
| `:event` | 1867 (always) | - | `keyword` |
| `:participants` | 1867 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1867 (always) | - | `long` |
| `:warm-up` | 1867 (always) | - | `boolean` |

### `:registered` (n=2500)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2500 (always) | - | `string` |
| `:event` | 2500 (always) | - | `keyword` |
| `:participants` | 2500 (always) | - | `vector<map{patient-id,role}>` |
| `:persona` | 2500 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 207/2500 | - | `vector<map{citation,codes,event,references}>` |
| `:t` | 2500 (always) | - | `long` |
| `:warm-up` | 2500 (always) | - | `boolean` |

### `:reschedule` (n=199)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 199 (always) | - | `string` |
| `:appointment-id` | 199 (always) | - | `string` |
| `:event` | 199 (always) | - | `keyword` |
| `:participants` | 199 (always) | - | `vector<map{patient-id,role}>` |
| `:prior-scheduled-t` | 199 (always) | - | `long` |
| `:scheduled-t` | 199 (always) | - | `long` |
| `:t` | 199 (always) | - | `long` |
| `:warm-up` | 199 (always) | - | `boolean` |

### `:result-available` (n=3398)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3398 (always) | - | `string` |
| `:attending` | 3398 (always) | - | `string` |
| `:concept` | 3398 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 2995/3398 | - | `string` |
| `:event` | 3398 (always) | - | `keyword` |
| `:location` | 3398 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 3398 (always) | - | `long` |
| `:participants` | 3398 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 3398 (always) | - | `keyword` |
| `:results` | 3398 (always) | - | `vector<map{abnormal-flag,concept,reference-range,unit,value}>` |
| `:t` | 3398 (always) | - | `long` |
| `:warm-up` | 3398 (always) | - | `boolean` |

### `:step-rejected` (n=38)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 38 (always) | - | `map{type}` |
| `:encounter-id` | 24/38 | - | `string` |
| `:event` | 38 (always) | - | `keyword` |
| `:participants` | 38 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 38 (always) | - | `keyword` |
| `:t` | 38 (always) | - | `long` |
| `:warm-up` | 38 (always) | - | `boolean` |

### `:transfer` (n=2429)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2429 (always) | - | `string` |
| `:attending` | 2429 (always) | - | `string` |
| `:bed-ready` | 2429 (always) | - | `boolean` |
| `:encounter-id` | 2429 (always) | - | `string` |
| `:event` | 2429 (always) | - | `keyword` |
| `:forced` | 2429 (always) | - | `boolean` |
| `:from` | 2429 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 2429 (always) | - | `string` |
| `:location` | 2429 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 2429 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 763/2429 | - | `keyword` |
| `:t` | 2429 (always) | - | `long` |
| `:warm-up` | 2429 (always) | - | `boolean` |

## Universal keys (present on every event of every kind)

- `:event`
- `:participants`
- `:t`
- `:warm-up`

## Nested `:event` vocabularies (NOT log events)

- `:conditions` entries: (:condition-end :condition-onset)
- `:pre-horizon-facts` entries: (:care-plan-end :care-plan-start :condition-end :condition-onset :medication-end :medication-order)

## Participant roles

- (:merged :subject :survivor)

## Per-corpus kind counts

- `/home/mg/perf-site7/a2500-nopersons.run1.edn`: :admission 1857, :appointment 2157, :appointment-cancel 166, :bed-status-change 11147, :bed-swap 742, :cancel-admit 13, :cancel-discharge 14, :cancel-transfer 732, :care-plan-end 1147, :care-plan-start 1183, :diagnostic-report 777, :discharge 1819, :medication-end 1552, :medication-order 1597, :merge 266, :no-show 307, :observation 2612, :order-placed 3398, :outpatient-visit 593, :outpatient-visit-end 593, :procedure 1867, :registered 2500, :reschedule 199, :result-available 3398, :step-rejected 38, :transfer 2429

## Per-corpus `:t` monotonicity (a RUN-level property)

- `/home/mg/perf-site7/a2500-nopersons.run1.edn`: `(apply <= (map :t ...))` = **true**

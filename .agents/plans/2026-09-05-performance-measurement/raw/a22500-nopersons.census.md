## Corpora

- `/home/mg/perf-out/a22500-nopersons.run1.edn` -- 431677 events

Total: **431677 events**, **26 kinds**.

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

### `:admission` (n=18200)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 18200 (always) | - | `string` |
| `:appointment-id` | 11590/18200 | - | `string` |
| `:attending` | 18200 (always) | - | `string` |
| `:citation` | 37/18200 | - | `map{module,state}` |
| `:conditions` | 37/18200 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 18200 (always) | - | `string` |
| `:event` | 18200 (always) | - | `keyword` |
| `:forced` | 18200 (always) | - | `boolean` |
| `:home-ward` | 18200 (always) | - | `string` |
| `:location` | 18200 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 18200 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 18163/18200 | - | `string` |
| `:t` | 18200 (always) | - | `long` |
| `:warm-up` | 18200 (always) | - | `boolean` |

### `:appointment` (n=21228)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 21228 (always) | - | `string` |
| `:appointment-class` | 21228 (always) | - | `keyword` |
| `:appointment-id` | 21228 (always) | - | `string` |
| `:event` | 21228 (always) | - | `keyword` |
| `:participants` | 21228 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 6238/21228 | - | `string` |
| `:scheduled-t` | 21228 (always) | - | `long` |
| `:t` | 21228 (always) | - | `long` |
| `:warm-up` | 21228 (always) | - | `boolean` |

### `:appointment-cancel` (n=1698)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1698 (always) | - | `string` |
| `:appointment-id` | 1698 (always) | - | `string` |
| `:event` | 1698 (always) | - | `keyword` |
| `:participants` | 1698 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1698 (always) | - | `long` |
| `:warm-up` | 1698 (always) | - | `boolean` |

### `:bed-status-change` (n=113449)

| key | present | nil | value shape |
|---|---|---|---|
| `:bed` | 113449 (always) | - | `string` |
| `:event` | 113449 (always) | - | `keyword` |
| `:from` | 113449 (always) | - | `keyword` |
| `:last-patient-id` | 38194/113449 | - | `string` |
| `:participants` | 113449 (always) | - | `vector<map{bed-id,role,ward}>` |
| `:t` | 113449 (always) | - | `long` |
| `:to` | 113449 (always) | - | `keyword` |
| `:ward` | 113449 (always) | - | `string` |
| `:warm-up` | 113449 (always) | - | `boolean` |

### `:bed-swap` (n=7372)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 7372 (always) | - | `keyword` |
| `:participants` | 7372 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 7372 (always) | - | `map{<patient-id> -> map{active-mrn,attending,encounter-id,from,to}}` |
| `:t` | 7372 (always) | - | `long` |
| `:warm-up` | 7372 (always) | - | `boolean` |

### `:cancel-admit` (n=169)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 169 (always) | - | `string` |
| `:cancels-event-id` | 169 (always) | - | `long` |
| `:event` | 169 (always) | - | `keyword` |
| `:participants` | 169 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 169 (always) | - | `long` |
| `:warm-up` | 169 (always) | - | `boolean` |

### `:cancel-discharge` (n=187)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 187 (always) | - | `string` |
| `:attending` | 187 (always) | - | `string` |
| `:cancels-event-id` | 187 (always) | - | `long` |
| `:encounter-id` | 187 (always) | - | `string` |
| `:event` | 187 (always) | - | `keyword` |
| `:home-ward` | 187 (always) | - | `string` |
| `:location` | 187 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 187 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 187 (always) | - | `long` |
| `:warm-up` | 187 (always) | - | `boolean` |

### `:cancel-transfer` (n=6486)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 6486 (always) | - | `string` |
| `:cancels-event-id` | 6486 (always) | - | `long` |
| `:encounter-id` | 6486 (always) | - | `string` |
| `:event` | 6486 (always) | - | `keyword` |
| `:home-ward` | 6486 (always) | - | `string` |
| `:in-error` | 4928/6486 | - | `boolean` |
| `:location` | 6486 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 6486 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 6486 (always) | - | `long` |
| `:warm-up` | 6486 (always) | - | `boolean` |

### `:care-plan-end` (n=11801)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 11801 (always) | - | `string` |
| `:care-plan-citation` | 11801 (always) | - | `map{module,state}` |
| `:encounter-id` | 11801 (always) | - | `string` |
| `:event` | 11801 (always) | - | `keyword` |
| `:participants` | 11801 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 11801 (always) | - | `long` |
| `:t` | 11801 (always) | - | `long` |
| `:warm-up` | 11801 (always) | - | `boolean` |

### `:care-plan-start` (n=11913)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 11913 (always) | - | `string` |
| `:activities` | 67/11913 | - | `vector<map{code,display,system}>` |
| `:citation` | 11913 (always) | - | `map{module,state}` |
| `:codes` | 11913 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 11913 (always) | - | `string` |
| `:event` | 11913 (always) | - | `keyword` |
| `:participants` | 11913 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 11913 (always) | - | `long` |
| `:warm-up` | 11913 (always) | - | `boolean` |

### `:diagnostic-report` (n=8254)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 8254 (always) | - | `string` |
| `:codes` | 8254 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 8254 (always) | - | `string` |
| `:event` | 8254 (always) | - | `keyword` |
| `:observations` | 8254 (always) | - | `vector<map{codes,unit,value}>` |
| `:participants` | 8254 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 8254 (always) | - | `long` |
| `:warm-up` | 8254 (always) | - | `boolean` |

### `:discharge` (n=18087)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 18087 (always) | - | `string` |
| `:attending` | 18087 (always) | - | `string` |
| `:citation` | 37/18087 | - | `map{module,state}` |
| `:encounter-id` | 18087 (always) | - | `string` |
| `:event` | 18087 (always) | - | `keyword` |
| `:location` | 18087 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 18087 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 18087 (always) | - | `long` |
| `:warm-up` | 18087 (always) | - | `boolean` |

### `:medication-end` (n=16499)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 16499 (always) | - | `string` |
| `:citation` | 1/16499 | - | `map{module,state}` |
| `:encounter-id` | 16498/16499 | - | `string` |
| `:event` | 16499 (always) | - | `keyword` |
| `:order-citation` | 16499 (always) | - | `map{module,state}` |
| `:order-event-id` | 16499 (always) | 1 | `long` |
| `:participants` | 16499 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 16499 (always) | - | `long` |
| `:warm-up` | 16499 (always) | - | `boolean` |

### `:medication-order` (n=16630)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 16630 (always) | - | `string` |
| `:citation` | 16630 (always) | - | `map{module,state}` |
| `:codes` | 16630 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 16630 (always) | - | `string` |
| `:event` | 16630 (always) | - | `keyword` |
| `:participants` | 16630 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 16630 (always) | - | `long` |
| `:warm-up` | 16630 (always) | - | `boolean` |

### `:merge` (n=2520)

| key | present | nil | value shape |
|---|---|---|---|
| `:encounter-id` | 2520 (always) | - | `string` |
| `:event` | 2520 (always) | - | `keyword` |
| `:merged-mrn` | 2520 (always) | - | `string` |
| `:merged-mrns` | 2520 (always) | - | `set<string>` |
| `:participants` | 2520 (always) | - | `vector<map{patient-id,role}>` |
| `:surviving-mrn` | 2520 (always) | - | `string` |
| `:t` | 2520 (always) | - | `long` |
| `:warm-up` | 2520 (always) | - | `boolean` |

### `:no-show` (n=3084)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3084 (always) | - | `string` |
| `:appointment-id` | 3084 (always) | - | `string` |
| `:encounter-id` | 12/3084 | - | `string` |
| `:event` | 3084 (always) | - | `keyword` |
| `:participants` | 3084 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 3084 (always) | - | `long` |
| `:warm-up` | 3084 (always) | - | `boolean` |

### `:observation` (n=26404)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 26404 (always) | - | `string` |
| `:category` | 49/26404 | - | `string` |
| `:citation` | 49/26404 | - | `map{module,state}` |
| `:codes` | 26404 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 26404 (always) | - | `string` |
| `:event` | 26404 (always) | - | `keyword` |
| `:participants` | 26404 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 26404 (always) | - | `long` |
| `:unit` | 26404 (always) | - | `string` |
| `:value` | 26404 (always) | - | `double` |
| `:warm-up` | 26404 (always) | - | `boolean` |

### `:order-placed` (n=34677)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 34677 (always) | - | `string` |
| `:attending` | 34677 (always) | - | `string` |
| `:concept` | 34677 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 34677 (always) | - | `string` |
| `:event` | 34677 (always) | - | `keyword` |
| `:location` | 34677 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 34677 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 34677 (always) | - | `keyword` |
| `:t` | 34677 (always) | - | `long` |
| `:warm-up` | 34677 (always) | - | `boolean` |

### `:outpatient-visit` (n=4576)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 4576 (always) | - | `string` |
| `:appointment-id` | 4045/4576 | - | `string` |
| `:attending` | 4576 (always) | - | `string` |
| `:citation` | 531/4576 | - | `map{module,state}` |
| `:conditions` | 58/4576 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 4576 (always) | - | `string` |
| `:event` | 4576 (always) | - | `keyword` |
| `:participants` | 4576 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 4045/4576 | - | `string` |
| `:t` | 4576 (always) | - | `long` |
| `:warm-up` | 4576 (always) | - | `boolean` |

### `:outpatient-visit-end` (n=4575)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 4575 (always) | - | `string` |
| `:attending` | 4575 (always) | - | `string` |
| `:citation` | 530/4575 | - | `map{module,state}` |
| `:encounter-id` | 4575 (always) | - | `string` |
| `:event` | 4575 (always) | - | `keyword` |
| `:participants` | 4575 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 4575 (always) | - | `long` |
| `:warm-up` | 4575 (always) | - | `boolean` |

### `:procedure` (n=18230)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 18230 (always) | - | `string` |
| `:citation` | 132/18230 | - | `map{module,state}` |
| `:codes` | 18230 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 18230 (always) | - | `string` |
| `:event` | 18230 (always) | - | `keyword` |
| `:participants` | 18230 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 18230 (always) | - | `long` |
| `:warm-up` | 18230 (always) | - | `boolean` |

### `:registered` (n=22500)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 22500 (always) | - | `string` |
| `:event` | 22500 (always) | - | `keyword` |
| `:participants` | 22500 (always) | - | `vector<map{patient-id,role}>` |
| `:persona` | 22500 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 603/22500 | - | `vector<map{citation,codes,event,references}>` |
| `:t` | 22500 (always) | - | `long` |
| `:warm-up` | 22500 (always) | - | `boolean` |

### `:reschedule` (n=2063)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2063 (always) | - | `string` |
| `:appointment-id` | 2063 (always) | - | `string` |
| `:event` | 2063 (always) | - | `keyword` |
| `:participants` | 2063 (always) | - | `vector<map{patient-id,role}>` |
| `:prior-scheduled-t` | 2063 (always) | - | `long` |
| `:scheduled-t` | 2063 (always) | - | `long` |
| `:t` | 2063 (always) | - | `long` |
| `:warm-up` | 2063 (always) | - | `boolean` |

### `:result-available` (n=34677)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 34677 (always) | - | `string` |
| `:attending` | 34677 (always) | - | `string` |
| `:concept` | 34677 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 30511/34677 | - | `string` |
| `:event` | 34677 (always) | - | `keyword` |
| `:location` | 34677 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 34677 (always) | - | `long` |
| `:participants` | 34677 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 34677 (always) | - | `keyword` |
| `:results` | 34677 (always) | - | `vector<map{abnormal-flag,concept,reference-range,unit,value}>` |
| `:t` | 34677 (always) | - | `long` |
| `:warm-up` | 34677 (always) | - | `boolean` |

### `:step-rejected` (n=1363)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 1363 (always) | - | `map{type}` |
| `:encounter-id` | 1169/1363 | - | `string` |
| `:event` | 1363 (always) | - | `keyword` |
| `:participants` | 1363 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 1363 (always) | - | `keyword` |
| `:t` | 1363 (always) | - | `long` |
| `:warm-up` | 1363 (always) | - | `boolean` |

### `:transfer` (n=25035)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 25035 (always) | - | `string` |
| `:attending` | 25035 (always) | - | `string` |
| `:bed-ready` | 25035 (always) | - | `boolean` |
| `:encounter-id` | 25035 (always) | - | `string` |
| `:event` | 25035 (always) | - | `keyword` |
| `:forced` | 25035 (always) | - | `boolean` |
| `:from` | 25035 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 25035 (always) | - | `string` |
| `:location` | 25035 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 25035 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 8230/25035 | - | `keyword` |
| `:t` | 25035 (always) | - | `long` |
| `:warm-up` | 25035 (always) | - | `boolean` |

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

- `/home/mg/perf-out/a22500-nopersons.run1.edn`: :admission 18200, :appointment 21228, :appointment-cancel 1698, :bed-status-change 113449, :bed-swap 7372, :cancel-admit 169, :cancel-discharge 187, :cancel-transfer 6486, :care-plan-end 11801, :care-plan-start 11913, :diagnostic-report 8254, :discharge 18087, :medication-end 16499, :medication-order 16630, :merge 2520, :no-show 3084, :observation 26404, :order-placed 34677, :outpatient-visit 4576, :outpatient-visit-end 4575, :procedure 18230, :registered 22500, :reschedule 2063, :result-available 34677, :step-rejected 1363, :transfer 25035

## Per-corpus `:t` monotonicity (a RUN-level property)

- `/home/mg/perf-out/a22500-nopersons.run1.edn`: `(apply <= (map :t ...))` = **true**

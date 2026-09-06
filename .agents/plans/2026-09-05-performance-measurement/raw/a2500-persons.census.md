## Corpora

- `/home/mg/perf-out/a2500-persons.run1.edn` -- 53942 events

Total: **53942 events**, **28 kinds**.

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
- `:coverage-change`
- `:demographic-update`
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

### `:admission` (n=2646)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2646 (always) | - | `string` |
| `:appointment-id` | 1145/2646 | - | `string` |
| `:attending` | 2646 (always) | - | `string` |
| `:citation` | 7/2646 | - | `map{module,state}` |
| `:conditions` | 7/2646 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 2646 (always) | - | `string` |
| `:event` | 2646 (always) | - | `keyword` |
| `:forced` | 2646 (always) | - | `boolean` |
| `:home-ward` | 2646 (always) | - | `string` |
| `:location` | 2646 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 2646 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 871/2646 | - | `string` |
| `:reason` | 2639/2646 | - | `string` |
| `:t` | 2646 (always) | - | `long` |
| `:warm-up` | 2646 (always) | - | `boolean` |

### `:appointment` (n=2433)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2433 (always) | - | `string` |
| `:appointment-class` | 2433 (always) | - | `keyword` |
| `:appointment-id` | 2433 (always) | - | `string` |
| `:encounter-id` | 21/2433 | - | `string` |
| `:event` | 2433 (always) | - | `keyword` |
| `:participants` | 2433 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 898/2433 | - | `string` |
| `:scheduled-t` | 2433 (always) | - | `long` |
| `:t` | 2433 (always) | - | `long` |
| `:warm-up` | 2433 (always) | - | `boolean` |

### `:appointment-cancel` (n=194)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 194 (always) | - | `string` |
| `:appointment-id` | 194 (always) | - | `string` |
| `:encounter-id` | 4/194 | - | `string` |
| `:event` | 194 (always) | - | `keyword` |
| `:participants` | 194 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 194 (always) | - | `long` |
| `:warm-up` | 194 (always) | - | `boolean` |

### `:bed-status-change` (n=13247)

| key | present | nil | value shape |
|---|---|---|---|
| `:bed` | 13247 (always) | - | `string` |
| `:event` | 13247 (always) | - | `keyword` |
| `:from` | 13247 (always) | - | `keyword` |
| `:last-patient-id` | 4452/13247 | - | `string` |
| `:participants` | 13247 (always) | - | `vector<map{bed-id,role,ward}>` |
| `:t` | 13247 (always) | - | `long` |
| `:to` | 13247 (always) | - | `keyword` |
| `:ward` | 13247 (always) | - | `string` |
| `:warm-up` | 13247 (always) | - | `boolean` |

### `:bed-swap` (n=718)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 718 (always) | - | `keyword` |
| `:participants` | 718 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 718 (always) | - | `map{<patient-id> -> map{active-mrn,attending,encounter-id,from,to}}` |
| `:t` | 718 (always) | - | `long` |
| `:warm-up` | 718 (always) | - | `boolean` |

### `:cancel-admit` (n=12)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 12 (always) | - | `string` |
| `:cancels-event-id` | 12 (always) | - | `long` |
| `:event` | 12 (always) | - | `keyword` |
| `:participants` | 12 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 12 (always) | - | `long` |
| `:warm-up` | 12 (always) | - | `boolean` |

### `:cancel-discharge` (n=19)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 19 (always) | - | `string` |
| `:attending` | 19 (always) | - | `string` |
| `:cancels-event-id` | 19 (always) | - | `long` |
| `:encounter-id` | 19 (always) | - | `string` |
| `:event` | 19 (always) | - | `keyword` |
| `:home-ward` | 19 (always) | - | `string` |
| `:location` | 19 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 19 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 19 (always) | - | `long` |
| `:warm-up` | 19 (always) | - | `boolean` |

### `:cancel-transfer` (n=680)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 680 (always) | - | `string` |
| `:cancels-event-id` | 680 (always) | - | `long` |
| `:encounter-id` | 680 (always) | - | `string` |
| `:event` | 680 (always) | - | `keyword` |
| `:home-ward` | 680 (always) | - | `string` |
| `:in-error` | 459/680 | - | `boolean` |
| `:location` | 680 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 680 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 680 (always) | - | `long` |
| `:warm-up` | 680 (always) | - | `boolean` |

### `:care-plan-end` (n=1088)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1088 (always) | - | `string` |
| `:care-plan-citation` | 1088 (always) | - | `map{module,state}` |
| `:encounter-id` | 1088 (always) | - | `string` |
| `:event` | 1088 (always) | - | `keyword` |
| `:participants` | 1088 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 1088 (always) | - | `long` |
| `:t` | 1088 (always) | - | `long` |
| `:warm-up` | 1088 (always) | - | `boolean` |

### `:care-plan-start` (n=1119)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1119 (always) | - | `string` |
| `:activities` | 16/1119 | - | `vector<map{code,display,system}>` |
| `:citation` | 1119 (always) | - | `map{module,state}` |
| `:codes` | 1119 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 1119 (always) | - | `string` |
| `:event` | 1119 (always) | - | `keyword` |
| `:participants` | 1119 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1119 (always) | - | `long` |
| `:warm-up` | 1119 (always) | - | `boolean` |

### `:coverage-change` (n=2430)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2430 (always) | - | `string` |
| `:cause` | 2430 (always) | - | `keyword` |
| `:encounter-id` | 31/2430 | - | `string` |
| `:event` | 2430 (always) | - | `keyword` |
| `:participants` | 2430 (always) | - | `vector<map{patient-id,role}>` |
| `:payer` | 2430 (always) | - | `map{id,name,type}` |
| `:person-event-id` | 2430 (always) | - | `string` |
| `:prior-payer` | 2430 (always) | - | `map{id,name,type}` |
| `:t` | 2430 (always) | - | `long` |
| `:warm-up` | 2430 (always) | - | `boolean` |

### `:demographic-update` (n=4461)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 4461 (always) | - | `string` |
| `:cause` | 4461 (always) | - | `keyword` |
| `:encounter-id` | 44/4461 | - | `string` |
| `:event` | 4461 (always) | - | `keyword` |
| `:field` | 4461 (always) | - | `keyword` |
| `:participants` | 4461 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 4461 (always) | - | `string` |
| `:persona` | 305/4461 | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:placeholder-event-id` | 305/4461 | - | `long` |
| `:prior-value` | 4461 (always) | - | `keyword` \| `map{address,status}` \| `map{family,given}` \| `map{last-known-address,status}` \| `string` |
| `:residence` | 1/4461 | - | `map{last-known-address,status}` |
| `:t` | 4461 (always) | - | `long` |
| `:value` | 4461 (always) | - | `keyword` \| `map{address,status}` \| `map{family,given}` \| `map{last-known-address,status}` \| `string` |
| `:warm-up` | 4461 (always) | - | `boolean` |

### `:diagnostic-report` (n=759)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 759 (always) | - | `string` |
| `:codes` | 759 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 759 (always) | - | `string` |
| `:event` | 759 (always) | - | `keyword` |
| `:observations` | 759 (always) | - | `vector<map{codes,unit,value}>` |
| `:participants` | 759 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 759 (always) | - | `long` |
| `:warm-up` | 759 (always) | - | `boolean` |

### `:discharge` (n=2608)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2608 (always) | - | `string` |
| `:attending` | 2608 (always) | - | `string` |
| `:citation` | 7/2608 | - | `map{module,state}` |
| `:encounter-id` | 2608 (always) | - | `string` |
| `:event` | 2608 (always) | - | `keyword` |
| `:location` | 2608 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 2608 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2608 (always) | - | `long` |
| `:warm-up` | 2608 (always) | - | `boolean` |

### `:medication-end` (n=1514)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1514 (always) | - | `string` |
| `:encounter-id` | 1514 (always) | - | `string` |
| `:event` | 1514 (always) | - | `keyword` |
| `:order-citation` | 1514 (always) | - | `map{module,state}` |
| `:order-event-id` | 1514 (always) | - | `long` |
| `:participants` | 1514 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1514 (always) | - | `long` |
| `:warm-up` | 1514 (always) | - | `boolean` |

### `:medication-order` (n=1550)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1550 (always) | - | `string` |
| `:citation` | 1550 (always) | - | `map{module,state}` |
| `:codes` | 1550 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 1550 (always) | - | `string` |
| `:event` | 1550 (always) | - | `keyword` |
| `:participants` | 1550 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1550 (always) | - | `long` |
| `:warm-up` | 1550 (always) | - | `boolean` |

### `:merge` (n=303)

| key | present | nil | value shape |
|---|---|---|---|
| `:cause` | 45/303 | - | `keyword` |
| `:encounter-id` | 258/303 | - | `string` |
| `:event` | 303 (always) | - | `keyword` |
| `:merged-mrn` | 303 (always) | - | `string` |
| `:merged-mrns` | 303 (always) | - | `set<string>` |
| `:participants` | 303 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 45/303 | - | `string` |
| `:surviving-mrn` | 303 (always) | - | `string` |
| `:t` | 303 (always) | - | `long` |
| `:warm-up` | 303 (always) | - | `boolean` |

### `:no-show` (n=333)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 333 (always) | - | `string` |
| `:appointment-id` | 333 (always) | - | `string` |
| `:encounter-id` | 2/333 | - | `string` |
| `:event` | 333 (always) | - | `keyword` |
| `:participants` | 333 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 333 (always) | - | `long` |
| `:warm-up` | 333 (always) | - | `boolean` |

### `:observation` (n=2517)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2517 (always) | - | `string` |
| `:category` | 12/2517 | - | `string` |
| `:citation` | 12/2517 | - | `map{module,state}` |
| `:codes` | 2517 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 2517 (always) | - | `string` |
| `:event` | 2517 (always) | - | `keyword` |
| `:participants` | 2517 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2517 (always) | - | `long` |
| `:unit` | 2517 (always) | - | `string` |
| `:value` | 2517 (always) | - | `double` |
| `:warm-up` | 2517 (always) | - | `boolean` |

### `:order-placed` (n=3289)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3289 (always) | - | `string` |
| `:attending` | 3289 (always) | - | `string` |
| `:concept` | 3289 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 3289 (always) | - | `string` |
| `:event` | 3289 (always) | - | `keyword` |
| `:location` | 3289 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 3289 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 3289 (always) | - | `keyword` |
| `:t` | 3289 (always) | - | `long` |
| `:warm-up` | 3289 (always) | - | `boolean` |

### `:outpatient-visit` (n=765)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 765 (always) | - | `string` |
| `:appointment-id` | 621/765 | - | `string` |
| `:attending` | 765 (always) | - | `string` |
| `:citation` | 144/765 | - | `map{module,state}` |
| `:conditions` | 15/765 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 765 (always) | - | `string` |
| `:event` | 765 (always) | - | `keyword` |
| `:participants` | 765 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 621/765 | - | `string` |
| `:t` | 765 (always) | - | `long` |
| `:warm-up` | 765 (always) | - | `boolean` |

### `:outpatient-visit-end` (n=765)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 765 (always) | - | `string` |
| `:attending` | 765 (always) | - | `string` |
| `:citation` | 144/765 | - | `map{module,state}` |
| `:encounter-id` | 765 (always) | - | `string` |
| `:event` | 765 (always) | - | `keyword` |
| `:participants` | 765 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 765 (always) | - | `long` |
| `:warm-up` | 765 (always) | - | `boolean` |

### `:procedure` (n=1775)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1775 (always) | - | `string` |
| `:citation` | 30/1775 | - | `map{module,state}` |
| `:codes` | 1775 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 1775 (always) | - | `string` |
| `:event` | 1775 (always) | - | `keyword` |
| `:participants` | 1775 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1775 (always) | - | `long` |
| `:warm-up` | 1775 (always) | - | `boolean` |

### `:registered` (n=2850)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2850 (always) | - | `string` |
| `:alias-name` | 351/2850 | - | `map{family,given}` |
| `:event` | 2850 (always) | - | `keyword` |
| `:identity` | 351/2850 | - | `keyword` |
| `:mother-patient-id` | 330/2850 | - | `string` |
| `:participants` | 2850 (always) | - | `vector<map{patient-id,role}>` |
| `:person-id` | 2850 (always) | - | `string` |
| `:persona` | 2850 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 164/2850 | - | `vector<map{citation,codes,event,references}>` |
| `:residence` | 396/2850 | - | `map{last-known-address,status}` \| `map{status}` |
| `:t` | 2850 (always) | - | `long` |
| `:warm-up` | 2850 (always) | - | `boolean` |
| `:window-close-t` | 350/2850 | - | `long` |

### `:reschedule` (n=223)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 223 (always) | - | `string` |
| `:appointment-id` | 223 (always) | - | `string` |
| `:encounter-id` | 3/223 | - | `string` |
| `:event` | 223 (always) | - | `keyword` |
| `:participants` | 223 (always) | - | `vector<map{patient-id,role}>` |
| `:prior-scheduled-t` | 223 (always) | - | `long` |
| `:scheduled-t` | 223 (always) | - | `long` |
| `:t` | 223 (always) | - | `long` |
| `:warm-up` | 223 (always) | - | `boolean` |

### `:result-available` (n=3289)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3289 (always) | - | `string` |
| `:attending` | 3289 (always) | - | `string` |
| `:concept` | 3289 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 2911/3289 | - | `string` |
| `:event` | 3289 (always) | - | `keyword` |
| `:location` | 3289 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 3289 (always) | - | `long` |
| `:participants` | 3289 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 3289 (always) | - | `keyword` |
| `:results` | 3289 (always) | - | `vector<map{abnormal-flag,concept,reference-range,unit,value}>` |
| `:t` | 3289 (always) | - | `long` |
| `:warm-up` | 3289 (always) | - | `boolean` |

### `:step-rejected` (n=52)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 52 (always) | - | `map{type}` |
| `:encounter-id` | 36/52 | - | `string` |
| `:event` | 52 (always) | - | `keyword` |
| `:participants` | 52 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 52 (always) | - | `keyword` |
| `:t` | 52 (always) | - | `long` |
| `:warm-up` | 52 (always) | - | `boolean` |

### `:transfer` (n=2303)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2303 (always) | - | `string` |
| `:attending` | 2303 (always) | - | `string` |
| `:bed-ready` | 2303 (always) | - | `boolean` |
| `:encounter-id` | 2303 (always) | - | `string` |
| `:event` | 2303 (always) | - | `keyword` |
| `:forced` | 2303 (always) | - | `boolean` |
| `:from` | 2303 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 2303 (always) | - | `string` |
| `:location` | 2303 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 2303 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 730/2303 | - | `keyword` |
| `:t` | 2303 (always) | - | `long` |
| `:warm-up` | 2303 (always) | - | `boolean` |

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

- `/home/mg/perf-out/a2500-persons.run1.edn`: :admission 2646, :appointment 2433, :appointment-cancel 194, :bed-status-change 13247, :bed-swap 718, :cancel-admit 12, :cancel-discharge 19, :cancel-transfer 680, :care-plan-end 1088, :care-plan-start 1119, :coverage-change 2430, :demographic-update 4461, :diagnostic-report 759, :discharge 2608, :medication-end 1514, :medication-order 1550, :merge 303, :no-show 333, :observation 2517, :order-placed 3289, :outpatient-visit 765, :outpatient-visit-end 765, :procedure 1775, :registered 2850, :reschedule 223, :result-available 3289, :step-rejected 52, :transfer 2303

## Per-corpus `:t` monotonicity (a RUN-level property)

- `/home/mg/perf-out/a2500-persons.run1.edn`: `(apply <= (map :t ...))` = **true**

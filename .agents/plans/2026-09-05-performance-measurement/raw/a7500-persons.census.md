## Corpora

- `/home/mg/perf-out/a7500-persons.run1.edn` -- 167197 events

Total: **167197 events**, **28 kinds**.

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

### `:admission` (n=8062)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 8062 (always) | - | `string` |
| `:appointment-id` | 3422/8062 | - | `string` |
| `:attending` | 8062 (always) | - | `string` |
| `:citation` | 30/8062 | - | `map{module,state}` |
| `:conditions` | 30/8062 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 8062 (always) | - | `string` |
| `:event` | 8062 (always) | - | `keyword` |
| `:forced` | 8062 (always) | - | `boolean` |
| `:home-ward` | 8062 (always) | - | `string` |
| `:location` | 8062 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 8062 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 2654/8062 | - | `string` |
| `:reason` | 8032/8062 | - | `string` |
| `:t` | 8062 (always) | - | `long` |
| `:warm-up` | 8062 (always) | - | `boolean` |

### `:appointment` (n=7340)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7340 (always) | - | `string` |
| `:appointment-class` | 7340 (always) | - | `keyword` |
| `:appointment-id` | 7340 (always) | - | `string` |
| `:encounter-id` | 41/7340 | - | `string` |
| `:event` | 7340 (always) | - | `keyword` |
| `:participants` | 7340 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 2796/7340 | - | `string` |
| `:scheduled-t` | 7340 (always) | - | `long` |
| `:t` | 7340 (always) | - | `long` |
| `:warm-up` | 7340 (always) | - | `boolean` |

### `:appointment-cancel` (n=592)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 592 (always) | - | `string` |
| `:appointment-id` | 592 (always) | - | `string` |
| `:encounter-id` | 4/592 | - | `string` |
| `:event` | 592 (always) | - | `keyword` |
| `:participants` | 592 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 592 (always) | - | `long` |
| `:warm-up` | 592 (always) | - | `boolean` |

### `:bed-status-change` (n=41399)

| key | present | nil | value shape |
|---|---|---|---|
| `:bed` | 41399 (always) | - | `string` |
| `:event` | 41399 (always) | - | `keyword` |
| `:from` | 41399 (always) | - | `keyword` |
| `:last-patient-id` | 13912/41399 | - | `string` |
| `:participants` | 41399 (always) | - | `vector<map{bed-id,role,ward}>` |
| `:t` | 41399 (always) | - | `long` |
| `:to` | 41399 (always) | - | `keyword` |
| `:ward` | 41399 (always) | - | `string` |
| `:warm-up` | 41399 (always) | - | `boolean` |

### `:bed-swap` (n=2220)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 2220 (always) | - | `keyword` |
| `:participants` | 2220 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 2220 (always) | - | `map{<patient-id> -> map{active-mrn,attending,encounter-id,from,to}}` |
| `:t` | 2220 (always) | - | `long` |
| `:warm-up` | 2220 (always) | - | `boolean` |

### `:cancel-admit` (n=54)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 54 (always) | - | `string` |
| `:cancels-event-id` | 54 (always) | - | `long` |
| `:event` | 54 (always) | - | `keyword` |
| `:participants` | 54 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 54 (always) | - | `long` |
| `:warm-up` | 54 (always) | - | `boolean` |

### `:cancel-discharge` (n=51)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 51 (always) | - | `string` |
| `:attending` | 51 (always) | - | `string` |
| `:cancels-event-id` | 51 (always) | - | `long` |
| `:encounter-id` | 51 (always) | - | `string` |
| `:event` | 51 (always) | - | `keyword` |
| `:home-ward` | 51 (always) | - | `string` |
| `:location` | 51 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 51 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 51 (always) | - | `long` |
| `:warm-up` | 51 (always) | - | `boolean` |

### `:cancel-transfer` (n=2146)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2146 (always) | - | `string` |
| `:cancels-event-id` | 2146 (always) | - | `long` |
| `:encounter-id` | 2146 (always) | - | `string` |
| `:event` | 2146 (always) | - | `keyword` |
| `:home-ward` | 2146 (always) | - | `string` |
| `:in-error` | 1429/2146 | - | `boolean` |
| `:location` | 2146 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 2146 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2146 (always) | - | `long` |
| `:warm-up` | 2146 (always) | - | `boolean` |

### `:care-plan-end` (n=3486)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3486 (always) | - | `string` |
| `:care-plan-citation` | 3486 (always) | - | `map{module,state}` |
| `:encounter-id` | 3486 (always) | - | `string` |
| `:event` | 3486 (always) | - | `keyword` |
| `:participants` | 3486 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 3486 (always) | - | `long` |
| `:t` | 3486 (always) | - | `long` |
| `:warm-up` | 3486 (always) | - | `boolean` |

### `:care-plan-start` (n=3567)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3567 (always) | - | `string` |
| `:activities` | 58/3567 | - | `vector<map{code,display,system}>` |
| `:citation` | 3567 (always) | - | `map{module,state}` |
| `:codes` | 3567 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 3567 (always) | - | `string` |
| `:event` | 3567 (always) | - | `keyword` |
| `:participants` | 3567 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 3567 (always) | - | `long` |
| `:warm-up` | 3567 (always) | - | `boolean` |

### `:coverage-change` (n=7341)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7341 (always) | - | `string` |
| `:cause` | 7341 (always) | - | `keyword` |
| `:encounter-id` | 72/7341 | - | `string` |
| `:event` | 7341 (always) | - | `keyword` |
| `:participants` | 7341 (always) | - | `vector<map{patient-id,role}>` |
| `:payer` | 7341 (always) | - | `map{id,name,type}` |
| `:person-event-id` | 7341 (always) | - | `string` |
| `:prior-payer` | 7341 (always) | - | `map{id,name,type}` |
| `:t` | 7341 (always) | - | `long` |
| `:warm-up` | 7341 (always) | - | `boolean` |

### `:demographic-update` (n=13477)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 13477 (always) | - | `string` |
| `:cause` | 13477 (always) | - | `keyword` |
| `:encounter-id` | 104/13477 | - | `string` |
| `:event` | 13477 (always) | - | `keyword` |
| `:field` | 13477 (always) | - | `keyword` |
| `:participants` | 13477 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 13477 (always) | - | `string` |
| `:persona` | 943/13477 | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:placeholder-event-id` | 943/13477 | - | `long` |
| `:prior-value` | 13477 (always) | - | `keyword` \| `map{address,status}` \| `map{family,given}` \| `map{last-known-address,status}` \| `string` |
| `:residence` | 3/13477 | - | `map{last-known-address,status}` |
| `:t` | 13477 (always) | - | `long` |
| `:value` | 13477 (always) | - | `keyword` \| `map{address,status}` \| `map{family,given}` \| `map{last-known-address,status}` \| `string` |
| `:warm-up` | 13477 (always) | - | `boolean` |

### `:diagnostic-report` (n=2446)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2446 (always) | - | `string` |
| `:codes` | 2446 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 2446 (always) | - | `string` |
| `:event` | 2446 (always) | - | `keyword` |
| `:observations` | 2446 (always) | - | `vector<map{codes,unit,value}>` |
| `:participants` | 2446 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2446 (always) | - | `long` |
| `:warm-up` | 2446 (always) | - | `boolean` |

### `:discharge` (n=8001)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 8001 (always) | - | `string` |
| `:attending` | 8001 (always) | - | `string` |
| `:citation` | 30/8001 | - | `map{module,state}` |
| `:encounter-id` | 8001 (always) | - | `string` |
| `:event` | 8001 (always) | - | `keyword` |
| `:location` | 8001 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 8001 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 8001 (always) | - | `long` |
| `:warm-up` | 8001 (always) | - | `boolean` |

### `:medication-end` (n=4885)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 4885 (always) | - | `string` |
| `:citation` | 1/4885 | - | `map{module,state}` |
| `:encounter-id` | 4884/4885 | - | `string` |
| `:event` | 4885 (always) | - | `keyword` |
| `:order-citation` | 4885 (always) | - | `map{module,state}` |
| `:order-event-id` | 4885 (always) | 1 | `long` |
| `:participants` | 4885 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 4885 (always) | - | `long` |
| `:warm-up` | 4885 (always) | - | `boolean` |

### `:medication-order` (n=4978)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 4978 (always) | - | `string` |
| `:citation` | 4978 (always) | - | `map{module,state}` |
| `:codes` | 4978 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 4978 (always) | - | `string` |
| `:event` | 4978 (always) | - | `keyword` |
| `:participants` | 4978 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 4978 (always) | - | `long` |
| `:warm-up` | 4978 (always) | - | `boolean` |

### `:merge` (n=891)

| key | present | nil | value shape |
|---|---|---|---|
| `:cause` | 119/891 | - | `keyword` |
| `:encounter-id` | 772/891 | - | `string` |
| `:event` | 891 (always) | - | `keyword` |
| `:merged-mrn` | 891 (always) | - | `string` |
| `:merged-mrns` | 891 (always) | - | `set<string>` |
| `:participants` | 891 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 119/891 | - | `string` |
| `:surviving-mrn` | 891 (always) | - | `string` |
| `:t` | 891 (always) | - | `long` |
| `:warm-up` | 891 (always) | - | `boolean` |

### `:no-show` (n=981)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 981 (always) | - | `string` |
| `:appointment-id` | 981 (always) | - | `string` |
| `:encounter-id` | 3/981 | - | `string` |
| `:event` | 981 (always) | - | `keyword` |
| `:participants` | 981 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 981 (always) | - | `long` |
| `:warm-up` | 981 (always) | - | `boolean` |

### `:observation` (n=7827)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7827 (always) | - | `string` |
| `:category` | 37/7827 | - | `string` |
| `:citation` | 37/7827 | - | `map{module,state}` |
| `:codes` | 7827 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 7827 (always) | - | `string` |
| `:event` | 7827 (always) | - | `keyword` |
| `:participants` | 7827 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 7827 (always) | - | `long` |
| `:unit` | 7827 (always) | - | `string` |
| `:value` | 7827 (always) | - | `double` |
| `:warm-up` | 7827 (always) | - | `boolean` |

### `:order-placed` (n=10274)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 10274 (always) | - | `string` |
| `:attending` | 10274 (always) | - | `string` |
| `:concept` | 10274 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 10274 (always) | - | `string` |
| `:event` | 10274 (always) | - | `keyword` |
| `:location` | 10274 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 10274 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 10274 (always) | - | `keyword` |
| `:t` | 10274 (always) | - | `long` |
| `:warm-up` | 10274 (always) | - | `boolean` |

### `:outpatient-visit` (n=2318)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2318 (always) | - | `string` |
| `:appointment-id` | 1917/2318 | - | `string` |
| `:attending` | 2318 (always) | - | `string` |
| `:citation` | 401/2318 | - | `map{module,state}` |
| `:conditions` | 53/2318 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 2318 (always) | - | `string` |
| `:event` | 2318 (always) | - | `keyword` |
| `:participants` | 2318 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 1917/2318 | - | `string` |
| `:t` | 2318 (always) | - | `long` |
| `:warm-up` | 2318 (always) | - | `boolean` |

### `:outpatient-visit-end` (n=2316)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2316 (always) | - | `string` |
| `:attending` | 2316 (always) | - | `string` |
| `:citation` | 399/2316 | - | `map{module,state}` |
| `:encounter-id` | 2316 (always) | - | `string` |
| `:event` | 2316 (always) | - | `keyword` |
| `:participants` | 2316 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2316 (always) | - | `long` |
| `:warm-up` | 2316 (always) | - | `boolean` |

### `:procedure` (n=5461)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 5461 (always) | - | `string` |
| `:citation` | 116/5461 | - | `map{module,state}` |
| `:codes` | 5461 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 5461 (always) | - | `string` |
| `:event` | 5461 (always) | - | `keyword` |
| `:participants` | 5461 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 5461 (always) | - | `long` |
| `:warm-up` | 5461 (always) | - | `boolean` |

### `:registered` (n=8521)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 8521 (always) | - | `string` |
| `:alias-name` | 1063/8521 | - | `map{family,given}` |
| `:event` | 8521 (always) | - | `keyword` |
| `:identity` | 1063/8521 | - | `keyword` |
| `:mother-patient-id` | 981/8521 | - | `string` |
| `:participants` | 8521 (always) | - | `vector<map{patient-id,role}>` |
| `:person-id` | 8521 (always) | - | `string` |
| `:persona` | 8521 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 484/8521 | - | `vector<map{citation,codes,event,references}>` |
| `:residence` | 1204/8521 | - | `map{last-known-address,status}` \| `map{status}` |
| `:t` | 8521 (always) | - | `long` |
| `:warm-up` | 8521 (always) | - | `boolean` |
| `:window-close-t` | 1062/8521 | - | `long` |

### `:reschedule` (n=770)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 770 (always) | - | `string` |
| `:appointment-id` | 770 (always) | - | `string` |
| `:encounter-id` | 8/770 | - | `string` |
| `:event` | 770 (always) | - | `keyword` |
| `:participants` | 770 (always) | - | `vector<map{patient-id,role}>` |
| `:prior-scheduled-t` | 770 (always) | - | `long` |
| `:scheduled-t` | 770 (always) | - | `long` |
| `:t` | 770 (always) | - | `long` |
| `:warm-up` | 770 (always) | - | `boolean` |

### `:result-available` (n=10274)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 10274 (always) | - | `string` |
| `:attending` | 10274 (always) | - | `string` |
| `:concept` | 10274 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 9061/10274 | - | `string` |
| `:event` | 10274 (always) | - | `keyword` |
| `:location` | 10274 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 10274 (always) | - | `long` |
| `:participants` | 10274 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 10274 (always) | - | `keyword` |
| `:results` | 10274 (always) | - | `vector<map{abnormal-flag,concept,reference-range,unit,value}>` |
| `:t` | 10274 (always) | - | `long` |
| `:warm-up` | 10274 (always) | - | `boolean` |

### `:step-rejected` (n=179)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 179 (always) | - | `map{type}` |
| `:encounter-id` | 121/179 | - | `string` |
| `:event` | 179 (always) | - | `keyword` |
| `:participants` | 179 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 179 (always) | - | `keyword` |
| `:t` | 179 (always) | - | `long` |
| `:warm-up` | 179 (always) | - | `boolean` |

### `:transfer` (n=7340)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7340 (always) | - | `string` |
| `:attending` | 7340 (always) | - | `string` |
| `:bed-ready` | 7340 (always) | - | `boolean` |
| `:encounter-id` | 7340 (always) | - | `string` |
| `:event` | 7340 (always) | - | `keyword` |
| `:forced` | 7340 (always) | - | `boolean` |
| `:from` | 7340 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 7340 (always) | - | `string` |
| `:location` | 7340 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 7340 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 2385/7340 | - | `keyword` |
| `:t` | 7340 (always) | - | `long` |
| `:warm-up` | 7340 (always) | - | `boolean` |

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

- `/home/mg/perf-out/a7500-persons.run1.edn`: :admission 8062, :appointment 7340, :appointment-cancel 592, :bed-status-change 41399, :bed-swap 2220, :cancel-admit 54, :cancel-discharge 51, :cancel-transfer 2146, :care-plan-end 3486, :care-plan-start 3567, :coverage-change 7341, :demographic-update 13477, :diagnostic-report 2446, :discharge 8001, :medication-end 4885, :medication-order 4978, :merge 891, :no-show 981, :observation 7827, :order-placed 10274, :outpatient-visit 2318, :outpatient-visit-end 2316, :procedure 5461, :registered 8521, :reschedule 770, :result-available 10274, :step-rejected 179, :transfer 7340

## Per-corpus `:t` monotonicity (a RUN-level property)

- `/home/mg/perf-out/a7500-persons.run1.edn`: `(apply <= (map :t ...))` = **true**

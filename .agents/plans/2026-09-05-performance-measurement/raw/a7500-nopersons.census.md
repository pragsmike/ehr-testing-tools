## Corpora

- `/home/mg/perf-out/a7500-nopersons.run1.edn` -- 131410 events

Total: **131410 events**, **26 kinds**.

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

### `:admission` (n=5525)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 5525 (always) | - | `string` |
| `:appointment-id` | 3491/5525 | - | `string` |
| `:attending` | 5525 (always) | - | `string` |
| `:citation` | 37/5525 | - | `map{module,state}` |
| `:conditions` | 37/5525 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 5525 (always) | - | `string` |
| `:event` | 5525 (always) | - | `keyword` |
| `:forced` | 5525 (always) | - | `boolean` |
| `:home-ward` | 5525 (always) | - | `string` |
| `:location` | 5525 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 5525 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 5488/5525 | - | `string` |
| `:t` | 5525 (always) | - | `long` |
| `:warm-up` | 5525 (always) | - | `boolean` |

### `:appointment` (n=6484)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 6484 (always) | - | `string` |
| `:appointment-class` | 6484 (always) | - | `keyword` |
| `:appointment-id` | 6484 (always) | - | `string` |
| `:event` | 6484 (always) | - | `keyword` |
| `:participants` | 6484 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 1918/6484 | - | `string` |
| `:scheduled-t` | 6484 (always) | - | `long` |
| `:t` | 6484 (always) | - | `long` |
| `:warm-up` | 6484 (always) | - | `boolean` |

### `:appointment-cancel` (n=488)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 488 (always) | - | `string` |
| `:appointment-id` | 488 (always) | - | `string` |
| `:event` | 488 (always) | - | `keyword` |
| `:participants` | 488 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 488 (always) | - | `long` |
| `:warm-up` | 488 (always) | - | `boolean` |

### `:bed-status-change` (n=33892)

| key | present | nil | value shape |
|---|---|---|---|
| `:bed` | 33892 (always) | - | `string` |
| `:event` | 33892 (always) | - | `keyword` |
| `:from` | 33892 (always) | - | `keyword` |
| `:last-patient-id` | 11401/33892 | - | `string` |
| `:participants` | 33892 (always) | - | `vector<map{bed-id,role,ward}>` |
| `:t` | 33892 (always) | - | `long` |
| `:to` | 33892 (always) | - | `keyword` |
| `:ward` | 33892 (always) | - | `string` |
| `:warm-up` | 33892 (always) | - | `boolean` |

### `:bed-swap` (n=2228)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 2228 (always) | - | `keyword` |
| `:participants` | 2228 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 2228 (always) | - | `map{<patient-id> -> map{active-mrn,attending,encounter-id,from,to}}` |
| `:t` | 2228 (always) | - | `long` |
| `:warm-up` | 2228 (always) | - | `boolean` |

### `:cancel-admit` (n=50)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 50 (always) | - | `string` |
| `:cancels-event-id` | 50 (always) | - | `long` |
| `:event` | 50 (always) | - | `keyword` |
| `:participants` | 50 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 50 (always) | - | `long` |
| `:warm-up` | 50 (always) | - | `boolean` |

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

### `:cancel-transfer` (n=2176)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2176 (always) | - | `string` |
| `:cancels-event-id` | 2176 (always) | - | `long` |
| `:encounter-id` | 2176 (always) | - | `string` |
| `:event` | 2176 (always) | - | `keyword` |
| `:home-ward` | 2176 (always) | - | `string` |
| `:in-error` | 1461/2176 | - | `boolean` |
| `:location` | 2176 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 2176 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2176 (always) | - | `long` |
| `:warm-up` | 2176 (always) | - | `boolean` |

### `:care-plan-end` (n=3541)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3541 (always) | - | `string` |
| `:care-plan-citation` | 3541 (always) | - | `map{module,state}` |
| `:encounter-id` | 3541 (always) | - | `string` |
| `:event` | 3541 (always) | - | `keyword` |
| `:participants` | 3541 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 3541 (always) | - | `long` |
| `:t` | 3541 (always) | - | `long` |
| `:warm-up` | 3541 (always) | - | `boolean` |

### `:care-plan-start` (n=3629)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3629 (always) | - | `string` |
| `:activities` | 67/3629 | - | `vector<map{code,display,system}>` |
| `:citation` | 3629 (always) | - | `map{module,state}` |
| `:codes` | 3629 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 3629 (always) | - | `string` |
| `:event` | 3629 (always) | - | `keyword` |
| `:participants` | 3629 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 3629 (always) | - | `long` |
| `:warm-up` | 3629 (always) | - | `boolean` |

### `:diagnostic-report` (n=2459)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2459 (always) | - | `string` |
| `:codes` | 2459 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 2459 (always) | - | `string` |
| `:event` | 2459 (always) | - | `keyword` |
| `:observations` | 2459 (always) | - | `vector<map{codes,unit,value}>` |
| `:participants` | 2459 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 2459 (always) | - | `long` |
| `:warm-up` | 2459 (always) | - | `boolean` |

### `:discharge` (n=5464)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 5464 (always) | - | `string` |
| `:attending` | 5464 (always) | - | `string` |
| `:citation` | 37/5464 | - | `map{module,state}` |
| `:encounter-id` | 5464 (always) | - | `string` |
| `:event` | 5464 (always) | - | `keyword` |
| `:location` | 5464 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 5464 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 5464 (always) | - | `long` |
| `:warm-up` | 5464 (always) | - | `boolean` |

### `:medication-end` (n=4915)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 4915 (always) | - | `string` |
| `:citation` | 1/4915 | - | `map{module,state}` |
| `:encounter-id` | 4914/4915 | - | `string` |
| `:event` | 4915 (always) | - | `keyword` |
| `:order-citation` | 4915 (always) | - | `map{module,state}` |
| `:order-event-id` | 4915 (always) | 1 | `long` |
| `:participants` | 4915 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 4915 (always) | - | `long` |
| `:warm-up` | 4915 (always) | - | `boolean` |

### `:medication-order` (n=5030)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 5030 (always) | - | `string` |
| `:citation` | 5030 (always) | - | `map{module,state}` |
| `:codes` | 5030 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 5030 (always) | - | `string` |
| `:event` | 5030 (always) | - | `keyword` |
| `:participants` | 5030 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 5030 (always) | - | `long` |
| `:warm-up` | 5030 (always) | - | `boolean` |

### `:merge` (n=778)

| key | present | nil | value shape |
|---|---|---|---|
| `:encounter-id` | 778 (always) | - | `string` |
| `:event` | 778 (always) | - | `keyword` |
| `:merged-mrn` | 778 (always) | - | `string` |
| `:merged-mrns` | 778 (always) | - | `set<string>` |
| `:participants` | 778 (always) | - | `vector<map{patient-id,role}>` |
| `:surviving-mrn` | 778 (always) | - | `string` |
| `:t` | 778 (always) | - | `long` |
| `:warm-up` | 778 (always) | - | `boolean` |

### `:no-show` (n=973)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 973 (always) | - | `string` |
| `:appointment-id` | 973 (always) | - | `string` |
| `:encounter-id` | 1/973 | - | `string` |
| `:event` | 973 (always) | - | `keyword` |
| `:participants` | 973 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 973 (always) | - | `long` |
| `:warm-up` | 973 (always) | - | `boolean` |

### `:observation` (n=7958)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7958 (always) | - | `string` |
| `:category` | 49/7958 | - | `string` |
| `:citation` | 49/7958 | - | `map{module,state}` |
| `:codes` | 7958 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 7958 (always) | - | `string` |
| `:event` | 7958 (always) | - | `keyword` |
| `:participants` | 7958 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 7958 (always) | - | `long` |
| `:unit` | 7958 (always) | - | `string` |
| `:value` | 7958 (always) | - | `double` |
| `:warm-up` | 7958 (always) | - | `boolean` |

### `:order-placed` (n=10406)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 10406 (always) | - | `string` |
| `:attending` | 10406 (always) | - | `string` |
| `:concept` | 10406 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 10406 (always) | - | `string` |
| `:event` | 10406 (always) | - | `keyword` |
| `:location` | 10406 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 10406 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 10406 (always) | - | `keyword` |
| `:t` | 10406 (always) | - | `long` |
| `:warm-up` | 10406 (always) | - | `boolean` |

### `:outpatient-visit` (n=1819)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1819 (always) | - | `string` |
| `:appointment-id` | 1288/1819 | - | `string` |
| `:attending` | 1819 (always) | - | `string` |
| `:citation` | 531/1819 | - | `map{module,state}` |
| `:conditions` | 58/1819 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 1819 (always) | - | `string` |
| `:event` | 1819 (always) | - | `keyword` |
| `:participants` | 1819 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 1288/1819 | - | `string` |
| `:t` | 1819 (always) | - | `long` |
| `:warm-up` | 1819 (always) | - | `boolean` |

### `:outpatient-visit-end` (n=1818)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1818 (always) | - | `string` |
| `:attending` | 1818 (always) | - | `string` |
| `:citation` | 530/1818 | - | `map{module,state}` |
| `:encounter-id` | 1818 (always) | - | `string` |
| `:event` | 1818 (always) | - | `keyword` |
| `:participants` | 1818 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1818 (always) | - | `long` |
| `:warm-up` | 1818 (always) | - | `boolean` |

### `:procedure` (n=5581)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 5581 (always) | - | `string` |
| `:citation` | 132/5581 | - | `map{module,state}` |
| `:codes` | 5581 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 5581 (always) | - | `string` |
| `:event` | 5581 (always) | - | `keyword` |
| `:participants` | 5581 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 5581 (always) | - | `long` |
| `:warm-up` | 5581 (always) | - | `boolean` |

### `:registered` (n=7500)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7500 (always) | - | `string` |
| `:event` | 7500 (always) | - | `keyword` |
| `:participants` | 7500 (always) | - | `vector<map{patient-id,role}>` |
| `:persona` | 7500 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 603/7500 | - | `vector<map{citation,codes,event,references}>` |
| `:t` | 7500 (always) | - | `long` |
| `:warm-up` | 7500 (always) | - | `boolean` |

### `:reschedule` (n=663)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 663 (always) | - | `string` |
| `:appointment-id` | 663 (always) | - | `string` |
| `:event` | 663 (always) | - | `keyword` |
| `:participants` | 663 (always) | - | `vector<map{patient-id,role}>` |
| `:prior-scheduled-t` | 663 (always) | - | `long` |
| `:scheduled-t` | 663 (always) | - | `long` |
| `:t` | 663 (always) | - | `long` |
| `:warm-up` | 663 (always) | - | `boolean` |

### `:result-available` (n=10406)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 10406 (always) | - | `string` |
| `:attending` | 10406 (always) | - | `string` |
| `:concept` | 10406 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 9142/10406 | - | `string` |
| `:event` | 10406 (always) | - | `keyword` |
| `:location` | 10406 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 10406 (always) | - | `long` |
| `:participants` | 10406 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 10406 (always) | - | `keyword` |
| `:results` | 10406 (always) | - | `vector<map{abnormal-flag,concept,reference-range,unit,value}>` |
| `:t` | 10406 (always) | - | `long` |
| `:warm-up` | 10406 (always) | - | `boolean` |

### `:step-rejected` (n=178)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 178 (always) | - | `map{type}` |
| `:encounter-id` | 124/178 | - | `string` |
| `:event` | 178 (always) | - | `keyword` |
| `:participants` | 178 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 178 (always) | - | `keyword` |
| `:t` | 178 (always) | - | `long` |
| `:warm-up` | 178 (always) | - | `boolean` |

### `:transfer` (n=7398)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 7398 (always) | - | `string` |
| `:attending` | 7398 (always) | - | `string` |
| `:bed-ready` | 7398 (always) | - | `boolean` |
| `:encounter-id` | 7398 (always) | - | `string` |
| `:event` | 7398 (always) | - | `keyword` |
| `:forced` | 7398 (always) | - | `boolean` |
| `:from` | 7398 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 7398 (always) | - | `string` |
| `:location` | 7398 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 7398 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 2359/7398 | - | `keyword` |
| `:t` | 7398 (always) | - | `long` |
| `:warm-up` | 7398 (always) | - | `boolean` |

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

- `/home/mg/perf-out/a7500-nopersons.run1.edn`: :admission 5525, :appointment 6484, :appointment-cancel 488, :bed-status-change 33892, :bed-swap 2228, :cancel-admit 50, :cancel-discharge 51, :cancel-transfer 2176, :care-plan-end 3541, :care-plan-start 3629, :diagnostic-report 2459, :discharge 5464, :medication-end 4915, :medication-order 5030, :merge 778, :no-show 973, :observation 7958, :order-placed 10406, :outpatient-visit 1819, :outpatient-visit-end 1818, :procedure 5581, :registered 7500, :reschedule 663, :result-available 10406, :step-rejected 178, :transfer 7398

## Per-corpus `:t` monotonicity (a RUN-level property)

- `/home/mg/perf-out/a7500-nopersons.run1.edn`: `(apply <= (map :t ...))` = **true**

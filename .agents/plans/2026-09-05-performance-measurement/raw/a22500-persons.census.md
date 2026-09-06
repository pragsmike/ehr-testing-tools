## Corpora

- `/home/mg/perf-out/a22500-persons.run1.edn` -- 533147 events

Total: **533147 events**, **28 kinds**.

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

### `:admission` (n=25697)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 25697 (always) | - | `string` |
| `:appointment-id` | 11135/25697 | - | `string` |
| `:attending` | 25697 (always) | - | `string` |
| `:citation` | 37/25697 | - | `map{module,state}` |
| `:conditions` | 37/25697 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 25697 (always) | - | `string` |
| `:event` | 25697 (always) | - | `keyword` |
| `:forced` | 25697 (always) | - | `boolean` |
| `:home-ward` | 25697 (always) | - | `string` |
| `:location` | 25697 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 25697 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 8057/25697 | - | `string` |
| `:reason` | 25660/25697 | - | `string` |
| `:t` | 25697 (always) | - | `long` |
| `:warm-up` | 25697 (always) | - | `boolean` |

### `:appointment` (n=23648)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 23648 (always) | - | `string` |
| `:appointment-class` | 23648 (always) | - | `keyword` |
| `:appointment-id` | 23648 (always) | - | `string` |
| `:encounter-id` | 57/23648 | - | `string` |
| `:event` | 23648 (always) | - | `keyword` |
| `:participants` | 23648 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 8867/23648 | - | `string` |
| `:scheduled-t` | 23648 (always) | - | `long` |
| `:t` | 23648 (always) | - | `long` |
| `:warm-up` | 23648 (always) | - | `boolean` |

### `:appointment-cancel` (n=1885)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 1885 (always) | - | `string` |
| `:appointment-id` | 1885 (always) | - | `string` |
| `:encounter-id` | 6/1885 | - | `string` |
| `:event` | 1885 (always) | - | `keyword` |
| `:participants` | 1885 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 1885 (always) | - | `long` |
| `:warm-up` | 1885 (always) | - | `boolean` |

### `:bed-status-change` (n=133356)

| key | present | nil | value shape |
|---|---|---|---|
| `:bed` | 133356 (always) | - | `string` |
| `:event` | 133356 (always) | - | `keyword` |
| `:from` | 133356 (always) | - | `keyword` |
| `:last-patient-id` | 44840/133356 | - | `string` |
| `:participants` | 133356 (always) | - | `vector<map{bed-id,role,ward}>` |
| `:t` | 133356 (always) | - | `long` |
| `:to` | 133356 (always) | - | `keyword` |
| `:ward` | 133356 (always) | - | `string` |
| `:warm-up` | 133356 (always) | - | `boolean` |

### `:bed-swap` (n=7145)

| key | present | nil | value shape |
|---|---|---|---|
| `:event` | 7145 (always) | - | `keyword` |
| `:participants` | 7145 (always) | - | `vector<map{patient-id,role}>` |
| `:swap` | 7145 (always) | - | `map{<patient-id> -> map{active-mrn,attending,encounter-id,from,to}}` |
| `:t` | 7145 (always) | - | `long` |
| `:warm-up` | 7145 (always) | - | `boolean` |

### `:cancel-admit` (n=173)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 173 (always) | - | `string` |
| `:cancels-event-id` | 173 (always) | - | `long` |
| `:event` | 173 (always) | - | `keyword` |
| `:participants` | 173 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 173 (always) | - | `long` |
| `:warm-up` | 173 (always) | - | `boolean` |

### `:cancel-discharge` (n=174)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 174 (always) | - | `string` |
| `:attending` | 174 (always) | - | `string` |
| `:cancels-event-id` | 174 (always) | - | `long` |
| `:encounter-id` | 174 (always) | - | `string` |
| `:event` | 174 (always) | - | `keyword` |
| `:home-ward` | 174 (always) | - | `string` |
| `:location` | 174 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 174 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 174 (always) | - | `long` |
| `:warm-up` | 174 (always) | - | `boolean` |

### `:cancel-transfer` (n=6252)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 6252 (always) | - | `string` |
| `:cancels-event-id` | 6252 (always) | - | `long` |
| `:encounter-id` | 6252 (always) | - | `string` |
| `:event` | 6252 (always) | - | `keyword` |
| `:home-ward` | 6252 (always) | - | `string` |
| `:in-error` | 4824/6252 | - | `boolean` |
| `:location` | 6252 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 6252 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 6252 (always) | - | `long` |
| `:warm-up` | 6252 (always) | - | `boolean` |

### `:care-plan-end` (n=11439)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 11439 (always) | - | `string` |
| `:care-plan-citation` | 11439 (always) | - | `map{module,state}` |
| `:encounter-id` | 11439 (always) | - | `string` |
| `:event` | 11439 (always) | - | `keyword` |
| `:participants` | 11439 (always) | - | `vector<map{patient-id,role}>` |
| `:start-event-id` | 11439 (always) | - | `long` |
| `:t` | 11439 (always) | - | `long` |
| `:warm-up` | 11439 (always) | - | `boolean` |

### `:care-plan-start` (n=11552)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 11552 (always) | - | `string` |
| `:activities` | 65/11552 | - | `vector<map{code,display,system}>` |
| `:citation` | 11552 (always) | - | `map{module,state}` |
| `:codes` | 11552 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 11552 (always) | - | `string` |
| `:event` | 11552 (always) | - | `keyword` |
| `:participants` | 11552 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 11552 (always) | - | `long` |
| `:warm-up` | 11552 (always) | - | `boolean` |

### `:coverage-change` (n=21937)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 21937 (always) | - | `string` |
| `:cause` | 21937 (always) | - | `keyword` |
| `:encounter-id` | 218/21937 | - | `string` |
| `:event` | 21937 (always) | - | `keyword` |
| `:participants` | 21937 (always) | - | `vector<map{patient-id,role}>` |
| `:payer` | 21937 (always) | - | `map{id,name,type}` |
| `:person-event-id` | 21937 (always) | - | `string` |
| `:prior-payer` | 21937 (always) | - | `map{id,name,type}` |
| `:t` | 21937 (always) | - | `long` |
| `:warm-up` | 21937 (always) | - | `boolean` |

### `:demographic-update` (n=40578)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 40578 (always) | - | `string` |
| `:cause` | 40578 (always) | - | `keyword` |
| `:encounter-id` | 284/40578 | - | `string` |
| `:event` | 40578 (always) | - | `keyword` |
| `:field` | 40578 (always) | - | `keyword` |
| `:participants` | 40578 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 40578 (always) | - | `string` |
| `:persona` | 2901/40578 | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:placeholder-event-id` | 2901/40578 | - | `long` |
| `:prior-value` | 40578 (always) | - | `keyword` \| `map{address,status}` \| `map{family,given}` \| `map{last-known-address,status}` \| `string` |
| `:residence` | 25/40578 | - | `map{last-known-address,status}` |
| `:t` | 40578 (always) | - | `long` |
| `:value` | 40578 (always) | - | `keyword` \| `map{address,status}` \| `map{family,given}` \| `map{last-known-address,status}` \| `string` |
| `:warm-up` | 40578 (always) | - | `boolean` |

### `:diagnostic-report` (n=8043)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 8043 (always) | - | `string` |
| `:codes` | 8043 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 8043 (always) | - | `string` |
| `:event` | 8043 (always) | - | `keyword` |
| `:observations` | 8043 (always) | - | `vector<map{codes,unit,value}>` |
| `:participants` | 8043 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 8043 (always) | - | `long` |
| `:warm-up` | 8043 (always) | - | `boolean` |

### `:discharge` (n=25554)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 25554 (always) | - | `string` |
| `:attending` | 25554 (always) | - | `string` |
| `:citation` | 37/25554 | - | `map{module,state}` |
| `:encounter-id` | 25554 (always) | - | `string` |
| `:event` | 25554 (always) | - | `keyword` |
| `:location` | 25554 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 25554 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 25554 (always) | - | `long` |
| `:warm-up` | 25554 (always) | - | `boolean` |

### `:medication-end` (n=16072)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 16072 (always) | - | `string` |
| `:encounter-id` | 16072 (always) | - | `string` |
| `:event` | 16072 (always) | - | `keyword` |
| `:order-citation` | 16072 (always) | - | `map{module,state}` |
| `:order-event-id` | 16072 (always) | - | `long` |
| `:participants` | 16072 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 16072 (always) | - | `long` |
| `:warm-up` | 16072 (always) | - | `boolean` |

### `:medication-order` (n=16197)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 16197 (always) | - | `string` |
| `:citation` | 16197 (always) | - | `map{module,state}` |
| `:codes` | 16197 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 16197 (always) | - | `string` |
| `:event` | 16197 (always) | - | `keyword` |
| `:participants` | 16197 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 16197 (always) | - | `long` |
| `:warm-up` | 16197 (always) | - | `boolean` |

### `:merge` (n=2850)

| key | present | nil | value shape |
|---|---|---|---|
| `:cause` | 376/2850 | - | `keyword` |
| `:encounter-id` | 2481/2850 | - | `string` |
| `:event` | 2850 (always) | - | `keyword` |
| `:merged-mrn` | 2850 (always) | - | `string` |
| `:merged-mrns` | 2850 (always) | - | `set<string>` |
| `:participants` | 2850 (always) | - | `vector<map{patient-id,role}>` |
| `:person-event-id` | 376/2850 | - | `string` |
| `:surviving-mrn` | 2850 (always) | - | `string` |
| `:t` | 2850 (always) | - | `long` |
| `:warm-up` | 2850 (always) | - | `boolean` |

### `:no-show` (n=3300)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 3300 (always) | - | `string` |
| `:appointment-id` | 3300 (always) | - | `string` |
| `:encounter-id` | 19/3300 | - | `string` |
| `:event` | 3300 (always) | - | `keyword` |
| `:participants` | 3300 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 3300 (always) | - | `long` |
| `:warm-up` | 3300 (always) | - | `boolean` |

### `:observation` (n=25599)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 25599 (always) | - | `string` |
| `:category` | 47/25599 | - | `string` |
| `:citation` | 47/25599 | - | `map{module,state}` |
| `:codes` | 25599 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 25599 (always) | - | `string` |
| `:event` | 25599 (always) | - | `keyword` |
| `:participants` | 25599 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 25599 (always) | - | `long` |
| `:unit` | 25599 (always) | - | `string` |
| `:value` | 25599 (always) | - | `double` |
| `:warm-up` | 25599 (always) | - | `boolean` |

### `:order-placed` (n=33681)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 33681 (always) | - | `string` |
| `:attending` | 33681 (always) | - | `string` |
| `:concept` | 33681 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 33681 (always) | - | `string` |
| `:event` | 33681 (always) | - | `keyword` |
| `:location` | 33681 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 33681 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 33681 (always) | - | `keyword` |
| `:t` | 33681 (always) | - | `long` |
| `:warm-up` | 33681 (always) | - | `boolean` |

### `:outpatient-visit` (n=6453)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 6453 (always) | - | `string` |
| `:appointment-id` | 5992/6453 | - | `string` |
| `:attending` | 6453 (always) | - | `string` |
| `:citation` | 461/6453 | - | `map{module,state}` |
| `:conditions` | 58/6453 | - | `vector<map{citation,codes,event,references}>` |
| `:encounter-id` | 6453 (always) | - | `string` |
| `:event` | 6453 (always) | - | `keyword` |
| `:participants` | 6453 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 5992/6453 | - | `string` |
| `:t` | 6453 (always) | - | `long` |
| `:warm-up` | 6453 (always) | - | `boolean` |

### `:outpatient-visit-end` (n=6453)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 6453 (always) | - | `string` |
| `:attending` | 6453 (always) | - | `string` |
| `:citation` | 461/6453 | - | `map{module,state}` |
| `:encounter-id` | 6453 (always) | - | `string` |
| `:event` | 6453 (always) | - | `keyword` |
| `:participants` | 6453 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 6453 (always) | - | `long` |
| `:warm-up` | 6453 (always) | - | `boolean` |

### `:procedure` (n=17659)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 17659 (always) | - | `string` |
| `:citation` | 137/17659 | - | `map{module,state}` |
| `:codes` | 17659 (always) | - | `vector<map{code,display,system}>` |
| `:encounter-id` | 17659 (always) | - | `string` |
| `:event` | 17659 (always) | - | `keyword` |
| `:participants` | 17659 (always) | - | `vector<map{patient-id,role}>` |
| `:t` | 17659 (always) | - | `long` |
| `:warm-up` | 17659 (always) | - | `boolean` |

### `:registered` (n=25781)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 25781 (always) | - | `string` |
| `:alias-name` | 3286/25781 | - | `map{family,given}` |
| `:event` | 25781 (always) | - | `keyword` |
| `:identity` | 3286/25781 | - | `keyword` |
| `:mother-patient-id` | 3004/25781 | - | `string` |
| `:participants` | 25781 (always) | - | `vector<map{patient-id,role}>` |
| `:person-id` | 25781 (always) | - | `string` |
| `:persona` | 25781 (always) | - | `map{address,age,dob,name,payer,phone,sex,ssn}` |
| `:pre-horizon-facts` | 548/25781 | - | `vector<map{citation,codes,event,references}>` |
| `:residence` | 3679/25781 | - | `map{last-known-address,status}` \| `map{status}` |
| `:t` | 25781 (always) | - | `long` |
| `:warm-up` | 25781 (always) | - | `boolean` |
| `:window-close-t` | 3280/25781 | - | `long` |

### `:reschedule` (n=2445)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 2445 (always) | - | `string` |
| `:appointment-id` | 2445 (always) | - | `string` |
| `:encounter-id` | 5/2445 | - | `string` |
| `:event` | 2445 (always) | - | `keyword` |
| `:participants` | 2445 (always) | - | `vector<map{patient-id,role}>` |
| `:prior-scheduled-t` | 2445 (always) | - | `long` |
| `:scheduled-t` | 2445 (always) | - | `long` |
| `:t` | 2445 (always) | - | `long` |
| `:warm-up` | 2445 (always) | - | `boolean` |

### `:result-available` (n=33681)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 33681 (always) | - | `string` |
| `:attending` | 33681 (always) | - | `string` |
| `:concept` | 33681 (always) | - | `map{code,display,system}` |
| `:encounter-id` | 29668/33681 | - | `string` |
| `:event` | 33681 (always) | - | `keyword` |
| `:location` | 33681 (always) | - | `map{bed,placement,ward}` |
| `:order-event-id` | 33681 (always) | - | `long` |
| `:participants` | 33681 (always) | - | `vector<map{patient-id,role}>` |
| `:profile` | 33681 (always) | - | `keyword` |
| `:results` | 33681 (always) | - | `vector<map{abnormal-flag,concept,reference-range,unit,value}>` |
| `:t` | 33681 (always) | - | `long` |
| `:warm-up` | 33681 (always) | - | `boolean` |

### `:step-rejected` (n=1433)

| key | present | nil | value shape |
|---|---|---|---|
| `:attempted-step` | 1433 (always) | - | `map{type}` |
| `:encounter-id` | 1245/1433 | - | `string` |
| `:event` | 1433 (always) | - | `keyword` |
| `:participants` | 1433 (always) | - | `vector<map{patient-id,role}>` |
| `:reason` | 1433 (always) | - | `keyword` |
| `:t` | 1433 (always) | - | `long` |
| `:warm-up` | 1433 (always) | - | `boolean` |

### `:transfer` (n=24110)

| key | present | nil | value shape |
|---|---|---|---|
| `:active-mrn` | 24110 (always) | - | `string` |
| `:attending` | 24110 (always) | - | `string` |
| `:bed-ready` | 24110 (always) | - | `boolean` |
| `:encounter-id` | 24110 (always) | - | `string` |
| `:event` | 24110 (always) | - | `keyword` |
| `:forced` | 24110 (always) | - | `boolean` |
| `:from` | 24110 (always) | - | `map{bed,placement,ward}` |
| `:home-ward` | 24110 (always) | - | `string` |
| `:location` | 24110 (always) | - | `map{bed,placement,ward}` |
| `:participants` | 24110 (always) | - | `vector<map{patient-id,role}>` |
| `:placement` | 7755/24110 | - | `keyword` |
| `:t` | 24110 (always) | - | `long` |
| `:warm-up` | 24110 (always) | - | `boolean` |

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

- `/home/mg/perf-out/a22500-persons.run1.edn`: :admission 25697, :appointment 23648, :appointment-cancel 1885, :bed-status-change 133356, :bed-swap 7145, :cancel-admit 173, :cancel-discharge 174, :cancel-transfer 6252, :care-plan-end 11439, :care-plan-start 11552, :coverage-change 21937, :demographic-update 40578, :diagnostic-report 8043, :discharge 25554, :medication-end 16072, :medication-order 16197, :merge 2850, :no-show 3300, :observation 25599, :order-placed 33681, :outpatient-visit 6453, :outpatient-visit-end 6453, :procedure 17659, :registered 25781, :reschedule 2445, :result-available 33681, :step-rejected 1433, :transfer 24110

## Per-corpus `:t` monotonicity (a RUN-level property)

- `/home/mg/perf-out/a22500-persons.run1.edn`: `(apply <= (map :t ...))` = **true**

# Engine extraction census — the namespace map and the apply-path inventory

Serves `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5) and, for section 4, `roadmap.md#event-stream-mutation` (P6), which
names the unified apply path as its injection point.

**Every line number in this file is at `517a96d`**, re-derived in the
session that wrote it and never copied from a design channel. Sizes at
that sha: `engine.clj` 4,884 lines / 157 top-level forms (plus the `ns`),
`emit_hl7.clj` 2,498 lines / 86 top-level forms (plus the `ns`). The P5
row's own 4,884 (at `da21a28`) and 2,498 still hold; nothing has moved
either file since.

**Population closure.** Sections 1 and 2 list EVERY top-level form of
each file exactly once — 157 and 86 — so a cluster cannot be proposed
by leaving something out. The engine's 32 `decide` methods and 27
`evolve` methods are each named in their own cluster's list.

**What a later session takes.** One cluster per session, in a
dependency-respecting order (section 3's DAG), under author ruling
C1(a): `engine.clj` remains the namespace every existing requirer
resolves against, moved public vars get delegating defs, and no test
file changes until a ruled repoint pass. Section 5 carries the
constraints a mover must not break.

## 1. `engine.clj` — proposed namespace map

Ten clusters. Proposed namespace is `ehrt.sim-engine.<cluster>` for
each, except `run`, whose contents are what `engine.clj` keeps as the
facade under C1(a) (it is listed as a cluster so the population closes,
not as a namespace to create).

| cluster | forms | lines | what it is |
|---|---:|---:|---|
| `streams` | 16 | 284 | RNG stream partition, draw primitives, deterministic id minting. **Extracted this session.** |
| `state` | 13 | 365 | the patient accumulator: `PatientState` and the five records it nests, plus its two constructors |
| `encounters` | 10 | 173 | encounter records: the opt-in gate, the stamp, and the four lifecycle transitions `evolve` applies |
| `evolve` | 32 | 493 | the fold multimethod and its 27 methods, plus three private fold helpers |
| `fold` | 3 | 110 | the derived-state fold: `replay`, the bed index it does NOT maintain (section 4), and that index's correction table |
| `log-index` | 10 | 202 | queries over the ground-truth log: the two cancel/citation scans and the reinstatement machinery |
| `decide` | 59 | 1,760 | the decide multimethod, its 32 methods, and 26 private helpers |
| `assignment` | 3 | 74 | weighted pathway/module assignment |
| `config` | 5 | 144 | `config-keys` and the two opt-in schemas |
| `run` | 6 | 1,278 | the pre-loop (`prelude`, 612 lines), `person-plan`, and `run`'s own loop (594 lines) |

Forms, with line spans at `517a96d`:

**`streams`** — 16 forms, 284 lines

  * `467-471` `defn- rand-int-in`
  * `472-488` `defn mix64`
  * `489-504` `defn patient-id-for`
  * `505-534` `defn encounter-id-for`
  * `535-544` `defn next-encounter-ordinal`
  * `545-571` `defn appointment-id-for`
  * `572-594` `defn next-appointment-ordinal`
  * `595-612` `defn- minted-appointment-id-field`
  * `613-629` `defn- minted-encounter-id-field`
  * `772-791` `def stream-scheme`
  * `792-827` `def stream-family-tag`
  * `828-847` `defn stream-seed`
  * `848-859` `defn stream`
  * `860-878` `defn newborn-id-tag`
  * `879-888` `defn one-stream`
  * `2165-2168` `defn- uniform-choice`

**`state`** — 13 forms, 365 lines

  * `102-124` `def ConditionRecord`
  * `125-149` `def ObservationRecord`
  * `150-163` `def MedicationOrderRecord`
  * `164-180` `def CarePlanRecord`
  * `181-234` `def Demographics`
  * `235-255` `defn demographics-from-persona`
  * `256-270` `defn placeholder-demographics`
  * `271-280` `def PatientLocation`
  * `281-333` `def EncounterRecord`
  * `334-362` `def AppointmentRecord`
  * `363-450` `def PatientState`
  * `451-456` `defn valid-patient?`
  * `457-466` `defn initial-patient`

**`encounters`** — 10 forms, 203 lines

  * `630-650` `defn- encounter-openable?`
  * `651-657` `def compiled-encounter-openers`
  * `658-666` `def compiled-encounter-closers`
  * `667-722` `defn- gate-compiled-encounters`
  * `723-734` `def two-encounter-event-types`
  * `735-771` `defn- stamp-encounter`
  * `2587-2600` `defn- open-encounter`
  * `2601-2619` `defn- close-encounter`
  * `2620-2631` `defn- cancel-open-encounter`
  * `2632-2647` `defn- reopen-encounter`

**`evolve`** — 32 forms, 428 lines

  * `2541-2571` `defn- fold-condition-annotation`
  * `2572-2586` `defn- fold-conditions`
  * `2648-2658` `defmulti evolve`
  * `2659-2694` `defmethod evolve :registered`
  * `2695-2711` `defmethod evolve :demographic-update`
  * `2712-2716` `defmethod evolve :coverage-change`
  * `2717-2749` `defn- resolve-appointment`
  * `2750-2756` `defn- keep-appointment`
  * `2757-2778` `defmethod evolve :admission`
  * `2779-2782` `defmethod evolve :transfer`
  * `2783-2808` `defmethod evolve :discharge`
  * `2809-2815` `defmethod evolve :cancel-admit`
  * `2816-2819` `defmethod evolve :cancel-transfer`
  * `2820-2842` `defmethod evolve :cancel-discharge`
  * `2843-2846` `defmethod evolve :bed-swap`
  * `2847-2856` `defmethod evolve :merge`
  * `2857-2864` `defmethod evolve :step-rejected`
  * `2865-2868` `defmethod evolve :order-placed`
  * `2869-2890` `defmethod evolve :result-available`
  * `2891-2901` `defmethod evolve :outpatient-visit`
  * `2902-2913` `defmethod evolve :outpatient-visit-end`
  * `2914-2933` `defmethod evolve :appointment`
  * `2934-2946` `defmethod evolve :reschedule`
  * `2947-2950` `defmethod evolve :appointment-cancel`
  * `2951-2959` `defmethod evolve :no-show`
  * `2960-2967` `defmethod evolve :procedure`
  * `2968-2978` `defmethod evolve :observation`
  * `2979-2986` `defmethod evolve :diagnostic-report`
  * `2987-2992` `defmethod evolve :medication-order`
  * `2993-3014` `defmethod evolve :medication-end`
  * `3015-3021` `defmethod evolve :care-plan-start`
  * `3022-3029` `defmethod evolve :care-plan-end`

**`fold`** — 3 forms, 110 lines

  * `3030-3063` `def bed-correction-event-types`
  * `3064-3097` `defn- update-beds`
  * `3098-3139` `defn replay`

**`log-index`** — 10 forms, 230 lines

  * `889-898` `defn events-for-patient`
  * `2059-2074` `def reinstatable-event-types`
  * `2075-2094` `defn- last-uncancelled-index`
  * `2409-2416` `def cited-opening-event-types`
  * `2417-2459` `defn- last-cited-index`
  * `3140-3154` `defn- bed-reoccupied-by-someone-else?`
  * `3155-3170` `def status-a-cancel-target-leaves`
  * `3171-3194` `def statuses-that-supersede-a-reinstatement`
  * `3195-3228` `defn- subject-superseded?`
  * `3229-3272` `defn- reinstated-state`

**`decide`** — 59 forms, 1613 lines

  * `899-919` `defmulti decide`
  * `920-976` `defn- exhausted-outcome`
  * `977-1046` `defn compile-patient`
  * `1047-1123` `defmethod decide :registered`
  * `1124-1134` `defn- citation-fields`
  * `1135-1156` `defn- reason-field`
  * `1157-1194` `defn- person-stamp-field`
  * `1195-1207` `defn- demographic-target`
  * `1208-1223` `defmethod decide :demographic-update`
  * `1224-1254` `defmethod decide :coverage-change`
  * `1255-1271` `def delivery-stay-minutes`
  * `1272-1277` `def injury-stay-minutes`
  * `1278-1284` `def unidentified-stay-minutes`
  * `1285-1295` `defn- hook-ward`
  * `1296-1335` `defmethod decide :person-encounter`
  * `1336-1399` `defmethod decide :repeat-arrival`
  * `1400-1410` `defn- appointment-outcome`
  * `1411-1412` `defn- days->seconds`
  * `1413-1493` `defmethod decide :appointment`
  * `1494-1505` `defmethod decide :no-show`
  * `1506-1530` `defn- identity-fill-outcome`
  * `1531-1555` `defmethod decide :identity-fill`
  * `1556-1630` `defmethod decide :identification-merge`
  * `1631-1636` `defn- bed-status`
  * `1637-1656` `defn- bed-status-change`
  * `1657-1672` `defn- turnaround-seconds`
  * `1673-1699` `defn- vacate-bed`
  * `1700-1759` `defn- waiting-boarder`
  * `1760-1769` `defn- appointment-ref-field`
  * `1770-1802` `defmethod decide :admission`
  * `1803-1824` `defmethod decide :delay`
  * `1825-1849` `defmethod decide :transfer`
  * `1850-1859` `defn- death-disposition-fields`
  * `1860-1910` `defn- bed-ready-location`
  * `1911-1929` `defn- bed-ready-transfer-event`
  * `1930-2020` `defmethod decide :discharge`
  * `2021-2030` `defmethod decide :bed-cleaning`
  * `2031-2058` `defmethod decide :bed-ready`
  * `2095-2108` `def documented-step-rejection-reasons`
  * `2109-2128` `defn- rejected-outcome`
  * `2129-2140` `defmethod decide :cancel-admit`
  * `2141-2164` `defmethod decide :transfer-in-error`
  * `2169-2203` `defmethod decide :bed-swap`
  * `2204-2240` `defmethod decide :merge`
  * `2241-2303` `defmethod decide :order`
  * `2304-2310` `defmethod decide :result-followup`
  * `2311-2334` `defmethod decide :outpatient-visit`
  * `2335-2351` `defmethod decide :outpatient-visit-end`
  * `2352-2359` `defmethod decide :procedure`
  * `2360-2376` `defn- observation-value-fields`
  * `2377-2391` `defmethod decide :observation`
  * `2392-2400` `defmethod decide :diagnostic-report`
  * `2401-2408` `defmethod decide :medication-order`
  * `2460-2480` `defn person-entry`
  * `2481-2514` `defmethod decide :medication-end`
  * `2515-2523` `defmethod decide :care-plan-start`
  * `2524-2540` `defmethod decide :care-plan-end`
  * `3273-3303` `defmethod decide :cancel-transfer`
  * `3304-3334` `defmethod decide :cancel-discharge`

**`assignment`** — 3 forms, 74 lines

  * `3335-3354` `defn- weighted-pick`
  * `3355-3386` `defn assign-pathway`
  * `3387-3408` `defn assign-module`

**`config`** — 5 forms, 144 lines

  * `3417-3455` `def config-keys`
  * `3456-3494` `def Persons`
  * `3495-3535` `def Scheduling`
  * `3536-3552` `defn valid-scheduling?`
  * `3553-3560` `defn valid-persons?`

**`run`** — 6 forms, 1333 lines

  * `3409-3416` `defn- pop-min`
  * `3561-3592` `defn- placeholder-registration`
  * `3593-3621` `defn- select-person`
  * `3622-4233` `defn- prelude`
  * `4234-4290` `defn person-plan`
  * `4291-4885` `defn run`

## 2. `emit_hl7.clj` — proposed namespace map

Eight clusters, `ehrt.sim-emit-hl7.<cluster>`; `facade` is what
`emit_hl7.clj` keeps.

| cluster | forms | lines | what it is |
|---|---:|---:|---|
| `hl7-time` | 7 | 54 | the reference clock, MSH-7 rendering, and `transmit-seconds`' second-clock shift |
| `registry` | 13 | 291 | the message-type catalog and the four kind-sets/ladders that select from it |
| `timelines` | 5 | 130 | state-at-instant views over the log (demographics, MRN, encounter spans) |
| `er7` | 19 | 176 | escaping, primitive field composition, Z-segment rendering |
| `segments` | 15 | 434 | the thirteen segment builders, `control-id-for`, `charge-concept` |
| `messages` | 13 | 549 | the twelve per-kind message builders and `event->messages` |
| `planners` | 11 | 335 | the arc-4 add-on planners: latency, chatter, charges, status ladders |
| `facade` | 3 | 151 | `default-providers`, `emit`, `emit-wire` |

Forms, with line spans at `517a96d`:

**`hl7-time`** — 7 forms, 54 lines

  * `30-37` `def default-reference-date`
  * `38-44` `def default-utc-offset`
  * `225-227` `def hl7-timestamp-formatter`
  * `228-231` `defn- reference-instant`
  * `232-237` `defn- hl7-offset-suffix`
  * `238-250` `defn hl7-timestamp`
  * `762-774` `defn- transmit-seconds`

**`registry`** — 13 forms, 291 lines

  * `45-177` `def message-type-registry`
  * `178-195` `def skeleton-message-types`
  * `196-213` `def add-on-message-types`
  * `214-224` `def emittable-message-types`
  * `924-932` `def siu-event-kinds`
  * `933-956` `defn siu-renders?`
  * `957-968` `def siu-filler-status`
  * `1560-1569` `def room-and-board-code`
  * `1570-1578` `def charge-closing-kinds`
  * `1945-1961` `def chatter-event-kinds`
  * `2252-2266` `def order-status-ladder`
  * `2267-2276` `def result-status-ladder`
  * `2277-2281` `def final-result-stage`

**`timelines`** — 5 forms, 156 lines

  * `515-595` `defn- demographics-timeline`
  * `596-611` `defn- demographics-at`
  * `1886-1914` `defn- encounter-spans`
  * `1915-1934` `defn- mrn-timeline`
  * `1935-1944` `defn- mrn-at`

**`er7`** — 19 forms, 217 lines

  * `342-347` `def er7-escape-table`
  * `348-360` `defn escape-er7`
  * `361-363` `def er7-decode-map`
  * `364-382` `defn unescape-er7`
  * `383-390` `defn- xpn-field`
  * `391-399` `defn- xad-field`
  * `400-432` `defn- tn-field`
  * `612-622` `defn- location-field`
  * `623-629` `defn- provider-field`
  * `630-633` `defn- provider-by-id`
  * `642-645` `defn- blank-fields`
  * `704-717` `defn- context-for-event`
  * `718-729` `defn- render-z-field`
  * `730-734` `defn- z-segment-for`
  * `735-761` `defn- z-segments-for`
  * `1118-1132` `defn- cwe-field`
  * `1133-1143` `def code-system->hl7-table-0396`
  * `1144-1150` `defn- coded-value-field`
  * `1591-1599` `defn- money`

**`segments`** — 15 forms, 559 lines

  * `251-294` `defn control-id-for`
  * `295-319` `defn- msh-segment`
  * `320-341` `defn- evn-segment`
  * `433-490` `defn- pid-segment`
  * `491-514` `defn- in1-segment`
  * `634-641` `defn- mrg-segment`
  * `646-703` `defn- pv1-segment`
  * `856-867` `defn- npu-segment`
  * `969-1023` `defn- sch-segment`
  * `1151-1185` `defn- orc-segment`
  * `1186-1245` `defn- obr-segment`
  * `1246-1292` `defn- obx-segment`
  * `1421-1479` `defn- observation-obx-segment`
  * `1579-1590` `defn- charge-concept`
  * `1600-1639` `defn- ft1-segment`

**`messages`** — 13 forms, 623 lines

  * `775-822` `defn- single-subject-message`
  * `823-855` `defn- bed-swap-message`
  * `868-923` `defn- bed-status-message`
  * `1024-1090` `defn- siu-message`
  * `1091-1117` `defn- merge-message`
  * `1293-1352` `defn- orm-message`
  * `1353-1420` `defn- oru-message`
  * `1480-1517` `defn- observation-message`
  * `1518-1559` `defn- diagnostic-report-message`
  * `1640-1683` `defn- dft-message`
  * `1684-1767` `defn event->messages`
  * `2113-2146` `defn- chatter-message`
  * `2381-2402` `defn- ladder-message`

**`planners`** — 11 forms, 419 lines

  * `1822-1878` `defn plan-latency`
  * `1879-1885` `def restatement-day-seconds`
  * `1962-1966` `defn- chatter-trigger`
  * `1967-1993` `defn- event-driven-chatter`
  * `1994-2055` `defn- periodic-chatter`
  * `2056-2087` `defn- assign-restatement-ordinals`
  * `2088-2112` `defn plan-chatter`
  * `2147-2251` `defn plan-charges`
  * `2282-2288` `defn- ladder-stage`
  * `2289-2296` `defn- rung-instant`
  * `2297-2380` `defn plan-ladders`

**`facade`** — 3 forms, 151 lines

  * `1768-1775` `def default-providers`
  * `1776-1821` `defn emit`
  * `2403-2499` `defn emit-wire`

## 3. Cross-seam call census

Method: every top-level form's body (line comments and string literals
removed) scanned for a whole-symbol occurrence of every other top-level
name in the same file; an occurrence whose definer sits in a different
cluster is one seam edge. Counted per (form, callee) pair, not per
textual occurrence.

### 3a. `engine.clj`

| caller | callee | edges | what crosses |
|---|---|---:|---|
| `decide` | `log-index` | 11 | `last-uncancelled-index`, `last-cited-index`, `reinstated-state`, `subject-superseded?`, `bed-reoccupied-by-someone-else?` |
| `decide` | `streams` | 11 | `rand-int-in` ×4, `uniform-choice` ×3, `minted-encounter-id-field` ×2, `minted-appointment-id-field` |
| `evolve` | `encounters` | 6 | `open-encounter` ×2, `close-encounter` ×2, `cancel-open-encounter`, `reopen-encounter` |
| `run` | `decide` | 6 | `decide` itself, `compile-patient`, `days->seconds`, the three `*-stay-minutes` tables |
| `run` | `streams` | 4 | `stream` ×2, `patient-id-for`, `rand-int-in` |
| `decide` | `encounters` | 3 | `encounter-openable?` ×2, `gate-compiled-encounters` |
| `evolve` | `state` | 3 | `demographics-from-persona` ×2, `placeholder-demographics` |
| `evolve` | `decide` | 2 | `observation-value-fields` ×2 — **the one back-edge; see below** |
| `run` | `assignment` | 2 | `assign-pathway`, `assign-module` |
| `run` | `config` | 2 | `valid-persons?`, `valid-scheduling?` |
| `run` | `log-index` | 2 | `reinstatable-event-types`, `cited-opening-event-types` |
| `encounters` | `streams` | 1 | `open-encounter` → `next-encounter-ordinal` |
| `evolve` | `streams` | 1 | `evolve :appointment` → `next-appointment-ordinal` |
| `fold` | `evolve` | 1 | `replay` → `evolve` |
| `fold` | `state` | 1 | `replay` → `initial-patient` |
| `log-index` | `fold` | 1 | `reinstated-state` → `replay` |
| `run` | `encounters` | 1 | `run` → `stamp-encounter` |
| `run` | `evolve` | 1 | `run` → `evolve` |
| `run` | `fold` | 1 | `run` → `update-beds` |
| `run` | `state` | 1 | `run` → `initial-patient` |

**`streams` has NO outgoing edge.** It is the only leaf cluster that is
also a whole coherent concern, which is what makes author ruling C2(b)'s
choice of it as the first extraction verifiable rather than asserted.

**One cycle, and its single breaker.** `decide` → `log-index` → `fold`
→ `evolve` → `decide` closes only through `observation-value-fields`
(`2360-2376`, private), which `decide :observation` (`:2381`) and
`evolve :observation`/`:diagnostic-report` (`:2971`, `:2983`) all call.
Move that one form to a cluster below both — `state` is the natural home,
since what it computes is the `ObservationRecord` value fields — and the
remaining graph is a DAG in the order

```
streams · state · config · assignment  →  encounters  →  evolve
   →  fold  →  log-index  →  decide  →  run
```

which is the order later sessions should take the clusters in. `state`
must move before `evolve`, and `evolve` before `fold`, or a session
creates a namespace it then has to un-create.

### 3b. `emit_hl7.clj`

| caller | callee | edges | heaviest crossings |
|---|---|---:|---|
| `messages` | `segments` | 62 | every builder assembles MSH/EVN/PID/PV1/… |
| `messages` | `hl7-time` | 21 | `hl7-timestamp`, `transmit-seconds` |
| `segments` | `er7` | 18 | `escape-er7`, `cwe-field`, `xpn-field`, `xad-field`, `tn-field`, `money`, … |
| `messages` | `er7` | 16 | `z-segments-for`, `provider-by-id` |
| `messages` | `registry` | 13 | `message-type-registry`, `siu-renders?`, `siu-event-kinds`, `charge-closing-kinds` |
| `messages` | `timelines` | 10 | `demographics-at` |
| `planners` | `registry` | 6 | the four ladder/kind tables plus `room-and-board-code` |
| `facade` | `messages` | 4 | `event->messages`, `chatter-message`, `ladder-message` |
| `planners` | `timelines` | 4 | `encounter-spans`, `mrn-timeline`, `mrn-at` |
| `facade` | `timelines` | 3 | `demographics-timeline`, `encounter-spans` |
| `planners` | `segments` | 3 | `control-id-for`, `charge-concept` |
| `segments` | `registry` | 2 | `message-type-registry`, `siu-filler-status` |
| `facade` | `hl7-time` | 2 | `default-utc-offset`, `transmit-seconds` |
| `er7` | `timelines` | 1 | `context-for-event` → `demographics-at` |
| `facade` | `segments` | 1 | `control-id-for` |
| `segments` | `hl7-time` | 1 | `hl7-timestamp` |

Already a DAG, no breaker needed, in the order

```
hl7-time · registry  →  timelines  →  er7  →  segments
   →  messages  →  planners  →  facade
```

Two assignments here are judgment calls and are named as such:
`transmit-seconds` (`762-774`) reads as a planner's helper but is pure
`t`-plus-offset arithmetic with ten callers among the message builders,
so it is placed in `hl7-time`; and `context-for-event` (`704-717`) is
placed with the Z-segment renderers in `er7` rather than with
`segments`, which is what keeps `er7`'s only outgoing edge a single one
into `timelines`.

## 4. Apply-path inventory

Every site where an event enters the ground-truth log or a state fold.
This is the P5 unification census and P6's injection-point census, one
list.

### 4a. The single producer

**`engine.clj:4747-4748`** — `(decide (assoc base-streams :patient …) t
world patient-id step)` in `run`'s loop. This is the ONLY expression in
the tree that produces a ground-truth event. The P5 row's phrasing —
"decide-drawn / module-compiled / churn-injected events through one
apply choke point" — needs one correction against the live tree, and it
is the census's first finding: **module-compiled and churn-injected work
enters as STEPS, never as events**, and every step reaches the log by
being decided at this one call. The four step-injection paths are

| path | site | what it injects |
|---|---|---|
| authored + churned pathway | `engine.clj:3816` (`churn/inject` inside `prelude`) | steps, into a patient's own initial entry |
| module-compiled trajectory | `engine.clj:3927` (`compile-patient` inside `prelude`), spliced at `engine.clj:1122` (`decide :registered`'s `:prepend-steps`) and consumed at `engine.clj:4880` | steps, in front of whatever was queued |
| person-fold seeded steps | `engine.clj:4209` / `:4232` (`prelude`'s `seeded-steps`), queued at `engine.clj:4571-4574` | queue entries, at their own `:t` |
| mid-run follow-ups | `engine.clj:4867-4871` (`schedule-followup`, singular or plural) | queue entries, at their own `[t seq-no]` |

So unification's subject is not three event sources; it is the **three
places an event is APPLIED**, below.

### 4b. Apply site 1 — `run`'s in-loop fold (`engine.clj:4775-4843`)

The full-fidelity path. In order:

| step | site | what it does |
|---|---|---|
| decoration | `:4775` | `stamp-encounter` then `mark-warmup` (`:4696`), per event, off `world` as it stands before the batch |
| log index | `:4776` | `base-idx` = `(count (:ground-truth world))` |
| reinstate index | `:4790-4792` | pre-event subject state, for `reinstatable-event-types` only |
| citation index | `:4793-4798` | `[event-type patient-id citation]` → log index |
| registration index | `:4805-4807` | `:registered` subject → log index |
| patient state | `:4813-4815` | `evolve` per participant with a `:patient-id` |
| bed index | `:4816-4819` | `update-beds`, only when `world` carries `:beds` |
| log (persistent mirror) | `:4824-4825` | `:ground-truth (into (:ground-truth world) events)` |
| log (transient accumulator) | `:4829` | `(reduce conj! ground-truth events)` |
| state history | `:4835-4840` | per-participant append of the post-event state |

Note the log is written TWICE, deliberately: the persistent mirror is
what `decide` reads back mid-run (`:4584-4592`'s own comment), the
transient is what `final-result` (`:4716-4721`) persists.

### 4c. Apply site 2 — `replay` (`engine.clj:3098-3139`)

The consumer-facing re-fold, from an empty world: bootstraps missing
patients with `initial-patient` (`:3122-3126`) and folds `evolve`
(`:3127`). Callers, all through `sim-engine/interface`'s `replay`
(`interface.clj:89`): `check.clj` (15 `(engine/replay ground-truth)` call forms at this sha;
`roadmap.md#performance-residual-sites` counts 14 INDEPENDENT ones, a
different accounting — one of the fifteen reuses a `:records` value
when the caller already folded, `check.clj:845`), `emit_fhir.clj:314`,
`identifiers.clj:174`.

**The divergence, stated because unification is where it gets paid.**
`replay` does not do six of the ten things site 1 does: no encounter
stamp, no warm-up mark, no bed index, and none of the three log
indexes. It cannot — those are decorations applied on the way IN, and
`replay` reads a log where they already are. But it means the bed index
`ehrt.sim-check.check` would want is unavailable on the replay path
(`check.clj:527`'s own comment says as much), and it is the concrete
shape of what "one apply path" would have to reconcile.

### 4d. Apply site 3 — `reinstated-state`'s replay fallback (`engine.clj:3229-3271`)

`(:before (nth (replay ground-truth) idx))` at `:3271`, taken only when
`world` carries no `:reinstate-index` — i.e. for a hand-built world in a
test. The `run` path takes the index instead (`:3269-3270`). A third
apply, and the one a unification pass can most cheaply delete.

### 4e. Outside `sim-engine`

No other component folds ground-truth events into patient state.
`ehrt.sim-emit-hl7.v2-replay/evolve-entry` (`v2_replay.clj:291`) and
`ehrt.corpus.board` fold MESSAGES, one layer up the wire, and are named
here only so that a later session does not rediscover them and mistake
them for a fourth apply path.

## 5. Constraints a mover must not break

1. **`engine/stream` must stay the var `run` calls through.**
   `engine_test.clj:2505` (`mutating-one-patients-stream-seed-moves-only-
   that-patient`) perturbs the partition by `with-redefs` on
   `ehrt.sim-engine.engine/stream`. If `run`'s call sites
   (`engine.clj:3670`, `:3675`, `:4059`, `:4543`) are rewritten to a
   moved namespace's var, that test passes vacuously — `moved` becomes
   `#{}` and the assertion `= #{3}` fails, so it goes red rather than
   silent, but the fix is to keep the delegating var and leave the call
   sites unqualified. (`person_simulator/consumption_test.clj:44`,`:142`
   redefine `ehrt.sim-engine.interface/stream`, a different var, and are
   unaffected either way.)
2. **`stream`'s docstring must travel with its delegating def.**
   `docs/consuming-ground-truth.md` §Determinism names
   "`ehrt.sim-engine.engine/stream-scheme`'s own docstring" as the
   authority for the `:stream-scheme` marker. A bare `(def stream-scheme
   streams/stream-scheme)` would make that citation resolve to nothing.
3. **`stream` must stay unhinted.** Its own comment (`:851-856`) records
   why: primitive-long hints compile callers to an `IFn$LOLO` call site
   that a plain `with-redefs` replacement cannot satisfy.
4. **`interface.clj` keeps naming `engine/…`.** It re-exports `mix64`,
   `stream-scheme`, `stream-seed`, `stream`, `newborn-id-tag`,
   `compile-patient`, `person-plan`, `valid-persons?`, `run`,
   `config-keys`, `replay`, `documented-step-rejection-reasons`. Under
   C1(a) every one of those keeps a delegating def in `engine.clj`.
5. **A private var that moves becomes public in its new namespace.**
   `rand-int-in`, `uniform-choice`, `stream-family-tag`,
   `minted-encounter-id-field`, `minted-appointment-id-field` are all
   `defn-`/`^:private` today. They must NOT gain a delegating def in
   `engine.clj` — that would widen the engine's public surface, which
   C1(a) does not ask for and `poly check` would not catch.
6. **A snippet pinned BY PATH from another brick's doc is invisible to
   this census, and cost the streams extraction a red.**
   `components/person-simulator/docs/limitations.md` rows 1 and 10 cite
   `engine.clj` plus a verbatim docstring phrase, and
   `ehrt.docs-tooling.person-simulator-charter-test` resolves both
   against the named file. Row 1's "pinned at 0 for as long as" lives
   in `newborn-id-tag` and row 10's "arc 2's demographic/life-arc
   layer. ZERO draw sites" in `stream-family-tag` -- both inside the
   moved text, both repointed to `streams.clj` by the extraction
   commit. A call-graph census cannot see this class of edge, so the
   recipe for every later cluster is: before moving a form, grep the
   whole repo for a distinctive phrase from its DOCSTRING, not only for
   its name. `components/patient-simulator/docs/limitations.md` is the
   sibling register to check the same way.
7. **`person-simulator`'s limitations row 10 is a bare token scan** over
   that component's own `src` for `sim-engine` names. It sees
   `#{"stream" "newborn-id-tag"}` today
   (`person_simulator/limitations_test.clj:225`). Those names do not
   change; the scan is over person-simulator's tree, not the engine's,
   so an added engine namespace is invisible to it.

# The apply-unification census — matrix, cones, choke point

Serves `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), which is now solely this arc, and — through section 2's matrix —
`roadmap.md#event-stream-mutation` (P6), which names the unified apply
path as its injection point.

**Every line number in this file is at `3e65ff9`**, re-derived in the
session that wrote it against the live tree, never copied from
`.agents/plans/engine-extraction-census.md` section 4 (whose own cites
are at `517a96d`, before the ten extractions moved all three sites) and
never copied from the design channel. Section 5 lists, row by row, where
this census CORRECTS section 4 and the P5 row that quotes it.

**Scope.** This is the stage-1 artefact of the staged plan ruled
2026-08-30: stage 1 unifies the choke point with each site's CURRENT
accumulator stack as an explicit projection, output-identical; stage 2
enables omitted (site × accumulator) pairs ONE COMMIT EACH, where a
delta is a FINDING. Nothing here enables or disables an accumulator.

**Population closure.** Section 1 names every distinct concern the three
apply sites perform between them — thirteen — and section 2's matrix has
a cell for every one of the 39 (site × concern) pairs, so a concern
cannot be omitted from the arc by being left off a list. Seventeen cells
are PRESENT, twenty-two OMITTED; section 3 carries a cone prediction for
each of the twenty-two.

## 0. The three sites, at this sha

| # | site | home | span |
|---|---|---|---|
| 1 | `run`'s in-loop fold | `components/sim-engine/src/ehrt/sim_engine/run.clj` | `1319-1392`, inside `defn run` (`843`) |
| 2 | `replay` | `components/sim-engine/src/ehrt/sim_engine/fold.clj` | `121-155` |
| 3 | `reinstated-state`'s replay fallback | `components/sim-engine/src/ehrt/sim_engine/log_index.clj` | `263-305`, the fallback at `305` |

Site 3 is a THIRD apply because it invokes an apply fold of its own, not
because it folds differently: its call is `(fold/replay ground-truth)`
at `log_index.clj:305`, so its accumulator stack is site 2's, exactly.
What distinguishes it is its READ — `(:before (nth … idx))`, one
element — which is why its cones in section 3 are narrower than site
2's even where the stacks are identical.

Site 2's live consumers, all reached through
`ehrt.sim-engine.interface/replay` (`interface.clj:89`) →
`engine/replay` (`engine.clj:351-357`) → `fold/replay`:
`components/sim-check/src/ehrt/sim_check/check.clj` at `215`, `234`,
`284`, `298`, `399`, `509`, `647`, `845`, `913`, `927`, `1027`, `1102`,
`1552`, `1579` — FOURTEEN code call forms, not the fifteen section 4c
counts (correction C6);
`components/sim-emit-fhir/src/ehrt/sim_emit_fhir/emit_fhir.clj:314`;
`components/sim/src/ehrt/sim/identifiers.clj:174`.

Site 3's live consumers are the two reinstating cancel decides,
`decide.clj` — see `log_index.clj:263-301`'s own docstring for the
ADR-0169 history of why the index exists and the fallback survives.

## 1. The accumulator inventory

Thirteen concerns. Each is stated as the fold it is — the shape a
unified apply path can express uniformly — with the shape's honest
grain named, because four of the thirteen are NOT per-event folds and
pretending otherwise is how an output-identical claim gets lost.

Grain vocabulary, used in the table and nowhere else in this repo yet:

* **decoration** — `(event, world-before-batch) -> event'`, a pre-pass
  over the batch before any state moves;
* **per-event** — `(acc, event, world-before-event, world-after-event)
  -> acc'`, inside the fold;
* **per-batch** — `(acc, events, world-after-batch) -> acc'`, a
  post-pass after the whole batch has folded;
* **parameter** — not an accumulator: a run-scoped value a concern
  needs.

| # | concern | grain | the fold |
|---|---|---|---|
| A1 | `:encounter-stamp` | decoration | `(encounters/stamp-encounter world ev)` — writes `:encounter-id` onto an event that has none, read off the PRE-BATCH world (`encounters.clj:138-153`) |
| A2 | `:warm-up-mark` | decoration | `(assoc ev :warm-up (< (:t ev) warm-up-seconds))` |
| A3 | `:log-ordinal` | per-event | `idx = base-idx + offset`, where `base-idx = (count (:ground-truth world))`; the shared input A4/A5/A6 key on, and an accumulator in its own right (a counter over the log) |
| A4 | `:reinstate-index` | per-event | `ridx' = (assoc ridx idx (get-in w [:patients subject]))` when `(reinstatable-event-types (:event ev))` — the PRE-event subject state, at the log index |
| A5 | `:citation-index` | per-event | `cidx' = (assoc cidx [(:event ev) patient-id (:citation ev)] idx)` per participant, when `(cited-opening-event-types (:event ev))` and `(:citation ev)` is non-nil |
| A6 | `:registration-index` | per-event | `gidx' = (assoc gidx subject idx)` when `(= :registered (:event ev))` |
| A7 | `:patient-bootstrap` | per-event | `(assoc ps pid (state/initial-patient pid (:active-mrn event)))` for every participant id not already in the patients map |
| A8 | `:patient-state` | per-event | `(update-in w [:patients patient-id] evolve/evolve ev)` per participant carrying `:patient-id` |
| A9 | `:bed-index` | per-event | `(fold/update-beds (:beds w-next) ev (:patients w) (:patients w-next))`, gated on `(:beds w-next)` |
| A10 | `:log-mirror` | per-batch | `(assoc world' :ground-truth (into (:ground-truth world) events))` — the PERSISTENT log a mid-run `decide` reads back |
| A11 | `:log-accumulator` | per-batch | `(reduce conj! ground-truth events)` — the TRANSIENT log `final-result` persists |
| A12 | `:state-history` | per-batch | per participant of every event, `(update sh2 patient-id (fnil conj []) (get-in world' [:patients patient-id]))` — appended off the POST-BATCH world |
| A13 | `:replay-entries` | per-event | `(conj! acc {:event … :patient-id … :before … :after … :world-before … :world-after …})` |
| — | `:warm-up-seconds` | parameter | A2's threshold; run-scoped, not accumulated |

Two subject notions coexist and are NOT interchangeable; the arc must
carry both:

* **`subject`** (A4, A6) — `(:patient-id (first (:participants ev)))`,
  the first participant's patient-id, which is `nil` for a
  `:bed-status-change` whose first participant names a BED. Neither A4
  nor A6 can see such an event (`reinstatable-event-types` is
  `#{:transfer :discharge}`, `log_index.clj:81-95`; A6's guard is
  `:registered`), so the `nil` is unreachable rather than handled.
* **`subject-id`** (A13) — `(first participant-ids)` where
  `participant-ids` is `(mapv :patient-id (filter :patient-id
  (:participants event)))`, i.e. the first participant that HAS a
  patient-id. `fold.clj:139-144`'s own comment records why the filter
  exists: a nil-keyed phantom patient must not reach
  `ehrt.sim-check.check`.

They coincide on every event whose first participant is a patient and
diverge on every event whose first participant is not. A unified apply
path computes both, independently, or it changes output.

## 2. The site × accumulator matrix

`P` = present, `—` = omitted. Every cell cites `file:line` at
`3e65ff9`; an omitted cell cites the site's own span, which is the
evidence of absence.

| # | concern | site 1 — `run` fold | site 2 — `replay` | site 3 — `reinstated-state` |
|---|---|---|---|---|
| A1 | `:encounter-stamp` | **P** `run.clj:1327` | — `fold.clj:121-155` | — `log_index.clj:305` |
| A2 | `:warm-up-mark` | **P** `run.clj:1327` (fn at `:1248`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A3 | `:log-ordinal` | **P** `run.clj:1328`, `:1340` | — `fold.clj:121-155` | — `log_index.clj:305` |
| A4 | `:reinstate-index` | **P** `run.clj:1342-1344` (init `:1157`, published `:1378`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A5 | `:citation-index` | **P** `run.clj:1345-1350` (init `:1164`, published `:1379`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A6 | `:registration-index` | **P** `run.clj:1356-1358` (init `:1203`, published `:1380`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A7 | `:patient-bootstrap` | — `run.clj:1319-1392` | **P** `fold.clj:145-149` | **P** `fold.clj:145-149` via `log_index.clj:305` |
| A8 | `:patient-state` | **P** `run.clj:1365-1367` | **P** `fold.clj:150` | **P** `fold.clj:150` via `log_index.clj:305` |
| A9 | `:bed-index` | **P** `run.clj:1368-1371` | — `fold.clj:121-155` | — `log_index.clj:305` |
| A10 | `:log-mirror` | **P** `run.clj:1376-1377` (init `:1144`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A11 | `:log-accumulator` | **P** `run.clj:1381` (init `:1281`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A12 | `:state-history` | **P** `run.clj:1387-1392` (init `:1282`) | — `fold.clj:121-155` | — `log_index.clj:305` |
| A13 | `:replay-entries` | — `run.clj:1319-1392` | **P** `fold.clj:152-155` (acc at `:135`) | **P** `fold.clj:152-155` via `log_index.clj:305`, read at `log_index.clj:305` as `(:before (nth … idx))` |

Totals: site 1 present 11 of 13, site 2 present 3 of 13, site 3 present
3 of 13. Seventeen present, twenty-two omitted.

Site 3's three PRESENT cells are inherited, not chosen: it calls
`fold/replay`, so its stack is whatever site 2's is, and its own
projection at stage 1 is a restatement of site 2's rather than an
independent selection. That is a fact about the tree, not a modelling
convenience — and it is exactly what makes site 3 "the one a
unification pass can most cheaply delete" (section 4d of the extraction
census), because at the end state A4 supplies its answer directly.

## 3. Cone predictions for the twenty-two omitted pairs

For each omitted pair: what, downstream of that site, could observe the
accumulator if stage 2 enabled it. **OUTPUT-MOVING** predicts a delta at
a named consuming path; **INERT** predicts none. A stage-2 commit that
contradicts its own row here is a FINDING under the ruling, and arrives
explained either way.

### 3a. Site 1 — 2 omitted pairs

| pair | prediction | cone |
|---|---|---|
| 1 × A7 `:patient-bootstrap` | **INERT** | Reachable only if an event names a participant absent from `world`'s `:patients`. `run` registers every patient before any event of theirs is decided (`decide :registered` is every patient's first event — `decide.clj:264-270`, and `person_fold.clj:33` calls the property structural), and today an unknown participant would evolve `nil` rather than throw. Prediction: no unknown participant arises, so bootstrap never fires and no output moves. If it DOES fire, that is the finding — an unregistered participant reaching the log — and it is worth more than the pair. |
| 1 × A13 `:replay-entries` | **INERT** | Nothing in `run`'s result reads an entries vector: `final-result` (`run.clj:1268-1273`) merges `:ground-truth`, `:state-history`, `:facility`, `:providers` and nothing else, and no caller of `ehrt.sim-engine.interface/run` asks for more. Enabling costs allocation (one map per event carrying two whole-world snapshots) and moves no byte. |

### 3b. Site 2 — 10 omitted pairs

| pair | prediction | cone |
|---|---|---|
| 2 × A1 `:encounter-stamp` | **OUTPUT-MOVING** | `replay` reads a log whose events already carry `:encounter-id` (stamped inbound by site 1); re-stamping recomputes it off `replay`'s own world. `evolve` READS `:encounter-id` off the event and writes it into conditions, observations, medication orders and care plans (`evolve.clj:210`, `:220`, `:338`, `:343`, `:412-415`, `:423-427`, `:431-434`, `:459-463`), so a changed stamp changes `:before`/`:after` too, not only `:event`. Consuming paths: `check.clj`'s encounter invariants, `emit_fhir.clj`, and the four emit-hl7 namespaces that read `:encounter-id` (`messages.clj`, `planners.clj`, `timelines.clj`, `registry.clj`). Note `stamp-encounter` is the identity on a legacy run with no `:encounters` opt-in (`encounters.clj:151-152`), so the move is expected on encounter-carrying corpora ONLY. |
| 2 × A2 `:warm-up-mark` | **OUTPUT-MOVING** | `:warm-up-seconds` is not in the log; replay has no source for it, so enabling this pair either takes a new parameter or defaults to `0` and overwrites every event's existing `:warm-up true` with `false`. Consuming path: `check.clj:184-185`'s `:warm-up-mark-matches-window` invariant, which compares the mark against the window and would then be comparing replay's recomputation with itself — the vacuous-gate shape. Also `er7.clj:245`. This pair is the arc's clearest candidate for "enabled, and the answer is no". |
| 2 × A3 `:log-ordinal` | **INERT** | A counter with no reader: replay's entry map (`fold.clj:153-155`) carries no index, and no consumer asks for one. Inert until A4/A5/A6 are enabled at this site, which is the only reason to enable it. |
| 2 × A4 `:reinstate-index` | **INERT** | The index would accumulate correctly (its input, the pre-event subject state, is exactly `:before`, which replay already computes) and nothing would read it: replay returns the entries vector, not a world. Its VALUE is that site 3's read then has a first-class source at site 2 — which is the deletion section 4d predicts, and a stage-2 pair of its own. |
| 2 × A5 `:citation-index` | **INERT** | Same shape: accumulates, nothing reads it. Note `last-cited-index` (`log_index.clj:157-166`) already falls back to a whole-log scan when `world` has no `:citation-index`, so a replay-built index is not consulted by that path either. |
| 2 × A6 `:registration-index` | **INERT** | Same shape. No consumer of `replay` reads a registration index; `check.clj`'s own registration invariants walk the entries. |
| 2 × A9 `:bed-index` | **INERT** | The extraction census (4c) and the P5 row both say this pair is what the arc costs — that `ehrt.sim-check.check` has no bed index on the replay path. **That reading is wrong at this sha and was wrong at `517a96d` too** (section 5, correction C3): `check.clj:522-528` says the bed fold there is DELIBERATELY not `update-beds`, on independent-judge grounds, and the same sentence stood at the census's own sha. So enabling this pair moves no output and unlocks no invariant: `check.clj` would still not call it, and should not. Its real value is to a FUTURE consumer that wants the engine's own index off a replayed log, which is P6's territory, not a debt this arc owes `check`. |
| 2 × A10 `:log-mirror` | **INERT** | Replay is HANDED the log; accumulating a second copy of it into a world it does not return is a pure duplicate. |
| 2 × A11 `:log-accumulator` | **INERT** | Same, in transient form. |
| 2 × A12 `:state-history` | **INERT** | Replay's entries vector already generalizes state history across patients — `fold.clj:122-133`'s own docstring says so, citing sim/ADR-0008 — and no consumer of `replay` asks for a separate `{patient-id -> [state …]}` map. Enabling it duplicates, in a narrower shape, what A13 already returns. |

### 3c. Site 3 — 10 omitted pairs

Site 3's read is one element of one key: `(:before (nth entries idx))`
— a patient state, not an event. That narrows every cone below relative
to its site-2 twin.

| pair | prediction | cone |
|---|---|---|
| 3 × A1 `:encounter-stamp` | **OUTPUT-MOVING** | Narrower than 2 × A1 but real, and by the same mechanism: `evolve` folds `:encounter-id` into the patient's conditions/observations/medications/care-plans, so a re-stamped log gives a different `:before` at `idx`. That value is what the two reinstating cancel decides restore, so the delta reaches emitted events and the byte-identity gate. Consuming path: `decide.clj`'s `:cancel-transfer`/`:cancel-discharge` → the log → every emitter. |
| 3 × A2 `:warm-up-mark` | **INERT** | `:warm-up` is a key on the EVENT and `evolve` never reads it (no `warm-up` occurrence in `evolve.clj`), so it cannot reach a `:before`. The only site-3 read is a patient state. |
| 3 × A3 `:log-ordinal` | **INERT** | Not read; see 2 × A3. |
| 3 × A4 `:reinstate-index` | **INERT, and it is the pair the arc exists for** | Enabling A4 on site 3's own fold makes the fallback's answer available as a lookup rather than an `nth` over a materialized replay — which is precisely what `run` already does at site 1 (`log_index.clj:284-292`), and what `ehrt.sim.run-test/cancel-decides-reinstate-exactly-what-replay-would-hand-back` proves equal on every gated corpus. Predicted INERT because the two are proven to agree; the win is the O(N)-per-cancel cost `log_index.clj:274-282` measures at 35.3% of the generate phase, not an output move. |
| 3 × A5 `:citation-index` | **INERT** | Not read at this site. |
| 3 × A6 `:registration-index` | **INERT** | Not read at this site. |
| 3 × A9 `:bed-index` | **INERT** | `reinstated-state` returns a patient state; a bed index is not in it. The reinstatement's own bed question is asked separately and against the LIVE board, not a replayed one — `bed-reoccupied-by-someone-else?` (`log_index.clj:174-187`), whose docstring says so explicitly. |
| 3 × A10 `:log-mirror` | **INERT** | See 2 × A10. |
| 3 × A11 `:log-accumulator` | **INERT** | See 2 × A11. |
| 3 × A12 `:state-history` | **INERT** | See 2 × A12. |

Summary of predictions: **3 OUTPUT-MOVING** (2 × A1, 2 × A2, 3 × A1),
**19 INERT**. All three OUTPUT-MOVING pairs are decorations — A1 and A2,
the two concerns that are applied on the way IN and that a re-fold of an
existing log therefore RECOMPUTES rather than accumulates. That is the
divergence's real shape, and it is the same fact section 4c states
(`replay` "cannot" do them) rendered as a prediction stage 2 can check.

### 3d. Stage-2 status, pair by pair — the checklist, marked

Stage 2 ran 2026-09-01 (`.agents/session-records/2026-09-01-apply-
unification-stage-2.md`). **Every one of the nineteen INERT predictions
is CONFIRMED; none was refuted, so the cone method itself was never put
in question.** The proof is three pushed spans, each with
`bin/regression-oracle` IDENTICAL over 41 roots and
`bin/ground-truth-bracket` IDENTICAL over 38, no declaration owed at
any of the six runs. **Span-identity is not per-commit identity** — a
delta anywhere in a span would have forced a bisect to the enabling
commit — but no span moved, so no bisect was owed.

**One commit in the table is not a pair.** The de-alias, `c4f6ddd`, is
the precondition correction C5 anticipated: `reinstated-projection` was
`replay-projection` BY IDENTITY, so sites 2 and 3 could not be enabled
independently. It is forced rather than chosen — 3 × A2 is INERT while
its twin 2 × A2 is OUTPUT-MOVING, so the two columns must diverge for
the inert one to land at all. It enabled nothing: the set's value is
unchanged.

| pair | prediction | status | commit |
|---|---|---|---|
| — (de-alias, correction C5) | n/a | **LANDED**, output-identical | `c4f6ddd` |
| 1 × A7 `:patient-bootstrap` | INERT | **CONFIRMED-INERT** | `649403e` |
| 1 × A13 `:replay-entries` | INERT | **CONFIRMED-INERT** | `e05da9c` |
| 2 × A1 `:encounter-stamp` | OUTPUT-MOVING | **PREPARED, NOT LANDED** | — |
| 2 × A2 `:warm-up-mark` | OUTPUT-MOVING | **PREPARED, NOT LANDED** | — |
| 2 × A3 `:log-ordinal` | INERT | **CONFIRMED-INERT** | `0d0db6e` |
| 2 × A4 `:reinstate-index` | INERT | **CONFIRMED-INERT** | `2505a68` |
| 2 × A5 `:citation-index` | INERT | **CONFIRMED-INERT** | `396e047` |
| 2 × A6 `:registration-index` | INERT | **CONFIRMED-INERT** | `59605ed` |
| 2 × A9 `:bed-index` | INERT | **CONFIRMED-INERT** | `7658e82` |
| 2 × A10 `:log-mirror` | INERT | **CONFIRMED-INERT**, with an observation below | `d771829` |
| 2 × A11 `:log-accumulator` | INERT | **CONFIRMED-INERT** | `15ea306` |
| 2 × A12 `:state-history` | INERT | **CONFIRMED-INERT** | `b547f8f` |
| 3 × A1 `:encounter-stamp` | OUTPUT-MOVING | **PREPARED, NOT LANDED** | — |
| 3 × A2 `:warm-up-mark` | INERT | **CONFIRMED-INERT**, at one declared value | `00373db` |
| 3 × A3 `:log-ordinal` | INERT | **CONFIRMED-INERT** | `f34c423` |
| 3 × A4 `:reinstate-index` | INERT | **CONFIRMED-INERT** | `1aa5796` |
| 3 × A5 `:citation-index` | INERT | **CONFIRMED-INERT** | `ee2b01e` |
| 3 × A6 `:registration-index` | INERT | **CONFIRMED-INERT** | `162080a` |
| 3 × A9 `:bed-index` | INERT | **CONFIRMED-INERT** | `5f11ee7` |
| 3 × A10 `:log-mirror` | INERT | **CONFIRMED-INERT** | `bc34aba` |
| 3 × A11 `:log-accumulator` | INERT | **CONFIRMED-INERT** | `ae93afe` |
| 3 × A12 `:state-history` | INERT | **CONFIRMED-INERT** | `3abfa44` |

End state after stage 2: site 1 at **13 of 13** (full product, the
ruled end state), site 2 at **11 of 13**, site 3 at **12 of 13**. The
three cells still omitted are exactly the three this section predicted
OUTPUT-MOVING, all of them DECORATIONS.

**Two findings the cones did not carry, recorded here because they are
properties of the concerns rather than of one commit.**

* **`:log-mirror` REVERSES when the site's world carries no
  `:ground-truth`.** The concern is `(assoc world' :ground-truth (into
  (:ground-truth world) events))`. Site 1's world always holds a
  `:ground-truth` VECTOR, so `into` appends in log order; sites 2 and 3
  start from `{:patients {}}`, so it is `(into nil events)` — a LIST,
  in REVERSE order. Inert at both sites because both discard the world,
  and NOT fixed here (editing the accumulator while enabling is outside
  the session's fence). A consumer must seed `:ground-truth []` first.
* **Only the two TRANSIENT accumulators need a slot.** `:log` and
  `:entries` are `conj!`-ed and throw on nil; every persistent concern
  — the three indexes, `:log-mirror`, `:state-history` — builds from
  nil cleanly. That is what decided which of the nineteen enabling
  diffs touched a call site at all: three did (1 × A13, 2 × A11,
  3 × A11), plus 3 × A2 for its parameter.

**3 × A2 carries one DECLARED value, disclosed rather than absorbed.**
`:warm-up-mark` takes a `:warm-up-seconds` parameter and a log has no
source for one — which is precisely why its site-2 twin is
OUTPUT-MOVING. Site 3's call site declares `0`, the option this section
names in the 2 × A2 row, and the effect is that every event in that
fallback's local copy of the batch is marked `:warm-up false`. It is
inert only because `evolve` reads `:warm-up` nowhere (checked: zero
occurrences in `evolve.clj`) and site 3 reads a PATIENT STATE.

## 4. The choke point

### 4a. Home — `fold.clj`, confirmed, at one cost

The design channel's expectation was `components/sim-engine/src/ehrt/
sim_engine/fold.clj`. **Confirmed**, and it is the only home among the
three sites' own namespaces that does not require moving a fold. The
argument is the require graph at this sha, derived from the `ns` forms:

    encounters -> streams
    evolve     -> encounters, state, streams
    fold       -> evolve, state
    log-index  -> fold, sim-model
    decide     -> encounters, log-index, order-profiles, state, streams, …
    run        -> assignment, churn, config, decide, encounters, evolve,
                  fold, log-index, order-profiles, person-fold, state,
                  streams, …

Site 2 lives in `fold`, so `fold` must own the choke point or require
it. Site 3 lives in `log-index`, which already requires `fold`. Site 1
lives in `run`, which requires everything. So `fold` as home adds NO
edge for any of the three calls.

**The one cost.** The choke point's A4 and A5 guards name
`reinstatable-event-types` and `cited-opening-event-types`, which live
in `log_index.clj` (`:81-95`, `:117-123`) — and `log-index` requires
`fold`, so `fold` naming them would close a cycle. The two sets are
apply-site policy, not log-query policy: each names which events an
in-fold index RECORDS, `log_index.clj:82-83`'s own docstring says as
much ("the only ones `run`'s `:reinstate-index` records"), and each has
exactly ONE live code consumer in the repository — site 1, at
`run.clj:1342` and `run.clj:1345`. **They move down into `fold.clj`,
with delegating defs left in `log_index.clj` under ruling C1(a).**
That is a relocation of two pure-data `def`s, output-identical by
inspection, and it costs no test change (there are zero test references
to either name) and no live prose repoint (`decide.clj:902` names
`reinstatable-event-types` bare, with no path; `notes/adr/0174…:577`
cites it at `engine.clj`, where it has not lived since the sixth
extraction — a STALE-BEFORE-THE-MOVE citation, disclosed and backlogged
under `rulings.md#R-move-not-improve`, not fixed here).

Two alternatives were derived and rejected, and are recorded so a later
session does not re-derive them:

* **`log_index.clj` as home** — acyclic for sites 1 and 3, but site 2
  would need `fold` to require `log-index` while `log-index` requires
  `fold` for `update-beds`. Paying it means moving `replay`,
  `update-beds` AND `bed-correction-event-types` out of `fold`, i.e.
  gutting the namespace the fifth extraction created. Strictly more
  movement than moving two sets.
* **A new namespace above both** — `apply-path` requiring `fold` and
  `log-index` works for site 1 and for nothing else: sites 2 and 3 both
  sit BELOW it and cannot call up. Every repair converges on the same
  two sets.

### 4b. Signature

```clojure
(defn apply-events
  "acc x events x projection -> acc'."
  [acc events projection]
  …)
```

`projection` is a SET of concern keys from section 1 — an explicit
declared subset of `full-algebra`, never a per-site rewrite of the
body. `acc` is a map whose keys are the accumulator slots the
projection names, plus the parameter slots those concerns need:

| slot | held by | shape |
|---|---|---|
| `:world` | always | the world map; `(:patients …)` is what A7/A8/A9/A12/A13 read and write |
| `:ground-truth` | A11 | the transient log accumulator, in and out as a TRANSIENT (site 1 persists it at `final-result`, not here) |
| `:state-history` | A12 | `{patient-id [state …]}` |
| `:entries` | A13 | the transient entries accumulator |
| `:warm-up-seconds` | A2 | parameter, threaded unchanged |

A10 needs no slot of its own: it publishes into `(:world acc')` under
`:ground-truth`, as site 1 does today at `run.clj:1376-1377`. A3 needs
none either: `base-idx` is derived from `(:world acc)` at entry.

The body's ORDER is site 1's, unchanged, because that is what makes
stage 1 output-identical by construction: decorate the batch off the
pre-batch world (A1, A2); take `base-idx` off the pre-batch world (A3);
one per-event reduce (A4, A5, A6, A7, A8, A9, A13, in that order);
then the per-batch post-pass (A10, A11, A12) off the post-reduce world.
Each concern is guarded by its own `(projection :key)` and by nothing
else.

### 4c. The three projections, as declared subsets

```clojure
(def full-algebra
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(def run-loop-projection   (disj full-algebra :patient-bootstrap :replay-entries))
(def replay-projection     #{:patient-bootstrap :patient-state :replay-entries})
(def reinstated-projection replay-projection)
```

`reinstated-projection` is `replay-projection` by VALUE and says so:
section 2's site-3 column is inherited, and writing it as an alias
rather than a second literal is what keeps the two from drifting apart
without a stage-2 commit saying so.

The co-landed invariant `ehrt.sim-engine.apply-projection-test` asserts
these three sets against section 2's matrix, column by column, so the
matrix and the code cannot diverge silently — the gate stage 2 checks
each pair off against.

### 4d. What stage 1 does NOT do

No accumulator is enabled or disabled at any site; no draw order
changes; `interface.clj` is not edited; site 3 is not deleted, though
section 4d of the extraction census predicts it can be. The three
OUTPUT-MOVING pairs of section 3 stay omitted. `bin/regression-oracle`
and `bin/ground-truth-bracket` must both report IDENTICAL with no
declaration; anything else is a stop.

## 5. Corrections to the extraction census's section 4

The stage-1 ruling asks that census corrections go back to the design
channel's expectations. Six, in descending order of consequence. **None
of them disagrees with section 4c's divergence in KIND** — `replay`
still does strictly fewer things than site 1, and the six it does not do
are the six named — so the fence that would have stopped this session
before implementing did not fire.

* **C1 — the divergence is six wide at site 1's grain and ten wide at
  the matrix's.** Section 4c counts six concerns `replay` omits
  (encounter stamp, warm-up mark, bed index, three log indexes). All six
  confirmed. The matrix adds four more (A3 `:log-ordinal`, A10
  `:log-mirror`, A11 `:log-accumulator`, A12 `:state-history`) because
  section 4c was comparing against a ten-row list that folded A1+A2 into
  one "decoration" row and did not name the two concerns site 2 has and
  site 1 does not (A7 `:patient-bootstrap`, A13 `:replay-entries`). Same
  divergence, counted from both ends instead of one.

* **C2 — `:state-history` appends the POST-BATCH state, not the
  post-event state.** Section 4b's row reads "per-participant append of
  the post-event state". The live fold (`run.clj:1387-1392`) reads
  `(get-in world' [:patients patient-id])`, where `world'` is the
  accumulator AFTER the whole batch has folded — so for a batch of two
  events touching one patient, that patient gets TWO history entries
  holding the SAME final state, not one per event. This is the single
  correction that would most easily have broken an output-identical
  refactor, since the natural unification is to fold history per event.
  Consequence for the arc: A12's grain is per-batch, and section 4b's
  ten-row list is the wrong shape to generalize from without it.

* **C3 — `check.clj:527`'s comment does not say what section 4c says it
  says, and did not at `517a96d` either.** Section 4c: "the bed index
  `ehrt.sim-check.check` would want is unavailable on the replay path
  (`check.clj:527`'s own comment says as much)". The comment
  (`check.clj:522-528` at this sha; identical text at `517a96d`, checked
  by `git show`) says the opposite — that the fold there is DELIBERATELY
  not `update-beds`, because "calling the engine's own index-builder
  here would prove only that the engine agrees with itself, which is the
  vacuous-gate shape this repository has already been bitten by twice".
  The P5 roadmap row repeats the mis-reading in its own closing sentence
  ("which is why `ehrt.sim-check.check` has no bed index on the replay
  path"). Consequence: the 2 × A9 pair has no consumer waiting for it,
  and the arc should not be sold on delivering `check` a bed index.

* **C4 — the two subject notions are distinct and section 4 names
  neither.** `subject` (A4, A6) and `subject-id` (A13) differ on any
  event whose first participant carries no `:patient-id`; see section 1.
  A unification that collapsed them would be output-identical on today's
  corpora only by accident.

* **C5 — site 3 has no projection of its own to state.** Section 4d
  presents site 3 as a third apply site alongside the other two; it is,
  but its stack is site 2's by construction (it calls `fold/replay`),
  so stage 1 declares it as an alias rather than a third literal. The
  arc's ten site-3 pairs are still real, because stage 2 could give it
  one — but at stage 1 there is nothing independent to project.

* **C6 — `replay`'s call-form count in `check.clj` is fourteen, not
  fifteen.** Section 4c: "`check.clj` (15 `(engine/replay
  ground-truth)` call forms at this sha …)". The literal string occurs
  fifteen times, but one of the fifteen — `check.clj:395` — is inside
  `fold-records`' own DOCSTRING, not a call. Fourteen code call forms,
  listed in section 0, which is exactly
  `roadmap.md#performance-residual-sites`' own "14 independent
  `engine/replay` calls" (`roadmap.md:20`) — so there was never a gap
  between the two accountings, and section 4c's sentence explaining one
  away by naming `check.clj:845`'s `(or (:records folded) …)` reuse is
  reconciling a difference that the docstring occurrence created.
  Consequence for this arc: none — no cell of the matrix depends on it —
  which is why it is listed last.

Line numbers in section 4 are stale by design and are not counted as
corrections: all three sites moved during the extraction phase, and
section 0 above re-homes them.

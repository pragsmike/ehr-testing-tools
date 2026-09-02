# 2026-09-02 — downstream `:self-check-failed`: repro and diagnosis

**Ceremony:** R30 standing default (commit and push at each checkpoint).
**Fence:** ruling `R-shape` — diagnosis only. No change under any
`components/*/src` or `bases/*/src`; no roadmap edit; STOP at step 6.
Held: the only tracked files this session touched are `README.md`, two
new files under `test-fixtures/`, and this record with its prompt
archive.
**Driving prompt:** [`.agents/prompts/2026-09-02-downstream-self-check-failed.md`](../prompts/2026-09-02-downstream-self-check-failed.md).

## Headline

**Reproduced at tip, unchanged, byte-identical to the downstream run.**
One invariant convicts — `outpatient-patients-occupy-no-bed` — on one
patient, 30,507 times. The mechanism is traced end to end below, and it
is **not new**: it is the exact case
[`2026-08-29-ts-5-superseded-cancel.md`](2026-08-29-ts-5-superseded-cancel.md)
named as "an adjacent case this change deliberately does not reach",
reached here for the first time by a configuration that turns
`:encounters` and `:scheduling` on together.

**And the same defect state sits, unconvicted, in a run this repository
certifies as clean.** At `--patients 1984` the identical patient
receives the identical reinstatement and holds the identical stale bed
for the rest of a twenty-year log, and `sim run` exits 0. That fact is
what makes the fork below a real fork rather than a bug report.

Nothing was fixed. See [The fork](#the-fork).

## What landed

| commit | contents |
|---|---|
| `d00b5a5` | step 1 — README names the top-level-vector counting rule |
| `7463e7f` | step 2 — the downstream config as a fixture, with provenance |
| *(this)* | step 6 — this record and the prompt archive |

Riders `R-A` (the consumer-docs step-2 fence breach, ratified as landed)
and `R-B` (the documented `:capacity-exhausted` consequence: halts, no
corpus, no self-check, payload patient/ward/census) are hereby
**enacted**. Both were already in the tree exactly as the rulings
describe them, so enactment is this note and nothing else — neither
ruling had an outstanding edit against it, and none was invented to give
it one.

## Step 3 — repro at tip

```bash
bin/ehrt sim run --seed 424242 --patients 2000 --reference-date 2026-08-31 \
  --churn --config test-fixtures/downstream-calibration/config.edn \
  --format ground-truth
```

**Exit 2**, `{:status :error :category :self-check-failed}`, stdout
**2,985,469 bytes** — the same byte count the downstream report gives for
its discarded partial stdout.

The complete payload. `:payload` carries `:violations` and nothing else:

| | |
|---|---|
| distinct invariants | **1** — `:outpatient-patients-occupy-no-bed` |
| violation rows | **30,507** |
| distinct patients named | **1** — `PID-001086-869d73e0` |
| row shape | `{:at :invariant :patient-id}`, uniform across all 30,507 |
| first row, in full | `{:invariant :outpatient-patients-occupy-no-bed, :patient-id "PID-001086-869d73e0", :at 4572240}` |
| `:at` range | 4,572,240 … 631,353,025 |

The log behind that error is 49,016 events over 22 kinds, and it is not
in the failing run's stdout — the error envelope replaces it. It was
captured separately through `run-command`'s own documented injectable
seam (`:engine-run-fn`, its second arity) from a script outside the brick
tree. No `src` file was edited to get it.

## Step 4 — confirm at their revision `386e738d`

A second clone (not a worktree — `version` reads `git describe`, which a
worktree breaks), checked out at
`386e738d95e49b0a2aefccfedbf20d172a1fcfa9`, tree
`c8ab539224df649585610d969be509234184fa79`, `ehrt version` reporting
`stable-20260821-patient-simulator-charter-230-g386e738` — their reported
identity exactly.

| N | revision | exit | bytes | sha256 |
|---|---|---|---|---|
| 2,000 | `386e738d` | **2** | 2,985,469 | `7ee31e42…` |
| 2,000 | tip | **2** | 2,985,469 | `7ee31e42…` |
| 500 | `386e738d` | 0 | 9,751,861 | `434232a9…` |
| 1,000 | tip | 0 | 11,966,511 | `ddcfc319…` |

**No mismatch to disclose — the opposite.** The 500-arrival digest equals
the downstream `434232a913c3389fdc3856f9a6eb14854ff6174499e8a5caa0643085824a03d5`
and the 1,000-arrival digest equals their
`ddcfc319ffed230a1ce2edd13f62f2fbfd4fd4264eface5bf6a37967ba2deb11`,
**verbatim**, across the JDK difference the prompt flagged as a live risk
(theirs 17.0.18, this machine's 21.0.7). And the two failing 2,000-arrival
payloads are byte-identical to each other across 88 commits. Within-version
determinism holds, and the 88 commits between their revision and tip move
nothing this configuration reaches.

## Step 5 — the shrink

Bisection over `(1000, 2000]` at tip, everything else held. Ten bisection probes, plus an endpoint re-run and one sample:

| N | exit |
|---|---|
| 1,000 (endpoint, re-run here) | 0 |
| 1,200 · 1,500 · 1,750 · 1,875 · 1,937 · 1,968 | 0 |
| 1,984 · 1,992 · 1,996 · 1,998 · **1,999** | 0 |
| **2,000** | **2** |

**N_min = 2,000 — the interval's own upper endpoint.** Every value the
bisection tried below it passes, 1,999 included. The failure is a single
point in `(1000, 2000]`, not a threshold with a run of failures above it.

**Therefore the "is the invariant set stable across N_min..2000"
question is degenerate**: that range is the single point 2,000, whose
invariant set is the one row already reported. It is recorded as
answered-by-collapse rather than answered.

Two cautions the numbers themselves raise:

- **The bisection assumes monotonicity in `--patients`, and nothing in
  the tree guarantees it.** `--patients` is an engine key: turning it up
  draws, and a draw reshuffles the population, so "passes at N" does not
  formally imply "passes at N−1". With N_min landing on the endpoint,
  the bisection has in effect only *sampled* ten points; it has not
  proved 2,000 is the least failing count.
- **Output size is already non-monotone**, which is the same fact
  showing: 1,998 arrivals produce 16,559,710 bytes and 1,999 produce
  16,401,176.

**The offending ordinal is below every passing N, and that is the
finding.** `PID-001086-869d73e0` is arrival ordinal 1086. It is present
in every passing run checked, and in none of them does it offend.

## Step 6 — the STOP report

### The convicting invariant

`outpatient-patients-occupy-no-bed`, defined at
[`components/sim-check/src/ehrt/sim_check/check.clj:476`](../../components/sim-check/src/ehrt/sim_check/check.clj),
over the predicate `outpatient-with-bed?` at `:474`, catalog member at
`:1836`. It is one of the 44 invariants
[`docs/consuming-ground-truth.md`](../../docs/consuming-ground-truth.md)
lists under "What `ehrt sim check` certifies", and it is **not** one of
the four needing config the log does not carry — it reads only folded
patient state:

```clojure
(defn- outpatient-with-bed? [{:keys [class location]}]
  (and (= class :outpatient) (some? location)))
```

Its docstring calls it "the structural half of item 6's conditional
validity row: `:class :outpatient => :location nil`, for the visit's
entire duration".

### What the checker saw

At the fold record for the `:outpatient-visit` event at `:t` 4,572,240,
`world-after` held, for `PID-001086-869d73e0`:

```clojure
{:class    :outpatient
 :location {:ward "Emergency" :bed "ED-H372" :placement :surge}}
```

`:class` `:outpatient` and a `:location` that is not nil. The fold
reported that pair at that event and at every subsequent event for which
the flag was still set — which is every event to the end of the log:
30,507 rows, the last at `:t` 631,353,025.

### The log slice the first conviction judged

That patient's complete history is 21 events. The location-bearing ones,
in order:

| `:t` | event | `:location` after |
|---|---|---|
| 1,926,480 | `:registered` | — |
| 1,926,480 | `:appointment` (`:appointment-class :inpatient`) | — |
| 2,225,880 | `:admission` ENC-**00** | `Emergency / ED-H219 / surge` |
| 2,232,540 | `:transfer` | `Renal / RENAL-47 / licensed` |
| 2,273,760 | `:discharge` ENC-00 | nil |
| 2,963,280 | `:admission` ENC-**01** | `Emergency / ED-H372 / surge` |
| 2,972,040 | `:transfer` | `Renal / RENAL-06 / licensed` |
| 3,017,040 | `:discharge` ENC-01 | nil |

Then **five events all at `:t` 3,017,040, in this order**:

| # | event | what it left on the patient |
|---|---|---|
| 1 | `:discharge` ENC-01 | `:status :discharged`, `:location` nil, encounter closed |
| 2 | `:appointment` `APT-001086-01`, `:appointment-class :outpatient`, `:reason "Follow-up"`, `:scheduled-t 3,881,040` | — |
| 3 | `:reschedule` of that appointment to `:scheduled-t` **4,572,240** | — |
| 4 | `:cancel-admit`, `:cancels-event-id 14115` | `:status :new`; `:class`, `:location`, `:home-ward`, `:attending`, `:admitted-at` all **dissoc'd** |
| 5 | `:cancel-transfer`, `:cancels-event-id 14152`, `:home-ward "Emergency"`, `:location {Emergency ED-H372 surge}` | `:location` and `:home-ward` **put back** |

From 3,017,040 the patient carries `:status :new`, **no `:class` at
all**, and a non-nil `:location` of `Emergency / ED-H372 / surge`. **No
invariant in the catalog convicts that state**: `admitted-occupies-one-slot`
judges only `:status :admitted`, and `outpatient-patients-occupy-no-bed`
judges only `:class :outpatient`.

1,555,200 seconds later the rescheduled follow-up fires:

| `:t` | event | |
|---|---|---|
| 4,572,240 | `:outpatient-visit` ENC-**02**, `:appointment-id APT-001086-01`, `:reason "Follow-up"` | sets `:class :outpatient`; **`:location` untouched** |
| 4,573,440 | `:outpatient-visit-end` ENC-02 | sets `:status :discharged`; **`:class` and `:location` untouched** |
| 272,314,003 | `:demographic-update` (`:residence-move`) | — |
| 291,600,006 | `:coverage-change` (`:eligibility`) | — |

The conviction begins at 4,572,240 and never lifts.

### The same state, unconvicted, in a green run

At `--patients 1984` — exit 0, certified clean — the same patient's
whole history is eight events:

```
1926480  :registered
1926480  :admission        ENC-001086-00   loc={Emergency ED-H082 surge}
1928280  :order-placed     ENC-001086-00
1931340  :transfer         ENC-001086-00   loc={Renal RENAL-21 licensed}
1931400  :result-available ENC-001086-00
1968180  :discharge        ENC-001086-00   loc={Renal RENAL-21 licensed}
1968180  :cancel-admit                     cancels=8874
1968180  :cancel-transfer                  cancels=8910  loc={Emergency ED-H082 surge}
```

**The identical `:cancel-admit` → `:cancel-transfer` pair, at the same
batch instant, leaving the same `:status :new` patient holding a bed
they were discharged from — and nothing after it, for the remaining
~630 million seconds of the log.** The 1,984-arrival run differs from
the 2,000-arrival one in one respect only: no scheduled follow-up ever
stamps `:class :outpatient` on top of the stale hold, so nothing
convicts.

The 1,000-arrival run (also green, and digest-matched to the downstream
report) shows the same patient with no reinstatement at all: one
admission, one discharge into `Emergency / ED-H313`, then nine years of
demographic and coverage traffic.

### The config facts the conviction depends on

All from [`test-fixtures/downstream-calibration/config.edn`](../../test-fixtures/downstream-calibration/config.edn),
sha256 `4dd4a5c0…`:

- **`:encounters true`** — what lets ENC-01 and ENC-02 exist. Without it
  `encounter-openable?` is `(= :new (:status patient))` and each patient
  gets one encounter.
- **`:scheduling {… :reschedule-rate 0.08 … :follow-up {:rate 0.25 :interval-days [7 30]}}`**
  — what produced the follow-up appointment at the discharge and moved
  it 8 days out.
- **`--churn` with defaults** — what inserted the `:cancel-admit` and
  `:cancel-transfer` into the gap after the pathway's last step.
- **`:facility`, Emergency ward `:beds 0 :surge-slots 500`** — ED-H372 is
  a surge slot; a 500-slot ED is why this pathway mix places at all at
  2,000 arrivals.
- **`:persons {:count 20000 :years 20}`** — why the log runs to `:t`
  631,353,025, and therefore why one un-cleared flag becomes 30,507 rows
  rather than a handful.

### The fork

Two readings. The evidence does not decide between them; it does
sharpen both.

**(A) The log is genuinely illegal and the engine produced it.** A
patient holds `Emergency / ED-H372` from `:t` 3,017,040 to the end of a
twenty-year log, having been discharged from it, and then attends an
outpatient visit while holding it. Three independent places could carry
a fix:

- **(A1) `log-index/subject-superseded?` lets the reinstatement
  through.** `statuses-that-supersede-a-reinstatement` is
  `#{:discharged :expired :merged}`, and **`:new` is deliberately
  excluded** — documented, measured, and named as "an adjacent case this
  change deliberately does not reach"
  ([`2026-08-29-ts-5-superseded-cancel.md`](2026-08-29-ts-5-superseded-cancel.md)).
  Here the `:cancel-admit` at event #4 is precisely what makes the status
  `:new`, so the `:cancel-transfer` at event #5 — same `:t`, same batch —
  is not blocked. **This is that adjacent case, reached.** The TS-5 close
  measured 2 such cancel-transfers at the `nobed` 10^5 cell and left them
  alone. Draw-affecting; owes its own declared sweep.
- **(A2) `evolve :outpatient-visit` sets `:class :outpatient` without
  clearing `:location`/`:home-ward`.** Its own comment says "`:location`
  and `:home-ward` are never set at all (stay absent/nil)" — true of what
  it writes, not of what it leaves. The violation begins exactly at this
  event and the location predates it by 1,555,200 s. Narrowest possible
  fix, and the one that states the documented rule
  (`:class :outpatient => :location nil`) as a transition rather than
  only as an assertion. It also does **nothing** for the 1,984-arrival
  run, where the stale hold exists with no outpatient visit behind it.
- **(A3) `encounters/encounter-openable?` never asks about `:location`.**
  It asks `(nil? (:encounter patient))` and
  `(not (#{:merged :expired} (:status patient)))`. A patient holding a
  bed they are not admitted to passes it.

**(B) The invariant is mis-scoped for this configuration shape.** Two
grounds, either sufficient on its own:

- **(B1) It is status-blind, so it outlives the visit it is about.**
  `outpatient-with-bed?` reads `:class` and `:location` and never
  `:status`. The visit ENDED at 4,573,440 — one event and 1,200 seconds
  after it opened — and `:outpatient-visit-end` leaves `:class`
  `:outpatient` standing. So **30,505 of the 30,507 rows are stamped
  after the encounter closed**, against a patient the log says is
  `:discharged`. If the rule means "for the visit's entire duration", as
  its docstring says, almost every row it produced is about something
  else.
- **(B2) Its coverage boundary, not its content, is where the defect
  shows.** The 1,984-arrival run is the witness and it is not a
  hypothetical: the same stale bed hold, produced the same way, sits in a
  log this repository exits 0 on. The rule catches that state only when a
  later, unrelated event happens to write `:class :outpatient` over it.
  If holding a discharged bed is what is wrong, it is being caught by
  accident.

**(C) Both, independently.** (A) and (B) are not exclusive. The engine
could stop producing the state *and* the rule could stop convicting a
closed encounter, and neither change makes the other unnecessary. (B2)
in particular is untouched by any of (A1)/(A2)/(A3) except (A1).

**What this session cannot tell the author**, and did not guess: which
of (A1)/(A2)/(A3) is the design's intended seam; whether (B1) is a
defect or a deliberate "the state is wrong whatever the status" reading;
and why 2,000 arrivals reach a follow-up that 1,999 do not.

### Proposed roadmap row — NOT ADDED

Fence held: `.agents/plans/roadmap.md` is untouched. Proposed text, for
the author to place, edit or discard:

```
- OPEN **[cancel-transfer-reinstates-a-new-subject]** PRIORITY n --
  MEASURED 2026-09-02 against a downstream QA calibration config
  (test-fixtures/downstream-calibration/, sha256 4dd4a5c0...): the case
  `roadmap.md#cancel-transfer-reinstates-a-discharged-patient`'s close
  named as "an adjacent case this change deliberately does not reach",
  reached. A same-batch `:cancel-admit` rewrites the subject to
  `:status :new`, which `statuses-that-supersede-a-reinstatement`
  deliberately excludes, so the `:cancel-transfer` behind it reinstates
  `Emergency / ED-H372` onto a patient already discharged from it.
  SILENT ON ITS OWN -- no invariant judges `:status :new` + non-nil
  `:location`, and the SAME state sits unconvicted in the green
  --patients 1984 run, same patient, same batch instant. It becomes a
  conviction only when a scheduled follow-up's `:outpatient-visit`
  writes `:class :outpatient` over it without clearing the bed, at which
  point `outpatient-patients-occupy-no-bed` fires and never lifts:
  30,507 rows, one patient, `PID-001086-869d73e0`, at seed 424242 /
  --patients 2000 / --reference-date 2026-08-31 / --churn. Byte-identical
  at 386e738d and at tip, 88 commits apart, across JDK 17 and 21; the
  downstream 500- and 1,000-arrival digests reproduce verbatim. Ten
  bisection probes in (1000, 2000] all pass, 1,999 included, so N_min is
  the endpoint and the count moves the trigger rather than the
  population size. FORKED, not fixed: (A1) admit `:new` to the
  superseding set, (A2) have `evolve :outpatient-visit` clear the
  location, (A3) gate `encounter-openable?` on a nil location, (B1)
  status-scope the invariant so it stops convicting a closed encounter,
  (B2) widen the catalog to judge the stale hold directly -- (B2) is the
  only one that reaches the green-run witness. Any of A is
  draw-affecting and owes a declared sweep. Record:
  `.agents/session-records/2026-09-02-downstream-self-check-failed.md`.
```

## Judgment calls, and their ratification status

1. **The log capture used `run-command`'s injectable `:engine-run-fn`
   arity.** Unratified. It is the seam that arity documents itself as,
   the script lives outside the brick tree, and no `src` file moved — but
   the prompt did not authorise it explicitly, and the obvious
   alternative (a probe inside `src`) was fenced out.
2. **A `--patients 1000` endpoint check at tip was added.** Unratified,
   and cheap: the bisection's lower endpoint was otherwise attested only
   by a downstream report at a different revision. It also produced the
   second digest match.
3. **A `--patients 1984` comparison run was read for the same patient.**
   Unratified, and not asked for. It is where the (B2) evidence came
   from, and without it the fork would have been a two-option guess.
4. **The proposed roadmap row is proposed only.** Ratification pending by
   definition — the prompt asked for exactly this.

## Findings

1. **The reproduction is exact, and stronger than the prompt asked for.**
   Two downstream digests reproduced verbatim across an 88-commit gap and
   a major JDK version change, and the failing payload is byte-identical
   at both revisions.
2. **The defect state is already shipping green.** The 1,984-arrival run
   carries the same reinstatement onto the same `:status :new` patient
   and exits 0. The 2,000-arrival failure is that state being *noticed*,
   not that state being *created*.
3. **The failure is one invariant, one patient, one moment** — not the
   "four invariant families red" shape the 2026-08-29 traffic-scale close
   recorded for the nine-key configuration at 10^4 and above. Whether
   these are the same defect at different scales is not established here.
4. **A conviction that never lifts inflates its own count.** 30,507 rows
   describe one bad state, not 30,507 bad states, and 30,505 of them are
   stamped after the encounter they are about had closed. The payload
   gives a reader no way to see that: every row is the same three keys.
5. **`--patients` moves the trigger, not just the population size**, and
   output size is non-monotone in it (1,998 → 16,559,710 bytes;
   1,999 → 16,401,176).

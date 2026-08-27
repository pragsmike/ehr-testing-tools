# Session record -- arc 3b, sweep 1 of 3: the encounter horizon, lifted

**Date:** 2026-08-26
**Prompt:** [`.agents/prompts/2026-08-26-arc-3b-encounter-horizon.md`](../prompts/2026-08-26-arc-3b-encounter-horizon.md)
**Base:** `b4e3048` -- **Tip:** `04e4df1`, the pushed tip CI ran
against, plus the one commit that can only follow it: this CI marker
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony,
no tag, CI green at the tip as the close marker.

## What landed

ADR-0174 section 2(a) -- the ENCOUNTER HORIZON -- under rulings A1, B1,
C1 and E1, landed dark and then turned on, alone, as ruling E1's three
sweeps require. Sweeps 2 (bed status, plus the ADT^A20 the author added
at ruling C) and 3 (scheduling) are untouched.

| commit | what |
|---|---|
| `f3ef935` | ADR-0174 -> Accepted, five rulings quoted where they land; `[corpus-player-slices]` corrected |
| `1ad2d81` | the encounter, DARK behind `:encounters` -- oracle IDENTICAL, 36 roots, no declaration |
| `ef563fe` | the encounter, TURNED ON in six corpora -- one declared sweep |
| `611a285` | the 37th root's own three integration-tier pins |
| `1f046bd` | this record, the prompt, the roadmap, and the tripwire row |
| `04e4df1` | the shas above, which no commit can write about itself |
| *(this one)* | the CI marker: run 33033373325, conclusion success |

## The two oracle lines, verbatim

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between b4e3048 and HEAD
```

```
--- declared-digest-change: yes (soundness: no outside the leading docstring) ---
DIFFERS: digests diverge between 1ad2d81 and HEAD -- STOP, escalate (do not fix under this script)
+2d50bbe266e5a1e8fa967e79e0e6a57ce3e3656034a950e3e7606e90c5f37bac  encounter-horizon.edn
```

**36 baseline rows, 37 target rows, and the whole diff is that one
added line.** Not one pre-existing root moved a byte in either commit.

## The gates

| gate | result |
|---|---|
| `bin/regression-oracle b4e3048 1ad2d81` | **IDENTICAL**, exit 0, no declaration, **36 roots** |
| `bin/regression-oracle 1ad2d81 HEAD --declared-digest-change` | **DIFFERS by exactly ONE manifest line**, exit 1, 36 baseline + 37 target roots |
| `make test` (dark, at `1ad2d81`) | MAKE_EXIT=0, 4,342 tests / 20,322 assertions, 0 failures, 19m07s |
| `make test` (on, at `ef563fe`) | **MAKE_EXIT=0**, 4,350 tests / 20,442 assertions, 0 failures, 0 errors, 19m00s |
| `make integration` at the tip | **INT_EXIT=0**, zero `FAIL:` lines, 1,634 tests / 5,873 assertions; both demo exercisers "every command asserted, every named invariant held, tree clean" |
| `clojure -M:poly check` | OK at both commits |
| `bin/preflight` | no findings, exit 0, at session start |
| CI at the pushed tip `04e4df1` | **run 33033373325, workflow `test`, conclusion success** -- the close marker (`rulings.md#R-session-verifies-ci-via-gh`, kept as the marker after the tag was retired). ONE run at this sha |

`make test` figures are the sum over BOTH project runs the polylith
runner performs, which double-counts bricks shared by two projects --
the same convention the previous three records use, kept so the numbers
are comparable to theirs.

## The witness table -- the horizon's cost, recovered

Max encounter openers per patient was **ONE at every corpus this repo
had**. That is ADR-0174 section 1's own census, measured at `b9d4d77`,
and it was not a coincidence of seeds: `check.clj`'s
`admission-only-when-new` was the single-encounter horizon
(`sim/ADR-0007` point 3) written as an invariant, and `evolve
:discharge` never returned a patient to `:new`.

| corpus | events, before -> after | patients with >1 encounter | encounters recovered | max/patient |
|---|---|---|---|---|
| `seed-202-ed-tuesday` | 648 -> 711 | 15 | 16 | **3** |
| `seed-424242-clinic-decade` | 1,279 -> 1,309 | 11 | 15 | **4** |
| `seed-5-clinic-decade` | 1,058 -> 1,072 | 6 | 7 | **3** |
| `adhd-seed-45` | 66 -> 68 | 1 | 1 | **2** |
| `ed-tuesday` (demo seed 20260811) | 695 -> 745 | 14 | 15 | **3** |
| `clinic-decade` (demo seed 20260807) | 1,136 -> 1,156 | 7 | 10 | **4** |

Same patient-id and same MRN on every return, which is the point: an
MPI under test has to see the same MRN twice.

**Where the recovered encounters come from differs by scenario, and the
difference is informative.** At clinic-decade all ten are PARENT
DELIVERIES (17 -> 27): its patients mostly walk no pathway, so the
hooks reach them, and a person delivering a second child used to
produce no admission for it. At ed-tuesday the parent-delivery count
did NOT move (still 1) and the fifteen recovered encounters are repeat
ARRIVALS instead -- because that scenario's patients nearly all walk a
scripted ED pathway, so `prelude`'s STATIC `clinically-idle?` filter
still refuses them a hook encounter. That static filter is untouched by
this sweep and is now the only thing left in a hook's way.

**Every person-stream count is unchanged** at both demo scenarios -- 15
unidentified arrivals, 15 newborns, 8 injuries, 3 unhoused
registrations at ed-tuesday; 28 placeholders, 29 newborns, 2 unhoused
at clinic-decade. That is the right answer and worth stating: the
person stream runs upstream of the encounter, so lifting the horizon
adds return VISITS, not people.

## The wire

PV1-19 was EMPTY on every message this project had ever produced -- one
of the 28 blanks `pv1-segment` laid down between PV1-7 and PV1-36. It
now renders the encounter's own `ENC-` id (ruling C1), and the blank
run moved 28 -> 11 + PV1-19 + 16.

`ed-tuesday`: **328 of 333 PV1 segments carry a visit number.** The
five that do not belong to no OPEN encounter and correctly say so, and
the census is gated by MSH-9 in `run_test`:

| MSH-9 | blanks | why |
|---|---|---|
| `ADT^A40` | 1 | an identification merge on a patient whose stay had already ended (its PV1-3 is empty for the same reason) |
| `ORU^R01` | 3 | results arriving after discharge -- the pending-labs case `order-only-when-admitted`'s own docstring names as real |
| `ADT^A11` | 1 | a `:cancel-admit` against an already-discharged patient's original admission, the degenerate-but-legal churn sequence |

Everything else -- A01, A02, A03, A04, A12, A13 and **both** PV1s of an
A17 -- carries one on every message of every opted-in corpus. The A17 is
worth naming: a `:bed-swap` names two encounters and one top-level
field cannot carry both, so each side's id rides its own `:swap` entry,
beside the `:active-mrn` and `:from`/`:to` already there.

**The prompt's wire witness said "PV1-19 non-empty on every PV1".** The
tree says three named classes of message legitimately carry none, and
the gate asserts that set rather than the universal claim.

## The defect the turn-on found, and units could not have

The first `seed-202-ed-tuesday` run under `:encounters` exited
`:self-check-failed`. `every-encounter-is-opened-and-closed-or-still-open`
fired twice, both on a `:merge`, both reporting `:not-exactly-one-opener`
for an id whose arrival ordinal was not the patient's own.

The cause: `run`'s stamp reads the FIRST participant's open encounter,
while `check.clj`'s `events-by-patient` puts a multi-participant event
in EVERY participant's sequence -- so a merge stamped with the
SURVIVOR's encounter also appeared in the MERGED patient's log, where
that id has no opener and never could. The invariant was right; the
reading of the field was wrong. `encounter-id-of` now attributes a
top-level `:encounter-id` to the event's first participant alone, and
says why in its own docstring.

This is the third arc running in which the defects that mattered were
found OFF the unit gates, at population scale, by an opt-in probe.

## What re-pinned, and nothing outside this list

* the four gated fixtures and their four digests (`run_test`'s
  `arc0-pinned-digest`);
* `run_test`'s reinstating-cancel count, **7 -> 9** -- the first of four
  re-counts to move that witness UP. Churn is injected per ARRIVAL, so a
  repeat arrival's injected steps are now queued instead of discarded;
* both scenario READMEs, from freshly regenerated runs: event/message/
  snapshot counts, the ed-tuesday board peak (11 -> 15 concurrent
  inpatients) and its closing snapshot, the clinic-decade board census
  (`inpatients: 0` 94 -> 84, `inpatients: 1` 87 -> 97, the other two
  unchanged), the closing summary map, and the batch listing;
* `bin/demo-exerciser-ed-tuesday`'s batch count, 105 -> 106;
* the invariant catalog roster pin, 35 -> 36 (dark commit);
* `.agents/state-derived.md` and `notes/ADRs.md`, both generated.

**CORRECTED while re-witnessing, and it predates this session:**
ed-tuesday's README printed its closing batch bucket as
`2045-09-27T13:00Z`. `:start-ms` 2390166000000 is `23:00Z`. Wrong by
ten hours, and the figure it mis-rendered never moved.

## What did NOT move, verified rather than assumed

* `demos/traces/` is byte-identical across a full `make docsgen` at
  BOTH commits -- 14 derived captures, the FHIR bundle among them;
* `docs/formats.md` and the event-schema export moved once, in the dark
  commit, for the schema change alone;
* **both committed conformance baselines are byte-equal.**
  `projects/conformance` ran clean, 0 failures, and neither
  `sim-v2-gate-baseline.edn` nor `sim-v2-full-capability-baseline.edn`
  changed a byte -- their own configs take no opt-in.

## The new oracle root, and why it is worth more than the encounter

`encounter-horizon` is the 37th root: sixty arrivals over a pool of
twenty people, a real admit/delay/discharge pathway, `--churn` on,
`:encounters` on. 170 events, 106 messages, 14 patients with more than
one encounter, 16 recovered, max 3.

It is the first root ever to pass `:churn true` over a pathway that
ADMITS, and that is where its value is:

* `:bed-swap`, `:cancel-admit` and `:cancel-transfer` leave the
  unreached-kind set -- **19 of 23 witnessed, from 16**;
* **ADT^A11, ADT^A12 and ADT^A17** join `witnessed-message-types`;
* `bed-swap-message` and `churn/inject`+`strip` leave the vacuous list;
* the capacity witness is **two roots deep instead of one** --
  `death-fixture` no longer carries the only `:transfer`, the only
  ADT^A02 and the only `:bed-ready true` by itself;
* `:merge` and ADT^A40 are no longer `demographic-fold`'s alone.

**`:cancel-discharge` STAYED unreached** -- churn's lottery did not draw
one at that seed -- and the COVERAGE block says so rather than rounding
up to "the churn family is covered". Rung 4, `:forced` and `:exhausted`
are still zero across all 37, which is what sweep 2 will have to move.

## The event contract: 1.4.0 -> 1.5.0, OWED

`classify-change` returned, against the frozen 1.4.0 baseline:

```
{:additive? false, :breaking [":bed-swap: key changed: :swap (value schema changed)"]}
```

Read precisely. The twenty-three top-level `:encounter-id` entries ARE
additive and are reported as such. What `classify-change` will not call
additive is the same `{:optional true}` key one level DOWN, inside
`[:map-of :string BedSwapSide]`, because it compares a nested value
schema whole rather than descending into it. That conservatism is
deliberate and was taken at its word rather than argued around.

So the bump is owed and was taken, baseline re-frozen with `make
event-schema-freeze`. The prompt said "bump only if owed" and ADR-0174
recommended taking 1.5.0 whether owed or not; they agree here, by
different routes. A 1.4.0-era log validates unchanged against 1.5.0 --
every key added is optional at both levels.

## ADR premises the tree contradicted

1. **There is no committed FHIR conformance baseline.** The prompt's
   step-2 re-pin list expects "both conformance baselines (the FHIR one
   moves: Encounter ids)". Both baselines under
   `projects/conformance/test-fixtures/reports/` are v2 (HAPI); the
   FHIR gate lives in `projects/integration/baseline_gating_test.clj`
   and builds its baseline at RUNTIME from the file under test, so
   there is nothing to re-pin. Neither v2 baseline moved.

2. **`discharge-follows-admission` has never enforced "and not twice".**
   ADR-0174's table says that clause "moves" to the per-encounter form.
   It could not move, because it was never there: the function's body
   tests only that no discharge precedes the patient's FIRST admission.
   Measured -- four lines, none of them counting anything.
   `discharge-closes-an-open-encounter` is the first code to make the
   claim true, and a test asserts the old row's silence so the finding
   cannot be re-forgotten.

3. **`config-latency.edn` is a seventh FILE and not a seventh corpus.**
   The prompt fences the opt-in to six corpora. `demos/scenarios/
   ed-tuesday/config-latency.edn` is `config.edn` plus a `:latency`
   block, and `bin/demo-exerciser-ed-tuesday` asserts the two produce
   byte-identical ground truth (ADR-0109's second-clock zero-offset
   identity). Any engine-facing key in one must be in the other. The
   exerciser's own `diff` went red and is how this surfaced; the key
   was added with that reasoning in the file.

4. **`ehrt.sim.run/run-command`'s `engine-params` is an explicit
   select-keys list, not `config-keys`.** `:encounters` joined it in the
   turn-on commit for the reason ADR-0173 gave `:persons`: a corpus
   whose patients can have more than one visit is a different artifact,
   and its manifest should say so on its own face.

## Red before green

Every gate this session added was proved red first, by neutering the
code it checks and running the real suite. Full output, not filtered:

* `admission-only-when-no-open-encounter` returning nothing -- **4
  failures**, including both absorbing-terminal cases and the absorbed
  `outpatient-visit-only-when-new` case.
* `discharge-closes-an-open-encounter` returning nothing -- **1
  failure**.
* `every-encounter-is-opened-and-closed-or-still-open` returning
  nothing -- **8 failures**, including the bed-swap-side case.
* `carried-encounter-is-not-the-open-one?` forced false (the
  per-encounter half of the three moved validity rows) -- **3
  failures**, one per row.
* `:encounters` removed from `:adhd-seed-45`'s opts -- **13 failures**
  across six deftests, including every new population-scale witness,
  the pinned-digest gate and the live-corpus gate.

## The integration tier held three pins the push lane cannot see

`make integration` went red after `ef563fe` on THREE assertions, all in
`projects/integration`'s own oracle-coverage gate, all of them counts
the 37th root moved: 36 -> 37 `.edn` files, 33 -> 34 engine-layer roots,
and the CAPACITY-WITNESS ROOT LIST, `["death-fixture"]` ->
`["death-fixture" "encounter-horizon"]`. `611a285` is those three pins,
its own commit because
`rulings.md#R-amend-unpushed-message-only` forbids amending content
into an existing commit even unpushed.

**This is review 4's `W-1` firing again** -- `make test` skips the
integration tier, so a gate can land unexecuted -- and it is the same
shape arc 3a's own `1fb1957` had. Named here rather than filed as a
finding of its own, per the de-scaffold ruling.

**The half that did NOT move is the reassuring half.** The fresh
37-root digest's `witnessed-event-kinds` and `witnessed-message-types`
both matched what `ef563fe` had already committed, first try. The three
added kinds and three added MSH-9s were PREDICTED before editing
(`digest.clj:733`'s own rule) rather than discovered by the gate.

## The stale-asset tripwire, and why nothing was redrawn

`docs/manual/assets/straddle-timeline.svg` cites
`demos/scenarios/ed-tuesday/README.md`, which changed at `ef563fe`, so
the tripwire fired for the fourth time. Every value that diagram draws
was re-witnessed against the fresh run and NONE moved: MRN000002 admits
at 00:37:39Z and discharges at 01:59:02Z, batch-000 is [00:00Z, 01:00Z)
carrying 2/2, batch-001 is [01:00Z, 02:00Z) carrying 4/4, and the
window boundaries are untouched. So the row's `:reviewed-at` bumps and
the asset does not.

Worth keeping, because it is the reason a HORIZON LIFT left a STRADDLE
diagram alone: the encounter that asset depicts is MRN000002's FIRST,
and a first encounter is exactly what the lift does not touch.

## One process deviation, disclosed

`git commit --amend` was run once, on `1f046bd` and while it was
UNPUSHED, to fill the two sha placeholders in this record. That is a
CONTENT amend and `rulings.md#R-amend-unpushed-message-only` allows an
amend only for a MESSAGE. Caught immediately, reverted by
`git reset --hard 1f046bd` (the amended commit `7217775` was never
pushed and exists only in this clone's reflog), and redone the way the
rule requires: the fill is its own commit.

The structural fact underneath it is the same one the
`straddle-timeline.svg` tripwire row already records: a commit cannot
carry the sha that names itself, so a record that names its own tip is
always one commit behind. The remedy is a following commit, never an
amend, and this paragraph is here so the next session does not have to
rediscover that.

## Open, for the sweeps that follow

* **Multiple CONCURRENT open encounters are still not possible.** A
  repeat arrival landing while the patient's first encounter is open
  opens nothing (`decide :repeat-arrival`), and both absorbing
  terminals still refuse. `patient-state-model.md`'s
  encounters-as-first-class bullet now says which half landed and which
  did not.
* **The clinical-content accumulators carry `:encounter-id` but nothing
  restructures them.** ADR-0174's own "What this ADR does NOT design"
  names this; the stamp makes it possible and does not take it.
* **`prelude`'s `clinically-idle?` is now the only static wall a hook
  meets**, and it is stricter than the runtime guard needs to be. Named,
  not taken.

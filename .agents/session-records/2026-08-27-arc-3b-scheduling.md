# Arc 3b sweep 3 — scheduling as state (ADR-0174 §2(b))

Session of 2026-08-27, from `ab156c3`. Ceremony mode: R30 default
(commit and push at each checkpoint, unattended). Arc 3b's third and
last sweep, closing arc 3.

| commit | what |
|---|---|
| `41a34f0` | ADR-0174 §2(c) gains three ratifications; item 4's premise WITHDRAWN |
| `dfdb7b2` | scheduling, landed DARK behind `:scheduling` |
| `d6e6546` | scheduling, TURNED ON in six corpora, plus `scheduling`, the 39th root |
| `bbafa5b` | the straddle timeline's own review sha, which its own commit could not write |

## Preflight

`bin/preflight` exit 1, one finding, disclosed: a red run among the last
five on main — `33067974420` at `9b1a9b3`, sweep 2's own stale-asset
tripwire, remediated by `36ea745`. Two greens since; HEAD green at
`33070870350`. Not a blocker.

Disclosed method error, caught in the act: the first preflight run was
piped through `tail`, which reported `PREFLIGHT_EXIT=0` for a script
that exits 1 — the exact mask the build-session skill names. Re-run
unpiped before anything else was read.

## Both oracle lines

```
bin/regression-oracle ab156c3 dfdb7b2
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between ab156c3 and HEAD
ORACLE_EXIT=0
```

```
bin/regression-oracle dfdb7b2 bbafa5b --declared-digest-change
--- declared-digest-change: yes (soundness: no outside the leading docstring) ---
DIFFERS: digests diverge between dfdb7b2 and HEAD
ORACLE_EXIT=1
```

The DIFFERS manifest diff is **exactly one line**, and it is an
addition:

```
@@ -22,6 +22,7 @@
  20bfe1b6…  rheumatoid-arthritis.edn
 +21c977a0a12717dfc5020a638818f8ad4ccc328eabd15b140248d962975c48b9  scheduling.edn
  10b4c1f2…  sepsis.edn
```

**38 IDENTICAL, one `+`** — the predicted mover set, met exactly. Exit 1
is the script's DIFFERS signal and is correct here: the manifests
genuinely differ by one added root. `--declared-digest-change` covers
the `digest.clj` SOURCE soundness check, which differs because this
sweep adds `scheduling-pair` to it.

## The witness table

Measured on the shipped configs, not predicted:

| corpus | appt | resch | cancel | no-show | schArr | followUp | events | rung |
|---|---|---|---|---|---|---|---|---|
| `seed-202-ed-tuesday` | 50 | 6 | 4 | 4 | 40 | **34** | 1,131 → 1,213 | 3 |
| `seed-424242-clinic-decade` | 42 | 3 | 5 | 6 | 29 | **29** | 1,660 → 1,774 | 2 |
| `seed-5-clinic-decade` | 27 | 3 | 7 | 3 | 15 | **15** | 1,342 → 1,412 | 2 |
| `adhd-seed-45` | 2 | 0 | 0 | 1 | 1 | **1** | 92 → 97 | 2 |
| `ed-tuesday` (demo) | 44 | 5 | 3 | 5 | 35 | **21** | 1,151 → 1,269 | — |
| `clinic-decade` (demo) | 40 | 6 | 9 | 4 | 27 | **27** | 1,456 → 1,569 | — |

The headline is `followUp`: second encounters that are SCHEDULED, each
booked at its own patient's discharge and each opening an encounter that
names its appointment.

**Not every cell is `pos?`, and no seed was hunted to make it so.**
`adhd-seed-45` shows 0 reschedules and 0 cancels — ten patients, two
appointments, an 0.10 reschedule rate, so an expectation of 0.2 events.
The SET covers all four outcomes many times over; that one run does not.

## The margin table

The **capacity gate ran before the opt-in, not after it**, because
`:exhausted` HALTS a run. The margin measured is the union of beds the
FOUR RUNGS can reach — never one ward's own count.

| corpus | worst home ward | before | after |
|---|---|---|---|
| `seed-202-ed-tuesday` | Renal | 7/26 free (26.9%) | 10/26 (38.5%) |
| `seed-424242-clinic-decade` | Emergency | 11/14 (78.6%) | 11/14 (78.6%) |
| `seed-5-clinic-decade` | Emergency | 12/14 (85.7%) | 12/14 (85.7%) |
| `adhd-seed-45` | Emergency | 12/14 (85.7%) | 12/14 (85.7%) |

**The prompt's `<10%` fence fired on one reading and not the other, and
that was a STOP-AND-REPORT.** Per-ward, `seed-202`'s Renal is at **0%**
— peak 6 of its own 6 beds non-ready, before AND after. Under the ladder
reading the same corpus is at 26.9%. Reported to the author with both
tables; **ruled: the ladder union governs**, because per-ward saturation
cannot produce `:exhausted` — the ladder spills to rungs 3 and 4, and
only the union can be exhausted. Re-measured with scheduling ON before
the opt-in was committed, per that ruling.

**And the finding the gate bought: scheduling LOOSENS the one tight
corpus rather than tightening it**, which is the opposite of what the
prompt's own premise ("scheduling ADDS arrivals") predicts. Cancels and
no-shows REMOVE admissions; lead times SPREAD arrivals off their
original instants; and a follow-up produces an `:outpatient-visit`,
which occupies no bed at all. The ladder reaches rung 3 once at
`seed-202` (was three times) and rung 4 nowhere.

## Draw counts

Fixed on both sides, and gated as fixed rather than asserted.

* **`:world`, two per arrival ordinal, always** — the
  scheduled-vs-walk-in Bernoulli and the lead time, in ordinal order in
  the pre-loop block, immediately after ADR-0173's person-selection
  uniform. Gated by arrival INSTANTS being identical across
  `:scheduled-fraction` 0.1 and 0.9.
* **`:patient`, two per appointment, always** — one banded uniform for
  the outcome, one reschedule offset taken whether or not a reschedule
  fired. Gated by every arrival-side (`-00-`) appointment id being
  identical across two `:cancel-rate` values, and by every patient still
  registering at the same instant with the same id across two outcome
  rates.
* **`:patient`, two more at `decide :discharge`** when the run opted in
  — the follow-up Bernoulli and the interval, the second taken whether
  or not the first fired.
* **Zero draws** for an appointment ID: `appointment-id-for` is
  `encounter-id-for`'s law one level sideways, pure over
  (seed, arrival ordinal, appointment ordinal), off every stream.

**The follow-up COUNT is data-dependent and the ADR licenses it** — *"the
number of appointments a patient has may be data-dependent without
breaching the fixed-consumption law, which exists so draw count never
depends on ANOTHER patient's data."* A cancel takes the visit, the visit
takes the discharge, the discharge is what books the follow-up — all
inside one patient. The gate asserts arrival-side equality only; a gate
asserting follow-ups equal too would assert something the design
deliberately does not promise. That was a test bug found by running it.

## Filters and traps hit

**Two real defects, both caught by gates, both fixed here.** Neither was
reachable from the unit fixture; both came from a population-scale run.

1. **The follow-up's visit opened with no guard.**
   `bin/demo-exerciser-ed-tuesday` went `:self-check-failed` on
   `admission-only-when-no-open-encounter` for a follow-up that opened
   while its own patient's encounter was still open, with a cascade of
   `outpatient-patients-occupy-no-bed` behind it. **The guard has to be
   asked at `:scheduled-t`, not at the booking instant** — a world
   `decide :appointment` cannot see — so the carried steps now route
   through `:repeat-arrival`, which IS that guard asked at the right
   moment and which already propagates the stamp inward.

2. **Appointment ordinals could repeat.**
   `appointment-reaches-at-most-one-terminal` — *the invariant the ADR
   marks OWED, the one rows 1–3 cannot see* — reported
   `:terminals [:kept :kept :no-show]` on ONE id. **Appointments can
   OVERLAP**: a repeat arrival books at its own instant, which may fall
   while a previous encounter is open. A second booking displaced the
   open record, `:appointments` never grew, and
   `next-appointment-ordinal` handed out the same ordinal twice.
   `evolve :appointment` now ARCHIVES a displaced record instead of
   dropping it, and `resolve-appointment` looks in BOTH places.

   The owed invariant paid for itself on its first population-scale run.

**The stale-asset tripwire was PREDICTED, not discovered.** Its source is
`demos/scenarios/ed-tuesday/README.md`. The registry's sources were read
BEFORE any doc was touched, the asset was re-witnessed against a fresh
exerciser run, and the redraw landed in the same commit as the source
edit. The depicted fact survived — three windows, the middle one holding
neither half, verified by `grep -cF MRN000002 batch-001.hl7` returning 0
— and two printed values moved and were redrawn: batch-002's count
16 → 11, the discharge's transmit time 02:13:46Z → 02:10:37Z. The
admission's own 00:37:39Z has now held across five redraws.

**`d6e6546` is nonetheless a RED-FIRST commit**, disclosed under
`rulings.md#R-red-pushed-with-green` and pushed with `bbafa5b`. The
tripwire reads `git log -1` on a source that commit edits, so no value
written inside it can be its own sha. Structurally impossible in one
commit; the pushed TIP is green.

**`check.clj` may not call `volatile!`.** Both order-dependent invariants
were first written with a volatile accumulator; `sim_purity_lint_test`
(ADR-0108) reddened. They are pure left folds now. The lint was right.

**Five hardcoded counts moved 24 → 28** (the closed vocabulary):
`event_log_doc_test`, `oracle_coverage_test`, `limitations_test`,
`event_schema_test`; and 38 → 39 roots in `oracle_coverage_test` plus
`digest.clj`'s own docstring. Each re-commented with what moved it.

**A fifth licensed re-count**: `seed-202`'s reinstating-cancel witness
9 → 8 (7 `:cancel-transfer` + 1 `:cancel-discharge`), still non-vacuous.
It moves DOWN for the mirror image of the reason sweep 1 moved it up:
churn is injected per ARRIVAL, and a scheduled arrival whose appointment
is cancelled or no-showed never runs its steps.

**One self-inflicted syntax error, disclosed**: a re-pin note quoting
*"scheduling adds arrivals"* put raw quotes inside a Clojure docstring
and broke `run_test.clj`. Caught by the suite, fixed, and every other
edited Clojure file balance-checked before the next run.

## ADR premises the tree contradicted

1. **"Nothing about the main loop changes" does not survive sweep 2.**
   §2(b) says the follow-up rides the same `schedule-followup` seam and
   that the loop is untouched — *"the third arc in a row that has been
   true."* Under `:scheduling` AND `:bed-cycle` one `decide :discharge`
   owes TWO followups: the bed it dirtied and the visit it booked. The
   loop now accepts a SEQUENCE; a single map is normalised to a
   one-element sequence and takes the identical path, so every other
   site is untouched. §2(b) was written before sweep 2 gave the
   discharge a followup of its own — the collision is real, not a design
   change.

2. **§2(b) tables THREE invariants and owes a FOURTH; the prompt asked
   for three.** The prompt's citation `:439-447` runs into invariant 4's
   first line, and the ADR is explicit that 1–3 are jointly satisfiable
   by a log where an appointment is both cancelled and kept. All four
   landed. Fix-forward with disclosure under
   `rulings.md#R-stop-only-on-two-defensible-readings` — a mechanical
   conflict with one defensible reading. Invariant 4 then caught defect
   2 above, which rows 1–3 could not see.

3. **The ADR states NO DEFAULT VALUES for any of the six sub-keys.** The
   prompt says to opt in "at the ADR's default sub-keys". §2(b) names
   the six keys and gives no numbers. Every value in the shipped configs
   is this session's own choice on clinical grounds, disclosed as a
   choice in each config's own comment. The one sub-key the two
   scenarios genuinely disagree about is `:scheduled-fraction` — 0.15 at
   `ed-tuesday` (an ED shift is walk-in traffic), 0.70 at
   `clinic-decade` (a decade of ambulatory care is booked traffic). A
   single default across both would have been wrong about one of them.

4. **"Scheduling ADDS arrivals" is false in the direction that matters.**
   The prompt's capacity gate rests on it. Measured, it does the
   opposite — see the margin table.

5. **Four ADR line citations had drifted** and were re-derived by name
   rather than trusted: `engine.clj:412` → `:615`, `:1415` → `:1359`,
   `:2158` → `:2864`, `:3333` → the placeholder block. `engine.clj:733`
   (`exhausted-outcome`), `run.clj:573` and `check.clj`'s own sites were
   exact.

## The contract: 1.6.0 → 1.7.0, NOT owed, taken

`classify-change` against the frozen 1.6.0 baseline returns
`{:additive? true :breaking []}` — four new kinds and one new optional
key are exactly the two shapes it names non-breaking. **No bump is
owed.** It is taken on 1.3.0's own stated grounds: `:event-schema-version`
is a consumer's only handle on what a log it holds can CONTAIN, and a
1.7.0 log may carry four kinds a 1.6.0-era consumer will dispatch on
`:event` for. A 1.6.0-era log validates unchanged, in the strong sense —
nothing existing moved at all.

**None of the four reaches the wire** (ruling C): the SIU family is v2.4
structure and every message carries MSH-12 `"2.3"`. Recorded in all
three places `event-conformance-test` demands — each kind's `:doc`,
`message-type-registry`'s own comment, and that gate's silent set.

## Gates

| gate | result |
|---|---|
| `make test` (dark, `dfdb7b2`) | `MAKE_EXIT=0`, 378 namespaces, 0 failures |
| `make test` (on, `bbafa5b`) | `MAKE_EXIT=0`, 378 namespaces, 0 failures |
| `clojure -M:poly check` | OK |
| `ehrt.sim-engine.scheduling-test` | 27 tests, 758 assertions, 0 failures |
| RED capture (whole mechanism stashed) | compile failure — nothing existed |
| RED capture (opener stamp neutered) | **11 assertions across 6 tests**, including `invariant-2-is-non-vacuous-and-here-is-the-count` |

The second red capture is the one that matters: it proves the
non-vacuity gate BITES. A gate asserting only greenness would have gone
silent when the stamp disappeared.

## Roadmap

`[engine-fold-extensions]` CLOSED — arc 3 complete. The MSH-12/SIU
question stays rowed for arc 4, alongside the halt-vs-reject question
this sweep opened and did not answer.

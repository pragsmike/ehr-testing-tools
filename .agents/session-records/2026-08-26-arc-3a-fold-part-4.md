# Session record -- arc 3a, part 4: hooks, identification, and the turn-on

**Date:** 2026-08-26
**Prompt:** [`.agents/prompts/2026-08-26-arc-3a-fold-part-4.md`](../prompts/2026-08-26-arc-3a-fold-part-4.md)
**Base:** `dd4cf8d` -- **Tip:** `a920ea3`
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony,
no tag, CI green at the tip as the close marker.

## What landed

ADR-0173 sections 2(c) and 2(d) -- the two clinical hooks and the
identification flow -- and then ruling D1's COMMIT 2, the single
declared sweep that turns `:persons` on. Arc 3a's half of
`roadmap.md#engine-fold-extensions` is CLOSED.

| commit | what |
|---|---|
| `a70a2c2` | hooks and identification, dark -- oracle IDENTICAL, 35 roots, no declaration |
| `279df87` | the fold turned on -- six corpora, one declared sweep |
| `acd9f4a` | the oracle harness needs `run-command`'s own require closure |
| `7e06eb5` | this record, the prompt, the roadmap, and the straddle asset the tripwire caught |
| `8f1bad6` | the asset register's own `:reviewed-at`, which can only be written after the commit it names |
| `1fb1957` | the INTEGRATION harness builds the same classpath by hand, and only one of the two had been taught |

## The two proofs

| gate | result |
|---|---|
| `bin/regression-oracle dd4cf8d a70a2c2` | **IDENTICAL**, exit 0, no declaration, **35 roots** |
| `bin/regression-oracle a70a2c2 HEAD --declared-digest-change` | **DIFFERS by exactly ONE manifest line**, exit 1, **35 baseline + 36 target roots** -- the added `demographic-fold.edn` and nothing else |
| `make test` (step 1) | MAKE_EXIT=0, 4,272 tests / 20,038 assertions |
| `make test` at the tip | **MAKE_EXIT=0**, 4,284 tests / 20,098 assertions, 0 failures, 0 errors (18m17s) |
| `make integration` at the tip | **INT_EXIT=0**, 0 `FAIL:` lines, 1,601 tests / 5,702 assertions; both demo exercisers "every command asserted, every named invariant held, tree clean" |
| `clojure -M:poly check` | OK |
| CI at the pushed tip `a920ea3` | **run 33015547985, conclusion success** -- the close marker (`rulings.md#R-session-verifies-ci-via-gh`, kept as the marker after the tag was retired). One run at this sha, the `test` workflow |

Step 1's dark line, verbatim:

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between dd4cf8d and a70a2c2
```

Step 2's declared line, and the WHOLE of its manifest diff, verbatim:

```
--- declared-digest-change: yes (soundness: no outside the leading docstring) ---
DIFFERS: digests diverge between a70a2c2 and HEAD -- STOP, escalate (do not fix under this script)
+6f6d5e9f1e67b953ab17a817b031b339e32ac85c0f39e70a030cf4b54f7e5ada  demographic-fold.edn
```

**ONE `+` line, no `-` line, and no existing root moved** -- which is
the claim this session owed. `DIFFERS` and exit 1 are the EXPECTED
verdict here rather than a finding: the new root is a FIRST BASELINE
and the baseline side cannot produce a file for it. What would have
been a STOP is any of the 35 pre-existing digests differing, and none
does. The soundness check reports `no` because `digest.clj`'s body
genuinely changed -- the new root and the COVERAGE block -- which is
what `--declared-digest-change` is for, and the coverage widening it
records is below.

## What a later reader cannot re-derive

### 1. ADR-0173 section 2(d)'s placeholder rule is unreachable as written

Section 2(d) reads the placeholder registration as an arrival that
COINCIDES with an open `:identity-unavailable` window. At the person
process's own rates that coincidence never happens, and this was
MEASURED rather than reasoned about. Over a 200-person, ten-year walk
at seed 424242 the process opens **9 windows covering 11,491,200 of
63,072,000,000 person-seconds -- 0.018% of the horizon** -- and the
EARLIEST of them opens at **t 36,118,094 (day 418)**, while every t0
arrival of a scenario at `:arrival-gap` 5 has happened inside the first
60,000 seconds. The two intervals cannot meet.

The rule is implemented exactly as written AND joined by its own
antecedent: an `:identity-unavailable` event is not a state somebody is
quietly in, it is an unidentified PRESENTATION -- which is what the
author's *"unhoused unresponsive John Does"* describes -- so it mints an
ED arrival the same way `:occupational-injury` does.
`person-fold/hook-kinds` carries the measurement. One defensible
reading once the numbers are in, so fix-forward with disclosure.

### 2. Three defects, none reachable from any pre-existing fixture

All three were found by probing real corpora during the opt-in, not by
reasoning, and each now has a unit gate written for it.

1. **The engine promised a close instant it could not keep.**
   `clinic-decade` seed 5 over an 800-person pool exited
   `:self-check-failed` on
   `every-placeholder-registration-is-resolved-or-still-open` for
   `PID-000208-f8f59cb6`. That patient's person opened an identity
   window at t 62,829,345 due to close at 65,248,545 and **died at
   64,751,457 -- 497,088 seconds short of it**, so the process
   correctly emitted no resolution and nothing could ever resolve the
   placeholder. `:window-close-t` now rides ONLY a window that
   actually resolves; the invariant's own "carries none, cannot be
   judged" clause covers the rest. A window that DOES resolve still
   carries its close, so a resolution the engine failed to mint still
   goes red.
2. **The resolution step was queued on the survivor.** The run loop
   short-circuits a queue entry whose patient is `:merged`, so the step
   vanished the moment anything merged that survivor away. Queued on
   the PLACEHOLDER now, with `decide :identification-merge`
   degenerating to a fill when the world refuses the merge -- section
   2(d)'s no-survivor rule, applied at decide time.
3. **`ehrt play` died on the John Doe.**
   `v2-replay/hl7-date->iso` threw a NullPointerException on a
   placeholder registration's EMPTY PID-7. Every other reader in that
   namespace was already nil- and blank-safe. A user replaying a real
   corpus would have hit this first.

### 3. The payer-pool interface gap, closed rather than forked

`ehrt.person-simulator.process`'s own docstring records that a run
naming neither `:payers-under-65` nor `:payers-65-plus` gets NO
`:coverage-change` events -- the variates are drawn and the event is
not -- and no scenario config names them. Measured: **zero
`:coverage-change` across all four gated corpora**, for a kind contract
1.3.0 declares. `sim-model`'s two real pools are promoted onto its
interface and `ehrt.sim.run/person-walk-config` defaults to them.
Restating the pools in a scenario file was the alternative, and it is
the forked-pool drift that component's own docstring forbids for
addresses. After: **510 across the four**.

### 4. Why `:count` is twice the arrival count and `:years` is twenty

Both are measurements, and both are recorded in the configs themselves.

* At one person per arrival the birthday paradox still leaves better
  than a third of the arrivals as repeats, and a repeat queues NOTHING.
  Module coverage thinned far enough to take the only module-compiled
  `:admission`/`:discharge` pair in any gated corpus with it, and
  `gated-runs-collectively-produce-every-emittable-event-type` went red
  on exactly those two kinds. At twice the arrival count both return.
* At ten years this population produced ZERO occupational injuries --
  the hazard is conditioned on `:employed` person-years -- so the hook
  would have been declared and never witnessed. At twenty it fires.

### 5. The adhd slot moved seeds a second time, 130 -> 45

`:persons` moves where a bound patient's Persona comes from, so it
reshuffles which patient walks what. At seed 130 with `:persons` on,
this run produces six registrations, fourteen `:demographic-update`s
and NOT ONE cited end -- both ADR-0163 counted witnesses vacuous at
once, which is the failure repo review 5 predicted for this run (L1-7)
a second time. A sweep of seeds 0-799 under the live engine with
`:persons` on yields FOURTEEN with one cited `:medication-end` and one
cited `:care-plan-end`; 45 is the smallest and is taken for that reason
alone, the same criterion the seed-2 -> seed-130 re-point used.

## The wire witnesses

Over the COMMITTED baselines. Every one `pos?`, none pinned
(`rulings.md#R-empty-population-is-red`).

| | ed-202 | cd-424242 | cd-5 | adhd-45 |
|---|---|---|---|---|
| events | 648 | 1,279 | 1,058 | 66 |
| registrations | 103 | 227 | 224 | 14 |
| placeholder registrations | 8 | 24 | 33 | 1 |
| identity fills | 7 | 19 | 29 | 1 |
| identification merges | 1 | 5 | 4 | 0 |
| unhoused registrations | 3 | 3 | 5 | 0 |
| newborn registrations | 14 | 38 | 24 | 4 |
| birth encounters | 14 | 38 | 24 | 4 |
| parent delivery encounters | 0 | 23 | 14 | 1 |
| occupational-injury arrivals | 5 | 16 | 12 | 1 |
| unidentified arrivals | 8 | 24 | 33 | 1 |
| `:demographic-update` | 197 | 478 | 372 | 28 |
| `:coverage-change` | 72 | 240 | 190 | 8 |

**ed-tuesday's zero parent-delivery encounters is the
single-encounter horizon doing its job**, not a gap: its patients
nearly all walk a scripted ED pathway, and a hook may only put an
encounter on a patient whose own queue is otherwise empty. The gate
asserts each family `pos?` ACROSS the set, and separately that no
single corpus is the sole witness for every family at once.

**The never-due placeholder is NOT exercised at population scale**, and
that is disclosed rather than gated: a person dying inside their own
window is rare at 0.004 per person-year, and a `pos?` on it here would
be a coin flip a hazard retune could flip. It is exercised as a unit,
on a stream that puts the death inside the window by construction.

## What re-pinned, and what did not

| artefact | before -> after |
|---|---|
| `arc0_gated_seed_202_ed_tuesday.edn` | 393 -> 648 events |
| `arc0_gated_seed_424242_clinic_decade.edn` | 343 -> 1,279 |
| `arc0_gated_seed_5_clinic_decade.edn` | 363 -> 1,058 |
| `arc0_gated_adhd_seed_130.edn` -> `_45.edn` | 12 -> 66, and a new seed |
| `arc0-pinned-digest` | all four |
| seed-202's reinstating-cancel count | 9 -> 7 |
| ed-tuesday's batch listing | 32 -> 105 |
| ed-tuesday's latency-disorder figure | 8 of 92 -> 3 of 110 |
| both scenario READMEs | regenerated runs |
| `--rate` in both scenarios | 100,000 -> 10,000,000 |
| `.agents/state-derived.md` | 35 -> 36 oracle roots |

**NOT re-pinned, and deliberately:** `arc0-pre-partition-digest` (it
records what the slot digested BEFORE the partition and nothing since),
the ADR-0169 F3 byte/value tripwire, the 35 invariant names in
`arc0-invariant-catalog` (part 4 adds no invariant), both conformance
baselines and every `demos/traces` byte -- the trace configs do not read
the scenario files, and `make traces` regenerated with nothing moved.

**The latency-disorder direction is worth reading twice.** 8 of 92
became 3 of 110 -- the denominator grew and the numerator shrank. The
eighteen added admitted patients are all HOOK encounters spread across
twenty years, whose admission-to-discharge gaps are hours or days;
`config-latency.edn`'s bands are 15 to 90 minutes, so those encounters
cannot disorder at all. The three that do are the scripted shift's own.

## `--rate`, and why a taught command changed

`--rate` is stream-seconds per wallclock-second, so it has to be read
against the stream it paces. Both scenarios went from ten years and 35
hours to nineteen years each. At 100,000, clinic-decade's board play
took **9m40s** and its events ticker roughly **an hour**, almost all of
it spent skipping gaps one five-second `--idle-cap` timeout at a time
(`:skip-count` 98). At 10,000,000 both play in about a minute with
`:skip-count` 0, and the full `bin/demo-exerciser-clinic-decade` run is
**150s**. The board census is byte-identical at 100,000, 1,000,000 and
10,000,000 -- the number paces the demo and changes nothing it shows.

## The oracle's 36th root

`demographic-fold` -- the first root to turn `:persons` on, a FIRST
BASELINE, and the reason step 2's oracle run carries
`--declared-digest-change`. Measured on its own output rather than
predicted: 671 events, 134 messages, kinds `#{:admission
:coverage-change :demographic-update :discharge :medication-order
:merge :outpatient-visit :outpatient-visit-end :registered}`, MSH-9s
`#{"ADT^A01" "ADT^A03" "ADT^A04" "ADT^A40"}`.

**Five surfaces leave the vacuous set**: the `:merge` KIND,
`merge-message`, `mrg-segment`, `ADT^A40`, and the two kinds the fold
mints. `witnessed-event-kinds` goes 13-of-21 to 16-of-23;
`witnessed-message-types` gains ADT^A40. The `decide` dispatch count is
NOT re-instrumented and the COVERAGE block says so rather than carrying
an updated-looking number -- the multi gained five methods this arc, so
both numerator and denominator moved and only a re-run of review 4's
battery can answer it.

**It is now a single point of failure**, stated rather than discovered
later: `:merge`, `:demographic-update`, `:coverage-change` and ADT^A40
are all this one root's alone.

## Contract 1.3.0 -> 1.4.0, and this bump IS owed

Unlike 1.3.0's. `classify-change` against the frozen 1.3.0 baseline
returns `:additive? false` with exactly four reasons, all on
`:demographic-update` and all WIDENINGS -- `:cause` gained
`:identity-fill`, `:field` gained `:identity`, and `DemographicValue`
gained the identity arm the other two need. Everything else part 4 adds
is additive: five optional keys on `:registered`, three more on
`:demographic-update`, one on `:admission`, two on `:merge`. No kind
was added or removed. MINOR, because a 1.3.0-era LOG validates
unchanged against 1.4.0 -- every value a 1.3.0 producer could emit is
still in range.

## Red before green

Step 1's gates were run with the src stashed back to `dd4cf8d` and the
tests kept:

| namespace | red |
|---|---|
| `ehrt.sim-engine.persons-test` | 28 tests / 156 assertions, **53 failures, 1 error** |
| `ehrt.sim-check.person-invariants-test` | 14 / 66, **6 failures** |
| `ehrt.sim-emit-hl7.emit-hl7-test` | 68 / 230, **6 failures** |

## The full suite found what the targeted runs could not

`rulings.md#R-full-suite-before-push`, paid twice more this session.

* **Step 1:** two part-3 test ASSUMPTIONS in `ehrt.sim.persons-run-test`
  -- a brick this session had not touched. `#"^PERSON-\d+#\d+$"` is not
  the person id space (a NEWBORN's is `PERSON-nnnnnn/bK`, and part 3
  could not see it because a newborn had no patient for their events to
  fold onto), and `(filter :residence registered)` is no longer the
  unhoused set (`:residence` now has two producers). Both corrected
  rather than relaxed.
* **Step 2:** `.agents/state-derived.md` went stale on the oracle-root
  count -- a generated page, caught by its own freshness gate.

## Two process shapes worth naming

**A SECOND HAND-BUILT CLASSPATH.** `bin/regression-oracle` is not the
only harness that stands up `digest.clj` by hand:
`ehrt.integration.oracle-coverage-test` builds its own to run a FRESH
36-root digest and gate the committed COVERAGE claim against reality.
Teaching one and not the other left the second dying at load time --
and its own guard rail did exactly its job: *the digest ran at all -- a
failed run must never read as agreement* failed FIRST, before any set
comparison, so a dead harness could not be mistaken for a coverage
match. `make test` never sees this tier (review 4's standing W-1), which
is why `make integration` runs before a push.

**THE RECORD COMMIT IS RED ON ONE GATE AND ITS SUCCESSOR IS WHAT MAKES
IT GREEN**, disclosed under `rulings.md#R-red-pushed-with-green` and
pushed with it. `hand-owned-asset-freshness-test` compares an asset's
`:reviewed-at` against `git log -1` on its cited SOURCE, so a commit
that changes the source cannot also carry the sha that names itself.
The straddle SVG's source is `demos/scenarios/ed-tuesday/README.md`,
which this session changed twice; the register bump therefore rides one
commit behind, and amending was not available
(`rulings.md#R-amend-unpushed-message-only`: a content change is a new
commit).

**THE STALE-ASSET TRIPWIRE FIRED AND WAS RIGHT**, for the second time
in two arcs. `docs/manual/assets/straddle-timeline.svg` cites the
ed-tuesday README's straddle prose, and the opt-in moved it: MRN000002
-- the same ORDINAL again -- is now Hernandez, Sandra, admitting at
00:37:39Z (unchanged) and discharging at 01:59:02Z rather than
01:33:03Z. Redrawn by hand, following the 2026-08-25 precedent
exactly: the depicted fact, the window boundaries and the drawing's
whole geometry are untouched and three text values moved. The README's
own straddle paragraph carried the same stale time and is corrected
with it.

## What part 4 leaves open

One roadmap row, `[multi-encounter-horizon]`, for the author to place.
A repeat arrival queues no steps, so a returning patient produces no
second encounter -- `admission-only-when-new` is this project's
single-encounter horizon (sim/ADR-0007 point 3) and lifting it is not
an arc-3a change. It is the same wall the two hooks meet: a delivery or
an injury may only land on a patient whose own queue is otherwise
empty, which is why ed-tuesday witnesses zero parent-delivery
encounters. Owner unassigned; candidates are arc 3b (`R-mix-5`: a
scheduled return is a second encounter) or its own arc.

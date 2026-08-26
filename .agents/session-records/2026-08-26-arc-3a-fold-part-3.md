# Session record -- arc 3a, part 3: the fold, landed dark

**Date:** 2026-08-26
**Prompt:** [`.agents/prompts/2026-08-26-arc-3a-fold-part-3.md`](../prompts/2026-08-26-arc-3a-fold-part-3.md)
**Base:** `a5d2239` -- **Tip:** `4764512`
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony,
no tag, CI green at the tip as the close marker.

## What landed

Everything ADR-0173 section 2 specifies except the two clinical hooks
and the identification flow, which are part 4. `:persons` is ABSENT
from every existing config, scenario and oracle root, so the proof is
still the DARK one -- ruling D1's commit 1.

| commit | what |
|---|---|
| `274cfef` | ride-along: ADR-0172 row 10's reverse half, position-matched |
| `473f1d8` | RED -- every gate the fold owes, before the fold |
| `80dcbcc` | GREEN -- the fold |
| `81e8fd1` | the `:ehrt` alias needs person-simulator too (`make traces` found it) |
| `ba9126d` | three gates the full suite found, in three OTHER bricks |
| this record | the close |

## The proof

| gate | result |
|---|---|
| `bin/regression-oracle a5d2239 HEAD` | **IDENTICAL**, exit 0, no declaration, **35 roots** per manifest |
| `git diff --stat a5d2239 HEAD` on `arc0_gated_*` (4), `pinned_seed_42_patients_5.edn`, both conformance baselines, `demos/traces` | **empty** |
| `make traces` | regenerated, `git status --porcelain` showed only `deps.edn` -- not one trace byte moved |
| `clojure -M:poly check` | **OK** |
| `make test` | **MAKE_EXIT=0**, 4,232 tests / 19,694 assertions, 0 failures, 0 errors |
| `make integration` | **INT_EXIT=0**, 0 `FAIL:` lines, 1,575 tests / 5,500 assertions |
| CI at the pushed tip `4764512` | **run 32982334630, conclusion success** -- the close marker (`rulings.md#R-session-verifies-ci-via-gh`, kept as the marker after the tag was retired). One run at this sha, the `test` workflow; no separate Integration run was scheduled for it |

The oracle line, verbatim:

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between a5d2239 and HEAD
```

IDENTICAL here does NOT mean the fold does nothing. It means `:persons`
ABSENT is byte-identical, which is ruling D1's whole point: commit 1
lands the machinery dark, so commit 2's re-pin -- a later session's --
has exactly one possible cause.

## Witness counts, both `:persons` runs

Neither of these is an oracle root, and neither is pinned. Every gate
asserts `pos?` rather than a count, because a pinned count turns a
future hazard retune into a false red while `pos?` catches the failure
that matters (`rulings.md#R-empty-population-is-red`).

**The REAL stream** (`run-command`, seed 42, 26 arrivals over a
14-person pool walking 30 years, `:unhoused {:t0-fraction 0.60}`):

| | count | |
|---|---|---|
| `:registered` | 8 | 26 arrivals, 8 patients -- the other 18 are repeats that RESOLVED |
| `:admission` / `:discharge` | 8 / 8 | |
| `:demographic-update` | **15** | all `:cause :residence-move` at this seed |
| `:coverage-change` | **13** | `:eligibility` 5, `:employment` 4, `:age-65` 2, `:loss` 2 |
| `:registered` carrying `:residence` | **5** | ruling E1's t0-unhoused registrations |

**The FLEET fixture** (fifth run, hand-authored stream, seed 15, 3
arrivals over a 2-person pool):

| | count |
|---|---|
| `:registered` | 2 (3 arrivals; one repeat resolved) |
| `:demographic-update` | 3 -- one per cause: `:residence-move`, `:residence-loss`, `:identity-correction` |
| `:coverage-change` | 1 |

The fleet's stream is HAND-AUTHORED and the reason is the one that put
the churn family in that file as explicit IR: reaching all three
`:demographic-update` causes plus a `:coverage-change` out of a real
person walk took a fourteen-person, thirty-year population in this
session's own probing, which is per-push-hostile and seed-fragile. The
engine's own contract is that person events arrive as DATA, so a
hand-authored vector exercises exactly the code a drawn one does. That
the REAL stream composes with this engine is gated separately, against
the real component, in `ehrt.sim.persons-run-test`.

## The 1.3.0 delta

Two kinds and one optional key:

* `:demographic-update` -- `:cause` (`:residence-move` / `:residence-loss`
  / `:identity-correction`), `:field` (`:residence` / `:name` / `:dob`),
  `:value`, optional `:prior-value`, `:person-event-id`, `:active-mrn`.
* `:coverage-change` -- `:cause` (`:employment` / `:age-65` / `:loss` /
  `:eligibility`), `:payer`, optional `:prior-payer`, `:person-event-id`,
  `:active-mrn`.
* `:registered` gains `{:optional true} :residence`, the three-armed sum
  (`:housed` / `:unhoused` / `:unknown`).

**THE BUMP WAS NOT OWED.** Section 2(f) says `classify-change` decides,
and it does -- run against the FROZEN 1.2.0 baseline before re-freezing:

```
:version 1.3.0
:classify {:additive? true, :breaking []}
```

A new kind and a new optional key are the two shapes its own docstring
calls non-breaking, so the policy's rule is PATCH/none. The bump is
taken anyway and deliberately: `:event-schema-version` is a consumer's
only handle on what a log they hold can CONTAIN, and a 1.3.0 log may
carry two kinds a 1.2.0-era consumer has never seen and will dispatch
on `:event` for.

**A 1.2.0-ERA LOG VALIDATES UNCHANGED AGAINST 1.3.0**, and here is how:
the two kinds are new BRANCHES of the `:event` multi, so no existing
branch's key set, optionality or value schema moves at all; and
`:residence` is `{:optional true}`, so a `:registered` event that omits
it validates exactly as before. The direction that breaks is the other
one -- a 1.2.0 schema meeting a 1.3.0 log fails to dispatch on two
kinds -- which is what a version is FOR.

`docs/formats.md` is the only generated surface that moved, plus the
two schema resources and the examples resource that feed it.

## ADR-0172 limitations row 6, STRUCK

Its substance -- *a delta folded onto patient state is invisible to
every message* -- is now FALSE BY DESIGN, so the row is gone from both
tables, `expected-row-count` is 12, and
`personas-are-keyed-by-patient-id-alone-test` is DELETED rather than
repaired. A gate over a limitation that no longer exists can only
assert something untrue or something vacuous.

Its successor is a POSITIVE law rather than a negative one:
`demographics-at-answers-state-at-t-test` (sim-emit-hl7) builds a log
with a delta between two messages and asserts the second renders the
new PID-11 and the first the old one. Two siblings landed with it:
`an-unhoused-patient-renders-pid-11-absent-test` (ruling E1, with the
whole-message equality assertion that makes the absence the ONLY
difference) and
`a-later-message-still-renders-a-persons-corrected-name-and-payer-test`.

**The surviving rows keep their numbers**: 1-5 and 7-13, with no 6.
Renumbering would have silently re-pointed every citation of "row N" in
three documents and eleven tests at a different limitation. ADR-0172
section 4 carries the strike where the row stood, with both dated
corrections to how it was predicted.

## ADR premises the tree contradicted

Six, all disclosed in ADR-0173's own Consequences as dated deviations,
and every one with a single defensible reading
(`rulings.md#R-stop-only-on-two-defensible-readings`).

1. **"The second encounter's steps simply continue that patient's
   log"** (section 2(a)) -- the tree refuses. A second `:admission` for
   a patient whose status is `:discharged` violates
   `admission-only-when-new`, which is this project's single-encounter
   horizon (sim/ADR-0007 point 3) expressed as an invariant. A repeat
   arrival therefore queues NO steps. Everything a repeat arrival is
   FOR survives: the person resolves to the patient they already are,
   and every later demographic event of theirs lands on that one
   patient. Its `:patient`-stream draws are still taken.
2. **`:persons` is a TWO-LAYER key**, and section 2(a)'s code block is
   the CONFIG side, not `engine/run`'s. Forced rather than chosen: the
   engine may not require the component that draws the stream, so
   somebody has to translate, and `run` cannot be that somebody. Same
   shape `:modules` already has.
3. **Ruling C1's ordering needed a third pass.** As written, aliveness
   depends on deaths, deaths depend on the person-to-arrival binding,
   and the binding depends on aliveness. `ehrt.sim.run` calls `persons`
   TWICE: pass one with no compiled deaths, whose `:person-death`
   events become the `:alive` map; `engine/person-plan` answers the
   binding from that fixed data; pass two runs with the real
   `:deaths`. The cycle is broken at the ALIVE FILTER, and the
   conservatism is stated in `engine-persons`' own docstring.
4. **The bump was not OWED** (above).
5. **The two kinds reach GROUND TRUTH, not a new HL7 message type.**
   ADR-0173 specifies the emitter's work as the re-key and nothing
   more, and an A08 is a registry entry, a control-id derivation, a
   derivability-property row and a `witnessed-message-types` claim.
   Named as a later arc's candidate rather than left a silence, in
   `message-type-registry`'s own comment and in each kind's `:doc`
   (which is what `docs/formats.md` renders).
6. **Row 4's gate asserted the wrong thing**, and `:coverage-change` --
   a name section 2(b)'s own fold table puts on BOTH sides
   deliberately -- is what found it. Rewritten to row 4's structural
   claim, with the shared-name set pinned at exactly
   `#{:coverage-change}`.

Two PROMPT premises also did not hold, and both are the same species:

* **`event_schema.clj:188` is not the kind enum.** Line 188 is
  `PreHorizonFact`'s own six-value `:event` enum. The closed vocabulary
  is the `[:multi {:dispatch :event}]` at `:263`, which is where the
  two kinds landed. ADR-0173 section 2(b) cites `:263` correctly; the
  prompt's `:188` is a stale line reference.
* **`:163-178`, the ADR's `:persons` shape**, is the CONFIG-facing map
  (deviation 2). Both `run-command` and `engine/run` accept a
  `:persons` key and reject a malformed one, so the prompt's step 1 is
  satisfied under either reading, and both are gated.

## Red before green

Every new gate was run RED against the src stashed back to `274cfef`,
and the red run's own output is in `473f1d8`'s message. Summarised:

| namespace | red because |
|---|---|
| `ehrt.sim-engine.persons-test` | `Syntax error compiling at (persons_test.clj:116:14)` -- no `person-plan` |
| `ehrt.sim.persons-run-test` | `Syntax error compiling at (persons_run_test.clj:87:13)` -- no `valid-persons?` |
| `ehrt.sim-check.person-invariants-test` | `Unable to resolve var: check/identity-fill-references-its-placeholder-registration` |
| `ehrt.sim-emit-hl7.emit-hl7-test` | 66 tests, **5 failures** -- the three fold gates |
| `ehrt.sim-engine.event-schema-test` | 19 tests, **1 failure** -- 21 declared, not 23 |
| `ehrt.docs-tooling.person-simulator-charter-test` | 14 tests, **7 failures** -- row 6 still tabled |
| `ehrt.docs-tooling.event-log-doc-test` | 8 tests, **1 failure** |
| `ehrt.docs-tooling.oracle-coverage-test` | 6 tests, **1 failure** |
| `ehrt.person-simulator.limitations-test` | 11 tests, **1 failure** |

The RIDE-ALONG's own red could not be run the ordinary way, and that is
itself the row it gates: a real `:require` from sim-engine to
person-simulator is a `Cyclic load dependency` and will not compile, so
the scan body was extracted and run standalone over a mutated tree, in
BOTH positions the rewritten gate matches:

```
require position:
  back-edges: [.../order_profiles.clj: (:require ehrt.person-simulator.interface)]
call position:
  back-edges: [.../order_profiles.clj: (ehrt.person-simulator.interface/...)]
```

## The three gates the full suite found, in three OTHER bricks

`rulings.md#R-full-suite-before-push`, paid for a third time this arc.
Every namespace this session wrote was green; all three of these live in
bricks it had not touched. Detail is in `ba9126d`'s own message.

1. `person-death-emits-no-ground-truth-event-test` (person-simulator) --
   name-disjointness, red on a name the ADR chose. Rewritten to the
   structural claim.
2. `arc0-check-all-findings-are-identical-on-every-gated-corpus` (sim) --
   RE-PINNED 29 -> 35 and disclosed, because its own message says a red
   there is a STOP and not a re-pin. **Which invariant moved: none.**
   `:status` is `:ok` on both sides and `:events` is unchanged on all
   four corpora; what grew is the ROSTER, by exactly the six section
   2(e) adds. That is the case the def's own docstring anticipates.
3. `the-kinds-this-emitter-deliberately-does-not-render-are-still-
   contract-kinds` (sim-emit-hl7) -- recorded in all three places its
   own failure message demands.

And a FOURTH, found by `make traces` rather than by `make test`: the
`:ehrt` alias in root `deps.edn` enumerates its own `:local/root` set
by hand, so `bin/ehrt` could not load `ehrt.sim.run` any more. `poly
check` was already OK and the whole suite loads through `:dev`/`:test`.
The CLI-surface rule is what put a gate in front of it at all.

## The suite delta

**The suite delta reconciles exactly, and BOTH sides were measured this
session** rather than one being inherited: `make test` was run at
`a5d2239` in a disposable worktree as well as at the tip.

|  | tests | assertions |
|---|---|---|
| baseline `a5d2239` (measured) | 4,132 | 18,951 |
| tip (measured) | 4,232 | 19,694 |
| delta | **+100** | **+743** |

**+2 of those assertions are an artefact of measuring the baseline in a
git WORKTREE, not a real change.**
`generator-sha256-is-not-the-all-zero-placeholder-when-git-is-present`
guards its own `is` with `(when (version/git-sha) ...)`, and a
worktree's `.git` is a file rather than a directory, so the assertion
did not run on the baseline side. Correct for it and the baseline is
**18,953** -- which is exactly the figure part 2's record carries, an
independent confirmation that the two sessions are counting the same
way. The real delta is **+100 tests / +741 assertions**.

Every namespace that moved, measured on both sides:

| namespace | tests | assertions | why |
|---|---|---|---|
| `ehrt.sim-engine.persons-test` | +28 | +152 | NEW: 14 deftests x 2 projects |
| `ehrt.sim.persons-run-test` | +14 | +272 | NEW: 7 x 2 |
| `ehrt.sim-check.person-invariants-test` | +20 | +102 | NEW: 10 x 2 |
| `ehrt.sim-emit-hl7.emit-hl7-test` | +6 | +24 | 3 new deftests x 2 |
| `ehrt.person-simulator.*` (four namespaces) | +32 | +147 | the brick now runs in TWO projects, not one |
| `ehrt.sim-engine.event-schema-test` | 0 | +26 | the fleet's FIFTH run: one validity assertion per event it produces |
| `ehrt.docs-tooling.event-log-doc-test` | 0 | +8 | two more kinds in the generated block |
| `ehrt.docs-tooling.test-source-live-path-lint-test` | 0 | +6 | tree-scanning: four new files |
| `ehrt.docs-tooling.io-vocabulary-lint-test` | 0 | +2 | tree-scanning, same |
| `ehrt.sim.run-test` | 0 | +2 | `run-command-forwards-every-engine-config-key` iterates `config-keys`, now 16 |
| `ehrt.sim.version-test` | 0 | (+2) | the worktree artefact above -- NOT a real change |
| | **+100** | **+741** | |

THE PERSON-SIMULATOR DOUBLING IS THE ONE ROW WORTH READING TWICE. That
brick was composed in exactly ONE project before this session
(`conformance`, and only so its own tests would EXECUTE -- arc 2b's own
finding, review 4's W-1 in a second shape). It is now composed in all
three, because `ehrt.sim.run` requires it for real. `poly info`'s own
matrix says so: `stx / --- / ---` at `a5d2239`, `stx / stx / stx` at the
tip. So a copy of its 33 tests joined `make test`, and
`limitations-test` lost row 6's own deftest inside that.

The twenty-eight new deftests, named:

| test | where |
|---|---|
| `persons-is-a-config-key-run-command-must-forward-test` | sim-engine/persons-test |
| `run-accepts-a-persons-payload-and-rejects-a-malformed-one-test` | " |
| `each-arrival-binds-a-living-person-by-one-world-draw-test` | " |
| `a-dead-person-is-not-in-the-arrival-candidate-set-test` | " |
| `the-selection-draw-is-taken-whether-or-not-anyone-is-eligible-test` | " |
| `a-second-arrival-of-the-same-person-resolves-to-the-same-patient-test` | " |
| `the-fold-is-queue-seeded-in-t-order-among-the-engines-own-events-test` | " |
| `demographics-fold-onto-patient-state-and-leave-the-persona-alone-test` | " |
| `a-person-death-mints-nothing-test` | " |
| `a-registration-bound-to-an-unhoused-person-carries-the-residence-sum-test` | " |
| `an-event-that-reports-no-change-is-not-an-event-test` | " |
| `the-prior-value-is-the-folded-state-not-the-person-events-own-claim-test` | " |
| `evolve-writes-one-demographics-field-and-is-total-test` | " |
| `person-plan-keys-the-compiled-death-by-person-test` | " |
| `run-command-builds-the-person-stream-only-when-persons-is-present-test` | sim/persons-run-test |
| `run-command-hands-the-engine-compiled-deaths-keyed-by-person-test` | " |
| `run-command-rejects-a-malformed-persons-config-test` | " |
| `the-manifest-stamps-persons-and-persona-config-iff-present-test` | " |
| `a-real-person-stream-reaches-the-log-as-the-two-kinds-test` | " |
| `a-persons-run-satisfies-the-whole-invariant-catalog-test` | " |
| `a-persons-run-is-deterministic-test` | " |
| `the-clean-run-actually-folded-something-test` | sim-check/person-invariants-test |
| `the-whole-person-family-is-clean-on-a-real-fold-test` | " |
| `the-six-are-registered-in-the-catalog-test` | " |
| `identity-fill-references-its-placeholder-registration-test` | " |
| `identification-merge-survivor-is-the-persons-prior-patient-test` | " |
| `every-placeholder-registration-is-resolved-or-still-open-test` | " |
| `demographic-update-reports-a-real-change-test` | " |
| `no-demographic-event-after-a-patient-expires-test` | " |
| `person-scoped-provenance-is-a-stamp-not-a-reference-test` | " |
| `check-all-rejects-a-run-that-breaks-one-of-the-six-test` | " |
| `demographics-at-answers-state-at-t-test` | sim-emit-hl7/emit-hl7-test |
| `an-unhoused-patient-renders-pid-11-absent-test` | " |
| `a-later-message-still-renders-a-persons-corrected-name-and-payer-test` | " |

Thirty-four, not twenty-eight: 14 + 7 + 10 + 3 = **34 new deftests**,
minus the ONE deleted (`personas-are-keyed-by-patient-id-alone-test`,
row 6's), which is why the per-project figure is 33 and the whole-suite
figure is 66 -- the remaining +34 is the person-simulator doubling.

**`make integration`: INT_EXIT=0, 0 `FAIL:` lines, 1,575 tests / 5,500
assertions**, against part 2's record's 1,508 / 5,066: **+67 / +434**.
Same two causes: the `integration` project also composes
person-simulator for real now (+33 tests / +145 assertions, one copy),
plus this session's own 34 new deftests minus none (they all run there
too: 31 tests / 263 assertions across the three new namespaces and
emit-hl7's three), plus +13 in `event-schema-test` from the fleet's
fifth run and +1 in a tree-scanning lint. Run because review 4's W-1
stands: `make test` skips this tier, so a gate can land unexecuted.

## What part 4 inherits

* `:persons`, both layers, with `engine/person-plan` answering the
  binding and the compiled deaths, and `run/engine-persons` doing the
  two-pass translation.
* `:person-index`, written for real, one entry per bound person. Part 4
  adds `:placeholders` to each entry.
* `person-fold/demographic-effect` and `wire-step`: two functions, one
  `case` each, where a new person-event kind's wire face goes.
* Three of the six invariants already written and mutation-proved for
  the identification flow, plus the fields they read and part 4 must
  mint: `:identity :placeholder` and `:window-close-t` on `:registered`,
  `:person-id` on `:registered`, `:cause :identity-fill` and
  `:placeholder-event-id` on `:demographic-update`, `:cause
  :identification` on `:merge`. None is in the 1.3.0 schema, on
  purpose: a schema that describes a future is the ADR-0170 species.
* Ruling D1's COMMIT 2 -- turning `:persons` on in a gated corpus and
  re-pinning in ONE declared sweep -- is still unstarted, and is still
  the only sweep whose diff has exactly one possible cause.

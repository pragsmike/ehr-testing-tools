# Session record -- arc 2b: the person-simulator, the component

**Date:** 2026-08-25
**Prompt:** [`.agents/prompts/2026-08-25-arc-2b-person-simulator-component.md`](../prompts/2026-08-25-arc-2b-person-simulator-component.md)
**Base:** `9d64ae2` -- **Tip:** this commit's own parent chain from `3a6aed0`
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony
(commit and push at each checkpoint), no tag, CI green at the tip as the
close marker.

## What landed

Four commits, in the order the prompt names them.

| commit | what |
|---|---|
| `3a6aed0` | ADR-0172 **Accepted**, the seven rulings quoted where they land |
| `754315f` | the component skeleton and its charter gate, no behaviour |
| `04b86bf` | RED -- eleven limitation gates, the referential invariants, fixed consumption, a counted witness |
| `98099dc` | GREEN -- the fourteen kinds, drawn from the `:person` family alone |
| `e1b478d` | this record, the prompt archive, and the roadmap row |
| `e1462c8` | fix -- one household per parent, and no hash-order pick |
| `b4f1115` | fix -- an event that reports no change is not an event |

The author ruled **A1 B1 C1 D1 E1 F1 G1** -- the recommendation on all
seven. F1 is the one that shaped the session: the component lands
ALONE, the engine does not call it, and the corpus proof is therefore
available. Green here is evidence, not an absence of red.

## The fourteen kinds, and their counted witness

One config+seed -- 60 persons, 24 years, master seed 42,
`:merge-fraction` 0.35 -- under which every kind ADR-0172 section 2
charters occurs. Counts pinned in
`ehrt.person-simulator.witness-test`, and each also asserted `pos?`,
because a pinned total alone would not say which kind a reshuffle
emptied (ADR-0171's own lesson).

| kind | count | | kind | count |
|---|---|---|---|---|
| `:residence-move` | 163 | | `:person-death` | 17 |
| `:coverage-change` | 147 | | `:household-leave` | 16 |
| `:employment-change` | 135 | | `:identity-resolution` | 5 |
| `:household-join` | 52 | | `:identity-unavailable` | 5 |
| `:household-form` | 38 | | `:occupational-injury` | 5 |
| `:identity-correction` | 36 | | `:delivery` | 26 |
| `:pregnancy` | 26 | | `:person-registered` | 26 |

697 events total. `:pregnancy` = `:delivery` = `:person-registered` = 26
is not a coincidence and not luck: it is limitations row 11's bijection
and row 2's closure, both of which had to be built for rather than
observed (see "what the tree contradicted", 5).

## Draws, per process

| process | draws | note |
|---|---|---|
| a person-YEAR | **18**, always | fired or not, branch taken or not; drawn all at once ahead of any emission |
| a t0 adult's Persona | **13** | `sim-model/persona` unchanged, through the interface -- 16 with the config-gated demographic weights |
| a newborn's Persona | **4** | ruling A1: sex, given name, SSN group, SSN serial. Surname, address and phone from the household; `:dob` from the delivery; `:payer` from the parent's coverage |
| a person walked N years | **13 + 18N** (t0) / **4 + 18N** (newborn) | a function of N alone -- gated |

Within-year instants are derived from the firing variate itself, never
drawn: a separately-drawn instant would be a variate whose COUNT
depends on whether the hazard fired.

## The corpus proof (ruling F1)

| gate | result |
|---|---|
| `make test` (unpiped, `MAKE_EXIT` captured) | **MAKE_EXIT=0**, 0 failures, 0 errors, 15m08s |
| `make integration` (Makefile:52 -- the tier `make test` skips) | **INT_EXIT=0**, 0 `FAIL:` lines, 1,502 tests / 4,813 assertions, 10m08s |
| `bin/regression-oracle 9d64ae2 HEAD`, **no declaration** | **IDENTICAL**, exit 0, 35 roots per side, 70 digest lines |
| four `arc0_gated_*` digests, `pinned_seed_42`, both conformance baselines | `git diff --stat 9d64ae2 HEAD` on those paths: **empty** |

The oracle is the one that matters, and it is worth saying exactly what
it proves. `bin/regression-oracle` stands up two disposable worktrees,
runs `ehrt.oracle.digest`'s fixed-seed golden run for every vendored
root on each side's OWN component code, and diffs the two SHA-256
manifests. Its soundness check ran first and passed without a
declaration (`IDENTICAL outside the leading docstring -- proceeding`),
then all thirty-five roots matched byte for byte:

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between 9d64ae2 and HEAD
```

That is ruling F1 discharged. The `:person` stream family has zero draw
sites in the engine, this component draws from nothing else, and
nothing calls it -- so a green suite here is EVIDENCE that the corpus
did not move, not merely an absence of red. It is ADR-0169's own
equivalence-proof shape applied to a whole new component.

**Suite reconciliation against `9d64ae2`**, measured by running the same
target in a worktree at that commit, not estimated:

| | tests | assertions | exit |
|---|---|---|---|
| `9d64ae2` (worktree) | 4,056 | 18,170 | 0 |
| tip | 4,111 | 18,400 | 0 |
| delta | **+55** | **+230** | 0 |

**+55 reconciles exactly**, and every one is a new test:

| namespace | runs | tests | assertions |
|---|---|---|---|
| `ehrt.docs-tooling.person-simulator-charter-test` | 2 (conformance, ehrt-cli) | 28 | 94 |
| `ehrt.person-simulator.limitations-test` | 1 (conformance) | 10 | 36 |
| `ehrt.person-simulator.invariants-test` | 1 | 9 | 21 |
| `ehrt.person-simulator.consumption-test` | 1 | 7 | 29 |
| `ehrt.person-simulator.witness-test` | 1 | 1 | 18 |
| | | **55** | **198** |

The charter gate runs TWICE because two projects compose `docs-tooling`;
the component's own four run ONCE, because only `conformance` composes
`person-simulator` -- which is the whole reason that entry exists (see
"what the tree contradicted", 1).

**+230 - 198 = +32 residue**, all of it accounted for, none of it a new
test:

| namespace | delta | why |
|---|---|---|
| `docs-tooling.io-vocabulary-lint-test` | +10 | one assertion per brick src file, x2 runs -- 5 new src files |
| `docs-tooling.test-source-live-path-lint-test` | +8 | one per brick TEST file, x2 -- 4 new `*_test.clj` |
| `docs-tooling.structure-currency-test` | +6 | per-brick, x2 -- the new brick in both structure surfaces |
| `docs-tooling.invocation-lint-test` | +4 | per src file, x2 |
| `docs-tooling.stale-path-test` | +2 | per scanned path, x2 |
| `sim.version-test` | +2 | **a measurement artefact, not a change** |

That last one is worth its own line. `ehrt.sim.version/git-sha` reads
`.git/HEAD` as a FILE, and in a `git worktree` `.git` is a file
containing `gitdir:`, not a directory -- so the baseline worktree got
`nil` and
`generator-sha256-is-not-the-all-zero-placeholder-when-git-is-present`
skipped its one assertion, twice. The tip, a real clone, ran it. Nothing
in this arc touched `sim`; the difference is between a worktree and a
clone. Recorded rather than absorbed into the residue, because a
baseline measured in a worktree is the method this repo uses and this
is a standing property of it.

## What the tree contradicted

Eight premises, one line each. None is a defect in the charter; each is
something only execution could find.

1. **`poly check` was never the binding constraint on
   `projects/conformance/deps.edn`.** The prompt predicted it would not
   demand the entry, and it did not -- but the brick's own tests DID
   NOT RUN. A deliberately failing probe test left `clojure -M:poly
   test project:development` at exit 0, because `poly test` runs a
   brick's tests in the projects that COMPOSE it and the development
   project is not one of those. Review 4's W-1 in a second shape.
2. **`initial-persona`'s `t0` cannot be a bare instant.** A two-argument
   function that must DRAW has no seed in `person-id` and none in a
   time. Ruling C1's own words settle it -- the compiled death instant
   arrives "as a t0 parameter" -- so `t0` is the map of parameters
   available at t0. The charted arities are unchanged.
3. **`sim-model`'s payer pools are not on its interface.**
   `under-65-payers` / `sixty-five-plus-payers` are private to
   `ehrt.sim-model.persona`. So a `:coverage-change` payer comes from
   CONFIG, and a run supplying no pool gets no coverage-change events
   (the variates are drawn anyway). Copying the pools here would be the
   forked-table drift row 7 forbids for addresses.
4. **The vendored tables ARE reachable -- as resources, not interface.**
   `places.edn` and `given-names.edn` are read off the classpath, which
   is what the prompt directs for places and what keeps this component
   from forking `sim-model`'s content.
5. **ADR-0172's three mortality anchors are not collinear in log
   space.** No two-parameter Gompertz passes through 0.0009 at 30, 0.02
   at 70 and 0.15 at 90. The fit takes the outer two and lands 0.027 at
   70. Said out loud in the `PROVISIONAL` marker rather than smoothed
   away.
6. **Row 11's bijection is not a property of the rates.** A pregnancy
   whose delivery would fall outside the horizon, or after the mother's
   death, leaves a `:pregnancy` with no `:delivery`. Making the row's
   own gate hold required computing each person's death instant BEFORE
   emission, from variates already drawn -- which is also what made
   ruling C1's truncation exact.
7. **The prompt's two gate names are not the rows' names.** The rows
   say `pregnancy-and-delivery-are-in-bijection-test` and
   `every-provisional-rate-is-tabled-test`; the prompt paraphrased them
   as `-are-one-to-one` and `-is-marked`. "Named as the row names them"
   is the instruction, and the row is what the mirror gate compares, so
   the rows win.
8. **A new brick is not wiring alone.** `AGENTS.md` and
   `docs/dev/architecture.md` both went red under
   `ehrt.docs-tooling.structure-currency-test`, and `.agents/state-derived.md`
   under its own freshness gate. None was on the prompt's wiring list.

## Bugs found, and by what

Recorded because they are the reason the gates were written, and both
were caught by a gate rather than by reading.

1. **A newborn aged twelve years in one.** `clock/age-at-year` read a
   Persona's `:age` as its age at the RUN's year zero. A newborn's
   derived Persona carries `:age` 0 as of its own BIRTH year, so a baby
   born in year 11 came out twelve the following year, drew adult
   hazards and joined households as a minor.
   `minors-join-households-only-by-birth-or-formation-test` went red on
   five joins. `age-at-year` now takes the origin year as a parameter.
2. **A stream collision inside one person.** A newborn's walk rebuilt
   its own `Random` from the same id-tag, replaying as its first year
   the four variates its Persona had already consumed. The consumption
   gate caught it: the newborn's draw count came out a multiple of
   eighteen instead of four more than one. The walk now continues on
   the already-positioned stream.

Two more were found NOT by a gate but by asking the witness stream a
question no gate was asking -- *which persons head more than one
household* -- which is worth recording as a method: a green suite is
evidence about the questions you asked it.

3. **A parent's second and third children each constituted their own
   household.** The births pass reads the parent's household state as
   snapshotted by the WALK, which ran before the pass and could not see
   what the pass had just created, so `p-034` headed
   `hh-p-034-birth0`, `-birth1` and `-birth2`: three households, one
   per child, siblings in none of each other's. Eight parents affected.
   The pass now threads a `constituted` map.
4. **A family move propagated to whichever household the hash offered.**
   The head -> household lookup was a reverse `some` scan over a map,
   returning ONE entry in iteration order -- a derivation through
   nondeterminism (`rulings.md#R-no-derivation-through-nondeterminism`)
   that silently dropped propagations. It is now an explicit head ->
   SORTED vector index, and propagation reaches every household the
   mover heads that was already formed.

And three more of a different kind, found by reading the payloads: a
`:residence-move` to the address already lived at, an
`:employment-change` to the status already held, and an
`:identity-correction` whose corrected `:dob` equalled the original.
Each fired a hazard and then said nothing had happened. All three are
now suppressed when the value does not change, and the dob correction
got a real error to model -- `transposed-dob` swaps month and day, the
transcription error a registrar makes on an international form, and
only when the day is 12 or less so the result is always a REAL date.
Digit transposition was tried first and rejected: it turns 1985-04-27
into 1985-04-72, and an impossible date presented as ground truth is
the fabricated-by-omission class this project may not ship.

**Consumption is untouched by all five suppressions**, which is why
they are suppressions and not resamplings: every variate is drawn
either way, and the draw-count gate stayed green at 13 + 18x24 per t0
person throughout.

## Fences

No file under `components/sim-engine`, `sim-model`, `sim-check`,
`sim-emit-hl7`, `sim-emit-fhir`, `patient-simulator` or `sim` was
changed -- `git diff --name-only 9d64ae2 HEAD` carries none of them.
Every draw comes from the `:person` family, gated by
`every-draw-comes-from-the-person-family-test`. No rulings row was
added (`rulings.md` is FROZEN). No tag was paid. No hazard rate is
presented as sourced.

The consumption gate redefines `ehrt.sim-engine.interface/stream` with
`with-redefs` -- the same mechanism the engine's own stream-locality
test uses, and the reason `stream` is deliberately left unhinted. That
is a test-time rebinding, not a change to that component.

## Blemishes

* The RED commit and the GREEN commit were first written with a
  two-line subject (the message file's first paragraph wrapped). Both
  were unpushed, so both messages were rewritten
  (`rulings.md#R-amend-unpushed-message-only` for the first; the second
  was re-applied from its own `format-patch` output). No content
  changed; the RED/GREEN split is intact.
* **One v1 artefact survives, stated in `persons`' own docstring rather
  than left to be discovered.** A parent unhoused at their delivery who
  later forms a household on their own hazard heads TWO -- the one the
  birth constituted and the one they formed. Four such persons in the
  witness population. Both have members and a move by the head reaches
  both; household structure has no wire surface at all (limitations row
  8). Fixing it needs a second walk pass or a feedback edge from the
  births pass into the walk, and neither buys a message.
* `:identity-unavailable` and `:identity-resolution` witness at 5 each
  and `:occupational-injury` at 5 -- thin. They are the knife-edge
  fixtures a future reshuffle would empty first, which is exactly why
  the counts are pinned and `pos?` is asserted separately.

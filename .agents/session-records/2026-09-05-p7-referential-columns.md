# 2026-09-05 -- P7 resumed: columns A, B2 and C shipped, and the corpus authoring was never needed

`roadmap.md#referential-corpus-population` (PRIORITY 7), resumed from
`.agents/session-records/2026-09-05-p7-stop-derivation.md`. The
fourteen cells that row carried as POPULATION GAPS are operators. The
event catalog is **26**, not 12. The row is CLOSED.

Base `753d320`. Ceremony: R30, commit and push at each checkpoint. No
sub-agents. Six commits:

| sha | commit |
|---|---|
| `cedac1d` | test: columns A, B2, C -- 14 conviction witnesses, RED |
| `74c6d87` | feat(corpus): referential columns A, B2 and C shipped (ADR-0176) |
| `adbf6e1` | docs: sim check is its own pipeline stage |
| `28e6ef4` | test(cli): the operator count pins move 22 -> 36 and 26 event ones |
| `b9c755e` | docs: population ledger closed |
| this one | docs: P7 session record (archives prompt) |

**The headline is what did NOT happen.** The row PRICED this work as
corpus authoring -- *"a scenario or module set that exercises the
cancel family and closes its medication and care-plan spans"* -- and
that price was never paid. `demos/scenarios/dense-7500/config.edn`,
committed 2026-09-04 for the Scale table and for no reason connected
to this row, already carried all three columns. The whole payload is
one helper, three data rows and an id stem.

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** One disclosure
and it is the correct state: HEAD is not tagged `stable-*`, and no tag
is paid. A second disclosure the tool made itself: the CI run at
`753d320` was still PENDING when preflight ran, which it reports as
"not awaited to conclusion, not counted as red" -- the four runs before
it green. Edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`; tree clean including untracked; local HEAD matched
`origin/main`.

## 1. Step 1 -- the measurement, and the STOP record's numbers held

`demos/scenarios/dense-7500/config.edn`, seed 5, `--patients 20`,
`:churn true`, per R-population. Run: **18,466 events in 53.6 s** --
the same event count the derivation measured before ADR-0178, which is
the expected outcome: that ADR removed three `:window-close-t nil`
KEYS and added no event.

**The parent is clean AND schema-valid**: `check-all` reports `#{}`,
and `every? valid-event?` is true over all 18,466. Both halves matter.
The first is what makes every finding-set equality below a statement
about the OPERATOR rather than about the corpus; the second is the
thing the derivation stopped for, and ADR-0178 fixed it.

Carrier census at 20 arrivals: `{:cancel-transfer 4, :medication-end
6, :care-plan-end 8}`. `:cancel-admit` and `:cancel-discharge` do not
occur at this size, exactly as the derivation recorded, so column A's
per-carrier `:target` map is EXERCISED on one of its three keys and
the other two are structure rather than witness. Recorded rather than
papered over.

### The 14 cells

| col | shape | sites @20 | sampled | observed = declared |
|---|---|---|---|---|
| A | `phantom` | 4 | 4 (all) | yes |
| A | `cross-patient` | 4 | 4 (all) | yes |
| A | `wrong-kind` | 4 | 4 (all) | yes |
| A | `inverted-span` | 4 | 4 (all) | **yes** -- see below |
| B2 | `phantom` | 6 | 6 (all) | yes |
| B2 | `null` | 6 | 6 (all) | yes |
| B2 | `cross-patient` | 6 | 6 (all) | yes |
| B2 | `wrong-kind` | 6 | 6 (all) | yes |
| B2 | `inverted-span` | 6 | 6 (all) | yes |
| C | `phantom` | 8 | 8 (all) | yes |
| C | `null` | 8 | 8 (all) | yes |
| C | `cross-patient` | 8 | 6 of 8 | yes |
| C | `wrong-kind` | 8 | 6 of 8 | yes |
| C | `inverted-span` | 8 | 6 of 8 | yes |

**14 of 14 convict exactly**, in both directions, with the mutated
event schema-valid at every site measured. Sites are the exhaustive
count, not a sample, because `resolving-sites` is a pure function of
the log.

**Three of the fourteen are 6 of 8 and the rest are exhaustive**, and
that split is deliberate. B2's and C's invariants both EXCUSE a nil
reference when the patient's own `:registered` carries a matching
`:pre-horizon-facts` citation, and a site reaching that branch would
ship a cell convicting NOTHING. Those four cells (`null` and `phantom`
on each column) were therefore measured over EVERY site -- 28 of 28
convict, 0 reach the branch. Column C's other three shapes do not read
that branch and are sampled, as the ledger's section 7 licenses.

### A/`inverted-span` -- the refused cell arriving

**Declared** `#{:cancel-references-existing-uncancelled-event,
:timestamps-monotone}`. **Observed** the same, at all four sites. The
derivation measured `#{:timestamps-monotone}` alone here on this same
population and REFUSED the cell under Q5(a) rather than ship a
duplicate `:clock-skew` wearing a reference field's name. ADR-0178
(R-time) gave `cancel-references-existing-uncancelled-event` its fifth
disjunct; the cell ships because the CHECKER gained a clause, not
because a corpus gained a site. It is the ledger's own third gap kind
-- a SHAPE gap -- opening and closing inside a week.

Probes were NOT promoted to `bin/` (ledger section 7's rule): that is
author-licensed fence widening, and nothing here needs to run per push.

## 2. Steps 2 and 3 -- RED, then GREEN

**RED** (`cedac1d`), the namespace alone, real output:

```
Ran 10 tests containing 545 assertions.
115 failures, 3 errors.
```

- 14 x 8 in `every-event-operator-declares-the-event-shape-test` -- and
  the 14 ids it names are exactly the 14 rows added, no more, no fewer.
- 2 in `every-schema-reference-field-is-covered-or-declared-empty-test`
  -- `:cancels-event-id` and `:start-event-id` no longer declared
  population gaps and not yet covered.
- 1 in the new `every-registered-event-operator-has-a-loop-row-test`.
- 3 ERRORS, disclosed rather than smoothed: the three `doseq` tests
  abort at the FIRST nil operator instead of reporting fourteen
  apiece, because an exception ends a `doseq`. So the red is 14 named
  cells plus three aborts, not 14 x 6.
- `every-population-checks-clean-test` GREEN on all three populations,
  which is the half that had to be true first.

**GREEN** (`74c6d87`):

```
Ran 10 tests containing 906 assertions.
0 failures, 0 errors.
```

`clojure -M:poly check` OK. `make docsgen` regenerated
`docs/operators.md` (36 operators, 26 event-level).

## 3. Three things the prompt's step-3 scope did not survive

Step 3 said "R-target helper; three entries in `referential-columns`;
nothing else". Two of the three below are mechanical with ONE
defensible reading, so `rulings.md#R-stop-only-on-two-defensible-readings`
does not bind and they are fix-forward WITH DISCLOSURE; the third is
doc currency.

**(a) `:target` per carrier -- R-target, ruled in advance.** Executed
as ruled: `:target` is a keyword OR a map from carrier event kind to
target kind, read through one `target-kind` helper. Column A's three
cancel kinds cancel three DIFFERENT event classes but must be ONE
column, because `referential-entry` derives one operator id per
(shape, column). A keyword column's per-site answer is its column-wide
one, so columns D and B1 are unchanged -- `:wrong-kind`'s site
predicate moved from a once-per-column existence check to a per-site
one and means the same thing on a keyword target.

**(b) THE ID COLLISION, which the prompt did not name and which has no
symptom.** `referential-entry` derives its id from `(shape, field)`,
and TWO columns share a field: `:order-event-id` rides both
`:result-available` (B1) and `:medication-end` (B2) -- addendum (b)'s
own point, one layer down. `register!` is a bare `swap! registry
assoc`. So B2's five entries would have **SILENTLY REPLACED** four of
B1's: no refusal, no catalog gap, no exception, nothing in any log.
The catalog would simply have been four operators smaller than it
said. A column may now declare an id `:slug`; B1 keeps the bare stem
because its four ids are already published in `docs/operators.md` and
are what `--operator-id` takes, and B2 takes
`medication-end-order-event-id`. **The law landed as a gate, not as a
comment** (AGENTS.md): `every-registered-event-operator-has-a-loop-row-test`
is a BIJECTION between the loop rows and the registered event
operators, so it moves no pin when an operator is added and turns red
on a collision in either direction.

**(c) Two consumer-facing claims went false the moment the catalog
grew.** `docs/consuming-ground-truth.md` said "The event-level catalog
is twelve operators deep" and carried a "**What is *not* there yet**"
paragraph naming these three columns as uncovered. Both corrected in
the GREEN commit rather than in the docs commit: a tree whose prose
contradicts its own catalog is not a state worth committing to.

## 4. Step 4 -- the rider, and the diagram it silently broke

`pipeline.edn` gains `:sim-check` ("Sim check", `:kind :judge`,
`:built`) and the oracle use case's second equation repoints from
`[Check]` to `[Sim check]`. `lint.clj` untouched, per R-rider.

**The stage's POSITION in `:stages` is load-bearing, and that was
measured, not reasoned.** `resource_equations_to_mermaid.py` resolves
a resource to its LAST producer, and three stages now output
`pass`/`rejected`. Appended after `:check`, the regenerated diagram
showed `Sim_check -- pass --> Report` where `Check -- pass --> Report`
had been -- Report's two input arrows silently re-drawn to a stage
whose verdict never reaches it. Placed BEFORE `:check` the diff is
purely additive: one equation line, one box, two resource nodes, and
Arrow 6 byte-identical. The EDN carries that as its own comment,
because nothing else in the tree would tell the next session.

Two shapes recorded in the same comment rather than left to be
discovered: `event-log` is produced by no stage in this file (the
corpus pipeline does not re-model the simulator, which
`components/sim/docs/sim-theory.edn` already does in full), and
`invariant-catalog` is declared an INPUT rather than `:catalytic`
though it is plainly used-not-consumed -- stated exactly as the use
case's equation has always stated it, because promoting it would need
a classification row in `catalytic-resource-targets` and R-rider ruled
`lint.clj` untouched. An omission with a date on it.

`make lint-pipeline` OK. usecases + pipeline + lint + docsgen-closure
+ stale-path: 97 tests, 591 assertions, 0 failures, 0 errors.

## 5. The cost, measured -- and the one-line fix this session may not make

**This gate is expensive, and the reason is not the gate.** The
population's run is ~54 s, flat in `--patients`; each `check-all` over
its 18,466 events is **~45 s**, of which **~42 s is
`every-event-is-schema-valid` alone** (ADR-0178, landed 2026-09-04).
The acceptance suite makes 15 such calls, so the namespace runs in
~14 minutes where it used to run in seconds.

The mechanism, measured on `event-examples.edn` this session:

| call | ms per event |
|---|---|
| `engine/valid-event?`, i.e. `(m/validate Event e)` | **2.288** |
| a prebuilt `(m/validator engine/Event)` | **0.0063** |

**365x.** Malli rebuilds the validator on every `m/validate` call, and
`Event` is a large schema. The fix is one line beside the schema --
bind `(m/validator Event)` once and call it -- and it is
`components/sim-engine` src, which **this session's fence forbids
touching**. So: recorded, not adapted around. The acceptance suite's
own step-3 assertion DOES use a prebuilt validator (`schema-valid?`,
same schema object, same predicate, documented at its definition),
which is inside the fence and removes half the cost; the other half is
inside `check-all` and unreachable from `components/corpus`.

**This is also a consumer-facing cost, not only a CI one.** `ehrt sim
check` over a dense-7500 run at its documented 7,500 arrivals (167,190
events) pays ~6 minutes to schema-validation before any invariant that
says something about the hospital. AUTHOR ACTION: the fix is a
one-line change in `event_schema.clj:1057` and needs a session that
may touch `sim-engine` src.

## 5a. The four other gates this session moved, and one it did not

**The CLI's count pins, `28e6ef4`, its own commit per R-pins.** Four
pins and two spelled-out id sets in `bases/cli`, all one fact: 22 -> 36
in the registry, 12 -> 26 in the `:format :event` filter. Found by the
full suite, not predicted -- the corpus-side gates say nothing about
what `ehrt corpus operators` prints. `ehrt.cli.core-test` alone: 340
tests, 1064 assertions, 0 failures, where it had been 6 failures and
every one of them a count.

**The stale-asset tripwire, and the trigger did NOT fire.**
`docs/manual/assets/inject-expect-loop.svg` names
`components/corpus/docs/pipeline.edn` as its source, so the rider
tripped it. Its trigger is "pipeline.edn's Mutate/Gate stages or the
worked instance's own witnessed values", and the rider's diff is 44
insertions, 0 deletions, all of them one stage entry inserted before
`:check`. The asset's own text was read rather than assumed -- a
file-level `storefront-patient.json` through `Mutate` with
`remove-required-element` to a `rejected :invalid` Gate verdict, and
not one sim-side name in it. `:reviewed-at` bumped to `adbf6e15` with
the reasoning as its note.

**The oracle and the bracket, both IDENTICAL against `753d320`.**
`bin/regression-oracle 753d320 HEAD` -- IDENTICAL, every root.
`bin/ground-truth-bracket 753d320 HEAD` -- IDENTICAL, 38 digested
roots, the same 3 keyless skips (`appendicitis`, `ear-infections`,
`sore-throat`). Both expected and both worth paying: mutation is
POST-RUN (Q1(a)), outside `engine/run` entirely, and no oracle root
applies an operator -- so an operator catalog that doubled must move
nothing, and a moved digest would have meant the catalog reached
somewhere it must not.

**The one this session did not move:**
`roadmap.md#event-mutation-catalog-gate`. Its second clause stays
BLOCKED on Q11(a); only its stale cost figure moved, and section 6
call 4 discloses that.

## 5b. The close gate

**Full `make test`, `MAKE_EXIT=0`: 4,827 tests, 26,851 assertions, 0
failures, 0 errors**, over both projects (`conformance`, `ehrt-cli`),
in **2,043 s** wall. The run went to a log with `$?` captured
explicitly and the wrapper ended in `exit "$MAKE_EXIT"`.

**The comparison figure needs a caveat, and this is it.** The last
`make test` figure recorded ANYWHERE is 4,817 / 25,797, in
`.agents/session-records/2026-09-04-prime-audience.md`. The session
between it and this one -- ADR-0178, 2026-09-05 -- records "Full `make
test` green" and no numbers, so **+10 tests / +1,054 assertions spans
TWO sessions, not one**, and part of it (a catalog pin 45 -> 46, a
`:transfer-in-error` equality boundary test, two `persons_test`
assertions rewritten from `nil?` to `contains?`) belongs to that one.
Stated rather than silently attributed here.

`clojure -M:poly check` green. Two earlier full runs both stopped, and
both stops were real: the stale-asset tripwire at `docs-tooling`, then
the four CLI count pins.

## 6. Judgment calls, and their ratification status

1. **Fixing (b), the id collision, rather than stopping.** One
   defensible reading -- two operators cannot share an id, and the
   alternative is a catalog that lies about its own size. Fix-forward
   with disclosure. Not separately ratified.
2. **Shipping the gate on the slow path rather than fixing
   `valid-event?`.** The fence is explicit and the deliverable is not
   blocked by the cost, only made expensive; a fence is not something
   to reason past because the work would be nicer without it. Not
   ratified; disclosed above and rowed nowhere, per the de-scaffold
   ruling's "a finding is one line in the record".
3. **Correcting `docs/consuming-ground-truth.md` inside the GREEN
   commit.** Co-landing, not scope creep: the two sentences are claims
   ABOUT the catalog that commit changes.
4. **Editing `roadmap.md#event-mutation-catalog-gate`'s cost clause**,
   a row this session did not otherwise touch and whose fence keeps it
   BLOCKED. It said "twelve operators over two real runs"; this
   session made that false in both numbers, and the clause exists to
   price that row. Errata, not scope. Disclosed here because
   `rulings.md#R-register-hygiene-at-close` says to touch only the
   rows a session changed.
5. **Sampling 6 of 8 for column C's three non-excusing shapes.** The
   ledger's section 7 licence, and the same honesty: those three are
   evidence for a declaration, the four excusing-branch cells are a
   proof over the population.

## 7. Findings and HEAD landed

- **Finding 1 -- the id collision has NO symptom.** `register!` is
  `swap! assoc`; two entries deriving one `[id version]` overwrite
  silently. Closed by `:slug` plus a bijection gate, this session.
- **Finding 2 -- `valid-event?` rebuilds its validator per call**,
  365x slower than a prebuilt one, and `check-all` pays it per event
  since ADR-0178. AUTHOR ACTION, `sim-engine` src, one line.
- **Finding 3 -- the pipeline renderer resolves a resource to its LAST
  producer**, so appending a stage that outputs an already-produced
  resource silently re-draws another stage's arrows. Worked around by
  ordering, and the ordering is commented where it is load-bearing.
- **Not touched, per fence:** no `sim`, `sim-engine` or `sim-check`
  src. `roadmap.md#event-mutation-catalog-gate` stays OPEN and its
  second clause BLOCKED; only its stale cost figure moved.

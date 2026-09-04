# 2026-09-05 -- P7 stopped at step 1: the derivation, and why 13 cells ship rather than 14

`roadmap.md#referential-corpus-population` (PRIORITY 7) asked this
session to add referential carrier columns A, B2 and C as data, witness
all 14 cells under ADR-0176 Q5(a) set-equality, amend the population
ledger and close the row. **It stopped at the end of step 1** — the
derivation — and wrote nothing to `components/`. Step 1 was the only
step that carried no commit, so the stop costs no rework: the
measurement it produced is below and is what the resumed session builds
from.

Two things the derivation measured made the remaining steps
unexecutable as written. One has a single defensible reading and the
author assented to it (13 cells, not 14). The other has two, so
`rulings.md#R-stop-only-on-two-defensible-readings` bound and the author
ruled it a separate session that runs BEFORE P7 resumes.

Base `094791e`. Ceremony: R30, commits at each checkpoint. No
sub-agents. Docs-only diff: no `components/` file, no test and no
generated payload surface changed.

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** Last five CI runs
on `main` green; edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`; `core.fileMode` true, `core.ignorecase` unset; tree clean
including untracked; local HEAD matched `origin/main` at `094791e`. One
disclosure, and it is the correct state: HEAD is not tagged `stable-*`
— no tag is paid.

## 1. The population, measured

`demos/scenarios/dense-7500/config.edn` at seed 5 with `:churn true`,
per R-population. The ruling asked for the SMALLEST `--patients` at
which every A/B2/C shape has a site, derived by measurement, with
generation under 60 s in-test.

| `--patients` | events | run wall | A | B2 | C | parent `check-all` |
|---|---|---|---|---|---|---|
| 1 | 18,135 | 27.2 s | 0 | 0 | 0 | `#{}` |
| 5 | 18,174 | 25.8 s | 0 | 0 | 0 | `#{}` |
| 10 | 18,279 | 25.6 s | 1 | **0** | 1 | `#{}` |
| **20** | **18,466** | **25.8 s** | **4** | **6** | **8** | `#{}` |
| 40 | 18,874 | 25.8 s | 13 | 22 | 17 | `#{}` |

**The answer is 20**, and the reason 10 is not is B2: at 10 patients no
`:medication-end` resolves to a log event at all. Generation is **~26 s
and FLAT across every `--patients`**, which is the fact worth carrying
forward — the cost is the `:persons {:count 15000 :years 20}` layer, not
`--patients`, so going below 20 buys nothing and 7,500 would buy the
whole ledger's counts for minutes rather than seconds. Comfortably
inside the 60 s bound either way.

`:cancel-admit` NEVER OCCURS in this population: column A's carriers are
4 `:cancel-transfer` at 20 patients, and 12 `:cancel-transfer` plus 1
`:cancel-discharge` at 40. Recorded rather than papered over — the
column is real and populated, but two of its three carrier kinds are
witnessed only at 40 and one not at all.

## 2. The 14 cells

Carrier, target and schema type per column, with each shape's own
narrowed site count measured at both 20 and 40 patients. Schema types
are read from `components/sim-engine/src/ehrt/sim_engine/event_schema.clj`.

| col | field | carrier kinds | target kind | schema type | null cell |
|---|---|---|---|---|---|
| **A** | `:cancels-event-id` | `:cancel-admit` / `:cancel-transfer` / `:cancel-discharge` (`:716`, `:723`, `:735`) | **per carrier**: `:admission` / `:transfer` / `:discharge` | `:int` | dropped (Q9(a)) |
| **B2** | `:order-event-id` | `:medication-end` (`:944`) | `:medication-order` | `[:maybe :int]` | kept |
| **C** | `:start-event-id` | `:care-plan-end` (`:972`) | `:care-plan-start` | `[:maybe :int]` | kept |

So 4 + 5 + 5 = **14 cells named**, which is the step-1 gate, and it
matches the ledger's own section 6 arithmetic. Site counts, and the
Q5(a) verdict per cell over 6 strided sites each at 40 patients:

| col | shape | sites @20 | sites @40 | observed = declared |
|---|---|---|---|---|
| A | `phantom` | 4 | 13 | yes |
| A | `cross-patient` | 4 | 13 | yes |
| A | `wrong-kind` | 4 | 13 | yes |
| A | `inverted-span` | 4 | 13 | **NO** (finding 1) |
| B2 | `phantom` | 6 | 22 | yes |
| B2 | `null` | 6 | 22 | yes |
| B2 | `cross-patient` | 6 | 22 | yes |
| B2 | `wrong-kind` | 6 | 22 | yes |
| B2 | `inverted-span` | 6 | 22 | yes |
| C | `phantom` | 8 | 17 | yes |
| C | `null` | 8 | 17 | yes |
| C | `cross-patient` | 8 | 17 | yes |
| C | `wrong-kind` | 8 | 17 | yes |
| C | `inverted-span` | 8 | 17 | yes |

**13 of 14 convict exactly**, in both directions, with the mutated event
schema-valid at every sampled site. The parent is clean (`#{}`) at every
`--patients`, so each equality is a statement about the operator rather
than about the corpus — step 1 of ADR-0176 section 2(iv)'s loop.

B2's and C's `null` and `phantom` cells were the two most at risk and
both hold. Both invariants excuse a nil reference when the patient's own
`:registered` carries a matching `:pre-horizon-facts` citation
(`check.clj` `pre-horizon-referent?`, and `resolving-sites`' own
docstring warns that a column for either "would have to mirror it").
These four cells were therefore re-measured EXHAUSTIVELY rather than by
sample -- every site, not 6 of them -- and nulling or dangling the
reference convicts at **all 106 of them: 0 non-matching** (at 20
patients, B2 6 + 6 and C 8 + 8; at 40, B2 22 + 22 and C 17 + 17). No
site in this population reaches the excusing branch, so the mirroring
`resolving-sites` warns about is not owed here. This is the ONE claim in
this record that is a proof over the population rather than a sample,
and it is the one that most needed to be: an unsampled site reaching
that branch would have shipped a cell that fails intermittently.

## 3. Finding 1 -- `A/inverted-span` is unwitnessable, and 13 ship

**Declared** `#{:cancel-references-existing-uncancelled-event,
:timestamps-monotone}`. **Observed** `#{:timestamps-monotone}` — at
every sampled site, and at the two sites timed individually
(`:cancel-transfer` at log index 105, `:t` 4500 -> 4499; index 141, `:t`
7860 -> 4739; 2.7 s and 2.6 s respectively).

The cause is structural and is in the checker, not in the corpus:
**`cancel-references-existing-uncancelled-event` HAS NO TIME CLAUSE.**
Its four disjuncts are a missing target, a wrong target kind, a target
not naming the cancel's own patient, and a target already cancelled by
an earlier cancel of the same kind (`check.clj:940`). Unlike B2's and
C's invariants it never compares `(:t target)` with `(:t event)`. So
moving a cancel behind its referent breaks monotonicity and nothing
referential.

Shipping the cell anyway would register an operator in the referential
family that convicts nothing referential — a duplicate `:clock-skew`
wearing a reference field's name, and it would let
`every-schema-reference-field-is-covered-or-declared-empty-test` count
`:cancels-event-id` as covered by a shape that cannot convict it. Under
ADR-0176 Q5(a) ("an unwitnessable operator is refused, not shipped")
the cell is **REFUSED**. Column A ships **3**, the catalog gains **13**,
and the 14th is a SHAPE gap — the ledger's own third category, the same
outcome measurement forced on all three structural operators on
2026-09-01, and not a population gap.

**Author ruling, 2026-09-05: assented, 13 not 14**, with the 14th's
cause rowed as a checker gap —
`roadmap.md#cancel-invariant-has-no-time-clause`, added by this session.
The interesting half is what the invariant has INSTEAD of a time clause:
an already-cancelled disjunct that neither B2 nor C has. A shape derived
from THAT disjunct would restore a fourth cell to column A honestly.
Deliberately not designed here — it is a shape, and shapes are ADR-0176
section 2(i)'s business, not a build session's.

## 4. Finding 2 -- the population's parent log violates the event schema

**Three `:registered` events carry `:window-close-t nil`** where the
schema declares `[:window-close-t {:optional true} :int]`
(`event_schema.clj:626`). Optional permits the key to be ABSENT; it does
not permit it to be present and nil. Log indices 1669, 18794 and 18825
at 40 patients, and **3 at every `--patients` measured, including 1** —
they arrive with the `:persons` layer, so no choice of population size
avoids them.

The mechanism is two lines, and they disagree with each other:

- **`components/sim-engine/src/ehrt/sim_engine/run.clj:146`** —
  `placeholder-registration`'s `(cond-> ... (some? (:branch window))
  (assoc :window-close-t (:until-t window)))` deliberately OMITS the key
  when the window never resolves. Its docstring is emphatic that this is
  a correction the tree forced (ADR-0173 section 2(d)): the person died
  inside the window, so "the ENGINE declines to promise a close instant
  it already knows will never come".
- **`components/sim-engine/src/ehrt/sim_engine/decide.clj:331`** — the
  `placeholder?` branch of `decide :registered`'s `cond->` re-adds it
  UNCONDITIONALLY, alongside `:alias-name` and `:residence`. The
  compiled entry has no such key, so the destructured
  `window-close-t` is `nil` and the emitted event carries
  `:window-close-t nil`.

The omission upstream is undone one layer down. The fix is to move that
one key into its own `(some? window-close-t)` clause — `sim-engine` src,
inside this session's fence.

**Why it has been invisible.** Nothing in the tree reads the distinction
between absent and nil, and three assertions that look like they guard
it cannot:

1. `components/sim-engine/test/ehrt/sim_engine/persons_test.clj:651` —
   `(is (nil? (:window-close-t (first ph))) "an unresolved window
   promised a close instant it cannot keep")`.
2. `components/sim-engine/test/ehrt/sim_engine/persons_test.clj:844` —
   `(is (every? #(nil? (:window-close-t %)) ph) "a window with no
   resolution still promised a close instant")`, whose sibling comment
   says "only the DUE instant is withheld".
3. `components/sim-check/src/ehrt/sim_check/check.clj:1596` —
   `every-placeholder-registration-is-resolved-or-still-open` classifies
   `(nil? window-close-t)` as `:unjudgeable`, identically to absent, so
   the checker tolerates it too.

`nil?` is true whether the key is absent or present-with-nil. All three
assert the engine's INTENT in the one form that cannot distinguish it
from the defect, which is why `check` runs green over a log Malli
rejects. This is the ADR-0166 shape again — a distinction one surface
draws that no gate reads — one layer further out.

**Why this stopped the session rather than being fixed forward.** The
loop's step 3 is ruled: `(every? engine/valid-event? mutant)`, ADR-0176
Q9(a). On this population it is false on the PARENT, for reasons no
operator causes. Two readings are both defensible — (a) restate step 3
as "the mutated event is schema-valid and the mutation adds no invalid
event", which measurement shows true for all 13 cells, and ship on a
parent that violates the schema; or (b) hold that a schema-invalid
parent disqualifies an oracle population and fix the engine first. That
is exactly `rulings.md#R-stop-only-on-two-defensible-readings`.

**Author ruling, 2026-09-05: (b), as a separate session that runs BEFORE
P7 resumes.** Not rowed here — the author holds it.

## 5. Finding 3 -- `:target` must be per carrier

Step 3 of the prompt scoped the payload to "three entries in
`referential-columns`; nothing else in `operators.clj`". That premise
does not hold. `referential-entry` derives one operator id per
`(shape, field)` pair, so column A must be ONE column; but its three
carrier kinds cancel three DIFFERENT event classes
(`check.clj`'s own `cancel-target-type` map), and three of the five
shapes read `(:target column)` as a single keyword to pick or reject a
referent.

The resumed session therefore also needs `:target` to be a keyword OR a
per-carrier map, read through one small `target-kind` helper the shapes
call — measured working in the probe that produced section 2's table.
Mechanical, one defensible reading, so it is fix-forward with
disclosure rather than a second stop. Recorded because the resumed
session's own scope sentence should say it up front.

## 6. Judgment calls, and their ratification status

1. **Stopping at step 1 rather than shipping (a).** RATIFIED by the
   author, 2026-09-05, both counts.
2. **13 cells, the 14th refused and rowed as a checker gap.** RATIFIED,
   2026-09-05.
3. **Naming all 14 cells before refusing one.** The step-1 gate is "14
   cells named", and they are — refusal is a verdict ON a named cell,
   not a reason to leave it unnamed. Not separately ratified; it is how
   the gate reads.
4. **Sampling 6 strided sites per cell, not all of them.** Same
   licence and the same honesty the ledger's section 7 takes: every
   "observed = declared" above is a SAMPLE and evidence for a
   declaration, not a proof over all sites. The shipped gate asserts at
   specific seeds. ONE EXCEPTION, and it is deliberate: B2's and C's
   `null` and `phantom` cells are measured over EVERY site (section 2),
   because those four are the ones an unsampled site could have flipped
   -- the pre-horizon excusing branch is a real branch in the invariant,
   not a hypothetical. Not ratified; consistent with precedent.
5. **Probes not promoted to `bin/`.** The ledger's section 7 rule,
   followed: that is author-licensed fence widening and nothing here
   needs to run per push. Not ratified; consistent with precedent.

## 7. Findings and HEAD landed

- **Finding 1** — `A/inverted-span` refused; catalog gains 13 not 14;
  cause rowed at `roadmap.md#cancel-invariant-has-no-time-clause`.
- **Finding 2** — `:window-close-t nil` on 3 `:registered` events at
  every population size; `run.clj:146` vs `decide.clj:331`; three blind
  `nil?` readers named in section 4. Author-held, its own session next.
- **Finding 3** — `:target` must be per carrier; fix-forward, disclosed
  for the resumed session's scope sentence.
- **Not touched, per fence:** no `components/` file, no test, no
  generated payload surface, no `sim` / `sim-engine` / `sim-check` src.
  `roadmap.md#event-mutation-catalog-gate` stays BLOCKED and untouched.
  `roadmap.md#referential-corpus-population` stays OPEN, unedited — the
  resumed session amends it.
- **The ledger is NOT amended.** Its section 6 still reads 14 population
  gaps, which was true when measured and is still true: nothing shipped.
  The 14th's reclassification from population gap to shape gap lands
  when the operators do.

# Session record -- arc 3a, part 1: `:residence-loss`, the fifteenth kind

**Date:** 2026-08-25
**Prompt:** [`.agents/prompts/2026-08-25-arc-3a-demographic-fold-execution.md`](../prompts/2026-08-25-arc-3a-demographic-fold-execution.md)
**Base:** `e9bc65b` -- **Tip:** `67270dd`
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony
(commit and push at each checkpoint), no tag, CI green at the tip as the
close marker.

## READ THIS FIRST -- the session did not complete its prompt

The prompt names five steps. **Steps 0 and 1 landed, complete and
proven. Steps 2, 3 and the roadmap half of step 4 did NOT, and nothing
of them is half-landed.** The boundary is deliberate and is the whole
finding of this record; the section *What steps 2-3 still owe* below
sizes what remains and says why stopping here was the safe cut rather
than an arbitrary one.

What that means for the tree: `:persons` does not exist as a config
key, `sim-engine` still does not require `person-simulator` and still
has no caller, and every one of the 35 oracle roots is byte-identical
to `e9bc65b`. ADR-0172 ruling F1 -- *the component lands ALONE* --
still holds verbatim. Arc 3a's roadmap row is still OPEN, and the ADR
is Accepted rather than implemented.

## What landed

| commit | what |
|---|---|
| `24d9c4c` | ADR-0173 **Accepted**, the five rulings quoted where they land |
| `67270dd` | `:residence-loss`, the fifteenth kind -- oracle IDENTICAL |

### Step 0 -- ADR-0173 Accepted

All five lettered rulings took the recommendation: **A1 B1 C1 D1 E1**.
Each is quoted at the option it selected, ADR-0172 section 5's shape,
with the declined options kept unstruck. Plus the author's one
sequencing instruction, which is not a lettered ruling and binds the
execution session anyway: **`:residence-loss` lands FIRST**, before
any engine code.

`notes/ADRs.md` and `.agents/state-derived.md` regenerated (`make
adr-index`, `make state-derived`). `:onboarding` reading set 1,370 ->
1,372 against a 1,530 cap: headroom 160 -> 158, no budget question and
so nothing owed to `rulings.md#R-budget-stop`.

### Step 1 -- the fifteenth kind

UNHOUSED IS A STATE, not an address. `sim-model/Persona`'s `:address`
is required and non-nilable, and limitations row 7's gate is `(remove
#(pool (:address %)) moves)` -- so an unhoused `:residence-move` would
go RED against a gate that is doing its job. The sum therefore lands
as a new KIND, and row 7's gate stays green **verbatim** rather than
repaired. That is ADR-0173 section 2(b)'s own reasoning, and it
survived contact with the tree unchanged.

* `:residence-loss` carries `:prior-address` and **no `:address` at
  all**; `:at-t0` marks a person who entered the run with no residence
  rather than losing one. The engine folds a stream and has nowhere
  else to read an initial condition from, which is why `:unhoused
  {:t0-fraction 0.02}` reaches it as an event at t0 rather than as a
  field.
* The return to housing is an ordinary `:residence-move` whose
  `:prior-address` is **ABSENT** -- absent, not nil. The no-change
  guard (`b4f1115`'s law: *an event that reports no change is not an
  event*) is exempted for it, because landing back on the last row
  lived at is still a change when the prior state was nowhere.
* Two new PROVISIONAL rates, both tabled under limitations row 9:
  `residence-loss-rate` 0.006 per HOUSED person-year, `rehousing-rate`
  1.2 per UNHOUSED person-year. The second is read off the **same**
  `:move` variate the housed move uses, with only the RATE conditioned
  on state -- one variate, two rates, so the return to housing costs
  no second draw.
* The draw block grows **18 -> 19**, appended so positions 1-18 keep
  the names and order arc 2b gave them, plus **one** t0 residence
  variate per t0 adult. A newborn draws neither: ruling A1 derives its
  housing from its household.

## The deviation: a thirteenth limitations row ADR-0173 did not price

**`A household never loses its housing.`**

The tree forced it, and it was found by a red test rather than
reasoned about in advance. The first design propagated a head's
`:residence-loss` to every household member, mirroring ruling B1's
existing move propagation, and gave the propagated event its own
referential field `:household-loss-event-id`. Adding that field turned
an EXISTING gate red:

```
FAIL in (every-reference-resolves-to-an-event-of-the-right-kind-test)
population is non-empty (R-empty-population-is-red)
not every referential field is exercised by the witness stream
expected: (= (set (keys reference-kinds)) (set (map second refs)))
  actual: (not (= #{... :household-loss-event-id ...} #{...}))
```

That gate requires every referential field to be EXERCISED by the
witness, and a head losing housing while heading a household with live
members is knife-edge rare at any honest rate. Raising the rate to
make the gate green would have been tuning a world model to satisfy a
test.

Reading the propagation pass instead gave the real answer: it copies a
head's `:residence-move` to every member **verbatim**, so a member who
could lose housing on their own would receive copies reporting a
change they never had -- a housing-gained move naming no
`:prior-address`, delivered to somebody who never stopped having one.
Coupling housing to household membership is what keeps that copy
honest, and it costs no draw:

* the loss hazard reads zero for anyone in a household;
* the household hazard reads zero for any year not spent housed
  *throughout* (unhoused at its start, or made unhoused inside it);
* ruling A1's newborn is the one member who can be unhoused, because
  it is DELIVERED into an unhoused household rather than losing
  anything -- and it gets its own `:residence-loss` at the birth
  instant, since its Persona must carry the household's last known
  address and a newborn entering the run housed at an address nobody
  lives at is the fabricated-by-omission class this project may not
  ship;
* a household constituted BY such a birth is kept off the join roster,
  so nobody housed can join one whose head has no residence.

One defensible reading, so the tree wins and this is the record
(`rulings.md#R-stop-only-on-two-defensible-readings`). Carried in
three places in the same commit: ADR-0172 section 4 (row 13 plus a
paragraph saying it was NOT anticipated there), the charter table, and
ADR-0173's own Consequences as a dated DEVIATION.

## Red before green

Six new deftests, every one run red against the unfixed `src` first.
The red run's own output is in the commit and summarised here; the
unplanned failure above is included rather than filtered.

| test | red because |
|---|---|
| `the-fifteen-kinds-are-the-closed-vocabulary-test` | the stream had fourteen kinds |
| `residence-loss-carries-no-address-and-names-the-one-it-lost-test` | `(not (seq []))` -- no `:residence-loss` at all |
| `a-move-out-of-unhoused-is-housing-gained-and-names-no-prior-address-test` | nobody had ever regained housing |
| `a-move-while-housed-still-names-its-prior-address-test` | ran green from the start (the arc-2b side of the sum, asserted so the new branch cannot quietly take it over) |
| `the-t0-unhoused-fraction-is-honoured-at-t0-test` | no `:at-t0` event existed |
| `a-newborn-of-an-unhoused-household-is-born-unhoused-test` | 26 newborns, every one entering housed |
| `only-household-less-persons-become-unhoused-test` (row 13) | no `:residence-loss` in the stream |
| `every-one-of-the-fifteen-kinds-has-a-counted-witness-test` | `:residence-loss has no witness at all` |

35 failing assertions across 33 tests on the red run; 0 on the green.

## The re-pin: one cause, declared

The nineteenth variate moved every draw from a person's second year
onward. **Nothing calls this component** (ADR-0172 ruling F1), so no
corpus moved with it -- the witness table IS the whole blast radius,
and that is why this re-pin needs no corpus sweep behind it.

| kind | arc 2b | arc 3a | | kind | arc 2b | arc 3a |
|---|---|---|---|---|---|---|
| `:residence-move` | 163 | 118 | | `:identity-correction` | 36 | 26 |
| `:residence-loss` | -- | **6** | | `:identity-resolution` | 5 | 8 |
| `:coverage-change` | 147 | 131 | | `:identity-unavailable` | 5 | 8 |
| `:employment-change` | 135 | 122 | | `:occupational-injury` | 5 | **2** |
| `:household-form` | 38 | 34 | | `:person-death` | 17 | 19 |
| `:household-join` | 52 | 37 | | `:person-registered` | 26 | 11 |
| `:household-leave` | 16 | 10 | | `:pregnancy` | 26 | 11 |
| `:delivery` | 26 | 11 | | **total** | 697 | **554** |

Two movements are FINDINGS rather than numbers, and are disclosed in
the witness table's own docstring so a later reader meets them there
and not only here:

1. **`:occupational-injury` fell 5 -> 2.** ADR-0173 section 1 had
   named it as one of the four thinnest fixtures in this witness
   BEFORE the reshuffle, and it is now the thinnest thing in it. Still
   `pos?`, so no gate went vacuous -- but it is thin enough that the
   next reshuffle could empty it, and arc 3a's own occupational-injury
   hook (section 2(c)) will need a denser witness than this one.
2. **`:delivery` / `:person-registered` / `:pregnancy` fell 26 -> 11
   together**, which is ONE movement of the three: a delivery is
   deterministic given its pregnancy, and a newborn's registration is
   deterministic given its delivery.

**The witness population was deliberately NOT widened to compensate.**
Widening it would move all fifteen counts for a SECOND reason inside
the same diff, which is exactly the confounding ADR-0173 ruling D1
exists to avoid. Row 12's own pinned count moved 4 -> 3 for the same
single cause and was re-pinned with the cause named at the assertion.

**The witness config now sets `:unhoused {:t0-fraction 0.08}`**, not
the ADR's 0.02 default. At 60 persons the default draws 1.2 t0-unhoused
people in expectation and drew **zero** at this seed -- a witness that
would have proved the t0 path by going empty
(`rulings.md#R-empty-population-is-red`). The DEFAULT is asserted
separately, on its own config, by driving the fraction to 0.0 and 1.0
and by comparing an absent `:unhoused` key against the default's own
value.

## Gates

| gate | result |
|---|---|
| `clojure -M:poly check` | **OK** -- no `sim-engine` -> `person-simulator` edge exists or was added (ADR-0172 limitations row 10, both halves) |
| `make test` | **MAKE_EXIT=0**, 4,120 tests / 18,447 assertions, 0 failures, 0 errors |
| `make integration` | **INT_EXIT=0**, 0 `FAIL:` lines, 1,502 tests / 4,813 assertions -- byte-for-byte the same counts arc 2b's record closed at, so no integration test moved. Run because W-1 stands: `make test` skips this tier, so a gate can land unexecuted |
| `bin/regression-oracle e9bc65b HEAD` | **IDENTICAL**, exit 0, no declaration |

**The suite delta reconciles exactly.** The design session's own record
(`.agents/session-records/2026-08-25-arc-3a-demographic-fold-design.md`,
which is the document that CARRIES that figure) closed at 4,114 tests /
18,412 assertions. This session closes at 4,120 / 18,447: **+6 tests,
+35 assertions**, which is the six new deftests and nothing else.

**One self-inflicted red, disclosed rather than filtered.** The FIRST
`make integration` run went **INT_EXIT=2** on a single line:

```
== demo-exerciser-ed-tuesday: checking named invariants ==
FAIL: tree not clean after a full run (ADR-0005 postcondition violated):
 M .agents/plans/roadmap.md
 M .agents/prompts/INDEX.md
 M .agents/session-records/INDEX.md
 M .agents/state-derived.md
?? .agents/prompts/2026-08-25-arc-3a-demographic-fold-execution.md
?? .agents/session-records/2026-08-25-arc-3a-residence-loss.md
```

Every one of those six paths is this record and its register updates,
written WHILE the tier was running. The gate is right and the operator
was wrong: `demo-exerciser-ed-tuesday` asserts ADR-0005's postcondition
over the whole worktree, so editing anything during the run fails it.
The fix was to `git stash push -u` back to `67270dd`'s exact tree
(`398c1dc...`, the same tree the oracle bracketed) and re-run; the
INT_EXIT=0 above is that clean run. **The lesson is a scheduling one:
this tier may not share a worktree with an editing session, and a
close-out write during a background gate run is the way to trip it.**

**The oracle line, verbatim:**

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between e9bc65b and HEAD
```

Two precisions about that line, because both would otherwise be read
wrong. It was produced against commit `57d1bd0`, which a **message-only**
`git commit --amend` on an unpushed commit then rewrote as `67270dd`
in order to carry the oracle line in its own message
(`rulings.md#R-amend-unpushed-message-only`, disclosed). The two trees
are the same object -- `git rev-parse 57d1bd0^{tree} 67270dd^{tree}`
returns `398c1dc...` twice -- so the bracket the oracle proved is the
tree that is pushed. And IDENTICAL here is not a claim that the fold
does not move the corpus; it is a claim that **the fold has not landed
yet**, which is F1 still holding after a fifteenth kind and a
nineteenth variate.

## What steps 2-3 still owe

Nothing of the engine fold landed, and the cut is at a clean boundary
by design: every remaining piece of ADR-0173 section 2 is entangled
with every other, and a half-landed `:persons` key that half-works is
worth less than none. Sized here from the tree so the next session
does not re-derive it.

| ADR-0173 | what it needs | where |
|---|---|---|
| 2(a) config | `:persons` joins `config-keys` in the same change that teaches `run` to read it (that def's own docstring requires it), plus `ehrt.sim.run/run-command` forwarding, which has its own completeness test | `engine.clj:1582`, `run.clj` |
| 2(a) selection | one `:world` draw per arrival ordinal over persons alive at that instant; `:person-index` in `init-world` beside `:reinstate-index`/`:citation-index` | `engine.clj:1882`/`:1889` |
| C1 ordering | the module walk moves from arrival-time to run-start so `persons` can be called with real `:deaths`; the engine exports the compile so `ehrt.sim.run` can call it on the engine's own `:patient` stream. **This is the piece with real byte-identity risk** and the one that must be gated hardest | `engine.clj:483-517` |
| 2(b) fold | `PatientState` gains `:demographics`; new `evolve` siblings; a queue-seeding pass (`run`'s queue is already a `sorted-map` keyed `[t seq-no]` and `schedule-followup` already inserts at an absolute instant, so the loop itself does not change) | `engine.clj:166`, `:1847`, `:1994` |
| 2(b) re-key | `personas-by-patient-id` -> `demographics-at`, one lookup shape and **twelve** threading signatures; `personas-are-keyed-by-patient-id-alone-test` goes red in exactly this change, which is what it was written for, and limitations row 6 is then STRUCK rather than repaired | `emit_hl7.clj:302` and `:237/:245/:254/:286/:390` |
| 2(b) vocabulary | `:demographic-update` and `:coverage-change`; contract **1.2.0 -> 1.3.0**. Note the hard ordering this creates: `make event-schema-examples` lifts one REAL event per kind out of `ehrt.sim-engine.event-fleet`, so the fleet needs a fifth fixture run carrying `:persons` before the two kinds can be declared. A test-path fixture with `:persons` is not "an existing config", so the opt-in law and the oracle proof both survive it | `event_schema.clj:263`, `event_fleet.clj` |
| 2(c) hooks | delivery -> newborn patient at ordinal `(+ patients k)` plus the parent's admission; occupational-injury -> an ED arrival cause | -- |
| 2(d) identification | placeholder registration, the fill, and an `:identification-merge` step type with its OWN guard emitting churn's `:merge` shape (churn's `never-mergeable?` excludes `:new`, and a placeholder patient is exactly `:new`) | `engine.clj:828`/`:837`/`:1246` |
| 2(e) invariants | six, registered in `check-all` | `check.clj` |
| 2(f) provenance | `engine-params` grows `:persons` and `:persona-config`; `ManifestV1_1` is an open map so no schema change is owed | `run.clj:391`, `manifest.clj:99` |

Step 3's opt-in list and its single declared sweep are unchanged by
this session and are exactly as the prompt states them.

**One thing the next session should NOT re-derive:** ADR-0173's own
premises were re-probed against the live tree this session wherever
step 1 touched them, and every one held -- `sim-model/Persona`'s
`:address` really is required and non-nilable, limitations row 7's
gate really does read `(:address %)` and really would go red on a nil,
and `clojure -M:poly check` really does refuse the reverse edge. The
one premise the tree ADDED to is row 13, above.

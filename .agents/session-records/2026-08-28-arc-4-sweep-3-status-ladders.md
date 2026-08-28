# Arc 4 sweep 3 — status ladders (ADR-0175 design (b), ruling B1)

2026-08-28. Base `d0e7feb`; four payload commits; this record and the
CI marker follow them. Ceremony: R30 (commit and push at each
checkpoint), taken from the session prompt.

`bin/preflight` ran first, **exit 0, no findings**: the last five CI
runs on main all green; repo root not under `/mnt/`; `core.fileMode`
true and `core.ignorecase` unset; working tree clean including
untracked; local HEAD matched `origin/main`; HEAD not tagged `stable-*`
— disclosed and correct, no tag is paid.

## What landed

| sha | commit |
|---|---|
| `b0c85fd` | `order-pathway`, the 41st root — the first ever to place an order |
| `4c4df1d` | status ladders — ORC-5, OBR-25, OBX-11, DARK |
| `11664e0` | ladders TURNED ON in six corpora and the `order-pathway` root |
| `9c57280` | re-witness the straddle timeline at the ladder turn-on |

## The three brackets, and what each one proves

**Step 0, the root** (`d0e7feb` → `b0c85fd`). Both scripts under
`--declared-digest-change`, and the declaration is owed twice over:
`digest.clj` gains a producer function, and its COVERAGE block moves.

```
bin/ground-truth-bracket d0e7feb b0c85fd --declared-digest-change
  36 pre-existing roots' :ground-truth IDENTICAL
  1 line ADDED: order-pathway.edn        0 changed, 0 removed

bin/regression-oracle d0e7feb b0c85fd --declared-digest-change
  40 pre-existing roots' digests IDENTICAL
  1 line ADDED: order-pathway.edn        0 changed, 0 removed
```

**WHAT `bin/regression-oracle`'S CONTRACT ACTUALLY SAYS ABOUT AN ADDED
ROOT**, since the prompt asked. It has NO vocabulary for one: it
sha256s every `*.edn` in each side's output directory, diffs the two
manifests, and prints `DIFFERS` with a unified diff and exit 1 on any
difference at all — an addition included. So "IDENTICAL plus one `+`"
is a READING of its diff, stated here with the counts that back it, not
a verdict the script emits. The declaration is forced independently, by
the soundness check over `digest.clj`'s body, which aborts without the
flag. Both halves are sweep 2's precedent, unchanged.

**Step 1, the mechanism, DARK** (`b0c85fd` → `4c4df1d`). `digest.clj`
is untouched, so no declaration is owed and none was passed:

```
bin/ground-truth-bracket b0c85fd 4c4df1d
  soundness: IDENTICAL outside the leading docstring -- proceeding
  IDENTICAL: every digested root's :ground-truth matches (38 roots)   GTB_EXIT=0

bin/regression-oracle b0c85fd 4c4df1d
  IDENTICAL: every root's digest matches                              RO_EXIT=0
```

**THAT IS THE STRONGEST LINE IN THIS RECORD.** A new config schema, a
new planner, three new site-profile code tables, four builders given a
new arity, a status-aware replay arm — and not one byte of ground truth
or of any wire moved anywhere.

**Step 2, the turn-on** (`4c4df1d` → `9c57280`), both declared:

```
bin/ground-truth-bracket 4c4df1d 9c57280 --declared-digest-change
  IDENTICAL: every digested root's :ground-truth matches (38 roots)   GTB_EXIT=0

bin/regression-oracle 4c4df1d 9c57280 --declared-digest-change
  1 line CHANGED: order-pathway.edn      0 added, 0 removed
```

**THE MOVER SET IS EXACTLY THE OPTED-IN ROOT, AND THE PREDICTION WAS
MADE BEFORE THE RUN.** `order-pathway` is the only root that opts in
and the only root that places an order; every other one of the 41 is a
non-mover. Named explicitly, because an IDENTICAL over the wrong
population is what this sweep's step 0 existed to prevent:
`dermatitis` and `veteran-self-harm` (sweep 1's two ZERO-MESSAGE roots)
are IDENTICAL over an empty wire and prove nothing about emission;
`chatter-charges` is IDENTICAL because it deliberately places no order;
and the 36 module-only roots are IDENTICAL because none of them admits,
let alone orders. **Ground truth is IDENTICAL even on the root that
moved** — the whole claim of arc 4, on the one root where a ladder
exists to move it.

## The witness table, every column measured on the shipped configs

```
corpus                       events   msgs  orders results ORMrung ORUrung finalF
seed-202-ed-tuesday           1,213  1,323      30      30      30      30     30
seed-424242-clinic-decade     1,774  1,882       0       0       0       0      0
seed-5-clinic-decade          1,412  1,455       0       0       0       0      0
adhd-seed-45                     97    103       0       0       0       0      0
ed-tuesday (demo)             1,269  1,497      25      25      25      25     25
ed-tuesday-latency (demo)     1,269  1,497      25      25      25      25     25
clinic-decade (demo)          1,569  1,629       0       0       0       0      0
```

**THE ORDER COUNT WAS TAKEN FIRST AND IS PRINTED FIRST**, which is the
prompt's own instruction and the only honest way to read the rest of
the row: `pos?` may be asserted for rungs exactly where orders exist.
**Three of the seven corpora place NO order at all** — both
clinic-decade seeds, the clinic-decade demo, and `adhd-seed-45`, whose
content is a decade of ambulatory ADHD care with no `:order` step
anywhere. For those four, `:ladders` is inert.

**INERT WAS PROVED, NOT ASSUMED.** `seed-424242-clinic-decade` was
rendered twice from one process, with and without the `:ladders` key:
1,882 messages both ways and `BYTE-IDENTICAL true`.

**THE EVENTS COLUMN IS UNCHANGED IN EVERY ROW.** Read it against the
messages column: that is what an emission add-on is.

The rung volume is one ORM and one ORU per order at the shipped
`{:rungs [0.5] :order-rungs [0.25]}` — +60 messages on seed-202 (+4.9%)
and +50 on each ed-tuesday demo (+3.5%). The `order-pathway` root ships
richer, `{:rungs [0.25 0.5] :order-rungs [0.1 0.2]}`, so that the
oracle witnesses a ladder with a SECOND stage rather than a single
restatement.

## The rung's identity tuple, and its injectivity

**`(active-mrn, trigger, at, ordinal)`**, rendered as MSH-10
`mrn-trigger-t-<ordinal>`, where `trigger` is `O01` or `R01` and the
ordinal counts within `(mrn, trigger, at)`.

It is the FOUR-part key sweep 2 corrected ADR-0175 section 4 to, not
that section's own three-part `(basis-event-index, trigger, ordinal)`,
which sweep 2 measured non-injective for chatter. The ladder does not
worsen it: two rungs of one order differ in `at` by construction, and
two rungs of two orders at one instant differ in the ordinal. Asserted
injective on the plan and on a real population, and MSH-10 asserted
unique across base + chatter + ladder at a deliberate `t` collision
(the unit fixture offsets two four-thousand-second intervals by a
thousand seconds so that order 2's second rung and order 3's first land
on one instant for one MRN under one trigger).

`assign-chatter-ordinals` was extracted to
`assign-restatement-ordinals` and now serves chatter and the ladder
alike, so the two mint control ids by ONE construction. Chatter's bytes
are unchanged by construction and by the dark bracket.

## Defaults disclosed

The three tables ship in `ehrt.sim-emit-hl7.site-profile` beside
`:bed-status`, as authored, overridable data. **They are not citations
and nothing in the tree asserts them as HL7:** tables 0038/0123/0085
appear in no jar and no resource on any classpath here —
`hapi-structures-v24` 2.6.0 ships structures, not table content.

| field | table | ladder stages | terminal |
|---|---|---|---|
| ORC-5 | 0038 | `:scheduled` "SC" → `:in-progress` "IP", saturating | `:final` "CM", authored, unrendered today |
| OBR-25 | 0123 | `:preliminary` "P", saturating | `:final` "F" |
| OBX-11 | 0085 | `:preliminary` "P", saturating | `:final` "F" |

**"P", NOT "I", AND THE REASON IS CLINICAL.** A rung carries the
order's own analyte values — this project's log holds ONE result per
order, so there is no intermediate value to restate and inventing one
would be minting a fact — and 0123's "I" means precisely that no
results are available. "P" is the code for a verified early value that
may still change, which is what a rung actually is here. `:in-process`
and `:corrected` are authored and unused, so a site whose analyzer
really does send an empty in-process report has a code to override to.

The scenario configs ship `{:rungs [0.5] :order-rungs [0.25]}`: two
rungs is the smallest thing that is a ladder, and ORM-first/ORU-second
tells the sequence a reader expects — picked up, then preliminary, then
final. All three tables' overrides are proven green.

## No draw, all the way

`plan-ladders` takes no `java.util.Random` and claims no `:emission`
id-tag. `:result-available` carries `:order-event-id`, so both ends of
the interval are in the log and a rung is a pure function of
`(log, config)` — `plan-charges`' standing, one step stronger than
`plan-chatter`'s. **There is no fixed-consumption law here because
there is nothing to consume**, and the sweep says so rather than
inheriting a law it does not owe. Latency holds `:emission` id-tag 0
and chatter 1; this sweep claims no third. ADR-0175's rejected option
(2) is why the fractions are not sampled.

## What this sweep DECLARES it moves

The terminal `ORU^R01` of an order that grew a rung gains OBR-25 and
OBX-11. That edits an existing message's bytes and it is part of this
sweep's declaration.

**THE EDIT IS PER-ORDER, NOT PER-CONFIG.** `plan-ladders` returns
`:final` as the set of result LOG INDICES that actually grew a rung, so
an order whose interval admits no rung renders exactly today's bytes,
terminal message included. That is what makes *no rung ⇒ no byte
change* an assertable property rather than a hope, and the unit gate
asserts it on a zero-length interval sitting beside two laddered ones.
Indices rather than control ids, deliberately — see finding 1.

Measured on the ed-tuesday demo: 25 terminal results move, 1,447 other
messages do not, and stripping OBR-8..25 and OBX-11 from the 25 puts
them back byte-for-byte.

## The consumer: a preliminary is not folded

`v2-replay`'s `"R01"` arm now reads OBR-25 and skips a non-final
result, so **replaying a laddered corpus reconstructs the same
accumulator as replaying its un-laddered twin** — asserted, with the
observation count proven non-empty first so the equality is not two
empties. A corpus with no OBR-25 — every corpus this project emitted
before today — folds exactly as it always did.

It reads the STANDARD 0123 vocabulary rather than the site profile, and
that limit is stated in the arm itself: `fold-message` is handed bytes
and nothing else, so a site that overrides `:result-status` to
non-standard strings gets its preliminaries folded. Threading emission
config into a wire reader would make the replay depend on something the
wire does not carry, which is worse.

## Gate-side facts

* **No new registry entry and no new fold arm**, unlike chatter's three
  families and the DFT: a rung is an `ORM^O01`/`ORU^R01`, both handled
  in `evolve-entry` since M3.
* **The sampler absorbed the volume with no code change.**
  `skeleton-message-types` is DERIVED from `message-type-registry`, so
  both ladder families are SKELETON and every rung is gated in FULL.
  Asserted over a real laddered wire in
  `ehrt.conformance.ladder-sampling-test` — the one place `judge` and
  `sim` are composed — with the ORM/ORU strata reporting `n = gated`
  and `add-on? false`, and with a chatter stratum truncated at cap 5 in
  the same run so both halves of the policy are exercised rather than
  only the trivial one.
* **`classify-change` was never asked a question.**
  `event-schema.edn`, its baseline and its version are untouched; no
  event kind was added and no field moved. A ladder sweep that touched
  the event schema would have crossed `rulings.md#R-skeleton-or-emission`.
* **Neither witnessed set moves on the turn-on.** A rung is a family
  the oracle already witnesses as of step 0, and a ladder is emission,
  so `witnessed-message-types` and `witnessed-event-kinds` both stand.
  The message-side list was re-derived rather than assumed, and the
  digest's coverage block states the non-movement in its own prose.

## ADR premises contradicted

1. **Section 2(b) does not say which config decides a rung's family.**
   Implemented as two independent vectors — `:rungs` (ORU) and
   `:order-rungs` (ORM) — because one list cannot express a feed that
   sends order-status traffic at one cadence and preliminary results at
   another, and every real lab feed does exactly that.
2. **Section 2(b) proposes rungs at `k/(r+1)`, a COUNT.** Shipped as
   explicit FRACTIONS. Even spacing remains writable (`[0.5]`,
   `[0.33 0.67]`); a count cannot express the asymmetry the two
   families want.
3. **Section 2(b)'s "+30 messages, +4.0% at r = 1" is the ORU half
   only.** With both families at one rung each the probe-shaped corpora
   take twice that: seed-202 measured +60.
4. **Section 4's derivability law is used in sweep 2's corrected
   form**, the four-part key, not section 4's three-part triple.
5. **Section 2(b) is silent on the transmit clock.** The rule adopted
   and gated: a rung rides its BASIS event's own latency offset — the
   order's for an ORM rung, the result's for an ORU rung — so a rung
   can never overtake the message it restates under any latency
   profile. That is sweep 2's DFT finding generalised, and it is why
   chatter (which has no basis event) and the ladder (which has one)
   legitimately differ.
6. **Not an ADR premise but a GATE premise, and the more interesting
   one.** `ehrt.docs-tooling.oracle-coverage-test` demanded the
   committed kind claim be a PROPER subset of the closed vocabulary —
   *"if every closed kind were witnessed there would be no vacuous set
   to name, and L1-2's finding would be void"*. Step 0 made the honest
   claim 28 of 28, and **the gate reddened on the truth**:

   ```
   FAIL in (the-committed-coverage-claim-is-populated-and-drawn-from-the-closed-vocabulary-test)
   the claim is a PROPER subset -- coverage is thin, and saying so is the point
   expected: (< (count kinds) (count closed))
     actual: (not (< 28 28))
   ```

   L1-2's point was never "coverage must stay thin"; it was "nothing
   may be stated that no root can move", whose twin hazard is a claim of
   TOTAL coverage nobody looked at. The assertion now requires subset,
   and requires a TOTAL claim to state its own ratio in the digest's
   prose, so a reader meets "28 of 28" written down. The measurement
   that the claim is TRUE stays where it always was, in
   `ehrt.integration.oracle-coverage-test`, against a fresh digest.

## Findings

1. **`control-id-for` IS NOT INJECTIVE OVER `:result-available`, and it
   is live in two shipped corpora.** Two results for one patient at one
   second mint the same MSH-10 — the id is `mrn-R01-t` with nothing to
   separate them, the same shape `:bed-status-change`'s own arm of
   `control-id-for` was widened to fix in arc 3b sweep 2. Measured:
   `seed-424242-clinic-decade` carries 6 duplicate MSH-10s in 2 groups
   (`MRN000189-R01-119086260` appears SIX times, `...-125739060` twice),
   and the clinic-decade demo carries 1. Every other corpus is fully
   distinct. **PRE-EXISTING, and proved so rather than asserted:** those
   corpora place no order, so the ladder produces nothing in them, and
   the with/without render is byte-identical with the same 6 duplicates
   on both sides. The colliding messages are `:observation` /
   `:diagnostic-report` ORUs, which share the default control-id branch.
   NOT FIXED HERE — widening the key moves every existing corpus's
   bytes and is a declared sweep of its own — and rowed rather than
   silently carried. It is also why this sweep keys `:final` on log
   INDICES: a ladder keyed on that id would put terminal codes on the
   wrong twin.
2. **Two orphaned JVMs from a foreground timeout stole two thirds of
   the machine.** A `timeout 120` wrapper around `clojure -M:poly test`
   killed the wrapper, not the JVM; two survived and ran at ~130% CPU
   each beside the real suite, which is ADR-0167's own suite-time
   doubling in miniature. Found by `ps`, killed BY PID (never `pkill
   -f`, which self-matches the harness shell —
   `feedback_process_matching_self_match`).
3. **`poly test` aborts at the first failing brick, alphabetically.** A
   stale `.agents/state-derived.md` in `docs-tooling` stopped the run
   before `sim` and `sim-emit-hl7` ever loaded, so two rounds of "the
   ladder tests passed" were actually "the ladder tests never ran".
   Regenerating state-derived BEFORE the suite is the ordering that
   avoids it.
4. **A new test namespace's execution was proven, not assumed.** A
   deliberate failing probe planted in
   `ehrt.conformance.ladder-sampling-test` — `(= 999999 (count
   messages))` — came back `actual: (not (= 999999 104))` as the ONLY
   failure in the whole suite, which simultaneously proves the
   namespace runs and that everything else was green
   (`project_poly_composition_gates_test_execution`).
5. **An early unit fixture broke `emit-wire`'s identity property by
   being out of time order.** The hand-built log had an `:observation`
   at t=6000 between results at 5200 and 6200; ground truth is
   `:t`-nondecreasing by the engine's own priority-queue invariant, and
   `emit-wire`'s absent-offsets identity rests on the stable tie-break
   over a sorted log. Six byte-identity assertions failed for that
   reason alone. The fixture now says so in its own docstring.

## Re-pinned

`demos/scenarios/ed-tuesday/README.md`'s headline message count (1,447
→ 1,497), its PV1 arithmetic (631 → 681, with the reason: a rung IS
rendered by the builder it restates, PV1 included), both ticker
excerpts' `:emitted`, and the batch section's closing summary;
`components/docs-tooling/resources/docs-tooling/hand-owned-assets.edn`'s
`straddle-timeline.svg` row, bumped to `11664e0` in that commit's own
successor because the tripwire reads `git log -1` on its source and
cannot see an uncommitted edit; both oracle-coverage gates' root counts
(40 → 41, 37 → 38 engine-layer) and their docstrings;
`.agents/state-derived.md`, regenerated.

**DID NOT MOVE, and each is a witness rather than an absence:**
`:snapshot-count` is still 574; the hourly BUCKET COUNT is still 615,
and `bin/demo-exerciser-ed-tuesday`'s 615-batch assertions passed
untouched; the straddle's own batch-000/001/002 counts are still 9, 14
and 16, so `straddle-timeline.svg` needed re-witnessing but no redraw;
`docs/manual/05-batch-delivery.md` needed nothing; clinic-decade's
README needed nothing, because its corpus places no order. A rung sits
STRICTLY inside an order-to-result interval and this scenario's
turnarounds are 30–120 minutes, so rungs thicken hours that already
carried their own order and result — they cannot open new ones. That is
the exact opposite of chatter's 186 → 615 move, and it falls straight
out of what a rung is.

## Gates, and the close

* `make test` — **`MAKE_EXIT=0`**, on the tree that carries this record.
  An earlier run reddened at `MAKE_EXIT=2` on `state_derived_test`
  alone, because adding this record and its prompt archive moves two
  generated INDEXes and the derived register; `make state-derived`
  regenerates them and is what this session's final run precedes.
* `make traces` — **`TRACES_EXIT=0`**, and **NO TRACE MOVED**: every
  `demos/traces/*/config.edn` runs its own config and not one of them
  names `:ladders`, including `order-result`, the trace that places an
  order. The only untracked files after it were this record and its
  prompt.
* `make integration` — **`INT_EXIT=0`**, on a clean tree at `c602972`:
  `projects/integration`'s own suite (the nightly tier, where the fresh
  41-root oracle-coverage digest runs) plus both demo exercisers, tree
  clean afterwards.
* `clojure -M:poly check` green throughout.
* `bin/demo-exerciser-ed-tuesday` — every assertion green, including
  the 615-batch listing and the straddle; its one FAIL was the ADR-0005
  tree-clean postcondition, on the sweep's own uncommitted work,
  disclosed rather than filtered.
* `bin/post-push-verify d0e7feb c602972` — remote tip matches HEAD,
  every commit message in range pure ASCII, CI run reported once
  (in_progress at the time, DISCLOSED per AR-CI-4 and awaited below).
* **CI run 33189524366, `conclusion=success` at `c602972`** — the close
  marker (`rulings.md#R-session-verifies-ci-via-gh`). No tag paid.

The RED-FIRST commits this sweep carried, all pushed with their green
successors and never alone (`rulings.md#R-red-pushed-with-green`):
`11664e0` was red on `hand-owned-asset-freshness-test` until `9c57280`,
which is the tripwire's own design — it reads `git log -1` on its
source and cannot see an uncommitted edit, so no local run can catch it
before the commit that causes it.

A NOTE ON WHAT THE SWEEP DID NOT DO. ADR-0175's fences held: no ground
truth change of any kind (bracket-enforced at all three commits), no
new event kind, no schema bump, no SIU, no NK1, no fan-out, no MLLP.
`engine/config-keys` is untouched and `:ladders` provably cannot reach
it. `person-simulator` limitations rows 5 and 8 stand.

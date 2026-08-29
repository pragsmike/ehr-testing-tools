# Arc 4 sweep 4 — SIU (ADR-0175 ruling B1)

2026-08-28. Base `a0eadb0`; four commits; this record and the CI marker
follow them. Ceremony: R30 (commit and push at each checkpoint), taken
from the session prompt.

`bin/preflight` ran first, **exit 0, no findings**: the last five CI
runs on main all green; repo root not under `/mnt/`; `core.fileMode`
true and `core.ignorecase` unset; working tree clean including
untracked; local HEAD matched `origin/main`; HEAD not tagged `stable-*`
— disclosed and correct, no tag is paid.

## What landed

| sha | commit |
|---|---|
| `fc559ee` | row the ORU control-id collision sweep 3 found (step 0, docs-only) |
| `83929d7` | SIU^S12/S14/S15/S26 — scheduling on the wire, DARK |
| `7eb94a1` | SIU TURNED ON in six corpora and the `scheduling` root |
| `2f43b66` | redraw the straddle timeline at the SIU turn-on |

## The trigger-mapping verification, and exactly what the jar settles

The prompt asked for this against the jar rather than the comment, and
the answer has two halves.

**WHAT THE JAR SETTLES.**
`hapi-structures-v24` 2.6.0 carries
`ca/uhn/hl7v2/parser/eventmap/2.4.properties`, which maps `SIU_S13`
through `SIU_S24` and `SIU_S26` onto the structure `SIU_S12`. So S12
(the structure itself), S14, S15 and S26 are all real v2.4 trigger
events resolving to one structure. Reflected from the same jar rather
than read from a table: `SIU_S12`'s segment names are `[MSH SCH NTE
PATIENT RESOURCES]`, its `PATIENT` group is `[PID PD1 PV1 PV2 OBX DG1]`,
`SCH` has 27 fields, SCH-1/SCH-2 are `EI`, SCH-7 and SCH-25 are `CE`,
and SCH-11 is `TQ` whose 4th component is a `TS`.

**WHAT THE JAR CANNOT SETTLE, measured rather than assumed.** Neither
`hapi-base` nor `hapi-structures-v24` carries HL7 Table 0003 or any
trigger-event DESCRIPTION at all — `unzip -p` piped through `strings`
over both jars finds neither `rescheduling` nor `did not show`
anywhere. The eventmap is a structure map, not a semantic one. **So the
jar cannot adjudicate S13 vs S14 for a reschedule notification, and the
prompt's "the jar wins" could not be executed as written.**

**WHAT DECIDED IT INSTEAD.** The EVENT CONTRACT. `event_schema.clj`'s
own `:reschedule` doc says `SIU^S14` at contract 1.7.0; so do the
generated `event-schema.edn`, the frozen `event-schema-baseline.edn`,
`docs/formats.md`, `components/sim/docs/operational-models.md` and
`emit_hl7.clj`'s own registry comment. **`notes/adr/0174-*.md`:697
enumerates "S12/S13/S15/S26" and is the lone surface that disagrees.**
It is a census sentence, not the contract, and moving the contract to
match it would be a schema diff this sweep's fences forbid. **S14
stands**, the disagreement is recorded in the registry comment and
pinned by a test, and a later session that wants S13 (which is what HL7
Table 0003 calls "rescheduling", where S14 is "modification") takes it
as a deliberate contract change rather than a quiet edit.

## The brackets

**Step 1, the mechanism, DARK** (`fc559ee` → `83929d7`). `digest.clj`
untouched, so no declaration is owed and none was passed:

```
bin/ground-truth-bracket fc559ee 83929d7
  soundness: IDENTICAL outside the leading docstring -- proceeding
  IDENTICAL: every digested root's :ground-truth matches (38 roots)   GTB_EXIT=0

bin/regression-oracle fc559ee 83929d7
  IDENTICAL: every root's digest matches                              RO_EXIT=0
```

**That is the strongest line a sweep can produce**, and it is the shape
sweep 3 named: a new message FAMILY, a new segment, a new config
schema, a new site-profile code table, a fold-path change in
`v2-replay`, four registry entries — and not one byte of any of the 41
roots' output moved, because nothing in the tree names `:siu` yet.

**Step 2, the turn-on** (`83929d7` → `2f43b66`). `digest.clj` moves, so
both scripts take `--declared-digest-change`:

```
bin/ground-truth-bracket 83929d7 2f43b66 --declared-digest-change
  IDENTICAL: every digested root's :ground-truth matches (38 roots)   GTB_EXIT=0

bin/regression-oracle 83929d7 2f43b66 --declared-digest-change
  DIFFERS -- ONE line changed, ZERO added, ZERO removed:
    -88cd64df...  scheduling.edn
    +893dcc73...  scheduling.edn
                                                                      RO_EXIT=1
```

**The mover set is exactly the opted-in root.** `scheduling` is the
only root that turns `:scheduling` on, so it is the only root with an
appointment to render; the other 40 are IDENTICAL by construction and
are so measured. The GROUND-TRUTH half of that same root did not move,
which is the pair of lines arc 4 exists to be able to print.

## The witness table

Counted before anything was opted in, because `pos?` may only be
asserted where the population exists:

| corpus | events | msgs | appt | S12 | S14 | S15 | S26 | chains | PV1 |
|---|---|---|---|---|---|---|---|---|---|
| seed-202-ed-tuesday | 1,213 | 1,387 | 64 | 50 | 6 | 4 | 4 | 6 | 0 |
| seed-424242-clinic-decade | 1,774 | 1,938 | 56 | 42 | 3 | 5 | 6 | 3 | 0 |
| seed-5-clinic-decade | 1,412 | 1,495 | 40 | 27 | 3 | 7 | 3 | 3 | 0 |
| adhd-seed-45 | 97 | 106 | 3 | 2 | 0 | 0 | 1 | 0 | 0 |
| ed-tuesday (demo) | 1,269 | 1,554 | 57 | 44 | 5 | 3 | 5 | 5 | 0 |
| ed-tuesday-latency (demo) | 1,269 | 1,554 | 57 | 44 | 5 | 3 | 5 | 5 | 0 |
| clinic-decade (demo) | 1,569 | 1,688 | 59 | 40 | 6 | 9 | 4 | 6 | 0 |

`chains` = appointments whose family carries a reschedule, all of them
sharing ONE filler id across S12 → S14 → terminal. `appt` = the count of
scheduling-family ground-truth events; it equals the SIU message count
in every row, one message per event.

**The events column is unchanged in every row.** Read it against the
messages column: that is what an emission add-on is. Ground truth and
non-SIU bytes were asserted equal with and without `:siu` in the same
pass that produced this table, per corpus, not only by the bracket.

**EVERY CORPUS HAS APPOINTMENTS.** Unlike the ladder — inert in three of
the seven — there is no zero-population row here and no byte-identical
pair. `adhd-seed-45` is the thin one and is disclosed rather than waved
through: three appointments over ten ADHD patients, drawing no
reschedule and no cancel at this seed, so `pos?` on S14 or S15 may not
be asserted there. The other five carry all four triggers.

**Sampled strata**, over a 40-patient composite corpus in
`ehrt.conformance.siu-gate-test`, printed by the gate itself:

```
SIU^S12      n=58     gated=58     full (skeleton)
SIU^S14      n=13     gated=13     full (skeleton)
SIU^S15      n=8      gated=8      full (skeleton)
SIU^S26      n=9      gated=9      full (skeleton)
```

Gated in FULL with no code change in `judge` or `cli`:
`skeleton-message-types` is DERIVED from `message-type-registry`, so
the four families became skeleton the instant they got entries. The
same run's chatter strata ARE capped at 5, so both halves of design
(h)'s policy are exercised rather than one.

**Judge tier**, same corpus, through `#'hapi/new-context` — the private
constructor `gate v2` itself calls:

```
resolved v2.4 structures over 88 SIU messages:
  {["SIU^S12" "SIU_S12"] 58, ["SIU^S14" "SIU_S12"] 13,
   ["SIU^S15" "SIU_S12"] 8,  ["SIU^S26" "SIU_S12"] 9}
```

Zero parse failures, zero `GenericMessage`, all four triggers onto the
one structure `2.4.properties` maps them to. Sweep 1's flip is re-earned
rather than inherited: SCH's `EI`/`CE`/`TQ` fields are as subject to
v2.4's primitive rules as PID-13 was, and PID-13 is what killed 346 of
the probe corpus's 747 before sweep 1 rendered it `(NNN)NNN-NNNN`.

## The builder-seam choice

**A sibling builder, `siu-message`, not a branch in
`single-subject-message`** — and the prompt was right that A20's reason
does not apply. A20 had no patient at all; SIU has one, so the PID/PV1
pair that builder's contract is built around does exist here.

It still cannot share it, on three structural counts, any one of which
would force a branch:

* SIU carries **no EVN**, and `single-subject-message` renders one
  unconditionally.
* Its **SCH sits BEFORE the PID**, and that builder has no seam ahead of
  the patient — every extension point it has is after PV1.
* Its **PV1 is CONDITIONAL**, and that builder always renders one.

Three branches inside a builder whose own docstring says "a PID/PV1 pair
per subject" is a different builder wearing the first one's name.

## The two measurements that were not predicted

**1. THE PV1 BRANCH IS UNREACHABLE FROM ANY RUN IN THIS REPOSITORY, and
it is structural rather than seeded.** The prompt asked for "PV1 only
when an encounter is open" and for an assertion that a pre-arrival S12
has none. The rule is right; what the measurement found is that the
other half never happens. `:encounter-id` is on **zero** of the
`scheduling` oracle root's 72 appointment-family events, zero of
seed-202-ed-tuesday's 64, zero of seed-424242-clinic-decade's 56, and
zero across all seven corpora in the table above.

The cause is `decide` ordering, not luck. Both of this project's booking
producers decide OUTSIDE an open encounter: the pre-loop books an
arrival before that patient has any encounter at all, and a follow-up is
a step `decide :discharge` PREPENDS, so its own `decide :appointment`
runs after that discharge has already closed the encounter —
`stamp-encounter` reads the world BEFORE the batch, and by then there is
none open.

The branch is KEPT and exercised by a hand-built event, which is the
honest form for a rule that is right and currently unreachable; the
alternative, rendering PV1 unconditionally, would put a visit segment on
a notification about a visit that has not happened. **The population
fact is itself gated** — no SIU of any real run carries a PV1, with the
measurement as its stated reason — so the day the engine starts stamping
a mid-stay booking, a gate says so instead of the rendering changing
silently.

**2. SIU OPENS NEW HOURS WHERE THE LADDER OPENED NONE.** ed-tuesday's
hourly bucket count moved **615 → 620** and its PV1 count moved **not at
all** (681 before and after, on 57 more messages). Both are the exact
inverse of sweep 3's move one commit earlier, where the bucket count was
unchanged and the PV1 count rose with the message count. One cause for
both: a rung's instant is DERIVED from an order-to-result interval that
already exists, so it is trapped inside an occupied hour and is rendered
by the builder it restates, PV1 included. An appointment's instant is
derived from nothing on the wire — a booking days or months before the
visit, a no-show at an instant the patient never reached, a reschedule
at whatever hour the scheduler was open — and it names no visit, so it
carries no PV1 and it can open hours nothing else did.

## ADR premises contradicted

* **ADR-0174 §2(d):697** — "So S12/S13/S15/S26 are real v2.4 trigger
  events". S13 is real, but it is not what this project's own event
  contract names for `:reschedule`, and six other surfaces say S14. The
  contract wins; recorded in the registry and pinned by a test.
* **ADR-0175 §1(iii)** — "ANY LATER SWEEP ADDING AN SIU ENTRY OWES a
  `v2-replay/evolve-entry` arm with it". It owes a FOLD ARM, and the arm
  it owes is not an `evolve-entry` one. Routing an SIU through that
  `case` would bootstrap an accumulator entry from its PID
  (`fold-message`'s never-yet-seen-mrn rule), so a booking made weeks
  before a patient arrives would enter the reconstruction as `:new`
  while the true side has no record of them. The skip is by message-TYPE,
  before dispatch. The throw still fires for every family that belongs.
* **`message-type-registry`'s own comment** — "an entry here is a claim
  that one ground-truth event renders one message". These four are the
  first entries for which that is conditional: they render when `:siu`
  is on. The gate lives in `event->messages`, not the registry, because
  three other readers of that map want the families present
  unconditionally.

## The gates, with their own exit codes

Run on a clean tree at `246fd66`, each to a full log with the exit code
captured explicitly:

```
make test          23,463 passes, 0 failures, 0 errors   MAKE_TEST_EXIT=0
make integration                                          MAKE_INTEGRATION_EXIT=0
```

`make test` was run TWICE before it was green, and both reds were real:
the first stopped at `chatter_test`'s whole-registry pin, which `poly
test`'s abort-at-the-first-failing-brick had hidden behind
`ladders_test`'s. `make integration` is where
`ehrt.integration.oracle-coverage-test` checks the committed
`witnessed-message-types` against a FRESH 41-root digest, which is what
makes this sweep's four new MSH-9s a measured claim rather than an
edited set.

CI at the pushed tip: run **33225246683**, conclusion **success**, head
`246fd66`. The dark half's own run, **33221323253** at `83929d7`, also
concluded success. No tag is paid (de-scaffold ruling, 2026-08-25); CI
green at the tip is the close marker.

## Findings

1. **`docs/formats.md` now carries a sentence that has become false, and
   it is NOT fixed here.** Each of the four kinds' own `:doc` still
   reads "deliberately unrendered in 1.7.0", which was true of the
   emitter until this sweep. The remedy is a four-line `:doc` edit in
   `event_schema.clj` plus `make event-schema-export formats-event-log`;
   `classify-change` reports zero for a doc-only edit and no version
   bump is owed. It is left undone because it moves a contract artifact
   and this sweep's fences forbid a schema diff — two defensible
   readings, so STOP-AND-REPORT on that item alone rather than on the
   sweep. **AUTHOR ACTION.**
2. **THREE separate whole-registry pins reddened, in three different
   namespaces**, and only one was predictable from the diff:
   `ladders_test`, `chatter_test` and `event_conformance_test` each pin
   the registry's key set or its complement. The third was found only by
   `make test` — `poly test` aborts at the first failing brick, so the
   first run reported one failure and hid the second. Every red was
   captured before its fix.
3. **`clojure -M:poly check` passes over a source file that will not
   COMPILE.** An unescaped `"` inside a docstring I added to
   `identifiers.clj` silently terminated the string and left a malformed
   `defn-`; `check` was green and `make traces` caught it. A reminder
   that `check` is a dependency-graph gate, not a compiler.
4. **`sim identifiers` becomes a SUPERSET for these four kinds and is
   left one deliberately.** `control-id-for` keys on the registry, so a
   run with `:scheduling` and without `:siu` now inventories ids that
   reach no wire. Kept: an id listed that was never emitted costs a
   fruitless search, an id emitted and not listed is the failure that
   matters, and `identifiers-test`'s property is a subset assertion for
   exactly that reason. Documented at the function.
5. **`bin/demo-exerciser-*`'s tree-clean postcondition makes them
   unrunnable against uncommitted work.** Both exercisers passed every
   content assertion and failed only that postcondition until the
   turn-on was committed. Not a defect — worth knowing before reading
   an exerciser's exit code mid-session.

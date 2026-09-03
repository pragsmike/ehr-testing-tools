# 2026-09-03 — B2/B1: the stale-hold invariant, and the outpatient rule scoped to its visit

**Ceremony:** R30 standing default, with the prompt's own sequencing:
commits at the named steps, one push at step 8, CI verified with
`gh run view`, close-marker commit after.
**Fences:** no change under `components/sim-engine/src` or
`components/sim/src`; no re-pin (R-catalog-pin: a pinned corpus B2
convicts is a STOP); no A1 (R-fork: engine fix is a later session with
its own ADR). Independent judge: no engine predicate reuse — in
particular `ehrt.sim-engine.log-index/subject-superseded?` and
`ehrt.sim-engine.fold/update-beds` are read as evidence and never
called.
**Driving prompt:** [`.agents/prompts/2026-09-03-b2-b1-stale-hold.md`](../prompts/2026-09-03-b2-b1-stale-hold.md).
**Predecessor:** [`2026-09-02-downstream-self-check-failed.md`](2026-09-02-downstream-self-check-failed.md)
(the STOP record whose fork R-fork resolved as (C): B2 now, A1 later,
B1 riding on B2).

## Headline

**The oracle hole is closed and the accident is scoped to its
docstring.** `non-admitted-patients-hold-no-bed` (B2) now convicts the
stale hold directly: the calibration config exits 2 at BOTH 1,984 and
2,000 arrivals, each conviction starting at its own reinstatement
instant — the formerly certified-clean 1,984 run included.
`outpatient-patients-occupy-no-bed` (B1) convicts only while the visit
is open: the 2,000-arrival run's 30,507 accidental rows are now 2. The
engine is untouched (fence held: nothing under
`components/sim-engine/src` or `components/sim/src` moved); A1 is
`roadmap.md#cancel-transfer-reinstates-a-new-subject`, PRIORITY 2. Both
oracle scripts report IDENTICAL vs `526d262`.

## What landed

| commit | contents |
|---|---|
| `946be99` | step 2 — RED: six tests (B2 trio, B1 scope, two twin agreements) |
| `630b2ce` | step 3 — GREEN: B2 + B1 scoping, naive twins, all-seven harness |
| `4f99f98` | step 5 — catalog pin 44→45, docs; no declaration widened |
| *(this)* | step 7 — record, prompt archive, roadmap row, regenerated indexes |

## Step 1 — the derivation, before code

### B2: which statuses may hold a `:location`

The catalog's two location-affirming rows are the frame:

- `admitted-occupies-one-slot` (`check.clj:439`, predicate
  `one-slot-offender?` at `:432`): a `:status :admitted` patient whose
  `:class` is not `:outpatient` must hold a `:location` with a bed.
- `expired-patient-retains-location` (`check.clj:498`): an `:expired`
  patient retains their `:location` — "the body stays wherever it was".

B2 is the complement: **any status outside the may-hold set, holding a
non-nil `:location`, is a violation.** The status vocabulary is closed —
`state.clj:266` enumerates `:new :admitted :discharged :merged
:expired`, and `state.clj:410` seeds every patient `:status :new` — so
the set can be derived writer by writer from `evolve` (read as
evidence; the implementation reads only the log through
`engine/replay`, per the independent-judge note above check.clj's
bed-cycle rows, `check.clj:514-528`).

**May hold a `:location` — three statuses, each with its cited writer:**

| status | status writer | how the location is legally there |
|---|---|---|
| `:admitted` | `evolve :admission` (`evolve.clj:213`), `evolve :cancel-discharge` (`:279`), `evolve :outpatient-visit` (`:341`) | written by `evolve :admission` (`:216`), `evolve :transfer` (`:224`), `evolve :bed-swap` (`:288`), `evolve :cancel-discharge` (`:279`), and `evolve :cancel-transfer` (`:261`) while admitted |
| `:expired` | `evolve :discharge`, expired arm (`evolve.clj:244-245`) | the expired arm deliberately does NOT touch `:location` (its own comment, `:234-242`): the body retains the bed the last admitted-era writer gave it. `expired-patient-retains-location` asserts exactly this retention |
| `:merged` | `evolve :merge`, merged arm (`evolve.clj:295`) | the merged arm's `assoc` touches only `:status`, leaving `:location` standing — and a bed-holder is a LEGAL merge target: `decide :merge`'s `never-mergeable?` excludes only `:new` and `:merged` (`decide.clj:1390`), so an `:admitted` (or `:expired`) patient holding a bed can be absorbed; churn's applicability oracle inserts `:merge` only at `:admitted?` gaps at all (`churn.clj:145`). The absorbed record retains the bed exactly the way `:expired` does |

The `:merged` row is the derivation's one finding beyond the prompt's
own frame: the complement of the TWO named invariants alone would
convict a legal log. The catalog today neither affirms nor convicts
merged retention; B2 must exempt it or go red on any corpus whose churn
merge happens to absorb a bed-holder. (Probed empirically too: the
seed-18/60-patient churn fixture run from
`the-mutations-actually-make-all-six-invariants-fire` folds to 3
`:merged` patients, all location-free — the exemption is licensed by
`decide :merge`'s own eligibility, not witnessed in that particular
corpus.)

**Must NOT hold a `:location` — the complement B2 convicts:**

- `:new` — seeded location-free (`state.clj:410`); `evolve
  :cancel-admit` writes `:status :new` AND dissocs `:location` in the
  same form (`evolve.clj:255-256`). No evolve arm writes a location
  onto a `:new` patient. The ONE path that can is the status-blind
  reinstatement `evolve :cancel-transfer` (`:261`) — engine-guarded for
  `:discharged`/`:expired`/`:merged` subjects but deliberately NOT for
  `:new` (`log_index.clj:201-223`, the TS-5 close's named adjacent
  case). That is the downstream witness's exact mechanism, and it is
  judged here, not called.
- `:discharged` — `evolve :discharge`'s non-expired arm writes
  `:location nil` in the same `assoc` that writes the status
  (`evolve.clj:247`); `evolve :outpatient-visit-end` (`:345-349`)
  writes `:discharged` without touching `:location`, whose nil-ness
  during the visit is B1's own row. A `:discharged` bed-holder is
  engine-blocked today (TS-5), but the judge judges the log.
- a patient with no `:status` at all is unreachable through the engine
  (`state.clj:410`); on a hand-authored log B2 convicts it — absence
  of status is absence of entitlement, the conservative complement
  reading.

**B2's predicate**, over each replay record's `world-after`:

```clojure
(and (some? location)
     (not (contains? #{:admitted :expired :merged} status)))
```

Name: **`non-admitted-patients-hold-no-bed`** (the two absorbing
terminals that retain — `:expired`, `:merged` — are carved out in the
docstring as the documented retention cases, exactly as
`admitted-occupies-one-slot` carves out `:class :outpatient`).
Registered adjacent to its converse pair: after
`expired-patient-retains-location` in source and in the catalog vector
— the reporting-order placement argument ADR-0166's twin span made.

Every function the implementation reads is a log-reading function of
`check.clj` itself: `fold-records`/`engine/replay` (`check.clj:394`),
`participants-of` (`:357`), `reflag` (`:384`), and the emission
convention of `admitted-occupies-one-slot` (`:439`) it mirrors.

### B1: "visit open", from the encounter vocabulary

`check.clj:67-76` already names the vocabulary: `encounter-openers`
`#{:admission :outpatient-visit}`, `encounter-closers` `#{:discharge
:outpatient-visit-end}` (`:cancel-admit` marks, never closes). B1's
scope is: **a patient is in-visit from an `:outpatient-visit` opener
until the first closer that names them.** The fold carries the set of
in-visit patients beside the existing offender flags; both the flag
update and the emission's `world-after` scan gain the in-visit
condition. The STOP record's B1 ground: 30,505 of 30,507 rows were
stamped after `:outpatient-visit-end` had closed the visit, against a
`:discharged` patient — rows about a stale hold, not about the visit
the docstring scopes the rule to. Those records become B2's: at the
`:outpatient-visit-end` record itself `world-after` is already
`:discharged` + held location, so B2 takes over at the exact record B1
lets go — no gap, no overlap.

**Gate check (step 1): every status in the may-hold set has a cited
writer** — `:admitted` (`evolve.clj:213/216/224/279/288/261`),
`:expired` (`evolve.clj:244-245`), `:merged` (`evolve.clj:295` +
`decide.clj:1390` + `churn.clj:145`). Done, no commit.

## Step 2 — RED (`946be99`)

Six red, zero errors, everything else green (the `sim-check` brick run
stopped at the first failing project; its `conformance` pass was fully
green at 90 assertions and `development`'s `check_test` read 131
passes / 6 failures): the B2 trio (convicts the reinstated hold at the
reinstatement's `:t`; silent for `:expired`; silent for a merged
bed-holder — the derivation's own third-status finding, tested), B1's
post-visit-end scope test, and the two naive-twin agreement fixtures.
The B2 tests resolved the invariant's var dynamically in this commit so
the namespace still loaded while `check` did not define it — a direct
call would have compile-failed the whole namespace and taken every
green test down with it; the GREEN commit rewrote them to direct calls.
Both population-scale defspecs stayed green with the scoped naive twin
already in place, confirming the step-2 analysis that the five existing
mutators never produce a post-visit-end outpatient hold.

## Step 3 — GREEN (`630b2ce`)

`non-admitted-patients-hold-no-bed` lands as a `fold-records` +
`reflag` implementation mirroring `admitted-occupies-one-slot`'s own
emission convention, over the predicate
`(and (some? location) (not (statuses-entitled-to-a-location status)))`;
registered in `catalog` directly after `expired-patient-retains-location`
with the reporting-order placement comment. B1 carries the open-visit
set through the fold beside its flags: scope-in on `:outpatient-visit`,
scope-out on any other encounter boundary, both the flag update and the
emission scan gated on it. Harness: `all-six` → `all-seven`,
`stale-holds` mutator appended last in `mutate` (reinstates an
uncancelled transfer's location onto up to three finally-`:discharged`
patients, final-log indices so no reference dangles), mechanism check
renamed `the-mutations-actually-make-all-seven-invariants-fire` with a
B2 row, and the two session fixtures joined `small-mutated-fixtures`.
Gate: sim-check brick green in all three projects (conformance 90,
development 140, integration 56 assertions; 0 failures, 0 errors).

## Step 4 — the witness, at a real shell

Fixture config (`test-fixtures/downstream-calibration/config.edn`),
seed 424242, `--reference-date 2026-08-31 --churn --format
ground-truth`, at tip = `630b2ce`:

| run | exit | payload |
|---|---|---|
| `--patients 1984` | **2** | 39,684 × `non-admitted-patients-hold-no-bed`, 0 × the outpatient rule, one patient `PID-001086-869d73e0`, `:at` 1,968,180 … 632,192,091 |
| `--patients 2000` | **2** | 34,600 × `non-admitted-patients-hold-no-bed` (`:at` 3,017,040 … 631,353,025) + **2** × `outpatient-patients-occupy-no-bed` (`:at` 4,572,240 and 4,573,080) |
| ed-tuesday (`--seed 202 --patients 100 --churn --config demos/scenarios/ed-tuesday/config.edn`) | **0** | 424,772 bytes of clean corpus |

Every number lines up with the STOP record's mechanism: 1984's
conviction now begins at its reinstatement batch instant (1,968,180 —
the formerly *certified clean* run is the one B2 exists to convict);
2000's begins at ITS reinstatement instant (3,017,040) rather than at
the visit 1,555,200 s later; and the old invariant's 30,507 rows shrink
to the 2 records actually inside the visit window
[4,572,240, 4,573,440). B2 goes silent exactly while the visit holds
status `:admitted` and resumes at the closer — the no-gap-no-overlap
handoff, witnessed. Gate (three exit codes): held. No commit.

## Step 5 — pins and declarations (`4f99f98`)

- **`arc0-invariant-catalog` re-pinned 44 → 45** per R-catalog-pin: the
  new name is an ADDITION with a dated citation, inserted directly
  after `expired-patient-retains-location` (where `catalog` itself puts
  it); nothing renamed or removed. The findings gate stays a full-value
  `=`: `:status` `:ok` on all four gated corpora, `:events` unchanged
  (1,131 / 1,660 / 1,342 / 92). **No pinned corpus is convicted — no
  STOP.** The corpus that IS convicted is
  `test-fixtures/downstream-calibration/`, deliberately unpinned, and
  the re-pin note says so.
- **Mutation conviction declarations: none widened, disclosed as
  none.** The corpus brick ran green unchanged — `event_mutate_test`'s
  Q5(a) equality (`actual findings = :expected-findings`, per operator,
  at its sampled sites) holds for every registered event operator, so
  no operator's set gains B2. This matches the step-1 analysis: the
  referential/structural operators edit reference fields, drop a
  registration, or re-attribute clinical content — none of them writes
  a `:location` onto a non-admitted subject.
- **`docs/consuming-ground-truth.md`**: the certifies list gains
  `non-admitted-patients-hold-no-bed` beside
  `expired-patient-retains-location`, and every "44" is now "45" (the
  run count, the four config-needing rows, the seven vacuous rows, and
  the not-warranted scope sentence). The "known-open behaviours"
  bullets were checked and none describes the stale-hold shape, so no
  erratum rides along.
- **Gate: full `make test` green over the whole tree** (exit 0, zero
  failures across all 828 result lines). One intermediate full run had
  exactly two reds — `state_derived_test`'s index-freshness pair, fired
  by this session's own record and prompt-archive files existing before
  `make state-derived` ran. That is the index-completeness gate doing
  its job mid-session, not a step-5 defect; regenerated, re-run, and
  the final full run is the green one.

## Step 6 — the oracle, both halves

`bin/regression-oracle 526d262 4f99f98`: **IDENTICAL — every root's
digest matches** (its own closing line, exit 0; soundness check on
digest.clj passed with no declared change). `bin/ground-truth-bracket
526d262 4f99f98`: **IDENTICAL across all 38 roots' `:ground-truth`**
(exit 0; not a regression-oracle claim, per its own banner and
`rulings.md#R-oracle-script-contract`). No root lost its
`:ground-truth` key — the golden runs carry no churn-reinstatement
shape, so B2 changes no self-check verdict there. No payload moved,
exactly as a judge-only session must measure.

## Step 7 — roadmap row, under measured headroom

The STOP record's proposed row lands as
`roadmap.md#cancel-transfer-reinstates-a-new-subject`, PRIORITY 2 under
`## Next`, updated for R-fork: the catalog half (B2 + B1) is landed and
cited to this record; WHAT REMAINS is A1 alone (admit `:new` to
`log-index/statuses-that-supersede-a-reinstatement`), draw-affecting,
owing its own ADR and declared sweep, flipping the calibration fixture
back to exit 0 when it lands. **Headroom, measured, not assumed**:
`:onboarding` stood at 1,486/1,530 (44 lines) before the row; with the
row and the session's files indexed, `make state-derived` reports
1,504/1,530 — 26 lines of headroom remaining, so the row is covered and
added rather than reported.

## Judgment calls, and their ratification status

1. **`:merged` joined the may-hold set.** The prompt frames B2 as the
   complement of TWO invariants, whose complement alone would convict
   `:merged` bed-holders — and `decide :merge`'s own eligibility
   (`never-mergeable?` excludes only `:new`/`:merged`,
   `decide.clj:1390`) makes an admitted bed-holder a legal merge
   target, so that reading convicts legal logs (the repo's own
   `legit-merge-log` fixture among them). The step-1 gate ("name each
   status that may hold a `:location`, citing the writer") is what
   surfaced it; the exemption is cited, tested
   (`non-admitted-patients-hold-no-bed-stays-silent-for-a-merged-bed-holder`),
   and stated in `statuses-entitled-to-a-location`'s docstring.
   Unratified as a ruling; ratified in effect by every gate that would
   otherwise be red.
2. **The RED commit resolved the invariant's var dynamically** so the
   test namespace could load while `check` did not define it — a direct
   call would have compile-failed the namespace and taken every green
   test down, violating the "all else green" gate. Rewritten to direct
   calls in GREEN; disclosed in both commit messages.
3. **The RED set was six deftests, not four.** The prompt names four
   red items; the merged-silence test (call 1's own witness) and the
   split of "naive twins for both" into two agreement deftests make six.
   All six red at `946be99`, everything else green.
4. **"Update the naive twins" was read as the full ADR-0169
   treatment**: `all-seven`, B2 in both population-scale comparisons, a
   `stale-holds` mutator so the new pair is non-vacuous (L1-4's own
   critique — a twin nothing exercises proves nothing), the mechanism
   check renamed `...-all-seven-...` (old name cited only in frozen
   history), and the session's two fixtures joined
   `small-mutated-fixtures`. Unratified as scope; each piece follows a
   standing convention.
5. **One empirical probe ran outside the tree** (a REPL script folding
   the seed-18 fixture run) to check whether merged bed-holders occur
   in that corpus before ruling the exemption — read-only, no `src`
   touched, same spirit as the STOP session's ratified probe pattern.
6. **Sequencing disclosure:** the first full `make test` ran with the
   session's record/prompt files already in the tree and went red on
   exactly the two index-freshness assertions; `make state-derived` and
   the final full run (green, exit 0) are what the step-5 gate names.

## Findings

1. **Merged retention is real but unjudged.** A `:merged` patient may
   legally retain a bed (`decide :merge` absorbs admitted bed-holders)
   and the catalog neither affirms nor convicts that retention — B2 now
   exempts it with citations, but no row asserts it the way
   `expired-patient-retains-location` asserts the expired case. One
   line here per the de-scaffold ruling, not a roadmap row.
2. **The formerly certified-clean run is the louder conviction.** At
   1,984 arrivals B2 produces 39,684 rows from `:t` 1,968,180 — more
   rows than the 2,000-arrival run's 34,600, because its reinstatement
   lands ~1M seconds earlier in the log. The STOP record's finding 4
   (a conviction that never lifts inflates its own count) is true of
   B2 too; the rows-per-record emission convention is the catalog's
   own, kept deliberately, and A1 is what will empty it.
3. **The B1→B2 handoff is exact.** B2 is silent precisely while the
   visit holds `:status :admitted` and resumes at the closer record,
   where `world-after` is already `:discharged` plus the held bed — no
   gap, no overlap, witnessed at 2,000 arrivals.

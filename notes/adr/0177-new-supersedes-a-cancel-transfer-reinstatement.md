## ADR-0177 — `:new` supersedes a cancel-transfer reinstatement, and does not supersede a cancel-discharge

**Status:** Accepted (author-ruled R-A1-scope 2026-09-03; build session
same day, R30). Executes the engine half of
`roadmap.md#cancel-transfer-reinstates-a-new-subject`.

### Context

Every line number below is at `c16bb26`, read this session.

TS-5 (2026-08-29, `.agents/session-records/2026-08-29-ts-5-superseded-cancel.md`)
made a reinstating cancel ask the one question nothing had asked: has
the subject's state been superseded by a later event in their own
life? The guard is `ehrt.sim-engine.log-index/subject-superseded?`
(`log_index.clj:225-257`), two tables and a comparison:
`statuses-that-supersede-a-reinstatement` `#{:discharged :expired
:merged}` (`:201-223`) and `status-a-cancel-target-leaves`
`{:cancel-transfer :admitted, :cancel-discharge :discharged}`
(`:185-199`), whose two-entry shape exists because the ASYMMETRY is
the design: the same `:discharged` that makes a cancel-transfer
illegal is what makes a cancel-discharge legal.

`:new` was declared **deliberately ABSENT** from the superseding set,
in the set's own docstring (`log_index.clj:206-216`): it is what
`evolve :cancel-admit` writes, a cancel-admit is a correction of the
record rather than an event in the patient's life, and the `nobed`
10^5 cell at seed 20260824 carried 2 cancel-transfers against a
`:new` subject that the exclusion left alone — "an adjacent case this
change deliberately does not reach." The boundary was pinned
executable:
`engine-test/a-cancel-transfer-against-a-cancel-admitted-subject-is-still-applied`
(`engine_test.clj:751-771`), "pinned so that widening it is a decision
and not a drift."

The adjacent case was then reached. The 2026-09-02 STOP record
(`.agents/session-records/2026-09-02-downstream-self-check-failed.md`,
Step 6) measured it against a downstream QA calibration config
(`test-fixtures/downstream-calibration/`): five events in one batch at
`:t` 3,017,040, in which a `:cancel-admit` rewrites the subject to
`:status :new` (dissoc'ing `:class`, `:location`, `:home-ward`,
`:attending`, `:admitted-at`) and the `:cancel-transfer` behind it
puts `:location` and `:home-ward` **back** onto that `:new` subject.
Nothing ever vacates the bed again. B2
(`non-admitted-patients-hold-no-bed`, 2026-09-03,
`.agents/session-records/2026-09-03-b2-b1-stale-hold.md`) now convicts
exactly that state from the log side, so the calibration fixture exits
2 at both 1,984 and 2,000 arrivals. R-fork ruled the split: the
catalog half first (landed), the engine half — this ADR — its own
session.

### Decision (R-A1-scope, verbatim and binding)

> `:new` supersedes a `:cancel-transfer` reinstatement and does NOT
> supersede a `:cancel-discharge`. M6 Task 2 (`engine_test.clj:649`)
> stands; it goes green or the session STOPs.

The supersession becomes KIND-AWARE: `statuses-that-supersede-a-
reinstatement` extends the existing table asymmetry — per cancel kind,
its own superseding set — rather than staying one shared set:

- `:cancel-transfer` — `#{:new :discharged :expired :merged}`
- `:cancel-discharge` — `#{:discharged :expired :merged}` (unchanged
  in effect: `:discharged` is then excluded by the second conjunct,
  exactly as today, so `:expired`/`:merged` remain the only statuses
  that supersede one)

`subject-superseded?`'s comparison shape is unchanged; only the set it
consults is now looked up by `kind`.

### Why kind-aware and not plain admission to the set

The roadmap row's own wording ("admit `:new` to
`log-index/statuses-that-supersede-a-reinstatement`") is the naive fix,
and it rejects M6 Task 2
(`engine-test/cancel-discharge-restores-class-even-after-a-preceding-cancel-admit-stripped-it`,
`engine_test.clj:649-669`). Traced against that test's `world3`
(admit → discharge → cancel-admit, so `:status :new`): conjunct 1,
`:new` in the widened set → truthy; conjunct 2, `(not= :new
:discharged)` → true; the guard fires and
`decide :cancel-discharge` (`decide.clj:1697-1699`) rejects the very
cancel-discharge M6 requires to be applied. The session record's
Step 1 has the full trace.

### The coherence argument

The two cancels reinstate into **incoherent vs coherent** states, and
that difference — not symmetry — is the rule:

- A `:cancel-discharge` onto a `:new` subject reinstates
  `:admitted` + bed + `:class` **as a whole** (`evolve
  :cancel-discharge` restores status, location, home-ward, and class
  together — M6 Task 2's own finding). The result is a coherent
  admitted patient; the cancel-admit's record-correction is itself
  being corrected, and every field agrees.
- A `:cancel-transfer` onto a `:new` subject reinstates **only**
  `:location`/`:home-ward` (`evolve :cancel-transfer` moves a patient,
  it does not admit one). The result is the STOP record's measured
  state: `:status :new`, no `:class` at all, a held bed — a
  non-patient holding a bed for the rest of the log, the exact state
  B2 convicts (`check.clj:557-574`).

A cancel-transfer presupposes an admitted subject to move back;
a cancel-discharge is the undo that can re-create one. `:new` — the
subject's presence in the record corrected away — therefore supersedes
the former and not the latter.

### Payload effect

A churn `:cancel-transfer` decided against a `:status :new` subject
becomes `:step-rejected`, `:reason
:illegal-cancel-transfer-subject-superseded`, with decide's `:rejected`
map carrying `{:status :new}` (`decide.clj:1670-1672` — the existing
branch; no new reason, no new branch). Consequences:

- **Event-schema: no change, no bump.** Both `-subject-superseded`
  reasons entered the enum at 1.8.0 (`event_schema.clj:315-321`);
  `{:status :new}` rides decide's return value, never the event.
- **Draw-affecting in the corpus sense, not the decide sense.** Both
  the applied and rejected paths consume zero draws
  (`engine-test/a-superseded-cancel-consumes-exactly-the-draws-the-applied-path-consumes`),
  but the worlds diverge after the instant — a corpus carrying a
  `:new`-subject reinstatement re-times everything downstream of it.
  Hence the declared-sweep obligation the roadmap row names: oracle
  and bracket run against the pre-change tip, IDENTICAL expected on
  the golden roots (none carries the shape), any moving root declared
  per `bin/oracle-lib.sh`'s protocol or STOP.
- **The calibration fixture flips back to exit 0** at 1,984 and 2,000
  arrivals: the reinstatement B2 convicts is now refused at decide
  time, so the stale hold never enters the log.
- **The pinned boundary reverses, as its own docstring anticipated.**
  `a-cancel-transfer-against-a-cancel-admitted-subject-is-still-applied`
  pinned the old behaviour "so that widening it is a decision and not
  a drift" — this ADR is that decision, and the test is rewritten to
  assert the rejection, keeping its world (cancel-admit with no
  intervening discharge) as a second witness shape.

### The TS-5 exclusion, superseded with history kept

The "deliberately ABSENT" paragraph (`log_index.clj:206-216`) is
rewritten to record the reversal: `:new` absent for `:cancel-transfer`
was MEASURED-and-decided on 2026-08-29 (2 cancel-transfers at the
`nobed` 10^5 cell left alone), and REACHED on 2026-09-02 by the
downstream calibration config — the record-correction reading survives
for `:cancel-discharge` (M6 Task 2 still stands), while for
`:cancel-transfer` the exclusion is reversed by this ADR. The original
measurement's cell (`dense-7500-nobed.edn`, seed 20260824) is not in
the tree and is recorded as unreproducible in the session record; the
2 cancel-transfers it counted would be rejected under this ADR, a
statement this session can make from the mechanism but not re-measure.

### Not changed

- `churn.clj` — the division of labour stands as its own docstring
  states it (`churn.clj:126-140`): the static applicability oracle
  inserts, the decide-time guard rejects. A1 changes the guard only.
- `decide.clj` — both call sites pass `kind` already; the rejection
  branches, their ordering (subject before bed, TS-5's disclosed
  ordering), and `rejected-outcome` are untouched.
- `status-a-cancel-target-leaves` — unchanged; the second conjunct
  still encodes "the status the cancelled event left behind is the one
  status a legal cancel can still find."
- The cancel-discharge behaviour, anywhere: `:new` does not supersede
  it, per R-A1-scope.

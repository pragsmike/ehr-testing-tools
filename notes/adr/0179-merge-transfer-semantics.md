## ADR-0179 — a merge releases the bed and carries the pending result across

**Status:** Accepted (author-ruled R-bed / R-queue / R-inv / R-loc,
2026-09-05; build session same day, R30).

### Context

Every line number below is at `007deea6`, read this session.

A merge today is one `assoc`. `evolve.clj:290-295`:

```clojure
(defmethod evolve :merge
  [patient {:keys [participants surviving-mrn merged-mrns]}]
  (let [role ...]
    (case role
      :survivor (-> patient (update :mrns into merged-mrns) (assoc :active-mrn surviving-mrn))
      :merged (assoc patient :status :merged))))
```

The `:merged` arm sets a status and touches nothing else. The run loop
then does the rest of the work, at `run.clj:1289-1300`: a patient whose
status is `:merged` has its next queue entry popped and **discarded
whole**, which is how "the merged patient-id's stream ends with a
terminal merged-into event" is enforced rather than merely asserted.

Two consequences follow, and neither was decided — both are what falls
out of an arm that does nothing.

**1. The absorbed record keeps its bed.** An admitted bed-holder is a
legal merge target: `decide :merge`'s `never-mergeable?` excludes only
`:new` and `:merged`. Nothing after the merge ever moves that patient
again — every step of theirs is dropped at `:1289` — so the
`:location` they held at the instant of the merge is the `:location`
they hold for the rest of the run. The bed is occupied by a record that
has ceased to exist as a patient: a **census ghost**, visible to
`no-double-occupancy` and `occupancy-within-capacity`, both of which
fold the same `world-after` and both of which count it.
`check.clj:546-551` states the situation exactly as it stands, and
names the arm as the reason: `:merged` is in
`statuses-entitled-to-a-location` because "`evolve :merge`'s merged arm
touches only `:status`".

**2. A pending result vanishes.** `decide :order`
(`decide.clj:1494-1495`) does not splice its result into `:events` — it
returns `:schedule-followup`, a genuine future queue entry at
`result-t`, because a future-`t` event spliced into the present call's
events would enter the log ahead of smaller-`t` events and break the
log's global time ordering. That entry is an ordinary queue entry on the
ordering patient's own id. If that patient is merged away inside the
turnaround window, the entry is popped at `:1289` and dropped with
everything else — and the result, whose content was **already fully
computed and RNG-drawn back at order time**, is simply lost. The order
stands in the log with nothing ever resulting from it.

The second is the sharper defect. Losing the bed is a census error a
consumer can see; losing the result is a log that says a specimen was
collected and never reports it, with no event anywhere recording that
anything was abandoned.

### Decision

Four rulings, author-given 2026-09-05, three executed here.

**R-bed — the `:merged` arm clears `:location` and `:home-ward`.** The
absorbed record gives up its bed at the instant of the merge. Cleared
by `dissoc`, the idiom `evolve :cancel-admit` (`evolve.clj:252-257`)
already uses for the same job, so the key is **absent** rather than
present-and-nil — ADR-0178's distinction, applied here from the start
rather than corrected later.

**R-queue — a pending result follows the survivor.**
`:result-followup` steps of the absorbed patient-id **re-queue on the
survivor at the same `:t`**, with `:active-mrn` rewritten to the
surviving MRN and the result event's `:subject` participant rewritten
to the survivor's patient-id. **All other queued steps stay dropped** —
this is a narrow carve-out of one step type from `:1289`'s
short-circuit, not a reopening of the merged stream. The re-queue goes
through the *same* `reduce` at `run.clj:1380-1387` that every other
`schedule-followup` goes through, at its own `[t seq-no]`, so the
seq-no discipline that keeps the log globally time-ordered is the one
that was already there.

The survivor is read from a new PatientState field, **`:merged-into`**,
which the `:merged` arm sets to the `:role :survivor` participant's
patient-id. The run loop needs the survivor's identity at a point where
it has the absorbed patient's *state* and not the merge *event*, so the
fold is where that fact has to be recorded. It is `{:optional true}` on
`PatientState` (`state.clj:308`), set on exactly one arm, and read in
exactly one place.

**R-inv — the referential invariant resolves through the merge.**
`result-references-existing-order-and-follows-it-in-time`
(`check.clj:1173-1188`) accepts an order whose subject was merged into
the result's subject by a `:merge` at `:t` at or before the result's
own `:t`. Without this the checker convicts every log R-queue makes:
the result now names the survivor and the order it cites still names
the absorbed patient, which is the whole point of carrying it across.
Same invariant name, no new finding class, no new catalog row — **the
count pin stays at 46.**

**R-loc — the result's `:location` and `:attending` stay order-time,**
untouched. They are the patient's state when the specimen was ordered,
which is the convention `decide :order` documents at
`decide.clj:1470-1476` and which real order/result pairs follow when a
patient moves between the two. Recorded here as **OPEN pending
downstream reply**: a consumer that reads a result's PV1 context as
"where this patient is now" sees the absorbed patient's last bed on a
message whose PID is the survivor. That is a coherent reading of a real
HL7 convention and it is also a question about what the downstream team
expects, so it is asked rather than answered.

#### The transitive case, disclosed

R-inv is written for one hop. Chains are reachable: a survivor is
itself `:admitted` and so is itself mergeable, and when it is merged
away, R-queue moves the carried follow-up on again — leaving a result
whose subject is two merges away from the order it cites. The
implementation therefore resolves the merge relation **transitively**,
forward through merges at `:t` at or before the result's `:t`. This is
the strict generalization that contains the ruling's own one-hop case
exactly, taken as fix-forward-with-disclosure
(`rulings.md#R-stop-only-on-two-defensible-readings`: the literal
reading convicts a log the engine itself now writes, which is a
mechanical conflict rather than a second defensible reading). Flagged
for ratification.

### Payload effect, measured rather than predicted

The full derivation is
`.agents/plans/2026-09-05-adr-0179-merge-census.md`, run at `007deea6`
before any engine edit. The headline counts:

| | roots / logs | reach a `:merge` | absorbed holds a bed | follow-ups queued at merge |
|---|---:|---:|---:|---:|
| oracle roots carrying `:ground-truth` | 38 | **4** (7 merges) | **2** (both in `encounter-horizon`) | **0** |
| downstream-calibration @ 500 | 1 | 48 merges | 0 | **0** |
| downstream-calibration @ 1,000 | 1 | 77 merges | 2 | **0** |

Two facts do the work here.

**R-bed's expected mover set is `{encounter-horizon}`.** Five of the
seven oracle merges absorb a `:discharged` record, whose `:location` is
already gone; clearing its `:home-ward` changes no decision, because
nothing reads a merged patient's `:home-ward` and the run loop ends
that stream regardless. Only `encounter-horizon`'s two churn merges
absorb a bed-holder, and only a freed bed can reach another patient's
allocation. `bin/ground-truth-bracket` is expected to DIFFER on that
root and be IDENTICAL on the other 37 — an upper bound the bracket then
narrows, not a prediction it confirms.

**The oracle is BLIND to R-queue.** Zero absorbed patient-ids had a
follow-up pending, in every corpus this repository gates —
`:order-placed` and `:result-available` are equinumerous in both
fixture runs, 189/189 and 392/392. An IDENTICAL verdict on the
follow-up half would be vacuous, the same shape
`.agents/session-records/2026-09-01-event-stream-mutation.md` recorded
for `engine/replay`. R-queue's proof is the hand-built run-loop test
this ADR co-lands and nothing else, and that is stated here rather than
left for a reader to infer from a green bracket.

Mechanically the population is empty because the two ways to be merged
both miss the turnaround window: an `:identification` merge consumes a
placeholder, which has ordered nothing, and a churn merge in these
corpora almost always absorbs a `:discharged` record whose orders have
long since resulted. The window is not structurally closed — which is
why R-queue is a real fix and not a no-op — but nothing in the gated
corpora aims at it.

### Not changed

- **The `:merge` event's own payload.** Same kind, same
  `:survivor`/`:merged` roles, same
  `:surviving-mrn`/`:merged-mrn`/`:merged-mrns`. No schema change, and
  `:event-schema-version` stays `"1.8.0"`: `:merged-into` is a
  PatientState field, not an event field, and the log is byte-for-byte
  the same shape it was.
- **`decide :merge`.** Its validity table, its `never-mergeable?`, its
  rejection reasons are all untouched. This ADR is a fold change and a
  queue change.
- **`no-events-after-merged-terminal`.** It still holds, and holds for
  the same reason: the carried follow-up's participants name the
  survivor, so the absorbed id appears nowhere after its own merge. The
  invariant is what makes the subject rewrite in R-queue mandatory
  rather than cosmetic.
- **`statuses-entitled-to-a-location`'s membership** (`check.clj:551`).
  `:merged` stays in the set. After R-bed the engine cannot produce a
  merged bed-holder, so the entry is now tolerance for a hand-authored
  log rather than a description of engine behaviour; removing it would
  create a new conviction class that no ruling licenses. Its docstring's
  claim that "`evolve :merge`'s merged arm touches only `:status`" is
  corrected in the same commit, since that sentence is exactly what
  stops being true.
- **The bed's housekeeping status. OPEN, beside R-loc.** `:merge` is
  not in `ehrt.sim-engine.fold/bed-correction-event-types`, so a bed the
  absorbed record newly leaves does not return to `:ready` — it stays
  `:occupied` in the engine's own index and in `check.clj`'s
  independent one. After R-bed the two bed-holding merges therefore
  leave a bed no patient holds and housekeeping never reclaims. That is
  the shape that set's own docstring calls unintended for a cancelled
  admission ("the ward silently loses capacity"), reached from the
  other direction. Not fixed: the ruling licenses a state change, and
  emitting a `:bed-status-change` from a merge is a decide-layer change
  no ruling covers. It is also the reason nothing convicts today — the
  bed is never re-allocated to anyone, so the cost is capacity, not
  correctness.

### Consequences

- `docs/patient-state-model.md`'s `:merge` row moves from **planned** to
  **landed** and gains the transfer semantics; the accumulator table
  gains `:merged-into`.
- A consumer folding the log for census purposes stops counting merged
  records as bed-holders. That is a behaviour change at the consumer's
  own boundary, and it is the defect being fixed, not a side effect.
- A result carried across a merge reports under the survivor's MRN with
  the absorbed patient's order-time `:location`/`:attending` (R-loc).
  Flagged above as the one thing this ADR asks rather than answers.

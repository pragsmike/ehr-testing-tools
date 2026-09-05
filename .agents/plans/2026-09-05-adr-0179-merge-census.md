# ADR-0179 derivation — which corpora reach a `:merge`, and what a merge absorbs

Step 1 of the merge-transfer-semantics session (2026-09-05), run at
`007deea6` **before any engine edit**, so every count below is a
baseline the same session's own gates are then measured against.

Two questions, both asked of real logs rather than of the code:

1. **Which oracle roots' ground truth contains a `:merge`** — the upper
   bound on what R-bed can move, since `evolve :merge`'s `:merged` arm
   runs nowhere else.
2. **In which of those did the absorbed patient-id have a
   `:result-followup` queued at merge time** — the population R-queue
   re-queues, measured as an `:order-placed` by the absorbed subject
   with no `:result-available` naming its log index anywhere in the log.
   A dropped follow-up has exactly that signature and no other.

## Method

`ehrt.oracle.digest`'s own 41 roots were run to EDN
(`clojure -M:dev -m ehrt.oracle.digest`, with
`components/patient-simulator/test` on the path — the two fixture roots
slurp from there, which is what `bin/oracle-lib.sh` wires as
`:oracle-run`). Three roots are interpreter-layer batches that write a
vector of walks and carry no `:ground-truth` key at all
(`appendicitis`, `ear-infections`, `sore-throat`); the remaining
**38 carry a `:ground-truth`** and are the population below.

Bed-holding at merge is read from `ehrt.sim-engine.interface/replay`'s
`:world-before` at the merge record — the absorbed patient's actual
`:status`, `:location` and `:home-ward` one instant before the arm
under change runs — not inferred from event kinds.

## 1. Oracle roots reaching a `:merge`

Four of the 38, seven merges in all. The other 34 contain no `:merge`
event and are untouchable by this ADR by construction.

| root | events | merges | absorbed `:status` at merge | holding a bed | holding a `:home-ward` |
|---|---:|---:|---|---:|---:|
| `chatter-charges` | 477 | 2 | `:discharged` x2 | 0 | 2 |
| `demographic-fold` | 671 | 2 | `:discharged` x2 | 0 | 2 |
| `encounter-horizon` | 170 | 2 | `:admitted` x2 | **2** | 2 |
| `scheduling` | 487 | 1 | `:discharged` | 0 | 1 |

Five of the seven carry `:cause :identification` (the placeholder-join
merge, `decide`'s own demographic arm) and two carry no `:cause` at all
(churn's M2b `:merge`). The split matters: **every bed-holding absorbed
patient is a churn merge**, and both are in `encounter-horizon`.

**Predicted mover set for R-bed: `{encounter-horizon}`, an upper bound.**
Clearing `:home-ward` on a `:discharged` absorbed record changes no
decision — nothing reads a merged patient's `:home-ward`, and the run
loop ends that patient-id's stream regardless — so the other three
roots can only move if freeing a bed nobody held moved something,
which is not a thing. Whether `encounter-horizon` actually moves is for
`bin/ground-truth-bracket` to say, not this table.

## 2. Result-followups queued on an absorbed patient-id

**Zero, in every one of the 38 roots.** `:order-placed` by an absorbed
subject before its own merge: 0 in all seven merges. There is nothing
for R-queue to re-queue anywhere in the oracle.

That is not an artifact of the oracle's small arrival counts. The same
census over the largest population this repository has —
`test-fixtures/downstream-calibration/config.edn`, seed 424242,
`--churn`, at both calibrated arrival counts — finds the same emptiness:

| population | events | merges | absorbed `:discharged` | absorbed `:admitted` | holding a bed | orphaned follow-ups |
|---|---:|---:|---:|---:|---:|---:|
| 500 arrivals | 29,063 | 48 | 48 | 0 | 0 | **0** |
| 1,000 arrivals | 35,408 | 77 | 75 | 2 | **2** | **0** |

`:order-placed` and `:result-available` are **equinumerous** in both —
189/189 at 500, 392/392 at 1,000. Not one follow-up has ever been
dropped by the M2b short-circuit in any corpus this repository gates.

**Consequence, stated plainly rather than discovered later: the oracle
is BLIND to R-queue.** An IDENTICAL bracket verdict says nothing about
whether the re-queue works, exactly as
`.agents/session-records/2026-09-01-event-stream-mutation.md` recorded
for `engine/replay`. R-queue's only witness is the hand-built run-loop
test the same session's RED step owes, and that is the whole of its
proof.

## 3. Why the population is empty, mechanically

A follow-up is queued only between `decide :order` and its own
`result-t`, a profile-sampled turnaround. An absorbed patient must
therefore be merged away *inside that window*. Two of the three ways to
be merged never open one:

* `:cause :identification` merges consume a **placeholder**
  registration, and a placeholder that has not been identified has
  placed no orders.
* churn's `:merge` picks its absorbed partner among patients that are
  neither `:new` nor already `:merged` (`decide :merge`'s
  `never-mergeable?`), which in these corpora resolves overwhelmingly to
  `:discharged` records whose orders have long since resulted.

The window exists — it is not structurally closed, which is why R-queue
is a real fix and not a no-op — but nothing in the gated corpora aims
at it. Reaching it needs orders and merges on the same patient at
overlapping instants, which only a purpose-built log arranges.

## 4. The bed R-bed frees is not returned to housekeeping

Recorded here because the measurement is what raises it. `:merge` is
not in `ehrt.sim-engine.fold/bed-correction-event-types`, so a bed the
absorbed record newly leaves does **not** go back to `:ready`: it stays
`:occupied` in both the engine's own bed index and
`check.clj`'s independent one. After R-bed the two bed-holding merges
therefore leave a bed that no patient holds and that housekeeping never
reclaims — the exact shape that set's own docstring names as
unintended for a cancelled admission ("the ward silently loses
capacity"), arrived at from the other direction.

**Not fixed here.** The ruling under execution is verbatim
("the `:merged` arm clears `:location` and `:home-ward`") and licenses
a state change, not a new emission; adding a `:bed-status-change` to a
merge is a decide-layer change no ruling covers. Carried into
ADR-0179's own open items beside R-loc.

## Reproducing

The three throwaway census scripts are not vendored — they are twenty
lines each of `edn/read-string` plus the two predicates stated above,
and the numbers, not the scripts, are the artifact. What a re-derivation
needs is in the Method section.

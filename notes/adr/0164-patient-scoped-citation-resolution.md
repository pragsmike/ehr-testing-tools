## ADR-0164 — decide-time citation resolution becomes patient-scoped: a latent cross-patient `{:module :state}` collision, fixed on direct assertion and NOT on the seed-424242 reproduction

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-23.

### Context

A citation in this project is `{:module :state}` — a MODULE
COORDINATE. It is not patient-qualified, and was never meant to be:
`compile_trajectory.clj`'s own `citation` is literally `{:module
(:module event) :state (:state event)}`. Two patients walking the same
module therefore produce **byte-identical** citations.

Both decide-time citation scans searched the whole ground-truth log
with no patient filter:

- `engine.clj:849`, `decide :medication-end` — `(last (keep-indexed ...
  (= :medication-order (:event ev)) (= order-citation (:citation ev))))`
- `engine.clj:885`, `decide :care-plan-end` — the same shape against
  `:care-plan-start`

`last` takes the most recent match anywhere in the log. So whenever a
peer patient opened under the same citation *after* this patient's own
opening and *before* this patient's end, the end resolved to the
**peer's** event — attributing one patient's prescription or care plan
to another, silently, with a well-formed index.

### Read facts, measured this session

The collisions are real and ordinary, not contrived. Counted directly
off both reproduction logs at `7a3ffd84`:

| run | colliding `:medication-order` citations | colliding `:care-plan-start` citations |
| --- | --- | --- |
| seed 424242 | 3 — `sore_throat/:prescribe-antibiotics-adult` across 5 patients, `sinusitis/:prescribe-antibiotic` across 3, `bronchitis/:acetaminophen` across 9 | 1 — `bronchitis/:nonsmoker-careplan` across 10 patients |
| seed 5 | 4 — `bronchitis/:acetaminophen` across 4, `sore_throat/:prescribe-antibiotics-adult` across 4, `sinusitis/:prescribe-antibiotic` across 4, `sore_throat/:prescribe-antibiotics-child` across 3 | 1 — `bronchitis/:nonsmoker-careplan` across 5 patients |

### This is NOT the cause of the seed-424242 failure (R4)

Stated explicitly, because the design channel got this wrong once
already and the error is recorded below.

> R4: Q1(a)/Q2(a) stand for commit 2 — same-patient predicate in both
> decide-time scans — justified by the engine-level direct assertion,
> explicitly NOT by the 424242 reproduction. The ADR must say so.

**Zero cross-patient resolutions occurred in either run.** Checked
directly: every `:medication-end`/`:care-plan-end` carrying a non-nil
resolved index was compared against its own patient, and none
mismatched. For seed 424242 the reason is structural, not lucky — its
one `:medication-end` carried `:order-citation nil` (ADR-0163's
defect), and `(when order-citation ...)` guards the scan out entirely,
so the unscoped scan **was never reached**. Fixing this changes nothing
about that failure.

The entire justification for this commit is the two engine-level tests
below. It is a latent defect fixed on direct assertion.

### The fix

The same-patient participant condition added to both scan predicates,
and nothing else — no other logic, no RNG change, no citation-shape
change:

```clojure
(some #(= patient-id (:patient-id %)) (:participants ev))
```

Mirrored, not invented. This is the identical predicate
`last-uncancelled-index` (`engine.clj:538-541`) already uses for
exactly this reason, and the identical one `check.clj`'s own
`medication-end-references-existing-order-and-follows-it-in-time`
already tests the resolved target against — the checker was always
asking for same-patient resolution; only the resolver was not
supplying it.

### Regression shape

Two tests in `engine_test.clj`, both red first with real output. Each
scripts A ordering, then B ordering under the identical citation, then
A ending, and asserts A's resolved index names A:

```
FAIL in (medication-end-resolves-its-own-patients-order-not-a-later-peers)
FAIL in (care-plan-end-resolves-its-own-patients-start-not-a-later-peers)
expected: (= "A" (:patient-id (first (:participants resolved))))
  actual: (not (= "A" "B"))
```

**R5 disclosure.** The `world-of`/`admit`/`fold-events` scaffold these
use cannot produce a `:registered` event, so the full `check-all`
catalog is unavailable to them — the same scaffold gap every scripted
test in that file already carries. Per R5 they assert the resolved
index **directly**, following the convention
`expired-disposition-discharge-satisfies-its-own-new-invariant`
(`engine_test.clj:1133-1147`) already establishes. Recorded here rather
than left implicit.

### Oracle sweep

`make docsgen`, exit 0: **no artifact changed** — as predicted for a
fix that only ever alters an index that was resolving to the wrong
patient.

Step 10's prior count reconciles: three `:order-event-id` occurrences
each in `demos/traces/emit-state/ground-truth.edn` and
`demos/traces/order-result/ground-truth.edn`, plus two
`:order-event-id` and one `:start-event-id` in
`components/sim-engine/resources/sim-engine/event-examples.edn`. None
involved cross-patient resolution, and the zero diff is itself the
proof: had any resolved to a peer, this fix would have moved its index
and the artifact would have differed. Two further facts make the
outcome unsurprising — the six trace occurrences belong to
`:result-available` events, which resolve through the **already**
patient-scoped `last-uncancelled-index`, not through either scan
touched here; and `event-examples.edn`'s fixture citations are unique
per patient in the fleet run that produces them.

### Error ledger

- **Original landing — unearned specificity.** Both scans were written
  as if a citation identified an event uniquely. It identifies a
  MODULE STATE; the patient dimension was simply never considered, in
  a codebase whose sibling resolver one screen up already filtered by
  participant.
- **Design channel, this arc — unearned specificity.** The observed
  seed-424242 failure was attributed to this unscoped scan from
  mechanism plausibility, without tracing the violating event. The
  trace shows the scan was never reached: the event carried
  `:order-citation nil`, guarded out before the scan ran. The real
  cause was the compile-time unpaired end-step (ADR-0163). Two real
  defects, one of them load-bearing for the report — and the
  plausible one was the wrong one. This ADR exists in its own commit,
  on its own evidence, for that reason.

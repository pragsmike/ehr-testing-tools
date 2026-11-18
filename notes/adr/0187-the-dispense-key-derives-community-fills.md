## ADR-0187 — the `:dispense` key: one community fill derived for each order an outpatient visit wrote

**Status:** Accepted 2026-11-18 (author rulings "Agree on all. Go.",
2026-11-17, and "2 ok as long as the time is after the encounter",
2026-11-18; build session same day). Event schema **1.12.0**,
unchanged; describe report **1.3.0**, unchanged.

### Context

Every line number below is at `2aaefd6a`, read this session.

ADR-0186 landed `:medication-dispense` as a step a pathway authors and
named this as owed: fills DERIVED for orders nobody authored a dispense
for, behind an engine key. The population is module-authored
prescriptions. The census of 2026-11-17 counted 79 in-log at
`dense-7500`'s 7,500 cell, and the diagnosis record of the same date
found 156 in the oracle's `osteoporosis` root.

Two rulings stand behind this slice. On 2026-11-17 the author agreed to
"a `:dispense` key gating only the derived half". On 2026-11-18, to the
proposal "under `:dispense`, each in-log order written in a
non-inpatient encounter gets one `:community` fill at the instant that
encounter closes", the author ruled: "2 ok as long as the time is after
the encounter". The design channel read the condition as: the fill is
never inside the visit. It follows the visit's closing event in the
log, at that event's `:t` or later, and does not carry the closed
visit's `:encounter-id`.

This is slice 1b: the key and the derivation. Community setting only,
one fill per order, no draw, no constant.

### Decision

1. **The key.** `:dispense`, truthy, on the run config. It is a member
   of `ehrt.sim-engine.config/config-keys`, is carried on the world as
   `:dispense` (nil unless the run opted in), and is stamped on the
   manifest's `:engine-params` the way `:encounters` is. Absent
   entirely is the byte-identical path.
2. **The rule.** When an outpatient visit closes, each medication order
   that visit wrote and left open gets one `:medication-dispense` with
   `:setting :community`, in the orders' own order, at the closing
   instant.
   - *This visit's orders* are read off the patient's own
     `:medication-orders` records: those carrying the open encounter's
     `:encounter-id` where the run mints one, otherwise those written
     at or after the instant the open encounter record says it opened.
     Without `:encounters` a patient has one encounter in the whole
     run, so the instant is enough.
   - *Left open* means `:status :active`. An order the visit itself
     ended gets no fill. ADR-0186's law bounds a dispense by its
     order's end, so a fill placed after the close would be one the
     catalog convicts.
3. **The fill sits after the close, as a queued step.**
   `decide :outpatient-visit-end` returns its closing event alone, and
   with it one `:schedule-followup` queue entry at the same instant
   holding one `:medication-dispense` step per order. The run loop
   gives that entry a `[t seq-no]` ahead of the patient's own remaining
   steps, so the fills follow the close and precede whatever the
   patient does next.

   The channel expected the fills to ride the closing decide's own
   event vector, with no queue entry. The tree cannot emit them that
   way and meet the ruling. `fold/apply-events` stamps a whole batch
   off the world as it stood BEFORE the batch
   (`encounters/stamp-encounter`), so an event returned beside the
   closer would carry the `:encounter-id` of the visit it is supposed
   to follow. A step decided later meets a world in which the close has
   been folded, and is stamped with nothing. The alternative was to
   teach the stamp to skip events after a closer in the same batch,
   which changes a code path every run takes; the queue entry changes
   none.
4. **The event is the authored one.** The queued step is ADR-0186's
   own step, so `decide :medication-dispense` builds the event and
   resolves `:order-event-id` by citation, as it does for an authored
   fill. A derived fill states no `:citation`: no module state and no
   pathway step is its source. No event-schema change:
   `classify-change` against the frozen 1.12.0 baseline returns
   `{:additive? true, :breaking []}`, and the live export equals the
   baseline.
5. **No draw.** Neither decide touches a stream.
6. **Admission-class orders are excluded.** An order written while an
   `:admission` is open gets nothing derived. An emergency-department
   treat-and-release is an admission in this model, so a prescription
   written at an ED discharge gets none either.
7. **The key does not look for an authored fill.** A pathway that
   authors a community dispense of an order and runs with the key gets
   two dispenses of that order.
8. **One fill.** Each order is filled once, however long the
   prescription runs. The chronic refill ruled in principle on
   2026-11-17 is still owed; its period and its cap are unruled.

### The erasure law, the key's contract

The key adds events and draws nothing, so a run with it is the keyless
run with dispenses inserted. Delete every `:medication-dispense` from
the keyed log, renumber every log-index-bearing field past the
deletions, and the result equals the keyless log, event for event.

`ehrt.sim-engine.dispense-derivation-test/erasing-the-derived-dispenses-leaves-the-keyless-run-event-for-event`
gates it over five runs, comparing the whole log:

| Run | Fills |
|---|---|
| the oracle's `osteoporosis` root config | 156 |
| the oracle's `sinusitis` root config | 7 |
| `osteoporosis` with `:encounters` | 156 |
| two authored orders per visit, 3 patients | 6 |
| authored visits and stays under churn, with `:encounters` and `:bed-cycle` | 30 |

The reference fields are walked out of the live event schema, the walk
`ehrt.corpus.event-mutate-test` makes, so a field added to the schema
is renumbered without the test being touched. The law was green as
first written and was not adjusted.

Rendered HL7 v2 follows the log. The keyed `osteoporosis` run emits the
keyless run's 312 messages; 310 differ from their twin, in MSH-10 only,
whose suffix is the event's log index.

This is the exception to the opt-in law's second rule, which says an
engine key draws and so reshuffles the population.
`docs/consuming-ground-truth.md` now states the exception.

### The producer

The oracle's 45th root, `osteoporosis-dispense`: the `osteoporosis`
root's own config plus the key, a FIRST BASELINE.

Measured: 1,236 events against `osteoporosis`'s 1,080. 156 medication
orders, all written in an outpatient visit, none in an admission. 156
`:medication-dispense`, all `:community`, naming 156 distinct orders.
`check-all` clean.

**No existing corpus byte moves.** The ground-truth bracket and the
regression oracle each differ from `2aaefd6a` by the one added root
(banners in the session record). No shipped scenario sets the key.

### The describe row

`:medication-dispense-community` reads `:configured :yes` when a
pathway authors a community dispense or the run's options carry
`:dispense`. The inpatient row is unchanged. `describe-version` stays
1.3.0: its rule makes a new row or key minor and a changed meaning
major, and this adds neither a row nor a key and changes no report of
a run without the key.

### Follow-ons owed (not taken here)

- **Chronic refills.** Ruled in principle; the period and the cap are
  open. `osteoporosis`'s 156 orders are all ones a module marks
  chronic, so this root is the population that rule would act on.
- **ED-discharge prescriptions.** An ED visit is an admission here, so
  decision 6 excludes it. Whether such an order is filled in the
  community is unruled.
- **Turning the key on in `dense-7500`.** A ground-truth change to a
  shipped corpus, owed notice to downstream. It would add fills for
  the module-authored orders only; the 4,907 hand-authored orders at
  the 7,500 cell are written during admissions.
- **The mutation column.** ADR-0186's own follow-on, unchanged: no
  shipped log carries a dispense to calibrate one against.
- **Two orders of one citation in one visit.** Both derived fills
  would resolve to the later order, because the step resolves by
  citation and takes the latest. Neither module root measured here
  (`osteoporosis`, `sinusitis`) holds such a pair. Fixing it means carrying the order's log index on its state
  record.

### What this does not do

- Rows nothing on the roadmap: this record is the record.
- Changes no event schema, no invariant and no HL7 or FHIR rendering.
- Derives nothing inpatient.

## ADR-0188 — the second ground-truth rebaseline: an end references the latest open opening; `dense-7500` takes the `:dispense` key

**Status:** Accepted 2026-11-18 (author rulings "1 yes" and "3 accept",
2026-11-18, and "Key only", 2026-11-18, given after this session halted
at its fifth step; build session same day). Event schema **1.12.0**,
unchanged; describe report **1.3.0**, unchanged.

### Context

Two ground-truth changes, batched by ruling into one declared change so
that the gated `dense-7500` figures regenerate once and the downstream
consumer gets one notice and one pin.

1. `index-of-citation` in the GMF interpreter returned the FIRST event
   of a module, type and state. A MedicationEnd, ConditionEnd or
   CarePlanEnd that names its opening by state therefore referenced the
   first opening of that state, not its own. The second and later ends
   in a looping module each re-ended the first opening, and the later
   openings stayed active. The diagnosis is the 2026-11-17 record
   `unreached-medications-and-references`, step 3.
2. `dense-7500` set no `:dispense` key and authored no dispense
   (ADR-0186 and ADR-0187, follow-ons).

### Decision

**1. The reference rule.** An end by state name resolves to the latest
opening of that state, in its own module, that no earlier end of the
same type references. When every opening of the state is already ended
it keeps the old answer, the first opening; when the state was never
opened it references nothing. One private function,
`index-of-open-citation`, serves all three end types.

`index-of-citation` stays for one caller, the death-cause lookup. That
caller reads the onset's CODES, and every firing of one state carries
the same codes, so first-match is right there and was left alone.

**2. `dense-7500` takes the key and authors nothing.** `config.edn`
gains `:dispense true` as its tenth opt-in key. `config-nobed.edn` and
`config-28024.edn` follow by their own derivation rules;
`config-bare.edn` stays bare of it.

The session prompt asked for more: one authored
`{:type :medication-dispense :setting :inpatient}` step per order in
`dense-inpatient`, with the erasure law holding across the change. That
cannot hold, and the session halted on it. Measured at the 750 cell on
scratch configs:

| run | baseline events | with the steps, dispenses deleted | equal |
|---|---|---|---|
| `--churn` | 33,296 | 33,379 | no, from index 41 |
| no churn | 32,279 | 32,279 | no, from index 399 |

- Under churn an authored step costs draws. Churn injection draws at
  every gap between steps, so two more steps are twelve more draws from
  that patient's stream and the population reshuffles.
- Without churn an authored step costs a queue turn. The log holds the
  same events at the same instants, and 372 positions change order.

The author ruled "Key only". Inpatient dispenses remain something a
pathway may author (ADR-0186); this scenario authors none.

### What moved, predicted and measured

**Oracle roots, under the reference rule.** The prediction was committed
with the red tests, before the fix existed (`c8b83cc1`). The regression
oracle from `164fae67` to the fix commit `0c4bc797` reports DIFFERS on
exactly the predicted ten and IDENTICAL on the other 35:

bronchitis, demographic-fold, dermatitis, ear-infections,
ear-infections-engine, injuries, med-rec, sinusitis,
total-joint-replacement-engine, veteran-prostate-cancer.

The ground-truth bracket reports the same set less `ear-infections`, a
batch root it does not digest. `ear-infections-history-engine` holds
the same 142 wrong ends as its twin and is IDENTICAL, as predicted:
history mode drops the history-phase events, so no reference reaches
its log.

All three end types were wrong, not one. Ends that differed between
first-match and latest-open, by root, before the fix: `med-rec` 17,965,
bronchitis 1,766, injuries 816, ear-infections-engine 142,
veteran-prostate-cancer 142, total-joint-replacement-engine 110,
demographic-fold 108, ear-infections 51, sinusitis 10, dermatitis 2.

**Gated corpora.** Three of the four arc-0 pins moved and were re-pinned
with their baselines: seed-202-ed-tuesday (2 of 1,214 events differ),
seed-424242-clinic-decade (15 of 1,774), seed-5-clinic-decade (27 of
1,412). Every differing event is a `:registered` event's
`:pre-horizon-facts`. No event count moved. The prompt expected only
`dense-7500` to move among the shipped scenarios.

**`dense-7500`**, seed 20260824, `--churn`, measured at `3ba95de9`:

| cell | events before | events now | dispenses | messages before | messages now |
|---|---|---|---|---|---|
| `config.edn` 7,500 | 167,715 | 167,763 | 48 | 176,971 | 176,973 |
| `config-nobed.edn` 7,500 | 125,700 | 125,748 | 48 | 134,886 | 134,891 |
| `config-bare.edn` 7,500 | 101,120 | 101,120 | 0 | 65,643 | 65,643 |
| `config.edn` 750 | 33,296 | 33,299 | 3 | 35,926 | 35,925 |
| `config-28024.edn` 28,024 | 671,589 | 671,646 | 57 | -- | -- |

Every event-count difference is the dispenses. The reference rule moved
no event count in any cell; at the 750 cell it changed 39 events (32
`:registered` facts, 7 `:outpatient-visit`) and no event changed
position. The message differences are the rule's, not the key's: a
dispense renders no message.

The 28,024 log is now 264,575,690 bytes, sha-256
`5fb8187b5aef0bef2f518b61becc5ddc21ce042eafca6bd7f861beef5d8cc6c0`. The
log it wrote from ADR-0185 until now, `ee59219e...`, is kept in
`figures.edn` under `:pre-adr-0188`, as `589600c5...` was kept before
it.

Every dispense in every cell is `:community`. The inpatient describe
row reads observed 0.

### The erasure equality at dense

Across the adoption commit, at the 750 cell: the log with the key, with
its 3 dispenses deleted and every log-index field renumbered, equals
the log the same tree writes from the config without the key, event for
event (33,296 events). The same comparison at 7,500 on a scratch config
before adoption: equal, 167,715 events, 48 fills.

### ADR-0187's two findings, as measured here

- **Two orders of one citation in one visit.** None at the 750 cell and
  none at 7,500, so no derived fill in `dense-7500` resolves to the
  wrong order. The defect stands in the code; this scenario does not
  reach it.
- **A derived fill carrying the closed visit's `:encounter-id`.** None:
  0 of 3 at 750, 0 of 48 at 7,500, 0 of 57 at 28,024.

### What did not change

- Event schema 1.12.0 and the describe report 1.3.0.
- The checker: no invariant added, removed or reworded. `check-all` is
  clean over the new 750 cell.
- Determinism: both timed runs of every cell wrote byte-identical logs.
- The engine. The reference rule is in the interpreter; the adoption is
  configuration.

### Downstream

Corpora that move: every corpus walking a module that ends a state it
opens more than once, which among shipped scenarios is `dense-7500`
(all five cells), `ed-tuesday` and `clinic-decade`. Why: `:references`
on end facts now names the end's own opening, and `dense-7500`'s keyed
cells gain community dispenses. Schema unchanged. A retained corpus is
a valid artifact of the earlier pin. New 28,024 sha-256 above.
`dense-7500` carries dispenses in the community setting only.

### Owed next

- **Inpatient dispenses at `dense-7500`.** Not possible as authored
  steps without reshuffling the population. The choices are to accept
  the reshuffle, to derive the inpatient half, or to make the step free
  of draws and of a queue turn in the engine. Unruled.
- **The mutation column.** A shipped log now carries dispenses, 48 at
  the 7,500 cell, all community, so a column can be calibrated against
  one setting.
- **Chronic refills.** Period and cap unruled (ADR-0187).
- **ED-discharge prescriptions.** Unruled (ADR-0187).

### What this does not do

- Rows nothing on the roadmap: this record is the record.
- Authors no dispense anywhere.
- Re-measures nothing in `docs/consuming-ground-truth.md` below the
  Scale table's four rows; the 2026-08-29 paragraphs say so themselves.

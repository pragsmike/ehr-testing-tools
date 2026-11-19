# The `:dispense` key derives the inpatient dispense, after a drawn pharmacy turnaround set per ward (ADR-0189)

2026-11-19. Base `046e611c`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 1 on one finding, reported and not a
premise of this session: among the last five CI runs, the `Integration`
workflow at `164fae67` is red. That is the stale 46-invariant pin the
2026-11-18 record describes, fixed in `f8df1749`; the `test` workflow
is green at `046e611c`.

Prompt: `.agents/prompts/2026-11-19-inpatient-dispense-derived.md`.

## Steps 1 and 2: red tests, then the build

- `05e47820` the config half: a ward's optional `:dispense-minutes`,
  `default-dispense-minutes` (`:ed [5 25]`, `:inpatient [15 60]`,
  provisional) and the one reader, in `sim-model`.
- `bf24dfcc` the red tests: 16 failing assertions in
  `inpatient_dispense_derivation_test`, all because nothing was
  derived. Green by construction there, as that commit says: (c) both
  cases, (d), and (e)'s equality.
- `18a7f84f` the build. `poly check` OK; `brick:sim-engine` 60
  namespaces, 14,202 passes, 0 failures; `brick:sim-check` 12
  namespaces, 1,230 passes, 0 failures.

**The draw site.** The stream scheme did offer one that moves no other
draw: a family of its own. `:pharmacy`, tag 7, keyed by arrival ordinal
as `:patient` is, built only under the key. No existing stream's seed
changed and the scheme marker stays "1.0". ADR-0187's erasure test is
green for the whole key and was not edited, its stays-under-churn case
included, which now erases inpatient dispenses as well.

## Step 3: the prediction, written before either instrument ran

**Oracle roots that will DIFFER: none.** One root sets the key,
`osteoporosis-dispense`. Its module's only Encounter state is a
wellness encounter, which compiles to an outpatient visit, so no order
in that root is written during an admission. The `dispense` root
authors its dispenses and does not set the key. Every other root does
not set the key, and without the key no code path differs. Both
instruments: all roots IDENTICAL.

**Gated corpora that will move:** the three `dense-7500` configs that
set the key (`config.edn`, `config-nobed.edn`, `config-28024.edn`), at
every cell. `dense-inpatient` writes two orders per stay.

**Gated corpora that will not move:** `config-bare.edn`; the four arc-0
pins; the `ed-tuesday` and `clinic-decade` scenarios. None sets the
key.

### Step 3, measured

- `bin/regression-oracle 046e611c 18a7f84f --declared-digest-change`:
  exit 0, IDENTICAL, every root.
- `bin/ground-truth-bracket 046e611c 18a7f84f --declared-digest-change`:
  exit 0, IDENTICAL, 42 roots.
- The three keyed dense configs moved and `config-bare.edn` did not
  (step 4). The arc-0 pins and the two other scenarios are the suite's
  and the integration tier's to confirm (Close).

The moved set equals the prediction. It also means no oracle root
exercises the inpatient half; ADR-0189 names that as owed.

## Step 4: dense-7500

`8acd6028` gives every ward of the four configs its
`:dispense-minutes`, the class defaults written out. `config-bare.edn`
carries them because it is `config.edn`'s byte prefix; it has no key
and reads none.

Measured at `8acd6028` by ADR-0188's drill: one warm-up and two timed
runs per cell, `/usr/bin/time -v`, Windows-side load sampled before
every run (0 to 18 over the twelve; 1 before the 28,024 run). All three
runs of every cell wrote one sha.

| cell | events | messages | msg/event | wall s | peak RSS MiB | events.edn sha-256 |
|---|---|---|---|---|---|---|
| `config.edn` 7,500 | 170,224 | 176,956 | 1.0395 | 123.58 | 1,903 | `670fbf97...826a509d` |
| `config-nobed.edn` 7,500 | 128,209 | 134,895 | 1.0521 | 106.83 | 1,692 | `dd56d27b...d5cac51d` |
| `config-bare.edn` 7,500 | 101,120 | 65,643 | 0.6492 | 51.06 | 1,170 | `56559a11...` (unchanged) |
| `config.edn` 750 | 33,546 | 35,920 | 1.0708 | 54.80 | 1,167 | `99f294fe...d1f5a412` |
| `config-28024.edn` 28,024 | 681,741 | -- | -- | 371.04 | 4,602 | `ce0982dec2e5d0dcab2e342a6b8422343a7a09349c1f084b08fa4b0ad9d961ce` |

The 28,024 log is 267,680,351 bytes, 32,080 subjects. `5fb8187b...` is
kept in `figures.edn` under `:pre-adr-0189`.

The per-cell dispense table (orders written in admissions, inpatient
dispenses, the undispensed fraction by cause, order-to-dispense minutes
by ward class, community fills) is in ADR-0189, "What moved", and in
the dense README. In short, at 7,500: 4,938 admission-written orders,
2,461 dispensed, 2,477 not (2,444 ended at their own instant, 31 whose
stay closed first, 2 merged away), 48 community fills.

Gates:

- Erasure at the 750 cell: the keyed log less its 250 dispenses equals
  the keyless log, 33,296 events.
- `sim check` over the keyed 750 log: ok.
- `describe` over the new 750 cell: inpatient row `:configured :yes`,
  observed 247; community row `:configured :yes`, observed 3. Over the
  28,024 log: 10,095 and 57.
- `bin/demo-exerciser-dense-7500` rewrote `:asserted` to 170,224 /
  176,956 / 1.0395 and 33,546; its only failure on that run was the
  tree-clean postcondition, the tree holding the uncommitted figures.
- `brick:docs-tooling`: 98 namespaces, 5,350 passes, 0 failures.

**How the cause of an undispensed order was read.** From the log, as
the first of three events after the order: its end, its stay's close,
the merge absorbing its patient. The draw itself is not in the log.

**How the ward class at the order was read.** By folding ward-bearing
events per patient, bed swaps included. A first version read
`:order-placed` and `:result-available` locations too, which are
order-time context and not where the patient is; it showed lags
outside the ED range that were inpatient-ward lags. Corrected, no lag
in any cell is outside its ward's range.

## Steps 5 and 6

`8a507c06` figures and docs; then ADR-0189 with this record. The config
reference is `components/sim/docs/operational-models.md`, "Pharmacy
turnaround".

## What this prompt got wrong, and what the session found

1. **"Roughly 4,900 inpatient orders" would mostly be dispensed.** Half
   are: `dense-inpatient` ends its analgesia order at the instant it
   writes it. The prompt expected this and asked for the fraction; it
   is 50.2% undispensed at 7,500.
2. **A queued dispense of a merged patient "dies".** It does, and it is
   rare: 0 at 750, 2 at 7,500, 6 at 28,024.
3. **`:dispense-minutes` "mirroring `:turnaround-minutes` exactly".**
   Not exactly. The turnaround schema is a bare pair of ints and
   refuses no inverted range; step 1(b) asked that an invalid range be
   refused, so this key's schema checks `0 <= lo <= hi`. And
   `default-facility` does not write the values out, since that would
   put a new key into every default-facility run's configuration.
4. **The mechanism needed one more condition.** A dispense finds its
   order by citation, latest first. With two orders of one citation in
   one stay, a dispense of the earlier would name the later. The
   earlier one is not dispensed once the later is written. No shipped
   corpus reaches it.
5. **Not in the prompt: the wire moves.** The erasure law holds for
   ground truth. Rendered messages are not held fixed: the emitter
   draws once per ground-truth event for chatter and latency. Six
   ADT^A08 at the 750 cell. This also falsifies a sentence ADR-0187 put
   in the consumer doc ("the messages differ only in MSH-10") and
   ADR-0188's attribution of its message movement to the reference
   rule; both are corrected.
6. **`bin/preflight` was not clean.** The `Integration` workflow run at
   `164fae67` is red, for the 46-invariant pin fixed since.
7. **Describe.** The inpatient row reads `:configured :yes` under the
   key. `describe-version` was left at 1.3.0 on ADR-0187's reasoning;
   the prompt did not rule on it.

## Downstream line

`dense-7500` now carries dispenses in both settings: at 7,500 arrivals
2,461 `:inpatient` and 48 `:community`. The pin is ADR-0189; the
corpora that move are the three keyed `dense-7500` configs and any
corpus generated with `:dispense` in which an admission writes an
order. Event schema 1.12.0, unchanged. Retained corpora are valid
artifacts of the earlier pin. New 28,024 sha-256
`ce0982dec2e5d0dcab2e342a6b8422343a7a09349c1f084b08fa4b0ad9d961ce`
(267,680,351 bytes). The pharmacy turnaround is a per-ward option,
`:dispense-minutes [lo hi]` on a ward of the facility config, minutes,
uniform; unset, an `:ed` ward takes `[5 25]` and an `:inpatient` ward
`[15 60]`, both provisional. An admission-written order with no
dispense was stopped, or its stay closed, before pharmacy filled it.
With chatter or latency configured, message counts move by a handful.

## Close

Appended by the close commits.

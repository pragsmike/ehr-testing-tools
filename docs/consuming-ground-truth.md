# Consuming the ground-truth event stream

**You want the rich semantic stream this simulator produces, not the
HL7v2 messages it lowers that stream into.** This page is the contract
for that: how to invoke it, how to parameterize the mix, what the stream
is guaranteed to satisfy, what it is explicitly *not* guaranteed to
satisfy, and how big it goes before it stops.

It is a companion to [`formats.md`](formats.md#the-event-log), not a
replacement. `formats.md` is the **shape** contract — 28 closed event
kinds, per-kind key tables, one real example each, generated from the
committed schema. This page is the **run** contract: which knobs make
which kinds appear, which of them move the population, and what a green
`ehrt sim check` does and does not certify. Read `formats.md`'s "Read
the top-level vector only" subsection before you write a line of
consuming code; it is the log's sharpest edge and it is not repeated
here.

If you want the *messages* instead, you are on the wrong page — start at
[`docs/README.md`](README.md). If you want to write your own emitter
over this stream, [`use-cases/custom-emitter-from-the-event-log.md`](use-cases/custom-emitter-from-the-event-log.md)
is the worked path with two runnable examples; this page is what that
page is written against.

Every figure below was measured against this repository's own tree, at
the commit this page landed on, by running the commands shown. Where a
figure comes from the traffic-scale measurement programme instead, it is
cited to `.agents/plans/2026-08-24-traffic-scale-program.md`'s appendix.

## The invocation

Three ways in. All three produce the same vector of the same event maps;
they differ only in what else you get.

| Command | You get |
|---|---|
| `ehrt sim run --seed N --patients M --format ground-truth` | the **bare EDN vector** on stdout, nothing around it — redirect it to a file, or pipe it to `ehrt sim check` |
| `ehrt sim run --seed N --patients M` | the full result envelope, `{:status :ok :payload {:ground-truth [...] :manifest {...} :summary {...}}}` |
| `ehrt corpus generate sim --seed N --patients M --out-dir D` | `D/events.edn` (the same vector), `D/manifest.edn` (provenance), and one `D/msg-NNN.hl7` per message (the lowered messages you did not ask for; the index is zero-padded to whatever width that corpus needs, so file order is lexical order) |

`--seed` is required and has no default. Determinism here is a feature,
not a fallback.

The first form is the one to build against:

```bash
bin/ehrt sim run --seed 42 --patients 20 --churn --format ground-truth > events.edn
```

**`--format ground-truth --json` still emits EDN**, deliberately — the
flags read as "the bare ground-truth vector, as JSON", and that is not a
thing this command produces. For JSON, drop `--format` and take
`payload.ground-truth` out of the envelope ([`formats.md`](formats.md#json)).

**The stream carries no wall clock.** Every event's `:t` is an integer
count of seconds since the run began. Anchoring to a real date happens
at *emit* time, from `--reference-date`; a consumer of ground truth
never needs a date parser. See [Time](#time) below.

## The mix, and why the default is thin

**The interesting parts of this stream are opt-in, and nothing about a
bare invocation tells you so.** Two runs, same seed, same patient count,
same `--churn`, differing only in whether a `--config` file is supplied:

```bash
bin/ehrt sim run --seed 42 --patients 20 --churn --format ground-truth
bin/ehrt sim run --seed 42 --patients 20 --churn --config demos/scenarios/ed-tuesday/config.edn --format ground-truth
```

| | events | distinct kinds |
|---|---|---|
| no `--config` | **74** | **7** |
| `ed-tuesday`'s own `config.edn` | **399** | **18** |

The bare run produces `:admission` `:bed-swap` `:cancel-transfer`
`:discharge` `:registered` `:step-rejected` `:transfer` and nothing
else. The configured one keeps six of those seven and adds twelve:
`:appointment` `:bed-status-change` `:cancel-admit` `:coverage-change`
`:demographic-update` `:merge` `:no-show` `:order-placed`
`:outpatient-visit` `:outpatient-visit-end` `:reschedule`
`:result-available`.

**Five times the events and more than twice the vocabulary, from a
configuration file.** Be precise about what that file changed, because
it changed two different kinds of thing: it scripts clinical content
(`:pathways`, `:facility`, `:modules`, `:order-profiles`) *and* it takes
the four opt-in keys the next section is about. To separate them: the
minimal config at the end of this section — the four opt-in keys and
their emission companions, no pathways and no modules — gives **261
events across 15 kinds** at the same seed and patient count, so the
opt-in keys alone account for most of the gap.

A consumer who benchmarks their transform against a bare
`ehrt sim run` has benchmarked it against the thinnest stream this
simulator produces.

### The opt-in law

Three rules govern every key on this page, and they are worth stating
once rather than per key.

1. **Absent is not the same as false.** A key omitted *entirely* is the
   byte-identical path — the run produces exactly what it produced
   before that key existed. Writing `:encounters false` is not the same
   statement and is not what the byte-identical guarantee covers.
2. **An engine key draws; an emission key does not.** Engine keys are
   fact generators: turning one on consumes randomness and therefore
   *reshuffles the whole population*, so the same seed with and without
   it describes two different hospitals. Emission keys are pure
   functions of a log that already exists; turning one on cannot move a
   single ground-truth byte.
3. **`{}` means "on" for exactly one key.** `:siu {}` renders all four
   scheduling kinds, because that key has nothing to configure but
   *which* kinds. `:chatter {}`, `:ladders {}` and `:charges` with an
   empty table are off, because their settings are the whole of what
   they do.

### Engine keys — these change the facts

Supplied through `--config PATH` (an EDN map), except where a flag is
noted. Each of these reaches the simulation itself.

| Key | Flag | Effect on the mix |
|---|---|---|
| `:seed` | `--seed` | required; the whole run's randomness |
| `:patients` | `--patients` | how many scripted arrivals |
| `:arrival-gap` | `--arrival-gap` | maximum minutes between arrivals; the shift's density |
| `:warm-up-seconds` | `--warm-up-seconds` | events before this `:t` carry `:warm-up true` |
| `:pathway` / `:pathways` | — | the scripted clinical trajectories, weighted or per-ordinal; the main lever on clinical content |
| `:facility` | — | wards, bed counts, surge slots, per-ward `:turnaround-minutes`; the capacity the population runs against |
| `:providers` | — | the attending pool |
| `:churn-profile` | `--churn` | the full churn map; the bare flag takes sensible defaults. Produces `:merge`, `:cancel-*` and the other correction traffic |
| `:order-profiles` | — | which analytes an ordered panel resolves to |
| `:persona-config` | — | demographic and payer pools |
| `:modules` | — | vendored Synthea-format module names to compile clinical trajectories from |
| `:module-assignment` | — | which patient ordinals walk which module |
| `:module-horizon-days` | — | how far forward a module walk is compiled |
| `:module-initial-attributes` | — | seed attributes for a module walk's entry state. The one key here that is *config-facing*: `ehrt.sim.run` folds it into the resolved module closures rather than passing it on, so it is not itself a `config-keys` member |
| `:history` | — | whether pre-horizon clinical history is attached to `:registered` |
| **`:persons`** | — | **the demographic fold.** `{:count N :years Y}` runs a population of `N` people through `Y` years of life alongside the clinical shift: residence moves, coverage changes, births, occupational injuries, unidentified arrivals. Produces `:demographic-update`, `:coverage-change`, and the identity traffic below |
| **`:encounters`** | — | **the encounter horizon.** Truthy lifts the one-encounter-per-patient wall and mints an `:encounter-id` at every opener, so a returning person opens a *second* encounter on the same patient and the same MRN — which is what an MPI under test needs to see |
| **`:bed-cycle`** | — | **the bed-status cycle.** Truthy makes a vacated bed go `:dirty` → `:cleaning` → `:ready` at each ward's own pace, gates allocation on `:ready`, and produces `:bed-status-change` |
| **`:scheduling`** | — | **appointments.** Splits arrivals scheduled-vs-walk-in and books follow-ups at discharge; produces `:appointment`, `:reschedule`, `:appointment-cancel`, `:no-show` |

The four bold rows are the ones that make the stream rich, and the
three below `:persons` did not exist in any corpus this repository
produced before 2026-08-26. The canonical list is
`ehrt.sim-engine.config/config-keys` — nineteen keys, one per row above
except that `:pathway` and `:pathways` share a row and
`:module-initial-attributes` is not a member — and it carries a per-key
comment for each. Every member of it reaches the engine whether it
arrived by flag or by `--config`, which is that list's own gated
plumbing-completeness property.

### Emission keys — these cannot change the facts

These ride `--config` alongside the engine keys and are read only when a
message is rendered. **A ground-truth-only consumer can ignore this
whole table**, which is precisely the point of it being a separate
table: the same seed and the same engine keys give you the same
`events.edn` whatever you do here.

| Key | What it renders |
|---|---|
| `:site-profile` | your site's local dialect — MSH identity, code-table overrides, Z-segments |
| `:latency` | per-event-type transmit delays, so MSH-7 diverges from the clinical instant and messages arrive out of order |
| `:chatter` | re-statement traffic: an ADT^A08/A31 per demographic or coverage change, an A28 per registration, plus a periodic census of open encounters |
| `:charges` | a DFT^P03 per encounter close, one FT1 per priced fact, from a price table you supply |
| `:ladders` | order/result status restatements at fixed fractions of an order's own wait |
| `:siu` | whether scheduling's four kinds reach the wire at all; `{}` renders all four, `{:triggers [...]}` narrows it |
| `:fan-out` | a subscriber table — extra spools, each a byte-exact subsequence of the base one |

Each of `:chatter`, `:charges`, `:ladders`, `:siu` and `:fan-out` is
schema-**closed**: a misspelled sub-key is a rejected configuration
rather than a rule that silently never fires. Their schemas are
`ehrt.sim-model.config`'s `ChatterProfile`, `ChargesProfile`,
`LadderProfile`, `SiuProfile` and `FanOutSubscriber`.

### An authored example

`demos/scenarios/ed-tuesday/config.edn` is this repository's own worked
example. It carries **fifteen** of the keys above — `:facility`,
`:arrival-gap`, `:pathways`, `:modules`, `:module-assignment`,
`:module-horizon-days`, `:persons`, `:encounters`, `:bed-cycle`,
`:scheduling`, `:chatter`, `:charges`, `:ladders`, `:siu` and
`:fan-out` — with a long header comment on each explaining why its
values are what they are, including the several that were tuned by
measurement rather than chosen. Its sibling
`config-latency.edn` is the same file plus a `:latency` block. `demos/scenarios/clinic-decade/config.edn`
is its deliberate contrast: population-scale ambulatory incidence over
ten years rather than one scripted ED shift. Read one of those before
writing your own.

The minimal rich config referred to above — the four opt-in keys and
their emission companions, and nothing else. Save it anywhere and pass
it as `--config`; at seed 42 and 20 patients it gives 261 events across
15 kinds, against the bare run's 74 across 7:

```clojure
{:persons {:count 40 :years 20}
 :encounters true
 :bed-cycle true
 :scheduling {:scheduled-fraction 0.30
              :lead-time-days [1 14]
              :no-show-rate 0.10
              :reschedule-rate 0.08
              :cancel-rate 0.07
              :follow-up {:rate 0.25 :interval-days [7 30]}}
 :chatter {:demographic-update 1.0
           :coverage-change 1.0
           :registered 1.0
           :restatement {:rate-per-patient-day 0.25}}
 :charges {:price-table {"58410-2"    {:amount 148.00 :display "CBC panel"}
                         "ROOM-BOARD" {:amount 1875.00 :display "Room and board, per day"}}}
 :ladders {:rungs [0.5] :order-rungs [0.25]}
 :siu {}}
```

**Two sizing facts that are measurements, not preferences**, both from
`ed-tuesday`'s own header comment. `:persons`' `:count` wants to be
roughly **twice** the arrival count: with `:persons` on, an arrival
*binds* to a person, and at one person per arrival the birthday paradox
makes better than a third of arrivals repeats, thinning clinical
content. And `:years` wants to be long enough for the rare hooks to
fire — at ten years that scenario's population produced **zero**
occupational injuries, because the hazard is conditioned on employed
person-years; at twenty it fires.

**A run's log extends to the *person* horizon, well past its clinical
content.** With `:persons {:count 200 :years 20}`, `ed-tuesday`'s
scripted ED shift happens in the first ~35 hours and the remaining two
decades are those people's own lives. Plan for a stream that is mostly
demographic tail.

## Determinism

**Same seed + same config + same generator version ⇒ byte-identical
`events.edn`.** That is the guarantee, and it is a *within-version*
one. The three facts a reproduction needs are all stamped into the run's
own `manifest.edn` (see [Provenance](#provenance)).

What does **not** enter the log, verified rather than assumed:

- **The host clock, locale and timezone.** The log carries no absolute
  time at all. Measured directly: the same command run under
  `TZ=Asia/Tokyo` and under the host's own `America/New_York` produced
  the same SHA-256 over `events.edn`.
- **File order and message rendering.** `:latency` reorders the
  `msg-NNN.hl7` files and moves their MSH-7 values; it reaches no
  engine key and moves no ground-truth byte. Witnessed in
  `demos/scenarios/ed-tuesday/README.md`'s "The second clock" section as
  a `diff` and a matching pair of digests.

What **does** invalidate a reproduction:

- **A different generator version.** `:generator {:name "ehrt.sim"
  :version ... :sha256 ...}` is the cross-version key. Seed stability is
  not claimed across versions.
- **A different RNG stream scheme.** `:stream-scheme` is a top-level
  string in every sim manifest — `"1.0"` today, and a corpus generated
  before the stream partition carries no such key at all. It is a
  **discriminator, not a warranty**: two corpora with the same seed and
  config and *different* stream schemes are expected to differ, and the
  marker says so on the artifact's face instead of making you resolve a
  version against a changelog. `ehrt.sim-engine.streams/stream-scheme`'s
  own docstring is the authority.
- **Turning an engine key on or off.** Every engine key in the table
  above is a fact generator, so it draws, and a draw reshuffles
  everything downstream of it. Adding `:bed-cycle true` does not add bed
  events to an otherwise unchanged run; it produces a different run.
  Emission keys carry no such hazard.

## Identity

Six identifier kinds, and an MPI consumer needs to keep them apart.

| Identifier | Where it appears | What it identifies |
|---|---|---|
| `:person-id` | on `:registered`, and on person-scoped events | **a human being**, across their whole modelled life. Present only when `:persons` is on |
| `:patient-id` | under `:participants`, on every event about a patient | **a medical record** at this facility. `"PID-000000-1522c269"`. One person may hold several. A `:bed-status-change`'s participant is a bed instead — `{:bed-id :ward :role}` — so a participant map is not always a patient |
| `:active-mrn` | most events | the MRN that record currently answers to. `"MRN000001"`. A `:merge` moves MRNs between records |
| `:encounter-id` | openers and their content, when `:encounters` is on | **one visit**. `"ENC-000000-00-4a75c0cb"`; renders as PV1-19 |
| `:appointment-id` | scheduling kinds, and the encounter kept against a booking | **one booking**. `"APT-000017-01-24a12efa"` |
| bed | `:location {:ward :bed :placement}` | a physical bed. `"ED-H08"` |

**Partition a log by `:participants`, never by `:active-mrn`** —
`:active-mrn` is absent from `:bed-swap`, `:merge` and `:step-rejected`.

### Placeholder, fill and merge

This is the shape an MPI is actually tested by, and it is worth reading
in full.

- **A placeholder registration** is an unidentified arrival: a
  `:registered` carrying `:identity :placeholder`, a `Doe` alias, no
  address, and a `:window-close-t` — the instant by which the record is
  due to be resolved.
- **A fill** resolves it in place: a `:demographic-update` carrying
  `:cause :identity-fill` and a `:placeholder-event-id` indexing that
  registration. Same patient record, now with a real name.
- **An identification merge** resolves it by joining: a `:merge`
  carrying `:cause :identification`, whose `:merged` participant is the
  placeholder and whose `:survivor` is the patient that same person
  already had.
- **An ordinary merge** carries **no** `:cause` — it is churn-injected
  duplicate-record traffic and is not an identity resolution.

A corpus is allowed to contain a placeholder that is never resolved
because an ordinary churn merge consumed it first. That is a real MPI
failure shape, the simulator is telling the truth about it, and it is
counted rather than prevented — see [What is not warranted](#what-is-not-warranted).

## Time

- **`:t` is an integer count of seconds since the run began.** Not a
  wall-clock instant; there is no `#inst` anywhere in a log.
- **`:t` never decreases within a run**, so you may stream the vector in
  order rather than sorting it. This is a *run*-level property:
  concatenating two logs breaks it, and nothing in an event marks a run
  boundary.
- **The horizon is whatever the config asks for**, and with `:persons`
  on that is the *person* horizon, not the clinical one. A twenty-year
  `:years` gives a twenty-year log however short the clinical shift is.
- **Log order is not wire order.** The event log is in run order. With
  `:latency` on, the rendered messages are sorted by *transmit* time
  instead, so the `msg-NNN.hl7` file order and the log's order disagree
  on purpose. Ground truth is invariant under this; only the rendering
  moves.

## What `ehrt sim check` certifies

```bash
bin/ehrt sim run --seed 42 --patients 20 --churn --format ground-truth \
  --config demos/scenarios/ed-tuesday/config.edn | bin/ehrt sim check
```

`sim check` runs **44 invariants** over the log and reports each one it
ran, by name, in `:payload :invariants-checked`. Exit 0 with `:status
:ok` means every one held; exit 1 with `:status :rejected` and
`:category :invariant-violation` carries a `:violations` vector naming
the invariant, the patient and the instant for each. The catalog, in
reporting order, is `ehrt.sim-check.check/catalog` plus its three
config-needing siblings:

`timestamps-monotone` `discharge-follows-admission`
`every-event-has-participants` `participant-ids-exist-in-run`
`admission-only-when-no-open-encounter` `discharge-closes-an-open-encounter`
`every-encounter-is-opened-and-closed-or-still-open` `transfer-only-when-admitted`
`transfer-from-matches-state` `no-double-occupancy` `admitted-occupies-one-slot`
`cancel-references-existing-uncancelled-event` `bed-swap-both-admitted-before-swap`
`merge-survivor-absorbs-merged-mrns` `no-events-after-merged-terminal`
`step-rejected-reason-is-documented` `order-only-when-admitted`
`result-references-existing-order-and-follows-it-in-time`
`abnormal-flags-consistent-with-value-vs-range` `registered-is-every-patients-first-event`
`registered-persona-is-schema-valid` `outpatient-patients-occupy-no-bed`
`clinical-content-only-when-admitted`
`medication-end-references-existing-order-and-follows-it-in-time`
`care-plan-end-references-existing-start-and-follows-it-in-time`
`expired-patient-retains-location` `identity-fill-references-its-placeholder-registration`
`identification-merge-survivor-is-the-persons-prior-patient`
`every-placeholder-registration-is-resolved-or-still-open`
`no-resolution-after-a-placeholder-is-consumed`
`demographic-update-reports-a-real-change` `no-demographic-event-after-a-patient-expires`
`person-scoped-provenance-is-a-stamp-not-a-reference`
`no-assignment-to-a-non-ready-bed` `every-ready-follows-a-cleaning`
`bed-cycle-transitions-are-legal` `appointment-reference-resolves`
`scheduled-encounter-follows-its-appointment` `no-show-has-no-encounter`
`appointment-reaches-at-most-one-terminal` `occupancy-within-capacity`
`surge-only-when-earlier-rungs-exhausted` `warm-up-mark-matches-window`
`result-analytes-match-order-profile`

Two things about that list that a reader will otherwise get wrong.

**`sim check` reads the DEFAULT facility, warm-up window and order
profiles.** It takes a log on stdin and has no flags at all, so it
cannot know what `--config` produced the log. **Four** of the 44
invariants need config the log does not carry —
`occupancy-within-capacity` and `surge-only-when-earlier-rungs-exhausted`
need the facility, `warm-up-mark-matches-window` needs the warm-up
window, and `result-analytes-match-order-profile` needs the order
profiles — and all four are checked against the shipped defaults
regardless. Measured: `ehrt sim check` over
`ed-tuesday`'s own corpus — a scenario whose `:facility` raises the
Emergency ward's surge slots from 6 to 16 — reports **115
`:occupancy-within-capacity` violations, every one of them spurious**,
because the check is comparing a 16-slot ward against the 6-slot
default. **The run's own self-check, which `ehrt sim run` and
`ehrt corpus generate sim` perform in-process, does not have this
problem for the first three** — it passes the real facility and the real
warm-up window (`ehrt.sim.run`'s call is `check/check-all ground-truth
facility warm-up-seconds`). It does **not** pass order profiles, so
`result-analytes-match-order-profile` runs against the shipped defaults
even there. If your corpus overrides `:facility` or `:warm-up-seconds`,
trust the run's own self-check over a piped `sim check`; if it overrides
`:order-profiles`, neither one is checking what you configured.

**Seven of the 44 are vacuous on a log that does not opt in.** The three
bed-cycle invariants are no-ops on a log with no `:bed-status-change`,
and the four scheduling invariants are no-ops on a log with no
`:appointment` — but all 44 are still listed in
`:invariants-checked`. A green check on a thin log is a weaker statement
than a green check on a rich one, and the report does not distinguish
them for you. Count the kinds in your own log.

## What is not warranted

**This section is the other half of the one above and is meant to be
read with the same weight.** A green `sim check` is a statement about
the 44 invariants in that catalog and about nothing else.

**Known-open behaviours the catalog permits by construction.**

- **A cancel-discharge re-opens an encounter that never closes.** A
  legal `:cancel-discharge` re-opens the encounter its own `:discharge`
  closed — deliberately; a reinstated stay is one encounter — and
  nothing ever re-queues a closer for it. At a 10^5-event run, **55 of
  55 cancel-discharges re-open and 54 have no closer of any kind** for
  the remaining ~144,000 events. The catalog permits this in as many
  words: `every-encounter-is-opened-and-closed-or-still-open` reads *"or
  still open"*, so a stay that never ends is green, and the patient
  keeps `:class :inpatient` holding the bed the reinstatement gave back,
  which `admitted-occupies-one-slot` requires. **It is invisible to
  every gate in the catalog today.** Tracked at
  `.agents/plans/roadmap.md#cancel-discharge-reopens-an-encounter-that-never-closes`.
- **A churn merge may consume an unresolved placeholder.** An ordinary
  `:merge` carrying no `:cause` can name an open-window placeholder as
  its `:merged` participant, destroying a resolution that was seeded and
  never decided. The catalog *tolerates* this shape rather than
  forbidding it — `every-placeholder-registration-is-resolved-or-still-open`
  reads resolved-or-**consumed**-or-still-open — because an erroneous
  merge eating a John Doe is a real MPI failure and the corpus is
  telling the truth about it. `no-resolution-after-a-placeholder-is-consumed`
  is the companion gate that keeps the tolerated clause safe — no
  resolution may follow the merge that consumed its placeholder — and it
  IS in the catalog, so `sim check` runs it. Beside it,
  `ehrt.sim-check.check/placeholder-dispositions` sorts every placeholder
  into six disjoint columns, of which `consumed-by-churn` is one, so the
  tolerated shape is **counted rather than hidden**. That census is a
  function this repository's own tests call and not a row `sim check`
  reports: to see your own corpus's split you have to call it. Do that
  before concluding a corpus's identity traffic is clean.

**Every hazard rate in the person process is authored-provisional.** No
cited table stands behind any number in `person-simulator`: each is a
general-knowledge order of magnitude, with no source read and no source
cited. The argument for shipping them anyway is that traffic realism is
insensitive to whether the residence-move rate is 0.11 or 0.13 and very
sensitive to whether moves happen at all. Do not read a rate off this
stream and use it as an epidemiological figure.

**Two gated limitation registers say what each simulator deliberately
does not model**, each row carrying its own reason and its own enforcing
test:

- `components/person-simulator/docs/limitations.md` — twins and
  multiples excluded; the population is closed (no immigration or
  emigration); no foster placement or adoption; a death outside care
  mints no wire event; legal name change and data-entry correction are
  collapsed; geography is a 24-row address pool with no adjacency;
  household structure has no wire surface at all (no emitter writes NK1,
  so next-of-kin never reaches a message); every pregnancy reaches a
  delivery; and **the engine tells the person process nothing** — there
  is no feedback edge, so an admission cannot delay a move and a death
  in care cannot end a residence.
- `components/patient-simulator/docs/limitations.md` — the clinical
  trajectory layer's own equivalent.

**And the standing scope fence.** This workspace does not do semantic
correctness checking of your transforms, full terminology validation
against licensed vocabularies, production message routing, or hosted
validation. [`what-is-this.md`](what-is-this.md#scope--what-this-deliberately-does-not-do)
is where that is stated normatively; the root
[`README.md`](../README.md#maturity)'s maturity table is the
per-capability contract.

## Scale

Measured on the traffic-scale programme's own reference machine — WSL2,
6c/12t i7-10750H, 15 GiB, OpenJDK 21.0.7, JVM defaults as shipped
(`MaxHeapSize` 3.88 GB; `bin/ehrt` sets no JVM options). Full method and
run parameters in `.agents/plans/2026-08-24-traffic-scale-program.md`'s
appendix.

**10^5 events is comfortable; 10^6 is not, and the reason is the
emitter.**

| Cell | events | messages | msg/event | in-process wall |
|---|---|---|---|---|
| all nine opt-in keys | 171,864 | 233,286 | **1.3574** | 270.37 s |
| the same, less `:bed-cycle` | 129,415 | 165,946 | **1.2823** | 232.67 s |
| no opt-in key at all | 105,214 | 67,638 | **0.643** | 118.9 s |

The first two rows are the traffic-scale programme's own closing
measurement of 2026-08-29; the third is its `old` continuity series.
Each is one seed at one machine, sampled as warm-up plus two timed runs;
read them as an order of magnitude, not as a benchmark.

**Messages per event rises with scale rather than settling.** The
all-keys series reads **1.050 → 1.217 → 1.357** across 10^3 → 10^4 →
10^5 and was still climbing at the last point measured. Against the
0.643 of a corpus with no opt-in key — itself stable to three places
across a full decade — the opt-in payload is worth **1.63× the message
volume per event at 10^3, and 2.11× at 10^5**.

**Generate dominates, and it is super-linear.** Log-log slope over the
10^4 → 10^5 decade, all-keys series: generate **1.624**, the person
layer **1.061** (linear, and 13% of the cell), check **sub-linear at
0.914**. The remaining generate-side super-linearity is tracked at
`.agents/plans/roadmap.md#performance-residual-sites`.

**Ground-truth-only is the cheap path, and it is the only one that
reaches 10^6.** Peak heap by phase at a 10^5 cell: generate 500 MB,
check 807 MB, **emit 987 MB**, spool 577 MB. Projected to 10^6, the
retained event log is **1.18 GB and fits comfortably**, while the emit
phase's message vector projects to **9.87 GB against the 3.88 GB
ceiling** and peak RSS to 15.5 GB against the machine's 15 GiB. **The
10^6 cell was declined on that arithmetic, and the binding constraint is
emission, not the log.** A consumer who takes `--format ground-truth`
and writes their own emitter is not paying it. Emit and spool together
are 11% of a 10^5 wall (13.0 s of 118.9 s), so on wall clock the saving
is modest; on heap at 10^6 it is the whole difference between running
and not.

## Provenance

Every generated corpus carries a `manifest.edn`. **A sim corpus's
manifest is not the shape [`formats.md`](formats.md#the-corpus-manifest)
tabulates** — that section documents the external-generator case, whose
worked example is a Synthea run with `:seeds {:master :clinician}` and a
subprocess `:invocation`. A sim manifest looks like this:

| Field | Value in a real sim manifest | What it is for |
|---|---|---|
| `:schema-version` | `"1.1"` (a **string**) | the manifest schema, not the event contract |
| `:stage` | `:simulated` | which pipeline stage produced it |
| `:generator` | `{:name "ehrt.sim" :version "0.1.0-pre" :sha256 "1b486fa2…"}` | the engine, by content hash — the cross-version reproduction key |
| `:seeds` | `{:primary 20260811}` | the seed. One key, not Synthea's two |
| `:event-schema-version` | `"1.8.0"` | which version of the event contract this log satisfies |
| `:stream-scheme` | `"1.0"` | which RNG stream partition produced it |
| `:engine-params` | the engine keys this run used | the flag-reachable half of the config |
| `:invocation` | `{:verb "run" :opts {…}}` | **the whole config, engine and emission keys alike** |
| `:config` | `{:path :sha256}` | the `--config` file by content hash; `"(inline)"` when there was none |
| `:environment` | `{:locale :timezone :jvm-version}` | provenance only. The log carries no wall clock, and the timezone half of that is verified directly under [Determinism](#determinism) |
| `:runtime` | absent for a sim run | the JVM artifact record, for externally-generated corpora |
| `:canonicalizers-applied` | `[]` | ordered `[id version]` pairs |

**To cite a corpus in a bug report, quote four fields**: `:seeds`,
`:generator`, `:stream-scheme` and `:event-schema-version`, plus the
`--config` file (or `:invocation :opts` if it was inline). Those four
are exactly what makes a reproduction attempt either succeed or fail
loudly, and `:invocation :opts` is the only place the emission keys are
recorded at all.

`ehrt sim identifiers --seed N --config PATH` is the companion command:
config plus seed to the complete inventory of every identifier the run's
output would contain — patient-ids, MRNs, beds, control ids, resource
ids, NPIs, run-id — which is how you find and remove synthetic data that
reached a real system.

## Fault injection, as it stands today

Four mechanisms exist, across three layers — and the event layer holds
two of them, which is the distinction worth reading carefully.

| Layer | Mechanism | What it produces |
|---|---|---|
| **Event — behaviour** | `:churn-profile` / `--churn`, at generate time | *correct* traffic that is awkward: merges, cancels, transfers-in-error, duplicate records. Real behaviours a real hospital produces |
| **Event — content** | `ehrt sim mutate --operator-id … --seed …` | *incorrect* content injected into the ground-truth log itself, with the finding it should trip declared up front. Every emitter downstream inherits the one mutated truth. See [`operators.md`](operators.md) |
| **File** | `ehrt corpus mutate --operator-id …` | *incorrect* content injected into rendered messages, each with an expected finding attached. See [`operators.md`](operators.md) |
| **Transport** | `:latency`, `ehrt corpus batch` | *correct* transport behaviour: delayed transmission, schedule batching, straddled encounters |

The taxonomy is worth keeping straight, and the two event-layer rows are
why: churn and transport realism simulate correct behaviour
deterministically — the world really did that, and the record is right.
Mutation injects incorrect content — the world was right, and the
*record* is wrong. Message loss and duplication sit on the boundary and
are a named, unresolved taxonomy question.

**The event-level catalog exists now, and it is one operator deep.**
`ehrt sim mutate` is a filter, so the loop is something you can type:

```bash
ehrt sim run --format ground-truth --seed 5 --patients 60 --config demos/scenarios/clinic-decade/config.edn \
  | ehrt sim mutate --operator-id phantom-placeholder-event-id --seed 424242 \
  | ehrt sim check
```

Run without the middle stage, `ehrt sim check` exits 0. With it, the
checker reports exactly the finding that operator declares —
`:identity-fill-references-its-placeholder-registration` — and nothing
else. That equality is the contract, not a description of one run: an
operator naming a defect class it does not produce, or producing
findings it did not name, fails the build. Applied with no
`--operator-id` at all, `ehrt sim mutate` passes its input through
byte for byte, so the stage costs nothing when it is not wanted.

Each operator mutates exactly ONE site per application, chosen by one
draw from its own seed — independent of the run's seed, so it works on
any log, including one whose run seed you don't have. Re-running with
the same seed reproduces the same mutant; a different seed injects
somewhere else. For many faults, apply it many times with many seeds.

What is *not* there yet: the catalog is one operator, deliberately —
the spine, built to prove the whole contract end to end. The full
family (one operator per reference field per defect shape, plus
structural operators for dropped events, clock skew and orphan
participants) is the next session's work. Until then, mutating the
vector yourself between `--format ground-truth` and your own consumer
is still supported, and `ehrt sim check` is still the oracle that tells
you what you broke.

**And three whole fault layers are missing, not just this one.**
[`future-features.md`](future-features.md) is the menu — wrong bytes
inside a message, wrong sequence with the messages intact, wrong framing
— with the design stance on each and the layer boundary that makes them
different faults rather than three ways of saying the same thing.

## Where this comes from

- [`formats.md`](formats.md#the-event-log) — the shape contract, generated from
  [`event-schema.edn`](../components/sim-engine/resources/sim-engine/event-schema.edn)
- [`use-cases/custom-emitter-from-the-event-log.md`](use-cases/custom-emitter-from-the-event-log.md) — the worked path, two runnable emitters
- [`glossary.md`](glossary.md) — the vocabulary this page assumes
- [`cli.md`](cli.md#ehrt-sim) — every flag `ehrt sim` accepts, generated from the CLI's own spec
- `ehrt.sim-engine.config/config-keys` — the canonical engine-key list, with a comment per key
- `ehrt.sim-model.config` — the five emission-key schemas
- `ehrt.sim-check.check/catalog` — the invariant catalog
- `demos/scenarios/ed-tuesday/config.edn` and `demos/scenarios/clinic-decade/config.edn` — the authored examples

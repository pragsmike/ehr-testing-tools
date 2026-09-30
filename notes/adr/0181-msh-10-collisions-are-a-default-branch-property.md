## ADR-0181 — MSH-10 collisions are a property of the default branch, not of `:result-available`

**Status:** Accepted 2026-09-23 — **candidate 3, the log index**
(author ruling, "1. 3"), landed the same day in `32344ca9`.

**As first written this record was Proposed and enacted nothing**: it
was an author-ruled halt, *characterize only*, and its measurement
section below is unchanged from that day. Every line number in it is
at `e81f7026`. What the ruling added is the decision the closing
section asked for, recorded in "The shape that landed" at the foot of
this file; read the measurement first, because the shape only makes
sense against it.

The session was prompted to close
`roadmap.md#oru-control-id-collision` by giving `:result-available` a
four-part MSH-10 keyed on its own `:order-event-id`. Step 1's own
premise did not hold against the live tree, the session halted under
`docs/dev/way-of-working.md` §2, and the author ruled characterization
only. What follows is the measurement that halted it.

### Context

`control-id-for` (`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj:61-97`)
mints MSH-10. Four arms carry a discriminator — SIU keys on
`appointment-id`, `:bed-status-change` on bed plus target status,
`:bed-swap` on both MRNs, `:merge` on the survivor — and everything
else falls to the default, `active-mrn` then trigger then `t`.

Arc 4 sweep 3 measured that key non-injective and rowed it
(`.agents/session-records/2026-08-28-arc-4-sweep-3-status-ladders.md:288-303`).
The row, and eight readers after it, name the defect
`:result-available`. **That attribution is wrong**, and the row's own
evidence already said so: sweep 3's finding 1 wrote that the colliding
messages are `:observation` / `:diagnostic-report` ORUs sharing the
default control-id branch, and the row generalized the branch to one
event type that happens to sit on it.

### The measurement

Seven roots, generated this session at their own committed opts — the
four `gated-runs` of `ehrt.sim.run-test`
(`components/sim/test/ehrt/sim/run_test.clj:427-470`) plus three demo
scenarios, each at the seed and arrival count its own shipped exerciser
runs (`bin/demo-exerciser-dense-7500:93`,
`bin/demo-exerciser-clinic-decade:74`). `control-id-for`
applied to every ground-truth event; duplicates counted by whole id.

| root | events | `:result-available` | `:observation` | `:diagnostic-report` | dup groups |
|---|---|---|---|---|---|
| `seed-202-ed-tuesday` | 1,213 | 30 | 0 | 0 | 0 |
| `seed-424242-clinic-decade` | 1,774 | **0** | 19 | 2 | 2 |
| `seed-5-clinic-decade` | 1,412 | 0 | 14 | 0 | 0 |
| `adhd-seed-45` | 66 | 0 | 0 | 0 | 0 |
| `ed-tuesday` `config-latency` | 1,213 | 30 | 0 | 0 | 0 |
| `clinic-decade` demo @ seed 20260807, 200 | 1,603 | **0** | 18 | 3 | **3** |
| `dense-7500` @ seed 20260824, 750 arrivals | 33,306 | 1,028 | 780 | 240 | **43** |

**Finding 1 — neither rowed corpus carries any `:result-available` at
all.** `seed-424242-clinic-decade`'s six duplicate MSH-10s in two groups
are `MRN000189-R01-119086260` (five `:observation`, one
`:diagnostic-report`) and `MRN000189-R01-125739060` (one of each). One
patient, one ED bundle, one instant. The `clinic-decade` demo, which
the row prices at **one** duplicate, in fact carries **three groups
covering ten messages** — `MRN000015` twice and `MRN000171` once, same
composition, same absence of orders. Three event types render `ORU^R01`
— `:result-available`, `:observation`, `:diagnostic-report`
(`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/registry.clj:59`,
`:147`, `:152`) — and the two that collide here carry **no
`:order-event-id`** (measured absent, not nil) and no `:concept`. The
key shape the session was prompted to land is therefore *unavailable*
to them, and applying it to `:result-available` alone leaves this
corpus at two groups and six duplicates, unchanged. That was measured,
not reasoned: the probe re-keyed the log both ways and compared.

**Finding 2 — the rowed collision has an empty population everywhere.**
`:result-available` against `:result-available` on one
`(active-mrn, t)`: **zero occurrences in all seven roots**, including the
1,028 `:result-available` events of the `dense-7500` cell. The defect
the row names is real by construction — two results for one patient at
one second would collide — but no corpus this project ships or gates
has ever produced one. It is reachable in principle: order turnaround
is a whole number of minutes drawn per order
(`components/sim-engine/src/ehrt/sim_engine/decide.clj:1543-1544`) over
the shipped profiles' 30-to-90 and 45-to-120 minute ranges, so two
results coincide exactly when their orders' times are congruent modulo
sixty seconds and the minute draws close the gap.

**Finding 3 — the live defect is five classes wide, and ORU is the
minority.** The `dense-7500` cell carries 43 duplicate groups covering
86 messages:

| class | trigger | groups |
|---|---|---|
| `:transfer` with `:transfer` | A02 | 25 |
| `:cancel-transfer` with `:cancel-transfer` | A12 | 8 |
| `:result-available` with `:observation` | R01 | 6 |
| `:result-available` with `:diagnostic-report` | R01 | 2 |
| `:merge` with `:merge` | A40 | 2 |

Thirty-five of the forty-three are ADT, not ORU. The ruled key shape
would resolve the eight R01 groups here — each holds exactly one
`:result-available`, so giving that side a discriminator separates the
pair — and none of the other thirty-five.

**So the defect is not an ORU defect and not a `:result-available`
defect.** It is a property of the default branch: the
mrn-trigger-t key is non-injective wherever two events of one trigger
family land on one patient in one second, and which families that bites
is a function of what a corpus contains, not of anything about orders.
`:merge` is the sharpest reading of that: it already HAS a
discriminator arm, keyed on the surviving MRN, and it still collides
when one survivor absorbs two records at one instant.

### What this costs the gates that already exist

`ehrt.conformance.mllp-pairing-test`
(`projects/conformance/test/ehrt/conformance/mllp_pairing_test.clj:13-57`)
regenerates `seed-424242-clinic-decade` and asserts the duplicates are
still there before asserting anything about positional ACK pairing — a
deliberate guard against going vacuous. Its second assertion requires
every duplicated id to match `-R01-`, captioned as catching "a NEW
collision shape appeared, outside the rowed one". **That caption is
corpus-limited, not false**: it would fire on the `dense-7500` cell
above, where 35 of 43 groups are A02/A12/A40 — but that cell is not
what the gate runs, so the four non-ORU classes have been live and
unseen. The gate is not wrong about what it watches; it watches one
root.

### The question handed back

Not decided here. The design channel owes a ruling on the key's
*shape*, and the three candidates the measurement leaves standing are:

1. **Per-arm discriminators**, the path the tree is already on (SIU,
   bed-status, bed-swap, merge). Each colliding family gets a field it
   already carries — `:order-event-id` for `:result-available`, the
   primary LOINC code for `:observation`/`:diagnostic-report`, and
   something not yet identified for `:transfer`/`:cancel-transfer`.
   Additive and local; but it is five more arms, each owing its own
   injectivity argument, and `:merge` shows an arm is not by itself a
   guarantee.
2. **A uniform ordinal on the default branch**, the shape chatter
   already uses
   (`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/planners.clj:249-270`,
   mrn-trigger-t then ordinal). One change, covering every class
   including ones no corpus has produced yet. But the emitter sees one
   event at a time through `event->messages`, so an ordinal has to be
   threaded from the caller — an API change, not an arm change.
3. **The log index**, which the 2026-08-28 fan-out ruling already made
   the project's identity of record — identity is the log index, never
   MSH-10
   (`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/fan_out.clj:12-20`,
   `components/corpus/src/ehrt/corpus/generators.clj:166-171`). Total,
   ordered, and minted by nothing. Same threading problem as (2).

Whichever lands moves HL7 bytes in every corpus carrying a colliding
family — at minimum `seed-424242-clinic-decade` and the `dense-7500`
cells — and is a declared oracle change. **Ground truth does not move
under any of them**: MSH-10 is minted at emission time out of fields
the log already carries.

### Fence (as of the Proposed record, 2026-09-23 — superseded below)

This record does not: change `control-id-for`; decide a key shape; add,
retire or weaken any gate, including the conformance tripwire that goes
vacuous the day a fix lands; or close the roadmap row, which stays
**OPEN** and is rewritten in this same commit to name the population
actually measured rather than the one it inherited. The eight readers
that stated the `:result-available` attribution are corrected in this
commit too — prose only, no assertion and no fixture value changes.

**That fence was lifted the same day, by the ruling below.** It is
kept verbatim because it is what the measurement was published under.

---

## The shape that landed

**Ruled by the author 2026-09-23 ("1. 3"): candidate 3, the log
index.** Landed in `32344ca9`, over the red tests of `85eb29ea`.

### The key

Every arm of `control-id-for` — the default `mrn-trigger-t`, the SIU
four-part key, `:bed-status-change`, `:bed-swap` and `:merge` — now
ends in a **marker plus the event's own 0-based position in the log**:

    MRN000189-R01-119086260      ->  MRN000189-R01-119086260#4021

Three properties fall out, and each is asserted rather than asserted
about:

1. **The old id is a strict PREFIX of the new one**, in every arm. A
   downstream reader holding a pre-change id finds its successor by
   prefix.
2. **The suffix names the event's place in the log**, so any message
   joins back to the ground-truth event that produced it without a
   lookup table. That is the same identity `fan_out.clj` was already
   built on — *identity is the log index, never MSH-10* — so this
   ruling makes MSH-10 carry the identity the project already had
   rather than inventing a second one.
3. **Injectivity is by construction, not by argument.** No arm owes an
   injectivity case of its own any more, which is the direct answer to
   finding 3: `:merge` already HAD a discriminator and still collided
   with itself.

### The marker is `#`, and that is load-bearing

Channel-recommended, and verified rather than assumed. Every
restatement this emitter makes — chatter's A08/A31/A28 and the
ladder's O01/R01, both through
`planners/assign-restatement-ordinals` — already mints
`mrn-trigger-t-<ordinal>`. A `-` marker would have given ground-truth
ids that same four-part shape and collided the two families the first
time a log index equalled an ordinal at one `(mrn, trigger, t)`. `#`
keeps them disjoint by construction: a ground-truth id contains it, a
restatement id never does. It is not an HL7 v2 delimiter (`|^~\&`), so
it needs no escaping and moves no field boundary.

`assign-restatement-ordinals`' own docstring said "a ground-truth
event's own id has NO ordinal suffix". True of the three-part key,
false from this commit, corrected in place.

### The mechanism, and why ground truth does not move

One function, `segments/stamp-log-index`, applied at each of the five
funnels that hand events to `control-id-for`: `emit`, `emit-wire`,
`plan-latency`, `plan-ladders` and `ehrt.sim.identifiers`. They have
to agree event for event — `emit-wire` looks a latency offset up under
the id `plan-latency` minted, and a ladder rung rides its basis
event's — so they stamp the same whole log through the same function
rather than five `map-indexed`s. The stamp goes on BEFORE any filter,
never inside one, or the index would number the survivors rather than
the log.

The key is namespaced (`:ehrt.sim-emit-hl7.segments/log-index`) and
lives on each call's own copy of the log. It reaches no engine output,
no `check`, and no EDN this project writes. `control-id-for` **throws**
on a registered event that carries no stamp: falling back to the
three-part id would mint something no reader could tell from a
pre-change id and that collides exactly as before, silently. The
registry is consulted first, so an unregistered event is still nil,
stamped or not.

### Measured

The `dense-7500` 750-arrival cell's full wire, rendered at `2b52fc57`
and at the landing tree — 40,291 messages both sides:

| | before | after |
|---|---|---|
| duplicate MSH-10 groups | 43 | **0** |
| messages sharing an id | 86 | **0** |
| longest MSH-10 | 46 chars | 52 chars |

21,518 ids moved and 18,773 did not; every moved id passes the prefix
law (0 violations). **Stripping every `#<digits>` from the after-wire
reproduces the before-wire byte for byte**, so the marker and index are
the only bytes this change moves anywhere.

Two parsers agree, over 543 messages — the 86 formerly-colliding ones
plus the first 25 of each of the 20 MSH-9 families:

- `ehrt.corpus-io.er7/parse` round-trips all 40,291 messages exactly on
  both sides, with stable MSH field counts.
- `gate v2` (HAPI tier): 543/543 pass, zero findings, both sides.
- `gate v2-nist` against `COVID19_ELR-v2.3.1`: an identical
  finding-class histogram, code for code and count for count —
  including `structure/Length Spec Error` at 17,069 on both sides,
  which is the length question answered. Per-file verdict, finding
  count and codes identical across all 543.

Ground truth held: `bin/ground-truth-bracket 2b52fc57 HEAD` IDENTICAL
on all 38 digested roots. The declared oracle change is
`bin/regression-oracle 2b52fc57 HEAD --declared-digest-change`: **36
roots DIFFER, 5 are IDENTICAL.** The five are the three batch roots
that carry no `:hl7` key at all (`appendicitis`, `ear-infections`,
`sore-throat`) **plus `dermatitis` and `veteran-self-harm`, whose
`:hl7` half is present and EMPTY** — 301 and 300 ground-truth events,
zero messages, so there is no MSH-10 to move. The rule is "every root
that renders at least one message differs, and only those".

### What this ruling does NOT cover

**`messages/dft-message` mints its own MSH-10 inline, `mrn-P03-t`,
without going through `control-id-for`** — so DFT^P03 keeps a
three-part key and stays non-injective for two encounter closes of one
patient at one second. 2,609 of the cell's 40,291 messages are DFT^P03
and not one of their ids moved. Found by the before/after render, not
by reasoning. It is the same defect this record names, in the one
family that does not read the fixed function; fixing it is a BUILDER
change, which the landing session's own charter excluded, so it is
rowed as `roadmap.md#dft-control-id-collision` rather than smuggled in.
No duplicate DFT id occurs in any root the new gate runs, so like this
record's own `:result-available` case it is reachable-in-principle
rather than witnessed.

### Gates

`ehrt.sim.run-test/control-id-for-is-injective-over-every-corpus-this-
lane-runs` is the all-message gate this record said was missing: MSH-10
read off the WIRE over the four gated corpora **plus the `dense-7500`
750 cell**, which is what makes the four non-ORU classes visible for
the first time. `ehrt.sim-emit-hl7.control-id-log-index-test` states
the local law over hand-built events, including the fail-closed throw.

`ehrt.conformance.mllp-pairing-test` was REBUILT, not weakened. Its
"the duplicates are STILL THERE" premise is false by construction now,
so it is retired; the pairing law is stated over the same real spool
with a **hand-built** duplicate pair appended, because a `--sink
mllp://` carries foreign corpora too and positional pairing has to
survive a duplicate whoever minted it. No test asserts any more that a
shipped corpus carries duplicate MSH-10s.

### One correction to the measurement above

The table's `adhd-seed-45` row reads 66 events. Re-run 2026-09-23 at
`ehrt.sim.run-test`'s own `gated-runs` opts, verbatim and complete, that
run produces **97**. The duplicate-group count is 0 either way, so no
finding moves; the figure is corrected here rather than left to be
re-derived.

### Dated amendment, 2026-09-30 — DFT^P03 joins the index rule; the gate reads every rendered id's shape

Author ruling 2026-09-30, "go", on the session that closed
`roadmap.md#dft-control-id-collision`. Landed in `39e51282`.

**DFT^P03's MSH-10 is `mrn-P03-t#<log index>`**, the closing event's
own index, appended through `segments/with-log-index` — now the ONE
place the suffix is appended; every arm of `control-id-for` ends in it
too, so the throw on an unstamped event is shared rather than copied.
The pre-change id is a strict prefix of the new one, which is property
1 above extended to the family "What this ruling does NOT cover" named.
An `:outpatient-visit-end` has no registry entry and so no
`control-id-for`, but it is stamped like every event, so its DFT takes
the suffix all the same. The DFT's latency offset is still looked up
under the basis event's own `control-id-for`, unchanged.

A hand-built log with two `:discharge`s for one MRN at one `t` renders
`MRN000001-P03-91000#3` and `...#4`; before, both were
`MRN000001-P03-91000`.

**The gate.** `control-id-for-is-injective-over-every-corpus-this-lane-
runs` already read MSH-10 off the rendered wire — DFTs, restatements
and rungs included — so its injectivity clause was GREEN at `64d55fec`
(0 duplicates in every root). What it could not see was a family
minting ids beside the rule while happening not to collide. It gains a
SHAPE clause: every MSH-10 ends `#<digits>` (rendered from a
ground-truth event) or is a restatement's `mrn-trigger-t-<ordinal>`
over the five restatement triggers. Red at `64d55fec` on exactly the
DFTs: 114 / 124 / 96 / 8 at the four gated roots, 2,609 at the
`dense-7500` 750 cell. Green at `39e51282`.

**Measured**, at the four gated roots and the 750 cell rendered at
`64d55fec` and `39e51282`: every DFT id moved (2,609 at the cell), no
message of any other family moved on this account, and stripping
`#<digits>` from every DFT MSH-10 (together with the PV1-2 restoration
ADR-0174's dated note of the same day describes) reproduces the
before-wire byte for byte at all five. `gate v2` (HAPI) passes 68/68 on
both sides over every changed ORU plus 40 DFTs; `gate v2-nist` against
`COVID19_ELR-v2.3.1` gives an identical finding-code histogram and
identical per-file codes. `bin/ground-truth-bracket 64d55fec 39e51282`
IDENTICAL on 39 roots; `bin/regression-oracle ... --declared-digest-
change` DIFFERS on exactly `chatter-charges` (99 DFTs) among the roots
this amendment moves. Record:
`.agents/session-records/2026-09-30-wire-fidelity-pv1-dft.md`.

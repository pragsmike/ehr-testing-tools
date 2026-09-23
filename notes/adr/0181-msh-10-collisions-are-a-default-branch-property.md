## ADR-0181 — MSH-10 collisions are a property of the default branch, not of `:result-available`

**Status:** Proposed (author-ruled halt, 2026-09-23 — *characterize
only*). **THIS RECORD ENACTS NOTHING.** No arm of `control-id-for`
changes, no key shape is decided, no gate is added or retired. Every
line number below is at `e81f7026`, read this session.

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

### Fence

This record does not: change `control-id-for`; decide a key shape; add,
retire or weaken any gate, including the conformance tripwire that goes
vacuous the day a fix lands; or close the roadmap row, which stays
**OPEN** and is rewritten in this same commit to name the population
actually measured rather than the one it inherited. The eight readers
that stated the `:result-available` attribution are corrected in this
commit too — prose only, no assertion and no fixture value changes.

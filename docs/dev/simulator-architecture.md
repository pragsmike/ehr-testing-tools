# Simulator Architecture

**Status:** dev-docs (R34), descriptive — an aid to understanding the
simulator's own theory, and a guardrail against feature work drifting
from it (author charter, 2026-08-11, `notes/ADRs.md` ADR-0108: *"I want
to document this architecture in the tools repo, as that's where the
implementation is. This is more of an aid to understanding the design,
as well as a guide for agents to avoid departing too much from the
established theory when adding features."*). Made load-bearing by a
co-landed purity lint (`components/docs-tooling/test/ehrt/docs_tooling/
sim_purity_lint_test.clj`) — this doc states the census the lint
enforces; the lint keeps this doc's own state-isolation claim from
going stale silently. Wired into the agent reading path: any session
prompt fencing sim-family `src` carries this doc in Read-first
(standing channel practice, ADR-0108).

**Scope:** the seven `sim-*` bricks plus `sim` itself — what each one
is for, the decide/evolve event-sourcing doctrine that ties them
together, the mutable-state census that makes the purity claim
checkable, and the diagrammatic-composition (palgebra) reading of the
whole pipeline. Not a tutorial (`docs/dev/engine-onboarding.md` is
closer to that) and not a design record with open decisions
(`docs/dev/source-sink-design.md`, whose register style this document
mirrors, is that for corpus I/O) — every claim below is a
**description of what the tree already does**, cited to the file, ADR,
or test that establishes it, not a proposal.

**Companion:** `notes/ADRs.md` ADR-0108 is the reasoning-of-record for
this document's own charter and the lint's landing; `sim/ADR-0008`
(`notes/sim/ADRs.md`, frozen provenance) is the reasoning-of-record for
the decide/evolve split itself, which this document only restates and
cites, never re-derives.

Every fact asserted below about the current codebase was re-read from
source while writing this record (2026-08-11): `components/sim-engine/
src/ehrt/sim_engine/engine.clj`, `components/patient-simulator/src/ehrt/
patient_simulator/{gmf,gmf_interpreter,compile_trajectory,census}.clj`,
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/{interface,v2_replay}.
clj`, `components/sim-emit-fhir/{src,test}/ehrt/sim_emit_fhir/*`,
`components/sim-check/src/ehrt/sim_check/check.clj`, `components/sim/
src/ehrt/sim/{run,version,manifest}.clj`, and every brick's own
`interface.clj`.

---

## 1. The seven bricks

Each brick's own `interface.clj` is the re-export census this table is
built from — grep it directly for the full var list; this table names
only what a reader orienting to the *architecture* needs first.

| Brick | Job | Interface entry points |
|---|---|---|
| `sim-model` | Pure schemas and sampling: pathway IR, facility/ward allocation, `Persona`, provider config. No engine, no RNG threading of its own beyond what its sampling functions take as an argument. | `components/sim-model/src/ehrt/sim_model/interface.clj` |
| `patient-simulator` | GMF module loading and interpretation: `load-closure` resolves a module (plus its `CallSubmodule` targets and lookup tables) into a closure; `run-module` walks it, RNG-driven, into a `Trajectory`; `compile-trajectory` reshapes that trajectory into pathway IR the engine can execute. This is "the GMF walk." | `components/patient-simulator/src/ehrt/patient_simulator/interface.clj` (`load-closure` → `gmf.clj:1580`; `run-module` → `gmf_interpreter.clj:2161`, driving `walk-module` → `gmf_interpreter.clj:2061`; `compile-trajectory` → `compile_trajectory.clj:1`) |
| `sim-engine` | The discrete-event core: the `decide`/`evolve` multimethod pair (`sim/ADR-0008`), the `run` loop, `replay`, plus the churn and order-profiles catalytics. | `components/sim-engine/src/ehrt/sim_engine/interface.clj` (`run` → `engine.clj:1242`; `replay` → `engine.clj:1059`) |
| `sim-emit-hl7` | Ground-truth log → HL7v2 ER7 messages (`emit`), plus the wire-side replay accumulator (`fold-message`, `v2_replay.clj`) a stranger's own paced stream can be folded through — the same accumulator the emitter-coherence property reasons about. | `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/interface.clj` |
| `sim-emit-fhir` | Folded state (never the log directly) → FHIR R4 Bundle (`bundle-run`, built on the pure `snapshot-at`/resource-builder functions). A rendering accent over state, sim-emit-hl7's sibling, not its dependent. | `components/sim-emit-fhir/src/ehrt/sim_emit_fhir/interface.clj` |
| `sim-check` | The invariant catalog (`check-all`): internal-consistency claims over a ground-truth log, built on `sim-engine/replay` — the same fold `evolve` always was, reused rather than reimplemented. | `components/sim-check/src/ehrt/sim_check/interface.clj` |
| `sim` | Mount: orchestration only (`run.clj`'s `run-command`, `identifiers.clj`, `version.clj`, `manifest.clj`), a deliberately wide, frozen-shape façade (`ehrt.sim.interface`) re-exporting result envelopes (`ok`/`rejected`/`error`), commands (`run-command`, `check-all`, `identifiers-command`), and manifest/version identity. Every other sim concern lives one layer down, in its own brick. | `components/sim/src/ehrt/sim/interface.clj` |

Dependency direction (`AGENTS.md` Constraints, enforced by `poly
check`): `sim-model` depends on nothing but `kernel`; `patient-simulator`
depends on nothing but `sim-model` and `kernel`; `sim-emit-hl7` depends
on nothing but `sim-model` (never `sim` or `patient-simulator`);
`sim-emit-fhir` depends on nothing but `sim-engine`; `sim-check`
depends on nothing but `sim-engine`, `sim-model`, and `kernel`; `sim`
depends on all of them plus `provenance`, and never the reverse.

---

## 2. The decide/evolve doctrine

`sim/ADR-0008` (`notes/sim/ADRs.md`, frozen provenance, accepted
2026-07-26) replaced a single fused `transition` function with a pair,
stated verbatim in `engine.clj`'s own ns docstring (`engine.clj:10-23`,
the primary citation — everything below restates that text, never
supersedes it):

- **`decide`** — `(rng, t, world, patient-id, step) -> {:events [...]
  :advance <seconds>}` (`engine.clj:259`, a `defmulti` dispatching on
  `(:type step)`). Reads `world` — every patient's own state so far,
  plus static facility/provider config — read-only, and consumes the
  run's single seeded RNG, to decide what happens. **Never returns a
  new state.** This is where cross-patient coupling lives: a
  discharge's own `decide` call may emit a `:transfer` event for a
  *different*, boarding patient (`engine.clj:424-460`, the
  bed-ready-transfer coupling), and a merge names a survivor and a
  merged patient in one call (`engine.clj:581-613`).
- **`evolve`** — `(patient-state, event) -> patient-state'`
  (`engine.clj:857`, a `defmulti` dispatching on `(:event event)`).
  Pure and total: no RNG, no knowledge of the step or decision that
  produced the event, no knowledge of `world` or of any patient but the
  one the event names. This is the **only** function that ever
  produces a new patient state.

**Patient state exists only as the fold of `evolve` over events.**
`replay` (`engine.clj:1059-1088`) is that fold, re-run: given a
ground-truth log, it walks every event, folding each participant's own
slice of state through `evolve`, and returns a parallel sequence of
`{:event :patient-id :before :after :world-before :world-after}`
records. `sim-check`'s entire invariant catalog is built on this same
`replay` (`components/sim-check/src/ehrt/sim_check/check.clj:21-23`,
its own header docstring: *"These read patient/world state via
`ehrt.sim-engine.engine/replay` — the same fold `evolve` always was,
reused rather than reimplemented"*) — this is the oracle regime: a
log's own claimed state is never trusted as a second source of truth,
only ever re-derived by folding.

---

## 3. State isolation: the mutable-state census

**Zero atoms, refs, agents, or volatiles in the simulation path**,
across all seven bricks' `src` — verified by grep against every `.clj`
file under `components/{sim-model,patient-simulator,sim-engine,
sim-emit-hl7,sim-emit-fhir,sim-check,sim}/src` for `(atom `, `(ref `,
`(agent `, and `volatile!`/`set-validator!` as call forms:

```
$ grep -rn '(atom \|(ref \|(agent \|volatile!\|set-validator!' \
    components/sim-model/src components/patient-simulator/src \
    components/sim-engine/src components/sim-emit-hl7/src \
    components/sim-emit-fhir/src components/sim-check/src components/sim/src
components/patient-simulator/src/ehrt/patient_simulator/census.clj:407:    fetched (atom {id root-json-text})
```

**Two named exceptions, both outside the decide/evolve/replay path
itself:**

1. **`census.clj`'s probe-fetch memoization atom** (`components/
   patient-simulator/src/ehrt/patient_simulator/census.clj:407`, inside
   `walk-one`). `fetched` records every distinct module/table file a
   census walk actually reads, so a `:load-failed` row can report an
   honest `:closure-file-count` without re-fetching. This is a curation
   tool's own bookkeeping over a *census run* (`ehrt sim census`), not
   simulation state — no `decide`, `evolve`, or `run` call ever reads
   or writes it.
2. **`version.clj`'s git read** (`components/sim/src/ehrt/sim/
   version.clj:19-37`, `git-sha`). Not an atom/ref/agent/volatile at
   all — no mutable-state primitive appears in the grep above for this
   file — but a real impurity nonetheless: a `slurp` of `.git/HEAD`
   (and, one level of indirection deeper, the ref file it points at),
   wrapped in `try`/`catch` returning `nil` on any failure. Read for
   manifest/version identity only (`ehrt.sim.manifest`'s `:generator`
   block, `sim version`/`--version`) — never consulted by `decide`,
   `evolve`, `run`, or `replay`.

**The one deliberate impurity inside the simulation path itself is
`java.util.Random`** (`engine.clj:67`, `(:import [java.util Random])`)
— seeded once in `run` (`engine.clj:1413`, `(Random. ^long seed)`),
explicitly **threaded** as `decide`'s own first argument rather than
held in any var or atom, with **fixed consumption per draw site**
(`engine.clj:48-56`, the Determinism doctrine paragraph: *"ALL
randomness flows from the single `java.util.Random` seeded in `run`...
Same config + seed => identical output, byte for byte"*). This is the
RNG-path law (`.agents/rulings.md`, "Measurements sample the claimed
population, standing," AR-RL2-2, `notes/ADRs.md` ADR-0092): a
measurement or sweep claiming to characterize the simulator's own
output must draw from this exact seeded, threaded path — never an
independent synthetic RNG assumed equivalent to it. `assign-pathway`
and `assign-module` (`engine.clj:1165-1217`) are the load-bearing
worked examples: each *always* consumes exactly one `.nextDouble`,
whether or not the draw's own outcome is used, specifically so that
adding one scripted override never shifts every other patient's
downstream draws.

---

## 4. The palgebra

Reading the pipeline as resource equations (`docs/dev/notation.md`),
using that document's own product operator (`×`) and the diagrammatic
(left-to-right, "then") composition operator **⨟** (U+2A1F) —
**never** the infix ring-compose `∘`, whose right-to-left reading
inverts the order this pipeline actually executes in:

```
walk    : RNG × Persona × Closure → Trajectory
engine  : RNG × Config → GT          (per-step: decide, then evolve-fold over World)
emitH   : GT × Params → ER7*          emitF : GT × Params → FHIR*
replay  : GT → (State × State)*       check = replay ⨟ invariants
board   = split ⨟ fold-message*
```

Read left to right: `walk` (`patient-simulator`'s `run-module`) takes the
run's own RNG, a sampled `Persona`, and a loaded `Closure`, and
produces a `Trajectory`. `engine` (`sim-engine`'s `run`) takes the RNG
and a `Config` map and produces `GT`, the ground-truth log — internally,
one `decide` call per pending step followed by an `evolve`-fold of that
call's own events into `World`. `emitH`/`emitF` are `sim-emit-hl7`'s
`emit` and `sim-emit-fhir`'s `bundle-run`: both consume the *same* `GT`
object plus rendering `Params` (reference date, UTC offset, site
profile), producing ER7 messages or a FHIR Bundle respectively — two
independent renderings of one ground truth, never one derived from the
other. `replay` is `sim-engine`'s own function of that name, producing
a sequence of before/after state pairs; `check` is exactly that fold
followed by the invariant catalog (`sim-check`'s `check-all`) —
`replay ⨟ invariants`, not a separately-computed pass. `board` names a
consumer *outside* this document's own seven bricks (`corpus`'s player
board, `notes/ADRs.md` ADR-0067 AR-BB2-1): splitting a paced HL7 v2
stream into individual messages, then folding each one through
`sim-emit-hl7`'s exported `fold-message` (`components/sim-emit-hl7/
src/ehrt/sim_emit_hl7/interface.clj:35-38`) — the Kleene-star marks
that `fold-message` applies once per message in the stream, not once
overall.

### Two honest wrinkles

**`engine` is not two independent parallel folds; it is ONE fold over
a shared `World`.** A naive reading of `engine : RNG × Config → GT` as
"fold each patient's own stream independently" is wrong: `run`'s own
loop (`engine.clj:1534-1541`) folds every event's own participants into
the *same* `world` value, one event at a time —

```clojure
world' (reduce (fn [w ev]
                  (reduce (fn [w2 {:keys [patient-id]}]
                            (update-in w2 [:patients patient-id] evolve ev))
                          w (:participants ev)))
                world events)
```

— because `decide`'s own cross-patient coupling (§2) means the *next*
patient processed may need to read a state another patient's own
`decide` call just wrote. Drawing this as a diagram means one merge
node accumulating every patient's own wire into a single `World` box,
never two side-by-side parallel wires that never touch.

**The GMF walk is an *unfold* meeting `evolve`'s *fold*.**
`walk-module`/`run-module` (`gmf_interpreter.clj:2061`, `:2161`) drive
`step` repeatedly from a seed `ctx`, growing a `Trajectory` one
transition at a time until a Terminal state or horizon bound stops it
— an anamorphism: seed in, sequence out. `compile-trajectory` reshapes
that sequence into pathway IR; the engine's own `decide`/`evolve` pair
then folds each IR step's resulting events back down into
`PatientState` (§2) — a catamorphism: sequence in, accumulated value
out. The `Trajectory` (and, one stage later, the ground-truth log) is
the seam where the walk's own unfold and `evolve`'s own fold meet —
not one continuous operation, two dual ones sharing a boundary.

### The naturality witness

`emitH`/`emitF`'s own claim — that both renderings, drawn from one
`GT` object, resolve the *same* patient identity — is not asserted by
inspection; it is a passing property test, the witness this document
cites by name: `fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-
identity` (`components/sim-emit-fhir/test/ehrt/sim_emit_fhir/
emit_fhir_test.clj:147`, a 150-trial `defspec`), which generates random
runs and checks that `sim-emit-fhir`'s own `Patient.id`/`identifier`
resolve to the identical `patient-id`/`active-mrn` pair
`sim-emit-hl7`'s own PID-3 renders, over every generated case. This is
the naturality square's own commutation, proven rather than assumed.

### The two layers, instantiated

`palgebra-design.md`'s two-layer read (D5, §I.4) applies to this
pipeline directly, not by analogy: **`GT` is the sim's abstract-layer
object** — content on wires, no infrastructure, the layer where "two
workflows are equal iff their abstract diagrams are equal" (§I.4). The
sim purity lint (`components/docs-tooling/test/ehrt/docs_tooling/
sim_purity_lint_test.clj`, the mechanical check behind this doc's own
§3 mutable-state census) is that same rule read mechanically: **zero
atoms, refs, agents, or volatiles in the simulation path** (modulo
§3's own disclosed exceptions) is exactly "no infrastructure on
abstract-layer wires," enforced by grep rather than merely asserted. The lint and the layer are the same discipline
seen from two sides — one names it in prose, the other checks it in
CI.

**The founding thesis as algebra.** This project's own founding
sentence (`docs/glossary.md`, "Emitter": *"Formats are just emitters
of the patient state machine"*) is a layer claim: `emitH` and `emitF`
are two **lowerings** of one abstract object, `GT`, never two
independent computations that happen to agree. Read this way, the
naturality witness cited just above
(`fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`,
`emit_fhir_test.clj:147`, already cited in full above) is the
**coherence law** between those two lowerings — two paths from the
same abstract object to two different lowered targets, proven to
resolve the same identity rather than merely asserted to.

**Honest wrinkle: no `erase` exists from wire back to `GT`.** The
palgebra's own soundness anchor is `lower ⨟ erase = id` (D6, §I.5) —
but the emitter arrows `emitH`/`emitF` are one-way. No total `erase :
ER7* → GT` (or `FHIR* → GT`) is implemented or intended anywhere in
this pipeline; nothing folds a wire stream back into the ground-truth
shape the way `sim-engine`'s own `replay` folds events into
`PatientState` (§2). This is precisely why the regression oracle
freezes bytes rather than re-deriving and comparing abstract objects:
**where erasure doesn't exist, byte-identity of the lowered image is
the checkable surrogate for abstract-object equality.** The machinery
is used where it holds, named absent where it doesn't — never silently
assumed either way.

**Where `lower ⨟ erase = id` genuinely holds, witnessed:**

- **The framing codecs are lower/erase pairs at the transport tier.**
  Each `ehrt.corpus-io.framing` codec's own encode is a `lower`
  (content plus a wire envelope — BHS/BTS, an MLLP wrapper, an NDJSON
  line) and its decode is the matching `erase` (forget the envelope,
  recover the content): `batch-round-trip-property-test`
  (`components/corpus-io/test/ehrt/corpus_io/framing_test.clj:266`)
  and its four sibling round-trips —
  `file-per-item-round-trip-property-test` (`:29`),
  `er7-multi-round-trip-property-test` (`:121`),
  `ndjson-round-trip-property-test` (`:142`),
  `mllp-round-trip-property-test` (`:224`) — are `lower ⨟ erase = id`
  proven once per codec.
- **Pacing is movement within a fiber, erasing to the same unpaced
  bytes.** `ehrt play`'s own real-time delivery is a `lower` — content
  plus a timing infrastructure — and reading it back at an arbitrarily
  large rate, or to a file sink, is the matching `erase`:
  `play-command-at-huge-rate-matches-show-identity-test`
  (`bases/cli/test/ehrt/cli/core_test.clj:2800`) and
  `play-command-file-sink-writes-byte-identical-to-unpaced-content-test`
  (`:2827`) both show the paced rendering and the unpaced content
  erase to identical bytes — pacing changes nothing about the abstract
  object, only how long delivering it takes.
- **The latency second clock's zero point is the identity.**
  `emit-wire`'s own `LatencyParams` argument is a `lower` from `GT` to
  `TimedWire` (§5/ADR-0109); at the identity element of that
  parameter — absent, `nil`, or `{}` offsets —
  `emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`
  (`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj:29`,
  a 100-trial `defspec`) proves the lowering collapses to plain
  `emit`'s own output exactly. The stronger claim — that `GT` itself is
  invariant under ANY latency configuration, not merely the identity
  one — is architectural (latency parameters enter at the emitter seam
  only, never reaching `engine/run`, §5/ADR-0109) and was witnessed
  live rather than merely argued: ADR-0110's own `diff`/`sha256sum` of
  ground truth generated under `config-latency.edn` against the
  unlagged baseline, byte-identical.

**Transport realism versus mutation, as algebra.** The ADR-0111
taxonomy note (`.agents/rulings.md`, "From ADR-0111," "Transport
realism versus mutation, the taxonomy note") restated in layer terms:
transport realisms — delayed individual transmission (ADR-0109),
schedule batching (ADR-0111) — move **within** the erasure fiber: the
abstract object, `GT`, is unchanged, only its lowered image's own
infrastructure (timing, framing) varies. Mutation (`ehrt corpus
mutate`) deliberately produces a **different** abstract object, with
an expected finding attached — a different fiber, not a different
point in the same one. Message loss and duplication remain that
note's own named open boundary, unresolved here: a real transport does
both, and where they sit relative to this transport-realism/mutation
split is a named future question, not decided by this subsection.

---

## 5. Extension point: downstream-latency realism

The roadmap's own next row (`.agents/plans/roadmap.md` Next section;
author charter, `notes/ADRs.md` ADR-0107, 2026-08-11, verbatim: *"lab
results take time to come back, providers take time to log things in
the EHR... we need to supply [downstream receivers] with such cases"*)
will add a second clock between `engine` and the emitters — an arrow
`GT → TimedWire`, sitting between `engine`'s own `GT` output and
`emitH`/`emitF`'s consumption of it, so a message's own wire-emission
instant can lag its clinical-event instant by a realistic, sampled
delay. Named here as the one extension point this document already
anticipates; nothing about it is built, designed, or scheduled by this
session.

**Addendum, 2026-08-11 (ADR-0109): the arrow now exists, HL7v2-side.**
Author ruling (2026-08-11, verbatim "I like a. go", `.agents/
rulings.md`): the second clock lives in `sim-emit-hl7`'s own emitter
seam, `GT × LatencyParams → TimedWire`, keeping `engine`'s own `GT`
pure — never a third RNG-consuming stage between `engine` and
`emitH`. Two new pure functions, `emit-hl7/plan-latency` (`RNG × GT ×
LatencyProfile → offsets`, fixed RNG consumption — exactly one draw
per ground-truth event, draw-and-discard for a type the profile
doesn't cover, the same law `assign-pathway`/`assign-module` already
establish in `engine.clj`) and `emit-hl7/emit-wire` (`GT ×
reference-date × utc-offset × facility × providers × site-profile ×
offsets → TimedWire`, no RNG at all — sampling stays out of emit,
this section's own doctrine, unchanged) — `plan-latency`'s own
sampling is the ONLY new RNG consumption, and it is a second,
independently-seeded `java.util.Random` (`ehrt.sim.run`'s own call
site), never the engine's sealed stream, so `engine`'s own `GT` output
is unperturbed by whether `:latency` is present at all. `emitH`'s
plain `emit` is unchanged and stays byte-frozen (the identity property,
`ehrt.sim-emit-hl7.latency-test`, is the proof); `emit-wire` is the
split-clock sibling, returning messages sorted by TRANSMIT time (not
log order) — out-of-order clinical arrival falls out of the sort, not
a special case. The field audit behind the split (notes/adr/0109-*.md)
found exactly two timestamp-bearing fields rendered by this project's
emitter today: MSH-7 (message/transmit time, now shiftable) and EVN-2
(event/clinical time, on ADT messages only, never shifted); every
order/result/observation message type carries MSH-7 alone. `emitF`
(FHIR) gets no such arrow this session — a named deferral (notes/
adr/0109-*.md), since FHIR resources' own instant fields are a
distinct rendering surface this session's own fence did not open.

**Addendum, 2026-08-16 (ADR-0142): the split clock reaches the result
wire.** ADR-0109's field audit, quoted just above, is what dated this
addendum into existence: it recorded that `OBR-7` and `OBX-14` —
HL7v2's own clinical-time fields on a result message — were simply not
rendered, which is why "every order/result/observation message type
carries MSH-7 alone" was true. It no longer is, for results. Both
fields now render on all three shapes this project emits as `ORU^R01`
(`:result-available`, ORC+OBR+one OBX per analyte; `:observation`, a
single OBX with no order context; `:diagnostic-report`, ORC+OBR+one OBX
per embedded child), carrying the result event's own `:t` through
`hl7-timestamp` exactly as `evn-segment` receives `clinical-ts`, and
never shifting under `emit-wire` — MSH-7 alone carries transmit time.
The arrow itself is untouched: `plan-latency`, `emit-wire` and
`transmit-seconds` have the same signatures and the same behaviour, and
the identity property still holds (zero offsets is still plain `emit`).
What changed is only what a shifted message has to say for itself,
which is the point — a downstream receiver handed a late result can now
back-date it from the message rather than guess.

Two boundaries this addendum draws deliberately. **`ORM^O01` is
byte-frozen**: `obr-segment` renders there too, but OBR-7 means
*observation* time and an order's observation has not happened yet; the
field an order would actually owe is ORC-9, transaction time, and it
stays unrendered and named rather than filled with a plausible-looking
wrong value (author ruling, 2026-08-16, "Results only; ORM
byte-frozen"). And this is an EMITTER-seam change only: the
ground-truth event log's shape is untouched, `:event-schema-version`
stays `"1.0.0"`, and nothing new entered the log — both fields are
rendered from `:t`, which every event already carries. `emitF` remains
the standing deferral it was in 2026-08-11.

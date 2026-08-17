## ADR-0109 — Latency realism: the second clock in the emitter seam

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Author charter, 2026-08-11, verbatim (`.agents/rulings.md`, "From
ADR-0107"): *"I want to make sure that the simulation faithfully
simulates what happens in real life: lab results take time to come
back, providers take time to log things in the EHR, etc. so it's
possible that a downstream receiver of the HL7 traffic will have
incomplete encounter records for some time. That's not our problem to
solve, but in order to test that such downstream receivers handle it
properly (whatever that might mean for them) we need to supply them
with such cases."* ADR-0108's own ratified sequence named this session
as its second step: the architecture doc lands first (done, ADR-0108),
then the latency design pass. The design channel offered option (a) --
the second clock lives in the emitter seam, `GT × LatencyParams →
TimedWire`, keeping ground truth pure, the arrow ADR-0108's own section
5 already named as the one extension point it anticipated. Author
ruling, 2026-08-11, verbatim: *"I like a. go."* This session executes
that ruling as mechanism + tests + doc; the end-to-end demo (the
user-guide trigger's other half, ADR-0108) is a FUTURE session.

### Tag ceremony

Design channel verified the ADR-0108 landing at `d6ed674` by fresh
public clone. `stable-20260811-simulator-architecture-doc` tagged
annotated at `d6ed674`, message "simulator architecture doc landed,
design-channel-verified 2026-08-11 (ADR-0108)"; pushed; peeled ref
confirmed `d6ed67469cbeb8e2d2cfac6ee16fabe4105feb67` -- exact match;
remote had not moved (`git fetch` confirmed `origin/main` already at
`d6ed674` at session start; the last five CI runs on main were all
`completed`/`success`).

### Decision

**[A] The ruling, executed as designed.** `components/sim-emit-hl7/
src/ehrt/sim_emit_hl7/emit_hl7.clj` gains two pure functions --
`plan-latency` (`RNG × GT × LatencyProfile → offsets`, the ONLY new RNG
consumption ADR-0109 introduces) and `emit-wire` (`GT × reference-date
× utc-offset × facility × providers × site-profile × offsets →
TimedWire`, no RNG at all) -- plus a split-clock rendering threaded
through every message builder. `emit`'s own bytes and order are
UNCHANGED (frozen, verified below); `emit-wire` is the split-clock
sibling. `components/sim-model/src/ehrt/sim_model/config.clj` gains the
`LatencyProfile`/`LatencyRange` schema. `components/sim/src/ehrt/sim/
run.clj` gains an optional `:latency` opt, threaded the same
emit-only, never-reaches-`engine/run` way `:site-profile` already is.

### Step 1: the field audit

Every timestamp-bearing field this project's emitter renders, found by
direct inspection of every segment-builder call site in `emit_hl7.clj`
(every call to `hl7-timestamp`, and every segment builder's own
parameter list, grepped and read in full before any code changed):

| Segment | Field | HL7v2 semantics | Classification | Rendered today? |
|---|---|---|---|---|
| MSH | MSH-7 (Date/Time of Message) | The instant the SENDING SYSTEM created/transmitted the message | **Message/transmit time** | Yes -- every message type |
| EVN | EVN-2 (Recorded Date/Time) | The instant the event itself occurred/was recorded clinically | **Clinical time** | Yes -- ADT messages only (single-subject, bed-swap, merge); order/result/observation messages carry no EVN segment at all (HL7v2 convention: EVN is ADT-specific) |
| PID | PID-7 (Date/Time of Birth) | A demographic date, not an event timestamp -- not derived from the event's own `:t` at all (sourced from `persona`) | N/A -- out of this audit's scope | Yes, but untouched by this session |
| PV1 | PV1-44/45 (Admit/Discharge Date/Time) | Clinical time, were it rendered | Would be **clinical time** if ever added | **Not rendered** -- `pv1-segment` renders PV1-1/2/3/6/7/36 only, no positional pad reaches 44/45 |
| ORC | ORC-9 (Date/Time of Transaction) | Clinical/order time, were it rendered | Would be **clinical time** if ever added | **Not rendered** -- `orc-segment` renders ORC-1/2 only |
| OBR | OBR-7 (Observation Date/Time) | Clinical time, were it rendered | Would be **clinical time** if ever added | **Not rendered** -- `obr-segment` renders OBR-1/4 only |
| OBX | OBX-14 (Date/Time of the Observation) | Clinical time, were it rendered | Would be **clinical time** if ever added | **Not rendered** -- neither `obx-segment` nor `observation-obx-segment` renders it |
| Z-segments | Any `:path`-bound field | Site-specific, template-authored | Out of scope -- never a `:t`-derived timestamp field this session's audit governs | N/A |

**Finding: exactly two timestamp-bearing fields exist in this
project's emitter today, and they are already the complete
message-time/clinical-time split** -- MSH-7 (message-time, every
message type) and EVN-2 (clinical-time, ADT messages only). Every
other HL7v2 field this standard would call a clinical-time candidate
(PV1-44/45, ORC-9, OBR-7, OBX-14) is simply not rendered by this
project's builders at all, confirmed by reading every segment
builder's own parameter list and `create-field` calls (none of the
four takes or computes a `:t`-derived value) -- classified
conservatively per the driving prompt's own instruction ("where a
field is genuinely ambiguous, classify conservatively (clinical)"),
though in every one of these four cases there was no live rendering to
be ambiguous about, only a documented absence. This makes the
mechanism unusually simple: `msh-segment`'s own `ts` argument becomes
`transmit-ts` everywhere, `evn-segment`'s own `ts` argument becomes
`clinical-ts` everywhere it is called, and no other builder needed any
change beyond accepting the new `offsets` parameter to compute its own
single MSH-7 argument.

### Step 3: the mechanism

**`plan-latency`** (fixed RNG consumption, the RNG-path law's own
worked precedent in `engine.clj` -- `assign-pathway`/`assign-module`,
`engine.clj:1165-1217` -- extended here): walks `ground-truth` in log
order, drawing exactly one `.nextDouble` PER EVENT regardless of
whether that event's own `:event` type is covered by `latency-profile`
-- draw-and-discard for an uncovered type. A covered event's offset
samples uniformly from its `{:from-minutes :to-minutes}` range
(`sim/ADR-0011`'s own minutes-authored/seconds-engine convention,
mirrored: `(long (Math/round (* 60.0 ...)))`), keyed in the returned
map by `control-id-for`'s own control-id (the same join key
`control-id-for-matches-every-rendered-messages-own-msh-10` already
proves is the canonical GT-event-to-message key). Absent/nil/{}
`latency-profile` still draws once per event -- proven a REAL draw,
not skipped, by `plan-latency-with-an-absent-profile-draws-and-
discards-and-returns-empty` returning `{}` regardless of RNG seed
(the draws happen; they simply produce no map entries).

**Fixed-consumption evidence, non-vacuous
(`plan-latency-adding-a-covered-event-type-never-shifts-another-types-
own-offset`, `ehrt.sim-emit-hl7.latency-test`):** a churn-enabled run
(seed 7, 6 patients) produces a real mix of event types. Offsets
planned under `{:admission {...}}` alone, and under
`{:admission {...} :discharge {...}}`, from the SAME fresh `Random.
123` in each call: every `:admission` control-id's own offset is
IDENTICAL between the two maps (asserted per-entry, not just
aggregate-equal), and the wider profile's map is strictly LARGER (
`:discharge` entries actually appeared) -- proving both that the law
holds and that the test isn't vacuously comparing two empty maps.

**`emit-wire`**: renders the SAME messages `emit` would (via
`event->messages`, now threaded an `offsets` parameter down to every
builder), pairs each with its own `transmit-seconds` (`:t` shifted by
`offsets`' entry for that event's control-id, or unshifted), and
returns them SORTED by `[transmit-seconds log-position]` -- ties break
on log position, stable. No RNG anywhere in `emit-wire` itself
(sampling stays out of emit, docs/dev/simulator-architecture.md
section 5's own doctrine, unchanged).

**Identity property, non-vacuous
(`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`,
100 trials, seed/patients/churn generated):** `emit`, `emit-wire`
called with `nil` offsets, and `emit-wire` called with `{}` offsets all
produce the IDENTICAL vector, byte for byte, in the identical order --
the mechanism guaranteeing this: `transmit-seconds` falls back to a 0
offset for any control-id absent from the map, so every message's own
transmit second equals its own clinical `:t`, and since ground truth is
already `:t`-nondecreasing (the engine's own priority-queue invariant),
the stable `[transmit-seconds log-position]` sort reproduces exactly
`emit`'s own log order.

**Nonzero-latency evidence
(`emit-wire-shifts-msh-7-transmit-time-and-leaves-evn-2-clinical-time-
unchanged`, `emit-wire-msh-only-message-types-shift-their-sole-
timestamp-field`):** a +3600s offset on a real admission's own
control-id moves MSH-7 by exactly that amount and leaves EVN-2
untouched, proven by direct field comparison against `emit`'s own
rendering of the same ground truth; ORM^O01 (order-placed, no EVN)
shifts its own sole timestamp field (MSH-7) the same way.

**Transmit-time ordering
(`emit-wire-orders-messages-by-transmit-time-reordering-a-lagged-event-
past-its-followers`):** a +999h offset on an admission's own control-id
(pathway: admission -> transfer -> discharge) moves that message from
FIRST (log order) to LAST (wire order); every remaining message's own
MSH-7 stays non-decreasing across the whole wire output; the same
clinical events (by trigger + EVN-2, unaffected by the one lagged
message's own necessarily-different MSH-7) appear on both sides, same
multiset, same count -- reordered, never dropped or duplicated.

**`run.clj`/config threading**, exactly the fence's own "config-schema
+ one emit-call-site change" budget: `:latency` destructured alongside
`:site-profile` in `run-command`'s own `let`; the `(= "hl7" emit)`
branch now checks `(if latency ...)`, calling `emit-hl7/plan-latency`
against a FRESH `(java.util.Random. seed)` (a second, independently-
seeded stream -- never the engine's own sealed RNG, so `engine/run`'s
own output is unperturbed by whether `:latency` is present at all) and
`emit-hl7/emit-wire` when present, `emit-hl7/emit` unchanged otherwise.
`:latency` is NOT a member of `engine/config-keys` -- structurally
incapable of reaching `engine/run`, the same `:site-profile` precedent
-- asserted directly in `run-command-threads-latency-into-emit-wire-
transmit-time-ordering`. Proven end to end (`ehrt.sim.run-test`): a
huge-latency admission is pushed past its own followers in
`run-command`'s own `:messages` output; absent `:latency` (or an
explicit `:latency nil`) renders byte-identical to a run that never
named the key; `:latency` rides `:config` the same passthrough way
`:site-profile` does (`run-command-config-file-passthrough-carries-
latency`).

### Step 5: the disorder probe (a finding, nothing fixed)

Probed live against a real run (seed 1, pathway admission -> transfer
-> discharge): the admission's own transmit time shifted +2h, landing
its message on the wire AFTER both the transfer's and the discharge's
own (unshifted) messages -- wire order becomes A02, A03, A01. Folded
through `ehrt.sim-emit-hl7.v2-replay/fold-message` in that disordered
order and diffed against the same messages folded in ordinary log
order:

```
LOG ORDER (baseline):   {:status :discharged, :location nil,
                          :admitted-at 1704067200000,
                          :discharged-at 1704070800000, ...}

WIRE (disordered) ORDER: {:status :admitted, :location
                          {:ward "Renal" :bed "RENAL-01"},
                          :admitted-at 1704074400000,
                          :discharged-at 1704070800000, ...}
```

**Finding, disclosed, nothing fixed this session:** `fold-message` has
no defense against out-of-order arrival. Each trigger's own
`evolve-entry` case applies unconditionally and positionally --
assuming the message it is folding is causally the LATEST fact about
the patient, never checking whether an earlier-arriving message
already carries a later clinical instant. When the admission (A01)
arrives last, its own unconditional `(assoc entry :status :admitted
:location ... :admitted-at t)` clobbers the ALREADY-FOLDED discharge's
`:status :discharged`/`:location nil`, while `:discharged-at`
(untouched by the A01 case) survives from the earlier fold --
producing an internally INCONSISTENT final state: `:status :admitted`
alongside a non-nil `:discharged-at`, and a `:location` reverted to the
pre-transfer ward. This is exactly the class of downstream behavior
the author's own charter (this ADR's Context) wants tested IN OTHERS,
not fixed here: a real downstream receiver folding this same
disordered stream would face the identical inconsistency, which is the
point of supplying it. `fold-message`'s own behavior is unchanged by
this session -- the probe script is disposable (scratchpad only, not
committed); this finding, and its reproduction recipe (admission
control-id, +2h offset, the same pathway above), are the durable
record.

### Named deferrals

- **FHIR-side latency**: `emit-fhir`'s `bundle-run` gets no `offsets`
  parameter, no split-clock treatment, this session. A distinct
  rendering surface (FHIR `Bundle` entries' own instant fields --
  `Encounter.period`, `Observation.effectiveDateTime`, etc.) this
  session's own fence does not open. Revisit trigger: a future session
  extending `GT × LatencyParams → TimedWire` to `emitF`, per ADR-0108
  section 4's own two-independent-renderings palgebra entry.
- **Late amendments (trailing A08s)**: named deferral, standing from
  the driving prompt's own scope fence -- a late amendment is a NEW
  ground-truth EVENT (a correction fact, not a delayed rendering of an
  existing one), GT-side and outside ruling (a)'s own seam (the emitter
  seam only ever renders events that already exist; it invents none).
  Revisit trigger: a future engine-side design pass on correction/
  amendment events, unscheduled.

### Design doc addendum

`docs/dev/simulator-architecture.md` section 5 gains a dated
2026-08-11 addendum naming the arrow now built (`plan-latency`/
`emit-wire`, the second independently-seeded RNG, the field-audit
finding, the FHIR-side deferral) -- the extension point that section
already anticipated, instantiated.

### Commit

`dc5ebad` -- "feat: latency realism -- the second clock in the
emitter seam (ADR-0109)." Files: `components/sim-emit-hl7/src/ehrt/
sim_emit_hl7/emit_hl7.clj`, `components/sim-emit-hl7/src/ehrt/
sim_emit_hl7/interface.clj`, `components/sim-emit-hl7/test/ehrt/
sim_emit_hl7/latency_test.clj` (new), `components/sim-model/src/ehrt/
sim_model/config.clj`, `components/sim-model/test/ehrt/sim_model/
config_test.clj`, `components/sim/src/ehrt/sim/run.clj`,
`components/sim/test/ehrt/sim/run_test.clj`, `docs/dev/
simulator-architecture.md`. Pushed; post-push verification: one delta
against the message file, the known harmless trailing-newline
artifact; ASCII check clean (`git log --format=%B -1 | LC_ALL=C grep
-n '[^ -~]'`, empty).

### Oracle bracket

**Pre-analysis**: `emit`'s own call sites are unchanged (always pass
offsets `{}`); `run-command`'s default (`:latency` absent) codepath
still calls `emit-hl7/emit` unchanged. Expectation: pure identity
across every root -- no oracle root enables `:latency`, and the
identity property test is the local witness of the same fact.

**Bracket result.** `bin/regression-oracle d6ed674 dc5ebad`:
`IDENTICAL: every root's digest matches between d6ed674 and dc5ebad`
-- all 35 roots (the `injuries` root, first-baselined by ADR-0107,
included) byte-identical. Matches the pre-analysis exactly; no
STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all skip:integration`): 612 occurrences of "0 failures, 0 errors"
across the entire output, zero `FAIL`/`ERROR` report lines anywhere,
3 minutes 58 seconds (up from ADR-0108's own 608-occurrence baseline
by exactly the new/grown test namespaces this session added --
`ehrt.sim-emit-hl7.latency-test` new, `ehrt.sim-model.config-test`/
`ehrt.sim.run-test` grown). `ehrt.cli.cli-parse-guard-lint-test`: 4
tests, 22 assertions, 0/0 -- unchanged from ADR-0108's own baseline
(this session never touched `bases/cli`). `ehrt.docs-tooling.
sim-purity-lint-test`: 5 tests, 14 assertions, 0/0 -- unchanged from
ADR-0108's own baseline, confirming this session's own new
`(java.util.Random. seed)` construction (`run.clj`, `latency-test.clj`)
does NOT trip the lint (object construction, not an
atom/ref/agent/volatile). `bin/verify-nist-lock`: OK, 6
hit-nexus-sourced coordinates matched. `gitleaks git --staged -v`
(pre-commit) and `gitleaks detect` (pre-push): no leaks found.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start, before this session's own push): all
`completed`/`success` -- `d6ed674` (ADR-0108 session-record close,
4m30s), `62d1d5e` (ADR-0108 doc landing, 4m41s), `5a2832f` (ADR-0107
CI-flake disclosure, 4m17s), `1b66fb7` (ADR-0107 session-record close,
4m42s), `29392cd` (ADR-0107 injuries batch feat, 4m16s) -- no red
among the five.

### Fences

Touched exactly: `components/sim-emit-hl7/{src,test}`,
`components/sim-model/{src,test}` (schema), `components/sim/src/ehrt/
sim/run.clj` + its test, `docs/dev/simulator-architecture.md` (dated
addendum), plus the usual close-phase register files (`notes/adr/
0109-*.md` this file, `notes/ADRs.md`, `notes/adr/README.md`, `.agents/
plans/roadmap.md`, `.agents/rulings.md`, `.agents/prompts/`, `.agents/
session-records/`). Plain `emit`'s output stayed BYTE-FROZEN (the
oracle and the identity property test are the dual witnesses). GT,
engine, interpreter, check, replay, `fold-message` itself, player,
board, corpus, cli: all untouched -- `fold-message`'s own behavior
under disorder is a recorded finding, not a change. No FHIR-side
changes; no new GT event types; no scenario configs land this session
(the demo session authors those). The purity lint passed with these
additions unallowlisted (`plan-latency`/`emit-wire`/`run.clj`'s own
`(java.util.Random. seed)` are object construction, not an
atom/ref/agent/volatile -- the same pattern `engine.clj`'s own `(Random.
^long seed)` already establishes).

### Deviations, dated 2026-08-11

None. Every step executed as the driving prompt specified.

### Index line

```
- 2026-08-11 — latency-second-clock — ADR-0109
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Latency realism: the second clock in the emitter seam — the author's own "I like a. go" ruling executes option (a), the extension point ADR-0108 named: two new pure functions in `ehrt.sim-emit-hl7.emit-hl7` (`plan-latency`, fixed-RNG-consumption per ground-truth event, the same law `assign-pathway`/`assign-module` establish; `emit-wire`, no RNG at all, sorts messages by transmit time), a `LatencyProfile` schema in `ehrt.sim-model.config`, and an optional `:latency` opt threaded through `ehrt.sim.run` the same emit-only treatment `:site-profile` already gets; a field audit of every timestamp-bearing segment builder finds exactly two rendered fields — MSH-7 (message/transmit time, now shiftable) and EVN-2 (event/clinical time, unshifted) — every other HL7v2 clinical-time candidate (PV1-44/45, ORC-9, OBR-7, OBX-14) simply isn't rendered by this project's emitter at all; plain `emit`'s own output stays byte-frozen, proven both by a 100-trial identity property and the oracle bracket (`IDENTICAL` across all 35 roots); a disorder probe folds a wire-reordered lagged-admission stream through `fold-message` and finds it produces an internally inconsistent reconstructed state (`:status :admitted` alongside a non-nil `:discharged-at`) — disclosed as a finding, `fold-message` itself left untouched, exactly the downstream-receiver behavior this arc exists to expose; FHIR-side latency and late-amendment/A08 events both named as deferrals with their own revisit triggers; the roadmap's downstream-latency-realism row moves from "awaiting design pass" to "mechanism landed, demo half remains"

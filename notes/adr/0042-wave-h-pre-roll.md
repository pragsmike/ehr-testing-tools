<!-- Attic file: notes/adr/0042-wave-h-pre-roll.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0042 — Wave H pre-roll: the history phase lands — opt-in, phase-marked, straddle-safe. GMF parity arc COMPLETE

**Status:** Accepted (author-ruled 2026-08-04, design channel, AR-1
through AR-6 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
2026-08-04.

### Context

Wave H — pre-roll — was ratified as the SOLE remaining wave once
ADR-0041 declared parity (84/85 `:ok-walked` + 1
`:out-of-scope-by-ruling`, ADR-0039 AR-7's own re-ordering having
already put it last so its history-phase design would exercise
against the complete catalog). `docs/gmf-interpreter.md` §3 already
ratified the shape (2026-07-26): ONE continuous walk from DOB, no
fixed tick, `:pre-horizon` marked by `t < registration-t`. Three dated
notes had accumulated on the parity plan's own H row waiting for this
session: emit-nothing REAFFIRMED for the history phase generally
(ADR-0031 AR-3); the UTI closure's own Care Pathways Encounter
reliably straddles `engine.clj`'s fixed registration-t anchor, dodged
by an empirically-chosen seed (777) rather than resolved (ADR-0033's
own execution note, carried forward by ADR-0034 AR-6); `:wellness-
wait` reached during a future history phase must fold state without
emitting, the same discipline every other state type already owes
(ADR-0037 AR-2(c)). This session's own rulings (design channel,
ratified 2026-08-04) resolve all three at once.

### Decision

**AR-1 (phase-marked events, one interpreter).** When history is
enabled, the walk runs DOB → `horizon-end-t` as ONE continuous walk
with a phase boundary at `registration-t`. History-phase events are
MINTED into the trajectory with a `:phase :history` mark; they fold
state effects identically to horizon events (clinical-state channel
per ADR-0027 — conditions, medications, careplans, vitals, attributes,
wellness state); the compile step DROPS them (the ConditionEnd "real,
worth keeping, not worth a message" shape, generalized to a phase). No
second interpreter, no fold-only mode: glass-box traceability keeps
all 45 years inspectable; the wire stays in-window. `:wellness-wait`
needs no special handling — pre-registration ticks are history events
like any other (ADR-0037's note, discharged automatically).

**AR-2 (straddle: encounter-anchored phase inheritance).** An event's
phase is inherited from its ENCOUNTER's opening phase, not its own
timestamp. An encounter that opens in history is history in full — its
contents and its close fold, never emit, regardless of timestamps — so
no orphaned close events, no `:clinical-content-only-when-admitted`
trips. Disclosed v1 cost: a straddling encounter yields no in-window
wire traffic for that patient. CARRY-ACROSS (emitting patients mid-stay
at window open, as real hospital censuses have) is the NAMED FUTURE,
roadmap Deferred, trigger: a test scenario needs mid-stay-at-window-open
realism. Events outside any encounter phase by their own timestamp
against `registration-t`.

**AR-3 (config opt-in — the identity ruling).** History runs ONLY when
the run config requests it (a `:history` flag at the same layer
`:modules` lives, threaded to `run-module`; absent = today's
registration-t start, byte-identical draw streams). This is
load-bearing for AR-5's pure-identity bracket. The census's walks do
NOT enable history (its parity claim is about the interpreter's module
vocabulary, unchanged); a history-census is a possible future, not
this session.

**AR-4 (seed-777 retirement).** With AR-2 landed, the UTI engine
round-trip test drops its hand-picked seed: replace with an ordinary
seed (or the config default), assert the straddle resolves for it, and
add a dated note closing the ADR-0033/0034 linkage. If the test STILL
needs a hand-picked seed after AR-2, that is a STOP-AND-ESCALATE — the
rule didn't do what the design channel claimed.

**AR-5 (oracle — pure identity + new history baselines).** Fresh scan
unnecessary for identity (AR-3's gating is the argument — state it in
the ADR); every existing oracle batch byte-identical (any change
escalates, suspect the gating first). Co-landing: extend `digest.clj`
with history-enabled batches for at least UTI (engine layer, straddle
exercised) and `ear_infections` (wellness ticks folding) and record
their FIRST history-mode baselines in the session record.

**AR-6 (the reconciliation read + escalation).** The existing
`:pre-horizon-facts` mechanism and the engine's registration-t anchor
were NOT read by the design channel. Before implementing: read them,
and record in the ADR whether AR-1/AR-2 subsume them (expected:
`:pre-horizon-facts` becomes the phase fold's landing zone or retires
into it, and the invariant needs no change because history content
never reaches its checked surface). If the existing mechanics conflict
with the rulings in any way phase inheritance does not cleanly
resolve — STOP-AND-ESCALATE with the read, implement nothing.

### AR-6's own reconciliation (read first, recorded before any edit)

**The mechanism, as built (M5b Task 4, `engine.clj`'s own `:registered`
decide method).** Every closure-assigned patient's module walk already
runs `run-module` from DOB (`initial-context`'s own `:t`), unconditionally,
marking each trajectory event `:pre-horizon` by the pure predicate
`t < registration-t` — this is the M5a-as-built interpreter, not
something this session adds. `compile-trajectory` reads that mark and
splits pre-horizon content two ways: `:encounter`/`:encounter-end`/
`:procedure`/`:observation`/`:diagnostic-report`/`:death`/
`:imaging-study` DROP outright (`pre-horizon-dropped-types`);
`:condition-onset`/`:condition-end`/`:medication-order`/
`:medication-end`/`:care-plan-start`/`:care-plan-end` become a
CONDENSED `:registration-facts` entry (`pre-horizon-fact-types`) —
never a pathway IR step. `engine.clj`'s `:registered` decide method
rides `:registration-facts` onto the SAME engine-internal `:registered`
event every patient already gets, as `:pre-horizon-facts`, only when
non-empty. Separately, `engine.clj`'s `ConditionRecord` docstring
already discloses a v1 scope boundary: `:pre-horizon-facts` content is
NOT folded into the engine's own clinical-content accumulator
(`ConditionRecord`/`MedicationOrderRecord`/etc.) — only conditions
attached to a compiled, OPERATIONAL encounter step are. This is the
CDA-style "deferred with a contract note" gap the UTI/TJR/ear-infections
round-trip tests' own docstrings already cite.

**Where the straddle bug actually lives.** `compile-trajectory`'s own
dropped-types/fact-types split is PER-EVENT, keyed only on that
event's own `:pre-horizon` (its own raw `t < registration-t`) — it has
no concept of "this event's containing encounter." An Encounter that
opens pre-horizon (dropped, its own `:pre-horizon` true) but closes
post-horizon (its own `:pre-horizon` false, since its own `:t` has
crossed the boundary) compiles its OWN `:encounter-end` for real — an
orphaned `:discharge`/`:outpatient-visit-end` with no matching open
admission, tripping `check.clj`'s own
`:clinical-content-only-when-admitted` invariant (`check.clj:438`,
which reads compiled IR step types replayed through folded engine
state — never `:pre-horizon-facts` or the raw trajectory directly).
This is EXACTLY the UTI closure's own empirically-observed straddle
(ADR-0033/0034's own dated notes) — not a hypothetical.

**Resolution: AR-1/AR-2 CLEANLY SUBSUME the mechanism — no conflict,
no STOP-AND-ESCALATE.** `history?` false (absent, the default) is the
LEGACY path, entirely UNCHANGED — `:pre-horizon-facts` continues to
land on `:registered` exactly as before, byte-identical (AR-5's pure-
identity bracket; empirically confirmed, Verification baselines,
below). `history?` true is the NEW path: AR-2's encounter-anchored
phase inheritance (minted at the interpreter, `mark-phase`, replacing
per-event raw-timestamp phase with "inherit the currently-open
encounter's own opening phase") means a straddling encounter's own
close is ALSO `:phase :history`, so it drops together with its own
open — no orphan, ever, for any seed. `compile-trajectory`'s new path
drops EVERY `:phase :history` event uniformly, condition/medication/
care-plan included — no `:registration-facts` bucket at all in this
mode, so `:pre-horizon-facts` is simply never populated under
`history?` true (retires FOR THAT MODE, not as a mechanism: `history?`
false still lands there exactly as before). `check.clj`'s own
`:clinical-content-only-when-admitted` needed NO code change: it never
reads `:pre-horizon-facts` or the raw trajectory, only compiled IR
step types replayed through folded state, and AR-2 guarantees a
straddling encounter's own contents never reach `:steps` at all — the
invariant's checked surface simply never sees history content, in
either mode, confirmed by a property test running 150 random seeds
against a purpose-built module whose own Encounter is GUARANTEED to
straddle (`:persona-config {:age-min 0 :age-max 0}` bounds every
possible DOB-to-registration-t gap to 3–365 days; the module's own
Encounter closes 500 days after opening, always past that window).

**Step 3 finding, NOT anticipated by the ruling text (found live,
running the REAL UTI closure under an ordinary seed, AR-4's own proof
obligation) — a second, narrower gap in the SAME class.** Open-
encounter inheritance alone does not cover `:medication-end`/
`:care-plan-end`/`:condition-end`, which can legitimately fire OUTSIDE
any encounter (a medication started during a dropped history-phase
encounter, ended after discharge, in horizon, with nothing open to
inherit from at the moment it ends) — that event's own raw phase reads
`:horizon` while its own antecedent (`:medication-order` etc.) was
dropped, an orphaned `:medication-end` tripping
`medication-end-references-existing-order-and-follows-it-in-time`.
This is judged NOT an AR-6 conflict (the `:pre-horizon-facts`
mechanism is untouched by it) but a direct, narrow extension of AR-2's
own stated principle ("no orphaned reference to something dropped"),
generalized one `:references` hop further — implemented, not
escalated, since the fix is mechanical and the principle is already
ratified; `compile_trajectory.clj`'s own `history-phase?` (Step 2/3)
has the full reasoning. Recorded here per the same disclosure
discipline as every other live finding this project's ADRs already
practice, not silently folded into AR-2's own text.

### Verification baselines

**Identity bracket.** `bin/regression-oracle 537f954 6a587ff` (the tip
before Step 1 → this session's own Step 4 landing commit): all NINE
pre-existing vendored root batches IDENTICAL — `appendicitis`,
`death-fixture`, `ear-infections`, `ear-infections-engine`, `sepsis`,
`sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
`urinary-tract-infections-engine`. AR-3's own gating argument holds,
byte-verified, not merely asserted. `clojure -M:poly check` clean.

**New history-mode baselines (AR-5, FIRST BASELINES — no "before" to
diff against).**

```
37885c6635918975be76abb37e9b662ebef7858ffefd883b3b4f5a6046b34af4  ear-infections-history-engine.edn
ecc49eb4d6d632f09be24b563aabb4dd1c7dcd1736e91928edaf76726d3534d3  urinary-tract-infections-history-engine.edn
```

Both roots' own `:history` absent digest (run at the pre-session
baseline commit, where `:history true` is silently ignored by an
engine that doesn't yet destructure it) matches their own LEGACY
sibling batch exactly (`ear-infections-history-engine` ==
`ear-infections-engine`'s own baseline digest,
`5a631475998e505c7edaf902c60bfa519ce171a4e673ae9e99a1eb2687742303`) —
independent confirmation, at the digest level, that the flag was
inert before this session and genuinely load-bearing after.

### Execution record

Steps 1–4 landed as four commits, in order: `98f099b` (Step 1, config +
interpreter phase boundary — `:history` flag, `mark-phase`'s own AR-2
inheritance, additive arities throughout), `73bb26f` (Step 2, compile
filter + straddle inheritance — `compile-trajectory`'s new uniform-drop
path, the engine-scale property test proving the guaranteed-straddle
module never trips the invariant catalog across 150 seeds), `9240db8`
(Step 3, seed-777 retirement + the Step 3 finding above +
`history-phase?`'s own reference-chain extension + the ear-infections
wellness-fold interpreter-layer proof), `6a587ff` (Step 4, oracle —
`digest.clj` gains the two history-mode roots, the identity bracket
above). Full test suite (sim/sim-trajectory/sim-emit-hl7-adjacent,
every namespace this session touched or could plausibly have
perturbed): 0 failures, 0 errors throughout; `clojure -M:poly check`
green before every push.

### Fence

No carry-across implementation (AR-2's own named future, roadmap
Deferred), no history-census (AR-3), no backloaded emission in any
form (ADR-0031 AR-3 stands, unchanged). The chronic-meds cadence cap
(ADR-0037's own Deferred row) is untouched. This ADR closes the GMF
parity arc's own final wave — **the roadmap's own "Wave H" row and the
parity plan's own H row both close here; no further GMF-coverage wave
is scheduled.**

Ratification note (author-ruled 2026-08-04, design channel review). The Step 3
finding's one-hop extension of AR-2's phase inheritance along the :references
back-edge (history-phase?, compile_trajectory.clj) is RATIFIED as within AR-2's
own stated principle — "no orphaned reference to something dropped" — applied to
end-events whose antecedent was dropped as history. Not a new rule: the same
edge, one hop further. The disclosed v1 cost (a straddle-adjacent end-event
yields no in-window wire traffic) is subsumed by the carry-across named future
(roadmap Deferred, unchanged trigger). This closes the disclosure above; AR-2's
body text stands as written.

---


## ADR-0085 — Colorectal investigation: the straddling encounter, named

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: the fidelity arc closed (`notes/adr/0084-fidelity-arc-close.md`,
tip `45eb2f4`) carrying the roadmap's own oldest live Deferred row
forward — `colorectal_cancer.json`'s own `:clinical-content-only-when-
admitted` gap, true name, undiagnosed (ADR-0083's own erratum, the row
this session executes). This is a DIAGNOSIS session per the design
channel's own 2026-08-08 ruling ("Concur. go."): no fix, no vendoring,
no interpreter/compile/engine edit anywhere in the workspace — its
product is knowledge, recorded here, not code.

Read-first: `notes/adr/0084-fidelity-arc-close.md` (the close, the tag
debt); `notes/adr/0082-encounterend-fix.md` (the probe precedent this
session extends — the `with-redefs` interception technique, the
per-seed violation tables, AR-EE-1a's own truncation-layer finding, the
hypothesis this session must explicitly test); `notes/adr/0083-
fidelity-payoff.md` (the erratum chain); `.agents/plans/roadmap.md`'s
own colorectal row; `components/sim-check/src/ehrt/sim_check/check.clj`
(`clinical-content-only-when-admitted`, `discharge-follows-admission`);
`components/sim-trajectory/src/ehrt/sim_trajectory/compile_trajectory.clj`
and `components/sim-engine/src/ehrt/sim_engine/engine.clj` (the two
candidate layers).

### Decision

Author rulings, recorded verbatim. `[A]` author-ruled, `[C]`
channel-inferred.

1. **AR-CI-0** `[A — tag law, case (ii)]`. `stable-20260808-fidelity-
   close` annotated and pushed at `45eb2f4` (ADR-0084's own closing
   tip, confirmed HEAD at session start, working tree clean). Tag did
   not already exist locally or on the remote; created fresh. **Executed
   Step 0.**

2. **AR-CI-1** `[A — the design-channel-ruled sequencing]`. Diagnose-
   only fence, held: no `src/`/`test/`/`deps.edn` edit anywhere in the
   workspace this session. The mechanism this session finds (below) has
   an obvious-looking fix shape; it is named, not taken, per AR-P-4's
   own law (a tempting fix found mid-probe is a finding, not an
   invitation).

3. **AR-CI-2** `[C — probe protocol, extending ADR-0082's own
   mechanics]`. Layer bisection executed as specified: pin verified
   (`7e08387c68a7f0e21d13076609a159fd473fc902`, direct `git rev-parse
   HEAD` against `/home/mg/synthea-checkout`); `colorectal_cancer.json`
   loaded from the pin-verified checkout via `gmf/load-closure` with a
   filesystem `resolve-call-path` reaching `anemia/anemia_sub.json`
   there too (no `:persona-config` override, confirmed unnecessary by
   ADR-0082's own inspection and by this session's own clean load); 300
   patients; seeds 42 and 20260802. Probes ran in-session via `clojure
   -M:dev` scratch scripts (not committed; listed in full in the session
   record) using `with-redefs` interception at the
   `ehrt.sim-trajectory.interface/run-module` AND `.../compile-
   trajectory` boundaries simultaneously — capturing, per patient, (a)
   the raw interpreter walk exactly as `run-module` returned it, (b) the
   exact trajectory `compile-trajectory` compiled from, and (c) the
   engine's own ground-truth plus `engine/replay`'s status stream.
   Per-call capture order was cross-checked against each patient's own
   `:registered` ground-truth event (in log order) via each event's own
   `:pre-horizon-facts` count against the matching captured
   `:registration-facts` count — **zero mismatches across all 300
   patients at both seeds** — before trusting any per-patient
   attribution below.

4. **AR-CI-3** `[C — the truncation hypothesis, probed not assumed]`.
   **CONFIRMED, in a more precise and narrower form than the hypothesis
   named.** ADR-0082 AR-EE-1a's own truncation-layer finding named TWO
   pre-existing mechanisms: the `:pre-horizon` drop gate, and
   `compile-trajectory`'s own `encounter-closed?` single-encounter-per-
   run scope. This session's own probe (below) implicates the FIRST of
   the two directly and by name — but in a failure shape AR-EE-1a never
   exercised (that finding was about a fully pre-horizon dangling
   `:encounter-end` being silently absorbed; this defect is about a
   STRADDLING encounter, one whose own opening `:encounter` is
   pre-horizon while its own closing `:encounter-end` and intervening
   clinical content are not). `encounter-closed?` plays only its
   ordinary, ratified, non-defective role here (see Diagnosis, below) —
   it is NOT independently responsible for either violation.

5. **AR-CI-4** `[C — the discharge-follows-admission trace]`. Traced to
   its own patient and mechanism: **the SAME patient, the SAME
   mechanism** as the `:clinical-content-only-when-admitted` violations
   at that seed (seed 20260802, `PID-000239-c79b3f7f`) — not a second
   defect. See Diagnosis, below.

6. **AR-CI-5** `[A — acceptance bar]`. Met via (i), a LOCALIZED
   diagnosis: named namespace
   (`ehrt.sim-trajectory.compile-trajectory/compile-trajectory`), named
   mechanism (the per-event, not per-encounter-span, `:pre-horizon` drop
   gate), probe-evidenced across 100% of the violating population (2 of
   2 distinct violating patients, both seeds), plus a proposed fix shape
   for the design channel below.

### Reproduction (Step 1)

`check/check-all` at 300 patients, the closure loaded exactly as
AR-CI-2 describes, via `engine/run` + `check/check-all` (the same
round-trip shape every vendored module's own committed test uses):

| seed | `:clinical-content-only-when-admitted` | `:discharge-follows-admission` | total | distinct violating patients |
|---|---|---|---|---|
| 20260802 | 3 | 1 | 4 | 1 (`PID-000239-c79b3f7f`) |
| 1 | 0 | 0 | 0 | 0 |
| 42 | 4 | 0 | 4 | 1 (`PID-000038-f5560829`) |

Zero-violation at seed 1, violations at 42 and 20260802 — the expected
shape. **A qualitative discrepancy against ADR-0082's own record,
disclosed rather than smoothed over:** this session's exact counts
match ADR-0072's own ORIGINAL finding verbatim ("20260802 and 42 each 4
violations, seed 1 clean") and match ADR-0082's own summary table
("Matches ADR-0072's own exact record (4/0/4)") — but do NOT match
ADR-0082's own immediately-following prose, which reports `{:clinical-
content-only-when-admitted 19, :discharge-follows-admission 1}` at seed
42 (20 total, not 4) and `{:discharge-follows-admission 1, :clinical-
content-only-when-admitted 3}` at seed 20260802 (4 total, matching this
session at that one seed only). No commit in this repo's history
touches `check.clj`, `compile_trajectory.clj`, `gmf_interpreter.clj`, or
`engine.clj` between `dad2553` (the EncounterEnd fix, which ADR-0082
itself already ran against) and this session's own `45eb2f4` HEAD — so
the invariant catalog and every candidate layer are byte-identical to
what ADR-0082 measured; there is no code-drift explanation available.
This session's own methodology was independently verified (the
zero-mismatch pairing check, AR-CI-2, above) and its raw violation
counts are a direct, untouched pass-through of `check/check-all`'s own
output, not derived through any per-patient attribution logic that
could itself have dropped or miscounted violations. The likeliest
explanation is that ADR-0082's own seed-42 prose figure was itself in
error (a session that was mid-fix, not fully re-verifying a byte-for-
byte-identical secondary finding against its own summary table one
paragraph earlier) — but this session did not have that session's own
scratch state to inspect and does not assert this as settled fact,
only as the best-supported reading of two internally-consistent
measurements (ADR-0072's, this session's) against one that
contradicts itself. This session's own diagnosis (below) is built on
ITS OWN freshly-verified counts and per-patient evidence, not on
ADR-0082's disputed prose figures.

### Bisection and diagnosis (Step 2)

Both distinct violating patients — 100% of the violating population
across both seeds — show the exact same mechanism, evidenced at all
three layers AR-CI-2 names.

**The shape, seed 42, `PID-000038-f5560829`:** the raw interpreter walk
opens an `:ambulatory` encounter (`:routine-colonoscopy-encounter`,
idx 6, `t=20074`, **`:pre-horizon true`**) and mints a `:procedure`
inside it (idx 7, also pre-horizon). The registration horizon falls
between `t=20074` and `t=20115` — so the FIRST post-horizon event is
`:condition-onset` (idx 8, `:pre-horizon false`), immediately followed
by `:observation`/`:procedure`/`:observation`/`:procedure` (idx
9-12, all post-horizon) and the encounter's own `:encounter-end`
(idx 13, `:pre-horizon false`, `:references 6` — pointing back at the
pre-horizon-dropped opening). `compile-trajectory`'s own loop drops idx
6-7 via its per-event `pre-horizon` gate (`:encounter`/`:procedure` are
both in `pre-horizon-dropped-types`) — **without ever setting
`encounter-closed?`**, since the drop clause recurs past it untouched.
`encounter-closed?` is therefore still `false` when idx 8 arrives; idx
8-12 compile normally as ordinary post-horizon clinical content
(`:condition-onset` compiles to nothing — no matching compiled
encounter step exists for `annotate-condition` to attach to, which is
why `registration-facts: 0` for this patient, not a second gap); idx 13
(`:encounter-end`) also compiles normally, resolving its `:references`
back to idx 6's `:ambulatory` class and emitting `:outpatient-visit-
end` — **with no `:admission`/`:outpatient-visit` step ever compiled
first.** `engine/replay`'s status fold therefore reads `:status :new`
(never `:admitted`) at every one of these four clinical-content events,
tripping `clinical-content-only-when-admitted` four times, exactly the
count reproduced.

**The shape, seed 20260802, `PID-000239-c79b3f7f`:** identical
mechanism, one layer more consequential. The raw walk's own
`:partial-colectomy-encounter` (idx 21, `:inpatient`, `t=20067`,
**`:pre-horizon true`**) and its own `:partial-colectomy-procedure`
(idx 22, also pre-horizon) are dropped the same way. The horizon falls
between `t=20067` and `t=20101`; the first post-horizon events are
`:observation` (`:pain-vital-3`, idx 23), `:care-plan-start`
(`:partial-colectomy-careplan`, idx 24), `:procedure`
(`:postoperative-care`, idx 25) — three clinical-content steps compiled
with the same missing-admission gap, tripping `clinical-content-only-
when-admitted` three times. The encounter's own `:encounter-end` (idx
26, `:end-diverting-colostomy-encounter`, `:pre-horizon false`,
`:references 21`) resolves back to the dropped `:inpatient` opening and
compiles to `:discharge` — flipping `encounter-closed?` true (correctly
halting everything after, per this project's own ratified single-
encounter-per-run scope, sim/ADR-0007 point 3 — NOT itself defective).
Because this patient's ENTIRE compiled trajectory is this one straddling
encounter's own tail, no `:admission` event exists anywhere in the
patient's compiled ground truth at all — so `discharge-follows-
admission`'s own check (`(neg? first-admit)`) trips too, the single
early violation AR-CI-4 traces. **Same patient, same mechanism, not a
second defect** — the discharge violation is a direct corollary of the
identical missing-admission gap, not an independent one.

**Diagnosis.** Namespace:
`ehrt.sim-trajectory.compile-trajectory/compile-trajectory`. Mechanism:
the LEGACY (`history?` false, the only mode this run ever exercises)
pre-horizon drop clauses (`compile_trajectory.clj` ~426-435) test only
`(:pre-horizon event)` — the event's OWN flag — with no back-reference
check against the event the current one's own `:references` index (or,
for an `:encounter-end`, the encounter it closes) points to. An
encounter whose OPENING is pre-horizon (dropped) but whose CLOSING and
intervening content are not (compiled normally) — a real, clinically
ordinary shape: any encounter admitted before "today" and still open
through it — produces compiled clinical-content and terminal-discharge
steps with no compiled opening step to match. This is the SAME general
class of gap `compile-trajectory`'s own Wave H `history-phase?`
mechanism (~336-362) already closes for `:medication-end`/`:care-plan-
end`/`:condition-end` — but ONLY in `history?` true mode, and NOT for
`:encounter-end`/`:observation`/`:procedure`/`:care-plan-start` at all,
in either mode. The legacy path this run exercises has no analogous
check whatsoever.

### Truncation-hypothesis verdict (AR-CI-3)

CONFIRMED, narrowed. The `:pre-horizon` drop gate is a REAL, evidenced
root mechanism — but the specific failure is a straddling-encounter
shape AR-EE-1a's own hypothyroidism trace never exercised (that trace
concerned a fully-pre-horizon dangling reference, silently and
correctly absorbed; this is a partially-pre-horizon, genuinely open
span, incorrectly absorbed on one side only). `encounter-closed?`'s own
single-encounter-per-run truncation is present in both traces (it halts
compilation once each patient's own first compiled `:encounter-end`
fires) but plays no defective role — it fires exactly where the
project's own ratified scope says it should, on already-malformed
input the pre-horizon gate handed it.

### Proposed fix shape (for the design channel, not executed here)

Not a fix — the shape a future ruled fix session would need to choose
between, named per AR-CI-5(i): (a) track, in the legacy path, whether
an `:encounter`/`:encounter-end` pair straddles the horizon (the
opening pre-horizon, the closing not) and SYNTHESIZE a compiled
opening step at the horizon boundary for the straddling case — the
"already admitted when the record starts" shape real HL7 feeds handle
via a still-open PV1 segment; or (b) generalize `history-phase?`'s own
back-reference principle (an event whose own antecedent was dropped is
dropped too) to the legacy path and to `:encounter-end`/clinical-
content types, at the cost of dropping real post-horizon clinical
content that happens to belong to a pre-horizon-opened encounter. Both
are genuine, different-tradeoff design decisions, not mechanical
follow-through — left to the design channel and a dedicated ruled
session, per AR-CI-1's own fence.

### Confirmation

- `clojure -M:poly check`: OK (Step 0, before any probe).
- Oracle pre-digest: `bin/regression-oracle 45eb2f4 45eb2f4` —
  IDENTICAL, all 28 roots, byte-for-byte (a self-bracket, confirming the
  harness and the tree agree with themselves before any session work).
- Last five CI runs on `main` at session start (`gh run list --limit 5
  --branch main`): all five `success` — `31266927895` (`45eb2f4`),
  `31266367045`, `31263297709`, `31261179158`, `31260846758`. No red
  window.
- Zero working-tree mutation this session outside this ADR's own
  commit and the session-record/prompt-archive commit — `git status`
  clean at every checkpoint prior to staging; every probe ran via
  `clojure -M:dev` scratch scripts under the session's own scratchpad
  directory, never inside the repo.
- Full suite (`clojure -M:poly test :all skip:integration`): see the
  session record for the exact run(s) and disambiguation, if the
  disclosed `mutate-stdout-into-intake-stdin-real-loopback-test` flake
  fired.

### Successor tag debt, recorded here

The next session that opens fresh work tags
`stable-20260808-colorectal-investigation` at THIS session's own
closing tip — the same tag-law case (ii) pattern every prior close in
this repo has used for its own predecessor.

### Roadmap disposition

No Done-section entry — per AR-CI-1/AR-CI-5, this is a diagnosis
session, not a closure. The roadmap's own colorectal Deferred row stays
LIVE, gaining a dated note pointing here (this session's own edit,
landed in the same commit as this ADR). `notes/ADRs.md` gains this
ADR's own citation-index line, one line, in that file's own running
sequence (below).

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Colorectal investigation: the straddling encounter, named — a diagnosis-only session (no fix, per the design channel's own ruled fence) localizes `colorectal_cancer.json`'s own `:clinical-content-only-when-admitted`/`:discharge-follows-admission` violations to `compile-trajectory`'s own legacy `:pre-horizon` drop gate, which tests only an event's own flag with no back-reference to the encounter it belongs to — an encounter opened pre-horizon and closed post-horizon compiles its post-horizon content with no matching admission step; confirmed across 100% of the violating population (2 of 2 patients, both seeds), the truncation hypothesis narrowed and confirmed, a fix shape proposed for a future ruled session

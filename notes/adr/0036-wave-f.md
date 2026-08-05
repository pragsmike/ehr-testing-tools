<!-- Attic file: notes/adr/0036-wave-f.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0036 — Wave F: Counter/ImagingStudy/SupplyList land; the condition rider (`Not`/`Race`/`Socioeconomic Status`) and persona race/SES fields close (`.agents/plans/2026-08-02-gmf-parity-plan.md` §4, ADR-0035 AR-8's own resequencing)

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-8 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
same day.

### Context

The post-F0 census (`2026-08-03-synthea-7e08387-wave-f0.edn`, ADR-0035)
ranked Wave F at 29 modules: `Counter` (14), `ImagingStudy` (10),
`SupplyList` (5), plus a rider of three condition types — `:race`,
`:socioeconomic-status`, `:not` — unblocking 4 more (counts are lower
bounds; F0 demonstrated fail-fast masking). All semantics are pinned
from Synthea source at the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902` — `State.java` classes
`Counter`/`ImagingStudy`/`SupplyList`, `Logic.java` classes `Race`/
`SocioeconomicStatus`/`Not`/`VitalSign`). The `:vital-sign` condition
and `VitalSign` state are DEFERRED OUT of this wave by ruling (AR-7).
No vendored root uses any Wave F state or condition type (verified by
recursive scan of state bodies including nested condition trees), so
the oracle claim is PURE IDENTITY, with one hazard called out in AR-5.

### Decision

**AR-1 (Counter).** Upstream: read patient attribute (missing → 0),
increment or decrement by `amount` (default 1 when absent/0 — port the
legacy default), write back numeric. Zero draws, no time advance, no
trajectory event. Sim: follow `:set-attribute`'s exact shape —
module-namespaced attribute, pass-through outcome. The namespacing
narrows upstream's shared-attribute semantics (cross-module counters
would not see each other); DISCLOSED in the state's doc entry — no
vendored or census module currently depends on cross-module counter
visibility, and the honest-absence rule (AR-4) will surface it if one
ever does.

**AR-2 (ImagingStudy).** Upstream: records an imaging study (series ×
instances, modality codes) AND a companion Procedure with a fixed
30-minute stop; extends `State`, returns true immediately — NO
module-clock advance (unlike duration-bearing Procedures). Draws: when
`min_number_series`/`max_number_series` bounds are given, series count
= one uniform integer draw inclusive (upstream `rand(min, max+1)`
int-cast ≡ `rand-int-in min max`); per-series instance bounds likewise,
one draw per materialized series. Sim shape: one trajectory event
`:imaging-study` carrying procedure code, modality, and the drawn
series/instance counts (glass-box), compiling to the SAME IR step
family a `:procedure` produces — upstream's own companion-procedure
move, with the 30-minute stop as record metadata, not clock advance.
DICOM UID synthesis is NOT ported (deferred, disclosed).

**AR-3 (SupplyList).** Upstream: for each `{code, quantity}` component,
record supply usage; no draws, no clock, no encounter requirement.
Sim: a log-only trajectory fact (`:supply-list` with components)
compiling to NO IR step — the ConditionEnd no-open-encounter precedent
verbatim. Wire rendering of supplies is out of scope for hospital
traffic v1; disclose.

**AR-4 (condition rider + persona fields + honest absence).**
Implement `:not` (recursive negation), `:race` (case-insensitive match
against persona race), and `:socioeconomic-status` (match against
persona category). Persona gains OPTIONAL `:race`/`:socioeconomic-
category` fields, sampled ONLY when persona config supplies category
weights. Honest-absence rule: evaluating `:race`/`:socioeconomic-status`
against a persona lacking the field is a WALK ERROR (a value, recorded,
per result-not-throw) — NOT a silent false.

**AR-5 (draw-law hazard — persona sampling).** The new persona fields
draw ONLY when config requests them. Absent config = zero additional
draws = every existing persona byte-identical. This is the wave's one
identity hazard; a persona-stream shift would perturb EVERY digest. The
oracle bracket (AR-6) is the proof; a targeted unit test (persona with
and without the new config, draw counts compared) is the co-landing
invariant.

**AR-6 (oracle bracket — pure identity).** No vendored root uses any
Wave F state or condition type. Every oracle-covered root byte-identical
across the bracket; any change is a STOP-AND-ESCALATE.

**AR-7 (explicit deferrals — record, don't drift).** OUT of Wave F by
ruling: the `:vital-sign` condition and `VitalSign` state (modules
`contraceptives`, `covid19`) — engine-delegated calibration content,
pairing naturally with re-scoped Wave E — record as a named roadmap
item "vital-sign channel." Also deferred to Wave I: lookup-table
columns `race` and `time` (modules `acute-myeloid-leukemia`,
`hiv-diagnosis`); the `race` column shares the persona-race
prerequisite this wave lands.

**AR-8 (census re-run + substance note + F0's unowned findings).**
Re-run the census (same header params plus the AR-4 persona fields,
disclosed in the header), commit alongside prior artifacts with a
disambiguated name. Records step also lands: (a) a §15 substance note —
26 of 42 pre-F `:ok-walked` modules produce zero events on every census
seed, so walk-verification attests determinism of what walks touch,
which for the gated chronic cluster is currently almost nothing; (b)
roadmap Deferred rows for census tool refinements, the UTI Observation
raw-`:distribution` normalization gap, and AR-7's two deferral items.

### Execution note (filled same day, 2026-08-03)

**Step 1 (Counter/ImagingStudy/SupplyList, `98f53ad`).** All three
land together (one commit, not three — shared files, interleaved case
dispatch; disclosed rather than forced apart by hand-edited patch
hunks). Loader (`ehrt.sim-trajectory.gmf`): `gmf-type->keyword` gains
all three; `normalize-state` gains `:action` keywordization (Counter)
and `:procedure-code`/`:series`/`:supplies` Concept normalization
(ImagingStudy/SupplyList); `raw-attribute-writes` gains Counter as a
third attribute-writing leaf (section 5's own collision check, extended
the same way Symptom already joined SetAttribute); new schema types
`ImagingSeries`/`ImagingInstance`/`SupplyComponent`. Interpreter
(`ehrt.sim-trajectory.gmf-interpreter`): `:counter` case (legacy
amount-default-to-1 ported EXACTLY — Clojure's `0` is truthy, so
`(or amount 1)` would have silently kept an authored `0`; an explicit
`(if (or (nil? amount) (zero? amount)) 1 amount)` is required, found
while writing the case, not merely anticipated); `imaging-study-extra`
(series-count draw, then one instance-count draw per materialized
series); `:supply-list` case via `emit-and-advance`.
`compile-trajectory`: `:imaging-study` folded into the existing
`:procedure` compile clause (reuses `procedure->step` unchanged) and
added to `pre-horizon-dropped-types`; `:supply-list` gets its own
explicit no-op clause (never a step, any phase). `gmf-test`/
`census-test`'s own "still-deferred" example fixture swapped from
`ImagingStudy` to `VitalSign` (AR-7's own deferral) — the same
"stale premise, not silently left" treatment those fixtures document
across three prior waves. 30 new tests across `gmf-test`/
`gmf-interpreter-test` (increment/decrement/missing-attribute-default/
legacy-amount-default/zero-draws for Counter; fixed-shape/bounded-
series/bounded-instances/no-clock-advance/procedure-shaped-compile for
ImagingStudy; log-only-fact/no-IR-step for SupplyList).

**Step 2 (condition rider + persona, `c9b2bbf`).** `gmf.clj`:
`condition-type->keyword` gains `Not`/`Race`/`Socioeconomic Status`
explicitly (the slug fallback would have produced the same keywords,
but this map is the project's own grep-able vocabulary registry, not
merely a convenience transform); `normalize-condition` gains a THIRD
recursive clause for `:not`'s own singular `:condition` key — the
existing recursive clause was gated on the plural `:conditions` key
(`#{:and :or :at-least}`), which would never have fired for `Not`
(found while implementing, not merely anticipated: a genuine gap in
the existing recursion, not a stale claim). `gmf-interpreter.clj`:
`not-condition-holds?`, `race-condition-holds?` (case-insensitive,
`.equalsIgnoreCase`), `socioeconomic-status-condition-holds?`
(case-SENSITIVE, `.equals` — source-confirmed, NOT the same as Race);
`honest-absence` builds a distinctly-marked `ex-info`
(`::honest-absence` in ex-data) the new `step-safely` wrapper is the
ONE place that catches — `walk-module`/`run-module`'s own loop now
calls `step-safely` instead of `step` directly, converting exactly
that marker into a `:walk-error` status/payload, re-throwing anything
else unchanged (a genuinely unsupported condition type, an unrecognized
vital-sign name, etc. all stay loud, uncaught crashes — proven by a
dedicated test that a bogus condition type still throws through
`walk-module`, never silently downgraded).

`ehrt.sim-model.persona`: `persona`'s own `config` gains optional
`:race-weights`/`:socioeconomic-weights`; two NEW draws (14/15),
config-gated — `(seq pool)` both the presence check and the empty-pool
divide-by-zero guard `weighted-pick` would otherwise hit. This is a
DELIBERATE, disclosed, narrow exception to the fixed-RNG-consumption
law (`sim/ADR-0009`): the law itself guards against draw count
depending on a RUNTIME OUTCOME within one persona's own sampling
(which weighted-pick bucket is chosen must never change how many draws
happen); `:race-weights`/`:socioeconomic-weights` presence is a
CONFIG-TIME decision, the same class of variation `age-min`/`age-max`
already are — not a value this function chooses partway through. The
actual identity-preservation reason: an unconditional 14th/15th draw
would shift every subsequent draw for every persona ever sampled by
every EXISTING (unconfigured) caller, the precise concern the fixed-
consumption law exists to prevent. `persona-test`'s own `counting-random`
(a pre-existing helper, reused verbatim) proves both halves directly by
method-call count: 13 draws with no config (byte-identical to every
prior persona), 15 with both weights, 14 with one. 21 new tests across
`gmf-test`/`gmf-interpreter-test`/`persona-test`.

**Step 3 (oracle bracket).** `bin/regression-oracle e26c9c1 c9b2bbf`
(the tip before Step 1 → the Step 2 tip) — all 9 root batches:

| root | changed? |
|---|---|
| `appendicitis` | no |
| `death-fixture` | no |
| `ear-infections` | no |
| `ear-infections-engine` | no |
| `sepsis` | no |
| `sinusitis` | no |
| `sore-throat` | no |
| `total-joint-replacement-engine` | no |
| `urinary-tract-infections-engine` | no |

`IDENTICAL: every root's digest matches between e26c9c1 and c9b2bbf` —
AR-6's pure-identity claim holds, byte-verified, the real
`bin/regression-oracle` script's own output.

**Step 4 (census re-run, `83f7858`).** Same header parameters plus
AR-8's own disclosed persona-config delta (fixed, equal-weighted
`:race-weights`/`:socioeconomic-weights` pools). Verdict counts:
`:ok-walked` 42→60, `:load-failed` 34→18, `:walk-failed` 9→7, total
85→85 (unchanged). All 20 verdict changes traced individually
(`docs/gmf-interpreter.md` §15's own AR-8 subsection has the full
per-module account): 10 `Counter`-blocked and 4 `SupplyList`-blocked
modules resolve fully to `:ok-walked`; 2 more (one each) surface a
`max-steps` runaway, joining `med-rec`/`veteran-substance-abuse-
treatment` in the Wave G ledger; all 4 `Race`/`Not`-blocked modules
resolve fully; all 10 `ImagingStudy`-blocked modules surface a next
blocker — NEVER resolved by `ImagingStudy` alone, NEVER regressed —
`VitalSign` (1, AR-7's own deferral), `Physiology` (1, a genuinely new
deferred type, out of scope), an unrecognized lookup-table column (7,
each distinct — Wave I's own tail, AR-7), and a pre-existing
`complex_transition`/NamedDistribution schema gap (1, `injuries.json`
— D3b/H3's own already-documented scope boundary, now a confirmed real
instance). All seven vendored roots stayed `:ok-walked`, byte-identical,
matching the oracle bracket. Substance note (AR-8b, found live): 26 of
42 pre-Wave-F `:ok-walked` modules — including `stroke` — produce ZERO
trajectory events on every seed; recorded in `docs/gmf-interpreter.md`
§15, named for a future ranking session, not a Wave F defect.

**Step 5 (records, this commit).** `docs/gmf-interpreter.md` §1
(Counter/ImagingStudy/SupplyList moved from Deferred to the main table;
`VitalSign` newly disclosed in Deferred), §2 (Not/Race/Socioeconomic
Status prose paragraph), §15 (the Wave F census re-run subsection +
AR-8b substance note). `.agents/plans/2026-08-02-gmf-parity-plan.md`
§4 (Wave F row marked DONE). `.agents/plans/roadmap.md` (Wave F moved
to Done with full account; Wave G design session enters Next; four new
Deferred rows — the stale `myocardial_infarction.json` claim corrected,
census tool refinements, the UTI Observation raw-`:distribution` gap,
the vital-sign channel, lookup-table columns `race`/`time`).

`clojure -M:poly check`: OK, every checkpoint. `clojure -M:poly test
:affected`: 0 failures / 0 errors throughout (316 assertions in
`gmf-interpreter-test`, up from 301; every other affected project —
`sim-model`, `sim-trajectory`, `sim-emit-hl7` — green). `gitleaks git
--staged -v`: clean, every commit.

### Fence

No wellness/Wave-G work; no VitalSign work in any form (AR-7's
deferral is a ruling, not an oversight); no census-tool fixes beyond
the filename workaround. AR-5's persona-draw conditionality is
load-bearing for AR-6; the identity bracket held. Emit vocabulary
beyond the companion procedure (imaging ORM/ORU, supply wire formats)
is out of scope; glass-box facts only.

---


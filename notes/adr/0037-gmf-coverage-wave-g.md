<!-- Attic file: notes/adr/0037-gmf-coverage-wave-g.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0037 — GMF coverage Wave G: the wellness cycle lands — genuine wait semantics, the create-now substitution retired, four loop modules resolve

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-8 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
same day.

### Context

ADR-0031 AR-2 ruled the wellness cycle in scope and shaped it (a
genuine wait state, cadence ported as provenance-cited content, a
seeded phase-offset anchor as an interim answer pending Wave H); this
session ratifies the final design and executes it in one pass — the
wellness schedule is a PURE FUNCTION of persona and time, no stored
schedule state anywhere, the tick sequence derived from DOB via
Synthea's own age-banded cadence, zero RNG draws. This replaces the
Wave B create-now normalization (`ehrt.sim-trajectory.gmf`'s own
`normalize-state` clause, the disclosed timing substitution ADR-0031
AR-5(b) found) with real wait semantics, retires the `:wellness-timing`
substitution tag for the 19 tagged modules, and resolves the four
max-steps loop modules (`med-rec`, `mend-program`, `metabolic-syndrome-
care`, `veteran-substance-abuse-treatment`) whose spins are the
substitution's own artifact. Semantics are pinned from Synthea source
at the pin `7e08387c68a7f0e21d13076609a159fd473fc902`, transcribed via
live WebFetch of the raw source (no sibling checkout kept — the
census's own external-checkout convention is orthogonal, used only for
the census re-run itself, AR-8).

### Decision

Ruled 2026-08-03, design channel, recorded verbatim:

**AR-1 (cadence as provenance-cited content).** Transcribe the age-
banded interval table VERBATIM from `EncounterModule.
recommendedTimeBetweenWellnessVisits` at the pin
(`src/main/java/org/mitre/synthea/modules/EncounterModule.java`, lines
176-201) into a data table (the vital-sign-table discipline: source
file, line range, pin sha cited; facts-register entry; NOTICE
addendum). EXCLUDE the chronic-medications annual-cap branch (same
method, lines 209-211) — deferred to the calibration register as a
named item ("wellness cadence chronic-meds cap"); the transcription
note discloses the exclusion.

**AR-2 (pure schedule function, zero draws).** `next-wellness-tick` is
a pure function: given persona DOB and a time `t`, iterate the cadence
bands from birth (upstream's own `next = previous + band(age)`
recurrence) to the first tick ≥ `t`. No RNG anywhere in the schedule.
DOB varies per patient, so phase varies naturally; a hash-derived
phase-offset knob is NOT built — recorded as a future calibration
option. Zero draws means every non-wellness walk's rng stream is
untouched (load-bearing for AR-6). **Deviation, found live (AR-2's own
boundary, refined mid-session, not re-ruled): the "first tick ≥ t"
wording above is what was ruled, but the REAL `med_rec.json`'s own
wellness-wait loop has zero delay anywhere in its own loop body
(Wellness_Encounter → ... → EncounterEnd → Initial → ConditionOnset →
Wellness_Encounter again) — an inclusive `>=` first call at `t` = DOB
returns DOB unchanged, and every re-entry at that SAME unchanged `t`
returns the same tick again: an infinite zero-advance spin into
`max-steps`, found running this session's own AR-8 census against the
real catalog, not merely anticipated. Fixed same session, before AR-8's
census ran for real: `next-wellness-tick` returns the first tick
STRICTLY AFTER `t` (`>`, not `>=`) — every call now guarantees a
genuinely later tick, matching upstream's own real mechanism
(`person.record.timeSinceLastWellnessEncounter` resets to ZERO the
instant an encounter fires, so the next check always needs a genuinely
NEW interval, never the same instant twice). This is a refinement of
AR-2's own boundary condition, not a reversal of its design — the
recurrence, the zero-draw property, and the DOB anchor are all
unchanged.**

**AR-3 (wait semantics).** The loader maps `wellness: true` Encounter
states to a distinct `:wellness-wait` state (schema entry; `:reason`
and transitions preserved), REPLACING the create-now normalization.
The interpreter, on `:wellness-wait` at time `t`: advance the module
clock to `next-wellness-tick(persona, t)` (bounded by `horizon-end-t`
exactly as Delay is — parking past the horizon ends the walk in the
same status Delay uses, via the SAME mechanism: `run-module`'s own
loop re-checks `:t` against `horizon-end-t` before every step, so a
single step's own advance can overshoot the horizon just as Delay's
already can), open a wellness `:outpatient-visit`-family `:encounter`
event at the tick (the same trajectory event family the substituted
path produced, now correctly timed), attach the state's own `:reason`,
and proceed via the ordinary transition. Loop bounding falls out: each
loop iteration advances a full cadence interval (AR-2's own strict-`>`
fix makes this true even with zero other delay in the loop body), so
the horizon bounds iterations — the four loop modules are the
acceptance evidence (AR-7).

**AR-4 (engine unchanged; attachment deferred with a trigger).** The
wait is a module-clock advance inside `run-module`; the engine drives
walks as it does today and needs NO changes. Upstream's own all-
waiting-modules-attach-to-one-visit semantics only diverges when one
patient runs multiple modules concurrently, which the engine's one-
module-per-patient assignment does not do. DEFERRED with the named
trigger "multi-module assignment per patient" (roadmap Deferred).

**AR-5 (substitution tag retirement + Physiology ruling).** The
census tool's `:wellness-timing` detector (`wellness-substitution?`)
retires — kept as history (a dated comment explains why, not deleted
outright), no longer called; the tag vocabulary (`:disclosed-
substitutions`) stays extensible. The census gains its first
`:out-of-scope-by-ruling` entry: the `Physiology` state (module
`gallstones`), citing this ADR — Synthea's ODE physiology engine is out
of the sim's fitness-for-purpose scope by author ruling. A new
`out-of-scope-by-ruling?` classifier reclassifies a `:load-failed`
closure ONLY when its entire gap is ruled-out state types — a genuinely
mixed gap stays `:load-failed`.

**AR-6 (oracle bracket — identity except one root, disclosed).** Zero
non-wellness draws change (AR-2) and no vendored root except
`ear_infections` contains a `wellness: true` state (verified, ADR-0031
AR-5(c)'s own sweep). Therefore: every oracle batch byte-identical
EXCEPT `ear-infections` (interpreter) and `ear-infections-engine`
(engine) — those two change BY DESIGN (the visit moves from
immediately-after-medications to the next cadence tick), re-baselined
with a dated disclosure naming the old and new digests. Any OTHER
batch changing is a STOP-AND-ESCALATE.

**AR-7 (acceptance evidence).** Co-landing tests: schedule-function
band/spot tests (an independent re-transcription of the cited source
lines, not a tautology against the implementation); `:wellness-wait`
interpreter tests (advance-to-tick, zero draws, reason attachment
present/absent, horizon parking, a wait-act loop terminating horizon-
bounded — the loop fixture deliberately matches `med_rec.json`'s own
zero-delay shape, the real regression case for AR-2's strict-`>` fix);
updated `ear_infections` interpreter and engine round-trip tests
asserting the NEW timing (the wellness encounter now fires strictly
after the last medication ends, not at the same instant — proven at
the interpreter layer per AR-4's own boundary ruling that semantic walk
claims are interpreter-layer, engine round-trips a narrower does-it-
still-work-end-to-end check). The four upstream loop modules are
census-level evidence (AR-8), not test dependencies — tests use inline
fixtures only.

**AR-8 (census re-run).** Same header params, disambiguated filename
(`2026-08-03-synthea-7e08387-wave-g.edn`, overwrite bug still open —
workaround as before). Expected movement, ALL CONFIRMED: the four loop
modules leave `:walk-failed` (`:ok-walked`); the 19 tagged modules lose
their tags (retired detector) — 7 of the 19 also change walk digest
(the wait now times the encounter differently), 8 show no observable
difference (their own seeds/horizon never cross the wellness-wait path
differently, a module-specific gating fact, not a gap); `gallstones`
moves to `:out-of-scope-by-ruling`; vendored roots other than
`ear_infections` unmoved. No module outside these 19 moved verdict or
digest at all — classified individually, full account in
`docs/gmf-interpreter.md`'s own new dated subsection.

### Execution note (filled same day, 2026-08-03)

**Step 1 (cadence table + schedule function, `d209267`).**
`resources/sim-trajectory/wellness-cadence.edn` (a two-tier table,
months for age ≤ 3 years, years otherwise, matching the source's own
two-tier `if`), its own NOTICE addendum and `notes/facts-register.md`
F23 (this content is GENUINELY EXTRACTED, unlike the same directory's
hand-curated `vital-signs.edn`). `next-wellness-tick`/`wellness-
cadence-band`/`age-months-at` land in `gmf-interpreter.clj`. Band/spot
tests in `gmf_interpreter_test.clj`, including a full-sequence test
independently re-deriving the cadence table from the same cited source
lines and walking 60 ticks, confirming every one.

**Step 2 (loader, `23974ba`).** `GmfState`'s multi schema gains
`:wellness-wait`; `normalize-state`'s `effective-state-type` override
maps the `wellness: true`/no-`encounter_class` idiom onto it instead of
synthesizing `:encounter-class :wellness` — the retired clause's own
dated comment stays as history. `docs/gmf-interpreter.md` sections 4
and 9 gain matching dated retirement notes. The existing loader test
(`wellness-true-boolean-idiom-normalizes-to-...`) updated to assert
`:wellness-wait`, not a synthesized `:encounter-class`.

**Step 3 (interpreter, `cbf5330`).** `wellness-wait-step` (a variant of
`emit-and-advance` citing the COMPUTED tick time, `death-step`'s own
precedent) plus a new `:wellness-wait` case in `step`. Six new
interpreter tests: advance-to-tick + reason attachment, zero rng
draws, reason absence (never fabricated), horizon parking (Delay's own
mechanism, unchanged), and a wait-act loop terminating horizon-bounded.
`ear_infections`' own interpreter test gains
`next-wellness-encounter-now-resolves-at-a-real-cadence-tick-not-
immediately`; the sim-emit-hl7 engine round-trip test's docstring gains
a dated note (the round trip stays green under the new timing, the
timing claim itself proven at the interpreter layer).

**Live finding, fixed same session (`203ed9f`), before the census ran
for real.** Running this Wave's own census against the pinned Synthea
checkout (a shallow clone — the repo's own current HEAD happened to
equal the pin exactly) showed `med-rec`/`mend-program`/`metabolic-
syndrome-care`/`veteran-substance-abuse-treatment` STILL `:walk-failed`
at `max-steps`, contradicting AR-8's own expectation. Traced directly
(a standalone step-by-step trace of `med_rec.json`): its own wellness-
wait loop has zero delay anywhere in the loop body, and the FIRST call
(`t` = DOB) under the originally-ruled inclusive `>=` semantics
returned DOB unchanged — every re-entry at that same unchanged `t`
returned the same tick again, forever. Fixed (`next-wellness-tick`
returns strictly-later ticks, AR-2's own dated deviation record,
above); re-verified directly (`med_rec.json` now completes at
`:horizon-complete` with 269 events) before re-running the oracle
bracket and the census for real.

**Oracle bracket (`58fdd9c` → `203ed9f`, post-fix).** IDENTICAL on
`appendicitis`, `death-fixture`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`.
CHANGED, by design, on `ear-infections`
(`6dcd3d2d97059d23c10401d8aeda3f0d4b29aa4af602705fd1a1c574b53a6e54` →
`6ad02f827a66def26b5cd87e7c64fea2f48dd4fb782aaaf70fe6cfb10f1721ed`) and
`ear-infections-engine`
(`2294f7849b336f8fd38bb8b93240087cc0f18149654d555bec34eccef91d70aa` →
`5a631475998e505c7edaf902c60bfa519ce171a4e673ae9e99a1eb2687742303`).
The `next-wellness-tick` fix itself changed NEITHER ear-infections
digest from what the pre-fix oracle run already recorded (re-confirmed
directly, not assumed) — the fix's own boundary case never fires on
ear_infections' real seeded walks. AR-6's pure-identity claim holds,
byte-verified, `bin/regression-oracle`'s own real output.

**Step 5 (census re-run, `8fc4b03`).** `:ok-walked` 60→64,
`:load-failed` 18→17, `:walk-failed` 7→3, `:out-of-scope-by-ruling`
0→1, total 85→85. Full movement classified: the four loop modules
resolve fully; `gallstones` reclassifies; 7 of the 19 formerly-tagged
modules (`asthma`, `bronchitis`, `dementia`, `ear-infections`,
`osteoporosis`, `sleep-apnea`, `veteran-hyperlipidemia`) change walk
digest; 8 (`atrial-fibrillation`, `copd`, `epilepsy`, `hypertension`,
`mtbi`, `stable-ischemic-heart-disease`, `veteran-prostate-cancer`,
`wellness-encounters`) show no observable difference; two modules stay
`:walk-failed` for reasons wholly unrelated to this Wave
(`anemia-unknown-etiology`, `wellness-encounters` — the latter's own
name is coincidental, its own failure is an unrecognized vital-sign
name, `docs/gmf-interpreter.md` section 11's own gap). No module
outside these 19 moved at all. Full account in `docs/gmf-interpreter.md`'s
own new dated subsection.

`clojure -M:poly check`: OK, every checkpoint. `clojure -M:poly test
:brick:sim-trajectory`/`:brick:sim-emit-hl7`: 0 failures/0 errors
throughout (396 assertions in `gmf-interpreter-test`, up from 380).
`clojure -M:dev:test` (census, not poly-tested, ADR-0034's own
disclosed gap): 27 assertions, 0 failures, run twice. `gitleaks git
--staged -v`: clean, every commit.

### Fence

No chronic-meds cap (deferred to the calibration register, AR-1); no
phase-offset knob (AR-2's own "not built" ruling); no shared-visit
attachment (AR-4's own named deferral); no lookup-column work; no Wave
H pre-roll mechanics (the wellness-wait-during-history-phase fold rule
is NAMED, on Wave H's own row in the parity plan, not built here — this
session proves the wait mechanism at the interpreter layer only,
`registration-t`/`horizon-end-t` already threaded generically by
`run-module`). The `next-wellness-tick` deviation (AR-2) is a REFINEMENT
of this ADR's own ruling, found and fixed within the same session,
before AR-8's own census ran for real — not a premise that failed
against a stale prompt, and not left for a future session to resolve.

---


# 2026-08-02 — GMF coverage expansion: wave plan

Ruled 2026-08-02 (design channel; ratified via the Wave A prompt, AR-1).
Source analysis: the module survey in
`components/sim-trajectory/docs/gmf-interpreter.md`. Wave A — condition
vocabulary (interpreter-only): `At Least`, `Or`, `Date`, `Observation`-as-
condition, `Active Allergy` (membership per data-source characterization;
`Vital Sign`/ `Active CarePlan` excluded pending state homes). Plus
`Symptom` as consumed-internally write (ruled). Unlocks: sore_throat; most
of stroke. Wave B — CallSubmodule: loader recursion (load/namespace/
validate submodules, module search path), interpreter call/return (stack,
attribute scoping, cross-boundary provenance citations), and the fifth
transition kind (`type_of_care_transition`). Unlocks: ear_infections'
therapeutic content, UTI, MI past ECG, much of total_joint_replacement.
The structural lift; own plan detail ruled at its start. Wave C — Death:
wire to the existing `:expired`/post-mortem machinery (deferred-table's
own instruction), no new mechanism. Completes stroke; contributes to
sepsis/MI/CHF. Wave D — state types needing IR + emitter homes:
`DiagnosticReport`, `MultiObservation`, `CarePlanStart/End`,
`ImagingStudy`, plus any Wave A drops (`Vital Sign`, `Active CarePlan`).
Each needs loader + interpreter + trajectory event + compile-trajectory
mapping ruling + `sim-model` pathway schema addition + engine handling +
an emission decision — the emission decision is a live sim-split S3
trigger (an ORU-emitting DiagnosticReport is emit-hl7 work; siblings, not
fat sim). Payoff sequence: A → sore_throat; A+C → stroke; B → UTI,
ear_infections; B+C → MI; A+C+D → sepsis; CHF last (24% deferred states —
prioritization data only, per the survey). Cross-cutting, every wave:
co-landing (state type + invariants same change), rng-consumption order
documented and property-tested for every new sampling state, every
vendored module gets survey row + vendored test, sim/ADR-0013 point 4
curation per module, survey updated fix-forward.

**Dated fix-forward note (2026-08-02, GMF coverage Wave C, C7(a)/(b),
`notes/ADRs.md` ADR-0028).** Wave B's own payoff yielded exactly one
vendored module, `ear_infections.json` — `urinary_tract_infections.json`
stayed deferred (D6, ADR-0027): its own real closure (twelve files, not
the four this plan's own top-level survey assumed) is dirty with
`DiagnosticReport`/`MultiObservation`, both already this plan's own Wave
D scope. UTI therefore moves fully into **Wave D's** payoff list (the "B
→ UTI" line above is superseded, not struck — Wave B's own structural
mechanism, `CallSubmodule`, is still a prerequisite UTI needed and now
has; what UTI still lacks is Wave D's own scope, not Wave B's). Wave D
also gains a second, genuinely new item: `lookup_table_transition` — a
SIXTH GMF transition kind (beyond this plan's own four originally-named
plus Wave B's `type_of_care_transition`), found on
`urinary_tract_infections.json`'s own entry path, named as a finding and
not built in Wave B (it would need an external lookup-table CSV
mechanism this project has no analog for; the outcome — UTI deferred —
does not change either way it is eventually resolved). Wave D is named
here as this finding's own wave-home so it has a named owner rather than
sitting only in a session record.

**Second dated fix-forward note (2026-08-02, GMF coverage Wave C,
`notes/ADRs.md` ADR-0028).** This plan's own "A+C → stroke" payoff line
is superseded, not struck: Wave C built `Death` in full (loader,
interpreter, compile-trajectory, engine/check — ADR-0028's own C1-C4),
closing the gap this plan originally named as stroke's own last
blocker, but a NEW, unrelated gap surfaced by real-closure
characterization now blocks it instead — `Chance_of_Stroke`'s own
`distributed_transition` reads an upstream attribute (`stroke_risk`,
real Synthea's own Framingham cardiovascular-risk score) this project
has no source for, whose own JSON-specified default makes stroke onset
structurally unreachable if honored literally
(`components/sim-trajectory/docs/gmf-interpreter.md` section 10 has the
full account). Escalated and ruled: `stroke.json` stays deferred this
wave, `Death` proven instead against this project's own hand-authored
test fixture. `A+C → stroke` has no scheduled wave until an
attribute-sourced transition-weight mechanism and a stroke-risk-
equivalent data source both land — named, not scoped, here.

**Third dated fix-forward note (2026-08-02, GMF coverage Wave D design
pass, `notes/ADRs.md` ADR-0029, R1–R7).** Wave D is restructured into
four stages, sequenced (R6): **D0** — the sim-split S3 emitter
extraction (`sim-emit-hl7`: `emit-hl7`/`v2-replay`/`site-profile`),
fired now rather than waiting on its own named trigger (R1) — front-
running deliberately, since emitter growth inside fat `sim` is the
anti-pattern S3 exists to prevent. **D1** — the observation family
(`DiagnosticReport`/`MultiObservation`, one new `:diagnostic-report` IR
step, ORU^R01-with-OBR emission) — payoff: sepsis, closures permitting.
**DONE, 2026-08-02 (stage D1b, ADR-0029's own dated ruling note and
execution note): sepsis.json vendored for real** (`resources/modules/
NOTICE`) — the closure surveyed clean at D1a (§11), the schema RULED
same day, and the full chain (loader/interpreter/compile-trajectory/
engine/emission) landed, oracle-fenced against every prior vendored
root (byte-identical). **D2** — the CarePlan family (a paired IR span mirroring `:medication-
order`/`:medication-end`, the `Active CarePlan` condition type; CarePlan
itself stays v2-silent per R3, its natural rendering deferred to a
future `sim-emit-fhir`) — payoff: MI and `total_joint_replacement`,
closures permitting. **D3** — `lookup_table_transition` (the sixth GMF
transition kind, ADR-0027's own D6 finding), attribute-weighted
`distributed_transition` weights, and the UTI closure re-characterization
— payoff: UTI. Two items are named here as explicitly OUT of this
restructuring, not silently dropped: **`ImagingStudy`** (R5) waits on a
CHF trigger, unowned by D0–D3; the **stroke-risk data source** (R7,
`Chance_of_Stroke`'s own `stroke_risk` attribute, ADR-0028's escalated
finding) is a calibration/content-provenance item, unowned by any wave
until a future session rules it.

**Fourth dated fix-forward note (2026-08-02, GMF coverage Wave D
stage D2, `notes/ADRs.md` ADR-0029's own D2 note; full account:
`components/sim-trajectory/docs/gmf-interpreter.md` §13).** D2's own
mechanism (paired IR span, all four layers -- sim-model/sim-trajectory/
sim/sim-emit-hl7) landed in full, real and tested. **The payoff line
above ("MI and `total_joint_replacement`, closures permitting") did
NOT land as a vendored root this stage -- REVISED to ZERO, an outcome
ADR-0029's own G4 explicitly permits.** `myocardial_infarction.json`'s
real closure (27 files, fetched in full) is dirty with three
independent, each-sufficient blockers unrelated to CarePlan
(`lookup_table_transition`/D3, `ImagingStudy`/R5, a genuinely new
state type `SupplyList`) -- deferred, same shape UTI already has.
`total_joint_replacement.json`'s real closure (4 files) surveys clean
of every Wave-D-scoped type except CarePlan itself; its own
`joint_replacement` attribute gap was resolved live (a small,
disclosed `run-module` extension), but a SECOND, independent blocker
surfaced testing that fix against the real closure: `Joint_Replacement_
Guard`'s own compound Age condition (`Age > 50` AND'd with an
attribute check) is outside this interpreter's own `age-guard-jump-
days` analytical-resolution shape (bare `:age >= N years` only) -- the
walk blocks permanently at age 0, confirmed empirically. Extending
Guard's own condition-resolution machinery is real interpreter-core
work touching every vendored root's own Guard/Delay behavior, outside
this stage's own ruled scope -- escalated, not improvised. Named here
as a live gap: the FIRST future session vendoring `total_joint_
replacement.json` (or any other compound-Age-Guard-gated module) needs
`age-guard-jump-days`/`guard-step` extended first; unowned by any wave
until then.

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

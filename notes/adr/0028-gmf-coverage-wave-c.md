<!-- Attic file: notes/adr/0028-gmf-coverage-wave-c.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0028 — GMF coverage Wave C: `Death` — terminal contract, `:expired` status lands in code

**Status:** Accepted (author-ruled 2026-08-02, design channel, C1–C7 of
the Wave C session prompt; session executed same day).

### Context

Wave C (`.agents/plans/2026-08-02-gmf-coverage-plan.md`, roadmap's own
Wave C row) is the GMF coverage arc's third session: `Death`, the state
type every `Death`-bearing module this project's own M7 survey ever read
found on a genuinely excludable tail, never once on a mandatory path
(`components/sim-trajectory/docs/gmf-interpreter.md`'s own prioritization
table). Wave C's own payoff is `stroke.json` — its `Date` gap closed in
Wave A (ADR-0026), its `Death` gap is the last thing standing between it
and vendoring. The deferred-table's own instruction (`docs/gmf-
interpreter.md` section 1) was always to wire `Death` to "the expired-
state machinery this project has already captured" — `:expired` in
`components/sim/docs/patient-state-model.md`'s accumulator table and the
post-mortem event-validity rows in `components/sim/docs/clinical-
realities.md`. Design was ruled 2026-08-02 in the design channel,
recorded verbatim below (C1–C7); this ADR is that record, not a
narrative retelling of it.

### Decision

**C1 — Death semantics come from Synthea source at the pinned commit,
characterized before implementation** (the D5 precedent, ADR-0027): the
Death state's forms (immediate, ranged/exact delayed, attribute-
referenced), what each consumes from the person record or rng, and
which forms `stroke.json` actually uses. The loader's all-or-nothing
gate means every form PRESENT IN THE VENDORED MODULE must be supported;
forms no vendored module uses are named in the docs as unexercised, not
speculatively built.

> **C1 characterization note (filled Step 1, 2026-08-02):** real
> Synthea's own `Death` state (`State.java`, same pinned commit) declares
> five fields — `codes`, `conditionOnset`, `referencedByAttribute`,
> `range`, `exact` — three time forms (immediate/exact/range, `range`
> costing exactly one rng draw, the SAME `{low high unit}` shape
> `Delay`/`Procedure` duration already uses) and three cause-of-death
> resolution forms. `stroke.json`'s own `Death` state uses exactly the
> `range` time form and the `codes` cause form; `exact`/immediate are
> built anyway (zero marginal cost, the same existing time-resolution
> helper); `conditionOnset`/`referencedByAttribute` are named unbuilt
> (interpreter throws, the same disposition an unsupported condition
> type already gets). Real Synthea's own module CONTINUES past `Death`
> to whatever it transitions to next — this project's own C2 ruling
> deliberately departs from that (the walk terminates AT `:death`), a
> disclosed simplification, not an oversight. Full account, with source
> citations: `components/sim-trajectory/docs/gmf-interpreter.md` section
> 10's own "C1 — Death forms" and "C5 — stroke.json's own closure
> survey."

**C2 — Terminal contract, co-landed:** the trajectory gains a `:death`
event carrying its time; the walk terminates at it; the invariant "no
trajectory event follows `:death`" lands in the same change as the
event, property-tested. Death ends the WALK — donor/post-mortem content
remains future work exactly as the deferred row already says,
named-not-built.

**C3 — Ground-truth wiring is scope-gated by the Step 1 gap table.** The
wave implements the MINIMAL COHERENT PATH for stroke's death: whatever
the engine fold needs so derived patient state reaches `:expired` per
patient-state-model.md's own row, and whatever check invariant makes
post-mortem incoherence structurally visible for the event classes
stroke actually emits. Anything beyond that minimal path (full
post-mortem validity catalog, donor pathways, emit-hl7 discharge-
disposition rendering) is a named finding with its wave-home proposed,
not built. If the gap table shows even the minimal path requires a
pathway-IR or sim-model schema change, ESCALATE with the evidence before
building.

> **C3 gap table (filled Step 1, 2026-08-02):** checked against the LIVE
> code, not the docs' own prose — `:expired` appears NOWHERE in
> `components/sim/src` except three lines of PROSE in `ehrt.sim.check`'s
> own comments; docs-only. `order-only-when-admitted`/`clinical-content-
> only-when-admitted` already generalize to cover `:expired`
> automatically once it is a real, distinct status value (zero new
> invariant needed for that half). Declared minimal coherent path:
> (1) `:expired` joins `PatientState`'s `:status` enum. (2) `Death` maps
> via the EXISTING `:discharge` IR step (C4), gaining two new optional
> fields (`:disposition [:enum :expired]`, `:codes`) — no new IR step
> type. (3) `:discharge`'s own `decide`/`evolve` branch on `:disposition`:
> `:expired` sets `:status :expired`, leaves `:location`/`:attending`
> UNCHANGED, and — a finding the docs' own gap table didn't name —
> SUPPRESSES the existing bed-ready-transfer coupling (no bed is
> actually vacated). (4) One new structural invariant,
> `expired-patient-retains-location`, named explicitly rather than left
> to fall out of the generalization by accident. No pathway-IR
> step-type or `sim-model` schema change beyond two optional fields —
> no escalation triggered by this table. Full account:
> `components/sim-trajectory/docs/gmf-interpreter.md` section 10's own
> "C3 — the `:expired` gap table."

**C4 — compile-trajectory mapping, ruled with a rebuttable default:** a
`:death` event maps into the compiled pathway WITHOUT a new IR step
type — death inside an encounter attaches as that encounter's terminal
disposition; death outside any encounter closes the pathway at that
timestamp. If characterization shows this default cannot represent
stroke's actual death sites, that is the C3 escalation, with evidence.

**C5 — stroke vendors per the full Wave B closure discipline** even
though its survey row predicts a trivial closure: fetch the real file at
the pin, resolve its transitive closure (expected: none), verify its
transition kinds against the now-SIX known kinds (including
`lookup_table_transition` — its presence would be a D6 drop with
evidence, not a build), survey row, D7 hidden-import check, NOTICE,
provenance header, ADR-0013 point 4 on the closure.

**C6 — Vendored test proves the tail:** alongside load-clean and
fixed-seed determinism, at least one seed's walk must REACH the death
branch (terminal `:death`, nothing after it, engine-side derived state
`:expired` if C3's minimal path lands engine-side) and at least one must
complete the non-death path. Seed selection uses the established
mixer-RNG pattern, never sequential raw seeds (the Wave B `Random`-
clustering lesson).

**C7 — Step 0 bookkeeping riders:** (a) a dated fix-forward note in
`.agents/plans/2026-08-02-gmf-coverage-plan.md`'s payoff sequence — Wave
B yielded `ear_infections`; UTI moves to Wave D scope, which now also
carries `lookup_table_transition` (sixth transition kind, ADR-0027
deviation 3) and UTI's dirty closure; (b) the same note names Wave D as
`lookup_table_transition`'s wave home. Landed this commit
(`.agents/plans/2026-08-02-gmf-coverage-plan.md`'s own dated note) and
in `.agents/plans/roadmap.md` (Wave C moved from Deferred to Now).

### Verification baselines

Fixed-seed walks of all four currently-vendored modules (`sinusitis`,
`appendicitis`, `sore_throat`, `ear_infections` — the same 6-seed × 2-sex
oracle ADR-0027 established) proven byte-identical before and after
every commit. `poly check` and `poly test :all skip:integration` clean
at every checkpoint, confirmed one final time at session close (Step 4)
across the FULL workspace.

### Deviation record

**Step 3 executes against a different target than C5/C6's own literal
text names, by author ruling, not silent substitution.** Step 1's own
characterization (`components/sim-trajectory/docs/gmf-interpreter.md`
section 10) found `stroke.json`'s own `Chance_of_Stroke` state gates
onset on `{"attribute": "stroke_risk", "default": 0}` — a real Synthea
engine attribute (Framingham cardiovascular risk, `CardiovascularDisease
Module`) this project has no source for. Honoring the JSON's own
`default: 0` literally (the only choice consistent with this project's
own code-passthrough/no-fabrication discipline) makes stroke onset, and
therefore the `Death` branch, structurally unreachable — not merely
rare — under a bare vendored run. Escalated to the author (design
channel, 2026-08-02) rather than silently resolved, since this
threatens C5/C6's own payoff claim directly, not a peripheral detail.
**Ruled: `stroke.json` stays deferred this wave**, the same D6 treatment
ADR-0027 already gave `urinary_tract_infections.json` — a module whose
own mandatory path can't be honestly resolved within scope drops from
vendoring, payoff shrinks honestly. `Death` (C1-C4) is built and proven
in full regardless, against this project's own hand-authored test
fixture rather than `stroke.json`'s own death branch — Step 3's own
revised scope. `stroke.json`'s own survey row
(`components/sim-trajectory/docs/gmf-interpreter.md`, the M7 appendix
table) carries a dated note naming the new gap and its own revisit
trigger (an attribute-sourced `distributed_transition` weight mechanism
plus a stroke-risk-equivalent data source, neither scoped this
session).

### Execution record

Session executed same day as ruled, all seven checkpoints (Step 0
through Step 4) landed. Commits, in order: `7e4204b` (Step 0, this ADR
+ coverage-plan payoff riders), `ed4f7bd` (Step 1, characterization —
Death forms, stroke's own closure survey, the `:expired` gap table, the
`stroke_risk` finding and its own escalation ruling), `a900f99` (Step
2a, Death state and terminal contract), `47d0f66` (Step 2b, compile-
trajectory death mapping), `380a3e2` (Step 2c, engine/check minimal
path), `66005ae` (Step 3, the hand-authored death-fixture proof, stroke
deferred). Full account, with source citations:
`components/sim-trajectory/docs/gmf-interpreter.md` section 10 and this
session's own session record
(`.agents/session-records/2026-08-02-gmf-coverage-wave-c.md`).

### Fence

This ADR covers Wave C's design only. Its own characterization (Step 1)
and build record (Step 2–4) are recorded in `components/sim-trajectory/
docs/gmf-interpreter.md` and this session's own session record, not
restated here. Wave D (state types needing IR + emitter homes) is not
started — see `.agents/plans/2026-08-02-gmf-coverage-plan.md` and
`.agents/plans/roadmap.md`'s own Deferred rows.

---


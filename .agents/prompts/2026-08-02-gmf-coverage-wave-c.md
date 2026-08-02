# 2026-08-02 — GMF coverage Wave C session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). The ext4 clone was already at
`origin/main`, `d8447e6`, confirmed clean via `git fetch` at session
start — exactly the HEAD the prompt itself named (Wave B's own final
commit).

## Prompt, verbatim

> 2026-08-02 — ehr-testing: GMF coverage Wave C — Death
>
> Context
> Third session of the GMF coverage-expansion arc (`.agents/plans/2026-08-02-gmf-coverage-plan.md`; Wave B landed `a92254b..d8447e6`, ADR-0027). Wave C builds `Death`: the interpreter learns Synthea's Death state, the trajectory gains a terminal death event, and death is wired through to the ground-truth patient state — completing `stroke.json`, the payoff module (its `Date` gap closed in Wave A; its ~17.5% Death tail is all that blocks it). This is the first wave that may legitimately touch the residual `components/sim` (engine fold, check invariants): the split made that boundary explicit, and cross-component commits are in scope where the characterization says the wiring lives there.
> One discipline governs this wave above all: captured ≠ implemented. The deferred-table's own Death row defers to "expired-state machinery this project has already captured" — in `components/sim/docs/ patient-state-model.md` (the `:expired` accumulator row) and `clinical-realities.md` (post-mortem event-validity rows). Captured in DOCS. A first grep shows `expired` in `check.clj` and nowhere else in sim's src. Whether the engine's evolve actually folds an expired status, and what emit-hl7 does with one, is precisely the installed-vs-used question this workspace has been burned by before — Step 1 answers it with a docs-vs-code gap table, and NOTHING in this prompt asserts the machinery exists in code.
> Regression oracle: fixed-seed walks of all four vendored roots (appendicitis, sinusitis, sore_throat, ear_infections-with-closure) byte-identical before and after every commit.
> Ceremony: R30-mode — commit and push at each checkpoint, unattended, with R30's safeguards (staged-scope `--stat` check per checkpoint, personal-info scan, message via file — Write tool per the Wave B double-quote-hazard memory — session record before final push, hooks as backstop; tags and repo-level `gh` outside the grant). Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `d8447e6`), record HEAD.
> Read first
>
> 1. `components/sim-trajectory/docs/gmf-interpreter.md` — the `Death` deferred row, the `stroke.json` survey row, §9's Wave B closure account (the method this wave's stroke characterization repeats), and the D1–D4 order contract.
> 2. `components/sim/docs/patient-state-model.md` (the `:expired` accumulator row) and `components/sim/docs/clinical-realities.md` (post-mortem event-validity rows) — the captured machinery whose implementation status Step 1 establishes.
> 3. `components/sim/src/ehrt/sim/engine.clj` (the evolve fold — where an expired status would live), `check.clj` (the one place `expired` appears today), and `compile_trajectory.clj` in sim-trajectory (where the death event needs a mapping).
> 4. `notes/ADRs.md` ADR-0027 (Wave B record, deviation precedents) and the Wave B session record for the pinned-commit Synthea fetch method.
> 5. `.agents/plans/2026-08-02-gmf-coverage-plan.md` + roadmap Wave C row (Step 0 amends both — the riders below).
>
> Author rulings (ruled 2026-08-02, design channel)
>
> * C1 — Death semantics come from Synthea source at the pinned commit, characterized before implementation (the D5 precedent): the Death state's forms (immediate, ranged/exact delayed, attribute- referenced), what each consumes from the person record or rng, and which forms `stroke.json` actually uses. The loader's all-or-nothing gate means every form PRESENT IN THE VENDORED MODULE must be supported; forms no vendored module uses are named in the docs as unexercised, not speculatively built.
> * C2 — Terminal contract, co-landed: the trajectory gains a `:death` event carrying its time; the walk terminates at it; the invariant "no trajectory event follows `:death`" lands in the same change as the event, property-tested. Death ends the WALK — donor/post-mortem content remains future work exactly as the deferred row already says, named-not-built.
> * C3 — Ground-truth wiring is scope-gated by the Step 1 gap table. The wave implements the MINIMAL COHERENT PATH for stroke's death: whatever the engine fold needs so derived patient state reaches `:expired` per patient-state-model.md's own row, and whatever check invariant makes post-mortem incoherence structurally visible for the event classes stroke actually emits. Anything beyond that minimal path (full post-mortem validity catalog, donor pathways, emit-hl7 discharge-disposition rendering) is a named finding with its wave-home proposed, not built. If the gap table shows even the minimal path requires a pathway-IR or sim-model schema change, ESCALATE with the evidence before building — that death is IR-homed would make it partially Wave D scope, and that is a design-channel call, not a session call.
> * C4 — compile-trajectory mapping, ruled with a rebuttable default: a `:death` event maps into the compiled pathway WITHOUT a new IR step type — death inside an encounter attaches as that encounter's terminal disposition; death outside any encounter closes the pathway at that timestamp. If characterization shows this default cannot represent stroke's actual death sites, that is the C3 escalation, with evidence.
> * C5 — stroke vendors per the full Wave B closure discipline even though its survey row predicts a trivial closure: fetch the real file at the pin, resolve its transitive closure (expected: none), verify its transition kinds against the now-SIX known kinds (including `lookup_table_transition` — its presence would be a D6 drop with evidence, not a build), survey row, D7 hidden-import check, NOTICE, provenance header, ADR-0013 point 4 on the closure.
> * C6 — Vendored test proves the tail: alongside load-clean and fixed-seed determinism, at least one seed's walk must REACH the death branch (terminal `:death`, nothing after it, engine-side derived state `:expired` if C3's minimal path lands engine-side) and at least one must complete the non-death path. Seed selection uses the established mixer-RNG pattern, never sequential raw seeds (the Wave B `Random`-clustering lesson).
> * C7 — Step 0 riders (bookkeeping ruled in the design channel's Wave B review): (a) dated fix-forward note in `2026-08-02-gmf-coverage-plan.md`'s payoff sequence — Wave B yielded ear_infections; UTI moved to Wave D scope, which now also carries `lookup_table_transition` (sixth transition kind, ADR-0027 deviation 3) and UTI's dirty closure; (b) same note names Wave D as `lookup_table_transition`'s wave home so the finding has an owner.
>
> Steps
>
> 0. Records. Apply C7's riders to the coverage plan; roadmap Wave C → Now; land the Wave C design ADR (next number) with C1–C6 verbatim (attributed: ruled 2026-08-02, design channel) and marked placeholders for the C1 characterization and C3 gap table. Commit: `docs: Wave C design ADR (C1-C6); coverage-plan payoff riders (C7)`.
> 1. Characterize (gates all scope). (a) Fetch at the pin: `stroke.json` + its transitive closure + Synthea's Death state handling source. Blocked-on-fetch stops the session after Step 0, recorded. (b) C1 form inventory: which Death forms exist upstream, which stroke uses, what each consumes; fill the ADR placeholder. (c) C5 survey: stroke closure rows, transition-kind sweep, D7 check. (d) C3 gap table: for each captured claim in patient-state-model.md's `:expired` row and clinical-realities.md's post-mortem rows — implemented where (file:line), partially, or docs-only. Declare the minimal coherent path and this wave's engine/check scope from it; fill the ADR placeholder. Escalate here per C3/C4 if the IR is implicated. (e) Regression baseline hashes for the four vendored roots. Commit: `docs(sim-trajectory): Wave C characterization — Death forms, stroke closure, expired-machinery gap table`.
> 2. Implement, one commit per feature, red→green, oracle green, invariants co-landed: (a) Loader + interpreter Death per C1/C2 (forms stroke needs; terminal event + no-event-after-death property). `feat(sim-trajectory): Death state, terminal contract (Wave C C1-C2)` (b) compile-trajectory mapping per C4. `feat(sim-trajectory): death disposition mapping (Wave C C4)` (c) Engine/check minimal path per the gap table — commits scoped to `components/sim`, invariants co-landed, named findings for everything deliberately not built. `feat(sim): fold death to expired state (Wave C C3)` (adjust to the gap table's actual shape).
> 3. Vendor stroke per C5/C6. `feat(sim-trajectory): vendor stroke closure (Wave C payoff)`.
> 4. Close out. Full suite + `poly check` green; oracle byte-identical finally; docs fix-forward (Death row moved to the v1 disposition table Wave B's precedent set; stroke survey row; payoff map — A+C → stroke now real); ADR finalized with execution + deviation records; roadmap Wave C → Done with shas; session record; self-archive this prompt to `.agents/prompts/`. Final commit: `docs: Wave C records (ADR, survey, roadmap; archives prompt)`.

## How the rulings were applied, and the one real deviation

C1/C2/C4/C5/C6 were applied exactly as written. C3's own gap table
(Step 1) found the minimal coherent path touched no pathway-IR step
type and no `sim-model` schema beyond two optional fields on the
already-existing `:discharge` step — no escalation triggered by that
table.

**A genuinely new, unplanned finding — outside C1–C7's own named
scope, discovered by C5's own real-closure characterization — changed
Step 3's actual execution.** `stroke.json`'s own `Chance_of_Stroke`
state gates onset on `{"attribute": "stroke_risk", "default": 0}`, a
real Synthea engine attribute (`CardiovascularDiseaseModule`'s own
Framingham risk score) this project has no source for, whose own
JSON-specified default makes onset — and therefore the `Death` branch
this whole wave exists to unlock — structurally unreachable if honored
literally (this project's own code-passthrough/no-fabrication
discipline leaves no principled alternative). Unlike the two prior
attribute-sourced-data gaps this project has already accepted
(`type_of_care_transition`'s payer attribute, `Active Allergy`), both
of which left a real, useful default branch reachable, this one's
default is a hard zero on the module's own mandatory onset path —
materially worse, and consequential enough to the wave's own stated
payoff claim ("completing stroke.json") that it was escalated to the
author rather than silently resolved either way. Ruled (design
channel, mid-session): `stroke.json` stays deferred this wave, the
same D6 treatment ADR-0027 already gave a dirty closure — `Death`
itself is built and proven in full regardless, against this project's
own hand-authored `death-fixture.json` rather than stroke's own death
branch. Step 3's own commit message and scope reflect this ruling
directly (`feat(sim-trajectory): vendor a Death test fixture (Wave C
payoff)`, not the literally-named `vendor stroke closure`); Step 4's
own "payoff map" line is corrected fix-forward rather than written as
the prompt's own literal "A+C → stroke now real," which did not turn
out to be true this session. Full account, with source citations:
`components/sim-trajectory/docs/gmf-interpreter.md` section 10;
`notes/ADRs.md` ADR-0028's own Deviation record.

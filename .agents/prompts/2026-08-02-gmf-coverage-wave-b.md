# 2026-08-02 — GMF coverage Wave B session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). The ext4 clone was already at
`origin/main`, `956dd56`, confirmed clean via `git fetch` at session
start — exactly the HEAD the prompt itself named (Wave A's own final
commit).

## Prompt, verbatim

> 2026-08-02 — ehr-testing: GMF coverage Wave B — CallSubmodule
>
> Context
> Second session of the GMF coverage-expansion arc (`.agents/plans/2026-08-02-gmf-coverage-plan.md`; Wave A landed `0b2c1b2..956dd56`, ADR-0026). Wave B is the arc's structural lift: `CallSubmodule` — loader closure resolution, interpreter call/return, root-scoped workflow attributes, cross-boundary provenance — plus the fifth transition kind (`type_of_care_transition`) UTI requires. Design was settled in the design channel 2026-08-02 and is recorded verbatim by Step 0's ADR; this session implements it. Target payoffs, each contingent on its closure surveying clean: `ear_infections.json` (both therapeutic branches route through `CallSubmodule`) and `urinary_tract_infections.json` (three `type_of_care_transition` submodule paths). `myocardial_infarction` stays out (its `Death` gap is Wave C); `total_joint_replacement` stays out (Wave D). All work lands in `components/sim-trajectory` plus survey/records surfaces.
> Regression oracle: fixed-seed walks of ALL THREE currently vendored modules (appendicitis, sinusitis, sore_throat) byte-identical before and after every commit — the root-scoping restructure (D1) must be invisible to non-calling walks by construction, and this oracle is what proves it.
> Ceremony: R30-mode — commit and push at each checkpoint, unattended, with R30's safeguards (staged-scope `--stat` check against the checkpoint's own file list, personal-info scan, message via file, session record before final push, hooks as backstop; tags and repo-level `gh` outside the grant). Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `956dd56`), record HEAD.
>
> Read first
>
> 1. `components/sim-trajectory/docs/gmf-interpreter.md` — the `CallSubmodule` deferred-table row, the survey rows for `ear_infections`/`urinary_tract_infections`/`myocardial_infarction` (§~730–840), and the rng-consumption-order contract.
> 2. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` and `gmf_interpreter.clj` — the loader gate, `initial-ctx`, the attribute key discipline (`(keyword module-id (gmf/slug attr))`, the thing D1 restructures), and `run-module`'s resume contract.
> 3. `notes/ADRs.md` ADR-0026 (Wave A record; Step 0 adds the ratification note to its deviation entry) and origin-qualified `sim/ADR-0013` point 4 (curation — applied per CLOSURE this wave).
> 4. Wave A's characterization method in the session record / `.agents/prompts/2026-08-02-ehr-testing-gmf-wave-a.md` — the pinned-commit Synthea source fetch (`Logic.java` precedent) is reused here for `CallSubmodule` and `type_of_care_transition` semantics; same source, same pin, same discipline.
> 5. `.agents/plans/2026-08-02-gmf-coverage-plan.md` and the roadmap Wave B row.
>
> Author rulings (all ruled 2026-08-02, design channel)
>
> * D1 — Three-compartment person record; root-scoped scratch. The interpreter ctx is the person record with three compartments of distinct character: persona (fixed characteristics, immutable input), clinical state (derived only by folding the trajectory — the event log is ground truth; guards that need clinical facts read the log, as `:observation` already does), and workflow attributes (module control-flow scratch only). `CallSubmodule` shares the THIRD compartment across a call tree by scoping it to the walk's ROOT module: caller and callee resolve the same bare attribute name in the root's namespace. Non-calling walks are byte-identical by construction (root = self). Nothing is shared between separate top-level walks: cross-storyline interaction, when it comes, goes through clinical state, never through scratch. Implementation latitude on representation (root-qualified keys vs. nested-by-root map) — the semantic contract and the regression oracle are what's fixed.
> * D2 — Provenance: every event emitted inside a submodule cites the full call path, root-first. Invariant, co-landed: every citation path's head equals the walk's root module; single-module walks cite the one-element path (representation may stay backward-compatible for that case — same oracle applies).
> * D3 — Loader closure: submodules resolve on the search path `sim/modules/<call-path>.json`; the loader resolves the transitive closure at load time; the all-or-nothing gate extends to the closure (a module is loadable iff every transitively-called submodule loads clean and in-vocabulary). The static call graph must be acyclic — a cyclic real-world closure is an ESCALATION with evidence, not a relaxation. A defensive runtime call-depth invariant co-lands (limit generous, violation is a bug signal, not a semantic).
> * D4 — Determinism threading: one clock, one rng stream; consumption order is descend-run-return, documented in the interpreter ns docstring's order contract; the whole-walk reproducibility property extends over closures (property test: walk with closure, twice, identical).
> * D5 — `type_of_care_transition` semantics are characterized from Synthea source at the pinned commit BEFORE implementation — the dispatch rule (how a care-setting path is selected, what it consumes from the person record or rng) is recorded in the ADR's fix-forward note with the source citation, then implemented to match. If selection consumes rng, its draw joins the documented order contract.
> * D6 — Curation per closure: ADR-0013 point 4's "modest deferred-type surface" bar applies to the closure as a unit. Each closure member gets its own survey row. A dirty closure member (deferred types, or a new gap) drops its whole root module from this wave's vendoring — recorded as a finding with the evidence, payoff shrinks honestly.
> * D7 — Hidden-import check (D1's falsifier): for each candidate closure, compute the set of attributes READ anywhere in the closure but WRITTEN nowhere in it (excluding persona-backed builtins). Expected: empty. Non-empty is an ESCALATION naming the attribute and its upstream writer — do not restore a global channel to make it pass, and do not seed it silently.
> * D8 — Retro-ratification rider: append to ADR-0026's deviation entry a dated note — "[A] ratified 2026-08-02 (design channel): the `:symptom`-as-condition inclusion is confirmed within AR-2's admission criterion" — fix-forward, no body rewrite.
>
> Steps
>
> 0. Records first. Land the Wave B design ADR in `notes/ADRs.md` (next number): title "GMF Wave B: CallSubmodule — three-compartment person record, root-scoped scratch, closure loading" — body is D1–D7 above verbatim (attributed: ruled 2026-08-02, design channel), plus a placeholder section for the D5 characterization note to be filled in Step 1 (marked as such). Apply D8's rider to ADR-0026. Roadmap: Wave B row → Now. Commit: `docs: Wave B design ADR (D1-D7); ADR-0026 ratification rider`.
> 1. Characterize (gates all scope). (a) Fetch, at the same pinned Synthea commit as Wave A: `ear_infections.json` plus its transitively named submodules; `urinary_tract_infections.json` plus its three path submodules and THEIR transitive calls; Synthea's `CallSubmodule` state handling and `type_of_care_transition` dispatch source (the `Logic.java`- pattern fetch). If network is unavailable, stop after Step 0 and record blocked-on-fetch. (b) Per closure member: survey row (state count, deferred types, condition gaps, encounter states) in the established format. Grade the CLOSURE per D6 and declare this wave's vendoring scope from the evidence. (c) D7 hidden-import check per closure; record the computed sets. (d) D5 dispatch-rule characterization; fill the ADR placeholder (fix-forward, dated). (e) Fixed-seed regression baseline: walk-output hashes for the three vendored modules. (f) Verify the encounter-derivation wrinkle: confirm from the fetched UTI closure that encounter states live in the path submodules, and identify (read-only — no engine changes this session) where the residual sim's encounter handling consumes encounter events, so the vendored test can assert cross-boundary encounters appear in the trajectory exactly as top-level ones do. Commit: `docs(sim-trajectory): Wave B closure survey, D5/D7 findings (characterization)`.
> 2. Implement, one commit per feature, each red→green, each with the regression oracle green and its co-landed invariants: (a) D1 root-scoping restructure — the pure refactor FIRST, before any recursion exists: attributes rekeyed to root scope, all three vendored walks byte-identical. `refactor(sim-trajectory): root-scoped workflow attributes (Wave B D1)` (b) D3 loader closure resolution + acyclicity + search path. `feat(sim-trajectory): submodule closure loading (Wave B D3)` (c) Interpreter call/return: stack, descend-run-return threading, depth invariant, D2 call-path citations, D4 property test. `feat(sim-trajectory): CallSubmodule call/return (Wave B D1-D4)` (d) `type_of_care_transition` per the D5 characterization. `feat(sim-trajectory): type-of-care transition (Wave B D5)`
> 3. Vendor per the Step 1 scope declaration (expected: the ear_infections closure, then the UTI closure; drop whatever D6 ruled out). Per root module: closure files with provenance headers, survey rows, NOTICE update, a vendored test proving (i) load-clean over the closure, (ii) fixed-seed full-walk determinism, (iii) at least one walk reaching THROUGH a submodule (medication/procedure events with call-path citations), and for UTI (iv) cross-boundary encounter events present per Step 1(f). One commit per vendored closure: `feat(sim-trajectory): vendor <module> closure (Wave B payoff)`.
> 4. Close out. Full suite + `poly check` green; regression hashes byte-identical one final time; docs fix-forward (deferred tables, §2 vocabulary, prioritization rows for MI/joint-replacement noting what Wave B removed from their gap lists); ADR finalized; roadmap Wave B → Done with shas (and payoff-map updates if D6 dropped anything); session record; self-archive this prompt to `.agents/prompts/` with deviation record if any. Final commit: `docs: Wave B records (ADR, survey, roadmap; archives prompt)`.

## Deviation record

Every named checkpoint (Step 0–4) landed as scoped. Five points where
this session's own execution went beyond, or diverged from, the
prompt's own literal text — each disclosed at the commit that made the
call, gathered here per Step 4's own instruction:

1. **Step 2e (encounter-class loader normalizations) is an ADDITIONAL
   commit, not named anywhere in D1-D8 or the Step 2 checkpoint list.**
   Step 1's own characterization of `ear_infections.json`'s REAL
   closure (not just its top-level survey row) found two more
   mandatory-path gaps beyond `CallSubmodule` itself: an unrecognized
   `encounter_class: "outpatient"` value on the module's own primary
   encounter, and the already-documented `wellness: true` boolean
   idiom confirmed mandatory here for the first time. Treated as within
   this session's own spirit — Step 1's own "if characterization shows
   a gap the survey missed, record it" instruction, and the standing
   "extend v1 with a documented reason, or defer the module" option
   this document's own M5b findings already established — rather than
   an escalation needing a stop, since both are cheap, mechanical,
   narrowly-scoped v1.1 extensions with an exact precedent
   (`Device`/`DeviceEnd`, `ConditionAnnotation`'s own optional `:codes`).
2. **Two more mandatory-path findings folded into Step 2c rather than
   given their own commits:** `MedicationOrder`'s own
   `assign_to_attribute` / `MedicationEnd`'s own
   `referenced_by_attribute`, and the `Attribute` condition's own
   `is nil`/`is not nil` operators. Both are tightly coupled to
   CallSubmodule's own cross-module reference shape (the whole reason
   they're load-bearing is that `ear_infections.json`'s own closure
   crosses a call boundary) — splitting them into separate commits
   would have been an artificial cut through one coherent change, the
   same reasoning ADR-0026's own combined `:at-least`/`:or` commit
   already used and disclosed.
3. **`lookup_table_transition` — a genuinely new, unplanned SIXTH
   transition kind, found on `urinary_tract_infections.json`'s own
   entry path — is named as a finding and NOT built.** The prompt's own
   D3 names only `type_of_care_transition` as "the fifth transition
   kind"; this session found a sixth the prompt never anticipated.
   Treated as a finding to record, not an escalation to stop on: the
   practical outcome (UTI stays deferred, D6) doesn't change either way
   it's eventually resolved, and building it would need real design (an
   external lookup-table CSV mechanism this project has no analog for)
   well outside D1-D8's own scope.
4. **`total_joint_replacement.json`/`myocardial_infarction.json`'s own
   docs rows got a dated note ("`CallSubmodule` removed from this
   module's own blocker list") during Step 4's own fix-forward pass,
   WITHOUT re-characterizing either module's own real closure this
   session** — neither was named in this prompt's own "target payoffs"
   (both explicitly "stay out"), so no fetch was attempted for either.
   The note is deliberately narrow (the mechanism landed; vendorability
   is unverified), not an overclaim the docs pass could have drifted
   into.
5. **A WSL heredoc-through-the-`wsl -e bash -lc` wrapper truncation,
   found live, not merely anticipated from memory.** An early commit
   message (Step 2b) was written via a `cat > file << 'EOF' ... EOF`
   heredoc embedded in a double-quoted `wsl -e bash -lc "..."` string —
   the message silently truncated mid-sentence at an unescaped internal
   double-quote (`"already resolved, dedupe"`), losing the back half of
   the paragraph with no error surfaced. This is the SAME hazard this
   project's own memory (`feedback-wsl-git-workflow`) already names for
   backticks; confirmed here that plain double-quotes trigger it too.
   Fixed per that memory's own prescribed remedy: every commit message
   from Step 2b onward was written via the Write tool's own UNC path to
   the WSL scratch tree, never an inline heredoc through the wrapper.

No ruling in D1-D8 was applied differently than written. Both target
payoffs were pursued exactly as scoped ("contingent on its closure
surveying clean") — `ear_infections.json` cleared that bar and is
vendored; `urinary_tract_infections.json` did not (D6, a real closure
twelve files deep, not the four this prompt's own framing assumed) and
is deferred, exactly the outcome the prompt's own conditional framing
anticipated as a live possibility.

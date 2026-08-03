# 2026-08-03 — GMF coverage Wave F session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`) — confirmed at session start that
a native-path Read hit the STALE `/mnt/c` clone (behind by five
commits, missing all of ADR-0035/Wave F0), before any edit was made;
switched to the UNC path for every subsequent Read/Edit/Write. Preflight
otherwise clean: ext4 clone at `origin/main`'s own HEAD (`e26c9c1`), no
uncommitted changes; Synthea checkout (`/home/mg/synthea-checkout`)
confirmed at the pin (`7e08387c68a7f0e21d13076609a159fd473fc902`) via
`git rev-parse HEAD`; ADR-0035 confirmed the latest ADR, next ADR 0036.

## Prompt, verbatim

> 2026-08-03 — Build session: Wave F — Counter, ImagingStudy, SupplyList + condition-type rider
>
> Context
>
> Post-F0 census (`2026-08-03-synthea-7e08387-wave-f0.edn`, ADR-0035) ranks Wave F at 29 modules: `Counter` (14), `ImagingStudy` (10), `SupplyList` (5), plus a rider of three condition types — `:race`, `:socioeconomic-status`, `:not` — unblocking 4 more (counts are lower bounds; F0 demonstrated fail-fast masking). The design channel pinned all semantics from Synthea source at the pin (`7e08387c68a7f0e21d13076609a159fd473fc902` — `State.java` classes `Counter`/`ImagingStudy`/`SupplyList`, `Logic.java` classes `Race`/`SocioeconomicStatus`/`Not`/`VitalSign`); cite, don't re-derive. The `:vital-sign` condition and `VitalSign` state are DEFERRED OUT of this wave by ruling (AR-7). No vendored root uses any Wave F state or condition type (verified by recursive scan of state bodies including nested condition trees), so the oracle claim is PURE IDENTITY — with one hazard called out in AR-5.
>
> Read first
>
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` (state schema, type map, rejection families) and `gmf_interpreter.clj` (`:set-attribute` — Counter's template; `emit-and-advance` — ImagingStudy's template; condition evaluation — where guards dispatch; `rand-int-in`)
> 3. `components/sim-model/src/ehrt/sim_model/persona.clj` (schema and the sex-sampling pattern the new fields follow) and `config.clj`
> 4. `components/sim-trajectory/docs/gmf-interpreter.md` §15 (census sections) and the log-only-fact precedent (`ConditionEnd` with no open encounter — "real, worth keeping, not worth a message")
> 5. `notes/ADRs.md` — ADR-0035 (incl. its two disclosed unowned findings: census same-day filename overwrite; UTI Observation states carrying never-normalized raw `:distribution`); next ADR expected 0036
> 6. `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387-wave-f0.edn` (header params for the re-run)
>
> Author rulings (design channel, 2026-08-03; record in ADR-0036)
>
> * AR-1 (Counter). Upstream: read patient attribute (missing → 0), increment or decrement by `amount` (default 1 when absent/0 — port the legacy default), write back numeric. Zero draws, no time advance, no trajectory event. Sim: follow `:set-attribute`'s exact shape — module-namespaced attribute, pass-through outcome. The namespacing narrows upstream's shared-attribute semantics (cross-module counters would not see each other); DISCLOSE in the state's doc entry — no vendored or census module currently depends on cross-module counter visibility, and the honest-absence rule (AR-4) will surface it if one ever does.
> * AR-2 (ImagingStudy). Upstream: records an imaging study (series × instances, modality codes) AND a companion Procedure with a fixed 30-minute stop; extends `State`, returns true immediately — NO module-clock advance (unlike duration-bearing Procedures — pin this in a test). Draws: when `min_number_series`/`max_number_series` bounds are given, series count = one uniform integer draw inclusive (upstream `rand(min, max+1)` int-cast ≡ `rand-int-in min max`); per-series instance bounds likewise, one draw per materialized series. Consumption is deterministic GIVEN prior draws — the same branching-consumption family distributed transitions already establish; state this in the ADR. Sim shape: one trajectory event `:imaging-study` carrying procedure code, modality, and the drawn series/instance counts (glass-box), compiling to the SAME IR step family a `:procedure` produces — upstream's own companion-procedure move, with the 30-minute stop as record metadata, not clock advance. DICOM UID synthesis is NOT ported (record-level identity belongs to the sim's identifier machinery if ever needed; defer, disclose).
> * AR-3 (SupplyList). Upstream: for each `{code, quantity}` component, record supply usage; no draws, no clock, no encounter requirement. Sim: a log-only trajectory fact (`:supply-list` with components) compiling to NO IR step — the ConditionEnd no-open-encounter precedent verbatim. Wire rendering of supplies is out of scope for hospital traffic v1; disclose.
> * AR-4 (condition rider + persona fields + honest absence). Implement `:not` (recursive negation — trivial), `:race` (case-insensitive match against persona race; upstream vocabulary: White, Native, Hispanic, Black, Asian, Other), and `:socioeconomic-status` (match against persona category: High, Middle, Low). Persona gains OPTIONAL `:race` and `:socioeconomic-category` fields following the sex-sampling pattern, sampled ONLY when persona config supplies category weights (authored scenario content). Honest-absence rule: evaluating `:race`/`:socioeconomic-status` against a persona lacking the field is a WALK ERROR (a value, recorded, per result-not-throw) — NOT a silent false. Silent-false is the SetAttribute-nil species ADR-0035 just eradicated; do not reintroduce it at the guard layer. The census persona-config gains both fields (fixed values, recorded in the artifact header) so census walks exercise the guards.
> * AR-5 (draw-law hazard — persona sampling). The new persona fields draw ONLY when config requests them. Absent config = zero additional draws = every existing persona byte-identical. This is the wave's one identity hazard; a persona-stream shift would perturb EVERY digest. The oracle bracket (AR-6) is the proof; a targeted unit test (persona with and without the new config, draw counts compared) is the co-landing invariant.
> * AR-6 (oracle bracket — pure identity). No vendored root uses any Wave F state or condition type (recursive-scan verified, including nested condition trees inside transitions — the scan method is named because a top-level grep missed a root once, ADR-0032's execution note). Every oracle-covered root byte-identical across the bracket; any change is a STOP-AND-ESCALATE.
> * AR-7 (explicit deferrals — record, don't drift). OUT of Wave F by ruling: the `:vital-sign` condition and `VitalSign` state (modules `contraceptives`, `covid19`) — they require a vital-sign register with baseline values, which is engine-delegated content upstream (Synthea's lifecycle engine sets baselines modules then test); supplying it is authored calibration content, pairing naturally with re-scoped Wave E — record as a named roadmap item "vital-sign channel (calibration content + VitalSign state + :vital-sign condition)". Also deferred to Wave I: lookup-table columns `race` and `time` (modules `acute-myeloid-leukemia`, `hiv-diagnosis`); note the `race` column shares the persona-race prerequisite this wave lands.
> * AR-8 (census re-run + substance note + F0's unowned findings). Re-run the census (same header params plus the AR-4 persona fields, disclosed in the header), commit alongside prior artifacts with a disambiguated name (the same-day overwrite bug is STILL OPEN — work around by filename as F0 did; do not fix the tool this session). Expected: the 29 core + 4 rider modules move (resolved or next-blocker-surfaced — classify); vendored roots unmoved (STOP-AND-ESCALATE otherwise). Records step also lands, as dated docs notes: (a) a §15 substance note — 26 of 42 pre-F `:ok-walked` modules produce zero events on every census seed (immediate-terminal on absent persona attributes, cross-module attribute blocks, empty horizon-completes — stroke included), so walk-verification attests determinism of what walks touch, which for the gated chronic cluster is currently almost nothing; (b) roadmap Deferred rows for: census tool refinements (substance qualifier on verdicts, optional per-module census seeds, same-day filename fix), the UTI Observation raw-`:distribution` normalization gap (ADR-0035's disclosure), and AR-7's two deferral items.
>
> Steps
>
> Step 0 — Preflight. Standard; confirm ADR-0035 at origin, next ADR 0036, Synthea checkout at pin present.
> Step 1 — Counter. Loader (type map, schema) + interpreter (SetAttribute-shaped pass-through) + tests (increment, decrement, missing-attribute default, legacy amount default, zero draws). Commit: `feat(sim-trajectory): Counter state -- attribute arithmetic, SetAttribute-shaped (ADR-0036 AR-1)`
> Step 2 — ImagingStudy. Loader (schema incl. series/instances bounds) + interpreter (`:imaging-study` event, companion-procedure IR compile, draws per AR-2, no clock advance pinned by test) + emit path exercised in at least one test rendering the companion procedure. Commit: `feat(sim-trajectory): ImagingStudy -- imaging event with companion procedure, no clock advance (ADR-0036 AR-2)`
> Step 3 — SupplyList. Loader + log-only fact + compile-to-no-step test (the ConditionEnd precedent cited in the docstring). Commit: `feat(sim-trajectory): SupplyList as log-only supply facts (ADR-0036 AR-3)`
> Step 4 — Condition rider + persona. Persona optional fields + conditional sampling (AR-5 unit test with draw counting); `:not`, `:race`, `:socioeconomic-status` conditions; honest-absence walk errors + tests for both present and absent personas. Commit: `feat(sim): race/socioeconomic persona fields + not/race/ses conditions, honest absence (ADR-0036 AR-4/AR-5)`
> Step 5 — Oracle bracket. Pure identity per AR-6 across `<tip-before-Step-1> -> <Step 4 tip>`. Record the table; any change escalates.
> Step 6 — Census re-run. Per AR-8: new artifact (disambiguated filename), movement classification, vendored-root anchor check. Commit: `docs(sim-trajectory): census re-run after Wave F -- Counter/ImagingStudy/SupplyList class closed (ADR-0036)`
> Step 7 — Records. ADR-0036 (AR-1..AR-8 verbatim, attributed; execution note: oracle table, census classification). AR-8's dated docs notes and Deferred rows. Roadmap: F → Done; G design session enters Next (its ledger per ADR-0031 AR-2 and the census wellness tags). Session record + prompt self-archive + budget check. Commit: `docs: wave F records -- 29+4 module wave landed, deferrals named (archives prompt)`
>
> Fences
>
> * No wellness/Wave-G work; no VitalSign work in any form (AR-7's deferral is a ruling, not an oversight); no census-tool fixes beyond the filename workaround.
> * AR-5's persona-draw conditionality is load-bearing for AR-6; if the identity bracket breaks, suspect it first.
> * Emit vocabulary beyond the companion procedure (imaging ORM/ORU, supply wire formats) is out of scope; glass-box facts only.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation record (dated 2026-08-03, this session)

- **Read-first item 3's "sex-sampling pattern" premise did not hold.**
  Sex sampling in `persona.clj` is unconditional and config-blind (a
  fixed 50/50 flip, zero config knobs) — there is no PRE-EXISTING
  conditional-draw pattern to follow. Resolved by analysis, not
  escalated: AR-5's own actual ruling (draw only when config supplies
  weights) is self-consistent and correct for a different reason than
  "following existing precedent" — it is the ONLY way to satisfy AR-5's
  own stated goal ("absent config = zero additional draws = every
  existing persona byte-identical") for a field being added to an
  ALREADY-SHIPPED generator, and does not actually conflict with the
  fixed-RNG-consumption law (`sim/ADR-0009`), which guards against draw
  count depending on a RUNTIME OUTCOME within one call, not against a
  CONFIG-TIME decision to add new optional content. Proceeded with
  AR-4/AR-5 as ruled; this correction is recorded in `persona.clj`'s
  own docstring, ADR-0036, and the roadmap, not silently smoothed over.
- **Steps 1-3 committed as ONE commit (`98f53ad`), not three**, and
  Step 4 as a second (`c9b2bbf`) rather than a fourth — the three
  states share files (`gmf.clj`/`gmf_interpreter.clj`/
  `compile_trajectory.clj`) and were implemented in one interleaved
  edit pass per file, not sequentially with a commit boundary between
  each. Splitting the diff after the fact into three commits would have
  meant hand-editing patch hunks rather than a real per-state boundary;
  disclosed in both commit messages, the session record, and here
  rather than silently deviating from the prompt's own Step numbering.
- **No independently-verified red→green per state/condition**, unlike
  ADR-0035's own Steps 1-3 (each stashed and re-run to prove a failing
  baseline). This session wrote code + tests together, ran the full
  suite once, green. The tests still exercise the exact mechanism (a
  legacy-amount-default test that would fail under `(or amount 1)` but
  passes under the actual `(if (or (nil? amount) (zero? amount)) 1
  amount)` fix), but the failing-baseline half of the ceremony was not
  separately staged. Disclosed as a deviation in the session record,
  not treated as equivalent rigor.
- **`gmf_test.clj`/`census_test.clj`'s own "still-deferred" example
  fixtures swapped from `ImagingStudy` to `VitalSign`** — not named in
  the prompt, but a mechanical consequence of AR-7's own ruling and the
  same "stale premise, not silently left" treatment those fixtures'
  own docstrings already document across three prior waves.
- Read-first item 4's "log-only-fact precedent" and item 3's persona
  schema were both read and cited correctly; no other premise mismatch
  found.

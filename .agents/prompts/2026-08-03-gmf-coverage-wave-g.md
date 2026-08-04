# 2026-08-03 — GMF coverage Wave G session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target. Preflight clean:
ext4 clone at `origin/main`'s own HEAD (`58fdd9c`), no uncommitted
changes; ADR-0036 confirmed the latest ADR, next ADR 0037. No
persistent Synthea checkout existed on this machine at session start —
one was cloned fresh into a scratchpad location for the census re-run
(Step 5), confirmed via `git rev-parse HEAD` to already equal the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) exactly (the repo's
current default-branch HEAD, at fetch time, happened to be the pin).

## Prompt, verbatim

> 2026-08-03 — Build session: Wave G — the wellness cycle (wait semantics, substitution retired)
> Context
> ADR-0031 AR-2 ruled the wellness cycle in scope and shaped it; the author (2026-08-03, design channel) ratified the final design: the wellness schedule is a PURE FUNCTION of persona and time — no stored schedule state anywhere — with the tick sequence derived from DOB via Synthea's own age-banded cadence, zero RNG draws. This session replaces the Wave B create-now normalization (the disclosed timing substitution, ADR-0031 AR-5(b)) with genuine wait semantics, retiring the `:wellness-timing` substitution tag for the 19 tagged modules and resolving the four max-steps loop modules (`med-rec`, `mend-program`, `metabolic-syndrome-care`, `veteran-substance-abuse-treatment`) whose spins are the substitution's artifact. Also ratified: the chronic-meds annual cap is DEFERRED to the calibration register (not implemented), the `Physiology` state is `:out-of-scope-by-ruling`, and lookup-column work WAITS for the post-G census read — none of it belongs to this session. Semantics are pinned from Synthea source at `7e08387c68a7f0e21d13076609a159fd473fc902`; the session transcribes the cadence table from source (locations in AR-1), never from memory.
> Read first
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. `notes/ADRs.md` — ADR-0031 AR-2/AR-5 (the ruling this implements), ADR-0036; next ADR expected 0037
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` — the wellness normalization clause and its ADR-0031 AR-5(b) disclosure comment (this session retires both, with dated notes, not deletion of the history)
> 4. `gmf_interpreter.clj` — the walk loop's time model, `resolve-time-advance`, horizon handling (the wait must respect `horizon-end-t` exactly as Delay does)
> 5. `components/sim/resources/sim/modules/ear_infections.json` (`Next_Wellness_Encounter` — the one vendored root whose behavior changes) and its interpreter + engine round-trip tests
> 6. Synthea at the pin: `src/main/java/org/mitre/synthea/engine/State.java` (Encounter wellness branch, ~950–980) and `src/main/java/org/mitre/synthea/modules/EncounterModule.java` (`recommendedTimeBetweenWellnessVisits`, ~215–265)
> 7. The post-F census artifact (`2026-08-03-synthea-7e08387-wave-f.edn`) header and the census tool's substitution-tag detector
> Author rulings (design channel, 2026-08-03; record in ADR-0037)
> * AR-1 (cadence as provenance-cited content). Transcribe the age-banded interval table VERBATIM from `EncounterModule.recommendedTimeBetweenWellnessVisits` at the pin into a data table (the vital-sign-table discipline: source file, line range, pin sha cited; facts-register entry; NOTICE if required by that discipline). EXCLUDE the chronic-medications annual-cap branch — deferred by ruling to the calibration register as a NAMED item ("wellness cadence chronic-meds cap"); the transcription note discloses the exclusion.
> * AR-2 (pure schedule function, zero draws). `next-wellness-tick` is a pure function: given persona DOB and a time t, iterate the cadence bands from birth (upstream's own next = previous + band(age) recurrence) to the first tick ≥ t. No RNG anywhere in the schedule. DOB varies per patient, so phase varies naturally; a hash-derived phase-offset knob is NOT built — record it as a future calibration option in the ADR. Zero draws means every non-wellness walk's rng stream is untouched (load-bearing for AR-6).
> * AR-3 (wait semantics). The loader maps `wellness: true` Encounter states to a distinct `:wellness-wait` state (schema entry; reason and transitions preserved), REPLACING the create-now normalization. The interpreter, on `:wellness-wait` at time t: advance the module clock to `next-wellness-tick(persona, t)` (bounded by `horizon-end-t` exactly as Delay is — parking past the horizon ends the walk in the same status Delay uses), open a wellness `:outpatient-visit` encounter at the tick (same trajectory event family the substituted path produced, now correctly timed), attach the state's reason, and proceed. Loop bounding falls out: each loop iteration advances a full cadence interval, so the horizon bounds iterations — the four loop modules are the acceptance evidence (AR-7).
> * AR-4 (engine unchanged; attachment deferred with a trigger). The wait is module-clock advance inside `run-module`; the engine drives walks as it does today and needs NO changes. Upstream's all-waiting-modules-attach-to-one-visit semantics only diverges when one patient runs multiple modules concurrently, which the engine's one-module-per-patient assignment does not do. DEFER shared-visit attachment with the named trigger "multi-module assignment per patient"; record in the ADR and roadmap Deferred.
> * AR-5 (substitution tag retirement + Physiology ruling). The census tool's `:wellness-timing` detector retires (dated note in the tool: the substitution no longer exists; the tag vocabulary stays extensible). The census gains its first `:out-of-scope-by-ruling` entry: the `Physiology` state (module `gallstones`), citing this ADR — Synthea's ODE physiology engine is out of the sim's fitness-for-purpose scope by author ruling.
> * AR-6 (oracle bracket — identity except one root, disclosed). Zero non-wellness draws change (AR-2) and no vendored root except `ear_infections` contains a `wellness: true` state (verified in the ADR-0031 AR-5(c) sweep). Therefore: every oracle batch byte-identical EXCEPT `ear-infections` (interpreter) and `ear-infections-engine` — those two change BY DESIGN (the visit moves from immediately-after-medications to the next cadence tick) and re-baseline with a dated disclosure that names the old and new behavior. Any OTHER batch changing is a STOP-AND-ESCALATE.
> * AR-7 (acceptance evidence). Co-landing tests: schedule-function band tests transcribed alongside the table (spot values asserted against the cited source lines); `:wellness-wait` interpreter tests (advance-to-tick, horizon parking, reason attachment) on inline fixture modules including a med-rec-shaped wait-act-loop that must terminate horizon-bounded with events; updated `ear_infections` interpreter and engine round-trip tests asserting the NEW timing (the infection now resolves at a cadence tick). The four upstream loop modules are census-level evidence (AR-8), not test dependencies — tests use inline fixtures only.
> * AR-8 (census re-run). Same header params, disambiguated filename (overwrite bug still open — workaround as before). Expected movement: the four loop modules leave `:walk-failed`; the 19 tagged modules lose their tags (retired detector) — their walk digests may change wherever a wait now times differently (expected, classify); `gallstones` moves to `:out-of-scope-by-ruling`; vendored roots other than ear_infections unmoved. Classify all movement; anything outside the expected classes is a finding.
> Steps
> Step 0 — Preflight. Standard; ADR-0036 at origin; next ADR 0037; Synthea checkout at pin present (AR-1 transcription + census).
> Step 1 — Cadence table + schedule function. AR-1 transcription (register discipline) + AR-2 pure function + band/spot tests. Commit: `feat(sim-trajectory): wellness cadence table (pinned Synthea content) + pure schedule function (ADR-0037 AR-1/AR-2)`
> Step 2 — Loader: `:wellness-wait`. Schema + mapping replacing the create-now normalization; dated retirement notes on the normalization clause's disclosure comment and the `gmf-interpreter.md` §4/§9 wellness notes (fix-forward chain: overturned → disclosed → resolved). Commit: `feat(sim-trajectory): wellness:true loads as :wellness-wait -- create-now substitution retired (ADR-0037 AR-3)`
> Step 3 — Interpreter wait semantics. AR-3 mechanics + AR-7 inline fixture tests + updated ear_infections round-trip tests (both layers) asserting new timing. Commit: `feat(sim-trajectory): :wellness-wait advances to the cadence tick -- loops horizon-bound (ADR-0037 AR-3/AR-7)`
> Step 4 — Oracle bracket. AR-6: identity everywhere except the two ear-infections batches; record old/new digests for those with the disclosure; escalate on anything else.
> Step 5 — Census tool tag retirement + Physiology ruling. AR-5 edits + re-run per AR-8; commit artifact + movement classification. Commit: `docs(sim-trajectory): census after Wave G -- substitution retired, loops resolved, Physiology out-of-scope (ADR-0037)`
> Step 6 — Records. ADR-0037 (AR-1..AR-8 verbatim, attributed; execution note: oracle table incl. the disclosed ear-infections re-baseline, census classification). Register item for the chronic-meds cap; roadmap: G → Done, attachment deferral row, H enters Next (its row already carries the straddle pointer — this session ADDS a note that `:wellness-wait` during a future history phase must fold state without emitting, per ADR-0031 AR-3, and that the UTI seed-777 dodge retires with H). Session record + prompt self-archive + budget check. Commit: `docs: wave G records -- wellness cycle landed, cadence registered (archives prompt)`
> Fences
> * No chronic-meds cap, no phase-offset knob, no shared-visit attachment, no lookup-column work, no Wave H pre-roll mechanics — all explicitly ruled out or deferred above.
> * Red→green per step is REQUIRED (ADR-0036's session skipped the baseline once; its own record flags it — do not repeat).
> * AR-6's expected-change set is exactly two batches; treat its precision as load-bearing.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation record

- **AR-1's read-first item 6 named `~215–265` for
  `recommendedTimeBetweenWellnessVisits`; the real method sits at lines
  176-213** (the chronic-meds cap at 209-211) — a minor line-number
  drift from the prompt's own estimate, found fetching the real source
  via WebFetch. Cited the ACTUAL lines throughout (the resource file,
  the NOTICE, the facts-register entry, the ADR), not the prompt's own
  estimate — a premise mismatch corrected at the source, not silently
  carried forward.
- **AR-2's own "first tick ≥ t" wording needed a live refinement to "first
  tick > t," found running this session's own AR-8 census, not merely
  anticipated by the prompt** — the real `med_rec.json` has zero delay
  anywhere in its own wellness-wait loop body, and the inclusive `>=`
  semantics returned the SAME tick forever once first reached at DOB
  exactly. Fixed within this session, before AR-8's census ran for
  real; recorded as a dated deviation on ADR-0037's own AR-2, not
  silently smoothed into the ADR's prose as if originally ruled that
  way. Full account: this prompt's own driving session record
  (`.agents/session-records/2026-08-03-gmf-coverage-wave-g.md`), the
  ADR's own AR-2 deviation text, and this file's own commit history
  (`203ed9f`).
- **Step 2 and Step 3 were verified together (one full green test run)
  before either was pushed, then split into two commits matching the
  prompt's own checkpoint boundaries and pushed in the same `git push`
  call** — the loader-only change in isolation would leave a real walk
  reaching `wellness: true` throwing (no interpreter case exists until
  Step 3), a genuine regression risk if that commit alone ever reached
  `origin/main`. Judged this as the correct reading of "commit and push
  at each checkpoint" (checkpoint granularity in the commit history,
  not a guarantee every individual commit's own tree passes the full
  suite standalone) rather than merging the two checkpoints into one
  commit. Not escalated; disclosed in the session record.
- **Read-first item 4 named the ear_infections interpreter AND engine
  round-trip tests; the engine-layer one was updated with a dated
  docstring note rather than a new timing-specific assertion** — the
  compiled `:outpatient-visit` IR step type is shared by both the
  primary and wellness encounters, so the engine layer cannot
  distinguish which produced a given step; the concrete new-timing
  assertion lives at the interpreter layer instead (AR-4's own boundary
  ruling: semantic walk claims are an interpreter-layer concern). Not
  escalated — a proportionate reading of "updated... asserting the new
  timing," not a shortfall against it.
- No other premise mismatch found. Every other read-first item (the
  wellness normalization clause and its disclosure comment, the walk
  loop's time model/horizon handling, the census artifact header and
  its substitution-tag detector) matched the prompt's own description
  exactly.

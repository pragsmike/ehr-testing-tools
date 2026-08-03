# 2026-08-03 — Engine closure-context fix session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). Preflight (Step 0): fetched and
confirmed `origin/main`'s own HEAD (`fbb5412`) already carries ADR-0032
and the Procedure-duration fix (`1ea1f4a`), the stated precondition —
proceeded without a STOP.

## Prompt, verbatim

> 2026-08-03 — Build session: engine closure context (J3's two gaps)
>
> Context
>
> ADR-0030 J3 confirmed the compile-trajectory/engine/emit round trip is broken for closure-having roots, two ways: `engine.clj`'s `:registered` decide calls `run-module` at the bare 5-arity, so (1) the submodule registry defaults to the root alone — `ear_infections`/UTI THROW at any `CallSubmodule` — and (2) there is no `initial-attributes` slot — TJR blocks silently at age 0, zero content. Three pinned tests under `components/sim-emit-hl7/test/` assert the broken behavior and are designed to fail loudly when this session's fix lands; this session converts them into real round-trip assertions. This is AR-6's SECOND defect-fix session — it assumes the Procedure-duration fix (ADR-0032) has already landed; verify that at preflight and STOP if it hasn't. The design channel surveyed the registration chain and made the shape rulings below; cite them rather than re-deriving.
>
> Read first
>
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. `components/sim/src/ehrt/sim/engine.clj` — the `:registered` decide method; `run`'s config docstring (`:modules` / `:module-assignment`); `module-for` / `registered-steps-for`
> 3. `components/sim/src/ehrt/sim/run.clj` — the `:modules` name-string resolution (currently `load-module` per name)
> 4. `components/sim-trajectory/src/ehrt/sim_trajectory/interface.clj` (`run-module`'s exported arities) and `gmf_interpreter.clj`'s `run-module` full-arity docstring (D2/D3 notes)
> 5. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` — `load-closure` (Result shape, resolve-fn discipline)
> 6. The three J3 tests: `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_ear_infections_test.clj`, `vendored_uti_test.clj`, `vendored_tjr_test.clj` — and the H7 `initial-attributes` usage in `components/sim-trajectory/test/`'s vendored TJR test (the authored seed value this session REUSES, same citation)
> 7. `notes/ADRs.md` — ADR-0029 (D2 H7, D3), ADR-0030 (J3), ADR-0031 (AR-6), ADR-0032 (duration fix — MUST exist); next ADR expected 0033
> 8. `.agents/plans/roadmap.md` — this session's `Next` row and the upgraded Deferred row it points at
>
> Author rulings (design channel, 2026-08-03; record in ADR-0033)
>
> * AR-1 (seed supply — author-ruled option (a)). `initial-attributes` is a SCENARIO knob, authored in run config: the config layer gains an optional `:module-initial-attributes` map, `{module-name {attr value}}`, at the same layer `:modules` name strings live. The same root can run seeded or unseeded in different configs. The engine stays generic — it threads what the config supplies and invents nothing. Provenance duty travels with the authoring site: the TJR seed value is the SAME authored, provenance-cited value the vendored TJR interpreter test already supplies (D2 H7 — reuse value and citation, do not re-derive). No declaration/validation machinery (option (c) was considered and not taken); the round-trip tests are the guard against unseeded-silence regressions.
> * AR-2 (registration shape). The ENGINE-FACING `:modules` entries become closure-shaped — `load-closure`'s `:ok` payload (`{:root <id> :modules {id->module ...} :tables {...}}`), plus the optional `:initial-attributes` from AR-1 attached per entry. A single-module root embeds as the singleton closure (`{:root id :modules {id module} :tables {}}`). This is a HARD SWITCH of the internal shape — `run.clj` and tests are the only producers — while the CLI/config-facing `:modules` (name strings) is UNCHANGED. `run.clj`'s resolution moves from `load-module` to `load-closure` per name, with the thin `io/resource` resolve-fns `load-closure`'s own docstring already specifies; a `:error` Result surfaces per result-not-throw at run-assembly time, never a throw mid-run.
> * AR-3 (the call). `:registered`'s decide calls `run-module` at the FULL arity: `(run-module root rng persona reg-t horizon-end-t (:modules closure) initial-attributes (:tables closure))` with `initial-attributes` defaulting `{}` when the config supplies none. `interface.clj` gains the full-arity re-export (purely additive).
> * AR-4 (draw law / oracle bracket). For every existing single-module engine run, the singleton-closure wrap and the empty seed map are DRAW-NEUTRAL and BYTE-NEUTRAL: the walk consumes the identical rng sequence. Therefore EVERY root `bin/oracle-src/ehrt/ oracle/digest.clj` currently covers must come out byte-identical in the oracle run — the identity set is DERIVED FROM THE TOOL'S OWN COVERAGE, not enumerated here (ADR-0032's execution note records why: its AR-4 enumerated a partition from an incomplete survey and the oracle immediately falsified it). Any change to a covered root's digest is a STOP-AND-ESCALATE.
> * AR-4b (oracle extension — co-landing). ADR-0032 disclosed that the oracle has never covered `total_joint_replacement` or the UTI closure (and covers `ear_infections` at the interpreter layer only): before this session those roots COULD NOT be engine-layer digested — they threw or silenced. This session makes them engine-runnable, so the invariant lands with the capability: extend `digest.clj` with engine-layer digest pairs for the three closure roots (ear_infections, UTI, TJR — TJR seeded via AR-1's config mechanism), and record their FIRST engine-layer baselines in the session record. This closes the ADR-0032 disclosure; note the closure in its dated-note trail.
> * AR-5 (test conversion). The three J3 pinned tests convert from asserting the broken behavior to asserting the working round trip, per their own docstrings' stated design. Minimum assertions per root: walk completes (no throw), compiled content non-empty where the module's own semantics produce operational steps in-window, emit renders (HL7 where applicable), and — for TJR — the seeded attribute actually unblocks the guard (non-zero content with seed; keep a small assertion that the UNSEEDED run still yields zero content, as the disclosed-behavior record, cited to AR-1's no-validation ruling). Co-landing: shape change + its invariants in the same commits.
>
> Steps
> Step 0 — Preflight. Build-session preflight. Verify ADR-0032 exists and the duration fix is at origin; STOP if not. `git fetch`, record tip.
> Step 1 — Loader/config plumbing. `run.clj`: `:modules` name resolution via `load-closure` (resolve-fns per its docstring); `:module-initial-attributes` config key (schema/validation updated, absent-means-`{}`); engine-facing entries per AR-2. Update `run`'s config docstring and `valid?` coverage. Suite green. Commit: `feat(sim): module registration carries the closure -- load-closure at run assembly (ADR-0033 AR-2)`
> Step 2 — Engine call + interface. Full-arity re-export in `interface.clj`; `:registered` decide per AR-3; `module-for` / `registered-steps-for` carry the closure entry through. Suite green — existing engine tests must not perturb (AR-4's neutrality is testable here cheaply via the pinned fixture before the oracle run). Commit: `fix(sim): :registered threads closure modules/tables/initial-attributes to run-module (ADR-0033 AR-3, J3)`
> Step 3 — Convert the three J3 tests (one commit per root, mirroring J3's own structure). Per AR-5. TJR's config supplies the H7 seed via `:module-initial-attributes`. Commits: `test(sim): ear_infections closure round trip real (J3 pin converted, ADR-0033)` `test(sim): urinary_tract_infections closure round trip real (J3 pin converted, ADR-0033)` `test(sim): total_joint_replacement round trip real -- seeded via config (J3 pin converted, ADR-0033)`
> Step 4 — Oracle bracket (identity half). `bin/regression-oracle` across `<tip-before-Step-1> -> <Step 3 tip>`, with digest.clj AS IT EXISTS at each endpoint (the tool extension in Step 5 deliberately comes AFTER, so this run compares like with like). Required per AR-4: every covered root byte-identical. Any change: STOP-AND-ESCALATE with the table. Record the table.
> Step 5 — Oracle extension (AR-4b). Extend `digest.clj` with engine-layer pairs for the three closure roots; run it once at the new tip and record their first baselines in the session record. Update the tool's docstring scope note (it currently cites J1's six-root ruling verbatim — dated note, not a rewrite). Commit: `test: regression oracle covers closure roots at engine layer (ADR-0033 AR-4b, closes ADR-0032 disclosure)`
> Step 6 — Records. ADR-0033 (AR-1..AR-4b..AR-5 verbatim, attributed; execution note with both oracle tables — Step 4's identity table and Step 5's first-baseline table). Dated notes: roadmap `Next` row → Done, Deferred row → FIXED; `gmf-interpreter.md` §13 G1/H7 notes gain the engine-layer resolution pointer; ADR-0032's oracle-gap disclosure gains its closure pointer. Session record (`.agents/session-records/2026-08-03-engine-closure-context.md`, both digest tables verbatim); self-archive prompt; budget check. Commit: `docs: engine closure-context records -- J3 closed, oracle identity proven (archives prompt)`
> Fences
>
> * Duration-fix territory (`resolve-time-advance`, `trajectory-and-advance`, loader duration normalization): untouched — landed last session.
> * No wellness/Wave-G work, however adjacent it looks.
> * AR-4's identity claim is absolute: this session adds capability; it changes no existing byte. An identity break is an escalation.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation-record appendix (per the prompt's own Fences clause)

- **Steps 1 and 2 landed as ONE commit, not the two the prompt's own
  Step 1/Step 2 commit messages describe.** Isolating Step 1's own file
  set with `git stash` and running the full suite found a real failure
  in `projects/conformance`'s own `sim-full-capability-gate-test`: the
  old `engine.clj`'s `modules-by-id` keys off a module's `:id`, which a
  closure map doesn't carry, so "no module assigned" and "the resolved
  closure" collide at the same `nil` key — a real, silent
  mis-assignment, not a cosmetic gap. Landing `run.clj`'s own hard shape
  switch (AR-2) one commit ahead of `engine.clj` reading it (AR-3) is
  therefore not a safe intermediate state to push to `origin/main`.
  Combined into one commit (`74be432`) with both AR-2 and AR-3's own
  attribution kept explicit in the message and in `notes/ADRs.md`
  ADR-0033's own execution note, rather than silently dropping one of
  the two prompt-specified commit messages.
- **The UTI round-trip test's own seed changed from 20260802 to 777,
  not named or anticipated by the prompt.** A real `check/check-all`
  invariant violation, traced to a documented, already-existing v1
  scope boundary (`:pre-horizon-facts` not feeding the engine's
  patient-state fold when an Encounter straddles the fixed
  registration-t anchor) that this closure's own long self-looping
  Delay makes common to trip — confirmed across 10 sampled seeds (8
  tripped it), not a fluke of the pin's own seed, and not a defect this
  session's own AR-1..AR-5 scope is meant to fix. Full mechanism and the
  seed search are in `notes/ADRs.md` ADR-0033's own execution note and
  this session's own record.
- **AR-4's oracle bracket ran through a hand-written per-worktree
  script, not `bin/regression-oracle` itself, unmodified.** That
  script's own design (always reading `digest.clj` from the current
  checkout, ADR-0030 J1/J2) assumes the test code stays source-
  compatible across the two component-code versions under comparison —
  an assumption ADR-0033's own hard `:modules` shape switch falsifies
  for the producer functions this session touched (`gmf/singleton-
  closure` doesn't exist at the pre-ADR-0033 baseline — confirmed live,
  a compile error, not a digest difference). Verified the SAME identity
  claim by running each commit's own `digest.clj` against its own
  worktree/classpath instead. Both digest tables (this workaround's
  six-root identity table, and Step 5's three-root first-baseline
  table) are in `notes/ADRs.md` ADR-0033's own execution note and this
  session's own record, verbatim.

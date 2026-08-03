# 2026-08-03 — Procedure-duration fix session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). Found, at Step 0: the initial
Windows-path reads (`C:\Users\prags\Documents\ehr-testing-tools`) had
landed on the `/mnt/c` clone, which was FIVE commits behind `origin/
main` (`71093d5` vs. `dc7b371`) — the post-Wave-D cleanup session's own
J4 dual-clone guardrails (`/mnt/c` read-only, reject-all hooks) fired
exactly as designed when a stray `git fetch` there returned `Permission
denied`. Re-read every "Read first" file against the ext4 clone
(confirmed at `origin/main`'s own HEAD, `dc7b371`, clean tree) before
Step 1; `gmf.clj` and `gmf-interpreter.md` had each changed since the
stale `/mnt/c` read (the AR-5(b)/AR-5(c) wellness dated notes, ADR-0031)
but neither touched the Procedure-duration call site this session
fixes.

## Prompt, verbatim

> 2026-08-03 — Build session: Procedure-duration fix (D3c finding 1)
>
> Context
>
> `resolve-time-advance` destructures nested `:range`/`:exact` keys from a Procedure's `:duration`, but post-loader every vendored Procedure duration is a FLAT `{:low :high :unit}` map — so no duration-bearing Procedure has ever advanced virtual time (found live, Wave D stage D3c finding 1; roadmap `Next` row per ADR-0031 AR-6). This session fixes it, oracle-bracketed. The design channel has already done the H1-discipline semantics pin from Synthea source at the interpreter doc's own pin (`7e08387c68a7f0e21d13076609a159fd473fc902`) — recorded as author rulings below, cite them rather than re-deriving. This is AR-6's FIRST defect-fix session; the engine closure-context fix is a SEPARATE later session — do not touch `engine.clj`, the three J3 pinned round-trip tests, or anything closure-registration-shaped.
>
> Read first
>
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md` (preflight + VERIFICATION sections)
> 2. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj` — `resolve-time-advance` and the `trajectory-and-advance` call site (`(if-let [duration (:duration state)] ...)`)
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` — `apply-gmf-v2-procedure-duration` and its docstring (the "bug and all" clause this session retires)
> 4. `components/sim-trajectory/docs/gmf-interpreter.md` — §14's D3c finding 1 dated note
> 5. `notes/ADRs.md` — ADR-0029 (D3c), ADR-0030 (J1 oracle discipline), ADR-0031 (AR-6 sequencing); next ADR number expected 0032
> 6. `bin/regression-oracle` (usage; you will run it twice)
> 7. `.agents/plans/roadmap.md` — this session's `Next` row and the Deferred provenance row it points at
>
> Author rulings (design channel, 2026-08-03; record in ADR-0032)
>
> * AR-1 (semantics pin, fetched-source, pin `7e08387c...f902`). Upstream, `State.Procedure extends Delayable`: `processOnce` sets `stop = procedure.start + Utilities.convertTime(unit, person.rand(low, high))` — ONE uniform draw — and `endOfDelay` returns that stop, so the module clock BLOCKS until the procedure ends. Procedure duration genuinely advances the walk upstream. The sim's documented intent (docstring of `resolve-time-advance`) matches in kind; the defect is shape only. Cite `src/main/java/org/mitre/synthea/engine/State.java` (Procedure's `processOnce`/`endOfDelay`, ~lines 1744–1830) in the ADR.
> * AR-2 (shape ruling). The flat `{:low :high :unit}` map IS Procedure's canonical duration shape — it is upstream GMF 1.0's own JSON encoding (`RangeWithUnit<Long> duration`), and the loader's v2 translation already targets it. Therefore the fix is at the CALL SITE, not in `resolve-time-advance` and not in the loader: `trajectory-and-advance` wraps the flat map as `{:range duration}` before calling `resolve-time-advance`. `resolve-time-advance`'s own contract, Delay's nested shape, Death's Wave-C usage, and the loader schema all stay untouched.
> * AR-3 (draw law). The fix introduces exactly one uniform integer draw per duration-bearing Procedure execution, via the existing `rand-int-in` — the established fixed-consumption law. Degenerate ranges (`sepsis`'s `{:low 30 :high 30}`) still consume one draw (`.nextInt 1`), deterministically returning `:low` — uniform consumption, no special-casing.
> * AR-4 (expected blast radius — the oracle must prove BOTH halves). Duration-bearing Procedures exist in exactly three vendored roots: `appendicitis` (Appendectomy), `sepsis` (7 states), and the UTI closure (~30 states, all via the v2 normalization). After the fix: those three roots' digests CHANGE (disclosed, re-baselined); `sinusitis`, `sore_throat`, `ear_infections`, the death fixture, and `total_joint_replacement` MUST be byte-identical — the fix adds draws only inside walks that reach a duration-bearing Procedure. A digest change in any of the five identity roots is a STOP-AND-ESCALATE (the fix did something the ruling says it cannot), not a re-baseline.
> * AR-5 (test posture). Co-landing: the fix lands with its invariant — at minimum a focused test proving a duration-bearing Procedure advances virtual time by an amount inside `[low, high]` of its unit (use `appendicitis`'s Appendectomy or a minimal inline module). Existing tests that FAIL because they encoded the zero-advance behavior (timestamps, event times, counts downstream of timing) are updated WITH a dated disclosure naming this session; any failure NOT attributable to timing now advancing is an escalation. The three J3 round-trip pins must still pass untouched (they assert engine-layer gaps this session does not enter).
>
> Steps
>
> Step 0 — Preflight. Build-session preflight (ext4 target resolution, `/mnt/c` untouched). `git fetch`; note origin tip. Run `bin/regression-oracle` for the PRE run: current tip vs itself is vacuous — instead record the tip sha; the POST run in Step 3 compares tip-before-fix → fix commit.
>
> Step 1 — The fix + its invariant (one commit).
>
> 1. `trajectory-and-advance`: wrap the flat duration — `(resolve-time-advance rng (:t ctx) {:range duration})` — with a short comment citing ADR-0032 AR-2.
> 2. The AR-5 focused test (advance lands in `[low, high]`, one draw consumed — assert draw consumption if the harness makes that cheap; otherwise the in-range assertion suffices).
> 3. Retire the "bug and all" clause in `apply-gmf-v2-procedure-duration`'s docstring with a dated note (fix-forward: the clause stays, annotated FIXED with ADR-0032 cite — do not rewrite the docstring's history).
> 4. Run the FULL suite. Apply AR-5's failure triage. Timing-encoding test updates go in this same commit with their disclosure. Commit: `fix(sim-trajectory): Procedure :duration advances virtual time (D3c finding 1, ADR-0032)`
>
> Step 2 — ADR-0032. Append: context (D3c finding 1, ADR-0031 AR-6 sequencing), AR-1 through AR-5 verbatim (attributed, design channel 2026-08-03), and an execution-note stub to fill in Step 4. Commit: `docs: ADR-0032 -- Procedure-duration fix rulings and semantics pin`
>
> Step 3 — Oracle bracket (the load-bearing step). Run `bin/regression-oracle` across `<tip-before-fix> -> <Step 1 commit>`. Required outcome per AR-4: the five identity roots byte-identical; `appendicitis`/`sepsis`/UTI changed. Record the full digest table in the session record. Any deviation from AR-4's partition: STOP-AND-ESCALATE with the table, no fixes. (No commit — evidence only, lands in Step 4's records.)
>
> Step 4 — Records + roadmap. Fill ADR-0032's execution note (digest table summary, both halves). Dated notes: `gmf-interpreter.md` §14 D3c finding 1 → FIXED (ADR-0032, commit sha); roadmap — move this session's `Next` row to Done, annotate the Deferred provenance row FIXED. Session record to `.agents/session-records/2026-08-03-procedure-duration-fix.md` (include the digest table verbatim); self-archive this prompt. Reading-set budget check per pattern. Commit: `docs: procedure-duration fix records -- oracle table, D3c closed (archives prompt)`
>
> Fences
>
> * `engine.clj`, the three J3 round-trip tests, closure registration: untouched. That is the NEXT session.
> * No loader/schema changes; the fix is the call-site wrap plus tests and docs.
> * AR-4's identity half is as load-bearing as its change half — an unexpected identity break is an escalation even if the suite is green.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation-record appendix (per the prompt's own Fences clause)

- **AR-2's own working name for the fix's call site,
  "`trajectory-and-advance`," names no function in the live tree.** The
  actual call site — the `(if-let [duration (:duration state)] ...)`
  binding the "Read first" section points at — lives in
  `emit-and-advance`, the shared helper every v1 trajectory-event-
  producing state type's own `:duration` handling resolves through.
  Corrected inline in ADR-0032's own AR-2 text rather than silently
  using the wrong name or silently substituting the right one without
  saying so.
- **AR-4's own STOP-AND-ESCALATE clause fired for real, at Step 3.**
  `death-fixture` — one of the five roots AR-4 named as "must stay
  byte-identical" — changed. Per the prompt's own Step 3 instruction
  ("Any deviation from AR-4's partition: STOP-AND-ESCALATE with the
  table, no fixes"), this session stopped rather than reclassifying the
  root itself. Root cause, found reading `death-fixture.json` directly:
  its `Stabilization_Procedure` state carries a genuine duration-bearing
  `:duration` (`{:low 30 :high 30 :unit "minutes"}`) that AR-4's own
  three-root survey never enumerated (a hand-authored fixture, outside
  the "vendored roots" framing the survey was scoped to) — the fix
  behaved exactly as AR-2/AR-3 specify. Put to the author via
  `AskUserQuestion` before proceeding; ruled to (1) correct AR-4 with a
  dated note reclassifying `death-fixture` into the duration-bearing set
  and accept its new digest as baseline, and (2) disclose only (not fix
  under this session) that `bin/oracle-src/ehrt/oracle/digest.clj` has
  never covered `total_joint_replacement`/the UTI closure at all — a
  pre-existing six-root scope from the post-Wave-D cleanup session, not
  introduced by this one. Both dated notes land on ADR-0032's own AR-4;
  full account in the session record
  (`.agents/session-records/2026-08-03-procedure-duration-fix.md`).

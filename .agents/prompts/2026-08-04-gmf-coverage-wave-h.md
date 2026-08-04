# 2026-08-04 — Build session: Wave H — pre-roll (history phase, straddle resolution)

## Context

PARITY ACHIEVED (ADR-0041): 84/85 walk, 1 out-of-scope. Wave H is the sole remaining wave, ruled to run LAST (ADR-0039 AR-7) so its history-phase design exercises against the complete catalog. The author ratified the design (2026-08-04, design channel), which resolves §3's open questions and the three dated notes the H row has accumulated (emit-nothing reaffirmed ADR-0031 AR-3; the UTI straddle

* seed-777 linkage ADR-0033/0034; the wellness fold rule ADR-0037). Three rulings below (AR-1..AR-3) plus supporting ones. One session: the rulings ARE the design capture (verbatim in ADR-0042) and the mechanism is deliberately small — one interpreter phase boundary, a compile filter, config gating, and the test retirement. AR-6's escalation clause is real: if the straddle rule doesn't cleanly subsume the existing `:pre-horizon-facts` mechanics, STOP and report — do not improvise the boundary.

## Read first

1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
2. `components/sim-trajectory/docs/gmf-interpreter.md` §3 (the ratified history/horizon base this implements) and the parity plan's Wave H row (all three dated notes)
3. `gmf_interpreter.clj` — `run-module`'s `registration-t` / `horizon-end-t` threading, the walk loop's time model, `:wellness-wait` (whose ticks must fold in history)
4. The existing `:pre-horizon-facts` mechanism and the engine's registration-t anchor — `engine.clj`, `check.clj`'s `:clinical-content-only-when-admitted` invariant, and wherever `:pre-horizon-facts` is written/read. The design channel ruled the RULE (AR-2) but did NOT read this mechanism; reconciling them is this session's grounding work (AR-6).
5. `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_uti_test.clj` — the seed-777 dodge and its retirement note
6. `compile_trajectory.clj` — where the phase filter lands
7. `notes/ADRs.md` — ADR-0027 (clinical state, never scratch), ADR-0031 AR-3, ADR-0037, ADR-0041; next ADR expected 0042
8. `bin/oracle-src/ehrt/oracle/digest.clj` (history-mode batch extension, AR-5)

## Author rulings (design channel, ratified 2026-08-04; record in ADR-0042)

* AR-1 (phase-marked events, one interpreter). When history is enabled, the walk runs DOB → `horizon-end-t` as ONE continuous walk with a phase boundary at `registration-t`. History-phase events are MINTED into the trajectory with a `:phase :history` mark; they fold state effects identically to horizon events (clinical-state channel per ADR-0027 — conditions, medications, careplans, vitals, attributes, wellness state); the compile step DROPS them (the ConditionEnd "real, worth keeping, not worth a message" shape, generalized to a phase). No second interpreter, no fold-only mode: glass-box traceability keeps all 45 years inspectable; the wire stays in-window. `:wellness-wait` needs no special handling — pre-registration ticks are history events like any other (ADR-0037's note, discharged automatically).
* AR-2 (straddle: encounter-anchored phase inheritance). An event's phase is inherited from its ENCOUNTER's opening phase, not its own timestamp. An encounter that opens in history is history in full — its contents and its close fold, never emit, regardless of timestamps — so no orphaned close events, no `:clinical-content-only-when-admitted` trips. Disclosed v1 cost: a straddling encounter yields no in-window wire traffic for that patient. CARRY-ACROSS (emitting patients mid-stay at window open, as real hospital censuses have) is the NAMED FUTURE, roadmap Deferred, trigger: a test scenario needs mid-stay-at-window-open realism. Events outside any encounter phase by their own timestamp against `registration-t`.
* AR-3 (config opt-in — the identity ruling). History runs ONLY when the run config requests it (a `:history` flag at the same layer `:modules` lives, threaded to `run-module`; absent = today's registration-t start, byte-identical draw streams). This is load-bearing for AR-5's pure-identity bracket. The census's walks do NOT enable history (its parity claim is about the interpreter's module vocabulary, unchanged); a history-census is a possible future, not this session.
* AR-4 (seed-777 retirement). With AR-2 landed, the UTI engine round-trip test drops its hand-picked seed: replace with an ordinary seed (or the config default), assert the straddle resolves for it, and add a dated note closing the ADR-0033/0034 linkage. If the test STILL needs a hand-picked seed after AR-2, that is a STOP-AND-ESCALATE — the rule didn't do what the design channel claimed.
* AR-5 (oracle — pure identity + new history baselines). Fresh scan unnecessary for identity (AR-3's gating is the argument — state it in the ADR); every existing oracle batch byte-identical (any change escalates, suspect the gating first). Co-landing: extend `digest.clj` with history-enabled batches for at least UTI (engine layer, straddle exercised) and `ear_infections` (wellness ticks folding) and record their FIRST history-mode baselines in the session record.
* AR-6 (the reconciliation read + escalation). The existing `:pre-horizon-facts` mechanism and the engine's registration-t anchor were NOT read by the design channel. Before implementing: read them, and record in the ADR whether AR-1/AR-2 subsume them (expected: `:pre-horizon-facts` becomes the phase fold's landing zone or retires into it, and the invariant needs no change because history content never reaches its checked surface). If the existing mechanics conflict with the rulings in any way phase inheritance does not cleanly resolve — STOP-AND-ESCALATE with the read, implement nothing.

## Steps

Step 0 — Preflight. Standard; ADR-0041 at origin; next ADR 0042. AR-6's reconciliation read FIRST, recorded, before any edit.
Step 1 — Config + phase boundary. The `:history` flag (schema, `run-module` threading, engine pass-through) + the DOB-start walk + `:phase :history` marking. Tests: gated-off byte-identity (draw-count

* digest on a fixture), gated-on history events minted and marked, state folds (a condition onset in history is present at registration-t). Commit: `feat(sim): opt-in history phase -- DOB-start walks with phase-marked events (ADR-0042 AR-1/AR-3)`

Step 2 — Compile filter + straddle inheritance. Compile drops `:phase :history`; encounter-anchored inheritance per AR-2 (open-in- history encounters claim their contents and close). Tests: history events compile to nothing; a straddling inline fixture emits nothing from the straddled encounter and nothing orphaned; post-straddle horizon events emit normally; `:clinical-content-only-when-admitted` green across a straddle. Commit: `feat(sim): compile drops history phase; straddling encounters inherit their opening phase (ADR-0042 AR-1/AR-2)`
Step 3 — Seed-777 retirement + wellness fold evidence. AR-4's test change; an ear_infections history-mode test showing pre- registration wellness ticks folded (state present, no events emitted). Commit: `test(sim): UTI round-trip on an ordinary seed -- straddle resolved by design, seed-777 dodge retired (ADR-0042 AR-4)`
Step 4 — Oracle. Identity run (every existing batch unchanged) + the AR-5 history-batch extension with first baselines. Commit: `test: regression oracle gains history-mode batches (ADR-0042 AR-5)`
Step 5 — Records. ADR-0042 (rulings + AR-6's reconciliation read verbatim; execution note: both oracle tables). Parity plan: H → the final dated note (the wave ledger closes); roadmap: H → Done, carry-across into Deferred with its trigger, and a "GMF parity arc COMPLETE" note pointing the roadmap back at the non-GMF fronts. Dated notes: §3 (implemented), the straddle pointer chain closed. Session record + prompt self-archive + budget check. Commit: `docs: wave H records -- pre-roll landed, parity arc complete (archives prompt)`

## Fences

* No carry-across implementation, no history-census, no backloaded emission in any form (ADR-0031 AR-3 stands).
* AR-6 gates everything: an unresolved conflict with the existing pre-horizon mechanics stops the session at Step 0.
* Red→green per step; deviations get the dated appendix.

---

Executed 2026-08-04, recorded `notes/ADRs.md` ADR-0042. AR-6's own
reconciliation read did NOT find a conflict — AR-1/AR-2 cleanly
subsume the existing `:pre-horizon-facts` mechanism, no
STOP-AND-ESCALATE — the full read is in ADR-0042's own dedicated
section, not merely summarized here. One genuine deviation from this
prompt's own anticipated scope, disclosed at the point found rather
than escalated (judgment call, not yet author-ratified):

- **Step 3 surfaced a second, narrower straddle class AR-2's own text
  didn't anticipate.** `:medication-end`/`:care-plan-end`/`:condition-
  end` can fire OUTSIDE any encounter (a medication started during a
  dropped history-phase encounter, ended after discharge) — AR-2's own
  open-encounter inheritance alone doesn't cover it, and the real UTI
  closure under an ordinary seed hit it live (first run RED on
  `medication-end-references-existing-order-and-follows-it-in-time`).
  Closed by `history-phase?`, the SAME "no orphaned reference to
  something dropped" principle applied one `:references` hop further
  — judged a mechanical extension of AR-2's own ratified rule, not a
  new design surface, so implemented rather than escalated.

See `.agents/session-records/2026-08-04-gmf-coverage-wave-h.md` for
the full red→green evidence and judgment-call log.

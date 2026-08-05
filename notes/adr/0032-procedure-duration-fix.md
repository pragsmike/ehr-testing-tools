<!-- Attic file: notes/adr/0032-procedure-duration-fix.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0032 — Procedure-duration fix: rulings and semantics pin (D3c finding 1, ADR-0031 AR-6 first defect-fix)

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-5 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
same day.

### Context

`ehrt.sim-trajectory.gmf-interpreter/resolve-time-advance` destructures
nested `:range`/`:exact` keys from its own 4th argument, but a
Procedure state's `:duration` field is (post-loader, v1 and v2
`gmf_version` alike) a FLAT `{:low :high :unit}` map — so no
duration-bearing Procedure has ever advanced virtual time (found live,
Wave D stage D3, `docs/gmf-interpreter.md` section 14's own D3c finding
1; named, unowned, in the roadmap's own Deferred section until ADR-0031
AR-6 sequenced it as the FIRST of two defect-fix sessions ahead of the
census). This ADR records the design channel's own rulings, ruled
before the fix commit per this project's own H1 characterize-before-
build discipline (the D5/C1 precedent), and the fix's execution note.

### Decision

Ruled 2026-08-03, design channel, recorded verbatim:

**AR-1 (semantics pin, fetched-source, pin
`7e08387c68a7f0e21d13076609a159fd473fc902`).** Upstream,
`State.Procedure extends Delayable`: `processOnce` sets `stop =
procedure.start + Utilities.convertTime(unit, person.rand(low, high))`
— ONE uniform draw — and `endOfDelay` returns that stop, so the module
clock BLOCKS until the procedure ends. Procedure duration genuinely
advances the walk upstream. The sim's documented intent (docstring of
`resolve-time-advance`) matches in kind; the defect is shape only. Cite
`src/main/java/org/mitre/synthea/engine/State.java` (Procedure's
`processOnce`/`endOfDelay`, ~lines 1744–1830).

**AR-2 (shape ruling).** The flat `{:low :high :unit}` map IS
Procedure's canonical duration shape — it is upstream GMF 1.0's own
JSON encoding (`RangeWithUnit<Long> duration`), and the loader's v2
translation already targets it. Therefore the fix is at the CALL SITE,
not in `resolve-time-advance` and not in the loader: the call site
wraps the flat map as `{:range duration}` before calling
`resolve-time-advance`. `resolve-time-advance`'s own contract, Delay's
nested shape, Death's Wave-C usage, and the loader schema all stay
untouched. **Execution note (naming correction):** the ruling's own
working name for this call site, "`trajectory-and-advance`," names no
function in the live tree — the actual call site is
`ehrt.sim-trajectory.gmf-interpreter/emit-and-advance`
(gmf-interpreter.clj), the shared helper every v1 trajectory-event-
producing state type's own `:duration` (Procedure's only real user)
resolves through; corrected here rather than propagated.

**AR-3 (draw law).** The fix introduces exactly one uniform integer
draw per duration-bearing Procedure execution, via the existing
`rand-int-in` — the established fixed-consumption law. Degenerate
ranges (`sepsis`'s `{:low 30 :high 30}`) still consume one draw
(`.nextInt 1`), deterministically returning `:low` — uniform
consumption, no special-casing.

**AR-4 (expected blast radius — the oracle must prove BOTH halves).**
Duration-bearing Procedures exist in exactly three vendored roots:
`appendicitis` (Appendectomy), `sepsis` (7 states), and the UTI closure
(~30 states, all via the v2 normalization). After the fix: those three
roots' digests CHANGE (disclosed, re-baselined); `sinusitis`,
`sore_throat`, `ear_infections`, the death fixture, and
`total_joint_replacement` MUST be byte-identical — the fix adds draws
only inside walks that reach a duration-bearing Procedure. A digest
change in any of the five identity roots is a STOP-AND-ESCALATE (the
fix did something the ruling says it cannot), not a re-baseline.

> **Dated correction (2026-08-03, author-ruled at Step 3's own
> STOP-AND-ESCALATE): AR-4's own survey was incomplete, not the fix.**
> Running `bin/regression-oracle` (`dc7b371` -> `1ea1f4a`) found
> `death-fixture`'s own digest changed — triggering this paragraph's
> own escalation clause literally, since `death-fixture` is named
> above as a "must stay identical" root. Reading `death-fixture.json`
> directly: its `Stabilization_Procedure` state carries `"duration":
> {"low": 30, "high": 30, "unit": "minutes"}` — a real duration-bearing
> Procedure this ruling's own survey (three roots named above) never
> enumerated (a hand-authored fixture, not a vendored module, so it sat
> outside the "vendored roots" framing AR-4 was scoped to). The fix did
> exactly what AR-2/AR-3 specify — advance time for ANY Procedure
> carrying `:duration` — so this is a corrected census, not a fix
> defect: **`death-fixture` moves from the five-root identity set into
> the duration-bearing set (now four: `appendicitis`, `sepsis`,
> `death-fixture`, the UTI closure)**; the identity set shrinks to
> `sinusitis`/`sore_throat`/`ear_infections`/`total_joint_replacement`.
> `death-fixture`'s new digest is accepted as its baseline going
> forward — author-ruled, not silently re-baselined by this session on
> its own initiative. Separately, ALSO found at Step 3: `bin/oracle-src/
> ehrt/oracle/digest.clj` (the post-Wave-D cleanup session's own J1
> equipment) hardcodes exactly six roots and has never covered
> `total_joint_replacement` or the UTI closure at all — a pre-existing
> gap, not introduced by this session, but it means AR-4's own "both
> halves" byte-digest claim is UNVERIFIABLE for those two roots with
> current tooling. Author-ruled: disclose only this session (see the
> execution note and session record for the corroborating,
> non-oracle evidence used instead); extending `digest.clj` to cover
> `total_joint_replacement`/UTI is named, not built, its own small
> follow-up.
>
> **Closure pointer (2026-08-03, ADR-0033 AR-4b): this disclosure is
> CLOSED.** `digest.clj` now covers `total_joint_replacement` and the
> UTI closure at the engine layer (`total-joint-replacement-engine`/
> `urinary-tract-infections-engine`, plus `ear-infections-engine` for
> the third closure root) — first baselines recorded in ADR-0033's own
> execution note, since the round trip never completed before that
> session (there was no prior digest to compare against, only a gap to
> fill).

**AR-5 (test posture).** Co-landing: the fix lands with its invariant
— at minimum a focused test proving a duration-bearing Procedure
advances virtual time by an amount inside `[low, high]` of its unit
(use `appendicitis`'s Appendectomy or a minimal inline module).
Existing tests that FAIL because they encoded the zero-advance
behavior (timestamps, event times, counts downstream of timing) are
updated WITH a dated disclosure naming this session; any failure NOT
attributable to timing now advancing is an escalation. The three J3
round-trip pins must still pass untouched (they assert engine-layer
gaps this session does not enter).

> **Execution note (filled Step 4, 2026-08-03).** `emit-and-advance`
> (gmf-interpreter.clj) now wraps a Procedure's flat `:duration` as
> `{:range duration}` before calling `resolve-time-advance` (AR-2). The
> focused test (`gmf_interpreter_test.clj`,
> `procedure-duration-advances-virtual-time-within-its-range` +
> `procedure-duration-consumes-a-fixed-single-rng-draw`) was proven RED
> (0 advance, 0 draws) against the pre-fix tree, then GREEN after —
> AR-5's own invariant, red→green, not merely asserted. Full
> non-integration suite (`clojure -M:poly test :all skip:integration`)
> stayed 0 failures/0 errors throughout — no existing test encoded the
> zero-advance behavior, so AR-5's own test-triage clause needed no
> action; the three J3 round-trip pins were not touched (this session
> never entered `engine.clj`).
>
> `bin/regression-oracle dc7b371 1ea1f4a` (tip before this session's
> fix commit -> the fix commit) — the six roots `digest.clj` actually
> covers:
>
> | root | changed? | duration-bearing Procedure reached? |
> |---|---|---|
> | `appendicitis` | YES | yes (Appendectomy) |
> | `sepsis` | YES | yes (7 states) |
> | `death-fixture` | YES | yes (`Stabilization_Procedure`, found this session — AR-4's dated correction, above) |
> | `ear-infections` | no | no |
> | `sinusitis` | no | no |
> | `sore-throat` | no | no |
>
> Full digest table in `.agents/session-records/
> 2026-08-03-procedure-duration-fix.md`. Every changed root has a
> duration-bearing Procedure on the batch/pair `digest.clj` exercises;
> every unchanged root does not — the partition AR-4 predicted, once
> corrected for the fixture AR-4's own survey missed. `total_joint_
> replacement` and the UTI closure are NOT in `digest.clj` (a
> pre-existing six-root scope from the post-Wave-D cleanup session,
> not this session's own gap) — the corroborating evidence for those
> two instead: (1) `clojure -M:poly test :all skip:integration` stayed
> 0 failures/0 errors both before and after the fix, including
> `vendored-uti-test`/`vendored-tjr-test`'s own interpreter-layer walks
> and the three J3 round-trip pins (untouched, still failing the same
> documented way — this session never entered `engine.clj`); (2) TJR's
> own vendored closure was read directly and confirmed to contain no
> Procedure state with a `:duration` field, so it has no mechanism to
> be affected regardless of oracle coverage. This is disclosure, not a
> byte-digest oracle claim for those two roots — the build-session
> skill's own VERIFICATION section names exactly this distinction.

### Fence

This session fixes exactly the call site AR-2 names, plus its own
invariant test and this ADR. `engine.clj`, the three J3 pinned
round-trip tests, and anything closure-registration-shaped are
untouched — that is ADR-0031 AR-6's SECOND defect-fix session, not
this one. No loader/schema change.

---


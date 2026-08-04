# 2026-08-04 — Build session: Wave I2 — the last two (parity closer)

## Context

Post-I census: 82/85 walking, 1 out-of-scope, 2 blocked — both unmasked findings ADR-0040 recorded for the design channel: `congestive-heart-failure` needs Death's `condition_onset` / `referenced_by_attribute` cause-of-death forms (the sim's OWN Wave C disclosure firing — the error message cites `docs/gmf-interpreter.md` §10), and `wellness-encounters` needs the `:active-careplan` condition. Both pinned at `7e08387c68a7f0e21d13076609a159fd473fc902` (`State.java` `Death` ~2488–2530; `Logic.java` `ActiveCarePlan` ~612–632 with its `ActiveLogic` parent). Two mechanical, additive ports; if both classify clean, the parity plan §1 definition is MET and Wave I's deferred declaration (ADR-0040 AR-7's condition) lands here.

## Read first

1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
2. `gmf_interpreter.clj` — `death-step` and its cause handling (the §10 disclosure this session resolves), `ConditionEnd`'s reference-prior-event-by-state-name lookup (the shape Death's `condition_onset` form reuses), the careplan span state (Wave C) and the ActiveLogic-family conditions already present (locate how `Active Condition`-style checks dispatch, if present)
3. `docs/gmf-interpreter.md` §10 (the Death forms disclosure — gets its resolution note)
4. Synthea at the pin: `State.java` `Death.process` (~2488–2530 — note `hadPriorState` guarding and null-reason tolerance), `Logic.java` `ActiveCarePlan` + `ActiveLogic` (~560–632 — READ the parent: codes vs `referenced_by_attribute` dispatch is defined there, the design channel read only the subclass)
5. `notes/ADRs.md` — ADR-0040 (the two findings + the deferred parity condition); next ADR expected 0041
6. Post-I census artifact header

## Author rulings (design channel, 2026-08-04; record in ADR-0041)

* AR-1 (Death cause forms). Additive to `death-step`: `condition_onset` resolves the named ConditionOnset state's condition code via the existing prior-event lookup (absent prior state → cause stays nil, per upstream's `hadPriorState` guard — tolerated, NOT an error); `referenced_by_attribute` reads the attribute's condition entry for its code (absent attribute → nil cause, same tolerance). The existing `codes` form and range/exact timing are untouched. §10's disclosure gets a dated resolution note.
* AR-2 (:active-careplan condition). Per `ActiveLogic`: `codes` form → is a careplan with that code active in the careplan span; `referenced_by_attribute` form → is the attribute's careplan entry active. No active careplan → FALSE (the natural answer, not honest-absence — activity is what the condition tests, the AR-0040 AR-3 distinction applies verbatim). Read the parent class for any dispatch subtlety the subclass hides; record the read in the ADR.
* AR-3 (oracle — pure identity, additive-only discipline). Fresh recursive vendored-root scan for both mechanisms (the death fixture uses the `codes` form only, per the sim's own disclosure — verify, don't inherit). Implementation must be additive: existing Death and condition paths byte-identical. Every oracle batch identical; any change STOP-AND-ESCALATE.
* AR-4 (census + the parity declaration). Re-run (same params, disambiguated filename). IF the result is 84/85 `:ok-walked` + 1 `:out-of-scope-by-ruling` and zero blocked: land the dated PARITY ACHIEVED notes (parity plan §1 with the artifact named; roadmap; the interpreter doc's census section) — the countable definition from ADR-0031 AR-4, met. Any other result: no declaration, record what remains, the design channel reads it. Either way Wave H (last, per the VS-session re-ordering) becomes the sole remaining wave, and the roadmap says so.

## Steps

Step 0 — Preflight. Standard; ADR-0040 at origin; next ADR 0041; pin checkout present; AR-3 fresh scans recorded before edits.
Step 1 — Death cause forms. Loader schema (two new optional fields) + `death-step` resolution per AR-1 + tests (each form, absent-reference tolerance, existing-forms regression). Red→green. Commit: `feat(sim-trajectory): Death condition-onset and referenced-by-attribute cause forms (ADR-0041 AR-1)`
Step 2 — :active-careplan. Condition dispatch + both forms + false-on-inactive tests + the parent-class read recorded. Red→green. Commit: `feat(sim-trajectory): :active-careplan condition reads the careplan span (ADR-0041 AR-2)`
Step 3 — Oracle bracket. Pure identity per AR-3; record; escalate on any change.
Step 4 — Census + parity determination. Per AR-4. Commit: `docs(sim-trajectory): census after Wave I2 (ADR-0041)`
Step 5 — Records. ADR-0041 (rulings + reads; execution note: oracle table, classification, the determination). The declaration notes IF earned. Roadmap: I2 → Done; H is the sole remaining wave. Session record + prompt self-archive + budget check. Commit: `docs: wave I2 records (archives prompt)`

## Fences

* No Wave H mechanics; no census-tool fixes; nothing beyond the two mechanisms.
* The declaration is conditional — do not write PARITY ACHIEVED over anything other than AR-4's exact clean result.
* Red→green per step; deviations get the dated appendix.

---

Executed 2026-08-04, recorded `notes/ADRs.md` ADR-0041. Two deviations
from this prompt, both disclosed at the point found — factual
corrections against the live pin, not design decisions, so neither
was escalated:

- **AR-1's own framing of the absence-tolerance rule (attributed to
  "upstream's own `hadPriorState` guard") did not hold against the
  real source when re-read.** `State.java`'s own `Death.process`
  checks `:codes` FIRST, not last (this prompt's own §10 citation, and
  the doc's own C1 account, both had the priority order backwards);
  `:referenced-by-attribute`'s absence-tolerance is a genuine,
  disclosed DEPARTURE from upstream (which throws), not a
  `hadPriorState`-style guard at all. Implemented per the corrected
  reading, disclosed in ADR-0041 AR-1 directly.
- **AR-1's own "referenced-by-attribute reads the attribute's condition
  entry" phrase presupposes something writes that attribute — nothing
  did, found live.** `congestive_heart_failure.json`'s own `CHF
  Condition Start` authors `assign_to_attribute: "chf"`, but
  `:condition-onset`'s own `step` case never wired it. Ported (mirrors
  `:medication-order`'s own case verbatim) — without it, the mechanism
  this session exists to build would never fire on its own real,
  mandatory target path.
- Steps 1+2 landed as ONE commit rather than the two named above —
  disclosed in that commit's own message, the same shared-file-region
  shape ADR-0040 AR-5 already took.

See `.agents/session-records/2026-08-04-gmf-coverage-wave-i2.md` for
the full red→green evidence and judgment-call log.

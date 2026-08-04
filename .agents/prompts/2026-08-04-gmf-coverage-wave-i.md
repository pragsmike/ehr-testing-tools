# 2026-08-04 — Build session: Wave I — the tail (full parity)

## Context

Post-VS census: 75/85 walk, 1 out-of-scope, 9 blocked across six small,
fully-pinned mechanisms. This is the CLOSING unlock wave: if the
classification goes as ruled, the parity plan §1's countable
definition is MET (84/85 walking + `gallstones` out-of-scope by
ruling) and only Wave H (architectural, ruled last) remains. All
semantics pinned at `7e08387c68a7f0e21d13076609a159fd473fc902`; the two
record-writing states the design channel did not fully read are named
session reads (AR-5). Six mechanisms in one session follows the Wave F
precedent — each is mechanical, pinned, and lands step-per-mechanism
with red→green per step.

## Read first

1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
2. `gmf.clj` — the F0 `set-attribute-value-conflict` rejection (this
   session retires it for a precedence chain), the schema's transition
   distributions, the state-type map
3. `gmf_interpreter.clj` — the Observation-condition implementation
   (its absence error changes), distributed/complex transition
   resolution (NamedDistribution lands there), the LC attribute-
   resolution helpers (reused by AR-1)
4. `sim-trajectory/vital-signs.edn` (or wherever the observation
   vital-name vocabulary lives — the census error names the file)
5. Synthea at the pin: `Transition.java` `NamedDistribution`
   (~748–770) and its use in distributed/complex transitions;
   `State.java` `SetAttribute.process` (~797–830, the precedence
   chain); `Logic.java` `Observation.test` (~237–290, incl. the
   issue-774 band-aid); `State.java` `AllergyOnset` (~1373, extends
   `OnsetState`) and `Vaccine` (~2555) — AR-5's session reads
6. `notes/ADRs.md` — ADR-0036/0038/0039 precedents; next ADR expected
   0040
7. Post-VS census artifact header
   (`2026-08-04-...-wave-vs.edn`)

## Author rulings (design channel, 2026-08-04; record in ADR-0040)

* AR-1 (NamedDistribution — 4 modules). A transition distribution
  given as `{"attribute": name, "default": d}` fetches the probability
  from the named attribute at transition time, falling back to
  `default` when absent — upstream defines absence, so NO honest-
  absence error here. Resolution reuses LC's order (module attributes
  first, then persona mapping). Schema accepts the map form wherever
  numeric distributions are accepted; the fetched value must be
  numeric (non-numeric attribute value → a recorded walk error naming
  attribute and value). Draw behavior unchanged (the pick is still one
  distributed-transition draw).
* AR-2 (SetAttribute precedence — CHF). Retire the F0
  `set-attribute-value-conflict` rejection: upstream's `process`
  defines an explicit precedence chain — expression > range >
  seriesData > distribution > valueCode > valueAttribute > literal
  value — so co-present sources are legal and ordered, not
  conflicting. Implement the chain for the sources the sim supports
  (distribution, valueCode, valueAttribute, literal value, range —
  range is one draw via `person.rand(low, high)` semantics, port it);
  `expression` and `seriesData` remain clean load rejections naming
  the feature (the existing expression pattern). Dated note on F0's
  AR-4: the mutual-exclusivity reading was stricter than upstream;
  corrected from the pinned chain.
* AR-3 (Observation-condition absence — anemia). For comparison
  operators with no matching prior observation, upstream returns FALSE
  (the documented issue-774 band-aid in `Logic.java`); `is nil` / `is
  not nil` are explicit absence tests and stay as-is. Change the sim's
  absence error to match: absent → false, recorded as a walk-level
  FACT (glass-box: keep a trace/log entry so the census substance read
  can still see it), not an error. The ADR discloses the distinction
  from honest-absence doctrine: absence is what this condition TESTS,
  so false-on-absent is the semantics, not a silent default.
* AR-4 (vital-name vocabulary — wellness-encounters). Extend the
  observation vital-name vocabulary with the COMPLETE catalog-wide
  enumeration at the pin — 22 names (mechanically enumerated:
  Observation states with category `vital-signs`; re-run the
  enumeration during the session and transcribe, don't trust this
  count) — killing the name-at-a-time unmasking pattern in one step.
  Names map per the vocabulary's existing shape.
* AR-5 (AllergyOnset + Vaccine — session reads). Two record-writing
  states the design channel located but did not read: `AllergyOnset`
  (extends `OnsetState` — read how it differs from `ConditionOnset`,
  which the sim already supports; expect the same onset/targeting
  shape with an allergy record entry) and `Vaccine` (read `process`
  fully — series/dose fields if any, draw behavior, encounter
  requirements). Implement per the pinned reads, following the nearest
  existing state's sim shape (`ConditionOnset` for AllergyOnset;
  medication/immunization-adjacent for Vaccine), with the read
  findings recorded in the ADR. If either read reveals scope
  materially beyond a mechanical port (e.g., an engine-side channel),
  STOP-AND-ESCALATE with the read rather than improvising.
* AR-6 (oracle bracket — pure identity). Run a FRESH recursive
  vendored-root scan for every mechanism in this wave (SetAttribute
  co-present sources, NamedDistribution shapes, Observation conditions
  with absence-reachable paths, the two state types, the 22 names) —
  do not reuse any prior scan's conclusion. Expected: no vendored root
  affected → every oracle batch byte-identical; any change
  STOP-AND-ESCALATE. If the scan itself finds a vendored root IS
  affected (e.g., a sepsis Observation condition whose absence path
  now returns false instead of erroring), STOP before Step 1 and
  report — that would convert part of this wave into a disclosed
  re-baseline, which needs an author ruling first.
* AR-7 (census re-run + the parity declaration). Same params,
  disambiguated filename. Expected: all 9 move; the classification
  distinguishes resolved vs unmasked (any unmasking is a finding — the
  tail should be the tail). IF the result is 84/85 walking + 1
  out-of-scope with zero `:load-failed`/`:walk-failed`: the records
  step adds the dated PARITY ACHIEVED note to the parity plan §1 (the
  countable definition, met, with the artifact named) and the roadmap
  — and Wave H becomes the sole remaining wave. If anything remains
  blocked, no declaration: record what and why, and the design channel
  reads it.

## Steps

Step 0 — Preflight. Standard; ADR-0039 at origin; next ADR 0040;
Synthea checkout at pin; AR-6's fresh scans recorded BEFORE any edit.

Step 1 — NamedDistribution. Schema + walk-time resolution + tests
(attribute present, default fallback, non-numeric error, one draw).
Red→green against `injuries`-shaped fixtures. Commit:
`feat(sim-trajectory): NamedDistribution transition probabilities --
attribute-fetched with default (ADR-0040 AR-1)`

Step 2 — SetAttribute precedence. Chain per AR-2 + conflict rejection
retired (dated note) + range-source support + tests per source and
precedence pair. Commit: `feat(sim-trajectory): SetAttribute source
precedence per upstream chain -- conflict rejection retired (ADR-0040
AR-2)`

Step 3 — Observation-condition absence. Absent → false with glass-box
trace + is-nil operators pinned by test + dated note on the old error.
Commit: `fix(sim-trajectory): Observation condition absent -> false
per upstream (ADR-0040 AR-3)`

Step 4 — Vocabulary completion. The enumerated 22-name transcription +
test that every name in the remaining catalog resolves. Commit:
`feat(sim-trajectory): vital observation vocabulary completed from
catalog enumeration (ADR-0040 AR-4)`

Step 5 — AllergyOnset + Vaccine. Per AR-5's reads; one commit each;
loader + interpreter + tests following the nearest-state pattern.
Commits: `feat(sim-trajectory): AllergyOnset state (ADR-0040 AR-5)` /
`feat(sim-trajectory): Vaccine state (ADR-0040 AR-5)`

Step 6 — Oracle bracket. Pure identity per AR-6; record; escalate on
any change.

Step 7 — Census re-run + parity determination. Per AR-7. Commit:
`docs(sim-trajectory): census after Wave I -- tail closed (ADR-0040)`

Step 8 — Records. ADR-0040 (rulings + AR-5/AR-6 read findings;
execution note: oracle table, classification, parity determination).
Plan/roadmap notes per AR-7. Roadmap: I → Done; H is Next (last).
Session record + prompt self-archive + budget check. Commit: `docs:
wave I records -- tail landed (archives prompt)`

## Fences

* No Wave H mechanics, no Wave E content, no census-tool fixes
  (overwrite bug stays worked-around).
* AR-5's escalation clause is real: a state that turns out
  non-mechanical stops the step, not the session — land the rest,
  disclose the gap, no improvised design.
* Red→green per step; fresh scans before edits; deviations get the
  dated appendix.

## Execution note (added at archive time)

Executed 2026-08-04, recorded `notes/ADRs.md` ADR-0040. Two deviations
from this prompt, both disclosed at the point found:

- **AR-1's "4 modules" claim did not hold against the live tree** —
  only `injuries.json` used NamedDistribution; the other three were
  blocked by unrecognized encounter-class values. Escalated to the
  user via AskUserQuestion before any code was written; folded in as
  AR-1b, a dated addendum, per the user's choice.
- **AR-7's own parity contingency did not hold** — the census landed
  at 82/85 walking + 1 out-of-scope + 2 walk-failed, not 84/85 + zero.
  No PARITY ACHIEVED note was written. Two modules
  (`congestive-heart-failure`, `wellness-encounters`) unmask new,
  unrelated, unfixed gaps once their own original blockers clear
  (Death's own cause-of-death forms; the `:active-careplan` condition
  type) — disclosed, not fixed, per AR-7's own instruction for exactly
  this case.

See `.agents/session-records/2026-08-04-gmf-coverage-wave-i.md` for the
full red→green evidence and judgment-call log.

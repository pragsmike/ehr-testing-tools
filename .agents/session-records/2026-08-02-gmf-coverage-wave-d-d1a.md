# 2026-08-02 — GMF coverage Wave D stage D1a: observation-family characterization (halts for ruling)

## Scope

Second session of GMF coverage Wave D (ADR-0029, R1–R7; D0 landed
`7935b71..7776098`). D1 is the observation family — `MultiObservation`,
`DiagnosticReport`, `VitalSign`-as-observation (R2(a)/(c)) — and its IR
addition, the `:diagnostic-report` step, is the highest-blast-radius
schema decision left in the wave. This session was scoped as
CHARACTERIZATION ONLY (E1): fetch the evidence, survey `sepsis.json`'s
closure, draft the schema as a PROPOSAL, and halt for a design-channel
ruling. No implementation commit, no `sim-model` edit, no interpreter
edit — the halt is the deliverable's shape, not a failure mode.
Implementation is D1b, prompted after the ruling. Full decision record:
`notes/ADRs.md` ADR-0029's own D1a characterization note and PROPOSED
section; full evidence account:
`components/sim-trajectory/docs/gmf-interpreter.md` section 11.

## Red→green evidence highlights

A docs-only session — no code changed, so the proof is the suite
staying green and untouched, not a red→green cycle. `poly check` and
`clojure -M:poly test :docs-tooling` (stale-path, index-completeness,
readme-presence, reading-set-budget, structure-currency, and every
other docs-tooling namespace) ran clean at every one of the three
checkpoints (Step 0/1/2); `gitleaks git --staged -v` clean at every
commit. Reading-set budget self-caught once (`:onboarding`, 925→932
lines, Step 0's own roadmap.md/ADRs.md growth) — the same recurring
pattern every prior GMF-wave session's own record already describes;
fixed forward in the same commit.

## Judgment calls and their ratification status

- **The demos-move (E2(a)) was verified oracle-free before moving, not
  merely asserted** — grepped for any code path reading under
  `components/sim/docs/demos/site-profiles/` before `git mv`ing it;
  zero hits (one comment-only citation, swept separately). Matches the
  rider's own stated precondition.
- **P1/P2 of the schema PROPOSAL choose to reuse the EXISTING
  `:observation` step shape verbatim for `:diagnostic-report`'s own
  children, rather than a bespoke child type** — a design judgment
  within the proposal itself, not a ruling; marked PROPOSED throughout,
  awaiting the design channel.
- **The `VitalSign` compile-mapping sketch (P3) is explicitly flagged
  LOW confidence**, sketched from R2(c)'s own ruling text alone since
  `sepsis.json` doesn't exercise `VitalSign` at all — deliberately NOT
  presented with the same confidence as the sepsis-grounded
  `MultiObservation`/`DiagnosticReport` mapping (HIGH confidence),
  surfacing a real gap (no source-given code exists for a `VitalSign`
  state) rather than papering over it.
- **P4 recommends Option A** (a documented default-range simplification
  for the `vital_sign` field gap) over Option B (leave unbuilt) but
  states the recommendation is not self-evident — left as Q3, an open
  question, not decided by this session.
- **The Clojars/H5 rider (E2(b)) touched only `notes/ADRs.md` ADR-0001's
  own H5 entry and `.agents/plans/roadmap.md`'s release row**, per the
  rider's own literal scope ("the roadmap's release row (and its ADR if
  one names the open question)") — two OTHER live citations of H5
  (`notes/ADRs.md` lines ~580 and ~1255, both still correctly describing
  H5 as a whole, which still has an open coordinates-naming half) were
  read and deliberately left untouched, a scope call disclosed here
  rather than silently expanded or silently skipped.
- **Q4 names, rather than resolves, whether one closure's worth of
  evidence is enough to rule the HIGH-confidence half of the proposal**
  — this session's own budget did not extend to fetching and
  characterizing a second `MultiObservation`/`DiagnosticReport`-bearing
  module (`congestive_heart_failure`/`gallstones`/`wellness_encounters`/
  `dialysis`/`lung_cancer`/`colorectal_cancer`, all cited in the existing
  prioritization table but none closure-read this session).

## Findings and HEAD landed

- **`number_of_observations` (the JSON field) is dead** — grepped
  exhaustively across `State.java`, never read; the real children count
  is the `observations` list's own Java-level `.size()`, always. A
  concrete, source-grounded finding, not an inference from `sepsis.json`
  alone.
- **The `vital_sign` field's real source, `LifecycleModule.java`, is a
  hardcoded Java module this project has never ported and has no
  persona/clinical-state equivalent for** — traced past `Person.java`'s
  own `getVitalSign` (which throws rather than defaulting) to the actual
  setter call sites. A genuine, load-bearing gap distinct from R2(c)'s
  own `VitalSign`-state/`Vital-Sign`-condition dissolution design.
- **`sepsis.json` needs ZERO of the three D3-scoped transition kinds** —
  checked against all six of its own `distributed_transition` states'
  literal weight values, none attribute-sourced. D1 carries no D3
  dependency via transitions; the prompt's own anticipated "shrinking or
  resequencing" did not occur on this axis.
- **The `VitalSign` state type and `Vital Sign` condition type are BOTH
  absent from `sepsis.json`'s own closure** — R2(c)'s own dissolution
  design is therefore neither confirmed nor contradicted by this
  session's evidence, recorded as a negative result rather than silently
  treated as validation.
- **Root `docs/site-profiles.md`'s own `docs/demos/site-profiles/`
  citation was broken since the `c0b5b0a` merge** (pre-dates this
  session, unrelated to D1a's own subject) — fixed forward with a dated
  provenance note, per E2(a).
- **H5's Clojars-vs-Maven-Central sub-question was already, in effect,
  ruled 2026-07-31** (`.agents/plans/roadmap.md`'s own "Clojars publish"
  release row) but never cross-referenced against `notes/ADRs.md`
  ADR-0001's own H5 entry — cross-referenced now, per E2(b); the
  coordinates-naming half and publication itself both stay open/parked.
- This session ran under R30 (commit and push at each checkpoint,
  unattended), the ratified standing default, exactly as the prompt
  itself named. Every checkpoint's `git diff --cached --stat` matched
  that checkpoint's own file list before staging; every commit message
  written to a plain file first (never an inline heredoc through the
  WSL wrapper); `git commit -F`'d; every push verified against its
  message file (every diff was the expected trailing-newline formatting
  artifact, never a real mismatch).
- HEAD at session end: this record's own companion commit (`docs: D1a
  records (session record; archives prompt)`) — see that commit for its
  own sha. Prior commits, in order: `de5bf51` (Step 0, riders),
  `b210ae0` (Step 1, characterization), `9f33bed` (Step 2, schema
  PROPOSAL).

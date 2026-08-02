# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (in progress)
- GMF coverage Wave D, stage D1 (ADR-0029 R6) — observation family
  (`MultiObservation`/`DiagnosticReport`/`VitalSign`-as-observation, one new
  `:diagnostic-report` IR step). D1a (characterization + schema PROPOSAL,
  2026-08-02) halted for a design-channel ruling; RULED same day (ADR-0029's
  own dated ruling note, Q1–Q4 resolved) — D1b (implementation: reference
  table, IR/loader/interpreter/compile/engine/emit chain, sepsis.json
  vendoring) is IN PROGRESS, session started 2026-08-02.

## Next (backlog, no session scheduled)
- Pairing-as-data (review P3-3): mutate↔judge conviction registry — design pass in
  the design channel first; vocabulary is load-bearing
- Storefront demo fixture: minimal clean-gating FHIR fixture so the README's mutate
  demo shows a real accepted→rejected flip (2026-08-01 capture session finding)
- make quickstart → nightly integration workflow + single-```sh-fence guard in README
  (quickstart_fresh docstring corrected in same change)
- generator-source three-concerns split (ADR-0017 named-future)
- ehrt.corpus.display placement — presentation-leaning (ADR-0018 named-future)
- Markdown-table helper dedup (ADR-0018 named-future)

## Externals (author-only)
- NIST licensing inquiry: send the drafted gist (retires the confirmation-pending
  posture cited on the storefront Gate row)
- IG pinning: choose and commit the profile-tier conformance target (Gate row's
  other caveat)
- Clojars publish, when satisfied with the product (ruled 2026-07-31; ends the
  greenfield era — output formats freeze harder after first tag). **Dated note
  (D1a rider, 2026-08-02): this row IS the Clojars-vs-Maven-Central ruling —
  cross-referenced into `notes/ADRs.md` ADR-0001's own H5 entry today, closing
  that half of H5 as an open gate; the group/coordinates naming half and
  publication itself both stay open/parked, unchanged by this note.**
- SETUP rewalk by an unspoiled human reader (F3 superseded-pending-rewalk)
- Upstream the adapted repo-adaptation skill to pragsmike/skills (and cyberneutics
  if wanted) — AUTHOR ACTION named 2026-08-01
- Item 9 (ADR-0024, landed 2026-08-01 as mirror-with-gate, not symlinks): the
  fresh-session discovery probe is DONE — see Done section below. Remaining:
  fast-forward the /mnt/c clone to origin/main (several commits behind,
  including .claude/skills/) — AUTHOR ACTION named 2026-08-01

## Deferred (explicitly, with revisit triggers)
- P2-5 intake staging-dir behavior (deferred 2026-07-31)
- Reading-set budget numbers (charter §6: rule after real sizes are measured)
- Verdict-cache placement revisit (ADR-0011 note: second consumer, or never)
- Sim-manifest interop design between sim and corpus (pre-review open thread)
- Sim split S4 (`sim-engine`: `engine`, `churn`, `order-profiles`) —
  trigger: a second `engine` consumer appears (the FHIR emitter is the
  likely one) or engine work itself needs the emit-state/check boundary
  designed, same plan
- GMF coverage Wave D, stages D2–D3 (ADR-0029 R6; D0 done, D1 is in Now
  above) — D2: CarePlan family (paired IR span, `Active CarePlan`
  condition; CarePlan itself stays v2-silent, R3) — payoff: MI,
  `total_joint_replacement`, closures permitting. D3:
  `lookup_table_transition` (sixth transition kind) + attribute-weighted
  `distributed_transition` weights + UTI closure re-characterization —
  payoff: UTI. `ImagingStudy` (R5, CHF trigger) and the stroke-risk data
  source (R7) are named in ADR-0029/the coverage plan but unowned by
  D0–D3.

## Done (this session, 2026-08-01, migration session 1)
- Items 6+7: `agent/scenario-roster.md` merged into `.agents/skills/scenarios/roster.md`,
  `agent/` (singular) retired (47c815c)
- Item 13: `.agents/plans/roadmap.md` (this file) lands from the design-channel
  ledger handover (47c815c)
- Item 9: `.claude/skills/` mirror-with-gate lands (ADR-0024); end-to-end proof
  and the /mnt/c fast-forward are AUTHOR ACTION, see Externals above (a9e5be6,
  8df3cf3)

## Done (this session, 2026-08-02, migration session 2)
- Items 1+12: `notes/prompts/` sealed — `ehrt.docs-tooling.notes-prompts-frozen-test`
  pins the 29-file set, `stale_path_test.clj` gains the archive-instruction
  tripwire (both red→green live-proven) (6c3c494)
- Items 4+11: `notes/README.md` lands (six top-level files + three subdirs
  indexed, zone-marked); `.agents/skills/README.md` plus all 10 skill-directory
  READMEs land (mirrored to `.claude/skills/`); `ehrt.docs-tooling.readme-presence-test`
  enforces both trees going forward, `notes/sim/`/`notes/tools/` exempt
  (ruling 6) (ab9fe5e)

## Done (this session, 2026-08-02, migration session 3)
- Item 10: `ehrt.docs-tooling.index-completeness-test` lands — both directions,
  over `.agents/plans/`, `.agents/prompts/`, `.agents/session-records/`,
  `.agents/skills/`, `notes/`; `notes/prompts/` convention-exempt; ruling 6
  extended to completeness (77880f7)
- Item 3(a): sim citation-stubs pass, reading (a) — 8 F-rows and 10 ADRs cited
  at their live restatement site (4 were miscitations, fixed not just
  supplemented); `notes/facts-register.md` F20 stub names the two-file
  topology; full accounting in the session record (54ab3b6)

## Done (this session, 2026-08-02, migration session 4)
- Item 8: `.agents/reading-sets.edn` lands — five named sets, each path
  justified inline, gated by `ehrt.docs-tooling.reading-set-budget-test`
  (ghost-path + budget checks, both red→green live-proven); every
  `:budget-lines` is this session's own measured actual, corrected once
  more for this section's own weight (see the session record's "it
  caught itself twice" note): `:onboarding` 538, `:corpus` 1519, `:sim`
  574, `:judge` 644, `:docs` 433 — the baseline the author's future
  budget ruling (charter §6) now has a real number to cite (ab679c9)
- `components/sim/src`/`test` bare-`ADR-NNNN` docstring sweep (the "Next"
  backlog item migration session 3 named): 151 bare references across 39
  files classified and requalified — 149 to `sim/ADR-NNNN`, 1 to
  `tools/ADR-0015` (a cross-repo miscitation this sweep discovered), 1
  left deliberately bare (already correctly cites the live register);
  two further wrong-file-path miscitations also fixed; docstring/comment/
  fixture-remark edits only, zero behavior change; full one-to-one
  accounting in the session record (72f5542)

## Done (this session, 2026-08-02, migration session 5)
- Item 5: way-of-working session mechanics distilled into five repo-local
  skills -- `build-session`, `capture-session`, `extraction-stage`,
  `errata-sweep`, `session-prompt` -- each citing its own provenance in
  AGENTS.md/AUTHORS-GUIDE.md/notes/ADRs.md/named session records;
  mirrored to `.claude/skills/`, indexed in both READMEs, all three
  affected gates (readme-presence, index-completeness,
  skill-mirror-currency) green (60b9f87)
- Item 9's fresh-session discovery probe: CONFIRMED 2026-08-02 (external
  observation, no commit) -- a fresh, non-nested Claude Code session's
  own Skill listing showed `wsl-windows-git-hygiene` with its full
  description, closing the acceptance test migration session 1 could
  not self-administer. The paired AUTHOR ACTION (fast-forward `/mnt/c`)
  remains open, see Externals above.
- `build-session` added to all five `.agents/reading-sets.edn` sets
  (AR-3: ceremony applies regardless of task class); `session-prompt`
  named in `:onboarding`'s own comments for design-channel
  prompt-authoring sessions, not added as a budgeted path;
  `capture-session`/`extraction-stage`/`errata-sweep` stay excluded from
  every set, same reason the existing ten skills are (session-type, not
  domain-task-class). Budget deltas in the session record and
  `notes/ADRs.md` ADR-0023's own dated-note thread.

## Done (this session, 2026-08-02, migration session 6)
- Item 14 (last open item, migration report fully executed): use-cases
  split — `docs/use-cases.md` is now a generated index (one line per
  case, linking out), `docs/use-cases/<id>.md` is one standalone page
  per case, both from `components/corpus/docs/use-cases.edn` unchanged
  except for the internal cross-case and reference-doc links the file
  split itself requires (never a case's own narrative/strip/equations
  text). Content conservation proven one-to-one against the prior
  single-file rendering (script-diffed per case, modulo heading/banner/
  link-depth scaffolding); the CI freshness gate
  (`.github/workflows/test.yml`) now diffs `docs/use-cases/` alongside
  the index. Every repo citation of `docs/use-cases.md#<case>` swept to
  its per-case file. Full accounting in the session record (ceca0f7,
  plus this checkpoint's own commit).

## Done (this session, 2026-08-02, sim split S1+S2)
- Sim split S1: `components/sim-model` extracted from `components/sim`
  (`pathway`/`facility`/`persona`/`config`), `.agents/plans/2026-08-02-
  sim-split-plan.md`'s AR-1..AR-4, `notes/ADRs.md` ADR-0025. Committed
  and pushed by the author (8d5c86c) before S2 began.
- Sim split S2: `components/sim-trajectory` extracted from
  `components/sim` (`gmf`/`gmf-interpreter`/`compile-trajectory`, their
  three docs and vendored-module fixtures), same plan, same ADR.
  poly check clean, poly test 0 failures/0 errors, golden run
  byte-identical, deftest+defspec parity (403=403=403) at every stage.
  Full caller map, verification baselines, and the one fixed-forward
  deviation (fixture resource-path/`ns`-form misses, caught by `poly
  test` before commit) in the session record and ADR-0025's own
  Decision section.
- S3 (`sim-emit-hl7`)/S4 (`sim-engine`) rows moved to Deferred above
  with their trigger conditions; the GMF coverage-expansion payoff
  milestone S2 unblocks is deferred alongside them, explicitly not
  started this session (plan's own R-4).

## Done (this session, 2026-08-02, GMF coverage Wave A)
- Condition vocabulary v1→v1.1: `:or`/`:at-least` (compound wrappers),
  `:date` (calendar-year vs. `ctx`'s own virtual clock), `:observation`-
  as-a-condition-type (log query over already-emitted `:observation`
  events), and `:symptom`-as-a-condition-type (an emergent finding, not
  one of AR-2's five named candidates — required for `:at-least`'s only
  real vendored use). `Active Allergy` needed no new work (already M5b).
  `Vital Sign`/`Active CarePlan` confirmed still OUT (AR-2's pre-ruling,
  neither appears in `sore_throat.json`). `notes/ADRs.md` ADR-0026.
  Commits: `0b2c1b2` (wave plan), `9176250` (`:symptom`), `f99e87a`
  (`:at-least`+`:or`), `5e3e72c` (`:date`), `6a35492` (`:observation`),
  `6a3e11b` (docs).
- `sore_throat.json` vendored (`a2cf68d`) — the wave's own payoff module,
  state-type clean since M5-prep, blocked at every prior survey by
  exactly the condition-vocabulary gap this wave closed. Real branch
  coverage through `Determine_if_Bacterial`'s `At Least` compound
  (`vendored_sore_throat_test.clj`, both threshold branches).
  `stroke.json`'s own `Date` gap is also resolved, but the module stays
  deferred (its `Death` state-type gap, AR-6 — Wave C's own trigger).
- Regression oracle (fixed-seed `sinusitis`/`appendicitis` walks)
  byte-identical across every commit this session, confirmed at close.
  Full accounting in the session record and ADR-0026's own Decision
  section.

## Done (this session, 2026-08-02, GMF coverage Wave B)
- `CallSubmodule` — loader closure resolution (D3, `gmf/load-closure`,
  the all-or-nothing gate extended over a whole closure, a cyclic-graph
  check with a real bug found and fixed by its own red test),
  interpreter call/return (D1/D4, descend-run-return, root-scoped
  workflow attributes, a defensive call-depth backstop), and D2's own
  cross-boundary `:call-path` citations. The fifth transition kind,
  `type_of_care_transition` (D5, characterized against Synthea's own
  `Transition.java` + `telemedicine_config.json` before
  implementation — a real Java `Random` sequential-seed clustering bug
  found and fixed in the test suite itself). `notes/ADRs.md` ADR-0027.
  Commits: `a92254b` (ADR + roadmap), `f596a37` (closure survey, D5/D7),
  `9a2f0cd` (D1 refactor), `599fa47` (D3 closure loading), `cc9e0d6`
  (D1-D4 call/return), `13b924e` (encounter-class normalizations,
  disclosed addition), `3adf974` (D5).
- `ear_infections.json` closure vendored (`01eb56b`) — the fourth real
  vendored module and this project's first CLOSURE (root plus two
  called submodules, `resources/modules/NOTICE`'s own new rows), state-
  type clean of every Wave-D-scoped deferred type once its real closure
  was read, at the cost of two Step 2c mandatory-path findings
  (`assign_to_attribute`/`referenced_by_attribute`, `is nil`/`is not
  nil` operators) plus two Step 2e loader normalizations. A real,
  end-to-end vendored walk reaches through a called submodule with
  correct call-path citations and cross-module `referenced_by_attribute`
  resolution (`vendored_ear_infections_test.clj`).
- **Payoff-map update: `urinary_tract_infections.json` is NOT vendored
  this wave (D6).** Its own real closure (twelve files, not the four
  the wave plan's own top-level survey assumed) is dirty in EVERY
  branch with `DiagnosticReport`/`MultiObservation`, both Wave D's own
  scope — the payoff shrinks from two closures to one, honestly, per
  the session prompt's own "contingent on its closure surveying clean"
  framing. A genuinely new, unplanned finding along the way:
  `lookup_table_transition`, a SIXTH GMF transition kind, on that
  module's own entry path — named, not built (ADR-0027's own Deviation
  record). `total_joint_replacement.json`/`myocardial_infarction.json`
  had `CallSubmodule` removed from their own blocker lists
  (`docs/gmf-interpreter.md`'s own dated notes) but neither was
  re-characterized this session — both still need a real closure read
  before any vendoring claim, and MI still needs Wave C (`Death`) per
  the wave plan's own "B+C → MI" sequencing either way.
- Regression oracle (fixed-seed `sinusitis`/`appendicitis`/
  `sore_throat` walks) byte-identical across every commit this session,
  confirmed at close; full workspace `poly test :all skip:integration`
  green (a self-caught `.agents/reading-sets.edn` budget bump along the
  way, the same shape Wave A's own close-out already hit).

## Done (this session, 2026-08-02, GMF coverage Wave C)
- `Death` lands as real, terminal, trajectory-event-producing v1 state
  (loader + interpreter, `notes/ADRs.md` ADR-0028 C1/C2) — three time
  forms grounded against `State.java`'s own real `Death` class at the
  pinned commit (immediate/exact/range, `range` reusing the same
  fixed-consumption helper `Delay`/`Procedure` duration already share);
  only the `:codes` cause-of-death form is built (`:condition-onset`/
  `:referenced-by-attribute` named unbuilt, no vendored module needs
  either). The walk terminates AT `:death` — a disclosed departure from
  real Synthea's own continue-past-Death semantics — property-tested
  (`no-trajectory-event-ever-follows-death`, 200 seeds).
- `:death` compiles into the pathway WITHOUT a new IR step type (C4) —
  `:discharge` (`sim-model/pathway.clj`) gains two new optional fields,
  `:disposition`/`:codes`; real HL7v2 already models a death this way
  (an ordinary ADT^A03 whose PV1-36 carries an expired disposition
  code). Death inside an encounter attaches as its own terminal
  disposition; death outside any encounter closes the pathway without
  fabricating a discharge from an admission that never happened.
- `:expired` lands in CODE for the first time (C3) — `PatientState`'s
  own `:status` enum, `docs/patient-state-model.md`'s own accumulator
  claim, docs-only until this session (checked directly: `:expired`
  existed nowhere in `components/sim/src` except three lines of prose).
  `ehrt.sim.engine`'s own `:discharge` decide/evolve fold an
  expired-disposition discharge to `:status :expired`, location/
  attending UNCHANGED (the body stays where it was), and suppress the
  existing bed-ready-transfer coupling (a finding the docs' own gap
  table didn't name — no bed is actually vacated). One new structural
  invariant, `expired-patient-retains-location`; `order-only-when-
  admitted`/`clinical-content-only-when-admitted` already generalize to
  cover `:expired` automatically, zero code change, confirmed by the
  green suite.
- **Payoff-map correction: `stroke.json` is NOT vendored this wave.**
  Real-closure characterization found `Chance_of_Stroke`'s own
  `distributed_transition` gates onset on `{"attribute": "stroke_risk",
  "default": 0}` — a real Synthea engine attribute (Framingham
  cardiovascular risk, `CardiovascularDiseaseModule`) this project has
  no source for, whose own JSON default makes onset structurally
  unreachable if honored literally. Escalated and ruled (design
  channel, 2026-08-02): deferred, D6-style, revisit trigger named
  (`docs/gmf-interpreter.md` section 10; ADR-0028's own Deviation
  record). `Death` is proven instead against this project's own hand-
  authored `death-fixture.json` — interpreter, compile-trajectory, and
  a full 200-patient engine/check round trip, both outcomes present,
  the full invariant catalog holds.
- Commits, in order: `7e4204b` (Step 0, ADR + coverage-plan riders),
  `ed4f7bd` (Step 1, characterization), `a900f99` (Step 2a, Death state/
  terminal contract), `47d0f66` (Step 2b, compile-trajectory mapping),
  `380a3e2` (Step 2c, engine/check minimal path), `66005ae` (Step 3,
  death-fixture proof). `notes/ADRs.md` ADR-0028.
- Regression oracle (fixed-seed `sinusitis`/`appendicitis`/
  `sore_throat`/`ear_infections` walks) byte-identical across every
  commit this session, confirmed at close; full workspace `poly test
  :all skip:integration` and `poly check` green throughout.

## Done (this session, 2026-08-02, sim split S3 / GMF coverage Wave D stage D0)
- Sim split S3: `components/sim-emit-hl7` extracted from `components/sim`
  (`emit-hl7`/`v2-replay`/`site-profile`), fired now per ADR-0029 R1
  rather than waiting on its own named trigger — front-running
  deliberately, the same session that ruled Wave D's own design (R1–R7).
  `ehrt.sim-emit-hl7.interface` re-exports `emit` (3/5/6-arg only),
  `control-id-for`, `default-reference-date`, `default-utc-offset`;
  `v2-replay`/`site-profile` have no real external caller and stay fully
  internal. `notes/ADRs.md` ADR-0025 (dated S3-executed note) and
  ADR-0029 (Wave D design, D0 execution note).
- `poly check` clean; `poly test :all skip:integration` 0 failures/0
  errors; golden run (seed 42, 5 patients, `--emit hl7`) byte-identical
  except the manifest's `:generator :sha256` (tracks HEAD, expected);
  deftest+defspec parity 281 = 206 residual + 75 `sim-emit-hl7`.
- Wave D itself (D1–D3: observation family, CarePlan family,
  `lookup_table_transition`) is not started — named in ADR-0029 and
  `.agents/plans/2026-08-02-gmf-coverage-plan.md`'s own restructure,
  each its own future session.
- Commits, in order: `7935b71`/`7a3dd58` (Step 0, ADR-0029 + plan
  restructure, plus a same-session fix-forward for an ADR
  insertion-order mistake caught before Step 1), `ccce1fc` (Step 1,
  characterization), `e38e232` (Step 2, extraction), and this session's
  own closing records commit.

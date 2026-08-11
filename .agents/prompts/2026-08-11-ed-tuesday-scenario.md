# 2026-08-11 — ehr-testing-tools: ed-tuesday scenario (ADR-0104)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `741b2f6` (ADR-0103's own close) and closed at
the scenario commit (`51f0e68`) plus this record's own close-phase
commit. Original prompt follows verbatim.

## Original prompt (verbatim)

Session prompt -- ed-tuesday: the scripted ED scenario (ADR-0104)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session executes option A of the author's ruled
"C-with-A-first" (2026-08-10): a NEW sibling scenario,
demos/scenarios/ed-tuesday/, whose weighted scripted-pathway pool
simulates a single busy ED shift -- real admissions, transfers,
discharges, churn -- alongside a thin ambulatory module tail.
busy-tuesday stands unchanged as the population-scale contrast
(its config was landed verbatim under AR-VB2-R -- a ruled artifact).
Option B (vendoring upstream's injuries family) is a SEPARATE future
batch under the standing vendoring ceremony -- not this session.
HEAD at handoff: 741b2f6. This session's ADR is ADR-0104.

Machinery facts (channel-probed, verify-then-act):
- `:pathways` (plural) is tested config machinery: a pool of
  `{:pathway P :weight N}` entries plus optional
  `{:patient-ordinal i :pathway P}` explicit entries
  (components/sim/test/ehrt/sim/run_test.clj ~195-214).
- CONSTRAINT: an ordinal with BOTH an admission-bearing pathway AND a
  module is `:incompatible-assignment`. The static check
  (run.clj ~119-152) rejects only CERTAIN conflicts; how a weighted
  mixed population is legitimately expressed (disjoint sub-pools?
  runtime behavior on an RNG-coincident draw?) is THIS SESSION'S
  FIRST verification task -- read `assign-module` and the pathway
  assignment in sim-engine, plus the `:incompatible-assignment`
  machinery end to end, BEFORE authoring the config. If a weighted
  mixed population is not expressible without per-ordinal explicit
  entries or without RNG-dependent rejection risk, STOP-AND-REPORT
  with the semantics you found -- that is an engine-side design
  moment for the author, not a config workaround.
- Default facility (sim-model/config.clj): ward names "Emergency"
  (:class :ed, 0 beds + 6 surge), "Renal", "Cardiology" (:inpatient,
  4 beds + 2 surge each). Scripted `:admission`/`:transfer`
  `:location` values are these NAMES.
- Pathway step vocabulary (sim-model/pathway.clj `Step`): :admission
  {:location :reason}, :delay {:from :to} in MINUTES (engine samples
  uniformly, seeded), :transfer {:location}, :discharge (optional
  :disposition :expired), :order {:profile} (auto-paired result),
  the churn family, :outpatient-visit/-end.
- `--churn` (sample-profile) inserts operational-noise steps per gap
  around authored steps -- with real admissions present it now has
  material to work with.
- Scenario conventions (demos/scenarios/README.md): a scenario is a
  RUNNABLE configuration, never a captured trace; the README carries
  generate + play commands and a witnessed block. Mirror
  busy-tuesday's landing gates: read ADR-0071's AR-VB2-R rider
  section for what accompanied that config (validity checks, live
  probe) and match the pattern.

Oracle bracket, with its reasoning: pure identity on all 34 roots is
EXPECTED -- the footprint is a new scenario directory, two README
cross-reference lines, and close-phase files; zero src or test
namespace changes (unless the assignment verification triggers
STOP-AND-REPORT, which ends the session before any landing). Movement
= STOP-AND-REPORT.

## Read first

- .agents/plans/roadmap.md -- the ED-redesign Next row (this session
  executes its A half; the row stays open for B, amend its text to
  say so at the close)
- notes/adr/0103-*.md -- the chartering context and diagnosis
- notes/adr/0071-*.md -- AR-VB2-R (how busy-tuesday's config landed)
- components/sim-model/src/ehrt/sim_model/pathway.clj -- Step schema
  in full
- components/sim-model/src/ehrt/sim_model/config.clj -- facility,
  wards, config schema
- components/sim/src/ehrt/sim/run.clj -- assignment translation and
  the incompatible-assignment check
- components/sim-engine/src/ehrt/sim_engine/engine.clj --
  assign-module and pathway assignment (the Step 2 verification)
- demos/scenarios/README.md and demos/scenarios/busy-tuesday/ -- the
  conventions and the sibling being contrasted
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-10, author verbatim "C-with-A-first." -- this session
  is A: the scripted ED scenario. B (injuries vendoring) is not
  touched; the roadmap row records it as the remaining half.
- [A] 2026-08-10, direction, author verbatim: "Maybe weight the
  patient population toward immediate, emergent conditions like
  trauma/injuries? This would simulate an actual ED, which is where
  a lot of the activity and churn would happen." The scenario's
  weighting honors this: the pathway pool is weighted toward
  quick emergent presentations, with admissions and churn visible
  on the board.
- [C] Sibling-not-revision (flagged to the author, un-vetoed):
  demos/scenarios/ed-tuesday/ is NEW; busy-tuesday's config is
  untouched; each README gains one line cross-referencing the other
  as its contrast (day-scale scripted vs population-scale
  incidence).

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0103 landing at `741b2f6` by fresh
   public clone. Tag `stable-20260810-board-boundary-catchup` at
   `741b2f6`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Verify the population-split semantics** per the Context
   constraint. Outcome A: a sanctioned weighted mixed-population
   shape exists -- record it in the ADR with the code citations and
   proceed. Outcome B: it doesn't -- STOP-AND-REPORT with the
   findings; do not author around it.

3. **Author the scenario.** demos/scenarios/ed-tuesday/config.edn:
   a pathway pool weighted toward quick emergent presentations --
   e.g. ED fast-track (admission "Emergency", short delays,
   discharge), ED observe-and-discharge (longer delays, an :order),
   ED-admit-and-transfer (admission "Emergency", delay, transfer
   "Renal" or "Cardiology", delay, discharge) -- exact trajectories,
   weights, delays are yours to author against the schema, tuned so
   a board at --board 60 shows inpatients rising and falling,
   discharges accruing, and churn events when --churn is on; plus
   the ambulatory module tail per the verified split shape (a few
   of the already-vendored quick modules, low weight). Day-scale:
   arrival-gap a few minutes, module horizon short. The config's
   header comment states its provenance (this ADR) and the tuning
   intent. README.md per the scenario conventions: generate command
   (WITH --churn and a --reference-date), play commands (--board,
   and the events.edn playback line), a witnessed block from a real
   run (the numbers you actually saw -- inpatients > 0 is the
   scenario's whole point; if tuning can't produce it, that is a
   finding, not a silent retune loop), and the busy-tuesday
   cross-reference line. Add the sibling line to busy-tuesday's
   README and the parent README's scenario list. Mirror AR-VB2-R's
   gate pattern for config validity.
   Commit message (ASCII only):
   `feat: ed-tuesday scenario -- scripted ED shift, weighted emergent mix (ADR-0104)`

4. **Oracle bracket.** Expected pure identity per Context;
   movement = STOP-AND-REPORT.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0104
   (the split-semantics findings with citations, the authored
   trajectories' rationale, the witnessed block verbatim,
   deviations dated); roadmap: the ED row amended -- A landed, B
   (injuries vendoring) remains, anchored; .agents/rulings.md
   records "C-with-A-first." verbatim; notes/ADRs.md index row;
   notes/adr/README.md count 101 -> 102; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- ed-tuesday scenario (ADR-0104)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: demos/scenarios/ed-tuesday/ (new),
  demos/scenarios/busy-tuesday/README.md (one cross-ref line),
  demos/scenarios/README.md (list line), notes/adr/0104-*.md,
  notes/ADRs.md, notes/adr/README.md, .agents/* close-phase files.
  The sweep RULE governs over this list (ADR-0099 precedent).
- NO src or test changes anywhere. busy-tuesday/config.edn is a
  ruled artifact -- untouched. If Step 2 hits Outcome B, the session
  ends at STOP-AND-REPORT with nothing landed but the report.
- Tuning is authorship, not code: if the board cannot show the
  intended activity from config alone, report, don't patch engines.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims are verify-then-act.

## Deviation record

None from the driving prompt's own steps, fences, or rulings. Step 2
found Outcome A (a sanctioned, provably conflict-free disjoint-cohort
shape) — the session proceeded to author and land, as the prompt's
own branch anticipated. The ambulatory module tail showing zero live
encounters in the shipped run is disclosed under the prompt's own
named contingency ("if tuning can't produce it, that is a finding, not
a silent retune loop"), not a deviation from any ruling or fence — see
`notes/adr/0104-ed-tuesday-scenario.md`'s own Deviations section for
the full account.

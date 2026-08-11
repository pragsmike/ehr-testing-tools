# 2026-08-11 — ehr-testing-tools: injuries B2 assessment (ADR-0106)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `1abee30` (ADR-0105's own close) and closed at
this record's own close-phase commit (no code/module commit — Branch C
fired, nothing vendored). Original prompt follows verbatim; a
deviation record follows that.

## Original prompt (verbatim)

# Session prompt -- B2: injuries vendoring, assessment-first (ADR-0106)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session is B2 of the injuries arc, under the author's
2026-08-11 ruling "b": ATTEMPT the injuries vendoring batch under the
standing ceremony, with a WIDENED charter -- if the known pre-existing
`nested :encounter` gap fires at the round-trip gate, the session's
deliverable BECOMES the full characterization of that gap under the
ADR-0070 bail-out precedent, and nothing is vendored. Either outcome
is a successful session. HEAD at handoff: 1abee30. This session's
ADR is ADR-0106.

Known state (channel-probed and ADR-recorded; verify-then-act):
- ADR-0070 deferred injuries WHOLE on the run-submodule max-steps
  defect; ADR-0105 closed that exact defect (0/120 max-steps failures
  post-fix at ADR-0070's own probe parameters).
- ADR-0105's probe ALSO found: 2/120 walks fail on `Assert failed:
  ... nested :encounter -- this project's GMF subset assumes
  encounters never nest`, SAME two seeds pre- and post-fix --
  a separate, pre-existing interpreter-subset gap. At a
  300-patient round-trip population, firing is ~99.4% likely
  ((1-2/120)^300) -- expect the characterization branch.
- The closure (8 files): 4 already vendored byte-identical at the pin
  from prior batches (medications/ear_infection_antibiotic,
  medications/otc_pain_reliever,
  medications/moderate_opioid_pain_reliever, dme/wheelchair_end) --
  re-verify at the pin, never re-vendor, no new NOTICE rows; 4 new
  (injuries.json, injuries/broken_jaw.json,
  snf/skilled_nursing_facility.json, dme/wheelchair.json).
- ADR-0070's expected-count lesson: `:closure-file-count` counts JSON
  only -- check the closure for lookup-table CSVs before estimating
  the landing set.
- AR-VB4-2's attribute-gate hazard check applies; broken_jaw's
  `dental_referral` never-cleared gate is that exact class, now
  HANDLED by ADR-0105's horizon truncation -- record it as
  handled-by-ADR-0105, not disqualifying.

## Read first

- notes/adr/0070-*.md -- AR-VB1-2/3/4 mechanics IN FULL, the injuries
  section, the expected-count disclosure
- notes/adr/0090-*.md -- AR-VB4-0..5 (fresh-gate-at-pin discipline,
  attribute-gate check, per-passer landing shape, bracket form)
- notes/adr/0105-*.md -- the probe method, the nested-encounter
  finding verbatim, the two failing seeds
- components/sim/resources/sim/modules/NOTICE -- pin discipline,
  the injuries dated section
- components/sim-trajectory/src/ehrt/sim_trajectory/
  gmf_interpreter.clj -- the nested-encounter assert site and the
  encounter open/close model around it
- components/oracle/src/ehrt/oracle/digest.clj -- root mechanics
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim "b" (to: run B2 as an
  assessment-first session with a widened charter: attempt the batch
  under the standing ceremony, and if the nested-encounter assert
  fires, the session's deliverable becomes the full characterization
  -- root cause, upstream semantics, rate -- under the bail-out
  precedent).
- [C] Channel finding to record at the close regardless of branch
  (ADR-0105 verification, design channel): the landed step budget
  accumulates without reset, so a LEGAL time-advancing loop exceeding
  ~10000 iterations within horizon still throws via its one
  zero-advance check per cycle -- unreachable for 1-7-day delays by
  concentration, a real class for e.g. a fixed 1-day wait over 50
  years. A dated finding line in ADR-0106, no action.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0105 landing at `1abee30` by fresh
   public clone. Tag `stable-20260811-interpreter-horizon-budget` at
   `1abee30`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Fresh gate at the pin (AR-VB4-1 discipline).** Re-enumerate the
   closure at the recorded pin; re-verify the 4 already-vendored
   members byte-identical; fetch the 4 new members to SCRATCH (never
   committed until/unless the vendoring branch lands them); check
   for lookup-table data files; run the attribute-gate check
   (AR-VB4-2). Then the round-trip probe: ADR-0070's own method
   (direct interpreter, registration age 30, 50-year horizon,
   well-mixed seeds, >=120 walks) AND an engine-level population run
   at the round-trip test's own parameters.

3. **THE FORK -- read the probe honestly, then take exactly one
   branch:**

   BRANCH V (assert does not fire at the round-trip gate's own
   parameters): land the batch per AR-VB1-2/3/4 + AR-VB4-3
   mechanics. Byte-verbatim copies, SHA-256 + pin NOTICE rows for
   the 4 new members (dated section updated: the deferral lifted,
   citing ADR-0105), one round-trip test per landed module
   red-before-resource, new engine-layer oracle roots as FIRST
   BASELINES, modules AVAILABLE not default. Note in the ADR that
   the 2/120 interpreter-level rate still exists at OTHER seeds --
   vendoring with a known seed-dependent walk hazard needs its own
   honest sentence and a named tripwire (the round-trip test's seed
   pins the green; a future seed change may trip the assert).
   Commit message (ASCII only):
   `feat: injuries closure vendored -- deferral lifted per ADR-0105 (ADR-0106)`

   BRANCH C (the assert fires -- expected): NOTHING vendored, scratch
   cleaned. The deliverable is the characterization, in the ADR:
   - The assert site and the interpreter's encounter open/close
     model around it, cited to source.
   - The exact module states involved: which encounter is open, which
     state opens the second one (walk the two failing seeds'
     trajectories; name the state path, e.g. a parent encounter
     still open when broken_jaw or SNF opens its own).
   - Upstream semantics: what upstream Synthea does with concurrent/
     overlapping encounters (probe upstream source or docs at the
     pin where feasible; label inference as inference).
   - The measured rate at both probe layers, with seeds.
   - DESIGN OPTIONS for a future fix, each with its blast radius:
     e.g. (i) auto-close the open encounter on a new open (upstream-
     faithfulness?), (ii) an encounter stack widening the subset,
     (iii) suppress the nested open with a disclosed event loss,
     (iv) module-level exclusion. No recommendation required --
     options with evidence; the ruling is the author's.
   - NOTICE's injuries dated section updated: max-steps leg closed
     (ADR-0105), nested-encounter leg now the named blocker, revisit
     trigger = a session ruling on and implementing one of the
     options.
   Commit message (ASCII only):
   `docs: injuries assessment -- nested-encounter gap characterized, vendoring still deferred (ADR-0106)`

4. **Oracle bracket.** BRANCH V: existing 34 roots byte-identical;
   new roots recorded as first baselines (both facts in the ADR).
   BRANCH C: pure identity trivially (no src/resource changes) --
   still run it. Movement outside these expectations =
   STOP-AND-REPORT.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0106
   per the branch taken (plus the [C] budget-boundary finding line);
   roadmap: the B2 row dispositioned per branch (landed, or
   re-anchored on the nested-encounter blocker with the new
   trigger); .agents/rulings.md records the 2026-08-11 "b" verbatim
   with its widened-charter framing; notes/ADRs.md index row;
   notes/adr/README.md count 103 -> 104; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- injuries B2 assessment (ADR-0106)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- BRANCH V may touch: components/sim/resources/sim/modules/ (the 4
  new members + any closure data files + NOTICE),
  components/sim/test/ (round-trip tests -- confirm the vendored-test
  home by reading a batch-4 test's path first),
  components/oracle/src/ehrt/oracle/digest.clj (first-baseline roots
  only), plus close-phase files. BRANCH C may touch: NOTICE's dated
  section and close-phase files ONLY.
- Both branches: NO interpreter/engine/loader changes -- the
  nested-encounter fix is a FUTURE session's ruling, not this one's
  improvisation. No default-config changes. No module-content edits
  ever. Scratch fetches cleaned before any commit.
- The sweep RULE governs over these lists (ADR-0099 precedent).
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims (closure membership, rates, the 99.4% arithmetic)
  are verify-then-act.

## Deviations from the driving prompt

- **The fork resolved to Branch C**, as the prompt's own arithmetic
  predicted — both probe legs (120-walk direct interpreter, a full
  300-patient `engine/run`) fired the nested-encounter assert.
  Nothing vendored; the deliverable is `notes/adr/
  0106-injuries-b2-assessment.md`'s own full characterization, per
  the driving prompt's own Branch C instructions.
- **The closure-membership count corrected**: the driving prompt's own
  inherited "4 already vendored / 4 new" (ADR-0070's own count) is
  stale — `dme/wheelchair.json` landed via a SIBLING batch (ADR-0071)
  after ADR-0070 was written. Fresh-gate re-enumeration (AR-VB4-1
  discipline, explicitly required by this prompt's own Step 2) caught
  it: the true disposition is 5 already vendored, 3 genuinely new.
  Disclosed in ADR-0106, not silently absorbed.
- **A missing ADR-0105 Done pointer, found and fixed**: `.agents/
  plans/roadmap.md`'s own Done section was missing the pointer
  ADR-0105's own execution record claims it added. A one-line,
  disclosed fix lands in this session's own close-phase commit,
  alongside this session's own new Done pointer for ADR-0106 — the
  same "transcript-witnessed is not repo-recorded" discipline this
  repo's own rulings register already names, applied to a prior
  session's own written claim.
- **An engine-layer severity finding, not previously named**: `ehrt.
  sim-engine.engine/run`'s own per-patient walk call site carries no
  try/catch around `sim-trajectory/run-module` — unlike `census.clj`'s
  own per-seed-isolated `walk-one` — so one unlucky patient among 300
  aborts the WHOLE population run. Neither ADR-0070 nor ADR-0105 named
  this (both used the census's own isolated method or a horizon
  sweep); this session's own engine-level probe (explicitly required
  by Step 2) surfaced it directly.

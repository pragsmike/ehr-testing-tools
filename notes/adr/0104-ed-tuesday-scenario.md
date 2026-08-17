## ADR-0104 — ed-tuesday: the scripted ED scenario, "A" of "C-with-A-first"

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Prior: ADR-0103 fixed the `--board` snapshot-cadence bug and chartered,
but did not execute, an ED-weighted scenario redesign — the author's
own 2026-08-10 direction (`.agents/rulings.md`, "From ADR-0103"),
verbatim: *"Maybe weight the patient population toward immediate,
emergent conditions like trauma/injuries? This would simulate an
actual ED, which is where a lot of the activity and churn would
happen."* The author's own 2026-08-10 "C-with-A-first" ruling splits
that redesign into two halves: A, a NEW sibling scenario
(`demos/scenarios/ed-tuesday/`) executed this session; B, vendoring
upstream's injuries family, a separate future batch under the standing
vendoring ceremony, not touched here. `demos/scenarios/busy-tuesday/`
stands unchanged as the population-scale contrast (its own config
landed verbatim under AR-VB2-R, `notes/ADRs.md` ADR-0071 — a ruled
artifact, not tuned).

### Decision

**Author rulings, verbatim, citing ADR-0103 (2026-08-10):**

- **"C-with-A-first."** — this session executes A only. The roadmap's
  own ED-redesign Next row (`.agents/plans/roadmap.md`) stays open,
  amended below to record A landed and B remaining.
- **ED-direction** (quoted above, in full in ADR-0103's own Decision
  section) — the scenario's own pathway-pool weighting honors it:
  quick emergent presentations dominate by weight, with admissions and
  churn visible on a `--board`.
- **Sibling-not-revision** (flagged to the author 2026-08-10,
  un-vetoed): `ed-tuesday/` is NEW; `busy-tuesday/config.edn` is
  untouched; each README gains one cross-reference line to the other.

**Tag ceremony.** The design channel verified the ADR-0103 landing at
`741b2f6` by fresh public clone. `origin/main` confirmed at `741b2f6`
at session start (`git fetch` + `git rev-parse origin/main` ==
session's own starting `HEAD`) — remote had not moved.
`stable-20260810-board-boundary-catchup` tagged annotated at `741b2f6`,
pushed, peeled ref verified: `git rev-list -n1
stable-20260810-board-boundary-catchup^{commit}` resolves exactly to
`741b2f6105e2460ca057cc89373b34fe9cc9e5b6` — exact match.

### Step 2: the population-split semantics, verified before authoring

The driving prompt's own constraint: an ordinal assigned BOTH an
admission-bearing pathway AND a module is `:incompatible-assignment`
(`components/sim/src/ehrt/sim/run.clj`'s
`incompatible-assignments`/`ordinal-guaranteed-admission-bearing?`/
`ordinal-guaranteed-module?`, lines ~114-178). Read end to end, plus
`ehrt.sim-engine.engine/assign-pathway`/`assign-module`
(lines 1165-1217) and `run`'s own docstring (lines 1316-1356), plus
`ehrt.sim-trajectory.gmf/ModuleAssignment`/`ModulesConfig` (lines
1656-1665), before authoring a single step of `config.edn`.

**The finding.** `assign-pathway` and `assign-module` draw
INDEPENDENTLY, one RNG draw per ordinal each, from their own config's
pool — `run`'s own docstring states the composition law directly: "A
patient's own compiled module content is PREPENDED onto whatever
`:pathway`/`:pathways` already queued for them, never a replacement...
A caller wanting MODULE-ONLY patients must pass an explicit empty
pathway... the DEFAULT `:pathway` otherwise still runs AFTER the
module's own compiled content, and usually conflicts with it." Two
INDEPENDENT weighted pools covering the SAME ordinal range therefore
carry a real, RNG-coincident collision risk the static
`incompatible-assignments` check cannot see: `ordinal-guaranteed-
admission-bearing?` only reports a CERTAIN conflict (every pool member
admission-bearing, or an explicit override), so a MIXED pathway pool
(some admission-bearing entries, some not) is never flagged either
way — the check is silent, not clearing, on exactly the shape a second
weighted module pool would create. Compounding this,
`ModuleAssignment`'s own pool-member schema (`[:map {:closed true}
[:module-id :string] [:weight [:or :int :double]]]`) has NO "no
module" option — `assign-module`'s own `(seq pool) (weighted-pick pool
draw :module-id)` branch means a non-empty module pool resolves EVERY
ordinal it covers to SOME real module-id, with no way to make it apply
to only a minority of patients.

**Outcome A: a sanctioned, provably conflict-free shape exists** — not
merely one the static check happens not to flag, but one where the
RNG genuinely never gets the chance to collide. List the module-tail
ordinals EXPLICITLY, in BOTH `:pathways` (an explicit `{:patient-
ordinal :pathway}` override to an empty pathway — the documented
module-only-patient pattern) and `:module-assignment` (an explicit
`{:patient-ordinal :module-id}`, never a `{:weight}` pool member).
Every ordinal NOT named in either explicit list falls through
`assign-pathway`'s weighted ED pool alone (since `module-assignment`'s
own pool is empty — only explicit entries — `assign-module` returns
`nil` for every ordinal it doesn't name, per its own `:else nil`
branch, `components/sim-engine/src/ehrt/sim_engine/engine.clj:1210-
1217`). Every named ordinal never draws from the ED pool (its own
explicit `:pathways` override always wins over the pool). The two
cohorts are disjoint by construction, not by chance — the same shape
`run_test.clj`'s own `run-command-rejects-explicit-per-ordinal-
pathway-plus-module-conflict-but-not-the-disjoint-cohort-pattern` test
already exercises for a single ordinal, generalized here to a real
population split. This is the shape `config.edn` implements; its own
header comment records this finding verbatim for a future reader.

Genuinely weighted, RNG-drawn behavior is NOT lost — it just lives
inside the ED pool (five trajectories, real weights, real draws) and,
separately, inside which of the four ambulatory modules each explicit
tail ordinal is assigned (a hand-authored round-robin, not RNG-drawn,
disclosed as such — a real limitation of the explicit-list shape, not
hidden).

### Authored scenario

`demos/scenarios/ed-tuesday/config.edn`: `:pathways` is a five-member
weighted pool — `ed-fast-track` (55, admission + short delay +
discharge), `ed-observe-and-discharge` (30, admission + delay + a
`:cbc` order + delay + discharge), `ed-admit-renal` (8, admission +
delay + transfer Renal + delay + discharge), `ed-admit-cardiology` (5,
same shape, Cardiology), `ed-trauma-imaging-admit` (2, admission +
delay + a `:cbc` order + delay + transfer Renal + delay + discharge) —
plus 8 explicit per-ordinal overrides (module-only, empty pathway) at
ordinals 6/18/30/42/54/66/78/90. `:module-assignment` explicitly
assigns those same 8 ordinals, round-robin, across
`sore_throat`/`sinusitis`/`bronchitis`/`ear_infections` (busy-tuesday's
own already-vendored quick modules). `:modules` names the same four.
`:module-horizon-days 90` (~3 months — short, per the driving prompt,
relative to busy-tuesday's 3650).

**Facility, tuned by live-probe.** `sim-model/default-facility`'s
Emergency ward ships 0 regular beds + 6 surge — with 0 beds, EVERY ED
admission boards on surge immediately (the allocation ladder's own
"surge only when full" rule is trivially satisfied at 0 beds), so
surge slot count IS the ED's real concurrent-boarding capacity. A
first draft at the default 6 surge slots, `:arrival-gap 10` (avg 5
min), 240 patients, and heavier admit-pathway weights (15/12/8)
exhausted at patient 23 (`{:status :error, :category
:capacity-exhausted, :payload {... :ward "Renal", :census {"Emergency"
{:occupied 12 :capacity 12} "Renal" {:occupied 6 :capacity 6}
"Cardiology" {:occupied 4 :capacity 6}}}}` even after doubling
Emergency's surge to 12). Re-tuned by Little's-law estimate (concurrent
occupancy ≈ arrival-rate × dwell) and re-probed: `:arrival-gap 30`
(avg 15 min), admit weights rebalanced toward a realistic ~15%
admission fraction (8/5/2), inpatient dwell shortened (720-2880 min →
240-1440 min ranges), Emergency's surge bumped to 16 (Renal/Cardiology
left at the default 4 beds + 2 surge, live-probed sufficient at this
pacing), 100 patients. This final shape ran clean — no
`:capacity-exhausted`, no `:self-check-failed` — confirmed by the
actual generate command below, not merely estimated.

**The ambulatory module tail: a disclosed, live-probed zero.** The 8
explicit module-tail ordinals produced ZERO live `:outpatient-visit`
events in the shipped run (seed 20260811, horizon 90 days). This was
tested, not assumed: the SAME 8-patient tail was re-run at
`:module-horizon-days` 14, 90, and 3650 (busy-tuesday's own horizon).
Only the 3650-day run produced any live content — exactly 1 of 8
(`:outpatient-visit`/`:outpatient-visit-end`, a paired event). The
mechanism, read from `components/sim/resources/sim/modules/
sore_throat.json`'s own `Potential_Infection` state: onset is a
monthly-`Delay`-gated `complex_transition` at roughly 0.5-1%
probability per month — most of a patient's simulated history, and
most of any short forward window, produces nothing at all. This
matches busy-tuesday's own README, which already discloses the same
low-incidence shape at population scale (68 messages from 200
patients over a 10-year horizon). Per the driving prompt's own fence
("Tuning is authorship, not code: if the board cannot show the
intended activity from config alone, report, don't patch engines"),
`:module-horizon-days` stays at 90 (short, as instructed) and the
zero is disclosed in both `config.edn`'s own header comment and the
scenario's own README, rather than inflated toward busy-tuesday's own
10-year horizon just to force a nonzero result — that would defeat the
entire point of the day-scale contrast this scenario exists to draw.

### Witnessed block (verbatim from `demos/scenarios/ed-tuesday/README.md`)

Generate: `bin/ehrt corpus generate sim --seed 20260811 --patients 100
--reference-date 2026-08-11 --churn --config demos/scenarios/
ed-tuesday/config.edn --out-dir out/scenarios/ed-tuesday`.

383 ground-truth events, 283 HL7 v2 messages, 34 board snapshots over a
128,520,000 ms (~35.7-hour) stream span. **Inpatients rise and fall:**
first snapshot 4 occupied beds, peak 21 concurrent inpatients mid-run,
drained to 3 by the final snapshot (`inpatients: 0` never held once,
unlike busy-tuesday throughout its own run — the scenario's whole
point). **Discharges accrue** 1 → 84; **churn fires** (`merged` 0 → 5,
an `InjectChurn` bed-merge event, meaningful here because real admitted
patients exist for it to touch — busy-tuesday's own outpatient-only
mix gives churn nothing to merge). **Capacity held** throughout — no
`:capacity-exhausted` anywhere in the run. **The module tail: zero live
encounters** — `active outpatients` reads 0 in every snapshot, the
disclosed finding above. Full closing summary:
`{:unparseable-count 0, :snapshot-count 34, :skip-count 0, :rate
100000.0, :idle-cap-ms 5000, :wallclock-ms 1855, :stream-span-ms
128520000, :clamped-count 0, :emitted 283, :unfolded-count 0, :sink
"ticker"}`.

### Oracle bracket

Pure identity was the prediction (a new scenario directory, two README
cross-reference lines, one Contents-list line — no `src`/`test`
namespace touched, Step 2's verification staying entirely read-only).
`bin/regression-oracle 741b2f6 51f0e68`: soundness check clean, all 34
roots' SHA-256 digests IDENTICAL between baseline and target. Matches
the prediction exactly — no STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all skip:integration`, unredirected capture): 596 occurrences of "0
failures, 0 errors" across the entire log, 0 FAIL/ERROR anywhere, exit
0, 3 minutes 54 seconds — the SAME 596 figure ADR-0103 reported,
consistent with a session that touched zero test/src namespaces.
`ehrt.docs-tooling.invocation-lint-test` confirmed green within that
same run (4 tests, 197 assertions) — this scenario's own README
generate/play commands resolve and parse under the fence-path
machinery, the "mirror AR-VB2-R's own gate pattern" instruction's
concrete check. `ehrt.cli.cli-parse-guard-lint-test` also confirmed
green (4 tests, 22 assertions). `bin/verify-nist-lock`: OK, 6
hit-nexus-sourced coordinates matched.

### Fences

Touched exactly the list the driving prompt named:
`demos/scenarios/ed-tuesday/` (new: `config.edn`, `README.md`),
`demos/scenarios/busy-tuesday/README.md` (one cross-reference line),
`demos/scenarios/README.md` (one Contents-list entry),
`notes/adr/0104-*.md`, `notes/ADRs.md`, `notes/adr/README.md`,
`.agents/*` close-phase files. No `src`/`test` change anywhere.
`busy-tuesday/config.edn` untouched, as ruled. Step 2's verification
was reading three source files, never editing them.

### Deviations, dated 2026-08-11

- **First-draft config exhausted ED capacity** (disclosed under
  Authored scenario, above) — re-tuned by calculation, then re-probed
  to confirm, before landing; not a deviation from any ruling, but
  recorded per the driving prompt's own "tuning is authorship" fence
  (report what didn't work, not just what shipped).
- **The ambulatory module tail shows zero live encounters in the
  shipped run** — disclosed above and in both `config.edn`'s header
  and the README, per the driving prompt's own named contingency for
  exactly this outcome class ("if tuning can't produce it, that is a
  finding, not a silent retune loop"). No engine/interpreter change
  made or considered; `:module-horizon-days` stays short (90) rather
  than inflated to force a nonzero result.
- **The module-tail's own module assignment is a hand-authored
  round-robin across four names, not a weighted RNG draw** — a direct,
  disclosed consequence of Step 2's own finding: the sanctioned
  conflict-free shape requires explicit per-ordinal `:module-
  assignment` entries (never a `{:weight}` pool member) for the tail,
  so there is no pool for the RNG to draw from at that layer. The ED
  pool itself stays genuinely weighted and RNG-drawn.

### Consequence

`demos/scenarios/ed-tuesday/` joins `busy-tuesday/` as the day-scale
half of the scenario library's own A/B contrast: a scripted single ED
shift with real, RNG-weighted admissions/transfers/discharges driving
visible inpatient census and real churn on a `--board`, versus
busy-tuesday's population-scale, sparse, outpatient-only incidence
mix. The population-split semantics this session verified — disjoint
explicit-ordinal cohorts, not a second weighted pool — is a reusable
finding for any future scenario mixing scripted pathways with GMF
modules, recorded here and in `config.edn`'s own header for the next
reader. The roadmap's ED-redesign row stays open, amended to record A
landed and B (injuries vendoring) still pending, under the standing
vendoring ceremony, a separate future session.

### Index line

```
- 2026-08-11 — ed-tuesday-scenario — ADR-0104
```

(appended to `.agents/plans/roadmap.md`'s own Done section; the
ED-redesign Next row amended in place, not replaced.)

`notes/adr/README.md`'s own file count corrects 101→102, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

ed-tuesday: the scripted ED scenario, "A" of "C-with-A-first" — a new sibling scenario, `demos/scenarios/ed-tuesday/`, contrasts busy-tuesday's population-scale sparse incidence with a day-scale scripted single ED shift; Step 2's own verification finds that mixing a weighted scripted-pathway pool with a weighted GMF-module pool over the SAME patients carries a real RNG-coincident `:incompatible-assignment` collision risk the static check cannot see (`ModuleAssignment`'s own pool has no "no module" option), and lands the sanctioned fix — disjoint explicit-ordinal cohorts, never a second weighted pool; five weighted ED trajectories (55/30/8/5/2) plus an 8-patient explicit ambulatory-module tail; a first-draft config exhausted ED capacity, re-tuned by calculation and re-probed clean; witnessed run: inpatients rise to a peak of 21 and fall to 3, discharges 1→84, churn merges 0→5, zero `:capacity-exhausted`; the ambulatory tail shows zero live encounters in the shipped (90-day-horizon) run, live-probed at 14/90/3650 days and disclosed as a genuine low-incidence finding, not silently retuned; the oracle holds pure identity across all 34 roots

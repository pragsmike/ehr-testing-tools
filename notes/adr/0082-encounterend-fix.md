## ADR-0082 — The EncounterEnd fix: the interpreter learns the five arms

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: `notes/adr/0081-fidelity-riders.md` opened the fidelity arc,
re-verified the design brief (`.agents/plans/2026-08-08-encounterend-
design.md`) against both upstream `synthetichealth/synthea`
`State.java` (pin `7e08387c68a7f0e21d13076609a159fd473fc902`) and the
live interpreter, and recorded three author rulings (R1 openness-only
with disclosed divergence, R2 count-and-surface, R3 predict-and-
confirm identical with any mover escalating) licensing this session.

This session executes the licensed fix — but not on the first attempt
at landing code. AR-EE-1's own blast-radius probe (required BEFORE any
fix code, per R3) found a real, nonzero mover among the 27 oracle
roots — `hypothyroidism`, already vendored, already carrying a real
dangling `:encounter-end` in its own oracle-seed walk TODAY, pre-fix.
Per R3's own bar ("any mover — predicted, before the fix lands, or
actual, after — is STOP-AND-REPORT with evidence, never a silently-
accepted movement"), the session stopped, reported the finding with
full walk evidence, and returned control to the author before writing
a line of the fix. The author's ruling on that STOP-AND-REPORT — the
trace-first requirement, the amended R3, and the ADR-0071 erratum — is
recorded verbatim below (AR-EE-1a/1b/1c) alongside the six rulings the
session's own driving prompt carried (AR-EE-0 through AR-EE-6).

Read-first (this session): the brief in full; ADR-0081 (the rulings
verbatim, the tag debt); `gmf_interpreter.clj` ~1690-1712 (the compile
dispatch), ~1209 (`index-of-last-open-encounter`, retired this
session), ~1930-1955 (the Wave H fold whose pairing discipline moves
into the walk); `census.clj`'s `walk-one` (where the suppressed-end
counter surfaces); ADR-0071/0072's own deferral sections (the incident
record); `docs/gmf-interpreter.md`'s section conventions.

### Decision

Author rulings, recorded verbatim. `[A]` author-ruled, `[C]` channel-
inferred.

1. **AR-EE-0** `[A — tag law, case (ii)]`. `stable-20260808-fidelity-
   riders` annotated and pushed at `c2bcb6737b4b72431414f16abeae39367
   147ece9` (ADR-0081's own closing tip — the commit that landed that
   ADR's own file, `c2bcb67`, confirmed HEAD at session start); peeled
   ref verified both locally and via `git ls-remote origin refs/tags/
   stable-20260808-fidelity-riders^{}`, both resolving exactly.
   **Executed Step 0.**

2. **AR-EE-1** `[A — R3, the prediction FIRST]`. For each of the 27
   oracle roots (`components/oracle/src/ehrt/oracle/digest.clj`'s own
   `roots` map), the root's own recorded seed/config was walked through
   the CURRENT (pre-fix) interpreter and every `:encounter-end`
   emission counted as unmatched when its own `:references` was `nil`
   OR already closed by an earlier `:encounter-end` in the same
   trajectory. **The prediction table:**

   | root | walks | unmatched | predicted |
   |---|---|---|---|
   | allergic-rhinitis | 3000 | 0 | identical |
   | appendicitis | 200 | 0 | identical |
   | asthma | 300 | 0 | identical |
   | attention-deficit-disorder | 300 | 0 | identical |
   | bronchitis | 300 | 0 | identical |
   | death-fixture | 200 | 0 | identical |
   | dementia | 300 | 0 | identical |
   | dermatitis | 300 | 0 | identical |
   | ear-infections | 200 | 0 | identical |
   | ear-infections-engine | 300 | 0 | identical |
   | ear-infections-history-engine | 300 | 0 | identical |
   | fibromyalgia | 300 | 0 | identical |
   | **hypothyroidism** | 300 | **5** | **MOVER (STOP-AND-REPORT)** |
   | med-rec | 300 | 0 | identical |
   | metabolic-syndrome-care | 300 | 0 | identical |
   | osteoarthritis | 300 | 0 | identical |
   | osteoporosis | 300 | 0 | identical |
   | rheumatoid-arthritis | 300 | 0 | identical |
   | sepsis | 500 | 0 | identical |
   | sinusitis | 30 | 0 | identical |
   | sleep-apnea | 300 | 0 | identical |
   | sore-throat | 200 | 0 | identical |
   | total-joint-replacement-engine | 300 | 0 | identical |
   | urinary-tract-infections-engine | 300 | 0 | identical |
   | urinary-tract-infections-history-engine | 300 | 0 | identical |
   | vhd-pulmonic | 300 | 0 | identical |
   | vhd-tricuspid | 300 | 0 | identical |

   26 of 27 roots predicted zero unmatched, exactly as the brief
   expected. `hypothyroidism` did not — 5 of its own 300 oracle-seed
   walks (walks #72, #85, #189, #200, #234) each contain one
   `:encounter-end` whose own reference was already consumed by an
   earlier `:encounter-end` (walks #85/#189/#234) or `nil` (none in
   this exact seed's own 5, though the incident record's own original
   cross-module case IS the `nil` shape — see AR-EE-1a). Evidence
   (walk #72): `anemia/anemia_sub`'s own `end-any-active-encounter-
   just-in-case` state fires an `:encounter-end` with `:references
   nil` immediately after `hypothyroidism`'s own second encounter had
   ALREADY closed properly — the exact dangling-reference idiom
   ADR-0071/0072 diagnosed, now confirmed reachable inside a module
   this repo has vendored and shipped since 2026-08-07. **Session
   STOPPED here, before any fix code, per R3's own bar. No commit.**

3. **AR-EE-1a** `[A — the trace first]`. Before any fix code: walk
   #72's own shape traced through `compile-trajectory` → `engine/run`
   → `emit-hl7/emit` against the CURRENT (pre-fix) tree, in-session.
   **Finding: today's 300-patient `hypothyroidism` round trip passes
   (`check/check-all` returns `:ok`) DESPITE the dangling end — a
   silent-drop absorption, not a coincidence, and not something this
   fix needs to touch separately.** Two absorption layers, both
   PRE-EXISTING and unrelated in origin to the EncounterEnd gap:
   - **3 of the 5 dangling walks (#85/#189/#234) are entirely
     pre-horizon** — every event in the whole span, including the
     encounters themselves, predates `registration-t`
     (`sim-model/reference-today-epoch-day`, epoch-day 20089 at this
     seed) and is dropped by `compile-trajectory`'s own `:pre-horizon`
     gate (`pre-horizon-dropped-types` includes `:encounter-end`)
     before it ever reaches `encounter-end->step`.
   - **2 of the 5 (#72, #200) are in-horizon**, and are absorbed by a
     DIFFERENT, also pre-existing rule: `compile-trajectory`'s own
     `encounter-closed?` short-circuit (sim/ADR-0007 point 3's own
     single-encounter-per-run scope — "compile through the end of the
     FIRST horizon-phase encounter, then stop"). `hypothyroidism`'s
     own FIRST encounter compiles cleanly (`:outpatient-visit` → two
     observations → `:outpatient-visit-end`), and `encounter-closed?`
     flips `true` at that point — dropping EVERYTHING after it
     unconditionally, dangling or not: the module's own SECOND
     encounter, the dangling `:encounter-end` itself, and
     `anemia/anemia_sub`'s own real (properly-matched) encounter and
     its content. The full 300-patient ground-truth histogram for this
     root is `{:registered 300, :outpatient-visit 4, :observation 8,
     :outpatient-visit-end 4}` — confirming almost the entire closure's
     own content, dangling-end walks included, never reaches ground
     truth at all, by a rule that has nothing to do with EncounterEnd
     openness.

   This is the D4-family absorbed-error shape the ruling named: an
   existing, ratified truncation mechanism happens to swallow the
   dangling event as a side effect of swallowing everything else past
   the walk's own first encounter close — not because it validates or
   recognizes the reference. The arm dispatch (AR-EE-2, below) makes
   the INPUT to that absorption disappear at the source (A5 suppresses
   rather than emits); the absorption mechanism itself is untouched,
   ratified, and out of this session's own scope.

4. **AR-EE-1b** `[A — R3 amended, the mover licensed]`. R3's bar
   amends to: **26 roots predicted-and-confirmed identical +
   `hypothyroidism` a PREDICTED, EVIDENCED, LICENSED mover** — a
   correction, not a regression (its pre-fix digests freeze
   trajectories containing events upstream semantics forbid). Any root
   OTHER than `hypothyroidism` moving remains STOP-AND-ESCALATE — the
   license names one mover, not a class. Requirements riding the
   license, and their outcomes (AR-EE-4, below): before/after digests
   recorded side by side; the round-trip test re-witnessed green;
   post-fix walks showing `:suppressed-encounter-ends` at the
   predicted counts; no other root moving.

5. **AR-EE-1c** `[C — the erratum]`. ADR-0071's own "[the shared
   `anemia/anemia_sub.json` submodule stays vendored via
   `hypothyroidism.json`'s closure,] confirmed clean there at 3000
   patients — its own call path never reaches the hazardous state" is
   CORRECTED, append-not-erase, by this dated note: that 3000-patient
   check verified CONTENT PRODUCTION (`:substance :produces-content`,
   the census-substance artifact's own metric), not end-matching — a
   different property than the one later cited to mean "this call path
   is safe." This session's own oracle-seed probe (300 patients, seed
   20260802) found the hazardous state reachable at 5/300 (~1.7%),
   confirmed above (AR-EE-1). Pointer left in `notes/adr/0071-
   vendoring-batch-2.md` at the corrected sentence.

6. **AR-EE-2/AR-EE-3** `[A — the ruled design, executed]`. Landed
   `dad2553`, full diff below (Fix, below). Per the brief: (i) walk-
   state openness tracking via a pure fold (`open-encounter-index`,
   NOT threaded ctx state — recomputed from `ctx`'s own trajectory on
   demand, the same O(n) cost class the retired function already had);
   `:suppressed-encounter-ends` IS threaded (a plain integer cannot be
   recovered post-hoc, since A5's whole point is that no event exists
   to recount from); (ii) the compile dispatch — A1 open → emit
   referencing the TRACKED index; A5 none-open → NO EVENT, ordinary
   transition, counter incremented (R2); wellness arms by openness
   alone (R1, `wellness-wait-step` shares the SAME tracking site since
   it mints raw `:encounter` events too); A4 disclosed unreachable
   (unchanged prose, one-module-per-patient); (iii)
   `index-of-last-open-encounter` RETIRED — deleted, fresh grep
   confirms zero remaining call sites (only its own name inside the
   new function's docstring, a historical citation); (iv) the counter
   surfaces in `census.clj`'s `walk-one` (`:suppressed-encounter-ends`
   when nonzero) — the sole existing "round-trip metadata" surface
   this repo has for per-walk stats (a fresh search found no other).

7. **AR-EE-4/AR-EE-5** `[A — R3 confirmed under the amended bar, C —
   the paper trail]`. Landed `deabbbd`. See Confirmation, below.

8. **AR-EE-6** `[C — scope]` (fences). Held: `src/` edits landed only
   in `gmf_interpreter.clj` and `census.clj`; new test files and one
   new fixture; docs sections in `docs/gmf-interpreter.md`. NO
   vendoring (anemia/colorectal stay unvendored — the payoff rider is
   next). NO engine/emitter/player changes (`compile-trajectory`,
   `engine.clj`, `emit-hl7` all read, none edited). One tempting fix
   found mid-move — `colorectal_cancer.json`'s own SEPARATE, newly
   discovered defect (below) — recorded as a finding, not acted on, per
   this fence and AR-P-4's own law. Standing untracked files untouched.

### Fix

`components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj`:
`open-encounter-index` (new, pure fold, replaces `index-of-last-open-
encounter`); `initial-context` seeds `:suppressed-encounter-ends 0`;
`pass-through-outcome`/`blocked-outcome`/the `:terminal` case/`death-
step`/`step-safely`'s catch clause all thread `:suppressed-encounter-
ends` through unchanged (the full-value-passthrough shape `:attributes`/
`:vital-signs` already establish); `walk-module`/`run-submodule`/
`call-submodule-step`/`run-module`'s own FOUR independent ctx-folding
loops all updated to fold the new field back (a called submodule's own
suppression, e.g., must reach the caller's own walk); the `:encounter`
case and `wellness-wait-step` both gain the "one in-flight encounter"
assert (`open-encounter-index` called before minting a new one); the
`:encounter-end` case branches on `open-encounter-index`: A1 emits
referencing the tracked index, A5 bumps the counter and no-ops.
`components/sim-trajectory/src/ehrt/sim_trajectory/census.clj`:
`walk-one` additively carries `:suppressed-encounter-ends` when
nonzero. `docs/gmf-interpreter.md`: a dated resolution note on section
7 item 3 (the original M5a decision this session supersedes), naming
the five-arm collapse and R1's own disclosed wellness divergence.

**Red, witnessed in-session, not committed:** `components/sim-
trajectory/test/ehrt/sim/fixtures/encounter-end-fixture.json` (new,
hand-authored, no NOTICE obligation) — `Open` (Encounter) → `Close`
(EncounterEnd, matched) → `Close_Again` (EncounterEnd, the idiom
itself, nothing open) → `Terminal`. Run against the git-`HEAD`
(pre-fix) `gmf_interpreter.clj`, loaded into an isolated namespace
without touching the working tree: `Close_Again` compiled to a SECOND
`:encounter-end` referencing index `0` — the SAME index `Close` already
consumed (the retired function's own module-filtered "last `:encounter`
event" logic, blind to the fact it was already closed) — not the `nil`
the fixture's first draft assumed, but the same dangling-reference
family AR-EE-1's own definition names ("already has a matching earlier
end"). **Green, committed:** the same fixture post-fix — exactly one
`:encounter-end` reaches the trajectory, referencing index `0`;
`:suppressed-encounter-ends` = 1. Two further committed unit tests:
open→close→open→close (each end references its OWN open, never the
stale one) and the nesting assert (`AssertionError` on a second
`:encounter` while one is open). One pre-existing test fixture,
`wellness-wait-act-loop-module` (`gmf_interpreter_test.clj`), was found
to never close its own wellness encounter between loop iterations — an
inaccuracy in the fixture the new assert caught, corrected to add the
missing `:encounter-end` state (still zero Delay in the loop body, the
property the test actually exercises).

Full suite: 511 assertions (baseline) → 521 assertions (198 tests, +3
tests / +10 assertions, this session's own new coverage), 0
failures/0 errors throughout.

### In-session proof: anemia and colorectal (AR-EE-3)

Both loaded from `/home/mg/synthea-checkout` (pin-verified,
`7e08387c68a7f0e21d13076609a159fd473fc902`), population scale, the
same seeds ADR-0071/0072 recorded, via `engine/run` + `check/check-
all` (the same round-trip shape every vendored module's own committed
test already uses) — the pre-fix run via an isolated load of git
`HEAD`'s own `gmf_interpreter.clj`, intercepted with `with-redefs` at
the `ehrt.sim-trajectory.interface/run-module` boundary (arity-
compatible, zero working-tree disturbance), never a stash.

**`anemia___unknown_etiology.json`** (`:persona-config` race-weighted,
per ADR-0071's own fix note — its `Initial` state is Race-gated):

| seed | PRE-FIX violations | POST-FIX violations |
|---|---|---|
| 20260802 | 14 | **0** |
| 1 | 18 | **0** |
| 42 | 11 | **0** |

Fully extinguished at all three seeds — the same qualitative shape
ADR-0071 recorded (12/17/6; this session's own exact counts differ
slightly, plausibly the invariant catalog's own evolution since
2026-08-07, not a methodology gap — the setup was verified: race-
weighting confirmed necessary and present, the SAME `check/check-all`
call every other root's own committed test uses). CLEAN and ready for
its own vendoring rider.

**`colorectal_cancer.json`** (no `:persona-config` override needed —
its own `Initial` state is not Race-gated; confirmed by inspection):

| seed | PRE-FIX violations | POST-FIX violations |
|---|---|---|
| 20260802 | 4 | **4 — unchanged** |
| 1 | 0 | **0** |
| 42 | 4 | **4 — unchanged** |

Matches ADR-0072's own exact record (4/0/4) — and **this fix has ZERO
effect on it.** A raw-trajectory scan (the same pure fold the fix
itself uses) over all 300 seed-42 walks found ZERO dangling
`:encounter-end` references anywhere, post-fix — the interpreter-level
gap this session fixes is genuinely absent from `colorectal_cancer.
json`'s own walks at this population. The residual violations are a
SEPARATE, previously-undiagnosed defect: `check/check-all` at seed 42
reports `{:clinical-content-only-when-admitted 19, :discharge-follows-
admission 1}`; at seed 20260802, `{:discharge-follows-admission 1,
:clinical-content-only-when-admitted 3}` — clinical content compiling
or replaying as though outside an open encounter, one layer downstream
of the interpreter (`compile-trajectory` or the engine itself, not yet
localized). **Not fixed this session** (AR-EE-6's own fence, AR-P-4's
law: a tempting fix found mid-move is a finding) — recorded here and
in the roadmap's own Deferred row as colorectal's own new, narrower
blocker. The payoff rider (anemia + colorectal as a mini-batch) named
in the brief is revised by this finding: **anemia is ready; colorectal
is not, on a different gap than the one this session closes.**

### Confirmation (AR-EE-4)

- **Oracle bracket:** `bin/regression-oracle c2bcb67 dad2553
  --declared-digest-change`: **IDENTICAL — all 27 roots, including
  `hypothyroidism`, byte-for-byte unchanged.** This is a STRONGER
  result than AR-EE-1b's own license anticipated, not a weaker one:
  AR-EE-1a's own trace already explains why — `compile-trajectory`'s
  pre-existing single-encounter-scope truncation drops the SAME
  content (dangling reference or not) either way for this specific
  oracle seed/population, so the fix's own effect is invisible at the
  engine/ground-truth granularity here, though real (see below) at the
  raw-walk granularity the fix directly touches. `hypothyroidism`'s own
  digest, both sides: `b98916600fe1faabf766ed9be168ab1dc6337e99a9d28f16
  19287a589a4a1c95` (engine `{:ground-truth :hl7}` pair, seed 20260802,
  300 patients) — hand-verified independently of the harness, matching.
- **Raw per-patient trajectory digest for `hypothyroidism`** (the same
  seed/population, `sim256` over the captured 300-walk vector, hand-
  computed, NOT the oracle's own claim): PRE-FIX
  `595ef653e38c504bae816425436df8fb7a900138d783cd59b2d9aba2eb0bbef8` →
  POST-FIX
  `4df9061592c97d7d4aa0c6a9e366c15f837fb22e83feca521d94d2ce42654019` —
  DIFFERENT, exactly as licensed: this is where the mover actually
  lives.
- **`hypothyroidism`'s own committed round-trip test**
  (`ehrt.sim-emit-hl7.vendored-hypothyroidism-test`,
  `engine-run-completes-real-hypothyroidism-closure-content`):
  re-witnessed GREEN post-fix, part of the full-suite run above (seed
  20260802, 300 patients, full invariant catalog).
- **Labeled census re-run** (`encounterend`,
  `components/sim-trajectory/docs/census/2026-08-08-synthea-7e08387-
  encounterend.edn`, against the pin-verified checkout): 85 modules
  censused, `{:ok-walked 84, :out-of-scope-by-ruling 1}` — parity holds
  exactly (84/85 + the one ruled-exclusion module, matching every prior
  census's own shape). Diffed module-by-module (`:verdict`/`:substance`/
  `:event-counts`) against the prior artifact (`2026-08-07-synthea-
  7e08387-substance.edn`): **exactly ONE row changed** —
  `anemia-unknown-etiology`'s own middle census seed drops from 10 to 9
  events (the exact, minimal, fully-explained effect of one suppressed
  `:encounter-end` no longer emitting) — every other one of the 85 rows
  byte-identical, none silently absorbed. `:suppressed-encounter-ends`
  appears as new census data: `anemia-unknown-etiology`'s own middle
  seed carries `:suppressed-encounter-ends 1`.
- `clojure -M:poly check`: OK, every commit this session.
- Full suite: 521 assertions, 0 failures/0 errors (post-fix, every run
  from `dad2553` forward).
- `gitleaks git --staged -v`: clean before both commits; the pre-push
  hook's own re-scan clean on both pushes.
- Post-push message verification: `dad2553` one delta (the known
  trailing-blank-line artifact); `deabbbd` the same, single delta.
- CI: `dad2553`'s own push watched — run `31258259066`, success,
  3m30s. Last-five on `main` at session start all green (`c2bcb67`,
  `9eb7da9` [sic, see ADR-0081's own last-five], `9eb7da9`/`c9c3b3f`/
  the scheduled Integration run/`8eeafb2` per ADR-0081's own disclosure
  — this session's own preflight re-confirmed the SAME five plus
  `c2bcb67` and `6cb4627`, all green, no red window).

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260808-encounterend-fix` at THIS session's own closing tip**
— the same tag-law case (ii) pattern every prior close in this repo
has used for its own predecessor.

### Index line

```
- 2026-08-08 — encounterend-fix — ADR-0082
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 79→80, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

The interpreter's own first semantics change since the GMF coverage
waves lands, predicted before it moved anything and proven after —
`hypothyroidism`'s own real (if downstream-invisible) correction
recorded rather than silently absorbed twice over, and a second,
unrelated defect in `colorectal_cancer.json` caught as a byproduct
rather than left for the payoff rider to trip over unexplained.
Untouched, carried forward from ADR-0081's own horizon note: the
pairing-as-data registry session, Wave E's risk-attribute/vital-sign
register, vendoring batch 4 (the veteran family), the census closure-
count refinement, publish-prep (F-5/F-6 + F-7), review 2, `sim-emit-
cda`, the fixture-relocation and ADR-footnote Next rows (still unruled/
un-prerequisite-inventoried). **What DOES change:** the payoff rider
narrows — `anemia___unknown_etiology.json` vendors clean, on schedule;
`colorectal_cancer.json` waits on its own new diagnosis
(`:clinical-content-only-when-admitted`, downstream of the
interpreter) before it can join.

### Consequence

The EncounterEnd gap closes structurally, not by patching the one
symptom two vendoring sessions happened to trip over: real openness
tracking replaces a module-filtered guess, upstream's own five arms
collapse to the two this subset's one-module-per-walk scope actually
needs, and the suppression the old code silently manufactured a
dangling event for is now a counted, zero-cost fact. The predict-
then-confirm protocol did its job twice over in one session — once
catching a real, already-shipped defect before any code moved (the
STOP-AND-REPORT), and once catching that the licensed correction, once
made, turned out invisible at the oracle's own granularity for the
reason AR-EE-1a's trace names precisely rather than leaves mysterious.

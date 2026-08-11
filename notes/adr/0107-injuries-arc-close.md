## ADR-0107 — Injuries arc close: auto-close on reopen lands, the batch vendors, both deferral legs closed

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Prior: ADR-0070 (2026-08-07) deferred `injuries.json`'s own eight-file
closure WHOLE on a `run-submodule exceeded max-steps` defect.
ADR-0105 (2026-08-11, B1) closed that defect. ADR-0106 (2026-08-11,
B2) re-gated the closure fresh under a widened, assessment-first
charter and found ADR-0105's own fix complete but a SEPARATE,
pre-existing gap firing: `step`'s own `nested :encounter` assert,
2 of 120 well-mixed-seed walks at the direct-interpreter layer, and a
full 300-patient `engine/run` throwing UNCAUGHT. ADR-0106 recorded four
design options, no recommendation, and left the ruling to the author.

This session (B3, 2026-08-11, the author's own verbatim "Let's do
(i)." — `.agents/rulings.md`, "From ADR-0107") executes option (i) —
auto-close on reopen, matching upstream exactly — as PHASE 1, then, ON
ITS GREEN, lands the injuries vendoring batch itself as PHASE 2, per
the B3 framing the author accepted. Both phases landed green; the arc
that opened at ADR-0070 is now fully closed.

### Tag ceremony

Design channel verified the ADR-0106 landing (`fdb3984`) by fresh
public clone. `stable-20260811-injuries-b2-assessment` tagged annotated
at `fdb3984`, message "injuries B2 assessment landed, design-channel-
verified 2026-08-11 (ADR-0106)"; pushed; peeled ref confirmed
`fdb3984bf17a53e9fa660742ae278686c6b07ef7` — exact match; remote had
not moved (`origin/main` was already at `fdb3984` at session start,
confirmed by `git fetch` before tagging).

### Phase 1 — the interpreter fix (option (i), executed verbatim)

`components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj`,
`step`'s own `:encounter` case (~1823, pre-fix): the assert
`(nil? (open-encounter-index (:trajectory ctx)))` becomes a conditional
auto-close. When `open-encounter-index` is non-nil, the case now
synthesizes an implicit `:encounter-end` event for the stale open
FIRST — `(trajectory-event module-id ctx :encounter-end {:references
open-idx})`, built from the SAME `ctx` the new `:encounter` event will
also be built from (no time has advanced yet at this point in `step`,
so the synthesized end's own `:t` is byte-identical to the new
encounter's own `:t` — end-before-open at the SAME instant, matching
`State.java`'s own `Encounter.process` same-module-reopen branch,
source-cited by ADR-0106) — then emits the new `:encounter` against a
ctx whose trajectory already carries that close. Both events land in
the outcome's own `:events` vector, end first, so `walk-module`'s/
`run-module`'s own fold appends them to the real trajectory in the
correct order.

**The invariant `mark-phase`/the runtime assert both already assumed
(one in-flight encounter) is now satisfied BY CONSTRUCTION, not merely
by every vendored closure's own good authoring** — there is still
never more than one encounter open at once, the auto-close guarantees
it. `mark-phase` itself (the Wave H straddle-phase fold, ADR-0042/
ADR-0086) is UNTOUCHED, per the driving prompt's own explicit
instruction — only its docstring gained a dated note stating the new
guarantee; its own fold correctly closes the stale phase (the
synthesized end, first in `:events`) before opening the new one (the
reopening `:encounter`, second), since both land in the SAME outcome's
`:events` list, in that order.

**The `:encounter-end`-with-nothing-open path
(`:suppressed-encounter-ends`, ADR-0082 R2) is unchanged** — a
structurally different case (`step`'s own `:encounter-end` dispatch
arm), never touched by this fix. `wellness-wait-step`'s own SEPARATE
nesting assert (the "(wellness)" variant, a different mint site) is
also UNCHANGED, a disclosed, narrower scope decision: ADR-0106's own
full-graph sweep found the hazard only in the ordinary `:encounter`
case dispatch, and the driving prompt's own Context names "the
`:encounter` case's own assert" specifically, not the wellness one.

**The counter decision.** A new `:synthesized-encounter-ends` field
was added, mirroring `:suppressed-encounter-ends`'s own zero-cost-
countable precedent exactly — threaded as a full-value passthrough
through `initial-context`, `pass-through-outcome`, `blocked-outcome`,
`run-submodule`'s own ctx fold, `call-submodule-step`'s own `base` map,
`death-step`, `step`'s own `:terminal` case, `step-safely`'s own
honest-absence catch, `walk-module`'s own ctx fold, and `run-module`'s
own ctx fold — every site `:suppressed-encounter-ends` already touches,
none skipped. Chosen per the driving prompt's own explicit instruction
("if you add it, co-land its assertion; if not, say why") — added,
because a countable witness is exactly what phase 2's own named-
regression requirement needed, and the marginal cost of mirroring an
already-proven passthrough pattern is small. Asserted at three
altitudes: the hermetic fixture test (below), the phase-2 named
regression test against the real vendored closure, and a pinned
population-scale count at the engine-round-trip layer (4 across 300
patients, seed 20260802).

### Phase 1 red, reproduced hermetically

`gmf_interpreter_test.clj`'s own pre-existing
`nested-encounter-asserts-rather-than-silently-nesting` test — a
hand-built `nesting-module` fixture, `Encounter -> Encounter` with NO
`EncounterEnd` between them, BYTE-IDENTICAL in shape to `injuries.
json`'s own `Spinal_Injury_Treatment_Encounter` reopen — asserted
`thrown? AssertionError` pre-fix. Renamed and rewritten as
`nested-encounter-auto-closes-the-stale-one-rather-than-throwing`,
asserting the DESIRED post-fix behavior instead (`:status
:horizon-complete`, one synthesized `:encounter-end` present,
correctly referencing the stale open's own index, timestamped at the
new encounter's own `:t`, `:synthesized-encounter-ends` = 1) — this is
the SAME fixture, red under the OLD assertion pre-fix (it would have
thrown `AssertionError` against the new assertions' own expectations
too, since pre-fix the walk itself throws), green post-fix. Verified
directly this session by a `git stash` of the source file alone (test
and doc changes kept): pre-fix, the walk throws `AssertionError` with
the exact pre-existing message; post-fix, restored, it passes.

**The real, pinned closure's own two ADR-0106-recorded failing seeds**
(mixer-seed 20260803, census parameters: registration age 30, 50-year
horizon, `default-persona-config`), re-probed this session via a
SCRATCH script (never committed, cleaned before this record's own
commit) against the pinned checkout (`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`, working tree clean,
re-confirmed):

- **Pre-fix** (`git stash` of the interpreter source alone): both seeds
  (`-576131918266266247`/`-5690589783821964774`) throw the identical
  `AssertionError` ADR-0106 recorded, verbatim.
- **Post-fix**: both seeds complete, `:status :horizon-complete`, each
  with `:synthesized-encounter-ends` = 1, `:suppressed-encounter-ends`
  = 0.

The zero-open suppressed-end tests (`close-if-open-idiom-emits-once-
and-suppresses-the-second-close`, `open-close-open-close-sequence-
tracks-each-encounter-independently`) pass UNMODIFIED except a new
companion assertion (`:synthesized-encounter-ends` = 0 on the latter,
proving zero-cost passthrough when the auto-close arm never fires) —
no existing assertion edited.

`components/sim-trajectory/docs/gmf-interpreter.md` gains a dated
resolution note on §7 item 3 (the SAME item the EncounterEnd fix's own
dated resolution, ADR-0082, already lands on) — the auto-close's own
timing, the citation shape, the `mark-phase` satisfied-by-construction
claim, and the disclosed `wellness-wait-step` scope decision.

**Interpreter test namespace, full run**: 203 tests, 535 assertions
(up from 527 pre-session — the rewritten test's own 8 assertions
replacing the old test's 1, plus 1 new companion assertion), 0
failures, 0 errors.

Fix commit: `7db2044` — "fix: nested encounter open auto-closes the
stale one, upstream-faithful (ADR-0107)." Pushed; post-push
verification: one delta against the message file, the known harmless
trailing-newline artifact.

### Phase 1 gate

- Interpreter test namespace: green (above).
- 120-seed scratch probe (ADR-0105's own method, mixer-seed 20260803,
  census parameters): 0 of 120 walks throw, either `max-steps` or
  `nested :encounter` — matching the full closure of both deferral
  legs.
- Oracle bracket leg 1, `bin/regression-oracle fdb3984 7db2044`:
  `IDENTICAL: every root's digest matches between fdb3984 and
  7db2044` — all 34 pre-existing roots byte-identical, matching the
  driving prompt's own pre-analysis exactly (no currently vendored
  module contains the auto-close hazard shape; `digest.clj` itself
  untouched this phase, so no `--declared-digest-change` flag needed).

Gate green; phase 2 proceeds.

### Phase 2 — the batch lands

**Closure disposition, re-verified this session (ADR-0106's own
correction, not re-derived from scratch): 5 already vendored, 3
genuinely new.** All 5 already-vendored members (`medications/
ear_infection_antibiotic.json`, `medications/otc_pain_reliever.json`,
`medications/moderate_opioid_pain_reliever.json`, `dme/
wheelchair_end.json`, `dme/wheelchair.json`) re-verified byte-identical
against the pin (`diff -q`, zero deltas) — NOT re-vendored, no new
NOTICE rows. The 3 new members (`injuries.json`, `injuries/
broken_jaw.json`, `snf/skilled_nursing_facility.json`) vendored
byte-verbatim from the pinned checkout, SHA-256 recorded fresh:

| File | SHA-256 |
|---|---|
| `injuries.json` | `d772845bf1420a0bd582e510e30e8411adbd3d08c322efbaac349fbf4eaf74d5` |
| `injuries/broken_jaw.json` | `55e2c7eedfdfe02eca1044ac4d7069a5d9025fb2cd5eac7427eaf67de2e81f0c` |
| `snf/skilled_nursing_facility.json` | `0631ed8af2fbe1905e4f67b58bebc7a26aab58930074c226ecc70814d91ac987` |

`.gitattributes`' own `components/sim/resources/sim/modules/** -text`
rule confirmed by grep to already cover the new `injuries/`/`snf/`
subdirectories. `NOTICE`'s own injuries dated section gains a closing
2026-08-11 amendment: both legs closed, the true disposition restated,
the counter's population-scale pin, the horizon-value finding (below).

**Attribute-gate check (per the driving prompt's own instruction).**
`injuries.json`'s own `Initial` state direct-transitions to
`Wait_For_Injury` — no attribute gate, matching ADR-0106's own
finding, re-confirmed by every test below running with NO
`:initial-attributes` seed. `broken_jaw.json`'s own `dental_referral`
never-cleared gate (ADR-0070's original bail-out cause) is HANDLED by
ADR-0105's horizon truncation — re-confirmed: 0 of 120 walks throw
`max-steps` at that branch at the census's own 50-year horizon.

**Round-trip tests, red-before-resource.** Both new test files
(`components/sim-trajectory/test/ehrt/sim_trajectory/
vendored_injuries_test.clj`, the interpreter-layer home mirroring
`vendored_ear_infections_test.clj`'s own shape; `components/
sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_injuries_test.clj`, the
engine-layer home mirroring a batch-4 test's own shape) were verified
red BEFORE the vendored resources: a `git stash push` of the three
resource files alone reproduced `IllegalArgumentException: Cannot open
<nil> as a Reader` (via a `Syntax error macroexpanding` at the
`slurp (io/resource ...)` call site) on both new namespaces; `git
stash pop` restored the resources and both suites went green.

**Interpreter-layer suite** (`vendored-injuries-test`): closure
loads clean (all 8 modules present by name); a 200-case `defspec`
walks bounded-horizon-without-throwing at census parameters
(registration age 30, 50-year horizon); whole-walk determinism; and
the named regression test (below). 4 tests, 8 assertions, 0/0.

**Engine-layer suite** (`vendored-injuries-test`): real compiled
clinical content (`:encounter`/`:encounter-end`/`:condition-onset`/
etc.) across 300 patients, the full `check.clj` invariant catalog
holds, real HL7 renders. A SECOND deftest pins
`:synthesized-encounter-ends` at 4 across the SAME 300-patient
population (seed 20260802), intercepted at the `sim-trajectory/
run-module` call boundary itself (the same "intercept the real
caller" technique the colorectal/veteran-prostate-cancer payoffs
already established one layer downstream, applied here one layer up,
since this counter lives on `run-module`'s own return ctx, not on
`compile-trajectory`'s) — the auto-close firing for real, at
population scale, not merely in the two named seeds. 2 tests, 5
assertions, 0/0.

**Disclosed deviation: `:module-horizon-days` is 18250 (50 years), not
the 36500-day (100-year) convention most engine-layer roots use.**
Found live, not assumed: `engine/run` at 36500 days throws
`run-submodule exceeded max-steps` at `broken_jaw.json`'s own `Wait
for Dental Visit` branch — exactly ADR-0106's own dated finding [C]
predicted (mean ~9124 cycles to cross 100 years at ~2 steps/cycle,
over the interpreter's 10000-step budget; mean ~4562 cycles at 50
years, safely under it, "unreachable... by concentration, not by
design" at the shorter horizon). The engine-layer round-trip test, the
interpreter-layer walk-result helper, and `digest.clj`'s own new
`injuries-pair` all use 18250 days, matching `census.clj`'s own
`default-horizon-years` and every probe this arc has ever run this
closure at (ADR-0070/ADR-0105/ADR-0106). This is a real, disclosed
boundary this session's own probe hit, not a defect ADR-0107 needed
to (or should) fix — `max-steps`'s own budget is a deliberate
backstop, not a promise every horizon is safe for every module.

**The arc's own closing witness.** A NEW named regression test,
`nested-encounter-regression-adr-0106-spinal-injury-seed-now-
completes` (interpreter layer), re-walks ADR-0106's own seed
`-576131918266266247` against the NOW-REAL vendored closure (not a
synthetic fixture): the walk completes, `:status :horizon-complete`,
both the ED-visit and treatment encounters reach the trajectory,
`:synthesized-encounter-ends` = 1, and the synthesized end references
the FIRST encounter's own trajectory index — not asserted by
construction, verified against the real authored module content.

**Oracle root: `injuries-pair`, FIRST BASELINE.** `components/oracle/
src/ehrt/oracle/digest.clj` gains one new producer function
(mirroring `ear-infections-engine-pair`'s own closure-loading shape,
the closest structural precedent — root plus called submodules, no
lookup tables) and one new `roots` map entry, purely additive — every
existing producer function and root entry byte-unchanged (confirmed
by diff before staging). The new module is AVAILABLE (present in
`resources/sim/modules/`, an oracle root, a round-trip test target),
NOT DEFAULT — no scenario or demo config assigns it.

Commit: `29392cd` — "feat: injuries closure vendored -- both
deferral legs closed, arc complete (ADR-0107)." Pushed; post-push
verification: one delta against the message file, the known harmless
trailing-newline artifact.

### Oracle bracket leg 2

`bin/regression-oracle 7db2044 29392cd --declared-digest-change`:
`DIFFERS` (expected, `digest.clj`'s own body changed outside its `(ns
...)` form, purely additively) — the diff shows exactly ONE added line
(`injuries.edn`) and ZERO removed or changed lines among the 34
pre-existing roots. Composed with leg 1's own IDENTICAL result, every
one of the 34 roots that existed at `fdb3984` stays byte-identical
all the way through `29392cd`; `injuries` is the 35th root, a first
baseline with no "before" to compare against.

### Full gate

`clojure -M:poly check`: OK (confirmed after both phase commits and
again after every close-phase edit). `clojure -M:poly test :all
skip:integration`, FINAL run (all close-phase edits landed first,
including the reading-set-budget re-baseline and the state.md
staleness-tripwire fix, both below): zero `FAIL`/`ERROR` occurrences
anywhere in the entire output, 604 occurrences of "0 failures, 0
errors" across every project block, 4 minutes 4 seconds. An INTERIM
run mid-close (before those two fixes landed) surfaced exactly the
two expected reds — `reading-set-budget-test` and `state-staleness-
tripwire-test` — nothing else, confirming both were the ONLY
consequences of this session's own close-phase register growth.
`ehrt.cli.cli-parse-guard-lint-test`: 4 tests, 22 assertions, 0/0.
`ehrt.docs-tooling.notice-verbatim-test`: 4 tests, 163 assertions (up
from 157 — the 3 new rows' own 6 assertions). `bin/verify-nist-lock`:
OK, 6 hit-nexus-sourced coordinates matched (`nist-hl7-v2-parser`,
`nist-hl7-v2-profile`, `nist-hl7-v2-validation`, `nist-xml-util`,
`nist-hl7-v2-schemas`, `nist-validation-report`). `gitleaks detect
--source . --no-git -v`: no leaks found.

### The arc's closing narrative

ADR-0070 (2026-08-07) deferred `injuries.json` WHOLE on a real
`gmf-interpreter` gap, naming its own revisit trigger. ADR-0105
(2026-08-11, B1) closed that trigger's exact defect — `run-submodule`
horizon-awareness, the zero-advance-only runaway budget — and its own
real-content probe found the fix complete but surfaced a SEPARATE,
pre-existing `nested :encounter` gap, unaffected by the fix. ADR-0106
(2026-08-11, B2) characterized that gap in full under a widened,
assessment-first charter: root cause, upstream Synthea's own real
source-cited semantics (a quiet same-module auto-close, never a nest
or a throw), four design options with blast radius, no recommendation.
This ADR (2026-08-11, B3) executes the author's own ruling among those
four options — auto-close on reopen, matching upstream exactly — and,
on that fix's own green, lands the batch the whole arc was chartered
to deliver. Both deferral legs (max-steps, nested-encounter) are now
closed; `injuries.json`'s own eight-file closure is fully vendored;
the arc has no revisit trigger remaining.

### Roadmap, rulings, index

`.agents/plans/roadmap.md`'s Next-section B row is CLOSED in place
(B1 + B2 + B3, all landed 2026-08-11) — the arc's own full narrative,
no revisit trigger. A NEW Next row anchors the author's own SEPARATE,
2026-08-11 chartering ruling (downstream-latency realism — lab results
and EHR logging both take real time, so a downstream HL7 receiver may
see incomplete encounter records for a while; testing that receivers
handle this "is not our problem to solve" but supplying such cases is)
— marked awaiting-design-pass, nothing executed this session.
`.agents/rulings.md` gains a new "From ADR-0107" section recording
both 2026-08-11 rulings verbatim (the option-(i) selection, the
latency-realism charter). `notes/ADRs.md` gains this ADR's own index
line; `notes/adr/README.md`'s own file count corrects 104→105,
verified by `ls notes/adr/*.md | grep -v README | wc -l`.

### `.agents/state.md`: staleness tripwire tripped by this ADR's own filename, citation-only fix (precedented)

This ADR's own filename, `0107-injuries-arc-close.md`, matches
`state_staleness_tripwire_test.clj`'s own `*-arc-close.md` regex — the
SAME class of gate `notes/adr/0097-review-2-arc-close.md` tripped at
its own close, for the SAME reason (a filename ending in "arc-close,"
without a chartered full `state.md` regeneration in the same session).
Followed the EXACT precedent already recorded at `.agents/state.md`'s
own header (the ADR-0097 citation-only entry): a NEW citation-only
paragraph lands above it, moving the tripwire's own cited regeneration
point from ADR-0097 to ADR-0107, explicitly disclosing content was NOT
re-probed — satisfying the tripwire's own docstring ("checks
CURRENCY... not CONTENT"), not a silent evasion of the standing
AR-C-1 regeneration contract. A full regeneration remains owed to a
future session that charters it, unchanged by this fix. Verified: this
ADR's own name choice ("injuries arc close," naming the ADR-0070→
ADR-0105→ADR-0106→ADR-0107 thread's own conclusion) is disclosed here
as NOT a claim that this is one of the repo-wide arcs `state.md`'s own
sections track (alignment/UX/player/vendoring/fidelity/conviction/
etc.) — a narrower, single-closure thread that happens to share the
filename convention.

### `.agents/reading-sets.edn`: `:onboarding` budget re-baseline

Folded into this same close-phase commit, not a separate session — the
same "routine growth, re-baseline on the session that trips it"
discipline every prior re-derivation comment in that file already
establishes. `reading-set-budget-test` went red once this session's
own `roadmap.md`/`prompts/README.md`/`session-records/README.md`
growth (accumulated since the conviction-arc-close re-derivation,
ADR-0089, across every session from ADR-0090 through this one) landed:
`:onboarding` measured 1480 lines against its own 1470-line budget.
Re-applying the standing formula (actual x1.15, rounded up to the
nearest 5) against this session's own FINAL state (after every
close-phase edit landed) is the number recorded in `reading-sets.edn`
itself, dated to this session.

### Fences

Phase 1 touched exactly: `components/sim-trajectory/{src,test}`
(`gmf_interpreter.clj`, `gmf_interpreter_test.clj`), `components/
sim-trajectory/docs/gmf-interpreter.md` (the dated addendum). `mark-
phase` itself: untouched (docstring only). Emitters: untouched — the
synthesized end is an ordinary trajectory event to every emitter, no
special-casing found or needed. Phase 2 touched exactly: `components/
sim/resources/sim/modules/` (the 3 new files, `NOTICE`), the two new
vendored-test files, `components/oracle/src/ehrt/oracle/digest.clj`
(one new producer function, one new root entry, both additive). Close
phase touched exactly: the usual register files (`notes/adr/0107-*.md`
this file, `notes/ADRs.md`, `notes/adr/README.md`, `.agents/plans/
roadmap.md`, `.agents/rulings.md`, `.agents/prompts/`, `.agents/
session-records/`, `.agents/reading-sets.edn`, `.agents/state.md`
citation sentence only). `engine/run` per-
patient isolation: not this session, its roadmap row stands unchanged.
No default-config change anywhere — `injuries` is AVAILABLE, never
assigned by any scenario or demo. Scratch fetches and probe scripts:
never committed, cleaned before this record's own commit.

### Deviations, disclosed

- **`:module-horizon-days` 18250, not 36500** — see the dedicated
  section above; found live, not assumed, and consistent with every
  probe this arc has ever run this closure at.
- **The `:synthesized-encounter-ends` counter was added** — per the
  driving prompt's own explicit either-way instruction; see the
  counter-decision section above for the reasoning.
- **`wellness-wait-step`'s own separate nesting assert left
  unchanged** — a disclosed, narrower scope decision matching the
  driving prompt's own precise Context wording ("the `:encounter`
  case's own assert"), not an oversight.
- **`.agents/reading-sets.edn`'s own `:onboarding` re-baseline** —
  folded into this close-phase commit rather than deferred, matching
  the file's own standing "re-baseline on the session that trips it"
  discipline.
- **`.agents/state.md`'s own staleness tripwire, tripped by this ADR's
  own filename** — a citation-only fix, exactly precedented by the
  ADR-0097 entry already on record there; see the dedicated section
  above.

### Index line

```
- 2026-08-11 — injuries-arc-close — ADR-0107
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

## ADR-0122 — Positive-seed invariant violation: diagnosis

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-13.

### Context

ADR-0121's own pre-commit-1 `make test` run hit a `clojure.test.check`
failure in `ehrt.sim-engine.engine-test`'s
`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
defspec, at an UNPINNED seed, `1786589996178`, `failing-size 144`. That
session characterized it as "a pre-existing, ADR-0114-R8-chartered
`ehrt.sim-engine.engine-test` flake ... self-cleared on re-run" and
proceeded, citing R8's standing license
(`.agents/rulings.md`, "From ADR-0114"). This session's own erratum to
`notes/adr/0121-*.md` (commit 1, `.agents/rulings.md` "From ADR-0122"
R12/R13/R-clarify) corrects that: R8's own text names ONE seed,
`7844068501`, as its repro handle, and ADR-0116 already exercised that
license — pinning `7844068501` found it passed clean, 150/150 trials,
closing R8's own specific charter. A failure at a DIFFERENT seed is a
new finding, not covered by R8's retired scope. This session's own
charter, per the author's own 2026-08-13 "Both a." ruling ((a),
`.agents/rulings.md` R12): diagnose this new finding fully before any
fix — root cause, blast radius against the 35 oracle roots, lettered
fix options. No fix lands here; that is a separate, future, ruled
session.

Read first, per this session's own driving prompt: `notes/adr/
0116-engine-seed-contract.md` (the diagnosis method precedent — pin the
seed via `clojure.test.check/quick-check`, capture the shrunk witness,
walk it through `engine/run` + `check/check-all` directly); `components/
sim-engine/test/ehrt/sim_engine/engine_test.clj`'s own defspec (lines
1160-1187, `gen/large-integer* {:min 0}`, post-ADR-0116); `notes/adr/
0121-*.md`'s own Verification section (the erratum this session's
commit 1 appended); `components/sim-check/`'s invariant catalog.

### Tag ceremony

`origin/main` at `f483ab7` (ADR-0121 close) at session start, matching
this session's own driving prompt exactly. **The last five `main` CI
runs** (`gh run list --limit 5 --branch main`, checked at session
start): all `completed`/`success` — `31680996212` (scheduled
Integration, 9m36s), `31663630998` (ADR-0121 session record, 3m35s),
`31663134103` (ADR-0121 chapter 5, 4m34s), `31662814966` (ADR-0121
chapter 4, 4m41s), `31658899582` (ADR-0120 session record, 4m43s) — no
red among the five. Tag `stable-20260812-manual-s3` created ANNOTATED
at `f483ab7`; pushed; peeled ref confirmed
`f483ab70fe89f9a1f4cf4ca051c8bd08db785efd` — exact match. License: case
(i), per this session's own driving prompt's citation (channel
fresh-clone verification 2026-08-13 — lineage, ASCII x3, zero
`src`/`test`, docs-only footprint, oracle identity — CI confirmed green
per this preflight).

### Step 1 — Repro (Step 2.1 of this session's own driving prompt)

`clojure.test.check/quick-check` run directly against the property
reconstructed verbatim from the defspec (same config: 4 patients, 2 on
an explicit scripted admission/delay/discharge pathway, 2 module-only
assigned to the hand-authored `fixture-clinic` closure,
`:module-horizon-days 3650`), 150 trials, `:seed 1786589996178`:

```
{:shrunk
 {:total-nodes-visited 33, :depth 0, :pass? false, :result false,
  :time-shrinking-ms 142, :smallest [8589258984]},
 :failed-after-ms 877, :num-tests 145, :seed 1786589996178,
 :fail [8589258984], :result false, :failing-size 144, :pass? false}
```

**Reproduces exactly**: same seed, same `failing-size 144`. Shrunk
minimal counterexample: seed `8589258984`.

### Step 2 — Direct witness (Step 2.2)

`engine/run` evaluated directly at the defspec's own exact config with
`:seed 8589258984`, then `check/check-all` on the result:

```clojure
{:status :rejected,
 :category :invariant-violation,
 :payload
 {:violations
  [{:invariant
    :medication-end-references-existing-order-and-follows-it-in-time,
    :patient-id "PID-000003-fd6d262d",
    :at 436440}]}}
```

**One invariant, one patient, one violation** — no other invariant in
the catalog fires. The violating patient's own two ground-truth events
(patient-ordinal 2, module-only, assigned to `fixture-clinic`):

```clojure
{:event :registered, :t 4440, :active-mrn "MRN000004",
 :persona {:dob "2024-12-26" :age 0 ...}
 :participants [{:patient-id "PID-000003-fd6d262d", :role :subject}]
 :pre-horizon-facts
 [{:event :condition-onset ... :citation {:module "fixture-clinic" :state :sinusitis-onset} ...}
  {:event :medication-order ... :citation {:module "fixture-clinic" :state :prescribe-amoxicillin} ...}]
 :warm-up false}

{:event :medication-end, :t 436440, :active-mrn "MRN000004",
 :order-event-id nil,
 :order-citation {:module "fixture-clinic", :state :prescribe-amoxicillin},
 :participants [{:patient-id "PID-000003-fd6d262d", :role :subject}]
 :citation {:module "fixture-clinic", :state :end-medication}
 :warm-up false}
```

This patient's own DOB (`2024-12-26`, age 0) falls essentially at this
run's own FIXED registration anchor (`persona/reference-today-epoch-
day`, `engine_test.clj`'s own docstring at line ~1080 names
`fixture-clinic`'s episode as "a single near-birth event"). The
medication order (amoxicillin, for a sinusitis onset) fires just before
registration — HISTORY phase, compiled to a `:pre-horizon-facts` entry
riding the `:registered` event, never its own ground-truth
`:medication-order` event — while the corresponding `:medication-end`
fires 432000 seconds (5 days) later, safely inside the horizon, and is
emitted as a normal ground-truth event with `:order-event-id nil`
(no matching `:medication-order` event exists anywhere in the log to
resolve to) and `:order-citation` intact.

### Step 3 — Root cause (Step 2.3)

**The checker.** `medication-end-references-existing-order-and-
follows-it-in-time`
(`components/sim-check/src/ehrt/sim_check/check.clj:457-473`) walks
every `:medication-end` event, resolves `target` via `(get indexed
(:order-event-id event))`, and flags a violation when `target` is `nil`
(among other legs). Read directly: this is exactly what fired here —
`:order-event-id` is `nil`, so `target` is `nil`, so the first `:when`
disjunct (`(nil? target)`) is true. **The checker is not wrong about
what it currently checks** — a `:medication-end` with no resolvable
order really is, by its own stated invariant, a dangling reference.

**The engine.** `decide :medication-end`
(`components/sim-engine/src/ehrt/sim_engine/engine.clj:774-791`)
resolves `order-event-id` by scanning `world`'s own `:ground-truth` log
for a `:medication-order` event whose `:citation` matches
`order-citation`:

```clojure
(let [{:keys [ground-truth patients]} world
      patient (get patients patient-id)
      order-event-id (when order-citation
                       (last (keep-indexed (fn [i ev] (when (and (= :medication-order (:event ev))
                                                                 (= order-citation (:citation ev)))
                                                        i))
                                           ground-truth)))]
  ...)
```

This search only ever looks at TOP-LEVEL ground-truth events named
`:medication-order`. It never looks inside any `:registered` event's
own `:pre-horizon-facts` vector — which is exactly where a
medication-order compiled as pre-horizon content lives
(`components/sim-trajectory/src/ehrt/sim_trajectory/compile_trajectory.clj`'s
own `pre-horizon-fact-types`, line 328, explicitly includes
`:medication-order`/`:medication-end` in the "ratified item 5" set —
"the only pre-horizon events that ever become a REGISTRATION-TIME fact
rather than being dropped outright," reasoned as "ongoing therapeutic
content ... exactly as clinically relevant as an active medication
already is"). `compile-trajectory` evaluates PRE-HORIZON STATUS
per-event (line 436, `effective-pre-horizon?`), not per order/end PAIR
— so an order that individually falls before registration is compiled
to a fact, while its own end, individually falling after registration,
is compiled and emitted as a normal ground-truth event. This is a
deliberate, documented design (a straddling medication episode is real,
witnessable content, the same class reasoning the `:encounter`/
`:encounter-end` straddle fix (ADR-0086) already applied one layer
over). The gap is that `decide :medication-end`'s own citation search
was never widened to also match against this legitimate alternate
location, so a `:medication-end` closing a legitimately pre-horizon
order always resolves to `order-event-id nil` — indistinguishable, to
the checker, from a genuinely dangling reference.

**A related, second-order gap, found but out of this diagnosis's own
scope.** `evolve :medication-end`
(`components/sim-engine/src/ehrt/sim_engine/engine.clj:1019-1032`)
resolves independently, by `:order-citation` match against the FOLDING
patient's own `:medication-orders` accumulator — built only from
`evolve :medication-order` calls the fold has already seen. Since a
pre-horizon order's `decide :medication-order` never runs (it compiles
to a fact, not an event), the accumulator never gains an entry for it
either, so `evolve :medication-end` silently no-ops (`if-let` finds no
match, returns `patient` unchanged) rather than throwing. This means
`patient-state-is-a-fold-of-the-log`'s own state-fold representation of
this patient never shows this medication episode at all, pre- or
post-end — a state-fidelity gap adjacent to, but distinct from, the
referential-integrity violation this ADR diagnoses. Named here for the
fix session's own awareness; not itself the chartered finding, and no
catalog invariant currently checks fold-completeness against
pre-horizon facts.

**Conclusion: genuine violation, incomplete engine/checker coverage of
a legitimate, already-designed straddle case** — not a checker
mis-specification in the sense of asserting something false, and not a
data-corruption engine bug in the sense of producing wrong clinical
content; the emitted event log is exactly the content the compile-layer
design intends. The gap is that the referential-integrity check and the
order-resolution search were both written before (or without regard
to) the order/end pre-horizon-straddle case the compile layer already
supports.

### Step 4 — Blast estimate against the 35 oracle roots (Step 2.4)

`components/oracle/src/ehrt/oracle/digest.clj`'s own `roots` map (the
regression-oracle's standing fixed-seed golden-run producers) has
exactly 35 entries. 3 are INTERPRETER-layer batches
(`appendicitis`/`sore-throat`/`ear-infections`) that call
`sim-trajectory/run-module` directly, never `engine/run` — `decide
:medication-end`'s own resolution code (`sim-engine`, engine layer)
structurally never executes for them. The other 32 are ENGINE-layer
pairs (`engine-pair`, calling `engine/run` then `emit-hl7/emit`) —
directly reachable in principle.

This session ran every one of the 32 engine-layer root producers at
their own pinned seed/population/horizon (unmodified, straight from
`digest.clj`) and called `check/medication-end-references-existing-
order-and-follows-it-in-time` on each one's own `:ground-truth`
directly:

```
allergic-rhinitis    medication-end events: 0   violations: 0
anemia                medication-end events: 0   violations: 0
asthma                medication-end events: 0   violations: 0
attention-deficit-disorder  medication-end events: 0   violations: 0
bronchitis            medication-end events: 0   violations: 0
colorectal            medication-end events: 0   violations: 0
death-fixture         medication-end events: 0   violations: 0
dementia              medication-end events: 0   violations: 0
dermatitis            medication-end events: 0   violations: 0
ear-infections-engine medication-end events: 0   violations: 0
ear-infections-history-engine  medication-end events: 0   violations: 0
fibromyalgia          medication-end events: 0   violations: 0
hypothyroidism        medication-end events: 0   violations: 0
injuries              medication-end events: 0   violations: 0
med-rec               medication-end events: 0   violations: 0
metabolic-syndrome-care  medication-end events: 0   violations: 0
osteoarthritis        medication-end events: 0   violations: 0
osteoporosis           medication-end events: 0   violations: 0
rheumatoid-arthritis  medication-end events: 0   violations: 0
sepsis                 medication-end events: 0   violations: 0
sinusitis              medication-end events: 0   violations: 0
sleep-apnea            medication-end events: 0   violations: 0
total-joint-replacement-engine  medication-end events: 0   violations: 0
urinary-tract-infections-engine medication-end events: 0   violations: 0
urinary-tract-infections-history-engine  medication-end events: 0   violations: 0
veteran-lung-cancer   medication-end events: 0   violations: 0
veteran-prostate-cancer  medication-end events: 0   violations: 0
veteran-ptsd            medication-end events: 0   violations: 0
veteran-self-harm       medication-end events: 0   violations: 0
veteran-substance-abuse-treatment  medication-end events: 0   violations: 0
vhd-pulmonic            medication-end events: 0   violations: 0
vhd-tricuspid           medication-end events: 0   violations: 0
```

**Zero of the 32 reachable oracle roots emit even one `:medication-end`
event at their own pinned seed/population** — the bug is entirely
unreached by every current oracle digest. This is not because the
class is impossible for real vendored content: a grep of every vendored
module JSON for `"MedicationEnd"` finds 14 modules that DO author a
medication order/end pair (`rheumatoid_arthritis`, `dementia`,
`fibromyalgia`, `sore_throat`, `injuries`, `asthma`, `bronchitis`,
`dermatitis`, `sinusitis`, `urinary_tract_infections`,
`veteran_lung_cancer`, `ear_infections`, `attention_deficit_disorder`,
`veteran_ptsd`) — several of them among the 32 checked roots above,
all showing 0 `:medication-end` events at their own pinned
seed/population. The straddle needs a patient whose sampled age lands
the whole medication episode close enough to the fixed registration
anchor that the ORDER falls in history phase and the END falls in
horizon phase — a narrower window than these roots' own populations
happen to hit at their pinned seeds, exactly the same "single
near-birth event, only rarely caught by a fixed anchor decades removed
from most sampled ages" dynamic `engine_test.clj`'s own docstring
(line ~1080) already names for `fixture-clinic` specifically. **A fix
can therefore hold pure digest identity across all 35 oracle roots by
construction — no root's own output would change** — verified
empirically here, not merely argued structurally, though the
structural argument (zero-current-reach, real latent reachability)
independently corroborates it.

### Step 5 — Fix options (Step 2.5, evidence only, no fix lands this session)

- **(a) Checker fix, RECOMMENDED.** Widen `medication-end-references-
  existing-order-and-follows-it-in-time`
  (`components/sim-check/src/ehrt/sim_check/check.clj`) to treat a
  `nil` `:order-event-id` as satisfying the invariant when the SAME
  patient's own `:registered` event carries a `:pre-horizon-facts`
  entry with `:event :medication-order` and a `:citation` matching the
  `:medication-end`'s own `:order-citation` — the same citation-match
  idiom `decide`/`evolve` already use, applied to the one place
  `decide` never looked. Zero `sim-engine` `src` change; the emitted
  ground-truth event shape is untouched. **Oracle consequence: NONE,
  by construction** — the checker never runs during ground-truth or
  HL7 generation (`digest.clj`'s own `engine-pair`/`interpreter-batch`
  never call `check/check-all` or any catalog function), so a
  checker-only change cannot move any oracle root's own digested
  bytes, independent of this session's own empirical zero-reach
  finding above. Lowest blast radius of the three; does not touch the
  related `evolve` fold gap (named above, left to the fix session's own
  judgment on whether it is in scope).
- **(b) Engine fix.** Widen `decide :medication-end`'s own resolution
  (`engine.clj:774-791`) to also search the patient's own
  `:pre-horizon-facts` for a matching citation, and when found there
  (rather than in `ground-truth` proper), set a new field on the
  emitted event (e.g. `:order-pre-horizon? true`) so the checker can
  distinguish "resolved, pre-horizon" from "genuinely dangling" without
  independently re-deriving the pre-horizon-facts lookup itself. Touches
  `sim-engine` `src` (a new field on every `:medication-end` event,
  additive/default-nil so existing occurrences are byte-unchanged) and
  `sim-check` `src` (the checker must read the new field). **Oracle
  consequence: provably none TODAY** (same zero-reach evidence as (a)
  — no oracle root emits `:medication-end` at all, so no root's own
  bytes carry the new field either way) **but structurally riskier
  going forward** — any FUTURE oracle root that does emit
  `:medication-end` would carry the new field in its own digest from
  first landing, a live surface for accidental drift a future session
  must remember to preserve. Not recommended over (a) without a reason
  to want the distinction visible on the event itself (e.g. a future
  FHIR/HL7 rendering need).
- **(c) Compile-layer fix, NOT recommended.** Extend
  `compile_trajectory.clj`'s own existing `:encounter`/`:encounter-end`
  straddle machinery (`effective-pre-horizon?`/`straddle-open?`, line
  436) to treat `:medication-order`/`:medication-end` as a PAIRED
  straddle too, so an order that is pre-horizon forces its own end to
  also compile as pre-horizon content (both fold into one
  registration-time fact) rather than the end landing as a normal
  ground-truth event. Touches `sim-trajectory` `src`; the most
  architecturally symmetric with the encounter precedent, but actively
  REGRESSES the design intent `pre-horizon-fact-types`'s own docstring
  states (an active medication ending inside the observation horizon
  IS real, clinically relevant, witnessable content — dropping it into
  a registration-time fact discards exactly the information this
  session's own witnessed event, `:medication-end` at `t 436440`,
  correctly makes visible). **Oracle consequence: provably none today**
  (same zero-reach evidence), but changes what future content looks
  like for every straddling medication episode, not just this
  violation's own referential-integrity gap. Not recommended.

No fix lands in this session, per its own charter — the ruling among
(a)/(b)/(c) belongs to the author, in a future, separate, ruled fix
session.

### Full gate

`clojure -M:poly check`: OK, both commits. `clojure -M:poly test :all
skip:integration` run three times this session: alongside commit 1's
own pre-push gate (twice, `:poly check` and `:poly test :all`
separately — the defspec passed clean both times, freshly-drawn random
seeds `1786616367009`/`1786616508593`); and once more as part of this
close-phase's own full `make test`, where it FAILED —
`:seed 1786617342587`, `:failing-size 125`, shrunk `:smallest
[1087719748893272]`. Recorded here as additional evidence per this
session's own standing gate policy (a failure this session's own runs
hit is recorded, one re-run to complete the gate is licensed HERE
ONLY): the shrunk seed was walked through `engine/run` + `check/
check-all` directly (the same method as Step 2's own witness, above) —
**the SAME invariant fires**, `medication-end-references-existing-
order-and-follows-it-in-time`, a different patient
(`PID-000002-8af354bc`) at a different time (`:at 360`). This is a
THIRD independent occurrence of the exact diagnosed bug class in this
session alone (the chartered seed's own shrunk witness, `8589258984`;
and now this one), corroborating both the root cause (a structural gap
in `decide :medication-end`'s own resolution, not a one-off) and the
blast-radius finding (real, reachable, just narrow enough that no
oracle root's own fixed seed/population happens to hit it). Re-run
immediately after: GREEN, same test passing at a freshly-drawn random
seed. `bin/verify-nist-lock`: OK, all 6 hit-nexus-sourced coordinates
matched. `gitleaks git --staged -v`/`gitleaks detect`: no leaks found,
both commits.

### Oracle bracket

Pure identity expected and required — this session's own diagnosis
runs (the repro, the direct witness, the 32-root blast-radius sweep)
all write to disposable in-process scratch only, nothing committed;
`notes/adr/0121-*.md`'s erratum, `.agents/rulings.md`, `.agents/plans/
roadmap.md`, and this file are the only `src`/`test`-adjacent-looking
touches, all pure prose. `bin/regression-oracle f483ab7 <this
close-phase commit>`: **IDENTICAL, all 35 roots** (recorded at close,
below) — matching the trivial-identity expectation exactly, zero `src`/
`test`/`resources` change anywhere this session.

### Fences

Touched exactly: `notes/adr/0121-manual-s3-transport-pair.md` (the
erratum, commit 1); `.agents/rulings.md` (commit 1); `.agents/plans/
roadmap.md` (commit 1 and this close); `notes/adr/
0122-positive-seed-invariant-violation-diagnosis.md` (this file);
`notes/ADRs.md`; `notes/adr/README.md`; `.agents/session-records/*`;
`.agents/prompts/*`. ZERO `src`/`test`/`docs/manual` change anywhere —
every quoted code excerpt above is read-only citation; every diagnosis
run (the pinned quick-check, the direct witness, the 32-root sweep)
executed from scratch scripts outside the repo tree (this session's own
scratchpad), never committed. No fix of any kind lands.

### Deviations

**None.** Every Read-first document matched this session's own
characterization of it; the repro reproduced exactly at the pinned
seed and failing-size the finding named; the shrunk counterexample was
a non-negative seed as expected (ADR-0116's own R9 already forecloses
negative seeds at entry, so this positive-seed class was always the
remaining exposure); the checker was read and confirmed genuinely
firing, not a false positive, though its own coverage is incomplete
against a legitimate compile-layer design; the blast estimate ran
empirically against all 32 reachable oracle roots rather than resting
on the structural argument alone.

### Index line

```
- 2026-08-13 — positive-seed-invariant-violation-diagnosis — ADR-0122
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Positive-seed invariant violation: diagnosis — diagnosis-only session, zero fix: pins the failing seed ADR-0121's own gate hit (`1786589996178`, `failing-size 144`) via `clojure.test.check/quick-check`, reproduces it exactly, shrinks to seed `8589258984`; the direct witness shows one genuine violation, `medication-end-references-existing-order-and-follows-it-in-time`, for a near-birth patient whose medication order compiles as a pre-horizon `:registered` fact while its own end lands in-horizon as a normal ground-truth event with `:order-event-id nil` — `decide :medication-end`'s own citation search (`engine.clj`) never looks inside `:pre-horizon-facts`, the one place a legitimately straddling order lives (`compile_trajectory.clj`'s own ratified pre-horizon-fact-types design); root cause traced and the checker confirmed genuinely firing on its own current (incomplete, not false) specification; blast estimate runs all 32 reachable oracle-root producers directly — zero of them emit even one `:medication-end` event at their own pinned seed/population, so a fix can hold pure oracle identity by construction; three lettered fix options recorded (a widen-the-checker fix RECOMMENDED, an engine-side resolution widening, a compile-layer straddle extension NOT recommended as it would regress the design's own intent) — the ruling and the fix itself are chartered to a future session; also corrects the S3 gate event's own mischaracterization (R8's standing license named seed `7844068501` specifically, already investigated and closed by ADR-0116 — a failure at any other seed was always a new finding), and charters a future ceremony-scripts session (tag ceremony, preflight, post-push verification, close-phase scaffold as scripts, absorbed into `build-session`); zero `src`/`test`/`docs/manual` touched anywhere, the oracle holds pure identity across all 35 roots

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0122 (positive-seed invariant violation: diagnosis; ruled
2026-08-13)

The design channel framed the S3 gate event's own recharacterization
and this diagnosis session's charter as one question; the author ruled,
verbatim, 2026-08-13: *"Both a."*

- **R12, diagnosis-before-fix [A, ruled 2026-08-13, "Both a." part (a)]:**
  the positive-seed invariant violation found at seed `1786589996178`
  (`failing-size 144`, ADR-0121's own gate, recharacterized by this
  session's erratum to ADR-0121) gets a diagnosis-only session
  (ADR-0122) before any fix session runs -- root cause, blast radius
  against the 35 oracle roots, and lettered fix options land first; the
  fix itself is a separate, future, ruled session.
- **R13, ceremony-scripts charter [A, ruled 2026-08-13, "Both a." part
  (b)]:** this repo's own recurring session-start/session-end ceremony
  -- tag ceremony, preflight (last-five-CI-runs check, edit-root
  confirmation), post-push message verification, and the close-phase
  scaffold (self-archive, session record, prompt archive, index bump)
  -- moves from prose a session re-reads each time to scripts, with
  checkpoint isolation, red capture, and sweep census absorbed into the
  `build-session` skill alongside them. Scheduled post-manual-arc (after
  S4/S5 land), not this session's own work.
- **R-clarify, R8's scope [C, channel-inferred from the author's own
  ruling text, un-vetoed]:** R8's own standing license
  (`.agents/rulings.md`, "From ADR-0114") named one specific seed,
  `7844068501`, as its repro handle -- it licensed pinning and
  classifying THAT seed, not open-ended cover for any future generative
  failure in the same defspec. ADR-0116 already exercised the license
  R8 granted (pinning `7844068501` found it passed clean, closing that
  specific investigation); a failure at a different seed is therefore
  always a new finding under R8, not a re-run candidate. This clarifies
  R8's own text; it does not narrow or retract anything R8 itself ruled.
- **Standing gate policy, repo-wide, effective now:** any generative
  failure in
  `mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
  (or, by the same reasoning, any other `clojure.test.check` defspec in
  this repo) is a new finding a session must STOP on and record, never
  a re-run licensed by a prior seed-specific charter's retired scope.
  Applies to every future session that hits a generative test failure
  in this defspec; the author may strike or correct this reading.

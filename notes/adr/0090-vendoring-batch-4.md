## ADR-0090 — Vendoring batch 4: the veteran family comes home, five of nine, and a mechanism name gets corrected in the open

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: the conviction arc closed (`notes/adr/0089-conviction-arc-
close.md`, tip `a6640e9`), two loops convicted on evidence, `state.md`
and the reading-set budgets regenerated, `.agents/rulings.md` gained
the arc's two new standing laws (witnessed rows only; licenses bind at
their own granularity). The author ruled, verbatim, 2026-08-08:
**"Batch 4"** — the veteran family, the horizon's first named item
(carried since ADR-0074/ADR-0087's own dated horizon notes). Nine
candidates named from the wave-f census (2026-08-03): `veteran.json`,
`veteran_hyperlipidemia.json`, `veteran_lung_cancer.json`,
`veteran_mdd.json`, `veteran_prostate_cancer.json`,
`veteran_ptsd.json`, `veteran_self_harm.json`,
`veteran_substance_abuse_conditions.json`,
`veteran_substance_abuse_treatment.json`. Their census verdicts predate
Waves G/I/VS/H and the straddle fix (ADR-0086) — this session's own
driving prompt named them a prior map, never current evidence, and
required every disposition to rest on FRESH gates at the pin.

Read-first: `notes/adr/0070-vendoring-batch-1.md` through
`0072-vendoring-batch-3.md` (the batch mechanics — closure enumeration
including non-JSON data files, byte-verbatim vendoring at pin,
NOTICE hashing, defer-with-true-name for failers); `notes/adr/
0087-colorectal-payoff.md` (the current per-module payoff shape: a
round-trip test at population scale, measured counter pins, an
additive FIRST-BASELINE root); `.agents/rulings.md` (vendored-bytes
law, the population-scale gate, the multi-seed law, the conviction
arc's two new laws); `components/sim-trajectory/docs/census/
2026-08-03-synthea-7e08387-wave-f.edn` (the stale prior verdicts).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-08). `[A]` author-ruled, `[C]` channel-inferred.

**AR-VB4-0 `[A]` (ADR-0089, "mechanical debt").** Tag
`stable-20260808-conviction-close` at `a6640e9`, annotated, standing
ceremony (design-channel-verified 2026-08-08). Not present locally or
on the remote; created fresh, pushed, peeled ref verified. **Executed
Step 0.**

**AR-VB4-1 `[A]` (the batch, ruled "Batch 4").** Gate all nine
candidates FRESH at the pin (`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`, verified FIRST): closure
enumeration fresh per candidate (data files included), full compile/
engine/check round trip, 300 patients, 2-3 well-mixed seeds. See the
disposition table, below.

**AR-VB4-2 `[C]` (the attribute gate, this family's own hazard).** All
nine gate on the upstream `veteran` Person attribute — read FIRST for
every candidate's own `Initial`/guard chain, per the ruling. **Disclosed
correction, not silently absorbed:** the driving prompt named
`:persona-config` as "the interpreter's existing... mechanism (the
established precedent for attribute-gated modules)." That precedent is
actually `:initial-attributes` (ADR-0033 AR-1, `total_joint_
replacement.json`'s own `vendored_tjr_test.clj`) — a closure-level
seed, keyed `{<root-id>/<attribute> value}`. `:persona-config`
(`anemia___unknown_etiology.json`/`colorectal_cancer.json`'s own
`:race-weights` shape) reaches only PERSONA-level condition types
(`Race`/`Socioeconomic`/`State`, `sim_model.persona/persona`'s own
config-gated draws 14-16); the generic `Attribute` condition type every
one of these nine candidates uses resolves ONLY against `(:attributes
ctx)`, root-namespaced (`gmf_interpreter.clj`'s own `attribute-
condition-holds?`), and never reads the persona map at all. Confirmed
by direct inspection of both functions before writing a single gate
run, not asserted. Every vendorable module below is seeded
`{<root-id>/veteran true}` via `:initial-attributes`, disclosed in its
own test docstring and pinned in its own oracle root.

**AR-VB4-3 `[C]` (per-passer landing, the ADR-0087 shape).** Five
vendorable modules land byte-verbatim (upstream sha256 in NOTICE,
`.gitattributes` coverage confirmed by grep not assumed), each with
its own round-trip test and additive FIRST-BASELINE oracle root,
co-landed in one self-contained checkpoint commit. See Execution
record.

**AR-VB4-4 `[C]` (the bracket).** `bin/regression-oracle a6640e9
<tip> --declared-digest-change` declaring exactly the five additive
roots landed. See Verification.

**AR-VB4-5 `[C]`.** No census-tool changes; no pairing-registry rows;
the stale-census diff (old verdict vs fresh) is below, and the one old
`:walk-failed` verdict now passing (`veteran_substance_abuse_
treatment.json`) is named `unknown` — not bisected this session,
an evidenced non-attribution, not a guess.

### Fresh-gate disposition table (AR-VB4-1/2)

Every row gated fresh this session against the pin; `veteran`
attribute seeded via `:initial-attributes` except where noted.

| Candidate | Closure (fresh) | Seeds | Result | Disposition | Old-census verdict → fresh |
|---|---|---|---|---|---|
| `veteran.json` | root only | 20260802, 1 | Walks clean, 300/300 `:registered`-only, zero clinical states in the file at all (SetAttribute/Simple only) | **DEFERRED** — zero-substance, population-scale gate | `:zero-on-every-seed` → confirmed same, structural (the module never emits clinical content by design) |
| `veteran_hyperlipidemia.json` | root only | 20260802, 1 | Walks clean of exceptions; `check/check-all` **FAILS** — `:medication-end-references-existing-order-and-follows-it-in-time`, 20+ violations/300 patients at seed 20260802, confirmed at horizons 16000/18000/20000 days | **DEFERRED WHOLE** — real population-scale invariant violation, true name below | `:zero-on-every-seed` (attribute never set) → real defect surfaces once gated open |
| `veteran_lung_cancer.json` | root only | 20260802, 1 | Clean, real content (`:admission`/`:procedure`/`:discharge`), HL7 renders | **VENDORABLE** | `:blocked` (attribute never set) → clean once gated |
| `veteran_mdd.json` | root + `medications/moderate_opioid_pain_reliever.json` (already vendored, batch 1; re-verified byte-identical, not re-copied) | 20260802, 1 | `run-module` throws `exceeded max-steps` at `:therapy-delay`/`:end-therapy-visit`; reproduced at horizons 36500/18250/3650 (only a useless 100-day horizon avoids it) | **BLOCKED** — real interpreter max-steps exhaustion, true name below | `:zero-on-every-seed` (attribute never set) → real gap surfaces once gated open |
| `veteran_prostate_cancer.json` | root only | 20260802, 1, 42 | Clean, real content (`:diagnostic-report`/`:medication-order`/`:care-plan-start`/`:procedure`), HL7 renders; `:suppressed-straddle-spans` measured nonzero (2/0/0) | **VENDORABLE** | `:zero-on-every-seed` (attribute never set) → clean once gated |
| `veteran_ptsd.json` | root only | 20260802, 1, 42 | Clean, real content (`:care-plan-start`/`:procedure`), HL7 renders; `:suppressed-straddle-spans` measured nonzero (14/6/7) | **VENDORABLE** | `:zero-on-every-seed` (attribute never set) → clean once gated |
| `veteran_self_harm.json` | root + `veterans/veteran_suicide_probabilities.json` (genuinely new) | 20260802, 1 | Clean, real content (`:admission`/`:procedure`/`:discharge`), HL7 renders | **VENDORABLE** | `:blocked` (attribute never set) → clean once gated |
| `veteran_substance_abuse_conditions.json` | root only | 20260802, 1 | Walks clean, 300/300 `:registered`-only, zero clinical states in the file at all (SetAttribute/Simple/Delay only) | **DEFERRED** — zero-substance, population-scale gate | `:blocked` (attribute never set) → clean walk, but zero-substance regardless |
| `veteran_substance_abuse_treatment.json` | root only | 20260802, 1, 42 | Clean, real content (`:outpatient-visit`/`:outpatient-visit-end`), HL7 renders — reproduces IDENTICALLY seeded and unseeded (own top-level Guard is Age-only, not veteran-gated) | **VENDORABLE** | `:walk-failed` (max-steps at `:alcoholism-post-treatment`/`:encounter-end`, all 3 census seeds) → clean this session; which fix closed it is `unknown`, not bisected |

Five vendorable, four not: two zero-substance (`veteran`,
`veteran_substance_abuse_conditions`), one real invariant violation
(`veteran_hyperlipidemia`), one real interpreter max-steps exhaustion
(`veteran_mdd`).

### `veteran_hyperlipidemia.json`: assessed, DEFERRED WHOLE

The module's own annual reassessment loop —
`Time Delay`(1yr)→`annual_hyperlipidemia_assessment`→`Record_CMP_2`→
`Record_LipidPanel_2`→[if `statin_initial` is not nil]→`end old statin`
(`MedicationEnd`, `referenced_by_attribute: statin_initial`)→
`Hyperlipidemia_medication_renewal` (`MedicationOrder`,
`assign_to_attribute: statin_renewal`)→`end encounter`→`Time Delay`
(loop) — never clears `statin_initial` after the FIRST iteration ends
it. Every subsequent annual iteration re-checks `statin_initial is not
nil` (still true — the attribute persists, only the underlying order
was ended), so `end old statin` fires AGAIN against the SAME
already-ended order. `ehrt.sim-check.check`'s own
`:medication-end-references-existing-order-and-follows-it-in-time`
invariant fails once a patient survives two or more annual cycles —
confirmed at population scale (20+ violations of 300 patients, seed
20260802) and non-seed-tunable (the same violation shape persists at
horizons 16000/18000/20000 days, i.e. whenever enough patients reach a
second annual cycle at all). A real upstream module-authoring pattern
this project's interpreter compiles faithfully into a real, repeated
invariant violation — not an interpreter bug in the narrow sense, but
per the standing fence no module-content edit lands to work around it
either. Full finding recorded in NOTICE's own dated batch-4 section;
roadmap Deferred row added under its true name.

### `veteran_mdd.json`: assessed, BLOCKED

`Therapy_Visit`'s own recurring cycle — `therapy_delay` (5-14 day
Delay)→`Therapy_Visit` (Encounter)→`Therapy_Note` (Observation)→
`end therapy visit` (EncounterEnd)→[PriorState `MDD_Re_evaluation
Encounter` within 3 months? loop to `therapy_delay` : else
`MDD_Re_evaluation Encounter`] — genuinely advances real time each
iteration (the 5-14 day Delay), so this is not a true zero-time-advance
spin, but at a ~10-day average cycle length it accumulates roughly
3650 iterations across a 100-year horizon, each contributing several
interpreter steps — enough to exceed `gmf_interpreter.clj`'s own fixed
`max-steps` backstop (10000) before the walk's own horizon bound would
otherwise end it. Horizon-swept per the `injuries.json` bail-out
precedent (ADR-0070): fails identically at 36500/18250/3650 days;
only a 100-day horizon (too short to be a useful population-scale
round trip at all) avoids it. This is the SAME backstop-vs-legitimate-
long-loop tension `injuries.json`'s own dangling-`dental_referral` gap
first named, a different mechanism entirely (that one a true
zero-advance spin; this one a real, bounded-but-long recurring
schedule). Per the standing fence, no interpreter/module-content edit
lands this session. Full finding recorded in NOTICE's own dated
batch-4 section; roadmap Deferred row added under its true name.

### `veteran.json` / `veteran_substance_abuse_conditions.json`: assessed, DEFERRED (zero-substance)

Both walk clean at every seed tried, both seeded and unseeded, and
both compile ZERO clinical content — `veteran.json`'s own states are
entirely `Initial`/`Terminal`/`Guard`/`Simple`/`SetAttribute` (nine of
each of the last two); `veteran_substance_abuse_conditions.json`'s own
are `Initial`/`Terminal`/`Guard`/`SetAttribute`/`Delay`/`Simple`.
Neither file contains an `Encounter`, `ConditionOnset`,
`MedicationOrder`, `Procedure`, or `Observation` state anywhere —
these are upstream's own attribute-setting utility modules, feeding
downstream veteran-family siblings' own conditions in real Synthea's
shared-attribute-space design, never modules with their own clinical
content. Per the standing population-scale gate law (zero-substance
modules are not vendorable — a census/walk-clean verdict is evidence
for curation, never a vendoring license), both are DEFERRED, joining
the attribute-blocked set alongside batch 3's own three zero-substance
family siblings. No dedicated roadmap row (matching that same batch-3
precedent) — recorded here and in NOTICE's own dated section only.

### `veteran_substance_abuse_treatment.json`: old census `:walk-failed`, now clean — `unknown`, not guessed

The 2026-08-03 wave-f census recorded this module `:walk-failed` on
all three of its own seeds, the SAME `run-module exceeded max-steps`
exception (at `:alcoholism-post-treatment`/`:encounter-end`). This
session's fresh gate is clean at three seeds (20260802/1/42), both
seeded and unseeded — the module's own top-level Guard is Age-only
(`> 18 years`; its own "Veteran Guard" state name notwithstanding),
never itself veteran-gated, and the identical clean result reproduces
regardless of whether `:initial-attributes` seeds `veteran`. Two
candidate fixes landed 2026-08-08, both before this session and both
after the stale census: the EncounterEnd fix (ADR-0082) and the
straddle fix (ADR-0086). Which one (or both) actually closed this
module's own prior loop was not bisected this session (that would
require a pre-fix worktree and a targeted re-run, out of this
session's own scope) — named `unknown`, per AR-VB4-5's own explicit
instruction, an evidenced non-attribution rather than an unearned one.

### Execution record

**Step 0 (no commit).** Cwd confirmed the ext4 clone
(`~/src/ehr-testing-tools`), tip `a6640e9`, working tree clean. Last
five CI runs on `main` disclosed, all `success`
(`31288621176`/`31288276758`/`31287834460`/`31286768535`/`31286289031`
— no red window). `clojure -M:poly check` OK. Oracle self-bracket
(`bin/regression-oracle a6640e9 a6640e9`): IDENTICAL, all 29 roots,
byte-for-byte. Pin checkout re-confirmed (`/home/mg/synthea-checkout`,
`git rev-parse HEAD` = `7e08387c68a7f0e21d13076609a159fd473fc902`,
working tree clean). AR-VB4-0 executed directly:
`stable-20260808-conviction-close` created annotated at `a6640e9`,
pushed, verified — peeled ref resolves exactly.

**Step 1 (no commit).** Fresh gates run against the pin checkout via a
scratch harness (a `resolve-fn`/`table-resolve-fn` reading the
checkout directly, the same shape `census.clj`'s own private
`make-resolve-fn` uses, never landed) — closure enumeration, guard-
chain inspection, and full compile/engine/check(/emit) round trips per
candidate, at the seed counts the disposition table names. The
`:persona-config`/`:initial-attributes` mechanism correction (AR-VB4-2)
was found and confirmed by direct inspection of `gmf_interpreter.clj`'s
own `attribute-condition-holds?` and `sim_model/persona.clj`'s own
`persona` function BEFORE any gate ran, not discovered by trial and
error. `veteran_hyperlipidemia.json`'s own violation was traced to its
exact state-machine cause by reading its annual-reassessment states
directly, then confirmed non-seed-tunable via a horizon sweep.
`veteran_mdd.json`'s own max-steps exhaustion was traced to its
recurring therapy-visit cycle by reading its own states directly, then
confirmed non-horizon-tunable via the SAME sweep method ADR-0070's own
`injuries.json` bail-out used. `veteran_substance_abuse_treatment.
json`'s own clean-both-ways result was confirmed by an explicit
unseeded re-run, not assumed from the seeded one alone.

**Step 2 (`7767326`, AR-VB4-1/2/3).** Five modules copied byte-verbatim
(`veteran_lung_cancer.json`, `veteran_prostate_cancer.json`,
`veteran_ptsd.json`, `veteran_self_harm.json` plus its own called
`veterans/veteran_suicide_probabilities.json`,
`veteran_substance_abuse_treatment.json`) — `.gitattributes`' own
`components/sim/resources/sim/modules/** -text` rule confirmed by grep
to cover the new `veterans/` subdirectory, not assumed. NOTICE gained
six new provenance rows plus a dated batch-4 section (the full
disposition table, the mechanism correction, both true-named
deferrals, the old-census-verdict diff). Five
`vendored_veteran_<module>_test.clj` files authored, each mirroring
the ADR-0087 two-deftest shape where a nonzero counter was measured
(`veteran_prostate_cancer`/`veteran_ptsd`, `:suppressed-straddle-spans`
2/0/0 and 14/6/7 respectively, measured via `with-redefs` interception
at the SAME `ehrt.sim-trajectory.interface/compile-trajectory`
boundary ADR-0087's own colorectal test established) or a single
round-trip deftest where the counter measured zero at every seed
(`veteran_lung_cancer`/`veteran_self_harm`/`veteran_substance_abuse_
treatment` — "no third bucket for the common case," ADR-0071's own
discipline). Witnessed green in-session before staging: 7 tests, 50
assertions, 0/0. `digest.clj` gained five new producer functions
(`veteran-lung-cancer-pair`/`veteran-prostate-cancer-pair`/`veteran-
ptsd-pair`/`veteran-self-harm-pair`/`veteran-substance-abuse-
treatment-pair`) and five new `roots` map entries — purely additive,
every existing producer function and root entry byte-unchanged
(confirmed by diff before staging); each new root function run
directly and its `:ground-truth`/`:hl7` counts cross-checked against
the committed test's own live numbers before commit. `ehrt.docs-
tooling.notice-verbatim-test` re-run against the updated NOTICE: green,
4 tests, 157 assertions (up from 145 — six new rows' own twelve
assertions). Full suite (`clojure -M:poly test :all skip:integration`):
every project block `0 failures, 0 errors`, confirmed by grepping the
ENTIRE run's own output for any non-zero failure/error count (not just
the tail); both new veteran test files present twice across project
groupings (standard for this workspace, ADR-0087's own note).
`clojure -M:poly check` OK. `gitleaks git --staged -v`: clean. Staging
hygiene: `git diff --cached --stat` showed exactly the thirteen files
this checkpoint touches — nothing else staged.

The official standing harness, `bin/regression-oracle a6640e9 7767326
--declared-digest-change`, reported `DIFFERS` — EXPECTED, per the
ADR-0070/0071/0072/0083/0087 precedent: the diff shows exactly five
ADDED lines (`veteran-lung-cancer.edn`/`veteran-prostate-cancer.edn`/
`veteran-ptsd.edn`/`veteran-self-harm.edn`/`veteran-substance-abuse-
treatment.edn`) and ZERO removed or changed lines among the 29
pre-existing roots.

Commit `7767326` ("feat: the veteran family comes home, group 1 --
lung cancer, prostate cancer, ptsd, self harm, substance abuse
treatment, gated fresh and pinned (batch 4, AR-VB4-1/2/3)"). Pushed;
post-push verification (`git log --format=%B -1` diffed against the
source message file): one delta, the known trailing-blank-line
artifact. CI watched to conclusion: run `31291802190`, `success`,
4m3s.

**Step 3 (this record).** `notes/adr/0090-vendoring-batch-4.md`
authored directly; index line appended to `notes/ADRs.md`; `notes/adr/
README.md`'s own stale file count corrected (87→88, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic); roadmap
gained two Deferred rows under their true names (`veteran_
hyperlipidemia.json`'s stale-reference bug, `veteran_mdd.json`'s
max-steps exhaustion), the "Now" section updated to this session's own
close, and a Done pointer (`- 2026-08-08 — vendoring-batch-4 —
ADR-0090`) added; session record and prompt archive land in the same
commit as this record's own citation-index update.

This session's own successor tag debt: `stable-20260808-vendoring-
batch-4` at this session's own closing tip is owed to the next
session's own Step 0, per tag law (ADR-0057 AR-T-1) — not created here
(no ruling licensed it at this session's own closing commit).

### Verification

- `bin/regression-oracle a6640e9 7767326 --declared-digest-change`:
  `DIFFERS`, EXPECTED — five added roots, zero changed/removed among
  the 29 pre-existing ones (the diff output itself is the evidence, not
  a count comparison).
- The five new `vendored_veteran_*_test.clj` files: witnessed GREEN
  in-session (7 tests, 50 assertions, 0/0) before staging — real
  compiled content, a clean invariant-catalog pass at every seed tried,
  real rendered HL7, and (where measured nonzero) the pinned
  `:suppressed-straddle-spans` totals reproduced live via `with-redefs`
  interception, not guessed.
- `ehrt.docs-tooling.notice-verbatim-test`: green, 4 tests, 157
  assertions (up from 145).
- Full suite (`clojure -M:poly test :all skip:integration`): green
  throughout, 0 failures/0 errors, confirmed by grepping the ENTIRE run
  output (not just the tail) for any non-zero failure/error count.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v` / `gitleaks git -v` (pre-push): clean,
  this session's one commit and the pushed history.
- Post-push message verification: one delta, the known harmless
  trailing-blank-line artifact.
- Tag verification: `stable-20260808-conviction-close` peeled ref
  resolves to `a6640e9` exactly, both locally and via `git ls-remote`.
- NOTICE hash cross-check: all six new SHA-256 values re-derived by
  fresh `sha256sum` against the vendored bytes and matched against the
  table before commit.
- CI: last-five on `main` at session start disclosed above (five
  green, no red window); this session's own push watched to
  conclusion, `success`, 4m3s.
- `veteran_hyperlipidemia.json` bail-out: horizon-swept (16000/18000/
  20000 days), violation persists at every horizon tried — not a
  seed- or horizon-tunable fluke at population scale.
- `veteran_mdd.json` bail-out: horizon-swept (36500/18250/3650/100
  days, the `injuries.json` precedent's own method), fails identically
  at every horizon long enough to be useful — not horizon-tunable.
- `veteran_substance_abuse_treatment.json` clean-both-ways: confirmed
  by an explicit unseeded re-run (900/900 events, identical kinds),
  not assumed from the seeded run alone.

### Deviations, disclosed

- **The `:persona-config`/`:initial-attributes` mechanism correction
  (AR-VB4-2)** — see the dedicated section in the Decision, above; the
  session's own driving prompt misnamed the established precedent for
  generic Attribute-condition gates. Corrected in the open, in NOTICE's
  own dated section and every new test's own docstring, before any
  gate ran on the wrong premise — not silently substituted.
- **`veteran_hyperlipidemia.json` and `veteran_mdd.json` deferred
  whole** — see their own dedicated sections above; two of nine
  candidates named "the veteran family" land as real, disclosed
  defects rather than vendored modules, each under its own true name,
  each with its own roadmap Deferred row (unlike the zero-substance
  pair, which get no dedicated row, matching batch 3's own precedent
  for that class).
- **`veteran_substance_abuse_treatment.json`'s own old-verdict
  correction attributed `unknown`** — see its own dedicated section
  above; AR-VB4-5's own explicit instruction followed rather than
  guessing between the EncounterEnd fix and the straddle fix.
- **Three seeds for `veteran_prostate_cancer`/`veteran_ptsd`/
  `veteran_substance_abuse_treatment`, two for the other three
  vendorable candidates** — the family's own 2-seed baseline
  (AR-VB4-1's own "2-3... THREE once flagged" range) with judgment
  applied per candidate: the first two because a nonzero
  `:suppressed-straddle-spans` measurement was worth a fuller picture
  even though it is not itself a gate failure; the third because of its
  own real prior census instability, an above-and-beyond disclosed
  choice, not a rule requirement.
- **`poly test`'s own change-detection gap on untracked-only test
  additions** — the same operational finding every prior batch ADR has
  named, repeated here (staged before every `poly test` invocation, not
  itself fixed, out of this session's own scope).

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Vendoring batch 4: the veteran family comes home, five of nine, and a mechanism name gets corrected in the open — nine candidates gated fresh at the pin (the 2026-08-03 wave-f census verdicts treated as a stale prior map, never current evidence); the driving prompt's own `:persona-config` mechanism claim is corrected in the open to the real precedent, `:initial-attributes` (ADR-0033 AR-1), before any gate runs on the wrong premise; five modules land (lung cancer, prostate cancer, ptsd, self harm, substance abuse treatment), the oracle's 30th-34th roots; four do not — two zero-substance, one a real population-scale `MedicationEnd`-double-reference defect (hyperlipidemia), one a real interpreter max-steps exhaustion in a legitimate long-running loop (mdd), each named under its own true name with its own roadmap Deferred row; one old `:walk-failed` census verdict now passes, attributed `unknown` rather than guessed

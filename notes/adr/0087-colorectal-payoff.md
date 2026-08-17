## ADR-0087 — Colorectal payoff: the twenty-ninth root, and the straddle counter finds its witness

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: the straddle fix (`notes/adr/0086-straddle-fix.md`, tip
`eb4b339`) closed the legacy pre-horizon drop gate structurally,
generalizing the Wave H `history-phase?` back-reference principle to
the legacy path — `colorectal_cancer.json` clean at all three of the
colorectal investigation's own seeds (20260802, 1, 42; 300 patients
each) as an in-session proof, but the fix session's own fence held: no
`digest.clj` root added, no NOTICE entry, the module still not
vendored. The roadmap's own "Colorectal vendoring payoff" Next row
(entered by ADR-0086) named this session's own brief exactly: the
population-scale gate law requires a fresh vendoring rider's own
committed test and `digest.clj`'s 29th root before the module joins the
tree, the same round-trip discipline every other vendored module
already carries.

`colorectal_cancer.json` has now been assessed, deferred, and
re-examined across four ADRs before landing: deferred whole at
vendoring batch 3 (ADR-0072) on a diagnosis-by-adjacency; that
diagnosis overturned by the fidelity payoff's own trajectory scan
(ADR-0083); the real blocker diagnosed to `compile-trajectory`'s own
legacy straddle gap (ADR-0085); the gap fixed structurally (ADR-0086).
This session executes the roadmap's own intake row and pins the
result.

Read-first: `notes/adr/0083-fidelity-payoff.md` (the anemia payoff,
this session's own shape precedent); `notes/adr/0086-straddle-fix.md`
(the fix this payoff collects on, the tag debt, the intake finding);
`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_anemia_test.clj`
(the committed two-deftest shape mirrored here); `notes/adr/0070-
vendoring-batch-1.md` (the vendoring mechanics, the NOTICE hashing, the
CSV/data-file lesson); `.agents/rulings.md`'s vendored-bytes and
population-scale-gate laws; `components/oracle/src/ehrt/oracle/
digest.clj` (the 28-root roots map, `anemia-pair`'s own producer
shape).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-08). `[A]` author-ruled, `[C]` channel-inferred.

1. **AR-CP-0** `[A — tag law, case (ii), ADR-0086's own successor tag
   debt]`. `stable-20260808-straddle-fix` annotated and pushed at
   `eb4b339` (ADR-0086's own closing tip, confirmed HEAD at session
   start, working tree clean), message "straddle fix landed,
   design-channel-verified 2026-08-08 (ADR-0086)". Tag did not already
   exist locally or on the remote; created fresh. **Executed Step 0.**

2. **AR-CP-1** `[C — vendoring mechanics per NOTICE/ADR-0013
   discipline, the ruled queue's own 2026-08-08 intake row]` (the
   vendoring). Fresh closure enumeration against the pin checkout
   (`/home/mg/synthea-checkout`, `7e08387c68a7f0e21d13076609a159fd473
   fc902`, verified first): `colorectal_cancer.json`'s own root file
   scanned for both `CallSubmodule` and `LookupTableTransition`
   references, finding exactly one closure member — the shared
   `anemia/anemia_sub.json` submodule, already vendored byte-identical
   at this pin since batch 2's own `hypothyroidism.json` closure
   (re-verified byte-identical before reuse, not re-copied) — and no
   lookup tables, no other data files. `colorectal_cancer.json` itself
   is the ONE genuinely new byte on disk, copied byte-verbatim (SHA-256
   `10a2e4574332650e0bf0e6f466390c704111e525be6e425f85a87fdcccc74e51`,
   2188 lines). `.gitattributes`' own `components/sim/resources/sim/
   modules/** -text` rule confirmed (not assumed) to cover the new
   path by direct grep. One NOTICE row plus a dated section landed,
   citing this ADR and the four-ADR deferral story. No `:persona-
   config` override — the module's own `Initial` state direct-
   transitions with no Gender/Race gate (confirmed by inspection of the
   pin checkout's own JSON, not by citation).

3. **AR-CP-2** `[C — population-scale gate law + the R2/AR-SF-7 counter
   precedent]` (the committed test). `vendored_colorectal_test.clj`
   mirrors `vendored_anemia_test.clj`'s own two-deftest shape: (i) the
   full compile/engine/check/emit round trip at all three of the
   investigation's own seeds (20260802, 1, 42; 300 patients), asserting
   real compiled clinical content, a clean invariant pass, and real
   rendered HL7; (ii) a pinned `:suppressed-straddle-spans` count. This
   session's own considered deviation from the anemia test's own
   interpreter-layer-only sweep, disclosed here: `:suppressed-straddle-
   spans` lives on `compile-trajectory`'s own return map (ADR-0086,
   AR-SF-7), not the interpreter layer alone — unlike
   `:suppressed-encounter-ends` (invisible to `engine/run`), `engine.clj`'s
   own `:registered` decide method calls `compile-trajectory` directly
   for every patient, so the counter is reachable through the SAME
   `engine/run` population the round-trip test above already exercises,
   via `with-redefs` interception at the `ehrt.sim-trajectory.
   interface/compile-trajectory` boundary — the exact technique the
   colorectal investigation itself used (ADR-0085, AR-CI-2). This
   measures the counter against the SAME real straddling patients the
   investigation traced by name (`PID-000239-c79b3f7f` at seed
   20260802, `PID-000038-f5560829` at seed 42), rather than a
   separately-constructed 300-walk mixed-seed sweep, which this session
   first tried and found undercounted the real (rare, ~2-of-900) branch
   entirely — see Measurement, below, for both attempts disclosed.

4. **AR-CP-3** `[C — the oracle]`. `colorectal-pair` joins `digest.clj`
   as the TWENTY-NINTH root, FIRST BASELINE, purely additive, mirroring
   `anemia-pair`'s own producer shape minus the persona-config override
   (not needed, per AR-CP-1). Bracket: `bin/regression-oracle eb4b339
   34305d9 --declared-digest-change`, declaring the additive root — all
   28 pre-existing roots IDENTICAL, `colorectal-pair` present at target
   only.

5. **AR-CP-4** `[C — STOP-AND-REPORT conditions, explicit]`. None
   fired: the closure enumerated exactly the disclosed members (no
   surprise closure member), zero violations at all three seeds, zero
   pre-existing roots moved, zero NOTICE/hash mechanism friction.

### Measurement (AR-CP-2, disclosed in full)

**First attempt, undercounted, disclosed not silently discarded.** A
300-walk-per-seed, 150-mixed-seed-times-2-sexes sweep (the anemia
test's own `mixed-seeds`/`run-walk` idiom, reused verbatim), reg-t =
DOB + 25 years, a 100-year horizon, at all three deferral seeds as
MIXER seeds: **zero** `:suppressed-straddle-spans` at every mixer seed
(20260802/1/42), 900 walks total. This methodology derives an
independent walk population per mixer seed via a different RNG path
than `engine/run`'s own per-patient seeding — colorectal's own
straddling rate is real but rare (2 of 900 patients across the
investigation's own three engine-seeded populations, ADR-0085), and
this session's own synthetic sweep simply did not sample that branch
within three tries. Named here as a real methodological finding for
future population-scale interpreter-layer counters (`:suppressed-
straddle-spans` is compile-layer-reachable, unlike the A5 arm's own
counter — the anemia test's own sweep idiom does not transfer without
adaptation), not silently abandoned.

**Second attempt, adopted.** `with-redefs` interception of
`ehrt.sim-trajectory.interface/compile-trajectory` around the SAME
`engine/run` populations the round-trip test's own three seeds already
exercise (300 patients each, `:module-horizon-days` 36500), summing
`:suppressed-straddle-spans` across all 300 compiled patients per
run:

| seed | `:suppressed-straddle-spans` (measured) |
|---|---|
| 20260802 | 1 |
| 1 | 0 |
| 42 | 1 |

Matches ADR-0085's own diagnosis exactly (1 distinct straddling
patient at seed 20260802, 0 at seed 1, 1 at seed 42) — the counter
witnesses the SAME real patients the investigation traced by name, not
a re-derived population. These three values are what
`vendored_colorectal_test.clj`'s own `suppressed-straddle-spans-is-
pinned-per-seed` deftest pins.

### Execution record

**Step 0 (no commit).** Cwd confirmed the ext4 clone
(`~/src/ehr-testing-tools`), tip `eb4b339`, working tree clean. Last
five CI runs on `main` disclosed, all `success`
(`31274667607`/`31274259259`/`31273576426`/`31269790361`/`31269357505`
— no red window). `clojure -M:poly check` OK. Oracle self-bracket
(`bin/regression-oracle eb4b339 eb4b339`): IDENTICAL, all 28 roots,
byte-for-byte. Pin checkout re-confirmed
(`/home/mg/synthea-checkout`, `git rev-parse HEAD` =
`7e08387c68a7f0e21d13076609a159fd473fc902`, working tree clean).
AR-CP-0 executed directly: `stable-20260808-straddle-fix` created
annotated at `eb4b339`, pushed, verified — peeled ref resolves exactly
both locally and via `git ls-remote`.

**Step 1/2 (`34305d9`, AR-CP-1/2/3).** `colorectal_cancer.json` copied
byte-verbatim; NOTICE gained one new row plus a dated section (verified
green post-edit: `notice-verbatim-test`, 4 tests, 145 assertions, up
from 143 — the new row's own two assertions).
`vendored_colorectal_test.clj` authored and witnessed green in-session
(2 tests, 13 assertions, 0/0) — the engine round trip at all three
seeds (real compiled content: `#{:outpatient-visit :outpatient-visit-end
:observation :procedure ...}` at every seed, `check/check-all` `:ok`
at every seed, real rendered HL7, 230 messages at seed 20260802) and
the pinned straddle-span counter (1/0/1, above). `digest.clj` gained
one new producer function (`colorectal-pair`) and one new `roots` map
entry — purely additive, every existing producer function and root
entry byte-unchanged (confirmed by diff before staging). Full suite
(`clojure -M:poly test :all skip:integration`): every project block `0
failures, 0 errors`; both new tests confirmed present in the run output
(appearing twice across project groupings, standard for this
workspace). `clojure -M:poly check` OK. `gitleaks git --staged -v`:
clean. Staging hygiene: `git diff --cached --stat` showed exactly the
four files this checkpoint touches (`digest.clj`, NOTICE, the new test,
the new module file) — nothing else staged.

The official standing harness, `bin/regression-oracle eb4b339 34305d9
--declared-digest-change`, reported `DIFFERS` — EXPECTED, per the
ADR-0070/0071/0072/0083 precedent: the diff shows exactly one ADDED
line (`colorectal.edn`) and ZERO removed or changed lines among the 28
pre-existing roots.

Commit `34305d9` ("feat: colorectal comes home — three arcs deferred,
one diagnosis and one fix later, pinned forever (colorectal payoff,
AR-CP-1/2/3)"). Pushed; post-push verification (`git log --format=%B
-1` diffed against the source message file): one delta, the known
trailing-blank-line artifact. CI watched to conclusion: run
`31276085600`, `success`, 3m13s.

**Step 3 (this record).** `notes/adr/0087-colorectal-payoff.md`
authored directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own stale file count corrected (84→85,
verified by `ls notes/adr/*.md | grep -v README | wc -l`, not
arithmetic); roadmap's "Colorectal vendoring payoff" Next row removed
(executed by this session) and a Done pointer
(`- 2026-08-08 — colorectal-payoff — ADR-0087`) added; session record
and prompt archive land in the same commit as this record's own
citation-index update.

### Verification

- `bin/regression-oracle eb4b339 34305d9 --declared-digest-change`:
  `DIFFERS`, EXPECTED — one added root, zero changed/removed among the
  28 pre-existing ones (the diff output itself is the evidence, not a
  count comparison).
- `vendored_colorectal_test.clj`: witnessed GREEN in-session (2 tests,
  13 assertions, 0/0) — real compiled content, zero invariant-catalog
  violations at all three seeds, real rendered HL7, and the pinned
  `:suppressed-straddle-spans` totals (1/0/1) reproduced live via
  `with-redefs` interception, not guessed.
- `ehrt.docs-tooling.notice-verbatim-test`: green, 4 tests, 145
  assertions (up from 143).
- Full suite (`clojure -M:poly test :all skip:integration`): green
  throughout, 0 failures/0 errors, confirmed by grepping the entire run
  output (not just the tail) for any non-zero failure/error count.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, this session's one commit.
- Post-push message verification: one delta, the known harmless
  trailing-blank-line artifact.
- Tag verification: `stable-20260808-straddle-fix` peeled ref resolves
  to `eb4b339` exactly, both locally and via `git ls-remote`.
- NOTICE hash cross-check: the new SHA-256 re-derived by fresh
  `sha256sum` against the vendored bytes and matched against the table
  before commit.
- CI: last-five on `main` at session start disclosed above (five
  green, no red window); this session's own push watched to
  conclusion, `success`, 3m13s.

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260808-colorectal-payoff` at THIS session's own closing tip**
— the same tag-law case (ii) pattern every prior close in this repo
has used for its own predecessor.

### Index line

```
- 2026-08-08 — colorectal-payoff — ADR-0087
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 84→85, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

The colorectal thread closes: deferred at vendoring batch 3 (ADR-0072)
on a diagnosis-by-adjacency, reclassified under its own true name
(ADR-0083), diagnosed to a real compile-layer gap (ADR-0085), fixed
structurally (ADR-0086), and vendored (this session) — four ADRs and
three sessions after the first deferral, closing this repo's second
oldest live vendoring deferral (after `injuries.json`, still blocked by
its own unrelated `gmf-interpreter` runaway-loop gap). The vendored-
module count rises to twenty-nine content-producing engine-layer
oracle roots; the straddle fix's own `:suppressed-straddle-spans`
counter now has a committed witness beyond the fix session's own
synthetic unit tests, tying it to a real, population-scale, named
patient population. Untouched, carried forward from ADR-0086's own
horizon note: the carry-across emission row's own compile-layer half
(shape (a), still deferred), the pairing-as-data registry session,
Wave E's risk-attribute/vital-sign register, vendoring batch 4 (the
veteran family), the census closure-count refinement, publish-prep
(F-5/F-6 + F-7), review 2, `sim-emit-cda`, the fixture-relocation and
ADR-footnote Next rows, and the sleep-apnea latent-defect intake named
for review 2.

### Consequence

The population-scale gate's own standing law — a module joins the tree
only with a witnessed content-producing engine-layer round trip, a
census verdict is evidence for curation never a vendoring license —
lands its cleanest demonstration yet: three separate sessions'
diagnostic work (ADR-0083's reclassification, ADR-0085's localization,
ADR-0086's fix) converge into one committed test that a future
regression in the straddle gate would break as a moved integer, not a
silent pass. The measurement section's own disclosed first attempt (a
methodology that works for one counter but silently undercounts
another) is this session's own instance of the arc's recurring lesson:
a technique proven for one gap does not transfer to a structurally
different one without re-verification.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Colorectal payoff: the twenty-ninth root, and the straddle counter finds its witness — `colorectal_cancer.json` vendors clean at last (four ADRs, three sessions after its first deferral), pinned by a committed test asserting a clean invariant pass plus a `:suppressed-straddle-spans` count measured via `with-redefs` interception against the SAME real straddling patients the investigation traced by name (1/0/1 across seeds 20260802/1/42) — a first, undercounting synthetic-sweep attempt disclosed rather than discarded; the oracle gains its 29th root, the 28 pre-existing ones byte-identical

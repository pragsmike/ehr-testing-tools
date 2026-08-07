## ADR-0070 — Vendoring batch 1: the everyday ambulatory load — five ailments join the mix, one deferred

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: the census substance qualifier landed (`notes/adr/0069-census-
substance.md`, tip `cd16fa9`), ranking the 84 `:ok-walked` modules 51
`:zero-on-every-seed` / 33 `:produces-content`. The design channel ran
a curation pass over that ranked catalog (design channel, 2026-08-07)
and the author CONCURRED with a batched plan. This session is the
vendoring arc's second, executing batch 1.

Read-first: `components/sim/resources/sim/modules/NOTICE` in full;
`components/sim/src/ehrt/sim/run.clj` `resolve-modules`;
`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_tjr_test.clj`
and `vendored_ear_infections_test.clj`; `components/oracle/src/ehrt/
oracle/digest.clj` header and its ADR-0033 AR-4b dated note; the
substance artifact's own rows for the six batch-1 candidates
(`components/sim-trajectory/docs/census/2026-08-07-synthea-7e08387-
substance.edn`); roadmap Deferred "Vital-sign channel" row.

### The curation plan (recorded verbatim per AR-VB1-1)

Design-channel curation pass, 2026-08-07, over the substance artifact
(tally re-derived from the artifact itself: 33 produces-content / 51
zero-on-every-seed). The author CONCURRED with the batched plan:

- **Batch 1 (this session): the everyday ambulatory load** — asthma
  (closure 3 files, peak 87 events), bronchitis (1, 22), sleep-apnea
  (1, 116), injuries (8, 134), fibromyalgia (1, 38), dementia (1, 11).
- **Batches 2–4: held for later ruling.** Batch 2 is named as "the
  chronic clinic tail" — its own full composition, and batches 3–4's
  own composition, await the author's own future ruling; not
  enumerated further here because they were not enumerated further in
  the concurrence this ADR records.
- **Wellness-encounters: a NAMED DESIGN ITEM, never routine
  vendoring** — it is upstream's own wellness machinery and collides
  with this engine's own wellness-cadence design; waits its own pass.
- **CHF/contraceptives: held for after Wave E.**
- **Homelessness: unbatched.**

The design channel's own chat is not the record; this section is.

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-VB1-0 `[A — tag law, case (ii); debt recorded in ADR-0069]`.**
Annotated `stable-20260807-census-substance` at `cd16fa9`, message
"census substance landed, design-channel-verified 2026-08-07
(ADR-0069)"; pushed; peeled ref verified — resolves exactly to
`cd16fa9`.

**AR-VB1-1 `[A — the author's concurrence with the curation plan,
design channel 2026-08-07; the batch composition is the channel's,
concurred]`.** This ADR records the curation plan (above) and the
concurrence — this section, not the design channel's chat, is the
record.

**AR-VB1-2 `[C — vendoring mechanics per NOTICE/ADR-0013 discipline]`
(the vendoring).** Executed for five of six candidates; the sixth
(`injuries.json`) was assessed and DEFERRED WHOLE (AR-VB1-6, below).
Full closure enumeration, byte-verbatim copy, SHA-256, and NOTICE rows
— see Execution record.

**AR-VB1-3 `[C — the round-trip gate per ADR-0033 pattern]` (the
tests).** One `vendored_<module>_test.clj` per landed module,
red-before-resource, green-after. See Execution record.

**AR-VB1-4 `[C — oracle mechanics per ADR-0033 AR-4b precedent]` (the
roots).** Five new engine-layer roots joined `digest.clj`, each a
FIRST BASELINE. The existing eleven batches verified byte-identical
across this session's entire span (both a manual pre/post digest
comparison and the official `bin/regression-oracle` bracket). See
Verification.

**AR-VB1-5 `[C — the rider, from the curation pass's own artifact
re-derivation]` (vital-sign row correction).** Landed Step 1,
`0dc3850`: the roadmap's Deferred "Vital-sign channel" row gained a
dated note — `congestive-heart-failure` `[0 117 0]` and
`contraceptives` `[0 89 0]` both `:produces-content` (post-Wave-VS),
`covid19` `[0 0 0]` `:zero-on-every-seed`, still fully blocked. Trigger
unchanged.

**AR-VB1-6 `[C — scope]` (fences + oracle).** Held exactly: no
module-content edits, ever; no loader/interpreter/engine/emitter
changes (the bail-out precedent fired for `injuries.json`, below); no
default-config changes (the five new modules are AVAILABLE, not
default); no batch-2/3/4 modules attempted; standing untracked files
untouched.

### `injuries.json`: assessed, DEFERRED WHOLE (the bail-out precedent)

`injuries.json`'s own closure (census substance artifact:
`:closure-file-count 8`) resolves to root plus seven called
submodules, of which FOUR are already vendored, byte-identical at this
same pin, from the ear-infections and total-joint-replacement closures
(`medications/ear_infection_antibiotic.json`, `medications/otc_pain_
reliever.json`, `medications/moderate_opioid_pain_reliever.json`,
`dme/wheelchair_end.json`) — re-verified byte-identical against the
pin before reuse, not re-vendored, no new NOTICE row for them. The
four genuinely NEW closure members (`injuries.json` itself,
`injuries/broken_jaw.json`, `snf/skilled_nursing_facility.json`,
`dme/wheelchair.json`) were vendored, then a round-trip test written
and run — `engine/run` threw `ehrt.sim-trajectory.gmf-interpreter:
run-submodule exceeded max-steps` at `:check-for-dental-visit`, at
every one of three `:module-horizon-days` values tried (36500, 18250,
3650) — ruling out a horizon-tuning fix.

Root cause: `injuries/broken_jaw.json`'s own `Dental Referral` state
sets the `dental_referral` attribute once (`SetAttribute`) and no
state anywhere in the file ever clears it; `Check for Dental Visit`
loops with `Wait for Dental Visit` (a 1–7 day `Delay`) for as long as
`dental_referral` stays set — i.e., permanently, once reached. Direct
interpreter-layer probing (bypassing `engine.clj` entirely, `run-
module` called directly at the census's own exact parameters —
registration age 30, 50-year horizon) reproduced the SAME exception on
4 of 120 well-mixed-seed walks (~3.3%). The census's own three-seed
sample (`gap {:walk-errors []}` for `injuries`) simply never sampled
this branch — with a 300-patient engine population, hitting it at
least once is a near-certainty (`(1-0.033)^300 ≈ 5×10⁻⁵` of avoiding
it entirely), so no seed choice at that population size dodges it.

This is the AR-VB1-6 bail-out trigger exactly as named: the closure
trips a `gmf-interpreter` gap the census didn't, and per the fence, no
loader/interpreter/engine change lands this session to fix it.
`injuries.json` and its four new-only closure members are NOT
vendored — no NOTICE row, no test, no oracle root. Full finding
recorded in `components/sim/resources/sim/modules/NOTICE`'s own dated
section. Revisit trigger: a future session willing to extend
`gmf-interpreter`'s own runaway-loop handling (e.g. detecting an
attribute-gated self-transition pair that never clears its own gate).

### Expected-count disclosure (AR-VB1-2)

The session prompt's own estimate — "≈15 files (3+1+1+8+1+1)" — summed
the census artifact's own `:closure-file-count` per module, a metric
that counts JSON modules only, NOT lookup-table data files. The real
composition landed is materially different in SHAPE, though it lands
at the SAME total by coincidence:

| Module | Estimate basis | Actual NEW files landed |
|---|---|---|
| asthma | 3 (root + 2 submodules) | **11** (3 JSON + 8 lookup-table CSVs the estimate didn't count — `lookup_table_transition`-driven therapeutic content, D3a/H2's own mechanism) |
| bronchitis | 1 | 1 |
| sleep-apnea | 1 | 1 |
| injuries | 8 | **0** (deferred whole; of the 8-file closure, 4 were already vendored from prior batches and needed no re-landing, and the closure was never vendored at all — see above) |
| fibromyalgia | 1 | 1 |
| dementia | 1 | 1 |
| **Total** | **15** | **15** |

Disclosed per AR-VB1-2's own instruction, not silently absorbed: the
matching total is coincidental, not confirmation the estimate's own
reasoning held.

### Execution record

**Step 0 (preflight + tag).** Cwd confirmed the ext4 clone, tip
`cd16fa9`, working tree clean. Synthea checkout pin-verified
(`/home/mg/synthea-checkout`, `git rev-parse HEAD` = `7e08387c...f902`,
matching `census.clj`'s own `synthea-pin`, working tree clean).
Baseline: `clojure -M:poly check` OK; full suite green (`clojure -M:poly
test`, 511 assertions total across sim-trajectory/sim-engine/etc., 0
failures/0 errors); oracle pre-digest (manual, direct `ehrt.oracle.
interface` invocation against the clean tree, all eleven roots)
recorded to a scratch manifest for later comparison. AR-VB1-0 executed
directly: `stable-20260807-census-substance` created annotated at
`cd16fa9`, pushed, verified — peeled ref resolves exactly.

**Step 1 (`0dc3850`, AR-VB1-5).** Roadmap's Deferred "Vital-sign
channel" row gained the dated note (CHF/contraceptives/covid19
current-evidence citation, above). Post-push verification: one delta
against the message file, the known trailing-newline artifact.

**Step 2 (`4f90ce8`, AR-VB1-2/3).** Module by module, red (missing
classpath resource, `IllegalArgumentException: Cannot open <nil> as a
Reader`) then green:

| Module | Closure | Config | First-try result |
|---|---|---|---|
| dementia | 1 file | seed 20260802, 300 patients, 36500-day horizon | green, first try |
| bronchitis | 1 file | same | green, first try |
| sleep-apnea | 1 file | same | green after fixing the test's own expected-kinds assertion (`:outpatient-visit`/`:outpatient-visit-end`, not `:encounter`/`:encounter-end` — a test-authoring correction, not a module or interpreter issue) |
| fibromyalgia | 1 file | same | green, first try |
| asthma | 3 modules + 8 lookup tables | same | green, first try |
| injuries | 8-file closure | three horizons tried | DEFERRED WHOLE (above) |

All five landed modules verified together (`clojure -M:dev:test`,
5 tests, 20 assertions, 0/0), then via `clojure -M:poly test` with the
new files staged (staging was necessary for poly's own change
detection to register `sim-emit-hl7` as affected — an untracked-only
change to a brick's test directory does not register otherwise, an
operational finding worth naming for a future session) — every project
(`conformance`, `ehrt-cli`, `integration`) green, 0 failures/0 errors
throughout. `clojure -M:poly check` OK. NOTICE gained fifteen new rows
plus a dated section (the five-landed/one-deferred narrative and the
full `injuries.json` finding); every hash cross-checked by fresh
`sha256sum` against the table before commit. Post-push verification:
one delta, the known trailing-newline artifact.

**Step 3 (`be59ace`, AR-VB1-4).** `digest.clj` gained five new
producer functions (`asthma-pair`/`bronchitis-pair`/`sleep-apnea-
pair`/`fibromyalgia-pair`/`dementia-pair`) and five new `roots` map
entries — purely additive, every existing producer function and root
entry byte-unchanged (confirmed by diff before staging). Manual
pre/post comparison (Step 0's own scratch manifest vs. a fresh run
against the edited tree) showed the eleven pre-existing roots' own
`.edn` output byte-identical. The official standing harness,
`bin/regression-oracle 4f90ce8 be59ace --declared-digest-change` (the
flag required since `digest.clj`'s own body differs outside its `(ns
...)` form, purely additively), reported `DIFFERS` (exit 1) — EXPECTED,
per the Wave H pre-roll precedent (`digest.clj`'s own dated note,
2026-08-04, ADR-0042 AR-5): the diff shows exactly five ADDED lines
(`asthma.edn`/`bronchitis.edn`/`dementia.edn`/`fibromyalgia.edn`/
`sleep-apnea.edn`) and ZERO removed or changed lines — every one of
the eleven pre-existing root hashes identical between baseline and
target. `clojure -M:poly check` OK (no test brick covers `digest.clj`
directly; it is dev equipment, its own only real caller is `bin/
regression-oracle`'s per-worktree classpath).

**Step 4 (this record).** `notes/adr/0070-vendoring-batch-1.md`
authored directly; index line appended to `notes/ADRs.md`; roadmap's
"Now" section updated to name this session's own close and the
successor-tag debt; Done pointer (`- 2026-08-07 — vendoring-batch-1 —
ADR-0070`) added in the same commit as the index line; session record
and prompt archive land in the same commit.

This session's own successor tag debt: `stable-20260807-vendoring-
batch-1` at this session's own closing tip is owed to the next
session's own Step 0, per tag law (ADR-0057 AR-T-1) — not created here
(no ruling licensed it at this session's own closing commit).

### Verification

- `bin/regression-oracle 4f90ce8 be59ace --declared-digest-change`:
  `DIFFERS` (exit 1), EXPECTED — five added roots, zero changed/removed
  among the eleven pre-existing ones (see Step 3, above; the diff
  output itself is the evidence, not a count comparison).
- Manual pre/post digest comparison (Step 0 baseline vs. post-Step-3
  tree, direct `ehrt.oracle.interface` invocation, no worktree): the
  eleven pre-existing roots' `.edn` manifests byte-identical.
- Full suite (`clojure -M:poly test`): green at the Step 0 baseline
  (511 assertions, 0/0) and again after Step 2, with the new
  sim-emit-hl7 vendored tests staged so poly's own change detection
  picked them up (every project green, 0/0 throughout).
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-census-substance` peeled ref
  resolves to `cd16fa9` exactly.
- NOTICE hash cross-check: all fifteen new SHA-256 values re-derived
  by fresh `sha256sum` against the vendored bytes and matched against
  the table before commit (this record's own re-check, and named as
  the design channel's own next-pass spot-check target).

### Deviations, disclosed

- **`injuries.json` deferred whole** — see the dedicated section
  above; the largest single deviation from the session prompt's own
  stated premise (six modules landing), disclosed in full, not
  silently absorbed. The batch commit message and NOTICE both name it.
- **Expected-file-count divergence** — see the dedicated section
  above; total landed matches the prompt's own estimate (15) by
  coincidence, not because the estimate's own per-module reasoning
  held.
- **`poly test`'s own change-detection gap on untracked-only test
  additions** — a new test file with no corresponding change to any
  already-tracked file in the same brick does not register that brick
  as "changed" for `poly test`'s own incremental scoping, so the new
  tests silently do not run until `git add`ed. Worked around this
  session by staging before every `poly test` invocation; named here
  as an operational finding for future sessions, not fixed (out of
  this session's own scope — no tooling change licensed).
- **`sleep_apnea.json`'s compiled event kinds** — the test's own first-
  draft assertion expected `:encounter`/`:encounter-end`; the real
  compiled kinds are `:outpatient-visit`/`:outpatient-visit-end`
  (matching every other landed module's own actual output) — a test-
  authoring correction made and disclosed here, not a module or
  interpreter finding.

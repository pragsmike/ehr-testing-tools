# 2026-08-07 — Vendoring batch 1: the everyday ambulatory load — five ailments join the mix

## Scope

Session prompt naming AR-VB1-0 through AR-VB1-6, executing the
vendoring arc's second session — batch 1 of the design channel's own
curated plan (author concurrence, design channel 2026-08-07). Vendors
the everyday ambulatory load the substance census (ADR-0069) ranked as
content-producing: asthma, bronchitis, sleep-apnea, injuries,
fibromyalgia, dementia. Full account, rulings, the curation plan text,
the `injuries.json` finding, and the parity/oracle verification:
`notes/ADRs.md` ADR-0070.

Step 0 (preflight) confirmed the working directory is the ext4 clone,
tip `cd16fa9`, working tree clean, Synthea checkout pin-verified.
Baseline: `clojure -M:poly check` OK; full suite green (`clojure
-M:poly test`, 511 assertions, 0/0); oracle pre-digest (manual, direct
`ehrt.oracle.interface` invocation) recorded for all eleven roots.
AR-VB1-0 executed directly: `stable-20260807-census-substance` created
annotated at `cd16fa9`, pushed, verified — peeled ref resolves exactly.

Step 1 (`0dc3850`, AR-VB1-5) landed the roadmap's Deferred "Vital-sign
channel" row dated note: the substance census shows
`congestive-heart-failure` `[0 117 0]` and `contraceptives` `[0 89 0]`
now `:produces-content` (post-Wave-VS), `covid19` `[0 0 0]` alone still
`:zero-on-every-seed` — the row's blocked-citation updates to the
current evidence, trigger unchanged.

Step 2 (`4f90ce8`, AR-VB1-2/3) vendored five of six candidates,
module by module, red (missing classpath resource) then green (real
compiled clinical content, real rendered HL7): dementia, bronchitis,
fibromyalgia, and asthma each green on the first population/horizon
choice (seed 20260802, 300 patients, a 100-year `:module-horizon-days`
— the established `vendored-sepsis-test`/`vendored-uti-test`
convention); sleep-apnea needed one test-assertion correction (the
real compiled kinds are `:outpatient-visit`/`:outpatient-visit-end`,
not `:encounter`/`:encounter-end`). `injuries.json` was assessed and
DEFERRED WHOLE: `engine/run` threw `gmf-interpreter`'s own `max-steps`
exception on `injuries/broken_jaw.json`'s own `Check for Dental
Visit`/`Wait for Dental Visit` loop (a `dental_referral` attribute set
once, never cleared) at every one of three horizons tried; direct
interpreter-layer probing (bypassing `engine.clj`, the census's own
exact code path and parameters) reproduced the same exception on 4 of
120 well-mixed-seed walks — a real gap the census's own narrow
three-seed sample never sampled, not fixable by horizon tuning, and
out of scope to fix under this session's own fence (no loader/
interpreter/engine changes licensed). Fifteen new files landed (three
asthma JSON files plus eight lookup-table CSVs the census's own
`:closure-file-count` metric doesn't count, plus four single-file
modules) — a materially different composition than the session
prompt's own "≈15 (3+1+1+8+1+1)" estimate, landing at the same total
by coincidence (full disclosure: ADR-0070's own "Expected-count
disclosure" section). NOTICE gained fifteen new rows plus a dated
section recording both the landed five and the `injuries.json`
finding in full; every hash cross-checked by fresh `sha256sum` before
commit, and again before this record. `clojure -M:poly test` (files
staged, necessary for poly's own change detection to register
`sim-emit-hl7` as affected) green across every project, 0/0
throughout; `clojure -M:poly check` OK.

Step 3 (`be59ace`, AR-VB1-4) added five new engine-layer roots to
`digest.clj` — `asthma-pair`/`bronchitis-pair`/`sleep-apnea-pair`/
`fibromyalgia-pair`/`dementia-pair` — purely additive, every existing
producer function and root entry byte-unchanged. The official
`bin/regression-oracle 4f90ce8 be59ace --declared-digest-change`
bracket reported `DIFFERS` (exit 1) — EXPECTED, matching the Wave H
pre-roll precedent for an additive digest.clj change: the diff shows
exactly five added lines and zero changed/removed lines among the
eleven pre-existing roots.

Step 4 (this record) authored `notes/adr/0070-vendoring-batch-1.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own stale file count (67→68, verified by
`ls`), updated the roadmap's "Now" section (successor tag debt named,
batch 2's own status recorded) and added the Done pointer
(`- 2026-08-07 — vendoring-batch-1 — ADR-0070`) in the same commit as
the index line, archived this prompt, and recorded this session.

## Red→green evidence highlights

Every landed module's own red was the same shape:
`IllegalArgumentException: Cannot open <nil> as a Reader` — the
vendored test's own `(slurp (io/resource "sim/modules/<file>.json"))`
resolving `nil` before the module existed on the classpath, witnessed
BEFORE the module file landed, per checkpoint order within Step 2.
Green followed the verbatim copy in every case; sleep-apnea's own
green needed one round of test-assertion correction first (the
expected-kinds set), not a resource or interpreter change.

`injuries.json`'s own red never turned green: this is the session's
one genuine STOP-AND-REPORT, not a red-then-green pair — the finding
is recorded in full in NOTICE and ADR-0070, not silently dropped.

## Judgment calls and their ratification status

- **Population/horizon convention reused without a fresh empirical
  search for four of five landed modules** — seed 20260802, 300
  patients, 36500-day horizon, the same values `vendored-sepsis-test`/
  `vendored-uti-test` already established as this repo's own working
  default. Channel-inferred (AR-VB1-3's own "the seed hunt is part of
  the work" language allows reusing a proven default rather than
  re-deriving one per module when it already works on the first try).
- **`injuries.json`'s deferral verified at TWO layers before
  concluding it was a genuine gap, not a config-tuning miss** — three
  different `:module-horizon-days` values tried through `engine/run`
  first (ruling out horizon tuning), THEN a direct interpreter-layer
  probe at the census's own exact code path and parameters (120
  well-mixed walks) to confirm the gap is real and independent of
  `engine.clj`'s own fixed-registration-anchor design, not an artifact
  of it. Channel-inferred diligence beyond the letter of AR-VB1-6's
  own "different code path" phrasing — judged necessary to make an
  honest STOP-AND-REPORT rather than a hasty one.
- **Staging files before each `poly test` invocation mid-session** —
  an operational workaround for `poly test`'s own change-detection gap
  on untracked-only test-file additions (named in ADR-0070's own
  Deviations section). Channel-inferred, not itself a fix to that gap.

## Verification block (for the record)

- `bin/regression-oracle 4f90ce8 be59ace --declared-digest-change`:
  `DIFFERS` (exit 1), EXPECTED — five added roots, zero changed/
  removed among the eleven pre-existing ones.
- Full suite (`clojure -M:poly test`): green at the Step 0 baseline
  and again after Step 2 (files staged), every project, 0/0
  throughout.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-census-substance` peeled ref
  resolves to `cd16fa9` exactly.
- NOTICE hash cross-check: all fifteen new SHA-256 values re-derived
  by fresh `sha256sum` and matched, twice (before commit, and again
  authoring this record).

## Deviations, disclosed

Full account in `notes/adr/0070-vendoring-batch-1.md`'s own
"Deviations, disclosed" section: `injuries.json` deferred whole; the
expected-file-count divergence (same total, different composition);
`poly test`'s own change-detection gap on untracked-only test
additions; the sleep-apnea test-assertion correction.

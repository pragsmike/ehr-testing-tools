# 2026-08-08 — Colorectal payoff: the twenty-ninth root, and the straddle counter finds its witness

## Scope

Session prompt naming AR-CP-0 through AR-CP-4, executing the roadmap's
own Next-section intake row "Colorectal vendoring payoff" (entered by
ADR-0086, the straddle fix): `colorectal_cancer.json` was clean at all
three of the colorectal investigation's own seeds post-fix but not yet
vendored — the fix session's own fence held. This session vendors the
module's own closure, authors a committed round-trip test pinning both
an invariant-catalog pass and the straddle fix's own
`:suppressed-straddle-spans` counter, lands `colorectal-pair` as
`digest.clj`'s 29th root, and records the four-ADR, three-session arc
this module traveled before landing.

Preflight: working directory confirmed the ext4 clone, HEAD `eb4b339`
exactly (the straddle fix, ADR-0086), branch up to date with
`origin/main`, working tree clean. `clojure -M:poly check`: OK. Oracle
self-bracket (`bin/regression-oracle eb4b339 eb4b339`): all 28 roots
IDENTICAL, byte-for-byte, both sides the same commit. Last five CI runs
on `main` all `success`: `31274667607` (`eb4b339`), `31274259259`,
`31273576426`, `31269790361`, `31269357505` — no red window.

## Step 0 — Tag (AR-CP-0)

`stable-20260808-straddle-fix` did not already exist locally or on the
remote. Created annotated at `eb4b339`, message "straddle fix landed,
design-channel-verified 2026-08-08 (ADR-0086)"; pushed; peeled ref
verified — resolves exactly to `eb4b339` both locally and via
`git ls-remote origin refs/tags/stable-20260808-straddle-fix^{}`. No
commit this step, per the prompt.

## Step 1 — Vendor (AR-CP-1)

Fresh closure enumeration against the pin checkout
(`/home/mg/synthea-checkout`, pin
`7e08387c68a7f0e21d13076609a159fd473fc902`, verified first via direct
`git rev-parse HEAD`, working tree clean): `colorectal_cancer.json`'s
own root file scanned (Python, both `CallSubmodule` targets and
`LookupTableTransition` names) for every closure member.

| Member | Status |
|---|---|
| `colorectal_cancer.json` | NEW — copied byte-verbatim, SHA-256 `10a2e4574332650e0bf0e6f466390c704111e525be6e425f85a87fdcccc74e51`, 2188 lines |
| `anemia/anemia_sub.json` | Already vendored (batch 2, `hypothyroidism.json`'s own closure) — re-verified byte-identical against the pin (`bde888cb...`) before reuse, not re-copied |

No lookup tables, no other data files (confirmed by a direct grep of
both files for `.csv`/`.json` string literals beyond the one
`CallSubmodule` target already named). `.gitattributes`' own
`components/sim/resources/sim/modules/** -text` rule confirmed by
direct grep to cover the new path. One NOTICE row plus a dated section
landed. `colorectal_cancer.json`'s own `Initial` state confirmed by
direct inspection of the pin checkout's JSON to `direct_transition`
with no Gender/Race gate — no `:persona-config` override needed.

## Step 2 — Pin it (AR-CP-2/3), commit `34305d9`

**The committed test** (`vendored_colorectal_test.clj`, mirroring
`vendored_anemia_test.clj`'s own two-deftest shape): witnessed green
in-session, 2 tests, 13 assertions, 0 failures/0 errors.

- **Engine round trip**, all three of the investigation's own seeds
  (20260802, 1, 42), 300 patients each, `:module-horizon-days` 36500:
  real compiled clinical content at every seed (observed kinds
  included `:outpatient-visit`/`:outpatient-visit-end`/`:observation`/
  `:procedure` at every seed, plus `:diagnostic-report`/
  `:medication-order`/`:care-plan-end` at some), `check/check-all`
  `:status :ok` (0 violations) at every seed, real rendered HL7 (230
  messages at seed 20260802).
- **The straddle counter, measured not guessed (AR-CP-2).** First
  attempt (the anemia test's own mixed-seed interpreter-layer sweep,
  verbatim): **zero** `:suppressed-straddle-spans` across 900 walks (3
  mixer seeds x 300 walks) — undercounted, disclosed as a
  methodological finding, not silently reported as the pin (full
  reasoning in `notes/adr/0087-colorectal-payoff.md`'s own Measurement
  section and the archived prompt's own deviation record). Adopted
  instead: `with-redefs` interception of
  `ehrt.sim-trajectory.interface/compile-trajectory` around the SAME
  three-seed, 300-patient `engine/run` populations the round-trip test
  above already exercises — measured **1 / 0 / 1** across seeds
  20260802/1/42, matching the colorectal investigation's own diagnosis
  (ADR-0085) exactly: the same two named patients
  (`PID-000239-c79b3f7f` at seed 20260802, `PID-000038-f5560829` at
  seed 42). These three values are what the committed
  `suppressed-straddle-spans-is-pinned-per-seed` deftest pins.

**NOTICE:** one new row plus a dated section; `notice-verbatim-test`
re-run green, 4 tests, 145 assertions (up from 143, the new row's own
two assertions).

**`digest.clj`:** one new producer function (`colorectal-pair`) and one
new `roots` map entry — purely additive, every existing producer
function and root entry byte-unchanged (confirmed by diff before
staging).

**Staging hygiene:** `git diff --cached --stat` reviewed before
committing — exactly the four intended files (`digest.clj`, NOTICE, the
new test, the new module file), nothing else staged.

**Full suite** (`clojure -M:poly test :all skip:integration`): every
project block 0 failures/0 errors, confirmed by grepping the ENTIRE run
output (not just the tail) for any non-zero failure/error count — none
found. Both new tests confirmed present in the run output (appearing
twice across project groupings, standard for this workspace).
`clojure -M:poly check`: OK. `gitleaks git --staged -v`: clean.

**Oracle bracket** (`bin/regression-oracle eb4b339 34305d9
--declared-digest-change`): manifest diff confined to exactly one ADDED
line — `colorectal.edn`, `85f57ba3...` — all 28 pre-existing roots
byte-identical, matching AR-CP-3's own bar exactly.

Committed `34305d9` ("feat: colorectal comes home — three arcs
deferred, one diagnosis and one fix later, pinned forever (colorectal
payoff, AR-CP-1/2/3)"). Pushed; post-push verification: one delta, the
known trailing-blank-line artifact. CI watched to conclusion: run
`31276085600`, `success`, 3m13s.

## Step 3 — Record (`afed003`)

`notes/adr/0087-colorectal-payoff.md` authored in full (the closure
member table, both measurement attempts disclosed side by side, the
bracket manifest, the four-ADR/three-session arc narrative). `notes/
ADRs.md` gained its index line. `notes/adr/README.md`'s own file count
corrected 84→85 (`ls notes/adr/*.md | grep -v README | wc -l`, not
arithmetic). Roadmap: the "Colorectal vendoring payoff" Next-section
row removed (executed by this session — no double-closure of the
already-CLOSED Deferred row, which moved to Done under its own notes at
ADR-0086's own session); the Done pointer (`- 2026-08-08 —
colorectal-payoff — ADR-0087`) added.

`clojure -M:poly check`: OK. `ehrt.docs-tooling.done-pointer-adr-test`
re-run green before staging: 4 tests, 4 assertions. `git diff --cached
--stat` reviewed: exactly the four intended files (`roadmap.md`,
`ADRs.md`, `README.md`, the new ADR file). Full suite run before
committing: clean, 0 failures/0 errors, confirmed by the same full-log
grep as Step 2. `gitleaks git --staged -v`: clean.

Committed `afed003` ("docs: the colorectal payoff recorded — the
twenty-ninth root, and the straddle counter finds its witness
(ADR-0087)"). Pushed; post-push verification: one delta, the known
trailing-blank-line artifact. CI watched to conclusion: run
`31276534167`, `success`, 3m37s.

## Step 4 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-08-colorectal-payoff.md` (the
driving prompt, archived verbatim, with a deviation record covering the
counter-sweep methodology switch) land together, indexed in both
READMEs' own entry lists.

## Successor tag debt

Recorded in `notes/adr/0087-colorectal-payoff.md`: the next session
that opens fresh work tags `stable-20260808-colorectal-payoff` at this
session's own closing tip.

## Judgment calls and their ratification status

- **The counter-sweep methodology switch (AR-CP-2).** Not separately
  ratified — the prompt licensed "a well-mixed seed sweep" without
  naming the exact mechanism, and this session's own first, literal
  attempt (mirroring the anemia test's interpreter-layer idiom)
  measured zero, an undercounting result disclosed rather than
  silently adopted as the pin. The switch to `with-redefs` interception
  against the round-trip test's own populations — reusing the
  colorectal investigation's own technique (ADR-0085, AR-CI-2) — is
  this session's own judgment call, justified in full in ADR-0087's own
  Measurement section: it measures the SAME real straddling patients
  the investigation traced by name, rather than a differently-seeded
  synthetic re-sample that missed them.
- **No `:persona-config` override.** Confirmed by direct inspection of
  `colorectal_cancer.json`'s own `Initial` state (a plain
  `direct_transition`, no Gender/Race `And` gate) rather than assumed
  from ADR-0082's own general finding — matching the prompt's own
  AR-CP-1 instruction to confirm, not assume.

## Findings, disclosed not acted

- **The mixed-seed sweep's own transfer failure.** A methodology
  proven for one counter (`:suppressed-encounter-ends`, interpreter-
  layer-only, the anemia test's own idiom) does not transfer
  automatically to a structurally different one
  (`:suppressed-straddle-spans`, compile-layer, reachable through
  `engine/run` directly) — the first attempt's own zero-count result
  was real but methodologically blind to the branch it was meant to
  witness, not a sign the branch doesn't exist. Named in ADR-0087 as a
  finding for any future population-scale interpreter-layer counter
  work, not merely absorbed into the final pin.

## HEAD landed

`afed003` (Step 3's own commit — Step 4's own commit lands after this
record, in the same push as the prompt archive).

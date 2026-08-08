# 2026-08-08 — Colorectal investigation: the straddling encounter, named

## Scope

Session prompt naming AR-CI-0 through AR-CI-5, executing the roadmap's
own oldest live Deferred row — `colorectal_cancer.json`'s own
`:clinical-content-only-when-admitted` gap, true name, undiagnosed
(ADR-0083's own erratum). Diagnosis-only, per the design channel's own
2026-08-08 ruling ("Concur. go."): no `src/`/`test/`/`deps.edn` edit
anywhere in the workspace this session, no vendoring, no interpreter/
compile/engine change. This session's own product is
`notes/adr/0085-colorectal-investigation.md` — knowledge, not code.

Preflight: working directory confirmed the ext4 clone (`uname -a`:
`penny`, WSL2), HEAD `45eb2f4` exactly (the fidelity arc close,
ADR-0084), branch up to date with `origin/main`, working tree clean.
`git status --porcelain --ignored=matching` disclosed only standing
gitignored build artifacts (`.cpcache/`, `.clj-kondo/.cache/`, `.lsp/
.cache/`, `out/`, `target/`, `.claude/scheduled_tasks.lock`,
`.claude/settings.local.json`) — no `config/busy-weekday.md`, nothing
requiring disclosure beyond this list. `clojure -M:poly check`: OK.
Oracle pre-digest (`bin/regression-oracle 45eb2f4 45eb2f4`): all 28
roots IDENTICAL, byte-for-byte, both sides the same commit — as
expected for a self-bracket. Last five CI runs on `main`
(`gh run list --limit 5 --branch main`) all `success`: `31266927895`
(`45eb2f4`), `31266367045`, `31263297709`, `31261179158`,
`31260846758` — no red window.

## Step 0 — Tag (AR-CI-0)

`stable-20260808-fidelity-close` did not already exist locally or on
the remote (`git tag -l` empty, `git ls-remote --tags origin` empty for
this name). Created annotated at `45eb2f4`, pushed
(`git push origin stable-20260808-fidelity-close`), landed as
`[new tag]` in the push output — no local/remote pre-existence to
verify-and-disclose instead. No commit this step, per the prompt.

## Step 1 — Reproduction

Pin verified first: `/home/mg/synthea-checkout` at
`7e08387c68a7f0e21d13076609a159fd473fc902` (direct `git rev-parse
HEAD`), matching exactly. `colorectal_cancer.json` loaded from that
checkout via `ehrt.sim-trajectory.interface/load-closure` with a
filesystem `resolve-call-path` resolving `anemia/anemia_sub` from the
same checkout (its own only `CallSubmodule` reference, confirmed by
inspection) — no `:persona-config` override, matching ADR-0082's own
finding that colorectal's `Initial` state is not Race-gated.
`engine/run` + `check/check-all` at 300 patients, seeds 20260802/1/42
— the same round-trip shape every vendored module's own committed test
uses.

| seed | `:clinical-content-only-when-admitted` | `:discharge-follows-admission` | total | distinct violating patients |
|---|---|---|---|---|
| 20260802 | 3 | 1 | 4 | 1 |
| 1 | 0 | 0 | 0 | 0 |
| 42 | 4 | 0 | 4 | 1 |

Zero at seed 1, violations at 42/20260802 — the expected shape,
matching ADR-0072's own original record ("20260802 and 42 each 4
violations, seed 1 clean") and ADR-0082's own summary table ("4/0/4")
exactly. **Disclosed discrepancy, not smoothed over:** this does NOT
match ADR-0082's own prose breakdown one paragraph past that table
(`{:clinical-content-only-when-admitted 19, :discharge-follows-
admission 1}` at seed 42 — 20 total, not 4). No commit in this repo's
history touches `check.clj`/`compile_trajectory.clj`/
`gmf_interpreter.clj`/`engine.clj` between `dad2553` (which ADR-0082's
own prose already post-dates) and this session's own `45eb2f4` HEAD —
verified by `git log --oneline` on each file individually — so there
is no code-drift explanation available; every candidate layer is
byte-identical to what ADR-0082 measured. This session's own
methodology was independently verified before trusting it (see Step 2)
and its raw counts are `check/check-all`'s own direct output, not
filtered through any per-patient logic that could itself drop
violations. Full detail and the session's own reading of the
likeliest explanation: `notes/adr/0085-colorectal-investigation.md`'s
own Reproduction section.

## Step 2 — Bisection

Probe script (`clojure -M:dev`, scratch, never touching the working
tree): `with-redefs` simultaneously intercepted
`ehrt.sim-trajectory.interface/run-module` and
`ehrt.sim-trajectory.interface/compile-trajectory`, capturing, per
call, (a) the raw interpreter walk exactly as `run-module` returned it
and (b) the exact trajectory `compile-trajectory` compiled from — both
captured in call order, which is provably patient-index order (the
engine's own arrival-queue key `[arrival-t index]` is non-decreasing in
index, and `:registered` — the only caller of either intercepted
function — is always the first step popped for each patient). Full
`engine/run` + `check/check-all` produced (c) the ground truth and
`engine/replay`'s own status stream. Before trusting any per-patient
attribution, the pairing was independently cross-checked: each
patient's own captured `:registration-facts` count against the SAME
patient's own ground-truth `:registered` event's `:pre-horizon-facts`
count, across all 300 patients at both seeds — **zero mismatches**.

Both distinct violating patients (100% of the violating population)
show the identical mechanism:

- **Seed 42, one patient:** an `:ambulatory` encounter
  (`:routine-colonoscopy-encounter`) opens PRE-horizon and mints a
  `:procedure` inside it, also pre-horizon. The registration horizon
  falls mid-encounter; four subsequent clinical-content events
  (`:condition-onset`/`:observation`/`:procedure`/`:observation`/
  `:procedure`, only the non-condition-onset four actually compile —
  `:condition-onset` has no compiled encounter step to attach to) and
  the encounter's own `:encounter-end` all fire POST-horizon.
  `compile-trajectory`'s per-event `pre-horizon` drop clause silently
  drops the opening `:encounter`/`:procedure` WITHOUT setting
  `encounter-closed?` (the drop branch never touches it); the four
  post-horizon clinical-content steps then compile normally with no
  preceding `:admission`/`:outpatient-visit` step, and the
  `:encounter-end` compiles to `:outpatient-visit-end` (resolving its
  `:references` back to the dropped opening's `:ambulatory` class).
  `engine/replay` reads `:status :new` (never `:admitted`) at all four
  — four `:clinical-content-only-when-admitted` violations, exactly
  reproduced.
- **Seed 20260802, one patient:** identical mechanism, one layer more
  consequential — an `:inpatient` `:partial-colectomy-encounter` opens
  pre-horizon; `:observation`/`:care-plan-start`/`:procedure` compile
  post-horizon with the same missing-admission gap (three violations);
  the encounter's own `:encounter-end` compiles to `:discharge`,
  flipping `encounter-closed?` true (correctly, per this project's own
  ratified single-encounter-per-run scope — not itself defective).
  Because this patient's entire compiled trajectory is this one
  straddling encounter's tail, no `:admission` event exists anywhere in
  their compiled ground truth — `discharge-follows-admission` trips
  too. AR-CI-4's own trace: **same patient, same mechanism**, not a
  second defect.

**Diagnosis:**
`ehrt.sim-trajectory.compile-trajectory/compile-trajectory`'s own
legacy (`history?` false) `:pre-horizon` drop clauses test only an
event's own flag, with no back-reference check against the encounter
it belongs to — unlike the Wave H `history-phase?` mechanism, which
already implements exactly this back-reference principle, but only for
`:medication-end`/`:care-plan-end`/`:condition-end`, and only in
`history?` true mode.

**AR-CI-3 (truncation hypothesis): CONFIRMED, narrowed.** The
`:pre-horizon` drop gate is the real, evidenced mechanism — but in a
straddling-encounter shape ADR-0082 AR-EE-1a's own hypothyroidism trace
never exercised (that trace was a fully-pre-horizon dangling reference,
correctly absorbed; this is a genuinely-open span, incorrectly
absorbed on one side only). `encounter-closed?`'s own single-
encounter-per-run truncation plays no defective role in either trace.

Full evidence tables (raw walk, compiled steps, replay status stream,
per patient): `notes/adr/0085-colorectal-investigation.md`.

## Step 3 — Record (`3458fa3`)

`notes/adr/0085-colorectal-investigation.md` authored (reproduction
table, bisection method, per-layer evidence, diagnosis, truncation
verdict, discharge trace, proposed fix shape for a future ruled
session — NOT executed, per AR-CI-1). `notes/ADRs.md` gained its index
line. `notes/adr/README.md`'s file count corrected 82→83 (`ls
notes/adr/*.md | grep -v README | wc -l`, not arithmetic). Roadmap's
colorectal Deferred row gained a dated note pointing to ADR-0085 — row
stays LIVE (`**Dated note ... DIAGNOSED, not fixed — row stays
LIVE.**`, the same "row stays live" phrasing ADR-0082's own note used
to satisfy `roadmap-deferred-closure-lint-test` without actually
closing anything).

`clojure -M:poly check`: OK. Full suite
(`clojure -M:poly test :all skip:integration`) run TWICE before
committing (once during authoring, once as the final pre-commit
check): both runs clean, 0 failures/0 errors throughout, last project
block 198 tests / 521 assertions (matching ADR-0082's own baseline
exactly — no drift). The disclosed `mutate-stdout-into-intake-stdin-
real-loopback-test` flake did NOT fire in either run — no
disambiguation needed.

`git diff --cached --stat` reviewed before staging: exactly the four
intended files (`notes/adr/0085-colorectal-investigation.md` new,
`notes/ADRs.md`/`notes/adr/README.md`/`.agents/plans/roadmap.md`
edited), nothing extraneous. `gitleaks git --staged -v`: clean.
Committed `3458fa3` ("docs: the colorectal investigation records its
findings (ADR-0085)"), message via file. Pushed; post-push
verification (`git log --format=%B -1 3458fa3` diffed against the
source message file): one delta, the known trailing-blank-line
artifact. CI watched to conclusion: run `31269357505`, `success`,
3m14s.

## Step 4 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-08-colorectal-investigation.md`
(the driving prompt, archived verbatim) land together, indexed in both
READMEs' own entry lists, in the commit
"docs: session record and prompt archive — colorectal investigation".

## Successor tag debt

Recorded in `notes/adr/0085-colorectal-investigation.md`: the next
session that opens fresh work tags
`stable-20260808-colorectal-investigation` at this session's own
closing tip.

## Deviations, disclosed

- **The seed-42 count discrepancy against ADR-0082's own prose** (this
  session's own 4 total vs. ADR-0082's stated 20) — disclosed in full
  in Step 1, above, and in `notes/adr/0085-colorectal-investigation.md`'s
  own Reproduction section, rather than silently adopted or silently
  ignored. This session's own diagnosis is built on its own
  independently-verified counts and per-patient evidence, not on the
  disputed figure.
- **No STOP-AND-REPORT triggered** — the session reached AR-CI-5(i), a
  full localized diagnosis, not (ii); disclosed here only because the
  prompt names both as equally acceptable outcomes and this record
  should say plainly which one landed.

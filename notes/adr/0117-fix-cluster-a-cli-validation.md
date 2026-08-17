## ADR-0117 — Fix cluster A: CLI validation and error quality

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

`.agents/plans/roadmap.md`'s "Fix cluster A -- CLI validation and error
quality" row (chartered `notes/ADRs.md` ADR-0115, from review-3's own
`fix-session-candidate` register rows,
`.agents/plans/2026-08-12-review-3-user-surface-findings.md`) lands in
this session: eight fixes, F1-F8, each with red-before-green, each
touching only error paths, validation, help text, or a flag rename on a
verb no oracle root invokes. Oracle identity is argued per-fix and
verified once at the close of the fix commits.

### Tag ceremony

`git fetch` confirmed `origin/main` at `baf6a8c`
(`baf6a8c02db381a09eb2bcf84737fcabdc7dbf34`, ADR-0116 close) at session
start. **The last five `main` CI runs** (`gh run list --limit 5
--branch main`, checked at session start): all `completed`/`success` --
`baf6a8c` (ADR-0116 session record, 4m35s), `fc72f54` (ADR-0116 fix
commit, 4m25s), `c6deb5a` (ADR-0115 fix-forward, 4m23s), `e31492b`
(ADR-0115 session record, 3m14s), `ed00e3a` (ADR-0115 rulings landing,
4m13s) -- no red among the five; this completes the CI leg ADR-0116's
own record left session-transcribed.

License: tag-law case (i) -- the design channel's own 2026-08-12
verification of the ADR-0116 landing, this session's own preflight
(fresh `git fetch`, exact SHA match, CI green on the last five runs by
direct API read). `stable-20260812-engine-seed-contract` tagged
ANNOTATED at `baf6a8c`; pushed; peeled ref confirmed
`baf6a8c02db381a09eb2bcf84737fcabdc7dbf34` -- exact match.

### Decision

Every "current (verify)" claim in this session's own driving prompt
was verified live against the tree before its fix landed -- all held
exactly as stated, no STOP-AND-REPORT triggered.

#### F1 (R3-B2-1, HIGHEST PRIORITY) -- `check` target validation

**Red (live probe, pre-fix):**
```
$ bin/ehrt check
{:status :ok, :payload {..., :totals {:pass 0, :rejected 0, :indeterminate 0, :no-verdict 0}, :files []}}
$ bin/ehrt check /tmp/definitely-not-a-real-dir-xyz
{:status :ok, :payload {..., :totals {:pass 0, ...}, :files []}}
$ bin/ehrt check /tmp/empty-check-dir-xyz   # genuinely empty, existing
{:status :ok, :payload {..., :totals {:pass 0, ...}, :files []}}
```
All three exit 0, indistinguishable from a genuine zero-finding pass --
confirmed exactly as the driving prompt's evidence base claimed.

**Fix:** `check-target-error` (`bases/cli/src/ehrt/cli/core.clj`), a
new CLI-boundary-only validator (judge/check's own `check-corpus`
untouched): nil path -> `result/error :missing-required-opt {:opt
:path ...}`; path doesn't exist -> `result/error :invalid-target
{:path ... :reason :not-found ...}`; path exists but contains zero
files (recursively, matching `check-corpus`'s own `files-in` semantics)
-> `result/error :invalid-target {:path ... :reason :empty ...}`.
`check-command` now runs `(or (check-target-error path) ...)` first.
Exit 2 throughout (`result/error`).

**Green:** `check-command-missing-target-is-missing-required-opt-test`,
`check-command-nonexistent-target-is-invalid-target-test`,
`check-command-empty-target-is-invalid-target-test` --
`bases/cli/test/ehrt/cli/core_test.clj`, all passing.

#### F2 (R3-B2-2) -- parse-error translation

**Red (live probe, pre-fix):**
```
$ bin/ehrt sim run --seed abc --patients 1
Execution error (ExceptionInfo) at babashka.cli/->error-fn$fn (cli.cljc:272).
Invalid value for option --seed: cannot transform input "abc" to long
Full report at: /tmp/clojure-<random>.edn
```
Real exit code 1 (JVM uncaught-exception default), library name and
file:line leaked, exactly as claimed.

**Probe of babashka.cli's own ex-data shape** (direct REPL call,
`cli/parse-args ["--seed" "abc"] {:spec {:seed {:coerce :long}}}`):
`{:type :org.babashka/cli, :cause :coerce, :option :seed, :value "abc",
...}` -- `:option`/`:value` are exactly what a translation needs.

**Fix:** `flag-expected-type`/`parse-error-result`/`safe-parse`
(`core.clj`), inserted right after `parse`. `safe-parse` wraps `parse`
in a `try`/`catch clojure.lang.ExceptionInfo`; when `ex-data` carries
`:option`, translates to `result/error :invalid-flag-value {:flag
"--seed" :value "abc" :expected "a long"}`; otherwise rethrows
unchanged (never misrepresents an unrecognized shape). `main!` now
calls `safe-parse` instead of `parse` directly; when `:parse-error` is
present, `dispatch-fn` is never invoked.

**Green:** `safe-parse-translates-babashka-cli-coercion-failure-test`,
`safe-parse-passes-through-well-formed-args-test`,
`main-bang-parse-error-exits-two-with-no-stack-trace-or-library-name-test`
-- all passing; the last one asserts the printed text contains neither
"babashka" nor "ExceptionInfo".

#### F3 (R3-B2-3 + R3-B4-1) -- `corpus intake --out` required

**Red (live probe, pre-fix):**
```
$ bin/ehrt corpus intake /tmp/intake-src-xyz --label test
Execution error (NullPointerException) at ehrt.corpus.intake/intake! (intake.clj:376).
Cannot invoke "java.io.File.mkdirs()" because "out_dir" is null
```
Real exit code 1, raw NPE with an internal file:line, exactly as
claimed.

**Fix:** `intake-command` (`core.clj`) now checks `(nil? out)` first,
before any generator-URL/stdin-URL resolution or file I/O:
`result/error :missing-required-opt {:opt :out ...}`. **Ruled
require-not-derive** [C, un-vetoed] -- a derived path (mirroring
`corpus generate`'s own input-derived out-dir pattern, D12) would fold
`--received`'s own wall-clock default (the RQ3 class exemption,
ADR-0115) into a filesystem name, quietly unreproducible; requiring is
honest. Recorded in `.agents/rulings.md`, "From ADR-0117."

**Green:** `intake-command-missing-out-is-missing-required-opt-test`
passing; every other `intake-command` test in the suite already passed
an explicit `:out`, unaffected.

#### F4 (R3-B1-5) -- missing-required-flag unification

**Census** (grep for the categories named in the driving prompt):
three live sites --
`components/... bases/cli/src/ehrt/cli/core.clj`:
- `batch-command`, `:interval-required` (`result/rejected`, exit 1)
- `v2-nist-gate-command`, `:v2-nist-profile-required` (`result/rejected`, exit 1)
- `mutate-command`, `:unknown-operator` fired even when `:operator-id`
  was literally absent (nil), not just when a real id was unrecognized
  (`result/rejected`, exit 1)

**Red (test-level, pre-fix, captured before implementing F4):**
```
FAIL in (batch-command-interval-required-test)
expected: (result/error? r)
  actual: (not (result/error? {:status :rejected, :category :interval-required, ...}))

FAIL in (v2-nist-gate-command-requires-profile-test)
expected: (result/error? r)
  actual: (not (result/error? {:status :rejected, :category :v2-nist-profile-required, ...}))

FAIL in (mutate-command-missing-operator-id-is-missing-required-opt-test)
expected: (result/error? r)
  actual: (not (result/error? {:status :rejected, :category :unknown-operator, :payload {:id nil, ...}}))
```

**Fix:** all three retired their verb-specific category for
`result/error :missing-required-opt {:opt <flag> ...}` at exit 2.
`mutate-command`'s operator lookup now short-circuits on `(nil?
operator-id)` before ever calling `operator-lookup` -- a real,
unrecognized id given (e.g. `"no-such-operator"`) still returns
`:unknown-operator`/exit 1 unchanged (a legitimate rejection: the
lookup ran, the id just doesn't exist).

**Green:** the three updated tests, plus the new
`mutate-command-missing-operator-id-is-missing-required-opt-test`, all
passing; `mutate-command-rejects-unknown-operator-test` (a real bogus
id) unaffected.

#### F5 (R3-B1-3) -- source-scoping validation

**Red (test-level, pre-fix):**
```
FAIL in (dispatch-corpus-generate-sim-rejects-a-synthea-scoped-flag-test)
"the mismatched flag must be rejected before generate-sim-fn ever runs"
expected: (not (clojure.core/deref called))
  actual: (not (not true))
```
(`--population` given during `corpus generate sim` reached the fake
`:generate-sim-fn` unrejected -- exactly the silent-misconfiguration
class the register described.)

**Fix:** `generate-flag-scope` (`core.clj`) derives a flag-keyword ->
`:sim`/`:synthea` map from `help/cli-spec`'s own doc-string prefix
convention ("sim: "/"synthea: ", the same convention
`declared-flag-keywords` already reads, AR-U3-1) -- no second,
hand-maintained list. `validate-generate-source-scope` rejects the
first opts key scoped to the OTHER source with `result/error
:flag-source-mismatch {:flag ... :flag-scope ... :selected-source
...}`, wired into dispatch's `"generate"` case for both the `"sim"` and
`"synthea"` branches, before `generate-sim-fn`/`generate-fn` ever runs.
**Ruled reject-not-warn** [C, un-vetoed] -- consistent with this
cluster's own strict-validation direction. Recorded in
`.agents/rulings.md`, "From ADR-0117."

**Green:**
`dispatch-corpus-generate-sim-rejects-a-synthea-scoped-flag-test`,
`dispatch-corpus-generate-synthea-rejects-a-sim-scoped-flag-test`,
`dispatch-corpus-generate-sim-allows-shared-and-sim-scoped-flags-test`,
`dispatch-corpus-generate-synthea-allows-shared-and-synthea-scoped-flags-test`
-- all passing.

#### F6 (R3-B2-5 + R3-B3-3) -- `help <unknown-group>`

**Red (test-level, pre-fix):**
```
FAIL in (dispatch-help-verb-with-unknown-group-is-a-named-error-test)
expected: (result/error? r)
  actual: (not (result/error? {:status :ok, :payload {:text "Usage: ehrt <group> ...", ...}, :category :cli-help}))
```
`bin/ehrt help crops` (live probe, pre-fix) confirmed the same: exit 0,
silent top-level fallback, no indication `crops` isn't a real group.

**Fix:** dispatch's `(= group "help")` branch now checks, after
`validate-top-level-flags`, whether `action` is non-nil and not a
member of `(help/group-names help/cli-spec)`; if so, reuses
`unknown-command-error` verbatim (the same function/category
`ehrt <unknown-group>` itself already uses) with `[action]` as its
`args`. Bare `ehrt help` (`action` nil) is unaffected.

**Green:** `dispatch-help-verb-with-unknown-group-is-a-named-error-test`,
`dispatch-help-verb-with-no-group-still-shows-top-level-usage-test` --
both passing.

#### F7 (R3-B1-1, RULED ADR-0115 RQ1) -- `gate fhir --out-dir` -> `--scratch-dir`

**Sweep census** (extension-blind grep, `gate fhir` proximate to
`--out-dir`/`out/scratch/gate-fhir`/`default-fhir-gate-out-dir` across
every live `.md`/`.edn`/`.clj` file, `notes/sim/`/`notes/tools/`
excluded as frozen provenance): exactly four live-editable sites, all
in this rename's own natural reach --
- `bases/cli/src/ehrt/cli/help.clj:135,145` -- the `gate` group's own
  doc string ("PATH and --out-dir also accept dir:/file: URL
  designators") and the `--out-dir` flag entry itself.
- `bases/cli/src/ehrt/cli/core.clj:1211,1214,1230,1235` --
  `default-fhir-gate-out-dir`, `fhir-gate-command`'s own `:out-dir`
  destructure and docstring.
- `bases/cli/src/ehrt/cli/core.clj:2319-2337` --
  `resolve-path-designators`'s key list (`[:path :out-dir :out]`),
  needing `:scratch-dir` ADDED (not a rename in place) to preserve the
  dir:/file: URL-designator-acceptance behavior under the new flag
  name -- a valid-input behavior change is exactly what the fence
  forbids.
- `docs/cli.md` -- regenerated (`make cli-doc`), never hand-edited.

Every OTHER `gate fhir`/`--out-dir` co-occurrence found by the same
grep is either (a) `corpus generate`/`mutate`/`batch`'s own, unrelated
`--out-dir` (a genuinely different flag, untouched), or (b) a dated,
frozen record of an actual past invocation --
`notes/judge-engine-extraction-characterization.md` (a 2026-07-29
session's own literal characterization baseline, correctly left
untouched: that session really did type `--out-dir`, and editing it
would misrepresent history), `notes/adr/*.md` (frozen per-ADR
execution records), `.agents/plans/`/`.agents/prompts/` (dated planning
and prompt archives). `components/corpus/docs/use-cases.edn` and every
`docs/use-cases/*.md` file were checked directly: zero references to
`gate fhir`'s own `--out-dir` (every `--out-dir` example in those files
is `corpus generate`/`mutate`/`batch`'s). `demos/**` was checked
directly: zero `gate fhir` invocations exist there at all. No
back-compat alias -- the tool is unpublished (ADR-0008).

**Red (compile-level, pre-fix):** `No such var:
cli/default-fhir-gate-scratch-dir` -- the var didn't exist yet;
`dispatch-gate-fhir-rejects-the-old-out-dir-flag-test` (added alongside)
proves, post-fix, that `--out-dir` no longer reaches
`fhir-gate-command` at all (`:unknown-flag`, since only `--scratch-dir`
is declared for the verb).

**Fix:** `--out-dir` -> `--scratch-dir` in `help.clj`'s `cli-spec` (the
`gate fhir` verb entry) and its own group-doc mention;
`default-fhir-gate-out-dir` -> `default-fhir-gate-scratch-dir`;
`fhir-gate-command` destructures `:scratch-dir` and builds
`judge-fhir-official`'s own internal `fhir-opts` map with `:out-dir (or
scratch-dir default-fhir-gate-scratch-dir)` -- that internal parameter
name is untouched, the rename is CLI-surface only.
`resolve-path-designators` gains `:scratch-dir` in its resolved-key
vector. `docs/cli.md` regenerated: exactly the 3 predicted deltas (the
group-doc line, the flag-name row, the flag-doc-string row) landed in
commit 3.

**Green:** `dispatch-gate-fhir-accepts-scratch-dir-test`,
`dispatch-gate-fhir-accepts-a-dir-url-scratch-dir-test`,
`dispatch-gate-fhir-rejects-the-old-out-dir-flag-test`,
`fhir-gate-command-defaults-to-the-standard-scratch-dir-test` -- all
passing.

#### F8 (R3-B1-4, RULED ADR-0115 RQ2) -- `--seed` doc-string tiering note

**Red (test-level, pre-fix):**
```
FAIL in (corpus-generate-seed-doc-states-the-ergonomic-front-door-tiering-test)
expected: "...defaulted here as the ergonomic front door..."
  actual: "patient/master-generation seed (integer), shared by both sources"
```

**Fix:** `help.clj`'s `corpus generate` `--seed` flag doc string
becomes, verbatim per the driving prompt: "patient/master-generation
seed (integer; non-negative when --source sim), shared by both
sources; defaulted here as the ergonomic front door -- the sim-tier
verbs (sim run, sim identifiers) require a seed explicitly." This also
closes the gap ADR-0116 disclosed on this exact row (`corpus
generate`'s own dual-source `--seed` row was found but deliberately
left unedited there, since appending bare "(non-negative)" would have
misstated the `:synthea` half). `docs/cli.md` regenerated: one row
delta, landed in the same commit-3 diff as F7's.

**Green:**
`corpus-generate-seed-doc-states-the-ergonomic-front-door-tiering-test`
passing; `cli-md-is-current-test` green again after regen.

### Commit structure

- Commit 1 (`573bae4`) -- F1+F2: `fix: cli check target validation and
  parse-error translation (ADR-0117)`.
- Commit 2 (`5d05825`) -- F3+F4+F5+F6: `fix: cli required-flag,
  source-scoping, and help-group validation (ADR-0117)`.
- Commit 3 (`c058706`) -- F7+F8 + doc regen: `fix: gate fhir
  scratch-dir rename; seed-row tiering note (ADR-0117)`.
- Commit 4 -- this file, registers, rulings, session record, prompt
  archive: `docs: session record and prompt archive -- fix cluster A
  (ADR-0117)`.

Full `make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration` + `bin/verify-nist-lock`) run green before each of
the three code pushes: 0 FAIL/ERROR anywhere in 308 tested namespaces
each run; `bin/verify-nist-lock` OK, all 6 hit-nexus-sourced
coordinates matched, every run.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots. F1-F6 change only
error paths on invalid inputs no oracle root supplies (every root uses
a valid target directory, well-formed flag values, an explicit `--out`,
a valid `--operator-id`/`--interval`/`--profile`, source-matched flags,
and no root invokes `ehrt help` with a bad group name). F7 renames a
flag on `gate fhir`, a verb no oracle root invokes (channel-verified:
zero `gate` references anywhere in `bin/regression-oracle`). F8 is help
text.

**Bracket result.** `bin/regression-oracle baf6a8c c058706` (run after
commit 3, the last `src`-changing commit, before this close-phase
commit -- the same step-ordering precedent ADR-0116 set): `IDENTICAL:
every root's digest matches between baf6a8c and c058706` -- all 35
roots, matching the pre-analysis exactly. No STOP-AND-REPORT needed.

### Full gate

`make test`: green throughout every push (see Commit structure above).
`gitleaks git --staged -v` (pre-stage) and `gitleaks detect` (pre-push,
via the `.githooks/pre-push` hook): no leaks found, every commit.
Post-push verification, every commit: pushed message diffed against
its own source file (only the known trailing-blank-line `git log
--format=%B` artifact); `git log --format=%B -1 | LC_ALL=C grep -n
'[^ -~]'` empty (ASCII clean) on every commit.

### Deviations

None in the fix work itself. Every "current (verify)" claim in the
driving prompt was confirmed live against the tree, exactly as stated,
before its fix landed (F1's triple `check` probe, F2's `sim run --seed
abc` probe, F3's `corpus intake` NPE probe, F6's `help crops` probe --
all four transcribed above). No red test refused to go red; no regen
delta landed outside F7/F8's own predicted reach (confirmed:
`docs/cli.md`'s diff after commit 3 was exactly 4 lines across the two
fixes' own rows, `make use-cases` a confirmed no-op). No oracle
non-identity.

**A brief CI-red window on commit 2, self-caught while confirming CI
status ahead of this session's own close, disclosed fix-forward.** `gh
run list --limit 8 --branch main` showed commit 2's own push
(`5d05825`, run `31642842797`) `completed`/`failure` at 41s -- far
short of this suite's own ~4-4.5-minute runtime, the first sign this
was not a real test failure. `gh run view 31642842797 --log-failed`
confirmed: the `DeLaGuardo/setup-clojure@13.4` action's own Clojure CLI
tools download hit a transient `curl: (22) The requested URL returned
error: 503`, retried five times with exponential backoff, then gave up
-- failing before the workspace checkout finished building, let alone
running any test. Not a defect this session introduced -- this
session's own local `make test` for commit 2's exact diff had already
run green (`clojure -M:poly check` + the full `clojure -M:poly test
:all skip:integration` suite, 308 tested namespaces, 0 FAIL/ERROR +
`bin/verify-nist-lock` OK) before that push. `gh run rerun 31642842797`
confirmed `completed`/`success` on the identical commit, 4m40s runtime,
matching every sibling push. No `src`/`test`/`docs` edit accompanies
this disclosure -- the tree was never at fault.

### Fences

Touched: `bases/cli/src/ehrt/cli/core.clj`, `bases/cli/src/ehrt/cli/
help.clj`; `bases/cli/test/ehrt/cli/core_test.clj`,
`bases/cli/test/ehrt/cli/help_test.clj`; `docs/cli.md` (regenerated,
`make cli-doc`); `.agents/plans/roadmap.md`;
`.agents/plans/2026-08-12-review-3-user-surface-findings.md` (ten
disposition-cell notes, fix-forward, summary table untouched per the
ADR-0115 snapshot rule); `.agents/rulings.md`; `notes/ADRs.md`;
`notes/adr/README.md`; `notes/adr/0117-*.md` (this file);
`.agents/prompts/*`; `.agents/session-records/*`. `make use-cases` was
run to confirm no drift (a no-op, zero file changes) -- not itself a
fence-widening edit. ZERO judge/check component internals touched,
ZERO engine/sim `src` touched, ZERO behavior change on any valid input
to any verb. No file outside this list was touched.

### Index line

```
- 2026-08-12 — fix-cluster-a-cli-validation — ADR-0117
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Fix cluster A: CLI validation and error quality — lands all eight members review-3 chartered (ADR-0115), three commits, red-before-green per fix: `ehrt check` now requires DIR to exist and be non-empty (`:missing-required-opt`/`:invalid-target`, exit 2 — R3-B2-1, the register's own HIGHEST-PRIORITY finding); a `babashka.cli` coercion failure (`--seed abc`) no longer leaks the library's own name and a file:line at the wrong exit code, caught at a new `safe-parse` boundary and translated to `:invalid-flag-value` (R3-B2-2); `corpus intake --out` is now required rather than crashing with a raw `NullPointerException`, ruled require-not-derive (R3-B2-3 + R3-B4-1); every "required flag missing" case across `corpus batch`/`gate v2-nist`/`corpus mutate` is unified onto the shared `:missing-required-opt` shape at exit 2, retiring three verb-specific categories (R3-B1-5); a `synthea:`/`sim:`-scoped flag given to the wrong `corpus generate` source is now rejected by name, ruled reject-not-warn (R3-B1-3); `ehrt help <unknown-group>` now gives the same named error `ehrt <unknown-group>` itself already gave (R3-B2-5 + R3-B3-3); `gate fhir`'s `--out-dir` is renamed `--scratch-dir`, no back-compat alias, per ADR-0115's own RQ1 ruling — sweep census found zero live doc surfaces citing it explicitly (R3-B1-1); `corpus generate`'s `--seed` doc string states the two-tier front-door/engine-tier design explicitly, per ADR-0115's own RQ2 ruling, also closing the gap ADR-0116 disclosed but left open on this same row (R3-B1-4); zero judge/check component internals touched, zero engine/sim `src` touched, the oracle holds pure identity across all 35 roots

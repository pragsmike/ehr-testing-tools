## ADR-0060 — UX fixes 2: errors that name their artifact — the config crash dies, and the fences actually run

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: ux fixes 1 landed and was design-channel-verified (`c998c7b`,
`notes/adr/0059-ux-fixes-1.md`) — with one residual the design
channel's own probe caught: the swept demo fences kept their OLD
cwd-relative `--config` paths, which under `bin/ehrt`'s forced root
cwd resolve to nothing (verified: `docs/demos/` does not exist at
root; the file lives at
`components/sim/docs/demos/module-mix/config.edn`). Register row A-2
(`.agents/plans/2026-08-06-ux-audit-findings.md`) verified grammar,
not path existence — that verification-gap lesson is recorded here.
This session fixes the fences (the rider), then executes the
error-surface cluster: C-1's `--config` crash adopts the Result
vocabulary, U4's near-miss folds in, B-6/D-3's hint gains specificity,
and the author's B-5 ruling (bare invocation exits 0) lands. The FIRST
src-behavior session of this arc — every fix is red-first-tested, and
the oracle bracket proves no emitted byte moved.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-U2-0 (tag, standing ceremony).** Annotated
`stable-20260806-ux-fixes-1` at `c998c7b`, message "ux fixes 1 landed,
design-channel-verified 2026-08-06 (ADR-0059)"; push; verify.

**AR-U2-R (rider — the fences actually run).** Fresh enumeration of
EVERY file-path-carrying flag in live doc command fences (`--config`,
`--out-dir`, `--lockfile`, any others; multi-line backslash-continued
fences included — a line-scoped grep undercounts, as the design
channel's own did): each path must resolve from workspace root.
Rewrite non-resolving paths root-relative. Extend
`invocation_lint_test` with a fence-path-resolution assertion: every
path argument in a live doc fence exists on disk from root (output-dir
flags whose targets are CREATED by the command are exempted by flag
name, not by path). Natural red first. This ADR records the A-2
lesson: grammar-validity is not path-validity; fence verification must
resolve, not just parse.

**AR-U2-1 (C-1 — the crash dies).** `merge-config-file` adopts the
Result pattern its siblings prove (C-2, C-5): missing file →
`result/error :config-not-found {:path path}`; unreadable/malformed
EDN → `result/error :config-unreadable {:path path :message
<parse-message>}`. Propagates through every caller (`run-command`,
`identifiers-command`, `generate-sim-command`) exactly as
`:missing-required-opt` already does; exit code 2 via the existing
`result->exit-code` mapping. Red-first: a failing test per category
BEFORE the fix (missing path; malformed EDN), each asserting the
category, the `:path` in the payload, and the exit-code mapping.

**AR-U2-2 (U4 — the near-miss, folded).** When `:config-not-found`
fires and a sibling file with the same stem but different extension
exists in the same directory, the payload gains `:did-you-mean
<sibling-path>`. Check `ehrt.kernel` for an existing similar-file
helper first (register's unconfirmed hint); reuse if real, else a
small local fn — disclosed which. Red-first test: the founding
incident's exact shape (`busy-weekday.md` present, `.edn` requested).

**AR-U2-3 (B-6/D-3 — the hint knows the group).** When the
unknown-command token matches a real group name, the hint becomes
`run: ehrt help <group>` instead of the generic `run: ehrt help`.
Genuinely-unknown tokens keep the generic hint. Red-first test for
both branches.

**AR-U2-4 (B-5, ruled: exit 0).** Bare `bin/ehrt` renders the same
help text and exits 0, matching the spec's own `--help`-exits-0
convention (author ruling, 2026-08-06). Locate the `result/error
:cli-help` construction (`bare-invocation-response`, core.clj ~1476)
and convert to the success path `help-response` itself uses. One
factual line lands in the exit-codes table documenting that bare
invocation, `help`, and `--help` all exit 0 — DISCLOSED overlap with a
future voice rewrite: this line is a factual addition now, restyled
later with everything else; kept to plain words with no citations so a
future rewrite has nothing to relocate. Red-first test asserting the
exit code.

**AR-U2-5 (scope).** C-4 (unknown-flag validation), the voice rewrite,
and the wrap mechanism are later sessions. Nothing from those lands
here, however adjacent the code. The oracle bracket must show all
ELEVEN batches identical — error paths and help dispatch touch no
emitted byte; any digest change is STOP-AND-ESCALATE.

### Execution record

**Step 0 — preflight + tag.** Working directory confirmed
`~/src/ehr-testing-tools` (ext4, `/dev/sdd`); tip `c998c7b` exactly;
working tree clean apart from `config/busy-weekday.md` (the unrelated
pre-existing untracked founding-incident fixture, left alone as every
session since the incident has). Baseline: `clojure -M:poly check`:
OK; `clojure -M:poly test`: 0 failures/0 errors across every brick
(2m39s). AR-U2-0 executed: `stable-20260806-ux-fixes-1` did not exist
locally or on origin (checked both); created annotated at `c998c7b`,
message "ux fixes 1 landed, design-channel-verified 2026-08-06
(ADR-0059)"; pushed; verified — peeled ref resolves to `c998c7b`
exactly. Oracle pre-digest: not run as a separate artifact this step
(`bin/regression-oracle` takes two refs and runs both worktrees
itself — there is no standalone single-ref digest to capture; the
prior session's own Step 0 disposed of this identically) — the
baseline ref `c998c7b` is simply the bracket's own fixed left side,
exercised at Step 3.

**Step 1 — the rider (AR-U2-R).** Fresh enumeration, mechanically
re-derived by parsing every ```bash/```sh fence in the sweep's own
four-root scope (`README.md`, `AUTHORS-GUIDE.md`, `docs/**`,
`components/*/docs/**`), backslash-continuations joined before
tokenizing:

| flag | in-scope literal values | disposition |
|---|---|---|
| `--config` | 4 stale (`docs/demos/order-result/config.edn` ×2 in `emit-state/README.md`, ×1 in `order-result/README.md`, `docs/demos/module-mix/config.edn` in `module-mix/README.md`, `docs/demos/site-profiles/config-aldric.edn` in `sim-emit-hl7/docs/demos/site-profiles/README.md`); 6 already-resolving (`config/synthea/synthea.properties`-style, different flag `--config-path`, unaffected); 1 disclosed exemption (`docs/simulate-your-facility.md`'s own `stmarys.edn` — the reader's own hypothetical facility config, never a repo path; the surrounding prose already says the shipped `demos/` examples are the exact, runnable ones) | 4 rewritten root-relative; 1 exempted, named and disclosed, not silently skipped |
| `--profile` | 1 (`components/corpus/test-fixtures/v2-nist/COVID19_ELR-v2.3.1`) | already resolves, untouched |
| `--path` | 1 literal (`components/corpus/test-fixtures/v2/adt-a01-admit.hl7`); others use a shell variable, skipped by construction | already resolves, untouched |
| `--out-dir` | every in-scope literal value is `out/...`, command-created | exempted by flag name |
| `--report` | every in-scope literal value is `out/...`, command-created | exempted by flag name |
| `--baseline` | 1 (`out/regression/baseline.edn`, created by that same tutorial's own earlier `--report`) | exempted by flag name |
| `--lockfile` | no literal-path fence in scope (only default-value prose) | n/a |

Natural red first: `fence-path-arguments-resolve-from-workspace-root-test`
run against the unswept tree — 5 failures on first pass (4 real +
1 false positive, a shell comment `# --path takes a file, or a
directory` in `docs/use-cases/generate-controlled-fault-data.md`
misread as a real `--path` argument by the naive tokenizer);
comment-stripping added (`strip-line-comments`, matching real shell
`#`-comment semantics), re-run: exactly 4 failures, the real ones.
Full transcript below. Fences rewritten root-relative (the 4 real
ones); the `stmarys.edn` exemption named explicitly in the gate's own
`illustrative-path-exemptions` set, one entry, with its own comment.
Re-run: 0 failures. Commit (docs + the extended gate, one commit,
staging hygiene confirmed via `git diff --cached --stat` — exactly the
4 corrected READMEs + the extended test file,
`config/busy-weekday.md` confirmed not staged): `06e5d99` ("docs: the
fences actually run — paths resolve from root, gated (ux fixes 2,
AR-U2-R)"). `gitleaks git --staged -v`: clean. `clojure -M:poly
check`: OK. Scoped `clojure -M:poly test :dev :ehrt/docs-tooling`
(and, redundantly, a full-workspace run at Step 2's own green check):
`invocation-lint-test` 221 passes, 0 failures, 0 errors. Pushed;
post-push verification: one delta against the message file, the known
harmless trailing-newline artifact.

**Step 2 — the error cluster (AR-U2-1..4).** Read-first: `run.clj`'s
`merge-config-file` (~194-208), its two callers (`run-command`
~301, `identifiers-command` ~100), `core.clj`'s `dispatch`/
`unknown-command-error`/`bare-invocation-response`/`help-response`
(~1462-1748), `help.clj`'s `exit-codes` table (~14-21).

C-1/U4 fix, `components/sim/src/ehrt/sim/run.clj`: `merge-config-file`
rewritten to the same Result pattern `ehrt.kernel.artifact/
read-lockfile` already proves (existence check up front →
`:config-not-found`; `try`/`catch Exception` around
`slurp`+`edn/read-string` → `:config-unreadable {:path :message}`). U4
folded in: a new local `similar-sibling-config` fn (a fresh grep of
`ehrt.kernel` for `similar|did-you-mean|suggest|sibling|same-stem|
levenshtein|fuzzy` found NOTHING — confirmed absent, so this is a
small local fn, not a reused one, matching the Fences' scope-to-this-
namespace anyway) — a same-directory, same-stem, different-extension
sibling's path rides `:did-you-mean` when one exists. Both callers
(`run-command`, `identifiers-command`) updated to unwrap the Result
(`if-not (result/ok? config-result) config-result (let [opts (:payload
config-result)] ...)`), the same passthrough idiom `generate-sim-
command`'s own `resolved-result` guard already uses elsewhere in this
codebase. `result->exit-code` (core.clj ~107-117) needed NO new
entries: confirmed by inspection that its mapping is category-
agnostic (`ok? → 0`, the one named `:gate-no-verdict → 3` exception,
`rejected? → 1`, `:else → 2`) — any `result/error` whose category
isn't `:gate-no-verdict` already reaches exit 2 through the catch-all,
exactly the way `:missing-required-opt` (zero special-cased dispatch
code anywhere) already does. `generate-sim-command`'s own chain
(`sim-adapter/run!` → `ehrt.sim.interface/run-command` →
`merge-config-file`) needed no changes either — every intermediate
layer already does a bare `if-not (result/ok? ...) r ...` passthrough
or an `ok?` short-circuit.

B-6/D-3 fix, `bases/cli/src/ehrt/cli/core.clj`: `unknown-command-error`
now derives its `:hint` from whether `(first args)` names a real
top-level group (`help/group-names`) — `run: ehrt help <group>` when
it does, the unchanged generic `run: ehrt help` otherwise. One change,
applied uniformly at the single construction site every `case`
branch already funnels through — no per-branch special-casing needed.

B-5 fix, same file: `bare-invocation-response` converted from
`(result/error :cli-help {...})` to `(assoc (result/ok {...}) :category
:cli-help)`, the exact pattern `help-response` already uses — same
`:text` payload, same `:category`, `:status :ok` now. `result->exit-code`
needed no change (it already maps `ok? → 0` generically); `main!`'s own
`:cli-help`/`:display-text` text bypass (core.clj ~1931-1938) is keyed
off `:category`, not `:status`, so it too needed no change.

Exit-codes-table line, `bases/cli/src/ehrt/cli/help.clj`: one new
entry appended to the `exit-codes` vector, `{:code 0 :meaning "bare
invocation, help, and --help all exit 0 too"}` — the existing
`render-exit-codes` renders it as a second `0` line automatically, no
new rendering code needed. `docs/cli.md` regenerated (`clojure -X:dev
ehrt.cli.help/write-cli-md! :out '"docs/cli.md"'`, the documented
`make cli-doc` target's own invocation) — exactly one line added,
confirmed by `git diff --stat`.

Red-first, captured against the unfixed tree via disposable `git
stash` isolation (three separate scoped runs, since Polylith's own
test runner aborts an entire project's run on the FIRST uncaught
exception rather than continuing to the next namespace — itself
evidence of how severe the founding crash is): `ehrt.sim.run-test`/
`ehrt.sim.identifiers-test` — 4 errors + 1 failure (the raw
`FileNotFoundException`/`RuntimeException`/`ClassCastException`
crashes, plus one clean assertion failure on the opts-passthrough
identity); `ehrt.cli.core-test` — 3 clean assertion failures (the
group-hint test, and two assertions inside the bare-invocation test).
Full transcripts below, including the founding incident's own exact
shape reproduced live: `FileNotFoundException: config/busy-weekday.edn
(No such file or directory)`.

Two additional pre-existing tests, found only once the fix ran (not
anticipated by the register): `main-bang-bare-invocation-prints-usage-
and-exits-two-test` (a `main!`-level exit-2 assertion the dispatch-
level test at line ~146 doesn't cover) — renamed to
`main-bang-bare-invocation-prints-usage-and-exits-zero-test`, assertions
flipped to 0, disclosed here as a fix-forward (the register's own
B-5/D-1 rows named the dispatch-level symptom; this deeper duplicate
assertion was a genuine surprise). Also fixed two Clojure paren-balance
bugs introduced by this session's own restructuring of `run-command`/
`identifiers-command` (each missing exactly one closing paren after
the new `if-not`/`let` wrapping) — caught immediately by `clojure -M:poly
check`'s `Error 111: Unreadable namespace`, fixed before any test ran
green, disclosed as a mechanical slip, not a design question.

All fixes landed; full green: `clojure -M:poly check`: OK; `clojure
-M:poly test` (workspace root, ADR-0059's own cwd lesson unbroken):
314 passes, 0 failures, 0 errors, every brick, 2m6s. Manual fresh-probe
confirmation (the design channel's own upcoming verification, run here
first as a sanity check): bare `bin/ehrt` → exit 0; `bin/ehrt sim run
--seed 1 --patients 1 --config config/busy-weekday.edn` → `{:status
:error, :category :config-not-found, :payload {:path
"config/busy-weekday.edn", :did-you-mean "config/busy-weekday.md"}}`,
exit 2 (clean, not a crash, not exit 1); `bin/ehrt sim` → `:hint "run:
ehrt help sim"`. Commit (ONE commit, staging hygiene confirmed —
exactly the 8 files: `core.clj`, `help.clj`, `core_test.clj`,
`identifiers.clj`, `run.clj`, `identifiers_test.clj`, `run_test.clj`,
`docs/cli.md`; `config/busy-weekday.md` confirmed not staged):
`63f27e8` ("fix: errors name their artifact — config crash dies, hints
know their group, bare help succeeds (ux fixes 2,
AR-U2-1/2/3/4)"). `gitleaks git --staged -v`: clean. Pushed; post-push
verification: one delta against the message file, the known harmless
trailing-newline artifact.

**Step 3 — this record.** `notes/ADRs.md` gains this ADR's own index
line; `notes/adr/README.md`'s own file count corrected (the file was
already one behind reality — 57 files existed before this session,
not 56 as the README stated; both drifts corrected together, 57→58,
"as of ADR-0060"). Done pointer added to `.agents/plans/roadmap.md`
in the same commit as the index line. Oracle bracket
(`bin/regression-oracle c998c7b 63f27e8` — Step 0's own tip to Step
2's own closing commit, the last behavior-bearing one; this record's
own commit adds no source, so it is not itself a bracket endpoint,
the same disposition ADR-0059's own bracket used): **`IDENTICAL: every
root's digest matches between c998c7b and 63f27e8`** — all ELEVEN
vendored-root batches (`appendicitis`, `death-fixture`,
`ear-infections`, `ear-infections-engine`,
`ear-infections-history-engine`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`,
`urinary-tract-infections-history-engine`) byte-identical, exactly as
AR-U2-5 required — error paths and help dispatch touch no emitted
byte. Soundness check: `digest.clj` identical outside its own `(ns
...)` form (the script's own preflight, printed before either
worktree ran); no `--declared-digest-change` needed. Session record
(`.agents/session-records/2026-08-06-ux-fixes-2.md`) and this
session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-2.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md`
in the same commit.

### Verification

- `clojure -M:poly check`: OK, every step (including immediately after
  the paren-balance slip was fixed, before any test ran).
- Red→green, Step 1: 5 failures (naive tokenizer, one false positive
  from a shell comment) → 4 failures (comment-stripping added, the
  real stale `--config` fences only) → 0 failures (fences rewritten).
- Red→green, Step 2: 4 errors + 1 failure (`ehrt.sim.run-test`) + 1
  error (`ehrt.sim.identifiers-test`) + 3 failures (`ehrt.cli.core-
  test`) → 0 failures/0 errors across all three namespaces, same test
  files, no test logic changed between red and green captures.
- `clojure -M:poly test`: green, every brick, 314 passes/0 failures/0
  errors, workspace root (ADR-0059's own cwd lesson respected
  throughout — every test run this session ran from
  `~/src/ehr-testing-tools`, never a subdirectory).
- `gitleaks`: clean at every scan (staged scan before each of the two
  commits; the push's own pre-push hook run, both pushes).
- Post-push message verification: one delta against the message file
  at each of the two pushes, the known harmless trailing-newline
  artifact both times.
- Tag verification: `stable-20260806-ux-fixes-1` peeled ref resolves
  to `c998c7b` exactly.
- **Oracle bracket** (`bin/regression-oracle c998c7b 63f27e8`):
  `IDENTICAL: every root's digest matches between c998c7b and
  63f27e8` — all eleven vendored-root batches byte-identical; per
  AR-U2-5, any digest change here would have been STOP-AND-ESCALATE.
- Manual fresh-probe: bare invocation, the founding incident's exact
  command shape, and the `sim`-with-no-verb hint all confirmed live
  against the built `bin/ehrt`, not only under `clojure.test`.

### Fences (standing law applies unchanged, this session's own prompt)

Src edits landed ONLY in `merge-config-file` and its direct callers'
error propagation (`run-command`, `identifiers-command`); the two
dispatch sites for the hint (`unknown-command-error`) and the
bare-invocation exit (`bare-invocation-response`); the one
exit-codes-table line (`help.clj`, plus its generated `docs/cli.md`
mirror). No other `help.clj` text changed. No flag-parsing changes
(C-4 is a later session). No gate weakening — the extended
`invocation-lint-test` only adds an assertion. Frozen archives
untouched apart from this ADR + index + Done pointer + session-record/
prompt archival, all sanctioned. `result->exit-code`/the dispatch
structure matched the register's own reading exactly — no STOP-AND-
REPORT was triggered.

### Consequence

The founding incident — `bin/ehrt sim run --config
<missing-or-malformed>` crashing with a raw, uncategorized JVM stack
trace instead of a Result — can no longer happen: every `--config`
failure now names its own artifact, its own category, and (when a
same-stem sibling exists) its own likely fix, at the same exit code
every other operational error in this CLI already uses. A stranger
typing bare `ehrt`, or `ehrt sim` to see what it does, now gets the
SAME successful exit code the documented `--help` convention already
promises, and a hint that already knows which group they meant. The
demo fences the prior session swept now actually run, not just
parse — closing the exact verification gap (grammar vs. path
validity) that produced this session's own rider. After landing: the
design channel verifies by fresh probe, including re-running the
founding incident's exact command shape against the fixed tree (this
session's own manual check, above, already did so once); session 3
(C-4 unknown-flag validation) follows, and this landing's own tag
rides its Step 0 under standing ceremony.

### Step 3 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line;
`notes/adr/README.md`'s own file count corrected 57→58 ("as of
ADR-0060"). Done pointer added in the same commit as the index line:

```
- 2026-08-06 — ux-fixes-2 — ADR-0060
```

Session record
(`.agents/session-records/2026-08-06-ux-fixes-2.md`) and this
session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-2.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md` in
the same commit.

### Appendix — red transcripts

**Step 1 (rider), against the unswept tree, final pass (comment-
stripping already added — the real 4 failures only):**

```
FAIL in (fence-path-arguments-resolve-from-workspace-root-test) (invocation_lint_test.clj:140)
components/sim/docs/demos/emit-state/README.md has a command fence path argument that does not resolve from workspace root: ({:flag "--config", :value "docs/demos/order-result/config.edn"} {:flag "--config", :value "docs/demos/order-result/config.edn"}) -- grammar-validity is not path-validity (AR-U2-R, ADR-0060)
expected: (empty? problems)
  actual: (not (empty? ({:flag "--config", :value "docs/demos/order-result/config.edn"} {:flag "--config", :value "docs/demos/order-result/config.edn"})))

FAIL in (fence-path-arguments-resolve-from-workspace-root-test) (invocation_lint_test.clj:140)
components/sim/docs/demos/order-result/README.md has a command fence path argument that does not resolve from workspace root: ({:flag "--config", :value "docs/demos/order-result/config.edn"}) -- grammar-validity is not path-validity (AR-U2-R, ADR-0060)
expected: (empty? problems)
  actual: (not (empty? ({:flag "--config", :value "docs/demos/order-result/config.edn"})))

FAIL in (fence-path-arguments-resolve-from-workspace-root-test) (invocation_lint_test.clj:140)
components/sim/docs/demos/module-mix/README.md has a command fence path argument that does not resolve from workspace root: ({:flag "--config", :value "docs/demos/module-mix/config.edn"}) -- grammar-validity is not path-validity (AR-U2-R, ADR-0060)
expected: (empty? problems)
  actual: (not (empty? ({:flag "--config", :value "docs/demos/module-mix/config.edn"})))

FAIL in (fence-path-arguments-resolve-from-workspace-root-test) (invocation_lint_test.clj:140)
components/sim-emit-hl7/docs/demos/site-profiles/README.md has a command fence path argument that does not resolve from workspace root: ({:flag "--config", :value "docs/demos/site-profiles/config-aldric.edn"}) -- grammar-validity is not path-validity (AR-U2-R, ADR-0060)
expected: (empty? problems)
  actual: (not (empty? ({:flag "--config", :value "docs/demos/site-profiles/config-aldric.edn"})))

Ran 4 tests containing 221 assertions.
4 failures, 0 errors.
```

**Step 2 (C-1/U4), `ehrt.sim.run-test`/`ehrt.sim.identifiers-test`,
against the unfixed `merge-config-file` — the founding incident
reproduced live:**

```
ERROR in (merge-config-file-returns-config-not-found-for-a-missing-path) (FileInputStream.java:-2)
Uncaught exception, not in assertion.
expected: nil
  actual: java.io.FileNotFoundException: /tmp/merge-config-file-test.../does-not-exist.edn (No such file or directory)
 at java.io.FileInputStream.open0 (FileInputStream.java:-2)
    ...
    ehrt.sim.run$merge_config_file.invokeStatic (run.clj:207)

ERROR in (merge-config-file-suggests-a-same-stem-sibling-file) (FileInputStream.java:-2)
Uncaught exception, not in assertion.
expected: nil
  actual: java.io.FileNotFoundException: config/busy-weekday.edn (No such file or directory)
 at java.io.FileInputStream.open0 (FileInputStream.java:-2)
    ...

ERROR in (merge-config-file-returns-config-unreadable-for-malformed-edn) (Util.java:221)
Uncaught exception, not in assertion.
expected: nil
  actual: java.lang.RuntimeException: EOF while reading
 at clojure.lang.Util.runtimeException (Util.java:221)
    clojure.lang.EdnReader.readDelimitedList (EdnReader.java:746)
    ...
    ehrt.sim.run$merge_config_file.invokeStatic (run.clj:207)

FAIL in (merge-config-file-with-no-config-key-is-the-identity-on-opts) (run_test.clj:309)
expected: (= (result/ok {:seed 1, :patients 2}) (run/merge-config-file {:seed 1, :patients 2}))
  actual: (not (= {:status :ok, :payload {:seed 1, :patients 2}} {:seed 1, :patients 2}))

ERROR in (run-command-propagates-config-not-found-unchanged) (FileInputStream.java:-2)
Uncaught exception, not in assertion.
expected: nil
  actual: java.io.FileNotFoundException: /tmp/merge-config-file-test.../does-not-exist.edn (No such file or directory)
 at java.io.FileInputStream.open0 (FileInputStream.java:-2)
    ...
    ehrt.sim.run$run_command.invokeStatic (run.clj:301)

ERROR in (identifiers-command-propagates-config-unreadable-unchanged) (core.clj:84)
Uncaught exception, not in assertion.
expected: nil
  actual: java.lang.ClassCastException: class clojure.lang.Symbol cannot be cast to class clojure.lang.IPersistentCollection ...
 at clojure.core$conj__5476.invokeStatic (core.clj:84)
    clojure.core$merge$fn__6049.invoke (core.clj:3077)
```

**Step 2 (B-5/B-6), `ehrt.cli.core-test`, against the unfixed
`core.clj`:**

```
FAIL in (dispatch-unknown-verb-in-a-real-group-hints-that-groups-own-help-test) (core_test.clj:110)
expected: (= "run: ehrt help sim" (:hint (:payload r)))
  actual: (not (= "run: ehrt help sim" "run: ehrt help"))

FAIL in (dispatch-bare-invocation-succeeds-with-usage-text-test) (core_test.clj:163)
expected: (result/ok? r)
  actual: (not (result/ok? {:status :error, :category :cli-help, :payload {:text "Usage: ehrt <group> [<verb>] [flags]..."}}))

FAIL in (dispatch-bare-invocation-succeeds-with-usage-text-test) (core_test.clj:165)
expected: (= 0 (cli/result->exit-code r))
  actual: (not (= 0 2))

FAIL in (main-bang-bare-invocation-prints-usage-and-exits-zero-test) (core_test.clj:1926)
expected: (= 0 code)
  actual: (not (= 0 2))

FAIL in (main-bang-bare-invocation-prints-usage-and-exits-zero-test) (core_test.clj:1927)
expected: (= 0 @exit-code)
  actual: (not (= 0 2))

Ran 230 tests containing 594 assertions.
3 failures, 0 errors.
```

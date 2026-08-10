## ADR-0098 — Permission-denied gate legs categorized across the judge family; bare-level unknown flags routed (D8-4)

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

The roadmap Next row (`.agents/plans/roadmap.md`, "`ehrt gate fhir PATH`'s
own permission-denied leg, true name") anchors this session, citing
`notes/ADRs.md` ADR-0096 Finding 1 and ADR-0097 AR-AC-1 item 1 (the
anchor law, AR-RL2-3, first execution at an actual arc close). ADR-0096's
own Finding 1: `ehrt gate fhir PATH` against a chmod-000 file raises the
same-looking raw `FileNotFoundException`, but its stack bottoms three
frames past `core.clj` — `ehrt.judge-fhir-official.fhir/gate-file` →
`verdict-cache-lookup` → `ehrt.kernel.digest/sha256-file` — never
reachable from `bases/cli/src`, outside cluster B's own fence
("nothing in corpus/kernel/sim/judge/engine src"). Cluster B fixed the
two in-fence legs (bare `gate` dispatch, `show`); this leg was
disclosed, not fixed, and anchored to its own `Next` row by ADR-0097.

**Charter width, ruled** (2026-08-09, author verbatim "Q1 a."): the
charter widens beyond `judge-fhir-official` alone to all three judge
engines this session touches gate-file — fhir, v2-hapi, v2-nist — since
`judge-v2-hapi`/`judge-v2-nist`'s own `gate-file` guards share the
identical defect shape (an `.isFile`-only entry check that a
chmod-000 path passes, followed by a bare `slurp` that throws raw) and
their own docstrings already promise "a readable file," currently false
for this leg.

**Category shape, ruled** (2026-08-09, author verbatim "Q2 a."):
`:file-not-found` stays the single family category for both the
missing and unreadable legs, adding a distinguishing payload key on
the unreadable leg (`:reason :permission-denied`) — no new category,
family parity (ruled 2026-07-31) preserved, and the docstrings'
existing "readable file" wording becomes literally true.

**The fhir entry-guard placement** (channel-inferred, verified against
the live tree before acting): the guard belongs at the component entry
— a shared private check used by both `gate-file` and `gate-batch`,
before any `verdict-cache-lookup` — never inside
`kernel.digest/sha256-file`, which returns a bare hex string consumed
at six sites across four components; the fix applies where THIS read
actually lives, per the roadmap row's own words.

**D8-4 rider, ruled** (2026-08-09, author verbatim "I choose a."): bare/
`help`-level unknown flags — currently silently swallowed (help
printed, exit 0) while subcommands report `:unknown-flag` — route
through the same `:unknown-flag` category, in this same session, with
its own red→green evidence. `docs/cli.md` is not touched by this
anchor (option (b) of D8-4's own register text stays struck).

Read first: `.agents/plans/roadmap.md`'s Next row (verbatim, the row
this session executes); `notes/adr/0097-review-2-arc-close.md`'s
mechanical-debt section (the tag ceremony this session's own Step 1
executes); `notes/adr/0096-cluster-b-parse-guards.md` Finding 1 (the
original live red, and cluster B's own precedented fix shape this
session extends); `components/judge-fhir-official/src/ehrt/judge_fhir_official/fhir.clj`,
`components/judge-v2-hapi/src/ehrt/judge_v2_hapi/v2.clj`,
`components/judge-v2-nist/src/ehrt/judge_v2_nist/v2.clj` (the three
`gate-file` entry guards and their docstrings); `bases/cli/src/ehrt/cli/core.clj`
(`dispatch`'s cond, `unknown-flag-error`, `flag-validation-context`);
`bases/cli/src/ehrt/cli/help.clj` (`global-flags`, the bare-level valid
flag set).

### Decision

**AR-1 (the judge-family fix).** A private entry check —
`check-readable` in `judge-fhir-official`, an extended `cond` in the
existing `.isFile` guard for `judge-v2-hapi`/`judge-v2-nist` — returns
`kernel/error :file-not-found {:path}` for a missing path and
`kernel/error :file-not-found {:path :reason :permission-denied}` for
an exists-but-unreadable one (Java's own `File` semantics: `.isFile`
true, `.canRead` false on a chmod-000 path, confirmed live in this
session's own red-evidence pass). In `judge-fhir-official/fhir.clj`,
`check-readable` runs at the top of BOTH `gate-file` and `gate-batch`
(the latter fails fast on the first bad path among its own `paths`
argument, in order — matching `gate-batch`'s own existing
first-failing-step contract for every other step). `gate-dir` is
covered transitively via `gate-file`, no separate change. In
`judge-v2-hapi/v2.clj` and `judge-v2-nist/v2.clj`, the existing
`.isFile`-only guard in `gate-file` becomes a three-armed `cond` adding
the `.canRead` leg, same category, same payload key. All three
docstrings updated to name the `:reason` key. No other reshaping (move,
don't improve) — the read this whole pipeline eventually does was
already there; only the guard around it changed.

**AR-2 (the D8-4 rider).** `bases/cli/src/ehrt/cli/core.clj`'s
`dispatch` short-circuits on `(:help opts)`, `(= group "help")`, and
`(nil? group)` before `validate-known-flags` ever runs (that call sits
in the `:else` branch's own `(or ...)`, line ~2076) — so a typo'd flag
at the bare or `help` level was silently absorbed into a help render
instead of being rejected by name. Scoped to exactly the two branches
the rider names ("bare/`help`-level"): `(= group "help")` and
`(nil? group)`. `(:help opts)` (`--help` given anywhere, including
alongside a real group/verb) is UNTOUCHED — out of the rider's own
named scope, and the acceptance criterion "`ehrt --help` alone still
prints help and exits 0" depends on this branch staying exactly as it
was. A new private `validate-top-level-flags` checks `opts`'s keys
against `help/global-flags`' own keyword set (the complete bare/help-
level valid surface: `--json`/`--pretty`/`--edn`/`--help`/`--width` —
`--width` is already declared there, confirmed by reading `help.clj`
directly rather than assumed) and returns the identical
`unknown-flag-error`/did-you-mean shape a real subcommand's own
`validate-known-flags` returns on a miss. Wired ahead of each of the
two branches' own width-resolution/response call, so an unknown flag
is caught before any help text renders. `verb-label` in the error
payload: `"ehrt"` for the bare case (there is no group to name), `"help"`
for the help-verb case — mirroring `flag-validation-context`'s own
group-as-verb-label fallback for a no-verb group.

### Red evidence (judge family, captured before any fix; `whoami`: `mg`, `id -u`: 1000, non-root)

**`ehrt gate fhir` on a chmod-000 `.json` file:**

```
Execution error (FileNotFoundException) at java.io.FileInputStream/open0 (FileInputStream.java:-2).
<scratch>/unreadable.json (Permission denied)
```
exit 1. Full stack bottoms exactly as ADR-0096 Finding 1 predicted:
```
[ehrt.kernel.digest$sha256_file invokeStatic "digest.clj" 11]
[ehrt.judge_fhir_official.fhir$verdict_cache_lookup invokeStatic "fhir.clj" 145]
[ehrt.judge_fhir_official.fhir$gate_file invokeStatic "fhir.clj" 409]
[ehrt.cli.core$fhir_gate_command$fn__10451 invoke "core.clj" 970]
[ehrt.cli.core$dispatch invokeStatic "core.clj" 2104]
```

**`ehrt gate fhir /nonexistent/no.json` (missing path, actual behavior
— disclosed per the driving prompt's own instruction, not assumed):**

```
Execution error (FileNotFoundException) at java.io.FileInputStream/open0 (FileInputStream.java:-2).
/nonexistent/no.json (No such file or directory)
```
exit 1 — ALSO raw, same mechanism, not categorized anywhere before this
session. This is `judge-fhir-official`'s first-ever entry check for
either leg, not just the unreadable one.

**Component-level reds, `judge-v2-hapi`/`judge-v2-nist` `gate-file` on
the same chmod-000 file** (direct component call — classpath via
`clojure -Spath -M:ehrt` fed to a bare `clojure -Scp ... -M -e`, since
`-M:ehrt` alone forces `-m ehrt.cli.core` and swallows `-e`):

```
judge-v2-hapi:  THREW: java.io.FileNotFoundException <scratch>/unreadable.json (Permission denied)
judge-v2-nist:  THREW: java.io.FileNotFoundException <scratch>/unreadable.json (Permission denied)
```
Both confirm the `.isFile`-passes/bare-`slurp`-throws shape exactly;
`judge-v2-nist`'s own `validator-state` was `nil` in this probe — the
guard throws before it is ever touched.

### Green evidence (judge family, post-fix)

```
$ ehrt gate fhir <chmod-000 file>
{:status :error, :category :file-not-found, :payload {:path "...", :reason :permission-denied}}
exit 2

$ ehrt gate fhir /nonexistent/no.json
{:status :error, :category :file-not-found, :payload {:path "/nonexistent/no.json"}}
exit 2
```

Component-level: `check-readable`/the extended `.isFile` guard proven
red→green by `git stash push --keep-index` isolating the three fixed
`src` files away from their own new tests (`fhir_test.clj`,
`judge-v2-hapi/v2_test.clj`, `judge-v2-nist/v2_test.clj`) — pre-fix:
11 failures, 2 errors (uncaught `FileNotFoundException` in both v2
engines' own new permission-denied tests, plus assertion failures
everywhere the new `:file-not-found`/`:permission-denied` shape was
asserted); `git stash pop` restored the fix; post-fix: 59 tests, 168
assertions (including `judge-v2-nist/v2-engine-test`), 0 failures, 0
errors.

### Co-landed tests

Per engine, a `deftest` creates a temp file, `chmod 000`s it via
`clojure.java.shell/sh` (the same shell-out convention
`bases/cli/test/ehrt/cli/executable_bits_test.clj` already uses),
guarded — `(.canRead f)` still true after the chmod names a root (or
equivalent) environment, in which case the test prints an explicit
`SKIPPED ...` message and passes trivially rather than fail or lie —
then asserts the categorized `:file-not-found`/`:permission-denied`
result. `judge-fhir-official/fhir_test.clj` gains three: the missing-
path leg (its first-ever entry-check test), the unreadable-path leg on
`gate-file`, and the unreadable-path leg on `gate-batch` (asserting
fail-fast: `[readable-path unreadable-path]` reports the unreadable one
by name, the readable sibling never reached). `judge-v2-hapi/v2_test.clj`
and `judge-v2-nist/v2_test.clj` each gain one unreadable-path test
(missing-path was already covered pre-session for both engines);
`judge-v2-nist`'s own test lives in `v2_test.clj` (the pure/synthetic-
capture file, not the real-engine `v2_engine_test.clj`) since the guard
runs before `validator-state` is ever touched — a `nil` validator-state
is safe, matching that file's own existing convention for the msg-id-
contract tests.

None of the three permission-denied tests were skipped in this
session's own environment (confirmed: no `SKIPPED` line in the test
run's own output) — `id -u` 1000 held for the whole session.

### Verification (judge family)

- `clojure -M:test:ehrt` (via `clojure -Spath` fed to a bare `-Scp`,
  the same classpath-without-forced-main-opts technique the red
  evidence itself needed): `ehrt.judge-fhir-official.fhir-test`,
  `ehrt.judge-v2-hapi.v2-test`, `ehrt.judge-v2-nist.v2-test`,
  `ehrt.judge-v2-nist.v2-engine-test` — 59 tests, 168 assertions, 0
  failures, 0 errors.
- `clojure -M:poly check`: OK, at this commit (`ae34a98`).
- Full local suite, oracle bracket, and `bin/verify-nist-lock`:
  recorded in this ADR's own close-phase append, below, after the D8-4
  rider fix lands alongside.

### Fences (this commit)

Src/test changes in `components/judge-fhir-official/{src,test}`,
`components/judge-v2-hapi/{src,test}`, `components/judge-v2-nist/{src,test}`
ONLY. No `bases/cli` touch in this commit (the D8-4 rider is a
separate commit, tracked below). No `result->exit-code`/exit-mapping
change — every new payload rides the existing generic `:error` → exit
2 rule. No `kernel.digest` touch — the guard sits at each component's
own entry, ahead of any digest call, per the channel-inferred
placement ruling above.

---

### D8-4 rider: red evidence (captured before any fix)

```
$ ehrt --hlep
Usage: ehrt <group> [<verb>] [flags]
...
$ echo $?
0

$ ehrt help --hlep
Usage: ehrt <group> [<verb>] [flags]
...
$ echo $?
0
```

Both print the full top-level usage text and exit 0 — the typo'd flag
silently absorbed, exactly the shape the rider names, confirmed live
before any code changed.

### D8-4 rider: green evidence (post-fix)

```
$ ehrt --hlep
{:status :error, :category :unknown-flag, :payload {:flag "--hlep", :verb "ehrt", :did-you-mean "--help"}}
$ echo $?
2

$ ehrt help --hlep
{:status :error, :category :unknown-flag, :payload {:flag "--hlep", :verb "help", :did-you-mean "--help"}}
$ echo $?
2

$ ehrt --help
Usage: ehrt <group> [<verb>] [flags]
...
$ echo $?
0

$ ehrt help
Usage: ehrt <group> [<verb>] [flags]
...

$ ehrt help gate
ehrt gate -- Conformance-gate a file or directory against HL7 v2, FHIR, or ...
```

Every acceptance criterion in the rider's own row text confirmed
directly against the real CLI: the typo'd bare/`help`-level flag now
categorizes with the subcommand exit semantics and a did-you-mean;
`--help` alone, bare `help`, and `help <group>` are all unchanged.

Component-level red→green: `git stash push --keep-index` isolated
`bases/cli/src/ehrt/cli/core.clj`'s own fix away from the two new
`core_test.clj` tests — pre-fix: 12 failures (6 assertions per test,
both new tests, `:status :ok`/`:category :cli-help`/exit 0 instead of
the expected categorized shape); `git stash pop` restored the fix;
post-fix: `ehrt.cli.core-test` — 259 tests, 762 assertions, 0 failures,
0 errors.

### D8-4 rider: co-landed tests

`bases/cli/test/ehrt/cli/core_test.clj` gains three: the bare-level
typo (`dispatch-bare-unknown-flag-is-rejected-by-name-test`, asserting
category, flag name, `:verb "ehrt"`, did-you-mean, and exit code 2),
the `help`-verb-level typo (`dispatch-help-verb-unknown-flag-is-
rejected-by-name-test`, identical shape, `:verb "help"`), and an
acceptance-property test mirroring AR-U3-4c one level up
(`dispatch-bare-every-declared-global-flag-parses-without-unknown-
flag-test`: every `help/global-flags` entry parses without
`:unknown-flag` at both the bare and `help`-verb level — spec-derived,
so a future global flag is automatically covered).

### Verification, in full (both fix commits)

- `clojure -M:poly check`: OK, after each commit and again at this
  close.
- `ehrt.judge-fhir-official.fhir-test`, `ehrt.judge-v2-hapi.v2-test`,
  `ehrt.judge-v2-nist.v2-test`, `ehrt.judge-v2-nist.v2-engine-test`: 59
  tests, 168 assertions, 0 failures, 0 errors. None of the three
  permission-denied tests skipped (non-root environment held for the
  whole session).
- `ehrt.cli.core-test`: 259 tests, 762 assertions, 0 failures, 0
  errors.
- `ehrt.cli.cli-parse-guard-lint-test`: 3 tests, 18 assertions, 0
  failures, 0 errors (unaffected by either fix — neither touches a
  bare `slurp`/`edn/read-string`/`json/read-str` call site in
  `core.clj`; the D8-4 fix adds a flag-validation call, not a read).
- Full local suite (`clojure -M:poly test :all skip:integration`): run
  twice at this close. First run: one failure, an unrelated
  `sim-engine` `defspec` flake, disambiguated live and disclosed in
  the Deviations section above (not fixed, out of fence). Second run,
  immediately after: 0 failures, 0 errors anywhere in the log (grepped
  in full), 4m33s — the run this close-phase commit relies on.
- `bin/verify-nist-lock`: OK, 6 hit-nexus-sourced coordinates match
  `artifacts.lock.edn` exactly, exit 0.
- `make docsgen`: run before the D8-4 commit; `git status --porcelain
  docs/` empty both before and after — `docs/cli.md` produces no diff,
  confirming the ruling ("`docs/cli.md` is not touched by this
  anchor") holds structurally, not just by omission.
- Oracle bracket (`bin/regression-oracle 558e6bf3573b7f063c1b22eb04541c1c42b4906f 104329fcfc955a8573572de4d3a79fcd251fc38a`
  — this session's own opening tag to the post-fix tip, both fix
  commits included): all THIRTY-FOUR roots IDENTICAL — pure identity,
  as predicted (`ehrt.oracle.digest`'s own requires touch only
  sim-trajectory/sim-model/sim-engine/emit-hl7, never judge or
  kernel.digest; this session's fence touches only judge components
  and bases/cli).
- `gitleaks git --staged -v`: clean at both commits.
- Tag verification: `stable-20260809-review-2-arc-close` (Step 1, this
  session) tagged at `558e6bf3573b7f063c1b22eb04541c1c42b4906f`, pushed,
  peeled ref resolves to that commit exactly.
- Post-push message verification and the ASCII check (`git log
  --format=%B -1 <sha> | LC_ALL=C grep -n '[^ -~]'`, AR-RL2-5), both
  fix commits: message diffs against their own source file show only
  `git log --format=%B`'s own trailing-blank-line artifact; ASCII
  check empty on both.
- `git status --porcelain`: clean before this session's first tool
  call.
- Last five `test`-lane runs on `main` (`gh run list --limit 5
  --branch main`), checked at Step 0 (before this session's first
  commit): all `completed`/`success`
  (`31344918738`, `31340857607`, `31340341691`, `31330881843`,
  `31330580554`).

### Fences, both commits

Commit 1 (judge family): `components/judge-fhir-official/{src,test}`,
`components/judge-v2-hapi/{src,test}`, `components/judge-v2-nist/{src,test}`,
plus `notes/adr/0098-*.md` (created), `notes/ADRs.md` (index row),
`notes/adr/README.md` (count 95→96) — landed together, matching the
cluster B precedent (`git show a2c31c8 --stat`: ADR file + index +
count bundled with the fix, not deferred to a later commit). Commit 2
(D8-4 rider): `bases/cli/{src,test}` ONLY — no `docs/cli.md` touch
(verified structurally above), no `result->exit-code` change, no
`(:help opts)` branch touch. Neither commit touches
`kernel`/`corpus`/`sim`/`engine`/`oracle` src. `.agents/plans/roadmap.md`,
`.agents/rulings.md`, `.agents/state.md`, and the session
record/prompt archive land in this session's own third, docs-only
close-phase commit, not either fix commit.

### Deviations, dated 2026-08-09

None found against this session's own driving prompt. The prompt's own
channel-inferred claims (line numbers, the `.exists()`-only diagnosis,
the `:else` branch's flag-validation ordering) were each verified
against the live tree before being built on (per the prompt's own
verify-then-act instruction) and held exactly as stated — no
correction needed. The one genuine judgment call this session made
(the `verb-label` strings `"ehrt"`/`"help"` for the two rider
branches, and the `check-readable`/guard-`cond` naming/shape in each
judge component) was a naming/placement choice within the ruled
category shape (Q2 "a."), not a deviation from any ruled or
channel-inferred claim.

**A newly-found, unrelated flake, disambiguated live rather than
smoothed past (the ADR-0084 precedent).** The first full-suite run
after the D8-4 rider fix landed (`clojure -M:poly test :all
skip:integration`) reported one failure:
`ehrt.sim-engine.engine-test/mixed-authored-and-compiled-run-
satisfies-the-full-invariant-catalog` — an unpinned `defspec` (150
generated cases, `seed 665147938144496768`, shrunk to a single
`:smallest` input) in `components/sim-engine`, entirely outside this
session's own fence (judge components and `bases/cli` only). Per the
standing `defspec` seed policy (`.agents/rulings.md`, AR-RL-5(3)):
seeds stay unpinned repo-wide for generator diversity; a spec that has
actually flaked pins its seed — this one hasn't yet, and isn't the
already-pinned sibling (`every-churned-run-satisfies-the-invariant-
catalog`, `:seed -60645`, ADR-0079). Re-run three times in isolation
(`t/run-test-var`, three fresh random seeds): passed all three. A
genuine intermittent flake, not a regression this session's own
judge-family/CLI changes could plausibly cause (different component,
no shared code path). Disclosed here, NOT fixed (out of fence — any
`sim-engine` edit is outside "components/judge-fhir-official,
judge-v2-hapi, judge-v2-nist, bases/cli"), and NOT silently re-run
past — the full suite was re-run once more in full immediately after
(clean, 0 failures/0 errors, logged below) before this close-phase
commit landed, so no push carries a knowingly-failing test. A future
session's own D3-2 SOAK (the "both flakes" carry ADR-0092 already
named for review 3) should treat this as a third named intermittent,
not restart from zero.

### Consequence

`ehrt gate fhir PATH`'s own permission-denied leg — the review-2 arc's
own first anchored finding under the new roadmap-anchor law (AR-RL2-3)
— is fixed, widened by author ruling to the full judge family rather
than scoped to fhir alone: `judge-fhir-official`, `judge-v2-hapi`, and
`judge-v2-nist` all now categorize both the missing and the
exists-but-unreadable leg through the same `:file-not-found` category,
distinguished by a `:reason :permission-denied` payload key, proven
red-to-green per engine with a root-environment-guarded chmod-000
test. `judge-fhir-official` gains its first-ever entry check for
either leg (previously unguarded even for a plain missing path). The
D8-4 rider closes the last named gap in this repo's own unknown-flag
discipline (AR-U3-2 through AR-U3-4): a bare or `help`-level typo now
reports `:unknown-flag` with the same did-you-mean machinery and exit
semantics a real subcommand's typo already had, while `--help` itself,
bare `help`, and `help <group>` stay exactly as they were. The oracle
holds pure identity across all 34 roots — this session touched only
judge components and CLI-shell code, never sim/engine-path work.

### Index line

```
- 2026-08-09 — permission-legs-and-bare-flags — ADR-0098
```

(appended to `.agents/plans/roadmap.md`'s own Done section, alongside
the Next-row removal this same commit makes.)

### Dated append, 2026-08-09 — a CI transient on the close-phase push, disclosed fix-forward

The close-phase commit's own push (`104329f..10c4d0e`) triggered a
`test`-lane run (`31351267585`) that FAILED at `poly check` in 12s —
Maven Central returned 403 Forbidden resolving `org.clojure:clojure:
pom:1.12.5`, a registry-side transient with nothing to do with this
session's own diff (`deps.edn` untouched throughout). Investigated
before disclosure: `gh run rerun`, watched to conclusion, passed clean
on the identical commit, 3m53s, all four steps green. Full detail in
this session's own session record
(`.agents/session-records/2026-08-09-permission-legs-and-bare-flags.md`),
its own dated append. No code change, no amended commit — this is a
disclosure-only append, the same fix-forward discipline every other
close in this repo applies to a post-push finding.

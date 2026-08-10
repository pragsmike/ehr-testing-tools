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
- `clojure -M:poly check`: [recorded at commit time, below].
- Full local suite, oracle bracket, and `bin/verify-nist-lock`:
  recorded in this ADR's own close-phase append, after the D8-4 rider
  fix lands alongside.

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

*(D8-4 rider Decision/evidence and this session's own closing ceremony
— Verification in full, Deviations appendix, Consequence, Index line —
land in this same file's own append, below, once both fix commits are
in.)*

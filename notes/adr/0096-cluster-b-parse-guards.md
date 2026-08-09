## ADR-0096 — Cluster B: CLI reads guarded, categorized, linted

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

The author ruled 2026-08-09: "Cluster B." — ADR-0092's fix cluster B
(`.agents/plans/2026-08-09-repo-review-findings.md`, register rows
D4-5, D4-6, D4-7, D8-3). ADR-0092's own charter paragraph, quoted
verbatim:

> **Cluster B — the CLI parse-guard family (D4-5, D4-6, D4-7, D8-3).**
> Four unguarded reads sharing one root cause and one already-precedented
> fix shape in this same codebase (`kernel/artifact.clj/read-lockfile`'s
> categorized-rejection pattern, `sim/run.clj`'s config-loader
> try/catch): `mutate-command`'s per-file JSON read, `gate-command
> --baseline`'s EDN read, `check-command --assertions`'s EDN read
> (D4-5/6/7), and `corpus mutate`/`gate`/`show`'s file-open path on a
> permission-denied (not just missing) target (D8-3, AR-RL-3's own
> incomplete fix). One small session: wrap each read in a
> try/catch-around-the-read, matching the sibling pattern exactly, no
> new design. Co-landed gate: extend `io_vocabulary_lint_test.clj`'s own
> family (or a sibling lint) to also flag a bare `edn/read-string`/
> `json/read-str`/`slurp` on an operator-supplied CLI path with no
> enclosing `try` in the same function — the same shape of static check
> that already works for the `.listFiles`/`.renameTo` class.

The four register rows, quoted in full:

> **D4-5** | `read-base-data`'s `:fhir` branch (`bases/cli/core.clj:391`,
> `(json/read-str (slurp file))`), live-executed against a scratch
> malformed `.json` file via `mutate-command` | **Raw
> `java.io.EOFException` raised uncaught** — no `try`/`catch` anywhere
> in the call chain up through `-main`. Sibling function
> `corpus/display.clj`'s `render-fhir-json` DOES guard the same content
> shape with a categorized rejection. | A malformed input file mid-batch
> crashes the whole batch with a raw stack trace instead of a
> categorized rejection — loud, not silent (not D4-1-class), but real,
> live, and outside ADR-0078's own named scope. | Wrap the `:fhir`
> branch the same way `display.clj` already does. | fix-session-candidate

> **D4-6** | `gate-command`'s `--baseline` path (`core.clj:911`,
> `(edn/read-string (slurp baseline))`), live-executed against a
> malformed EDN file | **Raw `RuntimeException: EOF while reading`,
> uncaught.** Sibling `kernel/artifact.clj/read-lockfile` guards an
> almost-identical read with a categorized rejection. | A
> corrupt/truncated `--baseline` file (a plausible real mistake)
> produces a raw stack trace instead of a clean rejection. | Same guard
> shape as `read-lockfile`. | fix-session-candidate

> **D4-7** | `check-command`'s `--assertions` path (`core.clj:1552`,
> same `edn/read-string (slurp ...)` idiom), live-executed | Identical
> shape to D4-6 — raw, uncaught `RuntimeException`. | Same
> operator-facing gap as D4-6, different flag. | Same fix, can land in
> the same small session as D4-6. | fix-session-candidate

> **D8-3** | Deepened re-verify: `corpus mutate`/`gate fhir`/`show`
> against a path that EXISTS but is UNREADABLE (`chmod 000`), vs. a
> MISSING path | Missing path: clean everywhere (categorized
> `:file-not-found`, exit 2). **Permission-denied (exists, unreadable):
> all three commands still throw a raw, unhandled `FileNotFoundException`
> stack trace**, exit 1 — the exact bug class AR-RL-3 was supposed to
> close, just a wider trigger than the fence happened to test. Root
> cause: the fix added an `.exists()` pre-check only, not a `try`/`catch`
> around the actual read. `sim run --config` on the SAME unreadable file
> DOES return a clean categorized error, via a real `try`/`catch`
> (ADR-0060, predates AR-RL-3). | A live, reproducible INCOMPLETE fix:
> AR-RL-3 closed the literal case the fence hit but left a more
> realistic real-world trigger (permission-denied) open in three
> commands, with the correct pattern already sitting one file away. |
> Wrap `corpus mutate`/`gate`/`show`'s file-open paths in the same
> try/catch-around-the-read pattern `sim run`'s config loader already
> uses. | fix-session-candidate

Precedent shapes read in full before any fix (as the driving prompt's
own "Read first" named): `components/kernel/src/ehrt/kernel/
artifact.clj`'s `read-lockfile` (`.exists` pre-check, then
`try`/`catch` around `edn/read-string`+`slurp`, `:parse-failed`
category) and `components/sim/src/ehrt/sim/run.clj`'s config loader
(`.exists` pre-check, then `try`/`catch`, `:config-unreadable`
category). Both fixes below match this shape exactly.

### Decision

**AR-CB-1 (the four fixes):** `read-base-data` becomes result-returning
— `try`/`catch` around the existing `case` body, `result/ok` the value
or `result/error :base-data-unreadable {:path :message}` — its two
callers (`mutate-to-stdout!`, `mutate-command`'s directory-write loop)
short-circuit on the new Result, the minimal caller change. `gate-
command`'s `--baseline` read and `check-command`'s `--assertions` read
each get the identical `try`/`catch` wrap, `:baseline-unreadable` and
`:assertions-unreadable` respectively. `sniff-path-format` — the shared
helper `sniff-gate-command`'s bare-dispatch path AND `show-command`/
`show-file` all delegate to for D8-3's `gate`/`show` legs — becomes
result-returning the same way, `:path-unreadable`; its four call sites
(two direct, two via a new `sniff-files` helper that short-circuits a
directory scan on the first read failure) updated to unwrap. `show-
file`'s own SECOND read (rendering the already-sniffed content) is
guarded the same way, since it is the same command's own file-open path
D8-3 names — closing "show" end-to-end, not just its sniff step. No
`result->exit-code` mapping change: every new category is a plain
`result/error`, already mapped to exit 2 by the existing generic rule.

**AR-CB-2 (red evidence, live, before any fix):** all six raw failures
reproduced against scratch inputs at `b8fac5a` before Step 2 began —
pasted below, then the same six invocations green after the fix.

**AR-CB-3 (behavioral gates):** three new `deftest`s in `bases/cli/
test/ehrt/cli/core_test.clj` (the family convention — every other
`*-command` categorized-error test already lives there, so a sibling
file would fragment, not extend, the convention): malformed-JSON
`mutate-command`, malformed-EDN `gate-v2-command --baseline`,
malformed-EDN `check-command --assertions`, each asserting a
categorized `result/error` (never a thrown exception) and `(= 2
(cli/result->exit-code r))`. Permission-denied stays SESSION EVIDENCE
only (AR-CB-2), not a committed test — no existing skip-when-root/
non-POSIX guard convention was found anywhere in this test tree
(searched), and a `chmod`-based test that silently passes under a
root-running CI container is worse than no test at all (D3's own
environment-independence lesson, named in the driving prompt).

**AR-CB-4 (the co-landed lint):** `bases/cli/test/ehrt/cli/
cli_parse_guard_lint_test.clj`, function-granular (finer than the
sibling `io_vocabulary_lint_test.clj`, which scans whole-file text):
every top-level `defn`/`defn-` form in `bases/cli/src/ehrt/cli/core.clj`
is parsed with the Clojure reader (never a regex over raw text, the
same discipline `ehrt.docs-tooling.project-classpath-test`/
`sim-emit-hl7-dependency-test` already use) and walked for a bare
`edn/read-string`, `json/read-str`, or `slurp` call with no enclosing
`(try ...)` ancestor *within that same top-level form* — a `try` in a
different function guards nothing. Allowlist is BY FUNCTION NAME,
disclosed (mirroring the sibling's own `ehrt.sim.run` grandfather
clause): see the new finding below.

**AR-CB-5 (docs freshness):** `make docsgen` run before commit —
`docs/cli.md`'s exit-code table already reads exit 2 for every
operational error, generically, by category name never appearing in
its own text; no diff produced. Verified, not touched.

### Two new findings, disclosed and NOT fixed (out of this session's own fence)

**Finding 1 — `ehrt gate fhir PATH`'s own permission-denied leg is not
reachable from `bases/cli/src`.** AR-CB-2's own red-evidence pass (live,
before any fix) found that `ehrt gate fhir PATH` against a chmod-000
file raises the SAME-LOOKING raw `FileNotFoundException`, but its stack
bottoms out three frames past `core.clj` —
`ehrt.judge_fhir_official.fhir/gate-file` → `verdict-cache-lookup` →
`ehrt.kernel.digest/sha256-file` — reading the file for its
verdict-cache hash, never through any function this session's own fence
permits touching ("nothing in corpus/kernel/sim/judge/engine src"). The
register's own root-cause prose ("AR-RL-3's fix added an `.exists()`
pre-check only… lines 1097-1103, 1178-1184") describes exactly
`sniff-gate-command`/`show-command` — the bare `ehrt gate PATH` sniff
dispatch and `show`, both fixed here — not `fhir-gate-command`, which
never sniffs and never itself opens the file. Surfaced to the author
mid-session (AskUserQuestion); ruled: fix the two in-fence legs (bare
`gate`, `show`), disclose this one as a new register candidate rather
than widen the fence into `judge-fhir-official`/`kernel`. `ehrt gate
fhir PATH` on a permission-denied file still raises raw today — a
future session's own register row, not this one's.

**Finding 2 — `ehrt play`'s own two file-reading helpers carry the
identical bare-read shape, never named in the charter.** Discovered
while updating every caller of `sniff-path-format` (its signature
change from a bare value to a Result is a breaking change for every
existing caller, caught by the full local suite going red — 36 test
failures/3 errors in `play-command-*` tests before the callers were
fixed): `play-events-from-file` and `play-events-from-dir` both called
`sniff-path-format` directly, and `play-events-from-dir`'s own
per-file loop also calls `(slurp file)` bare. Both functions' own
calls to `sniff-path-format` were updated (mechanically, to unwrap the
now-Result-returning helper — required for correctness, not new
guarding) so `ehrt play` did not regress. Their OWN bare `slurp` reads
were left exactly as unguarded as before: `ehrt play` was never named
in ADR-0092's D4-5/D4-6/D4-7/D8-3 rows nor in this session's own AR-CB-2
test list, and guarding it would touch a fifth command this session was
never authorized to touch. The new lint (AR-CB-4) allowlists both
function names explicitly, by name, with this same disclosure inline in
its own docstring — the sibling lint's own `ehrt.sim.run` grandfather
clause is the precedent for the SHAPE of that allowlist, not the reason
for it. Confirmed non-vacuous: removing the allowlist entries and
re-running the lint against the live post-fix tree reports exactly
`[play-events-from-dir play-events-from-file]`.

A related labeling note, not a third finding: the lint's own live
witness pair (below) reports FIVE function names at the pre-fix tree,
not four — `read-base-data`, `gate-command`, `check-command`,
`sniff-path-format`, AND `show-file`. This is not a fifth site outside
the charter; D8-3's own "show" row spans two physical functions
(`sniff-path-format`, shared with `gate`, and `show-file`'s own second,
content-rendering read) — both are part of closing the SAME register
row's SAME command, both fixed, both report clean post-fix.

### The six-fold red/green evidence (AR-CB-2)

Scratch inputs under a session-scratch directory; `unreadable.json`
`chmod 000`, run as a non-root user (`id -u` = 1000, confirmed before
the D8-3 legs — a root-run chmod-000 test would silently lie).

**D4-5 — malformed `.json` through `corpus mutate` — before:**

```
Execution error at clojure.data.json/invalid-key-exception (json.clj:372).
JSON error (non-string key in object), found `n`, expected `"`
```

**— after:**

```
{:status :error, :category :base-data-unreadable, :payload {:path ".../malformed.json", :message "JSON error (non-string key in object), found `n`, expected `\"`"}}
```

exit 2.

**D4-6 — malformed EDN through `gate fhir --baseline` — before:**

```
Execution error at ehrt.cli.core/gate-command$fn (core.clj:911).
EOF while reading
```

**— after:**

```
{:status :error, :category :baseline-unreadable, :payload {:path ".../malformed-baseline.edn", :message "EOF while reading"}}
```

exit 2.

**D4-7 — malformed EDN through `check --assertions` — before:**

```
Execution error at ehrt.cli.core/check-command (core.clj:1552).
EOF while reading
```

**— after:**

```
{:status :error, :category :assertions-unreadable, :payload {:path ".../malformed-assertions.edn", :message "EOF while reading"}}
```

exit 2.

**D8-3 / `corpus mutate` on a chmod-000 file — before:**

```
Execution error (FileNotFoundException) at java.io.FileInputStream/open0 (FileInputStream.java:-2).
.../unreadable.json (Permission denied)
```

stack bottoms at `ehrt.cli.core$read-base-data (core.clj:391)`.

**— after:**

```
{:status :error, :category :base-data-unreadable, :payload {:path ".../unreadable.json", :message ".../unreadable.json (Permission denied)"}}
```

exit 2 (same fix as D4-5 — one function, two register rows, as the
driving prompt's own note predicted).

**D8-3 / bare `gate PATH` (sniff dispatch) on the same file — before:**

```
Execution error (FileNotFoundException) at java.io.FileInputStream/open0 (FileInputStream.java:-2).
```

stack bottoms at `ehrt.cli.core$sniff-path-format (core.clj:1077)`,
called from `sniff-gate-command`.

**— after:**

```
{:status :error, :category :path-unreadable, :payload {:path ".../unreadable.json", :message ".../unreadable.json (Permission denied)"}}
```

exit 2.

**D8-3 / `show` on the same file — before:** identical shape, stack
bottoms at the same `sniff-path-format`, called from `show-file`.

**— after:**

```
{:status :error, :category :path-unreadable, :payload {:path ".../unreadable.json", :message ".../unreadable.json (Permission denied)"}}
```

exit 2.

(`gate fhir` on the same file is NOT included in the "after" set —
Finding 1, above, names why: still raw today, disclosed, not fixed.)

### The lint's witness pair (AR-CB-4)

Live run of the lint's own predicate (`violating-defn-names`) against
the real `b8fac5a` blob (`git show b8fac5a:bases/cli/src/ehrt/cli/
core.clj`, written to a scratch file, never a regex over the working
tree) and the real post-fix working tree:

```
=== PRE-FIX (git b8fac5a) ===
[check-command gate-command read-base-data show-file sniff-path-format]
=== POST-FIX (live working tree) ===
[]
```

Five names pre-fix (the labeling note above explains why five, not
four), zero post-fix. Confirmed non-vacuous by removing the
`play-events-from-file`/`play-events-from-dir` allowlist entries and
re-running against the post-fix tree:

```
=== POST-FIX, allowlist removed ===
[play-events-from-dir play-events-from-file]
```

The committed test suite carries its own permanent regression proof
independent of git history — `violation-predicate-reproduces-the-
four-charter-sites-test`, cluster A's own minimal-reproduction method
(`project-classpath-test`'s `violation-predicate-reproduces-the-
2088763-incident-test`) — each charter site's structural shape,
pre-fix and post-fix, reduced to its essence and asserted to trip/not
trip.

### Category names chosen, and where they surface

`:base-data-unreadable` (`read-base-data`, D4-5 + D8-3's `mutate` leg),
`:baseline-unreadable` (`gate-command --baseline`, D4-6),
`:assertions-unreadable` (`check-command --assertions`, D4-7),
`:path-unreadable` (`sniff-path-format`/`show-file`, D8-3's `gate`/
`show` legs) — following the file's own local voice (the register's
own cited precedent pair, `:parse-failed`/`:config-unreadable`, is the
model; `:file-not-found`/`:gate-path-not-found`/`:gate-format-
ambiguous` are the file's own sibling names already in place). All four
surface only in the CLI's own EDN/JSON result envelope (`:category`
key) — no new exit code, no `result->exit-code` change, no doc surface
names them by literal string (verified by `make docsgen` producing no
diff).

### Verification

- `clojure -M:poly check`: OK, at Step 0 and after every fix.
- Oracle pre-digest (`bin/regression-oracle b8fac5a b8fac5a`): all
  THIRTY-FOUR roots IDENTICAL (68 digest lines, 34 per side), the
  expected trivial tip-against-itself result.
- Oracle bracket over this session's own in-flight changes: a
  temporary local commit (`e7d46d4`) captured the full dirty working
  tree (`core.clj` + the two new/extended test files), `bin/
  regression-oracle b8fac5a e7d46d4` run, then `git reset --soft
  HEAD~1` undid the commit, restoring the exact pre-commit dirty state
  (staging re-verified via `git status`) — the same no-commit-until-
  Step-4 ordering cluster A's own session used (there via `git stash
  create`; here via a real commit + soft reset, functionally
  equivalent). Result: all THIRTY-FOUR roots IDENTICAL — PURE IDENTITY,
  as predicted (this session touches CLI-shell code and CLI tests only,
  nothing in any digested root's own vendored path).
- `ehrt.cli.core-test`: 256 tests (253 existing + 3 new), 740 assertions,
  0 failures, 0 errors. Red→green proven for the 3 new tests: stashing
  only the `core.clj` fix (keeping the new tests) reproduced an
  uncaught `Exception` at `read_base_data.invoke (core.clj:386)` for
  the malformed-JSON test; restoring the fix returned it to green.
- `ehrt.cli.cli-parse-guard-lint-test`: 3 tests, 18 assertions, 0
  failures, 0 errors.
- Full local suite (`clojure -M:poly test :all skip:integration`): 0
  failures, 0 errors anywhere (grepped the entire log). A genuine
  regression was caught and fixed mid-session here, not just avoided:
  `sniff-path-format`'s signature change (bare value → Result) broke
  every existing caller that compared its return directly to `:v2` or
  mapped it into a `[name fmt]` pair without unwrapping — `play-
  command-*` tests (36 failures, 3 errors) surfaced this immediately;
  `play-events-from-file`/`play-events-from-dir`/`sniff-gate-command`/
  `show-command` were all updated to unwrap the new Result (mechanical,
  required for correctness, not new guarding — see Finding 2 above for
  why their OWN bare reads were left alone).
- Last five `test`-lane runs (`gh run list --limit 5 --branch main`),
  checked at Step 0: all green (`31330881843`, `31330580554`,
  `31328812231`, `31328209204`, `31323443420`).
- `gitleaks git --staged -v`: clean at this commit.
- Tag verification: `stable-20260809-cluster-a-gate-wiring` (this
  session's own Step 0, the successor tag debt ADR-0095 named) tagged
  at `b8fac5a`, pushed, resolves to that commit exactly.
- `git status --porcelain`: clean before this session's first tool
  call.

### Fences

Src changes in `bases/cli/src/ehrt/cli/core.clj` ONLY. No corpus/
kernel/sim/judge/engine src touched (Finding 1's own `gate fhir` leg
lives in `judge-fhir-official`/`kernel` — disclosed, not crossed). No
`result->exit-code`/exit-mapping changes. No D8-4 work — not
encountered in this session's own vicinity. New test files: the
behavioral additions to `core_test.clj` and the new
`cli_parse_guard_lint_test.clj`, nothing else. No workflow or Makefile
edits.

### Index line

```
- 2026-08-09 — cluster-b-parse-guards — ADR-0096
```

(appended to `.agents/plans/roadmap.md`'s own Done section; cluster B
was carried only in horizon notes, never its own `Next` row, so there
is no Next-row removal to pair with this pointer — the same shape
ADR-0095's own index-line note recorded for cluster A.)

`notes/adr/README.md`'s own file count corrects 93→94, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

Untouched, carried forward from ADR-0095, plus this session's own two
new findings: `ehrt gate fhir PATH`'s own permission-denied leg
(Finding 1, above — a `judge-fhir-official`/`kernel.digest` fix, out of
this session's fence); `ehrt play`'s own two unguarded reads (Finding
2, above — allowlisted in the new lint, disclosed, not fixed);
cluster C's ride-along fixes (D8-7, D8-4, D7-7/D7-8); ruling 1's own
unruled option (b); the oracle's own blind-spot intake (H-3); the two
remaining `defspec` flake watch items (D3-2); the ADR-footnote-fork
backlog row (D7-14); `make quickstart`'s own untimed full run (D8-8);
the two deferred veteran modules under their true names; and
publish-prep Externals. What's new: this session's own successor tag
debt (below); cluster B itself is now CLOSED, not carried further.

### This session's own successor tag debt

The next session that opens fresh work tags
`stable-20260809-cluster-b-parse-guards` at THIS session's own closing
tip, under standing ceremony — the tag-law case (ii) pattern.

### Consequence

Four register rows close on one root cause and one precedented fix
shape, proven red-to-green six ways (three malformed-input legs, three
permission-denied legs) with the fix pattern matched exactly to its own
cited precedent (`read-lockfile`/`sim.run`'s config loader). A new,
function-granular static lint closes the recurrence loop the same way
`io_vocabulary_lint_test.clj` already does for a sibling bug class, its
own witness pair proven both as a live session-time run against real
git history and as a permanent, git-history-independent regression test
in the committed suite. Two new findings surfaced honestly rather than
silently fixed or silently buried: `gate fhir`'s own deeper
permission-denied gap (structurally out of this session's fence) and
`ehrt play`'s own identical-shaped, never-charted bare reads (disclosed
via an explicit, named lint allowlist, confirmed non-vacuous). The
oracle holds pure identity across all 34 roots — this is CLI-shell
code and CLI tests only, no sim/engine-path work.

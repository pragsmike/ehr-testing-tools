# 2026-08-09 — Cluster B: CLI parse guards

## Scope

Session prompt naming AR-CB-0 through AR-CB-5, executing the author's
ruling "Cluster B." — ADR-0092's fix cluster B (register rows D4-5,
D4-6, D4-7, D8-3): four CLI reads sharing one root cause (a bare
`slurp`/`edn/read-string`/`json/read-str` with no guard) and one
already-precedented fix shape (`kernel/artifact.clj/read-lockfile`,
`sim/run.clj`'s config loader). Landed the four guards, a co-landed
behavioral test suite extension, and a new function-granular static
lint that closes the recurrence loop the same way
`io_vocabulary_lint_test.clj` already does for the `.listFiles`/
`.renameTo` class.

## Red→green evidence highlights

All six raw failures reproduced live before any fix, matching the
register's own predicted shapes exactly (`EOFException`/
`RuntimeException: EOF while reading`/`FileNotFoundException`, all
uncaught, exit 1). After the fix, all six return a categorized
`result/error` with a distinct category name and exit 2. Full
transcripts, both sides, in `notes/adr/0096-cluster-b-parse-guards.md`.

The new lint's own witness pair, run live against the real `b8fac5a`
git blob and the post-fix working tree:

```
=== PRE-FIX (git b8fac5a) ===
[check-command gate-command read-base-data show-file sniff-path-format]
=== POST-FIX (live working tree) ===
[]
```

Five names pre-fix, not four — a labeling nuance, not a sixth
deviation: D8-3's own "show" row spans two physical functions
(`sniff-path-format`, shared with `gate`; `show-file`'s own second,
content-rendering read), both fixed, both the same charter row. Full
explanation in the ADR.

Full local suite (`clojure -M:poly test :all skip:integration`): 0
failures, 0 errors anywhere. A real regression was caught and fixed
mid-session, not merely avoided: `sniff-path-format`'s signature
change (bare value → Result, required by the fix itself) broke every
existing caller that hadn't been updated — 36 `play-command-*` test
failures and 3 errors surfaced this immediately on the first full-suite
run; every caller (`sniff-gate-command`, `show-command`,
`play-events-from-file`, `play-events-from-dir`) was updated to unwrap
the new Result, restoring 728/728 (later 740/740 with the new tests)
green.

## Judgment calls and their ratification status

- **D8-3's `gate fhir` leg — found unfixable within this session's own
  fence, escalated mid-session via AskUserQuestion, ruled before any
  further fix work.** The register's own root-cause prose (AR-RL-3's
  `.exists()`-only pre-check, citing `core.clj` lines 1097-1103/
  1178-1184) precisely describes the bare `ehrt gate PATH` sniff
  dispatch and `show` — both fixed here — but `ehrt gate fhir PATH`'s
  own crash on a permission-denied file bottoms out three frames past
  `core.clj`, inside `judge-fhir-official`/`kernel.digest`, explicitly
  fenced out ("nothing in corpus/kernel/sim/judge/engine src"). Ruled:
  fix the two in-fence legs, disclose `gate fhir` as a new, unfixed
  finding. Full evidence and finding text in the ADR.
- **`ehrt play`'s own bare reads — a second, later-discovered scope
  question, resolved by extension of the same ruling rather than a
  second AskUserQuestion round-trip.** Discovered while fixing the
  regression above: `play-events-from-file`/`play-events-from-dir`
  carry the identical bare-`slurp` shape this session's own gate exists
  to catch, never named in the charter. Applying the SAME "fix only the
  four charter rows, disclose the rest" principle the prior ruling
  already established: their calls to `sniff-path-format` were updated
  (mechanical, required for correctness), their OWN bare reads were
  left alone, and the new lint allowlists both functions by name with
  inline disclosure — confirmed non-vacuous by removing the allowlist
  and re-running the lint, which then reports exactly those two names.
- **`sniff-files`, a new small helper (not named in the prompt).** The
  prompt's own "minimal caller change" language was written for
  `read-base-data`'s two callers specifically; `sniff-path-format`'s
  own directory-scanning callers (`sniff-gate-command`, `show-command`)
  each mapped it over multiple files inline, which cannot short-circuit
  on a Result without a small reduce-based helper. Mechanical, matches
  the file's own existing "fails fast" convention (stated in
  `mutate-command`'s own docstring), disclosed in the prompt archive's
  deviation record rather than treated as silent scope creep.
- **Oracle bracket technique:** captured via a real temporary local
  commit (`e7d46d4`) + `git reset --soft HEAD~1`, rather than cluster
  A's own `git stash create` — functionally equivalent (both produce a
  commit object `bin/regression-oracle`'s own `git worktree add
  --detach` can check out without disturbing the session's in-flight
  dirty tree), named here since the driving prompt's own "oracle
  bracket vs worktree" wording did not specify a mechanism.

No other calls made; the ruling was otherwise fully specified (fix
shape named exactly, category-naming voice named as session
discretion, lint spec given in full).

## Findings and HEAD landed

Two new findings surfaced and disclosed, neither fixed (both outside
this session's own fence): `ehrt gate fhir PATH`'s own permission-denied
crash (`judge-fhir-official`/`kernel.digest`, a future session's own
register row); `ehrt play`'s own two unguarded reads
(`play-events-from-file`/`play-events-from-dir`, allowlisted by name in
the new lint with disclosure). Oracle bracket (`b8fac5a` vs this
session's own in-flight changes, captured at `e7d46d4` then reset away):
PURE IDENTITY across all 34 roots — this session touches CLI-shell code
and CLI tests only.

Tag `stable-20260809-cluster-a-gate-wiring` (ADR-0095's own successor
tag debt) created at `b8fac5a`, annotated, pushed, verified. Fix commit
`a2c31c8` ("fix: cluster B -- CLI reads guarded, categorized, linted
(ADR-0096, D4-5/6/7 D8-3)") pushed; CI watched to conclusion — `test`
lane run `31340341691`, green, 4m15s, all steps including
`verify-nist-lock (supply-chain integrity)` and `generated-doc
freshness (regen + diff)` passed. This record's own commit follows,
closing the session at its own tip.

This session's own successor tag debt: the next session tags
`stable-20260809-cluster-b-parse-guards` at this session's own closing
tip (ADR-0096's own "This session's own successor tag debt" section).

## Close-out echo

**The six-fold before/after** — full transcripts in
`notes/adr/0096-cluster-b-parse-guards.md`; summary:

| Site | Before | After | Exit |
|---|---|---|---|
| D4-5 malformed JSON, `corpus mutate` | raw `EOFException` | `:base-data-unreadable` | 2 |
| D4-6 malformed EDN, `gate --baseline` | raw `EOF while reading` | `:baseline-unreadable` | 2 |
| D4-7 malformed EDN, `check --assertions` | raw `EOF while reading` | `:assertions-unreadable` | 2 |
| D8-3 chmod-000, `corpus mutate` | raw `FileNotFoundException` | `:base-data-unreadable` | 2 |
| D8-3 chmod-000, bare `gate PATH` | raw `FileNotFoundException` | `:path-unreadable` | 2 |
| D8-3 chmod-000, `show` | raw `FileNotFoundException` | `:path-unreadable` | 2 |

(`gate fhir` on the chmod-000 file is NOT in this table — disclosed as
Finding 1, still raw.)

**Lint witness pair** — five names pre-fix
(`check-command gate-command read-base-data show-file
sniff-path-format`), zero post-fix; allowlist-removed control run
confirms `play-events-from-dir play-events-from-file` would otherwise
trip.

**Category names chosen:** `:base-data-unreadable`,
`:baseline-unreadable`, `:assertions-unreadable`, `:path-unreadable` —
following the file's own local voice, no `result->exit-code` change.

**Permission-denied-test decision:** session evidence only (AR-CB-2),
not a committed test — no existing skip-when-root/non-POSIX guard
convention found anywhere in the test tree.

**Oracle verdict:** IDENTICAL, all 34 roots, both the pre-digest
(`b8fac5a` vs itself) and the in-flight bracket (`b8fac5a` vs `e7d46d4`).

**SHAs:** fix commit `a2c31c8`; this record's own commit follows.

**CI:** `test` lane run `31340341691`, success, 4m15s, all steps green.
Last five runs checked at Step 0 (`31330881843`, `31330580554`,
`31328812231`, `31328209204`, `31323443420`): all green.

# 2026-08-09 — Cluster A: CI/gate wiring

## Scope

Session prompt naming AR-CA-0 through AR-CA-6, executing the author's
ruling "let's do cluster A" — ADR-0092's fix cluster A (register rows
D2-4, D2-18): two "a check exists (or should exist) but doesn't run
where it matters" gaps. Landed a new reader-based docs-tooling gate
that closes the `2088763` classpath-break class structurally, and
wired `bin/verify-nist-lock` into the actual push lane its own header
had falsely claimed to run in for three arcs.

## Red→green evidence highlights

The new gate, `ehrt.docs-tooling.project-classpath-test` (5 tests, 8
assertions): clean at HEAD (`0 failures, 0 errors`); against a
disposable worktree pinned to `2088763~1` (the commit immediately
before the historical fix), trips EXACTLY one violation — judge's own
`pairing_conviction_test.clj` requiring `ehrt.judge-v2-nist.interface`
while `integration`'s own composition lacked it — and nothing else,
confirming the gate encodes the invariant precisely rather than either
under- or over-firing.

`bin/verify-nist-lock` proven both directions before landing: against
an empty scratch `--repo`, exit 2, all six hit-nexus coordinates
reported missing ("not yet resolved"); against the real local `~/.m2`
repo, exit 0, all six coordinates matched.

Full local suite: 295 namespace test blocks, 0 failures, 0 errors
anywhere (grepped the entire log). The new gate confirmed running
twice — once per project composing `docs-tooling` (`development`,
`conformance`).

## Judgment calls and their ratification status

- **`development`'s composition source:** resolved via the root
  `deps.edn`'s own `:dev` alias (no `projects/development/deps.edn`
  exists) — matches `root_alias_completeness_test.clj`'s own existing
  treatment of that project. Disclosed in the prompt archive's own
  deviation record as within AR-CA-1's stated naming/implementation
  discretion, not ratified as a separate ruling (none needed).
- **Oracle bracket technique:** `bin/regression-oracle` only accepts
  git refs; this session's own in-flight changes were captured via
  `git stash create` (dangling commit object, no working-tree or
  stash-list effect) standing in for the bracket's "worktree" side —
  the same technique the immediately preceding session (ADR-0094) used
  and disclosed. Precedented, not a fresh judgment call requiring
  ratification.

No other calls made; the ruling was otherwise fully specified (test
name suggested and used verbatim, wiring point named exactly, header
correction scoped to naming the actual surfaces).

## Findings and HEAD landed

No live violation found at HEAD — only the historical `2088763~1`
witness tripped, exactly as the ruling anticipated (AR-CA-2's own
STOP-AND-REPORT branch was not triggered). Oracle bracket
(`7234f8c` vs this session's own in-flight changes): PURE IDENTITY
across all 34 roots — CI wiring, a docs-tooling gate, and a script
header comment touch no sim/engine-path code.

Tag `stable-20260809-census-closure-file-count` (ADR-0094's own
successor tag debt) created at `7234f8c`, annotated, pushed, peeled
ref verified. Fix commit `d17f9dc` ("fix: cluster A -- classpath
static gate lands, verify-nist-lock joins the push lane (ADR-0095,
D2-18/D2-4)") pushed; CI watched to conclusion — `test` lane run
`31330580554`, green, 4m14s, the new `verify-nist-lock (supply-chain
integrity)` step visible and green in the run log, AR-CA-3's own first
live witness. This record's own commit follows, closing the session at
its own tip.

This session's own successor tag debt: the next session tags
`stable-20260809-cluster-a-gate-wiring` at this session's own closing
tip (ADR-0095's own "This session's own successor tag debt" section).

## Close-out echo

**Witness pair** — trip at `2088763~1`:

```
Testing ehrt.docs-tooling.project-classpath-test

FAIL in (every-project-composed-test-tree-requires-only-documented-bricks-test) (project_classpath_test.clj:154)
test-tree require(s) naming a brick absent from the composing project's own deps.edn/:necessary list -- the `2088763` classpath-break class:
  components/judge/test/ehrt/judge/pairing_conviction_test.clj requires ehrt.judge-v2-nist.interface (brick judge-v2-nist), but project integration does not compose it

Ran 5 tests containing 8 assertions.
1 failures, 0 errors.
```

Clean at HEAD:

```
Testing ehrt.docs-tooling.project-classpath-test

Ran 5 tests containing 8 assertions.
0 failures, 0 errors.
```

**Exit-2 scratch proof:**

```
not yet resolved -- run a full build first (missing from <scratch-dir>):
  nist-hl7-v2-parser (...) nist-hl7-v2-profile (...) nist-hl7-v2-validation (...)
  nist-xml-util (...) nist-hl7-v2-schemas (...) nist-validation-report (...)
```
Exit code: 2.

**Exit-0 real run:**

```
OK: 6 hit-nexus-sourced coordinate(s) match artifacts.lock.edn exactly
  nist-hl7-v2-parser nist-hl7-v2-profile nist-hl7-v2-validation
  nist-xml-util nist-hl7-v2-schemas nist-validation-report
```
Exit code: 0.

**`test.yml` step as landed** (after `poly test :all skip:integration`,
before generated-doc freshness):

```yaml
      - name: verify-nist-lock (supply-chain integrity)
        run: bin/verify-nist-lock
```

First CI conclusion: `test` lane run `31330580554`, green, 4m14s.

**Header line, before:**
```
# Wired into `make test` (the per-push lane) right after
```
**After:**
```
# Wired into two surfaces, both right after
#   - `make test`, the local convenience target (Makefile)
#   - `test.yml`, the per-push lane every push and PR actually runs
```

**Oracle-bracket verdict:** PURE IDENTITY, all 34 roots, both the
pre-digest (`7234f8c` vs itself) and the in-flight bracket (`7234f8c`
vs the session's own `git stash create` object).

**Shas:** tag `stable-20260809-census-closure-file-count` at
`7234f8c`; fix commit `d17f9dc`; this record's own commit follows.

**CI:** `test` lane run `31330580554`, green, 4m14s, watched to
conclusion, all lanes' last-five checked green at Step 0.

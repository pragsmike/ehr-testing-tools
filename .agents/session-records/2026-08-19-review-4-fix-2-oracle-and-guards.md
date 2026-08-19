# Session record — review-4 fix 2/5: the oracle's coverage claim (E) and guard coverage (C)

**Date:** authored 2026-08-19, executed 2026-08-19.
**Mode:** R30 (commit and push at each checkpoint, unattended).
**ADR:** [ADR-0156](../../notes/adr/0156-oracle-coverage-and-guard-coverage.md).
**Prompt:** [2026-08-19-review-4-fix-2-oracle-and-guards.md](../prompts/2026-08-19-review-4-fix-2-oracle-and-guards.md).
**Range:** `1e20c63..` — `079fe80`, `35fc375`, `7ef2e1c`, `6ed54fe`,
`764e8d0`, and this close.

## Step 0

`bin/preflight` — the NEW fail-closed script ADR-0155 landed, so its
exit is load-bearing here for the first time:

```
-- 1. Last five CI runs on main --   five green (1e20c63, 660b7bf, f704c91, bf0b381, 7d998f0)
-- 2. Edit-root confirmation --      OK: not under /mnt/
-- 3. Tree-clean check --            OK: clean, untracked included
-- 4. HEAD-vs-remote tip match --    OK: matches origin/main (1e20c63d…)
-- 5. Last stable-* tag --           stable-20260819-review-4-fix-1-closure-and-harness (660b7bf)
                                     DISCLOSED: HEAD is not currently tagged stable-*
== bin/preflight complete: no findings, exit 0 ==
```

Baseline `make test`, unpiped, `MAKE_EXIT` captured, wrapper ending
`exit "$MAKE_EXIT"`: **MAKE_EXIT=0, 352 blocks / 3,990 tests / 17,876
assertions**, 0 `FAIL/ERROR`. **Reconciles with ADR-0155 exactly.**
`poly check` OK.

Reading sets at Step 0, all under budget, every budget at its baseline:
`:onboarding` 1410/1530, `:corpus` 1807/2045, `:sim` 1253/1405,
`:judge` 901/1000, `:docs` 714/785.

### The six measurements, taken before editing

| | measured | outcome |
|---|---|---|
| (a) `bin/regression-oracle 7d998f0 HEAD` | `IDENTICAL`, 35 rows, `declared-digest-change: no`, **exit 0** | fix 1/5's un-run bracket, closed on the record |
| (b) fresh 35-root pre-digest at HEAD | exit 0, 35 `.edn`, **114 s** | 32 engine + 3 interpreter; **13 of 21** event kinds witnessed, **5** MSH-9 types; `:transfer`/`ADT^A02` = 1, `death-fixture` only |
| (c) `digest_body_of` on the live file | 593 → **524** lines, 0 of 4 requires surviving | reproduces L1-4 — and the arithmetic does not close (see below) |
| (d) the widened awk | 593 → **493** lines (593 − 100 docstring lines), requires in | `7d998f0..HEAD` still IDENTICAL under it (digest.clj unchanged there) |
| (e) `post-push-verify` check 3, un-indexed run | `status= conclusion=<pending>`, every field empty | the fourth shape, reproduced verbatim |
| (f) AUDIENCES segments vs the link rule | segments 1/2/3/4/5/6 → **0**/1/1/3/**0**/6 links | **two** linkless, not the one the prompt expected → STOP-AND-REPORT |

**(c)'s arithmetic, and what it turned up.** The first `^(defn` is at
line 110 of 593, so the body should be 484 lines, not 524. Both awk
rules match every `(defn` line, so each of the 41 is printed — 40 of
them twice. 484 + 40 = 524. Harmless to the diff, wrong in every line
count ever taken from it, L1-4's own included. Gated now by
`the-soundness-body-prints-each-line-once-test`, which was red at `41`
vs `81`.

**(b)'s trap, recorded because the schema names it.** A first pass
grepped `:event ` across the EDN and got 17 kinds. `:pre-horizon-facts`
carry their own `:event` from a different six-value vocabulary, and the
interpreter batches are a third — `event_schema`'s `PreHorizonFact`
docstring calls this *"the single most likely way a proprietary emitter
gets this log wrong."* Re-derived per engine root's top-level vector:
13 witnessed, 8 vacuous, matching L1-2's count by another method.

**Cross-check on the derivation.** The pre-digest's `sha256sum` manifest
is byte-identical to `bin/regression-oracle`'s own baseline manifest in
(a) and to its target manifest in the self-bracket. Same bytes.

**STOP-AND-REPORT at (f), and its ruling.** Reported to the author with
both readings and the finding that segment 1 — not deferred — is
linkless too, so the exemption reading would have left it red anyway.
**Author ruling 2026-08-19: (i), the universal law, no exemption.**

## Checkpoints

| commit | what |
|---|---|
| `079fe80` | **red** — 10 failing assertions across 5 deftests: no committed coverage sets, nothing inside the soundness body, `:require` change invisible, `(defn` lines doubled, `Six roots` against 35 |
| `35fc375` | **green** — COVERAGE block inside the compared region, docstring current-state paragraph, `digest_body_of` widened, `R-oracle-script-contract` made true |
| `7ef2e1c` | docs — ADR-0153 dated addendum, `roadmap.md#oracle-coverage-roots` rowed and priced |
| `6ed54fe` | **red** — 10 failing assertions: segments 1 and 5 linkless, three process laws uncited, no amend row |
| `764e8d0` | **green** — two entry-path links, three skill citations + HISTORY, two rulings rows, `ci-parity` named, rubric sentence, D2-4 paragraph, `post-push-verify` fourth-shape guard |
| this close | ADR-0156, registers, record, prompt archive, `state-derived` |

Every red was captured with its own real output before its fix existed.
The `post-push-verify` fix was isolated with a disposable `git stash` of
`bin/post-push-verify` so the red ran against the unfixed script (2
failing assertions), then popped for green.

## The oracle on itself

| invocation | soundness | verdict | exit |
|---|---|---|---|
| `bin/regression-oracle 1e20c63 HEAD` | `DIFFERS outside the leading docstring -- STOP` | aborted before running | **1** |
| `… --declared-digest-change` | `DIFFERS … asserted, proceeding` | **`IDENTICAL: every root's digest matches`**, 35 rows | **0** |

The gate refusing an undeclared digest-source change *is* the fix
working. Under the declaration all 35 digests are identical — no root,
emitter or digest-logic path moved. Predicted before running.

## Close verification

- **Full `make test` on the final tree:** MAKE_EXIT=0, **358 blocks /
  4,012 tests / 18,008 assertions**, 0 `FAIL/ERROR`. Delta vs Step 0:
  **+6 blocks, +22 tests, +132 assertions.** The +6 reconciles exactly:
  three new `docs-tooling` namespaces (`oracle_coverage_test`,
  `audience_entry_path_test`, `process_law_citation_test`), each run
  from BOTH projects that compose the brick (conformance and ehrt-cli) —
  3 x 2 = 6 blocks, and the log shows six `Testing …` lines for them.
  The +22 tests are those namespaces' 10 deftests x 2 plus the new
  `post-push-verify` deftest in `exit_truthfulness_test` x 2.
  `ehrt.integration.oracle-coverage-test` does NOT appear: it is in the
  lane `skip:integration` excludes, which is where its 114-second digest
  belongs.
- `poly check` OK. `gitleaks git --staged -v` clean before every commit;
  `git diff --cached --stat` read before every commit.
- **CI:** run `32271198594` at the pushed tip
  `841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e`, `status=completed
  conclusion=success` (`gh run view 32271198594`), concluded while this
  session was still open.
- `bin/post-push-verify 1e20c63 841fb75`: three checks recorded below.
- **Reading sets at close:** `:onboarding` 1426/1530, `:corpus`
  1815/2045, `:sim` 1261/1405, `:judge` 909/1000, `:docs` 722/785. All
  under; +8 in every set from `build-session/SKILL.md` (125 → 133),
  `:onboarding` +16 because it also carries `rulings.md` (+2) and
  `roadmap.md` (+6). No compaction needed, no bump taken.

## Deviations

1. **The gate's home moved.** The prompt named
   `components/oracle/test/…`. The oracle brick belongs to no testable
   project (`poly info`: `---` under conformance/ehrt-cli/integration,
   `s--` under dev) and poly's own `help test` says brick tests run from
   every project *"except for the development project"* — a test there
   could never fail. Fix-forward with disclosure, one defensible
   reading (`R-stop-only-on-two-defensible-readings`): per-push half in
   `docs-tooling`, 114-second fresh-digest half in
   `projects/integration`. No `deps.edn` or project composition changed.
2. **Two test extractors refined between red and green** (`^:private`
   defs; quoting `Six roots` as history stays green, restating it stays
   red). Both were still red at `079fe80`.
3. **`make test` went red mid-session** on `state_derived_test` — four
   new test namespaces move its generated counts. Expected under
   ADR-0143's contract; regenerated once at the close rather than three
   times along the way, and the omission was disclosed in the commit
   that made it.
4. **The `#repo-review-4` roadmap row was compacted**, not grown:
   adding fix 2's line made it seven and `roadmap-lint-test` said so.
5. **Pushes batched.** `rulings-lint-test`'s every-cited-ADR-resolves
   check is red on a row citing ADR-0156 until that file exists, so the
   six commits push together after the close.
   `R-red-pushed-with-green` holds (no red pushed alone) and
   `R-full-suite-before-push` holds (full suite on the final tree).
6. **One register correction carried into the fix:** L1-2's summary
   lists `oru-message` among the never-invoked and generalises it to
   "the whole order→result path". ORU^R01 *is* emitted, 1,768 times
   across 14 roots, by `observation-message` and
   `diagnostic-report-message`. The per-function count was right; the
   generalisation was not.

## Fence

Touched: `digest.clj` (docstring + COVERAGE block only),
`bin/regression-oracle`, `bin/post-push-verify`, four docs-tooling test
files, one integration-lane test file, `.agents/rulings.md`,
`docs/dev/AUDIENCES.md`, `build-session/SKILL.md` + `HISTORY.md` +
mirrors, `repo-review/SKILL.md` + mirror, `.github/workflows/test.yml`
(comment only), `notes/adr/0153-*.md` (dated addendum),
`.agents/plans/*` (registers), generated: `notes/ADRs.md`,
`.agents/state-derived.md`, both `INDEX.md`, `docs/dev/pipeline.md`.

Not touched, deliberately: no new oracle root; no `roots`, emitter call
or digest logic; no engine/emitter/check src; no `deps.edn`; no project
composition; no local gate added for the three `.mermaid`;
`docs/what-is-this.md` left alone with the reason stated in the test.

## Post-push verification

```
== bin/post-push-verify (main, range 1e20c63d..841fb75e) ==

-- 1. Remote tip vs HEAD --
OK: origin/main (841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e) matches tip (841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e)

-- 2. Per-commit ASCII check, 1e20c63d..841fb75e --
OK: every commit message in range is pure ASCII

-- 3. CI run at tip (841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e) --
CI run for 841fb75e…: status=in_progress conclusion=<pending> https://github.com/pragsmike/ehr-testing-tools/actions/runs/32271198594
DISCLOSED: reported once, not awaited to conclusion (AR-CI-4)

== bin/post-push-verify complete ==
```

Check 3 rendered `conclusion=<pending>` for a run that genuinely exists
and is genuinely in progress — which is the shape this session's own
fourth-shape fix leaves intact, and correctly so. The defect fixed today
was the *empty-field* rendering of a run that does not exist yet; a real
pending run still reports as pending. Awaited separately below, per
`rulings.md#R-session-verifies-ci-via-gh`.

## Tag

**PAID IN SESSION.** `rulings.md#R-session-verifies-ci-via-gh`: the
licence's CI condition is met by this session's own `gh run view` —
run **32271198594** at `841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e`
concluded **`success`** while the session was open, so
`rulings.md#R-tag-law` makes paying it ceremony rather than a
judgement call (deferring a licensed tag is the deviation).

`bin/tag-ceremony stable-20260819-review-4-fix-2-oracle-and-guards
841fb75 <msg-file> --push`, peeled remote ref verified against
`841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e` exactly:

```
OK: created annotated tag 'stable-20260819-review-4-fix-2-oracle-and-guards' at 841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e
gitleaks: 1022 commits scanned, no leaks found
poly check: OK
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260819-review-4-fix-2-oracle-and-guards -> stable-20260819-review-4-fix-2-oracle-and-guards
OK: pushed refs/tags/stable-20260819-review-4-fix-2-oracle-and-guards
OK: remote peeled ref for 'stable-20260819-review-4-fix-2-oracle-and-guards' is 841fb75e4ba51ab4d5f2e2a1c09ac7e05ff4754e, matches target exactly
```

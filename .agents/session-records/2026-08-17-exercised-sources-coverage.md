# 2026-08-17 -- every exercised-sources row gated by construction: check-all over load-registry, zero-command sources never fresh, cited exercisers must be rows

Ceremony: **R30** (commit and push at each checkpoint, unattended), taken
from the prompt. Narrative of record:
`notes/adr/0148-exercised-sources-coverage.md`. This file is the ceremony
log only.

## Step 0 receipts

`bin/preflight` on `main` at `5c1d73e`, every finding disclosed:

| check | result |
|---|---|
| last five CI runs | **FINDING: one RED** — see below |
| edit root | OK, not under `/mnt/` |
| tree clean (untracked included) | OK |
| HEAD vs `origin/main` | OK, exact match |
| last `stable-*` tag | `stable-20260817-compression-arc` at `9b3432a`; **DISCLOSED: HEAD not tagged** |

**The red run, run down rather than passed over.** `32065822565` at
`77f4fba`, `conclusion: failure`. It is ADR-0147's own, already disclosed
and closed there (§"CI run `32065822565`") and in
`.agents/session-records/2026-08-17-compression-d-state.md` S-7:
`bin/state-migrate-0147` landed mode `100644`, `ehrt.cli.executable-bits-test`
fired, fixed forward with `git update-index --chmod=+x` in the close
commit. The two commits after it — `9b3432a` and `5c1d73e` — are both
`success`. Nothing owed; no live red.

**Tag, per the prompt's standing ruling.** `5c1d73e` is ADR-0147's
addendum, the arc tag sits at `9b3432a`, and `5c1d73e`'s own run
`32069841972` is `success` (verified by this session's own `gh run list`,
not on relay alone). No tag owed at Step 0.

**Baseline `make test`**, unpiped to a log, exit captured explicitly:
`MAKE_EXIT=0`, **342 blocks / 3,890 tests / 17,496 assertions**, zero
failures, 13m49s, `bin/verify-nist-lock` matching six coordinates.
**Reconciles exactly** with ADR-0147's recorded figures. `clojure -M:poly
check` OK.

## Commits

1. `8068e86` — **docs: ADR-0148 opens** — the census: nine rows against
   their live cases, `check-all` unreached, the two trivial-pass probes,
   the (d) population diff.
2. `697c8a5` — **test: red** — `exercised_sources_coverage_test`, 8 tests
   / 26 assertions / **6 failures**.
3. `3dd20ed` — **feat: the register gated by construction** —
   `reject-vacuous`, the hand-case marks, both registers, regenerated
   `state-derived.md`.
4. (this commit) — **docs: ADR-0148 close** — record, prompt archive,
   indexes.

**R-red-pushed-with-green, disclosed:** commit 2 is red and was NOT
pushed alone. It goes to the remote in the same push as commits 3 and 4.

## Red witness

Captured before any src change, real output, nothing filtered:

    Ran 8 tests containing 26 assertions.
    6 failures, 0 errors.

All six in `a-source-yielding-no-taught-commands-is-never-reported-fresh-test`
— two assertions × three routes (no fence of the row's language; a fence
of only comments; a `:paired` source with no genuine pair). Every other
test in the namespace was **green on arrival**, which the ADR-0148 census
had predicted in advance and states as such.

## Green gates

| gate | result |
|---|---|
| `make test` unpiped, `MAKE_EXIT` | **0** |
| blocks / tests / assertions | 344 / 3,906 / 17,548 |
| delta vs baseline | +2 / +16 / +52 — the new namespace, twice (two projects) |
| `clojure -M:poly check` | OK |
| `bin/regression-oracle 5c1d73e 3dd20ed` | **IDENTICAL**, 35/35 roots, that script's own output |
| `make docsgen` drift | none after regeneration |

**DISCLOSED — the first full green run was red, and why that is the
system working.** `MAKE_EXIT=2`, one failure:
`state-derived-md-matches-a-fresh-render-test` (`state_derived_test.clj:30`).
This session added an ADR file, a test namespace, a roadmap row and two
rulings rows — all counted by `.agents/state-derived.md`, which ADR-0147
made generated one session ago. Fixed by `make docsgen` and committed,
never by hand editing the generated page. Recorded rather than smoothed:
it is the first outside confirmation that ADR-0147's freshness gate
catches an ordinary session's ordinary footprint.

## Premise corrections

- **The register has nine rows, not the prompt's ten.**
  `exercised_sources_test.clj:27` pins nine; the roadmap row being closed
  says "one of nine". Reported, not adapted around.
- **The `bash -c` hazard was already caught.** Probed at `5c1d73e` before
  writing: it diverges loudly carrying the wrapper text. Pinned as a
  permanent case; no `:unreadable` classification added, because the gate
  already held (`R-move-not-improve`). The genuinely silent sibling — a
  source yielding zero taught commands — is what the src change fixes.
- **(d) as scoped would have been vacuous.** No reader-facing page cites
  any `bin/usecase-*` script, so the cite-filtered gate over them asserts
  nothing. Widened to the unconditional tree-population closure *plus* the
  cite closure, both with explicit non-empty assertions.

Full reasoning for all three: ADR-0148 findings F-1, F-2, F-3.

**Commit-message wording departs from the prompt** on commits 2 and 3
("zero-command sources never fresh" in place of "unreadable wrappers
named"), for the F-2 reason above. Disclosed in commit 2's own body.

## Registers at close

- Roadmap: `[exercised-row-gate-closure]` → `## Done`, CLOSED 2026-08-17
  ADR-0148. `[strip-fresh-hand-case-retirement]` opened, PRIORITY 16.
- Rulings: `R-register-gated-by-its-own-loader`,
  `R-empty-population-is-red`.
- No `exercised-sources.edn` row edited: (d) forced none.
- No test deleted. The nine hand cases are marked, kept, and registered
  for retirement.

## Reading sets at close

Re-measured from the regenerated `.agents/state-derived.md`, all five at
or under baseline, none bumped:

| set | actual | budget | baseline | headroom |
|---|---|---|---|---|
| `:corpus` | 1799 | 2045 | 2045 | 246 |
| `:docs` | 706 | 785 | 785 | 79 |
| `:judge` | 893 | 1000 | 1000 | 107 |
| `:onboarding` | 1371 | 1530 | 1530 | 159 |
| `:sim` | 1245 | 1405 | 1405 | 160 |

`:onboarding` moved 1360 → 1371 (+11): this session's two rulings rows
and its net roadmap row. Inside budget; no compaction owed, no bump
available.

## Tag

Per the prompt's standing ruling: this session's close tag is paid
in-session if its tip run concludes `success` while the session is open,
else it falls to the next Step 0. The tip run is only observable after
this commit is pushed, so which branch was taken is recorded in a dated
addendum to `notes/adr/0148-exercised-sources-coverage.md` — the same
shape ADR-0147 used for the same reason, rather than a claim written
before its evidence exists.

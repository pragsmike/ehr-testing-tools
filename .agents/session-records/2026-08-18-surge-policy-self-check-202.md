# 2026-08-18 -- surge-policy self-check failure at seed 202 under --churn: repro test, diagnosis, fix

**Ceremony mode:** R30 (commit and push at each checkpoint,
unattended), taken from the prompt.
**ADR:** `notes/ADRs.md` ADR-0153 -- the reasoning of record; this file
is the ceremony log only (`rulings.md#R-session-narrative-hierarchy`).
**Row:** `roadmap.md#surge-policy-self-check-202`, CLOSED here.

## Step 0

`bin/preflight` (main), every finding disclosed:

- last five CI runs on `main` all green; `c1a40d0` green at
  `2026-08-18T20:28:03Z`
- edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`
- working tree clean, untracked included
- local HEAD `c1a40d073103d151f01a0304b4b33f3744886846` == `origin/main`
- last stable tag `stable-20260818-sim-theory-edn-hop` @ `c509e462`;
  **DISCLOSED: HEAD not tagged stable-*** -- no tag owed at Step 0, per
  the prompt

Baseline `make test`, unpiped, full log, `MAKE_EXIT=0`:
**348** zero-failure blocks / **3,956** tests / **17,730** assertions.
Reconciles exactly against ADR-0152's 348 / 3,956 / 17,730.

`clojure -M:poly check`: **OK**.

Reading sets, from the generated `.agents/state-derived.md` (never
prose): `:corpus` 1801/2045, `:docs` 708/785, `:judge` 895/1000,
`:onboarding` 1392/1530, `:sim` 1247/1405 -- all green, all at their
ratchet baselines.

`out/` backed up and cleared before the demo-corpus runs.

## Checkpoints

**C1 -- red, `ceedcfd`.** Two tests, five failures, real output in the
commit message and in ADR-0153 Step 2. `git diff --cached --stat` read
(2 files, +91); `gitleaks git --staged -v` clean; message from a file.
Not pushed alone (`rulings.md#R-red-pushed-with-green`).

**C2 -- green, `885b1c9`.** One src file
(`components/sim-engine/src/ehrt/sim_engine/engine.clj`, +48/-11),
plus nothing else. `git diff --cached --stat` read; gitleaks clean;
message from a file.

**DISCLOSED: C2 was amended once.** The commit was created as
`6e100da`, then `git commit --amend -F` carried the verified
`bin/regression-oracle` result into its message once the oracle
finished. The tree is byte-identical across the amend, the commit was
never pushed, and the one artifact that records a commit id --
`manifest.edn`'s `:generator :sha256` in the demo-corpus comparison --
is attributed to `6e100da` explicitly in ADR-0153 rather than to
"HEAD". Recorded rather than quietly re-run: writing an unverified
oracle claim into a commit message and verifying afterwards was the
alternative, and it is the worse one.

**C3 -- close.** ADR-0153, roadmap (row closed under `## Done`; new row
`#bed-ready-vacancy-cascade`), census S-5 closed and dated, prompt
archived, this record, `make adr-index state-derived`.

## Verification

- **Regression oracle:** `bin/regression-oracle c1a40d0 HEAD`, exit 0,
  its own output: `IDENTICAL: every root's digest matches between
  c1a40d0 and HEAD`, `--- declared-digest-change: no (soundness: yes
  outside ns form) ---`, all 35 roots. Not a test-count comparison
  (`rulings.md#R-oracle-script-contract`).
- **`make traces`:** `TRACES_EXIT=0`, `demos/traces/**` byte-identical
  (`git status` showed only `engine.clj` modified afterwards).
- **The census's `--churn` corpus shapes**, digested at both refs via a
  throwaway worktree at `c1a40d0`: six unmoved byte-for-byte, the
  seventh (`edchurn-202`) fixed, and the ed-tuesday demo's 285-file
  tree identical but for `manifest.edn`'s commit-id provenance stamp.
  Full table in ADR-0153.
- **Full `make test` before push**, unpiped, `MAKE_EXIT` recorded --
  see below (`rulings.md#R-full-suite-before-push`).
- **`bin/post-push-verify`** after the push -- see below.

## Post-fix full suite

`make test`, unpiped, full log, `MAKE_EXIT=0`: **348** zero-failure
blocks / **3,960** tests / **17,758** assertions.

Reconciled against Step 0 (348 / 3,956 / 17,730):

- **blocks 348 -> 348**, and a per-namespace `diff` of the two runs'
  `Testing <ns>` lines is EMPTY -- no namespace added, removed, or
  renamed.
- **tests +4** for the two `deftest`s added. Two, not four, because
  `ehrt.sim-engine.engine-test` and `ehrt.sim.run-test` each run in two
  project lanes.
- **assertions +28** = 14 `is` forms (12 in the engine repro, 2 in the
  run-level test) x the same two lanes.

Reading sets re-measured at close from the regenerated
`.agents/state-derived.md` (`rulings.md#R-register-hygiene-at-close`):
`:corpus` 1801/2045, `:docs` 708/785, `:judge` 895/1000, `:onboarding`
**1398**/1530, `:sim` 1247/1405. Only `:onboarding` moves, +6, and only
because `roadmap.md` is one of its paths and this session's row work is
net +6 lines there. 132 lines of headroom; no budget touched, so
`rulings.md#R-budget-stop` never comes into play.

## Push and CI

One push, `c1a40d0..5563f71`, carrying all three commits -- the red
commit went out together with its green successor, never alone
(`rulings.md#R-red-pushed-with-green`).

`bin/post-push-verify c1a40d0 5563f71`, its three checks:

1. remote tip `5563f71f7a43780ab58c1d8ed9193bd6ceb41a28` matches HEAD;
2. every commit message in the range is pure ASCII;
3. CI run reported once, `status=queued` at the time, disclosed as not
   awaited to conclusion (AR-CI-4).

Awaited separately, per `rulings.md#R-session-verifies-ci-via-gh`:
`gh run view 32195652221` -> **`status=completed conclusion=success`**
at `sha=5563f71f7a43780ab58c1d8ed9193bd6ceb41a28`.
https://github.com/pragsmike/ehr-testing-tools/actions/runs/32195652221

## Tag

No tag owed at Step 0 (preflight-confirmed). This session's own close
tag is licensed by the prompt to be paid in-session if this session's
tip CI run concludes success while the session is still open, else at
the next Step 0.

**PAID IN-SESSION.** Run 32195652221 concluded `success` at `5563f71`
while this session was open, so the licence's condition was met and
deferring would have been the deviation (`rulings.md#R-tag-law`).

`bin/tag-ceremony stable-20260818-surge-policy-self-check-202
5563f71f7a43780ab58c1d8ed9193bd6ceb41a28 <msg-file> --push`, its own
output:

    OK: created annotated tag 'stable-20260818-surge-policy-self-check-202'
        at 5563f71f7a43780ab58c1d8ed9193bd6ceb41a28
    OK: pushed refs/tags/stable-20260818-surge-policy-self-check-202
    OK: remote peeled ref for 'stable-20260818-surge-policy-self-check-202'
        is 5563f71f7a43780ab58c1d8ed9193bd6ceb41a28, matches target exactly

Message from a file, never inline. No `v*` tag, no repo-level `gh`
mutation, no git surgery -- those stay AUTHOR ACTION under either
ceremony mode.

## Scratch hygiene

Instrumentation lived in three throwaway `development/src/dev/scratch*`
namespaces (gitignored by that exact pattern) and was deleted before the
pre-push suite ran; the disposable `c1a40d0` worktree used for the
demo-corpus baseline was removed and `git worktree prune` run. `out/`
was backed up before being cleared for the runs and restored to its
Step 0 contents at close -- it is gitignored and reproducible, but it
was not this session's to destroy.

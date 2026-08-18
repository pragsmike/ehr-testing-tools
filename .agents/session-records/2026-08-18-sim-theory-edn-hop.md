# 2026-08-18 -- sim-theory head hop registered: sim-theory-equations.txt GENERATED from sim-theory.edn

Reasoning of record: [`notes/adr/0152-sim-theory-head-hop.md`](../../notes/adr/0152-sim-theory-head-hop.md).
Driving prompt: [`.agents/prompts/2026-08-18-sim-theory-edn-hop.md`](../prompts/2026-08-18-sim-theory-edn-hop.md).

Ceremony: R30 (commit and push at each checkpoint, unattended).
Author ruling opening the session: *"(a) edn single-source-of-truth,
keep equations. Rider."* Mid-session ruling, after the Step 0 STOP:
**A1+A2(a) -- widen the fence by two**, rider lands regardless.

## Step 0

`bin/preflight`, every finding disclosed:

1. Last five CI runs on `main` all green.
2. Edit root `/home/mg/src/ehr-testing-tools` not under `/mnt/`.
3. Working tree clean, untracked included.
4. HEAD `bde5f377` matches `origin/main`.
5. Last `stable-*` tag `stable-20260818-reason-nil-drop` @`8d4fac2`;
   DISCLOSED HEAD not tagged. No tag owed, matching the prompt.

Baseline `make test` unpiped, `MAKE_EXIT=0`:

    346 blocks / 3,928 tests / 17,648 assertions, 0 failures, 0 errors

reconciling **exactly** with ADR-0151. `clojure -M:poly check` OK.

Reading sets, all under budget:

| set | actual | budget |
|---|---|---|
| `:corpus` | 1801 | 2045 |
| `:docs` | 708 | 785 |
| `:judge` | 895 | 1000 |
| `:onboarding` | 1396 | 1530 |
| `:sim` | 1247 | 1405 |

`git grep sim-theory-equations -- ':!Makefile'` confirmed the prompt's
census with **one correction**: `.agents/state.md:73` is a live hazard
register, not history. See "Deviations" below.

## The STOP, and what it found

Prediction (a) was mandatory before any edit, with a STOP if any
equation line differed in content. It did. The translator's output
matched 10 of 13 lines byte for byte and dropped three annotations,
because `sim-theory.edn` **did not validate against the `Pipeline`
Malli** -- its two external stages sat inside `:stages` without
`:kind`/`:status`. Rendered, that would have cost both black boxes their
dashed border. Calibrate's `{feedback:}` had no schema key at all.

Both remedies were **simulated in `target/` and proven byte-exact before
the author was asked**, so the ruling was made against evidence:

    diff hand-equations vs A1+A2(a):  IDENTICAL -- all 13 lines
    diff .mermaid, arrows normalized: EMPTY

## Checkpoints

| # | commit | what |
|---|---|---|
| C1 | `b023332` | red -- 18 failures across five claims |
| C2 | `c311db1` | green -- translator, `.edn` repairs, Makefile, CI list, regeneration |
| C3 | `f3d8098` | rider -- `:execute`'s two ADR-0141 laws |
| C4 | `1e261f5` | `.agents/state-derived.md` regenerated |
| C5 | *this commit* | ADR-0152, registers, close |

Red run, real output, false positives included:

    Ran 44 tests containing 87 assertions.
    18 failures, 0 errors.

`docsgen-depends-on-the-sim-theory-target-test` deliberately **not**
among them -- already true, the stated right-population proof.

## Predictions vs actuals

| # | predicted | actual |
|---|---|---|
| (a) | all 13 equation lines byte-unchanged in TEXT | **exact** |
| (b) | arrows 19/21/23/25/27/29/31/33/36/39/42/45/46 -> 5/6/7/8/9/10/11/12/14/16/17/18/19 | **exact**, row for row |
| (c) | freshness population 18 -> 19 | 18 -> 19 |
| (d) | 50 ms -> ~3,110 ms | 50 ms -> **2,985 ms** |

## Verification

- **Oracle:** `bin/regression-oracle bde5f37 HEAD` ->
  `IDENTICAL: every root's digest matches`, its own output, 35 roots.
- `make pipeline` re-run: `pipeline.edn` and `docs/dev/pipeline.md`
  byte-identical.
- `make sim-theory` twice: all three artifacts md5-identical.
- `make docsgen` in full: nothing else moved.
- `gitleaks git --staged -v` before every commit: no leaks found.
- Every commit message written to a file and committed with `-F`.
- `bin/post-push-verify bde5f37 1e261f5`: remote tip matches, every
  commit message in range pure ASCII, CI run
  [32176462377](https://github.com/pragsmike/ehr-testing-tools/actions/runs/32176462377)
  reported once per AR-CI-4.

Full `make test` at the pushed tip, `MAKE_EXIT=0`:

    348 blocks / 3,956 tests / 17,730 assertions, 0 failures, 0 errors

Reconciled against Step 0's 346 / 3,928 / 17,648: **+2 blocks, +28
tests, +82 assertions**, all of them this session's -- 14 new `deftest`s
(8 in `sim_theory_head_hop_test`, 6 appended to `pipeline_test`), each
counted twice because the namespace runs under both the brick suite and
the project suite. No other namespace moved.

## Deviations, disclosed

1. **The red commit carries `pipeline.clj`'s renderer half.** Without it
   the test namespace does not compile and the red degrades to a
   `FileNotFoundException`. The renderer is inert until C2 wires it, and
   `write-equations-txt!` is untouched, so `pipeline.edn`'s output is
   byte-identical by construction -- asserted by a test that is green in
   the red run.
2. **The pre-push suite went RED at `MAKE_EXIT=2`**, on exactly one
   failure: adding a test namespace moves `.agents/state-derived.md`
   (186 -> 187 test namespaces, 43 -> 44 docs-tooling gates). Fixed by
   regeneration in C4 and recorded as a generalisation nobody had
   written down. The harness reported that same background run as
   "exit code 0" because the wrapper ended in `echo | tee`; the captured
   `MAKE_EXIT` is the only reason the red was seen.
3. **A prompt premise did not survive the tree.** The prompt's link
   census called `.agents/**` references "history". `.agents/state.md:73`
   is the live hazard register and still asserted the hazard this
   session eliminated. Fix-forward with disclosure rather than STOP
   (`rulings.md#R-stop-only-on-two-defensible-readings` -- one reading
   was clearly weaker), and rewritten as the CLASS rather than deleted.
4. **Prompt correction:** the rider's predicate is
   `valid-ground-truth?`, not `valid-log?`. No such var exists. Probed
   before writing.
5. **Fence widened by two, on the author's ruling**, both named at the
   time of the ask: `sim-theory.edn`'s `:stages` -> `:external-stages`
   relocation, and `pipeline.clj` beyond a header option.
6. **The close suite went RED a second time, at `MAKE_EXIT=2`**, on
   `state-md-stays-within-its-line-cap-test`: the state.md rewrite in
   deviation 3 took the file to 124 lines against a 120-line cap. The
   gate's own message names raising the cap as "the move this arc exists
   to make unavailable", so the bullet was compacted to five lines
   (119 total) rather than the cap touched. `:onboarding` therefore
   reads **1392** at the close, not the 1397 measured before the
   compaction: state.md's bullet nets +1 line over the original four,
   and roadmap.md -5, for -4 against Step 0's 1396.

## Register hygiene

- `roadmap.md#sim-theory-edn-hop` -> `CLOSED 2026-08-18 ADR-0152` under
  `## Done`, by anchored insertion. Remaining `## Next` priorities stay
  unique and ascending.
- ADR-0139's C-1 gains a dated addendum line, not an edit -- recording
  that the finding was **understated**, the pair having already drifted.
- `.agents/state.md`'s hazard bullet rewritten as the class.
- All five reading sets re-measured at the close, all under budget.
  `:onboarding` 1396 -> **1392** (state.md +1, roadmap.md -5); the
  other four unchanged, no path of theirs touched.

## Tag

Per the author's ruling -- *"pay in-session if its tip run concludes
success while open, else next Step 0"* -- see the close commit's own
disclosure for which arm fired.

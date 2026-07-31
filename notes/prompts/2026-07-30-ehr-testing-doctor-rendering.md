# 2026-07-30 — doctor renders its checklist: tailored pretty output, checklist-not-rejection wording, hint fallback

Repo: `github.com/pragsmike/ehr-testing-tools`, main at `a4f945a` at session start (probed first, clean, in sync with `origin/main`). Autonomous session per R30: commit AND push at each checkpoint; hooks gate; tags are the author's alone. No mid-session questions asked — the prompt's own decision procedures covered every branch actually hit.

## Context

Two author transcripts (Git Bash/Windows, 2026-07-30) were this session's evidence. First: `bin/ehrt doctor` printed `rejected (doctor-checks-failed)` plus the `--edn` pointer — nothing else. Second: `--edn` revealed a payload of four checks, three passing, and a platform check failing with a complete, precise, remedy-bearing message (`"Windows 11 -- native Windows is not supported; use WSL2 (SETUP.md section 2)"`). The defect was pure rendering: doctor — the verb whose entire job is naming what's wrong — was the one command whose pretty layer hid its findings. This session's own scratch capture reproduced the defect exactly before touching any code (see Deviations for the verbatim before/after).

No new ADR: ADR-0013 already sanctions payload-shape-tailored pretty rendering (`render-pretty` dispatches on payload shape; the gate/check case got per-file verdict lines under exactly this sanction). Doctor was the missing tailored case, added here citing ADR-0013 rather than amending it.

## Rulings (as executed)

* Doctor gets a tailored pretty rendering: one line per check — pass/fail marker, check name, and the `:detail` — with every check's detail always shown in full (failing checks' detail already carries the remedy; passing checks' detail is already short, so no differential truncation logic was needed either way). A final line states the overall outcome and, when any check failed, the failing count.
* The human wording is a checklist report, never `"rejected"`: doctor succeeded at diagnosing; the checks failed. The machine contract — category `:doctor-checks-failed`, envelope shape, and the exit contract (0 all passed / 1 any failed / 2 lockfile unreadable) — stayed byte-for-byte; only the pretty rendering changed.
* `:doctor-checks-failed` and the exit-2 lockfile-unreadable category (`:not-found`/`:invalid-lockfile`/`:parse-failed`, as surfaced through doctor specifically) both carry a `:hint` regardless of the tailored renderer — the generic-fallback rule from the `:not-cached` family (cold-start UX session): no category of doctor output is ever a dead end, even rendered by the generic path.
* Pass/fail markers follow the existing pretty-output vocabulary: gate/check's own `pretty-verdict-line` already renders verdicts as a plain lowercase word via `(name verdict)` — no symbol, no unicode. Doctor's per-check line does the same via `(clojure.core/name status)` (`"pass"` / `"fail"`), consistent with that precedent rather than inventing a new marker vocabulary.

## Steps (as executed)

1. **Probe and baseline.** Fresh state, clean, matching `origin/main` at `a4f945a`. `clojure -M:poly check` green. Doctor's current pretty output was captured directly via a scratch REPL call (`doctor-command` + `render-pretty`, both fake-injected) for all three outcomes — the exact defect from the transcripts reproduced: the all-pass case rendered `"ok\n(--edn or --json for the full result)"`, and the failing case rendered `"rejected (doctor-checks-failed)\n(--edn or --json for the full result)"`, nothing else. No commit.
2. **Renderer.** Added `doctor-checks-payload` (shape predicate on the `:checks` key), `pretty-doctor-check-line`, and `pretty-doctor-summary` to `bases/cli/src/ehrt/cli/core.clj`, and extended `render-pretty`'s `cond` with a `doctor-checks-payload` branch alongside the existing `report-payload` one. A workspace-wide grep confirmed `:checks` is used nowhere else in this codebase, so shape-keyed dispatch (consistent with the gate/check precedent) was sufficient — no departure to category-keying was needed (see Deviations). Tests added to `bases/cli/test/ehrt/cli/core_test.clj`: all-pass rendering, mixed pass/fail rendering (asserting `"rejected"` never appears in the pretty text), the exit-2 category correctly falling through to the generic summary (no `:checks` key), and two pinned exact-envelope tests (`doctor-command-all-pass-envelope-pinned-against-before-state-test`, `doctor-command-checks-failed-envelope-pinned-against-before-state-test`, `doctor-command-lockfile-unreadable-envelope-pinned-against-before-state-test`) proving the raw EDN envelope was untouched by this step. Gate: `poly check` OK, `make test` (`poly check` + `poly test :all skip:integration`) green — 2683 tests, 7341 assertions, 0 failures/errors. Commit `65b550b`, pushed.
3. **Hints.** `doctor-command` now wraps its own exit-2 branch with `(update artifacts-result :payload assoc :hint ...)`, naming the lockfile path (read off the error payload's own `:path`, not recomputed) and pointing at SETUP.md section 1; and attaches `:hint` to the `:doctor-checks-failed` rejection, naming the failing count, the failing checks' names, and `ehrt doctor --edn` (belt-and-suspenders, per the ruling — the tailored pretty view already shows full detail per check without needing this hint, but the payload carries it anyway as the generic-fallback safety net). Placement: at this CLI-boundary construction site inside `doctor-command`, not inside `kernel/artifact`'s shared `read-lockfile` — that function's other callers (`fetch`, `generate`, `version`, `gate v2-nist`) read the same lockfile and were left untouched, out of this session's scope. Tests: hint-presence assertions added to the two existing doctor tests that already exercise these paths (`doctor-command-any-failing-check-is-rejected-not-error-test`, `doctor-command-cannot-even-check-is-error-not-rejected-test`); the three pinned envelope tests from step 2 updated in place to include the new `:hint` key (not deleted — the pin's referent changed by ruling, same as this workspace's compat-test precedent). Pinned-envelope sweep: grepped the rest of the CLI test suite for any test asserting full equality against a doctor payload or against `:not-found`/`:invalid-lockfile`/`:parse-failed` from a non-doctor call site — none found; every other doctor test and every other lockfile-error test asserts individual keys, not the whole map, so none broke. Gate: `poly check` OK, `make test` green — 2683 tests, 7343 assertions (the two new hint-presence assertions), 0 failures/errors. Commit `ed3d0e4`, pushed.
4. **Gate and close.** Final `clojure -M:poly check` green; `make cli-doc` re-run standalone, zero diff (doctor's help text and exit-code contract were never touched, so `docs/cli.md` regenerates identically); `git status --porcelain` clean. This file.

## Deviation record

**No departure from shape-keyed dispatch was needed.** The prompt's own decision procedure named a fallback ("`render-pretty`'s dispatch cannot distinguish doctor's shape from another `:checks`-carrying payload: key on category instead for this case") for a conflict that turned out not to exist: a grep of `:checks` across every `.clj` file in the workspace found exactly two matches, both in `core.clj`/`core_test.clj` — doctor's own construction and its own tests. Shape-keyed dispatch stays fully consistent with `report-payload`'s own precedent; the category-keying fallback was never invoked. (One further wrinkle the fallback anticipated — an all-pass `result/ok` carries no `:category` at all, since `result/ok` never sets one — would have made category-keying impossible to apply uniformly across both the ok and rejected outcomes anyway; shape-keying sidesteps that entirely.)

**The checks vector's shape matched the transcript's exactly.** `:name`/`:status`/`:detail`, `:status` values `:pass`/`:fail` only (binary, no `:warn`) — read directly from `doctor-command`'s own construction site before writing the renderer, not assumed.

**Marker vocabulary: plain lowercase word, not a symbol.** `pretty-verdict-line` (gate/check's own tailored renderer) already renders verdicts via bare `(name verdict)` — "pass", "rejected", etc. — with no checkmark, no unicode, no bracketed tag. Doctor's per-check line follows the same convention via `(clojure.core/name status)`, satisfying the ruling's own preference order ("follow whatever marker vocabulary existing pretty output uses if one exists") without needing the plain-`ok`/`FAIL` fallback the ruling also named.

**Hint placement: CLI boundary (`doctor-command`), not the shared kernel `read-lockfile`.** The decision procedure offered both options ("command-agnostic wording at the shared site, or CLI-boundary attachment"); CLI-boundary was chosen because the ruling itself scoped this specifically to doctor's own exit-2 category, and `read-lockfile` is shared by several other CLI commands (`fetch`, `fetch --all`, every `generate` lane, `version`, `gate v2-nist`) whose own lockfile-read failures were not in scope for this session and were left exactly as they were — no hint added there. This is the narrower-blast-radius choice, and it matches the cold-start session's own kernel-vs-CLI reasoning in spirit (attach where the advice is unambiguously correct for every caller of *this* site) without extending a hint into call sites this session never audited.

**Pinned-envelope sweep: no other test broken.** Every doctor test besides the three envelope-equality tests this session added asserts individual keys (`:status`, `:category`, specific `:checks` entries), never the whole payload map — the `:hint` addition is additive to all of them, confirmed by running the full suite green both before and after step 3's change, not just by inspection.

**No frozen-file edits, no renumbering, no destructive git operations, no forced pushes, no new ADR.** `notes/tools/ADRs.md` was not touched; `notes/ADRs.md` itself was not touched either — this session's change was scoped entirely to `bases/cli/src/ehrt/cli/core.clj` and its own test file, cited against ADR-0013's existing sanction rather than amending it. Every commit landed fresh, never amended.

**Unexpected `git status` dirt: none, at any checkpoint.** Every `git status --porcelain` check across all four steps showed exactly the files that step's own ruling named, nothing else.

### Before/after, verbatim (scratch capture, step 1 vs. after step 3)

All-pass, before:
```
ok
(--edn or --json for the full result)
```

All-pass, after (step 2 renderer; step 3 adds no hint to the ok case):
```
pass  java resolution (via the artifact registry) -- resolved: /fake/java
pass  artifact cache (per lockfile entry) -- 1 artifact(s) cached
pass  git hooksPath wiring (contribution sessions only) -- core.hooksPath = .githooks
pass  platform -- Linux

all checks passed
```

Checks-failed, before:
```
rejected (doctor-checks-failed)
(--edn or --json for the full result)
```

Checks-failed, after (step 3's payload `:hint` is a generic-fallback safety net, not shown in this tailored view since the per-check detail already is the remedy):
```
pass  java resolution (via the artifact registry) -- resolved: /fake/java
pass  artifact cache (per lockfile entry) -- 1 artifact(s) cached
pass  git hooksPath wiring (contribution sessions only) -- core.hooksPath = .githooks
fail  platform -- Windows 11 -- native Windows is not supported; use WSL2 (SETUP.md section 2)

1 of 4 check(s) failed
```

Lockfile-unreadable (exit 2), before and after (unchanged — this category has no `:checks` key, so it was and remains generic-summary rendered; step 3 adds the payload's `:hint` line, shown here):
```
error (not-found)
path=/no/such/lockfile.edn
couldn't read the lockfile at /no/such/lockfile.edn -- see SETUP.md section 1
(--edn or --json for the full result)
```

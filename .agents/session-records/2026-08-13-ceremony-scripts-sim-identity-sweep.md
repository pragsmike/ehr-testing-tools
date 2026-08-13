# 2026-08-13 — Ceremony scripts, build-session skill absorption, sim-identity citation sweep (ADR-0127)

## Scope

Two threads chartered together per the driving prompt's own session
pairing: the ceremony-scripts row (R13, `.agents/rulings.md` "From
ADR-0122") and the sim-identity citation sweep ADR-0126 disclosed but
did not fix. Four commits landed; this record is the fourth's own
close-phase companion.

## Ceremony

`gh run list --limit 5 --branch main` at session start: all five
green (`04ad5af`, `a4203fa`, `0266bc4`, `c6d0257`, and one earlier).
HEAD confirmed `04ad5af`, tree clean.

Tag `stable-20260813-citation-sweep` created ANNOTATED at `04ad5af`
via `bin/tag-ceremony ... --push`; pushed; peeled ref confirmed
`04ad5affd3f56901bb84972d03af7c7b12538697` exactly. **Self-caught
miss, disclosed:** this payment was originally supposed to happen at
Step 0, before Step 1's own work began — it was instead missed and
only caught while drafting this record, during the transcript
re-check the driving prompt's own closing line asks for
("Double-check your own tag ceremony in your transcript before
closing"). Paid at that point, after `bin/tag-ceremony` existed to do
it (Step 2 landed first). Full disclosure in `notes/adr/0127-*.md`'s
own Step 0 section.

## Commit 1 (`c214bfb`) — sim-identity citation sweep

Full inventory re-derived rather than trusting the channel's own
17-site census (which the driving prompt itself flagged as an
undercount): a grep across the ENTIRE `components/sim/docs/` and
`components/sim-trajectory/docs/` trees (not the six named files
alone — the prompt's own 1a instruction: "plus any sibling file in the
same two docs/ trees the grep surfaces") found 238 raw `ADR-NNNN`
hits across 10 files. Classified by content-topic match against all
three ADR registers (this workspace's live `notes/ADRs.md`, the frozen
`notes/sim/ADRs.md`, the frozen `notes/tools/ADRs.md`):

- **106 sim-era sites** (numbers `ADR-0001`–`ADR-0013`) — every
  citation's own surrounding text matches `notes/sim/ADRs.md`'s own
  title for that number (event-sourcing/`decide`-`evolve` →
  sim/ADR-0008; patient identity → sim/ADR-0010; the time model →
  sim/ADR-0011; GMF module vendoring → sim/ADR-0013; etc.), never this
  workspace's or `tools`'s own same-numbered, topically-unrelated
  record. Origin-qualified to `sim/ADR-NNNN`.
- **132 workspace-current sites** (numbers `ADR-0026` and above: GMF
  coverage Waves A–I2, the player-fold arc, the vendoring/injuries
  arcs) — spot-checked (every distinct number's first occurrence read
  in full context, not assumed from the range alone), matched this
  workspace's own live register exactly, correctly left bare.

Zero ambiguous hits; zero blanket seds (scoped substitution, numbers
1–13 only, verified before and after against a full context dump).
Full per-file/per-number table in `notes/adr/0127-ceremony-scripts-
sim-identity-sweep.md`.

Eight markdown-link citations also had their hrefs fixed: `../notes/
ADRs.md[#adr-NNNN]` was independently broken (one directory level too
shallow from a three-deep `docs/` directory) AND pointed at the wrong,
workspace-current register. Fixed to `../../../notes/sim/ADRs.md`,
anchor stripped, matching `docs/glossary.md`'s own established
`[sim/ADR-NNNN](../notes/sim/ADRs.md)` convention.

`sim-theory.edn`'s `:contract` strings confirmed prose-consumed only
before editing (every `components/sim*/test`/`src` hit is a docstring
citation, never a slurp/hash; `bin/check-palgebra-drift`'s own scope
excludes this file; `stale_path_test.clj`'s own documented scope
excludes both docs/ trees entirely) — edited safely, re-verified as
valid EDN after.

`clojure -M:poly check`: OK. `clojure -M:poly test :all
skip:integration`: 535 passes, 0 failures, 0 errors. `bin/
verify-nist-lock`: OK. Pushed; `bin/post-push-verify 04ad5af HEAD`
(smoke-testing the not-yet-committed script directly against this
push): remote tip matched, ASCII clean, CI reported in-progress.
`bin/regression-oracle 04ad5af c214bfb`: **IDENTICAL**, all 35 roots.

## Commit 2 (`227ffaf`) — four ceremony scripts

`bin/preflight`, `bin/tag-ceremony`, `bin/post-push-verify`, `bin/
close-scaffold`. `tag_law_test.clj` and `index_completeness_test.clj`
read whole first; each script's own checks encode the SAME convention
those tests gate (e.g. `close-scaffold`'s index line matches
`index-completeness-test`'s own `star-bullet-token` regex exactly).
No census/count lock on `bin/`'s own contents exists in the live tree
(checked before writing) — no companion test lands with this commit.

Smoke-tested with real invocations, pasted here as actually run:

```
$ bin/preflight
== bin/preflight (main) ==

-- 1. Last five CI runs on main --
  PENDING  c214bfb7  2026-08-13T18:00:47Z  docs: sim-identity citation sweep -- origin-qualify sim-era ADR citat...
  green  04ad5aff  2026-08-13T17:32:11Z  docs: session record and prompt archive -- citation sweep and glossar...
  green  a4203fad  2026-08-13T17:23:20Z  docs: citation errata sweep -- origin-qualify verdict-family ADR-0010...
  green  0266bc48  2026-08-13T17:22:38Z  docs: glossary linkage across manual chapters 1, 3-7 (ADR-0126)
  green  c6d02571  2026-08-13T15:51:44Z  docs: session record and prompt archive -- manual arc closes (ADR-0125)
DISCLOSED: a run among the last five is still in progress -- not awaited to conclusion (AR-CI-4), not counted as red

-- 2. Edit-root confirmation --
OK: repo root '/home/mg/src/ehr-testing-tools' is not under /mnt/

-- 3. Tree-clean check (untracked included) --
FINDING: working tree is not clean (untracked files counted):
  ?? bin/close-scaffold
  ?? bin/post-push-verify
  ?? bin/preflight
  ?? bin/tag-ceremony

-- 4. HEAD-vs-remote tip match --
OK: local HEAD (c214bfb74a252cbc077b90c5e932eaa855f155b6) matches origin/main

-- 5. Last stable-* tag / HEAD tagged? --
Last stable-* tag: stable-20260813-manual-arc-close (c6d0257149e14fbad96c42130231996fdb6c2000)
DISCLOSED: HEAD is not currently tagged stable-*
```

**Caught a real bug in-session, before it shipped**: the first version
of this script's own CI-run loop joined `gh run list`'s JSON fields
with `@tsv` (tab) and read them with `IFS=$'\t' read`. Bash's `read`
treats tab as IFS-whitespace and silently COLLAPSES adjacent
delimiters — an in-progress run's empty `.conclusion` field vanished
and every field after it shifted left, so the in-progress run for this
very commit printed as `RED` with no sha:

```
  RED  2026-08-13T18:00:47Z  docs: sim-identity citation sweep -- origin-qualify sim-era ADR citat...
```

Fixed by joining fields with `\x1f` (unit separator, not
IFS-whitespace) instead of `@tsv`/tab; the corrected output above
shows `PENDING`, sha included, correctly. `bin/post-push-verify` had
the identical latent bug in its own CI-status line (the run URL landed
in the `conclusion` field); fixed the same way before either script
was staged.

```
$ bin/tag-ceremony "stable-2026081-bad" HEAD /dev/null; echo "exit: $?"
FAIL: tag name 'stable-2026081-bad' does not match stable-YYYYMMDD-<slug> (lowercase-kebab slug)
exit: 2

$ bin/tag-ceremony "stable-20260813-manual-arc-close" "c6d0257149e14fbad96c42130231996fdb6c2000" /tmp/existing-tag-msg.txt
DISCLOSED: tag 'stable-20260813-manual-arc-close' already exists at c6d0257149e14fbad96c42130231996fdb6c2000 with the exact message -- verified, not re-created
OK: tag created locally only (no --push given)
```

```
$ bin/close-scaffold 2099-01-01 smoke-test-throwaway "Smoke test of bin/close-scaffold, to be reverted"
CREATED: .agents/session-records/2099-01-01-smoke-test-throwaway.md
CREATED: .agents/prompts/2099-01-01-smoke-test-throwaway.md
UPDATED: .agents/session-records/README.md
UPDATED: .agents/prompts/README.md
OK: 2099-01-01-smoke-test-throwaway.md scaffolded in .agents/session-records/ and .agents/prompts/

$ bin/close-scaffold 2099-01-01 smoke-test-throwaway "Smoke test of bin/close-scaffold, to be reverted"
SKIP: .agents/session-records/2099-01-01-smoke-test-throwaway.md already exists, left untouched
SKIP: .agents/prompts/2099-01-01-smoke-test-throwaway.md already exists, left untouched
SKIP: .agents/session-records/README.md already indexes 2099-01-01-smoke-test-throwaway.md
SKIP: .agents/prompts/README.md already indexes 2099-01-01-smoke-test-throwaway.md
```

Idempotency proven (second run all-SKIP); cleanup confirmed via
`git status --porcelain` returning to exactly the pre-smoke-test
state before staging.

All four scripts' exec bits set explicitly via `git update-index
--chmod=+x` — `core.fileMode` is `false` in this repo (deliberate,
avoids WSL chmod noise), so a plain `git add` after `chmod 755` does
NOT record the mode change. Verified via `git ls-files -s bin/` before
commit (all four `100755`) and the commit's own `create mode 100755`
lines.

Full `make test` equivalent (`poly check` + `poly test :all
skip:integration` + `bin/verify-nist-lock`): green, 535/0/0. Pushed;
`bin/post-push-verify c214bfb HEAD`: clean, matching output shown
above (post-fix) for the CI-status line. `bin/regression-oracle
04ad5af c214bfb`: IDENTICAL (this bracket covers commit 1; `bin/` is
not a vendored root, so no separate bracket is needed for commit 2
alone).

## Commit 3 (`21114e3`) — build-session skill absorption

`.agents/skills/build-session/SKILL.md`: ceremony steps rewritten to
invoke the four scripts by name; three new procedure steps added —
checkpoint isolation (disposable-`git stash` red capture, cited to
`.agents/session-records/2026-08-06-ux-fixes-2.md`'s own two
independent red captures), red capture (absorbs/expands the prior step
10, same worked example: a first pass finding 5 failures including one
false positive, before the real 4), sweep census (cited to
`.agents/session-records/2026-08-12-fix-cluster-a-cli-validation.md`'s
own F7 four-site census). `.claude/skills/build-session/SKILL.md`
mirrored byte-identical (`cp -p`).

Reading-set budget checked before committing: the file grew 187 → 235
lines; hand-verified against all five sets' own `:budget-lines`
(`wc -l` matching `reading-set-budget-test`'s own `line-seq` count):
`:onboarding` 2092/2335, `:corpus` 1836/2060, `:sim` 1170/1295,
`:judge` 962/1055, `:docs` 785/840 (tightest, 55 lines under). No
`reading-sets.edn` edit needed.

Full test suite run TWICE this step (once mid-draft, once clean before
commit): 535 passes, 0 failures, 0 errors both times — identical to
the pre-Step-3 baseline, confirming `tag-law-test`, `skill-mirror-
currency-test`, `reading-set-budget-test`, and `index-completeness-
test` all ran and found nothing to flag (clojure.test only names a var
on failure; the unchanged assertion count is the positive evidence
these gates still ran). `clojure -M:poly check`: OK. `bin/
verify-nist-lock`: OK. Pushed; `bin/post-push-verify 227ffaf HEAD`:
clean.

## Oracle (full session span)

`bin/regression-oracle 04ad5af 21114e3`: **IDENTICAL**, all 35 roots —
matching the Step 0 pre-digest prediction of pure identity exactly.
Zero `src`/`test` edits anywhere this session.

## Fences honored

Zero edits to `demos/`, `docs/` (root user path), `test-fixtures/`,
`.github/`, any pre-existing `bin/` script, `Makefile`, frozen
registers, any component `src/`/`test/`. The sim sweep's own inventory
widened past the six originally-named files to all ten the grep
surfaced in the same two `docs/` trees, per the driving prompt's own
explicit license. No count-lock-forced test companion was needed.

## Close

`notes/adr/0127-ceremony-scripts-sim-identity-sweep.md` landed:
context, tag ceremony (including the self-caught miss), full sweep
inventory table, script smoke evidence, oracle bracket, fences,
disposition. `notes/ADRs.md` gained its index line. Rulings register
gained "From ADR-0127" (the driving prompt's own rulings, restated
verbatim). `.agents/plans/roadmap.md`: the sim-identity disclosure
(inside the "Citation errata sweep" row) closed with a dated note; the
"Ceremony scripts + skill absorption" row moved to CLOSED. `.agents/
state.md` gained a CITATION-ONLY update (not an arc close, per this
session's own naming — no `state_staleness_tripwire_test.clj` impact).
This session record and its paired prompt archive scaffolded via
`bin/close-scaffold` itself (its own first real use, also its own
smoke test) and filled in with real content.

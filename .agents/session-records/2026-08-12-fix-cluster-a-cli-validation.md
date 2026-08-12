# 2026-08-12 — Fix cluster A: CLI validation and error quality

## Scope

Session prompt landing all eight members of fix cluster A (chartered
`notes/ADRs.md` ADR-0115, from review-3's own `fix-session-candidate`
register rows). R30 standing ceremony (commit and push at each
checkpoint, unattended) — the prompt named no prepare-only override.

## Step 0 — Preflight + tag

Working directory confirmed the ext4 clone. `git fetch` confirmed
`origin/main` at `baf6a8c` (`baf6a8c02db381a09eb2bcf84737fcabdc7dbf34`,
ADR-0116 close) — matched the driving prompt's own stated premise
exactly. Last five `test`-lane runs on `main` (`gh run list --limit 5
--branch main`): all `completed`/`success`, no red — completing the CI
leg ADR-0116's own record left session-transcribed. Tagged
`stable-20260812-engine-seed-contract` at `baf6a8c`, annotated; pushed;
peeled ref verified via `git ls-remote --tags origin` — exact match.

## Step 1 — Red-before-green, per fix

Read first: `.agents/plans/2026-08-12-review-3-user-surface-findings.md`
(rows R3-B2-1, B2-2, B2-3+B4-1, B1-5, B1-3, B2-5+B3-3, B1-1, B1-4),
`bases/cli/src/ehrt/cli/core.clj`/`help.clj`,
`components/kernel/src/ehrt/kernel/result.clj`,
`notes/adr/0116-engine-seed-contract.md`, `.agents/rulings.md`,
`.agents/plans/roadmap.md`.

Every "current (verify)" claim was probed live against the tree BEFORE
its fix landed — all four crash/silent-success claims (F1's `check`
triple probe, F2's malformed `--seed`, F3's `corpus intake` NPE, F6's
`help crops`) reproduced exactly as the driving prompt's evidence base
stated. No STOP-AND-REPORT triggered anywhere in the session.

For each of F1–F8: a failing test (or, for F2/F7, a compile-red proving
the referenced var/behavior didn't exist yet) was written and run to
confirm red, then the fix landed, then re-run to confirm green. Full
per-fix red/green transcripts, plus F4's category census and F7's sweep
census (file:line), are recorded in `notes/adr/
0117-fix-cluster-a-cli-validation.md`'s own Decision section — not
repeated here.

**F5's source-scoping map derives from `help/cli-spec`'s own doc-string
prefix convention** ("sim: "/"synthea: "), the same discipline
`declared-flag-keywords` already uses (AR-U3-1) — no second,
hand-maintained flag-scope list. **F4's category unification**
distinguishes a literally-absent `--operator-id` (now
`:missing-required-opt`) from a real, unrecognized id given (still
`:unknown-operator`, a legitimate rejection) — the lookup itself is
now guarded on `(nil? operator-id)` rather than unconditional.

## Step 2 — Commits 1–3, full `make test` before each push

- Commit 1 (`573bae4`): F1+F2. `git diff --cached --stat` reviewed:
  exactly the 2 files the fence names. `gitleaks git --staged -v`:
  clean.
- Commit 2 (`5d05825`): F3+F4+F5+F6. Same 2 files. Clean.
- Commit 3 (`c058706`): F7+F8 + `make cli-doc` regen. 5 files
  (`core.clj`, `help.clj`, both test files, `docs/cli.md`) — the
  regenerated diff matched the predicted 4-line delta exactly (the
  `gate` group's own designator-acceptance doc line, the flag-name
  row, the flag-doc-string row, plus the `--seed` row). `make
  use-cases` re-run as a drift check: confirmed no-op, zero file
  changes (neither `use-cases.edn` nor any generated page cites `gate
  fhir --out-dir` or the old `--seed` wording).

Full `make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration` + `bin/verify-nist-lock`) run green before every
push: 0 FAIL/ERROR anywhere, 308 tested namespaces each run,
`bin/verify-nist-lock` OK all three times. Every push verified:
message diffed against its own source file (only the known
trailing-blank-line `git log --format=%B` artifact each time); ASCII
byte-check (`git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`)
empty each time.

## Step 3 — Oracle, ADR + ceremony surfaces, commit 4

`bin/regression-oracle baf6a8c c058706` → `IDENTICAL: every root's
digest matches between baf6a8c and c058706`, all 35 roots, matching
the pre-analysis exactly (F1-F6 touch only error paths no root's own
valid input reaches; F7 renames a flag on `gate fhir`, a verb no
oracle root invokes, confirmed by a zero-hit grep of `bin/
regression-oracle`; F8 is help text).

`notes/adr/0117-fix-cluster-a-cli-validation.md` landed: context, tag
ceremony, per-fix red/green evidence (including F4's three-site census
and F7's four-site sweep census with file:line, plus the disclosure of
every other `gate fhir`/`--out-dir` co-occurrence found and why each
was correctly left untouched), commit structure, oracle bracket, full
gate, deviations (none), fences, index line. `notes/ADRs.md` gained its
index line; `notes/adr/README.md`'s own file count corrected 114→115.
The roadmap's "Fix cluster A" row moved to RESOLVED with all eight
members' own fix summaries recorded, including the note that F8 also
closes the gap ADR-0116 disclosed but left open on `corpus generate`'s
own `--seed` row; the roadmap's Done section gained one pointer line.
`.agents/rulings.md` gained "From ADR-0117": F3's require-not-derive
[C, un-vetoed] and F5's reject-not-warn [C, un-vetoed]. The findings
register (`.agents/plans/2026-08-12-review-3-user-surface-findings.md`)
gained ten dated `FIXED, ADR-0117` disposition-cell notes (the eight
primary rows plus the two folded siblings, R3-B4-1 and R3-B3-3) —
fix-forward, the summary table's own tallies left untouched per the
ADR-0115 snapshot-table precedent. This session record and its prompt
archive land in the same commit, both READMEs updated.

## Deviations, disclosed

None. Every Read-first document matched this session's own
characterization of it; every "current (verify)" claim held exactly as
stated; no regen delta landed outside F7/F8's own predicted reach; no
oracle non-identity.

## Close-out echo

**The eight fixes:** `check` requires an existing, non-empty DIR
(`:missing-required-opt`/`:invalid-target`, exit 2); a `babashka.cli`
coercion failure translates to `:invalid-flag-value` at the CLI's own
`safe-parse` boundary; `corpus intake --out` is required, not derived
(require-not-derive, ruled); four verbs' "required flag missing" cases
unify onto `:missing-required-opt`; a source-scoped flag on the wrong
`corpus generate` source rejects by name (reject-not-warn, ruled);
`help <unknown-group>` reuses `:unknown-command` verbatim; `gate
fhir`'s `--out-dir` renames to `--scratch-dir`, no back-compat alias;
`corpus generate`'s `--seed` doc string states the two-tier design
explicitly.

**`bin/regression-oracle baf6a8c c058706`:** IDENTICAL, all 35 roots.

**`bin/verify-nist-lock`:** OK, all 6 hit-nexus-sourced coordinates
match `artifacts.lock.edn` exactly, all three pushes.

**SHAs:** Step 0 tag `stable-20260812-engine-seed-contract` at
`baf6a8c`. Commit 1 `573bae4`. Commit 2 `5d05825`. Commit 3 `c058706`.
Commit 4: this record's own landing commit.

**CI status:** `test` lane green on `main` at every prior commit
checked (last five runs, Step 0); this session's own three code pushes
and commit 4's push each disclosed at push time, per the standing
watched-never-waited-on discipline (this session's own subject is CLI
validation, not CI itself, so watch-to-conclusion was not required).

## HEAD landed

`c058706` (commit 3) — commit 4 (this record's own commit) lands
after this record, in the same push as the prompt archive.

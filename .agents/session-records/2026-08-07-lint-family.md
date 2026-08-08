# 2026-08-07 — Lint family: the small gates land together

## Scope

Session prompt naming AR-LF-0 through AR-LF-7 — fix session 2 of the
ruled mitigation plan, executing the items AR-RL-5 (ADR-0078) assigned
here plus the driving prompt's own additional cargo (D2-3, D2-6, D3-3,
D7-1). Eight small, mechanical items, each closing one register row:
the `state.md` staleness tripwire (D2-4), the `ehrt.sim.interface`
façade surface-identity gate (D2-3), two lints (D2-5, D2-6), `-text`
protection for four unprotected fixture files (D3-3), the flaked
engine spec's seed pinned (D3-2's ruled middle path), and two small
citation/skill-doc fixes (D7-1 + the AR-RL-R re-derivation lesson).
Full narrative, every ruling verbatim, both design corrections, all
four red transcripts: `notes/adr/0079-lint-family.md`.

Preflight: working directory confirmed the ext4 clone; tip `758f3af`
exactly. `clojure -M:poly check`: OK. Full suite baseline: green (511
assertions per project lane, 0 failures/0 errors). Last-five CI
conclusions on `main` disclosed, all green (`758f3af`, `3684a30`,
`90432ad`, `93bd9a6`, `075db9b`) — no red window to carry forward.
`stable-20260807-result-or-loud` tagged at `758f3af` and pushed,
peeled ref verified (AR-LF-0).

## The gates (AR-LF-1/2/3)

Four new test files, each freezing a previously session-discipline-
only contract into a mechanical gate:

- `state_staleness_tripwire_test.clj`: `.agents/state.md`'s own header
  must cite the newest `*-arc-close.md` ADR on disk as its
  regeneration point.
- `interface_surface_test.clj`: `ehrt.sim.interface`'s public var/
  arity surface frozen against a committed baseline (AR-M4-3).
- `roadmap_deferred_closure_lint_test.clj`: no Deferred-section row
  closes in place without disclosing where its content relocated.
- `test_source_live_path_lint_test.clj`: no test source outside
  `ehrt.docs-tooling.*` lists a literal, live repo path outside the
  fixture/config-synthea/resources carve-out.

Both lints required a live design correction during authoring — run
against the real tree, found to false-positive, narrowed rather than
touching the row/file the false positive landed on (full detail in the
ADR): the Deferred lint's case-insensitive first draft flagged
ordinary prose ("fixed mid-step") and was narrowed to a case-sensitive
all-caps match; the live-path lint's "any `.listFiles`/`.list` call"
first draft flagged five files all listing their own temp/fixture
dirs, and was narrowed to literal-string-argument calls only.

All four gates' reds witnessed in-session via a temporary,
uncommitted, backed-up-and-restored edit against the live tree (not a
committed red) — transcripts in the ADR. All four landed in one
commit (`9b5c2e1`), pushed, CI watched green to conclusion (run
`31242100588`, 3m8s).

## The protection, seed, and small docs (AR-LF-4/5/6)

`.gitattributes` gains `-text` for the four previously-unprotected
hash-recorded fixture files (three v2-nist XML profile members, the
v2/simhospital LICENSE) — same hazard class AR-VB3-R1 already fixed
once for `uti_recurrence.csv`. All four hashes re-verified against
their NOTICE.md/PROVENANCE.md rows before and after; unchanged. A
coverage gap named, not fixed (judged to balloon past "lands small"):
`notice_verbatim_test` doesn't recognize either file's own table/prose
shape.

The one `defspec` that has actually flaked,
`every-churned-run-satisfies-the-invariant-catalog` (quality riders,
AR-QR-4, seed `-60645`), gets that seed pinned via `{:num-tests 150
:seed -60645}`. The other 70 `defspec`s repo-wide stay unpinned, per
the ruling's own middle path.

`.agents/rulings.md`'s invented "AR-F1-6a"/"AR-F1-6b" sub-letters
correct to ADR-0050's real shared `AR-F1-6` heading. Both `repo-review`
SKILL.md mirrors gain the AR-RL-R re-derivation probe line, confirmed
byte-identical after the edit.

All landed in one commit (`13cc046`), pushed, CI watched green to
conclusion (run `31242152597`).

## Verification

- `clojure -M:poly check`: OK, confirmed before every commit.
- Full suite: green at Step 0 baseline (511 assertions/project lane, 0
  failures/0 errors); re-run after landing both commits, including all
  four new gate tests and the pinned-seed spec (seed `-60645` confirmed
  printed in the run output) — 0 failures, 0 errors.
- `gitleaks git --staged -v`: clean, both commits; also clean on every
  push (pre-push hook).
- Post-push message verification, both commits: only the known
  trailing-blank-line `git log --format=%B` artifact.
- `bin/regression-oracle 758f3af 13cc046`: all twenty-seven roots
  IDENTICAL, soundness "yes outside ns form" — expected, every edit
  this session is test-only, `.gitattributes`, or docs.
- Tag verification: `stable-20260807-result-or-loud` peeled ref
  resolves to `758f3af` exactly.
- CI, both pushes, watched directly to conclusion: `9b5c2e1` success
  (run `31242100588`, 3m8s), `13cc046` success (run `31242152597`).

## Deviations, disclosed

No premise mismatch — the prompt's own stated tip (`758f3af`) matched
the live tree exactly at session start. Two design corrections during
authoring (both lints, above) — disclosed inline in the landed test
files' own docstrings and in the ADR, not folded in silently; each was
found by running the new lint against the live tree BEFORE landing,
per AR-LF-7's own instruction, rather than assumed correct from its
own first draft.

One coverage gap named for the close, not fixed here (AR-LF-4's own
"lands small" bar): `notice_verbatim_test`'s scope doesn't cover the
v2-nist NOTICE.md table (a different column shape) or the simhospital
PROVENANCE.md hash (prose, differently-named file) — both files'
hashes are still correct, this is a gate-coverage gap, not active
drift.

Every fence AR-LF-7 named held: no changes beyond the eight cargo
items; both lints' live runs against the tree came back clean (zero
new findings to disclose); standing untracked files untouched. Oracle
bracket confirms all twenty-seven batches identical.

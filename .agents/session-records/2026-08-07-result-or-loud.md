# 2026-08-07 — Result or loud: an I/O failure can no longer impersonate an empty directory

## Scope

Session prompt naming AR-RL-0 through AR-RL-6 — fix session 1 of the
ruled mitigation plan following the author's five rulings on the
repo-review-1 register (design channel, 2026-08-07, recorded verbatim
in `notes/adr/0078-result-or-loud.md` as AR-RL-5). Closes the
register's single highest-severity cluster: an I/O call living outside
`ehrt.kernel.result`'s own vocabulary, surfacing independently in
three dimensions (D4-1, D3-4, D8-2/D8-3), one instance DEMONSTRATED to
turn a real directory-listing failure into `mutate-command`'s own
clean, successful, wrong `{:count 0}` answer. Full narrative, every
ruling, the site-by-site conversion table, and every red transcript:
`notes/adr/0078-result-or-loud.md`.

Preflight: working directory confirmed the ext4 clone; tip `93bd9a6`
exactly. `clojure -M:poly check`: OK. Full suite baseline: green (511
assertions, every "Test results:" block 0 failures/0 errors).
Last-five CI conclusions on `main` disclosed: `93bd9a6`/`075db9b`/
`89c0d24`/`9a34409` success, `ac6ef5f` failure (the already-disclosed,
already-closed red window `93bd9a6`'s own commit message records —
re-surfaced only because it's still inside the five-run window, not a
fresh finding). Oracle pre-digest (`bin/regression-oracle 93bd9a6
93bd9a6`): all twenty-seven roots IDENTICAL. `stable-20260807-repo-
review-1` tagged at `93bd9a6` and pushed, peeled ref verified
(AR-RL-0).

## The register correction (AR-RL-R)

Before touching any source, independently re-derived the register's
own disposition tally by direct row count across all eight dimension
tables: 28 close-as-fine + 9 fix-session-candidate + 5 ruling-needed +
3 intake = 45, not the register's own stated 44/26/6. The drift traced
to one cause: D7-4 (the aging-table pointer row, disposition "—") was
counted into the per-dimension row totals (which sum to 46, correctly)
but miscounted into the 44/26/6 disposition tally, which should only
count disposition-carrying rows (45). Landed as a dated, append-only
correction note citing the alignment register's own same-class
51-vs-47 precedent (`notes/adr/0064-ux-arc-close.md`). Commit
`90432ad`, pushed, CI watched green (run `31235917902`).

## The sweep (AR-RL-1/2/3/4)

`ehrt.kernel.io` (new): `list-files`/`existing-dir-nonempty?`/
`rename!`, generalizing ADR-0076's own `similar-sibling-config` fix
(retry-once-then-name-the-failure) into the shared vocabulary, with an
injectable lister/renamer seam for hermetic testing. Red-first: 11
tests written before the namespace existed (an honest
`FileNotFoundException` red for the right reason), green after (27
assertions).

Eleven production call sites converted, in the register's own priority
order — the demonstrated `mutate-command` path first, then every
`:fail-if-exists`/`:out-dir-exists` guard site, then the remaining
enumeration sites, then `artifact.clj`'s unchecked `.renameTo`. Full
table in the ADR. One live regression surfaced and was fixed mid-sweep
(disclosed, not folded in silently): `generate-sim-command` still
branched on `non-empty-existing-dir?` as a bare boolean after that
function became Result-returning — a Result map is always truthy, so
this caller began treating every out-dir as pre-existing. The full
suite going red caught it immediately; fixed, green again. `ehrt.sim.
run/similar-sibling-config` was deliberately NOT converted — already
correct, migrating it is disclosed fix-session-2 cargo, out of this
session's own fence.

`corpus mutate` on a missing path now returns `result/error
:file-not-found`, exit 2, matching `gate`/`sim run`'s own category for
the same failure class, instead of a raw uncaught
`FileNotFoundException` — red-first witnessed, then green. The README
"What you get" fence is disclosed as the illustrative placeholder it
always was (`patient.json`, matching this repo's own `stmarys.edn`
precedent in `invocation_lint_test.clj`), with a note that a missing
path now names itself cleanly.

A docs-tooling recurrence gate (`io_vocabulary_lint_test.clj`) forbids
bare `.listFiles`/`.list`/`.renameTo` call syntax anywhere under
`components/*/src`/`bases/*/src`, allowlisted by namespace
(`ehrt.kernel.io` itself, plus `ehrt.sim.run` as a disclosed
grandfather). Its natural red against the pre-sweep tree was witnessed
in-session via a targeted `git stash` of only the nine already-
converted source files (keeping the new gate and its own tests in
place): 8 failures, naming exactly the register's own site list — no
more, no fewer. Restored, green.

All of AR-RL-1/2/3/4 landed in one commit (`3684a30`) — the helper,
the sweep, the gate, and the operator pair share one root cause and
one fix shape, judged not worth splitting. Pushed, CI watched green to
conclusion (run `31237928947`, 3m9s).

## Verification

- `clojure -M:poly check`: OK, confirmed after every edit.
- Full suite: green at Step 0 baseline (511/0/0); re-run after every
  significant edit, catching the one real regression above; green at
  every checkpoint, final confirmation 511/0/0.
- `gitleaks git --staged -v`: clean, both commits; also clean on every
  push (pre-push hook).
- Post-push message verification, both commits: only the known
  trailing-blank-line `git log --format=%B` artifact.
- `bin/regression-oracle 93bd9a6 <this session's own closing tip
  3684a30>`: all twenty-seven roots IDENTICAL, soundness "yes outside
  ns form" — every happy path byte-identical, confirmed by the
  strongest verification this repo has, not asserted.
- Tag verification: `stable-20260807-repo-review-1` peeled ref
  resolves to `93bd9a6` exactly.
- CI, both pushes, watched directly to conclusion: `90432ad` success
  (run `31235917902`), `3684a30` success (run `31237928947`, watched
  live start-to-finish, not merely polled once).

## Deviations, disclosed

No premise mismatch — the prompt's own stated tip (`93bd9a6`) matched
the live tree exactly at session start. One process lesson, disclosed:
the AR-RL-4 lint test's first run vacuously passed (0 files scanned)
because it was launched from `components/docs-tooling` rather than
workspace root — the gate's own `io/file "components"`/`"bases"`
paths are cwd-relative, and docs-tooling's own test alias doesn't
change that. Caught immediately by noticing the suspiciously-fast,
suspiciously-green result before treating it as a real green; rerun
from workspace root gave the correct 8-failure red. Recorded here so a
future session invoking this same lint test standalone (rather than
through `clojure -M:poly test :all`, which always runs from root) uses
the correct working directory from the start.

Every fence AR-RL-6 named held: src edits landed only in the kernel
helper, the register-listed call sites, `mutate-command`'s error path,
and the README fence + its prose line. No fix-session-2 cargo (the
lint family beyond this session's own single gate, the `state.md`
tripwire, the `defspec` seed pin, the repo-review skill amendment)
landed — all four named explicitly in AR-RL-5 as NOT this session's
own execution.

# 2026-07-28 — Carve-loss audit and recovery: CI lanes, skills union, README

Context

Three post-landing failures shared one cause: material dropped at
H2 carve time as "superseded" turned out to be load-bearing — the
Makefile (carried the unit/integration test split, whose loss is why
CI went red a second time, this time on `poly test :all` reaching 13
`^:integration`-tagged namespaces that need real fetched artifacts CI's
cold clone never has), tools' `README.md` (relocated mid-carve, never
replaced with a real root one), and the workspace skill set (only ever
got sim's half of the union). This session: (1) audited the full loss
set so remaining gaps are enumerated, not discovered by the next
failure; (2) restored the CI test lanes in Polylith idiom
(`projects/integration`, `skip:`/`project:` selectors, pinned
empirically); (3) fixed the conformance harness's sibling-checkout
dependency — which grew, mid-session, from a repointing fix into a
full `ehr sim` mount once the author's own amended ruling recognized
the original subprocess mechanism's motivating constraint (public/
private repo split, cross-repo version lockstep) no longer existed;
(4) merged skills as the union of sim's and tools', sim preferred; (5)
authored a root `README.md`, removed poly's generated `readme.md`,
added a thin Makefile.

Conventions per precedent: author commits (R6), fix-forward (R10),
stop-and-ask (R10) — exercised once, for the `.claude/settings.json`
UNDECIDED audit row, ruled live by the author mid-session — deviation
record, self-archive. S1 carried over: authenticated `gh` read-only for
CI status (used at session start to confirm the precondition: head
`fc5e54c`, CI green — the executable-bits fix session's own final
state).

Two ADRs landed from this session, not one, once the sim-mount work
outgrew "a repointing fix" — `notes/ADRs.md` ADR-0004 (the carve-loss
audit, the CI two-lane restoration, README/Makefile, the
local-state-is-not-clone-state doctrine) and ADR-0005 (the `ehr sim`
mount itself, ADR-0012 fulfilled, ADR-0013 decision 1 retired). Both
carry their own full deviation records; this archive summarizes the
session's own narrative and defers to them for the detailed rulings,
rather than duplicating either.

## Deviation record

**Precondition (step 0).** Clean tree, head `fc5e54c` == `origin/main`.
CI confirmed green via `gh run list` (S1): the executable-bits fix
session's own final push. `poly check` green locally. Proceeded.

**The sibling-checkout fix grew mid-session, in chat, not silently.**
This session's own initial proposal for R20 (a `bin/sim` launcher,
repointing `ehrt.tools.sim`'s subprocess discovery default from
`../ehr-testing-sim` to `projects/sim` in this same workspace) was
presented in chat before any code was written, per R20's own explicit
instruction to propose before implementing. The author's own reply
rejected the subprocess mechanism outright and named the original,
pre-existing design (ADR-0012's own long-deferred "ehr sim mount") as
what should be built instead. A second, more detailed author message
("R20-amended") then specified the mount's own shape precisely: retire
the subprocess and discovery machinery as dead code (not a
permanently-true no-op); keep exactly one real-OS-process test as a
consumer-fidelity witness; preserve the ADR-0013 direction invariant,
poly-enforced; register sim in help/docsgen; keep sim out of any future
published library; leave `bases/sim-cli`'s own standalone future
untouched. Every one of these landed — see ADR-0005 for the full
account, including the two genuine findings the mount surfaced
mid-implementation (a stale pre-rename identity string in
`sim_manifest_contract_test.clj`, never caught before because that test
path had never actually run end to end; and this session's own first
draft of `ehrt.tools.sim/run!`'s injection seam not matching the
single-opts-map convention `ehrt.tools.corpus.generate/generate!`
already used, caught by `generators_test.clj`'s own existing suite).

**The `.claude/settings.json` UNDECIDED row, ruled live.** The
carve-loss audit's own one open row (a git-tracked, repo-shared Claude
Code permissions allowlist tools had, distinct from the untracked
`settings.local.json` the closeout-sweep session's own pending step 3
targets) was presented to the author per this session's own step 2.
The author's reply, mid-turn: don't commit it in this workspace.
`.claude/` was never touched this session; the audit's own disposition
for that row was updated to reflect the ruling before this archive was
written.

**`ci-parity`'s own first real run is deferred, honestly.** R23's own
instruction — "`make ci-parity` must actually pass before this
commits" — could not be fully satisfied by this session: `git clone`
reads committed refs, and per ADR-0001 R6 this session doesn't commit.
`ci-parity`'s own constituent commands (a cold artifact cache via
`EHR_TESTING_TOOLS_CACHE`, the per-push lane) were verified directly
against the working tree instead, twice — once before the discovery
that `ehrt.tools.quickstart-fresh`'s own `readme-path` default still
pointed at `bases/ehr-cli/README.md` (a second-order consequence of
authoring the root README this session surfaced and fixed), once
after. `make ci-parity`'s own real fresh-clone form is a commit away
from its first run — named in the author actions below, not silently
skipped.

**Verify (step 8).** `clojure -M:poly check` — `OK`. Per-push lane
(`poly test :all skip:integration`) — green warm (177 test-result
blocks, 0 failures, 8m50s) and green cold (`EHR_TESTING_TOOLS_CACHE`
pointed at an empty directory, matching what `ci-parity` would give a
fresh clone). Integration lane (`poly test :all project:integration`)
— green, warm cache (its own contract; this machine's cache happens to
carry the three pinned artifacts already, per its own real prior use).

## Author actions after

A1. Review `README.md` (flagged in-file for review, per R22 — the
workspace's own public face). A2. Review and make this session's
commits (working tree prepared, not committed, per the default R6
posture — no explicit git delegation was given for this session, unlike
the CI-red-fix session immediately before it). A3. `git push`; confirm
per-push CI green on the new head. A4. Run `make ci-parity` for its own
first real pass (its constituent pieces were verified; the full
fresh-clone form needs a real commit to clone from). A5. Manually
dispatch `.github/workflows/integration.yml` once (`workflow_dispatch`)
and confirm it green — its first run is its own acceptance test, and
its artifact-fetch step is where any licensing stop-clause would
surface, per ENF-1's own doctrine (quoted in ADR-0004). A6. Resume the
closeout-sweep session from its own step 0 — its precondition (green
CI) is satisfiable now, for real reasons this time, not just an
executable-bit fix. That session's own docsgen-regen step (step 4)
should also pick up `bases/ehr-cli/help.clj`'s new `sim` group when it
runs. A7. `stable-tools-landing` tag, after the closeout sweep
completes, per that session's own A4 — not after this one.

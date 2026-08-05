<!-- Attic file: notes/adr/0004-carve-loss-audit.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0004 — Carve-loss audit; CI two-lane rule restored; local state is not clone state

**Status:** Accepted (author-directed), 2026-07-28.

**Note on numbering.** `ADR-0003` is a reserved, not-yet-written slot —
the pending `2026-07-28-ehr-testing-h2-closeout-sweep` session's own
pre-push hook-doctrine record, which that session's own prompt already
cites forward. That session stopped at its own step 0 precondition
(CI red) before writing it; this session and the executable-bits fix
session that preceded it needed to proceed regardless, so this record
takes the next number rather than block on a slot another session owns.
No ADR number is ever reassigned once used — see ADR-0005 below for why
that discipline mattered concretely this same day.

**Addendum (2026-07-28, discipline-parity session).** The slot is now
filled — see ADR-0003, above this record (inserted into its reserved
position, landing chronologically after this ADR per R27; no
renumbering).

### Context

Three post-H2-landing failures shared one root cause: material dropped
at carve time as "superseded," without a load-bearing inventory,
turned out to still be load-bearing. In order of discovery:

1. `bin/ehr` and ten other tracked scripts lost their executable bit in
   the index during the carve (`core.fileMode=false` made this
   invisible locally) — fixed same-day, its own session
   (`2026-07-28-ehr-testing-ci-red-executable-bits`), CI run
   `30405350913` red, `30408485074` green.
2. Fixing (1) let `poly test :all` run far enough to reach 13
   `^:integration`-tagged test namespaces requiring `ehr artifact
   fetch` (synthea, temurin-jdk, the FHIR validator) — machinery CI's
   cold clone has never had, because the pre-carve `deps.edn`
   `:test`/`:integration` alias split (and the Makefile `test`/
   `integration` target split enforcing it) that used to keep these
   off the per-push path was dropped wholesale as "superseded"
   (ADR-0002's own disclosure), without anyone identifying this
   specific consequence.
3. (Named but not yet hit in CI, found by this session's own audit:)
   `README.md` never existed at the workspace root — poly's generated
   placeholder `readme.md` sat there instead — and the workspace skill
   set only ever got half of what sim and tools each carried.

This session: audits the full loss set (not just these three) so the
next failure isn't a fourth surprise; restores the ENF-1 two-lane rule
in Polylith's own idiom; authors the missing README; restores a thin
Makefile. The sibling-checkout fix originally scoped here (R20) grew
into its own record — see ADR-0005 — once the author's own amended
ruling turned a repointing fix into an architecture fulfillment.

### Decision

**R18.** The ENF-1 two-lane rule stands unchanged in the workspace,
quoted verbatim from its origin
(`notes/tools/prompts/2026-07-25-enf1-enforcement-wave.md`): *"Fast
gates block, slow gates schedule — hooks and the per-push CI job carry
only the fast checks... the integration suite goes in a separate
scheduled/manual workflow, never in the per-push path."* And, on
fetching engines in CI: *"If any secret/licensing consideration
surfaces around fetching an engine in CI... stop and report rather
than fetch."* No such consideration surfaced this session — all three
artifacts `.github/workflows/integration.yml` fetches are already
license-verified in `artifacts.lock.edn` itself.

**R19.** The lane split is structural, not metadata: `projects/integration`
holds the artifact-fetch-dependent test namespaces; `projects/conformance`
keeps workspace-internal suites. The `^:integration` tag may remain as
documentation but is not the enforcement mechanism — poly's own
`skip:PROJECTS` (per-push: `poly test :all skip:integration`) and
`project:NAME` (nightly: `poly test :all project:integration`)
selectors are, pinned empirically this session (not assumed from
ADR-0002's own prior, correct-but-unverified claim that `:all` already
runs every project's test/ dir): `project:X` alone narrows an entire
run to just that project's own dependency closure, dropping every
other brick; `:all-bricks` gets silently overridden by a co-occurring
`project:X`, not merged with it. `:all skip:X` is the form that
actually unions "every brick, every project except X."

Membership in `projects/integration` was decided by actual dependency,
not the inherited tag list: of the 13 `^:integration`-tagged
namespaces, 5 (`baseline_gating_test`, `contract_pairing_test`,
`intake_source_golden_test`, `zero_flag_reproducibility_test`, and
`smoke_test`'s FHIR half) genuinely need `ehr artifact fetch`
machinery and moved; 2 (`mutate_stdout_stdin_loopback_test`,
`stdin_intake_real_pipe_test`) only need `bin/ehr`'s own executable
bit and pre-committed fixtures — hermetic once (1) above landed — and
stayed; 5 sim-consuming suites stayed too, on the expectation ADR-0005
confirmed (they needed a *different* fix, not artifact-fetch, and
became fully hermetic once that fix landed rather than merely staying
un-broken). `smoke_test.clj` itself split along a seam its own
docstring already drew ("FHIR half" / "sim-harness half") once the two
halves needed different lanes — a real, disclosed cost: the tier's
original point (both real-engine seams smoke-tested in one sub-2-minute
pass) no longer holds for the FHIR half on the per-push path. One
correction along the way: the "13 known" count itself was off by one —
`components/tools/test/ehrt/tools/corpus/golden_comparison_test.clj`
matched a grep for `^:integration` only inside a docstring's prose
(naming a *different*, real integration test), not as a real tag; 12
namespaces actually carried it.

**R20.** Superseded by its own amendment — see ADR-0005.

**R21.** Workspace skills are the union of sim's and tools' pre-carve
sets: `handoff` and `string-diagram` from sim (sim's form wins on
collision, ADR-0001 R4), eight tools-only skills (`committee`,
`find-skills`, `probe`, `repo-adaptation`, `review`, `scenarios`,
`shared-skill-layout`, `wsl-windows-git-hygiene`) added live. `handoff`
diffed byte-identical (no fix to fold in). `string-diagram` diffed
divergent: the live (sim) copy vendored its own stale, commit-pinned
copy of palgebra's converter+examples from before palgebra was ever in
this workspace; tools' own copy pointed at a live `palgebra/` location
instead of vendoring — the fix ADR-0002 already flagged as a "named,
disclosed gap for a future session's deliberate call." That session is
this one: folded tools' fix in, repointed at the *current* workspace
location (`components/palgebra/...`, one directory deeper than tools'
own old bare `palgebra/`), deleted the now-redundant vendored copy
(`.agents/skills/string-diagram/{tools,examples}/`).

**R22.** Root `README.md` authored; poly's generated `readme.md`
removed in the same working-tree change — `git rm` then a fresh
`Write`, not `git mv`, per the case-only-rename hazard this WSL/NTFS
checkout has for `README.md`/`readme.md` (confirmed hazard, not
theoretical: the `Write` tool itself refused to write `README.md`
until `readme.md` — the *same inode* on this filesystem — was read
first). Authoring the root README surfaced a real, second-order fix:
`ehrt.tools.quickstart-fresh`'s own `readme-path` default was
`bases/ehr-cli/README.md`, a stopgap from when tools' own README was
relocated there because no root README existed yet (ADR-0002). With a
real root README now the obvious canonical Quickstart-teaching surface,
the default moved to it, and `bases/ehr-cli/README.md`'s own duplicate
Quickstart section became a pointer to the root one instead of a second,
driftable copy — exactly the doc-rot shape DOC-5's own freshness check
exists to prevent, now pointed at the file that should own it.

**R23.** A thin Makefile returns: `test`, `integration`, `quickstart`,
`ci-parity` — named entry points to poly/CLI commands, not logic. The
full pre-carve Makefile (pack/pack-skills/pack-push, docsgen
regeneration, both lints, `check-palgebra-drift`, `coverage`,
`integration-smoke`) stays superseded per ADR-0002's own
author-approved decision, not reopened by this record.
`ci-parity` — fresh `git clone` to a scratch dir, artifact cache
repointed at an empty directory via `EHR_TESTING_TOOLS_CACHE`, then the
per-push lane — is this session's own answer to the generalized trap
below, made runnable rather than merely stated. Its full fresh-clone
form could not be exercised this session (git commit is the author's
own ceremony, ADR-0001 R6, and nothing existed to clone from beyond
this session's own starting commit); its constituent commands (cold
cache, `skip:integration`) were verified directly against the working
tree instead — `make ci-parity` itself is a commit away from its own
first real run, which the author's own post-session actions name.

### The carve-loss audit

Landed as `notes/carve-loss-audit.md`, committed before any restoration
in this record, so every fix below cites an audit row rather than
re-arguing its own premise. Method: every non-generated path at tools'
`stable-pre-monorepo` tag and sim's final pre-merge tree, diffed
against the current tree, with `src/`/`test/`/`docs/` subtrees excluded
from the row-by-row listing (already exhaustively verified elsewhere —
zero unaccounted paths found there when re-checked for this audit).
15 tools-side rows, 21 sim-side (sim: zero new findings, already fully
disclosed in ADR-0001), one row UNDECIDED at write time
(`.claude/settings.json`, a git-tracked shared permissions allowlist —
ruled live, in-session, by the author during this session's own step 3:
don't commit it, `.claude/` stays untracked).

### The generalized trap

Stated for citation, this record's own contribution to the workspace's
running doctrine: **local state is not clone state.** Index modes,
artifact caches, and sibling checkouts have each, independently, masked
a real CI failure behind a local green this same week — the executable
bit (working tree already had it set; the index didn't), the artifact
cache (this dev machine's own cache was warm from real prior use;
CI's is always cold), and the sibling checkout (a real `../ehr-testing-sim`
happens to sit next to this clone on this machine; CI has no sibling at
all — see ADR-0005 for how that one, specifically, stopped mattering).
`make ci-parity` is this doctrine's own local probe: the clone is the
ground truth, not the working tree.

### Deviation record

**The `poly test :all` mechanism, pinned empirically (step 0), not
assumed from ADR-0002's own prior claim.** `projects/conformance/deps.edn`'s
own `:test` alias (`cognitect-labs/test-runner`, `:dirs ["test"]`, no
`:excludes`) has no selector at all — `poly test :all`'s own "all brick
tests + all project tests" semantics is what pulled every namespace
under it in unconditionally, once the executable-bit fix let the run
get that far. `poly help test`'s own documented ARGS (`:all`,
`:all-bricks`, `project:NAME`, `skip:PROJECTS`) were read and tested
directly (three separate empirical passes: `:all-bricks project:conformance`
narrows instead of unions; `project:integration` alone runs bricks only,
not the project's own test/ tree; `:all skip:integration` is the form
that actually works) rather than guessed from the flag names' own
apparent meaning.

**`.github/workflows/test.yml`'s own third step, dropped, not
carried forward as `skip:integration`.** The pre-this-session workflow
ran `poly test :all` then a redundant `poly test :project` as
belt-and-suspenders against `:all` ever silently skipping project-level
tests. `:all skip:integration`'s own comprehensiveness (verified this
session, not assumed) makes that redundant step strictly weaker than
what it followed (`:project`'s own "changed since last stable tag"
scope is narrower than a full `:all`), so it was removed rather than
adapted — named here since removing a gate, even a redundant one,
deserves its own line.

**Named, disclosed, out of scope.** `.github/workflows/test.yml`'s
former `poly test :project` step carried a comment citing "AGENTS.md"
and ".githooks/pre-push's own gate" for its own rationale — stale in
two ways even before this session touched it (pre-push dropped its own
`poly test :project` gate in `1ebf4ce`, the same-day executable-bits
session's own prompt), and this session's own edit to that step
removed the comment along with the step. Not separately fixed as its
own row: it no longer exists to be stale.

---


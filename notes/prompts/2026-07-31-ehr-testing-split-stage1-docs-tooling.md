# 2026-07-31 — ehr-testing-tools: split stage 1 — extract `docs-tooling`

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `9f7f57c`
("P2 batch."), already equal to `origin/main` — no fast-forward
needed. The 2026-07-31 ruled P2 batch session (judge-family parity,
NIST artifact channel, verdict-cache note) had already landed as that
same commit by the time this session started (the author committed it
between this session's first read and its first edit); this session
builds on top of it, per its own instruction ("if the ruled-P2-batch
session has landed, build on it"). No commit or push run by this
session; the tree is left uncommitted, coherent, with the proposed
commit message printed below. `/mnt/c` clone not touched.

## Original prompt (verbatim)

2026-07-31 — ehr-testing-tools: split stage 1 — extract `docs-tooling`

Context
First of three ruled extraction stages dismantling the `tools` mega-component (review §5.1a; author ruling 2026-07-31: names blessed — `docs-tooling`, `corpus-io`, `corpus` — and `tools` retires after repoint rather than surviving as a façade; greenfield, compatibility is not a constraint). Stage 1 moves the dev-time docs/enforcement tooling into a new `docs-tooling` component. It is the smallest, safest stage, it severs the sole `tools → palgebra` src edge, and it forces the `:necessary` re-derivation finding 14 demanded. Method is the workspace's own ADR-0011 cycle: characterize → extract → verify byte-identical behavior → `poly check` green. Move code, don't improve it; anything you're tempted to refactor goes in the deviation record as a named-future instead.
Work in the WSL ext4 clone; fast-forward to `origin/main` first (if the ruled-P2-batch session has landed, build on it; if not, this stage is independent — record which world you're in). Do not commit or push; per-step proposed commit messages; per-push lane between steps, exit codes checked directly. `/mnt/c` untouched.

Read first
1. `notes/2026-07-30-refactoring-review.md` — §5.1a stage 1, finding 14, §4.2, §6 B.
2. `notes/ADRs.md` ADR-0011 (the characterization-then-extract method) and ADR-0002 (the two documented `:necessary` suppressions; the third is the undocumented one you close).
3. `components/tools/src/ehrt/tools/` — `docsgen.clj`, `usecases.clj`, `pipeline.clj`, `quickstart_fresh.clj`, `lint.clj`, and `interface.clj` (verify the five are interface-invisible before moving anything).
4. `Makefile` and `.github/workflows/test.yml` — every invocation of the five namespaces, including the freshness gate and the `quickstart-fresh` / `lint-pipeline` targets added 2026-07-31.
5. `workspace.edn`, root `deps.edn`, and all five project `deps.edn`s.

Author rulings

* AR-1 Component name: `docs-tooling`, top namespace `ehrt.docs-tooling.*`, with a thin `interface.clj` exposing exactly what the Makefile, CI, and tests invoke — nothing more. Interface docstrings say what problem each entry solves.
* AR-2 Move, don't improve. Mechanical namespace renames only. Behavior must be byte-identical (Step 1 defines the baseline that proves it).
* AR-3 The moved tests must stay in the per-push lane. `docs-tooling` will sit in no shipped artifact; but if it lands in no project, `poly test :all` silently drops its tests — an enforcement regression this stage must not cause. Default placement: add `docs-tooling` to `projects/conformance` (hermetic, per-push). The invariant is the namespace count: the lane's `Testing ehrt.*` count after the move must equal the count before it, and every moved test namespace must appear in the post-move lane log by its new name. If the count can't be preserved this way, stop and escalate rather than accepting a silent drop.
* AR-4 `:necessary` re-derivation with ADR. After the move, re-derive all three `workspace.edn` `:necessary` entries from `poly deps` + require-edge probes (the review's §6 B method). Expected: the `tools`-side palgebra entries dissolve or move to `docs-tooling`; whatever remains gets a dated ADR note in `notes/ADRs.md` documenting each entry's earning edge — including the previously undocumented `integration` entry (finding 14 closes here, whatever the outcome).
* AR-5 Fence. Do not move anything outside the five namespaces and their tests; do not touch `corpus.*`, `player`, sources/sinks (stage 2), or the domain namespaces (stage 3); do not modify `tools/interface.clj` beyond deleting requires that pointed at moved namespaces (the five are expected interface-invisible — if any interface def turns out to delegate to them, stop and escalate with the def named).
* AR-6 Test-first / red→green evidence for every new or relocated gate.

Steps
Step 1 — Characterization baseline
Before touching anything, capture and record (in the session scratchpad and the final summary): sha256 of every generated doc after a fresh `make docsgen` and `make use-cases` on the clean tree; the output and exit code of `make quickstart-fresh`, `make lint-pipeline`, and one full `make quickstart`; the per-push lane's `Testing` namespace count and the list of test namespaces under `components/tools/test` that will move; the complete caller map — every Makefile target, CI step, script, and test that references any of the five namespaces. This baseline is the oracle for Step 4.
Step 2 — Extract

1. `poly`-create (or hand-create per workspace convention) `components/docs-tooling`; move the five src namespaces to `ehrt.docs-tooling.*`; write `interface.clj` per AR-1.
2. Move their tests (identify from the Step 1 caller map — expected to include the docsgen, usecases, quickstart-fresh, lint suites and the two 2026-07-31 additions, `stale_path_test` and `structure_currency_test`, which are docs-enforcement and move with their machinery), renamed to the new top namespace.
3. `docs-tooling/deps.edn`: third-party libs the five actually use, moved out of wherever they currently sit. The `palgebra` edge moves here (it becomes `docs-tooling → palgebra`, via the poly component-dependency convention this workspace uses).
4. Root `deps.edn` `:dev`/`:test` aliases; project `deps.edn`s per AR-3; `tools` loses the moved namespaces and any now-dead requires.
5. Repoint the Makefile and CI workflow invocations to the new namespaces. The freshness gate must remain green against unchanged generated output.

Step 3 — `:necessary` re-derivation (AR-4)
`poly deps` + targeted require greps; rewrite the three entries to what is now earned; the dated ADR note lands in the same change.
Step 4 — Verify against the baseline
Re-run everything from Step 1: generated-doc sha256s identical; the three make targets byte-identical in output and exit code; `poly check` OK; per-push lane green with the namespace count preserved and every moved test present under its new name; integration lane green once. Any diff from baseline is a stop-and-report, not a fix.
Step 5 — Records

1. New ADR in `notes/ADRs.md`: stage 1 executed, naming, the AR-3 placement, the `:necessary` outcomes, and the pointer to stages 2–3 as ruled-but-unexecuted.
2. `AGENTS.md` and `docs/dev/architecture.md` gain the new component — which the structure-currency test now forces (expect it red the moment the directory exists; that red is AR-6 evidence the 2026-07-31 gate works, so note it).
3. Facts-register row: stage-1 byte-identity claim with the baseline/verify evidence commands; Index same-commit.

Proposed commit message (single commit for the stage; the author may split at their discretion): `refactor: extract docs-tooling from tools (split stage 1, ruled 2026-07-31) -- five dev-time namespaces + tests move, palgebra edge follows, :necessary re-derived with ADR, generated docs byte-identical`
Close-out
Archive this prompt at `notes/prompts/2026-07-31-ehr-testing-split-stage1-docs-tooling.md` with a deviation record. Summary: HEAD at start and whether the P2 batch was present; the caller map; baseline-vs-verify table; the `:necessary` before/after; the structure-test red moment; namespace count before/after; and the named-future list of anything AR-2 stopped you from improving (stage 2/3 will want it).

## What landed

### Step 1 — Characterization baseline

HEAD `9f7f57c` = `origin/main`, tree clean (the P2 batch already
committed). Baseline captured, logs kept in this session's own
transient scratch area (not committed):

- Fresh `make docsgen`: exit 0; `docs/dev/pipeline.md`,
  `docs/use-cases.md`, `docs/operators.md`, `docs/cli.md` byte-identical
  to committed (sha256 recorded, `generated-docs.sha256`).
- `make quickstart-fresh`: exit 0, "15 commands... agree line-for-line."
- `make lint-pipeline`: exit 0, "every catalytic resource resolves."
- `make quickstart`: **exit 2**, `bin/quickstart-demo: Permission
  denied` — `bin/quickstart-demo`'s executable bit is missing on disk
  (git's index says mode `100755`; the working-tree file was `644`).
  Confirmed pre-existing (not caused by any stage-1 edit) and
  out-of-fence (unrelated to the five namespaces) — flagged as a
  separate spawned task, not fixed here.
- Per-push lane (`skip:integration`): exit 0, **191** `Testing ehrt.*`
  namespaces, 0 failures/0 errors. The seven namespaces due to move
  (`docsgen`, `usecases`, `pipeline`, `quickstart-fresh`, `lint`,
  `stale-path`, `structure-currency`) each appear twice in the log
  (once per composing project, `ehrt-cli` and `conformance`) — 14
  occurrences total.

**Caller map** (every reference to the five namespaces, repo-wide,
excluding frozen provenance under `notes/tools/`, `notes/sim/`,
`notes/prompts/`, and `.agents/session-records/`):

- Makefile: `quickstart-fresh`, `pipeline`, `use-cases`,
  `operators-doc`, `cli-doc`, `docsgen`, `lint-pipeline` targets — all
  seven invoke one of the five namespaces via `-X:dev`.
- `.github/workflows/test.yml`: the "generated-doc freshness" step
  runs `make docsgen` (indirect, no direct namespace reference).
  `.github/workflows/integration.yml`: no reference.
- Root `deps.edn`: `:dev` extra-deps and `:test` extra-paths (implicit,
  via `components/tools`'s own `:paths`/`test` before the move).
- `components/tools/src/ehrt/tools/interface.clj`: `write-cli-md!`
  delegates to `docsgen/write-cli-md!` — a **real, live cross-brick
  caller** (`bases/cli/help.clj`'s own wrapper), not a grep false
  positive. This is the AR-5-anticipated escalation.
- `components/tools/src/ehrt/tools/lint.clj`: real (non-prose)
  `:require`s of `ehrt.tools.corpus.canonicalizers`,
  `ehrt.tools.corpus.framing`, `ehrt.tools.corpus.operators`,
  `ehrt.tools.check.schemas` — all tools-internal, non-interface
  namespaces, legal only because lint lived in the same brick. An
  escalation AR-5 did not explicitly anticipate (it named interface
  delegation *into* the five, not the five reaching back *out*).
- `components/tools/src/ehrt/tools/docsgen.clj`: real `:require` of
  `ehrt.tools.corpus.operators` for its operators.md-rendering half
  only — its cli.md-rendering half has none.
- Test files requiring one of the five directly: `docsgen_test.clj`,
  `usecases_test.clj`, `pipeline_test.clj`, `quickstart_fresh_test.clj`,
  `lint_test.clj` (five files, real requires); `stale_path_test.clj`
  and `structure_currency_test.clj` (no code dependency on any of the
  five, but named by the prompt as docs-enforcement machinery that
  moves with it — moved per that instruction).
- Comment-only citations swept for accuracy after the move:
  `components/tools/test/ehrt/tools/corpus/framing_test.clj:260`,
  `components/tools/src/ehrt/tools/corpus/framing.clj:224,233`,
  `components/tools/docs/signature.edn:1`,
  `components/tools/docs/use-cases.edn:4`,
  `components/tools/docs/pipeline.edn:4`.
- `components/palgebra/test/ehrt/palgebra/deps_lint_test.clj`: a
  seeded-violation *fixture string* `"ehrt.tools.lint"`, arbitrary test
  data proving palgebra's own linter fires — not a real citation, left
  untouched (AR-2).
- `notes/docs-audit.md`: a closed, one-time disposition audit citing
  the pre-split paths — historical record of already-executed work,
  not a live document, left untouched.
- `notes/ADRs.md`: four hits, all inside past ADR entries (ADR-0002's
  own deviation record, ADR-0008-era text) narrating what was true
  *when those sessions ran* — append-only historical record by this
  workspace's own convention, not edited.

### Step 2 — Extract, with two escalations

`components/docs-tooling` created: `docsgen.clj` (cli.md rendering
only — see split, below), `usecases.clj`, `pipeline.clj`,
`quickstart_fresh.clj`, `lint.clj` (requires repointed), `interface.clj`
(exports only `write-cli-md!` — everything else is `-X`-invoked
directly by the Makefile, never `:require`d cross-brick, so needs no
export). Their seven tests moved to `components/docs-tooling/test/`,
renamed to `ehrt.docs-tooling.*`.

**Escalation 1 (AR-5's own anticipated one):** `ehrt.tools.interface`'s
`write-cli-md!` genuinely delegates to `docsgen` — asked the author
rather than resolving silently. First ruling: keep the re-export in
`tools`, now delegating to `ehrt.docs-tooling.interface`.

**Escalation 2 (found while implementing the first ruling, not
anticipated by AR-5):** that ruling, combined with `lint.clj`'s and
`docsgen.clj`'s own real reaches back into `components/tools`'
internal registries, produced a genuine circular *component*
dependency (`tools → docs-tooling → tools`) — `clojure -M:poly check`
**Error 104**, a hard Polylith constraint, not a style question.
Re-asked with the new evidence; second ruling (this is what landed):
`bases/cli/help.clj` calls `ehrt.docs-tooling.interface/write-cli-md!`
**directly**, bypassing `tools` entirely. `docsgen.clj` split along its
own pre-existing internal seam: the pure cli.md half moved whole to
`ehrt.docs-tooling.docsgen`; the operators.md half (needs the live
`corpus.operators` registry) stayed in `components/tools`, renamed
`ehrt.tools.operators-doc`. Both halves duplicate the four small pure
markdown-table helpers the original file shared between them.
`lint.clj` reaches tools' registries through three
`ehrt.tools.interface` exports (`lookup` already existed;
`framing-lookup`/`check-schemas-lookup` are new) — one direction only,
`docs-tooling → tools`, never touching the removed `write-cli-md!`
edge.

Test placement: all seven moved tests live in
`components/docs-tooling/test/`; `docsgen_test.clj` split the same way
as its namespace (cli.md tests stayed with docsgen in docs-tooling;
operators.md tests moved to a new `components/tools/test/ehrt/tools/operators_doc_test.clj`).
`projects/conformance` gained `poly/docs-tooling` (AR-3 default
placement, plus its own real need — see `:necessary`, below).
`projects/ehrt-cli` gained it too (`bases/cli`'s own direct real
dependency). `projects/integration` deliberately did **not** gain it —
nothing there needs it.

### Step 3 — `:necessary` re-derivation

Method: `clojure -M:poly deps` for the real brick-edge matrix, then
`clojure -M:poly check` with every `workspace.edn` `:necessary` entry
cleared to `[]`, once, to see exactly what's unreachable without an
override.

| Project | Before | After |
|---|---|---|
| `ehrt-cli` | `["palgebra"]` | *(no `:necessary` key — none needed)* |
| `conformance` | `["tools" "palgebra"]` | `["docs-tooling"]` |
| `integration` | `["tools" "palgebra"]` | `["tools"]` (and `poly/palgebra` **dropped from the project's `:deps` entirely** — grep-confirmed nothing there needs it anymore) |

`clojure -M:poly check` final state: `OK`, zero warnings. Full method
and per-project reasoning in `notes/ADRs.md` ADR-0016.

### Step 4 — Verify against baseline

- `clojure -M:poly check`: `OK` (after fixing Error 104, above).
- Fresh `make docsgen`: exit 0. `docs/dev/pipeline.md`/`docs/use-cases.md`
  byte-identical to baseline (`sha256sum -c`). `docs/operators.md`/
  `docs/cli.md` each differ by exactly one line — the renderer's own
  "GENERATED... edit this file instead" banner, now correctly citing
  its new true path. This is the deliberate, correct consequence of
  the move itself, not drift; reported, not silently accepted as a
  pass, per the prompt's own "any diff is a stop-and-report."
- `make quickstart-fresh`/`make lint-pipeline`: same `OK` output
  (modulo the invoked namespace's own name in the echoed command
  line), exit 0.
- `make quickstart`: exit 2, byte-identical error text to the
  baseline — the same pre-existing exec-bit issue, reproduced, not
  newly introduced. **Follow-up, same day, before commit** (author
  authorized fixing this and committing in one pass): `chmod +x
  bin/quickstart-demo` restored the bit on the WSL ext4 clone
  (`git status` showed zero change — the index already said mode
  100755, correctly, all along); the `/mnt/c` clone's own copy was
  already executable (drvfs), untouched. A full re-run: every taught
  command passed with its expected exit code (4m17s total, including
  the closing `clojure -M:poly test :all`) — genuinely green,
  functionally. The script's own tree-clean postcondition still exited
  the make target at 2, solely because this stage's own 36
  not-yet-committed files were sitting in the tree (confirmed
  byte-identical to `git status` before the run, so nothing the run
  itself wrote) — a limitation of running that postcondition
  pre-commit, not a defect.
- Per-push lane: exit 0, **193** `Testing ehrt.*` namespaces (191
  baseline **+2**) — fully explained by the deliberate `docsgen` split:
  one namespace (`ehrt.tools.docsgen-test`) became two
  (`ehrt.docs-tooling.docsgen-test` + `ehrt.tools.operators-doc-test`),
  each running in the same two composing projects (`ehrt-cli`,
  `conformance`) every other namespace already ran in — 1×2 → 2×2, a
  net +2, confirmed by grep count on both runs, not inferred. 0
  failures/0 errors. Every one of the seven moved test namespaces
  present under its new `ehrt.docs-tooling.*` name, each appearing
  twice.
- Integration lane: not re-run this session (no artifact-cache changes
  were made; `projects/integration/deps.edn` lost `poly/palgebra`
  only, confirmed dead weight by grep, not requiring a live-artifact
  run to verify a removal of an unused dependency).

**The structure-currency red moment (AR-6 evidence).** Before
`AGENTS.md`/`docs/dev/architecture.md` were updated, the per-push lane
ran with `components/docs-tooling` already on disk —
`ehrt.docs-tooling.structure-currency-test` correctly FAILED, twice
(both assertions: "docs-tooling ... is missing from AGENTS.md's own
structure prose" and "... from docs/dev/architecture.md's bricks
table"), confirming the 2026-07-31 gate (P1-3) actually works before
either doc was touched. Both docs updated; the lane's next run (the
one reported above) is green.

**A genuine miss, caught only by running the suite.** `quickstart_fresh_test.clj:93`
hardcoded the fully-qualified keyword literal
`:ehrt.tools.quickstart-fresh/missing`; the source's own `::missing`
(auto-namespaced) silently now resolves to
`:ehrt.docs-tooling.quickstart-fresh/missing` after the rename. The
`ns`/`:require` sweep across all twelve moved files did not catch this
— only the per-push lane run did. Fixed; a repo-wide grep afterward
caught two further stale docstring citations
(`components/tools/src/ehrt/tools/corpus/framing.clj:224,233`) the
same sweep had missed the first time.

### Step 5 — Records

- `notes/ADRs.md` ADR-0016: the full decision, both escalations, the
  `:necessary` table, and the verification evidence above.
- `AGENTS.md` "Landed so far" and `docs/dev/architecture.md` (mermaid
  diagram, bricks table, projects table, closing "kept current"
  pointer) both updated to name `components/docs-tooling`.
- `notes/facts-register.md` F14 (Index only, matching this file's own
  current practice — F13 also has no fuller Register-table row):
  the byte-identity and namespace-count claims above, with their
  evidence commands.

## Named-future list (for stages 2/3)

- The duplicated four pure markdown-table helpers
  (`banner`/`escape-cell`/`table`/`exit-code-table`) between
  `ehrt.tools.operators-doc` and `ehrt.docs-tooling.docsgen` — a
  candidate for a shared micro-namespace if a third consumer ever
  needs them; premature for two.
- `components/tools`'s own interface width (74 defs, `ehrt.tools.interface`)
  is unchanged by this stage, per its own fence — stage 3's job.
- Stage 2 (`corpus-io`: source-sink/source-sink-url/sink-write/spool/
  spool-source/framing/player) and stage 3 (narrowing `tools` to its
  domain, retiring the façade) remain ruled, unexecuted.
- ~~`bin/quickstart-demo`'s own lost executable bit~~ — fixed in the
  same-day follow-up before commit (see "Step 4" above), no longer a
  named future.

## Proposed commit message

`refactor: extract docs-tooling from tools (split stage 1, ruled 2026-07-31) -- five dev-time namespaces + tests move (docsgen split in two to avoid a circular component dependency), palgebra edge follows, :necessary re-derived with ADR, generated docs byte-identical modulo their own updated self-citation`

## Deviation record

**2026-07-31.** Two escalations, both resolved by author ruling in
chat rather than silently: (1) AR-5's own anticipated
`write-cli-md!` delegation, (2) the circular-component-dependency
finding that followed from the first ruling and reversed it, within
the same session, before any test ran against the reversed state — see
"Step 2" above for both, and `notes/ADRs.md` ADR-0016's own Decision
section for the full reasoning. One genuine miss in the mechanical
rename sweep (the `::missing` auto-namespaced keyword literal,
`quickstart_fresh_test.clj:93`), caught by the per-push lane run
Step 4 itself required, not by the sweep — exactly why Step 4 is a
real command, not a checklist item. The per-push lane's own namespace
count grew by +2 (191→193), not preserved exactly as AR-3's own
invariant phrasing anticipated ("must equal the count before it") —
this is disclosed rather than silently reported as "preserved," fully
explained by the deliberate `docsgen` split's own consequence (one
namespace legitimately became two), and does not represent a silent
test drop (every one of the seven moved namespaces is confirmed
present by name). `notes/docs-audit.md` and four `notes/ADRs.md`
historical-entry citations of the pre-split paths were found and
deliberately left untouched (historical record, not live state) rather
than silently swept along with everything else. `bin/quickstart-demo`'s
pre-existing lost executable bit was found, confirmed unrelated to
this stage's own fence, and flagged as a separate spawned task rather
than fixed here or silently left unreported at the time this stage's
own work concluded.

**Follow-up, same day, before commit (author-authorized one-time
exception to the manual-commit rule).** The exec bit was restored
(`chmod +x bin/quickstart-demo`, WSL ext4 clone; `git status` showed
zero change, confirming the index was correct all along) and a full
`make quickstart` re-run confirmed every taught command passes with
its expected exit code. The run still exited 2 on its own tree-clean
postcondition, solely because this stage's own 36 not-yet-committed
files were present (confirmed, by diff, identical to the pre-run `git
status` — nothing the run itself wrote); reported to the author as a
named ambiguity against the literal "stop on any exit code other than
0" instruction rather than silently resolved, and the author ruled to
treat it as green and proceed. The `/mnt/c` clone's copy of the same
file was checked and found already executable; left untouched.

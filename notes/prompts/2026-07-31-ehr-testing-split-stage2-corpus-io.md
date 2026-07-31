# 2026-07-31 — ehr-testing-tools: split stage 2 — extract `corpus-io`

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `039805e` ("docs:
archive split stage 1 prompt with deviation record"), already equal
to `origin/main` — no fast-forward needed. No commit or push run by
this session; the tree is left uncommitted, coherent, with the
proposed commit message printed below. `/mnt/c` clone not touched
(all edits made via the UNC path onto the WSL ext4 clone, per the
dual-clone-edit-hazard discipline).

## Original prompt (verbatim)

2026-07-31 — ehr-testing-tools: split stage 2 — extract `corpus-io`

Context
Second of three ruled extraction stages (author ruling 2026-07-31: names blessed, `tools` retires after repoint). Stage 2 moves the transport/IO seam of the corpus cluster — sources, sinks, spooling, framing, wire codecs — into a new `corpus-io` component, leaving the corpus domain (intake, mutate, operators, generators, manifests, golden comparison) in `tools` for stage 3 to narrow into `corpus`. Method is stage 1's proven cycle (see `notes/prompts/2026-07-31-ehr-testing-split-stage1-docs-tooling.md` for the precedent, including how its two escalations were handled): characterize → extract → verify byte-identical → `poly check` green. Move, don't improve.
Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `039805e`), record the HEAD sha. Do not commit or push; leave the tree coherent, print the proposed commit message, run the per-push lane between major steps, exit codes checked directly. `/mnt/c` untouched.

Read first
1. `notes/2026-07-30-refactoring-review.md` §5.1a stage 2; `notes/ADRs.md` ADR-0011 and ADR-0016 (stage 1's record — the dependency-direction facts it established bind you).
2. Every file in `components/tools/src/ehrt/tools/corpus/` (17 files) plus `components/tools/src/ehrt/tools/interface.clj` — build the require-edge map before deciding anything.
3. `components/docs-tooling/src/ehrt/docs_tooling/lint.clj` and the lookup exports stage 1 added to `tools/interface.clj` — some of what they reach may move in this stage.
4. `bases/cli/src/ehrt/cli/core.clj` — which subcommands touch the IO seam directly.
5. `workspace.edn`, root `deps.edn`, project `deps.edn`s.

Author rulings

* AR-1 Component: `corpus-io`, namespaces `ehrt.corpus-io.*`, thin `interface.clj`. Seam assignment, default (from the file survey — verify each against real require edges, not filenames): moves — `framing`, `er7`, `spool`, `spool_source`, `source_sink`, `source_sink_url`, `sink_write`; stays for stage 3 — `intake`, `mutate`, `operators`, `generators`, `generate`, `golden_comparison`, `manifest`, `operation_manifest`. Two files are known boundary cases, handled by AR-2/AR-3.
* AR-2 Dependency direction is the ruling that matters: protocols and IO abstractions live in `corpus-io`; the domain implements or consumes them. The permanent arrow is `corpus(-to-be) → corpus-io`. `corpus-io` must never require `ehrt.tools.*`, `ehrt.docs-tooling.*`, or any judge component — if a candidate file can't move without such an edge, it stays behind and you record why. Consequences: `generator_source` (a domain-backed source implementation) stays in `tools`, implementing `corpus-io`'s source protocol from the domain side.
* AR-3 `canonicalizers` and `er7` get an edge-based decision, escalate-style. Default: `er7` moves (wire codec, IO); `canonicalizers` moves only if its real require set is kernel-and-below. If either default conflicts with the edge map you build, stop and present the conflict with the edges named — stage 1's escalation pattern, one question, then proceed on the answer.
* AR-4 Repoint forward, not through `tools`. Consumers of moved entries repoint to `ehrt.corpus-io.interface` directly: domain namespaces in `tools`, `docs-tooling`'s lint (its stage-1 lookup exports through `tools/interface.clj` should be re-examined — delete any that now merely relay to `corpus-io` and repoint lint directly, per the retire-after-repoint end-state), and `bases/cli` where it touches the seam. `tools/ interface.clj` keeps no delegating re-exports of moved entries unless a consumer you cannot repoint this stage genuinely needs one — record any such remainder as stage-3 debt.
* AR-5 Tests follow their namespaces; the per-push lane must retain every moved test under its new name; namespace count before/after recorded and any delta explained line-by-line (stage 1's 191→193 correction is the precedent for honesty here). `corpus-io` joins the same projects `tools` composes into (it is runtime, unlike `docs-tooling` — verify against each project's actual needs rather than copying blindly).
* AR-6 Move, don't improve; test-first for anything new; fence: no domain-namespace changes beyond require repoints; no CLI behavior changes (byte-identical output on the baseline commands); `:necessary` re-derived again after the move with the ADR note updated (ADR-0016's comment block in `workspace.edn` is the format).

Steps

1. Characterize. Require-edge map for all 17 corpus files + every external consumer of the move list (tools domain, docs-tooling, cli, tests, Makefile/CI). Baseline captures: per-push lane count and test list; byte captures of representative CLI runs exercising the seam — at minimum one `corpus generate` (spool/sink path), one `corpus intake` from a directory source, one `corpus mutate`, one `gate v2` over a generated corpus, plus `ehr corpus operators` output; generated-doc shas (should be untouched this stage — pin that). Resolve AR-3 from the map (escalate only on conflict).
2. Extract per AR-1/AR-2/AR-4: move files and tests, write `corpus-io/interface.clj`, update `deps.edn`s (third-party libs follow their namespaces), repoint all consumers, delete relay exports per AR-4, update `workspace.edn` per AR-6.
3. Verify: `poly check` OK (expect the structure-currency test red until AGENTS.md / architecture.md gain `corpus-io` — that red is the gate working; capture it, then fix); per-push lane green with count accounting; baseline CLI runs byte-identical; generated docs unchanged; integration lane once.
4. Records: ADR-0017 (stage 2: seam assignment as actually executed, AR-3 outcomes, the `generator_source` direction note, `:necessary` re-derivation, stage-3 debt list); AGENTS.md + architecture.md; facts-register row with Index, same commit; archive this prompt at `notes/prompts/2026-07-31-ehr-testing-split-stage2-corpus-io.md` with deviation record.

Proposed commit message: `refactor: extract corpus-io from tools (split stage 2, ruled 2026-07-31) -- transport seam moves (sources/sinks/spool/framing/er7), domain implements the protocols, consumers repointed forward, :necessary re-derived; CLI byte-identical on seam baselines`
Close-out summary for the author
HEAD at start; the edge map (or a pointer to it in the archive); AR-3 outcomes; move list as executed vs. AR-1's default, with reasons for any difference; namespace count accounting; the structure-test red moment; stage-3 debt list (relay exports kept, if any, plus the running naming notes for the future `corpus` interface); anything AR-6 stopped you from improving.

## What landed

Full decision record, verification evidence, and deviation record all
live in `notes/ADRs.md` ADR-0017 — this file is the close-out summary
the prompt's own "Close-out" section asked for, not a duplicate of
that record.

**HEAD at start:** `039805e` = `origin/main`, tree clean.

**The edge map:** built by reading every one of the 17
`components/tools/src/ehrt/tools/corpus/` files' own `:require`
blocks (internal cross-file edges), then grepping the whole repo for
external requires of each candidate namespace (`ehrt.tools.interface`,
`docs-tooling/lint.clj`, `bases/cli/src/ehrt/cli/core.clj`, and —
found only later, by the integration lane, not by this initial map —
`projects/integration/test`'s own alias-indirected requires). Not
committed as a standalone document; ADR-0017's own Decision section
carries the map's conclusions (the internal edge table, the
`interface.clj` re-export inventory) inline rather than as a separate
artifact.

**AR-3 outcomes:** both resolved by default, no conflict. `er7`
requires kernel only — moved. `canonicalizers` requires kernel only
(its own docstring's citation of a `ehrt.tools.canonical` registry was
already stale — the real registry, `kernel/register!`, moved to
kernel at ADR-0008 — corrected in the same pass) — moved.

**Move list as executed vs. AR-1's default:** matches AR-1's default
list exactly for seven of nine namespaces. Two deviations, both
escalated to the author before any file moved (AskUserQuestion, both
resolved by the author choosing the recommended option) — full
reasoning in ADR-0017's Decision section:
1. `source-sink`'s own `generator-source` constructor (a real edge
   into `ehrt.tools.corpus.generators`) and `source-sink-url`'s
   `finish-source`/`parse-source-designator` (the only caller of that
   constructor) stayed in `ehrt.tools.corpus.generator-source`
   instead of moving whole with their old namespaces — `source-sink`
   and `source-sink-url` each moved *minus* this one piece, split
   along `parse-designator`'s own pre-existing `finish`-callback seam.
2. `operation-manifest` moved to corpus-io despite AR-1's literal
   default (which listed it as staying) — it has zero domain edges,
   and its most demanding consumer (`sink-write`) moved anyway.

**Namespace count accounting:** 193 before, 193 after — not merely
equal by coincidence. The full sorted `Testing ehrt.*` occurrence list
diffs to exactly the nine moved namespaces' two occurrences each
(conformance + ehrt-cli) renamed `ehrt.tools.corpus.* →
ehrt.corpus-io.*`; nothing else in the list changed. No `docsgen`-style
split happened this stage (no namespace became two, none merged) —
the clean 193→193 result is a genuine outcome of this stage's own
moves, not stage 1's kind of correction.

**The structure-currency red moment:** confirmed, same mechanism
stage 1 exercised. Per-push lane ran with `components/corpus-io`
already on disk, before `AGENTS.md`/`docs/dev/architecture.md` were
touched — `ehrt.docs-tooling.structure-currency-test` FAILED
correctly, naming `corpus-io` as missing from
`docs/dev/architecture.md`'s bricks table. Both docs updated; the
lane's next run was green.

**Stage-3 debt list:**
- `components/tools`'s own interface width is down nine defs from the
  moved namespaces but still wide (the domain surface: `corpus.*`,
  `check`, `sim`) — narrowing it further is stage 3's own job,
  untouched here per AR-6's fence.
- `ehrt.tools.corpus.generator-source` now carries three distinct
  concerns (execute-and-wrap `resolve!`, validate-and-shape
  `generator-source`, URL-parsing `parse-source-designator`) that
  share the same domain edge but weren't designed as one file's
  cohesion — worth revisiting if stage 3 (or a future session) finds
  a cleaner split; not attempted here (move, don't improve).
- `docs/formats.md` and `docs/locators.md` (user-path docs) cite the
  pre-move `ehrt.tools.corpus.operation-manifest`/`.er7` paths,
  including one now-broken relative link in `locators.md` — found,
  not fixed, per ADR-0011's own precedent for user-path docs outside
  a stage's declared fence; folds into the P1-1 errata sweep the
  2026-07-30 review already named.
- No relay exports were kept in `ehrt.tools.interface` for any moved
  entry — every real consumer this stage found could be repointed
  directly to `ehrt.corpus-io.interface`, so there is no stage-3
  "delete this relay" debt of the kind AR-4 anticipated.

**What AR-6 stopped from being improved:** the `generator-source.clj`
three-concerns question above is the main one — the honest move was
to relocate the domain-touching code as-is, not to redesign its home
while doing so. The duplicated pure URL-grammar helpers already
living entirely inside corpus-io (unaffected by this move) were left
alone too, out of scope.

## Proposed commit message

`refactor: extract corpus-io from tools (split stage 2, ruled 2026-07-31) -- transport seam moves (sources/sinks/spool/framing/er7), domain implements the protocols, consumers repointed forward, :necessary re-derived; CLI byte-identical on seam baselines`

## Deviation record

**2026-07-31.** Two escalations, both resolved by author ruling via
AskUserQuestion before any file moved (see ADR-0017's own Decision
section for the full edge-map reasoning behind each): (1) the
`source-sink`/`source-sink-url` split along the `parse-designator`
callback seam, to keep `generator-source`'s domain edge out of
corpus-io; (2) moving `operation-manifest` to corpus-io despite AR-1's
literal per-filename default. A third, unescalated deviation from the
naive default: none beyond the two above — every other file matched
AR-1's list exactly.

Three mechanical-rename misses, all caught only by running the
per-push or integration lane (per stage 1's own "Step 4 is a real
command, not a checklist item" lesson), not by the rename sweep
itself: `er7_test.clj`/`framing_test.clj` (both moved to corpus-io)
had their `ehrt.tools.corpus.simhospital-corpus` require — a test
fixture helper never in scope to move — incorrectly swept to
`ehrt.corpus-io.simhospital-corpus` by the mechanical
`ehrt.tools.corpus. → ehrt.corpus-io.` sed (it matched the same
prefix); `mutate_test.clj` (stayed in tools) kept a stale
`ehrt.tools.corpus.er7` require after `mutate.clj` itself was
repointed; `projects/integration/test/ehrt/tools/intake_source_golden_test.clj`
aliased `ehrt.tools.interface` as `source-sink` and called
`source-sink/dir-source` — invisible to Step 1's own require-edge
map, which searched for direct `ehrt.tools.corpus.X` requires, not
alias-indirected ones. All three fixed; a repo-wide grep afterward for
every moved function name across every test tree found no further
instances.

`integration`'s own `:necessary` re-derivation is reported as an
empirical `poly check` result (zero warnings with the entry cleared,
re-confirmed three times under different `project:` filters), not as
a graph-traced proof of exactly which new edge changed its
reachability — `corpus-io` itself has no edge into `tools` (the
directional rule this whole stage enforces), so the mechanism is
disclosed as observed rather than fully explained. See ADR-0017's own
deviation record for the full statement.

Two user-path docs (`docs/formats.md`, `docs/locators.md`) and one
component-adjacent historical experiment-results doc
(`components/tools/docs/experiments/EXP-A4-results.md`) were found
citing pre-move paths and deliberately left untouched, per ADR-0011's
own precedent — named in ADR-0017's own deviation record rather than
silently fixed or silently ignored.

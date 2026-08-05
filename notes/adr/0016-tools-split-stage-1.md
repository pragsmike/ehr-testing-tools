<!-- Attic file: notes/adr/0016-tools-split-stage-1.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0016 — `tools` split stage 1: `docs-tooling` extracted, `:necessary` re-derived

**Status:** Accepted (author-ruled 2026-07-31, session executed same day).

### Context

`notes/2026-07-30-refactoring-review.md` §5.1(a) proposed a staged
split of `components/tools` — a corpus domain component and the CLI's
universal façade wearing one name — into three independently-landable
extractions, using the method ADR-0011 already proved
(characterize → extract → verify byte-identical behavior → `poly
check` green). This record executes stage 1, the smallest and
safest: `docsgen`, `usecases`, `pipeline`, `quickstart-fresh`, `lint` —
dev-time-only doc/lint tooling, named by the review as "the sole
source of the `tools → palgebra` src edge" and the thing finding 14's
undocumented third `:necessary` entry (`integration`) hangs on.
Stages 2 (`corpus-io`) and 3 (narrow `tools` to its domain) are ruled
but not executed here.

Session start: the WSL ext4 clone was already at `origin/main`
(`9f7f57c`, no fast-forward needed). The 2026-07-31 "ruled P2 batch"
session (judge-family parity, NIST artifact channel, verdict-cache
note) had already landed as that same commit, `P2 batch.` — this
session builds on top of it, not around it.

### Decision

**Landing shape**, per author ruling on names (docs-tooling,
`ehrt.docs-tooling.*`) and on `tools` retiring after repoint rather
than surviving as a permanent façade:

```
components/docs-tooling/  -- src/ehrt/docs_tooling/{docsgen,usecases,
                             pipeline,quickstart_fresh,lint,interface}.clj,
                             test/ehrt/docs_tooling/{same + stale_path,
                             structure_currency}_test.clj, deps.edn
                             ({:deps {metosin/malli ...}})
components/tools/         -- loses the five namespaces; gains
                             operators_doc.clj (see split, below) and
                             operators_doc_test.clj
```

**Two escalations fired during Step 2, both resolved by author
decision, both documented here since AR-5 explicitly asked for
"stop and name the def" rather than silent resolution:**

1. **`ehrt.tools.interface`'s `write-cli-md!` genuinely delegates to
   `docsgen`** (AR-5's own anticipated check) — `bases/cli/help.clj`
   is a real, live caller, not a grep false positive. First ruling:
   keep the re-export, now delegating to
   `ehrt.docs-tooling.interface`.
2. **That ruling, combined with `docsgen.clj`'s own real dependency on
   `corpus.operators` (the operators.md-rendering half) and
   `lint.clj`'s real dependency on `corpus.canonicalizers`/
   `corpus.framing`/`corpus.operators`/`check.schemas`, produced a
   genuine circular *component* dependency** (`tools → docs-tooling →
   tools`) — `clojure -M:poly check` Error 104, a hard Polylith
   constraint (two bricks may not depend on each other, regardless of
   which specific namespaces create each edge), not a style
   preference discoverable by inspection alone. Second ruling (this
   is the one that landed): `bases/cli/help.clj` calls
   `ehrt.docs-tooling.interface/write-cli-md!` **directly**, bypassing
   `tools` entirely; `ehrt.tools.interface` drops `write-cli-md!`
   altogether. `docsgen.clj` itself split in two along its own
   pre-existing internal seam (it already had zero shared state
   between its cli.md half and its operators.md half, just shared
   pure helper functions): `render-cli-md`/`write-cli-md!` (pure, no
   tools dependency) moved whole to
   `ehrt.docs-tooling.docsgen`; `sorted-entries`/`render-operators-md`/
   `write-operators-md!` (needs the live `corpus.operators` registry)
   stayed in `components/tools`, renamed `ehrt.tools.operators-doc`.
   Both halves carry their own duplicate copy of the four small pure
   markdown-table helpers (`banner`/`escape-cell`/`table`/
   `exit-code-table`) the original file shared between them —
   duplicated rather than shared through an interface, since sharing
   them would reintroduce the same class of coupling the split exists
   to avoid, for four small pure functions. `lint.clj` keeps its own
   real need for tools' registries, routed through
   `ehrt.tools.interface` (three new exports: `check-schemas-lookup`,
   `framing-lookup`; `lookup`/`operators/entries` already existed) —
   this edge runs one direction only (`docs-tooling → tools`) and
   never touches `write-cli-md!`'s own removed edge, so it doesn't
   reintroduce the cycle.

**Test placement (AR-3).** All seven moved test files
(`docsgen_test` split the same way as its namespace; `usecases_test`,
`pipeline_test`, `quickstart_fresh_test`, `lint_test`, and the two
2026-07-31 doc-enforcement additions `stale_path_test`/
`structure_currency_test`, which carry no code dependency on any of
the five namespaces but move with their own docs-enforcement
machinery per this ruling) now live under
`components/docs-tooling/test/`. Default placement:
`projects/conformance` gains a real `poly/docs-tooling` dependency —
the only project that hosts docs-tooling's own tests. `bases/cli`
gains a real, direct dependency on `docs-tooling` (for
`write-cli-md!`); `projects/ehrt-cli` gains `poly/docs-tooling`
correspondingly. `projects/integration` does **not** gain
docs-tooling — nothing there needs it, and (see below) it lost its
own reason to need `palgebra` at all.

**`:necessary` re-derivation (AR-4, finding 14).** Method: `clojure
-M:poly deps` for the real brick-edge matrix, then `clojure -M:poly
check` with every `:necessary` entry in `workspace.edn` temporarily
cleared to `[]`, once, to see exactly what poly's own brick-reachability
check (real edges from a project's base, or between bricks the same
project declares — it does not see a project's own ad hoc test-tree
requires) considers unreachable without the override:

| Project | Before | After | Why |
|---|---|---|---|
| `ehrt-cli` | `["palgebra"]` | *(no `:necessary` key)* | `bases/cli` now directly requires both `ehrt.tools.interface` and `ehrt.docs-tooling.interface`; every other declared brick, palgebra included, is real-edge-reachable transitively through those two (confirmed: zero warnings with `:necessary []`). |
| `conformance` | `["tools" "palgebra"]` | `["docs-tooling"]` | `tools` and `palgebra` are now reachable via docs-tooling's own real edges (`docs-tooling → tools`, `docs-tooling → palgebra`) once docs-tooling itself is in the project; docs-tooling itself has no real incoming edge from anything else conformance declares (it's included solely to host its own moved tests), so it alone needs the override. |
| `integration` | `["tools" "palgebra"]` | `["tools"]` | `palgebra` dropped from the project's `:deps` entirely, not just the override — grep-confirmed nothing under `projects/integration/test/` requires `ehrt.palgebra`, and `tools` no longer requires it either (the edge moved to docs-tooling, which integration deliberately does not include). `tools` itself still needs the override: nothing else integration declares has a real edge to it, only its own test tree's direct requires (poly-invisible to this check, real at runtime). |

Confirmed: `clojure -M:poly check` — `OK`, zero warnings, with the
table above as the final `workspace.edn` state.

### Verification

Characterization baseline (before any Step 2 edit, HEAD `9f7f57c`):
fresh `make docsgen` exit 0, generated docs byte-identical to
committed (sha256 recorded); `make quickstart-fresh` exit 0 (15
commands agree); `make lint-pipeline` exit 0; per-push lane
(`skip:integration`) exit 0, 191 `Testing ehrt.*` namespaces, 0
failures/errors, all seven moving test namespaces present (each
running twice — once per composing project, `ehrt-cli` and
`conformance`). `make quickstart` exits 2 (`bin/quickstart-demo`
permission denied) — a **pre-existing, unrelated** exec-bit drop on
disk (git's index still says mode 100755; the working-tree file was
644), confirmed before any stage-1 edit and flagged separately
(spawned task, not fixed here — outside this stage's own fence).

Post-move verification, same commands: fresh `make docsgen` exit 0;
`docs/dev/pipeline.md` and `docs/use-cases.md` byte-identical to
baseline; `docs/operators.md` and `docs/cli.md` each differ by
exactly the renderer's own "GENERATED... edit this file instead"
banner line, now correctly citing the renderer's new true path — the
deliberate, correct consequence of the move itself, not drift (every
other line, byte-identical). `make quickstart-fresh`/`make
lint-pipeline`: same output modulo the invoked namespace's own name in
the echoed command line, same `OK` message, exit 0. `clojure -M:poly
check`: `OK`. Per-push lane: exit 0, **193** `Testing ehrt.*`
namespaces (191 baseline **+2**, exactly and only from the `docsgen`
split: `ehrt.docs-tooling.docsgen-test` and
`ehrt.tools.operators-doc-test` are two real namespaces where one
(`ehrt.tools.docsgen-test`) stood before, each running in the same two
composing projects — `ehrt-cli` and `conformance` — every other
existing namespace already ran in, so 1 namespace → 2 namespaces × 2
projects = a net +2 total occurrences, confirmed by grep count, not
inferred), 0 failures/0 errors, every moved test namespace present
under its new `ehrt.docs-tooling.*` name (each of the seven appearing
twice, once per composing project — same duplication pattern the
baseline already had for the un-split six).

**Follow-up, same day, before commit: `make quickstart` fixed and
verified green.** The exec-bit drop named above was pre-existing and
outside this stage's own fence, so it was left unfixed through Step 4;
the author separately authorized fixing it and committing this stage
in the same follow-up. `chmod +x bin/quickstart-demo` on the WSL ext4
clone restored the bit; `git status` showed zero change from this
(the index already recorded mode 100755 — it was always correct, only
the working-tree file had drifted). The `/mnt/c` clone's own copy was
checked and found already executable (drvfs reports it `-rwxrwxrwx`);
untouched. A full `make quickstart` re-run afterward: every taught
command passed with its expected exit code (`help`; both `corpus
generate` runs; all three `artifact fetch`es; `corpus mutate`; `gate
v2`; `gate fhir`, correctly rejecting, exit 1; `check`, 7/7 pass; `sim
run`; the closing `clojure -M:poly test :all`, 4m17s) — genuinely
green, functionally. The script's own final postcondition (`git
status --porcelain` must be empty) still exited the make target at 2,
because this stage's own not-yet-committed 36 files were sitting in
the tree at the time — confirmed, by diff, byte-identical to the
pre-run `git status`, so nothing the run itself wrote; a limitation of
running this postcondition against an intentionally uncommitted stage,
not a defect this stage introduced. Recorded here rather than
silently reclassified as unconditionally green.

### Deviation record

**AR-5's own anticipated escalation fired for real, and a second,
unanticipated one followed from it** — both resolved by author
ruling, both above (Decision, "Two escalations"). The first ruling
(keep `tools`'s re-export) was reversed by the second finding
(`poly check` Error 104) within the same session, before any test
ran against it — recorded as a genuine correction, not silently
dropped.

**A stale fully-qualified keyword literal, caught only by running the
suite, not by the rename sweep itself.**
`quickstart_fresh_test.clj:93` asserted
`(= :ehrt.tools.quickstart-fresh/missing (:script divergence))` — the
source's own `::missing` (an auto-namespaced keyword) resolves to
whatever namespace it's read in, so renaming `ehrt.tools.quickstart-fresh`
to `ehrt.docs-tooling.quickstart-fresh` silently changed the keyword
the test needed to assert against. `ns`/`:require` renames were swept
mechanically across all twelve moved files; this one hardcoded,
fully-qualified keyword literal in a test body was not caught by that
sweep — only by actually running the per-push lane, which is why
Step 4's own verification pass exists as a real command, not a
checklist item. Fixed to
`:ehrt.docs-tooling.quickstart-fresh/missing`; the same and one other
file's own docstring comments (`framing.clj:224,233`,
`usecases.clj:3`) citing the old qualified names by name were swept
in the same pass, found by a repo-wide grep after the fact rather
than anticipated in advance.

**`components/palgebra/test/ehrt/palgebra/deps_lint_test.clj`'s own
seeded-violation fixture string, `"ehrt.tools.lint"`, left
untouched.** It is arbitrary realistic-looking test data proving
palgebra's own `deps-lint` rule fires on a fake namespace string, not
a real citation of the namespace this session moved — renaming it
would not change what it tests and was outside AR-2's own "move,
don't improve" mandate.

**`notes/docs-audit.md`, a closed one-time disposition audit from an
earlier session, left untouched** despite citing the pre-split
`docsgen.clj`/`.usecases`/`.pipeline` paths — it is historical record
of a Phase-4 execution already completed, not a live "current state"
document this session's own read-first list named, and AGENTS.md's
own discipline-surface mapping does not list it among the "live,
current, edit freely" set. Named here per fix-forward-with-disclosure
rather than silently skipped.

**Named-future list, for stages 2/3 (not executed here):** the
duplicated pure markdown-table helpers between
`ehrt.tools.operators-doc` and `ehrt.docs-tooling.docsgen` (four small
functions, ~30 lines each copy) are a candidate for a shared
micro-namespace if a third consumer ever needs them, not before —
premature to extract for two. `components/tools`'s own interface
width (74 defs, unchanged by this stage) is untouched, per stage 1's
own fence. Stage 2 (`corpus-io`: source-sink/framing/player) and stage
3 (narrowing `tools` to its domain, retiring the façade) remain ruled,
unexecuted.

---


# Charter — `docs-tooling`

> **Draft for the author's edit.** Derived from
> `src/ehrt/docs_tooling/interface.clj`, the thirteen sibling
> namespaces in its `src`, its test tree, and the ADRs those
> docstrings cite. **UNCLEAR** marks a contract the shipped surface
> does not settle.

## 1. Mission

Own the repository's **documentation machinery**: generate the docs
that are derived from the tree, and enforce the gates that keep the
hand-written ones honest.

Extracted at the docs-tooling extraction, 2026-07-31 (refactoring
review stage 1).

## 2. Interface contract

**Thin, deliberately narrow** (AR-1): the interface re-exports exactly
what a **sibling brick** calls from outside this component's own
namespaces — **one entry.**

- `write-cli-md!` — renders the CLI reference document.
  `bases/cli/help.clj`'s own `write-cli-md!` wrapper is the one real,
  live caller — not a grep false positive — and it calls this
  **directly, never through the retired `tools` façade**. ADR-0016
  records the circular-dependency finding that ruled a relay out.

**Everything else this component does is invoked without an
interface**, and that is a deliberate, reasoned position rather than an
omission. `write-equations-txt!`, `write-pipeline-md!`,
`write-case-equations!`, `write-use-cases-md!`, `quickstart-fresh!`
and `lint-pipeline!` are invoked directly by the Makefile via `-X`,
never `:require`d cross-brick — and **`-X` addresses a namespace on
the classpath directly, regardless of which brick it lives in.**
Polylith's interface-boundary enforcement (`poly check`) applies to
compile-time `:require`s, not to `-X` invocations. So none of them
needs an export, and giving them one would widen a seam for no caller.

## 3. Data shapes owned

This brick owns few *data* shapes and many **rules**. Its authority is
over:

- **`.agents/state-derived.md`** and the `inputs` definition that
  produces it — one definition that `collect` reads through, **so the
  list and the reads cannot drift** (ADR-0158, register row L3-3).
- The **generated documents**: the CLI reference, the pipeline page,
  the use-cases index and its per-case pages, the equations files.
- The **citation contract** enforced by `citation_gate.clj`: a
  citation must resolve, anchored by **stable text and never a line
  number**, and must anchor **exactly one place**.
- The **exercised-sources** and **trace-capture** records — which
  documentation claims are backed by a real run.

## 4. Invariants guaranteed

These are the gates. Each is a promise about the tree, not about a
function's return value.

- **Generated docs are derived, not maintained.** `make docsgen`
  regenerates them; a hand edit is lost, which is the point.
- **Citations resolve, anchor stable text, and anchor exactly once.**
  The last clause is ADR-0162's own correction: a citation that
  matches wherever it is pasted anchors nothing.
- **Every index is complete.** `index_completeness_test` fails the
  build on a missing *or ghost* entry, which is why a session record
  and its archived prompt must land indexed in the same commit.
- **Every required directory has a README** — `.agents/` and `notes/`
  subdirectories, minus the frozen-provenance exemptions
  (`notes/sim`, `notes/tools`, ruling 6).
- **No live doc surface teaches a retired invocation**, and a fence's
  own path arguments must **resolve**, not merely parse (AR-U2-R).
- **Hand-owned assets are re-reviewed when their source moves** —
  a gate that reads **git history**, and therefore **cannot be
  exercised by a pre-commit run**: at Step 1 the suite sees an
  uncommitted tree, `git log` returns the old sha, and the gate is
  blind by construction. The remedy is **ordering**: run
  history-reading gates *after* the commit.

## 5. Non-goals

- **Not a runtime capability.** Nothing here ships in a user-facing
  command except the CLI reference it renders.
- **Not a general linter.** `lint-pipeline!` checks pipelines against
  `palgebra`'s signatures; it is not a code-style tool.
- **Owns no domain.** It reads `corpus`'s registries and `palgebra`'s
  grammar to verify documents; it defines neither.
- **Does not widen its seam for `-X` entry points**, on the reasoning
  in §2.

## 6. Forbidden edges

Requires `corpus`, `corpus-io`, `kernel` and `palgebra` in `src` — it
reads the registries whose contents it verifies.

Must never require:

- **`bases/cli`** — bases depend on components, never the reverse.
  This is the live constraint, not a hypothetical: `bases/cli` calls
  `write-cli-md!`, and **ADR-0016 ruled out a relay in the other
  direction precisely because it would close a circular dependency.**
- **`sim`** and every simulator brick, **`judge`** and every judge
  engine, **`oracle`**, **`provenance`** — none is required today, and
  each would give the documentation machinery a stake in a domain it
  is supposed to describe from outside.

## UNCLEAR — the author's review queue

- **UNCLEAR-DT1 — the brick's real surface is thirteen namespaces and
  a test tree, and its interface is one var.** By `poly check`'s
  rules that is exactly right, and §2's reasoning is sound. But it
  means the *stated* contract of this brick — one function that writes
  one file — describes almost none of what it does or guarantees. An
  agent reasoning from `interface.clj` alone would badly under-read
  it. Two readings: *(a)* correct and nothing to do — the gates are
  tests, and tests are not a public API; *(b)* this brick's contract
  genuinely lives in its **test namespaces**, and something should say
  so where a reader will look. This charter's §4 is an attempt at
  (b); whether that is the right home is the author's call.
- **UNCLEAR-DT2 — where a charter gate would live, if one is
  wanted.** `bin/charter-completeness` (this session) is a shell
  script, not a test, deliberately: the session's fence was docs-only
  and a new test file is code. The two existing charter gates —
  `patient-simulator-charter-test` and
  `person-simulator-charter-test` — live **here**, in this brick.
  If charter completeness should be enforced by `make test` rather
  than by a script someone remembers to run, this is the brick that
  would own it, and it would cost a new test namespace (which runs
  twice, under development and `projects/conformance`) plus a
  `state-derived` regeneration.

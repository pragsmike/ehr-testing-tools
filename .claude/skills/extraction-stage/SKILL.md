---
name: extraction-stage
description: >
  Split a Polylith brick into smaller ones (or extract an engine/module
  into its own component) with zero behavior change, proven byte-for-
  byte rather than asserted. Use when a session's own goal is to move
  code between components/bases without changing what it does — a
  census-driven split, an engine extraction, a façade retirement. Do not
  use this for changes that also alter behavior; that needs its own
  design, not this checklist.
license: MIT
compatibility:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - polylith
    - refactoring
    - session-mechanics
  tested-tools:
    - claude-code
---

# Extraction Stage

Encodes the characterize → extract → verify → records discipline this
workspace's split-stage sessions have used repeatedly: the original
judge extraction (`notes/ADRs.md` ADR-0008), the judge-v2-hapi/judge-
fhir-official split (ADR-0011, worked example:
`.agents/session-records/2026-07-29-judge-engine-extraction.md`), and
the three-stage `tools` split into `docs-tooling`/`corpus-io`/`corpus`
(ADR-0016, ADR-0017, ADR-0018).

## Use this skill when

- A session's stated goal is splitting one brick into more, or moving a
  component's content to a new name/location, with **no behavior
  change**.

## Do not use this skill when

- The change also alters what the code does (a "sanctioned improvement"
  beyond the split itself needs its own explicit naming — see step 3 —
  not blanket cover from this skill).

## Procedure

1. **Characterize before extracting.** Census every def/namespace of the
   brick being split; classify each by its *real* consumer, found by
   grep, not by judgment about what "should" be reusable. ADR-0018's
   strong form: all 64 `ehrt.tools.interface` defs were classified by
   live consumer before any interface-shape decision was made.
2. **Capture a byte-identity baseline before moving anything.** Record
   exact CLI outputs, `--report` EDN files, and stdout logs for the
   commands the extraction must not change. Worked example: the judge
   extraction's characterization commit recorded three `--report` files
   and three stdout logs *before* the move; the post-move diff against
   that baseline was zero on all six.
3. **Move-don't-improve, with named futures — except one sanctioned
   improvement, if any, named explicitly.** Default disposition is
   "move content unchanged." If real evidence supports one deliberate
   improvement during the move (ADR-0018's from-live-consumers interface
   redesign was the one sanctioned exception across three split stages),
   name it as *the* exception in the ADR's own Decision section — don't
   let "while I'm in here" improvements happen silently. Anything else
   worth improving is a named future, not done now.
4. **Re-derive `:necessary`, don't carry it forward.** Each stage
   re-derives its own dependency/`:necessary` set from what the
   post-move code actually requires (ADR-0016/ADR-0017's pattern), not
   from the pre-move brick's own list — a brick's real dependencies
   change shape when it shrinks.
5. **Expect `poly check` to find what a census can't.** A census shows
   what a namespace requires; it can't show whether the call site inside
   it actually uses what it requires. Real Polylith interface violations
   (Error 104, illegal component-to-component edges) surface only after
   the move — resolve each one on its own merits (dissolve into a shared
   interface if the call is genuine and load-bearing; delete outright if
   it turns out to be dead code) and document the resolution in the
   ADR's own deviation record.
6. **Account for every reference, one-to-one, when sweeping citations.**
   When a rename or move touches many files (docstrings, path strings,
   fixture citations), tally every hit found, classified, and disposed
   — not a sample. Every load-bearing path string across the workspace
   (`.gitattributes`, `Makefile`, test fixtures, help text) gets
   repointed in the same change (ADR-0018's own worked accounting).
7. **Red-evidence-first for any gate landing alongside the split.** If
   the split adds or hardens an enforcement test, prove it fails before
   the fix and passes after (the `31675e6`/`1c3d77c` precedent) — never
   land a gate you haven't watched go red first.
8. **Verify without a pipe, and exit the status you captured.** `clojure
   -M:poly check` green before and after; capture `clojure -M:poly test
   :all skip:integration`'s full log and exit code directly
   (`> file 2>&1; EXITCODE=$?; ...; exit "$EXITCODE"`) — a piped
   command's exit status belongs to the pipe's last stage, not the tool
   being tested, and has produced a false-positive "pass" before (the
   sim-sibling errata session's own caught mistake). The trailing `exit`
   is not decoration and this step taught the idiom without it until
   ADR-0155: a block that ends by ECHOING the status exits 0 whatever the
   tool did, which is exactly how ADR-0152's masked run reported green.

## Output

The split lands with `poly check`/`poly test` green, a byte-identical
behavior proof, a one-to-one reference-migration accounting table, and
every genuine `poly check` violation documented in the ADR's own
deviation record — not silently patched.

## Done when

- [ ] Every def/namespace was classified by real (grep-found) consumer
      before the interface was designed.
- [ ] A pre-move baseline was captured and the post-move output diffed
      against it byte-for-byte.
- [ ] Any improvement beyond the move itself is named explicitly as the
      one sanctioned exception, not folded in silently.
- [ ] Every `poly check` violation found post-move is resolved and
      documented, not worked around.
- [ ] Reference sweeps are accounted one-to-one, not sampled.
- [ ] `poly check`/`poly test` output was captured without a pipe.

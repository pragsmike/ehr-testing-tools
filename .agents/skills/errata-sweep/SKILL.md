---
name: errata-sweep
description: >
  Fix stale, contradicted, or retired claims across this repo's docs —
  a mechanism that changed underneath prose that still describes the old
  one. Use when a prompt names specific stale claims to fix (a retired
  code path, a renamed file, a superseded mechanism) across the doc
  tree. Do not use this for a first-time citation pass (that is
  provenance work, closer to capture-session) or for behavior changes.
license: MIT
compatibility:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - documentation
    - session-mechanics
  tested-tools:
    - claude-code
---

# Errata Sweep

Encodes the doc-freshness/citation-fix pattern this workspace uses when
a mechanism changed but prose describing it didn't follow. Worked
example: `.agents/session-records/2026-07-29-sim-sibling-errata-sweep.md`
(stale sibling-checkout/subprocess claims, refuted by live execution,
fixed at three named spots). The citation-vs-instruction distinction and
the tripwire-scoping pattern below are drawn from
`components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj`'s
own documented scope.

## Use this skill when

- A prompt names specific stale, contradicted, or retired claims to fix
  across `docs/`, `AGENTS.md`, or skill files.

## Do not use this skill when

- Adding first-time citations to a previously-uncited but still-correct
  claim (closer to [`capture-session`](../capture-session/SKILL.md)'s
  provenance discipline than a sweep).
- The fix would also change behavior, not just prose.

## Procedure

1. **Inventory with a grep, before and after.** Search the doc tree the
   prompt names for the retired term/path/mechanism; re-run the exact
   same grep after editing to confirm no scope growth beyond what the
   prompt named (the sim-sibling session's own before/after re-run
   found no hit beyond what the prompt already listed).
2. **Classify each hit: narration vs. instruction.** A doc that
   historically narrates a retired name in the past tense (an ADR, a
   session record, an archived prompt) is legitimate and stays untouched
   — it describes what happened, not what to do now. A doc that
   currently instructs or asserts the retired thing as still true is
   stale and gets fixed. `stale_path_test.clj`'s own scope embodies this:
   it deliberately never scans `notes/ADRs.md`, `notes/prompts/`, or
   `.agents/session-records/`, since those "narrate history and
   legitimately cite the old names."
3. **Default to deletion over rewording**, when a caveat is simply no
   longer needed (the sim-sibling session's own stated default for two
   stale `use-cases.edn` comments) — reword only when a caveat is still
   needed in some form.
4. **Verify link targets and anchors, don't assume them.** Compute an
   anchor the same way the doc's own existing anchors are computed
   (e.g. the GFM slug rule), cross-check against a working precedent in
   the same file, then re-verify with an actual link-resolution pass —
   not by eyeballing the rendered heading.
5. **Escalate genuine ambiguity; don't guess.** A hit is unambiguous only
   when it plainly does not name the retired thing (an ADR-number
   citation for an unrelated baseline, for instance) — otherwise stop
   and name it for the author rather than silently choosing a reading.
6. **Land a tripwire extension in the same commit.** Extend
   `stale_path_test.clj` (or a sibling test) to forbid the retired
   reference going forward, scoped by tense/context — not a bare token
   match, which would also flag legitimate historical narration. This is
   what stops the species recurring, not the one-time fix alone.
7. **Docs-only proof is the suite staying green, captured without a
   pipe.** `clojure -M:poly check`: `OK`. Full per-push lane output and
   exit code captured directly to a file, exit code checked separately
   — a piped capture's exit status belongs to the pipe, not the tool
   (the sim-sibling session's own caught mistake, worth not repeating).
8. **Account for every hit, one-to-one, in the session record.** A
   table: file, hit, classification (narration / stale), disposition
   (fixed / left, and why).

## Output

Every named stale claim fixed or explicitly left-with-reason; a tripwire
extension co-landed in the same commit; a one-to-one accounting table in
the session record.

## Done when

- [ ] The inventory grep was re-run after editing and came back clean.
- [ ] Every hit is classified narration-vs-instruction, not fixed by
      default.
- [ ] Ambiguous hits are named for the author, not silently resolved.
- [ ] A tripwire extension lands in the same commit as the fix.
- [ ] The accounting table covers every hit found, not a sample.

# 2026-08-25 -- arc 3a: the demographic fold, designed (ADR-0173)

Payload design session. Started at `ee573c4`, ended at the tip this
record's own close line names. Prompt archived at
`.agents/prompts/2026-08-25-arc-3a-demographic-fold-design.md`.

Two commits: the ride-alongs, then the design. No `components/*/src`
changed, as fenced.

## What landed

**Commit 1, `667d1a0`** -- the three ride-alongs the prompt asked for
first.

* ADR-0172's status block gains the `t0` clarification: `t0` is the t0
  CONTEXT map, not an instant, which is ruling C1's "as a t0 parameter"
  read literally and what `persona.clj` actually landed.
* Limitations row 12 (a parent may head more than one household), in
  both ADR-0172 section 4 and the component's own charter, with
  `a-parent-may-head-more-than-one-household-test`. The pinned witness
  is **4 multi-household heads out of 34 distinct heads over 38
  `:household-form` events** -- counted from the filter's own input, not
  from the corpus, and `pos?` asserted separately. Arc 2b's record said
  "four such persons"; this session recomputed it rather than quoting
  it, and it agrees.
* `ehrt.docs-tooling.brick-test-composition-test`, gate
  `every-brick-test-path-is-composed-into-a-project-test`. Root
  `deps.edn` aliases are deliberately not counted.
* `.agents/state-derived.md` regenerated: 195 -> 196 test namespaces,
  46 -> 47 docs-tooling gates.

**Commit 2** -- `notes/adr/0173-arc-3a-engine-folds-the-person-stream.md`,
Proposed, with five rulings A-E open, plus the roadmap's arc-3 row split
into 3a (designed) and 3b (untouched).

## Red witnesses

Both new gates are born green, so both were proved red by mutation and
restored.

`every-brick-test-path-is-composed-into-a-project-test`, with
`poly/person-simulator` removed from `projects/conformance/deps.edn`:

```
FAIL in (every-brick-test-path-is-composed-into-a-project-test)
1 brick(s) of 19 carry tests that NO project composes, so `poly test`
never runs them -- add the brick to a projects/*/deps.edn, not to a
root alias: ["person-simulator"]
```

`a-parent-may-head-more-than-one-household-test`, with the artefact
simulated as FIXED (birth-constituted households filtered out of the
gate's input):

```
FAIL in (a-parent-may-head-more-than-one-household-test)
population is non-empty (R-empty-population-is-red)
no parent heads more than one household -- either the artefact was
fixed (strike row 12) or the witness went empty
expected: (pos? (count multi))   actual: (not (pos? 0))

expected 4 multi-household heads out of 27 distinct heads over
27 :household-form events, read 0: []
```

The second message is worth reading twice: the filter's INPUT dropped
from 38 events to 27 under the mutation, and the gate says so in its own
failure text. That is the shape the two-vacuous-gates finding asked for
-- ask the filter's input size, never the corpus's.

## Where the tree and ADR-0172's census disagreed

The prompt said the census was a week old. It is one day old, and
**nothing in it moved**: `git diff --stat 41081dd..ee573c4 --
':(exclude)components/person-simulator'` touches no `src` file at all,
and all thirty-one cited line numbers were re-checked one by one and
resolve exactly. So there is no disagreement of the kind the fence
anticipated. Three REFINEMENTS the tree forced, all in ADR-0173
section 1:

1. `emit_fhir.clj:115` and `v2_replay.clj:381` read PATIENT STATE, not
   the log. ADR-0172 tagged both "time-varying" without distinguishing
   the source, and the source is what decides whether they need work.
   They do not: fold a delta onto patient state and they pick it up.
2. The emitter's edit surface is **twelve threading sites**, not six
   read sites -- `personas` is a parameter through `emit`,
   `emit-with-offsets`, `event->messages` and seven message builders.
   Cheaper than "six rewrites", wider than "one map".
3. ADR-0172's provenance finding is now proved rather than asserted:
   `run.clj:391` builds `engine-params` as `(select-keys opts
   [:patients :arrival-gap :warm-up-seconds])`, so `:persona-config`
   reaches the engine and never reaches the artifact's face.

## The STOP check

The fence said STOP if any ruled row of ADR-0172 cannot be honoured by
the fold as the tree stands. **All seven are honourable.** Two took
work, and the work is what shaped the design.

* **F1 forced the fold's LOCATION.** `components/person-simulator`
  depends on `components/sim-engine`; the reverse edge is a cycle
  `poly check` refuses, and it would turn limitations row 10 red. So
  the engine can never require the component. The fold takes person
  events as CONFIG DATA, produced by `ehrt.sim.run`, exactly the
  layering `:modules` already uses. Row 10 stays green verbatim, both
  halves.
* **C1 forced an ordering, and it resolves exactly.** `persons` is a
  whole-population front door and needs `:deaths` up front, while the
  compile lives inside `decide :registered` at arrival. The chain is
  persona -> compile -> death -> walk -> persona. It breaks because
  `initial-persona`'s own docstring says `:death-t` shapes no field of
  the returned Persona; the module walk is already independent of
  arrival time (its `reg-t` is a fixed calendar anchor and every
  `world` value it reads is config); and the compiled death instant is
  **exactly** computable up front, because `compile_trajectory.clj:441`
  emits `{:from g :to g}` delays and ADR-0171 section 2(d) made
  `:from` = `:to` draw-free. Had compiled delays been ranges, C1's
  literal reading would have been structurally impossible and this
  session would have stopped.

## Two findings that are one line each, not rows

* **The identification merge cannot reuse `decide :merge`'s guard.**
  Its `never-mergeable?` excludes `:new`, and a placeholder patient
  registered but never admitted is `:new`. The guard's stated reason
  ("no `:admission` event for `participant-ids-exist-in-run` to find")
  is stale for a run-produced world -- every patient has a
  `:registered` now -- but relaxing it would move churn corpora for an
  unrelated reason. ADR-0173 adds a step type with its own guard that
  emits the SAME `:merge` event instead.
* **An unhoused `:residence-move` would go red against limitations
  row 7's own gate**, correctly: the gate is `(remove #(pool (:address
  %)) moves)` and `(pool nil)` is `nil`. So the residence sum lands as
  a fifteenth person-event kind, `:residence-loss`, and row 7 stays
  green verbatim. That is the one change this arc owes
  `components/person-simulator`.

## Ruling E's evidence

The prompt asked for what real ED registration systems emit, cited.
Three sources, and they do not converge on an absent address:

* A curated-registry study of homelessness identification in the EHR
  (PMC11618276) names the historical registration identifiers as
  "homeless check box or keyword 'homeless' in patient address field",
  and separately "geocoded patient addresses corresponding to addresses
  of regional emergency shelters, transitional housing programs, or
  homeless service providers".
* A 2025 address-change study (PLOS One, pone.0318552) names
  "residential address that indicates 'undomiciled' or 'homeless'", a
  congregate living facility, or the hospital's own address.
* HL7 v2 offers nothing: no Table 0190 code, no US Core address
  extension. The v3 `Homeless` value set is a living-arrangement
  concept.

**A verification note, because it changed the answer.** A search-result
summary asserted that the first paper counted BLANK addresses as
homeless. Reading the paper directly does not support it -- the paper
describes the keyword and the geocoded-shelter criteria and no
blank-address criterion. The summary is not relied on, and the
recommendation is written against the papers, not against the search.
The recommendation still lands on absent PID-11 for v1, for a different
reason than the search suggested: every sentinel in the field is one
site's local convention, and this repo already has a seam for those.

## Gates

`clojure -M:poly check` green (OK). `bin/preflight` ran first, exit 0,
no findings; it disclosed only that HEAD is not tagged `stable-*`,
which is correct -- the de-scaffold ruling retired the tag and this
session pays none.

`make test` run in full before the push, its exit code captured
explicitly:

```
MAKE_EXIT=0
Execution time: 16 minutes 55 seconds
4,114 tests, 18,412 assertions, 0 failures, 0 errors
bin/verify-nist-lock: OK, 6 coordinates match artifacts.lock.edn
```

Two docstring-only edits landed AFTER that run and before the second
commit -- two citations to `.agents/memory` that do not resolve.
`.agents/memory/` holds a README and nothing else; the two facts I
attributed to it (a reshuffle empties knife-edge fixtures silently;
parse EDN rather than grep it) are real, but their homes are
`.agents/session-records/2026-08-25-arc-1b-stream-partition-migration.md`
and `ehrt.docs-tooling.project-classpath-test`'s own docstring. Both
citations were rewritten to their real sources, and the ten doc gates
that read docstrings and paths were re-run against the edited tree:
85 tests, 594 assertions, 0 failures, 0 errors. Disclosed here rather
than quietly re-run, because the full-suite figure above predates the
edit by two docstrings.

## Close

CI at the pushed tip is the close marker (no tag was paid -- the
de-scaffold ruling of 2026-08-25 retired the per-arc `stable-*` tag).

```
gh run view 32915571939
status=completed  conclusion=success
sha=b11e377bc8058eca965172ac356f5c4335115056
title=design: ADR-0173 -- arc 3a, the engine folds the person stream
```

Two commits pushed: `667d1a0` (the ride-alongs) and `b11e377` (the
design). `bin/post-push-verify ee573c4 b11e377` ran with all three
checks clean -- remote tip matches, every commit message in range pure
ASCII, and the CI run reported once at queue time and now concluded
above.

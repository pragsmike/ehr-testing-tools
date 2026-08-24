# Archived prompt: traffic-scale-program (2026-08-24)

**Repo:** `ehr-testing-tools`, WSL clone `/home/mg/src/ehr-testing-tools`.
**HEAD at session start:** `7c3418d`, tree clean, branch `main`.
**Mode:** docs-only landing session; one commit, local only, no push, no tag.
**Paired record:** [`../session-records/2026-08-24-traffic-scale-program.md`](../session-records/2026-08-24-traffic-scale-program.md).

The driving prompt, verbatim:

---

# Session prompt -- traffic-scale program documentation landing (docs-only)

## Context

A design-channel conversation (2026-08-24) settled the event-mix doctrine for
scaling ehrt toward hospital-realistic traffic volume, converted Q3(b) from
deferred to called-for, and chartered (in intent) a person-simulator. None of
it is repo-recorded. This session lands it: one anchoring ADR, one doctrine
document, one program plan, register entries, roadmap rows. NO src, NO tests,
NO schema changes -- documents, register, roadmap, indexes only.

The three document payloads below are AUTHORITATIVE CONTENT -- land them
verbatim except: fix the ADR number if 0168 is taken, fill dates from the
actual session date, and adjust internal cross-links to the paths as landed.
Mechanical conflicts (a heading style the doc gates lint, a link format)
are fix-forward with disclosure. Substantive conflicts (a payload claim the
tree contradicts) are FINDINGS -- stop and report.

## Author rulings (verbatim, from the design channel 2026-08-24)

- R-mix-1: life-arc dynamics are bespoke hazard-rate processes, not GMF
  modules.
- R-mix-2: family/household structure in scope, pregnancy->delivery
  explicitly.
- R-mix-3: geography stays small and file-drawn; grow the table modestly.
- R-mix-4: unidentified/unresponsive ED arrivals and delayed-insurance flows
  in scope.
- R-mix-5: scheduling is state.
- R-mix-6: bed-status is state.
- R-mix-7: chatter and fan-out are emission add-ons downstream of the fact
  generators.
- Q3 conversion: Q3(b) -- per-patient/per-person RNG streams plus the
  from==to delay-draw skip -- is called for, prerequisite to the traffic-scale
  program; the shared-RNG limitation stands until that arc lands.
- Classification principle: if downstream invariants or later messages'
  content must respect it, it is skeleton (ground truth, judged); if it is
  derivable restatement, it is emission (rendered, unjudged).
- Documentation disposition: (a) -- one anchoring ADR; doctrine detail in
  docs/; program detail in .agents/plans/.

## Read first

1. notes/adr/ tail -- confirm next free ADR number (expected 0168)
2. .agents/rulings.md -- register format for the R-mix entries
3. .agents/plans/2026-08-02-sim-split-plan.md -- plan-document precedent
4. docs/operational-models.md -- sibling doctrine doc, for house style
5. .agents/plans/roadmap.md -- row contract (6-line cap, token forms);
   NOTE: ## Done sits at exactly 30 lines -- rotate FIRST before any row add
6. components/sim-engine/src/ehrt/sim_engine/churn.clj docstring -- the six
   churn step types the taxonomy cites; verify the payload's claim matches

## Steps

0. Environment per standing structure. HEAD at current main tip (7c3418d or
   descendant); tree clean; NO baseline suite required (docs-only session;
   doc gates + targeted docs-tooling run suffice -- this is the standing
   docs-session dispensation, disclose its use).

1. Attic rotation FIRST: ## Done is at cap; rotate the oldest whole row to
   the month attic per ADR-0161 before any edit adds lines.

2. Land document 1: notes/adr/0168-traffic-scale-program.md -- PAYLOAD A.

3. Land document 2: docs/traffic-model.md -- PAYLOAD B.

4. Land document 3: .agents/plans/<session-date>-traffic-scale-program.md --
   PAYLOAD C.

5. Register: append the R-mix-1..7 entries, the Q3-conversion entry, and the
   classification-principle entry to .agents/rulings.md in register format,
   each citing ADR-0168.

6. Roadmap: four rows under ## Next, one per arc (stream-partition design
   ADR; person-simulator; engine fold extensions; emission add-ons), each
   pointing at the plan document, priorities ascending after existing rows.
   Also: update the Q3 row (named limitation -> conversion recorded, cites
   ADR-0168); leave Q4 untouched (unruled).

7. Regenerate derived indexes (make state-derived / docsgen as the tree
   requires). Doc gates: full docs-tooling brick run
   (poly test brick:docs-tooling) green. If the citation gate requires the
   new docs to be referenced from an index, wire per house convention.

8. Session record per standing structure: what landed, payload-vs-landed
   deltas (every mechanical fix-forward named), gate results, deviations.
   Self-archive this prompt (paired slug).

9. ONE commit (docs-only session, work and close combined, the suite-time
   probe session's own precedent):
   `docs: traffic-scale program -- event-mix doctrine, Q3(b) conversion,
   person-simulator intent; ADR-0168, docs/traffic-model.md, program plan,
   R-mix register entries (design channel 2026-08-24)`
   Local only; no push, no tag.

## Fences

- F1: no src, no test, no schema, no vendored content changes.
- F2: payloads land verbatim modulo the named mechanical classes; every
  delta disclosed in the session record.
- F3: estimates in PAYLOAD C's appendix stay labeled as estimates -- do not
  promote to fact, do not delete.
- F4: if the tree contradicts a payload claim (churn step types, component
  names, Q3 row wording), FINDING -- stop and report before landing that
  document.

(The three payloads -- PAYLOAD A for the ADR, PAYLOAD B for the doctrine
document, PAYLOAD C for the program plan -- followed here verbatim in the
original prompt. They are not duplicated in this archive: all three landed
essentially verbatim, and the landed files ARE the payload text. The four
mechanical deltas between payload and landed form are enumerated in
`notes/adr/0168-traffic-scale-program.md`'s own "Landing deltas, disclosed"
section and in the paired session record.)

---

## Deviation record

Four mechanical fix-forwards, no STOP taken; the full accounting is in the
paired session record's own "Payload-vs-landed deltas" section and, more
durably, in ADR-0168 itself. In brief: the doctrine document landed at
`docs/dev/traffic-model.md` rather than `docs/traffic-model.md` (three
convergent gate-and-rule reasons); PAYLOAD A's headings demoted one level
for the generated ADR index; no Q3 roadmap row existed to update, so the
conversion is carried by the arc-1 row created already stating it; and the
prompt's own reading-list item 4 named a path that does not exist
(`components/sim/docs/operational-models.md` is the real sibling).

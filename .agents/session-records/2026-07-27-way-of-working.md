# 2026-07-27 — way-of-working: capturing the meta-process before the design channel retires

## Scope

The design channel that orchestrated roughly forty independent Claude
Code sessions across this repo's life is being retired. This session's
job was to write down the *meta-process* — not another product
decision, but the working system itself (session types, the prompt
discipline, the standing rules and where each one came from, the
failure modes they defend against) — before the only place that
reasoning lived stopped existing. Three tasks, one seam after the
first:

1. **`docs/way-of-working.md`** — the meta-process document itself,
   citing real instances from this repo's own ADRs and roadmap rather
   than describing the process in the abstract. **This is the seam.**
2. **`.agents/session-records/`, started** — the one process gap the
   retiring channel's own reflection pass named: session summaries
   used to live only in chat. This record is the convention's first
   instance, demonstrating itself.
3. **Wiring** — `docs/README.md`'s map and trust taxonomy, `AGENTS.md`'s
   pointer to the new convention (plus three now-cited ADR origins on
   standing rules that lacked them), `AUTHORS-GUIDE.md`'s ceremony
   section, and a roadmap "process debts" note.

This is a documentation-only session: no `src/`, `test/`, `deps.edn`,
or `Makefile` file was touched.

## Red→green evidence highlights

No code changed, so there is no red→green cycle to report — the proof
for a docs-only session is different: the suite stays green and
untouched. Confirmed directly, not assumed: `clojure -X:test` reports
**437 tests / 1125 assertions, 0 failures/0 errors** both *before* this
session's first edit and again *after* its last one — identical to the
baseline the prior session (module curation, M7) left, exactly what
"docs-only" should mean when checked rather than claimed.

## Judgment calls and their ratification status

None of these were handed verbatim; each is a call this session made
against the brief's own reasoning, recorded here for the author to
ratify or correct, per this repo's own author-review-list convention
(ADR-0013's "Ratification record" is the model):

1. **Corrected a factual claim in the brief against the real record.**
   The brief's own framing named "the invariant catalog catching three
   engine bugs in M2b" as the receipt for test-first's payoff. The real
   record doesn't support that attribution: M2b's own roadmap section
   names exactly one property-testing-surfaced design decision (the
   conservative `:cancel-discharge` behavior), not three bugs. Milestone
   **M4**, not M2b, is where three real bugs were caught by property
   tests in one milestone (the 13-vs-14 RNG draw count; the missing
   `:active-mrn` on `:registered`, caught by the *existing* M2b merge
   invariant; and the ER7-escaping five-pass decoding bug, F9).
   `docs/way-of-working.md` §4 cites the real M4 instance and does not
   repeat the brief's M2b attribution — reality wins the conflict, per
   this session's own instructions, noted here rather than silently
   substituted.
2. **The "fixture-location case" the brief asked for, identified and
   cited precisely.** The brief named this as an example of a session
   following an ADR over a prompt's literal instruction, without
   pointing to a specific record. Found: `docs/gmf-interpreter.md` §7
   states the M5a interpreter fixture was "placed there [in
   `test/ehr_testing_sim/fixtures/`], not `resources/modules/`, per
   [ADR-0013 point 6's] own reasoning" — the more surface-obvious
   location (next to real vendored modules) was not used, because the
   ADR said otherwise. Cited in `docs/way-of-working.md` §1 with the
   real wording, not a paraphrase invented to fit the brief's framing.
3. **"Findings-not-failures" and the sibling's own dependency-discipline
   reasoning could not be verified against `ehr-testing-tools` directly**
   — no sibling checkout was available this session. Handled per the
   brief's own fallback instruction: marked "tools-territory pending"
   in `docs/way-of-working.md` §5 rather than invented or omitted
   silently. The three-defect-classes claim (§4/§5) *is* fully
   verifiable from this repo's own side (the "Task 0" fixes at M3, M4,
   and M6, each traceable to a tools-side consumer-loop finding) and is
   cited with real specifics.
4. **The `.agents/prompts/archive/` gap, surfaced and disclosed rather
   than glossed over.** The brief's own item 3 assumed prompts are
   "archived where the convention exists." Checked directly: the
   convention is named in the `Makefile`'s own comments but the
   directory has never been populated in this repo (`Makefile`: "this
   repo's `.agents/skills/` is empty and `.agents/prompts/archive/`
   doesn't exist"). `docs/way-of-working.md` §3 states this plainly as
   an honest gap rather than describing the convention as if it were
   already real here.
5. **Trust-taxonomy placement (Task 1, item 7) resolved as its own
   named class**, "Practice, not product," distinct from but adjacent
   to `docs/README.md`'s existing "Specs and as-built records" class —
   added to that page's own trust list in Task 3, not merely described
   inside `way-of-working.md` itself.
6. **Process-debts note (Task 3, item 2): found and fixed a real gap,
   not merely flagged one.** Checked whether each standing rule
   `way-of-working.md` cites carries its own origin inline where the
   rule lives (not just narrated in the roadmap's history). Three of
   `AGENTS.md`'s Code-conventions bullets (result-not-throw,
   determinism-is-law, co-landing) did not, unlike the CLI-surface
   bullet the brief used as its own example of what "having an origin"
   looks like. Fixed directly in `AGENTS.md` this session (added the
   ADR-0001/ADR-0002 point citations), and the fix is recorded in the
   roadmap's new "Process debts" section rather than left as an open
   item with no receipt.
7. **Cross-repo tools note: recorded, not acted on.** No sibling
   `ehr-testing-tools` checkout was available to check whether that
   repo already has an equivalent document or to write one there
   (ADR-0001's dependency direction also means this repo shouldn't be
   the one editing tools' tree regardless). Flagged in the roadmap's
   new section and in this session's own closing note to the author,
   per the brief's explicit instruction — author's call, next
   `ehr-testing-tools` session.

## Findings

- The `.agents/prompts/archive/` convention is named but has never been
  populated in this repo (item 4 above) — a real, pre-existing gap this
  session did not create but did surface plainly, for the first time in
  a committed document rather than only in a `Makefile` comment.
- Three `AGENTS.md` standing rules lacked an inline origin citation
  (item 6 above) — found and fixed in the same session, per this
  project's own fix-forward-with-disclosure convention (the fix is
  disclosed here, not silently folded into the diff).
- `ehr-testing-tools` may want its own copy or pointer to
  `docs/way-of-working.md` — named, not built (item 7 above; ADR-0001's
  dependency direction).

## HEAD landed

This session's entire diff — `docs/way-of-working.md`,
`.agents/session-records/` (this file and its `README.md`),
`docs/README.md`, `AGENTS.md`, `AUTHORS-GUIDE.md`, and
`.agents/plans/roadmap.md` — lands in this session's own single commit,
per the ceremony this record's own last line points to (write record →
commit → push). See `git log` for the resulting HEAD; this file and
that commit land together, so there is no separate hash to record here
that this session could observe before making it.

This session closes the design conversation that built this repo:
everything it captured is now repo truth; anything it didn't capture is
deliberately let go, per `docs/way-of-working.md` §6's own honest-limits
section.

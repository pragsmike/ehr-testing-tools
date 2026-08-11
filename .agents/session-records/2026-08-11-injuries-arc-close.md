# 2026-08-11 — Injuries arc close: auto-close on reopen lands, the batch vendors (ADR-0107)

## Scope

B3 of the author-ruled injuries arc (2026-08-11, author verbatim
"Let's do (i)."): PHASE 1 executes ADR-0106's option (i) — the
`:encounter` case's own assert becomes a conditional auto-close,
upstream-faithful. ON PHASE 1's OWN GREEN, PHASE 2 lands the injuries
vendoring batch itself under the standing vendoring ceremony. Two
commits: `7db2044` (fix) and `29392cd` (feat), plus this close-phase
record/ADR/prompt-archive commit.

## Red→green evidence highlights

**Phase 1.** The pre-existing `nested-encounter-asserts-rather-than-
silently-nesting` test's own fixture (`Encounter -> Encounter`, no
`EncounterEnd` between them) is BYTE-IDENTICAL in shape to
`injuries.json`'s own `Spinal_Injury_Treatment_Encounter` reopen —
renamed and rewritten to assert the desired post-fix behavior
instead. Verified red pre-fix (`git stash` of the source file alone):
the walk throws the exact pre-existing `AssertionError`. Post-fix:
green — `:status :horizon-complete`, one synthesized `:encounter-end`
present, correctly cited, `:t` equal to the reopening encounter's own
`:t`.

A scratch probe (never committed) re-ran ADR-0106's own two recorded
failing seeds (mixer-seed 20260803, census parameters) against the
pinned checkout: pre-fix, both throw the identical `AssertionError`;
post-fix, both complete with `:synthesized-encounter-ends` = 1 each.
The full 120-seed sweep: 2/120 fail pre-fix (the SAME two seeds), 0/120
fail post-fix.

Interpreter test namespace: 203 tests, 535 assertions (up from 527),
0/0. Oracle bracket leg 1 (`fdb3984` → `7db2044`): IDENTICAL, all 34
roots.

**Phase 2.** Both new round-trip test files verified red BEFORE the
vendored resources existed (`git stash push` of the 3 resource files
alone): `IllegalArgumentException: Cannot open <nil> as a Reader` at
the `io/resource` call sites, on both the interpreter-layer and
engine-layer namespaces. Restored, both suites green: interpreter
layer 4 tests/8 assertions, engine layer 2 tests/5 assertions (real
compiled content, the full invariant catalog holds, real HL7 renders,
`:synthesized-encounter-ends` pinned at 4 across 300 patients).

A live probe found `engine/run` at the 100-year (`36500`-day) horizon
convention most engine-layer roots use throws `run-submodule exceeded
max-steps` at `broken_jaw.json`'s own dental-referral loop — exactly
ADR-0106's own dated finding [C] predicted (mean ~9124 cycles at 100
years, over the 10000-step budget; mean ~4562 at 50 years, safely
under it). The round-trip test, the interpreter walk-result helper,
and `digest.clj`'s new root all use the 50-year (`18250`-day) horizon
instead, matching `census.clj`'s own default and every probe this arc
has ever run this closure at.

A NEW named regression test re-walks ADR-0106's own seed
`-576131918266266247` against the REAL vendored closure (not a
synthetic fixture): the walk completes, both encounters land, exactly
one synthesized end, correctly cited — the arc's own closing witness.

Oracle bracket leg 2 (`7db2044` → `29392cd`, `--declared-digest-change`):
`DIFFERS`, EXACTLY ONE added root (`injuries.edn`), zero changed/
removed among the 34 pre-existing.

## Judgment calls and their ratification status

- **The `:synthesized-encounter-ends` counter was added**, per the
  driving prompt's own explicit either-way instruction — mirrors
  `:suppressed-encounter-ends`'s own passthrough exactly, through
  every site that field already touches. Not a judgment call so much
  as an executed instruction; the reasoning (a countable witness
  phase 2's own named-regression requirement needed) is recorded in
  the ADR.
- **`wellness-wait-step`'s own separate nesting assert left
  unchanged.** A disclosed, narrower scope decision: the driving
  prompt's own Context names "the `:encounter` case's own assert"
  specifically; ADR-0106's own full-graph sweep never found the
  hazard reachable through the wellness site. Not touched, not
  guessed at.
- **The engine-layer horizon set to 18250, not 36500.** Found live
  (a real `max-steps` throw at 36500), not a preference — the ONLY
  choice that lets the round trip actually complete for this closure.
  Disclosed in both the ADR and the test file's own dated comment,
  not silently substituted for the "batch convention."
- **`.agents/reading-sets.edn`'s own `:onboarding` budget re-baseline
  folded into this close-phase commit**, not deferred to a future
  session — matches the file's own standing "re-baseline on the
  session that trips it" discipline, applied here for the first time
  by THIS session rather than cited as someone else's future work.
- **`.agents/state.md`'s own staleness tripwire fixed citation-only,
  exactly precedented.** This ADR's own filename (`0107-injuries-arc-
  close.md`) matched `state_staleness_tripwire_test.clj`'s own
  `*-arc-close.md` regex, tripping it red — the SAME gap
  `notes/adr/0097-review-2-arc-close.md` hit at its own close. Fixed
  the same way that entry was: a citation-only update, content NOT
  re-probed, disclosed as such, not a full regeneration this
  session's own driving prompt never chartered.

## Findings and HEAD landed

**Both deferral legs of the injuries arc are now closed**: max-steps
(ADR-0105) and nested-encounter (this ADR). `injuries.json`'s own
eight-file closure is fully vendored — 5 already-vendored members
re-verified byte-identical, 3 new members landed byte-verbatim.

**A real, disclosed horizon boundary for this specific closure**:
`broken_jaw.json`'s own dental-referral loop needs a horizon under
roughly 70-80 years (interpolating between the measured 50-year-safe
and 100-year-throws data points) to stay under the interpreter's
`max-steps` budget — a fact about THIS module's own authored content,
not a general interpreter defect, and not fixed this session (the
fence named `max-steps`/horizon changes as ADR-0105's own closed
scope).

**A NEW chartering ruling, recorded but not executed**: downstream-
latency realism (lab results/EHR logging both take real time; a
downstream HL7 receiver may see incomplete encounter records) — a
roadmap Next row anchors it for a future design pass.

**Tag paid forward**: `stable-20260811-injuries-b2-assessment` tagged
at `fdb3984` (Step 1, this session), peeled ref verified exact match,
remote unmoved.

**HEAD landed**: `7db2044` (phase 1 fix), `29392cd` (phase 2 batch),
plus this close-phase commit (this record's own commit), all pushed.

**A post-push, disclosed CI flake, unrelated to this session**: CI on
`29392cd` reported `failure` on `ehrt.corpus.sink-composability-test`
(a generator-exhaustion property-test flake, no fixed seed, last
touched 2026-07-31). Confirmed not caused by this session (the next
push's own full suite passed against the same code; a `gh run rerun
--failed` of the exact failed job passed). Disclosed in ADR-0107's own
dated append and given a roadmap Deferred row, not silently
reconciled by the passing re-run.

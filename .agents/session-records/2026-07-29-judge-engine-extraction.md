# 2026-07-29 — Per-engine judge split: `judge-v2-hapi`, `judge-fhir-official`

## Scope

Autonomous session (R30), three checkpoints. `components/judge`
(ADR-0008) held two gate engines (`ehrt.judge.v2`, HAPI HL7v2;
`ehrt.judge.fhir`, the official FHIR validator) and the shared verdict
vocabulary (`finding`/`report`/`verdict-cache`) in one brick. This
session extracted each engine into its own component
(`components/judge-v2-hapi`, `components/judge-fhir-official`),
following the ADR-0008 playbook (census → interface sizing by grep →
project-level dep wiring → full-suite verification), ahead of a
planned third engine (NIST HL7 v2, EXP-D3) that lands into the same
seam. Full reasoning-of-record: `notes/ADRs.md` ADR-0011.

## Red→green evidence highlights

`clojure -M:poly check`: green before any move (baseline probe) and
green after the extraction, once two real Polylith interface
violations were fixed (see Deviation record). `clojure -M:poly deps`:
`judge-fhir-official` → `{judge, kernel}`, `judge-v2-hapi` → `{kernel}`
(real) / `{judge}` (test-only), `judge` → `{kernel}` only, no
engine-to-engine arrow, `tools` → both engines + judge + kernel +
palgebra + sim — exactly the shape ADR-0011 names. `clojure -M:poly
test :all skip:integration`: exit code and full log captured directly
(`> file 2>&1; echo EXITCODE:$?`, no pipe, per the sim-sibling errata
session's own lesson) — `EXITCODE:0`, 20m35s, three projects
(`conformance`, `ehrt-cli`, `sim`), zero `FAIL`/error markers in the
1416-line log. Characterization: `bin/ehrt gate v2`, `gate fhir
--report`, and `check` re-run against the exact fixture set and
commands `notes/judge-engine-extraction-characterization.md` recorded
before the move (its own commit, checkpoint 1) — all three `--report`
EDN files and all three stdout logs byte-for-byte IDENTICAL to that
baseline (`diff`, zero output on all six comparisons). Zero-behavior-
change ruling verified, not merely asserted.

## Judgment calls and their ratification status

Autonomous session, no author present to ask — every call below is
this session's own, per the prompt's decision procedures, not
individually ratified yet:

- **Interface simplification.** `ehrt.judge-v2-hapi.interface`/
  `ehrt.judge-fhir-official.interface` export plain `gate-file`/
  `gate-dir`(`/gate-batch`) rather than carrying forward the
  `v2-`/`fhir-` qualification — the collision that qualification
  existed for (two engines sharing ONE interface) no longer exists once
  each has its own. `ehrt.tools.interface` re-applies its own
  qualification at its own re-export layer, unchanged, so nothing
  downstream sees a difference.
- **`worst-of`/`verdict-cache-*` left unqualified** in the widened
  `ehrt.judge.interface` — no collision against any existing export,
  so no prefix added beyond `verdict-cache-` for the four cache
  functions (readability, not disambiguation).
- **`ehrt.judge-v2-hapi.v2`'s unused `ehrt.judge.finding` require
  removed outright**, rather than routed through `ehrt.judge.interface`
  — it was genuinely dead code (no `finding/` call site in the file),
  found only by fixing the `poly check` violation it caused.
- **Moved test namespaces' own direct `ehrt.judge.finding` requires
  left as-is** (not rewritten to go through `ehrt.judge.interface`) —
  `poly check` does not flag test-alias code the way it flags `:default`
  (src) profile code, so this was judged not worth the churn.
- **`ehrt.tools.deps.edn`'s `data.json` comment and `ehrt.tools.lint`'s
  `target-2-deps-edn-paths` updated** to reflect the second coordinate
  move (ADR-0008 → ADR-0011) — small, mechanical, directly caused by
  this session's own change, not scope creep.
- **User-path doc citations (`docs/formats.md`, `docs/glossary.md`,
  `docs/judge-calibration.md`) found naming the moved namespaces —
  NOT fixed**, per the prompt's own explicit instruction to record
  rather than silently resolve. Left for an author ruling on whether
  namespace citations count as the "Polylith internals" ADR-0010's R34
  excludes from the user path.
- **`components/tools/docs/pipeline.edn`'s pre-existing stale
  `ehrt.tools.judge.fhir` citation (predates even ADR-0008) — found,
  not fixed**, out of this session's own narrowly-scoped docs sweep
  (Step 7 named only `docs/dev/architecture.md` and `AGENTS.md`).

## Findings and HEAD landed

Three checkpoints, three commits, all pushed clean (pre-push hook: WSL
provenance, gitleaks — no leaks — and `poly check`, all green):

1. `2caffd7` — `docs: judge extraction census and characterization
   baseline` (the pre-move census table and byte baselines,
   `notes/judge-engine-extraction-characterization.md`).
2. `e89ac0e` — `feat: extract judge-v2-hapi and judge-fhir-official;
   judge keeps the verdict vocabulary (ADR-0011)` (the `git mv`-based
   extraction, interface/deps rewiring, and the ADR itself).
3. `b4314d8` — `docs: architecture and AGENTS reflect the judge engine
   split (ADR-0011)` (the narrow dev-path docs sweep).

HEAD after this session's ceremony: `b4314d8`. No tags cut, no `gh`
mutations — both are the author's own ceremony per this repo's
standing rule, untouched this session.

## Deviation record

**`poly check`, run for the first time against the moved source, found
two real Polylith interface violations the census alone could not
show** (a census shows what a namespace requires, not whether the call
site inside it actually uses what it requires): `ehrt.judge-v2-hapi.v2`
and `ehrt.judge-fhir-official.fhir` both directly required
`ehrt.judge.finding` (and `fhir` additionally `ehrt.judge.verdict-cache`)
— legal while all three lived in one brick, illegal the moment `v2`/
`fhir` moved to their own bricks. Resolved two different ways (full
detail: ADR-0011's own deviation record): `v2`'s require turned out to
be dead code, removed outright; `fhir`'s two calls
(`finding/worst-of`, four `verdict-cache/*` functions) were genuine and
load-bearing, so `ehrt.judge.interface` was widened to re-export all
five and `fhir` now calls through it instead of reaching into `judge`'s
internals directly.

**Found, disclosed, not fixed (per the prompt's own instruction):**
three user-path docs (`docs/formats.md`, `docs/glossary.md`,
`docs/judge-calibration.md`) cite the moved namespaces directly, two of
the citations now stale; one component-adjacent doc
(`components/tools/docs/pipeline.edn`) carries a citation stale since
before ADR-0008. Both named in ADR-0011's own deviation record for an
author ruling, not resolved unilaterally by this session.

No test failures, no flakes, no environment surprises, no `gh`
mutations attempted, no CLI-surface or `gate` output change at any
point — the zero-behavior-change ruling held throughout, verified by
byte-diff rather than merely asserted.

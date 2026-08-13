# 2026-08-13 — ehr-testing-tools: user manual S4 — chapters 6-7 (ADR-0124)

## Context

Archived 2026-08-13. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `da72533` (ADR-0123's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.
One finding disclosed in full, not fixed here (out of this session's
own docs-and-registers-only fence, scoped to Chapters 6-7 alone): a
pre-existing, repo-wide `ADR-0010` citation drift, surfaced while
reading the driving prompt's own "verdict ranking... ADR-0010's
register trace" pointer — see this session's own session record and
`notes/adr/0124-manual-s4-mutate-and-gate.md`'s own dedicated section
for the full account, not reproduced here as it is not part of the
driving prompt.

## Original prompt (verbatim)

Session prompt — user manual arc, S4: chapters 6–7 (ADR-0124)
You are Claude Code executing under R30 ceremony for ehr-testing-tools, working for mg. DOCS-AND-REGISTERS-ONLY: zero src, zero test code, zero demo edits. Lands chapter 6 (breaking data on purpose) and chapter 7 (judging). STOP on any conflict. Standing notes: full `make test` before EVERY push; companions in-fence by rule; budget trip → STOP; strips copied verbatim from witnessed sources, cited per strip in the ADR; no ADR tokens in user prose; no cross-commit dangling references; NEVER fabricate output. GATE POLICY (explicit, the ADR-0121 lesson): any generative failure in the engine invariant defspec at ANY seed is a NEW finding — STOP-AND-REPORT with seed and shrunk value; no re-run license exists in this session.
Read first

1. `docs/manual/00-front.md` through `05-*.md` — voice, structure, figure conventions (the two S3 SVGs' shape).
2. `docs/use-cases/` — the mutation and gate cases (strip sources); root `README.md` Quickstart (gate strips).
3. `docs/operators.md` (linked, never restated) and `docs/judge-calibration.md` (same).
4. `components/corpus/docs/pipeline.edn` + `palgebra-design.md` (Mutate→Gate stages — chapter 6's figure derives here) and the verdict ranking in judge docs/ADR-0010's register trace (chapter 7's figure derives from the documented verdict ranking; no ADR token in prose — footnote-marker form if a citation is needed).
5. `.agents/rulings.md` R2/R6; `.agents/plans/roadmap.md` S4 row.

Step 0 — Preflight and tag ceremony

* origin/main at `da72533`; CI green (`gh run list --limit 5`, completing ADR-0123's channel leg). Else STOP.
* Tag `stable-20260813-invariant-fix`, ANNOTATED, at `da72533`; push; peeled exact. Case (i): channel fresh-clone verification 2026-08-13 (lineage, ASCII x2, src exactly checker+test, both ruled conditions present, over-charter test removed), CI per preflight.

Commit 1 — chapter 6, breaking data on purpose
`docs/manual/06-breaking-data-on-purpose.md`: mutation as deliberate, named defect injection — canonical corpus in, mutant corpus out with lineage; choosing operators (link operators.md for the catalog; teach choosing, not enumerating); the inject-X-expect-X idea at reader level: a defect class injected should surface as the matching finding class at the gate, the validator as the answer key. Strips from the witnessed mutation use-case pages, cited. Figure (`docs/manual/assets/`): the inject-X-expect-X loop — defect class in, finding class out, the gate as oracle; content derived from pipeline.edn's Mutate/Gate stages (cite in SVG source comment). Length: medium. Message: `docs: user manual -- chapter 6, breaking data on purpose (ADR-0124)`
Commit 2 — chapter 7, judging
`docs/manual/07-judging.md`: the three gates (official FHIR, v2 HAPI, v2 NIST) and what each checks at reader level; verdict semantics — ok, rejected, and WHY no-verdict exists (the criterion couldn't be applied — suppressed terminology, defective profile — distinct from passing); calibration (link judge-calibration.md). Strips from witnessed gate strips (Quickstart / gate use-case pages), cited. Figure: the verdict decision — the three outcomes and what dominates what, derived from the documented verdict ranking (cite source in SVG comment). Length: medium. Message: `docs: user manual -- chapter 7, judging (ADR-0124)`
Commit 3 — close
Registers (S4 row → LANDED; S5 → next); rulings only if a mid-session ruling occurs; self-archive at close-phase START; ADR-0124 (per-strip citations, both figures' derivation notes); indices 121 → 122; Done line; session record. Message: `docs: session record and prompt archive -- manual s4 (ADR-0124)`
Oracle bracket: pure identity, all 35 roots. `bin/regression-oracle da72533 <final>`; non-identity → STOP. Gates: standing; ASCII x3; gitleaks; invocation lint covers the new strips; CI confirm or disclose.
Fences
Touch ONLY: `docs/manual/06-*.md`, `07-*.md` (new), `docs/manual/assets/*` (two new SVGs), `docs/manual/00-front.md` (one-liners only); registers, prompts, session-records, `notes/adr/0124-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; companions by rule. ZERO src/test/demos; ZERO edits to chapters 1–5. Outside → STOP.
STOP-AND-REPORT on: any witnessed source lacking a needed excerpt; lint red not attributable to copy divergence; the gate policy above; oracle non-identity; anything not pre-decided.

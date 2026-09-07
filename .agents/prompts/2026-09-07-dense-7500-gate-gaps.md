# 2026-09-07 -- dense-7500 gate gaps: exerciser into `make integration`; Scale table gated

Session prompt, archived verbatim as driven. Record:
[`../session-records/2026-09-07-dense-7500-gate-gaps.md`](../session-records/2026-09-07-dense-7500-gate-gaps.md).

---

Session: dense-7500 gate gaps -- exerciser into make integration; Scale table gated -- 2026-09-07

Context: roadmap.md:37-56 [dense-7500-gate-gaps]. bin/demo-exerciser-dense-7500 runs the
README's fenced commands under exit-code and derivation assertions but is in no make
target (Makefile:52-55 wires ed-tuesday and clinic-decade only); docs/consuming-ground-
truth.md's Scale table (:574-578) and demos/scenarios/dense-7500/README.md's three
figure rows were re-measured by hand at 4ddf62c2 and have moved under sites 5, 6 and 7
with nothing going red. Both gates were commissioned by ADR-0180 R-gate-gaps. Fresh clone
at 2ba3490c or later. WSL only. No sub-agents. R-sentinel.

Read first: AGENTS.md; SKILL.md (:87-88, :142); roadmap.md:37-56; Makefile:20-60;
bin/demo-exerciser-dense-7500 (whole; its header states the README-first rule);
demos/scenarios/dense-7500/README.md (the three measured rows and their derivations);
docs/consuming-ground-truth.md:514-600; the 2026-09-06 divergence session record (how
the rows were re-measured last time, and the seven-event lesson: one path, one tip);
components/docs-tooling (state-derived-test -- the freshness-gate pattern to copy).

Author rulings, verbatim and binding:
 R-gate-gaps (ADR-0180): "dense-7500's exerciser joins make integration; the Scale table
   gets a gate."
 R-figures-file (ruled 2026-09-07): "the deterministic figures -- events, messages,
   msg/event per cell -- live in ONE committed file the exerciser writes; README rows
   and the Scale table quote it and a test proves they match. Process-wall cells are
   dated quotations (sha, date, machine) of the same file, never asserted against a
   live run."
 R-readme-first (exerciser header): commands change in the README before the script.
 R-edit, R-cap, R-pins (standing); :onboarding headroom 48.

Steps (one gate each; commit message given):
1. bin/preflight. Run bin/demo-exerciser-dense-7500 once at the tip, timed, to establish
   its current wall and that it still passes; record which README derivations it asserts.
   Gate: exit 0. No commit.
2. Figures file. The exerciser gains a final step that writes demos/scenarios/dense-7500/
   figures.edn: per cell, events, messages, msg/event, plus wall, sha, date, machine
   (channel expectation: the exerciser already computes the counts for its derivation
   assertions -- reuse them; correct from the script). Commit the file as produced.
   Invariant: the deterministic fields equal what `sim run --format ground-truth` gives
   at the tip (one path, one tip). Gate: make test.
   Commit: "demos: dense-7500 exerciser writes figures.edn (R-figures-file)".
3. The gate. A test in docs-tooling (or the exerciser's own test namespace -- correct
   from where state-derived-test lives) parses the three count cells of the Scale table
   and of the README's rows and asserts equality with figures.edn; wall cells are
   asserted to carry the file's sha and date. Re-measured rows in both docs per
   R-figures-file (script file, R-edit), quoting the step-2 figures. Red first: the
   stale 4ddf62c2 cells must fail the new test before the docs edit. Gate: make test
   red-then-green, recorded. Commit: "docs: Scale table and dense-7500 rows gated
   against figures.edn; re-measured at <sha>".
4. make integration gains bin/demo-exerciser-dense-7500 (Makefile:52-55 and the help
   text at :22); README of the scenario states the target. Invariant: make test's wall
   unchanged. Gate: make integration exit 0 (requires artifact fetch; record its wall).
   Commit: "build: dense-7500 exerciser joins make integration (R-gate-gaps)".
5. roadmap: row to Done (pointer). Session record; archive prompt; close ceremony; push;
   verify CI by sentinel; close-marker commit. Commit: "docs: record CI success at
   <sha> -- dense-7500 gate gaps close".

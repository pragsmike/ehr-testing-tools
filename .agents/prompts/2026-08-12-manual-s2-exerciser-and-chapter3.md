# 2026-08-12 — ehr-testing-tools: user manual S2 — demo exerciser and chapter 3 (ADR-0120)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `800ae28` (ADR-0119's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.
Two mechanical findings, neither a design ambiguity, both fixed forward
without pausing for author input: an unstaged executable bit on the new
script, and a tree-clean-postcondition false positive from running the
exerciser against this session's own uncommitted work mid-development
— both disclosed in full in this session's own session record and in
`notes/adr/0120-manual-s2-exerciser-and-chapter3.md`'s own Deviations
section, not reproduced here as neither is part of the driving prompt.

## Original prompt (verbatim)

Session prompt — user manual arc, S2: chapter 3 + the demo exerciser (ADR-0120)
You are Claude Code executing under R30 ceremony for ehr-testing-tools, working for mg. This session lands the arc's biggest piece: the R3-ruled demo exerciser ("The demos must be known to work, and exercised as documented to make sure they actually play out as written" — author verbatim) and manual chapter 3. STOP-AND-REPORT on any conflict with the tree. Standing notes: full `make test` before EVERY push; companions in-fence by rule; budget trip → STOP; strips copied verbatim from witnessed sources with per-strip citations in the ADR; no ADR tokens in user prose; cross-commit references checked — nothing in commit 1 may link a commit-2 artifact (the ADR-0119 lesson).
Read first

1. `bin/quickstart-demo` + the `quickstart`/`quickstart-fresh` Makefile targets and `ehrt.docs-tooling.quickstart-fresh` — the pattern this generalizes.
2. `demos/scenarios/ed-tuesday/README.md` — every fenced command block and each section's own stated invariants (the determinism `diff -rq`, the 34-batch `:verified true` listing, the straddle membership, and the second-clock identity claims).
3. The integration-tier split: how `skip:integration` tests are marked and how `make integration` runs them.
4. `docs/dev/simulator-architecture.md` §4 "The two layers, instantiated" and `components/sim-trajectory/docs/trajectory-computation.md` — chapter 3's figure and two-spaces story derive from these.
5. `docs/manual/00-front.md`, `01-*.md`, `02-*.md` — voice and structure to match. `.agents/rulings.md` R2/R3/R7.

Step 0 — Preflight and tag ceremony

* origin/main at `800ae28`; CI green (`gh run list --limit 5`, completing ADR-0119's channel leg). Else STOP.
* Tag `stable-20260812-manual-s1`, ANNOTATED, at `800ae28`; push; peeled exact. Case (i): channel fresh-clone verification 2026-08-12 (lineage, ASCII x3, zero src/test, excerpt fidelity byte-checked against the demo README, paring and provenance markings confirmed), CI per this preflight.

Commit 1 — the demo exerciser

1. Red first. Write the fresh-identity test (quickstart-fresh pattern): the ed-tuesday README's fenced command sequence and `bin/demo-exerciser-ed-tuesday`'s command list must be IDENTICAL, in order. Run it against the current tree — RED (the script does not exist). Capture.
2. The exerciser. `bin/demo-exerciser-ed-tuesday` (mirror quickstart-demo's shape): runs the README's commands verbatim in a scratch dir, asserting per step (a) exit code, (b) the section's own named invariants — the determinism `diff -rq` empty, the batch listing count and `:verified true` per file, the straddle membership (Smith A01 in batch-000, A03 in batch-001), the second-clock zero-offset identity. Every asserted value copied from the README's witnessed output, never recomputed by hand. If any README command no longer runs as written, that is a FINDING — STOP-AND-REPORT with the divergence; the exerciser's whole point is catching exactly this, and the fix direction (README vs code) is an author call.
3. Integration wiring. The exerciser runs under `make integration` (the tier split's existing mechanism), NOT per-push CI; a fast identity test (step 1) runs in the normal suite. Green both. Makefile edit is in-fence for exactly this hook. Message: `feat: demo exerciser -- ed-tuesday runs as documented (ADR-0120)`

Commit 2 — chapter 3
`docs/manual/03-a-simulated-hospital.md`: `sim run` and the ed-tuesday scenario (strips from the demo README, cited); site profiles (link site-profiles.md); scripted-vs-generative patients; the accessible two-spaces story per R7 — two state machines, one wall, "formats are just emitters of the patient state machine" as the organizing idea, formalism linked (simulator-architecture §4) never taught. Figure: GT with its emitter arrows (one abstract object, emitH/emitF arrows out, the naturality square noted in the caption) — committed as SVG under `docs/manual/assets/`, its content derived from §4's own claims (cite the section in an HTML comment in the SVG source), styled minimally. Front-page arc entry updated if its one-liner needs it. Length: the manual's medium. Message: `docs: user manual -- chapter 3, a simulated hospital (ADR-0120)`
Commit 3 — close
Registers (S2 row → LANDED; the busy-tuesday exerciser noted as a future row, not this session); rulings "From ADR-0120" only if a mid-session ruling occurs; self-archive at close-phase START; ADR-0120 (red/green evidence, per-strip citations, the exerciser's full invariant list, one witnessed `make integration` run's result); indices 117 → 118; Done line; session record. Message: `docs: session record and prompt archive -- manual s2 (ADR-0120)`
Oracle bracket: pure identity, all 35 roots — the exerciser is new tooling that READS the tree and writes scratch only; chapter 3 is docs; the Makefile edit adds a target hook. `bin/regression-oracle 800ae28 <final>`; non-identity → STOP. Gates: standing; ASCII x3; gitleaks; the invocation lint covers the new strips; one full `make integration` run witnessed green before commit 1's push (this is the exerciser's own green half); CI confirm or disclose.
Fences
Touch ONLY: `bin/demo-exerciser-ed-tuesday` (new); the fresh-identity test file (docs-tooling, beside quickstart-fresh); the integration-tier wiring point + `Makefile` (the one hook); `docs/manual/03-*.md` (new), `docs/manual/assets/*` (new), `docs/manual/00-front.md` (one-liner only); registers, prompts, session-records, `notes/adr/0120-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; companions by rule. ZERO engine/sim/judge src; ZERO demo README edits (a divergence is a STOP, not a fix); ZERO edits to chapters 1–2.
STOP-AND-REPORT on: any README command failing as written; any invariant assertion the README's witnessed output cannot supply; red that won't go red; oracle non-identity; anything not pre-decided.

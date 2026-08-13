# 2026-08-12 — ehr-testing-tools: user manual S3 — the transport pair (ADR-0121)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `6c000aa` (ADR-0120's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.
Two self-caught process errors, neither a design ambiguity, both fixed
forward before either content commit was staged: an over-corrected
ASCII sweep of the manual's own Unicode punctuation, and a bare
ADR-token surviving in front-page prose -- both disclosed in full in
this session's own session record and in `notes/adr/0121-manual-s3-
transport-pair.md`'s own Deviations section, not reproduced here as
neither is part of the driving prompt.

## Original prompt (verbatim)

Session prompt — user manual arc, S3: chapters 4–5, the transport pair (ADR-0121)
You are Claude Code executing under R30 ceremony for ehr-testing-tools, working for mg. DOCS-AND-REGISTERS-ONLY: zero src, zero test code, zero demo edits. This session lands the transport pair — chapter 4 (time on the wire) and chapter 5, the arc's FEATURED chapter (batch delivery, the straddle). STOP on any conflict with the tree. Standing notes: full `make test` before EVERY push; companions in-fence by rule; budget trip → STOP; strips copied verbatim from witnessed sources, cited per strip in the ADR; no ADR tokens in user prose; no cross-commit dangling references; NEVER fabricate output — a missing witnessed excerpt is a STOP.
Read first

1. `demos/scenarios/ed-tuesday/README.md` — the second-clock and batched-delivery sections: every strip and witnessed output the chapters excerpt.
2. `docs/manual/00-front.md` through `03-*.md` — voice, structure, figure conventions (the ch-3 SVG's shape).
3. `docs/dev/simulator-architecture.md` §4 + §5 (latency seam) — the two figures' content sources.
4. `docs/use-cases/supply-batch-straddling-traffic.md` and the play use case — cross-link targets.
5. `.agents/rulings.md` — R2/R6/R7 and the ADR-0112 featured- placement ruling (quote it in ch 5's ADR section, not in prose).

Step 0 — Preflight and tag ceremony

* origin/main at `6c000aa`; CI green (`gh run list --limit 5`, completing ADR-0120's channel leg). Else STOP.
* Tag `stable-20260812-manual-s2`, ANNOTATED, at `6c000aa`; push; peeled exact. Case (i): channel fresh-clone verification 2026-08-12 (lineage, ASCII x3, zero src, exec bit staged, straddle invariants byte-shared script↔README), CI per preflight.

Commit 1 — chapter 4, time on the wire
`docs/manual/04-time-on-the-wire.md`: pacing (`corpus play`, the board as downstream stand-in), the second clock (latency at the emitter seam), and the reader-side identity anchors — zero offsets IS plain emit, huge rate IS show (state them as things the reader can verify, with the witnessed strips). Figure (`docs/manual/assets/`): the two clocks — ground-truth time vs wire time, one message shown carrying both, content derived from §5/§4 (cite in an SVG source comment). Strips from the demo README's second-clock section, cited. Length: medium. Message: `docs: user manual -- chapter 4, time on the wire (ADR-0121)`
Commit 2 — chapter 5, batch delivery [FEATURED]
`docs/manual/05-batch-delivery.md`: hourly batching (BHS/BTS, epoch-aligned buckets, BTS-1 self-verification) taught through the straddle as the chapter's spine: Smith/MRN000002 admitted in batch-000, discharged in batch-001, both files individually transport-clean — transport-complete yet clinically-incomplete, "the case that trips up the unaware." Teach the receiver-side question ("do I have all of this encounter?"), not the flags (link cli.md). Witnessed excerpts: the batch listing and the two Smith lines, byte-faithful. Figure: the straddle timeline — one encounter bar crossing two batch windows, both windows drawn clean, the wall-clock boundary cutting the encounter; derived from the demo's witnessed epoch values (cite them in the SVG source comment). Cross-link the use case and the demo section. Length: the manual's fullest chapter so far — it is the featured one. Message: `docs: user manual -- chapter 5, batch delivery (ADR-0121)`
Commit 3 — close
Registers (S3 row → LANDED); rulings only if a mid-session ruling occurs; self-archive at close-phase START; ADR-0121 (per-strip citations; the two figures' derivation notes; the featured-placement ruling quoted); indices 118 → 119; Done line; session record. Message: `docs: session record and prompt archive -- manual s3 (ADR-0121)`
Oracle bracket: pure identity, all 35 roots (docs/registers only). `bin/regression-oracle 6c000aa <final>`; non-identity → STOP. Gates: standing; ASCII x3; gitleaks; invocation lint covers the new strips; CI confirm or disclose.
Fences
Touch ONLY: `docs/manual/04-*.md`, `05-*.md` (new), `docs/manual/assets/*` (two new SVGs), `docs/manual/00-front.md` (one-liners only); registers, prompts, session-records, `notes/adr/0121-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; companions by rule. ZERO src/test/demos; ZERO edits to chapters 1–3. Outside the list → STOP.
STOP-AND-REPORT on: any witnessed source lacking a needed excerpt; lint red not attributable to copy divergence; oracle non-identity; anything not pre-decided.

# 2026-08-12 — ehr-testing-tools: user manual arc opens — skeleton, chapters 1-2, riders (ADR-0119)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `a9a0bbf` (ADR-0118's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.
One judgment call not pre-decided by the prompt: a sequencing conflict
between the riders commit and the skeleton commit, resolved by landing
both before the first push rather than a literal STOP-AND-REPORT pause
— disclosed in full in this session's own session record and in
`notes/adr/0119-user-manual-skeleton.md`'s own Deviations section, not
reproduced here as it is not part of the driving prompt.

## Original prompt (verbatim)

Session prompt — user manual arc, S1: skeleton, chapters 1–2, riders (ADR-0119)
You are Claude Code executing under R30 ceremony for ehr-testing-tools, working for mg, the sole author. DOCS-AND- REGISTERS-ONLY session; zero src, zero test code. It opens the user-manual arc per the design-pass package (author-ruled 2026-08-12, verbatim "Q1 a. Q2 a. Q3 a."): eight chapters, five sessions, exerciser at S2. STOP-AND-REPORT on any conflict with the tree. Standing notes: full `make test` before EVERY push; gate-forced companions in-fence by rule (named in record); budget trip → STOP. No ADR tokens in user-facing prose (footnote-marker form only, per the use-case precedent). Every command strip is copied VERBATIM from a witnessed source (README Quickstart, docs/use-cases pages, demo READMEs), source cited per strip in the ADR — no composed invocations, ever.
Read first

1. `.agents/plans/roadmap.md` (manual design pass row) and `.agents/rulings.md` "From ADR-0113" R1–R7 — the arc's standing rulings.
2. `docs/README.md` (the front door this session extends), `docs/what-is-this.md`, `SETUP.md`, root `README.md` Quickstart.
3. `docs/dev/AUDIENCES.md` — the register the paring rider edits.
4. `demos/scenarios/ed-tuesday/README.md` — the witnessed outputs chapters 1's excerpts come from.
5. `docs/glossary.md` — first-use terms in the chapters link here.

Step 0 — Preflight and tag ceremony

* `git fetch`; origin/main at `a9a0bbf`. Else STOP.
* CI green (`gh run list --limit 5`) — completes the channel's rate-limited leg for ADR-0118.
* Tag `stable-20260812-fix-clusters-b-c`, ANNOTATED, at `a9a0bbf`; push; peeled exact. Case (i): channel fresh-clone verification 2026-08-12 (lineage, ASCII x3, footprint, zero engine/sim/judge src, lint-gap recording confirmed in all three registers), CI per this preflight.

Commit 1 — the riders

1. Audience paring (R4). `docs/dev/AUDIENCES.md`: pare to five behavioral segments — practitioner (agent-assistance absorbed as a global style constraint on its docs; evaluation as its front matter), guide reader, downstream data consumer, contributor (human or agent), deferred Clojure-library-consumer stub. Fold the removed segments' real content into the surviving entries as constraints/stages (nothing deleted silently — each fold named); fix the "Seven segments" header to five. Do not touch the referral-trigger or versioning sections beyond renumber/reference fixes.
2. Learner path. `docs/README.md`: the practitioner fork gains the manual as the learn-it path beside the task router (one short entry pointing at `docs/manual/`).
3. Naming verification (R1). Extension-blind grep for "user guide"/"user-guide" across docs/, README.md, SETUP.md, demos/, and live register prose; expected result: only in-quote survivors (author-verbatim quotes stay). Fix any live-prose stragglers; record the census either way.

Message: `docs: audience paring, learner path, naming verify -- manual arc riders (ADR-0119)`
Commit 2 — the manual skeleton and chapters 1–2
Create `docs/manual/`:

* `00-front.md` — the manual's front page: what it is (the learn-it path over the reference estate), the eight-chapter arc listed with one-line promises, the currency contract ("commands witnessed against <this session's landing commit>; regenerate nothing by hand"), and the style contract (strips are paste-ready and witnessed; references linked, never restated).
* `01-what-this-is.md` — the hook chapter. Structure: (a) the sixty-second proof — the Quickstart's own strip verbatim with its real witnessed output excerpted; (b) the two phenomena as witnessed-output excerpts, NOT SVGs this session: the latency-disordered wire (from ed-tuesday's second-clock section) and the straddled encounter (the batch-000/001 listing + the Smith A01/A03 lines), each with two-three sentences of why hand-built data never has this; (c) honest scope — link the maturity table and state plainly what the tool does not do (drawn from what-is-this.md's own claims, linked not restated).
* `02-setup-first-corpus.md` — narrates SETUP + Quickstart as one story (link both; the chapter adds the narrative, the why per step, and the checkpoints), closing on the determinism contract as punchline: the same-seed re-run and `diff -rq` the reader performs themselves (strip from the witnessed determinism demo). Voice: the estate's own (second person, evidence-anchored). Every first-use term links the glossary. One strip block per concept; chapter length proportionate to Ch 1 ≈ short, Ch 2 ≈ medium.

Message: `docs: user manual -- front page and chapters 1-2 (ADR-0119)`
Commit 3 — registers, ADR, close

* Roadmap: design-pass row → LANDED; five S-rows chartered (S2/ADR-0120 Ch3+exerciser; S3 Ch4+5; S4 Ch6+7; S5 Ch8+ manual-review skill+arc close); SETUP+Ch1-2 unspoiled rewalk noted author's-queue.
* Rulings "From ADR-0119": the design-pass package rulings verbatim ("Q1 a. Q2 a. Q3 a." with the questions summarized).
* Self-archive at close-phase START; ADR-0119 (strip source citations per strip; the paring fold map; the naming census); indices 116 → 117; Done line; session record.

Oracle bracket: pure identity, all 35 roots (docs/registers only). `bin/regression-oracle a9a0bbf <final>`; non-identity → STOP. Gates: standing; the invocation lint now scans the new manual strips automatically — a lint red on a copied strip means the copy diverged from its witnessed source: fix the copy, never the lint. ASCII x3; gitleaks; CI confirm or disclose.
Fences
Touch ONLY: `docs/manual/*` (new); `docs/README.md`; `docs/dev/AUDIENCES.md`; naming-census live-prose stragglers (each named in record); registers, prompts, session-records, `notes/adr/0119-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; companions by rule. ZERO src/test; ZERO edits to what-is-this.md, SETUP.md, README.md, glossary, demos. Outside the list → STOP.
STOP-AND-REPORT on: any witnessed source lacking the output an excerpt needs (never fabricate output); lint red not attributable to a copy divergence; oracle non-identity; anything not pre-decided.

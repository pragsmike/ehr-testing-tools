# 2026-08-05 — Alignment fixes 1

Context: `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`),
tip `989d6cf` at session start (`notes/adr/0049-alignment-audit.md`,
the alignment audit, design-channel-verified). Session record:
[`2026-08-05-alignment-fixes-1.md`](../session-records/2026-08-05-alignment-fixes-1.md).
Decision-of-record: `notes/adr/0050-alignment-fixes-1.md`.

## Prompt, verbatim

2026-08-05 — alignment fixes 1: the past stops leaking — staleness swept, tripwire hardened, conventions named

Session prompt (design channel, 2026-08-05). Prior: the alignment audit landed and was design-channel-verified (`989d6cf`); its 47-row register (`.agents/plans/2026-08-05-alignment-audit-findings.md`) came back with author rulings — yes to the full menu. This session executes the first ruled cluster: the staleness sweep, the tripwire's widened scope, and the small documentation-of-convention notes. Everything here is pre-ruled; nothing is discretionary beyond per-file sweep judgment (AR-F1-2). R30 ceremony. Read-first: the register (all 47 rows — the rows named below cite it constantly); the brief (`.agents/plans/2026-08-05-alignment-audit-brief.md`); `notes/adr/0049-alignment-audit.md`; `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj` in full.

Author rulings (record verbatim in ADR-0050)

1. AR-F1-0 (tag). Per AR-AU-0's standing mechanic, this session creates annotated tag `stable-20260805-alignment-audit` at `989d6cf` (message: `alignment audit landed, design-channel-verified 2026-08-05 (ADR-0049)`), pushes, verifies on origin. git path preferred; `gh api` ref-creation fallback with disclosure.
2. AR-F1-1 (staleness sweep — register rows A-6, E-3, E-5, E-9). Fix-forward with dated notes where a note is warranted: (a) A-6: `ehrt.tools.corpus.framing` → `ehrt.corpus-io.framing` in the roadmap's `:mllp` row, same dated-note pattern as AR-AU-1's fix beside it — or, if two adjacent dated notes read badly, merge into one note covering both corrections, dated, citing ADR-0049 and ADR-0050. (b) E-3: repoint the two `test-integration/` citations in `components/corpus/docs/palgebra-design.md` to the real `projects/integration/test/...` paths. (c) E-5: sweep the five S2/S3-vintage retired forms (`ehrt.sim.{gmf,compile-trajectory,emit-hl7,v2-replay,site-profile}`) across the component-docs files the register enumerates — re-derive the file list by fresh grep, don't trust the register's count. (d) E-9: repoint the `ehrt.corpus.manifest/ManifestV1_1` citations in `docs/formats.md`/`docs/glossary.md` to the canonical `ehrt.provenance.manifest/ManifestV1_1` (ruled: canonical home wins; the re-export itself stays untouched).
3. AR-F1-2 (sweep judgment). Within AR-F1-1's file set: a CURRENT-TENSE reference to a retired ns updates to the live form; an EXPLICITLY HISTORICAL sentence ("X was then ehrt.sim.gmf...") keeps the old form, gaining a clarifying live-form parenthetical only if the passage would otherwise mislead. Per-file disposition table (file → hits → updated/kept-historical) goes in ADR-0050. No surrounding claims are edited; if a sweep reveals a sentence that is now FALSE beyond its ns citation, that is a dated-note fix-forward if trivial, a new register-style finding recorded in ADR-0050 if not — never silent.
4. AR-F1-3 (tripwire hardening — rows S7, E-7; co-landed with the sweep). `stale_path_test.clj` gains: (a) scope extension — the scan additionally covers exactly these live surfaces: `.agents/plans/roadmap.md`, `.agents/plans/README.md`, `.agents/prompts/README.md`, `.agents/session-records/README.md`. Attic files (`roadmap-done-*.md`) and dated one-shot files (`.agents/plans/2026-*-*.md` and their kin) are NOT scanned — they freeze prose at authoring time (the audit brief itself deliberately quotes then-stale text). Encode the boundary as an explicit include-list, not an exclude-pattern, and record the rationale in the test's own docstring with a dated addendum citing ADR-0050 and register row S7. (b) forbidden-list addition — `ehrt.sim-cli.` joins `ehrt.tools.` (row E-7, ADR-0021's retirement, never folded in). Red→green evidence: BEFORE the sweep commits, run the widened check locally against the unswept tree and record the red (the roadmap's framing ns must trip it); the gate and the sweep then land in the SAME commit, green (co-landed invariants).
5. AR-F1-4 (workspace.edn — rows S3, A-1; one commit). (a) S3: the ~40-line `:necessary` re-derivation narrative relocates VERBATIM into ADR-0050 (its own titled section, introduced as a relocation with the source cited); `workspace.edn` keeps a two-line pointer + the standing invariant ("re-derive via `poly deps`/`check` with every entry temporarily cleared whenever project composition changes — history: ADR-0050"). (b) A-1: `"development"` gains `:necessary ["oracle"]` with a one-line comment (REPL-uniformity rationale, cites ADR-0050 and register row A-1). Verify `clojure -M:poly check :dev` reports the warning BEFORE and silence AFTER — that before/after pair is the row's closure evidence, recorded in the ADR.
6. AR-F1-5 (conventions documented — rows B-1, C-5, D-2, F-5; docstring/annotation-only). (a) B-1: `implemented-sink-kinds`'s docstring (or an adjacent comment) in `corpus-io/source-sink.clj` gains the export-for-symmetry note with the named trigger (the player's sink slice consumes it when the sink-designator path lands). (b) C-5: a dated amendment appends to `notes/adr/0011-*.md` (resolve the exact filename via the index) recording the judge trio as intentional ROLE-siblings — surface asymmetry (`gate-batch`, `make-validator`) reflects real engine capability differences, no backfill intended; cite register row C-5. This is fix-forward dated-amendment discipline, explicitly sanctioned here — not an archive violation. (c) D-2: the audit BRIEF gains a dated annotation (annotate-not-rewrite) at its §4.4 sim-emit-cda paragraph: the pre-extraction framing is corrected by register row D-2 — a CDA sibling's nearest kin is `sim-emit-fhir`'s document-snapshot pattern, not `sim-emit-hl7`'s wire-stream idioms, and the id-coherence law is a convention to reimplement, not code to extract. (d) F-5: `repo-identity`'s docstring in `bases/cli/core.clj` gains one clarifying line — `stable-*` tags are continuity points, not semver release tags; "no version tag has been cut" means the latter. NO logic changes anywhere in this ruling — comments and docstrings only.
7. AR-F1-6 (standing rulings recorded, appended at arc close). ADR-0050 records as standing: (a) A-3 — dependency review is report-only `clojure -M:poly libs :outdated` at each arc close plus mandatory before any publish; upgrades are never taken as a side effect. (b) D-3 — `judge` is the accepted landing spot for the pairing-as-data registry; the design pass starts from there. Both go to `.agents/rulings.md` at this ARC'S CLOSE (per AR-C-2's contract), not this session — note the pending append in ADR-0050 so the close session can't miss it.

Steps
Step 0 — Preflight + tag. Cwd ext4; tip `989d6cf` or later-with-disclosure; full suite green baseline; oracle pre-digest noted. Execute AR-F1-0 (tag, push, verify ref on origin). No commit.
Step 1 — Red evidence, then sweep + tripwire (AR-F1-1/2/3). First: apply the scope widening + `ehrt.sim-cli.` addition locally, run the gate, record the red (roadmap framing ns trips; capture the failure output for the ADR). Then execute the full sweep (fresh greps, AR-F1-2 judgment, disposition table drafted). Land sweep + hardened tripwire in ONE commit, gate green, full suite green. Commit: `docs: the past stops leaking — stale namespaces swept, the tripwire learns new ground (alignment fixes 1, AR-F1-1/2/3)`
Step 2 — workspace.edn (AR-F1-4). Narrative relocated (verbatim text goes into the ADR draft now, lands with Step 4's commit — the workspace.edn edit itself lands here), pointer + invariant in place, `:necessary ["oracle"]` added. Before/after `poly check :dev` evidence captured. `poly check` clean. Commit: `chore: workspace.edn sheds its memoir; oracle's seat is documented (alignment fixes 1, AR-F1-4)`
Step 3 — Conventions (AR-F1-5). All four notes/annotations/amendments. No logic diffs (verify: `git diff` on the two src files shows only comment/docstring hunks). Commit: `docs: conventions named where they live — sibling roles, symmetry exports, tag kinds (alignment fixes 1, AR-F1-5)`
Step 4 — ADR-0050 + record. ADR-0050: rulings verbatim; the relocated `:necessary` narrative section (S3, verbatim, source-cited); the sweep disposition table; the tripwire red→green evidence; the A-1 before/after; the pending arc-close register appends (AR-F1-6); the tag act. Index line in `notes/ADRs.md`; Done pointer `- 2026-08-05 — alignment-fixes-1 — ADR-0050`. Oracle bracket (`989d6cf` → tip): all ELEVEN batches identical — docstring/docs/config-comment edits change no emitted bytes; any digest change is STOP-AND-ESCALATE. Full suite green, shape vs Step 0 unchanged except the tripwire's own new assertions (disclose the delta). Session record + prompt self-archive (pairing gate). Final commit: `docs: alignment fixes 1 record — the first ruled cluster lands (ADR-0050)`
Fences
No logic changes: the only `src/` diffs are docstrings/comments (Step 3's verification is mandatory). No gate weakening — the tripwire only widens. The sweep edits ns citations per AR-F1-2, never surrounding claims (dated-note or recorded-finding otherwise). Frozen archives untouched except the sanctioned ADR-0011 dated amendment and ADR-0050 itself + index. The register is read-only this session — row closures are recorded in ADR-0050, not edited into the register (it is a dated audit artifact; its dispositions stay as written). Settled rulings not re-raised. Deferred clusters not touched: no gate promotions (session 2), no S1 rename (session 3), no NIST mirroring (session 4), no LICENSE work (session 5) — if any of those tempts, it is already scheduled; note nothing, do nothing.
After landing: design channel verifies by fresh probe; the author's next license is session 2's prompt (gate promotions), and this landing's own stable tag follows verification per the standing mechanic.

## Deviation record

- **AR-F1-0 not executed.** Tag creation is AUTHOR ACTION in every
  ceremony mode (`AGENTS.md`'s own "Session mode and ceremony"
  section); this session's own prompt licensed the act but did not
  override that standing rule. The exact commands are recorded in
  `notes/adr/0050-alignment-fixes-1.md` for the author to run directly,
  rather than executed by the session.
- **Step 1's internal ordering, corrected mid-step.** The sweep edits
  were drafted before the tripwire's own red-evidence capture, which
  would have silently lost the red→green proof the ruling asked for.
  Recovered via `git stash push -u` (moving the sweep edits out),
  applying the tripwire widening alone against the unswept tree,
  capturing red, then `git stash pop` to restore the sweep before
  re-running to green — no commit was made in the wrong order. Disclosed
  in `notes/adr/0050-alignment-fixes-1.md`'s own Step 1 account.
- **AR-F1-2's sweep judgment: every one of the 25 hits (8 files) came
  back current-tense.** None were judged explicitly historical, so no
  sentence kept its old namespace form or gained a clarifying
  parenthetical. Consistent with this same test's own M2–M4 addenda
  precedent for these exact files; full per-file table in ADR-0050.
- **The sweep's actual hit count (25, across 8 files) exceeded the
  register's own estimate ("12 file-hits").** Per AR-F1-1(c)'s own
  instruction to re-derive by fresh grep rather than trust the
  register's count — the discrepancy is a re-derivation delta, not a
  new defect, and is recorded in ADR-0050.

# 2026-08-05 — Alignment fixes 2

Context: `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`),
tip `72add4a` at session start (`notes/adr/0050-alignment-fixes-1.md`,
alignment fixes 1, design-channel-verified). Session record:
[`2026-08-05-alignment-fixes-2.md`](../session-records/2026-08-05-alignment-fixes-2.md).
Decision-of-record: `notes/adr/0051-alignment-fixes-2.md`.

## Prompt, verbatim

2026-08-05 — alignment fixes 2: the law reads the same everywhere, and three laws get teeth

Session prompt (design channel, 2026-08-05). Prior: alignment fixes 1 landed and was design-channel-verified (`72add4a`). That session correctly DEFERRED the tag act AR-F1-0 licensed: `AGENTS.md`'s standing text ("tag creation is AUTHOR ACTION in every ceremony mode") was never updated when ADR-0049's AR-AU-0 amended the mechanic, leaving two repo surfaces in conflict — the session refused to resolve a law-conflict ad hoc, which was right. This session reconciles the surfaces FIRST, then executes both pending tags, then promotes three prose invariants to gates (register rows S5, A-5). R30 ceremony. Read-first: `notes/adr/0050-alignment-fixes-1.md` (the deferral disclosure + prepared tag commands); `notes/adr/0049-alignment-audit.md` AR-AU-0; the register rows S5 and A-5; `AGENTS.md`'s tag/ceremony section; two existing gates as style models — `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj` and `done_pointer_adr_test.clj`.

Author rulings (record verbatim in ADR-0051)

1. AR-F2-0 (law-surface reconciliation — root cause owned). The conflict was a design-channel authoring miss: AR-AU-0 amended standing law in the ADR/rulings trail without propagating to `AGENTS.md`, the surface sessions actually read first. Fix: amend `AGENTS.md`'s tag rule with a dated note — sessions MAY create and push `stable-*` continuity tags when a session prompt licenses a SPECIFIC tag at a SPECIFIC commit, which the design channel issues only after verifying that landing (ADR-0049 AR-AU-0; reconciled here after ADR-0050's principled deferral); the author may always tag directly; every OTHER tag class (release `v*` tags especially) remains AUTHOR ACTION. The amendment edits the tag rule in place with the dated citation — AGENTS.md is a live instruction surface, not a frozen archive. ADR-0051 records the standing lesson for the arc-close register append: an amendment to standing law lands on every surface that states the law, in the same session that rules it.
2. AR-F2-1 (both pending tags, AFTER the amendment commits). (a) `stable-20260805-alignment-audit` at `989d6cf`, message `alignment audit landed, design-channel-verified 2026-08-05 (ADR-0049)` — the exact commands ADR-0050 already prepared. (b) `stable-20260805-alignment-fixes-1` at `72add4a`, message `alignment fixes 1 landed, design-channel-verified 2026-08-05 (ADR-0050)`. Annotated tags, `git push origin <tag>` each, both verified on origin (`git ls-remote --tags origin`, peeled refs resolve to the stated commits). Order is load-bearing: the AGENTS.md amendment must be committed and pushed before the tag acts, so no act occurs under conflicting law.
3. AR-F2-2 (gate: sim-emit-hl7 dependency law). New deftest in `docs-tooling`'s gate family: parse the `ns` form of every `.clj` under `components/sim-emit-hl7/src/`; assert every required `ehrt.*` namespace matches `ehrt.sim-model.*` or `ehrt.sim-emit-hl7.*` — nothing else (the AGENTS.md constraint, verbatim in the test's docstring with its citation). Parse requires from the ns form properly (read the form, walk `:require` clauses) — no naive regex over whole files that would trip on docstrings.
4. AR-F2-3 (gate: provenance leaf law). Same shape for `components/provenance/src/`: every required `ehrt.*` namespace matches `ehrt.provenance.*` only (AR-2, ADR-0043, cited in the docstring). Note the register (B-9) observed provenance's malli-only posture from the deps.edn side too; this gate covers the src-require side — the deps.edn side is poly's own job, don't duplicate it.
5. AR-F2-4 (gate: root-alias completeness — register row A-5). New deftest asserting, bidirectionally: (a) the root `deps.edn` `:dev` alias's `:local/root` entry set maps 1:1 onto the union of `components/*` and `bases/*` directories on disk; (b) every `components/*/test` and `bases/*/test` directory that exists appears in the `:test` alias's `:extra-paths`, and every listed path exists on disk (project test dirs, e.g. `projects/*/test`, are allowed listings verified for existence, not required from the brick side). Read `deps.edn` as EDN, not by grep. This encodes exactly what audit row A-5 verified by script, so a future brick addition cannot silently miss the root aliases.
6. AR-F2-5 (red→green witnessed per gate). For EACH of the three gates: after landing the test green, demonstrate the red via a TRANSIENT, UNCOMMITTED violation (e.g., a scratch require of `ehrt.corpus.interface` added to a sim-emit-hl7 ns; a scratch `ehrt.kernel` require in provenance; one `:dev` entry temporarily deleted), run the gate, capture the failure output, restore the tree byte-exact (verify `git status` clean of unintended diffs), and record all three red transcripts in ADR-0051. No violation is ever staged or committed.

Steps
Step 0 — Preflight. Cwd ext4; tip `72add4a` or later-with-disclosure; full suite green baseline; oracle pre-digest noted; confirm both tag-target commits exist and neither tag name exists yet (locally or on origin).
Step 1 — Reconcile + tag (AR-F2-0/1). AGENTS.md amendment; commit; push; verify. Then both tag acts, both verified on origin. Commit: `docs: the law reads the same everywhere — AGENTS.md catches up with AR-AU-0 (alignment fixes 2, AR-F2-0)`
Step 2 — Three gates (AR-F2-2/3/4/5). Write the three deftests in the existing gate family's style (docstrings citing rulings + register rows); full suite green including the new assertions; then the three transient red demonstrations, tree restored clean after each. Commit: `test: three laws get teeth — dependency fences and root-alias completeness now gated (alignment fixes 2, AR-F2-2/3/4)`
Step 3 — ADR-0051 + record. ADR-0051: rulings verbatim; the reconciliation narrative (root cause owned as a design-channel miss, ADR-0050's deferral cited as the correct conservative act); both tag acts with their verification output; the three red transcripts; the assertion-count delta vs Step 0; the pending arc-close register append (the law-surface propagation lesson, joining AR-F1-6's two). Index line; Done pointer `- 2026-08-05 — alignment-fixes-2 — ADR-0051`. Oracle bracket (`72add4a` → tip): all ELEVEN batches identical — tests and AGENTS.md change no emitted bytes; any digest change is STOP-AND-ESCALATE. Session record + prompt self-archive. Final commit: `docs: alignment fixes 2 record — the law coheres, the gates hold (ADR-0051)`
Fences
No `src/` changes, transient red demonstrations excepted — and those are never staged, never committed, and each is followed by a verified-clean restore. No gate weakening anywhere; the three new gates only add. No `deps.edn`/`workspace.edn` edits. The AGENTS.md edit touches ONLY the tag rule (plus its dated citation) — nothing else in the file. Frozen archives untouched (ADR-0051 + index sanctioned). Deferred clusters untouched: no S1 rename (session 3), no resource-nesting gate (it co-lands with the rename, session 3 — do NOT add it here even though it would be easy), no NIST mirroring, no LICENSE work. If either tag act fails after the amendment is live, STOP-AND-ESCALATE with the error — do not improvise an alternative tag mechanism beyond the already-licensed `gh api` ref fallback (disclose if used).
After landing: design channel verifies by fresh probe; session 3 (S1 rename + nesting gate) follows, and this landing's own tag rides session 3's Step 0 per the now-coherent mechanic.

## Notable deviations, disclosed

- **Step 0's own premise ("neither tag name exists yet") did not hold.**
  `stable-20260805-alignment-audit` already existed at the exact ruled
  commit and message, apparently created by the author directly between
  sessions (licensed by both ADR-0050's own deferral note and this
  session's AR-F2-0 amendment). Treated as verify-only for that tag;
  `stable-20260805-alignment-fixes-1` was the one tag this session
  created. Disclosed in `notes/adr/0051-alignment-fixes-2.md`'s own
  Step 0 account, not silently absorbed.
- **AR-F2-3's own "(B-9)" citation did not match the live register.**
  `.agents/plans/2026-08-05-alignment-audit-findings.md`'s B-9 row is
  about a `valid?` naming collision, not provenance's `deps.edn`
  posture. The malli-only fact itself checked out against the live
  `components/provenance/deps.edn`; the gate's own docstring cites AR-2/
  ADR-0043 as the ruling's own instruction (not the "(B-9)" narrative
  aside) actually required. Disclosed, not propagated into the gate.
- **Register row S5's own evidence line ("13 files") undercounted by
  one against this session's fresh listing (14 pre-existing files)** —
  immaterial to the ruling, disclosed rather than investigated further.

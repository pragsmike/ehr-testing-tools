# 2026-08-10 — ehr-testing-tools: marker-only footnotes, full user path (build session)

## Context

Archived 2026-08-10. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `062df94` (ADR-0101's own close) and closed at
the conversion commit (`3880a6b`) plus this record's own close-phase
commit. Original prompt follows verbatim; a deviation record follows
that.

## Original prompt (verbatim)

Session prompt -- marker-only footnotes, full user path (ADR-0102)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session executes three author rulings from 2026-08-10
(recorded verbatim below): the footnote form goes MARKER-ONLY (the
visible ADR-NNNN tokens leave user-facing prose), the scope widens to
origin-qualified citations (the full user path), and the :mllp sink is
abandoned for now (a rulings/close item plus a one-line help-text
accuracy fix -- no transport work). HEAD at handoff: 062df94. This
session's ADR is ADR-0102.

State of the tree (channel-probed, verify-then-act):
- ADR-0101 landed append-in-place form: visible token + marker
  (`ADR-0013[^adr-0013]`), 27 sites across six hand-authored docs and
  six generated pages (via components/corpus/docs/use-cases.edn).
  The author has now ruled that form insufficient: markers only,
  tokens out of prose.
- 12 origin-qualified citations (`sim/ADR-NNNN`, `tools/ADR-NNNN`)
  remain visible in the user path, untouched by ADR-0101's bare-only
  scope -- including docs/what-is-this.md (2 sites), which enters the
  sweep for the first time. Their definitions target the frozen
  pre-merge indexes: `../notes/sim/ADRs.md` / `../notes/tools/ADRs.md`
  (both exist; use-cases pages need `../../`). Distinct definition
  names avoid collision with same-numbered current records:
  `[^sim-adr-NNNN]` / `[^tools-adr-NNNN]`.
- 5 generated-page citation sites inside code-fence comments stay
  bare (footnote syntax cannot render there; ADR-0101's own finding).
- The two glossary citation-ACCURACY anomalies (Baseline and Pack
  entries, ADR-0101's disclosed finding with revisit trigger "a
  future citation-accuracy audit"): this session's conversion of
  those exact sites IS the trigger firing -- authoring their
  definitions requires choosing a target, and knowingly authoring a
  wrong one would manufacture fresh error. Per each entry's own
  parenthetical ("tools' pre-merge sequence"), their definitions
  target `../notes/tools/ADRs.md` with `[^tools-adr-NNNN]` names.
  Disclosed as closing ADR-0101's anomaly items 1-2, cited by name.
- The gate (link_footnote_gate_test.clj) currently checks link
  resolution and marker/definition parity; it does NOT yet forbid
  ADR tokens in prose -- this session hardens it to enforce the new
  ruling.
- Play's help text claims "dir:, blaze:, and mllp: are recognized but
  deferred" -- mllp: is NOT in the sink-URL grammar (it would parse
  :unknown-sink-scheme, not recognized-rejected), and the author has
  now abandoned the :mllp sink. One-line accuracy fix.

Oracle bracket, with its reasoning: pure identity on all 34 roots is
EXPECTED -- the footprint is markdown, the EDN docsgen source, one
test file, and one help.clj DOC-STRING line (no logic; docs/cli.md
regenerates). No oracle-path namespace is touched. Movement =
STOP-AND-REPORT.

## Read first

- notes/adr/0101-adr-footnotes.md -- the landed form being revised,
  the fence-comment finding, the two anomaly items (verbatim), the
  reworded-sentences precedent
- notes/ADRs.md header -- the citation rule (bare vs origin-qualified)
- docs/glossary.md, docs/formats.md, docs/judge-calibration.md,
  docs/site-profiles.md, docs/locators.md, docs/what-is-this.md
- components/corpus/docs/use-cases.edn
- components/docs-tooling/test/ehrt/docs_tooling/
  link_footnote_gate_test.clj
- bases/cli/src/ehrt/cli/help.clj -- the play --sink doc line
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-10, author verbatim: "Let's abandon :mllp for now."
  No transport work, no design-doc amendment. The close records the
  ruling AND the design-channel finding beside it: docs/dev/
  source-sink-design.md D2/D3 ("no socket code enters this repo"; no
  :mllp sink kind) contradicts ADR-0014's "future :mllp sink"
  deferral language and play's help text -- the ruling leaves D2/D3
  governing, with ADR-0014's contrary language noted as superseded-
  in-part by this ruling (inventory of the three places the
  mllp-transport phrasing lives goes in the ADR).
- [A] 2026-08-10, author verbatim: "For footnotes, do b, marker-only
  form. I don't want ADRs cluttering user-facing prose." Every
  footnoted citation site drops its visible ADR-NNNN token; the
  marker alone remains.
- [A] 2026-08-10, scope, author verbatim "a" (to: footnote the
  origin-qualified citations too): the FULL user path goes
  marker-only, origin-qualified included, definitions targeting the
  frozen pre-merge indexes.
- [C] Channel-specified, verify rendering before mass-applying:
  where the token lifts out cleanly, delete `ADR-NNNN` and keep the
  marker; where the citation is grammatically load-bearing, reword
  minimally with a generic referent ("the design record's own
  identity[^adr-0013][^adr-0014]"), technical claims identical,
  EVERY reworded sentence listed before/after in the ADR. Compound
  and parenthetical citation shells (`(notes/ADRs.md ADR-NNNN)`)
  collapse to the marker; empty parentheses never remain. Definition
  lines keep the linked ADR-NNNN text (that is where the number
  belongs now). R34 voice rules hold.
- [C] The glossary anomaly-target fix per Context, flagged to the
  author pre-session; if the author vetoed it before this session
  runs, the driving conversation will say so -- absent that, proceed
  as chartered.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0101 landing at `062df94` by fresh
   public clone. Tag `stable-20260810-adr-footnotes` at `062df94`,
   push, verify the peeled ref. Remote moved = STOP-AND-REPORT.

2. **Re-derive the inventory.** Fence-aware, definition-aware scan of
   docs/ proper (excluding docs/dev/) and the use-cases EDN: every
   remaining visible ADR token in prose, split bare-footnoted /
   origin-qualified / fence-comment. Reconcile against 27 / 12 / 5;
   disclose differences. The ADR's own table.

3. **Harden the gate first (red proven on the CURRENT tree).** Extend
   link_footnote_gate_test: no `ADR-\d{4}` token (any qualification)
   in prose -- outside code fences, off footnote-definition lines --
   anywhere in docs/ proper. This check is RED on the pre-conversion
   tree (the append-in-place tokens violate it): run it, paste the
   red verbatim into the ADR as the non-vacuity witness, and land the
   hardened gate in the SAME commit as the conversion that turns it
   green. Definition-name/marker parity extends to the new
   `[^sim-adr-*]`/`[^tools-adr-*]` families.

4. **Convert, one commit with the hardened gate.** Hand-authored
   files: every footnoted site to marker-only; every origin-qualified
   site to marker + frozen-index-targeted definition; the two
   glossary anomaly entries per Context. EDN prose likewise;
   `make use-cases` regenerate; diff-confirm only the expected
   generated pages moved (index and cli.md byte-identical THIS step).
   help.clj's play --sink doc line drops the mllp: claim (reword to
   name only dir:/blaze: as recognized-deferred); `make docsgen`
   (cli.md legitimately moves in THIS step only, that one line).
   Read one regenerated page end to end. Gate + docs-tooling suite
   green.
   Commit message (ASCII only):
   `docs: user-path citations go marker-only, full path, gate hardened (ADR-0102)`

5. **Oracle bracket.** Expected pure identity per Context; movement =
   STOP-AND-REPORT.

6. **Full gate.** poly check, full local suite, CLI parse-guard lint,
   bin/verify-nist-lock, lint-pipeline.

7. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0102
   (inventory, reworded-sentences list, the gate's red witness, the
   mllp contradiction inventory, the anomaly closure citing
   ADR-0101 items 1-2, deviations dated); roadmap: no mllp row
   existed -- record the abandonment under the rulings section
   instead, and note ADR-0014's deferral language as ruled-superseded
   -in-part; .agents/rulings.md records all three 2026-08-10 rulings
   verbatim (":mllp abandoned for now" / "b, marker-only" /
   "a" origin-qualified widening); notes/ADRs.md index row;
   notes/adr/README.md count 99 -> 100; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- marker-only footnotes, mllp ruling (ADR-0102)`

8. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: the named docs/ files plus any user-path file the
  re-derived inventory adds, docs/use-cases/*.md via regeneration,
  docs/cli.md via regeneration (the one help-line change),
  components/corpus/docs/use-cases.edn (prose only),
  bases/cli/src/ehrt/cli/help.clj (the one --sink doc line),
  link_footnote_gate_test.clj, notes/adr/0102-*.md, notes/ADRs.md,
  notes/adr/README.md, .agents/* close-phase files. The sweep RULE
  governs over this list (ADR-0099 precedent).
- docs/dev/ untouched. Generated pages never hand-edited. No logic
  changes anywhere; the help.clj edit is a doc string.
- docs/dev/source-sink-design.md and notes/adr/0014-*.md are NOT
  edited -- the mllp supersession is recorded in ADR-0102 and
  rulings.md, never by rewriting settled records (frozen-archives
  discipline; the design doc's D2/D3 stand as written and governing).
- Technical claims in every touched sentence unchanged.
- No history rewrites; deviations dated in the ADR;
  STOP-AND-REPORT over improvisation.
- Channel claims are verify-then-act.

## Deviations from the driving prompt

- **The prompt's own claim "roadmap: no mllp row existed" was checked
  and found wrong.** `.agents/plans/roadmap.md`'s Deferred section
  already carried a "Corpus player `:mllp` transport sink" row from
  ADR-0014's own original deferral. Since `:mllp` is now abandoned
  rather than merely still-deferred, that row closes in place this
  session (a dated note appended, pointing at the Done section's own
  one-line pointer -- the same pattern ADR-0100's close used), rather
  than the prompt's own instruction to skip roadmap entirely and
  record the abandonment only in `.agents/rulings.md`'s own section.
  Both actually happened: the Deferred row closes AND
  `.agents/rulings.md` gains its "From ADR-0102" section, satisfying
  the prompt's own substance (the ruling is recorded there, verbatim)
  while also correcting the roadmap to not silently misrepresent a
  live row as still-open. Disclosed in ADR-0102, not silently
  corrected.
- No other deviations from the driving prompt's own steps, fences, or
  rulings.

# 2026-08-10 — ehr-testing-tools: relocate test-fixtures to root (build session)

## Context

Archived 2026-08-10. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `8d4b1ee` (permission legs and bare flags,
ADR-0098) and closed at the move commit plus this record's own commit.
Original prompt follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt -- fixture relocation (ADR-0099)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session executes the roadmap Next row "Fixture
relocation" (2026-08-08, fidelity riders, ADR-0081, author backlog
addition), ruled 2026-08-09: the ENTIRE `components/corpus/test-fixtures/`
tree moves to a root-level `test-fixtures/` home. HEAD at handoff:
8d4b1ee. This session's ADR is ADR-0099.

The tree is 15 files across four subtrees: `v2/` (five hand-authored
`.hl7` fixtures plus `simhospital/` -- LICENSE, PROVENANCE.md,
messages.out), `v2-nist/` (NOTICE.md, `covidELR/` message, the
three-file `COVID19_ELR-v2.3.1` bundle), `fhir/`
(storefront-patient.json), and `reports/` (pre-split-baseline.edn --
a fourth member beyond the roadmap row's named three, riding along by
author ruling Q2 a., disclosed in the ADR).

This is a move-don't-improve session in its purest form: every fixture
file's BYTES are identical before and after; the only content edits
anywhere are path-text citations. The ADR-0073 demos-front-door
mechanic governs (`notes/adr/0073-*.md` AR-DM-1): `.gitattributes`
patterns move in the SAME commit as the files they protect,
byte-witnessing before and after, pointer-README stub in the vacated
directory, live docs swept, frozen archives untouched.

Channel-probed facts (verify-then-act, as always):
- Five `.gitattributes` `-text` patterns name the old paths (v2/*.hl7,
  v2/simhospital/messages.out, v2/simhospital/LICENSE,
  v2-nist/covidELR/*.txt, v2-nist/COVID19_ELR-v2.3.1/**).
- `notice-verbatim-test` resolves hashed files RELATIVE to each NOTICE
  file's own directory, so a subtree-whole move keeps hash
  verification working with zero table edits. NOTICE.md and
  PROVENANCE.md carry PROSE citations of the old paths (their
  .gitattributes cross-references) that update as text.
- The live-path lint's allowlist (`test_source_live_path_lint_test.clj`)
  checks `str/starts-with?` against `["test-fixtures" ...]` -- with the
  root `test-fixtures/` home, the existing entry matches new-path
  literals AS-IS. Expected: zero allowlist edit; verify by running the
  lint, and if it demands an edit, make the minimal one and disclose.
- The reference sweep: 16 `.clj` files (63 lines), including TWO src
  citations -- `bases/cli/src/ehrt/cli/help.clj` (the `--profile`
  flag doc) and `bases/cli/src/ehrt/cli/core.clj` (the v2-nist hint
  string) -- so `docs/cli.md` regenerates via `make docsgen` this
  session, legitimately. Plus: README.md demo commands,
  AUTHORS-GUIDE.md, four `docs/use-cases/*.md` files, `bin/ehrt`'s
  comment, `bin/quickstart-demo`'s live `gate v2` invocation.
- `stale_path_test` is an enforcement ally: it should catch any missed
  live-doc citation. Trust its red, not this list's completeness.

Oracle bracket, with its reasoning (earned from the caller set): pure
identity on all 34 roots is EXPECTED -- no logic changes anywhere (the
two cli src edits are doc-string literals), no sim/kernel/corpus/
engine src touched, and every fixture's bytes are witnessed identical.
Any digest movement is STOP-AND-REPORT.

## Read first

- `.agents/plans/roadmap.md` -- the Fixture relocation Next row, verbatim
- `notes/adr/0073-*.md` -- AR-DM-1 (the mechanic this session reuses:
  same-commit .gitattributes, byte-witness table, pointer READMEs,
  sweep enumeration, reading-sets check)
- `notes/adr/0081-*.md` -- the row's origin
- `.gitattributes` -- all five patterns and their comments
- `components/corpus/test-fixtures/v2/simhospital/PROVENANCE.md` and
  `components/corpus/test-fixtures/v2-nist/NOTICE.md` -- the prose
  citations that update
- `components/docs-tooling/test/ehrt/docs_tooling/
  test_source_live_path_lint_test.clj`, `notice_verbatim_test.clj`,
  `stale_path_test.clj`, `license_text_pointer_test.clj`
- `.agents/rulings.md` -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-09, target home, author verbatim "Q1 a.": the new home
  is root-level `test-fixtures/`.
- [A] 2026-08-09, scope, author verbatim "Q2 a.": the ENTIRE tree
  moves -- all four subtrees, including the roadmap-unnamed `reports/`
  -- disclosed in the ADR as riding beyond the row's named members.
- [A] The roadmap row itself, including the wrinkles it names
  (.gitattributes protection, demos-front-door mechanic, lint blessed
  roots).
- [C] Channel-inferred, verify before acting: everything under
  "Channel-probed facts" above, including the zero-edit allowlist
  expectation and the 16-file/63-line sweep inventory.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0098 landing at `8d4b1ee` by fresh
   public clone. Tag `stable-20260809-permission-legs-and-bare-flags`
   at `8d4b1ee` and push the tag. Verify HEAD is 8d4b1ee first; if
   the remote has moved, STOP-AND-REPORT.

2. **Pre-move witness.** sha256sum all 15 files under
   `components/corpus/test-fixtures/`, recorded for the ADR's witness
   table. `git check-attr text` on every file matched by the five
   `-text` patterns -- record the (unset) results.

3. **The move, one commit.** `git mv components/corpus/test-fixtures
   test-fixtures` (subtree-whole, preserving NOTICE-relative
   resolution). In the SAME commit:
   - Rewrite the five `.gitattributes` patterns to the new root paths,
     comments' own path mentions included.
   - Update NOTICE.md's and PROVENANCE.md's prose path citations
     (hash tables untouched -- they name files relative to
     themselves).
   - Sweep the 16 `.clj` files' path literals (63 lines; re-derive
     the list by grep, don't trust this count).
   - `make docsgen` (docs/cli.md follows help.clj).
   - Sweep live docs: README.md, AUTHORS-GUIDE.md, the four
     `docs/use-cases/*.md`, `bin/ehrt` comment,
     `bin/quickstart-demo`. Frozen archives (`notes/adr/`,
     `.agents/prompts/`, `.agents/session-records/`, ADR history in
     `notes/ADRs.md`) untouched.
   - Pointer README at `components/corpus/test-fixtures/README.md`
     (ADR-0073 stub style: where it went, which ADR).
   - Confirm `.agents/reading-sets.edn` carries no member path under
     the moved tree (ADR-0073 checked its trees; check yours).
   No fixture file's bytes change. No logic changes.
   Commit message (ASCII only):
   `refactor: relocate test-fixtures to root -- demos front door mechanic (ADR-0099)`

4. **Post-move witness.** Re-run sha256sum on all 15 files at their
   new paths -- identical to Step 2, table into the ADR.
   `git check-attr text` at all new protected paths -- still unset.
   A fresh `git stash`-free working tree check: `git status` clean
   except expected.

5. **Lint verification.** Run the live-path lint, notice-verbatim,
   provenance-leaf-law, stale-path, license-text-pointer, and
   quickstart-fresh tests explicitly. Expected: all green with ZERO
   edits to the lint allowlist. If any demands an edit, make the
   minimal one, same commit if not yet pushed or fix-forward if it
   is, and disclose in the ADR.

6. **Oracle bracket.** `bin/regression-oracle` from this session's
   opening tag to the move commit. Expected pure identity on all 34
   roots per the Context reasoning. Movement = STOP-AND-REPORT.

7. **Full gate.** `poly check`, full local suite,
   CLI parse-guard lint, `bin/verify-nist-lock` -- all green.

8. **Close phase.** FIRST: self-archive this prompt to
   `.agents/prompts/`. Then: ADR-0099 with both witness tables
   verbatim (ADR-0073 style), the `reports/` rider disclosure, and
   any deviations dated; roadmap row to Done; `.agents/rulings.md`
   records the two 2026-08-09 rulings ("Q1 a." / "Q2 a.");
   `notes/ADRs.md` index row; `notes/adr/README.md` count 96 -> 97;
   session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- fixture relocation (ADR-0099)`

9. **Push and verify.** Push at each checkpoint per R30. Post-push,
   ASCII check FIRST on every commit message this session created
   (`git log --format=%B -1 <sha> | LC_ALL=C grep -n '[^ -~]'`,
   expected empty), then CI confirmation.

## Fences

- Touch ONLY: the moved tree (old and new paths), `.gitattributes`,
  the swept `.clj` files (path literals only, no logic), `docs/cli.md`
  (generated), README.md, AUTHORS-GUIDE.md, `docs/use-cases/*.md`,
  `bin/ehrt` (comment line only), `bin/quickstart-demo` (path only),
  the pointer README, `notes/adr/0099-*.md`, `notes/ADRs.md`,
  `notes/adr/README.md`, `.agents/*` close-phase files.
- NO fixture file's bytes change -- the witness tables are the proof.
- NO logic changes anywhere; every code edit is a path string.
- Nothing in kernel/sim/engine/oracle/judge src beyond the two named
  cli doc-string literals.
- Frozen archives untouched.
- No history rewrites; deviations in the ADR's dated appendix;
  STOP-AND-REPORT over improvisation.
- Channel claims (file counts, line counts, lint semantics, pattern
  lists) are verify-then-act: re-derive from the tree before relying
  on them.

## Deviations, disclosed

- **The driving prompt's own sweep inventory was incomplete.** A fresh
  repo-wide grep (`grep -rl "components/corpus/test-fixtures" .
  --exclude-dir=.git`) surfaced 59 files, not the 16 `.clj` files the
  prompt named. The 16-`.clj`-file count itself was exactly right
  (re-derived independently, identical file set); what the prompt's
  own inventory missed was thirteen additional LIVE, non-`.clj` files,
  never named as part of the sweep and outside the prompt's own stated
  "Touch ONLY" fence. Most consequential:
  `components/judge/resources/judge/pairing-registry.edn` — a RUNTIME
  resource whose twelve `:fixture`/`:profile` path literals are the
  actual paths `judge`'s own registry loads fixtures from at test time,
  not documentation. Leaving it unswept would not have been a
  doc-staleness gap; it would have broken every pairing-registry-driven
  judge test the moment the fixture tree moved. The fence was widened
  to include it and twelve other live citations (full list in
  ADR-0099's own Sweep enumeration), landed in the SAME move commit,
  and disclosed here rather than either (a) silently deferring to the
  prompt's own narrower fence and shipping a real defect, or (b)
  stopping the session over a scope question the prompt's own
  "verify-then-act" instruction for channel claims already answers.
  Every added edit is a path-string literal — no logic changed
  anywhere, consistent with the prompt's own fence intent even though
  the fence's own literal file list undercounted.
- **`docs/use-cases/*.md`**: the prompt's own claim of "four" affected
  pages was stale; the live count (confirmed by grep before
  regeneration and by `git status` after) is six. `make use-cases`
  regenerated exactly those six, confirmed by `git status` showing no
  other page changed.
- **One PROVENANCE.md citation missed on the first edit pass**
  (`test-fixtures/v2/adt-a01-admit-repeated-identifiers.hl7`'s own
  repetition-preservation cross-reference), caught by the mandatory
  post-edit repo-wide grep before staging, fixed before the commit
  landed — not a post-hoc fix-forward, since it never reached a
  commit.

No other deviations from the driving prompt's own steps, fences, or
rulings. Every channel-inferred claim in the prompt (the five
`.gitattributes` patterns, `notice-verbatim-test`'s relative-resolution
behavior, the live-path lint's allowlist mechanics) was verified
against the live tree before being built on and held exactly as
stated.

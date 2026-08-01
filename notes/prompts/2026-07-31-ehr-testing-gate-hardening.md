# 2026-07-31 — ehr-testing-tools: gate hardening — two quietly-lying gates

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `31675e6`
("refactor: tools becomes corpus and retires (split stage 3, ruled
2026-07-31)"), already equal to `origin/main` — no fast-forward
needed. No commit or push run by this session; the tree is left
uncommitted, coherent, with the proposed commit message printed in
the session's close-out. `/mnt/c` clone not touched (all edits made
via the UNC path onto the WSL ext4 clone, per the
dual-clone-edit-hazard discipline).

## Original prompt (verbatim)

2026-07-31 — ehr-testing-tools: gate hardening — two quietly-lying gates
Context
Post-split cleanup, small session. Stage 3 of the `tools` split (ADR-0018) surfaced two enforcement gates that now pass vacuously: palgebra's deps-lint still polices the retired `ehrt.tools.*` prefix, so the ADR-0002 self-containment invariant is currently unenforced for the real component names; and the structure-currency test (added 2026-07-31, catch-up batch) matches brick names by substring, so `corpus` ⊂ `corpus-io` made its stage-3 red moment impossible, and it checks presence only — a retired component's row would never trip it. A gate that silently passes on everything is worse than a missing gate. This session makes both gates mean what they claim, hardened against the class of failure (renames), not just today's instance.
Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `31675e6`), record HEAD. No commit/push; per-push lane at the end; `/mnt/c` untouched (fast-forward it only if you are working there, which you should not be).
Read first

1. `components/palgebra/src/ehrt/palgebra/deps_lint.clj` and its test; `notes/ADRs.md` ADR-0002 (the invariant this gate encodes).
2. `components/docs-tooling/test/ehrt/docs_tooling/structure_currency_test.clj`; `AGENTS.md` and `docs/dev/architecture.md` (the surfaces it polices — note their exact table/token formats before writing matchers).
3. ADR-0018's named-futures list (both items originate there).

Author rulings

* AR-1 deps-lint becomes an allowlist. Denylists of forbidden prefixes rot on every rename — that is exactly how this gate went vacuous. Derive palgebra's actual current require set empirically (expected: `ehrt.palgebra.*` internals only, per ADR-0002's self-containment claim — if reality shows more, stop and escalate with the edges named rather than allowlisting them silently). Encode: any `ehrt.*` require from `ehrt.palgebra.*` src outside the allowlist fails, whatever its name — future components are forbidden by default, no maintenance needed. Update the docstring to describe the allowlist and cite this session's ADR note; ADR-0002 gets a dated amendment note (fix-forward, original text stays).
* AR-2 structure-currency matches exact tokens, both directions. Presence: every `components/*` and `bases/*` directory name appears in both surfaces as an exact token (backtick-delimited or table-cell — derive the matcher from the surfaces' real formats, and make it immune to the substring trap: with `corpus` removed and `corpus-io` present, the test MUST fail). Absence: every component-shaped token in the two surfaces' structure tables must exist on disk — this catches retired names (`tools`) with no denylist to maintain. Scope the reverse check to the structure tables/sections only, not prose — historical narrative legitimately names dead components.
* AR-3 Test-first, both gates, both directions. Required red evidence, seeded then reverted: (a) deps-lint — a temporary forbidden require in a palgebra src ns → red; (b) structure presence — remove `corpus` from AGENTS.md while `corpus-io` remains → red (this is the exact stage-3 vacuous case; it failing red is the point of the session); (c) structure absence — add a `tools` row to a structure table → red. Green on the untouched tree before and after. Record all three red→greens.
* AR-4 Fence. Nothing else: no producer-name literal (awaiting ruling), no test-tree renames (awaiting bless), no other named-futures, no doc sweeps beyond the two docstrings/notes this session's own changes require.

Steps

1. Characterize both gates' current matchers and the two surfaces' token formats; derive palgebra's require set; escalate per AR-1 if it exceeds self-containment.
2. Rewrite both gates per AR-1/AR-2; red→green evidence per AR-3.
3. Records: dated ADR note (one entry covering both gates, citing ADR-0018's named-futures as origin), facts-register row + Index same commit, archive this prompt at `notes/prompts/2026-07-31-ehr-testing-gate-hardening.md` with deviation record. Per-push lane green; integration lane not needed (dev-time gates only) — state that explicitly rather than running it silently short.

Proposed commit message: `fix: harden two vacuous gates -- palgebra deps-lint becomes an allowlist (renames forbidden by default), structure-currency matches exact tokens both directions (retired names now trip it); red evidence for all three failure modes`
Close-out summary for the author
HEAD at start; palgebra's derived require set; the matcher formats chosen and why; the three red→green records; confirmation the untouched tree is green both lanes' worth of coverage it participates in; anything AR-4 stopped you from touching.

## Deviation record

Full verification detail (derived require set, exact red→green
transcripts, per-push confirmation) lives in `notes/facts-register.md`
F17; the ADR-side record is a new dated amendment under `notes/ADRs.md`
ADR-0002 (not a new ADR number — a fix-forward note, per AR-1's own
instruction). This section records only where execution required a
judgment call the prompt didn't fully settle, or went beyond its
letter.

**Palgebra's require set matched self-containment on the first check —
no escalation fired.** `grep -rhoE 'ehrt\.[a-zA-Z0-9._-]+'` over every
`components/palgebra/{src,test}` file, cross-checked against each
file's own `ns` form, found requires under `ehrt.palgebra.*` only
(plus `clojure.*`/`malli.*` externals); the `ehrt.tools`/`ehrt.sim`
hits the same grep surfaced were all inside docstrings or a test
fixture's string literal, not real `:require` clauses. AR-1's
allowlist is exactly `ehrt.palgebra.*` — no wider edge existed to
name.

**The one real interpretive call: what counts as a "structure
table/section" for the absence check, since AGENTS.md has no actual
table.** AR-2 says the presence check covers "both surfaces" but the
absence check is scoped to "structure tables/sections only, not
prose." architecture.md has a literal markdown table (`| \`components/x\`
| kind | description |`); AGENTS.md's "Landed so far" section is
prose throughout, and — load-bearingly — that same prose legitimately
names the retired `components/tools` in backticks three times as
extraction history (docs-tooling split, corpus-io split, stage-3
retirement narrative), which a naive whole-file absence scan would
have flagged as false positives. Resolved by implementing the absence
check as a line-anchored regex matching only a table row's *first
cell* (`^\|\s*`(components|bases)/name`\s*\|`) and applying that same
extractor to both files' full text rather than hand-carving a
prose/table boundary per file: AGENTS.md has zero lines shaped that
way, so it naturally contributes nothing to the absence check without
needing an explicit per-file exclusion, and architecture.md's own
row-internal prose (the kernel and corpus rows each cite
`` `components/tools` `` inside their "What it is" cell, not as the
row's own identity) is correctly ignored because it isn't the line's
leading cell. This is a stronger and more literal reading of "not
prose" than manually bounding a text region between two headings would
have been, and was chosen because it degrades safely: a future doc
that gains its own stray table would start participating in the
absence check automatically, without a matching code change.

**One addition beyond AR-3's letter, disclosed:** `deps_lint_test.clj`
gained a new permanent test,
`lint-catches-an-arbitrary-future-component-outside-the-allowlist-test`,
seeding a fixture that requires a namespace that has never existed
(`ehrt.some-future-component.core`) rather than the historical
`ehrt.tools.lint`. AR-3(a) only asked for a manual seed-then-revert
run against the real tree as evidence for this record; this test makes
the allowlist's actual selling point — that a *future* rename or new
component needs no edit here to stay forbidden — a standing,
regression-proof assertion rather than a one-time transcript. The
original `ehrt.tools.lint` fixture test was kept unchanged as
regression coverage; both pass under the new allowlist code.

**Facts-register entry is Index-only, matching current practice, not
the full Index+Register dual-entry shape F1–F13 used.** F14/F15/F16
(the three prior split-stage sessions) were also added to the Index
table only — the Register section's last entry is still F13. Adding
F17 to the Register section as well was considered and rejected as
scope creep beyond what this session's own change requires (AR-4);
matching the three most recent precedents instead of resurrecting the
older dual-entry convention.

**Integration lane not run, by design, stated rather than silently
skipped:** both hardened gates are dev-time-only tests (palgebra and
docs-tooling bricks, exercised by the per-push lane's `conformance`
and `ehrt-cli` project runs already); `projects/integration` composes
neither brick's test tree and gates nothing this session touched, so
running it would have added ~8 minutes for zero additional coverage.

**Nothing else touched, per AR-4's fence:** `operation-manifest.edn`'s
`:producer :name "ehrt.tools"` literal, the project test trees'
`ehrt.conformance.*`/`ehrt.integration.*` naming, and every other item
on ADR-0018's post-split named-futures list besides items 3 and 5 were
left exactly as ADR-0018 left them.

# 2026-08-10 — Fixture relocation

## Scope

Session prompt executing the roadmap Next row anchored at the fidelity
riders arc open (ADR-0081 AR-FR-2(a)): relocate the entire
`components/corpus/test-fixtures/` tree to a root-level
`test-fixtures/`, symmetric with `demos/` (the ADR-0073 demos-front-
door mechanic reused: same-commit `.gitattributes` moves, byte-
witnessing, pointer-README stub). Widened by author ruling ("Q2 a.")
to all four subtrees, including the roadmap-unnamed `reports/` member.
A move-don't-improve session: every fixture file's bytes are identical
before and after; every edit anywhere is a path-text citation. This is
ADR-0099.

## Red→green evidence highlights

Not a red→green session in the usual sense (no behavior changed) — the
proof is the full suite staying green across the move plus the oracle
bracket staying pure identity. `clojure -M:poly check`: OK.
`clojure -M:poly test :all skip:integration`: 14,315 passes, 0
failures, 0 errors, confirmed failure-free across the entire run's own
output by grep. The six named docs-tooling gates run inside that suite,
individually confirmed green: `test-source-live-path-lint-test` (129
assertions, zero allowlist edits needed), `notice-verbatim-test` (157),
`provenance-leaf-law-test` (8), `stale-path-test` (167),
`license-text-pointer-test` (13), `quickstart-fresh-test` (14).
`bin/verify-nist-lock`: OK, 6/6 coordinates. `bin/regression-oracle
stable-20260809-permission-legs-and-bare-flags 4cb139d`: all 34 roots
IDENTICAL, exactly as predicted (no `src/` logic touched, the two `.clj`
`src` edits both doc-string literals). Byte-witness: 13 of 15 fixture
files sha256-identical before and after; the two prose files
(NOTICE.md, PROVENANCE.md) changed only in their own path-citation
text, as expected and disclosed.

## Judgment calls and their ratification status

- **The driving prompt's own sweep inventory (16 `.clj` files, "four"
  `docs/use-cases/*.md` pages) was found incomplete against the live
  tree.** Not ratified in advance — resolved in-session per the
  prompt's own "verify-then-act" instruction for channel claims,
  widened to cover the actual live citation set (30 files total),
  disclosed in both the prompt archive's own Deviations section and
  ADR-0099's Context/Sweep enumeration/Deviations. The most
  consequential addition, `components/judge/resources/judge/
  pairing-registry.edn`, was a functional-correctness fix (runtime
  fixture paths), not a documentation judgment call — there was no
  real discretion here, only a gap between the prompt's own claimed
  inventory and what the tree actually needed.
- **`.agents/plans/roadmap.md`'s own Fixture relocation row left
  untouched by the move commit, moved to Done only in this close-phase
  commit.** Matches the two-commit shape every prior Next-row-to-Done
  transition in this repo's own history has used (e.g. ADR-0098's own
  close). Not a deviation — the driving prompt's own Step 8 already
  specifies this ordering.

## Findings and HEAD landed

**No new defects found in shipped code.** The one real risk this
session surfaced — `pairing-registry.edn`'s runtime fixture paths going
unswept — was caught before it ever reached a red test, by re-deriving
the sweep from a repo-wide grep rather than trusting the driving
prompt's own inventory. Had it shipped unswept, the very next full
suite run (this session's own Step 7, or CI) would have turned every
pairing-registry-driven judge test red; it never did, because the gap
was closed in the same commit as the move.

**Tag paid forward:** `stable-20260809-permission-legs-and-bare-flags`
tagged at `8d4b1ee` (Step 1, this session), the successor-tag debt
ADR-0098 did not explicitly name (this session's own driving prompt
licensed it directly, tag law case (i)) — peeled ref verified.

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.

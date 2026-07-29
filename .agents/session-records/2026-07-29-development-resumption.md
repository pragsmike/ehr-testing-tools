# 2026-07-29 — Development resumption: kernel/judge extraction, ehrt rename, audience-forked docs

## Scope

Autonomous session (R30, ADR-0007) resuming development after the
migration/discipline-restoration era closed. Five phases, each its own
commit+push checkpoint: (0) R6→R30 ritual restoration, ADR-0007; (1)
`components/kernel`/`components/judge` extracted from `components/tools`
by census, ADR-0008 (ADR-0002 R14's named hole H4, closed); (2) the CLI
renamed `ehr` → `ehrt` ("e-heart") everywhere it appears, `bases/cli`,
`projects/ehrt-cli`, `sim-cli` deprecated with a dated retirement
trigger, ADR-0009; (3) `notes/docs-audit.md` — every doc under the
three pre-existing `docs/` trees dispositioned; (4) the audience fork
executed — root `docs/` (user path) and `docs/dev/` (maintainer path)
built, glossaries and problem statements editorially merged, README
rewritten as the all-audiences front door, ADR-0010; (5) this record.

What actually happened matches what was asked, with one scope
expansion the census/link-audit work itself surfaced and folded in
rather than deferring: several hand-authored docs (`formats.md`,
`judge-calibration.md`, `locators.md`, `notation.md`,
`source-sink-design.md`) still said `ehr` (not `ehrt`) and cited
pre-Polylith `ehr_testing_tools`-rooted namespace paths that predate
even ADR-0002's H2 landing — found only because promoting these docs
to the user/dev path required opening every one of them for link
fixes anyway. Fixed in Phase 4/5, not deferred to a third rename pass.

## Red→green evidence highlights

Every code-touching checkpoint (Phases 1, 2) ran the same three-gate
sequence before its commit: `clojure -M:poly check` (green each time),
`clojure -M:poly test :all skip:integration` (354 clean test-namespace
summaries, 0 failures/0 errors, run twice per phase — once pre-commit,
once via `make ci-parity`'s fresh-clone/cold-cache clone of the actual
commit), `make ci-parity` (green each time). Phase 1's own kernel/judge
namespaces were additionally verified in isolation: 169 tests, 431
assertions, 0 failures, run twice (before and after the `run!` →
`run-invocation!` shadow-warning fix) to confirm the rename changed
nothing but the warning. Phase 4 (docs-only, no source changes besides
`docsgen.clj`/`pipeline.clj`/`usecases.clj` write-path and banner
fixes) ran the identical full-suite + ci-parity gate rather than being
assumed safe because it "was just docs" — and caught nothing broken,
which is itself the evidence a docs-only change is supposed to
produce. `poly check` alone was re-run after Phase 5's own additional
doc fixes (below); no code changed in Phase 5, so the full suite
wasn't re-run a fifth time — recorded as a judgment call below.

## Judgment calls and their ratification status

Every `[C]`-tagged ruling in this session's own prompt (R35, R36, R37,
R38) was applied as given; none vetoed by anything discoverable
mid-session (no author present to veto — post-hoc review is A1/A2 in
the prompt's own "Author actions after"). Calls made *within* those
rulings, not themselves pre-ruled, and their disposition:

- **The `lineage` census surprise** (R36 applied literally: used only
  by `corpus`, not judge or a second consumer → stays in tools, against
  the prompt's own "expected kernel set" naming it). Recorded in
  ADR-0008 as the census doing its job, not silently reconciled to
  match the expectation. **Not ratified** — a real candidate for a
  future author veto if the expectation was itself the intended
  ruling; the census is transparent about disagreeing with it.
- **`sim.clj` staying in tools despite meeting R36's 2-of-4 test**,
  per R37's explicit naming of "the sim adapter" as residual-tools
  content. Ruled, not a call.
- **`components/tools/locators_doc_test.clj` staying in tools instead
  of moving to kernel** (would have inverted kernel→tools dependency
  direction) — the one real "cycle the census missed," resolved per
  the prompt's own named fallback ("resolve... per R36's rule").
  Structural, not really discretionary once found.
- **The two component-level `README.md` NAV pages retiring by
  role-supersession rather than textually merging** (`notes/docs-audit.md`)
  — an editorial call the audit made explicit and disclosed, not
  hidden inside "MERGE."
- **`docs/what-is-this.md` condensing sim's own seven-row validation
  table rather than reproducing it in full**, and dropping tools'
  pre-merge problem statement's entire Part 2 (40 research questions,
  process history, not "what is this") — R34's own "history-free"
  instruction, applied as a real editorial cut, not just a relocation.
  **Not separately ratified**; the merge itself is R38's one named
  editorial task (glossary), this is its problem-statement sibling,
  same discipline extended by inference.
- **Fixing pre-existing stale namespace/path citations found while
  moving files** (`ehr_testing_tools`-rooted paths in `formats.md`/
  `locators.md`/`judge-calibration.md`; two dead links to
  `.agents/memory/patterns.md`, repointed to their real frozen-
  provenance location) — fix-forward-with-disclosure, not a new
  finding requiring a stop; recorded in ADR-0010's own deviation
  record.
- **Not re-running the full test suite a fifth time after Phase 5's
  own doc-only fixes** (namespace citations, `docs/README.md`'s
  overclaim correction) — `poly check` alone re-run (green); no
  `.clj` source changed in this pass, only markdown. Judgment call,
  not a gate skipped: the full suite's own value is proving code
  compiles and passes, which nothing in this pass could have broken.

## Findings and HEAD landed

**The `docs/README.md` overclaim, found and corrected.** Its first
draft (Phase 4) claimed the user path carries "no `components/`
paths" — false on inspection: `use-cases.md` and the *pre-existing*
root Quickstart both use `components/tools/test-fixtures/v2` as a
literal, real fixture argument in a copy-pasteable command, and
`docs/glossary.md` links out to component-adjacent sim docs for
readers who want more depth. Corrected to state the actual, narrower,
true claim (no Polylith *vocabulary*, no architecture-as-narrative) —
recorded here rather than silently tightened, since it's exactly the
kind of doc claiming more than it delivers that this workspace's own
"maturity table, not a formality" ethic exists to catch.

**Cold-reader check.** Script-verified (not manually clicked), per
ADR-0010: every relative link in every file under `docs/` plus the
root `README.md` resolves to a real file on disk. First pass: 34
broken (files moved out from under same-directory links, `.edn`
source cross-references whose relative depth changed, two genuinely
pre-existing dead links). Second pass: 0. Full detail and method:
ADR-0010's own record.

**Census table.** ADR-0008's own record — the full require-graph
census of every `ehrt.tools.*` namespace, its disposition, and where
it disagreed with this session's own "expected kernel set."

**Grep sweeps (Phase 5's own gate).** Old CLI name (`ehr`, bare) in
user-visible surfaces: zero outside provenance (ADR text quoting
history), the `AGENTS.md`/`glossary.md`/`architecture.md`/
`bases/cli/README.md` naming-rationale sentences ("renamed from `ehr`;
`ehr` stays reserved for..."), and the executable-bits test's own
historical incident narrative — every one of those is a deliberate,
correct citation, not staleness, checked individually rather than
pattern-excluded. Repo-history/Polylith-vocabulary mentions in the
user path (`docs/*.md`, not `docs/dev/`): zero after the
`docs/README.md` correction above; the remaining `components/...`
strings are literal fixture paths and supplementary-material
hyperlinks, not narrative, per that same correction.

**Facts-register entries this session created:** F2 (`bases/sim-cli`
deprecation and its retirement trigger, R33/ADR-0009).

**HEAD this session's ceremony lands on:** the commit this record's
own checkpoint produces (`docs: session record and archived prompt --
development-resumption session`), pushed immediately after, per R30.

# R2 — Palgebra claim sweep: renderer, signature loader, lint split, direction lint

You are working in `ehr-testing-tools` (public). This session executes Phase 2 of `.agents/plans/judge-gate-refactor.md`: claim the palgebra language assets that already live in this repo — the diagram renderer, the generic signature-loading machinery, the catalytic-lint mechanism — into a `palgebra/` tree with an enforced dependency direction. This is claiming, not improving: moves, namespace splits, and data extraction only; no rewrites toward the specified language (sorts, `⨟` combinators, lower/erase/emit stay design-only, per `docs/palgebra-design.md` §I and the plan's Phase 4). No semantic changes: behavior is proven unchanged by the suite staying green and by byte-identical regeneration of the generated docs.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md` (ADR-0009 especially), `docs/palgebra-design.md` (D9, D13, §I.7, §II.4), `.agents/plans/judge-gate-refactor.md` (Phase 2), `.agents/skills/string-diagram/SKILL.md` and everything beside it, `Makefile` (the `pipeline` and `use-cases` targets), `src/ehr_testing_tools/pipeline.clj`, `src/ehr_testing_tools/lint.clj`, `src/ehr_testing_tools/usecases.clj`, `docs/notation.md`, `deps.edn`. Note R1's cleanup commit touched `lint.clj`, `corpus/intake.clj`, and `locator.clj` — read their current state, not remembered state. Test-first per ADR-0006 where a change is testable; the golden check below is this session's primary evidence. Ritual: commit → `git push origin`. Save this prompt to `.agents/prompts/2026-07-XX-r2-palgebra-claim.md`; final commit archives it.

Author rulings in effect: Layout — palgebra lives in a self-contained top-level `palgebra/` directory (`palgebra/src/palgebra/` for Clojure, `palgebra/tools/` for the Python renderer, `palgebra/examples/` for equation fixtures, `palgebra/test/`, `palgebra/HISTORY.md`), so eventual extraction is a `git mv`; deps.edn gains `"palgebra/src"` (and the test alias gains `"palgebra/test"`). Renderer stays Python — porting to Clojure is recorded as Phase 4 debt, not done here. SKILL.md content is untouched — only its internal file paths update; the didactic rewrite (R2b) waits for the signature-format spec. Lineage — cyberneutics is acknowledged as origin in HISTORY.md; backporting embellishments upstream is explicitly deferred, no sync machinery built. Name — `palgebra` is adopted as the in-repo namespace root now; the public/extracted name (design O5) stays open. In-repo naming split — the namespace root `palgebra` never collides with `ehr-testing-tools`; the placement test for every file is the plan's: names a sort or stage → stays `ehr-testing-tools`; speaks only in wires/boxes/composition/laws → `palgebra`.

## Step 0 — The palgebra tree, HISTORY, and deps

1. Create `palgebra/` with the layout above. `palgebra/HISTORY.md`: the lineage — cyberneutics origin (cite `docs/notation.md`'s verified upstream link for the string-diagram skill and the upstream `palgebra/` theory docs, e.g. `palgebra/duality-and-composition.md` which SKILL.md already cites); primitive palgebra there → this repo's embellishments (union resources, external stages, catalytic targets — P4–P6; the judge/gate factorization and the D-register — R1, per `docs/palgebra-design.md`). Record the author ruling verbatim: lineage acknowledged; backporting deferred; this repo's line is the living one for this repo's purposes.
2. `deps.edn`: add `"palgebra/src"` to `:paths`; add `"palgebra/test"` to the test alias's paths.

Commit: `R2: palgebra tree created; HISTORY records cyberneutics lineage (D9)`.

## Step 1 — Claim the renderer and examples (the first emitter)

1. `git mv .agents/skills/string-diagram/resource_equations_to_mermaid.py palgebra/tools/resource_equations_to_mermaid.py`.
2. `git mv` the example equation/mermaid sets (`*-equations.txt`, `*.mermaid` — lemon-pie, committee, deliberated-choice, ai-study, decision-monad) → `palgebra/examples/`. SKILL.md keeps teaching from them: update its references to the new paths, nothing else.
3. `Makefile`: both renderer invocations (the `pipeline` target and the `use-cases` loop) point at `palgebra/tools/…`.
4. Golden check (this session's primary evidence): run `make pipeline` and `make use-cases`; `git diff --exit-code docs/pipeline.md docs/use-cases.md` must pass — byte-identical regeneration proves the claim moved code without touching behavior.
5. One comment atop the renderer: `# palgebra's first emitter (diagram → Mermaid); Clojure port + source maps are Phase 4 debt (docs/palgebra-design.md §I.6)`.

Commit: `R2: claim renderer + examples into palgebra (byte-identical regeneration verified)`.

## Step 2 — palgebra.signature: the generic loader, kinds as data

1. Extract from `src/ehr_testing_tools/pipeline.clj` the generic halves — equation-EDN loading, the `Stage` and `UnionResource` schema shapes, validation plumbing — into `palgebra.signature` (`palgebra/src/palgebra/signature.clj`). The schema becomes parameterized by the kind set rather than hardcoding it.
2. The five stage kinds move to data: `docs/signature.edn` holding exactly the current kinds `#{:transform :normalize :enrich :judge :feedback}` and nothing more (their law prose stays where it lives in `docs/notation.md` — law-as-data is design O4, not this session). `ehr-testing-tools.pipeline` loads `docs/signature.edn`, passes the kinds to `palgebra.signature`, and keeps every existing entry point (`write-equations-txt!`, `write-pipeline-md!`) so the Makefile and callers see no change. This is D13 arriving: instantiating the language means authoring data.
3. Existing `pipeline_test.clj` stays green unchanged (or with require-site edits only); add minimal `palgebra/test` coverage for the loader against the toy signature (Step 4).

Commit: `R2: palgebra.signature extracted; stage kinds are signature data (D13)`.

## Step 3 — Lint split: mechanism vs. taxonomy

`src/ehr_testing_tools/lint.clj` splits: the mechanism — "every catalytic resource in the loaded equations resolves to a declared target kind; declared refs are checked mechanically" — becomes `palgebra.lint`, parameterized by a target taxonomy and a resource→target mapping supplied by the caller. The four concrete targets, `catalytic-resource-targets`, and everything citing `artifacts.lock.edn`/`deps.edn`/repo registries stay in `ehr-testing-tools.lint`, which delegates. `make lint-pipeline` behavior and output unchanged; the seeded-violation test still passes (moved or split as placement dictates).

Commit: `R2: catalytic-lint mechanism claimed into palgebra.lint; EHR taxonomy stays downstream`.

## Step 4 — Dependency-direction lint + toy signature

1. `palgebra.deps-lint` (or a small Clojure tool under `palgebra/tools/`): parse every ns form under `palgebra/`; fail if any requires `ehr-testing-tools.*` — tests included. Make target `lint-deps`; add it beside `lint-pipeline` in whatever grouping exists. Seeded-violation test (a temp file, or a fixture the lint is pointed at) proving it fires. CI wiring joins the enforcement wave per ADR-0006 — add one line to `corpus-foundations.md`'s enforcement-wave row if R1 didn't already.
2. Toy signature in `palgebra/test`: two sorts, three stages, its own tiny equations EDN — loaded via `palgebra.signature`, linted via `palgebra.lint` with a toy taxonomy. This is the proof that instantiation is data-authoring and the fixture future palgebra work tests against. No EHR vocabulary anywhere in it.

Commit: `R2: dependency-direction lint (palgebra never requires ehr) + toy signature fixture`.

## Step 5 — Close out

Full suite + `make lint-pipeline` + `make lint-deps` green; golden check re-verified (`make pipeline && make use-cases && git diff --exit-code docs/pipeline.md docs/use-cases.md`); integration suite not required (no judge-layer changes) — run it only if artifacts are cached and time permits, as a courtesy sanity pass. Final grep: no `ehr-testing-tools` strings under `palgebra/` (HISTORY.md's prose excepted); no live references to the old `.agents/skills/string-diagram/` renderer path. Update `.agents/plans/judge-gate-refactor.md` Phase 2 → Done and the `corpus-foundations.md` R row's summary. Archive this prompt.

Commit: `R2 complete: palgebra claimed — renderer, signature loader, lint mechanism, direction lint (archives prompt)`.

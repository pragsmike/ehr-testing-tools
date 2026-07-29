# Documentation disposition audit — every doc has a destination

Session: 2026-07-29 development resumption, Phase 3. Every file under
root `docs/`, `components/sim/docs/`, and `components/tools/docs/` gets
a disposition row here — the execution table Phase 4 works from
mechanically (`git mv` for moves, editorial judgment for merges).
Dispositions:

- **USER-PATH-MOVE (target)** — moves to root `docs/`, the complete,
  Polylith-free, history-free user path (R34).
- **DEV-PATH-MOVE (target)** — moves to `docs/dev/`, for maintainers.
- **COMPONENT-ADJACENT-STAY** — stays exactly where it is; component-
  internal material a user or a general workspace maintainer never
  needs, but someone working on that specific component's own code
  does. Not a residual "everything else" bucket — each row below
  earns this disposition on its own terms (cited in the reason
  column), the same discipline `notes/carve-loss-audit.md` already
  set for the four accepted root residents.
- **MERGE (into)** — content combines with a sibling doc; the
  merged doc's own destination is named, and both originals get
  PROVENANCE-RETIRE once the merge lands.
- **PROVENANCE-RETIRE (reason)** — moves to `notes/`, frozen, cited
  as history from here forward, never edited for new paths again.

**Execution note for Phase 4 (not resolved by this audit):** four
tools-side docs are docsgen *output* (`cli.md`, `operators.md`,
`pipeline.md`, `use-cases.md`) with hardcoded write-paths in
`components/tools/src/ehrt/tools/docsgen.clj`/`.usecases`/`.pipeline`
and the root `Makefile`'s `docsgen` targets. Moving their destination
means updating those write-paths in the same commit as the move —
never hand-editing the moved file, per this repo's own generated-vs-
authored discipline. Their *source* `.edn` files (`pipeline.edn`,
`use-cases.edn`, `signature.edn`) are COMPONENT-ADJACENT-STAY:
`docsgen.clj`/`.usecases`/`lint.clj` read them via cwd-relative
literal paths rooted at `components/tools/docs/`, and no user or
general maintainer ever hand-edits them directly.

## Pre-seeded rows (named in this session's own prompt)

| Files | Disposition |
|---|---|
| `components/sim/docs/GLOSSARY.md` + `components/tools/docs/GLOSSARY.md` | MERGE → root `docs/glossary.md` (R38, full editorial merge, alphabetized, source noted only where meanings differ) |
| `components/sim/docs/problem-statement.md` + `components/tools/docs/ehr-testing-tools-problem-statement.md` | MERGE → root `docs/what-is-this.md`, one history-free "what is this" for the unified repo; both originals PROVENANCE-RETIRE |
| `components/sim/docs/way-of-working.md` | PROVENANCE-RETIRE — stale pre-merge copy; root `docs/way-of-working.md` (→ `docs/dev/way-of-working.md`, this audit) is canonical |
| `components/sim/docs/research/*` (2 files) + `components/tools/docs/research/*` (3 files) | COMPONENT-ADJACENT-STAY — dev material |
| `docs/migration/polylith-brief.md` | DEV-PATH-MOVE → `docs/dev/migration/polylith-brief.md` |
| `components/tools/docs/positioning.md` | DEV-PATH-MOVE → `docs/dev/positioning.md`, revised for the unified repo; audiences amended to name domain experts and informaticists explicitly under practitioners |

## Root `docs/`

| File | Disposition |
|---|---|
| `docs/migration/polylith-brief.md` | DEV-PATH-MOVE → `docs/dev/migration/polylith-brief.md` |
| `docs/way-of-working.md` | DEV-PATH-MOVE → `docs/dev/way-of-working.md` (canonical; sim's own copy above retires against this one) |

## `components/sim/docs/`

| File | Disposition | Reason |
|---|---|---|
| `GLOSSARY.md` | MERGE → root `docs/glossary.md` | pre-seeded, R38 |
| `README.md` | PROVENANCE-RETIRE | this directory's own audience-routing NAV page, superseded by the new root `docs/README.md` (user path) and `docs/dev/` index (Phase 4) — its role moves, its text doesn't merge into anything |
| `clinical-realities.md` | COMPONENT-ADJACENT-STAY | catalog of how the engine models specific clinical edge cases — needed by someone extending sim's own modules, not a user running the tool or a general maintainer |
| `demos/README.md` | COMPONENT-ADJACENT-STAY | index into the demo fixtures below, same audience as them |
| `demos/boarding-transfer/{README.md,ground-truth.edn,messages.txt}` | COMPONENT-ADJACENT-STAY | worked-example fixtures pinned to sim's own engine tests |
| `demos/emit-state/{README.md,fhir-bundle-patient1.json,ground-truth.edn,messages.txt}` | COMPONENT-ADJACENT-STAY | same as above |
| `demos/module-mix/{README.md,config.edn,ground-truth.edn,messages.txt}` | COMPONENT-ADJACENT-STAY | same as above |
| `demos/order-result/{README.md,config.edn,ground-truth.edn,messages.txt}` | COMPONENT-ADJACENT-STAY | same as above |
| `demos/persona-enriched/{README.md,ground-truth.edn,messages.txt}` | COMPONENT-ADJACENT-STAY | same as above |
| `demos/site-profiles/{README.md,config-aldric.edn,ground-truth.edn,messages-aldric.txt,messages-default.txt}` | COMPONENT-ADJACENT-STAY | same as above — distinct from user-facing `site-profiles.md` below, which teaches the *concept*; these are its pinned fixture proof |
| `event-sourcing.md` | COMPONENT-ADJACENT-STAY | engine internals (the event-sourced state model) |
| `gmf-interpreter.md` | COMPONENT-ADJACENT-STAY | engine internals |
| `gmf-source-model.md` | COMPONENT-ADJACENT-STAY | engine internals |
| `operational-models.md` | COMPONENT-ADJACENT-STAY | engine internals |
| `patient-state-model.md` | COMPONENT-ADJACENT-STAY | engine internals |
| `positioning-notes.md` | PROVENANCE-RETIRE | its own first line: "This is not a positioning document" — raw pre-positioning scratch, mining-session notes already consumed; superseded once the merged `docs/dev/positioning.md` lands |
| `problem-statement.md` | MERGE → root `docs/what-is-this.md` | pre-seeded |
| `research/SimHospital-Synthea-experience.md` | COMPONENT-ADJACENT-STAY | pre-seeded |
| `research/SimHospital-Synthea-limitations-considered.md` | COMPONENT-ADJACENT-STAY | pre-seeded |
| `sim-theory-diagram.md` | COMPONENT-ADJACENT-STAY | palgebra-derived theory doc, engine-internal |
| `sim-theory-diagram.mermaid` | COMPONENT-ADJACENT-STAY | generated diagram source for the above |
| `sim-theory-equations.txt` | COMPONENT-ADJACENT-STAY | generated equations source |
| `sim-theory.edn` | COMPONENT-ADJACENT-STAY | hand-authored source the above are generated from |
| `sim-theory.md` | COMPONENT-ADJACENT-STAY | engine-internal theory doc |
| `simulate-your-facility.md` | USER-PATH-MOVE → `docs/simulate-your-facility.md` | named in R34's own user-path list; practitioner task doc |
| `site-profiles.md` | USER-PATH-MOVE → `docs/site-profiles.md` | named in R34's own user-path list; practitioner reference |
| `third-party-sources.md` | COMPONENT-ADJACENT-STAY | licensing/attribution for sim's own data sources |
| `trajectory-computation.md` | COMPONENT-ADJACENT-STAY | engine internals |
| `way-of-working.md` | PROVENANCE-RETIRE | pre-seeded — stale copy, root's is canonical |

## `components/tools/docs/`

| File | Disposition | Reason |
|---|---|---|
| `GLOSSARY.md` | MERGE → root `docs/glossary.md` | pre-seeded, R38 |
| `README.md` | PROVENANCE-RETIRE | this directory's own audience-routing NAV page, same disposition and reason as sim's `README.md` above |
| `cli.md` | USER-PATH-MOVE → `docs/cli.md` | core user reference; generated (see Execution note) |
| `components.md` | DEV-PATH-MOVE → `docs/dev/components.md` | pipeline architecture ("the pipeline's ingredient list") |
| `ehr-testing-tools-problem-statement.md` | MERGE → root `docs/what-is-this.md` | pre-seeded |
| `engine-onboarding.md` | DEV-PATH-MOVE → `docs/dev/engine-onboarding.md` | contributor checklist for wrapping a new engine |
| `experiments.md` | COMPONENT-ADJACENT-STAY | index into the evidence-trail experiments below |
| `experiments/EXP-A4.md`, `EXP-A4-results.md` | COMPONENT-ADJACENT-STAY | tools' own empirical evidence trail, cited by facts-register-style claims, not by any user or general-maintainer path |
| `experiments/EXP-B2.md`, `EXP-B2-results.md` | COMPONENT-ADJACENT-STAY | same |
| `experiments/EXP-C5.md`, `EXP-C5-results.md` | COMPONENT-ADJACENT-STAY | same |
| `experiments/EXP-SBOM.md`, `EXP-SBOM-results.md` | COMPONENT-ADJACENT-STAY | same |
| `experiments/results-rubric.md` | COMPONENT-ADJACENT-STAY | the template the above follow |
| `experiments/results-template.md` | COMPONENT-ADJACENT-STAY | same |
| `formats.md` | USER-PATH-MOVE → `docs/formats.md` | named in R34's own user-path list |
| `judge-calibration.md` | USER-PATH-MOVE → `docs/judge-calibration.md` | practitioner-facing per `positioning.md`'s own audience 5 citation ("the companion for reading verdicts in bulk") |
| `locators.md` | USER-PATH-MOVE → `docs/locators.md` | practitioner reference for `--locator-path` syntax |
| `notation.md` | DEV-PATH-MOVE → `docs/dev/notation.md` | the palgebra-derived equation language `pipeline.md` is written in — architecture vocabulary, not a task doc |
| `operators.md` | USER-PATH-MOVE → `docs/operators.md` | named in R34's own user-path list; generated (see Execution note) |
| `palgebra-design.md` | COMPONENT-ADJACENT-STAY | design doc for `components/palgebra` specifically |
| `pipeline.edn` | COMPONENT-ADJACENT-STAY | hand-authored docsgen source (see Execution note) |
| `pipeline.md` | DEV-PATH-MOVE → `docs/dev/pipeline.md` | architecture diagram + resource equations, maintainer-facing; generated (see Execution note) |
| `positioning.md` | DEV-PATH-MOVE → `docs/dev/positioning.md`, revised | pre-seeded |
| `research/EHR-testing-tools-selection-research.md` | COMPONENT-ADJACENT-STAY | pre-seeded pattern |
| `research/HL7v2-sanitized-corpus-research.md` | COMPONENT-ADJACENT-STAY | pre-seeded pattern |
| `research/License Status of NIST HL7 v2 Validation Software  Evidence-Based Classification.md` | COMPONENT-ADJACENT-STAY | pre-seeded pattern |
| `signature.edn` | COMPONENT-ADJACENT-STAY | hand-authored source for palgebra signature generation |
| `source-sink-design.md` | DEV-PATH-MOVE → `docs/dev/source-sink-design.md` | deep design rationale, cited throughout source as `Part I`-`Part IX`, contributor-facing |
| `use-cases.edn` | COMPONENT-ADJACENT-STAY | hand-authored docsgen source (see Execution note) |
| `use-cases.md` | USER-PATH-MOVE → `docs/use-cases.md` | named in R34's own user-path list; generated (see Execution note) |

## Totals

76 files dispositioned, 0 UNDECIDED: 6 USER-PATH-MOVE, 9 DEV-PATH-MOVE,
55 COMPONENT-ADJACENT-STAY, 2 MERGE pairs (4 files, 2 of them also
counted as PROVENANCE-RETIRE once merged), 4 PROVENANCE-RETIRE (2
stale/scratch, 2 the merge-supersedes count already folds in) — the
two `README.md` NAV pages are PROVENANCE-RETIRE by role-supersession,
not by merge, so they're additional to the GLOSSARY/problem-statement
merge pairs: 6 PROVENANCE-RETIRE rows total (2 way-of-working/scratch +
2 NAV READMEs + 2 merge-superseded originals).

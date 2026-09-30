# Related-work essays: lineage anchor + Synthetic Hospital (docs-only)

2026-09-30. Base `dc9153bb`. Ceremony: R30 (commit and push), taken
from the session prompt, which stated no prepare-only mode. Docs-only:
no source, config or test file changed.

`bin/preflight` ran first, exit 1. Findings disclosed: (1) its `gh run
list` FAILED (`error connecting to api.github.com`), so CI colour read
UNKNOWN from preflight; a direct `gh run list` seconds earlier succeeded
and showed tip run 36722769964 (`dc9153bb`, the other session's close)
`in_progress` and the three runs before it green. (2) Tree not clean:
exactly the two author drafts, `docs/related-work/README.md` and
`docs/related-work/synthetic-hospital.md`, as the prompt expected.
Everything else OK: repo root not under `/mnt/`, `core.fileMode` true,
`core.ignorecase` unset, HEAD matched `origin/main`, HEAD not tagged
`stable-*` (disclosed, no tag owed). Step 0's "tip CI green" gate was
therefore still pending when work began; its final state is recorded
under Close.

Mid-session, `git status` showed the two drafts as staged (`AM`)
though this session never ran `git add` on them. The index held the
drafts exactly as found; `git diff` against the working tree showed only
this session's step-1 corrections. Taken as the author's own staging
and committed with the corrections on top.

## For downstream

> Two new pages under `docs/related-work/` (lineage and non-intent; an
> essay on Synthetic Hospital, arXiv 2609.30027), a new
> `## Fidelity evidence` section on `docs/future-features.md`, and one
> pointer line each in `what-is-this.md`, `docs/README.md` and
> `third-party-sources.md`. No capability changed; nothing is promised.

## Step 1 -- claim checks against the tree

Each draft claim about this tree, its verdict, and where the tree says so.

| Claim (draft) | Verdict | Tree citation |
|---|---|---|
| A curated subset of Synthea GMF modules vendored verbatim, each hashed and pinned to its upstream commit | Holds | `components/sim/resources/sim/modules/` (module JSONs); `modules/NOTICE` table: upstream URL, commit `7e08387c...`, SHA-256 per file |
| Run through this workspace's own port of the module interpreter | Holds | `components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj`, `gmf.clj`; tests `gmf_interpreter_test.clj`; spec `components/patient-simulator/docs/gmf-interpreter.md` |
| SNOMED/LOINC/RxNorm codes travel through unchanged | Holds | `docs/what-is-this.md` Validation table, terminology row ("never invented"); `third-party-sources.md` Synthea bullet 3 |
| Synthea also wrapped directly as a corpus source, FHIR output | Holds | `docs/cli.md:87` (`corpus generate synthea`); `components/corpus/src/ehrt/corpus/generate.clj` (pinned jar, subprocess); `synthea-default.properties:17` `exporter.fhir.export = true` |
| Simulated Hospital: design only, no code, none of its data | Holds | `third-party-sources.md:11` ("design only: no code or data taken") and its "Explicitly NOT taken" paragraph |
| SH config "London demographics" | Tightened to "London ethnicities" | `third-party-sources.md:17` names ethnicities, not demographics |
| SH step vocabulary is the documented ancestor; engine is a pure fold over an immutable log | Holds | `third-party-sources.md:13-14` |
| Encounter horizon, not lifelong history | Holds | `docs/what-is-this.md` Constraint 7 |
| Site-tunable rates exist | Holds | `components/sim-engine/src/ehrt/sim_engine/config.clj:57` (`:churn-profile` a config key) |
| Replay over time exists, as corpus delivery | Holds | `docs/cli.md:30`, `:309-320` (`ehrt play`, `--rate`, `--sink file:`/`mllp://`) |
| No model in the generation path; same seed + config, byte-identical | Holds | `docs/what-is-this.md` Constraint 4; no LLM/provider dependency in any `deps.edn` (grep for anthropic/openai/llm/langchain: no hits) |
| Event log is a published, versioned contract | Holds | `docs/cli.md:249` (`--format ground-truth`: "a PUBLIC, VERSIONED contract") |
| HL7v2 is one rendering among several | Holds | `components/sim-emit-fhir` alongside `components/sim-emit-hl7` |
| Site profiles landed | Holds | `docs/site-profiles.md:3` ("**Landed.**") |
| Renderer invariance: "the analogous test would state it as a property ... site profiles included" -- i.e. not yet a check | **Corrected** | `docs/site-profiles.md` "The invariance claim -- proven": byte-identical ground truth across profiles (structural), messages equal after masking over 100 random seeds. The essay now says the narrow form is proven for site profiles and the broader form (every emitter configuration) is the idea |
| Site profiles already describe header conventions, optional-field population, Z-segments, identifier formats, timestamp precision | **Corrected** | `docs/site-profiles.md` "What this layer designs -- landed" (MSH dialect, code tables, `:surge-format`, Z-segments) and "Honest split" (ward/provider id formats future; nothing on optional-field population or timestamp precision). The essay now names the three it covers and the three it does not |
| The summarize command is the census (implied unbuilt) | **Corrected, premise held only in part** | `ehrt sim describe` landed 2026-09-29 (`e052eb81`, ADR-0183; `docs/cli.md:259-261`): per-kind counts, subject participation, `:t` range, four relationship predicates, over a log this workspace produced. The essay now says `sim describe` gives part of the census and the summarize entry names the rest. The summarize entry is not built: no message-type census, no scenario-inventory columns |

No new figures were added; the figures in the Synthetic Hospital essay
are the paper's own and are labelled so.

The summarize-premise gap was fix-forward with disclosure, not
STOP-AND-REPORT: step 1 explicitly asks for corrections in place, and
the corrected wording has one defensible reading.

## Step 2 -- `## Fidelity evidence`

Added between Scale ergonomics' last entry (Streaming output) and "What
is not on this menu", opening with the same "**Not every gap here is a
fault class.**" sentence. One entry, "Measuring how real the traffic
looks": consumer-voiced, one-line design stance, a *Today:* line, link
to `related-work/synthetic-hospital.md` (resolves: the file lands in
this commit). No internal sizing, no dates.

## Step 3 -- tie-ins

- (a) `docs/what-is-this.md`: one sentence after the Scope list, before
  "Relationship to `ehr-testing-guide`". The four fence bullets are
  byte-unchanged.
- (b) `components/sim/docs/third-party-sources.md`: appended to the
  opening paragraph, "`docs/related-work/README.md` is the argument this
  record supports." Link `../../../docs/related-work/README.md` resolves
  from `components/sim/docs/`.
- (c) `docs/README.md` does enumerate doc pages (its task-first list and
  per-audience routes), so one line was added to the "Deciding whether
  to adopt" paragraph, beside the Scope pointer.

**Stale text noticed, not edited** (one line each, per step 3):

- `third-party-sources.md:21`: `RunModules` "not yet built (`;;
  planned`)" -- the GMF interpreter is built
  (`patient_simulator/gmf_interpreter.clj`).
- `third-party-sources.md:23`: "The 85 GMF module JSONs, consumed as
  data (`RunModules`'s planned input)" -- a curated subset is vendored,
  not 85, and it is consumed today.
- `third-party-sources.md:28`: "the vendored Synthea module
  (`resources/modules/sinusitis.json`)" -- singular and old path; many
  modules now under `components/sim/resources/sim/modules/`.
- `third-party-sources.md:44`: HAPI/NIST "Not wired into CI yet" --
  `judge-v2-hapi` and `judge-v2-nist` components exist.
- `third-party-sources.md:49`: "nothing in this repo executes their
  code" -- `corpus generate synthea` runs the pinned Synthea jar as a
  subprocess (`corpus/generate.clj`). Still no deps.edn linkage.
- `docs/future-features.md:108` (transport entry): "nothing transmits
  over a socket" -- `ehrt play --sink mllp://HOST:PORT` sends over MLLP
  (`corpus_io/mllp.clj`, `java.net.Socket`).
- `docs/future-features.md` summarize entry, *Today:* line: does not
  mention `ehrt sim describe`, which now covers the per-kind census on
  the log side.

## Suite

Tip CI before work: run 36722769964 at `dc9153bb` concluded `success`
(watched to completion; it was `in_progress` at preflight).

First `make test` (log with `MAKE_EXIT` captured): **RED, MAKE_EXIT=2**,
one failure -- `state_derived_test.clj:30`,
`state-derived-md-matches-a-fresh-render-test`. The fresh render
differed from `.agents/state-derived.md` in exactly two cells: session
records 269 -> 270 and archived prompts 261 -> 262, this session's own
two new files. Fix: those two cells, which is byte-for-byte the render
the test printed. Hand-editing the INDEX.md count lines had not been
enough, because state-derived.md counts the same directories. The run
stopped at that brick, so the full suite was run again from the start.

Second `make test` (full, from the start; `poly check`, `poly test :all
skip:integration`, `bin/verify-nist-lock`): **GREEN, MAKE_EXIT=0**, zero
`FAIL in`/`ERROR in` lines. The docs-tooling gates the prompt names
(`link_footnote_gate_test`, `stale_path_test`, `invocation_lint_test`)
ran inside it. `make integration` was not run (docs-only; W-1 stands).

## Close

Committed and pushed as one commit (author: "go ahead and commit and
push when green"). Push verification and the CI run at the pushed tip
are reported in the session's final message; the record is committed
with the change it describes, so it cannot carry its own tip's CI id.

## Step 4 -- design-record references

No ADR or design-record reference was added to any `docs/` prose, so no
footnote was owed. The drafts carry none.

<!-- Attic file: notes/adr/0018-tools-split-stage-3.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0018 — `tools` split stage 3: the domain renamed `corpus`, the façade retired, the interface designed from live consumers

**Status:** Accepted (author-ruled 2026-07-31, session executed same day, autonomous per the stage-3 prompt; zero escalations fired — both anticipated escalation surfaces resolved by the prompt's own pre-rulings, see Deviation record).

### Context

`notes/2026-07-30-refactoring-review.md` §5.1a's staged split of
`components/tools`, final stage. Stages 1 (ADR-0016, `docs-tooling`)
and 2 (ADR-0017, `corpus-io`) left exactly the corpus domain wearing
the `tools` name: the `corpus/*` cluster (generate, generators,
generator-source, intake, mutate, operators, manifest,
golden-comparison), the check subsystem, diff, display, lineage,
operators-doc, player, and the sim adapter. The author ruling for this
stage (2026-07-31): `tools` retires after repoint — greenfield, no
compatibility constraint — and, unlike stages 1–2, this stage includes
ONE sanctioned improvement: the interface itself, designed from
evidence rather than inherited. Everything else stayed
move-don't-improve.

Session start: WSL ext4 clone at `origin/main` = `65e17c4` ("refactor:
extract corpus-io from tools (split stage 2, ruled 07-31)"), clean
tree, no fast-forward needed.

### Decision

**Landing shape (AR-1).** Component `corpus`, namespaces
`ehrt.corpus.*`. The nested `ehrt.tools.corpus.*` flattened
(`ehrt.corpus.mutate`, not `ehrt.corpus.corpus.mutate`); the
non-corpus-prefixed domain files became `ehrt.corpus.<name>` unchanged
in content; the sim adapter renamed `ehrt.corpus.sim-adapter` (its old
name `tools.sim` collided confusingly with the `sim` component once
the `tools` prefix vanished — the rename was ruled in scope, its
content was not; its test follows as `sim-adapter-test`).
`components/corpus/` keeps the component's `docs/` (pipeline.edn,
use-cases.edn, signature.edn, experiments/, research/,
palgebra-design.md) and `test-fixtures/` trees wholesale — every
load-bearing `components/tools/...` path string across the workspace
(`.gitattributes`' three `-text` fixture rules; the Makefile's
docsgen sources; docs-tooling's lint/pipeline/usecases defaults and
tests; judge/judge-v2-hapi/judge-v2-nist/corpus-io/bases-cli test
fixture paths; `bin/quickstart-demo` + README.md's quickstart fence,
kept in lockstep for `quickstart-fresh`; help text and the
`gate v2-nist` `--profile` hint; `artifacts.lock.edn`'s EXP-D3
comment; `judge-fhir-official`'s `verdict-mapping-cited-to` def)
repointed to `components/corpus/...` in the same change.

**The interface (AR-2, the sanctioned improvement).** All 64
`ehrt.tools.interface` defs were classified by live external consumer
before any design work — the table below is the design rationale.
Outcome: **38 defs kept (9 of them renamed), 25 relays dissolved, 1
deleted**. `ehrt.corpus.interface` requires only this component's own
namespaces; the corpus domain no longer requires any judge engine.

*Dissolved — kernel relays (12). Consumers repointed to
`ehrt.kernel.interface`, which already exports every one of these
names verbatim (ADR-0008):*

| Def | Live consumers (via the façade) | Disposition |
|---|---|---|
| `ok`, `ok?`, `rejected`, `rejected?`, `error`, `error?` | bases/cli src+tests, every project test tree | dissolve → kernel |
| `valid?` | bases/cli core_test | dissolve → kernel |
| `fetch`, `read-lockfile`, `resolve-artifact` | bases/cli src+tests, integration tests | dissolve → kernel |
| `sha256-file` | conformance mutate-stdout-stdin-loopback test | dissolve → kernel |
| `make` (locator) | bases/cli (`locator/make`) | dissolve → kernel |

*Dissolved — judge vocabulary relays (5). Consumers repointed to
`ehrt.judge.interface` (same names, ADR-0011):*

| Def | Live consumers | Disposition |
|---|---|---|
| `Report`, `build-report`, `baseline-relative-report` | bases/cli, conformance gate suites, integration | dissolve → judge |
| `diff-reports` | conformance sim-gate-loop/full-capability suites | dissolve → judge |
| `report-valid?` | bases/cli tests, conformance gate suites | dissolve → judge |

*Dissolved — judge-engine relays (8). The façade's whole
re-qualification layer (`v2-*`/`fhir-*`/`v2-nist-*`, ADR-0011/0012)
dies with it; consumers call each engine interface's own unqualified
names:*

| Def | Live consumers | Disposition |
|---|---|---|
| `v2-gate-file`, `v2-gate-dir` | bases/cli, conformance gate suites | dissolve → `ehrt.judge-v2-hapi.interface/gate-file`,`/gate-dir` |
| `fhir-gate-file`, `fhir-gate-dir`, `fhir-gate-batch` | bases/cli, integration suites | dissolve → `ehrt.judge-fhir-official.interface/gate-file`,`/gate-dir`,`/gate-batch` |
| `v2-nist-make-validator`, `v2-nist-gate-file`, `v2-nist-gate-dir` | bases/cli src+tests | dissolve → `ehrt.judge-v2-nist.interface/make-validator`,`/gate-file`,`/gate-dir` |

*Deleted (1), with grep evidence:*

| Def | Evidence | Disposition |
|---|---|---|
| `Assertion` | zero live code consumers anywhere outside `components/tools` — the only non-defining references were prose: `check-command`'s docstring citation in bases/cli (rewritten to point at `check-corpus`'s own documented contract) and test-file narration | deleted; `ehrt.corpus.check/Assertion` itself stays, component-internal |

*Kept (38) — the corpus domain surface. Renames per AR-2's naming
ruling: the two registries take symmetric noun prefixes (the old bare
`lookup`/`entries`/`register!` meant "operators" only by
collision-victory, ADR-0002; `generators-*` was the qualified loser),
and generator-source's bare `resolve!` (collision residue vs the
spool twin that left at ADR-0017) becomes `resolve-generator-source!`.
No signature changed anywhere:*

| Def (new name) | Old name | Live consumers |
|---|---|---|
| `generate!` | — | bases/cli, integration ×5 suites |
| `jdk-name`, `jdk-version`, `resolve-java-bin` | — | bases/cli (doctor, fhir gate) |
| `out-dir-exists?`, `out-dir-exists-error` | — | bases/cli src (+ core_test) |
| `generator-lookup` | `generators-lookup` | bases/cli src+tests |
| `generator-register!` | `generators-register!` | bases/cli tests only (hermetic registry swap) |
| `generator-resolve-params` | `generators-resolve-params` | bases/cli src+tests |
| `resolve-generator-source!` | `resolve!` | bases/cli, conformance sim-generator-source suite |
| `parse-source-designator` | — | bases/cli (intake's generator/stdin URL branch) |
| `intake!`, `intake-via-source!` | — | bases/cli, conformance, integration |
| `sniff-format` | — | bases/cli (D11 sniff dispatch, show, play) |
| `valid-catalog-entry?`, `valid-intake-record?` | — | conformance sim-intake suite only |
| `mutate` | — | bases/cli, integration ×3 |
| `operator-entries` | `entries` | bases/cli |
| `operator-lookup` | `lookup` | bases/cli, docs-tooling lint (target-4 registry check), integration ×3 |
| `operator-register!` | `register!` | bases/cli tests only |
| `operator-registry-snapshot` | `registry-snapshot` | bases/cli tests only |
| `operator-registry-reset!` | `reset-registry!` | bases/cli tests only |
| `ManifestV1_1` | — | conformance manifest-contract + smoke suites only |
| `check-corpus` | — | bases/cli |
| `check-schemas-lookup` | — | docs-tooling lint (target-4) |
| `compare-catalogs` | — | integration intake-source-golden suite only |
| `render-er7-message`, `render-er7-stream`, `render-fhir-json`, `split-er7-multi` | — | bases/cli (show/play, ADR-0013/0014) |
| `default-rate`, `default-idle-cap-ms`, `plan`, `message-timestamp-ms`, `message-type-trigger`, `message-patient-id`, `frame-event` | — | bases/cli (play, ADR-0014) |
| `sim-run!` | — | bases/cli, conformance sim-harness |

Defs consumed only by project/base test suites are kept per the
prompt's own pre-ruling ("keep, but say so") and are marked
`test-consumer only` in the interface source itself — they are
contract surface for the conformance/integration lanes, not CLI
wiring.

**The final graph (AR-3), as `poly deps` renders it:** `corpus → {kernel,
judge, corpus-io, sim}` in src context (the judge edge is `check`'s
verdict-vocabulary use, pre-existing) plus one test-context-only edge
`corpus →(t) judge-v2-hapi` (`v2_contract_pairing_test`'s own direct
engine require, pre-existing since ADR-0011); `docs-tooling → {kernel,
palgebra, corpus, corpus-io}`; `cli → {kernel, judge, judge-v2-hapi,
judge-fhir-official, judge-v2-nist, corpus, corpus-io, docs-tooling}`;
`corpus-io → kernel` (its test-context `t` toward corpus is the
simhospital fixture-helper require its tests have carried since
ADR-0017 — the forbidden src direction stays clean). No live
`corpus → judge-*` engine edge existed to escalate: characterization
found the engine requires lived only in the façade's relay layer.

**Retirement is total (AR-4).** `components/tools/` is gone (git-mv'd,
so history follows the rename); `poly/tools` left every `deps.edn`
(root `:dev`/`:ehrt`, all three project files, the coverage alias's
path lists); no tombstone or alias namespaces. `projects/integration`
additionally DROPPED `poly/judge-v2-nist` (plus the now-unneeded
`nist-hit` `:mvn/repos` entry): it was only ever on that classpath
because the façade's interface required every engine — the same
drop-don't-override rule ADR-0016 applied to palgebra there.
`poly/judge-v2-hapi` was ALSO dropped at first, on the same grep
evidence about integration's own test tree — and reverted when the
integration lane itself went red: the corpus BRICK's own
`v2_contract_pairing_test` requires that engine's interface, and poly
runs a declared brick's tests in every composing project, so the
brick's test-context edge is a classpath requirement grep-for-
project-tests cannot see (Deviation record).

**`:necessary` re-derived, fourth time** (same method: every entry
cleared, one `poly check`, read warning 207):

| Project | Before | After | Why |
|---|---|---|---|
| `ehrt-cli` | *(no key)* | *(no key)* | bases/cli requires every interface it consumes directly; palgebra reachable via docs-tooling. |
| `conformance` | `["docs-tooling"]` | `["docs-tooling" "judge-fhir-official" "judge-v2-nist"]` | docs-tooling: unchanged (hosts its own moved tests). The two engines: consumed only by this project's own parity/gate-loop test trees now that the façade's every-engine src relay is gone — poly cannot see project test-tree requires. NOT judge-v2-hapi, empirically: the corpus brick's own `v2_contract_pairing_test` requires that engine's interface, a brick test-context edge poly's reachability DOES count. |
| `integration` | *(no key)* | `["judge-fhir-official"]` | Stage 2's zero-warning state existed only because `tools`' src reached every engine; with that gone, the one engine its own test tree genuinely consumes needs the override (poly-invisible test-tree require, same class as ADR-0016's `tools` row there). |

Confirmed: `clojure -M:poly check` — `OK`, zero warnings, with the
table above as the final `workspace.edn` state.

**Project test trees renamed** — `projects/conformance/test/ehrt/tools/`
→ `.../ehrt/conformance/` (13 files, ns `ehrt.conformance.*`;
`sim-harness` helper included) and `projects/integration/test/ehrt/tools/`
→ `.../ehrt/integration/` (5 files, ns `ehrt.integration.*`). The
prompt did not rule on these names; leaving 18 live namespaces under a
retired component's prefix contradicted AR-4's total-retirement intent
(and would have made the new `ehrt.tools.`-forbidding tripwire's story
incoherent), and naming each tree after its own project states exactly
what these are: project-composition suites, not any brick's tests.
Recorded as this stage's one unruled judgment call — see Deviation
record.

**Debt sweep rider (AR-7), executed.** `docs/formats.md` and
`docs/locators.md` fixed to post-stage-3 reality in one sweep covering
all three stages' renames — including the stage-2 debt ADR-0017
flagged: `locators.md`'s broken relative link now points at
`components/corpus-io/src/ehrt/corpus_io/er7.clj`, and its
`ehrt.tools.corpus.er7` citations (plus `formats.md`'s
`ehrt.tools.corpus.operation-manifest`) correctly resolve to
`ehrt.corpus-io.*`, NOT `ehrt.corpus.*` — the mechanical
tools-→corpus rename deliberately special-cased the namespaces that
left at stage 2. The same sweep covered `docs/glossary.md`,
`judge-calibration.md`, `docs/README.md`, `docs/dev/*`, README.md's
capability table, SETUP.md, AUTHORS-GUIDE.md, the Makefile's help
text (which still cited stage-1's `ehrt.tools.quickstart-fresh`/
`ehrt.tools.lint`), and `use-cases.edn`'s own stale
`ehrt.tools.judge.report`/`ehrt.kernel.canonical`-family citations.
The stale-path tripwire
(`ehrt.docs-tooling.stale-path-test`) gained `ehrt.tools.` as a
forbidden string and its `docs/experiments/` lookbehind moved from
`tools/` to `corpus/`, each with a positive and negative self-test;
its scoping was CONFIRMED, not assumed, per AR-7: it scans
`docs/**/*.md` plus `components/corpus/docs/use-cases.edn` only, so
the historical surfaces (`notes/**`, `.agents/session-records/**`,
archived prompts) that legitimately cite the old names are never
policed.

### Verification

Characterization baseline (HEAD `65e17c4`, clean tree, before any
edit): per-push lane exit 0, **193** `Testing ehrt.*` occurrences,
0 failures/0 errors; sha256 pins of the four generated docs;
`poly check` OK and the `poly deps` matrix captured; byte captures of
the five stage-2 seam commands (`corpus generate sim`, `corpus intake`
DIR, `corpus operators --format v2`, `gate v2` on a scratch COPY of
the v2 fixture dir so the report's embedded path is move-invariant,
`corpus mutate --operator-id blank-required-field --locator-path
PID-3` on the generated corpus's own msg-000.hl7).

Post-move, same commands: `poly check` OK (zero warnings). Per-push
lane exit 0, **193** occurrences — unchanged count, and the full
sorted occurrence-list diff is exactly 52-out/52-in: the corpus
brick's 20 test namespaces ×2 composing projects renamed
`ehrt.tools.* → ehrt.corpus.*` (incl. `sim-test → sim-adapter-test`),
and conformance's 12 project-test namespaces ×1 renamed
`ehrt.tools.* → ehrt.conformance.*`. Nothing added, nothing dropped —
and zero test deletions: the one deleted def (`Assertion`) had no
test of its own to take with it (its behavior tests live against
`check-corpus`).

Seam commands: every stdout/stderr byte-identical modulo the scratch
directory name; the generated corpus tree, intake catalog, lineage
sidecar, mutant bytes, and gate report all byte-identical;
`operation-manifest.edn` differs by exactly the `:git` field's
`-dirty` suffix (this session's own uncommitted tree — ADR-0017's
identical expected difference, F15).

Generated docs: legitimate diffs this stage, as the prompt expected,
each line attributable to a rename and nothing else — `pipeline.md`/
`operators.md` banners cite their renamed sources; `cli.md`'s
`--profile` hint carries the moved fixture path; `use-cases.md`'s
changed lines are all fixture paths, component-adjacent doc paths,
project-test paths, or the corrected `ehrt.judge.report/diff-reports`
citation (a grep for any changed line NOT matching the rename
patterns returns empty). CI's docsgen-freshness step regenerates
cleanly.

Integration lane: run once red (the judge-v2-hapi drop, below), fixed,
then green (exit 0) — its namespace census differs from stage 2's by
exactly the dropped judge-v2-nist brick suite plus the
`ehrt.tools.* → ehrt.integration.*`/`ehrt.corpus.*` renames.

**The structure-currency moment (AR-6): honestly, it never went red.**
`ehrt.docs-tooling.structure-currency-test` checks that every brick
directory name appears verbatim in AGENTS.md and architecture.md —
and the string `corpus` appears in both trivially, as a substring of
`corpus-io` and as ordinary prose, before either doc named the new
component. So this stage got no red→green evidence from the gate for
the rename itself (both docs were updated in the same pass
regardless), and the `tools` row REMOVALS relied entirely on this
stage's own sweep, exactly the presence-only asymmetry AR-5 predicted
— compounded by the substring weakness this stage discovered. Both
recorded as one named-future below.

### Deviation record

**Zero escalations fired.** The prompt's two anticipated escalation
surfaces both resolved by its own pre-rulings: defs consumed only by
project tests were kept and marked (AR-2's ruled disposition), and no
live `corpus → judge-*` engine edge existed (the engine requires
lived only in the façade's relay layer, so AR-3's graph held without a
ruling call).

**One unruled judgment call, made and disclosed: the project test
trees' namespace prefixes** (`ehrt.conformance.*`/`ehrt.integration.*`,
Decision above). The conservative alternative — leaving live test
namespaces under the retired `ehrt.tools.` prefix — was rejected as
contradicting AR-4's own total-retirement language; if the author
prefers different names, the rename is mechanical and isolated to the
two project test dirs.

**`operation-manifest.edn`'s `:producer :name` still says
`"ehrt.tools"`.** AR-6 requires the seam commands byte-identical, and
the producer name is embedded in emitted output — changing it is an
output-format change needing its own ruling, not a rename sweep's
side effect. Kept, with an explanatory comment at the construction
site (`ehrt.cli.core/mutate-producer`). Named-future.

**A genuine over-drop, caught only by running the integration lane —
stage 1/2's "Step 4 is a real command" lesson, again.** Dropping
`poly/judge-v2-hapi` from `projects/integration` was justified by
grep over that project's OWN test tree — which is the wrong scope:
poly runs every declared brick's tests in every composing project,
and the corpus brick's `v2_contract_pairing_test` requires the engine
interface. The lane's first run failed on exactly that require;
the dep was restored with the true reason in its comment, `poly
check` re-confirmed OK, and the lane re-run green. The `judge-v2-nist`
drop survives the same scrutiny (no brick on integration's classpath
carries any test-context edge to it).

**A mis-swept citation caught by a path-existence check, then fixed.**
The mechanical `components/tools/test/ehrt/tools/ →
components/corpus/test/ehrt/corpus/` path rule rewrote two citations
whose targets had actually moved ELSEWHERE in earlier stages
(`bin/quickstart-demo`'s header pointing at what is really
docs-tooling's `quickstart_fresh_test.clj`; the NIST fixture
NOTICE.md pointing at what is really judge-v2-nist's
`v2_engine_test.clj`) and `docs/locators.md`'s er7 link (really
corpus-io's). All three were caught by verifying every cited
`components/*` path exists on disk after the sweep — recorded as the
check that belongs in any future rename sweep from the start, and the
er7 one was ADR-0017's own flagged broken link, now genuinely fixed.

**`components/palgebra`'s deps-lint still polices the retired
`ehrt.tools.*` prefix** (its rule string and its seeded-violation
fixture `"ehrt.tools.lint"`). Left untouched, extending ADR-0016's
own precedent (the fixture is arbitrary realistic-looking test data);
but the RULE half is now vacuous — nothing can require a namespace
that no longer exists — so extending `deps-lint` to police
`ehrt.corpus.*`/`ehrt.corpus-io.*` instead is a real strengthening
decision, not a rename, and is recorded as a named-future rather than
smuggled into this stage.

**Named-future list (consolidated from all three stages — the
post-split cleanup backlog seed):**
1. `generator-source.clj`'s three concerns (execute-and-wrap,
   validate-and-shape, URL-parsing) — ADR-0017's note, still not a
   designed cohesion; untouched per AR-5.
2. `ehrt.corpus.display` is presentation-leaning — relocating it
   toward the base is a future call; it stays `ehrt.corpus.display`
   this stage (AR-5, flagged as ruled).
3. The structure-currency gate checks presence only, and by substring
   — it neither catches stale (removed-component) mentions nor
   meaningfully gates a new brick whose name is a substring of an
   existing one (`corpus` vs `corpus-io`, proven vacuous this stage).
4. `operation-manifest.edn`'s producer identity string
   (`"ehrt.tools"`) awaits an output-format ruling.
5. palgebra's deps-lint prefix list (above).
6. The duplicated pure markdown-table helpers
   (`ehrt.corpus.operators-doc` / `ehrt.docs-tooling.docsgen`, four
   small fns each) — ADR-0016's note: extract only when a third
   consumer appears.
7. `bases/sim-cli` + `projects/sim` retirement trigger (pre-split,
   F2) — unchanged, listed here only because this backlog consolidates.

---


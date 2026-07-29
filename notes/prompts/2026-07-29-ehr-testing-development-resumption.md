2026-07-29 — Development resumes: kernel/judge extraction, ehrt rename, audience-forked docs

Context
The migration and discipline-restoration era is closed (ADR-0006; `main` at `b0f88a8`+). This session resumes development: the deferred judge/foundation extraction (ADR-0002 R14's named hole), the CLI rename to `ehrt`, sim-cli deprecation, and a full documentation reorganization so the repo speaks as ONE system to clearly forked audiences. It also restores the commit/push session ritual (R6′) and is therefore the apparatus shakedown ADR-0006 A4 called for: use the facts register, write a session record, follow the staging-hygiene ritual — unprompted is the point.
This session is AUTONOMOUS. The author will not answer questions mid-session. Decision procedures below cover the anticipated ambiguities. For genuine surprises: if a sub-item is blocked, record it in the deviation record, skip it, and continue — report at session end. Full STOP only if the suite cannot be returned to green.
Ruling provenance tags (new convention, per the R6 lesson recorded in ADR-0007 below): [A] = author-ruled verbatim; [C] = channel- inferred default the author may veto post-hoc, fix-forward.
Read first

1. This prompt.
2. `AGENTS.md`, `AUTHORS-GUIDE.md` (the unioned versions), the R6 paragraph specifically.
3. `notes/ADRs.md`: ADR-0002 (R14, the deferred extraction; the judge/corpus census), ADR-0005 (the `ehr sim` mount), ADR-0006.
4. `components/tools/docs/positioning.md` — the seven audiences.
5. `notes/facts-register.md` — live; this session adds to it.

Author rulings
R30 [A] (supersedes R6; ADR-0007): Committing at checkpoints and PUSHING AT EACH CHECKPOINT are part of the session ritual — the author watches progress via the remote. The staging-hygiene ritual stays. Hooks gate every push. Still the author's alone: tags (ADR-0003's trust boundary) and repo-level `gh` mutations (create/delete/settings/visibility — the packs lesson, correctly scoped). Provenance of the correction goes in ADR-0007: R6 was a channel-inferred rule encoding a stale model of sim's practice, dressed as an author ruling, invisible to the parity audit for that reason; hence these provenance tags. R31 [A] The foundation component is named `kernel`. R32 [A] The CLI tool is renamed `ehr` → `ehrt`, pronounced "e-heart" (the pronunciation note goes in the README). Rationale on record: memorable, and `ehr` stays reserved for future payload-EHR tooling. R33 [A] `sim-cli` (the standalone sim CLI) is DEPRECATED, not removed: it keeps working, its tests keep running, but the user path never mentions it and the dev path marks it deprecated with the retirement trigger recorded: "retire when a review finds no use outside its own tests." `ehrt sim` is the presented surface. R34 [A] User docs MOVE to root `docs/` (option a). The user path is complete at top level; domain experts and informaticists never need to descend into `components/`, never meet Polylith, and never learn sim and tools were separate repos. The only external repo acknowledged anywhere user-facing is `ehr-testing-guide`. Historical repo names remain untouched in ADRs, errata, experiments, and frozen provenance — citations, not voice. R35 [C] The base `ehr-cli` is renamed to `cli` (namespaces `ehrt.cli.*`), avoiding the `ehrt.ehrt-cli` stutter; the project `tools-cli` is renamed `ehrt-cli` (projects name deployables; the deployable is now ehrt). Veto post-hoc if the names grate. R36 [C] Kernel membership by census, not by list: a root-layer namespace used by two or more of {judge, corpus, cli, conformance} → kernel; used only by judge → judge; otherwise it stays in tools. `check/` (schemas) follows the same rule. The expected kernel set (verify, don't assume): result, digest, canonical, artifact, lineage, locator, invocation. R37 [C] The residual `components/tools` remains after kernel and judge leave (roughly corpus + generate/mutate + docsgen + the sim adapter). Renaming it (e.g. to `corpus`) is deliberately NOT done — that is the next extraction era's decision, recorded as a named future item. R38 [C] GLOSSARY: full editorial merge of sim's and tools' glossaries into one root `docs/glossary.md`, entries alphabetized, source noted only where a term's meaning differs between origins (rare; reconcile and note). This is the one genuinely editorial task in the session; do it carefully. R39 [A] README ships without pre-commit review this session; author reviews post-hoc, fix-forward.
Phases (each COMMIT is also a PUSH, per R30)
Phase 0 — Ritual restoration
Amend the R6 text in AGENTS.md and AUTHORS-GUIDE.md to R30; write ADR-0007 (content per R30, including the provenance-tag convention). This commit—and its push—is itself the first act under the restored ritual.
COMMIT/PUSH `docs: ADR-0007 -- commit/push restored to session ritual; ruling provenance tags adopted`
Phase 1 — Kernel and judge extraction

1. Census: build the require graph of every `ehrt.tools.*` namespace; classify per R36. Record the census table in the session record — it is the extraction's evidence.
2. `poly create component name:kernel` and `name:judge`; move namespaces (`git mv`), rename `ehrt.tools.X` → `ehrt.kernel.X` / `ehrt.judge.X` accordingly (judge's existing `ehrt.tools.judge.*` → `ehrt.judge.*`).
3. Interfaces: kernel and judge get delegation interfaces sized by grep of actual external callers (the H2 method). Tools' wide interface SHRINKS by exactly the moved exports — width was a migration expedient (ADR-0002), and this is the first narrowing.
4. Wiring: tools/cli/conformance deps and requires re-point; `workspace.edn` and root deps.edn updated.
5. Gates, in order: `poly check` zero errors; `poly deps` shows judge→kernel and tools→{kernel,judge} with NO arrow from judge or kernel back into tools, none from kernel to judge, sim and palgebra untouched; full per-push lane green; `make ci-parity` green. Any cycle the census missed: resolve by moving the offending namespace per R36's rule; if no rule applies, record and leave that namespace in tools rather than force it.
6. ADR-0008: the extraction, the census table's summary, the dependency-direction facts now poly-enforced, R37's named future item (corpus extraction; residual-tools rename).

COMMIT/PUSH `feat: extract components/kernel and components/judge; tools interface narrows (ADR-0008)`
Phase 2 — The ehrt rename and sim-cli deprecation

1. `bin/ehr` → `bin/ehrt` (executable bit: the guard test exists — let it prove itself). Base and project renames per R35 (`git mv`
   * namespace rename + poly wiring).
2. Every surface: help text, docsgen sources (`pipeline.edn`, `use-cases.edn` — hand-authored, edit them, then REGENERATE the .md outputs; never hand-edit outputs), quickstart strips, Makefile, README/quickstart-demo coherence pair, the consumer-fidelity witness (`bin/ehrt sim run | bin/ehrt intake`), CLI smoke baselines (regenerate baselines — the OLD baselines died with the old name; note in deviation record that byte-comparison against pre-rename baselines is intentionally broken by this rename and re-established at the new name).
3. `ehrt version` output: name updates; version semantics untouched (H5's business).
4. sim-cli deprecation per R33: dev-docs marker + retirement trigger into the facts register as a dated entry.
5. ADR-0009: rename rationale (R32 verbatim), R35 names, R33 deprecation with trigger.
6. Gates: `poly check`; full lane; `make ci-parity`; `bin/ehrt help` exit 0 from a fresh clone.

COMMIT/PUSH `feat: CLI renamed ehrt ("e-heart"); base cli, project ehrt-cli; sim-cli deprecated (ADR-0009)`
Phase 3 — Docs disposition audit
`notes/docs-audit.md`: EVERY file under root `docs/`, `components/sim/docs/`, `components/tools/docs/` gets a row: USER-PATH-MOVE (target path) / DEV-PATH-MOVE / COMPONENT-ADJACENT-STAY / MERGE (into what) / PROVENANCE-RETIRE (to notes/, with reason). Known rows to pre-seed: two GLOSSARYs → R38 merge; two problem statements → merge into one user-facing "what is this" (history-free)

* retire originals to provenance; sim's stale `way-of-working.md` copy → retire (root's is canonical); two `research/` dirs → COMPONENT-ADJACENT-STAY (dev material); `docs/migration/` → DEV-PATH; `positioning.md` → DEV-PATH, revised for the unified repo, audiences amended to name domain experts and informaticists explicitly under practitioners.

COMMIT/PUSH `docs: documentation disposition audit -- every doc has a destination`
Phase 4 — Build the two paths

1. Execute the table (`git mv` for moves; merges are editorial).
2. Root `README.md` rewritten as the all-audiences front door: what the system is (one system, no history), the pronunciation note, the fork in the first screenful — "I want to generate/judge test data" → user path; "I want to maintain or extend" → dev path — and the single acknowledgment of `ehr-testing-guide`.
3. User path at root `docs/`: what-is-this, quickstart (ehrt-voiced), use-cases, operators, formats, simulate-your-facility, site-profiles, glossary. Scrub per R34: no Polylith, no repo history, no `components/` paths, no sim-cli.
4. Dev path at `docs/dev/`: architecture.md (NEW — the workspace map: bricks, projects, where theory docs live, written for a maintainer who has never seen this repo), way-of-working link, positioning (revised), migration brief, deprecation notices, pointers into component-adjacent docs.
5. Cold-reader check, executed and recorded in the session record: starting from README only, walk the domain-expert path click by click; every link resolves, no construction leaks; then the maintainer path the same way. Fix what the walk finds.
6. docsgen regenerated; quickstart-coherence tests green; the README/quickstart-demo pair test green.
7. ADR-0010: the documentation doctrine — audience fork, R34's history rule, where each class of doc lives and why, and the standing instruction that new docs declare their audience row.

COMMIT/PUSH `docs: audience-forked documentation -- user path complete at top level, dev path distinct (ADR-0010)`
Phase 5 — Close
Full verification once more (`poly check`, full lane, ci-parity); grep sweeps (old CLI name in user-visible surfaces: zero outside provenance/ADRs/baseline-note; repo-history mentions in user path: zero); facts-register entries this session created are present; session record written (census table, cold-reader walk, deviations); prompt self-archived — and note: per the sequencing lesson, archive the prompt as the FIRST item of this phase, before the final verification, so an interrupted close still leaves provenance.
COMMIT/PUSH `docs: session record and archived prompt -- development-resumption session`
Author actions after
A1. Post-hoc reviews: README voice (R39), R35 names, R38 glossary merge quality — veto any [C] ruling by follow-up instruction. A2. Watch the checkpoint pushes and CI as they land (that is R30 working as designed). A3. When satisfied, tag: `stable-tools-landing` is still untagged and its content-claim has grown — author's call whether to tag this head under that name or a fresher one (`stable-ehrt-1` [C] suggestion). The tag remains yours.
Deviation record
(Ruling-tag vetoes needed: none expected. Census surprises, blocked sub-items, baseline-regeneration note, then dated entries. Empty valid; absent not.)

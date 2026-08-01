# 2026-07-31 — ehr-testing-tools: split stage 3 — `tools` becomes `corpus`, then retires

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `65e17c4`
("refactor: extract corpus-io from tools (split stage 2, ruled
07-31)"), already equal to `origin/main` — no fast-forward needed. No
commit or push run by this session; the tree is left uncommitted,
coherent, with the proposed commit message printed in the session's
close-out. `/mnt/c` clone not touched (all edits made via the UNC
path onto the WSL ext4 clone, per the dual-clone-edit-hazard
discipline).

## Original prompt (verbatim)

2026-07-31 — ehr-testing-tools: split stage 3 — `tools` becomes `corpus`, then retires

Context
Final ruled extraction stage (author ruling 2026-07-31: `tools` retires after repoint — greenfield, no compatibility constraint). What remains in `tools` after stages 1–2 is the corpus domain: the `corpus/*` cluster (generate, generators, generator-source, intake, mutate, operators, manifest, golden-comparison), the check subsystem, diff, display, lineage, operators-doc, player, and the sim adapter. Stage 3 renames this domain to `ehrt.corpus.*` with a deliberately designed interface, repoints every consumer, and deletes the `tools` component entirely — after which `bases/cli` composes component interfaces directly (the shape stage 1's cycle resolution already established for `docs-tooling`). Unlike stages 1–2, this stage includes one sanctioned improvement: the interface itself. Everything else remains move-don't-improve.
Method precedents: `notes/prompts/2026-07-31-ehr-testing-split-stage1-docs-tooling.md` and `...-stage2-corpus-io.md` (characterize → extract → verify → records; escalate once with edges named, proceed on the answer). Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `65e17c4`), record HEAD. No commit/push; per-push lane between major steps; `/mnt/c` untouched.
Read first

1. `notes/ADRs.md` ADR-0016/0017 — the established dependency directions and both stages' stage-3 debt lists (relay exports, naming notes for the future `corpus` interface, `generator-source`'s three-concerns note).
2. `components/tools/src/ehrt/tools/interface.clj` (64 defs) — classify every def before designing anything.
3. `bases/cli/src/ehrt/cli/core.clj` and `help.clj` — the full consumer picture; also `components/docs-tooling/src/ehrt/docs_tooling/lint.clj` and any remaining lookup exports it uses through `tools`.
4. `workspace.edn`, root `deps.edn`, all project `deps.edn`s, `Makefile`, `.github/workflows/*` — every place the string `tools` is load-bearing.
5. `docs/formats.md` and `docs/locators.md` — the stale-path debt ADR-0017 flagged (including one broken relative link in locators.md), folded into this stage's sweep.

Author rulings

* AR-1 Name and shape. Component `corpus`, namespaces `ehrt.corpus.*`. The current nested `ehrt.tools.corpus.*` flattens to `ehrt.corpus.*` (e.g. `ehrt.corpus.mutate`); the non-corpus-prefixed domain files (`check`, `diff`, `display`, `lineage`, `operators-doc`, `player`, `sim`) become `ehrt.corpus.<name>` unchanged in content. The sim adapter is renamed `ehrt.corpus.sim-adapter` (its current name `tools.sim` collides confusingly with the `sim` component once the `tools` prefix is gone — this rename is in scope, content is not).
* AR-2 Interface design — the sanctioned improvement. Build the `corpus` interface from evidence, not inheritance: classify all 64 defs by live external consumer (cli / docs-tooling / project tests / none). Defs with consumers keep their signatures exactly (no signature changes anywhere) and get deliberate, corpus-vocabulary names where the old name was tools-façade residue — honor stages 1–2's naming-debt notes. Defs that exist only to relay `kernel`, `judge-*`, or `corpus-io` entries dissolve: their consumers repoint to the owning interface directly. Defs with zero live consumers are deleted, each listed in the ADR with the grep evidence. Expected outcome: the interface says what the corpus domain is, and its def count falls substantially below 64 — report the number, don't target one.
* AR-3 Dependency directions, final graph. Permitted: `corpus → {kernel, judge, judge-* engines only if a live edge exists today, corpus-io, sim}`; `docs-tooling → {corpus, corpus-io, tools?—no: tools ceases to exist}`; `bases/cli → any interface it needs`. Forbidden forever: `corpus-io → corpus`, anything `→ docs-tooling` except the base, any cycle. If a live edge today contradicts this graph, stop and escalate with the edge named.
* AR-4 Retirement is total. `components/tools/` is deleted; `poly/tools` disappears from every `deps.edn`; `workspace.edn` loses it; `:necessary` re-derived a fourth time (ADR comment-block format). No tombstone namespace, no alias nses. Prose references to "the tools component" across `AGENTS.md`, `docs/`, and `notes/` current-tense surfaces get swept — historical narrative in ADRs and archived prompts stays as written (fix-forward: they describe the past accurately).
* AR-5 Named-futures stay future. Do not execute: the `generator-source` three-concerns split; relocating `display` toward the base (it stays `ehrt.corpus.display` this stage — flag it in the ADR as presentation-leaning); extending the structure-currency test to catch stale (removed-component) mentions — note that today it only checks presence, so the `tools` row removals in AGENTS.md / architecture.md rely on your sweep, not the gate; record that asymmetry as a named-future.
* AR-6 Baselines and honesty. Stage 2's baseline set is the template: per-push lane namespace list (renames-only diff expected — plus deletions if dead-def removal takes tests with it: every deleted test must be justified by its deleted def, listed one-to-one), the seam CLI commands byte-identical, generated docs — expect legitimate diffs this stage (operators.md's renderer cites renamed namespaces; cli.md if help text embeds namespace names): each generated-doc diff must be attributable line-by-line to a rename, nothing else. `poly check` OK; both lanes green; structure-currency red→green moment on the `corpus` directory appearing.
* AR-7 Debt sweep rider. Fix `docs/formats.md` and `docs/locators.md` stale paths and the broken locators link, updated to post-stage-3 reality in the same change (one sweep covering all three stages' renames). Extend the stale-path tripwire's pattern list with `ehrt\.tools\.` as a forbidden string in `docs/**/*.md` current-tense prose — verify the tripwire's existing exclusion mechanics can distinguish the ADR/archive historical surfaces it must not police (it scopes to `docs/`, so it already can — confirm rather than assume).

Steps

1. Characterize. The 64-def classification table (def → consumers → disposition: keep / rename / dissolve-to-owner / delete). The full `tools`-string caller map (code, deps, Makefile, CI, docs). Baselines per AR-6. Escalate anything AR-2/AR-3 can't classify cleanly — expected escalation surface: defs consumed only by project tests (keep, but say so), and any live `corpus → judge-*` engine edge.
2. Execute. Rename, design the interface per the table, repoint all consumers (cli composes owning interfaces directly; docs-tooling's lint repoints; relay defs die), delete `components/tools/`, update all wiring per AR-4.
3. Verify per AR-6, integration lane once.
4. Records. ADR-0018 (the classification table lands in the ADR — it is the interface's design rationale), AGENTS.md + architecture.md (tools rows out, corpus in), facts-register F16 + Index, AR-7's doc fixes, archive this prompt at `notes/prompts/2026-07-31-ehr-testing-split-stage3-corpus.md` with deviation record.

Proposed commit message: `refactor: tools becomes corpus and retires (split stage 3, ruled 2026-07-31) -- interface designed from live consumers (N defs from 64, relays dissolved, dead defs deleted with evidence), cli composes component interfaces directly, :necessary re-derived, three-stage rename debt swept from user docs`
Close-out summary for the author
HEAD at start; the classification table headline numbers (kept/renamed/dissolved/ deleted); the final dependency graph as `poly deps` renders it; namespace-count accounting with the one-to-one deleted-test justification; generated-doc diff attribution; the named-futures list consolidated from all three stages (this is the seed of the post-split cleanup backlog); anything that surprised you.

## Deviation record

Full decision record, verification evidence, and the named-future list
live in `notes/ADRs.md` ADR-0018; this section records only where the
session's execution deviated from, or decided beyond, the prompt's own
text.

**Zero escalations fired — the prompt's two anticipated escalation
surfaces both resolved by its own pre-rulings.** Defs consumed only by
project/base test suites were kept and marked `test-consumer only` in
the interface source (AR-2's ruled disposition, applied to
`compare-catalogs`, `valid-catalog-entry?`, `valid-intake-record?`,
`ManifestV1_1`, `generator-register!`, `operator-register!`,
`operator-registry-snapshot`, `operator-registry-reset!`). And no live
`corpus → judge-*` engine src edge existed to escalate: the engine
requires lived only in the façade's relay layer, so AR-3's graph held
as ruled. Headline numbers: 64 = 38 kept (9 renamed) + 25 dissolved
(12 kernel, 5 judge, 8 engines) + 1 deleted (`Assertion`).

**One unruled judgment call, made and disclosed: the project test
trees' namespace prefixes.** The prompt ruled component namespaces
(AR-1) but said nothing about `projects/*/test/ehrt/tools/` — 18 live
namespaces under the retired prefix. Leaving them contradicted AR-4's
total-retirement language and would have left `git grep ehrt\.tools\.`
non-empty on live code while the new tripwire forbids the prefix in
docs. Renamed to `ehrt.conformance.*` / `ehrt.integration.*` — each
tree named after its own project, stating what these are
(project-composition suites, no brick's tests). Mechanical and
isolated to revert if the author prefers other names.

**A genuine over-drop, caught only by running the integration lane.**
`poly/judge-v2-hapi` was dropped from `projects/integration` on grep
evidence scoped to that project's own test tree — the wrong scope,
since poly runs every declared brick's tests in every composing
project, and the corpus brick's own `v2_contract_pairing_test`
requires that engine's interface. The lane's first run failed on
exactly that require; the dep was restored with the true reason in
its comment, and the lane re-run green. The `judge-v2-nist` drop
survives the same scrutiny. Recorded in ADR-0018's deviation record
as this stage's instance of the standing "Step 4 is a real command"
lesson.

**Three citations were mis-swept by the mechanical path rules and
fixed via a path-existence check** (every cited `components/*` path
verified to exist on disk after the sweep): `bin/quickstart-demo`'s
header (its target is docs-tooling's `quickstart_fresh_test.clj`, a
stage-1 move the old citation had never caught up with), the NIST
fixture NOTICE.md's engine-test citation (judge-v2-nist's, since
ADR-0012), and `docs/locators.md`'s er7 link (corpus-io's, since
ADR-0017 — the exact broken link AR-7 named). The existence check
belongs in any future rename sweep from the start.

**The structure-currency red→green moment AR-6 expected never
occurred, and honestly couldn't have:** the gate checks brick-name
presence by substring, and `corpus` is a substring of `corpus-io`
(and ordinary prose) in both policed docs, so the check passes
vacuously for the new component name. Both doc updates were made by
this stage's own sweep regardless. Recorded in ADR-0018 as one
named-future together with AR-5's presence-only asymmetry.

**Kept deliberately despite the rename, each with a recorded reason:**
`operation-manifest.edn`'s `:producer :name "ehrt.tools"` (AR-6
byte-identity is explicit; changing an emitted identity string is an
output-format ruling, named-future); palgebra's deps-lint prefix list
and its seeded-violation fixture string (ADR-0016's own precedent,
extended — the rule half is now vacuous, named-future); historical
narrative in ADRs, archived prompts, `.agents/session-records/`,
`notes/tools/**`, and the component's own `docs/research/` spike notes
(AR-4: they describe the past accurately).

**Beyond the prompt's named sweep surfaces, the same change also
fixed** (fix-forward, disclosed rather than silently expanded):
`.gitattributes`' three load-bearing `-text` fixture rules (a checkout
that renamed the paths without them would CRLF-mangle the wire-format
fixtures); Makefile help text still citing stage-1's
`ehrt.tools.quickstart-fresh`/`ehrt.tools.lint`; SETUP.md's
namespace-shadow note; AUTHORS-GUIDE.md's dependency-direction quote,
which also still asserted the superseded subprocess-only sim
consumption (`poly/sim` has been a real classpath dependency since
ADR-0005) — corrected to current truth; six sibling interface
docstrings (kernel, judge, all three engines, palgebra) whose
"ehrt.tools.interface re-applies its own qualification" narratives
became false the moment the relays dissolved; and stale pre-ADR-0008
citations inside the moved component itself (`ehrt.tools.canonical` →
`ehrt.kernel.canonical` family), where current-tense.

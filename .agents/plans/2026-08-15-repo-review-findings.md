# Repo review 3 — the third assessment (findings register)

Findings-only register for the `repo-review` skill's third survey
(`.agents/skills/repo-review/SKILL.md`, steps 1-4). This is the arc's
OPENING instrument, not a fix session — every row is a recommendation,
never an executed fix. The only mutations this session made are the
Step-0 rubric amendment (`dbbeb1f`), the deferred micro-arc tag
(`stable-20260815-result-nodes`), this register, and the plan.

Row format matches reviews 1 and 2: `id | probe | evidence | finding |
recommendation | disposition`. Disposition in {ruling-needed,
fix-session-candidate, close-as-fine, intake}. Every row's evidence was
gathered by the mechanism the rubric names for its own dimension —
re-derive, re-hash, re-run, never re-read a claim as its own
verification.

Landed 2026-08-15 against tip `dbbeb1f` (this session's own Step-0
amendment commit, on top of `b139de5`, ADR-0135's close).

The prior assessment (`.agents/plans/2026-08-09-repo-review-findings.md`,
review 2, `notes/adr/0092-repo-review-2.md`) is this review's baseline.
Its scoreboard is carried forward below.

**This run is the first under the amended rubric.** Step 0 landed the
population-closure law and its three dimension patches (D5, D1, D7) in
`dbbeb1f`, and this review executed the amended text immediately. The
amendment's own three predicted catch-sites are marked **[AMENDED]** in
the tables below. All three fired, and two of them found defects that
every prior review scored green.

---

## Step 0 — record

| act | evidence |
|---|---|
| Preflight | `bin/preflight`: last five CI runs on main all green; edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/*`); tree clean including untracked; HEAD matches `origin/main`; last `stable-*` tag `stable-20260814-exact-name`. **Premise deviation, disclosed:** the driving prompt specified `bin/preflight --expect-tag stable-20260814-exact-name`; that flag does not exist (unknown args exit 2). The substance was verified directly instead — `git rev-parse stable-20260814-exact-name^{}` = `46b82babf1e109f6a5748f175f8a687419a3ea3e`, exactly the commit the prompt named. Recorded as a prompt-vs-tree premise mismatch, not silently adapted. |
| Deferred tag paid | `bin/tag-ceremony stable-20260815-result-nodes b139de58… --push` — annotated, gitleaks clean, pushed, remote peeled ref verified equal to target. License case (i) conditions both present in this prompt's context: design-channel fresh-clone verification (relayed, including an independent cross-environment converter re-run, byte-identical) plus the author-side CI relay (`gh run list`, run 31884986962 green on `b139de5`). |
| Amendment | Four edits to `.agents/skills/repo-review/SKILL.md`, verbatim as prompted; all four anchor texts confirmed present in the live file before editing; mirror byte-copied to `.claude/skills/repo-review/SKILL.md`; committed and pushed as `dbbeb1f`, message ASCII, `bin/post-push-verify` clean, CI green at `dbbeb1f`. |

---

## AR-RR3-1 — Prior arithmetic re-derivation (skill step 4's standing correction)

Method: direct mechanical extraction of every row whose first cell is a
`D<n>-<id>` label across all eight of review 2's dimension tables, with
the disposition read from each row's last cell — never trusting review
2's own summary line.

| dim | rows (re-derived) | review 2's own claim |
|---|---|---|
| D1 | 9 | 9 |
| D2 | 18 | 18 |
| D3 | 5 | 5 |
| D4 | 12 | 12 |
| D5 | 3 | 3 |
| D6 | 6 | 6 |
| D7 | 14 | 14 |
| D8 | 9 | 9 |
| **Total** | **76** | **76** |

Disposition tally, re-derived: **close-as-fine 57**,
**fix-session-candidate 8**, **ruling-needed 5**, **intake 5**, plus
**1 non-tallied cross-reference row** (D6-4). 57+8+5+5+1 = 76.

**Result: review 2's own summary line re-derives EXACTLY, in every
figure.** No correction owed. This is the first review whose predecessor
needed no arithmetic fix-forward (review 1's own summary was off by
44/26/6 vs 45/28/5). A methodological note for review 4: a naive
`^\| D[0-9]+-[0-9]+ \|` extraction undercounts by 3, because rows
D4-5/D4-6/D4-7 carry a `**(new)**` marker inside the id cell. The count
above used a loose id match and was cross-checked against the strict
one; the discrepancy is in the extractor, not in review 2.

---

## AR-RR3-2 — History scan, window 2026-08-09 → 2026-08-15

Window: ADR-0092 through ADR-0135 — **44 ADRs**, by direct count
(`ls notes/adr/`), the largest window of any review to date (review 2's
was 11). Every ADR in the window carries its own `Deviations` section
(verified by heading grep across all 44; only the two review/rulings
ADRs and a handful of pure-close ADRs use a differently-titled
equivalent) — the disclosure discipline is structurally intact across
the whole window.

Incidents named as a required minimum by this session's driving prompt,
each classified and folded into its owning dimension rather than
double-counted:

| # | incident | source | dimension(s) | evidence | disposition |
|---|---|---|---|---|---|
| H-1 | The channel's own misclassification of `sim-theory-diagram.md` as hand-authored ("unearned specificity") | ADR-0135, this arc's design-channel conversation | D5, D1 | The file's own first line has read `<!-- GENERATED by the string-diagram skill from docs/sim-theory-equations.txt` since it was authored. The misclassification was owned on the record rather than smoothed over, and it is the proximate cause of the debt sitting undetected for three sessions. | close-as-fine as an incident (disclosed and owned) — **the structural cause is counted at D5-4** |
| H-2 | Pipe-masked `make test` exit codes (`tail` swallowing `make`'s exit) | ADR-0135's own Step-3 account | D4, D2 | Probed directly: `git grep` for a gate command (`make test`/`make docsgen`/`poly test`/`bin/*`) piped into `tail`/`head`/`grep`/`tee`/`sed`/`awk` across `bin/`, `.agents/skills/`, `.github/`, and the `Makefile` returns **zero hits**. The defect lived in ad-hoc session practice, not in any tracked file. This review's own baseline run captured `MAKE_EXIT=0` explicitly for exactly this reason. | close-as-fine for the tree; **counted at D2-6** as a session-practice gap with no gate |
| H-3 | `bin/post-push-verify` scanned `a8a5e65..b139de5` while the push spanned `00bdad7..b139de5` | ADR-0135's close; caught and hand-covered in-session | D1, D2 | **Confirmed a live defect in the script, not a session slip** — see D1-6. The session's catch was diligence covering for a tool that is wrong by construction. | fix-session-candidate — **counted in full at D1-6** |
| H-4 | The manual-review-2 arc (ADR-0134) and the ADR-0130–0135 closes | ADR-0130 through 0135 | D7, D8 | All six closes landed with rulings appends, roadmap rows, session records and archived prompts; the pairing/index/done-pointer gate family is green in this review's own full-suite baseline. ADR-0130's own deferral (busy-tuesday exerciser row deferred on a real slug-EDN round-trip defect) was diagnosed under a true name (ADR-0131) rather than smoothed, then executed (ADR-0132). | close-as-fine |
| H-5 | Two debts escaping every prior review (the charter incident) | The channel post-mortem, 2026-08-14 | D5, D1, D7 | The post-mortem's diagnosis — three probes each reading a registry as its own population — is confirmed correct by this review, and **understated**: the same error is present in a fourth place (`stale_path_test.clj`'s own scan root, D1-2) and a fifth (`bin/post-push-verify`'s range derivation, D1-6). | **counted at D1-2, D1-6, D5-4, D7-4** |

**Cross-window pattern:** review 2's headline pattern was "a fix that
closed the literal case a probe happened to hit." This window's pattern
is its structural parent: **a probe, gate, or tool whose POPULATION is
a registry rather than the tree.** Five independent instances are
recorded in this register (D1-2, D1-6, D5-4, D7-4, and the amendment's
own motivating trio). This is the failure mode the Step-0 amendment
names, and its recurrence count is the argument for the amendment
having been worth landing before the review rather than after.

---

## Dimension 1 — Claim-reality coherence

Probes: fresh `wc`/`ls`/`grep` re-derivation of every stated count;
re-run of the recorded baseline metric; and the **[AMENDED]**
path-resolution sweep at full-tree scan-root scope.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D1-1 | Re-derive structural counts | 18 components, 1 base, 3 projects — unchanged from review 2. `notes/adr/*.md` (excl. README) = **133**; `notes/ADRs.md` index entries (`^- \*\*ADR-`) = **133**. Exact match. `stable-*` tags = 91 (up from review 2's 48, consistent with 44 window ADRs each licensing a successor tag plus this session's own two). | Every structural count re-derives clean; the ADR index/file reconciliation is exact and is additionally gated (`index-completeness-test`, green in this review's baseline). | None. | close-as-fine |
| D1-2 **[AMENDED]** | Re-resolve every cited path, scan roots covering **every tracked doc surface including `components/*/docs/`** (the amended D1 probe). 4,750 citations extracted across live surfaces; markdown links resolved from each file's own directory, backticked root-anchored paths from the repo root | **25 dead markdown links on live reader-facing surfaces, ALL 25 in `components/*/docs/`. Zero in `docs/`, zero in README/SETUP/AGENTS/AUTHORS-GUIDE, zero in `demos/`.** Two sub-classes: (a) **19 un-re-depthed `../` prefixes** — files that moved from a repo-root `docs/` to `components/<x>/docs/` at the merge gained two path segments and their relative prefixes were never adjusted (`components/sim/docs/third-party-sources.md` → `../notes/facts-register.md` resolves to `components/sim/notes/facts-register.md`; the correct target is `../../../notes/facts-register.md`, verified to exist); (b) **6 whose target is genuinely gone** (`.agents/memory/patterns.md`, `.agents/plans/archive/judge-gate-refactor.md`). **The gate that should catch this exists and is green:** `stale_path_test.clj`, whose own docstring reads *"Deliberately scoped: this scan covers docs/ (plus the use-cases.edn source above) only"* — and whose own origin (P1-1, 2026-07-31, finding 4) was **this exact link family**. It fixed the instances inside `docs/`, scoped its population to `docs/`, and has been green ever since over a population that excludes where the remaining 25 live. | **The third recorded hit of the scan-root class, and the first one found inside a GATE's population rather than a probe's.** E-5 (2026-08-05) and the sim-theory recipe path (2026-08-14) were the first two. `components/sim/docs/third-party-sources.md` is the highest-severity single instance: it is a licensing/provenance document whose six dead links all point at `notes/facts-register.md`, the register its own third-party claims rest on. | Widen `stale_path_test.clj`'s scan root to every tracked `*/docs/**/*.md` surface, then fix the 25 links in the same session — 19 are a mechanical `../` → `../../../` rewrite. The gate widening must land first and be witnessed red against the current tree. | **FIXED 2026-08-15 (ADR-0137)** — `stale_path_test.clj` gained a fourth scan: dead markdown-link resolution over every `*.md` under `docs/**` AND `components/<x>/docs/**`, component doc roots enumerated from the filesystem rather than a hand-list, with a test asserting that. Red witnessed at **exactly 25**, all under `components/<x>/docs/`, before any fix; remainder after the 19 mechanical re-depths was **exactly 6**, the stated midpoint. The `"Deliberately scoped"` sentence — the finding itself — is retired for a per-scan population statement naming all four scans, each population, and how each is enumerated. **The 6 were not a second class:** both "gone" targets exist, frozen — these docs came from the pre-merge `tools` repo, where `.agents/memory/patterns.md` and `.agents/plans/archive/judge-gate-refactor.md` were live siblings, and the merge froze that tree into `notes/tools/agents/`. Same relocation defect as the other 19, destination moved too. Ruled R-B1 "Re-point all six", label text included. Two halves the prompt asked for were **registered rather than improvised** (R-B2, R-B3) — see the new rows below. |
| D1-3 | Re-derive the recorded full-suite baseline metric by re-running it, unpiped | `make test` from the verified-clean tree: **`MAKE_EXIT=0`** captured explicitly; **636** `0 failures, 0 errors` occurrences; 318 `Test results:` blocks, every one zero-failure; **16,315 passes**; zero `FAIL in`/`ERROR in` lines; `bin/verify-nist-lock` OK. | **ADR-0135's own recorded figure of 636 re-derives EXACTLY**, by the same metric the record names (occurrences, not blocks). A clean positive control for the record's own measurement discipline. | None. | close-as-fine |
| D1-4 | Re-derive the oracle root count and the vendored-test-file count, and check the correspondence review 2 asserted between them | Oracle roots (string keys in `digest.clj`'s `roots` map): **34**. `vendored_*_test.clj` files: **36** (28 `sim-emit-hl7`, 8 `sim-trajectory`). Set comparison: 7 roots have no like-named test file (`death-fixture`, `sinusitis`, four `*-engine` roots); 9 test slugs have no like-named root (`uti`/`tjr` appear once per component). | Both counts are internally sound, but **review 2's D1-4 claim that the root count "matches the fresh vendored-test-file count exactly" was a coincidence of totals (34 = 34), not a structural correspondence** — the two populations use different naming and one duplicates across components. The totals have now diverged (34 vs 36) with nothing wrong. A same-class error to the one this review's amendment addresses: two registries equated without checking that they enumerate the same thing. | No repo change. Review 4 should compare the two sets, not their cardinalities. | close-as-fine (method note for the next review) |
| D1-5 | `bin/check-palgebra-drift`'s own header claims it is a "**Nightly** drift check" | Grepped every invocation surface — `Makefile`, both `.github/workflows/*.yml`, all of `bin/`, all of `components/` — for `check-palgebra-drift`: **the only hit is the `Makefile`'s comment listing it among targets that "stay superseded."** Nothing invokes it, nightly or otherwise. It would additionally clean-skip (exit 0) even if invoked, because it compares against a sibling `../ehr-testing-sim` checkout that this workspace's own merge retired. | A script whose own header states an enforcement cadence it does not have. Same shape as review 2's D2-4 (`verify-nist-lock`'s false enforcement claim) — which was found, ruled, and fixed. This is its unfound sibling. | Either delete the script with a carve-loss-audit row, or correct its header to state it is a manual, sibling-checkout-only tool that nothing schedules. Cheap either way; the author's call which. | **FIXED 2026-08-15 (ADR-0136)** — ruled R-1 (a), "accept all.": deleted, with the zero-caller inventory re-derived at deletion (not inherited from this row) and recorded in the commit message and in a new `notes/carve-loss-audit.md` "Later dispositions" row. `bases/cli/test/ehrt/cli/executable_bits_test.clj` names it in its docstring only and enumerates tracked files dynamically, so no `src`/test edit was needed. |
| D1-6 | `bin/post-push-verify`'s default range derivation, against its own documented contract | Header (lines 10-13) documents the `<base-sha>` default as *"the range's own merge-base with origin/`<branch>` before this call — in practice, the sha that was origin's tip immediately before your push."* Code (line 54): `base_sha="$(git rev-parse --verify "${tip_sha}^1")"` — **the tip's first parent, i.e. always exactly one commit**, regardless of how many the push carried. Re-derived against the incident: the ADR-0135 push spanned `00bdad7..b139de5` = **4 commits**; the default range for tip `b139de5` covers **1**. The other three commits' messages went unchecked, silently. | **Confirmed live defect, and a population error of exactly the class this review's amendment names**: the tool's stated population is "the pushed range," but it enumerates from the commit graph's parent link — a registry that cannot know what was pushed. The ADR-0135 session caught the gap and hand-covered it; that was diligence compensating for a wrong tool, and it will not recur reliably. | Derive the default from the remote's pre-push tip (`git rev-parse origin/<branch>` captured before the push, or the reflog's `origin/<branch>@{1}`), and fail loudly rather than defaulting when it cannot be derived. Co-land a test that pushes a synthetic multi-commit range and asserts every message is checked. | fix-session-candidate |
| D1-7 | Sample `[V]`-style live claims in `.agents/state.md` | `state.md`'s own citation pointer resolves to a real ADR; the staleness tripwire (`state_staleness_tripwire_test.clj`) is green in this review's baseline, which is the mechanical form of this check. One cosmetic hit: `state.md` cites the literal template string `notes/adr/NNNN-slug.md`, which the path sweep flags and which is correct as written (a template, not a citation). | Clean; the `NNNN-slug` hit is a sweep false positive, recorded so review 4 does not re-flag it. | None. | close-as-fine |
| D1-8 | Path-sweep false-positive control (the rubric's own "read each hit in context" discipline) | Three distinct false-positive classes identified and excluded before any row above was written: (a) this repo's **shorthand citation convention** (`sim/run.clj` for the full brick path) — thousands of hits, all legitimate; (b) **generator template sources** (`components/corpus/docs/use-cases.edn`), whose links are authored to resolve at the generated output's location, `docs/use-cases/` — ~50 hits, all legitimate; (c) `docs/dev/migration/polylith-brief.md`'s **external tutorial examples** (`projects/billing`, `components/invoice`) — legitimate, they cite Polylith's own docs. One further false positive: `components/corpus/docs/experiments.md`'s `%20`-encoded link to `research/License Status of NIST HL7 v2 Validation Software  Evidence-Based Classification.md`, which exists with literal spaces and resolves correctly in any markdown renderer. | Recorded so that the 25 in D1-2 are defensible as findings and so review 4 inherits the exclusion list rather than rediscovering it. | The widened gate proposed in D1-2 must encode all four exclusions, or it will land noisy and be weakened. | **ENCODED IN GATE 2026-08-15 (ADR-0137)** — all four classes carry their own passing test in `stale_path_test.clj`. Three are structural rather than by-list: (a) shorthand backticked citations, because the scan resolves markdown link destinations only; (b) the `use-cases.edn` template, because the scan reads `*.md` and the template's rendered output is in the population and resolves; (d) percent-encoding is decoded, not excluded. Only (c), `polylith-brief.md`'s external tutorial examples, is by name — it cannot be structural, since those paths are shaped exactly like real ones — and its test asserts the same shape still trips from any other file. **D1-8's exclusion list proved incomplete for the backticked half**, which is why that half was registered instead of built: see the new row below. |
| D1-9 **[NEW, opened by fix session B]** | The root-anchored **backticked** path half of D1-2's own probe method, re-derived against the live tree while building the widened gate | D1-8 class (a) excludes `sim/run.clj` correctly, but the shorthand convention is far broader than that shape. Measured: a first-segment-is-a-repo-root-entry reading checks 683 backticked candidates and leaves **216 dead**; adding component-root resolution still leaves **95**, and **many of those sit inside `docs/`**, which D1-2 measured as clean. The alternative reading, "root-anchored" = leading slash, yields 8 candidates, all OS absolute paths (`/tmp/exp-d3/`, `/root/.fhir/packages`) — 8 false positives and a vacuous check. The residue is dominated by a class D1-8 never named: post-relocation **basename shorthand** (`docs/notation.md` cited from anywhere, meaning that doc wherever it now lives, e.g. `components/corpus/docs/notation.md`), alongside backticked command lines (`bin/ehrt play … --board 60`, 60 hits), globs (`components/*/docs/*.md`, 44), and `file.clj:21-23` line suffixes. | **D1-8's exclusion list is incomplete**, and the gap is only visible once you try to build the gate rather than run the probe. Neither reading reaches green under the four documented classes, so shipping this half would have required inventing exclusions silently — the exact failure mode D1-8 exists to prevent. **Real findings are inside the residue**, not just noise: `components/tools` (a component retired at stage 3) cited twice in `docs/dev/architecture.md`, and `.agents/plans/corpus-foundations.md` cited five times in `docs/dev/source-sink-design.md`. | Give the backticked half its own session: name the basename-shorthand class (and the not-a-path candidate rules — command lines, globs, line suffixes) in this register first, then build the check against that stated exclusion set, then triage the residue. The 25 markdown links are already fixed and gated, so this session has no link-fixing on its critical path. | fix-session-candidate (opened by ADR-0137 under ruling R-B2, *"Ship link-half, register the rest"*) |
| D1-10 **[NEW, opened by fix session B]** | Would widening the *retired-name denylist* families (`stale_path_test.clj` scan 1) to `components/<x>/docs/` be clean? — the scan the retired `"Deliberately scoped"` sentence actually described | **No: 15 further files go red.** `ehr_testing_tools` (1: `components/corpus/docs/experiments/EXP-A4-results.md`), `ehrt.tools.` (1: `components/corpus/docs/research/judge-v2-nist-spike-notes.md`), and `(?<!corpus/)docs/experiments/` (14 files across `components/corpus/docs/` and `components/judge-v2-nist/docs/`). | Same species as D1-2 — a gate population that excludes where the hits live — but **not the same fix**. Most of the 14 look like the D1-9 basename-shorthand false positive: inside `components/corpus/docs/`, the sibling form `docs/experiments/EXP-C5.md` **is** correct, and the pattern's `(?<!corpus/)` lookbehind was written for the pre-merge layout. The two namespace hits sit in frozen experiment/spike records, the same narrative-legitimacy class the docstring already carves out for `notes/`. Triage, not a sweep. | Re-scope the `docs/experiments/` pattern for the post-merge layout (or retire it — the component-adjacent form it was written to enforce is now the default), rule on whether frozen experiment records under `components/<x>/docs/` inherit `notes/`'s narrative legitimacy, then widen scan 1. Fix session B kept scan 1 at `docs/` and stated that population as fact rather than leaving it behind "deliberately scoped". | fix-session-candidate (opened by ADR-0137 under ruling R-B3, *"Register it, keep docs/ scope"*) |

**Dimension 1 register summary:** 10 rows (8 at review time, plus D1-9
and D1-10 opened by fix session B, ADR-0137). **4 close-as-fine, 4
fix-session-candidate, 1 ruling-needed, 0 intake, 1 encoded-in-gate.**
D1-2 is FIXED; D1-8 is encoded in the gate.

**Dimension 1 verdict: YELLOW (down from GREEN).** Every re-derived
count is clean and one of them (the 636 baseline metric) reproduces a
recorded figure exactly by the record's own method. The downgrade is
driven entirely by the amended probe and its neighbours: 25 dead links
on live component-doc surfaces that three consecutive reviews scored
green because their sweeps stopped at `docs/`, plus two tracked scripts
whose headers assert enforcement they do not have. None is severe
alone; the pattern is that this dimension's own instrument was
measuring a subset and reporting it as the whole.

---

## Dimension 2 — Guard coverage

Probes: enumerate every gate from the tree (`ls` of the docs-tooling
test directory, both workflow files read in full, `Makefile` read in
full), then map claimed laws onto them.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-1 | Review 2's D2-4 (orphaned `verify-nist-lock`) — fixed? | `.github/workflows/test.yml` now carries an explicit `verify-nist-lock (supply-chain integrity)` step, and `make test`'s own target runs it (confirmed in this review's baseline log: `bin/verify-nist-lock` OK on all NIST coordinates). | **FIXED**, confirmed by reading the workflow and by the check actually executing in a live run — not by trusting ADR-0095's account. | None. | close-as-fine |
| D2-2 | Review 2's D2-18 (requires-vs-classpath static gate) — fixed? | `components/docs-tooling/test/ehrt/docs_tooling/project_classpath_test.clj` exists and is green in this review's baseline. | **FIXED.** Review 2's two fix-session candidates in this dimension are both closed. | None. | close-as-fine |
| D2-3 | Enumerate the gate population from the tree | 35 test namespaces under `components/docs-tooling/test/ehrt/docs_tooling/` (up from review 2's 27), including six landed this window: `citation_gate_test`, `exercised_sources_test`, `link_footnote_gate_test`, `mermaid_render_test`, `project_classpath_test`, `invocation_lint_test`. | The gate population grew by 8 in one window, every addition traceable to a ruled finding. The repo's fix-with-a-gate discipline is working at a visible rate. | None. | close-as-fine |
| D2-4 | Which derived artifacts have a freshness gate? (population taken from D5-4's tree-first enumeration, not from the make graph) | CI's freshness step diffs exactly five paths: `docs/dev/pipeline.md`, `docs/use-cases.md`, `docs/use-cases/`, `docs/operators.md`, `docs/cli.md`. **Five further derived artifacts exist in the tree with no gate of any kind** (D5-4). | A gate whose population is the make graph cannot see a derived artifact that was never added to the make graph. Structurally identical to D1-2 (a gate scoped to `docs/`) and D1-6 (a tool scoped to one parent commit). | See D5-4's recommendation — registering the derivations is the fix, and it closes this row too. | **FIXED 2026-08-15 (ADR-0136)** — CI's freshness step now diffs ten paths, not five, and its header comment records the obligation a new derived file carries (a make target AND a diff-list entry, same commit). |
| D2-5 | `bin/check-palgebra-drift` — a gate that runs nowhere | See D1-5. Zero invocation surfaces. | Counted at D1-5; recorded here because it is a guard-coverage fact as much as a claim-reality one. | See D1-5. | **cross-reference to D1-5, not separately tallied** |
| D2-6 | Is there any gate against the exit-code-masking class (H-2)? | No tracked file contains the pattern (probe in H-2), so nothing is currently broken. But nothing prevents a future session from writing `make test \| tail -40` in an ad-hoc command either — and the standing `build-session` ceremony does not name "capture the gate's exit code explicitly" as a step. | A real, currently-unrealised gap: the failure mode is session practice, and session practice in this repo IS gated in other places (staging hygiene, message-via-file, ASCII commit messages) by being written into the skill. | Add one line to `build-session`'s verification step: gate output is captured to a file with the exit code recorded explicitly, never read through a pipe that can eat it. Costs nothing and matches how the other practice laws are enforced here. | fix-session-candidate (trivial, doc-only) |
| D2-7 | Laws stated on multiple surfaces with no drift gate | The skill mirror is gated (`skill_mirror_currency_test`, green, `diff -rq` exit 0 across all 17 directory pairs re-verified independently this session). The structure/currency pair (`AGENTS.md` ↔ `docs/dev/architecture.md`) is gated (`structure_currency_test`). Tag law is gated (`tag_law_test`). | The multi-surface-drift family review 1 named is now covered at every instance this review could enumerate. Review 2's D2-17 watch item ("has the third-law-drifts trigger fired?") — still not fired. | None — re-probe at review 4. | close-as-fine (watch item, unchanged) |

**Dimension 2 register summary:** 6 tallied rows (D2-5 is a
cross-reference, not tallied). **4 close-as-fine, 2
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 2 verdict: YELLOW (unchanged).** Both of review 2's
fix-session candidates are closed and confirmed real by direct
verification, and the gate population grew by eight. It stays yellow on
the same structural note as D1: three of this review's findings (D2-4,
D2-5, and D1-6 counted upstream) are gates or tools whose population is
narrower than the thing they are believed to cover, and none of them is
detectable by running the gate — only by enumerating its population
from the tree and diffing.

---

## Dimension 3 — Environment independence

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D3-1 | Full-suite truth from a genuinely cold clone | **Substituted, disclosed:** this session did not run `make ci-parity` locally. The substitute evidence is CI's own run at this session's amendment commit `dbbeb1f` — `completed / success` — which is by construction a fresh `actions/checkout` clone with a cold artifact cache, running `poly check`, `poly test :all skip:integration`, `verify-nist-lock`, and the docsgen freshness diff. The last five runs on main are also all green (preflight). Local full-suite baseline on the working tree: green, `MAKE_EXIT=0`, 636/636. | Fresh-clone truth is established for this tip, by a genuinely cold runner. What is NOT established this session is the wider-scope local cold probe review 2 ran (HOME repointed, `EHR_TESTING_TOOLS_CACHE` repointed) — that probe covers author-machine-only assumptions CI's own image would not reveal. | Run the full local cold-clone probe at review 4, or once before the next arc close, so the review-2 method does not lapse to CI-only evidence two runs running. | intake (probe partially run; recorded as such rather than scored on CI alone) |
| D3-2 | The loopback flake's soak status | Zero recurrence in this review's own full-suite baseline (636/636, zero `FAIL in`/`ERROR in`). Still named in `.agents/state.md:668` — *"Named again for the next session that owns test-suite hygiene"* — and, per D7-4, still carries **no roadmap row**, 18 days after it was first recorded (`dc52a25`, 2026-07-28). | Evidence keeps strengthening; the tracking keeps not being anchored. The flake itself is close to closeable on sample size; the item's *carriage* is the finding, and it is counted at D7-4. | See D7-4. | close-as-fine as a flake (**tracking counted at D7-4**) |
| D3-3 | Anything depending on untracked files, author-local checkouts, or network | One found: `bin/check-palgebra-drift` reads a sibling `../ehr-testing-sim` checkout (D1-5). It is honest about this in its header and clean-skips when absent, so it cannot produce a false green — but the repo it compares against was consolidated into this workspace, so the check is inert by construction, not merely unscheduled. | Counted at D1-5. No other author-local or network dependency surfaced. | See D1-5. | close-as-fine |

**Dimension 3 register summary:** 3 rows. **2 close-as-fine, 0
fix-session-candidate, 0 ruling-needed, 1 intake.**

**Dimension 3 verdict: YELLOW (unchanged).** Fresh-clone truth holds at
this tip on cold-runner evidence, and no new hermeticity incident
surfaced in the window. It does not move to green because this
review substituted CI for review 2's own stronger local cold probe, and
a dimension should not improve its score in the same run that its
headline probe was weakened — recorded honestly rather than scored
optimistically.

---

## Dimension 4 — Error honesty

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D4-1 | Review 2's D4-5/D4-6/D4-7 (three unguarded `json/read-str`/`edn/read-string` operator-input reads) — fixed? | All three read in full at their live sites. `read-base-data` (`bases/cli/core.clj:441`) is now `try`/`catch` → `result/error :base-data-unreadable`, and its own docstring cites **"ADR-0078, ADR-0096, D4-5/D8-3"** by finding id. The `--baseline` read (`:1191`) → `:baseline-unreadable`; the `--assertions` read (`:2076`) → `:assertions-unreadable`. | **All three FIXED**, confirmed by reading the live code rather than the fix ADR, and each fix names the review finding that caused it — the review→fix→citation loop closing cleanly. | None. | close-as-fine |
| D4-2 | Review 2's D8-3 (permission-denied on an existing-but-unreadable file producing a raw stack trace in three commands) — fixed? | Live-executed against a `chmod 000` file: `ehrt gate fhir` → exit 2, categorized; `ehrt show` → exit 2, `:path-unreadable`. No raw stack trace from either. | **FIXED** — the incomplete-fix finding that drove review 2's D8 downgrade is closed under the wider trigger that exposed it. | None. | close-as-fine |
| D4-3 | Category honesty of the above: does the category name the actual condition? | `ehrt show` on an unreadable-but-present file reports `:path-unreadable` (accurate). `ehrt gate fhir` on the **same** file reports `:file-not-found` (inaccurate — the file was found; it could not be read). Sibling commands disagree on the category for one condition. | Low severity and loud either way, so not a silent-success defect — but a stranger diagnosing a permissions problem is told the file does not exist, which points them at the wrong fix. | Route `gate`'s pre-check through the same `:path-unreadable` category `show` already uses. Trivial, and it can ride along with any CLI-touching session. | fix-session-candidate (trivial) |
| D4-4 | Exit-code-masking class (H-2) across tracked files | Zero hits, full population of `bin/`, `.agents/skills/`, `.github/`, `Makefile` (probe in H-2). | Clean in the tree. | Gap in session practice counted at D2-6. | close-as-fine |

**Dimension 4 register summary:** 4 rows. **3 close-as-fine, 1
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 4 verdict: GREEN (unchanged).** Every one of review 2's
error-honesty findings is closed, verified against the live code rather
than the fix ADRs, and the fixes cite the findings by id. The single new
row is a category-naming inaccuracy in a path that already fails loudly
and safely.

---

## Dimension 5 — Mirror and derivation drift  ⚠ headline

Probes: byte-diff every mirrored pair; regenerate every derived doc and
compare — where **[AMENDED]** "every derived doc" is enumerated from
the tree first and then diffed against the make graph's targets.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D5-1 | Byte-diff every mirrored pair | `diff -rq .agents/skills .claude/skills` → exit 0, zero differences, 17 directories each side — including the `repo-review` skill this session amended in `dbbeb1f`, which is precisely the commit its own D5-2-descended gate had to survive. | No mirror drift anywhere. | None. | close-as-fine |
| D5-2 | Regenerate every **registered** derived doc and compare | `make docsgen` → exit 0; `git status --porcelain` **empty**; zero bytes of diff across all five registered outputs. | Every derived doc inside the make graph is byte-current with its live source, verified by regenerating, not trusted. | None. | close-as-fine |
| D5-3 **[AMENDED]** | Enumerate "every derived doc" **from the tree** — grep tracked files for generation banners, converter references, and embedded regeneration recipes — then diff that population against the make graph's targets | Tree-first enumeration finds **10** derived artifacts. The make graph registers **5** (`docs/dev/pipeline.md`, `docs/use-cases.md`, `docs/use-cases/*.md`, `docs/operators.md`, `docs/cli.md`). **Five are unregistered**, each carrying a generation banner or produced by the same converter, none named by any make target or CI diff step: `components/sim/docs/sim-theory-diagram.md`, `components/sim/docs/sim-theory-diagram.mermaid`, `components/palgebra/examples/ai-study-flow-v3.mermaid`, `components/palgebra/examples/committee-flow.mermaid`, `components/palgebra/examples/deliberated-choice-flow.mermaid`. | **Class confirmed: unregistered derivation, ×5.** The prompt predicted one (`sim-theory-diagram.md`) and said the enumeration might find others; it found four more. | Add a `make` target per derivation and extend CI's freshness diff to cover them — which retires the header-recipe workflow entirely and makes the D5-4 staleness below impossible to reintroduce. | **FIXED 2026-08-15 (ADR-0136)** — `make sim-theory` (converter + an `awk` splice into the `.md`'s embedded block, so all three surfaces agree byte for byte) and `make palgebra-examples`, both folded into `docsgen`; both header recipes retired for target pointers, the equations file's 46-line length preserved so no `%% Arrow N` renumbering occurred. |
| D5-4 **[AMENDED]** | For each unregistered derivation, regenerate and diff — the freshness question the registry-scoped probe structurally could not ask | `sim-theory-diagram.md`/`.mermaid`: **current** (ADR-0135 regenerated them by hand last session). **The three `components/palgebra/examples/*.mermaid` are STALE**, 12-13 differing lines each. Causation pinned, not inferred: the committed files contain **0** `_out` nodes, freshly regenerated they contain **6**; the diff is exactly ADR-0135's result-node feature (`%% --- Result types (terminal outputs) ---`, the `Op -- "name" --> name_out` wires, and the green `style …_out fill:#e8f5e9` block). | **The headline finding of this review.** ADR-0135 changed the converter (`8c9f291`) and regenerated every artifact the make graph knew about, plus `sim-theory` under a mid-session license — and left three shipped example diagrams stale. Those three are the **string-diagram skill's own teaching material**: they now demonstrate, to any reader who opens them, precisely the defect ADR-0135 was chartered to fix — every codomain dead-ending at the operation box with nothing wired out. The gap was invisible to `make docsgen` + diff (not in the graph), to CI (not in the diff list), to `bin/check-palgebra-drift` (it pairs the `.txt` sources and the `.py`, never the `.mermaid` outputs — and runs nowhere anyway, D1-5), and to ADR-0135's own careful manual sweep. | Regenerate all three in the fix session that lands D5-3's targets, and register them so the next converter change cannot repeat this. Red-first: the new gate must be witnessed failing against the current tree, which it will, three times. | **FIXED 2026-08-15 (ADR-0136)** — red witnessed first, CI's step run verbatim against `fca52ec`: **exactly three** failures, exactly these three, `sim-theory` correctly not among them. All three regenerated. **One correction to this row's own account, found by regenerating rather than by reading:** the delta is exactly the result-node feature for `committee-flow` and `deliberated-choice-flow`, but `ai-study-flow-v3` was **two** converter generations behind — additionally missing the gate/spider styling that predates ADR-0135. Recorded because it sharpens the finding: the unregistered population had been drifting longer than this row said. |

**Dimension 5 register summary:** 4 rows. **2 close-as-fine, 2
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 5 verdict: RED (down from GREEN, twice held).** Both
registry-scoped probes are immaculate — mirror byte-identical,
`make docsgen` producing zero bytes of diff — and both were immaculate
in reviews 1 and 2 for the same reason: they were asking about a
five-member population inside a ten-member one. Widening the population
by the amendment's own instruction immediately surfaced three live,
byte-verified stale artifacts whose content teaches the exact convention
this repo fixed six commits ago, plus five artifacts with no
regeneration path registered anywhere.

**The counter-argument, stated plainly for the author:** the three stale
files are teaching examples under `components/palgebra/examples/`, not
user-facing documentation, and nothing in the shipped product is wrong.
A reading of YELLOW is defensible on blast radius. RED is scored here on
the other axis the rubric names — a whole population sitting outside
every gate, with demonstrated live drift inside it, held green by three
consecutive reviews. The author may reasonably re-score this to YELLOW;
the finding rows themselves do not change either way.

---

## Dimension 6 — Sampling adequacy

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D6-1 | Review 2's D6-1 (census `:closure-file-count` undercounting lookup-table CSVs) — the most-aged item in the register, carried since ADR-0071 with an unhonoured "escalate priority" ask | Read live at `components/sim-trajectory/src/ehrt/sim_trajectory/census.clj:443`: `:closure-file-count (+ (count modules) (count tables))`, carrying an explicit `AR-D-6` comment, with the lookup-table resolver (`lookup_tables/` keyed reads, `:216`-`:224`) feeding `tables`. | **FIXED (ADR-0094), confirmed by reading the live counting logic, not the fix ADR.** The item that two consecutive reviews flagged as aging without a ruling was ruled and closed inside this window. | None. | close-as-fine |
| D6-2 | Re-derive the `defspec` population and the seed-pin policy | 105 `defspec` forms (up from review 2's 71 — the population grew ~48% in one window, tracking the manual and injuries arcs' new property work). | Growth is large but every addition is inside work this window's ADRs account for. No seed-policy drift found. | Review 4 should re-derive the seed-pin status across the grown population rather than sampling it, since the population nearly doubled. | close-as-fine (method note) |
| D6-3 | Is the 300-patient round-trip convention still exercised at its stated power? | Present across the vendored test family (confirmed by direct grep of `:patients 300` across `*_test.clj`). Re-derived power unchanged from reviews 1 and 2: at p=1%, `(1-p)^300` = 4.90%; at p=3.3%, 0.0042%. | The convention holds and the arithmetic re-derives identically for the third review running. | None. | close-as-fine |
| D6-4 | Did the window produce a new sampling-adequacy miss? | **Partial probe, disclosed:** this review read the window's ADR headings and deviation-section structure across all 44, and probed the two incident classes the driving prompt named, but did not read all 44 Deviations sections in full. No sampling miss surfaced in what was read; the coverage is honestly less than review 2's, which used a dedicated history-scan agent over an 11-ADR window. | Not a clean negative — an incomplete one. Recorded per the rubric's "a probe that could not run is recorded as blocked, never skipped." | Review 4 should either narrow its window or budget the full deviation read explicitly; a 44-ADR window is past what a single-session scan covers at review-2 depth. | intake |

**Dimension 6 register summary:** 4 rows. **3 close-as-fine, 0
fix-session-candidate, 0 ruling-needed, 1 intake.**

**Dimension 6 verdict: GREEN (up from YELLOW, held twice).** The single
finding that held this dimension yellow across both prior reviews — the
census closure-file-count undercount, carried since ADR-0071 and
escalated without effect at review 2 — is fixed, and the fix was
verified by reading the live counting expression. The round-trip
arithmetic re-derives identically for a third time. The upgrade is
qualified by D6-4's incomplete window scan, which is why that row is
intake rather than close-as-fine.

---

## Dimension 7 — Continuity integrity

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D7-1 | Pairing / index / done-pointer / mirror gate family, re-run | All green in this review's own full-suite baseline (636/636, zero failures): `index_completeness_test`, `done_pointer_adr_test`, `skill_mirror_currency_test`, `prompt_record_pairing_test`, `roadmap_deferred_closure_lint_test`, plus this window's new `citation_gate_test` and `link_footnote_gate_test`. | The continuity-gate family holds and has grown. | None. | close-as-fine |
| D7-2 | Review 2's D7-7/D7-8 (wellness-encounters and `notice_verbatim_test` carried as prose-only horizon notes with no roadmap anchor) — fixed? | Both now carry roadmap rows: `wellness` 9 hits in `.agents/plans/roadmap.md`, `notice_verbatim` 2, `closure-file-count` 2. | **FIXED** — review 2's structural recommendation (anchor the item, don't rely on the next session mentioning it) was executed, and the anchored items are exactly the ones that stayed visible. | None. | close-as-fine |
| D7-3 **[AMENDED]** | Grep tracked files for standing requests embedded **outside** the registers, then check each against a register row | Full-tree grep on the amendment's own terms. After excluding upstream-vendored Synthea `TODO`s (which must stay verbatim), `bin/close-scaffold`'s template placeholders, and requests already mirrored in `roadmap.md` (the Wave-E / vital-sign family, 10 roadmap hits) — **two genuinely unregistered standing requests remain**: (a) `components/sim-model/resources/sim-model/demographics/NOTICE:26`, *"A future session WITH a Synthea checkout available can replace the content of these three files wholesale with a real extraction"* — first appears `3f43a46`, **2026-08-05**, zero hits for `demographics` in either `roadmap.md` or `state.md`; (b) `docs/dev/source-sink-design.md:56` row **OPEN-4** (whether `corpus generate` grows an `--engine` flag), marked **Open** in its own table since `499cad4`, **2026-07-29** — zero hits for `OPEN-4` in `roadmap.md`, and the roadmap's four `source-sink` rows are all about MLLP sink kinds, a different question. | **Class confirmed: unregistered standing request, ×2**, aged 10 and 17 days respectively, both invisible to the carried-item aging probe by construction because that probe enumerates the registers. The sim-theory instance the prompt flagged is correctly **discharged** and says so in its own header (`ADR-0135 REGENERATION (2026-08-15)`), so the probe's expected first catch behaved exactly as the amendment predicted. | Land a roadmap row for each — Deferred with a revisit trigger for (a) (its trigger is literally "a session with a Synthea checkout"), Next or Deferred for (b) per the author's view of whether `--engine` is still live. Two-line edits, not a design session. | **REGISTERED 2026-08-15 (ADR-0136)** — ruled R-2, "accept all.": both landed as roadmap rows, visibility first, disposition later. (a) Deferred, revisit trigger stated verbatim, plus a pointer paragraph at the `NOTICE` itself (safe: that file is this repo's own provenance prose, holds no verbatim upstream bytes, and carries no SHA-256 provenance table for `notice_verbatim_test.clj`). (b) Next, carrying OPEN-4's own question as the row's question — deliberately not answered here; Next rather than Deferred because Deferred rows owe a revisit trigger and this one has none yet. |
| D7-4 | Carried-item aging across the register surfaces | The loopback flake (`ehrt.conformance.mutate-stdout-stdin-loopback-test`) is named in `.agents/state.md:668` and **nowhere in `roadmap.md`** — 18 days after `dc52a25` (2026-07-28) and across every arc close in the window. This is the same shape as review 2's D7-7/D7-8, which were fixed by adding roadmap anchors; this is the instance those fixes did not reach. | A third instance of the pattern review 2 diagnosed and half-closed. `state.md` is regenerated at closes and is not a durable anchor — that was review 2's own finding, restated here with a live example that survived the fix. | Add the roadmap row. Then the item is anchored and D3-2's soak can actually be closed against a stated bar. | **REGISTERED 2026-08-15 (ADR-0136)** — Deferred roadmap row landed, carrying the durable-anchor diagnosis and a stated closing bar: no recurrence by the next repo review closes this row and D3-2 together against the accumulated green runs. |
| D7-5 | Attic-vs-live consistency; frozen-provenance boundary | `roadmap.md`'s Deferred section is lint-green (`roadmap_deferred_closure_lint_test`). The frozen boundary holds: `notes/sim/` and `notes/tools/` were excluded from every sweep in this register per `AGENTS.md`'s own rule, and no live document was found citing them un-origin-qualified. | Clean. | None. | close-as-fine |

**Dimension 7 register summary:** 5 rows. **3 close-as-fine, 1
fix-session-candidate, 1 ruling-needed, 0 intake.**

**Dimension 7 verdict: YELLOW (unchanged).** The gate family is green
and growing, and review 2's roadmap-anchor recommendation was executed
and demonstrably worked for the items it reached. It stays yellow
because the amended probe found two standing requests that have aged 10
and 17 days entirely outside the aging probe's field of view, plus a
third instance of the anchor-less-item pattern the last review thought
it had closed.

---

## Dimension 8 — Operator experience

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D8-1 | CLI error matrix, run live against the built CLI | Bare invocation → usage, exit 0. `help` → usage, exit 0. **`--nonexistent-flag` at the bare level → exit 2, `:unknown-flag`** — review 2's D8-4 finding (a typo'd global flag silently swallowed, exit 0) is **FIXED**. `corpus mutate --nope` → exit 2 `:unknown-flag`; `gate fhir /no/such/file.json` → exit 2 `:file-not-found`; `bogusverb` → exit 2 `:unknown-command`. | The exit-code contract holds across the matrix and review 2's gap is closed. | None. | close-as-fine |
| D8-2 | Review 2's D8-4 residual nuance: `corpus --nonexistent-flag` categorised as `:unknown-command` rather than `:unknown-flag` | Re-probed live: still `:unknown-command`, with `:args ["corpus"]`. This is the group-with-no-verb path, where the flag is never reached. | Unchanged, low severity, arguably correct as written (the missing verb is the first error). | Optional; no change recommended. | close-as-fine (status quo) |
| D8-3 | Review 2's D8-7 (README linking a non-existent `docs/adr/…` path) — fixed? | Zero `docs/adr/` references remain in `README.md`, and the live-surface link sweep (D1-2) found **zero** dead links in `README.md`, `SETUP.md`, `AGENTS.md`, `AUTHORS-GUIDE.md` or anywhere under `docs/`. | **FIXED**, and the whole storefront surface is clean under a sweep far wider than the one that found the original defect. | None. | close-as-fine |
| D8-4 | Help output at 40 / 80 / 120 columns | Max observed line width exactly 40, 80, 120 respectively. | Degrades cleanly at all three widths, third review running. | None. | close-as-fine |
| D8-5 | Execute every live command fence across README, `docs/**`, the 21 use-case pages, `components/*/docs/**`, `demos/**`; plus `make quickstart` and `make integration` | **BLOCKED — not run this session, recorded rather than skipped.** Review 2 ran this battery through a dedicated sub-agent with live-execution latitude; this session ran without sub-agents and spent its execution budget on the full-suite baseline (which alone took ~25 minutes of wall clock) and the amended probes. `make integration` additionally requires a primed artifact cache. | No evidence either way about fence currency this window — and the window landed six user-facing arcs (the manual arc, ADR-0119-0125, plus ADR-0134's manual review) that touched exactly this surface. This is the largest single coverage gap in this register. | Run the fence battery as its own session before review 4, or explicitly budget it into review 4's own plan. It should not lapse a second run. | intake |

**Dimension 8 register summary:** 5 rows. **4 close-as-fine, 0
fix-session-candidate, 0 ruling-needed, 1 intake.**

**Dimension 8 verdict: YELLOW (unchanged) — held, not earned.** Every
review-2 finding in this dimension is confirmed fixed (D8-4's silent
flag swallow, D8-7's broken README link, and D8-3's permission-denied
gap counted at D4-2), the exit-code matrix is clean, and help wraps
exactly at three widths. On the evidence gathered, this dimension would
score GREEN. It is held at YELLOW because its single most informative
probe — executing the live fences — did not run, over a window that
landed the entire user manual. Scoring green on a battery that was not
executed would be exactly the "registry as population" error this
review's own amendment exists to prevent.

---

## Scoreboard — reviews 1, 2, 3

| dimension | review 1 (2026-08-07) | review 2 (2026-08-09) | review 3 (2026-08-15) | movement |
|---|---|---|---|---|
| D1 — Claim-reality coherence | GREEN | GREEN | **YELLOW** | **regressed** — the amended scan root found 25 dead links three reviews scored green |
| D2 — Guard coverage | YELLOW | YELLOW | **YELLOW** | unchanged (both review-2 candidates closed; two new population-scope gaps) |
| D3 — Environment independence | YELLOW | YELLOW | **YELLOW** | unchanged (cold-runner evidence green; local cold probe substituted, disclosed) |
| D4 — Error honesty | RED | GREEN | **GREEN** | unchanged — all three review-2 parse findings closed |
| D5 — Mirror and derivation drift | GREEN | GREEN | **RED** | **regressed** — 5 unregistered derivations, 3 demonstrably stale |
| D6 — Sampling adequacy | YELLOW | YELLOW | **GREEN** | **improved** — the census undercount, aged since ADR-0071, is fixed |
| D7 — Continuity integrity | GREEN | YELLOW | **YELLOW** | unchanged (anchors landed and worked; 2 unregistered requests found) |
| D8 — Operator experience | GREEN | YELLOW | **YELLOW** | unchanged, held on an unrun probe rather than on evidence |

**Overall: review 1 was 4 green / 3 yellow / 1 red. Review 2 was 3
green / 5 yellow / 0 red. Review 3 is 2 green / 5 yellow / 1 red.**

The headline number looks worse and the repository is not. Every single
finding that reviews 1 and 2 left open in D4, D6, D7 and D8 is closed,
verified against live code rather than the fix ADRs, with several fixes
citing the finding id that caused them. What moved D1 and D5 the other
way is not new decay — it is the amendment: two dimensions that were
green because their probes measured a subset turned out, on first
contact with their real populations, to be carrying defects that had
been there for weeks. D5's three stale diagrams have been wrong since
`8c9f291`, six commits ago; D1's 25 dead links have been wrong since
the merge.

---

## Register summary

**40 total rows** across 8 dimensions (D1: 8, D2: 7, D3: 3, D4: 4,
D5: 4, D6: 4, D7: 5, D8: 5), plus 5 history-scan rows (H-1…H-5) folded
into their owning dimensions rather than double-counted, and the Step-0
record table.

Disposition counts, tallied directly per dimension:
**close-as-fine 26**, **fix-session-candidate 8**, **ruling-needed 2**,
**intake 3**, plus **1 non-tallied cross-reference row** (D2-5, counted
at D1-5). 26+8+2+3 = 39 disposition-carrying rows, +1 cross-reference =
40 total, matching the row count exactly.

*Arithmetic note, disclosed fix-forward and in the spirit of the
standing correction this register applies to its predecessor: this
line's first draft claimed 35 rows and 24/7/2/3, and did not sum. The
figures above are the mechanical recount — every table row whose first
cell is a `D<n>-<id>` label, disposition read from the last cell — run
against this file before it was committed. Both counts of
"close-as-fine" include D3-2's "close-as-fine as a flake". Review 4
should re-derive these from the rows regardless, not trust this line.*

**Probes recorded as blocked or partial, never silently skipped:**
D3-1 (local cold-clone probe substituted by CI's own cold runner),
D6-4 (44-ADR window not read to review-2 depth), D8-5 (live fence
battery not executed). All three are the direct cost of running without
sub-agents over a window four times larger than review 2's, and all
three are named in the plan.

**The single cross-dimension pattern worth naming:** five independent
instances of one error — a probe, gate, or tool whose population is a
registry rather than the tree. `stale_path_test`'s scan root (D1-2),
`bin/post-push-verify`'s range derivation (D1-6), CI's freshness diff
list (D2-4/D5-3), `make docsgen`'s target set (D5-4), and the
carried-item aging probe's register enumeration (D7-3). Each was green,
each was green *correctly* against its own stated population, and each
was hiding a real defect just outside it. That is the amendment's thesis,
and this review is its first evidence.

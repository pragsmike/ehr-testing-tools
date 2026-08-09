# Repo review 2 — the second assessment (findings register)

Findings-only register for the `repo-review` skill's second survey
(`.agents/skills/repo-review/SKILL.md`, steps 1-4; `notes/adr/0092-
repo-review-2.md`). This is the arc's OPENING instrument, not a fix
session — every row is a recommendation, never an executed fix
(AR-RR2-1); the only mutations this session made are the standing-
ceremony tag (AR-RR2-0), this register, the paired ADR, and ceremony
files. Row format matches review 1's own established shape: `id |
probe | evidence | finding | recommendation | disposition`.
Disposition in {ruling-needed, fix-session-candidate, close-as-fine,
intake}. Every row's evidence was gathered by the mechanism the
rubric names for its own dimension — re-derive, re-hash, re-run, never
re-read a claim as its own verification. Eight of the eight probe
batteries ran independently (this session ran D1/D3/D5 directly,
single-command-shaped, same precedent review 1 set; D2/D4/D6/D7/D8 ran
as five independent, parallel, read-only sub-agents under the same
discipline, plus a sixth agent for the history scan). Clean probes are
recorded in full alongside findings, per the skill's own "a green
probe is inheritance, not noise" instruction.

Landed 2026-08-09 (repo review 2, `notes/ADRs.md` ADR-0092), against
tip `451d159` (ADR-0091's own closing commit, tagged
`stable-20260809-storefront-fixture` at this session's own Step 0).

The prior assessment (`.agents/plans/2026-08-07-repo-review-
findings.md`, review 1, `notes/adr/0077-repo-review-1.md`) is this
review's own baseline. Its scoreboard is carried forward below,
alongside this run's own column.

---

## AR-RR2-2 — Prior arithmetic re-derivation

Method: direct count, by disposition column, across all 8 of review
1's own dimension tables, never trusting either the register's
original summary line OR its own 2026-08-07 fix-forward correction
note without independently recounting.

Per-dimension disposition tally, recounted fresh from the actual rows:

| dim | close-as-fine | fix-session-candidate | ruling-needed | intake | non-disposition rows | total rows |
|---|---|---|---|---|---|---|
| D1 | 9 | 0 | 0 | 0 | 0 | 9 |
| D2 | 3 | 3 | 2 | 0 | 0 | 8 |
| D3 | 1 | 2 | 1 | 1 | 0 | 5 |
| D4 | 3 | 1 | 0 | 0 | 0 | 4 |
| D5 | 3 | 0 | 0 | 0 | 0 | 3 |
| D6 | 3 | 0 | 1 | 1 | 0 | 5 |
| D7 | 3 | 1 | 1 | 1 | 1 (D7-4, the aging-table pointer row, disposition "—") | 7 |
| D8 | 3 | 2 | 0 | 0 | 0 | 5 |
| **Total** | **28** | **9** | **5** | **3** | **1** | **46** |

**Result: this session's own independent recount reproduces the
register's own 2026-08-07 fix-forward correction EXACTLY** — 45
disposition-carrying rows (28 close-as-fine + 9 fix-session-candidate
+ 5 ruling-needed + 3 intake), 46 total rows including D7-4's
non-disposition aging-table pointer row. No further drift found beyond
what the prior correction already fixed — the correction has now held
across two independent recounts (the fix session that made it, and
this review), a genuinely clean audit trail.

This re-derivation follows the rubric's own standing instruction
(AR-RR2-2): "before drafting this run's own register, re-derive the
PRIOR assessment's own summary arithmetic directly from its
per-dimension rows — never trusted from its own summary line." The
prior register's ORIGINAL summary line (44/26/6) was already wrong,
caught and corrected fix-forward the same day (2026-08-07, fix session
1, `notes/adr/0078-result-or-loud.md` AR-RL-R). This review's own
independent recount confirms the corrected figures are accurate.

---

## AR-RR2-3 — History scan, window incidents (ADR-0081 through ADR-0091)

The window since review 1 (2026-08-07, ADR-0077 predecessor) through
this review's own tip (`451d159`, ADR-0091): the fidelity arc
(0081-0084), the conviction arc (0085-0089), vendoring batch 4 (0090),
the storefront fixture (0091), and the two closing ADRs of the
PRECEDING quality-review arc (0079-0080, which landed chronologically
after review 1's own register but is properly this window's opening
act, not review 1's — its own gates are re-verified fresh throughout
the D-dimension tables below).

Every incident named as a required minimum by this session's own
driving prompt, dimension-classified, with disposition. Rows already
fully counted inside a D-dimension table below are marked
"counted at [id]" rather than double-counted in the scoreboard; rows
that exist only as narrative context (no separate probe needed beyond
the history scan's own re-derivation) carry their own disposition
here.

| # | incident | source | commit(s) | dimension(s) | evidence | disposition |
|---|---|---|---|---|---|---|
| H-1 | Two-session arc-close resumptions (fidelity close, an accidental infra-block interruption; conviction close, the same pattern ADOPTED as a deliberate pre-split) | ADR-0084 "the two-session deviation record"; ADR-0089 "the pre-split adoption" | fidelity: `e7961b9`->`0227f2a`; conviction: `ed90706`->`0d7140d` | D7, D2 | Second-in-a-row infra-block interruption of an arc close (first: vendoring close, ADR-0074, pre-window) led the author to RULE the two-session shape as standing practice ("Close. adopt, two close sessions.") rather than treat it as an accident to avoid — first deliberate execution, conviction close. A lightweight (non-annotated) tag slip during the pre-split's own Session A was self-caught by the session's own verification step, deleted, recreated annotated — evidence the deliberate two-session shape contains blast radius exactly as designed. | close-as-fine (a repeat incident class that the repo responded to by changing its own practice, not by absorbing the repeat silently — the corrective response is itself the finding) |
| H-2 | The sleep-apnea licensed mover | ADR-0086 "Step 1 — Blast radius, evidenced" | fix `e2cef25` | D6, D1, D5 | First-ever full straddle-freedom sweep over 28 pre-existing oracle roots found ONE mover (`sleep-apnea`, 3/300 walks) — a wellness encounter straddling the registration horizon. Session STOPPED before any fix code; author licensed the named mover at exact granularity (walks #17/#58/#269). Old digest ratified as WRONG (a wire-impossible discharge summary for a stay that never began), new digest MORE correct. Second full exercise of the predict-then-license discipline ADR-0082 first proved. | close-as-fine (counted at D1-4/D6, the oracle-root count and the licensed-mover discipline both re-verified live this review) |
| H-3 | The oracle blind-spot intake | ADR-0086, cited ADR-0089 | n/a (structural finding) | D1, D5, D6 | Byte-identity digest comparison cannot see a malformed COMPILED shape if that shape never changed — `sleep-apnea.json`'s dangling `:outpatient-visit-end` shipped since vendoring batch 1 (ADR-0070) and passed every oracle bracket since, because nothing had ever compared it until the straddle-freedom sweep. A genuine limitation of the repo's own primary regression instrument, named for review 2's own pairing-as-data adequacy conversation. | intake (a structural instrument limitation, not fixable by a small session — worth the author's own explicit acknowledgment that byte-identity oracles are a floor, not a ceiling, on semantic correctness) |
| H-4 | The `2088763` classpath break — a requires-vs-classpath gate gap | ADR-0091 "Preflight finding, fixed forward" | broke silently since `948f5e5`; fixed `2088763` | D2, D3 | `projects/integration/deps.edn` dropped `poly/judge-v2-nist` on 2026-07-31; `948f5e5`'s new test directly required that interface; `poly check` did not catch it (confirmed: ADR-0088's own Verification reports `poly check: OK` against the very tree with the gap); only the next scheduled `Integration` run caught it, up to a day later. | fix-session-candidate — **counted in full at D2-18**, this review's own new finding: no static gate checks a project's composed classpath against composed bricks' TEST-tree requires, and this is the second closely-related incident (with H-5, below) inside one 24-hour session window. |
| H-5 | The `cd08b20` red push — second warm-cache incident in the local-state-is-not-clone-state family | ADR-0091 "Mid-session correction" | red `cd08b20`; fixed `c690ec3` | D3, D1 | A `:judge-fhir-official` arm landed directly in `judge`'s own test tree (composed by every project, including ones whose push-lane never primes the artifact cache); passed locally on a warm cache, failed in CI cold. FIRST instance of this exact class: ADR-0004 (2026-07-28, pre-window), "local state is not clone state." The fix-forward (`c690ec3`) relocated the FHIR witnessing into `projects/integration` and verified hermetic for real — cache directory renamed out of the way, suite still green. This review's own D3-1 cold-cache probe applies the SAME method at full-repository scope (not just the artifact cache) as its own standing headline check. | close-as-fine as an incident (caught, disclosed, fixed same-session) — **but this repeat is the row driving D3's own YELLOW verdict** per the rubric's repeat-raises-severity instruction; see D3's verdict text. |
| H-6 | The em-dash commit-message flattening + a verification gap structurally blind to it | ADR-0091 AR-SD-6 | batch-4 commits `7767326`/`889287d` | D4, D2 | Batch 4's own commit messages flattened em-dashes to plain hyphens (channel report, one session later); this session (ADR-0091) adopted ASCII-only commit messages going forward, a standing practice this review's own driving prompt also follows. The standing post-push verification (`git log --format=%B -1` diffed against the source message FILE) only catches drift between the committed body and the file used to create it — if the flattening happens when the file itself is authored, the check reports clean by construction. Never caught in-session; caught only by an external channel report. | ruling-needed — a real, still-open verification-scope gap (the check verifies "committed matches file," never "file matches intent"); worth a small ruling on whether this is worth closing (e.g. a byte-level non-ASCII-character disclosure step) or accepted as an author-side authoring discipline with no mechanical gate, mirroring D2's own "not every law needs a gate" precedent. |
| H-7 | The ADR-0089 prompt-citation mismatch | ADR-0089 Intake; `.agents/prompts/2026-08-08-conviction-close-b.md` | n/a | D7, D4 | The driving prompt cited "AR-A-5 over-literal prompt wording" as an already-disclosed error; no such citation exists anywhere in the committed record under that tag (`AR-A-5` is used elsewhere only for the standing scaffolding-compaction-A relocation law). The executing session caught this itself, named the mismatch explicitly in ADR-0089's own Intake rather than inventing a matching tag, and substituted the closest real match (ADR-0087's AR-CP-2). | close-as-fine — **counted in full at D7-3**: the disclosure law fired correctly, this is a positive control for D7, not a violation. |
| H-8 | The two gitleaks false positives | ADR-0091 Verification | caught pre-remote | D2 | The `generic-api-key` heuristic tripped three times on the same session (two file-level triggers on adjacent short judge-id keywords, one on the ADR's own prose describing the fix) — resolved by reformatting to one keyword per line each time, never by allowlisting/suppressing, and never reached the remote in a tripped state. | close-as-fine (the gate fired on the wrong signal three times but was never bypassed — the resolution pattern, reformat rather than suppress, is the healthy one) |
| H-9 | The three skipped NIST pairing cells | ADR-0088 "Measurement"; restated 0089, 0091 | registry landed `948f5e5` | D6, D2 | Two cells (`:corrupt-encoding-characters`, `:corrupt-segment-name`) are structurally un-witnessable at this judge tier — the NIST parser throws before any finding-level class exists, not a sampling-power gap. One cell (`:malformed-datetime-value`) is masked by one fixed fixture's own pre-existing noise floor, not diluted by population variance. All three honestly disclosed as skipped, never forced; none is a catalog contradiction. Still open at `451d159`, unchanged since ADR-0088, named in three consecutive ADRs without closing. | intake (aged, carried-item — **counted at D7's aging table** as a candidate; the un-witnessable pair is a permanent, not temporary, gap and should be named as such rather than kept as an open "someday" item) |
| H-10 | The loopback flake — occurrence count across the window | ADR-0084 (origin), restated 0085/0086/0089 | n/a | D3, D7 | Fired exactly ONCE (ADR-0084, under heavy concurrent JVM load from the session's own parallel background runs), reproduced-as-non-reproducible via 3 clean re-runs the same session. Re-checked clean at every subsequent arc-close full-suite run plus this review's own fresh cold-clone probe (D3-1) — zero recurrence against ~45 `test`-lane pushes since the preceding fix. Never silently dropped from the watch-list; re-stated at every close. | intake — **counted at D3-2**, carried forward as the next review's own watch item with a now-larger, still-clean sample. |

**Additional deviations swept, not already covered above** (full detail
in the history-scan agent's own working notes; each classified and
folded into its owning dimension's table rather than repeated here):
the fidelity-riders near-miss premature tag (ADR-0081 AR-FR-0, D7/D2);
the `deabbbd` ~15-minute CI-red window from an in-place roadmap
closure the session's own Step-4 full-suite catch caught before Step 5
(ADR-0082, D2/D4); the repeated "diagnosis by adjacency is not a
diagnosis" lesson (AR-EE-1c and AR-FP-2, ADR-0082/0083, now codified as
a standing ruling, D1); ADR-0087's own disclosed-not-discarded
undercounted first measurement of `:suppressed-straddle-spans`
(counted in full at D6-2); ADR-0090's five explicitly-disclosed
deviations (the `:persona-config` mechanism correction, two whole
modules deferred under true diagnosed names rather than smoothed over,
one honestly-unattributed `unknown` fix bisection, and the `poly
test` untracked-file change-detection gap **explicitly self-flagged by
the repo's own record as a repeat across multiple batch ADRs** — D2);
ADR-0091's own disclosed one-day authorship/execution date gap, first-
draft fixture-authoring honesty (two real, unanticipated base-FHIR
rejections fixed before the clean baseline was declared), and the
stale README example output corrected in the same session (all D1).

**Cross-window pattern, named once here rather than three times below:**
three D2/D3-flavored hermeticity-or-verification-scope gaps (H-4, H-5,
H-6) surfaced inside the SAME session (ADR-0091) — a session-density
signal worth the author's own attention independent of any single
row's severity, alongside D2's own new finding (D2-18) that the
`2088763` class remains entirely unguarded today.

---

## Dimension 1 — Claim-reality coherence

Probed directly by the landing session (mechanism: `ls`/`wc`/`grep`
re-derivation and direct-source counting, never trusting a prose
claim).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D1-1 | `ls -d components/*/ \| wc -l`; `ls -d bases/*/ \| wc -l` | 18 components, 1 base | Unchanged from review 1; matches state.md. | None. | close-as-fine |
| D1-2 | `grep -c '^\| \`' components/sim/resources/sim/modules/NOTICE` | 77 provenance rows | state.md's own live-tagged claim (`[V @a9c3abf]`) says 71 — a gap of 6, fully reconciled: state.md's vendored-module section was last verified at conviction-close Session A's tip (`a9c3abf`), BEFORE batch 4 (ADR-0090, +5 veteran modules) and its 1 supporting lookup table (`veterans/veteran_suicide_probabilities.json`, also NOTICE-hashed) landed. 71 + 5 + 1 = 77 exactly. | None — the staleness is contractual (state.md regenerates at arc closes / verified tags, not every commit), the review-1 D1-7 pattern repeating cleanly. | close-as-fine |
| D1-3 | `find . -iname "vendored_*_test.clj"` total + per-component | 34 total (27 `sim-emit-hl7`, 7 `sim-trajectory`) | Up from review-1's 27 (20/7). The +7 in `sim-emit-hl7` are exactly accounted for: `vendored_anemia_test.clj`, `vendored_colorectal_test.clj`, and 5 `vendored_veteran_*_test.clj` files, all newer than the quality-arc-close ADR (confirmed by `find -newer`). Matches the window's own vendoring narrative (colorectal payoff ADR-0087, batch 4 ADR-0090) exactly. | None. | close-as-fine |
| D1-4 | Direct read of `components/oracle/src/ehrt/oracle/digest.clj`'s `roots` map | 34 keys, hand-counted from the literal map | Matches the Step-0 oracle pre-digest's own bracket (34 roots IDENTICAL) and the fresh vendored-test-file count (D1-3) exactly. Up from review-1's 27: colorectal +1, veteran family +5, net +6 this window (review 1 had already counted the earlier baseline). | None. | close-as-fine |
| D1-5 | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | 27 files | Up from review-1's 23 — exactly +4, and the four new files are `io_vocabulary_lint_test.clj`, `roadmap_deferred_closure_lint_test.clj`, `state_staleness_tripwire_test.clj`, `test_source_live_path_lint_test.clj` — the four gates D2 (this review) independently confirmed real and green (D2-7, D2-14, D2-15, D2-16). Full cross-dimension reconciliation: D1's raw count and D2's own gate-by-gate verification agree exactly. | None. | close-as-fine |
| D1-6 | `ls notes/adr/*.md \| grep -v README \| wc -l` | 89 files | Matches `notes/adr/README.md`'s own self-disclosed "89... as of ADR-0091" line exactly. This review's own ADR-0092 will make it 90. | None. | close-as-fine |
| D1-7 | `git tag -l 'stable-*'` | 48 live `stable-*` tags | Up from review-1's 29. Every window ADR licensed exactly one successor tag under tag-law case (ii); one ADR's own text (ADR-0081 AR-FR-0) discloses a premature tag created-then-deleted before it reached the record — not a live discrepancy now. Arithmetic reconciles: 29 (review-1 baseline) + 19 new tags across the window's ADRs, riders, and multi-session closes = 48, confirmed by direct list. | None. | close-as-fine |
| D1-8 | Fresh `wc -l` sum of every `:paths` member per reading set in `.agents/reading-sets.edn`, vs each set's stated `:budget-lines` | onboarding 1322/1470 (margin 148, ~10.1%); corpus 1788/2060 (272, ~13.2%); sim 843/970 (127, ~13.1%); judge 914/1055 (141, ~13.4%); docs 727/840 (113, ~13.4%) | All five sets under budget. `onboarding`'s margin (10.1%) is now the TIGHTEST of the five — inverted from review 1, where `judge`/`docs` carried the thinnest margins. `onboarding` was last re-derived at ADR-0089 (conviction close); two more ADRs' worth of roadmap.md Now/Done churn (batch 4, storefront) have landed since without a re-derivation. | No re-derivation owed today (nothing red) — but `onboarding` is this review's own watch item for the next arc close's regeneration, replacing judge/docs as the tightest-margin set. | close-as-fine (watch item, ownership rotates to `onboarding`) |
| D1-9 | Cross-reference: does `make docsgen` (D5-1) change any of the counts re-derived above? | No — `make docsgen` touched zero files this session (see D5). | Confirms D1's re-derivations were against a genuinely current tree. | None. | close-as-fine |

**Dimension 1 verdict: GREEN.** Nine probes, zero drift beyond
fully-reconciled, contractual staleness — including a from-scratch
reconciliation of a 6-row NOTICE gap that state.md's own citation tag
explains precisely, and an exact cross-dimension match between D1's
raw docs-tooling test-file count and D2's own independent gate-by-gate
verification.

---

## Dimension 2 — Guard coverage

Probed by a dedicated sub-agent (mechanism: read every CI workflow in
full, read `.agents/rulings.md`'s new entries since review 1, and read
— not just grep for the filename of — every claimed gate test's own
body to confirm it does what it says).

### Part 1 — CI lane map (AR-RR2-4)

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-1 | Read `.github/workflows/test.yml` in full | **`test`**: triggers on `push` to `main` and every `pull_request`. Steps: `poly check`, `poly test :all skip:integration`, `make docsgen` + `git diff --exit-code` on the generated-doc paths. `projects/integration` excluded by construction (`skip:integration`). | Matches its own header exactly; no coverage/lint gate, disclosed as deliberate. | None. | close-as-fine |
| D2-2 | Read `.github/workflows/integration.yml` in full | **`Integration`**: triggers on `schedule` (07:00 UTC) and `workflow_dispatch` only, never push/PR. Primes the real artifact cache, runs `poly check` + `poly test :all project:integration`. Reports, never blocks a merge — nothing in branch protection or `test.yml` references its status. | Matches its own header exactly; the ONLY lane that ever composes/compiles `projects/integration`'s own test tree. | None. | close-as-fine |
| D2-3 | Cross-reference: which gates run in which lane, and which run in NEITHER | `bin/regression-oracle`, `bin/mirror-nist`, `bin/verify-nist-lock`, `bin/check-palgebra-drift` all still exist, still author-machine-only, still disclosed as such in their own headers. | Unchanged, fully disclosed inventory — no new undisclosed environment-restricted surface. | None for the inventory itself — see D2-4. | close-as-fine |
| D2-4 | **New.** `bin/verify-nist-lock`'s own header claims it is "wired into `make test`... right after `poly test :all skip:integration`," so "not yet resolved" should never fire in that lane. Does the actual push lane, or routine session practice, ever invoke it? | `test.yml` never calls `make test` — it inlines `poly check`/`poly test` as separate steps, never the Makefile's own `test:` target (which DOES include `bin/verify-nist-lock`, confirmed by reading the Makefile). Grepped all 17 ADRs from 0075 through 0091 for `make test`/`verify-nist-lock` in their own Verification sections: **zero hits**. `.agents/skills/build-session/SKILL.md`'s own standing ceremony names no step running it either. | A supply-chain integrity check (NIST-jar sha256 vs. `artifacts.lock.edn`) is currently exercised NOWHERE in routine practice — not CI, not the last 17 sessions' own verification, not the standing skill. Its own header's claimed enforcement surface is false against the live tree, drift this dimension's probe exists to catch. | Add `bin/verify-nist-lock` as an explicit `test.yml` step, or fold it into every session's canonical "Full suite" command. | fix-session-candidate |

### Part 2 — Standing rulings added since review 1 (through ADR-0089), mapped to gates

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-5 | "Multi-seed-once-flagged, standing" [A] | No mechanical test can assert future session practice; batch 4's own disposition table shows it actually followed (seeds `20260802, 1, 42`), verified by direct read. | Correctly gateless by nature. | None. | close-as-fine |
| D2-6 | "The `defspec` seed policy, standing" [A] | The one flaked spec (`sim-engine/engine_test.clj`) carries `{:num-tests 150 :seed -60645}`, confirmed live by direct grep. | The one concrete instance is genuinely, mechanically gated (a literal). | None. | close-as-fine |
| D2-7 | "I/O speaks Result or fails loud, standing" [C] | `io_vocabulary_lint_test.clj` read in full: scans `components/*/src`/`bases/*/src` for bare `.listFiles`/`.list`/`.renameTo`, allowlisted only by declared namespace. | Real, working recurrence gate for the D4-1/D3-4/D8-2/D8-3 cluster review 1 found. | None. | close-as-fine |
| D2-8 | "CI is watched, never waited on, commits land green, standing" [C] | Process discipline about session behavior, not a tree property. | Correctly gateless by nature. | None. | close-as-fine |
| D2-9 | "Semantics changes are predicted before they are made, standing" [C] | `bin/regression-oracle`'s hard behavior: any digest DIFFERS is an unconditional exit 1, no override flag for root-output drift — a session cannot silently continue past a moved root. ADR-0086's own sleep-apnea mover caught exactly this way. | Detection is hard-mechanical; the predict-first/named-license half is session discipline layered on top. | None — the hard-stop half is the load-bearing gate and it is real. | close-as-fine |
| D2-10 | "Plausible-by-adjacency is not a diagnosis, standing" [C] | An epistemic/methodological discipline, not a tree property. | Correctly gateless by nature. | None. | close-as-fine |
| D2-11 | "Witnessed rows only, standing" [A] (conviction arc) | `pairing_conviction_test.clj`'s `every-registry-row-witnesses-its-own-expected-class-test` RE-EXECUTES the mutate->judge loop on every test run for every registry row — not a cached assertion. Its FHIR twin does the same against the real validator subprocess. | Genuinely, strongly gated — one of the best-enforced rulings in this register. | None. | close-as-fine |
| D2-12 | "Licenses bind at their own granularity, standing" [A] (conviction arc) | Same hard-stop instrument as D2-9 underlies this ruling too; granularity-fidelity itself is a session-side comparison. | Same partial-gating shape as D2-9. | None. | close-as-fine |

### Part 3 — Review-1's D2 findings, re-verified against the live tree

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-13 | D2-3 (façade surface-identity gate) status? | `interface_surface_test.clj` read in full: a literal frozen-baseline var/arity map, `ns-publics` reflection re-derived fresh every run, diffed. A witnessed red->green transcript disclosed in ADR-0079. | **FIXED, confirmed real.** | None. | close-as-fine |
| D2-14 | D2-4 (state.md staleness tripwire) status? | `state_staleness_tripwire_test.clj` read in full: extracts the cited ADR number, compares against the highest-numbered arc-close ADR on disk. Has now genuinely governed session ordering TWICE (ADR-0080, ADR-0089), not just landed and forgotten. | **FIXED, confirmed real and actively exercised.** | None. | close-as-fine |
| D2-15 | D2-5 (Deferred in-place-closure lint) status? | `roadmap_deferred_closure_lint_test.clj` read in full: case-sensitive closure-word match requires a same-row disclosure phrase; currently green against the live Deferred section. | **FIXED, confirmed real.** | None. | close-as-fine |
| D2-16 | D2-6 (test-directory live-mutable-path lint) status? | `test_source_live_path_lint_test.clj` read in full: literal-string-argument scan with a narrow allowlist, a disclosed false-positive-narrowing history. | **FIXED, confirmed real.** | None. | close-as-fine |
| D2-17 | D2-7 (generalized multi-surface-law-drift scaffold) — has the named "third law drifts" trigger fired? | Grepped ADR-0081 through 0091 (11 ADRs) for any new multi-surface-law drift instance (the tag-law/state.md-tripwire shape) — none found. | **Trigger has NOT fired.** Genuinely negative, confirmed-clean re-probe. | None — re-probe at review 3. | close-as-fine (watch item, unchanged) |

### Part 4 — New finding: the requires-vs-classpath static-gate gap

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-18 | Does any gate on the push lane (or anywhere routinely watched) catch a test file's `:require` naming a component absent from its containing project's own classpath composition — the `2088763` incident class (H-4, above)? | `git show 2088763` read in full: `judge`'s test tree (composed into EVERY project including `integration`) required `ehrt.judge-v2-nist.interface` directly after `integration`'s own `deps.edn` had dropped that dependency; `poly check` did NOT catch it (ADR-0088's own Verification: "poly check: OK" against the very tree with the gap) — only the next scheduled `Integration` run did, up to a day later. Searched all 26 `docs-tooling` test files for anything checking project-deps-vs-test-tree-requires composition: none. Root cause is structural — this repo's own docstrings explicitly disclose test-context cross-brick requires as "deliberate and precedented," which is exactly what makes Polylith's native src-level dependency graph blind to this hazard class. | **Confirmed live gap.** Two closely related incidents (this one and H-5/`cd08b20`) inside one 24-hour session window, zero mechanical gate added for either — precisely "a check that runs only where nobody looks." | A static gate, docs-tooling-shaped like its siblings: for every project in `workspace.edn`, parse each composed brick's TEST-tree `ns` requires (reader-based, matching `sim_emit_hl7_dependency_test.clj`'s own extraction method), resolve back to the owning brick, assert it's in the composing project's own `deps.edn`. | fix-session-candidate |

**Dimension 2 register summary:** 18 rows. **16 close-as-fine, 2
fix-session-candidate, 0 ruling-needed, 0 intake** (independently
recounted from the rows above — the sub-agent's own narrative summary
said "15 close-as-fine," off by one; corrected here by direct count,
the same discipline AR-RR2-2 applies to review 1's own summary line).

**Dimension 2 verdict: YELLOW.** Both findings are real and actionable
in a small session. **D2-18** is the more consequential: a live,
currently-open static-gate gap that let the same root-cause class fire
twice in one day with zero mechanical recurrence guard added either
time. **D2-4** is smaller in mechanism but notable in kind: a
supply-chain integrity check silently orphaned from every routine
execution surface by the accumulated drift of CI and session practice,
not by any deliberate ruling. Against that, every one of review 1's
four executed fix-session gates is confirmed genuinely real by direct
read (not by trusting the closing ADRs' own narration), the D2-7
deferred trigger is confirmed still unfired by a fresh targeted search,
and both new conviction-arc rulings turn out to be among the
best-gated rules in the whole register.

---

## Dimension 3 — Environment independence

Probed directly by the landing session (mechanism: a genuinely cold
fresh clone, `git check-attr` re-verification, fresh greps against the
window's own new files) plus the history scan's own re-derivation for
the repeat-incident row that drives this dimension's color.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D3-1 | Cold-cache fresh-clone full-suite probe (AR-RR2-4 headline): fresh `git clone` into an isolated temp dir, `HOME` repointed to a temp dir with no `~/.m2`/`~/.gitlibs`/`~/.deps.clj` (confirmed absent before the run), `EHR_TESTING_TOOLS_CACHE` repointed to a fresh temp path; `clojure -M:poly check` then `clojure -M:poly test :all skip:integration` | `poly check`: OK. Full suite: 293 "Test results:" namespace blocks, 14183 total passes, **0 failures, 0 errors, anywhere** — confirmed by grepping the entire ~2200-line log for any nonzero failure/error count (none found). Test-phase wall-clock: 5m24s from a genuinely cold Maven/gitlibs cache (dependency resolution included). This is the `c690ec3` hermeticity method, applied at full-repository scope, now this review's own standard per AR-RR2-4. | Every test in the repo is green under a genuinely cold cache, not merely a cache-hit-fast local run — fresh evidence, not a re-read of `c690ec3`'s own claim. | None. | close-as-fine |
| D3-2 | SOAK status of the loopback flake (origin ADR-0084) and the older `merge-config-file` flake (origin ADR-0076) — `gh run list` re-enumeration since each fix/origin | `merge-config-file`: unchanged carried watch item, well past its own 5-7-push soak target, zero recurrence. Loopback flake (H-10, above): fired exactly once, under heavy concurrent JVM load, reproduced-as-non-reproducible via 3 clean re-runs same session, re-checked clean at every subsequent arc close plus this review's own D3-1 — zero recurrence across ~45 `test`-lane pushes since. ADR-0089's Intake explicitly declines to call either SOAK "closed" against a stated bar. | Both flakes show zero recurrence across a now-larger sample; evidence is stronger than review 1's, but the repo's own record still correctly declines to declare either closed. | Carry forward as the next review's own watch item — re-run once the sample roughly doubles again. | intake (next-review watch item, unchanged disposition, strengthening evidence) |
| D3-3 | `.gitattributes` byte-determinism: re-verify the 4 files review-1's fix extended `-text` to, plus sweep the window's own new byte-precious-candidate tree | All 4 previously-flagged files confirmed `text: unset` (`-text` in effect) — the review-1 fix holds. The new `components/corpus/test-fixtures/fhir/storefront-patient.json` (ADR-0091) returns `text: auto` (unprotected) — but ADR-0091 itself discloses this fixture is "authored this session, not vendored bytes — no upstream hash," confirmed by grep: no NOTICE/PROVENANCE/hash reference anywhere in that directory. | The one candidate new byte-precious file this window is NOT actually byte-precious by this repo's own definition — correctly unprotected. | None. | close-as-fine |
| D3-4 | Ignored-boolean I/O (`.mkdirs()`/`.delete()`/`.listFiles()`, unchecked) in NEW test files added this window | Every `_test.clj` file added since `stable-20260807-quality-close` (13 files) grepped — zero hits. The one production-code instance review 1 found (`kernel/artifact.clj:123`'s `.renameTo`) is now routed through `ehrt.kernel.io/rename!` (cross-checked against D4's own fresh sweep). | Clean; no new instance of the class this window. | None. | close-as-fine |
| D3-5 | Untracked/author-local/network dependencies — any new instance | No new shell-outs, no new live network call, no new hardcoded author-local path found anywhere this window (cross-referenced against the history scan's own per-ADR sweep). | Clean. | None. | close-as-fine |

**Dimension 3 verdict: YELLOW — holding, on stronger evidence, but NOT
green.** The headline cold-cache probe (D3-1) is unambiguously clean,
and the `-text`/ignored-boolean sub-probes are clean or correctly
scoped. What keeps this dimension from GREEN is the history scan's own
H-5 finding: the SECOND live instance of the exact "local state is not
clone state" incident class fired this window (`cd08b20`, a
warm-artifact-cache false green that passed locally and failed in CI —
the first instance being ADR-0004's pre-window origin). A repeat of a
named incident class raises this dimension's severity per the rubric's
own instruction, even though the repeat was caught, disclosed, and
fixed forward (`c690ec3`) within the same session it occurred.

---

## Dimension 4 — Error honesty (headline re-score)

Probed by a dedicated sub-agent with live-execution latitude for
verification only (function-level calls against scratch input, no
tracked file touched, confirmed by `git status --porcelain` clean
throughout). This is the review's single most important dimension:
review 1 scored it RED on a demonstrated, repo-wide silent-success
defect; a fix session (`notes/adr/0078-result-or-loud.md`) claimed to
close it. This probe re-verifies from fresh evidence, not by trusting
the fix's existence.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D4-1 | Direct read + call-graph trace of `mutate-command`/`files-with-extension-in` (`bases/cli/src/ehrt/cli/core.clj`) | `files-with-extension-in` resolves through `ehrt.kernel.interface/list-files` -> `ehrt.kernel.io/list-files`, the guarded retry-once-then-`result/error :listing-failed` helper. A listing failure now short-circuits the whole batch immediately rather than returning `{:status :ok, :count 0}`. | The single highest-severity row from review 1 no longer exists in the live tree, confirmed by direct code reading and call-graph tracing. | None. | close-as-fine |
| D4-1a | Read AND directly RUN the recurrence gate (`io_vocabulary_lint_test.clj`) | `clojure -M:dev:test` run fresh against HEAD: **3 tests, 95 assertions, 0 failures, 0 errors.** Allowlist self-test confirms it is still EXACTLY `#{"ehrt.kernel.io" "ehrt.sim.run"}` — not silently widened. | The gate is real, scans the right trees, and is currently green with an unexpanded allowlist. | None. | close-as-fine |
| D4-1b | Fresh, independent grep sweep for `.listFiles`/`.list(`/`.renameTo`, read every hit in context | Exactly the two allowlisted sites (`kernel/io.clj` itself; `sim/run.clj:223`'s disclosed, low-stakes `similar-sibling-config`), zero new violations anywhere. | Independently reproduces the gate's own finding exactly. | None. | close-as-fine |
| D4-1c | Fresh sweep of every commit since ADR-0079's closing tip through HEAD (colorectal payoff, straddle fix, pairing registry, storefront — 34 changed files under `components`/`bases`) | No bare `.listFiles`/`.list`/`.renameTo` introduced anywhere in the new code; confirmed by both D4-1a's live gate run (necessarily covers this code) and an independent diff-scoped re-grep. | The recurrence gate holds against everything that actually landed post-fix — the real regression test this review needed. | None. | close-as-fine |
| D4-2 | Every `catch` block in `components/*/src`/`bases/*/src` (34 total, full population, not a sample) read in context | Every site is a categorized `result/error`/`result/rejected`, a documented best-effort degrade with an explicit docstring, or a legitimate narrow rethrow. Two engine-adapter sites catch broadly but capture the exception's class+message into a named field. | No swallow-and-silently-default pattern found across the FULL population. | None. | close-as-fine |
| D4-3 | Fresh grep `(take N` near I/O/reporting code, repo-wide | Zero hits anywhere. | Confirms review 1's clean result stands unchanged. | None. | close-as-fine |
| D4-4 | Status check: `gmf.clj`'s weight-column `Double/parseDouble`, previously disclosed cosmetic gap | Confirmed still unguarded at `gmf.clj:1556`, sibling guards two lines up still guard. Unchanged since review 1. | No change; correctly deferred, low-priority. | Optional, unchanged. | close-as-fine (status quo) |
| D4-5 **(new)** | `read-base-data`'s `:fhir` branch (`bases/cli/core.clj:391`, `(json/read-str (slurp file))`), live-executed against a scratch malformed `.json` file via `mutate-command` | **Raw `java.io.EOFException` raised uncaught** — no `try`/`catch` anywhere in the call chain up through `-main`. Sibling function `corpus/display.clj`'s `render-fhir-json` DOES guard the same content shape with a categorized rejection. | A malformed input file mid-batch crashes the whole batch with a raw stack trace instead of a categorized rejection — loud, not silent (not D4-1-class), but real, live, and outside ADR-0078's own named scope. | Wrap the `:fhir` branch the same way `display.clj` already does. | fix-session-candidate |
| D4-6 **(new)** | `gate-command`'s `--baseline` path (`core.clj:911`, `(edn/read-string (slurp baseline))`), live-executed against a malformed EDN file | **Raw `RuntimeException: EOF while reading`, uncaught.** Sibling `kernel/artifact.clj/read-lockfile` guards an almost-identical read with a categorized rejection. | A corrupt/truncated `--baseline` file (a plausible real mistake) produces a raw stack trace instead of a clean rejection. | Same guard shape as `read-lockfile`. | fix-session-candidate |
| D4-7 **(new)** | `check-command`'s `--assertions` path (`core.clj:1552`, same `edn/read-string (slurp ...)` idiom), live-executed | Identical shape to D4-6 — raw, uncaught `RuntimeException`. | Same operator-facing gap as D4-6, different flag. | Same fix, can land in the same small session as D4-6. | fix-session-candidate |
| D4-8 | Scope check: `.delete(`/`.mkdirs(` outside the recurrence gate's own named scope | `.delete`: both sites inside `fetch`'s own try/catch, invoked after the real failure is already returned. `.mkdirs`: 12 bare sites, every one immediately followed by a write into that directory — a real mkdir failure surfaces loudly on the very next line, not absorbed as success. | Not a D4-class violation (loud, not silent) — this is D3's own ignored-boolean-return probe territory, already covered there. | None for D4. | close-as-fine |
| D4-9 | Scope check: `edn/read-string`/`json/read-str` reading repo-internal committed files (not operator input) | All unguarded, all read fixed, build-time-trusted content — a malformed one is a packaging/repo-integrity defect, and a loud crash at dev/build time is the correct response. | Not the class of defect D4 hunts. | None. | close-as-fine |

**Dimension 4 register summary:** 12 rows. **9 close-as-fine, 3
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 4 verdict: GREEN (up from RED).** The headline RED has
resolved to GREEN on fresh evidence gathered independently of the fix
session's own prose: the demonstrated worst case is closed (confirmed
by direct call-graph trace, not trusted prose); the recurrence gate is
real, was RUN directly (not merely read), and holds against every
commit landed since; an independent grep sweep reproduces the gate's
own finding exactly; the allowlist has not silently grown. Three new,
lower-severity findings (D4-5/6/7) surfaced only because this review's
probe went beyond ADR-0078's own named scope (`.listFiles`/`.list`/
`.renameTo` only) into adjacent operator-facing parse sites — loud
crashes, not silent successes, so they do not reopen the D4-1 verdict,
but they are real and worth a small follow-up session.

---

## Dimension 5 — Mirror and derivation drift

Probed directly by the landing session (mechanism: `make docsgen`
regenerates every derived doc from its live source; the resulting
`git diff`/`git status` is the verification).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D5-1 | `make docsgen` then `git status --porcelain` / `git diff --stat` | Every target regenerated; `git status --porcelain` empty before and after — zero byte of diff anywhere. | Every derived doc is byte-current with its live source, confirmed by regenerating, not trusted from the last session. | None. | close-as-fine |
| D5-2 | Skill-mirror currency: byte-diff `.agents/skills/*` vs `.claude/skills/*`, all 17 directories | `diff -rq` exit 0, zero differences; 17 dirs each side. The `repo-review` skill itself (this session's own driving instrument) is byte-identical between homes. | No drift between mirrored skill homes, including the skill actively driving this review. | None. | close-as-fine |
| D5-3 | Local-vs-CI generator currency: unchanged from review 1? | `cli.md`/`operators.md` still gated locally; `docs/dev/pipeline.md`/`docs/use-cases.md` still correctly CI-only. No new generated-doc surface this window needing reclassification. | No change from review 1's clean finding. | None. | close-as-fine |

**Dimension 5 verdict: GREEN.** Every derived-doc and mirrored-pair
probe clean, independently re-derived, including the skill directory
actively driving this review's own procedure.

---

## Dimension 6 — Sampling adequacy

Probed by a dedicated sub-agent (mechanism: direct read of
`census.clj`'s live counting logic; re-derived binomial arithmetic;
every historical finding's CURRENT status cross-checked against the
live tree).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D6-1 | Live re-read of `census.clj`'s `:closure-file-count` computation — still JSON-modules-only? | Confirmed unfixed: line 408 (`(count @fetched)`) and line 424 (`(count modules)`) both never touch the CSV-lookup-table resolver. `git log` since review 1 shows one touch to `census.clj` (the EncounterEnd fix, unrelated). Named again, unfixed, in the horizon-note lists of SIX separate window ADRs (0081, 0083, 0087, 0088, 0089, 0091). | Review 1's explicit "escalate priority" ask was not honored — no ruling, no scheduled session — while the item accumulated six more unaddressed citations. No fresh repeat-cost incident landed this window (no lookup-table-bearing module got vendored), so the bug did not bite again, but it is aging worse in disposition terms than in incident terms. | Schedule the fix now or explicitly re-defer with a stated trigger — a repeat "escalate priority" ask a second review running is not itself a disposition. | intake (aged; recommend a ruling this time, not another escalate-ask) |
| D6-2 | Fresh incident check: did the window produce its own new sampling-adequacy miss? | Yes — ADR-0087 (colorectal payoff): a first, 900-walk synthetic sweep for `:suppressed-straddle-spans` measured ZERO hits despite a real, disclosed 2-of-900 branch, because the synthetic sweep drew from an independent RNG path, not the actual `engine/run`-seeded population. Disclosed, not discarded; a second, population-matched method (interception at the real `engine/run` population) succeeded and is now the committed, pinned regression. | Same general lesson D6's own canonical miss teaches (a sampling technique validated for one counter doesn't transfer to a structurally different one without re-verification) — recurring in a new guise, caught and closed WITHIN the same session, a positive demonstration the discipline is actively practiced. | The general lesson is disclosed in prose (ADR-0087/0089) but not yet codified as a standing ruling, the way D6-4 (below) was. | ruling-needed (small — codify "match the measurement's RNG path to the claimed population" as a standing convention) |
| D6-3 | Re-derive `(1-p)^300` at p=1%/3.3%/5% for the standing 300-patient round-trip convention; check whether the window's own vendoring/fixture sessions followed it | Re-derived: p=1% -> 4.9041%, p=3.3% -> 0.0042%, p=5% -> ~0%; matches review 1's figures exactly, no drift. Batch 4 (ADR-0090) explicitly cites "300 patients, 2-3 well-mixed seeds" and visibly exercises the multi-seed-once-flagged practice (D6-4's own review-1 ask, now ratified as standing ruling AR-RL-5(5) and demonstrably followed). Storefront (ADR-0091) correctly does not use this convention — its FHIR operators are deterministic single-bundle measurements, not a statistical population sample. | The convention is adequately powered as before, now written down AND actively exercised — review-1's D6-4 fully closed, not just re-confirmed. | None — this is exactly the outcome review 1 asked for. | close-as-fine (D6-4 from review 1 fulfilled) |
| D6-4 | The three skipped NIST pairing cells (H-9, above) — sampling-power gap, or something else? | Two cells are structurally un-witnessable (the NIST parser throws before any finding-level class exists — an instrumentation ceiling, not an undersampling); one is masked by ONE fixed fixture's own noise floor, not population variance. | Not a D6 sampling-power gap on inspection — closer to a D2 guard-coverage matter (no finding-level class exists for these contracts at this judge tier). | Cross-reference to D2 for disposition; recorded here for completeness. | not a D6 finding — cross-reference D2 (not counted in D6's own disposition tally) |
| D6-5 | `defspec` count at both tag boundaries (`stable-20260807-quality-close` vs HEAD); did "the engine spec" (AR-RL-5(3)) actually get pinned? | 71 forms at both boundaries — zero added or removed. The one seed-pinned spec (`sim-engine/engine_test.clj:517-518`, `{:num-tests 150 :seed -60645}`) was already present BEFORE this window opened (pinned in the quality-review arc), not during it. | The "middle path" ruling continues to hold with zero drift through the entire window. | None. | close-as-fine |
| D6-6 | Any new census-seed-count/sampling-population claim in the window's session records deserving its own power check? | All 12 session records for the window grepped (92 hits on seed/walk/population/sweep terms) — every hit traces back to a claim already covered above. | No additional distinct sampling-population claim found. | None. | close-as-fine |

**Dimension 6 register summary:** 6 rows (D6-4 excluded from the
disposition tally per its own "not a D6 finding" verdict). **3
close-as-fine, 0 fix-session-candidate, 1 ruling-needed, 1 intake**,
plus D6-4 (cross-referenced to D2, not double-counted).

**Dimension 6 verdict: YELLOW (unchanged from review 1).** D6-1's
status is not worsened by any fresh incident but is aged further by
inaction — a known, disclosed, repeat-cost bug that received an
explicit "escalate" ask last review and instead accumulated six more
unaddressed citations without a ruling either way. Everything else in
the dimension improved or held clean: D6-4 from review 1 is fully
closed and demonstrably exercised in practice; the round-trip
arithmetic re-derives identically; the `defspec` population is
completely unchanged with zero seed-policy drift; the window even
produced a genuinely new, self-caught instance of the dimension's core
lesson (D6-2), disclosed in full and closed with a population-matched
pinned regression — positive evidence the discipline catches its own
misses.

---

## Dimension 7 — Continuity integrity

Probed by a dedicated sub-agent (mechanism: a widened citation sweep,
a live re-run of the full continuity-gate family, and a from-scratch
aging table built by direct grep of every close/session ADR's own
horizon-note section — not trusted from review 1's own table).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D7-1 | Citation-resolution sweep: 40+ samples (24 ADR citations, 16 commit SHAs, several file paths) across ADR-0085 through 0091 | All 24 ADR references resolve to real files; all 16 SHAs resolve via `git log -1 --format=%s` to commits whose subject matches the citing ADR's description exactly; all sampled file paths exist. Zero broken citations. | Clean across the entire window, a real improvement in sample size and confidence over review-1's own 18-sample sweep. | None. | close-as-fine |
| D7-2 | Positive control: was review-1's own D7-1 finding (invented `AR-F1-6a`/`AR-F1-6b` sub-letters) actually fixed? | `.agents/rulings.md` now reads the corrected `A-3`/`D-3` labels, and the fix entry self-documents its own provenance back to review-1's D7-1. | Confirms the review-to-fix pipeline works end-to-end, with the fix citing the finding that caused it. | None. | close-as-fine |
| D7-3 | The ADR-0089 prompt-citation mismatch (H-7, above) — locate the exact citation, describe the mismatch precisely | The driving prompt cited "AR-A-5 over-literal prompt wording" as an already-disclosed error; that tag is used elsewhere ONLY for the standing scaffolding-compaction-A relocation law. The executing session caught it itself and disclosed the mismatch by name in ADR-0089's own Intake rather than inventing a matching tag, substituting the closest real candidate (AR-CP-2). | A genuine "prompt cited a transcript-only event" — but caught and disclosed same-session, not silently absorbed; the disclosure law firing correctly is the actual finding here. | Prompt authors should `grep` a citation tag before writing it into a driving prompt. No repo edit — archived prompts are frozen provenance. | close-as-fine (caught same-session; worth a preventive habit note) |
| D7-4 | Directly re-run the pairing/index/done-pointer/skill-mirror gate family | Fresh full-suite run: `index-completeness-test` 43 assertions green (exact baseline match), `done-pointer-adr-test` 4 (exact match), `skill-mirror-currency-test` 224 (exact match), `prompt-record-pairing-test` 12 (exact match). Bonus fifth gate new since review 1: `roadmap-deferred-closure-lint-test`, 7 assertions green — directly executes review-1's own D2-5 recommendation. | The continuity-gate family holds at the exact review-1 baseline, independently re-derived, plus an upgrade from manual-only to gated for attic-vs-live consistency. | None. | close-as-fine |
| D7-5 | Attic-vs-live consistency re-check | The one in-place "CLOSED — see Done, below" Deferred row is the compliant shape the live lint test requires (a disclosure phrase in the same row); Externals' "RESOLVED" bullet remains correctly outside any relocation contract. | Clean, and now gate-enforced rather than only hand-verified. | None. | close-as-fine |
| D7-6 | Aging: pairing-as-data registry (review-1's D7-5) — did it land? | Yes — ADR-0088 executed the design pass, ADR-0091 extended it to a live gate. But it was restated as still-unbuilt at TWO MORE closes past review-1's own count of 4 (ADR-0080, ADR-0084) before landing — true age at resolution was 6 closes, not 4. | Genuinely resolved, a real win — but AR-RL-5(4)'s own "the register stops counting" instruction was not honored cleanly in the interim. | Process nuance: when a ruling says "the register stops counting," the next close's horizon note should say so explicitly, not restate the old "still paused" language. | close-as-fine (resolved; process nuance noted) |
| D7-7 | Aging: wellness-encounters (review-1's D7-6) | ADR-0080 DID execute the repair, explicitly citing D7-6 by ID. ADR-0084 restated it once more, aware of the recurrence risk — then it dropped OUT of the horizon-note chain for THREE consecutive ADRs (0089, 0090, 0091). Survives only in `state.md`'s own Live-work section; `roadmap.md` has never carried a row for it at all. | A genuine partial recurrence of the exact near-miss review 1 named and got fixed once — the fix held for exactly one restatement before drifting again. Root mechanism: no structural roadmap anchor. | Land a one-line Deferred or Next row in `roadmap.md` — converts "does the next horizon note happen to mention it" into a mechanically-gated check. | ruling-needed |
| D7-8 | Aging: the `notice_verbatim_test` coverage gap | First named ADR-0079, restated ADR-0080/0084, then absent from 0089/0090/0091 (three consecutive) — same pattern as D7-7, held only in `state.md`, no roadmap row. | A second independent instance of the same failure mode as D7-7 — confirms this is a repeating pattern (horizon-note-only items with no roadmap anchor drift), not an isolated slip. | Same fix as D7-7 — land a Deferred row, or fold both into one small "docs-tooling gate coverage gaps" row. | ruling-needed |
| D7-9 | Aging: census `:closure-file-count` undercount (D6-1, cross-referenced) | Restated at 0074, 0080, 0081, 0084 — missing from 0089 (one gap) — recovered at 0091. UNLIKE D7-7/D7-8, this item has a durable roadmap anchor (the "Census tool refinements" Deferred row). | A self-healing near-miss (recovered in one session, not three) — the contrast with D7-7/D7-8 is itself evidence for their own recommendation: anchored rows recover fast, unanchored rows don't recover within a window at all. | None beyond D6-1's own still-standing recommendation. | intake (counted jointly with D6-1; not double-tallied) |
| D7-10 | Aging: the engine `defspec` seed pin (positive control) | Re-derived fresh at every close since landing (0080, 0084, 0089) — byte-identical value every time, independently re-run each time, not copied forward. | A standing, routinely-reverified fact working exactly as designed, three closes running. | None — cite as a positive control. | close-as-fine |
| D7-11 | Aging: publish-prep Externals (NIST licensing, IG pinning, Clojars, SETUP rewalk, GitHub toggle) | Restated identically at 0080, 0084, 0089 — three more full closes since review 1's own count of 4. | Age now 7 closes, identical block, zero movement — correct parking (author-action-only), not stalled debt. | None — aging is expected and healthy for this class of row. | close-as-fine |
| D7-12 | Aging: vendoring batch 4 | Restated across 0075/0076/0080/0081/0084/0089 before ADR-0090 executed it: 5/9 vendored, 2 deferred whole under true, newly-diagnosed names, 2 deferred zero-substance, one honest `unknown` non-attribution rather than a guess. | A second review-1 item fully resolved, with the same disclosure discipline the repo's other closes show. | None. | close-as-fine |
| D7-13 | Aging: Vital-sign/Wave-E cluster (CHF, contraceptives, covid19) | Restated at 0074/0080/0084/0089 plus non-close sessions — underlying status unchanged the entire window (CHF/contraceptives `:produces-content`, covid19 alone still `:zero-on-every-seed`). | Age climbs from review-1's 1 to 4 closes with zero movement on the genuinely-blocked third — honestly and consistently tracked (no vanishing risk), but a real, mounting, unscheduled design debt. | Schedule Wave E, or park it with a named trigger the way Externals is parked, so future reviews stop re-flagging open-ended debt at increasing age. | ruling-needed |
| D7-14 | Aging: the author's two backlog rows (fixture relocation; ADR-references-in-user-docs) | Both first named ADR-0081. Fixture relocation: actively maintained and GROWING (ADR-0091 added a third fixture tree to the same row). ADR-footnote fork: restated 0084/0089, then absent from 0090/0091 — the same horizon-note-drop pattern as D7-7/D7-8, though it DOES have a live roadmap Next row anchoring it (lower risk). | Two siblings from the same ruling have diverged — one healthy, one showing early signs of the same drift pattern, worth naming before it becomes a third D7-7-class miss. | The next session touching the Next section should explicitly re-cite the ADR-footnote-fork row, breaking the two-session drop streak before it becomes three. | intake |

**Fresh aging table** (built by direct grep of each close/session
ADR's own horizon-note section, not trusted from review 1's table):

| item | first named | age (closes, this window) | status |
|---|---|---|---|
| Pairing-as-data registry | ADR-0050 (2026-08-05) | 6 closes carried, then RESOLVED (ADR-0088/0091) | closed |
| Vital-sign/Wave-E cluster | ADR-0036 (2026-08-03) | 4 (up from review-1's 1) | open, unscheduled, ruling-needed |
| Wellness-encounters | ADR-0070 (2026-08-07) | 3 restatements then a 3-session gap; no roadmap anchor | open, at risk, ruling-needed |
| Vendoring batch 4 | ADR-0073 (2026-08-07) | ~6 sessions carried, then RESOLVED (ADR-0090) | closed |
| Publish-prep Externals | ADR-0008/0049 | 7 (up from review-1's 4) | open, correctly parked |
| Census closure-count undercount | ADR-0071 (2026-08-07) | 4, self-healed after one gap | open, has roadmap anchor, aging debt |
| `notice_verbatim_test` coverage gap | ADR-0079 (2026-08-07) | 2 restatements then a 3-session gap; no roadmap anchor | open, at risk, ruling-needed |
| Engine `defspec` seed pin | ADR-0079 (2026-08-07) | 3, standing fact | closed/working-as-designed |
| Author backlog: fixture relocation | ADR-0081 (2026-08-08) | 3, growing, healthy | open, well-anchored |
| Author backlog: ADR-footnote fork | ADR-0081 (2026-08-08) | 2 restatements then a 2-session gap | open, early drift signal |

**Dimension 7 register summary:** 14 rows. **9 close-as-fine, 0
fix-session-candidate, 3 ruling-needed, 2 intake** (D7-9 counted
jointly with D6-1, not double-tallied against the grand total below).

**Dimension 7 verdict: YELLOW (down from GREEN).** The citation
mechanism is confirmed clean at 2x the prior sample size; the
continuity-gate family holds at exact baseline plus a genuinely new
fifth gate; two of review-1's five aged items fully resolved with real
disclosure discipline; this review's own headline claim (the ADR-0089
prompt-citation mismatch) checked out as a POSITIVE data point — caught
and disclosed same-session. What holds it at yellow: review 1 named
this exact failure mode once (wellness-encounters) and it got
explicitly repaired — this review found the SAME shape of near-miss
recurring in three separate items, all sharing one root mechanism
(prose-only horizon-note items with no roadmap-row anchor survive only
on session diligence, not a structural gate). The one comparable item
that DOES have an anchor (the census undercount) self-healed after a
single missed close; the two without one did not recover within this
window — a legible, mechanical, evidenced pattern, not vague caution.

---

## Dimension 8 — Operator experience

Probed by a dedicated sub-agent with live-execution latitude (scratch
output confined to fresh temp dirs, zero tracked files touched,
confirmed by `git status --porcelain` clean before, during, and after
— including after a background `make quickstart` run was deliberately
killed mid-flight rather than left to block this review indefinitely).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D8-1 | Build/run `bin/ehrt`; run every live command fence across README, AUTHORS-GUIDE, `docs/**` (all 20 use-case pages), `components/*/docs/**`, `demos/**/README.md` | 16/16 runnable use-case pages passed verbatim, several reproducing the doc's own exact numeric output; 4 correctly fence-free (conceptual only). All 7 demo-trace README fences and both busy-tuesday fences passed, reproducing exact closing-summary numbers. `make integration` ran clean (~3m54s, all suites green including the new `pairing-conviction-fhir-test`). `make mirror-nist` ran clean. SETUP.md's verification ladder matched exactly. | The overwhelming majority of the operator-facing surface still works exactly as taught — matching review 1's own strong positive signal. | None for the passing groups. | close-as-fine |
| D8-2 | Re-verify README's "What you get" fence, run exactly as currently written (post AR-RL-3/AR-SD-4) | `gate fhir` on the accepted fixture then a real mutate->reject flip — both produce the exact pasted transcript, exit 1 as documented. | AR-RL-3 and AR-SD-4 both landed as intended for this exact case. | None. | close-as-fine |
| D8-3 | Deepened re-verify: `corpus mutate`/`gate fhir`/`show` against a path that EXISTS but is UNREADABLE (`chmod 000`), vs. a MISSING path | Missing path: clean everywhere (categorized `:file-not-found`, exit 2). **Permission-denied (exists, unreadable): all three commands still throw a raw, unhandled `FileNotFoundException` stack trace**, exit 1 — the exact bug class AR-RL-3 was supposed to close, just a wider trigger than the fence happened to test. Root cause: the fix added an `.exists()` pre-check only, not a `try`/`catch` around the actual read. `sim run --config` on the SAME unreadable file DOES return a clean categorized error, via a real `try`/`catch` (ADR-0060, predates AR-RL-3). | A live, reproducible INCOMPLETE fix: AR-RL-3 closed the literal case the fence hit but left a more realistic real-world trigger (permission-denied) open in three commands, with the correct pattern already sitting one file away. | Wrap `corpus mutate`/`gate`/`show`'s file-open paths in the same try/catch-around-the-read pattern `sim run`'s config loader already uses. | fix-session-candidate |
| D8-4 | CLI error matrix re-run fresh: bare invocation, unknown command/flag (subcommand and bare level), missing file, below-minimum `--width` | Subcommand-level matches docs/cli.md's exit-code table exactly, including a new exit-3 spot-check. **New gap**: a typo'd flag at the BARE or `help` invocation level is silently swallowed (prints help, exits 0) rather than reported — inconsistent with the subcommand-level `:unknown-flag` behavior. `corpus --nonexistent-flag` errors correctly but under `:unknown-command` rather than `:unknown-flag` (a category-naming nuance). | Low-severity, narrow corner: a stranger's typo'd global flag could be silently accepted rather than flagged, masking a real mistake. | Either document bare/help-level flag tolerance as intentional, or route it through the same `:unknown-flag` path subcommands use. | fix-session-candidate (low severity) |
| D8-5 | Help output at COLUMNS 40/80/120 plus explicit `--width` | Max observed line width exactly matches each setting; 40-column output has no mid-word breaks, no truncated critical text. | Help degrades sanely at all three widths — the injectable-seam finding from review 1 still holds. | None. | close-as-fine |
| D8-6 | README's two-commands-to-demo front door (ADR-0073), run for real | Mechanically exact: exit 0, byte-reproducible closing-summary numbers matching ADR-0073's own witnessed figures almost exactly (only wallclock timing differs). But the ACTUAL on-screen content jumps a decade via idle-skips, shows `inpatients: 0` for nearly the entire run, only ONE inpatient ever admitted — nothing resembling a "busy" hospital appears. The sibling `demos/scenarios/busy-tuesday/README.md` already discloses this honestly in its own prose; the top-level README's "See it run" framing ("a busy Tuesday... watched on a live bed board") carries none of that disclosure. | The front door works exactly as documented at the mechanical level (review-1's own D8-5 verdict is correct on that narrow question) — but a stranger following the literal two commands gets an experience substantially at odds with the "busy Tuesday" framing, a gap the repo's own sibling doc already knows how to disclose honestly. Present since the fence was authored (ADR-0073), not a new regression. | Either carry the same honesty disclosure up into README's "See it run" section, or pick demo parameters that show a visibly busier board within the front door's own wallclock budget. | ruling-needed |
| D8-7 | Fresh dangling internal link, found while re-reading the "What you get" section | `README.md:140` links `docs/adr/0091-storefront-fixture.md` — that directory does not exist; the real path is `notes/adr/0091-storefront-fixture.md`. Introduced by this window's own storefront work (2026-08-09), not a pre-existing defect. | A stranger reading the exact section D8-2/D8-3 already scrutinize and clicking this link gets a 404 instead of the fixture's design writeup. | One-line path fix in README.md. | fix-session-candidate (trivial) |
| D8-8 | `make quickstart` (README's own full-sequence check), run live | Ran 586s (~9.8 min) with steady, error-free progress (0 failures/0 errors throughout ~2700 captured lines) before being deliberately killed rather than run to the prior review's own ~13-minute completion baseline, to keep this review's own probe from running unbounded. `git status --porcelain` confirmed clean before, during (after the kill), and after. | Not evidence of an actual hang or regression — behavior up to the kill point was consistent with the documented runtime. Recorded per the rubric's own "negative results are recorded, not dropped" instruction, and because this review's own probe could not complete it within its patience window. | Re-run `make quickstart` in isolation with a >15-minute budget for a clean completion reading next review. | intake |
| D8-9 | Minor: a use-case fence referencing a directory (`in/v2-corpus`) not created by any step on that same page | The page's own "You bring" framing already sets the right expectation; the fence ran cleanly once the prerequisite directory was populated from a repo fixture. | Very low severity — a first-time reader following the prose would not be surprised. | Optional: add one line naming a concrete source command for a fully literal copy-paste path. | close-as-fine |

**Dimension 8 register summary:** 9 rows. **4 close-as-fine, 3
fix-session-candidate, 1 ruling-needed, 1 intake.**

**Dimension 8 verdict: YELLOW (down from GREEN).** The stranger-facing
surface remains overwhelmingly sound at the mechanical level — every
genuinely runnable fence, all 16 executable use-case pages, all demo
pages, SETUP.md's ladder, `make integration`, `make mirror-nist`,
byte-reproducibility, the CLI's exit-code contract, and help-text
wrapping at three widths all reproduced their documented behavior
exactly. The downgrade is driven by one specific pattern, not volume:
last review's own D8-2/D8-3 fix turns out to be INCOMPLETE — it closed
the literal case a doc fence happened to hit, but left a more realistic
same-class trigger (permission-denied, not just missing) open in three
commands, with the correct pattern already sitting one file away.
Paired with a fresh broken link in the exact section under repeat
scrutiny, a small CLI-matrix inconsistency, and a real (if
pre-existing, non-regressive) gap between the front door's marketing
framing and its literal on-screen content, this round earns a caution
flag rather than a clean pass.

---

## Scoreboard — review 1 vs. review 2

| dimension | review 1 (2026-08-07) | review 2 (2026-08-09) | movement |
|---|---|---|---|
| D1 — Claim-reality coherence | GREEN | **GREEN** | unchanged |
| D2 — Guard coverage | YELLOW | **YELLOW** | unchanged (different findings: review-1's 4 gates confirmed fixed; 1 new supply-chain-gate orphan, 1 new static-gate gap) |
| D3 — Environment independence | YELLOW | **YELLOW** | unchanged (stronger cold-cache evidence; held yellow by a repeat warm-cache incident) |
| D4 — Error honesty | RED | **GREEN** | **improved** — the headline defect independently re-verified closed |
| D5 — Mirror and derivation drift | GREEN | **GREEN** | unchanged |
| D6 — Sampling adequacy | YELLOW | **YELLOW** | unchanged (one review-1 ruling-needed item fully closed; the aged census bug held flat) |
| D7 — Continuity integrity | GREEN | **YELLOW** | **regressed** — a near-miss pattern review 1 fixed once recurred in two more items |
| D8 — Operator experience | GREEN | **YELLOW** | **regressed** — a review-1 fix confirmed incomplete under a wider trigger, plus small new findings |

**Overall: review 1 was 4 green / 3 yellow / 1 red. Review 2 is 3
green / 5 yellow / 0 red.** Zero STOP-AND-REPORT-worthy findings in
either run. The net movement is genuinely mixed, not a simple
improvement or decline: the single most severe finding in this repo's
review history (D4's repo-wide silent-success I/O pattern) is closed,
independently re-verified rather than trusted from the fix session's
own account — but two dimensions that were clean last time (D7, D8)
each surface one real, evidenced regression pattern this time, both
traceable to the same root shape: a fix or a gate that closed the
LITERAL case a prior probe happened to hit, while leaving a
structurally-identical, more general trigger open (D8-3's
permission-denied vs. missing-path; D7-7/D7-8's "restated in the
success case, silent in the general case" horizon-note drift).

---

## Register summary

**76 total rows** across 8 dimensions (D1: 9, D2: 18, D3: 5, D4: 12,
D5: 3, D6: 6 including D6-4's cross-referenced non-tally row, D7: 14,
D8: 9). Disposition counts, independently recounted by direct tally
per dimension (not trusted from any sub-agent's own summary line —
the same discipline AR-RR2-2 applies upstream): **close-as-fine 57**,
**fix-session-candidate 8**, **ruling-needed 5**, **intake 5**, plus
**1 explicitly non-tallied cross-reference row** (D6-4, folded into
D2's own count rather than double-counted). 57+8+5+5+1 = 76, matching
the total row count exactly.

No dimension was left unprobed; every row's evidence was gathered by
re-derivation, re-run, live execution, or direct read-in-context — no
claim was re-read as its own verification. The ten named-minimum
history-scan incidents (AR-RR2-3) are all accounted for, each folded
into its owning dimension's table rather than double-counted, with
cross-references recorded in the History-scan section above.

**The single cross-dimension pattern worth naming explicitly:** three
separate dimensions this review (D2's D2-18, D3's H-5-driven verdict,
and D8's D8-3) each found a fix or gate that closed the SPECIFIC
trigger a prior probe or incident happened to exercise, while leaving
a structurally adjacent, more general trigger of the identical class
untouched — `2088763`'s classpath break and `cd08b20`'s warm-cache
break are two closely related instances of "the fast/local/per-push
lane structurally can't see this," landing in the SAME session
(ADR-0091); D8-3's permission-denied gap is the missing-path fix's own
untested sibling. None of these three is severe on its own, and all
three are small, well-precedented fixes — but their recurrence across
three independent dimension probes, in a single review window, is
itself the kind of signal this skill's rotating rubric exists to
surface.

**Fix-session clusters this register's own dispositions suggest
(informational only — the ruled plan is ADR-0092's own step 5, not
this register):** (a) the requires-vs-classpath static gate (D2-18)
pairs naturally with D2-4's orphaned `verify-nist-lock` wiring — both
are "a check exists but doesn't run where it should" fixes to
`test.yml`/CI wiring; (b) D4-5/D4-6/D4-7's three unguarded
`edn/read-string`/`json/read-str` CLI flag-file reads share one root
cause and one precedented fix shape, plus D8-3's own
permission-denied gap in the same file — plausibly one small session;
(c) D8-4's flag-matrix nuance and D8-7's broken link are both trivial,
single-line, could ride along with any doc-touching session; (d)
D7-7/D7-8's missing roadmap anchors are a two-line roadmap edit each,
not a design session.

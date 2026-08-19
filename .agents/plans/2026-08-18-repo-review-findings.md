# Repo review 4 — the fourth assessment (findings register)

Findings-only register for the `repo-review` skill's fourth survey
(`.agents/skills/repo-review/SKILL.md`, steps 1-4). This is an
ASSESSMENT, not a fix session: every row is a PROPOSED recommendation,
and this session executes none of them. Per the author's ruling of
2026-08-18 ("Q1 c, Q2 register and separate fix session"), the fix
sessions are separate and follow the author's rulings on the plan.

Row format matches reviews 1-3: `id | probe | evidence | finding |
recommendation | disposition`. Disposition in {ruling-needed,
fix-session-candidate, close-as-fine, intake}. Every row's evidence was
gathered by the mechanism the rubric names for its own dimension —
re-derive, re-hash, re-run, never re-read a claim as its own
verification.

Landed 2026-08-18 against tip `4d6ff78` (ADR-0153's addendum; tree
clean; no Step-0 mutation of any kind, unlike review 3, which landed a
rubric amendment at its own Step 0).

The prior assessment (`.agents/plans/2026-08-15-repo-review-findings.md`,
review 3, `notes/adr/0139-review-3-arc-close.md`) is this review's
baseline. Its scoreboard is carried forward below, and its twelve-row
inherited watch-list is re-derived row by row in D7.

**Shape — author-ruled (Q1 "c", HYBRID).** The coordinating session ran
the eight-dimension battery itself under a probe budget of at most 12
probes per dimension (96 cap), and dispatched three sub-agents, one per
line this window opened (L-1 oracle coverage, L-2 exit-code
truthfulness, L-3 generated-surface completeness), each in its own fresh
clone of `4d6ff78` with no probe cap. **Sub-agent rows are
transcript-witnessed until the coordinator re-derives at least one cited
artifact per finding in its own clone.** Every sub-agent row below
carries its provenance explicitly: `RE-DERIVED` (the coordinator
reproduced it) or `COORDINATOR COULD NOT REPRODUCE` (recorded, never
dropped, never promoted).

---

## Step 0 — record

| act | evidence |
|---|---|
| Fresh clone | `git clone` of the edit root into the session scratchpad, `git checkout 4d6ff78`, `CLONE-OK`. All Step-0 measurements and the pre-digest ran there; read-only probes ran against the verified-clean edit root at the same tip. |
| Preflight | `bin/preflight`, exit 0: **last five CI runs on `main` all green** (`4d6ff78`, `5563f71`, `c1a40d0`, `c509e46`, `1e261f5`, newest first, `2026-08-18T23:18:25Z` down to `19:24:58Z`); edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`; working tree clean including untracked; local HEAD == `origin/main` at `4d6ff78`; last `stable-*` tag `stable-20260818-surge-policy-self-check-202` @ `5563f71`; **HEAD untagged and no tag owed at Step 0**, exactly as the prompt states. |
| Arc mid-flight? | **No.** ADR-0153 closed and its tag paid in session (its own addendum). The sequencing gate the skill names ("an arc is mid-flight — reviews open arcs, they do not interrupt them") is satisfied. |
| Baseline suite | `make test > <log> 2>&1` (a **redirect, not a pipe**), `MAKE_EXIT=$?` captured in its own file: **`MAKE_EXIT=0`**, **348 zero-failure blocks / 3,960 tests / 17,758 assertions**, `grep -cE '^(FAIL\|ERROR) in'` = **0**. Reconciles **exactly** against the figures the prompt cites for ADR-0153 (348 / 3,960 / 17,758) — though see **D1-1** for where those figures actually live. `clojure -M:poly check` and `bin/verify-nist-lock` both green as part of the target. |
| Suite wall time | **1,273 s (21m13s)** at Step 0, against ADR-0149's recorded 14m52s for the same target. **Not a comparable measurement and not reported as one:** this run shared the machine with three sub-agents, one of which ran two full 35-root oracle digests and another `make docsgen`. Recorded so the number is not mistaken for a regression, and so review 5 knows the contention. This is the prompt's D4 timing probe, reported where it fits rather than under the rubric's D4 (error honesty) — see D4's disclosure. **The close-phase suite settles the comparison: on a quiet machine it ran 944 s (15m44s) with the identical 348 / 3,960 / 17,758, against ADR-0149's 892 s — roughly +6% across five ADRs that added test namespaces. Nothing got materially slower; the Step-0 figure was contention, as disclosed.** |
| Reading sets | All five under budget, taken from the generated `state-derived.md` rather than from prose: `:corpus` 1801/2045, `:docs` 708/785, `:judge` 895/1000, `:onboarding` 1398/1530, `:sim` 1247/1405. `:onboarding`'s headroom is **132** lines, up from the 32 that review 3 flagged as its tightest tripwire — the ADR-0143 ratchet re-baselined it. |
| Oracle pre-digest | Run in the coordinator's own clone with the deps block copied from `bin/regression-oracle`'s own `run_one`: `clojure -Sdeps … -M:oracle-run -m ehrt.oracle.digest $OUT`, **exit 0**, **35 `.edn` files**, 35-line `sha256sum` manifest kept as this run's artifact. Used directly to re-derive L-1's headline claims (see L1-1, L1-8). |
| Mutations at Step 0 | **None.** Unlike review 3, which landed a rubric amendment at its own Step 0, this session amends nothing: the rubric amendment D4-4 proposes is a **plan item for ruling**, not an act. |

---

## AR-RR4-1 — Prior arithmetic re-derivation (skill step 4's standing sub-step)

Method: mechanical extraction of every row whose first cell is a
`D<n>-<id>` label across all eight of review 3's dimension tables, with
the disposition read from each row's last cell — never trusting review
3's own summary line.

**The first extraction disagreed, and the disagreement was the
instrument's, not review 3's.** Run against the LIVE register file, the
count is **42 rows** (D1: 10) against review 3's claimed 40 (D1: 8), and
the disposition tally comes out 25 close-as-fine / 2
fix-session-candidate / 3 intake / 1 cross-reference plus **11 rows
whose last cell is a FIXED / ENCODED IN GATE / REGISTERED narrative
rather than a disposition token**.

Both discrepancies have the same cause, and it is a property of how this
repo's review arcs work: **the arc's fix sessions overwrite the
disposition cell in place**, and two rows (D1-9, D1-10) were *added* to
the register during the arc by fix session B. So the live file is not
the register whose arithmetic the summary line describes.

Re-derived against the register **as first committed** (`bc6f46c`,
"docs: repo review 3 -- findings register and mitigation plan"), which
is the correct population for a summary written at that commit:

| dim | rows (re-derived at `bc6f46c`) | review 3's own claim |
|---|---|---|
| D1 | 8 | 8 |
| D2 | 7 | 7 |
| D3 | 3 | 3 |
| D4 | 4 | 4 |
| D5 | 4 | 4 |
| D6 | 4 | 4 |
| D7 | 5 | 5 |
| D8 | 5 | 5 |
| **Total** | **40** | **40** |

Disposition tally, re-derived at `bc6f46c`: **close-as-fine 26**,
**fix-session-candidate 8**, **ruling-needed 2**, **intake 3**, plus
**1 non-tallied cross-reference row** (D2-5). 26+8+2+3 = 39, +1 = 40.

**Result: review 3's own summary arithmetic re-derives EXACTLY, in
every figure.** No correction owed. This is the second consecutive
review whose predecessor needed no arithmetic fix-forward.

The 11 arc-changed cells also re-derive exactly against ADR-0139's own
close-note claim of "eleven cells moved (8 FIXED, 1 encoded-in-gate, 2
registered)".

**Method note for review 5, in the tradition of review 3's own note
about `**(new)**` markers:** re-derive a predecessor's summary against
the register *as first committed*, not against the live file. This
repo's arcs mutate the register in place — dispositions are overwritten
by FIXED narratives and new rows are appended mid-arc — so a live-file
extraction measures a different population than the summary describes,
and will report a phantom discrepancy in both the row count and the
tally. `git log --follow` on the register, then `git show <first>:<path>`,
is the probe.

Commands:

    F=.agents/plans/2026-08-15-repo-review-findings.md
    FIRST=$(git log --format=%H --follow -- $F | tail -1)   # bc6f46c
    git show $FIRST:$F | grep -oE '^\| *(\*\*)?[DH][0-9]+-[0-9]+' | wc -l
    git show $FIRST:$F | grep -E '^\| *(\*\*)?[DH][0-9]+-[0-9]+' \
      | sed 's/ *|$//' | awk -F'|' '{print $NF}' \
      | grep -oE 'close-as-fine|fix-session-candidate|ruling-needed|intake|cross-reference' \
      | sort | uniq -c

---

## AR-RR4-2 — History scan, window ADR-0140 → ADR-0153

Window: **fourteen ADRs**, 2026-08-15 → 2026-08-18, by direct count
(`ls notes/adr/01[45]*.md`). This is the first window sized by ADR-0139
Q3 "a."'s cadence rule (roughly 15 ADRs), against review 3's 44 — and
the rule delivered what it promised: **every one of the fourteen was
read at heading depth PLUS its Deviations / Findings / "things worth
your attention" sections in full**, which is the D6-4 probe review 3
had to record as partial. D6-4 is discharged in this review, on
evidence, not by assertion.

The window's arcs: fence battery (0140), event-log contract arc
(0141-0142), compression arc (0143-0147), exercised-sources gate
(0148), traces gate (0149), shape defects + contract 1.1.0/1.2.0
(0150-0151), sim-theory head hop (0152), surge fix (0153).

Every incident, deviation, disclosed self-inflicted red, prediction
miss and channel erratum, classified to a dimension. Each seed the
prompt named was VERIFIED against the tree rather than carried.

| # | incident | class | dimension | verified |
|---|---|---|---|---|
| H-1 | **The executable-bit class, three hits.** `bin/state-migrate-0147` (ADR-0147 S-7), `bin/regen-traces` (ADR-0149, reached CI red at `76b4e20`), and ADR-0150's Step 0 disclosing that same red among the last five runs. Cause: `core.fileMode=false`. | environment residue | **D3** | Confirmed: `git config --local core.fileMode` = `false` in the edit root, `true` in a fresh clone. Gated by `executable_bits_test` reading the INDEX. **Root cause is unaddressed — see D3-1.** |
| H-2 | **`poly test brick:` used as a pre-push gate** (ADR-0149): a tree-scanning gate lives in another brick, so a targeted run is not a substitute. Second instance in two weeks. | guard scope | **D2** | Confirmed; now `rulings.md#R-full-suite-before-push` (ADR-0150). |
| H-3 | **The `echo \| tee` exit masking** (ADR-0152): a red suite reported as "exit code 0"; only the captured `MAKE_EXIT` caught it. Two suite reds in that session, neither visible in the run's tail. | error honesty / harness | **D4** | Confirmed verbatim at `notes/adr/0152-*.md:200-208`. **This is the "NEW way to mask an exit code" H-2/H-3 of review 3's watch-list said to watch for** — see the L-2 section. |
| H-4 | **`state-derived.md` undocumented movers**: adding an ADR or a register row (ADR-0143's note), then adding a TEST NAMESPACE (ADR-0151, ADR-0152) — the last never written down before the run found it. | derivation drift | **D5** | Confirmed; see the L-3 section and D1-2. |
| H-5 | **The `.edn` that did not validate against the schema its own header claimed** (ADR-0152): `sim-theory.edn`'s two external stages sat inside `:stages`, silently dropping `{external: true}` and publishing a diagram claiming this repo implements SystemUnderTest. Review 3's C-1 was UNDERSTATED — it named an ungated hop, not a live invalid artifact. | claim-reality | **D1/D5** | Confirmed at ADR-0152's own STOP record; the fix landed and `sim-theory-equations.txt` joined the CI freshness list. |
| H-6 | **Four channel-erratum classes, this window's own**: history-as-current (`state.md:73` asserted a hazard the commit eliminated, ADR-0152); carry-forward figures (ADR-0147's baseline vs ADR-0146's recorded 3,830/17,354, resolved by measuring); unearned specificity including mechanisms that do not exist (`docsgen_test` population, `valid-log?`, oracle IDENTICAL prediction (d) — ADR-0151 found it "unsatisfiable by any correct implementation"); fence-names-wrong-instrument (ADR-0144's numstat ledger, where the fence named a diffstat and the read-back was the stronger instrument). | continuity / claim-reality | **D1/D7** | All four verified in the ADRs' own text. **A fifth instance is this session's own driving prompt — see D1-1.** |
| H-7 | **One message-only `--amend` of an unpushed commit**, disclosed (ADR-0153). | ceremony | **D2** | No standing rule permits or forbids it; `rulings.md` has no `--amend` row. **Raised as a ruling in the plan** rather than dispositioned here. |
| H-8 | **Two prediction misses, both in the safe direction.** ADR-0150's prediction (c) census was one short — a `{:keys [... units ...]}` destructure fell between two greps' exclusions, caught by the full suite. ADR-0151's prediction (c) second half was WRONG: the manual's ground-truth digest was predicted to MOVE and did not. | sampling / method | **D6** | Both confirmed in the ADRs' own text. Both were caught by running rather than reading, which is the mitigation working. |
| H-9 | **A gate whose subject its own commit introduces** (ADR-0144 F-11): guard #1 matched zero rows at the red capture. | guard coverage | **D2** | Confirmed; its dual carried the pre-migration population and fired on 25. Recorded as method, not a defect. |

**Repeat-hit classes, which the skill says raise their dimension's
severity:** the executable-bit/environment-residue class (three hits,
D3), the exit-code-masking class (H-2/H-3 of review 3 said to watch for
recurrence; it recurred in a NEW shape, D4), and the
population-narrower-than-the-class family, which is review 3's central
finding and recurs in this review at D2-2, D4-1 and D5-1.

---

## Dimension 1 — Claim-reality coherence

Probes run: 5 of a 12 budget. Population for the count probe:
`state-derived.md`'s own claim table, enumerated from the file and
re-derived against the tree by each claim's own generator definition.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D1-1 | The window's suite-count chain: does each ADR's "reconciling exactly against ADR-NNNN's X/Y/Z" cite a figure the cited ADR carries? | Five consecutive instances checked by grepping the cited ADR for the cited figure. **All five miss.** ADR-0150 cites "ADR-0149's 346 / 3,918 / 17,610"; `grep -E '346\|3,918\|17,610' notes/adr/0149-*.md` returns nothing (ADR-0149 records its own BASELINE, 344 / 3,906 / 17,548). Same for 0151→0150, 0152→0151, 0153→0152. The cited close figures DO exist — in the **session records** (`grep -rl '3,918' .agents/session-records/` → `2026-08-17-demos-traces-gated.md:173`). | The substance reconciles; the **citation** does not. Each ADR records its own baseline and never its own close, so the close figure lives only in the session record, while the reconciliation sentence attributes it to the ADR. A cold reader following the citation finds a different number and concludes the chain is broken when it is not. **This session's own driving prompt repeats the class** — it asks to reconcile against "ADR-0153's 348 / 3,960 / 17,758", and ADR-0153 does not carry 3,960/17,758 either; `.agents/session-records/2026-08-18-surge-policy-self-check-202.md:83` does. | Either (a) each ADR records its own CLOSE figure in its Verification section, making the citation true, or (b) the reconciliation sentence cites the session record. (a) is better: the ADR is the narrative of record, and a close figure absent from it is a hole a successor must go elsewhere to fill. | fix-session-candidate |
| D1-2 | `.agents/state-derived.md`: re-derive every stated count against the tree, using each claim's own generator definition rather than a guessed one | Eleven claims sampled and re-derived: components 18/18, bases 1/1, module JSONs 31/31, oracle roots 35/35, test namespaces 187/187, docs-tooling gates 44/44, ADR files 151/151, rulings rows 113/113, rulings superseded 7/7, session records 156/156, archived prompts 149/149. | **All eleven re-derive EXACTLY**, and the generator's own freshness holds: `make state-derived` in the coordinator's clone exits 0 and `git diff --exit-code` over `state-derived.md` and both record `INDEX.md` files returns **0** — the committed bytes regenerate exactly. ADR-0147's generation design is doing what it was built for. **Two of the eleven initially appeared to disagree, and both were the auditor's error, not the register's**: my test-namespace probe omitted `projects/` (the generator concatenates `components`, `bases` AND `projects` — `state_derived.clj:211-213`), and my oracle-root `awk` terminated a line early (the map at `digest.clj:544` holds 35). A third apparent miss — superseded 8 vs the claimed 7 — was the row-contract TEMPLATE line in `rulings.md`'s own header being caught by a naive grep. | None. Recorded as the strongest clean result in this review, and as a standing caution: three of my own probes produced false discrepancies against a register that was right every time. The rubric's "audit uses the mechanism it recommends" applies to the auditor first. | close-as-fine |
| D1-3 | `.agents/state.md` pointer rot (the ADR-0147 probe shape): does every register it names exist, and does every gate it cites still run? | 16 named paths, all present. 8 cited gate namespaces, all resolve to a real `*_test.clj` under `components/docs-tooling/test/`. 4 cited `rulings.md#R-` anchors, all present. | **Clean, no rot.** | None, but see D1-4. | close-as-fine |
| D1-4 | `state.md`'s own line cap headroom | `wc -l .agents/state.md` = **119** against a cap of **120** (`state-residue-test`). ADR-0152 already went red on this cap once and compacted rather than raised it. | **One line of headroom.** Not a finding — a tripwire, in the same shape as review 3's `:onboarding` 32-line note. The next session that adds a bullet to `state.md` goes red, and the gate's own message names raising the cap as "the move this arc exists to make unavailable". | Expect to compact, not to bump. Named here so the next session is not surprised by the gate. | close-as-fine |
| D1-5 | `docs/dev/AUDIENCES.md`'s hand-maintained segment count vs actual | Header claims "Six segments"; `grep -cE '^[0-9]+\. \*\*'` over the `## Audience` section = **6**. The file's own text records that this same header "had drifted to 'Seven' three segments behind actual count" before ADR-0119 pared it. | **Correct today**, and ungated — see D2-1. | None on the count. | close-as-fine |

**Dimension 1 register summary:** 5 rows. **4 close-as-fine, 1
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 1 verdict: GREEN (improved from YELLOW).** Review 3's D1
went yellow on 25 dead links found by a widened scan root; that scan is
now gated (`stale_path_test`'s fourth scan) and this review found no
recurrence. The one open row is a citation-provenance defect, not a
claim-reality defect: every number in the window is *right*, and the
generated register re-derives perfectly on all eleven sampled claims.
What is wrong is where four ADRs say a number came from.

---

## Dimension 2 — Guard coverage

Probes run: 6 of a 12 budget. Population: all **113** rulings rows
enumerated from `.agents/rulings.md` by row shape (`^- \*\*R-<slug>\*\*`),
plus the CI workflow read in full and the `docs-tooling` test directory
listed from the filesystem.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-1 | Map each of the **nine** rulings rows added in this window to its enforcing gate, by MECHANISM rather than by slug citation | `R-adr-index-generated` → `adr_index_test/notes-adrs-md-is-exactly-what-the-generator-renders-test` (byte parity). `R-budget-stop` → `reading_set_budget_test/no-budget-exceeds-the-committed-baseline-test` (the ratchet, not just the budget). `R-exercised-implies-gated` → the ADR-0148 coverage test over `load-registry`. **`R-audience-has-entry-path` → NOTHING.** No test names `docs/dev/AUDIENCES.md` except `stale_path_test`, which resolves paths, not entry-path presence; `git grep AUDIENCES -- 'components/**/test'` returns one path-resolution hit and nothing else. | **A law with no gate.** ADR-0146 landed the rule *and* the sixth segment it exists to protect, and nothing enforces it. The register is hand-maintained, its header count has drifted before (D1-5), and `docs/README.md` "routes off this register rather than defining its own paths", so a segment without an entry path is, in the rule's own words, "a routing gap everywhere that register is keyed off". Segment 5 ("The Clojure library consumer, deferred stub") is the live edge case: it states a *serving* ("source docstrings only") but not a document entry path — defensible either way, which is exactly why the rule needs a gate to settle it. | A `docs-tooling` gate: every numbered segment under `## Audience` contains at least one markdown link, with `deferred stub` either exempted explicitly by the test or given a path. Rule on segment 5 first — the gate encodes whichever answer the author gives. | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** |
| D2-2 | The five remaining new rows are PROCESS rules — are they gated where process rules live? | `R-full-suite-before-push`, `R-red-pushed-with-green`, `R-register-hygiene-at-close`, `R-budget-stop` are all cited in `.agents/skills/build-session/SKILL.md` (and its byte-identical `.claude/` mirror). **`R-session-verifies-ci-via-gh` and `R-stop-only-on-two-defensible-readings` are cited in NO skill, no bin script, no CI file, no test** (`grep -rl` over `.agents/skills .claude/skills bin .githooks .github` → empty for both). | Two standing rules that bind every session live only as rows in a register the session is not required to read end-to-end. `R-session-verifies-ci-via-gh` is load-bearing for the tag licence — it is the rule that decides whether a close tag is payable in session — and it was applied correctly four times this window, but by ADRs quoting it, not by any surface a session is routed through. | Add both to `build-session/SKILL.md` where the other four already live (tag ceremony section, STOP section respectively), mirrors byte-copied. This is the R-law-surface-propagation pattern applied to two rules that never got it. | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** |
| D2-3 | Laws stated on multiple surfaces with no drift gate (`R-law-surface-propagation`'s own subject) | `.agents/skills` ↔ `.claude/skills`: **60 files, byte-identical, zero drift**, verified independently by `cmp` over every common path plus a two-way `comm` for orphans. The gate itself (`skill_mirror_currency_test`) was read: it asserts presence, **content equality via `slurp`**, executable-bit equality, and absence of orphans — all four. | **Clean, and the gate is complete** — this is the strongest guard in the repo. Review 2's D2-17 watch item ("has the third-law-drifts trigger fired?") still has not fired. | None — re-probe at review 5. | close-as-fine |
| D2-4 | Which of CI's **20** freshness-diffed paths have a LOCAL gate, and are the CI-only ones named as such where they live? | Mapped each path to the test tree. Locally gated: `notes/ADRs.md` (`adr_index_test`), `state-derived.md` + **both** record `INDEX.md` files (`state_derived_test/every-generated-record-index-matches-a-fresh-render-test`), `event-schema.edn` (`event_schema_test/committed-export-matches-the-source`), `demos/traces/` (`traces_fresh_test`), the sim-theory chain (`sim_theory_head_hop_test`). **Not locally gated: the three `components/palgebra/examples/*.mermaid`** (`git grep -l 'palgebra/examples' -- '*_test.clj'` → empty) **and `event-examples.edn`** — and that last one is a sharp asymmetry rather than an oversight-shaped gap: its sibling `event-schema.edn` has a dedicated freshness test (`committed-export-matches-the-source`, whose docstring reads *"the published EDN contract can never lag the Clojure source -- `make docsgen` regenerates it, CI diffs it, this asserts it"*), while **zero** test files mention `event-examples` or `write-examples` at all. `docsgen_test`'s own docstring states the staleness guard "is now [CI's] step" — it tests the renderer against a test spec, not the committed artifact. | Four of twenty paths are **CI-only**, and the workflow's 60-line comment block — which narrates every population widening in detail — does not distinguish locally-gated paths from CI-only ones. A local `make test` is green while those four are stale. This is not a defect in any artifact; it is the rubric's "CI-only checks named as such where they live", unmet. | One paragraph in the workflow's comment block naming which paths this step is the SOLE gate for, and the same list in `state.md`'s registers table or `AGENTS.md`. Optionally close the gap for the three `.mermaid` (cheap: the converter is already exercised by `mermaid_render_test`). | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** -- the naming paragraph landed; the OPTIONAL local gate for the three `.mermaid` was deliberately not taken and is named as its own change in the workflow comment |
| D2-5 | Is there a gate against the exit-code-masking class in its NEW shape? (review 3's H-2/H-3 watch item, which said to watch for a new way, not the old defect) | See the L-2 section: the taught law forbids a pipe or `tail` on the GATE command; ADR-0152's mask was in the WRAPPER's last command, which the law does not mention. `git grep -niE 'MAKE_EXIT\|masked' -- '*test*.clj'` → no gate of any kind. | **The watch item fired.** Recorded here as a guard-coverage fact; counted and evidenced in L-2 (L2-1), not tallied twice. | See L2-1. | **cross-reference to L2-1, not separately tallied** |
| D2-6 | Does any standing rule govern `git commit --amend`? (H-7) | `grep -i amend .agents/rulings.md` → no row. `build-session/SKILL.md` — no rule. ADR-0153 disclosed one message-only amend of an unpushed commit. | No law either way. The act is defensible (unpushed, message-only) and was disclosed, which is the discipline working; but the next session has no rule to follow and will have to re-reason it. | **Author ruling**, offered as lettered options in the plan. | ruling-needed; **FIXED ADR-0156 (2026-08-19)** |

**Dimension 2 register summary:** 5 tallied rows (D2-5 is a
cross-reference, not tallied). **1 close-as-fine, 3
fix-session-candidate, 1 ruling-needed, 0 intake.**

**Dimension 2 verdict: RED (regressed from YELLOW).** The mirror guard
is complete and the gate population keeps growing with the fixes that
earn it — but this dimension is scored on severity, not count, and the
severity is here. Three rows in this table are laws or gates whose
coverage is narrower than the class they are believed to cover: a rule
with no gate at all (D2-1), two rules on no surface a session reads
(D2-2), and four artifacts whose only gate is CI, undisclosed (D2-4).
**Two more arrive from L-3 and they are what makes this red:** the
workflow's "a new derived file goes on a make target AND on the diff
list" rule has closure assertions for exactly 2 of 12 leaves and 2 of 19
paths, so the rest can be dropped with the whole suite green (L3-1); and
`event-schema-baseline.edn`'s freeze — the thing that makes the repo's
**only** schema-change gate non-vacuous — is protected by a sentence in
a header and nothing else (L3-2). A gate that can be silently voided by
one word on a Makefile line is a red finding, and the class behind L3-1
has now been closed one artifact at a time, three times, without ever
being closed as a class.

---

## Dimension 3 — Environment independence

Probes run: 4 of a 12 budget.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D3-1 | **The executable-bit class's ROOT cause** — three hits in this window (H-1), always gated at the symptom, never diagnosed at the source | `git config --local --list` in the edit root: **`core.filemode=false`** AND **`core.ignorecase=true`**. The same read in a fresh `git clone` of the same repo: `core.fileMode` = **true**, `core.ignorecase` **unset**. Both are Windows-filesystem settings; the edit root is on **ext4** (`bin/preflight` check 2 confirms it is not under `/mnt/`), which is case-sensitive and does carry the executable bit. | **The three-hit class is residue of the retired `/mnt/c` era, surviving in the edit root's own `.git/config`, and no ADR has ever named it.** Every session treated `core.fileMode=false` as an unavoidable property of the workspace and gated the symptom (`executable_bits_test` reads the INDEX precisely because the worktree bit lies). The setting is a one-line local config, wrong for the filesystem it now sits on. `core.ignorecase=true` is the same residue with no recorded hit yet: it can mask a case-only rename, and it is equally wrong on ext4. | **Verified safe to flip:** a fresh clone at this tip with `core.fileMode=true` has a completely clean tracked tree (`git status --porcelain -uno` → empty), so `git config --local core.fileMode true` in the edit root introduces **zero** churn. There are no case-colliding tracked paths today (`git ls-files \| tr A-Z a-z \| sort \| uniq -d` → empty), so `--unset core.ignorecase` is equally safe. Keep `executable_bits_test` regardless — it is the gate that made the class visible. | fix-session-candidate; **PARTLY FIXED ADR-0157 (2026-08-19)** — the `bin/preflight` gate landed, and running it found this row's own "verified safe" to be FALSE. The row measured a FRESH CLONE and concluded about the EDIT ROOT: flipping `core.fileMode` there yields **360** `mode change 100644 => 100755` on ordinary text files whose worktree bit `core.fileMode=false` had been hiding, with zero content change. The index is sound and identical in both clones (1382 at `100644`, 45 at `100755`). Config restored to as-found; the flip plus a `chmod -x` sweep is AUTHOR ACTION, re-rowed as `roadmap.md#edit-root-worktree-residue`. |
| D3-2 | The local cold-clone probe (review 3's D3-1: "restore the review-2 method or retire it — two substitutions is where a method quietly becomes a former method") | `Makefile:288-297`, target **`ci-parity`**: `rm -rf` a scratch dir, `git clone --quiet .`, then `poly check` and `poly test :all skip:integration` with **`EHR_TESTING_TOOLS_CACHE` repointed to a cold cache dir**. That is the review-2 method, in the make graph, with a `make help` line describing it as "green as CI sees it, runnable locally". | **The watch-list posed a two-way choice — restore or retire — and the tree holds a third answer neither review saw: the method was never lost.** It has been a named `make` target the whole time. Two consecutive reviews recorded the probe as "substituted by CI's cold runner" while `make ci-parity` sat one `grep` away. This is the population-closure lesson applied to a review's own instrument: D3-1 enumerated its options from the register's memory instead of from the tree. | Run `make ci-parity` as the standing D3 probe from review 5 onward, and name it in the rubric's D3 text so the method cannot go missing again. It does not repoint `HOME`, so `~/.m2` is still shared — state that limit rather than claiming a fully cold environment. | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** |
| D3-3 | `.gitattributes` byte-determinism, and ADR-0149's CRLF finding re-probed | `.gitattributes` read in full: `* text=auto eol=lf` plus **six** `-text` carve-outs, each with a dated rationale (vendored modules, `demos/traces/**/messages*.txt`, the v2 ER7 fixtures, the NIST bundle's XML members and its ER7, the simhospital LICENSE). ADR-0149's finding was "five of six `ground-truth.edn` carry CRLF in this working tree, LF in HEAD"; `file` over all six in BOTH the edit root and a fresh clone now returns **ASCII text (LF) for all six, in both**. | **ADR-0149's CRLF finding is resolved** — `make traces` writes LF and the committed bytes match. The `-text` carve-out family is complete for every byte-precious tree this review could enumerate, each one traceable to the incident that earned it. | None. | close-as-fine |
| D3-4 | Does anything depend on untracked files or author-local checkouts? | The Step-0 baseline ran in a fresh `git clone` at `4d6ff78` with no author-local setup beyond the shared `~/.m2` and `~/.ehrt` caches. | Clean at the tree level. The shared-cache caveat is exactly what D3-2's `make ci-parity` exists to close, and is why that row is a fix-session candidate rather than close-as-fine. | See D3-2. | close-as-fine |

**Dimension 3 register summary:** 4 rows. **2 close-as-fine, 2
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 3 verdict: YELLOW (unchanged), but for a better-understood
reason than in any prior review.** Both open rows are now *actionable*
rather than structural: the environment class that has bitten three
times has a verified-safe two-line fix (D3-1), and the probe two
reviews recorded as lost turns out to be a `make` target (D3-2). This
dimension has been yellow for four consecutive reviews on the strength
of a substituted probe; it should be green or red on real evidence at
review 5, for the first time.

---

## Dimension 4 — Error honesty

Probes run: 4 of a 12 budget.

**A premise mismatch, disclosed rather than silently adapted.** The
driving prompt assigns D4 the probe *"`make test` timings vs the
window's recorded ones"*. The rubric's D4 is **error honesty**; timings
belong to no dimension in the rubric. Both were run: the error-honesty
probes are below, and the timing measurement is reported under D3/Step
0 with its own caveat, so neither the prompt's probe nor the rubric's
dimension was dropped.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D4-1 | Ignored boolean returns on `.mkdirs` / `.delete` (a probe the rubric's own D3 text names explicitly), population enumerated from `git ls-files` rather than a pathspec | **13** `.mkdirs` call sites in production `src`, **every one discarding the boolean**: `cli/core.clj:725,726,1030`, `corpus_io/spool.clj:203`, `corpus/generate.clj:282`, `corpus/generators.clj:162`, `corpus/intake.clj:376`, `docs_tooling/usecases.clj:298`, `judge_fhir_official/fhir.clj:193,252`, `oracle/digest.clj:586`, `sim_trajectory/census.clj:527`, and **`kernel/artifact.clj:191` — inside the kernel itself**. Plus 2 `.delete` sites (`kernel/artifact.clj:138,141`). The result-or-loud lint's own forbidden set is `[#"\(\.listFiles\b" #"\(\.list\b" #"\(\.renameTo\b"]` (`io_vocabulary_lint_test.clj:39`) — **`.mkdirs` and `.delete` are not in it**, and `ehrt.kernel` provides no directory-creation wrapper at all. | **The lint that enforces `R-io-result-or-loud` has a population narrower than the rule it enforces.** ADR-0143's own account of why this matters — "a nil from `.listFiles` is an I/O failure, not an empty directory" — applies with a twist to `.mkdirs`, whose `false` is genuinely ambiguous (already existed, or failed). A silent `false` followed by a write yields a confusing downstream `FileNotFoundException` instead of a named failure, which is `R-errors-name-artifact`'s exact subject. This is the third instance in this review of review 3's central pattern. | Not a mechanical sweep: `.mkdirs` returning `false` is not by itself an error, so the fix is a kernel helper asserting `(or (.mkdirs f) (.isDirectory f))` and failing loud otherwise, then routing the 13 sites through it and widening the lint's forbidden set by two. Red-first on one site. | fix-session-candidate; **FIXED ADR-0157 (2026-08-19)** — 15 sites routed (13 `.mkdirs`, 2 `.delete`) across 10 files; not one of the 13 checked the boolean by hand, so no check was doubled. Both `.delete` sites needed the declared `delete-quietly!` variant, which is what earned it. Lint at five patterns, plus the population assertion it never had. |
| D4-2 | `catch` blocks that continue without a category, read in context rather than auto-flagged | 23 production files carry `(catch ...)`. The two that swallow into a value are `cli/core.clj:269` (`(catch Exception _ "unknown")`) and `:306` (`(catch Exception _ nil)`). Both read in context: each wraps a `ProcessBuilder` running `git describe` / `git config --get`, each already sets `ProcessBuilder$Redirect/DISCARD` on stderr, and each is answering an optional environment question where absence is a legitimate answer. | **Clean.** "This is not a git checkout" is a real answer, not an absorbed error, and both sites return a value that says so. No catch in production `src` swallows a failure it was asked to report. | None. | close-as-fine |
| D4-3 | The CLI's error surface: does every failure name its artifact and carry a category? | Run against the built CLI. Unknown flag → `{:status :error, :category :unknown-flag, :payload {:flag "--nope", :verb "gate fhir"}}`, exit 2. Unknown subcommand → `:category :unknown-command` with `:valid-options` (9) and `:hint "run: ehrt help"`, exit 2. Missing file → `:category :file-not-found, :payload {:path "/nonexistent.json"}`, exit 2. | **Clean, and better than the rubric asks** — each error names the artifact AND the verb, and the unknown-command case carries a recovery route. Review 2's three parse findings remain closed. | None. | close-as-fine |
| D4-4 | **The auditor's own false green** — recorded because the rubric says a probe's first question is "how do I know this is all of them?" | My first D4 pass used `git grep -- 'components/*/src' 'bases/*/src'` and returned **0 catch blocks and 0 nil-returning I/O calls** — a perfect green. A sanity check (`git grep -lE 'ns ' -- 'components/*/src'` → **0 files matched**) proved the pathspec matched nothing; the tree actually holds **101** production `.clj` files, 23 of them with catch blocks. | **A silent empty population reported as a clean verdict**, produced by this review's own instrument, in the dimension whose subject is exactly that. Recorded rather than quietly fixed, because it is the same class as ADR-0148's `R-empty-population-is-red` — and it argues that the rule should bind audit probes, not only tests. | Every probe in this rubric that reports a zero should first assert its population is non-empty. Proposed as a one-line rubric amendment in the plan (an amendment is a plan item for ruling, not an act of this session). | ruling-needed; **FIXED ADR-0156 (2026-08-19)** |

**Dimension 4 register summary:** 4 rows. **2 close-as-fine, 1
fix-session-candidate, 1 ruling-needed, 0 intake.**

**Dimension 4 verdict: GREEN (unchanged).** No error is absorbed as an
answer anywhere this review could reach: the catch blocks are honest,
the CLI's categories are complete, and the result-or-loud discipline
holds where it is enforced. It stays green rather than regressing on
D4-1 because that row is a *coverage* gap in a lint, with no live
mis-reported failure behind it — but D4-1 is the row most likely to
produce a real incident if left, since `.mkdirs` sits on every output
path the tool writes.

---

## Dimension 5 — Mirror and derivation drift

Probes run: 4 of a 12 budget. Population enumerated from the tree
first: `git grep -lIE 'GENERATED|generated by|DO NOT EDIT|regenerate'`
over tracked files, excluding the frozen record trees, yields **68**
files, which was then diffed against the make graph's 12 `docsgen`
leaves and CI's 20-path freshness list. The deep three-way enumeration
is L-3's; the rows below are the coordinator's own.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D5-1 | Do CI's 20 freshness paths cover every `docsgen` leaf's output? | All **12** leaves map onto the list: `pipeline`→`docs/dev/pipeline.md`; `use-cases`→`docs/use-cases.md` + `docs/use-cases/`; `operators-doc`; `cli-doc`; `sim-theory`→ the equations file, the `.mermaid` AND the `.md`; `palgebra-examples`→3 `.mermaid`; `event-schema-export`; `event-schema-examples`; `formats-event-log`→`docs/formats.md`; `adr-index`→`notes/ADRs.md`; `state-derived`→`state-derived.md` + both record `INDEX.md`; `traces`→`demos/traces/`. | **Complete, with no leaf unlisted.** The make-graph half of the ADR-0136 obligation ("a make target AND a diff-list entry, same commit") has held across four subsequent widenings. | None. | close-as-fine |
| D5-2 | **Hand-regenerated derived surfaces with a stated trigger, off every gate** — the class ADR-0152 closed one hop upstream | Five `docs/manual/assets/*.svg`, each carrying a banner naming its own regeneration trigger: `gt-emitters.svg` ("regenerate by hand if simulator-architecture.md section 4's own equations change", ADR-0120), `inject-expect-loop.svg` ("if pipeline.edn's Mutate/Gate stages or the worked instance's own witnessed values change", ADR-0124), `straddle-timeline.svg` ("if the cited section's own witnessed values change", ADR-0121), `two-clocks.svg` ("if the cited section's own field audit changes", ADR-0121), `verdict-ranking.svg` ("if the ranking or the reserved status of :indeterminate changes", ADR-0124). Plus `components/sim-trajectory/docs/trajectory-computation.md:261-262`, an embedded mermaid block: "regenerate by hand if the pipeline shape changes". **None is on the freshness list; none has a make target; `git grep -l 'manual/assets' -- components bases` → no gate anywhere; `grep -inE 'svg\|asset' .agents/plans/roadmap.md` → no row.** | **Six derived surfaces whose staleness trigger is written down and watched by nothing.** They are honestly labelled hand-authored — which is better than ADR-0135's `sim-theory-diagram.md`, mislabelled as hand-authored while carrying a GENERATED banner — but each names a *specific live source* whose change invalidates it, and four of the six sit in the user manual. This is `sim-theory.edn`'s class with the automation absent rather than incomplete. | These cannot join `docsgen` (no generator exists). The honest instruments are a register row per asset with its trigger, or a gate asserting each banner's cited source file is unchanged since the asset's last commit. **Ruling needed on which** — see the plan. | ruling-needed |
| D5-3 | Unregistered standing requests outside the registers (the D7 probe class, run against the whole live tree) | The same six banners above are the population's dominant member. `components/sim-trajectory/docs/gmf-interpreter-findings.md:927,1212` carry two more ("the next session vendoring a real `VitalSign`-bearing candidate should re-derive against source"), which are contingent on work that may never happen and are attached to the doc that would be read when it does. | The six D5-2 banners are unregistered standing requests by the rubric's own definition and are counted there, not twice. The two gmf ones are judged **narrative-legitimate**: they are conditional guidance sited exactly where the triggering session would read it, not requests awaiting action. | The gmf pair: none. The six: see D5-2. | close-as-fine |
| D5-4 | Byte-diff every mirrored pair | The skills mirror, 60 files, byte-identical — see D2-3. No other dual-homed doc surfaced in the 68-file banner enumeration. | Clean. | None. | close-as-fine |

**Dimension 5 register summary:** 4 rows. **3 close-as-fine, 0
fix-session-candidate, 1 ruling-needed, 0 intake.**

**Dimension 5 verdict: YELLOW (improved from RED).** Review 3's five
unregistered derivations are all registered and gated, and this window
added two more registrations on its own initiative (`demos/traces/**`,
ADR-0149; `sim-theory-equations.txt`, ADR-0152) — the second of which
found a live invalid `.edn` in the process. The dimension does not
reach green because D5-2 shows the same class is still open in a corner
no widening has reached: six hand-maintained artifacts whose triggers
are documented and unwatched.

---

## Dimension 6 — Sampling adequacy

Probes run: 3 of a 12 budget.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D6-1 | **Could the suite have caught ADR-0153's defect?** — sample power against the rarest branch a verdict vouches for, applied to this window's one real `src` bug | The relevant verdict is `check_test.clj:487`, `(defspec every-m1-run-satisfies-the-invariant-catalog 150 ...)` — 150 trials over a generated seed and 1-12 patients, whose name claims **every** m1 run satisfies **the invariant catalog**. Its facility is a fixed literal: `:ed` with `beds 0, surge-slots 15`; `:renal` with `beds 1, surge-slots 0`. ADR-0153's defect required a ward holding **both** a licensed bed and a surge slot (a discharging patient in a surge slot while a licensed bed in the boarder's home ward stands free), **plus** `--churn` to produce the `:cancel-admit`. `engine/run` is called here with no churn profile and no pathways. | **A 150-trial property test was structurally incapable of finding the bug its own name vouches against**, and no number of trials would have changed that: the *configuration* excludes the branch, not the sample size. The defect was found by a census sweep at one seed (202) and is now gated by a hand-built 3-patient repro plus a single run-level seed. This is the canonical D6 class in a sharper form than the census-seeds-vs-injuries original — there the sample was too small, here it is in the wrong place. | Widen the defspec's facility to a generated one (or add a second defspec) whose wards can carry both a licensed bed and a surge slot, and put a churn profile on some fraction of trials. The catalog's coverage claim then means what it says. Note the open `roadmap.md#bed-ready-vacancy-cascade` row is a **known remaining gap in the same code path** — a widened sweep is what would find its siblings. | fix-session-candidate |
| D6-2 | Seed policy and trial counts across the tree | **109** defspecs: 5 at 50 trials, 38 at 100, 18 at 150, 17 at 200 (the remainder inherit the default). `R-defspec-seed-policy` ("seeds stay unpinned repo-wide; a spec that has actually flaked pins or durably logs its seed") and `R-multi-seed-once-flagged` both re-read against the tree. | Trial counts are healthy and the unpinned-seed policy is followed. The weakness this review found is not trial count anywhere — it is D6-1's fixed configuration. | None. | close-as-fine |
| D6-3 | The window's two prediction misses (H-8) as sampling evidence | ADR-0150's `:units` census missed a `{:keys [... units ...]}` destructure that fell between a keyword grep and a `grep -v test/`-filtered word grep; the **full suite** caught it. ADR-0151 predicted the manual's ground-truth digest would MOVE; re-witnessing showed it did not, because ed-tuesday's 92 admissions carry 92 non-nil reasons. | Both misses were caught by *running* rather than reading, and both were disclosed with the method error named. This is the mitigation working, and it is why D6 does not regress. | None. | close-as-fine |

**Dimension 6 register summary:** 3 rows. **2 close-as-fine, 1
fix-session-candidate, 0 ruling-needed, 0 intake.**

**Dimension 6 verdict: YELLOW (regressed from GREEN).** Review 3 moved
this dimension to green when the census undercount was fixed. D6-1
regresses it on a single but load-bearing finding: the repo's broadest
correctness property — 150 trials, "every m1 run", the invariant
catalog — samples a two-ward facility that cannot express the
precondition of the one engine defect this window actually found. The
severity is a coverage claim wider than its sample, which is precisely
what this dimension exists to catch.

---

## Dimension 7 — Continuity integrity

Probes run: 5 of a 12 budget. The twelve inherited watch-list rows
(ADR-0139 :464-482) are re-derived to CURRENT state below, per the
prompt's instruction; the aging probe deliberately does **not** use
`roadmap.md` as its exclusion oracle (watch-list C-2's own instruction).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D7-1 | **The twelve inherited watch-list rows, each re-derived** | **C-1** (ungated `.edn`→equations hop): CLOSED by ADR-0152, and closing it found the `.edn` had already drifted and did not validate — the row was UNDERSTATED. **C-2** (CarePlan/Guard): rowed, `roadmap.md#careplan-guard-resolution` OPEN PRIORITY 4. **C-3** (attic rotation): rowed OPEN PRIORITY 5 and **measurably worse** — see D7-2. **C-4** (`state_staleness_tripwire_test` enumerates filenames): **FIXED, and better than recommended** — `arc-close-adrs` now enumerates from the tree by reading each ADR's own first heading for "arc close", AND a second gate (`every-arc-close-adr-carries-the-filename-convention-test`) asserts the filename convention holds, so a filename-keyed gate cannot silently under-enumerate. ADR-0139 offered the two options as alternatives; the fix took both. **D8-5**: discharged 2026-08-16 (ADR-0140), survivor re-measured at D8-1. **D1-9 / D1-10**: still open, still unrowed — see D7-3. **D3-1**: see D3-2 (the method exists as `make ci-parity`). **D6-4**: discharged by this review — all fourteen ADRs read in full. **D1-4** (compare sets, not cardinalities): held as method. **`:onboarding` headroom**: now 1398/1530, **132 lines**, up from 32 after the ADR-0143 ratchet re-baselined it. **H-2/H-3**: the watch fired — see L2-1. | **Ten of twelve are closed, discharged, rowed or improved.** The two that are neither are D1-9 and D1-10. | Per row above. | close-as-fine |
| D7-2 | C-3 re-measured rather than carried | `## Done` in `roadmap.md` now holds **66** CLOSED pointers, against the **44** review 3 recorded at its close. The section's own law (`R-remainder-tokens-the-row` neighbourhood, ADR-0144) is "`## Done` holds the current arc only"; the oldest pointers date to the conviction arc's close, 2026-08-08. | **The backlog this row names has grown by 50% since it was opened**, exactly as its own row text predicted ("ADR-0144 retokened those pointers and added six missing ones rather than rotating them, so the backlog this row names is larger, not smaller"). The row is honest and the work has not happened. | Rotation is a records session of its own, as the row says. Worth pricing in the plan: at 66 pointers spanning a dozen arcs, deciding the boundaries is the whole cost. | intake |
| D7-3 | Carried-item aging for items with NO register home (the probe run without using `roadmap.md` as its exclusion oracle) | **D1-9** (backticked-path shorthand) and **D1-10** (denylist-family widening) were opened 2026-08-15 by ADR-0137 under rulings R-B2/R-B3, dispositioned `fix-session-candidate`, handed to review 4 in ADR-0139's watch-list — and `grep -inE 'backtick\|shorthand\|denylist\|experiments' .agents/plans/roadmap.md` returns **nothing**. They have now been carried through one arc close (ADR-0139) and fourteen ADRs. | Two ruled fix-session candidates whose only home is a closed review's watch-list. `rulings.md#R-unregistered-request-gets-a-row` says an unregistered standing request gets a roadmap row before it gets a disposition, visibility first — these have a disposition and no row, which is the inverse. **By the pairing-as-data precedent they are now AGED and must be named as such**, which this row does. | One roadmap row each (or one row covering both, since R-B2/R-B3 were ruled together), citing ADR-0137. Cheap, and it is the standing remedy this repo already ruled for exactly this shape. | fix-session-candidate |
| D7-4 | Citation resolution across the window's own new surfaces | `state.md`'s 16 paths, 8 gate namespaces and 4 rulings anchors all resolve (D1-3). The 20 CI freshness paths all exist. The nine new rulings rows' cited ADRs all exist. | Clean. | None. | close-as-fine |
| D7-5 | Items outside every register, named by the prompt: corpus-player slices (chartered ADR-0014, never a row), the NIST licensing send, the guide's palgebra chapter rulings, `#intake-staging-dir`'s deferred trigger | `roadmap.md` carries `EXTERNAL **[nist-licensing]**` (the send is rowed) and `DEFERRED (trigger: none recorded -- ADR-0144 finding F-6)` for the intake staging dir — **a Deferred row explicitly carrying "no trigger", which ADR-0144 F-6 raised and left for the author**. The corpus-player slices and the guide's palgebra chapter rulings have no row. | The staging-dir row is the sharpest: the roadmap's own contract requires a Deferred row to state a revisit trigger, and this row satisfies the grammar by *declaring the absence*. That is honest and it is not a trigger — the row cannot ever fire. | The staging-dir trigger is an **author ruling** (F-6 asked for it 2026-08-17 and it is still open). Corpus-player slices: a row or an explicit retirement. | ruling-needed |

**Dimension 7 register summary:** 5 rows. **2 close-as-fine, 1
fix-session-candidate, 1 ruling-needed, 1 intake.**

**Dimension 7 verdict: YELLOW (unchanged).** The watch-list mechanism
worked — ten of twelve rows moved, and C-4's fix is better than what
was asked for. It stays yellow because the dimension's own signature
defect recurred: two ruled items (D1-9/D1-10) aged through an arc close
with no register row, which is precisely the pairing-as-data precedent
this dimension exists to catch, and C-3's backlog grew by half again
while rowed.

---

## Dimension 8 — Operator experience

Probes run: 4 of a 12 budget. `out/` was cleared before the CLI probes,
per ADR-0140's own stale-`out/` incident class.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D8-1 | **The fence battery's surviving row, re-measured** (D8-5's handed-on figure: "56 bare of 74 command fences", ADR-0140 R-F8) | `bin/fence-census` at this tip: **103 files in scope, 228 fenced blocks, 26 command/exercised, 50 command/bare, 30 output, 122 other** (sum 228, closed). So the figure is now **50 bare of 76 command fences (65.8%)**, against ADR-0140's 56 of 74 (75.7%). | **The number moved, and it moved the right way**: six fences gained exercisers (ADR-0148's and ADR-0149's rows) while two new command fences appeared. Measured, not carried. | None — this row exists to supply the ruling in D8-2 with a current figure. | close-as-fine |
| D8-2 | The figure R-F8's **proposed rule** actually needs: bare fences **on the reader path** | Reader path = `README.md`, `SETUP.md`, `docs/manual/**`, `docs/use-cases/**`. **38 bare, 8 exercised.** Decomposed: README 1, SETUP 3, manual 21 (04-time-on-the-wire 4, 02-setup-first-corpus 4, 08-your-own-data 3, 05-batch-delivery 3, 07-judging 2, 06-breaking-data 2, 03-a-simulated-hospital 2, 01-what-this-is 1), use-cases 13. | R-F8's proposed default is *"every fence a reader meets on the README / SETUP / manual / use-case path is exercised … the census can gate bare-fence-count-on-reader-path = 0"*. **That rule costs 38 fences**, most of them in the user manual, several of which need a primed artifact cache to run. ADR-0140 said "only a ruling can say whether it is the right one" and could state the number; this is the number. | **Author ruling** (the plan offers it as lettered options, including a tiered variant that gates README+SETUP at 0 first — 4 fences — and phases the manual). | ruling-needed |
| D8-3 | The bare-invocation / unknown-flag / missing-file matrix against the built CLI | Bare `bin/ehrt` → usage, **exit 0**. `gate fhir --nope` → `{:status :error, :category :unknown-flag, :payload {:flag "--nope", :verb "gate fhir"}}`, exit 2. `frobnicate` → `:category :unknown-command` with all 9 `:valid-options` and `:hint "run: ehrt help"`, exit 2. `gate fhir /nonexistent.json` → `:category :file-not-found, :payload {:path ...}`, exit 2. | **Clean.** Every error carries a category and names its artifact; the unknown-command case carries a recovery route. Bare-invocation exit 0 is the status quo review 3 closed as fine (D8-2 there) and nothing has changed. | None. | close-as-fine |
| D8-4 | Help at 40 / 80 / 120 columns | `COLUMNS=40/80/120 bin/ehrt help`: **0 lines exceed the width at any of the three**. Default max line length 80. | **Clean** — help genuinely re-wraps to `COLUMNS` rather than assuming 80. | None. | close-as-fine |

**Dimension 8 register summary:** 4 rows. **3 close-as-fine, 0
fix-session-candidate, 1 ruling-needed, 0 intake.**

**Dimension 8 verdict: YELLOW (unchanged), but for the first time on
evidence rather than on an unrun probe.** Review 3 held D8 yellow
explicitly because D8-5 had never executed; it has now executed
(ADR-0140) and this review re-measured its survivor rather than
repeating it. The CLI's error surface and help are clean at every probe
run. What holds the dimension at yellow is D8-2: 38 of the 46 command
fences a stranger actually meets are unexercised, and the rule that
would fix it is unruled.

---

## Sub-agent line L-1 — Oracle coverage

Charter: what `bin/regression-oracle`'s 35 roots actually witness; the
per-root coverage matrix; the "IDENTICAL is vacuous here" set;
`digest.clj`'s docstring claims checked against measurement.

**Coordinator re-derivation.** The coordinator ran its **own** 35-root
pre-digest in its own clone (`clojure -Sdeps <the deps block copied from
`bin/regression-oracle` `run_one`> -M:oracle-run -m ehrt.oracle.digest
$OUT`, exit 0, 35 `.edn` files, sha256 manifest kept as this run's
artifact) and re-derived the load-bearing claims from those bytes
independently of the sub-agent's instrumentation.

| id | probe | evidence | finding | recommendation | disposition | provenance |
|---|---|---|---|---|---|---|
| L1-1 | Does any oracle root fire a `:bed-ready` transfer or reach ladder rung 3? | Coordinator's own digest, all 35 roots: `grep -o ':bed-ready true'` → **exactly 1**, in `death-fixture`; `:event :transfer` → **exactly 1**, same root. Independent ladder classification (home-ward vs location ward and placement, over every admission/transfer in all 35 `.edn`): **rung 1 = 48** (`total-joint-replacement-engine`), **rung 2 = 381** (9 roots), **rung 3 = 13** (`death-fixture`), **rung 4 / `:forced` / `:exhausted` = 0**. The full transfer event, extracted verbatim: `{:home-ward "Emergency", :bed-ready true, ... :from {:ward "Cardiology", :bed "CARDIOLOGY-02", :placement :licensed}, :placement :surge, :location {:ward "Emergency", :bed "ED-H02", :placement :surge}, :forced false}` — preceded by a `:discharge` vacating exactly `{:ward "Emergency", :bed "ED-H02", :placement :surge}`. `config.clj:41` gives the Emergency ward `:beds 0 :surge-slots 6`. | **The standing "the oracle is blind to capacity pressure" claim is FALSE as stated, and ADR-0153's own stated reason for IDENTICAL is wrong about the only event it describes.** ADR-0153:245 says it holds because "a vacated LICENSED bed is handed over exactly as before"; the oracle's one bed-ready transfer hands over a **surge** bed, so the `(= :surge (:placement vacated-location))` half of the new guard evaluates TRUE on it. IDENTICAL holds for a stronger, structural reason the ADR did not give: the ED has `:beds 0`, so `home-licensed-free?` is identically false for every ED boarder, and the ED is where the oracle's only bed-ready transfer lives. ADR-0153 explicitly flagged "a right answer for the wrong reason is worth catching once" — and then gave a second wrong reason. | A dated addendum on ADR-0153 replacing that sentence with the structural reason. Restate the coverage claim as **thin, not zero**: 1 bed-ready / 13 rung-3 / 0 rung-4 / 0 forced / 0 exhausted, every capacity witness one root deep. | ruling-needed; **FIXED ADR-0156 (2026-08-19)** | **RE-DERIVED** (coordinator's own digest and classifier; sub-agent's rung counts reproduced to the event: 48 / 381 / 13 / 0) |
| L1-2 | The vacuous set — surfaces no root can move | Sub-agent's instrumented reachability battery over 78 named surfaces, all 35 roots, whose output was proven **byte-identical to the clean digest** (that identity is the battery's own soundness check). Reports 0 invocations for 9 of 22 `decide` dispatches, 8 of 21 `evolve` dispatches, 8 event kinds, `orm-message`/`oru-message`/`bed-swap-message`/`merge-message`, `obx-segment`, `mrg-segment`, the Z-segment pair, `plan-latency`/`emit-wire`, `v2-replay/fold-message`, `churn/inject`+`strip`, and `engine/replay`. Coordinator re-derived the **structural cause** rather than the instrumentation: all 35 roots pass `:pathway {:name "module-only" :steps []}`, and 11 of 18 components plus `bases/cli` are off the oracle classpath entirely (`comm` of `ls components` against the deps block in `bin/regression-oracle`). | The vacuous set is real and large: the entire churn family, the whole order→result path, `:transfer`/`:bed-swap`/`:merge`, every site-profile override, the second clock (`plan-latency`/`emit-wire`), and `sim-check` in its entirety are outside what any `IDENTICAL` verdict can mean. | A `COVERAGE` section in `digest.clj`'s own docstring naming the vacuous set, plus (proposed) a gate asserting the witnessed kind set equals a committed list, so widening or narrowing coverage forces the claim to move. | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** | **RE-DERIVED in part** — the coordinator reproduced the structural cause (module-only pathway on all 35 roots; 11 of 18 components off-classpath) and the bed-ready/transfer/rung counts from its own digest, but did **not** re-run the 78-surface instrumentation. The per-surface zero counts are sub-agent-witnessed. |
| L1-3 | ADR-0150 (a) said Z-segments are outside the oracle — is that the whole story? | `digest.clj:171` is the sole emitter call and is the **five-arg** arity, so `site-profile` is nil at all four bind points (MSH dialect, `:patient-class` table, `:discharge-disposition` table, Z-segments). Coordinator confirmed the five-arg call site directly. Sub-agent's battery adds that `effective-msh` (4,940) and `code-for` (5,381) ARE invoked — on their nil-profile branch only. | ADR-0150 (a) is correct but named only the Z-segment quarter of the surface. The oracle witnesses the **absent-profile identity** and nothing else: `default-msh` and the standard code tables are inside it, every override branch is outside it. | Generalise the note where ADR-0150 (a) put it: any site-profile milestone must nominate a different witness up front. | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** | **RE-DERIVED** (five-arg call site at `digest.clj:171` confirmed in the coordinator's clone) |
| L1-4 | Does the oracle's soundness check cover `digest.clj`'s `(:require ...)` form? | Coordinator ran `bin/regression-oracle`'s own `digest_body_of` (`awk 'found{print} /^\(defn/{found=1; print}'`) against the live file: **593-line file → 524-line soundness body; 4 requires in the file, 0 surviving into the body; the `roots` map DOES survive** (so a root added or removed IS caught). | The soundness check excludes the entire `(ns …)` form, which the script header calls "(docstring + requires)". Requires are not inert: `digest.clj`'s own docstring records that the standing-equipment promotion repointed every require from implementation to interface namespaces, changing `run-walk`'s call from 6-arg to 8-arg — exactly the change class the check exists to stop, and exactly the class it cannot see. **`rulings.md#R-oracle-script-contract` says the script "aborts on an undeclared digest-source diff", which overstates what it does.** | Either widen the diff to the whole file minus the docstring, or amend the rule's text and the script header to say a `:require`/`:import` change is an undeclared digest-source change that must be asserted with `--declared-digest-change`. | ruling-needed; **FIXED ADR-0156 (2026-08-19)** | **RE-DERIVED** (coordinator ran `digest_body_of` itself; 593/524/4/0 reproduced exactly) |
| L1-5 | Does `digest.clj`'s docstring account for its own roots? | `digest.clj:45` opens `Six roots, matching this session's own J1 ruling verbatim`; the dated notes add three (ADR-0033) and two (ADR-0042) and stop, totalling 11. The map holds **35** (coordinator's own count from the map, and 35 `.edn` files written). The other 24 are documented only in `;;` comments inside the body. | Not a false claim — each note is dated and scoped — but a reader who stops at the docstring gets 11 roots, six of them presented as the whole set. Compounded by L1-4: the docstring is outside the soundness body, so it can drift without any gate noticing. | One current-state paragraph at the head of the docstring, gated against `(count roots)`. | fix-session-candidate; **FIXED ADR-0156 (2026-08-19)** | **RE-DERIVED** (line 45 confirmed; 35 roots confirmed from the map and from the digest run) |
| L1-6 | Is `interpreter-batch`'s "enough real content" claim true per root? | Sub-agent: `appendicitis` **185 of 200 walks empty**, 124 events total; `sore-throat` 194/200 non-empty; `ear-infections` 156/200. On the engine side `dermatitis` and `rheumatoid-arthritis` render **1** HL7 message each across 300 patients; `allergic-rhinitis` renders 4 from 3,000 patients. | The docstring's "enough real content … to make a silent behavior change unlikely to slip through undetected" is overstated for `appendicitis`, and eight roots' HL7 half is a 1-6 message assertion. Kind coverage is real; emitter witness density for those roots is close to nothing. | Record per-root witness density beside the root list so a session choosing a witness for an emitter change does not pick a 1-message root. Do not retune in passing — every retune is a declared oracle change. | intake | **RE-DERIVED in part** — coordinator confirmed 35 non-empty `.edn` files and the file-size spread from its own run; the per-root walk/message densities are sub-agent-witnessed. |
| L1-7 | Was ADR-0150's S-6 `:units`→`:unit` rename witnessable by the oracle? | Sub-agent's battery: `[decide :order]`, `[decide :result-followup]`, `[evolve :order-placed]`, `[evolve :result-available]`, `orm-message`, `oru-message`, `obx-segment` all **0**. ADR-0150 Step 3's own named edit sites are `engine.clj:637` (inside `decide :order`) and `emit_hl7.clj:646,654` (inside `obx-segment`). Coordinator confirmed independently that **no root emits `ORM^O01`** — matching ADR-0142's own finding of zero ORM across all 35. | Both of S-6's edit sites are in the vacuous set. ADR-0150 wrote a careful structural vacuity argument for its Z-segment step and none for Step 3, where the same reasoning applied equally. No false claim was made (its Receipts report no oracle run for Step 3), but the reasoning was not carried across. | Fold into L1-2's coverage statement: name `:order-placed` / `:result-available` / `ORM^O01` / `obx-segment` as an explicit no-witness family. | intake | **RE-DERIVED in part** — the ORM-is-zero half is coordinator-confirmed and independently corroborated by ADR-0142; the per-surface zero counts are sub-agent-witnessed. |
| L1-8 | Module breadth and determinism | Sub-agent: every one of the 31 vendored module JSONs is referenced by ≥1 root (`comm` diff empty); all 35 `.edn` non-empty; two independent full runs produce identical sha256 manifests. Coordinator: own run produced 35 non-empty `.edn` and a 35-line sha256 manifest, kept as this review's artifact. | **The oracle's breadth over vendored content is complete and its determinism is real.** Every gap this review found is in engine/emitter *path* coverage, never in module coverage. | None. | close-as-fine | **RE-DERIVED** (coordinator's own 35-root run and manifest) |
| L1-9 | `bin/regression-oracle --help` | Coordinator ran it: `bin/regression-oracle: line 50: positional[1]: usage: bin/regression-oracle <baseline-ref> <target-ref> [--declared-digest-change]`, **exit 1**. | `--help` is consumed as a positional ref; the usage text surfaces only as a bash parameter-expansion error with a `line 50:` prefix. The text is right, the channel is wrong. | A `--help`/`-h` branch printing the header's Usage plus one line on what a regression-oracle claim means. | intake | **RE-DERIVED** (coordinator ran it) |

**L-1 summary:** 9 rows. **1 close-as-fine, 3 fix-session-candidate, 2
ruling-needed, 3 intake.** Provenance: **6 fully re-derived**, **3
re-derived in part** (structural cause and headline counts reproduced by
the coordinator; the 78-surface instrumentation is sub-agent-witnessed
and labelled as such). **0 could-not-reproduce.**

**The line's headline:** the charter was written on three prior findings
and **two of them turn out to be wrong on their specifics.** The oracle
is not blind to the capacity path — it witnesses one bed-ready transfer
and 13 rung-3 placements, all in `death-fixture` — and ADR-0153's stated
reason for IDENTICAL is wrong about the only event it describes. The
correction makes the underlying advice *stronger*, not weaker: every
capacity witness the oracle has is one root deep, so `death-fixture` is
a single point of failure for `:transfer`, `ADT^A02`, `:bed-ready` and
rung 3 simultaneously.

---

## Sub-agent line L-2 — Exit-code / harness truthfulness

Charter: every place a session's or CI's exit code passes through a
pipe, wrapper, subshell, `tee`, `|| true`, background job or make
recipe; whether the true exit survives; the ADR-0152 instance verbatim.

**Population** (sub-agent, from the tree): 1 Makefile, 2 `.githooks/*`,
2 workflows, 24 bash `bin/*`, 6 python `bin/*`, 4 skill `*.sh` in three
byte-identical copies, plus the skill `.md` files that teach gate
commands. Shell-option census: **15** `bin/*` run `set -uo pipefail`
**without `-e`**; 6 run `set -euo pipefail`; the Makefile sets no
`SHELL`/`.SHELLFLAGS`/`.IGNORE`/`.ONESHELL` and carries **zero**
`-`-prefixed recipe lines.

| id | probe | evidence | finding | recommendation | disposition | provenance |
|---|---|---|---|---|---|---|
| L2-1 | Does the taught exit-code law reach the shape that actually masked ADR-0152? | The law is stated on four surfaces (`build-session/SKILL.md:90-94`, `HISTORY.md:287-295`, `state.md:100-105`, `rulings.md#R-full-suite-before-push`) and all four forbid **a pipe or `tail` on the gate command**. ADR-0152's mask was in the **wrapper's last command**: `echo "MAKE_EXIT=$MAKE_EXIT" \| tee …`. Coordinator reproduced the shape: a script doing `false > /dev/null 2>&1; MAKE_EXIT=$?; echo "MAKE_EXIT=$MAKE_EXIT" \| tee /dev/null` prints `MAKE_EXIT=1` and **exits 0**; appending `exit "$MAKE_EXIT"` makes it exit 1. `git grep -niE 'MAKE_EXIT\|masked' -- '*test*.clj'` → no gate. | **Review 3's H-2/H-3 watch item fired exactly as written — a NEW way to mask an exit code.** The law was obeyed and the exit was still masked, because the law governs the gate command and the mask migrated to the wrapper. | One clause on the law's four surfaces: a wrapper that captures `MAKE_EXIT` must END with `exit "$MAKE_EXIT"`; the exit code the harness reports is the wrapper's last command, not the gate's. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator reproduced both the masking and the fix) |
| L2-2 | Does any tracked instruction file *teach* the masking shape? | `.agents/skills/extraction-stage/SKILL.md:95` **and** its byte-identical `.claude/` mirror teach: *"capture … full log and exit code directly (`> file 2>&1; echo EXITCODE:$?`)"*. Coordinator ran it: `printf 'false > /tmp/x.log 2>&1; echo EXITCODE:$?\n' \| bash` prints `EXITCODE:1` and the **block exits 0**. | A skill whose purpose is un-masked verification teaches the exact idiom whose block-level exit is 0. The sibling `errata-sweep/SKILL.md` states the same rule in prose without a masking example and is fine. | Replace with `> file 2>&1; EXITCODE=$?; …; exit "$EXITCODE"` in both mirrors. **The highest-value single edit this review found.** | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator confirmed both mirrors at :95 and ran the idiom) |
| L2-3 | Does `bin/preflight` tell the truth when its CI query fails? | `bin/preflight:73-77` sets `runs_out=""` on a failed `gh run list`; `:80` `if [ -n "$runs_out" ]` is then false, so `any_red`/`any_pending` stay 0 and `:99-101`'s `else` prints **`OK: last five runs all green (or none found)`**. Coordinator read the code path directly and confirmed the fallthrough. | **A failed CI query renders as a green CI report**, in the one script whose Step-0 job is to establish CI colour — and `build-session/SKILL.md` instructs every session to run it and disclose its findings. **Nuance the coordinator adds, and the sub-agent understated:** the script DOES print `FAIL: gh run list failed:` and the error text immediately before the OK line, so the output is self-contradictory rather than silently false. The defect is that the summary line — the one a reader quotes — says OK. **No test covers `bin/preflight` at all.** | On the failure path set a `ci_unknown` flag and print `UNKNOWN: CI status could not be determined` instead of falling through to the OK branch. Add a test; there is none. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator read `bin/preflight:70-102` and confirmed the branch structure) |
| L2-4 | Is `bin/preflight`'s exit code a claim? | `bin/preflight:162` is an unconditional `exit 0`, reached after any `FINDING:` or `FAIL:` — coordinator confirmed by reading `:155-162`. Sub-agent additionally drove it with a `gh` shim: a red run → `FINDING: a red … run appears`, exit 0; an untracked file → `FINDING: working tree is not clean`, exit 0. | Every other ceremony script in `bin/` (`tag-ceremony`, `post-push-verify` checks 1-2, `verify-nist-lock`, `close-scaffold`) is fail-closed. `preflight`'s always-zero exit is undocumented, so the asymmetry invites a caller to trust `$?`. | **Author ruling:** either declare the exit code non-load-bearing in the script header AND in `build-session/SKILL.md`, or make it exit non-zero on any `FINDING:`/`FAIL:`. Do not leave it undeclared. | ruling-needed; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator confirmed the unconditional `exit 0`; the shim-driven FINDING cases are sub-agent-witnessed) |
| L2-5 | Does the `use-cases` target's for-loop propagate a converter failure? | `Makefile:90-92` is `@for f in target/use-cases/*.txt; do python3 …; done` — coordinator read it; a for-loop's exit is its **last iteration's**. Sub-agent ran it with one of the 22 `.txt` emptied: converter printed `No equations found`, **loop exit 0**. Backstop: `write-use-cases!` slurps the missing `.mermaid` and throws → `make use-cases` red. **Residual hole the sub-agent proved:** with a **stale** `.mermaid` already in `target/`, loop exit 0 **and** `write-use-cases!` exit 0 — the stale diagram is silently reused, and `Makefile:88` is `mkdir -p` only, never a clean. | **The only masking construct in the Makefile.** Survival is CONDITIONAL and the backstop is incidental (a `slurp` that happens to throw), not designed. CI's fresh checkout closes the stale path; the exposed case is a **local** `make docsgen` reporting success while a converter failed. | One line: either `python3 … \|\| exit 1` inside the loop, or `rm -rf target/use-cases` before the `mkdir -p`. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED in part** — coordinator confirmed the loop construct and its exit semantics; the emptied-input and stale-`.mermaid` experiments are sub-agent-witnessed. |
| L2-6 | Does `bin/post-push-verify` carry the CI verdict in its exit code? | Coordinator read `:136-158`: check 3 captures `gh run list … 2>&1` into `run_line`, prints `status=… conclusion=…`, discloses "reported once, not awaited to conclusion (AR-CI-4)", and the script ends `exit 0` unconditionally. Sub-agent drove it with shims: a `conclusion=failure` at the pushed tip → exit 0; a `gh` exiting 1 → the error text is folded INTO the status field as `status=error: HTTP 401: Bad credentials conclusion=<pending>`, exit 0. | The always-zero exit is **deliberate and documented** per AR-CI-4, so this is weaker than L2-3. The sharp half is the `2>&1` capture: a broken `gh` renders as a plausible-looking status line that skims as "pending". Checks 1 and 2 are correctly fail-closed, and the ADR-0138 range fix works (a fresh clone with no reflog refuses with exit 2). | Ruling on whether check 3 is advisory (say so in the header and the skill) or a verdict. At minimum, detect a non-zero `gh` and print `UNKNOWN:` rather than folding stderr into the status field. | ruling-needed; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator read the whole of check 3 and the unconditional `exit 0`) |
| L2-7 | Can a tree-clean postcondition report clean when it could not measure? | **8** sites, all `dirty="$(git status --porcelain)"` then `if [ -n "$dirty" ]`: `quickstart-demo:109`, `demo-exerciser-ed-tuesday:182`, `demo-exerciser-clinic-decade:150`, `readme-what-you-get:86`, `usecase-acceptance-qa:69`, `usecase-custom-emitter:159`, `usecase-judge-tier-calibration:60`, `usecase-regression-baselining:49`. A `$(...)` failure yields an empty string, which the test reads as "clean". | Structurally the same class as L2-3, one severity down. `git status` in the repo root realistically does not fail, so this is latent, not live. | If L2-4 is taken up, fold in: `dirty="$(git status --porcelain)" \|\| fail "git status failed"`. Eight identical one-line edits. | intake | **RE-DERIVED in part** — coordinator confirmed the 8 sites and the idiom by grep; the failing-`git` experiment is sub-agent-witnessed. |
| L2-8 | What does `set -uo pipefail` without `-e` cost? | 15 scripts, 10 ending in a bare `echo "== … =="`. Sub-agent traced every unguarded top-level command in the ten: seven harmless `rm -rf out/…`, `regen-traces`'s writes into a `mktemp -d` (backstopped by `materialize! \|\| fail`), and the L2-7 `dirty=` assignments. Everything else routes through `expect`/`expect_eval`/`\|\| fail`. | **The absence of `-e` is deliberate and correct** — `bin/quickstart-demo:11-15` explains it: `gate fhir`'s taught rejection is exit 1 *by design*, and `set -e` would abort on it. No live defect. The cost is an unenforced invariant: every command in these ten must route through a wrapper or its failure is absorbed while the trailing echo still claims success. | No fix. State the invariant in the `expect` header block of the family so the shape's cost is written down rather than rediscovered. | close-as-fine | **RE-DERIVED in part** — coordinator confirmed the counts by grep; the per-script command tracing is sub-agent-witnessed. |
| L2-9 | Does `bin/close-scaffold` report a write it did not make? | `close-scaffold:149-164`: `create_stub` does `printf … > "$path"` unguarded then `echo "CREATED: $path"`, so the function returns **echo's** 0; the two call sites at `:184-185` do not check it. `regenerate_indexes` at `:173-186` IS correctly guarded (`\|\| exit 1`). | A failed stub write is reported as `CREATED:` and the script proceeds to exit 0. Low exposure (both target directories always exist), same claim-without-check class. | `printf … > "$path" \|\| { echo "FAIL: …" >&2; return 1; }` plus `\|\| exit 1` at the two call sites. Ride-along. | intake | **RE-DERIVED in part** — sub-agent-witnessed line cites; coordinator did not re-read this file. |
| L2-10 | Do the wrapper comments describe the mechanism the code uses? | Coordinator ran `git grep -n tee -- bin` and read `demo-exerciser-ed-tuesday:47-60`. Six comment sites in four files (`demo-exerciser-ed-tuesday:39,47`, `demo-exerciser-clinic-decade:47`, `readme-what-you-get:38`, `usecase-custom-emitter:35,52`) say the wrapper **"tees"** stdout/stderr. The code does `"$@" > "$LOG_DIR/step-$STEP.out" 2> …err; local got=$?` then `cat` — a **redirect**, then a replay. | **The code is right and the comments name the exact wrong mechanism** — the one ADR-0152 was burnt by. An editor "fixing" the code to match its comment would reintroduce the defect into five gate scripts that run under `make integration` and `make quickstart`. | Reword the six sites to "captures … by redirect, never `tee` — `tee` returns its own status (ADR-0152)". Cheap, and it converts a trap into a teaching line. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator ran the grep and read the wrapper body) |
| L2-11 | H-2's re-probe, re-derived rather than re-read | Coordinator ran the pattern itself over the whole tree: no live gate command piped into `tee`/`tail`/`head` anywhere. The only `\| tee` matches are **prose** in `notes/adr/0152-*.md:206` and its session record; the only `\| tail` matches are prose in ADR-0135 and review 3's own register. | **Review 3's H-2 finding still holds by re-derivation: the tracked tree carries no pipe-masked gate command.** The defect class lives entirely in session/harness practice and in the one taught idiom at L2-2. | None for the tree. Keep the probe — it is cheap, and it is why ADR-0152's recurrence is attributable to practice rather than to a tracked file. | close-as-fine | **RE-DERIVED** (coordinator ran the greps) |
| L2-12 | Are the hooks, both workflows, and the `set -euo pipefail` scripts fail-closed? | Sub-agent: both hooks exit 1 on every failure branch with no pipes and no `\|\| true`; neither workflow contains a pipeline at all; GHA's default shell is `bash -e {0}`, verified locally to abort a multi-line `run:` on first failure; the 6 `set -euo pipefail` scripts exit non-zero explicitly; 4 scripts end in `exec`, passing the callee's status through; `trap … EXIT` in 6 scripts proven not to overwrite the status. Coordinator confirmed the Makefile half independently (no `-` prefix, no `.IGNORE`/`SHELL`/`.SHELLFLAGS`/`.ONESHELL`). | **Clean. Every irreversibility gate in the repo is fail-closed.** | None. | close-as-fine | **RE-DERIVED in part** — coordinator confirmed the Makefile semantics; hooks/workflow/trap experiments are sub-agent-witnessed. |

**L-2 summary:** 12 rows. **3 close-as-fine, 5 fix-session-candidate, 2
ruling-needed, 2 intake.** Provenance: **7 fully re-derived**, **5
re-derived in part** (construct and line cites confirmed by the
coordinator; shim-driven and experiment-driven observations
sub-agent-witnessed and labelled). **0 could-not-reproduce.**

**The line's headline:** the watch item fired. Review 3 said to watch
for a *new* way to mask an exit code rather than for the old defect, and
there is one — the wrapper's own last command, which all four surfaces
stating the law leave uncovered — plus a tracked skill that teaches the
shape (L2-2) and a preflight script that renders a failed CI query as a
green CI report (L2-3).

**Sub-agent's own disclosed limits, carried forward verbatim:** real CI
behaviour of the `test.yml` multi-line `run:` block was not observed
(the row rests on the documented GHA default plus a local `bash -e`
experiment); the literal ADR-0152 wrapper text is not recoverable from
the tree; and whether `preflight`'s always-zero exit is intentional is
undeterminable from the tree, which is why L2-4 is `ruling-needed`.

---

## Sub-agent line L-3 — Generated-surface completeness

Charter: for every GENERATED artifact, what inputs move it (from its
generator's source, not its header), whether every input is tracked,
whether the inputs are documented at the artifact, and whether any
generated file's header makes a claim its generator does not enforce.

**Method note worth carrying:** the sub-agent enumerated the population
**three ways and diffed them** — (E1) a tree-first banner scan of every
tracked file's first three lines, 35 genuine hits; (E2) the make graph
measured **by running it** and taking an mtime snapshot around a second
`make docsgen`, **52 tracked files written**; (E3) CI's `git diff
--exit-code` list, 19 paths expanding to 63 tracked files. It also
established the baseline this whole review rests on independently:
`make docsgen` at `4d6ff78` exits 0 in **2:48**, leaves
`git status --porcelain` **empty**, and is idempotent across two runs —
so the tree is byte-fresh against its own generators.

**An independent cross-check of this review's own Step 0:** L-3 measured
**348 blocks / 3,960 tests / 17,758 assertions** in its own clone,
matching the coordinator's Step-0 baseline exactly, from a different
clone on a different lane invocation.

| id | probe | evidence | finding | recommendation | disposition | provenance |
|---|---|---|---|---|---|---|
| L3-1 | Is the workflow's own rule — *"a new derived file goes on a make target AND on the diff list, same commit"* — mechanical? | Sub-agent **sabotaged both surfaces**: removed `operators-doc` and `palgebra-examples` from `docsgen:` and deleted `docs/operators.md` + the three palgebra `.mermaid` from CI's diff list, then ran the real per-push lane → **`SUITE_EXIT=0`, 348/3,960/17,758, 0 failures**. Coordinator re-derived the mechanism independently: only **two** tests assert a docsgen prerequisite at all — `sim_theory_head_hop_test:175` (`docsgen-depends-on-the-sim-theory-target-test`) and `traces_fresh_test:149` (`docsgen-depends-on-the-traces-target-test`). | **The rule is prose with no closure gate.** 2 of 12 leaves and 2 of 19 diff-list paths are asserted; the other ten leaves and seventeen paths can be silently dropped from either surface with every gate green. **This is the class ADR-0136 found for five artifacts and that ADR-0149 and ADR-0152 then each closed for exactly one more artifact — the class itself was never closed.** It is the sharpest instance in this review of the pattern the scoreboard names. | One gate that enumerates docsgen's actual write set (from a run, or from the recipes' output paths) and asserts set-equality with the diff list, replacing the two per-artifact assertions. | ruling-needed; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED in part** — coordinator confirmed that exactly two docsgen-prerequisite assertions exist, which is the finding's mechanism; the sabotage run is sub-agent-witnessed. |
| L3-2 | Does anything enforce `event-schema-baseline.edn`'s header claim that it is *"deliberately NOT on `make docsgen`"*? | Sub-agent put `event-schema-freeze` on `docsgen`'s prerequisites and ran the gates that would care: **42 tests, 171 assertions, 0 failures**. Coordinator re-derived: `event-schema-freeze` appears in the test tree **only inside failure-message strings** (`event_schema_test.clj:132,137`), never as an assertion about docsgen's prerequisites. | **The ADR-0152 class — a header asserting a mechanism nothing runs — on the highest-stakes artifact in the repo.** `non-additive-change-requires-a-version-bump` is the only gate that can force a version bump, and its entire non-vacuity rests on the baseline being frozen. One word added to the `docsgen:` line makes the gate compare the schema against itself **forever**, with nothing red — and because the baseline is (correctly) off the CI diff list, nothing would be diffed either. | A two-line test asserting `event-schema-freeze` ∉ prerequisites of `docsgen`, beside `docsgen-depends-on-the-traces-target-test`, whose helper already exists. **The cheapest high-value fix in this review.** | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator confirmed the absence of any assertion) |
| L3-3 | The third undocumented-mover class of `state-derived.md` | Coordinator re-derived the population directly from `.agents/reading-sets.edn`: **26 distinct paths**, every one a line-counted input to `state-derived.md` — seven component `interface.clj` files, `AGENTS.md`, seven `docs/dev/*.md`, five READMEs, `state.md`, `rulings.md`, `roadmap.md`, `build-session/SKILL.md`. Sub-agent proved the movement by appending one line to `corpus/interface.clj` and regenerating: `:corpus 1801 → 1802`. | ADR-0143 documented "adding an ADR moves it"; ADR-0152 *discovered* "adding a test namespace moves it". **The real generalisation is far wider and still unwritten: a line added to any of 26 files moves it**, as does `oracle/digest.clj`, the modules `NOTICE`, and the shape of `components/`, `bases/`, `projects/`. The artifact's banner says "the live tree", which is a *category*, so no session can predict the mover — and both prior sessions found out as a pre-push red. | Have the renderer emit its own input list into the page (it already holds every reading set's `:paths` and all the directory roots). A generated enumeration cannot go stale, and a session grepping for the file it just edited finds the answer before running the suite. | fix-session-candidate | **RE-DERIVED** (coordinator re-derived the 26-path population; the append-and-regenerate demonstration is sub-agent-witnessed) |
| L3-4 | A generated artifact that is an input to another generated artifact | Coordinator confirmed `docs/dev/pipeline.md` **is** a member of the `:corpus` reading set, hence a line-counted input to `state-derived.md` — a generated → generated edge. Sub-agent proved the consequence: serial, in Makefile order, `1801 → 1802` correctly; **`make -j8 pipeline state-derived` produced a STALE `state-derived.md` against a changed `pipeline.md`, twice.** | The edge exists **only as the left-to-right order of `docsgen:`'s prerequisite list** — `state-derived:` declares no prerequisites and no test asserts the ordering. Contrast `the-sim-theory-target-writes-the-equations-file-first-test`, which asserts exactly this property *within* a recipe; the discipline is absent *between* targets. CI is safe today only because it runs serial `make docsgen`, and nothing says it must. | Declare `state-derived: pipeline`, or add the inter-target ordering assertion in the shape `sim_theory_head_hop_test` already ships; and record the generated→generated edge at both artifacts. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED in part** — coordinator confirmed the dependency edge exists (`docs/dev/pipeline.md` ∈ reading-set paths); the `-j8` race is sub-agent-witnessed. |
| L3-5 | Does `docs/formats.md`'s generated block name the inputs that actually move it? | Coordinator read both: `event_log_doc.clj:213-214` slurps **two** artifacts (`schema-path` AND `examples-path`); the banner at `docs/formats.md:357` reads *"Generated by `make docsgen` from components/sim-engine/resources/sim-engine/event-schema.edn"* — **one input**. Sub-agent proved the second is a mover by editing an MRN in `event-examples.edn` and regenerating: `docs/formats.md | 1 +, 1 -`. | A reader or session tracing why `formats.md` moved is pointed at the wrong file — and `event-examples.edn` supplies every rendered example on the page. The banner also names the umbrella target rather than the leaf (`make formats-event-log`). | Name both inputs and the leaf target in `event_log_doc/render`'s banner string. One-line fix. | fix-session-candidate | **RE-DERIVED** (coordinator read the generator's two slurps and the page's one-input banner) |
| L3-6 | How many artifacts does the python converter move, and is it named at any of them? | Sub-agent: the converter produces bytes in **28** artifacts (`docs/dev/pipeline.md`, 22 use-case pages, `sim-theory-diagram.{mermaid,md}`, 3 palgebra `.mermaid`) and is named at **1** of them, and then only indirectly ("the string-diagram skill"). | **This is the ADR-0135 incident's exact shape**: a converter change moved every one of these, and the three palgebra examples were missed entirely because nothing at the artifact pointed back at the converter. The banners say "from `<x>.edn`", which is half the derivation. | Every banner over converter-rendered content names the converter alongside the `.edn`, so a converter change's blast radius is greppable from the artifacts. | fix-session-candidate | **RE-DERIVED in part** — the 3-of-5 palgebra hardcoding and zero test coverage are coordinator-confirmed (L3-10); the 28-artifact count is sub-agent-witnessed. |
| L3-7 | Do all docsgen outputs carry a generated marker? | Sub-agent's E1 ∖ E2 diff: **19 docsgen-written files carry no marker in their first three lines** — the four `.mermaid`, all 14 `demos/traces/**` derived files, and `docs/formats.md` (whose marker is at line 355, *inside* the region). | **The enumeration a future reviewer reaches for first — grep for "GENERATED" — under-counts the generated surface by ~36%**, and the two largest under-counted groups are exactly the two that have already drifted (ADR-0135 for the `.mermaid`, ADR-0142/0149 for the traces). Mermaid supports `%%` comments and the converter already emits them. | Give the converter a `%%` banner (verify the ADR-0135/0152 arrow-renumbering hazard is unaffected — the banner sits above `flowchart`, not in the equations file); add a per-directory note for `demos/traces/`. | fix-session-candidate | **RE-DERIVED in part** — coordinator independently found 68 banner-carrying files against 12 docsgen leaves in its own D5 probe, corroborating the direction; the exact 19-file diff is sub-agent-witnessed. |
| L3-8 | Does the agent-facing instruction surface enumerate the generated files? | Coordinator read `AGENTS.md:253-257`: the "**GENERATED, never hand-edited**" bullet names **four** files — `notes/ADRs.md`, `.agents/state-derived.md`, and the two record `INDEX.md`. Docsgen writes **53**. | **The primary instruction surface for AI agents in this repo tells a cold session that 4 files are generated when 53 are.** Every one of the omitted 49 — `docs/cli.md`, `docs/operators.md`, `docs/dev/pipeline.md`, `docs/use-cases.md` + 22 pages, `formats.md`'s region, 4 `.mermaid`, the equations file, both event-schema `.edn`, 14 `demos/traces/**` — is a file an agent could plausibly hand-edit. | Replace the hand list with a pointer to a generated one; the natural home is a section of `state-derived.md` rendered from the diff list or the recipes — which also gives L3-1 its population for free. | fix-session-candidate | **RE-DERIVED** (coordinator read the AGENTS.md bullet and counted its four entries) |
| L3-9 | Do the generators prune? | Coordinator read `usecases.clj:296-301`: `write-use-cases!` does `.mkdirs` then `doseq … spit` — **it never cleans `pages-dir`** — and `grep 'pages-dir\|orphan'` over `usecases_test.clj` returns nothing. Sub-agent planted an orphan page, ran `make use-cases`, and the orphan survived; 71 tests / 529 assertions still green. | If a case is dropped from `use-cases.edn`, its committed page persists: the index loses the row, `make use-cases` leaves the page untouched, `git diff --exit-code docs/use-cases/` sees **no change**, and nothing asserts the page set equals the case set. **A generated page would outlive its own source, indefinitely, green.** | A page-set closure test (`set(docs/use-cases/*.md) == set(case ids)`), the same shape `traces_fresh_test`'s population-closure assertion already uses. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator read the generator and confirmed no pruning and no closure test) |
| L3-10 | Is the `palgebra-examples` population closed? | Coordinator listed the directory: **5** `*-equations.txt` (`ai-study`, `committee`, `decision-monad`, `deliberated-choice`, `lemon-pie`) and **3** committed `*.mermaid`. Test references to `components/palgebra/examples` across the whole test tree: **0**. The recipe hardcodes three converter invocations, and the Makefile comment states the rule in prose: *"if a fourth example grows a committed `.mermaid`, it belongs on this target and in CI's freshness diff the same day."* | Nothing checks it. A `.mermaid` for `lemon-pie` or `decision-monad` added tomorrow lands in exactly the ungated state ADR-0136 found these three in — already stale against their own converter. | Derive the target's population from the directory (`*-equations.txt` with a sibling `.mermaid`) rather than hardcoding three lines, or assert the pair set. | fix-session-candidate; **FIXED ADR-0155 (2026-08-19)** | **RE-DERIVED** (coordinator listed the directory and ran the zero-test-reference grep) |
| L3-11 | Are the traces documented at the artifact? | Sub-agent: `make traces` appears in **3 of 7** per-trace READMEs (all inside errata footnotes) and **0 times** in `demos/traces/README.md`, the tree's own front door; **none of the 14 derived files carries any marker**. | The largest generated tree in the repo, and the slowest docsgen leaf, is the least self-documenting — and it is the surface that drifted twice (ADR-0142, ADR-0149) precisely for want of an at-the-artifact pointer. | One sentence in `demos/traces/README.md` naming `make traces` and `bin/regen-traces`, and the same in the four READMEs that lack it. | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed the 14 derived files carry no marker via its own D5 banner enumeration; the per-README counts are sub-agent-witnessed. |
| L3-12 | `config-latency.edn` claims byte-identity with `config.edn` below its header — true, and enforced? | Sub-agent diffed the bodies: **the claim is currently TRUE**. Grep for consumers finds only `bin/demo-exerciser-ed-tuesday` and a citation-shape mention; **no test, no target**. | A 70-line hand-maintained duplicate whose header states an invariant nothing checks — the same *"header asserts a mechanism no one runs"* shape as `sim-theory.edn`. Honest today, with no way of staying so. | A one-assertion test (`body(config.edn) + :latency == body(config-latency.edn)`), or generate the variant from the base. | intake | **RE-DERIVED in part** — sub-agent-witnessed diff and grep; coordinator did not re-read these two files. |
| L3-13 | Hand-derived VALUES inside `docs/manual/` | Sub-agent: **8 pasted 64-hex sha256 digests** across `docs/manual/{04,06,08}*.md`, and five hand-authored SVGs embedding concrete live data (`two-clocks.svg` carries MRN000013, `EVN-2 2026-08-11T03:36:00Z`, seed 20260811). No test, no target references `manual/assets`; `citation_gate` covers strip **provenance**, never a **value**. | A derived-value class outside all three enumerations. **ADR-0151 had to re-witness `d00bf49c…` by hand precisely because nothing re-derives it — and its prediction that the digest would move was WRONG**, settled only by a manual re-run. | Design pass. Cheapest partial fix: register the digests as pinned values in a test that re-runs the named scenario at integration tier. **Scope note so nothing is counted twice: this row is tallied for the EIGHT PASTED DIGESTS only.** The five SVGs it also names are counted once, at D5-2, which carries them as ruling-needed; L3-13 corroborates that row rather than adding to it. | intake | **RE-DERIVED in part** — coordinator independently found the same five SVG banners and the absent gate in D5-2; the 8 digests are sub-agent-witnessed. |
| L3-14 | Every `.edn` with a schema/header claim — is the claim enforced? (generalising ADR-0152) | Sub-agent swept all **75** tracked `.edn`; **25** carry a schema/validation claim, and **every one has an enforcing mechanism**: a `committed-*-is-valid-test` (pipeline, use-cases, sim-theory), validate-or-throw at load (`pairing-registry`, `exercised-sources`), re-derivation from the source of truth (`taxonomy.edn`), a ratchet test (`reading-sets`), or `bin/verify-nist-lock` (`artifacts.lock.edn`). | **CLEAN — ADR-0152's `.edn` class is closed**, with the single exception of L3-2, which is a *freeze* claim rather than a schema claim. Worth recording: the **generators** still don't validate (`read-signature-edn` is a bare `edn/read-string`), so the first signal of an invalid source is a suite red, never the generator. | None beyond L3-2. | close-as-fine | **RE-DERIVED in part** — coordinator confirmed the `sim-theory` half (ADR-0152's own fix) in D5; the 75-file sweep is sub-agent-witnessed. |
| L3-15 | Is the freshness list complete against what docsgen actually WRITES? (measured by running, not by reading the Makefile) | Sub-agent's E2 ⊆ E3 check: **zero uncovered** — all 52 docsgen-written tracked files fall under the 19 diff-list paths. Two consecutive `make docsgen` runs left `git status --porcelain` empty. Coordinator independently mapped all **12** docsgen leaves onto the 20-path list in D5-1 and found no leaf unlisted. | **CLEAN.** The list is complete *today*, docsgen is idempotent, and the tree at `4d6ff78` is byte-fresh. Recorded so the author can distinguish "the current contents are right" (they are) from "nothing keeps them right" (L3-1). | None. | close-as-fine | **RE-DERIVED** (coordinator's own D5-1 leaf-to-path mapping reaches the same verdict by a different route) |
| L3-16 | Derived or mirrored surfaces outside all three enumerations | Sub-agent enumerated nine: `.agents/reading-sets-baseline.edn`, two `sim-v2-*-baseline.edn`, `pre-split-baseline.edn`, `pinned_seed_42_patients_5.edn`, `test-fixtures/custom-emitter/seed42-p5-encounters.jsonl` (byte-diffed **at integration tier only**), `.claude/skills/**` (59 files), and `event-schema-baseline.edn`. | **No correctness gap — each is gated.** The gap is *registration*: there is no single place a session can read to learn "these files are derived, and here is what re-derives each", and three are regenerable only by a prose recipe. `.claude/skills/**` is documented as mirrored with **no recipe named anywhere**, so a session that trips the mirror test has to invent the `cp`. | Fold into whatever register L3-1/L3-8 produces, with a "gated by" column; add the mirror's one-line recipe beside its rule. | intake | **RE-DERIVED in part** — coordinator independently verified the `.claude/skills` mirror and its gate in D2-3/D2-4; the other eight are sub-agent-witnessed. |

**L-3 summary:** 16 rows. **2 close-as-fine, 10 fix-session-candidate,
1 ruling-needed, 3 intake.** Provenance: **7 fully re-derived**, **9
re-derived in part** (mechanism and population confirmed by the
coordinator; sabotage runs, mtime snapshots and the 75-file `.edn` sweep
sub-agent-witnessed and labelled). **0 could-not-reproduce.**

**The line's headline:** the freshness list is complete **today** and
nothing keeps it that way. The workflow's own rule — *"a new derived
file goes on a make target AND on the diff list, same commit"* — is
prose with two per-artifact assertions behind it, so ten of twelve
docsgen leaves and seventeen of nineteen diff-list paths can be dropped
with the whole suite green (L3-1), and the freeze that makes the
schema-version gate non-vacuous is protected by a header sentence and
nothing else (L3-2). The class ADR-0136 opened has been closed one
artifact at a time, three times, and never closed as a class.

**Sub-agent's own disclosed limits, carried forward verbatim:** whether
`make -j docsgen` misorders leaves other than `pipeline`→`state-derived`
was not established (one edge proven twice; no full parallel run);
whether any leaf writes outside the repo root was not established (the
mtime probe covered the working tree only); and its suite baseline was
measured under its own sabotage, which removes no test — the
coordinator's independent clean run at the same tip returns the same
348 / 3,960 / 17,758.

---

## Scoreboard — reviews 1, 2, 3, 4

| dimension | review 1 (2026-08-07) | review 2 (2026-08-09) | review 3 (2026-08-15) | **review 4 (2026-08-18)** | movement |
|---|---|---|---|---|---|
| D1 — Claim-reality coherence | GREEN | GREEN | YELLOW | **GREEN** | **improved** — the widened scan root is gated and found no recurrence; all 11 sampled `state-derived.md` claims re-derive and the file regenerates byte-identically |
| D2 — Guard coverage | YELLOW | YELLOW | YELLOW | **RED** | **regressed** — mirror guard complete, but the docsgen/diff-list closure rule has assertions for 2 of 12 leaves (L3-1) and the freeze protecting the only schema-change gate is enforced by a header sentence (L3-2); plus a law with no gate (D2-1) |
| D3 — Environment independence | YELLOW | YELLOW | YELLOW | **YELLOW** | unchanged in colour, **transformed in content** — the three-hit class's root cause is named and its fix verified safe; the "lost" cold-clone probe turns out to be `make ci-parity` |
| D4 — Error honesty | RED | GREEN | GREEN | **GREEN** | unchanged — catches honest, CLI categories complete; one lint-coverage gap (`.mkdirs`/`.delete`) with no live mis-report behind it |
| D5 — Mirror and derivation drift | GREEN | GREEN | RED | **YELLOW** | **improved** — review 3's five unregistered derivations all registered and gated, plus two more registered on this window's own initiative; six hand-regenerated surfaces remain unwatched |
| D6 — Sampling adequacy | YELLOW | YELLOW | GREEN | **YELLOW** | **regressed** — the 150-trial invariant-catalog defspec samples a facility that cannot express the precondition of the one engine defect this window found |
| D7 — Continuity integrity | GREEN | YELLOW | YELLOW | **YELLOW** | unchanged — 10 of 12 watch-list rows moved and C-4's fix beat its recommendation; two ruled items aged an arc with no row, C-3's backlog grew 44 → 66 |
| D8 — Operator experience | GREEN | YELLOW | YELLOW | **YELLOW** | unchanged in colour, **first time on evidence** — D8-5 executed (ADR-0140) and its survivor re-measured; CLI and help clean at every probe |

**Overall: review 1 was 4 green / 3 yellow / 1 red. Review 2 was 3 green
/ 5 yellow / 0 red. Review 3 was 2 green / 5 yellow / 1 red. Review 4 is
2 green / 5 yellow / 1 red.**

**The red moved rather than cleared, and that is the honest picture.** D5 came off red because every one of review 3's unregistered
derivations was registered and gated, and the sessions that did it went
looking for more on their own initiative (ADR-0149's `demos/traces/**`,
ADR-0152's `sim-theory-equations.txt`, the second of which found a live
invalid `.edn` in the process). D1 came off yellow because the widened
scan root is now a standing gate rather than a one-time sweep. **D2 went
red in their place, and it is the same debt one level up:** the
artifacts are all fresh — verified, twice, byte-for-byte — and what is
missing is anything that keeps them so as a class rather than one
registration at a time. What went
the other way is D6, on a single finding that is worth the regression:
the repo's broadest correctness property samples a configuration that
structurally excludes the branch it vouches for.

**The cross-dimension pattern, and it is the same one review 3 named.**
Review 3's thesis was "a probe, gate, or tool whose population is a
registry rather than the tree". This review found five more instances,
and the shape has *shifted one level up*: it is no longer registries
standing in for trees, it is **a gate's population standing in for the
class it is believed to enforce**.

- `io_vocabulary_lint`'s forbidden set is 3 calls; `R-io-result-or-loud`
  is a rule about I/O that can fail (D4-1).
- The exit-code law forbids a pipe on the *gate command*; the class is
  any construct that determines the reported exit (L2-1).
- `R-audience-has-entry-path` has no gate at all (D2-1).
- The invariant-catalog defspec's *facility* is fixed while its name
  claims every m1 run (D6-1).
- `R-oracle-script-contract` claims the script aborts on an undeclared
  digest-source diff; the script cannot see a `:require` change (L1-4).

Each of these is green against its own stated population, and each is
narrower than the law it is read as enforcing. **That is the finding
this review would put in front of the author above all others**, and it
is why three of the six proposed fix sessions co-land a *widened* gate
rather than a new one.

---

## Register summary

**72 total rows.** Per section, counted mechanically from every row
whose first cell is a `D<n>-<id>` or `L<n>-<id>` label, disposition read
from the disposition cell:

| section | rows | close-as-fine | fix-session-candidate | ruling-needed | intake | cross-ref |
|---|---:|---:|---:|---:|---:|---:|
| D1 — Claim-reality coherence | 5 | 4 | 1 | 0 | 0 | 0 |
| D2 — Guard coverage | 6 | 1 | 3 | 1 | 0 | 1 |
| D3 — Environment independence | 4 | 2 | 2 | 0 | 0 | 0 |
| D4 — Error honesty | 4 | 2 | 1 | 1 | 0 | 0 |
| D5 — Mirror and derivation drift | 4 | 3 | 0 | 1 | 0 | 0 |
| D6 — Sampling adequacy | 3 | 2 | 1 | 0 | 0 | 0 |
| D7 — Continuity integrity | 5 | 2 | 1 | 1 | 1 | 0 |
| D8 — Operator experience | 4 | 3 | 0 | 1 | 0 | 0 |
| L-1 — Oracle coverage | 9 | 1 | 3 | 2 | 3 | 0 |
| L-2 — Exit-code truthfulness | 12 | 3 | 5 | 2 | 2 | 0 |
| L-3 — Generated-surface completeness | 16 | 2 | 10 | 1 | 3 | 0 |
| **Total** | **72** | **25** | **27** | **10** | **9** | **1** |

25 + 27 + 10 + 9 = **71** disposition-carrying rows, **+1**
cross-reference (D2-5, counted at L2-1) = **72**, matching the row count
exactly.

**Sub-agent provenance, counted the same way** (37 sub-agent rows):
**20 fully re-derived** by the coordinator in its own clone, **17
re-derived in part** (the finding's mechanism, population or cited
artifact confirmed by the coordinator; instrumentation runs, shim-driven
experiments and sabotage runs labelled as sub-agent-witnessed),
**0 recorded as "sub-agent claim, coordinator could not reproduce"**.

*Every figure in this section was produced by extraction from the rows
above, not by tallying while writing. Two of this document's own draft
summary lines were wrong before that extraction ran — the L-2 and L-3
provenance splits were stated inverted — and are corrected here rather
than left. **Review 5 should re-derive these figures from the rows
regardless, not trust this line**, and should read AR-RR4-1's method
note first: re-derive against the register as FIRST COMMITTED, because
this repo's fix sessions overwrite disposition cells in place.*

**Probes recorded as blocked, partial, or not run:** enumerated per
dimension in the plan's Part 4. **No dimension exhausted its 12-probe
budget** (the highest was D2 at 6), so nothing here was displaced by the
budget — the un-run probes are named because the rubric requires it, not
because the battery ran out. The two limits worth the author's eye are
the **full 113-row rulings→gate map** (never run by any review, and no
artifact in the repo holds one) and the **`docsgen` per-push tier
question** ADR-0149 left open, which this review **cannot answer**: its
suite ran under three-sub-agent contention at 1,273 s and the number is
not comparable.

**The single cross-dimension pattern, and it is review 3's own thesis
one level up.** Review 3 found five instances of "a probe, gate, or tool
whose population is a registry rather than the tree". This review found
five instances of the successor shape: **a gate whose population is
narrower than the class it is read as enforcing** — `io_vocabulary_lint`
vs `R-io-result-or-loud` (D4-1), the exit-code law vs the class of
constructs that determine a reported exit (L2-1), `R-audience-has-entry-path`
with no gate at all (D2-1), the invariant-catalog defspec's fixed
facility vs its "every m1 run" claim (D6-1), and
`R-oracle-script-contract`'s "aborts on an undeclared digest-source
diff" vs a check that cannot see a `:require` change (L1-4). **L3-1 is
the same shape at the level of the meta-rule**: the obligation that a
new derived file joins both a make target and the diff list has closure
assertions for 2 of 12 leaves, and the class has been closed one
artifact at a time, three times, without ever being closed as a class.

Three of the six proposed fix sessions therefore co-land a **widened**
gate rather than a new one. That is the plan's central bet.

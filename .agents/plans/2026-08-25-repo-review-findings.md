# Repo review 5 — the fifth assessment (findings register)

Findings-only register for the `repo-review` skill's fifth survey
(`.agents/skills/repo-review/SKILL.md`, steps 1-4). This is an
ASSESSMENT, not a fix session: every row is a PROPOSED recommendation,
and this session executes none of them. Per the author's ruling of
2026-08-18, carried unchanged ("Q1 c, Q2 register and separate fix
session"), the fix sessions are separate and follow the author's
rulings on the plan.

Row format matches reviews 1-4: `id | probe | evidence | finding |
recommendation | disposition`. Disposition in {ruling-needed,
fix-session-candidate, close-as-fine, intake}. Every row's evidence was
gathered by the mechanism the rubric names for its own dimension —
re-derive, re-hash, re-run, never re-read a claim as its own
verification.

Landed 2026-08-25 against tip `f05f51a` (the arc-0 record's sha
correction; tree clean; no Step-0 mutation of any kind).

**CHARTERED FIVE ADRs EARLY, AS AN AUTHOR OVERRIDE.**
`roadmap.md#repo-review-5` charters this review at "approximately
ADR-0174", and `notes/adr/0159-review-4-arc-close.md:466-485` worked
that arithmetic explicitly, correcting the design channel's own
~ADR-0169 figure and recording the correction "so the next session does
not average them". This run lands at **ADR-0170**. That is an author
ruling of 2026-08-25, recorded here as an OVERRIDE of
`rulings.md#R-review-cadence-in-adrs` — **not** as compliance with it,
and not as a re-derivation of the cadence. The window under review is
therefore **ten** ADRs (0160-0169), not the ~fifteen the rule specifies.
Per the rule's own text, review 6's due point is computed from THIS
close. The override is itself a register row (L2-14) and a plan item.

The prior assessment (`.agents/plans/2026-08-18-repo-review-findings.md`,
review 4, `notes/adr/0159-review-4-arc-close.md`) is this review's
baseline. Its scoreboard is carried forward below, and its
**thirteen-row** inherited watch-list (ADR-0159:374-402, the thirteen rows themselves at :381-393) is re-derived
row by row in D7.

**Shape — author-ruled (Q1 "c", HYBRID), carried from review 4.** The
coordinating session ran the eight-dimension battery itself under a
probe budget of at most 12 probes per dimension (96 cap), and dispatched
three sub-agents, one per line this window opened (L-1 gate vacuity, L-2
the premise-correction ledger, L-3 measurement discipline), each in its
own fresh clone of `f05f51a` with no probe cap. **Sub-agent rows are
transcript-witnessed until the coordinator re-derives at least one cited
artifact per finding in its own clone.** Every sub-agent row below
carries its provenance explicitly: `RE-DERIVED` (the coordinator
reproduced it), `RE-DERIVED in part`, or `COORDINATOR COULD NOT
REPRODUCE` (recorded, never dropped, never promoted).

---

## Step 0 — record

| act | evidence |
|---|---|
| Edit root | Verified-clean ext4 tree at `/home/mg/src/ehr-testing-tools`, tip `f05f51a`, `git status --porcelain=v1 --untracked-files=all` EMPTY. The skill permits "fresh clone **or** verified-clean ext4 tree at the design-channel-verified tip"; the three sub-agents each took their own fresh clone of `f05f51a` and every sub-agent claim was re-derived here. |
| Preflight | `bin/preflight`, exit 0, no findings. Last five CI runs on `main`: `f05f51a` **in_progress** (run `32828026389`, DISCLOSED not counted red, AR-CI-4), `d49f1c6` green (Integration), `d49f1c6` green, `ff45ad1` green, `7c1dfa5` green (Integration). Edit root not under `/mnt/`; `core.fileMode` **true**; `core.ignorecase` unset; tree clean including untracked; local HEAD == `origin/main`; last `stable-*` tag `stable-20260821-patient-simulator-charter` @ `6ce2160`; **HEAD untagged**. |
| Arc mid-flight? | **No.** ADR-0169 closed in its own session (`.agents/session-records/2026-08-25-arc-0-performance-under-equivalence.md`), and its commits are pushed at `f05f51a`. Arc 1 (stream-partition design) is chartered as a roadmap row (`#stream-partition-design` P26) but has no session. The sequencing gate the skill names is satisfied. |
| **Tag ledger — DISCLOSED, not paid** | Newest tag `stable-20260821-patient-simulator-charter` (ADR-0162, `6ce2160`). **ADR-0163 through ADR-0169 carry NO tag** — seven ADRs. Enumerated from `git log` rather than from the ADR numbering, that is **THREE tagless arc closes**, not the two the prompt's own ledger implies: `68af03b` (2026-08-23, the unpaired end-step / citation-scope arc, ADR-0163/0164), `7c1dfa5` (2026-08-23, the generator-side coverage / care-plan-end arc, ADR-0165/0166), and `4772e73` (2026-08-25, **arc 0**, ADR-0169 — whose pushed tip is `f05f51a` after two follow-ups). Recorded as a premise correction against this session's own prompt. `rulings.md#R-arc-closes-in-own-session` says an arc closes "in its own session **with its own tag**". Two arc closes in this window are tagless. Disposition proposed in the plan; **this session pays no tag** (author ruling: tags remain the author's). |
| Baseline suite | See "Suite baseline" below. Taken **after** the three sub-agents finished, deliberately: review 4's own Step-0 suite ran at 21m13s under three-sub-agent contention and its register records the figure as not comparable. Deferring the timed run is this session's disclosed deviation from the prompt's Step-0 ordering, and the reason is ADR-0167's own lesson. |
| Host-side health record (ADR-0167 convention) | Sampled immediately before the timed run, at the moment of the figure. Windows `LoadPercentage` **1 / 4 / 3**; five `wslhost.exe` present, largest cumulative CPU **1.11 s** — no orphan (ADR-0167's was 68.7 CPU-**hours** across six threads). Linux `uptime` 1-min load **0.20**, 12 logical CPUs, up 14h00m. |
| Reading sets | From the generated `state-derived.md`, not from prose: `:corpus` 1836/2045, `:docs` 743/785, `:judge` 926/1000, `:onboarding` 1496/1530, `:sim` 1278/1405. **`:onboarding` headroom 34 lines** — tightest of five for the third review running (32 → 132 → 46 → **34**). W-13 is live; `R-budget-stop` applies to this session's own close. |
| Mutations at Step 0 | **None.** `make state-derived` and `make docsgen` were run as D1/D5 probes and left the tree byte-identical (`git status --porcelain -uall` empty after each); `out/` was cleared before and after every run. No rubric amendment, no ruling, no row. |

---

## AR-RR5-1 — Prior arithmetic re-derivation (skill step 4's standing sub-step)

Re-derived **mechanically from review 4's register as FIRST COMMITTED**
(`git show 0a07195:.agents/plans/2026-08-18-repo-review-findings.md`),
per that register's own method note — this repo's fix sessions overwrite
disposition cells in place, so the live file is not the review-day
document. Extraction: every table row whose first cell is a
`D<n>-<id>`/`L<n>-<id>` label; disposition read as the cell that exactly
equals one of the four tokens (the L-rows carry a seventh provenance
column, so "last cell" is the wrong rule and was not used).

| section | rows | close-as-fine | fix-session-candidate | ruling-needed | intake | cross-ref |
|---|---:|---:|---:|---:|---:|---:|
| D1 | 5 | 4 | 1 | 0 | 0 | 0 |
| D2 | 6 | 1 | 3 | 1 | 0 | 1 |
| D3 | 4 | 2 | 2 | 0 | 0 | 0 |
| D4 | 4 | 2 | 1 | 1 | 0 | 0 |
| D5 | 4 | 3 | 0 | 1 | 0 | 0 |
| D6 | 3 | 2 | 1 | 0 | 0 | 0 |
| D7 | 5 | 2 | 1 | 1 | 1 | 0 |
| D8 | 4 | 3 | 0 | 1 | 0 | 0 |
| L-1 | 9 | 1 | 3 | 2 | 3 | 0 |
| L-2 | 12 | 3 | 5 | 2 | 2 | 0 |
| L-3 | 16 | 2 | 10 | 1 | 3 | 0 |
| **Total** | **72** | **25** | **27** | **10** | **9** | **1** |

**Every cell reproduces review 4's own summary table exactly**, and the
totals reproduce its 72 / 25 / 27 / 10 / 9 / 1. The single UNMATCHED row
is `D2-5`, which is review 4's declared cross-reference to L2-1 — the
extraction found it independently rather than being told about it.

**Sub-agent provenance, re-derived the same way** (37 L-rows, last
non-empty cell): **20 fully `RE-DERIVED`**, **17 `RE-DERIVED in part`**,
**0 "coordinator could not reproduce"**. Matches review 4's 20/17/0.

**Verdict: review 4's register arithmetic is CORRECT in every figure.**
This is the first time this standing sub-step has come back clean —
review 1's own summary was off (44/26/6 claimed against a direct 45/28/5,
corrected at ADR-0078 AR-RL-R), which is why the step exists. Recorded as
a negative result the next review inherits: the mechanical-extraction
discipline review 4 adopted ("produced by extraction from the rows above,
not by tallying while writing") demonstrably works, and review 5 has
adopted it for its own summary below.

---

## AR-RR5-2 — History scan, window ADR-0160 → ADR-0169 (skill step 2)

Ten ADRs and eleven session records, 2026-08-20 → 2026-08-25, all read
at heading depth with their Deviations / findings / error-ledger /
"worth your attention" sections in full. Population enumerated from the
tree (`ls notes/adr/016*.md` → exactly ten; `ls .agents/session-records/
| grep -E '2026-08-2[0-5]'` → exactly eleven), not from either INDEX.

| incident class | hits this window | dimension | severity effect |
|---|---|---|---|
| **A gate green over a population that cannot exhibit its failure** | **FOUR** — ADR-0165's meter found the ADR-0163 fix had blinded the suite to both end types; arc 0 found two of its own gated-corpus gates near-vacuous before landing them; `digest.clj:570-588`'s own vacuous-set note; and (NEW, L1-1) the two ADR-0163 gates are STILL vacuous at HEAD | D2/D6 | **repeat hit — raises D2 and D6** |
| **The machine, not the tree** | ONE (ADR-0167). No tree-side hypothesis was acted on before the host was sampled: the session eliminated JDK, content, filesystem, Defender, `.wslconfig` and memory FIRST, then sampled Windows. Order was correct. | D3/D4 | no raise |
| **Diagnosis before trace** | ONE (ADR-0163: the channel's proposed mechanism — the unscoped decide-time scan — was overturned by the session's own trace, which found `:order-citation nil` so the scan was never reached). Caught by the session's step-2 trace gate. | D6 | no raise; the prompt structure worked |
| **A premise asserted without a probe (unearned specificity)** | **FORTY**, enumerated in L-2's ledger | D1/D7 | **repeat hit — raises D1** |
| **A figure quoted at a precision or status its record does not support** | **SIX** (L3-1, L3-2, L3-3, L3-7, L3-8, L3-9) | D1 | **repeat hit — raises D1** |
| **A citation that does not resolve in the artifact it names** | **THREE** (roadmap → "ADR-0169 F-1/F-2/F-3", which ADR-0169 does not contain; `#register-gate-row-ownership` → a defect no longer in `roadmap.md`; ≥9 of 12 `engine.clj:NNN` cites in a live onboarding doc) | D1/D7 | **repeat hit** |

**Seeds the prompt named, each verified rather than carried:**

- *"The second-suite-covers-the-close pattern."* Arc 0 ran `make test`
  twice (14m35s / 14m17s). `R-full-suite-before-push` binds a **push**,
  and arc 0 pushed once, at the end — so one run over the close commit
  would have satisfied the rule. Running it twice was *stronger* than
  the rule, not an over-read of it, and the session's own reason is
  stated ("a docs-only change can still break a docs-tooling gate"),
  which is a real hazard this repo has hit. Rowed as D2-6 for a ruling
  that makes the practice explicit either way.
- *"The memory-summary-supersedes-record pattern."* Re-derived, and the
  premise needs correcting: **the word "superseded" appears nowhere in
  `.agents/plans/2026-08-24-traffic-scale-program.md`** (`grep -n supersed`
  → no hits) and nowhere in `.agents/rulings.md` either (`grep -c -iE
  "immutab|supersession"` → **0**). What the plan actually does is place
  the post-arc-0 MEASURED row (`:195-218`) twenty-five lines BELOW the
  now-stale 2026-08-24 "where the time goes at 10^5" row (`:177-183`,
  "six of 29 invariants are 99.4%", check the larger quadratic) and leave
  the older row unmarked. Supersession by juxtaposition. A reader who
  reads the 08-24 row and stops takes a ranking arc 0 inverted as
  current MEASURED fact. The record was correctly left untouched — but
  no rule says so: `rulings.md#R-session-narrative-hierarchy` assigns the
  roles and says nothing about immutability or about how a live plan
  marks a superseded row. Rowed D5-5.
- *"The `## Done` count reported as 30 when the tree held 29."* Verified:
  `## Done` at `f05f51a` is its heading **plus 29 body lines**, cap 30,
  five rows. The session that reported 30 was counting the heading, which
  is exactly what `attic-rotation-test` counts ("`## Done` measured
  **exactly 30 lines including its header** -- the extent
  `ehrt.docs-tooling.attic-rotation-test` counts", traffic-scale record
  :128). **Not a figure asserted from memory** — two counting
  definitions, both defensible, one gated. Rowed D1-8, `R-ledger-counting-definition`
  is the existing law it should have cited.

---

## Dimension 1 — Claim-reality coherence

*8 probes run of 12. Population for every count probe enumerated from the
tree, never from the registry under audit.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D1-1** | Resolve every `ADR-016x F-n` citation on a live surface against the ADR it names | `roadmap.md:16` cites *"cannot ride either carrier without a second code path, **ADR-0169 F-3**"*; `roadmap.md:17` cites *"**ADR-0169 F-1/F-2**"*. `grep -nE 'F-[0-9]' notes/adr/0169-*.md` → **exit 1, no match**. The F-numbers live in `.agents/session-records/2026-08-25-arc-0-performance-under-equivalence.md:334, :344, :355`. | **The two highest-priority OPEN roadmap rows cite findings by label into an artifact that contains no such labels.** `citation-gate-test` gates strip provenance and `link-footnote-gate-test` gates ADR tokens in prose; **neither resolves a finding LABEL inside its cited ADR**. This is review 4's own W-3 class (an ADR that closes a row need not name it) inverted: a row that cites an ADR for a label the ADR does not carry. | Either move the F-rows into ADR-0169 (the natural home — it is the arc's ADR and carries no findings section at all), or re-cite the roadmap rows at the session record. A gate that resolves `ADR-NNNN <label>` against the named file is the durable fix and is the same shape `citation-gate` already has. | fix-session-candidate |
| **D1-2** | Count the arc-0 commits from the tree and compare against every surface that states a count | `git log --oneline d49f1c6..f05f51a \| wc -l` → **8**. `.agents/session-records/2026-08-25-arc-0-…:6` "**Five** commits, local only -- no push, no tag"; `:442` "**Seven** commits, local only -- no push, no tag"; `:477` "Local only -- **seven** commits, no push, no tag". The commit table (`:433-441`) lists seven and omits `f05f51a`, **the very commit whose subject line is "complete its commit table"**. All eight are pushed (`bin/preflight` check 4: HEAD == `origin/main`). | One counted figure with **three different values inside one file**, none of them the tree's 8, plus a "no push, no tag" claim that is false at HEAD for the push half. `f05f51a` fixed the table and left the header eight lines above it, and the two closing sentences, untouched. Independently found by the coordinator and by sub-agent line L-3 (L3-4). | Fix-forward the three counts and the push clause. Standing: a close record's headline commit count is DERIVED from `git log <base>..HEAD` at close time, never carried from the prompt — candidate `build-session` "Done when" box. | fix-session-candidate |
| **D1-3** | Regenerate the derived continuity register and diff | `rm -rf out; make state-derived` → `MAKE_EXIT=0`; `git status --porcelain=v1 --untracked-files=all` → **EMPTY**. Sampled counts re-derived independently against the tree: **29** invariants (catalog 25 + facility 2 + warmup 1 + order-profiles 1, `check.clj:803-850`); **83** `(defspec` forms; **3** `fence-exemptions.edn` rows; **59** files each side of the skills mirror; **31** OPEN roadmap rows; **11** session records in-window; `## Done` heading + 29 body lines. | **CLEAN.** `state-derived.md` is byte-fresh at `f05f51a` and every count sampled from it or from the ADRs re-derives against the tree. The one number that needed a reconciliation (D1-4) reconciles. | None. | close-as-fine |
| **D1-4** | Re-derive `AGENTS.md`'s generated-surface figure from the generated table it points at | `AGENTS.md:259` — *"`make docsgen` writes **53** tracked files, and the list is itself generated — read `.agents/state-derived.md`'s own `## Generated surface` section"*. Summing that table's own `tracked files` column: **63**. Reconciliation: `docs/use-cases/` 22 all generated; `demos/traces/` 24 tracked of which **14** derived, so 10 hand-owned (7 per-trace READMEs + 3 `config*.edn`, stated in the table's own preamble). 63 − 10 = **53**. | The figure is CORRECT, and the surface it cites renders neither it nor the subtraction. **The bullet that ADR-0158 rewrote precisely to stop hand-listing still hand-carries a number** — one that a reader following its own pointer cannot reproduce, and that any change to the trace directory's hand-owned files silently falsifies. | Render the written-file count in `state-derived.md` beside the tracked-file column (the renderer already distinguishes them in prose), and make `AGENTS.md` cite it instead of restating it. | fix-session-candidate |
| **D1-5** | Re-derive the "14 `engine/replay` calls" figure by the mechanism the phrase names | `grep -c '(engine/replay' check.clj`: **14 at `d49f1c6`** (all `for`-comprehension call sites), **12 at `f05f51a`** — 11 call sites + 1 docstring. Arc 0 collapsed six invariants onto the shared `fold-records` helper (`check.clj:233-241`), used at `:246, :288, :323, :364`. Folds per `check-all` = 10 direct + 4 via `fold-records` = **14**. | The figure survives, but **only under the fold reading**. `roadmap.md:12-13` states it as *"the **14 independent `engine/replay` calls** in `check.clj` (~40% of the **post-arc-0** 7.26 s check phase)"* — a pre-arc-0 call-site count attached to a post-arc-0 phase, on the repo's PRIORITY 1 open row. A grep re-derivation of the sentence as written returns 11. ADR-0169's own wording (*"14 separate full folds of the log per `check-all`"*) is the correct one. | Errata on `roadmap.md:12`: say "folds per `check-all`", not "calls in `check.clj`". One word. | fix-session-candidate |
| **D1-6** | W-13 — read the reading-set table in the generated register, at Step 0 and again after this session's own close | Step 0: `state-derived.md:82` → `\| :onboarding \| 10 \| 1496 \| 1530 \| 1530 \| **34** \|`. After this close: **1498 / 1530, headroom 32** — the two lines are this session's own star-bullets in `.agents/plans/README.md`, which is an `:onboarding` path (`reading-sets.edn:24`). Under budget, so `R-budget-stop` does not compel a compaction and none was taken. | **W-13 FIRED as written** ("under ~30 lines — expect to compact"): 34 is above the trigger by four lines, and `:onboarding` is tightest of five for the **third** consecutive review (32 → 132 → 46 → 34). `R-budget-stop` makes the bump unavailable, so any session touching one of the set's **ten** paths (`reading-sets.edn:22-31`) had ≤34 lines of room. **This session's own close spends two of them**, leaving 32 — the fourth consecutive review to hand its successor a smaller number. | Charter the `:onboarding` compaction as work, not as a per-session tax. Every review since review 3 has flagged it and every close has spent it further. | fix-session-candidate |
| **D1-7** | Enumerate the Verification sections the prompt's own D1 probe names, from the tree | `grep -l '^#\+ Verification' notes/adr/016*.md` → **NONE**. Corpus-wide: 74 of 167 ADRs; of the last thirty (0140-0169) only **four** (0140, 0143, 0146, 0159). The window's verification substance exists but under **ten different headings** — `Oracle sweep`, `Error ledger`, `Suite reconciliation`, `Close verification`, `Local green`, `Landing deltas`, `Red before green`, `Fences honoured`, `Fences honored`, `Regression shape`. ADR-0169 carries **none of them** and no suite, oracle or timing claim at all. | **A probe that enumerates "the ADRs' Verification sections" has an empty population, and would report a silent green.** This is the rubric's own R4-Q8(a) hazard realized inside this review's own charter. The substantive finding underneath: **the window's largest src change (ADR-0169, three site families) has an ADR with no verification section and no citation to the record that holds its figures** — sub-agent L-3 reached the same conclusion by a different route (L3-5). `build-session` step 14 sanctions figures living in the record; nothing requires the ADR to POINT at it. | Require an ADR that lands executable change to carry either a verification block or a one-line pointer to the record that does (`R-session-narrative-hierarchy` is the row to widen). Recorded also as an L-2 premise row against this session's own prompt. | ruling-needed |
| **D1-8** | Re-derive the `## Done` line count under both counting definitions | `awk '/^## Done/,0' roadmap.md \| wc -l` → **30** (heading + 29 body). Cap stated in the heading: "at most 30 LINES". `attic_rotation_test` counts **including the header** — stated verbatim in the traffic-scale record `:128`. | **NOT a figure asserted from memory.** Two defensible counting definitions, one of them gated, differing by exactly one. `rulings.md#R-ledger-counting-definition` already requires "every parity or deftest ledger states which counting definition it uses"; the `## Done` cap does not state its own, and the session that reported 30 and the prompt that expected 29 are both right. | Add the counting definition to the `## Done` heading itself (it already carries the cap and the rotation rule), or widen `R-ledger-counting-definition` to cover capped registers. | fix-session-candidate |

---

## Dimension 2 — Guard coverage

*6 probes run of 12.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D2-1** | Map every `rulings.md` row ADDED in the window to its enforcing gate | `git diff 92d23bc..f05f51a -- .agents/rulings.md` → **12 new rows, +26 lines**. Gates: `R-done-attic-rotation` → `ehrt.docs-tooling.attic-rotation-test` (live, 7 tests / 16 assertions green). The other **eleven** — `R-mix-1` … `R-mix-7`, `R-skeleton-or-emission`, `R-per-person-streams-before-generator-fixes`, `R-output-identical-exempt-from-reshuffle-era` — have **no enforcing test** anywhere in the tree. | Eleven of twelve new standing rules are ungated, which the skill's own words make a finding ("a law with no gate is a finding"). They are, however, **doctrine rules about what future arcs may generate** (`R-mix-*`, `R-skeleton-or-emission`) and **sequencing/exemption rules about which session may run when** (`R-per-person-streams-…`, `R-output-identical-…`) — a category the register has never had to classify, and one that a `deftest` structurally cannot reach. | A ruling on the CATEGORY, not on the eleven rows: either `rulings.md` gains a declared "ungateable by construction" marker (so the census can subtract it and the remaining gap is real), or the review rubric stops counting doctrine rows against D2. Without one, every future traffic-scale ruling widens a gap that no session can close. | ruling-needed |
| **D2-2** | W-11 — diff `rulings.md` across the window for clauses that landed without attribution | `git diff 92d23bc..f05f51a -- .agents/rulings.md` is **append-only**: 12 added rows, **zero modifications to any existing row**. Every new row ends `-- ADR-016N`. | **W-11 did NOT fire.** ADR-0159's F-2 class (a widened row that names no ADR for the widening) had no opportunity to recur, because no row was widened. Recorded as a negative result review 6 inherits — the class is untested this window, not disproven. | None. | close-as-fine |
| **D2-3** | W-2 — count `fence-exemptions.edn` rows against ADR-0158's 3, and read each reason | 3 rows. `git log --oneline -- components/docs-tooling/resources/docs-tooling/fence-exemptions.edn` → only `3c4e346` and `ca02aa0`, **both ADR-0158**; untouched since. Reasons: `sudo apt install` mutates the checker's machine; a fresh network `git clone` "is not a property of this tree"; a `--board 60 --rate 60` fence that "is weeks of wallclock" run as taught. | **W-2 did NOT fire.** Exempt count unchanged at 3, and all three reasons describe **impossibility**, not inconvenience — exactly the discrimination the watch asks for. The `exempt` disposition has still gained no ratchet, so the gap the watch names is intact; nothing has exploited it. | Carry W-2 to review 6 unchanged. | close-as-fine |
| **D2-4** | W-1 — for every gate added ADR-0160..0169, was it born red, and where is the red dispositioned? | Born-red DEEPENED: ADR-0166 witnesses red in **both directions** (`:70-90` — neuter the reporting guard with `:when (and false …)`, all **seven** rejection tests fail and **both** acceptance tests still pass); ADR-0162's charter gate was born red **and its own red witness caught that the GATE was wrong** (`:173`); ADR-0165's coverage meter was born red by design and its first execution found the ADR-0163 hole. Arc 0 then adopted the **opposite** discipline: gates **BORN GREEN on the unrefactored tree** (ADR-0169 `:62-66`, "witnessed passing before it has anything to catch"). `rulings.md` carries `R-red-pushed-with-green` (`:211`) and **no row for either practice**. | **W-1 FIRED on its third branch** — "the practice has continued and now deserves a `rulings.md` row of its own". And it has BIFURCATED: this window established that a behaviour change owes red-first while a **pure refactor owes born-green-on-the-old-tree**, which is a stronger obligation and is stated only inside one ADR. No red was tuned away; no finding went unrowed. | Land both halves as one ruling row: red-first for new behaviour, born-green-on-the-pre-change-tree for an output-identical refactor, with ADR-0166 and ADR-0169 as the two worked examples. | ruling-needed |
| **D2-5** | Resolve the ruling every timed-suite figure in the window cites, against that ruling's text | `.agents/session-records/2026-08-25-arc-0-…:230-231`: *"Host verified quiet first (Windows `LoadPercentage` **1 / 4 / 4**, no orphan `wslhost`), **per `rulings.md#R-full-suite-before-push`**"*. That row (`rulings.md:270-272`) reads in full: *"a push is preceded by full `make test` unpiped with MAKE_EXIT recorded, and a wrapper capturing it ENDS with `exit "$MAKE_EXIT"`; `poly test brick:`/`project:` are aids, never the gate"*. **No host-quiet clause, no machine-state clause.** | The session did exactly the right thing and **cited a ruling that does not require it**. The obligation it was honouring is ADR-0167's amendment prose (`:187-189`, "a tracked suite figure on penny is worth only its host-side health record"), which has **zero standing surfaces** (L3-10, re-derived: `AGENTS.md`, `rulings.md`, `state.md`, both skills and `bin/preflight` all return 0 hits for `LoadPercentage\|host-side\|verified.quiet\|wslhost`). An unearned citation is what a discipline with no home looks like. | This is the Q4 ruling's strongest evidence. Whatever form Q4 takes, `R-full-suite-before-push` is the row that should carry the clause, so the citation the sessions are already making becomes true. | fix-session-candidate |
| **D2-6** | Does `R-full-suite-before-push` require the second suite arc 0 ran? | Arc 0 ran `make test` twice (`MAKE_EXIT=0` both; 875 s / 857 s; 370 / 4,166 / 18,690 identical). The rule binds **a push**; arc 0 pushed once, at the end. One run over the close commit satisfies it. | **The second suite is STRONGER than the rule, not an over-read of it** — and the session's stated reason ("a docs-only change can still break a docs-tooling gate") names a hazard this repo has hit repeatedly (ADR-0149's CI red, ADR-0159's own state-staleness tripwire). But nothing writes it down, so the next arc will re-decide it from scratch, and this session had to re-derive the answer to run its own close. | A ruling either way. Recommended: the close commit gets its own suite run when it touches any docsgen-gated or `.agents/` surface — which is every close — stated as a clause on `R-full-suite-before-push`. | ruling-needed |

---

## Dimension 3 — Environment independence

*4 probes run of 12.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D3-1** | W-9 — did any session in the window reason around a `FINDING:`/`UNKNOWN:` from `bin/preflight`? | Two of eleven ran to **exit 1**. `2026-08-20-oracle-coverage-integration-half.md:15` — one `FINDING:`, a red among the last five (`Integration` at `e967fd7c`, run `32344505291`), **which is the red the session came to fix**; disclosed, named by run id, not a stop. `2026-08-21-committee-skill-mojibake-repair.md:20` — two `FINDING:`s, a superseded historical red (`c44d240`, three green runs after it) and a dirty tree. Nine ran exit 0. **No `UNKNOWN:` occurred anywhere in the window.** | **W-9 FIRED, benignly.** Both non-zero exits were disclosed in full with the specific finding named and its reason stated; neither was read as green. `R-preflight-fail-closed` behaved exactly as ADR-0155 designed it — the exit code carried a claim and the sessions honoured it. | None on this axis. Carry the `UNKNOWN:` half to review 6 untested. | close-as-fine |
| **D3-2** | For each exit-1 preflight, WHEN in the session did it run relative to the session's own edits? | `2026-08-21-committee-skill-mojibake-repair.md:29-31`: the tree-clean FINDING is *"the three `committee/SKILL.md` copies, **this session's own Step 2 edits**. Expected: preflight ran **after** the repair was staged in the working tree, not before it."* | **NEW, and not on any watch-list.** `bin/preflight`'s check 3 exists to establish the STARTING state; the script has no notion of when it ran, so a session satisfies Step 0's disclosure obligation with a preflight taken after its own work. The disclosure was honest, and it is precisely the honesty that shows the check measured the wrong moment. Same shape as L3-10's finding one level down (preflight fires at the one moment least likely to be the moment of the figure). | Either have `bin/preflight` record and print the HEAD it ran at plus whether any tracked file's mtime post-dates the script's start, or state in `build-session` that a Step-0 preflight is void once the session has edited. Cheap either way. | fix-session-candidate |
| **D3-3** | Under the new "the prompts do the push" ruling, what does `bin/post-push-verify` assume about who pushed? | `bin/post-push-verify:9-18` derives its default base from **`origin/<branch>@{1}`** — the remote-tracking ref's own reflog — because `git push` fast-forwards that ref. Fallback `git merge-base <tip> origin/<branch>`, and it FAILS LOUDLY when neither is derivable (review-3 D1-6). | **CLEAN, and improved by the new ruling rather than threatened by it.** The reflog derivation is only exactly right when the push originated from this checkout, which the 2026-08-25 ruling now makes the standing case. Its documented fallback narrows the range if it does not — that residue is disclosed in the script's own comment and unchanged. | None. Re-probe at review 6 once several sessions have pushed under the new ruling. | close-as-fine |
| **D3-4** | The rubric's STANDING D3 probe — `make ci-parity` (real `git clone` into a scratch dir, `EHR_TESTING_TOOLS_CACHE` at an empty cache, `poly check` + `poly test :all skip:integration`) | See "ci-parity" under Suite baseline below. | Recorded there with its one stated limit, verbatim from the rubric: **it does not repoint `HOME`, so `~/.m2` is still shared — cold-cache parity, not a cold machine.** | — | close-as-fine |

---

## Dimension 4 — Error honesty

*3 probes run of 12. Population: the **16 `*/src/*.clj` files** changed
`92d23bc..f05f51a`, enumerated by `git diff --stat` and read in context,
not auto-flagged.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D4-1** | Read every new index read arc 0 added, and ask what a missing entry does | `engine.clj:1283-1293` (`reinstated-state`) and `:885-904` (`last-cited-index`) both branch on `(contains? world :<key>)`, **never on the entry**. In-source reason, verbatim: *"The fallback is on the key's presence, never on a missing entry: a world that `run` built and an entry that is nevertheless absent is a DEFECT, and letting it read nil (which changes the emitted event, which the byte-identity gate then fails) is the behaviour that surfaces it. Silently replaying instead would hide it."* Coverage gated by `reinstate-index-covers-every-reinstatable-event-and-nothing-else`. | **CLEAN, and it is the rubric's own doctrine applied without being asked.** A nil that would otherwise flow on silently is deliberately routed into a loud gate failure instead of a quiet fallback. Recorded as the window's positive control for D4. | None. (One narrow gap in the covering gate itself is rowed at L1-12/D6-5.) | close-as-fine |
| **D4-2** | Grep the window's added `src` lines for the rubric's four patterns (nil-returning I/O flowing on, `catch` without a category, silent caps, parses that default) | Three bare `(edn/read-string (slurp (io/resource "patient-simulator/…edn")))` in `patient_simulator/census.clj`/`gmf.clj`. `git diff --stat` shows these files under a **rename** (`sim_trajectory` → `patient_simulator`, ADR-0162): the code MOVED, it is not new. No new `catch`-without-category, no new silent cap, no new defaulting parse. | Residue, not regression — the same class review 4's L3-14 recorded ("the **generators** still don't validate; `read-signature-edn` is a bare `edn/read-string`, so the first signal of an invalid source is a suite red, never the generator"). A missing resource NPEs loudly; malformed EDN throws loudly. No silent absorption. | None new. The generator-validation class stands as review 4 left it. | close-as-fine |
| **D4-3** | Live CLI error matrix against the BUILT binary (not `clojure.test`) | `bin/ehrt corpus generate sim --nonesuch` → `{:status :error, :category :unknown-flag, :payload {:flag "--nonesuch", :verb "corpus generate"}}`, **exit 2**. `bin/ehrt show /nonexistent/file.hl7` → `{:category :gate-path-not-found, :payload {:path …, :hint "no such file or directory -- run: ehrt help show"}}`, **exit 2**. Bare `bin/ehrt` → usage, **exit 0** (ruled intentional: ADR-0060 AR-U2-4, executed and recorded at ADR-0064's close table). | **CLEAN.** Every failure names its artifact and its category; the missing-file hint names the remedy command. No error absorbed as an answer. | None. | close-as-fine |

---

## Dimension 5 — Mirror and derivation drift

*5 probes run of 12. "Every derived doc" enumerated from the tree first
(109 tracked files carrying a generation banner or `do not hand-edit`
marker, `git ls-files -z | xargs -0 grep -lI …`), then diffed against the
make graph's targets.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D5-1** | Regenerate every derived doc and compare | `rm -rf out; make docsgen` → **2m12s wall, `MAKE_EXIT=0`**, all twelve leaves including the slow `traces` leaf; `git status --porcelain=v1 --untracked-files=all` → **EMPTY**. | **CLEAN.** The whole generated surface is byte-fresh at `f05f51a` and `docsgen` is idempotent. | None. | close-as-fine |
| **D5-2** | Byte-diff the mirrored pair | `diff -r .agents/skills .claude/skills` → **IDENTICAL**; `find … -type f \| wc -l` → **59** each side. | **CLEAN**, and it reconciles with ADR-0159's own correction (59, not the 60 review 4's D2-3/D5-4 and its plan recorded). | None. | close-as-fine |
| **D5-3** | Do review 4's landed banner fixes still hold? | `docs/formats.md:357` names **both** inputs and the leaf target (`make formats-event-log`), the L3-5 fix. `AGENTS.md:259-265` points at `state-derived.md`'s generated `## Generated surface` section, the L3-8 fix, with its reason recorded in place. | **CLEAN, both fixes intact after a ten-ADR window.** (The residual hand-carried "53" in the same bullet is rowed separately at D1-4.) | None. | close-as-fine |
| **D5-4** | W-8 — which generated artifacts still hand-maintain a list of what they read? | `state-derived.md` derives its input list from the renderer's own single `inputs` definition (the ADR-0158 L3-3 treatment) — **still the only one**. Every other banner states its inputs in a **hand-written renderer string**: `docs/dev/pipeline.md` ("from components/corpus/docs/pipeline.edn"), `docs/use-cases.md`, `docs/operators.md`, `docs/cli.md`, `notes/ADRs.md`, both record `INDEX.md`, `docs/formats.md`. | **W-8 FIRED, narrowly.** No banner is WRONG today (D5-3 confirms the two that were, were fixed). What has not happened is generalisation: the treatment that makes the enumeration underivable-from-drift was adopted once and applied to nothing else in ten ADRs. Every remaining banner is a hand-written claim about a generator's reads, and `docs/formats.md`'s was wrong for as long as anyone looked. | Low priority as a fix; useful as a rule — a generator that reads more than one input emits its input list rather than describing it. | intake |
| **D5-5** | Does any live plan carry a MEASURED row the same file later inverts, without a supersession marker? | `.agents/plans/2026-08-24-traffic-scale-program.md:177-183` — *"**MEASURED (2026-08-24)** — where the time goes at 10^5 … Check: six of 29 invariants are 99.4%, led by `occupancy-within-capacity` at 54.9%"* — stands unmarked. `:195-218`'s post-arc-0 row inverts it: check is now **6.7%** of the cell and "further work at this scale is generator-side". `grep -n supersed` over the file → **no hits**; `grep -c -iE 'immutab\|supersession' .agents/rulings.md` → **0**. | Supersession by juxtaposition, 25 lines apart, in the plan that is the live pointer for the whole traffic-scale program. A reader who reads the 08-24 row and stops takes an inverted ranking as current MEASURED fact. The session record was correctly left untouched — but **no rule anywhere says records are dated and plans carry supersession**, which is why the question had to be re-derived here. | Mark the superseded row in place (the plan's own `:53-60` pattern — amend, quote the original verbatim — is the good shape and is used exactly twice in the tree). Land the doctrine as a `rulings.md` row. | ruling-needed |

---

## Dimension 6 — Sampling adequacy

*5 probes run of 12. Sub-agent line L-1 carries this dimension's deepest
work; its rows are below with per-row provenance.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D6-1** | W-7 — re-derive the `defspec` SET (not the cardinality, per D1-4's standing lesson) at both tips and diff | `92d23bc` → **80**; `f05f51a` → **83**. `comm` on the sorted name sets: **three added, zero removed** — `fast-invariants-equal-their-naive-reference-implementations` (`check_test.clj:958`), `cancel-reinstatement-survives-the-fold-carried-index` (`engine_test.clj:1476`), `citation-index-resolves-exactly-what-the-scan-resolved` (`engine_test.clj:1598`). All three are arc 0's. | **W-7's own failure mode was caught IN SESSION, by the property's author.** `the-reinstatement-defspec-actually-sees-reinstating-cancels` (`engine_test.clj:1484`) exists *because* the first hand-picked run did not induce all six invariants — the record says so (deviation 10). That is the fixed-shape blind spot, found and closed before landing. Sampled fixed shapes: `churn-facility`/`churn-providers`/`active-churn-profile` fixed, seed and patients varied; the fixed part CAN express the branch, and the companion proves it does (≥1 in 40 seeds, floor of 2 runs). | None on the three new ones. W-7's question is answered for arc 0 and NOT for the pre-existing 80 — see D6-3. | close-as-fine |
| **D6-2** | Compare the two non-vacuity companions arc 0 co-landed, against each other | `the-reinstatement-defspec-actually-sees-reinstating-cancels` asserts over **40 seeds** with a floor (`pos? total` **and** `< 1 (count (filter pos? counts))`). `the-citation-index-defspec-actually-resolves-something` (`engine_test.clj:1607`) asserts on **ONE** hand-picked run (seed 7, patients 8). Neither 120/150-trial defspec asserts non-vacuity per trial. | Asymmetric rigour between two gates landed in the same commit for the same reason. L-1's independent instrumentation measures the actual populations — 799 reinstating cancels over 150 trials (148/150 non-empty) and 5,324 non-nil resolutions over 120 trials (120/120) — so **both are richly non-vacuous today**; what differs is how much each would notice if that changed. | Give the citation companion the reinstatement companion's shape (a multi-seed floor). Small. | fix-session-candidate |
| **D6-3** | For the ADR-0166 invariant added to the catalog this window, what population does the suite's flagship 300-trial catalog defspec actually reach? | L-1 re-derived the generator space at its own trial count: **10,142 events across 300 trials, exactly 10 of the schema's 21 closed kinds** — zero `:medication-end`, `:care-plan-end`, `:medication-order`, `:care-plan-start`, `:observation`, `:procedure`, `:diagnostic-report`, `:outpatient-visit(-end)`, `:order-placed`, `:result-available`. **Coordinator re-derivation:** the four committed gated corpora, parsed (not grepped — see the method note below), carry `:care-plan-end` in exactly **one** of four and `:medication-end` in exactly **one**, 1 event each. | `every-m1-run-satisfies-the-invariant-catalog` is the repo's broadest correctness property and is **blind to 11 of 21 event kinds**, including both referential END invariants — one of which (ADR-0166) was *added to the catalog inside this window* and reported as "no gated corpus newly fails". This is review 4's D6-1 finding (a fixed generator shape that cannot express the branch its name vouches for) recurring one level wider: not a fixed facility now, a fixed pathway space. | Extend ADR-0165's generator-side coverage discipline to the property-based catalog defspec: assert which kinds its generators produce, and either widen the pathway generator or declare the 11 uncovered kinds as rowed waivers. | fix-session-candidate |
| **D6-4** | Method note — how the coordinator's own first census was WRONG, and how it was caught | The coordinator's first pass counted event kinds in the committed corpora with `grep -o ':event :[a-z-]*'`, returning **174 `:medication-end` in seed-424242**. Parsing the same file with `clojure.edn` returns **343 top-level events and ZERO `:medication-end`** — the grep was matching events nested inside `:pre-horizon-facts`. | Recorded because it is the rubric's own law paying for itself inside this review: *audit evidence uses the mechanism it recommends*. A grep over an EDN log is not a census of that log, and the wrong instrument contradicted a sub-agent finding that turned out to be exactly right. **Any register row in this file that states an event count was produced by parsing, not by grepping.** | Keep the note. It is the cheapest possible warning for review 6. | close-as-fine |

---

## Dimension 7 — Continuity integrity

*8 probes run of 12.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D7-1** | `R-unregistered-request-gets-a-row` — does every standing request from the design channel have a roadmap row before it has a disposition? | **Q4** (host-side sample into `bin/preflight` or a convention doc) has **no roadmap row, no `rulings.md` row, no ADR**. The tree says so itself: `.agents/session-records/2026-08-24-traffic-scale-program.md:117-118` — *"`Q4` was not touched, as instructed; **there is nothing named Q4 in the tree either**."* Q2 closed by ADR-0167; Q3 converted into `roadmap.md#stream-partition-design` (P26) rather than minted standalone, with the reason recorded. | The rule (ADR-0139, *"visibility first"*) was followed for Q3 and **not followed for Q4**, which has been live in the design channel since 2026-08-24 with no register home. It is also the request sub-agent line L-3 was chartered to price — so this review is being asked to disposition a request that never got its row. | Row it before ruling it. Both halves are in the plan. | fix-session-candidate |
| **D7-2** | W-10 — does every roadmap continuation line sit under the row whose subject it names? | Per-row line counts across all 44 rows: **every row ≤ the six-line cap**, none over. ADR-0159's F-1 defect (the ADR-0152 row swallowing five of ADR-0150's continuation lines) **is no longer in `roadmap.md`** — the attic rotation law moved it, verbatim, to `.agents/plans/roadmap-done-2026-08.md:2675-2680`. | **W-10 fired sideways.** No new instance; the recorded instance **left the file the live row still says it is in**. `roadmap.md#register-gate-row-ownership` (PRIORITY 4, OPEN) reads *"`c509e46` inserted the ADR-0152 row inside the ADR-0150 row, so five continuation lines **now sit** under the wrong slug"* — of `roadmap.md`. They do not. They sit in an **append-only** attic file, which raises a question nobody has ruled: **can a defect that rotated into the append-only attic be corrected there at all?** | Ruling needed on correcting the attic (ADR-0161's law says rows move *verbatim*, which is what preserved the defect faithfully). Meanwhile the live row's location claim is a one-line errata. | ruling-needed |
| **D7-3** | W-3 — re-run audit (a)'s question over this window's own row closures: does the closing ADR name the slug it closes? | Population enumerated from both roadmap files: **eight** rows closed by an ADR-016x. Named: `#oracle-coverage-gate-integration-half` (ADR-0160, 2 hits), `#attic-rotation-law` (0161, 4), `#patient-simulator-charter` (0162, 2), `#suite-time-residual` (0167, 2). **NOT named: `#unpaired-end-step-and-citation-scope` (ADR-0163, 0 hits), `#generator-side-event-type-coverage` (0165, 0), `#suite-time-doubling-diagnosed` (0167, 0), `#performance-arc-0` (0169, 0).** | **W-3 FIRED, and the ratio worsened sharply**: review 4's arc was 7 unnamed of 38 closures (**18%**); this window is **4 of 8 (50%)**. Every unnamed one is carried by substance — the ADRs are unmistakably about their rows — but the register cannot route a reader from row to ADR mechanically, and `roadmap-lint-test` cannot see it (it gates the token, slug, cap and priority; not whether the cited ADR mentions the slug). | The gate is cheap and the population is small: assert that a `CLOSED … ADR-NNNN **[slug]**` row's ADR file contains the slug. That closes W-3 as a class rather than re-measuring it every review. | fix-session-candidate |
| **D7-4** | W-4 — is `#two-clocks-asset-field-audit` closed? | `roadmap.md:102`, **OPEN, PRIORITY 19**. Opened by ADR-0158 (review-4 fix 4, 2026-08-19). Still open through the review-4 arc close, the ADR-0163/0166 fix arc, and arc 0. | **W-4 FIRED as written** — "still open at review 5, it will have outlived two arcs". It has outlived **three**. Its blocker is not stated on the row, which is what `R-carried-item-aging` exists to prevent. | Name the blocker on the row, or charter it. It is a hand-authored SVG whose audit sentence is false since ADR-0142; the fix is a sentence. | fix-session-candidate |
| **D7-5** | Header-resident standing requests — grep tracked files outside the registers | Population: all tracked files, `git ls-files \| xargs grep`, excluding `notes/adr/`, `.agents/session-records/`, `.agents/prompts/`, `.agents/plans/` (dated artifacts, out of population by the standing boundary). Hits: the repo-review SKILL's own probe text (both mirror copies); `components/patient-simulator/docs/gmf-interpreter.md:1365` and `…-findings.md:1200`, both carrying a **dated ADR-0162 disposition note**; `components/sim/docs/sim-theory-diagram.md:125`, a dated ADR-0135 regeneration note; and vendored Synthea census `.edn` (upstream TODOs, not this repo's). | **CLEAN.** Every in-tree standing request outside the registers carries a dated disposition. The class review 3 opened stays closed. | None. | close-as-fine |
| **D7-6** | Is `#corpus-player-slices` rowed, or still only cited? | `roadmap.md:120`, **OPEN, PRIORITY 22**. | **Rowed.** The prompt's open question is answered: it is a register row, not a bare citation. | None. | close-as-fine |
| **D7-7** | Structural headroom in the roadmap itself | **25 of the 31 OPEN rows sit exactly AT the six-line cap**, `#repo-review-5` (P3) among them. | This session's own mandated close act — "the row gains one line pointing at the register and plan" — **cannot land without compacting the row first**. Reported as a live constraint rather than discovered at the close: a register whose rows are 80% at cap converts every future pointer into a rewrite, and the rewrite is where continuation lines get orphaned (ADR-0159 F-1's own mechanism). | Not a defect; a pressure reading. Worth the author's eye when deciding whether the cap or the row count moves. | intake |
| **D7-8** | Carried-item aging across the thirteen inherited watch-list rows | Full disposition table below. | **Six fired (W-1, W-3, W-4, W-5, W-9, W-13), one fired sideways (W-10), two did not fire (W-2, W-11), one fired narrowly (W-8), one is answered-and-superseded (W-12), one did not recur (W-6), one is answered for the new gates and open for the old (W-7).** | Per row, below. | — |

---

## Dimension 8 — Operator experience

*6 probes run of 12. Every probe executed against the BUILT `bin/ehrt`,
not `clojure.test`.*

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| **D8-1** | W-5 — re-measure with `bin/fence-census` against ADR-0158's 28 exercised / 3 exempt / 46 bare of 77 | `bin/fence-census` at `f05f51a`: **105 files in scope, 229 fenced blocks, 77 command fences — 28 exercised / 3 exempt / 46 bare.** Identical in every cell. Reader-path bare re-derived per file: `docs/manual/` **21**, `docs/use-cases/` **13** = **34**, exactly R4-Q4 (a)'s deferred set. | **W-5 FIRED.** The bare count on the reader path did not fall — it did not move **at all** in ten ADRs. It also did not fall by exemption (W-2's other half): exempt is still 3. `roadmap.md#reader-path-fence-battery` (P20) has had no session. This is aging, not regression, and the watch asked exactly the right question. | Charter it or re-price it. The row's own note says several of the 34 need a primed artifact cache, which is why D8-5 lapsed twice before ADR-0140 discharged it. | fix-session-candidate |
| **D8-2** | Bare invocation / unknown flag / missing file matrix | Bare `bin/ehrt` → usage text, **exit 0** (ruled: ADR-0060 AR-U2-4; recorded executed at ADR-0064's close table row "bare invocation exits 0 (ruled, then executed)"). `corpus generate sim --nonesuch` → `:unknown-flag`, exit 2. `show /nonexistent/file.hl7` → `:gate-path-not-found` with a remedy hint, exit 2. | **CLEAN**, and the exit-0 case is a ruled design decision rather than an oversight — re-verified against the ruling rather than re-flagged. | None. | close-as-fine |
| **D8-3** | Help at narrow width | `COLUMNS=40 bin/ehrt help` → **zero lines exceed 40 columns**. | **CLEAN.** | None. | close-as-fine |
| **D8-4** | The README's own two-commands-to-demo path, RUN FOR REAL from a cleared `out/` | `bin/ehrt corpus generate` → exit 0, `{:status :ok, :payload {:out-dir "out/corpus/sim-s1-p1"}}`, directory present. Re-run → `{:status :error, :category :out-dir-exists, … :hint "same inputs always derive the same out-dir, so this run refused to silently overwrite the last one -- run \`rm -rf out/corpus/sim-s1-p1\` to regenerate in place, or pass a different --out-dir"}`, exit 2. | **CLEAN, and the behaviour matches the README's own prose paragraph word for word** — including the never-overwrite contract the README stops to explain. The remaining Quickstart steps need network artifact fetches and are covered by `make quickstart` (a local/manual check, as the README itself discloses). | None. | close-as-fine |
| **D8-5** | Which of the 46 bare fences sit on the front door? | `README.md`: 3 exercised, 1 exempt, **0 bare**. `SETUP.md`: 2 exercised, 2 exempt, **0 bare**. | **CLEAN — R4-Q4 (a)'s `bare-on-README+SETUP = 0` gate is holding** ten ADRs later, and holding without the exempt count rising. | None. | close-as-fine |

---

## Suite baseline, and the D3 standing probe

**Full `make test`, unpiped to a log by redirect (not a pipe), wrapper
capturing `MAKE_EXIT` and ending `exit "$MAKE_EXIT"`
(`R-full-suite-before-push`).** Taken at `f05f51a`, tree clean, `out/`
cleared first, **after** all three sub-agents had finished — deliberately,
because review 4's own Step-0 suite ran at 21m13s under three-sub-agent
contention and its register had to record the figure as not comparable.

    MAKE_EXIT=0
    WALL_SECONDS=881  (14m41s)          <- wall
    poly Execution time: 14m02s (842 s)  <- poly's own line, a DIFFERENT kind
    370 zero-failure blocks / 4,166 tests / 18,690 assertions
    grep -cE '^(FAIL|ERROR) in'  =  0
    clojure -M:poly check  OK
    bin/verify-nist-lock   OK: 6 hit-nexus-sourced coordinate(s) match

**Health record, sampled at the moment of the figure** (ADR-0167's
convention; there is no standing surface that requires this — see L3-10):
Windows `LoadPercentage` **1 / 4 / 3**; five `wslhost.exe`, largest
cumulative CPU **1.11 s** — no orphan (ADR-0167's was 68.7 CPU-**hours**
across six threads at 99% of a core each). Linux `uptime` 1-minute load
**0.20**, 12 logical CPUs, up 14h00m.

**Reconciliation — and it settles L3-1 empirically.** The counts
reconcile **exactly** against ADR-0169's `370 / 4,166 / 18,690`. On time,
comparing **like with like**, both ways:

| comparison | baseline | this run | delta |
|---|---|---|---|
| **wall vs wall**, against the ADR-0167 post-reboot run | 878 s | **881 s** | **+3 s** |
| **poly vs poly**, against the same run | 839 s (13m59s) | **842 s (14m02s)** | **+3 s** |
| wall vs wall, against arc 0's own two runs (mean 866 s) | 866 s | 881 s | +15 s |

**The two kind-matched comparisons agree to the second, at +3 s.** The
arc-0 record's `+27 s` comes from subtracting a **poly execution time**
from a **wall**; wall-against-wall the same record's own figures give
**−12 s**. This run, on a machine sampled quiet at the moment of the
figure, puts the post-arc-0 suite **3 seconds** off the ADR-0167 baseline
on both clocks — which is the strongest available confirmation of L3-1's
mechanism and of its recommended remedy: **a recorded timed figure must
name its kind.**

**D3-4 — `make ci-parity`, the rubric's STANDING environment probe.**
Real `git clone` into `/tmp/ehr-testing-ci-parity`,
`EHR_TESTING_TOOLS_CACHE` repointed at an empty directory, then
`clojure -M:poly check` + `clojure -M:poly test :all skip:integration`.

    make ci-parity  ->  exit 0,  873 s wall,  poly Execution time 13m52s
    370 zero-failure blocks / 4,166 tests / 18,690 assertions
    grep -cE '^(FAIL|ERROR) in'  =  0
    "== ci-parity: green as CI sees it =="

**Green from a real fresh clone with a cold artifact cache**, and its
counts reconcile **exactly** with the edit-root suite above — 370 /
4,166 / 18,690 both. Nothing in this tree depends on an untracked file,
an author-local checkout, or a primed cache.

Its **one stated limit**, verbatim from the rubric rather than claimed
away: *it does not repoint `HOME`, so `~/.m2` is still shared — cold-cache
parity, not a cold machine.*

---

## Review 4's thirteen-row watch-list, dispositioned

Built by ADR-0159 (`:374-402`, thirteen rows at `:381-393`). Each row's
CURRENT state re-derived here, never carried.

| # | verdict | evidence, re-derived at `f05f51a` |
|---|---|---|
| **W-1** born-red gate discipline | **FIRED** (third branch: "the practice has continued and now deserves a `rulings.md` row") | Born-red deepened to a two-directional witness (ADR-0166 `:70-90`) and caught a wrong GATE by its own red (ADR-0162 `:173`); arc 0 then introduced the opposite discipline — **born green on the unrefactored tree** (ADR-0169 `:62-66`). No red was tuned away; every red is dispositioned in writing. Neither practice has a ruling row. → **D2-4** |
| **W-2** the `exempt` disposition has no ratchet | **DID NOT FIRE** | `fence-exemptions.edn` = **3** rows, untouched since ADR-0158 (`git log` shows only `3c4e346`/`ca02aa0`). All three reasons state impossibility. → **D2-3** |
| **W-3** an ADR closing a row need not name it | **FIRED, ratio worsened** | Review 4's arc: 7 unnamed of 38 (18%). This window: **4 unnamed of 8 (50%)** — ADR-0163, 0165, 0167(doubling), 0169 contain zero occurrences of the slug they close. → **D7-3** |
| **W-4** `#two-clocks-asset-field-audit` | **FIRED** | `roadmap.md:102`, still **OPEN** at PRIORITY 19. Opened ADR-0158; has now outlived **three** arcs (review-4 close, the 0163/0166 fix arc, arc 0), one more than the watch predicted. Blocker not stated on the row. → **D7-4** |
| **W-5** reader-path fence battery | **FIRED** | `bin/fence-census`: **28 / 3 / 46 of 77** — identical to ADR-0158 in every cell. Manual 21 + use-cases 13 = 34. Did not fall, and did not fall by exemption. → **D8-1** |
| **W-6** the historical-red technique | **DID NOT RECUR** | `grep -rn -i worktree` across the window's ADRs and records: what recurred is the **two-worktree digest bracket** (ADR-0162's rename bracket, arc 0's 10^5 byte-identity bracket) — a sibling that `bin/regression-oracle` already institutionalizes — not the historical red (prove a widened test red against a pre-fix engine). ADR-0167's diagnosis session explicitly **declined** a worktree run at the old commit (record `:139-144`, "F2 was therefore never engaged") for a stronger design. **So W-6's trigger did not fire — but a THIRD technique did reach two uses**: the *mutation witness* (neuter or mutate the mechanism, assert the whole rejection set fails), at ADR-0166 `:70-90` and in arc 0's mutation battery. By W-6's own standard ("worth a skill line once used twice") that is the one now owed. → plan item |
| **W-7** fixed-shape blind spots in `defspec`s | **ANSWERED for the new three, OPEN for the other 80** | Sets compared, not cardinalities: 80 → **83**, three added, zero removed, all arc 0's. All three carry non-vacuity companions, one of which exists *because* the first hand-picked run did not induce all six invariants (record deviation 10) — W-7's failure mode, caught in session. The pre-existing 80 are unsampled this review except for `every-m1-run-satisfies-the-invariant-catalog`, which **is** blind (D6-3). → **D6-1**, **D6-3** |
| **W-8** `state-derived` self-listing, adopted once | **FIRED, narrowly** | Still the only generated artifact that derives its own input list. Seven other banners hand-state their inputs in renderer strings. None is wrong today. → **D5-4** |
| **W-9** `R-preflight-fail-closed` in the wild | **FIRED, benignly** | Two of eleven sessions ran preflight to **exit 1**; both disclosed the specific `FINDING:` and reasoned about it explicitly. **No `UNKNOWN:` occurred at all**, so that half is untested. A new sub-finding fell out: one of the two ran preflight *after* staging its own edits. → **D3-1**, **D3-2** |
| **W-10** a roadmap row can swallow another's continuation lines | **FIRED SIDEWAYS** | No new instance; all 44 rows ≤ cap. The recorded instance **rotated verbatim into the append-only attic** (`roadmap-done-2026-08.md:2675-2680`), leaving the live `#register-gate-row-ownership` row asserting a location that is now false. → **D7-2** |
| **W-11** a widened `rulings.md` row need not say who widened it | **DID NOT FIRE** | The window's `rulings.md` diff is **append-only**: 12 new rows, zero modifications, every row attributed. The class had no opportunity to recur. → **D2-2** |
| **W-12** the plan's own live falsehood | **ANSWERED AND CLOSED, but the class recurred elsewhere** | `state-derived.md` is counted by **zero** reading sets (`grep -c state-derived .agents/reading-sets.edn` → 0), and the review-4 plan now carries the correction in place at `:327-331` under `R-RP`. The class itself recurred on two LIVE roadmap rows: the "14 calls" figure (**D1-5**) and `#generator-coverage-depth`'s one-deep count (**L1-7**). |
| **W-13** `:onboarding` headroom | **FIRED** | **34** lines (`state-derived.md:82`), tightest of five for the third consecutive review (32 → 132 → 46 → 34). `R-budget-stop` applies to this session's own close. → **D1-6** |

**Six fired, one fired sideways, one fired narrowly, one did not recur,
two did not fire, one is answered-and-closed, one is answered-in-part.**
Review 4's own thirteen-row list was, on this evidence, **well aimed**:
every row that could fire had a probe that could see it, and the two
that did not fire did not fire for a stated reason rather than for want
of looking.

---

## Sub-agent line L-1 — Gate vacuity

Charter: build the gate × witnessed-population matrix for EVERY gate
added ADR-0160..0169; name the vacuous-or-nearly set; rule on whether
ADR-0169's "assert the count so a drift to zero goes red" pattern earns
a `rulings.md` row. Own fresh clone of `f05f51a`, no probe cap.

**Population, three ways, symmetric difference EMPTY.** (A) the ADRs'
own gate sections; (B) `git diff --numstat -M 92d23bc f05f51a --
'*test*'` filtered to ≥10 added lines; (C) `grep -rl 'ADR-016'
components/*/test projects/*/test`. All three yield the **same 8
namespaces / 49 new `deftest`+`defspec`**. ADR-0167 and ADR-0168 add no
gate. **Coordinator note:** the charter's own seed grep (C) was complete
here only because every new gate in this window carries its ADR token —
a property of these ten ADRs, not a guarantee. Diff-derived enumeration
is the primary for review 6.

| id | probe | evidence | finding | recommendation | disposition | provenance |
|---|---|---|---|---|---|---|
| **L1-1** | Run `run_test`'s own `unpaired-ends` over the corpora its two ADR-0163 gates assert on | Coordinator re-derivation, by **parsing** the four committed baseline corpora with `clojure.edn` (not grepping — see D6-4): `arc0_gated_seed_424242_clinic_decade.edn` = **343 top-level events**, kinds `{:care-plan-start 9 :medication-order 20 :observation 10 :outpatient-visit 38 :outpatient-visit-end 38 :procedure 28 :registered 200}` — **zero `:medication-end`, zero `:care-plan-end`**. `…seed_5…` = **363 events**, same two kinds **zero**. `unpaired-ends` (`run_test.clj:772-786`) filters for exactly those two kinds, so `(is (empty? (unpaired-ends …)))` at `:809` and `:825` is **true by construction**. Neither asserts a non-empty candidate population. | **ADR-0163's two population-scale gates are VACUOUS at HEAD, and the tree already knows.** `gated-runs`' own comment (`run_test.clj:410-413`, added by ADR-0165) says *"The three scenario runs above produce NEITHER `:medication-end` NOR `:care-plan-end`"* — four hundred lines above two gates that filter for exactly those and stay green. Worse, `:811`'s docstring calls itself *"the ONLY population-scale exercise of the `:care-plan-end` half"*; at HEAD it exercises **none**. The fix that closed the defect removed the only events its own regression gates could see. | Add the ADR-0169 count assertion to both: pin the candidate count, red on a drift to zero. Then either re-point them at a corpus that carries the events or amend the seed-5 docstring. **Not rowed anywhere in `roadmap.md`.** | fix-session-candidate | **RE-DERIVED** (coordinator parsed all four corpora and read both gates and `unpaired-ends` in its own tree; the sub-agent's live-run event counts of 343/363 reproduce to the event) |
| **L1-2** | Re-derive the 300-trial catalog defspec's own generator space at its own trial count | Sub-agent: **10,142 events over 300 trials, exactly 10 of the schema's 21 closed kinds** — `{:admission 3601 :bed-swap 108 :cancel-admit 13 :cancel-discharge 12 :cancel-transfer 73 :discharge 1975 :merge 33 :registered 3779 :step-rejected 19 :transfer 529}`. Zero `:medication-end`, `:care-plan-end`, `:medication-order`, `:care-plan-start`, `:observation`, `:procedure`, `:diagnostic-report`, `:outpatient-visit(-end)`, `:order-placed`, `:result-available`. | The suite's flagship population-scale catalog judge (`check_test.clj:670`) is **blind to 11 of 21 event kinds**, including the ADR-0166 invariant *added to `catalog` inside this window*. ADR-0165 built a coverage meter for `run_test`'s four gated corpora and not for this defspec, which is the larger population by an order of magnitude. | Extend ADR-0165's generator-side coverage discipline to the catalog defspec: assert which kinds its generators produce; widen the pathway generator or row the 11 uncovered kinds as declared waivers. | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed `care-plan-end-references-existing-start-and-follows-it-in-time` is in `catalog` (`check.clj:827`) and that the four gated corpora carry **one** `:care-plan-end` and **one** `:medication-end` between them; the 300-trial instrumentation is sub-agent-witnessed |
| **L1-3** | Run the four CarePlanEnd-bearing vendored modules at their own committed configs | Sub-agent: asthma 314 events / 0 `:medication-end` / 0 `:care-plan-end`; bronchitis 1,681 / 0 / 0 despite **282 `:care-plan-start`** and 282 `:medication-order`; tjr 300 / 0 / 0; adhd 314 / 0 / 0. | Combined with L1-1 and L1-2, the two referential END invariants' **entire population-scale witness set in the per-push suite is two events**, both in `adhd-seed-2`, both on the ACCEPTING pre-horizon-escape branch. Neither has a standing gate that witnesses it REJECTING at population scale. | Row the depth explicitly; ADR-0166's own Red-2B mutation shows the shape of the missing gate but is not a landed one. | intake | **RE-DERIVED in part** — the gated-corpus half is coordinator-parsed (adhd = 12 events, 1 `:medication-end`, 1 `:care-plan-end`, both cited, **both resolving to nil**); the four vendored-module runs are sub-agent-witnessed |
| **L1-4** | Copy the defspec's own generators and five mutators verbatim; run 120 trials at its own pinned seed 20260825, tallying findings per invariant per trial | Sub-agent: trials with ZERO findings **on the mutated log** — `no-double-occupancy` **78/120**, `no-events-after-merged-terminal` **66/120**, `cancel-references-existing-uncancelled-event` **41/120**, `outpatient-patients-occupy-no-bed` 15/120, `admitted-occupies-one-slot` 1/120, `occupancy-within-capacity` 0/120; 37/120 trials draw `churn-profile` nil. | **Arc 0's own equivalence defspec compares `[] = []` on a majority of trials for two of the six invariants it exists to prove.** Its non-vacuity companion pins ONE point of the parameter space and cannot see this. | Move the per-invariant `(is (seq found))` INSIDE the property, or make each mutator's applicability part of the generated precondition. | fix-session-candidate | **RE-DERIVED in part** — coordinator read `the-mutations-actually-make-all-six-invariants-fire` (`check_test.clj:991-1010`) and confirms it pins exactly one point: seed 27, 60 patients, a hardcoded three-ward facility, with the fixture's own comment explaining why it does not reuse `mixed-ward-facility-gen`. The 120-trial tally is sub-agent-witnessed |
| **L1-5** | Classify every cancel-family test in `engine_test` by whether it reaches `engine/run` | Coordinator read `world-of` (`engine_test.clj:330-333`): it yields `{:patients :facility :providers :ground-truth :order-profiles}` — **no `:reinstate-index`, no `:citation-index`**. `engine.clj:1283-1293` and `:885-904` branch on `(contains? world :<key>)`, so a hand-built world takes the **replay/scan fallback**. Sub-agent: 7 of 11 cancel-family tests build hand worlds, **including ADR-0164's own two regression gates** (`:1177`, `:1198`). | After arc 0, **ADR-0164's two regression gates no longer run the code `run` ships.** The class survives only because arc 0 happened to add a population-scale citation defspec. `engine.clj:886` says so in-source ("a hand-built world, as most of engine-test uses") — disclosed, not gated. | Seed `world-of` with empty carrier maps so scripted tests take the shipped branch, or add a gate asserting which namespaces exercise which branch. Cross-cite the arc-0 defspecs from the ADR-0164 tests so the coverage is not accidental. | fix-session-candidate | **RE-DERIVED** (coordinator read `world-of` and both fallback sites in its own tree) |
| **L1-6** | Run both arc-0 engine defspecs' generator spaces at their own pinned seeds and trial counts | Sub-agent: `:1476` — 150 trials, **799 reinstating cancels, only 2/150 trials empty**; `:1598` — 120 trials, **5,324 cited ends, 5,324 non-nil resolutions, 0/120 empty**. Both carry explicit companions with floors. `run_test.clj:658` pins the cancel count at 10 with a `pos?` backstop; `:723` pins the exact cited-end vector. | **CLEAN, and the reference implementation.** ADR-0169's four gated-corpus/defspec pairs are the only gates in the ADR-0160..0169 population whose thinness is both **measured and pinned** — they degrade loudly rather than silently. | Keep. Use as the worked example for the ruling at L1-9. | close-as-fine | **RE-DERIVED in part** — coordinator read both companions and both pinned assertions (`engine_test.clj:1484-1499`, `:1607-1648`) and confirms the floors are as described; the trial-space tallies are sub-agent-witnessed |
| **L1-7** | Compute per-run emittable-vs-cited-produced with the coverage gate's OWN helpers over the four committed corpora | Sub-agent: cited depth `{:admission 1 :care-plan-end 1 :care-plan-start 14 :diagnostic-report 1 :discharge 1 :medication-end 1 :medication-order 41 :observation 35 :outpatient-visit 84 :outpatient-visit-end 84 :procedure 45}` — **five** one-deep types in **two** runs. `roadmap.md:41-46` (`#generator-coverage-depth`, P6) names **three** (`:admission :discharge :diagnostic-report`) in **one** run. ed-tuesday: 10 emittable, **0** cited produced. | The live roadmap row **undercounts its own subject**, and the two it omits — `:care-plan-end` and `:medication-end` — are the two types ADR-0165's whole hunt existed to cover. A reshuffle in `adhd-seed-2` takes both end types dark at once, which is the exact ADR-0163 invisibility this arc closed. | Errata on the row: five types, two runs. It changes the priority calculus, not just the number. | fix-session-candidate | **RE-DERIVED** — coordinator applied the gate's own `:citation` filter to all four parsed corpora and reproduces the union **exactly**: `{:admission 1 :care-plan-end 1 :care-plan-start 14 :diagnostic-report 1 :discharge 1 :medication-end 1 :medication-order 41 :observation 35 :outpatient-visit 84 :outpatient-visit-end 84 :procedure 45}`, one-deep set `(:admission :care-plan-end :diagnostic-report :discharge :medication-end)` — **five types, two runs** — and ed-tuesday at **zero** citation-bearing events of any kind |
| **L1-8** | Compare the two history-reading freshness gates | Coordinator read both: `hand_owned_asset_freshness_test.clj:81` uses `(shell/sh "git" "log" "-1" "--format=%H" "--" path)` — **committed history only**. `attic_rotation_test.clj:148` (ADR-0161, **one ADR earlier**) adds `working (numstat-deletions (git "diff" "--numstat" "HEAD" "--" path))` so the **working tree** is walked too. No enforcement of the post-commit-rerun ordering exists in `rulings.md`, the `Makefile` or `bin/preflight`. | ADR-0162 diagnosed a gate blind **by construction** to an uncommitted tree and handed the class to review 5. **The remedy already exists in the tree, one ADR earlier, and was not generalized to the sibling.** The adopted remedy — re-run the gate after committing — is a session habit no mechanism enforces. | Generalize `attic_rotation_test`'s HEAD-vs-working-tree step to every history-reading freshness gate. This is the mechanical discharge of ADR-0162's carried class. | fix-session-candidate | **RE-DERIVED** (coordinator read both gates and ran the three-surface grep for the ordering rule: zero hits) |
| **L1-9** | Read `R-empty-population-is-red` and test it against L1-1's vacuous gates | `rulings.md#R-empty-population-is-red` (ADR-0148) is **SATISFIED** by both L1-1 gates: seed-424242's corpus is 343 events, non-empty. Its candidate subset is **zero**. Sub-agent: the row is cited in 10 docs-tooling/integration test files and **zero** simulation-side ones; the simulation gates that do guard vacuity cite "ADR-0169" and the bare word "vacuous". Slug `R-witness-population-is-counted` is free. | **The existing ruling gates the corpus, not the witness.** That single distinction separates the gates that survived this review (L1-6) from the ones that did not (L1-1). It is currently a per-ADR habit, unrowed, and unknown to the half of the tree that most needs it. | Land the ruling. Proposed text, contract-shaped (3 lines): *"**R-witness-population-is-counted** -- a gate over a generated corpus asserts the SIZE of the subset that can EXHIBIT the failure it claims to catch, pinned as a count, not merely that the corpus is non-empty; a drift of that subset to zero is red, not green -- ADR-NNNN"*, citing ADR-0169 as origin. | ruling-needed | **RE-DERIVED** (coordinator read the ruling and demonstrated it satisfied by a vacuous gate via L1-1) |
| **L1-10** | Read `gated-runs`' opts against the churn mechanism and census the churn family | Coordinator: `run_test.clj:404-409` — **both clinic-decade entries pass `:churn true`**. Parsed census: seed-424242 and seed-5 carry **zero** of `{:cancel-admit :cancel-transfer :cancel-discharge :bed-swap :merge :transfer}`. Sub-agent's mechanism: `churn/inject` splices into **pathway IR**, and clinic-decade declares `:pathway {:name "clinic-decade" :steps []}` — zero steps, nothing to splice into. Only seed-202 churns (9+1 reinstating cancels, 10 bed-swap, 7 merge, 2 cancel-admit). | **Two of the four gated corpora declare a knob that does nothing.** A reader of `gated-runs` would conclude the churn/cancel/merge family is covered by three of four corpora; it is covered by one. This is the mechanism behind arc 0's own F-1, stated as a cause rather than as a count. | Drop `:churn true` from the two clinic-decade entries (honest declaration), or give them a pathway churn can act on. Record the mechanism where a future gate author will read it. | fix-session-candidate | **RE-DERIVED** (coordinator read the opts and parsed the census; the `churn/inject` source reading is sub-agent-witnessed) |
| **L1-11** | Read `arc0-check-all-findings-are-identical-on-every-gated-corpus` against the corpora it runs on | Coordinator read `run_test.clj:611-625`: it asserts `(= {:status :ok :payload {:invariants-checked arc0-invariant-catalog :events (count baseline)}} …)`. All four corpora are self-check CLEAN, so the finding list is empty in every case. | ADR-0169's SECOND equivalence claim ("identical FINDINGS, full-value `=`") is, over the gated corpora, **"still clean"** — it can catch catalog membership/order drift or a corpus going non-clean, never a change in finding CONTENT. The ADR discloses this and routes the real claim to `check_test`'s discrimination tests, **which L1-4 shows are themselves majority-vacuous for two of six**. The finding-content claim is carried at full strength by nothing. | Nothing to fix in this gate; fix L1-4 and the chain holds. Recorded so no future reader takes this gate as evidence of finding-map equivalence. | close-as-fine | **RE-DERIVED** (coordinator read the gate and its docstring) |
| **L1-12** | Read `reinstate-index-covers-every-reinstatable-event-and-nothing-else` and measure both of its populations | Coordinator read `engine_test.clj:1503-1526`: `(is (seq expected))` is asserted; **`targets` is not**, so `(every? expected #{})` is green over an empty target set. The test name promises "and nothing else"; `:reinstate-index` is not reachable from any test (it lives only inside `engine.clj`), so **nothing asserts the reverse direction**. Sub-agent measured `expected` 33, `targets` 10 at the pinned seed. | A name-versus-assertion mismatch inside an ADR-0169 gate, plus one unguarded population — the same shape L1-1 shows going all the way to zero. | `(is (seq targets))`; then either expose the index on the run result and assert its key set, or rename the test to what it checks. | fix-session-candidate | **RE-DERIVED** (coordinator read the assertions and confirmed `targets` carries none) |
| **L1-13** | Enumerate the gate population three independent ways and set-difference them | (A) ADR sections, (B) rename-aware `git diff --numstat`, (C) `grep -rl 'ADR-016'` — **same 8 namespaces**, symmetric difference EMPTY. | **A clean negative, and a warning.** The charter's seed grep was complete only because every new gate in this window carries its ADR token — discipline, not construction. | Use diff-derived enumeration as the primary at review 6. | close-as-fine | **RE-DERIVED in part** — coordinator independently confirms (C)'s result and that ADR-0167/0168 add no test file (`git diff --stat` shows no `*test*` path in their commits); the three-way set difference is sub-agent-witnessed |
| **L1-14** | Run all 8 gate namespaces green in a fresh clone at `f05f51a` | Sub-agent: run-test 31/124, check-test 77/102, engine-test 90/395, emittable-events 3/5, attic-rotation 7/16, charter 8/19, compile-trajectory 41/83, integration oracle-coverage **1 test / 8 assertions** — all 0 failures. The oracle figure is byte-identical to ADR-0160's own recorded green and to CI run `32402746494`. | **Every gate in the population is green at HEAD — which is the point of this whole line: green is what vacuity looks like.** | None. | close-as-fine | **RE-DERIVED** — the coordinator's own Step-0 full `make test` covers all eight namespaces at the same tip; figures under Suite baseline |
| **L1-15** | Read `ground-truth-column-names-only-real-log-event-kinds` against the schema | Sub-agent: the gate compares `declared` against a **hand-written 21-keyword literal** inside the test; enumerated from `ehrt.sim-engine.event-schema/Event` the schema has 21 kinds, matching today. No non-empty assertion on `declared`. | A registry restating the tree, inside a test — review 3's own thesis class. Currently accurate; failure mode is safe-ish in both directions (schema growth makes it stricter). The docstring justifies the literal on brick-graph grounds. | Add `(is (seq declared))`; note what would go stale, or derive it in a sim-engine-side test. Low priority. | close-as-fine | **RE-DERIVED** — coordinator enumerated `ehrt.sim-engine.event-schema/Event` itself after the timed suite finished: **21 kinds**, matching the test's hand-written `log-kinds` literal exactly, and confirmed there is no `(is (seq declared))` |

---

## Sub-agent line L-2 — The premise-correction ledger

Charter: enumerate EVERY prompt premise a session corrected in this
window; classify each; state which prompt fence caught it and whether
the correction landed in-tree or only in transcript; name the
prompt-structure mechanisms that would convert each class from a session
finding into a channel pre-check. Own fresh clone of `f05f51a`, no probe
cap.

**Population, enumerated from the tree:** 11 archived prompts
(`.agents/prompts/2026-08-2[0-5]-*.md`), 11 session records, 10 ADRs,
**plus this review's own charter, which is in-window by its own rule.**
Coordinator re-derivation of the population: 11 / 11 / 10, exact.

**The ledger totals 40 premise corrections in a six-day window.** Class
frequencies (denominator 40): **mechanism 14 (35%)**, **premise-of-fact
11 (27.5%)**, **path 8 (20%)**, figure 4 (10%), cadence 2 (5%),
population 1 (2.5%). Landing: **4** reached the erring artifact itself,
**4** were prepended as an erratum to the archived prompt, **3** opened a
roadmap row, and **~29 live only in a session record or an ADR
deviations list**, with the prompt archived verbatim and uncorrected.
Transcript-only corrections are **cannot-tell by construction** — they
leave no artifact for this probe to enumerate, so 40 is a floor.

**The inversion this line found, and the coordinator endorses:** the five
prompts carrying an explicit premise fence produced **more** recorded
corrections per prompt (18 over 5 = 3.6) than the six without one (10
over 6 = 1.7). **The fence does not lower the channel's error rate; it
converts error from a silent fix-forward disclosure into a countable
finding.** Any dimension score that reads correction count as prompt
quality inverts the truth, and this register's scoreboard does not.

| id | probe | evidence | finding | recommendation | disposition | provenance |
|---|---|---|---|---|---|---|
| **L2-1** | Re-derive the window population from the tree | `ls -1 .agents/prompts/2026-08-2[0-5]-*.md \| wc -l` = 11; same for records = 11; `ls -1 notes/adr/016*.md` = 10; highest ADR in tree = **0169**, no ADR-0170. | The charter's population claims re-derive exactly. Used as the denominator for every other L-2 row. | Record. | close-as-fine | **RE-DERIVED** (coordinator ran the same three enumerations) |
| **L2-2** | Read all 11 records' Deviations/findings sections, all 10 ADRs' error ledgers, all 11 prompts' fences | The 40-row ledger (in the sub-agent's return; class table above). | **Forty premise corrections in six days**, dominated not by wrong line numbers but by **a mechanism asserted from plausibility without a trace (35%)** and **a row/ruling/dispensation asserted to exist that does not (27.5%)**. | Carry the taxonomy into the plan: path/figure is mechanical (M1), mechanism/premise-of-fact is a ruling (M2). | intake | **RE-DERIVED in part** — coordinator independently found and confirmed seven of the forty (PC-11/12 via ADR-0167's F5 section, PC-20 via the traffic-scale record's delta (c), PC-27 via the arc-0 record's F3-1, PC-32 via arc-0's F-3, PC-35/36 and PC-38 against this session's own prompt); the full forty are sub-agent-witnessed |
| **L2-3** | `grep` the window's prompts for a "the tree contradicting this prompt is a FINDING" fence | Present in **5 of 11**, all dated 2026-08-24 or later, in five separately-invented wordings (F5/F3/F5/F4/F5), none citing a ruling or a skill. `2026-08-21-committee-skill-mojibake-repair.md` has **no `## Fences` section at all**. The two 08-23 prompts carry a FINDING clause **scoped to one named step**, which did not reach the four premises that actually broke in those sessions. | The most-exercised discipline of the window is a **per-author invention**, adopted five weeks into the practice and only by the prompts written last. | Make it a required line of the prompt anatomy rather than a per-author invention. | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed the fence's presence in the 2026-08-24 throughput-spike and 2026-08-25 arc-0 prompts and its absence from `2026-08-21-committee-skill-mojibake-repair.md`; the 5-of-11 tally is sub-agent-witnessed |
| **L2-4** | `grep -c -i premise .agents/rulings.md` | **0**. `build-session/SKILL.md:79-83` step 12 states the doctrine in full and cites `docs/dev/way-of-working.md` §2. | **The single most-exercised discipline of this window has no `rulings.md` row.** A prompt that wants to fence it has nothing to cite, which is exactly why six prompts shipped without one and five invented five wordings. | Mint `R-premise-correction-is-a-finding`, citing way-of-working §2 and this window's forty instances; cite it from both skills. | ruling-needed | **RE-DERIVED** (coordinator ran the grep — 0 — and read step 12 in its own tree) |
| **L2-5** | Read both skills' "Done when" checklists | `build-session/SKILL.md:125-142` — 12 boxes, **none** about premises. `session-prompt/SKILL.md:140-148` — 6 boxes, **none**. | The rule lives in build-session's *procedure* but not in its *checklist* — the only part a closing session mechanically walks. Nothing obliges a session to state whether it hit a premise mismatch, which is why landing is so uneven (4 artifact / 4 archive / 3 row / ~29 record-only). | One box each: "every premise mismatch found is rowed with its class and where the correction landed". | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed the build-session checklist carries no premise box; the session-prompt count is sub-agent-witnessed |
| **L2-6** | Read `session-prompt/SKILL.md`'s citation law and ask what it covers | Step 5 (`:109-115`) forbids `roadmap.md:NNN` outright and gives the reason verbatim: *"line numbers rot on every insert, and ADR-0143's own cite of this very row was fifteen lines stale one session later."* Step 2 requires the prompt to state the HEAD sha it was written against. **Neither is joined to the other**: there is no rule that a prompt's `file:line`, span, count or figure be re-derived at that stated HEAD before shipping. | The skill contains its own argument for the rule and never extended it past queue rows. This is the direct cause of the 12 path+figure corrections, **including three in this review's own charter**. | Extend step 5's law from queue items to every citation: each `file:line`/span/count/figure is either re-derived at the stated HEAD with the probe recorded, or written **anchor-shaped** (symbol, slug, heading) so it cannot rot. | fix-session-candidate | **RE-DERIVED** (coordinator read `:109-115` and step 2 in its own tree) |
| **L2-7** | Test `session-prompt/SKILL.md`'s own closing checklist against the gate that scans it | `SKILL.md:146` = *"Every queued item cites `roadmap.md:LINE` and quotes that row's…"* — the exact form `:111-114` calls red. `roadmap_lint_test.clj:91` `line-cite-pattern` = `#"roadmap\.md:\d"`; `live-scan-roots` (`:94-109`) **includes every `SKILL.md` in both skill trees**. The line survives **only because the placeholder `LINE` has no digit**. Both landed in `5b6e439` (ADR-0144). | **The skill's own closing checklist instructs the citation form the skill's procedure and a live gate both forbid**, and the gate passes over the line teaching the practice it exists to stop. Review 3's thesis in miniature: the gate's *pattern* is narrower than the rule it enforces. | Rewrite `:146` to `roadmap.md#<slug>`; consider widening `line-cite-pattern` to catch placeholder forms. Re-derive before landing — the `.claude` mirror is held byte-equal. | fix-session-candidate | **RE-DERIVED** (coordinator read `:146`, `:109-115`, the pattern at `:91` and `live-scan-roots` at `:94-109`, and confirms `SKILL.md` files are in the scanned population) |
| **L2-8** | Read the first lines of all 11 archives; read `bin/close-scaffold`'s archive template | Erratum preambles in **3 of 11**. `bin/close-scaffold:160` writes `# Archived prompt: %s (%s)\n\n(prompt text TODO)` — **no errata section**. | ~29 of 40 corrections live only in a session record while the erring prompt sits archived verbatim and unannotated — **and the archive is exactly what a future review's history scan reads.** PC-36 is the cost, demonstrated: this charter inherited a rotted `engine.clj:1490` from an uncorrected in-tree citation two hops downstream. | Add an `## Errata` stub to `close-scaffold`'s archive template; make filling it (or writing "none") part of the close. | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed `bin/close-scaffold` writes a bare archive stub; the 3-of-11 tally is sub-agent-witnessed |
| **L2-9** | Compare `way-of-working.md` §2's own worked example against the window's prompts | §2 teaches the doctrine with *"the session prompt characterized the environment as 'JDK 21 (Temurin)'… Temurin was present only at version 17"*. `2026-08-24-suite-time-residual-probe.md` step 0: *"expect Temurin 21, the default"*. Record `:45-49`: *"There is no Temurin 21 on penny… Temurin on this machine is 17."* | **The doctrine's own textbook example recurred verbatim inside the window, in a prompt authored by the same channel, and was caught the same way.** A doctrine taught by example and re-committed by its teacher has no pre-check — only a post-hoc rescue. | The strongest single argument that the fence must fire at prompt-*authoring* time. Cite this recurrence in the ruling. | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed the record's Temurin-17 correction; the §2 quotation is sub-agent-witnessed |
| **L2-10** | Test this charter's own `engine.clj:1490` / `:1504` premise against the tree and against history | Coordinator: `ls .agents/` → `memory plans prompts reading-sets*.edn rulings.md session-records skills state*.md` — **there is no `.agents/handoffs/`**. `for s in $(git log --format=%h --since=2026-08-19 -- …/engine.clj); do git show $s:…/engine.clj \| grep -n '(Random. ^long seed)'; done` → `c44d240` **1490**, `428eaed` **1504**, `878b638` **1564**, `b9d5178`/`f05f51a` **1605**. | **This charter's own premise fails twice**, and the second failure is the more interesting one: `:1490` was **CORRECT when written**, and `:1504` was true only in a two-commit window. **The citation was never wrong; it rotted, through two intermediate values, in four days, because arc 0's own commits moved it.** That is not a channel accuracy failure — it is the citation-rot class M1 addresses, mis-diagnosed by the charter as an accuracy failure. | Use as the flagship M1 example. A correct citation silently falsified by a landed refactor, twice, inside one window. | intake | **RE-DERIVED** (coordinator ran both the directory listing and the four-commit line walk in its own tree) |
| **L2-11** | Test this charter's attribution of a correction to arc-0's F-3 | `grep -rn "element in hand"` → three hits (record `:57`, ADR-0169 `:54`, prompt `:69`), **always as ruling S2(ii)'s own wording, never as a correction**. Arc-0's actual **F-3** (record `:355-364`) is *"`last-uncancelled-index` cannot ride either carrier"* — a **scope** correction against a conditional step-4 rider. Family (ii) (`:142-160`) implements the ruled mechanism exactly, gated by a four-corpus test plus a 150-trial defspec. | The charter attributes to F-3 a correction F-3 does not make and no session made. The genuine arc-0 mechanism corrections are the record's deviations 4, 5 and 10. | Correct the charter. | intake | **RE-DERIVED** (coordinator read arc-0's F-3 at `:355-364` and ADR-0169's S2(ii) wording, and confirms no retraction of "read the element in hand" anywhere) |
| **L2-12** | Resolve this charter's `run_test.clj:386-440` span | `(def ^:private gated-runs` at **`:396`**, closing `:430`; the `corpora` atom `:432-435`; `generate-corpora-once` `:437-439`; **`(use-fixtures :once generate-corpora-once)` at `:441`**. Line `:386` is `(is (= path (:path (:payload r))))`, inside the **preceding** deftest. True span **`:388-441`**. | Wrong at both ends: opens inside a different test and stops **one line short of the `use-fixtures` registration that makes it a fixture at all** — the omitted line is the load-bearing one. | Cite by symbol (`run_test.clj/gated-runs`) rather than by span. Second M1 exemplar. | close-as-fine | **RE-DERIVED** (coordinator ran the grep and read `:386`, `:396`, `:430`, `:441`) |
| **L2-13** | Resolve this charter's bare `digest.clj:575-588` | `find . -name digest.clj` → **two** (`components/oracle/…`, `components/kernel/…`). In the oracle one the vacuous-set span is correct. | Correct on substance; the bare basename does not resolve. | Cite the full path. | close-as-fine | **RE-DERIVED in part** — coordinator confirmed the tree holds two `digest.clj` and that ADR-0169 `:87-88` cites the oracle one by full path; the exact `:575-588` bullet span is sub-agent-witnessed |
| **L2-14** | Test the charter's cadence claim, and then test the charter against it | `roadmap.md:23-25` — **"approximately ADR-0174"**. `notes/adr/0159-…:478-485` — *"This close is ADR-0159. Review 5 is chartered at approximately ADR-0174. The channel's own figure of ~ADR-0169 measures 15 from the CHARTER (0154), which is neither the row's wording nor its worked precedent… **Recorded here so the next session does not average them.**"* Highest ADR in tree: **0169**. This review lands at **0170**. | The charter's cadence *correction* is **true and doubly landed — the window's best-executed premise correction.** And this review is being run **four ADRs before the tree's computed due point, in the same direction as the error ADR-0159 corrected**, over a ten-ADR window rather than fifteen. **A correction landed twice and then overridden in execution is worse than one never made** unless the override is itself recorded as a decision. | This register records it as an author OVERRIDE in its own header, not as compliance. The plan carries the ruling: does `R-review-cadence-in-adrs` gain a "may be pulled forward by author ruling, and the next due point is computed from the actual close" clause, or stay as written with this run recorded as a deviation? | ruling-needed | **RE-DERIVED** (coordinator read `roadmap.md:23-25`, ADR-0159 `:466-485`, and confirmed 0169 is the highest ADR on disk) |
| **L2-15** | Audit where each of the 40 corrections landed | 4 reached the erring artifact (ADR-0167's dated amendment `:159-175`; the plan's `:53-60` **amend-and-quote-the-original-verbatim**; the plan's `:219-225`; ADR-0163/0164's error ledgers); 4 annotated an archive; 3 opened a roadmap row; **~29 record-only**. | **Roughly three quarters of this window's premise corrections never touch the document that was wrong.** The record is honest and complete; the artifact is unamended, so the next reader of the prompt, plan or doc meets the original premise. The plan's `:53-60` amend-and-quote pattern is the good shape and is used **exactly twice** in the whole tree. | Standardize amend-and-quote as the default disposition for a correction against a LIVE artifact; leave record-only for corrections against a DATED one. Fold into L2-4's ruling. | ruling-needed | **RE-DERIVED in part** — coordinator read ADR-0167's amendment and the plan's `:53-60` quoted-original pattern; the 40-row landing audit is sub-agent-witnessed |
| **L2-16** | Cross-tab correction count against fence presence | Fenced prompts (5): 18 corrections = **3.6/prompt**. Unfenced (6): 10 = **1.7/prompt**. | The fence **converts** error from a fix-forward disclosure into a countable finding rather than reducing it. Both halves matter for scoring: the 08-24/08-25 sessions are not worse-prompted, they are better-instrumented. | State the instrumentation asymmetry in the scoreboard notes; do not score the pre-08-24 prompts as cleaner. **Adopted in this register's scoreboard.** | intake | **RE-DERIVED in part** — the ratio follows arithmetically from L2-2's ledger and L2-3's fence census, both of which are sub-agent-witnessed in their totals |
| **L2-17** | Resolve every `engine.clj:NNN` citation in a LIVE hand-authored dev doc | Coordinator resolved all twelve in `docs/dev/simulator-architecture.md` against `f05f51a`: **`:1242`→`run` is at 1426; `:1059`→`replay` at 1199; `:857`→`evolve` defmulti at 997; `:1413`→`(Random. ^long seed)` at 1605; `:1165-1217`→`assign-module` at 1381; `:1534-1541`→ a pathway-default docstring, not the participants fold; `:581-613`→ a rejection docstring, not the merge; `:67`→ the import is at 68; `:259`→ the `decide` defmulti is at 260.** Right: `:10-23` (ns doctrine), `:48-56` (determinism paragraph), `:424-460` (`decide :transfer` opens at 423). Doc last touched `c44d240`, **2026-08-21 — before the arc-0 commits that moved them**. | **At least nine of twelve citations in a live onboarding doc were silently falsified by this window's own commits, and nothing is red.** `docs/dev/` sits outside the link-footnote gate; the tree's only line-cite gate is roadmap-specific. The doc is a `:onboarding` reading-set member, i.e. what a cold session reads to learn the engine. M1's failure mode in the shipped doc tree, not merely in prompts. | Fix the nine or convert them to symbol-anchored form. A gate that resolves `<file>.clj:<n>` in a live surface against the named symbol would close the class. | fix-session-candidate | **RE-DERIVED** (coordinator resolved all twelve itself, and extended the sub-agent's six to nine) |
| **L2-18** | Ask where the window's most reusable mechanism correction is written down | PC-18 — that `ehrt sim run` pays generate **and** check inside one verb, and `ehrt sim check` is a third thing on the 1-arg arity — appears **only** at `.agents/session-records/2026-08-24-throughput-spike.md:105-127` (the sentence at `:117`). No doc, ADR, plan or row states it. | The tree's actual phase boundary, which any future performance prompt will get wrong the same way, is buried in one session record. Same shape as PC-24 (an `O(n^2)` characterization that "appears nowhere in the tree") and PC-25 (an invented docs-session dispensation). Representative of the ~29 record-only class. | Promote it to a line in `docs/dev/simulator-architecture.md` or the traffic-scale plan, so the next measurement prompt reads it instead of re-deriving it. | fix-session-candidate | **RE-DERIVED in part** — coordinator confirmed no doc/ADR/plan states the boundary (`grep -rn "already pays generate" --include=*.md .` → exactly one hit, that record at `:117`); the record passage itself is sub-agent-witnessed |

---

## Sub-agent line L-3 — Measurement discipline

Charter: enumerate every timed or counted figure recorded in the window;
for each, does it carry a health record including the host-side sample,
is it labelled MEASURED/PROJECTED where the plan requires, was it taken
unpiped, and does any later surface quote it with a precision or a status
the record does not support. Price Q4. Own fresh clone of `f05f51a`, no
probe cap, **no suite run and no benchmark** — the coordinator's timed
baseline was running and this line was instructed not to perturb it.

**Population, 37 files**, enumerated from the tree: the ten ADRs, eleven
records, eleven prompts, the traffic-scale plan, `roadmap.md`,
`state.md`, `state-derived.md`, `rulings.md`. Chain of custody then
traced tree-wide per figure.

**Headline: the window's two best measurement records are exemplary, and
the discipline that produced them has zero standing surfaces.** Every
arithmetic claim in the spike and arc-0 records re-derives independently
except one (L3-2). MEASURED/PROJECTED labelling survives every hop into
the plan and into the memory notes — including the F3-1 premise
correction that refused to flip a label that did not exist. What fails is
one class only: **a figure's KIND** (suite wall vs poly `Execution time`)
is dropped downstream, and once dropped it produces a sign error.

| id | probe | evidence | finding | recommendation | disposition | provenance |
|---|---|---|---|---|---|---|
| **L3-1** | Re-derive the KIND of every suite figure in the ADR-0167 → arc-0 chain, then re-compute the arc-0 suite delta wall-against-wall | Coordinator re-derived in its own tree. `2026-08-24-suite-time-residual-probe.md:93` `WALL = 878s (14m38s)`; `:97` `Execution time: 13 minutes 59 seconds`; its own era table `:125` is headed **"poly `Execution time`"**. Downstream: `notes/adr/0167:163` and `roadmap.md:330` quote `13m59s` **with no kind**; the arc-0 prompt then says "wall recorded against the 13m59s baseline"; `2026-08-25-arc-0-…:242-246` places `13m59s` in a column headed **`wall`** and `:250-252` derives *"the honest comparison against the 13m59s baseline is the 14m26s mean… **+24 tests, +240 assertions, +27s (mean)**"*. Arithmetic: mean(875, 857) = **866 s**; 866 − 839 (poly) = **+27 s**; 866 − 878 (**wall**) = **−12 s**. | **A poly `Execution time` was quoted as a wall three hops downstream, and arc 0's headline suite-cost delta is the artifact of comparing a wall to an execution time. Wall-against-wall the post-arc-0 suite is 12 seconds FASTER than the ADR-0167 baseline, not 27 slower.** The narrative built on it ("the suite got slower because the proof was added to it") may still be true — the arc did add 24 tests — but it has no support from the numbers as compared. ADR-0167's own before/after table keeps wall and poly in separate columns; the amendment prose and the roadmap row are where the qualifier was dropped. | Fix-forward the arc-0 record to compare like with like (baseline wall 878 s, delta −12 s mean) or to say the arc's poly figure was not recorded; add "poly `Execution time`" to `notes/adr/0167:163` and `roadmap.md:330`. **Standing: a recorded timed figure names its kind.** That clause, not the host sample, is the load-bearing half of Q4. | fix-session-candidate | **RE-DERIVED** (coordinator read `:88-100` and `:122-135` of the residual probe and `:225-260` of the arc-0 record, and re-ran the three subtractions) |
| **L3-2** | Re-derive the headline speedup from the record's own per-run figures | Coordinator: `2026-08-25-arc-0-…:285` gives generate 102.137 / 100.223 s and check 7.111 / 7.408 s → means **101.180 / 7.2595**, after-total **108.4395 s**. Baseline 324.09 + 711.09 = **1035.18 s**. 1035.18 / 108.4395 = **9.5462**. From the record's own rounded minutes, 17.3 / 1.81 = **9.5580**. Published **9.58×** at `:12`, `:278`, plan `:205`, `roadmap.md:326`, `:472`, and in the memory note's own description field. `1035.18 / 9.58` would need an after-total of **108.056 s**. | **9.58× is not reproducible from any combination of figures in the record.** Correct value **9.55×**, overstated by 0.35%. Every component figure re-derives exactly (3.20×, 97.9×, 1,036 ev/s, 14,442 ev/s, 6.7%), so this is an arithmetic slip in the derived headline, not a measurement problem — and it is **the single most-quoted number of the window**, propagated to five surfaces. | Correct to 9.55× on all five surfaces in one edit, or state the rounding basis that yields 9.58. | fix-session-candidate | **RE-DERIVED** (coordinator recomputed all of it from `:285` and the spike's `:183`) |
| **L3-3** | Trace the "3.1× a GitHub runner" figure to the run it was taken on, and read that run's own disclosed conditions | Coordinator read `2026-08-24-suite-time-doubling-diagnosis.md:146` — **both profiled runs are `script -qfe -c "make test" /dev/null \| awk …`**, i.e. through a pty AND a pipe; `:184` prices the pty at **~11%**; `:186` says runs 3–5 "use the repo's own unpiped convention", so the profiled pair is not among them. The 489 s sits in a column headed **"uncontended"** (`:158`) and is **pre-reboot** — `residual-probe:133` later proves a further **1.36×** of host contention was still resident. `:249-251` then states *"penny is also **3.1× slower than a GitHub runner on the same namespace uncontended** (489s vs 157.7s)"*. `notes/adr/0167:181-182` carries it forward as *"just penny's speed"* while explicitly saying it is *"not re-measured here"*. | A figure taken **piped through a pty** (11% tax, self-disclosed on the same page) and **pre-reboot** (1.36×, proven the next day) is labelled **"uncontended"** and promoted to a machine property across three surfaces. Deflating both: 489 / 1.11 / 1.36 ≈ **324 s → ~2.05×**, not 3.1×. ADR-0167's amendment is honest that it did not re-measure; what it does not disclose is that the number it carries forward was never taken under the conditions its own label claims. | Re-measure the namespace unpiped on a verified-quiet penny (one `poly test`, ~5 min), or downgrade the claim everywhere to "~2×, derived from a pty-taxed pre-reboot run, never re-measured". Do not leave "uncontended" standing at `:158`/`:251`. | fix-session-candidate | **RE-DERIVED** (coordinator read `:143-190` and `:240-258` of the diagnosis record and `:159-193` of ADR-0167's amendment) |
| **L3-4** | Count the arc-0 commits four ways | See **D1-2** — the coordinator found this independently before the sub-agent reported. `git log --oneline d49f1c6..f05f51a \| wc -l` = **8**; record `:6` says five, `:442` and `:477` say seven; the memory note says six. | One counted figure, four values, none of them the tree's. | See D1-2. Also a candidate standing rider: a close record's commit count is derived from `git log <base>..HEAD` at close time. | fix-session-candidate | **RE-DERIVED** (independently, before the sub-agent's return) |
| **L3-5** | Does `roadmap.md`'s 9.58× row have a resolving citation chain to a health record? | Coordinator: `grep -nE '101\.2\|7\.26\|9\.58\|1\.81\|14m\|host load\|LoadPercentage\|quiet' notes/adr/0169-*.md` → **one hit, and it is the spike's slope 1.814**, not any of the arc's own figures. `grep -nE 'session-records\|2026-08-25' notes/adr/0169-*.md` → **one hit, `:8`, and it is the SPIKE record** — the arc-0 session record is cited nowhere in the ADR. | ADR-0169 records the decision and the spike's context and **carries none of the arc's own outcome figures and no pointer to the record that holds them**. So `roadmap.md`'s 9.58× resolves to an ADR that does not contain it, and the F7 host-load disclosure ("cell C ran at 21–30%, biasing against the claim") is unreachable from the roadmap by any hop the documents provide. The plan appendix carries it correctly; **the ADR is the broken link.** Converges with **D1-7** by a different route. | One line in ADR-0169's Consequences recording the measured outcome and citing the arc-0 session record, with the elevated-host disclosure named — the natural place to also land L3-2's corrected 9.55×. | fix-session-candidate | **RE-DERIVED** (coordinator ran both greps in its own tree) |
| **L3-6** | Test the plan appendix's umbrella run-parameter block against the rows it claims to cover | Coordinator read `.agents/plans/2026-08-24-traffic-scale-program.md:130-131`: *"**Run parameters common to every MEASURED figure below.** 2026-08-24, penny… **host verified quiet before each cell**."* The 2026-08-25 arc-0 MEASURED row sits **below** it at `:195` and at `:212-214` discloses *"taken at Windows host load 21–30% against the baseline's 4/3/3, which biases against the speedup, not for it."* | The umbrella asserts "host verified quiet before each cell" over a figure for which that is false. The row corrects it eighteen lines later and names the bias direction honestly — so the file is **self-correcting for a reader who finishes the bullet and misleading for one who reads the umbrella and skims the table**. | Narrow the umbrella to "every MEASURED figure **dated 2026-08-24** below" and let the 08-25 row carry its own parameters, which it already does. One line. | fix-session-candidate | **RE-DERIVED** (coordinator read `:126-145` and `:195-218`) |
| **L3-7** | Locate `26m39s` — the low endpoint of ADR-0167's "doubled" band — in a tracked record | Coordinator: `grep -rn '26m39s' --include='*.md' .` → **four hits, none of them a measurement**: `notes/adr/0167:9`, `prompts/2026-08-24-suite-time-doubling-diagnosis.md:6`, `prompts/2026-08-23-unpaired-…:171` ("corrected make-test wall-clock"), and `session-records/2026-08-23-unpaired-…:54`, which calls it **"the prompt's own … '26m39s'"** — i.e. a design-channel number. The band's other endpoint, `31m04s`, **is** a measurement — a **wall** (`08-23-unpaired:44`, whose poly on the same row is 29m46s). `residual-probe:128` silently re-states the band as poly **26m41s–27m09s**. | **ADR-0167's Context sentence builds its "doubled" band from an unsourced design-channel number and a measured WALL, then compares it against an all-POLY band.** The residual probe corrected the band the next day; ADR-0167 still stands uncorrected. Same defect class as L3-1, one ADR earlier — which is what makes it systemic rather than a slip. | Correct `notes/adr/0167:9` to the residual probe's own poly band and drop `26m39s`, which no record supports. | fix-session-candidate | **RE-DERIVED** (coordinator ran the tree-wide grep and read the 08-23 record's own attribution of the figure to the prompt) |
| **L3-8** | Count the tracked 2026-08-23 suite runs from the two records that hold them | Coordinator read both: `2026-08-23-unpaired-…:38` says *"**Three** full runs this session"* and its own table at `:43-47` lists **FOUR** rows (baseline / commit 1 / commit 2 / final). `2026-08-23-generator-side-…:23-25` lists **TWO**. Total tracked = **6**. `notes/adr/0167:7` says *"**Five** tracked `make test` runs across two sessions on 2026-08-23"*; `residual-probe:128` repeats "five runs". | A counted figure that contradicts its own table in the same record (3 vs 4), and a downstream count (5) matching neither the tables (6) nor any stated exclusion. | Reconcile: state 6 with the two records enumerated, or name the excluded run and why. Trivial, and rowed because "how do I know this is all of them?" is the question this whole register is built on. | fix-session-candidate | **RE-DERIVED** (coordinator read `:34-56` of the unpaired record and `:20-28` of the coverage record and counted the rows) |
| **L3-9** | Ask whether "measured live set 190 MB" is a measurement or a normalization | `throughput-spike.md:423-427` measures **109.0 MB** retained and **90.5 MB** per replay vector **at 104,851 events** (per-event 1.065 KB and 0.883 KB). The table at `:445-447` renormalizes to a round 10^5 (104 + 86 = **190 MB**, 4.8%) in a section headed *"Does held-whole survive 10^5?"* whose sibling row is an explicit 10^6 projection. The direct sum at the actual event count is **199.5 MB (5.1%)**. Quoted flat as measurement at plan `:103`, `:192` and **`notes/adr/0169:23`**. | A figure derived by renormalizing measured per-event constants to a round N is presented flat as "measured", including on the ADR. The spike record is honest about the derivation in its surrounding prose ("the retained figures… are the ones extrapolated from"); the plan and ADR are not. Immaterial to every conclusion drawn from it (4.8% vs 5.1% of a 3.88 GB ceiling). | Either quote 199.5 MB at 104,851 events, or keep 190 MB and say "at a nominal 10^5, from measured per-event constants". | close-as-fine | **RE-DERIVED** (coordinator read `:418-450` and `notes/adr/0169:20-25`) |
| **L3-10** | Determine whether ADR-0167's host-side-sample lesson has any standing surface, and what `bin/preflight` actually samples | Coordinator read `bin/preflight` whole — five checks: last-five CI runs, edit root + `core.fileMode`/`core.ignorecase`, tree-clean, HEAD-vs-remote, last `stable-*` tag. **No `uptime`, no `/proc/stat`, no Windows sample, no machine-health check of any kind.** `for f in AGENTS.md .agents/rulings.md .agents/state.md .agents/skills/build-session/SKILL.md bin/preflight .agents/skills/session-prompt/SKILL.md .agents/skills/repo-review/SKILL.md; do grep -ciE 'LoadPercentage\|host-side\|verified.quiet\|wslhost\|host load' $f; done` → **0 0 0 0 0 0 0**. `rulings.md:270-272` (`R-full-suite-before-push`) mandates unpiped + MAKE_EXIT + the wrapper's exit clause and **nothing about machine state**. The lesson exists only at `notes/adr/0167:187-189`, in two archived one-shot prompts, and in the records those prompts produced. | **The discipline that produced the window's two best measurement records is carried entirely by prompt authorship.** The two 08-23 sessions had no such clause and produced two figures asserted quiet on Linux-side evidence alone, later proven taken under an orphaned `wslhost` holding half the machine. The two disciplined sessions are evidence the convention WORKS, not evidence it is durable. Direct corroboration at **D2-5**: arc 0 cited `R-full-suite-before-push` for an obligation that ruling does not carry. | **Q4, priced.** Options and a recommendation are in the plan; the sub-agent's recommendation is **C+B** — a `bin/host-sample` script cited by a two-line rider on `R-full-suite-before-push` and `build-session`'s gate-run bullet, with `bin/preflight` calling it once at session start — and its decisive argument against preflight-alone is arc 0's own three-sample table (23/16/29 at session start, 1/4/4 pre-suite, **21/30/25 pre-cell-C**): preflight fires at the one moment least likely to be the moment of the figure. | ruling-needed | **RE-DERIVED** (coordinator read `bin/preflight` whole, ran the seven-file grep itself — all zero — and read `R-full-suite-before-push` verbatim) |
| **L3-11** | Independently re-derive every arithmetic claim in the spike and arc-0 records that a later surface quotes | Sub-agent re-solved from the published walls alone: exponents 0.989 / 1.4712 / 1.7866 / 1.8144 (published 0.989 / 1.471 / 1.786 / 1.814); two-term fit generate a=2.150e-4 b=2.743e-8, check a=3.984e-4 b=6.088e-8, ratio 2.219 ("2.2×"); 10^3 back-prediction 0.703 s ("0.7 s"); 10^6 projection 88,923 s = **24.70 h** with 99.31% quadratic ("24.7 h / 99.3%"); linear-only 10^6 613.4 s = **10.22 min** ("10.2 min"); check share 7.26/108.44 = **6.69%** ("6.7%"). | **Every figure re-derives except L3-2's headline.** Both records carry full health records including the Windows-side sample; the spike **re-samples before each cell** and discloses that its attribution run ran at 29/20/14 with the evidence that contention did not distort it; arc 0 discloses cell C at 21–30% **and names the bias direction**. This is the standard the rest of the window should be held to. | Record as the reference exemplar; the spike's Step 0 and arc-0's F7 table are the model the Q4 mechanism should mechanize. | close-as-fine | **RE-DERIVED in part** — coordinator independently recomputed the check share (6.69%), the throughputs (1,036 ev/s) and the three speedups; the exponent and two-term fits are sub-agent-witnessed |
| **L3-12** | Check the out-of-repo memory surfaces against the tree | Sub-agent: `memory/reference_make_test_runtime.md:8-10` is **the only surface in or out of the repo that keeps wall and poly distinct for the 13m59s figure**, but its scope line says "~14 minutes at HEAD" one arc stale. `memory/project_arc_0_performance_closed.md:14-17` quotes 9.58× and the cell-C figures with **no mention of the 21–30% host-load disclosure**. | The memory note is MORE precise than the repo on the very distinction L3-1 found broken — it is what the arc-0 prompt should have read instead of `roadmap.md:330`. The arc-0 memory note drops the health record, so no surface between the roadmap and the session record carries it (see L3-5). | Refresh the runtime note to the arc-0 close figures with kinds named; add the cell-C host-load disclosure and its bias direction to the arc-0 note. **Outside the repo's remit for a fix session** — handed to the memory surface, not rowed for execution. | intake | **RE-DERIVED in part** — the memory files are outside the repo and outside this register's audit population; the coordinator confirms the in-repo half (no surface between `roadmap.md:326` and the session record carries the host-load disclosure) and records the memory half as sub-agent-witnessed |

---

## Scoreboard — reviews 1–5

| dimension | r1 (08-07) | r2 (08-09) | r3 (08-15) | r4 (08-18) | **r5 (08-25)** | movement |
|---|---|---|---|---|---|---|
| D1 — Claim-reality coherence | GREEN | GREEN | YELLOW | GREEN | **RED** | **regressed, two steps.** The GENERATED half is spotless — `state-derived.md` regenerates byte-identically, `docsgen` byte-clean across all twelve leaves, the mirror identical, every count sampled re-derives. The HAND-WRITTEN half has no clock at all: ≥9 of 12 `engine.clj:NNN` cites in a live onboarding doc silently falsified by this window's own commits (L2-17); a live roadmap row citing three findings into an ADR that contains none (D1-1); a poly execution time quoted as a wall three hops down, flipping a suite delta from −12 s to +27 s (L3-1); a "3.1× a GitHub runner" figure labelled *uncontended* from a pty-taxed pre-reboot run (L3-3); a headline 9.58× that no combination of its own record's figures reproduces (L3-2). **No gate anywhere resolves a prose citation or a figure's kind.** |
| D2 — Guard coverage | YELLOW | YELLOW | YELLOW | RED | **YELLOW** | **improved.** Everything that made review 4 red is closed, and re-derived here: `docsgen_closure_test` now carries **six** deftests — write-set vs CI diff-list as one *population* (`:193`), `event-schema-freeze` **not reachable from docsgen** (`:228`, review 4's L3-2), `state-derived`'s declared dependency on `pipeline` (`:244`, L3-4), the use-case page set equal to the case ids (`:262`, L3-9), and the palgebra example population (`:300`, L3-10) — replacing the two per-artifact assertions review 4 found. The mirror gate is complete, and this window's `rulings.md` diff is **append-only with every row attributed** (W-11 could not even fire). Born-red discipline deepened to a two-directional witness and bifurcated correctly for pure refactors — and neither half has a ruling row (D2-4). What remains: **11 of 12 new rulings rows are ungateable doctrine** in a category the register has never classified (D2-1), and one session cited a ruling for an obligation it does not carry (D2-5). |
| D3 — Environment independence | YELLOW | YELLOW | YELLOW | YELLOW | **YELLOW** | unchanged in colour. `R-preflight-fail-closed` was exercised twice for real and behaved exactly as designed; `bin/post-push-verify`'s reflog assumption becomes true under the new push ruling. One new finding, and it is a timing one: a Step-0 preflight can be taken **after** the session's own edits, and the script cannot tell (D3-2). |
| D4 — Error honesty | RED | GREEN | GREEN | GREEN | **GREEN** | unchanged, and with a positive control worth recording: arc 0's two new index reads fall back **on the key, never on a missing entry**, deliberately routing a would-be silent nil into a loud gate failure, with the reason written in-source (D4-1). |
| D5 — Mirror and derivation drift | GREEN | GREEN | RED | YELLOW | **YELLOW** | unchanged in colour, content improved. Both of review 4's landed banner fixes are intact ten ADRs later; `docsgen` is idempotent and byte-fresh. What did not happen is generalisation: the `state-derived` self-listing treatment is **still the only one** (W-8), and a live plan carries a MEASURED row its own file inverts 25 lines later with no supersession marker (D5-5). |
| D6 — Sampling adequacy | YELLOW | YELLOW | GREEN | YELLOW | **RED** | **regressed.** Two gates landed by ADR-0163 to catch unpaired end-steps are **vacuous at HEAD over a candidate population of zero**, one of them with a docstring calling itself "the ONLY population-scale exercise" of a thing it exercises none of (L1-1) — and the tree says so in a comment four hundred lines above them. The suite's flagship 300-trial catalog defspec is blind to **11 of 21 event kinds**, including the invariant ADR-0166 added to it inside this window (L1-2). Arc 0's own equivalence defspec compares `[] = []` on a **majority** of trials for two of the six invariants it exists to prove (L1-4). Against all that: arc 0's four gated-corpus/defspec pairs are the **best-guarded gates in the repo** — the only ones whose thinness is measured *and pinned* (L1-6). The dimension is red because the mechanism is missing, not because the practitioners are careless. |
| D7 — Continuity integrity | GREEN | YELLOW | YELLOW | YELLOW | **YELLOW** | unchanged in colour, and the aging is the concern. **Six** of thirteen inherited watch rows fired. `#two-clocks-asset-field-audit` has now outlived **three** arcs (W-4); the reader-path fence census did not move by a single fence in ten ADRs (W-5); the unnamed-closure ratio went **18% → 50%** (W-3); and Q4 has been live in the design channel for a day with no roadmap row, against a rule that exists to prevent exactly that (D7-1). The watch-list MECHANISM, by contrast, worked: every row that could fire had a probe that could see it. |
| D8 — Operator experience | GREEN | YELLOW | YELLOW | YELLOW | **YELLOW** | unchanged. Every CLI probe green against the built binary; the README's own demo path run for real, including the never-overwrite contract, matching its prose word for word; front-door bare fences still **0** without the exempt count rising. W-5 is the only mark against it, and it is aging rather than regression. |

**Overall: review 1 was 4 green / 3 yellow / 1 red. Review 2, 3 green /
5 yellow / 0 red. Review 3, 2 green / 5 yellow / 1 red. Review 4, 2
green / 5 yellow / 1 red. Review 5 is 1 green / 5 yellow / 2 red.**

**This is the worst scoreboard of the five, and the window it scores
contains the best engineering.** Both statements are true and they are
the same statement. Arc 0 proved a three-family refactor byte-identical
at 104,851 events and disclosed ten deviations by number; ADR-0166
witnessed its invariant red in both directions; ADR-0167 refused to
blame the tree until it had sampled the host. **Every red below is a
missing mechanism, not a missed practice** — which is what
`rulings.md#R-severity-tracks-mechanism` instructs the score to track.

### The cross-dimension pattern — and it is one level up from review 4's

Review 3's thesis: *a probe, gate, or tool whose population is a
registry rather than the tree.* Review 4's: *a gate's population
standing in for the class it is believed to enforce.* Review 5's:

> **A claim that was TRUE when it was written, that nothing keeps true.**

Every one of the following was correct on the day it landed, was
silently falsified by a later landed change, and is green:

- `engine.clj:1490` → `:1504` → `:1564` → **`:1605`**, four values in
  four days, because arc 0's own commits moved the line (L2-10).
- Nine-plus `engine.clj:NNN` cites in `docs/dev/simulator-architecture.md`,
  falsified by the same commits (L2-17).
- *"the ONLY population-scale exercise of the `:care-plan-end` half"* —
  true when ADR-0163 wrote it, **zero** at HEAD, because ADR-0163's own
  fix removed the events (L1-1).
- *"the 14 independent `engine/replay` calls"* — 14 at `d49f1c6`, **11**
  at `f05f51a`, on the PRIORITY 1 open row (D1-5).
- *"`~19m` is penny's honest `make test` figure at HEAD"* — falsified
  **within the day** by a reboot (ADR-0167's own amendment).
- The `#register-gate-row-ownership` row's *"five continuation lines now
  sit under the wrong slug"* — they sit in the attic now (D7-2).
- The plan's *"MEASURED (2026-08-24) — where the time goes"* ranking,
  inverted by arc 0 twenty-five lines below it (D5-5).
- *"Seven commits, local only — no push, no tag"*, in a record whose own
  correction commit made it eight and pushed (D1-2).

**The repo's gates ask "is this true?" at authoring time. Almost none
ask "is this STILL true?"** — and the exceptions prove it exactly: the
generated surfaces, which regenerate-and-diff on every push, are the one
part of this tree with nothing wrong in it at all. Review 4's fix arc
built the closure gates that made that so. **The same treatment has
never been applied to a hand-written claim**, and this window — ten
ADRs, one large refactor, four days — is what a repo with no clock on
its prose looks like when something finally moves fast.

That is the finding this review would put in front of the author above
all others, and the plan's Session A exists for it.

---

## Register summary

**88 total rows.** Per section, counted mechanically from every row
whose first cell is a `D<n>-<id>` or `L<n>-<id>` label, disposition read
as the cell that exactly equals one of the four tokens — the same
extraction used on review 4 in AR-RR5-1, and **not** a tally kept while
writing.

| section | rows | close-as-fine | fix-session-candidate | ruling-needed | intake | pointer |
|---|---:|---:|---:|---:|---:|---:|
| D1 — Claim-reality coherence | 8 | 1 | 6 | 1 | 0 | 0 |
| D2 — Guard coverage | 6 | 2 | 1 | 3 | 0 | 0 |
| D3 — Environment independence | 4 | 3 | 1 | 0 | 0 | 0 |
| D4 — Error honesty | 3 | 3 | 0 | 0 | 0 | 0 |
| D5 — Mirror and derivation drift | 5 | 3 | 0 | 1 | 1 | 0 |
| D6 — Sampling adequacy | 4 | 2 | 2 | 0 | 0 | 0 |
| D7 — Continuity integrity | 8 | 2 | 3 | 1 | 1 | 1 |
| D8 — Operator experience | 5 | 4 | 1 | 0 | 0 | 0 |
| L-1 — Gate vacuity | 15 | 5 | 8 | 1 | 1 | 0 |
| L-2 — Premise-correction ledger | 18 | 3 | 8 | 3 | 4 | 0 |
| L-3 — Measurement discipline | 12 | 2 | 8 | 1 | 1 | 0 |
| **Total** | **88** | **30** | **38** | **11** | **8** | **1** |

30 + 38 + 11 + 8 = **87** disposition-carrying rows, **+1** pointer row
(D7-8, which routes to the watch-list table and carries no disposition of
its own) = **88**, matching the row count exactly.

**Sub-agent provenance, counted the same way** (45 sub-agent rows):
**29 fully re-derived** by the coordinator in its own tree, **16
re-derived in part** (the finding's mechanism, population or cited
artifact confirmed here; multi-trial instrumentation and live-run
censuses labelled sub-agent-witnessed), and **0 recorded as "coordinator
could not reproduce"**. Two rows were promoted from in-part to full after
the timed suite finished and a JVM could be spent safely — L1-7's
`:citation`-filtered depth (the union reproduces exactly, and the
one-deep set is **five** types in two runs against the live roadmap row's
three in one) and L1-15's 21-kind schema enumeration. **No sub-agent
claim in this window failed coordinator re-derivation**; the one that was
contradicted was contradicted by the wrong instrument, not by the tree.

*Every figure in this section was produced by extraction from the rows
above. **Review 6 should re-derive them from the rows regardless**, and
should read AR-RR5-1's method note first: re-derive against the register
as FIRST COMMITTED, because this repo's fix sessions overwrite
disposition cells in place.*

**One sub-agent claim was contradicted by the coordinator's first probe
and then confirmed by its second**, and the reason is recorded at D6-4:
a `grep` over an EDN log is not a census of that log. Every event count
in this register was produced by parsing.

---

## Probes not run, per dimension

Enumerated because the rubric requires it, not because the budget ran
out. **No dimension exhausted its 12-probe budget**; the highest was D1
and D7 at 8. Budget used: D1 8, D2 6, D3 4, D4 3, D5 5, D6 5, D7 8, D8 6
— **45 of 96**, plus the three uncapped sub-agent lines.

| dimension | probes not run, and why |
|---|---|
| D1 | The remaining ~120 `<file>:<line>` citations on live surfaces outside `docs/dev/simulator-architecture.md` were **not** resolved. L2-17 sampled one file exhaustively and found ≥9 of 12 stale; the population-wide sweep is Session A's own first act, not this review's. |
| D2 | The **full 126-row rulings→gate map** was not built. Review 4 named this as never run by any review with no artifact in the repo holding one; it is still true. This review mapped only the **12 rows added in the window**. |
| D3 | `HOME`-repointed clone (a genuinely cold machine) — out of scope by the rubric's own statement of `ci-parity`'s limit. `.gitattributes` byte-determinism was not re-probed; unchanged since ADR-0157. |
| D4 | The nil-returning-I/O sweep covered only the **16 src files changed in the window**, not the tree. Review 4's tree-wide sweep is the standing baseline; nothing in this window's diff suggests it moved. |
| D5 | The **109** banner-carrying files were enumerated but not individually resolved against their generators; only the eight docsgen-leaf banners were read. |
| D6 | The **80 pre-existing `defspec`s** were not sampled beyond `every-m1-run-satisfies-the-invariant-catalog`. W-7 is answered for arc 0's three and open for the rest. |
| D7 | The citation-resolution sweep was scoped to the window's surfaces plus the thirteen watch rows; the tree-wide sweep last ran at review 4. |
| D8 | Help at **80 and 120 columns** was not run (40 was, as the narrow case). The `make quickstart` network path was not executed — the README itself discloses it as a local/manual check with no CI workflow. The **46 bare fences** were counted, not executed; that is `roadmap.md#reader-path-fence-battery`'s own chartered work. |

**The two limits worth the author's eye**, both carried from review 4
unchanged: the **126-row rulings→gate map** that no review has ever
built, and — new this review — the fact that **no probe in this rubric
can currently detect a stale prose citation**, which is why the pattern
above went eight instances deep before anything found it.

# Architecture & Refactoring Review — 2026-07-30

**Scope.** A deliberate review of the whole workspace at HEAD
`65b550b` ("doctor pretty rendering"), producing findings and dated,
liftable work items — not executing any refactoring. Method per the
session prompt: live probes over prose (R-7), every structural claim
grounded in a command run this session (Evidence appendix, §6), stale
docs proposed as fix-forward errata (R-6), every proposed task named
with its co-landed invariant (R-5).

**Workspace state at review start: fully green.** `clojure -M:poly
check` OK; per-push lane (`poly test :all skip:integration`) green,
185 `Testing ehrt.*` namespaces; integration lane (`poly test :all
project:integration`) green on this machine's warm artifact cache.
A review on a green workspace — no red findings to dispose of first.

**Environment disclosures.**
- Two live clones exist on this machine (the known dual-clone fact).
  At session start the WSL ext4 clone (`~/src/ehr-testing-tools`, the
  build/git clone) was one commit behind origin and was fast-forwarded
  to `65b550b` before any probe ran. The `/mnt/c` clone carries
  **uncommitted work-in-progress** in `bases/cli/src/ehrt/cli/core.clj`
  and its test — a continuation of the doctor-rendering session's
  step-3 `:hint` work. This review is of committed HEAD only; that WIP
  is disclosed here so it isn't lost or mistaken for drift.
- This file was written identically to both clones so neither diverges.

**Vocabulary reconciliation (R-1, applied via R-7).** The live code
defines two distinct value vocabularies, and this report uses them as
the code does:
- the **result envelope** (`ehrt.kernel.result`, and sim's own
  documented copy `ehrt.sim.result`): `:ok / :rejected / :error`;
- the **judge verdict** (`ehrt.judge.finding/Verdict`, ADR-0010):
  `:pass / :rejected / :indeterminate (reserved, no producer) /
  :no-verdict` with a `:cause` present iff `:no-verdict`
  (Malli-enforced, `valid-cause-pairing?`).

The review brief's own R-1 phrasing ("`:accepted` / `:rejected` /
`:error` / `:no-verdict`") conflates these two enums; the live enums
above are what `components/judge/src/ehrt/judge/finding.clj:38-69`
actually encodes, so proposals below use them. This is exactly the
"unearned specificity" species R-7 warns about, caught by probe.

---

## 1. Summary diagnosis

- **Alignment: partially aligned, trending well.** The storefront
  (README, `docs/what-is-this.md`, `docs/use-cases.md`) makes claims
  the CLI overwhelmingly honors — every documented command strip I ran
  verbatim worked exactly as written, including the newest
  (`gate v2-nist`, exit 3 with `:no-verdict`/`:profile-spec-error`,
  precisely as its use-case note promises). The misalignment is
  one-directional: **the maintainer/agent docs lag the last 48 hours
  of landings** (judge-v2-nist, ADR-0013/14/15), not the other way
  around.
- **The recurring defect species is stale citation, not wrong
  architecture.** Four user-path docs still cite pre-Polylith paths
  (`test/ehr_testing_tools/...`, `docs/experiments/...`); the live
  facts register cites a dead evidence path in 3 of 9 rows and has no
  row for any of the four newest landings; `AGENTS.md` and
  `docs/dev/architecture.md` both still call the NIST engine a
  "future addition." The threads are unraveled *records*, mostly, not
  unraveled *code*.
- **`tools` is now two things wearing one name**: a corpus domain
  component *and* the CLI's universal façade (its interface re-exports
  kernel and all three judge engines; `bases/cli` requires nothing
  else). 74 interface defs over 30 namespaces spanning five concerns.
  A staged split is warranted and the workspace already owns the
  method for it (ADR-0011's characterization-then-extract).
- **The judge family is structurally parallel where it was designed
  together and asymmetric where it wasn't**: the newest engine
  (judge-v2-nist) is the only one whose component-level gate functions
  neither return the kernel result envelope nor get their findings
  validated against the shared `Finding` schema; the base compensates,
  so the shipped CLI is fine, but the component boundary violates R-2
  for any non-CLI consumer.
- **Sim's home question is settled: consolidation, verified from git
  ancestry** — all 41 `ehr-testing-sim` commits are ancestors of this
  workspace's HEAD; content diff after mechanical rename is ~zero. The
  residual risk is external: the frozen sibling repo carries **no
  archive marker** and still asserts pre-merge rules as live.
- **Enforcement is real but has two promised-and-missing gates**: no
  generated-doc freshness check exists anywhere (a test's own prose
  claims CI has one), and no automated check ever executes a use-case
  strip (the "was run, once, locally" contract is author discipline
  plus commit evidence — honest, but one landing away from silent
  staleness, as the 48-hour doc lag demonstrates).
- **Velocity context:** 74 commits in the last 3 days (430 total).
  The docs/facts/ADR apparatus is built for this pace but its
  *catch-up half* (freshness gates, register rows, on-ramp currency)
  hasn't kept up with its *decision half* (ADRs, deviation records),
  which is current and excellent.

---

## 2. Key findings

1. **[verified] Workspace green end to end.** `poly check` OK; 185
   test namespaces green per-push; integration green. (§6 A.)
2. **[verified] `AGENTS.md` — the designated agent on-ramp — is stale
   on structure it promises to keep current.** Its "Landed so far"
   section (`AGENTS.md:42-44`) says a NIST engine "is a named future
   addition (EXP-D3)" and lists `projects/ehrt-cli` without
   judge-v2-nist; `poly ws get:components:keys` lists
   `judge-v2-nist` landed (ADR-0012, 2026-07-30).
   `docs/dev/architecture.md`'s mermaid diagram and bricks table
   (lines 25-62) have the same gap — that page self-discloses drift
   and defers to `poly ws`, but AGENTS.md is named there as the
   "kept current" counterpart, and it isn't.
3. **[verified] `docs/dev/components.md`'s NIST section is actively
   wrong**: "Role in pipeline: … adoption blocked pending NIST
   license confirmation… Deliberately not used for: Anything today —
   not yet adopted" (lines 130-155). `gate v2-nist` is a shipped,
   documented CLI verb backed by that engine. This also breaks
   `docs/dev/engine-onboarding.md`'s own checklist item 6 in spirit
   (the engine's section exists but describes the pre-adoption
   world).
4. **[verified] A stale-path family violates ADR-0010's user-path
   doctrine in four docs**: `docs/judge-calibration.md:5,7,58`,
   `docs/locators.md:265`, `docs/dev/components.md:95,119,179`,
   `docs/dev/source-sink-design.md:10-22,373,572` cite
   `test/ehr_testing_tools/...`, `test-integration/...`,
   `src/ehr_testing_tools/...`, and `docs/experiments/...` — none of
   which exist. The real homes are
   `components/tools/test/ehrt/tools/v2_contract_pairing_test.clj`,
   `projects/integration/test/ehrt/tools/contract_pairing_test.clj`,
   and `components/tools/docs/experiments/`. `docs/judge-calibration.md:75`
   additionally links "facts register F22" into
   `notes/facts-register.md`, which has rows F1–F9 only (F22 is
   `notes/tools/facts-register.md`'s row — an origin-qualification
   miss). `docs/judge-calibration.md:50` also still says the NIST
   engine is "not adopted."
5. **[verified] The facts register has drifted below its own
   discipline.** F4/F5/F6 cite `docs/experiments/EXP-D3-results.md`
   (path doesn't exist); F7's "177 namespaces" is now 185 (probe,
   §6 A); no F-row exists for any of the four 2026-07-30 landings
   despite verifiable claims in their commits (player byte-identity,
   cold-start envelope pins); `artifacts.lock.edn:33-35`'s block
   comment still says the NIST coordinates are "Not yet consumed by
   any src/ code." F3's 15-command SETUP walk predates `out/`,
   `show`, `play`, and the sim default.
6. **[verified] Judge-family asymmetries, all in the newest engine.**
   (a) `ehrt.judge-v2-nist.v2/gate-file` slurps its argument — a
   missing file **throws `FileNotFoundException` across the component
   interface** where `judge-v2-hapi/gate-file` returns
   `kernel/error :file-not-found` (R-2). `bases/cli` pre-checks
   `.isFile` so the CLI is unaffected — the violation bites only
   library consumers (audience 6). (b) nist's `gate-dir` returns a
   bare `{filename result}` map (no kernel envelope) and walks
   **recursively** (`file-seq`); hapi's returns
   `kernel/ok {:results [...]}` and walks **flat** (`.listFiles`).
   (c) nist's findings are never validated against
   `ehrt.judge.finding/Finding` anywhere (hapi's test does exactly
   this, `v2_test.clj:61`), and judge-v2-nist has no dependency on
   `judge` at all — its `:cause :profile-spec-error` and the enum in
   `judge.finding/Cause` are coupled by convention only.
7. **[verified] `verdict-cache`'s stated home justification did not
   materialize.** ADR-0011 kept it in `judge` because "the planned
   NIST v2 engine is its expected second consumer" (disclosed for
   post-hoc veto); judge-v2-nist landed without touching it. Sole
   consumer remains `judge-fhir-official`.
8. **[verified] The NIST engine's jars resolve through two channels
   at once, and `doctor` exposes the seam.** The six jars are Maven
   deps (`:mvn/repos nist-hit` in three project `deps.edn`s — how the
   engine actually loads) *and* six `:kind :engine` rows in
   `artifacts.lock.edn`. `ehrt doctor` exits 1 on this machine ("6
   not cached… run: ehrt artifact fetch --all") while `gate v2-nist`
   runs fine from `~/.m2`. This contradicts
   `docs/dev/engine-onboarding.md` checklist item 4's "resolves to
   **exactly one** of" the three lockfile targets.
9. **[verified] Sim is a consolidation, and the frozen sibling repo
   is unmarked.** All 41 sim commits are ancestors of HEAD
   (`git merge-base --is-ancestor` + `git rev-list … --not HEAD` = 0);
   post-rename content diff is zero for the core namespaces. The
   sibling clone (at `Documents/o/ehr-testing-sim`, HEAD `213abaa` =
   `origin/main`) has no archive/tombstone marker, and its
   `AGENTS.md:107` still asserts the pre-merge dependency rule as
   live. Not a two-canonical-homes P1 — but an unmarked frozen twin
   that reads as live is the same hazard one reader away.
10. **[verified] Two promised enforcement gates don't exist.**
    (a) No generated-doc freshness check (regenerate +
    `git diff --exit-code`) exists in either workflow, the Makefile,
    or the hooks — while
    `components/tools/test/ehrt/tools/docsgen_test.clj:20-22`'s prose
    asserts CI has one, and the carve-loss audit lists it
    "SUPERSEDED-CORRECTLY, not reopened here." (b) No automated check
    executes any use-case strip; the `use-cases.md:8` "was run, once,
    locally" contract is enforced by schema tests
    (`usecases_test.clj`: 20 cases, unique ids, strip-shape) plus
    author discipline only. Related staleness: `bin/quickstart-demo:17-21`
    says the freshness extractor "has no poly-era replacement yet" —
    false; `ehrt.tools.quickstart-fresh` exists and its test
    (README fence = script = 15 commands) is green in the per-push
    lane. `make quickstart-fresh` and `make lint-pipeline` are named
    in docstrings but are not Makefile targets.
11. **[verified] `CLAUDE.md` is promised and absent — named in two
    prior audits, closed zero times.** `AGENTS.md:5-6` says "Claude
    Code users: see `CLAUDE.md`, which points here";
    `git ls-files` has no such file. Carve-loss audit
    (DROPPED-WRONGLY, "restore: step 6") and discipline-parity M21
    both flagged it.
12. **[verified] `corpus intake` with a generator URL couples to the
    generator's default staging dir even under an explicit
    `--out-dir`.** `bin/ehrt corpus intake "sim:?seed=42&patients=2&emit=hl7"
    --out-dir /tmp/...` exited 2 with `:out-dir-exists` naming
    `out/corpus/sim-s42-p2` (the *internal* staging dir, left over
    from an earlier run) — the user's `--out-dir` never enters it.
    The hint text is good; the coupling is surprising and makes
    use-case 18's strip non-rerunnable on a used tree without an
    `rm -rf` of a directory the user never asked for.
13. **[verified] The mutate↔judge taxonomy alignment is prose plus
    test assertions, not declared data.** Operators carry
    `:contract {:type :violates :target "<prose>"}`; the
    inject-class-X-expect-class-X pairing lives in
    `v2_contract_pairing_test.clj` (HAPI tier, per-push) and
    `contract_pairing_test.clj` (FHIR tier, integration) as
    hand-written assertions. No pairing data structure exists, and
    the NIST tier has no pairing suite at all — despite
    `judge-v2-nist.v2`'s own docstring naming the engine's
    `reference.conf` finding taxonomy as the "alignment hook" that
    would make the pairing checkable against engine config.
14. **[verified] All three `:necessary` entries in `workspace.edn`
    are still classpath-earned, but the palgebra halves hang by the
    docs-tooling thread, and the `integration` entry is
    ADR-undocumented.** The `tools → palgebra` src edge exists solely
    via `ehrt.tools.lint` and `ehrt.tools.pipeline` (docs/lint
    tooling, not reachable from `ehrt.tools.interface`); conformance/
    integration test trees require only `ehrt.tools.interface` (and
    `sim-harness`) directly. ADR-0002 documents two suppressions;
    the third (`integration`, added under ADR-0004's commit) appears
    in no ADR text. A docs-tooling extraction (§5) severs the
    palgebra edge and obligates re-deriving all three entries.
15. **[unverified] `docs/formats.md`'s captured-output examples
    post-date ADR-0013's stdout-default change.** The page claims
    every shape is "backed by a real captured output"; I did not
    re-capture and diff them against the new TTY/pipe defaults. Worth
    one spot-check in the errata pass (P1-1).

---

## 3. Gaps vs. requirements

### 3.1 Per audience (the seven segments of [positioning.md](../docs/dev/positioning.md))

| # | Audience | Entry path | Walk result |
|---|---|---|---|
| 1 | Guide reader (method-first) | README → [docs/README.md](../docs/README.md) → positioning's referral triggers | **Works.** Deliberately one-way pre-release (guide cites nothing here until first release); positioning states this honestly. No gap. |
| 2 | Practitioner (task-first) | README Quickstart → [use-cases.md](../docs/use-cases.md) → [cli.md](../docs/cli.md) → operators/locators | **Works, verified live.** I ran `help`, `version`, `doctor`, `corpus operators`, `gate v2` (fixture, exit 0), `gate v2-nist` (use-case 15 strip **verbatim**: exit 3, `:no-verdict`, 473 findings — exactly as its note promises), `corpus generate sim --out-dir` (deterministic, manifest present), `show`, `play` (dir, ticker). Two dents: the intake-generator staging-dir coupling (finding 12), and step 7 of this audience's own path ([judge-calibration.md](../docs/judge-calibration.md)) carrying stale paths/claims (finding 4). |
| 3 | Contributor | README Contributing → [AGENTS.md](../AGENTS.md) → [AUTHORS-GUIDE.md](../AUTHORS-GUIDE.md) | **Works but the on-ramp's structure section misleads** (finding 2): a contributor reading "Landed so far" plans against a 7-component workspace with NIST as future. AUTHORS-GUIDE is current. |
| 4 | AI assistant as reader | AGENTS.md; CLI help surface; SETUP.md hand-off prompt | **CLI help surface is exemplary** (self-describing groups, enumerable-options errors, remedy hints — ADR-0013/15 landed well for this audience). Gaps: AGENTS.md staleness (finding 2), `CLAUDE.md` promised-and-absent (finding 11), facts-register F3's on-ramp walk stale (finding 5). |
| 5 | Downstream data consumer | [formats.md](../docs/formats.md), [glossary.md](../docs/glossary.md), judge-calibration's reading sections | **Structurally served**; formats.md's captured-output currency unverified post-ADR-0013 (finding 15); judge-calibration's stale citations (finding 4) sit directly on this audience's path. |
| 6 | Clojure library consumer | Deferred by design (positioning §6) | **Deferred as designed** — but finding 6a is this audience's first real bug: `v2-nist-gate-file` via `ehrt.tools.interface` throws on a missing file where every sibling returns a result value. |
| 7 | Evaluator | README maturity table + Scope + what-is-this | **Mostly works.** The maturity table (`README.md:40-46`) has no row/mention for the profile tier (`gate v2-nist`) and its Gate row still reads "no implementation guide pinned yet" with only the two original engines named — the evaluator can't discover the workspace's newest capability from the contract table that exists for exactly that purpose. |

### 3.2 Per use case (the 20 cases of [use-cases.md](../docs/use-cases.md))

Support status; "Fully" = implementation + tests + runnable strip
located. Spot-runs this session marked ✓.

| Case | Status | Evidence |
|---|---|---|
| 1 generate-conforming-data | Fully | `corpus/generate.clj`; EXP-A4; integration lane green ✓ (lane) |
| 2 generate-sim-traffic | Fully ✓ | ran `generate sim --out-dir` scratch; manifest + msg-NNN.hl7 |
| 3 play-a-generated-corpus | Fully ✓ | `player.clj` (pure plan) + CLI executor; ran `play` on fixture dir |
| 4 controlled-fault-data | Fully ✓ | operators registry probed; 5 v2 + FHIR operators, `:violates` contracts |
| 5 validator contract-pairing | Fully | both pairing suites exist (per-push v2, integration FHIR); doc cites stale paths (finding 4) |
| 6 judge-user-supplied-data | Fully ✓ | `gate v2` fixture run; intake tests |
| 7/8/10 (external-transform cases) | Honest no-strip | `{external: true}`, schema-enforced anti-claim — as designed |
| 9 regression-baselining | Fully | `--baseline` semantics in `judge.report`; baseline tests in conformance project |
| 11 vendor-corpus QA | Fully | intake + gate + check chain |
| 12 reproduction packages | Fully | manifest v1.1 + zero-flag reproducibility test (integration, green) |
| 13 audit evidence trail | Partially | strip exists; "experimental" self-label; lineage records live in `lineage.clj` — no end-to-end audit-trail test found |
| 14 judge-tier calibration | Fully | CAL-1 doc + both pairing suites; doc staleness is finding 4 |
| 15 profile-tier NIST gating | Fully ✓ | **ran verbatim**: exit 3, `:no-verdict/:profile-spec-error`, 473 findings, `by-code` table — matches doc note exactly |
| 16 training material | Fully | strip + operators doc |
| 17 bring-your-own-generator | Planned (honest) | foreign-format adapter future; schema-enforced no-strip |
| 18 sim-as-intake-source | Partially ✓ | works cold; **fails on a used tree** via staging-dir coupling (finding 12) |
| 19 stdin intake | Fully | `spool.clj`/`framing.clj`; real-pipe integration test |
| 20 mutate→intake pipe | Fully | loopback integration test |

**The calibration flow (CAL-1) end to end:** doc → operator registry →
pairing tests → baseline-relative gating all exist and are coherent;
the doc's citations are the weak link, not the machinery.

**Source/sink design vs. code:** materially converged, honestly
fenced. All six source schemes and four sink schemes parse
(`source_sink_url.clj:40-47`); `:mllp` is a real byte-exact codec
(`framing.clj:140-`); `blaze` is parser-recognized and explicitly
rejected downstream (disclosed, not silent); OPEN-5/OPEN-6 correctly
remain open in the design doc. The doc's residue of pre-Polylith
paths (finding 4) is its main defect — the design itself has not
outrun the code in any load-bearing way I could find.

**Player vs. v2_replay: genuinely distinct, keep both.**
`ehrt.tools.player` is paced re-emission of a corpus (time, no state);
`ehrt.sim.v2-replay` is wire-side state reconstruction proving
emitter coherence (state, no time). Only the names rhyme.

---

## 4. Ergonomics & navigability

### 4.1 Humans

- **The map exists and is good — where it's current.**
  `docs/README.md` routes by audience; `docs/dev/README.md` routes
  maintainers; `docs/dev/architecture.md` is the map and is one
  landing behind (finding 2). The doc-tree separation (`docs/` user /
  `docs/dev/` maintainer / component-adjacent / `notes/` internal) is
  clean and ADR-governed (ADR-0010); I found nothing in `notes/` that
  belongs in `docs/` or vice versa. One naming trap for newcomers:
  `docs/dev/components.md` is about *external engines*, not Polylith
  components — a new Clojure developer looking for the brick map will
  open it first and land in the wrong place (rename or add a
  first-line redirect in the errata pass).
- **The non-developer CLI consumer is genuinely well served** — help
  text, remedy hints, `show`/`play`, `--json`; the cold-start and
  output-UX sessions visibly landed for this audience.
- **The maintainer's own hazard is the citation debt**: stale-path
  family (finding 4), the F22 wrong-register link, bare `ADR-0016`
  citations in `judge_fhir_official/fhir.clj:210,397,423` that will
  become ambiguous the moment the live register reaches ADR-0016
  (the root register already concedes this class of debt at its
  ADR-0013 record).

### 4.2 Agents and token cost

- **Minimal doc set per task class** (measured line counts):
  - *corpus task:* `AGENTS.md` 235 + `architecture.md` 131 +
    `tools/interface.clj` 216 + `source-sink-design.md` 725 ≈ 1,300
    lines — inside the ~1,500 budget **only if** source-sink-design
    is needed; it usually is for anything touching intake/sink. Fine,
    but tight, and the interface's 74 defs force reading breadth.
  - *sim task:* AGENTS + architecture + `sim/interface.clj` +
    component-adjacent theory docs — the theory docs
    (`sim-theory.md` etc.) are correctly component-adjacent and only
    loaded when needed. Within budget.
  - *judge task:* AGENTS + architecture + `judge/finding.clj` +
    engine ns ≈ 700 lines. Comfortably within budget — the per-engine
    split (ADR-0011/0012) already bought this.
  - Discoverability is the gap, not volume: nothing tells an executor
    *which* subset to load; `AGENTS.md` could carry a three-line
    "minimal reading set per task class" block (P3-4).
- **`docs/use-cases.md` (1,460 lines): split the rendering, keep the
  source.** The file is **generated** (`make use-cases` from
  `components/tools/docs/use-cases.edn`), so any split must happen in
  the renderer. The machinery already exists:
  `usecases/write-case-equations!` splits per-case today. Proposal
  P3-1: emit `docs/use-cases/<id>.md` per case plus an index page,
  from the same EDN, keeping the schema tests (20-case tripwire,
  unique ids, strip-shape invariants in `usecases_test.clj`) and the
  anchor-stability rule (AUTHORS-GUIDE §6) intact. The enforcement
  named and verified: Malli `UseCase`/`UseCases` schemas + dogfooding
  tests + `lint.clj`'s catalytic-resource resolution — all per-push.
  `source-sink-design.md` (725) reads once per transport task; an
  index header (its Decision Register already is one) is sufficient —
  no split needed.
- **`notes/` sprawl: the risk is stale audits read as current.** The
  audits self-report "0 open" while their real open surface lives in
  deferral language ("not reopened here", "named-future"). Proposal
  P3-2: a `notes/README.md` index — one line per file: date, role,
  and a **"still-open pointers"** column naming what each audit
  deferred (the dropped CI gates, docsgen write-path check, M24
  fixture conversion, F-G1 signposting). Additive, dated, fix-forward
  — no file moves, no history edits. Also worth a line each for the
  two anomalous prompt files (the self-declared placeholder
  `2026-07-28-…-h2-closeout-sweep.md`; the "revN supersedes, unrun"
  headers) so agents skip them.
- **Interface width as token cost, quantified:** `ehrt.tools.interface`
  = 216 lines, 74 defs, requiring 15 internal namespaces + all five
  sibling component interfaces. 11 of tools' 29 non-interface
  namespaces are interface-invisible, but four of *those* are the
  docs/enforcement tooling an agent most often needs to understand
  (`usecases`, `pipeline`, `quickstart-fresh`, `lint`) — findable
  only via the Makefile. The §5 split localizes all of this.

---

## 5. Refactoring & design recommendations

All of these are proposals for author ruling — the fat-component
disclosure (AGENTS.md; ADR-0001 R5, ADR-0002 R13) explicitly reserves
narrowing decisions to a ruled session, and nothing below
relitigates R-1…R-7.

### 5.1 High-level positions

**(a) Split `tools` — FOR, staged, three extractions, façade
retained.** Evidence: findings 6/13/14, §4.2, and the dependency
matrix (`tools` depends on every other component; `cli` depends only
on `tools` — the interface is doing a base's composition job inside a
component). The workspace already owns the method: ADR-0011's
characterization → extract → re-export-compat → `poly check` cycle.
Proposed stages, each independently landable (names are candidates,
author's call; each name says what the thing is, per R-1):
  1. **`docs-tooling`** (or `docsgen`): `docsgen`, `usecases`,
     `pipeline`, `quickstart-fresh`, `lint` — dev-time only, the sole
     source of the `tools → palgebra` edge. Smallest, safest, and
     immediately clarifies §1.2.4's `:necessary` picture.
  2. **`corpus-io`** (or `transport`): `source-sink`,
     `source-sink-url`, `sink-write`, `spool`, `spool-source`,
     `framing`, `player` — the sources/sinks/framing vocabulary of
     ADR-0015/tools-ADR-0017, plus the player that consumes framing.
  3. **`corpus` narrows to the domain**: `intake`, `mutate`,
     `generate`, `generator-source`, `generators`, `operators`,
     `manifest`, `operation-manifest`, `er7`, `canonicalizers`,
     `golden-comparison`, `lineage`, plus `check`/`check.schemas`/
     `diff` (the corpus's second judge stays with the corpus) and the
     `sim` adapter. Whether `tools` survives as a thin façade
     re-exporting all three (zero-behavior-change for `bases/cli`,
     the ADR-0011 pattern) or retires after the CLI re-points is the
     author's naming call; the façade-first path is lower-risk.
  Every stage co-lands: a pre/post characterization baseline
  (judge-extraction method), the project `deps.edn` updates for all
  five projects, re-derived `:necessary` entries with an ADR note
  (closing finding 14's undocumented third entry), and green
  `poly check` + per-push lane.

**(b) Sim: one canonical home, confirmed — action is external, not
internal.** No dedup inside the workspace is needed: `notes/sim/` is
deliberate frozen provenance, working as designed. The work item is
the sibling repo (author-only, per AGENTS.md's repo-level-`gh`
rule): archive `pragsmike/ehr-testing-sim` on GitHub with a tombstone
README ("consolidated into ehr-testing-tools at merge `a0534d0`,
2026-07-28; this repo is frozen provenance"). Also note ADR-0001 R1
rules the workspace repo is `ehr-testing` while the actual remote is
`ehr-testing-tools` — either supersede R1's naming clause with a
dated note or rename the remote; today the workspace's own name
collides with its frozen parent's, which is exactly the confusion the
provenance headers exist to prevent.

**(c) Projects `conformance`/`integration`: KEEP both — the
separation is structural policy, not convenience.** They compose
identical brick sets and differ only in test tree; that is the
point — ADR-0004 R19 made "fetch-dependent vs. hermetic" a
*structural* property (a project boundary CI selects by name) rather
than a tag convention that regressed once before (ENF-1 history).
Poly test selections would re-soften what R19 deliberately hardened.
No change proposed.

**(d) Judge engine loading: isolation is adequate; the finding is
the double-channel, not the classpath.** All three engines co-reside
on every project's classpath and the suites are green, so no
isolation emergency exists (the official FHIR validator is a
subprocess; HAPI and NIST coexist in-process — SLF4J noise on stderr
is cosmetic). `artifacts.lock.edn` covers fhir-validator-cli and all
six NIST jars with sha256s and license posture
(`:use-permitted--unstated--confirmation-pending`); HAPI resolves via
`deps.edn`, which is a legitimate third target per
engine-onboarding #4. The defect is the NIST jars resolving through
*two* targets at once with `doctor` disagreeing with reality
(finding 8) — resolve by ruling (P2-3).

**(e) Docs → code → tests flow per audience:** the pipeline is
docs-from-data (`use-cases.edn`, cli-spec, operator registry) with
schema/dogfood tests — structurally excellent. What's missing is the
*freshness* half (regen-and-diff gate, P1-2) and the *catch-up* half
(errata pass P1-1, register rows P2-1). No structural redesign
needed.

### 5.2 Task list

Each task names scope, change, and its co-landed invariant (R-5).
Enforcement files named where touched.

**P1-1 — Stale-citation errata sweep (docs + register + lockfile).**
Scope: `docs/judge-calibration.md`, `docs/locators.md`,
`docs/dev/components.md`, `docs/dev/source-sink-design.md`,
`notes/facts-register.md` F4-F6, `artifacts.lock.edn` header comment,
`bin/quickstart-demo:17-21`, README maturity table (Gate row gains
the profile tier). Change: fix-forward dated errata — update paths to
their Polylith homes, origin-qualify the F22 citation, replace "not
adopted" NIST claims with ADR-0012 reality, add a maturity row/note
for `gate v2-nist`. Co-landed invariant: extend `ehrt.tools.lint` (or
a new doc test in the per-push lane) with a stale-path tripwire —
fail on `ehr_testing_tools`, `test-integration/`, or
`docs/experiments/` appearing in `docs/**/*.md` — so this class
can't silently re-accumulate. Keeps `make quickstart` green
(quickstart fence untouched).

**P1-2 — Generated-doc freshness gate in CI.** Scope:
`.github/workflows/test.yml` (enforcement file), Makefile. Change:
a job step running `make docsgen` then `git diff --exit-code` on the
four generated docs (needs `python3` in the runner for the mermaid
script — declare it). Co-landed invariant: the gate itself, plus
correcting `docsgen_test.clj:20-22`'s prose to cite the now-real
step. Closes the carve-loss audit's named-future row honestly.

**P1-3 — AGENTS.md / architecture.md structure catch-up.** Scope:
`AGENTS.md` "Landed so far", `docs/dev/architecture.md` diagram +
bricks/projects tables. Change: add judge-v2-nist and the current
project compositions; date the update. Co-landed invariant: a
per-push test asserting every name in
`clojure -M:poly ws get:components:keys` appears in both files (cheap
grep-shaped test; makes "kept current" mechanical instead of
aspirational). Alternative if the author prefers: generate the bricks
table like `cli.md` is generated.

**P1-4 — Restore `CLAUDE.md`.** Scope: repo root; twice-audited gap
(carve-loss, discipline-parity M21). Change: the conventional
pointer-to-AGENTS.md file, per the promise in `AGENTS.md:5-6`.
Co-landed invariant: fold a presence assertion into the existing
repo-hygiene test family (`bases/cli/test/.../executable_bits_test.clj`
precedent — index presence, not disk presence, per AUTHORS-GUIDE
§7a).

**P2-1 — Facts-register catch-up sweep.** Scope:
`notes/facts-register.md` (+ its Index, same commit, per
AUTHORS-GUIDE §4). Change: new F-rows for (i) judge-v2-nist's
verified engine behavior (the 473-finding fixture pin), (ii)
ADR-0013's TTY/pipe output contract, (iii) ADR-0014's player
byte-identity claim, (iv) ADR-0015-amendment's cold-start default;
refresh F7 (177 → 185, with run evidence); mark F3's walk
superseded-pending-rewalk. Co-landed invariant: the register's own
Index row per new F-row (the existing same-commit discipline is the
invariant; no new mechanism).

**P2-2 — Judge-family parity pass (the R-2/envelope/schema
asymmetries, finding 6).** Scope:
`components/judge-v2-nist/src/ehrt/judge_v2_nist/v2.clj`,
`bases/cli/src/ehrt/cli/core.clj` (adapter simplifies),
`components/judge-v2-nist/test/`. Change: `gate-file` returns
`kernel/error :file-not-found` instead of throwing; `gate-dir`
returns `kernel/ok {:results [...]}`; decide recursive-vs-flat
directory walk once for all engines and record it (hapi flat, nist
recursive today — either is defensible, divergence isn't). Co-landed
invariant: a cross-engine contract test (natural home:
`projects/conformance/test`) asserting for each engine — findings
validate against `ehrt.judge.finding/Finding`, verdict/cause pairing
satisfies `valid-cause-pairing?`, missing-file returns the error
value. This also gives judge-v2-nist its missing schema coupling to
`judge` (as a test dep, mirroring hapi's).

**P2-3 — Rule the NIST artifact channel (finding 8).** Scope:
`artifacts.lock.edn`, `bases/cli` doctor check, a dated ADR note.
Change (author ruling required): either (a) the six NIST rows stay as
provenance/license records but gain a marker doctor's cache check
skips (they're consumed via `deps.edn`, engine-onboarding target 2),
or (b) they leave the lockfile and the license posture moves to a
facts-register row + `deps.edn` comment. Co-landed invariant: a
doctor test pinning the chosen behavior (a machine with the Maven
deps resolved and an empty artifact cache exits 0 — or the ruling's
alternative), plus the lockfile header comment fix (P1-1 overlaps;
whichever lands first takes it).

**P2-4 — `verdict-cache` placement decision (finding 7).** Scope:
`components/judge/src/ehrt/judge/verdict_cache.clj`. Change: author
ruling — either move it into `judge-fhir-official` (its only
consumer; ADR-0011 explicitly invited the post-hoc veto) or keep it
in `judge` with a superseding one-paragraph justification that no
longer cites the NIST expectation. Co-landed invariant: existing
verdict-cache tests move with it; `poly check` green across all five
projects proves no hidden consumer.

**P2-5 — Intake generator-URL staging-dir behavior (finding 12).**
Scope: `components/tools/src/ehrt/tools/corpus/generator_source.clj`
(+ CLI docs for `corpus intake`). Change (author ruling on which):
(a) stage generator output under the intake's own `--out-dir` (or a
temp dir), decoupling from `out/corpus/<derived>`; or (b) keep the
shared staging dir and document the coupling in `ehrt help corpus` +
use-case 18's note, with the hint naming the *intake* context.
Co-landed invariant: a test reproducing this session's probe — an
existing derived staging dir + `intake sim:… --out-dir <fresh>` —
pinned to the ruled behavior.

**P3-1 — Use-cases rendering split.** Scope:
`components/tools/src/ehrt/tools/usecases.clj`, `Makefile`
`use-cases` target, `docs/`. Change: render per-case pages +
index from the same EDN (see §4.2 for why this is renderer work,
never a hand-split). Co-landed invariant: `usecases_test.clj`'s
schema/count/shape tests extended to the multi-file output; anchors
preserved (AUTHORS-GUIDE §6); P1-2's freshness gate covers the new
outputs automatically.

**P3-2 — `notes/` index with still-open ledger.** Scope: new
`notes/README.md`. Change: per §4.2 — one line per notes file (date,
role) plus each audit's deferred-items pointers; mark the placeholder
and superseded-rev prompts as skip-for-context. Co-landed invariant:
same-commit index discipline stated in the file header (the
facts-register Index precedent); no mechanical gate warranted.

**P3-3 — Operator↔finding pairing as data (finding 13).** Scope:
`components/tools/src/ehrt/tools/corpus/operators.clj` (registry
entries gain, e.g., `:convicts {:v2-hapi {:verdict :rejected :codes
[...]}, :fhir {...}}`), both contract-pairing suites. Change: move
the expected-outcome half of each pairing assertion into registry
data; suites iterate the registry instead of hand-listing. NIST-tier
pairing enters only when a project-owned profile exists (ADR-0012's
own "stand-in" disclosure gates it) — name it in the registry as
explicitly absent until then. Co-landed invariant: the rewritten
suites themselves, plus a registry-schema extension in the operators
Malli spec.

**P3-4 — AGENTS.md minimal-reading-set block.** Scope: `AGENTS.md`.
Change: three lines — corpus / sim / judge task classes, each naming
its ≤4-file reading set (§4.2 measurements). Co-landed invariant:
covered by P1-3's structure-currency test (the block names component
interfaces that test already checks exist).

**P3-5 — `ehrt.sim.result` vs. `ehrt.kernel.result` ruling.** Scope:
`components/sim/src/ehrt/sim/result.clj`. The copy's own docstring
names its expiry condition ("if a third repo ever needs the doctrine,
extract a shared microlib then") — kernel *is* that extraction, so
the rationale is stale even though the copy still works (structural
typing means zero runtime harm). Change (author ruling; touches the
sim-independence rule, since kernel is tools-derived by provenance):
either bless the copy with an updated docstring citing a workspace
ADR note, or move sim onto `kernel/result` and record the
dependency-direction implication in the same ADR. Co-landed
invariant: sim's suite green either way; if the dep lands, `poly
deps` gains a documented `sim → kernel` edge and AGENTS.md's
constraint paragraph is updated in the same commit.

**P3-6 — `sim-cli` retirement-trigger review (facts-register F2).**
Scope: `bases/sim-cli`, `projects/sim`. F2's trigger ("retire when a
review finds no use outside their own tests") is now checkable: this
review found `sim-cli` required by nothing but `projects/sim` and its
own tests (poly deps matrix). That *is* the trigger condition —
surfaced here for the author to fire or re-arm; not executed by this
review. Co-landed invariant if fired: removal lands with the
carve-loss-audit method (§7d) applied to both bricks, and
`docs/dev/README.md`'s deprecation notice updated in the same
commit.

---

## 6. Evidence appendix

All probes ran against the ext4 clone at `65b550b` unless noted.
Logs live in the session scratchpad (`poly-check.log`,
`poly-test.log`, `poly-integration.log`).

**A. Workspace health**
- `clojure -M:poly check color-mode:none` → `OK`, exit 0.
- `clojure -M:poly test :all skip:integration color-mode:none` →
  exit 0; `grep -c "^Testing"` on the log → **185** namespaces;
  final blocks all "0 failures, 0 errors". (facts-register F7 says
  177 — stale.)
- `clojure -M:poly test :all project:integration color-mode:none` →
  exit 0 ("Execution time: 2 minutes 34 seconds").

**B. Structure**
- `clojure -M:poly ws get:components:keys` → `["judge"
  "judge-fhir-official" "judge-v2-hapi" "judge-v2-nist" "kernel"
  "palgebra" "sim" "tools"]`; `get:bases:keys` → `["cli" "sim-cli"]`;
  `get:projects:keys` → `["conformance" "ehrt-cli" "integration"
  "sim" "development"]`.
- `clojure -M:poly deps color-mode:none` — salient rows: `tools` has
  `x` on every other component; `judge-fhir-official` → judge `x`;
  `judge-v2-hapi` → judge `t` (test-only); `judge-v2-nist` → kernel
  only; `cli` → tools only; `sim-cli` → sim only; `kernel`,
  `palgebra`, `sim` → nothing.
- Namespace census (`find components/*/src -name '*.clj'`): judge 4,
  each engine 2, kernel 7, palgebra 4, sim 21, tools 30.
- Interface width: `wc -l` → 216;
  `grep -c "^(def" …tools/interface.clj` → 74; require block: 15
  tools-internal namespaces + 5 component interfaces.
- Require-edge probes: `bases/cli` requires only `ehrt.cli.help` +
  `ehrt.tools.interface`. `projects/conformance/test` requires
  `ehrt.tools.interface` (×18) + `ehrt.tools.sim-harness` (×6);
  `projects/integration/test` requires `ehrt.tools.interface` (×27).
  `ehrt.palgebra` required from tools src only by `lint.clj` and
  `pipeline.clj`. Apparent kernel→tools / framing→lint hits were
  docstrings or test-fixture strings (verified individually).

**C. Judge family**
- Read in full: `judge_v2_hapi/v2.clj` (215 l),
  `judge_v2_nist/v2.clj` (232 l), `judge/finding.clj` (144 l);
  skimmed `judge_fhir_official/fhir.clj` (489 l). Asymmetries quoted
  in finding 6 are at: nist `gate-file` `(slurp file)`
  (v2.clj:218-223) vs hapi `kernel/error :file-not-found`
  (v2.clj:196-203); nist `gate-dir` `file-seq` bare map
  (v2.clj:225-232) vs hapi `.listFiles` + `kernel/ok` (v2.clj:205-215);
  `finding/valid?` asserted only in
  `judge-v2-hapi/test/.../v2_test.clj:61`.
- CLI adapter compensation: `bases/cli/src/ehrt/cli/core.clj:891-930`
  (`.isFile` pre-check → `:file-not-found`; catches only
  `:ambiguous-msg-id` ex-info).
- `verdict-cache` consumers: grep → `judge-fhir-official/fhir.clj`
  only (plus judge's own interface/tests).

**D. CLI walk (all via `bin/ehrt` on the ext4 clone)**
- `help` → full group/flag/exit-code surface as quoted in §3.1.
- `version` → exit 0; lockfile shows 9 artifacts incl. six `nist-*`.
- `doctor` → exit 1; "6 not cached: nist-…" while the integration
  lane (which exercises the NIST engine from `~/.m2`) is green —
  finding 8.
- `corpus operators --format v2` → 5 operators, each
  `:contract {:type :violates :target "<prose>"}`.
- `gate v2 components/tools/test-fixtures/v2` → exit 0, totals
  `{:pass 5}`; envelope clean on stdout (SLF4J noise on stderr only).
- Use-case 15 strip **verbatim** → exit 3;
  `no-verdict … (473 findings)`; `by-code` incl.
  `value-set/VS Not Found=28`, `structure/Length Spec Error=221`.
  (A first attempt with a hand-retyped path missing the `covidELR/`
  segment correctly produced `error (file-not-found)`, exit 2 —
  evidence the strips are precise and the error family works.)
- `corpus generate sim --seed 42 --patients 2 --out-dir /tmp/…` →
  exit 0, `manifest.edn` + `msg-00N.hl7`.
- `corpus intake "sim:?seed=42&patients=2&emit=hl7" --out-dir /tmp/…`
  → exit 2, `:out-dir-exists` naming `out/corpus/sim-s42-p2`
  (finding 12).
- `show <fixture>` → rendered ER7; `play <fixture-dir> --rate 1000000`
  → exit 0, ticker output.

**E. Docs/enforcement probes**
- Stale-path grep across `docs/`:
  `grep -rn "test/ehr_testing_tools\|ehr_testing_tools\|docs/experiments/" docs`
  → 12 hits in the four files of finding 4. `ls docs/experiments` →
  does not exist; `components/tools/docs/experiments/` does.
- `grep -n "F22" notes/facts-register.md` → no match (register is
  F1–F9).
- Contract-pairing locations: `find . -name "*contract_pairing*"` →
  `components/tools/test/ehrt/tools/v2_contract_pairing_test.clj`,
  `projects/integration/test/ehrt/tools/contract_pairing_test.clj`.
- Freshness-gate absence: no `git diff --exit-code` in
  `.github/workflows/*` / `Makefile` / `.githooks/*`; `Makefile`
  `.PHONY` has no `quickstart-fresh` or `lint-pipeline` despite
  docstring references (`quickstart_fresh.clj:107,111`,
  `lint.clj:199`).
- `git ls-files` → no `CLAUDE.md`.
- Designator schemes: `source_sink_url.clj:40-47` (six source, four
  sink); `framing.clj:140-` (`:mllp` codec); `source_sink.clj:48,88`
  ("blaze remains parser-recognized (D-a) but…" — disclosed
  deferral).
- `workspace.edn` `:necessary`: three entries (ehrt-cli/conformance/
  integration); ADR-0002 documents two; the integration entry appears
  in no ADR text (delegated-agent ADR enumeration, cross-checked
  against `workspace.edn` comment).

**F. Sim provenance (delegated agent; commands re-verifiable)**
- `git log --oneline --merges` → `a0534d0` (sim), `d57c1b7` (tools);
  three root commits; `git merge-base --is-ancestor 213abaa HEAD` →
  true; `git rev-list 213abaa --not HEAD | wc -l` → 0.
- `git log --follow -- components/sim` → single import commit
  `c0b5b0a` (+ docs move `499cad4`).
- Post-rename content diff ≈ 0 (8/12 core namespaces byte-identical
  after mechanical rename; remainder differ only by resource
  re-nesting / generator identity string).
- Sibling clone: `Documents/o/ehr-testing-sim`, HEAD `213abaa` =
  `origin/main`, no archive marker; its `AGENTS.md:107` asserts the
  pre-merge dependency rule.
- Velocity: `git log --oneline --since=2026-07-28 | wc -l` → 74;
  total 430.

**G. Delegated sub-analyses** (four research agents; their inputs
were the live tree; every load-bearing claim of theirs that this
report relies on was independently spot-verified above): use-case
inventory + enforcement map; prior-audit/facts-register summary; ADR
enumeration across all three registers; sim provenance.

---

## Deviation record

- **2026-07-30.** No instruction-level deviations. Two disclosures:
  (1) the report was written identically to both live clones (ext4
  and `/mnt/c`) to avoid the documented dual-clone divergence hazard;
  the prompt did not specify a clone. (2) `poly test :all` was run as
  the workspace's own two lanes (`skip:integration`, then
  `project:integration`) rather than one undifferentiated invocation
  — both green, so the combined requirement is satisfied; recorded
  because the invocation differs textually from the prompt's.
- The review brief's §1.2.2 contingency ("if two canonical homes
  exist, that is a P1 finding") did not trigger: provenance is
  consolidation with a frozen, unmarked twin — reported as §5.1(b)
  instead.
- The brief's R-1 verdict-vocabulary phrasing was reconciled against
  the live enums (header note) rather than used verbatim, per R-7's
  probes-over-prose rule.

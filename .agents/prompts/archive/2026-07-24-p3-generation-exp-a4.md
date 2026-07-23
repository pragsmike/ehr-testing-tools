Task: Execute P3 — first capability (generation) + EXP-A4
You are working in `ehr-testing-tools`. This is the session that produces data: it lands the artifact registry, the generation capability, and the schemas they rest on, then executes experiment EXP-A4 using them. It also inspects newly copied skills, seeds the pattern nursery, and establishes the test-first discipline.
Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md` (especially ADR-0004/0005), `docs/experiments/EXP-A4.md`, `docs/experiments.md`, `docs/components.md`, `.agents/memory/` (may not exist yet). Commits from WSL. Network available.
Save this prompt to `.agents/prompts/2026-07-24-p3-generation-exp-a4.md` (live); final commit moves it to `.agents/prompts/archive/`.
Test-first applies to every step below that produces code: write the failing test, run it, watch it fail, then implement. Your report must include red→green evid ence (the failing-run output or a per-step note of what failed before what passed). Committing tests and implementation together is fine; writing implementation before its test is not.
Step 0 — Working tree + skill inspection
The author has copied three skills into `.agents/skills/`: `scenarios`, `probe`, and `review` (from the cyberneutics methodology; the committee skill already here is part of the same family). The tree is likely dirty with these uncommitted. Before committing them, inspect each for incongruities with this repo:

* Paths or config they expect that don't exist here (e.g. a `situations_root` config, `.agents/committee-config.yml`, cyberneutics- repo-relative paths, directories like `.agents/reviews/`).
* References to skills, rosters, or files not present in this repo.
* Assumptions about the host repo (manuscript, chapters, guide-specific or cyberneutics-specific content).
* Anything in their frontmatter/metadata that misdescribes this context.

For each skill: report findings in a table (skill | incongruity | action taken). Make only minimal adaptations (path fixes, creating a small config or directory a skill requires); if something is borderline or would change a skill's substance, leave it and flag for the author. If a skill requires a directory from the standard layout (`.agents/plans/`, `.agents/memory/`, etc.), creating it is in-scope — Step 1 needs some of those anyway.
Commit: `Add scenarios, probe, review skills (inspected/adapted)`.
Step 1 — Housekeeping: packs, nursery, plan
1a. Pack slimming
Restructure the Makefile pack targets:

* `pack` (default, what `pack-push` pushes): all tracked files except `.agents/skills/**` and `.agents/prompts/archive/**`. Note the elision in the pack header (add a line like `elides: .agents/skills, .agents/prompts/archive`).
* `pack-skills`: a second pack containing exactly the elided directories, written to `$HOME/ehr-testing-tools-skills-pack.txt`, same header format. Not pushed anywhere; the author uploads it to project knowledge manually when skills change. Add to `help`; gitignore its output name.
* `pack-push` unchanged in behavior (pushes the slim pack).

Amend AUTHORS-GUIDE's pack ritual: slim pack auto-pushed each session; skills pack regenerated and re-uploaded by the author only when `.agents/skills/` or archived prompts change materially.
1b. Pattern nursery
Create `.agents/memory/patterns.md`: candidate design patterns observed in design discussion, statuses per the ladder tentative → validated (second real occurrence) → ratified (ADR). Header explaining that: these exist to align sessions that cannot see each other; promote via ADR, do not silently implement variants. Seed with these entries (terse, 2–4 lines each; keep the given names):

1. Two-step engines (execute/interpret) — every engine run splits into: mechanical execution preserving native output verbatim, and a pure, versioned interpreter from native output to canonical data. Payoff: re-interpretation without re-execution. Status: in implementation (P3).
2. Invocation record — one schema for "a subprocess ran": command, args, relevant env, artifact refs by hash, engine version, duration, exit code, output digests. Engine-agnostic; engine-specific fields are a smell. Status: in implementation (P3).
3. Canonicalizers (registry + laws) — named, versioned transformations with three laws: idempotence (property-tested), explicit composition (verified confluence or registry-imposed total order, recorded in manifests), endomorphism (never cross-format). Canonical forms are fixed points; c@v2 need not agree with c@v1. Status: in implementation (P3).
4. Locator — one type for "a place in a datum," per-format grammar (FHIRPath; v2 segment/field/component; table.column; XPath later). Shared by mutation records (injection site), findings (violation site), canonicalizers (scope). Mandatory column of format support. Status: schema stub in P3; first real use P4.
5. Lineage (immutable, hash-linked) — every derived datum carries a parent reference by content hash plus a transformation record; records are append-only, corrections are new records. Merkle-style self-verifying chain; forensic/regulatory-grade provenance. Status: tentative; first use P4.
6. Finding envelope, ternary verdicts — one canonical finding shape {severity, code, locator, message, engine, native-ref}; verdicts are pass / rejected / indeterminate, with indeterminate first-class (license-blocked checks, engine limitations). Status: tentative; lands with gates.
7. Corpus recipes — one EDN value naming a corpus intent (generator config, population, seed policy, mutation plan), referenced by hash from manifests. Status: tentative.
8. Corpus catalog — an index of corpus items (id, layer, format, lineage ref, tags); meaning lives in data, not filenames. Status: tentative.
9. Format-support matrix — adding a format means filling a column of obligations (round-trip-verified parser, locator grammar, mutation operators, canonicalizers, gate engine, docs), each cell with maturity. Status: tentative.
10. Compatibility claims as tested facts — engine-version × artifact-format claims live in F-rows or a compat table, each backed by a pinned smoke test. Status: tentative.
11. Result vocabulary — the result-not-throw doctrine's shared keywords (`:ok` / `:rejected` / `:error` + category), one namespace, no dialects. Status: in implementation (P3).
12. Soft types for prose artifacts — template/rubric pairs with scored membership for documents that can't be hard-schema'd (protocols, results files, capability doc pages); the prose complement of Malli. Status: first instance in P3 (EXP results files).

1c. Plan file
Create `.agents/plans/corpus-foundations.md`: the phase plan as a table — P0 housekeeping (done, date), P1 ADRs (done), P2 docs/protocol (done), P3 generation + EXP-A4 (this session), P4 mutation + EXP-B2, P5 EXP-SBOM mechanical half + EXP-C5/D3, and a named enforcement wave (not scheduled yet): pre-push hook running tests, offline GitHub Actions CI, coverage threshold gating, artifact-cache priming for CI. One line per phase: deliverables, status, pointer to its prompt in the archive.
Commit: `P3.1: slim/skills packs, pattern nursery, phase plan`.
Step 2 — ADR-0006: test-first discipline
Append to `notes/ADRs.md`, house format:
ADR-0006 — Test-first, staged enforcement. Context: capability code starts this session; the repo's credibility rests on verification discipline, and its own method (the guide's) is a testing method — a test-shy testing-tools repo is self-refuting. Sessions are executed by agents that cannot see each other; discipline must be written and mechanical, not remembered. Decision: test-first is a hard rule — a failing test precedes the implementation it motivates; sessions demonstrate red→green in their reports; property tests are required for law-bearing constructs (canonicalizer laws, hash verification, schema round-trips); coverage is measured (cloverage via a `:coverage` alias and `make coverage`) and regressions in coverage require justification in the session report. Enforcement is staged: now — convention + prompt discipline + coverage measurement; enforcement wave (planned, see `.agents/plans/corpus-foundations.md`) — pre-push hook running the suite, offline GitHub Actions, coverage threshold gating. Alternatives rejected: full mechanical enforcement immediately (procrastination-by- perfectionism; blocks the first capability on CI plumbing); coverage as vibes (unmeasured "good coverage" is unfalsifiable). Consequence: every code-producing prompt carries the red→green reporting duty; AGENTS.md hard rules gain: "Test-first: a failing test precedes implementation; red→green evidence in session reports; `make test` and `make coverage` green/reported before any session-final commit." Status: Accepted (author-directed).
Add that hard rule to `AGENTS.md`. Add the `:coverage` alias to `deps.edn` (cloverage, exact-pinned) and `make coverage` to the Makefile/help.
Commit: `ADR-0006: test-first with staged enforcement; coverage tooling`.
Step 3 — Schemas and the artifact registry (test-first)
Add to `deps.edn`, exact-pinned: `metosin/malli`, `org.babashka/cli` (and cloverage from Step 2 if not already committed). Then, red→green throughout:

1. Result vocabulary — `ehr-testing-tools.result`: `:ok` / `:rejected` / `:error` + category keyword + payload; helpers + Malli schemas. (Pattern 11.)
2. Invocation record — `ehr-testing-tools.invocation`: the engine-agnostic schema from pattern 2; a `run!` wrapper that executes a subprocess and returns the record (this is the only impure seam).
3. Locator stub — `ehr-testing-tools.locator`: the envelope schema {format, path} with format-dispatched validation; implement only the trivial grammar checks now (non-empty path, known format keyword) — real grammars arrive with mutation/gates. Tests included.
4. Canonicalizer registry — `ehr-testing-tools.canonical`: registry of {id, version, format, fn, docstring}; application records the ordered list applied; property-test harness for the laws — idempotence generatively tested for every registered canonicalizer (the harness runs against the registry, so future entries inherit the test), composition-order explicitness enforced by the API (no unordered set application). Registry starts empty or with entries demanded by EXP-A4 findings (Step 5).
5. Artifact registry — `ehr-testing-tools.artifact` per ADR-0005: lockfile read/validate (Malli), content-addressed cache at `~/.cache/ehr-testing-tools/artifacts/<sha256>` honoring `EHR_TESTING_TOOLS_CACHE`, `fetch` (download from recorded source, verify sha256, place in cache; result-vocabulary returns, no throws for operational failures), `resolve` (name+version → verified path or `:rejected`). Tests: hash-mismatch rejection (property-flavored), missing-entry, cache-hit short-circuit (no network on hit).
6. Create `artifacts.lock.edn` with the first entry: Synthea v4.0.0's runnable distribution (the with-dependencies jar from the GitHub release), `kind :engine`, real source URL, sha256 computed from the actual download during this session, `acquired` today, `license-status :verified` with a pointer to the components doc / F-row. Verify `fetch` then `resolve` work against it for real.
7. Manifest v0 — `ehr-testing-tools.corpus.manifest`: schema holding {generator artifact refs, seed, config path+content-hash, invocation record, canonicalizers applied, environment fields}; v0 is the EXP-A4 hypothesis, upgraded to v1 by its findings.

Commit granularity: your judgment, but tests visible with their implementations. Suggested: one commit per numbered item or sensible groups.
Step 4 — `corpus.generate` and the CLI (test-first)

1. `ehr-testing-tools.corpus.generate`: takes {config path, seed, population, output dir}; resolves the Synthea artifact; builds the java invocation; runs via the invocation wrapper; preserves Synthea's output tree verbatim (two-step engines: no normalization, no renaming, no post-processing in this step); emits a manifest v0 alongside. Repo-authored Synthea properties file lives at `config/synthea/` (git-versioned; keep it minimal: FHIR R4 export on, others off, fixed reference date if Synthea supports one — check its properties documentation). Generated corpora are not committed: output goes under a gitignored `out/` (or a path the caller gives); manifests and hashes are the committed record.
2. `ehr-testing-tools.cli`: `ehr` entrypoint via babashka.cli; subcommands `artifact fetch`, `artifact resolve`, `corpus generate`; exit codes 0/1/2 per ADR-0004; EDN output, `--json` projection. A `make ehr ARGS=...` or bin script so it's invokable; note in AGENTS.md commands. Tests for arg parsing and exit-code mapping (invoke the functions, not a subprocess, where practical).

Commit: `corpus.generate (two-step, manifest v0) + ehr CLI`.
Step 5 — Execute EXP-A4
First, two pieces of experiment machinery:

1. Results-file soft type: create `docs/experiments/results-template.md` and `docs/experiments/results-rubric.md`. Template: metadata (experiment id, date, executor, HEAD), environment record, per-round findings table (divergence observed | field(s) | classification pin/control/canonicalize | action taken), protocol amendments made, acceptance verdict against the protocol's criteria, artifacts produced (paths + hashes). Rubric: scored criteria (findings each classified; environment complete; amendments justified; verdict traceable to criteria; no unexplained divergences). Your results file must self-score against the rubric at the end.
2. Protocol correction check: `docs/experiments/EXP-A4.md` says to vary "thread count (`-p` parallelism)". This is suspected wrong: Synthea's `-p` sets population size. Verify Synthea's actual parallelism mechanism (its README/wiki/properties — e.g. a thread-pool or multithreading property, or JVM-level control), amend the protocol accordingly (edit in place + an "Amendments" note with date and reason — protocols are corrected loudly, not silently), and vary the real control in round 2.

Then execute the protocol as amended, via `ehr corpus generate` (the runs are the capability's acceptance suite): baseline triplicate, single-variable rounds (real parallelism control, locale, timezone, second JVM if available), byte-diff harness, classification of every divergence, canonicalizer entries added to the registry for `canonicalize`-class findings (their idempotence property tests come free from the harness), manifest schema upgraded to v1 with the complete pinned/controlled/canonicalized set, and the clean-environment regeneration check (fresh cache via `ehr artifact fetch`, fresh checkout or scrubbed env).
Write `docs/experiments/EXP-A4-results.md` per the template. Do not interpret beyond classification: findings and classifications yes; design conclusions, F-rows, and pattern promotions happen in the design channel afterward. Update the EXP-A4 row in `docs/experiments.md` (status: executed, link results).
Population sizing: keep rounds small enough to be fast (e.g. 100 patients per run) but run one larger generation (e.g. 1000) at the end under the final pinned configuration as the first real corpus; record its manifest and output-tree hash in the results file. Effort cap: if the protocol's stop condition triggers, stop, record, report.
Commit: `EXP-A4 executed: results, canonicalizers, manifest v1` (split if cleaner).
Step 6 — Finalize

1. `make test` green; `make coverage` run, headline numbers in the report.
2. Move this prompt to `.agents/prompts/archive/`.
3. `make pack-push`; verify `updated_at` via authenticated `gh api`.
4. Report must include: skill-inspection table; red→green evidence per step; pinned versions added; lockfile entry as committed (with sha256); protocol amendment made and its evidence; every EXP-A4 divergence with classification; canonicalizers registered; manifest v1 field list; the large-run manifest hash; results-file rubric self-score; coverage numbers; commits.

Out of scope
No mutation code, no gate code beyond the locator stub, no CI/GitHub Actions or hooks beyond what exists (enforcement wave is planned, not built), no Clojars publishing, no edits to ADR-0001..0005, no touching `../ehr-testing-guide` or the cyberneutics repo. Do not interpret EXP-A4 findings into design conclusions — classify and report only.

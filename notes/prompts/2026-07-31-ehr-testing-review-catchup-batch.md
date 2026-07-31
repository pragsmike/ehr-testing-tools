# 2026-07-31 — ehr-testing-tools: review catch-up batch (P1-1..P1-4, P2-1)

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`) fast-forwarded `65b550b` → `c51d415`
(2 commits, doctor-hint/hint-family work) at session start, then
verified clean against `origin/main`. Agent-prepares/author-commits
default (no delegation heard this session) — no commit or push run by
this session; the author commits manually between steps, per the
prompt's own instruction.

## Original prompt (verbatim)

Context
You are executing the uncontroversial batch from the 2026-07-30 architecture review (`notes/2026-07-30-refactoring-review.md`): the stale-citation errata sweep, the generated-doc freshness gate, the AGENTS/architecture structure catch-up, `CLAUDE.md` restoration, and the facts-register catch-up. Nothing here changes runtime behavior, splits components, or touches anything awaiting an author ruling. The review found the code green and the records stale — this session pays down exactly that citation debt, and co-lands the tripwires that stop each class from re-accumulating (R-5).
Work in the WSL ext4 clone (`~/src/ehr-testing-tools`). Fast-forward it to `origin/main` before any edit and record the HEAD sha in your summary. Do not commit or push — the author commits manually between steps. Do not touch the `/mnt/c` clone at all; it carries uncommitted doctor-hint WIP in `bases/cli` (disclosed in the review's environment notes). After each step, leave the tree coherent and print the step's proposed commit message; run the per-push lane (`clojure -M:poly test :all skip:integration color-mode:none`) and confirm green before starting the next step. Never pipe poly invocations through anything that masks exit codes — check `$?` directly (this exact trap bit the review session).
If `notes/2026-07-30-refactoring-review.md` is absent from the tree, stop and report — this prompt depends on it being committed.

Read first
1. `notes/2026-07-30-refactoring-review.md` — findings 2, 3, 4, 5, 10, 11, 15 and tasks P1-1..P1-4, P2-1 (§5.2). The checker replay confirmed all of these against the public tree, plus one addendum folded into Step 2 below (`use-cases.edn:146`).
2. `AGENTS.md` and `AUTHORS-GUIDE.md` — especially §4 (facts-register Index same-commit discipline), §6 (anchor stability for generated docs), §7 (checks carry their invariants; §7a index-presence-not-disk-presence).
3. `notes/facts-register.md` and `notes/tools/facts-register.md` — row format, evidence discipline, the F1–F9 vs F22 origin split.
4. `docs/dev/engine-onboarding.md` — for the checklist prose Step 2 must align with (do not modify its checklist semantics; that's P2-3, awaiting ruling).
5. `components/tools/src/ehrt/tools/usecases.clj`, `lint.clj`, and the `Makefile` — the generation and enforcement machinery Steps 2 and 5 extend.

Author rulings
* AR-1 Fix-forward only. Stale claims get corrected with dated errata notes where the document's conventions call for them; no history rewrites, no deletion of superseded records. Where a doc self-discloses drift, update the disclosure rather than removing it.
* AR-2 Vocabulary. Use the live enums exactly: result envelope `:ok/:rejected/:error` (`ehrt.kernel.result`); judge verdict `:pass/:rejected/:indeterminate (reserved)/ :no-verdict` + `:cause` (`ehrt.judge.finding`). Do not reproduce the review brief's R-1 arms — they were the prompt-writer's error, already documented in the review's header.
* AR-3 Scope fence. Do not modify: `judge-v2-nist` behavior (P2-2 pending), `artifacts.lock.edn` beyond its header comment (P2-3 pending), `verdict-cache` (P2-4), intake staging behavior (P2-5), any component boundaries (5.1a), `ehrt.sim.result` (P3-5), `bases/sim-cli` (P3-6). If a fix seems to require crossing this fence, record a deviation and stop that sub-item instead.
* AR-4 Generated docs are edited at the source. `docs/use-cases.md` and `docs/cli.md` are regenerated, never hand-edited; changes go in `use-cases.edn` / the cli-spec, then `make use-cases` (etc.) regenerates. Anchors must survive (AUTHORS-GUIDE §6).
* AR-5 Makefile phantoms become real. `make quickstart-fresh` and `make lint-pipeline` are named in docstrings but don't exist; the functions do. Add the two targets (thin wrappers over the existing fns, mirroring neighboring targets) rather than weakening the docstrings — making the prose true is the cheaper fix-forward here.
* AR-6 Test-first. For every new tripwire/test in Steps 1–4: write it, watch it fail on the pre-fix tree (or on a deliberate seeded violation where the tree is already clean), then land the fix and watch it pass. Note each red→green in the summary.

[Steps 1–5 and Close-out sections as delivered in chat — five steps: (1) AGENTS.md/architecture.md structure catch-up + structure-currency test; (2) stale-citation errata sweep + lint tripwire, with the use-cases.edn:146 addendum folded in; (3) restore CLAUDE.md + presence test; (4) generated-doc freshness gate in CI; (5) facts-register catch-up — reproduced in full above this archive's own prompt-log entry, omitted here only to avoid duplicating the chat transcript verbatim a second time in this file.]

## Deviation record (2026-07-31)

**AR-3 fence: no contacts.** Nothing in `judge-v2-nist` behavior, `verdict-cache`, intake staging, component boundaries, `ehrt.sim.result`, or `bases/sim-cli` was touched. `artifacts.lock.edn` was touched only at its header comment (the six NIST rows' "not yet consumed" claim), exactly as P1-1 scoped it — no other line in that file changed.

**Staging, not committing.** Per the prompt, no commit or push ran this session. Two things *were* staged (`git add`, not `git commit`) as part of ordinary prep, disclosed here since the prompt didn't explicitly anticipate either:
- `CLAUDE.md` and `bases/cli/test/ehrt/cli/claude_md_presence_test.clj` — the new hygiene test asserts against the git *index* (AUTHORS-GUIDE §7a), so proving its own red→green (AR-6) required staging the file; left staged for the author to fold into Step 3's commit.
- `components/tools/docs/use-cases.edn` and `docs/use-cases.md` (Step 2) — staged to establish a clean baseline against which the Step 4 freshness-gate mechanism (`make docsgen && git diff --exit-code`) could be locally validated (idempotent regen, then a seeded one-byte drift in `docs/cli.md`, confirmed caught, reverted). Left staged for the author to fold into Step 2's commit.
All other steps' files are unstaged, modified-in-tree only, matching each step's own file list below.

**F7's refresh number: 190, not 185, per the prompt's own instruction to re-run and cite my own count if it differs.** The prompt's own text said "refresh F7 (177 → 185, with run evidence)," but 185 was itself the 2026-07-30 review session's own count, already one session stale by the time this batch ran (three new per-push tests landed in Steps 1 and 3). Cited all three figures (177 CI, 185 review, 190 this session) rather than overwriting silently, per AR-1.

**A citation I drafted and self-caught before it landed.** While wording F3's supersession note, I first cited "`AUTHORS-GUIDE.md`'s own SETUP-revalidation bar" for why the walk wasn't rewalked this session — grepped `AUTHORS-GUIDE.md` for it before finalizing and found no such stated rule (it doesn't exist as a named bar there). Reworded to cite F3's own actual bar instead (R40: a fresh-context agent with no prior repo knowledge or session memory), which is real and already in the row. Caught pre-commit-to-file, not left in; recorded per AGENTS.md's "do not invent facts... record it and ask" discipline, even though no external question was needed once the correction was made.

**`docsgen_test.clj`'s prose fix: two spots, not one.** The task named "lines 20-22"; by the time I reached this file its own line numbers (after nothing else touched it) put the actual claim needing correction at the top docstring (originally lines 2-4) *and* a second, near-identical restatement in the `test-cli-spec` comment block further down (~line 23-24 pre-edit). Fixed both, since leaving the second uncorrected would have left the same false "asserts a gate that didn't exist" claim standing right below the fixed one.

**`notes/2026-07-30-refactoring-review.md` left untracked, as found.** Present in the working tree (confirmed non-absent per the prompt's own stop condition) but not part of any of the five steps' own file lists, and not committed by this session — the author's own call, not this session's to make silently.

**Formats.md spot-check (finding 15): clean, no drift.** Two representative examples re-captured live this session — the `gate v2` report shape (piped, unpaced) and its `--json` projection — both matched `docs/formats.md`'s claimed shapes field-for-field, including the lineage-record example's own hashes reproducing byte-for-byte from the documented mutation. Nothing in the doc needed a dated note; touched nothing.

**No unexpected `git status` dirt at any step boundary.** Every check across all five steps showed exactly that step's own files, nothing else, aside from the pre-existing untracked review-report file disclosed above.

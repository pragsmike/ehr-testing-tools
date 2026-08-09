# 2026-08-09 — ehr-testing-tools: cluster B -- CLI parse guards (build session)

## Context

Archived 2026-08-09. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `b8fac5a` (cluster A gate wiring, ADR-0095) and
closed at the fix + docs commit plus this record's own commit. Original
prompt follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

2026-08-09 -- ehr-testing-tools: cluster B -- CLI parse guards
Context
Conventions read at HEAD `b8fac5a` (cluster A gate wiring, ADR-0095), design channel, 2026-08-09, verified by fresh public clone. The author ruled 2026-08-09, verbatim: "Cluster B." -- ADR-0092's fix cluster B (register rows D4-5, D4-6, D4-7, D8-3): four unguarded reads, one root cause, one already-precedented fix shape. NO new design -- the pattern is `kernel/artifact.clj/read-lockfile` (lines 55-69, categorized `:parse-failed` rejection) and `sim/run.clj`'s config loader (lines 254-260, `:config-unreadable`). D8-4 (bare-level unknown-flag tolerance) remains explicitly UNRULED and OUT of scope. Cluster C is already fully consumed (D8-7 by ADR-0093, D7-7/D7-8 by the same; nothing rides along here).
Design-channel probe facts at `b8fac5a` (`bases/cli/src/ehrt/cli/core.clj`):

* D4-5: `read-base-data` (lines 386-392) -- `:fhir` branch `(json/read-str (slurp file))`, `:v2` branch bare `(slurp file)`, no guard on either. Two callers, lines 505 and 681, both already inside result-plumbed flows. NOTE: guarding this fn at the result level ALSO closes the `corpus mutate` leg of D8-3 (permission- denied slurp is the same catch), one fix, two register rows.
* D4-6: `gate-command` `--baseline` (line 911), `(edn/read-string (slurp baseline))` bare inside a `let`.
* D4-7: `check-command` `--assertions` (line 1552), same idiom.
* D8-3: the `gate`/`show` file-open legs -- AR-RL-3's fix added `.exists` pre-checks only (lines 1097-1103, 1178-1184), the actual reads downstream still throw raw on a permission-denied (exists, unreadable) target. `sim run --config` on the SAME file returns a clean categorized error (the ADR-0060 try/catch) -- the correct pattern one file away, the register's own words.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward, record HEAD (expect `b8fac5a`; later escalates unless explained). Commit messages ASCII-only; post-push ASCII check FIRST, standing.
Read first

1. Register rows D4-5, D4-6, D4-7, D8-3 IN FULL (`.agents/plans/2026-08-09-repo-review-findings.md`) and ADR-0092's Cluster B paragraph -- the charter, including the co-landed-lint spec.
2. `components/kernel/src/ehrt/kernel/artifact.clj` lines 55-69 and `components/sim/src/ehrt/sim/run.clj` lines 250-261 -- the two precedent shapes the fixes must match.
3. `components/docs-tooling/test/ehrt/docs_tooling/ io_vocabulary_lint_test.clj` IN FULL -- the sibling lint whose mechanism the new lint reuses: allowlist BY NAMESPACE, patterns anchored to CALL syntax so a docstring mentioning the idiom is never a false positive (its own header explains both).
4. `bases/cli/src/ehrt/cli/core.clj` -- the four sites, the `result->exit-code` mapping (errors already map to exit 2; NO mapping changes), and the file's existing category-naming voice (`:file-not-found`, `:gate-path-not-found`).
5. `notes/adr/0095-cluster-a-gate-wiring.md` -- tag-debt section.

Author rulings

* AR-CB-0 [A] (ADR-0095, successor tag debt): tag `stable-20260809-cluster-a-gate-wiring` at `b8fac5a`, Step 0, ANNOTATED, standing ceremony. Verify-and-disclose if present.
* AR-CB-1 [A] (the four fixes, ruled as ADR-0092 wrote them): try/catch-around-the-read, categorized `result/error` carrying `:path` and `:message`, flowing through the EXISTING result plumbing to the EXISTING exit-2 operational-error class. Category names follow the file's own local voice (the precedents' `:parse-failed`/`:config-unreadable` register is the model); naming is session discretion, mapping changes are NOT. `read-base-data` becomes result-returning, its two callers short-circuit -- the minimal caller change, nothing restructured around it.
* AR-CB-2 [C] (red evidence first, all four, live): before any fix, reproduce each raw failure against scratch inputs and paste verbatim into ADR-0096: malformed `.json` through `corpus mutate` (D4-5), malformed EDN through `gate --baseline` (D4-6) and `check --assertions` (D4-7), and a `chmod 000` existing file through `corpus mutate`, `gate fhir`, and `show` (D8-3, three commands). Then after the fix: the same six invocations, each now a categorized error with exit 2, pasted. The before/after pair is the session's spine.
* AR-CB-3 [C] (co-landed behavioral gates): new or extended test in `bases/cli/test/ehrt/cli/` (session's naming discretion in the family convention), tempdir-built, PORTABLE: malformed-JSON mutate, malformed-EDN baseline, malformed-EDN assertions each assert a categorized `result/error` (never a thrown exception) and the exit-2 mapping. Permission-denied stays SESSION EVIDENCE (AR-CB-2), not a committed test, unless the session finds an existing skip-when-root/non-POSIX guard convention to reuse -- a chmod-based test that lies under root is worse than no test (D3's own environment-independence lesson; CI containers often run as root). If omitted, ADR-0096 says so and why, one sentence.
* AR-CB-4 [C] (co-landed static lint, the charter's own spec): a sibling of `io_vocabulary_lint_test.clj` (suggested `cli_parse_guard_lint_test.clj`) scoped to `bases/cli/src/`: flag a bare `(edn/read-string (slurp ...))`, `(json/read-str (slurp ...))`, or bare `(slurp ...)` on an operator-supplied path inside a top-level `defn` with no enclosing `(try` in that same form. Function-granular (split top-level forms, the sibling's method family); allowlist by namespace if a guarded helper namespace needs it; call-syntax-anchored patterns so docstrings and comments never trip (the sibling's own hard-won rule). WITNESS PAIR, cluster A's method: the lint run against the session's own PRE-FIX tree must report exactly the four charter sites and nothing else; against the fixed tree, clean. Both outputs pasted. Anything else it reports at pre-fix is a STOP-AND-REPORT (either the lint misencodes its invariant or the register missed a fifth site -- finding vs escalation, do not guess).
* AR-CB-5 [C] (docs freshness): `make docsgen` before commit; if the CLI's generated docs surface the new categories, the regen rides the same commit. `docs/cli.md`'s exit-code table is already truthful (exit 2 unchanged) -- verify, touch only if regen says so.

Steps
Step 0 -- Preflight + tag (AR-CB-0). Standard preflight (clean tree, HEAD `b8fac5a`, untracked disclosure, `clojure -M:poly check`, oracle pre-digest `b8fac5a b8fac5a` -- 34 roots IDENTICAL expected; all lanes' latest conclusions disclosed). Tag. No commit.
Step 1 -- Red evidence (AR-CB-2 first half). The six raw failures, live, pasted. No commit.
Step 2 -- The four fixes (AR-CB-1). Matching the precedent shape exactly. No commit.
Step 3 -- Gates (AR-CB-3/4). Behavioral tests green; the lint's witness pair (pre-fix worktree trip on exactly four sites, clean at the fixed tree); the six green invocations (AR-CB-2 second half). Full local suite; oracle bracket vs worktree -- PURE IDENTITY expected on all 34 roots, any movement STOP-AND-REPORT. No commit.
Step 4 -- ADR + ceremony surfaces + commit. `notes/adr/0096-cluster-b-parse-guards.md`: charter rows quoted, the six-fold before/after pasted, the lint witness pair pasted, the permission-denied-test decision (AR-CB-3) stated, category names chosen and where they surface, docsgen result, oracle identity, this session's own successor tag debt. Index line; README count 93->94; roadmap Done pointer only. Single commit, fixes + both gates + docs together (co-landed invariants, standing):

```
fix: cluster B -- CLI reads guarded, categorized, linted (ADR-0096, D4-5/6/7 D8-3)

```

Push; ASCII check FIRST, then message verification; watch CI to conclusion, all lanes noted.
Step 5 -- Ceremony. Self-archive this prompt at the START of the close phase (`2026-08-09-cluster-b-parse-guards.md`), session record, both READMEs, one commit:

```
docs: session record and prompt archive -- cluster B parse guards

```

Same verification order.
Fences
Src changes in `bases/cli/src/ehrt/cli/core.clj` ONLY -- the four sites plus `read-base-data`'s two callers' short-circuits, nothing else in the file, nothing in corpus/kernel/sim/judge/engine src. No `result->exit-code` or exit-mapping changes. No D8-4 work in any form -- if the fixes' vicinity touches the bare-level flag path, STOP-AND-REPORT rather than improvise the author's open call. New test files: the behavioral test and the lint, nothing else. No workflow or Makefile edits (cluster A landed those). If the lint's pre-fix run or the suite finds a LIVE fifth site, STOP-AND-REPORT before fixing it -- widening the charter is the author's call.
Close-out
Echo to chat: the six-fold before/after verbatim (raw then categorized, per command); the lint witness pair; the category names chosen; the permission-denied-test decision; oracle verdict; shas, CI status across all lanes.

## Deviation record

- **AR-CB-1's "minimal caller change" scope.** `read-base-data`'s two
  callers got the minimal short-circuit the prompt named. `sniff-path-
  format` — D8-3's own shared helper, not separately named as a
  function in the prompt's probe facts (only its two call SITES,
  lines 1097-1103/1178-1184, were cited) — needed a wider caller
  update than "minimal": its directory-scan callers
  (`sniff-gate-command`, `show-command`) each mapped it over multiple
  files inline, which cannot short-circuit on a Result without a small
  reduce-based helper (`sniff-files`, new). Mechanical, no new design —
  same short-circuit-on-first-failure discipline the file's own
  `mutate-command` docstring already states as its "fails fast"
  convention.
- **D8-3's `gate fhir` leg — found unfixable in-fence, escalated
  mid-session, ruled.** AR-CB-2's own red-evidence pass found `ehrt
  gate fhir PATH`'s permission-denied crash bottoms out in
  `judge-fhir-official`/`kernel.digest`, three frames past `core.clj`,
  outside the stated fence. Surfaced via AskUserQuestion mid-session
  (not silently adapted, not silently widened); ruled: fix the two
  in-fence D8-3 legs (bare `gate`, `show`), disclose `gate fhir` as a
  new, unfixed finding in ADR-0096. Full text of the finding and the
  live evidence are in the ADR, not repeated here.
- **A second, later-discovered scope question — `ehrt play`'s own
  bare reads — resolved by extension of the same ruling, not
  re-asked.** Updating every caller of the now-Result-returning
  `sniff-path-format` (required for correctness — the full suite went
  red, 36 failures/3 errors, until every caller unwrapped the new
  Result) surfaced that `play-events-from-file`/`play-events-from-dir`
  carry the identical bare-`slurp` shape this session's own gate
  exists to catch, never named in the charter. Applying the SAME
  principle the prior ruling already established (fix only the four
  charter rows' own sites, disclose everything else) rather than
  re-asking the same question: their calls to `sniff-path-format`
  were updated (mechanical, required), their OWN bare reads were left
  alone, and the new lint allowlists both by name with inline
  disclosure. Full finding in ADR-0096.
- **The lint's own witness pair reports five function names pre-fix,
  not four.** Not a sixth deviation, a labeling note: D8-3's own
  "show" row spans two physical functions (`sniff-path-format`,
  shared with `gate`, and `show-file`'s own second, content-rendering
  read) — both fixed, both part of the same charter row. See
  ADR-0096's own "labeling note" paragraph.
- **The oracle bracket was captured via a real temporary local commit
  + `git reset --soft`, not `git stash create`** (cluster A's own
  precedent method). Functionally equivalent — both produce a commit
  object the oracle script's own `git worktree add --detach` can check
  out without disturbing the session's own in-flight dirty tree — but
  named here since the prompt's own Step 3 wording ("oracle bracket
  vs worktree") did not specify the mechanism and `bin/regression-
  oracle` itself requires two real refs, never a dirty working tree
  directly.

# 2026-08-09 — ehr-testing-tools: cluster A -- CI/gate wiring (build session)

## Context

Archived 2026-08-09. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `7234f8c` (census closure-file-count fix,
ADR-0094) and closed at `d17f9dc` (ADR-0095) plus this record's own
commit. Original prompt follows verbatim; a deviation record follows
that.

## Original prompt (verbatim)

2026-08-09 -- ehr-testing-tools: cluster A -- CI/gate wiring
Context
Conventions read at HEAD `7234f8c` (census closure-file-count fix, ADR-0094), design channel, 2026-08-09, verified by fresh public clone. The author ruled 2026-08-09: "Let's do cluster A" -- ADR-0092's fix cluster A (register rows D2-18, D2-4), verbatim in the ADR: two "a check exists (or should exist) but doesn't run where it matters" gaps.
Design-channel probe facts at `7234f8c`:

* `bin/verify-nist-lock` IS wired in `Makefile` lines 38-41 (`test:` target, after the poly steps) but is ABSENT from `.github/workflows/test.yml` -- the lane every push actually runs (`poly check` -> `poly test :all skip:integration` -> generated-doc freshness; no lock check). The script's own header (line 24) claims "Wired into `make test` (the per-push lane)" -- the wiring claim is true, the per-push-lane claim is false.
* `2088763` is the FIX commit for the classpath-break incident ("integration lane regains judge-v2-nist"): at `2088763~1`, `components/judge/test/ehrt/judge/pairing_conviction_test.clj` requires `ehrt.judge-v2-nist.interface` while the integration project's `deps.edn` lacked that brick -- the exact class D2-18's gate must catch, and a ready-made historical witness pair.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward, record HEAD (expect `7234f8c`; anything later escalates unless explained). Commit messages ASCII-only; post-push ASCII check FIRST, standing.
Read first

1. `.agents/plans/2026-08-09-repo-review-findings.md` rows D2-4 and D2-18 IN FULL, and ADR-0092's Cluster A paragraph -- the charter.
2. `components/docs-tooling/test/ehrt/docs_tooling/ sim_emit_hl7_dependency_test.clj` -- the reader-based extraction method the new gate reuses (the register names it as the sibling).
3. `bin/verify-nist-lock` header in full -- the three exit codes (0 match / 1 MISMATCH / 2 not-yet-resolved) and the line-24 claim to correct.
4. `workspace.edn` `:projects` and each project's `deps.edn` -- the composition data the gate parses. Note `development`'s `:necessary ["oracle"]` comment (ADR-0050, row A-1): existing, explained composition history, not a violation.
5. `.github/workflows/test.yml` in full -- step order and the dependency-resolution point the new step must follow.
6. `notes/adr/0094-census-closure-file-count.md` -- tag-debt section.

Author rulings

* AR-CA-0 [A] (ADR-0094, successor tag debt): tag `stable-20260809-census-closure-file-count` at `7234f8c`, Step 0, ANNOTATED, standing ceremony. Verify-and-disclose if present.
* AR-CA-1 [A] (D2-18, the static gate): new docs-tooling test (suggested `project_classpath_test.clj`, session's naming discretion within the family convention), reader-based like its sibling: for EVERY project named in `workspace.edn` (development included -- it composes everything and passes trivially, cheap generality), parse each composed brick's TEST-tree `:require` forms, resolve each `ehrt.<name>.*` namespace to its owning brick, assert that brick appears in the composing project's own `deps.edn` (or its documented `:necessary` list). Failure message names the test file, the required namespace, the owning brick, and the project whose composition lacks it -- the `2088763` commit message is the model of the disclosure this gate automates. The gate self-wires into the push lane by being an ordinary docs-tooling test; no workflow edit needed for (i).
* AR-CA-2 [C] (the gate's witness pair, recorded in ADR-0095): run the gate's extraction logic against a WORKTREE at `2088763~1` -- it MUST report exactly the judge / `ehrt.judge-v2-nist` / integration-project violation and nothing else; then against HEAD -- it MUST pass clean. Both outputs pasted. If the historical trip reports anything other than that one violation, STOP-AND-REPORT (either the gate misencodes its invariant or the window holds an undisclosed second break -- finding vs escalation per AUTHORS-GUIDE section 7, do not guess which).
* AR-CA-3 [A] (D2-4, the wiring): add `bin/verify-nist-lock` as an explicit named step in `test.yml` AFTER the `poly test :all skip:integration` step (the suite's own dependency resolution populates the Maven repo the check reads; placed before it, exit 2 "not yet resolved" would fail the lane spuriously). If CI's resolved-repo path differs from the script's `~/.m2` default, pass `--repo` explicitly rather than papering over a trip. If the step trips on CI anyway, that is a FINDING to disclose and fix in the open, never to mask with `|| true`.
* AR-CA-4 [C] (the check can actually fire -- D2-4's own verification demand): before landing, run `bin/verify-nist-lock --repo <empty scratch dir>` locally and paste the exit-2 "not yet resolved" output into ADR-0095 -- proof the check trips when reality diverges, not just passes when it doesn't. Also run it against the real local repo (exit 0, six coordinates) and paste that.
* AR-CA-5 [C] (the header): correct `bin/verify-nist-lock` line 24 to name the ACTUAL surfaces truthfully post-wiring: `make test` (local convenience target) and the `test.yml` push lane. No other script changes.
* AR-CA-6 [C] (Makefile): already correctly wired (lines 38-41) -- verify-and-disclose in ADR-0095, change NOTHING.

Steps
Step 0 -- Preflight + tag (AR-CA-0). Standard preflight (clean tree, HEAD `7234f8c`, untracked disclosure, `clojure -M:poly check`, oracle pre-digest `7234f8c 7234f8c` -- 34 roots IDENTICAL expected; all lanes' latest conclusions disclosed). Tag. No commit.
Step 1 -- The gate + witness pair (AR-CA-1/2). Write the test; green at HEAD; then the `2088763~1` worktree trip, both outputs captured. No commit.
Step 2 -- The wiring + trip proof (AR-CA-3/4/5/6). The `test.yml` step, the header correction, the scratch exit-2 and real exit-0 runs captured, the Makefile disclosure. No commit.
Step 3 -- Suite + bracket. Full local suite (the new gate runs inside it -- confirm it appears in the run); oracle bracket `7234f8c` vs worktree -- PURE IDENTITY expected on all 34 roots, any movement is STOP-AND-REPORT. No commit.
Step 4 -- ADR + ceremony surfaces + commit. `notes/adr/0095-cluster-a-gate-wiring.md`: the charter rows quoted, the witness pair pasted (both directions), the exit-2/exit-0 proofs, the header correction rationale, the Makefile disclosure, oracle identity, this session's own successor tag debt. Index line; README count 92->93; roadmap Done pointer only. Single commit, gate + wiring

* docs together (co-landed invariants, standing):
fix: cluster A -- classpath static gate lands, verify-nist-lock joins the push lane (ADR-0095, D2-18/D2-4)

Push; ASCII check FIRST, then message verification; watch CI to conclusion -- THIS run is itself AR-CA-3's first live witness: the new step's presence and conclusion in the run log are quoted in the session record. All lanes noted.
Step 5 -- Ceremony. Self-archive this prompt at the START of the close phase (`2026-08-09-cluster-a-gate-wiring.md`), session record, both READMEs, one commit:

```
docs: session record and prompt archive -- cluster A gate wiring

```

Same verification order.
Fences
No CLI src anywhere (cluster B is its own session; D8-4 still awaits the author's call). No census, sim, judge, or engine src. Workflow edits: the ONE `test.yml` step, nothing else in any workflow. No Makefile changes. `bin/verify-nist-lock`: the header comment lines ONLY, no logic. New test file + the one workflow step + the header lines + ceremony surfaces are the session's entire footprint. If the gate finds a LIVE violation at HEAD (not the historical one), STOP-AND-REPORT before any fix -- fixing it is not this session's charter.
Close-out
Echo to chat: the witness pair verbatim (trip at `2088763~1`, clean at HEAD); the exit-2 scratch proof and exit-0 real run; the test.yml step as landed and its first CI conclusion; the header line before/ after; oracle-bracket verdict; shas, CI status across all lanes.

## Deviation record

- **Oracle bracket before commit:** Step 3 asked for a bracket "`7234f8c` vs worktree." `bin/regression-oracle` only accepts git refs (it runs `git worktree add` internally), so the same technique the prior session (ADR-0094) used was repeated: the session's own uncommitted, in-flight changes were captured via `git stash create` into a dangling commit object (`cd88828c...`, no working-tree effect, nothing added to the stash list), and that object's own SHA stood in for "worktree." Disclosed here rather than silently substituted; same precedent, not a new deviation class.
- **`development`'s composition source, made explicit:** the ruling's own read-first note named `development`'s `:necessary ["oracle"]` entry but did not specify HOW the gate should determine `development`'s own composed-brick set, since `projects/development/deps.edn` does not exist. The gate resolves it via the root `deps.edn`'s own `:dev` alias (`workspace.edn`'s own `:alias "dev"` entry for that project) — the same alias `root_alias_completeness_test.clj` already treats as `development`'s real composition source. A judgment call within AR-CA-1's own stated discretion ("session's naming discretion within the family convention"), not a deviation from its intent.

No other deviations. All fences held: no CLI/census/sim/judge/engine
src touched; exactly one `test.yml` step added; no Makefile change;
`bin/verify-nist-lock` touched only at its header comment lines; no
live violation found at HEAD (only the historical `2088763~1` witness
tripped, as designed).

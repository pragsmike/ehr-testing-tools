# 2026-08-05 — Scaffolding compaction A: riders, vestige retirements, Deferred triage

## Scope

Design-channel session prompt naming AR-A-1 through AR-A-5. Session A
of the scaffolding-compaction arc (B — the ADR/roadmap restructure —
and C — the continuity register — are both pending author rulings, not
this session's scope). Three riders from the standing-equipment
promotion's own verification, two code-level vestige retirements the
design channel's own audit named, and the roadmap's Deferred-section
triage (19 rows, author-ruled classification: LIVE untouched,
CLOSED-WITH-NOTE relocated, STALE-AUDIT closed iff Step 0's evidence
confirms). Full account and verbatim rulings: `notes/ADRs.md`
ADR-0045.

Step 0 (characterize) verified tip `2d1dcf3`. **Dual-clone hazard hit
immediately and worked around, not silently:** the session's default
working directory resolved to the `/mnt/c` clone (read-only, and stale
— 1127 lines of `roadmap.md` there against 1472 on the ext4 clone of
record at session start). Every real read and edit for the rest of the
session routed through `wsl -e bash -lc` against `~/src/ehr-testing-
tools`, per the `build-session` skill's own preflight rule (J4d). Fresh
grep confirmed: zero external callers of `sim_adapter.clj`'s legacy
discovery keys; `ehrt.sim.manifest/build` stamps `"ehrt.sim"`, not
`"ehr-testing-sim"`; the interpreter's `active-careplan-condition-
holds?` is live (Wave I2, ADR-0041 AR-2); `ehrt.sim-model.persona`'s
`:race` field is live and Wave LC's own census moved both the
race-blocked and time-blocked modules to `:ok-walked`.

Step 1 (`7b0fa16`, AR-A-1) corrected `census_test.clj`'s two "the
roadmap's own Wave I finding" citations — no such row exists (GMF Wave
I is an unrelated arc) — to cite ADR-0044's own correction instead,
with the underlying invisibility claim's live before/after evidence
(202 `poly test` blocks before the standing-equipment promotion, 0
census assertions; 204 after) recorded in place of the retracted
citation.

Step 2 (`05bd9b7`, AR-A-3/AR-A-4) retired `sim_adapter.clj`'s
sibling-checkout discovery-key tolerance (`:sim-dir`,
`:env-sim-dir-fn`, `:default-dir`) — ADR-0012's own in-process mount
(2026-07-28) had already made the discovery dead code, and this
session's own fresh grep found zero real callers left. Proven live,
not merely asserted: a `clojure -M:dev -e` call confirmed the three
keys now leak through into the captured opts map unstripped, where
before this change they were silently absorbed. `intake_test.clj`'s
`sample-manifest` fixture's `:generator :name` corrected
`"ehr-testing-sim"` → `"ehrt.sim"`, read fresh from
`ehrt.sim.manifest/build`.

Step 3 (`4720fd6`, AR-A-2/AR-A-5) added the Deferred section's own
one-line contract ("Rows here are LIVE. Closed rows move to Done with
their notes.") and relocated four already-closed-with-note rows
verbatim to a new Done section: sim-manifest interop, sim split S4,
the J2 oracle-harness limitation, the docs coherence pass. Three
STALE-AUDIT closures on this session's own fresh evidence: budget
numbers (AR-D-3), Active CarePlan (ADR-0041 AR-2 + the live interpreter
clause), and the `race` half of the combined `race`/`time`
lookup-column row (ADR-0038 + the live persona field) — the `time`
half stays explicitly LIVE per the ruling's own "either way" clause,
recorded with its column-resolution evidence rather than silently
dropped, since a different, still-open schema-invalid-family `time` gap
(ADR-0039, named separately in the Next section) is not the same
concern.

Step 4 (this record, final commit) verification: `bin/regression-oracle
2d1dcf3 4720fd6` — all ELEVEN vendored-root batches byte-identical.
Deftest count 1566 → 1566 (`git grep -o '(deftest ' <ref> -- '*.clj' |
wc -l`), a wash — the one `sim_adapter_test.clj` rename accounted for.
`clojure -M:poly check`: OK throughout; `clojure -M:poly test
project:dev`: 98 test blocks, 0 failures, 0 errors, run fresh after
every code-adjacent step. `ehrt.docs-tooling.reading-set-budget-test`:
green, `:onboarding` at 1390 lines against its 2405-line budget.
`notes/ADRs.md` ADR-0045 records every ruling verbatim with its own
evidence.

## Deviations, disclosed

- **AR-A-2's commit boundary.** The prompt's own suggested Step 1
  commit message named both AR-A-1 and AR-A-2 ("budget row closes"). In
  execution, Step 1's commit carries AR-A-1 only — the budget-numbers
  closure note and its relocation to Done both landed together in Step
  3's commit instead, since the ruling's own text already says
  relocation "rides Step 3" and building the note in place in Step 1
  only to move it two steps later added a diff with no reader benefit.
  Substance unchanged; only which commit's diff carries it.

## Findings (disclosed, not fixed — out of this session's own scope)

- **Dual-clone drift, `/mnt/c` vs ext4.** `roadmap.md` on `/mnt/c` was
  345 lines behind the ext4 clone of record at session start (1127 vs
  1472) — the guardrails (J4a/b, ADR-0030 J4) correctly kept the
  session from ever writing to the stale copy, but the drift itself
  means `/mnt/c` had not been synced via `bin/sync-mnt-c` in some time.
  Not fixed this session (sync is an explicit, separate action, not
  implied by a read-only guard firing correctly); named here so a
  future session doesn't rediscover the same gap cold.
- **The AR-A-1 lesson names its own future gate, not built here.**
  Intra-session artifacts (like the driving prompt for the standing-
  equipment promotion) can cite a claim before a later step in the SAME
  session discovers it was wrong — the citation and the correction land
  in different files, and nothing currently sweeps the earlier one.
  Revisit trigger: session B or C of this compaction arc, per the
  ruling's own explicit deferral.

## Verification

- `bin/regression-oracle 2d1dcf3 4720fd6`: all 11 roots byte-identical
  (`appendicitis`, `death-fixture`, `ear-infections`,
  `ear-infections-engine`, `ear-infections-history-engine`, `sepsis`,
  `sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`).
- Deftest parity: 1566 → 1566, wash.
- `clojure -M:poly check`: OK. `clojure -M:poly test project:dev`: 98
  blocks, 0 failures, 0 errors (run after Steps 1, 2, 3, and this
  record's own final commit).
- `gitleaks git --staged -v`: clean, every commit this session.

Commits, in order: `7b0fa16` (Step 1), `05bd9b7` (Step 2), `4720fd6`
(Step 3), and this session's own closing records commit (Step 4).

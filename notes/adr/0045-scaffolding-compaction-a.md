<!-- Attic file: notes/adr/0045-scaffolding-compaction-a.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0045 — Scaffolding compaction A: riders, vestige retirements, Deferred triage

**Status:** Accepted (author-ruled 2026-08-05, design channel, AR-A-1
through AR-A-5 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`). Executed same
day.

### Context

Session A of the scaffolding-compaction arc (B — the ADR/roadmap
restructure — and C — the continuity register — are pending author
rulings, not this session's scope). Three riders surfaced by the
2026-08-05 standing-equipment promotion's own verification (ADR-0044):
`census_test.clj` attributes its own invisibility finding to "the
roadmap's own Wave I finding," a citation ADR-0044 already corrected as
unfindable; the "Reading-set budget numbers" Deferred row was closed in
substance by the docs coherence pass's own AR-D-3 re-baseline
(`notes/ADRs.md` ADR-0043's own tail) but carried no closure note of
its own; two code-level vestiges (`sim_adapter.clj`'s sibling-checkout
discovery-key tolerance, `intake_test.clj`'s dead-repo-named fixture)
were named by the design channel's own audit. A full pass over the
roadmap's own Deferred section found several rows that had already
accumulated a "RESOLVED"/"CLOSED"/"FIXED ... see Done, below" note in
place without ever actually moving to Done — the Deferred section had
drifted from "open items" to a mix of open and quietly-closed items,
undermining its own purpose as a live backlog.

### Decision

Ruled 2026-08-05, design channel, recorded verbatim:

**AR-A-1 (citation propagation).** The two `census_test.clj` docstring
attributions to "Wave I" are corrected to match ADR-0044's own
provenance (the invisibility was confirmed by live before/after
evidence, not a roadmap row); each gets a dated one-line note. The
lesson generalizes and is recorded: intra-session artifacts written
before a later step's discovery must be swept in the same session — a
gate for this is session-B/C material, not built now.

**AR-A-2 (budget-numbers row).** Closure note citing AR-D-3, then the
row moves to Done per AR-A-5.

**AR-A-3 (sim_adapter tolerance retirement).** IFF fresh grep confirms
zero callers pass the legacy keys outside the adapter's own test: the
keys stop being accepted (the dissoc and its docstring tolerance
retire), the test updates to the current contract, dated note cites
ADR-0012 and the M1 sweep. If a real caller exists: KEEP, record the
caller, the finding stands.

**AR-A-4 (intake fixture).** The fixture's generator name aligns with
what sim's `build` actually stamps today (read fresh from
`components/sim/src/ehrt/sim/manifest.clj`, not from memory); dated
note. Pure test-data change; if any assertion keyed on the old name, it
updates in the same commit.

**AR-A-5 (Deferred triage).** LIVE rows untouched; CLOSED rows relocate
to Done with annotations intact (relocation, not rewrite); STALE-AUDIT
rows close-and-relocate iff Step 0's evidence confirms, each citing its
landing evidence, else stay with the audit finding recorded. The
Deferred section header gains one line: "Rows here are LIVE. Closed
rows move to Done with their notes."

### Execution note

**Step 0 (characterize).** Tip confirmed `2d1dcf3`. Fresh grep found:
zero external callers of `sim_adapter.clj`'s legacy discovery keys (the
two `generators_test.clj` hits are prose describing history, not opts
actually passed — `sim_adapter_test.clj`'s own
`run-strips-out-dir-and-discovery-keys-before-delegating-test` was the
only real caller); `ehrt.sim.manifest/build` stamps `:generator {:name
"ehrt.sim" ...}`, not `"ehr-testing-sim"`; `ehrt.sim-trajectory.gmf-
interpreter/active-careplan-condition-holds?` is live (Wave I2,
2026-08-04, ADR-0041 AR-2, commit `14e8dce`); `ehrt.sim-model.persona`'s
optional `:race` field is live (line 122) and Wave LC's own census
(ADR-0038) confirmed `acute-myeloid-leukemia` (race) AND
`hiv-diagnosis` (time) both moved `:load-failed` → `:ok-walked`.
`.agents/plans/roadmap.md`'s own Deferred section carried four rows
already closed-with-note (sim-manifest interop, sim split S4, the J2
oracle-harness limitation, the docs coherence pass) sitting un-
relocated. **Dual-clone hazard encountered and worked around, not
silently:** the session's default working directory resolved to the
`/mnt/c` clone (read-only, stale — 1127 lines of `roadmap.md` there
against 1472 on the ext4 clone of record); every read and edit this
session routed through `wsl -e bash -lc` against `~/src/ehr-testing-
tools` instead, per the `build-session` skill's own preflight rule.

**Step 1 (`7b0fa16`, AR-A-1).** `census_test.clj`'s namespace docstring
and `load-failed-json` fixture docstring both corrected. Full suite:
98 test blocks project-wide, 0 failures, 0 errors.

**Step 2 (`05bd9b7`, AR-A-3/AR-A-4).** `sim_adapter.clj`'s `run!` no
longer dissocs (or mentions accepting) `:sim-dir`/`:env-sim-dir-fn`/
`:default-dir`; `sim_adapter_test.clj`'s coverage updated to the
current contract. Proven live (not merely asserted): a `clojure -M:dev
-e` call confirmed the three legacy keys now leak through unstripped
into the captured opts map, where before this change they were
silently absorbed — the tolerance is actually gone, not just
undocumented. `intake_test.clj`'s `sample-manifest` fixture's
`:generator :name` corrected `"ehr-testing-sim"` → `"ehrt.sim"`; no
assertion in that file keyed on the old string (fresh-grepped before
editing). Full suite: 98 blocks, 0 failures, 0 errors.

**Step 3 (`4720fd6`, AR-A-2/AR-A-5).** Deferred section header gains
the one-line contract statement. Four closed-with-note rows relocated
verbatim to a new Done section: sim-manifest interop, sim split S4,
the J2 oracle-harness limitation, the docs coherence pass. Three
STALE-AUDIT closures, each with its own evidence recorded in the new
Done entry: budget numbers (cites AR-D-3), Active CarePlan (cites
ADR-0041 AR-2 + the live interpreter clause), and the `race` half of
the combined `race`/`time` lookup-column row (cites ADR-0038 + the live
persona field) — the `time` half stays explicitly LIVE per the ruling's
own "either way" clause, a slimmed row recording the column-resolution
evidence honestly without treating it as closure, since the Next
section's own separate schema-invalid-family `time` gap (ADR-0039) is a
different, still-open concern this evidence does not resolve.
`ehrt.docs-tooling.reading-set-budget-test`: green, `:onboarding`
(roadmap.md's own reading set) at 1390 lines against its 2405-line
budget, no anomaly. Full suite: 98 blocks, 0 failures, 0 errors.

**Step 4 (verification, this entry).** `bin/regression-oracle 2d1dcf3
4720fd6`: all ELEVEN vendored-root batches byte-identical (`appendicitis`,
`death-fixture`, `ear-infections`, `ear-infections-engine`,
`ear-infections-history-engine`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`,
`urinary-tract-infections-history-engine`) — expected, since every
change this session is docs/docstring/corpus-adapter-side, outside
every digest path. Deftest count: 1566 before, 1566 after (`git grep -o
'(deftest ' <ref> -- '*.clj' | wc -l`) — a wash, the one test rename in
`sim_adapter_test.clj` accounted for, not a net change. `clojure -M:poly
check`: OK throughout. Roadmap's own Done header for this session
retitled to cite this ADR by number (was drafted mid-session citing
"ADR-0044 appendix" before the numbering decision landed on a fresh
entry instead).

### Fence

No session-B/C restructure work. No Deferred-row deletions —
relocation and annotation only. No adapter behavior changes beyond the
ruled tolerance retirement. No new gates (the AR-A-1 lesson about
sweeping intra-session citations is named as future gate material, not
built here). Frozen archives untouched.

---

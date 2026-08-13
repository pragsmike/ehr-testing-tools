# 2026-08-13 — ehr-testing-tools: positive-seed invariant violation, diagnosis (ADR-0122)

## Context

Archived 2026-08-13. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `f483ab7` (ADR-0121's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.

## Original prompt

Session prompt -- positive-seed invariant violation: diagnosis (ADR-0122)

You are Claude Code executing under R30 ceremony for ehr-testing-tools,
working for mg. DIAGNOSIS-ONLY session: ZERO src, ZERO test-code
commits -- the deliverable is a root-cause diagnosis and fix options
for an author ruling, never a fix. STOP on any conflict with the tree.
Standing notes: full `make test` before EVERY push; companions in-fence
by rule; the engine defspec's post-ADR-0116 gate policy IS STANDING
REPO-WIDE: any generative failure in
`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
is a new finding, never re-run past (R8's license died with ADR-0116's
resolution -- this session records that clarification).

### The finding (channel-verified 2026-08-13)

During ADR-0121's gate, the defspec failed at seed `1786589996178`
(failing-size 144) -- a NON-NEGATIVE, contract-legal seed, post-0116
generator. A real invariant-catalog violation at legal input. The S3
session re-ran past it citing R8; the recharacterization and the
prompt-side gate-policy carry-forward gap (channel-owned) land in this
session's records.

### Read first

1. `notes/adr/0116-engine-seed-contract.md` -- the diagnosis method
   precedent (its Step 1) and the gate-policy text.
2. `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` -- the
   defspec (post-0116 generator) and its property body.
3. `notes/adr/0121-*.md` -- the Verification section this session's
   erratum corrects.
4. `components/sim-check/` -- the invariant catalog (whichever
   invariant the repro names, read its checker).
5. `.agents/rulings.md` tail; `.agents/plans/roadmap.md`.

### Step 0 -- Preflight and tag ceremony

- origin/main at `f483ab7`; CI green (`gh run list --limit 5`,
  completing ADR-0121's channel leg). Else STOP.
- Tag `stable-20260812-manual-s3`, ANNOTATED, at `f483ab7`; push;
  peeled exact. Case (i): channel fresh-clone verification 2026-08-13
  (lineage, ASCII x3, zero src/test, docs-only footprint, oracle
  identity), CI per preflight.

### Step 1 -- Records first (commit 1)

1. ADR-0121 erratum (fix-forward, dated): append to its Verification
   section -- the gate event is recharacterized: a positive-seed
   invariant violation (new finding), not the retired R8 flake; R8's
   standing license ended at ADR-0116; the S3 session's re-run-past is
   understandable given the prompt's "standing" shorthand
   (channel-owned carry-forward gap) but the event required a STOP;
   diagnosis chartered here (ADR-0122).
2. Rulings "From ADR-0122": the author's verbatim "Both a." (2026-08-13)
   ruling (a) diagnosis-before-S4 and (b) the ceremony-scripts charter;
   plus the R8-scope clarification and the standing gate policy above.
3. Roadmap: new row -- positive-seed invariant violation, seed
   `1786589996178` verbatim, this session -> diagnosis; new row --
   ceremony scripts + skill absorption (tag ceremony, preflight,
   post-push verify, close-phase scaffold as scripts; checkpoint
   isolation, red capture, sweep census absorbed into the build-session
   skill), scheduled post-manual-arc; S4 row noted awaiting this
   diagnosis. Message: `docs: recharacterize s3 gate event; charter
   diagnosis and ceremony scripts (ADR-0122)`

### Step 2 -- Diagnosis (no fixes; evidence into the ADR)

1. Repro. `clojure.test.check/quick-check` on the property, 150 trials,
   `:seed 1786589996178` -- must fail; capture the SHRUNK minimal
   counterexample (a non-negative seed value). If it does NOT
   reproduce, STOP-AND-REPORT (generator or property drift since S3
   would itself be the finding).
2. Direct witness. Run the shrunk seed through the property's exact
   engine config + `check/check-all`; name the violated invariant(s)
   and the offending events verbatim (patient, times, event ordering).
3. Root cause. Walk the violation to its engine path: which
   decision/emission produces the illegal ordering, under what
   conditions (the shrunk seed's specific trajectory). Read the
   invariant's checker to confirm the violation is genuine (the checker
   could be wrong -- say so if so; that changes the fix options).
4. Blast estimate. State whether the buggy path is reachable at the 35
   oracle roots' seeds (run `check/check-all` over one or two roots'
   actual outputs if cheap, or argue structurally) -- this determines
   whether a fix can hold oracle identity or must be a
   DECLARED-ORACLE-CHANGE.
5. Fix options (lettered, with recommendation): e.g. (a) engine fix at
   the diagnosed path; (b) checker fix if the invariant is
   mis-specified; each with its oracle consequence stated. STOP HERE --
   the fix is a separate ruled session.

### Step 3 -- Close (commit 2)

Self-archive at close-phase START; ADR-0122 (full diagnosis evidence);
indices 119 -> 120; Done line (diagnosis landed; fix awaiting ruling);
session record. Message: `docs: session record and prompt archive --
positive-seed diagnosis (ADR-0122)`

### Oracle bracket

Pure identity, all 35 roots (records only; the diagnosis runs write
scratch, commit nothing). `bin/regression-oracle f483ab7 <final>`;
non-identity -> STOP. Gates: standing; ASCII x2; gitleaks; CI confirm
or disclose. If the defspec fails during this session's own `make
test` runs at ANY seed: record seed + shrunk value in the ADR as
additional evidence; one re-run to complete the gate is licensed HERE
ONLY because the finding is already chartered and this session exists
to diagnose it.

### Fences

Touch ONLY: `notes/adr/0121-*.md` (the erratum), registers, prompts,
session-records, `notes/adr/0122-*.md`, `notes/ADRs.md`, `notes/adr/
README.md`; companions by rule. ZERO src, ZERO test-code commits, ZERO
docs/manual. Outside -> STOP.

STOP-AND-REPORT on: non-repro at the pinned seed; a checker bug
changing the finding's nature mid-diagnosis (report, don't choose);
oracle non-identity; anything not pre-decided.

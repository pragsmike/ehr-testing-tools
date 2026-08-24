# Session prompt — diagnose the suite-time doubling (Q2), measurement session

## Context

`make test` ran 14m03s–14m48s in tracked records through 2026-08-21. Every run
since 2026-08-22 sits at 26m39s–31m04s (five runs, two sessions), including
baselines against UNMODIFIED HEAD 7a3ffd8 — a commit whose own era recorded
~14.5min. Test count moved only 4,100→4,142 across the boundary; the coverage
session's final run was FASTER than its baseline with more tests, so the noise
band is ~±1min and the doubling is real. One locating fact: the 08-23 baseline
log sat unmodified for 11+ minutes with `vendored-veteran-ptsd-test` as its
last line — roughly 40% of the run. A JDK change (Temurin 17→21) occurred on
penny at some point in this era; whether it coincides with the boundary is
unestablished.

This is a DIAGNOSIS session: no src, test, or vendored-module changes. Output
is a mechanism, on the record, with evidence — plus at most one licensed
environment remedy (fence F3).

## Read first

1. .agents/session-records/2026-08-20-*.md and 2026-08-21-*.md — the last-fast
   runs' recorded figures and their commits
2. The two 2026-08-23 session records — the slow runs' figures
3. Makefile + bin/ — how the suite is invoked, where JAVA_HOME/JDK is chosen

## Steps

1. CHEAP DISCRIMINATOR FIRST: `gh run list` — CI durations for the test
   workflow across the 08-20→08-23 window. GH runners are a fixed environment:
   if CI wall-time is flat across the boundary while penny doubled, the cause
   is penny-environmental and step 4a is the branch; if CI doubled too, it is
   repo content and 4b is the branch. Record run IDs and durations either way.
   If rate-limited or ambiguous, proceed — steps 2–3 decide independently.

2. Environment record, before any run: `java -version`, JAVA_HOME resolution
   per the Makefile, `ls /usr/lib/jvm`, apt/dpkg log dates for temurin
   packages, repo filesystem location (ext4 vs /mnt/c 9P), .wslconfig memory
   cap, free RAM, and whether Windows Defender real-time scanning covers the
   WSL distro. All recorded verbatim in the session record.

3. Two profiled runs, identical invocation, log lines timestamped (pipe
   through `awk '{print strftime("%T"), $0}'` or `ts`):
   a. Full suite at HEAD.
   b. Full suite at the newest commit with a RECORDED ~14.5min figure
      (identify from the 08-20/21 records; checkout in a worktree, do not
      move main).
   Compute per-namespace wall deltas from both logs. The decisive comparison:
   uniform ~2× across namespaces (environmental) vs concentrated in specific
   namespaces (content). Cross-check against step 1's verdict; if they
   disagree, that disagreement is itself the finding — report both.

4a. ENVIRONMENTAL branch: order probes by cost — if Temurin 17 is still
    installed, one suite run (or the top-3-slowest-namespaces subset once the
    profile identifies them) under JDK 17; then filesystem location; then
    Defender exclusion; then .wslconfig. Stop at the first probe that
    restores ~14.5min-proportional time. F3 license applies to the remedy.

4b. CONTENT branch: bisect the window between step 3b's commit and HEAD,
    using the top slow namespace's wall time as the probe (minutes per step,
    not full 28min suites). Identify the commit and READ the mechanism in it
    before naming it. Do not revert anything — report.

5. Session record: mechanism, evidence, all timings, environment record,
   CI comparison. Roadmap: close/update the Q2 row per outcome; if 4b names
   a commit, add a remediation row for author ruling rather than fixing.
   ADR only if F3's remedy was applied (document knob, before/after, and
   revert path); otherwise findings live in the record. Close commit per
   standing structure. Local commits only; no push, no tag.

## Fences

- F1: no changes to src, tests, vendored modules, invariants, or oracles.
- F2: worktree for the old-commit run; main never moves.
- F3 (author-licensed): if environmental and the remedy is a reversible
  config change OUTSIDE the tree, apply it, prove with one timed rerun,
  disclose in record + ADR. Anything inside the tree: report, don't touch.
- F4: bisect steps use the namespace-subset probe, not full suites — budget
  the whole session's suite-equivalents at ~5.
- F5: findings vs escalations — a probe contradicting this prompt's Context
  (e.g., the 14.5min records turn out to be a different invocation) is a
  FINDING: report what the records actually say and continue on the
  corrected baseline.

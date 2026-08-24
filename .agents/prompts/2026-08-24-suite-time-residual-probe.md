# Session prompt — suite-time residual probe, post-reboot measurement micro-session

> Archived verbatim by the session it drove
> ([R-A close-out](../skills/build-session/SKILL.md)). Record:
> [`.agents/session-records/2026-08-24-suite-time-residual-probe.md`](../session-records/2026-08-24-suite-time-residual-probe.md).
> The prompt was authored anticipating a 2026-08-25 run; the session ran
> 2026-08-24. Its "expect Temurin 21" in step 0 is a slip — penny's default
> is Ubuntu OpenJDK 21, as ADR-0167's own environment block records. Both
> noted in the record's Deviations, not edited here.

## Context

ADR-0167 (session record 2026-08-24) diagnosed the suite doubling: an orphaned
wslhost.exe consuming half of penny continuously. Killing it recovered
27m09s → 19m05s, confirmed three times to 0.5%. A stable 1.32× residual vs the
14m03s–14m48s era remained unexplained and was rowed as
`roadmap.md#suite-time-residual` (PRIORITY 2), with an eliminated list: JDK
(falsified twice), filesystem (ext4), Defender (not running), .wslconfig
(absent), repo content (CI flat 525–555s across the window).

penny has since been REBOOTED. This session runs one clean measurement to
discriminate the residual's class:
- ~14–15min → the residual was more lurking process state; reboot cured it.
- ~19min   → the residual survives reboot; it is structural.
- else     → new data point.

This is a measurement micro-session: one run, one row disposition, one commit.

## Read first

1. .agents/session-records/2026-08-24-*.md — the measurement conventions
   (clean unpiped invocation, poly "Execution time" as the figure of record)
   and the eliminated list
2. roadmap.md#suite-time-residual

## Steps

0. Health record before the run: repo at 6e57da1 (or descendant), tree clean;
   `java -version` (expect Temurin 21, the default); load average near zero;
   NO wslhost/orphan consuming CPU (`top -bn1` head recorded); interop alive
   (`powershell.exe -c "echo ok"` succeeds — record if not); uptime confirming
   the reboot. All verbatim into the session record.

1. One clean unpiped `make test` at HEAD — the exact invocation of record, no
   pty wrapper, no pipe, no concurrent work on the machine. Record MAKE_EXIT,
   wall, poly Execution time, namespace-run/test/assertion counts, reconciled
   against the 08-24 record's 370/4,142/18,450.
   INVARIANT: MAKE_EXIT=0 and counts reconcile. Nonzero exit or count drift is
   a FINDING (this tree ran green three times on 08-24) — stop and report.

2. Disposition by measured poly time:
   a. ≤ ~15min30s: CLOSE the row — mechanism confirmed as process-state
      contention cured by reboot; note the class remains recurrable and the
      record's discriminator sequence (CI-flat check, per-namespace
      uniformity) is the re-probe if a future baseline drifts.
   b. ~18–20min: SHARPEN the row — add "any process contention (reboot)" to
      the eliminated list; remaining suspects: WSL memory ballooning, host
      power plan, thermal. No probing beyond the one run — rule-worthy later.
   c. else: record the number, update the row with the observation, no
      speculation.

3. Close per standing structure: session record (health record, figure, era
   table 14.5/19/28.5/today, disposition), row edit, regenerated indexes if
   doc gates require, prompt self-archive, ONE commit:
   `docs: suite-time residual probe post-reboot -- <time>, row <closed|
   sharpened> (session record 2026-08-25)`
   No ADR unless the result contradicts ADR-0167's mechanism itself (that
   would be a FINDING first). Local commit only; no push, no tag.

## Fences

- F1: no src/test/module/oracle changes. Measurement only.
- F2: one suite run total. If the result is ambiguous (e.g. 16–18min), record
  it under 2c and stop — do not iterate toward a cleaner number.
- F3: findings vs escalations — if the health record shows the machine NOT
  quiet (load, a new orphan), fix the environment condition, disclose, and
  run once quiet; that is mechanical, not an escalation.

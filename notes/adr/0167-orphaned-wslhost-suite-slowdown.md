## ADR-0167 — the suite-time doubling is an orphaned `wslhost.exe`, and the machine is not the tree

**Status:** Accepted (author-directed diagnosis session, F3 remedy applied
with author execution), 2026-08-24.

### Context

Five tracked `make test` runs across two sessions on 2026-08-23 sat at
26m39s–31m04s where every run through 2026-08-21 had sat at 14m03s–14m48s,
including baselines against **unmodified** HEAD `7a3ffd84` — a commit whose
own era recorded 14m22s. `2026-08-23-unpaired-end-step-and-citation-scope.md`
recorded the observation and explicitly declined to diagnose it
("Cause not determined this session"). This ADR is that diagnosis.

The prompt's own Context proposed a JDK change (Temurin 17→21) as a
candidate and offered one locating fact: an 08-23 baseline log sitting
unmodified for eleven minutes with `vendored-veteran-ptsd-test` as its last
line. Both are addressed below, and **both turn out to be wrong** in the
sense the Context intended them.

### The mechanism

An **orphaned `wslhost.exe`, PID 116424**, started 2026-08-21 05:48:43,
whose parent process (134708) no longer exists, running **six threads pegged
at 99% of a core each**. Measured twice, seventy minutes apart, at 655% of
one core; a third 20-second sample put it at 598%. penny is an i7-10750H:
**6 physical cores / 12 logical**. Six spinning threads is one hyperthread on
every physical core — literally half the machine, taken continuously, by a
process with no parent and no work to do. It had accrued **247,153
CPU-seconds (68.7 CPU-hours)** by the time it was killed.

A whole-machine 20-second sample ranked it first by a factor of twenty-seven:

    wslhost 116424   598%
    claude    8412    22%
    claude  135732    19%
    (nothing else above 1% of a core)

### Why this is the tree's problem to record and not the tree's fault

The cheap discriminator ran first, exactly as the prompt ordered. GitHub's
runners are a fixed environment, so CI is a control for repo content. The
`test` workflow's own `poly test :all skip:integration` step, per-step from
the Actions API:

| date | sha | step wall |
| --- | --- | --- |
| 2026-08-20 | `92d23bcd` | 525s |
| 2026-08-20 | `8c534750` | 532s |
| 2026-08-21 | `7a3ffd84` | 539s |
| 2026-08-23 | `68af03b4` | 555s |
| 2026-08-24 | `7c1dfa53` (HEAD) | 535s |

Flat across the boundary, ±3%. Whole-workflow wall is flat too (625–701s over
08-17→08-24), and the `Integration` lane likewise (683–763s). **Repo content
is exonerated by a control the repo does not own.**

The per-namespace profile agrees independently. Two profiled runs with an
identical invocation, one with the runaway alive and one after it died,
both aborting at the identical point, give a **uniform** ratio rather than a
concentrated one — the prompt's stated environmental signature:

| namespace | contended | uncontended | ratio |
| --- | --- | --- | --- |
| `vendored-veteran-ptsd-test` | 608s | 489s | 1.24 |
| `mutate-stdout-stdin-loopback-test` | 28s | 20s | 1.40 |
| `vendored-veteran-substance-abuse-treatment-test` | 25s | 21s | 1.19 |
| `stdin-intake-real-pipe-test` | 23s | 19s | 1.21 |
| `v2-replay-test` | 13s | 11s | 1.18 |
| `vendored-metabolic-syndrome-care-test` | 13s | 9s | 1.44 |

The two discriminators do not disagree, so there is no disagreement to report
under the prompt's own cross-check clause.

### The knob, the before/after, and the revert path

**Knob.** `Stop-Process -Id 116424 -Force`, Windows-side, outside the tree.
Nothing in the repo, in `~/.clojure`, in `/etc`, or in any dotfile was
touched. Executed by the author, 2026-08-24, after the harness's own
permission classifier declined the call from the session — disclosed rather
than worked around.

**Before/after**, `make test` unpiped with `MAKE_EXIT` captured, the exact
convention every tracked figure in this repo uses:

| run | JDK | runaway | wall | poly `Execution time` | `MAKE_EXIT` |
| --- | --- | --- | --- | --- | --- |
| 2026-08-23 coverage close (recorded) | 21 | alive | 1,662s | 26m41s | 0 |
| 2026-08-23 coverage baseline (recorded) | 21 | alive | 1,702s | 27m09s | 0 |
| this session, run 3 | 21 | **killed** | **1,194s (19m54s)** | **19m05s** | 0 |
| this session, run 4 | 21 | killed | 1,191s (19m51s) | 18m59s | 0 |
| this session, run 5 | Temurin 17 | killed | 1,233s (20m33s) | 19m38s | 0 |

Runs 3 and 4 are the same tree twice: **reproducible to 0.5%**. Both carry
370 namespace-runs, the same count `2026-08-23-generator-side-coverage-and-
care-plan-end-invariant.md` closed on. The remedy is worth **1.42×** against
the coverage baseline.

**Revert path.** None is needed and none is possible: the process is gone and
was an orphan. WSL respawns interop hosts on demand. If a future runaway
appears, the diagnostic is `Get-Process -Name wslhost`, then per-thread
`TotalProcessorTime` deltas over a 20-second sample — a healthy interop host
carries 1–4 threads and no measurable CPU; this one carried 22 threads and
six spinners.

**Disclosed cost of the remedy.** The killed process was servicing this WSL
session's Windows-interop bridge. `powershell.exe` from WSL now returns
`UtilAcceptVsock:273: accept4 failed 110` and does not recover within the
session; `/mnt/c` filesystem access is unaffected, and so is everything
Linux-side. Cleared by `wsl.exe --shutdown` or a fresh session. The trade was
made knowingly: the session's shell ancestry (`bash ← ccd-cli ← server ←
Relay ← init`) was verified detached from every `wslhost` first, so the kill
could not take the session down.

### Three findings the prompt's Context did not anticipate (F5)

**1. The JDK hypothesis is dead twice over.** `openjdk-21` was installed and
became the `update-alternatives` default at **2026-07-28 08:32:41** — three
weeks *before* the boundary. `/etc/alternatives/java` carries that mtime, and
apt's history records no install, upgrade or removal of anything since. Every
14-minute run in the record already ran on JDK 21. Killed a second time by
direct measurement: run 5 above, the full suite on Temurin 17.0.20, came in
**slower** (19m38s vs 18m59s), not faster.

**2. `vendored-veteran-ptsd-test` is not stalling.** The eleven-minute
unmodified log the 08-23 record found is not a hang; it is the suite's
dominant namespace doing real work. It is 489s of penny's 687s conformance
project, and CI's own timestamped log shows it costing **157.7s and 172.8s**
across its two project runs — ~330s of CI's 535s step, 62%. It is the
heaviest namespace *everywhere*, by design (3 seeds × 300 patients ×
36,500-day horizon, twice over). Nothing is wrong with it.

**3. A 1.32× residual is real, stable, and NOT explained here.** 19m02s
(mean of runs 3–4) against the 14m22s of 2026-08-21 leaves a third of the
regression unaccounted for. It is not a settling transient — runs 3 and 4
agree to 6 seconds. It is not JDK, content, filesystem (ext4 `/dev/sdd`, not
`/mnt/c`), Windows Defender (`AMRunningMode: Not running` — it is not running
at all, Malwarebytes having replaced it), a `.wslconfig` cap (no such file
exists, nor `/etc/wsl.conf`), or memory pressure (15Gi total, 11Gi free, 0B
swap used). Every probe the prompt's 4a list names has been executed or ruled
out on evidence. The residual is rowed, not guessed at.

One dated coincidence is recorded for whoever picks that row up, explicitly
as a coincidence and not a mechanism: the oldest resident agent server on
penny started **2026-08-22 06:27:56**, the boundary date, and nine long-lived
agent processes now run continuously. Their measured draw is ~18% of ONE
logical CPU in total, which is far too small to produce a 32% wall increase
on a workload using ~2.4 cores — so it is named to be eliminated, not
believed.

### Consequences

- `~19m` is penny's honest `make test` figure at HEAD, replacing both the
  stale `~14.5min` and the `~28.5min` the 08-23 records carry.
- `reference_make_test_runtime` and any successor prompt quoting 28.5min are
  superseded by this ADR's table.
- The residual gets a roadmap row rather than a story.

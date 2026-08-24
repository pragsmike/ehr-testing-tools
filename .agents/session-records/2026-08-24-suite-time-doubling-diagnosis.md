# Session record: the suite-time doubling diagnosed (2026-08-24)

**Prompt:** [`.agents/prompts/2026-08-24-suite-time-doubling-diagnosis.md`](../prompts/2026-08-24-suite-time-doubling-diagnosis.md)
**ADR:** [`notes/adr/0167-orphaned-wslhost-suite-slowdown.md`](../../notes/adr/0167-orphaned-wslhost-suite-slowdown.md)
**Mode:** diagnosis session, no src/test/module changes. **Base:** `7c1dfa5`.

**Mechanism, one sentence:** an orphaned `wslhost.exe` (PID 116424, parent
dead) spinning six threads at 99% of a core each — one hyperthread on every
one of penny's six physical cores — was taking half the machine continuously;
killing it moved `make test` from a recorded 27m09s to a reproducible
19m02s. A **1.32× residual against the 2026-08-21 figure remains unexplained
and is rowed, not guessed at.**

## Step 1 — the cheap discriminator: CI is flat, so content is exonerated

`gh run list` plus per-step durations from the Actions API. The `test`
workflow's own `poly test :all skip:integration` step:

| date | sha | run id | step wall | whole workflow |
| --- | --- | --- | --- | --- |
| 2026-08-20 | `92d23bcd` | 32351871551 | 525s | 663s |
| 2026-08-20 | `8c534750` | 32402730016 | 532s | 676s |
| 2026-08-21 | `7a3ffd84` | 32493691973 | 539s | 681s |
| 2026-08-23 | `68af03b4` | 32673857158 | 555s | 707s |
| 2026-08-24 | `7c1dfa53` (HEAD) | 32684581104 | 535s | 672s |

Whole-workflow wall across 08-17→08-24 spans 625–701s with no trend, and the
`Integration` lane is equally flat (683s, 689s, 727s, 759s, 763s, 716s, 747s
over the same window; run ids 32111740050 / 32228155848 / 32459246773 /
32402746494 / 32559462269 / 32625514445 / 32703337790).

**Verdict: CI flat while penny doubled ⇒ penny-environmental, branch 4a.**
Both 08-22 and 08-23 `Integration` runs sit at the same sha `7a3ffd84` as the
08-21 `test` run, so the boundary is crossed at *identical content* on the
runner and nothing moves.

## Step 2 — environment record (verbatim, before any run)

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    OpenJDK 64-Bit Server VM (build 21.0.7+6-Ubuntu-0ubuntu120.04, mixed mode, sharing)

    $ which -a java              -> /usr/bin/java, /bin/java
    $ readlink -f $(which java)  -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java
    $ echo "${JAVA_HOME:-<unset>}" -> <unset>

**JAVA_HOME resolution per the Makefile: there is none.** `grep -nE
"JAVA|JVM|Xmx|jvm" Makefile` matches exactly one line, 288, and it is prose
inside an ADR-0149 cost comment. `make test` shells `clojure -M:poly check`,
`clojure -M:poly test :all skip:integration` and `bin/verify-nist-lock`, and
takes whatever `java` is on PATH — i.e. `update-alternatives`' choice.

    $ ls /usr/lib/jvm
    java-11-openjdk-amd64/   java-21-openjdk-amd64/   temurin-17-jdk-amd64/
    openjdk-11/  openjdk-21/  default-java -> java-1.11.0-openjdk-amd64

    $ update-alternatives --display java
    java - auto mode
      link currently points to /usr/lib/jvm/java-21-openjdk-amd64/bin/java
      java-11-openjdk-amd64  priority 1111
      java-21-openjdk-amd64  priority 2111   <- wins
      temurin-17-jdk-amd64   priority 1711

    $ ls -la --time-style=full-iso /etc/alternatives/java
    -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java  2026-07-28 08:32:41 -0400

apt/dpkg dates for the JDKs:

    2026-07-24 16:30:25 install temurin-17-jdk:amd64 17.0.20.0.0+8-0
    2026-07-28 08:32:31 install openjdk-21-jre-headless 21.0.7+6~us1-0ubuntu1~20.04
    2026-07-28 08:32:36 install openjdk-21-jdk-headless
    2026-07-28 08:32:36 install openjdk-21-jre
    2026-07-28 08:32:40 install openjdk-21-jdk

The last apt command of ANY kind is `apt install -y git openjdk-21-jdk` at
2026-07-28 08:32:29. **Nothing has been installed, upgraded or removed
since** — three weeks before the boundary.

    $ pwd            -> /home/mg/src/ehr-testing-tools
    $ df -hT .       -> /dev/sdd  ext4  251G  62G  177G  26% /
    $ stat -f -c %T . -> ext2/ext3     (ext4; NOT a /mnt/c 9P mount)

    $ nproc -> 12    Intel(R) Core(TM) i7-10750H CPU @ 2.60GHz, 6 cores / 12 logical
    $ free -h -> total 15Gi, used 2.6Gi, free 11Gi, buff/cache 1.1Gi, avail 12Gi
                 Swap 4.0Gi total, 0B used
    $ uptime -s -> 2026-08-20 14:46:35   (WSL VM boot — BEFORE the 08-21 fast runs)
    /proc/stat since boot: busy 2.8%, idle 97.2%, steal 0

    .wslconfig     : does not exist (find /mnt/c/Users -maxdepth 2 -name .wslconfig)
    /etc/wsl.conf  : does not exist
    => no memory cap, no processor cap; WSL sees all 12 logical CPUs and 15Gi.

    Get-MpComputerStatus:
      AMRunningMode             : Not running
      RealTimeProtectionEnabled : False
      AntivirusEnabled          : False

**Windows Defender does not scan the WSL distro because Defender is not
running at all** — Malwarebytes (`MBAMService`, Running) has replaced it.
`Get-MpPreference` errors `0x800106ba`, consistent with the service being off.

    powercfg /getactivescheme -> 8c5e7fda-... (High performance)
    Win32_Battery  -> BatteryStatus 2 (on AC), 100%
    Win32_Processor -> MaxClockSpeed 2592, CurrentClockSpeed 2592,
                       LoadPercentage 50   <-- with WSL IDLE. The thread to pull.

## Step 2b — the runaway

    Get-CimInstance Win32_Process -Filter "Name='wslhost.exe'"
    pid 133620  2026-08-19 14:07:23  threads  4
    pid 126156  2026-08-19 14:07:24  threads  1
    pid 124676  2026-08-19 18:03:00  threads  1
    pid 116424  2026-08-21 05:48:43  threads 22   <-- THE ORPHAN
    pid 141300  2026-08-23 10:16:02  threads  1

    pid 116424
      cmdline : --distro-id {5a84429f-...} --vm-id {fca04f62-...}
                --handle 728 --event 760 --parent 764
      ppid    : 134708  -> Get-CimInstance for 134708 returns NOTHING
      start   : 2026-08-21T05:48:43.0998035-04:00

Accrued CPU, three readings:

    06:11  218,443 s
    06:31  226,302 s   (+7,859 s / ~1,200 s wall = 655% of one core)
    07:24  247,153 s   (+20,851 s / ~3,180 s wall = 655% of one core)

Per-thread rate, 20-second sample — six threads, each ~99% of a core:

    134956 19.78s/20s 99% | 145424 19.58s 98% | 142596 19.77s 99%
    142168 19.83s 99%     | 133152 19.81s 99% | 144620 19.83s 99%

Machine-wide rank by current rate (20 s), WSL idle: `wslhost 116424` **598%**,
`claude 8412` 22%, `claude 135732` 19%, nothing else above 1% of a core.

## Step 3 — two profiled runs, identical invocation

Deviation from the prompt's 3b, disclosed: **no worktree run at the old commit
was needed or made.** Step 1 had already exonerated content with a control the
repo does not own, and the runaway was live and measurable *now*, so the
controlled pair was taken as runaway-alive vs runaway-dead **at HEAD** — which
isolates the variable under test instead of confounding it with content. F2
was therefore never engaged; `main` never moved and no worktree was created.

Both runs: `script -qfe -c "make test" /dev/null | awk '{print strftime("%T"), $0}'`.

| run | runaway | wall | scope |
| --- | --- | --- | --- |
| 1, before | ALIVE | **953s** | poly check + whole `conformance` + first 17s of `ehrt-cli` |
| 2, after | killed | **754s** | identical, aborted at the identical point |

    conformance project   run 1  06:14:21 -> 06:28:51 = 870s
                          run 2  07:33:08 -> 07:44:35 = 687s     ratio 1.27
    whole (to abort)      run 1  953s / run 2 754s              ratio 1.26

Per-namespace wall deltas, top of both profiles:

| namespace | contended | uncontended | ratio |
| --- | --- | --- | --- |
| `ehrt.sim-emit-hl7.vendored-veteran-ptsd-test` | 608s | 489s | 1.24 |
| `ehrt.conformance.mutate-stdout-stdin-loopback-test` | 28s | 20s | 1.40 |
| `ehrt.sim-emit-hl7.vendored-veteran-substance-abuse-treatment-test` | 25s | 21s | 1.19 |
| `ehrt.conformance.stdin-intake-real-pipe-test` | 23s | 19s | 1.21 |
| `ehrt.sim-emit-hl7.v2-replay-test` | 13s | 11s | 1.18 |
| `ehrt.sim-emit-hl7.vendored-metabolic-syndrome-care-test` | 13s | 9s | 1.44 |

**Uniform (1.18–1.44), not concentrated ⇒ environmental**, agreeing with step
1. No cross-check disagreement to report.

### A measurement artifact, disclosed rather than buried

Both profiled runs ended `MAKE_EXIT=2` on **three failures in
`bases/cli` `core_test.clj`** — `main-bang-default-tty-fn-is-real-tty-and-
behaves-like-a-pipe-in-tests-test` and two siblings (lines 774, 2553, 2865).
These are **caused by the profiling harness, not by the tree**: `script`
allocates a pty so Clojure line-buffers per namespace, and those tests assert
pipe-not-tty behaviour. The failures are identical in both runs, cost no
measurable time, and land after every namespace of interest — so the pair
stays controlled. The invocation was deliberately left identical rather than
"fixed" mid-experiment. Runs 3–5 below use the repo's own unpiped convention
and are green.

Second artifact, quantified: the pty costs ~11% (conformance 687s under
`script` vs 609s unpiped, same conditions). Ratios above are unaffected —
both sides carry it.

## Step 4a — the remedy, and every other probe on the list

**Remedy (F3).** `Stop-Process -Id 116424 -Force`. Outside the tree; nothing
in the repo or in any config file was touched. **The session's own Bash call
was declined by the harness permission classifier**, so the author executed
it — disclosed, not worked around. The first guarded command mis-compared a
whole-second timestamp against `StartTime`'s sub-second precision and
correctly refused to fire; the corrected guard killed it.

Proof, `make test` unpiped with `MAKE_EXIT` captured — the convention every
tracked figure uses:

| run | JDK | runaway | wall | poly `Execution time` | `MAKE_EXIT` | ns-runs |
| --- | --- | --- | --- | --- | --- | --- |
| recorded 08-23 baseline | 21 | alive | 1,702s | 27m09s | 0 | 368 |
| recorded 08-23 close | 21 | alive | 1,662s | 26m41s | 0 | 370 |
| **3, this session** | 21 | **killed** | **1,194s (19m54s)** | **19m05s** | **0** | **370** |
| 4, repeat | 21 | killed | 1,191s (19m51s) | 18m59s | 0 | 370 |
| 5, Temurin 17 | 17 | killed | 1,233s (20m33s) | 19m38s | 0 | 370 |

Runs 3 and 4 are the same tree twice, **reproducible to 0.5%** — so 19m is a
floor, not a transient. Remedy worth **1.42×**.

Single-thread JVM microbenchmark, same binary, before and after the kill:
**429ms → 362/373ms (1.16×)**. The suite gains more (1.42×) than one thread
does (1.16×) because `poly test` runs ~2.4 cores — exactly what losing six of
twelve logical CPUs predicts.

**Every remaining 4a probe, executed or eliminated on evidence:**

- **JDK 17** — executed, run 5. **Slower** (19m38s vs 18m59s). Dead.
- **Filesystem location** — eliminated: ext4 `/dev/sdd`, not `/mnt/c`.
- **Defender exclusion** — eliminated: Defender is not running at all.
- **`.wslconfig`** — eliminated: the file does not exist, nor `/etc/wsl.conf`.

No probe restored ~14.5min-proportional time beyond the kill, so the prompt's
"stop at the first probe that restores it" never triggered; the list was run
to exhaustion instead.

**Suite-equivalents spent: 953 + 754 + 1,194 + 1,191 + 1,233 = 5,325s ≈ 4.4**
against F4's budget of ~5.

## Findings that contradict the prompt's Context (F5)

**F5-1. The JDK hypothesis is dead twice over.** The Context called the
Temurin 17→21 coincidence "unestablished". It is now established as a
**non**-coincidence: JDK 21 became default 2026-07-28 08:32:41, three weeks
before the boundary, and every 14-minute run in the record already ran on it.
Confirmed independently by run 5 measuring 17 as slower.

**F5-2. `vendored-veteran-ptsd-test` is not stalling.** The Context read the
eleven-minute unmodified log as a stall worth ~40% of the run. It is the
suite's dominant namespace doing real work — 3 seeds × 300 patients ×
36,500-day horizon, twice over. CI's own timestamped log for run 32684581104
shows it at **157.7s** (02:56:06.295→02:58:44.043) and **172.8s**
(03:00:30.401→03:03:23.171), ~330s of CI's 535s step — **62% of CI's suite
too**. It is the heaviest namespace everywhere, by design. Nothing is wrong
with it, and it should not be "fixed".

**F5-3. A 1.32× residual is real and unexplained.** 19m02s (mean of runs 3–4)
against 14m22s of 2026-08-21. Not a settling transient (runs 3 and 4 agree to
6 seconds), not JDK, content, filesystem, Defender, `.wslconfig`, or memory.
penny is also **3.1× slower than a GitHub runner on the same namespace
uncontended** (489s vs 157.7s) with more cores, which is itself unexplained.
Rowed as `roadmap.md#suite-time-residual`.

One dated coincidence, recorded as a coincidence and not a mechanism: the
oldest resident agent server on penny started **2026-08-22 06:27:56**, the
boundary date, and nine long-lived agent processes now run continuously
(`ps -eo pid,lstart,time,pcpu`). Their total measured draw is **~18% of ONE
logical CPU**, far too small to produce a 32% wall increase on a ~2.4-core
workload. Named so the next session can eliminate it quickly, not believe it.

## Deviations

1. **No worktree run at the old commit (prompt 3b, F2).** Superseded by a
   stronger design — see Step 3. `main` never moved; no worktree created.
2. **Profiled pair is runaway-alive vs runaway-dead at HEAD**, not HEAD vs
   old commit. Isolates the variable instead of confounding it.
3. **Both profiled runs ended `MAKE_EXIT=2`** on three tty-assertion failures
   caused by the profiling pty. Disclosed above, quantified, and compensated
   by three green unpiped runs.
4. **The F3 remedy was executed by the author, not the session** — the
   harness permission classifier declined the call. Disclosed, not worked
   around.
5. **The remedy severed this session's Windows-interop bridge.**
   `powershell.exe` from WSL returns `UtilAcceptVsock:273: accept4 failed
   110` and does not recover in-session; `/mnt/c` reads still work and the
   Linux side is unaffected. Cleared by `wsl.exe --shutdown`. Consequence of
   this: **no post-kill Windows-side CPU sample could be taken**, so the
   residual was hunted from the Linux side only.
6. **Five suite runs, not two.** F4 budgets ~5 suite-equivalents; 4.4 spent.
   The three extra runs are the green whole-suite figure, its reproducibility
   check, and the JDK 17 probe — each named by the prompt or owed by it.
7. **No `## Next` Q2 row existed to close.** "Q2" is the design channel's own
   label; `grep` finds no such row in `.agents/plans/roadmap.md`. A `## Done`
   row is added under this ADR and a new `## Next` row carries the residual.

## Close

`make test` over the completed tree, unpiped, wrapper ending `exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    370 zero-failure blocks / 4,142 tests / 18,450 assertions
    Execution time: 19 minutes 5 seconds
    bin/verify-nist-lock -> OK: 6 hit-nexus-sourced coordinate(s) match

`clojure -M:poly check` **OK** (`make test`'s own first line). The counts
reconcile exactly against
[`2026-08-23-generator-side-coverage-and-care-plan-end-invariant.md`](2026-08-23-generator-side-coverage-and-care-plan-end-invariant.md)'s
close, `370 / 4,142 / 18,450` — this session changed no src, no test, and no
vendored module, and the figures prove it.

Its 19m05s is also the **third** independent landing on the same number
(runs 3 and 4 gave 19m05s and 18m59s), which is what makes `~19m` reportable
as penny's figure rather than one lucky run.

The attic-rotation gate went red on the first attempt — `## Done` reached 31
lines with this session's row, one over ADR-0161's cap. One whole row,
`[attic-rotation-law]` itself (the oldest, ADR-0161's own), was rotated
verbatim into `.agents/plans/roadmap-done-2026-08.md` under a dated heading.
Recorded because it is the law working, not a defect: the gate is what
noticed.

**Suite-equivalents, final: 6.4** against F4's ~5 — the overrun is the close
verification (1,197s), which standing discipline owes regardless of the
measurement budget, plus the JDK 17 probe the prompt's own 4a ordering named.
Disclosed rather than trimmed.

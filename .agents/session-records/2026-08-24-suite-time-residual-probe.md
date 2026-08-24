# Session record: the suite-time residual probe, post-reboot (2026-08-24)

**Prompt:** [`.agents/prompts/2026-08-24-suite-time-residual-probe.md`](../prompts/2026-08-24-suite-time-residual-probe.md)
**ADR:** none minted; [`notes/adr/0167-orphaned-wslhost-suite-slowdown.md`](../../notes/adr/0167-orphaned-wslhost-suite-slowdown.md) gains a dated amendment.
**Mode:** measurement micro-session, one run. **Base:** `6e57da1`, tree clean.

**Result, one sentence:** the 1.32× residual that survived the orphaned-
`wslhost` kill did **not** survive a reboot — one clean run on a
verified-quiet penny measures **13m59s**, `MAKE_EXIT=0`, counts reconciling
exactly, which is *inside* the pre-regression era and 23 seconds under the
2026-08-21 figure the residual was measured against. Disposition **2a**:
`roadmap.md#suite-time-residual` is CLOSED, class confirmed as host
process-state contention.

## Step 0 — the health record, verbatim, before the run

    $ date -Is       -> 2026-08-24T15:51:39-04:00
    $ uptime         -> 15:51:39 up 40 min, 0 users, load average 0.04, 0.05, 0.01
    $ uptime -s      -> 2026-08-24 15:10:46      (WSL VM boot -- REBOOT CONFIRMED)
    /proc/stat since boot: busy 0.18%, idle 99.82%, steal 0
    $ free -h        -> 15Gi total, 614Mi used, 14Gi free, 0B swap used
    $ df -hT .       -> /dev/sdd ext4 251G, 26% used
    $ nproc          -> 12

    $ git log --oneline -1  -> 6e57da1 (as the prompt specifies)
    $ git status --porcelain -> empty

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    OpenJDK 64-Bit Server VM (build 21.0.7+6-Ubuntu-0ubuntu120.04, mixed mode, sharing)
    $ readlink -f $(which java) -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java

    $ powershell.exe -NoProfile -c "echo ok"  -> ok, exit 0   (interop ALIVE)

    Windows side, immediately pre-run:
      LoadPercentage x3        -> 3, 3, 1
      wslhost.exe              -> 5 procs, all 15:10:49 / 15:13:06, thread counts
                                  4/1/1/5/1, no orphan, no dead parent
      top accrued CPU          -> agent servers and msedge only
      powercfg /getactivescheme -> High performance
      Win32_Battery            -> BatteryStatus 2 (AC), 100%
      Win32_Processor          -> Max 2592 / Current 2592 MHz

**JDK deviation, disclosed and harmless.** The prompt says "expect Temurin
21, the default". There is no Temurin 21 on penny — `update-alternatives`
resolves `java` to Ubuntu OpenJDK 21, exactly as ADR-0167's own environment
block records, and Temurin on this machine is 17 (ADR-0167 run 5, measured
slower). The run is therefore on the **identical JDK** to every figure it is
compared against, which is what the comparison requires. Prompt-side slip,
nothing fixed.

## Step 0b — the machine was NOT quiet, and the run waited (F3)

The first host-side sample, taken before anything was launched:

| sample | Overwatch.exe | host `LoadPercentage` |
| --- | --- | --- |
| 20s, 15:42 | **182.6%** of a core | 53 |
| 30s, 15:45 | **247.0%** of a core | 65 / 71 / 63 |

`Overwatch.exe` PID 3200, **91 threads, 10.8 GB working set**, created
15:28:13 — eighteen minutes after the reboot, i.e. launched deliberately and
running. Battle.net and `OmenCommandCenterBackground` alongside it.

F2 permits **one** suite run; a run taken against 2.5 contended cores would
have spent the session on a number that answers nothing. Per F3 this is an
environment condition to fix rather than an escalation — but force-closing a
live game is not a mechanical fix and is not the session's call, so the
author was asked and chose to close it. A watcher polled the host every 30s
and fired when Overwatch was gone and `LoadPercentage` held ≤12 across three
consecutive samples:

    QUIET: Overwatch gone, host LoadPercentage=3 for 3 consecutive samples
           (Battle.net procs=0) at 2026-08-24T15:51:14-04:00

**Elapsed waiting: ~9 minutes. Zero suite runs spent.** Recorded because the
health record is the only reason the figure below is worth anything — see
"What this session actually establishes".

## Step 1 — the one run

Invocation of record, unpiped, no pty, no concurrent work, `MAKE_EXIT`
captured, wrapper ending `exit "$MAKE_EXIT"`
([`state.md`](../state.md) "A gate run captures its exit code explicitly";
`rulings.md#R-full-suite-before-push`):

    make test > run.log 2>&1
    MAKE_EXIT=$?

    START = 2026-08-24T15:52:04-04:00
    END   = 2026-08-24T16:06:42-04:00
    WALL  = 878s (14m38s)

    MAKE_EXIT=0
    clojure -M:poly check                    -> OK
    Execution time: 13 minutes 59 seconds
    bin/verify-nist-lock -> OK: 6 hit-nexus-sourced coordinate(s) match

Counts, and their reconciliation against the 08-24 close (`370 / 4,142 /
18,450`):

| metric | measured | 2026-08-24 record | reconciles |
| --- | --- | --- | --- |
| namespace-run blocks (`Ran N tests containing M assertions`) | **370** | 370 | exact |
| tests | **4,142** | 4,142 | exact |
| assertions | **18,450** | 18,450 | exact |
| `Test results:` blocks, all zero-failure | 370 / 370 | — | — |
| `grep -cE '^(FAIL\|ERROR) in'` | **0** | 0 | exact |

The invariant holds: `MAKE_EXIT=0` and the counts reconcile exactly. No
finding. This session changed no src, no test, no module, no oracle before
the run (F1), and the identical triple is what proves it.

Post-run health, confirming the window stayed clean: Overwatch/Battle.net
still absent (count 0), host `LoadPercentage` 3, `wslhost` thread counts
unchanged at 4/1/1/4/1, and `msedge` accrued **0.03 CPU-seconds** across the
whole 16-minute window. Nothing contended with the run.

## Step 2 — disposition 2a, the row CLOSES

13m59s is under the prompt's ≤15m30s threshold by 91 seconds, and it is not
merely "recovered" — it is **faster than the era it is being restored to**.

| era | date | poly `Execution time` | condition |
| --- | --- | --- | --- |
| pre-regression | through 2026-08-21 | 14m03s – 14m48s (14m22s on 08-21) | — |
| doubled | 2026-08-23 / 08-24 | 26m41s – 27m09s (five runs, to 31m04s) | orphan alive |
| post-kill | 2026-08-24 | 18m59s / 19m05s (Temurin 17: 19m38s) | orphan killed, no reboot |
| **post-reboot** | **2026-08-24** | **13m59s** | **rebooted, host verified quiet** |

Ratios: the kill bought **1.42×**; the reboot buys a further **1.36×**
(19m02s → 13m59s), and 1.42 × 1.36 = 1.93 — the whole doubling, accounted
for, with nothing left over. Against 2026-08-21's 14m22s the new figure is
**0.97×**: the residual is not merely explained, it is gone.

**Mechanism, as far as this run licenses:** host process-state contention,
cured by reboot. What specifically was resident beyond the killed
`wslhost` — a second orphan, WSL memory ballooning that a fresh VM reset, an
accumulated host-side cost the kill did not reach — is **not knowable after
a reboot** and is not claimed. The class is confirmed; the individual is not
identified, and the row closes on the class because the class is what the
row asked about.

**The class is recurrable.** The re-probe, if a future baseline drifts, is
ADR-0167's own discriminator sequence, in order: (i) is CI flat over the same
window? — if yes, content is exonerated and it is penny; (ii) is the
per-namespace profile uniform rather than concentrated? — if yes,
environmental rather than a specific test; (iii) sample the **Windows** side,
not just Linux. Step (iii) is the one this session adds teeth to.

## What this session actually establishes, and what it does not

**Establishes.** `~14m` is penny's `make test` figure at HEAD on a quiet
machine, measured once, green, counts exact. `roadmap.md#suite-time-residual`
has no remaining question.

**Does not establish.** One run, not three — F2 caps it, and the prompt's 2a
does not ask for reproducibility because a figure *inside* the prior era
needs no defence against being a lucky outlier the way a novel figure would.
Nothing here re-measures penny-vs-runner (ADR-0167's 3.1× on
`vendored-veteran-ptsd-test`); that gap is untouched and unexplained, but it
is no longer part of a regression — it is simply penny's speed, and it was
present during the 14m era too.

**The finding worth more than the number.** ADR-0167's deviation 5 records
that the orphan kill severed interop, so **no post-kill Windows-side CPU
sample was ever taken** — the 19m residual was hunted from the Linux side
only. This session, with interop restored, found on its very first host-side
sample that a foreground application was eating 2.5 cores while every
Linux-side signal (`uptime` 0.12, `/proc/stat` 0.1% busy, `top` all idle)
said the machine was empty. Linux-side quiet is **not** evidence of a quiet
machine under WSL2. Whether a game was up during the 08-24 19m runs is
unknown and deliberately not probed — but it is now clear the 19m figure was
taken without the one instrument that could have said. Folded into the ADR
amendment as a standing warning, not as a retraction of its mechanism.

## Deviations

1. **A blocking question was asked, and the session waited ~9 minutes.**
   The prompt's F3 calls a non-quiet machine mechanical to fix. Closing a
   user's running game is not mechanical, so the author chose. Zero suite
   runs were spent on the contended machine.
2. **The prompt anticipates a `2026-08-25` record and commit.** The actual
   date is **2026-08-24** — the reboot and this probe fall on the same
   calendar day as ADR-0167 itself. Record, row and commit all carry 08-24.
3. **A dated amendment to ADR-0167 was added, which the prompt's step 3 does
   not name.** Not a new ADR and not a contradiction of the mechanism (the
   prompt's FINDING trigger is untripped): ADR-0167's Consequences assert
   "`~19m` is penny's honest `make test` figure at HEAD", which this run
   falsifies within the day. Leaving it standing would seed exactly the
   stale-claim class the errata discipline exists to prevent. The amendment
   retracts that one bullet and nothing else, explicitly.
4. **`## Done` now sits at exactly 30 lines, the cap.** ADR-0161's law
   rotates only when the section *exceeds* 30, so nothing was rotated —
   rotating early would be its own deviation. Flagged so the next close
   knows it has **zero** headroom and must rotate before adding a row.
5. **No `## Next` row replaces the closed one.** Nothing was learned that
   needs a session; the recurrable class is carried by the ADR amendment's
   warning and the discriminator sequence, which is where a re-probe would
   look anyway.

## Close

    MAKE_EXIT=0
    370 zero-failure blocks / 4,142 tests / 18,450 assertions
    Execution time: 13 minutes 59 seconds
    clojure -M:poly check -> OK
    bin/verify-nist-lock  -> OK: 6 hit-nexus-sourced coordinate(s) match

**Suite runs spent: 1**, exactly the prompt's budget. The measurement run is
also the close verification — this session changed only documentation after
it, and the doc gates are re-run as part of the docsgen freshness check
below rather than by a second suite.

Documentation landed: this record, its archived prompt, the ADR-0167
amendment, the roadmap row moved `## Next` → `## Done`, and the two
generated INDEXes regenerated by `make state-derived`. One commit, local
only; no push, no tag.

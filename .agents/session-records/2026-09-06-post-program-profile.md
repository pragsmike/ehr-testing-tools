# 2026-09-06 — Post-program profile: CPU, allocation and the live set

A MEASUREMENT session, not a build one. ADR-0180's four sites are landed and
at 0.00–0.01% of generate; this session asks what is left, at 7,500 and 22,500
arrivals, and ranks it. Ceremony mode: R30 (commit and push at each
checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-06-post-program-profile.md`](../prompts/2026-09-06-post-program-profile.md).
No ADR was written and none was owed: no engine code changed, and every
conclusion is a recommendation for the design channel.

Rulings in force: **R-measure-first** (no engine change this session),
**R-sentinel** (a background job is done when its own sentinel file says so;
notifications are not evidence; no sleep loops), **R-commit-cells**,
**R-edit**, **R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green (`63d5ee64`, `1d1111d7`, `08a41efd`, `228e2c62`, `eaa5fdd6`), edit root
`/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode` true,
`core.ignorecase` unset, working tree clean including untracked files, local
HEAD matching `origin/main` at `63d5ee64`. HEAD not tagged `stable-*`,
disclosed by the script and left alone.

## 1. What landed

Three commits, no engine code in any of them.

| commit | what |
|---|---|
| `10414cca` | the aggregator's two new tables, and four CPU recordings |
| `60359c46` | the allocation tables, the `-Xmx8g` GC run, and the EdnReader correction |
| `385d7f0d` | the dated `measurements.md` section and the roadmap pointer |

`profile-cell.sh` gained a project-frame table carrying SELF and INCLUSIVE
share side by side (it was self-only), an `apply-events` concern breakdown,
and an `--alloc` mode over `jdk.ObjectAllocationSample`.
`gc-summarize.py` is new: one `-Xlog:gc*` log to peak pre-collection
occupancy, the post-collection floor, and a decimated committable series.

## 2. The measurement

Every figure below is in `measurements.md`'s
`Post-program profile, 2026-09-06` section with its own table; this section
names only the sentinel each came from.

| artifact | sentinel |
|---|---|
| four CPU recordings | `/tmp/prof-run.done` = `0 0` |
| CPU re-aggregations (FOUR passes, see §4) | `/tmp/agg-run.done`, `/tmp/agg2.done`, `/tmp/agg3.done`, `/tmp/agg4.done`, all zeros |
| `-Xmx8g` GC run | `/tmp/gc-run.done` = `7d105743…78e0a` |
| `make test` | `/tmp/mt.done` |

**The headline.** At 22,500 the four ADR-0180 sites are 0.00–0.01% of generate
and `decide` is 72.28%. Two frames hold most of it, and neither is on
ADR-0180's census: `decide :merge` (`decide.clj:1449`) at 34.06% inclusive and
`decide :bed-swap` (`decide.clj:1414`) at 31.38%. Both scan the whole
`:patients` map per event; `:merge` additionally answers `already-merged?`
with a `some` over the ENTIRE `:ground-truth` log, which is site 4's own
defect under another method's name. The same two frames are 51% of all
allocation.

**The peak heap is not what the roadmap says it is.** One `-Xmx8g` run of
`a22500-nopersons`, byte-identical to the committed digest, showed peak RSS
RISING to 4,780 MB from 3,599 MB when the budget rose — so that reading
measures the budget, not the need. The post-collection floor reaches 432 MB by
the end of the simulation loop and 792 MB overall. The 3,863 MB peak was dated
off the profile's own sample timestamps to the in-run self-check
(`sim/run.clj:783`'s `check/check-all`): `decide`'s last sample is at 364 s,
`sim-check`'s samples are ~0 before 360 s and run to 461 s, the renderer runs
461–471 s, and the heap balloons at 323 s and collapses at 396 s of a 402 s
run. So the peak is a transient the self-check holds, and `R-order`'s site 5
addresses it.

**Live-set composition** came from the 112 `jdk.OldObjectSample` events the
same recordings already carried — a bounded sample that ranks rather than
measures. 35 of 112 survivors were allocated at `fold.clj:924`, the
`:warm-up-mark` decoration that mints the event map landing in
`:ground-truth`. No ADR-0180 index frame appears among the survivors at all.

## 3. Judgment calls

**No re-record for allocation, against the step's own wording.** The prompt
said "re-record generate at both cells with `jdk.ObjectAllocationSample`
enabled" and then told me to confirm the channel's expectation first.
`lib/jfr/profile.jfc` enables it at `300/s` with stack traces, and the step-1
recordings already carried 43,065 and 134,379 allocation samples — so
re-recording would have cost twenty minutes to produce a SECOND run whose
allocation could not be laid against step 1's CPU. Not re-recording is
strictly better evidence, not merely cheaper, and the throttle needed no
raise.

**The EdnReader correction rode in step 2's commit rather than its own.** It
is step-1 scope (it regenerates step 1's four tables), and a separate commit
would have been tidier. It is disclosed in that commit's message and here
instead.

**Linearity was not claimed.** The prompt asks whether the live set grows
linearly with events. The post-GC floor grows monotonically and its 40-second
increments FALL from ~100 MB to ~25 MB, which is not linear; the estimator is
a min-over-window through a collector that reclaims a different amount each
time, so the series is reported and the deceleration is left unexplained
rather than fitted.

## 4. Findings

**F1. THE AGGREGATOR WAS WRONG THREE TIMES AND PRODUCED A WELL-FORMED TABLE
EACH TIME.** (a) gawk runs escape processing over a `-v` assignment, so every
concern regex arrived with `\$` flattened to an anchor; all six concerns read
0.00% against a 100% residual. (b) Routed through a file instead,
`getline var < file` split that file on `RS` — already the sample separator —
so the list came back as ONE record and exactly one concern was defined; five
rows silently vanished rather than reading zero. (c) `:patient-state` matched
`evolve$evolve`, a class that does not exist, because `evolve` is a `defmulti`
whose methods compile to `evolve$eval<n>$fn__<n>`; it read 0.00% of a fold
that runs it on every event. Each was caught by a number that was implausible
rather than by an error, which is the whole hazard of this instrument class.

**F2. A NAMED SITE IS ONLY AS GOOD AS THE CLASS IT NAMES.** The named-site
list held `clojure.lang.LispReader` for "EDN parsing of the input log" and
reported 0.35%. `cli/read-ground-truth-stdin` uses `clojure.edn/read`, i.e.
`EdnReader`: the real figure is 20.12%, and one fifth of the check phase is
parsing. This was caught by the project-frame table, which is the argument for
carrying both — the named list only ever sees sites someone thought to name,
and it fails silently when the name is wrong.

**F3. EDITING A RUNNING BASH SCRIPT CORRUPTED AN IN-FLIGHT RUN.** I rewrote
`profile-cell.sh` in place while the 22,500 invocation was blocked in its
`clojure` call. Bash reads a script by byte offset, so on return it resumed at
a shifted offset: `line 244: ped: command not found`, and the cell banner
printed twice. The RECORDINGS survived intact (the `if [ ! -s gen.jfr ]` guard
skipped a second generate, and the check phase recorded once), and every
aggregation was re-run afterwards over the kept `.jfr` files — which is
exactly the separability the script's own header was built for. The rule the
next session needs: never edit a script that is currently executing; copy it
aside, or wait.

**F4. `occupancy-board`'s residual 0.48% is not site 3 leaking.** All 157 of
its samples at 22,500 are under `sim-check` and NONE under `decide`, counted
directly rather than assumed. Generate's in-run self-check calls the
from-scratch definitions the sites' gates keep alive.
`last-uncancelled-index` (4 samples) and `waiting-boarder` (2) ARE under
`decide`, at 0.01% each — the noise floor of a 32,394-sample recording,
recorded and not explained.

**F5. The 22,500 generate is byte-identical under BOTH new conditions.** Under
JFR with allocation sampling, and under `-Xmx8g` with GC logging, the log is
174,866,696 bytes digesting to `7d105743…78e0a` — the committed 2026-09-05
figure. Two more independent confirmations of the program's output identity,
free, at an order of magnitude past the oracle's 38 roots.

## 5. What this session did NOT do

R-measure-first was honoured: no engine code changed, and `components/` is
untouched in all three commits. The ranking in `measurements.md`'s section (d)
is a recommendation for the design channel and enacts nothing. In particular
the two `decide` methods were NOT fixed, the fourteen replays were NOT
collapsed, and no memory program was started — the third of those is
recommended AGAINST on this session's own evidence.

## 6. Suite figures

`make test`, once, as step 3's gate, at the tip of the three payload commits.
Read from the EXIT SENTINEL the command wrote itself (`/tmp/mt.done` = `0`),
never from the completion notification — the site-4 record's own §4 finding.

| | |
|---|---|
| exit | **0** |
| `Test results:` lines | 422 |
| passes / failures / errors | **27,935 / 0 / 0** |
| wall | 21 minutes 18 seconds |

`bin/verify-nist-lock` passed in the same run, six coordinates matching
`artifacts.lock.edn`. No test was added or changed by this session, so no
assertion-count movement was owed or seen.

## 7. Background processes

TEN harness-tracked background jobs, each judged by the sentinel its own
command wrote and each terminated before this section was written: one `/tmp`
search probe; one FAILED detached launch (below); the four profile recordings
(`/tmp/prof-run.done` = `0 0`); FOUR re-aggregation passes
(`/tmp/agg-run.done`, `/tmp/agg2.done`, `/tmp/agg3.done`, `/tmp/agg4.done`,
all zeros — one per instrument fault in §4's F1 and F2); the `-Xmx8g` GC run
(`/tmp/gc-run.done` carrying the digest itself); and `make test`
(`/tmp/mt.done` = `0`); and the CI waiter (`/tmp/ci-wait.done` =
`completed success 2f243c79…`). Everything else — `bin/preflight`, `make
state-derived`, and a dozen `jfr print` probes — ran in the foreground.

The CI waiter polls on a 30-second interval INSIDE its own job, which is the
shape R-sentinel asks for and not the shape it forbids: the waiting is the
job's, the session reads only the file it writes. `gh run watch --exit-status`
was deliberately not used — the site-4 record has it reporting complete while
the run was still `in_progress` — so it tests `status` explicitly and reads
`conclusion` only after.

**`nohup … & disown` INSIDE `wsl -e bash -lc` DOES NOT SURVIVE**, confirmed
live: the first launch of the recording driver was killed the moment that
`wsl` invocation exited, leaving an empty log and no process. The harness's
own `run_in_background` is the only launcher that holds a job here. This is
the already-recorded WSL hazard, met again in a new shape.

**ONE `sleep 240` WAITER EXISTED AND SHOULD NOT HAVE.** R-sentinel says it in
as many words — "no sleep loops" — and the site-4 record devotes its own §7 to
seventy-two of them. This session hand-rolled one, in the last wait for `make
test`, after eight clean waits. It is one and not seventy-two because the
completion notifications were being used as intended for the other eight; it
is still a violation and is recorded as one rather than rounded down.

`ps` before the close marker showed zero `sleep` and zero `java`.

## 8. HEAD landed

`385d7f0d` is the last payload commit — the three payload commits are
`10414cca`, `60359c46`, `385d7f0d`. This record and its prompt archive land on
top of it, and a close-marker commit follows once CI is verified green with
`gh run view`. Baseline for the whole session was `63d5ee64`.

## 9. CI

All four commits went out in ONE push, so GitHub Actions ran once, at the tip.
Verified with `gh run view` rather than assumed, and read from the waiter's own
sentinel (`/tmp/ci-wait.done`) before being confirmed a second time directly:

| commit | run | conclusion |
|---|---|---|
| `2f243c79` (tip, covering `10414cca`, `60359c46`, `385d7f0d`) | 34073537109 | **success** |

`bin/post-push-verify` ran immediately after the push, all three checks
passing: `origin/main` matches the tip, every commit message in
`63d5ee64..2f243c79` is pure ASCII, and the CI run was reported once rather
than awaited (AR-CI-4) — this section is where it was awaited. `gitleaks`
scanned 1,488 commits and 44.02 MB at the push hook and found no leaks.

Each of the four pushed messages was diffed against the file that produced it;
every diff was exactly one trailing blank line, which is `git log --format=%B`'s
own formatting artefact and not a mismatch.

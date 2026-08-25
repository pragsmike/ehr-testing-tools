# State of the project — hazards that bite

**REWRITTEN 2026-08-25 by the de-scaffold ruling, at `d6ad63a`.** This
file used to carry a continuity register: registers-and-their-gates,
what-this-repo-is, channel doctrine, environment prose. All of it was
either derivable (`state-derived.md`, regenerated and CI-diffed on every
push), stated better somewhere else (`AGENTS.md`, `notes/ADRs.md`), or a
promise nothing kept. It is now one thing only: **hazards a session will
hit at this tip, each with the command that reproduces it.** A hazard
that stops reproducing is deleted, not annotated. Nothing is carried
forward on the prior version's authority.

The prior 119-line text is in git history; the 724-line text before that
is verbatim in [`plans/state-history-2026-08.md`](plans/state-history-2026-08.md).

## The hazards

**1. `pgrep -f` / `pkill -f` self-match the harness shell.** The pattern
appears in the very command line doing the matching, so a `pkill -f`
aimed at a build kills the shell running it — it killed a 35-minute
build once. Use PIDs or a sentinel file.

```
pgrep -af 'make test'
```
Reproduces at `d6ad63a`: prints the `bash -c` that ran the `pgrep`.

**2. A piped or `tail`'d gate run reports the pipeline's exit code, not
the gate's**, and truncates the counts a session reconciles against. A
wrapper that captures `MAKE_EXIT` must also END with `exit "$MAKE_EXIT"`
— the harness reports the wrapper's LAST command.

```
make test > out.log 2>&1; MAKE_EXIT=$?; tail -40 out.log; exit "$MAKE_EXIT"
```

**3. `make test` does not run the integration tier.** It is
`poly test :all skip:integration`, so a gate living in
`projects/integration` can land unexecuted by every pre-push run.

```
sed -n '/^test:/,/^$/p' Makefile     # shows skip:integration
make integration                      # the other half, needs a primed artifact cache
```

**4. Generated surfaces go red if you edit their inputs and do not
regenerate.** Adding an ADR, or a line to any of the reading-set member
files, moves `.agents/state-derived.md`. CI regenerates and diffs.

```
make docsgen && git diff --exit-code
```

**5. Linux-side idle lies about WSL2 host contention.** `uptime` and
`/proc/stat` see only the VM. A timed figure must sample the Windows
side AT THE MOMENT OF THE FIGURE, and must name its kind — suite wall
vs `poly test`'s own `Execution time`. Mixing the two flipped a recorded
delta's sign (ADR-0170, L3-1).

```
powershell.exe -NoProfile -Command "(Get-CimInstance Win32_Processor).LoadPercentage"
```
Reproduces at `d6ad63a`: prints a percentage; `uptime` alone cannot.

**6. The skills mirror is byte-gated, both directions.** Edit
`.agents/skills/`, never `.claude/skills/`, then copy across in the same
commit.

```
diff -r .agents/skills .claude/skills
```

**7. Git runs from WSL only**, from the ext4 clone at
`/home/mg/src/ehr-testing-tools` — never a `/mnt/*` checkout. Enforced
by `.githooks/pre-commit` once `git config core.hooksPath .githooks` is
set per clone.

```
bin/preflight
```
Fail-closed: exits non-zero on any FINDING/FAIL/UNKNOWN, and checks the
edit root, `core.fileMode`/`core.ignorecase`, tree cleanliness,
HEAD-vs-remote and the last five CI runs in one pass.

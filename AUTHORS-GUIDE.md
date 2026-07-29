# Authors' Guide

This is the guide for future-you and any collaborator working on this
workspace. It exists so decisions don't have to be re-litigated every
time someone opens it. It is adapted from
[`ehr-testing-sim`](https://github.com/pragsmike/ehr-testing-sim/blob/main/AUTHORS-GUIDE.md)'s
own guide of the same name — see `notes/ADRs.md` ADR-0001 for why this
workspace adopts sim's authoring conventions as canonical rather than
inventing its own or deferring to tools' (sim's form wins where the two
differ; tools conforms on arrival, a later session).

## 1. Git operations: WSL only, and whose hands are on them

**All git operations on this repo — especially commits — are done from
WSL, never from native Windows.** Same hard-won reasons as sim: mixed-
platform git usage is how repos end up with line-ending wars, spurious
executable-bit flips, and diffs that are 90% whitespace noise. Windows
is fine for building, reading, and running tools once they exist; it
is not fine for writing to git history. `.githooks/pre-commit` and
`.githooks/pre-push` enforce this once a clone runs `git config
core.hooksPath .githooks` (see `AGENTS.md`).

**Commits and pushes are the author's ceremony (ADR-0001, R6).** An
agent session prepares the working tree and proposes commit messages;
it does not run `git commit`, `git push`, `git merge`, or `gh` on its
own initiative. A session may be told, explicitly, in that session's
own chat, that the author wants it to execute commits directly for the
remainder of that session — that's a live delegation scoped to the
session that received it, not a rewrite of this rule. The next session
starts back at the default: agent prepares, author commits, unless
told otherwise again. Pushing to the shared GitHub remote is a step
further still — confirm before pushing even in a session where commits
have been delegated, the same as any other push to a shared remote.

**Reaching WSL from a Windows-launched agent session.** If your shell
is Git Bash/MINGW64, do not run git natively and do not invoke
`wsl.exe <command>` inline with untrusted interpolation — MSYS path
conversion can mangle arguments and quoting, and heredoc commit
messages are unsafe through some wrapping paths (backticks can get
shell-interpreted before reaching WSL). Prefer `wsl -e bash -lc "cd
/mnt/c/<path> && <command>"` with the command passed as a single
argument, or write the commands to a script file and run that. If WSL
is unreachable, stop and hand the ceremony to the author — never
commit from the Windows side.

### Staging hygiene between checkpoints (R26e, discipline-parity session)

Two prior sessions slipped their commit boundaries under R6 — staged or
committed content that belonged to a later checkpoint, or omitted a
pathspec half of a rename (`notes/ADRs.md` ADR-0001's own "Step 7
commit-scoping mistake, self-caught" deviation record is one receipt).
This is now a standing ritual, not a retrospective fix: **between COMMIT
checkpoints, keep the index empty except for the checkpoint currently in
flight.** Before handing any checkpoint to the author (or, this session,
before executing the delegated commit), run `git diff --cached --stat`
and record its output in the session's own record; anything staged
beyond that checkpoint's own stated scope gets unstaged (`git restore
--staged <path>`) before the commit, not folded in silently because it
happened to already be there.

### Pre-push hooks: gitleaks

`.githooks/pre-push` refuses to push unless `gitleaks detect` finds
nothing and `clojure -M:poly check` is green -- tests run in CI, not in
this hook (`notes/ADRs.md` ADR-0003). `gitleaks` isn't packaged for apt
on Ubuntu/WSL2;
install it from a GitHub release, checksum-verified, same method used
for sim's own go-public secrets audit (sim's `notes/facts-register.md`
F15):

```sh
cd /tmp
curl -sL -o checksums.txt https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_checksums.txt
curl -sL -o gitleaks.tar.gz https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz
grep linux_x64 checksums.txt   # compare by eye against: sha256sum gitleaks.tar.gz
tar xzf gitleaks.tar.gz gitleaks
mkdir -p ~/.local/bin && install -m 755 gitleaks ~/.local/bin/gitleaks   # no sudo needed if ~/.local/bin is already on PATH
rm -f gitleaks gitleaks.tar.gz checksums.txt
gitleaks version   # expect 8.30.1
```

Pick the release asset matching your platform (`linux_arm64`,
`darwin_x64`, etc. — see the checksums file) if not `linux_x64`.

## 2. The pack ritual — RETIRED (discipline-parity session, 2026-07-28)

Not ported, and this is now a closed decision, not an open one. Both
sim's and tools' own `Makefile` `pack`/`pack-skills`/`pack-push` targets
existed to feed a non-git chat surface that couldn't read either repo's
filesystem directly — and both repos had *already independently retired
`pack-push`* from their own session-end ritual before the merge (sim,
2026-07-27; tools, 2026-07-25), once each repo went public and its own
design channel could clone directly instead. Every session run against
this workspace so far has had full filesystem and git access with no
non-git surface to feed; the situation the pack ritual exists to work
around does not occur here. If that ever changes, the mechanism is
cheap to reconstruct from either parent's own `Makefile` (preserved in
git history, `stable-pre-monorepo`/`213abaa`) — not preemptively ported.

## 3. ADR rules

`notes/ADRs.md` holds every workspace architecture/authoring decision
in one file, numbered sequentially, Status Accepted unless noted
otherwise, starting fresh at ADR-0001 for this workspace (not
continuing sim's or tools' own numbering — see ADR-0001 itself for how
it cross-references the legacy sequences it supersedes-in-place-of).
**Never silently revert an Accepted decision — supersede it with a new
numbered record.** This file outranks any agent's or collaborator's
own inference about why the workspace is organized a certain way; read
it before restructuring anything.

Sim's and (later) tools' own historical ADR sequences move into this
workspace intact as provenance (`notes/sim/ADRs.md`, byte-identical to
the source, never edited for new paths or namespaces) — cite them
origin-qualified (`sim/ADR-0008`) when this workspace's own ADR-0001
references a decision made before the merge.

## 4. Facts-register discipline

Inherited from sim, **live at `notes/facts-register.md`** as of the
discipline-parity session (2026-07-28, R25). The discipline is
**assert → register → date** — any time a doc in this workspace
asserts a load-bearing, externally verifiable fact (a license, a
release status, a dependency's capability), it gets an F-row in
`notes/facts-register.md` in the same commit — claim, where it's
asserted, evidence, and a last-verified date. The register carries
forward any still-load-bearing fact from the frozen provenance
registers below, each citing its origin row (`sim/F8`, `tools/F12`,
etc.) rather than being re-verified from scratch — see the register's
own header. Above the F-row table, an **Index** (tools' own addition,
adopted at instantiation, `notes/discipline-parity-audit.md` row M5a)
gives a one-line-per-row digest; update both in the same commit as any
new row, same discipline `notes/tools/facts-register.md` already
modeled.

`notes/sim/facts-register.md` and `notes/tools/facts-register.md`
(moved as provenance, per §3) remain the historical record for facts
asserted before the merge; they are not superseded by the live
register, and their F-rows are not migrated forward automatically — a
workspace-level claim that repeats one of theirs needs its own fresh
F-row, re-verified, not a pointer back, unless it is explicitly carried
forward with an origin citation as described above.

## 5. Checkpoint and deviation discipline (this migration's own convention)

Distinct from sim's own 40-session culture (`docs/way-of-working.md`
describes both): the bootstrap-and-landing sessions that built this
workspace follow a **checkpoint convention** — a session prompt marks
COMMIT points (the author commits before the session continues) and
AUTHOR ACTION points (git surgery, placing external documents — things
only the author does). When a step's premise turns out false against
the live tree, the rule is **fix-forward with disclosure**: stop,
record the finding in that session's own deviation appendix, and ask
— never silently adapt or guess. See `docs/way-of-working.md` for the
fuller shape and `notes/ADRs.md` ADR-0001's own deviation record for a
worked example.

## 6. User-facing docs are agent-read (inherited from tools, ADR-0006)

An AI assistant, acting on a human's behalf, is a first-class consumer
of this workspace's user-facing docs (`SETUP.md`, component-level
`docs/`, CLI help text) — not an edge case. Three style preferences
follow, not gates: prefer exact, copy-pasteable commands over prose
descriptions of commands; keep heading anchors stable across a page's
regeneration, since an anchor may be a link target an assistant already
cited; and make error text self-explanatory without a human in the loop
to interpret it — naming valid options plus a `run: ehr help`-style
hint, not a bare stack trace.

## 7. Standing doctrine, promoted from ADR findings (R26, discipline-parity session)

Five lessons, each surfaced by a real incident during this workspace's
first week and recorded in its own ADR at the time, promoted here as
standing instructions rather than left as prose buried in ADR context
sections:

**(a) The index, not the working tree, is what a clone inherits.**
A file present on disk but never `git add`ed does not exist for anyone
who clones this repo — CI, a collaborator, or `poly` itself, which
reads the committed tree. Verify with `git status`/`git ls-tree`, not a
directory listing. (`notes/ADRs.md` ADR-0004, the executable-bit
incident: the working tree had the right file mode; the index didn't,
and only the index travels.)

**(b) Local state is not clone state.** Warm dependency/artifact
caches, sibling checkouts, and any other fact true of this machine
right now are not facts true of a fresh clone. `make ci-parity` — a
real fresh `git clone` to a scratch directory, cold artifact cache,
per-push lane run there — is the standing local probe for this;
run it before trusting a working-tree-green result for anything
touching caches, generated files, or filesystem layout.
(`notes/ADRs.md` ADR-0004, "the generalized trap.")

**(c) Tests and tools run with cwd = workspace root.** Every
cwd-relative literal path in this workspace (`artifacts.lock.edn`,
`config/synthea/synthea.properties`, `components/tools/test-fixtures/**`, etc. — see
`notes/ADRs.md` ADR-0002's own deviation record) resolves against the
JVM process's actual working directory, which `poly test` and `bin/ehr`
both fix at the workspace root, not a project or component
subdirectory. Adding a new cwd-relative path without confirming this
invariant still holds is how a test passes locally and fails from a
different invocation point. (`notes/ADRs.md` ADR-0002.)

**(d) "Superseded" requires a load-bearing inventory before the drop.**
Marking pre-carve material "superseded" without checking whether
anything still depends on it is how a real gate (the integration lane,
the coverage floor) silently disappears. The carve-loss audit
(`notes/carve-loss-audit.md`) is the method: every non-generated path
at each parent's final tree, diffed against the current tree, each row
given an explicit disposition rather than assumed accounted-for. Run
this method again before any future large drop, not just this one.
(`notes/ADRs.md` ADR-0004.)

**(e) Dependency direction is poly-enforced at brick level.** The rule
("`components/tools`/`projects/conformance` may depend on
`components/sim`; `components/sim` must never depend on anything
tools-derived") is not a convention to remember — `poly check` fails
the build if it's violated, the same way a brick reaching into another
brick's non-interface namespace fails. `clojure -M:poly ws
get:components:keys` and the `poly deps` matrix are the first place to
look when a dependency-direction question comes up, not `AGENTS.md`'s
own prose restatement of the rule. (`notes/ADRs.md` ADR-0005, the `ehr
sim` mount — the moment the rule stopped being "these are separate
repos" and started being purely poly-enforced.)

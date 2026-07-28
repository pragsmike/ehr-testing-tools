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

## 2. The pack ritual

Not yet ported to this workspace. Sim's `Makefile` `pack`/`pack-skills`/
`pack-push` targets are a filesystem-access workaround for chat
sessions that can't read the tree directly; whether this workspace
needs the equivalent is undecided (not one of ADR-0001's named holes —
raise it with the author if a session actually needs it).

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

Inherited from sim, not yet instantiated here: the discipline is
**assert → register → date** — any time a doc in this workspace
asserts a load-bearing, externally verifiable fact (a license, a
release status, a dependency's capability), it gets an F-row in
`notes/facts-register.md` in the same commit — claim, where it's
asserted, evidence, and a last-verified date. That file doesn't exist
yet at the workspace level (nothing here has asserted such a fact
yet); create it the first time one does, rather than pre-seeding an
empty table. Sim's own `notes/sim/facts-register.md` (moved as
provenance, per §3) is the historical record for facts asserted before
the merge; it isn't superseded by this section, and its F-rows aren't
migrated forward automatically — a workspace-level claim that repeats
one of sim's needs its own fresh F-row, re-verified, not a pointer
back.

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

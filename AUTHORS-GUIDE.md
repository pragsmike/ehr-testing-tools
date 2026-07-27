# Authors' Guide

This is the guide for future-you and any collaborator working on this repo.
It exists so decisions don't have to be re-litigated every time someone
opens it. It is a scoped-down adaptation of
[`ehr-testing-tools`](https://github.com/pragsmike/ehr-testing-tools/blob/main/AUTHORS-GUIDE.md)'s
own guide of the same name — see `notes/ADRs.md` ADR-0003 for why this
repo adopts that repo's authoring conventions rather than inventing its
own.

## 1. Git operations: WSL only

**All git operations on this repo — especially commits — are done from
WSL, never from native Windows.** This is a hard rule inherited unchanged
from the sibling repo (same author, same machine, same hard-won reasons:
mixed-platform git usage is how repos end up with line-ending wars,
spurious executable-bit flips, and diffs that are 90% whitespace noise).
Windows is fine for building, reading, and running tools once they exist;
it is not fine for writing to git history. `.githooks/pre-commit` and
`.githooks/pre-push` enforce this once a clone runs `git config
core.hooksPath .githooks` (see `AGENTS.md`); don't rely on the hook to
catch a mistake for you — confirm you're in WSL *before* attempting a
commit, not after a rejection.

**Reaching WSL from a Windows-launched agent session.** If your shell is
Git Bash/MINGW64, do not run git natively and do not invoke `wsl.exe
<command>` inline — MSYS path conversion mangles arguments and quoting.
Instead: write the commands to a script file, then run `MSYS_NO_PATHCONV=1
wsl.exe bash /mnt/c/<path>/script.sh`. Heredoc commit messages are unsafe
through this wrapper (backticks get shell-interpreted before reaching WSL
— it has eaten commit-message text before); write the message to a file
and use `git commit -F`. If WSL is unreachable, stop and hand the
ceremony to the author — never commit from the Windows side.

## 2. The pack ritual — pack-push is DORMANT here (since 2026-07-27)

`make pack` concatenates most git-tracked files in the repo into
`ehr-testing-sim-pack.txt` (gitignored — it is session scaffolding, not a
deliverable), for pasting into a chat session that can't read the
filesystem directly. It elides `.agents/skills/**` and
`.agents/prompts/archive/**` — skill content is large and changes rarely,
so it doesn't belong in every session's context — and the header records
the elision. `make pack-skills` packs exactly the elided directories, to
a separate file.

**`make pack-push` is DORMANT, as of `notes/ADRs.md` ADR-0015.** This
repo's GitHub remote (`git@github.com:pragsmike/ehr-testing-sim.git`,
added private at ADR-0006) is now **public** — the trigger ADR-0003
named and ADR-0006 reaffirmed. Public raw file contents are fetchable
by URL (`raw.githubusercontent.com/pragsmike/ehr-testing-sim/...`), the
same demotion tools recorded at its own ADR-0008. **The session-end
ceremony is now: commit → `git push origin`.** `make pack`/`pack-skills`/
`pack-push` still work and remain available for the occasional
offline/local-snapshot use case, but none of them are a required step
anymore.

`pack-push` copies both packs into a local clone of the public
`pragsmike/packs` repo (`~/.packs`, cloned once by hand — see
`Makefile`), commits with a message naming this repo and its HEAD, and
pushes; a consuming surface fetches either pack by a plain
`raw.githubusercontent.com/pragsmike/packs/main/<file>` URL. **Ordering
caveat:** run `pack-push` *last* in a session, after the final commit,
not mid-session — the pack header's `git status --porcelain` line is
only a meaningful staleness signal if nothing was left uncommitted
afterward.

Pack markers (`========== FILE: ... ==========`, `========== END FILE
==========`) are only valid matched at line start. The Makefile's own
`pack` recipe legitimately contains this marker text mid-line (in the
`echo` commands that emit it) — a naive substring search over the pack
would misidentify those lines as marker lines. Anchor any parser to the
start of the line.

**Pre-release amend allowance — CLOSED 2026-07-27 (ADR-0015).** Pre-release,
sole-author message amends were permitted via `--force-with-lease`
followed by an immediate `pack-push` rerun; that allowance was
originally scoped to end at first release or second contributor —
neither has happened — but going public added a condition that scoping
didn't anticipate: once history is fetchable by anyone, a rewritten
commit can silently invalidate a clone or fork nobody here knows
exists. Public history is append-only in practice from this date
forward, regardless of release status. Fix mistakes with a new commit,
not an amend.

Mid-flight multi-session work ends with a handoff document in
`.agents/handoffs/`, not a pack — the pack is a filesystem-access
workaround for a chat session, not a substitute for recording what a
future session needs to pick up cold. Tools' `handoff` skill, kept at
the shared/user level per its shared-skill-layout convention rather
than copied into this repo (`notes/ADRs.md` ADR-0003), may be used to
generate it.

## 3. ADR rules

`notes/ADRs.md` holds every architecture/authoring decision in one file,
numbered sequentially, Status Accepted unless noted otherwise. **Never
silently revert an Accepted decision — supersede it with a new numbered
record** that says so explicitly. This file outranks any agent's or
collaborator's own inference about why the project is organized a
certain way; read it before restructuring anything.

## 4. Facts-register discipline

`notes/facts-register.md` holds only F-rows (externally verifiable facts
about tools, licenses, and ecosystem capabilities) — there is no C-table
here, because there is no manuscript tracking drafting coverage.

The discipline is **assert → register → date**: any time a doc in this
repo asserts a load-bearing, externally verifiable fact (a license, a
release status, a capability claim about a dependency or upstream
source), it gets an F-row in the same commit — claim, where it's
asserted, evidence (a URL or inspectable artifact), and a last-verified
date. Revisit F-rows when the event that would resolve them completes,
and update the date whether the claim held up or not.

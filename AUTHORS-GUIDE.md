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

## 2. The pack ritual — pack-push is ACTIVE here

`make pack` concatenates most git-tracked files in the repo into
`ehr-testing-sim-pack.txt` (gitignored — it is session scaffolding, not a
deliverable), for pasting into a chat session that can't read the
filesystem directly. It elides `.agents/skills/**` and
`.agents/prompts/archive/**` — skill content is large and changes rarely,
so it doesn't belong in every session's context — and the header records
the elision. `make pack-skills` packs exactly the elided directories, to
a separate file.

**One deliberate inversion from tools' guide: `make pack-push` is
ACTIVE here, not dormant, and is the session-end ceremony.** This repo
has no public GitHub remote yet, so the `pragsmike/packs` transport is
the *only* way a chat session can read it — the same arrangement tools
used before it went public (its ADR-0008). A session here ends with
commit → `make pack-push`, not commit → `git push origin`. When
ehr-testing-sim gets a public GitHub remote, demote `pack-push` to
dormant the same way tools did, and record that demotion as a new ADR
(see `Makefile`'s header comment, which carries the same note).

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

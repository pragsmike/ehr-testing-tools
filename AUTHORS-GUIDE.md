# Authors' Guide

This is the guide for future-you and any collaborator working on this repo.
It exists so decisions don't have to be re-litigated every time someone
opens it. It is a direct, scoped-down adaptation of
[`ehr-testing-guide`](<!-- TODO: guide URL -->AUTHORS-GUIDE.md)'s own guide
of the same name — see [`notes/ADRs.md`](notes/ADRs.md) ADR-0002 for why
this repo inherits that repo's discipline rather than inventing its own.

## 1. Git operations: WSL only

**All git operations on this repo — especially commits — are done from
WSL, never from native Windows.** This is a hard rule inherited unchanged
from the guide repo (same author, same machine, same hard-won reasons:
mixed-platform git usage is how repos end up with line-ending wars,
spurious executable-bit flips, and diffs that are 90% whitespace noise).
Windows is fine for building, reading, and running tools once they exist;
it is not fine for writing to git history.

## 2. The pack ritual

`make pack` concatenates every git-tracked file in the repo into
`ehr-testing-tools-pack.txt` at the repo root (gitignored — it is session
scaffolding, not a deliverable), for pasting into a chat session that
can't read the filesystem directly.

Unlike the guide's pack, this one leads with a header block — repo name,
UTC generation timestamp, current `git rev-parse HEAD`, and
`git status --porcelain` (or `working tree clean`) — so a stale pack is a
one-glance check, not a diff you have to run by hand. **Regenerate the
pack before ending a session** whenever the repo state changed during it;
a pack whose header HEAD doesn't match the current one is stale and
should not be trusted for context.

## 3. ADR rules

`notes/ADRs.md` holds every architecture/authoring decision in one file,
each as Context / Decision / Alternatives rejected / Consequence, numbered
sequentially, Status Accepted unless noted otherwise. **Never silently
revert an Accepted decision — supersede it with a new numbered record**
that says so explicitly. This file outranks any agent's or collaborator's
own inference about why the project is organized a certain way; read it
before restructuring anything.

## 4. Facts-register discipline

`notes/facts-register.md` holds only F-rows (externally verifiable facts
about tools, licenses, and ecosystem capabilities) — there is no C-table
here, because there is no manuscript tracking drafting coverage the way
the guide's claims register does.

The discipline is **assert → register → date**: any time a doc in this
repo asserts a load-bearing, externally verifiable fact (a license, a
release status, a capability claim about a dependency), it gets an F-row
in the same commit — claim, where it's asserted, evidence (a URL or
inspectable artifact), and a last-verified date. Revisit F-rows when the
experiment or event that would resolve them (see `docs/experiments.md`)
completes, and update the date whether the claim held up or not.

## 5. Tool authoring conventions

<!-- TODO: written at generator kickoff -->

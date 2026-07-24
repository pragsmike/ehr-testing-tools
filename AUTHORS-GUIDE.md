# Authors' Guide

This is the guide for future-you and any collaborator working on this repo.
It exists so decisions don't have to be re-litigated every time someone
opens it. It is a direct, scoped-down adaptation of
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide/blob/main/AUTHORS-GUIDE.md)'s
own guide of the same name — see [`notes/ADRs.md`](notes/ADRs.md) ADR-0002 for why
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

`make pack` concatenates most git-tracked files in the repo into
`ehr-testing-tools-pack.txt` (gitignored — it is session scaffolding, not
a deliverable), for pasting into a chat session that can't read the
filesystem directly. It elides `.agents/skills/**` and
`.agents/prompts/archive/**` — skill content is large and changes
rarely, so it doesn't belong in every session's context — and the header
records the elision. `make pack-skills` packs exactly the elided
directories, to a separate file. The skills pack is regenerated whenever
`.agents/skills/` or archived prompts change materially.

Unlike the guide's pack, this one leads with a header block — repo name,
UTC generation timestamp, current `git rev-parse HEAD`, and
`git status --porcelain` (or `working tree clean`) — so a stale pack is a
one-glance check, not a diff you have to run by hand. A pack whose header
HEAD doesn't match the current one is stale and should not be trusted for
context.

**Sessions end with `make pack-push`, run after the final commit.** This
subsumes the older "regenerate the pack before ending a session" rule —
`pack-push` depends on both `pack` and `pack-skills`, so it regenerates
both and publishes both in one step. **Pack transport v2 (2026-07-24):**
`pack-push` copies both packs into a local clone of the public
`pragsmike/packs` repo (`~/.packs`, cloned once by hand — see
`Makefile`), commits with a message naming this repo and its HEAD, and
pushes. The design channel then fetches either pack by a plain
`raw.githubusercontent.com/pragsmike/packs/main/<file>` URL. This
replaces the original transport, a PATCH to a gist
(`4fcd1abb4e74a5b54f9c241877edd02a`, left in place but retired, not
deleted): raw GitHub content is CDN-served and reliably fetchable by the
design channel, where the gist API's rate-limited shared pool was not.
With the skills pack published the same way as the slim pack, no manual
upload step remains anywhere in the workflow — both packs regenerate and
publish in the same `make pack-push` run. The ordering still matters: run
last, not mid-session, so the header's clean-tree line remains the
invariant it's meant to be. A pack generated mid-session, before the last
commit, can legitimately show a dirty tree; a pack pushed last and still
showing a dirty tree is a real signal something was left uncommitted.

Pack markers (`========== FILE: ... ==========`, `========== END FILE
==========`) are only valid matched at line start. The Makefile's own
`pack` recipe legitimately contains this marker text mid-line (in the
`echo` commands that emit it) — a naive substring search over the pack
would misidentify those lines as marker lines. Anchor any parser to the
start of the line.

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

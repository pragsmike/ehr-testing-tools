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

## 2. The pack ritual (retired from the session-end ceremony, 2026-07-25)

`make pack` concatenates most git-tracked files in the repo into
`ehr-testing-tools-pack.txt` (gitignored — it is session scaffolding, not
a deliverable), for pasting into a chat session that can't read the
filesystem directly. It elides `.agents/skills/**` and
`.agents/prompts/archive/**` — skill content is large and changes
rarely, so it doesn't belong in every session's context — and the header
records the elision. `make pack-skills` packs exactly the elided
directories, to a separate file. `pack`/`pack-skills` **remain as
utilities**: reach for them when feeding a non-git AI surface that can't
clone this repo directly.

`make pack` also elides the vendored SimHospital corpus bytes
(`test/fixtures/v2/simhospital/messages.out`, [ADR-0011](notes/ADRs.md)):
it is large, static, and already content-addressed by git, so a session
needs `PROVENANCE.md` (and the vendored `LICENSE`) to know what the
corpus is and where it came from, not the ER7 bytes themselves — those
stay packed while `messages.out` alone leaves. This elision is *not* the
complement of `pack-skills`: `pack-skills` packs only
`.agents/skills/**` and `.agents/prompts/archive/**`, unchanged by the
corpus's addition to what `pack` elides — see the `Makefile`'s
`PACK_ELIDE_PATTERN`/`PACK_SKILLS_PATTERN` split.

Unlike the guide's pack, this one leads with a header block — repo name,
UTC generation timestamp, current `git rev-parse HEAD`, and
`git status --porcelain` (or `working tree clean`) — so a stale pack is a
one-glance check, not a diff you have to run by hand. A pack whose header
HEAD doesn't match the current one is stale and should not be trusted for
context.

**Sessions now end with commit → `git push origin`, full stop.** Both
this repo and the `pragsmike/packs` transport repo are public
(`notes/ADRs.md` ADR-0008); the design channel clones either directly
instead of fetching a pack, so publishing a pack on every session's exit
no longer earns its keep as a ritual step. `make pack-push` is
**dormant, not deleted** — it still works exactly as described below,
for the day a pack-consuming surface is needed again — it is simply no
longer part of what ends a session. **Pack transport v2 (2026-07-24,
retained as history):** `pack-push` copies both packs into a local clone
of the public `pragsmike/packs` repo (`~/.packs`, cloned once by hand —
see `Makefile`), commits with a message naming this repo and its HEAD,
and pushes; a consuming surface fetches either pack by a plain
`raw.githubusercontent.com/pragsmike/packs/main/<file>` URL. This
replaced the original transport, a PATCH to a gist
(`4fcd1abb4e74a5b54f9c241877edd02a`, left in place but retired, not
deleted): raw GitHub content is CDN-served and reliably fetchable, where
the gist API's rate-limited shared pool was not. If `pack-push` is run
by hand, the same ordering caveat still applies: run it last, not
mid-session, so the header's clean-tree line remains the invariant it's
meant to be — a pack generated mid-session, before the last commit, can
legitimately show a dirty tree; a pack pushed last and still showing a
dirty tree is a real signal something was left uncommitted.

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

## 6. User-facing docs are agent-read

`docs/positioning.md`'s Audience register names an AI assistant, acting
on a human's behalf, as a first-class consumer of this repo's
user-facing docs — not an edge case. Three preferences follow from
that, style guidance rather than a gate: prefer exact, copy-pasteable
commands over prose descriptions of commands; keep heading anchors
stable across a page's regeneration, since an anchor is a link target
an assistant may have cited; and make error text self-explanatory
without a human in the loop to interpret it (DOC-1's enumerable-options
error family — naming valid options plus a `run: ehr help`-style hint —
is the precedent).

## 7. Session-prompt verification checks carry their invariants

A verification command a session prompt mandates is a measurement; the
invariant it's meant to encode is the claim. They can disagree, and the
executing agent must be able to tell which one broke — without the
invariant stated alongside the command, both failure modes below
collapse into a guess.

**Two failure modes.** Reality disagreeing with a sound check is a
finding — record it (an F-row, if it's a fact) and never adjust
reality's numbers toward the prompt's own figures. A check misencoding
its own stated invariant is an escalation — stop, report, and wait for a
corrected check from the author; never silently patch the check to
pass, and never silently wave a failure through.

**Craft discipline.** Prefer checks on structure and membership (file
lists, `FILE:` framing lines, exit codes, counts of structural markers)
over substring checks that ordinary prose can satisfy. Beware
self-reference: session prompts get archived and packed, so a prompt
that names an artifact will itself contain that name in its own
archived text — a substring check run over a pack that includes the
prompt archive is a *structurally guaranteed* false positive, not an
unlucky one. Where a check reproduces a reference figure, state what a
reproduction failure means; the corpus-adoption prompt's "a reference
figure fails to reproduce → the discrepancy is the deliverable; record
it, do not adjust toward this prompt" is the model to copy. Prompts
archive **as issued**: a defective check is corrected by an author
ruling recorded in the session report, never by editing the archive —
the archive is a record of what was asked, not of what should have been
asked.

Two sessions on 2026-07-26 hit each mode from opposite sides. The
SimHospital corpus-adoption session's reference figures
(`.agents/prompts/archive/2026-07-26-simhospital-corpus-adoption.md`)
described the corpus as "segment delimiter CR" — true, but coarser than
the vendored bytes' actual framing, which also separates messages with
a blank LF line; the prompt's own tripwire treated the gap as a finding
rather than a failure, and it now stands as F25 in
`notes/facts-register.md`. The pack-elide session's Step 1 verification
(`.agents/prompts/archive/2026-07-26-pack-elide-and-research-errata.md`)
mandated a bare `grep -c "simhospital"` over the skills pack, expecting
0; it returned 8, every hit prose inside the archived corpus-adoption
prompt's own legitimately-included text. The stated invariant — no
corpus file enters `pack-skills` — held; only the substring encoding
was wrong. The executing agent stopped and reported instead of
adjusting the grep, the author issued a corrected membership check
in-flight, and the archived prompt keeps the original, defective grep
exactly as issued.

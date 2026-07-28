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
it, do not adjust toward this prompt" is the model to copy. Prompt
**bodies** archive as issued; an in-flight author ruling that corrects a
defective check appends as a dated deviation record rather than editing
the body — the same shape as the research-doc errata convention
(`docs/research/HL7v2-sanitized-corpus-research.md`), applied here to
prompts instead of research records.

Two sessions on 2026-07-26 hit each mode from opposite sides. The
SimHospital corpus-adoption session's reference figures
(`.agents/prompts/archive/2026-07-26-simhospital-corpus-adoption.md`)
described the corpus as "segment delimiter CR" — true, but coarser than
the vendored bytes' actual framing, which also separates messages with
a blank LF line; the prompt's own tripwire treated the gap as a finding
rather than a failure, and it now stands as F25 in
`notes/facts-register.md`. The pack-elide session's Step 1 verification
mandated a bare `grep -c "simhospital"` over the skills pack, expecting
0; it returned 8, every hit prose inside the archived corpus-adoption
prompt's own legitimately-included text. The stated invariant — no
corpus file enters `pack-skills` — held; only the substring encoding
was wrong, and the corrected membership check plus the ruling are
recorded in that prompt's own dated "Session deviation record" appendix
(`.agents/prompts/archive/2026-07-26-pack-elide-and-research-errata.md`,
appended before this deviation-record convention was itself ratified).

## 8. Hermeticity: path split, not tag filter

`AGENTS.md`'s hard rules state the invariant (cold-clone, no-network
`make test`/`make coverage`, every network-touching `test/`-path test
goes through an injected fake); this section is the mechanics moved out
from under it (2026-07-27, NAV-1) so that bullet could stay short.

Hermeticity is a **path split, not a tag filter**: tests that genuinely
need the network, a real external engine, or a warm artifact cache live
under `test-integration/`, not `test/` — e.g.
`test-integration/ehr_testing_tools/contract_pairing_test.clj`, which
runs the real `validator_cli.jar` subprocess against a real generated
corpus. Neither the `:test` nor the `:coverage` alias in `deps.edn` adds
`test-integration` to its paths, so both are cold-cache/no-network green
*by construction* — this is deliberate, not incidental: `make coverage`
runs cloverage's own test runner, which does not honor `clojure.test`'s
`:excludes` the way `cognitect.test-runner` does, so a tag-only
exclusion (the original design) does not actually keep integration
tests out of `make coverage`. A path a test isn't on is a path
cloverage can't run it from, regardless of what runner it uses.
`^:integration` metadata stays on those tests as documentation of *why*
they moved, not as the enforcement mechanism.

Run the integration suite explicitly with `make integration`
(`clojure -X:integration`, the `:integration` alias) — it requires `ehr
artifact fetch` for `synthea`, `temurin-jdk`, and `fhir-validator-cli`
first (see `make help`). The per-push CI job
(`.github/workflows/ci.yml`) runs `make test`, both lints, generated-doc
freshness, and `make coverage` — every *fast* gate — but never `make
integration`; that suite runs only in `.github/workflows/integration.yml`,
scheduled nightly plus `workflow_dispatch`, with the artifact cache
pre-primed and keyed on `artifacts.lock.edn` (ENF-1, enforcement wave,
2026-07-25). Its failure reports; it blocks no merge. Verified
2026-07-25: fresh temp clone, deps primed once, then both targets run
inside a network-isolated namespace against an empty artifact cache —
see `.agents/prompts/archive/2026-07-25-ci-hotfix-integration-path.md`
for the exact counts.

## 9. Skills provenance: the cyberneutics-derived set

Moved out of `AGENTS.md`'s Skills section (2026-07-27, NAV-1), which now
carries only the list and this pointer.

The cyberneutics-derived set is five skills: `scenarios`, `probe`,
`review`, `committee`, and `string-diagram` — all copied and adapted
from the public
[`pragsmike/cyberneutics`](https://github.com/pragsmike/cyberneutics)
repo, the author's own methodology project. `string-diagram`'s upstream
provenance is verified directly: its `SKILL.md` (name and description
match the copy here) lives at `.claude/skills/string-diagram/SKILL.md`
in that repo, retrieved HTTP 200 2026-07-24 — see `docs/notation.md`
for the citation.

Of those five, `scenarios`, `probe`, and `committee` additionally share
a single `.agents/cyberneutics-config.yaml` for the `situations_root`
key that resolves where their output directories live — **2026-07-23
divergence from upstream cyberneutics**: upstream splits this into
`.claude/cyberneutics-config.yaml` (scenarios/probe) and
`.agents/committee-config.yml` (committee); this repo unifies both
under `.agents/` since all three read the same key for the same
purpose. `scenarios` and `probe` also depend on
`agent/scenario-roster.md`, copied verbatim from the public
`pragsmike/cyberneutics` repo (see that file's header comment for
provenance) — copied now, adapted at first real use.

## 10. Session allowlist growth: the two-strikes rule

Moved out of `AGENTS.md`'s "Session permissions allowlist" section
(2026-07-27, NAV-1), which now states the rule in two sentences and
points here.

A command family earns a spot on the allowlist once it has been
genuinely needed — and therefore prompted for — twice, not on the first
occurrence. One-off needs stay as one-off prompts; a family that
recurs is a signal the allowlist is missing something structural, not
that this session happened to need something unusual. This keeps the
list scoped to the repo's actual routine work instead of accreting
every command a single session happened to run.

`Write`/`Edit` are the standing exception: they stay scoped to this
repo and are never broadened to the filesystem at large, regardless of
how often a wider grant might be convenient — a mis-pathed write
outside the repo was caught by a permission prompt this week, which is
exactly the failure mode that scoping exists to catch, so it is not
subject to the two-strikes rule or any other empirical pressure.

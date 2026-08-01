# 2026-08-01 — ehr-testing-tools: sim adopts ehrt.kernel.result (P3-5, ruled: adopt)

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `4bf9be0` ("refactor:
retire sim-cli and the sim project (F2 trigger fired, ruled
2026-08-01)"), already equal to `origin/main` — no fast-forward needed.
No commit or push run by this session; the tree is left uncommitted,
coherent, with the proposed commit message printed in the session's
close-out. `/mnt/c` clone not touched (all edits made via the UNC path
onto the WSL ext4 clone, per the dual-clone-edit-hazard discipline).

## Original prompt (verbatim)

2026-08-01 — ehr-testing-tools: sim adopts ehrt.kernel.result (P3-5, ruled: adopt)
Context
Author ruling 2026-08-01: sim's private ok/error envelope — carried from its life as a standalone repo, with a docstring whose own expiry condition ("until this lives alongside a shared kernel") has been true since the consolidation — is replaced by `ehrt.kernel.result`. One result vocabulary workspace-wide, per the result-not-throw doctrine. This deliberately adds the `sim → kernel` edge and amends sim's self-containment story: that story was about acceptance instruments (which still must never live in sim), not error plumbing. Run this after the sim-cli retirement session — a single-CLI world shrinks the baseline surface.
The envelope is internal, so the fence is total: no emitted byte changes. If adopting kernel changes any output — ground-truth log, v2/HL7 emission, FHIR emission, manifest, CLI message, exit code — that is a finding to escalate, not a diff to accept.
Work in the WSL ext4 clone; fast-forward to `origin/main`, record HEAD. No commit/push; `/mnt/c` untouched.
Read first

1. Sim's envelope namespace (locate it — the docstring with the expiry condition is the landmark) and every sim namespace that uses it.
2. `ehrt.kernel.result` — the target vocabulary (`:ok/:rejected/:error` envelope). Note any shape mismatch between the two vocabularies before writing code: if sim's envelope carries arms or keys kernel's doesn't (or vice versa in ways sim relies on), stop and escalate with the mismatch named — mapping decisions are author calls, not translation guesses.
3. `components/corpus/src/ehrt/corpus/sim_adapter.clj` — whatever conversion it does at the corpus↔sim boundary today should simplify or disappear; characterize it first.
4. `notes/sim/` (ADRs and facts register — the self-containment story lives there and gets the amendment); ADR-0008 (`decide`/`evolve` — untouched by this session, read so you don't touch it by accident).
5. Any sim-side lint or guard policing sim's requires (check for one before assuming none): if it enforces self-containment mechanically, it learns the kernel exception this session, allowlist-style per the gate-hardening precedent.

Author rulings

* AR-1 Baseline before anything: outputs and error paths both. Byte captures of: `ehrt sim run` across at least two seeds (ground truth + both emitter outputs, plus `--format er7` and `--format ground-truth` bare renders), the full `sim run --format ground-truth | sim check` pipe, `sim identifiers`, and `sim version` — the three verbs the 2026-08-01 retirement session mounted are now part of sim's surface, and `check` in particular runs the invariant catalog and almost certainly speaks the envelope. Also a representative corpus generate (sim lane), and — the part that most exercises the envelope — error paths: trigger representative failures (bad flag value, seed collision / existing out-dir, a `sim check` fed malformed stdin, whatever sim's real error surfaces are, enumerated from code) and capture exit codes, stderr, and any report bytes. Envelope changes leak through error rendering if they leak at all.
* AR-2 Mechanical adoption. Replace envelope construction/consumption across sim with kernel's; add the `sim → kernel` dependency (deps wiring per split-stage precedent); simplify the corpus adapter's conversion to whatever remains necessary; delete sim's envelope namespace (no tombstone). Move, don't improve — anything tempting goes in the deviation record as named-future.
* AR-3 Fence and records. Verify AR-1's captures byte-identical; `poly check` OK; both lanes green with count accounting; `notes/sim/` gets the dated self-containment amendment (acceptance instruments still forbidden; error plumbing now shared); the expired docstring's promise is marked honored in the ADR note; facts-register row + Index; `:necessary` checked (likely unchanged — say so rather than skipping); archive at `notes/prompts/2026-08-01-ehr-testing-sim-kernel-result.md` with deviation record.

Proposed commit message: `refactor: sim adopts ehrt.kernel.result (ruled 2026-08-01) -- private envelope retired per its own expiry condition, sim->kernel edge added, adapter conversion simplified; all sim outputs and error paths byte-identical`
Close-out summary for the author
HEAD at start; the envelope shape comparison (and any escalation it forced); the error-path inventory used for the baseline; what the adapter's conversion reduced to; count accounting; the amendment text; anything AR-2 stopped you from improving.

## Deviation record

**`poly check` refused the literal instruction on the first try — a
finding, not a judgment call.** AR-2 says "replace envelope
construction/consumption across sim with kernel's," and the obvious
reading is `(:require [ehrt.kernel.result :as result])` (the exact
namespace name P3-5's own Context sentence uses). `poly check` failed
with five `Error 101: Illegal dependency on namespace kernel.result …
Use kernel.interface instead` — Polylith's own brick-boundary rule
(cross-brick access only through a component's `interface` namespace,
the same rule judge/corpus/`bases/cli` already follow for kernel).
Fixed by requiring `ehrt.kernel.interface` instead, which re-exports
the identical seven functions (`ok`/`ok?`/`rejected`/`rejected?`/
`error`/`error?`/`valid?`) as delegating vars — no further code
difference, since the alias (`result`) and every call site were
unchanged either way. Recorded because the prompt's own text names
`ehrt.kernel.result` by its internal name throughout, and a future
reader diffing this session's commit against that text might otherwise
wonder why the require doesn't match.

**Read-first step 3 (`sim_adapter.clj`) found nothing to simplify —
characterizing before touching paid off by finding a no-op.**
`ehrt.corpus.sim-adapter` requires `ehrt.sim.interface` only, never
`ehrt.sim.result`, and its four functions (`run!`/`check!`/
`identifiers!`/`version!`) delegate straight through with no envelope
unwrapping, parsing, or reshaping today — there was no conversion to
simplify or make disappear. Its own docstring already claimed the two
Result vocabularies were "structurally, not just nominally, the same
shape" (citing `ADR-0012` property 3); adopting kernel makes that
claim literally true (same namespace) rather than a coincidence, but
the sentence itself doesn't become false or stale, so it was left
untouched — editing accurate prose to sharpen its own "why," with no
code change riding along, is exactly the kind of improvement AR-2's
"move, don't improve" fences out. Zero lines of `sim_adapter.clj`
changed.

**Read-first step 4's "`notes/sim/` gets the amendment" was reinterpreted
against this workspace's own stated discipline, not followed literally.**
`notes/sim/ADRs.md` carries an explicit, load-bearing header: moved
byte-identical at merge, "never updated for the workspace's new paths
or namespaces," with the *current* decision record living in
`notes/ADRs.md` instead (that header's own words) — a rule
`notes/ADRs.md`'s own frontmatter restates independently ("Legacy ADRs
move into this workspace intact as provenance … frozen, not rewritten
for new paths/namespaces"). Writing the self-containment amendment
into `notes/sim/ADRs.md` directly would have violated a rule this
session did not have standing to waive. Placed the amendment instead
as a new `ADR-0022` in the live `notes/ADRs.md`, cross-referencing
`sim/ADR-0001` point 4 origin-qualified per this file's own citation
convention — the *content* the prompt asked for (the amendment) lives
where this workspace's own rules say current decisions live; only the
*file* differs from a literal reading of "`notes/sim/` gets the
amendment."

**`notes/facts-register.md`'s "Index" and "Register" sections have
already drifted apart, independently of this session — matched the
last five sessions' actual practice rather than either fixing the
drift or inventing a third convention.** The file's own header
describes "Index" as short digests and "Register" as full detail, but
F14 through F18 (the four most recent sessions before this one) all
added only a full-text row to "Index," leaving "Register" stalled at
F13 (2026-07-31, ADR-0015). This session's F19 follows that exact
precedent — full text in "Index" only. Fixing the drift (writing an
actual short digest, or backfilling "Register") was judged out of this
session's own fence (P3-5 is about sim's envelope, not the facts-
register's own bookkeeping) and is named here as a candidate
named-future rather than silently perpetuated without comment.

**The pre-edit test-namespace count was measured with a `git
stash`/`stash pop` round-trip run *after* AR-2's edits, not before
them.** AR-1's own baseline captures (CLI byte output) were taken
first, against pristine HEAD, as instructed. The *test-count* half of
AR-3's "count accounting," though, was only recognized as needed after
AR-2's edits already existed — measured by stashing the (already
clean, `poly-check`-passing) working tree, running the per-push lane
against bare HEAD, then popping the stash back and re-running. Chosen
over reverting-and-redoing-the-edits because the stash round-trip is
strictly safer (git-tracked, verified restored via `git status` before
proceeding) and gets the identical answer a true before-first
measurement would have. Result: 168 `Testing ehrt.*` namespaces both
before and after, identical final tallies — confirmed, not assumed.

**`:necessary` was checked, not re-derived by clearing overrides** (the
split-stage sessions' own heavier method). Reasoned instead: adding a
real `:require` edge only ever *adds* brick-graph reachability, never
removes it, so it cannot trigger poly's warning 207 (a brick declared
but not reached) for any project — and `poly check` returned clean
with no warnings anywhere, both before and after. `workspace.edn`
carries no override naming sim or kernel today, so there was nothing
to clear in the first place. Disclosed as a lighter-weight check than
the split-stage precedent's full clear-and-rederive, since AR-3 asked
to "say so rather than skipping," not to reproduce that exact method
unconditionally.

**`deps.edn` comment text updated in five files beyond the twelve sim
namespaces AR-2 named outright** (root `deps.edn`'s `:dev` and `:ehrt`
aliases, `projects/conformance`, `projects/integration`,
`projects/ehrt-cli`) — no `:local/root` wiring changed anywhere (this
workspace wires bricks at the project level only, and kernel was
already composed in every project sim is), only the prose comment next
to each pre-existing `poly/kernel` entry, which made an explicit claim
about *who* requires kernel that sim's own new edge would otherwise
make quietly incomplete. Judged as accuracy upkeep on existing
documentation-as-code, not scope creep, since AR-3 itself required
documenting the new edge and these comments are exactly where this
workspace's own convention already puts that information (the same
comments record every prior kernel-consuming edge, e.g. corpus').

**`AGENTS.md`'s Constraints section and `docs/dev/architecture.md`'s
mermaid diagram/bricks table both asserted, in the imperative present
tense, that `components/sim` depends on nothing — a claim this session's
own edge makes false the moment it lands.** Updated both (a `sim -->
kernel` mermaid edge; the bricks-table row's "Never depends on
anything" sentence gains a "Depends on kernel only" clause ahead of
it) rather than leaving a load-bearing doc claim to go stale
silently — the same doc-currency posture the gate-hardening session's
own structure-currency test polices mechanically for brick
presence/absence, applied here by hand since no existing gate checks
dependency-*edge* claims.

**Nothing else touched, per this session's own scope.** No
`bases/sim-cli`/`projects/sim` work (retired already, prior session);
no `ehrt.kernel.result`/`.interface` code changes (kernel's own shape
was the target, not something to alter); no acceptance-instrument code
(judge, corpus generators/mutators/checks) touched or discussed beyond
naming them, in the ADR, as still forbidden to sim.

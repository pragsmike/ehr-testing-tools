# Way of working

This document describes the *meta-process* that produced this
repository: not what was decided (`notes/ADRs.md` is that record), but
how roughly forty independent Claude Code sessions, none of which ever
saw another's transcript, ended up behaving like one engineering
culture instead of forty different ones. It exists because the design
channel that orchestrated those sessions — a single long-running chat
conversation between the author and Claude, spanning this repo's
entire life to date — is being retired. What that channel knew about
*why* things are shaped the way they are lives, from this point
forward, only in what got captured into the repo. This document is the
capture of the part that hadn't been captured yet: not a product
decision, but the working system itself.

**Audience:** a future maintainer of this repo — human or AI — picking
it up cold, and anyone considering running a comparable multi-session,
agent-executed project of their own. This is not a tutorial. It is a
description of a working system, with its reasoning and its real
artifacts cited, so a reader can check every claim against the tree
rather than take it on faith.

## 1. The division of labor

Two roles, deliberately kept apart:

- **The design channel** (chat) does design, planning, prompt
  authoring, and review. This is where product decisions get argued,
  ADRs get drafted and ratified, and session prompts get written.
- **Claude Code sessions execute.** Each one is stateless, launched
  fresh, and never meets another. A session reads the repo, does
  bounded work against a brief, and leaves.

The author retains every commit and every ratification. Nothing lands
in `notes/ADRs.md` as Accepted, and no milestone lands in
`.agents/plans/roadmap.md` as landed, without the author's sign-off —
visible throughout the roadmap and ADR files as "author-ratified,"
"author-directed," or an explicit "Ratification record" (e.g.
`docs/gmf-interpreter.md`'s own closing section, ADR-0013's appendix
review).

**Continuity lives in the repo, not the conversation.** Since sessions
are stateless and never meet, nothing in any one session's own working
memory can be the thing that keeps forty sessions coherent — the repo
itself has to be that thing. Concretely, three documents outrank a
session's own inference every time:

- `notes/ADRs.md` outranks inference about why the project is
  organized a certain way (stated identically in `AGENTS.md`,
  `AUTHORS-GUIDE.md` §3, and the ADR file's own header: "Numbered,
  append-only. Never silently revert an Accepted ADR; supersede it
  with a new numbered record").
- `notes/facts-register.md` outranks memory for any externally
  verifiable claim — a license, a version, a capability. The
  discipline is stated as "assert → register → date"
  (`AUTHORS-GUIDE.md` §4); the register's own rows (F1–F22) are the
  receipts, several explicitly re-verifying or superseding an earlier
  row rather than silently editing it (F13 "SUPERSEDED — test removed
  per ADR-0014; row retained for history"; F17 resolving a test-count
  discrepancy by re-running, not by assuming).
- `AGENTS.md` is read every session — it says so of itself ("Read
  `docs/problem-statement.md` first... `notes/ADRs.md` records *how*
  and outranks your own inference").

**The observable consequence: a session has followed an ADR's own
reasoning over what a prompt or surface convenience might have
suggested, and said so.** `docs/gmf-interpreter.md` §7 records exactly
this for the M5a interpreter fixture: the hand-written GMF-JSON test
fixture was "tested against
`test/ehr_testing_sim/fixtures/fixture-clinic.json` (ADR-0013 point
6's own hand-written fixture — placed there, not `resources/modules/`,
per that point's own reasoning: this project's authored test content
carries no NOTICE obligation and is not vendored upstream data)." A
module-shaped JSON fixture living next to real vendored modules in
`resources/modules/` would have been the more obvious place to put it;
ADR-0013 point 6 said otherwise, in writing, ahead of the session that
built it, and the session followed the ADR rather than the more
convenient shape. That is the constitution binding harder than any
one instruction — the system working as designed, not a session
being difficult.

## 2. Session types, deliberately distinct

Three kinds of session exist, and the roadmap and ADR files show the
seams between them clearly:

- **Capture sessions** produce ADRs and specs only — no code. Every
  design-capture ADR says so explicitly: ADR-0007 ("no code accompanied
  this ADR at acceptance time"), ADR-0010 ("no code lands with this
  ADR"), ADR-0011 ("no code lands with this ADR"), ADR-0012 ("no code
  lands with this ADR"), ADR-0013 ("no code or resources land with this
  ADR"). Decisions made in the design channel are verified and written
  down here — never improvised mid-build.
- **Build sessions** execute test-first against an already-ratified
  spec. The standing instruction is "build, don't re-decide": M1's own
  roadmap entry states plainly that `docs/operational-models.md`,
  "reviewed this session, is this milestone's spec — nothing here
  redecides what that document already decided"
  (`.agents/plans/roadmap.md`). The M2a/M2b split (engine-refactor
  shapes, landed first; churn content, built against those shapes
  second) and the M5a/M5b split (the interpreter itself, pure and
  fixture-tested; `CompileTrajectory` and the first real vendored
  module, second) are the same "shapes-then-content" precedent applied
  twice, each recorded as deliberate in the roadmap rather than
  discovered by drift.
- **Consumer/acceptance sessions**, in the sibling `ehr-testing-tools`
  repo, exercise this repo's output as a real consumer would —
  `ehr gate` judging a sim-generated corpus, the manifest contract
  test, the "tools-side gate-loop baseline review" the roadmap flags
  as due after every message-shape change. This repo's own ADR-0001
  names the reason the binding contract tests live there and not here:
  "the binding cross-repo contract tests belong in tools' integration
  tree, where both codebases share a classpath." What that loop found
  wrong in *this* repo consistently arrives back here as a **finding
  to fix**, not a failure to relitigate — see §4 below for the three
  concrete instances.

**Why the split exists.** Judgment is concentrated where context is
richest — the design channel, where the author and the full design
history are both present — and execution is made fast and safe by
specificity: a build session working against an already-ratified spec
doesn't need to re-derive the reasoning, only to build correctly
against it and prove it red-then-green. This is also, concretely, why
a mid-tier model (Sonnet) sufficed for the entire project: the sessions
that needed the deepest judgment were capture sessions, done in the
design channel with the author present, not build sessions executing
alone. The rule this project actually followed was **escalate on
evidence, not on anticipation** — nothing in this repo's own record
shows a build session needing more model than it was given; every
hard call surfaces first as a design-channel capture decision.

The rhythm across a milestone: **capture → ratify → build → review →
repeat.** ADR-0013's own closing line makes the ratify step concrete:
"All eight of both documents' author-review recommendations are
ratified, 2026-07-26... M5a (below) is built directly against this
ratified design, not against a still-open recommendation."

## 3. The prompt discipline

Prompts in this project's own convention carry several recurring
parts, visible throughout the roadmap's session write-ups even though
the prompts themselves are not committed verbatim (see the honest note
below on this repo's own prompt-archive gap):

- **A read-first list** — which docs and ADRs a session must read
  before touching anything, the same shape this document's own
  originating prompt used (`AGENTS.md`, `AUTHORS-GUIDE.md`,
  `notes/ADRs.md`, the roadmap, `.agents/memory/architecture.md`,
  `docs/README.md`, before any writing starts).
- **Author-directed verbatim tasks vs. judgment tasks**, named
  separately — e.g. ADR-0013's fixture decision (point 6) is
  author-ratified verbatim; the M7 module survey's own extension past
  its original 10-module target ("Actual survey grew to 41 real
  modules read... because the hit rate was far lower than the prompt
  assumed") is the session's own judgment call, made after checking
  back with the author at a seam rather than either stopping short or
  overrunning silently.
- **Red-before-green obligations** — ADR-0004's test-first rule made
  explicit and mechanical: "a failing test precedes the implementation
  it motivates; sessions demonstrate red→green in their reports."
  `AGENTS.md`'s Code conventions section restates the same obligation
  as a standing rule, not merely this one ADR's framing.
- **A marked SEAM** — a clean stopping point where nothing lands
  half-done. The M7 session's own account names this directly:
  "Fifteen more modules were histogram-scouted after the seam
  checkpoint" (`docs/gmf-interpreter.md`, M7 section) — the session
  paused at the seam, checked with the author whether to keep going,
  and only then continued.
- **Verification-with-evidence sections** — never "tests pass," always
  the actual count and delta: every landed milestone in the roadmap
  states its test/assertion count and coverage percentage against the
  prior milestone's own baseline (e.g. M7: "437 tests / 1125 assertions
  green (up from the M6-era baseline 432/1116), coverage 97.37%/98.99%
  (steady)"), and whether the pinned-seed regression fixture moved or
  stayed untouched, with the reason either way.
- **A closing author-review list** — every judgment call a session
  made, enumerated for the author to ratify or correct. ADR-0013's own
  "Ratification record" (referenced from the roadmap's M5 section:
  "All eight of both documents' author-review recommendations are
  ratified") is this convention's clearest instance: eight named items,
  each individually accepted, not a single "session went fine" summary.

**Prompts are provenance.** The intent, stated in this project's own
Makefile comments, is that prompts are archived where the convention
exists — `.agents/prompts/archive/` is named as elided content in the
pack ritual (`Makefile`'s `PACK_ELIDE_PATTERN`), the same treatment
`.agents/skills/` gets. **Honest gap, not silently glossed over:** as
of this session, that directory does not exist in this repo. The
Makefile's own comment says so plainly: "this repo's `.agents/skills/`
is empty and `.agents/prompts/archive/` doesn't exist, so the pattern
elides nothing — kept anyway so the pack format... stays identical
across repos and nothing changes when skills arrive." The convention is
named and the plumbing is in place; the archive itself has never been
populated here. Prompts for this repo's forty-odd sessions lived in the
now-retiring design channel, not in a committed archive — which is
part of why this document exists: it is what survives the channel's
retirement in place of the prompts themselves. A future session
wanting the prompt-archive convention to be real, not aspirational,
would need to start actually writing prompts to `.agents/prompts/` —
named here as a residual gap (see also `.agents/session-records/`,
established below, which *is* populated starting with this session).

## 4. The standing rules and where each came from

Every rule below is stated once, with the real record it traces to —
so a future session can check the origin instead of re-litigating the
rule.

- **Test-first + properties for law-bearing constructs** — ADR-0004.
  Concretely, in this project, "law-bearing" means determinism, the
  invariant catalog, emitter derivability, and schema round-trips
  (`AGENTS.md`, `AUTHORS-GUIDE.md` §"code conventions"). The
  discipline has a real, cited payoff: the fixed-consumption property
  test at Milestone M4 caught two real bugs a hand-written unit test
  would likely have missed — Persona's own docstring first claimed 14
  fixed RNG draws per persona; the property test (a call-counting
  `java.util.Random` proxy, not a synthetic skip sequence) caught the
  true count at 13; and separately, the same milestone's churned-run
  property test surfaced that `:registered` initially carried no
  `:active-mrn`, silently breaking `engine/replay`'s bootstrap for
  every patient — caught by the *existing* M2b merge invariant, not a
  new check written for the new code
  (`.agents/plans/roadmap.md`, M4 section). A third property test in
  the same milestone caught a self-inflicted bug in the ER7-escaping
  workaround itself (naive five-pass decoding spelling a spurious third
  token at a boundary; `notes/facts-register.md` F9). Three real bugs,
  one milestone, all caught before commit by property tests exercising
  existing invariants against new code — the concrete argument for why
  this rule is load-bearing rather than ceremonial.
- **Co-landing** (a new engine step type ships with its invariants and,
  where relevant, its message type, in the same change) — ADR-0002
  point 5, sharpened at Milestone M1 to extend explicitly to the
  emitter's `message-type-registry`, not just `check.clj`: "a step type
  without a registered message type produces traffic that's invisible
  to every consumer downstream of `EmitHL7`" (`.agents/plans/roadmap.md`,
  M1 section).
- **Result-not-throw and category honesty** — ADR-0001 point 4
  establishes the vocabulary (`{:status :ok|:rejected|:error :category
  ... :payload ...}`); its most concrete correction is the M6 session's
  own recategorization: a config conflict (one patient ordinal assigned
  both an authored encounter-opening pathway and a GMF module) reached
  `engine/run` and blew up as `:self-check-failed` only once the
  invariant catalog caught the resulting double encounter — "a config
  authoring error wearing a 'bug in us' category." `ehr-testing-sim.run/
  incompatible-assignments` now catches this statically, before
  `engine/run`'s own RNG ever starts, as `:rejected
  :incompatible-assignment` (`.agents/plans/roadmap.md`, M6 section,
  "Task 0"). A second instance of the same discipline: M2b's
  `ehr-testing-sim.facility/allocate` no longer throws on capacity
  exhaustion; `run-command` surfaces `:error :capacity-exhausted`
  instead (M2b section).
- **The CLI-surface rule** ("demos and verification commands run
  through the CLI surface, never engine internals") — born directly
  from a consumer-loop finding: M3's `:pathways` reached
  `ehr-testing-sim.engine/run` from a direct API caller
  (`engine-test`) but never from `ehr-testing-sim.run/run-command` —
  "CLI-invisible despite 181 green tests and a demo that called
  `engine/run` directly." The M4 session's own Task 0 fixed the
  specific gap and wrote the rule into `AGENTS.md` so the *class* of
  gap is caught by review next time, not just by a downstream consumer
  again (`.agents/plans/roadmap.md`, M4 section, "The wiring fix,
  first").
- **Fixture-as-tripwire and ADR-0009's perturbation-is-policy
  discipline** — the pinned-seed regression fixture
  (`test/ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn`) is
  regenerated, deliberately and visibly, every time a milestone grows
  the engine's stochastic surface (ADR-0009, first invoked when M1's
  allocation-ladder and provider-sampling draws perturbed it, then
  again at M2a's seconds-granularity change, then M4's Persona event).
  ADR-0009's own policy is explicit that this is expected, not a
  regression to chase: "same config + seed still yields byte-identical
  output... but that guarantee is now stated precisely as a
  **within-version** guarantee." Milestones that *don't* touch the
  engine's config surface (site profiles, M5a, EmitState) report the
  fixture untouched *by construction*, each one an explicit,
  positively-checked claim in the roadmap rather than an assumption.
- **Evidence-over-survey** — a module is vendored only once it clears
  ADR-0013 point 4's curation bar against the real interpreter, not
  against how promising it looked in a design-time survey.
  `sinusitis.json` was named in the original three-module survey and
  won; at M7, of 41 further real modules read, only `appendicitis.json`
  cleared the bar cleanly, and `spina_bifida.json` — initially
  characterized as vendorable because its one `Death` state looked
  like a safely isolated tail, the same shape `Device`/`DeviceEnd`
  already cleared — was caught before any commit: attempting to vendor
  it test-first produced an immediate `:unsupported-state-type`
  rejection, because `Death` (unlike `Device`) was never actually
  promoted into the loader's recognized set
  (`.agents/plans/roadmap.md`, M7 section). The lesson recorded there
  verbatim: "'this deferred state sits on an excludable tail' is NOT
  the same claim as 'this module will load.'"
- **Fix-forward with disclosure** — when a real gap in ground truth
  surfaces, it gets fixed in the log itself, not tolerated by loosening
  a downstream check. M6's own emitter-coherence property surfaced that
  a degenerate but structurally legal churn sequence
  (cancel-admit-then-cancel-discharge against an already-discharged
  patient) left `:class` absent from folded state, while `EmitHL7`'s
  own PV1-2 rendering always asserted `:inpatient` regardless.
  `ehr-testing-sim.engine/evolve`'s `:cancel-discharge` method now
  restores `:class` as part of its reinstatement — "the fix closes the
  gap in ground truth; the projection was never loosened to tolerate
  it" (`.agents/plans/roadmap.md`, M6 section). The disclosure half is
  literal: the finding is written into the roadmap by name, not folded
  silently into a changelog line.
- **No-guessing for external facts** — `AGENTS.md`'s constraint ("do
  not invent facts about upstream sources") is cited by name in
  ADR-0015 as "this project's own culture." The facts register is the
  mechanical form of the rule: every LOINC code this project embeds is
  verified directly at `loinc.org` before use (F7, and the license
  terms themselves at F16); the FHIR test-data security label (HTEST)
  is verified against HL7's own worked example before being stamped
  into every emitted resource (F14); SNOMED codes are verified against
  a live term browser even when the obvious source (a browser fetch
  tool) fails, with the failure and the fallback both recorded, not
  silently swapped in (F10, F22). Every one of these is a numbered
  F-row — claim, evidence, date — the receipt a "no-guessing" rule
  needs to be more than a stated intention.
- **Findings-not-failures at cross-repo boundaries.** This repo cannot
  see `ehr-testing-tools`' own conventions directly (no sibling
  checkout was available to this session — see the note in Task 3's
  cross-repo item, below), but the shape of the boundary is visible
  from this side, repeatedly: something the consumer loop in
  `ehr-testing-tools` surfaces about *this* repo's output routinely
  comes back as a "Task 0" fix at the top of this repo's own next
  milestone, not as a bug report to argue with. See the three defect
  classes named in the next paragraph — each one a finding a downstream
  consumer surfaced, landed here as a fix, never disputed or
  rationalized away.

**The three defect classes the consumer loop caught that this repo's
own producer-side assertions missed — the concrete argument for why
that loop exists at all.** By the time each was caught, this repo's
own suite had hundreds to over a thousand assertions passing, and none
of them had caught the gap:

1. **M3 — correlated schema drift the mirror could not self-detect.**
   `manifest/build` and `MirroredManifest` both omitted
   `:schema-version` — the same omission on both sides, so the mirror's
   own tests validated its output against its own (also wrong) copy of
   the schema and passed. "A mirror cannot catch itself agreeing with
   its own mistake" — exactly why ADR-0001 puts the *binding* contract
   test in tools' own integration tree, where both codebases share a
   classpath, rather than trusting a same-repo mirror to catch its own
   drift.
2. **M4 — a real capability, CLI-invisible.** `:pathways` worked
   end-to-end through the engine's own direct API and had 181 green
   tests behind it, but never reached the standalone or embedded CLI —
   a downstream consumer mounting the CLI group got nothing. See the
   CLI-surface rule above.
3. **M6 — a config error miscategorized as an internal bug.** The
   tools-side full-capability session hit `:self-check-failed` from an
   ordinary config authoring mistake (one patient ordinal double-
   assigned), which is exactly the kind of finding a downstream
   consumer is positioned to surface first, since it's the one actually
   authoring config at the boundary. See the result-not-throw item
   above.

**Silent scope drift** (→ seams, author-review lists, §3 above) and
**mutual accommodation between sibling validators** (→ independent
oracles, human-reviewed baselines) round out the failure-mode list in
§5 below, alongside session amnesia and confabulated specifics.

## 5. The failure modes this defends against

Named plainly, each with its defense and its receipt:

- **Session amnesia** — no session remembers any other session. Defense:
  the repo *is* the memory (§1). A session that reads `AGENTS.md`,
  `notes/ADRs.md`, and `.agents/plans/roadmap.md` before acting has
  everything a session that remembered the whole design-channel history
  would have needed, filtered to what was decided rather than what was
  merely discussed.
- **Confabulated specifics** — an agent inventing a plausible-sounding
  fact under time pressure. Defense: the facts register plus the
  no-guessing rule (§4). The register's own honest-failure rows are the
  proof the discipline is real rather than decorative: F8 records that
  SSA's own site returned HTTP 403 and the claim rests on a secondary
  source, said so explicitly rather than silently upgraded to
  "verified"; F19 records that a cache-isolation attempt *didn't* work
  ("did not find a working full-isolation flag for the Maven side in
  the time available") rather than claiming success it hadn't earned.
- **Producer blindness** — a producer's own test suite, however large,
  cannot see a gap that only becomes visible from a consumer's vantage
  point. Defense: the consumer loop in `ehr-testing-tools` (§4's three
  defect classes — schema-version drift, CLI-invisibility, config-error
  miscategorization — none caught by this repo's own suite, all three
  caught by a downstream consumer actually using the output).
- **Silent scope drift** — a session quietly doing more or less than it
  was asked, with nobody able to tell after the fact. Defense: the
  marked seam and the closing author-review list (§3) — M7's own "seam
  checkpoint" is the concrete instance: the session stopped, the author
  was asked whether to extend the survey, and the extension happened
  only because the author said yes, not because the session decided
  unilaterally that more was better.
- **Mutual accommodation between sibling validators** — two codebases
  gradually agreeing with each other's mistakes because they're the
  only two things checking each other. Defense: independent oracles and
  human-reviewed baselines. This repo's own instance is ADR-0001's
  mirror/binding-contract split (a same-repo mirror is a tripwire, not
  a source of truth; the binding check lives where an independent
  classpath can catch drift) and the M6 emitter-coherence property,
  which reconstructs state from the *wire*, independently of the engine
  that produced it, specifically so the log and the wire cannot quietly
  agree on the same mistake (`ehr-testing-sim.v2-replay`, described in
  `.agents/plans/roadmap.md`'s M6 section as "an INDEPENDENT
  reconstruction of patient state... never touching the engine, the
  log, or the RNG"). **Marked tools-territory, pending:** whether
  `ehr-testing-tools` has its own named dependency-discipline ADR for
  the same failure mode (e.g. around its own gate/validator
  independence) was not checked this session — no sibling checkout of
  `ehr-testing-tools` was available, and this document does not invent
  a citation it can't verify (§4's no-guessing rule, applied to itself).
  A future session with that checkout available should read this
  paragraph and either cite the real ADR or remove this note.

## 6. Honest limits

What this process does **not** give you, stated as plainly as the
things it does:

- **It still requires a design channel with deep context, and an
  author who reviews everything.** Nothing above works without a human
  who reads every ADR before ratifying it and every session's
  judgment-call list before accepting it. This document describes a
  division of labor, not a way to remove the author from the loop.
- **Capture quality bounds build quality.** A build session executes
  well against a spec; it cannot repair a spec that was wrong at
  capture time. Every design-capture ADR's own "no code lands with this
  ADR" framing (§2) is also a statement of where the real risk sits —
  get the capture wrong, and a test-first, red-green, well-reviewed
  build session will faithfully build the wrong thing.
- **Attachment and channel degradation on very long design threads is
  real.** A single conversation spanning roughly forty sessions' worth
  of design work accumulates context a chat interface was never built
  to carry indefinitely — which is precisely why this project's own
  answer is to keep the *artifacts*, not the *conversation*, as the
  record: ADRs, the facts register, the roadmap, this document. This
  document is itself the final proof of that answer: it exists because
  the channel that could have simply been asked "what's the way of
  working here?" is retiring, and the answer had to be written down
  instead of remembered.

## 7. Trust-taxonomy placement

Per `docs/README.md`'s own "How much to trust what you read" section,
this document is **a description of practice** — closest in kind to
that page's "Specs and as-built records" class, but distinct from it:
those documents describe *what this simulator does*; this one describes
*how sessions building it behave*. It changes when the practice
changes, and it does not outrank `notes/ADRs.md` on any question of
*why* a structural decision was made — the ADR file remains the
reasoning-of-record for product and architecture decisions; this
document is the reasoning-of-record for the *process* that produces
them, kept separate for the same reason problem-statement.md and the
ADR file are kept separate: different rate of change, different
authority on different questions.

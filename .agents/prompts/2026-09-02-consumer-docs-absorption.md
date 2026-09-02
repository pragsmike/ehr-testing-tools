SESSION: consumer-docs — downstream-report absorption (2026-09-02)

A downstream QA team's experience report (channel-held; its asks are
restated below — do not request it) found the tree ahead of it twice
and behind it three times. Ahead: consuming-ground-truth.md ## Scale,
and formats.md "Read the top-level vector only". Behind: --patients
semantics stated nowhere, capacity-as-ceiling undocumented, and
future-features.md missing its ruled scale-ergonomics half. Docs
payload plus one stale-docstring addendum. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
docs/consuming-ground-truth.md (## The mix, ## Scale); docs/formats.md
("Read the top-level vector only"); docs/future-features.md;
docs/cli.md (grep -n -- --patients);
components/person-simulator/src/ehrt/person_simulator/interface.clj:30-45.

Author rulings, verbatim and binding:
- R-ergonomics (2026-08-30): future-features.md gains a scale-
  ergonomics section: min-events natural-boundary stopping, a
  summarize command, progress instrumentation, richer capacity-
  exhaustion diagnostics; streaming output enters as pending
  measurement. Consumer-voiced; internal sizing stays out.
- R-F1 (2026-09-02): the F1 paragraph stays verbatim; a dated
  addendum follows it.

Steps:
1. F1 addendum. After "...absence of red." append: "2026-09-02: arc
   3's fold has landed -- ehrt.sim.run requires this interface and
   calls initial-persona and persons; the paragraph above is the
   landing-time record, kept verbatim." Invariant: no other docstring
   line changes; docstring-only, behavior-identical.
   Gate: make test green locally (a src file is touched).
   Commit: docs: F1 addendum -- person-simulator is called since arc 3
2. In docs/cli.md every --patients row gains ": simulated ARRIVALS,
   not emitted-event volume (docs/consuming-ground-truth.md#scale)".
   Invariant: table pipes intact.
   Gate: the docs-tooling invocation-lint test green.
   Commit: docs: --patients is arrivals, not output volume
3. Open ## Scale in consuming-ground-truth.md with a subsection
   "What --patients buys you": arrivals, not volume; volume is
   dominated by the opt-in keys (point at ## The mix); reachable
   capacity (ward beds, surge, bed-cycle) is the ceiling, and
   :capacity-exhausted is the run refusing an arrival it cannot
   place -- raise capacity or lower --patients; end with a cross-link
   to formats.md "Read the top-level vector only". Invariant: every
   claim derives from the sections read in Read-first; no invented
   numbers; existing Scale text unmoved.
   Gate: the link-footnote gate green (no bare ADR-NNNN in docs).
   Commit: docs: scaling a run -- patients, capacity, the ceiling
4. Write R-ergonomics's section before "## What is not on this menu",
   opening with one bridge line (not every gap is a fault class).
   Per entry: what a consumer gets; one-line current workaround where
   one exists; none for progress. Invariant: no dates, no sizing.
   Gate: full make test green.
   Commit: docs: future-features -- the scale-ergonomics half
5. Session record (.agents/session-records/2026-09-02-consumer-docs-
   absorption.md) with an asks->disposition table: done-before-asked /
   done-here / on-the-menu. Archive this prompt to .agents/prompts/.
   Fences: no line added to any path listed in .agents/reading-sets
   .edn; no new roadmap row; no src change beyond step 1.
   Commit: docs: session record -- consumer-docs absorption
6. Push; verify CI yourself (gh run view); close-marker commit
   recording the green run id.

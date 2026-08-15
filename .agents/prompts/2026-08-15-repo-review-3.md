# Archived prompt: repo-review 3, with the rubric amendment as Step 0 (choice b) — 2026-08-15

Driving prompt for the session recorded at
`.agents/session-records/2026-08-15-repo-review-3.md`. Archived
verbatim per charter R-A (`notes/ADRs.md` ADR-0023). Drafted by the
design channel, 2026-08-15; placeholders filled after channel
fresh-clone verification of the micro-arc landing (four commits, tip
`b139de5`, tree clean; converter re-run independently by the channel in
a separate environment — byte-identical to both the committed
standalone `.mermaid` and the embedded block).

---

SESSION PROMPT — repo-review 3, with the rubric amendment as Step 0 (choice b)

SEQUENCING GATE: this session runs only after the string-diagram
micro-arc (ADR-0135) is closed, pushed, and channel-verified. The
skill's own rule: reviews open arcs; they do not interrupt them.

## Context

Two debts escaped every prior review and were found only incidentally
by the string-diagram micro-arc: `components/sim/docs/sim-theory-diagram.md`
was stale against its own converter and carried a three-session-old
regeneration request in its header, and that header's recipe cited a
path dead since ADR-0005. The channel's post-mortem (2026-08-14, this
arc's design-channel conversation) established that the reviews ran
their probes faithfully — the probes' POPULATIONS were registry-derived,
and both debts lived outside every registry consulted:

* Dimension 5's probe is `make docsgen` + diff, so "every derived doc"
  silently means "every make target" — a hand-regenerated artifact is
  invisible to it by construction.
* The path-resolution sweeps' scan roots exclude `components/*/docs/` —
  the SECOND hit of a class first documented at E-5
  (`.agents/plans/2026-08-05-alignment-audit-findings.md:135`), which
  fixed the instance and never generalized the root.
* Dimension 7's carried-item aging enumerates the intake/design
  registers; a standing request embedded in a file header ages
  invisibly.

All three are one error: reading a registry as if it were the
population. The author ruled choice (b): amend the skill and run the
next repo review under the amended rubric immediately, so the amendment
proves itself by running.

## Author rulings, verbatim

* "That response mentions long-standing debt, dangling stale
  references, etc. Why weren't those found in the most recent repo
  review? Does the review skill need amendment or revision?" (the
  charter question)
* "Let's finish the currently-running arc and do the latest choice b."
  (choice b: fold the amendment into the next repo-review arc's own
  Step 0 so the amended rubric runs immediately on adoption)
* The amendment texts below are channel-drafted under that ruling; land
  them verbatim. STOP-AND-REPORT if any anchor text named below is
  absent from the live skill file.

## Read first

* `.agents/skills/repo-review/SKILL.md` (whole file — the instrument
  being amended and then run)
* `.agents/plans/2026-08-09-repo-review-findings.md` (prior register:
  its scoreboard carries forward; its arithmetic gets re-derived per
  the skill's own step 4)
* `.agents/plans/2026-08-05-alignment-audit-findings.md` E-5 (first hit
  of the scan-root class)
* `notes/adr/0135-*.md` (the micro-arc that surfaced both debts; its
  Step 3.4/3.5 accounts are evidence for the history scan)
* `components/sim/docs/sim-theory-diagram.md` header (the discharged
  standing request and the disclosed dangling wire — expected first
  catches of the amended probes, see Step 1 notes)

## Step 0 — Preflight, tags, and the amendment

1. `bin/preflight` with `--expect-tag stable-20260814-exact-name`
   (verifies the standing tag at
   `46b82babf1e109f6a5748f175f8a687419a3ea3e`).
2. Pay the micro-arc's deferred close tag under the standing
   conditional license (channel fresh-clone verification, relayed with
   this prompt, plus the author-side CI check): `bin/tag-ceremony
   stable-20260815-result-nodes b139de589083c6b4967c1a4769b2c6a8d17feac4
   <message-file> --push` (message file:
   `tag-message-result-nodes.txt`, supplied with this prompt). If the
   author-side CI relay is absent from this prompt's context,
   STOP-AND-REPORT before pushing the tag.
3. Amend `.agents/skills/repo-review/SKILL.md`, four edits, exact text:

**(0.3a) The population-closure law.** In the rubric preamble, directly
after the sentence ending "never re-read a claim as its own
verification.", append as a new sentence in the same paragraph:

> A sibling law: every probe states its population and enumerates it
> from the tree, never from the registry under audit — the make graph, a
> scan-root list, and the intake registers are themselves audit
> subjects, and equating any of them with the population converts their
> omissions into silent green verdicts. The first question of every
> probe is "how do I know this is all of them?"

**(0.3b) Dimension 5 patch.** In dimension 5's probe list, after
"regenerate every derived doc and compare (`make docsgen` and any
sibling generators)", insert:

> — where "every derived doc" is enumerated from the tree first (grep
> tracked files for generation banners, converter references, and
> embedded regeneration recipes), diffed against the make graph's
> targets; a derived artifact with no registered regeneration path is a
> finding (class: unregistered derivation) regardless of its current
> freshness

**(0.3c) Dimension 1 patch.** In dimension 1's probes, extend
"re-resolve every cited path" to:

> re-resolve every cited path, with scan roots covering every tracked
> doc surface including `components/*/docs/` (the scan-root class has
> two recorded hits: E-5, 2026-08-05, and the sim-theory recipe path,
> 2026-08-14 — never a root narrower than the tree again)

**(0.3d) Dimension 7 patch.** In dimension 7's probes, after the
carried-item aging sentence ending "(the pairing-as-data precedent).",
append:

> Header-resident requests: grep tracked files for standing requests
> embedded outside the registers ("standing request", "TODO", "FIXME",
> "regenerate", "next session"); any request not mirrored in a register
> row is a finding (class: unregistered standing request), aged from
> its first appearance in git history, since the aging probe above
> structurally cannot see it.

4. Byte-copy the amended file to `.claude/skills/repo-review/SKILL.md`
   (the mirror is byte-identical today; keep it so — dimension 5's own
   D5-2 probe will check this very commit).
5. Commit (message-via-file, ASCII):

```
feat: repo-review rubric gains the population-closure law
(choice b, channel post-mortem 2026-08-14)

Three registry-as-population escapes motivated it: D5 regenerated
the make graph while a hand-regenerated diagram rotted outside
it; the path sweeps' roots excluded components/*/docs/ (second
hit of the E-5 class); D7's aging read the registers while a
standing request aged in a file header. Amendment runs
immediately: this session's own review executes the amended
rubric. Mirror synced same commit.
```

## Step 1 — Run the review, per the skill's own steps 1-5

Execute the amended skill as written: baseline + history scan + probe
battery + register + plan. Session-specific notes, not substitutes for
the skill text:

* Register lands at `.agents/plans/<date>-repo-review-findings.md`
  (this is repo-review 3). Carry the 2026-08-09 scoreboard forward;
  re-derive that register's own summary arithmetic from its rows per
  step 4's standing correction.
* History-scan window: 2026-08-09 to now — includes the ADR-0130
  through 0135 closes, the manual-review-2 arc (ADR-0134), the
  channel's owned misclassification of `sim-theory-diagram.md` as
  hand-authored (unearned specificity, on the record in the micro-arc's
  channel conversation and ADR-0135), and the pipe-masked `make test`
  exit codes the micro-arc caught and corrected (`tail` swallowing
  `make`'s exit; a candidate dimension-4 finding class: gate output
  piped through anything that eats the exit code or truncates the
  countable signature). Sibling candidate from the same close:
  `bin/post-push-verify` scanned only `a8a5e65..b139de5` while the push
  spanned `00bdad7..b139de5` — the session caught and hand-covered the
  gap; the range derivation itself is the finding.
* Expected first catches of the amended probes — expected, not
  pre-judged; record what the probes actually return:
   * D5 (0.3b): `sim-theory-diagram.md` as an unregistered derivation
     even though freshly regenerated (mitigation candidate: a `make
     sim-theory` target, which retires the header-recipe workflow
     entirely). The enumeration may find others; that is the point.
   * D7 (0.3d): the header-resident request class — the sim-theory
     instance is now discharged and says so in its header; the probe
     verifies the grep finds no OTHER undischarged instances.
   * D1 (0.3c): the widened roots re-resolve component-doc citations
     for the first time; E-5's own swept files are the regression
     check.
   * The dangling `Calibrate -- churn-profile -->` wire, disclosed in
     the diagram's header by ADR-0135: the session judges its
     disposition (accepted-as-disclosed vs finding) on the evidence.
* The register and plan land WITHOUT fixes — the skill's own law:
  nothing beyond the register and plan moves before rulings. Step 5's
  plan goes to the author via the design channel; STOP there.

## Step 2 — After rulings only

Ruled fixes run as `build-session` sessions per the skill's step 6,
each with its co-landed gate; the arc close per step 7. Not this
session's work unless the author's rulings arrive mid-session and say
so.

## Fences

* Step 0 touches ONLY: `.agents/skills/repo-review/SKILL.md`, its
  `.claude/` mirror, and the tag ceremony's own artifacts.
* Step 1 lands ONLY the dated register and the plan (plus session
  record/prompt archive at close if the session ends at the STOP).
* Zero fixes, zero `src`, zero regeneration beyond what probes require
  read-only, before rulings.
* STOP-AND-REPORT: any amendment anchor text missing from the live
  skill; the mirror diverging before the amendment; the micro-arc tag
  license's CI relay absent; any probe requiring a capability the
  session lacks (record the probe as blocked, never skip it silently).

## Tag message (supplied with the prompt)

```
ADR-0135 close: string-diagram terminal-output result nodes (Q1 a,
Q2 b), 21 use-case pages + pipeline.md regenerated mechanically,
sim-theory diagram regenerated under the author-licensed Step 3.5
widening with its dead recipe path fixed in both copies. Gate: third
make test run, unpiped, MAKE_EXIT=0, 636 zero-failure blocks
reconciled as baseline 632 + 4 (one new namespace in two project
contexts). Tag paid at the following session's Step 0 under license
case (i): design-channel fresh-clone verification 2026-08-15
(including independent cross-environment re-run of the converter,
byte-identical) plus author-side CI check, per the standing
rate-limit accommodation.
```

## CI relay (supplied with the prompt)

`gh run list` output confirming run `31884986962` green on the
micro-arc's tip commit, plus the eleven preceding runs on main.

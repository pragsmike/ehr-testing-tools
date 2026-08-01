# 2026-08-02 — Migration session 4 prompt

Repo: `ehr-testing-tools`. Clone: WSL ext4 (`~/src/ehr-testing-tools`),
fast-forwarded to `origin/main` (`20ca886`) before work began; `/mnt/c`
untouched this session. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed to the WSL ext4 clone per this repo's own
[[feedback-wsl-git-workflow]] and [[feedback-dual-clone-edit-hazard]]
conventions.

## Prompt, verbatim

> 2026-08-02 — ehr-testing-tools: migration session 4 — reading sets
> land, sim docstrings get their qualifier
>
> Context
> Fourth build session. Executes item 8 (`.agents/reading-sets.edn`
> with placeholder-equals-measured budgets — charter R-D's mechanism
> lands, numbers stay deferred) and the sim docstring sweep session 3
> roadmapped (40+ bare `ADR-NNNN` references in `components/sim/src`
> that predate the merge and now resolve ambiguously or wrongly against
> the live register). Items 5 (gated on the author's item-9 discovery
> probe) and 14 remain fenced. Standing ceremony; WSL ext4 clone;
> fast-forward to `origin/main`, record HEAD; `/mnt/c` untouched;
> roadmap rows same-commit.
>
> Author rulings
>
> * AR-1 (item 8) Reading sets as data. Create `.agents/reading-sets.edn`:
>   named sets — `:onboarding` plus task classes `:corpus`, `:sim`,
>   `:judge`, `:docs` — each a vector of repo paths and a `:budget-lines`
>   integer. Derive contents from what a cold session of each class
>   genuinely needs (AGENTS.md is in every set; `:onboarding` adds the
>   `.agents/` index layer and the roadmap; each task class adds its
>   component's interface files, its zone of `docs/dev/`, and the skills
>   relevant to it — justify each inclusion in an EDN comment, and
>   prefer omission: the budget test makes additions conscious forever
>   after, so start lean). Set every `:budget-lines` to the measured
>   actual of the set as composed — record the numbers prominently in
>   the session record; the author rules real budgets later against
>   these measurements (charter §6). Gate: a per-push test (docs-tooling
>   family) that resolves every path (missing file = red — reading sets
>   can't cite ghosts), sums real line counts, and fails any set over
>   budget. Red→green three ways: a ghost path, an over-budget seed, and
>   green on the landed actuals. AGENTS.md's "forthcoming" reading-sets
>   pointer updates to cite the file.
> * AR-2 The sim docstring sweep. Session 3's accounting discipline,
>   applied to `components/sim/src` (and `test` if the species appears
>   there): every bare `ADR-NNNN` reference classified — resolves to a
>   frozen `notes/sim/ADRs.md` entry (re-qualify as `sim/ADR-N`),
>   resolves correctly to the live register as written (rare for sim
>   src; leave, note it), or genuinely ambiguous (escalate, don't
>   guess). Docstring/comment edits only — zero behavior change, and say
>   so with the lane run. One-to-one accounting table in the session
>   record: every hit, its classification, its disposition. No gate for
>   this species (src docstrings are outside the doc tripwires'
>   jurisdiction by design); the sweep plus the accounting is the fix,
>   and recurrence is unlikely — the frozen registers stopped growing.
> * AR-3 Records. Report row 8 executed (the sweep annotates the
>   roadmap row session 3 created); roadmap to done-with-sha; dated ADR
>   note carrying the measured budget numbers so the author's future
>   budget ruling has its baseline citable.
> * AR-4 Fence. No skill distillation (5), no use-cases split (14), no
>   budget tightening — placeholders equal actuals, tightening is the
>   author's ruling.
>
> Checkpoints
>
> * C1 — AR-1: `feat: reading sets land as data with measured-actual
>   budgets and a per-push gate -- growth is now conscious (migration
>   item 8)`
> * C2 — AR-2 + AR-3: `docs: sim src docstrings origin-qualified --
>   bare ADR refs classified and cited one-to-one, zero behavior
>   change; measured budgets recorded for the author's future ruling`
> * C3 — ritual: `docs: session record and prompt archive -- migration
>   session 4`
>
> Close-out
> Session record is the close-out; chat echo three lines. Record: HEAD,
> shas, the composed sets with per-set measured line counts, all three
> red→green proofs, the sweep's full accounting table with any
> escalations, post-push verification.

## Deviation record

- **AGENTS.md grew by 3 lines mid-session** (the reading-sets pointer
  rewrite, C1), which changed every set's own measured actual since
  AGENTS.md sits in all five — caught by the gate itself failing on the
  first full-suite `poly test` run after the edit (not by the earlier
  isolated-namespace run, which predated the AGENTS.md edit and was
  therefore stale). Budgets recomputed to the post-edit actuals (523 /
  1519 / 574 / 644 / 433) before C1's commit; no set's composition
  changed, only the numbers. Named here because it's a small but real
  illustration of exactly the "growth becomes conscious" property AR-1
  asked the gate to provide — it caught its own author's edit.
- **A third citation-origin, not just two.** AR-2's own wording
  anticipated two dispositions (resolves to frozen `sim/`, or correctly
  already live) — the sweep found a third: `run.clj`'s "tools
  full-capability session" citation resolves to neither; it names a
  decision recorded in `notes/tools/ADRs.md` (frozen provenance from
  the OTHER pre-merge repo, not sim's own), now `tools/ADR-0015`. Not
  escalated as ambiguous — the topic match to `notes/tools/ADRs.md`
  ADR-0015 ("full-capability" baseline) was unambiguous once checked —
  but disclosed as a refinement of AR-2's own literal two-way framing,
  in its spirit (get each reference correctly qualified against
  whichever frozen register it truly belongs to) rather than a
  deviation from it.
- **Bulk mechanical requalification, not line-by-line hand edits.**
  Given 151 bare occurrences across 39 files, classified the small
  number of distinct ADR numbers (12: 0001–0015 minus three sim never
  cites) against both frozen registers' own topics first, verified no
  number collided ambiguously between `notes/sim/ADRs.md` and
  `notes/tools/ADRs.md`, then applied a scripted regex pass (`sim/`
  prefix, skipping anything already `sim/`- or `tools/`-qualified) —
  faster and more consistent than 149 individual edits, with every
  file's own hits still read in context first to confirm the topic
  match before the script ran. Two files (a JSON fixture's `remarks`
  field, an EDN fixture's header comment) needed a manual follow-up fix
  after the bulk pass, because they cited the wrong FILE PATH
  (`notes/ADRs.md`) rather than a bare number — the script's naive
  prefix-insertion produced `notes/sim/ADRs.md sim/ADR-0013`-shaped
  double-qualification on those two lines specifically; caught by a
  grep sweep for exactly that shape immediately after the bulk pass,
  fixed by hand, re-verified.
- **Roadmap's "Done" section moved from C2 to C3.** The prompt's own
  AR-3 said "roadmap to done-with-sha" without specifying which
  checkpoint; drafting it as part of C2 hit the self-citation
  impossibility a commit's message/tree can't reference its own
  resulting sha without an endless amend loop. Session 3's own
  precedent (checked against its real commits) resolves this the same
  way: the roadmap's Done-section update lands in C3 alongside the
  session record, citing C1's and C2's by-then-fixed shas — not
  self-referential. Followed that precedent rather than inventing a
  new one; `notes/ADRs.md`'s own dated-note addition (which only names
  itself as "this ADR," never a sha) stayed in C2 as planned.
- No other deviations. Both author rulings (AR-1, AR-2) executed as
  stated; AR-4's fence held (items 5, 14 untouched, no budget
  tightening attempted).

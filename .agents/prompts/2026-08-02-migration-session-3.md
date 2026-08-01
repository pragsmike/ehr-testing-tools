# 2026-08-02 — Migration session 3 prompt

Repo: `ehr-testing-tools`. Clone: WSL ext4 (`~/src/ehr-testing-tools`),
fast-forwarded to `origin/main` (`4092b4c`) before work began; `/mnt/c`
untouched this session. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed to the WSL ext4 clone per this repo's own
[[feedback-wsl-git-workflow]] and [[feedback-dual-clone-edit-hazard]]
conventions.

## Prompt, verbatim

> 2026-08-02 — ehr-testing-tools: migration session 3 — indexes become
> law, sim rows get their citations
>
> Context
> Third build session of the approved migration. Executes item 10 (the
> index-completeness gate — indexes stop being prose and start being
> checked) and item 3(a) (the sim citation-stubs pass, per ruling 2's
> citation-only merge). Items 5, 8, 14 remain fenced. Standing
> ceremony; WSL ext4 clone; fast-forward to `origin/main`, record HEAD;
> `/mnt/c` untouched. Roadmap rows updated same-commit.
>
> Author rulings
>
> * AR-1 (item 10) Index-completeness, both directions. Per-push test
>   over `.agents/` and `notes/`: every tracked file in an indexed
>   directory appears in its directory's README index as an exact
>   token, and every index entry names a file that exists (no ghosts).
>   Mechanics per the gate-hardening precedent. Exemptions as data in
>   the test, each with its ruling cited: `notes/sim/` and
>   `notes/tools/` (ruling 6 — its README-presence exemption extends to
>   completeness, same rationale: frozen dirs are self-describing and
>   byte-stable; record this extension in the dated ADR note as a
>   design-channel ruling ratified by dispatch); the `.claude/skills/`
>   mirror (indexed via `.agents/skills/README.md` — the drift gate —
>   indexing copies twice adds nothing). Directories whose README
>   format doesn't support clean token extraction get the format
>   adjusted, not the gate weakened — but keep each README one screen;
>   if listing every file breaks that, an explicit "all files of
>   pattern X" clause in the README that the test understands is
>   acceptable (e.g., session-records: dated files matching the naming
>   convention are index-exempt if the README states the convention and
>   the test enforces the convention instead — choose per directory,
>   document choices). Red→green with both a missing-index seed and a
>   ghost-entry seed.
> * AR-2 (item 3a) Sim citation stubs. Survey pass: grep live surfaces
>   (`docs/`, `AGENTS.md`, `README.md`, `notes/ADRs.md`,
>   `notes/facts-register.md`, component docstrings) for claims that
>   restate facts recorded in the frozen `notes/sim/facts-register.md`
>   or decisions in `notes/sim/ADRs.md` without citing them. Add
>   `(sim/F-N)` / `(sim/ADR-N)` citations at each hit — citation added,
>   prose unchanged (this is attribution, not editing). Two
>   disciplines: (i) one-to-one accounting — the session record lists
>   every frozen row and whether a live restatement was found (cited
>   now), not found (fine), or found contradicting the frozen row
>   (escalate, don't reconcile silently: frozen provenance vs. live
>   claim conflicts are author calls); (ii) do not manufacture
>   restatements — a frozen row with no live echo needs nothing. Then
>   one index stub in the live `notes/facts-register.md`: a short
>   "sim-origin facts" pointer row noting the frozen register's
>   existence and citation convention, so the live register
>   self-describes the two-file topology ruling 2(a) chose.
> * AR-3 Records. Report rows 10/3(a) executed; roadmap rows to
>   done-with-sha; dated ADR note (the completeness-exemption extension
>   lands here).
> * AR-4 Fence. No reading sets (8), no skill distillation (5 — still
>   gated on the author's item-9 discovery probe), no use-cases work
>   (14).
>
> Checkpoints
>
> * C1 — AR-1: `fix: index-completeness gate over .agents and notes --
>   both directions, exemptions as cited data; indexes are now
>   checked, not trusted (migration item 10)`
> * C2 — AR-2 + AR-3: `docs: sim citation-stubs pass -- live
>   restatements of frozen sim rows now cite their provenance,
>   one-to-one accounted (migration item 3a); roadmap and report
>   annotated`
> * C3 — ritual: `docs: session record and prompt archive -- migration
>   session 3`
>
> Close-out
> Session record is the close-out; chat echo three lines. Record: HEAD,
> shas, the per-directory index-format choices, both red→green proofs,
> the full 3(a) accounting table, any contradictions escalated,
> post-push verification results.

## Deviation record

- **Scope bound on `components/sim/src` docstring fixes**, not stated
  literally in the prompt at this granularity — the prompt asked for a
  "survey pass" and "citations at each hit," which turned out, once the
  survey actually ran, to surface a MUCH larger pre-existing pattern
  (bare, mis-qualified `ADR-NNNN` references throughout
  `components/sim/src`, 40+ in `engine.clj` alone) than a single
  citation-stub session could exhaustively fix. Fixed the survey's own
  flagged instances plus every bare reference sharing a docstring block
  with one of them; left the rest as a named, disclosed future-work
  item on the roadmap rather than either silently completing an
  unbounded sweep or silently leaving the scale finding unmentioned.
  Judgment call, not separately ratified mid-session (no author present
  to ask); full reasoning in the session record's own "Judgment calls"
  section.
- **Four miscitations found and corrected** (`order_profiles.clj` F7,
  `persona.clj` F8, `emit_hl7.clj` F9, `engine.clj` ADR-0009 — each
  pointed at the wrong, live-numbered file instead of the frozen sim
  one) were treated as fix-forward corrections, not escalation-worthy
  contradictions — AR-2(i)'s escalate clause is for frozen-vs-live
  *content* conflicts, and these are citation-pointer bugs, a different
  species. Disclosed distinctly in the session record rather than
  folded into the "citations added" tally.
- No other deviations. Both author rulings (AR-1, AR-2) executed as
  stated, records updated per AR-3; AR-4's fence held (items 5, 8, 14
  all untouched).

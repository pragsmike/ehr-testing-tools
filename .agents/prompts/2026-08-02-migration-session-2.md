# 2026-08-02 — Migration session 2 prompt

Repo: `ehr-testing-tools`. Clone: WSL ext4 (`~/src/ehr-testing-tools`),
fast-forwarded to `origin/main` (`1dd98f8`) before work began; `/mnt/c`
untouched this session. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed to the WSL ext4 clone per this repo's own
[[feedback-wsl-git-workflow]] and [[feedback-dual-clone-edit-hazard]]
conventions.

## Prompt, verbatim

> 2026-08-02 — ehr-testing-tools: migration session 2 — archives seal,
> indexes land
>
> Context
> Second build session of the approved migration (report + rulings
> block: `.agents/plans/2026-08-01-migration-report.md`). Executes
> items 1+12 (the `notes/prompts/` tombstone is ratified — seal it with
> a gate) and 4+11 (the `notes/` index and the README-presence gate;
> frozen dirs exempt per ruling 6). Items 10, 5, 8, 3(a), 14 remain
> fenced for later sessions. Standing ceremony; WSL ext4 clone;
> fast-forward to `origin/main`, record HEAD; `/mnt/c` untouched.
> Roadmap rows for these items are updated in the same commits as the
> work — the roadmap's own header rule, first exercised here.
>
> Author rulings
>
> * AR-1 (items 1+12) Seal the frozen archive. `notes/prompts/` is
>   frozen by ruling 1: (a) pin its exact tracked file list in a
>   per-push test (freshness-gate pattern — any addition, removal, or
>   rename trips it; the README's own text may need a carve-out if it's
>   the one file allowed to change, decide and document); (b) sweep any
>   current-tense instruction still steering archives to
>   `notes/prompts/` (check `docs/dev/way-of-working.md` and
>   AUTHORS-GUIDE particularly — the capture session updated AGENTS.md
>   but may not have caught these) → they point at `.agents/prompts/`;
>   historical narrative stays; (c) the stale-path tripwire family gains
>   `notes/prompts/` as a forbidden destination in current-tense prose
>   (link-target nuance as before: citations of archived files by path
>   remain legal — only "archive to notes/prompts" instruction is
>   forbidden; encode that distinction, prove both directions
>   red→green).
> * AR-2 (item 4) The notes index. `notes/README.md`: one screen. What
>   each file/dir is; zone marking (current-truth registers vs. frozen
>   provenance vs. historical audits); explicit statement that open
>   work lives in `.agents/plans/roadmap.md`, not here — the index
>   points, it does not duplicate (charter R-C). The 2026-07-30 review
>   gets an entry naming it the origin of the current refactoring arc
>   (it earned it).
> * AR-3 (item 11) README-presence gate. Per-push test: every
>   subdirectory of `.agents/` and `notes/` carries a `README.md` —
>   except `notes/sim/` and `notes/tools/` (ruling 6, exemption list
>   lives in the test as data with the ruling cited). Create any
>   missing READMEs first (survey; `.agents/skills/` itself likely
>   lacks a top-level one — it should list the skills one line each and
>   note the `.claude/skills/` mirror + drift gate). Exact-token/
>   existence discipline per the gate-hardening precedent; red→green
>   with a seeded violation.
> * AR-4 Records. Report rows 1/12/4/11 marked executed; roadmap rows
>   moved to done-with-sha same-commit; dated ADR note (append to
>   ADR-0023's thread).
> * AR-5 Fence. No index-completeness gate yet (item 10 needs these
>   landed first); no register stubs (3a); no reading sets (8); no
>   skill distillation (5); no use-cases work (14).
>
> Checkpoints
>
> * C1 — AR-1: `fix: notes/prompts sealed -- file list pinned per-push,
>   archive instruction repointed, tripwire forbids the retired
>   destination (migration items 1+12)`
> * C2 — AR-2 + AR-3 + AR-4: `docs: notes/ index lands, README-presence
>   gate over .agents and notes (frozen dirs exempt) -- migration items
>   4+11; roadmap and report annotated`
> * C3 — ritual: session record + prompt archive + indexes: `docs:
>   session record and prompt archive -- migration session 2`
>
> Close-out
> Session record is the close-out; chat echo three lines. Record: HEAD
> at start, per-checkpoint shas, the sweep hits from AR-1(b), the
> README survey (which were missing), all red→green evidence,
> post-push message verification results.

## Deviation record

- **Roadmap sha-citation timing.** The roadmap's own header rule says
  rows update "in the same commit as work that changes a row," but a
  Done-section row citing its own commit's sha cannot literally be
  authored inside that commit (the sha isn't known until the commit
  exists). Verified against session 1's own real git history
  (`1dd98f8`'s diff) before proceeding, rather than assuming: the same
  situation arose there, resolved by filling the sha in during the
  session's own final ceremony commit (C3), not by amending or
  guessing. Followed the identical pattern here — item 1+12's row got
  its (already-known, from C1) sha directly in C2; item 4+11's row got
  a placeholder in C2, filled in with C2's real sha (`ab9fe5e`) by this
  same C3 commit.
- **Item 12's exact tripwire mechanism was a design call this prompt
  named at the "what" level (forbid current-tense instruction,
  distinguish it from legitimate citation) but not the "how" level**
  (which verb forms, which scan scope). Designed as a present-tense/
  imperative verb-phrase match over a scan scope widened per the
  migration report's own item-12 text (`AGENTS.md` + every skill's
  `SKILL.md` join the `docs/` scan for this one check). Not separately
  ratified mid-session (no author present to ask); recorded as a
  judgment call in this session's own record rather than presented as
  the prompt's literal instruction.
- No other deviations. All four author rulings (AR-1 through AR-4)
  executed as stated; AR-5's fence held (items 3(a), 5, 8, 10, 14 all
  untouched).

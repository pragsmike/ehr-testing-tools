# 2026-08-02 — Provenance-adoption rider session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention. Both clones (`/mnt/c` and `~/src/ehr-testing-tools`) were
already at `origin/main`, `a80e73a`, at session start — confirmed via
`git fetch` against the ext4 clone before any edit, no fast-forward
needed.

## Prompt, verbatim

> # 2026-08-02 — ehr-testing-tools: adopt the [A]/[C] provenance convention (rider session)
>
> ## Context
>
> Conventions read at HEAD `a80e73a`. Enacts the author's ruling on the finding
> session 5 escalated (ADR-0007's unused ruling-provenance tags). Run under the
> **build-session** skill. Note the standing pre-existing red: `structure-currency-test`
> trips on the untracked empty `bases/sim-cli/resources/sim-cli/` until the author
> removes it — proceed past that one failure only, as sessions 6's precedent, and
> say so in the record.
>
> ## Author rulings
>
> - [A] **Adopt.** The `[A]`/`[C]` convention is active law, effective from the
>   item-14 prompt onward (which ran tagged as the demonstration).
> - [C] **Enactment mechanics**: (i) dated note on ADR-0007 resolving the
>   session-5 finding — adopted 2026-08-02, with the escalation semantics stated:
>   a `[C]` line conflicting with the tree is a default to renegotiate; an `[A]`
>   line conflicting with the tree always escalates, since author intent vs.
>   reality is never the executor's call; (ii) the **session-prompt** skill gains
>   tagging as a required step in its anatomy section, with those semantics and a
>   one-line pointer to the R-1 incident as the motivating case
>   (`notes/2026-07-30-refactoring-review.md` header records it); mirror to
>   `.claude/skills/` same commit (drift gate enforces); (iii) resolve
>   `way-of-working.md`'s open divergence note for this finding with the ruling
>   and date; (iv) roadmap: if the finding has a row, done-with-sha — if not, no
>   row is manufactured.
> - [C] **Fence**: nothing else — not the stray-directory cleanup (author-only),
>   no budget changes beyond any measured raise the skill edit forces.
>
> ## Checkpoints
>
> - **C1** — enactment:
>   `docs: [A]/[C] ruling-provenance convention adopted (ruled 2026-08-02) -- ADR-0007 finding resolved, session-prompt skill requires tags, escalation semantics recorded`
> - **C2** — ritual per build-session:
>   `docs: session record and prompt archive -- provenance-adoption rider`
>
> ## Close-out
>
> Per build-session. Record: the amendment texts landed, any budget delta, and
> confirmation the mirror stayed identical.

## Deviation record

- **Expected pre-existing red (`structure-currency-test` tripping on
  `bases/sim-cli/resources/sim-cli/`) was not present.** The directory
  no longer exists on either clone — removed by the author between
  migration session 6's close and this session's start, outside any
  recorded session. Full `docs-tooling` brick ran clean both before and
  after this session's edits, no failure to proceed past. See the
  paired session record's Findings section.
- **Roadmap fence (ruling iv) applied as written.** No row in
  `.agents/plans/roadmap.md`'s "Now"/"Next"/"Deferred" sections named
  this finding — confirmed by reading the file before editing anything
  — so no row was added or closed.
- **C2's own budget delta, caught by the gate itself before commit.**
  Indexing this session's record/prompt filenames in their own READMEs
  grew `:onboarding`'s real line count from 715 to 717 (same pattern
  migration sessions 5 and 6 both hit at their own C3). Fixed forward
  in the same commit, not amended into C1 — see the paired session
  record.
- No other deviations. Both checkpoints executed as stated; the fence
  held (no stray-directory cleanup attempted; no reading-set budget
  touched beyond that one gate-caught correction).

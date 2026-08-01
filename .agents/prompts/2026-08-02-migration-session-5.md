# 2026-08-02 — Migration session 5 prompt

Repo: `ehr-testing-tools`. Clone: WSL ext4 (`~/src/ehr-testing-tools`),
already at `origin/main` (`6e7b277`, matching the prior session's own
HEAD) at session start — no fast-forward needed. `/mnt/c` untouched
this session (it remains behind, at `1dd98f8`; its own fast-forward is
a separate, still-open AUTHOR ACTION — see
`.agents/plans/roadmap.md`'s Externals section). Ran as a Claude Code
session against the native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed to the WSL ext4 clone and all file edits routed
through its UNC path, per this repo's own WSL-only-git and
dual-clone-hazard conventions.

Session context received: the author's own fresh-session probe had
already confirmed `.claude/skills/`'s mirror-with-gate reading works
(`wsl-windows-git-hygiene` visible, description intact) — item 9's
acceptance test passed 2026-08-02, ahead of this session's own start.

## Prompt, verbatim

> 2026-08-02 — ehr-testing-tools: migration session 5 — the disciplines become skills
>
> Context
> Fifth build session. The author's fresh-session probe confirmed the
> `.claude/skills/` mirror loads (`wsl-windows-git-hygiene` visible,
> description intact) — item 9's acceptance test passed 2026-08-02;
> close its roadmap row citing that confirmation. This session executes
> item 5: distill the session disciplines — proven across the
> 2026-07-30..08-02 arc and currently spread over `way-of-working.md`,
> `AUTHORS-GUIDE.md`, the amended ADR-0007, and eleven session archives
> — into five repo-local skills that future sessions load on demand
> instead of having the design channel re-encode them in every prompt.
> Item 14 remains fenced. Standing ceremony; WSL ext4 clone;
> fast-forward, record HEAD; `/mnt/c` untouched; roadmap same-commit.
> Note what carries this session: the gates now do the bookkeeping. New
> skills must appear in `.agents/skills/README.md` (index-completeness),
> carry READMEs (presence), and mirror to `.claude/skills/` in the same
> commit (drift gate) — the session doesn't remember these rules, the
> lane enforces them.
>
> Author rulings
>
> * AR-1 The five skills, each in `.agents/skills/<name>/` (SKILL.md ≤
>   ~150 lines, `compatibility:` field per the adapted convention,
>   depth in `references/` if needed — skills are context payload,
>   budget-minded by construction):
>    1. build-session — the standing shape: fast-forward + record HEAD;
>       checkpoints; the full ceremony (staged-scope `--stat` match,
>       personal-info scan, message-via-file, push, post-push message
>       verification); red→green discipline for every gate touched;
>       escalation-on-conflict with the one-question-then-proceed
>       cadence; deviation records; session record before final push;
>       prompt archive; roadmap rows same-commit.
>    2. capture-session — chat rulings → ADR/doc law: fence discipline,
>       dated amendments over rewrites, the ratified-by-dispatch rider
>       mechanism, the "record what this session deliberately does NOT
>       do" convention.
>    3. extraction-stage — characterize → extract → verify → records:
>       baselines and byte-identity fences, one-to-one count accounting,
>       `:necessary` re-derivation, move-don't-improve with
>       named-futures.
>    4. errata-sweep — fix-forward mechanics: the citation-vs-instruction
>       distinction, accounting tables, contradiction-escalation,
>       tripwire co-landing so the species can't recur.
>    5. session-prompt — the design channel's own preflight (charter
>       R-B): read AGENTS.md's head, the `.agents/` indexes, and current
>       mode rulings at a stated HEAD before authoring; the canonical
>       prompt anatomy (context, read-first, rulings, checkpoints,
>       close-out, fence); provenance citation for every "ruled" claim.
> * AR-2 Distill practice, cite sources, flag divergence. Every skill
>   cites its provenance (ADR-0007 as amended, AUTHORS-GUIDE §§,
>   way-of-working §§, specific session archives for patterns that
>   evolved in practice). Where evolved practice diverges from the
>   written narrative, the skill encodes practice and the divergence
>   gets a dated amendment note in the narrative doc same session —
>   unless the contradiction is substantive, in which case escalate.
>   Skills never duplicate the narrative; `way-of-working.md` gains a
>   short pointer block ("operationally encoded in
>   `.agents/skills/...`") and keeps its prose role.
> * AR-3 Reading sets. Add each skill to the task-class sets where it
>   belongs (build-session likely everywhere; session-prompt to a
>   design-channel note in `:onboarding`'s comments rather than a class
>   set — judge it). Budget raises this forces are legitimate and
>   conscious: raise `:budget-lines` to the new measured actuals with a
>   one-line justification comment per raise; record old→new numbers in
>   the session record and the ADR note (the author's future tightening
>   ruling needs the delta visible).
> * AR-4 Records. Report row 5 executed — and with it the migration
>   report's last open item other than 14: annotate the report's header
>   accordingly. Roadmap: item 5 and item 9's probe row to
>   done-with-sha/confirmation.
> * AR-5 Fence. No use-cases work (14); no budget tightening beyond
>   AR-3's measured raises; no rewriting way-of-working beyond the
>   pointer block and any AR-2 divergence notes.
>
> Checkpoints
>
> * C1 — AR-1 (skills + mirror + indexes, the gates enforcing all
>   three): `feat: session disciplines distilled into five repo-local
>   skills -- build-session, capture-session, extraction-stage,
>   errata-sweep, session-prompt (migration item 5)`
> * C2 — AR-2 pointers/divergence notes + AR-3 + AR-4: `docs:
>   way-of-working points at its operational encoding; reading sets
>   absorb the skills with measured budget raises; item 9 probe
>   confirmed and closed`
> * C3 — ritual: `docs: session record and prompt archive -- migration
>   session 5`
>
> Close-out
> Session record is the close-out; chat echo three lines. Record: HEAD,
> shas, each skill's line count and provenance list, any
> practice-vs-narrative divergences and their disposition, the budget
> old→new table, post-push verification.

## Deviation record

- **`session-prompt` names the fifth skill; the migration report's own
  item-5 text still says "four new skills."** Directly authorized by
  this prompt's own AR-1 (which lists five, `session-prompt` among
  them) — not a deviation from the prompt, but a divergence from the
  migration report's own, now-stale scope statement, disclosed in the
  report's own new "RULED 2026-08-02" annotation rather than silently
  left to contradict what actually landed.
- **AR-3's "judge it" for `capture-session`/`extraction-stage`/
  `errata-sweep`.** The prompt named `build-session` and `session-prompt`
  explicitly but left the other three's reading-set placement to this
  session's own judgment. Resolved by extending `reading-sets.edn`'s
  own already-stated reasoning for excluding the other ten skills
  (session-mechanics/meta, not domain-specific) — not author-ratified
  as the right call, disclosed in the session record's own judgment-calls
  section.
- **AR-4's "item 9's probe row to done-with-... confirmation" read as
  closing the migration report's own item 9, not only the roadmap
  line.** A plausible reading, not verbatim — see the session record.
- **The `[A]`/`[C]` provenance-tag divergence (ADR-0007), found while
  writing `session-prompt`, was not anticipated by this prompt** and is
  the session's own most substantive finding: escalated per AR-2's own
  instruction rather than resolved either direction. Full detail: the
  session record and `notes/ADRs.md` ADR-0007's own third dated
  amendment.
- No other deviations. All five author rulings executed as stated;
  AR-5's fence held (no use-cases work, no budget tightening beyond the
  measured raises AR-3 itself required, way-of-working.md touched only
  for the pointer block and the one AR-2 divergence note).

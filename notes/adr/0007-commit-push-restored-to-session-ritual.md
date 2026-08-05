<!-- Attic file: notes/adr/0007-commit-push-restored-to-session-ritual.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0007 — Commit/push restored to session ritual; ruling provenance tags adopted

**Status:** Accepted (author-directed), 2026-07-29.

### Context

`AGENTS.md` and `AUTHORS-GUIDE.md` §1 both state, as ADR-0001 R6: git
commit/push/merge/`gh` are the author's ceremony; a session prepares
the working tree and proposes commit messages but does not itself
commit or push, absent an explicit, in-session, in-chat delegation that
does not generalize to future sessions. That rule was itself a
channel-inferred default, not a verbatim author ruling — it encoded a
stale model of `ehr-testing-sim`'s own 40-session practice (an async,
multi-session culture where an out-of-band author review gated every
push) carried into this workspace's bootstrap prompt dressed as if it
were an author ruling, and it went unexamined by the discipline-parity
audit (ADR-0006) for exactly that reason: the audit inventoried
mechanisms against both parents' final states, and a rule that never
existed in either parent's own terms in the form R6 stated it has no
parent-side row to diff against.

The author's actual practice this session (2026-07-29, development
resumption: kernel/judge extraction, the `ehrt` rename, audience-forked
docs) is to commit and push at each checkpoint, unattended, watching
progress land on the remote — the opposite of R6's default. This
record supersedes R6 in place (ADR-0001 is not reverted; R6's own text
stands as the historical record of what was ruled and why it was
wrong) and adopts a provenance-tag convention so a future audit can
tell, without re-deriving it from a close prose reading, which rulings
came from the author directly and which are this workspace's own
inferred defaults.

### Decision

**R30** (supersedes R6). Committing at checkpoints, and pushing at
each checkpoint, are part of the session ritual for sessions the
author has told, explicitly, in that session's own chat, to operate
this way — the same scoping R6 always had for its one-off delegations,
now the *standing* mode rather than the exception, until a future
ruling changes it again. The staging-hygiene ritual (`AUTHORS-GUIDE.md`
§1, "Staging hygiene between checkpoints") is unchanged and still
applies before every commit, delegated or not: `git diff --cached
--stat` recorded, anything outside the checkpoint's own stated scope
unstaged first. Two classes of action stay the author's alone,
unaffected by R30: **tags** (ADR-0003's own trust boundary — the
`stable-*` tag, not the push, is what CI and a future clone actually
trust) and **repo-level `gh` mutations** — create/delete/settings/
visibility (the `pragsmike/packs` precedent, `AGENTS.md`'s own
citation of it, correctly scoped and left as-is here).

**Provenance tags, adopted this record forward.** Every author-ruling
list in a session prompt or ADR from this point on marks each ruling
`[A]` (author-ruled, verbatim or a direct paraphrase the author would
recognize as their own) or `[C]` (channel-inferred: a default this
workspace's own tooling or a prior session supplied, reasonable but
not something the author said in so many words, and vetoable post-hoc
without it counting as reverting an Accepted decision). R6 itself,
read again with this distinction available, was a `[C]` ruling
wearing `[A]`'s clothing — the tag exists so that mistake doesn't
recur silently. Tags are provenance metadata, not a quality signal:
a `[C]` ruling is not weaker or more provisional than an `[A]` one
once accepted; it is only *more revisable* by a later author veto
without that veto needing to clear the "supersede, don't revert" bar
this file otherwise holds every Accepted decision to.

### Deviation record

None — this record is itself the first act taken under R30 (its own
commit is also its own push), so there is nothing yet to disclose
about R30's application beyond this record's own existence.

### Amendments (2026-08-01, agent-ux capture session — fix-forward, dated, not a revert)

- **R-F ratified: R30-mode becomes the standing ceremony default.**
  The agent-UX charter (`.agents/plans/2026-08-01-agent-ux-charter.md`
  §3, R-F) proposed and the author ruled: commit-and-push-at-each-
  checkpoint, unattended, is now the default a session runs under
  absent contrary instruction; "prepare-only" (R6's own shape) becomes
  the exception a session's own prompt must state explicitly — the
  inverse of R30's original scoping above ("a session the author has
  told, explicitly, in that session's own chat, to operate this way").
  R6's and R30's own text above stand unedited; this is a dated
  amendment, not a rewrite of either. The ceremony's codified
  safeguards, drawn from this week's actual practice rather than
  invented fresh: staged scope must match the session's own file list
  (`git diff --cached --stat`, reviewed before every commit, per
  `AUTHORS-GUIDE.md` §1's existing "Staging hygiene" ritual); a
  personal-info/secrets scan of staged content before each commit
  (gitleaks already gates this at push per ADR-0003 — the scan here is
  the same discipline applied earlier, at stage time, not a new gate);
  the commit message written to a file, never inlined through the WSL
  wrapper (the heredoc hazard `AUTHORS-GUIDE.md` §1 already names); the
  session record (`.agents/session-records/`) written before the
  session's own final push, not after; hooks remain the backstop, not
  the only defense. **AUTHOR ACTION checkpoints stay author-only in
  every mode** — R30 was never a grant over tags or repo-level `gh`
  mutations, and this amendment doesn't extend it into one.
  `AUTHORS-GUIDE.md` §1 and `docs/dev/way-of-working.md` §1 carry
  matching dated notes; `AGENTS.md`'s own restatement of this rule is
  updated directly as part of this same capture session's AGENTS.md
  restructure (current-tense operational text, not a historical record
  like this file — no separate amendment note needed there).

### Amendments (2026-08-01, skill-adaptation session — fix-forward, dated, not a revert)

- **Ceremony gains post-push message verification.** Motivated by the
  C3 quoting hazard (`feedback_wsl_dollar_exitcode_quoting` /
  `feedback_wsl_wrapper_raw_byte_patterns`-class failures: the WSL
  wrapper has, in past sessions, silently mangled backticked literals
  or raw control bytes out of a commit message on its way from a
  message file through to the pushed commit). The ceremony's existing
  safeguards catch scope and secrets before a commit; nothing
  previously checked that what actually landed on the remote matches
  what was intended, after the fact. Added: **after every push, run
  `git log --format=%B -1` against the pushed commit and diff it
  against the message file that produced it.** A clean match needs no
  record. A mangled message is never fixed by amending a pushed
  commit — it gets a fix-forward note in that session's own
  `.agents/session-records/` entry (per R10, `notes/ADRs.md`'s
  standing fix-forward discipline) naming what the wrapper dropped and
  whether a corrective commit was made. This is a checkpoint-time
  check like the rest of R30's safeguards, not a new gate a hook
  enforces — `AUTHORS-GUIDE.md` §1 and `docs/dev/way-of-working.md` §1
  carry matching dated notes.

### Amendment (2026-08-02, migration session 5 — fix-forward, dated, not a revert)

- **The `[A]`/`[C]` provenance-tag convention (this record's own
  "Decision" section above) has gone unused since it was adopted.**
  Distilling this record into `.agents/skills/session-prompt/SKILL.md`
  (migration item 5) required checking real practice against the
  written rule, and the check found that none of the five session
  prompts written since this convention's adoption
  (`.agents/prompts/2026-08-01-migration-session-1.md` through this
  session's own prompt) tag any Author-ruling `[A]` or `[C]`. Not
  silently fixed by retroactively tagging those five prompts (which
  would fabricate a provenance judgment after the fact — the opposite
  of what the tags are for) or by quietly dropping the convention from
  the new skill (which would let the lapse read as if it had been
  ratified). The skill documents the convention as this record states
  it; the lapse itself is named here and in
  `docs/dev/way-of-working.md`'s own matching dated note, for the
  author to rule on.

### Amendment (2026-08-02, provenance-adoption rider session — fix-forward, dated, not a revert)

- **Ruled `[A]` (ADR-0007 provenance tag): adopted, not retired, not
  retroactively applied.** The `[A]`/`[C]` convention is active law,
  effective from the item-14 prompt onward
  (`.agents/prompts/2026-08-02-migration-session-6.md`, which ran
  tagged as the demonstration). The five prompts written before that
  one stay untagged — migration session 5's own amendment above already
  declined to fabricate the judgment after the fact, and this ruling
  doesn't reopen that choice.
- **Escalation semantics, stated for the first time.** A `[C]`-tagged
  ruling conflicting with the live tree is a default the executing
  session may fix-forward and reconcile without stopping — the
  precedent is the refactoring review's own vocabulary reconciliation
  (`notes/2026-07-30-refactoring-review.md`'s header, "Vocabulary
  reconciliation (R-1, applied via R-7)"), where a review brief's own
  unverified phrasing was corrected against live code rather than
  escalated back to the author. An `[A]`-tagged ruling conflicting with
  the live tree always escalates instead, unconditionally — whether the
  author's stated intent or the tree's current state is the thing to
  trust is never the executing session's call to make silently.
- **Enactment mechanics.** `.agents/skills/session-prompt/SKILL.md`
  (mirrored to `.claude/skills/session-prompt/SKILL.md`, skill-mirror-
  currency gate covers it) gains tagging as a required step of its
  Author-rulings anatomy — not an optional when-it-matters annotation as
  it read before this ruling — carrying this escalation semantics and a
  one-line pointer to the R-1 incident above as the motivating case;
  `docs/dev/way-of-working.md`'s own divergence note (§6) is resolved
  with this ruling and its date. No `.agents/plans/roadmap.md` row is
  touched — the finding never had one to close.

---


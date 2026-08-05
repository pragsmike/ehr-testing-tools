<!-- Attic file: notes/adr/0003-pre-push-gate-doctrine.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0003 — Pre-push gate doctrine: irreversibility-only

**Status:** Accepted (author-directed), 2026-07-28. **Written into its
own reserved slot** by the discipline-parity session, landing
chronologically after ADR-0004 and ADR-0005 — ADR-0004's own numbering
note already recorded why: the pending closeout-sweep session that owned
this record stopped at its own step-0 precondition (CI red) before
writing it, and no ADR number is ever reassigned once used, so ADR-0004
took the next number rather than block on this slot. This record is
that slot, filled in, not renumbered.

### Context

`.githooks/pre-push` originally ran `clojure -M:poly test :project`
alongside `gitleaks detect` and `clojure -M:poly check` (ADR-0001's own
R7, `61a1573`). The author removed it directly, terse commit message,
no session prompt: `1ebf4ce "Don't run tests on pre-push."`. This record
is the doctrine that commit was acting on, written down after the fact
so a future session doesn't reintroduce a test gate at push time without
understanding why it was removed.

**Honest evidence note.** The session prompt that requested this ADR
named "the connection-close incident" as the motivating event — a
long-running `poly test :project` invocation inside the push hook
plausibly timing out or dropping an SSH/terminal connection mid-push.
This workspace's own tree carries no record of that specific incident
beyond the terse commit message itself; per this workspace's
fix-forward-with-disclosure rule, this ADR states the doctrine the
author has directed and cites the one piece of repo evidence that
exists, rather than fabricating incident detail nothing in the tree
supports.

### Decision

**The pre-push hook gates on irreversibility, not correctness.** Three
checks, all fail-closed:

1. **WSL provenance** (`.githooks/pre-commit` and `.githooks/pre-push`
   both check `$WSL_DISTRO_NAME`/`/proc/version`) — a commit or push
   from native Windows is a mixed-platform-git mistake that's cheap to
   prevent and expensive to unwind (line-ending wars, executable-bit
   flips) once it's in history.
2. **`gitleaks detect`, fail-closed** — a secret pushed to a public
   remote is irreversible the instant it leaves the clone, regardless of
   whether it's reverted a commit later; history is treated as public
   from the moment of push (the same posture tools' own AUTHORS-GUIDE
   took once it went public).
3. **`clojure -M:poly check`, fail-closed** — a dependency-direction or
   interface violation compounds the longer it survives in shared
   history; catching it before it leaves the clone is cheap, catching it
   after is a revert.

**Tests are deliberately not in this list.** A failing test is
*reversible* — CI catches it on the very next push, the failure is
visible, and fixing it costs one more commit. It shares none of the
three properties above (public-secret exposure, cross-repo build
breakage, un-revertable history) that justify blocking a push
synchronously, in a hook a human is sitting in front of, rather than
async in CI. Running the full suite (or even `:project` alone, which by
this same day's own empirical findings, ADR-0004, pulls in every
artifact-fetch-dependent integration test unless structurally excluded)
inside a push hook trades a human's real time for a check that doesn't
need to happen synchronously at all.

**The trust boundary is the `stable-*` tag, not the push.** Ordinary
pushes to `main` may be red between commits — CI reports it, nothing
blocks it. A `stable-*` tag (`stable-bootstrap`, `stable-pre-monorepo`,
and this workspace's own future ones) is the point something is
asserted trustworthy, and that assertion rests on CI having run green at
that exact commit (`notes/ADRs.md` ADR-0001 H6, ADR-0002's own
verification sections) — not on the pre-push hook, which never ran the
suite at all by the time either tag was cut this session's own
lineage runs through.

### Consequence

`AGENTS.md`'s pre-push description (`4ed3ffa`, pre-dating this ADR)
already matches this doctrine in practice; this record is why, not a
change in mechanism. Any future session tempted to add a test gate back
into `.githooks/pre-push` should read this ADR first — the removal was
deliberate doctrine, not an oversight to quietly fix.

---


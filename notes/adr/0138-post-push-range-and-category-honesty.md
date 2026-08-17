## ADR-0138 — The push-verifier learns what a push actually carried; `gate` learns to say what actually went wrong

**Status:** Accepted — 2026-08-15

### Context

Repo review 3's third fix session (`.agents/plans/2026-08-15-repo-review-3-plan.md`
Session C), on the arc ruling *"accept all."* (2026-08-15). Two
ceremony-correctness rows and one `src` ride-along the channel batched
here so Session D stays docs-only.

**D1-6 — `bin/post-push-verify` was wrong by construction.** Its header
promised the `<base-sha>` default was *"the sha that was origin's tip
immediately before your push."* Its usage text said something else
outright — *"defaults to the tip's own first parent (a one-commit
range)"* — and its code implemented the usage text
(`base_sha="$(git rev-parse --verify "${tip_sha}^1")"`). The script
therefore contradicted **itself**, not just its contract, and the half
that was true was the wrong half.

The consequence is silent under-coverage. The stated population is *the
pushed range*; the enumeration ran off the commit graph's parent link,
which cannot know what was pushed. Three live sightings this arc:
ADR-0135's push spanned 4 commits (1 checked), Session A's spanned 3 (1
checked), Session B's spanned 1 (correct only by coincidence). In the
first two, every message but the tip's went unchecked while the script
printed `OK: every commit message in range is pure ASCII`. The ADR-0135
session caught the gap by hand; that was diligence compensating for a
wrong tool, and it does not recur reliably.

This is the fifth recorded instance of the population class the
review's own amendment names (D1-2, D1-6, D5-4, D7-4).

**D2-6 — the exit-code-masking class had no gate anywhere.** H-2 probed
every tracked file for a gate command piped into `tail`/`head`/`grep`
and found **zero hits**: the defect lives in ad-hoc session practice,
not in the tree. But session practice in this repo *is* gated — staging
hygiene, message-via-file, ASCII commit messages are all laws written
into `build-session` — and this one was not.

**D4-3 — sibling commands disagreed about one condition.** `ehrt show`
on an unreadable-but-present file reports `:path-unreadable`. `ehrt
gate` on the *same* file reported `:file-not-found`. Loud either way, so
never a silent-success defect — but a stranger diagnosing a permissions
problem is told the file does not exist, which points them at the wrong
fix.

### Decision

**D1-6.** Derive the default base from the remote-tracking ref's own
*previous* value — `origin/<branch>@{1}` — validated as an ancestor of
the tip and not equal to it; fall back to `git merge-base <tip>
origin/<branch>` when that ref has no usable reflog; **fail loudly**
with the existing *"pass one explicitly"* message when neither is
derivable, rather than quietly narrowing the range. Header and usage
text both rewritten to describe the implemented behaviour, and the
"one-commit range" sentence deleted.

**D2-6.** One law added to `build-session`'s VERIFICATION section, in
the style of its siblings, with a matching `Done when` checkbox: a gate
run writes its output to a full log file and records its exit code
explicitly (`MAKE_EXIT=$?`), never read through a pipe or a
`tail`/`head` that can swallow the exit code or truncate the countable
signature. Byte-copied to the `.claude/` mirror.

**D4-3.** `gate`'s read path gains the minimum try/catch-around-the-read
routing, returning the same `:path-unreadable` category `show` already
uses, at the CLI seam and nowhere deeper.

### The premise that did not survive the tree

The session prompt's Step 1.2 instructed: *"record `origin/<branch>`'s
SHA BEFORE the fetch (the script currently fetches first — order
matters)."* **That premise does not hold, and the fix does not rest on
it.** Probed directly in a throwaway repo before any code was written:
`git push` **itself** fast-forwards `refs/remotes/origin/<branch>` to
what it just pushed. The script's own `git fetch` is not what advances
the ref — the push already did — so `origin/<branch>` read at *any*
point inside a post-push run is the **post**-push tip, and using it as
the base would yield an empty range: strictly worse than the `tip^1`
defect being fixed.

The pre-push tip is recoverable only from that ref's reflog, which is
the mechanism register row D1-6 names as its own alternative
(*"or the reflog's `origin/<branch>@{1}`"*). So the ruled OUTCOME —
derive from origin's pre-push tip, fail loud when underivable — is
implemented exactly as ruled; only the prompt's stated means of reading
it needed correcting, from a source the register itself supplied. The
before-the-fetch ordering is kept anyway (the derivation runs above
check 1), because it costs nothing and keeps a fetch from shifting the
range underneath the check that is about to run.

Recorded here rather than silently absorbed, per `build-session` step
12 and `docs/dev/way-of-working.md` §2.

### Consequences

`bin/post-push-verify` with no arguments now checks every commit a push
carried, and says so in its range line. A repo whose remote-tracking
reflog is disabled gets a loud failure naming the flag to pass instead
of a quiet one-commit check — the deliberate trade, since the whole
defect was quietness.

The judge engines are **untouched**. ADR-0098's ruled engine-level shape
(one category `:file-not-found`, plus a distinguishing `:reason
:permission-denied` payload key, author ruling Q2 "a.") still holds, and
its three engine tests still assert it. D4-3's defect was at the CLI
surface — which is what the review measured, and what `show` is a
sibling of — so the fix sits at the CLI seam above the engines and
changes no engine contract. A **missing** path is deliberately left
alone: it is not a file, the new check does not fire on it, and the
engine's `:file-not-found` stays the right answer. That half is pinned
by its own test so the fix cannot over-reach.

No `docs/` surface documents either category (grep over `docs/**`:
zero hits for `:file-not-found` / `:path-unreadable`), so no generated
doc needed regenerating and none was touched.

No corpus `src`, no vendored bytes, no module JSON — **no oracle claim
is made or owed**.

### Red/green witnesses

**D1-6, script-level, fix stashed for the red run** (checkpoint
isolation — the red exercised exactly the unfixed script, with the new
test present):

The test builds a throwaway repo with a bare origin, pushes A, then
commits B, C, D with C's message carrying a U+2014 em dash, pushes
B..D, and runs the script with no base argument.

- **RED:** `range 338410e4..55dc4e10`, `OK: every commit message in
  range is pure ASCII`, exit **0** — a one-commit range over a
  three-commit push, the defect exactly as D1-6 describes it. Assertion
  output: `expected: (not (zero? exit)) / actual: (not (not true))`.
  The fixture's own precondition assertion passed in the same run (3
  commits really were pushed), so the red is the script's, not the
  harness's.
- **GREEN:** `Ran 1 tests containing 4 assertions. 0 failures, 0
  errors.`
- **No regression on the explicit-range path:** `bin/post-push-verify
  0027a6e` against the live tree — `range 0027a6e8..7544f7c7`, 4
  commits, ASCII OK, CI reported, exit 0.

**D4-3, CLI-level, fix stashed for the red run:**

- **RED:** `expected: (= :path-unreadable (:category r)) / actual: (not
  (= :path-unreadable :file-not-found))`.
- **GREEN:** both new deftests pass; the missing-path guard test passes
  in both states, as it should.
- **Precondition held:** the session ran as uid 1000, so `chmod 000`
  really did remove read access and the root-skip branch never fired.
  The skip guard is retained anyway, matching ADR-0098's own three
  engine tests.

### Block-count reconciliation

Predicted **before** the run, from the projects' test paths: one new
test namespace (`ehrt.docs-tooling.post-push-verify-range-test`), which
runs in two project contexts (`conformance`, `ehrt-cli`) at two lines
each = **+4**, giving **640**. The two new `gate` deftests join the
existing `ehrt.cli.core-test` namespace, so they raise assertions and
not blocks. Outcome: **640, exactly as predicted** — see the session
record for the full figures.

### Fences

Touched: `bin/post-push-verify`, one new docs-tooling test namespace,
`.agents/skills/build-session/SKILL.md` and its `.claude/` mirror,
`gate`'s read path in `bases/cli/src/ehrt/cli/core.clj` plus two
deftests in its existing test namespace, three register disposition
cells, and the close artifacts. `show`'s code was read as the model and
never edited. The judge engines were read and left alone.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

The push-verifier learns what a push actually carried; `gate` learns to say what actually went wrong — repo review 3's third fix session (`.agents/plans/2026-08-15-repo-review-3-plan.md` Session C), on the arc ruling *"accept all."* `bin/post-push-verify` contradicted **itself**: its header promised the `<base-sha>` default was "the sha that was origin's tip immediately before your push," its usage text said "the tip's own first parent (a one-commit range)," and the code implemented the usage text — so the stated population (*the pushed range*) was enumerated off the commit graph's parent link, which cannot know what was pushed. Three live sightings this arc: ADR-0135's push spanned 4 commits (1 checked), Session A's 3 (1 checked), Session B's 1 (correct only by coincidence); the fifth recorded instance of the population class the review's amendment names (D1-2, D1-6, D5-4, D7-4). The default base is now `origin/<branch>@{1}` — the remote-tracking ref's own pre-push value — validated as an ancestor of the tip and not equal to it, `git merge-base` as fallback, and a LOUD failure carrying the existing "pass one explicitly" message as the floor rather than a quiet one-commit check; header and usage rewritten to agree with each other and with the code. **One prompt premise did not survive the tree and is recorded rather than absorbed:** Step 1.2 instructed reading `origin/<branch>` before the script's own `git fetch`, but `git push` ITSELF fast-forwards that ref, so it reads as the POST-push tip at any point in a post-push run and would have yielded an empty range — strictly worse than the defect being fixed; the reflog mechanism the register row ALSO named is the one that holds, so the ruled outcome landed exactly as ruled from a source the register itself supplied. Co-landed docs-tooling test builds a throwaway repo with a bare origin, pushes A, commits B/C/D with a U+2014 em dash in C's message, pushes B..D, and runs the script with no base argument: RED at `range 338410e4..55dc4e10`, "every commit message in range is pure ASCII", exit 0 — a one-commit range over a three-commit push — GREEN at 4 assertions, with the explicit-range path re-verified unregressed against the live tree. D2-6's exit-code-masking class (H-2 probed the tree and found ZERO hits — the defect lives in session practice, not in any tracked file) becomes a law in `build-session`'s VERIFICATION section with a matching `Done when` checkbox, naming why `cmd | tail -40` reports `tail`'s exit status and how truncation drops the countable signature; `.claude/` mirror byte-identical. D4-3 rides along as this session's single `src` change: `gate` on an unreadable-but-present file said `:file-not-found` while `show` on the SAME file said `:path-unreadable`, so a stranger diagnosing a permissions problem was told the file does not exist — routed at the CLI seam through the same try/catch-around-the-read shape `show` uses, red witnessed (`:file-not-found` where `:path-unreadable` is honest), with the chmod-000 precondition holding live (uid 1000, root-skip branch never fired). **ADR-0098's ruled engine-level shape is deliberately untouched** (one category `:file-not-found` plus a distinguishing `:reason :permission-denied` payload key, author ruling Q2 "a."), its three engine tests still assert it, and a MISSING path still gets `:file-not-found` — pinned by its own test so the fix cannot over-reach. Zero `docs/` surface documents either category, so nothing needed regenerating; zero corpus `src`, zero vendored bytes, zero module JSON — no oracle claim made or owed. Closes review-3 rows D1-6, D2-6, D4-3

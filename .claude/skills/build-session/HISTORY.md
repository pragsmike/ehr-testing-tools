# Build Session — history

Split out of `SKILL.md` on 2026-08-17 by ADR-0145. `SKILL.md` says what to DO;
this file holds the incidents, near-misses, worked examples and provenance chains
that made each step a step. No reading set carries this file, which is the whole
point: a cold session pays for the imperative, not for the story behind it.

Each section below is the corresponding part of `SKILL.md` at `57a27f5`, moved
WHOLE and VERBATIM. The replacement steps are authored rather than excerpted, so
"the history" has no clean line boundary to subtract — the same reason ADR-0144
moved each roadmap row whole rather than trying to cut its overflow out of it.
A step's imperative therefore appears in both files; nothing is summarised and
nothing is dropped, and `bin/verify-skill-history-0145` reads every block back.

## Preamble (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)


Encodes the checkpoint/COMMIT/AUTHOR-ACTION ceremony this workspace runs
build sessions under, distilled from `AGENTS.md` ("Session mode and
ceremony"), `AUTHORS-GUIDE.md` §1, `docs/dev/way-of-working.md`, and
`notes/ADRs.md` ADR-0007 (R6, R30, and its two dated amendments — R-F,
2026-08-01; post-push message verification, 2026-08-01). Those documents
are the narrative; this skill is the operational checklist a session
actually runs. The mechanical parts of that checklist — preflight, tag
ceremony, post-push verification, and the close-phase scaffold — are now
four `bin/` scripts (R13, `notes/ADRs.md` ADR-0127): one definition, in
the script; this skill points at it by name rather than restating its
steps.


## Step 1 — ceremony mode (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

1. **Determine ceremony mode before touching git.** R30 (commit and push
   at each checkpoint, unattended) is the *standing default* since R-F
   (ADR-0007's 2026-08-01 dated amendment) — a session runs under it
   unless its own prompt states, explicitly, at the start, that this
   session is prepare-only (agent stages and proposes messages, never
   itself commits/pushes/merges/`gh`s). Whichever mode applies, it is
   scoped to this session only — the next session starts back at
   whichever mode its own prompt states.

## Step 2 — preflight (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

2. **Preflight: `bin/preflight [--branch BRANCH]`.** Run at session
   start, before touching git. Reports, deterministically, in one call:
   the last five CI runs on the branch (green/PENDING/RED — a
   probabilistic red hides behind any single green, quality riders
   AR-QR-3; a red finding is disclosed before proceeding, never silently
   passed, but watching a run TO CONCLUSION stays reserved for a session
   whose own claim is about CI, AR-CI-4); edit-root confirmation (the
   ext4 clone, never a Windows-mounted `/mnt/*` checkout — the `/mnt/c`
   hazard class retired 2026-08-05, `notes/ADRs.md` ADR-0047 AR-C-3; a
   fresh instance elsewhere is a NEW regression, STOP-AND-REPORT, not
   routine vigilance); tree-clean check counting untracked files too;
   HEAD-vs-remote tip match; the last `stable-*` tag and whether HEAD is
   tagged. Read-only, always exits 0 — a report, not a gate; disclose
   every finding in the session record.

## Step 3 — WSL git (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

3. **All git operations from WSL, never native Windows** —
   `.githooks/pre-commit`/`pre-push` enforce this once `git config
   core.hooksPath .githooks` is set per clone. If working from a
   Windows-launched session, route git through `wsl -e bash -lc "cd
   <repo-path> && <command>"`, one command per invocation — not an
   inline `wsl.exe` call with untrusted interpolation.

## Step 4 — staging hygiene (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

4. **Staging hygiene, before every commit.** Run `git diff --cached
   --stat`, record its output. Anything staged outside the checkpoint
   currently in flight gets unstaged (`git restore --staged <path>`)
   before committing — never folded in silently because it happened to
   already be there (`AUTHORS-GUIDE.md` §1, "Staging hygiene between
   checkpoints", R26e).

## Step 5 — secrets scan (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

5. **Personal-info/secrets scan before each commit** — the same
   discipline the pre-push hook's `gitleaks detect` applies, run earlier
   at stage time (`gitleaks git --staged -v`, or `protect --staged`).

## Step 6 — commit message via file (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

6. **Commit message via file, never an inline heredoc through the WSL
   wrapper.** Nested quoting and backticks have silently mangled
   messages crossing Bash-tool → `wsl.exe` → bash — write the message to
   a plain file with a non-shell tool, then `git commit -F <path>` as
   its own simple call.

## Step 7 — checkpoint isolation (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

7. **Checkpoint isolation, when a checkpoint pairs a src fix with its
   own test.** Polylith's own test runner aborts an entire project's
   run on the FIRST uncaught exception rather than continuing — so a
   red capture against a fix that is already partly staged or already
   applied elsewhere in the working tree is not evidence the FIX is
   what turns red to green, it is evidence of whatever else is in the
   tree too. Isolate: disposable `git stash` the src fix (or the test,
   whichever is smaller) before capturing red, so the red run exercises
   exactly the unfixed code the checkpoint claims to fix; `git stash
   pop` before capturing green. Two independent fixes in the same
   session each get their own isolated stash/red/pop/green cycle, never
   shared evidence (worked example: `.agents/session-records/
   2026-08-06-ux-fixes-2.md`, two independent red captures via
   disposable stash isolation). A small session-record checkpoint
   commit (landing only `.agents/` files) is sanctioned ahead of the
   final `make integration` run whenever that run's own tree-clean
   postcondition would otherwise fail solely because this session's
   own in-progress session-record/prompt-archive files are still
   uncommitted (ADR-0129's own discovered practice, `notes/adr/
   0129-strip-executability.md` Step 3/close).

## Step 8 — red capture (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

8. **Red capture, for every checkpoint that adds or edits an
   enforcement test or fixes a defect a test can name.** Prove the gate
   fails before the fix and passes after — never just assert green. The
   red run's own output (not a paraphrase of it) is what goes in the
   session record; a false positive in the first red pass (a naive
   check catching more than the real target) is itself worth recording,
   not silently filtered (worked example: the rider's gate in
   `.agents/session-records/2026-08-06-ux-fixes-2.md`, whose first red
   pass found 5 failures, one a false positive, before the real 4).

## Step 9 — sweep census (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

9. **Sweep census, when a fix's own scope is "every occurrence of X."**
   An exhaustive grep-based inventory of every site matching the
   pattern being fixed, disclosed with file:line evidence for EVERY
   hit — not just the ones fixed. A hit correctly left untouched (a
   false match, an out-of-fence site, a different meaning at the same
   token) is named and why, in the same table as the hits that were
   fixed — the census proves completeness, not just correctness (worked
   examples: `.agents/session-records/2026-08-12-fix-cluster-a-cli-
   validation.md`'s own F7 four-site sweep census with file:line; the
   `errata-sweep` skill's own inventory step is this same practice at
   full weight, for a session whose entire charter is a sweep rather
   than one checkpoint's own fix).

## Step 10 — post-push verification (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

10. **Push, then verify: `bin/post-push-verify [<base-sha>]
    [<tip-sha>]`.** Run immediately after every `git push`. Confirms the
    remote tip matches local HEAD (or `<tip-sha>`); runs the per-commit
    ASCII check over the pushed range (`<base-sha>..<tip-sha>`,
    AR-RL2-5 — a diff whose only delta is one trailing blank line is
    `git log --format=%B`'s own formatting artifact, not a failure; any
    other mismatch is never fixed by amending a pushed commit, add a
    fix-forward note instead); reports (polls once, never awaits to
    conclusion) the CI run triggered at the tip.

## Step 11 — tag ceremony (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

11. **`stable-*` tags: `bin/tag-ceremony <tag-name> <target-sha>
    <message-file> [--push]`, under license.** A session creates and
    pushes a `stable-*` continuity tag when (i) its own prompt licenses
    a SPECIFIC tag at a SPECIFIC commit, or (ii) for its own
    predecessor's design-channel-verified stable point, as standing
    ceremony — deferring a licensed tag is now the deviation, disclose
    why if you do (tag law, `notes/ADRs.md` ADR-0057 AR-T-1, superseding
    ADR-0003's author-only scope for this one class of tag only;
    ADR-0003's trust-boundary reasoning for every other AUTHOR ACTION
    item below is otherwise unchanged). The license to tag is still the
    session's own judgment call — the script only performs the
    mechanics once licensed: validates the tag name (`stable-YYYYMMDD-
    <slug>`, ASCII-only, the ADR-0120 slug-drift class), creates the
    ANNOTATED tag, pushes only with `--push`, and ALWAYS verifies the
    peeled ref against the remote afterward (the ADR-0124 skipped-tag
    class — the verify half is not optional). A tag already present at
    the exact commit and message is verified and disclosed, never
    re-created. **Release `v*` tags, repo-level `gh` mutations
    (create/delete/settings/visibility), git surgery, and placing
    external documents remain AUTHOR ACTION** — stop and hand these to
    the author regardless of ceremony mode.

## Step 12 — premise mismatch (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

12. **Fix-forward with disclosure on premise mismatch.** When a
    checkpoint's stated premise doesn't hold against the live tree, stop,
    record the finding, and ask — don't silently adapt or guess
    (`docs/dev/way-of-working.md` §2; worked examples: the JDK/Temurin
    premise, the gitleaks-hook premise, both in that document).

## Step 13 — close-phase scaffold (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

13. **Close-phase scaffold: `bin/close-scaffold <YYYY-MM-DD> <slug>
    <one-line description>` (R-A).** Run as the last checkpoint of any
    non-trivial session, before the final push. Scaffolds
    `.agents/session-records/<date>-<slug>.md` and `.agents/prompts/
    <date>-<slug>.md` (creating only whichever is missing) AND both
    directories' own README star-bullet index lines in the same call —
    the index-completeness gate (`ehrt.docs-tooling.index-completeness-
    test`) and the prompt/record pairing gate
    (`ehrt.docs-tooling.prompt-record-pairing-test`) both fail the build
    on a missing or ghost entry, so generate the pair rather than
    hand-remembering it (the index-completeness late catch,
    `notes/adr/0126-*.md`, is this lesson anticipated rather than
    rediscovered). Fill in the scaffolded stubs with the session's real
    record and archived prompt before committing; the script is
    idempotent, so re-running it after filling in content is a no-op.

## Step 14 — register hygiene at close (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

14. **Register hygiene at close (R-RH, `notes/adr/0143-adr-index-
    generated.md`).** The close commit moves THIS session's own closed
    rows to `Done` — a row whose own words say CLOSED may not be left
    sitting in `Now` or `Next` for the next session to re-triage — and
    re-measures every reading set against its budget, recording the
    actuals in the session record. The compression arc exists because
    both halves were skipped: `roadmap.md`'s latency row read "arc
    CLOSED" from under the `## Next (backlog, no session scheduled)`
    heading for five days, and budgets were re-measured only when a
    gate went red.

## Step 15 — budget stop (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

15. **Budget stop: exceed a reading-set budget and you compact or you
    stop — never bump (R-BS, ADR-0143, guard #3).** `.agents/reading-
    sets.edn`'s `:budget-lines` is now ratcheted against
    `.agents/reading-sets-baseline.edn` and
    `ehrt.docs-tooling.reading-set-budget-test` fails any budget above
    it, so the bump is not merely discouraged, it is unavailable. When
    a set goes over: compact the set's own paths back under the number
    in the same session (the ADR-0141 close is the worked example — a
    ~50-line register block compressed back to one row and a citation
    BEFORE any number moved), or STOP-AND-REPORT. A budget moves, and
    the baseline with it, only inside a compaction ADR. The history
    this replaces is in that file's own header: fourteen in-place bumps
    superseded by one 2026-08-05 re-baseline, then eleven more dated
    re-derivations through 2026-08-16, each one honest and each one
    raising the ceiling the gate had just caught.

## Step 16 — red pushed with green (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

16. **A red-first commit is pushed together with its green successor,
    never alone (R-RP, ADR-0143, ADR-0142's own practice made the
    rule).** Capture and commit the red exactly as before — the red
    checkpoint is what makes the green mean something — but hold the
    push until the green commit exists, then push both. A red commit
    pushed by itself puts a known-failing tip on `main` and burns a CI
    run proving what the session already knows. This applies equally
    when the red-first commit cannot compile standalone (a new test
    naming a function its own green commit introduces): that is a
    normal shape for red-first, and pushing the pair together is what
    keeps it off the remote as a lone tip. Disclose it in the session
    record when it happens.

## Step 17 — anchored register edits (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)

17. **Anchored edits on register files; read the diffstat before you
    commit (R-AE, ADR-0143, from the ADR-0141 near-miss).** A register
    made of INDEPENDENT rows — `.agents/plans/roadmap.md`,
    `.agents/rulings.md`, any README index — is edited by anchored
    insertion or anchored replacement of the specific row, never by
    slicing between two anchors and re-joining (`s[:start] + new +
    s[end:]` silently deletes every row in between: it took out the
    D8-5 closure, the repo-review-4 charter, and the sim-theory.edn
    row in one edit). Before every commit, read `git diff --cached
    --stat` and compare the changed-line count against what you
    intended to change; that comparison — 209 changed lines where ~35
    were intended — is what caught it. Any number computed from a
    damaged file is also wrong, so re-measure after restoring, never
    reuse the measurement taken during the damage.

## VERIFICATION (moved verbatim from SKILL.md, 2026-08-17, ADR-0145)


**A regression-oracle claim means SHA-256 digests of output files
across a disposable worktree at the baseline commit — a test-count or
assertion-count comparison is NOT an oracle and may not be reported as
one** (`notes/ADRs.md` ADR-0030, J2, ratified 2026-08-02 after finding
that exact substitution had gone uncaught through two prior sessions'
own dated notes, ADR-0029's D2/D3). `bin/regression-oracle
<baseline-ref> <target-ref>` is the standing harness — two disposable
`git worktree`s, a synthetic from-scratch classpath per worktree
(`:local/root` pointed at that worktree, never a historical commit's
own `deps.edn`), `bin/oracle-src/ehrt/oracle/digest.clj`'s own
fixed-seed golden runs for the vendored-root set current at the time
it runs. A session whose own prompt or ADR entry asserts "the
regression oracle held" or "byte-identical" without naming this
script's own output is making a claim it has not actually verified —
fix the claim (run the script) or fix the wording (name the weaker
method actually used, disclosed as a deviation the way ADR-0029's own
D2 dated note did), never leave it unlabeled.

**A gate run (`make test`, `make docsgen`, `clojure -M:poly test`, any
`bin/` script whose exit code is the claim) writes its output to a full
log file and records its exit code explicitly — `make test > <log> 2>&1;
MAKE_EXIT=$?` or equivalent — never read through a pipe or a
`tail`/`head` that can swallow the exit code or truncate the countable
signature** (review-3 D2-6; the ADR-0135 incident class, H-2). `cmd |
tail -40` reports the exit status of `tail`, which is 0 no matter what
`cmd` did, so a red run reads as green; the same truncation drops the
block/test counts a session reconciles against.

**Catching yourself writing a justification for skipping an
instructed step is the stop signal itself: do the step, or
STOP-AND-REPORT.** A drafted excuse is a fabrication near-miss and
goes in the session record either way (ADR-0128).

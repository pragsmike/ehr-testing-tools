# Archived prompt: review-3-fix-c-ceremony-and-category (2026-08-15)

Archived verbatim in substance; the design channel's original used typographic
dashes and em dashes, transliterated to ASCII here per this repo's archive
convention. Nothing else altered.

---

SESSION PROMPT -- review-3 fix session C: ceremony-script correctness (D1-6, D2-6) + the one src ride-along (D4-3)

Drafted by the design channel, 2026-08-15, against a fresh public clone at `7544f7c` (Session B landed and channel-verified: an independent dead-link scanner over docs/** + components/*/docs/** returns zero; both "gone-target" successors confirmed present at `notes/tools/agents/`; register rows D1-9/D1-10 present).

Channel evidence for this session, read from the tree, not carried forward: `bin/post-push-verify`'s header contradicts ITSELF -- its top comment (:10-13) promises the default base is "the range's own merge-base with origin/<branch> before your push," while its usage text (:40) says "defaults to the tip's own first parent (a one-commit range)" -- and the code (:54, `${tip_sha}^1`) implements the usage text. Three live sightings this arc: ADR-0135's 4-commit push (1 checked), Session A's 3-commit push (1 checked), Session B (correct only because each push carried one commit). The fix is code AND header, made consistent with each other.

## Author rulings, verbatim

* "accept all." (2026-08-15) -- adopting the review-3 plan's Session C design as proposed, plus D4-3 from Session D as this session's single `src` ride-along (both are error-category fidelity; the channel batched them here so Session D stays docs-only or disappears).

## Read first

* `.agents/plans/2026-08-15-repo-review-findings.md` rows D1-6, D2-6, D4-3, and H-2 (the exit-code-masking incident class)
* `.agents/plans/2026-08-15-repo-review-3-plan.md` Session C
* `bin/post-push-verify` (whole script -- 100-odd lines; note the header/usage contradiction above)
* `.agents/skills/build-session/SKILL.md` and its `.claude/` mirror (byte-identical today; keep it so)
* `bases/cli/src/ehrt/cli/core.clj` around :698-703 and :984-997 (`gate`'s `:file-not-found` sites) and :1375-1382, :1485 (`show`'s `:path-unreadable` pattern -- the model to match)
* ADR-0078 / AR-RL-3 (cited at :698 -- the loud-vs-result law that governs how error categories are surfaced; the fix must respect it)

## Step 0 -- Preflight

`bin/preflight` plain; verify `git rev-parse stable-20260815-result-nodes^{}` = `b139de589083c6b4967c1a4769b2c6a8d17feac4`. Baseline tip `7544f7c` or descendant; report it. No tag owed -- the arc tags at step 7.

## Step 1 -- D1-6: the range derivation, red first

1. Red first, as a test. Add a docs-tooling test (suggested `post_push_verify_range_test.clj`) that constructs a throwaway git repo with a bare "origin", pushes commit A, then commits B, C, D locally with C's message carrying a deliberate non-ASCII byte, pushes B..D, and invokes `bin/post-push-verify` with NO base argument. Assert non-zero exit (the non-ASCII message in the pushed range must be caught). Run it: witness RED (today the script checks only D and reports OK). If the script's `git fetch origin` step makes a throwaway-remote harness awkward, the test may set the remote to the bare repo path -- a `file://` origin is a real origin. STOP-AND-REPORT if the script cannot be exercised headlessly at all.
2. Green. Change the default base derivation to origin's pre-push tip: record `origin/<branch>`'s SHA BEFORE the fetch (the script currently fetches first -- order matters; if the pre-fetch ref is unavailable, fall back to `git merge-base HEAD origin/<branch>` computed BEFORE fetch, and if neither is derivable, FAIL LOUDLY with the existing "pass one explicitly" message rather than defaulting to `tip^1`). Fix the header AND usage text so both describe the implemented behavior; delete the "one-commit range" sentence.
3. Re-run the test: GREEN. Also run the script's existing manual invocation against the current tree with an explicit range to confirm no regression in the ASCII/verbatim checks.

## Step 2 -- D2-6: the pipe-discipline line

In `build-session`'s verification step, add ONE line (matching the skill's existing law style -- staging hygiene, message-via-file, ASCII messages are the siblings): gate output is captured to a full log file with `make`'s exit code recorded explicitly (`MAKE_EXIT=$?` or equivalent), never read through a pipe or `tail`/`head` that can mask the exit code or truncate the countable signature (the ADR-0135 incident class, H-2). Byte-copy to the `.claude/` mirror.

## Step 3 -- D4-3: `gate` reports the true category (the one src change)

Red first: a CLI test creating a file that exists but is unreadable (chmod 000; skip-with-disclosure if the test runs as root, where chmod cannot make a file unreadable -- assert on the category only when the precondition holds, and say so in the docstring) and asserting `ehrt gate` returns `:path-unreadable`, not `:file-not-found`. Witness RED. Then route `gate`'s read path through the same try/catch-around-the-read pattern `show` uses at :1375-1382, respecting ADR-0078/AR-RL-3's result-vs-loud law exactly as `show` does. GREEN. Docs: if `docs/cli.md`'s exit-code or error-category text is generated (it is -- `cli-md-is-current-test` gates it), regenerate rather than hand-edit; if `gate`'s category list is documented in `docs/formats.md` or the manual, update the one sentence and NOTHING else.

## Step 4 -- Commits, records, close

Three commits, message-via-file, ASCII, in this order:

1. `fix: post-push-verify derives the pushed range from origin's pre-push tip; fails loud when underivable (review-3 D1-6, three live sightings)` -- script + test co-landed, red witnessed.
2. `docs: build-session names explicit exit-code capture for gate runs (review-3 D2-6, ADR-0135 incident class)` -- skill + mirror.
3. `fix: gate reports :path-unreadable for unreadable files, matching show (review-3 D4-3)` -- src + test co-landed, red witnessed, any generated-doc regen in the same commit.

Records: ADR (next free number) with all three red/green witnesses; register rows D1-6, D2-6, D4-3 -> FIXED, ADR cited; session record; prompt archive. Full `make test` unpiped, `MAKE_EXIT` captured. Expected blocks: 636 + delta for the new test namespaces -- this session adds one or two namespaces (post-push-verify range test; possibly a new CLI test namespace if the unreadable-file test does not fit an existing one) -- predict the delta from the projects' test paths BEFORE running, then reconcile; explain any mismatch. Push. Then `bin/post-push-verify` WITH NO ARGUMENTS -- this is the fix's own first live use, and the pushed range is multi-commit (three-plus), so it must report checking ALL of them; record the range it derived and the count it checked in the session record. The by-hand full-range check runs one last time alongside it, as the fix's independent witness.

## Fences

* Touch ONLY: `bin/post-push-verify`, the new range test, `.agents/skills/build-session/SKILL.md` + mirror, `gate`'s read path in `bases/cli/src/ehrt/cli/core.clj` (the minimum try/catch routing -- no refactor of surrounding code), its test, any generated doc that must regenerate for the CLI test gate, the register's three disposition cells, and the close artifacts.
* `show`'s code is the model, not a target -- read-only.
* STOP-AND-REPORT: the script cannot be exercised headlessly; the category fix cannot respect AR-RL-3 without touching more than the read path; the running-as-root precondition makes the D4-3 red unwitnessable AND no other unreadable-file precondition is achievable (report before skipping); block-count delta not reconciling; any fence pressure.

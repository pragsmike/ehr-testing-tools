# 2026-08-11 — Batch-straddle recording: use case, rulings, and the user-guide opening (ADR-0112)

## Scope

A DOCS-AND-REGISTERS-ONLY session executing the author's own
deferred-to-next-session ruling from the ADR-0111 window
(`.agents/rulings.md`, "From ADR-0112"): the batch-boundary-straddling
encounter scenario gets three documentation placements — a demo
(already landed, ADR-0111), a use case (this session), and prominent
treatment in the tool-specific user guide (opened this session) and
the general EHR Testing Guide (the author's own queue, outside this
workspace). Zero `src` change anywhere. Two content commits plus this
record's own close-phase commit: `abed772` (use case),
`9bdc346` (rulings + roadmap).

## Evidence highlights

**The use case, verified against the schema and the docsgen gate.**
`:supply-batch-straddling-traffic` lands in
`components/corpus/docs/use-cases.edn` immediately after
`:play-a-generated-corpus-back-over-time`, its transport-realism
sibling — same generate-sim + `corpus batch` command pair ADR-0111's
own demo already witnessed, reused verbatim rather than re-executed
this session. `make docsgen` regenerated `docs/use-cases.md` and one
new per-case page as the only delta; `docs/cli.md`, `docs/pipeline.md`,
`docs/operators.md` came out byte-unchanged, confirmed by `git status`
after regen, exactly as the driving prompt required.

**A mid-session fence conflict, caught and STOPPED-AND-REPORTED, not
resolved silently.** The driving prompt's own Step 1 named
`ehrt.docs-tooling.usecases-test` as a gate this step "must pass by
name," but that namespace hardcodes the exact use-case count
(`committed-use-cases-edn-has-twenty-cases-test`) with a running
comment-log of every prior bump. Landing the 21st case broke it
(`expected: (= 20 ...) actual: (not (= 20 21))`), and the only fix — a
one-`deftest` edit under `test/` — was exactly what the same prompt's
own fence forbade ("zero test-code change anywhere"). The session held
here rather than guessing which instruction should give way, laid out
both the conflict and two ways to unblock, and waited. The author
licensed a scoped amendment (verbatim: *"a"*, accepting the channel's
drafted text) — rename the deftest, bump `20` to `21`, append one
comment-log line matching the six precedents already in the file,
co-land it in commit 1. Applied exactly as licensed; nothing else
under `test/` touched. `notes/adr/0112-*.md`'s own Deviations section
records this in full, including the channel's own incomplete-probe
error by name: the driving prompt's Read-first list omitted
`usecases_test.clj` even though the same prompt's Step 3 explicitly
bumps `notes/adr/README.md`'s own sibling count lock.

**A property-test flake, disclosed rather than silently re-run away.**
The first post-amendment `make test` failed
`ehrt.sim-engine.engine-test`'s
`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
— a generative test in a namespace this session's own fence never
touches at all. A second full run, same tree, zero code change in
between, passed the same test clean under a different random seed.
Recorded as a pre-existing, seed-dependent flake in that test's own
generative suite, named in `notes/adr/0112-*.md`'s Deviations section
rather than quietly treated as noise.

**Full gate**, run twice: once before commit 1 (green, including the
licensed amendment), once again after the docs-only rulings/roadmap
edits landed on top (green, the `engine-test` flake cleared on
re-run). `bin/verify-nist-lock`: OK, 6 hit-nexus-sourced coordinates
matched both times. `gitleaks git --staged -v` (each checkpoint) /
`gitleaks detect` (pre-push): no leaks found. Oracle bracket
(`bin/regression-oracle ed5f51d 9bdc346`): `IDENTICAL: every root's
digest matches` — all 35 roots, matching the pre-analysis exactly
(this session's own footprint is one `.edn` catalog file, generated
docs, one licensed deftest, and `.agents/*`/`notes/*` registers only).

## Judgment calls and their ratification status

- **The fence amendment itself** — author-ruled mid-session, recorded
  above and in `notes/adr/0112-*.md`'s own Deviations section in full,
  including both the author's verbatim licensing text and the
  channel's own named error. Not a judgment call this session made
  alone; explicitly ratified before being applied.
- **The user-guide trigger's RATIFIED status** — the roadmap paragraph
  now reads RATIFIED rather than PENDING, per `.agents/rulings.md`'s
  own "User-guide trigger read" entry. That entry's own provenance is
  disclosed as channel-read, not author-verbatim — the author's "ok"
  accepted the channel's proposed recording *sequence* (do this next
  session), not necessarily every downstream inference the channel
  drew from the same exchange. Flagged plainly so the author can strike
  or correct it at a glance, per the driving prompt's own instruction.
- **GitHub's own `displayTitle` for commit 1's CI run reads `"test"`**
  (the literal workflow name) instead of the commit subject, while
  every other run in the same `gh run list` output shows the real
  commit message. `headSha` matches `abed772` exactly and `conclusion`
  is `success`, so this is a cosmetic `gh run list`/API rendering
  quirk, not a run-content anomaly — disclosed in `notes/adr/0112-*.md`
  rather than silently normalized away.

## Findings and HEAD landed

No discrepancies found between the driving prompt's own pre-decided
content (the use case's exact text, the rulings/roadmap wording) and
the live tree that would have forced a further STOP-AND-REPORT beyond
the one fence conflict above, which the author resolved in-session.

The tag `stable-20260811-corpus-batching` was created at `ed5f51d`
(this session's own Step 0), peeled ref verified exact match, remote
unmoved (`git fetch` + `git rev-parse origin/main` confirmed `ed5f51d`
at session start; the last five `main` CI runs were all
`completed`/`success`).

**HEAD landed**: `abed772` (use case: `use-cases.edn`, generated docs,
the licensed `usecases_test.clj` amendment), `9bdc346` (rulings +
roadmap), and this record's own close-phase commit.

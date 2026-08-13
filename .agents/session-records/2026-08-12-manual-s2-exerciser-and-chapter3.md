# 2026-08-12 — User manual S2: demo exerciser and chapter 3, a simulated hospital (ADR-0120)

## Scope

S2 of the five-session user-manual arc: land R3's own charter (author
verbatim, ADR-0113: "The demos must be known to work, and exercised as
documented to make sure they actually play out as written") as a demo
exerciser for `ed-tuesday`, co-landed with Chapter 3
(`docs/manual/03-a-simulated-hospital.md`), matching the ratified
sequence. Two content commits landed: `07dbc5d` (the demo exerciser)
and `9473c81` (chapter 3), plus this record's own close-phase commit.

## Red→green evidence highlights

**Red first, by design.** `ehrt.docs-tooling.demo-exerciser-fresh-test`
was run against the tree before `bin/demo-exerciser-ed-tuesday`
existed: `committed-readme-and-script-agree-test` failed exactly as
predicted (`readme-count 21, script-count 0`, first divergence at
index 0), while the file's other five extraction-logic tests (synthetic
fixtures, no dependency on the real script) were already green —
proving the check can fail before it's trusted to pass.

**One real, unrelated red, caught by `make test` before any push.**
After writing `bin/demo-exerciser-ed-tuesday` and staging it,
`ehrt.cli.executable-bits-test` failed: the script was `chmod +x` on
disk but not staged as `100755` in the git index. Fixed with `git
update-index --chmod=+x bin/demo-exerciser-ed-tuesday`; re-run green
(535 assertions, 0 failures, 0 errors, `bin/verify-nist-lock` OK).

**`make integration`, run twice, the first a false-positive teaching a
process lesson.** The first witnessed run (staged but not yet
committed) failed only at its own tree-clean postcondition — every real
invariant (diff, digest equality, batch count/verified, straddle
membership) held, but this session's own uncommitted checkpoint-1 and
chapter-3 files made `git status --porcelain` non-empty. Resolved by
committing checkpoint 1, stashing the still-uncommitted chapter-3 files
(`git stash push -u`), and re-running against a tree that actually
matched `HEAD`: **green**, exact closing line `== demo-exerciser-
ed-tuesday: every command asserted, every named invariant held, tree
clean ==`. The stash was popped immediately after.

**Every ed-tuesday command dry-run verified before the script was
written.** All 11 taught commands (21 physical lines with
continuations) were run manually first, off the taught path, to catch
any README/tree divergence before committing to a design — every
witnessed value (payload maps, snapshot counts, the base/latency
`sha256sum` digest, the 34-batch listing, the straddle grep) matched
the README's own printed output exactly. No divergence found.

**Oracle bracket:** `bin/regression-oracle 800ae28 9473c81` →
`IDENTICAL: every root's digest matches`, all 35 roots — matching the
pre-analysis (no oracle root's own `src` touched; the new
docs-tooling identity check, the exerciser script, and the docs/figure
are all outside every oracle root).

## Judgment calls and their ratification status

- **Fenced-block extraction generalization — a design choice, not a
  ruling, disclosed for review.** `ed-tuesday`'s own README has several
  fenced blocks, some bare ` ```bash ` command fences and some `$
  `-prefixed terminal transcripts mixing a command with its own
  witnessed output — unlike `quickstart-demo`'s single fence.
  `ehrt.docs-tooling.demo-exerciser-fresh`'s own extraction rule (a line
  starting `bin/ehrt ` or `$ ` opens a command; a backslash-continued
  line's own successor is a continuation regardless of prefix; every
  other fenced line is skipped) is this session's own generalization of
  the pattern, not itself ruled by name. The author may correct this
  reading if a different extraction rule was intended.
- **Which invariants get their own assertion, versus exit-code-only —
  read from Read-first item 2's own enumeration.** The driving prompt
  named four: "the determinism `diff -rq`," the 34-batch listing, the
  straddle membership, "the second-clock identity claims." The README
  itself has no literal `diff -rq` (a two-file `diff`, not a recursive
  `-rq`); read as the same determinism/second-clock-invariance claim
  the README's own "Ground truth is invariant" section states via
  `diff` plus `sha256sum`, both asserted (diff emptiness plus
  independently re-derived digest equality). The Walker
  admission-after-discharge reordering narrative (also in that same
  README section) was NOT given its own programmatic assertion —
  outside the driving prompt's own named list, and adding it would have
  meant parsing ticker/board text output, a scope this session did not
  take on unprompted.
- **GitHub Actions integration workflow left untouched, disclosed as a
  scope note, not a finding requiring a fix.** `.github/
  workflows/integration.yml` calls `clojure -M:poly test :all
  project:integration` directly, not `make integration` — the new
  exerciser does not automatically run in the nightly/dispatch CI job.
  The driving prompt's own fence named only the Makefile as the
  licensed touch point; wiring CI to also run the exerciser is left for
  a future session.
- **Site-profiles link, not restated.** Chapter 3's own site-profiles
  section links `docs/site-profiles.md` and names its truth-invariance
  guarantee in one sentence, rather than reproducing any of that
  document's own detail — matching the manual's own style contract
  ("references are linked, never restated").

## Findings and HEAD landed

No discrepancies between the driving prompt's stated preflight premise
and the live tree: `origin/main` was at `800ae28` exactly; the last
five CI runs were all green; every Read-first document matched its own
characterization; every `demos/scenarios/ed-tuesday/README.md` command
ran exactly as documented — the STOP-AND-REPORT clause this session's
own prompt named for a README/tree divergence never fired.

The tag `stable-20260812-manual-s1` was created ANNOTATED at `800ae28`
(this session's own Step 0), pushed, peeled ref verified exact.

**HEAD landed**: `07dbc5d` (the demo exerciser), `9473c81` (chapter 3),
and this record's own close-phase commit.

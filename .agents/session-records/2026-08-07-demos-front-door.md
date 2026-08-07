# 2026-08-07 — demos front door

## Scope

Asked to relocate the sim demos and scenarios out of component-local
docs trees into a new top-level `demos/` (with `scenarios/`/`traces/`
subdirectories), and feature them in the top-level README, per the
author's own AR-VB3-3 direction (ADR-0072) and a design-channel
proposal ruled on three counts. Did exactly that: `git mv` for all
twenty-seven files across three source trees, `.gitattributes`'
`-text` pattern moved in the same commit, three pointer READMEs left
behind, every live path citation swept to the new root-resolvable
form, `demos/README.md` written as the new front door, and a "See it
run" section landed in the top-level README — both live-probed to a
rendered bed board in two commands. Full detail, rulings, and
verification: `notes/adr/0073-demos-front-door.md`.

## Red→green evidence highlights

- Byte-witness: seven precious `messages*.txt` transcripts, sha256
  before and after `git mv`, identical hash sets.
- Full suite green at every checkpoint (Step 0 baseline, post-move,
  post-feature): 314-484 assertions depending on the run, 0
  failures/0 errors, confirmed failure-free across each entire run's
  own output by grep, not just the tail.
- `ehrt.docs-tooling.quickstart-fresh-test` caught a real regression:
  the new "See it run" section's own `` ```sh `` fence, placed ahead
  of README's Quickstart fence, hijacked that gate's literal
  first-fence extraction (readme-count 5 vs. script-count 15,
  witnessed RED). Fixed forward to `` ```bash `` (already every demo
  README's own convention, invisible to this gate's literal anchor);
  re-verified GREEN both targeted and full-suite.
- Oracle bracket `bin/regression-oracle 721adb6 d0296d2`: all
  twenty-seven roots IDENTICAL — expected and confirmed for a
  docs-only move.
- Live probe: both See-it-run commands ran to completion from a
  genuinely clean `out/` state (a stale prior-session leftover at the
  same path removed first); 74/74 messages rendered a board snapshot,
  none dropped.

## Judgment calls and their ratification status

All disclosed in `notes/adr/0073-demos-front-door.md`'s own Deviations
section, not yet author-ratified (session ran R30, unattended):

- Fence language `` ```sh `` → `` ```bash `` in README's "See it run"
  block, departing from the appendix's own literal tag, to avoid the
  `quickstart-fresh-test` collision.
- Removed a stale `out/corpus/busy-tuesday` directory (a different
  prior session's leftover output) before the live probe, since
  `generate`'s own never-overwrite contract refused to run over it.
- Rewrote `demos/traces/README.md`'s `site-profiles/` bullet (it
  previously described site-profiles as relocated to another
  component's doc tree — now false, since this move puts it back as a
  direct sibling) and `demos/scenarios/README.md`'s `demos/`→`traces/`
  references (the sibling directory's own name changed as a structural
  fact of the move) — both treated as mechanical consequences of the
  move under AR-DM-1, not prose improvements under AR-P-4.
- Noted, not a judgment call but a correction: the driving prompt's own
  claim that `readme-presence-test` is one of "the four" gates this
  move exercises doesn't hold — that gate's scan roots are `.agents/`
  and `notes/` only, never `demos/`; it passed because it was untouched,
  not because it judged the new tree.

## Findings and HEAD landed

No findings beyond the deviations above (AR-P-4's own fence — a
tempting fix mid-move is a finding, never taken; none arose). HEAD:
this record's own closing commit, following `d0296d2` (Step 2,
AR-DM-2) and `f07684c` (Step 1, AR-DM-1).

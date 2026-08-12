# 2026-08-11 — Corpus batching: the transport gets one notch real (ADR-0111)

## Scope

Two author rulings, 2026-08-11 (`.agents/rulings.md`, "From
ADR-0111"): the batcher is a corpus-level tool, separate from the sim,
working on any directory of valid v2 message files including foreign
corpora (Q1 a); the HL7 v2 batch protocol's BHS/BTS wrappers land as a
`:batch` framing codec in v1 (Q2 a). One session, mechanism plus demo:
a new pure partition fn (`ehrt.corpus-io.batch`), a new `:batch` codec
(`ehrt.corpus-io.framing`), a new CLI verb (`ehrt corpus batch DIR
--interval MINUTES --out-dir OUT`), and a witnessed demo run over
`demos/scenarios/ed-tuesday/`'s own latency out-dir. Three commits:
`1e0a1d6` (mechanism), `d1f8fa1` (demo), and this record's own
close-phase commit.

## Evidence highlights

**Determinism, witnessed directly, not merely claimed.** Two
independent `ehrt corpus batch` runs over the same
`out/scenarios/ed-tuesday-latency` input at the same `--interval 60`
produced byte-identical output directories (`diff -rq`, empty output).
Every one of the 34 written batch files self-verifies: the CLI's own
`write-and-verify-batch!` decodes what it just wrote straight back
in-memory and confirms `BTS-1` against the real message count before
ever reporting success — the codec's own free transport-integrity
check, exercised by real code, not offered as an unexecuted claim.

**The straddling encounter, real data.** Smith, James (MRN000002, bed
ED-H05): admitted (A01, MSH-7 `2026-08-11T00:30:26Z`) lands in
`batch-000.hl7` (3 messages, BTS-1 verified); discharged (A03, MSH-7
`2026-08-11T01:34:19Z`) lands in `batch-001.hl7` (4 messages, BTS-1
verified), one clock-hour later. Both files individually pass every
transport-level check available to a receiver holding only one of
them — the demo's own lesson made concrete rather than asserted in the
abstract. Total run: 283 messages, 34 occupied hourly buckets spanning
`2026-08-11T00:00Z` through `2026-08-12T13:00Z`; an interior gap
(`08:00Z`-`10:00Z` on 2026-08-12) is visibly absent from the numbered
sequence, the interior-empty-batch deferral shown live.

**The micro-relocation, proven byte-identical.** The partition fn
needed MSH-7 extraction; the one existing implementation lived in
`ehrt.corpus.player`, a `corpus` namespace corpus-io may never require
(AR-2, the directional rule `ehrt.corpus-io.interface`'s own docstring
states). The entire "lenient segment/field reads" block moved
verbatim to a new `ehrt.corpus-io.er7-fields`; `ehrt.corpus.player`
now re-exports the same four public names as plain `def`s. Proof, not
assertion: `ehrt.corpus.player-test`'s own 24 pre-existing deftests
over these functions ran completely unmodified and green (51
assertions, `component:corpus`'s own test run) — no new test
scaffolding was needed to prove the move byte-identical, because the
existing coverage already does, transitively, through the re-exported
names.

**Full gate**, run against the live tree before committing: `clojure
-M:poly check`: OK. Full local suite (`clojure -M:poly test :all
skip:integration`, unredirected capture, run via `make test`): 616
occurrences of "0 failures, 0 errors," zero `FAIL`/`ERROR` anywhere.
New coverage: `ehrt.corpus-io.batch-test` (7 tests, 20 assertions),
`ehrt.corpus-io.framing-test`'s own 8 new `:batch` cases (25 tests, 72
assertions total for the file), `ehrt.cli.core-test`'s own
`batch-command`/dispatch coverage folded into that namespace's 289
tests, 853 assertions overall. `ehrt.cli.cli-parse-guard-lint-test`:
4 tests, 22 assertions, 0/0. `ehrt.docs-tooling.sim-purity-lint-test`:
5 tests, 14 assertions, 0/0 (unchanged — no sim-family `src` touched).
`bin/verify-nist-lock`: OK, 6 hit-nexus-sourced coordinates matched.
`gitleaks git --staged -v` (each checkpoint) / `gitleaks detect`
(pre-push): no leaks found. Oracle bracket (`bin/regression-oracle
b5b9b9e d1f8fa1`): `IDENTICAL: every root's digest matches` — all 35
roots, matching the pre-analysis exactly (footprint is corpus-io + cli
+ demo docs only, zero `sim`/`emit`/engine `src` change anywhere).

## Judgment calls and their ratification status

- **`:batch` deliberately NOT added to `ehrt.corpus-io.source-sink/
  Framing`'s closed enum.** `docs/dev/source-sink-design.md` (the
  design doc naming that schema's own "five framing kinds") is out of
  this session's own fence — not licensed to touch. Since nothing in
  the batcher's own design requires constructing a Source/Sink map at
  all (it calls `corpus-io/encode :batch items` directly, the same way
  `ehrt.corpus.player/frame-event` already calls `:er7-multi`
  directly), `:batch` lives purely in `framing.clj`'s own dispatch and
  `known-framings` set. Disclosed in `notes/adr/0111-*.md`'s own
  "Deviations" section, not a departure from any ruling — a
  scope-respecting choice.
- **Interval spec: plain integer minutes, not a unit-suffixed
  grammar.** No existing flag in this CLI (`--rate`, `--idle-cap`,
  `--arrival-gap`, `--warm-up-seconds`) uses a `15m`/`1h`/`24h`-style
  grammar — every duration flag is a bare integer in one fixed, named
  unit. `--interval` follows that established convention rather than
  inventing an unprecedented grammar for this one flag alone, per the
  driving prompt's own "read help.clj's conventions and decide,
  disclosed" instruction.
- **Batch-file numbering: sequential over occupied buckets, not the
  bucket's own absolute epoch index.** An epoch-relative index would
  have made a genuinely huge, non-obvious number (millions, for any
  date past 1970) the first thing a reader sees in a filename; the
  simpler v1 choice trades away encoding the interior-gap information
  in the filename itself (disclosed as part of the interior-empty-
  batch deferral, not a silent simplification).
- **The taxonomy note's "mutation as imperfect transport" framing is a
  paraphrase, not a verbatim quote.** The driving prompt names this as
  "the author's... framing from the driving conversation," but the
  literal words were not carried into this session's own written
  context — only the paraphrase the prompt itself supplies. Recorded
  as a paraphrase in both `notes/adr/0111-*.md` and
  `.agents/rulings.md`, never presented inside quotation marks as
  someone's exact words.

## Findings and HEAD landed

No discrepancies found between the driving prompt's own THE DESIGN
section and the live tree that would have forced a STOP-AND-REPORT.
Two pre-existing CLI coverage tests needed updating in place when the
new verb landed — `dispatch-unknown-corpus-action-names-its-verbs-test`
and `spec-command-pairs-match-dispatchs-known-routes-test`/`stub-key`
(`bases/cli/test/ehrt/cli/help_test.clj`) — the same shape every prior
verb addition to `help/cli-spec` has always required; not a defect,
the expected consequence of extending the `corpus` group's own verb
set, caught red by the first full CLI test run and fixed before the
mechanism commit landed.

The tag `stable-20260811-latency-demo` was created at `b5b9b9e` (this
session's own Step 1), peeled ref verified exact match, remote unmoved
(`git fetch` + `git rev-parse origin/main` confirmed `b5b9b9e` at
session start; the last five `main` CI runs were all
`completed`/`success`).

**HEAD landed**: `1e0a1d6` (mechanism: partition fn, `:batch` codec,
CLI leg, co-landed tests, `docs/cli.md`), `d1f8fa1` (demo: the
"Batched delivery" README subsection with the real witnessed run), and
this record's own close-phase commit.

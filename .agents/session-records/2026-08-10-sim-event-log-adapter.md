# 2026-08-10 — Corpus player: sim event-log adapter

## Scope

Session prompt executing the roadmap Next row "Corpus player: sim
event-log input adapter" (named since ADR-0014's own Context), plus
the fired Deferred row "`ehrt play`'s own bare reads" (ADR-0096
Finding 2 / ADR-0097, revisit trigger: the next session touching
`ehrt play` or the corpus-player slices — this session touches both).
Three commits: the bare-reads fix (categorized reads, the lint
allowlist retired), the producer side (`corpus generate sim` spools
`events.edn`), and the adapter itself (`plan`'s own injectable
timestamp seam, an `.edn` event-log input leg in `play-command`'s own
dispatch, a compact event-line ticker, `--board`/`--sink` rejections
for event input). This is ADR-0100.

## Red→green evidence highlights

**Commit 1:** the lint with both allowlist entries emptied reported
exactly `("play-events-from-file" "play-events-from-dir")`,
non-vacuous — the ready-made red the roadmap row promised. Live
chmod-000/missing-file probes found the pre-fix tree already clean
end to end (the upstream `sniff-path-format` guard masks the second
bare read), a genuine, disclosed finding rather than the raw crash a
literal reading might expect. Fix wraps both reads via a new
`slurp-play-input`; lint green (4 tests, 22 assertions) with the
allowlist mechanism itself retired, not just emptied.

**Commit 2:** two new tests (`sim-execute-fn-events-edn-is-pr-str-of-
ground-truth-test`, `generate-sim-command-events-edn-matches-format-
ground-truth-bare-text-test`) both failed pre-fix (`git stash push
--keep-index` isolating `generators.clj`'s own fix from its own new
tests: 2 failures/2 errors, 1 failure/1 error, a raw
`FileNotFoundException` slurping the never-written `events.edn`);
green after `git stash pop` (18/44 and 260/766 respectively). The
CLI-level test proves real byte-equality end to end against `ehrt sim
run --format ground-truth`'s own bare stdout, not just a unit-level
`pr-str` assertion.

**Commit 3:** with every `src` change stashed (`core.clj`, `help.clj`,
`interface.clj`, `player.clj`, `docs/cli.md`), `ehrt.cli.core-test`
failed 20/2; `ehrt.corpus.player-test` failed to COMPILE (`No such
var: player/event-timestamp-ms`). Green after restoring: `player-test`
31/51, `core-test` 272/800 (twelve new deftests). Every existing
`player-test` deftest passed unmodified throughout — the byte-
identical-default witness for `plan`'s new `:timestamp-fn` seam.

**Full gate:** `clojure -M:poly check` OK at every commit; full local
suite (`clojure -M:poly test :all skip:integration`, unredirected
capture) 0 failures/0 errors across the entire log (592 occurrences of
"0 failures, 0 errors", grepped in full), 3m59s; `bin/verify-nist-lock`
OK, 6/6; oracle bracket
(`stable-20260810-fixture-relocation`→`3e49932`) all 34 roots
IDENTICAL, soundness check clean.

## Judgment calls and their ratification status

- **The Context's own event-shape sketch (`:type`) was wrong,
  corrected in the open.** Not ratified in advance — resolved
  in-session per the prompt's own verify-then-act instruction for
  "payload shapes," a live probe (a bare churn run plus a real
  `busy-tuesday`-config run) establishing the real key is `:event`
  and `:location` is a map. Disclosed in both this record's own
  companion prompt archive and ADR-0100's Context/Deviations.
- **`--sink` rejected on event input, beyond Q1 a.'s own literal
  scope** (which named only `--board`). Not separately ruled — a
  structural necessity (an unguarded `--sink` on event input would
  raise a raw `ClassCastException`, the exact bare-exception class
  this session's other half exists to close), applying the SAME
  bail-out style Q1 a. specified for `--board` to the one other flag
  sharing its precondition. Disclosed in ADR-0100's Commit 3 and
  Deviations.
- **Demo touch landed on commit 3, not the close commit.** Licensed
  directly by Step 5's own "your call, disclose which" — disclosed
  here and in the prompt archive.
- **`.agents/plans/roadmap.md`'s own Next-row removal and Deferred-row
  retirement, both landed only in this close-phase commit.** Matches
  every prior Next-row-to-Done transition in this repo's own history
  (e.g. ADR-0098's, ADR-0099's own close) — not a deviation, the
  driving prompt's own Step 8 specifies this ordering.

## Findings and HEAD landed

**No new defects found in shipped code.** The fired Deferred row's own
structural gap (the second bare `slurp` in `play-events-from-file`/
`-from-dir`) was real but not independently live-reproducible today
(masked by the upstream `sniff-path-format` guard) — disclosed rather
than either overclaimed as a live crash or silently treated as
already-safe; the fix (and a new witness-pair predicate test) closes
the structural gap regardless.

**Tag paid forward:** `stable-20260810-fixture-relocation` tagged at
`3e3c167` (Step 1, this session — ADR-0099's own successor-tag debt,
tag law case (i)), peeled ref verified.

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.

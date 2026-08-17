<!-- Attic file: notes/adr/0021-bases-sim-cli-projects-sim-retired.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0021 — `bases/sim-cli`/`projects/sim` retired (F2 fired): `ehrt sim` gains `check`/`identifiers`/`version`, closing the parity gap the retirement review found first

**Status:** Accepted (status line added ADR-0143, 2026-08-16, from `notes/ADRs.md`'s own index row).

### Context

`notes/facts-register.md` F2 (R33, ADR-0009, 2026-07-29) named a dated,
unscheduled retirement trigger for `bases/sim-cli` and its composing
`projects/sim`: retire when a review finds no use of either outside
their own test suites. `notes/2026-07-30-refactoring-review.md` P3-6
surfaced the trigger as checkable (`sim-cli` required by nothing but
`projects/sim` and its own tests, per `poly deps`) without firing it —
that was left for the author. This session (`notes/prompts/2026-08-01-ehr-testing-retire-sim-cli.md`,
author-ruled 2026-08-01: fire) was that review, gated on two things
before deletion: AR-1 verified parity command-by-command from
`bases/sim-cli`'s own source (not its docs), stopping to escalate on
any element with no `ehrt` equivalent rather than accepting a silent
capability loss; AR-2 required deletion to cost zero test-namespace
coverage, not merely zero line count.

AR-1's own enumeration found the trigger's stated precondition was not
literally true. `bases/sim-cli` exposed four commands: `run` (11
documented flags: `--seed`/`--patients`/`--arrival-gap`/`--emit`/
`--at`/`--reference-date`/`--utc-offset`/`--warm-up-seconds`/`--churn`/
`--config`/`--format`), `check` (reads a ground-truth EDN vector from
stdin, runs the invariant catalog), `identifiers` (3 flags: `--seed`/
`--patients`/`--config`), and `version` (none) — 14 distinct flags
total across its merged `cli-spec`. Against `ehrt sim`'s own dispatch
(`bases/cli/src/ehrt/cli/core.clj`) at session start: only `run` was
mounted, and even it was missing `--arrival-gap`/`--at`'s `:coerce
:long` entries (silently wrong once `run.clj`/`emit-state` did
arithmetic on the uncoerced string). `check` and `identifiers` had no
`ehrt` equivalent at all — and `identifiers` was not merely an
internal capability: `docs/simulate-your-facility.md` actively taught
`sim identifiers --seed <seed> --patients <n> [--config <file>]`
(the standalone binary) as the literal answer to a real user FAQ
("What if synthetic data ever reached a real system — how would we
find and remove it?"), directly contradicting both `AGENTS.md`'s "the
user path never mentions it" claim and F2's own "no use outside its
own tests" precondition. `--format er7`/`ground-truth`'s bare-stdout
modes were also load-bearing, not cosmetic: `bases/sim-cli`'s own test
suite named `run-then-check-cli-pipe-round-trips` "the real gap this
format exists to close" and property-tested it across 30 seeds.

Escalated to the author rather than proceeding (`AskUserQuestion`,
this session): wire the gaps first, then retire (chosen over firing
anyway with a disclosed capability loss, or not firing at all).

### Decision

**Mount `ehrt sim check`/`identifiers`/`version`, close `run`'s flag
gaps, THEN delete both bricks — parity map: 4 commands, 14 flags, all
mapped.**

- `ehrt.corpus.sim-adapter` (the existing `run!` adapter, ADR-0005's
  dependency-direction invariant: corpus depends on sim, never the
  reverse) gains three siblings — `check!` (1-arg `ehrt.sim.interface/
  check-all`, the same default arity `sim-cli`'s own `check-command`
  always used), `identifiers!` (delegates to `identifiers-command`,
  same opts pass-through convention as `run!`), and `version!`
  (`ehrt.sim.interface/version` + `git-sha`, sim's own library-version
  marker, distinct from this repo's `ehrt version` identity) —
  exported from `ehrt.corpus.interface` as `sim-check!`/
  `sim-identifiers!`/`sim-version!`.
- `bases/cli/src/ehrt/cli/core.clj` gains `sim-check-command` (ports
  `sim-cli`'s own stdin-EDN contract and its three named rejections
  verbatim), `sim-identifiers-command`, and `sim-version-command`,
  wired into `dispatch`'s `"sim"` case arm and its injectable `-fn`
  map. `cli-spec` gains `:arrival-gap`/`:at` (`:coerce :long` — the
  fix for the silent-string bug) and `:utc-offset` (`:coerce :string`,
  the same digit-only-string discipline `:reference-date` already
  documents).
- `--format er7`/`ground-truth` mount via a new `:bare-text` result
  METADATA convention (never `:payload`, so the EDN/JSON envelope is
  unaffected) that `main!` checks with the same precedence as its
  existing `:cli-help`/`:display-text` special cases — but, unlike
  `show-command`'s `:category :display-text` (always exit 0, ADR-0013),
  `:bare-text` does NOT override `result->exit-code`: a failing bare-
  format run must still exit non-zero, `sim-cli`'s own contract. One
  disclosed simplification: a non-`:ok` result under a bare format
  renders through the normal stdout path (EDN/`--pretty`) rather than
  `sim-cli`'s own stderr-only discipline for that case — still
  visible, still the right exit code, just not stream-split; a `sim
  check` reading a non-vector off a failed pipe already reports
  `:malformed-input` rather than misbehaving. `--format json`/`--json`
  needed no separate mount — already exactly what `ehrt`'s own
  pre-existing `--json` does (the full envelope, JSON instead of EDN).
  `--version`/`--help` per-verb shortcuts are not replicated as flags:
  the capability (retrieve sim's version) is reachable via the new
  `ehrt sim version` verb instead, matching this CLI's own group-based
  convention rather than `sim-cli`'s standalone-shell shortcuts.
- `docs/simulate-your-facility.md` and `docs/glossary.md` repointed to
  the `ehrt sim identifiers`/`ehrt sim check` spelling; `docs/cli.md`
  regenerated (`make cli-doc`) from `help.clj`'s updated `cli-spec`,
  which gained the three new verbs' documentation.
- Real subprocess smoke tests before deletion (`bin/ehrt sim
  version`/`identifiers`, and `bin/ehrt sim run --format ground-truth
  \| bin/ehrt sim check`) confirmed the mount end to end, not just
  through the injected-fn test suite.
- THEN `bases/sim-cli` and `projects/sim` deleted for real: root
  `deps.edn`'s `poly/sim-cli` dependency and `bases/sim-cli/test` path
  removed; `workspace.edn`'s `"sim"` project entry removed and
  `:necessary` re-derived a FIFTH time (clear every override, `poly
  check`, confirm the same two pre-existing warnings — `conformance`
  needs `docs-tooling`/`judge-fhir-official`/`judge-v2-nist`,
  `integration` needs `judge-fhir-official` — return unchanged; the
  retired `"sim"` entry itself carried no `:necessary` override to
  lose). `docs/dev/architecture.md`'s mermaid node/edge and both
  bricks' structure-table rows removed; `AGENTS.md`, `SETUP.md`,
  `docs/dev/README.md`'s now-empty "Deprecation notices" section swept
  to past tense or removed. `projects/conformance/test/.../
  sim_cli_real_invocation_test.clj` renamed to
  `ehrt_sim_run_real_invocation_test.clj` — it never subprocessed
  `sim-cli`, always `bin/ehrt sim run`, so the old name became doubly
  misleading once `sim-cli` itself was gone.
- Historical citations of `bases/sim-cli` left untouched, per this
  file's own R34 convention (citations, not voice): `ehrt.sim.interface`'s
  docstring ("re-exports exactly what bases/sim-cli's own src calls"),
  `ehrt.sim.run`/`run-test`'s citation of `ehrt.sim-cli.core/dispatch-action`'s
  `-fn` convention as that convention's origin, and every `notes/`/
  `.agents/session-records/` prose reference — all describe provenance,
  not present state, and none claim `sim-cli` currently exists.

### Verification

Coverage accounting (AR-2), before/after `projects/sim`'s deletion:
`components/sim`'s own 21 test namespaces ran 4× workspace-wide before
(`conformance`, `ehrt-cli`, `sim`, `integration` — each composes
`poly/sim` independently, confirmed by reading all four projects' own
`deps.edn`) and run 3× after (same namespaces, one fewer duplicate
pass — exactly the "same namespaces, fewer duplicate passes" shape
AR-2 called the expected one; zero coverage loss). `ehrt.sim-cli.core-test`
(89 assertions) is gone — correctly, retired with the code it tested,
not a loss. `ehrt.conformance.sim-cli-real-invocation-test` (now
`ehrt-sim-run-real-invocation-test`) was unaffected by the deletion
either way — it subprocesses `bin/ehrt`, not `sim-cli`.

`ehrt.docs-tooling.structure-currency-test`'s absence gate (hardened
2026-07-31, `notes/facts-register.md` F17) caught the stale
`bases/sim-cli` architecture-table row on its own, unprompted, before
any doc sweep landed — captured as real red evidence
(`every-structure-table-row-names-a-real-brick-test` failing, citing
`bases/sim-cli` by name, the exact absence-direction property it was
built to catch), then green after the fix; the `projects/sim` row
went unchecked by this same gate (it only tracks `components`/`bases`
kinds, not `projects` — a pre-existing scope limit, not something this
session changed) and was swept by hand instead.

`poly check`: OK, before and after, `:necessary` re-derivation
included. Per-push lane (`poly test :all skip:integration` —
`conformance`+`ehrt-cli` after `projects/sim`'s removal): 0
failures/0 errors, both before the parity mount and after the full
retirement. Integration lane (`poly test :all project:integration`):
0 failures/0 errors, both times. New tests added for the parity mount
itself: `ehrt.corpus.sim-adapter-test` (+6 tests covering `check!`/
`identifiers!`/`version!`, hermetic + one real-default case each,
mirroring `run!`'s own existing pattern) and `ehrt.cli.core-test` (+20
tests covering the three new dispatch routes, `sim-check-command`'s
stdin contract including all three named rejections, `--format`'s
bare-text metadata for both `er7` and `ground-truth`, the real
`run --format ground-truth | sim check` pipe, and `--arrival-gap`/
`--at` coercion); `ehrt.cli.help-test`'s hand-mirrored coverage
constants (`stub-key`, `known-dispatch-pairs`) updated to match.

### Deviation record

Two carve-outs from `bases/sim-cli`'s own bare-format behavior,
disclosed rather than silently dropped: (1) a non-`:ok` result under
`--format er7`/`ground-truth` renders to stdout through the normal
path instead of `sim-cli`'s own stderr-only discipline for that case
(see Decision, above, for why this was judged an acceptable
simplification rather than a capability loss). (2) `sim-cli`'s own
cross-verb bare-format gate (`--format ground-truth` on a non-`run`
verb is a named rejection) is not replicated as an explicit check —
`ehrt`'s `:format` opt is only ever consulted by `sim-run-command`
itself, so the combination silently no-ops elsewhere rather than
erroring; judged low-stakes since the combination is nonsensical
either way. `--version`/`--help` per-verb shortcut flags are
deliberately not replicated (see Decision) — a named, disclosed
design choice, not an oversight.

**Status.** Accepted (author-directed via `AskUserQuestion`
escalation, autonomous session otherwise), 2026-08-01. Session record:
`notes/prompts/2026-08-01-ehr-testing-retire-sim-cli.md`.

---


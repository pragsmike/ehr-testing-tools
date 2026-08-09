# 2026-08-09 — Storefront fixture: FHIR pairing rows + the coverage promotion

## Scope

Session prompt naming AR-SD-0 through AR-SD-6 (dated 2026-08-08,
executed 2026-08-09 — a one-day authorship/execution gap, disclosed),
executing the roadmap's own Next row "Storefront demo fixture." This
session authors a minimal, project-authored FHIR fixture that gates
`:accepted` from the real official validator, witnesses all five FHIR
defect operators against it as new pairing-registry rows
(`:judge-fhir-official`), promotes tier-two coverage from report-only
to a gating test over the full ten-operator catalog, and rewrites the
README's mutate demo with a real accepted→rejected transcript against
the committed fixture.

Preflight: working directory confirmed the ext4 clone
(`~/src/ehr-testing-tools`), HEAD `b7a1dc8` exactly (vendoring batch
4, ADR-0090), branch up to date, working tree clean. `clojure -M:poly
check`: OK. Last five CI runs on `main` disclosed one red: the
scheduled `Integration` workflow (`31301880957`), diagnosed and fixed
forward (see Step 0, below) before the tag.

## Step 0 — Preflight fix-forward + tag (AR-SD-0)

Diagnosed the red: `projects/integration/deps.edn` dropped
`poly/judge-v2-nist` on 2026-07-31 on a premise the pairing-registry
landing (`948f5e5`, 2026-08-08) broke silently —
`pairing_conviction_test.clj` now requires `ehrt.judge-v2-nist.
interface` directly, and `integration` is scheduled-only, so the break
wasn't caught until the next scheduled run. Fixed forward: re-added
the dep and its `nist-hit` `:mvn/repos` entry, earning edge named.
Proven red (the real CI failure log) then green (local run with CI's
own exact command, then a fresh `workflow_dispatch`, `31308023126`,
success). Committed `2088763`; pushed; verified.

`stable-20260808-vendoring-batch-4` created annotated at `b7a1dc8`,
pushed, peeled ref verified. Oracle pre-digest
(`bin/regression-oracle b7a1dc8 b7a1dc8`): 34 roots IDENTICAL.

## Step 1 — Fixture + measurement (no commit)

Authored `components/corpus/test-fixtures/fhir/storefront-patient.json`
(a minimal `Bundle`+`Patient`, no `meta.profile`). First draft rejected
at baseline on two real base-spec rules (a missing `Bundle.entry.
fullUrl`, then an invalid `urn:uuid:` value) — both fixed, then the
clean fixture gated `:accepted` for real (one `:warning`/`:pass`
`dom-6` advisory, nothing else).

All five FHIR operators measured against the fixture, one real
`bin/ehrt corpus mutate` + `bin/ehrt gate fhir` run each — zero skips,
every observed `:code` genuinely new relative to the baseline's sole
finding. Full table: `notes/adr/0091-storefront-fixture.md`.

## Step 2 — Land, two commits (a mid-session correction disclosed)

**First attempt, `cd08b20`:** fixture, five registry rows, a
`:judge-fhir-official` gate-mutant arm added directly to
`pairing_conviction_test.clj` (in `judge`'s own test tree), the
promoted tier-two coverage gate, and the README rewrite — landed
together. Local full-suite run green (this session's own artifact
cache was already warm from Step 1's manual measurement runs). Pushed.
**CI failed**: `poly test :all skip:integration` — `:not-cached,
fhir-validator-cli`. `judge`'s test tree is composed by every project,
including `conformance`/`ehrt-cli`, whose ordinary push lane never
primes the artifact cache (AGENTS.md's hermetic-test-suite rule) — a
premise this session's own local warm cache had masked.

**Fixed forward, same session, `c690ec3`:** `pairing_conviction_test.clj`
reverted to witnessing only the artifact-independent judges;
`projects/integration/test/ehrt/integration/pairing_conviction_fhir_
test.clj` (new file, `^:integration`) witnesses the FHIR rows instead
— the same placement `contract_pairing_test.clj`/`baseline_gating_
test.clj` already use for the identical reason. Verified hermetic FOR
REAL: `skip:integration` run green with the artifact cache directory
renamed out of the way entirely, restored after. Both CI lanes
confirmed green for real afterward: the push lane (`31312272026`) and
a fresh `Integration` `workflow_dispatch` (`31312458033`).

**Red→green witnessed directly, in-session:** corrupted
`:duplicate-element`'s own `:expected` set to a nonsense code — failed
naming the real observed classes; restored byte-identical (diffed
against a backup); re-ran green.

**gitleaks false positives, twice:** a `generic-api-key` heuristic
tripped by three adjacent short judge-id keywords on one line (the
`JudgeId` enum and its test twin) — resolved by reformatting to one
keyword per line before either commit ever reached the remote (the
first inside an unpushed local amend; the ADR's own prose tripped the
same heuristic a third time describing the fix, reworded before
staging).

Oracle bracket (`bin/regression-oracle b7a1dc8 c690ec3`): 34 roots
IDENTICAL — no sim/compile/engine path touched, as AR-SD-5 required.

## Step 3 — Record, commit `eefc531`

`notes/adr/0091-storefront-fixture.md` authored in full (design
rationale, measurement table, coverage table at promotion, README
before/after, both fix-forward findings disclosed, the
fixture-relocation backlog's third member, this session's own
successor tag debt). `notes/ADRs.md` gained its index line;
`notes/adr/README.md`'s own file count corrected 88→89 (`ls
notes/adr/*.md | grep -v README | wc -l`); the roadmap's "Storefront
demo fixture" Next row removed, Done pointer added. Pushed; CI watched
to conclusion: `31313690663`, success.

## Step 4 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-09-storefront-fixture.md`
(the driving prompt, archived verbatim, with a deviation record) land
together, indexed in both READMEs' own entry lists.

## Successor tag debt

Recorded in `notes/adr/0091-storefront-fixture.md`: the next session
that opens fresh work tags `stable-20260809-storefront-fixture` at
this session's own closing tip.

## Judgment calls and their ratification status

- **Date used throughout (2026-08-09, not the prompt's own 2026-08-08
  header).** Not separately ratified — the prompt was authored the
  evening of 2026-08-08 but this session's own local clock read
  2026-08-09 throughout execution; every artifact (witness dates, the
  ADR's own Status line, this record's own filename) uses the date
  the work actually happened, disclosed as a one-day gap in the ADR's
  own Context section rather than silently following a filename
  instruction written before the execution date was known.
- **`:expected` sets are bare `:code` strings, not `{severity, code}`
  pairs.** Matches the existing v2-hapi rows' own convention exactly;
  `:remove-required-element`'s own `"invariant"` finding was excluded
  from its `:expected` set specifically because that bare code recurs
  in the clean baseline too (a different occurrence) — `"invalid"`
  alone is the unambiguous witness, and `some` only needs one match.
- **Coverage's catalog-completeness filter (`(filter :doc ...)`).**
  `operator-entries` reads the same shared, global, mutable registry
  atom `ehrt.corpus.operators-test`'s own registry-mechanics tests
  register throwaway entries into — filtering to entries carrying
  `:doc` is the SAME distinguishing signal that suite's own comment
  already uses to set its own throwaway entries aside, not a new
  convention invented here.
- **The mid-session test-placement correction itself.** Not ratified
  in advance (the driving prompt's own AR-SD-2 didn't anticipate this
  hermeticity conflict) — disclosed in full in ADR-0091's own
  "Mid-session correction" section rather than silently absorbed into
  a clean-looking single landing.

## Findings, disclosed not acted

- **The preflight CI regression** (Step 0, above) — a pre-existing
  defect from a prior session's own landing, unrelated to this
  session's own design work, fixed forward with the author's
  agreement rather than left for a future session to re-discover.
- **The README's own pre-session example output was stale** — no code
  anywhere in the repo produces the `totals:`/`by-code:` prose-line
  format the old mutate demo pasted; the CLI's real default output is
  the raw `{:status ...}` map. Corrected as part of AR-SD-4's own
  rewrite, not a separate finding requiring its own session.

## HEAD landed

`eefc531` (Step 3's own commit — Step 4's own commit lands after this
record, in the same push as the prompt archive).

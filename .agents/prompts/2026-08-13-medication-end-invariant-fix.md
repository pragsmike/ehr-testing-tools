# 2026-08-13 — ehr-testing-tools: medication-end invariant fix (ADR-0123)

## Context

Archived 2026-08-13. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `6827f5b` (ADR-0122's own close) and closed at
this record's own close-phase commit. Original prompt follows verbatim.

## Original prompt

Session prompt — medication-end invariant: pre-horizon referents (ADR-0123)

You are Claude Code executing under R30 ceremony for ehr-testing-tools,
working for mg. Small SRC session executing the author's ruling (a) on
ADR-0122's diagnosis (verbatim "a", 2026-08-13): the CHECKER is fixed
-- `medication-end-references-existing-order-and-follows-it-in-time`
widens to accept order referents living in a patient's
`:pre-horizon-facts` (the compile layer's designed straddle case), with
its time law adjusted to hold wherever the order lives. The engine and
compile layer are untouched. STOP on any conflict. Standing notes: full
`make test` before EVERY push; companions in-fence by rule; post-0116
gate policy standing (a defspec failure at any seed is a new finding --
during THIS session, at the two chartered seeds it is the expected red;
at any OTHER seed, STOP).

### Read first

1. `notes/adr/0122-positive-seed-invariant-violation-diagnosis.md` --
   the full diagnosis; the fix implements its option (a) exactly.
2. The invariant's checker in `components/sim-check/` and its test
   file -- current spec, conventions.
3. `components/sim-trajectory/docs/trajectory-computation.md` --
   pre-horizon-facts' design meaning (the fix's justification: the law
   matches the design).

### Step 0 -- Preflight and tag ceremony

* origin/main at `6827f5b`; CI green (`gh run list --limit 5`,
  completing ADR-0122's channel leg incl. commit 2's then-pending run).
  Else STOP.
* Tag `stable-20260813-positive-seed-diagnosis`, ANNOTATED, at
  `6827f5b`; push; peeled exact. Case (i): channel fresh-clone
  verification 2026-08-13 (lineage, ASCII x2, zero src/test, seeds and
  erratum recorded), CI per preflight.

### Step 1 -- The two conditions, red-first (commit 1)

1. Positive control FIRST (green-stays-green): a deftest with a
   hand-built minimal GT carrying a `:medication-end` whose
   `order-event-id` matches nothing -- neither a top-level
   `:medication-order` nor any pre-horizon fact. Run pre-fix: must be
   GREEN (rejected). If it is NOT rejected pre-fix, STOP -- the law is
   already blind in a way the diagnosis didn't find.
2. The regression red: a deftest running the property's engine config
   at shrunk seed `8589258984` through `check/check-all`, asserting
   zero violations. Pre-fix: RED (the diagnosed violation). Capture.
3. The fix: widen the checker per option (a) -- referent search covers
   top-level orders AND the patient's pre-horizon facts; the
   follows-in-time law compares against the order's time wherever it
   lives. Minimal diff; docstring states the widened spec and why
   (design language, no ADR tokens).
4. Green evidence: the regression deftest green; the positive control
   STILL green; the full defspec re-run at both recorded failing seeds
   (`1786589996178`, `1786617342587`), 150 trials each, both green;
   full `make test` green. Message: `fix: medication-end invariant
   recognizes pre-horizon order referents (ADR-0123)`

### Step 2 -- Close (commit 2)

Registers: diagnosis row -> RESOLVED (fix landed, option (a), both
seeds green); rulings "From ADR-0123": the "a" ruling verbatim with the
two conditions; S4 row -> next. Self-archive at close-phase START;
ADR-0123 (red/green evidence incl. the positive control's both-sides
runs); indices 120 -> 121; Done line; session record. Message: `docs:
session record and prompt archive -- invariant fix (ADR-0123)`

### Oracle bracket

Pure identity, all 35 roots -- empirically established by the
diagnosis (zero roots emit `:medication-end`); the checker change is
unreachable from every root's output. `bin/regression-oracle 6827f5b
<final>`; non-identity -> STOP. Gates: standing; ASCII x2; gitleaks; CI
confirm or disclose.

### Fences

Touch ONLY: the invariant's checker file in `components/sim-check/`;
its test file; registers, prompts, session-records, `notes/adr/
0123-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; companions by rule.
ZERO engine, ZERO compile/trajectory, ZERO other invariants, ZERO
docs/manual. Outside -> STOP (widen-by-ruling).

STOP-AND-REPORT on: the positive control failing pre-fix; the
regression red not red; any post-fix defspec failure at a seed other
than the two chartered; oracle non-identity; anything not pre-decided.

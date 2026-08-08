# 2026-08-08 — The EncounterEnd fix: the interpreter learns the five arms

## Scope

Session prompt naming AR-EE-0 through AR-EE-6, executing the fix
ADR-0081's own R1-R3 rulings licensed: real openness tracking in the
GMF interpreter's walk state, the A1/A5 compile-arm split, a counted
suppression (R2), retiring `index-of-last-open-encounter`, surfacing
the counter in `census.clj`. Src edits confined to
`gmf_interpreter.clj`/`census.clj` plus new tests and one new fixture,
per the prompt's own fence — no vendoring, no engine/emitter/player
changes.

Preflight: working directory confirmed the ext4 clone, HEAD `c2bcb67`
exactly (ADR-0081's own closing tip), working tree clean. `clojure
-M:poly check` OK; full suite green (511 assertions, 0/0); oracle
pre-digest (`bin/regression-oracle c2bcb67 c2bcb67`) all twenty-seven
roots IDENTICAL; last-five CI runs on `main` all green. AR-EE-0
executed directly: `stable-20260808-fidelity-riders` annotated and
pushed at `c2bcb67`, peeled ref verified both locally and via `git
ls-remote`.

## The STOP-AND-REPORT this session actually hit

AR-EE-1's own blast-radius probe (required before any fix code) walked
each of the 27 oracle roots' own recorded seed/config through the
CURRENT, pre-fix interpreter, counting unmatched `:encounter-end`
emissions. 26 roots predicted zero, as expected. `hypothyroidism` —
already vendored, shipping since 2026-08-07 — did not: 5 of its own
300 oracle-seed walks contain a real, already-shipped dangling
`:encounter-end`, the SAME idiom ADR-0071/0072 diagnosed and deferred
two OTHER modules on, now caught reachable inside a module this repo
already ships. Per R3's own bar, the session stopped here, reported
the finding with full walk evidence, and returned control to the
author rather than writing a line of the fix — the report is preserved
verbatim in this session's own transcript; the finding and its
evidence are also recorded in `notes/adr/0082-encounterend-fix.md`'s
own AR-EE-1 entry.

The author's ruling (design channel, 2026-08-08, recorded verbatim in
ADR-0082 as AR-EE-1a/1b/1c): trace the absorption mechanism BEFORE any
fix code (AR-EE-1a); amend R3 to license `hypothyroidism` as one
disclosed mover, with requirements riding the license (AR-EE-1b);
correct ADR-0071's own erratum, append-not-erase (AR-EE-1c). This
session executed all three before Step 2's own fix code moved.

## Steps and commits

**Step 1 (no commit, per R3's own bar on a nonzero finding).** The
prediction table; the STOP-AND-REPORT; the author's ruling; the trace
(AR-EE-1a) — `compile-trajectory`'s own PRE-EXISTING single-encounter-
scope truncation (`encounter-closed?`, sim/ADR-0007 point 3) and its
own pre-horizon gate both already silently drop the dangling event's
own consequences today, for reasons unrelated to EncounterEnd openness
— explaining, not merely observing, why the 300-patient round trip
passes despite the dangling reference.

**Step 2 (`dad2553`, AR-EE-2/3).** The fix: `open-encounter-index` (a
pure walk-level fold, retiring `index-of-last-open-encounter`);
`:suppressed-encounter-ends` threaded as ctx state across FOUR
independent fold sites (`walk-module`, `run-submodule`,
`call-submodule-step`, `run-module` — a called submodule's own
suppression must reach the caller); the A1/A5 arm split; the
"encounters never nest" assert (`:encounter` case and
`wellness-wait-step`, the other raw `:encounter`-minting site); a
dated resolution note on `docs/gmf-interpreter.md` section 7 item 3.
Red witnessed in-session (a synthetic fixture,
`encounter-end-fixture.json`, run against an isolated load of git
HEAD's own pre-fix interpreter — no stash, no working-tree disturbance)
then committed green (three new tests, the fixture, the implementation,
the doc note, one existing test fixture corrected — it never actually
closed its own wellness encounter, an inaccuracy the new assert
caught). Full suite: 511 → 521 assertions, 0/0 throughout. Pushed; CI
watched to conclusion (run `31258259066`, success, 3m30s); post-push
message verified (one delta, the known trailing-blank-line artifact).

**Step 3 (`deabbbd`, AR-EE-4/5).** Confirmation under the amended bar.
`bin/regression-oracle c2bcb67 dad2553 --declared-digest-change`:
IDENTICAL — all 27 roots, `hypothyroidism` included, byte-for-byte
unchanged (a STRONGER result than the license anticipated — AR-EE-1a's
own trace already explains why: the absorption layer swallows the
dangling content either way, at this population). The RAW per-patient
trajectory digest for `hypothyroidism` DOES move (hand-verified
separately from the harness) — this is where the licensed correction
actually lives. `hypothyroidism`'s own committed round-trip test
re-witnessed green. A labeled census re-run (`encounterend`) against
the pin-verified checkout: 85 modules, `{:ok-walked 84,
:out-of-scope-by-ruling 1}` parity holds; diffed against the prior
census artifact, exactly ONE row moved
(`anemia-unknown-etiology`'s own middle seed, 10→9 events, the exact
predicted effect of one suppression), the other 84 byte-identical.
Brief's own executed-note added; roadmap's Deferred row updated.

**Step 4 (this record).** `notes/adr/0082-encounterend-fix.md`
authored directly; `notes/adr/0071-vendoring-batch-2.md` gains the
dated erratum (AR-EE-1c); index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own file count corrected (79→80, verified by
`ls`); roadmap Done pointer added; session record and prompt archive
land; successor tag debt recorded in the ADR.

## The finding this session did NOT fix

The in-session anemia/colorectal proof (AR-EE-3), run against the
pin-verified checkout at ADR-0071/0072's own exact seeds:
`anemia___unknown_etiology.json` is fully extinguished post-fix (0
violations at all three seeds, was 14/18/11 pre-fix — the same
qualitative shape ADR-0071 recorded, small numeric drift plausibly
catalog evolution since 2026-08-07). `colorectal_cancer.json` is NOT —
its own violations (4/0/4, matching ADR-0072's own record exactly) are
BYTE-IDENTICAL pre- and post-fix. A raw-trajectory scan confirmed ZERO
dangling `:encounter-end` references anywhere in its own 300-patient
seed-42 walk post-fix — this fix's own target defect is genuinely
absent there. The residual violations
(`:clinical-content-only-when-admitted`, mostly, plus one
`:discharge-follows-admission`) are a SEPARATE, previously-undiagnosed
defect, one layer downstream of the interpreter, not localized this
session (AR-EE-6's own fence: a tempting fix found mid-move is a
finding, not an act). Recorded in ADR-0082 and the roadmap's own
Deferred row, narrowing the payoff rider: anemia is ready to vendor;
colorectal needs its own future diagnosis first.

## Verification

See `notes/adr/0082-encounterend-fix.md`'s own Confirmation section
for the full account (digests, census diff, suite counts, gitleaks,
CI runs, tag verification) — not restated here.

## Deviations, disclosed

- **The session's own driving prompt anticipated a clean predict-and-
  confirm pass (all 27 identical); the live tree delivered a real
  STOP-AND-REPORT instead.** Handled per the prompt's own R3 bar and
  the author's own subsequent ruling — not a deviation from either, but
  the exact scenario R3 was written to catch, disclosed here because
  the prompt's own narrative assumed the clean case throughout its
  Steps 2-4 framing.
- **The colorectal finding narrows the brief's own payoff-rider
  framing** ("anemia and colorectal vendor as a mini-batch") — disclosed
  in ADR-0082, the roadmap, and this record rather than silently
  narrowed without a trace.

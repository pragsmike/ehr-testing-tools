# 2026-08-13 — Medication-end invariant: pre-horizon referents, fixed (ADR-0123)

## Scope

Small SRC session executing the author's own "a" ruling on ADR-0122's
three lettered fix options: widen
`medication-end-references-existing-order-and-follows-it-in-time`
(`components/sim-check/src/ehrt/sim_check/check.clj`) to accept an
order referent living in a patient's own `:pre-horizon-facts` — the
compile layer's designed straddle case — with the follows-in-time law
adjusted to hold wherever the order lives. Two commits: commit 1 (the
red-first fix, two chartered conditions proven, the checker widened,
green evidence captured); commit 2 (this record's own commit — the
close-phase scaffold: ADR-0123, registers, self-archive). The engine
and compile layer are untouched, per the ruling and this session's own
fences.

## Red→green evidence highlights

- **Positive control, run pre-fix: GREEN (rejected), as required.** A
  hand-built log with an UNRELATED `:pre-horizon-facts`
  `:medication-order` entry on `:registered`, plus a `:medication-end`
  whose own `:order-citation` matches neither that entry nor any
  top-level order — the pre-fix checker already rejects it correctly,
  proving the widening (once it lands) cannot become permissive by the
  mere presence of `:pre-horizon-facts` machinery, only by an actual
  citation match.
- **The regression, run pre-fix: RED, exactly the diagnosed
  violation.** A new deftest reconstructs `engine-test.clj`'s own
  `mixed-authored-and-compiled-run-satisfies-the-full-invariant-
  catalog` property's exact config at the ADR-0122 shrunk seed
  `8589258984`, through `check/check-all` — same patient
  (`PID-000003-fd6d262d`), same instant (`:at 436440`) ADR-0122's own
  Step 2 direct witness names.
- **Post-fix: 64 tests, 65 assertions, 0 failures, 0 errors**
  (`ehrt.sim-check.check-test`, direct namespace run) — the regression
  deftest green, the positive control still green.
- **Both recorded failing seeds green, 150 trials each**
  (`clojure.test.check/quick-check` against the property reconstructed
  verbatim): `1786589996178` and `1786617342587`.
- **Full `make test` green** (`clojure -M:poly check` then `clojure
  -M:poly test :all skip:integration`): exit 0, zero `FAIL`/`ERROR`
  anywhere; `bin/verify-nist-lock` OK, all 6 hit-nexus coordinates
  matched.
- **Oracle bracket: IDENTICAL, all 35 roots**, `bin/regression-oracle
  6827f5b f9fbeca` (an actual run, not a re-assertion of ADR-0122's own
  zero-reach argument) — soundness check passed, no
  `--declared-digest-change` needed.

## Judgment calls and their ratification status

- **The extra hand-built accept-case test, added then removed
  [self-caught, no ratification needed].** A third deftest directly
  constructing the pre-horizon straddle case and asserting acceptance
  was drafted alongside the two chartered tests, then removed before
  any commit — the driving prompt charters exactly two deftests (the
  positive control and the regression), and the regression test
  against the real engine's own output already proves the accept path
  end to end through genuine compile-layer content, making the extra
  hand-built case redundant against this session's own minimal-diff
  discipline. Nothing landed that wasn't asked for.
- **The oracle bracket ran against commit 1's own SHA (`f9fbeca`), not
  a literal `<final>` placeholder [channel-inferred, disclosed].** The
  driving prompt's own Oracle bracket section names `bin/regression-
  oracle 6827f5b <final>`. Commit 2 (this record, ADR-0123, registers)
  is docs-only and cannot move any digest by construction, so running
  the actual script against the only code-touching commit is
  equivalent to running it against the eventual final SHA and is more
  verifiable than a self-referential placeholder that cannot resolve
  until after that same commit is written — the same practical choice
  ADR-0122's own Oracle bracket section faced and left implicit; this
  session makes the substitution and its reasoning explicit instead.

## Findings and HEAD landed

**No new findings.** The fix landed exactly as diagnosed and ruled:
zero surprises in the checker's own behavior, zero conflicts with the
tree, zero defspec failures at any seed other than the two chartered
during this session's own gate runs.

**S4 (the user-manual arc's own Chapters 6-7 session) is unblocked** —
the roadmap's own S4 row, previously "awaiting the positive-seed
invariant-violation diagnosis," now reads "next."

**Tag paid forward:** `stable-20260813-positive-seed-diagnosis` tagged
at `6827f5b` (Step 0, this session — the driving prompt's own case (i)
license), peeled ref verified exact match
(`6827f5bb8a84ecc12b52f5071574bb0d641ce247`).

**HEAD landed:** commit 1 (`f9fbeca`, the checker fix and its two
chartered tests) pushed and post-push-verified; this record's own
close-phase commit follows.

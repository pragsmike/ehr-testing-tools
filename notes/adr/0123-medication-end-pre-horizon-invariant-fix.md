## ADR-0123 — Medication-end invariant: pre-horizon referents, fixed

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-13.

### Context

ADR-0122's diagnosis fully characterized a genuine invariant-catalog
violation (`medication-end-references-existing-order-and-follows-it-
in-time`, at the shrunk counterexample seed `8589258984`) and recorded
three lettered fix options, deferring the choice among them to a
future, ruled session. The author's own ruling, verbatim, 2026-08-13:
*"a"* — option (a), the checker fix, with the two acceptance conditions
this session's own driving prompt names: a positive control proving
the checker still rejects a genuinely dangling reference, and the
diagnosed regression proving the widening resolves the designed
straddle case. This session implements exactly that fix; the engine
and compile layer are untouched, per the ruling and this session's own
fences.

### Tag ceremony

`origin/main` at `6827f5b` (ADR-0122 close) at session start, matching
this session's own driving prompt exactly. **The last five `main` CI
runs** (`gh run list --limit 5 --branch main`, checked at session
start): all `completed`/`success` — `31692375261` (ADR-0122 session
record, 4m29s), `31690811656` (ADR-0122 diagnosis/ceremony charter,
4m33s), `31680996212` (scheduled Integration, 9m36s), `31663630998`
(ADR-0121 session record, 3m35s), `31663134103` (ADR-0121 chapter 5,
4m34s) — no red among the five. Case (i) verification: lineage
confirmed (`origin/main` at `6827f5b`, working tree clean); both
commits at issue (`3f0db5e`, `6827f5b`) touch zero `src`/`test` paths;
both commit messages are pure ASCII (ASCII x2); ADR-0122's own Context
section already records the seeds (`1786589996178`, `8589258984`,
`1786617342587`) and the erratum this session's driving prompt cites.
Tag `stable-20260813-positive-seed-diagnosis` created ANNOTATED at
`6827f5b`; pushed; peeled ref confirmed
`6827f5bb8a84ecc12b52f5071574bb0d641ce247` — exact match.

### Step 1 — Red-first, then the fix (commit 1)

**Positive control, run pre-fix.** A hand-built minimal ground-truth
log (`check_test.clj`'s own
`medication-end-references-existing-order-and-follows-it-in-time-
detects-phantom-order-even-with-unrelated-pre-horizon-facts`): a
`:registered` event carrying an UNRELATED `:pre-horizon-facts`
`:medication-order` entry (a different citation), plus a
`:medication-end` whose own `:order-citation` matches neither that
entry nor any top-level `:medication-order` event. Run against the
pre-fix checker: **GREEN (rejected)**, as required — the mere presence
of `:pre-horizon-facts` machinery on `:registered` must not make the
checker permissive in general; only a citation match should satisfy
the widened branch. This proves the pre-fix baseline already behaves
as expected before any widening is even in play.

**The regression red, run pre-fix.** A new deftest
(`medication-end-references-existing-order-and-follows-it-in-time-
holds-at-the-adr-0122-shrunk-seed`) reconstructs `engine-test.clj`'s
own `mixed-authored-and-compiled-run-satisfies-the-full-invariant-
catalog` property's exact config (4 patients, 2 on an explicit
scripted admission/delay/discharge pathway, 2 module-only assigned to
`fixture-clinic`, `:module-horizon-days 3650`) at the shrunk seed
`8589258984`, and asserts `(result/ok? (check/check-all ground-truth
(:facility result)))`. Run pre-fix:

```clojure
FAIL in (...-holds-at-the-adr-0122-shrunk-seed) (check_test.clj:283)
expected: (result/ok? (check/check-all ground-truth (:facility result)))
  actual: (not (result/ok? {:status :rejected, :category :invariant-violation,
                            :payload {:violations
                            [{:invariant :medication-end-references-existing-order-and-follows-it-in-time,
                              :patient-id "PID-000003-fd6d262d", :at 436440}]}}))
```

**RED, exactly the diagnosed violation** — same patient
(`PID-000003-fd6d262d`), same instant (`:at 436440`) ADR-0122's own
Step 2 direct witness names. Captured here as the regression's own
proof of red before any fix landed.

**The fix.** `medication-end-references-existing-order-and-follows-
it-in-time`
(`components/sim-check/src/ehrt/sim_check/check.clj`) widens: a new
private helper, `pre-horizon-medication-order-citations-by-patient`,
maps each patient-id to the set of `:citation` values riding that
patient's own `:registered` event as a `:medication-order` entry in
`:pre-horizon-facts` (the compile layer's designed straddle case,
`components/sim-trajectory/docs/trajectory-computation.md`'s "History
phase": an order crossed during history phase is real, ongoing
therapeutic content, promoted to a registration-time fact rather than
dropped, while its own end can legitimately land in horizon phase as a
normal ground-truth event with nothing in top-level `:medication-
order` to resolve `:order-event-id` against). The main function's
`:when` clause now treats a `nil`-resolving `:order-event-id` as
satisfying the invariant when the event's own `:order-citation` is a
member of that set for the same patient — every other disjunct (wrong
event type, wrong patient, order after end) is unchanged, since those
only fire when `target` already resolved to something.

**The adjusted time law.** A `:pre-horizon-facts` entry carries no
`:t` of its own (`compile_trajectory.clj`'s own `:registration-facts`
construction, line ~494, emits only `{:event :codes :citation
:references}`) — there is nothing to compare against directly. The
law is satisfied by construction in this branch instead: a pre-horizon
fact is definitionally prior to registration (it is crossed during
history phase, before the registration instant), and every
ground-truth event for that patient — including the `:medication-end`
under test — comes after registration, since `:registered` is always
a patient's first event
(`registered-is-every-patients-first-event`, the same catalog). The
order therefore always precedes the end in effective time whenever the
pre-horizon branch resolves at all; no explicit comparison is needed
or possible.

### Green evidence

Post-fix, direct namespace run (`clojure -M:dev:test`,
`ehrt.sim-check.check-test`): **64 tests, 65 assertions, 0 failures, 0
errors** — the regression deftest green, the positive control still
green, every other invariant test in the namespace unaffected.

The full property re-run at both recorded failing seeds, 150 trials
each, via `clojure.test.check/quick-check` against the property
reconstructed verbatim:

```
seed 1786589996178: {:result true, :pass? true, :num-tests 150, :seed 1786589996178}
seed 1786617342587: {:result true, :pass? true, :num-tests 150, :seed 1786617342587}
```

Both **GREEN**.

Full `make test` (`clojure -M:poly check` then `clojure -M:poly test
:all skip:integration`): exit code 0, zero `FAIL`/`ERROR` anywhere in
the run, `bin/verify-nist-lock` OK (all 6 hit-nexus-sourced
coordinates matched). `clojure -M:poly check`: OK. `gitleaks git
--staged -v`: no leaks found, staged content only the two chartered
files.

### Oracle bracket

`bin/regression-oracle 6827f5b f9fbeca` (`f9fbeca`, this session's own
commit 1 — the only commit touching `src`/`test`; commit 2, this file
and the registers, is docs-only and cannot move any digest by
construction): **IDENTICAL, every root's digest matches** — the
soundness check passed (`digest.clj` identical outside its `ns` form
between the two worktrees), `--declared-digest-change` not needed.
Matches ADR-0122's own empirical finding exactly: zero of the 35
oracle roots ever emit a `:medication-end` event at their own pinned
seed/population, so a checker-only change (never invoked during
ground-truth or HL7 generation) cannot move any root's own bytes,
confirmed here rather than merely re-asserted.

### Fences

Touched exactly: `components/sim-check/src/ehrt/sim_check/check.clj`
(the checker); `components/sim-check/test/ehrt/sim_check/check_test.clj`
(its test file — two new requires, one new fixture-module def, two new
deftests); registers (`.agents/rulings.md`, `.agents/plans/
roadmap.md`, `notes/ADRs.md`, `notes/adr/README.md`); this file;
`.agents/session-records/*`; `.agents/prompts/*`. ZERO `sim-engine`
`src`, ZERO `sim-trajectory`/compile-layer `src`, ZERO other invariant
in the catalog, ZERO `docs/manual`.

### Deviations

**None.** The positive control was GREEN pre-fix as required; the
regression deftest was RED pre-fix, reproducing ADR-0122's own exact
witness (same patient, same instant); no defspec failure occurred at
any seed other than the two chartered during this session's own gate
runs; the oracle held pure identity, confirmed by an actual
`bin/regression-oracle` run rather than asserted from the
zero-touch/zero-reach argument alone.

### Index line

```
- 2026-08-13 — medication-end-pre-horizon-invariant-fix — ADR-0123
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Medication-end invariant: pre-horizon referents, fixed — implements the author's own "a" ruling on ADR-0122's three lettered fix options: `medication-end-references-existing-order-and-follows-it-in-time` (`components/sim-check/src/ehrt/sim_check/check.clj`) widens to accept an order referent living in a patient's own `:pre-horizon-facts` (the compile layer's designed straddle case, `trajectory-computation.md`'s "History phase"), the follows-in-time law adjusted to hold by construction wherever the order lives (a pre-horizon fact carries no `:t` of its own, but is definitionally prior to registration, and every ground-truth event comes after registration); red-first per the ruling's own two conditions — a positive control (a phantom order matching neither a top-level order nor any pre-horizon fact, even with an unrelated pre-horizon fact present) green both before and after the fix, and the diagnosed regression (the property's exact engine config at the ADR-0122 shrunk seed `8589258984`) RED before the fix, reproducing the same patient/instant ADR-0122's own witness names, GREEN after; the full defspec re-run at both recorded failing seeds, 150 trials each, green; full `make test` green; the oracle held pure identity across all 35 roots, confirmed by an actual `bin/regression-oracle 6827f5b f9fbeca` run (not merely re-asserted from ADR-0122's own zero-reach argument); zero `sim-engine`/`sim-trajectory` `src`, zero other invariant, zero `docs/manual` touched

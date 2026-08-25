## ADR-0169 — arc 0: quadratic removals under equivalence proof

**Status:** Accepted (author-ruled in the design channel, 2026-08-24, rulings
S1 and S2; executed by build session 2026-08-25).

### Context

The throughput spike (`.agents/session-records/2026-08-24-throughput-spike.md`,
landed at `d49f1c6`) measured, rather than assumed, where the simulator's time
goes at 10^5 events. Both phases are quadratic-shaped: generate slope **1.786**
and check slope **1.814** over the 10^4→10^5 decade. It named the sites by
source line:

* **Check** — six of 29 invariants are **99.4%** of the phase, and
  `occupancy-within-capacity` alone is **54.9%**. Four of the six are
  unconditionally O(N×P) (each walks the whole patient population once per
  event); two are O(C×N)/O(M×N) inner full-log loops gated on churn density.
* **Generate** — `replay` inside the two reinstating cancel decides is
  **35.3%** (a full `evolve` re-simulation of the entire log, allocating a
  vector of N maps, to read ONE element at an index already in hand); the two
  ADR-0164 citation scans are **21.3%** + **10.9%**.

Memory is linear at this scale (190 MB live against the shipped 3.88 GB
ceiling). The quadratics are time only.

#### The channel error this ADR corrects

ADR-0168 put the generator into a declared-reshuffle era, and
`rulings.md#R-per-person-streams-before-generator-fixes` records that
per-person RNG streams precede the traffic-scale generator arcs. The program
plan then wrote, of arc 3's quadratic removals (`.agents/plans/2026-08-24-traffic-scale-program.md`
line 43 as of `d49f1c6`), verbatim:

> the O(n^2) decide-time scan removals (carry order indexes in fold state, the
> :result-available pattern) — REQUIRED at 10^5 scale, and a generator
> change, so it lands within this era, never before arc 1.

That sentence **conflates "generator change" with "draw-affecting change".**
The reshuffle hazard R-per-person-streams guards against is that the engine's
one shared RNG makes any change to the *sequence of draws* reshuffle the whole
population. A refactor that consumes the identical draws in the identical
order and emits the identical events has no such hazard, whatever file it
edits. The ordering constraint was therefore stated one category too wide.

### Decision

**S1 (author ruling, verbatim): "a"** — output-identical refactors
(byte-identical corpus + identical findings at fixed seeds) are EXEMPT from
the reshuffle-era constraint; only draw-affecting changes wait for the stream
migration.

**S2 (author ruling, verbatim): "a"** — commission arc 0 (performance) ahead
of arc 1, one session: (i) six occupancy/churn invariants → fold-carried
incremental state; (ii) replay-per-cancel → read the one element in hand;
(iii) fold-carried order indexes retiring the ADR-0164 decide scans. All gated
by byte-identity + findings-identity tests co-landed.

#### The equivalence obligation, operationally

A pure refactor does not owe red-before-green — there is no new behaviour to
witness failing. It owes something **stronger**: proof that nothing moved. In
this repo that proof is two claims, asserted by tests that land in the SAME
commit as the refactor they gate, and BORN GREEN on the unrefactored tree so
that the gate itself is witnessed passing before it has anything to catch.

1. **Byte-identical corpus.** For every entry in `gated-runs`
   (`components/sim/test/ehrt/sim/run_test.clj`), the SHA-256 of the
   ground-truth log **as the shipped writer serialises it** — the
   `bin/regression-oracle` idiom: digest what `ehrt sim run` actually writes,
   never a `pr-str` invented in a test — is pinned against a committed
   baseline. Where the corpus is small enough to commit whole, value-identity
   (`=`) of `:ground-truth` against the same baseline is asserted alongside,
   and a mismatch names the **first differing event index**, not merely
   "digest mismatch".

2. **Identical findings.** The self-check gates already assert the gated runs
   are CLEAN, so "identical findings" there is "still clean". For the
   non-empty case the six invariants' discrimination tests assert the **full
   finding map** — `:invariant`, `:patient-id`/`:bed`/`:ward`, `:at`,
   `:occupied`/`:capacity` as each emits at `d49f1c6` — not merely that
   something was returned. Additionally each rewritten invariant keeps its
   **original body, verbatim**, as a `naive-*` reference oracle in the test
   namespace, and a pinned-seed `defspec` asserts `(= (naive-x log) (fast-x
   log))` over generated churn-bearing logs.

`bin/regression-oracle` is **necessary but NOT sufficient** for this arc, and
is run as a bracket rather than relied on as the proof: `digest.clj`'s own
vacuous-set note (`components/oracle/src/ehrt/oracle/digest.clj`) records that
the 35 golden roots reach **none** of the cancel family, **not** `engine/replay`,
and **not `sim-check` in its entirety**. An IDENTICAL verdict from the oracle
says nothing at all about three of this arc's three site families. The gates
above are what actually carry the claim.

#### Scope — exactly three site families

IN, and nothing else:

* **(i)** `no-double-occupancy`, `admitted-occupies-one-slot`,
  `outpatient-patients-occupy-no-bed`, `occupancy-within-capacity`,
  `cancel-references-existing-uncancelled-event`,
  `no-events-after-merged-terminal` (`components/sim-check/src/ehrt/sim_check/check.clj`).
* **(ii)** the `(:before (nth (replay ground-truth) idx))` read in
  `decide :cancel-transfer` and `decide :cancel-discharge`.
* **(iii)** the `:medication-end` and `:care-plan-end` whole-log citation
  scans ADR-0164 scoped by patient but did not shorten.

OUT, rowed rather than touched, per `rulings.md#R-move-not-improve` (one
sanctioned improvement per site; anything else seen is a finding):

* `sim-model/occupancy-board` folding every patient ever created, because
  `run`'s `init-world` seeds `:patients` with all of them at t=0 (8.1% of
  generate).
* `decide :discharge`'s waiting-boarder `filter` + `sort-by` over all patients
  (~7.9% of generate).
* The **14 independent `engine/replay` calls** in `check.clj` — 14 separate
  full folds of the log per `check-all`.
* `last-uncancelled-index`'s own 2×O(N) per call (5.9%), admitted to family
  (ii) only if the same carrier answers its query without a second code path.

### Consequences

The reshuffle era stands for every draw-affecting change; what it no longer
does is block work that cannot possibly reshuffle. Arc 1 (stream-partition
design) is unaffected and still precedes arcs 2–4.

Arc 3's `roadmap.md#engine-fold-extensions` row loses the quadratic removals,
which are now this arc's; what remains there is the genuinely draw-affecting
half (demographic timeline, scheduling state, bed-status cycle, the new
invariant families).

The `naive-*` reference oracles are a permanent cost: six invariant bodies now
exist twice, once as the shipped implementation and once as the test-only
definition of what it must equal. That duplication is the price of the proof
and is deliberate — a future change to any of the six must move both, and the
`defspec` is what notices if it does not.

# Arc 4 sweep 2 — chatter and DFT^P03 (ADR-0175 designs (a) and (c), ruling B1)

2026-08-28. Base `1a71b36`, last payload commit `42864aa`; this
record and the CI marker follow it. Nine payload commits. Ceremony: R30
(commit and push at each checkpoint), taken from the session prompt.

`bin/preflight` ran first, exit 0, no findings. Last five CI runs on
main all green; tree clean; HEAD matched `origin/main`; HEAD not tagged
`stable-*`, disclosed and correct — no tag is paid.

## What landed

| sha | commit |
|---|---|
| `9a765be` | re-statement chatter A08/A31/A28/IN1, DARK (design (a)) |
| `27f1694` | DFT^P03 charges, DARK (design (c)) |
| `29b581f` | `gate v2 --sample-add-ons`, DARK (design (h), ruling D1) |
| `964171a` | the board's foreign-trigger skip re-points A08 -> A34 |
| `d0a3624` | guard the sampler's own read, and gate what it cannot classify |
| `87aa8d2` | a DFT rides its basis event's latency, DARK |
| `1e3119f` | pad a spooled corpus's msg index to the width it needs |
| `ab4cda0` | chatter and charges TURNED ON in six corpora, plus `chatter-charges`, the 40th root |
| `42864aa` | re-review the straddle timeline at the chatter turn-on |

## Both brackets

**The dark batch** (`1a71b36` -> `d0a3624`, before any config opted in):

```
bin/ground-truth-bracket 1a71b36 HEAD
--- coverage: 36 roots carry :ground-truth and are digested;
    3 skipped (no such key): appendicitis.edn, ear-infections.edn, sore-throat.edn ---
--- THIS IS NOT A REGRESSION-ORACLE CLAIM: the :hl7 half of every root is
    excluded by construction ---
IDENTICAL: every digested root's :ground-truth matches between 1a71b36 and HEAD (36 roots)

bin/regression-oracle 1a71b36 HEAD
IDENTICAL: every root's digest matches between 1a71b36 and HEAD
```

No declaration on either. That is what DARK means and it is the
strongest line in this record: three commits of mechanism — a new
planner, a new message family, a new gating policy — and not one byte
of ground truth or of any wire moved.

**The turn-on** (`1a71b36` -> `42864aa`), both under
`--declared-digest-change`, which the SOUNDNESS check demands because
`digest.clj` itself gains a root:

```
bin/ground-truth-bracket 1a71b36 HEAD --declared-digest-change
  36 pre-existing roots' :ground-truth digests IDENTICAL
  1 line ADDED: chatter-charges.edn
  0 lines changed, 0 removed

bin/regression-oracle 1a71b36 HEAD --declared-digest-change
  39 pre-existing roots' digests IDENTICAL
  1 line ADDED: chatter-charges.edn
  0 lines changed, 0 removed
```

**THE MOVER SET IS EMPTY, AND THAT IS THE PREDICTION HOLDING RATHER
THAN A GAP.** The prompt asked for "the opted-in engine-layer roots";
none of the six opted-in corpora IS an oracle root — they are the four
`gated-runs` and the two demo scenarios — so the only new digest is the
root this sweep adds. Every one of the 39 is a non-mover, `dermatitis`
and `veteran-self-harm` (sweep 1's two ZERO-MESSAGE roots) included:
IDENTICAL on those two is an empty population and proves nothing about
emission, which is exactly why the evidence for this sweep is the six
gated corpora and the new root, never the 39.

Neither script has a vocabulary for an ADDED root — both print a
unified diff and exit 1 — so "IDENTICAL plus one `+`" is a reading of
the diff, stated here with the counts that back it.

## The witness table, every column measured on the shipped configs

A08 is split into its EVENT-DRIVEN and PERIODIC halves throughout,
because ADR-0175 section 2(a) names an event-driven-only reading of an
A08 witness as the miss.

```
corpus                     events   msgs  A08ev A08pd    A31   A28   IN1   DFT  lines  procLines
seed-202-ed-tuesday          1,213  1,263     2    34    263   103    72   114    158      0
seed-424242-clinic-decade    1,774  1,882     0   141    718   227   240   124    339      9
seed-5-clinic-decade         1,412  1,455     0    58    562   224   190    96    140      6
adhd-seed-45                    97    103     0     4     36    14     8     8     14      0
ed-tuesday (demo)            1,269  1,447     5   130    288   116   117   126    167      0
clinic-decade (demo)         1,569  1,629     0   102    616   230   217   106    162      6
```

**THE EVENTS COLUMN IS UNCHANGED IN EVERY ROW.** Read it against the
messages column: that is what an emission add-on is.

**469 of 478 A08s ARE PERIODIC.** Four of six corpora produce zero
event-driven A08s; the other two produce 2 and 5. ADR-0175 section
2(a)'s prediction — the person process walks twenty years while the
clinical content is a shift or a decade of short visits, so demographic
churn happens almost entirely BETWEEN encounters — holds across six
independent populations. A sweep that shipped only the event-driven
half would have satisfied the letter of design (a) and almost none of
its point.

**NOT EVERY CELL IS `pos?`.** `adhd-seed-45` and both ed-tuesday
corpora produce ZERO procedure charge lines: `:procedure` comes only
from vendored GMF modules and ed-tuesday's module tail is documented as
producing no live encounters at its horizon. FT1-25 is witnessed 9, 6
and 6 times across the three clinic-decade corpora and at unit scale;
it is not asserted where the population is empty.

**The skip census, which is the evidence the price table is CONFIG.**
Unpriced codes counted rather than silently dropped: 8 distinct codes at
`seed-424242-clinic-decade`, 1 at `seed-5-clinic-decade`, 10 at the
clinic-decade demo, 0 at the ed-tuesday corpora and adhd (which have no
procedures at all). `plan-charges` never invents an amount and never
reads ground truth for one.

## The sampling table (design (h), ruling D1)

`bin/ehrt gate v2 --sample-add-ons 5` over the shipped ed-tuesday demo
corpus, 1,447 files:

```
stratum     n      gated   disposition
ADT^A01     126    126     full (skeleton)
ADT^A02      21     21     full (skeleton)
ADT^A03     126    126     full (skeleton)
ADT^A04      21     21     full (skeleton)
ADT^A12       6      6     full (skeleton)
ADT^A13       1      1     full (skeleton)
ADT^A17       9      9     full (skeleton)
ADT^A20     421    421     full (skeleton)
ADT^A40       1      1     full (skeleton)
ORM^O01      25     25     full (skeleton)
ORU^R01      25     25     full (skeleton)
ADT^A08     135      5     sampled (add-on)
ADT^A28     116      5     sampled (add-on)
ADT^A31     288      5     sampled (add-on)
DFT^P03     126      5     sampled (add-on)
```

1,447 gated in full (15.4 s, all pass) against 802 sampled (12.7 s, all
pass). The saving is modest at this corpus size and is not the point:
the policy exists for the 10^6-message case ADR-0175 section 2(h)
priced at ~88 minutes, and what this run demonstrates is that the
per-stratum census is PRINTED — `:sampling {:cap 5 :strata {...}}` on
the report's own `:run` map — so a cap is never silent.

## Draw-consumption proof

`plan-chatter` takes exactly two passes and BOTH have log-determined
draw counts: one `.nextDouble` per ground-truth event in log order, then
one per patient-day of open-encounter care. Neither count depends on the
config, so a rule turned off still draws and discards.

Asserted three ways rather than described:

* `two-configs-differing-in-one-rule-draw-identically-for-everything-else`
  runs three pairs (registered on/off, coverage-change on/off, periodic
  on/off) and compares every instruction the UNCHANGED rules produced.
  RED under the obvious mistake: making the draw conditional on
  coverage reddens it immediately.
* `plan-chatter-is-a-pure-function-of-rng-log-and-profile`, 50 trials.
* At population scale, `chatter-and-latency-are-independent-and-decorrelated-streams`.

**AND ONE GATE WAS VACUOUS AS FIRST WRITTEN, found by mutation.**
Pointing chatter at latency's own `:emission` id-tag 0 left every
assertion GREEN. It cannot fail: each planner is handed its OWN
`java.util.Random`, so a tag collision CORRELATES the two streams
rather than shifting either. What the id-tag actually buys is
DECORRELATION (ADR-0171 ruling C1's own `mix64`), so the gate now
asserts that tags 0 and 1 produce different draw sequences — which can
fail — and says in its own body why the other half cannot.

## ADR premises contradicted

1. **Section 4's replacement derivability law is internally
   inconsistent.** It says *every message maps to exactly one
   (basis-event-index, trigger, ordinal) triple, which is what its
   MSH-10 carries*. Section 2(a) scopes the ordinal to `(mrn, trigger,
   t)`, so two periodic restatements inside ONE patient-day share a
   basis (their encounter's opener), a trigger and an ordinal 0, and
   differ only in the INSTANT. Measured at `:rate-per-patient-day` 2.0:
   eight instructions, seven distinct triples. MSH-10 is
   `mrn-trigger-t-ordinal` and IS unique; the instant is what section 4
   dropped. The gate asserts the four-part key.
2. **Section 2(c)'s FT1 mapping is too narrow.** It maps `:procedure`
   -> FT1-25 and `:order-placed` -> FT1-7. FT1-7 is the TRANSACTION
   code — a charge line without one is not a chargeable line — so every
   line carries FT1-7 and a procedure line additionally carries FT1-25.
3. **Section 2(c)'s "this is what `event->messages` was already shaped
   for" is right, but not the way it reads.** `event->messages` returns
   `[]` for any kind outside `message-type-registry`, and
   `:outpatient-visit-end` is deliberately outside it — so the charge
   branch had to sit OUTSIDE that guard, not inside the `cond`.
4. **Section 2(a)'s "no fold is needed" for the A08/A31 split SURVIVED
   a challenge and is confirmed.** Probing found two events carrying an
   `:encounter-id` long after their encounter's `:discharge`, which
   looks like the stamp outliving the encounter. It is not: a
   `:cancel-discharge` at the same instant reinstated that encounter and
   nothing ever re-closed it, so the encounter is genuinely open for
   1,432 more days. Reading the stamp is correct; a fold keyed on
   `:discharge` would have been wrong.

## Findings, one line each

1. **`msg-%03d` overflows at 1,000 messages and a corpus stops
   replaying in its own order.** `intake` walks a spool sorted by PATH
   and `player` treats that as semantic, so `msg-1000.hl7` sorted
   between `msg-100` and `msg-101`. Latent since the generator was
   written; nothing here had ever emitted 1,000 messages from one seed
   until this sweep took the ed-tuesday demo from 782 to 1,447. Fixed in
   `1e3119f`, width-padded and floored at three so no existing corpus
   moves. It surfaced as a bed board showing patients admitted years
   after their own discharge.
2. **A DFT overtook the ADT^A03 it accompanies by 46 minutes**, because
   the offset was looked up under `mrn-P03-t`, which no `:latency`
   profile keys on. Fixed in `87aa8d2`; it is also what saved the
   straddle drawing.
3. **Two tests used A08 as their example of an UNFOLDABLE trigger** and
   stopped being true the moment chatter co-landed its fold arm. One was
   caught inside `29b581f`, the other only by the FULL suite —
   `feedback_repo_gate_ordering`'s rule earning itself twice in one
   session.
4. **`sampled-gate-entries` called `slurp` with no guard** (ADR-0096),
   caught by `cli-parse-guard-lint` in the full suite. Fixed so an
   unreadable file lands in the `unknown` stratum and is gated in FULL.
5. **`gate-dir` and `sampled-gate-dir` both NPE on a genuinely
   unreadable file inside a gated DIRECTORY** — `(:payload (gate-file
   f))` hands an ERROR map to `report/build-report`. Predates this sweep
   (`gate-dir` has done it since ADR-0011) and behaves identically with
   and without `--sample-add-ons`. Rowed here, not fixed.
6. **`demos/scenarios/ed-tuesday/README.md` carried four stale
   transcript excerpts and `docs/manual/05-batch-delivery.md` told the
   straddle with the wrong patient, times and batch.** Two of the
   README's board snapshots named instants no run has produced for
   several sweeps; the manual said `Smith, James ... batch-001.hl7`
   while `bin/demo-exerciser-ed-tuesday` has asserted `batch-002.hl7`
   since 2026-08-27. Nothing gates a transcript excerpt. All re-witnessed
   against one fresh execution of each README's own commands.
7. **One ed-tuesday encounter is open for 1,432 days (6,144 at the demo
   seed)** — reinstated by a `:cancel-discharge` and never re-closed —
   and it is 89% and 97% of that scenario's patient-day census. That is
   why `config.edn` ships `:rate-per-patient-day 0.02` where
   clinic-decade ships 0.25. The stay is GROUND TRUTH and arc 4 changes
   none of it.

## Re-pinned

`bin/demo-exerciser-ed-tuesday`'s batch count 186 -> 615 (twice); both
scenario READMEs' headline figures, board excerpts, closing summaries
and batch sections; `docs/manual/05-batch-delivery.md`'s wrapper bytes,
straddle narrative and provenance table; `components/corpus/docs/
use-cases.edn`'s straddle sentence (and `docs/use-cases/` regenerated
from it); `docs/manual/assets/straddle-timeline.svg`'s three window
counts, 5/9/11 -> 9/14/16; `witnessed-message-types` (+4);
`oracle_coverage_test`'s root counts, 39 -> 40 and 36 -> 37;
`.agents/state-derived.md`; `docs/cli.md`.

**Did NOT move, and each is a witness that ground truth held:** the four
`arc0_gated_*` fixtures and `run_test`'s `arc0-pinned-digest` (a red
there is a STOP, never a re-pin — fence F1); `demos/traces/**`, which
`make traces` regenerated with an empty `git status` because every trace
runs its own `demos/traces/*/config.edn` and none opts in; both
conformance baselines; `event-schema.edn` and its version, so
`classify-change` was never asked a question (fence: a chatter sweep
that touched the event schema would have crossed
`R-skeleton-or-emission`).

## Gates, and the close

* `make test` — **`MAKE_EXIT=0`**.
* `make traces` — `TRACES_EXIT=0`, no diff.
* `make integration` — **`INT_EXIT=0`**, both demo exercisers and all six
  use-case scripts green, tree clean.
* `clojure -M:poly check` green throughout.
* `bin/post-push-verify 1a71b36 3b6e53b` — remote tip matches HEAD,
  every commit message in range pure ASCII, CI run reported once
  (queued at the time, DISCLOSED per AR-CI-4 and awaited below).
* **CI run 33172510496, `conclusion=success` at `3b6e53b`** — the close
  marker (`rulings.md#R-session-verifies-ci-via-gh`). No tag paid.

The RED-FIRST commits this sweep carried, all pushed with their green
successors and never alone (`rulings.md#R-red-pushed-with-green`):
`9a765be`..`29b581f` were red on `ehrt.corpus.board-test` until
`964171a`; `29b581f` was red on `cli-parse-guard-lint` until `d0a3624`;
`ab4cda0` was red on `hand-owned-asset-freshness-test` until `42864aa`.
The pushed TIP is green, and CI at that tip is the proof.

A NOTE ON WHAT THE SWEEP DID NOT DO. ADR-0175's fences held: no ground
truth change of any kind, no new event kind, no schema bump, no SIU, no
NK1, no fan-out, no MLLP. `engine/config-keys` is untouched.
`person-simulator` limitations rows 5 and 8 stand. And no oracle root
places an order — deliberately, so that sweep 3 can price that as its
own step 0 rather than inherit it half-done.

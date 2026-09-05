# 2026-09-05 — Q11 re-ruled (c): the finding vocabulary as a test-side law, and the catalog-wide gate

Roadmap row `roadmap.md#event-mutation-catalog-gate`, PRIORITY 6, both
live follow-ons. Ceremony mode: R30 (commit and push at each
checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-05-q11c-catalog-wide-gate.md`](../prompts/2026-09-05-q11c-catalog-wide-gate.md).
Reasoning-of-record: `notes/adr/0176-event-stream-mutation.md` section 8.

Rulings in force: **Q11(c)** (2026-09-05, superseding Q11(a)),
**R-c**, **R-wide**, **R-rider**, **R-pins**.

## 0. Preflight

`bin/preflight` exit 0, no findings. Two disclosures it printed and
this record repeats: a run among the last five (Integration at
6fe6811e) was still in progress and was NOT awaited to conclusion
(AR-CI-4), and HEAD was not tagged `stable-*` — which is correct, since
`rulings.md#R-tag-law` is RETIRED. Tree clean including untracked,
edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), HEAD equal to
`origin/main` at 6fe6811e.

## 1. What landed

Four commits, in the prompt's own order.

**e2a00f8 — `test: every declared finding is a checker invariant -- RED`.**
The red half. `checker-vocabulary` deliberately returns `#{}` so both
new tests run against a vocabulary that has not been derived yet.

**12430760 — `test: the finding vocabulary is a law (Q11(c))`.**
The derivation, off `check`'s four catalogs.

**8c5379a3 — `test: the catalog-wide oracle loop (ADR-0176 2(iv))`.**
The loop over every sited `(operator, population)` pair, the unsited
report, and the `declared-shape-gaps` register the run below forced.

**0e7646fc — `docs: close ceremony -- background processes terminated, no hand-rolled waiters`.**
R-rider, net-zero in `SKILL.md`.

## 2. Q11(c): the block was an artefact, and `poly check` proved it

Q11(a) rowed the vocabulary cross-check as BLOCKED because the only
costed shape was widening `ehrt.sim-check.interface`. **A test may
reach any namespace**, so the law is a corpus-brick test requiring
`ehrt.sim-check.check` directly. Measured rather than assumed:
`clojure -M:poly check` is **OK** over that require — poly's
brick-boundary rule governs a brick's own `src`, not its test tree.
`ehrt.sim-check.interface` is untouched, so the AR-M4-3 frozen-surface
baseline never came into it.

**The vocabulary is derived from the catalog vars, and the derivation
is exact rather than conventional.** Measured this session:

```
$ grep -o ':invariant :[a-z0-9-]*' components/sim-check/src/ehrt/sim_check/check.clj \
    | sed 's/:invariant //' | sort -u | wc -l
46
$ (the four catalogs' var names, keyworded and sorted)
46
$ diff  ->  IDENTICAL
```

46 catalog vars against the 46 distinct `:invariant` keywords the whole
of `check.clj` carries, name for name and set for set. It is also
self-policing in the file: the oracle loop asserts observed = declared
over REAL violation keywords while the law asserts declared ⊆ derived,
so a var whose emitted keyword drifted from its own name turns one of
the two red.

### Red, verbatim (`brick:corpus project:ehrt-cli`)

```
FAIL in (an-unknown-declared-finding-is-named-not-tolerated-test) (event_mutate_test.clj:590)
the offender, named by id and by the finding it invented
expected: (= #{[:dummy-unknown-finding :no-such-invariant]} (unknown-declared-findings))
  actual: (not (= #{[:dummy-unknown-finding :no-such-invariant]} #{[:null-medication-end-order-event-id ...

FAIL in (every-declared-finding-is-an-invariant-check-can-produce-test) (event_mutate_test.clj:564)
the four catalogs must yield a vocabulary at all (R-empty-population-is-red)
expected: (seq (checker-vocabulary))
  actual: (not (seq #{}))

FAIL in (every-declared-finding-is-an-invariant-check-can-produce-test) (event_mutate_test.clj:566)
an event operator declares a finding `check` cannot produce. ...
expected: (= #{} (unknown-declared-findings))
  actual: (not (= #{} #{[:null-medication-end-order-event-id ...

Ran 12 tests containing 910 assertions.
3 failures, 0 errors.
```

Exactly the two new deftests, nothing else in the brick — the prompt's
step-1 gate. **The real catalog yielded NO offender**, so step 2's STOP
condition (a shipped operator promising a finding `check` cannot make)
did not fire and `operators.clj` was not touched.

**Disclosed: the red is a stubbed-derivation red, not a
stash-isolated one.** Step 7 of the build-session skill isolates a
src fix from its own test; there is no src fix here, and the law is
green on a healthy tree by construction, so the only honest way to
show both tests red was to land them against an empty vocabulary
first. What proves the law can go red on real data is the second
test, which registers a synthetic operator declaring
`:no-such-invariant` and requires it named by id and by finding.

Green: `clojure -M:poly test brick:corpus`, all three composing
projects, **5571 passes / 0 failures / 0 errors**, exit 0, against a
5559-pass baseline at 6fe6811 — delta +12, being 4 assertions x 3
projects. Wall 414.06 s against 421.10 s: the law reads vars, so it
costs nothing measurable.

## 3. The catalog-wide gate, and what it found

45 of 78 pairs sited. The 33 unsited are printed by name on every run:

```
DISCLOSURE: catalog-wide oracle loop -- 45 of 78 (operator, population) pairs
are sited and run the loop; 33 offer no candidate site:
  no site: :phantom-order-event-id over :clinic-decade
  no site: :cross-patient-order-event-id over :clinic-decade
  no site: :wrong-kind-order-event-id over :clinic-decade
  no site: :inverted-span-order-event-id over :clinic-decade
  no site: :orphan-participant over :ed-tuesday
  no site: :phantom-cancels-event-id over :clinic-decade / :ed-tuesday
  no site: :cross-patient-cancels-event-id over :clinic-decade / :ed-tuesday
  no site: :wrong-kind-cancels-event-id over :clinic-decade / :ed-tuesday
  no site: :inverted-span-cancels-event-id over :clinic-decade / :ed-tuesday
  no site: :phantom-medication-end-order-event-id over :clinic-decade / :ed-tuesday
  no site: :null-medication-end-order-event-id over :clinic-decade / :ed-tuesday
  no site: :cross-patient-medication-end-order-event-id over :clinic-decade / :ed-tuesday
  no site: :wrong-kind-medication-end-order-event-id over :clinic-decade / :ed-tuesday
  no site: :inverted-span-medication-end-order-event-id over :clinic-decade / :ed-tuesday
  no site: :phantom-start-event-id over :clinic-decade / :ed-tuesday
  no site: :null-start-event-id over :clinic-decade / :ed-tuesday
  no site: :cross-patient-start-event-id over :clinic-decade / :ed-tuesday
  no site: :wrong-kind-start-event-id over :clinic-decade / :ed-tuesday
  no site: :inverted-span-start-event-id over :clinic-decade / :ed-tuesday
```

(The run prints one line per pair; the `/` pairs above are two lines
each in the real output — 33 lines in all.) That set is a property of
the corpus, not a defect: clinic-decade mints no `:medication-end` or
cancel family, ed-tuesday no cancel family or care-plan spans. It is
REPORTED rather than pinned, and what is gated instead is that the
matrix is complete (26 x 3 = 78), that every row's own declared
population is sited, and that no operator is unsited everywhere.

### THE FINDING — and it is a STOP-AND-REPORT

The first run of the wide gate went red on exactly one pair:

```
FAIL in (the-closed-oracle-loop-holds-for-every-sited-pair-test) (event_mutate_test.clj:383)
:orphan-participant over :dense-7500 (48 sites) steps 4 and 5 -- the loop closes, on EQUALITY (Q5(a))
observed = declared, exactly
  actual: (not (= #{CLIN ENC PID REG}
                  #{CLIN ENC PID REG :medication-end-references-existing-order-and-follows-it-in-time}))
```

Characterised rather than patched. Sixteen seeds first (all four
findings — the divergence is site-dependent, not seed-independent),
then **exhaustively over all 48 candidate sites**:

| sites | observed set |
|---|---|
| 34 | the declared four |
| 6 | + `medication-end-references-existing-order-and-follows-it-in-time` |
| 8 | + `care-plan-end-references-existing-start-and-follows-it-in-time` |

Seed 424242 draws site 122, one of the six. **This is a shape gap in
ADR-0176 addendum (c)'s own sense** — an observed set that varies site
to site — on an operator addendum (c) had already narrowed once. The
mechanism is that addendum's own point 2 one layer on: the narrowing
derives its site list from `check`'s
`clinical-content-only-when-admitted`, whose kind list contains the
span STARTS, so on a log that CLOSES its spans the span's referential
invariant convicts too. Neither calibration log closes a span.

**Three defensible readings** — narrow again, retire the operator, or
ratify a measured per-pair set — so this is STOP-AND-REPORT under
`rulings.md#R-stop-only-on-two-defensible-readings`, not fix-forward.
Widening `:expected-findings` is not among them: Q5(a) is equality, so
a five-element declaration goes red on clinic-decade.

**What was landed pending the ruling, disclosed as a deviation from
R-wide.** R-wide said every sited pair runs the loop with Q5(a)
equality; one does not. The pair is carried in `declared-shape-gaps`,
which is neither an exemption nor a skip: the pair still runs the FULL
loop and still asserts set EQUALITY, against the set measured at the
site the gate's own seed draws, and
`every-declared-shape-gap-actually-diverges-test` asserts it really
does diverge from its declaration — so narrowing the operator turns the
register entry red and forces its deletion. It cannot decay into a
silent pass in either direction. Rowed at
`roadmap.md#orphan-participant-shape-gap`; `operators.clj` untouched,
per the prompt's fence.

### Cost, per R-wide

`clojure -M:poly test brick:corpus project:ehrt-cli`, stash-isolated
before/after at 12430760:

| | wall |
|---|---|
| before | 148.72 s |
| after | 230.51 s |
| delta | **+81.79 s per project** |

Under the 120 s rule, so the gate stays in `make test` rather than
moving to `make integration`. Note the multiplier this figure carries:
`make test` runs `poly test :all skip:integration`, so corpus's tests
run in TWO projects there and the third under `make integration`.

## 4. The rider

`SKILL.md`'s close step (13) gains two postconditions and the Done-when
checkbox names the first: enumerate and TERMINATE every background
process the session started, before the close marker; never hand-roll
an `until` waiter for one. Net-zero by compaction inside the file —
step 11 8 -> 6 lines (the tag-retirement narrative folds into its own
ruling row), step 13 5 -> 7, step 15 6 -> 5, the checkbox 2 -> 3.
`SKILL.md` 146 -> 146 lines; `:docs` actual 785 against budget 785 and
baseline 785, headroom 0, unmoved. `.claude/skills/` re-mirrored in the
same commit.

**Disclosed: this session broke the rule it wrote, before writing it.**
Two `Monitor` waiters were armed with hand-rolled `until grep -q ...;
do sleep 5; done` commands, and both timed out unused because the
background jobs' own completion notifications had already arrived.
That is exactly the waste the rider now forbids, found by doing it.

## 5. Findings, one line each

* **`:orphan-participant` is a shape gap on dense-7500** — section 3
  above, ADR-0176 section 8, `roadmap.md#orphan-participant-shape-gap`.
* **`poly check` does not police a brick's TEST tree** — measured, not
  assumed, and it is what made Q11(c) available at all.
* **The catalog var name IS the emitted invariant keyword**, 46 for 46,
  which is what lets the vocabulary be derived rather than transcribed.

## 6. Gates

| gate | result |
|---|---|
| `bin/preflight` | exit 0, no findings |
| `clojure -M:poly check` | OK, after every commit |
| `clojure -M:poly test brick:corpus` (step 2) | 5571 passes, 0 failures, exit 0 |
| `make test` (step 3/5) | **27423 passes, 0 failures, 0 errors**, `MAKE_EXIT=0`, wall 1479.20 s |
| `gitleaks git --staged -v` | no leaks, before every commit |
| `bin/post-push-verify` | see section 7 |

The `make test` figure is this session's own close figure and lives
here, not in ADR-0176 (build-session step 14). 414 namespace runs,
zero failures and zero errors across all of them. The wall carries no
claim: this repo's CI spread is 436-1302 s and a single local run is
not a performance measurement (`roadmap.md`-adjacent finding, the
validators-once record).

## 7. Push and CI

Pushed `6fe6811e..99320589` to `origin/main`, six commits. The red-first
commit e2a00f8 went out WITH its green successor 12430760, never alone
(`rulings.md#R-red-pushed-with-green`).

`bin/post-push-verify 6fe6811e 99320589`, all three checks:

```
-- 1. Remote tip vs HEAD --
OK: origin/main (9932058905e4...) matches tip (9932058905e4...)
-- 2. Per-commit ASCII check, 6fe6811e..99320589 --
OK: every commit message in range is pure ASCII
-- 3. CI run at tip --
CI run for 9932058905e4...: status=in_progress conclusion=<pending>
DISCLOSED: reported once, not awaited to conclusion (AR-CI-4)
```

**CI green at the pushed tip, awaited to conclusion by this session**
(`rulings.md#R-session-verifies-ci-via-gh`, the close marker): run
**33965913548**, `status=completed conclusion=success` at
`9932058905e4f897463b0b0a11cbac1a652be331`. No tag was paid
(`rulings.md#R-tag-law`, RETIRED).

**Background processes, per the rider this session wrote** (SKILL.md
step 13): every one enumerated and terminated before this marker — six
timed gate runs and two characterisation probes, all exited on their
own; the CI watcher stopped explicitly. `ps` at close shows nothing of
this session's left running.

## 8. AUTHOR ACTION

**The `:orphan-participant` disposition** —
`roadmap.md#orphan-participant-shape-gap`, ADR-0176 section 8. Narrow
`:candidate-sites` again to exclude a span START, retire the operator,
or ratify the `declared-shape-gaps` register as it landed. Until then
the register carries it, red-if-fixed in both directions.

No regression-oracle claim is made: this session changed no emitting or
folding code, only a test tree and two documents.

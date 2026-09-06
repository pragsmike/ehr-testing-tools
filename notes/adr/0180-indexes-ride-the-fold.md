## ADR-0180 — indexes ride the fold: the generate-quadratic program

**Status:** Accepted (author-ruled R-fold-carrier / R-equivalence /
R-order / R-gate-gaps, 2026-09-06). **CHARTER ONLY — this record enacts
no site.** Every line number below is at `3d9f5c24`, read this session.

### Context

`.agents/plans/2026-09-05-performance-measurement/measurements.md`
timed a 2,500 / 7,500 / 22,500-arrival decade on committed cells,
generate and check separately, as `sim run --format ground-truth` piped
into `sim check` — the prime audience's own invocation. Two of its
tables settle what this program is for.

**Where the pair's wall is** (`measurements.md:39-46`): at the top cell,
generate is **2,329.46 s** and check **130.57 s** — check is **5.3% of
the pair**, and it is linear (corrected decade slope 1.03-1.04 against
ADR-0169's 1.814, and `occupancy-within-capacity` 4.08% of the phase
against its 54.9%). Arc 0 did what it was commissioned to do. Generate
did not get the same treatment, and its corrected decade slope is
**1.851**, rising from a local 1.587 at the bottom to **2.116** at the
top (`measurements.md:74-79`) — a single exponent understates the next
decade.

**Where generate's time is** (`measurements.md:234-245`, inclusive
share, taken with `--stack-depth 2048` for the reason that table's own
preamble gives): `decide` as a whole is **70.35%** at the top cell, and
four sites inside it are **78.4 percentage points** of inclusive share
between them —

| site | 7,500 | 22,500 | ADR-0169's figure |
|---|---|---|---|
| `decide :discharge`'s `waiting-boarder` | 25.23% | **30.54%** | ~7.9%, rowed OUT |
| `run/select-person` | 14.17% | **19.98%** | on no list at all |
| `sim-model/occupancy-board` | 11.65% | **17.11%** | 8.1%, rowed OUT |
| `log-index/last-uncancelled-index` | 10.29% | **10.78%** | 5.9%, F-3 |

Three of the four are the three ADR-0169 declined to fix under
`rulings.md#R-move-not-improve`, at estimates made by inspection that
the samples put at between two and four times their stated share; the
fourth is on no list at all because ADR-0169 profiled a configuration
in which the person layer did not yet exist. The ranking, as a
recommendation and not a decision, is `measurements.md:294-318`.

**They are one shape.** Every one of them is a per-event or per-arrival
scan over a population that is only ever appended to, recomputed from
scratch at each use because no carrier holds the answer. And a carrier
already exists: `fold/apply-events` maintains four indexes per event
today — `:reinstate-index`, `:citation-index`, `:registration-index`
and `:bed-index` (`fold.clj:522-562`), each guarded by its own
projection-membership test and by nothing else, each written inside the
fold *for the same stated reason*: the pre-event and post-event patient
maps both exist at that point and nowhere later.

### Decision

Four author rulings, verbatim, ratified in the design channel
2026-09-06:

**R-fold-carrier.** *"Every remaining generate quadratic is fixed the
same way — an index maintained at `fold/apply-events`, with the
from-scratch definition kept as the gate that proves the index equal at
every replay entry."*

**R-equivalence.** *"Each site is its own session; every commit
bracket-proven; draw- or allocation-affecting sites carry an explicit
equivalence argument in the ADR before the session, not a bracket after
it."*

**R-order.** *"Sites in measured order: `waiting-boarder`,
`select-person`, `occupancy-board`, `last-uncancelled-index`. Check's
fourteen replays last."*

**R-gate-gaps.** *"`dense-7500`'s exerciser joins `make integration`;
the Scale table gets a gate."*

#### One disclosed narrowing of R-fold-carrier's LOCATION

R-fold-carrier names `fold/apply-events` as the carrier. That holds for
three of the four sites. It does not hold for `select-person`, and the
reason is mechanical rather than a matter of taste: `select-person` is
called from `prelude` (`run.clj:246`), which is *"everything `run`
computes before its loop starts"* — the whole `bindings` vector is
built over the arrival instants before a single event has been folded.
There is no `apply-events` at that point to hang an index off.

Fix-forward with disclosure (`rulings.md#R-stop-only-on-two-defensible-
readings`: one defensible reading, so this is not a stop). The ruling's
**law** — a maintained index whose from-scratch definition is kept as
the gate — applies to all four sites unchanged. Its **location**
applies to three; `select-person`'s carrier is `prelude`'s own
t-ascending arrival sweep, which has the property that makes an index
possible for exactly the same reason `apply-events` does: the state it
needs exists at that point and nowhere later.

### The four sites

Each site below states its scan, the index that replaces it, whether it
is draw- or allocation-affecting, and — per R-equivalence — the
equivalence argument, written here before the session rather than
asserted by a bracket after it.

#### 1. `waiting-boarder` — 30.54%

**The scan** (`decide.clj:983-991`): `(:patients world)`, minus the
excluded id, filtered to patients who are `:admitted`, hold a ward, and
are boarding — `(not= (:home-ward p) (get-in p [:location :ward]))` —
in the vacated ward, then `sort-by [(:admitted-at p) pid]`, then
`ffirst`. O(P) plus an O(k log k) sort, once per `:discharge`
(`:1198`) and once per `:bed-ready` (`:1275`).

**The index**: home-ward → an ordered set of boarders keyed
`[admitted-at patient-id]`, maintained at `apply-events`. Every event
that can move a patient into or out of that set changes at most its own
participants' `:status`, `:location`, `:home-ward` or `:admitted-at` —
which is exactly the pre/post pair the `:bed-index` concern already
reads at `fold.clj:545-562`.

**Draw-affecting: YES, through its result — not through its own
consumption.** The function draws nothing and says so
(`decide.clj:971-976`, "DRAWS. This function consumes none"). But a
non-nil answer takes a branch that draws twice: `bed-ready-transfer-
event` calls `bed-ready-location` with `world-rng`
(`decide.clj:1143-1160`), and `decide :bed-ready` additionally calls
`vacate-bed` with `facility-rng` (`decide.clj:1281-1283`). So *which*
id comes back, and *whether* one comes back at all, decides whether two
draw-consuming calls happen and against which patient.

**Equivalence argument.** The obligation is identity of the returned
id — not "a legal boarder", which is a strictly weaker claim an index
can satisfy while moving every byte after it. Two properties make that
identity available, and the index inherits both or it is wrong:

* The sort key `[(:admitted-at p) pid]` is a **total** order. The
  `patient-id` tiebreak is what makes the answer independent of the
  `:patients` map's own iteration order, so a from-scratch scan and an
  ordered index agree by construction rather than by luck. An index
  keyed on `:admitted-at` alone would be a *different* function that
  happens to agree on logs with no simultaneous admissions.
* nil-vs-non-nil is load-bearing on its own. A stale entry the index
  failed to evict emits a `:transfer` where the scan emitted nothing —
  and a missing entry drops one. Both reshuffle everything downstream.

The two callers pass different worlds — `:1198` passes `world` and
excludes the discharging patient; `:1275` passes `world'`, which
differs only in `(:beds world)` having one bed flipped to `:ready`.
A `:patients`-derived index is blind to that difference, so one carrier
serves both, and the excluded-id argument stays a caller-side filter
over the index's answer rather than a second index.

#### 2. `run/select-person` — 19.98%

**The scan** (`run.clj:168-176`): a `filterv` over the WHOLE
`:population`, per arrival, dropping people whose death instant has
passed (`(or (nil? d) (> d t))` — half-open, a person whose death
instant IS `t` is dead). One `.nextDouble` follows, taken whether or
not the filter removed anyone, and the result indexes positionally:
`(nth candidates (min (dec (count candidates)) (long (* draw (count
candidates)))))`. The provenance makes `:persons :count` **twice the
arrival count** by rule (`derive-cells.sh`, and the rule's own reason:
a smaller pool "would collide nearly every arrival onto an
already-registered person"), so the site is O(arrivals × persons) =
**O(arrivals²) by construction**.

**The index**: the alive set only ever SHRINKS across the call
sequence, because `arrivals` is t-ascending by construction
(`run.clj:238-239`, `reductions +`). So one pass over the population
sorted by death instant, with a cursor that advances as `t` does, plus
an order-preserving structure that knows its own count, replaces the
per-arrival `filterv`. Carrier: `prelude`'s arrival sweep, per the
disclosed narrowing above.

**Draw-affecting: YES, and this is the only one of the four whose index
content is read BY the draw.** The draw *sequence* cannot move — one
uniform per arrival, unconditionally, which is that function's own
fixed-consumption law. What moves is what the uniform resolves to,
since both `(count candidates)` and `candidates`'s positional order
feed the `nth`.

**Equivalence argument.** The obligation is the strongest of the four:
at every arrival instant the index must yield a vector `=` to
`(filterv alive? population)` — **same elements, same order, same
count**. Order is not incidental here; it is half the function. Two
consequences:

* The half-open convention travels with the index and is not
  re-derived. `run.clj:517-519` states the same convention a second
  time for hook minting, and cites `select-person` as its source; a
  carrier that read `>=` at one of those two sites and `>` at the other
  would be self-consistent and wrong.
* The `min` clamp is part of the function, not a guard: at `draw` very
  close to 1.0 it is what keeps the index in range. It moves with the
  site or the site changes behaviour at the boundary.

The population is FIXED at prelude time — no person is appended after
`prelude` starts — which is what makes a single sorted pass sufficient
and is the fact the session should assert rather than assume.

#### 3. `sim-model/occupancy-board` — 17.11%

**The scan** (`facility.clj:44-54`): `into {}` over the whole
`:patients` map, keeping `[bed patient-id]` for every patient holding
one. `run`'s `init-world` seeds `:patients` with EVERY patient at t=0
(`run.clj:1131-1132`), so P is the whole population from the first
event — ADR-0169's own stated reason for rowing it. Four call sites in
`decide` (`:1010`, `:1063`, `:1136`, `:1341`) and one in
`log_index.clj:182`.

**The index**: bed-id → patient-id maintained at `apply-events` off
exactly the `(:patients w)` / `(:patients w-next)` pair the
`:bed-index` concern already reads (`fold.clj:545-562`) — a second map
built in the same pass, not a second pass.

**Draw-affecting: NO in the ORDER sense; allocation-affecting in the
VALUE sense.** The board reaches a draw only through `free`
(`facility.clj:56-84`), which uses it as a **predicate** — `(remove
board ids)` over the derived, deterministically ordered `ids` list — so
the board's own iteration order never reaches `choose`'s `(.nextInt rng
(count candidates))` (`facility.clj:113-118`). Only its KEY SET does,
through the count and through which ids survive. `ward-census`
(`facility.clj:145-158`) likewise counts `(keys board)` against a
per-ward slot set, order-immune.

**Equivalence argument.** Value equality of the key set is what the
allocator needs; value equality of the VALUES is separately owed
because one site reads them as occupant ids —
`bed-reoccupied-by-someone-else?` at `log_index.clj:182`. Two wrinkles
the index must REPRODUCE rather than improve
(`rulings.md#R-move-not-improve`):

* **`into {}` has a hash-order dependence today.** Two patients in one
  bed leaves whichever comes LAST in the `:patients` map's own seq
  order. `no-double-occupancy` forbids that state, so it is unreachable
  in a log the checker passes — but the index must not silently answer
  differently on a log where the checker would have convicted anyway.
  Named, not fixed.
* **`decide.clj:1136` asks a different question**: `(occupancy-board
  (dissoc (:patients world) patient-id))` — the board with the subject
  removed. A maintained index answers that in O(1) by masking that
  patient's own bed, but it is a SECOND query shape and it is where an
  index most easily diverges from its definition.

`components/sim-model/docs/charter.md:170-182` states the law for this
site in words: **"The occupancy board is a definition, not a cache."**
An index makes it a cache. That charter line is amended in the SAME
commit as this site — not before, not after — and what replaces it is
the law in the next section: the board is still a definition, and the
index is proven equal to it at every replay entry.

#### 4. `log-index/last-uncancelled-index` — 10.78%

**The scan** (`log_index.clj:96-113`): TWO passes over the whole
`ground-truth` per call — one building `already-cancelled` from every
`cancel-type` event's `:cancels-event-id`, one `keep-indexed` taking
the LAST matching index. Flat across the decade (10.29% → 10.78%),
which makes it the least urgent of the four despite being the one
ADR-0169's F-3 left admissible.

**The index**: per `[patient-id event-type]` the ordered indices of
that patient's events, plus a per-`cancel-type` set of already-cancelled
indices — both derivable in the pass that already mints
`:citation-index` and `:registration-index` (`fold.clj:526-541`). F-3
admitted this site *"to family (ii) only if the same carrier answers
its query without a second code path"*; that condition is exactly what
this row now commissions, and a session that ends up writing a second
code path has failed the row rather than completed it.

**Draw-affecting: NO. Neither is it allocation-affecting.** The
function draws nothing, and its result is an integer written into
`:cancels-event-id` (`decide.clj:1330-1336` and the two reinstating
cancels) or nil, which `decide` turns into a structured rejection. Its
answer is a pure function of the log — and the log is what the fold is
folding, which is why the carrier can hold it at all.

**Equivalence argument.** Index-value identity: same integer, same nil,
at every call. "Not draw-affecting" means this site owes no draw-ORDER
argument; it does not mean it is cheap to get wrong. A nil where the
scan returned an integer turns a legal cancel into a `:step-rejected`,
which changes the event stream and therefore every draw after it — the
same mechanism that moved 21 `:cancel-transfer` events in ADR-0179's
own addendum. The bracket is what catches that, and the bracket is
owed here as everywhere.

### The law: the from-scratch definition is the gate

One test shape, instantiated four times. It is ADR-0169's `naive-*`
idiom, generalized from invariants to indexes:

1. **The from-scratch definition is KEPT, verbatim,** as a reference
   implementation in the test namespace. For sites 1, 3 and 4 that is
   the shipped body being replaced; for site 2 it is `select-person`'s
   own `filterv`.
2. **A pinned-seed property test over generated churn-bearing logs
   asserts `(= (naive-x state) (index-x state))` at EVERY replay entry**
   — not at the end. An index is a claim about every intermediate
   state, and an end-state-only assertion is precisely the assertion
   that passes on a carrier which is wrong for 500,000 events and right
   for the last one.
3. **`bin/ground-truth-bracket` per commit**, per R-equivalence. Note
   what it can and cannot say here: `bin/regression-oracle` hashes each
   root's `{:ground-truth :hl7}` pair as one digest, and the bracket is
   the `:ground-truth`-only sibling (AGENTS.md, ADR-0175 E1) — the
   right instrument for a change that must move no ground-truth byte.
   It is **necessary and not sufficient**, for the reason ADR-0169
   already recorded about its own arc: the oracle's golden roots are a
   population, not a proof, and `digest.clj`'s vacuous-set note names
   what they do not reach.
4. **A charter amendment where a charter states the law in words.**
   Site 3 owes one (`sim-model` charter §4); the other three do not.

### Sequencing (R-order)

One session each, in this order, each commit bracket-proven:

1. `waiting-boarder` — 30.54%
2. `run/select-person` — 19.98%
3. `sim-model/occupancy-board` — 17.11%
4. `log-index/last-uncancelled-index` — 10.78%
5. **Check's fourteen `engine/replay` calls** — 50.97% of the check
   phase (`measurements.md:274-281`), and 2.7% of the pair's wall. The
   carrier already exists: `replay` is a PROJECTION of `apply-events`
   rather than a fold of its own (`fold.clj:594-640`), so what this row
   buys is calling it ONCE and passing the entries, which
   `check.clj:929` already accepts (`(or (:records folded) (engine/replay
   ground-truth))`). Fourteen call sites, counted this session:
   `check.clj` `:215 :234 :284 :298 :399 :532 :731 :929 :1060 :1074
   :1174 :1308 :1758 :1785`. Worth roughly 65 s at the top cell —
   about one twentieth of what the same effort buys in generate, which
   is why it is last and not dropped.

Sites 1, 2 and 3 are draw- or allocation-affecting and their
equivalence arguments are above, written before their sessions. Site 4
is neither, and carries one anyway.

### What is NOT in scope

* **Constant factors.** Generate's self-time table is topped by
  `KeywordLookupSite$1.get` at 12.58% and `Util.equiv` at 9.62%
  (`measurements.md:265-269`). Both are true and neither names a site.
  No row, and a session that finds itself tuning keyword lookup has
  left this program.
* **`:persons` itself.** 4.10% of the top cell and linear; the
  2×-arrivals pool is the provenance's own rule, not a defect. What is
  in scope is the O(arrivals²) *scan over* that pool, which is site 2.
* **The emitters.** This program measures `sim run --format
  ground-truth` piped into `sim check`. Emission is a different phase
  with its own record (ADR-0175); nothing here licenses touching it.
* **Memory.** 3,973 MB peak RSS against a shipped 3.88 GB
  `MaxHeapSize` is why 67,500 arrivals is unreachable on defaults,
  measured and not extrapolated. That is a ceiling, not a quadratic,
  and closing it is not this program's business.
* **The `naive-*` duplication itself.** ADR-0169 declared it a
  permanent cost; this program adds four more definitions that live
  twice and declares the same.

### Consequences

**Five sessions, four of them in generate.** If every site lands at its
measured share the top-cell generate phase loses most of 78 points of
inclusive share — but shares do not sum, an outer frame's cost is
partly its callees', and this ADR predicts no wall figure. The next
measurement is what says what was bought; this record's job is to make
each session's claim falsifiable, not to pre-announce the answer.

**The projection algebra grows.** `fold/apply-events` goes from
thirteen concerns to at least sixteen. Each new index is guarded by its
own membership test and by nothing else — the contract that keeps a
site which does not opt in paying nothing for one it never reads. A
session that reaches for a second guard, or for an index computed
outside the fold and passed in, is doing something other than what this
charters.

**One charter invariant is amended, not deleted.** `sim-model`'s "the
occupancy board is a definition, not a cache" becomes "the occupancy
board is a definition, and the index is proven equal to it at every
replay entry". That is a weaker sentence and it is the honest one.

**Two gate gaps are owed alongside** (R-gate-gaps), rowed separately
because they are not site work: `bin/demo-exerciser-dense-7500` is
wired into neither `make test` nor `make integration`
(`.agents/plans/2026-09-05-7-event-divergence.md:112-115`), and
`docs/consuming-ground-truth.md`'s Scale table cites figures from that
same unwired scenario. The four generate sites will move nothing in
that table if they hold — which is exactly why it should be gated
BEFORE they land, so that a table which does move says so.

**Nothing here is enacted.** This ADR is the charter and its rows; the
sites are five sessions that have not run.

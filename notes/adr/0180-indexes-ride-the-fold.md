## ADR-0180 — indexes ride the fold: the generate-quadratic program

**Status:** Accepted (author-ruled R-fold-carrier / R-equivalence /
R-order / R-gate-gaps, 2026-09-06). **CHARTER ONLY — this record enacts
no site.** Every line number below is at `3d9f5c24`, read this session.

**EXTENDED 2026-09-06 by the addendum at the end of this record**
(author-ruled R-order-2 / R-hash-order / R-already-merged /
R-check-once, same day, same channel): sites 5, 6 and 7 --
`decide :merge`, `decide :bed-swap` and the in-run and standalone
`check-all` -- chartered, and the site numbering restated there.
Still charter only; still nothing enacted.

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

Fix-forward with disclosure: one defensible reading, so this is not a
stop (`rulings.md#R-stop-only-on-two-defensible-readings`). The ruling's
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

### Dated corrections

**2026-09-06 — two corrections from site 3's session**
(R-min-clamp-correction, and R-pins for the second -- both ratified in
the design channel 2026-09-06, like the four above).
Neither changes a decision. Both change a sentence a later reader would
otherwise take on trust, which is what a charter owes once sessions have
run against it. The sections above are left as they stood, and this
section is where they are read against.

**1. Section 2's `min` clamp is DEFENSIVE, not load-bearing.** That
section says the clamp "at a draw very close to 1.0 is what keeps the
index in range". Site 2's session probed it directly and it never fires:
for `draw` the largest value `.nextDouble` can return
(`Math/nextDown 1.0`), `(long (* draw n))` was `< n` for every `n` in
1..200,000 — zero hits in IEEE-754 double arithmetic
(`.agents/session-records/2026-09-06-adr-0180-site-2-select-person.md`
section 5). **THE CLAMP STAYS**, and nothing was changed on the strength
of the finding: R-move-not-improve says the body moves character for
character, and it did. What survives of the original sentence is its
second half — "it moves with the site or the site changes behaviour at
the boundary" — which was always the load-bearing claim. What is
withdrawn is the first half's implication that the engine currently
depends on it.

**2. Section 3's five call-site line cites, refreshed.** They were
written on 2026-09-05 and were 23 lines stale by the time site 3's
session opened, which is the ordinary fate of a line cite in a file
under edit. As of `24f7915e` — the commit that repointed them — the five
sites read the index rather than rebuilding the board, and are:

| site | cited here as | at `24f7915e` |
|---|---|---|
| `decide :admission` | `decide.clj:1010` | `decide.clj:1047` |
| `decide :transfer` | `decide.clj:1063` | `decide.clj:1102` |
| `decide`'s `bed-ready-location` (the `dissoc` query) | `decide.clj:1136` | `decide.clj:1184` |
| `decide :transfer-in-error` | `decide.clj:1341` | `decide.clj:1390` |
| `log-index/bed-reoccupied-by-someone-else?` | `log_index.clj:182` | `log_index.clj:193` |

**AND SECTION 3'S CENSUS OF THEM WAS ONE SHORT.** It says "Four call
sites in `decide` and one in `log_index.clj:182`"; there is a SIXTH,
`ehrt.sim-check.check`'s `surge-only-when-earlier-rungs-exhausted`
(`check.clj:935` at the time this ADR was written, unmoved by site 3).
It is in the CHECK phase and calls the definition over a replay entry's
`:world-before`, not over a world — and since `replay` deliberately does
not carry `:board`, it is a site the index does not reach and correctly
still rebuilds. The omission changed no decision, because that site was
never in scope; it is recorded because a reader counting call sites
against the tree would otherwise find one this record does not mention,
and because it is the reason the definition stays live code rather than
becoming a test-only reference.

### Addendum, 2026-09-06 — sites 5, 6 and 7

**CHARTER ONLY, like the record it extends — this addendum enacts no
site.** Author-ruled R-order-2 / R-hash-order / R-already-merged /
R-check-once in the design channel, 2026-09-06, over
`.agents/plans/2026-09-05-performance-measurement/measurements.md`'s
own `## Post-program profile, 2026-09-06` (`:923-1140`), which profiled
the tree at `63d5ee64` after all four sites above had landed. Every line
number below was read against the tree at `13bbad53` this session; the
sections before "Dated corrections" are left as they stood, and this one
is where they are read against.

The profile found what was left, and it is the same shape:

| site | inclusive CPU, 22,500 gen | allocation, 22,500 gen | where |
|---|---|---|---|
| `decide :merge` | **34.06%** (8.70% self) | **21.01%** | `decide.clj:1449` |
| `decide :bed-swap` | **31.38%** (14.57% self) | **29.98%** | `decide.clj:1414` |
| in-run `check-all` | **18.79%** of generate | -- | `sim/run.clj:783` |

and standalone `sim check` spends **49.85%** of its wall inside those
same replays (`measurements.md:1079-1097`). Two methods hold a third of
generate's CPU and half of everything it allocates; the self-check is a
fifth of generate and half of check. Neither pair was on the four-site
census, for the ordinary reason: ADR-0169 profiled a configuration in
which they were not yet the top rows, and this record's own census was
taken before the four sites removed what stood above them.

#### The numbering, stated because it moved

R-order-2 is verbatim: *"merge, then bed-swap, then in-run check-all."*
So **site 5 is `decide :merge`, site 6 is `decide :bed-swap`, and site 7
is the check-all/replay work.** The "Sequencing (R-order)" section above
numbers that last item **5**, and its sentence "Check's fourteen
`engine/replay` calls" is superseded here rather than edited in place:
it is site **7**, it is re-scoped to cover the IN-RUN self-check as well
as the standalone phase, and its own price is corrected below. A reader
who counts sites against that section and against this one will find
five and seven; seven is current.

#### 5. `decide :merge` — 34.06%

**The scan** (`decide.clj:1458-1461`): `(:patients world)`, minus the
subject, minus every patient `never-mergeable?` calls -- `(#{:new
:merged} (:status p))`, `decide.clj:1457` -- then `mapv first`. O(P) per
`:merge` step. **And a second scan beside it** (`decide.clj:1467-1471`):
`already-merged?`, a `some` over the ENTIRE `(:ground-truth world)`
looking for any `:merge` event naming `merged-id` in the `:merged` role.
That is site 4's own defect surviving under a different method's name --
a whole-log scan per decide -- and R-already-merged disposes of it
without an index at all.

**The index**: the `:eligible-index` concern below, merge view.

**Draw-affecting: YES, through the candidate vector's ORDER and COUNT.**
`streams/uniform-choice` is positional -- `(nth candidates (.nextInt rng
(count candidates)))`, `streams.clj:47-49` -- so the draw SEQUENCE is
fixed (one `.nextInt` per merge step whenever `eligible` is non-empty and
`:with` is absent) while the draw's RESOLUTION depends on both the count
and the position of every element.

**Equivalence argument.** The obligation is vector identity: at every
`:merge` decide, the index's merge view must be `=` to `(->> patients
(remove subject) (remove never-mergeable?) (mapv first))` -- same
elements, same order, same count. Elements and count follow from
R-membership-from-post-state applied to the one predicate; ORDER is
R-hash-order's subject and is the part that is not free.

**Obligation.** The subject exclusion stays a caller-side `remove` over
the index's answer, not a second index -- the same shape site 1 used for
its excluded-id argument, and for the same reason: an index keyed on the
subject would be a different index per caller.

#### 6. `decide :bed-swap` — 31.38%

**The scan** (`decide.clj:1418-1421`): the same `(:patients world)`
sweep, minus the subject, filtered to `(and (= :admitted (:status p))
(some? (:location p)))`, then `mapv first`. O(P) per `:bed-swap` step,
and the top allocator in the whole phase at 29.98%.

**The index**: the `:eligible-index` concern below, bed-swap view.

**Draw-affecting: YES, and by exactly site 5's mechanism** --
`streams/uniform-choice`, positional, one draw per bed-swap step with a
non-empty `eligible` and no `:with`.

**Equivalence argument.** Vector identity again, against `(->> patients
(remove subject) (filter admitted-with-location) (mapv first))`. The
predicate is the only thing that differs from site 5, and it is strictly
narrower: `:admitted` is not in `#{:new :merged}`, so **every bed-swap-
eligible patient is merge-eligible**. That containment is what lets one
sub-map carry both, and it is the fact the session should assert rather
than assume -- `evolve :merge`'s `:merged` arm `dissoc`es `:location`
(`evolve.clj:318-320`), so a merged patient fails the bed-swap predicate
twice over, by status and by location.

#### The `:eligible-index` concern — ONE sub-map, TWO views

Per R-fold-carrier: a single concern at `fold/apply-events`, guarded by
its own projection membership and by nothing else, maintaining ONE
sub-map of `(:patients w-next)` -- the merge-eligible patients, keyed by
patient-id -- from which both call sites read:

* **merge view** -- the sub-map's keys, in the sub-map's own seq order.
* **bed-swap view** -- those keys filtered to `:admitted` with a
  `:location`, in that same order.

Membership is recomputed for each participant from its POST-state after
every event, per R-membership-from-post-state (site 1), so there are no
eviction cases to enumerate and none to miss. One sub-map rather than
two is not an economy: it means the order argument below is made ONCE,
for one structure, rather than twice for two that could drift apart.

`run`'s `init-world` (`run.clj:1280-1281`) seeds every patient as
`state/initial-patient`, i.e. `:status :new`, so the correct seed is the
EMPTY sub-map -- the definition's own answer at t=0, the same sentence
`:board`'s seed is in (`run.clj:1284-1297`). Whether the read is raw
(R-raw-read, site 3) or throws on a missing index (site 4's own
sharpening) is the session's call and its record says which; note that
nil here is NOT a legal answer -- an absent index read as an empty
`eligible` turns every legal merge and bed-swap into a `:step-rejected`
and moves every draw after it, which is site 4's failure mode exactly.

#### R-hash-order — the argument, its hole, and the hole MEASURED

Ruled verbatim: *"sites 5 and 6 stay output-identical: `eligible`'s
ORDER is the hash-map iteration order of `:patients` and is preserved,
not replaced. The addendum states the argument AND its hole, and names
the law test at every replay entry as the detection. Replacing hash
order with sorted order is a declared oracle change, deferred to its own
row."*

**The argument.** `PersistentHashMap`'s seq is a depth-first walk of a
trie indexed by 5-bit slices of the key's `hasheq`, and every node type
iterates its slots in ascending slot order. So the relative order of two
non-colliding keys is fixed by their hash bits alone and is independent
of which other keys are present and of the order they were inserted. A
sub-map of `:patients` therefore iterates its keys in the same relative
order `:patients` does, and `(keys sub-map)` equals
`(filter sub-map-key? (keys :patients))`. Measured this session rather
than asserted: over 2,000 minted-shape ids, forward and reverse
insertion produce the identical key seq, and a 697-key sub-map built by
inserting a SHUFFLE of its members reproduces the parent's filtered
order exactly.

**The hole.** Keys whose FULL 32-bit `hasheq` collides land in a
`HashCollisionNode`, whose array is ordered by insertion. A sub-map
inserted in eligibility order can therefore differ from `:patients`
inserted in registration order at such a node -- and only there.

**The hole is REAL at this program's own scale, and the instruments are
blind to it.** Patient-ids are `PID-%06d-%08x` over `mix64`
(`streams.clj:83-84`), one per arrival, so the sub-map's key space is
the arrival count:

| seed | patient-ids | distinct 32-bit `hasheq` | colliding |
|---|---|---|---|
| 424242 | 2,500 / 7,500 / 22,500 | all | **0** |
| 424242 | 45,000 | 44,999 | **1** |
| 424242 | 67,500 | 67,499 | **1** |
| 2 | 45,000 | 44,999 | **1** |
| 1, 7, 99, 123456, 987654321 | 45,000 | all | 0 |

The colliding pair at seed 424242 is `PID-032071-30c64e95` and
`PID-038357-4bf55dc9` (arrival ordinals 32,071 and 38,357, `hasheq`
1559044961), and inserting them in the reverse of their `:patients`
order demonstrably reverses them in a sub-map. **Every committed cell of
the measured decade is collision-free**, so `bin/ground-truth-bracket`,
`bin/regression-oracle` and both bracketed cells CANNOT see this hole --
the same shape of blindness ADR-0179's R-queue claim carried, named here
before the session rather than after it. The detection is the law test:
the from-scratch definition kept verbatim and asserted `=` to the
index's answer at EVERY replay entry, per the law section above.

**A SECOND HOLE, of a different mechanism, that the ruling does not name
and the session will meet first.** `{}` and `(hash-map)` are
`PersistentArrayMap`, which iterates in INSERTION order and only becomes
a `PersistentHashMap` at nine entries. A sub-map grown from `{}` is
therefore in eligibility order for its first eight members while
`:patients` -- 2,500 entries and up from `init-world`'s own `into {}` --
has been a hash map since t=0. Measured: five minted ids give array-map
order `[0 1 2 3 4]` against hash-map order `[2 1 4 0 3]`, and a
three-key array-map parent disagrees with its own two-key array-map
sub-map. This is not exotic; it bites the FIRST merge or bed-swap of
every run, and it bites every hand-built scripted test world, which are
array-maps throughout. **The carrier must be a `PersistentHashMap` from
empty** (`clojure.lang.PersistentHashMap/EMPTY`, whose class the probe
confirms), and the session states that in the code rather than leaving
it to be inferred -- this is the site-1 `fold-events` lesson in a new
place.

**Not licensed here:** keying the index on arrival ordinal, on
`patient-id` lexically, or on anything else that would give `eligible` a
different order. That is R-hash-order's deferred oracle change and it
gets its own row, not a session's judgment call. A carrier that
preserves hash order by keying on each patient's POSITION in
`:patients`' own fixed seq order would close both holes rather than
detect them, and is recorded as an OPTION and not a licence: it rests on
`:patients` gaining no key mid-run, which is true of `run` (`prelude`
seeds every patient, which is why `fold`'s `:patient-bootstrap` branch
is inert at that site) and is an assumption, not a guarantee. Taking it
needs a ruling.

#### R-already-merged — the proof

Ruled verbatim: *"`already-merged?` is removed as provably redundant --
`:status :merged` is set by the `:merged` arm and excluded by
`never-mergeable?` on both the eligible path and the `:with` path; the
addendum carries the proof and the law test states the implication over
generated logs."*

1. **Every `:merge` event's `:merged` participant ends the event with
   `:status :merged`.** `evolve :merge` dispatches on the participant's
   own role and its `:merged` arm `(assoc :status :merged ...)`
   (`evolve.clj:316-320`), and the fold's `:patient-state` concern
   applies `evolve` to EVERY patient-bearing participant of the event
   (`fold.clj:980-984`), not only the subject. This holds for both
   emitters of a `:merge` event: `decide :merge` (`decide.clj:1475-1480`)
   and `decide :identification-merge` (`decide.clj:836-843`), which is
   the same kind with the same two roles.
2. **`:merged` is absorbing.** No `evolve` method moves a status off
   `:merged`; the run loop abandons a merged patient's remaining queue
   before their next step is decided (`run.clj:1467`); `:merged` is a
   terminal status for both reinstating cancels
   (`log_index.clj:293-294`); and `encounters.clj:51` reads it as
   absorbing. Nor can a merged patient re-enter the log as another
   patient's participant: the participant roles are exactly
   `#{:subject :survivor :merged}` (`event_schema.clj:358`), `decide
   :bed-swap`'s peer must be `:admitted` with a `:location`, and `decide
   :identification-merge`'s survivor is guarded
   `(#{:merged :expired} (:status survivor))`.
3. **Both paths to `merged-id` already exclude `:merged`.** The dynamic
   path removes it from `eligible` (`decide.clj:1460`); the `:with` path
   is caught by `(never-mergeable? merged)` in the rejection test
   (`decide.clj:1472-1473`), which is evaluated on the same `if`.

Therefore `already-merged?` is true only where
`(never-mergeable? merged)` is already true, and the disjunct never
decides an outcome. It is a whole-log scan whose answer is a strictly
weaker restatement of a map lookup that sits three lines above it.

**What the proof rests on, said plainly:** `already-merged?` reads the
LOG and `never-mergeable?` reads the WORLD, and they agree because the
world is the fold of the log. A hand-built world carrying a `:merge`
event in `:ground-truth` whose participants' states were never folded
would answer differently -- which is a world no fold produces, and
exactly the drift site 1's `fold-events` rewrite removed from the test
tree. `engine_test`'s `double-merge-of-the-same-patient-id-is-rejected`
(`engine_test.clj:1029-1037`) already folds through
`fold/apply-events`, so it will keep passing on the surviving guard: the
law test therefore has to state the IMPLICATION -- over generated
churn-bearing logs, no `:merge` decide's outcome changes when
`already-merged?` is dropped -- rather than lean on a scripted case that
cannot distinguish the two guards.

#### 7. the `check-all` replays — 18.79% of generate AND 49.85% of check

Ruled verbatim (R-check-once): *"site 7 replays zero times in-run: the
run hands `check-all` its own projection through the `(:records folded)`
carrier; standalone `sim check` replays ONCE and every invariant reads
the shared projection."*

**The carrier already exists on both sides.** `replay` is a PROJECTION
of `apply-events`, not a fold of its own (`fold.clj:1063-1093`), and
`check.clj:929` already prefers pre-folded records to a replay --
`(or (:records folded) (engine/replay ground-truth))`, fed by
`bed-fold`'s own `:records` key (`check.clj:703-768`, the key returned
at `:734`) through `log-derived-bed-fold` (`:775-782`). Note the `or`'s
real shape: `log-derived-bed-fold` is `bed-fold` OR NIL -- nil on a log
carrying no bed cycle -- so the carrier serves `:929` only on cycle-
bearing logs and `:929` replays on the rest. That idiom generalizes to
the catalog: `check-all`
(`check.clj:2113-2140`) computes the entries once and threads them, and
in-run `run` hands over the entries **it already builds and never
realises** -- `:replay-entries` is in `full-algebra` (`fold.clj:522-524`),
its accumulator is seeded at `run.clj:1577`, the concern conjes one
entry per event at `fold.clj:985-992`, and `fold.clj:572-579` records
that nothing reads what it accumulates. Site 7 is the session that makes
that sentence false, which is a doc obligation it owes in the same
commit.

**Not fourteen invocations -- SEVENTEEN, or TWENTY, and both measured.**
The fourteen `engine/replay` call sites (`check.clj:215 :234 :284 :298
:399 :532 :731 :929 :1060 :1074 :1174 :1308 :1758 :1785`) are TEXTUAL.
`:399` is inside the private helper `fold-records`, invoked from five
invariants (`:408 :450 :497 :585 :865`); `:731` is inside `bed-fold`,
reached from `bed-transitions` (`:770-773`, itself read by three rows)
and from `log-derived-bed-fold`. Counted live this session by rebinding
`engine/replay` around one `check-all`, `sim-engine`'s own `run` at 60
patients with the active churn profile:

| log | events | `engine/replay` invocations | events replayed |
|---|---|---|---|
| churn, no `:bed-cycle` | 278 | **17** | 4,726 |
| churn with `:bed-cycle` | 171 | **20** | 3,420 |

The R-order section above prices this site at "~65 s at the top cell"
off fourteen; the multiple is 17 on the shipped default and 20 with the
bed cycle on, and the in-run copy pays it a second time.

**What those invocations actually read, because that is what the handoff
has to reproduce:**

| what the entry key gives | sites |
|---|---|
| `:event` + `:before` + `:patient-id` (the subject only) | `:215 :234 :284 :298 :1174 :1308 :1758 :1785` |
| `:event` + `:patient-id` + `:after` | `:532` |
| the whole record seq, folded | `:399` (x5), `:731` (x1, or x4 with the bed cycle) |
| `:world-before`, indexed by a participant id | `:1060` |
| `:world-before` + `:world-after`, indexed by participant ids | `:1074` |
| `:world-before`, ITERATED WHOLE | `:929`, via `sim-model/occupancy-board` |

**THE ONE REAL DIVERGENCE, and site 7's central obligation.** Run-built
entries are NOT replay-built entries, for two mechanical reasons a
session must settle before it hands one over as the other:

* **Population.** `run`'s `init-world` seeds `:patients` with EVERY
  patient at t=0 (`run.clj:1280-1281`), so its `:world-before` carries
  every one of them as a `state/initial-patient` record from the first
  event. `replay` starts `{:patients {}}` (`fold.clj:1082-1093`) and
  bootstraps a participant on first sight (`fold.clj:949-956`), so its
  `:world-before` carries only patients seen so far. The eight
  subject-only sites and the two participant-indexed sites
  (`:1060 :1074`) are indifferent -- they `get` a participant, who is
  present and identical in both. `:929` is NOT: it iterates the whole
  map through `occupancy-board`, whose VALUE still agrees (an
  unregistered `state/initial-patient` names no bed) but whose COST goes
  from O(seen-so-far) to O(all patients) per surge event -- the very
  quadratic site 3 removed, reintroduced in check. Naming the remedies
  without choosing one: carry `:board` in the entry, or leave `:929` on
  its own replay, or filter the handed world. The session picks and
  says why.
* **The bootstrap MRN.** `init-world` seeds with `(mrn-for i)`;
  `replay`'s bootstrap seeds with `(:active-mrn ev)`
  (`fold.clj:953-954`). For a patient's first event these are the same
  MRN in every log `run` produces, and that is an assertion the session
  owes rather than an assumption it may make.

**The instrument is NOT the bracket.** Site 7 moves no ground-truth
byte by construction -- it changes only how the checker gets its
records -- so `bin/ground-truth-bracket` IDENTICAL is necessary and says
almost nothing here. The obligation is that `check-all` returns the SAME
verdict, violation vector for violation vector, over the oracle's roots
and over generated churn-bearing corpora, computed both ways. If
run-built and replay-built entries cannot be shown equal for what those
sites read, the site STOPS for a ruling rather than shipping a
quietly weaker self-check.

**`:warm-up-mark`'s omission at the replay site STAYS** (ruling A2(b),
2026-09-01, `fold.clj:700-719`): a log does not carry a warm-up window
and declaring 0 instead is measurably lossy. Site 7 does not touch it,
and the handoff does not reach it either -- `run`'s entries carry events
already decorated by that concern, and `replay`'s carry the log's own
marks, which are the same marks.

**And the price is in WORK, not in the peak.** The profile's ranking
(`measurements.md:1101-1113`) reasons from the 3,863 MB peak heap to
67,500 arrivals being unreachable. That inference is left standing here
as a HYPOTHESIS site 7's own measurement tests, not as a prediction this
record makes, and the profile's own finding is why: peak RSS ROSE from
3,599 MB to 4,780 MB when the heap budget rose from 3.88 GB to 8 GB on a
BYTE-IDENTICAL run (`measurements.md:1003-1012`), so the peak measures
what G1 was allowed rather than what the run needs. Replaying once
instead of seventeen times removes sixteen seventeenths of that
allocation; whether it lowers the peak is a question for `-Xlog:gc*`'s
post-collection floor, which is the instrument that measured the live
set at 432 MB (`measurements.md:1014-1030`), and not for peak RSS.

#### Sequencing (R-order-2)

One session each, in this order, each commit bracket-proven:

5. `decide :merge` -- 34.06% CPU, 21.01% allocation. Two changes, and
   `already-merged?`'s removal is the smaller and the more surprising.
6. `decide :bed-swap` -- 31.38% CPU, 29.98% allocation. The
   `:eligible-index` concern is site 5's; this is the second view over
   it.
7. the in-run and standalone `check-all` -- 18.79% of generate, 49.85%
   of check, seventeen replays per invocation, twenty with the bed cycle on.

Sites 5 and 6 are draw-affecting and their equivalence arguments are
above, written before their sessions per R-equivalence. Site 7 moves no
ground-truth byte and owes a different instrument, named above.

#### What this addendum does not enact, and does not license

* **No engine code changes.** Charter only, exactly as the record above.
* **The two gate gaps stay where they are** (R-gate-gaps,
  `roadmap.md#dense-7500-gate-gaps`): unmoved by this addendum, and
  still owed BEFORE a site that could move the Scale table.
* **The constant-factor exclusion still stands.**
  `sim-model/licensed-bed-ids`' inner fn at 5.25% of allocation and
  check's 20.12% EDN parse are both real and neither is chartered here
  (`measurements.md:1134-1139`).
* **`decide`'s remaining share is not a fifth site.** `decide` as a
  whole is 72.28% of generate after the four; sites 5 and 6 are named
  because they are the SHAPE, not because the frame above them is large.
* **The `naive-*` duplication grows again.** Two more definitions that
  live twice, declared a permanent cost here as ADR-0169 declared it and
  as the four sites above re-declared it.

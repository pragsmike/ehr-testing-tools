Session prompt -- arc 3b-3: scheduling as state (ADR-0174 §2(b), sweep 3 of 3)

Context. HEAD ab156c3. Sweeps 1-2 landed the encounter and the bed cycle;
sweep 2's three unratified items were RATIFIED 2026-08-27 (the sixth
`occupied->ready` correction arc; `:turnaround-minutes [lo hi]` per leg;
invariant 5's re-read) -- Step 0 records them in ADR-0174 §2(c). This
sweep is R-mix-5: appointments as skeleton STATE, arrivals split
scheduled-vs-walk-in, follow-ups at discharge producing scheduled second
encounters. C1 holds: NO SIU on the wire (MSH-12 `"2.3"` vs the v2.4
structures -- unresolved, stays rowed); the four kinds are skeleton-only.
Dark-then-on behind `:scheduling` (ADR §2(b) :395-400 names the key and
its six sub-keys; absent = today byte-for-byte). Read ADR-0174 §2(b) whole
(:369-460), sweeps 1-2's records (three traps: `classify-change` nested
whole-value compare; first-participant rule for top-level fields; the
stale-asset tripwire reads `git log -1` -- if you edit a scenario README
or anything a `docs/manual/assets` registry row cites, redraw or bump
BEFORE the push, never after CI). Re-derive every line.

Step 0. ADR-0174 §2(c) gains the three ratifications, dated. Own commit.

Capacity gate, BEFORE any opt-in (new, from sweep 2's finding 4): `:exhausted`
HALTS a run (`engine.clj:733`, `run.clj:573` `:error :capacity-exhausted`)
rather than emitting the visible `:step-rejected` §2(c) item 4 claims.
Scheduling ADDS arrivals. So: for each corpus you will opt in, measure the
ladder margin at the current tip (peak occupancy per ward vs `:beds` +
`:surge-slots`, rung reached) and state the headroom in the config comment.
A corpus that exhausts on turn-on is NOT tuned into passing by lowering the
scheduled fraction below the ADR's default without saying so; if the margin
is under 10% anywhere, STOP-AND-REPORT before turning on. Row the
halt-vs-reject question under `[performance-residual-sites]`' sibling or a
one-line row of its own; do not change `exhausted-outcome` here.

Step 1. RED then GREEN, dark. Unit configs WITH `:scheduling`, `pos?`
witnesses: (i) the four kinds with the fields at :376-380, one open-
appointment record per patient in `PatientState`, `:appointment-id` =
`(mix64 ...)` on the same law as `:encounter-id` (B1); (ii) the split --
scheduled-vs-walk-in Bernoulli and lead time on `:world` (:402-410); an
appointment's outcome (kept / rescheduled / cancelled / no-show) on the
patient's own `:patient` stream (:411-418); state the draw count per
appointment and that it is fixed; (iii) a scheduled arrival's opener
carries `:appointment-id` and the open encounter references it (:387-
392); a `:no-show` is emitted AT `:scheduled-t` and opens nothing; (iv)
follow-up: at `decide :discharge`, Bernoulli + interval on `:patient`,
minting an `:appointment` at the discharge instant whose arrival, if
kept, is a SECOND encounter (:427-436) -- the producer R-mix-5 needs;
(v) the three invariants (:439-447): `appointment-reference-resolves`,
`scheduled-encounter-follows-its-appointment` (non-vacuous only because
sweep 1 landed -- assert the count), `no-show-has-no-encounter`; plus
`registered-is-every-patients-first-event` still holds; (vi)
`classify-change` reported, bump if owed (four kinds + `:appointment-
id` on openers: expect additive at top level, check the nested maps).
PROOF: `bin/regression-oracle ab156c3 HEAD` IDENTICAL, 38 roots, no
declaration; fixtures, both v2 baselines, `demos/traces` byte-equal.

Step 2. ON. Opt in the six corpora (+ `config-latency.edn`) at the ADR's
default sub-keys, each config carrying its measured margin; ONE new root
`scheduling` (38 untouched). Predicted movers: every opted-in corpus
(arrival ordinals re-split; follow-ups add encounters). Re-pin ONCE per
the list; `--declared-digest-change` bracket: 38 IDENTICAL, one `+`.
Witness table across the opted-in set, `pos?`: appointments, reschedules,
cancels, no-shows, scheduled arrivals, follow-up encounters (the
headline: second encounters that are SCHEDULED, per corpus), and per
corpus the ladder rung reached before/after. `make test` +
`make integration` (39th root's integration pins). Commit ON.

Step 3. Push; CI; no tag. Record one page: both oracle lines, the
witness table, the margin table, draw counts, filters/traps hit, ADR
premises the tree contradicted. Roadmap: `[engine-fold-extensions]`
CLOSED (arc 3 complete); MSH-12/SIU stays rowed for arc 4.

Fences. Step 1 moves nothing. Step 2 moves only opted-in corpora and the
new pair. No SIU, no new message type. No change to the bed cycle or
churn. No exhaustion "fixed" by tuning below the ADR default without
disclosure. One sweep.

# Session prompt -- arc 3a execution: the engine folds the person stream (ADR-0173)

> ARCHIVED VERBATIM as issued. The session it drove executed steps 0
> and 1 only; see
> [`../session-records/2026-08-25-arc-3a-residence-loss.md`](../session-records/2026-08-25-arc-3a-residence-loss.md)
> for what landed, what did not, and why the cut was made where it was.

Context. HEAD e9bc65b. ADR-0173 designed the demographic fold; the author
ruled 2026-08-25: **A1 B1 C1 D1 E1**, and `:residence-loss` lands first.
Read ADR-0173 whole: §2(a)-(f) is the spec (config shape :163-178; the
opt-in law -- `:persons` ABSENT is the byte-identical path; `:person-index`;
the six state-at-event sites; hooks; identification per the author
statement; the check.clj family named in (e); provenance (f)). The fold
enters `run` as config data via `ehrt.sim.run`, never as a `sim-engine`
require (cycle; ADR-0172 row 10). D1 fixes this session's shape: land DARK
and prove IDENTICAL, then turn on with ONE declared sweep. Re-derive every
line number. Payload session.

Step 0. ADR-0173 -> Accepted, five rulings quoted where they land. Commit.

Step 1. `:residence-loss` -- person-simulator's fifteenth kind (§2(d):
unhoused is a STATE; an unhoused `:residence-move` correctly goes red on
row 7). RED: a kind-vocabulary test counts 15, the witness config produces
`pos?` of them, row 7's guard still holds; GREEN: the kind, `:unhoused
{:t0-fraction 0.02}` honoured at t0, `:residence-move` from unhoused =
housing gained. `bin/regression-oracle e9bc65b HEAD` IDENTICAL (nothing
calls the component). Commit, message names the oracle line.

Step 2. DARK (D1 commit 1). All of §2's engine/run/emit/check/provenance
code lands with `:persons` absent from EVERY existing config: `ehrt.sim.run`
calls `person-simulator/persons` only when `:persons` is present and hands
`run` the event vector + `:deaths` (C1's compiled instants -- the ordering
ADR-0173 §2(a) proves computable); `init-world` carries `:person-index`;
`decide :registered` reads Persona from the fold when a person is bound,
else exactly as today (the 13 draws at :493 stay on the `:patient` stream
on this path -- byte-identity depends on it); the six state-at-event
sites read state; hooks; identification (B1: fresh placeholder MRN,
`:identity-resolution :merge` rides churn's `:merge` shape; E1: PID-11
absent while unhoused); (e)'s invariants registered in `check-all`; (f)'s
stamp present only when `:persons` is. RED first: unit tests per §2
subsection, each on a config WITH `:persons`, failing on e9bc65b's engine
for the reason "no fold". GREEN. PROOF: `bin/regression-oracle e9bc65b
HEAD` IDENTICAL, no declaration; `arc0_gated_*`, `pinned_seed_42`, both
conformance baselines byte-equal (`git diff --stat` empty); `make test`
counts reconciled (delta = the new tests only); `make integration`.
Commit: `feat(sim): the demographic fold, landed dark -- oracle IDENTICAL`.

Step 3. ON (D1 commit 2). Opt in, per the opt-in law, exactly: the four
`gated-runs`, `demos/scenarios/ed-tuesday` and `clinic-decade`, and ONE
NEW oracle root pair in `digest.clj` with `:persons` (do NOT mutate the 35
existing roots -- they never opted in and must stay IDENTICAL; the new
pair is the oracle's witness of the fold). Predicted movers (§1): every
opted-in corpus moves because `:registered` no longer draws Persona from
`:patient`. Re-pin ONCE: the opted-in fixtures and their digests, the
counted witnesses (`pos?` floors stay; a witness that goes empty is
re-derived like arc 1b's seed 130, disclosed), `make traces` and
`event-schema-examples` for opted-in scenarios only, both scenario
READMEs re-derived from their regenerated runs. `bin/regression-oracle
<commit-1> HEAD --declared-digest-change`: exactly the new pair DIFFERS
(first run: it has no baseline -- say so), all 35 IDENTICAL. Every
`R-witness-population-is-counted` count in check_test / run_test asserted
`pos?` over the new corpora: placeholder registrations, fills, merges,
unhoused arrivals, deliveries, occupational injuries -- each kind that §2
says reaches the wire is counted at least once across the opted-in set.
`make test` + `make integration` unpiped. Commit: `feat(sim): the
demographic fold, turned on -- six corpora re-pinned once, declared`.

Step 4. Push; CI is the gate; no tag. Record one page: the two oracle
lines, per-kind witness counts on the wire, the draw sites that moved
(named, from §1), what re-pinned, ADR premises the tree contradicted.
Roadmap: `[engine-fold-extensions]` arc-3a half closed, 3b row stays.

Fences. Commit 2 moves NOTHING (a moved byte is a STOP). Commit 3 moves
only opted-in corpora; an existing oracle root differing is a STOP.
`sim-engine` never requires `person-simulator`. No new hazard, no
person-side draw outside `:person`, no engine->person edge. No re-pin
outside step 3's list without naming it. One reshuffle.

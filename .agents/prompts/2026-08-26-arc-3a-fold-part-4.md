# Session prompt -- arc 3a-4: hooks, identification, and the turn-on (ADR-0173, part 4 of 4)

**Date:** 2026-08-26
**Record:** [`.agents/session-records/2026-08-26-arc-3a-fold-part-4.md`](../session-records/2026-08-26-arc-3a-fold-part-4.md)

Archived verbatim, as issued.

---

Context. HEAD dd4cf8d. Parts 1-3 landed the fold dark (oracle IDENTICAL
three times). This session lands §2(c) hooks and §2(d) identification --
still dark, proven IDENTICAL -- and then D1 commit 2: six corpora opt in,
one declared sweep, the program's first corpus that carries the payload.
Read first: ADR-0173 §2(c)-(d), its Consequences (six tabled deviations --
esp. "a repeat arrival queues no steps"), part 3's record; `churn.clj`
`:merge` (:67-132: the shape and its `(:admitted? state)` guard);
`engine.clj` `patient-id-for`/`mrn-for` (functions of an ordinal -- the
newborn is ordinal `(+ patients k)`); `digest.clj` :56 (how the 32
engine-layer pairs are declared); `run_test.clj` `gated-runs` :394-410;
`demos/scenarios/{ed-tuesday,clinic-decade}/config.edn`. Author statement
(binding): unhoused persons arrive at ED; unhoused unresponsive John Does
exist. Re-derive every line.

Step 1 (dark). RED then GREEN on `:persons` unit configs; every test
carries a `pos?` witness. Hooks: `:delivery` -> parent admission at the
delivery `:t` + the newborn's first encounter as an ADDITIONAL patient
(ordinal `(+ patients k)` in delivery-`:t` order, so ids/MRNs stay
functions of the ordinal) with the mother-baby link;
`:occupational-injury` -> an ED arrival with that cause. Identification
(G1, B1, E1): `:identity-unavailable` at arrival -> placeholder
registration minted by the ENGINE (`:alias-name`, no address, a FRESH
MRN); `:identity-resolution :fill` -> `:identity-fill` referencing
`:placeholder-event-id`, same MRN; `:identity-resolution :merge` ->
`:identification-merge` emitted in churn's `:merge` SHAPE (survivor =
the person's prior patient) so check.clj's merge invariants and the
post-merge shadow apply unchanged -- add nothing to churn's lottery;
unhoused residence renders PID-11 absent. §2(e)'s four identification
invariants (placeholder resolved-or-open; fill references its
placeholder; merge survivor is the prior patient; nothing after expiry)
fire on mutated corpora and are clean on the unit run. Repeat arrivals
COUNTED as a witness (`repeat-arrivals-resolve-and-queue-nothing`) so
the deviation is visible, not silent. Proof: `bin/regression-oracle
dd4cf8d HEAD` IDENTICAL, 35 roots, no declaration; pinned artifacts
byte-equal. Commit: `feat(sim): hooks and identification, dark`.

Step 2 (ON, D1 commit 2). Opt in exactly: the four `gated-runs`
(`:persons {:count <n>}`, n proportionate to `:patients`, say why);
`ed-tuesday` and `clinic-decade` `config.edn`; and ONE NEW oracle pair in
`digest.clj` with `:persons` (do NOT touch the 32/35 existing roots).
Predicted movers: every opted-in corpus (Persona now from the person;
`:patient` loses 13 draws at `:registered`). Re-pin ONCE: the four
`arc0_gated_*` fixtures + digests (F3 tripwire stays), counted witnesses
(`pos?` floors stay; an emptied witness is re-derived like arc 1b's seed
130 and disclosed), `make traces` + `event-schema-examples` for opted-in
scenarios, both scenario READMEs from their regenerated runs. Wire
witnesses across the opted-in set, each `pos?`: placeholder
registrations, fills, merges, unhoused arrivals (PID-11 absent),
deliveries with a newborn `:registered`, occupational-injury arrivals,
`:demographic-update`, `:coverage-change`, repeat arrivals. `bin/
regression-oracle <step-1 sha> HEAD --declared-digest-change`: the 35
existing roots IDENTICAL, the new pair has no baseline (say so) --
an existing root differing is a STOP. `make test` + `make integration`.
Commit: `feat(sim): the demographic fold, turned on -- six corpora
re-pinned once, declared`.

Step 3. Push; CI is the gate; no tag. Record one page: both oracle
lines, the wire-witness table, what re-pinned, the `:patient`-family
sites that moved. Roadmap: arc-3a half CLOSED; add ONE `## Next` row
`[multi-encounter-horizon]` -- "a repeat arrival queues no steps
(`admission-only-when-new`); returning patients produce no second
encounter; owner unassigned, candidates arc 3b (R-mix-5: a scheduled
return is a second encounter) or its own arc" -- for the author to
place; do not start it.

Fences. Step 1 moves NOTHING. Step 2 moves only opted-in corpora and
the new pair. No second churn lottery entry. No sentinel address (E1).
No hazard or draw change in `person-simulator`. No new limitation
lifted. No re-pin outside step 2's list without naming it. One sweep.

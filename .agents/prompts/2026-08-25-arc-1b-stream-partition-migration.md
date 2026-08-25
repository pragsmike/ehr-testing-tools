# 2026-08-25 — arc 1b: the RNG stream partition, migrated (ADR-0171 executed)

Archived verbatim, as the session's last pre-push act. Record:
[`../session-records/2026-08-25-arc-1b-stream-partition-migration.md`](../session-records/2026-08-25-arc-1b-stream-partition-migration.md).

---

Session prompt -- arc 1b: the RNG stream partition, migrated (ADR-0171 executed)

Context. HEAD 97f22fd. ADR-0171 (`notes/adr/0171-arc-1-rng-stream-partition-
design.md`) designed the partition from a census of every draw site; the author
ruled on 2026-08-25: **A1 B1 C1 D1 E1 F1, and LOCALITY option (a)** -- the
weakened property over PATIENT-scoped fields, four WORLD sites excluded by
name (ADR §1 rows `:480`, `:610`, `:643`, `:672` at design HEAD). This session
executes the ADR as one draw-affecting migration, F1: partition + from==to
delay-skip + every re-pin + oracle re-baseline + the churn.clj docstring
replacement (§2f), one commit group, one reshuffle. Everything in the corpus
moves once; nothing may move twice. Read ADR-0171 whole first; §1 is your
site list, §2 your spec, §3 your test list, §3's "definite movers" your
re-pin list. Do not re-derive the design; do re-derive every line number.

Step 0. Flip ADR-0171 to Accepted with the seven rulings quoted. Own commit.

Step 1. RED. Land §3's tests before the partition:
`mutating-one-patients-stream-seed-moves-only-that-patient` (LOCALITY, §3
:396, asserting `engine/events-for-patient` byte-identity for every OTHER
patient and a counted witness: how many patients the perturbation moved,
pinned, `pos?`); the DETERMINISM CONTINUITY test (§3 :419); WITNESS
COUNTS (§3 :440) -- each must be RED on 97f22fd for the reason the ADR
gives (there is no per-patient stream to perturb yet: the test must fail
by moving everyone, not by failing to compile). Commit RED, message says
which assertion fails and why.

Step 2. The partition, per §2: `mix64` (`engine.clj:225`) promoted to the
sim-engine interface (A1); families PATIENT / PERSON / WORLD / FACILITY /
EMISSION derived `(mix64 master family-tag) -> (mix64 that id-tag)`; newborn
key `(parity-index, within-delivery-index=0)` (B1); `run.clj:422`'s
emission Random seeded from the EMISSION family (C1); FACILITY draws
(`materialize-providers`, `choose-attending`, `:outpatient-visit` provider
pick) off WORLD (E1), `allocate` stays WORLD; `decide :delay` draws ZERO
times when from==to (§2d); `churn.clj` docstring paragraph replaced (§2f).
Every census row in §1 gets its family; a row you cannot classify from the
code as the ADR classified it is a STOP-AND-REPORT, not a guess.

Step 3. Provenance: top-level `:stream-scheme` string on the sim manifest
(D1), sibling of `:event-schema-version`, value from one `def` the engine
exports; manifest schema test extended; ADR-0009's within-version policy
cited in the docstring.

Step 4. Re-pin, once each, exactly the §3 "definite movers": the four
`arc0_gated_*.edn` fixtures and their digests (`run_test.clj:500-503`);
`pinned_seed_42_patients_5.edn`; the counted witnesses (`:680`, `:852` --
new counts must still be `pos?`, and the F3 tripwire stays); the two
conformance baselines; `make traces` (14 captures); `make
event-schema-examples`. `R-defspec-seed-policy`: NO defspec re-pin -- the
three seeded defspecs and the arc-0 naive-vs-fast equivalence defspecs
stay green untouched; a red there is a finding, STOP. Then
`bin/regression-oracle 97f22fd HEAD --declared-digest-change` -- all 35
roots MUST differ; any IDENTICAL root is a site the partition missed.

Step 5. GREEN: Step 1's tests pass; `make test` and `make integration`
(Makefile:52 -- `make test` skips that tier) unpiped, exit captured, on a
sampled-quiet machine; suite counts reconciled vs 97f22fd (+3 tests
expected from step 1; explain any other delta). Push; CI is the gate; no
tag. Record: one page -- families and their tags, sites per family
(count), what re-pinned, oracle summary line, the locality witness count,
any ADR premise the tree contradicted (one line each).

Fences. Draw-affecting by design -- but ONLY through the partition and the
from==to skip; no other behaviour change rides (no allocation, churn, or
persona change). No re-pin outside §3's list without saying which and why.
No vendored-module change. No history rewrite. One reshuffle: if you find
a second draw-affecting change is needed mid-session, STOP-AND-REPORT
before landing anything that would move the corpus again.

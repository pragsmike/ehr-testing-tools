# 2026-09-01 — event-stream mutation (P6) implementation 1: the spine

Archived verbatim, per `AGENTS.md`'s session-record ritual (R-A). The
record this prompt drove is
[`2026-09-01-event-stream-mutation-spine.md`](../session-records/2026-09-01-event-stream-mutation-spine.md).

---

SESSION: event-stream mutation (P6) implementation 1 — the spine
Repo: pragsmike/ehr-testing-tools, tip (7096394 or descendant).
Roadmap row P6. RULINGS (2026-09-01, all in force): ADR-0176
Q1-Q9 ALL RULED (a) — post-run whole-log mutation stage outside
engine/run; catalog joins corpus.operators as :format :event; one
site per application, one draw; operator's own seed, family tag 6
reserved unused; expected-findings as a SET with the gate asserting
EQUALITY; unconvictable operators refused registration and recorded
as catalog gaps; CLI ehrt sim mutate as a stdin→stdout filter; v1 =
derived referential family + three structural; schema-valid mutants
only. RED-BEFORE-GREEN IS BACK IN FORCE — this is behavior, not
refactor; S1(a) does not apply.

THIS SESSION IS THE SPINE, not the catalog: ONE referential operator
end-to-end — registration, application, lineage, CLI, and the closed
loop — proving the whole contract. Breadth is session 2.

READ FIRST: ADR-0176 whole; the design-session record's four
one-line findings (esp. :person-event-id is a stamp — NO referential
operator; and consuming-ground-truth.md:561-588 goes stale on THIS
commit — fixing it is in scope, it's this commit's own wake).

STEPS (one gate each; full make test per push)
1. Derive from the tree: corpus.operators' registration shape and
   :format :event slot; check's finding classes for the chosen
   operator's defect class; the log's serialized form the filter
   reads/writes. Gate: recorded.
2. RED: the closed-loop acceptance test — apply operator O at seed s
   to a real generated log; check must report EXACTLY O's declared
   finding set (Q5 equality) and the unmutated log must check clean;
   plus the lineage assertion (parent identity, operator id, seed,
   site recorded); plus byte-identity of everything the mutation
   didn't touch. Commit the failing tests first. Gate: red for the
   right reason, shown.
3. GREEN: the mutate stage (pure (log, operator, seed) → log'), ONE
   referential operator with its derived finding set, Q6 refusal
   path (register an unconvictable dummy in a test, assert
   refusal + gap record), lineage envelope. Gate: suite green;
   acceptance loop passes; the refusal test passes.
4. CLI: ehrt sim mutate, stdin→stdout, args operator + seed;
   self-explaining per help.clj conventions; default invocation
   with no operator is a byte-identical pass-through (the opt-in
   law's CLI face). Gate: an exerciser run in the record — generate,
   mutate, check, with the finding-set equality shown.
5. Docs, same push: consuming-ground-truth.md:561-588 corrected to
   the new truth; the P6 row's stale "unified apply path as
   injection point" sentence corrected to the Q1(a) shape (one
   line — the channel's inference, superseded by ADR-0176 Q1).
   Gate: state-derived LAST; suite green.
6. Push; CI via gh; close marker. Record: the contract proven
   end-to-end; session-2 scope (remaining 19 referential + 3
   structural operators) priced; any ADR-0176 reading the tree
   refuted, one sentence each.
FENCES: no engine/run edits; no emitter edits; no fold/apply-events
edits; mutation absent = byte-identical everywhere; oracle
IDENTICAL expected on all existing paths (the stage is post-run and
opt-in) — a delta is a defect, stop and report.
SELF-ARCHIVE: prompt and record in the final push.

SESSION: sim check learns its facility — the config-starved checker,
plus the P6 close and the family-6 reservation
Repo: pragsmike/ehr-testing-tools, tip (188e26a or descendant).
RULINGS (2026-09-01): Q14(a) — `ehrt sim check` gains a facility
config input so the taught pipe stops convicting the shipped demo's
own clean log; the author licenses THIS widening of the check CLI
surface (Q11(a)'s no-widening ruling covered sim-check.interface's
finding vocabulary and stands untouched — derive whether the CLI
verb can thread config through the EXISTING interface; if honoring
Q14 requires widening ehrt.sim-check.interface itself, STOP and
report the minimal widening for a ruling rather than take it).
Q13(a) — the family tag 6 (:mutation) reservation comment lands in
streams.clj's scheme, a one-line rider, Q4(a) finally tree-recorded.
Q15(a) — the P6 row migrates to Done; follow-on rows stay live.
RED-BEFORE-GREEN for the check change — this is behavior.

READ FIRST: the breadth session record's config-starvation finding
(default 6 ED surge slots vs ed-tuesday's 16; the corpus is sound,
the checker is starved); sim run's --config handling as the shape
to mirror; help.clj's sim check entry.

STEPS (one gate each; full make test per push)
1. Derive: how run's facility reaches its checkers vs how check's
   CLI constructs its checker inputs; the minimal thread. Gate:
   recorded, with the interface-widening question answered
   explicitly (thread-through-existing vs STOP).
2. RED: a CLI-level test — ed-tuesday's canonical clean log through
   sim check WITH its config exits 0 and reports no
   :occupancy-within-capacity finding; and (pinning today's honest
   behavior) without the flag the default-facility conviction still
   occurs, asserted as the documented default rather than silently
   changed. Commit red, shown for the right reason.
3. GREEN: `ehrt sim check --config PATH` (flag name mirroring sim
   run's); no flag = today's behavior byte-for-byte. Gate: suite
   green; the RED test passes; the exerciser pipe run in the
   record: run --config X | mutate | check --config X convicting
   exactly the injected class, and the clean pipe exiting 0.
4. Q13(a) rider: the one-line :mutation family-6 reservation in
   streams.clj (comment/scheme entry per its convention; NO
   behavior change — oracle IDENTICAL expected, a delta is a
   defect, stop and report).
5. Docs: help text; README's front-door pipe gains --config where
   it teaches check (derive every teaching site — the breadth
   record says the demo teaches the pipe as its oracle);
   consuming-ground-truth.md if it states check's config behavior;
   Q15(a): the P6 row's narrative to Done per rotation (catalog at
   twelve, the loop, the three ADR corrections, the gap ledger),
   live follow-ons remain. Gate: roadmap-lint + ADR-index green;
   state-derived LAST.
6. Push; CI via gh; close marker. Record: the fixed pipe shown
   end-to-end; both oracles over the span (IDENTICAL expected —
   check reads, never writes).
FENCES: no engine/run, emitter, or fold edits beyond step 4's
comment line; no sim-check.interface widening without the step-1
STOP; no finding-vocabulary changes; default invocations
byte-identical.
SELF-ARCHIVE: prompt and record in the final push.

# Errata: class-at keyed by log position, proven equal to replay (the location-at rule, applied back)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-class-at-log-position.md`, which
> records that step 1 was RED at baseline on every root while no wire
> byte could move -- every divergence was an opener reading its own
> class, and openers do not render through `class-at` -- so step 3's
> "red anywhere -> DIFFERS" branch did not describe the oracle outcome.

Context: 2026-09-30's second session found that "most recent placement at or before t" and "equal to
replay's before-state" disagree on 297 of 1,019 bedded observations at the 750 cell (same-second
sequencing; 235 are observations logged before the discharge that ends the stay), and keyed
`location-at` by log position. `class-at` (timelines.clj:163-175), landed the day before to the same
spec, is still keyed by t and has no replay-equality gate. Same fold family, same rule. Baseline 233e1bcd.

Read first:
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/timelines.clj:120-260 (both folds; location-at is the model)
- components/sim/test/ehrt/sim/run_test.clj (the 2026-09-30 replay-equality test for location; the class tests)
- components/sim-engine/src/ehrt/sim_engine/evolve.clj (which arms set :class: admission, outpatient-visit, cancel-admit)
- notes/adr/0174-*.md (the two 2026-09-30 dated notes)

Author ruling 2026-09-30: errata. Expectation: `class-timeline` becomes `{pid [[log-index class] ...]}`
and `class-at` reads the class held BEFORE the event's own index, exactly as `location-at` does; the
`:inpatient` fallback stays.

Steps:
1. Red test, sim brick: for every gated root and the 750 cell, `class-at` equals `engine/replay`'s
   before-state `:class` at every event (nil before-state -> the fallback). Red or green at baseline is
   itself the finding -- record which roots, if any, diverge and by how many events.
2. Re-key the fold and the lookup; the two builders' calls take the event's index (already stamped).
   Invariant: step 1 green on every root; `git diff -- components/sim-engine` empty.  Gate: brick:sim-emit-hl7, brick:sim.
3. `bin/ground-truth-bracket 233e1bcd HEAD` (no flag) IDENTICAL; `bin/regression-oracle 233e1bcd HEAD`
   -- if step 1 was green at baseline, IDENTICAL everywhere and no flag; if red anywhere, DIFFERS on
   exactly those roots, declared, with the per-root event count.  Gate: banners.
4. ADR-0174: amend the 2026-09-30 PV1-2 note in place (dated), stating the position rule and why t is
   wrong. Record + prompt archive; full `make test` unpiped with MAKE_EXIT; push; CI green.
   Downstream line: "no wire byte moves" or "PV1-2 corrected on N messages in same-second sequences".

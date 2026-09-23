# MSH-10 carries the log index: ADR-0181 candidate 3 lands (roadmap row oru-control-id-collision)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-23-msh-10-log-index.md`, which
> records where the live tree differed from this prompt's own
> invariants — step 2's `git diff` scope, step 6's count of identical
> oracle roots, and the DFT residual step 2's "No builder changes"
> fence put out of reach.

Context: ADR-0181 (Proposed, 2b52fc57) measured `control-id-for`'s default branch non-injective in
five classes -- 43 groups / 86 messages at the dense-7500 750 cell, 35 of them ADT; `:merge` collides
with an arm of its own. The author ruled candidate 3. The index is minted at emission from the log
position; ground truth does not move. Downstream (32k-subject corpus at e81f7026) is told: every
MSH-10 changes, the old id is the prefix of the new, the suffix is the event's log index.

Read first:
- notes/adr/0181-*.md:32-160; notes/adr/0175-arc-4-emission-add-ons.md:264-272
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj:61-97; emit.clj:77-120, 175-192
- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/messages.clj:559-580 (event->messages); planners.clj:120-130, 249-270, 485-495
- components/sim/src/ehrt/sim/identifiers.clj:70-80; components/sim-emit-hl7/src/ehrt/sim_emit_hl7/interface.clj:26
- projects/conformance/test/ehrt/conformance/mllp_pairing_test.clj:13-57
- tests: emit_hl7_test.clj:265-275, chatter_test.clj:238-250, ladders_test.clj:325-335, siu_test.clj, identifiers_test.clj:77-110
- bin/ground-truth-bracket, bin/regression-oracle (usage lines; the three roots with no :hl7 half)

Author ruling 2026-09-23: "1. 3" -- ADR-0181 key shape is candidate 3, the log index. Marker is
channel-recommended `#`, not ruled: verify it (step 3) and record the choice.

Steps:
1. Red tests. (a) unit: two events equal in (mrn, trigger, t) with distinct stamped indices ->
   distinct ids; an UNSTAMPED event -> throws (fail closed, never a silent 3-part id). (b) gate:
   `control-id-for` distinct over the WHOLE log for each of run_test's four gated-runs roots and
   the dense-7500 750 cell at its exerciser's opts -- the all-message gate ADR-0181 says was missing.
   Invariant: (b) red at 2b52fc57 with exactly ADR-0181's counts (2 groups seed-424242, 3 clinic-
   decade demo if included, 43 dense-7500, 0 elsewhere).  Gate: brick:sim-emit-hl7 shows these only.
2. Stamping. One function (`stamp-log-index`: map-indexed assoc of a namespaced key) applied at every
   funnel: `emit`, `emit-wire`, `plan-latency`, the ladder basis lookup, `identifiers`. `control-id-for`
   appends `<marker><index>` to every non-nil id and throws on a missing key. No builder changes.
   Invariant: for every event, the id at 2b52fc57 is a strict prefix of the new id; restatement and
   rung ids (planners.clj:256) are byte-unchanged; `git diff -- components/sim-engine components/
   sim-check` is empty.  Gate: step 1 green; brick:sim-emit-hl7, brick:sim, brick:corpus-io green;
   clojure -M:poly check OK.
3. Marker and length. Render the 750 cell's wire before and after; parse with corpus-io's ER7 reader
   and `judge v2` at the HAPI tier and the NIST tier; diff finding classes. If `#` fails any parser,
   use `-i<index>` and say so.  Invariant: no new finding class attributable to MSH-10; the longest
   MSH-10 at the max index is recorded.  Gate: the two judge diffs, in the record.
4. Tripwire. mllp_pairing_test's "duplicates are STILL THERE" premise goes vacuous by construction.
   Rebuild its pairing assertions on a hand-built duplicate pair (foreign corpora may carry
   duplicates; the pairing law stays) and retire the corpus-regeneration premise.
   Invariant: no test asserts that a shipped corpus carries duplicate MSH-10s.  Gate: project:conformance green.
5. Declared fixture moves: identifiers_test, siu_test, chatter_test, ladders_test, vendored_* -- every
   updated assertion names which side of the marker it asserts (ground-truth id vs restatement/rung).
   Invariant: no assertion weakened to `includes?` where it was `=`.  Gate: those bricks green.
6. `bin/ground-truth-bracket 2b52fc57 HEAD` (no flag); `bin/regression-oracle 2b52fc57 HEAD
   --declared-digest-change`.  Invariant: bracket IDENTICAL on every digested root; oracle DIFFERS on
   every root with an :hl7 half and IDENTICAL on the three without; both lists in the record.  Gate: banners.
7. Docs. ADR-0181 Proposed -> Accepted, shape + marker + date; ADR-0175:266-272 dated pointer to 0181;
   planners.clj:256 docstring ("NO ordinal suffix") corrected to the marker rule; roadmap row -> CLOSED
   with the commit; figures.edn untouched (a field moved, no count).  Invariant: `make docsgen && git
   diff --exit-code` clean.  Gate: that command.
8. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive, same slug. Push; gh run view the tip; CI green is the close marker. Record
   carries the downstream notice: shape, "old id is the prefix", index = 0-based log position and a
   join key from any message to its event, ground truth unchanged.

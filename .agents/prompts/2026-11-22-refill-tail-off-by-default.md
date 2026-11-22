# Session: the refill tail is optional and off by default -- a chain stops on its own draw or its order's end

Context. ADR-0191 bounds a refill chain three ways: the continuation draw (0.94), the order's end, and `:refill-tail-days` (default 90), measured from the LAST ARRIVAL. The tail was ruled on a premise the session then measured false: that a dense log spans about three weeks, so refills past the arrivals would sit in an otherwise empty tail. The arrival clock is 19.6 days at 28,024, but the log runs about twenty years. The tail therefore lengthens nothing; it confines every refill to the first 110 days, stops 1,441 of 1,799 chains itself (the draw stops 342), and in a long-horizon run leaves an order written years in with no refill at all (`osteoporosis-dispense` had none until it stated a tail of 36,500). The author has re-ruled. Stability across revisions is relaxed by standing ruling (ADR-0189); determinism at one revision is not. Base `b1186f18` (verify HEAD == origin/main, clean tree, `bin/preflight`), event schema 1.13.0. HALT only on a premise mismatch that changes what is built; report every other difference and carry on.

Read first (ranges at `b1186f18`):
- notes/adr/0191-*.md, whole ("Owed next": the tail found measured from the arrival clock); its session record (step 1's time spans, step 4's table, premises 3 and 4, the figures drill as last run)
- components/sim-model/src/ehrt/sim_model/config.clj 171-232, 250-272 (the three refill options: defaults, readers, schema)
- components/sim-engine/src/ehrt/sim_engine/decide.clj 2040-2080 (the chain: two draws in a fixed number, then the tail test)
- components/oracle/src/ehrt/oracle/digest.clj 1250-1290 (`osteoporosis-dispense` states `:refill-tail-days 36500`)
- demos/scenarios/dense-7500/config.edn: the facility's three refill options, written out
- components/sim/docs/operational-models.md (the refill section); docs/consuming-ground-truth.md (the refill and tail passages)

Author ruling, verbatim, 2026-11-22: "Have the prompt execute on the tail re-ruling." -- the re-ruling being the channel's: "Make the tail optional and off by default, so a chain stops on the continuation draw or the order's end. ... The tail stays available as an option for a scenario that wants a hard cutoff." The interval (28-35 days) and the continuation chance (0.94) stand as ruled on 2026-11-20.

Channel expectations (not rulings; correct from the tree and report):
- `default-refill-tail-days` goes; `refill-tail-days` returns the facility's own value or nil; nil means no bound. A stated tail keeps today's meaning exactly (days after the last arrival).
- The two draws stay first and fixed in number; only the test after them changes. So no chain's draws move, and every chain the draw or an end stopped under the 90-day tail is unchanged; the chains the tail stopped run on.
- dense-7500 drops its `:refill-tail-days` line and keeps the other two written out. `osteoporosis-dispense` drops its 36,500; its log should be IDENTICAL, since 36,500 days already bounded nothing -- measure that, do not assume it.
- At 0.94 a chain averages about 16 refills, so dense 28,024 goes from 3,596 refills to something like 27,000. That is arithmetic, not a measurement.
- A chain also ends when its patient dies or is merged away without a survivor to carry it. Say from the code which of these stop a chain today and which do not; ADR-0191 names one that does not (an expired record absorbed by a merge).
- No chain should outrun the log's existing span. If the last event moves later, report by how much and why.

Steps:
1. Predict, before any edit, for the 750, 7,500 and 28,024 cells: refills, refills per filled chronic order (mean, median, max), chains by what stops them (draw, end, death, merge), the last refill's day, the last event's day, events.
   Invariant: tree untouched. Gate: the prediction, committed ahead of step 2.
2. Red tests. (a) with no tail stated and continuation at 1, a chain runs past day 90 from the last arrival and is stopped only by the order's end. (b) with no tail stated and the default continuation, over many chronic orders, the mean chain length is within a stated tolerance of 0.94 / 0.06. (c) a stated tail still stops a chain where it did. (d) the config schema accepts an absent tail and refuses a negative one. (e) two runs at one revision are byte-identical.
   Invariant: red at the base for the stated reasons only; say which are green by construction. Gate: the bricks.
3. The change, in the option's reader, default and docstrings, and the two configs. Invariant: `classify-change` reports no event-schema change. Gate: step 2 green; `poly check`.
4. Measure against step 1; the same rows, plus `osteoporosis-dispense`'s digest before and after.
   Invariant: none; a measurement. Gate: the table beside the prediction, each difference explained.
5. Whatever the suite shows moved, each in its own commit: oracle roots (predict the set first), dense's figures by the usual drill (28,024 included; keep `dd4516c0...` labelled as the value before this change), pins, the invariant-count pin in `bin/usecase-ground-truth-oracle` if it is touched, describe's refill measure.
   Invariant: no figure in a consumer doc is one this session did not measure. Gate: the figures gate; `describe` over the new 750 cell.
6. Docs and record. The config reference: the tail is optional, absent by default, measured from the last arrival when stated, and why a long-horizon scenario should leave it out. The consumer docs' refill passages and counts. A dated addendum to ADR-0191 recording the re-ruling, the false premise and the measured result. Expectation, not a ruling: an addendum suffices; if the tree's rule wants a new ADR for a changed default, write one and say why. Still owed, restated: a per-order bound; refills for chronic orders that predate the horizon; a 90-day supply; a non-uniform first-fill lag; the pathway drug codes; prescription detail.
7. Not docs-only: full `make test` unpiped with MAKE_EXIT recorded, then `make integration`; nothing written into the tree while either runs. Archive this prompt under the same slug; record; `make docsgen`; push; `bin/post-push-verify b1186f18 <tip>`; `gh run watch --exit-status`; CI green is the close marker. The record carries the downstream line. Report: the shas, the CI run id, step 1 against step 4, the new figures, and every premise this prompt got wrong.

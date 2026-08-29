# Session prompt -- traffic-scale close: the spike rerun at target scale (ADR-0168's last obligation)

Archived verbatim as handed over. Paired record:
[`../session-records/2026-08-29-traffic-scale-close.md`](../session-records/2026-08-29-traffic-scale-close.md).

---

Context. HEAD 6eb4aa6. Arc 4 sweep 6 of 6 is this measurement session:
the plan (`.agents/plans/2026-08-24-traffic-scale-program.md`) owes a
"post-arc-3 rerun of the same spike at target scale" (:120) and a priced
"gating policy at scale" (:121-123, now ruled D1 and shipped as `gate v2
--sample-add-ons`). The appendix's two 10^6 entries (:227, :235) are
PROJECTED from a generator that no longer exists -- five arcs of payload
(streams, persons, encounters, bed cycle, scheduling, chatter, ladders,
SIU, fan-out) landed since. This session MEASURES; it changes no
behaviour. F3 is absolute: nothing interpolated is ever MEASURED.
Sweep 2's `msg-%03d` lesson: volume finds what invariants don't --
treat any new defect as a FINDING, rowed, never fixed here.

Read first: the plan whole (esp. :127-240, the labels' law and every
prior figure's run-parameters block); the throughput-spike record
(2026-08-24) Step 0's health-record shape and its two-instrument driver;
arc-0's record :261-268 (the scratch survived on penny: `dense-7500.edn`,
`driver.clj`, `cell.sh`, `run.sh` -- confirm it still does; the
45-minute re-author budget applies if not, PROJECTED stands if exceeded).

Step 0. Health record BEFORE anything timed: Linux AND Windows-side
samples (ADR-0167), unpiped invocations, quiet machine or disclose.
The dense config must be brought CURRENT, disclosed as a new scenario
version, not a comparable rerun: `dense-7500.edn` predates every opt-in
key. Produce `dense-7500-v2.edn` = the old config + the six keys the
gated corpora carry (`:persons :encounters :bed-cycle :scheduling
:chatter :charges :ladders :siu :fan-out` -- copy values from
clinic-decade's config, disclose each). Run BOTH configs at 10^4 once:
the old one is the continuity check against arc-0's curve; the v2 is
the program's own baseline. State clearly which figures are comparable
to 2026-08-24/25 (old config only) and which start a new series (v2).

Step 1. The cells, warm-up + two timed each, per phase (generate /
check / emit+spool): old config at 10^4 and 10^5 (cell C: 7,500
patients -- the arc-0 comparison, 1.81 min then); v2 at 10^3, 10^4,
10^5; v2 at 10^6 ONLY if the 10^5 wall and linear memory projection
(~1.9 GB live set, plan :239) say penny tolerates it -- a declined
10^6 is stated with the arithmetic, and both 10^6 entries stay
PROJECTED with a dated note that the basis config is superseded.
Per cell: wall, per-phase split, events, messages (v2 -- count them:
chatter/ladders/SIU/fan-out multiply the stream; messages-per-event is
itself a headline figure the program was commissioned for), peak heap,
retained. Exponents per phase per series by log-log slope over the
decades measured -- named MEASURED with their decade span only.

Step 2. Gating at scale -- the D1 policy priced for real: `gate v2
--sample-add-ons <n>` over the v2 10^5 spool (and 10^6 if taken):
wall for the sampled run, per-stratum n/gated printed, plus a
full-width run at 10^4 only for the ratio (a full 10^5 NIST-tier run
is NOT owed -- say why with the 10^4 arithmetic). Determinism: the
sampled gate twice, same verdict set, asserted.

Step 3. Paper. The plan appendix gains one MEASURED (dated) block per
series with the full run-parameters preamble; the two PROJECTED 10^6
entries either convert (measured) or gain the superseded-basis note;
the "Post-arc-3 rerun" and "gating policy" bullets close with pointers.
`[emission-add-ons]` CLOSED (6 of 6); ADR-0168's program is complete --
close it the de-scaffold way: a dated completion note in the plan and
the roadmap, NO new ADR. `[corpus-player-slices]`: re-derive what
remains (MLLP landed; accumulator corrected in sweep 5) and trim the
row to what is real. Session record: the full figure tables, health
records per cell, the comparability statement, findings rowed. Push;
CI; no tag. Scratch (v2 config, driver deltas) copied into the record
verbatim if small, else its regeneration documented -- the spike's
own lesson: a figure whose driver died is a figure nobody can check.

Fences. NO src/test change of any kind -- a measurement session; a
defect found is a row plus a one-line disclosure, and if it BLOCKS a
cell (a crash, an exhaustion) the cell is reported blocked, not
worked around. No config tuning to improve a number. Every figure
carries its health record. F3. One session; if 10^6 runs long, the
record says how long honestly.

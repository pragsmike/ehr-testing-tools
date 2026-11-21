## ADR-0190 — a community fill is drawn: a share that is filled, a lag, and discharge prescriptions under the same two options

**Status:** Accepted 2026-11-20 (author ruling of 2026-11-20, quoted
below; build session same day). Amends ADR-0187 and ADR-0189. Event
schema **1.12.0**, unchanged; describe report **1.3.0**, unchanged;
stream scheme **"1.0"**, unchanged.

### Context

Under the `:dispense` key every order an outpatient visit wrote was
filled once, `:community`, at the instant the visit closed (ADR-0187).
`dense-7500` has held that population since its clinic pathways landed:
7,791 community fills at 28,024 arrivals. Two things about it were
unrealistic and one population was missed. Every prescription was
filled. Every fill was instantaneous. And an order written during an
admission, still active and never dispensed when the stay closed, got
nothing from either half of the key. The `injuries` module is the
measured case: it prescribes as the last act of an emergency visit, and
in an all-`injuries` run all 210 orders in a closed stay share their
discharge's instant
(`.agents/session-records/2026-11-20-dense-outpatient-pathways.md`,
step 4).

Author ruling, verbatim, 2026-11-20: "Accept all, go." -- to these
defaults and this scope: "Share of prescriptions filled 0.80"; "Lag
from visit close to first fill: 1 hour to 7 days, uniform"; and
"Session A: the fill share and lag, plus discharge prescriptions. An
order written in an admission that is still active and undispensed at
discharge gets a community fill under the same two options." Standing:
frequencies are options in the site config with reasonable defaults.
Stability across revisions stays relaxed (ADR-0189's ruling);
determinism at one revision does not.

### Decision

**1. The fill is a draw.** Under the key, a prescription is filled in
the community with a stated probability, after a lag drawn uniformly
from a stated range of minutes. A prescription is one of two things:
an order an outpatient visit wrote and left open, counted from the
visit's close; or a discharge prescription (3).

**2. Two facility options.**

| key | meaning | default |
|---|---|---|
| `:community-fill-share` | probability a prescription is filled at all, a number from 0 to 1 | `0.80` |
| `:community-fill-minutes` | `[lo hi]` minutes from the close (or the discharge) to the fill, integers with `0 <= lo <= hi`, uniform | `[60 10080]` |

They sit on the facility, beside `:wards`, because a community pharmacy
belongs to no ward. The `Facility` schema refuses anything else. Each
is read through one function in `sim-model`. A share of 1 with a range
of `[0 0]` is ADR-0187's rule exactly, and that ADR's tests now run on
such a facility.

The defaults are **provisional**: a quick reading of four studies,
fitted to none.

- 78% of 195,930 e-prescriptions were filled, 72% of those for a new
  medication (Fischer et al., J Gen Intern Med 2010;25(4):284-90, PMID
  20131023).
- 17% of 150,565 new prescriptions in primary care were never filled
  (Tamblyn et al., CMAJ; https://cmaj.ca/node/149155).
- Of 32,586 new chronic-medication orders in an elderly US cohort, 75%
  were filled within 7 days and 81% within 30 (Franklin et al., J Gen
  Intern Med 2018, PMC6206334).
- After discharge from a general medicine service, 28% of patients with
  new prescriptions had not filled one or more at 7 days (PMC3642181).

One share for every prescription is a simplification, and a uniform
lag is cruder than the studies: real first fills cluster early and
trail off over weeks.

**3. Discharge prescriptions.** An order written during an admission
that is still active, still the patient's latest of its citation, and
never dispensed `:inpatient`, once its stay has been discharged, is
filled `:community` under the same share and lag, counted from the
discharge. An order the stay ended gets nothing. An order dispensed
inpatient is not filled again. A cancelled admission and an expired
patient produce none.

It is decided where the queued pharmacy step fires, and not in `decide
:discharge` as the session prompt expected. That step is the only thing
that can dispense the order inpatient, so its firing is the first
instant at which "never dispensed" is known without a new index over
the log. The outcome is the same: the lag is counted from the discharge
either way.

**4. The draws.** Two per prescription, on the patient's `:pharmacy`
stream: filled at all, then the lag. Both are always taken. A visit's
orders are drawn at the visit's close, in the orders' own order. An
admission order's are drawn when the order is written, after its
turnaround draw, and carried on the queued step, because whether it
becomes a discharge prescription is not known until later. So an
admission order takes three draws where it took one, and a patient's
second and later inpatient turnarounds are different numbers than they
were under ADR-0189. No stream's seed changed.

**5. When a queued fill is declined.** The fill is a queue entry at the
close plus the lag. When it is decided, nothing is emitted if the order
has ended, or is no longer the patient's latest of its citation; if the
patient has died; or, for a discharge prescription, if a cancelled
discharge has put the patient back in the same stay. A patient absorbed
by a merge takes the queued entry with them, as with every queued step
but a result.

**6. A community dispense carries no `:encounter-id`.** A lagged fill
can land while the same patient is inside a later visit or stay, and
the encounter stamp would then name an encounter that had nothing to do
with it (ADR-0187's second finding, reachable for the first time). The
stamp now skips every `:community` dispense. This is the one change
that applies with the key absent: an AUTHORED community dispense placed
inside an open encounter, under `:encounters`, used to be stamped. No
shipped config does that.

**7. Nothing falls off the end.** The run loop drains its queue, so a
fill is emitted however late it lands. The session prompt expected some
to fall past the run's last event; none does, in any cell.

### What a missing fill means

Under the key, a `:medication-order` with no `:medication-dispense` is
not an error. It is a prescription the patient never filled (the
share's draw), or one whose order ended, or whose patient died or was
merged away, before the lag elapsed. No event marks it. A consumer must
not read the absence as a defect, and must not assume a fill follows
its order closely.

### The erasure law

Unchanged, and still gated: delete every dispense, renumber, and the
keyless log is left, event for event. Its test runs at the default
share and lag; only its count pins moved. Checked on `dense-7500` as
well: at 750 and 7,500 the logs before and after this ADR, less their
dispenses and their index fields, are the same events in the same
order.

### What moved, predicted and measured

Seed 20260824, `--churn`, at `10ae3f71`.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| orders written in an outpatient visit | 198 | 198 | 1,925 | 1,925 | 7,791 | 7,791 |
| community fills, visit orders | 148 to 168 | 152 | 1,500 to 1,570 | 1,463 | 6,150 to 6,300 | 5,961 |
| unfilled, patient merged away within a week of the close | 0 to 3 | 6 | 5 to 30 | 66 | 20 to 100 | 224 |
| unfilled, nothing else in the log to say why | 30 to 50 | 40 | 350 to 420 | 396 | 1,480 to 1,640 | 1,606 |
| unfilled: ended first; past the end of the run | 0; 0 | 0; 0 | 0; 0 | 0; 0 | 0; 0 | 0; 0 |
| discharge prescriptions | 2 | 2 | 32 | 32 | 41 | 41 |
| of them filled | 0 to 2 | 0 | 20 to 30 | 27 | 28 to 38 | 33 |
| lag from the close, minutes (min / median / max) | 60 / 5,000 / 10,080 | 76 / 5,423 / 10,044 | 60 / 5,070 / 10,080 | 63 / 4,990 / 10,078 | 60 / 5,070 / 10,080 | 63 / 4,961 / 10,078 |
| fills carrying an `:encounter-id` | 0 | 0 | 0 | 0 | 0 | 0 |
| fills landing inside a later encounter | a handful | 0 | tens | 1 | under 200 | 15 |
| `:inpatient` dispenses | 287 | 287 | 2,533 | 2,533 | 10,332 | 10,332 |
| events | about 29,660 | 29,655 | about 126,800 | 126,741 | about 502,700 | 502,436 |

The prediction was wrong about merges. A patient absorbed by a merge
during the lag takes the prescription with them, and that takes three
to four in a hundred, not about one: the filled share is 0.76 to 0.77,
not 0.80. The log cannot separate a prescription the draw left unfilled
from one a merge took, so the merged row is an upper bound on the
merge's own share and the row under it a lower bound on the draw's.

The new 28,024 log is 188,719,656 bytes, sha-256
`bd4dec16e8358184c3a3fc0f3b7bf93c91d322fd04ca206acbcf2dcd24c5222f`;
`d50929d2...` is kept in `figures.edn` under `:pre-adr-0190`.

**Oracle roots.** Predicted to move: the two that set the key,
`osteoporosis-dispense` and `dispense-inpatient`, and no other. Both
instruments with `--declared-digest-change` are in the session record.
`osteoporosis-dispense` keeps its config: 134 of its 156 prescriptions
are filled where all were. `dispense-inpatient` states share 1 and
`[120 240]`, so its three orders outrun by a discharge are discharge
prescriptions by construction; it is the one root reaching that rule.

### Downstream

Corpora that move: every corpus generated with `:dispense`. Schema
unchanged. A retained corpus is a valid artifact of the earlier pin.
Under the key a prescription may have no fill, and a fill may trail its
visit by up to a week on the defaults; neither is a defect. A community
fill never carries an `:encounter-id`. The two options are
`:community-fill-share` and `:community-fill-minutes` on the facility;
`dense-7500` writes both out at their defaults.

### Owed next

- **Chronic refills**, with a chronic marker on an order and a fill
  number on a dispense. Already ruled, 2026-11-20: a refill interval of
  28 to 35 days and a continuation chance of 0.94.
- **Refills for chronic orders that predate the horizon.**
- **A non-uniform lag.**
- **The pathway drug codes** (carried from ADR-0189's addenda).
- Carried: the mutation column for the dispense, and counting the
  sites its operators give up.

### What this does not do

- Refills nothing.
- Marks no order chronic.
- Enforces the `Facility` schema at run time; like `:dispense-minutes`,
  the two options are validated by the schema and nothing calls it on
  a run's config.
- Makes the wire stable under the key.

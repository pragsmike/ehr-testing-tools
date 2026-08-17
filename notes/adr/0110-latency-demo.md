## ADR-0110 — Latency demo: same ground truth, two wires, the board as witness

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Author charter, 2026-08-11, verbatim (`.agents/rulings.md`, "demo
session."): this session is the demo half of the latency arc ADR-0109
built the mechanism for -- a `:latency`-bearing scenario variant and
one witnessed end-to-end run into a downstream-receiver stand-in, this
workspace's own `--board`, whose reconstruction of state from
honest-but-late traffic is exactly the class of downstream behavior
the author's own charter (ADR-0107, quoted in full below) targets.
Zero `src` changes anywhere: this session is authorship over the
landed ADR-0109 mechanism, not a code change.

**The chartering direction, restated (standing since ADR-0107)**:
*"I want to make sure that the simulation faithfully simulates what
happens in real life: lab results take time to come back, providers
take time to log things in the EHR, etc. so it's possible that a
downstream receiver of the HL7 traffic will have incomplete encounter
records for some time. That's not our problem to solve, but in order
to test that such downstream receivers handle it properly (whatever
that might mean for them) we need to supply them with such cases."*

### Tag ceremony

Design channel verified the ADR-0109 landing at `2faa5ba` by fresh
public clone. `stable-20260811-latency-second-clock` tagged annotated
at `2faa5ba`, message "ADR-0109 latency mechanism landed,
design-channel-verified 2026-08-11"; pushed; peeled ref confirmed
`2faa5bac13460bf18dc2f924f87f9667322fa2ec` -- exact match; remote had
not moved (`git fetch` confirmed `origin/main` already at `2faa5ba` at
session start; the last five CI runs on `main` were all
`completed`/`success`).

### Decision

**[A] The demo, authored as charted.** `demos/scenarios/ed-tuesday/`
gains a sibling config, `config-latency.edn`: byte-identical to
`config.edn` below the header, plus one added top-level `:latency`
block (`ehrt.sim-model.config/LatencyProfile`). `config.edn` itself and
ADR-0104's own witnessed blocks are untouched. `demos/scenarios/
ed-tuesday/README.md` gains a new section, "The second clock" -- the
two generate commands, the ground-truth invariance witness (a live
`diff`/`sha256sum` of both `events.edn` files), the `--board` play
command, and the witnessed board block showing the ADR-0109 disorder
finding live. `demos/scenarios/README.md` gains one line pointing at
the sibling config from the ed-tuesday bullet.

### The tuned profile

Ranges, minutes-authored (`sim-model/config.clj`'s own `LatencyRange`):
`:order-placed` (10-45 min) and `:result-available` (20-120 min)
approximate real specimen-to-result turnaround; `:transfer`/`:discharge`
(15-60 min each) approximate real charting lag once the clinical event
itself has happened; `:admission` (15-90 min) deliberately overlaps
the other two bands rather than dominating them.

**A first draft was rejected by live-probe, not shipped.** An earlier
`:admission` band (60-240 min, centered well above `:transfer`/
`:discharge`) produced disorder on roughly a quarter of every admitted
patient at this scenario's own seed -- statistically overwhelming, not
an occasional, notable finding, and not clinically plausible (real EDs
do not fail to chart admissions correctly for a quarter of patients).
Retuned twice by live-probe against the actual seed (not by
calculation alone): the shipped ranges above produce disorder on
exactly 8 of 92 admitted patients (seed 20260811) -- occasional and
visible, matching the driving prompt's own "at least one" bar without
overshooting into "most of them," the same "tuning is authorship"
discipline ADR-0104 already established for this scenario.

### Step 2: ground-truth invariance, witnessed

```bash
bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config.edn \
  --out-dir out/scenarios/ed-tuesday-base

bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config-latency.edn \
  --out-dir out/scenarios/ed-tuesday-latency
```

```
$ diff out/scenarios/ed-tuesday-base/events.edn out/scenarios/ed-tuesday-latency/events.edn
$ sha256sum out/scenarios/ed-tuesday-base/events.edn out/scenarios/ed-tuesday-latency/events.edn
b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1  out/scenarios/ed-tuesday-base/events.edn
b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` reports no differences; the digests match exactly -- 383
ground-truth events, byte-identical either way, the mechanism's own
guarantee (`:latency` never enters `engine/config-keys`; a fresh,
independently-seeded second RNG stream, `run.clj` ~407-417) made
visible rather than merely cited. Only rendering differs: the two
out-dirs' `msg-%03d.hl7` files carry different MSH-7 values and a
different file order (`emit-wire` sorts by transmit time, not log
order) -- the palgebra's own `GT -> TimedWire` arrow
(`docs/dev/simulator-architecture.md` section 5), visible in a diff of
two directories generated from the same seed.

### Step 3: the downstream witness

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 100000
```

Patient MRN000013 (Walker, William), pathway `ed-fast-track`: admitted
(EVN-2 clinical time `2026-08-11T03:36:00Z`), discharged 37 clinical
minutes later (`04:13:00Z`) -- ordinary, log-order-correct history. On
the latency wire, the discharge message's own sampled delay (20m54s)
is shorter than the admission message's own (1h00m46s), so the
discharge (A03, MSH-7 `04:33:54Z`) transmits before the admission (A01,
MSH-7 `04:36:46Z`) -- reordered on the wire, never in ground truth. The
board, folding messages in the order it receives them:

```
-- board snapshot: 2026-08-11T04:33:54Z --

Emergency:
  ED-H08  D'Angelo, James  MRN MRN000012  inpatient  attending: 3327386918
  ED-H12  Rodriguez, Jacob  MRN MRN000005  inpatient  attending: 3327386918
  ED-H16  Anderson-Lee, Linda  MRN MRN000009  inpatient  attending: 3327386918

inpatients: 3  active outpatients: 0  discharged: 9  merged: 0
-- board snapshot: 2026-08-11T05:43:41Z --

Emergency:
  ED-H01  Garcia-Lopez, Amanda  MRN MRN000018  inpatient  attending: 3327386918
  ED-H03  Moore, Amanda  MRN MRN000015  inpatient  attending: 3327386918
  ED-H13  Walker, William  MRN MRN000013  inpatient  attending: 3327386918
  ED-H13  Gonzalez, Emma  MRN MRN000017  inpatient  attending: 3327386918
  ED-H14  Johnson, Joshua  MRN MRN000014  inpatient  attending: 3327386918
  ED-H16  Anderson-Lee, Linda  MRN MRN000009  inpatient  attending: 3327386918

inpatients: 6  active outpatients: 0  discharged: 10  merged: 0
```

Walker's own discharge (folded first, between these two snapshots)
already removed him from the board. His admission then arrives --
`fold-message`'s own `:admission` case applies unconditionally
(ADR-0109's own Step 5 finding, live here rather than probed): it puts
him right back on the board as `inpatient` in `ED-H13`, the same bed
label the board independently shows occupied by Gonzalez, Emma in this
same snapshot, Walker's own ghost entry never having cleared -- two
patients shown occupying the same bed, one of them (Walker) already
discharged, in ground truth, before his own admission message ever
posts. His phantom entry never clears (no further message for him
exists in this pathway) -- it is still on the board at the run's own
last snapshot. The same patient in the base (no-latency) run above
appears exactly once, admitted and never seen again once discharged --
the entire disorder is the wire's doing, not the ground truth's.

The tuning above targets **occasional, not universal**: 8 of 92
admitted patients (seed 20260811) have their own admission message
arrive on the wire after their own transfer or discharge message.
Per this session's own driving-prompt fence, "fixing the board is
explicitly not this demo's business" -- `fold-message` itself is
unchanged, untouched, its behavior under disorder recorded here as
data about a real downstream-receiver failure mode, exactly the class
of case the author's own charter (quoted above) asks this demo to
supply.

Closing summary: `{:unparseable-count 0, :snapshot-count 33,
:skip-count 0, :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 1765,
:stream-span-ms 128950000, :clamped-count 0, :emitted 283,
:unfolded-count 0, :sink "ticker"}` -- the same 283 messages as the
base run's own closing summary, one fewer snapshot (33 vs 34: the
board's own tick-crossing schedule shifts when transmit times shift),
a stream span 430 seconds longer (a shifted final message extends the
wire's own tail past the last clinical event).

### Step 4: README section

`demos/scenarios/ed-tuesday/README.md` gains "The second clock" --
both generate commands, the invariance witness, the play command, the
witnessed board block above, and one sentence of reader orientation
(not a prescription): a receiver that buffered incoming messages
briefly and reconciled by clinical time (EVN-2, when present) rather
than folding strictly in arrival order would not have produced
Walker's own phantom re-admission -- whether or how to do that is the
receiver's own design question. One cross-reference line links the
base demo's own "What to look for" section down to this one.

### Step 5: the trigger's status

The user-guide deferral trigger (`.agents/plans/roadmap.md`'s own Next
section, channel-proposed, un-vetoed): "the latency-realism arc landed
PLUS one witnessed end-to-end demo of latency-realistic traffic played
into a downstream-receiver stand-in." ADR-0109 landed the arc's
mechanism half; this session executes the second condition -- one
witnessed end-to-end demo, this workspace's own `--board` standing in
as the downstream receiver. **Trigger conditions MET, PENDING AUTHOR
RATIFICATION**: whether the board counts as the stand-in the trigger's
own language anticipated, and whether to open the tool-specific
user-guide work, are the author's own calls -- this session records
the state and decides neither. The roadmap's own Next-section row is
amended to record this.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots was the prediction --
this session's own footprint is a new config file, README sections,
and close-phase files only; no `src`/`test` namespace touched anywhere.

**Bracket result.** `bin/regression-oracle 2faa5ba 916de14`
(`916de14`: this session's own config+README commit, run before the
close-phase commit as the driving prompt's own Step 4 orders it):
`IDENTICAL: every root's digest matches between 2faa5ba and 916de14` --
all 35 roots, matching the pre-analysis; no STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all skip:integration`): 612 occurrences of "0 failures, 0 errors,"
zero `FAIL`/`ERROR` anywhere -- unchanged from ADR-0109's own baseline,
consistent with a session that touched zero `src`/`test` namespaces.
`ehrt.docs-tooling.invocation-lint-test`: confirmed green within that
same run -- this scenario's own new generate/play commands (both the
base-vs-latency generate pair and the `--board` play command) resolve
and parse under the fence-path machinery. `ehrt.cli.cli-parse-guard-
lint-test`: also confirmed green, unchanged from ADR-0109's own
baseline (`bases/cli` untouched). `bin/verify-nist-lock`: OK, 6
hit-nexus-sourced coordinates matched. `gitleaks git --staged -v`
(pre-commit) and `gitleaks detect` (pre-push): no leaks found.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): all `completed`/`success` --
`2faa5ba` (ADR-0109 session-record close, 4m45s), `d6ed674` (ADR-0108
doc landing, 4m30s), `62d1d5e` (ADR-0108 architecture doc, 4m41s),
`5a2832f` (ADR-0107 CI-flake disclosure, 4m17s), `1b66fb7` (ADR-0107
session-record close, 4m42s) -- no red among the five.

### Fences

Touched exactly: `demos/scenarios/ed-tuesday/` (new: `config-
latency.edn`; the existing `README.md` gains one section), `demos/
scenarios/README.md` (one line, ed-tuesday's own bullet), `notes/adr/
0110-*.md` (this file), `notes/ADRs.md`, `notes/adr/README.md`,
`.agents/*` close-phase files. Zero `src`/`test` change anywhere. The
base `config.edn` untouched -- verified by diff of the map body below
the header, differing only in the added `:latency` key. The board/fold
untouched -- the disorder shown above is `fold-message`'s own
pre-existing, unmodified behavior under disordered input, recorded as
a live finding, not a fix.

### Deviations, dated 2026-08-11

- **The first-drafted `:latency` profile was rejected by live-probe**
  (disclosed under "The tuned profile," above) -- an initial
  `:admission` band produced disorder on roughly a quarter of admitted
  patients, retuned to the shipped, occasional-not-universal ranges.
  Not a deviation from any ruling; recorded per the driving prompt's
  own "tuning is authorship" fence (report what didn't work, not just
  what shipped), the same discipline ADR-0104 established for this
  scenario.

### Index line

```
- 2026-08-11 — latency-demo — ADR-0110
```

(appended to `.agents/plans/roadmap.md`'s own Done section; the
downstream-latency-realism Next-section row amended in place.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Latency demo: same ground truth, two wires, the board as witness — the arc's demo half, author-chartered "demo session.": a sibling scenario config, `demos/scenarios/ed-tuesday/config-latency.edn`, byte-identical to `config.edn` below the header plus one added `:latency` block; generating both configs at the same seed produces byte-identical ground truth (witnessed `diff`/`sha256sum` of both `events.edn` files, 383 events either way) while `emit-wire` renders a differently-ordered wire; a first-drafted `:admission` latency band was rejected by live-probe (disorder on ~a quarter of admitted patients, statistically overwhelming) and retuned to an occasional, not-universal shape (8 of 92 admitted patients, seed 20260811); played into this workspace's own `--board` as the downstream-receiver stand-in, the ADR-0109 disorder finding reproduces live — a lagged admission message (MRN000013/Walker) re-adds an already-discharged patient to the board, double-booking a bed another patient already occupies — `fold-message` itself untouched, its confusion the demonstration, not a defect fixed here; the user-guide deferral trigger's own second condition (one witnessed end-to-end demo into a downstream-receiver stand-in) is executed, recorded as MET, PENDING AUTHOR RATIFICATION, decided by neither the driving prompt nor this session; zero `src`/`test` changes anywhere, the oracle holds pure identity across all 35 roots

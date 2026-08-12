# 2026-08-11 — ehr-testing-tools: the latency demo, same truth two wires (ADR-0110)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `2faa5ba` (ADR-0109's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim; a deviation record follows that.

## Original prompt (verbatim)

# Session prompt -- the latency demo: same truth, two wires (ADR-0110)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session is the demo half of the latency arc, author-
ruled 2026-08-11 ("demo session."): a :latency-bearing scenario
variant and one witnessed end-to-end run into a downstream-receiver
stand-in -- the workspace's own board, whose reconstruction of state
from honest-but-late traffic is exactly the class of downstream
behavior the author's charter targets. HEAD at handoff: 2faa5ba. This
session's ADR is ADR-0110. Zero src changes anywhere: this is
authorship over the landed ADR-0109 mechanism.

THE DEMO'S SPINE (channel-specified; verify mechanics against the
tree):

1. **The sibling config.** demos/scenarios/ed-tuesday/ gains
   config-latency.edn: byte-wise the base config PLUS a `:latency`
   block (LatencyProfile schema, sim-model/config.clj). Author the
   ranges for demo legibility and clinical plausibility -- e.g.
   result turnaround tens-of-minutes-to-hours, discharge/transfer
   documentation minutes-to-a-couple-hours -- tuned at the demo seed
   so the wire VISIBLY disorders (at least one admission arriving
   after its own transfer or discharge). The base config.edn and
   ADR-0104's witnessed blocks are UNTOUCHED.

2. **Ground-truth invariance, witnessed.** Generate twice, same
   seed, base vs latency config, separate out-dirs. Witness in the
   README and ADR: `diff` of the two events.edn files -- byte-
   identical (the mechanism's own guarantee: :latency never enters
   engine/config-keys; a fresh second RNG stream, run.clj ~324-334)
   -- while the msg-%03d files differ in MSH-7 and in ORDER. Same
   ground truth, two wires: the palgebra's GT -> TimedWire arrow,
   visible in a diff.

3. **The downstream witness.** Play the latency wire with --board.
   The witnessed block documents what the board ACTUALLY shows --
   including, if the tuning achieves it, the ADR-0109 disorder
   finding live (an internally inconsistent reconstruction from a
   lagged admission). Frame it in the README with the author's own
   charter (quote it): supplying downstream receivers with such
   cases is the point; the board here is the stand-in, its confusion
   is the demonstration, and FIXING the board is explicitly not this
   demo's business (a future ruling if ever wanted). If tuning
   cannot produce a visible disorder at a reasonable seed/rate,
   that is a finding to report, not a silent retune loop --
   ADR-0104's precedent.

4. **README section.** "The second clock" (or similar): the two
   generate commands, the events.edn diff witness, the play command,
   the witnessed board block, one sentence on what a receiver COULD
   do better (buffer/timestamp-reconcile) purely as reader
   orientation -- no prescriptions. One cross-reference line from
   the base demo narrative to the latency section.

5. **The trigger's status.** The user-guide deferral trigger (roadmap,
   verbatim: "the latency-realism arc landed PLUS one witnessed
   end-to-end demo of latency-realistic traffic played into a
   downstream-receiver stand-in") -- this session completes its
   second condition. The roadmap note records: trigger conditions
   MET, PENDING AUTHOR RATIFICATION (whether the board counts as the
   stand-in, and whether to open the user-guide work, are the
   author's calls -- record the state, decide nothing).

ORACLE BRACKET: pure identity on all 35 roots, trivially -- the
footprint is a config file, README sections, and close-phase files.
Movement = STOP-AND-REPORT.

## Read first

- notes/adr/0109-*.md -- the mechanism's contracts, the disorder
  finding's exact shape (your step-3 target), the deferrals
- notes/adr/0104-*.md -- the scenario's own landing, tuning
  precedent, witnessed-block conventions
- components/sim-model/src/ehrt/sim_model/config.clj -- the
  LatencyProfile schema, exactly
- components/sim/src/ehrt/sim/run.clj ~320-335 -- the passthrough
  and the second-RNG-stream wording (cited in the README's
  invariance sentence)
- demos/scenarios/ed-tuesday/{config.edn,README.md} and
  demos/scenarios/README.md
- docs/dev/simulator-architecture.md section 5 -- the arrow this
  demo makes visible
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim "demo session." -- this charter.
- [A] The arc's chartering direction (roadmap, verbatim) -- quoted in
  the README's framing per Design 3.
- [C] The board as the downstream stand-in; the sibling-config shape;
  the pending-ratification framing of the trigger -- all flagged to
  the author in the driving conversation, un-vetoed.
- [C] NOT this session: the OBR-7/OBX-14 clinical-time fidelity
  increment (changes plain emit's frozen bytes -- a declared-oracle-
  change session of its own, named in the roadmap as future work if
  not already).

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0109 landing at `2faa5ba` by fresh
   public clone. Tag `stable-20260811-latency-second-clock` at
   `2faa5ba`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Author config-latency.edn**; validate against the schema; run
   the two-generate/diff/play sequence live; tune per Design 1/3;
   capture the witnessed blocks from real runs.

3. **One commit**: the config, the README sections, the parent
   README line if its conventions call for one.
   Commit message (ASCII only):
   `docs: latency demo -- same ground truth, two wires, the board as witness (ADR-0110)`

4. **Oracle bracket.** All 35 identical. Movement = STOP-AND-REPORT.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0110
   (the witnessed evidence verbatim, the tuning rationale, deviations
   dated); roadmap: the latency arc's demo half DONE, the arc
   closed; the user-guide trigger note per Design 5; the OBR-7
   fidelity row added if absent; .agents/rulings.md records the
   2026-08-11 "demo session." ruling; notes/ADRs.md index row;
   notes/adr/README.md count 107 -> 108; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- latency demo (ADR-0110)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: demos/scenarios/ed-tuesday/ (the new config file and
  README section), demos/scenarios/README.md (if a line is owed),
  notes/adr/0110-*.md, notes/ADRs.md, notes/adr/README.md, .agents/*
  close-phase files. The sweep RULE governs (ADR-0099 precedent).
- ZERO src or test changes. The base config.edn untouched. The
  board/fold untouched. Tuning is authorship; if the demo cannot be
  made to work from config alone, STOP-AND-REPORT.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims are verify-then-act.

## Deviation record

Executed as specified — no STOP-AND-REPORT triggered. Two disclosed,
non-ruling deviations, both recorded in `notes/adr/0110-*.md`: (1) the
first-drafted `:admission` latency band was rejected by live-probe
(disorder on ~a quarter of admitted patients) and retuned to the
shipped, occasional-not-universal ranges (8/92) — the driving prompt's
own "tuning is authorship" fence, exercised as intended, not a
departure from it; (2) the witnessed board block uses `--board 60`
(the base demo's own cadence) rather than a finer grid — a finer
`--board 3` probe located the disorder during tuning but was not the
cadence shipped in the README/ADR, chosen for reproducibility with the
base demo's own documented command style. `run.clj`'s own latency
threading was found at lines ~407-417 in the live tree, not the
prompt's own cited ~320-335 (the file has grown since ADR-0109 landed
it) — read in full at its real location before any config was
authored, no functional discrepancy from the prompt's own description.

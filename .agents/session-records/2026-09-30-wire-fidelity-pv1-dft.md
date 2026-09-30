# Wire fidelity: order-less ORU PV1-2 from the encounter's class; DFT^P03 carries the log index

2026-09-30. Base `64d55fec`. Ceremony: R30 (commit and push at each
checkpoint), taken from the session prompt, which stated no
prepare-only mode. The code commit was held locally and pushed together
with this record, so no red tip ever reached CI.

`bin/preflight` ran **late, before the docs commit rather than first** —
disclosed, not excused. Its one finding at that point was this
session's own unpushed code commit; otherwise green (repo root
`/home/mg/src/ehr-testing-tools`, `core.fileMode` true,
`core.ignorecase` unset, tree clean, HEAD not tagged `stable-*`).

## For downstream

> wire-only; ORU observations/reports in outpatient encounters now
> PV1-2 = O; DFT^P03 ids gain `#<index>`, old id a strict prefix;
> ground truth unchanged.

Code at `39e51282`.

## Author ruling

2026-09-30, "go". The channel's expectations were taken as written and
checked against the tree: PV1-2 for the order-less ORU kinds is the
patient's class at `t` (most recent opener at or before it, I when
none); DFT's id is `mrn-P03-t#<index>` through the shared suffix
helper; DFT's own class rule is untouched.

## Where the tree differed from the prompt

1. **The ADR-0181 gate is in `components/sim/test/ehrt/sim/run_test.clj`
   (`control-id-for-is-injective-over-every-corpus-this-lane-runs`), not
   in `emit_hl7_test.clj`, and it already read MSH-10 off the RENDERED
   wire** — DFTs, chatter restatements and ladder rungs included — not
   `control-id-for` over events. So step 1(c)'s injectivity half was
   **green at baseline**: 0 duplicate MSH-10s in every gated root and
   the 750 cell. The gap was that a family minting ids beside the rule
   was invisible while it happened not to collide. The widening is
   therefore a SHAPE clause (below), and that is what went red.
2. **`seed-424242-clinic-decade` carries 19 observations, not 19
   outpatient ones.** Measured by an independent fold over its log: 13
   observations under an `:outpatient-visit`, 6 observations and 2
   diagnostic reports under an `:admission`. The corpus test pins those
   measured floors (13 O / 8 I), not 19.
3. There is no pre-existing "shared suffix helper": the suffix was
   appended inline at the end of `control-id-for`. It is extracted as
   `segments/with-log-index` (same fail-closed throw, same message and
   ex-data), and both `control-id-for` and `dft-message` call it.
4. `formats.md` and `consuming-ground-truth.md` state neither the DFT
   id shape nor PV1-2. Both say every MSH-10 lowered from an event ends
   in its log index — false for DFT^P03 until this session, true now —
   so neither needed an edit.
5. `cancel-discharge` joins the class fold as I: the engine's own
   `evolve :cancel-discharge` sets `:class :inpatient` (a reinstated
   stay). It moves no byte in any root measured.

## Step 1 — red, for the stated reasons only

Targeted run at `64d55fec` with the tests in place:

* (a) `emit_hl7_test`: outpatient observation + report render
  `["I" "I"]` where `["O" "O"]` is expected; the admission control is
  green; the hand-built walk (none / visit / after its end / admission /
  cancel-admit) renders `I I I I I` where `I O O I I` is expected.
  `run_test/order-less-oru-pv1-2-is-the-patients-class-at-t`, joining
  wire to truth by the MSH-10 `#<index>` suffix: 13 outpatient ORUs at
  424242 and 1 at the 750 cell render `I` (the seed-5 corpus was added
  after the first red run, at its measured 14; it is red for the same
  reason by the step-4 measurement). Every event rendered exactly one
  ORU, and the inpatient control held.
* (b) `charges_test/two-closes-of-one-patient-in-one-second-...`: the
  two DFTs share `MRN000001-P03-91000`.
* (c) the shape clause — every MSH-10 ends `#<digits>` or is a
  restatement `mrn-{A08|A28|A31|O01|R01}-t-<ordinal>` — red on exactly
  the DFTs: 114 (ed-tuesday), 124 (424242), 96 (seed 5), 8 (adhd-45),
  2,609 (750 cell). Injectivity: green at baseline, see above.

## Steps 2–3 — the fix (`39e51282`)

* `timelines/class-timeline` (one pass: `:admission`/`:cancel-discharge`
  I, `:outpatient-visit` O, `:cancel-admit` clears) and `class-at` (I
  when none), computed unconditionally in `emit` and `emit-wire`,
  threaded as a trailing `classes` argument of `event->messages`; every
  lower arity passes nil, i.e. today's byte. Both `:inpatient` literals
  left the builders; `order-patient-class` is untouched.
* `dft-message`: `(segments/with-log-index (str mrn "-P03-" t) ev)`.
  The latency lookup still uses `control-id-for` of the basis event.
* `charges_test`'s ENC-3 outpatient order drops its hand-authored bed
  `:location`; the existing DFT id pins become `...#5` / `...#13`
  (strict extensions).
* `git diff -- components/sim-engine` empty.

Gates: brick:sim-emit-hl7 EXIT 0, 98 `Test results` lines over two
projects, 0 failures; brick:sim EXIT 0, 38 lines, 0 failures; the
targeted tests re-run green after a behaviour-neutral rename; `poly
check` OK.

## Step 4 — measured

The four gated rendering cells and the 750 cell, rendered at
`64d55fec` and `39e51282` (same message counts both sides):

| cell | messages | changed | PV1-2 I->O | DFT ids changed | other changed |
|---|---|---|---|---|---|
| dense-7500 750 | 40,291 | 2,610 | observation 1 | 2,609 | 0 |
| seed-202-ed-tuesday | 1,387 | 114 | 0 | 114 | 0 |
| seed-424242-clinic-decade | 1,938 | 137 | observation 13 | 124 | 0 |
| seed-5-clinic-decade | 1,495 | 110 | observation 14 | 96 | 0 |

No diagnostic report flips anywhere. Each flip count equals that
cell's count of observation/report events under an outpatient opener,
computed by a separate fold (1, 0, 13, 14). **Stripping `#<digits>`
from every DFT MSH-10 and restoring `I` on exactly the flipped PV1-2s
reproduces the before-wire byte for byte at all four.**

Judges, over every changed ORU (28) plus 10 DFTs per cell (40), before
and after:

* `gate v2` (HAPI): 68/68 **pass** both sides, zero findings.
* `gate v2-nist` against `COVID19_ELR-v2.3.1`: **no new finding
  class** — identical finding-code histogram (11 codes, e.g.
  `structure/Length Spec Error` 1,794 both sides) and identical per-file
  verdicts and codes across all 68. `O` draws what `I` drew.

## Step 5 — the instruments, `64d55fec` vs `39e51282`

* `bin/ground-truth-bracket 64d55fec HEAD`, no flag — **IDENTICAL**:
  every digested root's `:ground-truth` matches, 39 roots. Exit 0.
* `bin/regression-oracle 64d55fec HEAD --declared-digest-change`
  (soundness: yes outside the leading docstring) — **DIFFERS on 12,
  IDENTICAL on 30.**
  * DIFFER: `anemia`, `chatter-charges`, `colorectal`, `dementia`,
    `fibromyalgia`, `hypothyroidism`, `osteoarthritis`, `osteoporosis`,
    `total-joint-replacement-engine`,
    `urinary-tract-infections-engine`,
    `urinary-tract-infections-history-engine`,
    `veteran-prostate-cancer`.
  * IDENTICAL: `allergic-rhinitis`, `appendicitis`, `asthma`,
    `attention-deficit-disorder`, `bed-cycle`, `bronchitis`,
    `death-fixture`, `demographic-fold`, `dermatitis`,
    `ear-infections`, `ear-infections-engine`,
    `ear-infections-history-engine`, `encounter-horizon`,
    `immunization`, `injuries`, `med-rec`, `metabolic-syndrome-care`,
    `order-pathway`, `rheumatoid-arthritis`, `scheduling`, `sepsis`,
    `sinusitis`, `sleep-apnea`, `sore-throat`, `veteran-lung-cancer`,
    `veteran-ptsd`, `veteran-self-harm`,
    `veteran-substance-abuse-treatment`, `vhd-pulmonic`,
    `vhd-tricuspid`.

  **Predicted before the oracle finished, and matched exactly**: every
  root was run in-process at HEAD and counted for observation/report
  events under an outpatient opener and for DFT^P03 messages. The
  eleven module roots carry 3–242 such events each (e.g.
  `veteran-prostate-cancer` 242, `dementia` 5) and no DFT;
  `chatter-charges` carries 99 DFTs and no outpatient observation.
  Every IDENTICAL root carries zero of both (or no `:hl7` key:
  appendicitis, ear-infections, sore-throat).

## Step 6 — docs

ADR-0181 dated amendment (DFT joins the index rule via
`with-log-index`; the gate's shape clause); ADR-0174 dated note (PV1-2
on the order-less ORU kinds derives from the encounter's class);
`roadmap.md#dft-control-id-collision` CLOSED to `## Done` at
`39e51282`; `formats.md` / `consuming-ground-truth.md` unchanged (see
"differed" item 4).

## Step 7 — the suite

Full `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`, run after `make docsgen` with this record and the prompt archive on disk and staged: **MAKE_EXIT=0** — 432 `Test results` lines, 29,255 passes, 0 failures, 0 errors.

## Findings, not acted on

1. **`pv1-segment` renders an empty PV1-3 on every inpatient
   observation/report** in the measured corpora: all 1,019 order-less
   ORUs under an admission at the 750 cell carry no `:location` on the
   event (measured). Not this session's defect — it predates it and
   PV1-2 was the ruled field — but a consumer reading class `I` with no
   bed may want to know.
2. The prompt's step list names `brick:sim-emit-hl7` as the gate for the
   corpus-scale tests; they live one layer up in `ehrt.sim.run-test`
   by this repo's own convention (the population half of an emit-hl7
   law is always `ehrt.sim.*-run-test`), so brick:sim was run too.

CI green on the pushed tip is this session's close marker.

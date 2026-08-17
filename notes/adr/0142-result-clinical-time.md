## ADR-0142 — Clinical time on the result wire: OBR-7 and OBX-14

**Status:** In progress (author-directed, autonomous session per R30),
opened 2026-08-16.

### Context

ADR-0109 built the second clock in the emitter seam and, in the same
step, wrote down exactly what it could shift: its own field audit found
**two** timestamp-bearing fields in this project's emitter — MSH-7
(transmit, every message type) and EVN-2 (clinical, ADT only) — and
classified PV1-44/45, ORC-9, OBR-7 and OBX-14 as "would be clinical
time if ever added," all four **not rendered**. That audit's own
consequence is the gap this session closes: a latency-shifted ORU
carries only MSH-7, so a downstream receiver handed a late result has
nothing on the wire to back-date it with. Real feeds carry both clocks
on result messages; this one did not.

ADR-0109's Named deferrals and `.agents/plans/roadmap.md`'s
downstream-latency-realism row both name this increment as "a future,
declared-oracle-change session of its own." This is that session.

It is an **emitter-seam change** and a **declared oracle change**:
plain `emit`'s frozen bytes move on every root that emits an ORU. It is
**contract-neutral**: the ground-truth event log's SHAPE does not
change, `event-schema-test` stays green with no version bump, and
`components/sim-engine/resources/sim-engine/event-schema*.edn` are not
touched (`:event-schema-version` stays `"1.0.0"`). Anything that wants
to ENTER the log is a schema change under ADR-0141 Q-A's versioning,
ruled separately, not here.

### Author rulings, verbatim

- Session scope: option **(a)** of the latency follow-on set — the
  OBR-7/OBX-14 clinical-time increment. FHIR-side latency and late
  amendments/A08 stay named deferrals; `emit-fhir` and `sim-engine` are
  not touched.
- **Q1 (OBR-7 value): "a"** — the result event's own `:t`, rendered via
  `hl7-timestamp` exactly as EVN-2's `clinical-ts` is; the
  order-placed-`:t`/OBR-22 variant is a named revisit, not this session.
- **Q2 (OBX-14 in `observation-obx-segment`): "a"** — render it in all
  three ORU shapes; the positional pad OBX-9..13 (and 7-8 when the
  observation carries neither) is accepted and disclosed in this ADR and
  in that builder's docstring, superseding its "never a positional pad"
  sentence for OBX-14 only.
- Tag licenses (case i, channel fresh-clone verification + author CI
  relay 2026-08-16 via `gh run list`): *"Pay it, message verbatim"* —
  `24f351d` (run 31961309197) and `c90c9bd` (run 31975476669). Both
  paid at Step 0; see "Tag ceremony" below.

**Q3, opened and ruled in-session (scope collision, not in the prompt's
own question set).** The driving prompt's Context says "OBR-7 wherever
`obr-segment` renders" — but `obr-segment` also renders in
`orm-message` (`emit_hl7.clj:646`), while the prompt's Step 3 fence,
its Fences block, and its STOP-AND-REPORT list all hold ORM untouched,
and its own mover rule ("does its digest contain any ORU^R01") and its
Step 2 test set are ORU-only. Reported rather than silently resolved.
Author ruling, 2026-08-16, by selection: **"Results only; ORM
byte-frozen"** — `obr-segment` gains an extra arity carrying
`clinical-ts`, used only by the ORU builders; `orm-message`'s call site
is literally unchanged, so ORM^O01 stays byte-frozen and the mover
prediction stays ORU-only. Recorded as a revisit trigger alongside Q1's
own (below), not as a silent narrowing.

### Tag ceremony

Both licensed tags paid at Step 0, in date order, through
`bin/tag-ceremony` with `--push`; each finished with its own peeled-ref
verification against the remote, and both were confirmed again with
`git tag --points-at` and `git ls-remote --tags`:

```
stable-20260816-fence-battery        -> 24f351dfc90f7e77958f33909d084ec89ecef5b7
stable-20260816-event-log-contract   -> c90c9bdba34e04b1d7d73ce02c345aa41b4d0a5e
```

Both discharge the deferred close tags the two predecessor sessions
recorded as pending (`.agents/session-records/
2026-08-16-fence-battery-ruled-fixes.md`,
`.agents/session-records/2026-08-16-event-log-contract-arc.md`) — the
second of which explicitly recorded that the first was NOT paid because
the author-side CI relay was absent from its prompt. This session's
prompt carried it.

### Step 0 — preflight, disclosed in full

`bin/preflight` (plain), every finding as printed:

- **FINDING: a red run appears among the last five CI runs on `main`.**
  `0f647ea` (run 31962855332), 2026-08-16T17:51:29Z, *"docs: the
  ground-truth event log's census…"*. Read to conclusion rather than
  passed: the failure is `FAIL in (every-real-item-is-indexed-test)
  (index_completeness_test.clj:134)`, "Test results: 42 passes, 1
  failures, 0 errors", exit 1 — the event-log-contract arc's own
  index-completeness late catch, which that session recorded and fixed
  in the same arc. Every commit after it (`c6d55e2`, `4ba51f7`,
  `3b979e9`, `c90c9bd`) is green, including the tip this session
  branches from. No live defect; disclosed, not silently passed.
- Edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/` — OK.
- Tree clean including untracked — OK (`git status --short | wc -l` = 0).
- Local HEAD `c90c9bdb…` matches `origin/main` — OK.
- Last `stable-*` tag `stable-20260815-review-3-fixes`; **DISCLOSED:
  HEAD not tagged** — discharged immediately by the two tag payments
  above.

**Full-suite baseline.** `make test`, unpiped, full log to a file,
`MAKE_EXIT=$?` captured: **`MAKE_EXIT=0`**, **332** zero-failure blocks,
**17,054** passes, **zero** `FAIL`/`ERROR` report lines. That
reconciles **exactly** against ADR-0141's own 332-block / 17,054-pass
baseline — no drift in either figure. (A first count returned 664 and
was re-derived rather than reported: `make test`'s output states the
result twice per namespace — once as a bare `0 failures, 0 errors.`
line and once inside the coloured `Test results: N passes, 0 failures,
0 errors.` line — so a pattern matching both double-counts. All three
independent counts agree at 332: `^0 failures, 0 errors\.$`,
`Test results:.*0 failures, 0 errors`, and `^Ran .* tests containing`.)

`clojure -M:poly check`: **OK**, exit 0.

### Step 1a — the field audit, re-derived from the live tree

Re-derived by reading every segment builder and every ORU builder in
`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` at
`c90c9bd`, not copied from ADR-0109. Line cites verified against the
tree (the driving prompt's own cite for the three ORU builders,
":626-770", is corrected here to :649-767; every other prompt cite held).

| builder | line | segments it renders | timestamp fields today |
|---|---|---|---|
| `orc-segment` | :580 | ORC-1 (`NW`), ORC-2 (placer = control-id) | **none** — ORC-9 not rendered |
| `obr-segment` | :591 | OBR-1 (set-id), OBR-2/3 (empty), OBR-4 (CWE panel concept) | **none** — OBR-7 not rendered |
| `obx-segment` | :602 | OBX-1..OBX-8 | **none** — OBX-14 not rendered |
| `observation-obx-segment` | :679 | OBX-1..OBX-6 always; OBX-7/8 only when the observation carries `reference-range` or `interpretation` | **none** — OBX-14 not rendered |
| `oru-message` (`:result-available`) | :649 | MSH, PID, PV1, ORC, OBR (:673), 1 `obx-segment` per `:results` entry (:665), Z | **MSH-7 only** |
| `observation-message` (`:observation`) | :714 | MSH, PID, PV1, ONE `observation-obx-segment` (:735), Z — no ORC/OBR | **MSH-7 only** |
| `diagnostic-report-message` (`:diagnostic-report`) | :746 | MSH, PID, PV1, ORC, OBR (:766), 1 `observation-obx-segment` per embedded child (:758), Z | **MSH-7 only** |

Confirmed by grep over the whole namespace: `hl7-timestamp` (:112) is
called at exactly seven sites — :474/:475, :509/:510, :536/:537 (the
three ADT builders' `clinical-ts`/`transmit-ts` pairs) and :636, :662,
:726, :755 (the four order/result builders' `transmit-ts`, no
`clinical-ts` computed at all). **OBR-7 and OBX-14 appear nowhere in
the emitter**, in any spelling. `transmit-seconds` (:435) is the only
shift point and is untouched by this session.

**ORC-9 stays OUT of scope**, explicitly: `orc-segment` renders ORC-1/2
and gains nothing here. Reaching ORC-9 would require a seven-field
positional pad on a segment whose own two rendered fields are order
identity, not time, and the prompt's own STOP-AND-REPORT list names it.
It remains classified exactly as ADR-0109 left it: "would be clinical
time if ever added."

### Step 1b — oracle mover prediction (population-closure law)

Enumerated from the LIVE tree in both halves — the root set from
`components/oracle/src/ehrt/oracle/digest.clj:544-579` (**35 roots**,
counted from the map, not from any ADR), and the per-root content from
this session's own PRE-digest run over all 35 (`ehrt.oracle.digest`
against a synthetic from-scratch classpath at `c90c9bd`; 35 `.edn`
files written, `sha256sum` manifest recorded as the baseline). Never
from a list in the prompt or in any ADR.

**Predicted MOVERS — 14 roots, every root whose digest contains
`ORU^R01`:**

| root | ORU^R01 | OBX\| | OBR\| |
|---|---|---|---|
| anemia | 12 | 132 | 12 |
| colorectal | 57 | 57 | 0 |
| dementia | 7 | 7 | 0 |
| fibromyalgia | 2 | 2 | 0 |
| hypothyroidism | 8 | 8 | 0 |
| injuries | 63 | 63 | 0 |
| osteoarthritis | 31 | 31 | 0 |
| osteoporosis | 154 | 154 | 0 |
| sepsis | 67 | 81 | 23 |
| total-joint-replacement-engine | 185 | 931 | 185 |
| urinary-tract-infections-engine | 338 | 1623 | 121 |
| urinary-tract-infections-history-engine | 558 | 2474 | 190 |
| veteran-prostate-cancer | 280 | 526 | 18 |
| veteran-ptsd | 6 | 6 | 0 |

**Predicted IDENTICAL — the remaining 21 roots:**
`allergic-rhinitis`, `appendicitis`, `asthma`,
`attention-deficit-disorder`, `bronchitis`, `death-fixture`,
`dermatitis`, `ear-infections`, `ear-infections-engine`,
`ear-infections-history-engine`, `med-rec`, `metabolic-syndrome-care`,
`rheumatoid-arthritis`, `sinusitis`, `sleep-apnea`, `sore-throat`,
`veteran-lung-cancer`, `veteran-self-harm`,
`veteran-substance-abuse-treatment`, `vhd-pulmonic`, `vhd-tricuspid`.

**Split: 14 move, 21 identical.** The driving prompt's own aside said
"the 27-ish predicted-identical roots"; the live derivation says **21**,
and the derivation wins (this is precisely the population-closure law's
own point, ADR-0139 rule 9). Disclosed, not reconciled away.

Two findings that fall straight out of the census and bear on scope:

1. **`ORM^O01` count is ZERO across all 35 roots.** No oracle root emits
   an order message at all, so the Q3 ruling costs the oracle nothing
   either way — but it also means the oracle could never have witnessed
   an ORM change, which is its own argument for keeping ORM frozen
   rather than moving it unwitnessed.
2. **Every `OBR|` in the digest set sits inside an ORU** (ORM being
   absent), so the ORU-keyed prediction is complete for OBR-7 as well as
   OBX-14 — there is no root that gains OBR-7 without gaining OBX-14.

Every predicted mover contains at least one OBX; six of the fourteen
also contain OBR (anemia, sepsis, total-joint-replacement-engine, both
UTI engine roots, veteran-prostate-cancer). Ground truth is untouched
by this change, so each mover's `.edn` moves only in its `:hl7` half.

### Step 1c — the doc/demo result-strip census

Enumerated over the scan-root class (`docs/**`, `components/*/docs/**`,
`demos/**`, `notes/**`, plus `.agents/**` and `README.md`), restricted
to TRACKED files (`git ls-files -z | xargs -0 grep -l`). Two patterns:
`OB[RX]|` (a rendered OBR/OBX strip) and `MSH|.*ORU\^R01` (a rendered
ORU message header). **Both patterns return the same three files:**

| file | what it embeds | gated? |
|---|---|---|
| `demos/traces/order-result/README.md` | a fenced ORM^O01 + ORU^R01 excerpt, "verbatim from `messages.txt`" — 1 OBR strip + 3 OBX strips in the ORU half | **NO** |
| `demos/traces/order-result/messages.txt` | the captured 12-message trace itself (3 ORU) | **NO** |
| `demos/traces/emit-state/messages.txt` | the captured HL7 half of the M6 emit/FHIR pair (3 ORU) | **NO** |

**Gating verified negative, not assumed:** no `Makefile` target
references `demos/traces` (checked against all 17 targets, `docsgen`'s
seven dependencies included); no workflow under `.github/` references
it; no `*_test.clj` reads it; `bin/demo-exerciser-ed-tuesday` and
`bin/demo-exerciser-clinic-decade` exercise `demos/scenarios/`, not
`demos/traces/`. The only tracked mechanism touching these paths at all
is `.gitattributes:27` (`demos/traces/**/messages*.txt -text`), which
protects their bytes from EOL rewriting but checks nothing about their
freshness. **`demos/traces/**` is an ungated derived-artifact tree** —
so all three files are fix-forward-with-disclosure items in Step 4
(regenerate by hand, dated errata note, and a roadmap row naming the
missing gate).

**Two of the prompt's three known-from-clone hits did not survive the
tree** and are corrected rather than trusted:

- `demos/traces/README.md` — **no strip.** Prose only; it names
  `ORM^O01 + ORU^R01` as the demo's subject and embeds no message text.
  Nothing to regenerate.
- `demos/traces/emit-state/README.md` — **no strip.** One prose mention
  at `:96` ("OBX-8 `H` in the ORU^R01 message (`messages.txt`, not
  shown…)"), explicitly declining to quote the strip. That sentence
  stays TRUE after this change: OBX-8 keeps its position and value; the
  new field lands at OBX-14.
- `demos/traces/order-result/README.md` — **confirmed**, as above.

The other four trace dirs (`boarding-transfer`, `module-mix`,
`persona-enriched`, `site-profiles`) contain zero ORU messages, hence
zero OBR/OBX strips, and are predicted byte-frozen — the demo-side twin
of the oracle prediction.

**`docs/formats.md` describes no ORU field list** (zero occurrences of
`ORU`, `OBR`, `OBX` or `ORC` in the whole file), so the Step 4 census
condition resolves to a no-op there — and no event-log section is
touched either way. Two other files carry HL7 strips that are NOT
result messages and are predicted unaffected: `docs/locators.md:197`
and `docs/manual/05-batch-delivery.md` (ADT^A01/A03 only, MSH/EVN).
Untracked-by-this-class external fixtures (`test-fixtures/v2-nist/…`,
`test-fixtures/v2/simhospital/messages.out`) carry OBR/OBX strips but
are vendored third-party corpora, never regenerated by this project —
named here so the census is complete, and deliberately untouched.

### Remaining steps

Step 2 (red first), Step 3 (green + oracle bracket), Step 4 (docs) to
follow; prediction-vs-actual, re-baselines, and the footprint land in
this file at close.

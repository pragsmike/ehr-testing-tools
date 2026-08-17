## ADR-0142 — Clinical time on the result wire: OBR-7 and OBX-14

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-16.

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

### Step 2 — red first

`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/result_clock_test.clj`,
a SIBLING of `latency_test.clj` rather than more of it. The reason is
in the new file's own docstring: half of what it asserts is about
plain `emit` — that a result message carries its own clinical instant
at all, latency or no latency — which `latency_test`'s own "the second
clock" docstring would misdescribe. The other half IS the split-clock
law, made for results exactly as `latency_test` makes it for EVN-2.
`latency_test`'s 100-trial identity property stays where it is and is
re-run, never duplicated.

OBR-7 and OBX-14 are read off the RAW ER7 text (splitting a segment on
`|`, so field n is element n exactly) rather than through the parser's
field accessors, so an ABSENT trailing field reads as `nil` rather
than as whatever an accessor happens to do at an out-of-range index —
the red has to be "the field is not there", unambiguously.

RED, captured against the unfixed emitter with no src change present
in the tree (checkpoint isolation satisfied by construction, since the
src edit had not yet been made):

```
Ran 6 tests containing 58 assertions.
20 failures, 0 errors.
{:test 6, :pass 38, :fail 20, :error 0, :type :summary}

expected: (= expected (raw-field obr 7))
  actual: (not (= "20240101010300+0000" nil))
```

| failing test | failures |
|---|---|
| `result-available-oru-renders-obr-7-and-obx-14-as-the-events-own-clinical-time` | 6 |
| `observation-oru-renders-obx-14-as-the-events-own-clinical-time` | 1 |
| `diagnostic-report-oru-renders-obr-7-and-obx-14-on-every-child` | 3 |
| `emit-wire-shifts-msh-7-on-every-oru-shape-and-never-obr-7-or-obx-14` | 10 |

Two of the six were GREEN in the red run by design and had to stay
green after: the per-shape `emit-wire` identity witness (both sides
move together or neither does) and the contract-neutrality assertion.
Disclosed, since a red capture where some tests pass is worth naming
rather than glossing.

**Exit-code honesty (D2-6's own law, applied to a run it does not
strictly cover):** the red capture was `clojure -M -e` around
`clojure.test`, which does NOT exit nonzero on failure. The claim
therefore rests on the captured assertion text and summary map above,
never on an exit status. The full `make test` gates below capture
`MAKE_EXIT` explicitly.

### Step 3 — the mechanism

- **`obr-segment` gains a 3-arity** `[set-id concept clinical-ts]`:
  OBR-5/OBR-6 empty positional fields, OBR-7 = `clinical-ts`. The
  2-arity is unchanged and is what `orm-message` still calls, so
  ORM^O01's call site has a literally empty diff (Q3).
- **`obx-segment`** takes `clinical-ts`: OBX-9..13 empty pad, OBX-14 =
  `clinical-ts`. One caller (`oru-message`).
- **`observation-obx-segment`** takes `clinical-ts`: pad from wherever
  OBX-7/8 left off up to OBX-13, then OBX-14. Two callers, both ORU
  (`observation-message`, `diagnostic-report-message`).
- Each of the three ORU builders computes `clinical-ts (hl7-timestamp
  reference-date t utc-offset)` — the same expression the three ADT
  builders already use — and threads it.

**The `observation-obx-segment` docstring amendment (Q2 a), in place
and dated, not deleted.** That builder's own extension discipline read
"never a positional pad for a field nothing supplies". OBX-14 requires
a pad, and reaching it requires padding through OBX-7/8 as well when
the observation carries neither reference-range nor interpretation.
The amendment states the distinction that keeps the rule coherent
rather than merely excepted: **OBX-7/8 pad for a value the observation
MIGHT NOT HAVE, whereas `clinical-ts` derives from `:t`, which every
event in the log carries by construction — there is no case where the
pad leads to nothing.** The original sentence is retained and stands
for every other field.

Each ORU builder's own ADR-0109 docstring sentence ("OBR-7/OBX-14 …
not rendered") is corrected in place and dated. `orm-message`'s is
corrected too — and it is the one that is **still true**; its docstring
now says why it is deliberately so, rather than leaving a reader to
infer an oversight.

Untouched, as fenced: `plan-latency`, `emit-wire`, `transmit-seconds`,
`msh-segment`, `evn-segment`, every ADT builder, `orm-message`'s body,
`emit-fhir`, `sim-engine`. `emit_hl7.clj` is the only `src` file this
session changed.

### Step 3 — the gate

`make test`, unpiped, full log to a file, `MAKE_EXIT=$?` captured:
**`MAKE_EXIT=0`**, **334** zero-failure blocks, **17,176** passes,
**zero** `FAIL`/`ERROR` lines. Reconciled against Step 0 exactly, and
per namespace rather than in aggregate:

| delta | source |
|---|---|
| +116 | `ehrt.sim-emit-hl7.result-clock-test`, 58 assertions x 2 project contexts |
| +4 | `emit-hl7-test`, the one re-baselined assertion becoming three, x2 |
| +2 | `ehrt.docs-tooling.test-source-live-path-lint-test`, 137 -> 138, x2 — its assertion count is a function of the test files it scans, and this session added one |
| **+122** | matching **17,176 - 17,054** exactly; blocks **332 + 2 = 334** |

`ehrt.sim-emit-hl7.latency-test` is UNCHANGED at 7 tests / 171
assertions in both runs — the 100-trial identity property held green
straight through the change, which is the assertion that both sides
moved together.

**Run hygiene, disclosed:** the first post-change full run was started
before two docstring edits landed and is not what is claimed above. A
second, clean `make test` was run with `out/` cleared and the tree in
exactly the state committed at `14e718f`; it returned the identical
figures, and it is that run the table above reports.

### Step 3 — the one pinned test re-baselined

Per test, with old, new, and why — never a silent edit:

| test | old | new | why |
|---|---|---|---|
| `ehrt.sim-emit-hl7.emit-hl7-test/observation-emits-oru-with-one-obx-and-no-orc-or-obr` | `(= 7 (count (str/split obx-line #"\|" -1)))` | `(= 15 ...)`, plus OBX-7 and OBX-8 asserted **empty** | Reaching OBX-14 pads through OBX-7..13, so the field count is now fixed at 14 for every OBX. ADR-0029's D1 property was phrased as field ABSENCE; what D1 actually cared about — that a reader cannot mistake this observation for one carrying a range — survives intact and is now asserted as emptiness. Ruled by Q2 "a", which names the OBX-7/8 pad explicitly. |

One further test edit that is NOT a re-baseline and is recorded so it
is not mistaken for one: `latency-test`'s
`emit-wire-msh-only-message-types-shift-their-sole-timestamp-field`
had a `testing` string reading "ORM^O01/ORU^R01 … no rendered
OBR-7/OBX-14", while its assertions only ever exercised ORM (`\^O01`).
The claim was wider than the test. The string is amended and dated;
**no assertion changed**.

### Step 3 — the oracle bracket

`bin/regression-oracle c90c9bd HEAD`, exit 1 — `DIFFERS`, which is the
declared-change signal this session predicted, not a failure. The
script's own soundness line reads `declared-digest-change: no
(soundness: yes outside ns form)`: `digest.clj` is IDENTICAL between
the two sides, so both ran the same measuring instrument and the
divergence is genuinely in the emitter.

**Prediction vs actual: EXACT MATCH, no residue in either direction.**
The actual mover set, extracted mechanically from the manifest diff
and compared with `diff` against the Step 1 prediction file:

```
predicted: 14  actual: 14
diff predicted-movers.txt actual-movers.txt
EXACT MATCH -- prediction == actual, no residue either way
```

`anemia`, `colorectal`, `dementia`, `fibromyalgia`, `hypothyroidism`,
`injuries`, `osteoarthritis`, `osteoporosis`, `sepsis`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`,
`urinary-tract-infections-history-engine`, `veteran-prostate-cancer`,
`veteran-ptsd` — and the other **21 roots byte-identical**. No
STOP-AND-REPORT fired.

### Step 4 — the ungated demo traces, and what regenerating them found

The three strip-bearing files from the Step 1c census, regenerated by
hand (there being no gate to regenerate them through), from each
demo's own README-declared command at `--seed 42 --patients 3`:

| file | changed lines | class |
|---|---|---|
| `demos/traces/emit-state/messages.txt` | 18 | **ADR-0142 only** — 3 `OBR-7`, 15 `OBX-14`, across the three ORU messages |
| `demos/traces/order-result/messages.txt` | 30 | 18 ADR-0142 + **12 pre-existing `PV1` drift** |
| `demos/traces/order-result/README.md` | the excerpt strip | same two classes, distinguished in a dated errata note in the file |

**A finding, from running rather than reading.** The two
`messages.txt` files are supposed to be identical — `emit-state`'s own
README says "same as `order-result/messages.txt`", the two demos
sharing one seed and one config by construction. They were NOT: the
`order-result` copy had been captured BEFORE PV1 gained its trailing
positional fields (the site-profiles milestone's PV1-36 disposition)
and had sat drifted ever since, because nothing regenerates or
freshness-checks `demos/traces/**`. Both are now regenerated and are
byte-identical at 5,822 bytes, so the README's claim is true again.
The two classes are separated in the errata rather than blended, so a
later reader is not told ADR-0142 moved PV1.

**The ORM freeze, witnessed on a committed artifact rather than only
asserted.** `emit-state/messages.txt` was current, so its regeneration
isolates ADR-0142's change exactly — and of the six `OBR` lines in that
file (three ORM, three ORU), **only the three ORU ones changed**. The
three `ORM^O01` messages are byte-identical across the regeneration.
`ground-truth.edn` in both demo directories is untouched, as ground
truth must be.

`demos/traces/**`'s missing gate is a roadmap row, not a fix — the
proposed shape (a `docsgen` target re-running each trace's own
README-declared command, plus the CI freshness diff every other
generated surface already gets) is recorded there for whichever session
takes it, and named as a candidate for repo review 4's D5.

The other four trace directories (`boarding-transfer`, `module-mix`,
`persona-enriched`, `site-profiles`) contain no ORU and were predicted
byte-frozen; `git status` after the regeneration confirms it — they are
not in the diff.

### Step 4 — the manual, the architecture doc, and `events.edn`

`docs/manual/04-time-on-the-wire.md` gains a "When the result is late"
section built on a strip regenerated THIS session (seed 20260811,
`out/` cleared first, both `corpus generate sim` commands run from the
chapter's own text): patient MRN000005's CBC panel, resulted
`03:22:00Z`, transmitted `04:07:40Z` — a 45m40s sample from
`config-latency.edn`'s own `:result-available` 20-120 minute band. The
two wires are shown side by side: `MSH-7` moves by 45m40s, `OBR-7` and
`OBX-14` are the SAME BYTES on both, and the message's own position
moves (`msg-020` -> `msg-023`) under `emit-wire`'s transmit-time sort.
The chapter's existing sentence "exactly two timestamp-bearing fields"
is given a dated update rather than a silent rewrite, and the ORM
asymmetry is stated so a reader does not read it as an omission.

**No `config-latency.edn` widening was needed and none was made.**
Step 4's conditional (add result coverage only if the demo-exerciser
and README claims survive byte-for-byte) never engaged: that file
ALREADY covers `:order-placed` (10-45 min) and `:result-available`
(20-120 min), verified by reading it before acting.

**`events.edn` did not move, anywhere.** The chapter's own
ground-truth-invariance transcript was re-witnessed rather than
trusted: `diff` prints nothing and both digests are
`b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1` —
byte-for-byte the value already committed in the chapter. The
STOP-AND-REPORT condition on `events.edn` movement never came close to
firing, which is what "contract-neutral" means operationally.

`docs/dev/simulator-architecture.md` section 5 gains a dated
2026-08-16 addendum: the arrow itself is untouched (same signatures,
same behaviour, identity property intact), and what changed is only
what a shifted message has to say for itself. `docs/formats.md` is not
edited — Step 1c's census found it describes no ORU field list at all
(zero occurrences of `ORU`/`OBR`/`OBX`/`ORC`), so its Step 4 item
resolved to a no-op, and no event-log section was touched.

### Step 4 — a gate caught this session mid-edit

`ehrt.docs-tooling.link-footnote-gate-test/no-visible-adr-token-in-prose-test`
went RED on the first draft of the chapter-4 addition:

```
docs/manual/04-time-on-the-wire.md has visible ADR-NNNN token(s) in prose: #{"ADR-0142"}
```

ADR-0102's own ruling: the manual cites ADRs through footnotes, never
as a bare token in prose, because a reader of the user manual should
not need to know this repo's internal decision numbering to follow a
sentence. Fixed by moving the citation into a `[^result-clock]`
footnote — the sanctioned mechanism, and one the same gate checks in
both directions (marker without definition, definition without
marker). Recorded rather than quietly fixed: it is a live instance of a
gate doing exactly its job on a session that had read the chapter and
still reached for the token.

`make docsgen` ran clean (exit 0) with no generated surface drifting —
none of this session's doc edits touch a generated file. The full
`make test` was re-run a THIRD time with every Step 4 doc edit in
place: `MAKE_EXIT=0`, the same 334 blocks / 17,176 passes.

### Deviations, dated 2026-08-16

1. **The Step 2 push was held until Step 3 was green**, and the two
   commits were pushed together. R30 says push at each checkpoint;
   pushing a knowingly-red commit on its own would have put a red CI
   run on `main` — manufacturing exactly the finding class
   `bin/preflight`'s own CI check exists to catch, and which this
   session already had to disclose one instance of at Step 0.
   `bin/post-push-verify` ran over the whole `760f81d1..14e718f0`
   range, so the red commit's own message was ASCII-checked like any
   other.
2. **`components/oracle` vs the prompt's own cite.** Step 1.2 said to
   enumerate the roots from `components/oracle` (the roots list); the
   list lives at `components/oracle/src/ehrt/oracle/digest.clj:544-579`.
   Minor, recorded because the enumeration's provenance is the whole
   point of the population-closure law.
3. **The prompt's "27-ish predicted-identical roots" is 21**, and its
   ":626-770" cite for the three ORU builders is :649-767. Both
   corrected from the tree, per this repo's own verification
   discipline.

### Footprint

`src`: `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj`
ONLY. `test`: `result_clock_test.clj` (new), `emit_hl7_test.clj` (one
disclosed re-baseline), `latency_test.clj` (docstrings only, no
assertion changed). Docs/demos:
`docs/manual/04-time-on-the-wire.md`,
`docs/dev/simulator-architecture.md`,
`demos/traces/order-result/{README.md,messages.txt}`,
`demos/traces/emit-state/{README.md,messages.txt}`. Registers: this
file, `notes/ADRs.md`, `.agents/plans/roadmap.md`,
`.agents/rulings.md`, `.agents/session-records/`, `.agents/prompts/`.

NOT touched: `sim-engine` (including both `event-schema*.edn`, which
are byte-identical and whose `:event-schema-version` stays `"1.0.0"`),
`sim-emit-fhir`, `sim-model`, `corpus-*`, any vendored module JSON,
`plan-latency`/`emit-wire` signatures, `orm-message`'s body, any ADT
builder, `orc-segment`, `docs/formats.md`.

### Index line

```
- 2026-08-16 — result-clinical-time — ADR-0142
```

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Clinical time on the result wire: OBR-7 and OBX-14 — the fidelity increment ADR-0109 and the roadmap both named as "a future, declared-oracle-change session of its own", executed. ADR-0109 built the second clock and in the same step recorded what it could not shift: its field audit found `OBR-7` and `OBX-14` simply **not rendered**, so a latency-shifted `ORU` carried MSH-7 alone and a downstream receiver handed a late result had nothing on the wire with which to back-date it. Both fields now render on all three shapes this project emits as `ORU^R01` (`:result-available`, `:observation`, `:diagnostic-report`), carrying the result event's own `:t` through `hl7-timestamp` exactly as `evn-segment` receives `clinical-ts` (**Q1 "a"**), and never shifting under `emit-wire` — the split-clock law ADR-0109 proved for EVN-2, made for results. **Q2 "a"** accepts the positional pad OBX-9..13 (and OBX-7/8 when the observation carries neither reference-range nor interpretation), superseding `observation-obx-segment`'s own "never a positional pad" sentence **for OBX-14 only** — amended in place and dated, with the distinction that keeps the rule coherent rather than merely excepted: OBX-7/8 pad for a value the observation might not have, whereas `clinical-ts` derives from `:t`, which every event carries by construction. **A scope collision in the driving prompt was reported rather than silently resolved and became Q3, ruled "Results only; ORM byte-frozen":** the prompt's Context said "OBR-7 wherever `obr-segment` renders", but `obr-segment` also renders in `orm-message`, which the same prompt's Step 3 fence, Fences block, STOP-AND-REPORT list, ORU-only mover rule and ORU-only test set all held untouched — executed by giving `obr-segment` an extra arity used only by the ORU builders, so ORM's call site has a literally empty diff, and ORC-9 becomes a named revisit rather than a silent ride-along (OBR-7 on an order would mean the observation time of an observation that has not happened yet). **The declared oracle change landed exactly as predicted:** the mover set was derived BEFORE any `src` edit from the live tree in both halves — the root set read from `digest.clj` (35, counted from the map) and per-root content from a PRE-digest run over all 35 — predicting 14 movers (every root whose digest contains an `ORU^R01`) and 21 identical; `bin/regression-oracle c90c9bd HEAD` returned `DIFFERS` with the actual mover set **matching the prediction exactly, no residue in either direction**, the soundness line confirming `digest.clj` itself unchanged so both sides ran the same instrument. Two prompt figures did not survive the tree and are corrected from it: "27-ish predicted-identical roots" is **21**, and `ORM^O01`'s count across all 35 roots is **zero** — no oracle root emits an order message at all, so ORM could never have been witnessed moving, which is its own argument for freezing it rather than moving it unwitnessed. **Contract-neutral by construction and verified as such:** `:event-schema-version` stays `"1.0.0"` in both halves of ADR-0141's two-artifact gate (asserted by a negative test), both `event-schema*.edn` are byte-identical, and `events.edn` did not move anywhere — the manual's own ground-truth-invariance transcript was re-witnessed rather than trusted and returned the identical digest `b4e776f7…` already committed there. Gate: `make test` unpiped, `MAKE_EXIT=0`, **334** zero-failure blocks / **17,176** passes, reconciled against ADR-0141's 332/17,054 **per namespace** rather than in aggregate (+116 the new namespace across two project contexts, +4 the one re-baselined assertion becoming three, +2 a docs-tooling lint whose assertion count is a function of the test files it scans — 122 exactly). `latency-test` unchanged at 171 assertions: the 100-trial identity property held green straight through, which is the witness that both sides moved together. **One pinned test re-baselined with disclosure, and it is a semantic change rather than a count change:** `observation-emits-oru-with-one-obx-and-no-orc-or-obr` asserted `(= 7 …)` OBX fields as ADR-0029's absence-phrased "no positional pad" property; reaching OBX-14 pads through OBX-7..13, so the count is now fixed at 14 and what D1 actually cared about — that a reader cannot mistake this observation for one carrying a range — is now asserted as OBX-7/8 being **empty** rather than **absent**. **A second finding came from running rather than reading.** The Step 1 census established that `demos/traces/**` is an ungated derived-artifact tree — no `Makefile` target, no workflow, no test, only `.gitattributes` byte protection — and regenerating its two ORU-bearing traces proved the cost: `order-result/messages.txt` had drifted 12 `PV1` lines behind the site-profiles milestone's trailing positional fields and had made its sibling's "same as `order-result/messages.txt`" claim false for several sessions. Both are regenerated (byte-identical again at 5,822 bytes) with the two change classes separated in dated errata rather than blended, and the missing gate is a roadmap row with a proposed shape, not a silent fix — `notes/adr/0139-*.md` rule 9's own class, applied to a directory. That same regeneration **witnesses the ORM freeze on a committed artifact rather than only asserting it**: of the six `OBR` lines in the file whose copy was current, only the three ORU ones changed. Manual chapter 4 gains a "When the result is late" section built on a strip regenerated this session (seed 20260811, `out/` cleared first): MRN000005's CBC resulted `03:22:00Z`, transmitted `04:07:40Z`, `MSH-7` moving 45m40s while `OBR-7`/`OBX-14` are the same bytes on both wires and the message's own position moves `msg-020` → `msg-023` under the transmit-time sort — no `config-latency.edn` widening needed or made, that file already covering `:result-available`. `emit_hl7.clj` is the only `src` file touched; `plan-latency`/`emit-wire`/`transmit-seconds` signatures and behaviour, every ADT builder, `orc-segment`, `orm-message`'s body, `emit-fhir`, `sim-engine` and `docs/formats.md` all untouched. One deviation disclosed: the red-test commit's push was **held** until the fix was green and the two were pushed together, because pushing a knowingly-red commit alone would have manufactured exactly the CI-red finding class this session had already had to disclose one instance of at Step 0

### Roadmap history (moved verbatim from roadmap.md by ADR-0144, 2026-08-17)

The `.agents/plans/roadmap.md` row this ADR owns, as it stood at `deb9a33` before the ADR-0144 row contract capped rows at six lines. The live row now states what remains and cites this ADR for the rest; this is the rest, verbatim.

- **Downstream-latency realism -- MECHANISM LANDED 2026-08-11
  (ADR-0109), DEMO LANDED 2026-08-11 (ADR-0110), arc CLOSED;
  OBR-7/OBX-14 increment LANDED 2026-08-16 (ADR-0142).**
  *Lands against Event schema v1.0.0* (ADR-0141, 2026-08-16): further
  latency-realism work now sits downstream of a versioned event contract, so a
  change altering the log's shape owes a version bump rather than a silent edit.
  `:latency` never reaches `engine/config-keys` (ADR-0109), so today's mechanism
  is emit-only and contract-neutral.

  New chartering direction, author
  verbatim, 2026-08-11 (`.agents/rulings.md`, "From ADR-0107"): *"I want
  to make sure that the simulation faithfully simulates what happens in
  real life: lab results take time to come back, providers take time to
  log things in the EHR, etc. so it's possible that a downstream
  receiver of the HL7 traffic will have incomplete encounter records
  for some time. That's not our problem to solve, but in order to test
  that such downstream receivers handle it properly (whatever that
  might mean for them) we need to supply them with such cases."*

  **The ratified sequence (2026-08-11, `notes/ADRs.md` ADR-0108,
  author-ratified "Good sequence"):** (1) the simulator architecture
  doc lands first (`docs/dev/simulator-architecture.md`, ADR-0108,
  DONE 2026-08-11) -- names this extension point in one sentence
  (section 5: an arrow `GT -> TimedWire` between `engine` and the
  emitters), builds nothing; (2) **THIS latency design pass, DONE
  2026-08-11 (`notes/adr/0109-latency-second-clock.md`)** -- author
  ruling verbatim "I like a. go" (option (a), the second clock in the
  emitter seam): `ehrt.sim-emit-hl7.emit-hl7/plan-latency` +
  `emit-wire`, a `LatencyProfile` schema, `ehrt.sim.run`'s own optional
  `:latency` opt, the field audit (MSH-7 message-time, EVN-2
  clinical-time, every other HL7v2 clinical-time-candidate field simply
  not rendered by this project's emitter), the identity property (plain
  `emit` byte-frozen), and a disclosed `fold-message`-under-disorder
  finding (fixed nothing, recorded as data); (3) a guide-side treatment
  (`docs/`, user path) derives from the architecture doc afterward, in
  the author's own queue, not chartered to any session yet; (4) the
  tool-specific user manual (distinct from the generic EHR Testing
  Guide, permanently out of this workspace, `AGENTS.md`) stays DEFERRED
  under its own named trigger, author verbatim (`.agents/rulings.md`,
  "From ADR-0108"): *"I've been deferring creating the tool-specific
  user guide in tools repo (distinct from EHR Testing Guide, which is
  more generic) until things settled down and the tools were able to
  produce the realistic traffic I need. That remains to be seen, but
  it's getting more likely to verifiably happen soon."* Trigger
  (channel-proposed, un-vetoed): the latency-realism arc landed PLUS
  one witnessed end-to-end demo of latency-realistic traffic played
  into a downstream-receiver stand-in.

  **Trigger's second condition executed 2026-08-11 (`notes/adr/
  0110-latency-demo.md`):** `demos/scenarios/ed-tuesday/config-
  latency.edn`, a sibling of `config.edn` carrying a live-probed
  `LatencyProfile`, generates ground truth byte-identical to the base
  config at the same seed (witnessed `diff`/`sha256sum`) while its own
  `emit-wire`-rendered messages, played into this workspace's own
  `--board`, reproduce the ADR-0109 disorder finding live: a lagged
  admission message re-adds an already-discharged patient
  (MRN000013/Walker) to the board as `inpatient`, double-booking a bed
  another patient already occupies. `fold-message` itself untouched,
  per this session's own fence -- the board's confusion is the
  demonstration, not a defect to fix here. **Trigger conditions MET,
  RATIFIED 2026-08-11 (ADR-0112, `.agents/rulings.md` "From ADR-0112",
  the "User-guide trigger read" entry)**: this workspace's own
  `--board` counts as the downstream-receiver stand-in the trigger's
  own language anticipated, and the tool-specific user-manual work
  named below is OPEN. Provenance is channel-read, not
  author-verbatim -- the author did not veto the reading when it was
  stated explicitly in the same exchange that produced it; the author
  may still strike or correct it.

  Named deferrals from ADR-0109, still standing, each with its own
  revisit trigger (`notes/adr/0109-*.md`): FHIR-side latency
  (`emit-fhir` gets no `offsets` parameter); late amendments/trailing
  A08s (a GT-side new-event-type concern, outside the emitter-seam
  scope both ADR-0109 and this session's own fence share).

  **The OBR-7/OBX-14 clinical-time fidelity increment: LANDED
  2026-08-16 (`notes/adr/0142-result-clinical-time.md`).** Both fields
  now render on all three `ORU^R01` shapes, carrying the result event's
  own `:t` and never shifting under `emit-wire` -- so a latency-shifted
  result carries both clocks and a downstream receiver can back-date
  it. Declared oracle change, as this row anticipated: plain `emit`'s
  frozen bytes moved on 14 of the 35 oracle roots, exactly the set
  predicted from the live tree before any src edit (every root whose
  digest contains an `ORU^R01`); the other 21 stayed byte-identical.
  Contract-neutral -- `:event-schema-version` stays `"1.0.0"` and the
  committed EDN export is untouched, both fields being rendered from a
  key the log already carries.

  Named deferrals still standing after ADR-0142, each with its own
  revisit trigger:
  - **FHIR-side latency** (`emit-fhir` gets no `offsets` parameter) --
    from ADR-0109, unchanged; ADR-0142 did not touch `emit-fhir`.
  - **Late amendments / trailing A08s** -- from ADR-0109, unchanged; a
    GT-side new-event-type concern, outside the emitter seam.
  - **NEW (ADR-0142, author ruling Q1 "a" naming the alternative it
    declined): OBR-7 = the ORDER-placed `:t` with OBR-22 = the result
    `:t`.** ADR-0142 renders OBR-7 as the result event's own instant,
    which reads OBR-7 as "when this observation happened". HL7v2 also
    supports the reading where OBR-7 is specimen/collection time and
    OBR-22 (Results Rpt/Status Chng) is when the result was reported --
    a distinction this project's log can express (`:order-placed` and
    `:result-available` are separate events, joined by
    `:order-event-id`) but does not currently render. Revisit trigger:
    *a downstream-receiver case that needs specimen time distinct from
    result time.*
  - **NEW (ADR-0142, author ruling Q3): ORC-9 on `ORM^O01`.** ORM stays
    byte-frozen -- an order's clinical-time field is ORC-9 (transaction
    time), not OBR-7, and rendering OBR-7 there would put a
    plausible-looking timestamp in a field whose meaning does not fit.
    Revisit trigger: *a receiver case that needs order transaction time
    on the wire.*
- **`demos/traces/**` is an ungated derived-artifact tree -- REGISTERED
  2026-08-16 (ADR-0142), not fixed.** Seven committed `messages*.txt`
  traces and their README strips are produced by `bin/ehrt sim run` and
  regenerated by nothing: no `Makefile` target (checked against all 17,
  `docsgen`'s seven dependencies included), no `.github/` workflow, no
  test. The only tracked mechanism touching these paths is
  `.gitattributes`' `demos/traces/**/messages*.txt -text`, which
  protects their bytes from EOL rewriting and checks nothing about
  their freshness. **Found by drift, not by audit:** regenerating
  `order-result/messages.txt` for ADR-0142's own field change also
  picked up 12 changed `PV1` lines from the site-profiles milestone's
  trailing positional fields -- drift that had sat there through
  multiple sessions, and that made the sibling `emit-state/README.md`'s
  "same as `order-result/messages.txt`" claim false until this session
  made it true again. This is `notes/adr/0139-*.md` rule 9's own class
  (a population that is a registry rather than the tree), applied to a
  directory. Both files and the affected README strip are regenerated
  by hand with dated errata notes in place; what is NOT done is the
  gate. Proposed shape, for whichever session takes it: a `docsgen`
  target that re-runs each trace's own README-declared command into its
  own directory, plus the CI freshness diff every other generated
  surface already gets. Candidate for repo review 4's D5 (generated-
  artifact gating), which is where the same class already lives.

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0142 — clinical time on the result wire (2026-08-16)

- **Session scope, ruled:** option (a) of the latency follow-on set —
  the OBR-7/OBX-14 clinical-time increment. FHIR-side latency and late
  amendments/A08 stay named deferrals; `emit-fhir` and `sim-engine` are
  not touched. An emitter-seam change and a DECLARED ORACLE CHANGE
  (plain `emit`'s frozen bytes move on every root emitting an ORU),
  deliberately CONTRACT-NEUTRAL (`:event-schema-version` stays
  `"1.0.0"`; anything wanting to ENTER the log is a schema change under
  ADR-0141 Q-A's versioning, ruled separately).
- **Q1 — what OBR-7 carries: RULED (a).** The result event's own `:t`,
  rendered via `hl7-timestamp` exactly as EVN-2's `clinical-ts` is. The
  order-placed-`:t` / OBR-22 variant (OBR-7 as specimen time, OBR-22 as
  report time) is a NAMED REVISIT, roadmap-registered, trigger *"a
  downstream-receiver case that needs specimen time distinct from
  result time"*.
- **Q2 — OBX-14 in `observation-obx-segment`: RULED (a).** Render it in
  all three ORU shapes. The positional pad at OBX-9..13 — and at
  OBX-7/8 when the observation carries neither reference-range nor
  interpretation — is ACCEPTED and disclosed in the ADR and in that
  builder's own docstring, superseding its "never a positional pad"
  sentence **for OBX-14 only**. The sentence is amended in place with a
  date, not deleted, and the distinction that keeps it coherent is
  stated: OBX-7/8 pad for a value the observation might not have,
  whereas `clinical-ts` derives from `:t`, which every event carries by
  construction.
- **Q3 — scope collision, opened in-session and ruled: "Results only;
  ORM byte-frozen."** The driving prompt's Context said "OBR-7 wherever
  `obr-segment` renders", but `obr-segment` also renders in
  `orm-message`, which the same prompt's fences, mover rule and test
  set all held untouched. Reported rather than silently resolved.
  Executed by giving `obr-segment` an extra arity used only by the ORU
  builders, so `orm-message`'s call site has a literally empty diff.
  ORC-9 on ORM^O01 becomes a named revisit, trigger *"a receiver case
  that needs order transaction time on the wire"*.
- **Tag licences, author verbatim ("Pay it, message verbatim"),** case
  (i): design-channel fresh-clone verification plus the author's own CI
  relay 2026-08-16 via `gh run list`. Both paid at Step 0 and
  peel-verified against the remote: `stable-20260816-fence-battery` at
  `24f351d` (run 31961309197) and `stable-20260816-event-log-contract`
  at `c90c9bd` (run 31975476669). These discharge the close tags the
  two predecessor sessions had each recorded as deferred.

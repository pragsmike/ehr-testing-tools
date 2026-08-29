# The consumer contract: lift the moratorium, row the queue, document ground-truth consumption

Session record, 2026-08-29. HEAD at start `da21a28`; ceremony R30
(commit and push at each checkpoint, unattended), taken from the prompt.
Docs-only session by fence -- no `src`, no `test`, no runtime config.

## 0. What this session did

Three things, in three commits plus their close.

1. **The scaffolding moratorium is LIFTED** and two author-ruled rows
   joined `## Next` at PRIORITY 5 and 6.
2. **An audit was run as an unspoiled consumer over SHIPPED surfaces
   only** -- `docs/`, `ehrt --help`'s verb tables, `docs/formats.md` --
   attempting the four things a downstream consumer of the ground-truth
   event stream actually needs to do. **Fourteen gaps.** The list is
   section 2 and is this record's centerpiece.
3. **`docs/consuming-ground-truth.md`** is that list's discharge, and
   `roadmap.md#post-partition-narrative-refresh` was absorbed on the way
   past -- the six named narrative files re-derived against runs
   regenerated at HEAD.

**The audit's sharpest single finding is not in the new page at all,
because it was a live defect on four existing surfaces**: "there are
exactly 21 event kinds and the set is closed, so an unknown `:event`
value is a contract violation" stood in `docs/README.md`,
`docs/glossary.md`, `docs/dev/AUDIENCES.md` and the generated
custom-emitter use-case page, while `docs/formats.md`'s own GENERATED
section carries **28**. A consumer taking 21 as a completeness check
ships an emitter blind to seven kinds. `formats.md`'s own provenance
table said **23**, a third number. All corrected.

**The second sharpest is a measured behaviour, not a doc gap**: a piped
`ehrt sim check` reads the DEFAULT facility. Over `ed-tuesday`'s own
corpus it reports **115 `:occupancy-within-capacity` violations, every
one spurious**, because the scenario raises ED surge slots 6 -> 16 and
the check has no flag through which to learn that.

## Fences honoured

- **Docs-only.** No file under any `components/*/src` or
  `components/*/test` was touched. `components/corpus/docs/use-cases.edn`
  WAS edited -- it is the docsgen SOURCE for `docs/use-cases/*.md`, and
  two of this session's own required edits (the stale message count on
  `supply-batch-straddling-traffic.md`, one of the six P2 files, and the
  "21 kinds" sentence) are reachable only through it. Disclosed here
  rather than treated as obviously in-scope.
- **No claim without a citation.** Every figure below is either
  reproduced by a command shown, or cited to
  `.agents/plans/2026-08-24-traffic-scale-program.md`'s appendix or to a
  named source file.
- **No promise of unbuilt features.** The new page's fault-injection
  section names the two rowed futures and states outright that nothing
  on them is built.
- **The warranty's exclusions carry the same prominence as its
  guarantees**, as instructed: "What `ehrt sim check` certifies" and
  "What is not warranted" are adjacent top-level sections of the same
  weight.

## 1. Step 0 -- the lift and the rows

`AGENTS.md`'s de-scaffold paragraph gained its closing line verbatim as
ruled, both halves of the moratorium's condition cited by sha. The two
`## Next` rows went in at PRIORITY 5 and 6 -- the free gap between the
existing 4 and 9, so nothing was renumbered and `head` still answers
"what is next".

**ONE FIGURE IN THE RULING CORRECTED AGAINST THE TREE**, disclosed in
the row itself rather than shipped stale. The extraction row gives
`engine.clj` as 4,705 lines. It is **4,884** at `da21a28`. The ruled
figure was exactly true at `1b4e264` -- the TS-2 close, earlier the same
day -- and grew 179 lines across the TS-3 and TS-5 fixes that followed
it. `emit_hl7.clj`'s 2,498 is exact. This is the review-5 species
caught in the act of being created: a figure true when written that a
few hours made false.

`ehrt.docs-tooling.roadmap-lint-test` green over the new rows (20 tests,
32 assertions) before the commit: tokens, unique slugs, ascending
PRIORITY, and the dual's closure-word check over both first sentences.

## 2. THE GAP LIST

The method: attempt each of the prompt's four tasks (a)-(d) using only
what a consumer can reach -- `docs/`, `ehrt --help` and `ehrt help
<group>`, `docs/formats.md`. **Anything that required opening
`notes/adr/` or a session record is, by the prompt's own definition, a
gap.** Each entry states what a consumer cannot learn, where it actually
lives, and what it discharged into.

### (a) Generate a rich stream -- ONE gap

`--format ground-truth` itself is **not** a gap. It is documented at
`docs/cli.md`'s `ehrt sim run` flag table, at `formats.md`'s own opening
orientation and its three-row "how you get one" table, and worked end to
end in `use-cases/custom-emitter-from-the-event-log.md`. The verb table
in `ehrt --help` reaches it in one hop. This half of the estate is in
good shape.

**G1. A consumer cannot learn that the interesting stream is opt-in,
and that the default is the thinnest one this simulator produces.**
Nothing in `docs/` says so. MEASURED at HEAD, same seed, same patient
count, same `--churn`, differing only in whether a `--config` was
supplied:

| | events | kinds |
| --- | --- | --- |
| `bin/ehrt sim run --seed 42 --patients 20 --churn --format ground-truth` | **74** | **7** |
| the same, plus `--config demos/scenarios/ed-tuesday/config.edn` | **399** | **18** |
| the same, plus the minimal opt-in-keys-only config the page ships | **261** | **15** |

Five times the events and more than twice the vocabulary from a
configuration file, and the third row is what separates the two things
that file changes: the scripted clinical content (`:pathways`,
`:facility`, `:modules`, `:order-profiles`) from the four opt-in keys.
The opt-in keys alone account for most of the gap. *Lives in:*
`ehrt.sim-engine.engine/config-keys`' per-key comments, ADR-0173/0174/0175,
and the arc session records. *Discharged into:* "The mix, and why the
default is thin".

### (b) Parameterize the mix -- SIX gaps, the largest cluster

**G2. The config-key catalog exists in NO shipped doc.** Verified by
census over `docs/`, `README.md` and `SETUP.md`: **twelve keys appear in
no file at all** -- `:persons`, `:encounters`, `:bed-cycle`,
`:scheduling`, `:chatter`, `:charges`, `:ladders`, `:fan-out`,
`:history`, `:persona-config`, `:module-horizon-days`,
`:module-initial-attributes`. `:siu` appears only as a parenthetical
inside four generated per-kind entries in `formats.md`. And
`docs/cli.md`'s own `--config` text is
`(:pathway/:pathways/:order-profiles/:churn-profile/:site-profile/:modules/...)`
-- **an ellipsis exactly where the catalog would be**, which is the gap
rendered as punctuation. *Lives in:* `engine/config-keys`,
`ehrt.sim.run/run-command`'s docstring, `ehrt.sim-model.config`'s five
schemas. *Discharged into:* two tables, nineteen engine keys and seven
emission keys, each with its effect on the mix.

**G3. A consumer cannot learn the engine/emission split** -- which keys
are fact generators that DRAW (so turning one on reshuffles the whole
population, and the same seed then describes a different hospital) and
which are pure functions of a log that already exists (so they cannot
move a ground-truth byte). This is the single most load-bearing fact
about the config surface for a ground-truth consumer, and it is
invisible. *Lives in:* ADR-0175, `rulings.md#R-skeleton-or-emission`,
and the docstrings. *Discharged into:* the opt-in law, rule 2, and the
split into two tables.

**G4. A consumer cannot learn the opt-in law's two traps.** ABSENT is
the byte-identical path and is not the same statement as `false`; and
`:siu {}` is ON while `:chatter {}`, `:ladders {}` and an empty
`:charges` table are off. *Lives in:* `config-keys`' comments and
`SiuProfile`'s docstring. *Discharged into:* "The opt-in law", three
numbered rules.

**G5. No shipped doc points a consumer at an authored example config.**
`demos/scenarios/ed-tuesday/config.edn` and `clinic-decade/config.edn`
are richly commented and shipped, and nothing in `docs/` routes to them
as *the* worked example. `docs/simulate-your-facility.md` covers
`:facility`, `:providers`, `:pathways` and `:modules` and stops there.
*Discharged into:* "An authored example", naming the fifteen keys
`ed-tuesday` actually carries, plus a minimal runnable config.

**G6. A consumer cannot learn the two sizing facts that are
measurements rather than preferences** -- that `:persons`' `:count`
wants to be roughly twice the arrival count (at parity the birthday
paradox makes better than a third of arrivals repeats, thinning clinical
content), and that `:years` must be long enough for the rare hooks to
fire (at ten years `ed-tuesday`'s population produced ZERO occupational
injuries; at twenty it fires). *Lives in:* `config.edn`'s header
comment. *Discharged into:* the same section.

**G7. A consumer cannot learn that a run's log extends to the PERSON
horizon, well past its clinical content** -- that with `:persons` on,
the stream is mostly demographic tail. *Discharged into:* the same
section and "Time".

### (c) Learn the vocabulary -- TWO gaps, one of them a live defect

`formats.md`'s generated section is excellent and is not a gap: 28
kinds, per-kind key tables, a real example each, the `:t`-is-seconds
statement, the EDN conventions, and the tree-walking trap called out
first.

**G8. FOUR SHIPPED SURFACES SAY THE VOCABULARY IS 21 KINDS. IT IS 28.**
`docs/README.md`, `docs/glossary.md`, `docs/dev/AUDIENCES.md`, and
`components/corpus/docs/use-cases.edn` -> the generated
`custom-emitter-from-the-event-log.md`, whose sentence is the damaging
one because it instructs a completeness check: *"There are exactly 21
event kinds and the set is closed, so an unknown `:event` value is a
contract violation rather than something to skip past."* A consumer
following that ships an emitter blind to seven kinds and treats them as
contract violations. `formats.md`'s own "Where this comes from" table
gave a THIRD number, 23, from the 2026-08-16 census. **All corrected
this session**; the provenance row now states no count of its own and
names 23 as the census figure against 28 today.

**G9. A consumer cannot learn which keys produce which kinds.**
`:bed-status-change` requires `:bed-cycle`; the four scheduling kinds
require `:scheduling`; `:demographic-update` and `:coverage-change` in
volume require `:persons`. `formats.md` describes every kind as though
every run produced it. *Discharged into:* the engine-key table's
per-key "effect on the mix" column.

### (d) Learn the guarantees -- FIVE gaps, the deepest cluster

**G10. `ehrt sim check`'s catalog is nowhere enumerated in `docs/`.**
`cli.md` says "the invariant catalog (capacity/surge-ladder,
timestamp-monotone, and friends)". There are **44**, and the verb itself
reports every one by name in `:payload :invariants-checked`, so the
information is one command away and no page says to look. *Lives in:*
`ehrt.sim-check.check/catalog` plus its three config-needing siblings.
*Discharged into:* the full list, taken from a real run's own output.

**G11. A PIPED `ehrt sim check` READS THE DEFAULT FACILITY, WARM-UP
WINDOW AND ORDER PROFILES.** It takes a log on stdin and has no flags at
all, so four of the 44 invariants -- `occupancy-within-capacity`,
`surge-only-when-earlier-rungs-exhausted`,
`warm-up-mark-matches-window`, `result-analytes-match-order-profile` --
run against the shipped defaults whatever `--config` produced the log.
MEASURED: `bin/ehrt sim check < ed-tuesday/events.edn` reports **115
`:occupancy-within-capacity` violations, every one of them spurious**,
because that scenario's `:facility` raises the Emergency ward's surge
slots from 6 to 16 and the check is comparing against 6. The run's own
in-process self-check passes the real facility and warm-up
(`check/check-all ground-truth facility warm-up-seconds`) -- but its
3-arity means **order profiles default even there**, so a corpus
overriding `:order-profiles` is checked against the defaults on both
paths. Documented nowhere. *Discharged into:* two bolded paragraphs
under "What `ehrt sim check` certifies".

**G12. A consumer cannot learn that seven of the 44 are vacuous on a
thin log** -- the three bed-cycle invariants no-op without
`:bed-status-change`, the four scheduling ones without `:appointment`
-- while all 44 are still listed as "checked". A green check on a thin
log is a weaker statement than a green check on a rich one and the
report does not distinguish them. *Discharged into:* the same section.

**G13. The determinism contract is not stated in `docs/` at all.**
Seed + config + generator version, as a WITHIN-version guarantee;
`:stream-scheme` as a discriminator rather than a warranty; what does
and does not invalidate a reproduction. *Lives in:* sim/ADR-0009 (under
`notes/sim/`, frozen provenance a consumer will never read), ADR-0171
ruling D1, and `engine/stream-scheme`'s docstring. *Discharged into:*
"Determinism", including the measured host-independence check below.

**G14. `formats.md`'s "The corpus manifest" documents the WRONG
MANIFEST for a sim corpus.** Its field table and its worked example are
the external-generator case: `:seeds {:master 100 :clinician 555}`,
`:generator {:name "synthea"}`, a `:runtime` block, and an
`:invocation` carrying a subprocess command line. A sim `manifest.edn`,
read straight off a real run this session, carries
`:event-schema-version`, `:stream-scheme`, `:stage :simulated`,
`:seeds {:primary N}`, `:invocation {:verb "run" :opts {...}}`,
`:generator {:name "ehrt.sim"}` and NO `:runtime`. None of the first
five appears in that table. *Discharged into:* "Provenance", with the
sim manifest's own field table and the four fields a bug report must
quote.

Two more the prompt named and the audit found already covered, recorded
so the next session does not re-derive them: **identity semantics** are
partly reachable (the per-kind key tables carry `:person-id`,
`:encounter-id`, `:identity :placeholder`, `:cause :identification`)
but never assembled into one table for an MPI consumer -- assembled now;
and **time semantics** are largely covered by `formats.md#ordering`
(`:t` is seconds from run start, monotone within a run, no `#inst`
anywhere), which is why "Time" on the new page is short and adds only
the two things that section does not say: the person-vs-clinical
horizon, and log order versus wire order.

## 3. What the new page says, and what backs each claim

`docs/consuming-ground-truth.md`, ~570 lines, in the manual's voice.
Sections: the invocation; the mix and the opt-in law; the two key
tables; an authored example; determinism; identity; time; what `sim
check` certifies; what is NOT warranted; scale; provenance; fault
injection as it stands today.

**Figures produced by running the tool this session, at HEAD:**

- 74 events / 7 kinds versus 399 / 18 (shipped config) versus 261 / 15
  (opt-in keys alone) -- G1's table.
- 44 invariants, from `:payload :invariants-checked` on a real run.
- 115 spurious `:occupancy-within-capacity` violations (G11).
- The sim manifest field table, read off `out/audit/ed-tuesday/manifest.edn`.
- **Host-independence, verified rather than asserted**: the same
  command under `TZ=Asia/Tokyo` and under the host's own
  `America/New_York` gives the SAME SHA-256 over `events.edn`
  (`a87d859e…`). The log carries no wall clock, and this is what
  proves it rather than restating it.

**Figures cited to the traffic-scale appendix and the TS-4 close:** the
three 10^5 cells (171,864 / 233,286 / 1.3574 / 270.37 s; 129,415 /
165,946 / 1.2823 / 232.67 s; 105,214 / 67,638 / 0.643 / 118.9 s), the
exponents (generate 1.624, persons 1.061, check 0.914), the peak-heap
phase table, and the 10^6 arithmetic (retained log projects to 1.18 GB
and fits; emit's message vector projects to 9.87 GB against a 3.88 GB
ceiling, which is why the cell was declined).

**Exclusions stated at guarantee weight**, per the prompt: the
never-closing re-opens quoted with their own numbers (55 of 55 re-open,
54 with no closer of any kind, green on every gate in the catalog,
because `every-encounter-is-opened-and-closed-or-still-open` reads "or
still open"); the tolerated consumed-placeholder clause with the two
counts that keep it honest; hazard rates authored-provisional; and both
gated limitation registers summarised by row.

### Two prompt premises corrected, disclosed rather than silently followed

1. **`no-event-references-a-merged-placeholder` does not exist.** Grep
   over the whole tree returns nothing. The companion gate the TS-4
   close actually landed is
   **`no-resolution-after-a-placeholder-is-consumed`**, and it is in the
   catalog. The page names the real one.
2. **msg/event RISES with scale; it does not fall.** The prompt asks for
   "its FALL with scale from 1.63x at 10^3". The completed `v2` series
   reads **1.050 -> 1.217 -> 1.357** across 10^3 -> 10^4 -> 10^5 and the
   TS-4 close says in as many words that it "is still climbing"; against
   the 0.643 skeleton baseline the add-on multiplier goes **1.63x at
   10^3 to 2.11x at 10^5**. The page states the measured direction.

## 4. Step 3 -- P2 absorbed, and the row's own count was wrong

`roadmap.md#post-partition-narrative-refresh` says "43 tokens over 6
files". **The row was written 2026-08-25 and four later sweeps
re-witnessed parts of what it counted, while the same sweeps created
new staleness the row could not have known about.** So the count is not
43 in either direction, and the honest report is per file, against runs
regenerated at HEAD.

### The runs

All at `2e141f2`, seed 20260811, 100 patients, `--reference-date
2026-08-11 --churn`:

- `corpus generate sim --config demos/scenarios/ed-tuesday/config.edn`
- the same with `config-latency.edn`
- `corpus batch <latency> --interval 60`
- `play --board 60 --rate 10000000` over each corpus

### Per file, before -> after

| file | row's count | tokens actually changed | what they were |
| --- | --- | --- | --- |
| `demos/scenarios/ed-tuesday/README.md` | 19 | **10** | PV1 fill `630 of 631` -> `679 of 681` and `The ONE` -> `The TWO`; board `(dirty)`/`(cleaning)` line total `43 (15, 28)` -> `44 (15, 29)`; first snapshot `4 occupied beds` -> `3`; census peak `14` -> `12` (twice); two `:wallclock-ms` values |
| `docs/manual/01-what-this-is.md` | 8 | **~16** | the whole out-of-order board snapshot and its cast (Walker, William MRN000013 -> Gonzalez, Olivia MRN000095); `8 of 92` -> `5 of 111`; the batch listing's three counts, its elision range, its `:latest-ms`, `34-batch` -> `620-batch`; the straddle's cast (Smith, James ED-H05 -> Hernandez, Sandra ED-H09) and `batch-001` -> `batch-002` |
| `docs/manual/04-time-on-the-wire.md` | 4 | **~25** | both `sha256sum` digests `d00bf49c…` -> `fe13a7ba…`; `383` -> `1,269` events; `msg-%03d.hl7` -> `msg-NNNN.hl7`; `--rate 100000` -> `10000000` twice; the two-clocks cast and all five of its timestamps; the ORU pair's patient, both filenames, both MSH-7 values and the delay |
| `docs/manual/05-batch-delivery.md` | 10 | **~18** | `283` -> `1,554`; `34` -> `620` buckets (three places); the span's end; the batch listing's counts and tail; `615` -> `620` (three places); `BTS|9` -> `BTS|10` twice; `Nine of nine` -> `Ten of ten`; "two messages further in" |
| `docs/manual/00-front.md` | 1 | **2** | the straddle's cast, and "two adjacent" -> the three-file shape |
| `docs/use-cases/supply-batch-straddling-traffic.md` | 1 | **1** | `283 messages` -> `1,554`, edited in `components/corpus/docs/use-cases.edn` and regenerated |

**Total: 43 rowed, ~72 changed.** "Token" is not a defined unit here and
the two counts are not strictly comparable -- the row counted what a
2026-08-25 reader saw, and four sweeps have run since. What IS
comparable is the direction: the row UNDERSTATED the work, and it
understated it because narrative staleness compounds.

### Three findings from the refresh

1. **THE DEMO README WAS ALMOST ENTIRELY FRESH; THE MANUAL WAS NOT.**
   `ed-tuesday`'s own README reproduced exactly at HEAD in every
   structural figure checked -- 1,269 events, 1,554 messages, 147
   encounter openers across 111 patients with 30 holding more than one
   and a maximum of 3, 141 bed turnovers, 35 openers naming an
   appointment, all four SIU families, the 620-batch listing to the
   count, both quoted board snapshots byte-for-byte, the
   ground-truth-invariance digest, the `head -c 100`/`tail -c 45`
   wrapper bytes, and both Hernandez MSH segments with their EVN-2
   times. The manual chapters that quote it were, in places, four
   sweeps behind. **The asymmetry is structural**:
   `bin/demo-exerciser-ed-tuesday` re-runs the demo's commands and
   asserts against them; nothing gates a manual excerpt.
2. **A FILE CONTRADICTED ITSELF IN BOTH DIRECTIONS.** Chapter 5 opened
   on "283 messages across 34 occupied hourly buckets" and closed on
   "`:verified true` on all 615" and "615-for-615 self-verification" --
   two figures that cannot both describe one run, and neither of which
   described any run at this commit. The demo README had the same shape
   at smaller scale: its PV1 paragraph said "630 of this run's 631"
   while a dated note two paragraphs below it already said 681. **The
   failure mode is a partial re-witness**: a sweep updates the
   paragraph it is about and leaves the arithmetic around it.
3. **THE SECOND BLANK PV1-19 HAS A CAUSE WORTH NAMING.** The README
   said one message lacked a visit number; there are two, and the second
   is the status ladder's: a rung restating a pending lab is rendered by
   the same builder as the result it restates, so it inherits the same
   absent encounter. Both are `ORU^R01`, both are the
   pending-labs-at-discharge shape.

## 5. Gates run

- `ehrt.docs-tooling.roadmap-lint-test` -- 20 tests, 32 assertions, 0
  failures. Run twice: after the two new rows landed (commit 1) and
  again after P2's row moved to `## Done` (commit 4).
- `ehrt.docs-tooling.hand-owned-asset-freshness-test` -- 5 tests, 28
  assertions, 0 failures, after the `straddle-timeline.svg` bump.
- `make docsgen` -- exit 0. Run once mid-session and once at the close.
  Beyond this session's own edits it moved exactly one file,
  `.agents/state-derived.md`.
- `make test` -- run TWICE, and **the first run was RED**, which is the
  most useful thing any gate did for this session.

**THE RED, AND WHY IT WAS RIGHT.** `ehrt.docs-tooling.invocation-lint-test`'s
`fence-path-arguments-resolve-from-workspace-root-test` failed on the
new page with `{:flag "--config", :value "rich.edn"}`, twice. The page's
opt-in demonstration named a config file that exists nowhere in the
tree: it was written to scratch, measured from scratch, and quoted from
scratch, so a reader pasting the fence would have got
`:config-not-found` on the page's own headline example. **`make test`
276 passes, 1 failure, `MAKE_EXIT=2`.**

Fixed at the source rather than exempted -- `illustrative-path-exemptions`
exists and would have silenced it, and taking that route would have
shipped a headline example nobody can run. Both fences now name
`demos/scenarios/ed-tuesday/config.edn`, a real shipped file, and the
comparison was **re-measured against it**: 399 events across 18 kinds
against the bare run's 74 across 7. The minimal opt-in-keys-only config
is still shown, as an EDN block rather than a command, with its own
measured 261/15 -- which turns out to be a better page anyway, because
the three-row comparison separates what the scenario's `:pathways` and
`:modules` contribute from what the four opt-in keys do. The whole
`sim run | sim check` pipeline was then run verbatim as printed, exit 0.

**SECOND RUN GREEN: 4,751 tests, 24,109 assertions, 0 failures, 0
errors, `MAKE_EXIT=0`.** Two prose-only edits to the new page landed
after that run started (the fault-injection lead-in and the manifest's
`:environment` row), so the four gates that scan `docs/*.md` were re-run
over the final tree -- `invocation-lint-test`, `stale-path-test`,
`roadmap-lint-test`, `hand-owned-asset-freshness-test`: 42 tests, 549
assertions, 0 failures. Disclosed rather than left implicit; a third
full run for two sentences of prose would have bought nothing those four
gates do not already cover.

## 6. Judgment calls

1. **The moratorium's "so half of that condition is now met" clause was
   left standing.** It sits two sentences before the new LIFTED line and
   is now historically true rather than currently true. The prompt ruled
   an ADDITION to that paragraph and nothing else; the added line
   supersedes the clause unambiguously, and rewriting ruled prose to
   tidy it is not this session's call.
2. **The extraction row's line count was corrected rather than shipped
   verbatim.** "Rows verbatim as ruled" and "no claim without a tree
   citation" collide on `engine.clj (4,705 lines)`. Fix-forward with
   disclosure, per
   `rulings.md#R-stop-only-on-two-defensible-readings`: only one reading
   is defensible once the tree says 4,884, and the row carries the
   correction, its own measurement and the sha at which 4,705 was true.
3. **`components/corpus/docs/use-cases.edn` was edited under a docs-only
   fence.** It is the docsgen SOURCE for `docs/use-cases/*.md`, and one
   of the six files the prompt named for refresh is generated from it.
   Refusing to touch it would have made step 3 impossible to complete;
   it carries no runtime behaviour.
4. **The "21 event kinds" correction was made rather than merely
   recorded.** The de-scaffold ruling says a payload session's finding
   is one line in its record. This one is a wrong instruction on the
   exact surface this session's own audience reads for this session's
   own subject, and the correction is four tokens. Recorded AND fixed.
5. **`:wallclock-ms` was re-witnessed along with everything else in its
   transcript block.** It is the one field in those summaries that is a
   property of the machine rather than of the corpus. Both old and new
   values are real; re-witnessing keeps each block one coherent
   execution rather than a splice, which is the convention the
   surrounding notes already follow.
6. **The prompt's two wrong premises were fixed forward, not stopped
   on** (section 3). Neither has two defensible readings: a gate that
   exists nowhere in the tree cannot be cited, and a series the close
   describes as "still climbing" is not falling.

## 7. Scratch

`out/audit/` holds everything this session measured and is not
committed: `ed-tuesday/` and `ed-tuesday-latency/` (the two regenerated
corpora), `batches/` (620 files), `board-base.txt` and `board-lat.txt`
(579 and 620 snapshots), `check.edn` (the 115 spurious violations),
`rich.edn` / `rich-events.edn` / `plain-events.edn` (G1's two runs),
`rich-tokyo.edn` (the TZ check), and `docsgen.log`.

## 8. Where the next session picks up

- **`roadmap.md#engine-namespace-extraction-and-apply-unification`
  (PRIORITY 5) is what is next.** It is a payload arc, the moratorium is
  lifted, and its own row names the bracket discipline it owes.
- **`roadmap.md#event-stream-mutation` (PRIORITY 6) sits behind it** and
  owes a design ADR first.
- **Two things this session found and did NOT fix**, each one line here
  rather than a row of its own, per the de-scaffold ruling:
  1. `ehrt sim check` has no way to be told the facility, warm-up
     window or order profiles that produced the log it is reading, so it
     reports spurious capacity violations over any corpus with a
     non-default facility (115 of them over `ed-tuesday`'s). The run's
     own self-check covers the first two and defaults the third. A
     `--config` flag on `sim check`, or a manifest read, would close it;
     both are payload changes and neither is this session's.
  2. `docs/manual/assets/two-clocks.svg` is `:verdict :stale` in
     `hand-owned-assets.edn` and this session moved the chapter its
     depicted case lives in -- the cast went from Walker to Gonzalez,
     and the EVN-2/MSH-7 gap the drawing shows went from about an hour
     to an hour and a quarter. The row's own source is
     `docs/dev/simulator-architecture.md`, not the chapter, so the
     tripwire did not fire and the row's verdict is unchanged; the
     drawing is one more sweep out of date than it was.

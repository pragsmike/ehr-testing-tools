# 2026-09-08 -- docs currency: what the downstream can now do

Prompt:
[`../prompts/2026-09-08-docs-currency-downstream.md`](../prompts/2026-09-08-docs-currency-downstream.md).
Base `911c9101`, tip `04e4d3f4` before this record.

Ceremony mode: R30, unattended commit-and-push at each checkpoint --
the prompt named no prepare-only fence.

Rulings in force: **R-figures-file** (2026-09-07), **R-move-not-improve**
and **R-sentinel** (this prompt), **R-empty-population-is-red**,
**R-stop-only-on-two-defensible-readings**, **R-red-pushed-with-green**,
**R-full-suite-before-push**, **R-register-hygiene-at-close**.

## What moved

Three claims in the consumer docs predated ADR-0179 and ADR-0180 and had
come to say the opposite of what the tree measures. Four files, prose
only, plus one claim added to an existing gate. No engine run, no
bracket, no exerciser, no new measurement: the 22,500-arrival cell is
named as *run* and deliberately not quoted, because it has no committed
cell yet.

**1. `docs/future-features.md` -- the inventory's own motivating zero
was the instrument, not the corpus** (`b0632069`). The scenario-inventory
section argued that a result landing after its own subject was merged
away "is not reachable by generating more", on a 533,147-event run
reading **zero** in that column. `measurements.md`'s 2026-09-06
correction retracted the predicate: it asked for a `:result-available`
whose subject's status was `:merged`, which ADR-0179's R-queue makes
unsatisfiable rather than rare, because a pending result follows the
SURVIVOR. Re-derived over the same eight logs the column reads **43** at
533,147 events and **21** at the 7,500-arrival cell -- and that cell is
`demos/scenarios/dense-7500/config.edn` byte for byte, so it is a figure
a reader can reproduce.

The paragraph's ARGUMENT survives the flip intact and is in fact
strengthened: 2,850 merges yielding 43 post-merge results is precisely a
column no count of merges and no count of results can stand in for. The
correction section is linked by relative path. No ADR token appears in
this file (`link-footnote-gate-test` claim 3); the file carries no
footnote definitions, so the citation is the path.

**2. The 10^6 claim, three documents** (`fdad6a72`). It appeared as a
settled negative -- "10^6 is not [comfortable], and the reason is the
emitter" -- resting on a projection made before the index-and-fold
programme moved both the walls and the peak heap it was projected from.

- `docs/consuming-ground-truth.md`'s Scale headline now carries no
  figure of its own: 10^5 is comfortable, the decade above HAS been
  entered (this configuration run to 22,500 arrivals under that
  programme), that cell is not quoted until it is a committed one, and
  10^6 events have not been run at all. The distinction the edit buys is
  *unwitnessed* versus *declined*.
- The "tracked at `roadmap.md#performance-residual-sites`" pointer now
  says worked and closed. The row closed 2026-09-07 at `e6cd1dbb`.
- The third flagged paragraph gains ONE sentence saying its conclusion
  is superseded in DIRECTION by that programme while its figures stay
  un-re-measured. The paragraph's own opening assertion is left standing
  deliberately: the block above it already frames all three as kept only
  for the direction they report, and R-move-not-improve fences the rest.
- `docs/future-features.md`'s streaming-output section drops "reaches
  the shipped 3.88 GB ceiling on its own at ~533,000 events" and keeps
  the stance -- the log is still held whole and binds on its own further
  up. Its Scale pointer was already in the same sentence.
- `demos/scenarios/dense-7500/README.md` replaces "it is the only path
  that reaches that decade" with what is warranted: the cheaper path on
  heap, by a widening margin, because the emitter's peak binds first --
  and whether it is the ONLY path to 10^6 is not something this
  repository has run.

**3. `README.md` re-quotes the figures file, and joins its gate**
(`04e4d3f4`). The front door published **1.3574** and **0.643** -- the
2026-08-29 programme's cells, which the Scale table itself flags as not
re-measured. They are now **1.3327** and **0.6489**, from
`figures.edn`'s `:config-edn-7500` and `:config-bare-7500`.

## The gate, claim (g)

The front door is a THIRD quoting document, and it is prose rather than
a table row, so claim (b)'s population closure could never reach it.
That is exactly how it stayed stale through two re-measures while the
two tables were caught. The new deftest locates the paragraph by its
opening clause, matches its bolded figures by a regex over the whole
bold run (the first wraps a line: `**1.3327 messages per\nevent**`), and
asserts the count at **exactly two** -- so a third figure added there
fails rather than going unchecked, and a reworded clause fails by name
rather than looping over nothing.

Red first, against the stale README, real output:

```
FAIL in (the-front-door-quotes-the-figures-file-test) (dense_7500_figures_test.clj:286)
all nine opt-in keys
README.md quotes 1.3574 for the all-keys cell but demos/scenarios/dense-7500/figures.edn holds 1.3327 -- re-measure through the figures file, never a document
expected: (== (:msg-per-event (:config-edn-7500 cells)) (first found))
  actual: (not (== 1.3327 1.3574))

FAIL in (the-front-door-quotes-the-figures-file-test) (dense_7500_figures_test.clj:292)
no opt-in key at all
README.md quotes 0.643 for the bare cell but demos/scenarios/dense-7500/figures.edn holds 0.6489 -- see above
expected: (== (:msg-per-event (:config-bare-7500 cells)) (second found))
  actual: (not (== 0.6489 0.643))

Test results: 54 passes, 2 failures, 0 errors.
```

Worth naming: the "exactly two figures" assertion PASSED in that same
red run. The locator was proven to find the paragraph before either
figure convicted, so the two failures are the figures and not a
mis-located paragraph. Green after the README edit: 56 passes.

## Disclosures

**(a) The prompt names the wrong half of `figures.edn`, and the gate
does not follow it.** Step 3 says to hold the README "equal to
`:quoted`'s `config-edn-7500` and `config-bare-7500` msg-per-event".
`config-bare-7500`'s does live in `:quoted`; `config-edn-7500`'s lives in
`:asserted`, which is the half `bin/demo-exerciser-dense-7500` rewrites
on every run. Taking the instruction literally would have read `nil`.
This is a mechanical conflict with exactly one defensible reading, so
fix-forward with disclosure rather than STOP-AND-REPORT
(`rulings.md#R-stop-only-on-two-defensible-readings`): the gate reads
`merged-cells`, this namespace's own accessor and the one claims (c) and
(d) already use, which takes each figure from whichever half declares it
and reports a field declared in both as a defect.

**(b) R-figures-file, as compressed in the prompt, would forbid step 1's
own figures.** The prompt restates it as "no new wall or count enters
prose from any other source", and step 1 instructs 21, 43 and 533,147
into `future-features.md` from `measurements.md`. Read against the
ruling as it was actually made (2026-09-07 prompt, verbatim: "the
deterministic figures -- events, messages, msg/event per cell -- live in
ONE committed file the exerciser writes"), there is no conflict: the
ruling is scoped to the dense-7500 scenario's own per-cell figures, and
a census column is not one of them. The compression, not the ruling, is
what over-reaches. Recorded so a later reader does not take the
compressed form as the rule.

**(c) The prompt overrides AGENTS.md's docs-only push allowance,
upward.** The de-scaffold ruling says a docs-only diff pushes without a
local `make test`; this prompt requires `make test` green before each of
the three commits. The prompt is the stricter instruction and was
followed -- three full runs, ~20 minutes each.

**(d) One `poly test brick:` run is in this record and is NOT what
preceded any push.** The red/green pair for claim (g) ran
`brick:docs-tooling` for fast feedback. `R-full-suite-before-push` was
satisfied separately: `make test` ran in full before each of the three
commits.

## Figures

| run | tests | assertions | exit |
|---|---|---|---|
| step 1 (`b0632069`) | 4,941 | 28,525 | 0 |
| step 2 (`fdad6a72`) | 4,941 | 28,525 | 0 |
| step 3 (`04e4d3f4`) | 4,943 | 28,531 | 0 |

Step 3's `+2 tests / +6 assertions` for ONE new deftest with three
assertions is the namespace running in two projects, not a
double-count of anything. No new test FILE was created, so no lint
assertion and no state-derived regeneration was owed on that account.

`bin/preflight` at session start: exit 0, no findings -- last five CI
runs green, edit root not under `/mnt/`, tree clean, HEAD matching
`origin/main`. The one DISCLOSED line is the standing "HEAD is not
currently tagged `stable-*`", which the retired tag law makes expected.

No roadmap row changed: `performance-residual-sites` was already CLOSED
before this session, and this session cited it rather than moving it.

Background processes started and terminated: three `make test` runs,
each run to completion and reaped by the harness; no waiter was left
running.

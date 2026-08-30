# Root README front example: the front door leads with ed-tuesday

Session record, 2026-08-29. HEAD at start `079072e`; ceremony R30
(commit and push at each checkpoint, unattended), taken from the
prompt. Docs-only by fence -- no `src` logic, no `test`, no runtime
config, no seed change, no new scenario.

## 0. What moved

Two README sections, and the three sourced surfaces that follow them.

1. **"See it run"** (was `README.md:24-43`) now leads with `ed-tuesday`
   -- the generate command, `play --board 60 --rate 3600`, and five
   sentences of what that board shows. Clinic-decade is DEMOTED, not
   deleted: a one-paragraph "And the longitudinal version" carrying its
   own link, and the old self-apology ("the board mostly idle-skips",
   "only one inpatient is ever admitted") is gone rather than reworded.
2. **The ground-truth pointer** (was `README.md:181`, the
   `--patients 5` one-liner) is now the ed-tuesday invocation with
   `--format ground-truth`, a seven-event elided excerpt, and
   `docs/consuming-ground-truth.md` linked as the contract in the same
   breath -- plus one measured sentence quoting that page's own Scale
   table.
3. `fence-exemptions.edn`'s README row RE-ANCHORED;
   `bases/cli/src/ehrt/cli/help.clj`'s `play` example moved with the
   fence it is sourced from; two comments that labelled the fence
   "(clinic-decade)" corrected.

## 1. The run that sourced each excerpt

ONE run, regenerated at `079072e` before any edit, sourced everything:

```
bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config.edn \
  --out-dir out/scenarios/ed-tuesday
```

- **The board prose** -- `bin/ehrt play out/scenarios/ed-tuesday
  --board 60 --rate 10000000`, counted off its own captured output:
  579 snapshots, 15 `(dirty)` and 29 `(cleaning)` lines, wards
  Emergency (578 snapshots) / Renal (37) / Cardiology (16), peak
  `inpatients: 12`, final `discharged: 88  merged: 1`, last snapshot
  `2046-08-01T15:15:55Z`. Play's own closing summary:
  `:snapshot-count 579, :emitted 1554`.
- **The event excerpt** -- `bin/ehrt sim run --seed 20260811
  --patients 100 --reference-date 2026-08-11 --churn --config
  demos/scenarios/ed-tuesday/config.edn --format ground-truth`. Its
  bytes are IDENTICAL to `out/scenarios/ed-tuesday/events.edn` except
  for one trailing newline -- verified, not assumed:
  `head -c -1` of the `sim run` output and the corpus's `events.edn`
  both hash to `fe13a7ba...b8f8`. The README says so, so a reader who
  ran the generate command above does not have to run the second one.
- **The seven events** are indices 1, 7, 8, 9, 11, 698 and 825 of that
  1,269-event vector, read with `clojure.edn/read-string` -- never
  grepped (`rulings.md`: parse EDN, never grep it). All seven belong to
  ONE patient, `PID-000000-1522c269` / `MRN000001`: the admission into
  `ED-H08`, the discharge's bed transition `:occupied -> :dirty`, the
  `:appointment` booked at that same instant with `:scheduled-t
  1384620`, the bed's `:dirty -> :cleaning -> :ready` legs, the
  `:outpatient-visit` at `:t 1384620` naming the same
  `APT-000000-00-b82f275e`, and a `:coverage-change` on the same person
  at `:t 101535041`. Keys are elided with `...`; key ORDER inside each
  map is the stream's own, so a reader diffing against a fresh run sees
  a subsequence, not a rearrangement. The gap markers are counted, not
  estimated (5, 1, 686 and 126 events skipped).
- **The measured sentence** quotes `docs/consuming-ground-truth.md`'s
  own Scale table -- 1.3574 msg/event at 10^5 with all nine opt-in
  keys, 0.643 with none -- and carries that page's own caveat forward
  ("an order of magnitude rather than a benchmark"). Nothing was
  re-measured here.

## 2. CHECKED, not asserted: there is no John Doe on this board

The prompt asked for a John Doe "if one appears at the demo seed --
CHECK, do not assert". **It does not appear.** `grep -c 'Doe, Unknown'`
over the full 579-snapshot capture is **0**, at this seed. The scenario
README's own witness is that the run holds 15 unidentified ED arrivals;
none of them renders as an occupied-bed line.

What DOES reach the board from that family is the counter line's
`merged`, which ticks 0 -> 1, and
`demos/scenarios/ed-tuesday/README.md` is where that merge is
identified as an identification merge -- a John Doe record joined to
the patient the same person already had. The README prose says exactly
that and promises no bed.

## 3. The exerciser seam: it does NOT exist, and that is the honest answer

The prompt asked whether the new excerpts join
`bin/demo-exerciser-ed-tuesday`'s witness set. **They do not**, and the
reason is structural rather than an omission:

- That exerciser's freshness check
  (`ehrt.docs-tooling.demo-exerciser-fresh`) reads
  `demos/scenarios/ed-tuesday/README.md` only -- the root `README.md`
  is not its source.
- `exercised-sources.edn` carries exactly two `README.md` rows, and
  both point elsewhere: `bin/quickstart-demo` (the Quickstart fence)
  and `bin/readme-what-you-get` (the "What you get" pairs). Neither
  extraction reaches "See it run": `:paired` needs a command fence
  immediately followed by an output fence, and this one is followed by
  prose.
- So the new front-door fence is `exempt`, not `exercised` --
  `bin/fence-census` confirms it, and the front door still measures
  **zero bare command fences**.

Closing that seam would mean a new `bin/` script and a new register
row, which is not a docs-only change; it is left unopened and named
here rather than papered over. What the exerciser DOES prove is that
the same two commands run and the board renders the states the new
prose describes -- it just proves it from the scenario's own README.

## 4. Registry and sourced-surface consequences, all predicted

- **`fence-exemptions.edn` -- RE-ANCHORED, not duplicated.** The row
  excused a fence whose first command was
  `bin/ehrt corpus generate sim --seed 5 --patients 200 \`. Demoting
  clinic-decade to prose deletes that fence, and a row that outlives
  the fence it excuses is what the gate's own claim (c) forbids. The
  row now anchors the ed-tuesday fence, with a MEASURED reason: 100
  wallclock seconds of `--board 60 --rate 3600` reached snapshot 56 of
  the run's 579, so a checker still cannot run it as taught.
  `:covered-by` moves to `bin/demo-exerciser-ed-tuesday`.
- **`help.clj`'s `play` example** is witnessed verbatim from that same
  fence under ADR-0118's B2 sourcing rule, so it follows:
  `bin/ehrt play out/scenarios/ed-tuesday --board 60 --rate 3600`.
  `docs/cli.md` renders no `Example` block, so this moves no generated
  doc -- confirmed by `make docsgen` leaving `docs/cli.md` untouched.
- **Two stale comment labels** corrected -- `strip_fresh.clj`'s
  docstring and `bin/readme-what-you-get`'s header both called the
  "See it run" fence "(clinic-decade)". Neither is gated; both would
  have been wrong the moment the lead changed.
- **The hand-owned-asset tripwire does NOT fire, and that was
  PREDICTED.** `hand-owned-assets.edn`'s four sources were read BEFORE
  any file was edited: `docs/dev/simulator-architecture.md`,
  `components/corpus/docs/pipeline.edn`,
  `demos/scenarios/ed-tuesday/README.md` and
  `components/corpus/docs/palgebra-design.md`. This session touches
  none of them, so no `:reviewed-at` bump is owed and no red-first
  commit was needed. The root `README.md` is not a tripwire source.

## 5. Gates

- **`bin/preflight`**, before any git: last five CI runs on `main` all
  green; edit root not under `/mnt/`; `core.fileMode` true;
  `core.ignorecase` unset; local HEAD matched `origin/main` at
  `079072e`; HEAD not tagged (disclosed, and no tag is paid --
  `rulings.md#R-tag-law` is RETIRED). One FINDING: the working tree was
  not clean, which was this session's own in-flight edits. Exit 1 for
  that finding alone.
- **`bin/fence-census`** -- front door still measures **zero bare
  command fences**; README's lead fence reads `exempt`, the other three
  README fences and all of SETUP's unchanged. Population closed at 246
  blocks.
- **`make docsgen`** -- exit 0, and it moved **nothing**. `docs/cli.md`
  renders no `Example` block, so `help.clj`'s sourced example change is
  invisible downstream; every other generated surface is untouched by a
  README edit.
- **`bin/demo-exerciser-ed-tuesday`** -- exit **0**, run on a clean
  tree at `b55007a`. Every taught command asserted and every named
  invariant held, including its own independent re-witness of the two
  figures this session put in the root README: *"bed board: 15 dirty
  and 29 cleaning bed lines rendered across the snapshots"*. Also
  green: 620 `:verified true` batches, the second-clock digest
  identity, the MRN000002 straddle, both fan-out spools with fresh
  digests, and 273 of 273 MLLP messages acknowledged. Its closing
  postcondition -- tree clean after a full run -- held.
- **`make test`** -- `MAKE_EXIT=0`, captured explicitly into a full log
  (never a pipe). `clojure -M:poly check` OK; the two projects ran
  **4,751 tests containing 24,109 assertions, 0 failures, 0 errors**;
  `bin/verify-nist-lock` matched all six hit-nexus coordinates.
  Execution time 20 minutes 29 seconds (wall, on a host that was not
  quiet -- this is a gate result, not a timing measurement, and should
  not be compared against `reference_make_test_runtime`'s figures).

## 6. Fences honoured

- **Docs-only.** No file under any `components/*/src` logic or
  `components/*/test` changed behaviour. Two files under `src` WERE
  touched and both are doc surfaces: `bases/cli/src/ehrt/cli/help.clj`
  (emitted CLI help text, sourced from the fence that moved) and
  `components/docs-tooling/src/ehrt/docs_tooling/strip_fresh.clj` (one
  word inside a docstring). Disclosed here rather than treated as
  obviously in-scope.
- **No config, no seed change, no new scenario.** `ed-tuesday`'s
  `config.edn` is untouched; the seed the README now teaches,
  20260811, is the one that scenario already ships.
- **No number without a shipped-surface citation.** Every figure is
  either counted off a run reproduced by a command printed on the page,
  or quoted from `docs/consuming-ground-truth.md` with a link.
- **No excerpt the demo seed did not produce.** All seven event maps
  are slices of `out/scenarios/ed-tuesday/events.edn` at seed 20260811,
  keys elided but never reordered.
- **The old example is demoted, not deleted.** Clinic-decade keeps a
  paragraph and its link; what was deleted is the apology, not the
  scenario.
- **README carries no internal provenance codes.** The register-code
  tripwire (`ehrt.docs-tooling.stale-path-test`) forbids `ADR-\d+`,
  `EXP-`, `DOC-\d+` and bare `D\d+` in the storefront's prose; the new
  text cites by link and by scenario name only.

## 7. A disclosure about the order of the last two steps

The suite figure below was measured over a tree whose only subsequent
change is prose INSIDE this record file. `INDEX.md`'s own listing --
the only thing any gate reads about this file -- was regenerated and
frozen before the run, and lists this filename either way. Recorded
rather than left for a reader to notice from two adjacent commits.

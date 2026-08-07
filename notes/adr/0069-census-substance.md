## ADR-0069 — Census substance: the vendoring arc opens with an honest catalog

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: the player arc closed (`notes/adr/0068-player-arc-close.md`,
tip `b7ed686`) with a horizon note recording the sequencing for what
comes next — module vendoring, ratified real by the author
(`notes/adr/0066-player-fold.md` AR-BB1-R) and scheduled after the
player arc. That note's own recommendation, restated here per this
session's own driving prompt: the census substance qualifier FIRST
(curation over 84 walking modules is impossible without distinguishing
walks-but-produces-nothing from walks-with-real-content), then a
design-channel curation pass over the ranked catalog, then vendoring
sessions batched by closure family.

The substance DATA already existed in every census artifact —
`walk-one` (`ehrt.sim-trajectory.census`) has recorded `:event-count`
per seed in every walk row since ADR-0034. This session adds only the
qualifier derived from that existing data, the tallies, the filename
fix (roadmap "Census tool refinements" item (c)), and the fresh
labeled census run that becomes the curation pass's own input —
roadmap item (b) (per-module census-seed override) is untouched, its
own trigger unfired.

Read-first: `components/sim-trajectory/src/ehrt/sim_trajectory/census.clj`
(`walk-one`, `census-one`, `summarize`, `-main`);
`components/sim-trajectory/test/ehrt/sim_trajectory/census_test.clj`
(the 7 pre-existing tests); `.agents/plans/roadmap.md`'s own Deferred
"Census tool refinements" row; `gmf-interpreter-findings.md` section 15
(AR-8b's substance note, the pre-Wave-F "26 of 42" figure this session's
fresh run supersedes); the `2026-08-04-synthea-7e08387-wave-i2.edn`
parity artifact this run's verdicts are compared against.

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-VC-0 `[A — tag law ADR-0057 AR-T-1 case (ii); debt recorded in
ADR-0068 itself]`.** Annotated `stable-20260807-player-close` at
`b7ed686`, message "player arc closed, design-channel-verified
2026-08-07 (ADR-0068)"; pushed; peeled ref verified — resolves exactly
to `b7ed686`.

**AR-VC-1 `[A for the arc opening (the ratification, ADR-0066 AR-BB1-R,
and the author's "Start vendoring," design channel 2026-08-07); C for
the sequencing]`.** This ADR records the vendoring arc OPEN, first
session this one, sequencing per ADR-0068's own horizon note. The
arc's brief is the recorded chain (ADR-0064 intake → ADR-0066
ratification → ADR-0068 horizon) — cited, not restated.

**AR-VC-2 `[C — executes the Deferred row's own item (a), its revisit
trigger fired by this arc]` (the substance qualifier).** `census-one`'s
`:ok-walked` rows gain an ADDITIVE `:substance` key derived from the
walks' own already-recorded `:event-count`s: `:zero-on-every-seed` iff
every walk's count is 0, else `:produces-content`; alongside it,
`:event-counts` (the per-seed vector, surfaced at row level). The
verdict enum itself does NOT change. `summarize` gains
`:ok-walked-by-substance` (a tally). Red-first: two new fixture
modules (a zero-content Initial→Terminal walk, an unconditional
ambulatory Encounter/EncounterEnd pair), a non-`:ok-walked` negative
case, and a `summarize` tally check; the existing 7 tests pass
unmodified.

**AR-VC-3 `[C — executes the Deferred row's own item (c)]` (the
filename learns to disambiguate).** `-main` accepts an optional third
argument, a label, giving `<census-date>-synthea-<pin7>-<label>.edn`
(unchanged shape when absent). The derivation is extracted to a pure
fn, `artifact-filename`, red-first tested on both shapes.

**AR-VC-4 `[C — the run that feeds curation]` (the fresh census).**
Re-run at the pin against the author's own Synthea checkout
(`/home/mg/synthea-checkout`, confirmed `git rev-parse HEAD` at the pin
before the run), labeled `substance`:
`components/sim-trajectory/docs/census/2026-08-07-synthea-7e08387-substance.edn`.
Dated section appended to `gmf-interpreter-findings.md` section 15 per
the AR-5 convention — the substance tally and the full list of
`:ok-walked` modules with `:substance :zero-on-every-seed`. Every
module's verdict AND every `:ok-walked` module's per-seed digest were
compared directly against the Wave I2 parity artifact — zero diffs, no
STOP-AND-ESCALATE.

**AR-VC-5 `[C — AR-A-5 discipline, fix-forward form]` (the Deferred
row).** The roadmap's "Census tool refinements" row rewrites with a
dated note: items (a) and (c) CLOSED this session (their own text
relocated here, below, not deleted); item (b) STANDS alone, untouched,
its original trigger unfired.

**AR-VC-6 `[C — scope]` (fences + oracle).** Src edits ONLY in
`census.clj` and `census_test.clj`. No gmf loader or interpreter
change — a census observes the tree as it stands (ADR-0034's own
founding fence); any loader/interpreter gap the fresh run surfaces is a
finding for the curation pass, recorded never fixed (none surfaced
this run — zero verdict movement). No module vendoring this session.
The oracle bracket (below) shows all eleven vendored-root batches
IDENTICAL — the census reads upstream modules and writes a docs
artifact, emitted corpora are untouched.

### Relocated: Deferred row items (a) and (c), closed

Verbatim from `.agents/plans/roadmap.md`'s own "Census tool
refinements" row, the text this session's rewrite removes from the
live row (relocation, not deletion, per AR-VC-5):

> (a) no substance qualifier on a `:ok-walked` verdict — a module that
> produces zero trajectory events on every seed censuses identically to
> one with rich content (`docs/gmf-interpreter.md` §15's own AR-8b
> substance note: 26 of 42 pre-Wave-F `:ok-walked` modules produce zero
> events on every seed) ... (c) the artifact filename has no
> same-calendar-day disambiguation (worked around by hand-appending a
> wave suffix in both the F0 and F re-runs, not fixed in the tool
> itself).

Both CLOSED this session: (a) by `census-one`'s new `:substance`/
`:event-counts` fields and `summarize`'s `:ok-walked-by-substance`
tally; (c) by `artifact-filename`'s optional label.

### Execution record

**Step 0 (preflight + tag).** Cwd confirmed the ext4 clone, tip
`b7ed686`. Baseline: `clojure -M:poly check` OK; full suite green (227
`Test results:` lines, 0 failures/0 errors, `clojure -M:poly test :all
skip:integration`); `gitleaks detect -v` clean (702 commits); oracle
pre-digest (`bin/regression-oracle b7ed686 b7ed686`) all eleven roots
IDENTICAL. AR-VC-0 executed directly.

**Step 1 (`b52afdb`, red).** Six new `census_test.clj` cases (AR-VC-2/
AR-VC-3) landed alone, before any `census.clj` edit. Captured red:
`clojure -M:poly test :all skip:integration` failed to even compile
`ehrt.sim-trajectory.census-test` —
`java.lang.RuntimeException: No such var: census/artifact-filename` at
`census_test.clj:303` — proof the new assertions reference API this
session had not yet built. Pushed; post-push verification: one delta
against the message file, the known trailing-newline artifact.

**Step 2 (`7cb92c6`, green + the fresh run).** `census.clj` gained
`:substance`/`:event-counts` (AR-VC-2) and `artifact-filename`
(AR-VC-3). `clojure -M:poly check` OK. Full suite green (227 `Test
results:` lines, 0/0; `ehrt.sim-trajectory.census-test` itself: 13
tests, 39 assertions, 0/0 — up from 7 tests pre-session, all 7
originals untouched). Fresh census run (AR-VC-4) against
`/home/mg/synthea-checkout` at the pin, labeled `substance`: 84
`:ok-walked`, 1 `:out-of-scope-by-ruling` (`gallstones`), 0
`:load-failed`, 0 `:walk-failed` — identical to Wave I2's own parity
counts. Substance tally: 51 `:zero-on-every-seed`, 33
`:produces-content`. `gmf-interpreter-findings.md` section 15 gained
the dated subsection (the full 51-module list, the
`total-joint-replacement` finding — a vendored root that is itself
zero-content under this census's own fixed parameters, disclosed not
fixed); `gmf-interpreter.md` section 9's index gained a row. Pushed;
post-push verification: one delta, the same trailing-newline artifact.

**Step 3 (this record).** `notes/adr/0069-census-substance.md`
authored directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own file count corrected (66→67, verified by
`ls notes/adr | wc -l`); `.agents/plans/roadmap.md`'s "Census tool
refinements" row rewritten (items (a)/(c) closed with a dated note and
a citation to this ADR's own relocated text, above; item (b) untouched)
and its "Now" section updated to name this session's own close; Done
pointer (`- 2026-08-07 — census-substance — ADR-0069`) added in the
same commit as the index line; closing oracle bracket run
(`b7ed686` → this session's own closing commit); this prompt archived
and this session recorded.

This close's own successor tag debt: `stable-20260807-census-substance`
at this session's own closing tip is owed to the next session's own
Step 0, per tag law (ADR-0057 AR-T-1) — not created here (no ruling
licensed it at this session's own commit; the AR-VC-0 tag this session
DID create anchors the PRIOR arc's close, `b7ed686`, per the standing
predecessor-tag ceremony).

### Verification

- `bin/regression-oracle b7ed686 <this session's own closing commit>`:
  all eleven vendored-root batches IDENTICAL — expected, since no
  gmf/loader/interpreter/engine/emitter file changed this session
  (AR-VC-6).
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  the Step 0 baseline (227 namespaces, 0/0) and again after Step 2's
  edits (227 namespaces, 0/0, `census-test` itself 13/39 0/0).
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks detect -v` (full history): clean at Step 0 baseline (702
  commits).
- Post-push message verification, both Step 1 and Step 2: one delta
  each against the message file, the known harmless trailing-newline
  artifact.
- Tag verification: `stable-20260807-player-close` peeled ref resolves
  to `b7ed686` exactly.
- Parity comparison (AR-VC-4): every one of the 85 modules' own verdict,
  and every `:ok-walked` module's own per-seed digest, compared
  directly against `2026-08-04-synthea-7e08387-wave-i2.edn` — zero
  diffs (script-verified, not a count comparison).

### Deviations, disclosed

- **No premise mismatch this session** — the prompt's own preflight
  expectations (clean tree, fresh-clone-green baseline) held exactly as
  stated, and the Synthea checkout the prompt named
  (`/home/mg/synthea-checkout`) was found live, at the pin, without
  needing to ask.
- **Read-first citation correction:** the driving prompt cited "the
  AR-5 dated-append convention" and "AR-8b's substance note" against
  `docs/gmf-interpreter.md` §15 — that content actually lives in
  `gmf-interpreter-findings.md` §15 (moved there verbatim, 2026-08-05,
  ADR-0043 AR-D-1; `gmf-interpreter.md` §9 keeps only the index table
  pointing at it). This session appended to the correct live file and
  added the index row to `gmf-interpreter.md` §9, both per the
  convention as it actually stands, disclosed here as a small premise
  correction, not blocking.

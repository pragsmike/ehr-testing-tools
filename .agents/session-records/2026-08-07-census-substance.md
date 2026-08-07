# 2026-08-07 — Census substance: the vendoring arc opens with an honest catalog

## Scope

Session prompt naming AR-VC-0 through AR-VC-6, opening the vendoring
arc (ratified `notes/adr/0066-player-fold.md` AR-BB1-R, sequenced per
`notes/adr/0068-player-arc-close.md`'s own horizon note). Closes
roadmap "Census tool refinements" items (a) and (c) in
`ehrt.sim-trajectory.census`, runs a fresh labeled census against the
author's own Synthea checkout, and appends the ranked catalog to
`gmf-interpreter-findings.md` section 15 as the design channel's own
curation-pass input. Full account, rulings, the relocated Deferred-row
text, and the parity comparison: `notes/ADRs.md` ADR-0069.

Step 0 (preflight) confirmed the working directory is the ext4 clone,
tip `b7ed686`, working tree clean. Baseline: `clojure -M:poly check`
OK; full suite green (227 `Test results:` lines, 0 failures/0 errors);
`gitleaks detect -v` clean (702 commits); oracle pre-digest
(`bin/regression-oracle b7ed686 b7ed686`) all eleven roots IDENTICAL.
AR-VC-0 executed directly: `stable-20260807-player-close` created
annotated at `b7ed686`, pushed, verified — peeled ref resolves exactly.

Step 1 (`b52afdb`, AR-VC-2/AR-VC-3 red) landed six new
`census_test.clj` cases alone, before any `census.clj` edit — captured
red as a compile failure (`No such var: census/artifact-filename`),
proof the new assertions reference API this session had not yet built.

Step 2 (`7cb92c6`, AR-VC-2/AR-VC-3/AR-VC-4 green + the fresh run)
landed `census-one`'s additive `:substance`/`:event-counts` fields,
`summarize`'s `:ok-walked-by-substance` tally, and the extracted
`artifact-filename` pure fn with `-main`'s new optional label arg —
all 7 pre-existing tests unmodified, 6 new tests green (13 total, 39
assertions). Full suite green (227/0/0), `poly check` OK. The fresh
census (against `/home/mg/synthea-checkout`, confirmed at the pin
before the run) landed labeled `substance`: 84 `:ok-walked` + 1
`:out-of-scope-by-ruling` (`gallstones`), 0 `:load-failed`, 0
`:walk-failed` — identical to the Wave I2 parity artifact's own counts,
confirmed module-by-module and digest-by-digest, zero diffs. Substance
tally: 51 `:zero-on-every-seed`, 33 `:produces-content`. The dated
subsection landed in `gmf-interpreter-findings.md` section 15 (the full
51-module list, the finding that `total-joint-replacement` — one of the
seven vendored roots — is itself zero-content under this census's own
parameters, disclosed not fixed); `gmf-interpreter.md` section 9's
index gained a row.

Step 3 (this record) authored `notes/adr/0069-census-substance.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own stale file count (66→67, verified by `ls`),
rewrote the roadmap's "Census tool refinements" row (items (a)/(c)
closed with their own text relocated into ADR-0069, item (b) untouched)
and its "Now" section, added the Done pointer
(`- 2026-08-07 — census-substance — ADR-0069`) in the same commit as
the index line, ran the closing oracle bracket, archived this prompt,
and recorded this session.

## Red→green evidence highlights

Step 1's own red was a compile failure, not a runtime assertion
failure — `ehrt.sim-trajectory.census-test` could not even load,
`No such var: census/artifact-filename` at `census_test.clj:303`,
because the new tests reference the pure fn AR-VC-3 asks for before it
exists. Step 2 turned this fully green: `census-test` grew from 7 tests
to 13 (39 assertions), 0 failures, 0 errors, both projects that exercise
it (`conformance`, `integration`).

## Judgment calls and their ratification status

- **The two content-bearing fixtures (`zero-content-json`,
  `produces-content-json`) were designed to be deterministic across all
  3 census seeds without any RNG-dependent branch** — an unconditional
  Initial→Terminal walk for the zero case, an unconditional ambulatory
  Encounter/EncounterEnd pair (no `wellness: true`, no Guard) for the
  content case — so the red-first assertions (`[0 0 0]` / `[2 2 2]`)
  hold regardless of mixer-seed derivation. Channel-inferred, not a
  ruled design choice; unremarkable given the existing fixture style
  the file's own 7 pre-existing tests already establish.
- **`:substance`/`:event-counts` are added via `cond->`, gated on
  `ok-walked?`, rather than always-present-but-nil** — matches AR-VC-2's
  own "additive... on an `:ok-walked` row" language literally: a
  `:walk-failed`/`:load-failed`/`:out-of-scope-by-ruling` row carries
  neither key at all (not present, not `nil`), proven by a dedicated
  negative-case test.

## Findings and HEAD landed

One finding surfaced and disclosed, not fixed (within AR-VC-6's own
fence — no gmf/loader/interpreter edit): `total-joint-replacement`, one
of the seven currently-vendored roots, censuses `:zero-on-every-seed`
under this census's own fixed persona/seed/horizon parameters — its
real content (CarePlan pair, CallSubmodule branches) simply never fires
for any of the 3 seeds this run used. Recorded in
`gmf-interpreter-findings.md` section 15's own new subsection, named
for whichever future session tunes census parameters or corpus
generation for this root.

Commits, in order: `b52afdb` (Step 1, red), `7cb92c6` (Step 2, green +
fresh run), and this session's own closing records commit (Step 3).

## Verification

- `bin/regression-oracle b7ed686 <this session's own closing commit>`:
  all eleven vendored-root batches IDENTICAL — expected, no
  gmf/loader/interpreter/engine/emitter file changed this session.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (227 namespaces, 0/0) and again after Step 2's own
  edits (227 namespaces, 0/0; `census-test` itself 13/39, 0/0).
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks detect -v` (full history): clean at Step 0 baseline (702
  commits).
- Post-push message verification, both Step 1 and Step 2: one delta
  each against the message file, the known harmless trailing-newline
  artifact.
- Tag verification: `stable-20260807-player-close` peeled ref resolves
  to `b7ed686` exactly.
- Parity comparison (AR-VC-4's own gate): every one of the 85 modules'
  own verdict, and every `:ok-walked` module's own per-seed digest,
  script-compared directly against
  `2026-08-04-synthea-7e08387-wave-i2.edn` — zero diffs.

## Deviations, disclosed

- **Citation correction, not blocking:** the driving prompt cited
  "`docs/gmf-interpreter.md` §15" for the AR-8b substance note and the
  AR-VC-4 dated-append target; that content actually lives in
  `gmf-interpreter-findings.md` §15 (moved verbatim 2026-08-05,
  ADR-0043 AR-D-1), with `gmf-interpreter.md` §9 keeping only an index
  table pointing at it. This session read from and appended to the
  correct live file, and added the index row to `gmf-interpreter.md`
  §9 — full account in `.agents/prompts/2026-08-07-census-substance.md`'s
  own deviation record.
- **The Synthea checkout path was not named in the prompt** — found
  live at `/home/mg/synthea-checkout`, confirmed at the pin before use.
- **No parity regression, no STOP-AND-REPORT.** Every other premise
  in the prompt's own read-first list held exactly as stated against
  the live tree at Step 0.

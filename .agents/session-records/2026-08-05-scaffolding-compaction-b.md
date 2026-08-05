# 2026-08-05 — Scaffolding compaction B: the ADR split and the roadmap rotation

## Scope

Design-channel session prompt naming AR-B-1 through AR-B-5. Session B
of the scaffolding-compaction arc (A — riders, vestige retirements,
Deferred triage — landed same day, `notes/ADRs.md` ADR-0045; C — the
continuity register, `.agents/state.md` — is pending author rulings,
not this session's scope). The arc's big relocation: `notes/ADRs.md`
(10512 lines, 43 inline entries) becomes an index over
`notes/adr/NNNN-<slug>.md` files; `.agents/plans/roadmap.md`'s Done
history (1338 of 1538 lines) rotates to dated attic files. Full
account and verbatim rulings: `notes/ADRs.md` ADR-0046.

Step 0 (characterize) verified tip `3495181` — matches the prompt's
own cited tip, no STOP-AND-ESCALATE. Enumerated 43 `## ADR-` boundaries
(the split map: number, file, original line range — full table in
ADR-0046) and 33 Done entries with dates (the rotation map). Reading-
set membership check: `.agents/reading-sets.edn`'s `:onboarding` is
the only set carrying `roadmap.md`; no set carries `notes/ADRs.md` —
membership unchanged, as ruled, no FINDING to raise on this front.
Baseline form-anchored deftest count: 108 in
`components/docs-tooling/test/` (`grep -rc '^(deftest' *.clj`); 1566
repo-wide (`git grep -o '(deftest ' 3495181 -- '*.clj' | wc -l`, this
repo's own established whole-tree convention, ADR-0044/ADR-0045).

Step 1 (`c2cefc0`, AR-B-1/AR-B-2) split all 43 entries to
`notes/adr/`, each with a 2-line attic header, and rewrote
`notes/ADRs.md`'s own preamble-plus-index. **Proof, not assertion:** a
Python script sliced the original file once at each `## ADR-` boundary
(no manual re-typing); concatenating the 43 per-ADR files' own content
minus their 3-line header, in the original file's own order, and
diffing against `notes/ADRs.md`'s pre-split lines 33–10512 produced
zero difference (`reconstructed == original_span`, exact Python string
equality). The preamble (lines 1–31, the numbering/citation rules) was
separately diffed against itself post-edit and found byte-identical —
untouched. Two required-but-unnamed gate satisfactions, disclosed: a
one-line `notes/adr/` bullet in `notes/README.md` (the directory's own
`real-subdirs` presence check would otherwise fail) and a new, minimal
`notes/adr/README.md` (the README-presence gate has no frozen-
provenance exemption for a new, live-appended directory). `clojure
-M:poly check`: OK. `clojure -M:poly test :all skip:integration`: 511
passes / 0 failures / 0 errors.

Step 2 (`fad2cf5`, AR-B-3) rotated the Done section (lines 201–1538)
to `.agents/plans/roadmap-done-2026-07.md` and `-2026-08.md`. **Finding
disclosed, not silently adapted:** all 33 Done entries are dated
2026-08 — none 2026-07 — so the July file necessarily holds zero
entries; its own two-line header states this rather than omitting the
file or pretending an entry exists. Same proof discipline: the
archive's own content minus its header, diffed against the pre-
rotation Done span (via `git show HEAD~1:.agents/plans/roadmap.md`),
zero difference. The live roadmap's own pre-Done head (lines 1–200)
diffed against its own pre-rotation self: byte-identical. `.agents/
plans/README.md` gained two star-bullet lines for the new archive
files (index-completeness-test's presence direction). `clojure -M:poly
check`: OK. Full suite: 511 / 0 / 0, unchanged in shape.

Step 3 (`cde6303`, AR-B-4) re-derived `:onboarding`'s budget (the only
set touched — `roadmap.md`'s own line count fell 1342→204, dropping
the set's real total from 2405→950 by the rotation alone; no other
set carries `roadmap.md` or `notes/ADRs.md`). Re-applying AR-D-3's own
formula (actual × 1.15, rounded up to the nearest 5): 950 → **1095**
(a dated comment records the derivation, `.agents/reading-sets.edn`).
Added `ehrt.docs-tooling.done-pointer-adr-test`, the new gate AR-B-4
names: every Done one-liner in the live roadmap must cite an ADR
number the index actually lists. **Red→green proven live, not merely
asserted:** ran green against the live files (ADR-0045 resolves);
corrupted the live pointer in place (`ADR-0045` → `ADR-9999`); re-ran
— **FAIL**, quoted below; restored from a pre-edit copy (`git diff`
confirmed empty); re-ran — green again. `clojure -M:poly check`: OK.
Full suite: 511 / 0 / 0.

Step 4 (this record, final commit) authored `notes/adr/0046-
scaffolding-compaction-b.md` directly (the new standing convention: a
fresh ADR lands in its own file from day one, not inline in
`notes/ADRs.md`), with its own index line appended, and added this
session's own one-line Done pointer (`- 2026-08-05 —
scaffolding-compaction-b — ADR-0046`) beside compaction A's. Verified
the gate still passes with both pointers resolving. Ran the oracle
bracket and the whole-repo deftest recount (below), archived this
prompt, indexed both new files in their own READMEs.

## Deviations, disclosed

- **Slugging rule under-specified by AR-B-1's own text, resolved by
  precedent.** "Slug from the entry's own title, lowercased-hyphenated"
  does not by itself say what to do with a title carrying a
  parenthetical citation or a long tail after its own colon/semicolon
  (several titles do). Resolved as: strip parenthetical groups, cut at
  the title's own first `:`/`;`, lowercase-hyphenate the remainder.
  Not invented freehand — this reproduces, mechanically, the exact
  slugs this repo's own prior sessions already hand-chose for two of
  these same 43 entries (ADR-0044 → `standing-equipment-promotion`,
  ADR-0045 → `scaffolding-compaction-a`), confirming the rule describes
  existing practice rather than adding a new one. Recorded in
  ADR-0046's own decision section, not left implicit in the diff.
- **Index-line status field simplified.** AR-B-1 asks for "the entry's
  own status/arc if its header states one." All 43 entries' own
  `**Status:**` line reads `Accepted` (confirmed by grep — no
  Superseded/Rejected among them); arc-closing language (e.g. ADR-0042's
  "GMF parity arc COMPLETE") already lives in the title text the index
  line copies verbatim, so a separate arc field would duplicate it. Each
  index line ends `— Accepted`, nothing more elaborate.
- **Two READMEs and one new README gained mechanically-required text
  beyond what AR-B-1/AR-B-3 name.** `notes/README.md` (one `adr/`
  bullet), `.agents/plans/README.md` (two archive-file bullets), and a
  new `notes/adr/README.md` (the README-presence gate has no exemption
  for a new, non-frozen directory). None of these are content
  rewrites of ADR or roadmap substance — they are the index-layer
  bookkeeping the split and rotation themselves mechanically require;
  leaving any of the three gates red was not an available option.
  Disclosed here and in ADR-0046's own Fence, not silently expanded
  scope.

## Findings (disclosed, not fixed — out of this session's own scope)

- **Zero 2026-07 Done entries.** `.agents/plans/roadmap-done-2026-07.md`
  exists per AR-B-3's own naming but holds no entries — the workspace's
  Done log never accumulated a 2026-07 row (bootstrap was 2026-07-28;
  the earliest Done entry is 2026-08-01). Not a defect in the ruling or
  the rotation; a fact about this workspace's own history, stated in
  the file's own header rather than silently produced empty with no
  explanation.
- **`notes/README.md`'s `ADRs.md` bullet needed more than a one-word
  fix.** Its pre-session wording ("numbered sequentially, `^## ADR-`")
  described the pre-split file exactly and would have been actively
  false post-split (no `## ADR-` line survives in `notes/ADRs.md`
  itself). Corrected in the same edit that added the `adr/` bullet —
  not a separate errata-sweep action, since AR-B-1 itself is what made
  the old wording stale; fixing the direct consequence of this
  session's own edit is not scope creep the way a broader staleness
  sweep would be.

## Verification

- `bin/regression-oracle 3495181 cde6303`: `IDENTICAL: every root's
  digest matches between 3495181 and cde6303` — all ELEVEN
  vendored-root batches (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, run
  against the pre-B tip and the Step-3 landing commit (Step 4 itself
  touches no `src` either — the bracket's verdict still holds at this
  record's own final commit).
- Deftest parity, counting definition stated: `git grep -o '(deftest '
  <ref> -- '*.clj' | wc -l` moved 1566 (`3495181`) → 1570 (`cde6303`),
  raw +4. **+1 is "the gate"** AR-B-4 asks to ledger
  (`every-done-pointer-cites-an-adr-that-exists-in-the-index-test`);
  **+3 are mechanism-sanity** tests proving the gate's own extraction
  functions actually work — the same gate-plus-proof pairing
  `reading-set-budget-test`/`index-completeness-test` already
  established in this suite, not a second gate.
- The gate, red→green, quoted: green on the live files; after
  corrupting the live pointer, `FAIL in
  (every-done-pointer-cites-an-adr-that-exists-in-the-index-test) ...
  ".agents/plans/roadmap.md's Done section cites ADR number(s) not in
  notes/ADRs.md's own index: [\"ADR-9999\"]"`; green again after
  restoring (`git diff .agents/plans/roadmap.md` empty, confirmed,
  before re-running).
- `clojure -M:poly check`: OK, all four steps. `clojure -M:poly test
  :all skip:integration`: 511 passes / 0 failures / 0 errors, all four
  steps, shape unchanged throughout (the new test file's 4 forms are
  the only addition to the count; no existing test's own pass count
  moved).
- Both extraction diffs (ADR split, roadmap rotation): zero-difference,
  exact string equality, commands and results recorded in ADR-0046.
- `:onboarding` reading-set budget: 2405 → 1095 (re-derived, AR-D-3
  formula re-applied against the post-rotation actual, 950 lines).
  `ehrt.docs-tooling.reading-set-budget-test`: green throughout (a
  shrinking actual never risks a ceiling-only gate).
- `gitleaks git --staged -v`: clean, every commit this session.

Commits, in order: `c2cefc0` (Step 1), `fad2cf5` (Step 2), `cde6303`
(Step 3), and this session's own closing records commit (Step 4).

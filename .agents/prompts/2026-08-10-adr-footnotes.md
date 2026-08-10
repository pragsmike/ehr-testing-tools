# 2026-08-10 — ehr-testing-tools: user-path ADR citations become footnotes (build session)

## Context

Archived 2026-08-10. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `d5ea0ed` (sim event-log adapter, ADR-0100) and
closed at the conversion commit (`4514a3f`) plus this record's own
close-phase commit. Original prompt follows verbatim; a deviation
record follows that.

## Original prompt (verbatim)

Session prompt -- ADR references in user docs become footnotes (ADR-0101)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session executes the roadmap Next row "ADR references
in user-facing documentation" (2026-08-08, fidelity riders, ADR-0081),
whose unruled fork the author resolved 2026-08-10: bare ADR-NNNN
citations in the user path convert to clickable markdown FOOTNOTES
(not stripped, not inline links). HEAD at handoff: d5ea0ed. This
session's ADR is ADR-0101.

The row's own prerequisite -- a full inventory -- was done by the
design channel (verify-then-act; re-derive by grep before editing):

- Hand-authored user docs, ~39 occurrences: docs/glossary.md (18),
  docs/formats.md (8 -- FOUR of which are already inline links
  `[ADR-NNNN](../notes/ADRs.md)`, an in-tree convention this session
  unifies into the footnote form as its ONE sanctioned improvement,
  disclosed), docs/judge-calibration.md (5), docs/site-profiles.md
  (5), docs/what-is-this.md (2), docs/locators.md (1).
- Generated per-case pages, 16 occurrences across 6 files under
  docs/use-cases/ (generate-sim-traffic, judge-user-supplied-data,
  piped-hl7-traffic-as-intake-source,
  play-a-generated-corpus-back-over-time,
  profile-tier-hl7v2-conformance-gating,
  simulator-traffic-as-intake-source): their SOURCE is
  components/corpus/docs/use-cases.edn (`make use-cases` output, "do
  not hand-edit" banners) -- convert in the EDN prose, regenerate,
  never hand-edit the pages. The generator passes markdown prose
  through verbatim (usecases.clj's own docstring); PROVE footnote
  rendering survives generation by reading the regenerated output.
- docs/cli.md, docs/operators.md, docs/use-cases.md (index): zero
  citations -- untouched.
- No link-validity lint exists in docs-tooling today; this session
  lands one as the conversion's co-landed gate.

Oracle bracket, with its reasoning: pure identity on all 34 roots is
EXPECTED trivially -- this session's footprint is markdown, one
docsgen EDN source (documentation data, not runtime-reachable), and
one new test file; no src namespace changes anywhere. Any digest
movement is STOP-AND-REPORT.

## Read first

- `.agents/plans/roadmap.md` -- the Next row verbatim
- `notes/adr/0010-documentation-doctrine.md` -- the three-class
  audience split and R34's history rule (the footnote text must obey
  the user path's voice: no Polylith terms, no components/ paths)
- `notes/adr/README.md` preamble -- the citation law (cite through
  `notes/ADRs.md`, not the attic directly): the footnote definitions'
  link target follows it
- docs/formats.md -- the four existing inline links (the form being
  unified)
- components/corpus/docs/use-cases.edn and
  components/docs-tooling/src/ehrt/docs_tooling/usecases.clj -- the
  generation path
- components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj
  -- the scan-source pattern the new gate mirrors
- `.agents/rulings.md` -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-10, the fork, author verbatim "as clickable footnotes"
  then "a" (true markdown footnotes over inline links): every bare
  ADR-NNNN citation in the user path becomes a footnote marker whose
  definition links to the citation index.
- [A] 2026-08-10, --sink ratification, author verbatim "--sink call
  ok for now.": ADR-0100's disclosed judgment (rejecting --sink on
  event input, :play-sink-unsupported-for-events) is RATIFIED as a
  ruling. No code change -- this session records it in .agents/rulings.md
  at the close, citing ADR-0100 deviation 2.
- [C] Channel-specified shape, verify rendering before mass-applying:
  marker `[^adr-NNNN]` at the citation site; definition once per file,
  `[^adr-NNNN]: Design record [ADR-NNNN](../notes/ADRs.md).` --
  target is the INDEX file per the citation law and the existing four
  links' own precedent, never a notes/adr/ attic file. Repeat
  citations of the same ADR in one file reuse the one definition.
  Definitions sit grouped at each file's bottom; for the EDN-sourced
  pages, definitions live in that case's own prose block (GitHub
  collects them at render time regardless) with the link depth
  written for the GENERATED page's location (docs/use-cases/ ->
  `../../notes/ADRs.md`).
- [C] Rewording fence: wherever the citation lifts out cleanly, the
  surrounding sentence is byte-identical with the bare citation
  replaced by the marker. Where the citation is grammatically
  load-bearing ("per ADR-0013's own rule"), reword minimally, keep
  the technical claim identical, and LIST every reworded sentence in
  the ADR (before/after). Compound forms ("ADR-0013/ADR-0015",
  "`notes/ADRs.md` ADR-0007") normalize to one marker per ADR.
  Footnote text stays in the user path's voice per R34: "Design
  record", never a repo-layout tour.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0100 landing at `d5ea0ed` by fresh
   public clone. Tag `stable-20260810-sim-event-log-adapter` at
   `d5ea0ed`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Re-derive the inventory.** Extension-blind grep for
   `ADR-[0-9]{4}` across docs/ (excluding docs/dev/) AND the
   use-cases EDN source; reconcile against the channel counts above;
   disclose any difference. This is the ADR's own inventory table.

3. **The gate first (red proven, then green).** New docs-tooling
   test: (a) every relative markdown link `](...)` in docs/ proper
   (docs/dev/ excluded, matching the row's scope -- name dev-docs
   expansion as a want, not built) resolves to an existing file,
   anchors stripped, http(s) skipped, resolution relative to the
   linking file's own directory; (b) every `[^...]` footnote marker
   in those files has a matching in-file definition and vice versa.
   Non-vacuity witness: temporarily plant one broken link and one
   orphan marker, paste the red verbatim into the ADR, remove them.
   The gate passes on the PRE-conversion tree (the four existing
   links are valid) -- run and record that too.

4. **Convert, one commit with the gate.** The six hand-authored files
   per the [C] shape (formats.md's four inline links unified -- the
   one sanctioned improvement, disclosed); the use-cases EDN prose
   for the six generated pages; `make use-cases` regenerate; confirm
   by diff that ONLY the six expected generated pages changed (index
   and cli.md byte-identical). Read one regenerated page end to end
   to prove footnote syntax survived generation. Gate + full
   docs-tooling suite green.
   Commit message (ASCII only):
   `docs: user-path ADR citations become footnotes, link gate co-landed (ADR-0101)`

5. **Oracle bracket.** bin/regression-oracle across the session.
   Expected pure identity per Context; movement = STOP-AND-REPORT.

6. **Full gate.** poly check, full local suite, CLI parse-guard lint,
   bin/verify-nist-lock, lint-pipeline (use-cases.edn's catalytic
   resources untouched by prose edits -- the lint proves it).

7. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0101
   (inventory table, the reworded-sentences list, the non-vacuity
   red, deviations dated); roadmap row to Done; .agents/rulings.md
   records BOTH 2026-08-10 rulings -- the footnote fork ("a") and
   the --sink ratification ("ok for now", citing ADR-0100 deviation
   2); notes/ADRs.md index row; notes/adr/README.md count 98 -> 99;
   session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- ADR footnotes (ADR-0101)`

8. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: the six hand-authored docs named, docs/use-cases/*.md
  (generated, via regeneration only), docs/use-cases.md and
  docs/cli.md ONLY if regeneration itself moves them (expected: it
  does not -- a movement is a finding to disclose),
  components/corpus/docs/use-cases.edn (prose strings only),
  components/docs-tooling/test/ (the new gate), notes/adr/0101-*.md,
  notes/ADRs.md, notes/adr/README.md, .agents/* close-phase files.
  The sweep RULE governs over this list (ADR-0099 precedent): a
  citation the inventory missed gets converted and disclosed, not
  skipped.
- docs/dev/ untouched. Generated pages never hand-edited. No src
  namespace changes anywhere. Makefile untouched.
- Technical claims in every touched sentence are unchanged; this
  session moves citations, not content.
- No history rewrites; deviations in the ADR's dated appendix;
  STOP-AND-REPORT over improvisation.
- Channel claims (counts, file lists, generator pass-through) are
  verify-then-act.

## Deviation record

None from the driving prompt's own steps, fences, or rulings. Three
findings surfaced during the re-derived inventory (Step 2) that the
prompt's own channel-probed counts did not distinguish, all disclosed
in ADR-0101 rather than acted on silently or smoothed over:

1. The origin-qualified `sim/ADR-N`/`tools/ADR-N` citation form (12 of
   the 39 hand-authored occurrences) points at a different, frozen
   document than `notes/ADRs.md` per this workspace's own standing
   citation rule — out of scope for footnote conversion, confirmed
   before converting anything rather than assumed in scope.
2. Five of the sixteen generated-page citation sites sit inside a
   `:commands :lines` code-fence comment, where footnote markdown
   cannot render — verified by reading the actual rendered pages,
   left bare by necessity.
3. Two pre-existing citation-accuracy anomalies in `docs/glossary.md`
   (a citation naming the wrong document, per that ADR's own
   collision-disambiguation note) — disclosed and left, matching this
   workspace's own established posture for prose staleness found
   outside a link-audit's scan.

An additional, undirected design choice this session made and
verified rather than left implicit: an append-in-place footnote
convention (keep the visible `ADR-NNNN` text, insert the marker after
it, never remove it) — checked against the driving prompt's own
rewording-fence worked example and found to need zero actual
rewording under this convention. See ADR-0101's own Decision and
Deviations sections for the full reasoning on all of the above.

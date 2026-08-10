# 2026-08-10 — User-path ADR citations become footnotes (ADR-0101)

## Scope

Session prompt executing the roadmap Next row "ADR references in
user-facing documentation" (2026-08-08, fidelity riders, ADR-0081),
whose unruled fork the author resolved this session: bare `ADR-NNNN`
citations in the user path (`docs/` proper) become clickable footnotes,
not stripped. Two commits: the conversion itself (six hand-authored
docs, six generated pages via their EDN source, plus a new link/
footnote validity gate), and this close-phase record/prompt-archive
commit. This is ADR-0101.

## Red→green evidence highlights

The new gate (`ehrt.docs-tooling.link-footnote-gate-test`) ran
pre-conversion green (93 assertions, the four pre-existing `formats.md`
inline links already valid), then non-vacuously red after planting one
broken link and one orphan footnote marker in `docs/locators.md` (both
failures captured verbatim in ADR-0101), then green again after the
witness was removed (confirmed byte-identical `git diff`) and again
after the real conversion landed.

`make use-cases` regeneration: `git diff --stat` confirmed only the six
expected generated pages changed; the index (`docs/use-cases.md`) and
`docs/cli.md` stayed byte-identical, not merely unchanged in content
but absent from the diff entirely. One regenerated page read end to
end to prove footnote markers/definitions survive `make use-cases`
verbatim, and that the one fence-comment citation left bare renders
exactly as written (not garbled by the generator).

**Full gate:** `clojure -M:poly check` OK; full local suite
(`clojure -M:poly test :all skip:integration`, unredirected capture)
596 occurrences of "0 failures, 0 errors" across the entire log, 0
FAIL/ERROR anywhere, `EXIT:0`, 4 minutes; `bin/verify-nist-lock` OK,
6/6; `make lint-pipeline` OK; oracle bracket
(`d5ea0ed`→`4514a3f`) all 34 roots IDENTICAL, soundness check clean.

## Judgment calls and their ratification status

- **Append-in-place footnote convention, not full-replacement.** Not
  separately ratified — a verified design choice within the driving
  prompt's own [C] "verify rendering before mass-applying" license
  (the [C] shape named the marker and definition form, not whether the
  citation site's visible text survives). Disclosed in ADR-0101's
  Decision and Deviations, with the worked-example proof that the
  fence's own anticipated rewording case never actually arises under
  this convention.
- **Two pre-existing citation-accuracy anomalies in `docs/glossary.md`
  (the Baseline entry's `ADR-0013`/`ADR-0015`, the Pack entry's
  `ADR-0006-era`) found and disclosed, not fixed.** Matches this
  workspace's own established posture for prose staleness found
  outside a link-audit's scan (ADR-0010's own precedent), applied here
  by this session's own judgment — not separately ruled. Revisit
  trigger named in ADR-0101.
- **Five generated-page citation sites left bare (inside a `:commands
  :lines` code-fence comment).** Not a missed conversion — verified by
  reading the actual rendered pages that footnote markdown cannot
  process inside a fenced code block, disclosed rather than converted
  wrongly or silently skipped.
- **The footnote fork itself and the `--sink` ratification** — both
  ruled directly by the author in this session's own driving prompt
  (verbatim, cited in `.agents/rulings.md`'s new "From ADR-0101"
  section), not a judgment call this session made unprompted.

## Findings and HEAD landed

**No defects found in shipped code.** This session's own footprint is
markdown, one docsgen EDN source (documentation data, not
runtime-reachable), and one new test file — the oracle's pure-identity
prediction held exactly, confirming no `src` namespace was touched
anywhere.

**Tag paid forward:** `stable-20260810-sim-event-log-adapter` tagged at
`d5ea0ed` (Step 1, this session — the design channel's own verified
ADR-0100 landing, tag law case (i)), peeled ref verified exact match,
remote unmoved.

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.

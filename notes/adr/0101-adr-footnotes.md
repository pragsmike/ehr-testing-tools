## ADR-0101 — User-path ADR citations become footnotes, a link/footnote gate co-lands

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-10.

### Context

The roadmap Next row ("ADR references in user-facing documentation",
2026-08-08, fidelity riders, ADR-0081) anchors this session: bare
`ADR-NNNN` citations in the user path (`docs/` proper, per ADR-0010's
own audience fork — not `docs/dev/`) either strip or convert to
clickable footnotes, the fork left unruled at ADR-0081's own close. The
author resolved the fork 2026-08-10 (Q1 below): footnotes, not
stripping. This session's own prerequisite — the full inventory
ADR-0081 named but didn't run — is re-derived fresh, below, before any
edit lands (verify-then-act).

### Inventory, re-derived

Extension-blind grep for `ADR-[0-9]{4}` across `docs/` (excluding
`docs/dev/`) plus `components/corpus/docs/use-cases.edn` found every
file under scope is `.md` (no other extension carries a citation).

**Hand-authored docs, 39 occurrences, matching the design channel's
own count exactly per file:**

| File | Total | Bare (footnote candidates) | Origin-qualified (`sim/ADR-N`, out of scope) |
|---|---|---|---|
| `docs/glossary.md` | 18 | 9 | 9 |
| `docs/formats.md` | 8 (4 already inline links) | 8 | 0 |
| `docs/judge-calibration.md` | 5 | 5 | 0 |
| `docs/site-profiles.md` | 5 | 4 | 1 |
| `docs/what-is-this.md` | 2 | **0** | 2 |
| `docs/locators.md` | 1 | 1 | 0 |
| **Total** | **39** | **27** | **12** |

**A real scoping finding, not anticipated by the driving prompt's own
inventory:** `notes/ADRs.md`'s own preamble states a standing citation
rule (dated 2026-07-30): a **bare** `ADR-NNNN` anywhere in this
workspace means *this file's own record*; a citation into a frozen
pre-merge sequence (`notes/sim/ADRs.md`, `notes/tools/ADRs.md`) is
always **origin-qualified** (`sim/ADR-0008`, `tools/ADR-0012`) —
"never bare." `sim/ADR-NNNN`/`sim/FNN` citations therefore point at a
**different document** than `notes/ADRs.md`, and converting them to a
footnote whose definition links `notes/ADRs.md` would misattribute the
citation. These stay untouched, exactly as written, in every file —
most visibly `docs/what-is-this.md`, where **both** of its two
citations are origin-qualified: this file has **zero** footnote
conversions, a fact the driving prompt's own per-file inventory line
("what-is-this.md 2") did not distinguish.

**Generated pages (`components/corpus/docs/use-cases.edn` prose), 20
raw `ADR-NNNN` token matches, 16 distinct citation sites (a repeated
or compound citation on one line counts once) across the six named
pages — the channel's own "16" is the site count, reconciled here
against the raw 20:**

| Page | Raw tokens | Sites | Convertible (prose) | Left bare (in a `:lines` code-fence comment) |
|---|---|---|---|---|
| `generate-sim-traffic.md` | 3 | 2 | 3 | 0 |
| `judge-user-supplied-data.md` | 3 | 3 | 1 | 2 |
| `piped-hl7-traffic-as-intake-source.md` | 2 | 2 | 2 | 0 |
| `play-a-generated-corpus-back-over-time.md` | 4 | 3 | 3 | 1 |
| `profile-tier-hl7v2-conformance-gating.md` | 4 | 3 | 3 | 1 |
| `simulator-traffic-as-intake-source.md` | 4 | 3 | 3 | 1 |
| **Total** | **20** | **16** | **15** | **5** |

**A second real finding, verified by rendering before mass-applying
(per the driving prompt's own [C] instruction):** five of the sixteen
generated-page citation sites sit inside a `:commands :lines` shell
comment, rendered inside a fenced ` ```sh ` block. Markdown footnote
syntax does not process inside a fenced code block (confirmed by
reading the actual rendered pages, not assumed) — converting these
would print literal, broken `[^adr-NNNN]` text into a copy-pasted
terminal comment, not a clickable footnote. These five stay bare, by
necessity, disclosed here rather than silently skipped or wrongly
converted: `judge-user-supplied-data.md` (`ADR-0011`, `ADR-0014`),
`play-a-generated-corpus-back-over-time.md` (`ADR-0015`),
`profile-tier-hl7v2-conformance-gating.md` (`ADR-0012`),
`simulator-traffic-as-intake-source.md` (`ADR-0014`).

`docs/cli.md`, `docs/operators.md`, `docs/use-cases.md` (the generated
index): confirmed zero citations, untouched, matching the driving
prompt's own expectation.

### Decision

**Q1 a. (the footnote fork), author verbatim, 2026-08-10, "as
clickable footnotes" then "a":** every bare `ADR-NNNN` citation in the
user path becomes a footnote marker whose definition links the
citation index (`notes/ADRs.md`), not stripped.

**[C] Shape, verified by rendering before mass-applying:** marker
`[^adr-NNNN]` at the citation site; one definition per file (or, for a
generated page, per case's own prose block), `[^adr-NNNN]: Design
record [ADR-NNNN](../notes/ADRs.md).` (`../../notes/ADRs.md` for the
generated pages, one directory deeper); a repeat citation of the same
ADR in one file reuses the one definition.

**A convention decision this session made and verified, not left
implicit: append-in-place, never remove the visible `ADR-NNNN` text.**
The marker is inserted immediately after the existing, human-readable
`ADR-NNNN` mention (`ADR-0010[^adr-0010]`), rather than replacing that
text with the marker alone. Verified against the fence's own worked
example ("per `ADR-0013`'s own rule," named as needing a reword because
full replacement breaks the possessive grammar): under append-in-place,
`ADR-0013[^adr-0013]'s own rule` reads correctly with **no reword at
all**. Checked against every one of this session's own 27 hand-authored
plus 15 generated-page conversions — none needed rewording under this
convention (the rewording-fence's own worked example never actually
arises), so **the reworded-sentences list this session's own Steps
anticipated is empty by construction**, disclosed as a design choice
rather than silently absent. Every touched sentence's technical claim
is unchanged; only a marker was inserted.

**formats.md's four inline links unified, the one sanctioned
improvement, disclosed:** `[ADR-0010](../notes/ADRs.md)` /
`[ADR-0009](../notes/ADRs.md)` (four occurrences, two ADRs) become
`ADR-0010[^adr-0010]` / `ADR-0009[^adr-0009]`, matching every other
citation's new footnote form.

**Two pre-existing citation-accuracy anomalies found in
`docs/glossary.md` during the sweep, disclosed and left, not fixed —
matching ADR-0010's own established posture for prose staleness found
outside a link-audit's own scan ("disclosed rather than silently
incomplete"):**

1. **The Baseline entry** (`Register: `notes/ADRs.md` ADR-0013/ADR-0015
   (tools' pre-merge sequence, `notes/tools/ADRs.md`).`) cites bare
   `ADR-0013`/`ADR-0015` — which, per the standing citation rule above,
   means *this file's own* ADR-0013 ("Output UX doctrine") and ADR-0015
   ("CLI trial-UX") — while its own parenthetical says the content is
   "tools' pre-merge sequence." Cross-checked against both: the entry's
   own content (two gate-loop baselines, legacy-floor and
   full-capability) matches `notes/tools/ADRs.md`'s ADR-0013 ("the
   cross-repo consumer loop... baseline-delta drift detection") and
   ADR-0015 ("the gate loop maintains TWO baselines"), **not** this
   file's own same-numbered, unrelated records. `notes/adr/
   0013-output-ux-doctrine.md`'s own deviation note independently
   confirms the correct origin-qualified form is `tools/ADR-0013`. This
   session converted the citation exactly as literally written (both
   tokens footnoted to `notes/ADRs.md`, per the mechanical rule), which
   makes the pre-existing mismatch clickable and therefore more visible
   — not introduced by this session, not fixed by it either, since
   correcting *which document* a citation names is content work, this
   session's own fence reserves for "moves citations, not content."
2. **The Pack entry** (`Retired mechanism (`notes/ADRs.md`
   ADR-0006-era, tools' pre-merge sequence)`) names "ADR-0006-era" —
   itself an approximate, non-precise time marker — alongside "tools'
   pre-merge sequence," the same shape as (1). Converted literally
   (`ADR-0006[^adr-0006]-era`); the same disclosure applies.

Revisit trigger for both: a future session auditing citation *targets*
for accuracy, not just citation *form* — out of this session's own
scope.

### The gate, red then green (non-vacuity witness)

New test, `ehrt.docs-tooling.link-footnote-gate-test`
(`components/docs-tooling/test/ehrt/docs_tooling/link_footnote_gate_test.clj`):
(a) every relative markdown link in `docs/` proper resolves to a real
file, anchors stripped, http(s)/`mailto:` skipped, resolved relative to
the linking file's own directory; (b) every `[^id]` marker has a
matching in-file `[^id]:` definition and vice versa.

Pre-conversion (the four existing `formats.md` inline links, no
footnotes anywhere yet): green, 2 tests, 93 assertions, 0
failures/errors.

Non-vacuity witness: one broken link
(`[broken link](nonexistent-file-xyz.md)`) and one orphan marker
(`[^orphan-marker-xyz]`) planted at the end of `docs/locators.md`,
removed after capture. Red, verbatim:

```
FAIL in (every-relative-link-in-docs-proper-resolves-test) (link_footnote_gate_test.clj:106)
docs/locators.md has broken relative link(s): clojure.lang.LazySeq@9883a430
expected: (empty? broken)
  actual: (not (empty? ("nonexistent-file-xyz.md")))

FAIL in (every-footnote-marker-has-a-definition-and-vice-versa-test) (link_footnote_gate_test.clj:113)
docs/locators.md has footnote marker(s) with no definition: #{"orphan-marker-xyz"}
expected: (empty? undefined)
  actual: (not (empty? #{"orphan-marker-xyz"}))

Ran 2 tests containing 93 assertions.
2 failures, 0 errors.
```

`docs/locators.md` confirmed byte-identical (empty `git diff`) after
the witness was removed; green again before any real conversion landed.

### Convert, one commit with the gate

The six hand-authored files (formats.md's four inline links unified,
the sanctioned improvement); `components/corpus/docs/use-cases.edn`'s
prose for the six affected cases; `make use-cases` regenerated. `git
diff --stat` confirmed only the six expected generated pages changed;
`docs/use-cases.md` (index) and `docs/cli.md` byte-identical (not in
the diff at all). One regenerated page
(`docs/use-cases/play-a-generated-corpus-back-over-time.md`) read end
to end: footnote markers and definitions survived generation intact
(lines 8, 10, 31–33), and the one fence-comment citation left bare
(line 21) rendered exactly as written — proof the design holds under
real generation, not just in the source EDN.

Gate green post-conversion: 2 tests, 93 assertions, 0 failures/errors.
Full `clojure -M:poly test :all skip:integration`: 596 occurrences of
"0 failures, 0 errors" across the entire log, 0 FAIL/ERROR anywhere,
`EXIT:0`, 4 minutes execution time (unredirected capture, matching the
verification discipline this repo's own prior ADRs use).

Commit: `4514a3f` — "docs: user-path ADR citations become footnotes,
link gate co-landed (ADR-0101)."

### Oracle bracket

Pure identity was the prediction (markdown, one docsgen EDN source,
one new test file — no `src` namespace touched anywhere).
`bin/regression-oracle d5ea0ed 4514a3f`: soundness check IDENTICAL
outside `digest.clj`'s own `(ns ...)` form; all 34 roots' SHA-256
digests IDENTICAL between baseline and target. Matches the prediction
exactly — no STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. `bin/verify-nist-lock`: OK, 6/6.
`ehrt.cli.cli-parse-guard-lint-test`: confirmed green within the full
suite run (line 1149 of the captured log). `make lint-pipeline`: OK —
`use-cases.edn`'s own catalytic resources are untouched by this
session's prose-only edits, proven rather than assumed.

### Tag ceremony

`stable-20260810-sim-event-log-adapter` tagged at `d5ea0ed` (the design
channel's own verified ADR-0100 landing), pushed, peeled ref confirmed
`d5ea0ed200af074e5c79c8ab0707dfd4f8ce0769` — exact match, remote had
not moved.

### Fences

Touched exactly the list the driving prompt named: the six
hand-authored docs, `docs/use-cases/*.md` (regenerated only, six of
twenty actually moved), `components/corpus/docs/use-cases.edn` (prose
strings only), `components/docs-tooling/test/` (the new gate),
`notes/adr/0101-*.md`, `notes/ADRs.md`, `notes/adr/README.md`,
`.agents/*` close-phase files. `docs/dev/` untouched. No generated page
hand-edited (all six landed via `make use-cases`). No `src` namespace
changed anywhere. `Makefile` untouched.

### Deviations, dated 2026-08-10

1. **Append-in-place, not full-replacement, adopted as this session's
   own footnote convention** — see Decision, above. A design choice
   within the [C] ruling's own "verify rendering before mass-applying"
   license, not a departure from it: the [C] shape names the marker and
   the definition's own form, not whether the citation site's visible
   text survives. Disclosed rather than assumed obvious.
2. **Two pre-existing citation-accuracy anomalies found in
   `docs/glossary.md`, disclosed and left, not fixed** — see Decision,
   above (the Baseline and Pack entries). Revisit trigger: a future
   citation-*accuracy* audit, distinct from this session's own
   citation-*form* scope.
3. **Five generated-page citation sites left bare by necessity**
   (inside a `:commands :lines` code-fence comment) — see Inventory,
   above. Not a missed conversion: footnote markdown cannot render
   inside a fenced code block, verified by reading the actual rendered
   output before converting anything.

No other deviations from the driving prompt's own steps, fences, or
rulings.

### Consequence

Every bare `ADR-NNNN` citation in the user path (`docs/` proper) that
can legitimately become a clickable footnote now is one; the twelve
origin-qualified `sim/ADR-N`/`tools/ADR-N` citations and the five
fence-comment citations stay exactly as written, both for principled,
disclosed reasons rather than oversight. A link/footnote validity gate
lands co-committed, red-proven non-vacuous, green on the converted
tree, and will catch a future broken link or orphaned marker anywhere
under `docs/` (excluding `docs/dev/`) the same way. The roadmap's own
"ADR references in user-facing documentation" Next row closes.

### Index line

```
- 2026-08-10 — adr-footnotes — ADR-0101
```

(appended to `.agents/plans/roadmap.md`'s own Done section, alongside
the Next-row removal this same commit makes.)

`notes/adr/README.md`'s own file count corrects 98→99, verified by `ls
notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

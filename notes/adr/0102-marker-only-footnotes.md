## ADR-0102 — User-path ADR citations go marker-only, full user path, gate hardened; `:mllp` sink abandoned

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-10.

### Context

Three author rulings, 2026-08-10, all cited verbatim below, drove this
session:

1. The footnote FORM ADR-0101 landed (append-in-place: visible
   `ADR-NNNN` token immediately followed by the marker,
   `ADR-0013[^adr-0013]`) is superseded — the author wants marker-only,
   no visible ADR token left in user-facing prose.
2. The SCOPE widens from ADR-0101's bare-only citations to the full
   user path: the 12 origin-qualified citations (`sim/ADR-NNNN`,
   `tools/ADR-NNNN`) ADR-0101 explicitly left untouched now also go
   marker-only, footnoted for the first time, targeting the frozen
   pre-merge indexes (`notes/sim/ADRs.md`, `notes/tools/ADRs.md`) they
   actually name.
3. The `:mllp` sink (`notes/adr/0014-corpus-player.md`'s own named
   future work) is abandoned for now — no transport work, no design-doc
   amendment, just the ruling itself plus a one-line help-text
   correction and an inventory of where the now-superseded deferral
   language still lives.

### Author rulings, verbatim

- **[A] 2026-08-10, author verbatim: "Let's abandon `:mllp` for now."**
  Read as: no transport work, no amendment to
  `docs/dev/source-sink-design.md` (frozen-archives discipline — the
  design doc's D2/D3 stand as written and governing); the close records
  the ruling and the design-channel finding beside it (below).
- **[A] 2026-08-10, author verbatim: "For footnotes, do b, marker-only
  form. I don't want ADRs cluttering user-facing prose."** Every
  footnoted citation site drops its visible `ADR-NNNN` token; the
  marker alone remains.
- **[A] 2026-08-10, scope, author verbatim "a"** (in answer to: footnote
  the origin-qualified citations too): the FULL user path goes
  marker-only, origin-qualified citations included, their definitions
  targeting the frozen pre-merge indexes.

### Re-derived inventory, reconciled against ADR-0101's own count

Fence-aware, definition-aware re-scan of `docs/` proper (excluding
`docs/dev/`) plus `components/corpus/docs/use-cases.edn`, same method
ADR-0101's own inventory used. **Zero deltas found** — every site
ADR-0101 catalogued is still exactly where it left it; this session's
own scope is FORM, not content, so no new citation appeared or
disappeared between the two sessions.

| Class | ADR-0101's count | This session's re-derivation | Delta |
|---|---|---|---|
| Hand-authored, bare (already footnoted, append-in-place) | 27 | 27 | 0 |
| Hand-authored, origin-qualified (untouched by ADR-0101) | 12 | 12 | 0 |
| Generated-page sites, convertible prose (already footnoted) | 15 | 15 | 0 |
| Generated-page sites, fence-comment (left bare by necessity) | 5 | 5 | 0 |

Per-file breakdown, hand-authored bare sites (all six files ADR-0101
touched): `docs/glossary.md` 9 tokens across 8 sites (one compound,
`ADR-0013`/`ADR-0015`, the Baseline anomaly), `docs/formats.md` 8,
`docs/judge-calibration.md` 5, `docs/site-profiles.md` 4,
`docs/locators.md` 1, `docs/what-is-this.md` 0 — total 27, matching
ADR-0101's own table exactly.

Per-file breakdown, hand-authored origin-qualified sites (entering the
footnote sweep for the first time this session): `docs/glossary.md` 9
(`sim/ADR-0008`, `sim/ADR-0002`×2, `sim/ADR-0010`×2, `sim/ADR-0007`×2,
`sim/ADR-0012`, `sim/ADR-0011`), `docs/site-profiles.md` 1
(`sim/ADR-0002`), `docs/what-is-this.md` 2 (`sim/ADR-0007`,
`sim/ADR-0002`) — total 12, matching ADR-0101's own table exactly,
including `what-is-this.md`'s own two sites ADR-0101 named as its
"zero footnote conversions" file — this session is the first to touch
it.

The five fence-comment sites (`judge-user-supplied-data.md` ×2,
`play-a-generated-corpus-back-over-time.md`, `profile-tier-hl7v2-
conformance-gating.md`, `simulator-traffic-as-intake-source.md`) stay
exactly as ADR-0101 left them — verified unchanged by this session's
own gate (see below), not merely assumed.

### The gate, hardened first, red proven on the pre-conversion tree

`ehrt.docs-tooling.link-footnote-gate-test` gains a third check,
`no-visible-adr-token-in-prose-test`: strip every fenced code block
(any info string) and every footnote-definition line, then assert no
`ADR-\d{4}` substring survives in what's left — the regex doesn't
distinguish bare from origin-qualified, so a citation qualified
`sim/ADR-0008` trips it exactly like a bare `ADR-0008` would.
Definition-name/marker parity (`every-footnote-marker-has-a-definition-
and-vice-versa-test`) already covers the new `[^sim-adr-*]`/
`[^tools-adr-*]` families unmodified — the regex there was always
`[A-Za-z0-9-]+`, no change needed.

Proven red on the pre-conversion tree (working tree with the real
conversion stashed out, the new gate test un-stashed) via `clojure -M:poly
test :all component:docs-tooling`, verbatim:

```
FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/glossary.md has visible ADR-NNNN token(s) in prose: #{"ADR-0009" "ADR-0002" "ADR-0012" "ADR-0010" "ADR-0001" "ADR-0011" "ADR-0013" "ADR-0007" "ADR-0005" "ADR-0008" "ADR-0015" "ADR-0006"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0009" "ADR-0002" "ADR-0012" "ADR-0010" "ADR-0001" "ADR-0011" "ADR-0013" "ADR-0007" "ADR-0005" "ADR-0008" "ADR-0015" "ADR-0006"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/judge-calibration.md has visible ADR-NNNN token(s) in prose: #{"ADR-0012" "ADR-0010"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0012" "ADR-0010"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/formats.md has visible ADR-NNNN token(s) in prose: #{"ADR-0009" "ADR-0010" "ADR-0020" "ADR-0013" "ADR-0014"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0009" "ADR-0010" "ADR-0020" "ADR-0013" "ADR-0014"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/site-profiles.md has visible ADR-NNNN token(s) in prose: #{"ADR-0002" "ADR-0029" "ADR-0014"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0002" "ADR-0029" "ADR-0014"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/locators.md has visible ADR-NNNN token(s) in prose: #{"ADR-0008"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0008"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/use-cases/piped-hl7-traffic-as-intake-source.md has visible ADR-NNNN token(s) in prose: #{"ADR-0011" "ADR-0014"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0011" "ADR-0014"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/use-cases/judge-user-supplied-data.md has visible ADR-NNNN token(s) in prose: #{"ADR-0014"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0014"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/use-cases/generate-sim-traffic.md has visible ADR-NNNN token(s) in prose: #{"ADR-0015"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0015"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/use-cases/profile-tier-hl7v2-conformance-gating.md has visible ADR-NNNN token(s) in prose: #{"ADR-0010" "ADR-0013" "ADR-0004"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0010" "ADR-0013" "ADR-0004"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/use-cases/simulator-traffic-as-intake-source.md has visible ADR-NNNN token(s) in prose: #{"ADR-0014" "ADR-0015"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0014" "ADR-0015"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/use-cases/play-a-generated-corpus-back-over-time.md has visible ADR-NNNN token(s) in prose: #{"ADR-0013" "ADR-0014" "ADR-0015"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0013" "ADR-0014" "ADR-0015"}))

FAIL in (no-visible-adr-token-in-prose-test) (link_footnote_gate_test.clj:168)
docs/what-is-this.md has visible ADR-NNNN token(s) in prose: #{"ADR-0002" "ADR-0007"}
expected: (empty? tokens)
  actual: (not (empty? #{"ADR-0002" "ADR-0007"}))

Ran 3 tests containing 124 assertions.
12 failures, 0 errors.

Test results: 112 passes, 12 failures, 0 errors.
```

All 12 in-scope files fail, exactly the 12 files the re-derived
inventory names — a non-vacuous red proving the new check actually
fires against the real pre-conversion tree, not a synthetic fixture
only. `docs/use-cases/simulator-traffic-as-intake-source.md`,
`docs/use-cases/judge-user-supplied-data.md`, and
`docs/use-cases/profile-tier-hl7v2-conformance-gating.md`'s own
fence-comment tokens do NOT appear in these sets (the fence-stripping
half of the check already excludes them correctly, pre-conversion) —
confirmed by inspection, not merely assumed.

### Convert, one commit with the hardened gate

The six hand-authored files: every already-footnoted bare site drops
its visible token, marker only; every origin-qualified site gains a
first-time footnote (marker + a new, distinctly-named definition
targeting the frozen index it actually cites — `[^sim-adr-NNNN]` →
`../notes/sim/ADRs.md`, `[^tools-adr-NNNN]` → `../notes/tools/ADRs.md`,
`../../` one level deeper for the generated use-case pages).
`components/corpus/docs/use-cases.edn`'s prose likewise; `make
use-cases` regenerated; `git diff --stat` confirmed only the same six
generated pages ADR-0101 touched moved again, `docs/use-cases.md`
(index) and `docs/cli.md` byte-identical at this step (not in the diff
at all). `bases/cli/src/ehrt/cli/help.clj`'s `play --sink` doc line
drops the `mllp:` claim (see mllp section, below); `make docsgen`
regenerated `docs/cli.md` (the one line moves, legitimately, at this
step only) plus `docs/dev/pipeline.md` and `docs/operators.md`
byte-identical (neither in the diff).

Gate green post-conversion, confirmed within the full local suite run
(596 occurrences of "0 failures, 0 errors", 0 FAIL/ERROR anywhere,
`exit 0`, 4 minutes 52 seconds) and directly via a targeted
`component:docs-tooling` re-run showing zero `no-visible-adr-token-in-
prose-test` failures.

Commit: `3880a6b` — "docs: user-path citations go marker-only, full
path, gate hardened (ADR-0102)."

#### Shape rules applied, mechanically

- **Lifts out cleanly → delete the token, keep the marker in place.**
  Since ADR-0101's own append-in-place convention put the marker
  immediately after the token with no separating space
  (`ADR-0010[^adr-0010]`), deleting just the token's own characters
  leaves the marker exactly where it was, adjacent to whatever
  punctuation already surrounded the token. Used wherever the citation
  sat as an aside — a parenthetical list item, a `Register:` line, a
  comma-separated citation.
- **Parenthetical citation shells collapse.** A `(...)` containing
  ONLY the citation (`(ADR-0013[^adr-0013])`, or the doc-name form
  `` (`notes/ADRs.md` ADR-0005[^adr-0005]) ``) removes the parens
  entirely — the marker attaches directly to the preceding text. Mixed
  parens (citation plus other content) keep their parens and only the
  token text is deleted.
- **Grammatically load-bearing → reworded minimally, generic
  referent.** Every site where the token itself was a sentence's
  subject/object/predicate (`"The reasoning is ADR-0010[^adr-0010]"`)
  or a possessive's antecedent (`"ADR-0014[^adr-0014]'s own Task 4"`)
  is listed in full below, before and after — this session's own
  reworded-sentences list is non-empty, unlike ADR-0101's (that session
  dodged the case entirely by keeping the visible token; marker-only
  reopens it).

### Reworded sentences, every one, before and after

1. `docs/formats.md` — before: *"The reasoning is ADR-0010[^adr-0010];
   this page does not restate it."* — after: *"The reasoning is in the
   design record[^adr-0010]; this page does not restate it."*
2. `docs/formats.md` — before: *"That distinction is
   ADR-0009[^adr-0009]; again, not restated here."* — after: *"That
   distinction is in the design record[^adr-0009]; again, not restated
   here."*
3. `docs/formats.md` — before: *"Semantics cited, never restated here:
   ADR-0009[^adr-0009] (judge vs. gate, and why the per-finding field is
   `:disposition` rather than `:policy`) and ADR-0010[^adr-0010] (the
   `:no-verdict` arm, its `:cause` channel, and its own exit code)."* —
   after: *"Semantics cited, never restated here: the judge/gate
   split[^adr-0009] (judge vs. gate, and why the per-finding field is
   `:disposition` rather than `:policy`) and the `:no-verdict`
   design[^adr-0010] (the `:no-verdict` arm, its `:cause` channel, and
   its own exit code)."*
4. `docs/judge-calibration.md` — before: *"This holds against baselines
   captured before ADR-0010[^adr-0010] too: an old, three-valued
   baseline..."* — after: *"This holds against baselines captured
   before that split landed[^adr-0010] too: an old, three-valued
   baseline..."*
5. `docs/locators.md` — before: *"...also pins `ehrt.corpus-io.er7` —
   ADR-0008[^adr-0008]'s own deviation record), which runs in the
   ordinary `make test`."* — after: *"...also pins
   `ehrt.corpus-io.er7` — the design record's own deviation
   note[^adr-0008]), which runs in the ordinary `make test`."*
6. `docs/site-profiles.md` — before: *"MSH-11 (processing id — post-M6,
   ADR-0014[^adr-0014]'s own Task 4 addition),"* — after: *"MSH-11
   (processing id — post-M6, the design record's own Task 4
   addition[^adr-0014]),"*
7. `docs/site-profiles.md` — before: *"...following directly from
   `sim/ADR-0002`'s separation of ground truth from wire format: a site
   profile changes how a fact is *said*..."* — after: *"...following
   directly from the design record's own separation of ground truth
   from wire format[^sim-adr-0002]: a site profile changes how a fact
   is *said*..."*
8. `docs/site-profiles.md` — before: *"...surface post-M6
   (ADR-0014[^adr-0014]'s own Task 4), the fourth dialect knob..."* —
   after: *"...surface post-M6 (the design record's own Task
   4[^adr-0014]), the fourth dialect knob..."*
9. `docs/site-profiles.md` — before: *"**A TEST-surname knob** (post-M6,
   ADR-0014[^adr-0014]'s own Task 4) — an optional site-profile
   field..."* — after: *"**A TEST-surname knob** (post-M6, the design
   record's own Task 4[^adr-0014]) — an optional site-profile
   field..."*
10. `components/corpus/docs/use-cases.edn` (case
    `play-a-generated-corpus-back-over-time`, propagating to the
    generated page of the same name) — before: *"...`ehrt play PATH` at
    an arbitrarily large --rate is exactly `ehrt show PATH`
    (ADR-0013[^adr-0013]/ADR-0014[^adr-0014]'s own identity)."* — after:
    *"...`ehrt play PATH` at an arbitrarily large --rate is exactly
    `ehrt show PATH` (the design record's own
    identity[^adr-0013][^adr-0014])."* — this is the driving prompt's
    own worked example, verbatim.
11. `components/corpus/docs/use-cases.edn` (case
    `piped-hl7-traffic-as-intake-source`, propagating to the generated
    page of the same name) — before: *"The spool's own
    capture-manifest.edn is a distinct schema from
    ADR-0014[^adr-0014]'s manifest.edn (captured-at/origin/framing/
    format/item-count/per-item sha256s, not a generator's provenance
    record)..."* — after: *"The spool's own capture-manifest.edn is a
    distinct schema from the design record's own
    manifest.edn[^adr-0014] (captured-at/origin/framing/format/
    item-count/per-item sha256s, not a generator's provenance
    record)..."*

Every technical claim in all eleven sentences is unchanged; only the
citation's own grammatical role moved from a bare `ADR-NNNN` noun
phrase to a generic referent carrying the footnote marker.

### The glossary anomaly closure (ADR-0101 items 1–2, revisit trigger fired)

ADR-0101's own Deviations named two pre-existing citation-accuracy
anomalies in `docs/glossary.md`, disclosed and left, with an explicit
revisit trigger: *"a future session auditing citation targets for
accuracy, not just citation form."* This session's own conversion of
those exact two sites is that trigger firing — footnoting a citation
requires choosing what its definition points at, and this session
would have manufactured a *fresh*, knowingly-wrong definition
(pointing `[^adr-0013]`/`[^adr-0006]` at `notes/ADRs.md`'s own
same-numbered, unrelated records) had it not corrected the target
while converting the form. Per each entry's own parenthetical ("tools'
pre-merge sequence") and ADR-0101's own cross-check against
`notes/tools/ADRs.md`, both retarget to the tools index:

1. **The Baseline entry**: `Register: `notes/ADRs.md` ADR-0013[^adr-0013]/ADR-0015[^adr-0015] (tools' pre-merge sequence, `notes/tools/ADRs.md`).` becomes `Register: `notes/ADRs.md`[^tools-adr-0013]/[^tools-adr-0015] (tools' pre-merge sequence, `notes/tools/ADRs.md`).` — `[^tools-adr-0013]`/`[^tools-adr-0015]` now define against `../notes/tools/ADRs.md`.
2. **The Pack entry**: `Retired mechanism (`notes/ADRs.md` ADR-0006[^adr-0006]-era, tools' pre-merge sequence)` becomes `Retired mechanism (`notes/ADRs.md`[^tools-adr-0006]-era, tools' pre-merge sequence)` — `[^tools-adr-0006]` now defines against `../notes/tools/ADRs.md`.

The prose describing each entry (including the now-slightly-orphaned
"`notes/ADRs.md`" text preceding the marker) is left exactly as
ADR-0101 left it — this session moves citation *targets*, per its own
license, not the surrounding content; the mismatch between the visible
doc-name mention and the definition's real target is the same
disclosed anomaly, now at least resolving to the correct record when
followed, rather than the wrong one.

### `:mllp` abandoned — the ruling and the three-place inventory

**Ruling, verbatim (2026-08-10): "Let's abandon `:mllp` for now."** No
transport work, no design-doc amendment. `bases/cli/src/ehrt/cli/
help.clj`'s `play --sink` doc line claimed *"dir:, blaze:, and mllp:
are recognized but deferred"* — a claim that was already false on its
own terms before this ruling: `mllp:` was never in the sink-URL
grammar (`ehrt.corpus.source-sink-url`'s six fixed schemes,
`docs/dev/source-sink-design.md` D-a), so passing `--sink mllp:...`
would parse `:unknown-sink-scheme`, not a recognized-but-rejected
scheme the way `dir:`/`blaze:` genuinely are. Fixed to name only
`dir:`/`blaze:` as recognized-deferred; `make docsgen` regenerated the
one corresponding line in `docs/cli.md`.

Three places carried mllp-transport-sink-adjacent language, inventoried
here as the ruling directs, none rewritten except the third:

1. **`docs/dev/source-sink-design.md` D2/D3** — D2: *"MLLP support is a
   framing codec only (pure bytes⇄messages functions); transport is
   `nc`'s job, no socket code enters this repo."* D3: sink kinds listed
   as *"`dir`/`file`/`stdout`(optionally MLLP-framed)/`blaze`"* — no
   `:mllp` sink kind ever appears in this design doc's own sink-kind
   enumeration; `mllp` there names a *framing* option on `stdout`/
   `dir`/`file` sinks, a mechanism that ships and is unrelated to a
   network-transport sink. Untouched (frozen-archives discipline —
   the design doc stands as written and governing; it was never
   actually wrong, and stays the authority this ruling defers to).
2. **`notes/adr/0014-corpus-player.md`** — names a **future** `:mllp`
   *sink kind* (distinct from the framing above) as explicitly deferred
   work: *"a future session building the bed board, wiring the sim
   accumulator into the player, or adding the `:mllp` sink inherits a
   tested pacer and two working sinks..."*, restated in ADR-0014's own
   2026-07-30 fulfillment note as one of the items *"remain[ing]
   exactly as deferred here."* This forward-looking "future work, to be
   built eventually" framing is now **superseded in part** by this
   session's ruling — the sink is abandoned, not merely still-deferred
   — but the record itself is NOT edited (frozen-archives discipline;
   this ADR-0102 entry, plus `.agents/rulings.md`'s law-surface
   propagation discipline, is where the supersession is recorded).
3. **`bases/cli/src/ehrt/cli/help.clj`'s `play --sink` doc line** — the
   one place actually carrying the now-doubly-wrong claim (both
   grammatically ungrounded, per above, and now ruled-abandoned besides)
   — fixed this session, the only one of the three places edited.

### Tag ceremony

Design channel verified the ADR-0101 landing (`062df94`) by fresh
public clone. `stable-20260810-adr-footnotes` tagged at `062df94`,
pushed, peeled ref confirmed `062df94b8da87da840442f7a4d0d0520cca2e5bb`
— exact match, remote had not moved (`origin/main` was already at
`062df94` at session start).

### Oracle bracket

Pure identity was the prediction (markdown, one docsgen EDN source, one
test file, one help.clj docstring line — no `src` namespace, no
oracle-path namespace touched). `bin/regression-oracle 062df94
3880a6b`: soundness check IDENTICAL outside `digest.clj`'s own
`(ns ...)` form; all 34 roots' SHA-256 digests IDENTICAL between
baseline and target. Matches the prediction exactly — no
STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all skip:integration`, unredirected capture): 596 occurrences of "0
failures, 0 errors" across the entire log, 0 FAIL/ERROR anywhere, exit
0, 4 minutes 52 seconds. `ehrt.cli.cli-parse-guard-lint-test` confirmed
green within that same run (line 1149 of the captured log — 4 tests, 22
assertions, 0 failures/errors). `bin/verify-nist-lock`: OK, 6/6.
`make lint-pipeline`: OK.

### Fences

Touched exactly the list the driving prompt named: the six
hand-authored docs, `docs/use-cases/*.md` (regenerated only, the same
six of twenty that moved under ADR-0101), `docs/cli.md` (regenerated,
the one help-line change), `components/corpus/docs/use-cases.edn`
(prose only), `bases/cli/src/ehrt/cli/help.clj` (the one `--sink` doc
line), `components/docs-tooling/test/ehrt/docs_tooling/
link_footnote_gate_test.clj`, `notes/adr/0102-*.md`, `notes/ADRs.md`,
`notes/adr/README.md`, `.agents/*` close-phase files. `docs/dev/`
untouched — `docs/dev/source-sink-design.md` and
`notes/adr/0014-corpus-player.md` stand exactly as written. No
generated page hand-edited (all six landed via `make use-cases`;
`docs/cli.md` via `make docsgen`). No `src` namespace changed anywhere
except the one docstring line in `help.clj`. `Makefile` untouched.

### Deviations, dated 2026-08-10

1. **The driving prompt's own "roadmap: no mllp row existed" claim was
   checked and found wrong** — see the dedicated section above. The
   Deferred row closes in place this session rather than being
   silently skipped per the prompt's own unverified premise; the
   prompt's own substantive instruction (record the ruling in
   `.agents/rulings.md`) still happened, alongside the correction.

No other deviations from the driving prompt's own steps, fences, or
rulings. The mechanical shape rules (delete-only / shell-collapse /
reword) applied above are the driving prompt's own [C] ruling worked
out site-by-site, not a departure from it.

### Consequence

Every footnoted citation in the user path (`docs/` proper) now reads
marker-only — no `ADR-NNNN` token left visible in prose anywhere the
new gate scans, bare or origin-qualified alike. The 12 origin-qualified
citations ADR-0101 left untouched now carry real, clickable footnotes
into the frozen pre-merge indexes they actually name. The 5
fence-comment citations stay bare, unchanged, for the same disclosed
reason ADR-0101 found. The gate's third check makes the "no ADR
clutter in user-facing prose" rule self-enforcing going forward — a
future append-in-place regression, or a new bare citation landing
without its footnote treatment, fails the build rather than waiting
for the next sweep to notice. The two `docs/glossary.md`
citation-accuracy anomalies ADR-0101 disclosed and deferred are closed:
their footnotes now resolve to the record they actually describe.
`:mllp` as a future sink is abandoned, its help-text overclaim
corrected, and the record of where the old "future work" framing still
lives (frozen, on purpose) is inventoried rather than silently
orphaned.

### A channel claim checked and corrected: the mllp Deferred row did exist

The driving prompt's own Context asserted *"roadmap: no mllp row
existed"* and directed the abandonment be recorded under
`.agents/rulings.md`'s own rulings section instead. Verify-then-act
(this workspace's own standing discipline, `.agents/rulings.md`'s
"transcript-witnessed is not repo-recorded" ruling) caught this wrong
before acting on it: `.agents/plans/roadmap.md`'s own Deferred section
already carries a **"Corpus player `:mllp` transport sink"** row,
landed at ADR-0014's own original deferral and still live. Since
`:mllp` is now abandoned rather than merely still-deferred, that row
CLOSES this session, per this file's own established closure pattern
(a dated note appended in place, pointing at the Done section's own
one-line pointer — the same shape ADR-0100's close used for the
`ehrt play` bare-reads row) — never silently left open, and never cut
out of the Deferred section the way the pre-AR-B-4 discipline once
would have. Disclosed here as a finding this session made and
corrected, not a deviation from the driving prompt taken lightly: the
prompt's own claim was unverified, checked against the live tree, and
found wrong.

### Index line

```
- 2026-08-10 — marker-only-footnotes — ADR-0102
```

(appended to `.agents/plans/roadmap.md`'s own Done section; the mllp
Deferred row closes in place with a dated note pointing at this same
pointer, per the correction above. `.agents/rulings.md`'s own new
"From ADR-0102" section also records the abandonment ruling and the
ADR-0014 supersession note, as the driving prompt directed.)

`notes/adr/README.md`'s own file count corrects 99→100, verified by `ls
notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

User-path citations go marker-only, full user path, gate hardened; `:mllp` sink abandoned — three author rulings execute: the append-in-place form ADR-0101 landed is superseded (visible `ADR-NNNN` token drops everywhere, marker alone remains), scope widens to the 12 origin-qualified citations ADR-0101 left untouched (first-time footnotes into the frozen `sim`/`tools` pre-merge indexes, distinctly-named markers), and the `:mllp` sink is abandoned for now; a re-derived inventory finds zero deltas against ADR-0101's own count; the gate gains a third check (no visible `ADR-NNNN` token in prose, fences and definition lines exempted) proven red on the pre-conversion tree (12 files) then green; eleven sentences reworded where the citation was grammatically load-bearing, every one listed before/after; the two `glossary.md` citation-accuracy anomalies ADR-0101 disclosed are closed (their footnotes now target the tools index they actually describe); a three-place mllp-language inventory is disclosed, only the help-text overclaim corrected, the design doc and ADR-0014 left standing as written; the oracle holds pure identity across all 34 roots

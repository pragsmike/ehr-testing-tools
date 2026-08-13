# 2026-08-12 — User manual S3: the transport pair, chapters 4-5 (ADR-0121)

## Scope

S3 of the five-session user-manual arc: land the "transport pair" --
chapter 4, time on the wire (`docs/manual/04-time-on-the-wire.md`) and
chapter 5, batch delivery (`docs/manual/05-batch-delivery.md`), the
arc's own FEATURED chapter per the author's own charter (`.agents/
rulings.md` "From ADR-0112"). Two content commits landed: `750de99`
(chapter 4) and `35ea29b` (chapter 5), plus this record's own
close-phase commit.

## Red->green evidence highlights

**A pre-existing, chartered flake surfaced and self-cleared, exactly
as its own standing license predicts.** `clojure -M:poly test :all
skip:integration`, run before commit 1's push, went red on
`ehrt.sim-engine.engine-test`'s own `mixed-authored-and-compiled-run-
satisfies-the-full-invariant-catalog` at an unpinned random seed
(`1786589996178`) -- NOT the chartered repro seed `7844068501` ADR-0114
R8 names. Re-run immediately after: green, same test passing at a
different random seed. `git status --porcelain` before either run
showed only `docs/manual/` files touched -- this session's own fence
never came near `sim-engine` `src`/`test` -- confirming the failure is
pre-existing and unrelated by construction. R8's own standing license
("a future session may run the defspec pinned at seed `7844068501`...
without needing a fresh ruling") covers exactly this symptom; no STOP
fired.

**Two self-caught process errors, both fixed before either commit was
staged.** (1) Both new chapters' first drafts, plus both new SVGs'
source comments, were over-corrected to strip ALL Unicode punctuation
to ASCII, mistakenly generalizing the driving prompt's own "ASCII x3"
gate language (which checks git commit MESSAGES, `git log
--format=%B`) onto manual body prose. Chapters 1-3 were re-checked
(`LC_ALL=C grep -c '[^ -~\t]'`, all three non-zero) and confirmed to
use real Unicode em-dashes throughout, matching `docs/dev/simulator-
architecture.md`'s own notation; both new chapters and both SVGs'
visible text were rewritten to match before either commit, literal
`--` preserved only in real CLI-flag syntax and the SVGs' own XML
comments (matching `gt-emitters.svg`'s established ASCII-comment
precedent). (2) The front page's own resequencing disclosure used a
bare `ADR-0112` token in prose, tripping `ehrt.docs-tooling.link-
footnote-gate-test`'s no-visible-ADR-token check; fixed by converting
to a footnote marker with a definition-line citation, the same pattern
`docs/use-cases/supply-batch-straddling-traffic.md`'s own
`[^adr-0111]` already uses. Both caught by this session's own pre-push
test run, before any push carried either version.

**Every strip in both chapters re-derived by fresh regeneration this
session, not merely copied at a distance.** Because this session's
fence is docs-and-registers-only (zero `src`/`test`/`demos`), every
excerpt is copied verbatim from `demos/scenarios/ed-tuesday/README.md`
-- but rather than trust that prior witnessing, this session re-ran
`bin/ehrt corpus generate sim` (base and latency configs) and `bin/ehrt
corpus batch`, seed 20260811, against its own tree (writing only to
gitignored `out/`), and compared every resulting value against the
README's own prose: the ground-truth-invariance digest, the full
34-batch listing including the interior gap, Walker's own EVN-2/MSH-7
pair, and Smith's own two MSH segments all matched byte-for-byte. No
divergence found anywhere.

**Oracle bracket:** `bin/regression-oracle 6c000aa 35ea29b` ->
`IDENTICAL: every root's digest matches`, all 35 roots -- matching the
pre-analysis (no oracle root's own `src` touched; only `docs/manual/*`
and registers).

## Judgment calls and their ratification status

- **Front-page resequencing -- a disclosed judgment call, not a
  ruling.** Pre-session, Chapter 7's own working title ("Realism you
  didn't script... in depth") described exactly what this session's
  Chapters 4-5 now deliver, two sessions early. Left untouched, the
  page would show that material under two chapter numbers at once.
  This session's own resolution: Mutate stays at Chapter 6, Gate at
  Chapter 7 (neither moved), Check's own topic folds into Chapter 8
  alongside verdict-reading at scale. The ratified eight-chapter,
  five-session shape (`.agents/rulings.md` "From ADR-0119" R-M1/R-M2)
  is fully preserved -- S4 still lands two chapters, S5 still lands
  one -- only which topics sit in the three still-proposed slots
  changed. The author may correct this reading; nothing here forecloses
  a different split.
- **The straddle's own mechanism, stated explicitly, is new content
  beyond the demo README's own narration.** Chapter 5 observes that
  Smith's own EVN-2 clinical times sit entirely inside `batch-000`'s
  window -- it's the discharge message's own sampled transmit delay
  (Chapter 4's own second clock) that carries MSH-7 across the
  boundary, not a long clinical encounter. The demo README states the
  MSH-7 values but doesn't connect them to EVN-2 this explicitly; this
  session's own fresh regeneration supplied the EVN-2 values used to
  make that connection, disclosed per-strip in the ADR as
  session-witnessed rather than README-sourced.
- **Strip-versus-figure sourcing for the "one message, two clocks"
  instance.** Chapter 4's figure uses Walker's own raw HL7 field values
  (`20260811043646+0000`/`20260811033600+0000`), independently grepped
  from this session's own fresh regeneration rather than quoted
  directly from a README fence (the README states the same instant in
  prose, ISO-formatted). Disclosed in the SVG's own source comment as
  cross-verified against the README's own witnessed run, not a new,
  unrelated claim.

**`docs/manual/00-front.md` landed in this close commit, not split
across the two content commits (a process note, not a design call).**
The driving prompt named `00-front.md` only in its own Fences list,
with no per-commit assignment (unlike S2's own prompt); all of its
edits were drafted before either content commit and, by oversight,
never staged into either -- surfacing only at close via `git status`.
Both content commits were already pushed by then, so landing the
front-page edits here rather than amending either is the fix-forward,
fully disclosed in `notes/adr/0121-manual-s3-transport-pair.md`'s own
Deviations section.

## Findings and HEAD landed

No discrepancies between the driving prompt's stated preflight premise
and the live tree: `origin/main` was at `6c000aa` exactly; the last
five CI runs were all green; every Read-first document matched its own
characterization; every `demos/scenarios/ed-tuesday/README.md` command
excerpted by either chapter ran exactly as documented when re-run this
session -- the STOP-AND-REPORT clause this session's own prompt named
for a README/tree divergence never fired.

The tag `stable-20260812-manual-s2` was created ANNOTATED at `6c000aa`
(this session's own Step 0), pushed, peeled ref verified exact.

**HEAD landed**: `750de99` (chapter 4), `35ea29b` (chapter 5), and
this record's own close-phase commit.

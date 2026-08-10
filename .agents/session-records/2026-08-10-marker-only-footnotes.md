# 2026-08-10 — User-path citations go marker-only, full path, gate hardened; `:mllp` abandoned (ADR-0102)

## Scope

Three author rulings, 2026-08-10, executed in one session: the
footnote FORM ADR-0101 landed (append-in-place, visible `ADR-NNNN`
token plus marker) is superseded by marker-only; the SCOPE widens from
ADR-0101's bare-only citations to the full user path, footnoting the
12 origin-qualified citations (`sim/ADR-NNNN`, `tools/ADR-NNNN`)
ADR-0101 explicitly left untouched; and the `:mllp` player sink is
abandoned for now, with a one-line help-text accuracy fix and a
three-place inventory of where the old deferral language still lives.
Two commits: the conversion itself (six hand-authored docs, six
generated pages via their EDN source, the hardened gate, help.clj's
one line), and this close-phase record/prompt-archive commit. This is
ADR-0102.

## Red→green evidence highlights

The gate's new third check (`no-visible-adr-token-in-prose-test`) ran
red on the pre-conversion tree (docs conversion stashed out, the new
test un-stashed) against all 12 in-scope files at once — captured
verbatim in ADR-0102, a non-vacuous red against the real tree, not a
synthetic fixture. Green again post-conversion, confirmed both within
the full local suite (596 occurrences of "0 failures, 0 errors", 0
FAIL/ERROR anywhere) and via a targeted `component:docs-tooling`
re-run.

`make use-cases` regeneration: `git diff --stat` confirmed the same
six generated pages ADR-0101 touched moved again, `docs/use-cases.md`
byte-identical. `make docsgen` regeneration: `docs/cli.md` moved by
exactly the one expected line (the `--sink` doc-line fix);
`docs/dev/pipeline.md` and `docs/operators.md` regenerated
byte-identical, neither in the diff.

**Full gate:** `clojure -M:poly check` OK; full local suite
(`clojure -M:poly test :all skip:integration`, unredirected capture)
596 occurrences of "0 failures, 0 errors" across the entire log, 0
FAIL/ERROR anywhere, exit 0, 4 minutes 52 seconds;
`ehrt.cli.cli-parse-guard-lint-test` confirmed green within that run
(line 1149 of the captured log); `bin/verify-nist-lock` OK, 6/6;
`make lint-pipeline` OK; oracle bracket (`062df94`→`3880a6b`) all 34
roots IDENTICAL, soundness check clean.

## Judgment calls and their ratification status

- **Three mechanical shape rules (delete-only / parenthetical-shell-
  collapse / reword-with-generic-referent), applied site by site.** Not
  separately ratified beyond the driving prompt's own [C] ruling, which
  named the three categories but not every individual site's
  classification — this session's own judgment call in applying them,
  disclosed via the full reworded-sentences before/after list (eleven
  sentences) so every non-mechanical choice is checkable.
- **The two `docs/glossary.md` citation-accuracy anomalies (Baseline,
  Pack) retargeted to `notes/tools/ADRs.md`.** Directly licensed by the
  driving prompt's own Context and [C] ruling as "the trigger firing"
  for ADR-0101's own named revisit condition — not an independent
  judgment call, an explicit instruction executed.
- **A driving-prompt channel claim checked and found wrong:** the
  prompt's Context asserted "roadmap: no mllp row existed." The live
  `.agents/plans/roadmap.md` Deferred section in fact already carried a
  "Corpus player `:mllp` transport sink" row from ADR-0014's own
  original deferral. Caught by verify-then-act before acting on the
  claim; the row closes in place this session (the same dated-note-
  plus-Done-pointer pattern ADR-0100's close used), rather than being
  silently skipped per the wrong premise. Disclosed in ADR-0102 as a
  finding, not absorbed quietly.
- **The footnote form, the scope widening, and the `:mllp` abandonment
  itself** — all three ruled directly by the author in this session's
  own driving prompt (verbatim, cited in `.agents/rulings.md`'s new
  "From ADR-0102" section), not judgment calls this session made
  unprompted.

## Findings and HEAD landed

**No defects found in shipped code.** This session's own footprint is
markdown, one docsgen EDN source, one test file, and one help.clj
docstring line — the oracle's pure-identity prediction held exactly
across all 34 roots.

**One incorrect channel claim found and corrected** (see above) — the
mllp Deferred row existed and needed an in-place closure, not a
silent no-op.

**Tag paid forward:** `stable-20260810-adr-footnotes` tagged at
`062df94` (Step 1, this session — the design channel's own verified
ADR-0101 landing, tag law case (i)), peeled ref verified exact match,
remote unmoved.

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.

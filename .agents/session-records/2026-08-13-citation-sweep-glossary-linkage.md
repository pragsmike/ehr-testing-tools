# 2026-08-13 — Manual-arc tag payment, glossary linkage, citation errata sweep (ADR-0126)

## Scope

Two backlog rows from the manual arc's own close (ADR-0125), paired
into one session per the design channel's own "b go" ruling: the
manual-review skill's own dimension-4 FAIL (glossary linkage) and the
citation errata sweep ADR-0125 chartered ("a, go"). Three commits
landed; this record is the third's own close-phase companion.

## Ceremony

`make ci-parity` (fresh clone, cold artifact cache): green, 535 tests /
0 failures / 0 errors, 4m31s. HEAD confirmed `c6d0257`. CI license
re-verified against `gh run list --limit 5 --branch main`: top row
`c6d0257`, `completed`/`success`, matching the driving prompt's own
citation (run `31717674233`) exactly.

Tag `stable-20260813-manual-arc-close` created ANNOTATED at `c6d0257`;
pushed; peeled ref confirmed `c6d0257149e14fbad96c42130231996fdb6c2000`
via `git ls-remote --tags origin`, exact.

## Commit 1 — glossary linkage

`docs/glossary.md` links added at first use of glossary-defined terms,
Chapters 1, 3-7 (Chapters 2, 8 already conformed). Every link a bare
`[term](../glossary.md)` page reference, matching the existing pattern
exactly — no restated definitions, no anchors added. Full per-chapter,
per-term table in `notes/adr/0126-citation-sweep-glossary-linkage.md`.

`link_footnote_gate_test.clj` read whole before editing: only its
first check (relative-link resolution) applies to these edits, since
every added link is a bare page reference with no footnote marker.

`00-front.md`'s own currency-commit convention checked directly: it
names the commit each chapter's *witnessed command output* was
captured against, not incidental prose edits — nothing here touches a
strip or a captured output, so it was read and left untouched.

`make test`: green (full suite, 535+ tests across every namespace, 0
failures/0 errors). Commit `0266bc4`, pushed. Post-push ASCII check
(`git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`): empty, clean.

## Commit 2 — citation errata sweep

**Inventory (Step 2a).** Two-pass repo-wide grep: case-sensitive for
bare `ADR-0010`, then case-insensitive for `[^adr-0010]`-style footnote
markers (the first pass alone misses footnote *usages*, since the
marker string doesn't contain the literal `ADR-0010` substring outside
its own definition line — this found `docs/formats.md`'s and
`docs/judge-calibration.md`'s own usage sites the case-sensitive pass
had missed). Every hit classified into (i) verdict-family, (ii)
documentation-doctrine, (iii) meta-mention/frozen-archive, plus a
fourth class this session found and named — sim-identity (bare
`ADR-0010` in `components/sim/docs/`/`components/sim-trajectory/docs/`
meaning the frozen sim repo's own `sim/ADR-0010`, patient identity —
17 sites, 6 files, disclosed, out of this session's own touch fence,
not fixed).

**Corrected against the channel's own probe.** `docs/glossary.md` was
named as a verdict-family site needing a footnote rename. Direct
inspection found its one `[^adr-0010]` usage (line 5) is genuinely
class (ii) — cites R38, the doc-audit/glossary-merge decision, not
verdicts. `docs/glossary.md` carries no verdict-family citation
anywhere in the live tree; left untouched, zero sites fixed there.
Recorded as a correction, not a silent deviation from the driving
prompt's own 2b list.

**Fixed, within fence:** `docs/judge-calibration.md` and
`docs/formats.md` (footnote form, `[^adr-0010]` renamed
`[^tools-adr-0010]`, definitions retargeted to `notes/tools/ADRs.md`);
`docs/manual/assets/verdict-ranking.svg` (comment preserved, citation
edited); `components/corpus/docs/palgebra-design.md` +
`research/judge-v2-nist-spike-notes.md` (bare text); `components/
corpus/docs/use-cases.edn` (footnote form, same rename), regenerating
`docs/use-cases/profile-tier-hl7v2-conformance-gating.md` via `make
use-cases` in the same commit; all thirteen `.clj` comment/docstring
sites the widened, author-licensed charter named.

**The special check.** `bases/cli/src/ehrt/cli/help.clj` read line by
line: its three verdict-family sites are docstring/comment only, never
the rendered `:meaning` strings (`exit-codes`/`gate-common-flags`
checked directly — neither literal string contains the token).
`help.clj:471` is class (ii) (`write-cli-md!`'s own docstring,
doc-doctrine) — a blanket sed touched it by accident on the first
pass; caught by a full re-grep before commit, reverted to bare
`ADR-0010`. `core_test.clj`'s two sites are `;;` comments; a repo-wide
`grep -rn "adr-0010" --include="*_test.clj"` count-lock probe found no
test locking on the literal string.

**Out-of-fence, disclosed, not touched:** `bin/ehrt:3`,
`notes/2026-07-30-refactoring-review.md:33`,
`test-fixtures/reports/pre-split-baseline.edn:2` (explicitly fenced
out). Every `notes/adr/`, `notes/tools/`, `notes/sim/`,
`.agents/prompts/`, `.agents/session-records/` hit is class (iii) or
frozen-archive, left untouched.

`make test`: green. `bin/regression-oracle c6d0257 a4203fa`:
`IDENTICAL: every root's digest matches` — the pure-identity prediction
confirmed directly, not merely asserted. Commit `a4203fa`, pushed.
Post-push ASCII check: empty, clean.

## Manual-review, dimension 4 only — targeted re-run

**PASS.** Every chapter (1, 3-7; 2 and 8 already conforming) now links
`../glossary.md` at first use of a glossary-defined term. Chapter 3's
own "Pathway"/"script space"/"truth space" — the glossary's own
front-matter-named most-common misreading — link at first use (lines
66, 95, 103). Dimension 2 (no restatement) and dimension 3 (anchor
stability) both re-verified incidentally: every added link is a single
bracketed word, none a table; every link a bare page reference, no
anchor to break. The other seven dimensions were NOT re-run this
session, per its own narrower charter. Dimension 1 (strip
executability) stays the open FAIL it was at ADR-0125's own close —
untouched, no exerciser/lint mechanism edited.

`make test` re-run before this commit's own push caught a real gate:
`ehrt.docs-tooling.index-completeness-test` failed on this commit's own
two new files (`.agents/prompts/README.md` and `.agents/session-
records/README.md` both missing the index entry for this session's own
prompt archive / session record) — fixed in place (one line each),
re-run green.

## Registers

`notes/adr/0126-citation-sweep-glossary-linkage.md` (full inventory,
classification, and per-site table); `notes/ADRs.md` index line;
`.agents/rulings.md` "From ADR-0126"; `.agents/plans/roadmap.md`
(citation-sweep row CLOSED, dimension-4 row CLOSED, ceremony-scripts
row now front of the Next-section queue); `.agents/state.md`
citation-only update (not an arc-close file, so
`state_staleness_tripwire_test.clj` is untouched by this addition).

Zero `src`/`test`/`demos` behavior touched anywhere — every code edit
is comment-or-docstring text, confirmed per-site and by the oracle
bracket. Zero widening beyond disclosure for the sim-identity family
and the three out-of-fence verdict-family sites.

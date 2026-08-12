# 2026-08-12 — Review-3, the user-surface review (ADR-0114)

## Scope

A REVIEW session in the review-2/UX-audit lineage: produces a findings
register, executes zero fixes (AR-RR2-1). Two author-licensed docs
riders land first (the §4 precision-clause parenthetical; a new
method-vocabulary section plus two glossary entries), then a
seven-battery (B1-B7) live probe of the `ehrt` CLI surface, chartered
by `.agents/rulings.md` "From ADR-0113" R5. Three content commits plus
this record's own close-phase commit: `9f7697a` (the two riders),
`d0679e9` (the findings register).

## Evidence highlights

**Every UX-audit carry-forward row re-probed with a fresh, live
command, not trusted from the 2026-08-06 register's own text.** Nine
of ten open items came back resolved or substantially resolved:
`bin/ehrt`'s bare-invocation exit code (was 2, now 0, matching `ehrt
help`); `ehrt sim`/`corpus`/`artifact`/`gate`'s no-verb hint (was
generic "run: ehrt help", now tailored to the named group); `sim run
--config <missing/malformed>` (was a raw JVM stack trace, now a clean
categorized Result); unknown flags (was silently absorbed, now a clean
`:unknown-flag` rejection); the stale `clojure -M:cli` invocation form
(all 11 originally-flagged file:line groups fixed, confirmed by the
B5 sub-agent's own fresh grep); every ADR/milestone/ruling-number
citation the UX audit found leaking into rendered help text (24 ADR
tokens, 14 milestone tags, 3 ruling citations — all now zero, confirmed
by grepping this session's own captured help transcripts, not the
source).

**Three new highest-priority findings, each captured live.**
`bin/ehrt check` with no args, a nonexistent directory, or a genuinely
empty directory all return `{:status :ok, :payload {:totals {:pass 0
...}, :files []}}` at exit 0 — indistinguishable from a real
zero-finding pass (R3-B2-1, the single worst finding in the register).
`bin/ehrt sim run --seed abc` and `--patients notanumber` crash with a
raw `babashka.cli`/`ExceptionInfo` stack trace pointing at a throwaway
temp-file report, at exit 1 (the exit-code table's own definition
calls this a `2`-class operational error) (R3-B2-2). `bin/ehrt corpus
intake <dir> --label test` with no `--out` crashes with a raw
`NullPointerException` four layers into `intake.clj` (R3-B2-3).

**A dedicated read-only sub-agent ran B5** (cross-doc agreement),
matching repo-review-2's own "parallel read-only sub-agents"
precedent: 41 files read in full (README Quickstart, all 21
`docs/use-cases/*.md`, the relocated `demos/**` tree, `docs/
simulate-your-facility.md`), finding the four surfaces this battery's
own charter named fully clean, plus 3 small drift instances in the
post-ADR-0073-relocation `demos/traces/**` config-header comments (a
gate scan-root blind spot, not a violation of any existing gate) and
one out-of-band stale-alias instance in `.github/ISSUE_TEMPLATE/
bug-report.md`.

**Oracle bracket.** `bin/regression-oracle ea4346c d0679e9`:
`IDENTICAL: every root's digest matches` — all 35 roots, exactly
matching the pre-analysis (riders are docs prose, the register a new
plans file, every live CLI execution during B2/B4/B7 wrote only to the
session scratchpad or one removed `out/corpus/` positive control).

**Full gate, run once, clean.** `make test`: `poly check` OK, 0
failures/0 errors throughout, including `ehrt.sim-engine.engine-test`
(the flake R8 charters an investigation for) running clean on this
session's own single pass — no re-run needed. `bin/verify-nist-lock`:
OK. `gitleaks`: no leaks, staged or pre-push, across all three commits.

## Judgment calls and their ratification status

- **The findings register's own summary table was drafted, checked,
  and corrected before landing — not after.** A first pass miscounted
  B1's own row total (8 instead of 9) and its ruling-needed column (2
  instead of 3). This session's own direct-recount discipline
  (matching AR-RR2-2's standing lesson, deliberately invoked rather
  than assumed) caught it before the register was committed; the
  committed version carries the corrected table with an explicit note
  naming the correction, rather than silently landing the wrong number
  or scrubbing the error's own trace. Not author-ratified separately —
  this is the exact self-check discipline the prior review's own
  precedent (`.agents/plans/2026-08-09-repo-review-findings.md`,
  AR-RR2-2) already establishes as standing practice.
- **B7's dual-purpose rows (a narration sentence that cites an
  already-tallied B1/B2/B3 finding) counted once, at their first
  appearance, not again at their B7 citation.** A deliberate choice to
  avoid inflating the register's own disposition tally with the same
  underlying defect counted twice under two different ids — stated
  explicitly in the register's own summary section rather than left
  for a reader to infer.
- **B3's and B4's dual-disposition rows (e.g. R3-B3-1, a `design-
  channel-draft` content half plus a `fix-session-candidate` mechanism
  half) tallied under their primary/content-side disposition**, the
  same convention the UX audit's own B-4 row established. Not a new
  convention this session invented.
- **The glossary collision check (Rider B) found no pre-existing
  headed entry for either "Oracle" or "Witness"** — confirmed by direct
  grep before writing, per the driving prompt's own STOP-AND-REPORT
  instruction for that case. No collision, no stop triggered.

## Findings and HEAD landed

No discrepancies found between the driving prompt's own pre-decided
content and the live tree that forced a STOP-AND-REPORT. Every
Read-first document matched the prompt's own characterization of it;
`bin/ehrt`'s live behavior on several UX-audit-carried findings had
genuinely moved since 2026-08-06 (see Evidence highlights above) —
disclosed and re-probed fresh rather than assumed unchanged from the
prior register's own text, exactly the discipline a carry-forward
subsection exists to enforce.

The tag `stable-20260812-sim-palgebra-unification` was created at
`ea4346c` (this session's own Step 0), peeled ref verified exact match,
remote unmoved (`git fetch` + `git rev-parse origin/main` confirmed
`ea4346c` at session start; the last five `main` CI runs were all
`completed`/`success`, matching the driving prompt's own disclosed
"no split license needed" tag-ceremony framing).

**HEAD landed**: `9f7697a` (the two riders), `d0679e9` (the findings
register), and this record's own close-phase commit.

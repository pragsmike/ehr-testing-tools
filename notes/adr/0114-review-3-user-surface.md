## ADR-0114 — Review-3, the user-surface review

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

`.agents/rulings.md` "From ADR-0113" R5, author verbatim, 2026-08-12:
*"Should we run a repo review before we start on the manual? It might
lead to tweaks to the CLI."* Sequence ruled (channel proposal, author
"Q3 a"): review-3, scoped as a USER-SURFACE review (verb/flag
consistency, error-message quality, help surface, enumerable-options
family, derived-out-dir conventions) -> CLI tweak sessions from its
findings -> the user manual design pass -> chapter sessions -> a
manual-review skill built at arc close. This review is the sequence's
first step: every finding it lands is something the manual would
otherwise have to apologize for (a caveat, an "except when," a
teaching sentence that needs a footnote) — the review-2 lineage's own
findings-only discipline (AR-RR2-1) applies here unmodified: this
session produces a register and executes zero fixes, however trivial.

Two small author-licensed docs riders rode this session, landing first
(Step 1, commit `9f7697a`): Rider A closed a strict-as-stated overclaim
`docs/dev/simulator-architecture.md` §4 made (the census memoization
atom ADR-0108 already allowlisted was left uncited by the "zero atoms"
sentence); Rider B recorded a method-vocabulary section (`docs/dev/
way-of-working.md`) and two glossary entries (`docs/glossary.md`,
Oracle/Witness) for terms the design channel and this workspace's own
sessions have converged on through repeated use, author ruling
verbatim: *"add those terms, they've been successful."*

### Tag ceremony

`git fetch` confirmed `origin/main` at `ea4346c`
(`ea4346c596ccba447f10f9f5f4a070c18dc5f43b`, ADR-0113 close) at session
start. License: tag-law case (i), FULLY EARNED, no split needed — the
design channel's own 2026-08-12 verification of the ADR-0113 landing
(fresh clone; lineage; ASCII x3; footprint exact to the fence;
zero-`src` diff re-deriving the oracle identity basis; all nine witness
citations re-read at path:line; rulings quotes verbatim; token sweep
correct with only in-quote survivors) covered every dimension
including CI, whose own API check saw `ea4346c completed success`.
`stable-20260812-sim-palgebra-unification` tagged ANNOTATED at
`ea4346c`; pushed; peeled ref confirmed
`ea4346c596ccba447f10f9f5f4a070c18dc5f43b` — exact match.

### Decision

**[A] The two riders.** `docs/dev/simulator-architecture.md`'s "The two
layers, instantiated" subsection gains one parenthetical: "...zero
atoms, refs, agents, or volatiles in the simulation path (modulo §3's
own disclosed exceptions) is exactly..." — nothing else in the
sentence or surrounding prose changed. `docs/dev/way-of-working.md`
gains a new "## 7. Method vocabulary" section (matching the doc's own
top-level heading convention, placed after its existing §6, its own
last section — there was no closing/appendix material to place it
before): two term families, evidence (oracle, oracle bracket, witness,
red-before-green, count lock, tripwire, lint/house sense) and process
(probe, landing, fence, charter, rider, ruling-vs-recommendation, arc,
fix-forward, move-don't-improve, seam), each with the pre-decided
definition tightened to the doc's own bulleted-list voice (matching
§5's own convention, since the doc uses no `###` subheadings
anywhere), closed with one pointer line to `palgebra-design.md`/§4 for
the separate palgebra vocabulary. `docs/glossary.md` gains two new
headed entries in their correct alphabetical position (Oracle, between
"OPO / donor management" and "Pack"; Witness, between "Warm-up" and
the "Organizations and upstream projects" section header) — neither
term had an existing headed entry (confirmed by direct grep before
writing; no STOP-AND-REPORT triggered).

**[A] The review (B1-B7).** Landed as
`.agents/plans/2026-08-12-review-3-user-surface-findings.md`. Method:
B1 (verb/flag consistency) and B6 (output-shape) by direct enumeration
and cross-reading of the live `cli-spec`
(`bases/cli/src/ehrt/cli/help.clj`) against captured live-command
evidence; B2 (error quality), B3 (help surface), B4 (filesystem
conventions), and B7 (the narration test) by direct, live `bin/ehrt`
execution under the session scratchpad (a temp dir outside the repo)
plus one `out/corpus/` positive-control directory removed immediately
after inspection; B5 (cross-doc agreement) by one dedicated read-only
Explore-typed sub-agent, matching repo-review-2's own "parallel
read-only sub-agents" precedent, full transcript preserved in this
session's own record. The UX audit
(`.agents/plans/2026-08-06-ux-audit-findings.md`) is this review's
named baseline; its ten still-open rows were re-probed fresh in their
own carry-forward subsection, U-row ids preserved.

**Headline results.** 48 tallied dispositions across the new battery
rows (27 close-as-fine, 12 fix-session-candidate, 3 ruling-needed, 4
design-channel-draft, 2 incomplete) plus the UX audit's own 11-row
carry-forward, of which **9 of 10 open items are resolved or
substantially resolved on fresh evidence** — none of it this session's
own doing (U1's stale-alias sweep, B-1/B-2's agent-speak-in-help-text
citations, B-5/D-1's exit-code divergence, B-6/D-3's generic hint,
C-1's raw `--config` crash, and C-4's silently-absorbed unknown flags
are all confirmed fixed live). Against that strong trend line, three
new, high-priority findings: **R3-B2-1**, the single worst finding in
the register — `ehrt check` returns a clean, all-zero, exit-0 "pass"
result when given no target, a nonexistent target, or a genuinely
empty one, indistinguishable from a real zero-finding run over a
judge whose whole purpose is an authoritative pass/fail; **R3-B2-2**,
malformed numeric flag values (`--seed abc`, `--patients notanumber`)
crash with a raw `babashka.cli` stack trace and a wrong exit code (1,
not the exit-code table's own 2 for "operational error"); **R3-B2-3**,
`corpus intake` without `--out` crashes with a raw
`NullPointerException` four layers into `intake.clj`, the same "raw
crash instead of a categorized Result" class C-1 was fixed for
elsewhere. **R3-B1-5** cross-cuts four verbs at once: "you forgot a
required flag" is classified `:missing-required-opt`/exit-2 in two
verbs and a verb-specific `:category`/exit-1 in three others — the
cheapest, highest-leverage fix in the register. Full register,
including every probe's captured evidence and the B7 narration table
(the battery this review's own charter, R5, exists to feed directly
into the manual), is in the register file itself, not reproduced here.

### Deviations, dated 2026-08-12

**One self-caught arithmetic error, corrected before landing.** The
findings register's own summary table was drafted once, miscounting
B1's own row total (8, not the true 9) and its ruling-needed column (2,
not the true 3) — caught by this session's own direct-recount pass
(the same discipline AR-RR2-2 names) before the register was
committed, not after. The committed register carries the corrected
table with a note naming the correction explicitly, per this
workspace's own fix-forward-with-disclosure convention (rather than
silently overwriting a wrong number with no trace). No other deviation
this session: every Read-first document matched this session's own
characterization of it; the glossary collision check (Rider B) found
neither term pre-existing, no STOP-AND-REPORT triggered.

**A brief interim CI-red window on commit 2, self-caught and closed
before this ADR's own push.** Commit 2 (`d0679e9`, the findings
register) pushed before this session's own local `make test` gate ran
in full — the driving prompt's own Gates section applies the full
gate once, before Step 3's push, the same "run once, clean" precedent
ADR-0113 set, not at every checkpoint. `make test`, first run at Step
3, caught two gaps the register's own new file introduced:
`notes/ADRs.md`'s own `done-pointer-adr-test` (the roadmap's Done line
cited ADR-0114 before `notes/ADRs.md`'s own index carried it — resolved
by writing this ADR before the roadmap Done line's own commit) and
`.agents/plans/README.md`'s own `index-completeness-test` (the new
register file needed its own index line, a fence surface this
session's own driving prompt did not explicitly name but which the
gate's own scope makes load-bearing). Both fixed locally before commit
3; `main`'s own CI (`gh run list`) shows `d0679e9`
(`31598555300`) `completed`/`failure` on exactly this gap, and
`aeb45ab` (`31599697988`, this session's own close-phase commit)
`completed`/`success` once both were fixed — confirmed by direct
`gh run view` on both runs, not assumed clean. No tree state at any
point contradicted a local `make test` run's own result; the gap was
between commit 2's own push and the session's own full-gate run, not a
false-green anywhere.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots — the riders are docs
prose (`docs/dev/simulator-architecture.md`, `docs/dev/
way-of-working.md`, `docs/glossary.md`), the register is a new plans
file, and every CLI execution during B2/B4/B7 wrote only under the
session scratchpad (a temp dir outside the repo) or one `out/corpus/`
positive-control directory (gitignored, removed after inspection) —
zero `src` anywhere.

**Bracket result.** `bin/regression-oracle ea4346c d0679e9` (`d0679e9`:
this session's own commit 2, the findings register, run before the
close-phase commit, per the driving prompt's own step ordering):
`IDENTICAL: every root's digest matches between ea4346c and d0679e9` —
all 35 roots, matching the pre-analysis exactly; no STOP-AND-REPORT
needed.

### Full gate

`make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration`): green — `poly check` OK; every namespace 0
failures/0 errors, including `ehrt.sim-engine.engine-test` (the
flake this session's own R8 charters an investigation for, ADR-0112's
disclosure), which ran clean on this session's own single pass, no
re-run needed. `bin/verify-nist-lock`: OK. `gitleaks git --staged -v`
(pre-commit, each checkpoint) and `gitleaks detect` (pre-push): no
leaks found, across all three commits. ASCII byte-check on all three
commit messages: clean.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): all `completed`/`success`, matching
the tag ceremony's own CI-clean disclosure above — no red among the
five.

### Fences

Touched: `docs/dev/simulator-architecture.md` (Rider A's one
parenthetical); `docs/dev/way-of-working.md` (Rider B's new §7);
`docs/glossary.md` (Rider B's two entries); `.agents/plans/
2026-08-12-review-3-user-surface-findings.md` (new); `.agents/plans/
roadmap.md`; `.agents/rulings.md` (the R8 entry); `.agents/prompts/*`
(self-archive plus its README index line); `.agents/session-records/*`
(this session's own record plus its README index line); `notes/adr/
0114-*.md` (this file); `notes/ADRs.md`; `notes/adr/README.md`. ZERO
changes under any `src/` or `test/` path anywhere; zero generated-doc
regeneration; zero fixes of any register finding, however trivial — no
file outside the list above was touched.

### Index line

```
- 2026-08-12 — review-3-user-surface — ADR-0114
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

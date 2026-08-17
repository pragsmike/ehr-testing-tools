## ADR-0134 — Manual-review run 2: report landed verbatim, three findings fixed

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-14.

### Context

This session ran a rider block drafted by the design channel against a
fresh public clone at `46b82ba` (ADR-0133's own close, tree clean).
The block was written to be spliced after a host session's own steps
and fences; the host never materialized (its own Q2 was open at draft
time, and the block was deliberately authored self-contained and
orderable last so a host STOP would leave it cleanly undone rather
than half-landed). With no host, this session IS the host — the
rider's own "add a rider section to the host ADR" resolves to this
record, author-ruled 2026-08-14 ("New ADR-0134") when the session put
the three dispositions to them.

The charter, author verbatim: *"Do a thorough review of this repo's
user manual, here in the design channel using this strong model
(Fable). It was recently authored and one manual review arc was run,
but I think that used the weaker model."* Run 1
(`.agents/plans/2026-08-13-manual-review-1.md`, ADR-0125) is the FAIL
baseline this run re-scores; its two FAILs were separately closed by
ADR-0126 (dimension 4) and ADR-0129 (dimension 1).

**The reviewer/actor split, ruled.** Asked whether one session may
both land a review report and fix its findings, the author ruled
*"Q1 a."* — the split is satisfied ACROSS channel and session rather
than across sessions: the channel reviewed, this session acts, the
report commit precedes every fix commit, and each fix commit cites its
report row. That is the same review-discipline the `manual-review`
skill states ("this skill produces register rows, never edits"), read
at the channel/session boundary instead of the session/session one.

### Step 0 — Ceremony and tag payment

`bin/preflight`: last five CI runs on `main` all green (`46b82bab`,
`0d32d205`, `69e16523`, `ded3569d`, `c3b6fbc2`); edit-root confirmed
ext4 (`/home/mg/src/ehr-testing-tools`, not `/mnt/*`); tree clean
including untracked; local HEAD matched `origin/main` at `46b82ba`;
last `stable-*` tag `stable-20260814-clinic-decade`, HEAD not tagged.

Tag `stable-20260814-exact-name` created ANNOTATED at
`46b82babf1e109f6a5748f175f8a687419a3ea3e` via `bin/tag-ceremony ...
--push`, licensed by the rider's own case (i). Receipts:

```
OK: created annotated tag 'stable-20260814-exact-name' at 46b82babf1e109f6a5748f175f8a687419a3ea3e
no leaks found
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260814-exact-name -> stable-20260814-exact-name
OK: pushed refs/tags/stable-20260814-exact-name
OK: remote peeled ref for 'stable-20260814-exact-name' is 46b82babf1e109f6a5748f175f8a687419a3ea3e, matches target exactly
```

**Disclosed substitution, author-ruled.** Step R0's own fence:
STOP-AND-REPORT before pushing if the author's CI check has not been
relayed into the session's prompt context. It had not been. What this
session had instead was its OWN `bin/preflight` run of the same
mechanism the rider names (`gh run list`), green at the exact target
SHA. The session stopped and reported rather than deciding for itself;
the author ruled *"Pay it, message verbatim"* — the tag text's
"author-side CI check (gh run list)" stands as written, and the fact
that the check performed was session-side is recorded here and in the
session record rather than in the tag. The provenance is disclosed at
the durable surface, not silently absorbed and not quietly edited out
of the tag.

### The report (Step R1)

`.agents/plans/2026-08-14-manual-review-2.md`, landed verbatim as the
channel authored it — not re-derived, re-graded, or re-worded by this
session. **Overall: PASS with warns**, no fail-grade dimension. Both
run-1 FAILs verified remediated STRUCTURALLY (dimension 1 by
ADR-0129's exercised-sources register and citation gate; dimension 4
by ADR-0126's first-use links), not by hand-witnessing.

Two scope facts the report discloses in its own preamble, restated
here because they bound what this ADR may be cited for: the run was
made by the design channel rather than by a session invoking the
`manual-review` skill (a disclosed runner deviation, chartered), and
NOTHING was re-executed — the channel sandbox cannot resolve Clojure
dependencies, so every witnessed output was checked for
source-consistency, internal arithmetic, and mechanism coverage, never
re-witnessed. Execution-tier confidence rests on the two-gate
exerciser mechanism and the green suite at `46b82ba`, not on this run.

Dimension 7 (currency) is the one grade that strengthened beyond
"unchanged": run 1 disclosed a 4-claim sample; run 2 records that
currency is TEST-GUARANTEED (`cli-md-is-current-test` plus CI's
regen+diff step), so a green tip is itself the currency proof, and
then checks eight specific claims on top of that.

Four findings beyond the rubric: **F1** erratum (fix-worthy), **F2**
warn (pedagogy), **F3** cosmetic, **F4** an affirmative record of
every cited test, numeric value, and section attribution that DID
check out — the last of these deliberately, so a future reader can
tell "verified correct" from "not looked at."

### The fixes (Steps R2-R4), each citing its report row

**F1 — Chapter 8's `ehrt check` elision comment** said ";; ... five
more patient files and both info files, all :pass ...", totalling
1+5+2 = 8 against the same block's own `:totals {:pass 7}`. Seven is
correct (a 5-patient Synthea run yields 5 patient bundles plus
`hospitalInformation` and `practitionerInformation`); the comment
miscounted. Fixed to "four more patient files."

The rider required this premise be VERIFIED rather than assumed
before editing: that no test hashes a manual output fence. Checked,
holds — `ehrt.docs-tooling.citation-gate` parses only "Strip source
citations" TABLES from `docs/manual/0*.md`
(`citation_gate.clj:119-125`), and `demo-exerciser-fresh` compares a
scenario README's fenced COMMANDS against a script's taught command
list, never the manual's own output blocks. A repo-wide grep for
`docs/manual` across `.clj`/`.edn`/`Makefile`/`.yml` returns only
those two mechanisms. The currency contract's
never-hand-edit-to-match rule is therefore not implicated: the elided
lines were never output, only a hand-composed comment describing them.

**F2 — the intake-8 vs gate-5 divergence.** Both Chapter 8 strips run
against `test-fixtures/v2`; `corpus intake` reports `:file-count 8`
while `gate v2` reports `:pass 5`, with no explanation on the page or
on either cited use-case page. Both are correct — intake walks every
regular file recursively (`intake.clj`'s own `source-files`), which
picks up `simhospital/{LICENSE, PROVENANCE.md, messages.out}` beside
the five `.hl7`; the gate takes only the `.hl7`. Verified directly
against the fixture directory (8 files, 5 of them `.hl7`). One
parenthetical sentence added in place, immediately after the intake
record's own output block — the channel's own recommended wording,
adopted under the author's "go", landed with no material difference.

**F3 — `docs/glossary.md`'s headword.** Chapter 8 links the phrase
"intake record"; the entry read `**Intake.**`. Widened to
`**Intake / intake record.**`. This was the rider's single sanctioned
move-don't-improve allowance (it edits outside `docs/manual/`),
explicitly droppable without disclosure debt; taken because it is one
line and closes the one cosmetic hair dimension 4 otherwise carries.

### Fences honored, and the one widening

Touched exactly: `docs/manual/08-your-own-data.md` (both fix sites),
`.agents/plans/2026-08-14-manual-review-2.md`, `docs/glossary.md`
(one headword), plus this ADR, its index line, `.agents/rulings.md`,
`.agents/plans/roadmap.md`, and the session record / prompt archive
pair. Zero `src/` changes; red-before-green N/A (docs-only).

**Widening, disclosed:** the rider's own touch fence named the report
file but not `.agents/plans/README.md`'s index line for it.
`ehrt.docs-tooling.index-completeness-test` fails the build on an
unindexed plans entry in BOTH directions, so the index line is a
mechanical consequence of landing the file rather than a second
change — added in the same commit and disclosed in that commit's own
message. The roadmap Done row and this ADR are likewise beyond the
rider's literal list, licensed by the author's ADR-0134 ruling, which
brings the standing close ceremony with it.

Commit order was load-bearing and held: report (`bf13e88`) strictly
before F1 (`0a74a4a`), F2 (`8e74936`), F3 (`49cd75a`).

### Verification

`make test` green at the final tree: `clojure -M:poly check` OK, full
suite clean — 632 `0 failures, 0 errors` blocks, matching ADR-0133's
own closing baseline exactly, and zero lines matching a non-zero
failure or error count anywhere in the run — plus `bin/verify-nist-
lock` OK on all 6 hit-nexus-sourced coordinates. The three gates the
rider named specifically — citation gate, strip-fresh parity, docsgen
— are all inside that lane and all green.

No regression-oracle claim is made by this session and none is owed:
zero `src`/`test`/`demos`/module-JSON touched, so no oracle root can
have moved.

### Disposition

Manual-review run 2: CLOSED, PASS with warns. F1 and F3 fixed, F2
disclosed in place. Dimension 5 (running-example continuity) stays
WARN for the same structural reason run 1 documented — `ed-tuesday`
is HL7v2-only and cannot supply Chapters 6-8 their FHIR mutation,
FHIR-gate calibration, or foreign-corpus material — unchanged, still
disclosed rather than silently substituted, and still not a defect
under the dimension's own reading. It remains the manual's one
standing open register row.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Manual-review run 2: report landed verbatim, three findings fixed — pays tag `stable-20260814-exact-name` at `46b82ba` (ADR-0133's own close), under a disclosed substitution the author ruled on directly: Step R0's fence required the AUTHOR's `gh run list` relayed into the prompt context, which it was not, so the session STOPped rather than deciding for itself; ruled "Pay it, message verbatim" on this session's own `bin/preflight` showing all five runs green at the exact target SHA, with the session-side provenance recorded in the ADR and session record rather than edited into the tag. The second scored run of the `manual-review` skill's own rubric, authored by the DESIGN CHANNEL against a fresh public clone rather than by a session invoking the skill (a disclosed runner deviation the author chartered verbatim: "Do a thorough review of this repo's user manual, here in the design channel using this strong model (Fable)"), and landed here verbatim -- not re-derived, re-graded, or re-worded. The reviewer/actor split, ruled "Q1 a.", is satisfied ACROSS channel and session rather than across sessions: the channel reviews, the session acts, the report commit strictly precedes every fix commit, each fix commit cites its report row. **Overall PASS with warns, no fail-grade dimension** — both run-1 FAILs verified remediated STRUCTURALLY (dimension 1 by ADR-0129's exercised-sources register and citation gate, dimension 4 by ADR-0126's first-use links), not by hand-witnessing; dimension 7 strengthens from run 1's disclosed 4-claim sample to test-guaranteed (`cli-md-is-current-test` plus CI's regen+diff step make a green tip itself the currency proof) plus eight claims checked on top; dimension 5 stays WARN for the structural reason run 1 documented (HL7v2-only `ed-tuesday` cannot supply Chapters 6-8 their FHIR material), the manual's one standing open register row. Disclosed scope limit, stated in the report's own preamble: NOTHING was re-executed -- the channel sandbox cannot resolve Clojure dependencies, so every witnessed output was checked for source-consistency, internal arithmetic, and mechanism coverage, never re-witnessed; execution-tier confidence rests on the two-gate exerciser mechanism and the green suite at `46b82ba`. Four findings beyond the rubric, the fourth deliberately affirmative (F4, every cited test/numeric/attribution that DID check out, so a future reader can tell "verified correct" from "not looked at"): **F1** fixed -- Chapter 8's `ehrt check` elision comment said "five more patient files," totalling 8 against the same block's own `:totals {:pass 7}`, corrected to "four" only after VERIFYING the rider's stated premise that no test hashes a manual output fence (it holds -- the citation gate parses only "Strip source citations" tables, the exerciser parity test compares scenario-README commands to script command lists, and a repo-wide grep finds no third reader), so the currency contract's never-hand-edit-to-match rule is not implicated; **F2** disclosed in place -- one parenthetical sentence explaining why `corpus intake` reports `:file-count 8` over `test-fixtures/v2` while `gate v2` reports `:pass 5` (intake walks every regular file recursively and picks up the three `simhospital/` sidecar files; the gate takes only the five `.hl7`), both correct, the undisclosed divergence having invited a false "did the gate skip files?" alarm; **F3** fixed -- `docs/glossary.md`'s headword widened to `**Intake / intake record.**` to cover the phrase Chapter 8 actually links, the rider's single sanctioned move-don't-improve allowance. One fence widening disclosed: `.agents/plans/README.md`'s index line for the report, a mechanical consequence of landing the file (`ehrt.docs-tooling.index-completeness-test` gates plans entries in both directions), added in the report's own commit and named in its message. Zero `src` touched anywhere, so no oracle root can have moved and no oracle claim is made or owed; `make test` green at the final tree

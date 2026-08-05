# 2026-08-05 — Docs coherence pass: gmf-interpreter consolidation, patient-state errata, arc riders

## Scope

The sim split B arc's own AR-M4-7 named-future, "the docs coherence
pass," executed per the design channel's own driving prompt (six
steps, AR-D-1 through AR-D-6). Two disciplines kept deliberately
separate throughout: consolidation (Steps 1-2, restructure-by-move,
content relocated verbatim, never rewritten) and errata (Step 3, the
errata-sweep skill's own citation-vs-instruction procedure). Step 0
characterized before any edit: verified tip `0986a86`; a full
section-header census of `gmf-interpreter.md` (16 numbered sections,
the split boundary at section 9); a repo-wide grep for inbound
links/anchors into the file (result: zero markdown anchor links exist
anywhere in the repo — every citation is a prose section number, never
a resolved hyperlink, so nothing broke in the link-resolution sense;
the genuinely broken references turned out to be internal, sections
1-8's own bare `§9`-`§16` self-citations, handled in Step 2); a fresh
zero-caller grep for `explain-profiles`; actual line counts for all
five reading sets (all five were exactly at their own budget, by
design — the zero-headroom seed); and a stale-claim candidate list for
`patient-state-model.md` (two candidates checked against live code,
both found NOT contradicted — see Findings).

Step 1 (AR-D-1) split `gmf-interpreter.md` (3,668 lines): sections 1-8
+ appendix + ratification record stay as the living reference (1,428
lines after adding the new pointer-index section); sections 9-16
(eight dated GMF-coverage wave sections, 2026-08-02 through
2026-08-04) moved VERBATIM to the new `gmf-interpreter-findings.md`
(2,280 lines, extraction diff-verified byte-identical against the
pre-split source). Step 2 (AR-D-1's in-place half) reviewed sections
1-8/appendix/ratification in full against every ADR from ADR-0027
through ADR-0042 and found the document ALREADY current almost
everywhere — a standing convention (dated notes appended in place at
the point a later wave superseded an earlier claim, demonstrated
throughout section 1's state-type table and section 3's own Wave H
note) had kept most of it in sync. Two genuine contradictions found
and fixed with inline dated notes (below), plus 25 internal `§9`-`§16`
citations the split itself broke, repointed to
`gmf-interpreter-findings.md`. Step 3 (AR-D-2) added two sections to
`patient-state-model.md` — the history phase (ADR-0042) and the
vital-sign register (ADR-0039) — explicitly disambiguated from the
document's own pre-existing "log is `Person.history` done right"
discussion, and ran the errata-sweep procedure over Step 0's candidate
list (both candidates NOT contradicted; see Findings). Step 4 (AR-D-3)
recomputed every reading set's actual line count and set each budget
to actual × 1.15, rounded up to the nearest 5, replacing 14
accumulated bump comments with one dated note. Step 5 (AR-D-4/5/6):
`explain-profiles` deleted (fresh-grepped zero callers, enforcing
AR-M4-5(a) as originally ruled over M4's own conservative deviation);
two dated notes appended to `notes/ADRs.md` ADR-0043's own tail
(façade docstring annotation ratified; parity-ledger counting
definitions disclosed, conservation verified under both).

## Red→green evidence highlights

Five content commits (`e6a0b28`, `ed84c8d`, `66c98f4`, `9e3709c`,
`2a94144`), `clojure -M:poly check` clean after every one. Full local
suite (`clojure -M:poly test`) run fresh after Step 1 (511/511
sim-trajectory-adjacent tests, the docs-only baseline) and again after
Step 5 (the session's only code-adjacent edit): 530 test groups, 0
failures/0 errors both times — this is a docs-only session's own
red→green shape: the suite stays green and untouched, never red, since
nothing here changes behavior except one dead def's removal.
`ehrt.docs-tooling.reading-set-budget-test`: 5 tests, 15 assertions, 0
failures after Step 4's re-baseline, and again after this record's own
`roadmap.md` growth self-caught the gate a final time (`:onboarding`
actual rose from 2090 to 2149 lines — still comfortably under its
fresh 2405-line budget, the 15% headroom absorbing it with room to
spare).

**Section 2 fixes (Step 2), each with an inline dated note citing the
superseding wave:**

| Claim | Was | Now | Cites |
|---|---|---|---|
| `lookup_table_transition`'s column resolution | "age range + a curated set of other recognized attribute columns" (H2's whitelist) | H2's whitelist RETIRED — any header column not `age`/`time`/a transition-state name resolves as an ordinary attribute (module-namespaced first, then persona-field, else honest absence) | Wave LC, `notes/ADRs.md` ADR-0038 |
| `Vital Sign`/`Active CarePlan` condition types | "stay OUT ... no accumulator or IR home exists for either yet" | Both landed — `Vital Sign` at Wave VS (reads the new register), `Active CarePlan` at Wave I2 (dispatched off `Logic.java`'s `ActiveLogic` parent) | ADR-0039 AR-1, ADR-0041 AR-2 |

**Errata-sweep accounting (Step 3), one-to-one per the skill's own
Done-when checklist:**

| Candidate | Classification | Disposition |
|---|---|---|
| "The accumulator...does not carry its own visit history...the log is `Person.history` done right" | Re-verified against live code and the new mechanism's own semantics — describes GMF `PriorState` guard compilation (a query), unrelated to the new `:phase :history` mechanism (a temporal walk phase) despite the shared word "history" | NOT contradicted, left as written; the new History Phase section opens by explicitly disambiguating the two, per the skill's "reword only when a caveat is still needed" default |
| `:attributes` M5b-scope note ("the engine's own accumulator still doesn't populate this field until M5b") | Re-verified against `engine.clj`'s own `PatientState` docstring (line 154: "`:attributes` remains reserved, unused") — still true today | NOT contradicted, left as written |
| M6 accumulator fields (`:discharged-at`/`:conditions`/`:observations`/`:medication-orders`/`:care-plans`/`:merged` status) absent from this document | Genuine documentation gap, but an OMISSION, not a contradicted claim — the document never asserted these don't exist, it predates them | Out of AR-D-2's own scope (not history/vital-sign, not a stale claim to fix); disclosed as a named future below, not fixed |

**Regression oracle** (`bin/regression-oracle 0986a86 2a94144`): all
ELEVEN vendored-root batches byte-identical, expected — zero code
changes affect any producer (the one src edit, `explain-profiles`'s
deletion, had zero callers):

```
89bc2090fa783481e152b2e7a364f407d6332ece6baba71abd1a8008d0686c2d  appendicitis.edn
28087e14d3692bc460182eca9475e4bc3e820b388eeee701368cc88c9fbf8602  death-fixture.edn
5a631475998e505c7edaf902c60bfa519ce171a4e673ae9e99a1eb2687742303  ear-infections-engine.edn
37885c6635918975be76abb37e9b662ebef7858ffefd883b3b4f5a6046b34af4  ear-infections-history-engine.edn
6ad02f827a66def26b5cd87e7c64fea2f48dd4fb782aaaf70fe6cfb10f1721ed  ear-infections.edn
f0b8160db59e3177f2b24cde589c53ca97fc98566a211769e1e0d58d29af74b3  sepsis.edn
e9931b60be52fe16257618141c6ac9c0a9e24a3d4fd8741c7c31316704885531  sinusitis.edn
b451881e86dd066a743e7eb0a6c257def4e2bcbcd4d925a5613a6f9e38e0daa9  sore-throat.edn
818bff1c424cbba98810696eac003a638bc3f87e92d261ecd45c050ee70cb103  total-joint-replacement-engine.edn
97bece7c0d659a6cf47a64544d9884e029dcd453785e48707174cd55872e04b0  urinary-tract-infections-engine.edn
ecc49eb4d6d632f09be24b563aabb4dd1c7dcd1736e91928edaf76726d3534d3  urinary-tract-infections-history-engine.edn
```
`IDENTICAL: every root's digest matches between 0986a86 and 2a94144`.

**Deftest parity and the façade seam**: trivially unchanged — `git
diff --stat 0986a86..2a94144 -- '*test*'` is empty (zero test files
touched this session, so no counting-definition ambiguity to resolve
— every deftest/defspec everywhere is byte-identical, not merely
count-identical); `git diff 0986a86..2a94144 --
components/sim/src/ehrt/sim/interface.clj` is empty (the façade file
itself untouched). Confirmed by diffstat, not merely asserted, per
`build-session/SKILL.md`'s own VERIFICATION discipline.

## Judgment calls and their ratification status

- **Repointing sections 1-8's own bare `§9`-`§16` self-citations**
  (Step 2, folded into the Step 2 commit rather than Step 1's): AR-D-1
  named repointing "what the move breaks" as part of the split step,
  but the breakage here is internal to the document's own prose
  (self-references that used to resolve locally, now cross-file), not
  an external inbound link Step 1's own census could catch before
  editing. Discovered only after Step 1's commit had already landed
  and pushed, while drafting Step 2's currency-pass citations. Folded
  into Step 2 rather than amending the pushed Step 1 commit (R6/R30's
  own "never amend a pushed commit" discipline) — disclosed here for
  review, not silently absorbed.
- **Section 4's encounter-class mapping table, left unedited (a
  FINDING, not a fix):** the table's own row ("the vocabulary the
  three surveyed modules' `Encounter` states use directly") is
  explicitly scoped to three original modules, so Wave I's later
  vocabulary completion (ADR-0040 AR-1b: `urgent-care`/`hospice`/
  `home`/`snf`) does not contradict it — it is incomplete relative to
  current interpreter capability, not stale. Per the session prompt's
  own fence ("a judgment call beyond citing a wave's explicit finding
  is a FINDING for the record, not an edit"), left as written.
- **`explain-profiles` disclosure form** (AR-D-4): the ruling offered
  "a dated disclosure comment at the site... OR in the commit message
  + ADR line." Chose the ADR-line form — no comment left at the
  deletion site, since nothing calls the function and a "removed"
  comment is dead weight a future reader gains nothing from. This
  commit and ADR-0043's own new tail note are the disclosure.
- **`roadmap.md` updated** (Step 6, not a named AR in the driving
  prompt): the docs-coherence-pass Deferred row (added by the M4
  session, per `AR-M4-7`) is annotated EXECUTED and a new Done section
  added, matching every prior session's own established convention in
  this file (visible throughout its own Done-section history) — not
  explicitly required by this session's own six steps, done for
  consistency with the workspace's standing practice.

## Findings (disclosed, not fixed — out of this session's own scope)

- **The M6 accumulator-field gap in `patient-state-model.md`**
  (`:discharged-at`/`:conditions`/`:observations`/
  `:medication-orders`/`:care-plans`/`:merged` status, all landed in
  `engine.clj`'s own `PatientState`, none documented here): a real,
  substantial documentation gap, but not history/vital-sign related
  and not a contradicted claim — AR-D-2 named exactly two additive
  sections, not a full M6 documentation pass. A future session's call.
- **Section 4's encounter-class table incompleteness** re: Wave I's
  vocabulary completion (see Judgment calls, above) — named here again
  as the accounting this record's own errata-sweep-adjacent review
  surfaced, not fixed.

**HEAD landed:** `2a94144` before this record's own commit; this
record and its paired prompt archive land as the final commit of the
session.

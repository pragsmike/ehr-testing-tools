# 2026-08-14 -- Manual-review run 2 landed verbatim plus F1/F2/F3 fixes; ADR-0133 tag paid

Decision-of-record: `notes/adr/0134-manual-review-2.md` (ADR-0134).
Report-of-record: `.agents/plans/2026-08-14-manual-review-2.md`.
Ceremony mode: **R30** (commit and push at each checkpoint,
unattended) — the standing default, and the driving rider stated no
prepare-only exception.

## What this session was

A rider block drafted by the design channel, authored to be spliced
after a host session's own steps and fences. The host never
materialized (its own Q2 was open at draft time; the block was
deliberately written self-contained and orderable last so a host STOP
would leave it cleanly undone rather than half-landed). With no host,
this session ran the rider as the whole prompt.

## Step 0 -- preflight, disclosed in full

```
-- 1. Last five CI runs on main --
  green  46b82bab  2026-08-14T19:39:17Z  docs: exact-name resolution close ...
  green  0d32d205  2026-08-14T19:19:31Z  test: oracle re-baseline per declaration ...
  green  69e16523  2026-08-14T19:09:45Z  test: re-baseline three pinned trajectory-content counters (ADR-0133)
  green  ded3569d  2026-08-14T15:07:44Z  docs: exact-name resolution census and declared-oracle-change predict...
  green  c3b6fbc2  2026-08-14T13:44:38Z  docs: session record and prompt archive -- clinic-decade rename ...
OK: last five runs all green (or none found)
OK: repo root '/home/mg/src/ehr-testing-tools' is not under /mnt/
OK: working tree clean, including untracked files
OK: local HEAD (46b82babf1e109f6a5748f175f8a687419a3ea3e) matches origin/main
Last stable-* tag: stable-20260814-clinic-decade (c3b6fbc252dd8f923b8e78d5f60713215d87087d)
DISCLOSED: HEAD is not currently tagged stable-*
```

No red or FINDING line. The one DISCLOSED line (HEAD untagged) is
exactly what Step R0 existed to resolve.

### The R0 STOP, and its ruling

Step R0's own fence: *"If the author's CI check has not been relayed
into this session's prompt context, STOP-AND-REPORT before pushing the
tag -- the license is conditional on it."* It had not been. The rider
text asserts an author-side `gh run list` inside the tag MESSAGE, but
asserting it in the message the session is about to write is not the
same as the result having been relayed, so the fence fired.

What the session had instead: its own `bin/preflight` run of the same
mechanism the rider names, green at the exact target SHA (`46b82bab`,
above). The session stopped and put the choice to the author rather
than deciding for itself -- writing a justification for proceeding
would have been the fabrication near-miss ADR-0128 names.

**Ruled: "Pay it, message verbatim."** The tag text's "author-side CI
check (gh run list)" stands as the channel wrote it; the fact that the
check actually performed was session-side is recorded here and in
ADR-0134 rather than edited into the tag. The disclosure lands at the
durable surface, not silently absorbed.

### Tag receipts

`bin/tag-ceremony stable-20260814-exact-name
46b82babf1e109f6a5748f175f8a687419a3ea3e <message-file> --push`:

```
OK: created annotated tag 'stable-20260814-exact-name' at 46b82babf1e109f6a5748f175f8a687419a3ea3e
no leaks found
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260814-exact-name -> stable-20260814-exact-name
OK: pushed refs/tags/stable-20260814-exact-name
OK: remote peeled ref for 'stable-20260814-exact-name' is 46b82babf1e109f6a5748f175f8a687419a3ea3e, matches target exactly
```

Peeled-ref verify against the exact SHA: the receipt the rider asked
for. The ADR-0124 skipped-tag class does not recur here.

## Step R1 -- the report, landed verbatim (`bf13e88`)

`.agents/plans/2026-08-14-manual-review-2.md`, byte-for-byte as the
channel authored it between its own BEGIN/END markers. Not re-derived,
re-graded, or re-worded -- the ruling ("Q1 a.") makes the channel the
reviewer and this session the actor, so re-scoring here would have
collapsed the split the ruling exists to preserve.

Overall **PASS with warns**, no fail-grade dimension. Full evidence
table in the report; the dispositions are summarized in ADR-0134.

**Fence widening, disclosed.** The rider's touch list named the report
file but not `.agents/plans/README.md`'s index line for it.
`ehrt.docs-tooling.index-completeness-test` gates plans entries in
both directions (presence and ghost), so an unindexed report file
fails `make test` -- the index line is a mechanical consequence of
landing the file, not a second change. Added in the same commit and
named in that commit's own message.

## Step R2 -- F1 (`0a74a4a`)

Chapter 8's `ehrt check` output block elides with ";; ... five more
patient files and both info files, all :pass ...". 1 shown + 5 + 2 = 8,
against the same block's own `:totals {:pass 7}`. Seven is right; the
comment miscounted. Changed "five" -> "four".

**The rider's checked premise, verified rather than assumed:** no test
hashes a manual output fence.

- `ehrt.docs-tooling.citation-gate` reads only "Strip source
  citations" TABLES out of `docs/manual/0*.md`
  (`citation_gate.clj:119-125`, `manual-citations`); its test asserts
  per-chapter citation COUNTS (08 -> 3), never fence bytes.
- `ehrt.docs-tooling.demo-exerciser-fresh` compares a scenario
  README's fenced COMMAND lines against a script's taught command
  list. It never reads `docs/manual/` at all.
- Repo-wide: `grep -rn "08-your-own-data\|docs/manual" --include=*.clj
  --include=*.edn --include=Makefile --include=*.yml .` returns hits
  only in `citation_gate.clj`, `citation_gate_test.clj`, and
  `exercised-sources.edn`'s own comment. No third reader exists.

So the premise holds and no STOP was owed. The currency contract's
never-hand-edit-to-match rule is not implicated either: the elided
lines were never output; only the hand-composed comment describing
them was wrong.

## Step R3 -- F2 (`8e74936`)

One parenthetical sentence added immediately after the intake record's
output block, explaining why `corpus intake` reports `:file-count 8`
over `test-fixtures/v2` while the chapter's later gates report
`:pass 5`. Verified directly before landing:

```
test-fixtures/v2/adt-a01-admit-repeated-identifiers.hl7
test-fixtures/v2/adt-a01-admit.hl7
test-fixtures/v2/adt-a02-transfer.hl7
test-fixtures/v2/adt-a03-discharge.hl7
test-fixtures/v2/adt-a08-update-trailing-empty-fields.hl7
test-fixtures/v2/simhospital/LICENSE
test-fixtures/v2/simhospital/PROVENANCE.md
test-fixtures/v2/simhospital/messages.out
```

Eight regular files, five of them `.hl7` -- matching
`intake.clj`'s own `source-files` ("Every regular file under dir,
recursively"). Both numbers correct; only the divergence was
undisclosed.

The channel-recommended wording landed with no material difference
from what Step R3 quoted, so its own STOP-AND-REPORT condition never
fired. One sentence, nothing restated from a reference doc (dimension
2 untouched), no strip or citation table edited.

## Step R4 -- F3 (`49cd75a`)

`docs/glossary.md`'s `**Intake.**` -> `**Intake / intake record.**`,
covering the phrase Chapter 8 actually links. The rider's single
sanctioned move-don't-improve allowance, explicitly droppable; taken
because it is one line. Checked first that no lint reads glossary
headwords (`grep -rn "glossary" --include=*.clj components/ bases/`
returns nothing), so no gate constrains the wording.

## The absent host, ruled

"Add a rider section to the host ADR" had no host. Put to the author
alongside the R0 question; **ruled "New ADR-0134"** -- a standalone
numbered record, matching how run 1 and both of its remediations
landed (ADR-0125/0126/0129). That ruling brings the standing close
ceremony with it, which is why this session also touched
`.agents/plans/roadmap.md` (one narrative CLOSED row plus one Done
one-liner) and `.agents/rulings.md` beyond the rider's literal list.

## Verification

`make test` at the final tree: `clojure -M:poly check` OK, full suite
clean, `bin/verify-nist-lock` OK on all 6 hit-nexus-sourced
coordinates.

Counted rather than eyeballed, both directions: 632 `0 failures, 0
errors` blocks in the run (matching ADR-0133's own closing baseline
exactly) and zero lines matching a non-zero failure or error count
(`grep -cE "[1-9][0-9]* (failures|errors)"` -> 0). Tail:

```
Ran 204 tests containing 536 assertions.
0 failures, 0 errors.

Test results: 536 passes, 0 failures, 0 errors.

Execution time: 13 minutes 34 seconds
bin/verify-nist-lock
OK: 6 hit-nexus-sourced coordinate(s) match artifacts.lock.edn exactly
```

The three gates the rider named specifically -- citation gate,
strip-fresh parity, docsgen -- are all inside that lane and all green.

**No regression-oracle claim is made and none is owed:** zero
`src`/`test`/`demos`/module-JSON touched, so no oracle root can have
moved. Naming the weaker method rather than implying the stronger one
(ADR-0030 J2).

Red-before-green: N/A, docs-only, no enforcement test added or edited.

## Commits

| commit | step | what |
|---|---|---|
| `bf13e88` | R1 | report landed verbatim + its plans-README index line |
| `0a74a4a` | R2 | F1, Chapter 8 elision arithmetic |
| `8e74936` | R3 | F2, intake-8 vs gate-5 disclosure |
| `49cd75a` | R4 | F3, glossary headword |
| (close) | -- | ADR-0134, index line, rulings, roadmap, this record, prompt archive |

Order was load-bearing and held: the report commit strictly precedes
all three fix commits, and each fix commit cites its report row.

## Post-push verification

See the close-out receipts appended below at push time.

## Carried forward

- **Dimension 5 (running-example continuity) stays WARN** -- the
  manual's one standing open register row. `ed-tuesday` is HL7v2-only
  and structurally cannot supply Chapters 6-8 their FHIR mutation,
  FHIR-gate calibration, or foreign-corpus material. Disclosed, not
  silently substituted; not a defect under the dimension's own
  reading. Closing it would need a second running scenario that emits
  FHIR, which is a design question, not an errata fix.
- **The report was not execution-verified.** Its own preamble
  discloses this and so does ADR-0134: the channel sandbox cannot
  resolve Clojure dependencies, so nothing was re-executed. A future
  run that CAN execute should re-witness rather than inherit run 2's
  witnessed-output checks.

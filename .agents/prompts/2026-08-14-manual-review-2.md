# Archived prompt: manual-review-2 (2026-08-14)

Original driving prompt below, verbatim. It is a RIDER BLOCK, drafted
by the design channel to be spliced after a host session's own steps
and fences -- the host never materialized (its own Q2 was open at
draft time), so this block was the entire prompt and the acting
session became the host. Executed as ADR-0134: all four steps landed
as scoped, plus two questions put back to the author rather than
decided in-session.

**Step R0's own fence fired.** It required the author's CI check to
have been relayed into the session's prompt context before the tag
could be pushed; it had not been. The session STOPped and reported,
disclosing that its own `bin/preflight` had run the same mechanism
(`gh run list`) and found all five runs green at the exact target SHA.
Ruled *"Pay it, message verbatim"* -- tag text unchanged, session-side
provenance recorded in ADR-0134 and the session record instead.

**"Add a rider section to the host ADR" had no host.** Also put to the
author; ruled *"New ADR-0134"*, which brought the standing close
ceremony (roadmap row, rulings, session record, prompt archive) with
it -- the reason this session touched `roadmap.md` and `rulings.md`
beyond the block's own literal touch list. One further widening,
disclosed in its own commit message: `.agents/plans/README.md`'s index
line for the report, a mechanical consequence of landing the file
(`ehrt.docs-tooling.index-completeness-test` gates plans entries in
both directions).

Step R2's "checked premise, verify do not assume" was verified and
holds (no test hashes a manual output fence); Step R3's landed wording
matched what the block quoted, so its own STOP condition never fired;
Step R4, the optional allowance, was taken. See
`notes/adr/0134-manual-review-2.md` and
`.agents/session-records/2026-08-14-manual-review-2.md`.

---

# RIDER BLOCK — manual-review run 2: report + errata (splice after the host session's own steps and fences)

Drafted by the design channel, 2026-08-14, against a fresh public clone
at `46b82ba` (ADR-0133 close, tree clean). Host session: TBD by author
(Q2 open at draft time) — this block is self-contained and orderable
last, so a host STOP leaves it cleanly undone, never half-landed.

## Author rulings, verbatim (this arc)

- Charter: "Do a thorough review of this repo's user manual, here in
  the design channel using this strong model (Fable). It was recently
  authored and one manual review arc was run, but I think that used
  the weaker model."
- Q1 (reviewer/actor split for same-session report+fix): "Q1 a." —
  the split is satisfied across channel/session: the channel reviewed,
  this session acts; the report commit precedes every fix commit, and
  each fix commit cites its report row.
- Q3 (tag slug): "go" — default accepted:
  `stable-20260814-exact-name` at
  `46b82babf1e109f6a5748f175f8a687419a3ea3e`.
- R3 wording below is channel-recommended, adopted under the same
  "go"; STOP-AND-REPORT if the landed text would need to differ
  materially from what Step R3 quotes.

## Read first (rider scope only)

- `.agents/skills/manual-review/SKILL.md` (the rubric this report
  follows; note its report path/increment convention)
- `.agents/plans/2026-08-13-manual-review-1.md` (run 1, the FAIL
  baseline this run re-scores)
- `docs/manual/08-your-own-data.md` (both fix sites)
- `components/docs-tooling/test/ehrt/docs_tooling/citation_gate_test.clj`
  and `.../demo_exerciser_fresh_test.clj` (the gates that must stay
  green; neither hashes a manual output fence — checked premise, see
  R2)

## Step R0 — Tag payment (fold into the host's own Step 0)

Pay the ADR-0133 close tag, deferred at that session's own close
pending channel verification (now done: fresh public clone, this
channel, 2026-08-14 — all seven commits present, substance
spot-checked, tree clean at tip).

1. Write the annotated-tag message to a file (message-via-file, ASCII
   only), content exactly:
   ```
   ADR-0133 close: exact-name state resolution, restoration cascade,
   oracle bracket 4-movers-as-predicted. Tag paid at the following
   session's Step 0 under license case (i): design-channel fresh-clone
   verification 2026-08-14 plus author-side CI check (gh run list),
   per the standing rate-limit accommodation.
   ```
2. `bin/tag-ceremony stable-20260814-exact-name 46b82babf1e109f6a5748f175f8a687419a3ea3e <message-file> --push`
3. The script's own peeled-ref verify against the exact SHA is the
   receipt; record it in the session record's Step 0 section. If the
   author's CI check has not been relayed into this session's prompt
   context, STOP-AND-REPORT before pushing the tag — the license is
   conditional on it.

## Step R1 — Land the review report, before any fix

Create `.agents/plans/2026-08-14-manual-review-2.md` with EXACTLY the
content between the BEGIN/END markers below (verbatim; do not
re-derive, re-grade, or re-word — this is the channel's own scored
run, landed as the finding-of-record).

Commit (message-via-file):

```
docs: manual-review run 2 -- all dimensions scored, both run-1 FAILs
verified remediated, two new findings (F1 erratum, F2 warn)

Report authored by the design channel (Fable, fresh public clone at
46b82ba, 2026-08-14); landed verbatim by this session per the
reviewer/actor ruling ("Q1 a."): channel reviews, session acts,
report precedes fixes.
```

(The report payload the block carried between its BEGIN/END markers is
not duplicated here — it landed verbatim and lives at
`.agents/plans/2026-08-14-manual-review-2.md`, which is the
finding-of-record. This is the one deviation from verbatim archival in
this file, taken to keep one copy of the report authoritative rather
than two that can drift.)

## Step R2 — F1 fix (one word)

In `docs/manual/08-your-own-data.md`, change the elision comment

```
;; ... five more patient files and both info files, all :pass ...
```

to

```
;; ... four more patient files and both info files, all :pass ...
```

Checked premise (verify, do not assume): no test hashes a manual
output fence — the citation gate maps command strips to register
rows, and the parity tests compare script taught-commands to source
fences, never the manual's own output blocks. If any test does read
this block's bytes, STOP-AND-REPORT.

Commit (message-via-file):

```
docs: fix ch8 check-output elision arithmetic -- four patient files,
not five (manual-review-2 F1)

Edits a hand-composed elision comment, not witnessed bytes: the
elided lines were never output, and the comment describing them
miscounted (1 shown + 4 more + 2 info = 7, matching the block's own
:totals). The currency contract's never-hand-edit-to-match rule is
not implicated.
```

## Step R3 — F2 fix (one sentence)

In `docs/manual/08-your-own-data.md`, immediately after the
intake-record output block (the `{:origin "acme-delivery" ...}`
fence), insert one parenthetical paragraph, exactly:

```
(`:file-count 8` counts every regular file under the directory,
recursively — the five `.hl7` messages plus the simhospital sidecar's
three provenance files; the gates later in this chapter take only the
five `.hl7`, which is why their totals read 5.)
```

Constraints: one sentence, nothing restated from a reference doc
(dimension 2), no strip or citation table touched. STOP-AND-REPORT if
the landed text would need to differ materially.

Commit (message-via-file):

```
docs: disclose ch8 intake-8 vs gate-5 count divergence in place
(manual-review-2 F2)
```

## Step R4 — F3 fix (OPTIONAL — the block's one sanctioned improvement, droppable without disclosure debt)

In `docs/glossary.md`, change the entry headword `**Intake.**` to
`**Intake / intake record.**` (one line; outside `docs/manual/`, so
this is the rider's single move-don't-improve allowance — drop it
rather than expand it).

Commit (message-via-file):

```
docs: glossary headword covers "intake record" as linked from ch8
(manual-review-2 F3)
```

## Rider fences

- Touch ONLY: `docs/manual/08-your-own-data.md`,
  `.agents/plans/2026-08-14-manual-review-2.md`, optionally
  `docs/glossary.md`, plus the host ADR's rider section,
  `.agents/rulings.md`, and the session record.
- Zero `src/` changes; red-before-green N/A (docs-only), but the
  citation gate, strip-fresh parity tests, and docsgen tests MUST
  stay green; full `make test` before push (R30).
- Commit order is load-bearing: R1 (report) strictly before R2-R4.
- Records: add a rider section to the host ADR summarizing this block
  (report landed, F1-F3 dispositions); append rulings rows to
  `.agents/rulings.md` quoting the three author rulings verbatim as
  listed at the top of this block.

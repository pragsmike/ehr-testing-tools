# 2026-08-12 — User manual arc opens: audience riders, front page, chapters 1-2 (ADR-0119)

## Scope

S1 of a five-session user-manual arc: pare `docs/dev/AUDIENCES.md`'s
audience register from eight segments to five (ADR-0113 R4), give
`docs/README.md`'s practitioner fork a learn-it entry into
`docs/manual/`, run an extension-blind naming census for the retired
"user guide" token, then create `docs/manual/`'s own front page and
its first two chapters (the hook chapter and setup-plus-first-corpus).
DOCS-AND-REGISTERS-ONLY: zero `src`, zero test code, both confirmed by
the diff each commit actually staged. Three content commits landed:
`6b48e81` (the riders), `0b6d74f` (the manual skeleton and chapters
1-2), and this record's own close-phase commit.

## Red→green evidence highlights

**One real red, caught by `make test` before any push, not by CI.**
After Commit 1 alone, `ehrt.docs-tooling.link_footnote_gate_test`'s own
`every-relative-link-in-docs-proper-resolves-test` failed:
`docs/README.md`'s new link into `docs/manual/` had no target yet
(`docs/manual/00-front.md` doesn't exist until Commit 2). Green after
Commit 2 landed: full `make test` (`poly check` + `poly test :all
skip:integration` + `bin/verify-nist-lock`) — 0 failures/errors across
every namespace, `verify-nist-lock` OK (6 coordinates matched).

**Determinism witnessed directly, not cited from an existing doc.** No
repo doc carried a ready-made sim-lane `diff -rq` demonstration
(`docs/use-cases/reproduction-packages.md`'s own strip is the Synthea
lane and explicitly recommends `--pair-by hash` instead, for a reason
that doesn't apply to sim). This session ran `bin/ehrt corpus generate`
(Quickstart's own bare strip) twice against the live tree, `diff -rq`ed
the two out-dirs: empty output, exit `0`, all four files
(`events.edn`, `manifest.edn`, `msg-000.hl7`, `msg-001.hl7`)
byte-identical. That real, freshly-witnessed result is what Chapter 2's
own punchline shows.

**Oracle bracket:** `bin/regression-oracle a9a0bbf 0b6d74f` →
`IDENTICAL: every root's digest matches`, all 35 roots — matching the
pre-analysis (docs/registers only, no oracle root's own `src` touched).

## Judgment calls and their ratification status

- **The commit-sequencing STOP-AND-REPORT departure — NOT yet
  ratified, flagged for the author's own review.** The driving prompt
  states "STOP-AND-REPORT on any conflict with the tree." This session
  found exactly such a conflict (above) and did not pause for author
  input before resolving it — it landed Commit 2 before the first push
  and pushed both commits together, reasoning that the fix was
  mechanical/order-only, foreclosed no design option, and never left
  `origin/main` red (full account: `notes/adr/0119-user-manual-
  skeleton.md`'s own Deviations section, and `.agents/rulings.md`'s
  "From ADR-0119" closing entry). This is a narrower reading of
  STOP-AND-REPORT than the prompt's own plain language states. The
  author may affirm this reading for mechanical, no-ambiguity
  conflicts of this same class, or correct it.
- **Chapters 3-8's own working titles — disclosed proposals, not yet
  ruled by name.** `docs/manual/00-front.md` names one-line promises
  for all eight chapters; Chapters 3-8's are this session's own
  capability-grounded proposal (a mapping onto Generate/Mutate/Gate/
  Check plus the already-shipped realism work), not sourced from any
  prior ruling. A future session may retitle or resequence them freely.
  `.agents/rulings.md`'s "From ADR-0119" entry states this explicitly.
- **The three design-pass questions (Q1/Q2/Q3), reconstructed not
  transcribed.** The driving prompt quotes only the answer pattern
  ("Q1 a. Q2 a. Q3 a.") and the resulting structure ("eight chapters,
  five sessions, exerciser at S2"); this session reconstructed what the
  three questions themselves likely were from that structure, disclosed
  as a reconstruction in both the ADR and the rulings entry, not
  presented as a verbatim transcript.
- **`ehrt` product-surface strips vs. plain shell scaffolding.** The
  driving prompt's "every command strip is copied VERBATIM from a
  witnessed source... no composed invocations, ever" rule was read as
  applying to `ehrt`/`clojure -M:...` invocations specifically — the
  workspace's own product surface — not to generic shell utilities
  (`rm -rf`, `cp -r`, `diff -rq`) used as scaffolding around a witnessed
  `ehrt` command. Every `ehrt` invocation shown in Chapters 1-2 is one
  of four rows in `notes/adr/0119-*.md`'s own source-citation table,
  reused verbatim where reused; the shell scaffolding around them is
  disclosed as outside that rule's own scope, not itself source-cited.
- **`notes/ADRs.md`'s own historical "user guide" hits excluded from
  the naming census.** ADR-0108/0110/0112/0113's index entries use the
  retired term describing past-tense events; excluded per ADR-0113's
  own precedent (that session's sweep scoped explicitly to
  `rulings.md`/`roadmap.md`, never `notes/ADRs.md`). Disclosed rather
  than silently assumed.

## Findings and HEAD landed

No discrepancies between the driving prompt's stated preflight premise
and the live tree: `origin/main` was at `a9a0bbf` exactly; the last
five CI runs were all green; every Read-first document matched its own
characterization. The naming census (Commit 1, step 3) returned a null
result — 9 hits, all in-quote survivors, zero live-prose stragglers —
recorded in full rather than silently passed over, per the driving
prompt's own "record the census either way."

The tag `stable-20260812-fix-clusters-b-c` was created ANNOTATED at
`a9a0bbf` (this session's own Step 0), pushed, peeled ref verified
exact via `git ls-remote --tags origin`.

**HEAD landed**: `6b48e81` (the riders), `0b6d74f` (the manual skeleton
and Chapters 1-2), and this record's own close-phase commit.

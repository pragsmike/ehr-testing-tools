# 2026-07-27 — Part B (`ehr-testing-sim`, run AFTER Part A is pushed): shrink the family block to a pointer; repoint the crosswalk

## Context

`ehr-testing-tools` now carries the authoritative `docs/GLOSSARY.md`
(its ADR-0017; its F30 records that this repo's "Conformance & gating
vocabulary" block drifted to pre-R3 verdict semantics —
`:indeterminate` described with what is now
`no-verdict(:terminology-suppressed)`, and the mandatory `:cause`
pairing missing). Per this block's own standing disclaimer ("tools'
own code and ADRs are authoritative if any detail here drifts"), this
session shrinks the block to a pointer plus the minimal local
one-liners, with a dated note per fix-forward discipline — the pointer
IS the correction; no pre-R3 text survives. Three crosswalk references
are repointed for family terms.

## Read first

* `docs/GLOSSARY.md` — §"Conformance & gating vocabulary" (the
  block), the "Diagnosis" colliding-terms entry, and the crosswalk
  mentions at the block's preamble and near line 585
* `docs/README.md` (crosswalk mention, ~line 210)
* Tools' `docs/GLOSSARY.md` on the public repo (the pointer target —
  confirm it exists and note its exact URL and heading anchors)
* `notes/ADRs.md` ADR-0001 (boundary), ADR-0014 (no acceptance
  instruments) — context only, unchanged

## Author rulings

1. The block shrinks; it does not vanish. Keep the section heading.
   Its body becomes: (a) a dated note (2026-07-27) stating the
   definitions moved to tools' authoritative glossary, why (the drift
   tools' F30 records — one sentence, no self-flagellation, fix-forward
   register), and the link; (b) at most four one-line local glosses for
   the terms sim's own docs use mid-sentence — gate, findings, verdict,
   baseline — each ending "see tools' glossary," each too short to
   drift (no enumerations, no semantics of arms or causes).
2. No pre-R3 text survives anywhere in this repo. The full Judge /
   Verdict / Error / Findings / Report / Baseline / Gate definitions
   are deleted from the block, not corrected in place — correcting
   them here would recreate the second authoritative copy.
3. Diagnosis entry stays, halved. The clinical half (DG1, ICD-10-CM,
   generated content) is sim's and remains; the judge-side ruling
   ("never a judge's explanation — that is findings") compresses to
   one sentence pointing at tools' glossary entry.
4. Crosswalk references (GLOSSARY block preamble, GLOSSARY ~585,
   `docs/README.md` ~210): family conformance terms now reconcile
   against tools' glossary; the guide crosswalk remains planned for
   method/teaching terms. Adjust wording minimally at all three sites;
   do not expand the crosswalk plan itself.
5. No ADR here. The block's own disclaimer pre-authorized exactly this
   move; the dated note in the glossary is the record. If the agent
   finds this repo's conventions demand an ADR anyway (AUTHORS-GUIDE
   says so explicitly), STOP and report rather than authoring one
   unprompted.
6. Sweep before closing: grep this repo for `indeterminate`,
   `no-verdict`, `verdict` outside the glossary — any other doc
   teaching verdict-arm semantics (rather than merely using a term
   with a pointer available) is a finding for the report, not an
   in-session fix.

## Steps

### Step 1 — Shrink the block

Per rulings 1–2. Verify the tools glossary URL resolves (fetch it; if
Part A has not been pushed, STOP).

Commit: `docs: family conformance vocabulary now points at tools'
authoritative glossary (their ADR-0017); local one-liners only`

### Step 2 — Diagnosis entry and crosswalk references

Per rulings 3–4, all four edit sites.

Commit: `docs: diagnosis entry halved to sim's clinical side;
crosswalk repointed for family terms`

### Step 3 — Sweep, verify, archive

Ruling 6's sweep, classified in the report. Repo's own fast suite
green (docs-only, but run it). Archive this prompt to
`.agents/prompts/archive/` with deviation appendix if any. Push per
the session-end ritual.

Commit: `prompts: archive 2026-07-27 glossary-pointer session (part
B)`

## Final report

The four one-liners as landed, all crosswalk-site diffs summarized,
sweep findings (docs still teaching verdict semantics, if any),
deviations.

---

## Deviation-record appendix (Part B, this repo, as actually run 2026-07-27)

- **ADR number shift, inherited from Part A.** Part A's own session
  found `ADR-0017` already taken in `ehr-testing-tools` (by an earlier
  same-day session, "Formal Source and Sink types") and used
  `ADR-0018` instead — recorded in tools' own archived Part A prompt's
  deviation appendix. Every mention of "tools' ADR-0017" in this
  prompt body (Context, ruling 5, Step 1's commit-message template) is
  read as **ADR-0018** throughout this session's actual commits. The
  prompt body above archives unedited, as issued, per the
  research-doc-errata-style convention this repo and tools share
  (tools' `AUTHORS-GUIDE.md` §7).
- **Step 1 — URL verification.** Confirmed via `curl -s -o /dev/null
  -w '%{http_code}\n' https://raw.githubusercontent.com/pragsmike/ehr-testing-tools/main/docs/GLOSSARY.md`
  → `200`, after Part A's push (`ehr-testing-tools` commit `404cacf`).
  Part A had already been pushed at the time this session ran, so the
  ruling-1 STOP condition did not fire.
- **Step 1 — commit granularity.** The three-hunk diff to
  `docs/GLOSSARY.md` (Diagnosis edit, conformance-block shrink,
  sibling-repositories crosswalk sentence) was split at hunk
  granularity with `git add -p` so Step 1's commit carries only the
  conformance-block shrink (ruling 1–2's scope) and Step 2's commit
  carries the Diagnosis halving plus both crosswalk-site edits
  (`docs/GLOSSARY.md`'s sibling-repositories entry and
  `docs/README.md`'s constellation paragraph) together, matching the
  prompt's own two-commit plan exactly.
- **Step 3 — sweep (ruling 6).** `grep -rn -i 'indeterminate\|no-verdict\|verdict'` across
  tracked `*.md` files, excluding `docs/GLOSSARY.md` and
  `.agents/prompts/archive/`, returned nine hits across
  `.agents/plans/roadmap.md`, `docs/event-sourcing.md`,
  `docs/gmf-interpreter.md`, `docs/README.md` (this session's own
  pointer edit), `docs/sim-theory.md`, `docs/site-profiles.md`,
  `notes/ADRs.md` (×2), and `README.md`. Each was read in context:
  none re-teaches the judge's four-arm verdict semantics
  (`:pass`/`:rejected`/`:indeterminate`/`:no-verdict`) — they use
  "verdict" for unrelated senses (the emitter-coherence property's
  "invariant verdict" in `event-sourcing.md`/`sim-theory.md`,
  `check.clj`'s own pass/fail verdict in `site-profiles.md`, a
  module-expressibility triage column in `gmf-interpreter.md`, a
  Blaze-server verdict in `notes/ADRs.md`, and generic references to
  "the gate's verdict" in `roadmap.md`/`README.md` that don't enumerate
  the arms). No further fix commit was needed; this is a clean sweep
  reported per ruling 6, not silently absorbed.
- **No ADR was authored.** Ruling 5's condition did not fire —
  `AUTHORS-GUIDE.md`'s ADR-rules section states the general rule (never
  silently revert an Accepted decision) and does not specifically
  demand an ADR for a documentation-pointer shrink pre-authorized by
  the shrunk block's own disclaimer.

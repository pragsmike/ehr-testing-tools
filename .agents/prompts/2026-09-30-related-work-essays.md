# Session: related-work essays -- lineage anchor + Synthetic Hospital (docs-only)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-related-work-essays.md`, which
> records that the prompt's "summarize command still unbuilt" check held
> only in part: `ehrt sim describe` (ADR-0183, 2026-09-29) already covers
> the log half of the census the essay leaned on.

Context. The author has placed two uncommitted draft files at
docs/related-work/README.md (lineage: Synthea, Google Simulated Hospital,
Synthetic Hospital, and what this workspace does not intend) and
docs/related-work/synthetic-hospital.md (an essay on arXiv 2609.30027).
They were drafted outside this tree by the design channel. Your job:
land them, tie them into the consumer docs, and correct any claim the
tree contradicts. Docs-only: no source, config or test changes.

Read first:
- docs/related-work/README.md and docs/related-work/synthetic-hospital.md (whole)
- docs/future-features.md:1-53 (framing), :113-135 (Scale ergonomics
  opener and the summarize entry), :195-208 (closing section, footnotes)
- docs/what-is-this.md:116-183 (Constraints, Validation, Scope)
- components/sim/docs/third-party-sources.md (whole)
- AGENTS.md de-scaffold paragraph (docs-only diffs; ASCII commit law)

Author rulings, verbatim:
- "For now let's just plan to record it in the repo in the form of an
  essay about related work that might inform future direction, without
  making any promises."
- "We looked to those for inspiration and ideas (and lifted some modules
  from Synthea). We describe those projects, and this paper, more as an
  anchoring reference to show where our intent lies, and what we don't
  intend."
- "Rule A." (= essays under docs/related-work/; a new future-features
  section linking to them)

Steps:
0. Pull. Another session was finishing when this was drafted; start from
   its landed tip. Gate: clean tree apart from the two drafts; tip CI green.
1. Check every claim the drafts make about this tree, and correct any
   that are wrong in place. Keep the substance and voice; do not expand
   the essays. At minimum: the vendored Synthea module location and
   status; the existence and status of the GMF interpreter port; the
   `corpus generate synthea` lane; "design only, no code or data" for
   Simulated Hospital; site profiles landed; the summarize command
   still unbuilt; no model in the generation path; replay over time
   existing. Invariant: no claim of a capability the tree lacks, and no
   new figures. Gate: list each check and its tree citation in the
   session record.
2. In future-features.md, add `## Fidelity evidence` after Scale
   ergonomics and before "What is not on this menu". Open it the way
   Scale ergonomics opens (not a fault class). Add one entry, "Measuring
   how real the traffic looks": consumer-voiced, one-line design stance,
   a *Today:* line, and a link to related-work/synthetic-hospital.md. No
   internal sizing, no dates. Invariant: the menu-not-plan framing
   holds. Gate: section order as stated; the link resolves.
3. Tie-ins, one line each: (a) what-is-this.md, near the Scope section,
   points to related-work/README.md for lineage and non-intent;
   (b) third-party-sources.md points to it as "the argument this record
   supports"; (c) docs/README.md gets a line only if it already
   enumerates doc pages. Invariant: the scope fence text is unchanged.
   Gate: every added link resolves. Stale text you notice in
   third-party-sources.md (e.g. the interpreter's status) gets one line
   in the session record, not an edit.
4. Any ADR or design-record reference in docs/ prose is a marker-only
   footnote, `Design record [ADR-NNNN](../../notes/ADRs.md).` Gate: the
   docs-tooling gates (link_footnote_gate, stale_path, invocation_lint)
   pass, locally if cheap, otherwise in CI.
5. Commit with an ASCII-only message and push. Write the session record
   in .agents/session-records/ and add its index line (the index is
   gated both directions). Close on `gh run view` showing CI green at the
   pushed tip.

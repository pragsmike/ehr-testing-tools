# Session: the prime audience -- ground-truth QA teams, documented and routed (2026-09-04)

Archived verbatim, as issued. Record:
[`../session-records/2026-09-04-prime-audience.md`](../session-records/2026-09-04-prime-audience.md).

---

Author ruling (2026-09-04): teams that consume the ground-truth event
log as a semantic oracle for QA of their own downstream system are
this workspace's PRIME audience. Documentation serves them first; the
features they need are prominent and easy to discover and use. No
feature is removed or deprecated by this ruling. The in-tree witness
for this actor is test-fixtures/downstream-calibration/PROVENANCE.md;
their report is channel-held -- cite the fixture, quote nothing else.
Docs payload plus one exerciser script and one generated-surface src
edit, both named below. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
docs/dev/AUDIENCES.md (header; segments 4, 6; the grow-note
convention); docs/README.md; docs/what-is-this.md ## Audience;
components/corpus/docs/{use-cases.edn,pipeline.edn} (the edn header's
"docs/pipeline.edn" means the latter); bin/usecase-custom-emitter and
its exercised-sources.edn row (the shape a use-case exerciser takes);
docs/consuming-ground-truth.md :1-28 and its taught commands;
bases/cli/src/ehrt/cli/help.clj (sim run --format :doc); the
audience_entry_path, usecases, exercised_sources (row pin :38),
invocation-lint and link-footnote tests; .agents/reading-sets.edn.

Rulings, binding:
- R-prime: as above. Prominence = FIRST in every list this session
  touches; no existing segment renumbered, folded, or demoted.
- R-segment: AUDIENCES.md grows to seven; segment 7 declared prime in
  the preamble with a dated grow-note in the register's convention;
  session record, not an ADR.
- R-generated: docs/cli.md only via help.clj :doc plus `make cli-doc`;
  docs/use-cases.md only via use-cases.edn plus `make use-cases`;
  :flags vectors untouched.

Steps:
1. Register. Segment 7: runs `sim run --format ground-truth`; derives
   invariants over patients, encounters, appointments, beds from the
   world model; retains corpora as versioned QA assets with the
   provenance tuple; `sim check` as reference judge, `sim mutate` for
   controlled negatives; runs under automation. Entry path:
   consuming-ground-truth.md, then formats.md "Read the top-level
   vector only", then future-features.md#scale-ergonomics. Header
   6 -> 7. Gate: audience_entry_path test green.
   Commit: docs: audience register -- segment 7, the prime audience
2. Routing. docs/README.md gains a segment-7 section FIRST after
   "I don't know what this is yet"; what-is-this.md ## Audience gains
   the bullet FIRST; root README.md gains one short paragraph near its
   top (who, and where they start; no fences). Gate: link-footnote +
   stale-path tests. Commit: docs: the prime audience routed first
3. Use case in use-cases.edn, FIRST in :start-here ("I need a semantic
   ground truth to check my system's behaviour against"): audience per
   segment 7; :bring a system defined over those entities plus a seed;
   :get the versioned world model, guarantees and exclusions, check
   and mutate; :commands the ground-truth invocation and the check
   pipe as consuming-ground-truth.md teaches them (small --patients);
   :equations in pipeline.edn's stage names; :maturity :usable; :note
   citing the fixture. `make use-cases`. Gate: usecases test green.
   Commit: docs: use case -- ground truth as a test oracle
4. Exerciser bin/usecase-ground-truth-oracle in the custom-emitter
   shape (marker block, per-command expect), registry row, pin 17 ->
   18 with citation; run it once, exit 0. Gate: exercised_sources +
   demo_exerciser_fresh tests green. Commit: docs: the prime audience's
   use case is exercised
5. CLI discovery. help.clj: `sim run --format`'s :doc names
   ground-truth as the semantic stream and points at consuming-
   ground-truth.md; `make cli-doc`. Gate: invocation-lint green and
   `make docsgen` moves nothing beyond cli.md and derived indexes.
   Commit: docs: sim run --format names the ground-truth contract
6. Full make test; record (asks-to-disposition against the ruling;
   what is now first, where); indexes; archive prompt. Fences: no src
   beyond help.clj :doc; no renumbering; no removal or deprecation
   language; no reading-set path gains a line without measured
   headroom (AGENTS.md is in all five). Commit: docs: prime-audience
   session record (archives prompt)
7. Push; verify CI yourself (gh run view); close-marker commit.

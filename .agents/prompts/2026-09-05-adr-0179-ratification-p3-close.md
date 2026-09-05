# 2026-09-05 — ADR-0179 ratification amendment; person-simulator P3 close

Archived verbatim, per `AGENTS.md`'s session-record ritual (R-A). The
record this prompt drove is
[`2026-09-05-adr-0179-ratification-p3-close.md`](../session-records/2026-09-05-adr-0179-ratification-p3-close.md).

---

Session: ADR-0179 ratification amendment + person-simulator P3 close — 2026-09-05

Context: docs-only. Two rulings from 2026-09-05 need their tree record, and one
roadmap row is closed by ADRs already landed. Fresh clone of
pragsmike/ehr-testing-tools at 6e3271de or later. WSL only. No sub-agents.
The rulings register is FROZEN (.agents/rulings.md header, 2026-08-25): the ADR is
the record; do not touch rulings.md.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md (:87-88, :142 close
ceremony); notes/adr/0179-merge-transfer-semantics.md (whole; :113-126 the
transitive-case section ending "for ratification"); .agents/plans/roadmap.md:1-17
(row grammar), :31-40 (the P3 row), :273-279 (Done carries no ledger);
components/person-simulator/src/ehrt/person_simulator/interface.clj:36-46.

Author rulings, verbatim and binding (2026-09-05, design channel):
 R-inv-ratified: "R-inv is ratified as implemented -- transitive." The one-hop wording
   was the ruling's example case, not its extent.
 R-merge-bed-cycle-open: "the freed bed's housekeeping (:merge absent from
   bed-correction-event-types) stays OPEN beside R-loc, pending the downstream reply."
   No engine change; no :bed-status-change from a merge.
 R-edit (standing): backticked prose through a script file, never an inline wrapper.
 R-cap (standing): roadmap rows are pointers; the row-cap gate decides.

Steps (one gate each; commit message given):
1. bin/preflight. Amend ADR-0179 in place with a dated addendum under the transitive
   section: R-inv-ratified verbatim; and under the open-items section (channel
   expectation: the section that lists R-loc and the bed-cycle item -- correct the
   heading from the file) R-merge-bed-cycle-open verbatim, stating that a downstream
   answer to the A40/census question is the revisit trigger. Nothing above the addendum
   changes. Invariant: `git diff --stat` touches only that file. Gate: make state-derived
   diff clean, then make test. Commit: "docs: ADR-0179 addendum -- R-inv ratified
   transitive; merge bed-cycle held open with R-loc".
2. Delete roadmap.md's OPEN [person-simulator] PRIORITY 3 row. The commit body names
   what closed it: ADR-0172 (charter), ADR-0173 and ADR-0174 (the folds), and the
   2026-09-02 F1 addendum at interface.clj:45 recording that ehrt.sim.run now calls
   the component. If any of the row's "seven rulings A-G open" (ADR-0172) remain
   unruled in the tree, STOP: leave the row, record which letters are open in the
   session record, and skip to step 3. Invariant: no other row moves.
   Gate: make test (row-cap and index gates). Commit: "docs: roadmap -- person-simulator
   P3 closed by ADR-0172/0173/0174 and the F1 addendum".
3. Session record (asks-to-disposition; the A-G check result stated either way);
   archive this prompt; close ceremony per SKILL.md. Push. Verify CI. Close-marker
   commit recording the CI success sha.
   Commit: "docs: record CI success at <sha> -- ADR-0179 addendum / P3 close".

# Session: ADR-0180 site 7 -- check-all folds once; the run hands its own projection -- 2026-09-07

Archived driving prompt (charter R-A). Record:
[`../session-records/2026-09-07-adr-0180-site-7-check-folds-once.md`](../session-records/2026-09-07-adr-0180-site-7-check-folds-once.md).

---

Context: check-all invokes engine/replay twenty times per run (fourteen call sites,
check.clj: 215 234 284 298 399 532 731 929 1060 1074 1174 1308 1758 1785), each a full
fold of the log. That is 49.85% of check, 34.71% of generate (the in-run self-check at
sim/run.clj:429-440, measured at site 6), and the source of the transient heap peak
(measurements.md:1003-1030). ADR-0180 addendum s."7." (:704-816) is the charter; the
:929 carrier `(or (:records folded) (engine/replay ground-truth))` already exists.
Ground truth moves by zero bytes by construction, so the bracket is necessary and says
almost nothing; the obligation is VERDICT identity. Fresh clone at ee7d0070 or later.
WSL only. No sub-agents. R-sentinel.

Read first: AGENTS.md; SKILL.md (:87-88, :142); addendum :704-816 in full (the reading-
site table at :752-759; the two divergences; the :warm-up-mark clause); check.clj:2113-
2160 (check-all), :920-935, :390-405, :720-735; sim/run.clj:420-445; fold.clj (replay:
bootstrap on first sight; the entry shape; :board concern from site 3); run.clj init-
world (:1280-1297, seeds every patient at t=0; mrn-for); bin/ground-truth-bracket (the
pattern for the new instrument); facility.clj:130-140 (rider).

Author rulings, verbatim and binding:
 R-check-once (addendum): "in-run, the run hands check-all its own projection; standalone
   sim check replays ONCE; every invariant reads the shared records."
 R-board-in-entry (remedy (a), ruled 2026-09-07): ":929 reads (:board world-before);
   replay opts the :board concern in; sim-model/occupancy-board leaves check."
 R-verdict-bracket (ruled 2026-09-07, standing for check-side sites): "check-all's
   violation vector, computed via the handed projection and via a fresh replay, is equal
   -- element for element, in order -- over all 38 oracle roots and the three cells;
   the instrument has its own zero before any repoint."
 R-bootstrap-mrn (addendum): "(mrn-for i) equals (:active-mrn ev) at every patient's
   first event, asserted over generated logs, not assumed."
 R-warm-up-omission stays (A2(b), fold.clj:700-719): untouched, and the handoff must not
   reach it. R-no-second-path, R-raw-read, R-move-not-improve, R-pins, R-edit, R-cap.
 R-rider-hint: "facility.clj:136's rng gets ^java.util.Random; bracket IDENTICAL proves it."

Steps (one gate each; commit message given):
1. bin/preflight; bracket IDENTICAL at the clean tip; baseline the two cells' generate
   AND check walls plus a22500-nopersons check wall; -Xlog:gc* on a7500-persons generate
   for the post-collection floor. No commit.
2. Instrument first. bin/check-verdict-bracket: for each oracle root and the three cell
   logs, run check-all two ways (records handed vs replayed) and diff violation vectors;
   at the clean tip both ways ARE replay, so its zero is trivially IDENTICAL -- record
   it. Gate: the instrument runs over all 38+3 and reports. Commit: "bin: check-verdict-
   bracket -- verdict identity for check-side refactors (R-verdict-bracket)".
3. Fold once, standalone. check-all replays once into `records` and every invariant
   takes records (or the folded seq) instead of calling engine/replay; the fourteen call
   sites become parameters; :929 per R-board-in-entry with replay opting :board in.
   Invariant: no invariant's body changes beyond its record source. Gate: make test +
   verdict-bracket IDENTICAL + ground-truth bracket IDENTICAL. Commit: "check: check-all
   folds once; :929 reads :board (ADR-0180 site 7)".
4. Hand-off in-run. sim/run.clj passes the run's own entries; the R-bootstrap-mrn
   assertion lands as a test over generated logs; the population divergence is shown
   irrelevant to every reading site in the table (:752-759) -- if any site cannot be
   shown indifferent, STOP and record. Rider: R-rider-hint. Invariant: both cell logs
   byte-identical; verdict vectors identical. Gate: verdict-bracket + ground-truth
   bracket IDENTICAL + make test. Commit: "sim: run hands check-all its own projection;
   choose's rng hinted (ADR-0180 site 7)".
5. Measure. Both cells generate+check, a22500 check, JFR at 7,500 both phases (frames
   now read by name), -Xlog:gc* floor before/after. Dated site-7 section; program close
   table; roadmap P1 row to CLOSED or its residue (:onboarding headroom 16, >= 8).
   Gate: make test. Commit: "plans: site 7 measured -- check <before> -> <after>;
   ADR-0180 program closed".
6. Session record; archive prompt; close ceremony; push; verify CI by sentinel; close-
   marker commit. Commit: "docs: record CI success at <sha> -- ADR-0180 site 7 close".

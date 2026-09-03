# 2026-09-03 — B2/B1: the stale-hold invariant, and the outpatient rule scoped to its visit

Archived verbatim. Session record:
[`2026-09-03-b2-b1-stale-hold.md`](../session-records/2026-09-03-b2-b1-stale-hold.md).

Repo: `pragsmike/ehr-testing-tools`, ext4 clone of record
(`~/src/ehr-testing-tools`), tip `526d262` at session start (the STOP
session's close marker, and the session's own oracle baseline).
Ceremony: R30 standing default with the prompt's own sequencing —
commits at the named steps, one push at step 8.

---

# Session: B2/B1 -- the stale-hold invariant, and the outpatient rule scoped to its visit (2026-09-03)

The downstream self-check STOP record (2026-09-02-downstream-self-check-
failed.md) found a discharged-then-cancel-admitted patient holding
`Emergency / ED-H372` for twenty years; `sim run` convicts it at
--patients 2000 only because a later visit stamps `:class :outpatient`
over it, and exits 0 at 1984 with the identical hold. This session
closes the oracle hole (B2) and scopes the accidentally-firing rule to
its own docstring (B1). It does NOT touch the engine: A1 is a later
session with its own ADR. Independent judge: no engine predicate reuse.
No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; the STOP
record (## Step 6 in full); components/sim-check/src/ehrt/sim_check/
check.clj :67-72, :405-528, :1807-1921 (and its independent-judge note
above the bed-cycle rows); check_test.clj :364, :560-580, :940-960;
components/sim/test/ehrt/sim/run_test.clj :1100-1118;
components/sim-engine/src/ehrt/sim_engine/log_index.clj :201-225 (read
to know what NOT to call); test-fixtures/downstream-calibration/;
docs/consuming-ground-truth.md "## What `ehrt sim check` certifies".

Author rulings, verbatim and binding:
- R-fork (2026-09-03): option (C) as recommended -- B2 then A1, B1
  rides on B2. A2/A3 declined. A1 out of scope here.
- R-catalog-pin (2026-09-03): `arc0-invariant-catalog` gains the new
  name as an ADDITION with a dated citation; nothing renamed or
  removed. A pinned corpus that B2 CONVICTS is a STOP, not a re-pin.

Steps:
1. Derive, in the record, before code: B2's predicate as the complement
   of admitted-occupies-one-slot + expired-patient-retains-location --
   name each status that may hold a `:location`, citing the evolve
   line that writes it; B1's "visit open" from encounter-openers/
   closers. Invariant: the derivation cites only the log-reading
   functions of check.clj. Gate: every status in the set has a cited
   writer. No commit.
2. RED. Minimal-log tests (check_test.clj conventions): B2 convicts a
   location reinstated onto a non-admitted subject, at the
   reinstatement's :t; B2 stays silent for :expired; B1 stops
   convicting after :outpatient-visit-end; naive twins for both.
   Gate: exactly these tests red, all else green.
   Commit: test: stale-hold invariant and visit-scoped outpatient rule -- RED
3. GREEN. Implement B2 (registered adjacent to its converse pair --
   the catalog is in reporting order) and B1's scoping; update the
   naive twins. Gate: sim-check brick green.
   Commit: feat(sim-check): a non-admitted patient holds no bed; outpatient rule scoped to the visit
4. Witness at a real shell, fixture config, seed 424242, --reference-
   date 2026-08-31 --churn --format ground-truth: --patients 1984 and
   2000 both exit 2 naming B2; 2000's old-invariant rows shrink to the
   in-visit count; ed-tuesday's own config still exits 0. Gate: those
   three exit codes. Record; no commit.
5. Pins and declarations. run_test's catalog per R-catalog-pin. Run
   the mutation conviction tests: an operator whose set gains B2 gets
   its declaration widened with a dated citation (disclose each);
   any pinned corpus convicted -> STOP. consuming-ground-truth.md's
   invariant list gains the name. Gate: full make test green.
   Commit: test: catalog pin, mutation declarations, docs -- B2 named
6. Oracle: bin/regression-oracle and bin/ground-truth-bracket vs
   526d262. Expected IDENTICAL (no payload moves). A root losing its
   :ground-truth key is a disclosed STOP finding. Gate: IDENTICAL.
7. Record + archive; add the STOP record's proposed roadmap row only
   if measured :onboarding headroom covers it, else report it.
   Fences: no change under sim-engine or sim src; no re-pin; no A1.
   Commit: docs: B2/B1 session record (archives prompt)
8. Push; verify CI yourself (gh run view); close-marker commit.

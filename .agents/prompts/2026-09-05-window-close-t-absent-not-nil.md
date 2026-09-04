# Session: :window-close-t is absent, never nil -- the log honors its own schema (2026-09-05)

Archived verbatim, as issued. Record:
[`../session-records/2026-09-05-window-close-t-absent-not-nil.md`](../session-records/2026-09-05-window-close-t-absent-not-nil.md).

---

STOP record 2026-09-05-p7-stop-derivation.md, finding 2: run.clj:146
omits :window-close-t when a placeholder window never resolves
(ADR-0173); decide.clj:331 re-adds it unconditionally as nil; the
schema (event_schema.clj:626) is `{:optional true} :int`, so nil is
invalid. Every :persons config ships events its manifest's
:event-schema-version claims it satisfies and does not, and nothing on
the run path checks whole-event validity. Three guards read `nil?`,
which cannot tell absent from nil. This session fixes the engine,
adds the gate the run path lacks, and rides the cancel invariant's
missing time clause (row cancel-invariant-has-no-time-clause). Payload
moves (keys removed): ADR first, declared sweep. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; the
STOP record (findings 1-2 in full); components/sim-engine/src/ehrt/
sim_engine/run.clj :125-147 and decide.clj :300-335; event_schema.clj
:615-630 and its validator entry point; persons_test.clj :640-660,
:835-850; sim_check/check.clj :940-960, :1590-1600, :1882-1930 (and
the independent-judge note); notes/adr/0166-*.md, 0173-*.md; 2026-08-
29-ts-5-superseded-cancel.md :235-292 with bin/oracle-lib.sh :224-260
(declaration protocol); components/sim/test/ehrt/sim/run_test.clj
(catalog pin); docs/consuming-ground-truth.md :355-375, :615-635;
test-fixtures/downstream-calibration/PROVENANCE.md.

Author rulings, binding:
- R-fix: decide.clj's placeholder branch assocs :window-close-t only
  when run.clj:146 supplied it. The key is ABSENT, never nil. No
  schema change; 1.8.0 stands (the log becomes conformant to it).
- R-gate: check-all gains `every-event-is-schema-valid`: every event
  validates against the published event schema. Validating against
  the contract is not reusing an engine decision; the independent-
  judge note is satisfied. Registered first in reporting order.
- R-time: `cancel-references-existing-uncancelled-event` gains the
  clause a cancel's :t is not before its target's :t; same invariant
  name, no new finding class.
- R-sweep: expected movers are exactly the :persons roots, each by
  removed `:window-close-t nil` pairs and NOTHING else -- prove it per
  moved root (diff the two logs; every hunk is one removed pair), then
  declare per the protocol. Any other delta is a STOP. One sweep.
- R-pins: a moved count pin is its own commit naming the count.
- R-tests: the three `nil?` guards become absence assertions
  (`(not (contains? e :window-close-t))`); `nil?` may not remain as
  the sole assertion anywhere it was.

Steps:
1. Derive, in the record: which oracle roots carry :persons; per root
   the count of :registered events with the nil key at the documented
   invocation (dense-7500 at 20: 3, from the STOP record); the ADR's
   payload sentence. Gate: every root classified. No commit.
2. ADR (next free number): decision, mechanism, payload effect,
   ADR-0173 and 0166 cited, the sweep's expected movers named.
   Gate: link-footnote + adr-index. Commit: docs: ADR -- :window-
   close-t absent, never nil; the run path validates its own schema
3. RED: absence assertions replace the three nil? guards; minimal-log
   detection test for every-event-is-schema-valid (a nil on an
   optional :int convicts; a legit log is silent); detection test for
   the time clause (cancel before its target convicts). Gate: exactly
   these red. Commit: test: absent-not-nil, schema-valid log, cancel
   time clause -- RED
4. GREEN: R-fix; the invariant; the clause. Bricks green in every
   project; catalog pin 45 -> 46 per R-pins in its own commit.
   Commits: fix(sim-engine): :window-close-t is absent when the window
   never resolves (ADR-NNNN); feat(sim-check): every event validates
   against the schema; the cancel invariant has a time clause
5. Witness at a real shell: dense-7500 --patients 20 self-check exit 0
   with the nil count now 0; the downstream fixture at 500 and 1000:
   exit 0, sha256 vs 434232a9.../ddcfc319... -- record move-or-match
   and, if moved, that every hunk is a removed pair. Gate: exits 0.
6. Sweep vs d55b90d per R-sweep; docs: invariant list gains the name,
   :615-635 unchanged (the claim is now true). Full make test.
   Commit: docs: sweep declared -- :persons roots lose nil pairs only
7. Record; rows: cancel-invariant-has-no-time-clause -> CLOSED; a new
   row for this fix CREATED and CLOSED in one entry as a two-clause
   pointer (not a narrative), under measured :onboarding headroom;
   indexes; archive. Fences: no operators.clj, no mutate, no P7 work.
   Commit: docs: session record (archives prompt)
8. Push; verify CI yourself (gh run view); close-marker commit.

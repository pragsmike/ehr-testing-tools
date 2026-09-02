# 2026-09-02 — downstream self-check-failed: repro and diagnosis

Archived verbatim. Session record:
[`2026-09-02-downstream-self-check-failed.md`](../session-records/2026-09-02-downstream-self-check-failed.md).

Repo: `pragsmike/ehr-testing-tools`, ext4 clone of record
(`~/src/ehr-testing-tools`), tip `2e62b9f` at session start (successor
of the consumer-docs session's close marker). Ceremony: R30 standing
default — commit and push at each checkpoint, unattended. Ruling
`R-shape` made this a **diagnosis-only** session: no change under any
`components/*/src` or `bases/*/src`, stopping at step 6, with the fix
itself left to the author's ruling.

---

Session: downstream self-check-failed -- repro and diagnosis (2026-09-02)

A downstream QA team's controlled calibration (channel-held) hit a
deterministic `:self-check-failed` (exit 2) from `sim run` on its own
output: seed 424242, --patients 2000, --reference-date 2026-08-31,
--churn, their config, --format ground-truth -- at THEIR revision
386e738d (88 behind tip). 100/500/1000 arrivals complete cleanly.
Evidence says run's internal self-check was already config-threaded
there (3ec147f7's docstring), so expect this live at tip. This session
reproduces, shrinks, and names the convicting invariant. It does NOT
fix: every outcome forks into log-actually-illegal vs invariant-wrong-
for-this-config-shape, and that ruling is the author's. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
docs/consuming-ground-truth.md (## What `ehrt sim check` certifies,
## Determinism); components/sim/src/ehrt/sim/run.clj (the self-check
call and :self-check-failed branch); the config file provided with
this prompt (sha256 must equal
4dd4a5c01a4d9fefce77f92b16526aaa38378dcc40a0a1d1d61ddc1f66f07e02).

Author rulings, verbatim and binding:
- R-A (2026-09-02): the consumer-docs session's step-2 fence breach
  (help.clj :doc edit + cli.md regeneration) is RATIFIED as landed.
- R-B (2026-09-02): the documented :capacity-exhausted consequence
  (halts; no corpus; no self-check; payload patient/ward/census) is
  RATIFIED.
- R-shape (2026-09-02): diagnosis only. No change under any
  components/*/src or bases/*/src. STOP at step 6.

Steps:
1. Riders. In README.md, extend the formats.md clause (near :250,
   "is the wire-level shape") to name the counting rule and link
   docs/formats.md's "Read the top-level vector only" anchor. Note
   R-A/R-B enactment in this session's record later.
   Gate: link-footnote gate green.
   Commit: docs: README names the top-level-vector rule
2. Fixture. test-fixtures/downstream-calibration/{config.edn,
   PROVENANCE.md} -- config byte-exact (verify sha), provenance:
   source (downstream QA calibration, 2026-09-02), their commit
   386e738d, seed 424242, their result table verbatim.
   Gate: sha256sum equals the hash above.
   Commit: test: downstream calibration config as fixture, provenance
3. Repro at tip: the exact command, --patients 2000, stdout to a file.
   Capture exit code and the complete :self-check-failed payload
   (every finding: invariant name, count, first instance in full).
   If exit 0 at tip: run step 4, skip step 5, and the STOP report's
   subject becomes the healing commit (bisect 386e738d..tip).
4. Confirm at theirs: fresh in-clone checkout of 386e738d (a second
   clone, NOT a worktree -- version/git-sha breaks in worktrees), same
   command; also --patients 500 there, sha256 vs
   434232a913c3389fdc3856f9a6eb14854ff6174499e8a5caa0643085824a03d5
   (within-version determinism: a mismatch is a disclosed finding,
   JDK 21 here vs their JDK 17). Record both, fix nothing.
5. Shrink at tip: smallest failing --patients in (1000, 2000] by
   bisection; record N_min and whether the payload's invariant set is
   stable across N_min..2000.
6. STOP report, in the session record: the convicting invariant(s),
   their source location in sim-check, the log slice + config facts
   the first conviction judged (what the checker saw -- no cause
   attribution), and the fork as lettered options with evidence for
   the author. Propose (do not add) a roadmap row. Archive this
   prompt. Fences: no src change anywhere; no roadmap edit.
   Commit: docs: downstream self-check-failed -- repro record, STOP
7. Push; verify CI yourself (gh run view); close-marker commit.

---

## Deviations

None on scope. Three things the session did that the prompt left open,
each named in the record:

1. **Step 3's payload capture needed a second pass.** The failing run
   emits only the error envelope, so the log itself is not in the
   stdout the prompt asks for. The log was captured through
   `run-command`'s own injectable `:engine-run-fn` seam (its documented
   second arity) from a script outside the brick tree — no `src` edit,
   the fence held.
2. **Step 4 gained an endpoint check the prompt did not ask for.**
   `--patients 1000` was also run at tip and compared against the
   downstream `ddcfc319…` digest, because step 5's bisection takes
   `N=1000 passes` as its lower endpoint and that endpoint was
   otherwise only attested by the downstream report at a different
   revision.
3. **Step 5's bisection assumes monotonicity in `--patients`**, which
   nothing in the tree guarantees. The record says so, and the
   stability sample is what it is checked against rather than a proof.

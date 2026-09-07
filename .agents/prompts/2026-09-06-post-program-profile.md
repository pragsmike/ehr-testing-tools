# Session prompt — 2026-09-06, post-program profile

Archived verbatim, the prompt that drove
[`../session-records/2026-09-06-post-program-profile.md`](../session-records/2026-09-06-post-program-profile.md).

---

Session: post-program profile -- CPU top frames and allocation, 7,500 and 22,500 -- 2026-09-06

Context: ADR-0180's four sites are at 0.00% of generate; the innermost-project-frame
table has a new top row nobody has read. Separately, peak RSS is 3.6 GB at 22,500
arrivals (measurements.md:893-925) under a 3.88 GB max heap that is the JVM DEFAULT
(no -Xmx anywhere in the tree; deps.edn:245's only :jvm-opts is native-access), and RSS
rose with each index at 7,500. This session measures CPU and allocation after the
program and ranks what is next; it changes no engine code. Fresh clone at 63d5ee64 or
later. WSL only. No sub-agents.

Read first: AGENTS.md; SKILL.md (:87-88, :142 -- and the site-4 record's section 7: NO
polling waiters; judge every job by the sentinel its command wrote); measurements.md
:216-307 (site tables), :860-925 (program summary, RSS); profile-cell.sh (the recorder
and aggregator -- it prints jdk.ExecutionSample only); run.clj (the accumulator: what the
world carries for the whole run -- :ground-truth, every index, :population); ADR-0180
:311-334 (R-order's last item: check's fourteen replays).

Author rulings, verbatim and binding:
 R-measure-first (standing): no engine change this session.
 R-sentinel: "a background job is done when its own sentinel file says so; notifications
   are not evidence; no sleep loops."
 R-commit-cells, R-edit, R-cap (standing).

Steps (one gate each; commit message given):
1. bin/preflight. CPU: profile-cell.sh over a7500-persons and a22500-nopersons, generate
   and check, as before. Add to the aggregator a third table: top-15 innermost PROJECT
   frames with inclusive AND self share, and the `apply-events` concern breakdown (which
   concern's frame, per ehrt.sim-engine.fold fn). Gate: four recordings, tables present.
   Commit: "plans: post-program CPU profiles, 7,500 and 22,500".
2. Allocation: re-record generate at both cells with jdk.ObjectAllocationSample enabled
   (channel expectation: `settings=profile` already enables it at a throttle; confirm from
   `jfr configure` / the settings file and raise the throttle if samples are too few);
   aggregate by allocating project frame and by class. Separately, `-Xmx8g` run of
   a22500-nopersons with GC logging (-Xlog:gc*) to a file: report peak used heap after
   full GC vs peak RSS, and whether the live set grows linearly with events. Invariant:
   the -Xmx8g log's sha256 equals the committed 22,500 digest (7d105743...). Gate: both
   tables present; sha equal. Commit: "plans: allocation profile and live-set growth at
   22,500".
3. Rank. A dated section in measurements.md: (a) new CPU top frames with shares; (b) live-
   set composition -- what of the run's memory is the :ground-truth vector, what is index,
   what is :population/patients; (c) the check phase's fourteen replays' current share;
   (d) a ranked recommendation among: check replay-once, a memory program (streaming the
   log; what would have to change for the world to drop its own history), a named
   constant-factor site. Recommendation only, NOT enacted. roadmap P1 row: one line
   pointing at the section (R-cap; :onboarding headroom 39). Gate: make test.
   Commit: "plans: post-program ranking -- CPU, allocation, live set".
4. Session record (every figure with its sentinel path); archive prompt; close ceremony --
   `ps` shows zero sleep and zero java before the marker; push; verify CI by sentinel;
   close-marker commit. Commit: "docs: record CI success at <sha> -- post-program
   profile close".

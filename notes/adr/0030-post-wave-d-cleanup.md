<!-- Attic file: notes/adr/0030-post-wave-d-cleanup.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0030 — Post-Wave-D cleanup: oracle byte-verification, closure engine round-trips, dual-clone guardrails

**Status:** Accepted (author-ruled 2026-08-02, design channel, J1–J5
below; recorded verbatim, attributed, per ADR-0007's own provenance-tag
convention). This session executes all five rulings same day.

### Context

The design channel's own 2026-08-02 review of Wave D (`d23fa9b..7257775`)
surfaced three findings, none reopening Wave D's own design: (1) the
D2/D3 regression-oracle claim was, twice, a full-suite test/assertion-
count comparison SUBSTITUTED for the session prompt's own literal
SHA-256-digest-across-a-disposable-worktree method (D1b's own
precedent) — the substitution itself was disclosed at the point each
session made it (ADR-0029's own D2 dated note), but went uncaught by
the design channel through two stages, leaving `fdd0644`
(`first-matching-entry`'s own fallback-to-last-entry fix, a real
interpreter dispatch-behavior change) resting on good-but-indirect
regression evidence rather than a literal digest; (2) H6's own "a full
engine/check run" instruction for closure-bearing roots has never
actually been fulfilled for any of the three closures vendored to
date (`ear_infections`/`urinary_tract_infections`/
`total_joint_replacement`) — every one of them is disclosed,
repeatedly, as interpreter-layer-proof-only, and the gap has simply
been carried forward each time rather than closed; (3) the dual-clone
edit hazard (`feedback-dual-clone-edit-hazard`, this machine's own
memory system) fired FOUR times across stage D3 alone despite a cited
prior lesson — vigilance is not working as a mitigation.

### Decision

- **J1 — Oracle verification is READ-ONLY with a binary outcome.** A
  disposable worktree at `d23fa9b` (pre-D3) and one at `7257775`
  (post-D3): identical fixed-seed golden runs for the SIX roots
  vendored before D3 (appendicitis, sinusitis, sore_throat,
  ear_infections closure, the Wave C death fixture, sepsis), SHA-256 of
  every output file, HL7 bytes included. Identical -> dated notes on
  ADR-0029's D2 and D3 execution records upgrading the oracle claim to
  byte-verified, with the digest table in the session record. ANY
  difference -> STOP: record the differing root, seed, file, and hunk;
  escalate to the design channel; fix nothing. If time permits and
  `d23fa9b`->`d8447e6`-era baselines are cheaply reproducible, extend
  the same check across D2's own span; if not, note the D2 claim as
  count-verified-only, honestly.
- **J2 — Oracle doctrine lands where sessions read it.** The
  `build-session` skill gains a VERIFICATION section rule — "a
  regression-oracle claim means SHA-256 digests of output files across
  a disposable worktree at the baseline commit; test-count or
  assertion-count comparison is NOT an oracle and may not be reported
  as one" — and AGENTS.md's verification guidance gains the one-line
  pointer. Dated, citing this session.

  **Dated amendment (2026-08-05, standing-equipment promotion,
  `notes/ADRs.md` ADR-0044 AR-P-2/AR-P-3):** the harness's own
  implementation detail this ruling's execution produced — digest.clj
  always read from THIS checkout, a synthetic classpath pointing only
  `:local/root` component deps at whichever worktree is under test, so
  the SAME test code exercises two different component-code versions —
  is CLOSED. `bin/regression-oracle`'s own digest producer is now
  `components/oracle`, a real Polylith component; each side's synthetic
  classpath now points `poly/oracle` at THAT worktree too, the same way
  it already did for every other component. This closes the standing
  limitation named in `.agents/plans/roadmap.md`'s own Deferred section
  (the row this amendment also closes there) after it forced two live
  workarounds (ADR-0033 AR-4b's own hand-run six-root table; ADR-0043
  M2's own split-mode bracket) — see ADR-0044 for the full mechanism
  and verification.
- **J3 — Closure round-trips.** For each of the three closure-bearing
  roots (ear_infections, urinary_tract_infections,
  total_joint_replacement), an engine-layer test in the sepsis
  template's shape — compile-trajectory -> engine run -> check
  invariant catalog -> emitted-message assertions (including UTI's
  cross-boundary encounters finally proven THROUGH the engine, and
  TJR's care-plan silence held at the emission layer). Tests only: if
  the engine mishandles a closure-produced trajectory, that is a
  FINDING (potentially a real defect Wave B's deferred check would have
  caught) — recorded red, escalated, not patched under this session.
  Mixer-RNG seeds; runtime kept sane (small patient counts — round
  trips, not soak tests).
- **J4 — Dual-clone guardrails, layered, mechanical first.** (a)
  MECHANICAL EDIT GUARD: the `/mnt/c` clone's working tree made
  read-only (verified by attempting a write and seeing it FAIL). (b)
  MECHANICAL COMMIT/PUSH GUARD: reject-all `pre-commit`/`pre-push`
  hooks installed into `/mnt/c`'s own `.git/hooks` (per-clone,
  uncommitted by nature), naming the ext4 UNC path as the clone of
  record. (c) SANCTIONED SYNC PATH: a committed `bin/sync-mnt-c` script
  — clears the read-only state, `git pull --ff-only`, restores it —
  becoming the ONLY documented way `/mnt/c` moves; the `build-session`
  skill's fast-forward-for-hygiene step rewritten to invoke it. (d)
  SKILL HARDENING: `build-session` gains a preflight rule — resolve
  both clone roots at session start; every edit target must resolve
  under the ext4 root; a mismatch is a stop-and-report, not a
  copy-and-revert. (e) PROOF: each guard demonstrated firing (a blocked
  write, a blocked commit), recorded. Removal of the `/mnt/c` clone
  outright is explicitly NOT this session's call — a named author
  question in the close-out, the guards sound either way.
- **J5 — Roadmap bookkeeping.** The two named items from the design
  review ("oracle byte-verification", "closure engine round-trips")
  enter and exit the roadmap within this session; the dual-clone guard
  work gets its own row; H8's own standing-items list is untouched.

### Execution note (filled Step 5, 2026-08-02)

All five rulings executed same day. J1: `bin/regression-oracle` built
and run across `bbeceb6`->`d23fa9b`->`7257775` (D2's span and D3's
required span both), IDENTICAL on all six roots both times — dated
notes on ADR-0029's own D2/D3 sections have the full digest citations.
J2: `build-session/SKILL.md` (both mirrors) gained its VERIFICATION
section; `AGENTS.md` its pointer. J3: three new round-trip tests
(`components/sim-emit-hl7/test/`) each PIN a confirmed, real engine
gap rather than proving the round trip works — H6's own instruction,
tried for real for the first time, found genuinely broken (`engine.clj`
never threads a closure's own submodule registry, tables, or
`initial-attributes` through to `run-module`); not fixed, per this
ruling's own tests-only fence. J4: all five parts landed and each
mechanical guard demonstrated firing for real — a blocked `Edit`
(`EPERM`) against the locked `/mnt/c` tree, a blocked `git commit`
(`REJECTED`, exit 1) against its own reject-all hook, the pre-push
hook firing standalone too; `bin/sync-mnt-c` already run once to
fast-forward `/mnt/c` and bootstrap the lock. J5: roadmap rows entered
and exited within this session (see `.agents/plans/roadmap.md`'s own
"Done (2026-08-02, post-Wave-D cleanup)" section); H8's own
standing-items list untouched.

`poly check` clean throughout; the full non-integration suite green at
every checkpoint (0 failures/0 errors, including the three new
round-trip tests passing as designed — pinning known-broken behavior,
not proving success).

Commits, in order: `64e250f` (Step 0), `56c7cef` (Step 1, oracle
harness + verification), `31e8460`/`4eecd3f` (same-session exec-bit
fix-forwards, `core.fileMode=false` hiding a filesystem chmod from the
index — `ehrt.cli.executable-bits-test`'s own established bug class),
`cd76334` (a genuine concurrent write from another process using this
same author identity on the SAME ext4 clone mid-session — landed
directly, bundling this session's own in-progress J2 doctrine edits
with an unrelated new plan file under one non-ceremony message; kept
intact rather than force-pushed over once already public on `origin`,
a small fix-forward commit added on top instead — full account in the
session record), `71093d5` (the fix-forward), `00c32f8` (Step 3,
dual-clone guardrails), `9a2514f`/`46f066d`/`093d321` (Step 4, one
closure round-trip test per root), this commit (Step 5, records).
Session record: `.agents/session-records/2026-08-02-post-wave-d-
cleanup.md`.

### Deviation record

One deviation from the ruled plan, disclosed at the point it occurred:
mid-session, `git push` was rejected because `origin/main` already
carried `cd76334` — a commit this session did not make, landed by
another process sharing this clone and this author's own git identity
while Step 2 was in flight. Not anticipated by J1-J5 (which assume
this session has the ext4 clone to itself). Resolved per the author's
own live ruling (asked in chat, mid-session): kept `cd76334` intact on
`origin` rather than force-pushing a locally-rewritten "clean split"
history over it, and landed the small delta as its own fix-forward
commit instead. No J1-J5 ruling itself was reopened — this is a
process/concurrency finding, not a design one. Worth a standing note
for whoever schedules future R30 sessions: this clone is not always
exclusively this session's own, R30's own "unattended" framing
notwithstanding.

---


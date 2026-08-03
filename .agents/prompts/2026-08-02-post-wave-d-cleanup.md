# 2026-08-02 — Post-Wave-D cleanup session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). The ext4 clone was already at
`origin/main`'s own HEAD (`7257775`) at session start. A genuine
concurrent write on this SAME ext4 clone (a commit from another
process sharing this author's own git identity, mid-session) is not a
dual-clone mismatch in the usual sense this prompt's own J4 addresses
— see the session record's own Findings section for the full account
and how it was resolved.

## Prompt, verbatim

> 2026-08-02 — ehr-testing: post-Wave-D cleanup — oracle verification, closure round-trips, dual-clone guardrails
> Context
> Wave D is closed (`07ff1d5..7257775`). This session executes the design channel's three post-wave findings (2026-08-02 review): (1) VERIFY the D2/D3 regression-oracle claim with the literal byte-digest method — the sessions substituted a test/assertion-count comparison for the specified SHA-256-across-a-worktree check, the substitution went uncaught by the design channel through two stages, and `fdd0644` (`first-matching-entry` fallback-to-last) is a SEMANTIC change whose regression-freeness currently rests on good-but-indirect evidence; (2) build the closure ENGINE ROUND-TRIP tests that H6 required and D3 disclosed have never existed for any closure-bearing root; (3) make the dual-clone edit hazard — fired four times in D3 despite a cited lesson — structurally impossible rather than vigilance-dependent. Items 1 and 2 are verification and tests only: NO production code changes are in scope, and a failure in either is an ESCALATION with evidence, never a fix.
> Ceremony: R30-mode — commit and push at each checkpoint, unattended, with R30's safeguards; message via the Write tool; tags and repo-level `gh` outside the grant. Work in the WSL ext4 clone via the UNC path exclusively; fast-forward to `origin/main` (at or past `7257775`), record HEAD. Note: Step 3 installs guards that change how `/mnt/c` syncs — do NOT fast-forward `/mnt/c` before Step 3 lands its sync script; use it afterward.
> Read first
>
> 1. `.agents/session-records/2026-08-02-gmf-coverage-wave-d-stage-d3.md` — the oracle-method disclosure, the `fdd0644` isolation-run evidence, the H6 deviation, and the four dual-clone incidents.
> 2. The D1b session record / its oracle script — the disposable- worktree digest method Step 1 reuses (fixed-seed run per root, sha256 every output file including emitted HL7).
> 3. `components/sim-trajectory/test/.../vendored_sepsis_test.clj` AND the sim/sim-emit-hl7-layer sepsis tests from D1b — the engine/ check round-trip template Step 2 adapts for closures.
> 4. `.agents/skills/build-session/SKILL.md` and `.agents/skills/wsl-windows-git-hygiene/SKILL.md` — the surfaces Step 3 hardens.
> 5. `notes/ADRs.md` ADR-0029's D2/D3 execution records — where Step 1's verdict lands as dated notes.
>
> Author rulings (ruled 2026-08-02, design channel)
>
> * J1 — Oracle verification is READ-ONLY with a binary outcome. A disposable worktree at `d23fa9b` (pre-D3) and one at `7257775`: identical fixed-seed golden runs for the SIX roots vendored before D3 (appendicitis, sinusitis, sore_throat, ear_infections closure, the Wave C death fixture, sepsis), sha256 of every output file, HL7 bytes included. Identical → dated notes on ADR-0029's D2 and D3 execution records upgrading the oracle claim to byte-verified, with the digest table in the session record. ANY difference → STOP: record the differing root, seed, file, and hunk; escalate to the design channel; fix nothing. If time permits and `d23fa9b`→`d8447e6`-era baselines are cheaply reproducible, extend the same check across D2's span; if not, note the D2 claim as count-verified-only, honestly.
> * J2 — Oracle doctrine lands where sessions read it: the build-session skill gains a VERIFICATION section rule — "a regression-oracle claim means SHA-256 digests of output files across a disposable worktree at the baseline commit; test-count or assertion-count comparison is NOT an oracle and may not be reported as one" — and AGENTS.md's verification guidance gains the one-line pointer. Dated, citing this session.
> * J3 — Closure round-trips: for each of the three closure-bearing roots (ear_infections, urinary_tract_infections, total_joint_replacement), an engine-layer test in the sepsis template's shape — compile-trajectory → engine run → check invariant catalog → emitted-message assertions (including UTI's cross-boundary encounters finally proven THROUGH the engine, and TJR's care-plan silence held at the emission layer). Tests only: if the engine mishandles a closure-produced trajectory, that is a FINDING (potentially a real defect Wave B's deferred check would have caught) — record it red, escalate, do not patch the engine under this prompt. Mixer-RNG seeds; runtime kept sane (small patient counts; these are round-trips, not soak tests).
> * J4 — Dual-clone guardrails, layered, mechanical first: (a) MECHANICAL EDIT GUARD: the `/mnt/c` clone's working tree is made read-only (Windows ACL/attrib via the WSL interop, whichever proves reliable — verify by attempting a write and seeing it FAIL), so a misdirected Edit errors instead of landing. (b) MECHANICAL COMMIT/PUSH GUARD: install reject-all `pre-commit` and `pre-push` hooks into the `/mnt/c` clone's `.git/hooks` (per-clone, uncommitted by nature — the session record documents their installation and content) whose message names the ext4 UNC path as the clone of record. (c) SANCTIONED SYNC PATH: a committed `bin/sync-mnt-c` script — clears the read-only state, `git pull --ff-only`, restores the read-only state — becoming the ONLY documented way `/mnt/c` moves; the build-session skill's fast-forward-for-hygiene step is rewritten to invoke it. (d) SKILL HARDENING: the build-session skill gains a preflight rule — resolve both clone roots at session start; every edit target must resolve under the ext4 root; an edit that lands elsewhere is a stop-and-report, not a copy-and-revert. (e) PROOF: each guard is demonstrated firing (a blocked write, a blocked commit) and the demonstration recorded — red→green discipline applied to the guards themselves. Removal of the `/mnt/c` clone outright is explicitly NOT this session's call — it is a named author question in the close-out (what, if anything, still consumes it), with the guards sound either way.
> * J5 — Roadmap bookkeeping: the two named items from the design review ("oracle byte-verification", "closure engine round-trips") enter and exit the roadmap within this session; the dual-clone guard work gets its own row; the standing-items list from H8's retrospective is untouched.
>
> Steps
>
> 0. Records. Roadmap rows per J5; session-start note on a new ADR entry (next number: "post-Wave-D verification and guardrails", J1–J5 verbatim). Commit: `docs: post-wave cleanup session start (J1-J5)`.
> 1. Oracle verification per J1. Build the digest script from the D1b pattern (commit it under `bin/` or the established scripts home — it is now standing equipment, J2's rule will reference it), run both worktrees, land the verdict. Commit: `test: byte-digest oracle verification d23fa9b->7257775 (J1)` — or the escalation stop, with nothing committed past the evidence.
> 2. Doctrine per J2 (skill + AGENTS.md). Commit: `docs: oracle doctrine -- digests, not counts (J2)`.
> 3. Guardrails per J4, all five parts, proofs recorded. Commit: `chore: dual-clone guardrails -- RO tree, reject hooks, sync-mnt-c, skill preflight (J4)`.
> 4. Closure round-trips per J3, one commit per root (or one commit plus escalation records, as the runs decide): `test(sim): closure engine round-trip -- <root> (J3)`.
> 5. Close out. Full suite + `poly check` green; ADR execution + deviation records; roadmap rows closed per J5 with the `/mnt/c` author question named; session record (digest tables, guard proofs); self-archive this prompt to `.agents/prompts/`. Final commit: `docs: cleanup records (ADR, roadmap; archives prompt)`.

## Deviation from the prompt's own literal Step sequence

Step 5's own final commit message, as literally specified
("`docs: cleanup records (ADR, roadmap; archives prompt)`"), was
adjusted to name what it actually contains more precisely — the
prompt's own generic wording predates the mid-session concurrent-write
finding this record documents. Steps 0-4 ran exactly as specified, one
commit each (Step 1 additionally needed two small same-session
exec-bit fix-forward commits, the established `ehrt.cli.
executable-bits-test` bug class, not a deviation from J1 itself).

The concurrent-write incident (a real commit from another process on
this SAME ext4 clone, mid-Step-2, bundling this session's own J2
doctrine edits with an unrelated new plan file) was not anticipated by
J1-J5 or the Steps sequence — resolved by asking the author directly,
in chat, once the blast radius (a rejected push to a shared/public
remote) became clear, per this repo's own "confirm before force-
pushing shared history" discipline. Full account: this session's own
record, `.agents/session-records/2026-08-02-post-wave-d-cleanup.md`.

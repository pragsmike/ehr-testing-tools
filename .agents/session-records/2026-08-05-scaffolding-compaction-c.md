# 2026-08-05 — Scaffolding compaction C: the continuity register lands, `/mnt/c` retires, arc closes

## Scope

Design-channel session prompt naming AR-C-1 through AR-C-4. Session C
of the scaffolding-compaction arc (A — riders, vestige retirements,
Deferred triage, `notes/ADRs.md` ADR-0045; B — the ADR split and the
roadmap rotation, ADR-0046 — both landed and verified same day). This
session ran INVERTED: the design channel authored `.agents/state.md`'s
own draft directly, pasted into the prompt; this session's own primary
job was per-claim verification against the live tree at tip `53edcad`
before landing it, correcting or tagging every claim that didn't
hold — never silent retention of a failed claim. Full account,
rulings, the verification table, and the corrected `/mnt/c` incident
ledger: `notes/ADRs.md` ADR-0047.

Step 0 (verify the draft) probed every `[V]`-tagged claim in the
design channel's own draft against the live tree. Two structural
corrections survived probing: the component-graph claim that
`sim-emit-hl7` depends on `sim-engine` (false — it depends on
`sim-model` only, fresh-grepped var-by-var); and the draft's own
"four-incident ledger" for `/mnt/c` (three of its four named labels —
"M2 mis-target", "M4 staleness", "promotion memory note" — have zero
supporting evidence anywhere in the M2/M3/M4 or standing-equipment-
promotion session records). Several smaller citations also needed
correction: the "43 files" `notes/adr/` count (stale the moment
ADR-0046 added itself as file 44 — corrected to 45, accounting for
this session's own ADR-0047); AR-C-3's own claim that `AGENTS.md`
carries guarded-mirror language (fresh grep: zero hits, nothing to
retire there — disclosed, not silently no-opped). One drift found and
disclosed, not fixed (out of this arc's own fence): the
`myocardial_infarction.json` Deferred row carries an in-place
"RESOLVED... see Done, below" note rather than having actually been
relocated, a pre-existing gap in compaction A's own AR-A-5 sweep. Full
verification table, all thirteen rows: ADR-0047.

Step 1 (`e99c72b`, AR-C-1/AR-C-2) landed `.agents/state.md` with every
correction from Step 0 applied inline (at the point of the claim it
corrects, not as a separate errata section) and `.agents/rulings.md`
as a seed — standing rulings extracted from ADR-0043 through ADR-0047
only, back-filling ADR-0001..0042 named as a future trigger rather
than attempted. Neither file is indexed in any README —
`index_completeness_test.clj`'s own `indexed-directories` list does
not gate `.agents/` at its top level, confirmed by reading the test
directly before assuming an index edit was owed. Full suite green
before and after (511 assertions / 195 `deftest`s, 0 failures, 0
errors); `clojure -M:poly check`: OK.

Step 2 (`e7646b5`, AR-C-3) deleted `bin/sync-mnt-c` after confirming,
fresh, that no live automation ever called it (`grep -rn "sync-mnt-c"`
across `Makefile`, `.github/`, `.githooks/`, and `bin/` itself — all
empty; no STOP-AND-ESCALATE). `.agents/skills/build-session/SKILL.md`'s
own guarded-mirror preflight step rewritten with a dated retirement
note (not deleted outright — the step still has a smaller job: confirm
the session's cwd is the ext4 clone). The `.claude/skills/` mirror
edited identically; `diff` confirmed byte-identical after editing,
matching what `skill-mirror-currency-test` itself checks. The two
roadmap.md Externals rows that posed the `/mnt/c` disposition question
closed with dated notes pointing at ADR-0047. Full suite green before
and after; `clojure -M:poly check`: OK.

Step 3 (this record, AR-C-4) authored `notes/adr/0047-scaffolding-
compaction-c.md` directly, appended its own index line to
`notes/ADRs.md`, corrected `notes/adr/README.md`'s own stale file
count (43→45, found while landing — same staleness class as the
draft's own citation, disclosed in ADR-0047's verification table
addendum), and added this session's own Done pointer
(`- 2026-08-05 — scaffolding-compaction-c — ADR-0047`) beside
compaction A's and B's. Ran the oracle bracket (below), archived this
prompt, and recorded this session.

## Deviations, disclosed

- **State.md's own forward-looking citations.** Several claims inside
  `.agents/state.md` (the 45-file `notes/adr/` count, the `/mnt/c`
  disposition line, ADR-0047's own existence) describe the state this
  SESSION's own later steps produce, not the state at Step 1's own
  landing commit — `.agents/state.md` is a continuity artifact meant
  to be accurate at the arc's close, and this session closes the arc
  it describes within itself. Each such claim is phrased to say so
  explicitly ("by this arc's own close," "ADR-0047 AR-C-3") rather than
  reading as a claim already true at Step 1's own commit. Disclosed
  here since it is a real, if minor, tension in the Step-ordered
  ceremony (state.md lands before the ADR it cites exists) that a
  future arc-close session should expect to hit again.
- **`myocardial_infarction.json`'s own Deferred-row drift: disclosed,
  not fixed.** Fixing it would mean editing `.agents/plans/roadmap.md`
  beyond the two `/mnt/c` Externals rows AR-C-3 licenses — outside this
  session's own fence. Named in both `.agents/state.md` and
  `notes/adr/0047-scaffolding-compaction-c.md` so a future Deferred-
  triage session (the next natural home for this fix, per ADR-0045's
  own precedent) doesn't have to rediscover it from scratch.
- **AR-C-3's own premise correction, not silently adapted.** The
  ruling names `AGENTS.md` as a surface carrying guarded-mirror/
  dual-clone language to retire; fresh grep found none. Per this
  workspace's fix-forward-with-disclosure rule, the finding is
  recorded (in both `.agents/state.md` and ADR-0047's own verification
  table) rather than silently treated as "nothing to do here."

## Findings (disclosed, not fixed — out of this session's own scope)

- **`myocardial_infarction.json`'s own Deferred-row drift** (see
  above) — a pre-existing gap in compaction A's own AR-A-5 sweep,
  predating this arc's session C.
- **`AUTHORS-GUIDE.md`'s own `/mnt/c` example command** (§1, "Reaching
  WSL from a Windows-launched agent session": `wsl -e bash -lc "cd
  /mnt/c/<path> && <command>"`) and **`notes/facts-register.md`'s own
  F7 performance figures** (measuring a `9p`/`drvfs`-mounted `/mnt/c/
  ...` repo's own test-suite runtime) both still mention `/mnt/c` —
  neither file is named in AR-C-3's own scope (`AGENTS.md` and
  `.agents/skills/` only), and neither describes THIS repo's own
  guarded-mirror doctrine specifically (the AUTHORS-GUIDE line is
  generic Windows-launched-session routing advice; F7 is a historical
  timing measurement). Left untouched, named here rather than swept
  silently beyond the ruling's own stated boundary.

## Verification

- `bin/regression-oracle 53edcad e7646b5` (this session's own tip
  before this record's own commit — no `src` touched at any point in
  this session, so any digest change would have been STOP-AND-
  ESCALATE): `IDENTICAL: every root's digest matches between 53edcad
  and e7646b5` — all ELEVEN vendored-root batches (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as expected for a docs/script-deletion-only session. Full SHA-256
  table recorded in the bracket's own log, this session.
- Deftest/defspec parity: 195 `deftest`s / 511 assertions, project-
  wide, unchanged across all three steps — no code touched at any
  point this session, confirmed by direct full-suite re-run after each
  commit rather than assumed from the diff shape.
- `clojure -M:poly check`: OK, all three steps.
- `clojure -M:poly test :all skip:integration`: 511 passes / 0
  failures / 0 errors, all three steps, shape unchanged throughout.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, both prior checkpoints (Steps 1/2):
  each showed exactly one delta against its own message file — `git
  log --format=%B -1`'s own trailing-newline artifact, the same known,
  harmless class `AUTHORS-GUIDE.md`'s own dated amendment already
  names.

Commits, in order: `e99c72b` (Step 1), `e7646b5` (Step 2), and this
session's own closing records commit (Step 3).

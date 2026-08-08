# 2026-08-08 — ehr-testing-tools: colorectal investigation (arc opening)

Repo: `ehr-testing-tools`, ext4 clone, `~/src/ehr-testing-tools`. HEAD at
session start: `45eb2f4` (the fidelity arc close, ADR-0084). Archived
verbatim as this session's own driving prompt, per R-A.

---

## Context

Conventions read at HEAD `45eb2f4` (fidelity arc close, ADR-0084), design
channel, 2026-08-08, verified by fresh public clone. This session opens a
new arc. It executes the roadmap's own Deferred row
"**`colorectal_cancer.json`'s own `:clinical-content-only-when-admitted`
gap, true name, undiagnosed**" (`.agents/plans/roadmap.md`, entered by
ADR-0083) — ADR-0084's named top handoff — and pays ADR-0084's recorded
mechanical debt (the successor tag, AR-CI-0 below). Ruled in the design
channel 2026-08-08 ("Concur. go." over the proposed sequencing:
diagnose-only colorectal probe first; the pairing-as-data shape questions
run in the design channel in parallel and are OUT of this session's
scope).

R30 ceremony, standing. Working directory is the ext4 clone at its UNC
path (the only clone of record, ADR-0047 AR-C-3). Fast-forward to origin
and record HEAD before starting; expected tip `45eb2f4` — a later tip is
an escalation unless the record chain explains it. Commit-and-push at
every checkpoint; commits land green (rulings register, quality-review
arc). Roadmap rows land in the same commit as the work that changes them.

This is a DIAGNOSIS session. Its product is knowledge, recorded in
ADR-0085 — not a fix, not a vendoring, not a refactor.

## Read first

1. `notes/adr/0084-fidelity-arc-close.md` — the close, the intake list,
   the tag debt, the two-session deviation record.
2. `notes/adr/0082-encounterend-fix.md` — the probe precedent this
   session extends: the `with-redefs` interception at the
   `ehrt.sim-trajectory.interface/run-module` boundary, the per-seed
   violation tables (colorectal 4/0/4 at seeds 20260802/1/42), the
   raw-trajectory scan that cleared the interpreter layer, and AR-EE-1a
   (the truncation-layer absorbed-error trace — AR-CI-3's hypothesis).
3. `notes/adr/0083-fidelity-payoff.md` — the erratum chain; how the
   misdiagnosis was corrected append-don't-erase.
4. `.agents/plans/roadmap.md` — the colorectal Deferred row (this
   session's own row; its dated note lands here).
5. `.agents/state.md` — Live work (the `mutate-stdout-stdin-loopback`
   flake disclosure, relevant if the full suite runs red once).
6. `components/sim-check/src/ehrt/sim_check/check.clj` —
   `clinical-content-only-when-admitted` (line ~438: the invariant walks
   `engine/replay` and fires when a therapeutic-intent event's
   `(:status before)` ≠ `:admitted`) and
   `discharge-follows-admission`.
7. `components/sim-trajectory/src/ehrt/sim_trajectory/compile_trajectory.clj`
   and `components/sim-engine/src/ehrt/sim_engine/engine.clj` — the two
   candidate layers.
8. `.agents/skills/build-session/SKILL.md` — ceremony mechanics.

## Author rulings

- **AR-CI-0 [A]** (ADR-0084, "This close's own mechanical debt"): tag
  `stable-20260808-fidelity-close` at `45eb2f4`, Step 0, standing
  ceremony — tag law case (ii), the predecessor's design-channel-verified
  stable point (design channel re-verified 2026-08-08: fresh clone, tip
  exact, CI green five-deep via the Actions API). If the tag already
  exists at that exact commit, verify and disclose, never re-create
  (ADR-0057 AR-T-1). Deferring this licensed tag is the deviation and
  needs a disclosed reason.
- **AR-CI-1 [A]** (ruled 2026-08-08, the design-channel sequencing the
  author concurred to): **diagnose-only fence.** No production `src/` or
  `test/` edit anywhere in the workspace, no `deps.edn` edit, no
  vendoring, no interpreter/compile/engine change — even if the fix looks
  obvious once the mechanism is found. A tempting fix found mid-probe is
  a FINDING, recorded, never taken (AR-P-4, rulings register). The fix
  session is a separate, ruled follow-up the design channel authors from
  ADR-0085's own diagnosis. Committed artifacts this session:
  `notes/adr/0085-colorectal-investigation.md` + its `notes/ADRs.md`
  index line, the roadmap dated note, the session record, the archived
  prompt — nothing else.
- **AR-CI-2 [C]** (channel-inferred probe protocol, extending ADR-0082's
  own mechanics — fix-forward and disclose if the live tree makes any
  detail wrong): **localize by layer bisection.** The invariant fires at
  the replay layer over ground truth; the raw interpreter walk is already
  probe-cleared (ADR-0082: zero dangling references across all 300
  seed-42 walks). So for each violating patient, capture and compare
  three layers: (a) the raw interpreter walk (the events and their
  encounter structure as walked), (b) the compiled trajectory
  (`compile-trajectory`'s own output — where does the therapeutic-intent
  event sit relative to `:admission`/`:discharge` here?), (c) the ground
  truth and the replay's own status stream (what `(:status before)` was
  at the event's `:t`, and why). The diagnosis is which seam introduces
  the mispositioning: content already outside an open encounter at (b) is
  a compile-layer defect; content correctly positioned at (b) but
  replaying as un-admitted at (c) is an engine/replay defect. Mechanics:
  the pin checkout at `/home/mg/synthea-checkout` (verify the pin is
  `7e08387c68a7f0e21d13076609a159fd473fc902` FIRST — the census tool's
  own `verify-pin` or a direct `git rev-parse HEAD`; a wrong pin is
  STOP-AND-REPORT); `colorectal_cancer.json`'s closure loaded exactly as
  ADR-0082 loaded it (no `:persona-config` override — its `Initial`
  state is not Race-gated, confirmed there); 300 patients; seeds 42 and
  20260802 (the two violating seeds); probes run in-session via
  `clojure -M:dev` scratch and/or `with-redefs` interception, zero
  working-tree disturbance — results TRANSCRIBED INTO ADR-0085 as tables
  (transcript-witnessed is not repo-recorded; the ADR is the artifact).
- **AR-CI-3 [C]**: the truncation-layer mechanism (`:pre-horizon` drop
  gate + `encounter-closed?` single-encounter scope, ADR-0082 AR-EE-1a)
  is a HYPOTHESIS this session must explicitly probe, not a diagnosis it
  may assume — it is same-layer-adjacent, and plausible-by-adjacency is
  not a diagnosis (rulings register, fidelity arc — the law colorectal
  itself earned). ADR-0085 states, with probe evidence, whether the
  hypothesis is confirmed, refuted, or left open.
- **AR-CI-4 [C]**: trace the single early `:discharge-follows-admission`
  violation to its own patient and mechanism under the same protocol. It
  may share the mechanism or be a second defect — assume neither; say
  which the evidence shows, or that it doesn't.
- **AR-CI-5 [C]** (acceptance bar): the session closes successfully with
  either (i) a LOCALIZED diagnosis — named namespace, named mechanism,
  probe-evidenced, plus a proposed fix shape (not a fix) for the design
  channel to turn into a ruled fix session — or (ii) a STOP-AND-REPORT
  recording exactly which layers were exonerated, by what probe, and
  where the trail ends. Both are wins. Re-stating "one compile layer
  downstream, mechanism unknown" is not a close.

## Steps

**Step 0 — Preflight + tag (AR-CI-0).** Confirm the ext4 clone
(`uname -a`), fast-forward, record HEAD (expect `45eb2f4`), clean tree;
disclose whatever untracked files the preflight finds
(`config/busy-weekday.md`'s disposition is ceremonial — untouched if
present, per the temp-dir rider ADR-0076/0079). `clojure -M:poly check`
OK. Oracle pre-digest: `bin/regression-oracle 45eb2f4 45eb2f4` — all 28
roots IDENTICAL expected. Disclose the last five CI runs' conclusions
(watched, never waited on). Then create and push
`stable-20260808-fidelity-close` at `45eb2f4` (or verify-and-disclose if
present). No commit this step.

**Step 1 — Reproduce the baseline.** Verify the pin, load the closure,
run `check/check-all` at seeds 20260802, 1, and 42 (300 patients).
Expected shape: violations at 20260802 and 42, zero at 1 — ADR-0082
recorded `{:clinical-content-only-when-admitted 19,
:discharge-follows-admission 1}` at 42 and `{:discharge-follows-admission
1, :clinical-content-only-when-admitted 3}` at 20260802. Exact counts may
drift with invariant-catalog evolution (ADR-0082's own precedent) —
disclose exact counts; a QUALITATIVE change (seed 1 now violating, or a
new invariant class appearing) is disclosed prominently and folded into
the diagnosis, not smoothed over. No commit.

**Step 2 — Bisect and trace (AR-CI-2/3/4).** Identify the violating
patients (the invariant rows carry `:patient-id` and `:at`). For a
representative set — at minimum every distinct violating patient at seed
42, and the `:discharge-follows-admission` patient at each seed — capture
the three layers and localize the seam. Probe the truncation hypothesis
explicitly. Build the evidence tables. No commit.

**Step 3 — Record.** Author
`notes/adr/0085-colorectal-investigation.md`: the reproduction table, the
bisection method, the per-layer evidence, the diagnosis (or the
stop-report), the truncation-hypothesis verdict, the
discharge-violation trace, and the proposed fix shape (or the open
questions a fix session must answer). Append its index line to
`notes/ADRs.md`; correct `notes/adr/README.md`'s file count if it states
one. Append a dated note to the roadmap's colorectal Deferred row
pointing at ADR-0085 (row stays LIVE — deferred until a fix session
closes it). Run the full suite once before committing
(`clojure -M:poly test :all skip:integration`); if
`mutate-stdout-into-intake-stdin-real-loopback-test` fails once,
disambiguate per state.md's own disclosed protocol (independent second
run) and disclose both runs — it is a known load-sensitive flake, not
this session's regression, and remains out of scope (AR-CI-1). Commit:

    docs: the colorectal investigation records its findings (ADR-0085)

Push, verify the pushed message, watch CI to conclusion for this push
(the session's own claim includes "landed green"). Then archive this
prompt verbatim to `.agents/prompts/2026-08-08-colorectal-investigation.md`,
write `.agents/session-records/2026-08-08-colorectal-investigation.md`,
index both READMEs, same commit:

    docs: session record and prompt archive — colorectal investigation

## Fences (what this session does NOT do)

No fix, anywhere, of any kind (AR-CI-1). No vendoring — even if the
diagnosis somehow clears colorectal outright, vendoring is its own ruled
session (population-scale gate law applies). No census-tool changes (the
`:closure-file-count` undercount stays untouched). No investigation of
the loopback flake beyond the disambiguation re-run. No pairing-as-data
work — it is running in the design channel. No reading-set or state.md
regeneration — that is arc-close work.

## Close-out

The session record carries: HEAD at start and end, the tag act and its
verification, exact violation counts per seed, the probe commands (or
scratch-code listings) sufficient for a future session to re-run them,
commit shas, post-push verification, CI conclusion. Echo to chat: the
diagnosis (or stop-report) in one paragraph, the counts table, the
truncation-hypothesis verdict, shas, CI status.

## ADR-0047 — Scaffolding compaction C: the continuity register lands, `/mnt/c` retires, arc closes

**Status:** Accepted (author-ruled 2026-08-05, design channel, AR-C-1
through AR-C-4 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`). Executed same
day. This session ran INVERTED: the design channel authored the
`.agents/state.md` draft directly; this session's own primary job was
per-claim verification against the live tree before landing it, never
silent retention of a failed claim.

### Context

Session C of the scaffolding-compaction arc (A — riders, vestige
retirements, Deferred triage, ADR-0045; B — the ADR split and roadmap
rotation, ADR-0046 — both landed and verified). C's own deliverable,
named but deferred at B's own close (ADR-0046 AR-B-5): the continuity
register, `.agents/state.md`, plus a seeded rulings register and the
`/mnt/c` mirror's disposition, ruled at B's own close as "retire" but
not yet executed.

### Decision

Ruled 2026-08-05, design channel, recorded verbatim:

**AR-C-1 (state.md).** `.agents/state.md` lands from the design
channel's own draft AFTER per-claim verification: every `[V]`-tagged
claim re-probed at this session's own tip; every citation resolved;
failures corrected with a note or tagged `[unverified]` — NEVER
silently kept. The file's regeneration contract (design channel, each
arc close) is recorded here as standing rule.

**AR-C-2 (rulings register, seeded not completed).** `.agents/
rulings.md` is CREATED as a seed: header stating its own contract
(standing `[A]` rulings only, each citing its ADR via the index;
appended at each arc close; NOT a history — the attic is), plus the
rulings this arc's own ADRs (0043–0047) state as standing, extracted
by reading those five attic files only. Back-filling ADR-0001..0042's
own rulings is a named future with trigger "next design-channel
onboarding that misses a rule" — recorded, not attempted (a
forty-plus-file extraction is judgment work exceeding one session's
own licensed scope).

**AR-C-3 (`/mnt/c` retirement).** Author-ruled 2026-08-05: `bin/sync-
mnt-c` retires (dated pointer in a commit note; file deleted — this IS
a licensed deletion, the arc's own only one); guarded-mirror doctrine
and dual-clone hazard language retire from `AGENTS.md` and
`.agents/skills/` current-tense surfaces (dated notes); the four-
incident ledger and the UNC resolution are this ADR's own closing
narrative. If any live automation still invokes `sync-mnt-c` (fresh
grep: Makefile, CI, hooks), STOP-AND-ESCALATE.

**AR-C-4 (arc close).** This ADR closes the scaffolding compaction arc
(A/B/C); roadmap Done pointers per AR-B-4; the Code-side memory note
on WSL-routing workarounds is superseded (session updates its own
project memory accordingly).

### Step 0 — verification table (claim → probe → disposition)

The design channel's own draft carried citations for every structural
claim; each was re-probed against the live tree at this session's own
tip (`53edcad` through this ADR's own landing) before the file below
was allowed to land. Held claims are not re-listed exhaustively here
(the file itself carries its own citations); this table records every
claim that did NOT survive verification as drafted, and what changed.

| # | Claim as drafted | Probe | Disposition |
|---|---|---|---|
| 1 | `notes/adr/` holds "43 files" | `ls notes/adr/*.md \| grep -v README \| wc -l` | **CORRECTED to 45.** The draft's "43" was accurate only for the instant AR-B-1's own split completed — ADR-0046 (compaction B itself) and this ADR each add themselves as one more file, going stale the moment the arc that produced the count closes. Same failure mode `notes/adr/README.md`'s own "43 of them" line had already fallen into (also corrected, below). |
| 2 | Component graph: `sim-engine ← {sim-check, sim-emit-fhir, sim-emit-hl7, residual sim}` | `grep -rl "ehrt.sim-engine.interface" components/*/src/` | **CORRECTED.** `sim-emit-hl7` is NOT a dependent of `sim-engine` — its own `emit_hl7.clj` requires `ehrt.sim-model.interface` only, matching AGENTS.md's own stated constraint. The real caller set is `sim-check`, `sim-emit-fhir`, `sim/identifiers.clj`, `sim/run.clj`, plus dev-only `oracle/digest.clj`. The two emitters remain ADR-0043's own narrative "siblings" (peers over one state machine) but are not siblings in the require graph — `sim-emit-hl7` reads the event log residual `sim` hands it directly, never calling back into `sim-engine`. |
| 3 | Environment: "four-incident ledger: M2 mis-target, M4 staleness, promotion memory note, compaction-A 345-line stale read" | Fresh grep of `.agents/session-records/2026-08-04-sim-split-m2-engine.md`, `-m3-emit-fhir.md`, `-m4-check.md`, `2026-08-05-standing-equipment-promotion.md` for `mnt`/`clone`/`stale`; cross-read of `notes/adr/0030-post-wave-d-cleanup.md`, `.agents/session-records/2026-08-02-migration-session-6.md`, `2026-08-05-scaffolding-compaction-a.md` | **CORRECTED — three of the four named labels do not check out.** Zero mentions of `/mnt/c`, dual-clone, or clone staleness exist in the M2, M3, M4, or standing-equipment-promotion session records — "M2 mis-target", "M4 staleness", and "promotion memory note" are not evidenced anywhere in the live tree. The real, evidence-backed ledger is **three** dated clusters: (1) Wave D stage D3, 2026-08-02 — `feedback-dual-clone-edit-hazard` fired FOUR times in that single stage alone (ADR-0030's own context), the finding that produced the J4 guardrails; (2) migration session 6, 2026-08-02 — ran natively from `/mnt/c`, found migration session 5's record and persisted agent memory both stale about its sync state; (3) scaffolding compaction A, 2026-08-05 — session default cwd resolved to `/mnt/c`, 345 lines stale (1127 vs. 1472), worked around via `wsl -e bash -lc` routing. See below. |
| 4 | AR-C-3: "guarded-mirror doctrine and dual-clone hazard language retire from `AGENTS.md`" | `grep -n "mnt/c\|dual-clone\|guarded-mirror" AGENTS.md` | **CORRECTED — nothing to retire.** Fresh grep of `AGENTS.md` found zero mentions of `/mnt/c`, dual-clone, or guarded-mirror language. The ruling's own premise (that `AGENTS.md` carries this language) does not hold against the live tree; disclosed here per this workspace's fix-forward-with-disclosure rule rather than silently no-opped. The actual current-tense surface carrying this language was `.agents/skills/build-session/SKILL.md` (both the canonical file and its `.claude/skills/` mirror) — both retired with dated notes, per AR-C-3's own intent. |
| 5 | Deferred = "13, LIVE rows only" | Direct count: `awk '/^## Deferred/,/^## Done/' roadmap.md \| grep -c '^- '` | **HELD (count correct); one drift disclosed alongside.** 13 confirmed. Incidentally found: one of the 13 rows (`myocardial_infarction.json`) carries an in-place "RESOLVED... see Done, below" note rather than having actually been relocated to Done — a pre-existing drift from 2026-08-03 that predates compaction A's own AR-A-5 sweep and evidently escaped it (the closure note sits mid-paragraph, not as a standalone closed-with-note row the way the four AR-A-5 did relocate). Out of this arc's own fence to fix; disclosed in `.agents/state.md` and here so a future Deferred-triage session doesn't have to rediscover it. |
| 6 | `.agents/reading-sets.edn` onboarding budget "1095" | `grep budget-lines .agents/reading-sets.edn` | **HELD.** Confirmed `1095` live, matching AR-B-4's own re-derivation. |
| 7 | 7-slug pre-cutover prompt/record pairing allowlist | Read `prompt_record_pairing_test.clj` directly | **HELD.** Exactly seven slugs, matching the draft. |
| 8 | Façade freeze citation ("08-02 plan AR-3; corpus depends in-process, ADR-0012") | Read `components/sim/src/ehrt/sim/interface.clj`'s own docstring | **HELD.** Matches verbatim in substance. |
| 9 | `provenance ← {corpus, sim}`, forbidden forever from any other require | `grep -rl "ehrt.provenance.interface"`; `cat components/provenance/deps.edn` | **HELD.** Both real callers confirmed; `provenance`'s own `deps.edn` carries only `malli`. |
| 10 | Census "7 tested tests" | `grep -c "(deftest" census_test.clj` | **HELD.** Exactly 7. |
| 11 | Oracle: `--declared-digest-change` flag, per-worktree classpath | Read `bin/regression-oracle` directly | **HELD.** |
| 12 | `roadmap-done-2026-08.md` "33 entries" | `grep -c "^## Done"` | **HELD.** Exactly 33; the 07 file confirmed empty (0), matching ADR-0046's own disclosed finding. |
| 13 | `.agents/` has no top-level index convention (so `state.md`/`rulings.md` need no README entry) | Read `index_completeness_test.clj`'s own `indexed-directories` list directly | **HELD.** `.agents/` top level is not a gated directory; only `plans`, `prompts`, `session-records`, `notes`, `skills` are. |

**Also corrected, found while landing (not itself a draft claim):**
`notes/adr/README.md`'s own "Not restated as a per-file list (43 of
them...)" line — same staleness class as table row 1, bumped to 45 in
the same commit as this ADR's own index-line addition.

### The real `/mnt/c` incident ledger (AR-C-3's own closing narrative)

Three dated clusters, evidence-backed, superseding the draft's own
four mislabeled entries (table row 3, above):

1. **Wave D stage D3, 2026-08-02** (`notes/adr/0030-post-wave-d-
   cleanup.md`'s own context). The `feedback-dual-clone-edit-hazard`
   fired FOUR times within that single stage alone, despite a cited
   prior lesson — "vigilance is not working as a mitigation," in that
   ADR's own words. This is the finding that produced the J4
   guardrails: `/mnt/c` made read-only, reject-all commit/push hooks
   installed into its own `.git/hooks`, `bin/sync-mnt-c` as the only
   sanctioned sync path, and the `build-session` skill's own preflight
   rule (resolve both clone roots, STOP-AND-REPORT on mistarget).
2. **Migration session 6, 2026-08-02.** The session ran NATIVELY from
   `/mnt/c` (a real Windows-launched Claude Code session, not a WSL
   one) and found both migration session 5's own record and this
   repo's persisted agent memory stale — both claimed `/mnt/c` was
   still five sessions behind, at `1dd98f8`, with a fast-forward owed
   as AUTHOR ACTION; in fact the fast-forward had already happened,
   outside any recorded session. A staleness incident in the
   TRACKING, not a mistargeted edit — the guardrails weren't tested by
   this one, the record-keeping was.
3. **Scaffolding compaction A, 2026-08-05.** The session's own default
   working directory resolved to `/mnt/c` at session start — 345 lines
   stale (1127 vs. 1472 lines of `roadmap.md`, ext4 clone of record) —
   worked around by routing every read/edit through `wsl -e bash -lc`
   against `~/src/ehr-testing-tools` instead, per the `build-session`
   skill's own preflight rule firing as designed.

**The guardrails held throughout** — no edit or commit ever actually
landed on `/mnt/c` across any of these three clusters — but staleness
adjacent to the guard (memory, default-cwd resolution) kept recurring
regardless of how well the mechanical lock worked. **The UNC
resolution** (Claude Code's own project root pointed directly at the
ext4 clone by its UNC path, `\\wsl.localhost\Ubuntu\home\mg\src\ehr-
testing-tools`, rather than at a native-Windows path that could
resolve to either clone) closes the hazard class structurally: there
is no longer a second clone root for a session's own default cwd to
drift toward. This is why AR-C-3 retires the mechanical guardrails
themselves (read-only lock, reject-all hooks, `bin/sync-mnt-c`) rather
than merely re-affirming them once more — the UNC path makes the
guarded object itself absent, not merely well-guarded.

### Execution note

**Step 0 (this ADR's own verification table, above).** Tip confirmed
`53edcad`, no disclosure needed (matches the session prompt's own
stated tip).

**Step 1 (`e99c72b`, AR-C-1/AR-C-2).** `.agents/state.md` landed with
every correction from the table above applied inline (not as a
separate errata section — each correction lives at the point of the
claim it corrects, so a cold reader never has to cross-reference).
`.agents/rulings.md` landed as a seed, standing rulings extracted from
ADR-0043 through this ADR only. Full suite: 511 assertions, 195
`deftest`s (project-wide count unchanged from baseline — no code
touched), 0 failures, 0 errors, confirmed both before and after this
step's own edits. `clojure -M:poly check`: OK.

**Step 2 (`e7646b5`, AR-C-3).** `bin/sync-mnt-c` deleted after
confirming, fresh, zero live-automation callers (`grep -rn "sync-mnt-
c" Makefile .github/ .githooks/ bin/` — all empty). The guarded-mirror
preflight step in `.agents/skills/build-session/SKILL.md` (and its
byte-identical `.claude/skills/` mirror, confirmed via direct `diff`
after editing) rewritten with a dated retirement note rather than
deleted outright — the step itself (confirm the session's edit root)
still has a job, just a smaller one now that there is no second root
to resolve. The two roadmap.md Externals rows that posed the `/mnt/c`
disposition question closed with dated notes pointing here. Full
suite green (0 failures, 0 errors) both before and after; `clojure
-M:poly check`: OK.

**Step 3 (this entry, AR-C-4).** This ADR lands; `notes/ADRs.md`
gains its index line; `notes/adr/README.md`'s own file count
corrected 43→45 in the same commit (found while landing, table
addendum above). Roadmap gets its Done pointer:

```
- 2026-08-05 — scaffolding-compaction-c — ADR-0047
```

**Oracle bracket** (`bin/regression-oracle 53edcad <this session's own
tip>`): this session touches no `src` at all (docs, one script
deletion, roadmap/skill prose) — any digest change would be
STOP-AND-ESCALATE. See the session record for the bracket's own
recorded output.

**Deftest/defspec parity:** 195 `deftest`s / 511 assertions, project-
wide, unchanged across all three steps of this session (no code
touched at any point) — a wash, not a regression, confirmed by direct
re-run after each commit.

### Arc-close statement (AR-C-4)

The scaffolding-compaction arc — A (riders, vestige retirements,
Deferred triage, ADR-0045), B (the ADR split and roadmap rotation,
ADR-0046), C (this ADR: the continuity register, the rulings seed, the
`/mnt/c` retirement) — is complete. `notes/ADRs.md` is an index over
45 per-ADR attic files; `.agents/plans/roadmap.md` is a live map with
history in two dated attic files; `.agents/state.md` gives a cold
session a citation-carrying snapshot instead of a full re-read;
`.agents/rulings.md` seeds the standing-rules register this arc's own
five ADRs state, with the other forty-two named as a future, not
avoided. The design-channel/Code-channel contract that produced all
three sessions — chat plans and verifies, Code executes and self-
archives — is itself now stated in `.agents/state.md`'s own closing
section, citable rather than tribal.

### Fence

No back-fill of ADR-0001 through ADR-0042's own rulings (AR-C-2's own
explicit scope limit — a named future, not this session's licensed
work). No deletions beyond `bin/sync-mnt-c` (the arc's own only
one) — `AUTHORS-GUIDE.md`'s own `/mnt/c` example command and
`notes/facts-register.md`'s own F7 performance figures are OUT of
AR-C-3's own named scope (neither is `AGENTS.md` or
`.agents/skills/`) and stay untouched, disclosed here rather than
silently swept beyond the ruling's own stated boundary. No new gates.
No roadmap `:paths`/reading-set membership changes. Frozen archives
(`notes/sim/`, `notes/tools/`, `notes/prompts/`, every existing
`notes/adr/NNNN-*.md` file's own body) untouched — `notes/adr/
README.md`'s file-count correction and `notes/ADRs.md`'s new index
line are the only edits to already-existing attic-adjacent files, both
mechanical and disclosed above.

---

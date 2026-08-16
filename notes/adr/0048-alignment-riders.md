## ADR-0048 — Alignment riders: small debts paid, the audit brief lands, stable tags go live

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: the scaffolding compaction arc closed and verified (`89e327f`,
`notes/adr/0047-scaffolding-compaction-c-arc-close.md`). This session opens the
alignment arc: it lands the arc's own brief and pays three pre-ruled
debts BEFORE the audit session runs, so the audit starts clean.
Docs-only — no `src/` touched at any point. The alignment-audit brief
(`.agents/plans/2026-08-05-alignment-audit-brief.md`) was placed in
the working tree by the author from the design channel's own delivered
file, per this session's own Step 0 fence (a missing or empty brief
would have been STOP-AND-ESCALATE — it was present).

### Decision

Ruled 2026-08-05, recorded verbatim:

**AR-R-1 (brief lands, verified not trusted).** The alignment-audit
brief lands with an index entry in `.agents/plans/README.md`.
Doer-checker inversion, lightweight: before landing, re-probe the
brief's probeable seeded claims against the live tree — S1 (both
`components/sim-model/resources/sim/` and `components/sim/resources/
sim/` exist; `comm -12` of their sorted file lists is empty), S2's tag
inventory (`git tag` shows exactly `stable-bootstrap`, `stable-ehrt-1`,
`stable-pre-monorepo`), S4 (zero live roadmap rows matching
`mllp|bed board|accumulator|census sink|player`, case-insensitive). A
claim that fails probe is corrected in the brief with a dated note —
never silently kept, never silently dropped.

**AR-R-2 (stable-\* tagging adopted — STANDING).** Live `stable-*`
tagging is adopted: the author tags after each design-channel-verified
landing, format `stable-YYYYMMDD-<session-slug>`, matching the
existing `^stable-.*` pattern in `workspace.edn` (no config change).
The three legacy tags stay — frozen history, superseded by the first
new stable point. Tagging remains the author's act alone (R30); this
session tags nothing. Recorded in this ADR as a standing ruling; the
register append (AR-R-5) cites it.

**AR-R-3 (player backlog resurfaces).** The corpus player's remaining
work returns to the live roadmap, each row citing `notes/ADRs.md`
ADR-0014: bed board + census sink, accumulator wiring, and the sim
event-log input adapter as Next rows; the `:mllp` transport sink as a
Deferred row carrying ADR-0014's own bail-out reasoning
verbatim-by-citation and revisit trigger "a session needs wire
transport and a lands-small shape is identified". Row text is written
fresh against ADR-0014's actual deferral language, read this session —
not from this prompt's summary of it.

**AR-R-4 (stray Deferred row comes home).** The
`myocardial_infarction.json` row relocates from Deferred to the Done
attic `.agents/plans/roadmap-done-2026-08.md`, notes intact
(relocation, not rewrite — AR-A-5), with a dated relocation note naming
this session and the drift's original disclosure (ADR-0047 / `.agents/
state.md`'s finding). The attic file's own header contract was read
before appending; the append matches its format. Deferred's live-row
count drops 13 → 12; `.agents/state.md` is NOT edited (it regenerates
at arc close only, AR-C-1 — the count divergence is expected and
disclosed below).

**AR-R-5 (rulings register append).** Two appends to `.agents/
rulings.md`, under a new "From ADR-0048" section: (a)
transcript-witnessed ≠ repo-recorded — only repo artifacts are
citable; chat-witnessed events are `[unverified]` until a record
captures them — citing ADR-0047 Step 0 as the evidencing event (it
caught the prior design channel twice, 2026-08-05); (b) AR-R-2's
stable-tagging discipline, citing this ADR. The register's own
contract says appends happen at arc close; this mid-arc append is
author-licensed here, disclosed below as a deviation-with-license, not
silently normalized.

### Step 0 — verification table (claim → probe → disposition)

| # | Claim as drafted | Probe | Disposition |
|---|---|---|---|
| 1 | S1: both `components/sim-model/resources/sim/` and `components/sim/resources/sim/` exist; no filename overlap | `find` both trees, sorted, `comm -12` | **HELD.** Both directories exist; `comm -12` of the sorted file lists is empty — no current filename collision, matching the brief's claim exactly. |
| 2 | S2: exactly three `stable-*` tags exist (`stable-bootstrap`, `stable-ehrt-1`, `stable-pre-monorepo`) | `git tag` | **HELD.** Exactly those three, no others. |
| 3 | S4: zero live roadmap rows match `mllp\|bed board\|accumulator\|census sink\|player` (case-insensitive) | `grep -inE` against `.agents/plans/roadmap.md` (the live roadmap; `notes/sim/agents/plans/roadmap.md` is frozen provenance under `notes/`, not the live document) | **HELD.** Zero matches in the live roadmap prior to this session's own Step 2 edit. |
| 4 | Working directory is the ext4 clone, tip is `89e327f` or later-with-disclosure | `pwd -P`, `git log -1` | **HELD.** `~/src/ehr-testing-tools`, tip `89e327f` exactly, matching Step 0's own premise. |
| 5 | Brief file present at `.agents/plans/2026-08-05-alignment-audit-brief.md` | `Read` the file | **HELD.** Present, 252 lines, non-empty. |

No claim failed probe; the brief lands with no correction owed.

### A ruling's own arithmetic, corrected

AR-R-4's own text states Deferred's live-row count drops 13 → 12. That
is the relocation's own isolated effect, but AR-R-3 lands in the SAME
step and adds exactly one new Deferred row back in (the `:mllp`
transport sink) — the two rulings' own row-level actions were each
executed exactly as specified, but the ruling text's summary arithmetic
did not net the two together. The actual final Deferred count, verified
by direct count after Step 2's own commit, is **13, unchanged** (12
after the relocation, 13 after the `:mllp` addition). Disclosed here
per this workspace's fix-forward-with-disclosure rule, rather than
silently editing the ruling's own verbatim text (rulings are recorded
as ruled, not corrected in place) or silently letting the wrong number
stand uncorrected. `.agents/state.md` is not edited by this session
(AR-C-1 — it regenerates at arc close only); a future arc-close session
regenerating it should expect 13, not 12, and should not read this as a
new drift.

### Deviation, disclosed (AR-R-5's mid-arc register append)

`.agents/rulings.md`'s own header states its contract: "appended at
each arc close, by the design channel, citing the closing ADR." Both
AR-R-5 appends land here instead, mid-arc, from a build session — the
author licensed this explicitly in this session's own prompt (AR-R-5:
"this mid-arc append is author-licensed here, disclosed in the ADR as
a deviation-with-license, not silently normalized"). The register file
itself carries a parallel note at its own append point, per the same
discipline ADR-0047's AR-C-3 premise-correction used: disclose in
place, don't silently treat a stated contract as inapplicable.

### Consequence

The alignment-audit brief is live, indexed, and probe-verified — the
audit session (this arc's next step, per the brief's own §6 session
shape) can proceed from it directly. The corpus player's remaining work
is roadmap-visible again (three Next rows, one Deferred row), each
citing ADR-0014's own deferral text rather than a paraphrase. The
`myocardial_infarction.json` drift ADR-0047 disclosed but left
unfixed (out of that session's own fence) is closed: relocated to the
Done attic with its own closure note intact. The rulings register
carries two new standing entries, one of them (stable-tag adoption)
licensing the author's own next act — tagging this session's verified
landing `stable-20260805-alignment-riders`, the new discipline's first
tag.

### Verification

- `bin/regression-oracle 89e327f 8cb712f` (baseline: this session's
  own pre-session tip; target: the tip immediately before this record's
  own closing commit — no `src` touched at any point in this session,
  so any digest change would have been STOP-AND-ESCALATE): `IDENTICAL:
  every root's digest matches between 89e327f and 8cb712f` — all
  ELEVEN vendored-root batches (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as expected for a docs-only session. No `--declared-digest-change`
  licensed or needed.
- Full suite (`clojure -M:poly test :all skip:integration`): 511
  `deftest`-assertions... — see this session's own record for the
  exact before/after shape; index-completeness went from 42/43 (1
  expected failure, the not-yet-landed brief) to green after Step 1,
  stayed green through Steps 2–4.
- `clojure -M:poly check`: OK, this step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every checkpoint: each showed
  exactly one delta against its own message file — `git log
  --format=%B -1`'s own trailing-newline artifact, the same known,
  harmless class prior sessions already name.

Commits, in order: `eb97f1f` (Step 1, brief lands), `9f20ba3` (Step 2,
roadmap hygiene), `8cb712f` (Step 3, rulings register append), and
this session's own closing records commit (Step 4).

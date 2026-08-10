# 2026-08-09 — ehr-testing-tools: review-2 arc close (build session)

## Context

Archived 2026-08-09. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `05b8624` (cluster B parse guards, ADR-0096) and
closed at the docs commit plus this record's own commit. Original
prompt follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

2026-08-09 -- ehr-testing-tools: review-2 arc close
Context
Conventions read at HEAD `05b8624` (cluster B parse guards, ADR-0096), design channel, 2026-08-09, verified by fresh public clone. The author ruled 2026-08-09, verbatim: "Emit the prompt for one close session." -- the review-2 arc (ADR-0092 survey; ADR-0093 rulings landing; ADR-0094 census fix; ADR-0095 cluster A; ADR-0096 cluster B) closes in ONE session. That is a deliberate, author-ruled deviation from the two-session-close standing default (ADR-0084 intake); ADR-0097 records it as such, one sentence, no ceremony beyond that.
The plan is fully executed: all six rulings ruled and landed, clusters A, B, and C consumed. What this close does: pay the tag debt, anchor the two NEW findings cluster B disclosed (the anchor law's own first scheduled trigger -- restatement at a close), take or carry the D8-4 call, and write the arc-close ADR with review 3's inheritance.
This close moves no scoreboard -- re-scoring belongs to review 3.
R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward, record HEAD (expect `05b8624`; later escalates unless explained). Commit messages ASCII-only; post-push ASCII check FIRST, standing.
Read first

1. `notes/adr/0080-quality-arc-close.md` -- the review-1 close, the structural template ADR-0097 mirrors: arc narrative, final disposition tally, inherited watch-list, open Externals restated, the close's own mechanical debt, the horizon note.
2. `notes/adr/0092-repo-review-2.md`, especially its "Carried to review 3" section (~line 312) -- restated and extended, never silently rewritten.
3. `notes/adr/0093` through `0096` -- the window; 0096's "Two new findings" section (lines ~140-183) is the anchors' verbatim source.
4. `.agents/plans/2026-08-09-repo-review-findings.md` -- the register whose 76 rows the tally accounts for.
5. `.agents/rulings.md` -- the anchor law this close executes, and the two other review-2 laws whose first enforcement this window witnessed (the ASCII check ran on every push since; the RNG-path law awaits its first measurement occasion -- say which is which).
6. `notes/adr/0096` tag-debt section -- the exact successor tag name.

Author rulings

* AR-AC-0 [A] (ADR-0096, successor tag debt): tag `stable-20260809-cluster-b-parse-guards` at `05b8624`, Step 0, ANNOTATED, standing ceremony. Verify-and-disclose if present.
* AR-AC-1 [A -- the anchor law, first scheduled execution]: two roadmap rows, wording sourced from ADR-0096's own findings section:
   1. gate-fhir permission-denied leg -> Next: small fix session; `ehrt gate fhir PATH` on an exists-but-unreadable file still raises a raw `FileNotFoundException` three frames past `core.clj` (`judge_fhir_official.fhir/gate-file` -> `verdict-cache-lookup` -> `kernel.digest/sha256-file`); the fix shape is the same categorized-rejection family, applied where the read actually lives (a fence cluster B could not cross, ruled mid-session). Cite ADR-0096 Finding 1 and the register's D8-3 root-cause correction (fix-forward: the register's cited line ranges described the sniff legs, not this one).
   2. `ehrt play`'s bare reads -> Deferred: `play-events-from-file`/`play-events-from-dir`, the identical unguarded shape, never charted by review 2; lint-allowlisted by name (the allowlist entries are this row's own tripwire -- removing them is the fix's co-landed gate, ready-made). Revisit trigger: the next session touching `ehrt play` or the corpus- player slices (bed-board sink, ADR-0014). Cite ADR-0096 Finding 2.
* AR-AC-2 [A] (D8-4, ruled 2026-08-09, verbatim: "I choose a."): bare/`help`-level unknown flags -- currently silently swallowed (help printed, exit 0) while subcommands report `:unknown-flag` -- will be ROUTED through the same `:unknown-flag` category. That is src work, so it does NOT land in this close: this close anchors it as a one-line RIDER on the gate-fhir Next row (AR-AC-1 item 1) -- same file family, same fix session, its own red->green evidence required there (a typo'd bare-level flag before: help + exit 0; after: `:unknown-flag`, the subcommand exit semantics). `docs/cli.md` is NOT touched by this close; option (b) is struck.
* AR-AC-3 [C] (ADR-0097, in ADR-0080's shape):
   * Arc narrative: survey -> rulings landing -> census fix -> cluster A -> cluster B, one paragraph each, evidence-linked.
   * Final disposition tally: the register's 76 rows fully accounted -- 57 close-as-fine stand; 8 fix-candidates landed (name which ADR landed each); 5 ruling-needed all ruled (quote the author's verbatim rulings); 5 intake carried; the 1 non-tallied cross-reference noted.
   * Deviations sweep, BOTH sides, fix-forward: the design channel's two unearned-specificity instances (the "no Wave-E row" claim, corrected by AR-RL2-4's own verify-then-add; the Done-notes fence wording, corrected by the census session against AR-B-4); cluster B's charter premise mismatch (gate fhir) and its mid-session ruling; the `play` regression caught in-flight by the full suite before landing; the prompt-archive transport reflow (content-verbatim, not byte-verbatim -- the em-dash incident's benign sibling); the design channel's CI-API verification gap this window (run conclusions carried on session claims plus clone consistency, never independently confirmed).
   * Review 3's inheritance: ADR-0092's carried-to-review-3 section restated; PLUS the two anchored findings; PLUS the lint allowlist as a named watch item; PLUS D8-4 ruled (a), riding the gate-fhir fix session, not carried open.
   * Open Externals restated unchanged (NIST licensing, IG pinning, SETUP rewalk -- the 0080 pattern).
   * The close's own mechanical debt: successor tag `stable-20260809-review-2-arc-close` at this session's closing tip; the one-session-close deviation sentence.
   * Horizon note: the veteran family arc (Batch 4) opens next; the injuries-family/busy-board idea remains an un-committed author aside, noted without a row (no commitment ruled).
* AR-AC-4 [C]: no register edits, no re-scoring, no new laws.

Steps
Step 0 -- Preflight + tag (AR-AC-0). Standard preflight (clean tree, HEAD `05b8624`, untracked disclosure, `clojure -M:poly check`, oracle pre-digest `05b8624 05b8624` -- 34 roots IDENTICAL expected; all lanes' latest conclusions disclosed). Tag. No commit.
Step 1 -- Self-archive FIRST (standing law: prompts archive at the START of the close phase). Write `.agents/prompts/2026-08-09-review-2-arc-close.md` (verbatim prompt + deviation record placeholder) now; it lands in the MAIN commit so an interrupted close still leaves provenance. No commit yet.
Step 2 -- Anchors (AR-AC-1/2). The two roadmap rows, the D8-4 rider line on the gate-fhir row. No `docs/cli.md` touch. No commit yet.
Step 3 -- ADR + ceremony surfaces + commit (AR-AC-3). `notes/adr/0097-review-2-arc-close.md`; index line; README count 94->95; roadmap Done pointer. Full local suite (the window's own gates -- classpath, parse-guard lint, continuity -- all run inside it). Single commit:

```
docs: review-2 arc closes -- plan executed in full, two findings anchored (ADR-0097)

```

Push; ASCII check FIRST, then message verification; watch CI to conclusion, all lanes noted.
Step 4 -- Ceremony. Session record (`2026-08-09-review-2-arc-close.md`) + both READMEs (the prompt archive already landed in Step 3's commit; the pairing gate sees both by suite time), one commit:

```
docs: session record -- review-2 arc close

```

Same verification order.
Fences
No src anywhere -- the D8-4 routing is ANCHORED here, landed in the gate-fhir fix session, never in this close. No fixes of any kind, including the two anchored findings. No register edits. No law appends. No scoreboard. No `docs/cli.md` touch. Roadmap: the two named rows (one carrying the D8-4 rider line) + Done pointer, nothing else moves. If the suite or any gate goes red on this docs-only close, STOP-AND-REPORT -- a red here means the window left something undisclosed, and that finding outranks the close.
Close-out
Echo to chat: the anchor rows as landed; D8-4's disposition; the deviations list as recorded (both sides); review 3's inheritance in brief; the disposition tally's four numbers; shas, CI status across all lanes.

## Deviation record

- **Step 0 preflight, an already-stale CI red disclosed rather than
  investigated as new.** `gh run list --workflow=integration.yml
  --limit 3` showed the Integration lane's third-most-recent run
  (`31301880957`, 2026-08-09T07:45:13Z, `schedule` trigger) concluded
  `failure`. Checked before treating it as a live finding: its own
  `headSha` is `b7a1dc88` ("docs: session record and prompt archive --
  vendoring batch 4"), a commit that predates `2088763` (the
  classpath-break fix landing `judge-v2-nist` back onto the
  `integration` project, 2026-08-09T06:14:39-04:00 = 10:14:39 UTC) —
  the scheduled run's own trigger queued against a stale checkout
  rather than the tip current at 07:45 UTC. The failure log is the
  exact, already-disclosed H-4/`2088763` classpath-break signature
  (`FileNotFoundException: ...judge_v2_nist/interface...`), not a new
  incident. The two Integration runs after it (`31308023126` 10:18
  UTC, `31312458033` 12:06 UTC, both `workflow_dispatch`) are green,
  confirming the fix holds at the tips those runs actually checked out.
  Disclosed here rather than escalated as a fresh STOP-AND-REPORT
  trigger, since it resolves to already-known, already-fixed history
  on direct inspection, not live drift.
- **Step 3/Step 4's own push split, corrected before either push.** The
  driving prompt's own Step 1 rationale ("Self-archive FIRST... it
  lands in the MAIN commit so an interrupted close still leaves
  provenance") puts the prompt archive in Step 3's own commit while
  the session record waits for Step 4 — but Step 3's own text also
  says to push and watch CI to conclusion before Step 4 runs. Running
  the full local suite at Step 3, as instructed, surfaced the real
  mechanical conflict this ordering creates:
  `ehrt.docs-tooling.prompt-record-pairing-test` fails with the prompt
  archive present and no paired session record yet
  (`#{"2026-08-09-review-2-arc-close"}` reported as an orphan) — a
  real, reproducible red, not a hypothetical one. Every prior session
  in this window (0092 through 0096) bundled the prompt archive and
  session record into ONE final ceremony commit precisely to avoid
  this; pushing Step 3 alone here would carry the same orphan to CI's
  own push-lane run and go red there too, exactly what this close's
  own Fences section names as outranking the close. Resolved
  mechanically, without touching any fenced content: Step 3's commit
  (ADR + roadmap anchors + index + count + prompt archive) and Step
  4's commit (session record + READMEs) both land locally, in that
  order — preserving the stated provenance goal, since the prompt
  archive reaches git history in the main commit even if the session
  is interrupted before Step 4 — then both are pushed together in ONE
  `git push`, so CI's own push-lane run only ever evaluates the final
  tip, which carries both files. The full local suite was re-run after
  both commits' own files existed on disk, green throughout (see the
  ADR's own Verification section). Post-push message verification and
  the ASCII check ran against EACH commit individually; CI was watched
  to conclusion once, against the pushed tip.

2026-08-06 — ux arc close: the founding incident is mechanically impossible — appended, regenerated, rotated
Session prompt (design channel, 2026-08-06). Prior: ux fixes 5 landed and was design-channel-verified (`f5af489`, ADR-0063); every fix cluster of the UX arc (ADR-0056 through ADR-0063, tag-law included) is landed and verified. This session CLOSES the arc per the standing close pattern (ADR-0055 is the model): rulings appends, the dependency-review cadence's execution, `state.md` regeneration, budget re-derivation, Done rotation, the register's final tally, and the closing ADR. Docs-only; anything new found is next-arc intake, never an act. R30 ceremony. Read-first: `notes/adr/0055-alignment-arc-close.md` (the pattern, including its regeneration-table and tally formats); `.agents/rulings.md` in full; `.agents/state.md` in full; the UX register (`.agents/plans/2026-08-06-ux-audit-findings.md`); `.agents/reading-sets.edn`; ADR-0060/0062's disclosed evidence-method corrections (they feed append (c)).
Author rulings (record verbatim in ADR-0064)

1. AR-UC-0 (tag, standing ceremony). Annotated `stable-20260806-ux-fixes-5` at `f5af489`, message `ux fixes 5 landed, design-channel-verified 2026-08-06 (ADR-0063)`; push; verify.
2. AR-UC-1 (rulings appends). Three, under "From the UX arc (ADR-0056–0064)", each citing its recording ADR, wording condensed from those ADRs' verbatim text: (a) two voices, two homes, standing — user-facing surfaces (help, errors, command-bearing docs) speak operator language; maintainer content (citations, milestone history, internal names) lives in source comments and dev docs, relocated never deleted (brief §3, executed by ADR-0062); (b) errors name their artifact, standing — every operational error names the concrete thing it could not find or parse, with a next step where one exists; unknown input is rejected by name, never silently accepted (ADR-0060, ADR-0061); (c) audit evidence uses the mechanism it recommends, standing — fence verification resolves paths rather than parsing grammar, and string inventories walk the data the gate will walk rather than grepping source; two same-arc instances of the cheaper method being wrong: AR-U2-R's non-resolving fences (ADR-0060) and the 38-vs-36 token count (ADR-0062).
3. AR-UC-2 (cadence execution). `clojure -M:poly libs :outdated` per the standing dependency-review rule; the dated report lands in ADR-0064; no edit of any kind follows from it; an urgent-looking upgrade is an intake note.
4. AR-UC-3 (state.md regenerates). Per its contract and the ADR-0055 pattern: every `[V]` claim probe-backed THIS session; skeleton preserved; content updates at minimum — the gate inventory (now grown by: invocation-lint with path resolution, unknown-flag validation, the voice gate, the wrap width+content-preservation tests, tag-law's phrase gate, license-text pointer); the tag law's current form (sessions execute stable tags under license or standing ceremony, AGENTS.md/skill/register all reconciled, tag count by fresh `git tag` census); the CLI's user-facing posture (Result-vocabulary errors with did-you-mean, spec-derived flag validation, operator-voice help with 80-column wrap — the founding incident's four failures each now gate-guarded); both audit registers' pointers with final-tally status; Deferred/Next counts by fresh count; component graph re-probed (expected unchanged). Regeneration table (claim → old → new → probe) in ADR-0064; a wrong-in-kind discrepancy is STOP-AND-REPORT.
5. AR-UC-4 (budgets). Re-derive per the standing formula every reading set whose members changed during this arc (enumerate against `git log 12d3aa3..HEAD --name-only`; `AGENTS.md` changed via ADR-0057 and `state.md` regenerates here, so `:onboarding` at minimum), AFTER the regeneration lands.
6. AR-UC-5 (rotation). The Done section currently holds ADR-0055 through ADR-0063 (design-channel probed). Rotate: ADR-0055's pointer appends to the attic's EXISTING alignment-arc section with a one-line dated note (its own close left it as the then-current entry; disclosed leftover, same class ADR-0055 itself disclosed for the compaction pointers); ADR-0056–0063 rotate under a new `## UX arc — closed 2026-08-06 (ADR-0056–0064)` header. ADR-0064's pointer then lands as the sole current entry. Relocation-not-rewrite throughout.
7. AR-UC-6 (the closing ADR). `notes/adr/0064-ux-arc-close.md`: rulings verbatim; the arc summary one line per session (riders, tag-law, audit, fixes 1–5) with ADRs and tips; the UX register's final disposition tally (fresh count against the register itself — the alignment close caught its register undercounting; check for the same); the founding-incident closure narrative, four failures to four mechanisms: stale invocation → lint+path gates; opaque config crash → named Result with did-you-mean; silent typo → spec-derived rejection; agent-voice help → operator voice, gated, wrapped; the intake list for the next arc (any `--width`/COLUMNS note ADR-0063 carried; the module-vendoring candidate from the founding conversation, 2026-08-06, feature-shaped; anything the sessions noted); the open Externals restated unchanged (NIST licensing inquiry, IG pinning, SETUP rewalk); and the horizon note: "The horizon is feature-shaped: corpus-player slices (roadmap Next, ADR-0014), the pairing-as-data design pass (design channel, landing spot `judge`), module vendoring widening the ailment mix (intake, unruled), `sim-emit-cda` on its trigger. Publish-prep gates: F-5/F-6 decisions plus the alignment register's F-7 checklist."

Steps
Step 0 — Preflight + tag. Cwd ext4; tip `f5af489` or later-with-disclosure; full suite green baseline; oracle pre-digest. Execute AR-UC-0.
Step 1 — Appends + report (AR-UC-1/2). Commit: `docs: the ux arc's law is appended — two voices, named artifacts, honest evidence (arc close, AR-UC-1/2)`
Step 2 — State + budgets + rotation (AR-UC-3/4/5). Full suite green through the rotation (index/pointer gates). Commit: `docs: the state regenerates, the budgets re-derive, two arcs rest in the attic (arc close, AR-UC-3/4/5)`
Step 3 — ADR-0064 + record. Everything per AR-UC-6; index line; sole Done pointer. Oracle bracket (`f5af489` → tip): all ELEVEN batches identical — docs-only; any change is STOP-AND-ESCALATE. Session record + prompt self-archive. Final commit: `docs: the ux arc closes — a stranger can be handed this CLI (ADR-0064)`
Fences
Docs-only: no `src/`, no `test/`, no config, no Makefile, no gates. Both audit registers read-only — final tallies live in ADR-0064. The horizon note restates ruled directions plus NAMES unruled intake; it decides nothing. The `libs :outdated` output is a report, full stop. Frozen archives untouched (ADR-0064 + index + the live-attic appends sanctioned). A state-regeneration discrepancy wrong in kind is STOP-AND-REPORT.
After landing: design channel verifies by fresh probe — sampled re-probes of the regenerated state.md included — then the next arc's opening session tags `stable-20260806-ux-close` at this tip under standing ceremony, and the horizon is the author's to rule.

## Deviation record

- **The `clojure -M:poly test :dev :ehrt/docs-tooling` scoped-project
  invocation (ADR-0060's own documented shortcut for a targeted run)
  returned zero selected tests against this session's own workspace
  state** ("Execution time: 0 seconds", no `Test results:` line) —
  not investigated as a tooling defect (out of this session's own
  docs-only fence), worked around by relying on
  `clojure -M:poly test :all skip:integration` as the primary
  verification at every checkpoint instead, matching the full-suite
  count (222 `Test results:` lines, 0 failures/0 errors) against the
  Step 0 baseline each time. No claim in ADR-0064 rests on the scoped
  command's own output.
- No other deviation from the prompt's own rulings. AR-UC-0 was
  executed directly by this session (not merely licensed) — correct
  under the current tag law (`notes/adr/0057-tag-law.md` AR-T-1,
  case (ii): a session tags its own predecessor's design-channel-
  verified stable point as standing ceremony, without further
  license), unlike ADR-0056's own AR-U0-2 which predates that law's
  own reconciliation and stayed licensed-not-executed.
- Step 2's own rotation surfaced a live near-miss, caught and
  corrected before any commit landed, documented in ADR-0064's own
  Step 2 section rather than repeated here: this session first added
  ADR-0064's own Done pointer to the live roadmap in the same step as
  the rotation, then ran the full suite as usual — the gate genuinely
  fired (`done-pointer-adr-test` failed, citing an ADR number not yet
  present in `notes/ADRs.md`'s own index). The line was removed and
  the pointer deferred to Step 3 instead, the same sentinel-avoidance
  ADR-0055's own AR-AC-5 had already disclosed for its own equivalent
  moment. Disclosed as a live-caught mistake, not an anticipated one —
  the gate did its job.

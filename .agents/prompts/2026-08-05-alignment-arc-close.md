# 2026-08-05 — Alignment arc close

Context: `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`),
tip `2b3bb2b` at session start (`notes/adr/0054-alignment-fixes-5.md`,
alignment fixes 5, design-channel-verified). Session record:
[`2026-08-05-alignment-arc-close.md`](../session-records/2026-08-05-alignment-arc-close.md).
Decision-of-record: `notes/adr/0055-alignment-arc-close.md`.

## Prompt, verbatim

2026-08-05 — alignment arc close: the register empties, the state regenerates, the law is appended
Session prompt (design channel, 2026-08-05). Prior: alignment fixes 5 landed and was design-channel-verified (`2b3bb2b`); every fix cluster of the alignment arc (ADR-0048 through ADR-0054) is landed and verified. This session CLOSES the arc: the pending rulings-register appends land, `state.md` regenerates against the live tree per its own contract, reading-set budgets re-derive, the arc's Done pointers rotate to the attic, and the closing ADR records the final tally. Docs-only; nothing new is designed or fixed here — anything found is a note for the next arc's intake, never an act. R30 ceremony. Read-first: `.agents/rulings.md` in full (its contract header governs the appends); `.agents/state.md` in full (its contract header governs the regeneration); `notes/adr/0050-alignment-fixes-1.md` AR-F1-6; `notes/adr/0051` AR-F2-0 and `notes/adr/0053` AR-F4-4 (the law-surface lesson's two instances); `.agents/reading-sets.edn` and AR-D-3's formula; the compaction-B attic rotation as the pattern model (`.agents/plans/roadmap-done-2026-08.md` header + ADR-0046).
Author rulings (record verbatim in ADR-0055)

1. AR-AC-0 (tag). Annotated tag `stable-20260805-alignment-fixes-5` at `2b3bb2b`, message `alignment fixes 5 landed, design-channel-verified 2026-08-05 (ADR-0054)`; push; verify on origin.
2. AR-AC-1 (rulings appends — the arc-close contract executes). Three appends to `.agents/rulings.md` under a "From the alignment arc (ADR-0048–0055)" section, each citing the ADR that recorded it: (a) A-3's dependency-review cadence — report-only `clojure -M:poly libs :outdated` at each arc close plus mandatory before any publish, upgrades never taken as a side effect (ADR-0050 AR-F1-6a); (b) D-3's acceptance — `judge` is the pairing-as-data registry's landing spot for the design pass (ADR-0050 AR-F1-6b); (c) the law-surface propagation lesson — an amendment to standing law lands on every surface that states the law, in the same session that rules it; two instances this arc: the AGENTS.md tag rule (ADR-0051 AR-F2-0) and the vendoring prescription surfaces (ADR-0053 AR-F4-4). Wording drawn from those ADRs' verbatim text, condensed per the register's house style.
3. AR-AC-2 (first A-3 execution). Run `clojure -M:poly libs :outdated`; the output lands as a dated report section in ADR-0055 — coordinates, current, available, nothing else. NO deps.edn edit of any kind; if the report is empty, say so; if a listed upgrade looks urgent (a security-relevant major), that is a NOTE for next-arc intake, not an act.
4. AR-AC-3 (state.md regenerates — AR-C-1's duty). `.agents/state.md` is regenerated in place: contract header and section skeleton PRESERVED; every `[V]` claim in the regenerated file backed by a probe run THIS session (that is what regeneration means — no claim survives on the prior version's own authority). Content updates at minimum: the gate inventory (the tripwire's widened scope + five new gates: sim-emit-hl7 dependency law, provenance leaf law, root-alias completeness, resource nesting, license-text pointer); the tag mechanic (sessions tag when licensed post-verification, AGENTS.md reconciled, six `stable-*` continuity tags live as of this session's Step 0); the NIST supply-chain posture (user-side mirror, `verify-nist-lock` wired into `make test`, redistribution foreclosed per ADR-0005's amendment, inquiry still an open External); `workspace.edn`'s slimmed shape and the development project's documented `:necessary ["oracle"]`; the alignment arc's pointer (register at `.agents/plans/2026-08-05-alignment-audit-findings.md`, dated audit artifact, dispositions final per ADR-0055); current Deferred/Next counts by fresh count; component graph section re-probed (expected unchanged — 18 components, 1 base; sim-model's resources now self-named). Anything in the old state.md that fails this session's re-probe is corrected with the probe cited — and listed in ADR-0055's regeneration table (claim → old → new → probe).
5. AR-AC-4 (budgets — AR-D-3). Identify every reading set in `.agents/reading-sets.edn` whose member files changed during this arc (at minimum any set containing `AGENTS.md` or `state.md`; enumerate by diffing member paths against `git log 89e327f..HEAD --name-only`). Re-derive each affected budget by the standing formula (actual size × 1.15, rounded to the nearest 5) AFTER the state.md regeneration lands, so the numbers reflect the final tree. Unaffected sets untouched.
6. AR-AC-5 (Done rotation — compaction-B pattern). The arc's Done pointers (ADR-0048 through ADR-0054) relocate verbatim from the roadmap's Done section to `.agents/plans/roadmap-done-2026-08.md` under a dated arc header (`## Alignment arc — closed 2026-08-05 (ADR-0048–0055)`), matching the attic's format contract. ADR-0055's own pointer then lands as the Done section's sole current entry. Relocation-not-rewrite; the attic append is the sanctioned live-attic act.
7. AR-AC-6 (the closing ADR). `notes/adr/0055-alignment-arc-close.md`: rulings verbatim; the arc summary (riders, audit, five fix sessions — one line each with their ADRs and tips); the register's FINAL disposition tally (closed this arc / deferred-to-publish-prep: F-5's coordinate decision and F-6's artifact shape / standing-ruling-recorded / recommendation-only: S7's implemented, B-8/B-9/B-12's methodology caveats stand); the A-3 report; the state.md regeneration table; the budget re-derivations; the open Externals restated (NIST licensing inquiry — narrowed per the evidence doc; IG pinning; SETUP rewalk); and the horizon note verbatim: "The horizon is feature-shaped: corpus-player slices (roadmap Next, ADR-0014), the pairing-as-data design pass (design channel, landing spot `judge` per D-3), `sim-emit-cda` when its trigger fires (framing per register D-2). Publication readiness gates: F-5/F-6 decisions + the pre-publish checklist per F-7."

Steps
Step 0 — Preflight + tag. Cwd ext4; tip `2b3bb2b` or later-with-disclosure; full suite green baseline; oracle pre-digest. Execute AR-AC-0.
Step 1 — Appends + report (AR-AC-1/2). The three register appends; the `libs :outdated` run captured. Commit: `docs: the arc's law is appended — cadence, landing spot, and the propagation lesson (arc close, AR-AC-1/2)`
Step 2 — State + budgets + rotation (AR-AC-3/4/5). The regeneration with its probe log; budgets re-derived; Done rotated. Full suite green (index/pointer gates must hold through the rotation). Commit: `docs: the state regenerates, the budgets re-derive, the arc rotates to the attic (arc close, AR-AC-3/4/5)`
Step 3 — ADR-0055 + record. Everything per AR-AC-6; index line; ADR-0055's Done pointer as the sole current entry. Oracle bracket (`2b3bb2b` → tip): all ELEVEN batches identical — docs-only; any change is STOP-AND-ESCALATE. Session record + prompt self-archive. Final commit: `docs: the alignment arc closes — examined, ruled, fixed, gated, recorded (ADR-0055)`
Fences
Docs-only: no `src/`, no `test/`, no `deps.edn`, no `workspace.edn`, no Makefile, no gate changes. The audit register is read-only — final dispositions live in ADR-0055, the register stays a dated artifact. No new design work: the horizon note RESTATES ruled directions, it does not extend them. Frozen archives untouched (ADR-0055 + index + the live-attic append sanctioned). The `libs :outdated` output is a report and nothing else. If the state.md regeneration surfaces a discrepancy too large for a probe-cited correction (a structural claim that is wrong in kind, not degree), STOP-AND-REPORT — a broken continuity register is a design-channel problem, not a session's to patch silently.
After landing: design channel verifies by fresh probe — including sampled re-probes of the regenerated state.md's `[V]` claims — then the author licenses the arc's final tag (`stable-20260805-alignment-close`), and the next arc opens feature-shaped.

## Notable deviations, disclosed

- **AR-AC-5's own "sole current entry" phrasing conflicted with the live
  tree.** The ruling names only ADR-0048 through ADR-0054 for
  relocation, but the live roadmap's own Done section also carries
  three leftover pointers from the already-closed scaffolding-
  compaction arc (ADR-0045/0046/0047), never rotated at that arc's own
  close. Read literally: "sole current entry" for the alignment arc,
  not license to also relocate a different, unnamed arc's own leftover
  pointers. Resolved by taking the ruling's own explicit ADR range at
  face value and disclosing the leftover three as a small, named
  cleanup for a future session — not silently swept, not silently left
  unexplained either. Full reasoning in `notes/adr/
  0055-alignment-arc-close.md`'s own Step 2 account.
- **Sequencing correction: ADR-0055's own Done pointer could not land
  in Step 2.** AR-AC-5's own text ("ADR-0055's own pointer then lands
  as the Done section's sole current entry") reads as if it happens
  alongside the rotation. It cannot: `notes/ADRs.md` has no ADR-0055
  index line until Step 3 lands, and citing it early would trip
  `ehrt.docs-tooling.done-pointer-adr-test`'s own dangling-reference
  gate. The Steps section itself already sequences this correctly
  ("Step 3 — ADR-0055 + record... index line; ADR-0055's Done pointer
  as the sole current entry") — the pointer landed in Step 3, matching
  the Steps section's own explicit order over AR-AC-5's own more
  compressed prose.
- **The register's own summary-line arithmetic does not check out.**
  "47 total new+seeded rows," "close-as-fine 26, ruling-needed 12,
  fix-session-candidate 10... incomplete 3" (summing to 51, not 47) —
  a fresh direct count of the live register found 54 total rows, 13
  ruling-needed, 9 fix-session-candidate. Disclosed in ADR-0055's own
  tally section rather than silently repeating the register's own
  stale summary; not investigated further, per this arc's own
  precedent for count drifts of this size.
- **D-4 (named-future list hygiene) was never taken up by any of the
  five fix sessions** — found by grepping all five ADRs for its own
  subject matter and finding nothing. Named as a genuine, honest gap
  for the next arc's own intake in ADR-0055, not silently dropped and
  not fixed here (this session's own fence forbids new fixes).
- **The NIST licensing inquiry's own citation** (`docs/experiments/
  EXP-SBOM-inquiry-draft.md`, as ADR-0053 states it) does not resolve
  to a real file — corrected in the regenerated `state.md` to the real
  path (`components/corpus/docs/experiments/EXP-SBOM-results.md`) after
  a direct file-existence probe, per this arc's own evidence-over-
  ruling discipline.

No other deviation. Every step's own full suite/`poly check`/gitleaks/
oracle-bracket run is recorded in `notes/adr/
0055-alignment-arc-close.md`'s own Verification section, not repeated
here.

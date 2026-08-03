# 2026-08-02 — GMF coverage Wave D stage D3 session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`) — a dual-clone edit mismatch was
caught and corrected at Step 0 (the ADR/roadmap rider edit initially
landed on the native `/mnt/c` clone before being copied to the ext4
clone of record and reverted on `/mnt/c`), the same mistake this
prompt's own predecessor session already hit twice. The `/mnt/c` clone
was 8 commits behind `origin/main` at session start (fast-forwarded
before any edits began); the ext4 clone was already current.

## Prompt, verbatim

> 2026-08-02 — ehr-testing: Wave D stage D3 — mechanisms (lookup tables, attribute weights, compound guards)
> Context
> Final session of GMF coverage Wave D (ADR-0029; D2 landed `a41d8c2..d23fa9b` — mechanism complete, zero vendored, honestly). D3 builds the arc's three outstanding MECHANISMS and closes the wave: (1) `lookup_table_transition` (the sixth transition kind, ADR-0027's D6 finding — UTI's entry-path gate, and present ×39 in MI's closure, so its leverage outlives this session); (2) attribute-weighted `distributed_transition` weights (Wave C's stroke finding — the MECHANISM lands; stroke itself stays blocked, see H3); (3) compound-Guard analytical resolution (D2's escalated finding — ruled into D3 2026-08-02: extending the age-jump machinery so total_joint_replacement's compound Age guard stops blocking walks at age 0). Target payoffs, each conditional on its gate: `urinary_tract_infections.json` (full re-characterization — its D1-era blockers `DiagnosticReport`/`MultiObservation` are now built, its `type_of_care_transition` built, its lookup tables land here) and `total_joint_replacement.json` (closure surveyed clean in D2; its sole remaining blocker is the compound guard). Zero, one, or both vendored are acceptable outcomes — the gates decide.
> Regression oracle: fixed-seed runs of all six vendored roots byte-identical before and after every commit, INCLUDING every emitted HL7 byte.
> Ceremony: R30-mode — commit and push at each checkpoint, unattended, with R30's safeguards (staged-scope `--stat` check per checkpoint, personal-info scan, message via the Write tool, session record before final push, hooks as backstop; tags and repo-level `gh` outside the grant). Work in the WSL ext4 clone — and per the D2 session record's dual-clone lesson, use the UNC path for ALL edits from the start; fast-forward to `origin/main` (at or past `d23fa9b`), record HEAD.
> Read first
>
> 1. `notes/ADRs.md` ADR-0029 (R4's data-file-closure ruling, the specify-vs-delegate principle, D2's execution record, the D3 placeholder) and ADR-0027 (D6's lookup_table finding, D7's hidden-import discipline).
> 2. `components/sim-trajectory/docs/gmf-interpreter.md` — §11's D2 characterization (UTI's 12-file closure survey, TJR's compound-guard finding at line ~1130, the `age-guard-jump-days` analysis), the stroke survey row (H3's subject), the order contract.
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/ gmf_interpreter.clj` — `age-guard-jump-days`/`guard-step` (the machinery H4 extends), `run-module`'s initial-attributes arity (D2's extension, scoped by this session's Step 0 rider).
> 4. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` — `load-closure` (gains data-file members per H2), the transition schema (gains the two new forms).
> 5. Wave B/C/D1/D2 session records for the pinned fetch method; the vital-sign table's NOTICE/facts-register pattern (H2 mirrors it for CSVs).
>
> Author rulings (ruled 2026-08-02, design channel)
>
> * H1 — Each mechanism's semantics are pinned from Synthea source at the pin BEFORE its implementation commit, recorded in the ADR D3 placeholder: `LookupTableTransition`'s dispatch rule (key columns, row matching, the probability draw), the attribute-weight resolution rule (when the attribute is read, fallback semantics), and the compound-Guard forms TJR actually exercises. Any rng draw any mechanism adds joins the documented order contract. One commit per mechanism.
> * H2 — Lookup-table CSVs are closure DATA MEMBERS per R4: resolved by `load-closure` from the module's own table references, vendored beside the modules with sha256 lineage recorded in the NOTICE and a facts-register entry (the vital-sign table's pattern; CSVs carry no in-file header comment — the NOTICE carries provenance). The specify-vs-delegate audit applies to table KEY COLUMNS: a table keyed on person fields the persona genuinely supplies is buildable; one keyed on fields the persona lacks is an ESCALATION with the column named — not a silent default, not a fabricated field.
> * H3 — The attribute-weight mechanism landing does NOT unblock stroke, and the survey note must say so in so many words: stroke's artifact SPECIFIES `default: 0` with no in-project source for `stroke_risk` — per the specify-vs-delegate principle that stays blocked until the risk-source item (its own roadmap row) is ruled. The mechanism is built for schema honesty and for whatever module exercises it legitimately, with a loader/interpreter test using a fixture, not stroke.
> * H4 — Compound-guard resolution extends `age-guard-jump-days` under a sound-jump-or-escalate rule: a compound containing an Age condition may be jumped only to a bound provably no later than the earliest time the compound could become true (then re-evaluated); any form where no sound bound exists is an escalation with the form quoted, not a heuristic jump. Installed ≠ used: build the forms TJR exercises; name the rest.
> * H5 — Gates. UTI: FULL re-characterization — fresh fetch of the complete closure at the pin (do not trust the D2-era file list), survey rows, all-seven-kind sweep, D7, specify-vs-delegate audit including every lookup table's key columns; declared scope from evidence. TJR: re-verify the D2 fetch by hash (re-fetch only on mismatch), then its gate is H4 landing plus its D2 survey standing.
> * H6 — Vendored tests. UTI: entry-path lookup dispatch proven BOTH ways (seeds reaching Cystitis and Pyelonephritis), a `type_of_care_transition` path taken, cross-boundary encounter events asserted (Wave B's deferred (f) check finally exercised), full engine/check run. TJR: a walk that provably ADVANCES past the compound age guard, the care-plan span with G3's silence assertion, the initial-attributes seeding disclosed in the test's own docstring, full engine/check run. Mixer-RNG seed discipline throughout.
> * H7 — Step 0 rider (ruled 2026-08-02): ADR-0029's D2 execution record gains the scoping line — "initial-attributes is for walk-entry inputs standing in for out-of-closure writers, per-use disclosed in the vendored test — not a general cross-module channel (ADR-0027 D1: cross-module facts travel through clinical state)."
> * H8 — D3 closes the wave. The close-out writes a Wave D retrospective note in the coverage plan: the payoff tally as it actually happened (per wave: what was predicted, what landed, what moved), the standing named items (stroke risk source; ImagingStudy/CHF; SupplyList/Counter/MI's 27-file closure; the compound-guard forms not built), and an S4-trigger status line (whether any Wave D work created a second engine consumer — expected answer: no, `emit-state` remains the sole direct reader, S4 stays deferred).
>
> Steps
>
> 0. Records. H7's rider on ADR-0029; roadmap D3 → Now; ADR D3 placeholder session-start note (H1–H8 cited). Commit: `docs: D3 session start (H1-H8; initial-attributes scoping rider)`.
> 1. Characterize per H1/H5. Fetch UTI's full closure + its lookup tables + Synthea's LookupTableTransition, attribute-weight, and Guard sources at the pin (blocked-on-fetch stops after Step 0); hash-verify TJR's D2 fetch. Land the semantics pins, UTI survey rows, D7 sets, key-column audits, and the DECLARED SCOPE in the ADR placeholder and survey. Regression baseline hashes. Commit: `docs(sim-trajectory): D3 characterization -- mechanism semantics, UTI closure re-survey, declared scope`.
> 2. Implement per H1–H4, one commit per mechanism, red→green, oracle green, order contract current: (a) `feat(sim-trajectory): lookup_table_transition + closure data members (D3 H2)` (b) `feat(sim-trajectory): attribute-weighted distribution weights (D3 H3 -- stroke stays blocked, survey noted)` (c) `feat(sim-trajectory): compound-guard analytical resolution (D3 H4)`
> 3. Vendor the declared scope per H6, one commit per root: `feat(sim-trajectory): vendor <module> closure (D3 payoff)`. Drops recorded per the established pattern.
> 4. Close out per H8. Full suite + `poly check` green; oracle byte-identical finally; docs fix-forward (kind tables — six kinds with dispositions; deferred tables; payoff map; stroke note); ADR-0029 D3 execution + deviation records AND the wave-close note; coverage-plan retrospective; roadmap D3 → Done with shas and Wave D → closed; session record; self-archive this prompt to `.agents/prompts/`. Final commit: `docs: D3 records; Wave D close (ADR, retrospective; archives prompt)`.

## Deviation from the prompt's own literal Step sequence

The prompt's own Step 1/2 ordering (characterize everything, THEN
implement all three mechanisms) held for H1-H4's own three named
mechanisms exactly as written. Additional characterization surfaced
DURING Step 2/3's own build and vendoring — not fully anticipated by
Step 1 alone, since real closure content (`uti/ambulatory_path.json`'s
own `risk-check`/`Telephone_Encounter` states, real CSV bytes) only
fully exercises the loader/interpreter once actual vendoring is
attempted — was handled the same way ADR-0027/ADR-0029's own prior
waves already did: disclosed in its own commit, at the point it
surfaced, not silently folded into an existing commit or held back
until Step 4. Full account: this session's own record,
`.agents/session-records/2026-08-02-gmf-coverage-wave-d-stage-d3.md`.

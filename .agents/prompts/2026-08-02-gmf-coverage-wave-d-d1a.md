# 2026-08-02 — GMF coverage Wave D stage D1a session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). The `/mnt/c` clone was found 32
commits behind `origin/main` at session start (had never fetched) and
fast-forwarded to match; the ext4 clone was already at `origin/main`,
`7776098`, confirmed via `git fetch` — exactly the HEAD the prompt
itself named (D0's own final commit).

## Prompt, verbatim

> 2026-08-02 — ehr-testing: Wave D stage D1a — observation-family
> characterization (halts for ruling)
>
> Context
> Second session of GMF coverage Wave D (ADR-0029, R1–R7; D0 landed
> `7935b71..7776098`). D1 is the observation family — `MultiObservation`,
> `DiagnosticReport`, `VitalSign`-as-observation (R2(a)/(c)) — and its IR
> addition, the `:diagnostic-report` step, is the highest-blast-radius
> schema decision left in the wave: a `sim-model` change consumed by
> `sim-trajectory`, `sim`, and `sim-emit-hl7`. ADR-0029 left the two
> states' exact upstream coupling as a marked placeholder to be filled
> from evidence. THIS SESSION IS CHARACTERIZATION ONLY: it fetches the
> evidence, surveys sepsis's closure, drafts the schema as a PROPOSAL,
> and halts for a design-channel ruling. No implementation commit, no
> sim-model edit, no interpreter edit — the halt is the deliverable's
> shape, not a failure mode. Implementation is D1b, prompted after the
> ruling.
> Ceremony: R30-mode for the documentation checkpoints this session does
> make — commit and push each, unattended, with R30's safeguards
> (staged-scope `--stat` check, personal-info scan, message via the
> Write tool, session record before final push, hooks as backstop; tags
> and repo-level `gh` outside the grant). Work in the WSL ext4 clone;
> fast-forward to `origin/main` (at or past `7776098`), record HEAD.
> Read first
>
> 1. `notes/ADRs.md` ADR-0029 (R2's family design and its
>    characterization placeholders — this session fills D1's) and
>    ADR-0027 §D1 (the three-compartment person record — `VitalSign`'s
>    design leans on it).
> 2. `components/sim-trajectory/docs/gmf-interpreter.md` — the
>    `MultiObservation`/`DiagnosticReport` deferred rows, the sepsis
>    survey row, the seven-kind transition inventory (five built +
>    `lookup_table_transition` + attribute-weighted weights, both D3).
> 3. `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` — the
>    message-type registry and its `:result-available` (ORU^R01) and
>    unsolicited-`:observation` comments: the emission design D1
>    extends, and evidence for what OBR context the schema must carry.
> 4. `components/sim-model/src/ehrt/sim_model/pathway.clj` — the
>    `:observation` step shape (the child-entry precedent) and the C4
>    `:discharge` extension (the optional-fields precedent).
> 5. The Wave B/C session records for the pinned-commit Synthea fetch
>    method.
>
> Author rulings
>
> * E1 — Halt contract: Steps 0–2 only. The session's last act is a
>   findings summary addressed to the author; anything resembling
>   implementation is out of scope regardless of how clear the path
>   looks. (Wave C's stroke escalation is the precedent for why:
>   characterization has overturned design assumptions in this arc
>   before, and the schema must be ruled on evidence, not momentum.)
> * E2 — Step 0 riders (ruled 2026-08-02, design channel): (a)
>   `components/sim/docs/demos/site-profiles/` moves to
>   `components/sim-emit-hl7/docs/demos/site-profiles/` (subject-owned
>   by the emitter, D0's disclosed scope boundary); the broken citation
>   in root `docs/site-profiles.md` is repaired to the new location with
>   a dated note attributing the original breakage to the `c0b5b0a`
>   merge relocation (the path was valid at the pre-merge sim repo's own
>   root — `474aa5f` provenance) — fix-forward, no history rewritten;
>   sweep for any other citations of the old demos path. (b) The
>   release-target ruling is recorded: the author ruled Clojars (not
>   Maven Central) as ehr-testing's publication target — ruled prior to
>   2026-08-02, previously unrecorded; publication itself remains
>   deferred (not ready to publish). Land this as a dated note on the
>   roadmap's release row (and its ADR if one names the open question),
>   closing "Clojars vs. Maven Central" as an open gate while keeping
>   publication itself parked. (c) Roadmap: D1 → Now (characterization
>   phase).
> * E3 — Fetch set, at the same pinned Synthea commit as Waves B/C:
>   `sepsis.json` and its full transitive closure; Synthea's state
>   implementations for `MultiObservation`, `DiagnosticReport`,
>   `Observation` (its vital-sign and attribute-reference handling), and
>   `VitalSign`; any lookup tables or attribute sources sepsis's closure
>   references. Blocked-on-fetch stops after Step 0, recorded.
> * E4 — The characterization must answer, with source citations: (a)
>   the MultiObservation↔DiagnosticReport coupling — how children are
>   counted/referenced (`number_of_observations`, attribute references,
>   embedded codes), whether either state appears without the other in
>   sepsis and in the upstream corpus generally, and what each consumes
>   from the person record or rng (rng draws join the order contract if
>   any); (b) `VitalSign` semantics — what it sets, over what duration,
>   and whether sepsis's `Vital Sign` conditions read anything a
>   log-resident observation event cannot supply (a "yes" here is a
>   finding against R2(c)'s dissolution design and goes in the report,
>   not silently around it); (c) sepsis closure survey rows in the
>   established format, the transition-kind sweep against all seven
>   known kinds (presence of a D3 kind = a D3 dependency recorded,
>   shrinking or resequencing D1's payoff honestly), D7 hidden-import
>   check, encounter-bearing check; (d) the emission-side inventory:
>   which OBR/OBX fields the current ORU rendering populates for
>   `:result-available`, and therefore what the `:diagnostic-report`
>   step must carry for the solicited-shaped rendering to be a real,
>   legal ORU — read the emitter, list the fields.
> * E5 — The schema PROPOSAL: a `:diagnostic-report` malli shape drafted
>   from E4's evidence (children as observation-shaped entries per
>   R2(a); optional report codes; whatever E4(d) shows the OBR needs),
>   plus the compile-mapping sketch for all three GMF states, the engine
>   pass-through-or-handle question stated with a recommendation, and
>   open questions enumerated. This lands as a clearly-marked PROPOSED
>   section in the ADR-0029 D1 placeholder — labeled awaiting ruling, not
>   as decided text.
>
> Steps
>
> 0. Riders per E2 (demos move + citation repair + provenance note;
>    Clojars ruling recorded; roadmap). The demos move is a pure file
>    move with link sweep — the one permitted non-doc change this
>    session, oracle-free because no code path reads those files (verify
>    that claim by grep before moving; if anything reads them, that's a
>    finding and the move waits). Commit: `docs: D1a riders -- demos
>    relocation (c0b5b0a provenance), Clojars ruling recorded`.
> 1. Fetch per E3; characterize per E4. Land the findings in the
>    gmf-interpreter survey (rows, dated notes) and fill ADR-0029's D1
>    characterization placeholder with the evidence sections. Commit:
>    `docs(sim-trajectory): D1a characterization -- observation family,
>    sepsis closure`.
> 2. Draft the E5 proposal into the ADR's marked PROPOSED section.
>    Commit: `docs: D1a schema proposal (:diagnostic-report) -- PROPOSED,
>    awaiting ruling`.
> 3. Halt per E1. Session record + prompt self-archive to
>    `.agents/prompts/` (deviation record if any), final commit `docs:
>    D1a records (session record; archives prompt)`, then the closing
>    summary to the author: findings, the proposal, the open questions,
>    and any D3-dependency or R2(c)-counterevidence findings stated
>    plainly at the top.

## Deviation record

One disclosed scope-narrowing, not an escalation-and-stop case: E2(b)'s
own instruction ("Land this as a dated note on the roadmap's release row
(and its ADR if one names the open question)") was read literally —
only `notes/ADRs.md` ADR-0001's own H5 entry (the ADR that names the
open Clojars-vs-Maven-Central question) and `.agents/plans/roadmap.md`'s
own release row were amended. Two OTHER live citations of H5 exist in
`notes/ADRs.md` (a `tools/H5` pointer and a "once H5 ... resolves" line,
both still accurate as written — they describe H5 as a whole, which
still has an open coordinates-naming half after this session's own
narrower ruling) — read, and deliberately left untouched rather than
swept, since the prompt's own "(and its ADR if one names the open
question)" reads as singular and this session judged expanding it a
scope call beyond what was asked, not a correction it was authorized to
make unilaterally. Recorded here per this project's own disclosure
discipline, not silently decided either way.

No other deviation: every step ran as written, E1's halt held (no
sim-model/sim-trajectory/sim-emit-hl7 code changed), and every Step 0
rider (demos move, citation repair, Clojars note, roadmap D1→Now)
landed exactly as E2 named it.

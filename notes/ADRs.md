# Architecture Decision Records — ehr-testing (workspace)

Numbered, append-only, starting fresh at ADR-0001 for this workspace
— not a continuation of `ehr-testing-sim`'s or (later) `ehr-testing-tools`'
own numbering. Never silently revert an Accepted ADR; supersede it
with a new numbered record.

Legacy ADRs move into this workspace intact as provenance
(`notes/sim/ADRs.md`, `notes/tools/ADRs.md`, frozen, not rewritten for
new paths/namespaces) and are cited here origin-qualified, e.g.
`sim/ADR-0008`, `tools/ADR-0017`.

**Citation rule (added 2026-07-30, judge-v2-nist follow-through
session): a bare `ADR-00XX` in this file, or in any other workspace
document, means this file's own record.** Frozen-era ADRs are always
cited origin-qualified (`tools/ADR-0012`, `sim/ADR-0008`, etc.) — never
bare. This is the rule the two paragraphs above already modeled; it is
restated as an explicit standing rule here because ADR-0012 below now
shares its number with the frozen `notes/tools/ADRs.md` ADR-0012 (the
`ehr sim` mount design), a genuine collision this session's own
citation-space audit found, unambiguous when ADR-0005 wrote its own
unqualified `ADR-0012` references (2026-07-28, before this workspace's
own ADR-0012 existed) but ambiguous since. Renumbering either record
was considered and rejected: ADR numbers are load-bearing in immutable
places this workspace cannot edit (commit messages, archived prompts,
docstrings) and this register's own append-only, never-reassigned
numbering rule exists for exactly this reason. Existing unqualified
frozen-era references are fixed forward, dated, as they're found — not
rewritten wholesale, and never by editing the frozen files themselves.

---

## Index

**This file became an index on 2026-08-05** (scaffolding compaction B,
`notes/ADRs.md` ADR-0046). Every entry that used to live inline here
now lives verbatim in its own file under `notes/adr/`, moved
byte-for-byte (proof: the session record's own extraction diff) — this
file keeps its role as the citation target (`notes/ADRs.md ADR-NNNN`
resolves here, then follows the link below; the bare-`ADR-NNNN`
citation-qualification rule above is unchanged) and stays the sole
home for the preamble rules above. Order below matches this file's own
pre-split entry order — unchanged, not renumbered and not
re-sequenced. New execution-record appends to an existing ADR go
directly to its own `notes/adr/` file from this date forward; a line
below updates only when an arc closes.

- **ADR-0001** — Migration plan: bootstrap the workspace, land sim, freeze tools out — [`0001-migration-plan.md`](adr/0001-migration-plan.md) — Accepted
- **ADR-0002** — Land `ehr-testing-tools`: components/tools + components/palgebra + bases/ehr-cli, close H1/H2/H3 — [`0002-land-ehr-testing-tools.md`](adr/0002-land-ehr-testing-tools.md) — Accepted
- **ADR-0003** — Pre-push gate doctrine: irreversibility-only — [`0003-pre-push-gate-doctrine.md`](adr/0003-pre-push-gate-doctrine.md) — Accepted
- **ADR-0004** — Carve-loss audit; CI two-lane rule restored; local state is not clone state — [`0004-carve-loss-audit.md`](adr/0004-carve-loss-audit.md) — Accepted
- **ADR-0005** — The `ehr sim` mount: `notes/tools/ADRs.md` ADR-0012 fulfilled, `notes/tools/ADRs.md` ADR-0013 decision 1 retired — [`0005-the-ehr-sim-mount.md`](adr/0005-the-ehr-sim-mount.md) — Accepted
- **ADR-0006** — Discipline parity restored: guides, live registers, sweep completion — [`0006-discipline-parity-restored.md`](adr/0006-discipline-parity-restored.md) — Accepted
- **ADR-0007** — Commit/push restored to session ritual; ruling provenance tags adopted — [`0007-commit-push-restored-to-session-ritual.md`](adr/0007-commit-push-restored-to-session-ritual.md) — Accepted
- **ADR-0008** — Kernel and judge extraction: ADR-0002 R14 (named hole H4) closed — [`0008-kernel-and-judge-extraction.md`](adr/0008-kernel-and-judge-extraction.md) — Accepted
- **ADR-0009** — CLI renamed `ehrt` ("e-heart"); base `cli`, project `ehrt-cli`; `sim-cli` deprecated — [`0009-cli-renamed-ehrt.md`](adr/0009-cli-renamed-ehrt.md) — Accepted
- **ADR-0010** — Documentation doctrine: audience-forked, user path complete at root — [`0010-documentation-doctrine.md`](adr/0010-documentation-doctrine.md) — Accepted
- **ADR-0011** — Per-engine judge split: `judge-v2-hapi` and `judge-fhir-official`; `judge` keeps the verdict vocabulary — [`0011-per-engine-judge-split.md`](adr/0011-per-engine-judge-split.md) — Accepted
- **ADR-0012** — `judge-v2-nist` adopts the NIST engine directly: msg-id contract, Cause growth, fixture provenance — [`0012-judge-v2-nist-adopts-the-nist-engine-directly.md`](adr/0012-judge-v2-nist-adopts-the-nist-engine-directly.md) — Accepted
- **ADR-0013** — Output UX doctrine: single `out/` root, artifact-vs-display boundary (the TTY rule), the `show` verb, jet/`--json` surfacing — [`0013-output-ux-doctrine.md`](adr/0013-output-ux-doctrine.md) — Accepted
- **ADR-0022** — Sim adopts `ehrt.kernel.result`; its own copied envelope (`sim/ADR-0001` point 4) is retired, promise honored — [`0022-sim-adopts-ehrt-kernel-result.md`](adr/0022-sim-adopts-ehrt-kernel-result.md) — Accepted
- **ADR-0021** — `bases/sim-cli`/`projects/sim` retired (F2 fired): `ehrt sim` gains `check`/`identifiers`/`version`, closing the parity gap the retirement review found first — [`0021-bases-sim-cli-projects-sim-retired.md`](adr/0021-bases-sim-cli-projects-sim-retired.md) — Accepted
- **ADR-0018** — `tools` split stage 3: the domain renamed `corpus`, the façade retired, the interface designed from live consumers — [`0018-tools-split-stage-3.md`](adr/0018-tools-split-stage-3.md) — Accepted
- **ADR-0017** — `tools` split stage 2: `corpus-io` extracted, generator-source's own domain edge kept the seam split correctly, `:necessary` re-derived again — [`0017-tools-split-stage-2.md`](adr/0017-tools-split-stage-2.md) — Accepted
- **ADR-0016** — `tools` split stage 1: `docs-tooling` extracted, `:necessary` re-derived — [`0016-tools-split-stage-1.md`](adr/0016-tools-split-stage-1.md) — Accepted
- **ADR-0014** — Corpus player: pacer semantics, plan/execute time seam, cue rule extends artifact-vs-display; bed board and accumulator wiring deferred — [`0014-corpus-player.md`](adr/0014-corpus-player.md) — Accepted
- **ADR-0015** — CLI trial-UX: generate sources front door, play directories, gate v2-nist verb, breadcrumbs pretty-only — [`0015-cli-trial-ux.md`](adr/0015-cli-trial-ux.md) — Accepted
- **ADR-0023** — Agent-UX charter adopted: capture executed, R-F enacted, sequencing amended — [`0023-agent-ux-charter-adopted.md`](adr/0023-agent-ux-charter-adopted.md) — Accepted
- **ADR-0024** — `.claude/skills/` carved out of the untracked-`.claude/` ruling; mirror-with-gate lands the Claude Code discovery fix — [`0024-claude-skills-carved-out-of-the-untracked-claude-ruling.md`](adr/0024-claude-skills-carved-out-of-the-untracked-claude-ruling.md) — Accepted
- **ADR-0025** — sim split S1+S2: `sim-model` and `sim-trajectory` extracted from `sim` — [`0025-sim-split-s1-s2.md`](adr/0025-sim-split-s1-s2.md) — Accepted
- **ADR-0026** — GMF coverage Wave A: condition vocabulary v1→v1.1, `sore_throat.json` vendored — [`0026-gmf-coverage-wave-a.md`](adr/0026-gmf-coverage-wave-a.md) — Accepted
- **ADR-0027** — GMF coverage Wave B: `CallSubmodule` — three-compartment person record, root-scoped scratch, closure loading — [`0027-gmf-coverage-wave-b.md`](adr/0027-gmf-coverage-wave-b.md) — Accepted
- **ADR-0028** — GMF coverage Wave C: `Death` — terminal contract, `:expired` status lands in code — [`0028-gmf-coverage-wave-c.md`](adr/0028-gmf-coverage-wave-c.md) — Accepted
- **ADR-0029** — GMF coverage Wave D: design (R1–R7) — IR additions, CarePlan v2-silence, closure data files, D0–D3 sequencing — [`0029-gmf-coverage-wave-d.md`](adr/0029-gmf-coverage-wave-d.md) — Accepted
- **ADR-0030** — Post-Wave-D cleanup: oracle byte-verification, closure engine round-trips, dual-clone guardrails — [`0030-post-wave-d-cleanup.md`](adr/0030-post-wave-d-cleanup.md) — Accepted
- **ADR-0031** — Parity-plan rulings (Q1–Q4), wellness-semantics overturn, defect-fix sequencing — [`0031-parity-plan-rulings-wellness-semantics-overturn-defect-fix-sequencing.md`](adr/0031-parity-plan-rulings-wellness-semantics-overturn-defect-fix-sequencing.md) — Accepted
- **ADR-0032** — Procedure-duration fix: rulings and semantics pin (D3c finding 1, ADR-0031 AR-6 first defect-fix) — [`0032-procedure-duration-fix.md`](adr/0032-procedure-duration-fix.md) — Accepted
- **ADR-0033** — Engine closure-context fix: `:registered` threads a closure's own modules/tables/initial-attributes to `run-module` (ADR-0031 AR-6 second defect-fix, J3 closed) — [`0033-engine-closure-context-fix.md`](adr/0033-engine-closure-context-fix.md) — Accepted
- **ADR-0034** — GMF census tool: load/walk verdicts, substitution tags, first pinned artifact (`.agents/plans/2026-08-02-gmf-parity-plan.md` §3, ADR-0031 AR-1/AR-4) — [`0034-gmf-census-tool.md`](adr/0034-gmf-census-tool.md) — Accepted
- **ADR-0035** — Wave F0: GAUSSIAN/EXPONENTIAL/TRIANGULAR distributions land; SetAttribute's silent-nil gap closes (`.agents/plans/2026-08-02-gmf-parity-plan.md` §4 resequencing, ADR-0034's own `gmf_version 2` loader-exception finding) — [`0035-wave-f0.md`](adr/0035-wave-f0.md) — Accepted
- **ADR-0036** — Wave F: Counter/ImagingStudy/SupplyList land; the condition rider (`Not`/`Race`/`Socioeconomic Status`) and persona race/SES fields close (`.agents/plans/2026-08-02-gmf-parity-plan.md` §4, ADR-0035 AR-8's own resequencing) — [`0036-wave-f.md`](adr/0036-wave-f.md) — Accepted
- **ADR-0037** — GMF coverage Wave G: the wellness cycle lands — genuine wait semantics, the create-now substitution retired, four loop modules resolve — [`0037-gmf-coverage-wave-g.md`](adr/0037-gmf-coverage-wave-g.md) — Accepted
- **ADR-0038** — Wave LC: lookup-table columns generalize to attribute resolution — H2's own whitelist retires, 9 modules close — [`0038-wave-lc.md`](adr/0038-wave-lc.md) — Accepted
- **ADR-0039** — Wave VS: the vital-sign channel — register, `VitalSign` state, `:vital-sign` condition land; two of four blocked modules clear — [`0039-wave-vs.md`](adr/0039-wave-vs.md) — Accepted
- **ADR-0040** — GMF coverage Wave I: the singleton tail — six small mechanisms land; 7 of 9 blocked modules resolve, 2 unmask new gaps — [`0040-gmf-coverage-wave-i.md`](adr/0040-gmf-coverage-wave-i.md) — Accepted
- **ADR-0041** — GMF coverage Wave I2: the last two — parity frontier CLOSES — [`0041-gmf-coverage-wave-i2.md`](adr/0041-gmf-coverage-wave-i2.md) — Accepted
- **ADR-0042** — Wave H pre-roll: the history phase lands — opt-in, phase-marked, straddle-safe. GMF parity arc COMPLETE — [`0042-wave-h-pre-roll.md`](adr/0042-wave-h-pre-roll.md) — Accepted
- **ADR-0043** — Sim split B, M1: `provenance` component lands, sim's manifest mirror retires, the intake front door is written down — [`0043-sim-split-b-m1.md`](adr/0043-sim-split-b-m1.md) — Accepted
- **ADR-0044** — Standing-equipment promotion: census enters `sim-trajectory`, the oracle digest becomes a component, J2 closes structurally — [`0044-standing-equipment-promotion.md`](adr/0044-standing-equipment-promotion.md) — Accepted
- **ADR-0045** — Scaffolding compaction A: riders, vestige retirements, Deferred triage — [`0045-scaffolding-compaction-a.md`](adr/0045-scaffolding-compaction-a.md) — Accepted
- **ADR-0046** — Scaffolding compaction B: the ADR split and the roadmap rotation — [`0046-scaffolding-compaction-b.md`](adr/0046-scaffolding-compaction-b.md) — Accepted
- **ADR-0047** — Scaffolding compaction C: the continuity register lands, `/mnt/c` retires, arc closes — [`0047-scaffolding-compaction-c.md`](adr/0047-scaffolding-compaction-c.md) — Accepted
- **ADR-0048** — Alignment riders: small debts paid, the audit brief lands, stable tags go live — [`0048-alignment-riders.md`](adr/0048-alignment-riders.md) — Accepted
- **ADR-0049** — Alignment audit: the tree examined, findings registered, nothing moved — [`0049-alignment-audit.md`](adr/0049-alignment-audit.md) — Accepted
- **ADR-0050** — Alignment fixes 1: the past stops leaking — staleness swept, tripwire hardened, conventions named — [`0050-alignment-fixes-1.md`](adr/0050-alignment-fixes-1.md) — Accepted
- **ADR-0051** — Alignment fixes 2: the law reads the same everywhere, and three laws get teeth — [`0051-alignment-fixes-2.md`](adr/0051-alignment-fixes-2.md) — Accepted
- **ADR-0052** — Alignment fixes 3: sim-model's resources take their own name, and the nesting rule gets its gate — [`0052-alignment-fixes-3.md`](adr/0052-alignment-fixes-3.md) — Accepted
- **ADR-0053** — Alignment fixes 4: offline determinism without redistribution — the NIST mirror lives user-side, the lockfile grows teeth — [`0053-alignment-fixes-4.md`](adr/0053-alignment-fixes-4.md) — Accepted
- **ADR-0054** — Alignment fixes 5: the license text travels with the content — F-4 closes, gated — [`0054-alignment-fixes-5.md`](adr/0054-alignment-fixes-5.md) — Accepted
- **ADR-0055** — Alignment arc close: the register empties, the state regenerates, the law is appended — [`0055-alignment-arc-close.md`](adr/0055-alignment-arc-close.md) — Accepted
- **ADR-0056** — UX riders: the arc opens — brief lands, tags licensed, the compaction pointers come home — [`0056-ux-riders.md`](adr/0056-ux-riders.md) — Accepted
- **ADR-0057** — Tag law: the boundary moves to verification, where it always was — [`0057-tag-law.md`](adr/0057-tag-law.md) — Accepted
- **ADR-0058** — UX audit: every stranger-facing surface surveyed, nothing moved — [`0058-ux-audit.md`](adr/0058-ux-audit.md) — Accepted
- **ADR-0059** — UX fixes 1: every doc teaches the real invocation — swept, paired, gated — [`0059-ux-fixes-1.md`](adr/0059-ux-fixes-1.md) — Accepted
- **ADR-0060** — UX fixes 2: errors that name their artifact — the config crash dies, and the fences actually run — [`0060-ux-fixes-2.md`](adr/0060-ux-fixes-2.md) — Accepted
- **ADR-0061** — UX fixes 3: the typo that succeeded — unknown flags rejected, near-misses named — [`0061-ux-fixes-3.md`](adr/0061-ux-fixes-3.md) — Accepted
- **ADR-0062** — UX fixes 4: the help speaks to operators — the approved rewrite lands, gated — [`0062-ux-fixes-4.md`](adr/0062-ux-fixes-4.md) — Accepted
- **ADR-0063** — UX fixes 5: the help wraps like it means it — hanging indents, width gated — [`0063-ux-fixes-5.md`](adr/0063-ux-fixes-5.md) — Accepted
- **ADR-0064** — UX arc close: the founding incident is mechanically impossible — appended, regenerated, rotated — [`0064-ux-arc-close.md`](adr/0064-ux-arc-close.md) — Accepted
- **ADR-0065** — UX epilogue: muscle memory gets an answer, help gets a width — the retired `:cli` alias redirects, a sibling near-miss gets a real hint, `--width`/COLUMNS lands — [`0065-ux-epilogue.md`](adr/0065-ux-epilogue.md) — Accepted
- **ADR-0066** — Player fold: the accumulator learns two-participant messages and absolute time — A17/A40 fold into the wire-side accumulator, MSH-7 reads an absolute epoch instant, the emitter-coherence property runs over the full churn family — [`0066-player-fold.md`](adr/0066-player-fold.md) — Accepted
- **ADR-0067** — Player board: the whiteboard exists — the accumulator meets the pacer — `ehrt play --board N` renders a bed-state snapshot every N stream-minutes, foreign traffic skips as a counted cue not a crash — [`0067-player-board.md`](adr/0067-player-board.md) — Accepted
- **ADR-0068** — Player arc close: the hospital is watchable, the suite is honest, the state regenerates — six founding-incident mechanisms now guard the CLI, fresh-clone-green for the first time since ADR-0060, the state and its budgets re-derive — [`0068-player-arc-close.md`](adr/0068-player-arc-close.md) — Accepted
- **ADR-0069** — Census substance: the vendoring arc opens with an honest catalog — the `:substance`/`:event-counts`/`:ok-walked-by-substance` qualifier and the labeled-filename fix land, the fresh census ranks 84 `:ok-walked` modules 51/33 zero-content/content-producing, parity held — [`0069-census-substance.md`](adr/0069-census-substance.md) — Accepted
- **ADR-0070** — Vendoring batch 1: the everyday ambulatory load — five ailments join the mix (asthma, bronchitis, sleep-apnea, fibromyalgia, dementia), `injuries.json` assessed and deferred whole on a real `gmf-interpreter` gap the census's own narrow sample missed — [`0070-vendoring-batch-1.md`](adr/0070-vendoring-batch-1.md) — Accepted
- **ADR-0071** — Vendoring batch 2: the chronic clinic tail — seven ailments join the mix (hypothyroidism, rheumatoid-arthritis, osteoarthritis, osteoporosis, attention-deficit-disorder, allergic-rhinitis, dermatitis), `anemia___unknown_etiology.json` assessed and deferred whole on a real dangling-`:encounter-end` `gmf-interpreter` gap; a `scenarios/` home lands with the first runnable, population-scale demo, `busy-tuesday` — [`0071-vendoring-batch-2.md`](adr/0071-vendoring-batch-2.md) — Accepted
- **ADR-0072** — Vendoring batch 3: the families that travel, and the verbatim law gets teeth — `uti_recurrence.csv`'s silent LF-normalization closed with a `-text` rule and a standing NOTICE-verbatim gate; four content-producers land (metabolic-syndrome-care, vhd-pulmonic, vhd-tricuspid, med-rec), `colorectal_cancer.json` deferred whole on the same `:encounter-end` gap batch 2 found — [`0072-vendoring-batch-3.md`](adr/0072-vendoring-batch-3.md) — Accepted
- **ADR-0073** — Demos front door: the operator surface moves to the front, and the README shows it running — `demos/scenarios/`+`demos/traces/` land at the repo root, byte-witnessed, pointer READMEs left behind; README.md gains a "See it run" section, live-probed to a rendered bed board in two commands — [`0073-demos-front-door.md`](adr/0073-demos-front-door.md) — Accepted
- **ADR-0074** — Vendoring arc close: the mix more than tripled, the bytes cannot lie, the front door is open — closed across two Code sessions after an infrastructure block, no work lost; state and budgets regenerate, six Done pointers rotate to the attic, `libs :outdated` re-run fresh, the EncounterEnd two-module blocker named as next-arc's strongest candidate — [`0074-vendoring-arc-close.md`](adr/0074-vendoring-arc-close.md) — Accepted
- **ADR-0075** — CI current: the derived docs catch up, the staleness guard comes home, preflight learns to look — `docs/cli.md` had been stale since ADR-0065 (32 commits, not the estimated 25), CI red the whole time with nobody watching; docs/cli.md and docs/operators.md gain local, in-process staleness gates alongside CI's own; a second, unrelated intermittent test failure found and named for next-arc intake, not fixed here — [`0075-ci-current.md`](adr/0075-ci-current.md) — Accepted

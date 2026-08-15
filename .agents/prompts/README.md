# Prompts

The new home for session prompts (charter R-A, `notes/ADRs.md`
ADR-0023, 2026-08-01) — a session archives the prompt that drove it
here, paired by date-slug with its own
[`.agents/session-records/`](../session-records/README.md) entry. Not
a rewrite of the archival format: entries here follow the same shape
`notes/prompts/*.md` already used (a short repo/clone/HEAD context
paragraph, the original prompt verbatim, a deviation record) — only
the location changes.

**Why a new location, not more of `notes/prompts/`:** the agent-UX
charter's own diagnosis (§2) named signpost burial and a two-register
split as failure modes; consolidating session-driving archives under
`.agents/` alongside `.agents/session-records/`, `.agents/plans/`, and
`.agents/memory/` — the surfaces an agent picking up a session cold
actually needs to route through — closes one instance of that split.
`notes/prompts/` stays the historical archive for everything through
2026-08-01; see its own README for the forward pointer.

**When:** every non-trivial session, as its last pre-push act, archives
its own driving prompt here (paired with its session record) — the
same ritual `.agents/session-records/README.md` describes, R-A's other
half.

## Prompt list

Files in this directory:

  * 2026-08-01-agent-ux-capture.md
  * 2026-08-01-skill-adaptation.md
  * 2026-08-01-migration-session-1.md
  * 2026-08-02-migration-session-2.md
  * 2026-08-02-migration-session-3.md
  * 2026-08-02-migration-session-4.md
  * 2026-08-02-migration-session-5.md
  * 2026-08-02-migration-session-6.md
  * 2026-08-02-provenance-adoption-rider.md
  * 2026-08-02-sim-split-s1-s2.md
  * 2026-08-02-gmf-coverage-wave-a.md
  * 2026-08-02-gmf-coverage-wave-b.md
  * 2026-08-02-gmf-coverage-wave-c.md
  * 2026-08-02-sim-split-s3-wave-d-d0.md
  * 2026-08-02-gmf-coverage-wave-d-d1a.md
  * 2026-08-02-gmf-coverage-wave-d-stage-d1b.md
  * 2026-08-02-gmf-coverage-wave-d-stage-d2.md
  * 2026-08-02-gmf-coverage-wave-d-stage-d3.md
  * 2026-08-02-post-wave-d-cleanup.md
  * 2026-08-03-rulings-capture.md
  * 2026-08-03-procedure-duration-fix.md
  * 2026-08-03-engine-closure-context.md
  * 2026-08-03-gmf-census.md
  * 2026-08-03-wave-f0-distributions.md
  * 2026-08-03-gmf-coverage-wave-f.md
  * 2026-08-03-gmf-coverage-wave-g.md
  * 2026-08-03-gmf-coverage-wave-lc.md
  * 2026-08-04-gmf-coverage-wave-vs.md
  * 2026-08-04-gmf-coverage-wave-i.md
  * 2026-08-04-gmf-coverage-wave-i2.md
  * 2026-08-04-gmf-coverage-wave-h.md
  * 2026-08-04-sim-split-m1-provenance.md
  * 2026-08-04-sim-split-m2-engine.md
  * 2026-08-04-sim-split-m3-emit-fhir.md
  * 2026-08-04-sim-split-m4-check.md
  * 2026-08-05-docs-coherence-pass.md
  * 2026-08-05-standing-equipment-promotion.md
  * 2026-08-05-scaffolding-compaction-a.md
  * 2026-08-05-scaffolding-compaction-b.md
  * 2026-08-05-scaffolding-compaction-c.md
  * 2026-08-05-alignment-riders.md
  * 2026-08-05-alignment-audit.md
  * 2026-08-05-alignment-fixes-1.md
  * 2026-08-05-alignment-fixes-2.md
  * 2026-08-05-alignment-fixes-3.md
  * 2026-08-05-alignment-fixes-4.md
  * 2026-08-05-alignment-fixes-5.md
  * 2026-08-05-alignment-arc-close.md
  * 2026-08-06-ux-riders.md
  * 2026-08-06-tag-law.md
  * 2026-08-06-ux-audit.md
  * 2026-08-06-ux-fixes-1.md
  * 2026-08-06-ux-fixes-2.md
  * 2026-08-06-ux-fixes-3.md
  * 2026-08-06-ux-fixes-4.md
  * 2026-08-06-ux-fixes-5.md
  * 2026-08-06-ux-arc-close.md
  * 2026-08-06-ux-epilogue.md
  * 2026-08-06-player-fold.md
  * 2026-08-07-player-board.md
  * 2026-08-07-player-arc-close.md
  * 2026-08-07-census-substance.md
  * 2026-08-07-vendoring-batch-1.md
  * 2026-08-07-vendoring-batch-2.md
  * 2026-08-07-vendoring-batch-3.md
  * 2026-08-07-demos-front-door.md
  * 2026-08-07-vendoring-arc-close.md
  * 2026-08-07-ci-current.md
  * 2026-08-07-quality-riders.md
  * 2026-08-07-repo-review-1.md
  * 2026-08-07-result-or-loud.md
  * 2026-08-07-lint-family.md
  * 2026-08-07-quality-arc-close.md
  * 2026-08-08-fidelity-riders.md
  * 2026-08-08-encounterend-fix.md
  * 2026-08-08-fidelity-payoff.md
  * 2026-08-08-fidelity-arc-close.md
  * 2026-08-08-colorectal-investigation.md
  * 2026-08-08-straddle-fix.md
  * 2026-08-08-colorectal-payoff.md
  * 2026-08-08-pairing-registry.md
  * 2026-08-08-conviction-close-a.md
  * 2026-08-08-conviction-close-b.md
  * 2026-08-08-vendoring-batch-4.md
  * 2026-08-09-storefront-fixture.md
  * 2026-08-09-repo-review-2.md
  * 2026-08-09-review-2-rulings-landing.md
  * 2026-08-09-census-closure-file-count.md
  * 2026-08-09-cluster-a-gate-wiring.md
  * 2026-08-09-cluster-b-parse-guards.md
  * 2026-08-09-review-2-arc-close.md
  * 2026-08-09-permission-legs-and-bare-flags.md
  * 2026-08-10-fixture-relocation.md
  * 2026-08-10-sim-event-log-adapter.md
  * 2026-08-10-adr-footnotes.md
  * 2026-08-10-marker-only-footnotes.md
  * 2026-08-11-board-boundary-fix.md
  * 2026-08-11-ed-tuesday-scenario.md
  * 2026-08-11-interpreter-horizon-budget.md
  * 2026-08-11-injuries-b2-assessment.md
  * 2026-08-11-injuries-arc-close.md
  * 2026-08-11-simulator-architecture-doc.md
  * 2026-08-11-latency-second-clock.md
  * 2026-08-11-latency-demo.md
  * 2026-08-11-corpus-batching.md
  * 2026-08-11-batch-straddle-recording.md
  * 2026-08-12-sim-palgebra-unification.md
  * 2026-08-12-review-3-user-surface.md
  * 2026-08-12-review-3-rulings-landing.md
  * 2026-08-12-engine-seed-contract.md
  * 2026-08-12-fix-cluster-a-cli-validation.md
  * 2026-08-12-fix-clusters-b-and-c-help-and-docs.md
  * 2026-08-12-user-manual-skeleton.md
  * 2026-08-12-manual-s2-exerciser-and-chapter3.md
  * 2026-08-12-manual-s3-transport-pair.md
  * 2026-08-13-positive-seed-diagnosis.md
  * 2026-08-13-medication-end-invariant-fix.md
  * 2026-08-13-manual-s4-mutate-and-gate.md
  * 2026-08-13-manual-s5-chapter8-review-close.md
  * 2026-08-13-citation-sweep-glossary-linkage.md
  * 2026-08-13-ceremony-scripts-sim-identity-sweep.md — Ceremony scripts, build-session skill absorption, sim-identity citation sweep (ADR-0127)
  * 2026-08-13-ceremony-scripts-hardening.md — Agent-facing hardening -- ADR-0127 addendum, anti-fabrication tripwire, Step-0 receipts
  * 2026-08-13-strip-executability.md — Strip executability: exercisers, citation gate, ADR-0127 erratum (ADR-0129)
  * 2026-08-14-busy-tuesday-exerciser-deferred.md — Busy-tuesday exerciser: marker widening landed, row deferred on slug EDN round-trip defect (ADR-0130)
  * 2026-08-14-slug-edn-round-trip.md — Slug EDN round-trip fix + module-load injectivity guard (ADR-0131)
  * 2026-08-14-clinic-decade-rename-and-exerciser.md — Scenario rename busy-tuesday -> clinic-decade + exerciser completion (ADR-0132)
  * 2026-08-14-exact-name-resolution.md — Exact-name state resolution: collision fix, restoration cascade (ADR-0133)
  * 2026-08-14-manual-review-2.md — Manual-review run 2: channel-authored report landed verbatim, F1/F2/F3 fixed, ADR-0133's tag paid (ADR-0134)
  * 2026-08-15-string-diagram-terminal-outputs.md — String-diagram terminal-output result nodes (ADR-0135)
  * 2026-08-15-repo-review-3.md — Repo review 3: rubric amended at Step 0 (population-closure law) and executed immediately; 40-row register, plan, three probes recorded blocked/partial

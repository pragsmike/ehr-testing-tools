# Manual review — run 2 (2026-08-14)

Run by the design channel (Claude Fable) against a fresh public clone
at `46b82ba` (ADR-0133 close), NOT by an executing session invoking
the `manual-review` skill — a disclosed deviation from the skill's
usual runner, chartered by the author verbatim: "Do a thorough review
of this repo's user manual, here in the design channel using this
strong model (Fable)." Scope limit, disclosed: the channel sandbox
cannot resolve Clojure dependencies, so NOTHING was re-executed —
every witnessed output was checked for source-consistency, internal
arithmetic, and mechanism coverage, never re-witnessed. Execution-tier
confidence rests on the two-gate exerciser mechanism (per-push parity
tests + `make integration`) and the green full suite at `46b82ba`.
Whole manual read (`00-front.md` through `08-your-own-data.md`) before
any dimension was scored.

## Evidence table

| # | Dimension | Grade | Evidence | Rationale |
|---|---|---|---|---|
| 1 | Strip executability | **PASS** (run 1: FAIL) | `components/docs-tooling/resources/docs-tooling/exercised-sources.edn` carries 8 rows covering every source class the manual cites: README Quickstart (`:quickstart-fresh`), `demos/scenarios/ed-tuesday/README.md` and `.../clinic-decade/README.md` (`:demo-exerciser-fresh`), all four `docs/use-cases/*.md` pages the manual cites (`:single-fence` -> `bin/usecase-judge-tier-calibration`, `bin/usecase-profile-tier-v2`, `bin/usecase-acceptance-qa`, `bin/usecase-regression-baselining`), and README's own "What you get" section (`:paired` -> `bin/readme-what-you-get`). All seven scripts exist in `bin/`. `citation_gate_test.clj:2-7` forces every `docs/manual/0*.md` "Strip source citations" entry to resolve to a register row. Two-gate shape: per-push byte-parity (`demo_exerciser_fresh_test.clj:1-11`) plus `Makefile:43-51` (`make integration`) executing all seven scripts. | Run 1's structural gap (use-case pages and the "What you get" fence had no re-run mechanism) is closed structurally by ADR-0129, not by hand-witnessing — the remediation holds at the current tip. |
| 2 | No reference duplication | PASS | Every `^\|` pipe-table hit in `docs/manual/0*.md` is a "Strip source citations" table (04, 05, 06, 07, 08); zero restated flag/operator tables. | Unchanged from run 1. |
| 3 | Anchor stability | PASS | All 17 `](path#anchor)` links across chapters 01-08 resolve under the GFM slug rule, computed mechanically against each target file's real headings — including `what-is-this.md#scope--what-this-deliberately-does-not-do` (doubled hyphen) and `formats.md#baseline-mode-changes-the-payloads-shape` (dropped apostrophe). | Zero broken anchors. |
| 4 | Glossary linkage | **PASS** (run 1: FAIL) | Chapters 01, 03, 04, 06, 07, 08 now carry first-use glossary links (e.g. 03 links pathways, census, churn, site profiles, script space, truth space, ground truth, emitters); every linked term resolves to a real glossary entry, including the combined "Script space / truth space" entry at `docs/glossary.md:452` — the exact colliding-meaning pair run 1 flagged as the worst gap. One cosmetic hair, filed as F3 below: Chapter 8 links the phrase "intake record" but the glossary headword is "Intake." (`docs/glossary.md:317`). | Run 1's fail remediated; the chapter run 1 called most dangerous (03) is now among the best-linked. |
| 5 | Running-example continuity | WARN | `ed-tuesday` carries chapters 01/03/04/05; absent from 06/07/08 for the structural reason run 1 documented (HL7v2-only scenario cannot supply FHIR mutation, FHIR-gate calibration, or foreign-corpus intake content). | Unchanged; disclosed, structural, not silent substitution. |
| 6 | Maturity honesty | PASS | `01:105-116` (Honest scope) routes to `README.md#maturity` and `what-is-this.md` scope; in-place functional-limit disclosures verified at each narrative arrival point: interior-empty-batch v1 deferral (05), "the catalog doesn't promise conviction" (06), the entire `:no-verdict` treatment (07), `--received` non-determinism honesty (08). | No overclaims found. |
| 7 | Currency vs generated `cli.md` | PASS — test-guaranteed | Currency is enforced structurally: `bases/cli/test/ehrt/cli/help_test.clj`'s `cli-md-is-current-test` plus CI's generated-doc regen+diff step (`.github/workflows/test.yml:87`), so a green tip IS the currency proof. Additionally checked directly, claim by claim: `--interval` required/no-default with the exact "no universally sensible schedule" quote (`cli.md:150`), epoch/UTC-midnight alignment (150), `--rate`/`--board` semantics (291-294), `gate v2-nist` explicit `--profile` (24, 155), exit code 3 (54), `--treat-no-verdict-as` (168), `--pair-by path\|hash` (209), and BOTH mutate argument forms (positional AND `--path` are real, which is why Chapter 6 and Chapter 7 legitimately differ — each copies its own source verbatim: `README.md` What-you-get uses positional, `judge-tier-calibration-studies.md:27` uses `--path`). The one post-manual CLI-source change (`help.clj` `:example` rename in `214b0ec`) does not render into `cli.md` (examples are not emitted), so no staleness. | Materially stronger than run 1's disclosed 4-claim sample. |
| 8 | Diagram-source presence | PASS | 5/5 SVGs under `docs/manual/assets/` carry derivation comments in their opening lines (gt-emitters -> simulator-architecture section 4; inject-expect-loop -> pipeline.edn stage equations; straddle-timeline -> ed-tuesday README Batched delivery; two-clocks -> simulator-architecture section 5; verdict-ranking -> tools/ADR-0010 ranking). | Unchanged. |

## Findings beyond the rubric

**F1 — erratum (fix-worthy).** `docs/manual/08-your-own-data.md`,
the `ehrt check` output block's elision comment: one patient file is
shown, then ";; ... five more patient files and both info files, all
:pass ...". That totals 1+5+2 = 8, contradicting the block's own
`:totals {:pass 7 ...}` — and 7 is correct: a 5-patient Synthea run
yields 5 patient bundles + `hospitalInformation` +
`practitionerInformation` (shape confirmed against
`components/corpus/docs/experiments/EXP-A4-results.md` and
`README.md:211`). The comment should read "four more patient files."
The README Quickstart fence contains the command but no output block,
so the elision was composed for the landing session — the manual's
own arithmetic slip, unearned-specificity class, inside a witnessed
block's hand-composed elision.

**F2 — warn (pedagogy).** Both Chapter 8 strips run against
`test-fixtures/v2`, which yields two different counts with no
explanation on the page or on either cited use-case page: intake
reports `:file-count 8` (recursive over every regular file,
`components/corpus/src/ehrt/corpus/intake.clj:174-178` — the five
`.hl7` plus `simhospital/{LICENSE, PROVENANCE.md, messages.out}`),
while `gate v2` reports `:pass 5` (the five `.hl7` only). Both values
are correct; the undisclosed divergence invites a false "did the gate
skip files?" alarm.

**F3 — cosmetic.** Chapter 8 links the phrase "intake record" to the
glossary, whose entry headword is "Intake." (`docs/glossary.md:317`).
All manual glossary links are anchor-less by convention, so this is a
headword-wording nit only.

**F4 — affirmative record.** Every cited test exists and matches:
`play-command-at-huge-rate-matches-show-identity-test` and
`play-command-file-sink-writes-byte-identical-to-unpaced-content-test`
(`bases/cli/test/ehrt/cli/core_test.clj:3056, 3083`);
`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`
(`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj:29`);
the naturality defspec at exactly 150 trials as Chapter 3 claims
(`components/sim-emit-fhir/test/ehrt/sim_emit_fhir/emit_fhir_test.clj:147`);
`write-and-verify-batch!` (`bases/cli/src/ehrt/cli/core.clj:930`).
Every numeric witnessed value traced byte-for-byte to
`demos/scenarios/ed-tuesday/README.md`: the Chapter 1 board snapshot,
Walker's 20m54s/1h00m46s delays, 383/283/34, census 4->21->3,
discharges 1->84, "one of 8 (of 92)". All six quoted epoch-ms batch
boundaries compute to exactly the UTC windows the prose claims.
Section attributions both correct: identity-element law in section 4
(`docs/dev/simulator-architecture.md:320-321`), two-timestamp-field
audit in section 5 (:385-388). `ed-tuesday` config matches Chapter 3
(five pathways; eight `:module-assignment` entries across the four
named modules, `config.edn:119-127`). `:profile-spec-error` is a real
`Cause` enum member (`components/judge/src/ehrt/judge/finding.clj:56`).
`:relative`-always-binary matches `docs/formats.md:184`. Freshness
against ADR-0132/0133: zero `busy-tuesday` strays anywhere in
docs/demos/README; ADR-0133's disambiguation disclosure fires only
for the five colliding vendored modules, none of which `ed-tuesday`
uses, so no disclosure lines contaminate any witnessed output the
manual prints; `trajectory-computation.md` (linked from Chapter 3)
contains nothing ADR-0133 made stale.

## Overall verdict

**PASS with warns.** No fail-grade dimension. Both run-1 FAILs
(dimensions 1 and 4) verified remediated — structurally, not by
hand-witnessing. One erratum (F1), one warn (F2), one cosmetic (F3),
per-strip affirmative record (F4). Findings are register rows; fixes
land as separate, report-citing commits per the author's
reviewer/actor ruling ("Q1 a.").

# Manual review — run 1 (2026-08-13)

Run by the `manual-review` skill (`.agents/skills/manual-review/SKILL.md`),
its own first scored run, against the finished eight-chapter manual
(`docs/manual/00-front.md` through `08-your-own-data.md`, landed this
session, ADR-0125). Whole manual read before any dimension was scored.
`docs/cli.md` regenerated fresh this session (`make cli-doc`) and
confirmed byte-identical to the tracked copy before dimension 7 ran, so
every currency check below is against a confirmed-current file, not a
possibly-stale one.

## Evidence table

| # | Dimension | Grade | Evidence | Rationale |
|---|---|---|---|---|
| 1 | Strip executability | **FAIL** | See "Dimension 1" below — 3 of 8 chapters (06, 07, and 2 of 3 strips in 08) cite a `docs/use-cases/*.md` page or README's own "What you get" fence, neither of which any exerciser or `quickstart-fresh` mechanism re-runs | A real, structural, repeat-offender gap: nothing between sessions catches these strips going stale except the next session that happens to touch that chapter |
| 2 | No reference duplication | PASS | Every `^\|` pipe-table hit in `docs/manual/0*.md` is a "Strip source citations" table (`04-time-on-the-wire.md:149-150`, `05-batch-delivery.md:223-227`, `06-breaking-data-on-purpose.md:173-174`, plus the tables in `07-judging.md`/`08-your-own-data.md`); zero restated flag/operator tables found | Every chapter links `cli.md`/`operators.md`/`judge-calibration.md` rather than copying their content |
| 3 | Anchor stability | PASS | 17 `](path#anchor)` links found across `01`–`08`; every target file's headings were read directly (`README.md`, `SETUP.md`, `docs/cli.md`, `demos/scenarios/ed-tuesday/README.md`, `docs/dev/simulator-architecture.md`, `docs/what-is-this.md`, `docs/formats.md`) and every anchor resolves under the GFM slug rule, including the two non-trivial cases: `docs/what-is-this.md#scope--what-this-deliberately-does-not-do` (heading uses an em dash; doubled space → doubled hyphen, matches) and `docs/formats.md#baseline-mode-changes-the-payloads-shape` (apostrophe in "payload's" dropped, not replaced) | Zero broken anchors found across every cross-reference link in the manual |
| 4 | Glossary linkage on first-use terms | **FAIL** | Only `02-setup-first-corpus.md` (lines 61, 63, 104) and `08-your-own-data.md` (this session's own chapter) link `../glossary.md`. Chapters 01, 03, 04, 05, 06, 07 use glossary-defined terms at first use with zero glossary link: `03-a-simulated-hospital.md` uses "Pathway" (lines 63-77) and "script space"/"truth space" (lines 95-111) — the exact colliding-meaning terms `docs/glossary.md`'s own front matter warns "Getting these backwards is the single most common way to misread a page here" — with no link to that warning anywhere in the chapter; `06-breaking-data-on-purpose.md` uses "operator"/"lineage"/"mutant" throughout without a glossary link (links `operators.md`/`formats.md#the-lineage-record` instead); `07-judging.md` uses "verdict"/"gate"/"judge" throughout without a glossary link | The chapter most likely to cause the exact misreading `docs/glossary.md` names as this workspace's most common (Chapter 3, script/truth space and Pathway) is also the one with zero glossary links anywhere in it |
| 5 | Running-example continuity | WARN | `ed-tuesday` is the worked example in chapters 01, 03, 04, 05 (confirmed by direct citation in each); zero mentions in 06, 07, 08 (`grep -c ed-tuesday docs/manual/06-*.md docs/manual/07-*.md docs/manual/08-*.md` → 0/0/0) | Structurally explained, not an oversight: `ed-tuesday` only emits HL7v2 (`demos/scenarios/ed-tuesday/README.md` never runs `corpus generate synthea` or any FHIR path), and Chapters 6-8 teach FHIR mutation (`storefront-patient.json`), FHIR-gate calibration, and foreign/vendor-corpus intake — none of which `ed-tuesday`'s own output can supply. A real gap in the letter of "one running scenario... every chapter," but a disclosed, structural one, not silent substitution |
| 6 | Maturity honesty | PASS | `01-what-this-is.md:105-116` ("Honest scope") sends the reader to `README.md#maturity` and `what-is-this.md`'s scope section before any chapter teaches a capability; no chapter overclaims — spot-checked `06-breaking-data-on-purpose.md` ("The catalog doesn't promise conviction," lines 141-153, linking `judge-calibration.md`) and `07-judging.md` (the entire `:no-verdict` section, lines 62-100) both disclose real functional limits exactly where the narrative reaches them, not deferred | Satisfies the dimension's own resolution of its tension with dimension 2: point to the authoritative maturity table once, don't restate it, and disclose functional limits in place — all three observed |
| 7 | Currency against generated `cli.md` | PASS (sampled) | `docs/cli.md` regenerated fresh this session, confirmed byte-identical to the tracked copy (no drift to check against). Four specific, falsifiable claims spot-checked and matched exactly: `04-time-on-the-wire.md:14-15` ("`--rate` sets stream-seconds per wallclock-second, and `--board` swaps the... ticker for a bed-state snapshot, rendered every N stream-minutes") vs. `cli.md:293-294`; `05-batch-delivery.md`'s "REQUIRED: no default" framing for `--interval` vs. `cli.md:150`; `07-judging.md:37` ("needs `--profile` explicitly; there's no default bundle") vs. `cli.md:195`; `08-your-own-data.md`'s `--received` default `today` vs. `cli.md:131` | Disclosed as a sample, not exhaustive: 4 claims checked across 2 of 8 chapters, not every falsifiable claim in all 8 — a future run should widen the sample or automate the comparison |
| 8 | Diagram-source presence | PASS | All 5 SVGs under `docs/manual/assets/` carry a derivation comment in their own first 2-4 lines: `gt-emitters.svg:2-5`, `inject-expect-loop.svg:2-4`, `straddle-timeline.svg:2-4`, `two-clocks.svg:2-4`, `verdict-ranking.svg:2-4` | 5/5, no exceptions |

### Dimension 1, in full — every chapter's strip sources, by coverage class

| Chapter | Strip source(s) | Coverage class |
|---|---|---|
| 01 | `README.md` Quickstart fence; `demos/scenarios/ed-tuesday/README.md` | Quickstart-covered / exerciser-covered |
| 02 | `README.md` Quickstart fence (the `generate`/diff sequence composes standard shell tools around a Quickstart-covered command; not itself a second fence) | Quickstart-covered |
| 03 | `demos/scenarios/ed-tuesday/README.md`, "Generate" | exerciser-covered |
| 04 | `demos/scenarios/ed-tuesday/README.md`, "The second clock" | exerciser-covered |
| 05 | `demos/scenarios/ed-tuesday/README.md`, "Batched delivery" | exerciser-covered |
| **06** | `README.md`, **"What you get"** (lines 84-155 — a separate `` ```bash `` fence from the one `` ```sh `` Quickstart fence `quickstart-fresh-test`/`bin/quickstart-demo` walk; `README.md:184-229`) | **neither** |
| **07** | `docs/use-cases/judge-tier-calibration-studies.md`; `docs/use-cases/profile-tier-hl7v2-conformance-gating.md` | **neither** |
| **08** | `docs/use-cases/acceptance-qa-of-vendor-corpora.md` (intake strip); `README.md` Quickstart fence (`README.md:224`, the check strip — Quickstart-covered); `docs/use-cases/regression-baselining.md` (baseline strip) | 1 of 3 strips covered, 2 of 3 **neither** |

`ehrt.docs-tooling.usecases-test` tests `docs/use-cases.edn`'s own schema
and rendering functions — it does not execute any strip's command line.
`ehrt.docs-tooling.invocation-lint-test` forbids one specific retired
invocation string repo-wide — it is a lint against a banned phrase, not
proof any strip still runs. Neither mechanism closes this gap; as far
as this review can find, nothing in this repo's own test suite does.

## Overall verdict

**FAIL.** Two of eight dimensions (1, strip executability; 4, glossary
linkage) fail outright, on real, locatable, repeat-pattern evidence
across multiple chapters — not one-off slips. Per this skill's own
STOP discipline (`.agents/skills/manual-review/SKILL.md`, "A fail-grade
finding on any dimension STOPs..."), this run stops the invoking
session for a ruling before the manual arc is declared closed. Three
dimensions pass cleanly (2, 3, 8); two pass with disclosed scope limits
(6; 7, sampled); one warns on a structurally-explained but real gap in
the letter of the running-example contract (5).

Neither fail is a claim that any chapter's own *content* is wrong —
every strip this session and its four predecessors actually re-ran came
back byte-identical to its witnessed source, with zero divergence found
across all five landing sessions' own ADRs. Both fails are about what
happens **between** sessions: nothing mechanical catches a
`docs/use-cases/*.md`-sourced strip or README's "What you get" fence
going stale the way `quickstart-fresh-test` and the demo exerciser catch
their own two categories; and a first-time reader hitting Chapter 3's
"Pathway"/"script space"/"truth space" — the workspace's own
self-identified most-common misreading — has no in-chapter link to the
glossary entries that exist specifically to prevent that misreading.

This finding is a register row, not a fix — per this skill's own
review discipline (`.agents/skills/manual-review/SKILL.md`, "Do not use
this skill when... the finding needs a fix"), no chapter is edited by
this run. Fixing either gap (widening the exerciser/lint mechanism to
cover use-cases pages and README's second fence; adding glossary links
across Chapters 1, 3-7) is a future session's own charter, pending the
author's ruling this STOP requests.

# Storefront parity audit

Produced 2026-07-29, pre-takeover storefront-polish session, step 3.
Click-depth comparison from the **rendered root README** to six
destinations, for every audience `docs/dev/positioning.md`'s Audience
section names — practitioners (its segment 2) split into two rows,
domain experts and other informaticists, since the two on-ramps
(`SETUP.md`'s copy-paste prompt vs. a Python-comfortable direct read)
differ enough to matter for this audit even though the source document
groups them in one paragraph. **OLD** = `ehr-testing-tools`' final
pre-merge tree (`stable-pre-monorepo`, its own root `README.md` and
`docs/README.md`). **NEW** = this workspace's tree after this session's
checkpoints 1-2 (commits `d00a7b5`, `6db54fc`).

**Click counting convention.** 0 = answered inline on the page already
open. 1 = one link followed (including GitHub's own directory-to-
`README.md` auto-render, e.g. `docs/` → `docs/README.md`). Each
further link adds 1. A target reachable by more than one path is
counted by its *shortest* path — signposting quality (whether the
audience's own named section points there, vs. a reader having to
generalize from a neighboring section) is called out in prose below the
table, not folded into the number, so a real depth tie doesn't hide a
real discoverability loss.

## Table

| Audience | why-care | first-runnable-command | full-command-reference | evidence | LICENSE | contributing |
|---|---|---|---|---|---|---|
| **Guide readers** (method-first) | OLD 1 (`docs/README.md` §Method-first) → NEW 1 (`docs/README.md` §Guide reader) | OLD 2 (→ `use-cases.md`'s **You type:**) → NEW 2 (`use-cases.md`, via the neighboring §Task-first section, not this row's own — see finding F-G1) | OLD 2 (`pipeline.md` via §deep walk, same page) → NEW 2 (`docs/dev/positioning.md`'s referral-trigger sections) | OLD 1 (§deep walk's own EXP index, same page) → NEW 2 (individual EXP citations in `use-cases.md`/`components.md`; no live consolidated index — see finding F-G2, ruled not a regression) | 1 → 1 (root `LICENSE`, unchanged) | 2 (`docs/README.md` §Contributor → `AGENTS.md`) → 1 (root `README.md` §Contributing → `AGENTS.md` directly) |
| **Domain experts** (interface analysts, clinical informaticists) | OLD 0 (root pitch + persona sentence) → NEW 0 (root pitch + persona sentence, restored checkpoint 1) | OLD 1 (root Quickstart fence) → NEW 1 (same) | OLD 2 (`docs/README.md` §Task-first → `cli.md`) → NEW 2 (same path) | OLD 0 (root Maturity table's own EXP links) → NEW 0 (same, restored + extended checkpoint 1) | 1 → 1 | 1 (root, inline "see AGENTS.md" mention, uncommented but not a hyperlink in the old text — see finding F-C1) → 1 (root §Contributing, a real link) |
| **Other informaticists** (task-first, Python-comfortable) | same as domain experts row (positioning.md source groups them; SETUP.md's on-ramp is the only differentiator, covered below) | OLD 1 → NEW 1 | OLD 2 → NEW 2 | OLD 0 → NEW 0 | 1 → 1 | 1 → 1 |
| **Contributors** | OLD 2 (`docs/README.md` §Contributor's own framing) → NEW 1 (root §Contributing's own framing) | OLD 2 (`docs/README.md` → `AGENTS.md`'s `poly check`/`make test` mention) → NEW 1 (root §Contributing → `AGENTS.md`) | N/A both (contribution rules, not CLI flags — `AGENTS.md`/`AUTHORS-GUIDE.md` at 1-2 clicks both versions) | N/A both | 1 → 1 | 1 (`docs/README.md`, root has no direct link — F-C1) → 1 (root, direct) |
| **The AI assistant** (reader in its own right) | OLD 1 (root persona sentence → `SETUP.md`) → NEW 1 (same, restored checkpoint 1) | OLD 1 (`SETUP.md` §5's copy-paste prompt) → NEW 1 (`SETUP.md` §5, restored checkpoint 1 — this section did not exist in this workspace's tree before checkpoint 1; see finding F-A1) | OLD 2 (`docs/README.md` §AI-assistant → `cli.md`/`ehrt help`) → NEW 2 (via §Task-first, same as Guide readers' F-G1 pattern) | OLD 0 (root Maturity table) → NEW 0 | 1 → 1 | 1 → 1 |
| **Downstream data consumer** | OLD 2 (`docs/README.md` §Downstream) → NEW 1 (`docs/README.md` §Downstream, now one paragraph instead of a 3-step list — shallower, not deeper) | N/A both (never runs the CLI) | OLD 2 (`formats.md`) → NEW 2 (same) | OLD 3 (`docs/README.md` §Downstream step 2 → `judge-calibration.md` anchors) → NEW 1 (root Maturity table's own `judge-calibration.md` link, checkpoint 1) — net improvement, though `docs/README.md`'s own dedicated pointers to `judge-calibration.md`/`locators.md` for this audience are gone (F-D1) | 1 → 1 | 1 → 1 |
| **Clojure library consumer** | OLD 2 (`docs/README.md` §Clojure-library-consumer → `positioning.md`) → NEW 1 (root `README.md` → `docs/dev/positioning.md` directly) | N/A both (post-first-release only) | N/A both | N/A both (nothing shipped to verify yet, both versions say so) | 1 → 1 | 1 → 1 |
| **Evaluator** (adopt or not) | OLD 0 (root Maturity + inline Scope) → **NEW 0, after this session's fix** (Scope restored to root, this commit — was 1, `docs/what-is-this.md`, before the fix; see finding F-E1) | N/A both | N/A both | OLD 0 (root Maturity table) → NEW 0 | 1 → 1 | N/A both |

## Findings

**F-E1 — Evaluator row, Scope, 0→1 click. FIXED, this commit.** The
audience-fork README rewrite (ADR-0010) dropped the root `README.md`'s
own inline `## Scope` section (four bullets: no semantic checking, no
full terminology validation, no message routing, no hosted service) in
favor of `docs/what-is-this.md`'s fuller version — a real regression
for the Evaluator, whose canonical "why-care" page (`README.md` itself,
per `docs/dev/positioning.md` segment 7) lost one of its own load-
bearing sections. Not something ADR-0010 actually required: that
record dispositions the 76 files *under* `docs/`, not root `README.md`
content, and the root Maturity table already keeps its own inline
summary alongside a pointer to the fuller doc — the same pattern now
restored for Scope. Fixed by re-adding the section (same four bullets,
adapted) with a link to `docs/what-is-this.md`'s full version, same
commit as this audit.

**F-G1 / F-A1(partial) — "the neighboring section" signposting loss.**
`docs/README.md`'s old form gave **Method-first guide reader** and
**The AI assistant** each their own explicit pointer to `use-cases.md`
(guide readers) and to `ehrt help`/`cli.md` (the assistant) inside
*their own* section. The new `docs/README.md` still reaches the same
targets at the same click-depth, but only via the **Task-first
practitioner** section — a reader following their own row's prose
won't be told to cross over. Depth ties (2→2), so not a hard
regression by this audit's own counting rule, but a real discoverability
softening. **Named for author decision, not fixed here**: fixing it
well means either duplicating a line in each section (small, but a
content fork that can drift) or restructuring `docs/README.md`'s
routing prose (a redesign this session's own instructions reserve for
a dedicated pass, not a drive-by edit here).

**F-G2 — the consolidated experiments index has no live pointer.**
Old `docs/README.md`'s "deep walk" section ended with a full inventory
of every EXP (protocol + results links, including backlog/not-yet-run
items) in one place. That page, `experiments.md`, still exists
(`components/tools/docs/experiments.md`) but `notes/docs-audit.md`
explicitly dispositions it **COMPONENT-ADJACENT-STAY**, "cited by
facts-register-style claims, not by any user or general-maintainer
path" (ADR-0010). This is a **ruled** disposition, not an oversight —
disclosed here per this audit's own transparency norm, not reopened.
The specific evidence claims this index used to back are individually
reachable (often at *shorter* click-depth now — see the Maturity table
row above), which is the actual load-bearing claim; the index itself
was a convenience, not a unique path to otherwise-unreachable evidence.

**F-D1 — downstream consumer's dedicated `judge-calibration.md`/
`locators.md` pointers are gone from `docs/README.md`.** Old gave this
audience three explicit steps (`formats.md`, two named
`judge-calibration.md` anchors, `locators.md`); new gives one paragraph
(`formats.md` only). Net click-depth is *not* worse — the
`judge-calibration.md` link this audience needs is now 1 click from
root via the Maturity table (better than old's 3) — but `locators.md`
(reading a finding's own `:locator` back) has no path from this
audience's own section in either version's `docs/README.md`; it was
already collateral in old's own step 3, not this audience's dedicated
content. **Not a regression this audit's rule catches** (no cell got
deeper), named for completeness.

**F-C1 — old root README's "see AGENTS.md" was prose, not a link.**
Listed for the record: old `README.md` never linked `AGENTS.md`
directly from root at all (only a code-comment mention inside the
Quickstart fence, `# Run the test suite (hermetic — see AGENTS.md)` —
unclickable). The Contributor row's old "1" in the table above is
generous; a literal reading is closer to "0.5" (a citation, not a
navigable link). New root `README.md`'s `## Contributing` section is a
real, clickable improvement over old on every count for this row.

## Before-takeover decision items

One item carries forward to the author, per this audit's own rule
(worsened cells either get fixed in this session or named for a
decision — nothing worsened is silently accepted):

- **F-G1** (signposting: guide-reader/AI-assistant rows don't see their
  own section point at `use-cases.md`/`cli.md`, though the targets
  remain reachable at unchanged depth via the neighboring section).
  Recommend: a future `docs/README.md` editorial pass adds one line to
  each of those two sections rather than restructuring the page; not
  urgent enough to block the fast-forward push, since no cell's actual
  depth number regressed.

No other row/column combination in the table above worsened after
F-E1's fix; F-G2 and F-D1 are disclosed but not regressions by this
audit's own counting rule (F-G2 is a ruled disposition, F-D1 is a
net improvement with one already-collateral gap named for completeness).

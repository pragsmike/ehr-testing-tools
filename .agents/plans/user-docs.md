# Plan: User Documentation (DOC waves)

**Status (2026-07-26): scheduled waves complete.** Adopted 2026-07-25
as an active driver plan following a documentation audit
(design-channel session, 2026-07-25; the audit's findings are
compressed into this document so it stands alone — evidence over
memory). DOC-1 through DOC-5 (plus the two interludes, LOC-1 and
CLI-2) all landed between 2026-07-25 and 2026-07-26 — see the Tracker
at the bottom for each wave's own evidence trail. Completion here
triggers nothing by itself: the Deferred register below (cljdoc,
namespace demarcation, guide cross-references, "since"-markers) is
explicitly release-gated and stands unchanged; first release remains
the author's own gate (`.agents/plans/corpus-foundations.md`), not
something this plan's own completion advances. One prompt-session per
wave, prompts archive to `.agents/prompts/archive/` as usual.

**Companions:** `docs/positioning.md` (owns the audience definitions;
DOC-2 extends them), `docs/README.md` (the reading-order spine DOC-2
reshapes), `docs/use-cases.md` / `docs/use-cases.edn` (DOC-4's
target), `README.md` (quickstart — DOC-5's extraction source),
`SETUP.md` (**out of scope**: externally validated by the trial
cohort's 15-minute result; nothing below touches it except
cross-links).

**Goal served:** the run-the-tools audiences can find their use case,
type the commands for it, and read the outputs — without opening
Clojure source or contributor-register documents (ADRs, the design
doc). The organizing finding: most user-needed knowledge already
exists but in the wrong register; the work is extraction and
re-expression, plus one code wave (CLI help) and one enforcement wave
(executable quickstart), not research.

**Operating rules** (inherited from the house discipline): one
semantic change per commit; golden check
(`make pipeline && make use-cases && make operators-doc && make
cli-doc && git diff --exit-code docs/pipeline.md docs/use-cases.md
docs/operators.md docs/cli.md` — extended by DOC-3 from the
two-target form these rules were written against) proves
behavior-neutral sessions and trips on scope creep; evidence over
memory — every claim about the current CLI/doc surface is re-verified
against the repo at session time, not taken from this plan.

---

**Spent content (audience/audit context, all seven wave bodies, the Tracker) archived verbatim to `.agents/plans/archive/user-docs.md` (2026-07-27, NAV-1).**

## Deferred register (not scheduled; ride with first release)

- **cljdoc** — automatic once Clojars/Maven coordinates exist
  (`docs/positioning.md` open decision; the First-release row in
  `corpus-foundations.md`).
- **Public/internal namespace demarcation** — a docstring convention;
  near-zero cost, may slot into any wave above opportunistically.
- **Guide → tools cross-references** — positioning's referral
  trigger; waits for release by design.
- **"Since version" maturity markers** — premature before a first
  version tag exists.

## Open decisions

- **Operator-listing verb name** (author; blocks DOC-1's Step 3;
  recommendation: `ehr corpus operators`).
- **DOC-3 generated vs. hand-written**, per document — **decided
  2026-07-25 (author), as recommended**: `operators.md` and `cli.md`
  are generated (from the registry and from DOC-1's `cli-spec`
  respectively, on the pipeline.md/use-cases.md
  renderer-plus-freshness-gate pattern); `locators.md` and
  `formats.md` are hand-written with source citations. The `cli.md`
  branch resolved in favor of generation: Step 0 found `cli-spec` rich
  enough, needing one added flag.
- **DOC-4 route**: `:commands` field vs. cookbook — **decided
  2026-07-26 (author), as recommended**: the `:commands` field in
  `docs/use-cases.edn` plus a renderer extension, so the strips are a
  single source of truth living in the same freshness-gated document
  as the equations they ground, and the golden check inherits them. A
  separate `docs/cookbook.md` would have been a second place to rot.
- **CLI entry point** (raised by DOC-4's second finding) — **decided
  2026-07-26 (author): option (b)**, a `bin/ehr` wrapper as the taught
  entry point, rather than trying to make the Makefile propagate the
  child's status. The premise was measured before the wrapper was
  built (CLI-2 Step B0): GNU make's own exit status is 0/1/2 by
  definition, so a recipe cannot carry ADR-0004's 1 or ADR-0010's 3 no
  matter how it is written. `make ehr` stays as a compatibility
  spelling.
- **Sequencing against first release** — **decided 2026-07-25
  (author): now, pre-release**, accepting the soft interface-hardening
  pressure a full CLI reference creates. Mitigation shipped with it:
  both generated docs carry a one-line pre-release notice pointing at
  README.md's maturity table.

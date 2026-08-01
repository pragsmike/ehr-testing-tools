# 2026-08-01 — ehr-testing-tools: storefront truth + ruled literals (producer name, test-tree bless)

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `1641e11`
("Update README."), already equal to `origin/main` — no fast-forward
needed. No commit or push run by this session; the tree is left
uncommitted, coherent, with the proposed commit message printed in
the session's close-out. `/mnt/c` clone not touched (all edits made
via the UNC path onto the WSL ext4 clone, per the
dual-clone-edit-hazard discipline).

## Original prompt (verbatim)

2026-08-01 — ehr-testing-tools: storefront truth + ruled literals (producer name, test-tree bless)
Context
Three small ruled items, one session. (1) The revised README (commit `1641e11`) landed with a fabricated sample output block (marked `CAPTURE-BEFORE-LANDING` in an HTML comment) and an unverified CI claim — replace both with captured reality, and add the register tripwire so the storefront can't drift back into the internal logbook. (2) Author ruling 2026-08-01: operation manifests' `:producer :name` becomes `"ehrt"` — the product name, decoupled from component layout permanently. (3) Author ruling 2026-08-01: the stage-3 project test-tree naming (`ehrt.conformance.*` / `ehrt.integration.*`) is blessed — record it, closing ADR-0018's one disclosed unruled call.
Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `1641e11`), record HEAD. No commit/push; per-push lane at the end; `/mnt/c` untouched. Precedent for all conventions: the 2026-07-31 session archives in `notes/prompts/`.
Author rulings

* AR-1 Captured, not composed. The README's "What you get" block must be real output from actually running the mutate + `gate fhir` commands shown above it, on a corpus generated this session — trimmed for length if needed (say so with an ellipsis line), never paraphrased or prettified. Delete the `CAPTURE-BEFORE-LANDING` comment. If real output differs materially from the fabricated block's shape (it may — the block was invented), the surrounding prose ("The rejection is the point...") gets adjusted to match reality, not vice versa.
* AR-2 The CI claim tells the truth. Determine where `make quickstart` (the fence) actually runs — per-push workflow, nightly, or manual. The README sentence ("asserted by CI on every push") is corrected to exactly that. While there: check what `bin/quickstart-demo` asserts (commands only, or comments too), and sync the script's comments to the README's new user-facing register so the fence teaches one voice — commands stay byte-identical throughout.
* AR-3 Register tripwire. Extend the stale-path tripwire family: README.md body text may not contain internal provenance codes — patterns `ADR-\d`, `EXP-[A-Z]?\d`, `DOC-\d`, and bare `D\d+` ruling codes. Link destinations and HTML comments are exempt (the Evidence column's hrefs legitimately contain `EXP-A4-results.md`): strip markdown link targets `](...)` and comments before matching, and prove both directions per the usual red→green discipline — a seeded `ADR-0012` in prose must trip it; the existing EXP hrefs must not.
* AR-4 Producer name `"ehrt"`. Change the manifest writer's literal; regenerate every golden fixture that embeds an operation manifest; add a test pinning the literal (so the next rename can't silently change output vocabulary again); check whether the operation-manifest schema carries a version field and note the bump-or-not decision (default pre-release: no bump, recorded). Dated ADR note citing the ruling.
* AR-5 Bless line. The test-tree naming bless is recorded in the same ADR note — one dated sentence, no code changes.
* AR-6 Fence. Nothing else: no sim work (two follow-on sessions own P3-5/P3-6), no other named-futures.

Steps

1. Generate the demo corpus and mutant per the README's own commands; capture the real `gate fhir` output; land AR-1 and AR-2's README/script edits.
2. Register tripwire per AR-3, red→green both directions.
3. Producer literal per AR-4: writer, goldens, pinning test, red→green (the pinning test red against `"ehrt.tools"` first).
4. Records: one dated ADR note covering AR-4 + AR-5 and the README capture; facts- register row + Index; archive at `notes/prompts/2026-08-01-ehr-testing-storefront-and-ruled-literals.md` with deviation record. Per-push lane green.

Proposed commit message: `fix: storefront shows captured output, register tripwire guards it; producer name is "ehrt" (ruled 2026-08-01), goldens regenerated, literal pinned; test-tree naming blessed`
Close-out summary for the author
HEAD at start; the real captured output vs. the fabricated block (diff of shape, so the author sees what the invention got wrong); where the fence actually runs and what it asserts; tripwire red→green records; golden regeneration file count; anything surprising.

## Deviation record

**AR-1's "adjust prose to match reality" clause was exercised more
substantially than a shape mismatch — the fabricated block's entire
premise didn't hold.** The fabricated example ("`Patient.gender:
minimum required = 1, found 0`") implied the deliberately-planted
defect is what the gate catches. Running the real commands found
otherwise: base FHIR's `Patient.gender` is cardinality 0..1, not
required, so `remove-required-element` at that locator earns zero new
findings — the same Synthea patient gates `rejected` with the
identical 2560-finding count both before and after the mutation
(verified directly, by gating the unmutated file too). This exact
caveat already lived in `docs/judge-calibration.md`'s own defect/
locator table (a prior session's finding, not new), just not reflected
in the README or the matching Quickstart-fence/`bin/quickstart-demo`
comment. Per AR-1's own instruction, the prose was rewritten to state
this plainly rather than keep a punchier but false causal claim — the
alternative (swapping in a locator that genuinely is required, e.g.
`resourceType`) was considered and rejected as exceeding AR-1's letter
("real output from actually running the mutate + gate fhir commands
shown above it") and AR-6's fence — that would have been changing the
demo, not just correcting its account of it.

**A real bug found and fixed, not anticipated by the prompt.** The
prior "Update README" commit (`1641e11`) had, as an accident of adding
the "What you get" teaser box, introduced a *second* ` ```sh ` fence
into README.md. `ehrt.docs-tooling.quickstart-fresh`'s own extractor
is anchored to "the one ` ```sh ` fence in README.md" (its own
docstring's stated invariant) and, given two, silently reads the
*first* one — the teaser box, not the real Quickstart fence. Running
`make quickstart-fresh` for real (Step 1) failed outright on this,
independent of anything this session's own edits caused — apparently
never run against `1641e11` before this session (nothing in this
repo's own history shows a `quickstart-fresh` run at that commit).
Fixed non-invasively: retagged the teaser box ` ```bash ` (still
shell-syntax-highlighted, no longer string-equal to the extractor's
`` ```sh `` anchor) rather than touching the extractor itself or the
real Quickstart fence — the narrowest fix consistent with AR-6's
fence, confirmed green afterward. Recorded rather than silently
absorbed into AR-1/AR-2's own scope, since the prompt didn't
anticipate it.

**AR-2's CI claim correction surfaced a second, pre-existing
discrepancy, left as a named-future per AR-6.** `ehrt.docs-tooling.
quickstart-fresh`'s own docstring claims the invariant it checks is
"the fast, per-push half of DOC-5's two gates (the slow half, actually
running the commands, is `bin/quickstart-demo` / `make
quickstart-demo`, integration-tier)" — implying `integration.yml`
should run it nightly. Reading `.github/workflows/integration.yml`
directly found no such step: it primes the artifact cache and runs
`poly check` + `poly test :all project:integration` only. Whether this
is drift (a step lost during the Polylith carve, `notes/
carve-loss-audit.md`'s own "DROPPED-WRONGLY" finding covers the
Makefile targets but not this workflow step specifically) or a
deliberate never-fully-wired state wasn't investigated further — AR-6
scopes this session to correcting the README's *claim*, not fixing CI
wiring. README now says plainly: `quickstart-fresh` (the structural
check) is asserted every push; `make quickstart` (real execution) is
local/manual today, no CI workflow runs it. Flagged here for a future
session, not fixed.

**AR-2's "sync the script's comments" was read as covering the
existing Quickstart-fence inline comment too, not just
`bin/quickstart-demo`'s own file.** The overclaiming phrase ("a
genuine defect in the mutant, correctly caught") existed in *both*
places — inside README.md's own Quickstart fence (a comment line,
stripped by `quickstart-fresh`'s own comment-blank filter, so editable
without touching the structural fence) and in `bin/quickstart-demo`
verbatim. Both corrected identically, plus `bin/quickstart-demo`'s
header docstring softened at the one spot making a similar (weaker,
still slightly overclaiming) point about "a genuine defect, correctly
caught" as its own general design rationale for the `expect` wrapper —
none of these are taught command lines, so none of this touches
`quickstart-fresh`'s byte-identity invariant.

**AR-4's "regenerate every golden fixture that embeds an operation
manifest" found a second, independently stale literal, not created by
this session's own ruling.** `docs/formats.md`'s illustrative manifest
and `ehrt.corpus-io.operation-manifest-test`'s fixture both read
`"ehr-testing-tools"` — an even older name than `"ehrt.tools"`,
predating the Polylith-era rename entirely. Neither needed the
2026-08-01 ruling to be wrong; both were already inconsistent with the
actual code (`bases/cli/src/ehrt/cli/core.clj`'s own
`"ehrt.tools"` literal) before this session touched anything. Brought
in line with the new `"ehrt"` literal directly rather than first
"fixing" them to the old `"ehrt.tools"` and then re-editing — the
end state is the same either way, and the intermediate step would have
been pure churn.

**Facts-register entry is Index-only, matching current practice
(F14–F17's precedent), not the older dual Index+Register shape** —
same call already recorded in the prior session's own archive
(`2026-07-31-ehr-testing-gate-hardening.md`), reused here without
re-litigating it.

**Nothing else touched, per AR-6's fence:** no sim work, no other
named-futures beyond producer-name (item 4) and the test-tree bless —
the structure-currency/palgebra-deps-lint items (3, 5) were already
closed by the prior session; `generator-source.clj`'s three concerns
(item 1), `ehrt.corpus.display`'s presentation-leaning placement
(item 2), the duplicated markdown-table helpers (item 6), and the
`bases/sim-cli`/`projects/sim` retirement trigger (item 7) are
untouched, as this session's own fence requires.

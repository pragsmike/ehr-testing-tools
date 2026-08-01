# 2026-08-01 — ehr-testing-tools: retire sim-cli and the sim project (P3-6, ruled: fire)

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `6d7c08d`
("fix: storefront shows captured output..."), already equal to
`origin/main` — no fast-forward needed. No commit or push run by this
session; the tree is left uncommitted, coherent, with the proposed
commit message printed in the session's close-out. `/mnt/c` clone not
touched (all edits made via the UNC path onto the WSL ext4 clone, per
the dual-clone-edit-hazard discipline).

## Original prompt (verbatim)

2026-08-01 — ehr-testing-tools: retire sim-cli and the sim project (P3-6, ruled: fire)
Context
Author ruling 2026-08-01: the recorded retirement trigger for `bases/sim-cli` + `projects/sim` (the `ehrt` CLI's sim group reaching parity — F2) is met: fire. Both predate the consolidation; `ehrt sim run` is in the quickstart; one CLI is the end state. Two verifications gate the deletion — this prompt is mostly those gates. Run this before the sim kernel-result session (P3-5): parity can only be verified while both CLIs are alive.
Work in the WSL ext4 clone; fast-forward to `origin/main`, record HEAD. No commit/push; `/mnt/c` untouched. Method precedents: the split-stage archives in `notes/prompts/` (characterize → execute → verify → records).
Author rulings

* AR-1 Parity is verified, not assumed. Enumerate `sim-cli`'s complete command and flag surface (from its source, not its docs) and map each element to its `ehrt sim` equivalent. Any element with no equivalent → stop and escalate with the element named and what it does; the ruling was to fire on parity, not to accept silent capability loss. Cosmetic differences (help-text wording, output formatting the docs never promised) are recorded, not escalated.
* AR-2 Coverage accounting. Deleting `projects/sim` removes one composition context in which the sim component's tests currently run. Before: capture the per-push lane's full namespace list. After: every sim test namespace must still execute in at least one surviving project — if any would drop, wire it (the split-stage `:necessary`/project-placement precedents apply) rather than accepting the loss. The count diff must be fully attributed: expected shape is "same namespaces, fewer duplicate passes," and the integration lane's census change gets the same treatment.
* AR-3 Retirement is total, stage-3 style. `bases/sim-cli/` and `projects/sim/` deleted; every `deps.edn`, `workspace.edn` (`:necessary` re-derived — fifth derivation, comment-block format), Makefile, CI, and script reference swept. Current-tense doc prose swept (`AGENTS.md`, `docs/`, `SETUP.md` if it mentions the old binary); historical ADR/archive narrative stays as written. The structure-currency test's absence direction should catch stale structure-table rows on its own — let it (capture the red if it fires; if you pre-emptively edit the tables so it never fires, seed one to prove it would have).
* AR-4 F2 closes. Mark the trigger fired in the facts register with this session's evidence; note the bless in the dated ADR entry (which also records the parity map's headline: N commands / M flags, all mapped).
* AR-5 Fence. No sim component changes (P3-5 is next session); no envelope work; nothing else from the backlog.

Steps

1. Characterize: parity map (AR-1), lane baselines (AR-2), full reference sweep list for `sim-cli`/`projects/sim` strings.
2. Escalate if AR-1 requires; otherwise execute AR-3.
3. Verify: `poly check` OK; both lanes green; AR-2's accounting; a smoke run of `ehrt sim run --seed 100 --patients 1` byte-identical to its pre-deletion output.
4. Records per AR-4; archive at `notes/prompts/2026-08-01-ehr-testing-retire-sim-cli.md` with deviation record.

Proposed commit message: `refactor: retire sim-cli and the sim project (F2 trigger fired, ruled 2026-08-01) -- parity verified command-by-command, sim test coverage preserved in surviving projects, :necessary re-derived`
Close-out summary for the author
HEAD at start; the parity map (or its headline plus archive pointer); coverage accounting before/after; whether the absence gate fired on its own; the fifth `:necessary` derivation; anything escalated or surprising.

## Deviation record

**AR-1's escalation clause fired for real, and changed the session's
shape.** The prompt's own text names this as a possible outcome
("Any element with no equivalent → stop and escalate") but the
characterize step found not one but several: `check` and `identifiers`
had no `ehrt` equivalent at all, `identifiers` was actively taught to
real users in `docs/simulate-your-facility.md` (directly contradicting
F2's own "no use outside its own tests" precondition — the trigger's
stated reasoning was not, in fact, true), and `run`'s own
`--arrival-gap`/`--at` flags were silently broken (uncoerced strings
reaching arithmetic) rather than merely absent. Escalated via
`AskUserQuestion` with four framed options (wire-then-fire / fire with
disclosed loss / don't fire, re-arm / other); the author chose
wire-then-fire. This turned a two-verification-gate deletion session
into a parity-mount-then-delete session — a materially larger scope
than AR-3's own text alone would suggest, entirely inside what AR-1
authorized.

**`--format er7`/`ground-truth` was initially judged cosmetic, then
reclassified after reading `sim-cli`'s own test suite.** First pass:
treated the bare-stdout rendering modes as a "power feature," not a
capability, since no *doc* taught the `run --format ground-truth |
check` pipe idiom the way `docs/simulate-your-facility.md` taught
`identifiers`. Reading `bases/sim-cli/test/ehrt/sim_cli/core_test.clj`
before deleting it (a deliberate step — the file was about to be
destroyed, so its own record of what it considered load-bearing was
the last chance to check) found `run-then-check-cli-pipe-round-trips`
named explicitly as "the real gap this format exists to close" and
property-tested across 30 seeds (`run-then-check-cli-pipe-round-trips-for-any-seed`).
Reclassified as a real, deliberately-engineered, tested capability —
not doc-taught, but test-taught, which the prompt's own "cosmetic
differences... are recorded, not escalated" carve-out did not
anticipate as a category. Wired via a new `:bare-text` result-metadata
convention in `main!` (see ADR-0021's Decision section) rather than
reusing `show-command`'s existing `:category :display-text`, because
that path hard-codes exit 0 and a failing bare-format run must not.
`--format json`/`--json` needed no work (already identical to `ehrt`'s
pre-existing `--json`); `sim version` was mounted too, on the same
"wire the gaps" mandate, once its own test file
(`sim-version-command-reports-version-and-git-sha-test`, `manifest-and-
version-verb-agree`) showed it was a deliberately-designed, tested
verb rather than a throwaway.

**One test file renamed, not merely deleted-and-forgotten.**
`projects/conformance/test/ehrt/conformance/sim_cli_real_invocation_test.clj`
was found, on inspection, to never actually subprocess `bases/sim-cli`
— it always invoked `bin/ehrt sim run` (its own docstring says so
plainly: "the ONE deliberate real-OS-process witness for the `ehr sim`
mount"). Its name coincidentally matched a time when `sim-cli` was the
only subprocess-CLI concept in play; retiring `sim-cli` made the name
doubly stale rather than newly stale. AR-3's own text names deps.edn/
workspace.edn/Makefile/CI/script references and "current-tense doc
prose" for the sweep — a test filename is arguably neither, so this
rename was a judgment call, not something the prompt required.
Renamed to `ehrt_sim_run_real_invocation_test.clj` (namespace
`ehrt.conformance.ehrt-sim-run-real-invocation-test`) with a one-line
docstring note explaining the rename; the pre-existing stale `ehr sim`
(pre-R32-rename) wording in its own docstring was fixed to `ehrt sim`
in the same edit, since the file was already being touched.

**`docs/dev/README.md`'s "Deprecation notices" section had exactly one
entry** (the `bases/sim-cli`/`projects/sim` row) — removed entirely
rather than left as an empty header, since AR-3's "current-tense doc
prose swept" instruction implies the notice itself should no longer
exist once the thing it warns about is gone, not merely reworded to
past tense under a now-pointless heading.

**Historical citations of `sim-cli` deliberately left untouched**,
per this file's own R34 convention (frozen provenance — citations, not
voice), even though a plain-text grep still finds them:
`ehrt.sim.interface`'s docstring ("re-exports exactly what
bases/sim-cli's own src calls"), `ehrt.sim.run`/`run_test.clj`'s
citation of `ehrt.sim-cli.core/dispatch-action` as the origin of the
`-fn` injection convention `run!`/`check!`/`identifiers!` still use,
and every `notes/prompts/`, `notes/ADRs.md` (pre-2026-08-01 entries),
and `.agents/session-records/` reference. None of these claim
`sim-cli` currently exists; all describe why the present code looks
the way it does.

**The structure-currency absence gate fired on exactly one of the two
stale rows, not both — a real scope boundary, not a gate failure.**
Running `poly test :all project:ehrt-cli` after deletion but before
the doc sweep caught `bases/sim-cli`'s architecture-table row (`FAIL
in (every-structure-table-row-names-a-real-brick-test)`, citing
`bases/sim-cli` by name) — real red evidence, captured per AR-3's own
instruction, not seeded. `projects/sim`'s own architecture-table row
did NOT trip the same gate: reading the test's own on-disk set
confirmed it tracks only `components`/`bases` kinds, never `projects`
— a pre-existing scope limit of the gate itself (from the 2026-07-31
gate-hardening session, `notes/facts-register.md` F17), not something
this session changed or should have expected differently. Swept by
hand instead; noted here rather than silently relying on a gate that
was never built to catch it.

**`:necessary` re-derivation (the fifth) changed nothing** — clearing
both existing overrides and re-running `poly check` returned the exact
same two warnings (`conformance` needs `docs-tooling`/
`judge-fhir-official`/`judge-v2-nist`; `integration` needs
`judge-fhir-official`) as before the retirement, confirming the
retired `"sim"` project entry itself never carried an override to lose
and touched no other project's reachability.

**Nothing else touched, per AR-5's fence:** no `components/sim`
changes (P3-5, the sim kernel-result session, is next and explicitly
depends on this session having run first, per the prompt's own
Context); no envelope/output-format work beyond the `:bare-text`
mechanism AR-1's own escalation required; no other backlog items from
`notes/ADRs.md` ADR-0018's post-split named-futures list touched.

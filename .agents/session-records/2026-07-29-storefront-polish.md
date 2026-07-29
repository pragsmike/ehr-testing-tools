# 2026-07-29 — Pre-takeover storefront polish: README parity, self-references, parity audit, SETUP re-validation

## Scope

Autonomous session (R30, ADR-0007) run as the last set of commits
before the fast-forward push that lands this workspace's history onto
`pragsmike/ehr-testing-tools`. Four checkpoints: (1) four found
README regressions relative to the old tools storefront fixed in one
commit — H1/self-name, CI badge, the persona sentence, Maturity-table
evidence links plus the SETUP.md AI-assistant on-ramp surfaced; (2) a
grep-verified sweep of every live-doc self-reference to the interim
repo's URL; (3) `notes/storefront-parity-audit.md` — click-depth
comparison, old tools README vs. this session's new one, across
`docs/dev/positioning.md`'s seven audiences (practitioners split into
domain experts / other informaticists, eight rows), fixing the one
regression found and naming the rest; (4) a fresh-context agent walk
re-validating `SETUP.md`'s on-ramp against the bar R40 named, with the
friction it found fixed in `SETUP.md`'s own Troubleshooting section.

## Staging hygiene (R26e)

Each checkpoint's `git diff --cached --stat` was reviewed before its
commit; every one matched that checkpoint's own stated scope exactly
— no cross-checkpoint bleed:

- Checkpoint 1: `README.md`, `SETUP.md` (2 files, 30 lines).
- Checkpoint 2: `SETUP.md` (1 file, the clone URL only).
- Checkpoint 3: `README.md` (the Scope-section fix), `notes/storefront-parity-audit.md` (new).
- Checkpoint 4: `README.md` (one Quickstart comment), `SETUP.md`
  (four Troubleshooting bullets), `notes/facts-register.md` (F3).
  An untracked `out/` (the fresh-context agent's own demo output,
  created by working the Quickstart for real in this actual working
  tree) was deliberately left unstaged — ephemeral generated output,
  not gitignored but never meant to be committed, matching this
  workspace's own `target/corpus/` convention (`AUTHORS-GUIDE.md`,
  "generated corpora are never committed").

## R40/R41 — the SETUP re-validation walk

A fresh `general-purpose` agent, no prior turns, no repo knowledge, was
handed only the rendered root `README.md` (verbatim, this session's
own post-checkpoint-1 text) and the single instruction "get to a
generated, judged corpus on this machine" — R41's own no-repo-knowledge
constraint, enforced structurally (a genuinely fresh agent context)
rather than by asking a repo-aware session to role-play naivety.

**Result: reached a generated, judged corpus in 15 commands**, one
self-recovered failure included (a stale `target/corpus/synthea-s1-p5`
directory from prior dev-machine activity, correctly diagnosed from its
own fake manifest content and removed — not a fresh-clone condition,
disclosed as such in the walk's own report). Both judging paths
verified for real: `gate v2` 5/5 pass; `gate fhir` correctly rejected
the mutant (exit 1, 2560 cascading findings from the removed `gender`
field, exactly as the Quickstart's own comment predicts); `check` 7/7
pass. `clojure -M:poly test :all` was started but its own completion
wasn't confirmed within the walk's session — disclosed as a limitation
of the walk's own tool-process lifecycle, not a claim about the suite
itself, and not a precondition for "generated, judged corpus," which
step 14 (`check`) already satisfied.

**Four friction points found, all fixed this session** (`SETUP.md` §4
and one `README.md` Quickstart comment): `corpus generate`'s
`:out-dir-exists` rejection was undocumented; a harmless `run!`
namespace-shadow warning (`ehrt.tools.sim`) leaks onto stdout; `gate
... --report <file>` still dumps the full result to stdout alongside
writing the file; `clojure -M:poly test :all`'s multi-minute runtime
was unstated. Three further points were disclosed but NOT fixed,
judged out of this session's own scope (friction fixes to
`SETUP.md`/quickstart only, no redesign, no source changes): the
README's two-entry-point fork (`docs/what-is-this.md` vs. Quickstart)
gives no explicit steer for a pure generate/judge goal; `SETUP.md`'s
verification ladder's `git clone` line doesn't state it's skippable
for an existing clone; the walk's own working tree carried substantial
pre-existing `target/` clutter from prior dev-machine sessions, which
is a real-machine condition, not something a genuinely fresh clone
would have.

**R40's bar — a deviation, recorded, not silently absorbed.** The
"documented success report from a domain expert (a Python developer)"
R40 names as the bar could not be located verbatim anywhere in git
history. This workspace's own `.agents/session-records/` discipline
traces to sim only (`notes/discipline-parity-audit.md` row M13);
tools-era sessions — including
`notes/tools/prompts/2026-07-24-onboarding-wave.md`, the actual session
that built `SETUP.md` for exactly this trial audience (EHR domain
experts, Python-comfortable, per that prompt's own opening line) —
never committed a session report to git, only the prompt and the
commits it produced (`dd6ba2d`, `09eb094`, `f4e5c76`). Cited from that
provenance instead of the report itself: that prompt's own Step 4
("Legibility self-audit") stated the acceptance test any such report
would have had to pass. This session's own walk clears a stricter
version of that same test — real command execution against a real
corpus, not a citation exercise — and its result is recorded as the
new live claim in `notes/facts-register.md` F3, which names the
provenance gap explicitly rather than asserting the old report's
content by inference.

## Findings and their disposition

**The Evaluator row's Scope regression, found by the parity audit, fixed
in the same commit (step 3's own rule).** The audience-fork README
rewrite (ADR-0010, prior session) dropped root `README.md`'s own
inline `## Scope` section in favor of `docs/what-is-this.md`'s fuller
version — a real 0→1 click regression for the Evaluator audience,
whose canonical page is root `README.md` itself
(`docs/dev/positioning.md` segment 7). Not something ADR-0010 actually
required (that record dispositions files under `docs/`, not root
`README.md` content); fixed by restoring a compact version of the
section, same four bullets, with a pointer to the fuller doc — the
same pattern the Maturity table already uses for its own inline-
summary-plus-pointer shape.

**One softer finding named, not fixed, carried to the author as this
session's single before-takeover decision item (F-G1,
`notes/storefront-parity-audit.md`).** `docs/README.md`'s old form gave
the Guide-reader and AI-assistant rows their own explicit pointers
(`use-cases.md`, `ehrt help`/`cli.md`) inside each row's own section;
the current `docs/README.md` still reaches the same targets at the
same click-depth, but only via the neighboring Task-first-practitioner
section — a reader following their own row's prose isn't told to cross
over. Depth ties (no cell number worsened), so not a hard regression
by the audit's own counting rule, but a real discoverability softening.
Recommended fix (one line added to each of the two sections) is
deliberately left for a dedicated `docs/README.md` editorial pass
rather than a drive-by edit here — not urgent enough to block the
fast-forward push.

**F-G2 — the old consolidated experiments index has no live pointer,
disclosed as a RULED disposition, not reopened.** Old
`docs/README.md`'s "deep walk" section ended in a full EXP inventory;
that page (`components/tools/docs/experiments.md`) still exists but
`notes/docs-audit.md` explicitly dispositions it
**COMPONENT-ADJACENT-STAY** (ADR-0010) — a considered decision, not an
oversight. The individual evidence citations it used to back are
individually reachable, often at *shorter* click-depth now (the
Maturity table's own new evidence links, checkpoint 1).

## Facts-register entries this session created

F3 (`SETUP.md`'s on-ramp re-validation, R40/R41, this session's own
fresh-context walk and its four friction fixes).

## Gates

`clojure -M:poly check`: green (`OK`), run after checkpoint 1's edits
and again at session close. `make test` (the per-push lane — `poly
check` + `poly test :all skip:integration`): green, exit 0, zero
`FAIL`/`ERROR` anywhere in the run's own output, 12m36s wall clock —
every printed namespace summary reads `0 failures, 0 errors`
(`ehrt.sim.churn-test` 17/17, `ehrt.sim.churn-scenarios-test` 19/19,
`ehrt.sim.check-test` 60/60, `ehrt.sim-cli.core-test` 89/89, among
others). `make ci-parity` (fresh clone, cold artifact cache, the same
per-push lane, `ADR-0004`'s local-state-is-not-clone-state check):
green, exit 0, zero `FAIL`/`ERROR`, 2m10s wall clock (a cold-cache
fresh clone runs faster than the working tree's own `make test` above
— no artifact-fetch cache-miss penalty landed in this run; both are
real, independent green results, not one standing in for the other).
Run before this session's own closing commit, not skipped as
"docs-only, probably fine" — the full three-gate sequence
(`poly check`, per-push lane, `ci-parity`) is what actually leaves this
workspace's `main` before the takeover push touches it.

## HEAD this session's ceremony lands on

The commit this record's own checkpoint produces (`docs: pre-takeover
session record and archived prompt`), pushed immediately after, per
R30. This is the last commit before Part II's takeover push
(`notes/prompts/2026-07-29-ehr-testing-storefront-polish.md`, Part II)
— an author action, not this session's own.

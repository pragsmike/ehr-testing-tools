# 2026-09-08 -- nightly wiring: `integration.yml` runs `make integration`

The one-file session ADR-0120 asked for and did not take. Ceremony mode: R30
(commit and push at each checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-08-nightly-wiring.md`](../prompts/2026-09-08-nightly-wiring.md).
No ADR was written and none was owed: this is a CI wiring decision, and the
de-scaffold ruling of 2026-08-25 writes no ADR for a process or build decision.
No roadmap row moved -- `roadmap.md#dense-7500-gate-gaps` closed 2026-09-07 and
its claim "(a) the exerciser is a step of `make integration`" was and remains
true of the Makefile. What was false was that anything ran the Makefile.

## The ruling, carried into the tree

The prompt restated a channel ruling that had no tree record. Verbatim:

> **Nightly wiring (a)**, channel ruling, restated here because it is not yet
> tree-recorded: integration.yml's test step becomes `make integration`; the
> Makefile is the single definition of the integration tier; the per-push lane
> (test.yml) admits no exerciser.

> **R-move-not-improve:** one file. No exerciser, Makefile, or AGENTS.md edit;
> no header "improvements" beyond the truth-correction below.

> **R-sentinel:** read `gh run view <id>` yourself; no sleep waiters.

Rulings also in force: **R-full-suite-before-push**,
**R-amend-unpushed-message-only**, **R-figures-file** (its second sentence, on
walls as dated quotations), **R-session-verifies-ci-via-gh**.

## 0. Preflight

`bin/preflight` ran before any edit: **no findings, exit 0**. Last five CI runs
on `main` all green; edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`; `core.fileMode` true and `core.ignorecase` unset; working tree clean
including untracked; local HEAD `4cb76175` equal to `origin/main`; last
`stable-*` tag `stable-20260821-patient-simulator-charter`, HEAD not tagged
(DISCLOSED, and no tag was paid -- `R-tag-law` is retired).

The prompt's own precondition, checked first because it postdates the last
close marker: tip `4cb76175`'s per-push run is **34176996439, conclusion
success**, read with `gh run view`.

Session start and every baseline is `4cb76175`.

## 1. What was actually wrong

`.github/workflows/integration.yml` ran two steps -- `clojure -M:poly check`,
then `clojure -M:poly test :all project:integration`. The Makefile's
`integration` target (Makefile:74-86) runs that same suite **plus ten
scripts**, and every one of those scripts carries a header sentence of the form
"Integration-tier only (`make integration`), never per-push CI". Ten headers
naming a tier, and no job in this repo ran that tier. The suite half was
covered; the exerciser half had never once executed in CI since the first
exerciser landed with ADR-0120 on 2026-08-12.

ADR-0120 saw this and said so, in two places -- its integration-wiring
paragraph, and again in its own deviations list:

> **Scope note, disclosed:** `.github/workflows/integration.yml` invokes
> `clojure -M:poly test :all project:integration` directly, not `make
> integration` -- the nightly/dispatch-only CI job does not currently run the
> new exerciser. [...] wiring the GitHub workflow itself to call `make
> integration` (or to add its own `bin/demo-exerciser-ed-tuesday` step) is left
> for a future session to decide, not assumed here.

That is the note this session closes. It stayed open 27 days and nine more
exercisers.

## 2. The change

One file, `+24/-9`; `git diff --stat` showed exactly one path throughout, and
`.github/workflows/test.yml` was never opened for edit.

**The step.** `poly test :all project:integration` becomes `make integration`,
in a step named `make integration`. `poly check` still runs before it,
unchanged -- and that is not redundancy: the Makefile's `integration` target
does **not** run `check` (only its `test` target does), so dropping the
workflow's own `poly check` step in the name of "the Makefile is the single
definition" would have silently lost a gate. This is the one place where the
ruling's phrasing and the workflow's existing steps do not perfectly coincide,
and the prompt resolved it explicitly: keep `poly check` before it.

**The header.** Its first paragraph now says what the job runs -- the suite
under `out/test-tmp` (so `4cb76175`'s tmpdir discipline rides in for free, on a
runner as much as locally) and then the ten exercisers -- names ADR-0120's
scope note as closed, and keeps the per-push lane's exclusion stated. The ENF-1
stop-clause paragraph and the cold-cache paragraph are untouched; the diff
shows no line of either.

**The added sentence,** on why a job that now runs ten more scripts still
fetches nothing new: the three primed artifacts serve the suite only, the ten
exercisers are sim-only, and `gov.nist` coordinates already resolve from
`hit-nexus` for `project:integration`. That last clause was checked against the
tree rather than taken from the prompt -- `projects/integration/deps.edn:6`
carries `:mvn/repos {"nist-hit" {:url "https://hit-nexus.nist.gov/..."}}`, as
does the root `deps.edn`. The runner then proved it: the artifact-cache step
was a hit and no exerciser reached the network.

## 3. The local gate, and the order it forced

The exercisers assert a clean tree, so the gate cannot run against a
working-tree edit -- it has to run against the commit. The order was therefore
commit, gate, amend the message with the gate's own figures (message-only, on
an unpushed commit -- `R-amend-unpushed-message-only`), push. The amend rotated
the sha `a0b48ce4` -> `1de608a1`; the tree is identical across it, and the
figures below are what the amended message carries.

`make integration`, clean tree at this change, **exit 0**, whole target
**24m13s** -- a dated quotation (2026-09-08, penny), not a gate.
`R-figures-file`'s rule for walls applies here as much as to `figures.edn`: a
wall is a property of a machine and a load. The runner's own figure below is
quoted the same way.

- **Suite:** 157 `Test results:` blocks totalling **9,368 passes, 0 failures, 0
  errors**. Not one block off the `0 failures, 0 errors` form.
- **Ten exercisers, in the Makefile's order, each printing its own final
  summary line** -- the invariant the prompt named:

  ```
  == demo-exerciser-ed-tuesday: every command asserted, every named invariant held, tree clean ==
  == demo-exerciser-clinic-decade: every command asserted, every named invariant held, tree clean ==
  == demo-exerciser-dense-7500: every command asserted, every named derivation held, tree clean ==
  == usecase-judge-tier-calibration: every command asserted, tree clean ==
  == usecase-profile-tier-v2: every command asserted ==
  == usecase-acceptance-qa: every command asserted, tree clean ==
  == usecase-regression-baselining: every command asserted, tree clean ==
  == usecase-custom-emitter: OK (5 steps, 8 invariants) ==
  == usecase-ground-truth-oracle: OK (4 steps, 7 invariants) ==
  == readme-what-you-get: every command asserted, both pairs matched, tree clean ==
  ```

- **`git status --porcelain` empty afterwards.** For dense-7500 that empty
  output IS the assertion, not a tidiness check: the script rewrites
  `figures.edn`'s `:asserted` block from its own run, so a clean tree says the
  rewrite reproduced the committed block. It did -- 167,197 events and 222,819
  messages at 7,500 arrivals, 33,306 at 750, the figures gated 2026-09-07.
  dense-7500's own reported wall was 138s of the 24m13s.

## 4. The runner proof

Pushed `4cb76175..1de608a1`. `bin/post-push-verify 4cb76175 1de608a1`: remote
tip equals HEAD, every commit message in the range pure ASCII, CI run reported
once (and not awaited there -- AR-CI-4).

**Per-push run 34201046659, conclusion success**, 14m54s, headSha `1de608a1`.
This proves only that the per-push lane is undisturbed; it does not run this
workflow at all.

**Dispatch run 34202347771, conclusion success**, job `integration` **24m49s**,
headSha `1de608a1`, event `workflow_dispatch` -- a dated quotation
(2026-09-08), not a gate. Warm artifact cache, which the prompt licensed for
this proof: the change under test is the wiring, and no exerciser fetches an
artifact. Read with `gh run view` and watched with `gh run watch
--exit-status`; no sleep waiter (R-sentinel).

What the runner log shows, checked rather than assumed:

- The job's step list is `... / Prime artifact cache / poly check / make
  integration`. The step column of `gh run view --log` carries
  `make integration` on every one of the 15,750 log lines it produced.
- **All ten exerciser summary lines, identical in wording to the local run**
  (the block quoted in §3, line for line). No exerciser failed, so the prompt's
  STOP-and-report clause never fired.
- Suite: 157 blocks, **9,362 passes, 0 failures, 0 errors**.
- dense-7500 on the runner: wallclock **106s** -- *faster* than penny's 138s --
  and its `figures.edn` rewrite produced the same numbers, with the tree-clean
  postcondition holding there too. That is a stronger statement than the local
  run alone could make: the committed `:asserted` block now reproduces
  byte-for-byte on a second, differently-specified machine, so those counts are
  a property of the corpus rather than of penny.

## Findings

**One, and it is pre-existing.** The runner's suite total is **6 assertions
below** the local one -- 9,362 against 9,368 -- across an identical set of 157
namespaces. The whole difference is in a single namespace:

| | local (penny) | GitHub runner |
|---|---|---|
| `ehrt.corpus.operators-doc-test` | 133 | 127 |
| all other 156 namespaces | identical | identical |

It is **not this session's**, and not specific to `make integration`. The same
127 appears in the per-push run at the same sha (34201046659, a lane this
change does not touch) and in the nightly *before* this change existed
(34125799146, 2026-09-07). Both lanes, both before and after.

Both runs are green, and they are green for the same reason the counts differ:
every assertion in that namespace is "this registered operator appears in the
rendered `docs/operators.md`", one `is` per entry, so the count is a function
of how many entries are registered when the test runs -- and
`ehrt.corpus.operators/entries` is `(vals @registry)` over a global atom filled
by load-time `register!` side effects. Six assertions is exactly two entries'
worth (each contributes one name check plus a `:doc` and a contract check).
**That mechanism is the likely explanation, not a proven one** -- a probe to
load the namespace in isolation and count the registry failed on the `:test`
alias's classpath, and chasing it further is outside this session's one-file
fence, so it was left. Reported as the class: *a doc test whose assertion count
is read off a mutable global registry is load-order dependent, and its count is
therefore not a figure to reconcile between machines.*

## Fences

- **The exercisers themselves.** Out of fence by the prompt: a runner-side
  failure in one would be a class to report, not a script to patch here. None
  failed, so nothing was owed.
- **The Makefile, AGENTS.md, and every exerciser header.** R-move-not-improve
  held: one file in the diff, start to finish. The ten header sentences that
  claimed "integration-tier only (`make integration`)" were true-in-intent and
  are now true-in-fact; not one of them was edited to say so.
- **`test.yml`.** Untouched. Its own direct `clojure -M:poly test :all
  skip:integration` (test.yml:166) has the same shape as the invocation this
  session replaced in the nightly job, and a reader will ask whether that is a
  second scope note. It is not: `make test`'s other two payload steps, `poly
  check` and `bin/verify-nist-lock`, run in that lane as their own steps
  (test.yml:163, :169), so no step of that target goes unreached. One flag does
  not ride along -- `make test` runs the suite under
  `-J-Djava.io.tmpdir=out/test-tmp` and test.yml does not -- which is what
  `4cb76175` was about locally and is moot on an ephemeral runner discarded
  with its `/tmp`. Left alone: the ruling says the per-push lane admits no
  exerciser, and one file changes.
- **The node20 / setup-java@v4 deprecation annotations** on every run in this
  repo, this session's included. Pre-existing, unrelated, not this session's.

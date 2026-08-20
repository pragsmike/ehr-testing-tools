# Archived prompt: oracle-coverage-integration-half (2026-08-20)

Session prompt -- the oracle-coverage gate's integration half runs
green for the first time -- ADR-0160

## Context

Claude Code under R30 in ehr-testing-tools. HEAD at handoff: 92d23bc
(ADR-0159 second addendum; tree clean; CI green at tip; last tag
`stable-20260820-review-4-arc-close` @0e72ed4, no tag owed). Roadmap row
`roadmap.md#oracle-coverage-gate-integration-half` (OPEN PRIORITY 1) --
quote it; short form: `projects/integration/test/ehrt/integration/
oracle_coverage_test.clj:94` `committed` searches `"(def <name>"` while
`digest.clj` writes `(def ^:private <name>`, so `str/index-of` returns
nil and `subs` NPEs before any assertion. ADR-0156 refined the
docs-tooling half's extractor to match both forms
(`components/docs-tooling/test/.../oracle_coverage_test.clj:71-77`, a
`some` over the two prefixes) and did not touch this one. The gate has
NEVER run green; nightly `Integration` run 32344505291 is its first
execution and is the standing RED WITNESS -- no new red test is owed;
cite the run id as the red.

## Author ruling, verbatim

* "(a)." (2026-08-20) -- the minimal session, tonight: fix the
  extraction, trigger the `Integration` workflow, watch it to
  conclusion. Fold in nothing else.
* Tag: no tag owed at Step 0. Close tag: pay in-session if the PUSH
  workflow at the tip concludes success while open, else next Step 0.
  The INTEGRATION run's conclusion is the session's deliverable and is
  reported regardless.

## Step 0

Fresh clone, tip 92d23bc; `bin/preflight` -- it will report the red
nightly: that is the known F-5 finding, DISCLOSED not a stop; baseline
`make test` unpiped, MAKE_EXIT captured, wrapper ends
`exit "$MAKE_EXIT"`, reconcile vs ADR-0159's 364 blocks / 4,070 tests /
18,304 assertions; `poly check`. Reproduce the NPE locally: run the one
test namespace via the integration project's own invocation (whatever
`integration.yml` runs -- read it) and capture the NPE.

## Step 1 -- the fix

Preferred shape: ONE extractor, shared. If the two-form helper can be
shared without any project/composition change (e.g. it moves to a
docs-tooling `src` ns both tests require, and `poly check` stays clean),
do that and delete both private copies. If sharing requires composition
work, inline the two-form `some` pattern into the integration half with
a comment naming the docs-tooling sibling as the canonical twin, and add
one roadmap line under `#register-gate-row-ownership`-style hygiene for
the dedup -- say which branch you took and why. Run the namespace
locally: green, and its assertions actually executed (the 35-root count,
the witnessed-set equality -- paste the counts). Full `make test`; push.
Commit: "fix: integration half of the oracle-coverage gate matches
^:private defs; first green run (ADR-0160, review-4 F-5)"

## Step 2 -- the deliverable

`gh workflow run Integration --ref main` (or the dispatch invocation
`integration.yml` supports -- read it); watch run to CONCLUSION via
`gh run watch`/`view`; the run id and `success` in the session record
and ADR. If it concludes anything but success, STOP-AND-REPORT with the
log excerpt -- do not iterate fixes into the night; the author decides.

## Close (self-archive FIRST)

Archive to `.agents/prompts/2026-08-20-oracle-coverage-integration-
half.md`; session record; ADR-0160 (the NPE capture; the branch taken;
the local counts; BOTH run ids -- the historical red 32344505291 and the
first green); roadmap row -> CLOSED citing the green run id; full
`make test` reconciled vs Step 0; `bin/post-push-verify`; tag per
ruling.

## Fences

Files: the integration test ns, optionally ONE docs-tooling ns (the
shared extractor) + the docs-tooling test that loses its private copy,
roadmap, ADR, prompt archive, session record, state-derived
(regenerated); NO digest.clj, NO bin/, NO engine; the io-vocabulary
lint's scope is `components/*/src` -- the integration test's bare
`.listFiles`/`.mkdirs` at :100-106 are OUT of scope and OUT of fence:
leave them, note them in the ADR if you judge they deserve a row; oracle
unrun locally (the integration gate IS the digest run -- do not double
it); no test deletions beyond the private-copy removal on the shared
branch; exit codes unpiped; ASCII messages; R-RP. READ-BACK: files
touched vs this list; the two run ids with conclusions; the assertion
counts from the green local run.

# CI hotfix — integration tests to their own path; coverage made hermetic

You are working in `ehr-testing-tools` (public; CI is currently RED on
main). Root cause, already diagnosed: `make coverage` (cloverage) runs
the whole suite with its own runner, ignoring the `^:integration`
exclusion the default runner applies — so Actions executed the
contract-pairing fixture, which requires real cached artifacts CI has
never fetched. Fix structurally; verify the exact CI scenario locally;
confirm Actions green before finishing.

Small focused session. Test-first where new code appears (little
should). Ritual: commit → `git push origin`. Save this prompt to
`.agents/prompts/2026-07-25-ci-hotfix-integration-path.md`; final
commit archives it.

## Step 1 — Structural split

1. Create `test-integration/` as a separate source path. Move every
   test that requires real artifacts, network, or a warm cache into it
   — known: the contract-pairing suite; search for others (grep tests
   for real `artifact/fetch`, real `generate!` invocations without
   injected fakes, real validator subprocess calls; the P5 report
   mentions one integration-tagged baseline-gating candidate if it
   landed). Namespace names may stay as they are; only the path moves.
   Keep the `^:integration` metadata on them as documentation.
2. `deps.edn`: `:test` alias paths remain `["test"]` (plus src); add an
   `:integration` alias whose extra-paths include `"test-integration"`
   and whose runner includes everything on that path. `:coverage`
   alias unchanged — it now excludes integration by construction (its
   test path doesn't contain them).
3. Makefile: `make integration` target (runs the integration alias;
   document that it requires `ehr artifact fetch` first — say the two
   artifact names); `make test` and `make coverage` unchanged in
   invocation. `help` updated.
4. Simplify or keep the old excludes config in `:test` as harmless
   belt-and-braces — your call; report the choice.

## Step 2 — Silence the SLF4J noise

Add `org.slf4j/slf4j-nop` (exact-pinned, current stable) to the
`:test` and `:coverage` aliases only (not the base deps — library
consumers choose their own binding). Verify the warnings disappear
from test output.

## Step 3 — The verification the publication wave should have run

Reproduce CI's exact scenario locally and prove it green before
pushing:

1. Fresh temp clone of the repo.
2. Point the artifact cache at an EMPTY temp dir
   (`EHR_TESTING_TOOLS_CACHE=$(mktemp -d)`).
3. Network-isolated where feasible (`unshare -rn`, as previously used)
   after dependency priming.
4. Run `make test` AND `make coverage` — both must pass with the cold
   cache and no network. This pair is the new hermeticity definition.
5. Then, separately, warm the cache (`ehr artifact fetch` both
   artifacts) and run `make integration` — must pass (6/6 contract
   pairing plus anything else moved).

## Step 4 — Policy and plan

- `AGENTS.md`: hermeticity policy updated — `make test` and `make
  coverage` must be cold-cache/no-network green; integration tests
  live in `test-integration/` (path is the mechanism, the
  `^:integration` tag is documentation); `make integration` requires
  fetched artifacts.
- Plan file, enforcement wave: add "nightly/optional CI job running
  `make integration` with artifact-cache priming" as a listed item (do
  NOT add it to CI now).
- `docs/gate-calibration.md` or EXP-C5 results: no changes — this is
  infrastructure, not findings.

## Step 5 — Land and confirm green

1. Commit(s): `Split integration tests to test-integration/ (coverage
   hermetic)`, `Silence SLF4J in test aliases` (or combined — sensible
   splits).
2. Archive this prompt; final commit; `git push origin`.
3. Watch the Actions run to completion (`gh run watch` or poll `gh run
   list --limit 1`) and report the conclusion. Do not end the session
   reporting "pushed, should be fine" — the session ends when the
   badge's run is observed green or you report the failure with
   diagnosis.

## Report

Which tests moved (list); the Step 3 evidence (cold-cache test+coverage
results, warmed integration results); SLF4J before/after; the Actions
run id and conclusion; commits.

## Out of scope

Everything in the P6 prompt (queued next); no CI workflow changes
beyond none-needed (the workflow already calls make test/coverage —
verify and leave); no capability code; no test deletions or weakenings
— moving, not skipping.

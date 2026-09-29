# describe slice 2: the manifest records what each arrival was assigned, and the config it ran (ADR-0183)

2026-09-29. Base `f4251226`. Ceremony: R30, taken from the session
prompt, which stated no prepare-only mode. Author ruling "next": slice
2 lands now.

## For downstream

**Regenerate with the envelope, or keep manifest.edn beside
events.edn, and describe answers configured-versus-realized per
pathway; your retained bare-vector corpus is unchanged.**

- `ehrt sim describe --manifest manifest.edn < events.edn` describes a
  bare log exactly as its envelope would be, identity source
  `:bare-log+manifest`. `corpus generate sim` already writes both
  files; its `manifest.edn` is sim's manifest verbatim.
- **`:describe-version "1.1.0"`** (minor: a new identity source and
  new row keys). A pathway row with the record reads `:configured :yes
  :observed <ordinals assigned> :subjects <assigned patients with an
  event past :registered>` plus witnesses; without it, `:unprovable`
  as before. Module rows gain `:assigned` and `:assigned-subjects`.
- The manifest's `:config` is real: `{:path <as given> :sha256 <of the
  file's bytes> :hashed :file}` with `--config`; `{:path "(inline)"
  :sha256 <of (pr-str engine-params)> :hashed :engine-params}`
  without. `:schema-version` stays `"1.1"`.
- The manifest gains `:assignments`, one entry per arrival ordinal:
  `{:ordinal :patient-id :pathway :module :person-id :repeat?
  :placeholder?}`. Shape: `docs/formats.md`, "A simulator run's
  manifest".

## Commits

| sha | what |
|---|---|
| `2f4e16ee` | engine: `run/run` hands out `:assignments`, draw-free (+ red tests, engine and persons) |
| `2807621d` | manifest `:config`/`:assignments`, describe `--manifest` and record-observed rows, 1.1.0 golden, CLI flag, `docs/cli.md` (+ red tests) |
| `043dee0e` | docs: formats.md, consuming-ground-truth.md; ADR-0183 "Slice 2, landed" |
| this commit | record + prompt archive (+ generated indexes) |

## Where the tree differed from the prompt

1. **Steps 3 and 4 are one commit.** Both edit
   `components/sim/src/ehrt/sim/run.clj` (`run-command`'s manifest and
   `describe-command`'s `--manifest`), and this ceremony stages whole
   files. Their gates ran together, before that commit.
2. **The red tests landed with their implementations**, not as a
   separate RED commit, so no tip on `main` is red. Red was witnessed
   locally at `f4251226` + tests: 25 failures and 1 error, every one in
   a new test and each for the stated reason (no `:assignments`, zero
   hash, no `:hashed`, no `--manifest`). The error was `(apply < ())`
   over an empty witness list, the same cause.
3. **`merge-config-file` keeps its return shape.** "Return the path
   beside the merged opts" is done by a private `read-config-file`
   that returns both; `merge-config-file` projects it. Its four other
   callers and its tests (`(= (result/ok opts) ...)`) are untouched,
   and the two rejections are literally the same code.
4. **`:assignments` rides `final-result`'s base map**, not a caller's
   `extra`: one place, and no exit path can forget it.
5. **`:repeat?` is `prelude`'s owner rule.** A placeholder arrival owns
   itself, so it is not a repeat even for a person bound earlier.
6. **Module rows keep `:configured-detail`** (it is configuration) and
   gain the record's `:assigned`/`:assigned-subjects` beside it,
   rather than replacing it.
7. **A stale doc claim, fixed in passing.** consuming-ground-truth.md's
   manifest table said `:config` was "the `--config` file by content
   hash". Before this slice every sim manifest carried the zero hash.

## Gates

- Step 1: red as above (`nsrun` over the four touched test namespaces).
- Step 2: `bin/ground-truth-bracket f4251226 HEAD` (HEAD `2f4e16ee`):
  "IDENTICAL: every digested root's :ground-truth matches between
  f4251226 and HEAD (39 roots)", exit 0; 3 interpreter-layer roots
  skipped by name, as always. `brick:sim-engine :all skip:integration`:
  52 `Test results` lines, conformance and ehrt-cli, 0 failures.
- Steps 3-4, before `2807621d`: `brick:X :all skip:integration`, one
  brick per invocation, each in conformance and ehrt-cli: sim 38
  `Test results` lines, sim-check 24, provenance 18, corpus 60, cli 24
  (`cli-md-is-current-test` included), every one 0 failures; `poly
  check` OK. **WSL restarted mid-run** (uptime 0 min) during
  provenance, killing the gate wrapper with no failure recorded; sim
  and sim-check had finished, and provenance, corpus, cli and `poly
  check` were re-run from scratch. The figures above are the re-run's.
- Step 5: `make docsgen` exit 0; the only generated moves are the two
  INDEX.md files and `state-derived.md`'s record/prompt counts (266 /
  258), which ride this commit. Reading-set rows unchanged: neither
  edited doc is in a budgeted set.
- Step 6: `bin/regression-oracle f4251226 HEAD` (HEAD `043dee0e`):
  "IDENTICAL: every root's digest matches between f4251226 and HEAD",
  exit 0, 42 roots, declared-digest-change no.
- Step 7: full `make test` at `043dee0e` plus this record's files,
  unpiped to a log, `MAKE_EXIT=0`, 432 `Test results` lines, none with
  a failure or error; the wrapper ends `exit "$MAKE_EXIT"`.
- CI on the pushed tip is the close marker; a record cannot witness
  its own tip, so the result is in the session's closing report.

## The golden's diff

`components/sim-check/test/ehrt/sim_check/fixtures/describe-golden-immunization.edn`,
a bare log with no manifest, so step 4's invariant says only the
version may move:

```diff
- :describe-version "1.0.0",
+ :describe-version "1.1.0",
```

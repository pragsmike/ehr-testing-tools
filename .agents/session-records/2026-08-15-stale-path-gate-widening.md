# 2026-08-15 -- widen the stale-path gate to every tracked doc surface and fix 25 dead links (review-3 D1-2/D1-8)

Repo review 3 fix session B, second under the review-3 arc's `"accept
all."` ruling (2026-08-15). Landed as ADR-0137. Baseline tip `15f5943`
(fix session A's own close), ceremony mode R30 standing default, one
commit at the end per the prompt's own "nothing is committed yet"
fence.

## Step 0 -- preflight

`bin/preflight` plain, every line disclosed:

- Last five CI runs on `main`: **all green** (`15f5943`, `043305b`,
  `fca52ec`, `bc6f46c`, `dbbeb1f`).
- Edit root `/home/mg/src/ehr-testing-tools`, **not** under `/mnt/`.
- Working tree clean, untracked included.
- Local HEAD `15f594384a39e343c57c7ea2ff8c4c8501c04fac` matches
  `origin/main`.
- Last `stable-*` tag `stable-20260815-result-nodes`; **DISCLOSED: HEAD
  not tagged.** No tag owed this session -- the review arc tags at its
  step-7 close.

`git rev-parse stable-20260815-result-nodes^{}` =
`b139de589083c6b4967c1a4769b2c6a8d17feac4` -- matches the prompt's
stated value exactly. Baseline tip is `15f5943` itself, not a
descendant.

## Step 1 -- widen the gate, witness red

Widened `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj`
with a **fourth** scan: dead markdown-link resolution over every `*.md`
under `docs/**` and `components/<x>/docs/**` (88 files), the component
doc roots enumerated from the filesystem by `component-doc-roots`,
never a hand-list. The `"Deliberately scoped"` sentence -- itself the
finding -- was retired and replaced with a **per-scan population
statement** naming all four scans, each population, and how each is
enumerated.

All four D1-8 exclusion classes encoded, each with its own passing
test: (a) shorthand backticked citations excluded structurally (link
destinations only), (b) generator template source excluded
structurally (`*.md` only; its rendered output IS in the population and
resolves), (c) `polylith-brief.md`'s external tutorial examples
excluded by name with a test that the same shape still trips elsewhere,
(d) percent-encoding decoded rather than excluded.

### RED witnessed at exactly 25 -- the fix list and the fence

Verbatim from the failing run. All 25 under `components/<x>/docs/`;
none in `docs/`; none in vendored bytes.

```
dead markdown links (25):
  components/corpus/docs/experiments.md:21  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:21  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:21  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:21  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:24  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:26  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:26  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:26  -> ../notes/facts-register.md
  components/corpus/docs/experiments.md:26  -> ../notes/facts-register.md
  components/corpus/docs/experiments/EXP-C5-results.md:18  -> ../../notes/facts-register.md
  components/corpus/docs/experiments/EXP-C5-results.md:51  -> ../../.agents/memory/patterns.md
  components/corpus/docs/experiments/EXP-C5-results.md:167  -> ../../.agents/memory/patterns.md
  components/corpus/docs/experiments/EXP-C5.md:22  -> ../../notes/facts-register.md
  components/corpus/docs/experiments/EXP-C5.md:24  -> ../../.agents/memory/patterns.md
  components/corpus/docs/experiments/EXP-C5.md:65  -> ../../.agents/memory/patterns.md
  components/corpus/docs/experiments/EXP-D3.md:140  -> ../../../../.agents/memory/patterns.md
  components/corpus/docs/experiments/EXP-SBOM.md:14  -> ../../notes/facts-register.md
  components/corpus/docs/palgebra-design.md:5  -> ../.agents/plans/archive/judge-gate-refactor.md
  components/sim/docs/operational-models.md:6  -> ../.agents/plans/roadmap.md
  components/sim/docs/third-party-sources.md:11  -> ../notes/facts-register.md
  components/sim/docs/third-party-sources.md:21  -> ../notes/facts-register.md
  components/sim/docs/third-party-sources.md:34  -> ../notes/facts-register.md
  components/sim/docs/third-party-sources.md:34  -> ../notes/facts-register.md
  components/sim/docs/third-party-sources.md:38  -> ../notes/facts-register.md
  components/sim/docs/third-party-sources.md:44  -> ../notes/facts-register.md
```

Split: **19** un-re-depthed `../` prefixes + **6** whose target the
register recorded as gone. Matches D1-2's own arithmetic exactly.

## Step 2 -- the 19 mechanical fixes

`components/sim/docs/third-party-sources.md` first, per the prompt's
priority. Re-depths verified per file, not assumed: depth-3 files
(`components/<x>/docs/`) need `../../../`, depth-4 files
(`components/corpus/docs/experiments/`) need `../../../../`.

Every rewritten target was then **resolved individually** and confirmed
to exist -- 23 `../`-form links checked in the changed files, 19 OK and
the 4 still-dead ones exactly the pre-ruling gone-targets. Not a blind
`sed`-and-hope.

Gate re-run: **exactly 6 remaining**, all gone-targets. The prompt's
midpoint held.

## Step 3 -- STOP-AND-REPORT, and what the stop found

Reported three items and waited. Nothing was committed.

### The 6 were not a second class (ruling R-B1)

The register and the prompt both assumed the 6 targets were gone,
needing per-sentence rewrite-or-delete judgment. **Both targets exist,
frozen.** These docs came from the pre-merge `tools` repo, where
`.agents/memory/patterns.md` and
`.agents/plans/archive/judge-gate-refactor.md` were live sibling paths;
the merge froze that tree into `notes/tools/agents/`. So the 6 are the
**same** relocation defect as the 19, differing only in that the
destination moved too.

Evidence gathered before the ruling was sought:

- `.agents/memory/` has only ever contained `README.md`; there is no
  delete commit for `patterns.md`, because it was never created at the
  post-merge path. `.agents/memory/README.md` states this itself
  ("tools' own pre-merge memory files stay frozen at
  `notes/tools/agents/memory/`").
- `notes/tools/agents/memory/patterns.md` line 259 carries numbered
  pattern **15, "Provenance is measured at the point of execution"** --
  verbatim what all five citing sentences invoke (`unshare -r`
  remapping uid; "the JVM that actually runs the").
- `notes/tools/agents/plans/archive/judge-gate-refactor.md` opens by
  describing itself as the spent, repo-inventoried execution plan
  retained as record -- a verbatim match for `palgebra-design.md`'s
  Companion sentence.

**Ruling R-B1, verbatim: "Re-point all six (Recommended)"** -- including
`palgebra-design.md`'s visible backticked label, so no rendered text
names a path that does not exist.

### Two premise mismatches, registered rather than improvised

**R-B2, verbatim: "Ship link-half, register the rest (Recommended)".**
Step 1.2's backticked root-anchored path half rests on the premise that
class (a) excludes the shorthand convention structurally. It does not.
Measured against the live tree:

| reading | checked | dead |
|---|---|---|
| first segment is a repo-root entry | 683 | **216** |
| ... plus component-root resolution | 683 | **95** (many inside `docs/`, which D1-2 measured clean) |
| leading slash (`/docs/x.md`) | 8 | **8**, all OS absolute paths (`/tmp/exp-d3/`, `/root/.fhir/packages`) -- a vacuous check |

The residue is dominated by a class D1-8 never named: post-relocation
**basename shorthand** (`docs/notation.md` cited from anywhere, meaning
that doc wherever it now lives). Also present: backticked command lines
(`bin/ehrt play … --board 60`, 60), globs (`components/*/docs/*.md`,
44), `file.clj:21-23` line suffixes. Real findings sit inside the
residue -- `components/tools` (retired component) twice in
`docs/dev/architecture.md`, `.agents/plans/corpus-foundations.md` five
times in `source-sink-design.md`. Not built; registered.

**R-B3, verbatim: "Register it, keep docs/ scope (Recommended)".** The
retired sentence's actual subject was scan 1 (the retired-name denylist
family), not the link scan. Widening scan 1 to `components/<x>/docs/`
turns **15** more files red: `ehr_testing_tools` (1), `ehrt.tools.` (1),
`(?<!corpus/)docs/experiments/` (14 -- mostly the same
basename-shorthand false positive, since inside
`components/corpus/docs/` the sibling form is correct). Incompatible
with the exactly-25 red witness and outside the fence. Scan 1 keeps its
`docs/` population, stated as fact in the new population statement.

## Step 4 -- green, co-land, close

Six re-points applied; gate green. One commit carrying the widened gate
and all 25 fixes.

### Deviations and findings disclosed

- **A hung test run, unrelated to this session's change.** Verifying
  the green gate via `clojure -M:poly test :project:docs-tooling`
  wedged for 20 minutes at 116% CPU. `jstack` located it precisely:
  `ehrt.integration.contract-pairing-test` blocked in
  `ProcessImpl.waitFor` on a corpus-generate subprocess
  (`ehrt.kernel.invocation/run!`). The two earlier runs of the same
  command had returned in about a minute because the stale-path gate
  was still red and the run stopped before reaching it. The invocation
  was the error: `make test` runs `clojure -M:poly test :all
  skip:integration`, and `:project:docs-tooling` pulled the integration
  project in. Diagnosed rather than assumed, killed, and superseded by
  the real full run below. Not a defect introduced here, and not
  evidence about `make test`.

### Full suite

`make test`, unpiped, exit code captured explicitly:

- **`MAKE_EXIT=0`**
- **636** occurrences of `0 failures, 0 errors` -- **exactly the
  expected figure**, no block delta. This session widened an existing
  test namespace and added no new one, so the count was predicted to
  hold, and it did.
- 318 `Test results:` blocks, every one zero-failure.
- **16,369** passes, up 54 from the review-3 baseline's 16,315 --
  assertions rose (four new deftests in `stale-path-test`), blocks did
  not, exactly as the prompt anticipated.
- Zero `FAIL in` / `ERROR in` lines.
- `bin/verify-nist-lock`: OK, 6 hit-nexus-sourced coordinates match
  `artifacts.lock.edn` exactly.

## Fences honoured

- Touched only: `stale_path_test.clj`, the 8 doc files named by the
  red run's 25-hit list, the register's two disposition cells, and the
  close artifacts.
- Zero `src/` outside the one test namespace; zero converter or
  generator changes; vendored bytes verbatim.
- No `stable-*` tag created -- none owed; the arc tags at its step-7
  close.
- No AUTHOR ACTION items taken.

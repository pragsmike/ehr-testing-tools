## ADR-0052 — Alignment fixes 3: sim-model's resources take their own name, and the nesting rule gets its gate

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: alignment fixes 2 landed and was design-channel-verified
(`2599355`, `notes/adr/0051-alignment-fixes-2.md`) — the tag mechanic
is now coherent across every surface that states it, and three
prose-only invariants gained gates. This session executes the ruled S1
fix (register rows S1/C-1, `.agents/plans/2026-08-05-alignment-audit-
findings.md`): `components/sim-model/resources/sim/` renames to
`resources/sim-model/`, matching the nesting convention six of the
seven `components/*/resources` directories already followed by
construction, with a new gate co-landed so the rule is enforced rather
than merely observed. First src-touching session of the arc — the
oracle bracket is the whole point of the ceremony here. R30 ceremony.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's own
prompt):

**AR-F3-0 (tag).** Per the reconciled mechanic (`AGENTS.md` + AR-AU-0),
create annotated tag `stable-20260805-alignment-fixes-2` at `2599355`,
message `alignment fixes 2 landed, design-channel-verified 2026-08-05
(ADR-0051)`; push; verify on origin.

**AR-F3-1 (the rename).** `git mv components/sim-model/resources/sim
components/sim-model/resources/sim-model` (history-preserving), plus
exactly the three path-string edits in `persona.clj`
(`"sim/demographics/..."` → `"sim-model/demographics/..."`). Resource
CONTENT untouched — byte-identical files at new paths, verified with
`git diff --stat -M` showing pure renames for the four moved files.
ADR-0025's tolerance closes with this session; this ADR cites it as the
origin and the register rows as the trigger.

**AR-F3-2 (the gate, co-landed).** New deftest in the `docs-tooling`
gate family: every `components/*/resources` directory that exists
contains exactly one top-level entry, a directory named after its own
brick. No allowlist — post-rename, all seven conform. Docstring cites
AR-F3-2, register rows S1/C-1, and ADR-0025's closed tolerance.
Red→green natural: run the gate against the pre-rename tree first,
capture the genuine red (sim-model trips), then land rename + gate in
the SAME commit, green.

**AR-F3-3 (citation sweep, same commit).** Fresh grep for
`resources/sim/demographics` and `sim/demographics` as text across
docs (`AGENTS.md`, `AUTHORS-GUIDE.md`, `components/*/docs/`, component
READMEs, `.agents/` live surfaces): any current-tense citation of the
old path updates; explicitly historical mentions keep, per AR-F1-2's
judgment rule.

**AR-F3-4 (determinism bracket is the verdict).** Oracle bracket
(`2599355` → tip) must show all eleven batches identical — the
demographics EDN content is unchanged, so persona generation and every
downstream byte must be too. Any digest change means the rename touched
something this analysis missed: STOP-AND-ESCALATE with the diff,
revert nothing, land nothing further. No `--declared-digest-change`
licensed.

### Step 0 — preflight + tag

Working directory confirmed `~/src/ehr-testing-tools` (ext4, `df -T`
reports `ext4`); tip `2599355` exactly. Probe re-verified fresh against
the live tree, not trusted from the driving prompt: exactly three
`io/resource` call sites resolve `"sim/demographics/..."` paths, all in
`components/sim-model/src/ehrt/sim_model/persona.clj` (lines 57/62/67);
the moving set is exactly four files (`NOTICE`, `given-names.edn`,
`places.edn`, `surnames.edn`); the seven-dir census
(`components/{judge,kernel,provenance,sim-engine,sim-model,sim-
trajectory,sim}/resources`, confirmed as the complete set of
`components/*/resources` directories workspace-wide, zero under
`bases/*`) found sim-model the sole nonconformer (`resources/sim/`
instead of `resources/sim-model/`) — matching the prompt's own probe
exactly, in every particular. No eighth nonconforming directory
surfaced.

Baseline: full suite green (`clojure -M:poly test :all skip:integration`,
212 `Test results:` lines, 0 `FAIL`/`ERROR`/`Exception` anywhere).
`clojure -M:poly check`: OK. `gitleaks detect -v`: no leaks found, 657
commits scanned. Neither `stable-20260805-alignment-fixes-2` nor any
alignment-fixes-3 tag existed yet, locally or on origin.

**Tag act, verified on origin:**

```
$ git tag -a stable-20260805-alignment-fixes-2 2599355 \
    -m "alignment fixes 2 landed, design-channel-verified 2026-08-05 (ADR-0051)"
$ git push origin stable-20260805-alignment-fixes-2
 * [new tag]         stable-20260805-alignment-fixes-2 -> stable-20260805-alignment-fixes-2

$ git ls-remote --tags origin | grep alignment-fixes-2
4f84699966902a5f14179d0b50936091abe32e1d	refs/tags/stable-20260805-alignment-fixes-2
2599355240cdce1f547d443404eebbd99e627e44	refs/tags/stable-20260805-alignment-fixes-2^{}
```

The peeled ref resolves to `2599355` exactly; `git tag -l -n99` confirms
the message matches the ruling verbatim.

### Step 1 — red, then rename + gate + sweep (AR-F3-1/2/3)

`components/docs-tooling/test/ehrt/docs_tooling/resource_nesting_test.clj`
written first, against the still-unrenamed tree — genuine red, not
staged:

```
FAIL in (every-component-resources-dir-nests-under-its-own-brick-name-test) (resource_nesting_test.clj:51)
components/sim-model/resources top-level entries ("sim") -- expected exactly one directory named "sim-model"
expected: (conforms? brick-name res-dir)
  actual: (not (conforms? "sim-model" #object[java.io.File ...] "components/sim-model/resources"]))

Ran 2 tests containing 11 assertions.
1 failures, 0 errors.
```

(The file's own mechanism-sanity `deftest`, run in the same pass,
passed throughout — the failure is the real-tree gate alone, confirming
the extraction/predicate logic itself was never in question.)

Rename executed: `git mv components/sim-model/resources/sim
components/sim-model/resources/sim-model`. Three path-string edits in
`persona.clj` (`given-names-by-sex-and-decade`, `surnames`, `places`
defs, lines 57/62/67): `"sim/demographics/..."` →
`"sim-model/demographics/..."`. `git diff --cached --stat -M`:

```
 .../sim-model/resources/{sim => sim-model}/demographics/NOTICE      | 0
 .../resources/{sim => sim-model}/demographics/given-names.edn       | 0
 .../sim-model/resources/{sim => sim-model}/demographics/places.edn  | 0
 .../resources/{sim => sim-model}/demographics/surnames.edn          | 0
 components/sim-model/src/ehrt/sim_model/persona.clj                 | 6 +++---
 5 files changed, 3 insertions(+), 3 deletions(-)
```

All four resource files show as pure renames (0 lines changed, byte-
identical); `persona.clj` shows exactly the three path-string line
pairs, nothing else.

**Citation sweep (AR-F3-3).** Fresh grep for `resources/sim/demographics`
and `sim/demographics` as text, across `AGENTS.md`, `AUTHORS-GUIDE.md`,
`components/*/docs/`, component READMEs, and `.agents/` (recursive):
two hits, both explicitly historical, both kept unchanged per AR-F1-2's
judgment rule —

| Hit | Disposition |
| --- | --- |
| `notes/adr/0025-sim-split-s1-s2.md` (2 lines, describing the S1 sim-split session's own action, 2026-08-02) | Historical — describes what that session did, correctly, at the time. Also fenced from editing this session (frozen attic file; its closure is cited here, not written into it). |
| `.agents/plans/2026-08-05-alignment-audit-findings.md` row F-2 (describes NOTICE files read at audit time, 2026-08-05) | Historical — a dated audit finding recording what was observed during that probe, not a live claim about the current path. |

No current-tense citation of the old path existed anywhere in scope;
`components/sim-model` itself carries no README/docs mentioning the old
path either (checked directly, zero hits). Sweep disposition: zero
edits, both hits correctly historical.

Gate green post-rename: `Ran 2 tests containing 11 assertions. 0
failures, 0 errors.` `clojure -M:poly check`: OK. Full suite: 214
`Test results:` lines (+2 vs. Step 0's 212, matching one new namespace
appearing once per project × 2 projects that include `docs-tooling`,
the same pattern ADR-0051 Step 2 established), 0
`FAIL`/`ERROR`/`Exception` anywhere. `gitleaks git --staged -v`: clean.
`git status --porcelain` staged set: exactly the six files this
checkpoint touches (the new test file, the four renames, `persona.clj`)
— nothing else.

Committed `3f43a46` ("refactor: sim-model's resources take their own
name — the last nesting drift closes, gated (alignment fixes 3,
AR-F3-1/2/3)"), pushed. Post-push verification: `git log --format=%B -1
3f43a46` diffed against the message file — one delta, the known
harmless trailing-newline artifact.

### Verification

- `bin/regression-oracle 2599355 3f43a46`: **all eleven vendored-root
  batches byte-identical** (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`); soundness check "yes
  outside ns form"; `IDENTICAL: every root's digest matches between
  2599355 and 3f43a46`. Expected: the demographics EDN content moved
  byte-for-byte, and `persona.clj`'s only change was the resource path
  prefix each `io/resource` call resolves through — the sampled content
  is identical, so every downstream byte is too. No
  `--declared-digest-change` licensed or needed.
- Assertion-count delta vs. Step 0: `docs-tooling`'s own test directory
  went from 17 files (post alignment-fixes-2) to 18 files; +1 file,
  +2 deftests/+11 assertions (`resource-nesting-test`), measured by
  running the new namespace directly, not inferred from the full-suite
  log's larger per-project totals.
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan this session (baseline `detect`, both
  staged scans, both pushes).
- Post-push message verification, both checkpoints (the tag push has no
  message-file analog; the code commit's verification is above): one
  delta against the message file, the known harmless trailing-newline
  artifact prior sessions already name.

### ADR-0025 tolerance closure

ADR-0025's own S1 account disclosed, once, that `sim-model`'s
`resources/` kept the pre-split `sim/demographics/...` path so the
2026-08-02 extraction needed no `io/resource` edit — a deliberate,
named tolerance, never ruled permanent. This session closes it: the
tolerance is retired, `sim-model`'s resources now nest under its own
brick name like every sibling component, and AR-F3-2's gate makes the
underlying rule structural rather than a fact six components happened
to already satisfy. ADR-0025 itself is unedited — its own text remains
the accurate historical record of what the 2026-08-02 session actually
did; this ADR is where the closure is recorded, per this session's own
fence.

### Pending arc-close register append

Per AR-C-2's own contract (`.agents/rulings.md`'s header), joining
ADR-0050 AR-F1-6's and ADR-0051 AR-F2-0's queued entries — a fourth:

- **S1/C-1 closed** (this session): `sim-model`'s resource-nesting
  drift is fixed and gated; no register row remains open in this
  cluster.

Not appended this session — this is the note, not the append.

### Consequence

`components/sim-model/resources/sim-model/` now matches the nesting
convention every sibling `components/*/resources` directory already
followed; a new gate
(`ehrt.docs-tooling.resource-nesting-test`) makes that convention
structural, catching the next drift wherever it lands rather than
needing another audit to notice. ADR-0025's own disclosed tolerance is
closed, cited here rather than edited there. This session touched 2 of
the register's 47 rows directly (S1, C-1 — one cluster, not
double-counted). Session 4 (NIST jar mirroring) follows next, riding
this landing's own tag mechanic at its own Step 0.

## ADR-0051 — Alignment fixes 2: the law reads the same everywhere, and three laws get teeth

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: alignment fixes 1 landed and was design-channel-verified
(`72add4a`, `notes/adr/0050-alignment-fixes-1.md`). That session
correctly DEFERRED its own tag act (AR-F1-0): `AGENTS.md`'s standing
text ("tags... stay the author's alone under either mode") had never
been updated when ADR-0049's AR-AU-0 amended the actual mechanic
(sessions may tag when a specific commit is specifically licensed) —
two repo surfaces stated conflicting law, and the session refused to
resolve that conflict ad hoc, correctly. This session reconciles the
surfaces first, then executes both pending tags under the now-coherent
rule, then promotes three prose invariants (register rows S5, A-5) to
gated tests. R30 ceremony.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's own
prompt):

**AR-F2-0 (law-surface reconciliation — root cause owned).** The
conflict was a design-channel authoring miss: AR-AU-0 amended standing
law in the ADR/rulings trail without propagating to `AGENTS.md`, the
surface sessions actually read first. Fix: amend `AGENTS.md`'s tag rule
with a dated note — sessions MAY create and push `stable-*` continuity
tags when a session prompt licenses a SPECIFIC tag at a SPECIFIC
commit, which the design channel issues only after verifying that
landing (ADR-0049 AR-AU-0; reconciled here after ADR-0050's principled
deferral); the author may always tag directly; every OTHER tag class
(release `v*` tags especially) remains AUTHOR ACTION. The amendment
edits the tag rule in place with the dated citation — `AGENTS.md` is a
live instruction surface, not a frozen archive. This ADR records the
standing lesson for the arc-close register append: an amendment to
standing law lands on every surface that states the law, in the same
session that rules it.

**AR-F2-1 (both pending tags, AFTER the amendment commits).** (a)
`stable-20260805-alignment-audit` at `989d6cf`, message `alignment
audit landed, design-channel-verified 2026-08-05 (ADR-0049)` — the
exact commands ADR-0050 already prepared. (b)
`stable-20260805-alignment-fixes-1` at `72add4a`, message `alignment
fixes 1 landed, design-channel-verified 2026-08-05 (ADR-0050)`.
Annotated tags, `git push origin <tag>` each, both verified on origin
(`git ls-remote --tags`, peeled refs resolve to the stated commits).
Order is load-bearing: the AGENTS.md amendment must be committed and
pushed before the tag acts, so no act occurs under conflicting law.

**AR-F2-2 (gate: sim-emit-hl7 dependency law).** New deftest in
`docs-tooling`'s gate family: parse the `ns` form of every `.clj` under
`components/sim-emit-hl7/src/`; assert every required `ehrt.*`
namespace matches `ehrt.sim-model.*` or `ehrt.sim-emit-hl7.*` — nothing
else (the AGENTS.md constraint, verbatim in the test's docstring with
its citation). Parse requires from the ns form properly (read the form,
walk `:require` clauses) — no naive regex over whole files that would
trip on docstrings.

**AR-F2-3 (gate: provenance leaf law).** Same shape for
`components/provenance/src/`: every required `ehrt.*` namespace matches
`ehrt.provenance.*` only (AR-2, ADR-0043, cited in the docstring). The
deps.edn side (whether provenance's own `deps.edn` declares nothing
beyond `metosin/malli`) is `poly check`/`libs :outdated`'s own job — not
duplicated here.

**AR-F2-4 (gate: root-alias completeness — register row A-5).** New
deftest asserting, bidirectionally: (a) the root `deps.edn` `:dev`
alias's `:local/root` entry set maps 1:1 onto the union of
`components/*` and `bases/*` directories on disk; (b) every
`components/*/test` and `bases/*/test` directory that exists appears in
the `:test` alias's `:extra-paths`, and every listed path exists on
disk (project test dirs, e.g. `projects/*/test`, are allowed listings
verified for existence, not required from the brick side). Read
`deps.edn` as EDN, not by grep.

**AR-F2-5 (red→green witnessed per gate).** For each of the three
gates: after landing the test green, demonstrate the red via a
transient, uncommitted violation, run the gate, capture the failure
output, restore the tree byte-exact, and record all three red
transcripts here. No violation ever staged or committed.

### Step 0 — preflight

Working directory confirmed `~/src/ehr-testing-tools` (ext4); tip
`72add4a` exactly. `git tag`/`git ls-remote --tags origin` confirmed
both tag-target commits (`989d6cf`, `72add4a`) exist. **Premise check,
disclosed, not silently adapted:** the session's own read-first
material assumed both tags were still pending. The live tree showed
`stable-20260805-alignment-audit` already existing at `989d6cf`, tag
object `741dfb5`, message matching ADR-0050's prepared command
exactly — consistent with "the author may always tag directly" (both
ADR-0050 AR-F1-0's own deferral note and this ADR's own AR-F2-0
amendment): the author evidently ran ADR-0050's prepared command
directly between sessions. `stable-20260805-alignment-fixes-1` did not
yet exist. AR-F2-1(a) is therefore a verify-only act this session;
AR-F2-1(b) is the one tag this session actually creates.

Baseline: full suite green (`clojure -M:poly test :all skip:integration`,
206 `Test results:` lines, 0 `FAIL`/`ERROR`/`Exception` anywhere in the
captured log; `docs-tooling`'s own final project-block count unaffected
by later steps stayed unchanged run-over-run, confirming no cross-talk
between projects). `clojure -M:poly check`: OK. `gitleaks detect -v`:
no leaks found, 654 commits scanned.

**Second disclosed discrepancy:** register row S5's own evidence line
states `ls components/docs-tooling/test/ehrt/docs_tooling/` as "13
files"; this session's own fresh listing at Step 0 counted **14**
pre-existing files. Immaterial to AR-F2-2/3/4's own instructions (which
name the gates to add, not a target count) and not investigated
further — noted the same way ADR-0050 noted its own sweep-count
re-derivation delta, a minor register-evidence drift, not a defect.

### Step 1 — reconcile + tag (AR-F2-0/1)

`AGENTS.md`'s "Session mode and ceremony" section, tag-rule clause
amended in place (diff confined to that one clause, confirmed by `git
diff -- AGENTS.md` before staging — no other line in the file
touched): the two-classes-stay-author-alone sentence now carves out
`stable-*` continuity tags under a session-prompt-specific license, with
a `2026-08-05 amendment` dated note citing this ADR's own AR-F2-0 and
ADR-0049's AR-AU-0, and naming ADR-0050 AR-F1-0 as the principled
deferral the gap surfaced through. `gh` mutations and every other tag
class are unchanged.

Committed `3ee322f` ("docs: the law reads the same everywhere —
AGENTS.md catches up with AR-AU-0 (alignment fixes 2, AR-F2-0)"),
pushed. Post-push verification: `git log --format=%B -1 3ee322f`
diffed against the message file — one delta, the known harmless
trailing-newline artifact.

**Tag acts, both verified on origin:**

```
$ git tag -a stable-20260805-alignment-fixes-1 72add4a \
    -m "alignment fixes 1 landed, design-channel-verified 2026-08-05 (ADR-0050)"
$ git push origin stable-20260805-alignment-fixes-1
 * [new tag]         stable-20260805-alignment-fixes-1 -> stable-20260805-alignment-fixes-1

$ git ls-remote --tags origin | grep -E "alignment-fixes-1|alignment-audit"
741dfb53ba0ef2558c9b04b47859885c6060569c	refs/tags/stable-20260805-alignment-audit
989d6cf04d00ecbd3bdec23f427bd6d516e585ad	refs/tags/stable-20260805-alignment-audit^{}
d1d625008d5f86d4ddc424967c049fe2ff437057	refs/tags/stable-20260805-alignment-fixes-1
72add4ad65260ddf10144316beed750bd8c8de64	refs/tags/stable-20260805-alignment-fixes-1^{}
```

Both peeled refs (`^{}`) resolve to the exact ruled commits
(`989d6cf`, `72add4a`); both messages match the ruling's own text
exactly (alignment-audit's, pre-existing, confirmed by `git tag -l -n99`
before this session touched anything).

### Step 2 — three gates (AR-F2-2/3/4/5)

New files, `components/docs-tooling/test/ehrt/docs_tooling/`:
`sim_emit_hl7_dependency_test.clj` (3 deftests/10 assertions),
`provenance_leaf_law_test.clj` (3 deftests/8 assertions),
`root_alias_completeness_test.clj` (6 deftests/8 assertions) — 12
deftests/26 assertions total, each file following the established gate
family's shape (docstring citing rulings + register rows, private
extraction helpers, a real-tree gate deftest, plus mechanism-sanity
deftests on synthetic fixture data proving the extraction/allow-list
functions actually catch what they claim to, the same pairing
`ehrt.docs-tooling.done-pointer-adr-test` and
`ehrt.docs-tooling.index-completeness-test` already use). All three
read source with the Clojure reader (`PushbackReader` + `read`) and
walk the parsed `ns` form's own `:require` clause — never a regex over
raw file text, so an `ehrt.*`-shaped namespace name appearing only as
docstring prose (both `sim-emit-hl7` and `provenance` source files cite
sibling namespaces conversationally in comments) is never mistaken for
a real require. `root-alias-completeness-test` reads `deps.edn` via
`clojure.edn/read-string`, never grep.

Green on the live tree (verified by running the three new namespaces
directly, then via the full suite): `clojure -M:dev:test -e
"(require ...) (run-tests ...)"` → `Ran 12 tests containing 26
assertions. 0 failures, 0 errors.` `clojure -M:poly check`: OK. Full
suite (`clojure -M:poly test :all skip:integration`): green, 212
`Test results:` lines (+6 vs. Step 0's 206, matching 3 new namespaces
appearing once per project × 2 projects that include `docs-tooling`),
0 `FAIL`/`ERROR`/`Exception` anywhere.

**Red→green, all three, transient and uncommitted:**

1. **sim-emit-hl7.** Added `[ehrt.corpus.interface :as corpus]` to
   `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/site_profile.clj`'s
   own `:require`:
   ```
   FAIL in (sim-emit-hl7-src-requires-nothing-beyond-sim-model-and-its-own-namespaces-test)
   components/sim-emit-hl7/src/ehrt/sim_emit_hl7/site_profile.clj requires ehrt.* namespace(s) outside sim-model/its own component: ...
   expected: (empty? disallowed)
     actual: (not (empty? ("ehrt.corpus.interface")))
   Ran 3 tests containing 10 assertions.
   1 failures, 0 errors.
   ```
   `git checkout -- components/sim-emit-hl7/src/ehrt/sim_emit_hl7/site_profile.clj`; `git diff` on that path empty; `git status` clean of unintended changes afterward.

2. **provenance.** Added `[ehrt.kernel.interface :as kernel]` to
   `components/provenance/src/ehrt/provenance/manifest.clj`'s own
   `:require`:
   ```
   FAIL in (provenance-src-requires-nothing-beyond-its-own-namespaces-test)
   components/provenance/src/ehrt/provenance/manifest.clj requires ehrt.* namespace(s) outside provenance itself: ...
   expected: (empty? disallowed)
     actual: (not (empty? ("ehrt.kernel.interface")))
   Ran 3 tests containing 8 assertions.
   1 failures, 0 errors.
   ```
   `git checkout -- components/provenance/src/ehrt/provenance/manifest.clj`; `git diff` on that path empty; `git status` clean afterward.

3. **root-alias.** Deleted the `poly/palgebra {:local/root
   "components/palgebra"}` `:dev` entry from the root `deps.edn`:
   ```
   FAIL in (dev-local-roots-match-every-real-brick-dir-exactly-test)
   every real brick has a :dev :local/root entry
   components/bases directories missing from :dev's :local/root entries: #{"components/palgebra"}
   expected: (empty? (set/difference real declared))
     actual: (not (empty? #{"components/palgebra"}))
   Ran 6 tests containing 8 assertions.
   1 failures, 0 errors.
   ```
   `git checkout -- deps.edn`; `git diff` on that path empty; `git status` clean afterward.

Between each of the three, `git status --porcelain` showed only the
three new (not-yet-committed) test files as untracked — no violation
was ever staged. Re-run after all three restores: `clojure -M:poly
check` OK, the 12 new deftests green again, full suite green, 0
failures.

Committed `ab20b6f` ("test: three laws get teeth — dependency fences
and root-alias completeness now gated (alignment fixes 2,
AR-F2-2/3/4)"), pushed. Post-push verification: one delta against the
message file, the known trailing-newline artifact.

### Verification

- `bin/regression-oracle 72add4a ab20b6f`: **all ELEVEN vendored-root
  batches byte-identical** (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`); soundness check "yes
  outside ns form"; `IDENTICAL: every root's digest matches between
  72add4a and ab20b6f`. Expected: this session touched no `src/` file
  in any committed change (only `AGENTS.md` prose and new `test/`
  files), the transient red violations were never committed, and both
  tag acts touch no tree content. No `--declared-digest-change`
  licensed or needed.
- Assertion-count delta vs. Step 0: `docs-tooling`'s own test directory
  went from 14 files (Step 0's fresh count; register row S5's own
  evidence said 13, a minor drift disclosed above, not investigated
  further) to 17 files; +12 deftests/+26 assertions
  (`sim-emit-hl7-dependency-test` 3/10, `provenance-leaf-law-test` 3/8,
  `root-alias-completeness-test` 6/8), measured by running the three
  new namespaces directly, not inferred from the full-suite log's
  larger per-project totals.
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, both checkpoints: each showed exactly
  one delta against its own message file — the known, harmless
  trailing-newline artifact prior sessions already name.

### Pending arc-close register append

Per AR-C-2's own contract (`.agents/rulings.md`'s header), noted here
so the arc-close session cannot miss it — joining ADR-0050 AR-F1-6's
own two queued entries (A-3 dependency-review cadence, D-3
pairing-as-data registry landing spot), a third:

- **AR-F2-0's own standing lesson (law-surface propagation):** an
  amendment to standing law lands on every surface that states the
  law, in the same session that rules it — never left to a later
  session to notice the drift, and never resolved ad hoc by a session
  that only notices the conflict in passing (ADR-0050's own AR-F1-0
  deferral was the correct response to finding the gap; this ADR's own
  AR-F2-0 is the correct response to closing it).

Not appended this session — this is the note, not the append.

### Consequence

`AGENTS.md`'s tag rule now states the same law ADR-0049's AR-AU-0 ruled
three sessions ago, with a dated citation trail; both `stable-*`
continuity tags this arc has licensed so far are live and verified on
origin. Three prose-only invariants (sim-emit-hl7's dependency law,
provenance's leaf law, the root-alias completeness A-5 confirmed by
hand) are now gated tests, each proven to actually catch its own
violation before being trusted. This session touched 3 of the
register's 47 rows directly (S5 twice — both seeded candidates — and
A-5); the law-surface reconciliation itself was not a register row but
a standing-process defect this session both found and fixed in the
same pass. Session 3 (S1 rename + the resource-nesting gate) follows
next, riding this landing's own now-coherent tag mechanic at its own
Step 0.

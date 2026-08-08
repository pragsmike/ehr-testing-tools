## ADR-0078 — Result or loud: an I/O failure can no longer impersonate an empty directory

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: `notes/adr/0077-repo-review-1.md` landed the quality-review
arc's first survey — eight dimensions, 45 disposition-carrying rows
(corrected this session, see AR-RL-R below), nothing moved. This
session's own driving prompt is the author's ruled response to that
register (design channel, 2026-08-07, all five rulings recorded
verbatim as AR-RL-5, below): fix session 1 of the mitigation plan,
closing the register's single highest-severity cluster — ONE root
cause surfacing in three dimensions (D4-1, D3-4, D8-2/D8-3): a
production I/O call living outside this repo's own `Result`
vocabulary, including a DEMONSTRATED silent-success path —
`bases/cli/core.clj`'s `mutate-command` returned `{:status :ok,
:payload {:count 0, :files []}}` for a directory listing that had
actually failed at the OS level, indistinguishable in code from a
directory that is genuinely empty.

R30 ceremony. Read-first (this session): the register's D4-1, D3-4,
D8-2, D8-3 rows in full; `bases/cli/src/ehrt/cli/core.clj`'s
`files-with-extension-in`/`mutate-command`; `components/kernel/src/
ehrt/kernel/artifact.clj`'s `.renameTo` call (fetch), `extracted-
already?`, `find-executable`; `components/sim/src/ehrt/sim/run.clj`'s
`similar-sibling-config` (ADR-0076's own fix — the pattern this
session generalizes, including its retry idiom and its docstring's own
discipline of naming the failure mode rather than absorbing it
silently); `ehrt.kernel.result`'s API; every remaining register-listed
site; `README.md`'s "What you get" fence.

**CI-red policy note (this arc's own, disclosed as the pattern
shift it is):** red checkpoints are witnessed in-session and recorded
in this ADR's own transcripts, below; commits land green — no push
this session carried a knowingly-failing test. This supersedes the
older red-commit checkpoint pattern for this and future sessions.

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-07,
distributed across this session, fix session 2, and the arc close).
`[A]` author-ruled, `[C]` channel-inferred.

1. **AR-RL-0 `[A — tag law, case (ii); debt recorded in ADR-0077]`.**
   Annotated `stable-20260807-repo-review-1` at `93bd9a6`, message
   "repo review 1 landed, design-channel-verified 2026-08-07
   (ADR-0077)"; pushed; peeled ref verified. **Executed Step 0, this
   session.**

2. **AR-RL-R `[C — the register correction]`.** The findings register
   gains a dated correction note (append-don't-erase): the summary's
   44/26/6 corrects to 45/28/5 by direct row count, method stated; the
   alignment register's own same-class 51-vs-47 drift cited as
   precedent. **Executed Step 1** — commit `90432ad`.

3. **AR-RL-1 `[C — the helper]`.** `ehrt.kernel` gains the guarded-I/O
   helper(s) the sweep needs. **Executed** — `ehrt.kernel.io`
   (`list-files`/`existing-dir-nonempty?`/`rename!`), red-first unit
   tests on every branch via an injectable lister/renamer seam.

4. **AR-RL-2 `[C — the sweep, in priority order]`.** Every
   register-listed site converts to the helper, happy paths
   byte-identical. **Executed** — see the conversion table, below.

5. **AR-RL-3 `[C — the operator-facing pair]`.** `corpus mutate` on a
   missing/unreadable PATH returns the same categorized envelope its
   siblings give; the README fence adjusts minimally. **Executed.**

6. **AR-RL-4 `[C — the recurrence gate]`.** A docs-tooling lint test:
   no bare `.listFiles`/`.list(`/`.renameTo` in `components/*/src` or
   `bases/*/src` outside the kernel helper's own namespace (allowlist
   by ns). **Executed** — natural red against the pre-sweep tree
   witnessed in-session (below).

7. **AR-RL-5 `[A — the author's five rulings on the register, recorded
   verbatim, execution distributed]`.**
   1. The `state.md` staleness tripwire: ADOPTED — lands in fix
      session 2.
   2. The generalized multi-surface-law drift scaffold: DEFERRED with
      a named trigger — a third law drifting the hard way builds the
      registry.
   3. `defspec` seed policy: the middle path — seeds stay unpinned
      repo-wide; a spec that has actually flaked gets its seed pinned
      or durably logged (the engine spec, in fix session 2);
      test.check's printed seed plus CI log retention ruled sufficient
      for the rest, revisited on the next flake.
   4. Pairing-as-data: RULED IN — the design pass opens in the design
      channel in parallel with this session; the register stops
      counting.
   5. The multi-seed-once-flagged vendoring practice: ADOPTED as a
      rulings append at this arc's close.
   **Not this session's own execution** — recorded here per the
   prompt's own instruction, distributed across this session (item 1's
   landing spot named), fix session 2, and the arc close.

8. **AR-RL-6 `[C — scope]` (fences).** Src edits ONLY in: the kernel
   helper ns (new), the register-listed call sites, `mutate-command`'s
   error path, and the README fence + its prose line. **Held** — see
   Fences, below.

### The conversion table

Every register-listed site (D4-1's 9+, D3-4's 1, D8-2/D8-3's `corpus
mutate`), old behavior → new category. HAPPY PATHS UNCHANGED
throughout — every row below describes only what changes on the
FAILURE path (confirmed by the regression-oracle bracket, Verification
below, and by every existing test staying green).

| site | old behavior on an I/O failure | new behavior |
|---|---|---|
| `bases/cli/core.clj` `files-with-extension-in`/`mutate-command` (the demonstrated case) | `.listFiles` nil → read as empty dir → `mutate-command` returns `{:status :ok, :count 0}` | `result/error :listing-failed {:path}` propagated; the batch never runs |
| `bases/cli/core.clj` `mutate-command` on a missing/unreadable `:path` | falls into the single-file branch → raw, uncaught `FileNotFoundException` at read time | `result/error :file-not-found {:path}`, exit 2, checked before any listing is attempted |
| `corpus/generate.clj` `non-empty-existing-dir?` (`generate!`'s `:out-dir-exists` guard) | nil → `(seq nil)` false → guard reads "safe, proceed" → the determinism guard is defeated | `kernel/error :listing-failed`, refuses the run |
| `corpus/generator_source.clj` `non-empty-existing-dir?` (`resolve!`, pre-execute guard AND post-execute no-output check) | same guard-defeat pre-execute; post-execute an I/O failure was misreported as `:generator-produced-no-output` | `kernel/error :listing-failed` at both points, distinct from a genuine no-output result |
| `corpus-io/spool.clj` `non-empty-existing-dir?` (`spool!`'s `:spool-target-exists` guard) | same guard-defeat | `kernel/error :listing-failed`, refuses the run |
| `corpus-io/sink_write.clj` `non-empty-existing-dir?` (`write-dir!`'s `:sink-target-exists` guard) | same guard-defeat | `kernel/error :listing-failed`, refuses the run |
| `kernel/artifact.clj` `extracted-already?` (`resolve-and-extract`) | I/O failure read as "not yet extracted" → silently re-extracts, masking the failure | `result/error :listing-failed`, refuses rather than silently re-doing work |
| `kernel/artifact.clj` `find-executable` | I/O failure on an existing root misreported as the SAME `:executable-not-found` rejection a genuinely-empty root gives | `result/error :listing-failed` when root is a directory but listing it fails; `:executable-not-found` unchanged when root genuinely isn't a directory or genuinely has no match |
| `kernel/artifact.clj` `fetch` (`.renameTo`) | a `false` return (e.g. cross-filesystem rename) silently ignored — still returns `{:cached false}` success, file never moved | `result/error :rename-failed {:from :to}`; tmp deliberately left in place (already hash-verified, cheaper than a fresh download on retry) |
| `sim-trajectory/census.clj` `discover-root-modules` (`run-census`) | nil → `(filter pred nil)` => `()` → silently censuses zero root modules | `result/error :listing-failed` propagated through `run-census` |
| `judge-fhir-official/fhir.clj` `json-files-in` (`gate-dir`) | nil flows unguarded into the filter/sort chain | `result/error :listing-failed` propagated before `gate-dir`'s own reduce runs |
| `bases/cli/core.clj` `gate-candidate-files-in` (`sniff-gate-command`, `show-command`, `play-events-from-dir` — three callers) | nil → read as "no candidate files" → the WRONG category (`:gate-format-ambiguous`/`:show-format-ambiguous`/`:play-input-unsupported`, an expected-outcome rejection) for what was actually an operational I/O failure | `result/error :listing-failed` propagated first, at all three call sites |

**One live regression caught and fixed mid-sweep, disclosed rather
than folded in silently:** converting `corpus/generate.clj`'s
`non-empty-existing-dir?` to return a Result left ONE caller
un-updated — `bases/cli/core.clj`'s `generate-sim-command`, which
still branched on the old function as a bare boolean. A Result map is
always truthy, so this caller began treating every `--out-dir` as
already existing. The full suite (`clojure -M:poly test :all
skip:integration`), re-run after the guard-site sweep, went red —
`generate-sim-command-same-seed-is-byte-identical-test` and two
siblings failed. Traced immediately to this one call site, fixed
(unwraps the Result the same way every other converted caller does),
full suite green again. No other caller of any converted guard
function was missed — confirmed by `grep -rn` for every remaining
`non-empty-existing-dir?`/`out-dir-exists?` reference repo-wide after
the fix.

**Not converted, disclosed:** `ehrt.sim.run/similar-sibling-config`.
ADR-0076's own fix already implements the correct retry-once-then-
name-the-failure idiom locally (predating this session's shared
helper) — migrating it onto `ehrt.kernel.io` is deferred cargo, out of
this session's own fence (AR-RL-6), not a violation of the new
recurrence gate (AR-RL-4 allowlists `ehrt.sim.run` by name, disclosed
in the gate test's own docstring).

### Red transcripts, witnessed in-session

**The helper (`ehrt.kernel.io`).** Before the source file existed:
`Could not locate ehrt/kernel/io__init.class` (an honest red — the
right reason). After: 11 tests, 27 assertions, 0 failures.

**`mutate-command`'s two fixes.** `git stash push` on only
`bases/cli/src/ehrt/cli/core.clj` (keeping the new tests in place),
full suite re-run:
- The listing-failure test: expected `result/error?`, got
  `(not (result/error? {:status :ok, :payload {:count 1, :files
  [...]}}))` — the exact regression this session closes, demonstrated
  live against the pre-fix source, not asserted from memory.
- The missing-path test: `Uncaught exception, not in assertion` — a
  real `java.io.FileNotFoundException` stack trace, traced through
  `read-base-data` → `mutate-command`, confirming D8-2/D8-3's own
  finding exactly.

`git stash pop`, full suite green again (0 failures, 0 errors).

**The recurrence gate (AR-RL-4).** `git stash push` on the nine
already-converted source files only (the new gate test and
`ehrt.kernel.io` itself left in place), run from workspace root (the
gate scans `components/*/src`/`bases/*/src` from cwd, so this had to
run from the repo root, not the docs-tooling component directory —
confirmed the first attempt vacuously passed for the wrong reason,
zero files scanned, before rerunning correctly from root): **8
failures**, naming exactly `bases/cli/core.clj`, `corpus/generate.clj`,
`corpus/generator_source.clj`, `corpus-io/spool.clj`,
`corpus-io/sink_write.clj`, `kernel/artifact.clj` (two hits),
`judge-fhir-official/fhir.clj`, `sim-trajectory/census.clj` — the
register's own site list, no more, no fewer. `git stash pop`, full
suite green again (94 assertions passing on the gate's own three
tests).

### Verification

- `clojure -M:poly check`: OK, confirmed after every edit throughout
  the session (not just once at the end).
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (511 assertions, 0 failures/0 errors, matching every
  "Test results:" block across the run). Re-run after every
  significant edit; the one real regression (`generate-sim-command`,
  above) was caught this way and fixed before proceeding. Green at
  every subsequent checkpoint, final confirmation after AR-RL-4's gate
  landed: 511 assertions, 0 failures, 0 errors.
- `gitleaks git --staged -v`: clean, both commits this session
  (`90432ad`, `3684a30`); the pre-push hook ran it again on both
  pushes, clean.
- Post-push message verification: both commits' pushed message
  diffed against their own message files — only the known
  `git log --format=%B`-trailing-blank-line artifact, no other
  mismatch.
- `bin/regression-oracle 93bd9a6 <this session's closing tip
  3684a30>`: all twenty-seven roots confirmed **IDENTICAL**,
  soundness "yes outside ns form" — this session's own src edits
  never changed a single emitted byte on any happy path.
- CI, both pushes, watched to conclusion (not assumed): `90432ad`
  **success** (run `31235917902`, 3m25s); `3684a30` **success** (run
  `31237928947`, 3m9s, watched live start-to-finish). Last-five CI
  conclusions on `main` at Step 0 preflight: `93bd9a6` success,
  `075db9b` success, `ac6ef5f` **failure** (the already-disclosed,
  already-closed red window ADR-0077's own successor commit named and
  re-verified — not a fresh finding, `93bd9a6`'s own commit message
  records this), `89c0d24` success, `9a34409` success — disclosed in
  full per the widened five-run check (quality riders AR-QR-3).

### Fences

Src edits landed ONLY in: `ehrt.kernel.io` (new), the register-listed
call sites named in the conversion table above, `mutate-command`'s
error path (the `:file-not-found` check), and `README.md`'s "What you
get" fence + its immediately preceding prose line. No fix-session-2
cargo landed (the lint family beyond AR-RL-4's own single gate, the
`state.md` tripwire, the `defspec` seed pin, the repo-review skill
amendment) — all four named explicitly in AR-RL-5 above as NOT this
session's own execution. Standing untracked files (`config/
busy-weekday.md`) untouched. No happy-path output changed anywhere,
confirmed by the oracle bracket above, not merely asserted.

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260807-result-or-loud` at THIS session's own closing tip
(`3684a30`), under standing ceremony** — the same tag-law case (ii)
pattern ADR-0077 used for its own predecessor. This session does not
tag its own closing tip (a session tags its PREDECESSOR's own verified
stable point, not its own mid-flight one); it inherits
`stable-20260807-repo-review-1` (AR-RL-0, above) and passes its own
tag forward exactly the same way.

### Index line

```
- 2026-08-07 — result-or-loud — ADR-0078
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 75→76, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated unchanged

This session closed the register's single highest-severity cluster
only. Untouched, per AR-RL-6's own fence: the lint family beyond
AR-RL-4's own gate, the `state.md` staleness tripwire, the `defspec`
seed pin, the repo-review skill's summary-re-derivation amendment —
all four named in AR-RL-5 as fix session 2's own cargo. Also
untouched, carried forward unchanged from ADR-0074/0075/0076/0077: the
EncounterEnd design pass, Wave E's own register, vendoring batch 4,
publish-prep. Pairing-as-data is explicitly NOT this arc's item
anymore — AR-RL-5 item 4 ruled it in, opening in the design channel in
parallel with this session; the register stops counting its age.

**What DOES change:** after design-channel verification of this
session's own landing (the gate re-run, the conversion table sampled
site-by-site, the `mutate` error envelope read live), fix session 2
gets its own prompt (the lint family, the tripwire, the seed pin, the
skill amendment), and the arc close follows with AR-RL-5's five
rulings appended to their own permanent homes.

### Consequence

The register's single highest-severity row — a real I/O failure
producing a clean, successful, wrong `{:count 0}` answer with zero
error surfaced — no longer exists in the live tree, confirmed by a
red-then-green transcript against the pre-fix source itself, not
merely by reading the new code and trusting it. Its two siblings
(`kernel/artifact.clj`'s unchecked `.renameTo`, `corpus mutate`'s
unwrapped file-read) close in the same sweep, the shared root cause
the register itself named. A recurrence gate stands watch — witnessed
red against the exact pre-sweep tree, naming exactly the sites the
register named, no more, no fewer — so the next site that reaches for
a bare `.listFiles` fails a test instead of waiting for another review
to find it by hand.

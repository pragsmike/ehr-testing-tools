## ADR-0059 — UX fixes 1: every doc teaches the real invocation — swept, paired, gated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: the UX audit landed and was design-channel-verified (`bc66bd6`,
`notes/adr/0058-ux-audit.md`). This session executes the register's
first ruled cluster: U1's invocation sweep, the module-mix flag-pairing
fix, and the invocation-lint gate
(`.agents/plans/2026-08-06-ux-audit-findings.md`, register row U1;
A-1/A-2/A-3). Mechanical by the register's own evidence — A-2
pre-verified every corrected form against `docs/cli.md`'s live
grammar.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's own
prompt):

**AR-U1-0 (tag, standing ceremony).** Annotated
`stable-20260806-ux-audit` at `bc66bd6`, message "ux audit landed,
design-channel-verified 2026-08-06 (ADR-0058)"; push; verify.

**AR-U1-1 (the sweep).** Re-derive the stale-invocation inventory by
fresh grep (`clojure -M:cli` across `README.md`, `AUTHORS-GUIDE.md`,
`docs/**`, `components/*/docs/**` — `.md` and `.edn` header comments);
the register's 11 file:line groups / 14 instances are the expectation
— a delta in either direction is disclosed, not silently absorbed.
Every instance corrects to the `bin/ehrt` form (`clojure -M:cli run` →
`bin/ehrt sim run`, preserving each command's own flags). Same pass:
`components/sim/docs/demos/module-mix/README.md:13` and its
`config.edn:5-6` twin gain the missing `--emit hl7` (register U1's
pairing defect — `--format er7` requires it, per `docs/cli.md`).
LICENSED-OPTIONAL (register A-3): if touching
`docs/simulate-your-facility.md` anyway, the line-202 placeholder
mention may become a real fence with concrete values — cosmetic,
disclose the choice either way.

**AR-U1-2 (the gate, co-landed).** New deftest in the docs-tooling gate
family: the string `clojure -M:cli` is forbidden across the sweep's
exact surface set (the four roots above, `.md` and `.edn` files both);
frozen archives, `.agents/prompts/`, `.agents/session-records/`, dated
one-shot plan files, and the audit registers are OUT of scope. Red→green
NATURAL: run the gate against the unswept tree first, capture the red,
land sweep + gate in one commit, green.

**AR-U1-3 (nothing else).** No `help.clj`, no error strings, no `src/`
— sessions 2–5 own those. A newly-noticed defect is a note here for the
arc's intake, never an act.

### Execution record

**Step 0 — preflight + tag.** Working directory confirmed
`~/src/ehr-testing-tools` (ext4); tip `bc66bd6` exactly; working tree
clean apart from `config/busy-weekday.md` (unrelated pre-existing
untracked file, left alone). Baseline: `clojure -M:poly check`: OK.
AR-U1-0 executed: `stable-20260806-ux-audit` did not exist locally or
on origin (checked both); created annotated at `bc66bd6`, message "ux
audit landed, design-channel-verified 2026-08-06 (ADR-0058)"; pushed;
verified — peeled ref resolves to `bc66bd6` exactly.

**Step 1 — red, then sweep + gate (AR-U1-1/2).**
`ehrt.docs-tooling.invocation-lint-test` (new file,
`components/docs-tooling/test/ehrt/docs_tooling/invocation_lint_test.clj`)
written first, scoped to README.md, AUTHORS-GUIDE.md, every `.md`/`.edn`
file under `docs/**`, and every `.md`/`.edn` file under
`components/*/docs/**`. Run against the unswept tree: **11 failures**,
one per file — `docs/simulate-your-facility.md`,
`components/sim-emit-hl7/docs/demos/README.md`,
`components/sim-emit-hl7/docs/demos/site-profiles/README.md`,
`components/sim/docs/demos/README.md`,
`components/sim/docs/demos/emit-state/README.md`,
`components/sim/docs/demos/order-result/config.edn`,
`components/sim/docs/demos/order-result/README.md`,
`components/sim/docs/demos/module-mix/config.edn`,
`components/sim/docs/demos/module-mix/README.md`,
`components/sim/docs/demos/persona-enriched/README.md`,
`components/sim/docs/demos/boarding-transfer/README.md`. Red captured
verbatim below.

**Fresh-grep delta vs. the register's own expectation: NONE.** The
independent re-derivation (`grep -rn "clojure -M:cli" README.md
AUTHORS-GUIDE.md docs/ components/*/docs/`) found the identical 11
file:line groups / 14 instances the register named — same files, same
line numbers, same count. No new surface, nothing dropped.

| # | file | line(s) | instances |
|---|---|---|---|
| 1 | `docs/simulate-your-facility.md` | 170 | 1 |
| 2 | `components/sim-emit-hl7/docs/demos/README.md` | 3 | 1 |
| 3 | `components/sim-emit-hl7/docs/demos/site-profiles/README.md` | 12, 13 | 2 |
| 4 | `components/sim/docs/demos/README.md` | 3 | 1 |
| 5 | `components/sim/docs/demos/emit-state/README.md` | 14, 17 | 2 |
| 6 | `components/sim/docs/demos/order-result/config.edn` | 5 | 1 |
| 7 | `components/sim/docs/demos/module-mix/config.edn` | 5 | 1 |
| 8 | `components/sim/docs/demos/persona-enriched/README.md` | 17, 24 | 2 |
| 9 | `components/sim/docs/demos/boarding-transfer/README.md` | 17 | 1 |
| 10 | `components/sim/docs/demos/module-mix/README.md` | 13 | 1 |
| 11 | `components/sim/docs/demos/order-result/README.md` | 15 | 1 |

Total: 11 files, 14 instances — matches the register exactly.

All 14 instances corrected to `bin/ehrt sim run ...`, flags preserved
verbatim, multi-line continuations (`\`) kept exactly as authored.
Rows 7 and 10 (module-mix's own pair, the register's independent
flag-pairing defect) additionally gained `--emit hl7`, placed before
`--format er7` to match this codebase's own established flag-order
convention (`persona-enriched/README.md`, `boarding-transfer/README.md`
both already order `--emit hl7 --format er7`). Gate re-run
post-sweep: 0 failures.

**A-3 disposition: declined.** The LICENSED-OPTIONAL cosmetic
conversion of `docs/simulate-your-facility.md:202`'s placeholder
mention (`ehrt sim identifiers --seed <seed> --patients <n> [--config
<file>]`) into a concrete-value fence was considered and not taken —
no `clojure -M:cli` involvement (it wasn't part of the gate's own
violation set), and this session's own fence commits to a strictly
mechanical, single-purpose diff (the stale-alias sweep and the
module-mix pairing fix only). Left as-is, disclosed per A-3's own
"disclose the choice either way."

Commit (sweep + gate, one commit, staging hygiene confirmed via `git
diff --cached --stat` — exactly the 11 corrected files + the new test
file, `config/busy-weekday.md` confirmed not staged): `27bf545`
("docs: every doc teaches the real invocation — the stale alias dies,
gated (ux fixes 1, AR-U1-1/2)"). `gitleaks git --staged -v`: clean.
`clojure -M:poly check`: OK. `clojure -M:poly test` (run from the
workspace root — the correct cwd per `AUTHORS-GUIDE.md`'s own
`bin/ehrt`-relative-paths note; a first attempt from
`projects/conformance`'s own directory produced spurious
`bin/ehrt`/fixture-not-found errors, a cwd artifact, not a real
failure, not repeated): every brick green, `invocation-lint-test`
included, 0 failures/0 errors throughout. Pushed; post-push
verification: one delta against the message file, the known harmless
trailing-newline artifact.

**Step 2 — this record.** `notes/ADRs.md` gains this ADR's own index
line; `notes/adr/README.md`'s own file count corrected 56→57 ("as of
ADR-0059"). Done pointer added to the live roadmap in the same commit
as the index line. Session record
(`.agents/session-records/2026-08-06-ux-fixes-1.md`) and this
session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-1.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md` in
the same commit.

### Verification

- `clojure -M:poly check`: OK, every step.
- Gate red→green: 11 failures (unswept tree, transcript above) → 0
  failures (post-sweep), same test file, no test logic changed between
  the two runs.
- `clojure -M:poly test`: green, every brick, 0 failures/0 errors,
  including the new `ehrt.docs-tooling.invocation-lint-test` namespace.
- `gitleaks`: clean at every scan (staged scan before the commit; the
  push's own pre-push hook run).
- Post-push message verification: one delta against the message file,
  the known harmless trailing-newline artifact.
- Tag verification: `stable-20260806-ux-audit` peeled ref resolves to
  `bc66bd6` exactly.
- **Oracle bracket** (`bin/regression-oracle bc66bd6 27bf545`, Step 0's
  own tip to Step 1's own closing commit): `IDENTICAL: every root's
  digest matches between bc66bd6 and 27bf545` — all eleven vendored-root
  batches (`appendicitis`, `death-fixture`, `ear-infections`,
  `ear-infections-engine`, `ear-infections-history-engine`, `sepsis`,
  `sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  expected for a docs-and-one-test-file session; soundness check
  passed (`declared-digest-change: no`), no
  `--declared-digest-change` needed. Any change would have been
  STOP-AND-ESCALATE per this session's own prompt.

### Fences (standing law applies unchanged, this session's own prompt)

Docs + one test file, nothing else. Command semantics unchanged — this
session edits what docs SAY, and every corrected fence maps cleanly to
`docs/cli.md`'s own grammar (register A-2's pre-verification held; no
fence forced a STOP). No gate weakening. Frozen archives untouched
(this ADR + index + Done pointer are the sanctioned acts). The register
is read-only — no row's disposition or evidence column was edited.

### Consequence

The 11-file, 14-instance stale-invocation surface the audit found is
now zero, confirmed by a fresh independent re-derivation that matched
the register exactly (no drift between audit and fix session). The
module-mix pairing defect — a corrected `bin/ehrt`-form command that
would still have errored — is closed in the same pass, so no reader
following either demo hits a dead end. The new gate makes the species
structurally impossible to reintroduce on the swept surface, not just
observed-clean today. After landing: design channel verifies by fresh
probe; session 2 (error surfaces: C-1 + U4 + B-6/D-3 + the B-5 exit-0
ruling) follows, and this landing's own tag rides its Step 0 under
standing ceremony.

### Step 2 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line;
`notes/adr/README.md`'s own file count corrected 56→57 ("as of
ADR-0059"). Done pointer added in the same commit as the index line:

```
- 2026-08-06 — ux-fixes-1 — ADR-0059
```

Session record (`.agents/session-records/2026-08-06-ux-fixes-1.md`) and
this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-1.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md` in
the same commit.

### Appendix — red transcript (gate against the unswept tree, `bc66bd6`)

```
Testing ehrt.docs-tooling.invocation-lint-test

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
docs/simulate-your-facility.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/boarding-transfer/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/emit-state/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/order-result/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/order-result/config.edn teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/module-mix/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/module-mix/config.edn teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim/docs/demos/persona-enriched/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim-emit-hl7/docs/demos/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

FAIL in (no-stale-cli-alias-invocation-anywhere-in-live-docs-test) (invocation_lint_test.clj:45)
components/sim-emit-hl7/docs/demos/site-profiles/README.md teaches the stale `clojure -M:cli` invocation -- bin/ehrt is the live entry point (register row U1, AR-U1-2)

Ran 2 tests containing 108 assertions.
11 failures, 0 errors.
{:test 2, :pass 97, :fail 11, :error 0, :type :summary}
```
